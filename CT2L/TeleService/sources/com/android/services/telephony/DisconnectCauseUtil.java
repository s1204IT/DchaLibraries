package com.android.services.telephony;

import android.content.Context;
import android.provider.Settings;
import android.telecom.DisconnectCause;
import com.android.phone.PhoneGlobals;
import com.android.phone.R;

public class DisconnectCauseUtil {
    public static DisconnectCause toTelecomDisconnectCause(int telephonyDisconnectCause) {
        return toTelecomDisconnectCause(telephonyDisconnectCause, null);
    }

    public static DisconnectCause toTelecomDisconnectCause(int telephonyDisconnectCause, String reason) {
        Context context = PhoneGlobals.getInstance();
        return new DisconnectCause(toTelecomDisconnectCauseCode(telephonyDisconnectCause), toTelecomDisconnectCauseLabel(context, telephonyDisconnectCause), toTelecomDisconnectCauseDescription(context, telephonyDisconnectCause), toTelecomDisconnectReason(telephonyDisconnectCause, reason), toTelecomDisconnectCauseTone(telephonyDisconnectCause));
    }

    private static int toTelecomDisconnectCauseCode(int telephonyDisconnectCause) {
        switch (telephonyDisconnectCause) {
            case -1:
            case 0:
                break;
            case 1:
                break;
            case 2:
                break;
            case 3:
                break;
            case 4:
                break;
            case 5:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 17:
            case 18:
            case 19:
            case 25:
            case 26:
            case 27:
            case 28:
            case 29:
            case 30:
            case 31:
            case 32:
            case 33:
            case 36:
            case 38:
            case 40:
            case 41:
            case 43:
                break;
            case 6:
            case 39:
            case 42:
            case 45:
                break;
            case 15:
            case 20:
            case 21:
            case 22:
            case 23:
            case 24:
            case 34:
            case 35:
            case 37:
                break;
            case 16:
                break;
            case 44:
                break;
            default:
                Log.w("DisconnectCauseUtil.toTelecomDisconnectCauseCode", "Unrecognized Telephony DisconnectCause " + telephonyDisconnectCause, new Object[0]);
                break;
        }
        return 0;
    }

    private static CharSequence toTelecomDisconnectCauseLabel(Context context, int telephonyDisconnectCause) {
        if (context == null) {
            return "";
        }
        Integer resourceId = null;
        switch (telephonyDisconnectCause) {
            case 4:
                resourceId = Integer.valueOf(R.string.callFailed_userBusy);
                break;
            case 5:
                resourceId = Integer.valueOf(R.string.callFailed_congestion);
                break;
            case 7:
            case 25:
                resourceId = Integer.valueOf(R.string.callFailed_unobtainable_number);
                break;
            case 8:
                resourceId = Integer.valueOf(R.string.callFailed_number_unreachable);
                break;
            case 9:
                resourceId = Integer.valueOf(R.string.callFailed_server_unreachable);
                break;
            case 10:
                resourceId = Integer.valueOf(R.string.callFailed_invalid_credentials);
                break;
            case 11:
                resourceId = Integer.valueOf(R.string.callFailed_out_of_network);
                break;
            case 12:
                resourceId = Integer.valueOf(R.string.callFailed_server_error);
                break;
            case 13:
                resourceId = Integer.valueOf(R.string.callFailed_timedOut);
                break;
            case 14:
            case 27:
                resourceId = Integer.valueOf(R.string.callFailed_noSignal);
                break;
            case 15:
                resourceId = Integer.valueOf(R.string.callFailed_limitExceeded);
                break;
            case 17:
                resourceId = Integer.valueOf(R.string.callFailed_powerOff);
                break;
            case 18:
                resourceId = Integer.valueOf(R.string.callFailed_outOfService);
                break;
            case 19:
                resourceId = Integer.valueOf(R.string.callFailed_simError);
                break;
        }
        return resourceId == null ? "" : context.getResources().getString(resourceId.intValue());
    }

    private static CharSequence toTelecomDisconnectCauseDescription(Context context, int telephonyDisconnectCause) {
        if (context == null) {
            return "";
        }
        Integer resourceId = null;
        switch (telephonyDisconnectCause) {
            case 17:
                boolean airplaneOn = Settings.System.getInt(context.getContentResolver(), "airplane_mode_on", 0) == 1;
                if (!airplaneOn) {
                    resourceId = Integer.valueOf(R.string.incall_error_power_off_sim_disabled);
                } else {
                    resourceId = Integer.valueOf(R.string.incall_error_power_off);
                }
                break;
            case 18:
                resourceId = Integer.valueOf(R.string.incall_error_out_of_service);
                break;
            case 20:
                resourceId = Integer.valueOf(R.string.callFailed_cb_enabled);
                break;
            case 21:
                resourceId = Integer.valueOf(R.string.callFailed_fdn_only);
                break;
            case 22:
                resourceId = Integer.valueOf(R.string.callFailed_dsac_restricted);
                break;
            case 23:
                resourceId = Integer.valueOf(R.string.callFailed_dsac_restricted_normal);
                break;
            case 24:
                resourceId = Integer.valueOf(R.string.callFailed_dsac_restricted_emergency);
                break;
            case 37:
                resourceId = Integer.valueOf(R.string.incall_error_emergency_only);
                break;
            case 38:
                resourceId = Integer.valueOf(R.string.incall_error_no_phone_number_supplied);
                break;
            case 40:
                resourceId = Integer.valueOf(R.string.incall_error_missing_voicemail_number);
                break;
            case 43:
                resourceId = Integer.valueOf(R.string.incall_error_call_failed);
                break;
        }
        return resourceId == null ? "" : context.getResources().getString(resourceId.intValue());
    }

    private static String toTelecomDisconnectReason(int telephonyDisconnectCause, String reason) {
        String causeAsString = android.telephony.DisconnectCause.toString(telephonyDisconnectCause);
        return reason == null ? causeAsString : reason + ", " + causeAsString;
    }

    private static int toTelecomDisconnectCauseTone(int telephonyDisconnectCause) {
        switch (telephonyDisconnectCause) {
            case 2:
            case 3:
            case 36:
                return 27;
            case 4:
                return 17;
            case 5:
                return 18;
            case 18:
            case 27:
                return 95;
            case 25:
                return 21;
            case 28:
                return 37;
            case 29:
                return 38;
            default:
                return -1;
        }
    }
}
