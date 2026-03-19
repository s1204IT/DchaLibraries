package com.android.phone;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Binder;
import android.os.UserManager;
import android.provider.SearchIndexableResource;
import android.provider.SearchIndexablesContract;
import android.provider.SearchIndexablesProvider;
import com.android.phone.MobileNetworkSettings;

public class PhoneSearchIndexablesProvider extends SearchIndexablesProvider {
    private static SearchIndexableResource[] INDEXABLE_RES = {new SearchIndexableResource(1, R.xml.network_setting_fragment, MobileNetworkSettings.class.getName(), R.mipmap.ic_launcher_phone)};
    private static final String TAG = "PhoneSearchIndexablesProvider";
    private UserManager mUserManager;

    public boolean onCreate() {
        this.mUserManager = (UserManager) getContext().getSystemService("user");
        return true;
    }

    public Cursor queryXmlResources(String[] strArr) {
        MatrixCursor matrixCursor = new MatrixCursor(SearchIndexablesContract.INDEXABLES_XML_RES_COLUMNS);
        int length = INDEXABLE_RES.length;
        for (int i = 0; i < length; i++) {
            matrixCursor.addRow(new Object[]{Integer.valueOf(INDEXABLE_RES[i].rank), Integer.valueOf(INDEXABLE_RES[i].xmlResId), null, Integer.valueOf(INDEXABLE_RES[i].iconResId), "android.intent.action.MAIN", "com.android.phone", INDEXABLE_RES[i].className});
        }
        return matrixCursor;
    }

    public Cursor queryRawData(String[] strArr) {
        MatrixCursor matrixCursor = new MatrixCursor(SearchIndexablesContract.INDEXABLES_RAW_COLUMNS);
        Context context = getContext();
        String string = context.getString(R.string.carrier_settings_euicc);
        matrixCursor.newRow().add("rank", 0).add("title", string).add("keywords", context.getString(R.string.keywords_carrier_settings_euicc)).add("screenTitle", string).add("key", "esim_list_profile").add("intentAction", "android.telephony.euicc.action.MANAGE_EMBEDDED_SUBSCRIPTIONS").add("intentTargetPackage", context.getPackageName());
        return matrixCursor;
    }

    public Cursor queryNonIndexableKeys(String[] strArr) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            MatrixCursor matrixCursor = new MatrixCursor(SearchIndexablesContract.NON_INDEXABLES_KEYS_COLUMNS);
            if (!this.mUserManager.isAdminUser()) {
                for (String str : new String[]{MobileNetworkSettings.MobileNetworkFragment.BUTTON_PREFERED_NETWORK_MODE, "button_roaming_key", "cdma_lte_data_service_key", "enhanced_4g_lte", "button_apn_key", "button_carrier_sel_key", "carrier_settings_key", "cdma_system_select_key", "esim_list_profile", "mobile_data_enable", "data_usage_summary", "wifi_calling_key", "video_calling_key"}) {
                    matrixCursor.addRow(createNonIndexableRow(str));
                }
            } else {
                if (isEuiccSettingsHidden()) {
                    matrixCursor.addRow(createNonIndexableRow("esim_list_profile"));
                }
                if (isEnhanced4gLteHidden()) {
                    matrixCursor.addRow(createNonIndexableRow("enhanced_4g_lte"));
                }
            }
            matrixCursor.addRow(createNonIndexableRow(MobileNetworkSettings.MobileNetworkFragment.BUTTON_ENABLED_NETWORKS_KEY));
            matrixCursor.addRow(createNonIndexableRow("carrier_settings_euicc_key"));
            matrixCursor.addRow(createNonIndexableRow("advanced_options"));
            return matrixCursor;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    boolean isEuiccSettingsHidden() {
        return !MobileNetworkSettings.showEuiccSettings(getContext());
    }

    boolean isEnhanced4gLteHidden() {
        return MobileNetworkSettings.hideEnhanced4gLteSettings(getContext());
    }

    private Object[] createNonIndexableRow(String str) {
        Object[] objArr = new Object[SearchIndexablesContract.NON_INDEXABLES_KEYS_COLUMNS.length];
        objArr[0] = str;
        return objArr;
    }
}
