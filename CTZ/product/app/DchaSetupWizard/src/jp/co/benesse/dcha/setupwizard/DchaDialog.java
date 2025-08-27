package jp.co.benesse.dcha.setupwizard;

import android.app.Dialog;
import android.app.DialogFragment;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import jp.co.benesse.dcha.util.Logger;

/* loaded from: classes.dex */
public class DchaDialog extends DialogFragment {
    public static final int DIALOG_TYPE_NETWORK_ERROR = 3;
    public static final int DIALOG_TYPE_SYSTEM_ERROR = 2;
    public static final int DIALOG_TYPE_WIFI_DISCONNECT = 1;
    private static final String TAG = DchaDialog.class.getSimpleName();
    protected ParentSettingActivity mActivity;
    protected int mDialogType;
    protected String mMsg;

    public DchaDialog(ParentSettingActivity parentSettingActivity, int i) {
        Logger.d(TAG, "DchaDialog 0001");
        this.mActivity = parentSettingActivity;
        this.mDialogType = i;
        Logger.d(TAG, "DchaDialog 0002");
    }

    public DchaDialog(ParentSettingActivity parentSettingActivity, int i, String str) {
        Logger.d(TAG, "DchaDialog 0003");
        this.mActivity = parentSettingActivity;
        this.mDialogType = i;
        this.mMsg = str;
        Logger.d(TAG, "DchaDialog 0004");
    }

    @Override // android.app.DialogFragment
    public Dialog onCreateDialog(Bundle bundle) {
        Logger.d(TAG, "onCreateDialog 0001");
        Dialog dialog = new Dialog(getActivity());
        dialog.getWindow().requestFeature(1);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        int i = this.mDialogType;
        if (i == 1) {
            Logger.d(TAG, "onCreateDialog 0002");
            dialogWifiDisconnected(dialog);
        } else if (i == 2) {
            Logger.d(TAG, "onCreateDialog 0003");
            dialogSystemError(dialog);
        } else if (i == 3) {
            Logger.d(TAG, "onCreateDialog 0004");
            dialogNetworkError(dialog);
        }
        Logger.d(TAG, "onCreateDialog 0005");
        return dialog;
    }

    @Override // android.app.DialogFragment, android.app.Fragment
    public void onActivityCreated(Bundle bundle) {
        Logger.d(TAG, "onActivityCreated 0001");
        super.onActivityCreated(bundle);
        Dialog dialog = getDialog();
        WindowManager.LayoutParams attributes = dialog.getWindow().getAttributes();
        dialog.getWindow().setFlags(0, 2);
        Display defaultDisplay = dialog.getWindow().getWindowManager().getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        defaultDisplay.getRealMetrics(displayMetrics);
        attributes.width = displayMetrics.widthPixels;
        attributes.height = displayMetrics.heightPixels;
        dialog.getWindow().setAttributes(attributes);
        Logger.d(TAG, "onActivityCreated 0002");
    }

    protected void dialogWifiDisconnected(Dialog dialog) {
        Logger.d(TAG, "dialogWifiDisconnected 0001");
        dialog.setContentView(R.layout.dialog_wifi_disconnected);
        final ImageView imageView = (ImageView) dialog.findViewById(R.id.ok_btn);
        imageView.setOnClickListener(new View.OnClickListener() { // from class: jp.co.benesse.dcha.setupwizard.DchaDialog.1
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                Logger.d(DchaDialog.TAG, "onClick 0003");
                imageView.setClickable(false);
                DchaDialog.this.mActivity.moveIntroductionSettingActivity();
                DchaDialog.this.dismiss();
                DchaDialog.this.mActivity.finish();
                Logger.d(DchaDialog.TAG, "onClick 0004");
            }
        });
        Logger.d(TAG, "dialogWifiDisconnected 0002");
    }

    protected void dialogSystemError(Dialog dialog) {
        Logger.d(TAG, "dialogSystemError 0001");
        dialog.setContentView(R.layout.dialog_system_error);
        final ImageView imageView = (ImageView) dialog.findViewById(R.id.ok_btn);
        TextView textView = (TextView) dialog.findViewById(R.id.text);
        textView.setText(getString(R.string.msg_system_error_code) + this.mMsg);
        this.mActivity.setFont(textView);
        imageView.setOnClickListener(new View.OnClickListener() { // from class: jp.co.benesse.dcha.setupwizard.DchaDialog.2
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                Logger.d(DchaDialog.TAG, "onClick 0011");
                imageView.setClickable(false);
                DchaDialog.this.mActivity.moveIntroductionSettingActivity();
                DchaDialog.this.dismiss();
                DchaDialog.this.mActivity.finish();
                Logger.d(DchaDialog.TAG, "onClick 0012");
            }
        });
        Logger.d(TAG, "dialogSystemError 0002");
    }

    protected void dialogNetworkError(Dialog dialog) {
        Logger.d(TAG, "dialogNetworkError 0001");
        dialog.setContentView(getNetworkErrorLayoutId());
        final ImageView imageView = (ImageView) dialog.findViewById(R.id.ok_btn);
        imageView.setOnClickListener(new View.OnClickListener() { // from class: jp.co.benesse.dcha.setupwizard.DchaDialog.3
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                Logger.d(DchaDialog.TAG, "onClick 0013");
                imageView.setClickable(false);
                DchaDialog.this.mActivity.moveIntroductionSettingActivity();
                DchaDialog.this.dismiss();
                DchaDialog.this.mActivity.finish();
                Logger.d(DchaDialog.TAG, "onClick 0014");
            }
        });
        Logger.d(TAG, "dialogNetworkError 0002");
    }

    protected int getNetworkErrorLayoutId() {
        Logger.d(TAG, "getNetworkErrorLayoutId 0001");
        if (DchaNetworkUtil.isConnective(this.mActivity)) {
            Logger.d(TAG, "getNetworkErrorLayoutId 0002");
            return R.layout.dialog_server_busy_error;
        }
        Logger.d(TAG, "getNetworkErrorLayoutId 0003");
        return R.layout.dialog_network_error;
    }
}
