package com.android.settings.inputmethod;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.UserHandle;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.util.Log;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.Toast;
import com.android.internal.inputmethod.InputMethodUtils;
import com.android.settings.R;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedSwitchPreference;
import java.text.Collator;
import java.util.List;

class InputMethodPreference extends RestrictedSwitchPreference implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {
    private static final String TAG = InputMethodPreference.class.getSimpleName();
    private AlertDialog mDialog;
    private final boolean mHasPriorityInSorting;
    private final InputMethodInfo mImi;
    private final InputMethodSettingValuesWrapper mInputMethodSettingValues;
    private final boolean mIsAllowedByOrganization;
    private final OnSavePreferenceListener mOnSaveListener;

    interface OnSavePreferenceListener {
        void onSaveInputMethodPreference(InputMethodPreference inputMethodPreference);
    }

    InputMethodPreference(Context context, InputMethodInfo imi, boolean isImeEnabler, boolean isAllowedByOrganization, OnSavePreferenceListener onSaveListener) {
        super(context);
        this.mDialog = null;
        setPersistent(false);
        this.mImi = imi;
        this.mIsAllowedByOrganization = isAllowedByOrganization;
        this.mOnSaveListener = onSaveListener;
        if (!isImeEnabler) {
            setWidgetLayoutResource(0);
        }
        setSwitchTextOn("");
        setSwitchTextOff("");
        setKey(imi.getId());
        setTitle(imi.loadLabel(context.getPackageManager()));
        String settingsActivity = imi.getSettingsActivity();
        if (TextUtils.isEmpty(settingsActivity)) {
            setIntent(null);
        } else {
            Intent intent = new Intent("android.intent.action.MAIN");
            intent.setClassName(imi.getPackageName(), settingsActivity);
            setIntent(intent);
        }
        this.mInputMethodSettingValues = InputMethodSettingValuesWrapper.getInstance(context);
        this.mHasPriorityInSorting = InputMethodUtils.isSystemIme(imi) ? this.mInputMethodSettingValues.isValidSystemNonAuxAsciiCapableIme(imi, context) : false;
        setOnPreferenceClickListener(this);
        setOnPreferenceChangeListener(this);
    }

    public InputMethodInfo getInputMethodInfo() {
        return this.mImi;
    }

    private boolean isImeEnabler() {
        return getWidgetLayoutResource() != 0;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (!isImeEnabler()) {
            return false;
        }
        if (isChecked()) {
            setChecked(false);
            this.mOnSaveListener.onSaveInputMethodPreference(this);
            return false;
        }
        if (InputMethodUtils.isSystemIme(this.mImi)) {
            setChecked(true);
            this.mOnSaveListener.onSaveInputMethodPreference(this);
            return false;
        }
        showSecurityWarnDialog(this.mImi);
        return false;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (isImeEnabler()) {
            return true;
        }
        Context context = getContext();
        try {
            Intent intent = getIntent();
            if (intent != null) {
                context.startActivity(intent);
            }
        } catch (ActivityNotFoundException e) {
            Log.d(TAG, "IME's Settings Activity Not Found", e);
            String message = context.getString(R.string.failed_to_open_app_settings_toast, this.mImi.loadLabel(context.getPackageManager()));
            Toast.makeText(context, message, 1).show();
        }
        return true;
    }

    void updatePreferenceViews() {
        boolean isAlwaysChecked = this.mInputMethodSettingValues.isAlwaysCheckedIme(this.mImi, getContext());
        if (isAlwaysChecked && isImeEnabler()) {
            setDisabledByAdmin(null);
            setEnabled(false);
        } else if (!this.mIsAllowedByOrganization) {
            RestrictedLockUtils.EnforcedAdmin admin = RestrictedLockUtils.checkIfInputMethodDisallowed(getContext(), this.mImi.getPackageName(), UserHandle.myUserId());
            setDisabledByAdmin(admin);
        } else {
            setEnabled(true);
        }
        setChecked(this.mInputMethodSettingValues.isEnabledImi(this.mImi));
        if (isDisabledByAdmin()) {
            return;
        }
        setSummary(getSummaryString());
    }

    private InputMethodManager getInputMethodManager() {
        return (InputMethodManager) getContext().getSystemService("input_method");
    }

    private String getSummaryString() {
        InputMethodManager imm = getInputMethodManager();
        List<InputMethodSubtype> subtypes = imm.getEnabledInputMethodSubtypeList(this.mImi, true);
        return InputMethodAndSubtypeUtil.getSubtypeLocaleNameListAsSentence(subtypes, getContext(), this.mImi);
    }

    private void showSecurityWarnDialog(InputMethodInfo imi) {
        if (this.mDialog != null && this.mDialog.isShowing()) {
            this.mDialog.dismiss();
        }
        Context context = getContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(true);
        builder.setTitle(android.R.string.dialog_alert_title);
        CharSequence label = imi.getServiceInfo().applicationInfo.loadLabel(context.getPackageManager());
        builder.setMessage(context.getString(R.string.ime_security_warning, label));
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                InputMethodPreference.this.setChecked(true);
                InputMethodPreference.this.mOnSaveListener.onSaveInputMethodPreference(InputMethodPreference.this);
                InputMethodPreference.this.notifyChanged();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                InputMethodPreference.this.setChecked(false);
                InputMethodPreference.this.mOnSaveListener.onSaveInputMethodPreference(InputMethodPreference.this);
                InputMethodPreference.this.notifyChanged();
            }
        });
        this.mDialog = builder.create();
        this.mDialog.show();
    }

    int compareTo(InputMethodPreference rhs, Collator collator) {
        if (this == rhs) {
            return 0;
        }
        if (this.mHasPriorityInSorting != rhs.mHasPriorityInSorting) {
            return this.mHasPriorityInSorting ? -1 : 1;
        }
        CharSequence t0 = getTitle();
        CharSequence t1 = rhs.getTitle();
        if (TextUtils.isEmpty(t0)) {
            return 1;
        }
        if (TextUtils.isEmpty(t1)) {
            return -1;
        }
        return collator.compare(t0.toString(), t1.toString());
    }
}
