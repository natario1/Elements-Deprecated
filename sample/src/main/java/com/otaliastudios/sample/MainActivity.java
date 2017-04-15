package com.otaliastudios.sample;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;

import com.otaliastudios.elements.Element;
import com.otaliastudios.elements.ElementAdapter;
import com.otaliastudios.elements.ElementPresenter;
import com.otaliastudios.elements.Pager;

public class MainActivity extends AppCompatActivity implements ElementPresenter.OnClickListener {


    private RecyclerView list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recycler);

        ElementAdapter adapter = new ElementAdapter();
        adapter.setSource(new MainSource(),
                new DividerBelowHeadersSource(MainSource.class),
                new TopMessageSource("List with two main sources: one is providing menu strings, " +
                        "the other is adding dividers at the right position. This message is also " +
                        "part of the list, thanks to another source."));
        adapter.setPresenter(new TextPresenter(this, this), new DividerPresenter(this));
        adapter.restoreState(savedInstanceState);
        list = (RecyclerView) findViewById(R.id.list);
        list.setAdapter(adapter);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        ElementAdapter adapter = (ElementAdapter) list.getAdapter();
        adapter.saveState(outState);
    }

    @Override
    public void onElementClick(Pager.Page page, ElementPresenter.Holder holder, Element element) {
        if (element.getElementType() != TextPresenter.TYPE_TEXT_SMALL) return;
        // React to small text clicks.
        String text = (String) element.getData();
        int what = -1;
        switch (text) {
            case "List with loading placeholder":
                what = ChildActivity.SHOW_LOADING_PLACEHOLDER;
                break;
            case "List with empty placeholder":
                what = ChildActivity.SHOW_EMPTY_PLACEHOLDER;
                break;
            case "List with pagination (endless adapter)":
                what = ChildActivity.SHOW_PAGINATION_PLACEHOLDER;
                break;
            case "List with letter headers":
                what = ChildActivity.SHOW_LETTERS;
                break;
            case "List with ads":
                what = ChildActivity.SHOW_ADS;
                break;
        }
        if (what >= 0) {
            Intent i = new Intent(this, ChildActivity.class);
            i.putExtra("show", what);
            startActivity(i);
        }
    }
}
