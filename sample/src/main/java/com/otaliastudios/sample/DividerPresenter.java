package com.otaliastudios.sample;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.otaliastudios.elements.ElementPresenter;

import java.util.Arrays;
import java.util.List;


public class DividerPresenter extends ElementPresenter {

    public final static int TYPE_DIVIDER = 999;

    public DividerPresenter(Context context) {
        super(context);
    }

    @Override
    protected View onCreateView(ViewGroup parent, int elementType) {
        return LayoutInflater.from(getContext()).inflate(R.layout.divider, parent, false);
    }

    @NonNull
    @Override
    protected List<Integer> getElementTypes() {
        return Arrays.asList(TYPE_DIVIDER);
    }
}
