package com.android.server.telecom;

import android.content.ContentResolver;
import android.provider.Settings;

public final class Timeouts {
    private static long get(ContentResolver contentResolver, String str, long j) {
        return Settings.Secure.getLong(contentResolver, "telecom." + str, j);
    }

    public static long getDirectToVoicemailMillis(ContentResolver contentResolver) {
        return get(contentResolver, "direct_to_voicemail_ms", 500L);
    }

    public static long getNewOutgoingCallCancelMillis(ContentResolver contentResolver) {
        return get(contentResolver, "new_outgoing_call_cancel_ms", 300L);
    }

    public static long getDelayBetweenDtmfTonesMillis(ContentResolver contentResolver) {
        return get(contentResolver, "delay_between_dtmf_tones_ms", 300L);
    }

    public static long getEmergencyCallTimeoutMillis(ContentResolver contentResolver) {
        return get(contentResolver, "emergency_call_timeout_millis", 25000L);
    }

    public static long getEmergencyCallTimeoutRadioOffMillis(ContentResolver contentResolver) {
        return get(contentResolver, "emergency_call_timeout_radio_off_millis", 60000L);
    }
}
