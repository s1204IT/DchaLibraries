package com.android.packageinstaller;

import android.app.AlertDialog;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.content.pm.PackageUserState;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.Process;
import android.os.UserManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AppSecurityPermissions;
import android.widget.Button;
import android.widget.TabHost;
import android.widget.TextView;
import com.android.packageinstaller.PackageInstallerActivity;
import com.android.packageinstaller.PackageUtil;
import com.android.packageinstaller.permission.ui.OverlayTouchActivity;
import java.io.File;
import java.util.Set;

public class PackageInstallerActivity extends OverlayTouchActivity implements View.OnClickListener {
    private static final String ALLOW_UNKNOWN_SOURCES_KEY = PackageInstallerActivity.class.getName() + "ALLOW_UNKNOWN_SOURCES_KEY";
    private boolean mAllowUnknownSources;
    AppOpsManager mAppOpsManager;
    private PackageUtil.AppSnippet mAppSnippet;
    String mCallingPackage;
    private Button mCancel;
    private boolean mEnableOk;
    PackageInstaller mInstaller;
    IPackageManager mIpm;
    private Button mOk;
    private String mOriginatingPackage;
    private Uri mOriginatingURI;
    private Uri mPackageURI;
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
        int i;
        int i2;
        boolean z;
        if (this.mAppInfo != null) {
            bindUi(R.layout.install_confirm_perm_update, true);
        } else {
            bindUi(R.layout.install_confirm_perm, true);
        }
        ((TextView) findViewById(R.id.install_confirm_question)).setText(R.string.install_confirm_question);
        TabHost tabHost = (TabHost) findViewById(android.R.id.tabhost);
        tabHost.setup();
        TabsAdapter tabsAdapter = new TabsAdapter(this, tabHost, (ViewPager) findViewById(R.id.pager));
        boolean z2 = false;
        boolean z3 = this.mPkgInfo.applicationInfo.targetSdkVersion >= 23;
        this.mScrollView = null;
        this.mOkCanInstall = false;
        AppSecurityPermissions appSecurityPermissions = new AppSecurityPermissions(this, this.mPkgInfo);
        int permissionCount = appSecurityPermissions.getPermissionCount(65535);
        if (this.mAppInfo != null) {
            if ((this.mAppInfo.flags & 1) != 0) {
                i = R.string.install_confirm_question_update_system;
            } else {
                i = R.string.install_confirm_question_update;
            }
            this.mScrollView = new CaffeinatedScrollView(this);
            this.mScrollView.setFillViewport(true);
            if (z3) {
                z = false;
            } else {
                z = appSecurityPermissions.getPermissionCount(4) > 0;
                if (z) {
                    this.mScrollView.addView(appSecurityPermissions.getPermissionsView(4));
                    z2 = true;
                }
            }
            if (!z3 && !z) {
                TextView textView = (TextView) ((LayoutInflater) getSystemService("layout_inflater")).inflate(R.layout.label, (ViewGroup) null);
                textView.setText(R.string.no_new_perms);
                this.mScrollView.addView(textView);
            }
            tabsAdapter.addTab(tabHost.newTabSpec("new").setIndicator(getText(R.string.newPerms)), this.mScrollView);
        } else {
            i = 0;
        }
        if (!z3 && permissionCount > 0) {
            View viewInflate = ((LayoutInflater) getSystemService("layout_inflater")).inflate(R.layout.permissions_list, (ViewGroup) null);
            if (this.mScrollView == null) {
                this.mScrollView = (CaffeinatedScrollView) viewInflate.findViewById(R.id.scrollview);
            }
            ((ViewGroup) viewInflate.findViewById(R.id.permission_list)).addView(appSecurityPermissions.getPermissionsView(65535));
            tabsAdapter.addTab(tabHost.newTabSpec("all").setIndicator(getText(R.string.allPerms)), viewInflate);
            z2 = true;
        }
        if (!z2) {
            if (this.mAppInfo != null) {
                if ((this.mAppInfo.flags & 1) != 0) {
                    i2 = R.string.install_confirm_question_update_system_no_perms;
                } else {
                    i2 = R.string.install_confirm_question_update_no_perms;
                }
            } else {
                i2 = R.string.install_confirm_question_no_perms;
            }
            i = i2;
            bindUi(R.layout.install_confirm, true);
            this.mScrollView = null;
        }
        if (i != 0) {
            ((TextView) findViewById(R.id.install_confirm_question)).setText(i);
        }
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

    private void showDialogInner(int i) {
        DialogFragment dialogFragment = (DialogFragment) getFragmentManager().findFragmentByTag("dialog");
        if (dialogFragment != null) {
            dialogFragment.dismissAllowingStateLoss();
        }
        DialogFragment dialogFragmentCreateDialog = createDialog(i);
        if (dialogFragmentCreateDialog != null) {
            dialogFragmentCreateDialog.showAllowingStateLoss(getFragmentManager(), "dialog");
        }
    }

    private DialogFragment createDialog(int i) {
        switch (i) {
            case android.support.v4.app.DialogFragment.STYLE_NO_FRAME:
                return SimpleErrorDialog.newInstance(R.string.Parse_error_dlg_text);
            case android.support.v4.app.DialogFragment.STYLE_NO_INPUT:
                return OutOfSpaceDialog.newInstance(this.mPm.getApplicationLabel(this.mPkgInfo.applicationInfo));
            case 4:
                return InstallErrorDialog.newInstance(this.mPm.getApplicationLabel(this.mPkgInfo.applicationInfo));
            case 5:
                return SimpleErrorDialog.newInstance(R.string.unknown_apps_user_restriction_dlg_text);
            case 6:
                return AnonymousSourceDialog.newInstance();
            case 7:
                return NotSupportedOnWearDialog.newInstance();
            case 8:
                return ExternalSourcesBlockedDialog.newInstance(this.mOriginatingPackage);
            case 9:
                return SimpleErrorDialog.newInstance(R.string.install_apps_user_restriction_dlg_text);
            default:
                return null;
        }
    }

    @Override
    public void onActivityResult(int i, int i2, Intent intent) {
        if (i == 1 && i2 == -1) {
            this.mAllowUnknownSources = true;
            this.mAppOpsManager.noteOpNoThrow(AppOpsManager.permissionToOpCode("android.permission.REQUEST_INSTALL_PACKAGES"), this.mOriginatingUid, this.mOriginatingPackage);
            DialogFragment dialogFragment = (DialogFragment) getFragmentManager().findFragmentByTag("dialog");
            if (dialogFragment != null) {
                dialogFragment.dismissAllowingStateLoss();
            }
            initiateInstall();
            return;
        }
        finish();
    }

    private String getPackageNameForUid(int i) {
        String[] packagesForUid = this.mPm.getPackagesForUid(i);
        if (packagesForUid == null) {
            return null;
        }
        if (packagesForUid.length > 1) {
            if (this.mCallingPackage != null) {
                for (String str : packagesForUid) {
                    if (str.equals(this.mCallingPackage)) {
                        return str;
                    }
                }
            }
            Log.i("PackageInstaller", "Multiple packages found for source uid " + i);
        }
        return packagesForUid[0];
    }

    private boolean isInstallRequestFromUnknownSource(Intent intent) {
        return this.mCallingPackage == null || !intent.getBooleanExtra("android.intent.extra.NOT_UNKNOWN_SOURCE", false) || this.mSourceInfo == null || (this.mSourceInfo.privateFlags & 8) == 0;
    }

    private void initiateInstall() {
        String str = this.mPkgInfo.packageName;
        String[] strArrCanonicalToCurrentPackageNames = this.mPm.canonicalToCurrentPackageNames(new String[]{str});
        if (strArrCanonicalToCurrentPackageNames != null && strArrCanonicalToCurrentPackageNames.length > 0 && strArrCanonicalToCurrentPackageNames[0] != null) {
            str = strArrCanonicalToCurrentPackageNames[0];
            this.mPkgInfo.packageName = str;
            this.mPkgInfo.applicationInfo.packageName = str;
        }
        try {
            this.mAppInfo = this.mPm.getApplicationInfo(str, 8192);
            if ((this.mAppInfo.flags & 8388608) == 0) {
                this.mAppInfo = null;
            }
        } catch (PackageManager.NameNotFoundException e) {
            this.mAppInfo = null;
        }
        startInstallConfirm();
    }

    void setPmResult(int i) {
        Intent intent = new Intent();
        intent.putExtra("android.intent.extra.INSTALL_RESULT", i);
        int i2 = 1;
        if (i == 1) {
            i2 = -1;
        }
        setResult(i2, intent);
    }

    @Override
    protected void onCreate(Bundle bundle) {
        String packageNameForUid;
        Uri uriFromFile;
        super.onCreate(null);
        if (bundle != null) {
            this.mAllowUnknownSources = bundle.getBoolean(ALLOW_UNKNOWN_SOURCES_KEY);
        }
        this.mPm = getPackageManager();
        this.mIpm = AppGlobals.getPackageManager();
        this.mAppOpsManager = (AppOpsManager) getSystemService("appops");
        this.mInstaller = this.mPm.getPackageInstaller();
        this.mUserManager = (UserManager) getSystemService("user");
        Intent intent = getIntent();
        this.mCallingPackage = intent.getStringExtra("EXTRA_CALLING_PACKAGE");
        this.mSourceInfo = (ApplicationInfo) intent.getParcelableExtra("EXTRA_ORIGINAL_SOURCE_INFO");
        this.mOriginatingUid = intent.getIntExtra("android.intent.extra.ORIGINATING_UID", -1);
        if (this.mOriginatingUid == -1) {
            packageNameForUid = null;
        } else {
            packageNameForUid = getPackageNameForUid(this.mOriginatingUid);
        }
        this.mOriginatingPackage = packageNameForUid;
        if ("android.content.pm.action.CONFIRM_PERMISSIONS".equals(intent.getAction())) {
            int intExtra = intent.getIntExtra("android.content.pm.extra.SESSION_ID", -1);
            PackageInstaller.SessionInfo sessionInfo = this.mInstaller.getSessionInfo(intExtra);
            if (sessionInfo == null || !sessionInfo.sealed || sessionInfo.resolvedBaseCodePath == null) {
                Log.w("PackageInstaller", "Session " + this.mSessionId + " in funky state; ignoring");
                finish();
                return;
            }
            this.mSessionId = intExtra;
            uriFromFile = Uri.fromFile(new File(sessionInfo.resolvedBaseCodePath));
            this.mOriginatingURI = null;
            this.mReferrerURI = null;
        } else {
            this.mSessionId = -1;
            Uri data = intent.getData();
            this.mOriginatingURI = (Uri) intent.getParcelableExtra("android.intent.extra.ORIGINATING_URI");
            this.mReferrerURI = (Uri) intent.getParcelableExtra("android.intent.extra.REFERRER");
            uriFromFile = data;
        }
        if (uriFromFile == null) {
            Log.w("PackageInstaller", "Unspecified source");
            setPmResult(-3);
            finish();
        } else if (DeviceUtils.isWear(this)) {
            showDialogInner(7);
        } else {
            if (!processPackageUri(uriFromFile)) {
                return;
            }
            bindUi(R.layout.install_confirm, false);
            checkIfAllowedAndInitiateInstall();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.mOk != null) {
            this.mOk.setEnabled(this.mEnableOk);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (this.mOk != null) {
            this.mOk.setEnabled(false);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putBoolean(ALLOW_UNKNOWN_SOURCES_KEY, this.mAllowUnknownSources);
    }

    private void bindUi(int i, boolean z) {
        setContentView(i);
        this.mOk = (Button) findViewById(R.id.ok_button);
        this.mCancel = (Button) findViewById(R.id.cancel_button);
        this.mOk.setOnClickListener(this);
        this.mCancel.setOnClickListener(this);
        this.mEnableOk = z;
        this.mOk.setEnabled(z);
        PackageUtil.initSnippetForNewApp(this, this.mAppSnippet, R.id.app_snippet);
    }

    private void checkIfAllowedAndInitiateInstall() {
        int userRestrictionSource = this.mUserManager.getUserRestrictionSource("no_install_apps", Process.myUserHandle());
        if ((userRestrictionSource & 1) != 0) {
            showDialogInner(9);
            return;
        }
        if (userRestrictionSource != 0) {
            if (BenesseExtension.getDchaState() != 0) {
                return;
            }
            startActivity(new Intent("android.settings.SHOW_ADMIN_SUPPORT_DETAILS"));
            finish();
            return;
        }
        if (this.mAllowUnknownSources || !isInstallRequestFromUnknownSource(getIntent())) {
            initiateInstall();
            return;
        }
        int userRestrictionSource2 = this.mUserManager.getUserRestrictionSource("no_install_unknown_sources", Process.myUserHandle());
        if ((userRestrictionSource2 & 1) != 0) {
            showDialogInner(5);
            return;
        }
        if (userRestrictionSource2 != 0) {
            if (BenesseExtension.getDchaState() != 0) {
                return;
            }
            startActivity(new Intent("android.settings.SHOW_ADMIN_SUPPORT_DETAILS"));
            finish();
            return;
        }
        handleUnknownSources();
    }

    private void handleUnknownSources() {
        if (this.mOriginatingPackage == null) {
            Log.i("PackageInstaller", "No source found for package " + this.mPkgInfo.packageName);
            showDialogInner(6);
            return;
        }
        int iPermissionToOpCode = AppOpsManager.permissionToOpCode("android.permission.REQUEST_INSTALL_PACKAGES");
        int iNoteOpNoThrow = this.mAppOpsManager.noteOpNoThrow(iPermissionToOpCode, this.mOriginatingUid, this.mOriginatingPackage);
        if (iNoteOpNoThrow != 0) {
            switch (iNoteOpNoThrow) {
                case android.support.v4.app.DialogFragment.STYLE_NO_FRAME:
                    break;
                case android.support.v4.app.DialogFragment.STYLE_NO_INPUT:
                    this.mAppOpsManager.setMode(iPermissionToOpCode, this.mOriginatingUid, this.mOriginatingPackage, 2);
                    break;
                default:
                    Log.e("PackageInstaller", "Invalid app op mode " + iNoteOpNoThrow + " for OP_REQUEST_INSTALL_PACKAGES found for uid " + this.mOriginatingUid);
                    finish();
                    return;
            }
            showDialogInner(8);
            return;
        }
        initiateInstall();
    }

    private boolean processPackageUri(Uri uri) {
        byte b;
        this.mPackageURI = uri;
        String scheme = uri.getScheme();
        int iHashCode = scheme.hashCode();
        if (iHashCode != -807062458) {
            b = (iHashCode == 3143036 && scheme.equals("file")) ? (byte) 1 : (byte) -1;
        } else if (scheme.equals("package")) {
            b = 0;
        }
        switch (b) {
            case android.support.v4.app.DialogFragment.STYLE_NORMAL:
                try {
                    this.mPkgInfo = this.mPm.getPackageInfo(uri.getSchemeSpecificPart(), 12288);
                    break;
                } catch (PackageManager.NameNotFoundException e) {
                }
                if (this.mPkgInfo == null) {
                    Log.w("PackageInstaller", "Requested package " + uri.getScheme() + " not available. Discontinuing installation");
                    showDialogInner(2);
                    setPmResult(-2);
                    return false;
                }
                this.mAppSnippet = new PackageUtil.AppSnippet(this.mPm.getApplicationLabel(this.mPkgInfo.applicationInfo), this.mPm.getApplicationIcon(this.mPkgInfo.applicationInfo));
                return true;
            case android.support.v4.app.DialogFragment.STYLE_NO_TITLE:
                File file = new File(uri.getPath());
                PackageParser.Package packageInfo = PackageUtil.getPackageInfo(this, file);
                if (packageInfo == null) {
                    Log.w("PackageInstaller", "Parse error when parsing manifest. Discontinuing installation");
                    showDialogInner(2);
                    setPmResult(-2);
                    return false;
                }
                this.mPkgInfo = PackageParser.generatePackageInfo(packageInfo, (int[]) null, 4096, 0L, 0L, (Set) null, new PackageUserState());
                this.mAppSnippet = PackageUtil.getAppSnippet(this, this.mPkgInfo.applicationInfo, file);
                return true;
            default:
                throw new IllegalArgumentException("Unexpected URI scheme " + uri);
        }
    }

    @Override
    public void onBackPressed() {
        if (this.mSessionId != -1) {
            this.mInstaller.setPermissionsResult(this.mSessionId, false);
        }
        super.onBackPressed();
    }

    @Override
    public void onClick(View view) {
        if (view == this.mOk) {
            if (this.mOk.isEnabled()) {
                if (this.mOkCanInstall || this.mScrollView == null) {
                    if (this.mSessionId != -1) {
                        this.mInstaller.setPermissionsResult(this.mSessionId, true);
                        finish();
                        return;
                    } else {
                        startInstall();
                        return;
                    }
                }
                this.mScrollView.pageScroll(130);
                return;
            }
            return;
        }
        if (view == this.mCancel) {
            setResult(0);
            if (this.mSessionId != -1) {
                this.mInstaller.setPermissionsResult(this.mSessionId, false);
            }
            finish();
        }
    }

    private void startInstall() {
        Intent intent = new Intent();
        intent.putExtra("com.android.packageinstaller.applicationInfo", this.mPkgInfo.applicationInfo);
        intent.setData(this.mPackageURI);
        intent.setClass(this, InstallInstalling.class);
        String stringExtra = getIntent().getStringExtra("android.intent.extra.INSTALLER_PACKAGE_NAME");
        if (this.mOriginatingURI != null) {
            intent.putExtra("android.intent.extra.ORIGINATING_URI", this.mOriginatingURI);
        }
        if (this.mReferrerURI != null) {
            intent.putExtra("android.intent.extra.REFERRER", this.mReferrerURI);
        }
        if (this.mOriginatingUid != -1) {
            intent.putExtra("android.intent.extra.ORIGINATING_UID", this.mOriginatingUid);
        }
        if (stringExtra != null) {
            intent.putExtra("android.intent.extra.INSTALLER_PACKAGE_NAME", stringExtra);
        }
        if (getIntent().getBooleanExtra("android.intent.extra.RETURN_RESULT", false)) {
            intent.putExtra("android.intent.extra.RETURN_RESULT", true);
        }
        intent.addFlags(33554432);
        if (this.localLOGV) {
            Log.i("PackageInstaller", "downloaded app uri=" + this.mPackageURI);
        }
        startActivity(intent);
        finish();
    }

    public static class SimpleErrorDialog extends DialogFragment {
        private static final String MESSAGE_KEY = SimpleErrorDialog.class.getName() + "MESSAGE_KEY";

        static SimpleErrorDialog newInstance(int i) {
            SimpleErrorDialog simpleErrorDialog = new SimpleErrorDialog();
            Bundle bundle = new Bundle();
            bundle.putInt(MESSAGE_KEY, i);
            simpleErrorDialog.setArguments(bundle);
            return simpleErrorDialog;
        }

        @Override
        public Dialog onCreateDialog(Bundle bundle) {
            return new AlertDialog.Builder(getActivity()).setMessage(getArguments().getInt(MESSAGE_KEY)).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public final void onClick(DialogInterface dialogInterface, int i) {
                    this.f$0.getActivity().finish();
                }
            }).create();
        }
    }

    public static class AnonymousSourceDialog extends DialogFragment {
        static AnonymousSourceDialog newInstance() {
            return new AnonymousSourceDialog();
        }

        @Override
        public Dialog onCreateDialog(Bundle bundle) {
            return new AlertDialog.Builder(getActivity()).setMessage(R.string.anonymous_source_warning).setPositiveButton(R.string.anonymous_source_continue, new DialogInterface.OnClickListener() {
                @Override
                public final void onClick(DialogInterface dialogInterface, int i) {
                    PackageInstallerActivity.AnonymousSourceDialog.lambda$onCreateDialog$0(this.f$0, dialogInterface, i);
                }
            }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public final void onClick(DialogInterface dialogInterface, int i) {
                    this.f$0.getActivity().finish();
                }
            }).create();
        }

        public static void lambda$onCreateDialog$0(AnonymousSourceDialog anonymousSourceDialog, DialogInterface dialogInterface, int i) {
            PackageInstallerActivity packageInstallerActivity = (PackageInstallerActivity) anonymousSourceDialog.getActivity();
            packageInstallerActivity.mAllowUnknownSources = true;
            packageInstallerActivity.initiateInstall();
        }

        @Override
        public void onCancel(DialogInterface dialogInterface) {
            getActivity().finish();
        }
    }

    public static class NotSupportedOnWearDialog extends SimpleErrorDialog {
        static SimpleErrorDialog newInstance() {
            return SimpleErrorDialog.newInstance(R.string.wear_not_allowed_dlg_text);
        }

        @Override
        public void onCancel(DialogInterface dialogInterface) {
            getActivity().setResult(-1);
            getActivity().finish();
        }
    }

    public static class OutOfSpaceDialog extends AppErrorDialog {
        static AppErrorDialog newInstance(CharSequence charSequence) {
            OutOfSpaceDialog outOfSpaceDialog = new OutOfSpaceDialog();
            outOfSpaceDialog.setArgument(charSequence);
            return outOfSpaceDialog;
        }

        @Override
        protected Dialog createDialog(CharSequence charSequence) {
            return new AlertDialog.Builder(getActivity()).setMessage(getString(R.string.out_of_space_dlg_text, new Object[]{charSequence})).setPositiveButton(R.string.manage_applications, new DialogInterface.OnClickListener() {
                @Override
                public final void onClick(DialogInterface dialogInterface, int i) {
                    PackageInstallerActivity.OutOfSpaceDialog.lambda$createDialog$0(this.f$0, dialogInterface, i);
                }
            }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public final void onClick(DialogInterface dialogInterface, int i) {
                    this.f$0.getActivity().finish();
                }
            }).create();
        }

        public static void lambda$createDialog$0(OutOfSpaceDialog outOfSpaceDialog, DialogInterface dialogInterface, int i) {
            if (BenesseExtension.getDchaState() == 0) {
                Intent intent = new Intent("android.intent.action.MANAGE_PACKAGE_STORAGE");
                intent.setFlags(268435456);
                outOfSpaceDialog.startActivity(intent);
            }
            outOfSpaceDialog.getActivity().finish();
        }
    }

    public static class InstallErrorDialog extends AppErrorDialog {
        static AppErrorDialog newInstance(CharSequence charSequence) {
            InstallErrorDialog installErrorDialog = new InstallErrorDialog();
            installErrorDialog.setArgument(charSequence);
            return installErrorDialog;
        }

        @Override
        protected Dialog createDialog(CharSequence charSequence) {
            return new AlertDialog.Builder(getActivity()).setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public final void onClick(DialogInterface dialogInterface, int i) {
                    this.f$0.getActivity().finish();
                }
            }).setMessage(getString(R.string.install_failed_msg, new Object[]{charSequence})).create();
        }
    }

    public static class ExternalSourcesBlockedDialog extends AppErrorDialog {
        static AppErrorDialog newInstance(String str) {
            ExternalSourcesBlockedDialog externalSourcesBlockedDialog = new ExternalSourcesBlockedDialog();
            externalSourcesBlockedDialog.setArgument(str);
            return externalSourcesBlockedDialog;
        }

        @Override
        protected Dialog createDialog(final CharSequence charSequence) {
            try {
                PackageManager packageManager = getActivity().getPackageManager();
                ApplicationInfo applicationInfo = packageManager.getApplicationInfo(charSequence.toString(), 0);
                return new AlertDialog.Builder(getActivity()).setTitle(packageManager.getApplicationLabel(applicationInfo)).setIcon(packageManager.getApplicationIcon(applicationInfo)).setMessage(R.string.untrusted_external_source_warning).setPositiveButton(R.string.external_sources_settings, new DialogInterface.OnClickListener() {
                    @Override
                    public final void onClick(DialogInterface dialogInterface, int i) {
                        PackageInstallerActivity.ExternalSourcesBlockedDialog.lambda$createDialog$0(this.f$0, charSequence, dialogInterface, i);
                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public final void onClick(DialogInterface dialogInterface, int i) {
                        this.f$0.getActivity().finish();
                    }
                }).create();
            } catch (PackageManager.NameNotFoundException e) {
                Log.e("PackageInstaller", "Did not find app info for " + ((Object) charSequence));
                getActivity().finish();
                return null;
            }
        }

        public static void lambda$createDialog$0(ExternalSourcesBlockedDialog externalSourcesBlockedDialog, CharSequence charSequence, DialogInterface dialogInterface, int i) {
            if (BenesseExtension.getDchaState() != 0) {
                return;
            }
            Intent intent = new Intent();
            intent.setAction("android.settings.MANAGE_UNKNOWN_APP_SOURCES");
            intent.setData(Uri.parse("package:" + ((Object) charSequence)));
            try {
                externalSourcesBlockedDialog.getActivity().startActivityForResult(intent, 1);
            } catch (ActivityNotFoundException e) {
                Log.e("PackageInstaller", "Settings activity not found for action: android.settings.MANAGE_UNKNOWN_APP_SOURCES");
            }
        }
    }

    public static abstract class AppErrorDialog extends DialogFragment {
        private static final String ARGUMENT_KEY = AppErrorDialog.class.getName() + "ARGUMENT_KEY";

        protected abstract Dialog createDialog(CharSequence charSequence);

        protected void setArgument(CharSequence charSequence) {
            Bundle bundle = new Bundle();
            bundle.putCharSequence(ARGUMENT_KEY, charSequence);
            setArguments(bundle);
        }

        @Override
        public Dialog onCreateDialog(Bundle bundle) {
            return createDialog(getArguments().getString(ARGUMENT_KEY));
        }

        @Override
        public void onCancel(DialogInterface dialogInterface) {
            getActivity().finish();
        }
    }
}
