package com.android.settings;

import android.app.Activity;
import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.AppGlobals;
import android.app.IActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.settings.Settings;
import com.android.settingslib.RestrictedLockUtils;

public class ShowAdminSupportDetailsDialog extends Activity implements DialogInterface.OnDismissListener {
    private View mDialogView;
    private DevicePolicyManager mDpm;
    private RestrictedLockUtils.EnforcedAdmin mEnforcedAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mDpm = (DevicePolicyManager) getSystemService(DevicePolicyManager.class);
        this.mEnforcedAdmin = getAdminDetailsFromIntent(getIntent());
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        this.mDialogView = LayoutInflater.from(builder.getContext()).inflate(R.layout.admin_support_details_dialog, (ViewGroup) null);
        initializeDialogViews(this.mDialogView, this.mEnforcedAdmin.component, this.mEnforcedAdmin.userId);
        builder.setOnDismissListener(this).setPositiveButton(R.string.okay, (DialogInterface.OnClickListener) null).setView(this.mDialogView).show();
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        RestrictedLockUtils.EnforcedAdmin admin = getAdminDetailsFromIntent(intent);
        if (this.mEnforcedAdmin.equals(admin)) {
            return;
        }
        this.mEnforcedAdmin = admin;
        initializeDialogViews(this.mDialogView, this.mEnforcedAdmin.component, this.mEnforcedAdmin.userId);
    }

    private RestrictedLockUtils.EnforcedAdmin getAdminDetailsFromIntent(Intent intent) {
        RestrictedLockUtils.EnforcedAdmin admin = new RestrictedLockUtils.EnforcedAdmin(null, UserHandle.myUserId());
        if (intent != null && checkIfCallerHasPermission("android.permission.MANAGE_DEVICE_ADMINS")) {
            admin.component = (ComponentName) intent.getParcelableExtra("android.app.extra.DEVICE_ADMIN");
            admin.userId = intent.getIntExtra("android.intent.extra.USER_ID", UserHandle.myUserId());
        }
        return admin;
    }

    private boolean checkIfCallerHasPermission(String permission) {
        IActivityManager am = ActivityManagerNative.getDefault();
        try {
            int uid = am.getLaunchedFromUid(getActivityToken());
            return AppGlobals.getPackageManager().checkUidPermission(permission, uid) == 0;
        } catch (RemoteException e) {
            Log.e("AdminSupportDialog", "Could not talk to activity manager.", e);
            return false;
        }
    }

    private void initializeDialogViews(View root, ComponentName admin, int userId) {
        if (admin != null) {
            if (!RestrictedLockUtils.isAdminInCurrentUserOrProfile(this, admin) || !RestrictedLockUtils.isCurrentUserOrProfile(this, userId)) {
                admin = null;
            } else {
                ActivityInfo ai = null;
                try {
                    ai = AppGlobals.getPackageManager().getReceiverInfo(admin, 0, userId);
                } catch (RemoteException e) {
                    Log.w("AdminSupportDialog", "Missing reciever info", e);
                }
                if (ai != null) {
                    Drawable icon = ai.loadIcon(getPackageManager());
                    Drawable badgedIcon = getPackageManager().getUserBadgedIcon(icon, new UserHandle(userId));
                    ((ImageView) root.findViewById(R.id.admin_support_icon)).setImageDrawable(badgedIcon);
                }
            }
        }
        setAdminSupportDetails(this, root, new RestrictedLockUtils.EnforcedAdmin(admin, userId), true);
    }

    public static void setAdminSupportDetails(final Activity activity, View root, final RestrictedLockUtils.EnforcedAdmin enforcedAdmin, final boolean finishActivity) {
        if (enforcedAdmin == null) {
            return;
        }
        if (enforcedAdmin.component != null) {
            DevicePolicyManager dpm = (DevicePolicyManager) activity.getSystemService("device_policy");
            if (!RestrictedLockUtils.isAdminInCurrentUserOrProfile(activity, enforcedAdmin.component) || !RestrictedLockUtils.isCurrentUserOrProfile(activity, enforcedAdmin.userId)) {
                enforcedAdmin.component = null;
            } else {
                if (enforcedAdmin.userId == -10000) {
                    enforcedAdmin.userId = UserHandle.myUserId();
                }
                CharSequence supportMessage = null;
                if (UserHandle.isSameApp(Process.myUid(), 1000)) {
                    supportMessage = dpm.getShortSupportMessageForUser(enforcedAdmin.component, enforcedAdmin.userId);
                }
                if (supportMessage != null) {
                    TextView textView = (TextView) root.findViewById(R.id.admin_support_msg);
                    textView.setText(supportMessage);
                }
            }
        }
        root.findViewById(R.id.admins_policies_list).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                if (enforcedAdmin.component != null) {
                    intent.setClass(activity, DeviceAdminAdd.class);
                    intent.putExtra("android.app.extra.DEVICE_ADMIN", enforcedAdmin.component);
                    intent.putExtra("android.app.extra.CALLED_FROM_SUPPORT_DIALOG", true);
                    activity.startActivityAsUser(intent, new UserHandle(enforcedAdmin.userId));
                } else {
                    intent.setClass(activity, Settings.DeviceAdminSettingsActivity.class);
                    intent.addFlags(268435456);
                    activity.startActivity(intent);
                }
                if (!finishActivity) {
                    return;
                }
                activity.finish();
            }
        });
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        finish();
    }
}
