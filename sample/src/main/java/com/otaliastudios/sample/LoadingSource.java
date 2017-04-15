package com.otaliastudios.sample;

import com.otaliastudios.elements.BaseSource;
import com.otaliastudios.elements.ElementSerializer;
import com.otaliastudios.elements.ElementSource;
import com.otaliastudios.elements.Pager;
import com.otaliastudios.elements.StringSerializer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import bolts.Continuation;
import bolts.Task;


/**
 * This source delays the find for 2 seconds, just to let us see the
 * progress indicator.
 */
public class LoadingSource extends BaseSource {

    public LoadingSource() {
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
    protected Task<List<Object>> find(Pager.Page page) {
        return Task.delay(2000).onSuccess(new Continuation<Void, List<Object>>() {
            @Override
            public List<Object> then(Task<Void> task) throws Exception {
                return new ArrayList<Object>(Arrays.asList(Dataset.DATASET_ARRAY));
            }
        }, Task.BACKGROUND_EXECUTOR);
    }

    @Override
    protected int getValidElementType(Object data) {
        return TextPresenter.TYPE_TEXT_SMALL;
    }
}
