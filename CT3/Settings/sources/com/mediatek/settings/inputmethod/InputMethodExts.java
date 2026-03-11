package com.mediatek.settings.inputmethod;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.media.AudioManager;
import android.provider.Settings;
import android.service.voice.VoiceInteractionServiceInfo;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.util.Log;
import com.android.settings.R;
import java.util.List;

public class InputMethodExts {
    private Context mContext;
    private boolean mIsOnlyImeSettings;
    private PreferenceCategory mVoiceCategory;
    private Intent mVoiceControlIntent;
    private Preference mVoiceUiPref;

    public InputMethodExts(Context context, boolean isOnlyImeSettings, PreferenceCategory voiceCategory, PreferenceCategory pointCategory) {
        this.mContext = context;
        this.mIsOnlyImeSettings = isOnlyImeSettings;
        this.mVoiceCategory = voiceCategory;
    }

    public void initExtendsItems() {
        this.mVoiceUiPref = new Preference(this.mContext);
        this.mVoiceUiPref.setKey("voice_ui");
        this.mVoiceUiPref.setTitle(this.mContext.getString(R.string.voice_ui_title));
        if (this.mVoiceCategory != null) {
            this.mVoiceCategory.addPreference(this.mVoiceUiPref);
        }
        if ((!this.mIsOnlyImeSettings && isWakeupSupport(this.mContext)) || this.mVoiceUiPref == null || this.mVoiceCategory == null) {
            return;
        }
        Log.d("InputMethodExts", "initExtendsItems remove voice ui feature ");
        this.mVoiceCategory.removePreference(this.mVoiceUiPref);
    }

    public void resumeExtendsItems() {
        this.mVoiceControlIntent = new Intent("com.mediatek.voicecommand.VOICE_CONTROL_SETTINGS");
        this.mVoiceControlIntent.setFlags(268435456);
        List<ResolveInfo> apps = this.mContext.getPackageManager().queryIntentActivities(this.mVoiceControlIntent, 0);
        if (apps == null || apps.size() == 0) {
            if (this.mVoiceUiPref == null || this.mVoiceCategory == null) {
                return;
            }
            Log.d("InputMethodExts", "resumeExtendsItems remove voice ui feature ");
            this.mVoiceCategory.removePreference(this.mVoiceUiPref);
            return;
        }
        if (this.mIsOnlyImeSettings) {
            return;
        }
        Log.d("InputMethodExts", "resumeExtendsItems add voice ui feature ");
        if (this.mVoiceUiPref == null || this.mVoiceCategory == null) {
            return;
        }
        this.mVoiceCategory.addPreference(this.mVoiceUiPref);
    }

    public void onClickExtendsItems(String preferKey) {
        if (!"voice_ui".equals(preferKey)) {
            return;
        }
        this.mContext.startActivity(this.mVoiceControlIntent);
    }

    public static boolean isWakeupSupport(Context context) {
        AudioManager am = (AudioManager) context.getSystemService("audio");
        if (am == null) {
            Log.e("InputMethodExts", "isWakeupSupport get audio service is null");
            return false;
        }
        String state = am.getParameters("MTK_VOW_SUPPORT");
        if (state != null) {
            return state.equalsIgnoreCase("MTK_VOW_SUPPORT=true");
        }
        return false;
    }

    public static boolean isAssistServiceSupport(Context context, ServiceInfo serviceInfo) {
        ComponentName componentName = new ComponentName(serviceInfo.packageName, serviceInfo.name);
        String key = componentName.flattenToShortString();
        return isVoiceInteractionServiceSupport(context, key);
    }

    public static boolean isVoiceInteractionServiceSupport(Context context, String infoKey) {
        if ("com.mediatek.voicecommand.vis/VoiceWakeupInteractionService".equals(infoKey)) {
            return isWakeupSupport(context);
        }
        return true;
    }

    public static boolean isVoiceRecognitionServiceSupport(Context context, String infoKey) {
        if ("com.mediatek.voicecommand.vis/VoiceWakeupRecognitionService".equals(infoKey)) {
            return true;
        }
        return false;
    }

    public static boolean displayVoiceWakeupAlert(final Context context, final String packageName) {
        if (!packageName.equals("com.mediatek.voicecommand")) {
            return false;
        }
        int cmdStatus = Settings.System.getInt(context.getContentResolver(), Settings.System.VOICE_WAKEUP_COMMAND_STATUS, 0);
        Log.d("InputMethodExts", "DisplayVoiceWakeupAlert cmdStatus :" + cmdStatus);
        if (cmdStatus != 0) {
            return false;
        }
        displayVoiceWakeupAlert(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent("android.intent.action.MAIN");
                intent.setComponent(InputMethodExts.getSettingsComponent(context, packageName));
                intent.putExtra("Voice Wakeup Enable Confirm", true);
                context.startActivity(new Intent(intent));
            }
        }, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        }, context);
        return true;
    }

    private static void displayVoiceWakeupAlert(DialogInterface.OnClickListener positiveOnClickListener, final DialogInterface.OnClickListener negativeOnClickListener, Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.voice_wakeup)).setMessage(context.getString(R.string.voice_wakeup_confirm)).setCancelable(true).setPositiveButton(android.R.string.ok, positiveOnClickListener).setNegativeButton(android.R.string.cancel, negativeOnClickListener).setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                negativeOnClickListener.onClick(dialog, -2);
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public static ComponentName getSettingsComponent(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> services = pm.queryIntentServices(new Intent("android.service.voice.VoiceInteractionService"), 128);
        for (int i = 0; i < services.size(); i++) {
            ResolveInfo resolveInfo = services.get(i);
            VoiceInteractionServiceInfo voiceInteractionServiceInfo = new VoiceInteractionServiceInfo(pm, resolveInfo.serviceInfo);
            if (packageName.equals(resolveInfo.serviceInfo.packageName)) {
                return new ComponentName(resolveInfo.serviceInfo.packageName, voiceInteractionServiceInfo.getSettingsActivity());
            }
        }
        return null;
    }
}
