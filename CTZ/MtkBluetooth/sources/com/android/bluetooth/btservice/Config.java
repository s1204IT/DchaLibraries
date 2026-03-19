package com.android.bluetooth.btservice;

import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import com.android.bluetooth.R;
import com.android.bluetooth.a2dp.A2dpService;
import com.android.bluetooth.a2dpsink.A2dpSinkService;
import com.android.bluetooth.avrcp.AvrcpTargetService;
import com.android.bluetooth.avrcpcontroller.AvrcpControllerService;
import com.android.bluetooth.gatt.GattService;
import com.android.bluetooth.hdp.HealthService;
import com.android.bluetooth.hearingaid.HearingAidService;
import com.android.bluetooth.hfp.HeadsetService;
import com.android.bluetooth.hfpclient.HeadsetClientService;
import com.android.bluetooth.hid.HidDeviceService;
import com.android.bluetooth.hid.HidHostService;
import com.android.bluetooth.map.BluetoothMapService;
import com.android.bluetooth.mapclient.MapClientService;
import com.android.bluetooth.mesh.MeshService;
import com.android.bluetooth.opp.BluetoothOppService;
import com.android.bluetooth.pan.PanService;
import com.android.bluetooth.pbap.BluetoothPbapService;
import com.android.bluetooth.pbapclient.PbapClientService;
import com.android.bluetooth.sap.SapService;
import java.util.ArrayList;

public class Config {
    private static final String TAG = "AdapterServiceConfig";
    private static final ProfileConfig[] PROFILE_SERVICES_AND_FLAGS = {new ProfileConfig(HeadsetService.class, R.bool.profile_supported_hs_hfp, 2), new ProfileConfig(A2dpService.class, R.bool.profile_supported_a2dp, 4), new ProfileConfig(A2dpSinkService.class, R.bool.profile_supported_a2dp_sink, PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH), new ProfileConfig(HidHostService.class, R.bool.profile_supported_hid_host, 16), new ProfileConfig(HealthService.class, R.bool.profile_supported_hdp, 8), new ProfileConfig(PanService.class, R.bool.profile_supported_pan, 32), new ProfileConfig(GattService.class, R.bool.profile_supported_gatt, 128), new ProfileConfig(BluetoothMapService.class, R.bool.profile_supported_map, 512), new ProfileConfig(HeadsetClientService.class, R.bool.profile_supported_hfpclient, PlaybackStateCompat.ACTION_PREPARE_FROM_SEARCH), new ProfileConfig(AvrcpTargetService.class, R.bool.profile_supported_avrcp_target, PlaybackStateCompat.ACTION_PLAY_FROM_URI), new ProfileConfig(AvrcpControllerService.class, R.bool.profile_supported_avrcp_controller, PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM), new ProfileConfig(SapService.class, R.bool.profile_supported_sap, PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID), new ProfileConfig(PbapClientService.class, R.bool.profile_supported_pbapclient, PlaybackStateCompat.ACTION_PREPARE_FROM_URI), new ProfileConfig(MapClientService.class, R.bool.profile_supported_mapmce, PlaybackStateCompat.ACTION_SET_REPEAT_MODE), new ProfileConfig(HidDeviceService.class, R.bool.profile_supported_hid_device, PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE_ENABLED), new ProfileConfig(BluetoothOppService.class, R.bool.profile_supported_opp, PlaybackStateCompat.ACTION_SET_CAPTIONING_ENABLED), new ProfileConfig(BluetoothPbapService.class, R.bool.profile_supported_pbap, 64), new ProfileConfig(HearingAidService.class, R.bool.profile_supported_hearing_aid, PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE), new ProfileConfig(MeshService.class, R.bool.profile_supported_mesh, 4194304)};
    private static Class[] sSupportedProfiles = new Class[0];

    private static class ProfileConfig {
        Class mClass;
        long mMask;
        int mSupported;

        ProfileConfig(Class cls, int i, long j) {
            this.mClass = cls;
            this.mSupported = i;
            this.mMask = j;
        }
    }

    static void init(Context context) {
        Resources resources;
        if (context == null || (resources = context.getResources()) == null) {
            return;
        }
        ArrayList arrayList = new ArrayList(PROFILE_SERVICES_AND_FLAGS.length);
        for (ProfileConfig profileConfig : PROFILE_SERVICES_AND_FLAGS) {
            if (resources.getBoolean(profileConfig.mSupported) && !isProfileDisabled(context, profileConfig.mMask)) {
                Log.v(TAG, "Adding " + profileConfig.mClass.getSimpleName());
                arrayList.add(profileConfig.mClass);
            }
            sSupportedProfiles = (Class[]) arrayList.toArray(new Class[arrayList.size()]);
        }
    }

    static Class[] getSupportedProfiles() {
        return sSupportedProfiles;
    }

    private static long getProfileMask(Class cls) {
        for (ProfileConfig profileConfig : PROFILE_SERVICES_AND_FLAGS) {
            if (profileConfig.mClass == cls) {
                return profileConfig.mMask;
            }
        }
        Log.w(TAG, "Could not find profile bit mask for " + cls.getSimpleName());
        return 0L;
    }

    static long getSupportedProfilesBitMask() {
        long profileMask = 0;
        for (Class cls : getSupportedProfiles()) {
            profileMask |= getProfileMask(cls);
        }
        return profileMask;
    }

    private static boolean isProfileDisabled(Context context, long j) {
        return (Settings.Global.getLong(context.getContentResolver(), "bluetooth_disabled_profiles", 0L) & j) != 0;
    }
}
