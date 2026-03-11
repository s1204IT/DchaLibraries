package jp.co.benesse.dcha.systemsettings;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import jp.co.benesse.dcha.util.Logger;

public class HCheckDetailDialog extends DialogFragment implements View.OnClickListener {
    private DialogInterface.OnDismissListener dismissListener = null;
    private HealthCheckDto healthCheckDto;

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        Logger.d("HCheckDetailDialog", "onCreateDialog 0001");
        Dialog dialog = new Dialog(getActivity());
        try {
            dialog.getWindow().requestFeature(1);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            dialog.setContentView(R.layout.dialog_health_check_detail);
            this.healthCheckDto = (HealthCheckDto) getArguments().getSerializable("healthCheckDto");
            dialog.findViewById(R.id.close).setOnClickListener(this);
            dialog.findViewById(R.id.close).setClickable(true);
            drawingResultView(dialog, R.string.health_check_ok, R.id.hCheckRMacAddress, this.healthCheckDto.myMacaddress);
            drawingResultView(dialog, this.healthCheckDto.isCheckedSsid, R.id.hCheckResultSsid, this.healthCheckDto.mySsid);
            drawingResultView(dialog, this.healthCheckDto.isCheckedWifi, R.id.hCheckResultWifi, getString(this.healthCheckDto.isCheckedWifi));
            drawingResultView(dialog, this.healthCheckDto.isCheckedIpAddress, R.id.hCheckResultIpAddress, this.healthCheckDto.myIpAddress);
            drawingResultView(dialog, this.healthCheckDto.isCheckedIpAddress, R.id.hCheckResultSubnetMask, this.healthCheckDto.mySubnetMask);
            drawingResultView(dialog, this.healthCheckDto.isCheckedIpAddress, R.id.hCheckResultDefaultGateway, this.healthCheckDto.myDefaultGateway);
            drawingResultView(dialog, this.healthCheckDto.isCheckedIpAddress, R.id.hCheckResultDns1, this.healthCheckDto.myDns1);
            drawingResultView(dialog, this.healthCheckDto.isCheckedIpAddress, R.id.hCheckResultDns2, this.healthCheckDto.myDns2);
            drawingResultView(dialog, this.healthCheckDto.isCheckedNetConnection, R.id.hCheckResultNetConnection, getString(this.healthCheckDto.isCheckedNetConnection));
            if (this.healthCheckDto.isCheckedDSpeed == R.string.health_check_pending) {
                Logger.d("HCheckDetailDialog", "onCreateDialog 0002");
                dialog.findViewById(R.id.hCheckDSpeedPending).setVisibility(0);
            } else {
                Logger.d("HCheckDetailDialog", "onCreateDialog 0003");
                ImageView imageView = (ImageView) dialog.findViewById(R.id.hCheckResultDSpeedImg);
                imageView.setImageResource(this.healthCheckDto.myDSpeedImage);
                imageView.setVisibility(0);
                TextView textView = (TextView) dialog.findViewById(R.id.hCheckResultDSpeedText);
                textView.setText(this.healthCheckDto.myDownloadSpeed);
                textView.setVisibility(0);
            }
        } catch (RuntimeException e) {
            Logger.d("HCheckDetailDialog", "onCreateDialog 0004", e);
            dialog.dismiss();
        }
        Logger.d("HCheckDetailDialog", "onCreateDialog 0005");
        return dialog;
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        Logger.d("HCheckDetailDialog", "onActivityCreated 0001");
        super.onActivityCreated(bundle);
        Dialog dialog = getDialog();
        Window window = dialog.getWindow();
        WindowManager.LayoutParams attributes = window.getAttributes();
        Display defaultDisplay = window.getWindowManager().getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        defaultDisplay.getRealMetrics(displayMetrics);
        attributes.width = displayMetrics.widthPixels;
        attributes.height = displayMetrics.heightPixels;
        dialog.getWindow().setFlags(0, 2);
        dialog.getWindow().setAttributes(attributes);
        Logger.d("HCheckDetailDialog", "onActivityCreated 0002");
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        Logger.d("HCheckDetailDialog", "onDismiss 0001");
        super.onDismiss(dialogInterface);
        try {
            if (this.dismissListener != null) {
                this.dismissListener.onDismiss(dialogInterface);
            }
        } catch (RuntimeException e) {
            Logger.d("HCheckDetailDialog", "onDismiss 0002", e);
        }
        Logger.d("HCheckDetailDialog", "onDismiss 0003");
    }

    public void setOnDismissListener(DialogInterface.OnDismissListener onDismissListener) {
        Logger.d("HCheckDetailDialog", "setOnDismissListener 0001");
        this.dismissListener = onDismissListener;
        Logger.d("HCheckDetailDialog", "setOnDismissListener 0002");
    }

    @Override
    public void onClick(View view) {
        Logger.d("HCheckDetailDialog", "onClick 0001");
        if (view.getId() == R.id.close) {
            Logger.d("HCheckDetailDialog", "onClick 0002");
            close(view);
        }
        Logger.d("HCheckDetailDialog", "onClick 0003");
    }

    private void close(View view) {
        Logger.d("HCheckDetailDialog", "close 0001");
        view.setClickable(false);
        dismiss();
        Logger.d("HCheckDetailDialog", "close 0002");
    }

    protected void drawingResultView(Dialog dialog, int i, int i2, String str) {
        Logger.d("HCheckDetailDialog", "drawingResultView 0001");
        TextView textView = (TextView) dialog.findViewById(i2);
        if (i == R.string.health_check_pending) {
            Logger.d("HCheckDetailDialog", "drawingResultView 0002");
            textView.setText(getString(R.string.health_check_pending));
            textView.setTextColor(getResources().getColor(R.color.text_enable));
        } else if (i == R.string.health_check_ok) {
            if (TextUtils.isEmpty(str)) {
                Logger.d("HCheckDetailDialog", "drawingResultView 0003");
                textView.setText(getString(R.string.health_check_pending));
                textView.setTextColor(getResources().getColor(R.color.text_enable));
            } else {
                Logger.d("HCheckDetailDialog", "drawingResultView 0004");
                textView.setText(str);
                textView.setTextColor(getResources().getColor(R.color.text_black));
            }
        } else if (TextUtils.isEmpty(str)) {
            Logger.d("HCheckDetailDialog", "drawingResultView 0005");
            textView.setText(getString(R.string.health_check_pending));
            textView.setTextColor(getResources().getColor(R.color.text_enable));
        } else {
            Logger.d("HCheckDetailDialog", "drawingResultView 0006");
            textView.setText(str);
            textView.setTextColor(getResources().getColor(R.color.text_red_hc));
        }
        Logger.d("HCheckDetailDialog", "drawingResultView 0007");
    }
}
