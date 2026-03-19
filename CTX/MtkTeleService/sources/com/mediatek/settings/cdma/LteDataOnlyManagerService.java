package com.mediatek.settings.cdma;

import android.app.AlertDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import com.android.internal.telephony.Phone;
import com.android.phone.PhoneUtils;
import com.android.phone.R;
import com.mediatek.phone.PhoneFeatureConstants;

public class LteDataOnlyManagerService extends Service {
    private AlertDialog mDialog;
    private Enable4GHandler mHandler;
    private Phone mPhone;
    private int mStartId = -1;
    private int mSubId = -1;
    private ContentObserver mContentObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean z) {
            if (LteDataOnlyManagerService.this.mDialog != null && LteDataOnlyManagerService.this.mDialog.isShowing() && !TelephonyUtilsEx.is4GDataOnly(LteDataOnlyManagerService.this)) {
                LteDataOnlyManagerService.this.mDialog.dismiss();
            }
        }
    };
    private ContentObserver mObserverForRadioState = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean z) {
            if (LteDataOnlyManagerService.this.mDialog != null && LteDataOnlyManagerService.this.mDialog.isShowing() && !TelephonyUtilsEx.isSvlteSlotRadioOn()) {
                LteDataOnlyManagerService.this.mDialog.dismiss();
            }
        }
    };
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("LteDataOnlyManagerService", "onReceive action = " + action);
            if (action.equals("android.intent.action.AIRPLANE_MODE")) {
                if (intent.getBooleanExtra("state", false)) {
                    Log.d("LteDataOnlyManagerService", "Action stop service");
                    LteDataOnlyManagerService.this.stopSelf(LteDataOnlyManagerService.this.mStartId);
                    return;
                }
                return;
            }
            if (action.equals("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED") && !TelephonyUtilsEx.isSvlteSlotInserted()) {
                Log.d("LteDataOnlyManagerService", "Action stop service");
                LteDataOnlyManagerService.this.stopSelf(LteDataOnlyManagerService.this.mStartId);
            }
        }
    };

    @Override
    public void onCreate() {
        Log.d("LteDataOnlyManagerService", "onCreate");
        super.onCreate();
        createPermissionDialog();
        getContentResolver().registerContentObserver(Settings.System.getUriFor("msim_mode_setting"), true, this.mObserverForRadioState);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.AIRPLANE_MODE");
        intentFilter.addAction("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED");
        registerReceiver(this.mReceiver, intentFilter);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int i, int i2) {
        this.mStartId = i2;
        this.mSubId = intent.getIntExtra("subscription", -1);
        this.mPhone = PhoneUtils.getPhoneUsingSubId(this.mSubId);
        this.mHandler = new Enable4GHandler();
        Log.d("LteDataOnlyManagerService", "onStartCommand, startId = " + i2 + "; sub = " + this.mSubId);
        ContentResolver contentResolver = getContentResolver();
        StringBuilder sb = new StringBuilder();
        sb.append("preferred_network_mode");
        sb.append(this.mPhone.getSubId());
        contentResolver.registerContentObserver(Settings.Global.getUriFor(sb.toString()), true, this.mContentObserver);
        showPermissionDialog();
        return 2;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("LteDataOnlyManagerService", "onDestroy");
        getContentResolver().unregisterContentObserver(this.mContentObserver);
        getContentResolver().unregisterContentObserver(this.mObserverForRadioState);
        if (this.mDialog != null) {
            this.mDialog.dismiss();
            this.mDialog = null;
        }
        unregisterReceiver(this.mReceiver);
    }

    private void createPermissionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.lte_only_dialog_title_prompt).setMessage(R.string.lte_data_only_prompt).setNegativeButton(R.string.lte_only_dialog_button_no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                LteDataOnlyManagerService.this.stopSelf(LteDataOnlyManagerService.this.mStartId);
            }
        }).setPositiveButton(R.string.lte_only_dialog_button_yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (!LteDataOnlyManagerService.this.checkServiceCondition()) {
                    Log.d("LteDataOnlyManagerService", "PositiveButton onClick :checkServiceCondition failed, stop");
                    LteDataOnlyManagerService.this.stopSelf(LteDataOnlyManagerService.this.mStartId);
                } else {
                    LteDataOnlyManagerService.this.mPhone.setPreferredNetworkType(10, LteDataOnlyManagerService.this.mHandler.obtainMessage(0));
                }
            }
        }).setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                Log.d("LteDataOnlyManagerService", "OnDismissListener :stopSelf(), mStartId = " + LteDataOnlyManagerService.this.mStartId);
                LteDataOnlyManagerService.this.stopSelf(LteDataOnlyManagerService.this.mStartId);
            }
        });
        this.mDialog = builder.create();
        this.mDialog.getWindow().setType(2003);
        this.mDialog.setCanceledOnTouchOutside(false);
    }

    private void showPermissionDialog() {
        if (!checkServiceCondition()) {
            stopSelf(this.mStartId);
        } else if (this.mDialog != null && !this.mDialog.isShowing()) {
            this.mDialog.show();
        }
    }

    private boolean checkServiceCondition() {
        return PhoneFeatureConstants.FeatureOption.isMtkTddDataOnlySupport() && TelephonyUtilsEx.is4GDataOnly(this) && !TelephonyUtilsEx.isAirPlaneMode() && TelephonyUtilsEx.isSvlteSlotInserted() && TelephonyUtilsEx.isSvlteSlotRadioOn();
    }

    private class Enable4GHandler extends Handler {
        private Enable4GHandler() {
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == 0) {
                handleSetEnable4GNetworkTypeResponse(message);
            }
        }

        private void handleSetEnable4GNetworkTypeResponse(Message message) {
            if (((AsyncResult) message.obj).exception == null) {
                Settings.Global.putInt(LteDataOnlyManagerService.this.mPhone.getContext().getContentResolver(), "preferred_network_mode" + LteDataOnlyManagerService.this.mPhone.getSubId(), 10);
                Log.d("LteDataOnlyManagerService", "Start Network updated intent");
                LteDataOnlyManagerService.this.sendBroadcast(new Intent("com.mediatek.intent.action.ACTION_NETWORK_CHANGED"));
                return;
            }
            Log.d("LteDataOnlyManagerService", "handleSetPreferredNetworkTypeResponse: exception in Enable4GHandler.");
        }
    }
}
