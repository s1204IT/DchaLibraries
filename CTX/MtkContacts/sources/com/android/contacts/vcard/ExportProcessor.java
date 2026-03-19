package com.android.contacts.vcard;

import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.widget.Toast;
import com.android.contacts.R;
import com.android.contactsbind.FeedbackHelper;
import com.android.vcard.VCardComposer;
import com.android.vcard.VCardConfig;
import com.mediatek.contacts.util.Log;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class ExportProcessor extends ProcessorBase {
    private final String mCallingActivity;
    private volatile boolean mCanceled;
    private volatile boolean mDone;
    private final ExportRequest mExportRequest;
    private final int mJobId;
    private final NotificationManager mNotificationManager;
    private final ContentResolver mResolver;
    private final VCardService mService;
    private PowerManager.WakeLock mWakeLock;
    private volatile boolean mIsRunning = false;
    private final int SHOW_READY_TOAST = 1;
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            if (message.arg1 == 1) {
                Toast.makeText(ExportProcessor.this.mService, R.string.exporting_vcard_finished_toast, 1).show();
            }
        }
    };

    public ExportProcessor(VCardService vCardService, ExportRequest exportRequest, int i, String str) {
        Log.i("VCardExport", "[ExportProcessor]new,jobId = " + i + ",callingActivity = " + str);
        this.mService = vCardService;
        this.mResolver = vCardService.getContentResolver();
        this.mNotificationManager = (NotificationManager) this.mService.getSystemService("notification");
        this.mExportRequest = exportRequest;
        this.mJobId = i;
        this.mCallingActivity = str;
        this.mWakeLock = ((PowerManager) this.mService.getApplicationContext().getSystemService("power")).newWakeLock(536870918, "VCardExport");
    }

    @Override
    public final int getType() {
        return 2;
    }

    @Override
    public void run() {
        this.mIsRunning = true;
        this.mWakeLock.acquire();
        try {
            try {
                runInternal();
                if (isCancelled()) {
                    doCancelNotification();
                }
                synchronized (this) {
                    this.mDone = true;
                }
                if (this.mWakeLock == null || !this.mWakeLock.isHeld()) {
                    return;
                }
                this.mWakeLock.release();
            } catch (OutOfMemoryError | RuntimeException e) {
                FeedbackHelper.sendFeedback(this.mService, "VCardExport", "Failed to process vcard export", e);
                throw e;
            }
        } catch (Throwable th) {
            synchronized (this) {
                this.mDone = true;
                if (this.mWakeLock != null && this.mWakeLock.isHeld()) {
                    this.mWakeLock.release();
                }
                throw th;
            }
        }
    }

    private void runInternal() throws Throwable {
        VCardComposer vCardComposer;
        BufferedWriter bufferedWriter;
        String string;
        ExportRequest exportRequest = this.mExportRequest;
        boolean z = true;
        try {
            if (isCancelled()) {
                Log.i("VCardExport", "Export request is cancelled before handling the request");
                this.mService.handleFinishExportNotification(this.mJobId, false);
                return;
            }
            Uri uri = exportRequest.destUri;
            try {
                OutputStream outputStreamOpenOutputStream = this.mResolver.openOutputStream(uri);
                String str = exportRequest.exportType;
                VCardComposer vCardComposer2 = new VCardComposer(this.mService, TextUtils.isEmpty(str) ? VCardConfig.getVCardTypeFromString(this.mService.getString(R.string.config_export_vcard_type)) : VCardConfig.getVCardTypeFromString(str), true);
                try {
                    bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStreamOpenOutputStream));
                    try {
                        vCardComposer = vCardComposer2;
                        try {
                            if (!vCardComposer2.init(ContactsContract.Contacts.CONTENT_URI, new String[]{"_id"}, null, null, null, ContactsContract.RawContactsEntity.CONTENT_URI)) {
                                String errorReason = vCardComposer.getErrorReason();
                                Log.e("VCardExport", "initialization of vCard composer failed: " + errorReason);
                                doFinishNotification(this.mService.getString(R.string.fail_reason_could_not_initialize_exporter, new Object[]{translateComposerError(errorReason)}), null);
                                vCardComposer.terminate();
                                try {
                                    bufferedWriter.close();
                                } catch (IOException e) {
                                    Log.w("VCardExport", "IOException is thrown during close(). Ignored. " + e);
                                }
                                this.mService.handleFinishExportNotification(this.mJobId, false);
                                return;
                            }
                            int count = vCardComposer.getCount();
                            if (count == 0) {
                                doFinishNotification(this.mService.getString(R.string.fail_reason_no_exportable_contact), null);
                                vCardComposer.terminate();
                                try {
                                    bufferedWriter.close();
                                } catch (IOException e2) {
                                    Log.w("VCardExport", "IOException is thrown during close(). Ignored. " + e2);
                                }
                                this.mService.handleFinishExportNotification(this.mJobId, false);
                                return;
                            }
                            int i = 1;
                            while (!vCardComposer.isAfterLast()) {
                                if (isCancelled()) {
                                    Log.i("VCardExport", "Export request is cancelled during composing vCard");
                                    vCardComposer.terminate();
                                    try {
                                        bufferedWriter.close();
                                    } catch (IOException e3) {
                                        Log.w("VCardExport", "IOException is thrown during close(). Ignored. " + e3);
                                    }
                                    this.mService.handleFinishExportNotification(this.mJobId, false);
                                    return;
                                }
                                try {
                                    bufferedWriter.write(vCardComposer.createOneEntry());
                                    bufferedWriter.flush();
                                    if (i % 100 == 1) {
                                        doProgressNotification(uri, count, i);
                                    }
                                    i++;
                                } catch (IOException e4) {
                                    String errorReason2 = vCardComposer.getErrorReason();
                                    Log.e("VCardExport", "Failed to read a contact: " + errorReason2);
                                    String message = e4.getMessage();
                                    Log.e("VCardExport", "exception: " + message);
                                    if (message == null || message.indexOf("ENOSPC") < 0) {
                                        string = this.mService.getString(R.string.fail_reason_error_occurred_during_export, new Object[]{translateComposerError(errorReason2)});
                                    } else {
                                        string = this.mService.getResources().getString(R.string.storage_full);
                                        int i2 = i - 1;
                                        this.mService.getString(R.string.notifier_multichoice_process_report, new Object[]{Integer.valueOf(i2), Integer.valueOf(count - i2)});
                                    }
                                    doFinishNotification(string, null);
                                    vCardComposer.terminate();
                                    try {
                                        bufferedWriter.close();
                                    } catch (IOException e5) {
                                        Log.w("VCardExport", "IOException is thrown during close(). Ignored. " + e5);
                                    }
                                    this.mService.handleFinishExportNotification(this.mJobId, false);
                                    return;
                                }
                            }
                            Log.i("VCardExport", "Successfully finished exporting vCard " + exportRequest.destUri);
                            this.mService.updateMediaScanner(exportRequest.destUri.getPath());
                            try {
                                String openableUriDisplayName = ExportVCardActivity.getOpenableUriDisplayName(this.mService, uri);
                                if (isLocalFile(uri)) {
                                    Message messageObtainMessage = this.handler.obtainMessage();
                                    messageObtainMessage.arg1 = 1;
                                    this.handler.sendMessage(messageObtainMessage);
                                    doFinishNotificationWithShareAction(this.mService.getString(R.string.exporting_vcard_finished_title_fallback), this.mService.getString(R.string.touch_to_share_contacts), uri);
                                } else {
                                    doFinishNotification(openableUriDisplayName == null ? this.mService.getString(R.string.exporting_vcard_finished_title_fallback) : this.mService.getString(R.string.exporting_vcard_finished_title, new Object[]{openableUriDisplayName}), null);
                                }
                                vCardComposer.terminate();
                                try {
                                    bufferedWriter.close();
                                } catch (IOException e6) {
                                    Log.w("VCardExport", "IOException is thrown during close(). Ignored. " + e6);
                                }
                                this.mService.handleFinishExportNotification(this.mJobId, true);
                                return;
                            } catch (Throwable th) {
                                th = th;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            z = false;
                            Throwable th3 = th;
                            if (vCardComposer != null) {
                            }
                            if (bufferedWriter != null) {
                            }
                            this.mService.handleFinishExportNotification(this.mJobId, z);
                            throw th3;
                        }
                    } catch (Throwable th4) {
                        th = th4;
                        vCardComposer = vCardComposer2;
                    }
                } catch (Throwable th5) {
                    th = th5;
                    vCardComposer = vCardComposer2;
                    z = false;
                    bufferedWriter = null;
                }
            } catch (FileNotFoundException e7) {
                Log.w("VCardExport", "FileNotFoundException thrown", e7);
                doFinishNotification(this.mService.getString(R.string.fail_reason_could_not_open_file, new Object[]{uri, e7.getMessage()}), null);
                this.mService.handleFinishExportNotification(this.mJobId, false);
                return;
            }
        } catch (Throwable th6) {
            th = th6;
            z = false;
            vCardComposer = null;
            bufferedWriter = null;
        }
        Throwable th32 = th;
        if (vCardComposer != null) {
            vCardComposer.terminate();
        }
        if (bufferedWriter != null) {
            try {
                bufferedWriter.close();
            } catch (IOException e8) {
                Log.w("VCardExport", "IOException is thrown during close(). Ignored. " + e8);
            }
        }
        this.mService.handleFinishExportNotification(this.mJobId, z);
        throw th32;
    }

    private boolean isLocalFile(Uri uri) {
        return this.mService.getString(R.string.contacts_file_provider_authority).equals(uri.getAuthority());
    }

    private String translateComposerError(String str) {
        Resources resources = this.mService.getResources();
        if ("Failed to get database information".equals(str)) {
            return resources.getString(R.string.composer_failed_to_get_database_infomation);
        }
        if ("There's no exportable in the database".equals(str)) {
            return resources.getString(R.string.composer_has_no_exportable_contact);
        }
        if ("The vCard composer object is not correctly initialized".equals(str)) {
            return resources.getString(R.string.composer_not_initialized);
        }
        return resources.getString(R.string.generic_failure);
    }

    private void doProgressNotification(Uri uri, int i, int i2) {
        String openableUriDisplayName = ExportVCardActivity.getOpenableUriDisplayName(this.mService, uri);
        this.mService.startForeground(this.mJobId, NotificationImportExportListener.constructProgressNotification(this.mService, 2, this.mService.getString(R.string.exporting_contact_list_message, new Object[]{openableUriDisplayName}), this.mService.getString(R.string.exporting_contact_list_title), this.mJobId, openableUriDisplayName, i, i2));
    }

    private void doCancelNotification() {
        this.mNotificationManager.notify("VCardServiceProgress", this.mJobId, NotificationImportExportListener.constructCancelNotification(this.mService, this.mService.getString(R.string.exporting_vcard_canceled_title, new Object[]{ExportVCardActivity.getOpenableUriDisplayName(this.mService, this.mExportRequest.destUri)})));
    }

    private void doFinishNotification(String str, String str2) {
        Intent intent = new Intent();
        intent.setClassName(this.mService, this.mCallingActivity);
        Log.d("VCardExport", "[doFinishNotification] send callingActivity intent = " + intent.toString());
        this.mNotificationManager.notify("VCardServiceProgress", this.mJobId, NotificationImportExportListener.constructFinishNotification(this.mService, str, str2, intent));
    }

    private void doFinishNotificationWithShareAction(String str, String str2, Uri uri) {
        Intent intent = new Intent("android.intent.action.SEND");
        intent.setType("text/x-vcard");
        intent.putExtra("android.intent.extra.STREAM", uri);
        intent.setFlags(268435457);
        this.mNotificationManager.notify("VCardServiceProgress", this.mJobId, NotificationImportExportListener.constructFinishNotification(this.mService, str, str2, intent));
    }

    @Override
    public synchronized boolean cancel(boolean z) {
        if (!this.mDone && !this.mCanceled) {
            this.mCanceled = true;
            if (!this.mIsRunning) {
                doCancelNotification();
            }
            return true;
        }
        return false;
    }

    @Override
    public synchronized boolean isCancelled() {
        return this.mCanceled;
    }

    @Override
    public synchronized boolean isDone() {
        return this.mDone;
    }

    public ExportRequest getRequest() {
        return this.mExportRequest;
    }
}
