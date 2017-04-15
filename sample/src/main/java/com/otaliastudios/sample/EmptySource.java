package com.otaliastudios.sample;

import com.otaliastudios.elements.BaseSource;
import com.otaliastudios.elements.ElementSerializer;
import com.otaliastudios.elements.ElementSource;
import com.otaliastudios.elements.Pager;
import com.otaliastudios.elements.StringSerializer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import bolts.Continuation;
import bolts.Task;


/**
 * This source returns no results.
 */
public class EmptySource extends BaseSource {

    @Override
    protected boolean dependsOn(ElementSource other) {
        return false;
    }

    @Override
    protected ElementSerializer instantiateSerializer() {
        return null;
    }

    @Override
    protected Task<List<Object>> find(Pager.Page page) {
        return Task.<List<Object>>forResult(new ArrayList<>());
    }
}
