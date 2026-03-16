package com.android.phone.settings;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.preference.ListPreference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import com.android.internal.telephony.Phone;
import com.android.phone.R;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VoicemailProviderListPreference extends ListPreference {
    private static final String LOG_TAG = VoicemailProviderListPreference.class.getSimpleName();
    private Phone mPhone;
    private final Map<String, VoicemailProvider> mVmProvidersData;

    public class VoicemailProvider {
        public Intent intent;
        public String name;

        public VoicemailProvider(String name, Intent intent) {
            this.name = name;
            this.intent = intent;
        }

        public String toString() {
            return "[ Name: " + this.name + ", Intent: " + this.intent + " ]";
        }
    }

    public VoicemailProviderListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mVmProvidersData = new HashMap();
    }

    public void init(Phone phone, Intent intent) {
        this.mPhone = phone;
        initVoicemailProviders(intent);
    }

    private void initVoicemailProviders(Intent activityIntent) {
        log("initVoicemailProviders()");
        String providerToIgnore = null;
        if (activityIntent.getAction().equals("com.android.phone.CallFeaturesSetting.ADD_VOICEMAIL") && activityIntent.hasExtra("com.android.phone.ProviderToIgnore")) {
            log("Found ACTION_ADD_VOICEMAIL.");
            providerToIgnore = activityIntent.getStringExtra("com.android.phone.ProviderToIgnore");
            VoicemailProviderSettingsUtil.delete(this.mPhone.getContext(), providerToIgnore);
        }
        this.mVmProvidersData.clear();
        List<String> entries = new ArrayList<>();
        List<String> values = new ArrayList<>();
        String myCarrier = this.mPhone.getContext().getResources().getString(R.string.voicemail_default);
        this.mVmProvidersData.put("", new VoicemailProvider(myCarrier, null));
        entries.add(myCarrier);
        values.add("");
        PackageManager pm = this.mPhone.getContext().getPackageManager();
        Intent intent = new Intent("com.android.phone.CallFeaturesSetting.CONFIGURE_VOICEMAIL");
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, 0);
        for (int i = 0; i < resolveInfos.size(); i++) {
            ResolveInfo ri = resolveInfos.get(i);
            ActivityInfo currentActivityInfo = ri.activityInfo;
            String key = currentActivityInfo.name;
            if (!key.equals(providerToIgnore)) {
                log("Loading key: " + key);
                CharSequence label = ri.loadLabel(pm);
                if (TextUtils.isEmpty(label)) {
                    Log.w(LOG_TAG, "Adding voicemail provider with no name for display.");
                }
                String nameForDisplay = label != null ? label.toString() : "";
                Intent providerIntent = new Intent();
                providerIntent.setAction("com.android.phone.CallFeaturesSetting.CONFIGURE_VOICEMAIL");
                providerIntent.setClassName(currentActivityInfo.packageName, currentActivityInfo.name);
                VoicemailProvider vmProvider = new VoicemailProvider(nameForDisplay, providerIntent);
                log("Store VoicemailProvider. Key: " + key + " -> " + vmProvider.toString());
                this.mVmProvidersData.put(key, vmProvider);
                entries.add(vmProvider.name);
                values.add(key);
            }
        }
        setEntries((CharSequence[]) entries.toArray(new String[0]));
        setEntryValues((CharSequence[]) values.toArray(new String[0]));
    }

    @Override
    public String getValue() {
        String providerKey = super.getValue();
        return providerKey != null ? providerKey : "";
    }

    public VoicemailProvider getVoicemailProvider(String key) {
        return this.mVmProvidersData.get(key);
    }

    public boolean hasMoreThanOneVoicemailProvider() {
        return this.mVmProvidersData.size() > 1;
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
