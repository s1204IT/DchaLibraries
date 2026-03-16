package com.android.phone;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.OperatorInfo;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.phone.INetworkQueryServiceCallback;
import com.android.phone.NetworkQueryService;
import java.util.HashMap;
import java.util.List;

public class NetworkSetting extends PreferenceActivity implements DialogInterface.OnCancelListener {
    AlertDialog mAlertDialog;
    private Preference mAutoSelect;
    private Phone mNeedDisconnectDataPhone;
    private PreferenceGroup mNetworkList;
    private HashMap<Preference, OperatorInfo> mNetworkMap;
    String mNetworkSelectMsg;
    Phone mPhone;
    private Preference mSearchButton;
    private int mSubId;
    TelephonyManager mTelephonyManager;
    private UserManager mUm;
    private boolean mUnavailable;
    private Preference pendingObj;
    protected boolean mIsForeground = false;
    private int pendingAction = 0;
    private boolean needReconnectDataCall = false;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 100:
                    NetworkSetting.this.networksListLoaded((List) msg.obj, msg.arg1);
                    if (NetworkSetting.this.needReconnectDataCall) {
                        NetworkSetting.this.mNeedDisconnectDataPhone.enableDataCall();
                        NetworkSetting.this.needReconnectDataCall = false;
                    }
                    break;
                case 200:
                    NetworkSetting.this.log("hideProgressPanel");
                    NetworkSetting.this.removeDialog(100);
                    NetworkSetting.this.getPreferenceScreen().setEnabled(true);
                    AsyncResult ar = (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        NetworkSetting.this.log("manual network selection: failed!");
                        NetworkSetting.this.displayNetworkSelectionFailed(ar.exception);
                    } else {
                        NetworkSetting.this.log("manual network selection: succeeded!");
                        NetworkSetting.this.displayNetworkSelectionSucceeded();
                    }
                    NetworkSetting.this.mPhone = PhoneGlobals.getPhone();
                    if (NetworkSetting.this.needReconnectDataCall) {
                        NetworkSetting.this.mNeedDisconnectDataPhone.enableDataCall();
                        NetworkSetting.this.needReconnectDataCall = false;
                    }
                    break;
                case 300:
                    NetworkSetting.this.log("hideProgressPanel");
                    try {
                        NetworkSetting.this.dismissDialog(300);
                    } catch (IllegalArgumentException e) {
                        Log.w("phone", "[NetworksList] Fail to dismiss auto select dialog ", e);
                    }
                    NetworkSetting.this.getPreferenceScreen().setEnabled(true);
                    AsyncResult ar2 = (AsyncResult) msg.obj;
                    if (ar2.exception != null) {
                        NetworkSetting.this.log("automatic network selection: failed!");
                        NetworkSetting.this.displayNetworkSelectionFailed(ar2.exception);
                    } else {
                        NetworkSetting.this.log("automatic network selection: succeeded!");
                        NetworkSetting.this.displayNetworkSelectionSucceeded();
                    }
                    NetworkSetting.this.mPhone = PhoneGlobals.getPhone();
                    if (NetworkSetting.this.needReconnectDataCall) {
                        NetworkSetting.this.mNeedDisconnectDataPhone.enableDataCall();
                        NetworkSetting.this.needReconnectDataCall = false;
                    }
                    break;
                case 400:
                    NetworkSetting.this.log("event ACTION_NETWORK_SCAN");
                    if (NetworkSetting.this.checkPhoneState(400, (Preference) msg.obj)) {
                        NetworkSetting.this.loadNetworksList();
                    }
                    break;
                case 500:
                    NetworkSetting.this.log("event ACTION_NETWORK_SELECTION");
                    if (NetworkSetting.this.checkPhoneState(500, (Preference) msg.obj)) {
                        NetworkSetting.this.selectNetworkManually((Preference) msg.obj);
                    }
                    break;
                case 600:
                    NetworkSetting.this.log("event ACTION_AUTO_SELECT");
                    if (NetworkSetting.this.checkPhoneState(600, (Preference) msg.obj)) {
                        NetworkSetting.this.selectNetworkAutomatic();
                    }
                    break;
            }
        }
    };
    private INetworkQueryService mNetworkQueryService = null;
    private final ServiceConnection mNetworkQueryServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            NetworkSetting.this.log("connection created, binding local service.");
            NetworkSetting.this.mNetworkQueryService = ((NetworkQueryService.LocalBinder) service).getService();
            NetworkSetting.this.checkAndLoadNetworksList();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            NetworkSetting.this.log("connection disconnected, cleaning local binding.");
            NetworkSetting.this.mNetworkQueryService = null;
        }
    };
    private final INetworkQueryServiceCallback mCallback = new INetworkQueryServiceCallback.Stub() {
        @Override
        public void onQueryComplete(List<OperatorInfo> networkInfoArray, int status) {
            NetworkSetting.this.log("notifying message loop of query completion.");
            Message msg = NetworkSetting.this.mHandler.obtainMessage(100, status, 0, networkInfoArray);
            msg.sendToTarget();
        }
    };
    private PhoneStateListener mPhoneStateListener = new PhoneStateListener(SubscriptionManager.getDefaultDataSubId()) {
        @Override
        public void onDataConnectionStateChanged(int state) {
            if (state == 0 && NetworkSetting.this.pendingAction != 0) {
                Message msg = NetworkSetting.this.mHandler.obtainMessage(NetworkSetting.this.pendingAction, NetworkSetting.this.pendingObj);
                NetworkSetting.this.mHandler.sendMessageDelayed(msg, 1000L);
                NetworkSetting.this.pendingAction = 0;
                NetworkSetting.this.pendingObj = null;
            }
        }
    };
    private DialogInterface.OnClickListener mActionCancelListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            NetworkSetting.this.finish();
        }
    };

    private class DataAlertConfirmListener implements DialogInterface.OnClickListener {
        private int mAction;
        private Preference mObj;

        public DataAlertConfirmListener(int action, Preference obj) {
            this.mAction = 0;
            this.mObj = null;
            this.mAction = action;
            this.mObj = obj;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            NetworkSetting.this.pendingAction = this.mAction;
            NetworkSetting.this.pendingObj = this.mObj;
            switch (NetworkSetting.this.pendingAction) {
                case 400:
                    if (NetworkSetting.this.mIsForeground) {
                        NetworkSetting.this.showDialog(200);
                    }
                    NetworkSetting.this.displayEmptyNetworkList(false);
                    NetworkSetting.this.needReconnectDataCall = true;
                    NetworkSetting.this.mNeedDisconnectDataPhone.disableDataCall();
                    break;
                case 500:
                    String networkStr = NetworkSetting.this.pendingObj.getTitle().toString();
                    NetworkSetting.this.displayNetworkSeletionInProgress(networkStr);
                    NetworkSetting.this.needReconnectDataCall = true;
                    NetworkSetting.this.mNeedDisconnectDataPhone.disableDataCall();
                    break;
                case 600:
                    if (NetworkSetting.this.mIsForeground) {
                        NetworkSetting.this.showDialog(300);
                    }
                    NetworkSetting.this.needReconnectDataCall = true;
                    NetworkSetting.this.mNeedDisconnectDataPhone.disableDataCall();
                    break;
            }
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == this.mSearchButton) {
            checkAndLoadNetworksList();
            return true;
        }
        if (preference == this.mAutoSelect) {
            checkAndSelectNetworkAutomatic();
            return true;
        }
        checkAndSelectNetworkManually(preference);
        return true;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        if (dialog == this.mAlertDialog) {
            finish();
            return;
        }
        try {
            this.mNetworkQueryService.stopNetworkQuery(this.mCallback);
        } catch (RemoteException e) {
            log("onCancel: exception from stopNetworkQuery " + e);
        }
        finish();
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mUm = (UserManager) getSystemService("user");
        if (this.mUm.hasUserRestriction("no_config_mobile_networks")) {
            setContentView(R.layout.telephony_disallowed_preference_screen);
            this.mUnavailable = true;
            return;
        }
        addPreferencesFromResource(R.xml.carrier_select);
        this.mSubId = getIntent().getIntExtra("sub_id", SubscriptionManager.getDefaultSubId());
        this.mPhone = PhoneGlobals.getPhone(this.mSubId);
        this.mTelephonyManager = (TelephonyManager) getSystemService("phone");
        this.mTelephonyManager.listen(this.mPhoneStateListener, 64);
        this.mNetworkList = (PreferenceGroup) getPreferenceScreen().findPreference("list_networks_key");
        this.mNetworkMap = new HashMap<>();
        this.mSearchButton = getPreferenceScreen().findPreference("button_srch_netwrks_key");
        this.mAutoSelect = getPreferenceScreen().findPreference("button_auto_select_key");
        startService(new Intent(this, (Class<?>) NetworkQueryService.class).putExtra("sub_id", this.mSubId));
        bindService(new Intent(this, (Class<?>) NetworkQueryService.class), this.mNetworkQueryServiceConnection, 1);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mIsForeground = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mIsForeground = false;
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        try {
            this.mNetworkQueryService.unregisterCallback(this.mCallback);
        } catch (RemoteException e) {
            log("onDestroy: exception from unregisterCallback " + e);
        }
        this.mTelephonyManager.listen(this.mPhoneStateListener, 0);
        if (!this.mUnavailable) {
            unbindService(this.mNetworkQueryServiceConnection);
        }
        if (this.needReconnectDataCall) {
            this.mNeedDisconnectDataPhone.enableDataCall();
            this.needReconnectDataCall = false;
        }
        super.onDestroy();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == 100 || id == 200 || id == 300) {
            ProgressDialog dialog = new ProgressDialog(this);
            switch (id) {
                case 100:
                    dialog.setMessage(this.mNetworkSelectMsg);
                    dialog.setCancelable(false);
                    dialog.setIndeterminate(true);
                    break;
                case 300:
                    dialog.setMessage(getResources().getString(R.string.register_automatically));
                    dialog.setCancelable(false);
                    dialog.setIndeterminate(true);
                    break;
                default:
                    dialog.setMessage(getResources().getString(R.string.load_networks_progress));
                    dialog.setCanceledOnTouchOutside(false);
                    dialog.setOnCancelListener(this);
                    break;
            }
            return dialog;
        }
        return null;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        if (id == 100 || id == 200 || id == 300) {
            getPreferenceScreen().setEnabled(false);
        }
    }

    private void displayEmptyNetworkList(boolean flag) {
        this.mNetworkList.setTitle(flag ? R.string.empty_networks_list : R.string.label_available);
    }

    private void displayNetworkSeletionInProgress(String networkStr) {
        this.mNetworkSelectMsg = getResources().getString(R.string.register_on_network, networkStr);
        if (this.mIsForeground) {
            showDialog(100);
        }
    }

    private void displayNetworkQueryFailed(int error) {
        String status = getResources().getString(R.string.network_query_error);
        PhoneGlobals app = PhoneGlobals.getInstance();
        app.notificationMgr.postTransientNotification(2, status);
    }

    private void displayNetworkSelectionFailed(Throwable ex) {
        String status;
        if (ex != null && (ex instanceof CommandException) && ((CommandException) ex).getCommandError() == CommandException.Error.ILLEGAL_SIM_OR_ME) {
            status = getResources().getString(R.string.not_allowed);
        } else {
            status = getResources().getString(R.string.connect_later);
        }
        PhoneGlobals app = PhoneGlobals.getInstance();
        app.notificationMgr.postTransientNotification(2, status);
    }

    private void displayNetworkSelectionSucceeded() {
        String status = getResources().getString(R.string.registration_done);
        PhoneGlobals app = PhoneGlobals.getInstance();
        app.notificationMgr.postTransientNotification(2, status);
        this.mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                NetworkSetting.this.finish();
            }
        }, 3000L);
    }

    private void loadNetworksList() {
        log("load networks list...");
        try {
            this.mNetworkQueryService.startNetworkQuery(this.mCallback);
        } catch (RemoteException e) {
            log("loadNetworksList: exception from startNetworkQuery " + e);
            if (this.mIsForeground) {
                try {
                    dismissDialog(200);
                } catch (IllegalArgumentException e2) {
                }
            }
        }
    }

    private void networksListLoaded(List<OperatorInfo> result, int status) {
        String startString;
        int order;
        String stateString;
        log("networks list loaded");
        try {
            this.mNetworkQueryService.unregisterCallback(this.mCallback);
        } catch (RemoteException e) {
            log("networksListLoaded: exception from unregisterCallback " + e);
        }
        log("hideProgressPanel");
        try {
            dismissDialog(200);
        } catch (IllegalArgumentException e2) {
            log("Fail to dismiss network load list dialog " + e2);
        }
        getPreferenceScreen().setEnabled(true);
        clearList();
        if (status != 0) {
            log("error while querying available networks");
            displayNetworkQueryFailed(status);
            displayEmptyNetworkList(true);
            return;
        }
        if (result != null) {
            displayEmptyNetworkList(false);
            int count = 0;
            for (OperatorInfo ni : result) {
                Preference carrier = new Preference(this, null);
                TelephonyManager telephonyManager = this.mTelephonyManager;
                String simNumeric = TelephonyManager.getTelephonyProperty(this.mPhone.getPhoneId(), "gsm.sim.operator.numeric", "");
                if (ni.getRat() == OperatorInfo.RadioAccessTechnology.GSM || ni.getRat() == OperatorInfo.RadioAccessTechnology.GSM_COMPACT || ni.getRat() == OperatorInfo.RadioAccessTechnology.GSM_wEGPRS) {
                    startString = "2G ";
                    order = (result.size() * 2) + count + 2;
                } else if (ni.getRat() == OperatorInfo.RadioAccessTechnology.UTRAN || ni.getRat() == OperatorInfo.RadioAccessTechnology.UTRAN_wHSDPA || ni.getRat() == OperatorInfo.RadioAccessTechnology.UTRAN_wHSUPA || ni.getRat() == OperatorInfo.RadioAccessTechnology.UTRAN_wHSDPA_HSUPA) {
                    startString = "3G ";
                    order = result.size() + count + 2;
                } else if (ni.getRat() == OperatorInfo.RadioAccessTechnology.E_UTRAN) {
                    startString = "4G ";
                    order = count + 2;
                } else {
                    startString = null;
                    order = (result.size() * 3) + 2;
                }
                if (ni.getState() == OperatorInfo.State.AVAILABLE) {
                    if (ni.getOperatorNumeric().equals(simNumeric)) {
                        stateString = " (A, HPLMN)";
                    } else {
                        stateString = " (A)";
                    }
                } else if (ni.getState() == OperatorInfo.State.CURRENT) {
                    if (ni.getOperatorNumeric().equals(simNumeric)) {
                        stateString = " (C, HPLMN)";
                    } else {
                        stateString = " (C, VPLMN)";
                    }
                } else if (ni.getState() == OperatorInfo.State.FORBIDDEN) {
                    stateString = " (F)";
                } else {
                    stateString = null;
                }
                carrier.setTitle(startString + ni.getOperatorAlphaLong() + stateString);
                carrier.setPersistent(false);
                carrier.setOrder(order);
                count++;
                this.mNetworkList.addPreference(carrier);
                this.mNetworkMap.put(carrier, ni);
                log("  " + ni);
            }
            return;
        }
        displayEmptyNetworkList(true);
    }

    private void clearList() {
        for (Preference p : this.mNetworkMap.keySet()) {
            this.mNetworkList.removePreference(p);
        }
        this.mNetworkMap.clear();
    }

    private void selectNetworkAutomatic() {
        log("select network automatically...");
        Message msg = this.mHandler.obtainMessage(300);
        this.mPhone.setNetworkSelectionModeAutomatic(msg);
    }

    private void selectNetworkManually(Preference selectedCarrier) {
        if (selectedCarrier != null) {
            String networkStr = selectedCarrier.getTitle().toString();
            log("selected network: " + networkStr);
            Message msg = this.mHandler.obtainMessage(200);
            this.mPhone.selectNetworkManually(this.mNetworkMap.get(selectedCarrier), msg);
        }
    }

    private boolean checkPhoneState(int action, Preference selectedCarrier) {
        int resId;
        boolean ret = false;
        log("check preconditions for network operations");
        if (isFinishing()) {
            log("Network Setting is finishing, do nothing...");
            return false;
        }
        int dataState = this.mTelephonyManager.getDataState();
        if (action == 400) {
            resId = R.string.query_network_during_data_call;
        } else {
            resId = R.string.select_network_during_data_call;
        }
        if (this.mPhone.getState() != PhoneConstants.State.IDLE) {
            this.mAlertDialog = new AlertDialog.Builder(this).setTitle(getString(R.string.network_operation_information)).setIcon(android.R.drawable.ic_dialog_alert).setMessage(getString(R.string.network_operation_during_call)).setPositiveButton(getString(R.string.ok), this.mActionCancelListener).setCancelable(true).setOnCancelListener(this).show();
        } else if (dataState == 2 || dataState == 1) {
            this.mNeedDisconnectDataPhone = PhoneGlobals.getPhone(SubscriptionManager.getDefaultDataSubId());
            this.mAlertDialog = new AlertDialog.Builder(this).setTitle(getString(R.string.network_operation_information)).setIcon(android.R.drawable.ic_dialog_alert).setMessage(getString(resId)).setPositiveButton(getString(R.string.ok), new DataAlertConfirmListener(action, selectedCarrier)).setNegativeButton(getString(R.string.cancel), this.mActionCancelListener).setCancelable(true).setOnCancelListener(this).show();
        } else {
            ret = true;
        }
        return ret;
    }

    private void checkAndLoadNetworksList() {
        if (checkPhoneState(400, null)) {
            if (this.mIsForeground) {
                showDialog(200);
            }
            displayEmptyNetworkList(false);
            loadNetworksList();
        }
    }

    private void checkAndSelectNetworkAutomatic() {
        if (checkPhoneState(600, null)) {
            if (this.mIsForeground) {
                showDialog(300);
            }
            selectNetworkAutomatic();
        }
    }

    private void checkAndSelectNetworkManually(Preference selectedCarrier) {
        if (checkPhoneState(500, selectedCarrier)) {
            String networkStr = selectedCarrier.getTitle().toString();
            displayNetworkSeletionInProgress(networkStr);
            selectNetworkManually(selectedCarrier);
        }
    }

    private void log(String msg) {
        Log.d("phone", "[NetworksList] " + msg);
    }
}
