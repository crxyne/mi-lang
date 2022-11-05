package org.crayne.mi.util;

import org.jetbrains.annotations.NotNull;

import java.util.*;

@SuppressWarnings("unused")
public class LimitedSizeQueue <T> implements Collection<T> {

    private final List<T> queue;
    private final int limitedSize;

    public LimitedSizeQueue(final int limitedSize) {
        queue = new ArrayList<>();
        this.limitedSize = limitedSize;
    }

    public LimitedSizeQueue(@NotNull final Collection<T> other, final int limitedSize) {
        queue = new ArrayList<>(other);
        this.limitedSize = limitedSize;
    }

    public List<T> queue() {
        return queue;
    }

    public int size() {
        return queue.size();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public boolean contains(final Object o) {
        return queue.contains(o);
    }

    @NotNull
    public Iterator<T> iterator() {
        return queue.iterator();
    }

    @NotNull
    public Object @NotNull [] toArray() {
        return queue.toArray();
    }

    @SuppressWarnings("SuspiciousToArrayCall")
    @NotNull
    public <N> N @NotNull [] toArray(@NotNull final N @NotNull [] a) {
        return queue.toArray(a);
    }

    public boolean add(final T o) {
        if (queue.size() >= limitedSize) queue.remove(0);
        return queue.add(o);
    }

    public boolean remove(final Object o) {
        return queue.remove(o);
    }

    public boolean addAll(@NotNull final Collection<? extends T> c) {
        return queue.addAll(c);
    }

    public void clear() {
        queue.clear();
    }

    public boolean retainAll(@NotNull final Collection<?> c) {
        return queue.retainAll(c);
    }

    public boolean removeAll(@NotNull final Collection<?> c) {
        return queue.removeAll(c);
    }

    public boolean containsAll(@NotNull final Collection<?> c) {
        return new HashSet<>(queue).containsAll(c);
    }

}
