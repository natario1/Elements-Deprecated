package com.otaliastudios.elements;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * {@code ElementPresenter} is the component that takes model data as returned by
 * {@link ElementSource#find(Pager.Page)}, and binds it to views.
 * Speaking in the ordinary RecyclerView world, {@code ElementPresenter} holds the logic for
 * view holders and {@link RecyclerView.Adapter#onBindViewHolder(RecyclerView.ViewHolder, int)}.
 *
 * Callbacks:
 * The binding process happens through different steps.
 * - {@link #getElementTypes()} is called to understand which Elements should be bound by this
 *   presenter. For simple situations, the default (0) can be used for both this method and its
 *   source counterpart, {@link ElementSource#getElementType(Object)}.
 *   For more complex needs, you can easily have a presenter react only to specific types, or
 *   multiple sources be bound by the same presenter, in a reusable fashion.
 * - {@link #onCreateView(ViewGroup, int)} is called to create / inflate a view for the specified
 *   element type. It is up to you wheter to use the same view or different views for that type.
 * - {@link #onRegisterChildViews(int)} is called to create references to the inner views that will be
 *   used later. This improves performances, so that later you can call {@link Holder#getView(int)}
 *   instead of expensive findViewById().
 * - {@link #onInitialize(Holder)} is called for a certain holder. This is the point where you want
 *   to do operations on views that do not depend on input model data (e.g. setting a color filter
 *   to drawables). These operations can be done also {@link #onBind(Pager.Page, Holder, Element)},
 *   but that is called multiple times, so it's better to move some overhead here.
 * - {@link #onBind(Pager.Page, Holder, Element)} is called when binding.
 *   You can access page information, holder views with {@link Holder#getView(int)}, and model data
 *   with {@link Element#getData()}.
 *
 * State:
 * This is meant to be a stateful component. Think, for example, of selected / checked items in
 * a list. It is your responsibility to appropriately save and restore state in
 * {@link #saveState(Bundle)} and {@link #restoreState(Bundle)}.
 *
 * Clicks:
 * {@code ElementPresenter} provides built in click behavior to the view root.
 * This behavior can be activated by using a {@link OnClickListener}, see
 * {@link #setOnClickListener(OnClickListener)}.
 */
public abstract class ElementPresenter {

    private Context context;
    private OnClickListener listener;

    public ElementPresenter(Context context) {
        this.context = context;
    }

    public ElementPresenter(Context context, OnClickListener listener) {
        this.context = context;
        setOnClickListener(listener);
    }

    protected Context getContext() {
        return context;
    }

    /**
     * Sets a click listener for clicks on root views.
     * Clicks on child views should be managed {@link #onBind(Pager.Page, Holder, Element)}.
     * @param listener click listener
     */
    public void setOnClickListener(OnClickListener listener) {
        this.listener = listener;
    }

    /**
     * This is an important callback if you have multiple element types.
     * Here this presenter can register to respond to certain element types, as returned by
     * {@link ElementSource#getElementType(Object)}.
     * If multiple ElementPresenter ask to layout a certain element type, the priority is given
     * based on the order of presenters passed to {@link ElementAdapter#setPresenter(ElementPresenter...)}.
     *
     * @return a list of element types this ElementPresenter is able to lay out.
     */
    @NonNull
    protected List<Integer> getElementTypes() {
        return new ArrayList<>(Arrays.asList(0));
    }

    /* package */ final Holder instantiateHolder(ViewGroup parent, int elementType) {
        Holder holder = new Holder(onCreateView(parent, elementType), elementType, onRegisterChildViews(elementType));
        onInitialize(holder);
        return holder;
    }

    /**
     * Asks to return a View for the given elementType.
     *
     * @param parent parent view, in case you want to inflate layout params
     * @param elementType requested element type
     * @return a View for this elementType
     */
    protected abstract View onCreateView(ViewGroup parent, int elementType);

    /**
     * Should be overriden to improve performance. By returning a list of {@link ViewReference}s,
     * you can later retrieve the views using {@link Holder#getView(int)} and avoid
     * useless calls to findViewById.
     * This is like holding views in a view holder.
     *
     * @param elementType requested element type
     * @return a list of ViewReferences, or null
     */
    protected List<ViewReference> onRegisterChildViews(int elementType) { return null; }

    /**
     * Called when the holder is instantiated, has a root view and eventually child references.
     * The element type can be retrieved with {@link Holder#getElementType()}.
     *
     * This is the point where you would perform basic view initialization, e.g. setting a color
     * filter to a drawable, *before* having actual data to bind. The advantage is that this
     * is called just once per Holder.
     *
     * @param holder the holder instance.
     */
    protected void onInitialize(Holder holder) {};

    /**
     * Called when it's time to bind model data, represented by the {@link Element}, to views
     * represented by the given {@link Holder}, for the given {@link Pager.Page}.
     *
     * You can get the current element type by using either {@link Holder#getElementType()}
     * or {@link Element#getElementType()}.
     *
     * @param page requested page to be filled
     * @param holder view holder
     * @param element model data
     */
    @CallSuper
    @UiThread
    protected void onBind(final Pager.Page page, final Holder holder, final Element element) {
        holder.getRoot().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onElementClick(page, holder, element);
            }
        });
    }

    /**
     * A final Holder that wraps {@link RecyclerView.ViewHolder}. You don't need to extend that
     * anymore.
     */
    public static final class Holder extends RecyclerView.ViewHolder {

        private int elementType;
        private View root;
        private SparseArray<ViewReference> views;

        private Holder(View itemView, int elementType, List<ViewReference> views) {
            super(itemView);
            this.elementType = elementType;
            this.root = itemView;
            this.views = new SparseArray<>();
            if (views != null) {
                for (ViewReference reference : views) {
                    reference.find(root);
                    this.views.put(reference.id, reference);
                }
            }
        }

        public int getElementType() {
            return elementType;
        }

        public View getRoot() {
            return root;
        }

        /**
         * Register a View into the holder. This just means that the view will be stored in a
         * member field and can be retrieved, later, using {@link #getView(int)}.
         * This avoids unnecessary calls to findViewById.
         * TODO either this or onRegisterChildViews.
         * @param viewId the child view id.
         */
        public void registerView(int viewId) {
            ViewReference ref = this.views.get(viewId, null);
            if (ref == null) {
                ref = new ViewReference(viewId);
                ref.find(root);
                this.views.put(ref.id, ref);
            }
        }

        /**
         * Gets a View previously registered with {@link #registerView(int)} or with
         * {@link #onRegisterChildViews(int)}, to improve performance.
         * @param id the View id.
         * @param <T> the View type.
         * @return the View.
         */
        public <T extends View> T getView(int id) {
            ViewReference ref = views.get(id);
            return ref.find(root);
        }
    }

    /**
     * A 'weak' reference to a View so that it can be retrieved
     * without using findViewById.
     */
    public static class ViewReference {
        int id;
        View view;

        public ViewReference(int viewId) {
            this.id = viewId;
        }

        private <T extends View> T find(View root) {
            if (view == null) view = root.findViewById(id);
            // noinspection unchecked
            return (T) view;
        }
    }

    /**
     * Called when it's time to save the presenter state.
     * Any stateful information, such as which items are checked / selected at the time of saving,
     * should be inserted to outState.
     * @param outState an out bundle.
     */
    @UiThread
    protected void saveState(Bundle outState) {
    }

    /**
     * Called when it's time to restore the presenter state.
     * Here you can retrieve information previously put in {@link #saveState(Bundle)}.
     * Note that this is called from a worker thread.
     *
     * @param savedInstanceState the saved Bundle.
     */
    @WorkerThread
    protected void restoreState(@NonNull Bundle savedInstanceState) {
    }

    /**
     * Implement this to listen to element clicks, i.e., clicks on the root view as returned by
     * {@link #onCreateView(ViewGroup, int)}.
     */
    public interface OnClickListener {
        void onElementClick(Pager.Page page, Holder holder, Element element);
    }
}
