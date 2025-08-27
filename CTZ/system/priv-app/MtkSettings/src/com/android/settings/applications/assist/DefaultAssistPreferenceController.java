package com.android.settings.applications.assist;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.service.voice.VoiceInteractionServiceInfo;
import com.android.internal.app.AssistUtils;
import com.android.settings.R;
import com.android.settings.applications.defaultapps.DefaultAppPreferenceController;
import com.android.settingslib.applications.DefaultAppInfo;
import java.util.List;

/* loaded from: classes.dex */
public class DefaultAssistPreferenceController extends DefaultAppPreferenceController {
    private final AssistUtils mAssistUtils;
    private final String mPrefKey;
    private final boolean mShowSetting;

    public DefaultAssistPreferenceController(Context context, String str, boolean z) {
        super(context);
        this.mPrefKey = str;
        this.mShowSetting = z;
        this.mAssistUtils = new AssistUtils(context);
    }

    @Override // com.android.settings.applications.defaultapps.DefaultAppPreferenceController
    protected Intent getSettingIntent(DefaultAppInfo defaultAppInfo) {
        ComponentName assistComponentForUser;
        String assistSettingsActivity;
        if (!this.mShowSetting || (assistComponentForUser = this.mAssistUtils.getAssistComponentForUser(this.mUserId)) == null) {
            return null;
        }
        Intent intent = new Intent("android.service.voice.VoiceInteractionService").setPackage(assistComponentForUser.getPackageName());
        PackageManager packageManager = this.mPackageManager.getPackageManager();
        List<ResolveInfo> listQueryIntentServices = packageManager.queryIntentServices(intent, 128);
        if (listQueryIntentServices == null || listQueryIntentServices.isEmpty() || (assistSettingsActivity = getAssistSettingsActivity(assistComponentForUser, listQueryIntentServices.get(0), packageManager)) == null) {
            return null;
        }
        return new Intent("android.intent.action.MAIN").setComponent(new ComponentName(assistComponentForUser.getPackageName(), assistSettingsActivity));
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return this.mContext.getResources().getBoolean(R.bool.config_show_assist_and_voice_input);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return this.mPrefKey;
    }

    @Override // com.android.settings.applications.defaultapps.DefaultAppPreferenceController
    protected DefaultAppInfo getDefaultAppInfo() {
        ComponentName assistComponentForUser = this.mAssistUtils.getAssistComponentForUser(this.mUserId);
        if (assistComponentForUser == null) {
            return null;
        }
        return new DefaultAppInfo(this.mContext, this.mPackageManager, this.mUserId, assistComponentForUser);
    }

    String getAssistSettingsActivity(ComponentName componentName, ResolveInfo resolveInfo, PackageManager packageManager) {
        VoiceInteractionServiceInfo voiceInteractionServiceInfo = new VoiceInteractionServiceInfo(packageManager, resolveInfo.serviceInfo);
        if (!voiceInteractionServiceInfo.getSupportsAssist()) {
            return null;
        }
        return voiceInteractionServiceInfo.getSettingsActivity();
    }
}
