package com.android.internal.policy;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

public class PhoneLayoutInflater extends LayoutInflater {
    private static final String[] sClassPrefixList = {"android.widget.", "android.webkit.", "android.app."};

    public PhoneLayoutInflater(Context context) {
        super(context);
    }

    protected PhoneLayoutInflater(LayoutInflater layoutInflater, Context context) {
        super(layoutInflater, context);
    }

    @Override
    protected View onCreateView(String str, AttributeSet attributeSet) throws ClassNotFoundException {
        View viewCreateView;
        for (String str2 : sClassPrefixList) {
            try {
                viewCreateView = createView(str, str2, attributeSet);
            } catch (ClassNotFoundException e) {
            }
            if (viewCreateView != null) {
                return viewCreateView;
            }
        }
        return super.onCreateView(str, attributeSet);
    }

    @Override
    public LayoutInflater cloneInContext(Context context) {
        return new PhoneLayoutInflater(this, context);
    }
}
