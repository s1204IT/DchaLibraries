package com.google.common.collect;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.Enum;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class EnumHashBiMap<K extends Enum<K>, V> extends AbstractBiMap<K, V> {
    private static final long serialVersionUID = 0;
    private transient Class<K> keyType;

    @Override
    public void clear() {
        super.clear();
    }

    @Override
    public boolean containsValue(Object obj) {
        return super.containsValue(obj);
    }

    @Override
    public Set entrySet() {
        return super.entrySet();
    }

    @Override
    public BiMap inverse() {
        return super.inverse();
    }

    @Override
    public Set keySet() {
        return super.keySet();
    }

    @Override
    public void putAll(Map map) {
        super.putAll(map);
    }

    @Override
    public Object remove(Object obj) {
        return super.remove(obj);
    }

    @Override
    public Set values() {
        return super.values();
    }

    public static <K extends Enum<K>, V> EnumHashBiMap<K, V> create(Class<K> cls) {
        return new EnumHashBiMap<>(cls);
    }

    public static <K extends Enum<K>, V> EnumHashBiMap<K, V> create(Map<K, ? extends V> map) {
        EnumHashBiMap<K, V> enumHashBiMapCreate = create(EnumBiMap.inferKeyType(map));
        enumHashBiMapCreate.putAll(map);
        return enumHashBiMapCreate;
    }

    private EnumHashBiMap(Class<K> cls) {
        super(WellBehavedMap.wrap(new EnumMap(cls)), Maps.newHashMapWithExpectedSize(cls.getEnumConstants().length));
        this.keyType = cls;
    }

    @Override
    K checkKey(K k) {
        return (K) Preconditions.checkNotNull(k);
    }

    @Override
    public V put(K k, V v) {
        return (V) super.put(k, (Object) v);
    }

    @Override
    public V forcePut(K k, V v) {
        return (V) super.forcePut(k, (Object) v);
    }

    public Class<K> keyType() {
        return this.keyType;
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.defaultWriteObject();
        objectOutputStream.writeObject(this.keyType);
        Serialization.writeMap(this, objectOutputStream);
    }

    private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        objectInputStream.defaultReadObject();
        this.keyType = (Class) objectInputStream.readObject();
        setDelegates(WellBehavedMap.wrap(new EnumMap(this.keyType)), new HashMap((this.keyType.getEnumConstants().length * 3) / 2));
        Serialization.populateMap(this, objectInputStream);
    }
}
