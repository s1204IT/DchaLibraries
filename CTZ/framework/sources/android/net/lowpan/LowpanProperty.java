package android.net.lowpan;

import java.util.Map;

public abstract class LowpanProperty<T> {
    public abstract String getName();

    public abstract Class<T> getType();

    public void putInMap(Map map, T t) {
        map.put(getName(), t);
    }

    public T getFromMap(Map map) {
        return (T) map.get(getName());
    }
}
