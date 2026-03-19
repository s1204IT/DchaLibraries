package com.android.bluetooth.pbap;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import com.android.bluetooth.R;

public class BluetoothPbapConfig {
    private static boolean sUseProfileForOwnerVcard = true;
    private static boolean sIncludePhotosInVcard = false;

    public static void init(Context context) {
        Resources resources = context.getResources();
        if (resources != null) {
            try {
                sUseProfileForOwnerVcard = resources.getBoolean(R.bool.pbap_use_profile_for_owner_vcard);
            } catch (Exception e) {
                Log.e("BluetoothPbapConfig", "", e);
            }
            try {
                sIncludePhotosInVcard = resources.getBoolean(R.bool.pbap_include_photos_in_vcard);
            } catch (Exception e2) {
                Log.e("BluetoothPbapConfig", "", e2);
            }
        }
    }

    public static boolean useProfileForOwnerVcard() {
        return sUseProfileForOwnerVcard;
    }

    public static boolean includePhotosInVcard() {
        return sIncludePhotosInVcard;
    }
}
