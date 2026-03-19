package com.android.certinstaller;

import android.view.View;
import android.widget.TextView;

class ViewHelper {
    private boolean mHasEmptyError;
    private View mView;

    ViewHelper() {
    }

    void setView(View view) {
        this.mView = view;
    }

    void showError(int i) {
        TextView textView = (TextView) this.mView.findViewById(R.id.error);
        textView.setText(i);
        if (textView != null) {
            textView.setVisibility(0);
        }
    }

    String getText(int i) {
        return ((TextView) this.mView.findViewById(i)).getText().toString();
    }

    void setText(int i, String str) {
        TextView textView;
        if (str != null && (textView = (TextView) this.mView.findViewById(i)) != null) {
            textView.setText(str);
        }
    }

    void setHasEmptyError(boolean z) {
        this.mHasEmptyError = z;
    }

    boolean getHasEmptyError() {
        return this.mHasEmptyError;
    }
}
