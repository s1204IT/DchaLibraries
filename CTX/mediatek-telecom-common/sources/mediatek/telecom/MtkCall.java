package mediatek.telecom;

import android.telecom.Call;

public class MtkCall {

    public static class MtkDetails {
        private static final int MTK_CAPABILITY_BASE = 16777216;
        public static final int MTK_CAPABILITY_BLIND_OR_ASSURED_ECT = 134217728;
        public static final int MTK_CAPABILITY_CALL_RECORDING = 16777216;
        public static final int MTK_CAPABILITY_CONSULTATIVE_ECT = 33554432;
        public static final int MTK_CAPABILITY_INVITE_PARTICIPANTS = 67108864;
        public static final int MTK_CAPABILITY_VIDEO_RINGTONE = 268435456;
        private static final int MTK_PROPERTY_BASE = 65536;
        public static final int MTK_PROPERTY_CDMA = 131072;
        public static final int MTK_PROPERTY_VOICE_RECORDING = 262144;
        public static final int MTK_PROPERTY_VOLTE = 65536;
        public static final int PROPERTY_CONFERENCE_PARTICIPANT = 2097152;
        public static final int PROPERTY_GTT_LOCAL = 524288;
        public static final int PROPERTY_GTT_REMOTE = 1048576;
        public static final int PROPERTY_RTT_SUPPORT_LOCAL = 8388608;
        public static final int PROPERTY_RTT_SUPPORT_REMOTE = 4194304;

        public static String capabilitiesToStringShort(int i) {
            StringBuilder sb = new StringBuilder();
            sb.append("[Capabilities:");
            if (can(i, 1)) {
                sb.append(" hld");
            }
            if (can(i, 2)) {
                sb.append(" sup_hld");
            }
            if (can(i, 4)) {
                sb.append(" mrg_cnf");
            }
            if (can(i, 8)) {
                sb.append(" swp_cnf");
            }
            if (can(i, 32)) {
                sb.append(" rsp_v_txt");
            }
            if (can(i, 64)) {
                sb.append(" mut");
            }
            if (can(i, 128)) {
                sb.append(" mng_cnf");
            }
            if (can(i, 256)) {
                sb.append(" VTlrx");
            }
            if (can(i, 512)) {
                sb.append(" VTltx");
            }
            if (can(i, 1024)) {
                sb.append(" VTrrx");
            }
            if (can(i, 2048)) {
                sb.append(" VTrtx");
            }
            if (can(i, PROPERTY_RTT_SUPPORT_REMOTE)) {
                sb.append(" !v2a");
            }
            if (can(i, 262144)) {
                sb.append(" spd_aud");
            }
            if (can(i, PROPERTY_GTT_LOCAL)) {
                sb.append(" a2v");
            }
            if (can(i, PROPERTY_GTT_REMOTE)) {
                sb.append(" paus_VT");
            }
            if (can(i, PROPERTY_RTT_SUPPORT_LOCAL)) {
                sb.append(" pull");
            }
            if (can(i, 16777216)) {
                sb.append(" m_rcrd");
            }
            if (can(i, MTK_CAPABILITY_CONSULTATIVE_ECT)) {
                sb.append(" m_ect");
            }
            if (can(i, MTK_CAPABILITY_INVITE_PARTICIPANTS)) {
                sb.append(" m_invite");
            }
            if (can(i, 134217728)) {
                sb.append(" m_b|a_ect");
            }
            if (can(i, 268435456)) {
                sb.append(" m_vt_tone");
            }
            sb.append("]");
            return sb.toString();
        }

        public static String propertiesToStringShort(int i) {
            StringBuilder sb = new StringBuilder();
            sb.append("[Properties:");
            if (hasProperty(i, 1)) {
                sb.append(" cnf");
            }
            if (hasProperty(i, 2)) {
                sb.append(" gen_cnf");
            }
            if (hasProperty(i, 8)) {
                sb.append(" wifi");
            }
            if (hasProperty(i, 16)) {
                sb.append(" HD");
            }
            if (hasProperty(i, 4)) {
                sb.append(" ecbm");
            }
            if (hasProperty(i, 64)) {
                sb.append(" xtrnl");
            }
            if (hasProperty(i, 128)) {
                sb.append(" priv");
            }
            if (hasProperty(i, 131072)) {
                sb.append(" m_cdma");
            }
            if (hasProperty(i, 262144)) {
                sb.append(" m_rcrding");
            }
            if (hasProperty(i, 65536)) {
                sb.append(" m_volte");
            }
            if (hasProperty(i, PROPERTY_GTT_LOCAL)) {
                sb.append(" m_gtt_l");
            }
            if (hasProperty(i, PROPERTY_GTT_REMOTE)) {
                sb.append(" m_gtt_r");
            }
            if (hasProperty(i, PROPERTY_CONFERENCE_PARTICIPANT)) {
                sb.append(" m_cnf_chld");
            }
            if (hasProperty(i, PROPERTY_RTT_SUPPORT_LOCAL)) {
                sb.append(" m_rtt_l");
            }
            if (hasProperty(i, PROPERTY_RTT_SUPPORT_REMOTE)) {
                sb.append(" m_rtt_r");
            }
            sb.append("]");
            return sb.toString();
        }

        public static String deltaPropertiesToStringShort(int i, int i2) {
            int i3 = i ^ i2;
            return "Delta Properties Added: " + propertiesToStringShort(i2 & i3) + ", Removed: " + propertiesToStringShort(i & i3);
        }

        public static String deltaCapabilitiesToStringShort(int i, int i2) {
            int i3 = i ^ i2;
            return "Delta Properties Added: " + capabilitiesToStringShort(i2 & i3) + ", Removed: " + capabilitiesToStringShort(i & i3);
        }

        public static boolean can(int i, int i2) {
            return Call.Details.can(i, i2);
        }

        public static boolean hasProperty(int i, int i2) {
            return Call.Details.hasProperty(i, i2);
        }
    }
}
