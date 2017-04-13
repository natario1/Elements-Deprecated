package com.otaliastudios.elements;

import android.os.Bundle;
import android.support.annotation.NonNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A handy serializer for lists of objects that implement Serializable.
 * As such, they can easily be stored in the input Bundle, which is what we do here.
 */
public class SerializableSerializer implements ElementSerializer {

    @Override
    public void saveElements(Pager.Page page, ElementSource source, List<Object> elements, Bundle state) {
        int count = 0;
        String prefix = "page:"+page.getPageNumber()+":";
        for (Object element : elements) {
            if (element instanceof Serializable) {
                state.putSerializable(prefix+"num:"+count, (Serializable) element);
                count++;
            } else {
                break;
            }
        }
        state.putInt(prefix+"count", count);
    }

    @NonNull
    @Override
    public List<Object> restoreElements(Pager.Page page, ElementSource source, Bundle state) {
        String prefix = "page:"+page.getPageNumber()+":";
        int count = state.getInt(prefix+"count");
        ArrayList<Object> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(state.getSerializable(prefix+"num:"+i));
        }
        return list;
    }
}
