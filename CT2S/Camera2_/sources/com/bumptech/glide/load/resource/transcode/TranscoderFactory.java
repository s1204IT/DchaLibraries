package com.bumptech.glide.load.resource.transcode;

import java.util.HashMap;
import java.util.Map;

public class TranscoderFactory {
    private static final MultiClassKey GET_KEY = new MultiClassKey();
    private Map<MultiClassKey, ResourceTranscoder> factories = new HashMap();

    private static class MultiClassKey {
        private Class decoded;
        private Class transcoded;

        public MultiClassKey() {
        }

        public MultiClassKey(Class decoded, Class transcoded) {
            this.decoded = decoded;
            this.transcoded = transcoded;
        }

        public void set(Class decoded, Class transcoded) {
            this.decoded = decoded;
            this.transcoded = transcoded;
        }

        public String toString() {
            return "MultiClassKey{decoded=" + this.decoded + ", transcoded=" + this.transcoded + '}';
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            MultiClassKey that = (MultiClassKey) o;
            return this.decoded.equals(that.decoded) && this.transcoded.equals(that.transcoded);
        }

        public int hashCode() {
            int result = this.decoded.hashCode();
            return (result * 31) + this.transcoded.hashCode();
        }
    }

    public <Z, R> void register(Class<Z> decodedClass, Class<R> transcodedClass, ResourceTranscoder<Z, R> factory) {
        this.factories.put(new MultiClassKey(decodedClass, transcodedClass), factory);
    }

    public <Z, R> ResourceTranscoder<Z, R> get(Class<Z> decodedClass, Class<R> transcodedClass) {
        ResourceTranscoder<Z, R> result;
        if (decodedClass.equals(transcodedClass)) {
            return UnitTranscoder.get();
        }
        synchronized (GET_KEY) {
            GET_KEY.set(decodedClass, transcodedClass);
            result = this.factories.get(GET_KEY);
        }
        if (result == null) {
            throw new IllegalArgumentException("No transcoder registered for " + decodedClass + " and " + transcodedClass);
        }
        return result;
    }
}
