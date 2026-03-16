package android.view.inputmethod;

import android.os.Parcel;
import android.util.Slog;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

    public InputMethodSubtypeArray(List<InputMethodSubtype> subtypes) {
        if (subtypes == null) {
            this.mCount = 0;
        } else {
            this.mCount = subtypes.size();
            this.mInstance = (InputMethodSubtype[]) subtypes.toArray(new InputMethodSubtype[this.mCount]);
        }
    }

    public InputMethodSubtypeArray(Parcel source) {
        this.mCount = source.readInt();
        if (this.mCount > 0) {
            this.mDecompressedSize = source.readInt();
            this.mCompressedData = source.createByteArray();
        }
    }

    public void writeToParcel(Parcel dest) {
        if (this.mCount == 0) {
            dest.writeInt(this.mCount);
            return;
        }
        byte[] compressedData = this.mCompressedData;
        int decompressedSize = this.mDecompressedSize;
        if (compressedData == null && decompressedSize == 0) {
            synchronized (this.mLockObject) {
                compressedData = this.mCompressedData;
                decompressedSize = this.mDecompressedSize;
                if (compressedData == null && decompressedSize == 0) {
                    byte[] decompressedData = marshall(this.mInstance);
                    compressedData = compress(decompressedData);
                    if (compressedData == null) {
                        decompressedSize = -1;
                        Slog.i(TAG, "Failed to compress data.");
                    } else {
                        decompressedSize = decompressedData.length;
                    }
                    this.mDecompressedSize = decompressedSize;
                    this.mCompressedData = compressedData;
                }
            }
        }
        if (compressedData != null && decompressedSize > 0) {
            dest.writeInt(this.mCount);
            dest.writeInt(decompressedSize);
            dest.writeByteArray(compressedData);
        } else {
            Slog.i(TAG, "Unexpected state. Behaving as an empty array.");
            dest.writeInt(0);
        }
    }

    public InputMethodSubtype get(int index) {
        if (index < 0 || this.mCount <= index) {
            throw new ArrayIndexOutOfBoundsException();
        }
        InputMethodSubtype[] instance = this.mInstance;
        if (instance == null) {
            synchronized (this.mLockObject) {
                instance = this.mInstance;
                if (instance == null) {
                    byte[] decompressedData = decompress(this.mCompressedData, this.mDecompressedSize);
                    this.mCompressedData = null;
                    this.mDecompressedSize = 0;
                    if (decompressedData != null) {
                        instance = unmarshall(decompressedData);
                    } else {
                        Slog.e(TAG, "Failed to decompress data. Returns null as fallback.");
                        instance = new InputMethodSubtype[this.mCount];
                    }
                    this.mInstance = instance;
                }
            }
        }
        return instance[index];
    }

    public int getCount() {
        return this.mCount;
    }

    private static byte[] marshall(InputMethodSubtype[] array) {
        Parcel parcel = null;
        try {
            parcel = Parcel.obtain();
            parcel.writeTypedArray(array, 0);
            return parcel.marshall();
        } finally {
            if (parcel != null) {
                parcel.recycle();
            }
        }
    }

    private static InputMethodSubtype[] unmarshall(byte[] data) {
        Parcel parcel = null;
        try {
            parcel = Parcel.obtain();
            parcel.unmarshall(data, 0, data.length);
            parcel.setDataPosition(0);
            return (InputMethodSubtype[]) parcel.createTypedArray(InputMethodSubtype.CREATOR);
        } finally {
            if (parcel != null) {
                parcel.recycle();
            }
        }
    }

    private static byte[] compress(byte[] data) throws Throwable {
        ByteArrayOutputStream resultStream;
        GZIPOutputStream zipper;
        ByteArrayOutputStream resultStream2;
        ByteArrayOutputStream resultStream3 = null;
        GZIPOutputStream zipper2 = null;
        try {
            resultStream = new ByteArrayOutputStream();
            try {
                zipper = new GZIPOutputStream(resultStream);
            } catch (IOException e) {
                resultStream3 = resultStream;
            } catch (Throwable th) {
                th = th;
                resultStream3 = resultStream;
            }
        } catch (IOException e2) {
        } catch (Throwable th2) {
            th = th2;
        }
        try {
            zipper.write(data);
            if (zipper != null) {
                try {
                    zipper.close();
                } catch (IOException e3) {
                    Slog.e(TAG, "Failed to close the stream.", e3);
                }
            }
            if (resultStream != null) {
                try {
                    resultStream.close();
                    resultStream2 = resultStream;
                } catch (IOException e4) {
                    resultStream2 = null;
                    Slog.e(TAG, "Failed to close the stream.", e4);
                }
            } else {
                resultStream2 = resultStream;
            }
            if (resultStream2 != null) {
                return resultStream2.toByteArray();
            }
            return null;
        } catch (IOException e5) {
            zipper2 = zipper;
            resultStream3 = resultStream;
            if (zipper2 != null) {
                try {
                    zipper2.close();
                } catch (IOException e6) {
                    Slog.e(TAG, "Failed to close the stream.", e6);
                }
            }
            if (resultStream3 == null) {
                return null;
            }
            try {
                resultStream3.close();
                return null;
            } catch (IOException e7) {
                Slog.e(TAG, "Failed to close the stream.", e7);
                return null;
            }
        } catch (Throwable th3) {
            th = th3;
            zipper2 = zipper;
            resultStream3 = resultStream;
            if (zipper2 != null) {
                try {
                    zipper2.close();
                } catch (IOException e8) {
                    Slog.e(TAG, "Failed to close the stream.", e8);
                }
            }
            if (resultStream3 != null) {
                try {
                    resultStream3.close();
                } catch (IOException e9) {
                    Slog.e(TAG, "Failed to close the stream.", e9);
                }
            }
            throw th;
        }
    }

    private static byte[] decompress(byte[] data, int expectedSize) throws Throwable {
        ByteArrayInputStream inputStream = null;
        GZIPInputStream unzipper = null;
        try {
            ByteArrayInputStream inputStream2 = new ByteArrayInputStream(data);
            try {
                GZIPInputStream unzipper2 = new GZIPInputStream(inputStream2);
                try {
                    byte[] result = new byte[expectedSize];
                    int totalReadBytes = 0;
                    while (totalReadBytes < result.length) {
                        int restBytes = result.length - totalReadBytes;
                        int readBytes = unzipper2.read(result, totalReadBytes, restBytes);
                        if (readBytes < 0) {
                            break;
                        }
                        totalReadBytes += readBytes;
                    }
                    if (expectedSize != totalReadBytes) {
                        if (unzipper2 != null) {
                            try {
                                unzipper2.close();
                            } catch (IOException e) {
                                Slog.e(TAG, "Failed to close the stream.", e);
                            }
                        }
                        if (inputStream2 != null) {
                            try {
                                inputStream2.close();
                            } catch (IOException e2) {
                                Slog.e(TAG, "Failed to close the stream.", e2);
                            }
                        }
                        return null;
                    }
                    if (unzipper2 != null) {
                        try {
                            unzipper2.close();
                        } catch (IOException e3) {
                            Slog.e(TAG, "Failed to close the stream.", e3);
                        }
                    }
                    if (inputStream2 != null) {
                        try {
                            inputStream2.close();
                        } catch (IOException e4) {
                            Slog.e(TAG, "Failed to close the stream.", e4);
                        }
                    }
                    return result;
                } catch (IOException e5) {
                    unzipper = unzipper2;
                    inputStream = inputStream2;
                    if (unzipper != null) {
                        try {
                            unzipper.close();
                        } catch (IOException e6) {
                            Slog.e(TAG, "Failed to close the stream.", e6);
                        }
                    }
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e7) {
                            Slog.e(TAG, "Failed to close the stream.", e7);
                        }
                    }
                    return null;
                } catch (Throwable th) {
                    th = th;
                    unzipper = unzipper2;
                    inputStream = inputStream2;
                    if (unzipper != null) {
                        try {
                            unzipper.close();
                        } catch (IOException e8) {
                            Slog.e(TAG, "Failed to close the stream.", e8);
                        }
                    }
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e9) {
                            Slog.e(TAG, "Failed to close the stream.", e9);
                        }
                    }
                    throw th;
                }
            } catch (IOException e10) {
                inputStream = inputStream2;
            } catch (Throwable th2) {
                th = th2;
                inputStream = inputStream2;
            }
        } catch (IOException e11) {
        } catch (Throwable th3) {
            th = th3;
        }
    }
}
