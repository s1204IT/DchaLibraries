package com.google.android.mms.util;

import android.content.Context;
import android.drm.DrmConvertedStatus;
import android.drm.DrmManagerClient;
import android.util.Log;
import com.google.android.mms.pdu.PduPart;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class DrmConvertSession {
    private static final String TAG = "DrmConvertSession";
    private int mConvertSessionId;
    private DrmManagerClient mDrmClient;

    private DrmConvertSession(DrmManagerClient drmClient, int convertSessionId) {
        this.mDrmClient = drmClient;
        this.mConvertSessionId = convertSessionId;
    }

    public static DrmConvertSession open(Context context, String mimeType) {
        DrmManagerClient drmClient;
        DrmManagerClient drmClient2 = null;
        int convertSessionId = -1;
        if (context != null && mimeType != null && !mimeType.equals(UsimPBMemInfo.STRING_NOT_SET)) {
            try {
                try {
                    drmClient = new DrmManagerClient(context);
                    try {
                        convertSessionId = drmClient.openConvertSession(mimeType);
                    } catch (IllegalArgumentException e) {
                        Log.w(TAG, "Conversion of Mimetype: " + mimeType + " is not supported.", e);
                    } catch (IllegalStateException e2) {
                        Log.w(TAG, "Could not access Open DrmFramework.", e2);
                    }
                    drmClient2 = drmClient;
                } catch (IllegalArgumentException e3) {
                    Log.w(TAG, "DrmManagerClient instance could not be created, context is Illegal.");
                    if (drmClient2 != null) {
                    }
                    return null;
                } catch (IllegalStateException e4) {
                    Log.w(TAG, "DrmManagerClient didn't initialize properly.");
                    if (drmClient2 != null) {
                    }
                    return null;
                }
            } catch (IllegalArgumentException e5) {
                drmClient2 = drmClient;
                Log.w(TAG, "DrmManagerClient instance could not be created, context is Illegal.");
                if (drmClient2 != null) {
                }
                return null;
            } catch (IllegalStateException e6) {
                drmClient2 = drmClient;
                Log.w(TAG, "DrmManagerClient didn't initialize properly.");
                if (drmClient2 != null) {
                }
                return null;
            }
        }
        if (drmClient2 != null || convertSessionId < 0) {
            return null;
        }
        return new DrmConvertSession(drmClient2, convertSessionId);
    }

    public byte[] convert(byte[] inBuffer, int size) {
        DrmConvertedStatus convertedStatus;
        byte[] result = null;
        if (inBuffer != null) {
            try {
                if (size != inBuffer.length) {
                    byte[] buf = new byte[size];
                    System.arraycopy(inBuffer, 0, buf, 0, size);
                    convertedStatus = this.mDrmClient.convertData(this.mConvertSessionId, buf);
                } else {
                    convertedStatus = this.mDrmClient.convertData(this.mConvertSessionId, inBuffer);
                }
                if (convertedStatus == null || convertedStatus.statusCode != 1 || convertedStatus.convertedData == null) {
                    return null;
                }
                result = convertedStatus.convertedData;
                return result;
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Buffer with data to convert is illegal. Convertsession: " + this.mConvertSessionId, e);
                return result;
            } catch (IllegalStateException e2) {
                Log.w(TAG, "Could not convert data. Convertsession: " + this.mConvertSessionId, e2);
                return result;
            }
        }
        throw new IllegalArgumentException("Parameter inBuffer is null");
    }

    public int close(String filename) throws Throwable {
        RandomAccessFile rndAccessFile;
        int result = 491;
        if (this.mDrmClient == null || this.mConvertSessionId < 0) {
            return 491;
        }
        try {
            DrmConvertedStatus convertedStatus = this.mDrmClient.closeConvertSession(this.mConvertSessionId);
            if (convertedStatus == null || convertedStatus.statusCode != 1 || convertedStatus.convertedData == null) {
                return 406;
            }
            RandomAccessFile rndAccessFile2 = null;
            try {
                try {
                    rndAccessFile = new RandomAccessFile(filename, "rw");
                } catch (Throwable th) {
                    th = th;
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
                rndAccessFile.seek(convertedStatus.offset);
                rndAccessFile.write(convertedStatus.convertedData);
                result = PduPart.P_CONTENT_TRANSFER_ENCODING;
                if (rndAccessFile != null) {
                    try {
                        rndAccessFile.close();
                    } catch (IOException e5) {
                        result = 492;
                        Log.w(TAG, "Failed to close File:" + filename + ".", e5);
                    }
                }
            } catch (FileNotFoundException e6) {
                e = e6;
                rndAccessFile2 = rndAccessFile;
                result = 492;
                Log.w(TAG, "File: " + filename + " could not be found.", e);
                if (rndAccessFile2 != null) {
                    try {
                        rndAccessFile2.close();
                    } catch (IOException e7) {
                        result = 492;
                        Log.w(TAG, "Failed to close File:" + filename + ".", e7);
                    }
                }
            } catch (IOException e8) {
                e = e8;
                rndAccessFile2 = rndAccessFile;
                result = 492;
                Log.w(TAG, "Could not access File: " + filename + " .", e);
                if (rndAccessFile2 != null) {
                    try {
                        rndAccessFile2.close();
                    } catch (IOException e9) {
                        result = 492;
                        Log.w(TAG, "Failed to close File:" + filename + ".", e9);
                    }
                }
            } catch (IllegalArgumentException e10) {
                e = e10;
                rndAccessFile2 = rndAccessFile;
                result = 492;
                Log.w(TAG, "Could not open file in mode: rw", e);
                if (rndAccessFile2 != null) {
                    try {
                        rndAccessFile2.close();
                    } catch (IOException e11) {
                        result = 492;
                        Log.w(TAG, "Failed to close File:" + filename + ".", e11);
                    }
                }
            } catch (SecurityException e12) {
                e = e12;
                rndAccessFile2 = rndAccessFile;
                Log.w(TAG, "Access to File: " + filename + " was denied denied by SecurityManager.", e);
                if (rndAccessFile2 != null) {
                    try {
                        rndAccessFile2.close();
                    } catch (IOException e13) {
                        result = 492;
                        Log.w(TAG, "Failed to close File:" + filename + ".", e13);
                    }
                }
            } catch (Throwable th2) {
                th = th2;
                rndAccessFile2 = rndAccessFile;
                if (rndAccessFile2 != null) {
                    try {
                        rndAccessFile2.close();
                    } catch (IOException e14) {
                        result = 492;
                        Log.w(TAG, "Failed to close File:" + filename + ".", e14);
                    }
                }
                throw th;
            }
            return result;
        } catch (IllegalStateException e15) {
            Log.w(TAG, "Could not close convertsession. Convertsession: " + this.mConvertSessionId, e15);
            return result;
        }
    }
}
