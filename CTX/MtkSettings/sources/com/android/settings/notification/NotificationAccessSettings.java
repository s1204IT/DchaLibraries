package com.android.settings.notification;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Pair;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.notification.NotificationAccessSettings;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.utils.ManagedServiceSettings;

public class NotificationAccessSettings extends ManagedServiceSettings {
    private NotificationManager mNm;
    private static final String TAG = NotificationAccessSettings.class.getSimpleName();
    private static final ManagedServiceSettings.Config CONFIG = new ManagedServiceSettings.Config.Builder().setTag(TAG).setSetting("enabled_notification_listeners").setIntentAction("android.service.notification.NotificationListenerService").setPermission("android.permission.BIND_NOTIFICATION_LISTENER_SERVICE").setNoun("notification listener").setWarningDialogTitle(R.string.notification_listener_security_warning_title).setWarningDialogSummary(R.string.notification_listener_security_warning_summary).setEmptyText(R.string.no_notification_listeners).build();

    @Override
    public int getMetricsCategory() {
        return 179;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.mNm = (NotificationManager) context.getSystemService(NotificationManager.class);
    }

    @Override
    protected ManagedServiceSettings.Config getConfig() {
        return CONFIG;
    }

    @Override
    protected boolean setEnabled(ComponentName componentName, String str, boolean z) {
        logSpecialPermissionChange(z, componentName.getPackageName());
        if (!z) {
            if (!isServiceEnabled(componentName)) {
                return true;
            }
            new FriendlyWarningDialogFragment().setServiceInfo(componentName, str, this).show(getFragmentManager(), "friendlydialog");
            return false;
        }
        if (isServiceEnabled(componentName)) {
            return true;
        }
        new ManagedServiceSettings.ScaryWarningDialogFragment().setServiceInfo(componentName, str, this).show(getFragmentManager(), "dialog");
        return false;
    }

    @Override
    protected boolean isServiceEnabled(ComponentName componentName) {
        return this.mNm.isNotificationListenerAccessGranted(componentName);
    }

    @Override
    protected void enable(ComponentName componentName) {
        this.mNm.setNotificationListenerAccessGranted(componentName, true);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.notification_access_settings;
    }

    @VisibleForTesting
    void logSpecialPermissionChange(boolean z, String str) {
        FeatureFactory.getFactory(getContext()).getMetricsFeatureProvider().action(getContext(), z ? 776 : 777, str, new Pair[0]);
    }

    private static void disable(final NotificationAccessSettings notificationAccessSettings, final ComponentName componentName) {
        notificationAccessSettings.mNm.setNotificationListenerAccessGranted(componentName, false);
        AsyncTask.execute(new Runnable() {
            @Override
            public final void run() {
                NotificationAccessSettings.lambda$disable$0(this.f$0, componentName);
            }
        });
    }

    static void lambda$disable$0(NotificationAccessSettings notificationAccessSettings, ComponentName componentName) {
        if (!notificationAccessSettings.mNm.isNotificationPolicyAccessGrantedForPackage(componentName.getPackageName())) {
            notificationAccessSettings.mNm.removeAutomaticZenRules(componentName.getPackageName());
        }
    }

    public static class FriendlyWarningDialogFragment extends InstrumentedDialogFragment {
        public FriendlyWarningDialogFragment setServiceInfo(ComponentName componentName, String str, Fragment fragment) {
            Bundle bundle = new Bundle();
            bundle.putString("c", componentName.flattenToString());
            bundle.putString("l", str);
            setArguments(bundle);
            setTargetFragment(fragment, 0);
            return this;
        }

        @Override
        public int getMetricsCategory() {
            return 552;
        }

        @Override
        public Dialog onCreateDialog(Bundle bundle) {
            Bundle arguments = getArguments();
            String string = arguments.getString("l");
            final ComponentName componentNameUnflattenFromString = ComponentName.unflattenFromString(arguments.getString("c"));
            final NotificationAccessSettings notificationAccessSettings = (NotificationAccessSettings) getTargetFragment();
            return new AlertDialog.Builder(getContext()).setMessage(getResources().getString(R.string.notification_listener_disable_warning_summary, string)).setCancelable(true).setPositiveButton(R.string.notification_listener_disable_warning_confirm, new DialogInterface.OnClickListener() {
                @Override
                public final void onClick(DialogInterface dialogInterface, int i) {
                    NotificationAccessSettings.disable(notificationAccessSettings, componentNameUnflattenFromString);
                }
            }).setNegativeButton(R.string.notification_listener_disable_warning_cancel, new DialogInterface.OnClickListener() {
                @Override
                public final void onClick(DialogInterface dialogInterface, int i) {
                    NotificationAccessSettings.FriendlyWarningDialogFragment.lambda$onCreateDialog$1(dialogInterface, i);
                }
            }).create();
        }

        static void lambda$onCreateDialog$1(DialogInterface dialogInterface, int i) {
        }
    }
}
