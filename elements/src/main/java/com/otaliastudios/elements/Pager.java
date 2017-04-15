package com.otaliastudios.elements;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;
import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;


/**
 * Base class for managing pages.
 * TODO option to remove a page.
 */
public class Pager {

    private final static String TAG = Pager.class.getSimpleName();
    private final static boolean DEBUG = false;
    private static void log(String what) {
        if (DEBUG) Log.e(TAG, what);
    }

    private List<Page> pages = new ArrayList<>();
    private Page currentPage;
    private ElementAdapter adapter;
    private int countCache = -1;

    /* package */ Pager(ElementAdapter adapter) {
        this.adapter = adapter;
    }

    /* package */ Page getCurrentPage() {
        return currentPage;
    }

    // Throws if something is wrong. Called many times.
    /* package */ Element getElementForPosition(int position) {
        return getPageForPosition(position).getElementForAbsolutePosition(position);
    }

    /* package */ Page getPageForPosition(int position) {
        log("getPageForPosition: "+position);
        for (Page page : pages) {
            log("getPageForPosition: found page at "+page.getPageNumber() + " with count "+page.getElementsCount()+" and elements before "+page.elementsBefore);
            if (position >= page.elementsBefore &&
                    position < page.elementsBefore + page.getElementsCount()) {
                return page;
            }
        }
        log("getPageForPosition: returning null");
        return null;
    }

    @UiThread
    /* package */ Page openPage(int number, boolean clearContent) {
        log("openPage: called for position "+number+" clearing "+clearContent);
        int objectsBefore = 0;
        for (int i = 0; i < number; i++) {
            objectsBefore += pages.get(i).getElementsCount();
        }
        if (number >= pages.size()) {
            // Need to create a new one.
            currentPage = new Page(number, objectsBefore);
            pages.add(number, currentPage);
        } else if (clearContent) {
            // We want an already present, but with cleared content.
            clearPage(number);
            currentPage = new Page(number, objectsBefore);
            pages.set(number, currentPage);
        } else {
            // We just want the current page. it'll be erased as soon as stuff comes.
            currentPage = pages.get(number);
        }
        return currentPage;
    }


    /* package */ int getNumberOfPages() {
        return pages.size();
    }

    /* package */ Page getPage(int number) {
        return pages.get(number);
    }

    /* package */ int getElementsCount() {
        if (countCache != -1) {
            return countCache;
        } else {
            int count = 0;
            for (Page page : pages) {
                count += page.getElementsCount();
            }
            countCache = count;
            return count;
        }
    }

    // Called by pages to update the count cache.
    private void updateCountCache(int pageNumber, int delta) {
        if (countCache != -1) {
            countCache += delta;
            log("updateCountCache: to "+countCache);
        } else {
            // Makes no sense to update an invalid count cache. Recompute.
            getElementsCount();
            log("updateCountCache: recounted to "+countCache);
        }

        // Update elementsBefore of subsequent pages.
        int pageCount = getNumberOfPages();
        if (pageNumber == pageCount - 1) return;
        for (int i = pageNumber + 1; i < pageCount; i++) {
            getPage(i).elementsBefore += delta;
        }
    }


    @UiThread
    /* package */ void clearPage(int number) {
        if (number < pages.size()) {
            pages.get(number).clear();
        }
    }

    @UiThread
    /* package */ void clearPages(int fromNumber, int toNumber) {
        for (int i = fromNumber; i < toNumber; i++) {
            clearPage(i);
        }
    }

    /* package */ void saveState(Bundle outState) {
        outState.putInt("numberOfPages", pages.size());
        outState.putInt("currentPageNumber", currentPage.getPageNumber());
    }

    /* package */ void restoreState(Bundle savedInstanceState) {
        int numberOfPages = savedInstanceState.getInt("numberOfPages");
        int currentPageNumber = savedInstanceState.getInt("currentPageNumber");
        for (int i = 0; i < numberOfPages; i++) {
            Page page = openPage(i, false);
            if (i == currentPageNumber) currentPage = page;
        }
    }

    // Public facing class.
    // TODO implement an isEditable boolean, so some method throw when !isEditable
    public class Page {

        private int number;
        private int elementsBefore;
        private List<Element> elements;
        private final Object mutex = new Object();


        private Page(int number, int elementsBefore) {
            this.number = number;
            this.elementsBefore = elementsBefore;
            this.elements = new ArrayList<>();
        }

        /**
         * Returns the current count of objects in this page.
         * @return count
         */
        public int getElementsCount() {
            return elements.size();
        }

        /**
         * Asks the adapter to open and load the next page.
         * @return a task that is completed once the operation ends.
         */
        @UiThread
        public Task<Void> loadNextPage() {
            return adapter.loadSinglePage(number+1);
        }

        /**
         * Returns this page number.
         * @return the number
         */
        public int getPageNumber() {
            return number;
        }

        /**
         * True if this is the current page, that is, the last to be opened.
         * @return tue if current page
         */
        public boolean isCurrentPage() {
            return getCurrentPage().getPageNumber() == getPageNumber();
        }

        /**
         * Clear this page from any object currently present.
         */
        @UiThread
        public void clear() {
            synchronized (mutex) {
                int count = this.elements.size();
                this.elements.clear();
                notifyPageItemRangeRemoved(0, count);
            }
        }

        /**
         * Insert an object at the specified position in this page.
         * An element will be created.
         * @param position position in this page, 0 based
         * @param source the element source this data belogns to
         * @param data object to be inserted
         *
         */
        @UiThread
        public Element insertElement(int position, ElementSource source, Object data) {
            synchronized (mutex) {
                if (position >= 0 && position <= this.elements.size()) {
                    Element element = adapter.createElement(source, data);
                    this.elements.add(position, element);
                    notifyPageItemInserted(position);
                    return element;
                }
                return null;
            }
        }

        /**
         * Insert an element at the specified position in this page.
         * @param position position in this page, 0 based
         * @param element element to be inserted
         */
        @UiThread
        public void insertElement(int position, Element element) {
            synchronized (mutex) {
                if (position >= 0 && position <= this.elements.size()) {
                    this.elements.add(position, element);
                    notifyPageItemInserted(position);
                }
            }
        }

        @UiThread
        private void notifyPageItemInserted(int position) {
            updateCountCache(number, 1);
            log("notifyPageItemInserted: pos="+position);
            adapter.notifyItemInserted(elementsBefore + position);
        }

        /**
         * Remove an element at the specified position in this page.
         * @param position position in this page, 0 based
         */
        @UiThread
        public void removeElement(int position) {
            synchronized (mutex) {
                if (position >= 0 && position < this.elements.size()) {
                    this.elements.remove(position);
                    notifyPageItemRemoved(position);
                }
            }
        }

        /**
         * Remove the specified element from this page, if present.
         * @param element element to be removed
         */
        @UiThread
        public void removeElement(Element element) {
            int position = this.elements.indexOf(element);
            if (position != -1) {
                removeElement(position);
            }
        }

        @UiThread
        private void notifyPageItemRemoved(int position) {
            updateCountCache(number, -1);
            log("notifyPageItemRemoved: pos="+position);
            adapter.notifyItemRemoved(elementsBefore + position);
        }

        /**
         * Replaces the specified element with another.
         * @param item element to be replaced
         * @param withItem replacement
         */
        @UiThread
        public void replaceElement(Element item, Element withItem) {
            synchronized (mutex) {
                int position = this.elements.indexOf(item);
                if (position != -1) {
                    this.elements.set(position, withItem);
                    notifyPageItemChanged(position);
                }
            }
        }

        @UiThread
        private void notifyPageItemChanged(int position) {
            log("notifyPageItemChanged: pos="+position);
            adapter.notifyItemChanged(elementsBefore + position);
        }

        /**
         * Inserts the specified elements in this page, starting at position {@code position},
         * relative to this page.
         * @param position position
         * @param elements collection of elements
         */
        @UiThread
        public void insertElements(int position, Collection<Element> elements) {
            synchronized (mutex) {
                if (position >= 0 && position <= this.elements.size()) {
                    int count = elements.size();
                    int offset = 0;
                    for (Element e : elements) {
                        this.elements.add(position + offset, e);
                        offset += 1;
                    }
                    if (count > 0) {
                        notifyPageItemRangeInserted(position, count);
                    }
                }
            }
        }

        @UiThread
        private void notifyPageItemRangeInserted(int positionStart, int count) {
            updateCountCache(number, count);
            log("notifyPageItemRangeInserted: pos="+positionStart+" count="+count);
            adapter.notifyItemRangeInserted(elementsBefore + positionStart, count);
        }

        /**
         * Replaces elements in this page in the range {@code position} ->
         * {@code position + elements.length} with the specified elements.
         *
         * @param position starting position
         * @param elements collection of elements
         */
        @UiThread
        public void replaceElements(int position, Element... elements) {
            synchronized (mutex) {
                if (position >= 0 && position + elements.length <= this.elements.size()) {
                    int count = elements.length;
                    int offset = 0;
                    for (Element e : elements) {
                        this.elements.set(position + offset, e);
                        offset += 1;
                    }
                    if (count > 0) {
                        notifyPageItemRangeChanged(position, count);
                    }
                }
            }
        }

        @UiThread
        private void notifyPageItemRangeChanged(int positionStart, int count) {
            log("notifyPageItemRangeChanged: pos="+positionStart+" count="+count);
            adapter.notifyItemRangeChanged(elementsBefore + positionStart, count);
        }

        /**
         * Removes up to {@code count} elements starting at position {@code position}.
         * @param position starting position
         * @param count elements count
         */
        @UiThread
        public void removeElements(int position, int count) {
            synchronized (mutex) {
                if (position >= 0 && position + count < this.elements.size()) {
                    int c = 0;
                    for (int i = position; i < position + count; i++) {
                        this.elements.remove(i);
                        c += 1;
                    }
                    if (c > 0) {
                        notifyPageItemRangeRemoved(position, count);
                    }
                }
            }
        }

        @UiThread
        private void notifyPageItemRangeRemoved(int positionStart, int count) {
            updateCountCache(number, -count);
            log("notifyPageItemRangeRemoved: pos="+positionStart+" count="+count);
            adapter.notifyItemRangeRemoved(elementsBefore + positionStart, count);
        }

        /**
         * Returns a source instance the originated this element.
         * @param element query
         * @return the ElementSource
         */
        public ElementSource getSourceForElement(Element element) {
            return adapter.getSource(element.sourceId);
        }

        /* package */ Element getElement(int position) {
            return elements.get(position);
        }


        /* package */ Element getElementForAbsolutePosition(int position) {
            return getElement(position-elementsBefore);
        }

        /* package */ List<Element> getElements() { return elements; };

        /* package */ List<Element> getElementsBySource(int sourceId) {
            return getElementsBySource(Arrays.asList(sourceId));
        }

        @NonNull
        /* package */ List<Element> getElementsBySource(Collection<Integer> sourceSet) {
            synchronized (mutex) {
                List<Element> elems = new ArrayList<>();
                for (Element o : elements) {
                    if (sourceSet.contains(o.sourceId)) {
                        elems.add(o);
                    }
                }
                return elems;
            }
        }

        /* package */ List<Object> getDataBySource(int sourceId) {
            return getDataBySource(Arrays.asList(sourceId));
        }

        @NonNull
        /* package */ List<Object> getDataBySource(Collection<Integer> sourceSet) {
            synchronized (mutex) {
                List<Object> data = new ArrayList<>();
                for (Element o : elements) {
                    if (sourceSet.contains(o.sourceId)) {
                        data.add(o.getData());
                    }
                }
                return data;
            }
        }

        @WorkerThread
        /* package */ Task<Void> setElementsForSource(final int sourceId, final List<Element> newElements) {
            // Remove all other objects from the same source. This must happen in the UI thread.
            return Task.call(new Callable<Set<Integer>>() {
                @Override
                public Set<Integer> call() throws Exception {

                    // Remove all other objects from the same source. This must happen in UI.
                    List<Element> oldData = getElementsBySource(sourceId);
                    log("setElementsForSource: removing "+oldData.size()+" previous elements from same source");
                    for (Element element : oldData) {
                        removeElement(element);
                    }

                    // Quick ending if we have no dependencies.
                    Set<Integer> dependencies = adapter.getDependencies(sourceId);
                    if (dependencies.isEmpty()) {
                        // Add without logic, appending.
                        log("setElementsForSource: about to directly add "+newElements.size()+" elements");
                        int basePosition = elements.size();
                        insertElements(basePosition, newElements);
                        return null;
                    }
                    return dependencies;
                }
            }, Task.UI_THREAD_EXECUTOR).onSuccess(new Continuation<Set<Integer>, List<Pair<Integer, Integer>>>() {
                @Override
                public List<Pair<Integer, Integer>> then(Task<Set<Integer>> task) throws Exception {
                    if (task.getResult() == null) return null;

                    // This source has been found. The sources it depends on will be already here.
                    final ElementSource source = adapter.getSource(sourceId);
                    List<Element> dependenciesElements = getElementsBySource(task.getResult());
                    source.onPrepareOrder(Page.this, newElements, dependenciesElements);
                    // ??? not sure it works. indexes are considered as if the adapter was already updated.
                    // ^ It appears to be working.
                    List<Pair<Integer, Integer>> rangeUpdates = new ArrayList<>();
                    log("setElementsForSource: found "+newElements.size()+" elements.");
                    synchronized (mutex) {
                        int numInserted = 0;
                        int numRemaining;
                        for (int i = 0; i < dependenciesElements.size(); i++) {
                            int before = source.orderBefore(Page.this, i, dependenciesElements.get(i));
                            int after = source.orderAfter(Page.this, i, dependenciesElements.get(i));
                            int pageIndex = elements.indexOf(dependenciesElements.get(i));

                            // Now pageIndex is the index of the dependency object in the page.
                            numRemaining = newElements.size() - numInserted;
                            if (numRemaining == 0) break;
                            before = Math.min(before, numRemaining);
                            for (int j = 0; j < before; j++) {
                                // LOG.now("elements: adding element at index "+pageIndex);
                                elements.add(pageIndex, newElements.get(numInserted));
                                pageIndex += 1;
                                numInserted += 1;
                            }
                            if (before > 0) {
                                rangeUpdates.add(new Pair<>(pageIndex - before, before));
                            }

                            // Now pageIndex is STILL the index of the dependency object in the page.
                            numRemaining = newElements.size() - numInserted;
                            if (numRemaining == 0) break;
                            after = Math.min(after, numRemaining);
                            pageIndex += 1; // To put after.
                            for (int j = 0; j < after; j++) {
                                // LOG.now("elements: adding element at index "+pageIndex);
                                elements.add(pageIndex, newElements.get(numInserted));
                                pageIndex += 1;
                                numInserted += 1;
                            }
                            if (after > 0) {
                                rangeUpdates.add(new Pair<>(pageIndex - after, after));
                            }
                        }
                    }
                    log("setElementsForSource: returning updates with "+rangeUpdates.size()+" elements.");
                    return rangeUpdates;
                }
            }, Task.BACKGROUND_EXECUTOR).onSuccess(new Continuation<List<Pair<Integer,Integer>>, Void>() {
                @Override
                public Void then(Task<List<Pair<Integer, Integer>>> task) throws Exception {
                    List<Pair<Integer, Integer>> updates = task.getResult();
                    if (updates == null) return null;
                    for (Pair<Integer, Integer> p : updates) {
                        if (p.second == 1) {
                            notifyPageItemInserted(p.first);
                        } else {
                            notifyPageItemRangeInserted(p.first, p.second);
                        }
                    }
                    return null;
                }
            }, Task.UI_THREAD_EXECUTOR);
        }
    }
}
