package com.android.bluetooth.map;

import android.annotation.TargetApi;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.Telephony;
import android.util.Log;
import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.PduComposer;
import com.google.android.mms.pdu.PduPersister;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

@TargetApi(19)
public class MmsFileProvider extends ContentProvider {
    static final Uri CONTENT_URI = Uri.parse("content://com.android.bluetooth.map.MmsFileProvider");
    static final String TAG = "BluetoothMmsFileProvider";
    private PipeWriter mPipeWriter = new PipeWriter();

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        return null;
    }

    @Override
    public int delete(Uri uri, String str, String[] strArr) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String str) throws FileNotFoundException {
        String lastPathSegment = uri.getLastPathSegment();
        if (lastPathSegment == null) {
            throw new FileNotFoundException("Unable to extract message handle from: " + uri);
        }
        try {
            Long.parseLong(lastPathSegment);
            return openPipeHelper(Telephony.Mms.CONTENT_URI.buildUpon().appendEncodedPath(lastPathSegment).build(), null, null, null, this.mPipeWriter);
        } catch (NumberFormatException e) {
            Log.w(TAG, e);
            throw new FileNotFoundException("Unable to extract message handle from: " + uri);
        }
    }

    public class PipeWriter implements ContentProvider.PipeDataWriter<Cursor> {
        public PipeWriter() {
        }

        @Override
        public void writeDataToPipe(ParcelFileDescriptor parcelFileDescriptor, Uri uri, String str, Bundle bundle, Cursor cursor) throws Throwable {
            PduPersister pduPersister;
            FileOutputStream fileOutputStream = bundle;
            if (BluetoothMapService.DEBUG) {
                String str2 = "writeDataToPipe(): uri=" + uri.toString() + " - getLastPathSegment() = " + uri.getLastPathSegment();
                Log.d(MmsFileProvider.TAG, str2);
                fileOutputStream = str2;
            }
            PduPersister pduComposer = null;
            try {
                try {
                    try {
                        fileOutputStream = new FileOutputStream(parcelFileDescriptor.getFileDescriptor());
                        try {
                            pduPersister = PduPersister.getPduPersister(MmsFileProvider.this.getContext());
                        } catch (IOException e) {
                            e = e;
                        } catch (MmsException e2) {
                            e = e2;
                        }
                    } catch (IOException e3) {
                        pduComposer = "IOException: ";
                        Log.w(MmsFileProvider.TAG, "IOException: ", e3);
                    }
                } catch (IOException e4) {
                    e = e4;
                    fileOutputStream = 0;
                } catch (MmsException e5) {
                    e = e5;
                    fileOutputStream = 0;
                } catch (Throwable th) {
                    th = th;
                    fileOutputStream = 0;
                }
            } catch (Throwable th2) {
                th = th2;
            }
            try {
                pduComposer = new PduComposer(MmsFileProvider.this.getContext(), pduPersister.load(uri));
                fileOutputStream.write(pduComposer.make());
                if (pduPersister != null) {
                    pduPersister.release();
                }
                try {
                    fileOutputStream.flush();
                } catch (IOException e6) {
                    pduComposer = "IOException: ";
                    Log.w(MmsFileProvider.TAG, "IOException: ", e6);
                }
                fileOutputStream.close();
                fileOutputStream = fileOutputStream;
            } catch (MmsException e7) {
                e = e7;
                pduComposer = pduPersister;
                Log.w(MmsFileProvider.TAG, (Throwable) e);
                if (pduComposer != null) {
                    pduComposer.release();
                }
                try {
                    fileOutputStream.flush();
                } catch (IOException e8) {
                    pduComposer = "IOException: ";
                    Log.w(MmsFileProvider.TAG, "IOException: ", e8);
                }
                fileOutputStream.close();
                fileOutputStream = fileOutputStream;
            } catch (IOException e9) {
                e = e9;
                pduComposer = pduPersister;
                Log.w(MmsFileProvider.TAG, e);
                if (pduComposer != null) {
                    pduComposer.release();
                }
                try {
                    fileOutputStream.flush();
                } catch (IOException e10) {
                    pduComposer = "IOException: ";
                    Log.w(MmsFileProvider.TAG, "IOException: ", e10);
                }
                fileOutputStream.close();
                fileOutputStream = fileOutputStream;
            } catch (Throwable th3) {
                th = th3;
                pduComposer = pduPersister;
                if (pduComposer != null) {
                    pduComposer.release();
                }
                try {
                    fileOutputStream.flush();
                } catch (IOException e11) {
                    Log.w(MmsFileProvider.TAG, "IOException: ", e11);
                }
                try {
                    fileOutputStream.close();
                    throw th;
                } catch (IOException e12) {
                    Log.w(MmsFileProvider.TAG, "IOException: ", e12);
                    throw th;
                }
            }
        }
    }
}
