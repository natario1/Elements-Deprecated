package com.otaliastudios.elements;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A handy serializer for lists of objects that implement Parcelable.
 * As such, they can easily be stored in the input Bundle, which is what we do here.
 */
public class ParcelableSerializer implements ElementSerializer {

    @Override
    public void saveElements(Pager.Page page, ElementSource source, List<Object> elements, Bundle state) {
        ArrayList<Parcelable> list = new ArrayList<>();
        for (Object element : elements) {
            if (element instanceof Parcelable) {
                list.add((Parcelable) element);
            }
        }
        if (!list.isEmpty()) {
            state.putParcelableArrayList("page:"+page.getPageNumber(), list);
        }
    }

    @NonNull
    @Override
    public List<Object> restoreElements(Pager.Page page, ElementSource source, Bundle state) {
        ArrayList<Parcelable> list = state.getParcelableArrayList("page:"+page.getPageNumber());
        if (list == null) return new ArrayList<>();
        return new ArrayList<Object>(list);
    }
}
