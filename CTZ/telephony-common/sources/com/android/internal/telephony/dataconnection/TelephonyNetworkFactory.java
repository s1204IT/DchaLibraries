package com.android.internal.telephony.dataconnection;

import android.content.Context;
import android.net.NetworkCapabilities;
import android.net.NetworkFactory;
import android.net.NetworkRequest;
import android.net.StringNetworkSpecifier;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.Rlog;
import android.util.LocalLog;
import com.android.internal.telephony.PhoneSwitcher;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.SubscriptionMonitor;
import com.android.internal.util.IndentingPrintWriter;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashMap;

public class TelephonyNetworkFactory extends NetworkFactory {
    protected static final boolean DBG = true;
    private static final int EVENT_ACTIVE_PHONE_SWITCH = 1;
    private static final int EVENT_DEFAULT_SUBSCRIPTION_CHANGED = 3;
    private static final int EVENT_NETWORK_RELEASE = 5;
    private static final int EVENT_NETWORK_REQUEST = 4;
    private static final int EVENT_SUBSCRIPTION_CHANGED = 2;
    protected static final boolean RELEASE = false;
    protected static final boolean REQUEST = true;
    protected static final int REQUEST_LOG_SIZE = 40;
    private static final int TELEPHONY_NETWORK_SCORE = 50;
    public final String LOG_TAG;
    protected final DcTracker mDcTracker;
    protected final HashMap<NetworkRequest, LocalLog> mDefaultRequests;
    private final Handler mInternalHandler;
    protected boolean mIsActive;
    protected boolean mIsDefault;
    private int mPhoneId;
    private final PhoneSwitcher mPhoneSwitcher;
    protected final HashMap<NetworkRequest, LocalLog> mSpecificRequests;
    private final SubscriptionController mSubscriptionController;
    private int mSubscriptionId;
    private final SubscriptionMonitor mSubscriptionMonitor;

    public TelephonyNetworkFactory(PhoneSwitcher phoneSwitcher, SubscriptionController subscriptionController, SubscriptionMonitor subscriptionMonitor, Looper looper, Context context, int i, DcTracker dcTracker) {
        super(looper, context, "TelephonyNetworkFactory[" + i + "]", (NetworkCapabilities) null);
        this.mDefaultRequests = new HashMap<>();
        this.mSpecificRequests = new HashMap<>();
        this.mInternalHandler = new InternalHandler(looper);
        setCapabilityFilter(makeNetworkFilter(subscriptionController, i));
        setScoreFilter(50);
        this.mPhoneSwitcher = phoneSwitcher;
        this.mSubscriptionController = subscriptionController;
        this.mSubscriptionMonitor = subscriptionMonitor;
        this.mPhoneId = i;
        this.LOG_TAG = "TelephonyNetworkFactory[" + i + "]";
        this.mDcTracker = dcTracker;
        this.mIsActive = false;
        this.mPhoneSwitcher.registerForActivePhoneSwitch(this.mPhoneId, this.mInternalHandler, 1, null);
        this.mSubscriptionId = -1;
        this.mSubscriptionMonitor.registerForSubscriptionChanged(this.mPhoneId, this.mInternalHandler, 2, null);
        this.mIsDefault = false;
        this.mSubscriptionMonitor.registerForDefaultDataSubscriptionChanged(this.mPhoneId, this.mInternalHandler, 3, null);
        register();
    }

    private NetworkCapabilities makeNetworkFilter(SubscriptionController subscriptionController, int i) {
        return makeNetworkFilter(subscriptionController.getSubIdUsingPhoneId(i));
    }

    protected NetworkCapabilities makeNetworkFilter(int i) {
        NetworkCapabilities networkCapabilities = new NetworkCapabilities();
        networkCapabilities.addTransportType(0);
        networkCapabilities.addCapability(0);
        networkCapabilities.addCapability(1);
        networkCapabilities.addCapability(2);
        networkCapabilities.addCapability(3);
        networkCapabilities.addCapability(4);
        networkCapabilities.addCapability(5);
        networkCapabilities.addCapability(7);
        networkCapabilities.addCapability(8);
        networkCapabilities.addCapability(9);
        networkCapabilities.addCapability(10);
        networkCapabilities.addCapability(13);
        networkCapabilities.addCapability(12);
        networkCapabilities.setNetworkSpecifier(new StringNetworkSpecifier(String.valueOf(i)));
        return networkCapabilities;
    }

    private class InternalHandler extends Handler {
        public InternalHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    TelephonyNetworkFactory.this.onActivePhoneSwitch();
                    break;
                case 2:
                    TelephonyNetworkFactory.this.onSubIdChange();
                    break;
                case 3:
                    TelephonyNetworkFactory.this.onDefaultChange();
                    break;
                case 4:
                    TelephonyNetworkFactory.this.onNeedNetworkFor(message);
                    break;
                case 5:
                    TelephonyNetworkFactory.this.onReleaseNetworkFor(message);
                    break;
            }
        }
    }

    protected void applyRequests(HashMap<NetworkRequest, LocalLog> map, boolean z, String str) {
        for (NetworkRequest networkRequest : map.keySet()) {
            if (!ignoreCapabilityCheck(networkRequest.networkCapabilities, z)) {
                LocalLog localLog = map.get(networkRequest);
                localLog.log(str);
                if (z) {
                    this.mDcTracker.requestNetwork(networkRequest, localLog);
                } else {
                    this.mDcTracker.releaseNetwork(networkRequest, localLog);
                }
            }
        }
    }

    private void onActivePhoneSwitch() {
        boolean zIsPhoneActive = this.mPhoneSwitcher.isPhoneActive(this.mPhoneId);
        if (this.mIsActive != zIsPhoneActive) {
            this.mIsActive = zIsPhoneActive;
            String str = "onActivePhoneSwitch(" + this.mIsActive + ", " + this.mIsDefault + ")";
            log(str);
            if (this.mIsDefault) {
                applyRequests(this.mDefaultRequests, this.mIsActive, str);
            }
            applyRequests(this.mSpecificRequests, this.mIsActive, str);
        }
    }

    private void onSubIdChange() {
        int subIdUsingPhoneId = this.mSubscriptionController.getSubIdUsingPhoneId(this.mPhoneId);
        if (this.mSubscriptionId != subIdUsingPhoneId) {
            log("onSubIdChange " + this.mSubscriptionId + "->" + subIdUsingPhoneId);
            this.mSubscriptionId = subIdUsingPhoneId;
            setCapabilityFilter(makeNetworkFilter(this.mSubscriptionId));
        }
    }

    private void onDefaultChange() {
        boolean z = this.mSubscriptionController.getDefaultDataSubId() == this.mSubscriptionId;
        if (z != this.mIsDefault) {
            this.mIsDefault = z;
            String str = "onDefaultChange(" + this.mIsActive + "," + this.mIsDefault + ")";
            log(str);
            if (this.mIsActive) {
                applyRequests(this.mDefaultRequests, this.mIsDefault, str);
            }
        }
    }

    public void needNetworkFor(NetworkRequest networkRequest, int i) {
        Message messageObtainMessage = this.mInternalHandler.obtainMessage(4);
        messageObtainMessage.obj = networkRequest;
        messageObtainMessage.sendToTarget();
    }

    protected void onNeedNetworkFor(Message message) {
        LocalLog localLog;
        NetworkRequest networkRequest = (NetworkRequest) message.obj;
        boolean z = false;
        if (networkRequest.networkCapabilities.getNetworkSpecifier() == null) {
            localLog = this.mDefaultRequests.get(networkRequest);
            if (localLog == null) {
                localLog = new LocalLog(40);
                localLog.log("created for " + networkRequest);
                this.mDefaultRequests.put(networkRequest, localLog);
                z = this.mIsDefault;
            }
        } else {
            localLog = this.mSpecificRequests.get(networkRequest);
            if (localLog == null) {
                localLog = new LocalLog(40);
                this.mSpecificRequests.put(networkRequest, localLog);
                z = true;
            }
        }
        if ((this.mIsActive && z) || ignoreCapabilityCheck(networkRequest.networkCapabilities, true)) {
            localLog.log("onNeedNetworkFor");
            log("onNeedNetworkFor " + networkRequest);
            this.mDcTracker.requestNetwork(networkRequest, localLog);
            return;
        }
        String str = "not acting - isApp=" + z + ", isAct=" + this.mIsActive;
        localLog.log(str);
        log(str + " " + networkRequest);
    }

    public void releaseNetworkFor(NetworkRequest networkRequest) {
        Message messageObtainMessage = this.mInternalHandler.obtainMessage(5);
        messageObtainMessage.obj = networkRequest;
        messageObtainMessage.sendToTarget();
    }

    protected void onReleaseNetworkFor(Message message) {
        LocalLog localLogRemove;
        NetworkRequest networkRequest = (NetworkRequest) message.obj;
        boolean z = true;
        if (networkRequest.networkCapabilities.getNetworkSpecifier() != null ? (localLogRemove = this.mSpecificRequests.remove(networkRequest)) == null : (localLogRemove = this.mDefaultRequests.remove(networkRequest)) == null || !this.mIsDefault) {
            z = false;
        }
        if ((this.mIsActive && z) || ignoreCapabilityCheck(networkRequest.networkCapabilities, false)) {
            localLogRemove.log("onReleaseNetworkFor");
            log("onReleaseNetworkFor " + networkRequest);
            this.mDcTracker.releaseNetwork(networkRequest, localLogRemove);
            return;
        }
        String str = "not releasing - isApp=" + z + ", isAct=" + this.mIsActive;
        localLogRemove.log(str);
        log(str + " " + networkRequest);
    }

    protected void log(String str) {
        Rlog.d(this.LOG_TAG, str);
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        IndentingPrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "  ");
        indentingPrintWriter.println(this.LOG_TAG + " mSubId=" + this.mSubscriptionId + " mIsActive=" + this.mIsActive + " mIsDefault=" + this.mIsDefault);
        indentingPrintWriter.println("Default Requests:");
        indentingPrintWriter.increaseIndent();
        for (NetworkRequest networkRequest : this.mDefaultRequests.keySet()) {
            indentingPrintWriter.println(networkRequest);
            indentingPrintWriter.increaseIndent();
            this.mDefaultRequests.get(networkRequest).dump(fileDescriptor, indentingPrintWriter, strArr);
            indentingPrintWriter.decreaseIndent();
        }
        indentingPrintWriter.decreaseIndent();
    }

    protected boolean ignoreCapabilityCheck(NetworkCapabilities networkCapabilities, boolean z) {
        return false;
    }
}
