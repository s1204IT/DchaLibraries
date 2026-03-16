package com.android.contacts.common.list;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import com.android.contacts.common.MoreContactUtils;
import com.android.contacts.common.util.ViewUtil;

public class ContactTilePhoneFrequentView extends ContactTileView {
    private String mPhoneNumberString;

    public ContactTilePhoneFrequentView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected boolean isDarkTheme() {
        return true;
    }

    @Override
    protected int getApproximateImageSize() {
        return ViewUtil.getConstantPreLayoutWidth(getQuickContact());
    }

    @Override
    public void loadFromContact(ContactEntry entry) {
        super.loadFromContact(entry);
        this.mPhoneNumberString = null;
        if (entry != null) {
            this.mPhoneNumberString = entry.phoneNumber;
        }
    }

    @Override
    protected View.OnClickListener createClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContactTilePhoneFrequentView.this.mListener != null) {
                    if (!TextUtils.isEmpty(ContactTilePhoneFrequentView.this.mPhoneNumberString)) {
                        ContactTilePhoneFrequentView.this.mListener.onCallNumberDirectly(ContactTilePhoneFrequentView.this.mPhoneNumberString);
                    } else {
                        ContactTilePhoneFrequentView.this.mListener.onContactSelected(ContactTilePhoneFrequentView.this.getLookupUri(), MoreContactUtils.getTargetRectFromView(ContactTilePhoneFrequentView.this));
                    }
                }
            }
        };
    }
}
