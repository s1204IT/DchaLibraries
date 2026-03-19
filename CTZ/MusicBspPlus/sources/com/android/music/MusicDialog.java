package com.android.music;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

class MusicDialog extends AlertDialog {
    private Activity mActivity;
    private final DialogInterface.OnCancelListener mCancelListener;
    private final DialogInterface.OnClickListener mListener;
    private final DialogInterface.OnKeyListener mSearchKeyListener;
    private View mView;

    public MusicDialog(Context context, DialogInterface.OnClickListener onClickListener, View view) {
        super(context);
        this.mCancelListener = new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                MusicDialog.this.mActivity.finish();
            }
        };
        this.mSearchKeyListener = new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialogInterface, int i, KeyEvent keyEvent) {
                if (84 == i) {
                    return true;
                }
                return false;
            }
        };
        this.mActivity = (Activity) context;
        this.mListener = onClickListener;
        this.mView = view;
    }

    @Override
    protected void onCreate(Bundle bundle) {
        if (this.mView != null) {
            setView(this.mView);
        }
        super.onCreate(bundle);
    }

    @Override
    public void setCancelable(boolean z) {
        if (z) {
            setOnCancelListener(this.mCancelListener);
        }
        super.setCancelable(z);
    }

    public void setSearchKeyListener() {
        setOnKeyListener(this.mSearchKeyListener);
    }

    public void setPositiveButton(CharSequence charSequence) {
        setButton(-1, charSequence, this.mListener);
    }

    public void setNeutralButton(CharSequence charSequence) {
        setButton(-3, charSequence, this.mListener);
    }
}
