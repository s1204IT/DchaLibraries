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

    void showError(int msgId) {
        TextView v = (TextView) this.mView.findViewById(R.id.error);
        v.setText(msgId);
        if (v != null) {
            v.setVisibility(0);
        }
    }

    String getText(int viewId) {
        return ((TextView) this.mView.findViewById(viewId)).getText().toString();
    }

    void setText(int viewId, String text) {
        TextView v;
        if (text != null && (v = (TextView) this.mView.findViewById(viewId)) != null) {
            v.setText(text);
        }
    }

    void setHasEmptyError(boolean hasEmptyError) {
        this.mHasEmptyError = hasEmptyError;
    }

    boolean getHasEmptyError() {
        return this.mHasEmptyError;
    }
}
