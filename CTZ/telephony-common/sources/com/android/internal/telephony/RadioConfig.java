package com.android.internal.telephony;

import android.content.Context;
import android.hardware.radio.V1_0.RadioResponseInfo;
import android.hardware.radio.config.V1_0.IRadioConfig;
import android.hardware.radio.config.V1_0.SimSlotStatus;
import android.net.ConnectivityManager;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.IHwBinder;
import android.os.Message;
import android.os.Registrant;
import android.os.RemoteException;
import android.os.WorkSource;
import android.telephony.Rlog;
import android.util.SparseArray;
import com.android.internal.telephony.uicc.IccSlotStatus;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

public class RadioConfig extends Handler {
    private static final boolean DBG = true;
    private static final int EVENT_SERVICE_DEAD = 1;
    private static final String TAG = "RadioConfig";
    private static final boolean VDBG = false;
    private static RadioConfig sRadioConfig;
    private final WorkSource mDefaultWorkSource;
    private final boolean mIsMobileNetworkSupported;
    protected Registrant mSimSlotStatusRegistrant;
    private volatile IRadioConfig mRadioConfigProxy = null;
    private final AtomicLong mRadioConfigProxyCookie = new AtomicLong(0);
    private final SparseArray<RILRequest> mRequestList = new SparseArray<>();
    private final RadioConfigResponse mRadioConfigResponse = new RadioConfigResponse(this);
    private final RadioConfigIndication mRadioConfigIndication = new RadioConfigIndication(this);
    private final ServiceDeathRecipient mServiceDeathRecipient = new ServiceDeathRecipient();

    final class ServiceDeathRecipient implements IHwBinder.DeathRecipient {
        ServiceDeathRecipient() {
        }

        @Override
        public void serviceDied(long j) {
            RadioConfig.logd("serviceDied");
            RadioConfig.this.sendMessage(RadioConfig.this.obtainMessage(1, Long.valueOf(j)));
        }
    }

    private RadioConfig(Context context) {
        this.mIsMobileNetworkSupported = ((ConnectivityManager) context.getSystemService("connectivity")).isNetworkSupported(0);
        this.mDefaultWorkSource = new WorkSource(context.getApplicationInfo().uid, context.getPackageName());
    }

    public static RadioConfig getInstance(Context context) {
        if (sRadioConfig == null) {
            sRadioConfig = new RadioConfig(context);
        }
        return sRadioConfig;
    }

    @Override
    public void handleMessage(Message message) {
        if (message.what == 1) {
            logd("handleMessage: EVENT_SERVICE_DEAD cookie = " + message.obj + " mRadioConfigProxyCookie = " + this.mRadioConfigProxyCookie.get());
            if (((Long) message.obj).longValue() == this.mRadioConfigProxyCookie.get()) {
                resetProxyAndRequestList("EVENT_SERVICE_DEAD", null);
            }
        }
    }

    private void clearRequestList(int i, boolean z) {
        synchronized (this.mRequestList) {
            int size = this.mRequestList.size();
            if (z) {
                logd("clearRequestList: mRequestList=" + size);
            }
            for (int i2 = 0; i2 < size; i2++) {
                RILRequest rILRequestValueAt = this.mRequestList.valueAt(i2);
                if (z) {
                    logd(i2 + ": [" + rILRequestValueAt.mSerial + "] " + requestToString(rILRequestValueAt.mRequest));
                }
                rILRequestValueAt.onError(i, null);
                rILRequestValueAt.release();
            }
            this.mRequestList.clear();
        }
    }

    private void resetProxyAndRequestList(String str, Exception exc) {
        loge(str + ": " + exc);
        this.mRadioConfigProxy = null;
        this.mRadioConfigProxyCookie.incrementAndGet();
        RILRequest.resetSerial();
        clearRequestList(1, false);
        getRadioConfigProxy(null);
    }

    public IRadioConfig getRadioConfigProxy(Message message) {
        if (!this.mIsMobileNetworkSupported) {
            if (message != null) {
                AsyncResult.forMessage(message, (Object) null, CommandException.fromRilErrno(1));
                message.sendToTarget();
            }
            return null;
        }
        if (this.mRadioConfigProxy != null) {
            return this.mRadioConfigProxy;
        }
        try {
            this.mRadioConfigProxy = IRadioConfig.getService(true);
            if (this.mRadioConfigProxy != null) {
                this.mRadioConfigProxy.linkToDeath(this.mServiceDeathRecipient, this.mRadioConfigProxyCookie.incrementAndGet());
                this.mRadioConfigProxy.setResponseFunctions(this.mRadioConfigResponse, this.mRadioConfigIndication);
            } else {
                loge("getRadioConfigProxy: mRadioConfigProxy == null");
            }
        } catch (RemoteException | RuntimeException e) {
            this.mRadioConfigProxy = null;
            loge("getRadioConfigProxy: RadioConfigProxy getService/setResponseFunctions: " + e);
        }
        if (this.mRadioConfigProxy == null) {
            loge("getRadioConfigProxy: mRadioConfigProxy == null");
            if (message != null) {
                AsyncResult.forMessage(message, (Object) null, CommandException.fromRilErrno(1));
                message.sendToTarget();
            }
        }
        return this.mRadioConfigProxy;
    }

    private RILRequest obtainRequest(int i, Message message, WorkSource workSource) {
        RILRequest rILRequestObtain = RILRequest.obtain(i, message, workSource);
        synchronized (this.mRequestList) {
            this.mRequestList.append(rILRequestObtain.mSerial, rILRequestObtain);
        }
        return rILRequestObtain;
    }

    private RILRequest findAndRemoveRequestFromList(int i) {
        RILRequest rILRequest;
        synchronized (this.mRequestList) {
            rILRequest = this.mRequestList.get(i);
            if (rILRequest != null) {
                this.mRequestList.remove(i);
            }
        }
        return rILRequest;
    }

    public RILRequest processResponse(RadioResponseInfo radioResponseInfo) {
        int i = radioResponseInfo.serial;
        int i2 = radioResponseInfo.error;
        int i3 = radioResponseInfo.type;
        if (i3 != 0) {
            loge("processResponse: Unexpected response type " + i3);
        }
        RILRequest rILRequestFindAndRemoveRequestFromList = findAndRemoveRequestFromList(i);
        if (rILRequestFindAndRemoveRequestFromList == null) {
            loge("processResponse: Unexpected response! serial: " + i + " error: " + i2);
            return null;
        }
        return rILRequestFindAndRemoveRequestFromList;
    }

    public void getSimSlotsStatus(Message message) {
        IRadioConfig radioConfigProxy = getRadioConfigProxy(message);
        if (radioConfigProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(144, message, this.mDefaultWorkSource);
            logd(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest));
            try {
                radioConfigProxy.getSimSlotsStatus(rILRequestObtainRequest.mSerial);
            } catch (RemoteException | RuntimeException e) {
                resetProxyAndRequestList("getSimSlotsStatus", e);
            }
        }
    }

    public void setSimSlotsMapping(int[] iArr, Message message) {
        IRadioConfig radioConfigProxy = getRadioConfigProxy(message);
        if (radioConfigProxy != null) {
            RILRequest rILRequestObtainRequest = obtainRequest(145, message, this.mDefaultWorkSource);
            logd(rILRequestObtainRequest.serialString() + "> " + requestToString(rILRequestObtainRequest.mRequest) + " " + Arrays.toString(iArr));
            try {
                radioConfigProxy.setSimSlotsMapping(rILRequestObtainRequest.mSerial, primitiveArrayToArrayList(iArr));
            } catch (RemoteException | RuntimeException e) {
                resetProxyAndRequestList("setSimSlotsMapping", e);
            }
        }
    }

    private static ArrayList<Integer> primitiveArrayToArrayList(int[] iArr) {
        ArrayList<Integer> arrayList = new ArrayList<>(iArr.length);
        for (int i : iArr) {
            arrayList.add(Integer.valueOf(i));
        }
        return arrayList;
    }

    static String requestToString(int i) {
        switch (i) {
            case 144:
                return "GET_SLOT_STATUS";
            case 145:
                return "SET_LOGICAL_TO_PHYSICAL_SLOT_MAPPING";
            default:
                return "<unknown request>";
        }
    }

    public void registerForSimSlotStatusChanged(Handler handler, int i, Object obj) {
        this.mSimSlotStatusRegistrant = new Registrant(handler, i, obj);
    }

    public void unregisterForSimSlotStatusChanged(Handler handler) {
        if (this.mSimSlotStatusRegistrant != null && this.mSimSlotStatusRegistrant.getHandler() == handler) {
            this.mSimSlotStatusRegistrant.clear();
            this.mSimSlotStatusRegistrant = null;
        }
    }

    static ArrayList<IccSlotStatus> convertHalSlotStatus(ArrayList<SimSlotStatus> arrayList) {
        ArrayList<IccSlotStatus> arrayList2 = new ArrayList<>(arrayList.size());
        for (SimSlotStatus simSlotStatus : arrayList) {
            IccSlotStatus iccSlotStatus = new IccSlotStatus();
            iccSlotStatus.setCardState(simSlotStatus.cardState);
            iccSlotStatus.setSlotState(simSlotStatus.slotState);
            iccSlotStatus.logicalSlotIndex = simSlotStatus.logicalSlotId;
            iccSlotStatus.atr = simSlotStatus.atr;
            iccSlotStatus.iccid = simSlotStatus.iccid;
            arrayList2.add(iccSlotStatus);
        }
        return arrayList2;
    }

    private static void logd(String str) {
        Rlog.d(TAG, str);
    }

    private static void loge(String str) {
        Rlog.e(TAG, str);
    }
}
