package com.otaliastudios.sample;

import com.otaliastudios.elements.Element;
import com.otaliastudios.elements.ElementSerializer;
import com.otaliastudios.elements.ElementSource;
import com.otaliastudios.elements.HeaderSource;
import com.otaliastudios.elements.StringSerializer;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides 'letter' headings to the list.
 */
public class LettersSource extends HeaderSource<String, String> {

    public LettersSource(Class<? extends ElementSource> typeClass) {
        super(typeClass);
    }

    @Override
    protected ElementSerializer instantiateSerializer() {
        return new StringSerializer();
    }

    @Override
    protected List<String> getAnchors(List<Element> dependenciesElements) {
        List<String> anchors = new ArrayList<>();
        char last = (char) -1;
        for (Element element : dependenciesElements) {
            if (!(element.getData() instanceof String)) continue;
            String data = ((String) element.getData());
            char current = data.charAt(0);
            if (current != last) {
                anchors.add(data);
                last = current;
            }
        }
        return anchors;
    }

    @Override
    protected String getHeaderForAnchor(String s) {
        return s.substring(0, 1).toUpperCase();
    }

    @Override
    protected int getElementType(Object data) {
        return Presenter.TYPE_TEXT_LARGE;
    }
}
