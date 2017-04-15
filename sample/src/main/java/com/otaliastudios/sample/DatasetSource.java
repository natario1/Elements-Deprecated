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
 * Just returns items from the dataset.
 */
public class DatasetSource extends BaseSource {

    private final static ArrayList<Object> DATASET = Dataset.DATASET_LIST;

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
        return Task.<List<Object>>forResult(DATASET);
    }

    @Override
    protected int getValidElementType(Object data) {
        return TextPresenter.TYPE_TEXT_SMALL;
    }
}
