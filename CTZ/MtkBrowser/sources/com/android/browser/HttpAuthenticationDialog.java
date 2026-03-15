package com.android.browser;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class HttpAuthenticationDialog {
    private CancelListener mCancelListener;
    private final Context mContext;
    private AlertDialog mDialog;
    private final String mHost;
    private OkListener mOkListener;
    private TextView mPasswordView;
    private final String mRealm;
    private TextView mUsernameView;

    public interface CancelListener {
        void onCancel();
    }

    public interface OkListener {
        void onOk(String str, String str2, String str3, String str4);
    }

    public HttpAuthenticationDialog(Context context, String str, String str2) {
        this.mContext = context;
        this.mHost = str;
        this.mRealm = str2;
        createDialog();
    }

    private void createDialog() {
        View viewInflate = LayoutInflater.from(this.mContext).inflate(2130968606, (ViewGroup) null);
        this.mUsernameView = (TextView) viewInflate.findViewById(2131558488);
        this.mPasswordView = (TextView) viewInflate.findViewById(2131558489);
        this.mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener(this) {
            final HttpAuthenticationDialog this$0;

            {
                this.this$0 = this;
            }

            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i != 6) {
                    return false;
                }
                this.this$0.mDialog.getButton(-1).performClick();
                return true;
            }
        });
        this.mDialog = new AlertDialog.Builder(this.mContext).setTitle(this.mContext.getText(2131492957).toString().replace("%s1", this.mHost).replace("%s2", this.mRealm)).setIconAttribute(android.R.attr.alertDialogIcon).setView(viewInflate).setPositiveButton(2131492960, new DialogInterface.OnClickListener(this) {
            final HttpAuthenticationDialog this$0;

            {
                this.this$0 = this;
            }

            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (this.this$0.mOkListener != null) {
                    this.this$0.mOkListener.onOk(this.this$0.mHost, this.this$0.mRealm, this.this$0.getUsername(), this.this$0.getPassword());
                }
            }
        }).setNegativeButton(2131492963, new DialogInterface.OnClickListener(this) {
            final HttpAuthenticationDialog this$0;

            {
                this.this$0 = this;
            }

            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (this.this$0.mCancelListener != null) {
                    this.this$0.mCancelListener.onCancel();
                }
            }
        }).setOnCancelListener(new DialogInterface.OnCancelListener(this) {
            final HttpAuthenticationDialog this$0;

            {
                this.this$0 = this;
            }

            @Override
            public void onCancel(DialogInterface dialogInterface) {
                if (this.this$0.mCancelListener != null) {
                    this.this$0.mCancelListener.onCancel();
                }
            }
        }).create();
        this.mDialog.getWindow().setSoftInputMode(4);
    }

    private String getPassword() {
        return this.mPasswordView.getText().toString();
    }

    private String getUsername() {
        return this.mUsernameView.getText().toString();
    }

    public void reshow() {
        String username = getUsername();
        String password = getPassword();
        int id = this.mDialog.getCurrentFocus().getId();
        this.mDialog.dismiss();
        createDialog();
        this.mDialog.show();
        if (username != null) {
            this.mUsernameView.setText(username);
        }
        if (password != null) {
            this.mPasswordView.setText(password);
        }
        if (id != 0) {
            this.mDialog.findViewById(id).requestFocus();
        } else {
            this.mUsernameView.requestFocus();
        }
    }

    public void setCancelListener(CancelListener cancelListener) {
        this.mCancelListener = cancelListener;
    }

    public void setOkListener(OkListener okListener) {
        this.mOkListener = okListener;
    }

    public void show() {
        this.mDialog.show();
        this.mUsernameView.requestFocus();
    }
}
