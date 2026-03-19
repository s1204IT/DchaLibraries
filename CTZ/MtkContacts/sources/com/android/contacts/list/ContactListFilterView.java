package com.android.contacts.list;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import com.android.contacts.R;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.account.AccountType;
import com.mediatek.contacts.simcontact.SubInfoUtils;
import com.mediatek.contacts.util.ContactsCommonListUtils;
import com.mediatek.contacts.util.Log;

public class ContactListFilterView extends LinearLayout {
    private static final String TAG = ContactListFilterView.class.getSimpleName();
    private TextView mAccountType;
    private TextView mAccountUserName;
    private ContactListFilter mFilter;
    private ImageView mIcon;
    private RadioButton mRadioButton;
    private boolean mSingleAccount;

    public ContactListFilterView(Context context) {
        super(context);
    }

    public ContactListFilterView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public void setContactListFilter(ContactListFilter contactListFilter) {
        this.mFilter = contactListFilter;
    }

    public void setSingleAccount(boolean z) {
        this.mSingleAccount = z;
    }

    @Override
    public void setActivated(boolean z) {
        super.setActivated(z);
        if (this.mRadioButton != null) {
            this.mRadioButton.setChecked(z);
        } else {
            Log.wtf(TAG, "radio-button cannot be activated because it is null");
        }
        setContentDescription(generateContentDescription());
    }

    public boolean isChecked() {
        return this.mRadioButton.isChecked();
    }

    public void bindView(AccountTypeManager accountTypeManager) {
        if (this.mAccountType == null) {
            this.mIcon = (ImageView) findViewById(R.id.icon);
            this.mAccountType = (TextView) findViewById(R.id.accountType);
            this.mAccountUserName = (TextView) findViewById(R.id.accountUserName);
            this.mRadioButton = (RadioButton) findViewById(R.id.radioButton);
            this.mRadioButton.setChecked(isActivated());
        }
        if (this.mFilter == null) {
            this.mAccountType.setText(R.string.contactsList);
            return;
        }
        this.mAccountUserName.setVisibility(8);
        int i = this.mFilter.filterType;
        if (i != 0) {
            switch (i) {
                case -6:
                    bindView(0, R.string.list_filter_single);
                    break;
                case -5:
                    bindView(0, R.string.list_filter_phones);
                    break;
                case -4:
                    bindView(R.drawable.quantum_ic_star_vd_theme_24, R.string.list_filter_all_starred);
                    break;
                case -3:
                    bindView(0, R.string.list_filter_customize);
                    break;
                case -2:
                    bindView(0, R.string.list_filter_all_accounts);
                    break;
            }
        } else {
            this.mAccountUserName.setVisibility(0);
            this.mIcon.setVisibility(0);
            if (this.mFilter.icon != null) {
                this.mIcon.setImageDrawable(this.mFilter.icon);
            } else {
                this.mIcon.setImageResource(R.drawable.unknown_source);
            }
            AccountType accountType = accountTypeManager.getAccountType(this.mFilter.accountType, this.mFilter.dataSet);
            if (accountType.isIccCardAccount()) {
                this.mAccountUserName.setText(accountType.getDisplayLabel(this.mContext));
            } else {
                this.mAccountUserName.setText(this.mFilter.accountName);
            }
            this.mAccountType.setText(accountType.getDisplayLabel(getContext()));
            if (SubInfoUtils.getActivatedSubInfoCount() == 1 && accountType.isIccCardAccount()) {
                this.mAccountType.setVisibility(8);
                this.mAccountUserName.setTextAppearance(this.mContext, android.R.attr.textAppearanceMedium);
                this.mAccountUserName.setTextSize(18.0f);
            } else {
                this.mAccountType.setVisibility(0);
            }
            ContactsCommonListUtils.setAccountTypeText(getContext(), accountType, this.mAccountType, this.mAccountUserName, this.mFilter);
        }
        setContentDescription(generateContentDescription());
    }

    private void bindView(int i, int i2) {
        if (i != 0) {
            this.mIcon.setVisibility(0);
            this.mIcon.setImageResource(i);
        } else {
            this.mIcon.setVisibility(8);
        }
        this.mAccountType.setText(i2);
    }

    String generateContentDescription() {
        StringBuilder sb = new StringBuilder();
        if (!TextUtils.isEmpty(this.mAccountType.getText())) {
            sb.append(this.mAccountType.getText());
        }
        if (!TextUtils.isEmpty(this.mAccountUserName.getText())) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(this.mAccountUserName.getText());
        }
        return getContext().getString(isActivated() ? R.string.account_filter_view_checked : R.string.account_filter_view_not_checked, sb.toString());
    }
}
