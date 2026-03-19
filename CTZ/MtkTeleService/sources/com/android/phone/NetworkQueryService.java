package com.android.phone;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncResult;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.telephony.CellIdentityGsm;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.NetworkScan;
import android.telephony.NetworkScanRequest;
import android.telephony.RadioAccessSpecifier;
import android.telephony.TelephonyManager;
import android.telephony.TelephonyScanManager;
import android.util.Log;
import com.android.internal.telephony.OperatorInfo;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.phone.INetworkQueryService;
import com.mediatek.internal.telephony.MtkGsmCdmaPhone;
import com.mediatek.internal.telephony.MtkSubscriptionManager;
import java.util.ArrayList;
import java.util.List;

public class NetworkQueryService extends Service {
    static final String ACTION_LOCAL_BINDER = "com.android.phone.intent.action.LOCAL_BINDER";
    private static final boolean DBG = true;
    private static final int EVENT_CANCEL_NETWORK_SCAN_COMPLETED = 200;
    private static final int EVENT_NETWORK_SCAN_COMPLETED = 400;
    private static final int EVENT_NETWORK_SCAN_ERROR = 300;
    private static final int EVENT_NETWORK_SCAN_RESULTS = 200;
    private static final int EVENT_NETWORK_SCAN_VIA_PHONE_COMPLETED = 100;
    private static final boolean INCREMENTAL_RESULTS = true;
    private static final int INCREMENTAL_RESULTS_PERIODICITY_SEC = 3;
    private static final String LOG_TAG = "NetworkQuery";
    private static final int MAX_SEARCH_TIME_SEC = 300;
    public static final int QUERY_EXCEPTION = 1;
    private static final int QUERY_IS_RUNNING = -2;
    public static final int QUERY_OK = 0;
    private static final int QUERY_READY = -1;
    private static final int SCAN_TYPE = 0;
    private static final int SEARCH_PERIODICITY_SEC = 5;
    private NetworkScan mNetworkScan;
    private int mState;
    private final IBinder mLocalBinder = new LocalBinder();
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            int i = message.what;
            if (i == 100) {
                NetworkQueryService.log("scan via Phone completed, broadcasting results");
                NetworkQueryService.this.broadcastQueryResults(message);
                return;
            }
            if (i == 200) {
                NetworkQueryService.log("get scan results, broadcasting results");
                NetworkQueryService.this.broadcastQueryResults(message);
            } else if (i == 300) {
                NetworkQueryService.log("get scan error, broadcasting error code");
                NetworkQueryService.this.broadcastQueryResults(message);
            } else if (i == 400) {
                NetworkQueryService.log("network scan or stop network query completed");
                NetworkQueryService.this.broadcastQueryResults(message);
            }
        }
    };
    final RemoteCallbackList<INetworkQueryServiceCallback> mCallbacks = new RemoteCallbackList<>();
    private final INetworkQueryService.Stub mBinder = new INetworkQueryService.Stub() {
        @Override
        public void startNetworkQuery(INetworkQueryServiceCallback iNetworkQueryServiceCallback, int i, boolean z) {
            if (iNetworkQueryServiceCallback != null) {
                synchronized (NetworkQueryService.this.mCallbacks) {
                    NetworkQueryService.this.mCallbacks.register(iNetworkQueryServiceCallback);
                    NetworkQueryService.log("registering callback " + iNetworkQueryServiceCallback.getClass().toString());
                    switch (NetworkQueryService.this.mState) {
                        case NetworkQueryService.QUERY_IS_RUNNING:
                            NetworkQueryService.log("query already in progress");
                            break;
                        case -1:
                            if (z) {
                                NetworkQueryService.log("start network scan via TelephonManager");
                                NetworkQueryService.this.mNetworkScan = new TelephonyManager(NetworkQueryService.this.getApplicationContext(), MtkSubscriptionManager.getSubIdUsingPhoneId(i)).requestNetworkScan(new NetworkScanRequest(0, new RadioAccessSpecifier[]{new RadioAccessSpecifier(1, null, null), new RadioAccessSpecifier(3, null, null), new RadioAccessSpecifier(2, null, null)}, 5, TimeConsumingPreferenceActivity.EXCEPTION_ERROR, true, 3, null), NetworkQueryService.this.new NetworkScanCallbackImpl());
                                NetworkQueryService.this.mState = NetworkQueryService.QUERY_IS_RUNNING;
                            } else {
                                Phone phone = PhoneFactory.getPhone(i);
                                if (phone == null) {
                                    NetworkQueryService.log("phone is null");
                                } else {
                                    phone.getAvailableNetworks(NetworkQueryService.this.mHandler.obtainMessage(100));
                                    NetworkQueryService.this.mState = NetworkQueryService.QUERY_IS_RUNNING;
                                    NetworkQueryService.this.mPhoneId = i;
                                    NetworkQueryService.log("start network scan via Phone");
                                }
                            }
                            break;
                    }
                }
            }
        }

        @Override
        public void stopNetworkQuery() {
            NetworkQueryService.log("stop network query");
            NetworkQueryService.this.stopQueryAvailableNetworks();
            if (NetworkQueryService.this.mNetworkScan != null) {
                try {
                    NetworkQueryService.this.mNetworkScan.stop();
                    NetworkQueryService.this.mState = -1;
                } catch (RemoteException e) {
                    NetworkQueryService.log("stop mNetworkScan failed");
                } catch (IllegalArgumentException e2) {
                }
            }
        }

        @Override
        public void unregisterCallback(INetworkQueryServiceCallback iNetworkQueryServiceCallback) {
            if (iNetworkQueryServiceCallback != null) {
                synchronized (NetworkQueryService.this.mCallbacks) {
                    NetworkQueryService.log("unregistering callback " + iNetworkQueryServiceCallback.getClass().toString());
                    NetworkQueryService.this.mCallbacks.unregister(iNetworkQueryServiceCallback);
                }
            }
        }
    };
    private int mPhoneId = -1;

    public class LocalBinder extends Binder {
        public LocalBinder() {
        }

        INetworkQueryService getService() {
            return NetworkQueryService.this.mBinder;
        }
    }

    public class NetworkScanCallbackImpl extends TelephonyScanManager.NetworkScanCallback {
        public NetworkScanCallbackImpl() {
        }

        @Override
        public void onResults(List<CellInfo> list) {
            NetworkQueryService.log("got network scan results: " + list.size());
            NetworkQueryService.this.mHandler.obtainMessage(200, list).sendToTarget();
        }

        @Override
        public void onComplete() {
            NetworkQueryService.log("network scan completed");
            NetworkQueryService.this.mHandler.obtainMessage(400).sendToTarget();
        }

        @Override
        public void onError(int i) {
            NetworkQueryService.log("network scan got error: " + i);
            NetworkQueryService.this.mHandler.obtainMessage(TimeConsumingPreferenceActivity.EXCEPTION_ERROR, i, 0).sendToTarget();
        }
    }

    @Override
    public void onCreate() {
        this.mState = -1;
    }

    @Override
    public void onStart(Intent intent, int i) {
    }

    @Override
    public IBinder onBind(Intent intent) {
        log("binding service implementation");
        if (ACTION_LOCAL_BINDER.equals(intent.getAction())) {
            return this.mLocalBinder;
        }
        return this.mBinder;
    }

    private void broadcastQueryResults(Message message) {
        synchronized (this.mCallbacks) {
            this.mState = -1;
            for (int iBeginBroadcast = this.mCallbacks.beginBroadcast() - 1; iBeginBroadcast >= 0; iBeginBroadcast--) {
                INetworkQueryServiceCallback iNetworkQueryServiceCallback = (INetworkQueryServiceCallback) this.mCallbacks.getBroadcastItem(iBeginBroadcast);
                log("broadcasting results to " + iNetworkQueryServiceCallback.getClass().toString());
                try {
                    int i = message.what;
                    if (i == 100) {
                        AsyncResult asyncResult = (AsyncResult) message.obj;
                        if (asyncResult != null) {
                            iNetworkQueryServiceCallback.onResults(getCellInfoList((List) asyncResult.result));
                        } else {
                            log("AsyncResult is null.");
                        }
                        iNetworkQueryServiceCallback.onComplete();
                    } else if (i == 200) {
                        iNetworkQueryServiceCallback.onResults((List) message.obj);
                    } else if (i == 300) {
                        iNetworkQueryServiceCallback.onError(message.arg1);
                    } else if (i == 400) {
                        iNetworkQueryServiceCallback.onComplete();
                    }
                } catch (RemoteException e) {
                }
            }
            this.mCallbacks.finishBroadcast();
        }
    }

    private List<CellInfo> getCellInfoList(List<OperatorInfo> list) {
        String str;
        String strSubstring;
        ArrayList arrayList = new ArrayList();
        if (list == null) {
            log("list is null.");
            return arrayList;
        }
        for (OperatorInfo operatorInfo : list) {
            String operatorNumeric = operatorInfo.getOperatorNumeric();
            log("operatorNumeric: " + operatorNumeric);
            if (operatorNumeric == null || !operatorNumeric.matches("^[0-9]{5,6}$")) {
                str = null;
                strSubstring = null;
            } else {
                String strSubstring2 = operatorNumeric.substring(0, 3);
                strSubstring = operatorNumeric.substring(3);
                str = strSubstring2;
            }
            CellIdentityGsm cellIdentityGsm = new CellIdentityGsm(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, str, strSubstring, operatorInfo.getOperatorAlphaLong(), operatorInfo.getOperatorAlphaShort());
            CellInfoGsm cellInfoGsm = new CellInfoGsm();
            cellInfoGsm.setCellIdentity(cellIdentityGsm);
            arrayList.add(cellInfoGsm);
        }
        return arrayList;
    }

    private static void log(String str) {
        Log.d(LOG_TAG, str);
    }

    private boolean handleMessageMTK(int i) {
        log("[handleMessageMTK] msg = " + i);
        if (i == 200) {
            log("cancel get available networks action... ");
            return true;
        }
        return false;
    }

    private void stopQueryAvailableNetworks() {
        log("[stopQueryAvailableNetworks] PhoneID = " + this.mPhoneId + "; mState = " + this.mState);
        if (this.mPhoneId == -1) {
            return;
        }
        if (QUERY_IS_RUNNING == this.mState) {
            MtkGsmCdmaPhone phone = PhoneFactory.getPhone(this.mPhoneId);
            if (phone != null) {
                phone.cancelAvailableNetworks(this.mHandler.obtainMessage(200));
            } else {
                log("[stopQueryAvailableNetworks] phone is null!!");
            }
            this.mState = -1;
        }
        this.mPhoneId = -1;
    }
}
