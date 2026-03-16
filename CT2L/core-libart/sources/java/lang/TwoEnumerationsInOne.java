package java.lang;

import java.net.URL;
import java.util.Enumeration;

class TwoEnumerationsInOne implements Enumeration<URL> {
    private final Enumeration<URL> first;
    private final Enumeration<URL> second;

    public TwoEnumerationsInOne(Enumeration<URL> first, Enumeration<URL> second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public boolean hasMoreElements() {
        return this.first.hasMoreElements() || this.second.hasMoreElements();
    }

    @Override
    public URL nextElement() {
        return this.first.hasMoreElements() ? this.first.nextElement() : this.second.nextElement();
    }
}
