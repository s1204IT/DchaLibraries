package mediatek.telecom;

import android.os.Bundle;
import android.telecom.Connection;
import android.telecom.PhoneAccountHandle;

public class MtkConnection {
    private static final int CAPABILITY_BASE = 134217728;
    public static final int CAPABILITY_BLIND_OR_ASSURED_ECT = 536870912;
    public static final int CAPABILITY_CAPABILITY_CALL_RECORDING = 1073741824;
    public static final int CAPABILITY_CONSULTATIVE_ECT = 134217728;
    public static final int CAPABILITY_INVITE_PARTICIPANTS = 268435456;
    public static final int CAPABILITY_VIDEO_RINGTONE = Integer.MIN_VALUE;
    public static final String EVENT_3G_VT_STATUS_CHANGED = "mediatek.telecom.event.EVENT_3G_VT_STATUS_CHANGED";
    public static final String EVENT_CALL_ALERTING_NOTIFICATION = "mediatek.telecom.event.EVENT_CALL_ALERTING_NOTIFICATION";
    public static final String EVENT_CDMA_CALL_ACCEPTED = "mediatek.telecom.event.CDMA_CALL_ACCEPTED";
    public static final String EVENT_CONNECTION_LOST = "mediatek.telecom.event.CONNECTION_LOST";
    public static final String EVENT_CSFB = "mediatek.telecom.event.EVENT_CSFB";
    public static final String EVENT_DEVICE_SWITCH_FAILED = "mediatek.telecom.event.EVENT_DEVICE_SWITCH_FAILED";
    public static final String EVENT_DEVICE_SWITCH_SUCCESS = "mediatek.telecom.event.EVENT_DEVICE_SWITCH_SUCCESS";
    public static final String EVENT_INCOMING_INFO_UPDATED = "mediatek.telecom.event.INCOMING_INFO_UPDATED";
    public static final String EVENT_NUMBER_UPDATED = "mediatek.telecom.event.NUMBER_UPDATED";
    public static final String EVENT_OPERATION_FAILED = "mediatek.telecom.event.OPERATION_FAILED";
    public static final String EVENT_PHONE_ACCOUNT_CHANGED = "mediatek.telecom.event.PHONE_ACCOUNT_CHANGED";
    public static final String EVENT_RTT_EMERGENCY_REDIAL = "mediatek.telecom.event.EVENT_RTT_EMERGENCY_REDIAL";
    public static final String EVENT_RTT_UPDOWN_FAIL = "mediatek.telecom.event.EVENT_RTT_UPDOWN_FAIL";
    public static final String EVENT_SRVCC = "mediatek.telecom.event.EVENT_SRVCC";
    public static final String EVENT_SS_NOTIFICATION = "mediatek.telecom.event.SS_NOTIFICATION";
    public static final String EVENT_VOLTE_MARKED_AS_EMERGENCY = "mediatek.telecom.event.EVENT_VOLTE_MARKED_AS_EMERGENCY";
    public static final String EXTRA_3G_VT_STATUS = "mediatek.telecom.extra.3G_VT_STATUS";
    public static final String EXTRA_FAILED_OPERATION = "mediatek.telecom.extra.FAILED_OPERATION";
    public static final String EXTRA_NEW_NUMBER = "mediatek.telecom.extra.NEW_NUMBER";
    public static final String EXTRA_PHONE_ACCOUNT_HANDLE = "mediatek.telecom.extra.PHONE_ACCOUNT_HANDLE";
    public static final String EXTRA_SS_NOTIFICATION_CODE = "mediatek.telecom.extra.SS_NOTIFICATION_CODE";
    public static final String EXTRA_SS_NOTIFICATION_INDEX = "mediatek.telecom.extra.SS_NOTIFICATION_INDEX";
    public static final String EXTRA_SS_NOTIFICATION_NOTITYPE = "mediatek.telecom.extra.SS_NOTIFICATION_NOTITYPE";
    public static final String EXTRA_SS_NOTIFICATION_NUMBER = "mediatek.telecom.extra.SS_NOTIFICATION_NUMBER";
    public static final String EXTRA_SS_NOTIFICATION_TYPE = "mediatek.telecom.extra.SS_NOTIFICATION_TYPE";
    public static final String EXTRA_UPDATED_INCOMING_INFO_ALPHAID = "mediatek.telecom.extra.UPDATED_INCOMING_INFO_ALPHAID";
    public static final String EXTRA_UPDATED_INCOMING_INFO_CLI_VALIDITY = "mediatek.telecom.extra.UPDATED_INCOMING_INFO_CLI_VALIDITY";
    public static final String EXTRA_UPDATED_INCOMING_INFO_TYPE = "mediatek.telecom.extra.UPDATED_INCOMING_INFO_TYPE";
    public static final String OPERATION_ANSWER_CALL = "mediatek.telecom.operation.ANSWER_CALL";
    public static final String OPERATION_DISCONNECT_CALL = "mediatek.telecom.operation.DISCONNECT_CALL";
    private static final int PROPERTY_BASE = 32768;
    public static final int PROPERTY_CDMA = 65536;
    public static final int PROPERTY_CONFERENCE_PARTICIPANT = 262144;
    public static final int PROPERTY_VOICE_RECORDING = 131072;
    public static final int PROPERTY_VOLTE = 32768;

    public static final class MtkRttModifyStatus {
        private static final int MTK_SESSION_STATUS_CODE_BASE = 100;
        public static final int SESSION_DOWNGRADED_BY_REMOTE = 100;
    }

    public static class MtkVideoProvider {
        private static final int MTK_SESSION_MODIFY_BASE = 200;
        public static final int SESSION_EVENT_ERROR_CAMERA_CRASHED = 8003;
        public static final int SESSION_MODIFY_CANCEL_UPGRADE_FAIL = 200;
        public static final int SESSION_MODIFY_CANCEL_UPGRADE_FAIL_AUTO_DOWNGRADE = 201;
        public static final int SESSION_MODIFY_CANCEL_UPGRADE_FAIL_REMOTE_REJECT_UPGRADE = 202;
    }

    public static String propertiesToString(int i) {
        StringBuilder sb = new StringBuilder(Connection.propertiesToString(i));
        sb.setLength(sb.length() - 1);
        sb.append(mtkPropertiesToStringInternal(i, true));
        sb.append("]");
        return sb.toString();
    }

    public static String propertiesToStringShort(int i) {
        StringBuilder sb = new StringBuilder(Connection.propertiesToStringShort(i));
        sb.setLength(sb.length() - 1);
        sb.append(mtkPropertiesToStringInternal(i, false));
        sb.append("]");
        return sb.toString();
    }

    public static String capabilitiesToString(int i) {
        StringBuilder sb = new StringBuilder(Connection.capabilitiesToString(i));
        sb.setLength(sb.length() - 1);
        sb.append(mtkCapabilitiesToStringInternal(i, true));
        sb.append("]");
        return sb.toString();
    }

    public static String capabilitiesToStringShort(int i) {
        StringBuilder sb = new StringBuilder(Connection.capabilitiesToStringShort(i));
        sb.setLength(sb.length() - 1);
        sb.append(mtkCapabilitiesToStringInternal(i, false));
        sb.append("]");
        return sb.toString();
    }

    private static String mtkPropertiesToStringInternal(int i, boolean z) {
        StringBuilder sb = new StringBuilder();
        if (can(i, 32768)) {
            sb.append(z ? " M_PROPERTY_VOLTE" : " m_volte");
        }
        if (can(i, 65536)) {
            sb.append(z ? " M_PROPERTY_CDMA" : " m_cdma");
        }
        if (can(i, 131072)) {
            sb.append(z ? " M_PROPERTY_VOICE_RECORDING" : " m_rcrding");
        }
        if (can(i, 262144)) {
            sb.append(z ? " M_PROPERTY_CONFERENCE_PARTICIPANT" : " m_conf_child");
        }
        return sb.toString();
    }

    private static String mtkCapabilitiesToStringInternal(int i, boolean z) {
        StringBuilder sb = new StringBuilder();
        if (can(i, CAPABILITY_BLIND_OR_ASSURED_ECT)) {
            sb.append(z ? " M_CAPABILITY_BLIND_OR_ASSURED_ECT" : " m_b|a_ect");
        }
        if (can(i, CAPABILITY_CAPABILITY_CALL_RECORDING)) {
            sb.append(z ? " M_CAPABILITY_CAPABILITY_CALL_RECORDING" : " m_rcrd");
        }
        if (can(i, 134217728)) {
            sb.append(z ? " M_CAPABILITY_CONSULTATIVE_ECT" : " m_ect");
        }
        if (can(i, 268435456)) {
            sb.append(z ? " M_CAPABILITY_INVITE_PARTICIPANTS" : " m_invite");
        }
        if (can(i, CAPABILITY_VIDEO_RINGTONE)) {
            sb.append(z ? " M_CAPABILITY_VIDEO_RINGTONE" : " m_vt_tone");
        }
        return sb.toString();
    }

    public static boolean can(int i, int i2) {
        return Connection.can(i, i2);
    }

    public static class ConnectionEventHelper {
        public static Bundle buildParamsForPhoneAccountChanged(PhoneAccountHandle phoneAccountHandle) {
            Bundle bundle = new Bundle();
            bundle.putParcelable(MtkConnection.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle);
            return bundle;
        }

        public static Bundle buildParamsForOperationFailed(int i) {
            Bundle bundle = new Bundle();
            bundle.putInt(MtkConnection.EXTRA_FAILED_OPERATION, i);
            return bundle;
        }

        public static Bundle buildParamsForNumberUpdated(String str) {
            Bundle bundle = new Bundle();
            bundle.putString(MtkConnection.EXTRA_NEW_NUMBER, str);
            return bundle;
        }

        public static Bundle buildParamsForIncomingInfoUpdated(int i, String str, int i2) {
            Bundle bundle = new Bundle();
            bundle.putInt(MtkConnection.EXTRA_UPDATED_INCOMING_INFO_TYPE, i);
            bundle.putString(MtkConnection.EXTRA_UPDATED_INCOMING_INFO_ALPHAID, str);
            bundle.putInt(MtkConnection.EXTRA_UPDATED_INCOMING_INFO_CLI_VALIDITY, i2);
            return bundle;
        }

        public static Bundle buildParamsFor3GVtStatusChanged(int i) {
            Bundle bundle = new Bundle();
            bundle.putInt(MtkConnection.EXTRA_3G_VT_STATUS, i);
            return bundle;
        }

        public static Bundle buildParamsForSsNotification(int i, int i2, int i3, String str, int i4) {
            Bundle bundle = new Bundle();
            bundle.putInt(MtkConnection.EXTRA_SS_NOTIFICATION_NOTITYPE, i);
            bundle.putInt(MtkConnection.EXTRA_SS_NOTIFICATION_TYPE, i2);
            bundle.putInt(MtkConnection.EXTRA_SS_NOTIFICATION_CODE, i3);
            bundle.putString(MtkConnection.EXTRA_SS_NOTIFICATION_NUMBER, str);
            bundle.putInt(MtkConnection.EXTRA_SS_NOTIFICATION_INDEX, i4);
            return bundle;
        }
    }
}
