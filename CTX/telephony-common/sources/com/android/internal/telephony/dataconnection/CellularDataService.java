package com.android.internal.telephony.dataconnection;

import android.hardware.radio.V1_0.SetupDataCallResult;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.NetworkUtils;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.data.DataCallResponse;
import android.telephony.data.DataProfile;
import android.telephony.data.DataService;
import android.telephony.data.DataServiceCallback;
import android.text.TextUtils;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CellularDataService extends DataService {
    private static final int DATA_CALL_LIST_CHANGED = 6;
    private static final boolean DBG = false;
    private static final int DEACTIVATE_DATA_ALL_COMPLETE = 2;
    private static final int GET_DATA_CALL_LIST_COMPLETE = 5;
    private static final int SETUP_DATA_CALL_COMPLETE = 1;
    private static final int SET_DATA_PROFILE_COMPLETE = 4;
    private static final int SET_INITIAL_ATTACH_APN_COMPLETE = 3;
    private static final String TAG = CellularDataService.class.getSimpleName();

    private class CellularDataServiceProvider extends DataService.DataServiceProvider {
        private final Map<Message, DataServiceCallback> mCallbackMap;
        private final Handler mHandler;
        private final Looper mLooper;
        private final Phone mPhone;

        private CellularDataServiceProvider(int i) {
            super(CellularDataService.this, i);
            this.mCallbackMap = new HashMap();
            this.mPhone = PhoneFactory.getPhone(getSlotId());
            HandlerThread handlerThread = new HandlerThread(CellularDataService.class.getSimpleName());
            handlerThread.start();
            this.mLooper = handlerThread.getLooper();
            this.mHandler = new Handler(this.mLooper) {
                @Override
                public void handleMessage(Message message) {
                    List dataCallList;
                    DataServiceCallback dataServiceCallback = (DataServiceCallback) CellularDataServiceProvider.this.mCallbackMap.remove(message);
                    AsyncResult asyncResult = (AsyncResult) message.obj;
                    switch (message.what) {
                        case 1:
                            dataServiceCallback.onSetupDataCallComplete(asyncResult.exception != null ? 4 : 0, CellularDataService.this.convertDataCallResult((SetupDataCallResult) asyncResult.result));
                            break;
                        case 2:
                            dataServiceCallback.onDeactivateDataCallComplete(asyncResult.exception != null ? 4 : 0);
                            break;
                        case 3:
                            dataServiceCallback.onSetInitialAttachApnComplete(asyncResult.exception != null ? 4 : 0);
                            break;
                        case 4:
                            dataServiceCallback.onSetDataProfileComplete(asyncResult.exception != null ? 4 : 0);
                            break;
                        case 5:
                            int i2 = asyncResult.exception != null ? 4 : 0;
                            if (asyncResult.exception == null) {
                                dataCallList = CellularDataServiceProvider.this.getDataCallList((List<SetupDataCallResult>) asyncResult.result);
                            } else {
                                dataCallList = null;
                            }
                            dataServiceCallback.onGetDataCallListComplete(i2, dataCallList);
                            break;
                        case 6:
                            CellularDataServiceProvider.this.notifyDataCallListChanged(CellularDataServiceProvider.this.getDataCallList((List<SetupDataCallResult>) asyncResult.result));
                            break;
                        default:
                            CellularDataService.this.loge("Unexpected event: " + message.what);
                            break;
                    }
                }
            };
            this.mPhone.mCi.registerForDataCallListChanged(this.mHandler, 6, null);
        }

        private List<DataCallResponse> getDataCallList(List<SetupDataCallResult> list) {
            ArrayList arrayList = new ArrayList();
            Iterator<SetupDataCallResult> it = list.iterator();
            while (it.hasNext()) {
                arrayList.add(CellularDataService.this.convertDataCallResult(it.next()));
            }
            return arrayList;
        }

        public void setupDataCall(int i, DataProfile dataProfile, boolean z, boolean z2, int i2, LinkProperties linkProperties, DataServiceCallback dataServiceCallback) {
            Message messageObtain;
            if (dataServiceCallback != null) {
                messageObtain = Message.obtain(this.mHandler, 1);
                this.mCallbackMap.put(messageObtain, dataServiceCallback);
            } else {
                messageObtain = null;
            }
            this.mPhone.mCi.setupDataCall(i, dataProfile, z, z2, i2, linkProperties, messageObtain);
        }

        public void deactivateDataCall(int i, int i2, DataServiceCallback dataServiceCallback) {
            Message messageObtain;
            if (dataServiceCallback != null) {
                messageObtain = Message.obtain(this.mHandler, 2);
                this.mCallbackMap.put(messageObtain, dataServiceCallback);
            } else {
                messageObtain = null;
            }
            this.mPhone.mCi.deactivateDataCall(i, i2, messageObtain);
        }

        public void setInitialAttachApn(DataProfile dataProfile, boolean z, DataServiceCallback dataServiceCallback) {
            Message messageObtain;
            if (dataServiceCallback != null) {
                messageObtain = Message.obtain(this.mHandler, 3);
                this.mCallbackMap.put(messageObtain, dataServiceCallback);
            } else {
                messageObtain = null;
            }
            this.mPhone.mCi.setInitialAttachApn(dataProfile, z, messageObtain);
        }

        public void setDataProfile(List<DataProfile> list, boolean z, DataServiceCallback dataServiceCallback) {
            Message messageObtain;
            if (dataServiceCallback != null) {
                messageObtain = Message.obtain(this.mHandler, 4);
                this.mCallbackMap.put(messageObtain, dataServiceCallback);
            } else {
                messageObtain = null;
            }
            this.mPhone.mCi.setDataProfile((DataProfile[]) list.toArray(new DataProfile[list.size()]), z, messageObtain);
        }

        public void getDataCallList(DataServiceCallback dataServiceCallback) {
            Message messageObtain;
            if (dataServiceCallback != null) {
                messageObtain = Message.obtain(this.mHandler, 5);
                this.mCallbackMap.put(messageObtain, dataServiceCallback);
            } else {
                messageObtain = null;
            }
            this.mPhone.mCi.getDataCallList(messageObtain);
        }
    }

    public DataService.DataServiceProvider createDataServiceProvider(int i) {
        log("Cellular data service created for slot " + i);
        if (!SubscriptionManager.isValidSlotIndex(i)) {
            loge("Tried to cellular data service with invalid slotId " + i);
            return null;
        }
        return new CellularDataServiceProvider(i);
    }

    @VisibleForTesting
    public DataCallResponse convertDataCallResult(SetupDataCallResult setupDataCallResult) {
        String[] strArrSplit;
        String[] strArrSplit2;
        LinkAddress linkAddress;
        String[] strArrSplit3 = null;
        if (setupDataCallResult == null) {
            return null;
        }
        if (!TextUtils.isEmpty(setupDataCallResult.addresses)) {
            strArrSplit = setupDataCallResult.addresses.split("\\s+");
        } else {
            strArrSplit = null;
        }
        ArrayList arrayList = new ArrayList();
        if (strArrSplit != null) {
            for (String str : strArrSplit) {
                String strTrim = str.trim();
                if (!strTrim.isEmpty()) {
                    try {
                        if (strTrim.split("/").length == 2) {
                            linkAddress = new LinkAddress(strTrim);
                        } else {
                            InetAddress inetAddressNumericToInetAddress = NetworkUtils.numericToInetAddress(strTrim);
                            linkAddress = new LinkAddress(inetAddressNumericToInetAddress, inetAddressNumericToInetAddress instanceof Inet4Address ? 32 : 128);
                        }
                        arrayList.add(linkAddress);
                    } catch (IllegalArgumentException e) {
                        loge("Unknown address: " + strTrim + ", exception = " + e);
                    }
                }
            }
        }
        if (!TextUtils.isEmpty(setupDataCallResult.dnses)) {
            strArrSplit2 = setupDataCallResult.dnses.split("\\s+");
        } else {
            strArrSplit2 = null;
        }
        ArrayList arrayList2 = new ArrayList();
        if (strArrSplit2 != null) {
            for (String str2 : strArrSplit2) {
                String strTrim2 = str2.trim();
                try {
                    arrayList2.add(NetworkUtils.numericToInetAddress(strTrim2));
                } catch (IllegalArgumentException e2) {
                    loge("Unknown dns: " + strTrim2 + ", exception = " + e2);
                }
            }
        }
        if (!TextUtils.isEmpty(setupDataCallResult.gateways)) {
            strArrSplit3 = setupDataCallResult.gateways.split("\\s+");
        }
        ArrayList arrayList3 = new ArrayList();
        if (strArrSplit3 != null) {
            for (String str3 : strArrSplit3) {
                String strTrim3 = str3.trim();
                try {
                    arrayList3.add(NetworkUtils.numericToInetAddress(strTrim3));
                } catch (IllegalArgumentException e3) {
                    loge("Unknown gateway: " + strTrim3 + ", exception = " + e3);
                }
            }
        }
        return new DataCallResponse(setupDataCallResult.status, setupDataCallResult.suggestedRetryTime, setupDataCallResult.cid, setupDataCallResult.active, setupDataCallResult.type, setupDataCallResult.ifname, arrayList, arrayList2, arrayList3, new ArrayList(Arrays.asList(setupDataCallResult.pcscf.trim().split("\\s+"))), setupDataCallResult.mtu);
    }

    private void log(String str) {
        Rlog.d(TAG, str);
    }

    private void loge(String str) {
        Rlog.e(TAG, str);
    }
}
