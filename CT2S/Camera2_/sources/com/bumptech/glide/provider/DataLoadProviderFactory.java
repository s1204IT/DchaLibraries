package com.bumptech.glide.provider;

import com.bumptech.glide.DataLoadProvider;
import java.util.HashMap;
import java.util.Map;

public class DataLoadProviderFactory {
    private static final MultiClassKey GET_KEY = new MultiClassKey();
    private final Map<MultiClassKey, DataLoadProvider> providers = new HashMap();

    private static class MultiClassKey {
        private Class dataClass;
        private Class resourceClass;

        public MultiClassKey() {
        }

        public MultiClassKey(Class dataClass, Class resourceClass) {
            this.dataClass = dataClass;
            this.resourceClass = resourceClass;
        }

        public String toString() {
            return "MultiClassKey{dataClass=" + this.dataClass + ", resourceClass=" + this.resourceClass + '}';
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            MultiClassKey that = (MultiClassKey) o;
            return this.dataClass.equals(that.dataClass) && this.resourceClass.equals(that.resourceClass);
        }

        public int hashCode() {
            int result = this.dataClass.hashCode();
            return (result * 31) + this.resourceClass.hashCode();
        }

        public void set(Class dataClass, Class resourceClass) {
            this.dataClass = dataClass;
            this.resourceClass = resourceClass;
        }
    }

    public <T, Z> void register(Class<T> dataClass, Class<Z> resourceClass, DataLoadProvider provider) {
        this.providers.put(new MultiClassKey(dataClass, resourceClass), provider);
    }

    public <T, Z> DataLoadProvider<T, Z> get(Class<T> dataClass, Class<Z> resourceClass) {
        DataLoadProvider<T, Z> result;
        synchronized (GET_KEY) {
            GET_KEY.set(dataClass, resourceClass);
            result = this.providers.get(GET_KEY);
        }
        if (result == null) {
            return EmptyDataLoadProvider.get();
        }
        return result;
    }
}
