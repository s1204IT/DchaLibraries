package com.android.providers.telephony;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.Telephony;
import android.telephony.ServiceState;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.SubscriptionController;
import java.util.HashMap;

public class ServiceStateProvider extends ContentProvider {
    public static final Uri AUTHORITY_URI = Uri.parse("content://service-state");
    private static final String[] sColumns = {"voice_reg_state", "data_reg_state", "voice_roaming_type", "data_roaming_type", "voice_operator_alpha_long", "voice_operator_alpha_short", "voice_operator_numeric", "data_operator_alpha_long", "data_operator_alpha_short", "data_operator_numeric", "is_manual_network_selection", "ril_voice_radio_technology", "ril_data_radio_technology", "css_indicator", "network_id", "system_id", "cdma_roaming_indicator", "cdma_default_roaming_indicator", "cdma_eri_icon_index", "cdma_eri_icon_mode", "is_emergency_only", "is_data_roaming_from_registration", "is_using_carrier_aggregation"};
    private final HashMap<Integer, ServiceState> mServiceStates = new HashMap<>();

    @Override
    public boolean onCreate() {
        return true;
    }

    @VisibleForTesting
    public ServiceState getServiceState(int i) {
        return this.mServiceStates.get(Integer.valueOf(i));
    }

    @VisibleForTesting
    public int getDefaultSubId() {
        return SubscriptionController.getInstance().getDefaultSubId();
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        if (uri.isPathPrefixMatch(Telephony.ServiceStateTable.CONTENT_URI)) {
            try {
                int defaultSubId = Integer.parseInt(uri.getLastPathSegment());
                Log.d("ServiceStateProvider", "subId=" + defaultSubId);
                if (defaultSubId == Integer.MAX_VALUE) {
                    defaultSubId = getDefaultSubId();
                }
                ServiceState serviceState = new ServiceState();
                serviceState.setVoiceRegState(contentValues.getAsInteger("voice_reg_state").intValue());
                serviceState.setDataRegState(contentValues.getAsInteger("data_reg_state").intValue());
                serviceState.setVoiceOperatorName(contentValues.getAsString("voice_operator_alpha_long"), contentValues.getAsString("voice_operator_alpha_short"), contentValues.getAsString("voice_operator_numeric"));
                serviceState.setDataOperatorName(contentValues.getAsString("data_operator_alpha_long"), contentValues.getAsString("data_operator_alpha_short"), contentValues.getAsString("data_operator_numeric"));
                serviceState.setIsManualSelection(contentValues.getAsBoolean("is_manual_network_selection").booleanValue());
                serviceState.setRilVoiceRadioTechnology(contentValues.getAsInteger("ril_voice_radio_technology").intValue());
                serviceState.setRilDataRadioTechnology(contentValues.getAsInteger("ril_data_radio_technology").intValue());
                serviceState.setCssIndicator(contentValues.getAsInteger("css_indicator").intValue());
                serviceState.setCdmaSystemAndNetworkId(contentValues.getAsInteger("system_id").intValue(), contentValues.getAsInteger("network_id").intValue());
                serviceState.setCdmaRoamingIndicator(contentValues.getAsInteger("cdma_roaming_indicator").intValue());
                serviceState.setCdmaDefaultRoamingIndicator(contentValues.getAsInteger("cdma_default_roaming_indicator").intValue());
                serviceState.setCdmaEriIconIndex(contentValues.getAsInteger("cdma_eri_icon_index").intValue());
                serviceState.setCdmaEriIconMode(contentValues.getAsInteger("cdma_eri_icon_mode").intValue());
                serviceState.setEmergencyOnly(contentValues.getAsBoolean("is_emergency_only").booleanValue());
                serviceState.setDataRoamingFromRegistration(contentValues.getAsBoolean("is_data_roaming_from_registration").booleanValue());
                serviceState.setIsUsingCarrierAggregation(contentValues.getAsBoolean("is_using_carrier_aggregation").booleanValue());
                ServiceState serviceState2 = getServiceState(defaultSubId);
                notifyChangeForSubIdAndField(getContext(), serviceState2, serviceState, defaultSubId);
                notifyChangeForSubId(getContext(), serviceState2, serviceState, defaultSubId);
                this.mServiceStates.put(Integer.valueOf(defaultSubId), serviceState);
                return uri;
            } catch (NumberFormatException e) {
                Log.e("ServiceStateProvider", "insert: no subId provided in uri");
                throw e;
            }
        }
        return null;
    }

    @Override
    public int delete(Uri uri, String str, String[] strArr) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public String getType(Uri uri) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        int defaultSubId;
        if (!uri.isPathPrefixMatch(Telephony.ServiceStateTable.CONTENT_URI)) {
            throw new IllegalArgumentException("Invalid URI: " + uri);
        }
        try {
            defaultSubId = Integer.parseInt(uri.getLastPathSegment());
        } catch (NumberFormatException e) {
            Log.d("ServiceStateProvider", "query: no subId provided in uri, using default.");
            defaultSubId = getDefaultSubId();
        }
        Log.d("ServiceStateProvider", "subId=" + defaultSubId);
        if (defaultSubId == Integer.MAX_VALUE) {
            defaultSubId = getDefaultSubId();
        }
        ServiceState serviceState = getServiceState(defaultSubId);
        if (serviceState == null) {
            Log.d("ServiceStateProvider", "returning null");
            return null;
        }
        return buildSingleRowResult(strArr, sColumns, new Object[]{Integer.valueOf(serviceState.getVoiceRegState()), Integer.valueOf(serviceState.getDataRegState()), Integer.valueOf(serviceState.getVoiceRoamingType()), Integer.valueOf(serviceState.getDataRoamingType()), serviceState.getVoiceOperatorAlphaLong(), serviceState.getVoiceOperatorAlphaShort(), serviceState.getVoiceOperatorNumeric(), serviceState.getDataOperatorAlphaLong(), serviceState.getDataOperatorAlphaShort(), serviceState.getDataOperatorNumeric(), Integer.valueOf(serviceState.getIsManualSelection() ? 1 : 0), Integer.valueOf(serviceState.getRilVoiceRadioTechnology()), Integer.valueOf(serviceState.getRilDataRadioTechnology()), Integer.valueOf(serviceState.getCssIndicator()), Integer.valueOf(serviceState.getCdmaNetworkId()), Integer.valueOf(serviceState.getCdmaSystemId()), Integer.valueOf(serviceState.getCdmaRoamingIndicator()), Integer.valueOf(serviceState.getCdmaDefaultRoamingIndicator()), Integer.valueOf(serviceState.getCdmaEriIconIndex()), Integer.valueOf(serviceState.getCdmaEriIconMode()), Integer.valueOf(serviceState.isEmergencyOnly() ? 1 : 0), Integer.valueOf(serviceState.getDataRoamingFromRegistration() ? 1 : 0), Integer.valueOf(serviceState.isUsingCarrierAggregation() ? 1 : 0)});
    }

    private static Cursor buildSingleRowResult(String[] strArr, String[] strArr2, Object[] objArr) {
        boolean z;
        if (strArr == null) {
            strArr = strArr2;
        }
        MatrixCursor matrixCursor = new MatrixCursor(strArr, 1);
        MatrixCursor.RowBuilder rowBuilderNewRow = matrixCursor.newRow();
        for (int i = 0; i < matrixCursor.getColumnCount(); i++) {
            String columnName = matrixCursor.getColumnName(i);
            int i2 = 0;
            while (true) {
                if (i2 < strArr2.length) {
                    if (!strArr2[i2].equals(columnName)) {
                        i2++;
                    } else {
                        rowBuilderNewRow.add(objArr[i2]);
                        z = true;
                        break;
                    }
                } else {
                    z = false;
                    break;
                }
            }
            if (!z) {
                throw new IllegalArgumentException("Invalid column " + strArr[i]);
            }
        }
        return matrixCursor;
    }

    @VisibleForTesting
    public static void notifyChangeForSubIdAndField(Context context, ServiceState serviceState, ServiceState serviceState2, int i) {
        boolean z = serviceState == null;
        if (z || voiceRegStateChanged(serviceState, serviceState2)) {
            context.getContentResolver().notifyChange(Telephony.ServiceStateTable.getUriForSubscriptionIdAndField(i, "voice_reg_state"), (ContentObserver) null, false);
        }
        if (z || dataRegStateChanged(serviceState, serviceState2)) {
            context.getContentResolver().notifyChange(Telephony.ServiceStateTable.getUriForSubscriptionIdAndField(i, "data_reg_state"), (ContentObserver) null, false);
        }
        if (z || voiceRoamingTypeChanged(serviceState, serviceState2)) {
            context.getContentResolver().notifyChange(Telephony.ServiceStateTable.getUriForSubscriptionIdAndField(i, "voice_roaming_type"), (ContentObserver) null, false);
        }
        if (z || dataRoamingTypeChanged(serviceState, serviceState2)) {
            context.getContentResolver().notifyChange(Telephony.ServiceStateTable.getUriForSubscriptionIdAndField(i, "data_roaming_type"), (ContentObserver) null, false);
        }
    }

    private static boolean voiceRegStateChanged(ServiceState serviceState, ServiceState serviceState2) {
        return serviceState.getVoiceRegState() != serviceState2.getVoiceRegState();
    }

    private static boolean dataRegStateChanged(ServiceState serviceState, ServiceState serviceState2) {
        return serviceState.getDataRegState() != serviceState2.getDataRegState();
    }

    private static boolean voiceRoamingTypeChanged(ServiceState serviceState, ServiceState serviceState2) {
        return serviceState.getVoiceRoamingType() != serviceState2.getVoiceRoamingType();
    }

    private static boolean dataRoamingTypeChanged(ServiceState serviceState, ServiceState serviceState2) {
        return serviceState.getDataRoamingType() != serviceState2.getDataRoamingType();
    }

    @VisibleForTesting
    public static void notifyChangeForSubId(Context context, ServiceState serviceState, ServiceState serviceState2, int i) {
        if (serviceState == null || voiceRegStateChanged(serviceState, serviceState2) || dataRegStateChanged(serviceState, serviceState2) || voiceRoamingTypeChanged(serviceState, serviceState2) || dataRoamingTypeChanged(serviceState, serviceState2)) {
            context.getContentResolver().notifyChange(Telephony.ServiceStateTable.getUriForSubscriptionId(i), (ContentObserver) null, false);
        }
    }
}
