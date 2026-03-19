package com.android.internal.telephony.cdma;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.provider.Settings;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandsInterface;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class CdmaSubscriptionSourceManager extends Handler {
    private static final int EVENT_CDMA_SUBSCRIPTION_SOURCE_CHANGED = 1;
    private static final int EVENT_GET_CDMA_SUBSCRIPTION_SOURCE = 2;
    private static final int EVENT_RADIO_ON = 3;
    private static final int EVENT_SUBSCRIPTION_STATUS_CHANGED = 4;
    static final String LOG_TAG = "CdmaSSM";
    public static final int PREFERRED_CDMA_SUBSCRIPTION = 0;
    private static final int SUBSCRIPTION_ACTIVATED = 1;
    public static final int SUBSCRIPTION_FROM_NV = 1;
    public static final int SUBSCRIPTION_FROM_RUIM = 0;
    public static final int SUBSCRIPTION_SOURCE_UNKNOWN = -1;
    private static CdmaSubscriptionSourceManager sInstance;
    private CommandsInterface mCi;
    private static final Object sReferenceCountMonitor = new Object();
    private static final HashMap<Handler, CommandsInterface> sHandlerCis = new HashMap<>();
    private static final HashMap<CommandsInterface, CdmaSubscriptionSourceManager> sCiInstances = new HashMap<>();
    private static final HashMap<CommandsInterface, Integer> sCiReferenceCounts = new HashMap<>();
    private RegistrantList mCdmaSubscriptionSourceChangedRegistrants = new RegistrantList();
    private AtomicInteger mCdmaSubscriptionSource = new AtomicInteger(1);
    private int mActStatus = 0;

    private CdmaSubscriptionSourceManager(Context context, CommandsInterface ci) {
        this.mCi = ci;
        this.mCi.registerForCdmaSubscriptionChanged(this, 1, null);
        this.mCi.registerForOn(this, 3, null);
        int subscriptionSource = getDefault(context);
        log("cdmaSSM constructor: " + subscriptionSource);
        this.mCdmaSubscriptionSource.set(subscriptionSource);
        this.mCi.registerForSubscriptionStatusChanged(this, 4, null);
    }

    public static CdmaSubscriptionSourceManager getInstance(Context context, CommandsInterface ci, Handler h, int what, Object obj) {
        int referenceCount;
        synchronized (sReferenceCountMonitor) {
            sInstance = sCiInstances.get(ci);
            if (sInstance == null) {
                sInstance = new CdmaSubscriptionSourceManager(context, ci);
                sCiInstances.put(ci, sInstance);
            }
            sHandlerCis.put(h, ci);
            if (sCiReferenceCounts.get(ci) == null) {
                referenceCount = 0;
            } else {
                referenceCount = sCiReferenceCounts.get(ci).intValue();
            }
            sCiReferenceCounts.put(ci, Integer.valueOf(referenceCount + 1));
        }
        sInstance.registerForCdmaSubscriptionSourceChanged(h, what, obj);
        return sInstance;
    }

    public void dispose(Handler h) {
        this.mCdmaSubscriptionSourceChangedRegistrants.remove(h);
        synchronized (sReferenceCountMonitor) {
            CommandsInterface mCi = sHandlerCis.get(h);
            if (mCi == null) {
                log("The handler doesn't create CdmaSSM, return !");
                return;
            }
            int referenceCount = sCiReferenceCounts.get(mCi).intValue() - 1;
            sCiReferenceCounts.put(mCi, Integer.valueOf(referenceCount));
            sHandlerCis.remove(h);
            log("dispose ci = " + (mCi != null ? Integer.valueOf(mCi.hashCode()) : null) + "  referenceCount = " + referenceCount);
            if (referenceCount <= 0) {
                mCi.unregisterForCdmaSubscriptionChanged(this);
                mCi.unregisterForOn(this);
                mCi.unregisterForSubscriptionStatusChanged(this);
                this.mActStatus = 0;
                sCiInstances.remove(mCi);
                sCiReferenceCounts.remove(mCi);
            }
        }
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 1:
            case 2:
                log("CDMA_SUBSCRIPTION_SOURCE event = " + msg.what);
                handleGetCdmaSubscriptionSource((AsyncResult) msg.obj);
                break;
            case 3:
                this.mCi.getCdmaSubscriptionSource(obtainMessage(2));
                break;
            case 4:
                log("EVENT_SUBSCRIPTION_STATUS_CHANGED");
                AsyncResult ar = (AsyncResult) msg.obj;
                if (ar.exception == null) {
                    int actStatus = ((int[]) ar.result)[0];
                    log("actStatus = " + actStatus);
                    this.mActStatus = actStatus;
                    if (actStatus == 1) {
                        Rlog.v(LOG_TAG, "get Cdma Subscription Source");
                        this.mCi.getCdmaSubscriptionSource(obtainMessage(2));
                    }
                } else {
                    logw("EVENT_SUBSCRIPTION_STATUS_CHANGED, Exception:" + ar.exception);
                }
                break;
            default:
                super.handleMessage(msg);
                break;
        }
    }

    public int getCdmaSubscriptionSource() {
        log("getcdmasubscriptionSource: " + this.mCdmaSubscriptionSource.get());
        return this.mCdmaSubscriptionSource.get();
    }

    public static int getDefault(Context context) {
        int subscriptionSource = Settings.Global.getInt(context.getContentResolver(), "subscription_mode", 0);
        Rlog.d(LOG_TAG, "subscriptionSource from settings: " + subscriptionSource);
        return subscriptionSource;
    }

    private void registerForCdmaSubscriptionSourceChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mCdmaSubscriptionSourceChangedRegistrants.add(r);
    }

    private void handleGetCdmaSubscriptionSource(AsyncResult ar) {
        if (ar.exception == null && ar.result != null) {
            int newSubscriptionSource = ((int[]) ar.result)[0];
            if (newSubscriptionSource == this.mCdmaSubscriptionSource.get()) {
                return;
            }
            log("Subscription Source Changed : " + this.mCdmaSubscriptionSource + " >> " + newSubscriptionSource);
            this.mCdmaSubscriptionSource.set(newSubscriptionSource);
            this.mCdmaSubscriptionSourceChangedRegistrants.notifyRegistrants(new AsyncResult((Object) null, (Object) null, (Throwable) null));
            return;
        }
        logw("Unable to get CDMA Subscription Source, Exception: " + ar.exception + ", result: " + ar.result);
    }

    private void log(String s) {
        Rlog.d(LOG_TAG, s);
    }

    private void logw(String s) {
        Rlog.w(LOG_TAG, s);
    }

    public int getActStatus() {
        log("getActStatus " + this.mActStatus);
        return this.mActStatus;
    }
}
