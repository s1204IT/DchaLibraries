package com.android.contacts;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.provider.ContactsContract;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class NfcHandler implements NfcAdapter.CreateNdefMessageCallback {
    private final Uri mContactUri;
    private final Context mContext;

    public static void register(Activity activity, Uri contactUri) {
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(activity.getApplicationContext());
        if (adapter != null) {
            adapter.setNdefPushMessageCallback(new NfcHandler(activity, contactUri), activity, new Activity[0]);
        }
    }

    public NfcHandler(Context context, Uri contactUri) {
        this.mContext = context;
        this.mContactUri = contactUri;
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        Uri shareUri;
        ContentResolver resolver = this.mContext.getContentResolver();
        if (this.mContactUri != null) {
            String lookupKey = Uri.encode(this.mContactUri.getPathSegments().get(2));
            if (lookupKey.equals("profile")) {
                shareUri = ContactsContract.Profile.CONTENT_VCARD_URI.buildUpon().appendQueryParameter("nophoto", "true").build();
            } else {
                shareUri = ContactsContract.Contacts.CONTENT_VCARD_URI.buildUpon().appendPath(lookupKey).appendQueryParameter("nophoto", "true").build();
            }
            ByteArrayOutputStream ndefBytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            try {
                InputStream vcardInputStream = resolver.openInputStream(shareUri);
                while (true) {
                    int r = vcardInputStream.read(buffer);
                    if (r > 0) {
                        ndefBytes.write(buffer, 0, r);
                    } else {
                        NdefRecord record = NdefRecord.createMime("text/x-vcard", ndefBytes.toByteArray());
                        return new NdefMessage(record, new NdefRecord[0]);
                    }
                }
            } catch (IOException e) {
                Log.e("ContactNfcHandler", "IOException creating vcard.");
                return null;
            }
        } else {
            Log.w("ContactNfcHandler", "No contact URI to share.");
            return null;
        }
    }
}
