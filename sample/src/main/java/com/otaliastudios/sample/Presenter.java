package com.otaliastudios.sample;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.widget.TextView;

import com.otaliastudios.elements.BasePresenter;
import com.otaliastudios.elements.Element;
import com.otaliastudios.elements.Pager;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Extremely simple ElementPresenter that fits strings into a text view.
 */
public class Presenter extends BasePresenter {

    public final static int TYPE_TEXT_SMALL = 1;
    public final static int TYPE_TEXT_MESSAGE = 2;
    public final static int TYPE_TEXT_LARGE = 3;

    public Presenter(Context context, OnClickListener listener) {
        super(context, R.layout.text_view);
        setOnClickListener(listener);
    }

    @Override
    protected ArrayList<Integer> getValidElementTypes() {
        return new ArrayList<>(Arrays.asList(TYPE_TEXT_SMALL, TYPE_TEXT_MESSAGE, TYPE_TEXT_LARGE));
    }

    @Override
    protected void onBindView(Pager.Page page, Holder holder, Element element) {
        super.onBindView(page, holder, element);
        String text = (String) element.getData();
        TextView view = (TextView) holder.getRoot();
        view.setText(text);
        int size, textColor, bgColor;
        switch (element.getElementType()) {
            case TYPE_TEXT_SMALL:
                size = 16;
                textColor = Color.GRAY;
                bgColor = Color.TRANSPARENT;
                break;
            case TYPE_TEXT_MESSAGE:
                size = 18;
                textColor = Color.WHITE;
                bgColor = ContextCompat.getColor(getContext(), R.color.colorAccent);
                // textColor = Color.DKGRAY;
                // bgColor = Color.WHITE;
                break;
            case TYPE_TEXT_LARGE:
            default:
                size = 22;
                textColor = Color.BLACK;
                bgColor = Color.TRANSPARENT;
                break;
        }
        view.setTextSize(size);
        view.setTextColor(textColor);
        view.setBackgroundColor(bgColor);

    }
}
