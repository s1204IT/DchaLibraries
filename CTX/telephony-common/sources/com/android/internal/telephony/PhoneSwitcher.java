package com.android.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.MatchAllNetworkSpecifier;
import android.net.NetworkCapabilities;
import android.net.NetworkFactory;
import android.net.NetworkRequest;
import android.net.StringNetworkSpecifier;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.RemoteException;
import android.telephony.Rlog;
import android.util.LocalLog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.IOnSubscriptionsChangedListener;
import com.android.internal.telephony.dataconnection.DcRequest;
import com.android.internal.util.IndentingPrintWriter;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class PhoneSwitcher extends Handler {
    private static final int EVENT_DEFAULT_SUBSCRIPTION_CHANGED = 101;
    private static final int EVENT_EMERGENCY_TOGGLE = 105;
    private static final int EVENT_RELEASE_NETWORK = 104;
    private static final int EVENT_REQUEST_NETWORK = 103;
    private static final int EVENT_RESEND_DATA_ALLOWED = 106;
    private static final int EVENT_SUBSCRIPTION_CHANGED = 102;
    private static final String LOG_TAG = "PhoneSwitcher";
    private static final int MAX_LOCAL_LOG_LINES = 30;
    protected static final boolean REQUESTS_CHANGED = true;
    protected static final boolean REQUESTS_UNCHANGED = false;
    private static final boolean VDBG = false;
    private final RegistrantList[] mActivePhoneRegistrants;
    private final CommandsInterface[] mCommandsInterfaces;
    protected final Context mContext;
    private final BroadcastReceiver mDefaultDataChangedReceiver;
    protected int mDefaultDataSubscription;
    private final LocalLog mLocalLog;
    protected int mMaxActivePhones;
    protected final int mNumPhones;
    private final PhoneState[] mPhoneStates;
    protected final int[] mPhoneSubscriptions;
    protected final Phone[] mPhones;
    protected final List<DcRequest> mPrioritizedDcRequests;
    protected final SubscriptionController mSubscriptionController;
    private final IOnSubscriptionsChangedListener mSubscriptionsChangedListener;

    @VisibleForTesting
    public PhoneSwitcher(Looper looper) {
        super(looper);
        this.mPrioritizedDcRequests = new ArrayList();
        this.mDefaultDataChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                PhoneSwitcher.this.obtainMessage(101).sendToTarget();
            }
        };
        this.mSubscriptionsChangedListener = new IOnSubscriptionsChangedListener.Stub() {
            public void onSubscriptionsChanged() {
                PhoneSwitcher.this.obtainMessage(102).sendToTarget();
            }
        };
        this.mMaxActivePhones = 0;
        this.mSubscriptionController = null;
        this.mPhoneSubscriptions = null;
        this.mCommandsInterfaces = null;
        this.mContext = null;
        this.mPhoneStates = null;
        this.mPhones = null;
        this.mLocalLog = null;
        this.mActivePhoneRegistrants = null;
        this.mNumPhones = 0;
    }

    public PhoneSwitcher(int i, int i2, Context context, SubscriptionController subscriptionController, Looper looper, ITelephonyRegistry iTelephonyRegistry, CommandsInterface[] commandsInterfaceArr, Phone[] phoneArr) {
        super(looper);
        this.mPrioritizedDcRequests = new ArrayList();
        this.mDefaultDataChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                PhoneSwitcher.this.obtainMessage(101).sendToTarget();
            }
        };
        this.mSubscriptionsChangedListener = new IOnSubscriptionsChangedListener.Stub() {
            public void onSubscriptionsChanged() {
                PhoneSwitcher.this.obtainMessage(102).sendToTarget();
            }
        };
        this.mContext = context;
        this.mNumPhones = i2;
        this.mPhones = phoneArr;
        this.mPhoneSubscriptions = new int[i2];
        this.mMaxActivePhones = i;
        this.mLocalLog = new LocalLog(30);
        this.mSubscriptionController = subscriptionController;
        this.mActivePhoneRegistrants = new RegistrantList[i2];
        this.mPhoneStates = new PhoneState[i2];
        for (int i3 = 0; i3 < i2; i3++) {
            this.mActivePhoneRegistrants[i3] = new RegistrantList();
            this.mPhoneStates[i3] = new PhoneState();
            if (this.mPhones[i3] != null) {
                this.mPhones[i3].registerForEmergencyCallToggle(this, EVENT_EMERGENCY_TOGGLE, null);
            }
        }
        this.mCommandsInterfaces = commandsInterfaceArr;
        try {
            iTelephonyRegistry.addOnSubscriptionsChangedListener(context.getOpPackageName(), this.mSubscriptionsChangedListener);
        } catch (RemoteException e) {
        }
        this.mContext.registerReceiver(this.mDefaultDataChangedReceiver, new IntentFilter("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED"));
        PhoneSwitcherNetworkRequestListener phoneSwitcherNetworkRequestListener = new PhoneSwitcherNetworkRequestListener(looper, context, makeNetworkFilter(), this);
        phoneSwitcherNetworkRequestListener.setScoreFilter(101);
        phoneSwitcherNetworkRequestListener.register();
        log("PhoneSwitcher started");
    }

    protected NetworkCapabilities makeNetworkFilter() {
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
        networkCapabilities.setNetworkSpecifier(new MatchAllNetworkSpecifier());
        return networkCapabilities;
    }

    @Override
    public void handleMessage(Message message) {
        switch (message.what) {
            case 101:
                onEvaluate(false, "defaultChanged");
                break;
            case 102:
                onEvaluate(false, "subChanged");
                break;
            case EVENT_REQUEST_NETWORK:
                onRequestNetwork((NetworkRequest) message.obj);
                break;
            case EVENT_RELEASE_NETWORK:
                onReleaseNetwork((NetworkRequest) message.obj);
                break;
            case EVENT_EMERGENCY_TOGGLE:
                onEvaluate(true, "emergencyToggle");
                break;
            case 106:
                onResendDataAllowed(message);
                break;
        }
    }

    protected boolean isEmergency() {
        for (Phone phone : this.mPhones) {
            if (phone != null && (phone.isInEcm() || phone.isInEmergencyCall())) {
                return true;
            }
        }
        return false;
    }

    private static class PhoneSwitcherNetworkRequestListener extends NetworkFactory {
        private final PhoneSwitcher mPhoneSwitcher;

        public PhoneSwitcherNetworkRequestListener(Looper looper, Context context, NetworkCapabilities networkCapabilities, PhoneSwitcher phoneSwitcher) {
            super(looper, context, "PhoneSwitcherNetworkRequstListener", networkCapabilities);
            this.mPhoneSwitcher = phoneSwitcher;
        }

        protected void needNetworkFor(NetworkRequest networkRequest, int i) {
            Message messageObtainMessage = this.mPhoneSwitcher.obtainMessage(PhoneSwitcher.EVENT_REQUEST_NETWORK);
            messageObtainMessage.obj = networkRequest;
            messageObtainMessage.sendToTarget();
        }

        protected void releaseNetworkFor(NetworkRequest networkRequest) {
            Message messageObtainMessage = this.mPhoneSwitcher.obtainMessage(PhoneSwitcher.EVENT_RELEASE_NETWORK);
            messageObtainMessage.obj = networkRequest;
            messageObtainMessage.sendToTarget();
        }

        public boolean acceptRequest(NetworkRequest networkRequest, int i) {
            Method method;
            try {
                method = Class.forName("com.mediatek.internal.telephony.MtkPhoneSwitcher").getMethod("acceptRequest", NetworkRequest.class, Integer.TYPE);
            } catch (Exception e) {
                Rlog.e(PhoneSwitcher.LOG_TAG, "createInstance:got exception for acceptRequest " + e);
            }
            if (method != null) {
                return ((Boolean) method.invoke(null, networkRequest, Integer.valueOf(i))).booleanValue();
            }
            Rlog.e(PhoneSwitcher.LOG_TAG, "acceptRequest is null!");
            return super.acceptRequest(networkRequest, i);
        }
    }

    protected void onRequestNetwork(NetworkRequest networkRequest) {
        DcRequest dcRequestMakeDcRequest = TelephonyComponentFactory.getInstance().makeDcRequest(networkRequest, this.mContext);
        if (!this.mPrioritizedDcRequests.contains(dcRequestMakeDcRequest)) {
            this.mPrioritizedDcRequests.add(dcRequestMakeDcRequest);
            Collections.sort(this.mPrioritizedDcRequests);
            onEvaluate(true, "netRequest");
        }
    }

    protected void onReleaseNetwork(NetworkRequest networkRequest) {
        if (this.mPrioritizedDcRequests.remove(TelephonyComponentFactory.getInstance().makeDcRequest(networkRequest, this.mContext))) {
            onEvaluate(true, "netReleased");
        }
    }

    protected void onEvaluate(boolean z, String str) {
        StringBuilder sb = new StringBuilder(str);
        if (isEmergency()) {
            log("onEvalute aborted due to Emergency");
            return;
        }
        int defaultDataSubId = this.mSubscriptionController.getDefaultDataSubId();
        if (defaultDataSubId != this.mDefaultDataSubscription) {
            sb.append(" default ");
            sb.append(this.mDefaultDataSubscription);
            sb.append("->");
            sb.append(defaultDataSubId);
            this.mDefaultDataSubscription = defaultDataSubId;
            z = true;
        }
        boolean z2 = z;
        for (int i = 0; i < this.mNumPhones; i++) {
            int subIdUsingPhoneId = this.mSubscriptionController.getSubIdUsingPhoneId(i);
            if (subIdUsingPhoneId != this.mPhoneSubscriptions[i]) {
                sb.append(" phone[");
                sb.append(i);
                sb.append("] ");
                sb.append(this.mPhoneSubscriptions[i]);
                sb.append("->");
                sb.append(subIdUsingPhoneId);
                this.mPhoneSubscriptions[i] = subIdUsingPhoneId;
                z2 = true;
            }
        }
        if (z2) {
            log("evaluating due to " + sb.toString());
            ArrayList arrayList = new ArrayList();
            Iterator<DcRequest> it = this.mPrioritizedDcRequests.iterator();
            while (it.hasNext()) {
                int iPhoneIdForRequest = phoneIdForRequest(it.next().networkRequest);
                if (iPhoneIdForRequest != -1 && !arrayList.contains(Integer.valueOf(iPhoneIdForRequest))) {
                    arrayList.add(Integer.valueOf(iPhoneIdForRequest));
                    if (arrayList.size() >= this.mMaxActivePhones) {
                        break;
                    }
                }
            }
            suggestDefaultActivePhone(arrayList);
            for (int i2 = 0; i2 < this.mNumPhones; i2++) {
                if (!arrayList.contains(Integer.valueOf(i2))) {
                    deactivate(i2);
                }
            }
            Iterator<Integer> it2 = arrayList.iterator();
            while (it2.hasNext()) {
                activate(it2.next().intValue());
            }
        }
    }

    private static class PhoneState {
        public volatile boolean active;
        public long lastRequested;

        private PhoneState() {
            this.active = false;
            this.lastRequested = 0L;
        }
    }

    protected void deactivate(int i) {
        PhoneState phoneState = this.mPhoneStates[i];
        if (phoneState.active) {
            phoneState.active = false;
            log("deactivate " + i);
            phoneState.lastRequested = System.currentTimeMillis();
            if (this.mNumPhones > 1) {
                this.mCommandsInterfaces[i].setDataAllowed(false, null);
            }
            this.mActivePhoneRegistrants[i].notifyRegistrants();
        }
    }

    protected void activate(int i) {
        PhoneState phoneState = this.mPhoneStates[i];
        if (phoneState.active) {
            return;
        }
        phoneState.active = true;
        log("activate " + i);
        phoneState.lastRequested = System.currentTimeMillis();
        if (this.mNumPhones > 1) {
            this.mCommandsInterfaces[i].setDataAllowed(true, null);
        }
        this.mActivePhoneRegistrants[i].notifyRegistrants();
    }

    public void resendDataAllowed(int i) {
        validatePhoneId(i);
        Message messageObtainMessage = obtainMessage(106);
        messageObtainMessage.arg1 = i;
        messageObtainMessage.sendToTarget();
    }

    private void onResendDataAllowed(Message message) {
        int i = message.arg1;
        if (this.mNumPhones > 1) {
            this.mCommandsInterfaces[i].setDataAllowed(this.mPhoneStates[i].active, null);
        }
    }

    protected int phoneIdForRequest(NetworkRequest networkRequest) {
        int i;
        StringNetworkSpecifier networkSpecifier = networkRequest.networkCapabilities.getNetworkSpecifier();
        if (networkSpecifier == null) {
            i = this.mDefaultDataSubscription;
        } else if (networkSpecifier instanceof StringNetworkSpecifier) {
            try {
                i = Integer.parseInt(networkSpecifier.specifier);
            } catch (NumberFormatException e) {
                Rlog.e(LOG_TAG, "NumberFormatException on " + networkSpecifier.specifier);
                i = -1;
            }
        } else {
            i = -1;
        }
        if (i == -1) {
            return -1;
        }
        for (int i2 = 0; i2 < this.mNumPhones; i2++) {
            if (this.mPhoneSubscriptions[i2] == i) {
                return i2;
            }
        }
        return -1;
    }

    public boolean isPhoneActive(int i) {
        validatePhoneId(i);
        return this.mPhoneStates[i].active;
    }

    public void registerForActivePhoneSwitch(int i, Handler handler, int i2, Object obj) {
        validatePhoneId(i);
        Registrant registrant = new Registrant(handler, i2, obj);
        this.mActivePhoneRegistrants[i].add(registrant);
        registrant.notifyRegistrant();
    }

    public void unregisterForActivePhoneSwitch(int i, Handler handler) {
        validatePhoneId(i);
        this.mActivePhoneRegistrants[i].remove(handler);
    }

    private void validatePhoneId(int i) {
        if (i < 0 || i >= this.mNumPhones) {
            throw new IllegalArgumentException("Invalid PhoneId");
        }
    }

    protected void suggestDefaultActivePhone(List<Integer> list) {
    }

    private void log(String str) {
        Rlog.d(LOG_TAG, str);
        this.mLocalLog.log(str);
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        IndentingPrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "  ");
        indentingPrintWriter.println("PhoneSwitcher:");
        Calendar calendar = Calendar.getInstance();
        for (int i = 0; i < this.mNumPhones; i++) {
            PhoneState phoneState = this.mPhoneStates[i];
            calendar.setTimeInMillis(phoneState.lastRequested);
            StringBuilder sb = new StringBuilder();
            sb.append("PhoneId(");
            sb.append(i);
            sb.append(") active=");
            sb.append(phoneState.active);
            sb.append(", lastRequest=");
            sb.append(phoneState.lastRequested == 0 ? "never" : String.format("%tm-%td %tH:%tM:%tS.%tL", calendar, calendar, calendar, calendar, calendar, calendar));
            indentingPrintWriter.println(sb.toString());
        }
        indentingPrintWriter.increaseIndent();
        this.mLocalLog.dump(fileDescriptor, indentingPrintWriter, strArr);
        indentingPrintWriter.decreaseIndent();
    }
}
