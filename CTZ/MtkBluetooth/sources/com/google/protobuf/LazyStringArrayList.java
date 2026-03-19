package com.google.protobuf;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.RandomAccess;

public class LazyStringArrayList extends AbstractProtobufList<String> implements LazyStringList, RandomAccess {
    public static final LazyStringList EMPTY;
    private static final LazyStringArrayList EMPTY_LIST = new LazyStringArrayList();
    private final List<Object> list;

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean isModifiable() {
        return super.isModifiable();
    }

    @Override
    public boolean remove(Object obj) {
        return super.remove(obj);
    }

    @Override
    public boolean removeAll(Collection collection) {
        return super.removeAll(collection);
    }

    @Override
    public boolean retainAll(Collection collection) {
        return super.retainAll(collection);
    }

    static {
        EMPTY_LIST.makeImmutable();
        EMPTY = EMPTY_LIST;
    }

    static LazyStringArrayList emptyList() {
        return EMPTY_LIST;
    }

    public LazyStringArrayList() {
        this(10);
    }

    public LazyStringArrayList(int i) {
        this((ArrayList<Object>) new ArrayList(i));
    }

    public LazyStringArrayList(LazyStringList lazyStringList) {
        this.list = new ArrayList(lazyStringList.size());
        addAll(lazyStringList);
    }

    public LazyStringArrayList(List<String> list) {
        this((ArrayList<Object>) new ArrayList(list));
    }

    private LazyStringArrayList(ArrayList<Object> arrayList) {
        this.list = arrayList;
    }

    @Override
    public LazyStringArrayList mutableCopyWithCapacity2(int i) {
        if (i < size()) {
            throw new IllegalArgumentException();
        }
        ArrayList arrayList = new ArrayList(i);
        arrayList.addAll(this.list);
        return new LazyStringArrayList((ArrayList<Object>) arrayList);
    }

    @Override
    public String get(int i) {
        ?? r0 = this.list.get(i);
        if (r0 instanceof String) {
            return r0;
        }
        if (r0 instanceof ByteString) {
            String stringUtf8 = r0.toStringUtf8();
            if (r0.isValidUtf8()) {
                this.list.set(i, stringUtf8);
            }
            return stringUtf8;
        }
        byte[] bArr = (byte[]) r0;
        String stringUtf82 = Internal.toStringUtf8(bArr);
        if (Internal.isValidUtf8(bArr)) {
            this.list.set(i, stringUtf82);
        }
        return stringUtf82;
    }

    @Override
    public int size() {
        return this.list.size();
    }

    @Override
    public String set(int i, String str) {
        ensureIsMutable();
        return asString(this.list.set(i, str));
    }

    @Override
    public void add(int i, String str) {
        ensureIsMutable();
        this.list.add(i, str);
        this.modCount++;
    }

    private void add(int i, ByteString byteString) {
        ensureIsMutable();
        this.list.add(i, byteString);
        this.modCount++;
    }

    private void add(int i, byte[] bArr) {
        ensureIsMutable();
        this.list.add(i, bArr);
        this.modCount++;
    }

    @Override
    public boolean addAll(Collection<? extends String> collection) {
        return addAll(size(), collection);
    }

    @Override
    public boolean addAll(int i, Collection<? extends String> collection) {
        ensureIsMutable();
        if (collection instanceof LazyStringList) {
            collection = ((LazyStringList) collection).getUnderlyingElements();
        }
        boolean zAddAll = this.list.addAll(i, collection);
        ((AbstractList) this).modCount++;
        return zAddAll;
    }

    @Override
    public boolean addAllByteString(Collection<? extends ByteString> collection) {
        ensureIsMutable();
        boolean zAddAll = this.list.addAll(collection);
        this.modCount++;
        return zAddAll;
    }

    @Override
    public boolean addAllByteArray(Collection<byte[]> collection) {
        ensureIsMutable();
        boolean zAddAll = this.list.addAll(collection);
        this.modCount++;
        return zAddAll;
    }

    @Override
    public String remove(int i) {
        ensureIsMutable();
        Object objRemove = this.list.remove(i);
        ((AbstractList) this).modCount++;
        return asString(objRemove);
    }

    @Override
    public void clear() {
        ensureIsMutable();
        this.list.clear();
        ((AbstractList) this).modCount++;
    }

    @Override
    public void add(ByteString byteString) {
        ensureIsMutable();
        this.list.add(byteString);
        this.modCount++;
    }

    @Override
    public void add(byte[] bArr) {
        ensureIsMutable();
        this.list.add(bArr);
        this.modCount++;
    }

    @Override
    public Object getRaw(int i) {
        return this.list.get(i);
    }

    @Override
    public ByteString getByteString(int i) {
        Object obj = this.list.get(i);
        ByteString byteStringAsByteString = asByteString(obj);
        if (byteStringAsByteString != obj) {
            this.list.set(i, byteStringAsByteString);
        }
        return byteStringAsByteString;
    }

    @Override
    public byte[] getByteArray(int i) {
        Object obj = this.list.get(i);
        byte[] bArrAsByteArray = asByteArray(obj);
        if (bArrAsByteArray != obj) {
            this.list.set(i, bArrAsByteArray);
        }
        return bArrAsByteArray;
    }

    @Override
    public void set(int i, ByteString byteString) {
        setAndReturn(i, byteString);
    }

    private Object setAndReturn(int i, ByteString byteString) {
        ensureIsMutable();
        return this.list.set(i, byteString);
    }

    @Override
    public void set(int i, byte[] bArr) {
        setAndReturn(i, bArr);
    }

    private Object setAndReturn(int i, byte[] bArr) {
        ensureIsMutable();
        return this.list.set(i, bArr);
    }

    private static String asString(Object obj) {
        return obj instanceof String ? obj : obj instanceof ByteString ? obj.toStringUtf8() : Internal.toStringUtf8((byte[]) obj);
    }

    private static ByteString asByteString(Object obj) {
        return obj instanceof ByteString ? obj : obj instanceof String ? ByteString.copyFromUtf8(obj) : ByteString.copyFrom((byte[]) obj);
    }

    private static byte[] asByteArray(Object obj) {
        return obj instanceof byte[] ? obj : obj instanceof String ? Internal.toByteArray(obj) : ((ByteString) obj).toByteArray();
    }

    @Override
    public List<?> getUnderlyingElements() {
        return Collections.unmodifiableList(this.list);
    }

    @Override
    public void mergeFrom(LazyStringList lazyStringList) {
        ensureIsMutable();
        for (?? r0 : lazyStringList.getUnderlyingElements()) {
            if (r0 instanceof byte[]) {
                this.list.add(Arrays.copyOf((byte[]) r0, r0.length));
            } else {
                this.list.add((Object) r0);
            }
        }
    }

    private static class ByteArrayListView extends AbstractList<byte[]> implements RandomAccess {
        private final LazyStringArrayList list;

        ByteArrayListView(LazyStringArrayList lazyStringArrayList) {
            this.list = lazyStringArrayList;
        }

        @Override
        public byte[] get(int i) {
            return this.list.getByteArray(i);
        }

        @Override
        public int size() {
            return this.list.size();
        }

        @Override
        public byte[] set(int i, byte[] bArr) {
            Object andReturn = this.list.setAndReturn(i, bArr);
            ((AbstractList) this).modCount++;
            return LazyStringArrayList.asByteArray(andReturn);
        }

        @Override
        public void add(int i, byte[] bArr) {
            this.list.add(i, bArr);
            ((AbstractList) this).modCount++;
        }

        @Override
        public byte[] remove(int i) {
            String strRemove = this.list.remove(i);
            ((AbstractList) this).modCount++;
            return LazyStringArrayList.asByteArray(strRemove);
        }
    }

    @Override
    public List<byte[]> asByteArrayList() {
        return new ByteArrayListView(this);
    }

    private static class ByteStringListView extends AbstractList<ByteString> implements RandomAccess {
        private final LazyStringArrayList list;

        ByteStringListView(LazyStringArrayList lazyStringArrayList) {
            this.list = lazyStringArrayList;
        }

        @Override
        public ByteString get(int i) {
            return this.list.getByteString(i);
        }

        @Override
        public int size() {
            return this.list.size();
        }

        @Override
        public ByteString set(int i, ByteString byteString) {
            Object andReturn = this.list.setAndReturn(i, byteString);
            ((AbstractList) this).modCount++;
            return LazyStringArrayList.asByteString(andReturn);
        }

        @Override
        public void add(int i, ByteString byteString) {
            this.list.add(i, byteString);
            ((AbstractList) this).modCount++;
        }

        @Override
        public ByteString remove(int i) {
            String strRemove = this.list.remove(i);
            ((AbstractList) this).modCount++;
            return LazyStringArrayList.asByteString(strRemove);
        }
    }

    @Override
    public List<ByteString> asByteStringList() {
        return new ByteStringListView(this);
    }

    @Override
    public LazyStringList getUnmodifiableView() {
        if (isModifiable()) {
            return new UnmodifiableLazyStringList(this);
        }
        return this;
    }
}
