package com.android.common.userhappiness;

import android.content.Context;
import android.content.Intent;
import com.android.common.speech.LoggingEvents;

public class UserHappinessSignals {
    private static boolean mHasVoiceLoggingInfo = false;

    public static void setHasVoiceLoggingInfo(boolean z) {
        mHasVoiceLoggingInfo = z;
    }

    public static void userAcceptedImeText(Context context) {
        if (mHasVoiceLoggingInfo) {
            Intent intent = new Intent(LoggingEvents.ACTION_LOG_EVENT);
            intent.putExtra(LoggingEvents.EXTRA_APP_NAME, LoggingEvents.VoiceIme.APP_NAME);
            intent.putExtra(LoggingEvents.EXTRA_EVENT, 21);
            intent.putExtra(LoggingEvents.EXTRA_CALLING_APP_NAME, context.getPackageName());
            intent.putExtra(LoggingEvents.EXTRA_TIMESTAMP, System.currentTimeMillis());
            context.sendBroadcast(intent);
            setHasVoiceLoggingInfo(false);
        }
    }
}
