package com.android.contacts.common.vcard;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.util.Log;
import com.android.contacts.R;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.account.AccountWithDataSet;
import com.android.contacts.common.vcard.VCardService;
import com.android.vcard.VCardEntry;
import com.android.vcard.VCardEntryCounter;
import com.android.vcard.VCardParser;
import com.android.vcard.VCardParser_V21;
import com.android.vcard.VCardParser_V30;
import com.android.vcard.VCardSourceDetector;
import com.android.vcard.exception.VCardException;
import com.android.vcard.exception.VCardNestedException;
import com.android.vcard.exception.VCardVersionException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NfcImportVCardActivity extends Activity implements ServiceConnection, VCardImportExportListener {
    private AccountWithDataSet mAccount;
    private NdefRecord mRecord;

    class ImportTask extends AsyncTask<VCardService, Void, ImportRequest> {
        ImportTask() {
        }

        @Override
        public ImportRequest doInBackground(VCardService... services) throws Throwable {
            ImportRequest request = NfcImportVCardActivity.this.createImportRequest();
            if (request == null) {
                return null;
            }
            ArrayList<ImportRequest> requests = new ArrayList<>();
            requests.add(request);
            services[0].handleImportRequest(requests, NfcImportVCardActivity.this);
            return request;
        }

        @Override
        public void onCancelled() {
            NfcImportVCardActivity.this.unbindService(NfcImportVCardActivity.this);
        }

        @Override
        public void onPostExecute(ImportRequest request) {
            NfcImportVCardActivity.this.unbindService(NfcImportVCardActivity.this);
        }
    }

    ImportRequest createImportRequest() throws Throwable {
        VCardSourceDetector detector;
        VCardEntryCounter counter;
        VCardEntryCounter counter2 = null;
        VCardSourceDetector detector2 = null;
        int vcardVersion = 1;
        try {
            try {
                ByteArrayInputStream is = new ByteArrayInputStream(this.mRecord.getPayload());
                is.mark(0);
                VCardParser parser = new VCardParser_V21();
                try {
                    counter = new VCardEntryCounter();
                } catch (VCardVersionException e) {
                    detector = null;
                    counter = null;
                } catch (Throwable th) {
                    th = th;
                }
                try {
                    detector = new VCardSourceDetector();
                    try {
                        try {
                            parser.addInterpreter(counter);
                            parser.addInterpreter(detector);
                            parser.parse(is);
                            if (is != null) {
                                try {
                                    is.close();
                                    detector2 = detector;
                                    counter2 = counter;
                                } catch (VCardNestedException e2) {
                                    detector2 = detector;
                                    counter2 = counter;
                                    Log.w("NfcImportVCardActivity", "Nested Exception is found (it may be false-positive).");
                                } catch (VCardException e3) {
                                    e = e3;
                                    Log.e("NfcImportVCardActivity", "Error parsing vcard", e);
                                    return null;
                                } catch (IOException e4) {
                                    detector2 = detector;
                                    counter2 = counter;
                                }
                            } else {
                                detector2 = detector;
                                counter2 = counter;
                            }
                        } catch (VCardVersionException e5) {
                            is.reset();
                            vcardVersion = 2;
                            VCardParser parser2 = new VCardParser_V30();
                            try {
                                counter2 = new VCardEntryCounter();
                                try {
                                    detector2 = new VCardSourceDetector();
                                } catch (VCardVersionException e6) {
                                } catch (Throwable th2) {
                                    th = th2;
                                }
                            } catch (VCardVersionException e7) {
                            } catch (Throwable th3) {
                                th = th3;
                            }
                            try {
                                parser2.addInterpreter(counter2);
                                parser2.addInterpreter(detector2);
                                parser2.parse(is);
                                if (is != null) {
                                    try {
                                        is.close();
                                    } catch (IOException e8) {
                                    }
                                }
                            } catch (VCardVersionException e9) {
                                if (is == null) {
                                    return null;
                                }
                                try {
                                    is.close();
                                    return null;
                                } catch (IOException e10) {
                                    return null;
                                }
                            } catch (Throwable th4) {
                                th = th4;
                                if (is != null) {
                                }
                                throw th;
                            }
                        }
                    } catch (Throwable th5) {
                        th = th5;
                        if (is != null) {
                            try {
                                is.close();
                            } catch (IOException e11) {
                            }
                        }
                        throw th;
                    }
                } catch (VCardVersionException e12) {
                    detector = null;
                } catch (Throwable th6) {
                    th = th6;
                    if (is != null) {
                    }
                    throw th;
                }
            } catch (IOException e13) {
                Log.e("NfcImportVCardActivity", "Failed reading vcard data", e13);
                return null;
            }
        } catch (VCardNestedException e14) {
        } catch (VCardException e15) {
            e = e15;
        }
        return new ImportRequest(this.mAccount, this.mRecord.getPayload(), null, getString(R.string.nfc_vcard_file_name), detector2.getEstimatedType(), detector2.getEstimatedCharset(), vcardVersion, counter2.getCount());
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        VCardService service = ((VCardService.MyBinder) binder).getService();
        new ImportTask().execute(service);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
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
        NdefMessage msg = (NdefMessage) intent.getParcelableArrayExtra("android.nfc.extra.NDEF_MESSAGES")[0];
        this.mRecord = msg.getRecords()[0];
        AccountTypeManager accountTypes = AccountTypeManager.getInstance(this);
        List<AccountWithDataSet> accountList = accountTypes.getAccounts(true);
        if (accountList.size() == 0) {
            this.mAccount = null;
        } else if (accountList.size() == 1) {
            this.mAccount = accountList.get(0);
        } else {
            startActivityForResult(new Intent(this, (Class<?>) SelectAccountActivity.class), 1);
            return;
        }
        startImport();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == 1) {
            if (resultCode == -1) {
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
    public void onImportProcessed(ImportRequest request, int jobId, int sequence) {
    }

    @Override
    public void onImportParsed(ImportRequest request, int jobId, VCardEntry entry, int currentCount, int totalCount) {
    }

    @Override
    public void onImportFinished(ImportRequest request, int jobId, Uri uri) {
        if (isFinishing()) {
            Log.i("NfcImportVCardActivity", "Late import -- ignoring");
        } else if (uri != null) {
            Uri contactUri = ContactsContract.RawContacts.getContactLookupUri(getContentResolver(), uri);
            Intent intent = new Intent("android.intent.action.VIEW", contactUri);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onImportFailed(ImportRequest request) {
        if (isFinishing()) {
            Log.i("NfcImportVCardActivity", "Late import failure -- ignoring");
        }
    }

    @Override
    public void onImportCanceled(ImportRequest request, int jobId) {
    }

    @Override
    public void onExportProcessed(ExportRequest request, int jobId) {
    }

    @Override
    public void onExportFailed(ExportRequest request) {
    }

    @Override
    public void onCancelRequest(CancelRequest request, int type) {
    }
}
