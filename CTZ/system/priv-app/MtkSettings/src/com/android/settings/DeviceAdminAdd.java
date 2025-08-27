package com.android.settings;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.Dialog;
import android.app.admin.DeviceAdminInfo;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.Log;
import android.util.Pair;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.AppSecurityPermissions;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.users.UserDialogs;
import com.android.settingslib.RestrictedLockUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import org.xmlpull.v1.XmlPullParserException;

/* loaded from: classes.dex */
public class DeviceAdminAdd extends Activity {
    Button mActionButton;
    TextView mAddMsg;
    ImageView mAddMsgExpander;
    String mAddMsgText;
    boolean mAdding;
    boolean mAddingProfileOwner;
    TextView mAdminDescription;
    ImageView mAdminIcon;
    TextView mAdminName;
    ViewGroup mAdminPolicies;
    boolean mAdminPoliciesInitialized;
    TextView mAdminWarning;
    AppOpsManager mAppOps;
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
    private final IBinder mToken = new Binder();
    boolean mAddMsgEllipsized = true;
    boolean mUninstalling = false;
    boolean mIsCalledFromSupportDialog = false;

    /* JADX WARN: Code restructure failed: missing block: B:45:0x014c, code lost:
    
        r6.activityInfo = r0;
        new android.app.admin.DeviceAdminInfo(r9, r6);
     */
    /* JADX WARN: Code restructure failed: missing block: B:46:0x0155, code lost:
    
        r10 = true;
     */
    /* JADX WARN: Removed duplicated region for block: B:54:0x0191  */
    /* JADX WARN: Removed duplicated region for block: B:60:0x01ca  */
    /* JADX WARN: Removed duplicated region for block: B:79:0x0230  */
    /* JADX WARN: Removed duplicated region for block: B:85:0x0248  */
    @Override // android.app.Activity
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    protected void onCreate(Bundle bundle) throws PackageManager.NameNotFoundException {
        CharSequence charSequenceExtra;
        int size;
        boolean z;
        super.onCreate(bundle);
        this.mHandler = new Handler(getMainLooper());
        this.mDPM = (DevicePolicyManager) getSystemService("device_policy");
        this.mAppOps = (AppOpsManager) getSystemService("appops");
        PackageManager packageManager = getPackageManager();
        if ((getIntent().getFlags() & 268435456) != 0) {
            Log.w("DeviceAdminAdd", "Cannot start ADD_DEVICE_ADMIN as a new task");
            finish();
            return;
        }
        int i = 0;
        this.mIsCalledFromSupportDialog = getIntent().getBooleanExtra("android.app.extra.CALLED_FROM_SUPPORT_DIALOG", false);
        String action = getIntent().getAction();
        ComponentName componentName = (ComponentName) getIntent().getParcelableExtra("android.app.extra.DEVICE_ADMIN");
        if (componentName == null) {
            Optional<ComponentName> optionalFindAdminWithPackageName = findAdminWithPackageName(getIntent().getStringExtra("android.app.extra.DEVICE_ADMIN_PACKAGE_NAME"));
            if (!optionalFindAdminWithPackageName.isPresent()) {
                Log.w("DeviceAdminAdd", "No component specified in " + action);
                finish();
                return;
            }
            componentName = optionalFindAdminWithPackageName.get();
            this.mUninstalling = true;
        }
        if (action != null && action.equals("android.app.action.SET_PROFILE_OWNER")) {
            setResult(0);
            setFinishOnTouchOutside(true);
            this.mAddingProfileOwner = true;
            this.mProfileOwnerName = getIntent().getStringExtra("android.app.extra.PROFILE_OWNER_NAME");
            String callingPackage = getCallingPackage();
            if (callingPackage != null && callingPackage.equals(componentName.getPackageName())) {
                try {
                    if ((packageManager.getPackageInfo(callingPackage, 0).applicationInfo.flags & 1) == 0) {
                        Log.e("DeviceAdminAdd", "Cannot set a non-system app as a profile owner");
                        finish();
                        return;
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e("DeviceAdminAdd", "Cannot find the package " + callingPackage);
                    finish();
                    return;
                }
            } else {
                Log.e("DeviceAdminAdd", "Unknown or incorrect caller");
                finish();
                return;
            }
        }
        try {
            ActivityInfo receiverInfo = packageManager.getReceiverInfo(componentName, 128);
            if (!this.mDPM.isAdminActive(componentName)) {
                List<ResolveInfo> listQueryBroadcastReceivers = packageManager.queryBroadcastReceivers(new Intent("android.app.action.DEVICE_ADMIN_ENABLED"), 32768);
                if (listQueryBroadcastReceivers != null) {
                    size = listQueryBroadcastReceivers.size();
                } else {
                    size = 0;
                }
                int i2 = 0;
                while (true) {
                    if (i2 >= size) {
                        break;
                    }
                    ResolveInfo resolveInfo = listQueryBroadcastReceivers.get(i2);
                    if (!receiverInfo.packageName.equals(resolveInfo.activityInfo.packageName) || !receiverInfo.name.equals(resolveInfo.activityInfo.name)) {
                        i2++;
                    } else {
                        try {
                            break;
                        } catch (IOException e2) {
                            Log.w("DeviceAdminAdd", "Bad " + resolveInfo.activityInfo, e2);
                            z = false;
                            if (!z) {
                            }
                            ResolveInfo resolveInfo2 = new ResolveInfo();
                            resolveInfo2.activityInfo = receiverInfo;
                            this.mDeviceAdmin = new DeviceAdminInfo(this, resolveInfo2);
                            if ("android.app.action.ADD_DEVICE_ADMIN".equals(getIntent().getAction())) {
                            }
                            if (!this.mAddingProfileOwner) {
                            }
                            charSequenceExtra = getIntent().getCharSequenceExtra("android.app.extra.ADD_EXPLANATION");
                            if (charSequenceExtra != null) {
                            }
                            setContentView(R.layout.device_admin_add);
                            this.mAdminIcon = (ImageView) findViewById(R.id.admin_icon);
                            this.mAdminName = (TextView) findViewById(R.id.admin_name);
                            this.mAdminDescription = (TextView) findViewById(R.id.admin_description);
                            this.mProfileOwnerWarning = (TextView) findViewById(R.id.profile_owner_warning);
                            this.mAddMsg = (TextView) findViewById(R.id.add_msg);
                            this.mAddMsgExpander = (ImageView) findViewById(R.id.add_msg_expander);
                            View.OnClickListener onClickListener = new View.OnClickListener() { // from class: com.android.settings.DeviceAdminAdd.1
                                @Override // android.view.View.OnClickListener
                                public void onClick(View view) {
                                    DeviceAdminAdd.this.toggleMessageEllipsis(DeviceAdminAdd.this.mAddMsg);
                                }
                            };
                            this.mAddMsgExpander.setOnClickListener(onClickListener);
                            this.mAddMsg.setOnClickListener(onClickListener);
                            this.mAddMsg.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() { // from class: com.android.settings.DeviceAdminAdd.2
                                @Override // android.view.ViewTreeObserver.OnGlobalLayoutListener
                                public void onGlobalLayout() {
                                    boolean z2 = DeviceAdminAdd.this.mAddMsg.getLineCount() <= DeviceAdminAdd.this.getEllipsizedLines();
                                    DeviceAdminAdd.this.mAddMsgExpander.setVisibility(z2 ? 8 : 0);
                                    if (z2) {
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
                            this.mCancelButton.setOnClickListener(new View.OnClickListener() { // from class: com.android.settings.DeviceAdminAdd.3
                                @Override // android.view.View.OnClickListener
                                public void onClick(View view) {
                                    EventLog.writeEvent(90202, DeviceAdminAdd.this.mDeviceAdmin.getActivityInfo().applicationInfo.uid);
                                    DeviceAdminAdd.this.finish();
                                }
                            });
                            this.mUninstallButton = (Button) findViewById(R.id.uninstall_button);
                            this.mUninstallButton.setFilterTouchesWhenObscured(true);
                            this.mUninstallButton.setOnClickListener(new View.OnClickListener() { // from class: com.android.settings.DeviceAdminAdd.4
                                @Override // android.view.View.OnClickListener
                                public void onClick(View view) {
                                    EventLog.writeEvent(90203, DeviceAdminAdd.this.mDeviceAdmin.getActivityInfo().applicationInfo.uid);
                                    DeviceAdminAdd.this.mDPM.uninstallPackageWithActiveAdmins(DeviceAdminAdd.this.mDeviceAdmin.getPackageName());
                                    DeviceAdminAdd.this.finish();
                                }
                            });
                            this.mActionButton = (Button) findViewById(R.id.action_button);
                            View viewFindViewById = findViewById(R.id.restricted_action);
                            viewFindViewById.setFilterTouchesWhenObscured(true);
                            viewFindViewById.setOnClickListener(new View.OnClickListener() { // from class: com.android.settings.DeviceAdminAdd.5
                                @Override // android.view.View.OnClickListener
                                public void onClick(View view) {
                                    if (!DeviceAdminAdd.this.mActionButton.isEnabled()) {
                                        DeviceAdminAdd.this.showPolicyTransparencyDialogIfRequired();
                                        return;
                                    }
                                    if (!DeviceAdminAdd.this.mAdding) {
                                        if (DeviceAdminAdd.this.isManagedProfile(DeviceAdminAdd.this.mDeviceAdmin) && DeviceAdminAdd.this.mDeviceAdmin.getComponent().equals(DeviceAdminAdd.this.mDPM.getProfileOwner())) {
                                            final int iMyUserId = UserHandle.myUserId();
                                            UserDialogs.createRemoveDialog(DeviceAdminAdd.this, iMyUserId, new DialogInterface.OnClickListener() { // from class: com.android.settings.DeviceAdminAdd.5.1
                                                @Override // android.content.DialogInterface.OnClickListener
                                                public void onClick(DialogInterface dialogInterface, int i3) {
                                                    UserManager.get(DeviceAdminAdd.this).removeUser(iMyUserId);
                                                    DeviceAdminAdd.this.finish();
                                                }
                                            }).show();
                                            return;
                                        } else if (DeviceAdminAdd.this.mUninstalling) {
                                            DeviceAdminAdd.this.mDPM.uninstallPackageWithActiveAdmins(DeviceAdminAdd.this.mDeviceAdmin.getPackageName());
                                            DeviceAdminAdd.this.finish();
                                            return;
                                        } else {
                                            if (!DeviceAdminAdd.this.mWaitingForRemoveMsg) {
                                                try {
                                                    ActivityManager.getService().stopAppSwitches();
                                                } catch (RemoteException e3) {
                                                }
                                                DeviceAdminAdd.this.mWaitingForRemoveMsg = true;
                                                DeviceAdminAdd.this.mDPM.getRemoveWarning(DeviceAdminAdd.this.mDeviceAdmin.getComponent(), new RemoteCallback(new RemoteCallback.OnResultListener() { // from class: com.android.settings.DeviceAdminAdd.5.2
                                                    public void onResult(Bundle bundle2) {
                                                        CharSequence charSequence;
                                                        if (bundle2 != null) {
                                                            charSequence = bundle2.getCharSequence("android.app.extra.DISABLE_WARNING");
                                                        } else {
                                                            charSequence = null;
                                                        }
                                                        DeviceAdminAdd.this.continueRemoveAction(charSequence);
                                                    }
                                                }, DeviceAdminAdd.this.mHandler));
                                                DeviceAdminAdd.this.getWindow().getDecorView().getHandler().postDelayed(new Runnable() { // from class: com.android.settings.DeviceAdminAdd.5.3
                                                    @Override // java.lang.Runnable
                                                    public void run() {
                                                        DeviceAdminAdd.this.continueRemoveAction(null);
                                                    }
                                                }, 2000L);
                                                return;
                                            }
                                            return;
                                        }
                                    }
                                    DeviceAdminAdd.this.addAndFinish();
                                }
                            });
                        } catch (XmlPullParserException e3) {
                            Log.w("DeviceAdminAdd", "Bad " + resolveInfo.activityInfo, e3);
                            z = false;
                            if (!z) {
                            }
                            ResolveInfo resolveInfo22 = new ResolveInfo();
                            resolveInfo22.activityInfo = receiverInfo;
                            this.mDeviceAdmin = new DeviceAdminInfo(this, resolveInfo22);
                            if ("android.app.action.ADD_DEVICE_ADMIN".equals(getIntent().getAction())) {
                            }
                            if (!this.mAddingProfileOwner) {
                            }
                            charSequenceExtra = getIntent().getCharSequenceExtra("android.app.extra.ADD_EXPLANATION");
                            if (charSequenceExtra != null) {
                            }
                            setContentView(R.layout.device_admin_add);
                            this.mAdminIcon = (ImageView) findViewById(R.id.admin_icon);
                            this.mAdminName = (TextView) findViewById(R.id.admin_name);
                            this.mAdminDescription = (TextView) findViewById(R.id.admin_description);
                            this.mProfileOwnerWarning = (TextView) findViewById(R.id.profile_owner_warning);
                            this.mAddMsg = (TextView) findViewById(R.id.add_msg);
                            this.mAddMsgExpander = (ImageView) findViewById(R.id.add_msg_expander);
                            View.OnClickListener onClickListener2 = new View.OnClickListener() { // from class: com.android.settings.DeviceAdminAdd.1
                                @Override // android.view.View.OnClickListener
                                public void onClick(View view) {
                                    DeviceAdminAdd.this.toggleMessageEllipsis(DeviceAdminAdd.this.mAddMsg);
                                }
                            };
                            this.mAddMsgExpander.setOnClickListener(onClickListener2);
                            this.mAddMsg.setOnClickListener(onClickListener2);
                            this.mAddMsg.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() { // from class: com.android.settings.DeviceAdminAdd.2
                                @Override // android.view.ViewTreeObserver.OnGlobalLayoutListener
                                public void onGlobalLayout() {
                                    boolean z2 = DeviceAdminAdd.this.mAddMsg.getLineCount() <= DeviceAdminAdd.this.getEllipsizedLines();
                                    DeviceAdminAdd.this.mAddMsgExpander.setVisibility(z2 ? 8 : 0);
                                    if (z2) {
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
                            this.mCancelButton.setOnClickListener(new View.OnClickListener() { // from class: com.android.settings.DeviceAdminAdd.3
                                @Override // android.view.View.OnClickListener
                                public void onClick(View view) {
                                    EventLog.writeEvent(90202, DeviceAdminAdd.this.mDeviceAdmin.getActivityInfo().applicationInfo.uid);
                                    DeviceAdminAdd.this.finish();
                                }
                            });
                            this.mUninstallButton = (Button) findViewById(R.id.uninstall_button);
                            this.mUninstallButton.setFilterTouchesWhenObscured(true);
                            this.mUninstallButton.setOnClickListener(new View.OnClickListener() { // from class: com.android.settings.DeviceAdminAdd.4
                                @Override // android.view.View.OnClickListener
                                public void onClick(View view) {
                                    EventLog.writeEvent(90203, DeviceAdminAdd.this.mDeviceAdmin.getActivityInfo().applicationInfo.uid);
                                    DeviceAdminAdd.this.mDPM.uninstallPackageWithActiveAdmins(DeviceAdminAdd.this.mDeviceAdmin.getPackageName());
                                    DeviceAdminAdd.this.finish();
                                }
                            });
                            this.mActionButton = (Button) findViewById(R.id.action_button);
                            View viewFindViewById2 = findViewById(R.id.restricted_action);
                            viewFindViewById2.setFilterTouchesWhenObscured(true);
                            viewFindViewById2.setOnClickListener(new View.OnClickListener() { // from class: com.android.settings.DeviceAdminAdd.5
                                @Override // android.view.View.OnClickListener
                                public void onClick(View view) {
                                    if (!DeviceAdminAdd.this.mActionButton.isEnabled()) {
                                        DeviceAdminAdd.this.showPolicyTransparencyDialogIfRequired();
                                        return;
                                    }
                                    if (!DeviceAdminAdd.this.mAdding) {
                                        if (DeviceAdminAdd.this.isManagedProfile(DeviceAdminAdd.this.mDeviceAdmin) && DeviceAdminAdd.this.mDeviceAdmin.getComponent().equals(DeviceAdminAdd.this.mDPM.getProfileOwner())) {
                                            final int iMyUserId = UserHandle.myUserId();
                                            UserDialogs.createRemoveDialog(DeviceAdminAdd.this, iMyUserId, new DialogInterface.OnClickListener() { // from class: com.android.settings.DeviceAdminAdd.5.1
                                                @Override // android.content.DialogInterface.OnClickListener
                                                public void onClick(DialogInterface dialogInterface, int i3) {
                                                    UserManager.get(DeviceAdminAdd.this).removeUser(iMyUserId);
                                                    DeviceAdminAdd.this.finish();
                                                }
                                            }).show();
                                            return;
                                        } else if (DeviceAdminAdd.this.mUninstalling) {
                                            DeviceAdminAdd.this.mDPM.uninstallPackageWithActiveAdmins(DeviceAdminAdd.this.mDeviceAdmin.getPackageName());
                                            DeviceAdminAdd.this.finish();
                                            return;
                                        } else {
                                            if (!DeviceAdminAdd.this.mWaitingForRemoveMsg) {
                                                try {
                                                    ActivityManager.getService().stopAppSwitches();
                                                } catch (RemoteException e32) {
                                                }
                                                DeviceAdminAdd.this.mWaitingForRemoveMsg = true;
                                                DeviceAdminAdd.this.mDPM.getRemoveWarning(DeviceAdminAdd.this.mDeviceAdmin.getComponent(), new RemoteCallback(new RemoteCallback.OnResultListener() { // from class: com.android.settings.DeviceAdminAdd.5.2
                                                    public void onResult(Bundle bundle2) {
                                                        CharSequence charSequence;
                                                        if (bundle2 != null) {
                                                            charSequence = bundle2.getCharSequence("android.app.extra.DISABLE_WARNING");
                                                        } else {
                                                            charSequence = null;
                                                        }
                                                        DeviceAdminAdd.this.continueRemoveAction(charSequence);
                                                    }
                                                }, DeviceAdminAdd.this.mHandler));
                                                DeviceAdminAdd.this.getWindow().getDecorView().getHandler().postDelayed(new Runnable() { // from class: com.android.settings.DeviceAdminAdd.5.3
                                                    @Override // java.lang.Runnable
                                                    public void run() {
                                                        DeviceAdminAdd.this.continueRemoveAction(null);
                                                    }
                                                }, 2000L);
                                                return;
                                            }
                                            return;
                                        }
                                    }
                                    DeviceAdminAdd.this.addAndFinish();
                                }
                            });
                        }
                    }
                }
                if (!z) {
                    Log.w("DeviceAdminAdd", "Request to add invalid device admin: " + componentName);
                    finish();
                    return;
                }
            }
            ResolveInfo resolveInfo222 = new ResolveInfo();
            resolveInfo222.activityInfo = receiverInfo;
            try {
                this.mDeviceAdmin = new DeviceAdminInfo(this, resolveInfo222);
                if ("android.app.action.ADD_DEVICE_ADMIN".equals(getIntent().getAction())) {
                    this.mRefreshing = false;
                    if (this.mDPM.isAdminActive(componentName)) {
                        if (this.mDPM.isRemovingAdmin(componentName, Process.myUserHandle().getIdentifier())) {
                            Log.w("DeviceAdminAdd", "Requested admin is already being removed: " + componentName);
                            finish();
                            return;
                        }
                        ArrayList usedPolicies = this.mDeviceAdmin.getUsedPolicies();
                        while (true) {
                            if (i >= usedPolicies.size()) {
                                break;
                            }
                            if (this.mDPM.hasGrantedPolicy(componentName, ((DeviceAdminInfo.PolicyInfo) usedPolicies.get(i)).ident)) {
                                i++;
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
                if (!this.mAddingProfileOwner && !this.mDPM.hasUserSetupCompleted()) {
                    addAndFinish();
                    return;
                }
                charSequenceExtra = getIntent().getCharSequenceExtra("android.app.extra.ADD_EXPLANATION");
                if (charSequenceExtra != null) {
                    this.mAddMsgText = charSequenceExtra.toString();
                }
                setContentView(R.layout.device_admin_add);
                this.mAdminIcon = (ImageView) findViewById(R.id.admin_icon);
                this.mAdminName = (TextView) findViewById(R.id.admin_name);
                this.mAdminDescription = (TextView) findViewById(R.id.admin_description);
                this.mProfileOwnerWarning = (TextView) findViewById(R.id.profile_owner_warning);
                this.mAddMsg = (TextView) findViewById(R.id.add_msg);
                this.mAddMsgExpander = (ImageView) findViewById(R.id.add_msg_expander);
                View.OnClickListener onClickListener22 = new View.OnClickListener() { // from class: com.android.settings.DeviceAdminAdd.1
                    @Override // android.view.View.OnClickListener
                    public void onClick(View view) {
                        DeviceAdminAdd.this.toggleMessageEllipsis(DeviceAdminAdd.this.mAddMsg);
                    }
                };
                this.mAddMsgExpander.setOnClickListener(onClickListener22);
                this.mAddMsg.setOnClickListener(onClickListener22);
                this.mAddMsg.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() { // from class: com.android.settings.DeviceAdminAdd.2
                    @Override // android.view.ViewTreeObserver.OnGlobalLayoutListener
                    public void onGlobalLayout() {
                        boolean z2 = DeviceAdminAdd.this.mAddMsg.getLineCount() <= DeviceAdminAdd.this.getEllipsizedLines();
                        DeviceAdminAdd.this.mAddMsgExpander.setVisibility(z2 ? 8 : 0);
                        if (z2) {
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
                this.mCancelButton.setOnClickListener(new View.OnClickListener() { // from class: com.android.settings.DeviceAdminAdd.3
                    @Override // android.view.View.OnClickListener
                    public void onClick(View view) {
                        EventLog.writeEvent(90202, DeviceAdminAdd.this.mDeviceAdmin.getActivityInfo().applicationInfo.uid);
                        DeviceAdminAdd.this.finish();
                    }
                });
                this.mUninstallButton = (Button) findViewById(R.id.uninstall_button);
                this.mUninstallButton.setFilterTouchesWhenObscured(true);
                this.mUninstallButton.setOnClickListener(new View.OnClickListener() { // from class: com.android.settings.DeviceAdminAdd.4
                    @Override // android.view.View.OnClickListener
                    public void onClick(View view) {
                        EventLog.writeEvent(90203, DeviceAdminAdd.this.mDeviceAdmin.getActivityInfo().applicationInfo.uid);
                        DeviceAdminAdd.this.mDPM.uninstallPackageWithActiveAdmins(DeviceAdminAdd.this.mDeviceAdmin.getPackageName());
                        DeviceAdminAdd.this.finish();
                    }
                });
                this.mActionButton = (Button) findViewById(R.id.action_button);
                View viewFindViewById22 = findViewById(R.id.restricted_action);
                viewFindViewById22.setFilterTouchesWhenObscured(true);
                viewFindViewById22.setOnClickListener(new View.OnClickListener() { // from class: com.android.settings.DeviceAdminAdd.5
                    @Override // android.view.View.OnClickListener
                    public void onClick(View view) {
                        if (!DeviceAdminAdd.this.mActionButton.isEnabled()) {
                            DeviceAdminAdd.this.showPolicyTransparencyDialogIfRequired();
                            return;
                        }
                        if (!DeviceAdminAdd.this.mAdding) {
                            if (DeviceAdminAdd.this.isManagedProfile(DeviceAdminAdd.this.mDeviceAdmin) && DeviceAdminAdd.this.mDeviceAdmin.getComponent().equals(DeviceAdminAdd.this.mDPM.getProfileOwner())) {
                                final int iMyUserId = UserHandle.myUserId();
                                UserDialogs.createRemoveDialog(DeviceAdminAdd.this, iMyUserId, new DialogInterface.OnClickListener() { // from class: com.android.settings.DeviceAdminAdd.5.1
                                    @Override // android.content.DialogInterface.OnClickListener
                                    public void onClick(DialogInterface dialogInterface, int i3) {
                                        UserManager.get(DeviceAdminAdd.this).removeUser(iMyUserId);
                                        DeviceAdminAdd.this.finish();
                                    }
                                }).show();
                                return;
                            } else if (DeviceAdminAdd.this.mUninstalling) {
                                DeviceAdminAdd.this.mDPM.uninstallPackageWithActiveAdmins(DeviceAdminAdd.this.mDeviceAdmin.getPackageName());
                                DeviceAdminAdd.this.finish();
                                return;
                            } else {
                                if (!DeviceAdminAdd.this.mWaitingForRemoveMsg) {
                                    try {
                                        ActivityManager.getService().stopAppSwitches();
                                    } catch (RemoteException e32) {
                                    }
                                    DeviceAdminAdd.this.mWaitingForRemoveMsg = true;
                                    DeviceAdminAdd.this.mDPM.getRemoveWarning(DeviceAdminAdd.this.mDeviceAdmin.getComponent(), new RemoteCallback(new RemoteCallback.OnResultListener() { // from class: com.android.settings.DeviceAdminAdd.5.2
                                        public void onResult(Bundle bundle2) {
                                            CharSequence charSequence;
                                            if (bundle2 != null) {
                                                charSequence = bundle2.getCharSequence("android.app.extra.DISABLE_WARNING");
                                            } else {
                                                charSequence = null;
                                            }
                                            DeviceAdminAdd.this.continueRemoveAction(charSequence);
                                        }
                                    }, DeviceAdminAdd.this.mHandler));
                                    DeviceAdminAdd.this.getWindow().getDecorView().getHandler().postDelayed(new Runnable() { // from class: com.android.settings.DeviceAdminAdd.5.3
                                        @Override // java.lang.Runnable
                                        public void run() {
                                            DeviceAdminAdd.this.continueRemoveAction(null);
                                        }
                                    }, 2000L);
                                    return;
                                }
                                return;
                            }
                        }
                        DeviceAdminAdd.this.addAndFinish();
                    }
                });
            } catch (IOException e4) {
                Log.w("DeviceAdminAdd", "Unable to retrieve device policy " + componentName, e4);
                finish();
            } catch (XmlPullParserException e5) {
                Log.w("DeviceAdminAdd", "Unable to retrieve device policy " + componentName, e5);
                finish();
            }
        } catch (PackageManager.NameNotFoundException e6) {
            Log.w("DeviceAdminAdd", "Unable to retrieve device policy " + componentName, e6);
            finish();
        }
    }

    private void showPolicyTransparencyDialogIfRequired() {
        RestrictedLockUtils.EnforcedAdmin adminEnforcingCantRemoveProfile;
        if (isManagedProfile(this.mDeviceAdmin) && this.mDeviceAdmin.getComponent().equals(this.mDPM.getProfileOwner()) && !hasBaseCantRemoveProfileRestriction() && (adminEnforcingCantRemoveProfile = getAdminEnforcingCantRemoveProfile()) != null) {
            RestrictedLockUtils.sendShowAdminSupportDetailsIntent(this, adminEnforcingCantRemoveProfile);
        }
    }

    void addAndFinish() {
        try {
            logSpecialPermissionChange(true, this.mDeviceAdmin.getComponent().getPackageName());
            this.mDPM.setActiveAdmin(this.mDeviceAdmin.getComponent(), this.mRefreshing);
            EventLog.writeEvent(90201, this.mDeviceAdmin.getActivityInfo().applicationInfo.uid);
            unrestrictAppIfPossible(BatteryUtils.getInstance(this));
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

    void unrestrictAppIfPossible(BatteryUtils batteryUtils) {
        String packageName = this.mDeviceAdmin.getComponent().getPackageName();
        int packageUid = batteryUtils.getPackageUid(packageName);
        if (batteryUtils.isForceAppStandbyEnabled(packageUid, packageName)) {
            batteryUtils.setForceAppStandby(packageUid, packageName, 0);
        }
    }

    void continueRemoveAction(CharSequence charSequence) {
        if (!this.mWaitingForRemoveMsg) {
            return;
        }
        this.mWaitingForRemoveMsg = false;
        if (charSequence == null) {
            try {
                ActivityManager.getService().resumeAppSwitches();
            } catch (RemoteException e) {
            }
            logSpecialPermissionChange(false, this.mDeviceAdmin.getComponent().getPackageName());
            this.mDPM.removeActiveAdmin(this.mDeviceAdmin.getComponent());
            finish();
            return;
        }
        try {
            ActivityManager.getService().stopAppSwitches();
        } catch (RemoteException e2) {
        }
        Bundle bundle = new Bundle();
        bundle.putCharSequence("android.app.extra.DISABLE_WARNING", charSequence);
        showDialog(1, bundle);
    }

    void logSpecialPermissionChange(boolean z, String str) {
        FeatureFactory.getFactory(this).getMetricsFeatureProvider().action(this, z ? 766 : 767, str, new Pair[0]);
    }

    @Override // android.app.Activity
    protected void onResume() {
        super.onResume();
        this.mActionButton.setEnabled(true);
        updateInterface();
        this.mAppOps.setUserRestriction(24, true, this.mToken);
        this.mAppOps.setUserRestriction(45, true, this.mToken);
    }

    @Override // android.app.Activity
    protected void onPause() {
        super.onPause();
        this.mActionButton.setEnabled(false);
        this.mAppOps.setUserRestriction(24, false, this.mToken);
        this.mAppOps.setUserRestriction(45, false, this.mToken);
        try {
            ActivityManager.getService().resumeAppSwitches();
        } catch (RemoteException e) {
        }
    }

    @Override // android.app.Activity
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (this.mIsCalledFromSupportDialog) {
            finish();
        }
    }

    @Override // android.app.Activity
    protected Dialog onCreateDialog(int i, Bundle bundle) {
        if (i == 1) {
            CharSequence charSequence = bundle.getCharSequence("android.app.extra.DISABLE_WARNING");
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(charSequence);
            builder.setPositiveButton(R.string.dlg_ok, new DialogInterface.OnClickListener() { // from class: com.android.settings.DeviceAdminAdd.6
                @Override // android.content.DialogInterface.OnClickListener
                public void onClick(DialogInterface dialogInterface, int i2) {
                    try {
                        ActivityManager.getService().resumeAppSwitches();
                    } catch (RemoteException e) {
                    }
                    DeviceAdminAdd.this.mDPM.removeActiveAdmin(DeviceAdminAdd.this.mDeviceAdmin.getComponent());
                    DeviceAdminAdd.this.finish();
                }
            });
            builder.setNegativeButton(R.string.dlg_cancel, (DialogInterface.OnClickListener) null);
            return builder.create();
        }
        return super.onCreateDialog(i, bundle);
    }

    void updateInterface() {
        findViewById(R.id.restricted_icon).setVisibility(8);
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
        if (!TextUtils.isEmpty(this.mAddMsgText)) {
            this.mAddMsg.setText(this.mAddMsgText);
            this.mAddMsg.setVisibility(0);
        } else {
            this.mAddMsg.setVisibility(8);
            this.mAddMsgExpander.setVisibility(8);
        }
        if (!this.mRefreshing && !this.mAddingProfileOwner && this.mDPM.isAdminActive(this.mDeviceAdmin.getComponent())) {
            this.mAdding = false;
            boolean zEquals = this.mDeviceAdmin.getComponent().equals(this.mDPM.getProfileOwner());
            boolean zIsManagedProfile = isManagedProfile(this.mDeviceAdmin);
            if (zEquals && zIsManagedProfile) {
                this.mAdminWarning.setText(R.string.admin_profile_owner_message);
                this.mActionButton.setText(R.string.remove_managed_profile_label);
                RestrictedLockUtils.EnforcedAdmin adminEnforcingCantRemoveProfile = getAdminEnforcingCantRemoveProfile();
                boolean zHasBaseCantRemoveProfileRestriction = hasBaseCantRemoveProfileRestriction();
                if (adminEnforcingCantRemoveProfile != null && !zHasBaseCantRemoveProfileRestriction) {
                    findViewById(R.id.restricted_icon).setVisibility(0);
                }
                this.mActionButton.setEnabled(adminEnforcingCantRemoveProfile == null && !zHasBaseCantRemoveProfileRestriction);
            } else if (zEquals || this.mDeviceAdmin.getComponent().equals(this.mDPM.getDeviceOwnerComponentOnCallingUser())) {
                if (zEquals) {
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
            CharSequence longSupportMessageForUser = this.mDPM.getLongSupportMessageForUser(this.mDeviceAdmin.getComponent(), UserHandle.myUserId());
            if (!TextUtils.isEmpty(longSupportMessageForUser)) {
                this.mSupportMessage.setText(longSupportMessageForUser);
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

    private RestrictedLockUtils.EnforcedAdmin getAdminEnforcingCantRemoveProfile() {
        return RestrictedLockUtils.checkIfRestrictionEnforced(this, "no_remove_managed_profile", getParentUserId());
    }

    private boolean hasBaseCantRemoveProfileRestriction() {
        return RestrictedLockUtils.hasBaseUserRestriction(this, "no_remove_managed_profile", getParentUserId());
    }

    private int getParentUserId() {
        return UserManager.get(this).getProfileParent(UserHandle.myUserId()).id;
    }

    private void addDeviceAdminPolicies(boolean z) {
        if (!this.mAdminPoliciesInitialized) {
            boolean zIsAdminUser = UserManager.get(this).isAdminUser();
            Iterator it = this.mDeviceAdmin.getUsedPolicies().iterator();
            while (it.hasNext()) {
                DeviceAdminInfo.PolicyInfo policyInfo = (DeviceAdminInfo.PolicyInfo) it.next();
                this.mAdminPolicies.addView(AppSecurityPermissions.getPermissionItemView(this, getText(zIsAdminUser ? policyInfo.label : policyInfo.labelForSecondaryUsers), z ? getText(zIsAdminUser ? policyInfo.description : policyInfo.descriptionForSecondaryUsers) : "", true));
            }
            this.mAdminPoliciesInitialized = true;
        }
    }

    void toggleMessageEllipsis(View view) {
        int i;
        TextView textView = (TextView) view;
        this.mAddMsgEllipsized = !this.mAddMsgEllipsized;
        textView.setEllipsize(this.mAddMsgEllipsized ? TextUtils.TruncateAt.END : null);
        textView.setMaxLines(this.mAddMsgEllipsized ? getEllipsizedLines() : 15);
        ImageView imageView = this.mAddMsgExpander;
        if (this.mAddMsgEllipsized) {
            i = android.R.drawable.compass_base;
        } else {
            i = android.R.drawable.compass_arrow;
        }
        imageView.setImageResource(i);
    }

    int getEllipsizedLines() {
        Display defaultDisplay = ((WindowManager) getSystemService("window")).getDefaultDisplay();
        return defaultDisplay.getHeight() > defaultDisplay.getWidth() ? 5 : 2;
    }

    private boolean isManagedProfile(DeviceAdminInfo deviceAdminInfo) {
        UserInfo userInfo = UserManager.get(this).getUserInfo(UserHandle.getUserId(deviceAdminInfo.getActivityInfo().applicationInfo.uid));
        if (userInfo != null) {
            return userInfo.isManagedProfile();
        }
        return false;
    }

    private Optional<ComponentName> findAdminWithPackageName(final String str) {
        List<ComponentName> activeAdmins = this.mDPM.getActiveAdmins();
        if (activeAdmins == null) {
            return Optional.empty();
        }
        return activeAdmins.stream().filter(new Predicate() { // from class: com.android.settings.-$$Lambda$DeviceAdminAdd$3kbf0VppdPbIFmWVVpDZ5dj27E4
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return ((ComponentName) obj).getPackageName().equals(str);
            }
        }).findAny();
    }

    private boolean isAdminUninstallable() {
        return !this.mDeviceAdmin.getActivityInfo().applicationInfo.isSystemApp();
    }
}
