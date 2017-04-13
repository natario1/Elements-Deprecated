package com.otaliastudios.elements;


import android.util.SparseArray;

import java.util.ArrayList;
import java.util.List;

import bolts.Task;

/**
 * A simple {@code ElementSource} for displaying 'headers' on top of 'anchors'.
 * This is meant to be used as a secondary source, that is, acting after another source (the anchors
 * source) has found its elements.
 *
 * Model wise, a 'header' is an object (such as a {@link java.util.Date}) that must be anchored
 * to another object representing actual data, which is its 'anchor'.
 * For instance, you might have a list of objects provided by the root source, and you might want
 * to add a header date to some of them, or you might have a list of alphabetically sorted objects,
 * and might want to add a header letter.
 *
 * This class will care about ordering, and requires two methods to be implemented:
 * - {@link #getAnchors(List)}: asks to select, among the dependency objects, the actual anchors
 *   that will be 'topped' by our header.
 * - {@link #getHeaderForAnchor(Object)}: constructs header data (such as a {@link java.util.Date})
 *   out of the object to be anchored. A typical implementation might be
 *       {@code return anchor.getCreationDate()}
 * This source find() operation returns a list of {@link HeaderData} objects, and these will
 * eventually be converted to their UI representation by the presenter you will provide.
 *
 * @param <AnchorData> type of objects returned by the main source
 * @param <HeaderData> type of objects representing a header.
 */
public abstract class HeaderSource<AnchorData, HeaderData> extends ElementSource {

    public final static int HEADER_TYPE = -4;

    @Override
    protected int getElementType(Object data) {
        return HEADER_TYPE;
    }

    private Class<? extends ElementSource> typeClass;
    private SparseArray<List<HeaderData>> headerData;
    private SparseArray<List<AnchorData>> anchorData;

    public HeaderSource(Class<? extends ElementSource> typeClass) {
        this.typeClass = typeClass;
    }

    @Override
    protected boolean dependsOn(ElementSource other) {
        return typeClass.isInstance(other);
    }

    @Override
    protected final void onPrepareFind(Pager.Page page, List<Element> dependenciesElements) {
        super.onPrepareFind(page, dependenciesElements);
        initializeData(page, dependenciesElements);
    }

    private void initializeData(Pager.Page page, List<Element> dependenciesElements) {
        List<AnchorData> list = getAnchors(dependenciesElements);
        List<HeaderData> headerList = new ArrayList<>();
        for (AnchorData in : list) {
            HeaderData data = getHeaderForAnchor(in);
            headerList.add(data);
        }
        if (anchorData == null) anchorData = new SparseArray<>();
        if (headerData == null) headerData = new SparseArray<>();
        anchorData.put(page.getPageNumber(), list);
        headerData.put(page.getPageNumber(), headerList);
    }

    @Override
    protected Task<List<Object>> find(Pager.Page page) {
        return Task.forResult(headerData.get(page.getPageNumber())).cast();
    }

    @Override
    protected Task<List<Object>> onAfterFind(Pager.Page page, Task<List<Object>> task) {
        headerData.remove(page.getPageNumber());
        return super.onAfterFind(page, task);
    }

    @Override
    protected void onPrepareOrder(Pager.Page page, List<Element> elements, List<Element> dependenciesElements) {
        super.onPrepareOrder(page, elements, dependenciesElements);
        if (anchorData == null) {
            // This might be a configuration change. reinitialize.
            initializeData(page, dependenciesElements);
        }
    }

    @Override
    protected int orderBefore(Pager.Page page, int position, Element dependencyElement) {
        Object data = dependencyElement.getData();
        return anchorData.get(page.getPageNumber()).contains(data) ? 1 : 0;
    }

    @Override
    protected void onPageLoaded(Pager.Page page, List<Element> pageElements) {
        super.onPageLoaded(page, pageElements);
        anchorData.remove(page.getPageNumber());
    }

    /**
     * Construct a list of anchors from the whole dependencies elements list.
     * This means selecting the elements which should be topped by a header.
     * You can implement complex logic here, e.g. for a sorted list of contacts, selecting the ones
     * that will be topped by alphabet letters.
     *
     * @param dependenciesElements a list of elements returned by the source we depend on
     * @return a list of anchors
     */
    protected abstract List<AnchorData> getAnchors(List<Element> dependenciesElements);

    /**
     * Creates, out of anchor data returned by {@link #getAnchors(List)}, a {@code <HeaderData>}
     * instance. Can be whatever is needed, e.g. a String or Date.
     * The presenter will later convert header data to UI.
     *
     * @param anchorData an element that was designed to be topped by an header
     * @return the header model data
     */
    protected abstract HeaderData getHeaderForAnchor(AnchorData anchorData);
}
