package jp.co.benesse.dcha.setupwizard;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.DialogFragment;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import jp.co.benesse.dcha.util.Logger;

@SuppressLint({"ValidFragment"})
public class DchaDialog extends DialogFragment {
    public static final int DIALOG_TYPE_NETWORK_ERROR = 3;
    public static final int DIALOG_TYPE_SYSTEM_ERROR = 2;
    public static final int DIALOG_TYPE_WIFI_DISCONNECT = 1;
    private static final int HEIGHT_NAVIGATION_BAR = 25;
    private static final String TAG = DchaDialog.class.getSimpleName();
    protected ParentSettingActivity mActivity;
    protected int mDialogType;
    protected String mMsg;

    public DchaDialog(ParentSettingActivity activity, int dialogType) {
        Logger.d(TAG, "DchaDialog 0001");
        this.mActivity = activity;
        this.mDialogType = dialogType;
        Logger.d(TAG, "DchaDialog 0002");
    }

    public DchaDialog(ParentSettingActivity activity, int dialogType, String msg) {
        Logger.d(TAG, "DchaDialog 0003");
        this.mActivity = activity;
        this.mDialogType = dialogType;
        this.mMsg = msg;
        Logger.d(TAG, "DchaDialog 0004");
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Logger.d(TAG, "onCreateDialog 0001");
        Dialog dialog = new Dialog(getActivity());
        dialog.getWindow().requestFeature(1);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        switch (this.mDialogType) {
            case 1:
                Logger.d(TAG, "onCreateDialog 0002");
                dialogWifiDisconnected(dialog);
                break;
            case 2:
                Logger.d(TAG, "onCreateDialog 0003");
                dialogSystemError(dialog);
                break;
            case 3:
                Logger.d(TAG, "onCreateDialog 0004");
                dialogNetworkError(dialog);
                break;
        }
        Logger.d(TAG, "onCreateDialog 0005");
        return dialog;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Logger.d(TAG, "onActivityCreated 0001");
        super.onActivityCreated(savedInstanceState);
        Dialog dialog = getDialog();
        WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
        dialog.getWindow().setFlags(0, 2);
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        lp.width = metrics.widthPixels;
        lp.height = metrics.heightPixels + HEIGHT_NAVIGATION_BAR;
        dialog.getWindow().setAttributes(lp);
        Logger.d(TAG, "onActivityCreated 0002");
    }

    protected void dialogWifiDisconnected(Dialog dialog) {
        Logger.d(TAG, "dialogWifiDisconnected 0001");
        dialog.setContentView(R.layout.dialog_wifi_disconnected);
        final ImageView okBtn = (ImageView) dialog.findViewById(R.id.ok_btn);
        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logger.d(DchaDialog.TAG, "onClick 0003");
                okBtn.setClickable(false);
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
        final ImageView okBtn = (ImageView) dialog.findViewById(R.id.ok_btn);
        TextView text = (TextView) dialog.findViewById(R.id.text);
        text.setText(String.valueOf(getString(R.string.msg_system_error_code)) + this.mMsg);
        this.mActivity.setFont(text);
        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logger.d(DchaDialog.TAG, "onClick 0011");
                okBtn.setClickable(false);
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
        int layoutResID = getNetworkErrorLayoutId();
        dialog.setContentView(layoutResID);
        final ImageView okBtn = (ImageView) dialog.findViewById(R.id.ok_btn);
        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logger.d(DchaDialog.TAG, "onClick 0013");
                okBtn.setClickable(false);
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
        boolean connective = DchaNetworkUtil.isConnective(this.mActivity);
        if (connective) {
            Logger.d(TAG, "getNetworkErrorLayoutId 0002");
            return R.layout.dialog_server_busy_error;
        }
        Logger.d(TAG, "getNetworkErrorLayoutId 0003");
        return R.layout.dialog_network_error;
    }
}
