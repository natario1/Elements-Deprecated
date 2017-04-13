package com.otaliastudios.elements;


/**
 * An Element is the base block of the library. It is a wrapper around the model object {@code T}
 * that includes additional info: an id identifying the {@link ElementSource} that originated this,
 * and the element elementType, as returned by {@link ElementSource#getElementType(Object)}.
 * The elementType is later used to link this Element with an appropriate {@link ElementPresenter}.
 *
 * Elements are instantiated by the framework. If you need to insert a new element at a certain
 * point in the callback chain, use {@link ElementAdapter#createElement(ElementSource, Object)}
 * to specify the source, or just use {@link #cloneWithData(Object)} to change data.
 *
 * @param <T> The model class
 */
public final class Element<T> {
    /* package */ int sourceId;
    /* package */ private T data;
    /* package */ int elementType;

    /* package */ Element(int sourceId, int elementType, T data) {
        this.sourceId = sourceId;
        this.data = data;
        this.elementType = elementType;
    }

    /**
     * Returns model data linked to this Element.
     * @return model data.
     */
    public T getData() {
        return data;
    }

    /**
     * Returns the object element type as returned by {@link ElementSource#getElementType(Object)}.
     * @return the element type.
     */
    public int getElementType() {
        return elementType;
    }

    /**
     * Creates a copy of this element with different data. This is useful to insert
     * new elements into a {@link Pager.Page}.
     *
     * @param data new model data for the clone.
     * @return a new Element instance.
     */
    public Element<T> cloneWithData(T data) {
        return new Element<>(sourceId, elementType, data);
    }
}
