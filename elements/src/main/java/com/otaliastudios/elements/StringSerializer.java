package com.otaliastudios.elements;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A handy serializer for lists of strings.
 */
public class StringSerializer implements ElementSerializer {

    @Override
    public void saveElements(Pager.Page page, ElementSource source, List<Object> elements, Bundle state) {
        ArrayList<String> list = new ArrayList<>();
        for (Object element : elements) {
            if (element instanceof String) {
                list.add((String) element);
            }
        }
        if (!list.isEmpty()) {
            state.putStringArrayList("page:"+page.getPageNumber(), list);
        }
    }

    @NonNull
    @Override
    public List<Object> restoreElements(Pager.Page page, ElementSource source, Bundle state) {
        ArrayList<String> list = state.getStringArrayList("page:"+page.getPageNumber());
        if (list == null) return new ArrayList<>();
        return new ArrayList<Object>(list);
    }
}
