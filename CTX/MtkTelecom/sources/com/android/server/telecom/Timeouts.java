package com.android.server.telecom;

import android.content.ContentResolver;
import android.provider.Settings;
import java.util.concurrent.TimeUnit;

public final class Timeouts {

    public static class Adapter {
        public long getCallScreeningTimeoutMillis(ContentResolver contentResolver) {
            return Timeouts.getCallScreeningTimeoutMillis(contentResolver);
        }

        public long getCallRemoveUnbindInCallServicesDelay(ContentResolver contentResolver) {
            return Timeouts.getCallRemoveUnbindInCallServicesDelay(contentResolver);
        }

        public long getRetryBluetoothConnectAudioBackoffMillis(ContentResolver contentResolver) {
            return Timeouts.getRetryBluetoothConnectAudioBackoffMillis(contentResolver);
        }

        public long getBluetoothPendingTimeoutMillis(ContentResolver contentResolver) {
            return Timeouts.getBluetoothPendingTimeoutMillis(contentResolver);
        }

        public long getEmergencyCallbackWindowMillis(ContentResolver contentResolver) {
            return Timeouts.getEmergencyCallbackWindowMillis(contentResolver);
        }
    }

    private static long get(ContentResolver contentResolver, String str, long j) {
        return Settings.Secure.getLong(contentResolver, "telecom." + str, j);
    }

    public static long getNewOutgoingCallCancelMillis(ContentResolver contentResolver) {
        return get(contentResolver, "new_outgoing_call_cancel_ms", 500L);
    }

    public static long getMaxNewOutgoingCallCancelMillis(ContentResolver contentResolver) {
        return get(contentResolver, "max_new_outgoing_call_cancel_ms", 10000L);
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

    public static long getCallRemoveUnbindInCallServicesDelay(ContentResolver contentResolver) {
        return get(contentResolver, "call_remove_unbind_in_call_services_delay", 2000L);
    }

    public static long getBluetoothPendingTimeoutMillis(ContentResolver contentResolver) {
        return get(contentResolver, "bluetooth_pending_timeout_millis", 5000L);
    }

    public static long getRetryBluetoothConnectAudioBackoffMillis(ContentResolver contentResolver) {
        return get(contentResolver, "retry_bluetooth_connect_audio_backoff_millis", 500L);
    }

    public static long getCallScreeningTimeoutMillis(ContentResolver contentResolver) {
        return get(contentResolver, "call_screening_timeout", 5000L);
    }

    public static long getEmergencyCallbackWindowMillis(ContentResolver contentResolver) {
        return get(contentResolver, "emergency_callback_window_millis", TimeUnit.MILLISECONDS.convert(5L, TimeUnit.MINUTES));
    }
}
