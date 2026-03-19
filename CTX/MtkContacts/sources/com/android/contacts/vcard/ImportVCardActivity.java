package com.android.contacts.vcard;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.RelativeSizeSpan;
import android.widget.Button;
import com.android.contacts.R;
import com.android.contacts.activities.RequestImportVCardPermissionsActivity;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.vcard.ImportVCardDialogFragment;
import com.android.contacts.vcard.VCardService;
import com.android.contactsbind.FeedbackHelper;
import com.android.vcard.VCardEntryCounter;
import com.android.vcard.VCardParser;
import com.android.vcard.VCardParser_V21;
import com.android.vcard.VCardParser_V30;
import com.android.vcard.VCardSourceDetector;
import com.android.vcard.exception.VCardException;
import com.android.vcard.exception.VCardNestedException;
import com.android.vcard.exception.VCardVersionException;
import com.mediatek.contacts.eventhandler.BaseEventHandlerActivity;
import com.mediatek.contacts.util.AccountTypeUtils;
import com.mediatek.contacts.util.Log;
import com.mediatek.contacts.util.MtkToast;
import com.mediatek.contacts.util.VcardUtils;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

public class ImportVCardActivity extends BaseEventHandlerActivity implements ImportVCardDialogFragment.Listener {
    private AccountWithDataSet mAccount;
    private List<VCardFile> mAllVCardFileList;
    private ImportRequestConnection mConnection;
    private String mErrorMessage;
    private boolean mIsScanThreadStarted;
    VCardImportExportListener mListener;
    private ProgressDialog mProgressDialogForCachingVCard;
    private ProgressDialog mProgressDialogForScanVCard;
    private VCardCacheThread mVCardCacheThread;
    private VCardScanThread mVCardScanThread;
    private AlertDialog mVcardFileSelectDialog;
    private Handler mHandler = new Handler();
    private CancelListener mCancelListener = new CancelListener();
    private boolean mResumed = false;
    private String mSourcePath = null;
    private String mVolumeName = null;

    private static class VCardFile {
        private final String mCanonicalPath;
        private final long mLastModified;
        private final String mName;

        public VCardFile(String str, String str2, long j) {
            this.mName = str;
            this.mCanonicalPath = str2;
            this.mLastModified = j;
        }

        public String getName() {
            return this.mName;
        }

        public String getCanonicalPath() {
            return this.mCanonicalPath;
        }

        public long getLastModified() {
            return this.mLastModified;
        }
    }

    private class DialogDisplayer implements Runnable {
        private final int mResId;

        public DialogDisplayer(int i) {
            this.mResId = i;
        }

        public DialogDisplayer(String str) {
            this.mResId = R.id.dialog_error_with_message;
            ImportVCardActivity.this.mErrorMessage = str;
        }

        @Override
        public void run() {
            if (!ImportVCardActivity.this.isFinishing()) {
                ImportVCardActivity.this.showDialog(this.mResId);
            }
        }
    }

    private class CancelListener implements DialogInterface.OnCancelListener, DialogInterface.OnClickListener {
        private CancelListener() {
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            ImportVCardActivity.this.setResult(0);
            ImportVCardActivity.this.finish();
        }

        @Override
        public void onCancel(DialogInterface dialogInterface) {
            ImportVCardActivity.this.setResult(0);
            ImportVCardActivity.this.finish();
        }
    }

    private class ImportRequestConnection implements ServiceConnection {
        private VCardService mService;

        private ImportRequestConnection() {
        }

        public void sendImportRequest(List<ImportRequest> list) {
            Log.i("VCardImport", "Send an import request");
            this.mService.handleImportRequest(list, ImportVCardActivity.this.mListener);
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            this.mService = ((VCardService.MyBinder) iBinder).getService();
            if (ImportVCardActivity.this.mVCardCacheThread == null) {
                Log.e("VCardImport", "[onServiceConnected]mVCardCacheThread is null, some error happens.");
                VcardUtils.showErrorInfo(R.string.vcard_import_request_rejected_message, ImportVCardActivity.this);
            } else {
                Log.i("VCardImport", String.format("Connected to VCardService. Kick a vCard cache thread (uri: %s)", Arrays.toString(ImportVCardActivity.this.mVCardCacheThread.getSourceUris())));
                ImportVCardActivity.this.mVCardCacheThread.start();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.i("VCardImport", "Disconnected from VCardService");
        }

        public boolean isServiceBinded() {
            return this.mService != null;
        }

        public void setVCardCaching(boolean z) {
            Log.d("VCardImport", "[setVCardCaching] cache:" + z);
            this.mService.mCaching = z;
        }
    }

    private class VCardCacheThread extends Thread implements DialogInterface.OnCancelListener {
        private boolean mCanceled;
        private final String mDisplayName;
        private final byte[] mSource;
        private String[] mSourceDisplayNames;
        private Uri[] mSourceUris;
        private List<VCardFile> mVCardFileList;
        private VCardParser mVCardParser;
        private PowerManager.WakeLock mWakeLock;

        public VCardCacheThread(Uri[] uriArr, String[] strArr) {
            this.mVCardFileList = null;
            this.mSourceUris = uriArr;
            this.mSourceDisplayNames = strArr;
            this.mSource = null;
            this.mWakeLock = ((PowerManager) ImportVCardActivity.this.getSystemService("power")).newWakeLock(536870918, "VCardImport");
            this.mDisplayName = null;
        }

        public VCardCacheThread(List<VCardFile> list) {
            this.mVCardFileList = null;
            this.mVCardFileList = list;
            this.mSource = null;
            this.mWakeLock = ((PowerManager) ImportVCardActivity.this.getSystemService("power")).newWakeLock(536870918, "VCardImport");
            this.mDisplayName = null;
        }

        public void finalize() {
            if (this.mWakeLock != null && this.mWakeLock.isHeld()) {
                Log.w("VCardImport", "WakeLock is being held.");
                this.mWakeLock.release();
            }
        }

        @Override
        public void run() {
            ImportRequest importRequestConstructImportRequest;
            Log.i("VCardImport", "vCard cache thread starts running.");
            if (ImportVCardActivity.this.mConnection == null) {
                throw new NullPointerException("vCard cache thread must be launched after a service connection is established");
            }
            this.mWakeLock.acquire();
            try {
                try {
                    try {
                        ImportVCardActivity.this.mConnection.setVCardCaching(true);
                    } catch (IOException e) {
                        FeedbackHelper.sendFeedback(ImportVCardActivity.this, "VCardImport", "IOException during caching vCard", e);
                        ImportVCardActivity.this.runOnUiThread(ImportVCardActivity.this.new DialogDisplayer(ImportVCardActivity.this.getString(R.string.fail_reason_io_error)));
                        Log.i("VCardImport", "Finished caching vCard.");
                        ImportVCardActivity.this.mConnection.setVCardCaching(false);
                        this.mWakeLock.release();
                        if (!ImportVCardActivity.this.isFinishing() && ImportVCardActivity.this.mConnection != null && ImportVCardActivity.this.mConnection.isServiceBinded()) {
                            try {
                                ImportVCardActivity.this.unbindService(ImportVCardActivity.this.mConnection);
                                ImportVCardActivity.this.mConnection = null;
                            } catch (IllegalArgumentException e2) {
                                FeedbackHelper.sendFeedback(ImportVCardActivity.this, "VCardImport", "Cannot unbind service connection", e2);
                            }
                        }
                    }
                } catch (FileNotFoundException e3) {
                    Log.w("VCardImport", "[run] the vcf file is not found when import! exception:" + e3);
                    VcardUtils.showErrorInfo(R.string.vcard_import_failed, ImportVCardActivity.this);
                    Log.i("VCardImport", "Finished caching vCard.");
                    ImportVCardActivity.this.mConnection.setVCardCaching(false);
                    this.mWakeLock.release();
                    if (!ImportVCardActivity.this.isFinishing() && ImportVCardActivity.this.mConnection != null && ImportVCardActivity.this.mConnection.isServiceBinded()) {
                        try {
                            ImportVCardActivity.this.unbindService(ImportVCardActivity.this.mConnection);
                            ImportVCardActivity.this.mConnection = null;
                        } catch (IllegalArgumentException e4) {
                            FeedbackHelper.sendFeedback(ImportVCardActivity.this, "VCardImport", "Cannot unbind service connection", e4);
                        }
                    }
                } catch (OutOfMemoryError e5) {
                    FeedbackHelper.sendFeedback(ImportVCardActivity.this, "VCardImport", "OutOfMemoryError occured during caching vCard", e5);
                    System.gc();
                    ImportVCardActivity.this.runOnUiThread(ImportVCardActivity.this.new DialogDisplayer(ImportVCardActivity.this.getString(R.string.fail_reason_low_memory_during_import)));
                    Log.i("VCardImport", "Finished caching vCard.");
                    ImportVCardActivity.this.mConnection.setVCardCaching(false);
                    this.mWakeLock.release();
                    if (!ImportVCardActivity.this.isFinishing() && ImportVCardActivity.this.mConnection != null && ImportVCardActivity.this.mConnection.isServiceBinded()) {
                        try {
                            ImportVCardActivity.this.unbindService(ImportVCardActivity.this.mConnection);
                            ImportVCardActivity.this.mConnection = null;
                        } catch (IllegalArgumentException e6) {
                            FeedbackHelper.sendFeedback(ImportVCardActivity.this, "VCardImport", "Cannot unbind service connection", e6);
                        }
                    }
                }
                if (this.mCanceled) {
                    Log.i("VCardImport", "vCard cache operation is canceled.");
                    Log.i("VCardImport", "Finished caching vCard.");
                    ImportVCardActivity.this.mConnection.setVCardCaching(false);
                    this.mWakeLock.release();
                    if (ImportVCardActivity.this.isFinishing() || ImportVCardActivity.this.mConnection == null || !ImportVCardActivity.this.mConnection.isServiceBinded()) {
                        Log.d("VCardImport", "in VcardCacheThread, Run(), mConnection==null !!! ");
                    } else {
                        try {
                            ImportVCardActivity.this.unbindService(ImportVCardActivity.this.mConnection);
                            ImportVCardActivity.this.mConnection = null;
                        } catch (IllegalArgumentException e7) {
                            FeedbackHelper.sendFeedback(ImportVCardActivity.this, "VCardImport", "Cannot unbind service connection", e7);
                        }
                    }
                    ImportVCardActivity.this.mProgressDialogForCachingVCard.dismiss();
                    ImportVCardActivity.this.mProgressDialogForCachingVCard = null;
                    ImportVCardActivity.this.finish();
                    return;
                }
                ImportVCardActivity importVCardActivity = ImportVCardActivity.this;
                ArrayList arrayList = new ArrayList();
                if (this.mSource == null) {
                    if (this.mSourceUris == null && this.mVCardFileList != null) {
                        int size = this.mVCardFileList.size();
                        Log.d("VCardImport", "parse from file list. size=" + size);
                        Uri[] uriArr = new Uri[size];
                        this.mSourceUris = new Uri[size];
                        this.mSourceDisplayNames = new String[size];
                        Iterator<VCardFile> it = this.mVCardFileList.iterator();
                        int i = 0;
                        while (it.hasNext()) {
                            uriArr[i] = Uri.parse(ImportVCardActivity.this.getEncodeUriString(it.next().getCanonicalPath()));
                            this.mSourceUris[i] = ImportVCardActivity.this.readUriToLocalUri(uriArr[i]);
                            this.mSourceDisplayNames[i] = ImportVCardActivity.this.getDisplayName(uriArr[i]);
                            if (!this.mCanceled && this.mSourceUris[i] != null) {
                                i++;
                                if (i == Integer.MAX_VALUE) {
                                    throw new RuntimeException("Exceeded cache limit");
                                }
                            }
                            Log.i("VCardImport", "vCard cache break: mCanceled=" + this.mCanceled + ", uri=" + Log.anonymize(this.mSourceUris[i]));
                            String[] strArrFileList = ImportVCardActivity.this.fileList();
                            int length = strArrFileList.length;
                            for (int i2 = 0; i2 < length; i2++) {
                                String str = strArrFileList[i2];
                                if (str.startsWith("import_tmp_")) {
                                    ImportVCardActivity.this.deleteFile(str);
                                }
                            }
                            Log.i("VCardImport", "Finished caching vCard.");
                            ImportVCardActivity.this.mConnection.setVCardCaching(false);
                            this.mWakeLock.release();
                            if (ImportVCardActivity.this.isFinishing() || ImportVCardActivity.this.mConnection == null || !ImportVCardActivity.this.mConnection.isServiceBinded()) {
                                Log.d("VCardImport", "in VcardCacheThread, Run(), mConnection==null !!! ");
                            } else {
                                try {
                                    ImportVCardActivity.this.unbindService(ImportVCardActivity.this.mConnection);
                                    ImportVCardActivity.this.mConnection = null;
                                } catch (IllegalArgumentException e8) {
                                    FeedbackHelper.sendFeedback(ImportVCardActivity.this, "VCardImport", "Cannot unbind service connection", e8);
                                }
                            }
                            ImportVCardActivity.this.mProgressDialogForCachingVCard.dismiss();
                            ImportVCardActivity.this.mProgressDialogForCachingVCard = null;
                            ImportVCardActivity.this.finish();
                            return;
                        }
                        Log.e("VCardImport", "parse from file list done. size=" + i);
                    }
                    Uri[] uriArr2 = this.mSourceUris;
                    int length2 = uriArr2.length;
                    int i3 = 0;
                    int i4 = 0;
                    while (true) {
                        if (i3 >= length2) {
                            break;
                        }
                        Uri uri = uriArr2[i3];
                        if (this.mCanceled) {
                            Log.i("VCardImport", "vCard cache operation is canceled.");
                            break;
                        }
                        int i5 = i4 + 1;
                        String str2 = this.mSourceDisplayNames[i4];
                        try {
                            importRequestConstructImportRequest = constructImportRequest(null, uri, str2);
                        } catch (VCardException e9) {
                            FeedbackHelper.sendFeedback(ImportVCardActivity.this, "VCardImport", "Failed to cache vcard", e9);
                            VcardUtils.showFailureNotification(ImportVCardActivity.this, ImportVCardActivity.this.getString(R.string.fail_reason_not_supported), str2, 0, ImportVCardActivity.this.mHandler);
                        } catch (IOException e10) {
                            FeedbackHelper.sendFeedback(ImportVCardActivity.this, "VCardImport", "Failed to cache vcard", e10);
                            VcardUtils.showFailureNotification(ImportVCardActivity.this, ImportVCardActivity.this.getString(R.string.fail_reason_io_error), str2, 0, ImportVCardActivity.this.mHandler);
                        } catch (IllegalArgumentException e11) {
                            FeedbackHelper.sendFeedback(ImportVCardActivity.this, "VCardImport", "Failed to cache vcard", e11);
                            VcardUtils.showFailureNotification(ImportVCardActivity.this, ImportVCardActivity.this.getString(R.string.fail_reason_not_supported), str2, 0, ImportVCardActivity.this.mHandler);
                        }
                        if (this.mCanceled) {
                            Log.i("VCardImport", "vCard cache operation is canceled.");
                            Log.i("VCardImport", "Finished caching vCard.");
                            ImportVCardActivity.this.mConnection.setVCardCaching(false);
                            this.mWakeLock.release();
                            if (ImportVCardActivity.this.isFinishing() || ImportVCardActivity.this.mConnection == null || !ImportVCardActivity.this.mConnection.isServiceBinded()) {
                                Log.d("VCardImport", "in VcardCacheThread, Run(), mConnection==null !!! ");
                            } else {
                                try {
                                    ImportVCardActivity.this.unbindService(ImportVCardActivity.this.mConnection);
                                    ImportVCardActivity.this.mConnection = null;
                                } catch (IllegalArgumentException e12) {
                                    FeedbackHelper.sendFeedback(ImportVCardActivity.this, "VCardImport", "Cannot unbind service connection", e12);
                                }
                            }
                            ImportVCardActivity.this.mProgressDialogForCachingVCard.dismiss();
                            ImportVCardActivity.this.mProgressDialogForCachingVCard = null;
                            ImportVCardActivity.this.finish();
                            return;
                        }
                        arrayList.add(importRequestConstructImportRequest);
                        i3++;
                        i4 = i5;
                    }
                } else {
                    try {
                        arrayList.add(constructImportRequest(this.mSource, null, this.mDisplayName));
                    } catch (VCardException e13) {
                        FeedbackHelper.sendFeedback(ImportVCardActivity.this, "VCardImport", "Failed to cache vcard", e13);
                        Log.e("VCardImport", "Maybe the file is in wrong format", e13);
                        ImportVCardActivity.this.showFailureNotification(R.string.fail_reason_not_supported);
                        Log.i("VCardImport", "Finished caching vCard.");
                        ImportVCardActivity.this.mConnection.setVCardCaching(false);
                        this.mWakeLock.release();
                        if (ImportVCardActivity.this.isFinishing() || ImportVCardActivity.this.mConnection == null || !ImportVCardActivity.this.mConnection.isServiceBinded()) {
                            Log.d("VCardImport", "in VcardCacheThread, Run(), mConnection==null !!! ");
                        } else {
                            try {
                                ImportVCardActivity.this.unbindService(ImportVCardActivity.this.mConnection);
                                ImportVCardActivity.this.mConnection = null;
                            } catch (IllegalArgumentException e14) {
                                FeedbackHelper.sendFeedback(ImportVCardActivity.this, "VCardImport", "Cannot unbind service connection", e14);
                            }
                        }
                        ImportVCardActivity.this.mProgressDialogForCachingVCard.dismiss();
                        ImportVCardActivity.this.mProgressDialogForCachingVCard = null;
                        ImportVCardActivity.this.finish();
                        return;
                    }
                }
                if (arrayList.isEmpty()) {
                    Log.w("VCardImport", "Empty import requests. Ignore it.");
                } else {
                    ImportVCardActivity.this.mConnection.sendImportRequest(arrayList);
                }
                Log.i("VCardImport", "Finished caching vCard.");
                ImportVCardActivity.this.mConnection.setVCardCaching(false);
                this.mWakeLock.release();
                if (ImportVCardActivity.this.isFinishing() || ImportVCardActivity.this.mConnection == null || !ImportVCardActivity.this.mConnection.isServiceBinded()) {
                    Log.d("VCardImport", "in VcardCacheThread, Run(), mConnection==null !!! ");
                } else {
                    try {
                        ImportVCardActivity.this.unbindService(ImportVCardActivity.this.mConnection);
                        ImportVCardActivity.this.mConnection = null;
                    } catch (IllegalArgumentException e15) {
                        FeedbackHelper.sendFeedback(ImportVCardActivity.this, "VCardImport", "Cannot unbind service connection", e15);
                    }
                }
                ImportVCardActivity.this.mProgressDialogForCachingVCard.dismiss();
                ImportVCardActivity.this.mProgressDialogForCachingVCard = null;
                ImportVCardActivity.this.finish();
            } catch (Throwable th) {
                Log.i("VCardImport", "Finished caching vCard.");
                ImportVCardActivity.this.mConnection.setVCardCaching(false);
                this.mWakeLock.release();
                if (ImportVCardActivity.this.isFinishing() || ImportVCardActivity.this.mConnection == null || !ImportVCardActivity.this.mConnection.isServiceBinded()) {
                    Log.d("VCardImport", "in VcardCacheThread, Run(), mConnection==null !!! ");
                } else {
                    try {
                        ImportVCardActivity.this.unbindService(ImportVCardActivity.this.mConnection);
                        ImportVCardActivity.this.mConnection = null;
                    } catch (IllegalArgumentException e16) {
                        FeedbackHelper.sendFeedback(ImportVCardActivity.this, "VCardImport", "Cannot unbind service connection", e16);
                    }
                }
                ImportVCardActivity.this.mProgressDialogForCachingVCard.dismiss();
                ImportVCardActivity.this.mProgressDialogForCachingVCard = null;
                ImportVCardActivity.this.finish();
                throw th;
            }
        }

        private ImportRequest constructImportRequest(byte[] bArr, Uri uri, String str) throws Throwable {
            VCardSourceDetector vCardSourceDetector;
            VCardSourceDetector vCardSourceDetector2;
            VCardEntryCounter vCardEntryCounter;
            ContentResolver contentResolver = ImportVCardActivity.this.getContentResolver();
            boolean z = false;
            VCardEntryCounter vCardEntryCounter2 = null;
            int i = 1;
            try {
                InputStream byteArrayInputStream = bArr != null ? new ByteArrayInputStream(bArr) : contentResolver.openInputStream(uri);
                try {
                    this.mVCardParser = new VCardParser_V21();
                } catch (Throwable th) {
                    th = th;
                }
                try {
                    vCardEntryCounter = new VCardEntryCounter();
                } catch (VCardVersionException e) {
                    vCardEntryCounter = null;
                    vCardSourceDetector2 = null;
                } catch (Throwable th2) {
                    th = th2;
                    vCardSourceDetector2 = null;
                    if (byteArrayInputStream != null) {
                    }
                    throw th;
                }
                try {
                    vCardSourceDetector2 = new VCardSourceDetector();
                    try {
                        this.mVCardParser.addInterpreter(vCardEntryCounter);
                        this.mVCardParser.addInterpreter(vCardSourceDetector2);
                        this.mVCardParser.parse(byteArrayInputStream);
                        if (byteArrayInputStream != null) {
                            try {
                                byteArrayInputStream.close();
                            } catch (VCardNestedException e2) {
                                vCardEntryCounter2 = vCardEntryCounter;
                                vCardSourceDetector = vCardSourceDetector2;
                                Log.w("VCardImport", "Nested Exception is found (it may be false-positive).");
                                return new ImportRequest(ImportVCardActivity.this.mAccount, bArr, uri, str, vCardSourceDetector.getEstimatedType(), vCardSourceDetector.getEstimatedCharset(), i, vCardEntryCounter2.getCount());
                            } catch (IOException e3) {
                            }
                        }
                        vCardEntryCounter2 = vCardEntryCounter;
                        vCardSourceDetector = vCardSourceDetector2;
                    } catch (VCardVersionException e4) {
                        try {
                            byteArrayInputStream.close();
                        } catch (IOException e5) {
                        }
                        byteArrayInputStream = bArr != null ? new ByteArrayInputStream(bArr) : contentResolver.openInputStream(uri);
                        this.mVCardParser = new VCardParser_V30();
                        try {
                            vCardEntryCounter2 = new VCardEntryCounter();
                        } catch (VCardVersionException e6) {
                            vCardEntryCounter2 = vCardEntryCounter;
                        }
                        try {
                            vCardSourceDetector = new VCardSourceDetector();
                            try {
                                try {
                                    this.mVCardParser.addInterpreter(vCardEntryCounter2);
                                    this.mVCardParser.addInterpreter(vCardSourceDetector);
                                    this.mVCardParser.parse(byteArrayInputStream);
                                    if (byteArrayInputStream != null) {
                                        try {
                                            byteArrayInputStream.close();
                                        } catch (VCardNestedException e7) {
                                            Log.w("VCardImport", "Nested Exception is found (it may be false-positive).");
                                            return new ImportRequest(ImportVCardActivity.this.mAccount, bArr, uri, str, vCardSourceDetector.getEstimatedType(), vCardSourceDetector.getEstimatedCharset(), i, vCardEntryCounter2.getCount());
                                        } catch (IOException e8) {
                                        }
                                    }
                                    z = true;
                                } catch (VCardVersionException e9) {
                                    throw new VCardException("vCard with unspported version.");
                                }
                            } catch (Throwable th3) {
                                vCardSourceDetector2 = vCardSourceDetector;
                                th = th3;
                                if (byteArrayInputStream != null) {
                                    try {
                                        try {
                                            byteArrayInputStream.close();
                                        } catch (VCardNestedException e10) {
                                            vCardSourceDetector = vCardSourceDetector2;
                                            Log.w("VCardImport", "Nested Exception is found (it may be false-positive).");
                                            return new ImportRequest(ImportVCardActivity.this.mAccount, bArr, uri, str, vCardSourceDetector.getEstimatedType(), vCardSourceDetector.getEstimatedCharset(), i, vCardEntryCounter2.getCount());
                                        }
                                    } catch (IOException e11) {
                                    }
                                }
                                throw th;
                            }
                        } catch (VCardVersionException e12) {
                            vCardSourceDetector = vCardSourceDetector2;
                            throw new VCardException("vCard with unspported version.");
                        } catch (Throwable th4) {
                            th = th4;
                            if (byteArrayInputStream != null) {
                            }
                            throw th;
                        }
                    }
                } catch (VCardVersionException e13) {
                    vCardSourceDetector2 = null;
                } catch (Throwable th5) {
                    th = th5;
                    vCardSourceDetector2 = null;
                    vCardEntryCounter2 = vCardEntryCounter;
                    if (byteArrayInputStream != null) {
                    }
                    throw th;
                }
                if (z) {
                    i = 2;
                }
            } catch (VCardNestedException e14) {
                vCardSourceDetector = null;
            }
            return new ImportRequest(ImportVCardActivity.this.mAccount, bArr, uri, str, vCardSourceDetector.getEstimatedType(), vCardSourceDetector.getEstimatedCharset(), i, vCardEntryCounter2.getCount());
        }

        public Uri[] getSourceUris() {
            return this.mSourceUris;
        }

        public void cancel() {
            this.mCanceled = true;
            if (this.mVCardParser != null) {
                this.mVCardParser.cancel();
            }
        }

        @Override
        public void onCancel(DialogInterface dialogInterface) {
            Log.i("VCardImport", "Cancel request has come. Abort caching vCard.");
            cancel();
        }
    }

    private class ImportTypeSelectedListener implements DialogInterface.OnClickListener {
        private int mCurrentIndex;

        private ImportTypeSelectedListener() {
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            if (i == -1) {
                switch (this.mCurrentIndex) {
                    case 1:
                        ImportVCardActivity.this.showDialog(R.id.dialog_select_multiple_vcard);
                        ImportVCardActivity.this.mVcardFileSelectDialog.getButton(-1).setEnabled(false);
                        break;
                    case 2:
                        ImportVCardActivity.this.importVCardFromSDCard((List<VCardFile>) ImportVCardActivity.this.mAllVCardFileList);
                        break;
                    default:
                        ImportVCardActivity.this.showDialog(R.id.dialog_select_one_vcard);
                        break;
                }
                ImportVCardActivity.this.setResult(11112);
                return;
            }
            if (i == -2) {
                ImportVCardActivity.this.finish();
            } else {
                this.mCurrentIndex = i;
            }
        }
    }

    private class VCardSelectedListener implements DialogInterface.OnClickListener, DialogInterface.OnMultiChoiceClickListener {
        private int mCurrentIndex = 0;
        private Set<Integer> mSelectedIndexSet;

        public VCardSelectedListener(boolean z) {
            if (z) {
                this.mSelectedIndexSet = new HashSet();
            }
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) throws Throwable {
            boolean z = false;
            if (i == -1) {
                if (this.mSelectedIndexSet == null) {
                    ImportVCardActivity.this.importVCardFromSDCard((VCardFile) ImportVCardActivity.this.mAllVCardFileList.get(this.mCurrentIndex));
                } else {
                    ArrayList arrayList = new ArrayList();
                    int size = ImportVCardActivity.this.mAllVCardFileList.size();
                    for (int i2 = 0; i2 < size; i2++) {
                        if (this.mSelectedIndexSet.contains(Integer.valueOf(i2))) {
                            arrayList.add((VCardFile) ImportVCardActivity.this.mAllVCardFileList.get(i2));
                        }
                    }
                    ImportVCardActivity.this.importVCardFromSDCard(arrayList);
                }
                ImportVCardActivity.this.setResult(11112);
                return;
            }
            if (i == -2) {
                ImportVCardActivity.this.finish();
                return;
            }
            this.mCurrentIndex = i;
            if (this.mSelectedIndexSet != null) {
                Button button = ImportVCardActivity.this.mVcardFileSelectDialog.getButton(-1);
                if (this.mSelectedIndexSet.contains(Integer.valueOf(i))) {
                    this.mSelectedIndexSet.remove(Integer.valueOf(i));
                    if (this.mSelectedIndexSet.size() != 0) {
                    }
                    button.setEnabled(z);
                }
                this.mSelectedIndexSet.add(Integer.valueOf(i));
                z = true;
                button.setEnabled(z);
            }
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i, boolean z) throws Throwable {
            if (this.mSelectedIndexSet == null || this.mSelectedIndexSet.contains(Integer.valueOf(i)) == z) {
                Log.e("VCardImport", String.format("Inconsist state in index %d (%s)", Integer.valueOf(i), Log.anonymize(((VCardFile) ImportVCardActivity.this.mAllVCardFileList.get(i)).getCanonicalPath())));
            } else {
                onClick(dialogInterface, i);
            }
        }
    }

    private class VCardScanThread extends Thread implements DialogInterface.OnCancelListener, DialogInterface.OnClickListener {
        private File mRootDirectory;
        private PowerManager.WakeLock mWakeLock;
        private boolean mCanceled = false;
        private boolean mGotIOException = false;
        private Set<String> mCheckedPaths = new HashSet();

        private class CanceledException extends Exception {
            private CanceledException() {
            }
        }

        public VCardScanThread(File file) {
            this.mRootDirectory = file;
            this.mWakeLock = ((PowerManager) ImportVCardActivity.this.getSystemService("power")).newWakeLock(536870918, "VCardImport");
        }

        @Override
        public void run() {
            ImportVCardActivity.this.mAllVCardFileList = new Vector();
            try {
                try {
                    this.mWakeLock.acquire();
                    getVCardFileRecursively(this.mRootDirectory);
                } catch (CanceledException e) {
                    this.mCanceled = true;
                } catch (IOException e2) {
                    this.mGotIOException = true;
                }
                this.mWakeLock.release();
                if (this.mCanceled) {
                    ImportVCardActivity.this.mAllVCardFileList = null;
                }
                ImportVCardActivity.this.mProgressDialogForScanVCard.dismiss();
                ImportVCardActivity.this.mProgressDialogForScanVCard = null;
                if (this.mGotIOException) {
                    ImportVCardActivity.this.runOnUiThread(ImportVCardActivity.this.new DialogDisplayer(R.id.dialog_io_exception));
                    return;
                }
                if (!this.mCanceled) {
                    int size = ImportVCardActivity.this.mAllVCardFileList.size();
                    ImportVCardActivity importVCardActivity = ImportVCardActivity.this;
                    if (size != 0) {
                        ImportVCardActivity.this.startVCardSelectAndImport();
                        return;
                    } else {
                        ImportVCardActivity.this.runOnUiThread(ImportVCardActivity.this.new DialogDisplayer(R.id.dialog_vcard_not_found));
                        return;
                    }
                }
                ImportVCardActivity.this.finish();
            } catch (Throwable th) {
                this.mWakeLock.release();
                throw th;
            }
        }

        private void getVCardFileRecursively(File file) throws CanceledException, IOException {
            if (this.mCanceled) {
                throw new CanceledException();
            }
            if (file.listFiles() == null) {
                if (!TextUtils.equals(file.getCanonicalPath(), this.mRootDirectory.getCanonicalPath().concat(".android_secure"))) {
                    Log.w("VCardImport", "listFiles() returned null (directory: " + Log.anonymize(file) + ")");
                    return;
                }
                return;
            }
            try {
                for (File file2 : file.listFiles()) {
                    if (this.mCanceled) {
                        throw new CanceledException();
                    }
                    String canonicalPath = file2.getCanonicalPath();
                    if (!this.mCheckedPaths.contains(canonicalPath)) {
                        this.mCheckedPaths.add(canonicalPath);
                        if (file2.isDirectory()) {
                            getVCardFileRecursively(file2);
                        } else if (canonicalPath.toLowerCase().endsWith(".vcf") && file2.canRead()) {
                            ImportVCardActivity.this.mAllVCardFileList.add(new VCardFile(file2.getName(), canonicalPath, file2.lastModified()));
                        }
                    }
                }
            } catch (NullPointerException e) {
                Log.e("VCardImport", "Null pointer file path:" + Log.anonymize(file));
            }
        }

        @Override
        public void onCancel(DialogInterface dialogInterface) {
            this.mCanceled = true;
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            if (i == -2) {
                this.mCanceled = true;
            }
        }
    }

    private void startVCardSelectAndImport() {
        int size = this.mAllVCardFileList.size();
        if (getResources().getBoolean(R.bool.config_import_all_vcard_from_sdcard_automatically) || size == 1) {
            if (size == 1) {
                setResult(11112);
            }
            importVCardFromSDCard(this.mAllVCardFileList);
        } else if (getResources().getBoolean(R.bool.config_allow_users_select_all_vcard_import)) {
            runOnUiThread(new DialogDisplayer(R.id.dialog_select_import_type));
        } else {
            runOnUiThread(new DialogDisplayer(R.id.dialog_select_one_vcard));
        }
    }

    private void importVCardFromSDCard(List<VCardFile> list) {
        Log.d("VCardImport", "[importVCardFromSDCard]");
        importVCard(list);
    }

    private void importVCardFromSDCard(VCardFile vCardFile) throws Throwable {
        Log.d("VCardImport", "[importVCardFromSDCard:vcardFile]");
        Uri uri = Uri.parse(getEncodeUriString(vCardFile.getCanonicalPath()));
        Uri uriToLocalUri = readUriToLocalUri(uri);
        String displayName = getDisplayName(uri);
        if (uriToLocalUri != null) {
            importVCard(new Uri[]{uriToLocalUri}, new String[]{displayName});
        } else {
            Log.w("VCardImport", "No local URI for vCard import");
            finish();
        }
    }

    private void importVCard(final List<VCardFile> list) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!ImportVCardActivity.this.isFinishing() && ImportVCardActivity.this.mResumed) {
                    ImportVCardActivity.this.mVCardCacheThread = ImportVCardActivity.this.new VCardCacheThread(list);
                    ImportVCardActivity.this.mListener = new NotificationImportExportListener(ImportVCardActivity.this);
                    ImportVCardActivity.this.showDialog(R.id.dialog_cache_vcard);
                }
            }
        });
    }

    private void importVCard(Uri uri, String str) {
        importVCard(new Uri[]{uri}, new String[]{str});
    }

    private void importVCard(final Uri[] uriArr, final String[] strArr) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!ImportVCardActivity.this.isFinishing()) {
                    ImportVCardActivity.this.mVCardCacheThread = ImportVCardActivity.this.new VCardCacheThread(uriArr, strArr);
                    ImportVCardActivity.this.mListener = new NotificationImportExportListener(ImportVCardActivity.this);
                    ImportVCardActivity.this.showDialog(R.id.dialog_cache_vcard);
                }
            }
        });
    }

    private String getDisplayName(Uri uri) throws Throwable {
        Cursor cursorQuery;
        String string = null;
        if (uri == null) {
            return null;
        }
        try {
            cursorQuery = getContentResolver().query(uri, new String[]{"_display_name"}, null, null, null);
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.getCount() > 0 && cursorQuery.moveToFirst()) {
                        if (cursorQuery.getCount() > 1) {
                            Log.w("VCardImport", "Unexpected multiple rows: " + cursorQuery.getCount());
                        }
                        int columnIndex = cursorQuery.getColumnIndex("_display_name");
                        if (columnIndex >= 0) {
                            string = cursorQuery.getString(columnIndex);
                        }
                    }
                } catch (Throwable th) {
                    th = th;
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    throw th;
                }
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            if (TextUtils.isEmpty(string)) {
                if (isVcfSuffixFileUri(uri)) {
                    string = uri.getLastPathSegment();
                } else {
                    string = getDisplayNameByQueryMediaProvider(uri);
                }
            }
            Log.d("VCardImport", "[getDisplayName] sourceUri: " + Log.anonymize(uri) + ",displayName : " + Log.anonymize(string));
            return string;
        } catch (Throwable th2) {
            th = th2;
            cursorQuery = null;
        }
    }

    private Uri copyTo(Uri uri, String str) throws Throwable {
        ReadableByteChannel readableByteChannelNewChannel;
        FileChannel channel;
        Uri uri2;
        Log.i("VCardImport", String.format("Copy a Uri to app local storage (%s -> %s)", uri, str));
        try {
            readableByteChannelNewChannel = Channels.newChannel(getContentResolver().openInputStream(uri));
            try {
                uri2 = Uri.parse(getFileStreamPath(str).toURI().toString());
                channel = openFileOutput(str, 0).getChannel();
            } catch (Throwable th) {
                th = th;
                channel = null;
            }
        } catch (Throwable th2) {
            th = th2;
            readableByteChannelNewChannel = null;
            channel = null;
        }
        try {
            ByteBuffer byteBufferAllocateDirect = ByteBuffer.allocateDirect(8192);
            while (readableByteChannelNewChannel.read(byteBufferAllocateDirect) != -1) {
                byteBufferAllocateDirect.flip();
                channel.write(byteBufferAllocateDirect);
                byteBufferAllocateDirect.compact();
            }
            byteBufferAllocateDirect.flip();
            while (byteBufferAllocateDirect.hasRemaining()) {
                channel.write(byteBufferAllocateDirect);
            }
            if (readableByteChannelNewChannel != null) {
                try {
                    readableByteChannelNewChannel.close();
                } catch (IOException e) {
                    Log.w("VCardImport", "Failed to close inputChannel.");
                }
            }
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException e2) {
                    Log.w("VCardImport", "Failed to close outputChannel");
                }
            }
            return uri2;
        } catch (Throwable th3) {
            th = th3;
            if (readableByteChannelNewChannel != null) {
                try {
                    readableByteChannelNewChannel.close();
                } catch (IOException e3) {
                    Log.w("VCardImport", "Failed to close inputChannel.");
                }
            }
            if (channel != null) {
                try {
                    channel.close();
                    throw th;
                } catch (IOException e4) {
                    Log.w("VCardImport", "Failed to close outputChannel");
                    throw th;
                }
            }
            throw th;
        }
    }

    private String readUriToLocalFile(Uri uri) {
        int i = 0;
        while (true) {
            String str = "import_tmp_" + i + ".vcf";
            if (getFileStreamPath(str).exists()) {
                if (i == Integer.MAX_VALUE) {
                    throw new RuntimeException("Exceeded cache limit");
                }
                i++;
            } else {
                try {
                    copyTo(uri, str);
                    Log.d("VCardImport", "[readUriToLocalFile] sourceUri: " + Log.anonymize(uri) + ",localFilename : " + Log.anonymize(str));
                    if (str == null) {
                        Log.e("VCardImport", "Cannot load uri to local storage.");
                        showFailureNotification(R.string.fail_reason_io_error);
                        return null;
                    }
                    return str;
                } catch (IOException | SecurityException e) {
                    FeedbackHelper.sendFeedback(this, "VCardImport", "Failed to copy vcard to local file", e);
                    showFailureNotification(R.string.fail_reason_io_error);
                    return null;
                }
            }
        }
    }

    private Uri readUriToLocalUri(Uri uri) {
        String uriToLocalFile = readUriToLocalFile(uri);
        if (uriToLocalFile == null) {
            return null;
        }
        return Uri.parse(getFileStreamPath(uriToLocalFile).toURI().toString());
    }

    private Dialog getSelectImportTypeDialog() {
        int size;
        if (this.mAllVCardFileList != null) {
            size = this.mAllVCardFileList.size();
        } else {
            size = 0;
        }
        if (size == 0) {
            Log.w("VCardImport", "[getSelectImportTypeDialog] size: " + size);
            return null;
        }
        ImportTypeSelectedListener importTypeSelectedListener = new ImportTypeSelectedListener();
        AlertDialog.Builder negativeButton = new AlertDialog.Builder(this).setTitle(R.string.select_vcard_title).setPositiveButton(android.R.string.ok, importTypeSelectedListener).setOnCancelListener(this.mCancelListener).setNegativeButton(android.R.string.cancel, this.mCancelListener);
        negativeButton.setSingleChoiceItems(new String[]{getString(R.string.import_one_vcard_string), getString(R.string.import_multiple_vcard_string), getString(R.string.import_all_vcard_string)}, 0, importTypeSelectedListener);
        return negativeButton.create();
    }

    private Dialog getVCardFileSelectDialog(boolean z) {
        int size;
        if (this.mAllVCardFileList != null) {
            size = this.mAllVCardFileList.size();
        } else {
            size = 0;
        }
        if (size == 0) {
            Log.w("VCardImport", "[getVCardFileSelectDialog] size: " + size);
            return null;
        }
        VCardSelectedListener vCardSelectedListener = new VCardSelectedListener(z);
        AlertDialog.Builder negativeButton = new AlertDialog.Builder(this).setTitle(R.string.select_vcard_title).setPositiveButton(android.R.string.ok, vCardSelectedListener).setOnCancelListener(this.mCancelListener).setNegativeButton(android.R.string.cancel, this.mCancelListener);
        CharSequence[] charSequenceArr = new CharSequence[size];
        for (int i = 0; i < size; i++) {
            VCardFile vCardFile = this.mAllVCardFileList.get(i);
            SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
            spannableStringBuilder.append((CharSequence) vCardFile.getName());
            spannableStringBuilder.append('\n');
            int length = spannableStringBuilder.length();
            spannableStringBuilder.append((CharSequence) ("(" + DateUtils.formatDateTime(this, vCardFile.getLastModified(), 21) + ")"));
            spannableStringBuilder.setSpan(new RelativeSizeSpan(0.7f), length, spannableStringBuilder.length(), 33);
            charSequenceArr[i] = spannableStringBuilder;
        }
        if (z) {
            negativeButton.setMultiChoiceItems(charSequenceArr, (boolean[]) null, vCardSelectedListener);
        } else {
            negativeButton.setSingleChoiceItems(charSequenceArr, 0, vCardSelectedListener);
        }
        return negativeButton.create();
    }

    @Override
    protected void onCreate(Bundle bundle) throws Throwable {
        String str;
        String stringExtra;
        String stringExtra2;
        String stringExtra3;
        String displayName;
        super.onCreate(bundle);
        Uri data = getIntent().getData();
        Log.d("VCardImport", "[onCreate] sourceUri: " + Log.anonymize(data));
        if (isStorageUriEx(data) && RequestImportVCardPermissionsActivity.startPermissionActivity(this, isCallerSelf(this))) {
            Log.i("VCardImport", "[onCreate] it is storage uri and need request permission!");
            return;
        }
        if (data == null) {
            str = null;
        } else {
            String stringExtra4 = getIntent().getStringExtra("com.android.contacts.vcard.LOCAL_TMP_FILE_NAME");
            String stringExtra5 = getIntent().getStringExtra("com.android.contacts.vcard.SOURCE_URI_DISPLAY_NAME");
            if (TextUtils.isEmpty(stringExtra4)) {
                stringExtra4 = readUriToLocalFile(data);
                displayName = getDisplayName(data);
                if (stringExtra4 == null) {
                    Log.e("VCardImport", "Cannot load uri to local storage.");
                    showFailureNotification(R.string.fail_reason_io_error);
                    return;
                } else {
                    getIntent().putExtra("com.android.contacts.vcard.LOCAL_TMP_FILE_NAME", stringExtra4);
                    getIntent().putExtra("com.android.contacts.vcard.SOURCE_URI_DISPLAY_NAME", displayName);
                }
            } else {
                displayName = stringExtra5;
            }
            Uri uri = Uri.parse(getFileStreamPath(stringExtra4).toURI().toString());
            Log.d("VCardImport", "[onCreate] localTmpFileName: " + Log.anonymize(stringExtra4) + ",sourceDisplayName: " + Log.anonymize(displayName) + ",sourceUri: " + Log.anonymize(uri));
            str = displayName;
            data = uri;
        }
        if (RequestImportVCardPermissionsActivity.startPermissionActivity(this, isCallerSelf(this))) {
            Log.w("VCardImport", "[onCreate] need request permission!");
            return;
        }
        Intent intent = getIntent();
        if (intent != null) {
            stringExtra = intent.getStringExtra("account_name");
            stringExtra2 = intent.getStringExtra("account_type");
            stringExtra3 = intent.getStringExtra("data_set");
            this.mSourcePath = intent.getStringExtra("source_path");
        } else {
            Log.e("VCardImport", "intent does not exist");
            stringExtra = null;
            stringExtra2 = null;
            stringExtra3 = null;
        }
        if (!TextUtils.isEmpty(stringExtra) && !TextUtils.isEmpty(stringExtra2)) {
            this.mAccount = new AccountWithDataSet(stringExtra, stringExtra2, stringExtra3);
        } else {
            List<AccountWithDataSet> listBlockForWritableAccounts = AccountTypeManager.getInstance(this).blockForWritableAccounts();
            if (listBlockForWritableAccounts.size() == 0) {
                this.mAccount = null;
            } else if (listBlockForWritableAccounts.size() == 1) {
                this.mAccount = listBlockForWritableAccounts.get(0);
            } else {
                int size = listBlockForWritableAccounts.size();
                for (AccountWithDataSet accountWithDataSet : listBlockForWritableAccounts) {
                    if (AccountTypeUtils.isAccountTypeIccCard(accountWithDataSet.type)) {
                        size--;
                    } else {
                        this.mAccount = accountWithDataSet;
                    }
                }
                if (size > 1) {
                    startActivityForResult(new Intent(this, (Class<?>) SelectAccountActivity.class).setFlags(67108864), 0);
                    return;
                }
            }
        }
        if (isCallerSelf(this)) {
            startImport(data, str);
        } else {
            ImportVCardDialogFragment.show(this, data, str);
        }
    }

    @Override
    protected void onPause() {
        this.mResumed = false;
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.mResumed = true;
    }

    private static boolean isCallerSelf(Activity activity) {
        String packageName;
        ComponentName callingActivity = activity.getCallingActivity();
        if (callingActivity == null || (packageName = callingActivity.getPackageName()) == null) {
            return false;
        }
        return packageName.equals(activity.getApplicationContext().getPackageName());
    }

    @Override
    public void onImportVCardConfirmed(Uri uri, String str) {
        startImport(uri, str);
    }

    @Override
    public void onImportVCardDenied() {
        finish();
    }

    @Override
    public void onActivityResult(int i, int i2, Intent intent) throws Throwable {
        Uri uriToLocalUri;
        Log.i("VCardImport", "[onActivityResult] requestCode:" + i + ",resultCode: " + i2 + ",intent: " + intent);
        boolean z = false;
        if (i == 0) {
            if (i2 == -1) {
                this.mAccount = new AccountWithDataSet(intent.getStringExtra("account_name"), intent.getStringExtra("account_type"), intent.getStringExtra("data_set"));
                List<AccountWithDataSet> listBlockForWritableAccounts = AccountTypeManager.getInstance(this).blockForWritableAccounts();
                if (listBlockForWritableAccounts != null && listBlockForWritableAccounts.size() >= 1) {
                    int i3 = 0;
                    while (true) {
                        if (i3 >= listBlockForWritableAccounts.size()) {
                            break;
                        }
                        if (!this.mAccount.equals(listBlockForWritableAccounts.get(i3))) {
                            i3++;
                        } else {
                            z = true;
                            break;
                        }
                    }
                    if (!z) {
                        MtkToast.toast(this, getString(R.string.vcard_import_failed), 1);
                        Log.e("VCardImport", "[onActivityResult] " + this.mAccount + " doesn't existed !");
                        finish();
                    }
                }
                Uri data = getIntent().getData();
                if (data == null) {
                    startImport(data, null);
                    return;
                } else {
                    startImport(Uri.parse(getFileStreamPath(getIntent().getStringExtra("com.android.contacts.vcard.LOCAL_TMP_FILE_NAME")).toURI().toString()), getIntent().getStringExtra("com.android.contacts.vcard.SOURCE_URI_DISPLAY_NAME"));
                    return;
                }
            }
            if (i2 != 0) {
                Log.w("VCardImport", "Result code was not OK nor CANCELED: " + i2);
            }
            finish();
            return;
        }
        if (i == 100) {
            if (i2 == -1) {
                ClipData clipData = intent.getClipData();
                if (clipData != null) {
                    ArrayList arrayList = new ArrayList();
                    ArrayList arrayList2 = new ArrayList();
                    for (int i4 = 0; i4 < clipData.getItemCount(); i4++) {
                        Uri uri = clipData.getItemAt(i4).getUri();
                        if (uri != null && (uriToLocalUri = readUriToLocalUri(uri)) != null) {
                            String displayName = getDisplayName(uri);
                            arrayList.add(uriToLocalUri);
                            arrayList2.add(displayName);
                        }
                    }
                    if (arrayList.isEmpty()) {
                        Log.w("VCardImport", "No vCard was selected for import");
                        finish();
                        return;
                    }
                    Log.i("VCardImport", "Multiple vCards selected for import: " + arrayList);
                    importVCard((Uri[]) arrayList.toArray(new Uri[0]), (String[]) arrayList2.toArray(new String[0]));
                    return;
                }
                Uri data2 = intent.getData();
                if (data2 != null) {
                    Log.i("VCardImport", "vCard selected for import: " + data2);
                    Uri uriToLocalUri2 = readUriToLocalUri(data2);
                    if (uriToLocalUri2 != null) {
                        importVCard(uriToLocalUri2, getDisplayName(data2));
                        return;
                    } else {
                        Log.w("VCardImport", "No local URI for vCard import");
                        finish();
                        return;
                    }
                }
                Log.w("VCardImport", "No vCard was selected for import");
                finish();
                return;
            }
            if (i2 != 0) {
                Log.w("VCardImport", "Result code was not OK nor CANCELED" + i2);
            }
            finish();
        }
    }

    private void startImport(Uri uri, String str) {
        Log.d("VCardImport", "[startImport] uri: " + Log.anonymize(uri) + ",sourceDisplayName: " + Log.anonymize(str));
        if (uri != null) {
            Log.i("VCardImport", "Starting vCard import using Uri " + uri);
            importVCard(uri, str);
            return;
        }
        Log.i("VCardImport", "Start vCard without Uri. The user will select vCard manually.");
        doScanExternalStorageAndImportVCard();
    }

    @Override
    protected Dialog onCreateDialog(int i, Bundle bundle) {
        if (i == R.id.dialog_cache_vcard) {
            Log.d("VCardImport", "[onCreateDialog]dialog_cache_vcard");
            if (this.mProgressDialogForCachingVCard == null) {
                Log.d("VCardImport", "[onCreateDialog][dialog_cache_vcard]Dialog first created");
                String string = getString(R.string.caching_vcard_title);
                String string2 = getString(R.string.caching_vcard_message);
                this.mProgressDialogForCachingVCard = new ProgressDialog(this);
                this.mProgressDialogForCachingVCard.setTitle(string);
                this.mProgressDialogForCachingVCard.setMessage(string2);
                this.mProgressDialogForCachingVCard.setProgressStyle(0);
                this.mProgressDialogForCachingVCard.setOnCancelListener(this.mVCardCacheThread);
                startVCardService();
            }
            return this.mProgressDialogForCachingVCard;
        }
        if (i == R.id.dialog_error_with_message) {
            Log.d("VCardImport", "[onCreateDialog]dialog_error_with_message");
            String string3 = this.mErrorMessage;
            if (TextUtils.isEmpty(string3)) {
                Log.e("VCardImport", "Error message is null while it must not.");
                string3 = getString(R.string.fail_reason_unknown);
            }
            return new AlertDialog.Builder(this).setTitle(getString(R.string.reading_vcard_failed_title)).setIconAttribute(android.R.attr.alertDialogIcon).setMessage(string3).setOnCancelListener(this.mCancelListener).setPositiveButton(android.R.string.ok, this.mCancelListener).create();
        }
        if (i == R.id.dialog_io_exception) {
            Log.d("VCardImport", "[onCreateDialog]dialog_io_exception");
            return new AlertDialog.Builder(this).setMessage(getString(R.string.fail_reason_unknown, new Object[]{getString(R.string.fail_reason_io_error)})).setOnCancelListener(this.mCancelListener).setPositiveButton(android.R.string.ok, this.mCancelListener).create();
        }
        if (i != R.id.dialog_vcard_not_found) {
            switch (i) {
                case R.id.dialog_sdcard_not_found:
                    Log.d("VCardImport", "[onCreateDialog]dialog_sdcard_not_found");
                    return new AlertDialog.Builder(this).setMessage(R.string.no_sdcard_message).setOnCancelListener(this.mCancelListener).setPositiveButton(android.R.string.ok, this.mCancelListener).create();
                case R.id.dialog_searching_vcard:
                    Log.d("VCardImport", "[onCreateDialog]dialog_search_vcard");
                    if (this.mProgressDialogForScanVCard == null && this.mVCardScanThread != null) {
                        if (this.mIsScanThreadStarted) {
                            Log.w("VCardImport", "[onCreateDialog] Ignore !!!", new Exception());
                            return null;
                        }
                        this.mProgressDialogForScanVCard = ProgressDialog.show(this, "", getString(R.string.searching_vcard_message), true, false);
                        this.mProgressDialogForScanVCard.setOnCancelListener(this.mVCardScanThread);
                        this.mVCardScanThread.start();
                        this.mIsScanThreadStarted = true;
                    }
                    return this.mProgressDialogForScanVCard;
                case R.id.dialog_select_import_type:
                    Log.d("VCardImport", "[onCreateDialog]dialog_select_import_type");
                    return getSelectImportTypeDialog();
                case R.id.dialog_select_multiple_vcard:
                    Log.d("VCardImport", "[onCreateDialog]dialog_select_multiple_vcard");
                    this.mVcardFileSelectDialog = (AlertDialog) getVCardFileSelectDialog(true);
                    return this.mVcardFileSelectDialog;
                case R.id.dialog_select_one_vcard:
                    Log.d("VCardImport", "[onCreateDialog]dialog_select_one_vcard");
                    return getVCardFileSelectDialog(false);
                default:
                    Log.w("VCardImport", "[onCreateDialog]res id is invalid: " + i);
                    return super.onCreateDialog(i, bundle);
            }
        }
        Log.d("VCardImport", "[onCreateDialog]dialog_vcard_not_found");
        return new AlertDialog.Builder(this).setMessage(getString(R.string.import_no_vcard_dialog_text, new Object[]{this.mVolumeName})).setOnCancelListener(this.mCancelListener).setPositiveButton(android.R.string.ok, this.mCancelListener).create();
    }

    void startVCardService() {
        this.mConnection = new ImportRequestConnection();
        Log.i("VCardImport", "Bind to VCardService.");
        startService(new Intent(this, (Class<?>) VCardService.class));
        bindService(new Intent(this, (Class<?>) VCardService.class), this.mConnection, 1);
    }

    @Override
    protected void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);
        if (this.mProgressDialogForCachingVCard != null) {
            Log.i("VCardImport", "Cache thread is still running. Show progress dialog again.");
            showDialog(R.id.dialog_cache_vcard);
        }
    }

    private void doScanExternalStorageAndImportVCard() {
        String externalPath = VcardUtils.getExternalPath(this.mSourcePath);
        Log.d("VCardImport", "[doScanExternalStorageAndImportVCard]path : " + Log.anonymize(externalPath));
        File directory = VcardUtils.getDirectory(externalPath, Environment.getExternalStorageDirectory().toString());
        this.mVolumeName = VcardUtils.getVolumeName(externalPath, this);
        if (!directory.exists() || !directory.isDirectory() || !directory.canRead()) {
            showDialog(R.id.dialog_sdcard_not_found);
            return;
        }
        this.mIsScanThreadStarted = false;
        this.mVCardScanThread = new VCardScanThread(directory);
        showDialog(R.id.dialog_searching_vcard);
    }

    void showFailureNotification(int i) {
        ((NotificationManager) getSystemService("notification")).notify("VCardServiceFailure", 1, NotificationImportExportListener.constructImportFailureNotification(this, getString(i)));
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                MtkToast.toast(ImportVCardActivity.this, ImportVCardActivity.this.getString(R.string.vcard_import_failed), 1);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("VCardImport", "[onDestroy]");
        if (this.mVCardCacheThread != null && this.mVCardCacheThread.isAlive()) {
            Log.w("VCardImport", "[onDestroy]should not finish Activity when work did not finished");
            this.mVCardCacheThread.cancel();
        }
    }

    private String getEncodeUriString(String str) {
        if (TextUtils.isEmpty(str)) {
            Log.e("VCardImport", "[getEncodeUriString] filePathString is wrong !");
            return null;
        }
        int iLastIndexOf = str.lastIndexOf(File.separator);
        return "file://" + str.substring(0, iLastIndexOf) + File.separator + Uri.encode(str.substring(iLastIndexOf + 1, str.length()));
    }

    private boolean isStorageUriEx(Uri uri) {
        Log.d("VCardImport", "[isStorageUriEx] uri :" + Log.anonymize(uri));
        return uri != null && (uri.toString().startsWith("file:///storage") || uri.toString().startsWith("content://media"));
    }

    private boolean isVcfSuffixFileUri(Uri uri) {
        if (uri == null || !uri.toString().endsWith(".vcf")) {
            Log.d("VCardImport", "[isVcfSuffixFileUri] rst: false");
            return false;
        }
        return true;
    }

    private String getDisplayNameByQueryMediaProvider(Uri uri) throws Throwable {
        Throwable th;
        Cursor cursorQuery;
        String lastPathSegment;
        Log.d("VCardImport", "[getDisplayNameByQueryMediaProvider] sourceUri: " + Log.anonymize(uri));
        String string = null;
        if (uri == null) {
            Log.w("VCardImport", "[getDisplayNameByQueryMediaProvider] sourceUri is null,reteurn");
            return null;
        }
        try {
            cursorQuery = getContentResolver().query(uri, new String[]{"_data"}, null, null, null);
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.getCount() > 0 && cursorQuery.moveToFirst()) {
                        if (cursorQuery.getCount() > 1) {
                            Log.w("VCardImport", "[[getDisplayNameByQueryMediaProvider]] Unexpected multiplerows: " + cursorQuery.getCount());
                        }
                        int columnIndex = cursorQuery.getColumnIndex("_data");
                        if (columnIndex >= 0) {
                            string = cursorQuery.getString(columnIndex);
                            lastPathSegment = Uri.parse(string).getLastPathSegment();
                        } else {
                            lastPathSegment = null;
                        }
                        Log.i("VCardImport", "[getDisplayNameByQueryMediaProvider] index:" + columnIndex + ", filePath:" + Log.anonymize(string) + ",displayName: " + Log.anonymize(lastPathSegment));
                        string = lastPathSegment;
                    }
                } catch (Throwable th2) {
                    th = th2;
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    throw th;
                }
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            Log.i("VCardImport", "[getDisplayNameByQueryMediaProvider] displayName:" + Log.anonymize(string));
            return string;
        } catch (Throwable th3) {
            th = th3;
            cursorQuery = null;
        }
    }
}
