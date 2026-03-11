package com.android.settings.applications;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.usb.IUsbManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.BulletSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState;

public class ClearDefaultsPreference extends Preference {
    protected static final String TAG = ClearDefaultsPreference.class.getSimpleName();
    private Button mActivitiesButton;
    protected ApplicationsState.AppEntry mAppEntry;
    private AppWidgetManager mAppWidgetManager;
    private String mPackageName;
    private PackageManager mPm;
    private IUsbManager mUsbManager;

    public ClearDefaultsPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.app_preferred_settings);
        this.mAppWidgetManager = AppWidgetManager.getInstance(context);
        this.mPm = context.getPackageManager();
        IBinder b = ServiceManager.getService("usb");
        this.mUsbManager = IUsbManager.Stub.asInterface(b);
    }

    public ClearDefaultsPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ClearDefaultsPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ClearDefaultsPreference(Context context) {
        this(context, null);
    }

    public void setPackageName(String packageName) {
        this.mPackageName = packageName;
    }

    public void setAppEntry(ApplicationsState.AppEntry entry) {
        this.mAppEntry = entry;
    }

    @Override
    public void onBindViewHolder(final PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        this.mActivitiesButton = (Button) view.findViewById(R.id.clear_activities_button);
        this.mActivitiesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ClearDefaultsPreference.this.mUsbManager == null) {
                    return;
                }
                int userId = UserHandle.myUserId();
                ClearDefaultsPreference.this.mPm.clearPackagePreferredActivities(ClearDefaultsPreference.this.mPackageName);
                if (ClearDefaultsPreference.this.isDefaultBrowser(ClearDefaultsPreference.this.mPackageName)) {
                    ClearDefaultsPreference.this.mPm.setDefaultBrowserPackageNameAsUser(null, userId);
                }
                try {
                    ClearDefaultsPreference.this.mUsbManager.clearDefaults(ClearDefaultsPreference.this.mPackageName, userId);
                } catch (RemoteException e) {
                    Log.e(ClearDefaultsPreference.TAG, "mUsbManager.clearDefaults", e);
                }
                ClearDefaultsPreference.this.mAppWidgetManager.setBindAppWidgetPermission(ClearDefaultsPreference.this.mPackageName, false);
                TextView autoLaunchView = (TextView) view.findViewById(R.id.auto_launch);
                ClearDefaultsPreference.this.resetLaunchDefaultsUi(autoLaunchView);
            }
        });
        updateUI(view);
    }

    public boolean updateUI(PreferenceViewHolder view) {
        boolean autoLaunchEnabled;
        boolean hasBindAppWidgetPermission = this.mAppWidgetManager.hasBindAppWidgetPermission(this.mAppEntry.info.packageName);
        TextView autoLaunchView = (TextView) view.findViewById(R.id.auto_launch);
        if (AppUtils.hasPreferredActivities(this.mPm, this.mPackageName) || isDefaultBrowser(this.mPackageName)) {
            autoLaunchEnabled = true;
        } else {
            autoLaunchEnabled = AppUtils.hasUsbDefaults(this.mUsbManager, this.mPackageName);
        }
        if (!autoLaunchEnabled && !hasBindAppWidgetPermission) {
            resetLaunchDefaultsUi(autoLaunchView);
            return true;
        }
        boolean z = hasBindAppWidgetPermission ? autoLaunchEnabled : false;
        if (hasBindAppWidgetPermission) {
            autoLaunchView.setText(R.string.auto_launch_label_generic);
        } else {
            autoLaunchView.setText(R.string.auto_launch_label);
        }
        Context context = getContext();
        CharSequence text = null;
        int bulletIndent = context.getResources().getDimensionPixelSize(R.dimen.installed_app_details_bullet_offset);
        if (autoLaunchEnabled) {
            CharSequence autoLaunchEnableText = context.getText(R.string.auto_launch_enable_text);
            SpannableString s = new SpannableString(autoLaunchEnableText);
            if (z) {
                s.setSpan(new BulletSpan(bulletIndent), 0, autoLaunchEnableText.length(), 0);
            }
            text = TextUtils.concat(s, "\n");
        }
        if (hasBindAppWidgetPermission) {
            CharSequence alwaysAllowBindAppWidgetsText = context.getText(R.string.always_allow_bind_appwidgets_text);
            SpannableString s2 = new SpannableString(alwaysAllowBindAppWidgetsText);
            if (z) {
                s2.setSpan(new BulletSpan(bulletIndent), 0, alwaysAllowBindAppWidgetsText.length(), 0);
            }
            text = text == null ? TextUtils.concat(s2, "\n") : TextUtils.concat(text, "\n", s2, "\n");
        }
        autoLaunchView.setText(text);
        this.mActivitiesButton.setEnabled(true);
        return true;
    }

    public boolean isDefaultBrowser(String packageName) {
        String defaultBrowser = this.mPm.getDefaultBrowserPackageNameAsUser(UserHandle.myUserId());
        return packageName.equals(defaultBrowser);
    }

    public void resetLaunchDefaultsUi(TextView autoLaunchView) {
        autoLaunchView.setText(R.string.auto_launch_disable_text);
        this.mActivitiesButton.setEnabled(false);
    }
}
