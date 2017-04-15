package com.otaliastudios.sample;

import com.otaliastudios.elements.Element;
import com.otaliastudios.elements.ElementSerializer;
import com.otaliastudios.elements.ElementSource;
import com.otaliastudios.elements.FooterSource;
import com.otaliastudios.elements.StringSerializer;

import java.util.ArrayList;
import java.util.List;


public class DividerBelowHeadersSource extends FooterSource<String, String> {

    public DividerBelowHeadersSource(Class<? extends ElementSource> typeClass) {
        super(typeClass);
    }

    @Override
    protected List<String> getAnchors(List<Element> dependenciesElements) {
        List<String> list = new ArrayList<>();
        for (Element element : dependenciesElements) {
            if (!(element.getData() instanceof String)) continue;
            if (element.getElementType() == Presenter.TYPE_TEXT_LARGE) {
                list.add((String) element.getData());
            }
        }
        return list;
    }

    @Override
    protected String getFooterForAnchor(String s) {
        return "Divider";
    }

    @Override
    protected ElementSerializer instantiateSerializer() {
        return new StringSerializer();
    }

    @Override
    protected int getElementType(Object data) {
        return DividerPresenter.TYPE_DIVIDER;
    }
}
