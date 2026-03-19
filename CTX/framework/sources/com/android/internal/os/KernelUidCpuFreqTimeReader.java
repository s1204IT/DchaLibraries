package com.android.internal.os;

import android.net.wifi.WifiEnterpriseConfig;
import android.os.StrictMode;
import android.util.IntArray;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.KernelUidCpuTimeReaderBase;
import com.android.internal.util.Preconditions;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.function.Consumer;

public class KernelUidCpuFreqTimeReader extends KernelUidCpuTimeReaderBase<Callback> {
    private static final String TAG = KernelUidCpuFreqTimeReader.class.getSimpleName();
    private static final int TOTAL_READ_ERROR_COUNT = 5;
    static final String UID_TIMES_PROC_FILE = "/proc/uid_time_in_state";
    private boolean mAllUidTimesAvailable;
    private long[] mCpuFreqs;
    private int mCpuFreqsCount;
    private long[] mCurTimes;
    private long[] mDeltaTimes;
    private SparseArray<long[]> mLastUidCpuFreqTimeMs;
    private boolean mPerClusterTimesAvailable;
    private final KernelCpuProcReader mProcReader;
    private int mReadErrorCounter;

    public interface Callback extends KernelUidCpuTimeReaderBase.Callback {
        void onUidCpuFreqTime(int i, long[] jArr);
    }

    public KernelUidCpuFreqTimeReader() {
        this.mLastUidCpuFreqTimeMs = new SparseArray<>();
        this.mAllUidTimesAvailable = true;
        this.mProcReader = KernelCpuProcReader.getFreqTimeReaderInstance();
    }

    @VisibleForTesting
    public KernelUidCpuFreqTimeReader(KernelCpuProcReader kernelCpuProcReader) {
        this.mLastUidCpuFreqTimeMs = new SparseArray<>();
        this.mAllUidTimesAvailable = true;
        this.mProcReader = kernelCpuProcReader;
    }

    public boolean perClusterTimesAvailable() {
        return this.mPerClusterTimesAvailable;
    }

    public boolean allUidTimesAvailable() {
        return this.mAllUidTimesAvailable;
    }

    public SparseArray<long[]> getAllUidCpuFreqTimeMs() {
        return this.mLastUidCpuFreqTimeMs;
    }

    public long[] readFreqs(PowerProfile powerProfile) {
        Throwable th;
        Preconditions.checkNotNull(powerProfile);
        if (this.mCpuFreqs != null) {
            return this.mCpuFreqs;
        }
        if (!this.mAllUidTimesAvailable) {
            return null;
        }
        int iAllowThreadDiskReadsMask = StrictMode.allowThreadDiskReadsMask();
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(UID_TIMES_PROC_FILE));
            try {
                long[] freqs = readFreqs(bufferedReader, powerProfile);
                bufferedReader.close();
                return freqs;
            } catch (Throwable th2) {
                th = th2;
                th = null;
                if (th != null) {
                }
            }
        } catch (IOException e) {
            int i = this.mReadErrorCounter + 1;
            this.mReadErrorCounter = i;
            if (i >= 5) {
                this.mAllUidTimesAvailable = false;
            }
            Slog.e(TAG, "Failed to read /proc/uid_time_in_state: " + e);
            return null;
        } finally {
            StrictMode.setThreadPolicyMask(iAllowThreadDiskReadsMask);
        }
    }

    @VisibleForTesting
    public long[] readFreqs(BufferedReader bufferedReader, PowerProfile powerProfile) throws IOException {
        String line = bufferedReader.readLine();
        if (line == null) {
            return null;
        }
        String[] strArrSplit = line.split(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        this.mCpuFreqsCount = strArrSplit.length - 1;
        this.mCpuFreqs = new long[this.mCpuFreqsCount];
        this.mCurTimes = new long[this.mCpuFreqsCount];
        this.mDeltaTimes = new long[this.mCpuFreqsCount];
        int i = 0;
        while (i < this.mCpuFreqsCount) {
            int i2 = i + 1;
            this.mCpuFreqs[i] = Long.parseLong(strArrSplit[i2], 10);
            i = i2;
        }
        IntArray intArrayExtractClusterInfoFromProcFileFreqs = extractClusterInfoFromProcFileFreqs();
        int numCpuClusters = powerProfile.getNumCpuClusters();
        if (intArrayExtractClusterInfoFromProcFileFreqs.size() == numCpuClusters) {
            this.mPerClusterTimesAvailable = true;
            int i3 = 0;
            while (true) {
                if (i3 >= numCpuClusters) {
                    break;
                }
                if (intArrayExtractClusterInfoFromProcFileFreqs.get(i3) == powerProfile.getNumSpeedStepsInCpuCluster(i3)) {
                    i3++;
                } else {
                    this.mPerClusterTimesAvailable = false;
                    break;
                }
            }
        } else {
            this.mPerClusterTimesAvailable = false;
        }
        Slog.i(TAG, "mPerClusterTimesAvailable=" + this.mPerClusterTimesAvailable);
        return this.mCpuFreqs;
    }

    @Override
    @VisibleForTesting
    public void readDeltaImpl(final Callback callback) {
        if (this.mCpuFreqs == null) {
            return;
        }
        readImpl(new Consumer() {
            @Override
            public final void accept(Object obj) {
                KernelUidCpuFreqTimeReader.lambda$readDeltaImpl$0(this.f$0, callback, (IntBuffer) obj);
            }
        });
    }

    public static void lambda$readDeltaImpl$0(KernelUidCpuFreqTimeReader kernelUidCpuFreqTimeReader, Callback callback, IntBuffer intBuffer) {
        int i = intBuffer.get();
        long[] jArr = kernelUidCpuFreqTimeReader.mLastUidCpuFreqTimeMs.get(i);
        if (jArr == null) {
            jArr = new long[kernelUidCpuFreqTimeReader.mCpuFreqsCount];
            kernelUidCpuFreqTimeReader.mLastUidCpuFreqTimeMs.put(i, jArr);
        }
        if (!kernelUidCpuFreqTimeReader.getFreqTimeForUid(intBuffer, kernelUidCpuFreqTimeReader.mCurTimes)) {
            return;
        }
        boolean z = true;
        boolean z2 = false;
        for (int i2 = 0; i2 < kernelUidCpuFreqTimeReader.mCpuFreqsCount; i2++) {
            kernelUidCpuFreqTimeReader.mDeltaTimes[i2] = kernelUidCpuFreqTimeReader.mCurTimes[i2] - jArr[i2];
            if (kernelUidCpuFreqTimeReader.mDeltaTimes[i2] < 0) {
                Slog.e(TAG, "Negative delta from freq time proc: " + kernelUidCpuFreqTimeReader.mDeltaTimes[i2]);
                z = false;
            }
            z2 |= kernelUidCpuFreqTimeReader.mDeltaTimes[i2] > 0;
        }
        if (z2 && z) {
            System.arraycopy(kernelUidCpuFreqTimeReader.mCurTimes, 0, jArr, 0, kernelUidCpuFreqTimeReader.mCpuFreqsCount);
            if (callback != null) {
                callback.onUidCpuFreqTime(i, kernelUidCpuFreqTimeReader.mDeltaTimes);
            }
        }
    }

    public void readAbsolute(final Callback callback) {
        readImpl(new Consumer() {
            @Override
            public final void accept(Object obj) {
                KernelUidCpuFreqTimeReader.lambda$readAbsolute$1(this.f$0, callback, (IntBuffer) obj);
            }
        });
    }

    public static void lambda$readAbsolute$1(KernelUidCpuFreqTimeReader kernelUidCpuFreqTimeReader, Callback callback, IntBuffer intBuffer) {
        int i = intBuffer.get();
        if (kernelUidCpuFreqTimeReader.getFreqTimeForUid(intBuffer, kernelUidCpuFreqTimeReader.mCurTimes)) {
            callback.onUidCpuFreqTime(i, kernelUidCpuFreqTimeReader.mCurTimes);
        }
    }

    private boolean getFreqTimeForUid(IntBuffer intBuffer, long[] jArr) {
        boolean z = true;
        for (int i = 0; i < this.mCpuFreqsCount; i++) {
            jArr[i] = ((long) intBuffer.get()) * 10;
            if (jArr[i] < 0) {
                Slog.e(TAG, "Negative time from freq time proc: " + jArr[i]);
                z = false;
            }
        }
        return z;
    }

    private void readImpl(Consumer<IntBuffer> consumer) {
        synchronized (this.mProcReader) {
            ByteBuffer bytes = this.mProcReader.readBytes();
            if (bytes != null && bytes.remaining() > 4) {
                if ((bytes.remaining() & 3) != 0) {
                    Slog.wtf(TAG, "Cannot parse freq time proc bytes to int: " + bytes.remaining());
                    return;
                }
                IntBuffer intBufferAsIntBuffer = bytes.asIntBuffer();
                int i = intBufferAsIntBuffer.get();
                if (i != this.mCpuFreqsCount) {
                    Slog.wtf(TAG, "Cpu freqs expect " + this.mCpuFreqsCount + " , got " + i);
                    return;
                }
                int i2 = i + 1;
                if (intBufferAsIntBuffer.remaining() % i2 != 0) {
                    Slog.wtf(TAG, "Freq time format error: " + intBufferAsIntBuffer.remaining() + " / " + i2);
                    return;
                }
                int iRemaining = intBufferAsIntBuffer.remaining() / i2;
                for (int i3 = 0; i3 < iRemaining; i3++) {
                    consumer.accept(intBufferAsIntBuffer);
                }
            }
        }
    }

    public void removeUid(int i) {
        this.mLastUidCpuFreqTimeMs.delete(i);
    }

    public void removeUidsInRange(int i, int i2) {
        this.mLastUidCpuFreqTimeMs.put(i, null);
        this.mLastUidCpuFreqTimeMs.put(i2, null);
        int iIndexOfKey = this.mLastUidCpuFreqTimeMs.indexOfKey(i);
        this.mLastUidCpuFreqTimeMs.removeAtRange(iIndexOfKey, (this.mLastUidCpuFreqTimeMs.indexOfKey(i2) - iIndexOfKey) + 1);
    }

    private IntArray extractClusterInfoFromProcFileFreqs() {
        IntArray intArray = new IntArray();
        int i = 0;
        int i2 = 0;
        while (i < this.mCpuFreqsCount) {
            i2++;
            int i3 = i + 1;
            if (i3 == this.mCpuFreqsCount || this.mCpuFreqs[i3] <= this.mCpuFreqs[i]) {
                intArray.add(i2);
                i2 = 0;
            }
            i = i3;
        }
        return intArray;
    }
}
