package com.otaliastudios.elements;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.CallSuper;
import android.support.annotation.IntDef;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import bolts.Continuation;
import bolts.Task;

/**
 * A simple implementation of ElementPresenter that, along with {@link BaseSource}, supports
 * the display of three special entities in lists called Placeholders: a Pagination placeholder,
 * an Empty placeholder and an Error placeholder.
 *
 * Pagination placeholder:
 * Control with {@link #setPaginationMode(int)}, {@link #onInitializePaginationView(Holder)},
 * {@link #onBindPaginationView(Pager.Page, Holder, Element)}.
 * This class implements two different policies for pagination:
 * - {@link #PAGINATION_MODE_DELAYED}: when the view is bound, after a small delay,
 *   the adapter is asked for the next page. The view should be something like a ProgressBar.
 * - {@link #PAGINATION_MODE_ONCLICK}: when the view is clicked, the adapter is asked for
 *   the next page. The view should be something like a Button.
 *
 * Empty placeholder:
 * Control with {@link #setEmptyViewRes(int)}, {@link #onInitializeEmptyView(Holder)},
 * {@link #onBindEmptyView(Pager.Page, Holder)}.
 *
 * Error placeholder:
 * Control with {@link #setErrorViewRes(int)}, {@link #onInitializeErrorView(Holder)},
 * {@link #onBindErrorView(Pager.Page, Holder)}.
 *
 * Ordinary, non-placeholder views should be created and bound using {@link #setViewRes(int)},
 * {@link #onInitializeValidView(Holder)}, {@link #onBindView(Pager.Page, Holder, Element)}.
 *
 * @see BaseSource
 */
public abstract class BasePresenter extends ElementPresenter {

    // Pagination mode stuff.
    public final static int PAGINATION_MODE_DELAYED = 0;
    public final static int PAGINATION_MODE_ONCLICK = 1;
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({PAGINATION_MODE_DELAYED, PAGINATION_MODE_ONCLICK})
    public @interface PaginationMode {}

    @PaginationMode private int paginationMode;

    // Layout res.
    @LayoutRes private int paginationDelayedViewRes = R.layout.placeholder_pagination_progress;
    @LayoutRes private int paginationOnClickViewRes = R.layout.placeholder_pagination_onclick;
    @LayoutRes private int emptyViewRes = R.layout.placeholder_empty;
    @LayoutRes private int errorViewRes = R.layout.placeholder_error;
    @LayoutRes private int viewRes;

    private PlaceholderClickListener clickListener;

    public BasePresenter(Context context, @LayoutRes int viewRes) {
        super(context);
        this.viewRes = viewRes;
    }

    public void setViewRes(int viewRes) {
        this.viewRes = viewRes;
    }

    public void setEmptyViewRes(int emptyViewRes) {
        this.emptyViewRes = emptyViewRes;
    }

    public void setErrorViewRes(int errorViewRes) {
        this.errorViewRes = errorViewRes;
    }

    public void setPaginationDelayedViewRes(int delayedViewRes) {
        this.paginationDelayedViewRes = delayedViewRes;
    }

    public void setPaginationOnClickViewRes(int onClickViewRes) {
        this.paginationOnClickViewRes = onClickViewRes;
    }

    public void setPaginationMode(@PaginationMode int paginationMode) {
        this.paginationMode = paginationMode;
    }

    @PaginationMode
    public int getPaginationMode() {
        return paginationMode;
    }

    public void setPlaceholderClickListener(PlaceholderClickListener clickListener) {
        this.clickListener = clickListener;
    }

    // Element types

    @NonNull
    @Override
    protected final List<Integer> getElementTypes() {
        List<Integer> list = getValidElementTypes();
        list.add(BaseSource.TYPE_EMPTY);
        list.add(BaseSource.TYPE_ERROR);
        list.add(BaseSource.TYPE_PAGINATION);
        return list;
    }

    protected List<Integer> getValidElementTypes() {
        return new ArrayList<>(Arrays.asList(0));
    }

    // Views

    @Override
    protected View onCreateView(ViewGroup parent, int elementType) {
        int res;
        switch (elementType) {
            case BaseSource.TYPE_EMPTY: res = emptyViewRes; break;
            case BaseSource.TYPE_ERROR: res = errorViewRes; break;
            case BaseSource.TYPE_PAGINATION: res = getPaginationMode() == PAGINATION_MODE_DELAYED ?
                    paginationDelayedViewRes : paginationOnClickViewRes;
                break;
            default: res = viewRes; break;
        }
        return LayoutInflater.from(getContext()).inflate(res, parent, false);
    }

    // Initialization

    @Override
    protected final void onInitialize(Holder holder) {
        super.onInitialize(holder);
        switch (holder.getElementType()) {
            case BaseSource.TYPE_EMPTY: onInitializeEmptyView(holder); break;
            case BaseSource.TYPE_ERROR: onInitializeErrorView(holder); break;
            case BaseSource.TYPE_PAGINATION: onInitializePaginationView(holder); break;
            default: onInitializeValidView(holder); break;
        }
    }

    protected void onInitializePaginationView(Holder holder) {}
    protected void onInitializeEmptyView(Holder holder) {}
    protected void onInitializeErrorView(Holder holder) {}
    protected void onInitializeValidView(Holder holder) {}

    // Binding

    @Override
    protected final void onBind(final Pager.Page page, final Holder holder, final Element element) {
        switch (element.getElementType()) {
            case BaseSource.TYPE_EMPTY: onBindEmptyView(page, holder); break;
            case BaseSource.TYPE_ERROR: onBindErrorView(page, holder); break;
            case BaseSource.TYPE_PAGINATION: onBindPaginationView(page, holder, element); break;
            default: onBindView(page, holder, element); break;
        }
    }

    @CallSuper
    protected void onBindEmptyView(Pager.Page page, Holder holder) {
        if (clickListener != null) {
            holder.getRoot().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    clickListener.onEmptyClick();
                }
            });
        }
    }

    @CallSuper
    protected void onBindErrorView(Pager.Page page, Holder holder) {
        if (clickListener != null) {
            holder.getRoot().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    clickListener.onErrorClick();
                }
            });
        }
    }

    @CallSuper
    protected void onBindPaginationView(final Pager.Page page, Holder holder, final Element element) {
        // Remove the element from this page as soon as next page is loaded.
        final Continuation<Void, Void> then = new Continuation<Void, Void>() {
            @Override
            public Void then(Task<Void> task) throws Exception {
                page.removeElement(element);
                return null;
            }
        };
        // Act depending on pagination mode.
        if (getPaginationMode() == PAGINATION_MODE_DELAYED) {
            try {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        page.loadNextPage().onSuccess(then, Task.UI_THREAD_EXECUTOR);
                    }
                }, 500);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (getPaginationMode() == PAGINATION_MODE_ONCLICK) {
            holder.getRoot().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    page.loadNextPage().onSuccess(then, Task.UI_THREAD_EXECUTOR);
                }
            });
        }
    }

    @CallSuper
    protected void onBindView(Pager.Page page, Holder holder, Element element) {
        super.onBind(page, holder, element); // This sets the click listener.
    }

    // Clicks

    public interface PlaceholderClickListener {
        void onEmptyClick();
        void onErrorClick();
    }
}
