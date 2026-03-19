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

        public VoicemailProvider(String str, Intent intent) {
            this.name = str;
            this.intent = intent;
        }

        public String toString() {
            return "[ Name: " + this.name + ", Intent: " + this.intent + " ]";
        }
    }

    public VoicemailProviderListPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mVmProvidersData = new HashMap();
    }

    public void init(Phone phone, Intent intent) {
        this.mPhone = phone;
        initVoicemailProviders(intent);
    }

    private void initVoicemailProviders(Intent intent) {
        String stringExtra;
        log("initVoicemailProviders()");
        String action = intent.getAction();
        if (!TextUtils.isEmpty(action) && action.equals("com.android.phone.CallFeaturesSetting.ADD_VOICEMAIL") && intent.hasExtra("com.android.phone.ProviderToIgnore")) {
            log("Found ACTION_ADD_VOICEMAIL.");
            stringExtra = intent.getStringExtra("com.android.phone.ProviderToIgnore");
            VoicemailProviderSettingsUtil.delete(this.mPhone.getContext(), stringExtra);
        } else {
            stringExtra = null;
        }
        this.mVmProvidersData.clear();
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        String string = this.mPhone.getContext().getResources().getString(R.string.voicemail_default);
        this.mVmProvidersData.put("", new VoicemailProvider(string, null));
        arrayList.add(string);
        arrayList2.add("");
        PackageManager packageManager = this.mPhone.getContext().getPackageManager();
        List<ResolveInfo> listQueryIntentActivities = packageManager.queryIntentActivities(new Intent("com.android.phone.CallFeaturesSetting.CONFIGURE_VOICEMAIL"), 0);
        for (int i = 0; i < listQueryIntentActivities.size(); i++) {
            ResolveInfo resolveInfo = listQueryIntentActivities.get(i);
            ActivityInfo activityInfo = resolveInfo.activityInfo;
            String str = activityInfo.name;
            if (!str.equals(stringExtra)) {
                log("Loading key: " + str);
                CharSequence charSequenceLoadLabel = resolveInfo.loadLabel(packageManager);
                if (TextUtils.isEmpty(charSequenceLoadLabel)) {
                    Log.w(LOG_TAG, "Adding voicemail provider with no name for display.");
                }
                String string2 = charSequenceLoadLabel != null ? charSequenceLoadLabel.toString() : "";
                Intent intent2 = new Intent();
                intent2.setAction("com.android.phone.CallFeaturesSetting.CONFIGURE_VOICEMAIL");
                intent2.setClassName(activityInfo.packageName, activityInfo.name);
                VoicemailProvider voicemailProvider = new VoicemailProvider(string2, intent2);
                log("Store VoicemailProvider. Key: " + str + " -> " + voicemailProvider.toString());
                this.mVmProvidersData.put(str, voicemailProvider);
                arrayList.add(voicemailProvider.name);
                arrayList2.add(str);
            }
        }
        setEntries((CharSequence[]) arrayList.toArray(new String[0]));
        setEntryValues((CharSequence[]) arrayList2.toArray(new String[0]));
    }

    @Override
    public String getValue() {
        String value = super.getValue();
        return value != null ? value : "";
    }

    public VoicemailProvider getVoicemailProvider(String str) {
        return this.mVmProvidersData.get(str);
    }

    public boolean hasMoreThanOneVoicemailProvider() {
        return this.mVmProvidersData.size() > 1;
    }

    private static void log(String str) {
        Log.d(LOG_TAG, str);
    }
}
