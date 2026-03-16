package com.android.bluetooth.btservice;

import android.content.Context;
import android.content.res.Resources;
import android.os.SystemProperties;
import android.util.Log;
import com.android.bluetooth.R;
import com.android.bluetooth.a2dp.A2dpService;
import com.android.bluetooth.a2dp.A2dpSinkService;
import com.android.bluetooth.avrcp.AvrcpControllerService;
import com.android.bluetooth.gatt.GattService;
import com.android.bluetooth.hdp.HealthService;
import com.android.bluetooth.hfp.HeadsetService;
import com.android.bluetooth.hfpclient.HeadsetClientService;
import com.android.bluetooth.hid.HidService;
import com.android.bluetooth.map.BluetoothMapService;
import com.android.bluetooth.pan.PanService;
import java.util.ArrayList;

public class Config {
    private static final String TAG = "AdapterServiceConfig";
    private static final Class[] PROFILE_SERVICES = {HeadsetService.class, A2dpService.class, A2dpSinkService.class, HidService.class, HealthService.class, PanService.class, GattService.class, BluetoothMapService.class, HeadsetClientService.class, AvrcpControllerService.class};
    private static final int[] PROFILE_SERVICES_FLAG = {R.bool.profile_supported_hs_hfp, R.bool.profile_supported_a2dp, R.bool.profile_supported_a2dp_sink, R.bool.profile_supported_hid, R.bool.profile_supported_hdp, R.bool.profile_supported_pan, R.bool.profile_supported_gatt, R.bool.profile_supported_map, R.bool.profile_supported_hfpclient, R.bool.profile_supported_avrcp_controller};
    private static final int[] AUTO_PROFILE_SERVICES_FLAG = {R.bool.auto_profile_supported_hs_hfp, R.bool.auto_profile_supported_a2dp, R.bool.auto_profile_supported_a2dp_sink, R.bool.auto_profile_supported_hid, R.bool.auto_profile_supported_hdp, R.bool.auto_profile_supported_pan, R.bool.auto_profile_supported_gatt, R.bool.auto_profile_supported_map, R.bool.auto_profile_supported_hfpclient, R.bool.auto_profile_supported_avrcp_controller};
    private static Class[] SUPPORTED_PROFILES = new Class[0];

    static void init(Context ctx) {
        Resources resources;
        if (ctx != null && (resources = ctx.getResources()) != null) {
            String role = SystemProperties.get("persist.bt.role", "0");
            Log.d(TAG, "role is " + role);
            ArrayList<Class> profiles = new ArrayList<>(PROFILE_SERVICES.length);
            if (role.equals("1")) {
                for (int i = 0; i < AUTO_PROFILE_SERVICES_FLAG.length; i++) {
                    boolean supported = resources.getBoolean(AUTO_PROFILE_SERVICES_FLAG[i]);
                    if (supported) {
                        Log.d(TAG, "Adding " + PROFILE_SERVICES[i].getSimpleName());
                        profiles.add(PROFILE_SERVICES[i]);
                    }
                }
            } else {
                for (int i2 = 0; i2 < PROFILE_SERVICES_FLAG.length; i2++) {
                    boolean supported2 = resources.getBoolean(PROFILE_SERVICES_FLAG[i2]);
                    if (supported2) {
                        Log.d(TAG, "Adding " + PROFILE_SERVICES[i2].getSimpleName());
                        profiles.add(PROFILE_SERVICES[i2]);
                    }
                }
            }
            int totalProfiles = profiles.size();
            SUPPORTED_PROFILES = new Class[totalProfiles];
            profiles.toArray(SUPPORTED_PROFILES);
        }
    }

    static Class[] getSupportedProfiles() {
        return SUPPORTED_PROFILES;
    }
}
