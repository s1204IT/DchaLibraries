package com.google.protobuf;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;

public class UnmodifiableLazyStringList extends AbstractList<String> implements LazyStringList, RandomAccess {
    private final LazyStringList list;

    public UnmodifiableLazyStringList(LazyStringList lazyStringList) {
        this.list = lazyStringList;
    }

    @Override
    public String get(int i) {
        return (String) this.list.get(i);
    }

    @Override
    public Object getRaw(int i) {
        return this.list.getRaw(i);
    }

    @Override
    public int size() {
        return this.list.size();
    }

    @Override
    public ByteString getByteString(int i) {
        return this.list.getByteString(i);
    }

    @Override
    public void add(ByteString byteString) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void set(int i, ByteString byteString) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAllByteString(Collection<? extends ByteString> collection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] getByteArray(int i) {
        return this.list.getByteArray(i);
    }

    @Override
    public void add(byte[] bArr) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void set(int i, byte[] bArr) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAllByteArray(Collection<byte[]> collection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListIterator<String> listIterator(final int i) {
        return new ListIterator<String>() {
            ListIterator<String> iter;

            {
                this.iter = UnmodifiableLazyStringList.this.list.listIterator(i);
            }

            @Override
            public boolean hasNext() {
                return this.iter.hasNext();
            }

            @Override
            public String next() {
                return this.iter.next();
            }

            @Override
            public boolean hasPrevious() {
                return this.iter.hasPrevious();
            }

            @Override
            public String previous() {
                return this.iter.previous();
            }

            @Override
            public int nextIndex() {
                return this.iter.nextIndex();
            }

            @Override
            public int previousIndex() {
                return this.iter.previousIndex();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void set(String str) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void add(String str) {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public Iterator<String> iterator() {
        return new Iterator<String>() {
            Iterator<String> iter;

            {
                this.iter = UnmodifiableLazyStringList.this.list.iterator();
            }

            @Override
            public boolean hasNext() {
                return this.iter.hasNext();
            }

            @Override
            public String next() {
                return this.iter.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public List<?> getUnderlyingElements() {
        return this.list.getUnderlyingElements();
    }

    @Override
    public void mergeFrom(LazyStringList lazyStringList) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<byte[]> asByteArrayList() {
        return Collections.unmodifiableList(this.list.asByteArrayList());
    }

    @Override
    public List<ByteString> asByteStringList() {
        return Collections.unmodifiableList(this.list.asByteStringList());
    }

    @Override
    public LazyStringList getUnmodifiableView() {
        return this;
    }
}
