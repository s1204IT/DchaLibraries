package com.android.internal.util.function.pooled;

import android.os.Message;
import android.text.TextUtils;
import android.util.Pools;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.BitUtils;
import com.android.internal.util.function.HexConsumer;
import com.android.internal.util.function.HexFunction;
import com.android.internal.util.function.HexPredicate;
import com.android.internal.util.function.QuadConsumer;
import com.android.internal.util.function.QuadFunction;
import com.android.internal.util.function.QuadPredicate;
import com.android.internal.util.function.QuintConsumer;
import com.android.internal.util.function.QuintFunction;
import com.android.internal.util.function.QuintPredicate;
import com.android.internal.util.function.TriConsumer;
import com.android.internal.util.function.TriFunction;
import com.android.internal.util.function.TriPredicate;
import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

final class PooledLambdaImpl<R> extends OmniFunction<Object, Object, Object, Object, Object, Object, R> {
    private static final boolean DEBUG = false;
    private static final int FLAG_ACQUIRED_FROM_MESSAGE_CALLBACKS_POOL = 128;
    private static final int FLAG_RECYCLED = 32;
    private static final int FLAG_RECYCLE_ON_USE = 64;
    private static final String LOG_TAG = "PooledLambdaImpl";
    static final int MASK_EXPOSED_AS = 16128;
    static final int MASK_FUNC_TYPE = 1032192;
    private static final int MAX_ARGS = 5;
    private static final int MAX_POOL_SIZE = 50;
    long mConstValue;
    Object mFunc;
    static final Pool sPool = new Pool(new Object());
    static final Pool sMessageCallbacksPool = new Pool(Message.sPoolSync);
    Object[] mArgs = null;
    int mFlags = 0;

    static class Pool extends Pools.SynchronizedPool<PooledLambdaImpl> {
        public Pool(Object obj) {
            super(50, obj);
        }
    }

    private PooledLambdaImpl() {
    }

    @Override
    public void recycle() {
        if (!isRecycled()) {
            doRecycle();
        }
    }

    private void doRecycle() {
        Pool pool;
        if ((this.mFlags & 128) != 0) {
            pool = sMessageCallbacksPool;
        } else {
            pool = sPool;
        }
        this.mFunc = null;
        if (this.mArgs != null) {
            Arrays.fill(this.mArgs, (Object) null);
        }
        this.mFlags = 32;
        this.mConstValue = 0L;
        pool.release(this);
    }

    @Override
    R invoke(Object obj, Object obj2, Object obj3, Object obj4, Object obj5, Object obj6) {
        checkNotRecycled();
        if (fillInArg(obj) && fillInArg(obj2) && fillInArg(obj3) && fillInArg(obj4) && fillInArg(obj5)) {
            fillInArg(obj6);
        }
        int iDecodeArgCount = LambdaType.decodeArgCount(getFlags(MASK_FUNC_TYPE));
        int i = 0;
        if (iDecodeArgCount != 7) {
            for (int i2 = 0; i2 < iDecodeArgCount; i2++) {
                if (this.mArgs[i2] == ArgumentPlaceholder.INSTANCE) {
                    throw new IllegalStateException("Missing argument #" + i2 + " among " + Arrays.toString(this.mArgs));
                }
            }
        }
        try {
            return doInvoke();
        } finally {
            if (isRecycleOnUse()) {
                doRecycle();
            }
            if (!isRecycled()) {
                int size = ArrayUtils.size(this.mArgs);
                while (i < size) {
                    popArg(i);
                    i++;
                }
            }
        }
    }

    private boolean fillInArg(Object obj) {
        int size = ArrayUtils.size(this.mArgs);
        for (int i = 0; i < size; i++) {
            if (this.mArgs[i] == ArgumentPlaceholder.INSTANCE) {
                this.mArgs[i] = obj;
                this.mFlags = (int) (((long) this.mFlags) | BitUtils.bitAt(i));
                return true;
            }
        }
        if (obj == null || obj == ArgumentPlaceholder.INSTANCE) {
            return false;
        }
        throw new IllegalStateException("No more arguments expected for provided arg " + obj + " among " + Arrays.toString(this.mArgs));
    }

    private void checkNotRecycled() {
        if (isRecycled()) {
            throw new IllegalStateException("Instance is recycled: " + this);
        }
    }

    private R doInvoke() {
        int flags = getFlags(MASK_FUNC_TYPE);
        int iDecodeArgCount = LambdaType.decodeArgCount(flags);
        int iDecodeReturnType = LambdaType.decodeReturnType(flags);
        switch (iDecodeArgCount) {
            case 0:
                switch (iDecodeReturnType) {
                    case 1:
                        ((Runnable) this.mFunc).run();
                        return null;
                    case 2:
                    case 3:
                        return (R) ((Supplier) this.mFunc).get();
                }
            case 1:
                switch (iDecodeReturnType) {
                    case 1:
                        ((Consumer) this.mFunc).accept(popArg(0));
                        return null;
                    case 2:
                        return (R) Boolean.valueOf(((Predicate) this.mFunc).test(popArg(0)));
                    case 3:
                        return (R) ((Function) this.mFunc).apply(popArg(0));
                }
            case 2:
                switch (iDecodeReturnType) {
                    case 1:
                        ((BiConsumer) this.mFunc).accept(popArg(0), popArg(1));
                        return null;
                    case 2:
                        return (R) Boolean.valueOf(((BiPredicate) this.mFunc).test(popArg(0), popArg(1)));
                    case 3:
                        return (R) ((BiFunction) this.mFunc).apply(popArg(0), popArg(1));
                }
            case 3:
                switch (iDecodeReturnType) {
                    case 1:
                        ((TriConsumer) this.mFunc).accept(popArg(0), popArg(1), popArg(2));
                        return null;
                    case 2:
                        return (R) Boolean.valueOf(((TriPredicate) this.mFunc).test(popArg(0), popArg(1), popArg(2)));
                    case 3:
                        return (R) ((TriFunction) this.mFunc).apply(popArg(0), popArg(1), popArg(2));
                }
            case 4:
                switch (iDecodeReturnType) {
                    case 1:
                        ((QuadConsumer) this.mFunc).accept(popArg(0), popArg(1), popArg(2), popArg(3));
                        return null;
                    case 2:
                        return (R) Boolean.valueOf(((QuadPredicate) this.mFunc).test(popArg(0), popArg(1), popArg(2), popArg(3)));
                    case 3:
                        return (R) ((QuadFunction) this.mFunc).apply(popArg(0), popArg(1), popArg(2), popArg(3));
                }
            case 5:
                switch (iDecodeReturnType) {
                    case 1:
                        ((QuintConsumer) this.mFunc).accept(popArg(0), popArg(1), popArg(2), popArg(3), popArg(4));
                        return null;
                    case 2:
                        return (R) Boolean.valueOf(((QuintPredicate) this.mFunc).test(popArg(0), popArg(1), popArg(2), popArg(3), popArg(4)));
                    case 3:
                        return (R) ((QuintFunction) this.mFunc).apply(popArg(0), popArg(1), popArg(2), popArg(3), popArg(4));
                }
            case 6:
                switch (iDecodeReturnType) {
                    case 1:
                        ((HexConsumer) this.mFunc).accept(popArg(0), popArg(1), popArg(2), popArg(3), popArg(4), popArg(5));
                        return null;
                    case 2:
                        return (R) Boolean.valueOf(((HexPredicate) this.mFunc).test(popArg(0), popArg(1), popArg(2), popArg(3), popArg(4), popArg(5)));
                    case 3:
                        return (R) ((HexFunction) this.mFunc).apply(popArg(0), popArg(1), popArg(2), popArg(3), popArg(4), popArg(5));
                }
            case 7:
                switch (iDecodeReturnType) {
                    case 4:
                        return (R) Integer.valueOf(getAsInt());
                    case 5:
                        return (R) Long.valueOf(getAsLong());
                    case 6:
                        return (R) Double.valueOf(getAsDouble());
                    default:
                        return (R) this.mFunc;
                }
        }
        throw new IllegalStateException("Unknown function type: " + LambdaType.toString(flags));
    }

    private boolean isConstSupplier() {
        return LambdaType.decodeArgCount(getFlags(MASK_FUNC_TYPE)) == 7;
    }

    private Object popArg(int i) {
        Object obj = this.mArgs[i];
        if (isInvocationArgAtIndex(i)) {
            this.mArgs[i] = ArgumentPlaceholder.INSTANCE;
            this.mFlags = (int) (((long) this.mFlags) & (~BitUtils.bitAt(i)));
        }
        return obj;
    }

    public String toString() {
        if (isRecycled()) {
            return "<recycled PooledLambda@" + hashCodeHex(this) + ">";
        }
        StringBuilder sb = new StringBuilder();
        if (isConstSupplier()) {
            sb.append(getFuncTypeAsString());
            sb.append("(");
            sb.append(doInvoke());
            sb.append(")");
        } else {
            if (this.mFunc instanceof PooledLambdaImpl) {
                sb.append(this.mFunc);
            } else {
                sb.append(getFuncTypeAsString());
                sb.append("@");
                sb.append(hashCodeHex(this.mFunc));
            }
            sb.append("(");
            sb.append(commaSeparateFirstN(this.mArgs, LambdaType.decodeArgCount(getFlags(MASK_FUNC_TYPE))));
            sb.append(")");
        }
        return sb.toString();
    }

    private String commaSeparateFirstN(Object[] objArr, int i) {
        return objArr == null ? "" : TextUtils.join(",", Arrays.copyOf(objArr, i));
    }

    private static String hashCodeHex(Object obj) {
        return Integer.toHexString(obj.hashCode());
    }

    private String getFuncTypeAsString() {
        if (isRecycled()) {
            throw new IllegalStateException();
        }
        if (isConstSupplier()) {
            return "supplier";
        }
        String string = LambdaType.toString(getFlags(MASK_EXPOSED_AS));
        if (string.endsWith("Consumer")) {
            return "consumer";
        }
        if (string.endsWith("Function")) {
            return "function";
        }
        if (string.endsWith("Predicate")) {
            return "predicate";
        }
        if (string.endsWith("Supplier")) {
            return "supplier";
        }
        if (string.endsWith("Runnable")) {
            return "runnable";
        }
        throw new IllegalStateException("Don't know the string representation of " + string);
    }

    static <E extends PooledLambda> E acquire(Pool pool, Object obj, int i, int i2, int i3, Object obj2, Object obj3, Object obj4, Object obj5, Object obj6, Object obj7) {
        PooledLambdaImpl pooledLambdaImplAcquire = acquire(pool);
        pooledLambdaImplAcquire.mFunc = obj;
        pooledLambdaImplAcquire.setFlags(MASK_FUNC_TYPE, LambdaType.encode(i, i3));
        pooledLambdaImplAcquire.setFlags(MASK_EXPOSED_AS, LambdaType.encode(i2, i3));
        if (ArrayUtils.size(pooledLambdaImplAcquire.mArgs) < i) {
            pooledLambdaImplAcquire.mArgs = new Object[i];
        }
        setIfInBounds(pooledLambdaImplAcquire.mArgs, 0, obj2);
        setIfInBounds(pooledLambdaImplAcquire.mArgs, 1, obj3);
        setIfInBounds(pooledLambdaImplAcquire.mArgs, 2, obj4);
        setIfInBounds(pooledLambdaImplAcquire.mArgs, 3, obj5);
        setIfInBounds(pooledLambdaImplAcquire.mArgs, 4, obj6);
        setIfInBounds(pooledLambdaImplAcquire.mArgs, 5, obj7);
        return pooledLambdaImplAcquire;
    }

    static PooledLambdaImpl acquireConstSupplier(int i) {
        PooledLambdaImpl pooledLambdaImplAcquire = acquire(sPool);
        int iEncode = LambdaType.encode(7, i);
        pooledLambdaImplAcquire.setFlags(MASK_FUNC_TYPE, iEncode);
        pooledLambdaImplAcquire.setFlags(MASK_EXPOSED_AS, iEncode);
        return pooledLambdaImplAcquire;
    }

    static PooledLambdaImpl acquire(Pool pool) {
        PooledLambdaImpl pooledLambdaImplAcquire = pool.acquire();
        if (pooledLambdaImplAcquire == null) {
            pooledLambdaImplAcquire = new PooledLambdaImpl();
        }
        pooledLambdaImplAcquire.mFlags &= -33;
        pooledLambdaImplAcquire.setFlags(128, pool == sMessageCallbacksPool ? 1 : 0);
        return pooledLambdaImplAcquire;
    }

    private static void setIfInBounds(Object[] objArr, int i, Object obj) {
        if (i < ArrayUtils.size(objArr)) {
            objArr[i] = obj;
        }
    }

    @Override
    public OmniFunction<Object, Object, Object, Object, Object, Object, R> negate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> OmniFunction<Object, Object, Object, Object, Object, Object, V> andThen(Function<? super R, ? extends V> function) {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getAsDouble() {
        return Double.longBitsToDouble(this.mConstValue);
    }

    @Override
    public int getAsInt() {
        return (int) this.mConstValue;
    }

    @Override
    public long getAsLong() {
        return this.mConstValue;
    }

    @Override
    public OmniFunction<Object, Object, Object, Object, Object, Object, R> recycleOnUse() {
        this.mFlags |= 64;
        return this;
    }

    private boolean isRecycled() {
        return (this.mFlags & 32) != 0;
    }

    private boolean isRecycleOnUse() {
        return (this.mFlags & 64) != 0;
    }

    private boolean isInvocationArgAtIndex(int i) {
        return ((1 << i) & this.mFlags) != 0;
    }

    int getFlags(int i) {
        return unmask(i, this.mFlags);
    }

    void setFlags(int i, int i2) {
        this.mFlags &= ~i;
        this.mFlags = mask(i, i2) | this.mFlags;
    }

    private static int mask(int i, int i2) {
        return i & (i2 << Integer.numberOfTrailingZeros(i));
    }

    private static int unmask(int i, int i2) {
        return (i2 & i) / (1 << Integer.numberOfTrailingZeros(i));
    }

    static class LambdaType {
        public static final int MASK = 63;
        public static final int MASK_ARG_COUNT = 7;
        public static final int MASK_BIT_COUNT = 6;
        public static final int MASK_RETURN_TYPE = 56;

        LambdaType() {
        }

        static int encode(int i, int i2) {
            return PooledLambdaImpl.mask(7, i) | PooledLambdaImpl.mask(56, i2);
        }

        static int decodeArgCount(int i) {
            return i & 7;
        }

        static int decodeReturnType(int i) {
            return PooledLambdaImpl.unmask(56, i);
        }

        static String toString(int i) {
            int iDecodeArgCount = decodeArgCount(i);
            int iDecodeReturnType = decodeReturnType(i);
            if (iDecodeArgCount == 0) {
                if (iDecodeReturnType == 1) {
                    return "Runnable";
                }
                if (iDecodeReturnType == 3 || iDecodeReturnType == 2) {
                    return "Supplier";
                }
            }
            return argCountPrefix(iDecodeArgCount) + ReturnType.lambdaSuffix(iDecodeReturnType);
        }

        private static String argCountPrefix(int i) {
            switch (i) {
                case 1:
                    return "";
                case 2:
                    return "Bi";
                case 3:
                    return "Tri";
                case 4:
                    return "Quad";
                case 5:
                    return "Quint";
                case 6:
                    return "Hex";
                case 7:
                    return "";
                default:
                    throw new IllegalArgumentException("" + i);
            }
        }

        static class ReturnType {
            public static final int BOOLEAN = 2;
            public static final int DOUBLE = 6;
            public static final int INT = 4;
            public static final int LONG = 5;
            public static final int OBJECT = 3;
            public static final int VOID = 1;

            ReturnType() {
            }

            static String toString(int i) {
                switch (i) {
                    case 1:
                        return "VOID";
                    case 2:
                        return "BOOLEAN";
                    case 3:
                        return "OBJECT";
                    case 4:
                        return "INT";
                    case 5:
                        return "LONG";
                    case 6:
                        return "DOUBLE";
                    default:
                        return "" + i;
                }
            }

            static String lambdaSuffix(int i) {
                return prefix(i) + suffix(i);
            }

            private static String prefix(int i) {
                switch (i) {
                    case 4:
                        return "Int";
                    case 5:
                        return "Long";
                    case 6:
                        return "Double";
                    default:
                        return "";
                }
            }

            private static String suffix(int i) {
                switch (i) {
                    case 1:
                        return "Consumer";
                    case 2:
                        return "Predicate";
                    case 3:
                        return "Function";
                    default:
                        return "Supplier";
                }
            }
        }
    }
}
