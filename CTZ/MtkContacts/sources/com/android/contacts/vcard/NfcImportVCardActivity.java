package com.android.contacts.vcard;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.widget.Toast;
import com.android.contacts.R;
import com.android.contacts.activities.RequestPermissionsActivity;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.util.ImplicitIntentsUtil;
import com.android.contacts.vcard.VCardService;
import com.android.contactsbind.FeedbackHelper;
import com.android.vcard.VCardEntry;
import com.android.vcard.VCardEntryCounter;
import com.android.vcard.VCardParser_V21;
import com.android.vcard.VCardParser_V30;
import com.android.vcard.VCardSourceDetector;
import com.android.vcard.exception.VCardException;
import com.android.vcard.exception.VCardNestedException;
import com.android.vcard.exception.VCardVersionException;
import com.mediatek.contacts.util.Log;
import com.mediatek.contacts.util.VcardUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NfcImportVCardActivity extends Activity implements ServiceConnection, VCardImportExportListener {
    private AccountWithDataSet mAccount;
    private Handler mHandler = new Handler();
    private NdefRecord mRecord;

    class ImportTask extends AsyncTask<VCardService, Void, ImportRequest> {
        ImportTask() {
        }

        @Override
        public ImportRequest doInBackground(VCardService... vCardServiceArr) throws Throwable {
            ImportRequest importRequestCreateImportRequest = NfcImportVCardActivity.this.createImportRequest();
            if (importRequestCreateImportRequest == null) {
                return null;
            }
            ArrayList arrayList = new ArrayList();
            arrayList.add(importRequestCreateImportRequest);
            vCardServiceArr[0].handleImportRequest(arrayList, NfcImportVCardActivity.this);
            return importRequestCreateImportRequest;
        }

        @Override
        public void onCancelled() {
            NfcImportVCardActivity.this.unbindService(NfcImportVCardActivity.this);
        }

        @Override
        public void onPostExecute(ImportRequest importRequest) {
            if (importRequest == null) {
                NfcImportVCardActivity.this.finish();
            }
            NfcImportVCardActivity.this.unbindService(NfcImportVCardActivity.this);
        }
    }

    ImportRequest createImportRequest() throws Throwable {
        VCardEntryCounter vCardEntryCounter;
        VCardSourceDetector vCardSourceDetector;
        VCardEntryCounter vCardEntryCounter2;
        Throwable th;
        VCardSourceDetector vCardSourceDetector2;
        int i = 1;
        try {
            try {
                try {
                    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(this.mRecord.getPayload());
                    byteArrayInputStream.mark(0);
                    VCardParser_V21 vCardParser_V21 = new VCardParser_V21();
                    try {
                        vCardEntryCounter = new VCardEntryCounter();
                    } catch (VCardVersionException e) {
                        vCardEntryCounter = null;
                        vCardSourceDetector = null;
                    } catch (Throwable th2) {
                        th = th2;
                        vCardSourceDetector = null;
                        vCardEntryCounter2 = null;
                        int i2 = i;
                        th = th;
                        try {
                            try {
                                byteArrayInputStream.close();
                                throw th;
                            } catch (VCardNestedException e2) {
                                i = i2;
                                vCardEntryCounter = vCardEntryCounter2;
                                Log.w("NfcImportVCardActivity", "Nested Exception is found (it may be false-positive).");
                                return new ImportRequest(this.mAccount, this.mRecord.getPayload(), null, getString(R.string.nfc_vcard_file_name), vCardSourceDetector.getEstimatedType(), vCardSourceDetector.getEstimatedCharset(), i, vCardEntryCounter.getCount());
                            }
                        } catch (IOException e3) {
                            throw th;
                        }
                    }
                    try {
                        vCardSourceDetector = new VCardSourceDetector();
                        try {
                            try {
                                vCardParser_V21.addInterpreter(vCardEntryCounter);
                                vCardParser_V21.addInterpreter(vCardSourceDetector);
                                vCardParser_V21.parse(byteArrayInputStream);
                                try {
                                    byteArrayInputStream.close();
                                } catch (VCardNestedException e4) {
                                    Log.w("NfcImportVCardActivity", "Nested Exception is found (it may be false-positive).");
                                } catch (IOException e5) {
                                }
                            } catch (VCardVersionException e6) {
                                byteArrayInputStream.reset();
                                i = 2;
                                VCardParser_V30 vCardParser_V30 = new VCardParser_V30();
                                try {
                                    vCardEntryCounter2 = new VCardEntryCounter();
                                    try {
                                        try {
                                            vCardSourceDetector2 = new VCardSourceDetector();
                                        } catch (VCardVersionException e7) {
                                            e = e7;
                                        }
                                    } catch (Throwable th3) {
                                        th = th3;
                                    }
                                } catch (VCardVersionException e8) {
                                    e = e8;
                                    vCardEntryCounter2 = vCardEntryCounter;
                                }
                                try {
                                    vCardParser_V30.addInterpreter(vCardEntryCounter2);
                                    vCardParser_V30.addInterpreter(vCardSourceDetector2);
                                    vCardParser_V30.parse(byteArrayInputStream);
                                    try {
                                        byteArrayInputStream.close();
                                    } catch (VCardNestedException e9) {
                                        vCardSourceDetector = vCardSourceDetector2;
                                        vCardEntryCounter = vCardEntryCounter2;
                                        Log.w("NfcImportVCardActivity", "Nested Exception is found (it may be false-positive).");
                                        return new ImportRequest(this.mAccount, this.mRecord.getPayload(), null, getString(R.string.nfc_vcard_file_name), vCardSourceDetector.getEstimatedType(), vCardSourceDetector.getEstimatedCharset(), i, vCardEntryCounter.getCount());
                                    } catch (IOException e10) {
                                    }
                                    vCardSourceDetector = vCardSourceDetector2;
                                    vCardEntryCounter = vCardEntryCounter2;
                                } catch (VCardVersionException e11) {
                                    e = e11;
                                    vCardSourceDetector = vCardSourceDetector2;
                                    FeedbackHelper.sendFeedback(this, "NfcImportVCardActivity", "vcard with unsupported version", e);
                                    showFailureNotification(R.string.fail_reason_not_supported);
                                    try {
                                        byteArrayInputStream.close();
                                    } catch (VCardNestedException e12) {
                                        vCardEntryCounter = vCardEntryCounter2;
                                        Log.w("NfcImportVCardActivity", "Nested Exception is found (it may be false-positive).");
                                        return new ImportRequest(this.mAccount, this.mRecord.getPayload(), null, getString(R.string.nfc_vcard_file_name), vCardSourceDetector.getEstimatedType(), vCardSourceDetector.getEstimatedCharset(), i, vCardEntryCounter.getCount());
                                    } catch (IOException e13) {
                                    }
                                    return null;
                                } catch (Throwable th4) {
                                    th = th4;
                                    vCardSourceDetector = vCardSourceDetector2;
                                    int i22 = i;
                                    th = th;
                                    byteArrayInputStream.close();
                                    throw th;
                                }
                            }
                        } catch (Throwable th5) {
                            th = th5;
                            vCardEntryCounter2 = vCardEntryCounter;
                            int i222 = i;
                            th = th;
                            byteArrayInputStream.close();
                            throw th;
                        }
                    } catch (VCardVersionException e14) {
                        vCardSourceDetector = null;
                    } catch (Throwable th6) {
                        th = th6;
                        vCardSourceDetector = null;
                        vCardEntryCounter2 = vCardEntryCounter;
                        int i2222 = i;
                        th = th;
                        byteArrayInputStream.close();
                        throw th;
                    }
                } catch (VCardNestedException e15) {
                    vCardEntryCounter = null;
                    vCardSourceDetector = null;
                }
                return new ImportRequest(this.mAccount, this.mRecord.getPayload(), null, getString(R.string.nfc_vcard_file_name), vCardSourceDetector.getEstimatedType(), vCardSourceDetector.getEstimatedCharset(), i, vCardEntryCounter.getCount());
            } catch (IOException e16) {
                FeedbackHelper.sendFeedback(this, "NfcImportVCardActivity", "Failed to read vcard data", e16);
                showFailureNotification(R.string.fail_reason_io_error);
                return null;
            }
        } catch (VCardException e17) {
            FeedbackHelper.sendFeedback(this, "NfcImportVCardActivity", "Failed to parse vcard", e17);
            showFailureNotification(R.string.fail_reason_not_supported);
            return null;
        }
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        new ImportTask().execute(((VCardService.MyBinder) iBinder).getService());
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (RequestPermissionsActivity.startPermissionActivityIfNeeded(this)) {
            return;
        }
        Intent intent = getIntent();
        if (!"android.nfc.action.NDEF_DISCOVERED".equals(intent.getAction())) {
            Log.w("NfcImportVCardActivity", "Unknowon intent " + intent);
            finish();
            return;
        }
        String type = intent.getType();
        if (type == null || (!"text/x-vcard".equals(type) && !"text/vcard".equals(type))) {
            Log.w("NfcImportVCardActivity", "Not a vcard");
            finish();
            return;
        }
        this.mRecord = ((NdefMessage) intent.getParcelableArrayExtra("android.nfc.extra.NDEF_MESSAGES")[0]).getRecords()[0];
        List<AccountWithDataSet> listAddNonSimAccount = VcardUtils.addNonSimAccount(AccountTypeManager.getInstance(this).getAccounts(true));
        if (listAddNonSimAccount.size() != 0) {
            if (listAddNonSimAccount.size() != 1) {
                startActivityForResult(new Intent(this, (Class<?>) SelectAccountActivity.class), 1);
                return;
            }
            this.mAccount = listAddNonSimAccount.get(0);
        } else {
            this.mAccount = null;
        }
        startImport();
    }

    @Override
    public void onActivityResult(int i, int i2, Intent intent) {
        if (i == 1) {
            if (i2 == -1) {
                this.mAccount = new AccountWithDataSet(intent.getStringExtra("account_name"), intent.getStringExtra("account_type"), intent.getStringExtra("data_set"));
                startImport();
            } else {
                finish();
            }
        }
    }

    private void startImport() {
        Intent intent = new Intent(this, (Class<?>) VCardService.class);
        startService(intent);
        bindService(intent, this, 1);
    }

    @Override
    public Notification onImportProcessed(ImportRequest importRequest, int i, int i2) {
        return null;
    }

    @Override
    public Notification onImportParsed(ImportRequest importRequest, int i, VCardEntry vCardEntry, int i2, int i3) {
        return null;
    }

    @Override
    public void onImportFinished(ImportRequest importRequest, int i, Uri uri) {
        if (isFinishing()) {
            Log.i("NfcImportVCardActivity", "Late import -- ignoring");
        } else if (uri != null) {
            ImplicitIntentsUtil.startActivityInAppIfPossible(this, new Intent("android.intent.action.VIEW", ContactsContract.RawContacts.getContactLookupUri(getContentResolver(), uri)));
            finish();
        }
    }

    @Override
    public void onImportFailed(ImportRequest importRequest) {
        if (isFinishing()) {
            Log.i("NfcImportVCardActivity", "Late import failure -- ignoring");
        } else {
            showFailureNotification(R.string.vcard_import_request_rejected_message);
            finish();
        }
    }

    @Override
    public void onImportCanceled(ImportRequest importRequest, int i) {
    }

    @Override
    public Notification onExportProcessed(ExportRequest exportRequest, int i) {
        return null;
    }

    @Override
    public void onExportFailed(ExportRequest exportRequest) {
    }

    @Override
    public void onCancelRequest(CancelRequest cancelRequest, int i) {
    }

    void showFailureNotification(int i) {
        ((NotificationManager) getSystemService("notification")).notify("VCardServiceFailure", 1, NotificationImportExportListener.constructImportFailureNotification(this, getString(i)));
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(NfcImportVCardActivity.this, NfcImportVCardActivity.this.getString(R.string.vcard_import_failed), 1).show();
            }
        });
    }
}
