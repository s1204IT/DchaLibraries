package com.android.internal.accessibility;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.ActivityManager;
import android.app.ActivityThread;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Handler;
import android.os.RemoteException;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Slog;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;
import com.android.internal.R;
import com.android.internal.util.ArrayUtils;
import java.util.Collections;
import java.util.Map;

public class AccessibilityShortcutController {
    private static final String TAG = "AccessibilityShortcutController";
    private static Map<ComponentName, ToggleableFrameworkFeatureInfo> sFrameworkShortcutFeaturesMap;
    private AlertDialog mAlertDialog;
    private final Context mContext;
    private boolean mEnabledOnLockScreen;
    public FrameworkObjectProvider mFrameworkObjectProvider = new FrameworkObjectProvider();
    private boolean mIsShortcutEnabled;
    private int mUserId;
    public static final ComponentName COLOR_INVERSION_COMPONENT_NAME = new ComponentName("com.android.server.accessibility", "ColorInversion");
    public static final ComponentName DALTONIZER_COMPONENT_NAME = new ComponentName("com.android.server.accessibility", "Daltonizer");
    private static final AudioAttributes VIBRATION_ATTRIBUTES = new AudioAttributes.Builder().setContentType(4).setUsage(11).build();

    public static String getTargetServiceComponentNameString(Context context, int i) {
        String stringForUser = Settings.Secure.getStringForUser(context.getContentResolver(), Settings.Secure.ACCESSIBILITY_SHORTCUT_TARGET_SERVICE, i);
        if (stringForUser != null) {
            return stringForUser;
        }
        return context.getString(R.string.config_defaultAccessibilityService);
    }

    public static Map<ComponentName, ToggleableFrameworkFeatureInfo> getFrameworkShortcutFeaturesMap() {
        if (sFrameworkShortcutFeaturesMap == null) {
            ArrayMap arrayMap = new ArrayMap(2);
            arrayMap.put(COLOR_INVERSION_COMPONENT_NAME, new ToggleableFrameworkFeatureInfo(Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED, WifiEnterpriseConfig.ENGINE_ENABLE, WifiEnterpriseConfig.ENGINE_DISABLE, R.string.color_inversion_feature_name));
            arrayMap.put(DALTONIZER_COMPONENT_NAME, new ToggleableFrameworkFeatureInfo(Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, WifiEnterpriseConfig.ENGINE_ENABLE, WifiEnterpriseConfig.ENGINE_DISABLE, R.string.color_correction_feature_name));
            sFrameworkShortcutFeaturesMap = Collections.unmodifiableMap(arrayMap);
        }
        return sFrameworkShortcutFeaturesMap;
    }

    public AccessibilityShortcutController(Context context, Handler handler, int i) {
        this.mContext = context;
        this.mUserId = i;
        ContentObserver contentObserver = new ContentObserver(handler) {
            @Override
            public void onChange(boolean z, Uri uri, int i2) {
                if (i2 == AccessibilityShortcutController.this.mUserId) {
                    AccessibilityShortcutController.this.onSettingsChanged();
                }
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_SHORTCUT_TARGET_SERVICE), false, contentObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_SHORTCUT_ENABLED), false, contentObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_SHORTCUT_ON_LOCK_SCREEN), false, contentObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_SHORTCUT_DIALOG_SHOWN), false, contentObserver, -1);
        setCurrentUser(this.mUserId);
    }

    public void setCurrentUser(int i) {
        this.mUserId = i;
        onSettingsChanged();
    }

    public boolean isAccessibilityShortcutAvailable(boolean z) {
        return this.mIsShortcutEnabled && (!z || this.mEnabledOnLockScreen);
    }

    public void onSettingsChanged() {
        boolean z = !TextUtils.isEmpty(getTargetServiceComponentNameString(this.mContext, this.mUserId));
        ContentResolver contentResolver = this.mContext.getContentResolver();
        boolean z2 = Settings.Secure.getIntForUser(contentResolver, Settings.Secure.ACCESSIBILITY_SHORTCUT_ENABLED, 1, this.mUserId) == 1;
        this.mEnabledOnLockScreen = Settings.Secure.getIntForUser(contentResolver, Settings.Secure.ACCESSIBILITY_SHORTCUT_ON_LOCK_SCREEN, Settings.Secure.getIntForUser(contentResolver, Settings.Secure.ACCESSIBILITY_SHORTCUT_DIALOG_SHOWN, 0, this.mUserId), this.mUserId) == 1;
        this.mIsShortcutEnabled = z2 && z;
    }

    public void performAccessibilityShortcut() throws RemoteException {
        int i;
        int i2;
        Slog.d(TAG, "Accessibility shortcut activated");
        ContentResolver contentResolver = this.mContext.getContentResolver();
        int currentUser = ActivityManager.getCurrentUser();
        int intForUser = Settings.Secure.getIntForUser(contentResolver, Settings.Secure.ACCESSIBILITY_SHORTCUT_DIALOG_SHOWN, 0, currentUser);
        if (hasFeatureLeanback()) {
            i = 11;
        } else {
            i = 10;
        }
        Ringtone ringtone = RingtoneManager.getRingtone(this.mContext, Settings.System.DEFAULT_NOTIFICATION_URI);
        if (ringtone != null) {
            ringtone.setAudioAttributes(new AudioAttributes.Builder().setUsage(i).build());
            ringtone.play();
        }
        Vibrator vibrator = (Vibrator) this.mContext.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(ArrayUtils.convertToLongArray(this.mContext.getResources().getIntArray(R.array.config_longPressVibePattern)), -1, VIBRATION_ATTRIBUTES);
        }
        if (intForUser == 0) {
            this.mAlertDialog = createShortcutWarningDialog(currentUser);
            if (this.mAlertDialog == null) {
                return;
            }
            Window window = this.mAlertDialog.getWindow();
            WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.type = WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG;
            window.setAttributes(attributes);
            this.mAlertDialog.show();
            Settings.Secure.putIntForUser(contentResolver, Settings.Secure.ACCESSIBILITY_SHORTCUT_DIALOG_SHOWN, 1, currentUser);
            return;
        }
        if (this.mAlertDialog != null) {
            this.mAlertDialog.dismiss();
            this.mAlertDialog = null;
        }
        String shortcutFeatureDescription = getShortcutFeatureDescription(false);
        if (shortcutFeatureDescription == null) {
            Slog.e(TAG, "Accessibility shortcut set to invalid service");
            return;
        }
        AccessibilityServiceInfo infoForTargetService = getInfoForTargetService();
        if (infoForTargetService != null) {
            Context context = this.mContext;
            if (isServiceEnabled(infoForTargetService)) {
                i2 = R.string.accessibility_shortcut_disabling_service;
            } else {
                i2 = R.string.accessibility_shortcut_enabling_service;
            }
            Toast toastMakeToastFromText = this.mFrameworkObjectProvider.makeToastFromText(this.mContext, String.format(context.getString(i2), shortcutFeatureDescription), 1);
            toastMakeToastFromText.getWindowParams().privateFlags |= 16;
            toastMakeToastFromText.show();
        }
        this.mFrameworkObjectProvider.getAccessibilityManagerInstance(this.mContext).performAccessibilityShortcut();
    }

    private AlertDialog createShortcutWarningDialog(final int i) throws RemoteException {
        String shortcutFeatureDescription = getShortcutFeatureDescription(true);
        if (shortcutFeatureDescription == null) {
            return null;
        }
        return this.mFrameworkObjectProvider.getAlertDialogBuilder(this.mFrameworkObjectProvider.getSystemUiContext()).setTitle(R.string.accessibility_shortcut_warning_dialog_title).setMessage(String.format(this.mContext.getString(R.string.accessibility_shortcut_toogle_warning), shortcutFeatureDescription)).setCancelable(false).setPositiveButton(R.string.leave_accessibility_shortcut_on, (DialogInterface.OnClickListener) null).setNegativeButton(R.string.disable_accessibility_shortcut, new DialogInterface.OnClickListener() {
            @Override
            public final void onClick(DialogInterface dialogInterface, int i2) {
                Settings.Secure.putStringForUser(this.f$0.mContext.getContentResolver(), Settings.Secure.ACCESSIBILITY_SHORTCUT_TARGET_SERVICE, "", i);
            }
        }).setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public final void onCancel(DialogInterface dialogInterface) {
                Settings.Secure.putIntForUser(this.f$0.mContext.getContentResolver(), Settings.Secure.ACCESSIBILITY_SHORTCUT_DIALOG_SHOWN, 0, i);
            }
        }).create();
    }

    private AccessibilityServiceInfo getInfoForTargetService() {
        String targetServiceComponentNameString = getTargetServiceComponentNameString(this.mContext, -2);
        if (targetServiceComponentNameString == null) {
            return null;
        }
        return this.mFrameworkObjectProvider.getAccessibilityManagerInstance(this.mContext).getInstalledServiceInfoWithComponentName(ComponentName.unflattenFromString(targetServiceComponentNameString));
    }

    private String getShortcutFeatureDescription(boolean z) throws RemoteException {
        String targetServiceComponentNameString = getTargetServiceComponentNameString(this.mContext, -2);
        if (targetServiceComponentNameString == null) {
            return null;
        }
        ComponentName componentNameUnflattenFromString = ComponentName.unflattenFromString(targetServiceComponentNameString);
        ToggleableFrameworkFeatureInfo toggleableFrameworkFeatureInfo = getFrameworkShortcutFeaturesMap().get(componentNameUnflattenFromString);
        if (toggleableFrameworkFeatureInfo != null) {
            return toggleableFrameworkFeatureInfo.getLabel(this.mContext);
        }
        AccessibilityServiceInfo installedServiceInfoWithComponentName = this.mFrameworkObjectProvider.getAccessibilityManagerInstance(this.mContext).getInstalledServiceInfoWithComponentName(componentNameUnflattenFromString);
        if (installedServiceInfoWithComponentName == null) {
            return null;
        }
        PackageManager packageManager = this.mContext.getPackageManager();
        String string = installedServiceInfoWithComponentName.getResolveInfo().loadLabel(packageManager).toString();
        CharSequence charSequenceLoadSummary = installedServiceInfoWithComponentName.loadSummary(packageManager);
        if (!z || TextUtils.isEmpty(charSequenceLoadSummary)) {
            return string;
        }
        return String.format("%s\n%s", string, charSequenceLoadSummary);
    }

    private boolean isServiceEnabled(AccessibilityServiceInfo accessibilityServiceInfo) {
        return this.mFrameworkObjectProvider.getAccessibilityManagerInstance(this.mContext).getEnabledAccessibilityServiceList(-1).contains(accessibilityServiceInfo);
    }

    private boolean hasFeatureLeanback() {
        return this.mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK);
    }

    public static class ToggleableFrameworkFeatureInfo {
        private int mIconDrawableId;
        private final int mLabelStringResourceId;
        private final String mSettingKey;
        private final String mSettingOffValue;
        private final String mSettingOnValue;

        ToggleableFrameworkFeatureInfo(String str, String str2, String str3, int i) {
            this.mSettingKey = str;
            this.mSettingOnValue = str2;
            this.mSettingOffValue = str3;
            this.mLabelStringResourceId = i;
        }

        public String getSettingKey() {
            return this.mSettingKey;
        }

        public String getSettingOnValue() {
            return this.mSettingOnValue;
        }

        public String getSettingOffValue() {
            return this.mSettingOffValue;
        }

        public String getLabel(Context context) {
            return context.getString(this.mLabelStringResourceId);
        }
    }

    public static class FrameworkObjectProvider {
        public AccessibilityManager getAccessibilityManagerInstance(Context context) {
            return AccessibilityManager.getInstance(context);
        }

        public AlertDialog.Builder getAlertDialogBuilder(Context context) {
            return new AlertDialog.Builder(context);
        }

        public Toast makeToastFromText(Context context, CharSequence charSequence, int i) {
            return Toast.makeText(context, charSequence, i);
        }

        public Context getSystemUiContext() {
            return ActivityThread.currentActivityThread().getSystemUiContext();
        }
    }
}
