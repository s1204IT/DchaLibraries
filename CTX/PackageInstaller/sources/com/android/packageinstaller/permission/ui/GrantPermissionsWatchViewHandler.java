package com.android.packageinstaller.permission.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.support.wearable.view.AcceptDenyDialog;
import android.support.wearable.view.WearableDialogHelper;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Space;
import com.android.packageinstaller.R;
import com.android.packageinstaller.permission.ui.GrantPermissionsViewHandler;

final class GrantPermissionsWatchViewHandler implements DialogInterface.OnClickListener, GrantPermissionsViewHandler {
    private final Context mContext;
    private String mCurrentPageText;
    private Dialog mDialog;
    private String mGroupName;
    private Icon mIcon;
    private CharSequence mMessage;
    private GrantPermissionsViewHandler.ResultListener mResultListener;
    private boolean mShowDoNotAsk;

    GrantPermissionsWatchViewHandler(Context context) {
        this.mContext = context;
    }

    public GrantPermissionsWatchViewHandler setResultListener(GrantPermissionsViewHandler.ResultListener resultListener) {
        this.mResultListener = resultListener;
        return this;
    }

    @Override
    public View createView() {
        return new Space(this.mContext);
    }

    @Override
    public void updateWindowAttributes(WindowManager.LayoutParams layoutParams) {
        layoutParams.width = -1;
        layoutParams.height = -1;
        layoutParams.format = -1;
        layoutParams.type = 2008;
        layoutParams.flags |= 128;
    }

    @Override
    public void updateUi(String str, int i, int i2, Icon icon, CharSequence charSequence, boolean z) {
        String string;
        if (Log.isLoggable("GrantPermsWatchViewH", 3)) {
            Log.d("GrantPermsWatchViewH", "updateUi() - groupName: " + str + ", groupCount: " + i + ", groupIndex: " + i2 + ", icon: " + icon + ", message: " + ((Object) charSequence) + ", showDoNotAsk: " + z);
        }
        this.mGroupName = str;
        this.mShowDoNotAsk = z;
        this.mMessage = charSequence;
        this.mIcon = icon;
        if (i > 1) {
            string = this.mContext.getString(R.string.current_permission_template, Integer.valueOf(i2 + 1), Integer.valueOf(i));
        } else {
            string = null;
        }
        this.mCurrentPageText = string;
        showDialog(null);
    }

    private void showDialog(Bundle bundle) {
        Drawable drawableLoadDrawable;
        TypedArray typedArrayObtainStyledAttributes = this.mContext.obtainStyledAttributes(new int[]{android.R.attr.textColorPrimary});
        int color = typedArrayObtainStyledAttributes.getColor(0, this.mContext.getColor(android.R.color.white));
        typedArrayObtainStyledAttributes.recycle();
        if (this.mIcon != null) {
            drawableLoadDrawable = this.mIcon.setTint(color).loadDrawable(this.mContext);
        } else {
            drawableLoadDrawable = null;
        }
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
        if (!TextUtils.isEmpty(this.mCurrentPageText)) {
            spannableStringBuilder.append(this.mCurrentPageText, new TextAppearanceSpan(this.mContext, R.style.BreadcrumbText), 33);
            spannableStringBuilder.append('\n');
        }
        if (!TextUtils.isEmpty(this.mMessage)) {
            spannableStringBuilder.append(this.mMessage, new TextAppearanceSpan(this.mContext, R.style.TitleText), 33);
        }
        if (this.mDialog != null) {
            this.mDialog.dismiss();
            this.mDialog = null;
        }
        if (this.mShowDoNotAsk) {
            AlertDialog alertDialogShow = new WearableDialogHelper.DialogBuilder(this.mContext).setPositiveIcon(R.drawable.confirm_button).setNeutralIcon(R.drawable.cancel_button).setNegativeIcon(R.drawable.deny_button).setTitle(spannableStringBuilder).setIcon(drawableLoadDrawable).setPositiveButton(R.string.grant_dialog_button_allow, this).setNeutralButton(R.string.grant_dialog_button_deny, this).setNegativeButton(R.string.grant_dialog_button_deny_dont_ask_again, this).show();
            alertDialogShow.getButton(-1).setId(R.id.permission_allow_button);
            alertDialogShow.getButton(-3).setId(R.id.permission_deny_button);
            alertDialogShow.getButton(-2).setId(R.id.permission_deny_dont_ask_again_button);
            this.mDialog = alertDialogShow;
        } else {
            AcceptDenyDialog acceptDenyDialog = new AcceptDenyDialog(this.mContext);
            acceptDenyDialog.setTitle(spannableStringBuilder);
            acceptDenyDialog.setIcon(drawableLoadDrawable);
            acceptDenyDialog.setPositiveButton(this);
            acceptDenyDialog.setNegativeButton(this);
            acceptDenyDialog.show();
            acceptDenyDialog.getButton(-1).setId(R.id.permission_allow_button);
            acceptDenyDialog.getButton(-2).setId(R.id.permission_deny_button);
            this.mDialog = acceptDenyDialog;
        }
        this.mDialog.setCancelable(false);
        if (bundle != null) {
            this.mDialog.onRestoreInstanceState(bundle);
        }
    }

    @Override
    public void saveInstanceState(Bundle bundle) {
        Bundle bundle2 = new Bundle();
        bundle2.putByte("show_do_not_ask", this.mShowDoNotAsk ? (byte) 1 : (byte) 0);
        bundle2.putString("group_name", this.mGroupName);
        bundle2.putBundle("dialog_bundle", this.mDialog.onSaveInstanceState());
        bundle.putBundle("watch_handler_bundle", bundle2);
    }

    @Override
    public void loadInstanceState(Bundle bundle) {
        Bundle bundle2 = bundle.getBundle("watch_handler_bundle");
        this.mShowDoNotAsk = bundle2.getByte("show_do_not_ask") == 1;
        this.mGroupName = bundle2.getString("group_name");
        showDialog(bundle2.getBundle("dialog_bundle"));
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        switch (i) {
            case -3:
                notifyListener(false, false);
                break;
            case -2:
                notifyListener(false, dialogInterface instanceof AlertDialog);
                break;
            case -1:
                notifyListener(true, false);
                break;
        }
    }

    private void notifyListener(boolean z, boolean z2) {
        if (this.mResultListener != null) {
            this.mResultListener.onPermissionGrantResult(this.mGroupName, z, z2);
        }
    }
}
