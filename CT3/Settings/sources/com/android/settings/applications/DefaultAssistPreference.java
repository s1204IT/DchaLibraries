package com.android.settings.applications;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.voice.VoiceInteractionServiceInfo;
import android.util.AttributeSet;
import android.util.Log;
import com.android.internal.app.AssistUtils;
import com.android.settings.AppListPreferenceWithSettings;
import com.android.settings.R;
import com.mediatek.settings.inputmethod.InputMethodExts;
import java.util.ArrayList;
import java.util.List;

public class DefaultAssistPreference extends AppListPreferenceWithSettings {
    private static final String TAG = DefaultAssistPreference.class.getSimpleName();
    private final AssistUtils mAssistUtils;
    private final List<Info> mAvailableAssistants;

    public DefaultAssistPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mAvailableAssistants = new ArrayList();
        setShowItemNone(true);
        setDialogTitle(R.string.choose_assist_title);
        this.mAssistUtils = new AssistUtils(context);
    }

    @Override
    protected boolean persistString(String value) {
        Info info = findAssistantByPackageName(value);
        if (info == null) {
            setAssistNone();
            return true;
        }
        if (info.isVoiceInteractionService()) {
            setAssistService(info);
        } else {
            setAssistActivity(info);
        }
        return true;
    }

    private void setAssistNone() {
        Settings.Secure.putString(getContext().getContentResolver(), "assistant", "");
        Settings.Secure.putString(getContext().getContentResolver(), "voice_interaction_service", "");
        Settings.Secure.putString(getContext().getContentResolver(), "voice_recognition_service", getDefaultRecognizer());
        setSummary(getContext().getText(R.string.default_assist_none));
        setSettingsComponent(null);
    }

    private void setAssistService(Info serviceInfo) {
        String serviceComponentName = serviceInfo.component.flattenToShortString();
        String serviceRecognizerName = new ComponentName(serviceInfo.component.getPackageName(), serviceInfo.voiceInteractionServiceInfo.getRecognitionService()).flattenToShortString();
        Settings.Secure.putString(getContext().getContentResolver(), "assistant", serviceComponentName);
        Settings.Secure.putString(getContext().getContentResolver(), "voice_interaction_service", serviceComponentName);
        Settings.Secure.putString(getContext().getContentResolver(), "voice_recognition_service", serviceRecognizerName);
        setSummary(getEntry());
        String settingsActivity = serviceInfo.voiceInteractionServiceInfo.getSettingsActivity();
        setSettingsComponent(settingsActivity != null ? new ComponentName(serviceInfo.component.getPackageName(), settingsActivity) : null);
    }

    private void setAssistActivity(Info activityInfo) {
        Settings.Secure.putString(getContext().getContentResolver(), "assistant", activityInfo.component.flattenToShortString());
        Settings.Secure.putString(getContext().getContentResolver(), "voice_interaction_service", "");
        Settings.Secure.putString(getContext().getContentResolver(), "voice_recognition_service", getDefaultRecognizer());
        setSummary(getEntry());
        setSettingsComponent(null);
    }

    private String getDefaultRecognizer() {
        ResolveInfo resolveInfo = getContext().getPackageManager().resolveService(new Intent("android.speech.RecognitionService"), 128);
        if (resolveInfo == null || resolveInfo.serviceInfo == null) {
            Log.w(TAG, "Unable to resolve default voice recognition service.");
            return "";
        }
        return new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name).flattenToShortString();
    }

    private Info findAssistantByPackageName(String packageName) {
        for (int i = 0; i < this.mAvailableAssistants.size(); i++) {
            Info info = this.mAvailableAssistants.get(i);
            if (info.component.getPackageName().equals(packageName)) {
                return info;
            }
        }
        return null;
    }

    private void addAssistServices() {
        PackageManager pm = getContext().getPackageManager();
        List<ResolveInfo> services = pm.queryIntentServices(new Intent("android.service.voice.VoiceInteractionService"), 128);
        for (int i = 0; i < services.size(); i++) {
            ResolveInfo resolveInfo = services.get(i);
            VoiceInteractionServiceInfo voiceInteractionServiceInfo = new VoiceInteractionServiceInfo(pm, resolveInfo.serviceInfo);
            if (voiceInteractionServiceInfo.getSupportsAssist() && InputMethodExts.isAssistServiceSupport(getContext(), resolveInfo.serviceInfo)) {
                this.mAvailableAssistants.add(new Info(new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name), voiceInteractionServiceInfo));
            }
        }
    }

    private void addAssistActivities() {
        PackageManager pm = getContext().getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(new Intent("android.intent.action.ASSIST"), 65536);
        for (int i = 0; i < activities.size(); i++) {
            ResolveInfo resolveInfo = activities.get(i);
            this.mAvailableAssistants.add(new Info(new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name)));
        }
    }

    public ComponentName getCurrentAssist() {
        return this.mAssistUtils.getAssistComponentForUser(UserHandle.myUserId());
    }

    public void refreshAssistApps() {
        this.mAvailableAssistants.clear();
        addAssistServices();
        addAssistActivities();
        List<String> packages = new ArrayList<>();
        for (int i = 0; i < this.mAvailableAssistants.size(); i++) {
            String packageName = this.mAvailableAssistants.get(i).component.getPackageName();
            if (!packages.contains(packageName)) {
                packages.add(packageName);
            }
        }
        ComponentName currentAssist = getCurrentAssist();
        setPackageNames((CharSequence[]) packages.toArray(new String[packages.size()]), currentAssist != null ? currentAssist.getPackageName() : null);
    }

    private static class Info {
        public final ComponentName component;
        public final VoiceInteractionServiceInfo voiceInteractionServiceInfo;

        Info(ComponentName component) {
            this.component = component;
            this.voiceInteractionServiceInfo = null;
        }

        Info(ComponentName component, VoiceInteractionServiceInfo voiceInteractionServiceInfo) {
            this.component = component;
            this.voiceInteractionServiceInfo = voiceInteractionServiceInfo;
        }

        public boolean isVoiceInteractionService() {
            return this.voiceInteractionServiceInfo != null;
        }
    }
}
