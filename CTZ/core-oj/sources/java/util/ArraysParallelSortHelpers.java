package java.util;

import java.util.concurrent.CountedCompleter;

class ArraysParallelSortHelpers {
    ArraysParallelSortHelpers() {
    }

    static final class EmptyCompleter extends CountedCompleter<Void> {
        static final long serialVersionUID = 2446542900576103244L;

        EmptyCompleter(CountedCompleter<?> countedCompleter) {
            super(countedCompleter);
        }

        @Override
        public final void compute() {
        }
    }

    static final class Relay extends CountedCompleter<Void> {
        static final long serialVersionUID = 2446542900576103244L;
        final CountedCompleter<?> task;

        Relay(CountedCompleter<?> countedCompleter) {
            super(null, 1);
            this.task = countedCompleter;
        }

        @Override
        public final void compute() {
        }

        @Override
        public final void onCompletion(CountedCompleter<?> countedCompleter) {
            this.task.compute();
        }
    }

    static final class FJObject {
        FJObject() {
        }

        static final class Sorter<T> extends CountedCompleter<Void> {
            static final long serialVersionUID = 2446542900576103244L;
            final T[] a;
            final int base;
            Comparator<? super T> comparator;
            final int gran;
            final int size;
            final T[] w;
            final int wbase;

            Sorter(CountedCompleter<?> countedCompleter, T[] tArr, T[] tArr2, int i, int i2, int i3, int i4, Comparator<? super T> comparator) {
                super(countedCompleter);
                this.a = tArr;
                this.w = tArr2;
                this.base = i;
                this.size = i2;
                this.wbase = i3;
                this.gran = i4;
                this.comparator = comparator;
            }

            @Override
            public final void compute() {
                Comparator<? super T> comparator = this.comparator;
                T[] tArr = this.a;
                T[] tArr2 = this.w;
                int i = this.base;
                int i2 = this.size;
                int i3 = this.wbase;
                int i4 = this.gran;
                CountedCompleter emptyCompleter = this;
                int i5 = i2;
                while (i5 > i4) {
                    int i6 = i5 >>> 1;
                    int i7 = i6 >>> 1;
                    int i8 = i6 + i7;
                    int i9 = i3 + i6;
                    int i10 = i3;
                    T[] tArr3 = tArr2;
                    int i11 = i4;
                    Relay relay = new Relay(new Merger(emptyCompleter, tArr2, tArr, i3, i6, i9, i5 - i6, i, i4, comparator));
                    int i12 = i + i6;
                    int i13 = i + i8;
                    int i14 = i5 - i8;
                    Relay relay2 = new Relay(new Merger(relay, tArr, tArr3, i12, i7, i13, i14, i9, i11, comparator));
                    new Sorter(relay2, tArr, tArr3, i13, i14, i10 + i8, i11, comparator).fork();
                    new Sorter(relay2, tArr, tArr3, i12, i7, i9, i11, comparator).fork();
                    int i15 = i + i7;
                    int i16 = i6 - i7;
                    Relay relay3 = new Relay(new Merger(relay, tArr, tArr3, i, i7, i15, i16, i10, i11, comparator));
                    new Sorter(relay3, tArr, tArr3, i15, i16, i10 + i7, i11, comparator).fork();
                    emptyCompleter = new EmptyCompleter(relay3);
                    i5 = i7;
                    i3 = i10;
                    tArr2 = tArr3;
                    i4 = i11;
                }
                int i17 = i5;
                TimSort.sort(tArr, i, i + i17, comparator, tArr2, i3, i17);
                emptyCompleter.tryComplete();
            }
        }

        static final class Merger<T> extends CountedCompleter<Void> {
            static final long serialVersionUID = 2446542900576103244L;
            final T[] a;
            Comparator<? super T> comparator;
            final int gran;
            final int lbase;
            final int lsize;
            final int rbase;
            final int rsize;
            final T[] w;
            final int wbase;

            Merger(CountedCompleter<?> countedCompleter, T[] tArr, T[] tArr2, int i, int i2, int i3, int i4, int i5, int i6, Comparator<? super T> comparator) {
                super(countedCompleter);
                this.a = tArr;
                this.w = tArr2;
                this.lbase = i;
                this.lsize = i2;
                this.rbase = i3;
                this.rsize = i4;
                this.wbase = i5;
                this.gran = i6;
                this.comparator = comparator;
            }

            @Override
            public final void compute() {
                int i;
                int i2;
                Comparator<? super T> comparator = this.comparator;
                Object[] objArr = this.a;
                Object[] objArr2 = this.w;
                int i3 = this.lbase;
                int i4 = this.lsize;
                int i5 = this.rbase;
                int i6 = this.rsize;
                int i7 = this.wbase;
                int i8 = this.gran;
                if (objArr == null || objArr2 == null || i3 < 0 || i5 < 0 || i7 < 0 || comparator == null) {
                    throw new IllegalStateException();
                }
                while (true) {
                    int i9 = 0;
                    int i10 = 1;
                    if (i4 >= i6) {
                        if (i4 <= i8) {
                            break;
                        }
                        int i11 = i4 >>> 1;
                        Object obj = objArr[i11 + i3];
                        int i12 = i6;
                        while (i9 < i12) {
                            int i13 = (i9 + i12) >>> i10;
                            if (comparator.compare(obj, objArr[i13 + i5]) > 0) {
                                i9 = i13 + 1;
                            } else {
                                i12 = i13;
                            }
                            i10 = 1;
                        }
                        i2 = i11;
                        i = i12;
                        int i14 = i8;
                        Merger merger = new Merger(this, objArr, objArr2, i3 + i2, i4 - i2, i5 + i, i6 - i, i7 + i2 + i, i14, comparator);
                        addToPendingCount(1);
                        merger.fork();
                        i4 = i2;
                        i6 = i;
                        i8 = i14;
                        i7 = i7;
                        i5 = i5;
                        objArr = objArr;
                        objArr2 = objArr2;
                    } else {
                        if (i6 <= i8) {
                            break;
                        }
                        int i15 = i6 >>> 1;
                        Object obj2 = objArr[i15 + i5];
                        int i16 = i4;
                        while (i9 < i16) {
                            int i17 = (i9 + i16) >>> 1;
                            if (comparator.compare(obj2, objArr[i17 + i3]) > 0) {
                                i9 = i17 + 1;
                            } else {
                                i16 = i17;
                            }
                        }
                        i = i15;
                        i2 = i16;
                        int i142 = i8;
                        Merger merger2 = new Merger(this, objArr, objArr2, i3 + i2, i4 - i2, i5 + i, i6 - i, i7 + i2 + i, i142, comparator);
                        addToPendingCount(1);
                        merger2.fork();
                        i4 = i2;
                        i6 = i;
                        i8 = i142;
                        i7 = i7;
                        i5 = i5;
                        objArr = objArr;
                        objArr2 = objArr2;
                    }
                }
                int i18 = i4 + i3;
                int i19 = i6 + i5;
                while (i3 < i18 && i5 < i19) {
                    Object obj3 = objArr[i3];
                    Object obj4 = objArr[i5];
                    if (comparator.compare(obj3, obj4) <= 0) {
                        i3++;
                    } else {
                        i5++;
                        obj3 = obj4;
                    }
                    objArr2[i7] = obj3;
                    i7++;
                }
                if (i5 < i19) {
                    System.arraycopy(objArr, i5, objArr2, i7, i19 - i5);
                } else if (i3 < i18) {
                    System.arraycopy(objArr, i3, objArr2, i7, i18 - i3);
                }
                tryComplete();
            }
        }
    }

    static final class FJByte {
        FJByte() {
        }

        static final class Sorter extends CountedCompleter<Void> {
            static final long serialVersionUID = 2446542900576103244L;
            final byte[] a;
            final int base;
            final int gran;
            final int size;
            final byte[] w;
            final int wbase;

            Sorter(CountedCompleter<?> countedCompleter, byte[] bArr, byte[] bArr2, int i, int i2, int i3, int i4) {
                super(countedCompleter);
                this.a = bArr;
                this.w = bArr2;
                this.base = i;
                this.size = i2;
                this.wbase = i3;
                this.gran = i4;
            }

            @Override
            public final void compute() {
                byte[] bArr = this.a;
                byte[] bArr2 = this.w;
                int i = this.base;
                int i2 = this.size;
                int i3 = this.wbase;
                int i4 = this.gran;
                int i5 = i2;
                CountedCompleter emptyCompleter = this;
                while (i5 > i4) {
                    int i6 = i5 >>> 1;
                    int i7 = i6 >>> 1;
                    int i8 = i6 + i7;
                    int i9 = i3 + i6;
                    int i10 = i3;
                    byte[] bArr3 = bArr2;
                    Relay relay = new Relay(new Merger(emptyCompleter, bArr2, bArr, i3, i6, i9, i5 - i6, i, i4));
                    int i11 = i + i6;
                    int i12 = i + i8;
                    int i13 = i5 - i8;
                    Relay relay2 = new Relay(new Merger(relay, bArr, bArr3, i11, i7, i12, i13, i9, i4));
                    new Sorter(relay2, bArr, bArr3, i12, i13, i10 + i8, i4).fork();
                    new Sorter(relay2, bArr, bArr3, i11, i7, i9, i4).fork();
                    int i14 = i + i7;
                    int i15 = i6 - i7;
                    Relay relay3 = new Relay(new Merger(relay, bArr, bArr3, i, i7, i14, i15, i10, i4));
                    new Sorter(relay3, bArr, bArr3, i14, i15, i10 + i7, i4).fork();
                    emptyCompleter = new EmptyCompleter(relay3);
                    i5 = i7;
                    i3 = i10;
                    bArr2 = bArr3;
                }
                DualPivotQuicksort.sort(bArr, i, (i5 + i) - 1);
                emptyCompleter.tryComplete();
            }
        }

        static final class Merger extends CountedCompleter<Void> {
            static final long serialVersionUID = 2446542900576103244L;
            final byte[] a;
            final int gran;
            final int lbase;
            final int lsize;
            final int rbase;
            final int rsize;
            final byte[] w;
            final int wbase;

            Merger(CountedCompleter<?> countedCompleter, byte[] bArr, byte[] bArr2, int i, int i2, int i3, int i4, int i5, int i6) {
                super(countedCompleter);
                this.a = bArr;
                this.w = bArr2;
                this.lbase = i;
                this.lsize = i2;
                this.rbase = i3;
                this.rsize = i4;
                this.wbase = i5;
                this.gran = i6;
            }

            @Override
            public final void compute() {
                int i;
                int i2;
                byte[] bArr = this.a;
                byte[] bArr2 = this.w;
                int i3 = this.lbase;
                int i4 = this.lsize;
                int i5 = this.rbase;
                int i6 = this.rsize;
                int i7 = this.wbase;
                int i8 = this.gran;
                if (bArr == null || bArr2 == null || i3 < 0 || i5 < 0 || i7 < 0) {
                    throw new IllegalStateException();
                }
                while (true) {
                    int i9 = 0;
                    if (i4 >= i6) {
                        if (i4 <= i8) {
                            break;
                        }
                        int i10 = i4 >>> 1;
                        byte b = bArr[i10 + i3];
                        int i11 = i6;
                        while (i9 < i11) {
                            int i12 = (i9 + i11) >>> 1;
                            if (b > bArr[i12 + i5]) {
                                i9 = i12 + 1;
                            } else {
                                i11 = i12;
                            }
                        }
                        i2 = i10;
                        i = i11;
                        Merger merger = new Merger(this, bArr, bArr2, i3 + i2, i4 - i2, i5 + i, i6 - i, i7 + i2 + i, i8);
                        addToPendingCount(1);
                        merger.fork();
                        i4 = i2;
                        i6 = i;
                        bArr = bArr;
                        bArr2 = bArr2;
                    } else {
                        if (i6 <= i8) {
                            break;
                        }
                        int i13 = i6 >>> 1;
                        byte b2 = bArr[i13 + i5];
                        int i14 = i4;
                        while (i9 < i14) {
                            int i15 = (i9 + i14) >>> 1;
                            if (b2 > bArr[i15 + i3]) {
                                i9 = i15 + 1;
                            } else {
                                i14 = i15;
                            }
                        }
                        i = i13;
                        i2 = i14;
                        Merger merger2 = new Merger(this, bArr, bArr2, i3 + i2, i4 - i2, i5 + i, i6 - i, i7 + i2 + i, i8);
                        addToPendingCount(1);
                        merger2.fork();
                        i4 = i2;
                        i6 = i;
                        bArr = bArr;
                        bArr2 = bArr2;
                    }
                }
                int i16 = i4 + i3;
                int i17 = i6 + i5;
                while (i3 < i16 && i5 < i17) {
                    byte b3 = bArr[i3];
                    byte b4 = bArr[i5];
                    if (b3 <= b4) {
                        i3++;
                    } else {
                        i5++;
                        b3 = b4;
                    }
                    bArr2[i7] = b3;
                    i7++;
                }
                if (i5 < i17) {
                    System.arraycopy(bArr, i5, bArr2, i7, i17 - i5);
                } else if (i3 < i16) {
                    System.arraycopy(bArr, i3, bArr2, i7, i16 - i3);
                }
                tryComplete();
            }
        }
    }

    static final class FJChar {
        FJChar() {
        }

        static final class Sorter extends CountedCompleter<Void> {
            static final long serialVersionUID = 2446542900576103244L;
            final char[] a;
            final int base;
            final int gran;
            final int size;
            final char[] w;
            final int wbase;

            Sorter(CountedCompleter<?> countedCompleter, char[] cArr, char[] cArr2, int i, int i2, int i3, int i4) {
                super(countedCompleter);
                this.a = cArr;
                this.w = cArr2;
                this.base = i;
                this.size = i2;
                this.wbase = i3;
                this.gran = i4;
            }

            @Override
            public final void compute() {
                char[] cArr = this.a;
                char[] cArr2 = this.w;
                int i = this.base;
                int i2 = this.size;
                int i3 = this.wbase;
                int i4 = this.gran;
                CountedCompleter emptyCompleter = this;
                int i5 = i2;
                while (i5 > i4) {
                    int i6 = i5 >>> 1;
                    int i7 = i6 >>> 1;
                    int i8 = i6 + i7;
                    int i9 = i3 + i6;
                    int i10 = i3;
                    char[] cArr3 = cArr2;
                    Relay relay = new Relay(new Merger(emptyCompleter, cArr2, cArr, i3, i6, i9, i5 - i6, i, i4));
                    int i11 = i + i6;
                    int i12 = i + i8;
                    int i13 = i5 - i8;
                    Relay relay2 = new Relay(new Merger(relay, cArr, cArr3, i11, i7, i12, i13, i9, i4));
                    new Sorter(relay2, cArr, cArr3, i12, i13, i10 + i8, i4).fork();
                    new Sorter(relay2, cArr, cArr3, i11, i7, i9, i4).fork();
                    int i14 = i + i7;
                    int i15 = i6 - i7;
                    Relay relay3 = new Relay(new Merger(relay, cArr, cArr3, i, i7, i14, i15, i10, i4));
                    new Sorter(relay3, cArr, cArr3, i14, i15, i10 + i7, i4).fork();
                    emptyCompleter = new EmptyCompleter(relay3);
                    i5 = i7;
                    i3 = i10;
                    cArr2 = cArr3;
                }
                DualPivotQuicksort.sort(cArr, i, (i + i5) - 1, cArr2, i3, i5);
                emptyCompleter.tryComplete();
            }
        }

        static final class Merger extends CountedCompleter<Void> {
            static final long serialVersionUID = 2446542900576103244L;
            final char[] a;
            final int gran;
            final int lbase;
            final int lsize;
            final int rbase;
            final int rsize;
            final char[] w;
            final int wbase;

            Merger(CountedCompleter<?> countedCompleter, char[] cArr, char[] cArr2, int i, int i2, int i3, int i4, int i5, int i6) {
                super(countedCompleter);
                this.a = cArr;
                this.w = cArr2;
                this.lbase = i;
                this.lsize = i2;
                this.rbase = i3;
                this.rsize = i4;
                this.wbase = i5;
                this.gran = i6;
            }

            @Override
            public final void compute() {
                int i;
                int i2;
                char[] cArr = this.a;
                char[] cArr2 = this.w;
                int i3 = this.lbase;
                int i4 = this.lsize;
                int i5 = this.rbase;
                int i6 = this.rsize;
                int i7 = this.wbase;
                int i8 = this.gran;
                if (cArr == null || cArr2 == null || i3 < 0 || i5 < 0 || i7 < 0) {
                    throw new IllegalStateException();
                }
                while (true) {
                    int i9 = 0;
                    if (i4 >= i6) {
                        if (i4 <= i8) {
                            break;
                        }
                        int i10 = i4 >>> 1;
                        char c = cArr[i10 + i3];
                        int i11 = i6;
                        while (i9 < i11) {
                            int i12 = (i9 + i11) >>> 1;
                            if (c > cArr[i12 + i5]) {
                                i9 = i12 + 1;
                            } else {
                                i11 = i12;
                            }
                        }
                        i2 = i10;
                        i = i11;
                        Merger merger = new Merger(this, cArr, cArr2, i3 + i2, i4 - i2, i5 + i, i6 - i, i7 + i2 + i, i8);
                        addToPendingCount(1);
                        merger.fork();
                        i4 = i2;
                        i6 = i;
                        cArr = cArr;
                        cArr2 = cArr2;
                    } else {
                        if (i6 <= i8) {
                            break;
                        }
                        int i13 = i6 >>> 1;
                        char c2 = cArr[i13 + i5];
                        int i14 = i4;
                        while (i9 < i14) {
                            int i15 = (i9 + i14) >>> 1;
                            if (c2 > cArr[i15 + i3]) {
                                i9 = i15 + 1;
                            } else {
                                i14 = i15;
                            }
                        }
                        i = i13;
                        i2 = i14;
                        Merger merger2 = new Merger(this, cArr, cArr2, i3 + i2, i4 - i2, i5 + i, i6 - i, i7 + i2 + i, i8);
                        addToPendingCount(1);
                        merger2.fork();
                        i4 = i2;
                        i6 = i;
                        cArr = cArr;
                        cArr2 = cArr2;
                    }
                }
                int i16 = i4 + i3;
                int i17 = i6 + i5;
                while (i3 < i16 && i5 < i17) {
                    char c3 = cArr[i3];
                    char c4 = cArr[i5];
                    if (c3 <= c4) {
                        i3++;
                    } else {
                        i5++;
                        c3 = c4;
                    }
                    cArr2[i7] = c3;
                    i7++;
                }
                if (i5 < i17) {
                    System.arraycopy((Object) cArr, i5, (Object) cArr2, i7, i17 - i5);
                } else if (i3 < i16) {
                    System.arraycopy((Object) cArr, i3, (Object) cArr2, i7, i16 - i3);
                }
                tryComplete();
            }
        }
    }

    static final class FJShort {
        FJShort() {
        }

        static final class Sorter extends CountedCompleter<Void> {
            static final long serialVersionUID = 2446542900576103244L;
            final short[] a;
            final int base;
            final int gran;
            final int size;
            final short[] w;
            final int wbase;

            Sorter(CountedCompleter<?> countedCompleter, short[] sArr, short[] sArr2, int i, int i2, int i3, int i4) {
                super(countedCompleter);
                this.a = sArr;
                this.w = sArr2;
                this.base = i;
                this.size = i2;
                this.wbase = i3;
                this.gran = i4;
            }

            @Override
            public final void compute() {
                short[] sArr = this.a;
                short[] sArr2 = this.w;
                int i = this.base;
                int i2 = this.size;
                int i3 = this.wbase;
                int i4 = this.gran;
                CountedCompleter emptyCompleter = this;
                int i5 = i2;
                while (i5 > i4) {
                    int i6 = i5 >>> 1;
                    int i7 = i6 >>> 1;
                    int i8 = i6 + i7;
                    int i9 = i3 + i6;
                    int i10 = i3;
                    short[] sArr3 = sArr2;
                    Relay relay = new Relay(new Merger(emptyCompleter, sArr2, sArr, i3, i6, i9, i5 - i6, i, i4));
                    int i11 = i + i6;
                    int i12 = i + i8;
                    int i13 = i5 - i8;
                    Relay relay2 = new Relay(new Merger(relay, sArr, sArr3, i11, i7, i12, i13, i9, i4));
                    new Sorter(relay2, sArr, sArr3, i12, i13, i10 + i8, i4).fork();
                    new Sorter(relay2, sArr, sArr3, i11, i7, i9, i4).fork();
                    int i14 = i + i7;
                    int i15 = i6 - i7;
                    Relay relay3 = new Relay(new Merger(relay, sArr, sArr3, i, i7, i14, i15, i10, i4));
                    new Sorter(relay3, sArr, sArr3, i14, i15, i10 + i7, i4).fork();
                    emptyCompleter = new EmptyCompleter(relay3);
                    i5 = i7;
                    i3 = i10;
                    sArr2 = sArr3;
                }
                DualPivotQuicksort.sort(sArr, i, (i + i5) - 1, sArr2, i3, i5);
                emptyCompleter.tryComplete();
            }
        }

        static final class Merger extends CountedCompleter<Void> {
            static final long serialVersionUID = 2446542900576103244L;
            final short[] a;
            final int gran;
            final int lbase;
            final int lsize;
            final int rbase;
            final int rsize;
            final short[] w;
            final int wbase;

            Merger(CountedCompleter<?> countedCompleter, short[] sArr, short[] sArr2, int i, int i2, int i3, int i4, int i5, int i6) {
                super(countedCompleter);
                this.a = sArr;
                this.w = sArr2;
                this.lbase = i;
                this.lsize = i2;
                this.rbase = i3;
                this.rsize = i4;
                this.wbase = i5;
                this.gran = i6;
            }

            @Override
            public final void compute() {
                int i;
                int i2;
                short[] sArr = this.a;
                short[] sArr2 = this.w;
                int i3 = this.lbase;
                int i4 = this.lsize;
                int i5 = this.rbase;
                int i6 = this.rsize;
                int i7 = this.wbase;
                int i8 = this.gran;
                if (sArr == null || sArr2 == null || i3 < 0 || i5 < 0 || i7 < 0) {
                    throw new IllegalStateException();
                }
                while (true) {
                    int i9 = 0;
                    if (i4 >= i6) {
                        if (i4 <= i8) {
                            break;
                        }
                        int i10 = i4 >>> 1;
                        short s = sArr[i10 + i3];
                        int i11 = i6;
                        while (i9 < i11) {
                            int i12 = (i9 + i11) >>> 1;
                            if (s > sArr[i12 + i5]) {
                                i9 = i12 + 1;
                            } else {
                                i11 = i12;
                            }
                        }
                        i2 = i10;
                        i = i11;
                        Merger merger = new Merger(this, sArr, sArr2, i3 + i2, i4 - i2, i5 + i, i6 - i, i7 + i2 + i, i8);
                        addToPendingCount(1);
                        merger.fork();
                        i4 = i2;
                        i6 = i;
                        sArr = sArr;
                        sArr2 = sArr2;
                    } else {
                        if (i6 <= i8) {
                            break;
                        }
                        int i13 = i6 >>> 1;
                        short s2 = sArr[i13 + i5];
                        int i14 = i4;
                        while (i9 < i14) {
                            int i15 = (i9 + i14) >>> 1;
                            if (s2 > sArr[i15 + i3]) {
                                i9 = i15 + 1;
                            } else {
                                i14 = i15;
                            }
                        }
                        i = i13;
                        i2 = i14;
                        Merger merger2 = new Merger(this, sArr, sArr2, i3 + i2, i4 - i2, i5 + i, i6 - i, i7 + i2 + i, i8);
                        addToPendingCount(1);
                        merger2.fork();
                        i4 = i2;
                        i6 = i;
                        sArr = sArr;
                        sArr2 = sArr2;
                    }
                }
                int i16 = i4 + i3;
                int i17 = i6 + i5;
                while (i3 < i16 && i5 < i17) {
                    short s3 = sArr[i3];
                    short s4 = sArr[i5];
                    if (s3 <= s4) {
                        i3++;
                    } else {
                        i5++;
                        s3 = s4;
                    }
                    sArr2[i7] = s3;
                    i7++;
                }
                if (i5 < i17) {
                    System.arraycopy((Object) sArr, i5, (Object) sArr2, i7, i17 - i5);
                } else if (i3 < i16) {
                    System.arraycopy((Object) sArr, i3, (Object) sArr2, i7, i16 - i3);
                }
                tryComplete();
            }
        }
    }

    static final class FJInt {
        FJInt() {
        }

        static final class Sorter extends CountedCompleter<Void> {
            static final long serialVersionUID = 2446542900576103244L;
            final int[] a;
            final int base;
            final int gran;
            final int size;
            final int[] w;
            final int wbase;

            Sorter(CountedCompleter<?> countedCompleter, int[] iArr, int[] iArr2, int i, int i2, int i3, int i4) {
                super(countedCompleter);
                this.a = iArr;
                this.w = iArr2;
                this.base = i;
                this.size = i2;
                this.wbase = i3;
                this.gran = i4;
            }

            @Override
            public final void compute() {
                int[] iArr = this.a;
                int[] iArr2 = this.w;
                int i = this.base;
                int i2 = this.size;
                int i3 = this.wbase;
                int i4 = this.gran;
                CountedCompleter emptyCompleter = this;
                int i5 = i2;
                while (i5 > i4) {
                    int i6 = i5 >>> 1;
                    int i7 = i6 >>> 1;
                    int i8 = i6 + i7;
                    int i9 = i3 + i6;
                    int i10 = i3;
                    int[] iArr3 = iArr2;
                    Relay relay = new Relay(new Merger(emptyCompleter, iArr2, iArr, i3, i6, i9, i5 - i6, i, i4));
                    int i11 = i + i6;
                    int i12 = i + i8;
                    int i13 = i5 - i8;
                    Relay relay2 = new Relay(new Merger(relay, iArr, iArr3, i11, i7, i12, i13, i9, i4));
                    new Sorter(relay2, iArr, iArr3, i12, i13, i10 + i8, i4).fork();
                    new Sorter(relay2, iArr, iArr3, i11, i7, i9, i4).fork();
                    int i14 = i + i7;
                    int i15 = i6 - i7;
                    Relay relay3 = new Relay(new Merger(relay, iArr, iArr3, i, i7, i14, i15, i10, i4));
                    new Sorter(relay3, iArr, iArr3, i14, i15, i10 + i7, i4).fork();
                    emptyCompleter = new EmptyCompleter(relay3);
                    i5 = i7;
                    i3 = i10;
                    iArr2 = iArr3;
                }
                DualPivotQuicksort.sort(iArr, i, (i + i5) - 1, iArr2, i3, i5);
                emptyCompleter.tryComplete();
            }
        }

        static final class Merger extends CountedCompleter<Void> {
            static final long serialVersionUID = 2446542900576103244L;
            final int[] a;
            final int gran;
            final int lbase;
            final int lsize;
            final int rbase;
            final int rsize;
            final int[] w;
            final int wbase;

            Merger(CountedCompleter<?> countedCompleter, int[] iArr, int[] iArr2, int i, int i2, int i3, int i4, int i5, int i6) {
                super(countedCompleter);
                this.a = iArr;
                this.w = iArr2;
                this.lbase = i;
                this.lsize = i2;
                this.rbase = i3;
                this.rsize = i4;
                this.wbase = i5;
                this.gran = i6;
            }

            @Override
            public final void compute() {
                int i;
                int i2;
                int[] iArr = this.a;
                int[] iArr2 = this.w;
                int i3 = this.lbase;
                int i4 = this.lsize;
                int i5 = this.rbase;
                int i6 = this.rsize;
                int i7 = this.wbase;
                int i8 = this.gran;
                if (iArr == null || iArr2 == null || i3 < 0 || i5 < 0 || i7 < 0) {
                    throw new IllegalStateException();
                }
                while (true) {
                    int i9 = 0;
                    if (i4 >= i6) {
                        if (i4 <= i8) {
                            break;
                        }
                        int i10 = i4 >>> 1;
                        int i11 = iArr[i10 + i3];
                        int i12 = i6;
                        while (i9 < i12) {
                            int i13 = (i9 + i12) >>> 1;
                            if (i11 > iArr[i13 + i5]) {
                                i9 = i13 + 1;
                            } else {
                                i12 = i13;
                            }
                        }
                        i2 = i10;
                        i = i12;
                        Merger merger = new Merger(this, iArr, iArr2, i3 + i2, i4 - i2, i5 + i, i6 - i, i7 + i2 + i, i8);
                        addToPendingCount(1);
                        merger.fork();
                        i4 = i2;
                        i6 = i;
                        iArr = iArr;
                        iArr2 = iArr2;
                    } else {
                        if (i6 <= i8) {
                            break;
                        }
                        int i14 = i6 >>> 1;
                        int i15 = iArr[i14 + i5];
                        int i16 = i4;
                        while (i9 < i16) {
                            int i17 = (i9 + i16) >>> 1;
                            if (i15 > iArr[i17 + i3]) {
                                i9 = i17 + 1;
                            } else {
                                i16 = i17;
                            }
                        }
                        i = i14;
                        i2 = i16;
                        Merger merger2 = new Merger(this, iArr, iArr2, i3 + i2, i4 - i2, i5 + i, i6 - i, i7 + i2 + i, i8);
                        addToPendingCount(1);
                        merger2.fork();
                        i4 = i2;
                        i6 = i;
                        iArr = iArr;
                        iArr2 = iArr2;
                    }
                }
                int i18 = i4 + i3;
                int i19 = i6 + i5;
                while (i3 < i18 && i5 < i19) {
                    int i20 = iArr[i3];
                    int i21 = iArr[i5];
                    if (i20 <= i21) {
                        i3++;
                    } else {
                        i5++;
                        i20 = i21;
                    }
                    iArr2[i7] = i20;
                    i7++;
                }
                if (i5 < i19) {
                    System.arraycopy((Object) iArr, i5, (Object) iArr2, i7, i19 - i5);
                } else if (i3 < i18) {
                    System.arraycopy((Object) iArr, i3, (Object) iArr2, i7, i18 - i3);
                }
                tryComplete();
            }
        }
    }

    static final class FJLong {
        FJLong() {
        }

        static final class Sorter extends CountedCompleter<Void> {
            static final long serialVersionUID = 2446542900576103244L;
            final long[] a;
            final int base;
            final int gran;
            final int size;
            final long[] w;
            final int wbase;

            Sorter(CountedCompleter<?> countedCompleter, long[] jArr, long[] jArr2, int i, int i2, int i3, int i4) {
                super(countedCompleter);
                this.a = jArr;
                this.w = jArr2;
                this.base = i;
                this.size = i2;
                this.wbase = i3;
                this.gran = i4;
            }

            @Override
            public final void compute() {
                long[] jArr = this.a;
                long[] jArr2 = this.w;
                int i = this.base;
                int i2 = this.size;
                int i3 = this.wbase;
                int i4 = this.gran;
                CountedCompleter emptyCompleter = this;
                int i5 = i2;
                while (i5 > i4) {
                    int i6 = i5 >>> 1;
                    int i7 = i6 >>> 1;
                    int i8 = i6 + i7;
                    int i9 = i3 + i6;
                    int i10 = i3;
                    long[] jArr3 = jArr2;
                    Relay relay = new Relay(new Merger(emptyCompleter, jArr2, jArr, i3, i6, i9, i5 - i6, i, i4));
                    int i11 = i + i6;
                    int i12 = i + i8;
                    int i13 = i5 - i8;
                    Relay relay2 = new Relay(new Merger(relay, jArr, jArr3, i11, i7, i12, i13, i9, i4));
                    new Sorter(relay2, jArr, jArr3, i12, i13, i10 + i8, i4).fork();
                    new Sorter(relay2, jArr, jArr3, i11, i7, i9, i4).fork();
                    int i14 = i + i7;
                    int i15 = i6 - i7;
                    Relay relay3 = new Relay(new Merger(relay, jArr, jArr3, i, i7, i14, i15, i10, i4));
                    new Sorter(relay3, jArr, jArr3, i14, i15, i10 + i7, i4).fork();
                    emptyCompleter = new EmptyCompleter(relay3);
                    i5 = i7;
                    i3 = i10;
                    jArr2 = jArr3;
                }
                DualPivotQuicksort.sort(jArr, i, (i + i5) - 1, jArr2, i3, i5);
                emptyCompleter.tryComplete();
            }
        }

        static final class Merger extends CountedCompleter<Void> {
            static final long serialVersionUID = 2446542900576103244L;
            final long[] a;
            final int gran;
            final int lbase;
            final int lsize;
            final int rbase;
            final int rsize;
            final long[] w;
            final int wbase;

            Merger(CountedCompleter<?> countedCompleter, long[] jArr, long[] jArr2, int i, int i2, int i3, int i4, int i5, int i6) {
                super(countedCompleter);
                this.a = jArr;
                this.w = jArr2;
                this.lbase = i;
                this.lsize = i2;
                this.rbase = i3;
                this.rsize = i4;
                this.wbase = i5;
                this.gran = i6;
            }

            @Override
            public final void compute() {
                int i;
                int i2;
                long[] jArr = this.a;
                long[] jArr2 = this.w;
                int i3 = this.lbase;
                int i4 = this.lsize;
                int i5 = this.rbase;
                int i6 = this.rsize;
                int i7 = this.wbase;
                int i8 = this.gran;
                if (jArr == null || jArr2 == null || i3 < 0 || i5 < 0 || i7 < 0) {
                    throw new IllegalStateException();
                }
                while (true) {
                    int i9 = 0;
                    if (i4 >= i6) {
                        if (i4 <= i8) {
                            break;
                        }
                        int i10 = i4 >>> 1;
                        long j = jArr[i10 + i3];
                        int i11 = i6;
                        while (i9 < i11) {
                            int i12 = (i9 + i11) >>> 1;
                            if (j > jArr[i12 + i5]) {
                                i9 = i12 + 1;
                            } else {
                                i11 = i12;
                            }
                        }
                        i2 = i10;
                        i = i11;
                        Merger merger = new Merger(this, jArr, jArr2, i3 + i2, i4 - i2, i5 + i, i6 - i, i7 + i2 + i, i8);
                        addToPendingCount(1);
                        merger.fork();
                        i4 = i2;
                        i6 = i;
                        jArr = jArr;
                        jArr2 = jArr2;
                    } else {
                        if (i6 <= i8) {
                            break;
                        }
                        int i13 = i6 >>> 1;
                        long j2 = jArr[i13 + i5];
                        int i14 = i4;
                        while (i9 < i14) {
                            int i15 = (i9 + i14) >>> 1;
                            if (j2 > jArr[i15 + i3]) {
                                i9 = i15 + 1;
                            } else {
                                i14 = i15;
                            }
                        }
                        i = i13;
                        i2 = i14;
                        Merger merger2 = new Merger(this, jArr, jArr2, i3 + i2, i4 - i2, i5 + i, i6 - i, i7 + i2 + i, i8);
                        addToPendingCount(1);
                        merger2.fork();
                        i4 = i2;
                        i6 = i;
                        jArr = jArr;
                        jArr2 = jArr2;
                    }
                }
                int i16 = i4 + i3;
                int i17 = i6 + i5;
                while (i3 < i16 && i5 < i17) {
                    long j3 = jArr[i3];
                    long j4 = jArr[i5];
                    if (j3 <= j4) {
                        i3++;
                    } else {
                        i5++;
                        j3 = j4;
                    }
                    jArr2[i7] = j3;
                    i7++;
                }
                if (i5 < i17) {
                    System.arraycopy((Object) jArr, i5, (Object) jArr2, i7, i17 - i5);
                } else if (i3 < i16) {
                    System.arraycopy((Object) jArr, i3, (Object) jArr2, i7, i16 - i3);
                }
                tryComplete();
            }
        }
    }

    static final class FJFloat {
        FJFloat() {
        }

        static final class Sorter extends CountedCompleter<Void> {
            static final long serialVersionUID = 2446542900576103244L;
            final float[] a;
            final int base;
            final int gran;
            final int size;
            final float[] w;
            final int wbase;

            Sorter(CountedCompleter<?> countedCompleter, float[] fArr, float[] fArr2, int i, int i2, int i3, int i4) {
                super(countedCompleter);
                this.a = fArr;
                this.w = fArr2;
                this.base = i;
                this.size = i2;
                this.wbase = i3;
                this.gran = i4;
            }

            @Override
            public final void compute() {
                float[] fArr = this.a;
                float[] fArr2 = this.w;
                int i = this.base;
                int i2 = this.size;
                int i3 = this.wbase;
                int i4 = this.gran;
                CountedCompleter emptyCompleter = this;
                int i5 = i2;
                while (i5 > i4) {
                    int i6 = i5 >>> 1;
                    int i7 = i6 >>> 1;
                    int i8 = i6 + i7;
                    int i9 = i3 + i6;
                    int i10 = i3;
                    float[] fArr3 = fArr2;
                    Relay relay = new Relay(new Merger(emptyCompleter, fArr2, fArr, i3, i6, i9, i5 - i6, i, i4));
                    int i11 = i + i6;
                    int i12 = i + i8;
                    int i13 = i5 - i8;
                    Relay relay2 = new Relay(new Merger(relay, fArr, fArr3, i11, i7, i12, i13, i9, i4));
                    new Sorter(relay2, fArr, fArr3, i12, i13, i10 + i8, i4).fork();
                    new Sorter(relay2, fArr, fArr3, i11, i7, i9, i4).fork();
                    int i14 = i + i7;
                    int i15 = i6 - i7;
                    Relay relay3 = new Relay(new Merger(relay, fArr, fArr3, i, i7, i14, i15, i10, i4));
                    new Sorter(relay3, fArr, fArr3, i14, i15, i10 + i7, i4).fork();
                    emptyCompleter = new EmptyCompleter(relay3);
                    i5 = i7;
                    i3 = i10;
                    fArr2 = fArr3;
                }
                DualPivotQuicksort.sort(fArr, i, (i + i5) - 1, fArr2, i3, i5);
                emptyCompleter.tryComplete();
            }
        }

        static final class Merger extends CountedCompleter<Void> {
            static final long serialVersionUID = 2446542900576103244L;
            final float[] a;
            final int gran;
            final int lbase;
            final int lsize;
            final int rbase;
            final int rsize;
            final float[] w;
            final int wbase;

            Merger(CountedCompleter<?> countedCompleter, float[] fArr, float[] fArr2, int i, int i2, int i3, int i4, int i5, int i6) {
                super(countedCompleter);
                this.a = fArr;
                this.w = fArr2;
                this.lbase = i;
                this.lsize = i2;
                this.rbase = i3;
                this.rsize = i4;
                this.wbase = i5;
                this.gran = i6;
            }

            @Override
            public final void compute() {
                int i;
                int i2;
                float[] fArr = this.a;
                float[] fArr2 = this.w;
                int i3 = this.lbase;
                int i4 = this.lsize;
                int i5 = this.rbase;
                int i6 = this.rsize;
                int i7 = this.wbase;
                int i8 = this.gran;
                if (fArr == null || fArr2 == null || i3 < 0 || i5 < 0 || i7 < 0) {
                    throw new IllegalStateException();
                }
                while (true) {
                    int i9 = 0;
                    if (i4 >= i6) {
                        if (i4 <= i8) {
                            break;
                        }
                        int i10 = i4 >>> 1;
                        float f = fArr[i10 + i3];
                        int i11 = i6;
                        while (i9 < i11) {
                            int i12 = (i9 + i11) >>> 1;
                            if (f > fArr[i12 + i5]) {
                                i9 = i12 + 1;
                            } else {
                                i11 = i12;
                            }
                        }
                        i2 = i10;
                        i = i11;
                        Merger merger = new Merger(this, fArr, fArr2, i3 + i2, i4 - i2, i5 + i, i6 - i, i7 + i2 + i, i8);
                        addToPendingCount(1);
                        merger.fork();
                        i4 = i2;
                        i6 = i;
                        fArr = fArr;
                        fArr2 = fArr2;
                    } else {
                        if (i6 <= i8) {
                            break;
                        }
                        int i13 = i6 >>> 1;
                        float f2 = fArr[i13 + i5];
                        int i14 = i4;
                        while (i9 < i14) {
                            int i15 = (i9 + i14) >>> 1;
                            if (f2 > fArr[i15 + i3]) {
                                i9 = i15 + 1;
                            } else {
                                i14 = i15;
                            }
                        }
                        i = i13;
                        i2 = i14;
                        Merger merger2 = new Merger(this, fArr, fArr2, i3 + i2, i4 - i2, i5 + i, i6 - i, i7 + i2 + i, i8);
                        addToPendingCount(1);
                        merger2.fork();
                        i4 = i2;
                        i6 = i;
                        fArr = fArr;
                        fArr2 = fArr2;
                    }
                }
                int i16 = i4 + i3;
                int i17 = i6 + i5;
                while (i3 < i16 && i5 < i17) {
                    float f3 = fArr[i3];
                    float f4 = fArr[i5];
                    if (f3 <= f4) {
                        i3++;
                    } else {
                        i5++;
                        f3 = f4;
                    }
                    fArr2[i7] = f3;
                    i7++;
                }
                if (i5 < i17) {
                    System.arraycopy((Object) fArr, i5, (Object) fArr2, i7, i17 - i5);
                } else if (i3 < i16) {
                    System.arraycopy((Object) fArr, i3, (Object) fArr2, i7, i16 - i3);
                }
                tryComplete();
            }
        }
    }

    static final class FJDouble {
        FJDouble() {
        }

        static final class Sorter extends CountedCompleter<Void> {
            static final long serialVersionUID = 2446542900576103244L;
            final double[] a;
            final int base;
            final int gran;
            final int size;
            final double[] w;
            final int wbase;

            Sorter(CountedCompleter<?> countedCompleter, double[] dArr, double[] dArr2, int i, int i2, int i3, int i4) {
                super(countedCompleter);
                this.a = dArr;
                this.w = dArr2;
                this.base = i;
                this.size = i2;
                this.wbase = i3;
                this.gran = i4;
            }

            @Override
            public final void compute() {
                double[] dArr = this.a;
                double[] dArr2 = this.w;
                int i = this.base;
                int i2 = this.size;
                int i3 = this.wbase;
                int i4 = this.gran;
                CountedCompleter emptyCompleter = this;
                int i5 = i2;
                while (i5 > i4) {
                    int i6 = i5 >>> 1;
                    int i7 = i6 >>> 1;
                    int i8 = i6 + i7;
                    int i9 = i3 + i6;
                    int i10 = i3;
                    double[] dArr3 = dArr2;
                    Relay relay = new Relay(new Merger(emptyCompleter, dArr2, dArr, i3, i6, i9, i5 - i6, i, i4));
                    int i11 = i + i6;
                    int i12 = i + i8;
                    int i13 = i5 - i8;
                    Relay relay2 = new Relay(new Merger(relay, dArr, dArr3, i11, i7, i12, i13, i9, i4));
                    new Sorter(relay2, dArr, dArr3, i12, i13, i10 + i8, i4).fork();
                    new Sorter(relay2, dArr, dArr3, i11, i7, i9, i4).fork();
                    int i14 = i + i7;
                    int i15 = i6 - i7;
                    Relay relay3 = new Relay(new Merger(relay, dArr, dArr3, i, i7, i14, i15, i10, i4));
                    new Sorter(relay3, dArr, dArr3, i14, i15, i10 + i7, i4).fork();
                    emptyCompleter = new EmptyCompleter(relay3);
                    i5 = i7;
                    i3 = i10;
                    dArr2 = dArr3;
                }
                DualPivotQuicksort.sort(dArr, i, (i + i5) - 1, dArr2, i3, i5);
                emptyCompleter.tryComplete();
            }
        }

        static final class Merger extends CountedCompleter<Void> {
            static final long serialVersionUID = 2446542900576103244L;
            final double[] a;
            final int gran;
            final int lbase;
            final int lsize;
            final int rbase;
            final int rsize;
            final double[] w;
            final int wbase;

            Merger(CountedCompleter<?> countedCompleter, double[] dArr, double[] dArr2, int i, int i2, int i3, int i4, int i5, int i6) {
                super(countedCompleter);
                this.a = dArr;
                this.w = dArr2;
                this.lbase = i;
                this.lsize = i2;
                this.rbase = i3;
                this.rsize = i4;
                this.wbase = i5;
                this.gran = i6;
            }

            @Override
            public final void compute() {
                int i;
                int i2;
                double[] dArr = this.a;
                double[] dArr2 = this.w;
                int i3 = this.lbase;
                int i4 = this.lsize;
                int i5 = this.rbase;
                int i6 = this.rsize;
                int i7 = this.wbase;
                int i8 = this.gran;
                if (dArr == null || dArr2 == null || i3 < 0 || i5 < 0 || i7 < 0) {
                    throw new IllegalStateException();
                }
                while (true) {
                    int i9 = 0;
                    if (i4 >= i6) {
                        if (i4 <= i8) {
                            break;
                        }
                        int i10 = i4 >>> 1;
                        double d = dArr[i10 + i3];
                        int i11 = i6;
                        while (i9 < i11) {
                            int i12 = (i9 + i11) >>> 1;
                            if (d > dArr[i12 + i5]) {
                                i9 = i12 + 1;
                            } else {
                                i11 = i12;
                            }
                        }
                        i2 = i10;
                        i = i11;
                        Merger merger = new Merger(this, dArr, dArr2, i3 + i2, i4 - i2, i5 + i, i6 - i, i7 + i2 + i, i8);
                        addToPendingCount(1);
                        merger.fork();
                        i4 = i2;
                        i6 = i;
                        dArr = dArr;
                        dArr2 = dArr2;
                    } else {
                        if (i6 <= i8) {
                            break;
                        }
                        int i13 = i6 >>> 1;
                        double d2 = dArr[i13 + i5];
                        int i14 = i4;
                        while (i9 < i14) {
                            int i15 = (i9 + i14) >>> 1;
                            if (d2 > dArr[i15 + i3]) {
                                i9 = i15 + 1;
                            } else {
                                i14 = i15;
                            }
                        }
                        i = i13;
                        i2 = i14;
                        Merger merger2 = new Merger(this, dArr, dArr2, i3 + i2, i4 - i2, i5 + i, i6 - i, i7 + i2 + i, i8);
                        addToPendingCount(1);
                        merger2.fork();
                        i4 = i2;
                        i6 = i;
                        dArr = dArr;
                        dArr2 = dArr2;
                    }
                }
                int i16 = i4 + i3;
                int i17 = i6 + i5;
                while (i3 < i16 && i5 < i17) {
                    double d3 = dArr[i3];
                    double d4 = dArr[i5];
                    if (d3 <= d4) {
                        i3++;
                    } else {
                        i5++;
                        d3 = d4;
                    }
                    dArr2[i7] = d3;
                    i7++;
                }
                if (i5 < i17) {
                    System.arraycopy((Object) dArr, i5, (Object) dArr2, i7, i17 - i5);
                } else if (i3 < i16) {
                    System.arraycopy((Object) dArr, i3, (Object) dArr2, i7, i16 - i3);
                }
                tryComplete();
            }
        }
    }
}
