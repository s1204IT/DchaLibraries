package com.android.settings.applications.defaultapps;

import android.content.Context;
import android.content.pm.PackageManager;
import android.telecom.DefaultDialerManager;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import com.android.settings.R;
import com.android.settingslib.applications.DefaultAppInfo;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/* loaded from: classes.dex */
public class DefaultPhonePicker extends DefaultAppPickerFragment {
    private DefaultKeyUpdater mDefaultKeyUpdater;

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 788;
    }

    @Override // com.android.settings.applications.defaultapps.DefaultAppPickerFragment, com.android.settings.widget.RadioButtonPickerFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onAttach(Context context) {
        super.onAttach(context);
        this.mDefaultKeyUpdater = new DefaultKeyUpdater((TelecomManager) context.getSystemService("telecom"));
    }

    @Override // com.android.settings.widget.RadioButtonPickerFragment, com.android.settings.core.InstrumentedPreferenceFragment
    protected int getPreferenceScreenResId() {
        return R.xml.default_phone_settings;
    }

    @Override // com.android.settings.widget.RadioButtonPickerFragment
    protected List<DefaultAppInfo> getCandidates() {
        ArrayList arrayList = new ArrayList();
        List installedDialerApplications = DefaultDialerManager.getInstalledDialerApplications(getContext(), this.mUserId);
        Context context = getContext();
        Iterator it = installedDialerApplications.iterator();
        while (it.hasNext()) {
            try {
                arrayList.add(new DefaultAppInfo(context, this.mPm, this.mPm.getApplicationInfoAsUser((String) it.next(), 0, this.mUserId)));
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
        return arrayList;
    }

    @Override // com.android.settings.widget.RadioButtonPickerFragment
    protected String getDefaultKey() {
        return this.mDefaultKeyUpdater.getDefaultDialerApplication(getContext(), this.mUserId);
    }

    @Override // com.android.settings.widget.RadioButtonPickerFragment
    protected String getSystemDefaultKey() {
        return this.mDefaultKeyUpdater.getSystemDialerPackage();
    }

    @Override // com.android.settings.widget.RadioButtonPickerFragment
    protected boolean setDefaultKey(String str) {
        if (!TextUtils.isEmpty(str) && !TextUtils.equals(str, getDefaultKey())) {
            return this.mDefaultKeyUpdater.setDefaultDialerApplication(getContext(), str, this.mUserId);
        }
        return false;
    }

    static class DefaultKeyUpdater {
        private final TelecomManager mTelecomManager;

        public DefaultKeyUpdater(TelecomManager telecomManager) {
            this.mTelecomManager = telecomManager;
        }

        public String getSystemDialerPackage() {
            return this.mTelecomManager.getSystemDialerPackage();
        }

        public String getDefaultDialerApplication(Context context, int i) {
            return DefaultDialerManager.getDefaultDialerApplication(context, i);
        }

        public boolean setDefaultDialerApplication(Context context, String str, int i) {
            return DefaultDialerManager.setDefaultDialerApplication(context, str, i);
        }
    }
}
