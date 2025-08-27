package com.android.settings.development.qstile;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import com.android.internal.statusbar.IStatusBarService;
import com.android.settingslib.core.AbstractPreferenceController;
import java.util.Iterator;

/* loaded from: classes.dex */
public class DevelopmentTilePreferenceController extends AbstractPreferenceController {
    private final OnChangeHandler mOnChangeHandler;
    private final PackageManager mPackageManager;

    public DevelopmentTilePreferenceController(Context context) {
        super(context);
        this.mOnChangeHandler = new OnChangeHandler(context);
        this.mPackageManager = context.getPackageManager();
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return null;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        Context context = preferenceScreen.getContext();
        Iterator<ResolveInfo> it = this.mPackageManager.queryIntentServices(new Intent("android.service.quicksettings.action.QS_TILE").setPackage(context.getPackageName()), 512).iterator();
        while (it.hasNext()) {
            ServiceInfo serviceInfo = it.next().serviceInfo;
            int componentEnabledSetting = this.mPackageManager.getComponentEnabledSetting(new ComponentName(serviceInfo.packageName, serviceInfo.name));
            boolean z = true;
            if (componentEnabledSetting != 1 && (componentEnabledSetting != 0 || !serviceInfo.enabled)) {
                z = false;
            }
            SwitchPreference switchPreference = new SwitchPreference(context);
            switchPreference.setTitle(serviceInfo.loadLabel(this.mPackageManager));
            switchPreference.setIcon(serviceInfo.icon);
            switchPreference.setKey(serviceInfo.name);
            switchPreference.setChecked(z);
            switchPreference.setOnPreferenceChangeListener(this.mOnChangeHandler);
            preferenceScreen.addPreference(switchPreference);
        }
    }

    static class OnChangeHandler implements Preference.OnPreferenceChangeListener {
        private final Context mContext;
        private final PackageManager mPackageManager;
        private IStatusBarService mStatusBarService = IStatusBarService.Stub.asInterface(ServiceManager.checkService("statusbar"));

        public OnChangeHandler(Context context) {
            this.mContext = context;
            this.mPackageManager = context.getPackageManager();
        }

        @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
        public boolean onPreferenceChange(Preference preference, Object obj) {
            boolean zBooleanValue = ((Boolean) obj).booleanValue();
            ComponentName componentName = new ComponentName(this.mContext.getPackageName(), preference.getKey());
            this.mPackageManager.setComponentEnabledSetting(componentName, zBooleanValue ? 1 : 2, 1);
            try {
                if (this.mStatusBarService != null) {
                    if (zBooleanValue) {
                        this.mStatusBarService.addTile(componentName);
                    } else {
                        this.mStatusBarService.remTile(componentName);
                    }
                }
            } catch (RemoteException e) {
                Log.e("DevTilePrefController", "Failed to modify QS tile for component " + componentName.toString(), e);
            }
            return true;
        }
    }
}
