package com.android.services.telephony;

import android.content.Context;
import android.provider.Settings;
import android.telecom.DisconnectCause;
import com.android.phone.ImsUtil;
import com.android.phone.PhoneGlobals;
import com.android.phone.R;
import com.android.phone.SubscriptionInfoHelper;
import com.android.phone.TimeConsumingPreferenceActivity;
import com.mediatek.phone.ext.ExtensionManager;

public class DisconnectCauseUtil {
    public static DisconnectCause toTelecomDisconnectCause(int i) {
        return toTelecomDisconnectCause(i, -1, null);
    }

    public static DisconnectCause toTelecomDisconnectCause(int i, String str) {
        return toTelecomDisconnectCause(i, -1, str);
    }

    public static DisconnectCause toTelecomDisconnectCause(int i, int i2, String str) {
        PhoneGlobals phoneGlobals = PhoneGlobals.getInstance();
        int iChangeDisconnectCause = ExtensionManager.getIncomingCallExt().changeDisconnectCause(i);
        return new DisconnectCause(toTelecomDisconnectCauseCode(iChangeDisconnectCause), toTelecomDisconnectCauseLabel(phoneGlobals, iChangeDisconnectCause, i2), toTelecomDisconnectCauseDescription(phoneGlobals, iChangeDisconnectCause), toTelecomDisconnectReason(phoneGlobals, iChangeDisconnectCause, str), toTelecomDisconnectCauseTone(iChangeDisconnectCause));
    }

    private static int toTelecomDisconnectCauseCode(int i) {
        switch (i) {
            case SubscriptionInfoHelper.NO_SUB_ID:
            case 0:
                return 0;
            case 1:
                return 5;
            case 2:
                return 3;
            case 3:
                return 2;
            case 4:
                return 7;
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
            case 46:
            case 47:
            case 48:
            case 49:
            case 53:
            case 54:
            case 55:
                return 1;
            case 6:
            case 39:
            case 42:
            case 45:
                return 9;
            case 15:
            case 20:
            case 21:
            case 22:
            case 23:
            case 24:
            case 34:
            case 35:
            case 37:
            case 50:
                return 8;
            case 16:
                return 6;
            case 44:
                return 4;
            case 51:
                return 12;
            case 52:
                return 11;
            default:
                switch (i) {
                    default:
                        switch (i) {
                            case 65:
                                return 3;
                            case 66:
                            case 67:
                            case 68:
                            case 69:
                            case 70:
                            case 71:
                                break;
                            default:
                                switch (i) {
                                    case TimeConsumingPreferenceActivity.RESPONSE_ERROR:
                                    case 401:
                                    case 402:
                                    case 403:
                                        return 200;
                                    default:
                                        switch (i) {
                                            case 1041:
                                                return 4;
                                            case 1042:
                                            case 1043:
                                            case 1045:
                                            case 1046:
                                                return 200;
                                            case 1044:
                                                return 1;
                                            default:
                                                Log.w("DisconnectCauseUtil.toTelecomDisconnectCauseCode", "Unrecognized Telephony DisconnectCause " + i, new Object[0]);
                                                return 0;
                                        }
                                }
                        }
                    case 57:
                    case 58:
                    case 59:
                    case 60:
                    case 61:
                    case 62:
                        break;
                }
                break;
        }
    }

    private static CharSequence toTelecomDisconnectCauseLabel(Context context, int i, int i2) {
        if (i2 != -1) {
            return getLabelFromPreciseDisconnectCause(context, i2, i);
        }
        return getLabelFromDisconnectCause(context, i);
    }

    private static CharSequence getLabelFromDisconnectCause(Context context, int i) {
        if (context == null) {
            return "";
        }
        Integer numValueOf = null;
        switch (i) {
            case 4:
                numValueOf = Integer.valueOf(R.string.callFailed_userBusy);
                break;
            case 5:
                numValueOf = Integer.valueOf(R.string.callFailed_congestion);
                break;
            default:
                switch (i) {
                    case 7:
                        numValueOf = Integer.valueOf(R.string.callFailed_unobtainable_number);
                        break;
                    case 8:
                        numValueOf = Integer.valueOf(R.string.callFailed_number_unreachable);
                        break;
                    case 9:
                        numValueOf = Integer.valueOf(R.string.callFailed_server_unreachable);
                        break;
                    case 10:
                        numValueOf = Integer.valueOf(R.string.callFailed_invalid_credentials);
                        break;
                    case 11:
                        numValueOf = Integer.valueOf(R.string.callFailed_out_of_network);
                        break;
                    case 12:
                        numValueOf = Integer.valueOf(R.string.callFailed_server_error);
                        break;
                    case 13:
                        numValueOf = Integer.valueOf(R.string.callFailed_timedOut);
                        break;
                    case 14:
                        numValueOf = Integer.valueOf(R.string.callFailed_noSignal);
                        break;
                    case 15:
                        numValueOf = Integer.valueOf(R.string.callFailed_limitExceeded);
                        break;
                    default:
                        switch (i) {
                            case 17:
                                numValueOf = Integer.valueOf(R.string.callFailed_powerOff);
                                break;
                            case 18:
                                numValueOf = Integer.valueOf(R.string.callFailed_outOfService);
                                break;
                            case 19:
                                numValueOf = Integer.valueOf(R.string.callFailed_simError);
                                break;
                            default:
                                switch (i) {
                                    case 53:
                                        numValueOf = Integer.valueOf(R.string.callFailed_maximum_reached);
                                        break;
                                    case 54:
                                        numValueOf = Integer.valueOf(R.string.callFailed_data_disabled);
                                        break;
                                    case 55:
                                        numValueOf = Integer.valueOf(R.string.callFailed_data_limit_reached);
                                        break;
                                    default:
                                        switch (i) {
                                            case 61:
                                                numValueOf = Integer.valueOf(R.string.callFailed_low_battery);
                                                break;
                                            case 62:
                                                numValueOf = Integer.valueOf(R.string.dialFailed_low_battery);
                                                break;
                                            default:
                                                switch (i) {
                                                    case TimeConsumingPreferenceActivity.RESPONSE_ERROR:
                                                        numValueOf = Integer.valueOf(R.string.wfc_wifi_call_drop);
                                                        break;
                                                    case 401:
                                                        numValueOf = Integer.valueOf(R.string.wfc_internet_connection_lost);
                                                        break;
                                                    case 402:
                                                        numValueOf = Integer.valueOf(R.string.wfc_wifi_call_drop);
                                                        break;
                                                    case 403:
                                                        numValueOf = Integer.valueOf(R.string.wfc_no_network);
                                                        break;
                                                    default:
                                                        switch (i) {
                                                            case 1045:
                                                                numValueOf = Integer.valueOf(R.string.wfc_wifi_call_drop_bad_rssi);
                                                                break;
                                                            case 1046:
                                                                Integer.valueOf(R.string.wfc_wifi_call_drop_backhaul_congestion);
                                                                numValueOf = Integer.valueOf(R.string.incall_error_power_off);
                                                                break;
                                                            default:
                                                                switch (i) {
                                                                    case 25:
                                                                        break;
                                                                    case 27:
                                                                        break;
                                                                    case 51:
                                                                        numValueOf = Integer.valueOf(R.string.callEnded_pulled);
                                                                        break;
                                                                    case 71:
                                                                        numValueOf = Integer.valueOf(R.string.incall_error_power_off);
                                                                        break;
                                                                }
                                                                break;
                                                        }
                                                        break;
                                                }
                                                break;
                                        }
                                        break;
                                }
                                break;
                        }
                        break;
                }
                break;
        }
        return numValueOf == null ? "" : context.getResources().getString(numValueOf.intValue());
    }

    private static CharSequence getLabelFromPreciseDisconnectCause(Context context, int i, int i2) {
        Integer numValueOf;
        if (context == null) {
            return "";
        }
        switch (i) {
            case 16:
                numValueOf = Integer.valueOf(R.string.clh_callFailed_normal_call_clearing_txt);
                break;
            case 17:
                numValueOf = Integer.valueOf(R.string.clh_callFailed_user_busy_txt);
                break;
            case 18:
                numValueOf = Integer.valueOf(R.string.clh_callFailed_no_user_responding_txt);
                break;
            case 19:
                numValueOf = Integer.valueOf(R.string.clh_callFailed_user_alerting_txt);
                break;
            default:
                switch (i) {
                    case 21:
                        numValueOf = Integer.valueOf(R.string.clh_callFailed_call_rejected_txt);
                        break;
                    case 22:
                        numValueOf = Integer.valueOf(R.string.clh_callFailed_number_changed_txt);
                        break;
                    default:
                        switch (i) {
                            case 25:
                                numValueOf = Integer.valueOf(R.string.clh_callFailed_pre_emption_txt);
                                break;
                            case 26:
                                numValueOf = Integer.valueOf(R.string.clh_callFailed_non_selected_user_clearing_txt);
                                break;
                            case 27:
                                numValueOf = Integer.valueOf(R.string.clh_callFailed_destination_out_of_order_txt);
                                break;
                            case 28:
                                numValueOf = Integer.valueOf(R.string.clh_callFailed_invalid_number_format_txt);
                                break;
                            case 29:
                                numValueOf = Integer.valueOf(R.string.clh_callFailed_facility_rejected_txt);
                                break;
                            case 30:
                                numValueOf = Integer.valueOf(R.string.clh_callFailed_response_to_STATUS_ENQUIRY_txt);
                                break;
                            case 31:
                                numValueOf = Integer.valueOf(R.string.clh_callFailed_normal_unspecified_txt);
                                break;
                            default:
                                switch (i) {
                                    case 41:
                                        numValueOf = Integer.valueOf(R.string.clh_callFailed_temporary_failure_txt);
                                        break;
                                    case 42:
                                        numValueOf = Integer.valueOf(R.string.clh_callFailed_switching_equipment_congestion_txt);
                                        break;
                                    case 43:
                                        numValueOf = Integer.valueOf(R.string.clh_callFailed_access_information_discarded_txt);
                                        break;
                                    case 44:
                                        numValueOf = Integer.valueOf(R.string.clh_callFailed_requested_circuit_txt);
                                        break;
                                    default:
                                        switch (i) {
                                            case 49:
                                                numValueOf = Integer.valueOf(R.string.clh_callFailed_quality_of_service_unavailable_txt);
                                                break;
                                            case 50:
                                                numValueOf = Integer.valueOf(R.string.clh_callFailed_requested_facility_not_subscribed_txt);
                                                break;
                                            default:
                                                switch (i) {
                                                    case 57:
                                                        numValueOf = Integer.valueOf(R.string.clh_callFailed_bearer_capability_not_authorized_txt);
                                                        break;
                                                    case 58:
                                                        numValueOf = Integer.valueOf(R.string.clh_callFailed_bearer_capability_not_presently_available_txt);
                                                        break;
                                                    default:
                                                        switch (i) {
                                                            case 68:
                                                                numValueOf = Integer.valueOf(R.string.clh_callFailed_ACM_equal_to_or_greater_than_ACMmax_txt);
                                                                break;
                                                            case 69:
                                                                numValueOf = Integer.valueOf(R.string.clh_callFailed_requested_facility_not_implemented_txt);
                                                                break;
                                                            case 70:
                                                                numValueOf = Integer.valueOf(R.string.clh_callFailed_only_restricted_digital_information_bearer_capability_is_available_txt);
                                                                break;
                                                            default:
                                                                switch (i) {
                                                                    case 87:
                                                                        numValueOf = Integer.valueOf(R.string.clh_callFailed_user_not_member_of_CUG_txt);
                                                                        break;
                                                                    case 88:
                                                                        numValueOf = Integer.valueOf(R.string.clh_callFailed_incompatible_destination_txt);
                                                                        break;
                                                                    default:
                                                                        switch (i) {
                                                                            case 95:
                                                                                numValueOf = Integer.valueOf(R.string.clh_callFailed_semantically_incorrect_message_txt);
                                                                                break;
                                                                            case 96:
                                                                                numValueOf = Integer.valueOf(R.string.clh_callFailed_invalid_mandatory_information_txt);
                                                                                break;
                                                                            case 97:
                                                                                numValueOf = Integer.valueOf(R.string.clh_callFailed_message_type_non_existent_or_not_implemented_txt);
                                                                                break;
                                                                            case 98:
                                                                                numValueOf = Integer.valueOf(R.string.clh_callFailed_message_type_not_compatible_with_protocol_state_txt);
                                                                                break;
                                                                            case 99:
                                                                                numValueOf = Integer.valueOf(R.string.clh_callFailed_information_element_non_existent_or_not_implemented_txt);
                                                                                break;
                                                                            case 100:
                                                                                numValueOf = Integer.valueOf(R.string.clh_callFailed_conditional_IE_error_txt);
                                                                                break;
                                                                            case 101:
                                                                                numValueOf = Integer.valueOf(R.string.clh_callFailed_message_not_compatible_with_protocol_state_txt);
                                                                                break;
                                                                            case 102:
                                                                                numValueOf = Integer.valueOf(R.string.clh_callFailed_recovery_on_timer_expiry_txt);
                                                                                break;
                                                                            default:
                                                                                switch (i) {
                                                                                    case 1:
                                                                                        numValueOf = Integer.valueOf(R.string.clh_callFailed_unassigned_number_txt);
                                                                                        break;
                                                                                    case 3:
                                                                                        numValueOf = Integer.valueOf(R.string.clh_callFailed_no_route_to_destination_txt);
                                                                                        break;
                                                                                    case 6:
                                                                                        numValueOf = Integer.valueOf(R.string.clh_callFailed_channel_unacceptable_txt);
                                                                                        break;
                                                                                    case 8:
                                                                                        numValueOf = Integer.valueOf(R.string.clh_callFailed_operator_determined_barring_txt);
                                                                                        break;
                                                                                    case 34:
                                                                                        numValueOf = Integer.valueOf(R.string.clh_callFailed_no_circuit_available_txt);
                                                                                        break;
                                                                                    case 38:
                                                                                        numValueOf = Integer.valueOf(R.string.clh_callFailed_network_out_of_order_txt);
                                                                                        break;
                                                                                    case 47:
                                                                                        numValueOf = Integer.valueOf(R.string.clh_callFailed_resources_unavailable_unspecified_txt);
                                                                                        break;
                                                                                    case 55:
                                                                                        numValueOf = Integer.valueOf(R.string.clh_callFailed_incoming_calls_barred_within_the_CUG_txt);
                                                                                        break;
                                                                                    case 63:
                                                                                        numValueOf = Integer.valueOf(R.string.clh_callFailed_service_or_option_not_available_unspecified_txt);
                                                                                        break;
                                                                                    case 65:
                                                                                        numValueOf = Integer.valueOf(R.string.clh_callFailed_bearer_service_not_implemented_txt);
                                                                                        break;
                                                                                    case 79:
                                                                                        numValueOf = Integer.valueOf(R.string.clh_callFailed_service_or_option_not_implemented_unspecified_txt);
                                                                                        break;
                                                                                    case 81:
                                                                                        numValueOf = Integer.valueOf(R.string.clh_callFailed_invalid_transaction_identifier_value_txt);
                                                                                        break;
                                                                                    case 91:
                                                                                        numValueOf = Integer.valueOf(R.string.clh_callFailed_invalid_transit_network_selection_txt);
                                                                                        break;
                                                                                    case 111:
                                                                                        numValueOf = Integer.valueOf(R.string.clh_callFailed_protocol_Error_unspecified_txt);
                                                                                        break;
                                                                                    case 127:
                                                                                        numValueOf = Integer.valueOf(R.string.clh_callFailed_interworking_unspecified_txt);
                                                                                        break;
                                                                                    default:
                                                                                        switch (i2) {
                                                                                            case 17:
                                                                                                numValueOf = Integer.valueOf(R.string.clh_callFailed_powerOff_txt);
                                                                                                break;
                                                                                            case 18:
                                                                                                numValueOf = Integer.valueOf(R.string.clh_incall_error_out_of_service_txt);
                                                                                                break;
                                                                                            case 19:
                                                                                                numValueOf = Integer.valueOf(R.string.clh_callFailed_simError_txt);
                                                                                                break;
                                                                                            default:
                                                                                                numValueOf = Integer.valueOf(R.string.clh_card_title_call_ended_txt);
                                                                                                break;
                                                                                        }
                                                                                        break;
                                                                                }
                                                                                break;
                                                                        }
                                                                        break;
                                                                }
                                                                break;
                                                        }
                                                        break;
                                                }
                                                break;
                                        }
                                        break;
                                }
                                break;
                        }
                        break;
                }
                break;
        }
        return context.getResources().getString(numValueOf.intValue());
    }

    private static CharSequence toTelecomDisconnectCauseDescription(Context context, int i) {
        if (context == null) {
            return "";
        }
        Integer numValueOf = null;
        switch (i) {
            case 17:
                if (ImsUtil.shouldPromoteWfc(context)) {
                    numValueOf = Integer.valueOf(R.string.incall_error_promote_wfc);
                } else if (ImsUtil.isWfcModeWifiOnly(context)) {
                    numValueOf = Integer.valueOf(R.string.incall_error_wfc_only_no_wireless_network);
                } else if (ImsUtil.isWfcEnabled(context)) {
                    numValueOf = Integer.valueOf(R.string.incall_error_power_off_wfc);
                } else if (Settings.Global.getInt(context.getContentResolver(), "airplane_mode_on", 0) > 0) {
                    numValueOf = Integer.valueOf(R.string.incall_error_power_off);
                }
                break;
            case 18:
                if (ImsUtil.shouldPromoteWfc(context)) {
                    numValueOf = Integer.valueOf(R.string.incall_error_promote_wfc);
                } else if (ImsUtil.isWfcModeWifiOnly(context)) {
                    numValueOf = Integer.valueOf(R.string.incall_error_wfc_only_no_wireless_network);
                } else if (ImsUtil.isWfcEnabled(context)) {
                    numValueOf = Integer.valueOf(R.string.incall_error_out_of_service_wfc);
                } else {
                    numValueOf = Integer.valueOf(R.string.incall_error_out_of_service);
                }
                break;
            case 20:
                numValueOf = Integer.valueOf(R.string.callFailed_cb_enabled);
                break;
            case 21:
                numValueOf = Integer.valueOf(R.string.callFailed_fdn_only);
                break;
            case 22:
                numValueOf = Integer.valueOf(R.string.callFailed_dsac_restricted);
                break;
            case 23:
                numValueOf = Integer.valueOf(R.string.callFailed_dsac_restricted_normal);
                break;
            case 24:
                numValueOf = Integer.valueOf(R.string.callFailed_dsac_restricted_emergency);
                break;
            case 34:
                numValueOf = Integer.valueOf(R.string.incall_error_ecm_emergency_only);
                break;
            case 37:
                numValueOf = Integer.valueOf(R.string.incall_error_emergency_only);
                break;
            case 38:
                numValueOf = Integer.valueOf(R.string.incall_error_no_phone_number_supplied);
                break;
            case 40:
                numValueOf = Integer.valueOf(R.string.incall_error_missing_voicemail_number);
                break;
            case 43:
                numValueOf = Integer.valueOf(R.string.incall_error_call_failed);
                break;
            case 46:
                numValueOf = Integer.valueOf(R.string.callFailed_dialToUssd);
                break;
            case 47:
                numValueOf = Integer.valueOf(R.string.callFailed_dialToSs);
                break;
            case 48:
                numValueOf = Integer.valueOf(R.string.callFailed_dialToDial);
                break;
            case 49:
                numValueOf = Integer.valueOf(R.string.callFailed_cdma_activation);
                break;
            case 50:
                numValueOf = Integer.valueOf(R.string.callFailed_video_call_tty_enabled);
                break;
            case 51:
                numValueOf = Integer.valueOf(R.string.callEnded_pulled);
                break;
            case 53:
                numValueOf = Integer.valueOf(R.string.callFailed_maximum_reached);
                break;
            case 54:
                numValueOf = Integer.valueOf(R.string.callFailed_data_disabled);
                break;
            case 55:
                numValueOf = Integer.valueOf(R.string.callFailed_data_limit_reached_description);
                break;
            case 57:
                numValueOf = Integer.valueOf(android.R.string.ext_media_missing_title);
                break;
            case 58:
                numValueOf = Integer.valueOf(R.string.callFailed_imei_not_accepted);
                break;
            case 59:
                numValueOf = Integer.valueOf(R.string.callFailed_wifi_lost);
                break;
            case 61:
                numValueOf = Integer.valueOf(R.string.callFailed_low_battery);
                break;
            case 62:
                numValueOf = Integer.valueOf(R.string.dialFailed_low_battery);
                break;
            case 66:
                numValueOf = Integer.valueOf(R.string.callFailed_dialToDialVideo);
                break;
            case 67:
                numValueOf = Integer.valueOf(R.string.callFailed_dialVideoToSs);
                break;
            case 68:
                numValueOf = Integer.valueOf(R.string.callFailed_dialVideoToUssd);
                break;
            case 69:
                numValueOf = Integer.valueOf(R.string.callFailed_dialVideoToDial);
                break;
            case 70:
                numValueOf = Integer.valueOf(R.string.callFailed_dialVideoToDialVideo);
                break;
            case 71:
                numValueOf = Integer.valueOf(R.string.incall_error_power_off);
                break;
            case 1044:
                numValueOf = Integer.valueOf(R.string.volte_ss_not_available_tips);
                break;
        }
        return numValueOf == null ? "" : context.getResources().getString(numValueOf.intValue());
    }

    private static String toTelecomDisconnectReason(Context context, int i, String str) {
        if (context == null) {
            return "";
        }
        if (i != 60) {
            switch (i) {
                case 17:
                case 18:
                    if (ImsUtil.shouldPromoteWfc(context)) {
                        return "REASON_WIFI_ON_BUT_WFC_OFF";
                    }
                    break;
            }
            String string = android.telephony.DisconnectCause.toString(i);
            if (str == null) {
                return string;
            }
            return str + ", " + string;
        }
        return "REASON_IMS_ACCESS_BLOCKED";
    }

    private static int toTelecomDisconnectCauseTone(int i) {
        if (i == 18) {
            return 95;
        }
        if (i == 25) {
            return 21;
        }
        if (i == 36 || i == 50 || i == 65) {
            return 27;
        }
        switch (i) {
            case 2:
            case 3:
                return 27;
            case 4:
                return 17;
            case 5:
                return 18;
            default:
                switch (i) {
                    case 27:
                        return 95;
                    case 28:
                        return 37;
                    case 29:
                        return 38;
                    default:
                        return -1;
                }
        }
    }
}
