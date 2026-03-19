package com.mediatek.contacts.list;

import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import com.android.contacts.list.ContactListItemView;

public class ContactListItemViewEx {
    private CheckBox mChecktBox = null;
    private ContactListItemView mContactListItemView;

    public ContactListItemViewEx(ContactListItemView contactListItemView) {
        this.mContactListItemView = null;
        this.mContactListItemView = contactListItemView;
    }

    public void setCheckable(boolean z) {
        if (z) {
            if (this.mChecktBox == null) {
                getCheckBox();
            }
            this.mChecktBox.setVisibility(0);
        } else if (this.mChecktBox != null) {
            this.mChecktBox.setVisibility(8);
        }
    }

    public CheckBox getCheckBox() {
        if (this.mChecktBox == null) {
            this.mChecktBox = new CheckBox(this.mContactListItemView.getContext());
            this.mChecktBox.setClickable(false);
            this.mChecktBox.setFocusable(false);
            this.mChecktBox.setFocusableInTouchMode(false);
            this.mContactListItemView.addView(this.mChecktBox);
        }
        this.mChecktBox.setVisibility(0);
        return this.mChecktBox;
    }

    private boolean isVisible(View view) {
        return view != null && view.getVisibility() == 0;
    }

    public void measureTextView(TextView textView) {
        if (isVisible(this.mChecktBox)) {
            textView.measure(View.MeasureSpec.makeMeasureSpec(textView.getWidth(), 1073741824), View.MeasureSpec.makeMeasureSpec(textView.getHeight(), 1073741824));
        }
    }
}
