package com.android.internal.os;

import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.KernelUidCpuTimeReaderBase;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.function.Consumer;

public class KernelUidCpuActiveTimeReader extends KernelUidCpuTimeReaderBase<Callback> {
    private static final String TAG = KernelUidCpuActiveTimeReader.class.getSimpleName();
    private int mCores;
    private SparseArray<Double> mLastUidCpuActiveTimeMs;
    private final KernelCpuProcReader mProcReader;

    public interface Callback extends KernelUidCpuTimeReaderBase.Callback {
        void onUidCpuActiveTime(int i, long j);
    }

    public KernelUidCpuActiveTimeReader() {
        this.mLastUidCpuActiveTimeMs = new SparseArray<>();
        this.mProcReader = KernelCpuProcReader.getActiveTimeReaderInstance();
    }

    @VisibleForTesting
    public KernelUidCpuActiveTimeReader(KernelCpuProcReader kernelCpuProcReader) {
        this.mLastUidCpuActiveTimeMs = new SparseArray<>();
        this.mProcReader = kernelCpuProcReader;
    }

    @Override
    protected void readDeltaImpl(final Callback callback) {
        readImpl(new Consumer() {
            @Override
            public final void accept(Object obj) {
                KernelUidCpuActiveTimeReader.lambda$readDeltaImpl$0(this.f$0, callback, (IntBuffer) obj);
            }
        });
    }

    public static void lambda$readDeltaImpl$0(KernelUidCpuActiveTimeReader kernelUidCpuActiveTimeReader, Callback callback, IntBuffer intBuffer) {
        int i = intBuffer.get();
        double dSumActiveTime = kernelUidCpuActiveTimeReader.sumActiveTime(intBuffer);
        if (dSumActiveTime > 0.0d) {
            double dDoubleValue = dSumActiveTime - kernelUidCpuActiveTimeReader.mLastUidCpuActiveTimeMs.get(i, Double.valueOf(0.0d)).doubleValue();
            if (dDoubleValue > 0.0d) {
                kernelUidCpuActiveTimeReader.mLastUidCpuActiveTimeMs.put(i, Double.valueOf(dSumActiveTime));
                if (callback != null) {
                    callback.onUidCpuActiveTime(i, (long) dDoubleValue);
                    return;
                }
                return;
            }
            if (dDoubleValue < 0.0d) {
                Slog.e(TAG, "Negative delta from active time proc: " + dDoubleValue);
            }
        }
    }

    public void readAbsolute(final Callback callback) {
        readImpl(new Consumer() {
            @Override
            public final void accept(Object obj) {
                KernelUidCpuActiveTimeReader.lambda$readAbsolute$1(this.f$0, callback, (IntBuffer) obj);
            }
        });
    }

    public static void lambda$readAbsolute$1(KernelUidCpuActiveTimeReader kernelUidCpuActiveTimeReader, Callback callback, IntBuffer intBuffer) {
        int i = intBuffer.get();
        double dSumActiveTime = kernelUidCpuActiveTimeReader.sumActiveTime(intBuffer);
        if (dSumActiveTime > 0.0d) {
            callback.onUidCpuActiveTime(i, (long) dSumActiveTime);
        }
    }

    private double sumActiveTime(IntBuffer intBuffer) {
        boolean z = false;
        double d = 0.0d;
        for (int i = 1; i <= this.mCores; i++) {
            int i2 = intBuffer.get();
            if (i2 < 0) {
                Slog.e(TAG, "Negative time from active time proc: " + i2);
                z = true;
            } else {
                d += (((double) i2) * 10.0d) / ((double) i);
            }
        }
        if (z) {
            return -1.0d;
        }
        return d;
    }

    private void readImpl(Consumer<IntBuffer> consumer) {
        synchronized (this.mProcReader) {
            ByteBuffer bytes = this.mProcReader.readBytes();
            if (bytes != null && bytes.remaining() > 4) {
                if ((bytes.remaining() & 3) != 0) {
                    Slog.wtf(TAG, "Cannot parse active time proc bytes to int: " + bytes.remaining());
                    return;
                }
                IntBuffer intBufferAsIntBuffer = bytes.asIntBuffer();
                int i = intBufferAsIntBuffer.get();
                if (this.mCores != 0 && i != this.mCores) {
                    Slog.wtf(TAG, "Cpu active time wrong # cores: " + i);
                    return;
                }
                this.mCores = i;
                if (i > 0) {
                    int i2 = i + 1;
                    if (intBufferAsIntBuffer.remaining() % i2 == 0) {
                        int iRemaining = intBufferAsIntBuffer.remaining() / i2;
                        for (int i3 = 0; i3 < iRemaining; i3++) {
                            consumer.accept(intBufferAsIntBuffer);
                        }
                        return;
                    }
                }
                Slog.wtf(TAG, "Cpu active time format error: " + intBufferAsIntBuffer.remaining() + " / " + (i + 1));
            }
        }
    }

    public void removeUid(int i) {
        this.mLastUidCpuActiveTimeMs.delete(i);
    }

    public void removeUidsInRange(int i, int i2) {
        this.mLastUidCpuActiveTimeMs.put(i, null);
        this.mLastUidCpuActiveTimeMs.put(i2, null);
        int iIndexOfKey = this.mLastUidCpuActiveTimeMs.indexOfKey(i);
        this.mLastUidCpuActiveTimeMs.removeAtRange(iIndexOfKey, (this.mLastUidCpuActiveTimeMs.indexOfKey(i2) - iIndexOfKey) + 1);
    }
}
