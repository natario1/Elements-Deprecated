package com.otaliastudios.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;

import com.otaliastudios.elements.ElementAdapter;
import com.otaliastudios.elements.ElementSource;
import com.otaliastudios.elements.Pager;
import com.otaliastudios.elements.SourceMonitor;

import java.util.List;

import bolts.Task;


public class ChildActivity extends AppCompatActivity implements SourceMonitor.Callback<ElementSource> {

    public final static int SHOW_LOADING_PLACEHOLDER = 0;
    public final static int SHOW_EMPTY_PLACEHOLDER = 1;
    public final static int SHOW_PAGINATION_PLACEHOLDER = 2;
    public final static int SHOW_LETTERS = 3;
    public final static int SHOW_ADS = 4;

    private RecyclerView list;
    private int showWhat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recycler);

        showWhat = savedInstanceState != null ?
                savedInstanceState.getInt("show", -1) :
                getIntent().getIntExtra("show", -1);
        if (showWhat < 0) {
            finish();
            return;
        }
        list = (RecyclerView) findViewById(R.id.list);
        show(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        ElementAdapter adapter = (ElementAdapter) list.getAdapter();
        adapter.saveState(outState);
        outState.putInt("show", showWhat);
    }

    private void show(Bundle savedInstanceState) {
        switch (showWhat) {
            case SHOW_LOADING_PLACEHOLDER: showLoadingPlaceholder(savedInstanceState); break;
            case SHOW_EMPTY_PLACEHOLDER: showEmptyPlaceholder(savedInstanceState); break;
            case SHOW_PAGINATION_PLACEHOLDER: showPaginationPlaceholder(savedInstanceState); break;
            case SHOW_LETTERS: showLetters(savedInstanceState); break;
            case SHOW_ADS: showAds(savedInstanceState); break;
        }
        String title = null;
        switch (showWhat) {
            case SHOW_LOADING_PLACEHOLDER: title = "Loading placeholder"; break;
            case SHOW_EMPTY_PLACEHOLDER: title = "Empty list placeholder"; break;
            case SHOW_PAGINATION_PLACEHOLDER: title = "Paginated list"; break;
            case SHOW_LETTERS: title = "Letters headers"; break;
            case SHOW_ADS: title = "Ads"; break;
        }
        if (title != null) {
            getSupportActionBar().setTitle(title);
        }
    }

    private void showLoadingPlaceholder(Bundle savedInstanceState) {
        ElementAdapter adapter = new ElementAdapter();
        adapter.setSource(new LoadingSource(),
                new TopMessageSource("List with a single source that takes 2 seconds to load data. " +
                        "A loading indicator is shown while the task is going on. " +
                        "This message is also part of the list."),
                new SourceMonitor<>(ElementSource.class, this));
        adapter.setPresenter(new TextPresenter(this, null));
        adapter.restoreState(savedInstanceState);
        list = (RecyclerView) findViewById(R.id.list);
        list.setAdapter(adapter);
    }

    private void showEmptyPlaceholder(Bundle savedInstanceState) {
        ElementAdapter adapter = new ElementAdapter();
        adapter.setSource(new EmptySource(),
                new TopMessageSource("List with a single source that returns empty data. " +
                        "A placeholder is automatically shown saying there's no data. " +
                        "This message is also part of the list."),
                new SourceMonitor<>(ElementSource.class, this));
        adapter.setPresenter(new TextPresenter(this, null));
        adapter.restoreState(savedInstanceState);
        list = (RecyclerView) findViewById(R.id.list);
        list.setAdapter(adapter);
    }

    private void showPaginationPlaceholder(Bundle savedInstanceState) {
        ElementAdapter adapter = new ElementAdapter();
        adapter.setSource(new PaginationSource(),
                new TopMessageSource("List with a single source that returns fake items, " +
                        "in pages of 25 items each. Scrolling down, you will see a progress " +
                        "indicator while next page loads. " +
                        "This message is also part of the list."),
                new SourceMonitor<>(ElementSource.class, this));
        adapter.setPresenter(new TextPresenter(this, null));
        adapter.restoreState(savedInstanceState);
        list = (RecyclerView) findViewById(R.id.list);
        list.setAdapter(adapter);
    }

    private void showLetters(Bundle savedInstanceState) {
        ElementAdapter adapter = new ElementAdapter();
        adapter.setSource(new DatasetSource(),
                new LettersSource(DatasetSource.class),
                new DividerBelowHeadersSource(LettersSource.class),
                new TopMessageSource("List with three sources: one for fake items, one that " +
                        "adds headers (A, B, C) at the right position, and one that adds dividers " +
                        "just below header letters. This message is also part of the list."),
                new SourceMonitor<>(ElementSource.class, this));
        adapter.setPresenter(new TextPresenter(this, null), new DividerPresenter(this));
        adapter.restoreState(savedInstanceState);
        list = (RecyclerView) findViewById(R.id.list);
        list.setAdapter(adapter);
    }

    private void showAds(Bundle savedInstanceState) {
        ElementAdapter adapter = new ElementAdapter();
        adapter.setSource(new DatasetSource(),
                new AdsSource(),
                new TopMessageSource("List with two sources: one for fake items, and one that " +
                        "adds fake ads every 10 items. This message is also part of the list."),
                new SourceMonitor<>(ElementSource.class, this));
        adapter.setPresenter(new TextPresenter(this, null));
        adapter.restoreState(savedInstanceState);
        list = (RecyclerView) findViewById(R.id.list);
        list.setAdapter(adapter);
    }

    @Override
    public Task<List<Object>> onAfterFind(Pager.Page page, ElementSource source, Task<List<Object>> task) {
        return task;
    }

    @Override
    public void onPageLoaded(Pager.Page page, List<Object> sourceObjects) {
        if (page.getPageNumber() == 0) list.scrollToPosition(0);
    }
}
