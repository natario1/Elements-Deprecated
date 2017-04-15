package com.otaliastudios.sample;

import com.otaliastudios.elements.Element;
import com.otaliastudios.elements.ElementSerializer;
import com.otaliastudios.elements.ElementSource;
import com.otaliastudios.elements.Pager;
import com.otaliastudios.elements.SourceMonitor;
import com.otaliastudios.elements.StringSerializer;

import java.util.ArrayList;
import java.util.List;

import bolts.Task;

/**
 * Simple source that provide a single item (a header string)
 * always at the top of the list.
 *
 * This is done by depending on all other sources, so it is requested at the end,
 * and using orderBefore.
 */
public class TopMessageSource extends ElementSource {

    private String message;

    public TopMessageSource(String message) {
        this.message = message;
    }

    @Override
    protected boolean dependsOn(ElementSource other) {
        // All sources except source monitor, to avoid circular dependencies.
        return !(other instanceof SourceMonitor);
    }

    @Override
    protected Task<List<Object>> find(Pager.Page page) {
        List<Object> list = new ArrayList<>();
        if (page.getPageNumber() == 0) list.add(message);
        return Task.forResult(list);
    }

    @Override
    protected int getElementType(Object data) {
        return TextPresenter.TYPE_TEXT_MEDIUM;
    }

    @Override
    protected int orderBefore(Pager.Page page, int position, Element dependencyElement) {
        // We only have one element. Release it at the first call.
        return 1;
    }

    @Override
    protected ElementSerializer instantiateSerializer() {
        return new StringSerializer();
    }
}
