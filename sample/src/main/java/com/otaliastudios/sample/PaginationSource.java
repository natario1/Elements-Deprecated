package com.otaliastudios.sample;

import com.otaliastudios.elements.BaseSource;
import com.otaliastudios.elements.ElementSerializer;
import com.otaliastudios.elements.ElementSource;
import com.otaliastudios.elements.Pager;
import com.otaliastudios.elements.StringSerializer;

import java.util.ArrayList;
import java.util.List;

import bolts.Continuation;
import bolts.Task;


/**
 * This source returns fake strings from the Dataset class,
 * 25 per page.
 */
public class PaginationSource extends BaseSource {

    private final static ArrayList<Object> DATASET = Dataset.DATASET_LIST;
    private final static int OBJECTS_PER_PAGE = 25;

    public PaginationSource() {
        setLoadingPlaceholderEnabled(true);
    }

    @Override
    protected boolean dependsOn(ElementSource other) {
        return false;
    }

    @Override
    protected ElementSerializer instantiateSerializer() {
        return new StringSerializer();
    }

    @Override
    protected Task<List<Object>> find(final Pager.Page page) {
        return Task.delay(500).onSuccess(new Continuation<Void, List<Object>>() {
            @Override
            public List<Object> then(Task<Void> task) throws Exception {
                int start = page.getPageNumber() * OBJECTS_PER_PAGE; // skip 'start' items
                int end = Math.min(start + OBJECTS_PER_PAGE, DATASET.size());
                List<Object> list = new ArrayList<>(DATASET.subList(start, end));
                if (end < DATASET.size()) {
                    // There is another page, at least.
                    appendPaginationPlaceholder(list);
                }
                return list;
            }
        }, Task.BACKGROUND_EXECUTOR);
    }

    @Override
    protected int getValidElementType(Object data) {
        return Presenter.TYPE_TEXT_SMALL;
    }
}
