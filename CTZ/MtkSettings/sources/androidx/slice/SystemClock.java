package androidx.slice;

public class SystemClock implements Clock {
    @Override
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
