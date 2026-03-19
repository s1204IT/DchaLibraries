package com.android.contacts.vcard;

import android.accounts.Account;
import android.app.Notification;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.Process;
import com.android.contactsbind.FeedbackHelper;
import com.android.vcard.VCardEntry;
import com.android.vcard.VCardEntryCommitter;
import com.android.vcard.VCardEntryConstructor;
import com.android.vcard.VCardEntryHandler;
import com.android.vcard.VCardInterpreter;
import com.android.vcard.VCardParser;
import com.android.vcard.VCardParser_V21;
import com.android.vcard.VCardParser_V30;
import com.android.vcard.exception.VCardException;
import com.android.vcard.exception.VCardNotSupportedException;
import com.android.vcard.exception.VCardVersionException;
import com.mediatek.contacts.ExtensionManager;
import com.mediatek.contacts.util.Log;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ImportProcessor extends ProcessorBase implements VCardEntryHandler {
    private volatile boolean mCanceled;
    private volatile boolean mDone;
    private final ImportRequest mImportRequest;
    private final int mJobId;
    private final VCardImportExportListener mListener;
    private final ContentResolver mResolver;
    private final VCardService mService;
    private VCardParser mVCardParser;
    private final List<Uri> mFailedUris = new ArrayList();
    private int mCurrentCount = 0;
    private int mTotalCount = 0;
    private volatile boolean mIsRunning = false;

    public ImportProcessor(VCardService vCardService, VCardImportExportListener vCardImportExportListener, ImportRequest importRequest, int i) {
        this.mService = vCardService;
        this.mResolver = this.mService.getContentResolver();
        this.mListener = vCardImportExportListener;
        this.mImportRequest = importRequest;
        this.mJobId = i;
    }

    @Override
    public void onStart() {
    }

    @Override
    public void onEnd() {
    }

    @Override
    public void onEntryCreated(VCardEntry vCardEntry) {
        Notification notificationOnImportParsed;
        ExtensionManager.getInstance();
        ExtensionManager.getRcsRichUiExtension().loadRichScrnByVcardEntry(this.mCurrentCount == 0, vCardEntry, this.mService);
        this.mCurrentCount++;
        if (this.mListener != null && (notificationOnImportParsed = this.mListener.onImportParsed(this.mImportRequest, this.mJobId, vCardEntry, this.mCurrentCount, this.mTotalCount)) != null) {
            this.mService.startForeground(this.mJobId, notificationOnImportParsed);
        }
    }

    @Override
    public final int getType() {
        return 1;
    }

    @Override
    public void run() {
        try {
            try {
                Process.setThreadPriority(19);
                runInternal();
                if (isCancelled() && this.mListener != null) {
                    Log.d("VCardImport", "ImportProcessor, run() onImportCanceled jobId is:" + this.mJobId);
                    this.mListener.onImportCanceled(this.mImportRequest, this.mJobId);
                }
                synchronized (this) {
                    this.mDone = true;
                }
            } catch (OutOfMemoryError | RuntimeException e) {
                FeedbackHelper.sendFeedback(this.mService, "VCardImport", "Vcard import failed", e);
                synchronized (this) {
                    this.mDone = true;
                }
            }
        } catch (Throwable th) {
            synchronized (this) {
                this.mDone = true;
                throw th;
            }
        }
    }

    private void runInternal() throws Throwable {
        InputStream byteArrayInputStream;
        boolean oneVCard;
        Log.i("VCardImport", String.format("vCard import (id: %d) has started.", Integer.valueOf(this.mJobId)));
        ImportRequest importRequest = this.mImportRequest;
        if (isCancelled()) {
            Log.i("VCardImport", "Canceled before actually handling parameter (" + importRequest.uri + ")");
            return;
        }
        int[] iArr = importRequest.vcardVersion == 0 ? new int[]{1, 2} : new int[]{importRequest.vcardVersion};
        Uri uri = importRequest.uri;
        Account account = importRequest.account;
        int i = importRequest.estimatedVCardType;
        String str = importRequest.estimatedCharset;
        this.mTotalCount += importRequest.entryCount;
        VCardEntryConstructor vCardEntryConstructor = new VCardEntryConstructor(i, account, str);
        VCardEntryCommitter vCardEntryCommitter = new VCardEntryCommitter(this.mResolver);
        vCardEntryConstructor.addEntryHandler(vCardEntryCommitter);
        vCardEntryConstructor.addEntryHandler(this);
        InputStream inputStream = null;
        try {
            if (uri != null) {
                Log.i("VCardImport", "start importing one vCard (Uri: " + uri + ")");
                byteArrayInputStream = this.mResolver.openInputStream(uri);
            } else if (importRequest.data != null) {
                Log.i("VCardImport", "start importing one vCard (byte[])");
                byteArrayInputStream = new ByteArrayInputStream(importRequest.data);
            } else {
                byteArrayInputStream = null;
            }
            if (byteArrayInputStream != null) {
                try {
                    oneVCard = readOneVCard(byteArrayInputStream, i, str, vCardEntryConstructor, iArr);
                } catch (IOException e) {
                    if (byteArrayInputStream != null) {
                        try {
                            byteArrayInputStream.close();
                        } catch (Exception e2) {
                        }
                    }
                    oneVCard = false;
                } catch (Throwable th) {
                    inputStream = byteArrayInputStream;
                    th = th;
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (Exception e3) {
                        }
                    }
                    throw th;
                }
            } else {
                oneVCard = false;
            }
            if (byteArrayInputStream != null) {
                try {
                    byteArrayInputStream.close();
                } catch (Exception e4) {
                }
            }
        } catch (IOException e5) {
            byteArrayInputStream = null;
        } catch (Throwable th2) {
            th = th2;
        }
        this.mService.handleFinishImportNotification(this.mJobId, oneVCard);
        if (!oneVCard) {
            Log.w("VCardImport", "Failed to read one vCard file: " + uri);
            this.mFailedUris.add(uri);
            return;
        }
        if (isCancelled()) {
            Log.i("VCardImport", "vCard import has been canceled (uri: " + uri + ")");
            return;
        }
        Log.i("VCardImport", "Successfully finished importing one vCard file: " + uri);
        ArrayList<Uri> createdUris = vCardEntryCommitter.getCreatedUris();
        if (this.mListener != null) {
            if (createdUris != null && createdUris.size() == 1) {
                this.mListener.onImportFinished(this.mImportRequest, this.mJobId, createdUris.get(0));
                return;
            }
            if (createdUris == null || createdUris.size() == 0) {
                Log.w("VCardImport", "Created Uris is null or 0 length though the creation itself is successful.");
            }
            this.mListener.onImportFinished(this.mImportRequest, this.mJobId, null);
        }
    }

    private boolean readOneVCard(InputStream inputStream, int i, String str, VCardInterpreter vCardInterpreter, int[] iArr) {
        int length = iArr.length;
        for (int i2 = 0; i2 < length; i2++) {
            int i3 = iArr[i2];
            if (i2 > 0) {
                try {
                    try {
                        try {
                            if (vCardInterpreter instanceof VCardEntryConstructor) {
                                ((VCardEntryConstructor) vCardInterpreter).clear();
                            }
                        } catch (VCardNotSupportedException | IOException e) {
                            FeedbackHelper.sendFeedback(this.mService, "VCardImport", "Failed to read vcard", e);
                            if (inputStream != null) {
                                inputStream.close();
                            }
                        }
                    } catch (SecurityException e2) {
                        try {
                            Log.e("VCardImport", e2.toString());
                            if (inputStream != null) {
                                try {
                                    inputStream.close();
                                } catch (IOException e3) {
                                }
                            }
                        } catch (Throwable th) {
                            if (inputStream != null) {
                                try {
                                    inputStream.close();
                                } catch (IOException e4) {
                                }
                            }
                            throw th;
                        }
                    }
                } catch (VCardVersionException e5) {
                    if (i2 == length - 1) {
                        Log.e("VCardImport", "Appropriate version for this vCard is not found.");
                    }
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (VCardException e6) {
                    Log.e("VCardImport", e6.toString());
                    if (inputStream != null) {
                        inputStream.close();
                    }
                }
            }
            synchronized (this) {
                try {
                    this.mVCardParser = i3 == 2 ? new VCardParser_V30(i) : new VCardParser_V21(i);
                    if (isCancelled()) {
                        Log.i("VCardImport", "ImportProcessor already recieves cancel request, so send cancel request to vCard parser too.");
                        this.mVCardParser.cancel();
                    }
                } finally {
                }
            }
            this.mVCardParser.parse(inputStream, vCardInterpreter);
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e7) {
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public synchronized boolean cancel(boolean z) {
        if (!this.mDone && !this.mCanceled) {
            this.mCanceled = true;
            if (!this.mIsRunning && this.mListener != null) {
                this.mListener.onImportCanceled(this.mImportRequest, this.mJobId);
            }
            synchronized (this) {
                if (this.mVCardParser != null) {
                    this.mVCardParser.cancel();
                }
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
}
