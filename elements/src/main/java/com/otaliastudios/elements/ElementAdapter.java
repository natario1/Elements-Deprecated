package com.otaliastudios.elements;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import bolts.Continuation;
import bolts.Task;


/**
 * {@code ElementAdapter} is attached to {@link RecyclerView} and coordinates the work of other
 * components (elements, sources and presenters).
 *
 * After instantiating an ElementAdapter, accessors should:
 *
 * - call {@link #setSource(ElementSource...)} to register source(s) for the objects to be displayed.
 *   ElementAdapter will throw if it detects any circular dependency caused by
 *   {@link ElementSource#dependsOn(ElementSource)}.
 *
 * - call {@link #setPresenter(ElementPresenter...)} to register UI presenter(s).
 *   The mapping between source(s) and presenter(s) is not related to the position in the array.
 *   You can have multiple sources and single presenter, for instance. The mapping is done through
 *   {@link ElementSource#getElementType(Object)} and {@link ElementPresenter#getElementTypes()}.
 *
 * - call {@link #restoreState(Bundle)} with a possibly null saved instance state.
 *   Of course to have this working {@link #saveState(Bundle)} must be called when saving the state
 *   at the appropriate moment.
 *
 * - finally attaching the adapter to {@code RecyclerView}.
 *   It will automatically ask registered sources for objects for the first page,
 *   or use restored elements from the state bundle.
 *
 * Operations for Sources are bundled in groups that are performed together.
 * A group is defined as a set of Sources that are not dependent on each other. Groups are
 * computed in such a way that if {@code source1.dependsOn(source2)}, find operations for
 * {@code source1} are called after find operations for {@code source2}.
 *
 * Any call to {@code notify*} methods will likely break the internal state of the adapter.
 * Insertions, removals and any other dataset alteration must be done either through one of the many
 * {@link ElementSource} callbacks or through {@link Pager.Page} instances.
 *
 * @see Element
 * @see ElementSource
 * @see ElementPresenter
 * @see ElementSerializer
 *
 * TODO save & restore: what if a task had not ended? We should go by groups.
 * - Groups that had ended, should be restored through the Serializer
 * - Groups that had not, can't be restored and find() should be called again.
 * Some though must be given to callbacks consistency though.
 */
public final class ElementAdapter extends RecyclerView.Adapter<ElementPresenter.Holder> {


    private final static String TAG = ElementAdapter.class.getSimpleName();
    private final static boolean DEBUG = false;

    private static void log(String what) {
        if (DEBUG) Log.e(TAG, what);
    }

    private List<ElementSource> allSources;
    private List<ElementPresenter> presenters;
    private List<Set<Integer>> groups;

    private SparseArray<ElementSource> sourceIdMap;
    private SparseArray<ElementPresenter> elementTypeMap;
    private SparseArray<Set<Integer>> dependencyMap;
    private SparseArray<Set<Integer>> reverseDependencyMap;

    /* save */ private Pager pager;
    /* save */ private boolean initialized;

    // Not null for just a brief time.
    private Bundle savedInstanceState;

    /**
     * Create an ElementAdapter. By contract, {@link #setPresenter(ElementPresenter...)}
     * and {@link #setSource(ElementSource...)} must be called before attaching to a RecyclerView.
     */
    public ElementAdapter() {
        pager = new Pager(this);
        elementTypeMap = new SparseArray<>();
    }

    /**
     * Register an indefinite number of presenters that will receive Elements to be laid out.
     * @param presenters one or more ElementPresenter.
     */
    public void setPresenter(ElementPresenter... presenters) {
        this.presenters = Arrays.asList(presenters);
    }

    /**
     * Register an indefinite number of sources that will be asynchronously asked for Elements.
     * This will throw {@code IllegalArgumentException} if any circular dependency is detected.
     * @see ElementSource#dependsOn(ElementSource)
     *
     * @param sources one or more ElementSource.
     */
    public void setSource(ElementSource... sources) {
        int count = sources.length;
        allSources = Arrays.asList(sources);
        sourceIdMap = new SparseArray<>(count);
        dependencyMap = new SparseArray<>(count);
        reverseDependencyMap = new SparseArray<>(count);
        groups = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            // Assign a unique id to each source, based on its position in the input array.
            sourceIdMap.put(i, sources[i]);
            // Allocate maps for dependencies.
            dependencyMap.put(i, new HashSet<Integer>());
            reverseDependencyMap.put(i, new HashSet<Integer>());
        }

        // Initialize ordered lists.
        computeDependencies();
        computeGroups();
    }

    private void computeDependencies() {
        int size = allSources.size();
        for (int i = 0; i < size - 1; i++) {
            for (int j = i + 1; j < size; j++) {
                ElementSource o1 = allSources.get(i);
                ElementSource o2 = allSources.get(j);
                int id1 = getId(o1);
                int id2 = getId(o2);
                boolean dep1 = o1.dependsOn(o2);
                boolean dep2 = o2.dependsOn(o1);
                if (dep1 && dep2) {
                    throw new IllegalArgumentException("Circular dependency. Source " +
                            o1.getClass().getSimpleName() + " and source " +
                            o2.getClass().getSimpleName() + " both depend on each other.");
                } else if (dep1) {
                    // Surely o2 does not depend on o1. But we must check recursively that none
                    // of o2 dependencies depends on o1.
                    checkCircularDependencies(id1, id2);
                    dependencyMap.get(id1).add(id2);
                    reverseDependencyMap.get(id2).add(id1);
                } else if (dep2) {
                    // Same
                    checkCircularDependencies(id2, id1);
                    dependencyMap.get(id2).add(id1);
                    reverseDependencyMap.get(id1).add(id2);
                }
            }
        }

        // We also want to sort the sources based on a similar comparator.
        Collections.sort(allSources, new Comparator<ElementSource>() {
            @Override
            public int compare(ElementSource o1, ElementSource o2) {
                int id1 = getId(o1);
                int id2 = getId(o2);
                if (dependencyMap.get(id1).contains(id2)) {
                    return 1;
                } else if (dependencyMap.get(id2).contains(id1)) {
                    return -1;
                }
                return 0;
            }
        });
    }

    private void checkCircularDependencies(int source, int target) {
        Set<Integer> targetDependencies = dependencyMap.get(target);
        if (targetDependencies.contains(source)) throw new IllegalArgumentException("Indirect circular dependency detected.");
        for (int s : targetDependencies) {
            checkCircularDependencies(source, s);
        }
    }

    private void computeGroups() {
        // get groups of tasks that can be bundled.
        Set<Integer> currentGroup = new HashSet<>();
        for (ElementSource source : allSources) {
            int id = getId(source);
            // source must not be directly dependent of bundled sources.
            boolean canBeGrouped = true;
            for (int alreadyGrouped : currentGroup) {
                canBeGrouped = canBeGrouped && !dependencyMap.get(id).contains(alreadyGrouped);
            }
            if (canBeGrouped) {
                currentGroup.add(id);
            } else {
                // Close the bundle.
                groups.add(currentGroup);
                currentGroup = new HashSet<>();
                currentGroup.add(id);
            }
        }
        if (!currentGroup.isEmpty()) {
            groups.add(currentGroup);
        }
    }

    /* package */ int getId(ElementSource source) {
        return sourceIdMap.keyAt(sourceIdMap.indexOfValue(source));
    }

    /* package */ ElementSource getSource(int id) {
        return sourceIdMap.get(id);
    }

    /**
     * Returns a {@link Pager.Page} for the desired number.
     * Throws if the page has not been opened yet.
     *
     * @param pageNumber number of desired page
     * @return the Page dor the desired number.
     */
    public Pager.Page getPage(int pageNumber) {
        return pager.getPage(pageNumber);
    }

    /**
     * Returns the current {@link Pager.Page}, that is, the last page to be 'opened'.
     * This does not depend on the recycler scroll position.
     *
     * @return the current Page.
     */
    public Pager.Page getCurrentPage() {
        return pager.getCurrentPage();
    }

    /**
     * Creates a new element with desired data, assuming it was created by the provided {@code source}.
     * The source will be asked for the element type using {@link ElementSource#getElementType(Object)}.
     * This is useful for inserting new objects into an already opened page.
     *
     * @param source the element source that will be the Element parent.
     * @param data the model data
     * @return a new Element with desired data.
     */
    public <T> Element<T> createElement(ElementSource source, T data) {
        return new Element<>(getId(source), source.getElementType(data), data);
    }

    /**
     * Clear the whole adapter and leave it blank.
     */
    @UiThread
    public void clear() {
        pager.clearPages(0, pager.getNumberOfPages());
    }

    /**
     * Rebinds data for all pages.
     * @see #rebindPage(int)
     */
    @UiThread
    public void rebind() {
        for (int i = 0; i < pager.getNumberOfPages(); i++) {
            rebindPage(i);
        }
    }

    /**
     * Rebinds desired page. Rebinding does not asks sources for new objects through find(),
     * nor will notify callbacks. It just calls {@link ElementSource#getElementType(Object)}
     * and {@link ElementPresenter#onBind(Pager.Page, ElementPresenter.Holder, Element)}.
     *
     * This is useful for changing UI presentations through onBind without reloading data.
     * @param pageNumber page to be rebound
     */
    @UiThread
    public void rebindPage(int pageNumber) {
        Pager.Page page = pager.getPage(pageNumber);
        for (ElementSource source : allSources) {
            List<Element> elements = page.getElementsBySource(getId(source));
            for (Element element : elements) {
                element.elementType = source.getElementType(element.getData());
            }
        }
        List<Element> all = page.getElements();
        if (!all.isEmpty()) { // Replace elements with themselves, forcing a refresh.
            page.replaceElements(0, all.toArray(new Element[all.size()]));
        }
    }

    /**
     * Loads page 0. This means, among other things, asking sources for objects through
     * {@link ElementSource#find(Pager.Page)}.
     * If page 0 already has some objects for a certain source, they will be removed once
     * new objects from that source come.
     *
     * @return a Task that is completed once the page is loaded.
     */
    @UiThread
    public Task<Void> load() {
        return load(false);
    }

    /**
     * Loads page 0. This means, among other things, opening the page if needed and asking sources
     * for objects through {@link ElementSource#find(Pager.Page)}.
     * If {@code clearImmediately} is true, the page is cleared before asking for new objects.
     * If not, and the page already has some objects for a certain source, they will be removed
     * once new objects from that source come.
     *
     * @param clearImmediately wheter to immediately clear the page.
     * @return a Task that is completed once the page is loaded.
     */
    @UiThread
    public Task<Void> load(boolean clearImmediately) {
        return loadSinglePage(0, true, clearImmediately);
    }

    /**
     * Loads desired page. This means, among other things, opening the page if needed and asking
     * sources for objects through {@link ElementSource#find(Pager.Page)}.
     *
     * Objects belonging to other pages stay untouched.
     * Objects belonging to page {@code pageNumber}, if present, will be cleared as soon as new
     * objects arrive.
     *
     * @param pageNumber desired page.
     * @return a Task that is completed once the page is loaded.
     */
    @UiThread
    public Task<Void> loadSinglePage(int pageNumber) {
        return performFind(pageNumber, false, false);
    }

    /**
     * Loads desired page. This means, among other things, opening the page if needed and asking
     * sources for objects through {@link ElementSource#find(Pager.Page)}.
     *
     * Objects belonging to pages < {@code pageNumber} stay untouched.
     * Objects belonging to pages > {@code pageNumber}, are cleared if
     * {@code clearSubsequentPages} is true, after the requested page is loaded.
     * Objects belonging to page {@code pageNumber}, if present, will be cleared as soon as new
     * objects arrive.
     *
     * @param pageNumber desired page.
     * @return a Task that is completed once the page is loaded.
     */
    @UiThread
    public Task<Void> loadSinglePage(int pageNumber, boolean clearSubsequentPages) {
        return performFind(pageNumber, clearSubsequentPages, false);
    }

    /**
     * Loads desired page. This means, among other things, opening the page if needed and asking
     * sources for objects through {@link ElementSource#find(Pager.Page)}.
     *
     * Objects belonging to pages < {@code pageNumber} stay untouched.
     * Objects belonging to pages > {@code pageNumber} are cleared if
     * {@code clearSubsequentPages} is true, immediately if {@code immediately} is true, or after
     * the requested page is loaded.
     * Objects belonging to page {@code pageNumber}, if present, will be cleared as soon as new
     * objects arrive or immediately, depending on {@code immediately}.
     *
     * @param pageNumber desired page.
     * @return a Task that is completed once the page is loaded.
     */
    @UiThread
    public Task<Void> loadSinglePage(int pageNumber, boolean clearSubsequentPages, boolean immediately) {
        return performFind(pageNumber, clearSubsequentPages, immediately);
    }

    @UiThread
    private Task<Void> performFind(final int pageNumber, final boolean clearPagesAfter, final boolean immediately) {
        // Open page and clear if needed.
        final Pager.Page currentPage = pager.openPage(pageNumber, immediately);
        if (clearPagesAfter && immediately) {
            pager.clearPages(pageNumber, pager.getNumberOfPages());
        }

        // Call performGroupFind in sequence, for all groups of Sources.
        Task<Void> taskChain = Task.forResult(null);
        for (Set<Integer> group : groups) {
            final Set<Integer> finalGroup = group;
            taskChain = taskChain.onSuccessTask(new Continuation<Void, Task<Void>>() {
                @Override
                public Task<Void> then(Task<Void> task) throws Exception {
                    return performGroupFind(finalGroup, currentPage);
                }
            });
        }

        // Then dispatch onPageLoaded to sources.
        return taskChain.continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(Task<Void> task) throws Exception {
                boolean stillHere = currentPage.isCurrentPage();
                if (clearPagesAfter && !immediately && stillHere) {
                    pager.clearPages(pageNumber+1, pager.getNumberOfPages());
                }
                for (ElementSource source : allSources) {
                    source.onPageLoaded(currentPage, currentPage.getElements());
                }
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);
    }


    @WorkerThread
    private Task<Void> performGroupFind(Set<Integer> group, final Pager.Page page) {
        // Perform find operations concurrently on each Source in this group.
        List<Task<Void>> groupTasks = new ArrayList<>();
        for (final Integer sourceId : group) {
            // Get previous results.
            final ElementSource source = getSource(sourceId);
            // Call onPrepareFind with elements from this source dependencies Elements that, by
            // design, are already loaded.
            source.onPrepareFind(page, page.getElementsBySource(getDependencies(sourceId)));
            // Call find.
            Task<Void> task = source.find(page).continueWith(new Continuation<List<Object>, List<Object>>() {
                @Override
                public List<Object> then(Task<List<Object>> task) throws Exception {
                    // Let everyone redefine the result with onDependencyAfterFind.
                    task = source.onAfterFind(page, task);
                    Set<Integer> reverseDependencies = getReverseDependencies(sourceId);
                    for (int dependency : reverseDependencies) {
                        task = getSource(dependency).onDependencyAfterFind(page, source, task);
                    }
                    // If task is faulted, let source provide alternative objects.
                    if (task.isFaulted() || task.isCancelled()) {
                        List<Object> errorObjects = source.onFindError(page, task.getError());
                        if (errorObjects != null) {
                            return errorObjects;
                        }
                        throw new RuntimeException(task.getError());
                    }
                    return task.getResult();

                }
            }, Task.UI_THREAD_EXECUTOR).continueWithTask(new Continuation<List<Object>, Task<Void>>() {
                @Override
                public Task<Void> then(Task<List<Object>> task) throws Exception {
                    if (!task.isFaulted() && !task.isCancelled()) {
                        // Create Elements out of objects.
                        List<Object> list = task.getResult();
                        List<Element> result = new ArrayList<>(list.size());
                        for (Object o : list) {
                            // This sucks a little bit, but to now it is better than add typing
                            // to sources so they can create elements themselves..
                            result.add(createElement(source, o));
                        }
                        return page.setElementsForSource(sourceId, result);
                    } else {
                        log("performGroupFind: error: source find failed for source #"+sourceId+" with error "+task.getError());
                        throw new RuntimeException(task.getError());
                    }
                }
            }, Task.BACKGROUND_EXECUTOR);
            groupTasks.add(task);
        }

        // TODO If one of these fails for some reason, Task.whenAll returns early with the error.
        // But the other tasks keep going. This is bad because when Task.whenAll() returns,
        // onPageLoaded() is called. So in that case, for some sources, onPageLoaded() will be
        // called BEFORE ordering, for example.
        // That is unacceptable for sources clearing fields in onPageLoaded.
        return Task.whenAll(groupTasks);
    }

    @Override
    public int getItemCount() {
        return pager.getElementsCount();
        /* int count = pager.getElementsCount();
        if (count != lastCount) {
            lastCount = count;
            log("getItemCount: its " + lastCount);
        }
        return count; */
    }
    // debugging int lastCount = -1;


    @Override
    public int getItemViewType(int position) {
        return pager.getElementForPosition(position).getElementType();
    }

    @Override
    public ElementPresenter.Holder onCreateViewHolder(ViewGroup parent, int elementType) {
        // Find a reasonable ElementPresenter for this elementType. Try with cache map.
        log("onCreateViewHolder: called for type "+elementType);
        ElementPresenter presenter = elementTypeMap.get(elementType);
        if (presenter == null) {
            // Cycle to get reasonable presenter.
            for (ElementPresenter ep : presenters) {
                log("onCreateViewHolder: checking presenter "+ep);
                log("onCreateViewHolder: checking types are"+ep.getElementTypes());
                if (ep.getElementTypes().contains(elementType)) {
                    presenter = ep;
                    elementTypeMap.put(elementType, ep);
                    break;
                }
            }
        }
        if (presenter == null) throw new RuntimeException("No Presenter for this elementType: "+elementType);
        return presenter.instantiateHolder(parent, elementType);
    }

    @Override
    public void onBindViewHolder(ElementPresenter.Holder holder, int position) {
        log("onBindViewHolder: pos="+position+", type="+holder.getElementType());
        Pager.Page page = pager.getPageForPosition(position);
        Element element = page.getElementForAbsolutePosition(position);
        elementTypeMap.get(holder.getElementType()).onBind(page, holder, element);
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        if (!initialized) {
            // Auto load the first page.
            initialized = true;
            load();
        } else if (savedInstanceState != null) {
            // There is a savedInstanceState. Don't load, but restore from state.
            final Bundle state = savedInstanceState;
            savedInstanceState = null;
            // Pass this bundle to presenters.
            for (ElementPresenter presenter : presenters) {
                presenter.restoreState(state);
            }
            // Restore page count.
            pager.restoreState(state);

            // Restore everything else, one page at a time.
            final int pages = pager.getNumberOfPages();
            final int sources = allSources.size();
            for (int j = 0; j < pages; j++) {
                final Pager.Page page = pager.getPage(j);
                Task<Void> chain = Task.forResult(null);
                for (int i = 0; i < sources; i++) {
                    // Get source and id.
                    final ElementSource source = allSources.get(i);
                    final int sourceId = getId(source);
                    // Restore source stuff.
                    chain = chain.continueWithTask(new Continuation<Void, Task<List<Object>>>() {
                        @Override
                        public Task<List<Object>> then(Task<Void> task) throws Exception {
                            // Get source own bundle and restore page state.
                            String key = "source:"+source.getClass().getSimpleName()+":"+sourceId;
                            Bundle sourceBundle = state.getBundle(key);
                            if (sourceBundle == null) {
                                throw new RuntimeException("Invalid bundle for key: "+key);
                            }
                            return source.restorePageState(page, sourceBundle);

                        }
                    }, Task.BACKGROUND_EXECUTOR).onSuccessTask(new Continuation<List<Object>, Task<Void>>() {
                        @Override
                        public Task<Void> then(Task<List<Object>> task) throws Exception {
                            List<Object> objects = task.getResult();
                            final List<Element> elements = new ArrayList<>(objects.size());
                            log("restore: found " + objects.size() + " elements");
                            for (Object object : objects) {
                                elements.add(createElement(source, object));
                            }
                            return page.setElementsForSource(sourceId, elements);
                        }
                    });
                }

                // After restoring all sources for this page...
                chain.continueWith(new Continuation<Void, Void>() {
                    @Override
                    public Void then(Task<Void> task) throws Exception {
                        // Dispatch onPageLoaded() for this page.
                        for (int i = 0; i < sources; i++) {
                            ElementSource source = allSources.get(i);
                            source.onPageLoaded(page, page.getElements());
                        }
                        return null;
                    }
                }, Task.UI_THREAD_EXECUTOR);
            }
        }
    }

    /* package */ Set<Integer> getDependencies(int sourceId) {
        return dependencyMap.get(sourceId);
    }

    /* package */ Set<Integer> getReverseDependencies(int sourceId) {
        return reverseDependencyMap.get(sourceId);
    }

    /**
     * Save the adapter state into a non-null out bundle (e.g. from a Fragment or Activity callback).
     * ElementAdapter will:
     * - save state of presenters through {@link ElementPresenter#saveState(Bundle)}
     * - save state of pages through {@link ElementSource#savePageState(Pager.Page, List, Bundle)}
     * Each source is passed a unique Bundle so there is no risk of overriding.
     *
     * @see ElementSource#savePageState(Pager.Page, List, Bundle)
     * @param outState an out Bundle
     */
    public void saveState(@NonNull Bundle outState) {
        outState.putBoolean("initialized", initialized);
        log("saveState: initialized is "+initialized);
        if (!initialized) return;

        // Pass this bundle to presenters.
        for (ElementPresenter presenter : presenters) {
            presenter.saveState(outState);
        }

        // Pass this bundle to pager.
        pager.saveState(outState);

        // Create a new bundle for each source.
        for (int i = 0; i < allSources.size(); i++) {
            ElementSource source = allSources.get(i);
            int sourceId = getId(source);
            Bundle sourceBundle = new Bundle();

            int count = pager.getNumberOfPages();
            for (int j = 0; j < count; j++) {
                Pager.Page page = pager.getPage(j);
                List<Object> list = page.getDataBySource(sourceId);
                log("saveState: saving "+list.size()+" elements.");
                source.savePageState(page, list, sourceBundle);
            }

            // This assumes that allSources will have the same order later.
            String key = "source:"+source.getClass().getSimpleName()+":"+sourceId;
            outState.putBundle(key, sourceBundle);
        }
    }

    /**
     * Restores the state previously saved through {@link #saveState(Bundle)}, if the
     * bundle is not null.
     *
     * Internally the state will be restored once {@code ElementAdapter} is attached again to a
     * {@code RecyclerView}.
     * @param savedInstanceState a nullable bundle.
     */
    public void restoreState(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            initialized = savedInstanceState.getBoolean("initialized", false);
            log("sync: initialized is " + initialized);
            if (!initialized) return;
            // We must defer restoration to the moment the RecyclerView comes.
            this.savedInstanceState = savedInstanceState;
        }
    }
}
