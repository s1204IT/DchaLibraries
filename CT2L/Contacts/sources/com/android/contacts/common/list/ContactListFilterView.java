package com.android.contacts.common.list;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import com.android.contacts.R;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.account.AccountType;

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

    public ContactListFilterView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setContactListFilter(ContactListFilter filter) {
        this.mFilter = filter;
    }

    public void setSingleAccount(boolean flag) {
        this.mSingleAccount = flag;
    }

    @Override
    public void setActivated(boolean activated) {
        super.setActivated(activated);
        if (this.mRadioButton != null) {
            this.mRadioButton.setChecked(activated);
        } else {
            Log.wtf(TAG, "radio-button cannot be activated because it is null");
        }
    }

    public void bindView(AccountTypeManager accountTypes) {
        if (this.mAccountType == null) {
            this.mIcon = (ImageView) findViewById(R.id.icon);
            this.mAccountType = (TextView) findViewById(R.id.accountType);
            this.mAccountUserName = (TextView) findViewById(R.id.accountUserName);
            this.mRadioButton = (RadioButton) findViewById(R.id.radioButton);
            this.mRadioButton.setChecked(isActivated());
        }
        if (this.mFilter == null) {
            this.mAccountType.setText(R.string.contactsList);
        }
        this.mAccountUserName.setVisibility(8);
        switch (this.mFilter.filterType) {
            case -6:
                bindView(0, R.string.list_filter_single);
                break;
            case -5:
                bindView(0, R.string.list_filter_phones);
                break;
            case -4:
                bindView(R.drawable.ic_menu_star_holo_light, R.string.list_filter_all_starred);
                break;
            case -3:
                bindView(R.drawable.ic_menu_settings_holo_light, R.string.list_filter_customize);
                break;
            case -2:
                bindView(0, R.string.list_filter_all_accounts);
                break;
            case 0:
                this.mAccountUserName.setVisibility(0);
                this.mIcon.setVisibility(0);
                if (this.mFilter.icon != null) {
                    this.mIcon.setImageDrawable(this.mFilter.icon);
                } else {
                    this.mIcon.setImageResource(R.drawable.unknown_source);
                }
                AccountType accountType = accountTypes.getAccountType(this.mFilter.accountType, this.mFilter.dataSet);
                this.mAccountUserName.setText(this.mFilter.accountName);
                this.mAccountType.setText(accountType.getDisplayLabel(getContext()));
                break;
        }
    }

    private void bindView(int iconResource, int textResource) {
        if (iconResource != 0) {
            this.mIcon.setVisibility(0);
            this.mIcon.setImageResource(iconResource);
        } else {
            this.mIcon.setVisibility(8);
        }
        this.mAccountType.setText(textResource);
    }
}
