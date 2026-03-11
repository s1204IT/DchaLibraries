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

    public HttpAuthenticationDialog(Context context, String host, String realm) {
        this.mContext = context;
        this.mHost = host;
        this.mRealm = realm;
        createDialog();
    }

    public String getUsername() {
        return this.mUsernameView.getText().toString();
    }

    public String getPassword() {
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
        int focusId = this.mDialog.getCurrentFocus().getId();
        this.mDialog.dismiss();
        createDialog();
        this.mDialog.show();
        if (username != null) {
            this.mUsernameView.setText(username);
        }
        if (password != null) {
            this.mPasswordView.setText(password);
        }
        if (focusId != 0) {
            this.mDialog.findViewById(focusId).requestFocus();
        } else {
            this.mUsernameView.requestFocus();
        }
    }

    private void createDialog() {
        LayoutInflater factory = LayoutInflater.from(this.mContext);
        View v = factory.inflate(R.layout.http_authentication, (ViewGroup) null);
        this.mUsernameView = (TextView) v.findViewById(R.id.username_edit);
        this.mPasswordView = (TextView) v.findViewById(R.id.password_edit);
        this.mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v2, int actionId, KeyEvent event) {
                if (actionId != 6) {
                    return false;
                }
                HttpAuthenticationDialog.this.mDialog.getButton(-1).performClick();
                return true;
            }
        });
        String title = this.mContext.getText(R.string.sign_in_to).toString().replace("%s1", this.mHost).replace("%s2", this.mRealm);
        this.mDialog = new AlertDialog.Builder(this.mContext).setTitle(title).setIconAttribute(android.R.attr.alertDialogIcon).setView(v).setPositiveButton(R.string.action, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                if (HttpAuthenticationDialog.this.mOkListener != null) {
                    HttpAuthenticationDialog.this.mOkListener.onOk(HttpAuthenticationDialog.this.mHost, HttpAuthenticationDialog.this.mRealm, HttpAuthenticationDialog.this.getUsername(), HttpAuthenticationDialog.this.getPassword());
                }
            }
        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                if (HttpAuthenticationDialog.this.mCancelListener != null) {
                    HttpAuthenticationDialog.this.mCancelListener.onCancel();
                }
            }
        }).setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (HttpAuthenticationDialog.this.mCancelListener != null) {
                    HttpAuthenticationDialog.this.mCancelListener.onCancel();
                }
            }
        }).create();
        this.mDialog.getWindow().setSoftInputMode(4);
    }
}
