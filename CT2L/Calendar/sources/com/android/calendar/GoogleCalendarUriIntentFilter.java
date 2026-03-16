package com.android.calendar;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;
import com.android.calendarcommon2.DateException;
import com.android.calendarcommon2.Duration;

public class GoogleCalendarUriIntentFilter extends Activity {
    private static final String[] EVENT_PROJECTION = {"_id", "dtstart", "dtend", "duration"};

    private String[] extractEidAndEmail(Uri uri) {
        String[] strArr = null;
        try {
            String eidParam = uri.getQueryParameter("eid");
            if (eidParam == null) {
                return null;
            }
            byte[] decodedBytes = Base64.decode(eidParam, 0);
            for (int spacePosn = 0; spacePosn < decodedBytes.length; spacePosn++) {
                if (decodedBytes[spacePosn] == 32) {
                    int emailLen = (decodedBytes.length - spacePosn) - 1;
                    if (spacePosn == 0 || emailLen < 3) {
                        return null;
                    }
                    String domain = null;
                    if (decodedBytes[decodedBytes.length - 2] == 64) {
                        emailLen--;
                        switch (decodedBytes[decodedBytes.length - 1]) {
                            case 103:
                                domain = "group.calendar.google.com";
                                break;
                            case 104:
                                domain = "holiday.calendar.google.com";
                                break;
                            case 105:
                                domain = "import.calendar.google.com";
                                break;
                            case 109:
                                domain = "gmail.com";
                                break;
                            case 118:
                                domain = "group.v.calendar.google.com";
                                break;
                            default:
                                Log.wtf("GoogleCalendarUriIntentFilter", "Unexpected one letter domain: " + ((int) decodedBytes[decodedBytes.length - 1]));
                                domain = "%";
                                break;
                        }
                    }
                    String eid = new String(decodedBytes, 0, spacePosn);
                    String email = new String(decodedBytes, spacePosn + 1, emailLen);
                    if (domain != null) {
                        email = email + domain;
                    }
                    strArr = new String[]{eid, email};
                    return strArr;
                }
            }
            return null;
        } catch (RuntimeException e) {
            Log.w("GoogleCalendarUriIntentFilter", "Punting malformed URI " + uri);
            return strArr;
        }
    }

    @Override
    protected void onCreate(Bundle icicle) throws Throwable {
        int eventId;
        long startMillis;
        long endMillis;
        int attendeeStatus;
        Intent intent;
        super.onCreate(icicle);
        Intent intent2 = getIntent();
        if (intent2 != null) {
            Uri uri = intent2.getData();
            if (uri != null) {
                String[] eidParts = extractEidAndEmail(uri);
                if (eidParts == null) {
                    Log.i("GoogleCalendarUriIntentFilter", "Could not find event for uri: " + uri);
                } else {
                    String syncId = eidParts[0];
                    String ownerAccount = eidParts[1];
                    String selection = "_sync_id LIKE \"%" + syncId + "\" AND ownerAccount LIKE \"" + ownerAccount + "\"";
                    Cursor eventCursor = getContentResolver().query(CalendarContract.Events.CONTENT_URI, EVENT_PROJECTION, selection, null, "calendar_access_level desc");
                    if (eventCursor == null || eventCursor.getCount() == 0) {
                        Log.i("GoogleCalendarUriIntentFilter", "NOTE: found no matches on event with id='" + syncId + "'");
                        return;
                    }
                    Log.i("GoogleCalendarUriIntentFilter", "NOTE: found " + eventCursor.getCount() + " matches on event with id='" + syncId + "'");
                    while (eventCursor.moveToNext()) {
                        try {
                            eventId = eventCursor.getInt(0);
                            startMillis = eventCursor.getLong(1);
                            endMillis = eventCursor.getLong(2);
                            if (endMillis == 0) {
                                String duration = eventCursor.getString(3);
                                if (!TextUtils.isEmpty(duration)) {
                                    try {
                                        Duration d = new Duration();
                                        d.parse(duration);
                                        endMillis = startMillis + d.getMillis();
                                        if (endMillis >= startMillis) {
                                        }
                                    } catch (DateException e) {
                                    }
                                }
                            }
                            attendeeStatus = 0;
                            if ("RESPOND".equals(uri.getQueryParameter("action"))) {
                                try {
                                    switch (Integer.parseInt(uri.getQueryParameter("rst"))) {
                                        case 1:
                                            attendeeStatus = 1;
                                            break;
                                        case 2:
                                            attendeeStatus = 2;
                                            break;
                                        case 3:
                                            attendeeStatus = 4;
                                            break;
                                    }
                                } catch (NumberFormatException e2) {
                                }
                            }
                            Uri calendarUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId);
                            intent = new Intent("android.intent.action.VIEW", calendarUri);
                        } catch (Throwable th) {
                            th = th;
                        }
                        try {
                            intent.setClass(this, EventInfoActivity.class);
                            intent.putExtra("beginTime", startMillis);
                            intent.putExtra("endTime", endMillis);
                            if (attendeeStatus == 0) {
                                startActivity(intent);
                            } else {
                                updateSelfAttendeeStatus(eventId, ownerAccount, attendeeStatus, intent);
                            }
                            finish();
                            eventCursor.close();
                            return;
                        } catch (Throwable th2) {
                            th = th2;
                            eventCursor.close();
                            throw th;
                        }
                    }
                    eventCursor.close();
                }
            }
            try {
                startNextMatchingActivity(intent2);
            } catch (ActivityNotFoundException e3) {
            }
        }
        finish();
    }

    private void updateSelfAttendeeStatus(int eventId, String ownerAccount, final int status, final Intent intent) {
        ContentResolver cr = getContentResolver();
        AsyncQueryHandler queryHandler = new AsyncQueryHandler(cr) {
            @Override
            protected void onUpdateComplete(int token, Object cookie, int result) {
                int toastId;
                if (result == 0) {
                    Log.w("GoogleCalendarUriIntentFilter", "No rows updated - starting event viewer");
                    intent.putExtra("attendeeStatus", status);
                    GoogleCalendarUriIntentFilter.this.startActivity(intent);
                    return;
                }
                switch (status) {
                    case 1:
                        toastId = R.string.rsvp_accepted;
                        break;
                    case 2:
                        toastId = R.string.rsvp_declined;
                        break;
                    case 3:
                    default:
                        return;
                    case 4:
                        toastId = R.string.rsvp_tentative;
                        break;
                }
                Toast.makeText(GoogleCalendarUriIntentFilter.this, toastId, 1).show();
            }
        };
        ContentValues values = new ContentValues();
        values.put("attendeeStatus", Integer.valueOf(status));
        queryHandler.startUpdate(0, null, CalendarContract.Attendees.CONTENT_URI, values, "attendeeEmail=? AND event_id=?", new String[]{ownerAccount, String.valueOf(eventId)});
    }
}
