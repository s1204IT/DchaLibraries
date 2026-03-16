package com.android.contacts.common.list;

import android.content.Context;
import android.util.AttributeSet;
import com.android.contacts.common.util.ViewUtil;

public class ContactTileFrequentView extends ContactTileView {
    public ContactTileFrequentView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected boolean isDarkTheme() {
        return false;
    }

    @Override
    protected int getApproximateImageSize() {
        return ViewUtil.getConstantPreLayoutWidth(getPhotoView());
    }
}
