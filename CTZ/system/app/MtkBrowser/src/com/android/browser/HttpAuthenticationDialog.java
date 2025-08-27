package com.android.browser;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/* loaded from: classes.dex */
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

    private String getUsername() {
        return this.mUsernameView.getText().toString();
    }

    private String getPassword() {
        return this.mPasswordView.getText().toString();
    }

    public void setOkListener(OkListener okListener) {
        this.mOkListener = okListener;
    }

    public void setCancelListener(CancelListener cancelListener) {
        this.mCancelListener = cancelListener;
    }

    public void show() {
        this.mDialog.show();
        this.mUsernameView.requestFocus();
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

    private void createDialog() {
        View viewInflate = LayoutInflater.from(this.mContext).inflate(R.layout.http_authentication, (ViewGroup) null);
        this.mUsernameView = (TextView) viewInflate.findViewById(R.id.username_edit);
        this.mPasswordView = (TextView) viewInflate.findViewById(R.id.password_edit);
        this.mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() { // from class: com.android.browser.HttpAuthenticationDialog.1
            @Override // android.widget.TextView.OnEditorActionListener
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == 6) {
                    HttpAuthenticationDialog.this.mDialog.getButton(-1).performClick();
                    return true;
                }
                return false;
            }
        });
        this.mDialog = new AlertDialog.Builder(this.mContext).setTitle(this.mContext.getText(R.string.sign_in_to).toString().replace("%s1", this.mHost).replace("%s2", this.mRealm)).setIconAttribute(android.R.attr.alertDialogIcon).setView(viewInflate).setPositiveButton(R.string.action, new DialogInterface.OnClickListener() { // from class: com.android.browser.HttpAuthenticationDialog.4
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i) {
                if (HttpAuthenticationDialog.this.mOkListener != null) {
                    HttpAuthenticationDialog.this.mOkListener.onOk(HttpAuthenticationDialog.this.mHost, HttpAuthenticationDialog.this.mRealm, HttpAuthenticationDialog.this.getUsername(), HttpAuthenticationDialog.this.getPassword());
                }
            }
        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() { // from class: com.android.browser.HttpAuthenticationDialog.3
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i) {
                if (HttpAuthenticationDialog.this.mCancelListener != null) {
                    HttpAuthenticationDialog.this.mCancelListener.onCancel();
                }
            }
        }).setOnCancelListener(new DialogInterface.OnCancelListener() { // from class: com.android.browser.HttpAuthenticationDialog.2
            @Override // android.content.DialogInterface.OnCancelListener
            public void onCancel(DialogInterface dialogInterface) {
                if (HttpAuthenticationDialog.this.mCancelListener != null) {
                    HttpAuthenticationDialog.this.mCancelListener.onCancel();
                }
            }
        }).create();
        this.mDialog.getWindow().setSoftInputMode(4);
    }
}
