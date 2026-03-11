package com.mediatek.settings.wifi;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import com.android.settingslib.core.AbstractPreferenceController;

public class WapiCertPreferenceController extends AbstractPreferenceController {
    private Context mContext;

    public WapiCertPreferenceController(Context context) {
        super(context);
        this.mContext = context;
    }

    @Override
    public boolean isAvailable() {
        return isWapiCertPackageExist();
    }

    @Override
    public String getPreferenceKey() {
        return "wapi_cert_manage";
    }

    private boolean isWapiCertPackageExist() {
        if (this.mContext != null) {
            try {
                this.mContext.getPackageManager().getActivityInfo(new ComponentName("com.wapi.wapicertmanager", "com.wapi.wapicertmanager.WapiCertManagerActivity"), 0);
            } catch (PackageManager.NameNotFoundException e) {
                Log.d("WapiCertPreferenceController", "package exist: false");
                return false;
            }
        }
        Log.d("WapiCertPreferenceController", "package exist: true");
        return true;
    }
}
