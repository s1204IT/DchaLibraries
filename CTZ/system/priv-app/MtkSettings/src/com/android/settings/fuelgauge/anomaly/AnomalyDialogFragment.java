package com.android.settings.fuelgauge.anomaly;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

/* loaded from: classes.dex */
public class AnomalyDialogFragment extends InstrumentedDialogFragment implements DialogInterface.OnClickListener {
    Anomaly mAnomaly;
    AnomalyUtils mAnomalyUtils;

    public interface AnomalyDialogListener {
        void onAnomalyHandled(Anomaly anomaly);
    }

    public static AnomalyDialogFragment newInstance(Anomaly anomaly, int i) {
        AnomalyDialogFragment anomalyDialogFragment = new AnomalyDialogFragment();
        Bundle bundle = new Bundle(2);
        bundle.putParcelable("anomaly", anomaly);
        bundle.putInt("metrics_key", i);
        anomalyDialogFragment.setArguments(bundle);
        return anomalyDialogFragment;
    }

    @Override // com.android.settingslib.core.lifecycle.ObservableDialogFragment, android.app.DialogFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        initAnomalyUtils();
    }

    void initAnomalyUtils() {
        this.mAnomalyUtils = AnomalyUtils.getInstance(getContext());
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 988;
    }

    @Override // android.content.DialogInterface.OnClickListener
    public void onClick(DialogInterface dialogInterface, int i) {
        AnomalyDialogListener anomalyDialogListener = (AnomalyDialogListener) getTargetFragment();
        if (anomalyDialogListener == null) {
            return;
        }
        this.mAnomalyUtils.getAnomalyAction(this.mAnomaly).handlePositiveAction(this.mAnomaly, getArguments().getInt("metrics_key"));
        anomalyDialogListener.onAnomalyHandled(this.mAnomaly);
    }

    @Override // android.app.DialogFragment
    public Dialog onCreateDialog(Bundle bundle) {
        int i;
        Bundle arguments = getArguments();
        Context context = getContext();
        AnomalyUtils anomalyUtils = AnomalyUtils.getInstance(context);
        this.mAnomaly = (Anomaly) arguments.getParcelable("anomaly");
        anomalyUtils.logAnomaly(this.mMetricsFeatureProvider, this.mAnomaly, 988);
        int actionType = this.mAnomalyUtils.getAnomalyAction(this.mAnomaly).getActionType();
        if (actionType == 0) {
            AlertDialog.Builder title = new AlertDialog.Builder(context).setTitle(R.string.dialog_stop_title);
            if (this.mAnomaly.type == 0) {
                i = R.string.dialog_stop_message;
            } else {
                i = R.string.dialog_stop_message_wakeup_alarm;
            }
            return title.setMessage(getString(i, new Object[]{this.mAnomaly.displayName})).setPositiveButton(R.string.dialog_stop_ok, this).setNegativeButton(R.string.dlg_cancel, (DialogInterface.OnClickListener) null).create();
        }
        switch (actionType) {
            case 2:
                return new AlertDialog.Builder(context).setTitle(R.string.dialog_location_title).setMessage(getString(R.string.dialog_location_message, new Object[]{this.mAnomaly.displayName})).setPositiveButton(R.string.dialog_location_ok, this).setNegativeButton(R.string.dlg_cancel, (DialogInterface.OnClickListener) null).create();
            case 3:
                return new AlertDialog.Builder(context).setTitle(R.string.dialog_background_check_title).setMessage(getString(R.string.dialog_background_check_message, new Object[]{this.mAnomaly.displayName})).setPositiveButton(R.string.dialog_background_check_ok, this).setNegativeButton(R.string.dlg_cancel, (DialogInterface.OnClickListener) null).create();
            default:
                throw new IllegalArgumentException("unknown type " + this.mAnomaly.type);
        }
    }
}
