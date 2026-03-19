package com.mediatek.settings.cdma;

import android.app.AlertDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
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
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.phone.R;

public class LteSearchTimeoutCheckService extends Service {
    private AlertDialog mDialog;
    private Enable4GHandler mEnableHandler;
    private boolean mIsLteInService;
    private boolean mIsSvlteSlotInserted;
    private boolean mIsSvlteSlotRadioOn;
    private boolean mIsWaitingCheck;
    private Phone mPhone;
    private PhoneStateListener mPhoneStateListenerForLte;
    private TelephonyManager mTelephonyManager;
    private int mStartId = -1;
    private int mSubId = -1;
    private final Handler mHandler = new Handler();
    private Runnable mShowDialogRunnable = new Runnable() {
        @Override
        public void run() {
            LteSearchTimeoutCheckService.this.showTimeoutDialog();
        }
    };
    private ContentObserver mContentObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean z) {
            if (!TelephonyUtilsEx.is4GDataOnly(LteSearchTimeoutCheckService.this)) {
                Log.d("LteSearchTimeoutCheckService", "mContentObserver update, not 4GDataOnly,stopself");
                LteSearchTimeoutCheckService.this.stopSelf(LteSearchTimeoutCheckService.this.mStartId);
            }
        }
    };
    private ContentObserver mObserverForRadioState = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean z) {
            if (LteSearchTimeoutCheckService.this.mIsSvlteSlotRadioOn == TelephonyUtilsEx.isSvlteSlotRadioOn()) {
                return;
            }
            LteSearchTimeoutCheckService.this.mIsSvlteSlotRadioOn = !LteSearchTimeoutCheckService.this.mIsSvlteSlotRadioOn;
            Log.d("LteSearchTimeoutCheckService", "mObserverForRadioState update mIsSvlteSlotRadioOn : " + LteSearchTimeoutCheckService.this.mIsSvlteSlotRadioOn);
            if (LteSearchTimeoutCheckService.this.mIsSvlteSlotRadioOn) {
                LteSearchTimeoutCheckService.this.startCheckTimeout();
            } else {
                LteSearchTimeoutCheckService.this.stopCheck();
            }
        }
    };
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("LteSearchTimeoutCheckService", "onReceive action = " + action);
            if (action.equals("android.intent.action.AIRPLANE_MODE")) {
                if (intent.getBooleanExtra("state", false)) {
                    Log.d("LteSearchTimeoutCheckService", "Action enter flight mode");
                    LteSearchTimeoutCheckService.this.stopCheck();
                    return;
                } else {
                    Log.d("LteSearchTimeoutCheckService", "Action leave flight mode");
                    LteSearchTimeoutCheckService.this.startCheckTimeout();
                    return;
                }
            }
            if (!action.equals("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED") || LteSearchTimeoutCheckService.this.mIsSvlteSlotInserted == TelephonyUtilsEx.isSvlteSlotInserted()) {
                return;
            }
            LteSearchTimeoutCheckService.this.mIsSvlteSlotInserted = !LteSearchTimeoutCheckService.this.mIsSvlteSlotInserted;
            Log.d("LteSearchTimeoutCheckService", "Action update mIsSvlteSlotInserted : " + LteSearchTimeoutCheckService.this.mIsSvlteSlotInserted);
            if (LteSearchTimeoutCheckService.this.mIsSvlteSlotInserted) {
                LteSearchTimeoutCheckService.this.startCheckTimeout();
            } else {
                LteSearchTimeoutCheckService.this.stopCheck();
            }
        }
    };

    @Override
    public void onCreate() {
        Log.d("LteSearchTimeoutCheckService", "onCreate");
        super.onCreate();
        createTimeoutDialog();
        this.mTelephonyManager = (TelephonyManager) getSystemService("phone");
        this.mIsSvlteSlotInserted = TelephonyUtilsEx.isSvlteSlotInserted();
        this.mIsSvlteSlotRadioOn = TelephonyUtilsEx.isSvlteSlotRadioOn();
        this.mSubId = TelephonyUtilsEx.getCdmaSubId();
        this.mPhone = PhoneFactory.getPhone(SubscriptionManager.getPhoneId(this.mSubId));
        Log.d("LteSearchTimeoutCheckService", "subId = " + this.mSubId + ", mPhone = " + this.mPhone);
        this.mEnableHandler = new Enable4GHandler();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.AIRPLANE_MODE");
        intentFilter.addAction("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED");
        registerReceiver(this.mReceiver, intentFilter);
        getContentResolver().registerContentObserver(Settings.Global.getUriFor("preferred_network_mode" + this.mSubId), true, this.mContentObserver);
        getContentResolver().registerContentObserver(Settings.System.getUriFor("msim_mode_setting"), true, this.mObserverForRadioState);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int i, int i2) {
        Log.d("LteSearchTimeoutCheckService", "onStartCommand, startId = " + i2);
        this.mStartId = i2;
        startCheckTimeout();
        return 2;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("LteSearchTimeoutCheckService", "onDestroy");
        getContentResolver().unregisterContentObserver(this.mContentObserver);
        getContentResolver().unregisterContentObserver(this.mObserverForRadioState);
        this.mHandler.removeCallbacks(this.mShowDialogRunnable);
        if (this.mDialog != null) {
            this.mDialog.dismiss();
            this.mDialog = null;
        }
        if (this.mTelephonyManager != null && this.mPhoneStateListenerForLte != null) {
            this.mTelephonyManager.listen(this.mPhoneStateListenerForLte, 0);
        }
        unregisterReceiver(this.mReceiver);
    }

    private void startCheckTimeout() {
        Log.d("LteSearchTimeoutCheckService", "startCheckTimeout");
        if (!checkServiceCondition()) {
            return;
        }
        this.mIsWaitingCheck = false;
        if (this.mTelephonyManager != null && this.mDialog != null && !this.mDialog.isShowing()) {
            this.mHandler.removeCallbacks(this.mShowDialogRunnable);
            this.mHandler.postDelayed(this.mShowDialogRunnable, 180000L);
            createPhoneStateListener();
            if (this.mPhoneStateListenerForLte != null) {
                this.mTelephonyManager.listen(this.mPhoneStateListenerForLte, 1);
            }
            Log.d("LteSearchTimeoutCheckService", "startCheckTimeout ok");
        }
    }

    private void createTimeoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.lte_only_dialog_title_prompt).setMessage(R.string.lte_data_only_timeout).setNegativeButton(R.string.lte_only_dialog_button_no, (DialogInterface.OnClickListener) null).setPositiveButton(R.string.lte_only_dialog_button_yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Log.d("LteSearchTimeoutCheckService", "PositiveButton onClick");
                if (LteSearchTimeoutCheckService.this.checkServiceCondition()) {
                    Settings.Global.putInt(LteSearchTimeoutCheckService.this.mPhone.getContext().getContentResolver(), "preferred_network_mode" + LteSearchTimeoutCheckService.this.mSubId, 10);
                    LteSearchTimeoutCheckService.this.mPhone.setPreferredNetworkType(10, LteSearchTimeoutCheckService.this.mHandler.obtainMessage(0));
                }
            }
        }).setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                if (!LteSearchTimeoutCheckService.this.checkServiceCondition() || LteSearchTimeoutCheckService.this.mIsLteInService || LteSearchTimeoutCheckService.this.mIsWaitingCheck) {
                    Log.d("LteSearchTimeoutCheckService", "OnDismiss : donothing");
                    return;
                }
                Log.d("LteSearchTimeoutCheckService", "OnDismiss : will restart service");
                Intent intent = new Intent("com.mediatek.intent.action.STARTSELF_LTE_SEARCH_TIMEOUT_CHECK");
                intent.setComponent(new ComponentName("com.android.phone", "com.mediatek.settings.cdma.LteDataOnlySwitchReceiver"));
                LteSearchTimeoutCheckService.this.sendBroadcast(intent);
                LteSearchTimeoutCheckService.this.stopSelf(LteSearchTimeoutCheckService.this.mStartId);
            }
        });
        this.mDialog = builder.create();
        this.mDialog.getWindow().setType(2003);
        this.mDialog.setCanceledOnTouchOutside(false);
    }

    private void showTimeoutDialog() {
        if (checkServiceCondition() && this.mDialog != null && !this.mDialog.isShowing() && !this.mIsLteInService) {
            Log.d("LteSearchTimeoutCheckService", "showTimeoutDialog");
            this.mDialog.show();
        }
    }

    private void createPhoneStateListener() {
        if (this.mPhoneStateListenerForLte == null) {
            this.mPhoneStateListenerForLte = new PhoneStateListener(Integer.valueOf(this.mSubId)) {
                @Override
                public void onServiceStateChanged(ServiceState serviceState) {
                    Log.d("LteSearchTimeoutCheckService", "onServiceStateChanged, mSubId : " + this.mSubId + ", serviceState : " + serviceState);
                    if (serviceState.getDataRegState() != 0 || serviceState.getVoiceRegState() != 1 || serviceState.getDataNetworkType() != 13) {
                        if (LteSearchTimeoutCheckService.this.mIsLteInService) {
                            LteSearchTimeoutCheckService.this.mIsLteInService = false;
                            LteSearchTimeoutCheckService.this.startCheckTimeout();
                            return;
                        }
                        return;
                    }
                    Log.d("LteSearchTimeoutCheckService", "LTE is in service state, cancel show dialog");
                    LteSearchTimeoutCheckService.this.mIsLteInService = true;
                    LteSearchTimeoutCheckService.this.mHandler.removeCallbacks(LteSearchTimeoutCheckService.this.mShowDialogRunnable);
                    if (LteSearchTimeoutCheckService.this.mDialog != null && LteSearchTimeoutCheckService.this.mDialog.isShowing()) {
                        LteSearchTimeoutCheckService.this.mDialog.dismiss();
                    }
                }
            };
        }
    }

    private boolean checkServiceCondition() {
        return TelephonyUtilsEx.is4GDataOnly(this) && !TelephonyUtilsEx.isAirPlaneMode() && TelephonyUtilsEx.isSvlteSlotInserted() && TelephonyUtilsEx.isSvlteSlotRadioOn();
    }

    private void stopCheck() {
        Log.d("LteSearchTimeoutCheckService", "stopCheck");
        this.mIsLteInService = false;
        this.mIsWaitingCheck = true;
        this.mHandler.removeCallbacks(this.mShowDialogRunnable);
        if (this.mDialog != null && !this.mDialog.isShowing()) {
            this.mDialog.dismiss();
        }
        if (this.mPhoneStateListenerForLte != null) {
            this.mTelephonyManager.listen(this.mPhoneStateListenerForLte, 0);
        }
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
                Settings.Global.putInt(LteSearchTimeoutCheckService.this.mPhone.getContext().getContentResolver(), "preferred_network_mode" + LteSearchTimeoutCheckService.this.mSubId, 10);
                Log.d("LteSearchTimeoutCheckService", "Start Network updated intent");
                LteSearchTimeoutCheckService.this.sendBroadcast(new Intent("com.mediatek.intent.action.ACTION_NETWORK_CHANGED"));
                return;
            }
            Log.d("LteSearchTimeoutCheckService", "handleSetPreferredNetworkTypeResponse: exception in Enable4GHandler.");
        }
    }
}
