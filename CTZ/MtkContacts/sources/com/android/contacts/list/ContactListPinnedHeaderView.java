package com.android.contacts.list;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.contacts.R;

public class ContactListPinnedHeaderView extends TextView {
    public ContactListPinnedHeaderView(Context context, AttributeSet attributeSet, View view) {
        super(context, attributeSet);
        if (R.styleable.ContactListItemView == null) {
            return;
        }
        TypedArray typedArrayObtainStyledAttributes = getContext().obtainStyledAttributes(attributeSet, R.styleable.ContactListItemView);
        int color = typedArrayObtainStyledAttributes.getColor(2, -1);
        int dimensionPixelSize = typedArrayObtainStyledAttributes.getDimensionPixelSize(25, 0);
        int dimensionPixelSize2 = typedArrayObtainStyledAttributes.getDimensionPixelSize(16, 0);
        int dimensionPixelSize3 = getResources().getDimensionPixelSize(R.dimen.contact_list_section_header_width) + dimensionPixelSize2;
        typedArrayObtainStyledAttributes.recycle();
        setBackgroundColor(color);
        setTextAppearance(getContext(), R.style.SectionHeaderStyle);
        setLayoutParams(new LinearLayout.LayoutParams(dimensionPixelSize3, -2));
        setLayoutDirection(view.getLayoutDirection());
        setGravity(17);
        setPaddingRelative(getPaddingStart() + dimensionPixelSize2, getPaddingTop() + (dimensionPixelSize * 2), getPaddingEnd(), getPaddingBottom());
    }

    public void setSectionHeaderTitle(String str) {
        if (str != null) {
            setText(str);
            setVisibility(0);
        } else {
            setVisibility(8);
        }
    }
}
