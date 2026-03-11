package com.android.settings;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Process;
import android.os.UserHandle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.CheckBox;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;

public class AllowBindAppWidgetActivity extends AlertActivity implements DialogInterface.OnClickListener {
    private CheckBox mAlwaysUse;
    private int mAppWidgetId;
    private AppWidgetManager mAppWidgetManager;
    private Bundle mBindOptions;
    private String mCallingPackage;
    private boolean mClicked;
    private ComponentName mComponentName;
    private UserHandle mProfile;

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        this.mClicked = true;
        if (i == -1 && this.mAppWidgetId != -1 && this.mComponentName != null && this.mCallingPackage != null) {
            try {
                if (this.mAppWidgetManager.bindAppWidgetIdIfAllowed(this.mAppWidgetId, this.mProfile, this.mComponentName, this.mBindOptions)) {
                    Intent intent = new Intent();
                    intent.putExtra("appWidgetId", this.mAppWidgetId);
                    setResult(-1, intent);
                }
            } catch (Exception e) {
                Log.v("BIND_APPWIDGET", "Error binding widget with id " + this.mAppWidgetId + " and component " + this.mComponentName);
            }
            boolean zIsChecked = this.mAlwaysUse.isChecked();
            if (zIsChecked != this.mAppWidgetManager.hasBindAppWidgetPermission(this.mCallingPackage)) {
                this.mAppWidgetManager.setBindAppWidgetPermission(this.mCallingPackage, zIsChecked);
            }
        }
        finish();
    }

    protected void onPause() {
        if (!this.mClicked) {
            finish();
        }
        super.onPause();
    }

    protected void onCreate(Bundle bundle) {
        CharSequence applicationLabel;
        super.onCreate(bundle);
        setResult(0);
        Intent intent = getIntent();
        if (intent == null) {
            applicationLabel = "";
        } else {
            try {
                this.mAppWidgetId = intent.getIntExtra("appWidgetId", -1);
                this.mProfile = (UserHandle) intent.getParcelableExtra("appWidgetProviderProfile");
                if (this.mProfile == null) {
                    this.mProfile = Process.myUserHandle();
                }
                this.mComponentName = (ComponentName) intent.getParcelableExtra("appWidgetProvider");
                this.mBindOptions = (Bundle) intent.getParcelableExtra("appWidgetOptions");
                this.mCallingPackage = getCallingPackage();
                PackageManager packageManager = getPackageManager();
                applicationLabel = packageManager.getApplicationLabel(packageManager.getApplicationInfo(this.mCallingPackage, 0));
            } catch (Exception e) {
                this.mAppWidgetId = -1;
                this.mComponentName = null;
                this.mCallingPackage = null;
                Log.v("BIND_APPWIDGET", "Error getting parameters");
                finish();
                return;
            }
        }
        AlertController.AlertParams alertParams = this.mAlertParams;
        alertParams.mTitle = getString(R.string.allow_bind_app_widget_activity_allow_bind_title);
        alertParams.mMessage = getString(R.string.allow_bind_app_widget_activity_allow_bind, new Object[]{applicationLabel});
        alertParams.mPositiveButtonText = getString(R.string.create);
        alertParams.mNegativeButtonText = getString(android.R.string.cancel);
        alertParams.mPositiveButtonListener = this;
        alertParams.mNegativeButtonListener = this;
        alertParams.mView = ((LayoutInflater) getSystemService("layout_inflater")).inflate(android.R.layout.alert_dialog_leanback, (ViewGroup) null);
        this.mAlwaysUse = (CheckBox) alertParams.mView.findViewById(android.R.id.aboveThumb);
        this.mAlwaysUse.setText(getString(R.string.allow_bind_app_widget_activity_always_allow_bind, new Object[]{applicationLabel}));
        this.mAlwaysUse.setPadding(this.mAlwaysUse.getPaddingLeft(), this.mAlwaysUse.getPaddingTop(), this.mAlwaysUse.getPaddingRight(), (int) (this.mAlwaysUse.getPaddingBottom() + getResources().getDimension(R.dimen.bind_app_widget_dialog_checkbox_bottom_padding)));
        this.mAppWidgetManager = AppWidgetManager.getInstance(this);
        this.mAlwaysUse.setChecked(this.mAppWidgetManager.hasBindAppWidgetPermission(this.mCallingPackage, this.mProfile.getIdentifier()));
        setupAlert();
    }
}
