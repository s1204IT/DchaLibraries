package com.android.contacts.common.list;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.contacts.common.R;
import com.android.contacts.common.util.ViewUtil;

public class ContactListPinnedHeaderView extends TextView {
    public ContactListPinnedHeaderView(Context context, AttributeSet attrs, View parent) {
        super(context, attrs);
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ContactListItemView);
        int backgroundColor = a.getColor(15, -1);
        int textOffsetTop = a.getDimensionPixelSize(23, 0);
        int paddingStartOffset = a.getDimensionPixelSize(7, 0);
        int textWidth = getResources().getDimensionPixelSize(com.android.contacts.R.dimen.contact_list_section_header_width);
        int widthIncludingPadding = paddingStartOffset + textWidth;
        a.recycle();
        setBackgroundColor(backgroundColor);
        setTextAppearance(getContext(), com.android.contacts.R.style.SectionHeaderStyle);
        setLayoutParams(new LinearLayout.LayoutParams(widthIncludingPadding, -2));
        setLayoutDirection(parent.getLayoutDirection());
        setGravity((ViewUtil.isViewLayoutRtl(this) ? 5 : 3) | 16);
        setPaddingRelative(getPaddingStart() + paddingStartOffset, getPaddingTop() + (textOffsetTop * 2), getPaddingEnd(), getPaddingBottom());
    }

    public void setSectionHeaderTitle(String title) {
        if (!TextUtils.isEmpty(title)) {
            setText(title);
            setVisibility(0);
        } else {
            setVisibility(8);
        }
    }
}
