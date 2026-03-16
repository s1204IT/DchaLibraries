package com.android.phone.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.preference.ListPreference;
import android.preference.Preference;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.util.AttributeSet;
import java.util.List;
import java.util.Objects;

public class AccountSelectionPreference extends ListPreference implements Preference.OnPreferenceChangeListener {
    private PhoneAccountHandle[] mAccounts;
    private final Context mContext;
    private CharSequence[] mEntries;
    private String[] mEntryValues;
    private AccountSelectionListener mListener;
    private boolean mShowSelectionInSummary;

    public interface AccountSelectionListener {
        void onAccountChanged(AccountSelectionPreference accountSelectionPreference);

        boolean onAccountSelected(AccountSelectionPreference accountSelectionPreference, PhoneAccountHandle phoneAccountHandle);

        void onAccountSelectionDialogShow(AccountSelectionPreference accountSelectionPreference);
    }

    public AccountSelectionPreference(Context context) {
        this(context, null);
    }

    public AccountSelectionPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mShowSelectionInSummary = true;
        this.mContext = context;
        setOnPreferenceChangeListener(this);
    }

    public void setListener(AccountSelectionListener listener) {
        this.mListener = listener;
    }

    public void setModel(TelecomManager telecomManager, List<PhoneAccountHandle> accountsList, PhoneAccountHandle currentSelection, CharSequence nullSelectionString) {
        this.mAccounts = (PhoneAccountHandle[]) accountsList.toArray(new PhoneAccountHandle[accountsList.size()]);
        this.mEntryValues = new String[this.mAccounts.length + 1];
        this.mEntries = new CharSequence[this.mAccounts.length + 1];
        PackageManager pm = this.mContext.getPackageManager();
        int selectedIndex = this.mAccounts.length;
        int i = 0;
        while (i < this.mAccounts.length) {
            CharSequence label = telecomManager.getPhoneAccount(this.mAccounts[i]).getLabel();
            if (label != null) {
                label = pm.getUserBadgedLabel(label, this.mAccounts[i].getUserHandle());
            }
            this.mEntries[i] = label == null ? null : label.toString();
            this.mEntryValues[i] = Integer.toString(i);
            if (Objects.equals(currentSelection, this.mAccounts[i])) {
                selectedIndex = i;
            }
            i++;
        }
        this.mEntryValues[i] = Integer.toString(i);
        this.mEntries[i] = nullSelectionString;
        setEntryValues(this.mEntryValues);
        setEntries(this.mEntries);
        setValueIndex(selectedIndex);
        if (this.mShowSelectionInSummary) {
            setSummary(this.mEntries[selectedIndex]);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (this.mListener != null) {
            int index = Integer.parseInt((String) newValue);
            PhoneAccountHandle account = index < this.mAccounts.length ? this.mAccounts[index] : null;
            if (this.mListener.onAccountSelected(this, account)) {
                if (this.mShowSelectionInSummary) {
                    setSummary(this.mEntries[index]);
                }
                if (index != findIndexOfValue(getValue())) {
                    setValueIndex(index);
                    this.mListener.onAccountChanged(this);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        this.mListener.onAccountSelectionDialogShow(this);
        super.onPrepareDialogBuilder(builder);
    }
}
