package com.android.phone.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.preference.ListPreference;
import android.preference.Preference;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import com.android.phone.R;
import java.util.List;
import java.util.Objects;

public class AccountSelectionPreference extends ListPreference implements Preference.OnPreferenceChangeListener {
    private PhoneAccountHandle[] mAccounts;
    private final Context mContext;
    private CharSequence[] mEntries;
    private String[] mEntryValues;
    private AccountSelectionListener mListener;
    BroadcastReceiver mPowerButtonReceiver;
    private boolean mShowSelectionInSummary;

    public interface AccountSelectionListener {
        void onAccountChanged(AccountSelectionPreference accountSelectionPreference);

        boolean onAccountSelected(AccountSelectionPreference accountSelectionPreference, PhoneAccountHandle phoneAccountHandle);

        void onAccountSelectionDialogShow(AccountSelectionPreference accountSelectionPreference);
    }

    public AccountSelectionPreference(Context context) {
        this(context, null);
    }

    public AccountSelectionPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mShowSelectionInSummary = true;
        this.mPowerButtonReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                Log.d("AccountSelectionPreference", "action:" + intent.getAction());
                Dialog dialog = AccountSelectionPreference.this.getDialog();
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        };
        this.mContext = context;
        setOnPreferenceChangeListener(this);
        initPreference();
    }

    public void setListener(AccountSelectionListener accountSelectionListener) {
        this.mListener = accountSelectionListener;
    }

    public void setModel(TelecomManager telecomManager, List<PhoneAccountHandle> list, PhoneAccountHandle phoneAccountHandle, CharSequence charSequence) {
        String strValueOf;
        this.mAccounts = (PhoneAccountHandle[]) list.toArray(new PhoneAccountHandle[list.size()]);
        this.mEntryValues = new String[this.mAccounts.length + 1];
        this.mEntries = new CharSequence[this.mAccounts.length + 1];
        PackageManager packageManager = this.mContext.getPackageManager();
        int length = this.mAccounts.length;
        int i = 0;
        while (i < this.mAccounts.length) {
            PhoneAccount phoneAccount = telecomManager.getPhoneAccount(this.mAccounts[i]);
            CharSequence label = phoneAccount.getLabel();
            if (label != null) {
                label = packageManager.getUserBadgedLabel(label, this.mAccounts[i].getUserHandle());
            }
            boolean zHasCapabilities = phoneAccount.hasCapabilities(4);
            CharSequence[] charSequenceArr = this.mEntries;
            if (TextUtils.isEmpty(label) && zHasCapabilities) {
                strValueOf = this.mContext.getString(R.string.phone_accounts_default_account_label);
            } else {
                strValueOf = String.valueOf(label);
            }
            charSequenceArr[i] = strValueOf;
            this.mEntryValues[i] = Integer.toString(i);
            if (Objects.equals(phoneAccountHandle, this.mAccounts[i])) {
                length = i;
            }
            i++;
        }
        this.mEntryValues[i] = Integer.toString(i);
        this.mEntries[i] = charSequence;
        setEntryValues(this.mEntryValues);
        setEntries(this.mEntries);
        setValueIndex(length);
        if (this.mShowSelectionInSummary) {
            setSummary(this.mEntries[length]);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        if (this.mListener != null) {
            int i = Integer.parseInt((String) obj);
            if (this.mListener.onAccountSelected(this, i < this.mAccounts.length ? this.mAccounts[i] : null)) {
                if (this.mShowSelectionInSummary) {
                    setSummary(this.mEntries[i]);
                }
                if (i != findIndexOfValue(getValue())) {
                    setValueIndex(i);
                    this.mListener.onAccountChanged(this);
                    return true;
                }
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        this.mListener.onAccountSelectionDialogShow(this);
        super.onPrepareDialogBuilder(builder);
    }

    public void doDestroy() {
        Log.d("AccountSelectionPreference", "doDestroy");
        this.mContext.unregisterReceiver(this.mPowerButtonReceiver);
    }

    private void initPreference() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.SCREEN_ON");
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        this.mContext.registerReceiver(this.mPowerButtonReceiver, intentFilter);
    }
}
