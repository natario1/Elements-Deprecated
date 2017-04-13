package com.otaliastudios.elements;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A {@code ElementSerializer} that stores object in a static map.
 * This should generally be avoided: if your data is parcelable, use {@link ParcelableSerializer} or
 * make your own serializer for simpler data.
 *
 * That said, {@code StaticSerializer} is a handy workaround for lists of objects that can't be
 * serialized in any way. Objects in the cache are automatically removed after a reasonable time.
 * Use persistance at your own risk: if you restore after persistance has ended, you will end up
 * with an empty list.
 */
public class StaticSerializer implements ElementSerializer {

    private final static String TAG = StaticSerializer.class.getSimpleName();
    private final static SparseArray<List<Object>> CACHE = new SparseArray<>();
    private final static Random GENERATOR = new Random();

    private int persistanceMillis = -1;

    public StaticSerializer() {
    }

    public StaticSerializer(int persistanceMillis) {
        this.persistanceMillis = persistanceMillis;
    }


    @Override
    public void saveElements(Pager.Page page, ElementSource source, List<Object> elements, Bundle state) {
        String key = getIdKeyForPage(page);
        final int id = GENERATOR.nextInt();
        state.putInt(key, id);
        CACHE.put(id, elements);
        // Delete if asked to.
        if (persistanceMillis > 0) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    CACHE.remove(id);
                }
            }, persistanceMillis);
        }
    }

    @NonNull
    @Override
    public List<Object> restoreElements(Pager.Page page, ElementSource source, Bundle state) {
        int id = state.getInt(getIdKeyForPage(page));
        List<Object> list = CACHE.get(id);
        CACHE.remove(id);
        return list == null ? new ArrayList<>() : list;
    }


    private static String getIdKeyForPage(Pager.Page page) {
        return TAG + ":ID:" + page.getPageNumber();
    }
}
