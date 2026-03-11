package com.android.settings.fuelgauge;

import android.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Checkable;
import android.widget.TextView;
import com.android.settingslib.applications.ApplicationsState;

public class HighPowerDetail extends DialogFragment implements DialogInterface.OnClickListener, View.OnClickListener {
    private final PowerWhitelistBackend mBackend = PowerWhitelistBackend.getInstance();
    private boolean mDefaultOn;
    private boolean mIsEnabled;
    private CharSequence mLabel;
    private Checkable mOptionOff;
    private Checkable mOptionOn;
    private String mPackageName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mPackageName = getArguments().getString("package");
        PackageManager pm = getContext().getPackageManager();
        try {
            this.mLabel = pm.getApplicationInfo(this.mPackageName, 0).loadLabel(pm);
        } catch (PackageManager.NameNotFoundException e) {
            this.mLabel = this.mPackageName;
        }
        this.mDefaultOn = getArguments().getBoolean("default_on");
        this.mIsEnabled = !this.mDefaultOn ? this.mBackend.isWhitelisted(this.mPackageName) : true;
    }

    public Checkable setup(View view, boolean on) {
        ((TextView) view.findViewById(R.id.title)).setText(on ? com.android.settings.R.string.ignore_optimizations_on : com.android.settings.R.string.ignore_optimizations_off);
        ((TextView) view.findViewById(R.id.summary)).setText(on ? com.android.settings.R.string.ignore_optimizations_on_desc : com.android.settings.R.string.ignore_optimizations_off_desc);
        view.setClickable(true);
        view.setOnClickListener(this);
        if (!on && this.mBackend.isSysWhitelisted(this.mPackageName)) {
            view.setEnabled(false);
        }
        return (Checkable) view;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder b = new AlertDialog.Builder(getContext()).setTitle(this.mLabel).setNegativeButton(com.android.settings.R.string.cancel, (DialogInterface.OnClickListener) null).setView(com.android.settings.R.layout.ignore_optimizations_content);
        if (!this.mBackend.isSysWhitelisted(this.mPackageName)) {
            b.setPositiveButton(com.android.settings.R.string.done, this);
        }
        return b.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        this.mOptionOn = setup(getDialog().findViewById(com.android.settings.R.id.ignore_on), true);
        this.mOptionOff = setup(getDialog().findViewById(com.android.settings.R.id.ignore_off), false);
        updateViews();
    }

    private void updateViews() {
        this.mOptionOn.setChecked(this.mIsEnabled);
        this.mOptionOff.setChecked(!this.mIsEnabled);
    }

    @Override
    public void onClick(View v) {
        if (v == this.mOptionOn) {
            this.mIsEnabled = true;
            updateViews();
        } else {
            if (v != this.mOptionOff) {
                return;
            }
            this.mIsEnabled = false;
            updateViews();
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which != -1) {
            return;
        }
        boolean newValue = this.mIsEnabled;
        boolean oldValue = this.mBackend.isWhitelisted(this.mPackageName);
        if (newValue == oldValue) {
            return;
        }
        if (newValue) {
            this.mBackend.addApp(this.mPackageName);
        } else {
            this.mBackend.removeApp(this.mPackageName);
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        Fragment target = getTargetFragment();
        if (target == null) {
            return;
        }
        target.onActivityResult(getTargetRequestCode(), 0, null);
    }

    public static CharSequence getSummary(Context context, ApplicationsState.AppEntry entry) {
        return getSummary(context, entry.info.packageName);
    }

    public static CharSequence getSummary(Context context, String pkg) {
        int i;
        PowerWhitelistBackend powerWhitelist = PowerWhitelistBackend.getInstance();
        if (powerWhitelist.isSysWhitelisted(pkg)) {
            i = com.android.settings.R.string.high_power_system;
        } else {
            i = powerWhitelist.isWhitelisted(pkg) ? com.android.settings.R.string.high_power_on : com.android.settings.R.string.high_power_off;
        }
        return context.getString(i);
    }

    public static void show(Fragment caller, String packageName, int requestCode, boolean defaultToOn) {
        HighPowerDetail fragment = new HighPowerDetail();
        Bundle args = new Bundle();
        args.putString("package", packageName);
        args.putBoolean("default_on", defaultToOn);
        fragment.setArguments(args);
        fragment.setTargetFragment(caller, requestCode);
        fragment.show(caller.getFragmentManager(), HighPowerDetail.class.getSimpleName());
    }
}
