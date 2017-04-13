package com.otaliastudios.elements;

import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;

import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

import bolts.Task;

/**
 * A simple implementation of ElementSource that, along with {@link BasePresenter}, supports
 * the display of three special entities in lists called Placeholders.
 *
 * Pagination placeholder:
 * Special element, usually at the end of a Page, to indicate that there are more elements.
 * UI-wise, this might be a 'load more' button or a progress bar that, when displayed, asks
 * the adapter for another page. See {@link BasePresenter} for more info.
 * Usage is simple. Just call {@link #appendPaginationPlaceholder(List)} to append a pagination
 * placeholder to a certain list of objects. The best moment to do that is during
 * {@link #find(Pager.Page)} or from the {@link #onAfterFind(Pager.Page, Task)} callback.
 *
 * Empty placeholder:
 * Special element that is automatically displayed when {@link #find(Pager.Page)} returns an empty
 * list. This is automatically managed by this class.
 *
 * Error placeholder:
 * Same as empty placeholder, but shown when {@link #find(Pager.Page)} returns a failed task.
 * Might happen if there is no network connectivity, if an exception is thrown...
 *
 * @see BasePresenter
 */
public abstract class BaseSource extends ElementSource {

    public final static int TYPE_PAGINATION = -1;
    public final static int TYPE_EMPTY = -2;
    public final static int TYPE_ERROR = -3;

    private enum Placeholder {
        PAGINATION(TYPE_PAGINATION),
        EMPTY(TYPE_EMPTY),
        ERROR(TYPE_ERROR);

        /* package */ int elType;
        /* package */ Placeholder(int elType) {
            this.elType = elType;
        }
    }

    protected void appendPaginationPlaceholder(@NonNull List<Object> objs) {
        objs.add(Placeholder.PAGINATION);
    }

    @CallSuper
    @UiThread
    @Override
    protected Task<List<Object>> onAfterFind(Pager.Page page, Task<List<Object>> task) {
        if (!task.isFaulted() && !task.isCancelled() &&
                (task.getResult() == null || task.getResult().isEmpty())) {
            return Task.forError(new RuntimeException("Empty"));
        }
        return task;
    }

    @UiThread
    @Override
    protected List<Object> onFindError(Pager.Page page, Exception exception) {
        // Only if we are on first page.
        if (page.getPageNumber() == 0) {
            if ("Empty".equals(exception.getMessage())) {
                return Arrays.asList((Object) Placeholder.EMPTY);
            } else {
                return Arrays.asList((Object) Placeholder.ERROR);
            }
        }
        return null;
    }

    @Override
    protected final int getElementType(Object data) {
        if (data instanceof Placeholder) {
            return ((Placeholder) data).elType;
        }
        return getValidElementType(data);
    }

    protected int getValidElementType(Object data) {
        return 0;
    }


    public static boolean isEmptyTask(Task<List<Object>> task) {
        return (task.isFaulted() && "Empty".equals(task.getError().getMessage())) ||
                (task.getResult() != null
                && task.getResult().size() == 1
                && task.getResult().get(0).equals(Placeholder.EMPTY));
    }

    public static boolean isErrorTask(Task<List<Object>> task) {
        return (task.isFaulted() && !"Empty".equals(task.getError().getMessage())) ||
                (task.getResult() != null
                && task.getResult().size() == 1
                && task.getResult().get(0).equals(Placeholder.ERROR));
    }

    public static void clearPlaceholders(ElementAdapter adapter) {
        Pager.Page page = adapter.getPage(0);
        if (page.getElementsCount() == 1) {
            Element element = page.getElement(0);
            Object data = element.getData();
            if (data.equals(Placeholder.EMPTY) || data.equals(Placeholder.EMPTY)) {
                page.removeElement(0);
            }
        }
    }

    // List of objects can contain one or multiple placeholders. We must get these out of the
    // list, or the subclass Serializer might crash when trying to parcel/unparcel.
    @Override
    protected void onSavePageState(Pager.Page page, List<Object> elements, Bundle outState) {
        super.onSavePageState(page, elements, outState);

        String prefix = "page:"+page.getPageNumber()+":";
        int count = 0;
        ListIterator<Object> it = elements.listIterator();
        while (it.hasNext()) {
            int ind = it.nextIndex();
            Object obj = it.next();
            if (obj instanceof Placeholder) {
                Placeholder holder = (Placeholder) obj;
                outState.putString(prefix+"pos:"+ind+":placeholder", holder.name());
                it.remove();
                count++;
            }
        }
        outState.putInt(prefix+"placeholderCount", count);
    }

    @Override
    protected void onPageStateRestored(Pager.Page page, List<Object> restoredElements, Bundle state) {
        super.onPageStateRestored(page, restoredElements, state);
        String prefix = "page:"+page.getPageNumber()+":";
        int count = state.getInt(prefix+"placeholderCount", 0);
        if (count == 0) return;
        for (int i = 0; i < restoredElements.size() + count; i++) {
            String is = state.getString(prefix+"pos:"+i+":placeholder", null);
            if (is == null) continue;
            Placeholder placeholder = Placeholder.valueOf(is);
            restoredElements.add(i, placeholder);
        }
    }
}
