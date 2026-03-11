package com.mediatek.settings.cdma;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.telecom.PhoneAccountHandle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;
import com.android.settings.R;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import com.mediatek.settings.sim.SimHotSwapHandler;

public class CdmaSimDialogActivity extends Activity {
    private Dialog mDialog;
    private IntentFilter mIntentFilter;
    private SimHotSwapHandler mSimHotSwapHandler;
    private StatusBarManager mStatusBarManager;
    private int mTargetSubId = -1;
    private int mActionType = -1;
    private PhoneAccountHandle mHandle = null;
    private int mDialogType = -1;
    private BroadcastReceiver mSubReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("CdmaSimDialogActivity", "mSubReceiver action = " + action);
            CdmaSimDialogActivity.this.finish();
        }
    };

    private void init() {
        this.mSimHotSwapHandler = new SimHotSwapHandler(getApplicationContext());
        this.mSimHotSwapHandler.registerOnSimHotSwap(new SimHotSwapHandler.OnSimHotSwapListener() {
            @Override
            public void onSimHotSwap() {
                Log.d("CdmaSimDialogActivity", "onSimHotSwap, finish Activity~~");
                CdmaSimDialogActivity.this.finish();
            }
        });
        this.mIntentFilter = new IntentFilter("android.intent.action.AIRPLANE_MODE");
        registerReceiver(this.mSubReceiver, this.mIntentFilter);
        this.mStatusBarManager = (StatusBarManager) getSystemService("statusbar");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("CdmaSimDialogActivity", "onCreate");
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        init();
        if (extras != null) {
            int dialogType = extras.getInt("dialog_type", -1);
            this.mTargetSubId = extras.getInt("target_subid", -1);
            this.mActionType = extras.getInt("action_type", -1);
            this.mDialogType = dialogType;
            Log.d("CdmaSimDialogActivity", "dialogType: " + dialogType + " targetSubId: " + this.mTargetSubId + " actionType: " + this.mActionType);
            switch (dialogType) {
                case DefaultWfcSettingsExt.RESUME:
                    displayDualCdmaDialog();
                    return;
                case DefaultWfcSettingsExt.PAUSE:
                    displayAlertCdmaDialog();
                    return;
                case DefaultWfcSettingsExt.CREATE:
                    displayOmhWarningDialog();
                    return;
                case DefaultWfcSettingsExt.DESTROY:
                    displayOmhDataPickDialog();
                    return;
                default:
                    throw new IllegalArgumentException("Invalid dialog type " + dialogType + " sent.");
            }
        }
        Log.e("CdmaSimDialogActivity", "unexpect happend");
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.mSimHotSwapHandler.unregisterOnSimHotSwap();
        unregisterReceiver(this.mSubReceiver);
        if (this.mDialog == null || !this.mDialog.isShowing()) {
            return;
        }
        this.mDialog.dismiss();
        this.mDialog = null;
    }

    private void displayDualCdmaDialog() {
        Log.d("CdmaSimDialogActivity", "displayDualCdmaDialog...");
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage(R.string.two_cdma_dialog_msg);
        alertDialogBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (dialog != null) {
                    dialog.dismiss();
                }
                CdmaSimDialogActivity.this.finish();
            }
        });
        alertDialogBuilder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (dialog != null) {
                    dialog.dismiss();
                }
                CdmaSimDialogActivity.this.finish();
            }
        });
        alertDialogBuilder.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == 4) {
                    CdmaSimDialogActivity.this.finish();
                    return true;
                }
                return false;
            }
        });
        this.mDialog = alertDialogBuilder.create();
        this.mDialog.show();
    }

    private void displayAlertCdmaDialog() {
        Log.d("CdmaSimDialogActivity", "displayAlertCdmaDialog...");
        SubscriptionInfo defaultSir = null;
        int[] list = SubscriptionManager.from(this).getActiveSubscriptionIdList();
        for (int i : list) {
            if (i != this.mTargetSubId) {
                defaultSir = SubscriptionManager.from(this).getActiveSubscriptionInfo(i);
            }
        }
        if (defaultSir != null) {
            String switchDataAlertMessage = getResources().getString(R.string.default_data_switch_msg, defaultSir.getDisplayName());
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setMessage(switchDataAlertMessage);
            alertDialogBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (dialog != null) {
                        Log.d("CdmaSimDialogActivity", "displayAlertCdmaDialog, set data sub to " + CdmaSimDialogActivity.this.mTargetSubId);
                        CdmaSimDialogActivity.this.setDefaultDataSubId(CdmaSimDialogActivity.this, CdmaSimDialogActivity.this.mTargetSubId);
                        dialog.dismiss();
                    }
                    CdmaSimDialogActivity.this.finish();
                }
            });
            alertDialogBuilder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                    CdmaSimDialogActivity.this.finish();
                }
            });
            alertDialogBuilder.setOnKeyListener(new DialogInterface.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    if (keyCode == 4) {
                        CdmaSimDialogActivity.this.finish();
                        return true;
                    }
                    return false;
                }
            });
            alertDialogBuilder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    CdmaSimDialogActivity.this.finish();
                }
            });
            this.mDialog = alertDialogBuilder.create();
            this.mDialog.show();
            return;
        }
        Log.d("CdmaSimDialogActivity", "no need to show the alert dialog");
    }

    private void displayOmhWarningDialog() {
        Log.d("CdmaSimDialogActivity", "displayOmhWarningDialog...");
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage(R.string.omh_warning_dialog_msg);
        alertDialogBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (dialog != null) {
                    dialog.dismiss();
                }
                CdmaSimDialogActivity.this.finish();
            }
        });
        alertDialogBuilder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                Log.d("CdmaSimDialogActivity", "OMH warning dialog dismissed...");
                OmhEventHandler.getInstance(CdmaSimDialogActivity.this).sendEmptyMessage(103);
                CdmaSimDialogActivity.this.enableStatusBarNavigation(true);
            }
        });
        alertDialogBuilder.setCancelable(false);
        this.mDialog = alertDialogBuilder.create();
        this.mDialog.show();
        enableStatusBarNavigation(false);
    }

    @Override
    protected void onResume() {
        if (this.mDialogType == 2) {
            enableStatusBarNavigation(false);
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (this.mDialogType == 2) {
            OmhEventHandler.getInstance(this).sendEmptyMessage(103);
            enableStatusBarNavigation(true);
        }
        super.onPause();
    }

    private void displayOmhDataPickDialog() {
        Log.d("CdmaSimDialogActivity", "displayOmhDataPickDialog...");
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage(R.string.omh_data_pick_dialog_msg);
        alertDialogBuilder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (dialog != null) {
                    Log.d("CdmaSimDialogActivity", "OMH data pick dialog, set data sub to " + CdmaSimDialogActivity.this.mTargetSubId);
                    CdmaSimDialogActivity.this.setDefaultDataSubId(CdmaSimDialogActivity.this, CdmaSimDialogActivity.this.mTargetSubId);
                    dialog.dismiss();
                }
                CdmaSimDialogActivity.this.finish();
            }
        });
        alertDialogBuilder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (dialog != null) {
                    dialog.dismiss();
                }
                CdmaSimDialogActivity.this.finish();
            }
        });
        alertDialogBuilder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                CdmaSimDialogActivity.this.finish();
            }
        });
        this.mDialog = alertDialogBuilder.create();
        this.mDialog.show();
    }

    public void enableStatusBarNavigation(boolean enable) {
        int state = 0;
        if (!enable) {
            int state2 = 2097152 | 16777216;
            state = state2 | 4194304 | 33554432;
        }
        Log.d("CdmaSimDialogActivity", "enableStatusBarNavigation, enable = " + enable);
        this.mStatusBarManager.disable(state);
    }

    public void setDefaultDataSubId(Context context, int subId) {
        SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
        subscriptionManager.setDefaultDataSubId(subId);
        if (this.mActionType != 0) {
            return;
        }
        Toast.makeText(context, R.string.data_switch_started, 1).show();
    }
}
