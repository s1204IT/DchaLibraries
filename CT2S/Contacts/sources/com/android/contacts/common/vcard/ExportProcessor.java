package com.android.contacts.common.vcard;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import com.android.contacts.R;
import com.android.vcard.VCardComposer;
import com.android.vcard.VCardConfig;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class ExportProcessor extends ProcessorBase {
    private final String mCallingActivity;
    private volatile boolean mCanceled;
    private volatile boolean mDone;
    private final ExportRequest mExportRequest;
    private final int mJobId;
    private final NotificationManager mNotificationManager;
    private final ContentResolver mResolver;
    private final VCardService mService;

    public ExportProcessor(VCardService service, ExportRequest exportRequest, int jobId, String callingActivity) {
        this.mService = service;
        this.mResolver = service.getContentResolver();
        this.mNotificationManager = (NotificationManager) this.mService.getSystemService("notification");
        this.mExportRequest = exportRequest;
        this.mJobId = jobId;
        this.mCallingActivity = callingActivity;
    }

    @Override
    public final int getType() {
        return 2;
    }

    @Override
    public void run() {
        try {
            try {
                runInternal();
                if (isCancelled()) {
                    doCancelNotification();
                }
                synchronized (this) {
                    this.mDone = true;
                }
            } catch (OutOfMemoryError e) {
                Log.e("VCardExport", "OutOfMemoryError thrown during import", e);
                throw e;
            } catch (RuntimeException e2) {
                Log.e("VCardExport", "RuntimeException thrown during export", e2);
                throw e2;
            }
        } catch (Throwable th) {
            synchronized (this) {
                this.mDone = true;
                throw th;
            }
        }
    }

    private void runInternal() throws Throwable {
        VCardComposer composer;
        ExportRequest request = this.mExportRequest;
        VCardComposer composer2 = null;
        Writer writer = null;
        try {
            if (isCancelled()) {
                Log.i("VCardExport", "Export request is cancelled before handling the request");
                if (0 != 0) {
                    composer2.terminate();
                }
                if (0 != 0) {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        Log.w("VCardExport", "IOException is thrown during close(). Ignored. " + e);
                    }
                }
                this.mService.handleFinishExportNotification(this.mJobId, false);
                return;
            }
            Uri uri = request.destUri;
            try {
                OutputStream outputStream = this.mResolver.openOutputStream(uri);
                String exportType = request.exportType;
                int vcardType = TextUtils.isEmpty(exportType) ? VCardConfig.getVCardTypeFromString(this.mService.getString(R.string.config_export_vcard_type)) : VCardConfig.getVCardTypeFromString(exportType);
                composer = new VCardComposer(this.mService, vcardType, true);
                try {
                    Writer writer2 = new BufferedWriter(new OutputStreamWriter(outputStream));
                    try {
                        Uri contentUriForRawContactsEntity = ContactsContract.RawContactsEntity.CONTENT_URI;
                        if (!composer.init(ContactsContract.Contacts.CONTENT_URI, new String[]{"_id"}, null, null, null, contentUriForRawContactsEntity)) {
                            String errorReason = composer.getErrorReason();
                            Log.e("VCardExport", "initialization of vCard composer failed: " + errorReason);
                            String translatedErrorReason = translateComposerError(errorReason);
                            String title = this.mService.getString(R.string.fail_reason_could_not_initialize_exporter, new Object[]{translatedErrorReason});
                            doFinishNotification(title, null);
                            if (composer != null) {
                                composer.terminate();
                            }
                            if (writer2 != null) {
                                try {
                                    writer2.close();
                                } catch (IOException e2) {
                                    Log.w("VCardExport", "IOException is thrown during close(). Ignored. " + e2);
                                }
                            }
                            this.mService.handleFinishExportNotification(this.mJobId, false);
                            return;
                        }
                        int total = composer.getCount();
                        if (total == 0) {
                            String title2 = this.mService.getString(R.string.fail_reason_no_exportable_contact);
                            doFinishNotification(title2, null);
                            if (composer != null) {
                                composer.terminate();
                            }
                            if (writer2 != null) {
                                try {
                                    writer2.close();
                                } catch (IOException e3) {
                                    Log.w("VCardExport", "IOException is thrown during close(). Ignored. " + e3);
                                }
                            }
                            this.mService.handleFinishExportNotification(this.mJobId, false);
                            return;
                        }
                        int current = 1;
                        while (!composer.isAfterLast()) {
                            if (isCancelled()) {
                                Log.i("VCardExport", "Export request is cancelled during composing vCard");
                                if (composer != null) {
                                    composer.terminate();
                                }
                                if (writer2 != null) {
                                    try {
                                        writer2.close();
                                    } catch (IOException e4) {
                                        Log.w("VCardExport", "IOException is thrown during close(). Ignored. " + e4);
                                    }
                                }
                                this.mService.handleFinishExportNotification(this.mJobId, false);
                                return;
                            }
                            try {
                                writer2.write(composer.createOneEntry());
                                if (current % 100 == 1) {
                                    doProgressNotification(uri, total, current);
                                }
                                current++;
                            } catch (IOException e5) {
                                String errorReason2 = composer.getErrorReason();
                                Log.e("VCardExport", "Failed to read a contact: " + errorReason2);
                                String translatedErrorReason2 = translateComposerError(errorReason2);
                                String title3 = this.mService.getString(R.string.fail_reason_error_occurred_during_export, new Object[]{translatedErrorReason2});
                                doFinishNotification(title3, null);
                                if (composer != null) {
                                    composer.terminate();
                                }
                                if (writer2 != null) {
                                    try {
                                        writer2.close();
                                    } catch (IOException e6) {
                                        Log.w("VCardExport", "IOException is thrown during close(). Ignored. " + e6);
                                    }
                                }
                                this.mService.handleFinishExportNotification(this.mJobId, false);
                                return;
                            }
                        }
                        Log.i("VCardExport", "Successfully finished exporting vCard " + request.destUri);
                        this.mService.updateMediaScanner(request.destUri.getPath());
                        String filename = uri.getLastPathSegment();
                        String title4 = this.mService.getString(R.string.exporting_vcard_finished_title, new Object[]{filename});
                        doFinishNotification(title4, null);
                        if (composer != null) {
                            composer.terminate();
                        }
                        if (writer2 != null) {
                            try {
                                writer2.close();
                            } catch (IOException e7) {
                                Log.w("VCardExport", "IOException is thrown during close(). Ignored. " + e7);
                            }
                        }
                        this.mService.handleFinishExportNotification(this.mJobId, true);
                        return;
                    } catch (Throwable th) {
                        th = th;
                        writer = writer2;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            } catch (FileNotFoundException e8) {
                Log.w("VCardExport", "FileNotFoundException thrown", e8);
                doFinishNotification(this.mService.getString(R.string.fail_reason_could_not_open_file, new Object[]{uri, e8.getMessage()}), null);
                if (0 != 0) {
                    composer2.terminate();
                }
                if (0 != 0) {
                    try {
                        writer.close();
                    } catch (IOException e9) {
                        Log.w("VCardExport", "IOException is thrown during close(). Ignored. " + e9);
                    }
                }
                this.mService.handleFinishExportNotification(this.mJobId, false);
                return;
            }
        } catch (Throwable th3) {
            th = th3;
            composer = null;
        }
        if (composer != null) {
            composer.terminate();
        }
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e10) {
                Log.w("VCardExport", "IOException is thrown during close(). Ignored. " + e10);
            }
        }
        this.mService.handleFinishExportNotification(this.mJobId, false);
        throw th;
    }

    private String translateComposerError(String errorMessage) {
        Resources resources = this.mService.getResources();
        if ("Failed to get database information".equals(errorMessage)) {
            return resources.getString(R.string.composer_failed_to_get_database_infomation);
        }
        if ("There's no exportable in the database".equals(errorMessage)) {
            return resources.getString(R.string.composer_has_no_exportable_contact);
        }
        if ("The vCard composer object is not correctly initialized".equals(errorMessage)) {
            return resources.getString(R.string.composer_not_initialized);
        }
        return errorMessage;
    }

    private void doProgressNotification(Uri uri, int totalCount, int currentCount) {
        String displayName = uri.getLastPathSegment();
        String description = this.mService.getString(R.string.exporting_contact_list_message, new Object[]{displayName});
        String tickerText = this.mService.getString(R.string.exporting_contact_list_title);
        Notification notification = NotificationImportExportListener.constructProgressNotification(this.mService, 2, description, tickerText, this.mJobId, displayName, totalCount, currentCount);
        this.mNotificationManager.notify("VCardServiceProgress", this.mJobId, notification);
    }

    private void doCancelNotification() {
        String description = this.mService.getString(R.string.exporting_vcard_canceled_title, new Object[]{this.mExportRequest.destUri.getLastPathSegment()});
        Notification notification = NotificationImportExportListener.constructCancelNotification(this.mService, description);
        this.mNotificationManager.notify("VCardServiceProgress", this.mJobId, notification);
    }

    private void doFinishNotification(String title, String description) {
        Intent intent = new Intent();
        intent.setClassName(this.mService, this.mCallingActivity);
        Notification notification = NotificationImportExportListener.constructFinishNotification(this.mService, title, description, intent);
        this.mNotificationManager.notify("VCardServiceProgress", this.mJobId, notification);
    }

    @Override
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        boolean z = true;
        synchronized (this) {
            if (this.mDone || this.mCanceled) {
                z = false;
            } else {
                this.mCanceled = true;
            }
        }
        return z;
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
