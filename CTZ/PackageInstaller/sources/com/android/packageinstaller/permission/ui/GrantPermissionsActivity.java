package com.android.packageinstaller.permission.ui;

import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.content.pm.PermissionInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.text.Spanned;
import android.util.ArraySet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import com.android.internal.content.PackageMonitor;
import com.android.packageinstaller.DeviceUtils;
import com.android.packageinstaller.R;
import com.android.packageinstaller.permission.model.AppPermissionGroup;
import com.android.packageinstaller.permission.model.AppPermissions;
import com.android.packageinstaller.permission.model.Permission;
import com.android.packageinstaller.permission.ui.GrantPermissionsViewHandler;
import com.android.packageinstaller.permission.ui.auto.GrantPermissionsAutoViewHandler;
import com.android.packageinstaller.permission.ui.television.GrantPermissionsViewHandlerImpl;
import com.android.packageinstaller.permission.utils.ArrayUtils;
import com.android.packageinstaller.permission.utils.EventLogger;
import com.android.packageinstaller.permission.utils.SafetyNetLogger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;

public class GrantPermissionsActivity extends OverlayTouchActivity implements GrantPermissionsViewHandler.ResultListener {
    private AppPermissions mAppPermissions;
    private String mCallingPackage;
    private int[] mGrantResults;
    private PackageMonitor mPackageMonitor;
    private PackageManager.OnPermissionsChangedListener mPermissionChangeListener;
    private LinkedHashMap<String, GroupState> mRequestGrantPermissionGroups = new LinkedHashMap<>();
    private String[] mRequestedPermissions;
    boolean mResultSet;
    private GrantPermissionsViewHandler mViewHandler;

    private int getPermissionPolicy() {
        return ((DevicePolicyManager) getSystemService(DevicePolicyManager.class)).getPermissionPolicy(null);
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mCallingPackage = getCallingPackage();
        this.mPackageMonitor = new PackageMonitor() {
            public void onPackageRemoved(String str, int i) {
                if (GrantPermissionsActivity.this.mCallingPackage.equals(str)) {
                    Log.w("GrantPermissionsActivity", GrantPermissionsActivity.this.mCallingPackage + " was uninstalled");
                    GrantPermissionsActivity.this.finish();
                }
            }
        };
        setFinishOnTouchOutside(false);
        getWindow().addFlags(524288);
        getWindow().addFlags(2097152);
        setTitle(R.string.permission_request_title);
        if (DeviceUtils.isTelevision(this)) {
            this.mViewHandler = new GrantPermissionsViewHandlerImpl(this, this.mCallingPackage).setResultListener(this);
        } else if (DeviceUtils.isWear(this)) {
            this.mViewHandler = new GrantPermissionsWatchViewHandler(this).setResultListener(this);
        } else if (DeviceUtils.isAuto(this)) {
            this.mViewHandler = new GrantPermissionsAutoViewHandler(this, this.mCallingPackage).setResultListener(this);
        } else {
            this.mViewHandler = new com.android.packageinstaller.permission.ui.handheld.GrantPermissionsViewHandlerImpl(this, this.mCallingPackage).setResultListener(this);
        }
        this.mRequestedPermissions = getIntent().getStringArrayExtra("android.content.pm.extra.REQUEST_PERMISSIONS_NAMES");
        if (this.mRequestedPermissions == null) {
            this.mRequestedPermissions = new String[0];
        }
        int length = this.mRequestedPermissions.length;
        this.mGrantResults = new int[length];
        Arrays.fill(this.mGrantResults, -1);
        if (length == 0) {
            setResultAndFinish();
            return;
        }
        try {
            this.mPermissionChangeListener = new PermissionChangeListener();
            PackageInfo callingPackageInfo = getCallingPackageInfo();
            if (callingPackageInfo == null || callingPackageInfo.requestedPermissions == null || callingPackageInfo.requestedPermissions.length <= 0) {
                setResultAndFinish();
                return;
            }
            if (callingPackageInfo.applicationInfo.targetSdkVersion < 23) {
                this.mRequestedPermissions = new String[0];
                this.mGrantResults = new int[0];
                setResultAndFinish();
                return;
            }
            updateAlreadyGrantedPermissions(callingPackageInfo, getPermissionPolicy());
            this.mAppPermissions = new AppPermissions(this, callingPackageInfo, null, false, new Runnable() {
                @Override
                public void run() {
                    GrantPermissionsActivity.this.setResultAndFinish();
                }
            });
            for (String str : this.mRequestedPermissions) {
                AppPermissionGroup appPermissionGroup = null;
                Iterator<AppPermissionGroup> it = this.mAppPermissions.getPermissionGroups().iterator();
                while (true) {
                    if (it.hasNext()) {
                        AppPermissionGroup next = it.next();
                        if (next.hasPermission(str)) {
                            appPermissionGroup = next;
                        }
                    }
                }
                if (appPermissionGroup != null && appPermissionGroup.isGrantingAllowed()) {
                    if (!appPermissionGroup.isUserFixed() && !appPermissionGroup.isPolicyFixed()) {
                        switch (getPermissionPolicy()) {
                            case DialogFragment.STYLE_NO_TITLE:
                                if (!appPermissionGroup.areRuntimePermissionsGranted()) {
                                    appPermissionGroup.grantRuntimePermissions(false, computeAffectedPermissions(callingPackageInfo, str));
                                }
                                appPermissionGroup.setPolicyFixed();
                                break;
                            case DialogFragment.STYLE_NO_FRAME:
                                if (appPermissionGroup.areRuntimePermissionsGranted()) {
                                    appPermissionGroup.revokeRuntimePermissions(false, computeAffectedPermissions(callingPackageInfo, str));
                                }
                                appPermissionGroup.setPolicyFixed();
                                break;
                            default:
                                if (appPermissionGroup.areRuntimePermissionsGranted()) {
                                    appPermissionGroup.grantRuntimePermissions(false, computeAffectedPermissions(callingPackageInfo, str));
                                    updateGrantResults(appPermissionGroup);
                                } else {
                                    GroupState groupState = this.mRequestGrantPermissionGroups.get(appPermissionGroup.getName());
                                    if (groupState == null) {
                                        groupState = new GroupState(appPermissionGroup);
                                        this.mRequestGrantPermissionGroups.put(appPermissionGroup.getName(), groupState);
                                    }
                                    String[] strArrComputeAffectedPermissions = computeAffectedPermissions(callingPackageInfo, str);
                                    if (strArrComputeAffectedPermissions != null) {
                                        for (String str2 : strArrComputeAffectedPermissions) {
                                            groupState.affectedPermissions = ArrayUtils.appendString(groupState.affectedPermissions, str2);
                                        }
                                    }
                                }
                                break;
                        }
                    } else {
                        updateGrantResults(appPermissionGroup);
                    }
                }
            }
            setContentView(this.mViewHandler.createView());
            Window window = getWindow();
            WindowManager.LayoutParams attributes = window.getAttributes();
            this.mViewHandler.updateWindowAttributes(attributes);
            window.setAttributes(attributes);
            if (!showNextPermissionGroupGrantRequest()) {
                setResultAndFinish();
            } else if (bundle == null) {
                int length2 = this.mRequestedPermissions.length;
                for (int i = 0; i < length2; i++) {
                    EventLogger.logPermission(1242, this.mRequestedPermissions[i], this.mAppPermissions.getPackageInfo().packageName);
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            setResultAndFinish();
        }
    }

    private void updateIfPermissionsWereGranted() {
        boolean z;
        updateAlreadyGrantedPermissions(getCallingPackageInfo(), getPermissionPolicy());
        ArraySet arraySet = new ArraySet(this.mRequestedPermissions.length);
        for (int i = 0; i < this.mRequestedPermissions.length; i++) {
            if (this.mGrantResults[i] == 0) {
                arraySet.add(this.mRequestedPermissions[i]);
            }
        }
        int size = this.mAppPermissions.getPermissionGroups().size();
        boolean z2 = true;
        for (int i2 = 0; i2 < size; i2++) {
            GroupState groupState = this.mRequestGrantPermissionGroups.get(this.mAppPermissions.getPermissionGroups().get(i2).getName());
            if (groupState != null && groupState.mState == 0) {
                if (groupState.affectedPermissions != null) {
                    for (int i3 = 0; i3 < groupState.affectedPermissions.length; i3++) {
                        if (!arraySet.contains(groupState.affectedPermissions[i3])) {
                            z = false;
                            break;
                        }
                    }
                    z = true;
                    if (z) {
                    }
                } else {
                    z = false;
                    if (z) {
                        z2 = false;
                    } else {
                        groupState.mState = 1;
                        if (z2 && !showNextPermissionGroupGrantRequest()) {
                            setResultAndFinish();
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        PackageManager packageManager = getPackageManager();
        packageManager.addOnPermissionsChangeListener(this.mPermissionChangeListener);
        this.mPackageMonitor.register(this, getMainLooper(), false);
        try {
            packageManager.getPackageInfo(this.mCallingPackage, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w("GrantPermissionsActivity", this.mCallingPackage + " was uninstalled while this activity was stopped", e);
            finish();
        }
        updateIfPermissionsWereGranted();
    }

    @Override
    protected void onStop() {
        super.onStop();
        this.mPackageMonitor.unregister();
        getPackageManager().removeOnPermissionsChangeListener(this.mPermissionChangeListener);
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        View decorView = getWindow().getDecorView();
        if (decorView.getParent() != null) {
            getWindowManager().removeViewImmediate(decorView);
            getWindowManager().addView(decorView, decorView.getLayoutParams());
            if (this.mViewHandler instanceof com.android.packageinstaller.permission.ui.handheld.GrantPermissionsViewHandlerImpl) {
                ((com.android.packageinstaller.permission.ui.handheld.GrantPermissionsViewHandlerImpl) this.mViewHandler).onConfigurationChanged();
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        if (getWindow().getDecorView().getTop() != 0) {
            motionEvent.setLocation(motionEvent.getX(), motionEvent.getY() - r0.getTop());
        }
        return super.dispatchTouchEvent(motionEvent);
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        this.mViewHandler.saveInstanceState(bundle);
    }

    @Override
    protected void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);
        this.mViewHandler.loadInstanceState(bundle);
    }

    private boolean showNextPermissionGroupGrantRequest() {
        Spanned spannedFromHtml;
        Spanned spannedFromHtml2;
        Resources system;
        Icon iconCreateWithResource;
        int size = this.mRequestGrantPermissionGroups.size();
        int i = 0;
        for (GroupState groupState : this.mRequestGrantPermissionGroups.values()) {
            if (groupState.mState == 0) {
                CharSequence appLabel = this.mAppPermissions.getAppLabel();
                int request = groupState.mGroup.getRequest();
                if (request != 0) {
                    try {
                        spannedFromHtml = Html.fromHtml(getPackageManager().getResourcesForApplication(groupState.mGroup.getDeclaringPackage()).getString(request, appLabel), 0);
                    } catch (PackageManager.NameNotFoundException e) {
                        spannedFromHtml = null;
                    }
                } else {
                    spannedFromHtml = null;
                }
                if (spannedFromHtml == null) {
                    spannedFromHtml2 = Html.fromHtml(getString(R.string.permission_warning_template, new Object[]{appLabel, groupState.mGroup.getDescription()}), 0);
                } else {
                    spannedFromHtml2 = spannedFromHtml;
                }
                setTitle(spannedFromHtml2);
                try {
                    system = getPackageManager().getResourcesForApplication(groupState.mGroup.getIconPkg());
                } catch (PackageManager.NameNotFoundException e2) {
                    system = Resources.getSystem();
                }
                try {
                    iconCreateWithResource = Icon.createWithResource(system, groupState.mGroup.getIconResId());
                } catch (Resources.NotFoundException e3) {
                    Log.e("GrantPermissionsActivity", "Cannot load icon for group" + groupState.mGroup.getName(), e3);
                    iconCreateWithResource = null;
                }
                this.mViewHandler.updateUi(groupState.mGroup.getName(), size, i, iconCreateWithResource, spannedFromHtml2, groupState.mGroup.isUserSet());
                return true;
            }
            i++;
        }
        return false;
    }

    @Override
    public void onPermissionGrantResult(final String str, final boolean z, final boolean z2) {
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KeyguardManager.class);
        if (keyguardManager.isDeviceLocked()) {
            keyguardManager.requestDismissKeyguard(this, new KeyguardManager.KeyguardDismissCallback() {
                @Override
                public void onDismissError() {
                    Log.e("GrantPermissionsActivity", "Cannot dismiss keyguard perm=" + str + " granted=" + z + " doNotAskAgain=" + z2);
                }

                @Override
                public void onDismissCancelled() {
                }

                @Override
                public void onDismissSucceeded() {
                    GrantPermissionsActivity.this.onPermissionGrantResult(str, z, z2);
                }
            });
            return;
        }
        GroupState groupState = this.mRequestGrantPermissionGroups.get(str);
        if (groupState != null && groupState.mGroup != null) {
            if (z) {
                groupState.mGroup.grantRuntimePermissions(z2, groupState.affectedPermissions);
                groupState.mState = 1;
            } else {
                groupState.mGroup.revokeRuntimePermissions(z2, groupState.affectedPermissions);
                groupState.mState = 2;
                int length = this.mRequestedPermissions.length;
                for (int i = 0; i < length; i++) {
                    String str2 = this.mRequestedPermissions[i];
                    if (groupState.mGroup.hasPermission(str2)) {
                        EventLogger.logPermission(1244, str2, this.mAppPermissions.getPackageInfo().packageName);
                    }
                }
            }
            updateGrantResults(groupState.mGroup);
        }
        if (!showNextPermissionGroupGrantRequest()) {
            setResultAndFinish();
        }
    }

    private void updateGrantResults(AppPermissionGroup appPermissionGroup) {
        for (Permission permission : appPermissionGroup.getPermissions()) {
            int iIndexOf = ArrayUtils.indexOf(this.mRequestedPermissions, permission.getName());
            if (iIndexOf >= 0) {
                this.mGrantResults[iIndexOf] = permission.isGranted() ? 0 : -1;
            }
        }
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        return i == 4;
    }

    @Override
    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        return i == 4;
    }

    @Override
    public void finish() {
        setResultIfNeeded(0);
        super.finish();
    }

    private int computePermissionGrantState(PackageInfo packageInfo, String str, int i) {
        boolean z;
        int i2 = 0;
        while (true) {
            if (i2 < packageInfo.requestedPermissions.length) {
                if (!str.equals(packageInfo.requestedPermissions[i2])) {
                    i2++;
                } else {
                    if ((packageInfo.requestedPermissionsFlags[i2] & 2) != 0) {
                        return 0;
                    }
                    z = true;
                }
            } else {
                z = false;
                break;
            }
        }
        if (!z) {
            return -1;
        }
        try {
            PermissionInfo permissionInfo = getPackageManager().getPermissionInfo(str, 0);
            if ((permissionInfo.protectionLevel & 15) != 1) {
                return -1;
            }
            if ((permissionInfo.protectionLevel & 4096) == 0 && packageInfo.applicationInfo.isInstantApp()) {
                return -1;
            }
            if ((permissionInfo.protectionLevel & 8192) != 0) {
                if (packageInfo.applicationInfo.targetSdkVersion < 23) {
                    return -1;
                }
            }
            return i != 1 ? -1 : 0;
        } catch (PackageManager.NameNotFoundException e) {
            return -1;
        }
    }

    private PackageInfo getCallingPackageInfo() {
        try {
            return getPackageManager().getPackageInfo(this.mCallingPackage, 4096);
        } catch (PackageManager.NameNotFoundException e) {
            Log.i("GrantPermissionsActivity", "No package: " + this.mCallingPackage, e);
            return null;
        }
    }

    private void updateAlreadyGrantedPermissions(PackageInfo packageInfo, int i) {
        int length = this.mRequestedPermissions.length;
        for (int i2 = 0; i2 < length; i2++) {
            String str = this.mRequestedPermissions[i2];
            if (str != null && computePermissionGrantState(packageInfo, str, i) == 0) {
                this.mGrantResults[i2] = 0;
            }
        }
    }

    private void setResultIfNeeded(int i) {
        if (!this.mResultSet) {
            this.mResultSet = true;
            logRequestedPermissionGroups();
            Intent intent = new Intent("android.content.pm.action.REQUEST_PERMISSIONS");
            intent.putExtra("android.content.pm.extra.REQUEST_PERMISSIONS_NAMES", this.mRequestedPermissions);
            intent.putExtra("android.content.pm.extra.REQUEST_PERMISSIONS_RESULTS", this.mGrantResults);
            setResult(i, intent);
        }
    }

    private void setResultAndFinish() {
        setResultIfNeeded(-1);
        finish();
    }

    private void logRequestedPermissionGroups() {
        if (this.mRequestGrantPermissionGroups.isEmpty()) {
            return;
        }
        ArrayList arrayList = new ArrayList(this.mRequestGrantPermissionGroups.size());
        Iterator<GroupState> it = this.mRequestGrantPermissionGroups.values().iterator();
        while (it.hasNext()) {
            arrayList.add(it.next().mGroup);
        }
        SafetyNetLogger.logPermissionsRequested(this.mAppPermissions.getPackageInfo(), arrayList);
    }

    private static String[] computeAffectedPermissions(PackageInfo packageInfo, String str) {
        if (packageInfo.applicationInfo.targetSdkVersion <= 25) {
            return null;
        }
        String[] strArr = {str};
        for (PackageParser.SplitPermissionInfo splitPermissionInfo : PackageParser.SPLIT_PERMISSIONS) {
            if (splitPermissionInfo.targetSdk > 25 && packageInfo.applicationInfo.targetSdkVersion < splitPermissionInfo.targetSdk && str.equals(splitPermissionInfo.rootPerm)) {
                String[] strArrAppendString = strArr;
                for (int i = 0; i < splitPermissionInfo.newPerms.length; i++) {
                    strArrAppendString = ArrayUtils.appendString(strArrAppendString, splitPermissionInfo.newPerms[i]);
                }
                strArr = strArrAppendString;
            }
        }
        return strArr;
    }

    private static final class GroupState {
        String[] affectedPermissions;
        final AppPermissionGroup mGroup;
        int mState = 0;

        GroupState(AppPermissionGroup appPermissionGroup) {
            this.mGroup = appPermissionGroup;
        }
    }

    private class PermissionChangeListener implements PackageManager.OnPermissionsChangedListener {
        final int mCallingPackageUid;

        PermissionChangeListener() throws PackageManager.NameNotFoundException {
            this.mCallingPackageUid = GrantPermissionsActivity.this.getPackageManager().getPackageUid(GrantPermissionsActivity.this.mCallingPackage, 0);
        }

        public void onPermissionsChanged(int i) {
            if (i == this.mCallingPackageUid) {
                GrantPermissionsActivity.this.updateIfPermissionsWereGranted();
            }
        }
    }
}
