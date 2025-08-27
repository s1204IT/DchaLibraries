package com.android.settings.applications.assist;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.text.TextUtils;
import com.android.internal.app.AssistUtils;
import com.android.settings.R;
import com.android.settings.applications.assist.VoiceInputHelper;
import com.android.settings.applications.defaultapps.DefaultAppPickerFragment;
import com.android.settingslib.applications.DefaultAppInfo;
import com.android.settingslib.wrapper.PackageManagerWrapper;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/* loaded from: classes.dex */
public class DefaultVoiceInputPicker extends DefaultAppPickerFragment {
    private String mAssistRestrict;
    private AssistUtils mAssistUtils;
    private VoiceInputHelper mHelper;

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 844;
    }

    @Override // com.android.settings.applications.defaultapps.DefaultAppPickerFragment, com.android.settings.widget.RadioButtonPickerFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onAttach(Context context) throws Throwable {
        super.onAttach(context);
        this.mAssistUtils = new AssistUtils(context);
        this.mHelper = new VoiceInputHelper(context);
        this.mHelper.buildUi();
        ComponentName currentAssist = getCurrentAssist();
        if (isCurrentAssistVoiceService(currentAssist, getCurrentService(this.mHelper))) {
            this.mAssistRestrict = currentAssist.flattenToShortString();
        }
    }

    @Override // com.android.settings.widget.RadioButtonPickerFragment, com.android.settings.core.InstrumentedPreferenceFragment
    protected int getPreferenceScreenResId() {
        return R.xml.default_voice_settings;
    }

    @Override // com.android.settings.widget.RadioButtonPickerFragment
    protected List<VoiceInputDefaultAppInfo> getCandidates() {
        ArrayList arrayList = new ArrayList();
        Context context = getContext();
        Iterator<VoiceInputHelper.InteractionInfo> it = this.mHelper.mAvailableInteractionInfos.iterator();
        boolean z = true;
        while (it.hasNext()) {
            VoiceInputHelper.InteractionInfo next = it.next();
            boolean zEquals = TextUtils.equals(next.key, this.mAssistRestrict);
            arrayList.add(new VoiceInputDefaultAppInfo(context, this.mPm, this.mUserId, next, zEquals));
            z |= zEquals;
        }
        boolean z2 = !z;
        Iterator<VoiceInputHelper.RecognizerInfo> it2 = this.mHelper.mAvailableRecognizerInfos.iterator();
        while (it2.hasNext()) {
            arrayList.add(new VoiceInputDefaultAppInfo(context, this.mPm, this.mUserId, it2.next(), !z2));
        }
        return arrayList;
    }

    @Override // com.android.settings.widget.RadioButtonPickerFragment
    protected String getDefaultKey() {
        ComponentName currentService = getCurrentService(this.mHelper);
        if (currentService == null) {
            return null;
        }
        return currentService.flattenToShortString();
    }

    @Override // com.android.settings.widget.RadioButtonPickerFragment
    protected boolean setDefaultKey(String str) {
        Iterator<VoiceInputHelper.InteractionInfo> it = this.mHelper.mAvailableInteractionInfos.iterator();
        while (it.hasNext()) {
            VoiceInputHelper.InteractionInfo next = it.next();
            if (TextUtils.equals(str, next.key)) {
                Settings.Secure.putString(getContext().getContentResolver(), "voice_interaction_service", str);
                Settings.Secure.putString(getContext().getContentResolver(), "voice_recognition_service", new ComponentName(next.service.packageName, next.serviceInfo.getRecognitionService()).flattenToShortString());
                return true;
            }
        }
        Iterator<VoiceInputHelper.RecognizerInfo> it2 = this.mHelper.mAvailableRecognizerInfos.iterator();
        while (it2.hasNext()) {
            if (TextUtils.equals(str, it2.next().key)) {
                Settings.Secure.putString(getContext().getContentResolver(), "voice_interaction_service", "");
                Settings.Secure.putString(getContext().getContentResolver(), "voice_recognition_service", str);
                return true;
            }
        }
        return true;
    }

    public static ComponentName getCurrentService(VoiceInputHelper voiceInputHelper) {
        if (voiceInputHelper.mCurrentVoiceInteraction != null) {
            return voiceInputHelper.mCurrentVoiceInteraction;
        }
        if (voiceInputHelper.mCurrentRecognizer != null) {
            return voiceInputHelper.mCurrentRecognizer;
        }
        return null;
    }

    private ComponentName getCurrentAssist() {
        return this.mAssistUtils.getAssistComponentForUser(this.mUserId);
    }

    public static boolean isCurrentAssistVoiceService(ComponentName componentName, ComponentName componentName2) {
        return (componentName == null && componentName2 == null) || (componentName != null && componentName.equals(componentName2));
    }

    public static class VoiceInputDefaultAppInfo extends DefaultAppInfo {
        public VoiceInputHelper.BaseInfo mInfo;

        public VoiceInputDefaultAppInfo(Context context, PackageManagerWrapper packageManagerWrapper, int i, VoiceInputHelper.BaseInfo baseInfo, boolean z) {
            super(context, packageManagerWrapper, i, baseInfo.componentName, null, z);
            this.mInfo = baseInfo;
        }

        @Override // com.android.settingslib.applications.DefaultAppInfo, com.android.settingslib.widget.CandidateInfo
        public String getKey() {
            return this.mInfo.key;
        }

        @Override // com.android.settingslib.applications.DefaultAppInfo, com.android.settingslib.widget.CandidateInfo
        public CharSequence loadLabel() {
            if (this.mInfo instanceof VoiceInputHelper.InteractionInfo) {
                return this.mInfo.appLabel;
            }
            return this.mInfo.label;
        }

        public Intent getSettingIntent() {
            if (this.mInfo.settings == null) {
                return null;
            }
            return new Intent("android.intent.action.MAIN").setComponent(this.mInfo.settings);
        }
    }
}
