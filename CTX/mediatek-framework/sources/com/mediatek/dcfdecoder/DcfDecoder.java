package com.mediatek.dcfdecoder;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.MemoryFile;
import android.os.SystemProperties;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.Log;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import libcore.io.IoBridge;

public class DcfDecoder {
    private static final int ACTION_DECODE_FULL_IMAGE = 0;
    private static final int ACTION_JUST_DECODE_BOUND = 1;
    private static final int ACTION_JUST_DECODE_THUMBNAIL = 2;
    private static final int DECODE_THUMBNAIL_FLAG = 256;
    private static final int HEADER_BUFFER_SIZE = 128;
    private static final String TAG = "DRM/DcfDecoder";
    private static final int THUMBNAIL_TARGET_SIZE = 96;
    private static boolean sIsOmaDrmEnabled;

    private static native byte[] nativeDecryptDcfFile(FileDescriptor fileDescriptor, int i, int i2);

    private native byte[] nativeForceDecryptFile(String str, boolean z);

    static {
        sIsOmaDrmEnabled = false;
        sIsOmaDrmEnabled = SystemProperties.getBoolean("ro.vendor.mtk_oma_drm_support", false);
        if (sIsOmaDrmEnabled) {
            System.loadLibrary("dcfdecoderjni");
        }
    }

    public byte[] forceDecryptFile(String str, boolean z) {
        if (str == null) {
            Log.e(TAG, "forceDecryptFile: find null file name!");
            return null;
        }
        return nativeForceDecryptFile(str, z);
    }

    public static Bitmap decodeDrmImageIfNeeded(byte[] bArr, InputStream inputStream, BitmapFactory.Options options) throws Throwable {
        MemoryFile memoryFile;
        Bitmap bitmapDecodeDrmImage;
        MemoryFile memoryFile2 = null;
        if (!sIsOmaDrmEnabled) {
            return null;
        }
        if (options != null && options.inJustDecodeBounds && options.outWidth > 0 && options.outHeight > 0) {
            return null;
        }
        Log.d(TAG, "decodeDrmImageIfNeeded with stream");
        if (bArr == null) {
            return null;
        }
        int i = (bArr[0] & 255) | ((bArr[1] << 8) & 65280);
        Log.d(TAG, "decodeDrmImageIfNeeded: [" + ((int) bArr[0]) + "][" + ((int) bArr[1]) + "][" + ((int) bArr[2]) + "][" + ((int) bArr[3]) + "][" + ((int) bArr[4]) + "]");
        StringBuilder sb = new StringBuilder();
        sb.append("decodeDrmImageIfNeeded: headerSize = ");
        sb.append(i);
        Log.d(TAG, sb.toString());
        if (i < HEADER_BUFFER_SIZE) {
            byte[] bArr2 = new byte[HEADER_BUFFER_SIZE];
            System.arraycopy(bArr, 2, bArr2, 0, i);
            try {
                inputStream.read(bArr2, i, 128 - i);
                bArr = bArr2;
            } catch (IOException e) {
                Log.e(TAG, "decodeDrmImageIfNeeded read header with ", e);
                return null;
            }
        }
        if (!isDrmFile(bArr)) {
            return null;
        }
        try {
            if (inputStream instanceof FileInputStream) {
                bitmapDecodeDrmImage = decodeDrmImage(((FileInputStream) inputStream).getFD(), 0, options);
            } else {
                int iAvailable = inputStream.available();
                memoryFile = new MemoryFile("drm_image", HEADER_BUFFER_SIZE + iAvailable);
                try {
                    try {
                        memoryFile.writeBytes(bArr, 0, 0, HEADER_BUFFER_SIZE);
                        byte[] bArr3 = new byte[iAvailable];
                        inputStream.read(bArr3);
                        memoryFile.writeBytes(bArr3, 0, HEADER_BUFFER_SIZE, iAvailable);
                        bitmapDecodeDrmImage = decodeDrmImage(memoryFile.getFileDescriptor(), memoryFile.length(), options);
                        memoryFile2 = memoryFile;
                    } catch (IOException e2) {
                        e = e2;
                        Log.e(TAG, "decodeDrmImageIfNeeded with ", e);
                        if (memoryFile != null) {
                            memoryFile.close();
                        }
                        return null;
                    }
                } catch (Throwable th) {
                    th = th;
                    if (memoryFile != null) {
                        memoryFile.close();
                    }
                    throw th;
                }
            }
            if (memoryFile2 == null) {
                return bitmapDecodeDrmImage;
            }
            memoryFile2.close();
            return bitmapDecodeDrmImage;
        } catch (IOException e3) {
            e = e3;
            memoryFile = null;
        } catch (Throwable th2) {
            th = th2;
            memoryFile = null;
            if (memoryFile != null) {
            }
            throw th;
        }
    }

    public static Bitmap decodeDrmImageIfNeeded(FileDescriptor fileDescriptor, BitmapFactory.Options options) throws Throwable {
        long jLseek;
        byte[] bArr;
        if (!sIsOmaDrmEnabled) {
            return null;
        }
        if (options != null && options.inJustDecodeBounds && options.outWidth > 0 && options.outHeight > 0) {
            return null;
        }
        Log.d(TAG, "decodeDrmImageIfNeeded with fd");
        try {
            try {
                try {
                    jLseek = Os.lseek(fileDescriptor, 0L, OsConstants.SEEK_CUR);
                    try {
                        Os.lseek(fileDescriptor, 0L, OsConstants.SEEK_SET);
                        bArr = new byte[HEADER_BUFFER_SIZE];
                    } catch (ErrnoException e) {
                        e = e;
                        Log.e(TAG, "decodeDrmImageIfNeeded seek fd to beginning with ", e);
                        if (jLseek != -1) {
                            Os.lseek(fileDescriptor, jLseek, OsConstants.SEEK_SET);
                        }
                        return null;
                    } catch (IOException e2) {
                        e = e2;
                        Log.e(TAG, "decodeDrmImageIfNeeded get header with ", e);
                        if (jLseek != -1) {
                            Os.lseek(fileDescriptor, jLseek, OsConstants.SEEK_SET);
                        }
                        return null;
                    }
                } catch (Throwable th) {
                    th = th;
                    if (-1 != -1) {
                        try {
                            Os.lseek(fileDescriptor, -1L, OsConstants.SEEK_SET);
                        } catch (ErrnoException e3) {
                            Log.e(TAG, "decodeDrmImageIfNeeded seek fd to initial offset with ", e3);
                        }
                    }
                    throw th;
                }
            } catch (ErrnoException e4) {
                e = e4;
                jLseek = -1;
            } catch (IOException e5) {
                e = e5;
                jLseek = -1;
            } catch (Throwable th2) {
                th = th2;
                if (-1 != -1) {
                }
                throw th;
            }
        } catch (ErrnoException e6) {
            Log.e(TAG, "decodeDrmImageIfNeeded seek fd to initial offset with ", e6);
        }
        if (IoBridge.read(fileDescriptor, bArr, 0, HEADER_BUFFER_SIZE) != HEADER_BUFFER_SIZE || !isDrmFile(bArr)) {
            if (jLseek != -1) {
                Os.lseek(fileDescriptor, jLseek, OsConstants.SEEK_SET);
            }
            return null;
        }
        Bitmap bitmapDecodeDrmImage = decodeDrmImage(fileDescriptor, 0, options);
        if (jLseek != -1) {
            try {
                Os.lseek(fileDescriptor, jLseek, OsConstants.SEEK_SET);
            } catch (ErrnoException e7) {
                Log.e(TAG, "decodeDrmImageIfNeeded seek fd to initial offset with ", e7);
            }
        }
        return bitmapDecodeDrmImage;
        return null;
    }

    public static Bitmap decodeDrmImageIfNeeded(byte[] bArr, BitmapFactory.Options options) throws Throwable {
        MemoryFile memoryFile;
        if (!sIsOmaDrmEnabled) {
            return null;
        }
        if (options != null && options.inJustDecodeBounds && options.outWidth > 0 && options.outHeight > 0) {
            return null;
        }
        Log.d(TAG, "decodeDrmImageIfNeeded with data");
        if (!isDrmFile(bArr)) {
            return null;
        }
        try {
            memoryFile = new MemoryFile("drm_image", bArr.length);
            try {
                try {
                    memoryFile.writeBytes(bArr, 0, 0, bArr.length);
                    Bitmap bitmapDecodeDrmImage = decodeDrmImage(memoryFile.getFileDescriptor(), memoryFile.length(), options);
                    memoryFile.close();
                    return bitmapDecodeDrmImage;
                } catch (IOException e) {
                    e = e;
                    Log.e(TAG, "decodeDrmImageIfNeeded with ", e);
                    if (memoryFile != null) {
                        memoryFile.close();
                    }
                    return null;
                }
            } catch (Throwable th) {
                th = th;
                if (memoryFile != null) {
                    memoryFile.close();
                }
                throw th;
            }
        } catch (IOException e2) {
            e = e2;
            memoryFile = null;
        } catch (Throwable th2) {
            th = th2;
            memoryFile = null;
            if (memoryFile != null) {
            }
            throw th;
        }
    }

    private static Bitmap decodeDrmImage(FileDescriptor fileDescriptor, int i, BitmapFactory.Options options) {
        int i2;
        if (options == null) {
            i2 = 0;
        } else if (!options.inJustDecodeBounds) {
            if ((options.inSampleSize & 256) > 0) {
                i2 = 2;
            }
        } else {
            i2 = 1;
        }
        byte[] bArrNativeDecryptDcfFile = nativeDecryptDcfFile(fileDescriptor, i, i2);
        if (bArrNativeDecryptDcfFile == null) {
            return null;
        }
        if (i2 == 2) {
            BitmapFactory.Options options2 = new BitmapFactory.Options();
            options2.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bArrNativeDecryptDcfFile, 0, bArrNativeDecryptDcfFile.length, options2);
            options.inSampleSize = Math.min(options2.outWidth / THUMBNAIL_TARGET_SIZE, options2.outHeight / THUMBNAIL_TARGET_SIZE);
        }
        return BitmapFactory.decodeByteArray(bArrNativeDecryptDcfFile, 0, bArrNativeDecryptDcfFile.length, options);
    }

    private static boolean isDrmFile(byte[] bArr) {
        if (bArr == null || bArr.length < HEADER_BUFFER_SIZE) {
            return false;
        }
        String str = new String(bArr, 0, 8);
        if (str.startsWith("CTA5")) {
            Log.d(TAG, "isDrmFile: this is a cta5 file: " + str);
            return true;
        }
        if (bArr[0] != 1) {
            Log.d(TAG, "isDrmFile: version is not dcf version 1, no oma drm file");
            return false;
        }
        byte b = bArr[1];
        byte b2 = bArr[2];
        if (b <= 0 || b + 3 > HEADER_BUFFER_SIZE || b2 <= 0 || b2 > HEADER_BUFFER_SIZE) {
            Log.d(TAG, "isDrmFile: content type or uri len invalid, not oma drm file, contentType[" + ((int) b) + "] contentUri[" + ((int) b2) + "]");
            return false;
        }
        String str2 = new String(bArr, 3, (int) b);
        if (!str2.contains("/")) {
            Log.d(TAG, "isDrmFile: content type not right, not oma drm file");
            return false;
        }
        Log.d(TAG, "this is a oma drm file: " + str2);
        return true;
    }
}
