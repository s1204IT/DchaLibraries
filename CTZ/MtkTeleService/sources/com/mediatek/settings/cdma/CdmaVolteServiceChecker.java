package com.mediatek.settings.cdma;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.phone.PhoneUtils;
import com.android.phone.R;
import com.mediatek.ims.internal.MtkImsManager;
import com.mediatek.internal.telephony.MtkSubscriptionManager;
import com.mediatek.settings.TelephonyUtils;
import java.util.Iterator;
import java.util.List;

public class CdmaVolteServiceChecker extends Handler {
    private static SubscriptionManager mSubscriptionManager;
    private static CdmaVolteServiceChecker sInstance;
    private boolean mChecking;
    private ContentObserver mContentObserver;
    private Context mContext;
    private Dialog mDialog;
    private BroadcastReceiver mReceiver;

    public static CdmaVolteServiceChecker getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new CdmaVolteServiceChecker(context);
            mSubscriptionManager = SubscriptionManager.from(context);
        }
        return sInstance;
    }

    private CdmaVolteServiceChecker(Context context) {
        super(context.getMainLooper());
        this.mChecking = false;
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                Log.d("CdmaVolteServiceChecker", "onReceived, action = " + intent.getAction());
                CdmaVolteServiceChecker.this.updateState();
            }
        };
        this.mContentObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean z) {
                Log.d("CdmaVolteServiceChecker", "onChange...");
                CdmaVolteServiceChecker.this.updateState();
            }
        };
        this.mContext = context;
    }

    public void init() {
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("volte_vt_enabled"), true, this.mContentObserver);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("volte_vt_enabled_sim2"), true, this.mContentObserver);
        IntentFilter intentFilter = new IntentFilter("android.intent.action.SERVICE_STATE");
        intentFilter.addAction("android.intent.action.SIM_STATE_CHANGED");
        intentFilter.addAction("android.intent.action.AIRPLANE_MODE");
        intentFilter.addAction("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED");
        this.mContext.registerReceiver(this.mReceiver, intentFilter);
        updateState();
    }

    public void onEnable4gStateChanged() {
        Log.d("CdmaVolteServiceChecker", "onEnable4gStateChanged...");
        updateState();
    }

    private void updateState() {
        Log.d("CdmaVolteServiceChecker", "updateState, checking = " + this.mChecking);
        if (!this.mChecking && shouldShowVolteAlert()) {
            startTimeOutCheck();
        }
        if (this.mChecking && !shouldShowVolteAlert()) {
            stopTimeOutCheck();
        }
    }

    private int getMainCapabilitySubId() {
        int subIdUsingPhoneId = MtkSubscriptionManager.getSubIdUsingPhoneId(TelephonyUtilsEx.getMainPhoneId());
        Log.d("CdmaVolteServiceChecker", "getMainCapabilitySubId = " + subIdUsingPhoneId);
        return subIdUsingPhoneId;
    }

    private boolean shouldShowVolteAlert() {
        int listenSubId = getListenSubId();
        if (!SubscriptionManager.isValidSubscriptionId(listenSubId) || !TelephonyUtilsEx.isCtVolteEnabled() || !TelephonyUtilsEx.isCt4gSim(listenSubId)) {
            return false;
        }
        boolean zIsEnable4gOn = isEnable4gOn(listenSubId);
        boolean zIsEnhanced4gLteModeSettingEnabledByUser = MtkImsManager.isEnhanced4gLteModeSettingEnabledByUser(this.mContext, SubscriptionManager.getPhoneId(listenSubId));
        boolean zIsImsServiceAvailable = TelephonyUtils.isImsServiceAvailable(this.mContext, listenSubId);
        boolean zIsRoaming = TelephonyUtilsEx.isRoaming(PhoneFactory.getPhone(SubscriptionManager.getPhoneId(listenSubId)));
        boolean zIsAirPlaneMode = TelephonyUtilsEx.isAirPlaneMode();
        boolean zIsRadioOn = TelephonyUtils.isRadioOn(listenSubId, this.mContext);
        boolean zIsCtAutoVolteEnabled = TelephonyUtilsEx.isCtAutoVolteEnabled();
        boolean z = zIsImsServiceAvailable && isLteNetwork(listenSubId);
        Log.d("CdmaVolteServiceChecker", "shouldShowVolteAlert, subId = " + listenSubId + ", isEnable4gOn = " + zIsEnable4gOn + ", volteOn = " + zIsEnhanced4gLteModeSettingEnabledByUser + "imsAvailable = " + z + ", isRoaming = " + zIsRoaming + ", isAirplaneMode" + zIsAirPlaneMode + ", autoVolte = " + zIsCtAutoVolteEnabled);
        return (!zIsEnable4gOn || !zIsEnhanced4gLteModeSettingEnabledByUser || z || zIsRoaming || zIsAirPlaneMode || !zIsRadioOn || zIsCtAutoVolteEnabled) ? false : true;
    }

    private boolean isEnable4gOn(int i) {
        ContentResolver contentResolver = this.mContext.getContentResolver();
        StringBuilder sb = new StringBuilder();
        sb.append("preferred_network_mode");
        sb.append(i);
        return Settings.Global.getInt(contentResolver, sb.toString(), Phone.PREFERRED_NT_MODE) == 10;
    }

    private void startTimeOutCheck() {
        Log.d("CdmaVolteServiceChecker", "startTimeOutCheck...");
        this.mChecking = true;
        sendMessageDelayed(obtainMessage(100), 120000L);
    }

    private void stopTimeOutCheck() {
        Log.d("CdmaVolteServiceChecker", "stopTimeOutCheck...");
        this.mChecking = false;
        removeMessages(100);
    }

    @Override
    public void handleMessage(Message message) {
        if (message.what == 100) {
            Log.d("CdmaVolteServiceChecker", "time out..., mchecking = " + this.mChecking);
            if (this.mChecking && shouldShowVolteAlert()) {
                showAlertDialog(getListenSubId());
            }
        }
    }

    private void showAlertDialog(final int i) {
        Log.d("CdmaVolteServiceChecker", "showAlertDialog...");
        if (this.mDialog != null && this.mDialog.isShowing()) {
            Log.w("CdmaVolteServiceChecker", "dialog showing, do nothing...");
            return;
        }
        Context applicationContext = this.mContext.getApplicationContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(applicationContext, 5);
        builder.setMessage(applicationContext.getString(R.string.alert_volte_no_service, PhoneUtils.getSubDisplayName(i)));
        builder.setCancelable(false);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i2) {
                int phoneId = SubscriptionManager.getPhoneId(i);
                Log.d("CdmaVolteServiceChecker", "ok clicked, phoneId = " + phoneId);
                if (phoneId != Integer.MAX_VALUE) {
                    MtkImsManager.setEnhanced4gLteModeSetting(CdmaVolteServiceChecker.this.mContext, false, phoneId);
                }
                CdmaVolteServiceChecker.this.stopTimeOutCheck();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i2) {
                Log.d("CdmaVolteServiceChecker", "cancel clicked...");
                CdmaVolteServiceChecker.this.sendMessageDelayed(CdmaVolteServiceChecker.this.obtainMessage(100), 120000L);
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                Log.d("CdmaVolteServiceChecker", "cancelled...");
                CdmaVolteServiceChecker.this.sendMessageDelayed(CdmaVolteServiceChecker.this.obtainMessage(100), 120000L);
            }
        });
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                Log.d("CdmaVolteServiceChecker", "dismissed...");
                CdmaVolteServiceChecker.this.mDialog = null;
            }
        });
        AlertDialog alertDialogCreate = builder.create();
        alertDialogCreate.getWindow().setType(2003);
        alertDialogCreate.show();
        this.mDialog = alertDialogCreate;
    }

    private int getListenSubId() {
        int mainCapabilitySubId = getMainCapabilitySubId();
        if (mSubscriptionManager == null) {
            Log.d("CdmaVolteServiceChecker", "subManager mainId = " + mainCapabilitySubId);
            return mainCapabilitySubId;
        }
        if (SystemProperties.getInt("persist.vendor.mims_support", 1) == 1) {
            Log.d("CdmaVolteServiceChecker", "mims_support = 1, subId = " + mainCapabilitySubId);
            return mainCapabilitySubId;
        }
        if (TelephonyUtilsEx.isBothslotCtSim(mSubscriptionManager)) {
            Log.d("CdmaVolteServiceChecker", "getListenSubId mainId = " + mainCapabilitySubId);
            return mainCapabilitySubId;
        }
        List<SubscriptionInfo> activeSubscriptionInfoList = mSubscriptionManager.getActiveSubscriptionInfoList();
        if (activeSubscriptionInfoList == null) {
            Log.d("CdmaVolteServiceChecker", "infos mainId = " + mainCapabilitySubId);
            return mainCapabilitySubId;
        }
        Iterator<SubscriptionInfo> it = activeSubscriptionInfoList.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            int subscriptionId = it.next().getSubscriptionId();
            if (TelephonyUtilsEx.isCt4gSim(subscriptionId)) {
                mainCapabilitySubId = subscriptionId;
                break;
            }
        }
        Log.d("CdmaVolteServiceChecker", "getListenSubId = " + mainCapabilitySubId);
        return mainCapabilitySubId;
    }

    private boolean isLteNetwork(int i) {
        int dataNetworkType = TelephonyManager.getDefault().getDataNetworkType(i);
        int voiceNetworkType = TelephonyManager.getDefault().getVoiceNetworkType(i);
        Log.d("CdmaVolteServiceChecker", "dataNetworkType = " + dataNetworkType + ", voiceNetworkType = " + voiceNetworkType);
        boolean z = false;
        if (dataNetworkType == 0) {
            if (voiceNetworkType == 0) {
                voiceNetworkType = 0;
            }
        } else {
            voiceNetworkType = dataNetworkType;
        }
        if (voiceNetworkType == 13 || voiceNetworkType == 19) {
            z = true;
        }
        Log.d("CdmaVolteServiceChecker", "isLte = " + z);
        return z;
    }
}
