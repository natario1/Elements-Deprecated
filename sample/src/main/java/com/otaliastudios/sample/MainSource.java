package com.otaliastudios.sample;

import com.otaliastudios.elements.BaseSource;
import com.otaliastudios.elements.ElementSerializer;
import com.otaliastudios.elements.ElementSource;
import com.otaliastudios.elements.Pager;
import com.otaliastudios.elements.StringSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import bolts.Task;


public final class MainSource extends BaseSource {

    @Override
    protected boolean dependsOn(ElementSource other) {
        return false;
    }

    @Override
    protected Task<List<Object>> find(Pager.Page page) {
        return Task.callInBackground(new Callable<List<Object>>() {
            @Override
            public List<Object> call() throws Exception {
                List<Object> list = new ArrayList<>();
                list.add("Placeholders");
                list.add("List with loading placeholder");
                list.add("List with empty placeholder");
                list.add("List with pagination (endless adapter)");
                list.add("Mixed content");
                list.add("List with letter headers");
                list.add("List with ads");
                return list;
            }
        });
    }

    public boolean isHeader(String data) {
        return data.equals("Placeholders") || data.equals("Mixed content");
    }
    @Override
    protected int getValidElementType(Object data) {
        String text = (String) data;
        if (isHeader(text)) {
            return Presenter.TYPE_TEXT_LARGE;
        }
        return Presenter.TYPE_TEXT_SMALL;
    }

    @Override
    protected ElementSerializer instantiateSerializer() {
        return new StringSerializer();
    }
}
