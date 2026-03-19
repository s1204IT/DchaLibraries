package android.icu.impl;

public class Pair<F, S> {
    public final F first;
    public final S second;

    protected Pair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    public static <F, S> Pair<F, S> of(F first, S second) {
        if (first == null || second == null) {
            throw new IllegalArgumentException("Pair.of requires non null values.");
        }
        return new Pair<>(first, second);
    }

    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof Pair)) {
            return false;
        }
        Pair<?, ?> rhs = (Pair) other;
        if (this.first.equals(rhs.first)) {
            return this.second.equals(rhs.second);
        }
        return false;
    }

    public int hashCode() {
        return (this.first.hashCode() * 37) + this.second.hashCode();
    }
}
