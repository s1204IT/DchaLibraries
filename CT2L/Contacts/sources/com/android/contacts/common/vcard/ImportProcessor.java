package com.android.contacts.common.vcard;

import android.accounts.Account;
import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;
import com.android.vcard.VCardEntry;
import com.android.vcard.VCardEntryCommitter;
import com.android.vcard.VCardEntryConstructor;
import com.android.vcard.VCardEntryHandler;
import com.android.vcard.VCardInterpreter;
import com.android.vcard.VCardParser;
import com.android.vcard.VCardParser_V21;
import com.android.vcard.VCardParser_V30;
import com.android.vcard.exception.VCardException;
import com.android.vcard.exception.VCardNestedException;
import com.android.vcard.exception.VCardNotSupportedException;
import com.android.vcard.exception.VCardVersionException;
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

    public ImportProcessor(VCardService service, VCardImportExportListener listener, ImportRequest request, int jobId) {
        this.mService = service;
        this.mResolver = this.mService.getContentResolver();
        this.mListener = listener;
        this.mImportRequest = request;
        this.mJobId = jobId;
    }

    @Override
    public void onStart() {
    }

    @Override
    public void onEnd() {
    }

    @Override
    public void onEntryCreated(VCardEntry entry) {
        this.mCurrentCount++;
        if (this.mListener != null) {
            this.mListener.onImportParsed(this.mImportRequest, this.mJobId, entry, this.mCurrentCount, this.mTotalCount);
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
                try {
                    runInternal();
                    if (isCancelled() && this.mListener != null) {
                        this.mListener.onImportCanceled(this.mImportRequest, this.mJobId);
                    }
                    synchronized (this) {
                        this.mDone = true;
                    }
                } catch (RuntimeException e) {
                    Log.e("VCardImport", "RuntimeException thrown during import", e);
                    throw e;
                }
            } catch (OutOfMemoryError e2) {
                Log.e("VCardImport", "OutOfMemoryError thrown during import", e2);
                throw e2;
            }
        } catch (Throwable th) {
            synchronized (this) {
                this.mDone = true;
                throw th;
            }
        }
    }

    private void runInternal() {
        int[] possibleVCardVersions;
        Log.i("VCardImport", String.format("vCard import (id: %d) has started.", Integer.valueOf(this.mJobId)));
        ImportRequest request = this.mImportRequest;
        if (isCancelled()) {
            Log.i("VCardImport", "Canceled before actually handling parameter (" + request.uri + ")");
            return;
        }
        if (request.vcardVersion == 0) {
            possibleVCardVersions = new int[]{1, 2};
        } else {
            possibleVCardVersions = new int[]{request.vcardVersion};
        }
        Uri uri = request.uri;
        Account account = request.account;
        int estimatedVCardType = request.estimatedVCardType;
        String estimatedCharset = request.estimatedCharset;
        int entryCount = request.entryCount;
        this.mTotalCount += entryCount;
        VCardEntryConstructor constructor = new VCardEntryConstructor(estimatedVCardType, account, estimatedCharset);
        VCardEntryCommitter committer = new VCardEntryCommitter(this.mResolver);
        constructor.addEntryHandler(committer);
        constructor.addEntryHandler(this);
        InputStream is = null;
        boolean successful = false;
        try {
            if (uri != null) {
                Log.i("VCardImport", "start importing one vCard (Uri: " + uri + ")");
                is = this.mResolver.openInputStream(uri);
            } else if (request.data != null) {
                Log.i("VCardImport", "start importing one vCard (byte[])");
                is = new ByteArrayInputStream(request.data);
            }
            if (is != null) {
                successful = readOneVCard(is, estimatedVCardType, estimatedCharset, constructor, possibleVCardVersions);
            }
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                }
            }
        } catch (IOException e2) {
            successful = false;
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e3) {
                }
            }
        } catch (Throwable th) {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e4) {
                }
            }
            throw th;
        }
        this.mService.handleFinishImportNotification(this.mJobId, successful);
        if (successful) {
            if (isCancelled()) {
                Log.i("VCardImport", "vCard import has been canceled (uri: " + uri + ")");
                return;
            }
            Log.i("VCardImport", "Successfully finished importing one vCard file: " + uri);
            List<Uri> uris = committer.getCreatedUris();
            if (this.mListener != null) {
                if (uris != null && uris.size() == 1) {
                    this.mListener.onImportFinished(this.mImportRequest, this.mJobId, uris.get(0));
                    return;
                }
                if (uris == null || uris.size() == 0) {
                    Log.w("VCardImport", "Created Uris is null or 0 length though the creation itself is successful.");
                }
                this.mListener.onImportFinished(this.mImportRequest, this.mJobId, null);
                return;
            }
            return;
        }
        Log.w("VCardImport", "Failed to read one vCard file: " + uri);
        this.mFailedUris.add(uri);
    }

    private boolean readOneVCard(InputStream is, int vcardType, String charset, VCardInterpreter interpreter, int[] possibleVCardVersions) {
        boolean successful = false;
        int length = possibleVCardVersions.length;
        for (int i = 0; i < length; i++) {
            int vcardVersion = possibleVCardVersions[i];
            if (i > 0) {
                try {
                    try {
                        try {
                            if (interpreter instanceof VCardEntryConstructor) {
                                ((VCardEntryConstructor) interpreter).clear();
                            }
                        } catch (VCardException e) {
                            Log.e("VCardImport", e.toString());
                            if (is != null) {
                                try {
                                    is.close();
                                } catch (IOException e2) {
                                }
                            }
                        }
                    } catch (VCardNestedException e3) {
                        Log.e("VCardImport", "Nested Exception is found.");
                        if (is != null) {
                            try {
                                is.close();
                            } catch (IOException e4) {
                            }
                        }
                    } catch (IOException e5) {
                        try {
                            Log.e("VCardImport", "IOException was emitted: " + e5.getMessage());
                            if (is != null) {
                                try {
                                    is.close();
                                } catch (IOException e6) {
                                }
                            }
                        } finally {
                            if (is != null) {
                                try {
                                    is.close();
                                } catch (IOException e7) {
                                }
                            }
                        }
                    }
                } catch (VCardNotSupportedException e8) {
                    Log.e("VCardImport", e8.toString());
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException e9) {
                        }
                    }
                } catch (VCardVersionException e10) {
                    if (i == length - 1) {
                        Log.e("VCardImport", "Appropriate version for this vCard is not found.");
                    }
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException e11) {
                        }
                    }
                }
            }
            synchronized (this) {
                this.mVCardParser = vcardVersion == 2 ? new VCardParser_V30(vcardType) : new VCardParser_V21(vcardType);
                if (isCancelled()) {
                    Log.i("VCardImport", "ImportProcessor already recieves cancel request, so send cancel request to vCard parser too.");
                    this.mVCardParser.cancel();
                }
            }
            this.mVCardParser.parse(is, interpreter);
            successful = true;
            return successful;
        }
        return successful;
    }

    @Override
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        boolean z = true;
        synchronized (this) {
            if (this.mDone || this.mCanceled) {
                z = false;
            } else {
                this.mCanceled = true;
                synchronized (this) {
                    if (this.mVCardParser != null) {
                        this.mVCardParser.cancel();
                    }
                }
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
}
