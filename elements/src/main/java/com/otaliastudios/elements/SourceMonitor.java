package com.otaliastudios.elements;

import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import bolts.Task;


/**
 * A particular, final {@link ElementSource} that displays no items but can be used to monitor
 * the behavior of another Source. Accessors should pass a {@link Callback} instance that will fire
 * appropriate callbacks:
 * - {@link Callback#onAfterFind(Pager.Page, Object, Task)} after the monitored source elements are found
 * - {@link Callback#onPageLoaded(Pager.Page, List)} after the whole page has been loaded
 * Internally this is done by simply declaring a dependency on the requested source, based on its
 * class.
 *
 * You can quickly get a {@code SourceMonitor} for a certain source by calling
 * {@link ElementSource#getMonitor(SourceMonitor.Callback)}. monitor.getMonitor() will return null.
 *
 * @param <SourceType> The monitorable source type
 */
public final class SourceMonitor<SourceType extends ElementSource> extends ElementSource {

    public interface Callback<SourceType> {
        void onPageLoaded(Pager.Page page, List<Object> sourceObjects);
        Task<List<Object>> onAfterFind(Pager.Page page, SourceType source, Task<List<Object>> task);
    }

    private Callback<SourceType> callback;
    private Class<? extends SourceType> typeClass;

    public SourceMonitor(Class<? extends SourceType> typeClass, Callback<SourceType> callback) {
        super();
        this.callback = callback;
        this.typeClass = typeClass;
    }

    @Override
    public <T extends ElementSource> SourceMonitor<T> getMonitor(Callback<T> callback) {
        return null;
    }

    @Override
    protected final boolean dependsOn(ElementSource other) {
        return typeClass.isInstance(other);
    }

    @Override
    protected final int orderBefore(Pager.Page page, int position, Element dependencyElement) {
        return 0;
    }

    @Override
    protected final int orderAfter(Pager.Page page, int position, Element dependencyElement) {
        return 0;
    }

    @Override
    final protected Task<List<Object>> find(Pager.Page page) {
        return Task.forResult(new ArrayList()).cast();
    }

    @Override
    protected final Task<List<Object>> onAfterFind(Pager.Page page, Task<List<Object>> task) {
        return super.onAfterFind(page, task);
    }

    @Override
    protected final void onPrepareFind(Pager.Page page, List<Element> dependenciesElements) {}

    @Override
    protected void onPageLoaded(Pager.Page page, List<Element> pageElements) {
        if (callback != null) {
            List<Object> objs = new ArrayList<>();
            for (Element e : pageElements) {
                ElementSource source = page.getSourceForElement(e);
                if (dependsOn(source)) {
                    objs.add(e.getData());
                }
            }
            callback.onPageLoaded(page, objs);
        }
    }

    @Override
    protected Task<List<Object>> onDependencyAfterFind(Pager.Page page, ElementSource source, Task<List<Object>> task) {
        if (callback != null) {
            task = callback.onAfterFind(page, (SourceType) source, task);
        }
        return task;
    }

    @Override
    protected ElementSerializer instantiateSerializer() {
        return null;
    }
}
