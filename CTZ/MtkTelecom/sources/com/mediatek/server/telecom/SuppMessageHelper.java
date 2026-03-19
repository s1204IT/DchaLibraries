package com.mediatek.server.telecom;

import android.os.Bundle;
import android.util.SparseIntArray;
import com.android.server.telecom.R;

public class SuppMessageHelper {
    private static final SparseIntArray sMoCodeResIdPairs = new SparseIntArray() {
        {
            put(0, R.string.mo_code_unconditional_cf_active);
            put(1, R.string.mo_code_some_cf_active);
            put(2, R.string.mo_code_call_forwarded);
            put(3, R.string.call_waiting_indication);
            put(4, R.string.mo_code_cug_call);
            put(5, R.string.mo_code_outgoing_calls_barred);
            put(6, R.string.mo_code_incoming_calls_barred);
            put(7, R.string.mo_code_clir_suppression_rejected);
            put(8, R.string.mo_code_call_deflected);
            put(9, R.string.mo_code_call_forwarding);
        }
    };
    private static final SparseIntArray sMtCodeResIdPairs = new SparseIntArray() {
        {
            put(0, R.string.mt_code_forwarded_call);
            put(1, R.string.mt_code_cug_call);
            put(2, R.string.mt_code_call_on_hold);
            put(3, R.string.mt_code_call_retrieved);
            put(4, R.string.mt_code_multi_party_call);
            put(5, R.string.mt_code_on_hold_call_released);
            put(6, R.string.mt_code_forward_check_received);
            put(7, R.string.mt_code_call_connecting_ect);
            put(8, R.string.mt_code_call_connected_ect);
            put(9, R.string.mt_code_deflected_call);
            put(10, R.string.mt_code_additional_call_forwarded);
            put(11, R.string.mt_code_forwarded_call);
            put(12, R.string.mt_code_forwarded_call);
            put(13, R.string.mt_code_forwarded_call);
            put(14, R.string.mt_code_forwarded_call);
            put(15, R.string.mt_code_forwarded_call);
            put(16, R.string.mt_code_forwarded_call);
        }
    };
    private static final SparseIntArray sMtCodeForwardedDetailResIdPairs = new SparseIntArray() {
        {
            put(11, R.string.mt_code_forwarded_cf);
            put(12, R.string.mt_code_forwarded_cf_uncond);
            put(13, R.string.mt_code_forwarded_cf_cond);
            put(14, R.string.mt_code_forwarded_cf_busy);
            put(15, R.string.mt_code_forwarded_cf_no_reply);
            put(16, R.string.mt_code_forwarded_cf_not_reachable);
        }
    };

    public void onSsNotification(Bundle bundle) {
        int i = bundle.getInt("mediatek.telecom.extra.SS_NOTIFICATION_NOTITYPE");
        int i2 = bundle.getInt("mediatek.telecom.extra.SS_NOTIFICATION_TYPE");
        int i3 = bundle.getInt("mediatek.telecom.extra.SS_NOTIFICATION_CODE");
        String string = bundle.getString("mediatek.telecom.extra.SS_NOTIFICATION_NUMBER");
        int i4 = bundle.getInt("mediatek.telecom.extra.SS_NOTIFICATION_INDEX");
        StringBuilder sb = new StringBuilder();
        if (i == 0) {
            sb.append(getSuppServiceMoString(i3, i4));
        } else if (i == 1) {
            sb.append(getSuppServiceMtString(i3, i4));
            if (i2 == 145 && string != null && string.length() != 0) {
                sb.append(" +");
                sb.append(string);
            }
        }
        if (sb.length() > 0) {
            MtkTelecomGlobals.getInstance().showToast(sb.toString());
        }
    }

    private static String getSuppServiceMoString(int i, int i2) {
        int i3 = sMoCodeResIdPairs.get(i, R.string.incall_error_supp_service_unknown);
        StringBuilder sb = new StringBuilder();
        sb.append(MtkTelecomGlobals.getInstance().getContext().getString(i3));
        if (i == 4) {
            sb.append(" ");
            sb.append(i2);
        }
        return sb.toString();
    }

    private static String getSuppServiceMtString(int i, int i2) {
        int i3 = sMtCodeResIdPairs.get(i, R.string.incall_error_supp_service_unknown);
        StringBuilder sb = new StringBuilder();
        sb.append(MtkTelecomGlobals.getInstance().getContext().getString(i3));
        if (i == 1) {
            sb.append(" ");
            sb.append(i2);
        }
        if (sMtCodeForwardedDetailResIdPairs.get(i, -1) != -1) {
            String string = MtkTelecomGlobals.getInstance().getContext().getString(sMtCodeForwardedDetailResIdPairs.get(i));
            sb.append("(");
            sb.append(string);
            sb.append(")");
        }
        return sb.toString();
    }
}
