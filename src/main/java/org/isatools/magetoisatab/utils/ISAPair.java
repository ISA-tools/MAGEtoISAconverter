package org.isatools.magetoisatab.utils;

public class ISAPair<V, T> {

    public V fst;
    public T snd;

    public ISAPair(V fst, T snd) {
        this.fst = fst;
        this.snd = snd;
    }

    public V getFst() {
        return fst;
    }

    public T getSnd() {
        return snd;
    }
}
