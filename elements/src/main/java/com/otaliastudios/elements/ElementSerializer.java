package com.otaliastudios.elements;

import android.os.Bundle;
import android.support.annotation.NonNull;

import java.util.List;

/**
 * An object used by {@link ElementSource}s to serialize objects.
 * Each {@code ElementSerializer} is passed its own Bundle, so there's no risk of overriding
 * each other's properties.
 */
public interface ElementSerializer {

    /**
     * Called to save the list of objects in the passed Bundle.
     * @param page the page currently being saved
     * @param source the source this Serializer belongs to
     * @param elements the list of elements to be saved
     * @param state the bundle into which to save the objects
     */
    void saveElements(Pager.Page page, ElementSource source, List<Object> elements, Bundle state);


    /**
     * Called to restore the list of objects from the passed Bundle.
     * @param page the page currently being restored
     * @param source the source this Serializer belongs to
     * @param state the bundle from which to restore the objects
     * @return the list of restored elements, empty if no elements were restored. Not null.
     */
    @NonNull
    List<Object> restoreElements(Pager.Page page, ElementSource source, Bundle state);
}
