package com.android.calendar;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.AsyncQueryHandler;
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
        try {
            String queryParameter = uri.getQueryParameter("eid");
            if (queryParameter == null) {
                return null;
            }
            byte[] bArrDecode = Base64.decode(queryParameter, 0);
            int i = 0;
            while (true) {
                if (i >= bArrDecode.length) {
                    break;
                }
                if (bArrDecode[i] == 32) {
                    break;
                }
                i++;
            }
        } catch (RuntimeException e) {
            Log.d("GoogleCalendarUriIntentFilter", "Punting malformed URI " + uri);
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Intent intent = getIntent();
        if (intent != null) {
            Uri data = intent.getData();
            if (data != null) {
                String[] strArrExtractEidAndEmail = extractEidAndEmail(data);
                if (strArrExtractEidAndEmail == null) {
                    Log.d("GoogleCalendarUriIntentFilter", "Could not find event for uri: " + data);
                } else {
                    int i = 0;
                    String str = strArrExtractEidAndEmail[0];
                    String str2 = strArrExtractEidAndEmail[1];
                    Cursor cursorQuery = getContentResolver().query(CalendarContract.Events.CONTENT_URI, EVENT_PROJECTION, "_sync_id LIKE \"%" + str + "\" AND ownerAccount LIKE \"" + str2 + "\"", null, "calendar_access_level desc");
                    if (cursorQuery == null || cursorQuery.getCount() == 0) {
                        Log.i("GoogleCalendarUriIntentFilter", "NOTE: found no matches on event with id='" + str + "'");
                        if (cursorQuery != null) {
                        }
                        finish();
                        return;
                    }
                    Log.i("GoogleCalendarUriIntentFilter", "NOTE: found " + cursorQuery.getCount() + " matches on event with id='" + str + "'");
                    while (cursorQuery.moveToNext()) {
                        try {
                            int i2 = cursorQuery.getInt(0);
                            long j = cursorQuery.getLong(1);
                            long millis = cursorQuery.getLong(2);
                            if (millis == 0) {
                                String string = cursorQuery.getString(3);
                                if (!TextUtils.isEmpty(string)) {
                                    try {
                                        Duration duration = new Duration();
                                        duration.parse(string);
                                        millis = duration.getMillis() + j;
                                        if (millis < j) {
                                        }
                                    } catch (DateException e) {
                                    }
                                }
                            }
                            if ("RESPOND".equals(data.getQueryParameter("action"))) {
                                try {
                                    switch (Integer.parseInt(data.getQueryParameter("rst"))) {
                                        case 1:
                                            i = 1;
                                            break;
                                        case 2:
                                            i = 2;
                                            break;
                                        case 3:
                                            i = 4;
                                            break;
                                    }
                                } catch (NumberFormatException e2) {
                                }
                            }
                            Intent intent2 = new Intent("android.intent.action.VIEW", ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, i2));
                            intent2.setClass(this, EventInfoActivity.class);
                            intent2.putExtra("beginTime", j);
                            intent2.putExtra("endTime", millis);
                            if (i == 0) {
                                startActivity(intent2);
                            } else {
                                updateSelfAttendeeStatus(i2, str2, i, intent2);
                            }
                            finish();
                            return;
                        } finally {
                            cursorQuery.close();
                        }
                    }
                }
            }
            try {
                startNextMatchingActivity(intent);
            } catch (ActivityNotFoundException e3) {
            }
        }
        finish();
    }

    private void updateSelfAttendeeStatus(int i, String str, final int i2, final Intent intent) {
        AsyncQueryHandler asyncQueryHandler = new AsyncQueryHandler(getContentResolver()) {
            @Override
            protected void onUpdateComplete(int i3, Object obj, int i4) {
                int i5;
                if (i4 == 0) {
                    Log.w("GoogleCalendarUriIntentFilter", "No rows updated - starting event viewer");
                    intent.putExtra("attendeeStatus", i2);
                    GoogleCalendarUriIntentFilter.this.startActivity(intent);
                    return;
                }
                int i6 = i2;
                if (i6 != 4) {
                    switch (i6) {
                        case 1:
                            i5 = R.string.rsvp_accepted;
                            break;
                        case 2:
                            i5 = R.string.rsvp_declined;
                            break;
                        default:
                            return;
                    }
                } else {
                    i5 = R.string.rsvp_tentative;
                }
                Toast.makeText(GoogleCalendarUriIntentFilter.this, i5, 1).show();
            }
        };
        ContentValues contentValues = new ContentValues();
        contentValues.put("attendeeStatus", Integer.valueOf(i2));
        asyncQueryHandler.startUpdate(0, null, CalendarContract.Attendees.CONTENT_URI, contentValues, "attendeeEmail=? AND event_id=?", new String[]{str, String.valueOf(i)});
    }
}
