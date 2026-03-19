package com.android.contacts;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.provider.ContactsContract;
import com.mediatek.contacts.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class NfcHandler implements NfcAdapter.CreateNdefMessageCallback {
    private final Uri mContactUri;
    private final Context mContext;

    public static void register(Activity activity, Uri uri) {
        Log.i("ContactNfcHandler", "[register]");
        NfcAdapter defaultAdapter = NfcAdapter.getDefaultAdapter(activity.getApplicationContext());
        if (defaultAdapter == null) {
            Log.e("ContactNfcHandler", "[register],NfcAdapter is null,return!");
        } else {
            defaultAdapter.setNdefPushMessageCallback(new NfcHandler(activity, uri), activity, new Activity[0]);
        }
    }

    public NfcHandler(Context context, Uri uri) {
        Log.i("ContactNfcHandler", "[NfcHandler]new");
        this.mContext = context;
        this.mContactUri = uri;
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent nfcEvent) throws Throwable {
        Uri uriBuild;
        InputStream inputStreamOpenInputStream;
        ?? contentResolver = this.mContext.getContentResolver();
        if (this.mContactUri != null) {
            String strEncode = Uri.encode(this.mContactUri.getPathSegments().get(2));
            if (strEncode.equals("profile")) {
                uriBuild = ContactsContract.Profile.CONTENT_VCARD_URI.buildUpon().appendQueryParameter("no_photo", "true").build();
            } else {
                uriBuild = ContactsContract.Contacts.CONTENT_VCARD_URI.buildUpon().appendPath(strEncode).appendQueryParameter("no_photo", "true").build();
            }
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] bArr = new byte[1024];
            try {
                try {
                    inputStreamOpenInputStream = contentResolver.openInputStream(uriBuild);
                    while (true) {
                        try {
                            int i = inputStreamOpenInputStream.read(bArr);
                            if (i <= 0) {
                                break;
                            }
                            byteArrayOutputStream.write(bArr, 0, i);
                        } catch (IOException e) {
                            Log.e("ContactNfcHandler", "IOException creating vcard.");
                            if (inputStreamOpenInputStream != null) {
                                try {
                                    inputStreamOpenInputStream.close();
                                } catch (IOException e2) {
                                    e2.printStackTrace();
                                }
                            }
                            return null;
                        }
                    }
                    NdefMessage ndefMessage = new NdefMessage(NdefRecord.createMime("text/x-vcard", byteArrayOutputStream.toByteArray()), new NdefRecord[0]);
                    if (inputStreamOpenInputStream != null) {
                        try {
                            inputStreamOpenInputStream.close();
                        } catch (IOException e3) {
                            e3.printStackTrace();
                        }
                    }
                    return ndefMessage;
                } catch (Throwable th) {
                    th = th;
                    if (contentResolver != 0) {
                        try {
                            contentResolver.close();
                        } catch (IOException e4) {
                            e4.printStackTrace();
                        }
                    }
                    throw th;
                }
            } catch (IOException e5) {
                inputStreamOpenInputStream = null;
            } catch (Throwable th2) {
                th = th2;
                contentResolver = 0;
                if (contentResolver != 0) {
                }
                throw th;
            }
        } else {
            Log.w("ContactNfcHandler", "No contact URI to share.");
            return null;
        }
    }
}
