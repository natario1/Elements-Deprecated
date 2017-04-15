package com.otaliastudios.sample;

import com.otaliastudios.elements.Element;
import com.otaliastudios.elements.ElementSerializer;
import com.otaliastudios.elements.ElementSource;
import com.otaliastudios.elements.Pager;
import com.otaliastudios.elements.StringSerializer;

import java.util.ArrayList;
import java.util.List;

import bolts.Task;


public class AdsSource extends ElementSource {

    public AdsSource() {
    }

    @Override
    protected boolean dependsOn(ElementSource other) {
        return other instanceof DatasetSource;
    }

    private int dependencyCount;

    @Override
    protected void onPrepareFind(Pager.Page page, List<Element> dependenciesElements) {
        super.onPrepareFind(page, dependenciesElements);
        dependencyCount = dependenciesElements.size();
    }

    @Override
    protected Task<List<Object>> find(Pager.Page page) {
        int adCount = (int) Math.floor(dependencyCount / 10d);
        List<Object> list = new ArrayList<>(adCount);
        for (int i = 0; i < adCount; i++) {
            list.add("---------- This is an ad ----------");
        }
        return Task.forResult(list);
    }

    @Override
    protected int orderBefore(Pager.Page page, int position, Element dependencyElement) {
        return (position > 0 && position % 10 == 0) ? 1 : 0;
    }

    @Override
    protected ElementSerializer instantiateSerializer() {
        return new StringSerializer();
    }

    @Override
    protected int getElementType(Object data) {
        return Presenter.TYPE_TEXT_MESSAGE;
    }
}
