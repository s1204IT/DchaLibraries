package com.android.settings;

import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.RemoteException;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.CheckedTextView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.logging.MetricsLogger;

public class BugreportPreference extends CustomDialogPreference {
    private TextView mFullSummary;
    private CheckedTextView mFullTitle;
    private TextView mInteractiveSummary;
    private CheckedTextView mInteractiveTitle;

    public BugreportPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder, DialogInterface.OnClickListener listener) {
        super.onPrepareDialogBuilder(builder, listener);
        View dialogView = View.inflate(getContext(), R.layout.bugreport_options_dialog, null);
        this.mInteractiveTitle = (CheckedTextView) dialogView.findViewById(R.id.bugreport_option_interactive_title);
        this.mInteractiveSummary = (TextView) dialogView.findViewById(R.id.bugreport_option_interactive_summary);
        this.mFullTitle = (CheckedTextView) dialogView.findViewById(R.id.bugreport_option_full_title);
        this.mFullSummary = (TextView) dialogView.findViewById(R.id.bugreport_option_full_summary);
        View.OnClickListener l = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v == BugreportPreference.this.mFullTitle || v == BugreportPreference.this.mFullSummary) {
                    BugreportPreference.this.mInteractiveTitle.setChecked(false);
                    BugreportPreference.this.mFullTitle.setChecked(true);
                }
                if (v != BugreportPreference.this.mInteractiveTitle && v != BugreportPreference.this.mInteractiveSummary) {
                    return;
                }
                BugreportPreference.this.mInteractiveTitle.setChecked(true);
                BugreportPreference.this.mFullTitle.setChecked(false);
            }
        };
        this.mInteractiveTitle.setOnClickListener(l);
        this.mFullTitle.setOnClickListener(l);
        this.mInteractiveSummary.setOnClickListener(l);
        this.mFullSummary.setOnClickListener(l);
        builder.setPositiveButton(android.R.string.edit_accessibility_shortcut_menu_button, listener);
        builder.setView(dialogView);
    }

    @Override
    protected void onClick(DialogInterface dialog, int which) {
        if (which != -1) {
            return;
        }
        Context context = getContext();
        if (this.mFullTitle.isChecked()) {
            Log.v("BugreportPreference", "Taking full bugreport right away");
            MetricsLogger.action(context, 295);
            takeBugreport(0);
        } else {
            Log.v("BugreportPreference", "Taking interactive bugreport in 3s");
            MetricsLogger.action(context, 294);
            String msg = context.getResources().getQuantityString(android.R.menu.webview_find, 3, 3);
            Log.v("BugreportPreference", msg);
            Toast.makeText(context, msg, 0).show();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    BugreportPreference.this.takeBugreport(1);
                }
            }, 3000L);
        }
    }

    public void takeBugreport(int bugreportType) {
        try {
            ActivityManagerNative.getDefault().requestBugReport(bugreportType);
        } catch (RemoteException e) {
            Log.e("BugreportPreference", "error taking bugreport (bugreportType=" + bugreportType + ")", e);
        }
    }
}
