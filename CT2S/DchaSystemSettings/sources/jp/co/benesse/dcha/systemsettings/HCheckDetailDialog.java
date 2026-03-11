package jp.co.benesse.dcha.systemsettings;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import jp.co.benesse.dcha.util.Logger;

public class HCheckDetailDialog extends DialogFragment implements View.OnClickListener {
    private DialogInterface.OnDismissListener dismissListener = null;
    private HealthCheckDto healthCheckDto;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
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
                ImageView dSpeedImageView = (ImageView) dialog.findViewById(R.id.hCheckResultDSpeedImg);
                dSpeedImageView.setImageResource(this.healthCheckDto.myDSpeedImage);
                dSpeedImageView.setVisibility(0);
                TextView dSpeedTextView = (TextView) dialog.findViewById(R.id.hCheckResultDSpeedText);
                dSpeedTextView.setText(this.healthCheckDto.myDownloadSpeed);
                dSpeedTextView.setVisibility(0);
            }
        } catch (RuntimeException e) {
            Logger.d("HCheckDetailDialog", "onCreateDialog 0004", e);
            dialog.dismiss();
        }
        Logger.d("HCheckDetailDialog", "onCreateDialog 0005");
        return dialog;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Logger.d("HCheckDetailDialog", "onActivityCreated 0001");
        super.onActivityCreated(savedInstanceState);
        Dialog dialog = getDialog();
        WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int dialogWidth = metrics.widthPixels;
        int dialogHeight = metrics.heightPixels + 23;
        lp.width = dialogWidth;
        lp.height = dialogHeight;
        dialog.getWindow().setFlags(0, 2);
        dialog.getWindow().setAttributes(lp);
        Logger.d("HCheckDetailDialog", "onActivityCreated 0002");
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        Logger.d("HCheckDetailDialog", "onDismiss 0001");
        super.onDismiss(dialog);
        try {
            if (this.dismissListener != null) {
                this.dismissListener.onDismiss(dialog);
            }
            getDialog().findViewById(R.id.close).setOnClickListener(null);
        } catch (RuntimeException e) {
            Logger.d("HCheckDetailDialog", "onDismiss 0002", e);
        }
        Logger.d("HCheckDetailDialog", "onDismiss 0003");
    }

    public void setOnDismissListener(DialogInterface.OnDismissListener listener) {
        Logger.d("HCheckDetailDialog", "setOnDismissListener 0001");
        this.dismissListener = listener;
        Logger.d("HCheckDetailDialog", "setOnDismissListener 0002");
    }

    @Override
    public void onClick(View v) {
        Logger.d("HCheckDetailDialog", "onClick 0001");
        int id = v.getId();
        switch (id) {
            case R.id.close:
                Logger.d("HCheckDetailDialog", "onClick 0002");
                close(v);
                break;
        }
        Logger.d("HCheckDetailDialog", "onClick 0003");
    }

    private void close(View v) {
        Logger.d("HCheckDetailDialog", "close 0001");
        v.setClickable(false);
        dismiss();
        Logger.d("HCheckDetailDialog", "close 0002");
    }

    protected void drawingResultView(Dialog dialog, int healthCheckResult, int hCheckTView, String hCResultText) {
        Logger.d("HCheckDetailDialog", "drawingResultView 0001");
        TextView resultTextView = (TextView) dialog.findViewById(hCheckTView);
        if (healthCheckResult == R.string.health_check_pending) {
            Logger.d("HCheckDetailDialog", "drawingResultView 0002");
            resultTextView.setText(getString(R.string.health_check_pending));
            resultTextView.setTextColor(getResources().getColor(R.color.text_enable));
        } else if (healthCheckResult == R.string.health_check_ok) {
            if (TextUtils.isEmpty(hCResultText)) {
                Logger.d("HCheckDetailDialog", "drawingResultView 0003");
                resultTextView.setText(getString(R.string.health_check_pending));
                resultTextView.setTextColor(getResources().getColor(R.color.text_enable));
            } else {
                Logger.d("HCheckDetailDialog", "drawingResultView 0004");
                resultTextView.setText(hCResultText);
                resultTextView.setTextColor(getResources().getColor(R.color.text_black));
            }
        } else if (TextUtils.isEmpty(hCResultText)) {
            Logger.d("HCheckDetailDialog", "drawingResultView 0005");
            resultTextView.setText(getString(R.string.health_check_pending));
            resultTextView.setTextColor(getResources().getColor(R.color.text_enable));
        } else {
            Logger.d("HCheckDetailDialog", "drawingResultView 0006");
            resultTextView.setText(hCResultText);
            resultTextView.setTextColor(getResources().getColor(R.color.text_red_hc));
        }
        Logger.d("HCheckDetailDialog", "drawingResultView 0007");
    }
}
