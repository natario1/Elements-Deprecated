package com.otaliastudios.sample;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.LayoutRes;
import android.widget.TextView;

import com.otaliastudios.elements.BasePresenter;
import com.otaliastudios.elements.Element;
import com.otaliastudios.elements.ElementPresenter;
import com.otaliastudios.elements.Pager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Extremely simple ElementPresenter that fits strings into a text view.
 */
public class TextPresenter extends BasePresenter {

    public final static int TYPE_TEXT_SMALL = 1;
    public final static int TYPE_TEXT_MEDIUM = 2;
    public final static int TYPE_TEXT_LARGE = 3;

    public TextPresenter(Context context, OnClickListener listener) {
        super(context, R.layout.text_view);
        setOnClickListener(listener);
    }

    @Override
    protected ArrayList<Integer> getValidElementTypes() {
        return new ArrayList<>(Arrays.asList(TYPE_TEXT_SMALL, TYPE_TEXT_MEDIUM, TYPE_TEXT_LARGE));
    }

    @Override
    protected void onBindView(Pager.Page page, Holder holder, Element element) {
        super.onBindView(page, holder, element);
        String text = (String) element.getData();
        TextView view = (TextView) holder.getRoot();
        view.setText(text);
        switch (element.getElementType()) {
            case TYPE_TEXT_SMALL:
                view.setTextSize(16);
                view.setTextColor(Color.GRAY);
                break;
            case TYPE_TEXT_MEDIUM:
                view.setTextSize(18);
                view.setTextColor(Color.DKGRAY);
                break;
            case TYPE_TEXT_LARGE:
                view.setTextSize(22);
                view.setTextColor(Color.BLACK);
                break;
        }
    }
}
