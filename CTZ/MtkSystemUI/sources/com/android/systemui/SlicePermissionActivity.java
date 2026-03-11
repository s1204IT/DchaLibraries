package com.android.systemui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.slice.SliceManager;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.BidiFormatter;
import android.util.EventLog;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.TextView;

public class SlicePermissionActivity extends Activity implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener {
    private CheckBox mAllCheckbox;
    private String mCallingPkg;
    private String mProviderPkg;
    private Uri mUri;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mUri = (Uri) getIntent().getParcelableExtra("slice_uri");
        this.mCallingPkg = getIntent().getStringExtra("pkg");
        try {
            PackageManager packageManager = getPackageManager();
            this.mProviderPkg = packageManager.resolveContentProvider(this.mUri.getAuthority(), 128).applicationInfo.packageName;
            verifyCallingPkg();
            String strUnicodeWrap = BidiFormatter.getInstance().unicodeWrap(packageManager.getApplicationInfo(this.mCallingPkg, 0).loadSafeLabel(packageManager).toString());
            String strUnicodeWrap2 = BidiFormatter.getInstance().unicodeWrap(packageManager.getApplicationInfo(this.mProviderPkg, 0).loadSafeLabel(packageManager).toString());
            AlertDialog alertDialogCreate = new AlertDialog.Builder(this).setTitle(getString(R.string.slice_permission_title, new Object[]{strUnicodeWrap, strUnicodeWrap2})).setView(R.layout.slice_permission_request).setNegativeButton(R.string.slice_permission_deny, this).setPositiveButton(R.string.slice_permission_allow, this).setOnDismissListener(this).create();
            alertDialogCreate.getWindow().addPrivateFlags(524288);
            alertDialogCreate.show();
            ((TextView) alertDialogCreate.getWindow().getDecorView().findViewById(R.id.text1)).setText(getString(R.string.slice_permission_text_1, new Object[]{strUnicodeWrap2}));
            ((TextView) alertDialogCreate.getWindow().getDecorView().findViewById(R.id.text2)).setText(getString(R.string.slice_permission_text_2, new Object[]{strUnicodeWrap2}));
            this.mAllCheckbox = (CheckBox) alertDialogCreate.getWindow().getDecorView().findViewById(R.id.slice_permission_checkbox);
            this.mAllCheckbox.setText(getString(R.string.slice_permission_checkbox, new Object[]{strUnicodeWrap}));
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("SlicePermissionActivity", "Couldn't find package", e);
            finish();
        }
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        if (i == -1) {
            ((SliceManager) getSystemService(SliceManager.class)).grantPermissionFromUser(this.mUri, this.mCallingPkg, this.mAllCheckbox.isChecked());
        }
        finish();
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        finish();
    }

    private void verifyCallingPkg() {
        String stringExtra = getIntent().getStringExtra("provider_pkg");
        if (stringExtra == null || this.mProviderPkg.equals(stringExtra)) {
            return;
        }
        EventLog.writeEvent(1397638484, "159145361", Integer.valueOf(getUid(getCallingPkg())));
    }

    private String getCallingPkg() {
        Uri referrer = getReferrer();
        if (referrer == null) {
            return null;
        }
        return referrer.getHost();
    }

    private int getUid(String str) {
        if (str == null) {
            return -1;
        }
        try {
            return getPackageManager().getApplicationInfo(str, 0).uid;
        } catch (PackageManager.NameNotFoundException e) {
            return -1;
        }
    }
}
