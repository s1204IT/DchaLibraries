package com.mediatek.settings.cdma;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;
import com.android.settings.R;
import com.mediatek.settings.sim.SimHotSwapHandler;

/* loaded from: classes.dex */
public class CdmaSimDialogActivity extends Activity {
    private Dialog mDialog;
    private IntentFilter mIntentFilter;
    private SimHotSwapHandler mSimHotSwapHandler;
    private int mTargetSubId = -1;
    private int mActionType = -1;
    private int mDialogType = -1;
    private BroadcastReceiver mSubReceiver = new BroadcastReceiver() { // from class: com.mediatek.settings.cdma.CdmaSimDialogActivity.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            Log.d("CdmaSimDialogActivity", "mSubReceiver action = " + intent.getAction());
            CdmaSimDialogActivity.this.finish();
        }
    };

    private void init() {
        this.mSimHotSwapHandler = new SimHotSwapHandler(getApplicationContext());
        this.mSimHotSwapHandler.registerOnSimHotSwap(new SimHotSwapHandler.OnSimHotSwapListener() { // from class: com.mediatek.settings.cdma.CdmaSimDialogActivity.2
            @Override // com.mediatek.settings.sim.SimHotSwapHandler.OnSimHotSwapListener
            public void onSimHotSwap() {
                Log.d("CdmaSimDialogActivity", "onSimHotSwap, finish Activity~~");
                CdmaSimDialogActivity.this.finish();
            }
        });
        this.mIntentFilter = new IntentFilter("android.intent.action.AIRPLANE_MODE");
        registerReceiver(this.mSubReceiver, this.mIntentFilter);
    }

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) throws Resources.NotFoundException {
        Log.d("CdmaSimDialogActivity", "onCreate");
        super.onCreate(bundle);
        Bundle extras = getIntent().getExtras();
        init();
        if (extras != null) {
            int i = extras.getInt("dialog_type", -1);
            this.mTargetSubId = extras.getInt("target_subid", -1);
            this.mActionType = extras.getInt("action_type", -1);
            this.mDialogType = i;
            Log.d("CdmaSimDialogActivity", "dialogType=" + i + ", targetSubId=" + this.mTargetSubId + ", actionType=" + this.mActionType);
            switch (i) {
                case 0:
                    displayDualCdmaDialog();
                    return;
                case 1:
                    displayAlertCdmaDialog();
                    return;
                default:
                    throw new IllegalArgumentException("Invalid dialog type " + i + " sent.");
            }
        }
        Log.e("CdmaSimDialogActivity", "unexpect happend");
        finish();
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        this.mSimHotSwapHandler.unregisterOnSimHotSwap();
        unregisterReceiver(this.mSubReceiver);
        if (this.mDialog != null && this.mDialog.isShowing()) {
            this.mDialog.dismiss();
            this.mDialog = null;
        }
        super.onDestroy();
    }

    private void displayDualCdmaDialog() {
        Log.d("CdmaSimDialogActivity", "displayDualCdmaDialog...");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.two_cdma_dialog_msg);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() { // from class: com.mediatek.settings.cdma.CdmaSimDialogActivity.3
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i) {
                if (dialogInterface != null) {
                    dialogInterface.dismiss();
                }
                CdmaSimDialogActivity.this.finish();
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() { // from class: com.mediatek.settings.cdma.CdmaSimDialogActivity.4
            @Override // android.content.DialogInterface.OnCancelListener
            public void onCancel(DialogInterface dialogInterface) {
                if (dialogInterface != null) {
                    dialogInterface.dismiss();
                }
                CdmaSimDialogActivity.this.finish();
            }
        });
        builder.setOnKeyListener(new DialogInterface.OnKeyListener() { // from class: com.mediatek.settings.cdma.CdmaSimDialogActivity.5
            @Override // android.content.DialogInterface.OnKeyListener
            public boolean onKey(DialogInterface dialogInterface, int i, KeyEvent keyEvent) {
                if (i == 4) {
                    CdmaSimDialogActivity.this.finish();
                    return true;
                }
                return false;
            }
        });
        this.mDialog = builder.create();
        this.mDialog.show();
    }

    private void displayAlertCdmaDialog() throws Resources.NotFoundException {
        Log.d("CdmaSimDialogActivity", "displayAlertCdmaDialog...");
        SubscriptionInfo activeSubscriptionInfo = null;
        for (int i : SubscriptionManager.from(this).getActiveSubscriptionIdList()) {
            if (i != this.mTargetSubId) {
                activeSubscriptionInfo = SubscriptionManager.from(this).getActiveSubscriptionInfo(i);
            }
        }
        if (activeSubscriptionInfo != null) {
            String string = getResources().getString(R.string.default_data_switch_msg, activeSubscriptionInfo.getDisplayName());
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(string);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() { // from class: com.mediatek.settings.cdma.CdmaSimDialogActivity.6
                @Override // android.content.DialogInterface.OnClickListener
                public void onClick(DialogInterface dialogInterface, int i2) {
                    if (dialogInterface != null) {
                        Log.d("CdmaSimDialogActivity", "displayAlertCdmaDialog, set data sub to " + CdmaSimDialogActivity.this.mTargetSubId);
                        CdmaSimDialogActivity.this.setDefaultDataSubId(CdmaSimDialogActivity.this, CdmaSimDialogActivity.this.mTargetSubId);
                        dialogInterface.dismiss();
                    }
                    CdmaSimDialogActivity.this.finish();
                }
            });
            builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() { // from class: com.mediatek.settings.cdma.CdmaSimDialogActivity.7
                @Override // android.content.DialogInterface.OnClickListener
                public void onClick(DialogInterface dialogInterface, int i2) {
                    if (dialogInterface != null) {
                        dialogInterface.dismiss();
                    }
                    CdmaSimDialogActivity.this.finish();
                }
            });
            builder.setOnKeyListener(new DialogInterface.OnKeyListener() { // from class: com.mediatek.settings.cdma.CdmaSimDialogActivity.8
                @Override // android.content.DialogInterface.OnKeyListener
                public boolean onKey(DialogInterface dialogInterface, int i2, KeyEvent keyEvent) {
                    if (i2 == 4) {
                        CdmaSimDialogActivity.this.finish();
                        return true;
                    }
                    return false;
                }
            });
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() { // from class: com.mediatek.settings.cdma.CdmaSimDialogActivity.9
                @Override // android.content.DialogInterface.OnCancelListener
                public void onCancel(DialogInterface dialogInterface) {
                    CdmaSimDialogActivity.this.finish();
                }
            });
            this.mDialog = builder.create();
            this.mDialog.show();
            return;
        }
        Log.d("CdmaSimDialogActivity", "no need to show the alert dialog");
    }

    private void setDefaultDataSubId(Context context, int i) {
        SubscriptionManager.from(context).setDefaultDataSubId(i);
        if (this.mActionType == 0) {
            Toast.makeText(context, R.string.data_switch_started, 1).show();
        }
    }
}
