package com.android.settings;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.RemoteException;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.CheckedTextView;
import android.widget.TextView;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.CustomDialogPreference;

/* loaded from: classes.dex */
public class BugreportPreference extends CustomDialogPreference {
    private TextView mFullSummary;
    private CheckedTextView mFullTitle;
    private TextView mInteractiveSummary;
    private CheckedTextView mInteractiveTitle;

    public BugreportPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override // com.android.settingslib.CustomDialogPreference
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder, DialogInterface.OnClickListener onClickListener) {
        super.onPrepareDialogBuilder(builder, onClickListener);
        View viewInflate = View.inflate(getContext(), R.layout.bugreport_options_dialog, null);
        this.mInteractiveTitle = (CheckedTextView) viewInflate.findViewById(R.id.bugreport_option_interactive_title);
        this.mInteractiveSummary = (TextView) viewInflate.findViewById(R.id.bugreport_option_interactive_summary);
        this.mFullTitle = (CheckedTextView) viewInflate.findViewById(R.id.bugreport_option_full_title);
        this.mFullSummary = (TextView) viewInflate.findViewById(R.id.bugreport_option_full_summary);
        View.OnClickListener onClickListener2 = new View.OnClickListener() { // from class: com.android.settings.BugreportPreference.1
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                if (view == BugreportPreference.this.mFullTitle || view == BugreportPreference.this.mFullSummary) {
                    BugreportPreference.this.mInteractiveTitle.setChecked(false);
                    BugreportPreference.this.mFullTitle.setChecked(true);
                }
                if (view == BugreportPreference.this.mInteractiveTitle || view == BugreportPreference.this.mInteractiveSummary) {
                    BugreportPreference.this.mInteractiveTitle.setChecked(true);
                    BugreportPreference.this.mFullTitle.setChecked(false);
                }
            }
        };
        this.mInteractiveTitle.setOnClickListener(onClickListener2);
        this.mFullTitle.setOnClickListener(onClickListener2);
        this.mInteractiveSummary.setOnClickListener(onClickListener2);
        this.mFullSummary.setOnClickListener(onClickListener2);
        builder.setPositiveButton(android.R.string.lockscreen_transport_rew_description, onClickListener);
        builder.setView(viewInflate);
    }

    @Override // com.android.settingslib.CustomDialogPreference
    protected void onClick(DialogInterface dialogInterface, int i) {
        if (i == -1) {
            Context context = getContext();
            if (this.mFullTitle.isChecked()) {
                Log.v("BugreportPreference", "Taking full bugreport right away");
                FeatureFactory.getFactory(context).getMetricsFeatureProvider().action(context, 295, new Pair[0]);
                takeBugreport(0);
            } else {
                Log.v("BugreportPreference", "Taking interactive bugreport right away");
                FeatureFactory.getFactory(context).getMetricsFeatureProvider().action(context, 294, new Pair[0]);
                takeBugreport(1);
            }
        }
    }

    private void takeBugreport(int i) {
        try {
            ActivityManager.getService().requestBugReport(i);
        } catch (RemoteException e) {
            Log.e("BugreportPreference", "error taking bugreport (bugreportType=" + i + ")", e);
        }
    }
}
