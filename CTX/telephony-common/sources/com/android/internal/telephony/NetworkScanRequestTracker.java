package com.android.internal.telephony;

import android.os.AsyncResult;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.telephony.CellInfo;
import android.telephony.NetworkScanRequest;
import android.telephony.RadioAccessSpecifier;
import android.util.Log;
import com.android.internal.telephony.CommandException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class NetworkScanRequestTracker {
    private static final int CMD_INTERRUPT_NETWORK_SCAN = 6;
    private static final int CMD_START_NETWORK_SCAN = 1;
    private static final int CMD_STOP_NETWORK_SCAN = 4;
    private static final int EVENT_INTERRUPT_NETWORK_SCAN_DONE = 7;
    private static final int EVENT_RECEIVE_NETWORK_SCAN_RESULT = 3;
    private static final int EVENT_START_NETWORK_SCAN_DONE = 2;
    private static final int EVENT_STOP_NETWORK_SCAN_DONE = 5;
    private static final String TAG = "ScanRequestTracker";
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    NetworkScanRequestTracker.this.mScheduler.doStartScan((NetworkScanRequestInfo) message.obj);
                    break;
                case 2:
                    NetworkScanRequestTracker.this.mScheduler.startScanDone((AsyncResult) message.obj);
                    break;
                case 3:
                    NetworkScanRequestTracker.this.mScheduler.receiveResult((AsyncResult) message.obj);
                    break;
                case 4:
                    NetworkScanRequestTracker.this.mScheduler.doStopScan(message.arg1);
                    break;
                case 5:
                    NetworkScanRequestTracker.this.mScheduler.stopScanDone((AsyncResult) message.obj);
                    break;
                case 6:
                    NetworkScanRequestTracker.this.mScheduler.doInterruptScan(message.arg1);
                    break;
                case 7:
                    NetworkScanRequestTracker.this.mScheduler.interruptScanDone((AsyncResult) message.obj);
                    break;
            }
        }
    };
    private final AtomicInteger mNextNetworkScanRequestId = new AtomicInteger(1);
    private final NetworkScanRequestScheduler mScheduler = new NetworkScanRequestScheduler();

    private void logEmptyResultOrException(AsyncResult asyncResult) {
        if (asyncResult.result == null) {
            Log.e(TAG, "NetworkScanResult: Empty result");
            return;
        }
        Log.e(TAG, "NetworkScanResult: Exception: " + asyncResult.exception);
    }

    private boolean isValidScan(NetworkScanRequestInfo networkScanRequestInfo) {
        if (networkScanRequestInfo.mRequest == null || networkScanRequestInfo.mRequest.getSpecifiers() == null || networkScanRequestInfo.mRequest.getSpecifiers().length > 8) {
            return false;
        }
        for (RadioAccessSpecifier radioAccessSpecifier : networkScanRequestInfo.mRequest.getSpecifiers()) {
            if (radioAccessSpecifier.getRadioAccessNetwork() != 1 && radioAccessSpecifier.getRadioAccessNetwork() != 2 && radioAccessSpecifier.getRadioAccessNetwork() != 3) {
                return false;
            }
            if (radioAccessSpecifier.getBands() != null && radioAccessSpecifier.getBands().length > 8) {
                return false;
            }
            if (radioAccessSpecifier.getChannels() != null && radioAccessSpecifier.getChannels().length > 32) {
                return false;
            }
        }
        if (networkScanRequestInfo.mRequest.getSearchPeriodicity() < 5 || networkScanRequestInfo.mRequest.getSearchPeriodicity() > 300 || networkScanRequestInfo.mRequest.getMaxSearchTime() < 60 || networkScanRequestInfo.mRequest.getMaxSearchTime() > 3600 || networkScanRequestInfo.mRequest.getIncrementalResultsPeriodicity() < 1 || networkScanRequestInfo.mRequest.getIncrementalResultsPeriodicity() > 10 || networkScanRequestInfo.mRequest.getSearchPeriodicity() > networkScanRequestInfo.mRequest.getMaxSearchTime() || networkScanRequestInfo.mRequest.getIncrementalResultsPeriodicity() > networkScanRequestInfo.mRequest.getMaxSearchTime()) {
            return false;
        }
        return networkScanRequestInfo.mRequest.getPlmns() == null || networkScanRequestInfo.mRequest.getPlmns().size() <= 20;
    }

    private void notifyMessenger(NetworkScanRequestInfo networkScanRequestInfo, int i, int i2, List<CellInfo> list) {
        Messenger messenger = networkScanRequestInfo.mMessenger;
        Message messageObtain = Message.obtain();
        messageObtain.what = i;
        messageObtain.arg1 = i2;
        messageObtain.arg2 = networkScanRequestInfo.mScanId;
        if (list != null) {
            CellInfo[] cellInfoArr = (CellInfo[]) list.toArray(new CellInfo[list.size()]);
            Bundle bundle = new Bundle();
            bundle.putParcelableArray("scanResult", cellInfoArr);
            messageObtain.setData(bundle);
        } else {
            messageObtain.obj = null;
        }
        try {
            messenger.send(messageObtain);
        } catch (RemoteException e) {
            Log.e(TAG, "Exception in notifyMessenger: " + e);
        }
    }

    class NetworkScanRequestInfo implements IBinder.DeathRecipient {
        private final IBinder mBinder;
        private final Messenger mMessenger;
        private final Phone mPhone;
        private final NetworkScanRequest mRequest;
        private final int mScanId;
        private final int mUid = Binder.getCallingUid();
        private final int mPid = Binder.getCallingPid();
        private boolean mIsBinderDead = false;

        NetworkScanRequestInfo(NetworkScanRequest networkScanRequest, Messenger messenger, IBinder iBinder, int i, Phone phone) {
            this.mRequest = networkScanRequest;
            this.mMessenger = messenger;
            this.mBinder = iBinder;
            this.mScanId = i;
            this.mPhone = phone;
            try {
                this.mBinder.linkToDeath(this, 0);
            } catch (RemoteException e) {
                binderDied();
            }
        }

        synchronized void setIsBinderDead(boolean z) {
            this.mIsBinderDead = z;
        }

        synchronized boolean getIsBinderDead() {
            return this.mIsBinderDead;
        }

        NetworkScanRequest getRequest() {
            return this.mRequest;
        }

        void unlinkDeathRecipient() {
            if (this.mBinder != null) {
                this.mBinder.unlinkToDeath(this, 0);
            }
        }

        @Override
        public void binderDied() {
            Log.e(NetworkScanRequestTracker.TAG, "PhoneInterfaceManager NetworkScanRequestInfo binderDied(" + this.mRequest + ", " + this.mBinder + ")");
            setIsBinderDead(true);
            NetworkScanRequestTracker.this.interruptNetworkScan(this.mScanId);
        }
    }

    private class NetworkScanRequestScheduler {
        private NetworkScanRequestInfo mLiveRequestInfo;
        private NetworkScanRequestInfo mPendingRequestInfo;

        private NetworkScanRequestScheduler() {
        }

        private int rilErrorToScanError(int i) {
            switch (i) {
                case 0:
                    return 0;
                case 1:
                    Log.e(NetworkScanRequestTracker.TAG, "rilErrorToScanError: RADIO_NOT_AVAILABLE");
                    return 1;
                case 6:
                    Log.e(NetworkScanRequestTracker.TAG, "rilErrorToScanError: REQUEST_NOT_SUPPORTED");
                    return 4;
                case 37:
                    Log.e(NetworkScanRequestTracker.TAG, "rilErrorToScanError: NO_MEMORY");
                    return 1;
                case 38:
                    Log.e(NetworkScanRequestTracker.TAG, "rilErrorToScanError: INTERNAL_ERR");
                    return 1;
                case 40:
                    Log.e(NetworkScanRequestTracker.TAG, "rilErrorToScanError: MODEM_ERR");
                    return 1;
                case 44:
                    Log.e(NetworkScanRequestTracker.TAG, "rilErrorToScanError: INVALID_ARGUMENTS");
                    return 2;
                case 54:
                    Log.e(NetworkScanRequestTracker.TAG, "rilErrorToScanError: OPERATION_NOT_ALLOWED");
                    return 1;
                case 64:
                    Log.e(NetworkScanRequestTracker.TAG, "rilErrorToScanError: DEVICE_IN_USE");
                    return 3;
                default:
                    Log.e(NetworkScanRequestTracker.TAG, "rilErrorToScanError: Unexpected RadioError " + i);
                    return 10000;
            }
        }

        private int commandExceptionErrorToScanError(CommandException.Error error) {
            switch (error) {
                case RADIO_NOT_AVAILABLE:
                    Log.e(NetworkScanRequestTracker.TAG, "commandExceptionErrorToScanError: RADIO_NOT_AVAILABLE");
                    return 1;
                case REQUEST_NOT_SUPPORTED:
                    Log.e(NetworkScanRequestTracker.TAG, "commandExceptionErrorToScanError: REQUEST_NOT_SUPPORTED");
                    return 4;
                case NO_MEMORY:
                    Log.e(NetworkScanRequestTracker.TAG, "commandExceptionErrorToScanError: NO_MEMORY");
                    return 1;
                case INTERNAL_ERR:
                    Log.e(NetworkScanRequestTracker.TAG, "commandExceptionErrorToScanError: INTERNAL_ERR");
                    return 1;
                case MODEM_ERR:
                    Log.e(NetworkScanRequestTracker.TAG, "commandExceptionErrorToScanError: MODEM_ERR");
                    return 1;
                case OPERATION_NOT_ALLOWED:
                    Log.e(NetworkScanRequestTracker.TAG, "commandExceptionErrorToScanError: OPERATION_NOT_ALLOWED");
                    return 1;
                case INVALID_ARGUMENTS:
                    Log.e(NetworkScanRequestTracker.TAG, "commandExceptionErrorToScanError: INVALID_ARGUMENTS");
                    return 2;
                case DEVICE_IN_USE:
                    Log.e(NetworkScanRequestTracker.TAG, "commandExceptionErrorToScanError: DEVICE_IN_USE");
                    return 3;
                default:
                    Log.e(NetworkScanRequestTracker.TAG, "commandExceptionErrorToScanError: Unexpected CommandExceptionError " + error);
                    return 10000;
            }
        }

        private void doStartScan(NetworkScanRequestInfo networkScanRequestInfo) {
            if (networkScanRequestInfo != null) {
                if (!NetworkScanRequestTracker.this.isValidScan(networkScanRequestInfo)) {
                    NetworkScanRequestTracker.this.notifyMessenger(networkScanRequestInfo, 2, 2, null);
                    return;
                }
                if (networkScanRequestInfo.getIsBinderDead()) {
                    Log.e(NetworkScanRequestTracker.TAG, "CMD_START_NETWORK_SCAN: Binder has died");
                    return;
                } else {
                    if (!startNewScan(networkScanRequestInfo) && !interruptLiveScan(networkScanRequestInfo) && !cacheScan(networkScanRequestInfo)) {
                        NetworkScanRequestTracker.this.notifyMessenger(networkScanRequestInfo, 2, 3, null);
                        return;
                    }
                    return;
                }
            }
            Log.e(NetworkScanRequestTracker.TAG, "CMD_START_NETWORK_SCAN: nsri is null");
        }

        private synchronized void startScanDone(AsyncResult asyncResult) {
            NetworkScanRequestInfo networkScanRequestInfo = (NetworkScanRequestInfo) asyncResult.userObj;
            if (networkScanRequestInfo == null) {
                Log.e(NetworkScanRequestTracker.TAG, "EVENT_START_NETWORK_SCAN_DONE: nsri is null");
                return;
            }
            if (this.mLiveRequestInfo != null && networkScanRequestInfo.mScanId == this.mLiveRequestInfo.mScanId) {
                if (asyncResult.exception != null || asyncResult.result == null) {
                    NetworkScanRequestTracker.this.logEmptyResultOrException(asyncResult);
                    if (asyncResult.exception != null) {
                        deleteScanAndMayNotify(networkScanRequestInfo, commandExceptionErrorToScanError(((CommandException) asyncResult.exception).getCommandError()), true);
                    } else {
                        Log.wtf(NetworkScanRequestTracker.TAG, "EVENT_START_NETWORK_SCAN_DONE: ar.exception can not be null!");
                    }
                } else {
                    networkScanRequestInfo.mPhone.mCi.registerForNetworkScanResult(NetworkScanRequestTracker.this.mHandler, 3, networkScanRequestInfo);
                }
                return;
            }
            Log.e(NetworkScanRequestTracker.TAG, "EVENT_START_NETWORK_SCAN_DONE: nsri does not match mLiveRequestInfo");
        }

        private void receiveResult(AsyncResult asyncResult) {
            NetworkScanRequestInfo networkScanRequestInfo = (NetworkScanRequestInfo) asyncResult.userObj;
            if (networkScanRequestInfo == null) {
                Log.e(NetworkScanRequestTracker.TAG, "EVENT_RECEIVE_NETWORK_SCAN_RESULT: nsri is null");
                return;
            }
            if (asyncResult.exception != null || asyncResult.result == null) {
                NetworkScanRequestTracker.this.logEmptyResultOrException(asyncResult);
                deleteScanAndMayNotify(networkScanRequestInfo, 10000, true);
                networkScanRequestInfo.mPhone.mCi.unregisterForNetworkScanResult(NetworkScanRequestTracker.this.mHandler);
                return;
            }
            NetworkScanResult networkScanResult = (NetworkScanResult) asyncResult.result;
            if (networkScanResult.scanError == 0) {
                NetworkScanRequestTracker.this.notifyMessenger(networkScanRequestInfo, 1, rilErrorToScanError(networkScanResult.scanError), networkScanResult.networkInfos);
                if (networkScanResult.scanStatus == 2) {
                    deleteScanAndMayNotify(networkScanRequestInfo, 0, true);
                    networkScanRequestInfo.mPhone.mCi.unregisterForNetworkScanResult(NetworkScanRequestTracker.this.mHandler);
                    return;
                }
                return;
            }
            if (networkScanResult.networkInfos != null) {
                NetworkScanRequestTracker.this.notifyMessenger(networkScanRequestInfo, 1, 0, networkScanResult.networkInfos);
            }
            deleteScanAndMayNotify(networkScanRequestInfo, rilErrorToScanError(networkScanResult.scanError), true);
            networkScanRequestInfo.mPhone.mCi.unregisterForNetworkScanResult(NetworkScanRequestTracker.this.mHandler);
        }

        private synchronized void doStopScan(int i) {
            if (this.mLiveRequestInfo != null && i == this.mLiveRequestInfo.mScanId) {
                this.mLiveRequestInfo.mPhone.stopNetworkScan(NetworkScanRequestTracker.this.mHandler.obtainMessage(5, this.mLiveRequestInfo));
            } else if (this.mPendingRequestInfo != null && i == this.mPendingRequestInfo.mScanId) {
                NetworkScanRequestTracker.this.notifyMessenger(this.mPendingRequestInfo, 3, 0, null);
                this.mPendingRequestInfo = null;
            } else {
                Log.e(NetworkScanRequestTracker.TAG, "stopScan: scan " + i + " does not exist!");
            }
        }

        private void stopScanDone(AsyncResult asyncResult) {
            NetworkScanRequestInfo networkScanRequestInfo = (NetworkScanRequestInfo) asyncResult.userObj;
            if (networkScanRequestInfo == null) {
                Log.e(NetworkScanRequestTracker.TAG, "EVENT_STOP_NETWORK_SCAN_DONE: nsri is null");
                return;
            }
            if (asyncResult.exception != null || asyncResult.result == null) {
                NetworkScanRequestTracker.this.logEmptyResultOrException(asyncResult);
                if (asyncResult.exception != null) {
                    deleteScanAndMayNotify(networkScanRequestInfo, commandExceptionErrorToScanError(((CommandException) asyncResult.exception).getCommandError()), true);
                } else {
                    Log.wtf(NetworkScanRequestTracker.TAG, "EVENT_STOP_NETWORK_SCAN_DONE: ar.exception can not be null!");
                }
            } else {
                deleteScanAndMayNotify(networkScanRequestInfo, 0, true);
            }
            networkScanRequestInfo.mPhone.mCi.unregisterForNetworkScanResult(NetworkScanRequestTracker.this.mHandler);
        }

        private synchronized void doInterruptScan(int i) {
            if (this.mLiveRequestInfo != null && i == this.mLiveRequestInfo.mScanId) {
                this.mLiveRequestInfo.mPhone.stopNetworkScan(NetworkScanRequestTracker.this.mHandler.obtainMessage(7, this.mLiveRequestInfo));
            } else {
                Log.e(NetworkScanRequestTracker.TAG, "doInterruptScan: scan " + i + " does not exist!");
            }
        }

        private void interruptScanDone(AsyncResult asyncResult) {
            NetworkScanRequestInfo networkScanRequestInfo = (NetworkScanRequestInfo) asyncResult.userObj;
            if (networkScanRequestInfo != null) {
                networkScanRequestInfo.mPhone.mCi.unregisterForNetworkScanResult(NetworkScanRequestTracker.this.mHandler);
                deleteScanAndMayNotify(networkScanRequestInfo, 0, false);
            } else {
                Log.e(NetworkScanRequestTracker.TAG, "EVENT_INTERRUPT_NETWORK_SCAN_DONE: nsri is null");
            }
        }

        private synchronized boolean interruptLiveScan(NetworkScanRequestInfo networkScanRequestInfo) {
            if (this.mLiveRequestInfo != null && this.mPendingRequestInfo == null && networkScanRequestInfo.mUid == 1001 && this.mLiveRequestInfo.mUid != 1001) {
                doInterruptScan(this.mLiveRequestInfo.mScanId);
                this.mPendingRequestInfo = networkScanRequestInfo;
                NetworkScanRequestTracker.this.notifyMessenger(this.mLiveRequestInfo, 2, 10002, null);
                return true;
            }
            return false;
        }

        private boolean cacheScan(NetworkScanRequestInfo networkScanRequestInfo) {
            return false;
        }

        private synchronized boolean startNewScan(NetworkScanRequestInfo networkScanRequestInfo) {
            if (this.mLiveRequestInfo == null) {
                this.mLiveRequestInfo = networkScanRequestInfo;
                networkScanRequestInfo.mPhone.startNetworkScan(networkScanRequestInfo.getRequest(), NetworkScanRequestTracker.this.mHandler.obtainMessage(2, networkScanRequestInfo));
                return true;
            }
            return false;
        }

        private synchronized void deleteScanAndMayNotify(NetworkScanRequestInfo networkScanRequestInfo, int i, boolean z) {
            if (this.mLiveRequestInfo != null && networkScanRequestInfo.mScanId == this.mLiveRequestInfo.mScanId) {
                if (z) {
                    if (i == 0) {
                        NetworkScanRequestTracker.this.notifyMessenger(networkScanRequestInfo, 3, i, null);
                    } else {
                        NetworkScanRequestTracker.this.notifyMessenger(networkScanRequestInfo, 2, i, null);
                    }
                }
                this.mLiveRequestInfo = null;
                if (this.mPendingRequestInfo != null) {
                    startNewScan(this.mPendingRequestInfo);
                    this.mPendingRequestInfo = null;
                }
            }
        }
    }

    private void interruptNetworkScan(int i) {
        this.mHandler.obtainMessage(6, i, 0).sendToTarget();
    }

    public int startNetworkScan(NetworkScanRequest networkScanRequest, Messenger messenger, IBinder iBinder, Phone phone) {
        int andIncrement = this.mNextNetworkScanRequestId.getAndIncrement();
        this.mHandler.obtainMessage(1, new NetworkScanRequestInfo(networkScanRequest, messenger, iBinder, andIncrement, phone)).sendToTarget();
        return andIncrement;
    }

    public void stopNetworkScan(int i) {
        synchronized (this.mScheduler) {
            if ((this.mScheduler.mLiveRequestInfo != null && i == this.mScheduler.mLiveRequestInfo.mScanId && Binder.getCallingUid() == this.mScheduler.mLiveRequestInfo.mUid) || (this.mScheduler.mPendingRequestInfo != null && i == this.mScheduler.mPendingRequestInfo.mScanId && Binder.getCallingUid() == this.mScheduler.mPendingRequestInfo.mUid)) {
                this.mHandler.obtainMessage(4, i, 0).sendToTarget();
            } else {
                throw new IllegalArgumentException("Scan with id: " + i + " does not exist!");
            }
        }
    }
}
