package com.mediatek.calendar.nfc;

import android.app.Activity;
import android.content.ContentResolver;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.util.Log;
import com.android.calendar.EventInfoFragment;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class NfcHandler implements NfcAdapter.CreateNdefMessageCallback {
    private final EventInfoFragment mEventInfoFragment;

    public static void register(Activity activity, EventInfoFragment eventInfoFragment) {
        NfcAdapter defaultAdapter = NfcAdapter.getDefaultAdapter(activity.getApplicationContext());
        if (defaultAdapter == null) {
            Log.w("CalendarNfcHandler", "register nfc, NFC not available on this device!");
        } else {
            defaultAdapter.setNdefPushMessageCallback(new NfcHandler(eventInfoFragment), activity, new Activity[0]);
        }
    }

    public NfcHandler(EventInfoFragment eventInfoFragment) {
        this.mEventInfoFragment = eventInfoFragment;
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent nfcEvent) {
        Log.i("CalendarNfcHandler", "createNdefMessage..............");
        Uri uri = this.mEventInfoFragment.getUri();
        ContentResolver contentResolver = this.mEventInfoFragment.getActivity().getContentResolver();
        if (uri != null) {
            String lastPathSegment = uri.getLastPathSegment();
            Log.i("CalendarNfcHandler", "createNdefMessage, eventId=" + lastPathSegment);
            Uri uri2 = Uri.parse("content://com.mediatek.calendarimporter/" + lastPathSegment);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] bArr = new byte[1024];
            try {
                InputStream inputStreamOpenInputStream = contentResolver.openInputStream(uri2);
                if (inputStreamOpenInputStream == null) {
                    Log.i("CalendarNfcHandler", "createNdefMessage, vcalendarInputStream = null");
                    return null;
                }
                while (true) {
                    try {
                        int i = inputStreamOpenInputStream.read(bArr);
                        if (i > 0) {
                            byteArrayOutputStream.write(bArr, 0, i);
                        } else {
                            return new NdefMessage(NdefRecord.createMime("text/x-vcalendar", byteArrayOutputStream.toByteArray()), new NdefRecord[0]);
                        }
                    } finally {
                        inputStreamOpenInputStream.close();
                    }
                }
            } catch (IOException e) {
                Log.e("CalendarNfcHandler", "IOException creating vcalendar.");
                return null;
            }
        } else {
            Log.w("CalendarNfcHandler", "No event URI to share.");
            return null;
        }
    }
}
