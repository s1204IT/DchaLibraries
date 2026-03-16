package com.android.contacts.common.vcard;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.widget.Toast;
import com.android.contacts.R;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.account.AccountWithDataSet;
import com.android.contacts.common.vcard.ImportVCardDialogFragment;
import com.android.contacts.common.vcard.VCardService;
import com.android.vcard.VCardEntryCounter;
import com.android.vcard.VCardParser;
import com.android.vcard.VCardParser_V21;
import com.android.vcard.VCardParser_V30;
import com.android.vcard.VCardSourceDetector;
import com.android.vcard.exception.VCardException;
import com.android.vcard.exception.VCardNestedException;
import com.android.vcard.exception.VCardVersionException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

public class ImportVCardActivity extends Activity implements ImportVCardDialogFragment.Listener {
    private AccountWithDataSet mAccount;
    private List<VCardFile> mAllVCardFileList;
    private ImportRequestConnection mConnection;
    private String mErrorMessage;
    VCardImportExportListener mListener;
    private ProgressDialog mProgressDialogForCachingVCard;
    private ProgressDialog mProgressDialogForScanVCard;
    private VCardCacheThread mVCardCacheThread;
    private VCardScanThread mVCardScanThread;
    private Handler mHandler = new Handler();
    private CancelListener mCancelListener = new CancelListener();

    private static class VCardFile {
        private final String mCanonicalPath;
        private final long mLastModified;
        private final String mName;

        public VCardFile(String name, String canonicalPath, long lastModified) {
            this.mName = name;
            this.mCanonicalPath = canonicalPath;
            this.mLastModified = lastModified;
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

        public DialogDisplayer(int resId) {
            this.mResId = resId;
        }

        public DialogDisplayer(String errorMessage) {
            this.mResId = R.id.dialog_error_with_message;
            ImportVCardActivity.this.mErrorMessage = errorMessage;
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
        public void onClick(DialogInterface dialog, int which) {
            ImportVCardActivity.this.finish();
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            ImportVCardActivity.this.finish();
        }
    }

    private class ImportRequestConnection implements ServiceConnection {
        private VCardService mService;

        private ImportRequestConnection() {
        }

        public void sendImportRequest(List<ImportRequest> requests) {
            Log.i("VCardImport", "Send an import request");
            this.mService.handleImportRequest(requests, ImportVCardActivity.this.mListener);
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            this.mService = ((VCardService.MyBinder) binder).getService();
            Log.i("VCardImport", String.format("Connected to VCardService. Kick a vCard cache thread (uri: %s)", Arrays.toString(ImportVCardActivity.this.mVCardCacheThread.getSourceUris())));
            ImportVCardActivity.this.mVCardCacheThread.start();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i("VCardImport", "Disconnected from VCardService");
        }
    }

    private class VCardCacheThread extends Thread implements DialogInterface.OnCancelListener {
        private boolean mCanceled;
        private final String mDisplayName;
        private final byte[] mSource = null;
        private final Uri[] mSourceUris;
        private VCardParser mVCardParser;
        private PowerManager.WakeLock mWakeLock;

        public VCardCacheThread(Uri[] sourceUris) {
            this.mSourceUris = sourceUris;
            PowerManager powerManager = (PowerManager) ImportVCardActivity.this.getSystemService("power");
            this.mWakeLock = powerManager.newWakeLock(536870918, "VCardImport");
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
            ImportRequest request;
            Log.i("VCardImport", "vCard cache thread starts running.");
            if (ImportVCardActivity.this.mConnection == null) {
                throw new NullPointerException("vCard cache thread must be launched after a service connection is established");
            }
            this.mWakeLock.acquire();
            try {
                if (this.mCanceled) {
                    Log.i("VCardImport", "vCard cache operation is canceled.");
                    return;
                }
                Context context = ImportVCardActivity.this;
                int cache_index = 0;
                ArrayList<ImportRequest> requests = new ArrayList<>();
                if (this.mSource != null) {
                    try {
                        requests.add(constructImportRequest(this.mSource, null, this.mDisplayName));
                    } catch (VCardException e) {
                        Log.e("VCardImport", "Maybe the file is in wrong format", e);
                        ImportVCardActivity.this.showFailureNotification(R.string.fail_reason_not_supported);
                        return;
                    }
                } else {
                    ContentResolver resolver = ImportVCardActivity.this.getContentResolver();
                    Uri[] arr$ = this.mSourceUris;
                    int len$ = arr$.length;
                    int i$ = 0;
                    while (true) {
                        if (i$ >= len$) {
                            break;
                        }
                        Uri sourceUri = arr$[i$];
                        while (true) {
                            String filename = "import_tmp_" + cache_index + ".vcf";
                            File file = context.getFileStreamPath(filename);
                            if (!file.exists()) {
                                break;
                            } else {
                                if (cache_index == Integer.MAX_VALUE) {
                                    throw new RuntimeException("Exceeded cache limit");
                                }
                                cache_index++;
                            }
                        }
                        requests.add(request);
                        i$++;
                    }
                }
                if (requests.isEmpty()) {
                    Log.w("VCardImport", "Empty import requests. Ignore it.");
                } else if (requests.size() > 500) {
                    for (int start = 0; start < requests.size(); start += 500) {
                        int end = start + 500 > requests.size() ? requests.size() : start + 500;
                        Log.d("VCardImport", "request piece from " + start + " to " + (end - 1));
                        List<ImportRequest> r = requests.subList(start, end);
                        ImportVCardActivity.this.mConnection.sendImportRequest(r);
                    }
                } else {
                    ImportVCardActivity.this.mConnection.sendImportRequest(requests);
                }
            } catch (IOException e2) {
                Log.e("VCardImport", "IOException during caching vCard", e2);
                ImportVCardActivity.this.runOnUiThread(ImportVCardActivity.this.new DialogDisplayer(ImportVCardActivity.this.getString(R.string.fail_reason_io_error)));
            } catch (OutOfMemoryError e3) {
                Log.e("VCardImport", "OutOfMemoryError occured during caching vCard");
                System.gc();
                ImportVCardActivity.this.runOnUiThread(ImportVCardActivity.this.new DialogDisplayer(ImportVCardActivity.this.getString(R.string.fail_reason_low_memory_during_import)));
            } finally {
                Log.i("VCardImport", "Finished caching vCard.");
                this.mWakeLock.release();
                ImportVCardActivity.this.getApplicationContext().unbindService(ImportVCardActivity.this.mConnection);
                ImportVCardActivity.this.mProgressDialogForCachingVCard.dismiss();
                ImportVCardActivity.this.mProgressDialogForCachingVCard = null;
                ImportVCardActivity.this.finish();
            }
        }

        private Uri copyTo(Uri sourceUri, String filename) throws IOException {
            Log.i("VCardImport", String.format("Copy a Uri to app local storage (%s -> %s)", sourceUri, filename));
            Context context = ImportVCardActivity.this;
            ContentResolver resolver = context.getContentResolver();
            ReadableByteChannel inputChannel = null;
            WritableByteChannel outputChannel = null;
            try {
                inputChannel = Channels.newChannel(resolver.openInputStream(sourceUri));
                Uri destUri = Uri.parse(context.getFileStreamPath(filename).toURI().toString());
                outputChannel = context.openFileOutput(filename, 0).getChannel();
                ByteBuffer buffer = ByteBuffer.allocateDirect(8192);
                while (true) {
                    if (inputChannel.read(buffer) != -1) {
                        if (this.mCanceled) {
                            Log.d("VCardImport", "Canceled during caching " + sourceUri);
                            destUri = null;
                        } else {
                            buffer.flip();
                            outputChannel.write(buffer);
                            buffer.compact();
                        }
                    } else {
                        buffer.flip();
                        while (buffer.hasRemaining()) {
                            outputChannel.write(buffer);
                        }
                        if (inputChannel != null) {
                            try {
                                inputChannel.close();
                            } catch (IOException e) {
                                Log.w("VCardImport", "Failed to close inputChannel.");
                            }
                        }
                        if (outputChannel != null) {
                            try {
                                outputChannel.close();
                            } catch (IOException e2) {
                                Log.w("VCardImport", "Failed to close outputChannel");
                            }
                        }
                    }
                }
                return destUri;
            } finally {
                if (inputChannel != null) {
                    try {
                        inputChannel.close();
                    } catch (IOException e3) {
                        Log.w("VCardImport", "Failed to close inputChannel.");
                    }
                }
                if (outputChannel != null) {
                    try {
                        outputChannel.close();
                    } catch (IOException e4) {
                        Log.w("VCardImport", "Failed to close outputChannel");
                    }
                }
            }
        }

        private ImportRequest constructImportRequest(byte[] data, Uri localDataUri, String displayName) throws Throwable {
            InputStream is;
            InputStream is2;
            VCardSourceDetector detector;
            VCardEntryCounter counter;
            ContentResolver resolver = ImportVCardActivity.this.getContentResolver();
            VCardEntryCounter counter2 = null;
            VCardSourceDetector detector2 = null;
            int vcardVersion = 1;
            boolean shouldUseV30 = false;
            try {
                if (data != null) {
                    InputStream is3 = new ByteArrayInputStream(data);
                    is = is3;
                } else {
                    InputStream is4 = resolver.openInputStream(localDataUri);
                    is = is4;
                }
                this.mVCardParser = new VCardParser_V21();
                try {
                    counter = new VCardEntryCounter();
                    try {
                        detector = new VCardSourceDetector();
                        try {
                            try {
                                this.mVCardParser.addInterpreter(counter);
                                this.mVCardParser.addInterpreter(detector);
                                this.mVCardParser.parse(is);
                                if (is != null) {
                                    try {
                                        is.close();
                                        detector2 = detector;
                                        counter2 = counter;
                                    } catch (VCardNestedException e) {
                                        detector2 = detector;
                                        counter2 = counter;
                                        Log.w("VCardImport", "Nested Exception is found (it may be false-positive).");
                                        return new ImportRequest(ImportVCardActivity.this.mAccount, data, localDataUri, displayName, detector2.getEstimatedType(), detector2.getEstimatedCharset(), vcardVersion, counter2.getCount());
                                    } catch (IOException e2) {
                                        detector2 = detector;
                                        counter2 = counter;
                                    }
                                } else {
                                    detector2 = detector;
                                    counter2 = counter;
                                }
                            } catch (VCardVersionException e3) {
                                try {
                                    is.close();
                                } catch (IOException e4) {
                                }
                                shouldUseV30 = true;
                                is2 = data != null ? new ByteArrayInputStream(data) : resolver.openInputStream(localDataUri);
                                try {
                                    this.mVCardParser = new VCardParser_V30();
                                } catch (Throwable th) {
                                    th = th;
                                }
                                try {
                                    counter2 = new VCardEntryCounter();
                                    try {
                                        detector2 = new VCardSourceDetector();
                                        try {
                                            try {
                                                this.mVCardParser.addInterpreter(counter2);
                                                this.mVCardParser.addInterpreter(detector2);
                                                this.mVCardParser.parse(is2);
                                                if (is2 != null) {
                                                    try {
                                                        is2.close();
                                                    } catch (IOException e5) {
                                                    }
                                                }
                                            } catch (VCardVersionException e6) {
                                                throw new VCardException("vCard with unspported version.");
                                            }
                                        } catch (Throwable th2) {
                                            th = th2;
                                            if (is2 != null) {
                                                try {
                                                    is2.close();
                                                } catch (IOException e7) {
                                                }
                                            }
                                            throw th;
                                        }
                                    } catch (VCardVersionException e8) {
                                    } catch (Throwable th3) {
                                        th = th3;
                                        if (is2 != null) {
                                        }
                                        throw th;
                                    }
                                } catch (VCardVersionException e9) {
                                }
                            }
                        } catch (Throwable th4) {
                            th = th4;
                            is2 = is;
                            if (is2 != null) {
                            }
                            throw th;
                        }
                    } catch (VCardVersionException e10) {
                        detector = null;
                    } catch (Throwable th5) {
                        th = th5;
                        is2 = is;
                    }
                } catch (VCardVersionException e11) {
                    detector = null;
                    counter = null;
                } catch (Throwable th6) {
                    th = th6;
                    is2 = is;
                }
                vcardVersion = shouldUseV30 ? 2 : 1;
            } catch (VCardNestedException e12) {
            }
            return new ImportRequest(ImportVCardActivity.this.mAccount, data, localDataUri, displayName, detector2.getEstimatedType(), detector2.getEstimatedCharset(), vcardVersion, counter2.getCount());
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
        public void onCancel(DialogInterface dialog) {
            Log.i("VCardImport", "Cancel request has come. Abort caching vCard.");
            cancel();
        }
    }

    private class ImportTypeSelectedListener implements DialogInterface.OnClickListener {
        private int mCurrentIndex;

        private ImportTypeSelectedListener() {
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == -1) {
                switch (this.mCurrentIndex) {
                    case 1:
                        ImportVCardActivity.this.showDialog(R.id.dialog_select_multiple_vcard);
                        break;
                    case 2:
                        ImportVCardActivity.this.importVCardFromSDCard((List<VCardFile>) ImportVCardActivity.this.mAllVCardFileList);
                        break;
                    default:
                        ImportVCardActivity.this.showDialog(R.id.dialog_select_one_vcard);
                        break;
                }
                return;
            }
            if (which == -2) {
                ImportVCardActivity.this.finish();
            } else {
                this.mCurrentIndex = which;
            }
        }
    }

    private class VCardSelectedListener implements DialogInterface.OnClickListener, DialogInterface.OnMultiChoiceClickListener {
        private int mCurrentIndex = 0;
        private Set<Integer> mSelectedIndexSet;

        public VCardSelectedListener(boolean multipleSelect) {
            if (multipleSelect) {
                this.mSelectedIndexSet = new HashSet();
            }
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == -1) {
                if (this.mSelectedIndexSet == null) {
                    ImportVCardActivity.this.importVCardFromSDCard((VCardFile) ImportVCardActivity.this.mAllVCardFileList.get(this.mCurrentIndex));
                    return;
                }
                ArrayList arrayList = new ArrayList();
                int size = ImportVCardActivity.this.mAllVCardFileList.size();
                for (int i = 0; i < size; i++) {
                    if (this.mSelectedIndexSet.contains(Integer.valueOf(i))) {
                        arrayList.add(ImportVCardActivity.this.mAllVCardFileList.get(i));
                    }
                }
                ImportVCardActivity.this.importVCardFromSDCard(arrayList);
                return;
            }
            if (which == -2) {
                ImportVCardActivity.this.finish();
                return;
            }
            this.mCurrentIndex = which;
            if (this.mSelectedIndexSet != null) {
                if (this.mSelectedIndexSet.contains(Integer.valueOf(which))) {
                    this.mSelectedIndexSet.remove(Integer.valueOf(which));
                } else {
                    this.mSelectedIndexSet.add(Integer.valueOf(which));
                }
            }
        }

        @Override
        public void onClick(DialogInterface dialog, int which, boolean isChecked) {
            if (this.mSelectedIndexSet == null || this.mSelectedIndexSet.contains(Integer.valueOf(which)) == isChecked) {
                Log.e("VCardImport", String.format("Inconsist state in index %d (%s)", Integer.valueOf(which), ((VCardFile) ImportVCardActivity.this.mAllVCardFileList.get(which)).getCanonicalPath()));
            } else {
                onClick(dialog, which);
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

        public VCardScanThread(File sdcardDirectory) {
            this.mRootDirectory = sdcardDirectory;
            PowerManager powerManager = (PowerManager) ImportVCardActivity.this.getSystemService("power");
            this.mWakeLock = powerManager.newWakeLock(536870918, "VCardImport");
        }

        @Override
        public void run() {
            ImportVCardActivity.this.mAllVCardFileList = new Vector();
            try {
                this.mWakeLock.acquire();
                getVCardFileRecursively(this.mRootDirectory);
                try {
                    Thread.sleep(100L);
                } catch (Exception e) {
                }
                String path = System.getenv("SECONDARY_STORAGE");
                if (path != null) {
                    getVCardFileRecursively(new File(path));
                }
            } catch (CanceledException e2) {
                this.mCanceled = true;
            } catch (IOException e3) {
                this.mGotIOException = true;
            } finally {
                this.mWakeLock.release();
            }
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
        }

        private void getVCardFileRecursively(File directory) throws IOException, CanceledException {
            if (this.mCanceled) {
                throw new CanceledException();
            }
            File[] files = directory.listFiles();
            if (files == null) {
                String currentDirectoryPath = directory.getCanonicalPath();
                String secureDirectoryPath = this.mRootDirectory.getCanonicalPath().concat(".android_secure");
                if (!TextUtils.equals(currentDirectoryPath, secureDirectoryPath)) {
                    Log.w("VCardImport", "listFiles() returned null (directory: " + directory + ")");
                    return;
                }
                return;
            }
            File[] arr$ = directory.listFiles();
            for (File file : arr$) {
                if (this.mCanceled) {
                    throw new CanceledException();
                }
                String canonicalPath = file.getCanonicalPath();
                if (!this.mCheckedPaths.contains(canonicalPath)) {
                    this.mCheckedPaths.add(canonicalPath);
                    if (file.isDirectory()) {
                        getVCardFileRecursively(file);
                    } else if (canonicalPath.toLowerCase().endsWith(".vcf") && file.canRead()) {
                        String fileName = file.getName();
                        VCardFile vcardFile = new VCardFile(fileName, canonicalPath, file.lastModified());
                        ImportVCardActivity.this.mAllVCardFileList.add(vcardFile);
                    }
                }
            }
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            this.mCanceled = true;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == -2) {
                this.mCanceled = true;
            }
        }
    }

    private void startVCardSelectAndImport() {
        int size = this.mAllVCardFileList.size();
        if (getResources().getBoolean(R.bool.config_import_all_vcard_from_sdcard_automatically) || size == 1) {
            importVCardFromSDCard(this.mAllVCardFileList);
        } else if (getResources().getBoolean(R.bool.config_allow_users_select_all_vcard_import)) {
            runOnUiThread(new DialogDisplayer(R.id.dialog_select_import_type));
        } else {
            runOnUiThread(new DialogDisplayer(R.id.dialog_select_one_vcard));
        }
    }

    private void importVCardFromSDCard(List<VCardFile> selectedVCardFileList) {
        int size = selectedVCardFileList.size();
        String[] uriStrings = new String[size];
        int i = 0;
        for (VCardFile vcardFile : selectedVCardFileList) {
            uriStrings[i] = "file://" + vcardFile.getCanonicalPath();
            i++;
        }
        importVCard(uriStrings);
    }

    private void importVCardFromSDCard(VCardFile vcardFile) {
        importVCard(new Uri[]{Uri.parse("file://" + vcardFile.getCanonicalPath())});
    }

    private void importVCard(Uri uri) {
        importVCard(new Uri[]{uri});
    }

    private void importVCard(String[] uriStrings) {
        int length = uriStrings.length;
        Uri[] uris = new Uri[length];
        for (int i = 0; i < length; i++) {
            uris[i] = Uri.parse(uriStrings[i]);
        }
        importVCard(uris);
    }

    private void importVCard(final Uri[] uris) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!ImportVCardActivity.this.isFinishing()) {
                    ImportVCardActivity.this.mVCardCacheThread = ImportVCardActivity.this.new VCardCacheThread(uris);
                    ImportVCardActivity.this.mListener = new NotificationImportExportListener(ImportVCardActivity.this);
                    ImportVCardActivity.this.showDialog(R.id.dialog_cache_vcard);
                }
            }
        });
    }

    private Dialog getSelectImportTypeDialog() {
        DialogInterface.OnClickListener listener = new ImportTypeSelectedListener();
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(R.string.select_vcard_title).setPositiveButton(android.R.string.ok, listener).setOnCancelListener(this.mCancelListener).setNegativeButton(android.R.string.cancel, this.mCancelListener);
        String[] items = {getString(R.string.import_one_vcard_string), getString(R.string.import_multiple_vcard_string), getString(R.string.import_all_vcard_string)};
        builder.setSingleChoiceItems(items, 0, listener);
        return builder.create();
    }

    private Dialog getVCardFileSelectDialog(boolean multipleSelect) {
        int size = this.mAllVCardFileList.size();
        VCardSelectedListener listener = new VCardSelectedListener(multipleSelect);
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(R.string.select_vcard_title).setPositiveButton(android.R.string.ok, listener).setOnCancelListener(this.mCancelListener).setNegativeButton(android.R.string.cancel, this.mCancelListener);
        CharSequence[] items = new CharSequence[size];
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (int i = 0; i < size; i++) {
            VCardFile vcardFile = this.mAllVCardFileList.get(i);
            SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
            stringBuilder.append((CharSequence) vcardFile.getName());
            stringBuilder.append('\n');
            int indexToBeSpanned = stringBuilder.length();
            stringBuilder.append((CharSequence) ("(" + dateFormat.format(new Date(vcardFile.getLastModified())) + ")"));
            stringBuilder.setSpan(new RelativeSizeSpan(0.7f), indexToBeSpanned, stringBuilder.length(), 33);
            items[i] = stringBuilder;
        }
        if (multipleSelect) {
            builder.setMultiChoiceItems(items, (boolean[]) null, listener);
        } else {
            builder.setSingleChoiceItems(items, 0, listener);
        }
        return builder.create();
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        String accountName = null;
        String accountType = null;
        String dataSet = null;
        Intent intent = getIntent();
        if (intent != null) {
            accountName = intent.getStringExtra("account_name");
            accountType = intent.getStringExtra("account_type");
            dataSet = intent.getStringExtra("data_set");
        } else {
            Log.e("VCardImport", "intent does not exist");
        }
        if (!TextUtils.isEmpty(accountName) && !TextUtils.isEmpty(accountType)) {
            this.mAccount = new AccountWithDataSet(accountName, accountType, dataSet);
        } else {
            AccountTypeManager accountTypes = AccountTypeManager.getInstance(this);
            List<AccountWithDataSet> accountList = accountTypes.getAccounts(true);
            if (accountList.size() == 0) {
                this.mAccount = null;
            } else if (accountList.size() == 1) {
                this.mAccount = accountList.get(0);
            } else {
                startActivityForResult(new Intent(this, (Class<?>) SelectAccountActivity.class), 0);
                return;
            }
        }
        if (isCallerSelf(this)) {
            startImport();
        } else {
            ImportVCardDialogFragment.show(this);
        }
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
    public void onImportVCardConfirmed() {
        startImport();
    }

    @Override
    public void onImportVCardDenied() {
        finish();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == 0) {
            if (resultCode == -1) {
                this.mAccount = new AccountWithDataSet(intent.getStringExtra("account_name"), intent.getStringExtra("account_type"), intent.getStringExtra("data_set"));
                startImport();
            } else {
                if (resultCode != 0) {
                    Log.w("VCardImport", "Result code was not OK nor CANCELED: " + resultCode);
                }
                finish();
            }
        }
    }

    private void startImport() {
        Intent intent = getIntent();
        Uri uri = intent.getData();
        if (uri != null) {
            Log.i("VCardImport", "Starting vCard import using Uri " + uri);
            importVCard(uri);
        } else {
            Log.i("VCardImport", "Start vCard without Uri. The user will select vCard manually.");
            doScanExternalStorageAndImportVCard();
        }
    }

    @Override
    protected Dialog onCreateDialog(int resId, Bundle bundle) {
        switch (resId) {
            case R.id.dialog_searching_vcard:
                if (this.mProgressDialogForScanVCard == null) {
                    this.mProgressDialogForScanVCard = ProgressDialog.show(this, "", getString(R.string.searching_vcard_message), true, false);
                    this.mProgressDialogForScanVCard.setOnCancelListener(this.mVCardScanThread);
                    this.mVCardScanThread.start();
                }
                return this.mProgressDialogForScanVCard;
            case R.id.dialog_sdcard_not_found:
                AlertDialog.Builder builder = new AlertDialog.Builder(this).setIconAttribute(android.R.attr.alertDialogIcon).setMessage(R.string.no_sdcard_message).setOnCancelListener(this.mCancelListener).setPositiveButton(android.R.string.ok, this.mCancelListener);
                return builder.create();
            case R.id.dialog_vcard_not_found:
                AlertDialog.Builder builder2 = new AlertDialog.Builder(this).setMessage(getString(R.string.import_failure_no_vcard_file)).setOnCancelListener(this.mCancelListener).setPositiveButton(android.R.string.ok, this.mCancelListener);
                return builder2.create();
            case R.id.dialog_select_import_type:
                return getSelectImportTypeDialog();
            case R.id.dialog_select_one_vcard:
                return getVCardFileSelectDialog(false);
            case R.id.dialog_select_multiple_vcard:
                return getVCardFileSelectDialog(true);
            case R.id.dialog_cache_vcard:
                if (this.mProgressDialogForCachingVCard == null) {
                    String title = getString(R.string.caching_vcard_title);
                    String message = getString(R.string.caching_vcard_message);
                    this.mProgressDialogForCachingVCard = new ProgressDialog(this);
                    this.mProgressDialogForCachingVCard.setTitle(title);
                    this.mProgressDialogForCachingVCard.setMessage(message);
                    this.mProgressDialogForCachingVCard.setProgressStyle(0);
                    this.mProgressDialogForCachingVCard.setOnCancelListener(this.mVCardCacheThread);
                    startVCardService();
                }
                return this.mProgressDialogForCachingVCard;
            case R.id.dialog_io_exception:
                AlertDialog.Builder builder3 = new AlertDialog.Builder(this).setIconAttribute(android.R.attr.alertDialogIcon).setMessage(getString(R.string.scanning_sdcard_failed_message, new Object[]{getString(R.string.fail_reason_io_error)})).setOnCancelListener(this.mCancelListener).setPositiveButton(android.R.string.ok, this.mCancelListener);
                return builder3.create();
            case R.id.dialog_error_with_message:
                String message2 = this.mErrorMessage;
                if (TextUtils.isEmpty(message2)) {
                    Log.e("VCardImport", "Error message is null while it must not.");
                    message2 = getString(R.string.fail_reason_unknown);
                }
                AlertDialog.Builder builder4 = new AlertDialog.Builder(this).setTitle(getString(R.string.reading_vcard_failed_title)).setIconAttribute(android.R.attr.alertDialogIcon).setMessage(message2).setOnCancelListener(this.mCancelListener).setPositiveButton(android.R.string.ok, this.mCancelListener);
                return builder4.create();
            default:
                return super.onCreateDialog(resId, bundle);
        }
    }

    void startVCardService() {
        this.mConnection = new ImportRequestConnection();
        Log.i("VCardImport", "Bind to VCardService.");
        Intent intent = new Intent(this, (Class<?>) VCardService.class);
        startService(intent);
        getApplicationContext().bindService(new Intent(this, (Class<?>) VCardService.class), this.mConnection, 1);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (this.mProgressDialogForCachingVCard != null) {
            Log.i("VCardImport", "Cache thread is still running. Show progress dialog again.");
            showDialog(R.id.dialog_cache_vcard);
        }
    }

    private void doScanExternalStorageAndImportVCard() {
        File file = Environment.getExternalStorageDirectory();
        if (!file.exists() || !file.isDirectory() || !file.canRead()) {
            showDialog(R.id.dialog_sdcard_not_found);
        } else {
            this.mVCardScanThread = new VCardScanThread(file);
            showDialog(R.id.dialog_searching_vcard);
        }
    }

    void showFailureNotification(int reasonId) {
        NotificationManager notificationManager = (NotificationManager) getSystemService("notification");
        Notification notification = NotificationImportExportListener.constructImportFailureNotification(this, getString(reasonId));
        notificationManager.notify("VCardServiceFailure", 1, notification);
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ImportVCardActivity.this, ImportVCardActivity.this.getString(R.string.vcard_import_failed), 1).show();
            }
        });
    }
}
