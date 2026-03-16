package com.android.packageinstaller;

import android.app.Activity;
import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.ManifestDigest;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.content.pm.PackageUserState;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v4.view.ViewPager;
import android.util.ArraySet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AppSecurityPermissions;
import android.widget.Button;
import android.widget.TabHost;
import android.widget.TextView;
import com.android.packageinstaller.PackageUtil;
import java.io.File;
import java.util.List;

public class PackageInstallerActivity extends Activity implements DialogInterface.OnCancelListener, View.OnClickListener {
    private Button mCancel;
    View mInstallConfirm;
    private InstallFlowAnalytics mInstallFlowAnalytics;
    PackageInstaller mInstaller;
    private Button mOk;
    private Uri mOriginatingURI;
    private Uri mPackageURI;
    private ManifestDigest mPkgDigest;
    PackageInfo mPkgInfo;
    PackageManager mPm;
    private Uri mReferrerURI;
    ApplicationInfo mSourceInfo;
    UserManager mUserManager;
    private int mSessionId = -1;
    private int mOriginatingUid = -1;
    private boolean localLOGV = false;
    private ApplicationInfo mAppInfo = null;
    CaffeinatedScrollView mScrollView = null;
    private boolean mOkCanInstall = false;

    private void startInstallConfirm() {
        TabHost tabHost = (TabHost) findViewById(android.R.id.tabhost);
        tabHost.setup();
        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        TabsAdapter adapter = new TabsAdapter(this, tabHost, viewPager);
        adapter.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                if ("all".equals(tabId)) {
                    PackageInstallerActivity.this.mInstallFlowAnalytics.setAllPermissionsDisplayed(true);
                } else if ("new".equals(tabId)) {
                    PackageInstallerActivity.this.mInstallFlowAnalytics.setNewPermissionsDisplayed(true);
                }
            }
        });
        boolean permVisible = false;
        this.mScrollView = null;
        this.mOkCanInstall = false;
        int msg = 0;
        if (this.mPkgInfo != null) {
            AppSecurityPermissions perms = new AppSecurityPermissions(this, this.mPkgInfo);
            int NP = perms.getPermissionCount(1);
            int ND = perms.getPermissionCount(2);
            if (this.mAppInfo != null) {
                msg = (this.mAppInfo.flags & 1) != 0 ? R.string.install_confirm_question_update_system : R.string.install_confirm_question_update;
                this.mScrollView = new CaffeinatedScrollView(this);
                this.mScrollView.setFillViewport(true);
                boolean newPermissionsFound = perms.getPermissionCount(4) > 0;
                this.mInstallFlowAnalytics.setNewPermissionsFound(newPermissionsFound);
                if (newPermissionsFound) {
                    permVisible = true;
                    this.mScrollView.addView(perms.getPermissionsView(4));
                } else {
                    LayoutInflater inflater = (LayoutInflater) getSystemService("layout_inflater");
                    TextView label = (TextView) inflater.inflate(R.layout.label, (ViewGroup) null);
                    label.setText(R.string.no_new_perms);
                    this.mScrollView.addView(label);
                }
                adapter.addTab(tabHost.newTabSpec("new").setIndicator(getText(R.string.newPerms)), this.mScrollView);
            } else {
                findViewById(R.id.tabscontainer).setVisibility(8);
                findViewById(R.id.divider).setVisibility(0);
            }
            if (NP > 0 || ND > 0) {
                permVisible = true;
                LayoutInflater inflater2 = (LayoutInflater) getSystemService("layout_inflater");
                View root = inflater2.inflate(R.layout.permissions_list, (ViewGroup) null);
                if (this.mScrollView == null) {
                    this.mScrollView = (CaffeinatedScrollView) root.findViewById(R.id.scrollview);
                }
                if (NP > 0) {
                    ((ViewGroup) root.findViewById(R.id.privacylist)).addView(perms.getPermissionsView(1));
                } else {
                    root.findViewById(R.id.privacylist).setVisibility(8);
                }
                if (ND > 0) {
                    ((ViewGroup) root.findViewById(R.id.devicelist)).addView(perms.getPermissionsView(2));
                } else {
                    root.findViewById(R.id.devicelist).setVisibility(8);
                }
                adapter.addTab(tabHost.newTabSpec("all").setIndicator(getText(R.string.allPerms)), root);
            }
        }
        this.mInstallFlowAnalytics.setPermissionsDisplayed(permVisible);
        if (!permVisible) {
            if (this.mAppInfo != null) {
                msg = (this.mAppInfo.flags & 1) != 0 ? R.string.install_confirm_question_update_system_no_perms : R.string.install_confirm_question_update_no_perms;
            } else {
                msg = R.string.install_confirm_question_no_perms;
            }
            tabHost.setVisibility(8);
            this.mInstallFlowAnalytics.setAllPermissionsDisplayed(false);
            this.mInstallFlowAnalytics.setNewPermissionsDisplayed(false);
            findViewById(R.id.filler).setVisibility(0);
            findViewById(R.id.divider).setVisibility(8);
            this.mScrollView = null;
        }
        if (msg != 0) {
            ((TextView) findViewById(R.id.install_confirm_question)).setText(msg);
        }
        this.mInstallConfirm.setVisibility(0);
        this.mOk = (Button) findViewById(R.id.ok_button);
        this.mCancel = (Button) findViewById(R.id.cancel_button);
        this.mOk.setOnClickListener(this);
        this.mCancel.setOnClickListener(this);
        if (this.mScrollView == null) {
            this.mOk.setText(R.string.install);
            this.mOkCanInstall = true;
        } else {
            this.mScrollView.setFullScrollAction(new Runnable() {
                @Override
                public void run() {
                    PackageInstallerActivity.this.mOk.setText(R.string.install);
                    PackageInstallerActivity.this.mOkCanInstall = true;
                }
            });
        }
    }

    private void showDialogInner(int id) {
        removeDialog(id);
        showDialog(id);
    }

    @Override
    public Dialog onCreateDialog(int id, Bundle bundle) {
        switch (id) {
            case 1:
                return new AlertDialog.Builder(this).setTitle(R.string.unknown_apps_dlg_title).setMessage(R.string.unknown_apps_dlg_text).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.i("PackageInstaller", "Finishing off activity so that user can navigate to settings manually");
                        PackageInstallerActivity.this.finish();
                    }
                }).setPositiveButton(R.string.settings, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.i("PackageInstaller", "Launching settings");
                        PackageInstallerActivity.this.launchSettingsAppAndFinish();
                    }
                }).setOnCancelListener(this).create();
            case 2:
                return new AlertDialog.Builder(this).setTitle(R.string.Parse_error_dlg_title).setMessage(R.string.Parse_error_dlg_text).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PackageInstallerActivity.this.finish();
                    }
                }).setOnCancelListener(this).create();
            case 3:
                CharSequence appTitle = this.mPm.getApplicationLabel(this.mPkgInfo.applicationInfo);
                String dlgText = getString(R.string.out_of_space_dlg_text, new Object[]{appTitle.toString()});
                return new AlertDialog.Builder(this).setTitle(R.string.out_of_space_dlg_title).setMessage(dlgText).setPositiveButton(R.string.manage_applications, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent("android.intent.action.MANAGE_PACKAGE_STORAGE");
                        intent.setFlags(268435456);
                        PackageInstallerActivity.this.startActivity(intent);
                        PackageInstallerActivity.this.finish();
                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.i("PackageInstaller", "Canceling installation");
                        PackageInstallerActivity.this.finish();
                    }
                }).setOnCancelListener(this).create();
            case 4:
                CharSequence appTitle1 = this.mPm.getApplicationLabel(this.mPkgInfo.applicationInfo);
                String dlgText1 = getString(R.string.install_failed_msg, new Object[]{appTitle1.toString()});
                return new AlertDialog.Builder(this).setTitle(R.string.install_failed).setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PackageInstallerActivity.this.finish();
                    }
                }).setMessage(dlgText1).setOnCancelListener(this).create();
            case 5:
                CharSequence appTitle2 = this.mPm.getApplicationLabel(this.mSourceInfo);
                String dlgText2 = getString(R.string.allow_source_dlg_text, new Object[]{appTitle2.toString()});
                return new AlertDialog.Builder(this).setTitle(R.string.allow_source_dlg_title).setMessage(dlgText2).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PackageInstallerActivity.this.setResult(0);
                        PackageInstallerActivity.this.finish();
                    }
                }).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences prefs = PackageInstallerActivity.this.getSharedPreferences("allowed_sources", 0);
                        prefs.edit().putBoolean(PackageInstallerActivity.this.mSourceInfo.packageName, true).apply();
                        PackageInstallerActivity.this.startInstallConfirm();
                    }
                }).setOnCancelListener(this).create();
            case 6:
                return new AlertDialog.Builder(this).setTitle(R.string.unknown_apps_dlg_title).setMessage(R.string.unknown_apps_admin_dlg_text).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PackageInstallerActivity.this.finish();
                    }
                }).setOnCancelListener(this).create();
            default:
                return null;
        }
    }

    private void launchSettingsAppAndFinish() {
        if (BenesseExtension.getDchaState() != 0) {
            finish();
            return;
        }
        Intent launchSettingsIntent = new Intent("android.settings.SECURITY_SETTINGS");
        launchSettingsIntent.setFlags(268435456);
        startActivity(launchSettingsIntent);
        finish();
    }

    private boolean isInstallRequestFromUnknownSource(Intent intent) {
        String callerPackage = getCallingPackage();
        if (callerPackage != null && intent.getBooleanExtra("android.intent.extra.NOT_UNKNOWN_SOURCE", false)) {
            try {
                this.mSourceInfo = this.mPm.getApplicationInfo(callerPackage, 0);
                if (this.mSourceInfo != null) {
                    if ((this.mSourceInfo.flags & 1073741824) != 0) {
                        return false;
                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
        return true;
    }

    private boolean isVerifyAppsEnabled() {
        return this.mUserManager.hasUserRestriction("ensure_verify_apps") || Settings.Global.getInt(getContentResolver(), "package_verifier_enable", 1) > 0;
    }

    private boolean isAppVerifierInstalled() {
        PackageManager pm = getPackageManager();
        Intent verification = new Intent("android.intent.action.PACKAGE_NEEDS_VERIFICATION");
        verification.setType("application/vnd.android.package-archive");
        verification.addFlags(1);
        List<ResolveInfo> receivers = pm.queryBroadcastReceivers(verification, 0);
        return receivers.size() > 0;
    }

    private boolean isUnknownSourcesEnabled() {
        return Settings.Secure.getInt(getContentResolver(), "install_non_market_apps", 0) > 0;
    }

    private boolean isUnknownSourcesAllowedByAdmin() {
        return !this.mUserManager.hasUserRestriction("no_install_unknown_sources");
    }

    private void initiateInstall() {
        String pkgName = this.mPkgInfo.packageName;
        String[] oldName = this.mPm.canonicalToCurrentPackageNames(new String[]{pkgName});
        if (oldName != null && oldName.length > 0 && oldName[0] != null) {
            pkgName = oldName[0];
            this.mPkgInfo.packageName = pkgName;
            this.mPkgInfo.applicationInfo.packageName = pkgName;
        }
        try {
            this.mAppInfo = this.mPm.getApplicationInfo(pkgName, 8192);
            if ((this.mAppInfo.flags & 8388608) == 0) {
                this.mAppInfo = null;
            }
        } catch (PackageManager.NameNotFoundException e) {
            this.mAppInfo = null;
        }
        this.mInstallFlowAnalytics.setReplace(this.mAppInfo != null);
        this.mInstallFlowAnalytics.setSystemApp((this.mAppInfo == null || (this.mAppInfo.flags & 1) == 0) ? false : true);
        startInstallConfirm();
    }

    void setPmResult(int pmResult) {
        Intent result = new Intent();
        result.putExtra("android.intent.extra.INSTALL_RESULT", pmResult);
        setResult(pmResult == 1 ? -1 : 1, result);
    }

    @Override
    protected void onCreate(Bundle icicle) {
        PackageUtil.AppSnippet as;
        super.onCreate(icicle);
        getWindow().addPrivateFlags(524288);
        this.mPm = getPackageManager();
        this.mInstaller = this.mPm.getPackageInstaller();
        this.mUserManager = (UserManager) getSystemService("user");
        Intent intent = getIntent();
        if ("android.content.pm.action.CONFIRM_PERMISSIONS".equals(intent.getAction())) {
            int sessionId = intent.getIntExtra("android.content.pm.extra.SESSION_ID", -1);
            PackageInstaller.SessionInfo info = this.mInstaller.getSessionInfo(sessionId);
            if (info == null || !info.sealed || info.resolvedBaseCodePath == null) {
                Log.w("PackageInstaller", "Session " + this.mSessionId + " in funky state; ignoring");
                finish();
                return;
            } else {
                this.mSessionId = sessionId;
                this.mPackageURI = Uri.fromFile(new File(info.resolvedBaseCodePath));
                this.mOriginatingURI = null;
                this.mReferrerURI = null;
            }
        } else {
            this.mSessionId = -1;
            this.mPackageURI = intent.getData();
            this.mOriginatingURI = (Uri) intent.getParcelableExtra("android.intent.extra.ORIGINATING_URI");
            this.mReferrerURI = (Uri) intent.getParcelableExtra("android.intent.extra.REFERRER");
        }
        boolean unknownSourcesAllowedByAdmin = isUnknownSourcesAllowedByAdmin();
        boolean unknownSourcesAllowedByUser = isUnknownSourcesEnabled();
        boolean requestFromUnknownSource = isInstallRequestFromUnknownSource(intent);
        this.mInstallFlowAnalytics = new InstallFlowAnalytics();
        this.mInstallFlowAnalytics.setContext(this);
        this.mInstallFlowAnalytics.setStartTimestampMillis(SystemClock.elapsedRealtime());
        this.mInstallFlowAnalytics.setInstallsFromUnknownSourcesPermitted(unknownSourcesAllowedByAdmin && unknownSourcesAllowedByUser);
        this.mInstallFlowAnalytics.setInstallRequestFromUnknownSource(requestFromUnknownSource);
        this.mInstallFlowAnalytics.setVerifyAppsEnabled(isVerifyAppsEnabled());
        this.mInstallFlowAnalytics.setAppVerifierInstalled(isAppVerifierInstalled());
        this.mInstallFlowAnalytics.setPackageUri(this.mPackageURI.toString());
        String scheme = this.mPackageURI.getScheme();
        if (scheme != null && !"file".equals(scheme) && !"package".equals(scheme)) {
            Log.w("PackageInstaller", "Unsupported scheme " + scheme);
            setPmResult(-3);
            this.mInstallFlowAnalytics.setFlowFinished((byte) 1);
            finish();
            return;
        }
        if ("package".equals(this.mPackageURI.getScheme())) {
            this.mInstallFlowAnalytics.setFileUri(false);
            try {
                this.mPkgInfo = this.mPm.getPackageInfo(this.mPackageURI.getSchemeSpecificPart(), 12288);
            } catch (PackageManager.NameNotFoundException e) {
            }
            if (this.mPkgInfo == null) {
                Log.w("PackageInstaller", "Requested package " + this.mPackageURI.getScheme() + " not available. Discontinuing installation");
                showDialogInner(2);
                setPmResult(-2);
                this.mInstallFlowAnalytics.setPackageInfoObtained();
                this.mInstallFlowAnalytics.setFlowFinished((byte) 3);
                return;
            }
            as = new PackageUtil.AppSnippet(this.mPm.getApplicationLabel(this.mPkgInfo.applicationInfo), this.mPm.getApplicationIcon(this.mPkgInfo.applicationInfo));
        } else {
            this.mInstallFlowAnalytics.setFileUri(true);
            File sourceFile = new File(this.mPackageURI.getPath());
            PackageParser.Package parsed = PackageUtil.getPackageInfo(sourceFile);
            if (parsed == null) {
                Log.w("PackageInstaller", "Parse error when parsing manifest. Discontinuing installation");
                showDialogInner(2);
                setPmResult(-2);
                this.mInstallFlowAnalytics.setPackageInfoObtained();
                this.mInstallFlowAnalytics.setFlowFinished((byte) 2);
                return;
            }
            this.mPkgInfo = PackageParser.generatePackageInfo(parsed, (int[]) null, 4096, 0L, 0L, (ArraySet) null, new PackageUserState());
            this.mPkgDigest = parsed.manifestDigest;
            as = PackageUtil.getAppSnippet(this, this.mPkgInfo.applicationInfo, sourceFile);
        }
        this.mInstallFlowAnalytics.setPackageInfoObtained();
        setContentView(R.layout.install_start);
        this.mInstallConfirm = findViewById(R.id.install_confirm_panel);
        this.mInstallConfirm.setVisibility(4);
        PackageUtil.initSnippetForNewApp(this, as, R.id.app_snippet);
        this.mOriginatingUid = getOriginatingUid(intent);
        if (!requestFromUnknownSource) {
            initiateInstall();
            return;
        }
        boolean isManagedProfile = this.mUserManager.isManagedProfile();
        if (!unknownSourcesAllowedByAdmin || (!unknownSourcesAllowedByUser && isManagedProfile)) {
            showDialogInner(6);
            this.mInstallFlowAnalytics.setFlowFinished((byte) 4);
        } else if (!unknownSourcesAllowedByUser) {
            showDialogInner(1);
            this.mInstallFlowAnalytics.setFlowFinished((byte) 4);
        } else {
            initiateInstall();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.mOk != null) {
            this.mOk.setEnabled(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (this.mOk != null) {
            this.mOk.setEnabled(false);
        }
    }

    private ApplicationInfo getSourceInfo() {
        String callingPackage = getCallingPackage();
        if (callingPackage != null) {
            try {
                return this.mPm.getApplicationInfo(callingPackage, 0);
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
        return null;
    }

    private int getOriginatingUid(Intent intent) {
        String[] callingPackages;
        ApplicationInfo applicationInfo;
        int uidFromIntent = intent.getIntExtra("android.intent.extra.ORIGINATING_UID", -1);
        ApplicationInfo sourceInfo = getSourceInfo();
        if (sourceInfo != null) {
            if (uidFromIntent == -1 || (this.mSourceInfo.flags & 1073741824) == 0) {
                return sourceInfo.uid;
            }
            return uidFromIntent;
        }
        try {
            int callingUid = ActivityManagerNative.getDefault().getLaunchedFromUid(getActivityToken());
            if (uidFromIntent != -1 && (callingPackages = this.mPm.getPackagesForUid(callingUid)) != null) {
                for (String packageName : callingPackages) {
                    try {
                        applicationInfo = this.mPm.getApplicationInfo(packageName, 0);
                    } catch (PackageManager.NameNotFoundException e) {
                    }
                    if ((applicationInfo.flags & 1073741824) != 0) {
                        return uidFromIntent;
                    }
                }
            }
            return callingUid;
        } catch (RemoteException e2) {
            Log.w("PackageInstaller", "Could not determine the launching uid.");
            return -1;
        }
    }

    @Override
    public void onBackPressed() {
        if (this.mSessionId != -1) {
            this.mInstaller.setPermissionsResult(this.mSessionId, false);
        }
        this.mInstallFlowAnalytics.setFlowFinished((byte) 5);
        super.onBackPressed();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        finish();
    }

    @Override
    public void onClick(View v) {
        if (v == this.mOk) {
            if (this.mOkCanInstall || this.mScrollView == null) {
                this.mInstallFlowAnalytics.setInstallButtonClicked();
                if (this.mSessionId != -1) {
                    this.mInstaller.setPermissionsResult(this.mSessionId, true);
                    this.mInstallFlowAnalytics.setFlowFinishedWithPackageManagerResult(1);
                } else {
                    Intent newIntent = new Intent();
                    newIntent.putExtra("com.android.packageinstaller.applicationInfo", this.mPkgInfo.applicationInfo);
                    newIntent.setData(this.mPackageURI);
                    newIntent.setClass(this, InstallAppProgress.class);
                    newIntent.putExtra("com.android.packageinstaller.extras.manifest_digest", (Parcelable) this.mPkgDigest);
                    newIntent.putExtra("com.android.packageinstaller.extras.install_flow_analytics", this.mInstallFlowAnalytics);
                    String installerPackageName = getIntent().getStringExtra("android.intent.extra.INSTALLER_PACKAGE_NAME");
                    if (this.mOriginatingURI != null) {
                        newIntent.putExtra("android.intent.extra.ORIGINATING_URI", this.mOriginatingURI);
                    }
                    if (this.mReferrerURI != null) {
                        newIntent.putExtra("android.intent.extra.REFERRER", this.mReferrerURI);
                    }
                    if (this.mOriginatingUid != -1) {
                        newIntent.putExtra("android.intent.extra.ORIGINATING_UID", this.mOriginatingUid);
                    }
                    if (installerPackageName != null) {
                        newIntent.putExtra("android.intent.extra.INSTALLER_PACKAGE_NAME", installerPackageName);
                    }
                    if (getIntent().getBooleanExtra("android.intent.extra.RETURN_RESULT", false)) {
                        newIntent.putExtra("android.intent.extra.RETURN_RESULT", true);
                        newIntent.addFlags(33554432);
                    }
                    if (this.localLOGV) {
                        Log.i("PackageInstaller", "downloaded app uri=" + this.mPackageURI);
                    }
                    startActivity(newIntent);
                }
                finish();
                return;
            }
            this.mScrollView.pageScroll(130);
            return;
        }
        if (v == this.mCancel) {
            setResult(0);
            if (this.mSessionId != -1) {
                this.mInstaller.setPermissionsResult(this.mSessionId, false);
            }
            this.mInstallFlowAnalytics.setFlowFinished((byte) 5);
            finish();
        }
    }
}
