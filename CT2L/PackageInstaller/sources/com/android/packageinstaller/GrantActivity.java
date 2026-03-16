package com.android.packageinstaller;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AppSecurityPermissions;
import android.widget.Button;
import android.widget.TabHost;
import android.widget.TextView;
import com.android.packageinstaller.PackageUtil;
import java.util.ArrayList;
import java.util.List;

public class GrantActivity extends Activity implements View.OnClickListener {
    private Button mCancel;
    private Button mOk;
    private PackageManager mPm;
    private String mRequestingPackage;
    private String[] requested_permissions;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mPm = getPackageManager();
        this.mRequestingPackage = getCallingPackage();
        this.requested_permissions = getRequestedPermissions();
        if (this.requested_permissions.length == 0) {
            setResult(-1);
            finish();
            return;
        }
        PackageInfo pkgInfo = getUpdatedPackageInfo();
        AppSecurityPermissions perms = new AppSecurityPermissions(this, pkgInfo);
        if (perms.getPermissionCount(4) == 0) {
            setResult(-1);
            finish();
            return;
        }
        setContentView(R.layout.install_start);
        ((TextView) findViewById(R.id.install_confirm_question)).setText(R.string.grant_confirm_question);
        PackageUtil.AppSnippet as = new PackageUtil.AppSnippet(this.mPm.getApplicationLabel(pkgInfo.applicationInfo), this.mPm.getApplicationIcon(pkgInfo.applicationInfo));
        PackageUtil.initSnippetForNewApp(this, as, R.id.app_snippet);
        this.mOk = (Button) findViewById(R.id.ok_button);
        this.mOk.setText(R.string.ok);
        this.mCancel = (Button) findViewById(R.id.cancel_button);
        this.mOk.setOnClickListener(this);
        this.mCancel.setOnClickListener(this);
        TabHost tabHost = (TabHost) findViewById(android.R.id.tabhost);
        tabHost.setup();
        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        TabsAdapter adapter = new TabsAdapter(this, tabHost, viewPager);
        View newTab = perms.getPermissionsView(4);
        View allTab = getPermissionList(perms);
        adapter.addTab(tabHost.newTabSpec("new").setIndicator(getText(R.string.newPerms)), newTab);
        adapter.addTab(tabHost.newTabSpec("all").setIndicator(getText(R.string.allPerms)), allTab);
    }

    private PackageInfo getUpdatedPackageInfo() {
        try {
            PackageInfo pkgInfo = this.mPm.getPackageInfo(this.mRequestingPackage, 4096);
            for (int i = 0; i < pkgInfo.requestedPermissions.length; i++) {
                String[] arr$ = this.requested_permissions;
                for (String requested_permission : arr$) {
                    if (requested_permission.equals(pkgInfo.requestedPermissions[i])) {
                        int[] iArr = pkgInfo.requestedPermissionsFlags;
                        iArr[i] = iArr[i] | 2;
                    }
                }
            }
            return pkgInfo;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private View getPermissionList(AppSecurityPermissions perms) {
        LayoutInflater inflater = (LayoutInflater) getSystemService("layout_inflater");
        View root = inflater.inflate(R.layout.permissions_list, (ViewGroup) null);
        View personalPermissions = perms.getPermissionsView(1);
        View devicePermissions = perms.getPermissionsView(2);
        ((ViewGroup) root.findViewById(R.id.privacylist)).addView(personalPermissions);
        ((ViewGroup) root.findViewById(R.id.devicelist)).addView(devicePermissions);
        return root;
    }

    private String[] getRequestedPermissions() {
        String[] permissions = getIntent().getStringArrayExtra("android.content.pm.extra.PERMISSION_LIST");
        if (permissions == null) {
            return new String[0];
        }
        return keepRequestingPackagePermissions(keepNormalDangerousPermissions(permissions));
    }

    private String[] keepRequestingPackagePermissions(String[] permissions) {
        List<String> result = new ArrayList<>();
        try {
            PackageInfo pkgInfo = this.mPm.getPackageInfo(this.mRequestingPackage, 4096);
            if (pkgInfo.requestedPermissions == null) {
                return new String[0];
            }
            for (int i = 0; i < pkgInfo.requestedPermissions.length; i++) {
                int len$ = permissions.length;
                int i$ = 0;
                while (true) {
                    if (i$ < len$) {
                        String permission = permissions[i$];
                        boolean isRequired = (pkgInfo.requestedPermissionsFlags[i] & 1) != 0;
                        boolean isGranted = (pkgInfo.requestedPermissionsFlags[i] & 2) != 0;
                        if (!permission.equals(pkgInfo.requestedPermissions[i]) || isRequired || isGranted) {
                            i$++;
                        } else {
                            result.add(permission);
                            break;
                        }
                    }
                }
            }
            return (String[]) result.toArray(new String[result.size()]);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private String[] keepNormalDangerousPermissions(String[] permissions) {
        List<String> result = new ArrayList<>();
        for (String permission : permissions) {
            try {
                PermissionInfo pInfo = this.mPm.getPermissionInfo(permission, 0);
                int base = pInfo.protectionLevel & 15;
                if (base == 0 || base == 1) {
                    result.add(permission);
                }
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
        return (String[]) result.toArray(new String[result.size()]);
    }

    @Override
    public void onClick(View v) {
        if (v == this.mOk) {
            String[] arr$ = this.requested_permissions;
            for (String permission : arr$) {
                this.mPm.grantPermission(this.mRequestingPackage, permission);
            }
            setResult(-1);
        }
        if (v == this.mCancel) {
            setResult(0);
        }
        finish();
    }
}
