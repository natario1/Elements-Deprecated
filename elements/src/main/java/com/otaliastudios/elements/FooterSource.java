package com.otaliastudios.elements;


import android.util.SparseArray;

import java.util.ArrayList;
import java.util.List;

import bolts.Task;

/**
 * Like {@link HeaderSource}, but uses orderAfter instead of orderBefore.
 * This means that items returned here will be ordered below the anchors, not above.
 *
 * @param <AnchorData> type of objects returned by the main source
 * @param <FooterData> type of objects representing a footer.
 */
public abstract class FooterSource<AnchorData, FooterData> extends ElementSource {

    public final static int HEADER_TYPE = -6;

    @Override
    protected int getElementType(Object data) {
        return HEADER_TYPE;
    }

    private Class<? extends ElementSource> typeClass;
    private SparseArray<List<FooterData>> footerData;
    private SparseArray<List<AnchorData>> anchorData;

    public FooterSource(Class<? extends ElementSource> typeClass) {
        this.typeClass = typeClass;
    }

    @Override
    protected final boolean dependsOn(ElementSource other) {
        return typeClass.isInstance(other);
    }

    @Override
    protected final void onPrepareFind(Pager.Page page, List<Element> dependenciesElements) {
        super.onPrepareFind(page, dependenciesElements);
        initializeData(page, dependenciesElements);
    }

    private void initializeData(Pager.Page page, List<Element> dependenciesElements) {
        List<AnchorData> list = getAnchors(dependenciesElements);
        List<FooterData> headerList = new ArrayList<>();
        for (AnchorData in : list) {
            FooterData data = getFooterForAnchor(in);
            headerList.add(data);
        }
        if (anchorData == null) anchorData = new SparseArray<>();
        if (footerData == null) footerData = new SparseArray<>();
        anchorData.put(page.getPageNumber(), list);
        footerData.put(page.getPageNumber(), headerList);
    }

    @Override
    protected Task<List<Object>> find(Pager.Page page) {
        return Task.forResult(footerData.get(page.getPageNumber())).cast();
    }

    @Override
    protected Task<List<Object>> onAfterFind(Pager.Page page, Task<List<Object>> task) {
        footerData.remove(page.getPageNumber());
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
    protected int orderAfter(Pager.Page page, int position, Element dependencyElement) {
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
     * This means selecting the elements which should have a footer.
     *
     * @param dependenciesElements a list of elements returned by the source we depend on
     * @return a list of anchors
     */
    protected abstract List<AnchorData> getAnchors(List<Element> dependenciesElements);

    /**
     * Creates, out of anchor data returned by {@link #getAnchors(List)}, a {@code <FooterData>}
     * instance. Can be whatever is needed, e.g. a String or Date.
     * The presenter will later convert header data to UI.
     *
     * @param anchorData an element that was designed to be topped by an header
     * @return the header model data
     */
    protected abstract FooterData getFooterForAnchor(AnchorData anchorData);
}
