package com.otaliastudios.elements;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;

import java.util.ArrayList;
import java.util.List;

import bolts.Task;

/**
 * The base component required to asynchronously fetch the data to be modelled and then laid out
 * by {@link ElementPresenter}.
 *
 * Dependencies:
 * Sources can declare to be dependent on another source by returning true in
 * {@link #dependsOn(ElementSource)}, in a 'CoordinatorLayout.Behavior' fashion.
 * This has consequences explained below, and should be used when this source wants to alter its
 * *ordering* or *find* behavior based on other sources behavior.
 *
 * Find behavior:
 * The callbacks go as follows.
 * - {@link #onDependencyAfterFind(Pager.Page, ElementSource, Task)}: notifies that a source we
 *   depend on, has just finished its find task. At this point we are given the option to alter
 *   the other source results. This is possible because find tasks are always executed such that
 *   dependent sources start later.
 * - {@link #onPrepareFind(Pager.Page, List)}: let this source prepare the find call. If this depends
 *   on other sources, it is passed a list of all the dependent sources results for the same page.
 * - {@link #find(Pager.Page)}: simply asks this source for its elements, in a asynchronous action
 *   embedded in a Task.
 * - {@link #onAfterFind(Pager.Page, Task)}: called right after find completes. This means that we
 *   can query the passed task because it is completed. It is legit to alter our own results here.
 *   Note that after this ends, other sources that depend on this source will also be able to
 *   alter our results through their own {@link #onDependencyAfterFind(Pager.Page, ElementSource, Task)}.
 * - {@link #onFindError(Pager.Page, Exception)}: called if, after our own find, our own onAfterFind
 *   and after any alteration done by other sources, the task is not succesful.
 *   We can provide a replacement (e.g. an error string) here.
 *
 * Ordering behavior:
 * The default behavior is that elements are added to the page in the order they come.
 * For example, if you have two sources, their find() methods will be called concurrently. The one
 * who returns first, will have the objects added first to the recycler.
 * Things can get trickier if we declare some dependency: in that case, we have a chance to wait for
 * other sources elements to be laid out, and order our elements with a relative logic:
 * - {@link #onPrepareOrder(Pager.Page, List, List)} lets this source prepare the order call, based
 *   on the list of dependent sources elements (that are already ordered).
 * - {@link #orderBefore(Pager.Page, int, Element)} and {@link #orderAfter(Pager.Page, int, Element)}
 *   let this source order 0+ items right before/after dependency elements.
 *   These callbacks let you think in terms of an imaginary page made just of this source objects,
 *   and this source dependencies objects. It works.
 *
 * Presentation:
 * You can choose which {@link ElementPresenter} should present data by returning different element
 * types in {@link #getElementType(Object)}. This can become complex as you wish. The default value,
 * 0, is accepted by the default {@link ElementPresenter}.
 *
 * Element restoration:
 * {@code ElementSource}s are responsible of providing a {@link ElementSerializer}
 * instance for state save / restoration of elements. You can return null, but it is mandatory for
 * a functional adapter. It is as simple as implementing Parcelable in your model data, and using
 * {@link ParcelableSerializer}. If that's not possible, you can use {@link StaticSerializer} or
 * {@link SerializableSerializer} at your own risk.
 *
 * Inner state restoration:
 * {@code ElementSource}s should *not* be stateful in this sense. There is conceptually
 * no reason for a source to hold members that need to be restored. However,
 * {@link #onSavePageState(Pager.Page, List, Bundle)} and {@link #onPageStateRestored(Pager.Page, List, Bundle)}
 * callbacks are defined.
 */
public abstract class ElementSource {

    public ElementSource() {}

    // Save - restore stuff.

    private ElementSerializer serializer;

    private ElementSerializer getSerializer() {
        if (serializer == null) serializer = instantiateSerializer();
        return serializer;
    }

    /* package */ final void savePageState(Pager.Page page, List<Object> elements, Bundle outState) {
        onSavePageState(page, elements, outState);
        if (getSerializer() != null) {
            getSerializer().saveElements(page, this, elements, outState);
        }
    }

    @WorkerThread
    /* package */ final Task<List<Object>> restorePageState(Pager.Page page, Bundle outState) {
        if (getSerializer() != null) {
            List<Object> state = getSerializer().restoreElements(page, this, outState);
            onPageStateRestored(page, state, outState);
            return Task.forResult(state);
        }
        return Task.<List<Object>>forResult(new ArrayList<>());
    }

    /**
     * Returns a {@link ElementSerializer} implementation, to save and restore state.
     *
     * @see ElementSerializer
     * @see StaticSerializer
     * @see ParcelableSerializer
     * @return the serializer used to save state.
     */
    protected abstract ElementSerializer instantiateSerializer();

    /**
     * Called right before the page state is going to be saved by this source serializer.
     * This might be removed in the future, since there's no reason why a proper source should
     * receive this callback...
     *
     * @param page the page currently being saved
     * @param elements the (editable) list of elements that will be saved
     * @param outState the Bundle used to save state
     */
    protected void onSavePageState(Pager.Page page, List<Object> elements, Bundle outState) {
    }

    /**
     * Called right after the page state has been restored by this source serializer.
     * This can be used to recover particular states that depend on the elements list.
     * (when restoring, {@link #onAfterFind(Pager.Page, Task)} and similar callbacks are not called).
     *
     * @param page the page currently being restored
     * @param restoredElements the list of elements that were restored
     * @param outState the Bundle used to save state
     */
    @WorkerThread
    protected void onPageStateRestored(Pager.Page page, List<Object> restoredElements, Bundle outState) {
    }

    // Depends

    /**
     * True if the order of these elements depends on the order of elements provided by {@code other},
     * or if this source wants to be executed after {@code other}, and receive appropriate callbacks.
     *
     * If this returns true for some {@code other} source,
     * - {@link #onDependencyAfterFind(Pager.Page, ElementSource, Task)} is called right after the
     *   dependency has completed its find task
     * - {@link #find(Pager.Page)} is called right after
     * - {@link #onPrepareOrder(Pager.Page, List, List)} is called to let you set up the ordering strategy
     * - {@link #orderBefore(Pager.Page, int, Element)} and {@link #orderAfter(Pager.Page, int, Element)}
     *   are called until the number of elements is over
     *
     * @param other the other source to be checked
     * @return true if this source depends on other
     */
    protected abstract boolean dependsOn(ElementSource other);

    // Find

    /**
     * Notifies that a source we depend on has just finished its find task.
     * At this point we are given the change to modify that source results, by returning a different
     * or altered task.
     *
     * The returned task should be completed, e.g., the same {@code task}, or one constructed with
     * {@link Task#forResult(Object)}, {@link Task#forError(Exception)} or similar.
     *
     * @param page the current page
     * @param source the other source we depend on.
     * @param task a completed task as returned by the other source's find()
     * @return a completed task with the dependency object
     */
    @UiThread
    protected Task<List<Object>> onDependencyAfterFind(Pager.Page page, ElementSource source, Task<List<Object>> task) {
        return task;
    }

    /**
     * Called just before {@link #find(Pager.Page)}.
     * This view is passed all of its dependencies elements for the same page, if this matters
     * for the find call.
     *
     * @param page the current page
     * @param dependenciesElements already fetched elements for dependencies for the same page.
     */
    @WorkerThread
    protected void onPrepareFind(Pager.Page page, List<Element> dependenciesElements) {}

    /**
     * A task returning the objects to display. These will be later transformed into
     * {@link Element} instances.
     *
     * @param page the current page
     * @return a Task for the find operation
     */
    @WorkerThread
    protected abstract Task<List<Object>> find(Pager.Page page);

    /**
     * Called right after {@link #find(Pager.Page)} for this page, when the find task has
     * ended and the objects can be queried through the passed task.
     * It's legit to alter results here by returning an altered or different task.
     *
     * The returned task should be completed, e.g., the same {@code task}, or one constructed with
     * {@link Task#forResult(Object)}, {@link Task#forError(Exception)} or similar.
     *
     * Note that after this ends, the task will be passed to any other source that depends
     * on this source, and they will be able to alter our results as well.
     *
     * @param page the current page
     * @param task a completed task with our own objects
     * @return a completed task with our own objects
     */
    @UiThread
    protected Task<List<Object>> onAfterFind(Pager.Page page, Task<List<Object>> task) {
        return task;
    }

    /**
     * Something went wrong during {@link #find(Pager.Page)}, {@link #onAfterFind(Pager.Page, Task)},
     * or when other sources were given the change to alter our own task.
     * A null exception means that there was no error, but task was cancelled.
     *
     * We can provide a replacement list of data.
     * @param page the current page
     * @param exception exception, if task was faulted, null if task was cancelled
     * @return replacement data or null
     */
    @UiThread
    protected List<Object> onFindError(Pager.Page page, @Nullable Exception exception) {
        return null;
    }

    // Order

    /**
     * Called just before {@link #orderBefore(Pager.Page, int, Element)} and
     * {@link #orderAfter(Pager.Page, int, Element)}.
     * Useful for ordering patterns that require access to the whole list of elements.
     *
     * @param page the current page.
     * @param elements elements obtained via find().
     * @param dependenciesElements elements of dependencies, obtained via their find().
     */
    @WorkerThread
    protected void onPrepareOrder(Pager.Page page, List<Element> elements, List<Element> dependenciesElements) {}

    /**
     * orderBefore and orderAfter are called after this source has found its elements.
     * They are called only if order matters for this source, that is, if it dependsOn() some other
     * source.
     *
     * The position is referred to an imaginary page composed of only this source elements and
     * this source dependencies elements. If this source depends on everything, the imaginary page
     * corresponds to the whole page.
     *
     * @param page the current page.
     * @param position the position of dependencyElement in the described page.
     * @param dependencyElement the element to lay out before.
     * @return the number of elements to display *before* dependencyElement.
     */
    @WorkerThread
    protected int orderBefore(Pager.Page page, int position, Element dependencyElement) { return 0; }

    /**
     * orderBefore and orderAfter are called after this source has found its elements.
     * They are called only if order matters for this source, that is, if it dependsOn() some other
     * source.
     *
     * The position is referred to an imaginary page composed of only this source elements and
     * this source dependencies elements. If this source depends on everything, the imaginary page
     * corresponds to the whole page.
     *
     * @param page the current page.
     * @param position the position of dependencyElement in the described page.
     * @param dependencyElement the element to lay out before.
     * @return the number of elements to display *after* dependencyElement.
     */
    @WorkerThread
    protected int orderAfter(Pager.Page page, int position, Element dependencyElement) { return 0; }

    // Other stuff

    /**
     * This is called after find and ordering process have ended, not only for this source but for
     * every source registered. All items have been laid out (or, at least, the adapter has been
     * notified of their presence).
     *
     * @param page the current page
     * @param pageElements all the page elements
     */
    @UiThread
    protected void onPageLoaded(Pager.Page page, List<Element> pageElements) { }

    /**
     * Lets you send an integer (like a view type) to the {@link ElementPresenter} when binding.
     * This means that this objects will be laid out by a presenters who declares to accept
     * this element type.
     *
     * @param data Object as returned by find()
     * @return an element type describing this object, if you wish.
     */
    protected int getElementType(Object data) {
        return 0;
    }

    /**
     * Convenience method that creates a new {@link SourceMonitor} source that can be used to
     * monitor this source. The Source returned here can be added to the components list
     * of the ElementAdapter.
     * @see SourceMonitor
     *
     * @param callback the callbacks you will receive about this Source
     * @param <T> the Source type class
     * @return a Monitor instance
     */
    public <T extends ElementSource> SourceMonitor<T> getMonitor(SourceMonitor.Callback<T> callback) {
        //noinspection unchecked
        return new SourceMonitor(getClass(), callback);
    }
}
