package android.view.inputmethod;

import android.os.Parcel;
import android.util.Slog;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class InputMethodSubtypeArray {
    private static final String TAG = "InputMethodSubtypeArray";
    private volatile byte[] mCompressedData;
    private final int mCount;
    private volatile int mDecompressedSize;
    private volatile InputMethodSubtype[] mInstance;
    private final Object mLockObject = new Object();

    public InputMethodSubtypeArray(List<InputMethodSubtype> list) {
        if (list == null) {
            this.mCount = 0;
        } else {
            this.mCount = list.size();
            this.mInstance = (InputMethodSubtype[]) list.toArray(new InputMethodSubtype[this.mCount]);
        }
    }

    public InputMethodSubtypeArray(Parcel parcel) {
        this.mCount = parcel.readInt();
        if (this.mCount > 0) {
            this.mDecompressedSize = parcel.readInt();
            this.mCompressedData = parcel.createByteArray();
        }
    }

    public void writeToParcel(Parcel parcel) {
        int length;
        if (this.mCount == 0) {
            parcel.writeInt(this.mCount);
            return;
        }
        byte[] bArr = this.mCompressedData;
        int i = this.mDecompressedSize;
        if (bArr == null && i == 0) {
            synchronized (this.mLockObject) {
                bArr = this.mCompressedData;
                i = this.mDecompressedSize;
                if (bArr == null && i == 0) {
                    byte[] bArrMarshall = marshall(this.mInstance);
                    byte[] bArrCompress = compress(bArrMarshall);
                    if (bArrCompress == null) {
                        length = -1;
                        Slog.i(TAG, "Failed to compress data.");
                    } else {
                        length = bArrMarshall.length;
                    }
                    this.mDecompressedSize = length;
                    this.mCompressedData = bArrCompress;
                    i = length;
                    bArr = bArrCompress;
                }
            }
        }
        if (bArr != null && i > 0) {
            parcel.writeInt(this.mCount);
            parcel.writeInt(i);
            parcel.writeByteArray(bArr);
        } else {
            Slog.i(TAG, "Unexpected state. Behaving as an empty array.");
            parcel.writeInt(0);
        }
    }

    public InputMethodSubtype get(int i) {
        if (i < 0 || this.mCount <= i) {
            throw new ArrayIndexOutOfBoundsException();
        }
        InputMethodSubtype[] inputMethodSubtypeArrUnmarshall = this.mInstance;
        if (inputMethodSubtypeArrUnmarshall == null) {
            synchronized (this.mLockObject) {
                inputMethodSubtypeArrUnmarshall = this.mInstance;
                if (inputMethodSubtypeArrUnmarshall == null) {
                    byte[] bArrDecompress = decompress(this.mCompressedData, this.mDecompressedSize);
                    this.mCompressedData = null;
                    this.mDecompressedSize = 0;
                    if (bArrDecompress != null) {
                        inputMethodSubtypeArrUnmarshall = unmarshall(bArrDecompress);
                    } else {
                        Slog.e(TAG, "Failed to decompress data. Returns null as fallback.");
                        inputMethodSubtypeArrUnmarshall = new InputMethodSubtype[this.mCount];
                    }
                    this.mInstance = inputMethodSubtypeArrUnmarshall;
                }
            }
        }
        return inputMethodSubtypeArrUnmarshall[i];
    }

    public int getCount() {
        return this.mCount;
    }

    private static byte[] marshall(InputMethodSubtype[] inputMethodSubtypeArr) throws Throwable {
        Parcel parcelObtain;
        try {
            parcelObtain = Parcel.obtain();
            try {
                parcelObtain.writeTypedArray(inputMethodSubtypeArr, 0);
                byte[] bArrMarshall = parcelObtain.marshall();
                if (parcelObtain != null) {
                    parcelObtain.recycle();
                }
                return bArrMarshall;
            } catch (Throwable th) {
                th = th;
                if (parcelObtain != null) {
                    parcelObtain.recycle();
                }
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
            parcelObtain = null;
        }
    }

    private static InputMethodSubtype[] unmarshall(byte[] bArr) throws Throwable {
        Parcel parcelObtain;
        try {
            parcelObtain = Parcel.obtain();
            try {
                parcelObtain.unmarshall(bArr, 0, bArr.length);
                parcelObtain.setDataPosition(0);
                InputMethodSubtype[] inputMethodSubtypeArr = (InputMethodSubtype[]) parcelObtain.createTypedArray(InputMethodSubtype.CREATOR);
                if (parcelObtain != null) {
                    parcelObtain.recycle();
                }
                return inputMethodSubtypeArr;
            } catch (Throwable th) {
                th = th;
                if (parcelObtain != null) {
                    parcelObtain.recycle();
                }
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
            parcelObtain = null;
        }
    }

    private static byte[] compress(byte[] bArr) throws Throwable {
        Throwable th;
        Throwable th2;
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try {
                GZIPOutputStream gZIPOutputStream = new GZIPOutputStream(byteArrayOutputStream);
                try {
                    gZIPOutputStream.write(bArr);
                    gZIPOutputStream.finish();
                    byte[] byteArray = byteArrayOutputStream.toByteArray();
                    $closeResource(null, gZIPOutputStream);
                    $closeResource(null, byteArrayOutputStream);
                    return byteArray;
                } catch (Throwable th3) {
                    th = th3;
                    th2 = null;
                    $closeResource(th2, gZIPOutputStream);
                    throw th;
                }
            } catch (Throwable th4) {
                th = th4;
                th = null;
                $closeResource(th, byteArrayOutputStream);
                throw th;
            }
        } catch (Exception e) {
            Slog.e(TAG, "Failed to compress the data.", e);
            return null;
        }
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    private static byte[] decompress(byte[] bArr, int i) throws Throwable {
        Throwable th;
        Throwable th2;
        int i2;
        try {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bArr);
            try {
                GZIPInputStream gZIPInputStream = new GZIPInputStream(byteArrayInputStream);
                try {
                    byte[] bArr2 = new byte[i];
                    int i3 = 0;
                    while (i3 < bArr2.length && (i2 = gZIPInputStream.read(bArr2, i3, bArr2.length - i3)) >= 0) {
                        i3 += i2;
                    }
                    if (i != i3) {
                        $closeResource(null, gZIPInputStream);
                        $closeResource(null, byteArrayInputStream);
                        return null;
                    }
                    $closeResource(null, gZIPInputStream);
                    $closeResource(null, byteArrayInputStream);
                    return bArr2;
                } catch (Throwable th3) {
                    th = th3;
                    th2 = null;
                    $closeResource(th2, gZIPInputStream);
                    throw th;
                }
            } catch (Throwable th4) {
                th = th4;
                th = null;
                $closeResource(th, byteArrayInputStream);
                throw th;
            }
        } catch (Exception e) {
            Slog.e(TAG, "Failed to decompress the data.", e);
            return null;
        }
    }
}
