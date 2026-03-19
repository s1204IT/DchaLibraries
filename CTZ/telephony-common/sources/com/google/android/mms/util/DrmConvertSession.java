package com.google.android.mms.util;

import android.content.Context;
import android.drm.DrmConvertedStatus;
import android.drm.DrmManagerClient;
import android.util.Log;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class DrmConvertSession {
    private static final String TAG = "DrmConvertSession";
    private int mConvertSessionId;
    private DrmManagerClient mDrmClient;

    private DrmConvertSession(DrmManagerClient drmManagerClient, int i) {
        this.mDrmClient = drmManagerClient;
        this.mConvertSessionId = i;
    }

    public static DrmConvertSession open(Context context, String str) {
        ?? Equals;
        int iOpenConvertSession = -1;
        if (context != null && str != null && (Equals = str.equals("")) == 0) {
            try {
                try {
                    Equals = new DrmManagerClient(context);
                    try {
                        iOpenConvertSession = Equals.openConvertSession(str);
                    } catch (IllegalArgumentException e) {
                        Log.w(TAG, "Conversion of Mimetype: " + str + " is not supported.", e);
                    } catch (IllegalStateException e2) {
                        Log.w(TAG, "Could not access Open DrmFramework.", e2);
                    }
                } catch (IllegalArgumentException e3) {
                    Log.w(TAG, "DrmManagerClient instance could not be created, context is Illegal.");
                    if (Equals != 0) {
                    }
                    return null;
                } catch (IllegalStateException e4) {
                    Log.w(TAG, "DrmManagerClient didn't initialize properly.");
                    if (Equals != 0) {
                    }
                    return null;
                }
            } catch (IllegalArgumentException e5) {
                Equals = 0;
                Log.w(TAG, "DrmManagerClient instance could not be created, context is Illegal.");
                if (Equals != 0) {
                }
                return null;
            } catch (IllegalStateException e6) {
                Equals = 0;
                Log.w(TAG, "DrmManagerClient didn't initialize properly.");
                if (Equals != 0) {
                }
                return null;
            }
        } else {
            Equals = 0;
        }
        if (Equals != 0 || iOpenConvertSession < 0) {
            return null;
        }
        return new DrmConvertSession(Equals, iOpenConvertSession);
    }

    public byte[] convert(byte[] bArr, int i) {
        DrmConvertedStatus drmConvertedStatusConvertData;
        if (bArr != null) {
            try {
                if (i != bArr.length) {
                    byte[] bArr2 = new byte[i];
                    System.arraycopy(bArr, 0, bArr2, 0, i);
                    drmConvertedStatusConvertData = this.mDrmClient.convertData(this.mConvertSessionId, bArr2);
                } else {
                    drmConvertedStatusConvertData = this.mDrmClient.convertData(this.mConvertSessionId, bArr);
                }
                if (drmConvertedStatusConvertData == null || drmConvertedStatusConvertData.statusCode != 1 || drmConvertedStatusConvertData.convertedData == null) {
                    return null;
                }
                return drmConvertedStatusConvertData.convertedData;
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Buffer with data to convert is illegal. Convertsession: " + this.mConvertSessionId, e);
                return null;
            } catch (IllegalStateException e2) {
                Log.w(TAG, "Could not convert data. Convertsession: " + this.mConvertSessionId, e2);
                return null;
            }
        }
        throw new IllegalArgumentException("Parameter inBuffer is null");
    }

    public int close(String str) throws Throwable {
        String str2;
        StringBuilder sb;
        RandomAccessFile randomAccessFile;
        int i = 491;
        if (this.mDrmClient == null || this.mConvertSessionId < 0) {
            return 491;
        }
        try {
            DrmConvertedStatus drmConvertedStatusCloseConvertSession = this.mDrmClient.closeConvertSession(this.mConvertSessionId);
            if (drmConvertedStatusCloseConvertSession == null || drmConvertedStatusCloseConvertSession.statusCode != 1 || drmConvertedStatusCloseConvertSession.convertedData == null) {
                return 406;
            }
            ?? r3 = 0;
            RandomAccessFile randomAccessFile2 = null;
            RandomAccessFile randomAccessFile3 = null;
            RandomAccessFile randomAccessFile4 = null;
            RandomAccessFile randomAccessFile5 = null;
            r3 = 0;
            try {
                try {
                    try {
                        try {
                            randomAccessFile = new RandomAccessFile(str, "rw");
                        } catch (Throwable th) {
                            th = th;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        i = 492;
                    }
                } catch (FileNotFoundException e) {
                    e = e;
                } catch (IOException e2) {
                    e = e2;
                } catch (IllegalArgumentException e3) {
                    e = e3;
                } catch (SecurityException e4) {
                    e = e4;
                }
                try {
                    int i2 = drmConvertedStatusCloseConvertSession.offset;
                    randomAccessFile.seek(i2);
                    randomAccessFile.write(drmConvertedStatusCloseConvertSession.convertedData);
                    i = 200;
                    try {
                        randomAccessFile.close();
                        r3 = i2;
                    } catch (IOException e5) {
                        e = e5;
                        str2 = TAG;
                        StringBuilder sb2 = new StringBuilder();
                        sb2.append("Failed to close File:");
                        sb2.append(str);
                        sb2.append(".");
                        str = sb2.toString();
                        sb = sb2;
                        Log.w(str2, str, e);
                        r3 = sb;
                        return 492;
                    }
                } catch (FileNotFoundException e6) {
                    e = e6;
                    randomAccessFile2 = randomAccessFile;
                    Log.w(TAG, "File: " + str + " could not be found.", e);
                    r3 = randomAccessFile2;
                    if (randomAccessFile2 != null) {
                        try {
                            randomAccessFile2.close();
                            r3 = randomAccessFile2;
                        } catch (IOException e7) {
                            e = e7;
                            str2 = TAG;
                            StringBuilder sb3 = new StringBuilder();
                            sb3.append("Failed to close File:");
                            sb3.append(str);
                            sb3.append(".");
                            str = sb3.toString();
                            sb = sb3;
                            Log.w(str2, str, e);
                            r3 = sb;
                            return 492;
                        }
                    }
                    return 492;
                } catch (IOException e8) {
                    e = e8;
                    randomAccessFile3 = randomAccessFile;
                    Log.w(TAG, "Could not access File: " + str + " .", e);
                    r3 = randomAccessFile3;
                    if (randomAccessFile3 != null) {
                        try {
                            randomAccessFile3.close();
                            r3 = randomAccessFile3;
                        } catch (IOException e9) {
                            e = e9;
                            str2 = TAG;
                            StringBuilder sb4 = new StringBuilder();
                            sb4.append("Failed to close File:");
                            sb4.append(str);
                            sb4.append(".");
                            str = sb4.toString();
                            sb = sb4;
                            Log.w(str2, str, e);
                            r3 = sb;
                            return 492;
                        }
                    }
                    return 492;
                } catch (IllegalArgumentException e10) {
                    e = e10;
                    randomAccessFile4 = randomAccessFile;
                    Log.w(TAG, "Could not open file in mode: rw", e);
                    r3 = randomAccessFile4;
                    if (randomAccessFile4 != null) {
                        try {
                            randomAccessFile4.close();
                            r3 = randomAccessFile4;
                        } catch (IOException e11) {
                            e = e11;
                            str2 = TAG;
                            StringBuilder sb5 = new StringBuilder();
                            sb5.append("Failed to close File:");
                            sb5.append(str);
                            sb5.append(".");
                            str = sb5.toString();
                            sb = sb5;
                            Log.w(str2, str, e);
                            r3 = sb;
                            return 492;
                        }
                    }
                    return 492;
                } catch (SecurityException e12) {
                    e = e12;
                    randomAccessFile5 = randomAccessFile;
                    Log.w(TAG, "Access to File: " + str + " was denied denied by SecurityManager.", e);
                    r3 = randomAccessFile5;
                    if (randomAccessFile5 != null) {
                        try {
                            randomAccessFile5.close();
                            r3 = randomAccessFile5;
                        } catch (IOException e13) {
                            e = e13;
                            str2 = TAG;
                            StringBuilder sb6 = new StringBuilder();
                            sb6.append("Failed to close File:");
                            sb6.append(str);
                            sb6.append(".");
                            str = sb6.toString();
                            sb = sb6;
                            Log.w(str2, str, e);
                            r3 = sb;
                            return 492;
                        }
                    }
                } catch (Throwable th3) {
                    th = th3;
                    r3 = randomAccessFile;
                    if (r3 != 0) {
                        try {
                            r3.close();
                        } catch (IOException e14) {
                            Log.w(TAG, "Failed to close File:" + str + ".", e14);
                            throw th;
                        }
                    }
                    throw th;
                }
                return i;
            } catch (IllegalStateException e15) {
                e = e15;
                i = 492;
            }
        } catch (IllegalStateException e16) {
            e = e16;
        }
        Log.w(TAG, "Could not close convertsession. Convertsession: " + this.mConvertSessionId, e);
        return i;
    }
}
