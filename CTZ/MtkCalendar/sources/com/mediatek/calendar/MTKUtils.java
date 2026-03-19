package com.mediatek.calendar;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;
import com.android.calendar.R;

public final class MTKUtils {
    public static boolean isEventShareAvailable(Context context) {
        return "text/x-vcalendar".equalsIgnoreCase(context.getContentResolver().getType(Uri.parse("content://com.mediatek.calendarimporter/")));
    }

    public static void sendShareEvent(Context context, long j) {
        Log.i("MTKUtils", "Utils.sendShareEvent() eventId=" + j);
        Intent intent = new Intent("android.intent.action.SEND");
        intent.putExtra("android.intent.extra.STREAM", Uri.parse("content://com.mediatek.calendarimporter/" + j));
        intent.setType("text/x-vcalendar");
        try {
            context.startActivity(Intent.createChooser(intent, null));
        } catch (ActivityNotFoundException e) {
            Log.i("MTKUtils", "No way to share.");
        }
    }

    public static void writeUnreadReminders(Context context, int i) {
        LogUtil.d("MTKUtils", "Write and broadcast Unread Reminders. num=" + i);
    }

    public static boolean isLowStorage(Context context) {
        return false;
    }

    public static void toastLowStorage(Context context) {
        Toast.makeText(context, R.string.low_storage_save_failed, 1).show();
    }
}
