package com.android.settings;

import android.app.Activity;
import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.admin.DeviceAdminInfo;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.AppSecurityPermissions;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.settings.users.UserDialogs;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.xmlpull.v1.XmlPullParserException;

public class DeviceAdminAdd extends Activity {
    Button mActionButton;
    TextView mAddMsg;
    ImageView mAddMsgExpander;
    CharSequence mAddMsgText;
    boolean mAdding;
    boolean mAddingProfileOwner;
    TextView mAdminDescription;
    ImageView mAdminIcon;
    TextView mAdminName;
    ViewGroup mAdminPolicies;
    boolean mAdminPoliciesInitialized;
    TextView mAdminWarning;
    Button mCancelButton;
    DevicePolicyManager mDPM;
    DeviceAdminInfo mDeviceAdmin;
    Handler mHandler;
    String mProfileOwnerName;
    TextView mProfileOwnerWarning;
    boolean mRefreshing;
    TextView mSupportMessage;
    Button mUninstallButton;
    boolean mWaitingForRemoveMsg;
    boolean mAddMsgEllipsized = true;
    boolean mUninstalling = false;
    boolean mIsCalledFromSupportDialog = false;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        getWindow().addPrivateFlags(524288);
        this.mHandler = new Handler(getMainLooper());
        this.mDPM = (DevicePolicyManager) getSystemService("device_policy");
        PackageManager packageManager = getPackageManager();
        if ((getIntent().getFlags() & 268435456) != 0) {
            Log.w("DeviceAdminAdd", "Cannot start ADD_DEVICE_ADMIN as a new task");
            finish();
            return;
        }
        this.mIsCalledFromSupportDialog = getIntent().getBooleanExtra("android.app.extra.CALLED_FROM_SUPPORT_DIALOG", false);
        String action = getIntent().getAction();
        ComponentName who = (ComponentName) getIntent().getParcelableExtra("android.app.extra.DEVICE_ADMIN");
        if (who == null) {
            String packageName = getIntent().getStringExtra("android.app.extra.DEVICE_ADMIN_PACKAGE_NAME");
            Iterator component$iterator = this.mDPM.getActiveAdmins().iterator();
            while (true) {
                if (!component$iterator.hasNext()) {
                    break;
                }
                ComponentName component = (ComponentName) component$iterator.next();
                if (component.getPackageName().equals(packageName)) {
                    who = component;
                    this.mUninstalling = true;
                    break;
                }
            }
            if (who == null) {
                Log.w("DeviceAdminAdd", "No component specified in " + action);
                finish();
                return;
            }
        }
        if (action != null && action.equals("android.app.action.SET_PROFILE_OWNER")) {
            setResult(0);
            setFinishOnTouchOutside(true);
            this.mAddingProfileOwner = true;
            this.mProfileOwnerName = getIntent().getStringExtra("android.app.extra.PROFILE_OWNER_NAME");
            String callingPackage = getCallingPackage();
            if (callingPackage == null || !callingPackage.equals(who.getPackageName())) {
                Log.e("DeviceAdminAdd", "Unknown or incorrect caller");
                finish();
                return;
            }
            try {
                PackageInfo packageInfo = packageManager.getPackageInfo(callingPackage, 0);
                if ((packageInfo.applicationInfo.flags & 1) == 0) {
                    Log.e("DeviceAdminAdd", "Cannot set a non-system app as a profile owner");
                    finish();
                    return;
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.e("DeviceAdminAdd", "Cannot find the package " + callingPackage);
                finish();
                return;
            }
        }
        try {
            ActivityInfo ai = packageManager.getReceiverInfo(who, 128);
            if (!this.mDPM.isAdminActive(who)) {
                List<ResolveInfo> avail = packageManager.queryBroadcastReceivers(new Intent("android.app.action.DEVICE_ADMIN_ENABLED"), 32768);
                int count = avail == null ? 0 : avail.size();
                boolean found = false;
                int i = 0;
                while (true) {
                    if (i >= count) {
                        break;
                    }
                    ResolveInfo ri = avail.get(i);
                    if (!ai.packageName.equals(ri.activityInfo.packageName) || !ai.name.equals(ri.activityInfo.name)) {
                        i++;
                    } else {
                        try {
                            ri.activityInfo = ai;
                            new DeviceAdminInfo(this, ri);
                            found = true;
                            break;
                        } catch (IOException e2) {
                            Log.w("DeviceAdminAdd", "Bad " + ri.activityInfo, e2);
                        } catch (XmlPullParserException e3) {
                            Log.w("DeviceAdminAdd", "Bad " + ri.activityInfo, e3);
                        }
                    }
                }
                if (!found) {
                    Log.w("DeviceAdminAdd", "Request to add invalid device admin: " + who);
                    finish();
                    return;
                }
            }
            ResolveInfo ri2 = new ResolveInfo();
            ri2.activityInfo = ai;
            try {
                this.mDeviceAdmin = new DeviceAdminInfo(this, ri2);
                if ("android.app.action.ADD_DEVICE_ADMIN".equals(getIntent().getAction())) {
                    this.mRefreshing = false;
                    if (this.mDPM.isAdminActive(who)) {
                        if (this.mDPM.isRemovingAdmin(who, Process.myUserHandle().getIdentifier())) {
                            Log.w("DeviceAdminAdd", "Requested admin is already being removed: " + who);
                            finish();
                            return;
                        }
                        ArrayList<DeviceAdminInfo.PolicyInfo> newPolicies = this.mDeviceAdmin.getUsedPolicies();
                        int i2 = 0;
                        while (true) {
                            if (i2 >= newPolicies.size()) {
                                break;
                            }
                            DeviceAdminInfo.PolicyInfo pi = newPolicies.get(i2);
                            if (this.mDPM.hasGrantedPolicy(who, pi.ident)) {
                                i2++;
                            } else {
                                this.mRefreshing = true;
                                break;
                            }
                        }
                        if (!this.mRefreshing) {
                            setResult(-1);
                            finish();
                            return;
                        }
                    }
                }
                if (this.mAddingProfileOwner && !this.mDPM.hasUserSetupCompleted()) {
                    addAndFinish();
                    return;
                }
                this.mAddMsgText = getIntent().getCharSequenceExtra("android.app.extra.ADD_EXPLANATION");
                setContentView(R.layout.device_admin_add);
                this.mAdminIcon = (ImageView) findViewById(R.id.admin_icon);
                this.mAdminName = (TextView) findViewById(R.id.admin_name);
                this.mAdminDescription = (TextView) findViewById(R.id.admin_description);
                this.mProfileOwnerWarning = (TextView) findViewById(R.id.profile_owner_warning);
                this.mAddMsg = (TextView) findViewById(R.id.add_msg);
                this.mAddMsgExpander = (ImageView) findViewById(R.id.add_msg_expander);
                View.OnClickListener onClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        DeviceAdminAdd.this.toggleMessageEllipsis(DeviceAdminAdd.this.mAddMsg);
                    }
                };
                this.mAddMsgExpander.setOnClickListener(onClickListener);
                this.mAddMsg.setOnClickListener(onClickListener);
                this.mAddMsg.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        int maxLines = DeviceAdminAdd.this.getEllipsizedLines();
                        boolean hideMsgExpander = DeviceAdminAdd.this.mAddMsg.getLineCount() <= maxLines;
                        DeviceAdminAdd.this.mAddMsgExpander.setVisibility(hideMsgExpander ? 8 : 0);
                        if (hideMsgExpander) {
                            DeviceAdminAdd.this.mAddMsg.setOnClickListener(null);
                            ((View) DeviceAdminAdd.this.mAddMsgExpander.getParent()).invalidate();
                        }
                        DeviceAdminAdd.this.mAddMsg.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });
                toggleMessageEllipsis(this.mAddMsg);
                this.mAdminWarning = (TextView) findViewById(R.id.admin_warning);
                this.mAdminPolicies = (ViewGroup) findViewById(R.id.admin_policies);
                this.mSupportMessage = (TextView) findViewById(R.id.admin_support_message);
                this.mCancelButton = (Button) findViewById(R.id.cancel_button);
                this.mCancelButton.setFilterTouchesWhenObscured(true);
                this.mCancelButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        EventLog.writeEvent(90202, DeviceAdminAdd.this.mDeviceAdmin.getActivityInfo().applicationInfo.uid);
                        DeviceAdminAdd.this.finish();
                    }
                });
                this.mUninstallButton = (Button) findViewById(R.id.uninstall_button);
                this.mUninstallButton.setFilterTouchesWhenObscured(true);
                this.mUninstallButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        EventLog.writeEvent(90203, DeviceAdminAdd.this.mDeviceAdmin.getActivityInfo().applicationInfo.uid);
                        DeviceAdminAdd.this.mDPM.uninstallPackageWithActiveAdmins(DeviceAdminAdd.this.mDeviceAdmin.getPackageName());
                        DeviceAdminAdd.this.finish();
                    }
                });
                this.mActionButton = (Button) findViewById(R.id.action_button);
                this.mActionButton.setFilterTouchesWhenObscured(true);
                this.mActionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (DeviceAdminAdd.this.mAdding) {
                            DeviceAdminAdd.this.addAndFinish();
                            return;
                        }
                        if (DeviceAdminAdd.this.isManagedProfile(DeviceAdminAdd.this.mDeviceAdmin) && DeviceAdminAdd.this.mDeviceAdmin.getComponent().equals(DeviceAdminAdd.this.mDPM.getProfileOwner())) {
                            final int userId = UserHandle.myUserId();
                            UserDialogs.createRemoveDialog(DeviceAdminAdd.this, userId, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    UserManager um = UserManager.get(DeviceAdminAdd.this);
                                    um.removeUser(userId);
                                    DeviceAdminAdd.this.finish();
                                }
                            }).show();
                        } else if (DeviceAdminAdd.this.mUninstalling) {
                            DeviceAdminAdd.this.mDPM.uninstallPackageWithActiveAdmins(DeviceAdminAdd.this.mDeviceAdmin.getPackageName());
                            DeviceAdminAdd.this.finish();
                        } else {
                            if (DeviceAdminAdd.this.mWaitingForRemoveMsg) {
                                return;
                            }
                            try {
                                ActivityManagerNative.getDefault().stopAppSwitches();
                            } catch (RemoteException e4) {
                            }
                            DeviceAdminAdd.this.mWaitingForRemoveMsg = true;
                            DeviceAdminAdd.this.mDPM.getRemoveWarning(DeviceAdminAdd.this.mDeviceAdmin.getComponent(), new RemoteCallback(new RemoteCallback.OnResultListener() {
                                public void onResult(Bundle result) {
                                    CharSequence charSequence;
                                    if (result != null) {
                                        charSequence = result.getCharSequence("android.app.extra.DISABLE_WARNING");
                                    } else {
                                        charSequence = null;
                                    }
                                    DeviceAdminAdd.this.continueRemoveAction(charSequence);
                                }
                            }, DeviceAdminAdd.this.mHandler));
                            DeviceAdminAdd.this.getWindow().getDecorView().getHandler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    DeviceAdminAdd.this.continueRemoveAction(null);
                                }
                            }, 2000L);
                        }
                    }
                });
            } catch (IOException e4) {
                Log.w("DeviceAdminAdd", "Unable to retrieve device policy " + who, e4);
                finish();
            } catch (XmlPullParserException e5) {
                Log.w("DeviceAdminAdd", "Unable to retrieve device policy " + who, e5);
                finish();
            }
        } catch (PackageManager.NameNotFoundException e6) {
            Log.w("DeviceAdminAdd", "Unable to retrieve device policy " + who, e6);
            finish();
        }
    }

    void addAndFinish() {
        try {
            this.mDPM.setActiveAdmin(this.mDeviceAdmin.getComponent(), this.mRefreshing);
            EventLog.writeEvent(90201, this.mDeviceAdmin.getActivityInfo().applicationInfo.uid);
            setResult(-1);
        } catch (RuntimeException e) {
            Log.w("DeviceAdminAdd", "Exception trying to activate admin " + this.mDeviceAdmin.getComponent(), e);
            if (this.mDPM.isAdminActive(this.mDeviceAdmin.getComponent())) {
                setResult(-1);
            }
        }
        if (this.mAddingProfileOwner) {
            try {
                this.mDPM.setProfileOwner(this.mDeviceAdmin.getComponent(), this.mProfileOwnerName, UserHandle.myUserId());
            } catch (RuntimeException e2) {
                setResult(0);
            }
        }
        finish();
    }

    void continueRemoveAction(CharSequence msg) {
        if (!this.mWaitingForRemoveMsg) {
            return;
        }
        this.mWaitingForRemoveMsg = false;
        if (msg == null) {
            try {
                ActivityManagerNative.getDefault().resumeAppSwitches();
            } catch (RemoteException e) {
            }
            this.mDPM.removeActiveAdmin(this.mDeviceAdmin.getComponent());
            finish();
        } else {
            try {
                ActivityManagerNative.getDefault().stopAppSwitches();
            } catch (RemoteException e2) {
            }
            Bundle args = new Bundle();
            args.putCharSequence("android.app.extra.DISABLE_WARNING", msg);
            showDialog(1, args);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.mActionButton.setEnabled(true);
        updateInterface();
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.mActionButton.setEnabled(false);
        try {
            ActivityManagerNative.getDefault().resumeAppSwitches();
        } catch (RemoteException e) {
        }
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (!this.mIsCalledFromSupportDialog) {
            return;
        }
        finish();
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case DefaultWfcSettingsExt.PAUSE:
                CharSequence msg = args.getCharSequence("android.app.extra.DISABLE_WARNING");
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(msg);
                builder.setPositiveButton(R.string.dlg_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            ActivityManagerNative.getDefault().resumeAppSwitches();
                        } catch (RemoteException e) {
                        }
                        DeviceAdminAdd.this.mDPM.removeActiveAdmin(DeviceAdminAdd.this.mDeviceAdmin.getComponent());
                        DeviceAdminAdd.this.finish();
                    }
                });
                builder.setNegativeButton(R.string.dlg_cancel, (DialogInterface.OnClickListener) null);
                return builder.create();
            default:
                return super.onCreateDialog(id, args);
        }
    }

    void updateInterface() {
        this.mAdminIcon.setImageDrawable(this.mDeviceAdmin.loadIcon(getPackageManager()));
        this.mAdminName.setText(this.mDeviceAdmin.loadLabel(getPackageManager()));
        try {
            this.mAdminDescription.setText(this.mDeviceAdmin.loadDescription(getPackageManager()));
            this.mAdminDescription.setVisibility(0);
        } catch (Resources.NotFoundException e) {
            this.mAdminDescription.setVisibility(8);
        }
        if (this.mAddingProfileOwner) {
            this.mProfileOwnerWarning.setVisibility(0);
        }
        if (this.mAddMsgText != null) {
            this.mAddMsg.setText(this.mAddMsgText);
            this.mAddMsg.setVisibility(0);
        } else {
            this.mAddMsg.setVisibility(8);
            this.mAddMsgExpander.setVisibility(8);
        }
        if (!this.mRefreshing && !this.mAddingProfileOwner && this.mDPM.isAdminActive(this.mDeviceAdmin.getComponent())) {
            this.mAdding = false;
            boolean isProfileOwner = this.mDeviceAdmin.getComponent().equals(this.mDPM.getProfileOwner());
            boolean isManagedProfile = isManagedProfile(this.mDeviceAdmin);
            if (isProfileOwner && isManagedProfile) {
                this.mAdminWarning.setText(R.string.admin_profile_owner_message);
                this.mActionButton.setText(R.string.remove_managed_profile_label);
            } else if (isProfileOwner || this.mDeviceAdmin.getComponent().equals(this.mDPM.getDeviceOwnerComponentOnCallingUser())) {
                if (isProfileOwner) {
                    this.mAdminWarning.setText(R.string.admin_profile_owner_user_message);
                } else {
                    this.mAdminWarning.setText(R.string.admin_device_owner_message);
                }
                this.mActionButton.setText(R.string.remove_device_admin);
                this.mActionButton.setEnabled(false);
            } else {
                addDeviceAdminPolicies(false);
                this.mAdminWarning.setText(getString(R.string.device_admin_status, new Object[]{this.mDeviceAdmin.getActivityInfo().applicationInfo.loadLabel(getPackageManager())}));
                setTitle(R.string.active_device_admin_msg);
                if (this.mUninstalling) {
                    this.mActionButton.setText(R.string.remove_and_uninstall_device_admin);
                } else {
                    this.mActionButton.setText(R.string.remove_device_admin);
                }
            }
            CharSequence supportMessage = this.mDPM.getLongSupportMessageForUser(this.mDeviceAdmin.getComponent(), UserHandle.myUserId());
            if (!TextUtils.isEmpty(supportMessage)) {
                this.mSupportMessage.setText(supportMessage);
                this.mSupportMessage.setVisibility(0);
                return;
            } else {
                this.mSupportMessage.setVisibility(8);
                return;
            }
        }
        addDeviceAdminPolicies(true);
        this.mAdminWarning.setText(getString(R.string.device_admin_warning, new Object[]{this.mDeviceAdmin.getActivityInfo().applicationInfo.loadLabel(getPackageManager())}));
        if (this.mAddingProfileOwner) {
            setTitle(getText(R.string.profile_owner_add_title));
        } else {
            setTitle(getText(R.string.add_device_admin_msg));
        }
        this.mActionButton.setText(getText(R.string.add_device_admin));
        if (isAdminUninstallable()) {
            this.mUninstallButton.setVisibility(0);
        }
        this.mSupportMessage.setVisibility(8);
        this.mAdding = true;
    }

    private void addDeviceAdminPolicies(boolean showDescription) {
        if (this.mAdminPoliciesInitialized) {
            return;
        }
        boolean isAdminUser = UserManager.get(this).isAdminUser();
        for (DeviceAdminInfo.PolicyInfo pi : this.mDeviceAdmin.getUsedPolicies()) {
            int descriptionId = isAdminUser ? pi.description : pi.descriptionForSecondaryUsers;
            int labelId = isAdminUser ? pi.label : pi.labelForSecondaryUsers;
            View view = AppSecurityPermissions.getPermissionItemView(this, getText(labelId), showDescription ? getText(descriptionId) : "", true);
            this.mAdminPolicies.addView(view);
        }
        this.mAdminPoliciesInitialized = true;
    }

    void toggleMessageEllipsis(View v) {
        int i;
        TextView tv = (TextView) v;
        this.mAddMsgEllipsized = !this.mAddMsgEllipsized;
        tv.setEllipsize(this.mAddMsgEllipsized ? TextUtils.TruncateAt.END : null);
        tv.setMaxLines(this.mAddMsgEllipsized ? getEllipsizedLines() : 15);
        ImageView imageView = this.mAddMsgExpander;
        if (this.mAddMsgEllipsized) {
            i = android.R.drawable.btn_tonal;
        } else {
            i = android.R.drawable.btn_toggle_on_pressed_holo_light;
        }
        imageView.setImageResource(i);
    }

    int getEllipsizedLines() {
        android.view.Display d = ((WindowManager) getSystemService("window")).getDefaultDisplay();
        return d.getHeight() > d.getWidth() ? 5 : 2;
    }

    public boolean isManagedProfile(DeviceAdminInfo adminInfo) {
        UserManager um = UserManager.get(this);
        UserInfo info = um.getUserInfo(UserHandle.getUserId(adminInfo.getActivityInfo().applicationInfo.uid));
        if (info != null) {
            return info.isManagedProfile();
        }
        return false;
    }

    private boolean isAdminUninstallable() {
        return !this.mDeviceAdmin.getActivityInfo().applicationInfo.isSystemApp();
    }
}
