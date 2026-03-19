package com.mediatek.services.telephony;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneInternalInterface;
import com.android.internal.telephony.gsm.SuppServiceNotification;
import com.android.services.telephony.TelephonyConnection;
import com.android.services.telephony.TelephonyConnectionService;
import com.mediatek.internal.telephony.MtkGsmCdmaPhone;
import com.mediatek.internal.telephony.gsm.MtkSuppCrssNotification;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class MtkSuppMessageManager {
    private TelephonyConnectionService mConnectionService;
    private List<SuppMessageHandler> mSuppMessageHandlerList = new ArrayList();
    private HashMap<Integer, ArrayList<SuppServiceNotification>> mCachedSsnsMap = new HashMap<>();
    private HashMap<Integer, ArrayList<MtkSuppCrssNotification>> mCachedCrssnsMap = new HashMap<>();

    public MtkSuppMessageManager(TelephonyConnectionService telephonyConnectionService) {
        this.mConnectionService = telephonyConnectionService;
    }

    public void registerSuppMessageForPhones() {
        this.mSuppMessageHandlerList.clear();
        for (MtkGsmCdmaPhone mtkGsmCdmaPhone : PhoneFactory.getPhones()) {
            if (mtkGsmCdmaPhone != null) {
                Log.d("SuppMsgMgr", "registerSuppMessageForPhones()...for service" + this.mConnectionService + " for phone " + mtkGsmCdmaPhone.getPhoneId());
                SuppMessageHandler suppMessageHandler = new SuppMessageHandler(mtkGsmCdmaPhone);
                this.mSuppMessageHandlerList.add(suppMessageHandler);
                mtkGsmCdmaPhone.registerForSuppServiceFailed(suppMessageHandler, 250, (Object) null);
                if (mtkGsmCdmaPhone instanceof MtkGsmCdmaPhone) {
                    mtkGsmCdmaPhone.registerForCrssSuppServiceNotification(suppMessageHandler, 251, (Object) null);
                }
                mtkGsmCdmaPhone.registerForSuppServiceNotification(suppMessageHandler, 252, (Object) null);
            }
        }
    }

    public void unregisterSuppMessageForPhones() {
        for (SuppMessageHandler suppMessageHandler : this.mSuppMessageHandlerList) {
            MtkGsmCdmaPhone phone = suppMessageHandler.getPhone();
            if (phone != null) {
                Log.d("SuppMsgMgr", "unregisterSuppMessageForPhones()... for phone " + phone.getPhoneId());
                phone.unregisterForSuppServiceFailed(suppMessageHandler);
                phone.unregisterForSuppServiceNotification(suppMessageHandler);
                if (phone instanceof MtkGsmCdmaPhone) {
                    phone.unregisterForCrssSuppServiceNotification(suppMessageHandler);
                }
            }
        }
        this.mSuppMessageHandlerList.clear();
        this.mCachedSsnsMap.clear();
        this.mCachedCrssnsMap.clear();
    }

    public void registerSuppMessageForPhone(Phone phone) {
        Iterator<SuppMessageHandler> it = this.mSuppMessageHandlerList.iterator();
        while (it.hasNext()) {
            if (phone == it.next().getPhone()) {
                return;
            }
        }
        SuppMessageHandler suppMessageHandler = new SuppMessageHandler(phone);
        this.mSuppMessageHandlerList.add(suppMessageHandler);
        phone.registerForSuppServiceFailed(suppMessageHandler, 250, (Object) null);
        if (phone instanceof MtkGsmCdmaPhone) {
            ((MtkGsmCdmaPhone) phone).registerForCrssSuppServiceNotification(suppMessageHandler, 251, (Object) null);
        }
        phone.registerForSuppServiceNotification(suppMessageHandler, 252, (Object) null);
    }

    public void unregisterSuppMessageForPhone(Phone phone) {
        for (SuppMessageHandler suppMessageHandler : this.mSuppMessageHandlerList) {
            MtkGsmCdmaPhone phone2 = suppMessageHandler.getPhone();
            if (phone == phone2) {
                phone2.unregisterForSuppServiceFailed(suppMessageHandler);
                phone2.unregisterForSuppServiceNotification(suppMessageHandler);
                if (phone2 instanceof MtkGsmCdmaPhone) {
                    phone2.unregisterForCrssSuppServiceNotification(suppMessageHandler);
                }
                this.mSuppMessageHandlerList.remove(suppMessageHandler);
                return;
            }
        }
    }

    public void forceSuppMessageUpdate(TelephonyConnection telephonyConnection, Phone phone) {
        Log.d("SuppMsgMgr", "forceSuppMessageUpdate for " + telephonyConnection + ", " + phone + " phone " + phone.getPhoneId());
        ArrayList<SuppServiceNotification> arrayList = this.mCachedSsnsMap.get(Integer.valueOf(phone.getPhoneId()));
        if (arrayList != null) {
            Log.d("SuppMsgMgr", "forceSuppMessageUpdate()... for SuppServiceNotification for " + telephonyConnection + " for phone " + phone.getPhoneId());
            Iterator<SuppServiceNotification> it = arrayList.iterator();
            while (it.hasNext()) {
                onSuppServiceNotification(it.next(), phone, telephonyConnection);
            }
            this.mCachedSsnsMap.remove(Integer.valueOf(phone.getPhoneId()));
        }
        ArrayList<MtkSuppCrssNotification> arrayList2 = this.mCachedCrssnsMap.get(Integer.valueOf(phone.getPhoneId()));
        if (arrayList2 != null) {
            Log.d("SuppMsgMgr", "forceSuppMessageUpdate()... for SuppCrssNotification for " + telephonyConnection + " for phone " + phone.getPhoneId());
            Iterator<MtkSuppCrssNotification> it2 = arrayList2.iterator();
            while (it2.hasNext()) {
                onCrssSuppServiceNotification(it2.next(), phone, telephonyConnection);
            }
            this.mCachedCrssnsMap.remove(Integer.valueOf(phone.getPhoneId()));
        }
    }

    private class SuppMessageHandler extends Handler {
        private Phone mPhone;

        public SuppMessageHandler(Phone phone) {
            this.mPhone = phone;
        }

        private Phone getPhone() {
            return this.mPhone;
        }

        @Override
        public void handleMessage(Message message) {
            Log.d("SuppMsgMgr", "handleMessage()... for service " + MtkSuppMessageManager.this.mConnectionService + " for phone " + this.mPhone.getPhoneId());
            AsyncResult asyncResult = (AsyncResult) message.obj;
            if (asyncResult == null || asyncResult.result == null || MtkSuppMessageManager.this.mConnectionService == null) {
                Log.e("SuppMsgMgr", "handleMessage()...Wrong condition: ar / mConnectionService = " + asyncResult + " / " + MtkSuppMessageManager.this.mConnectionService);
                return;
            }
            switch (message.what) {
                case 250:
                    if (!(asyncResult.result instanceof PhoneInternalInterface.SuppService)) {
                        Log.e("SuppMsgMgr", "handleMessage()...Wrong data for Phone.SuppService");
                    } else if (asyncResult.result != PhoneInternalInterface.SuppService.CONFERENCE) {
                        MtkSuppMessageManager.this.onSuppServiceFailed((PhoneInternalInterface.SuppService) asyncResult.result, this.mPhone);
                    } else {
                        Log.d("SuppMsgMgr", "Service is conference don't trigger onSuppServiceFailed");
                    }
                    break;
                case 251:
                    if (asyncResult.result instanceof MtkSuppCrssNotification) {
                        MtkSuppMessageManager.this.onCrssSuppServiceNotification((MtkSuppCrssNotification) asyncResult.result, this.mPhone, null);
                    } else {
                        Log.e("SuppMsgMgr", "handleMessage()...Wrong data for SuppCrssNotification");
                    }
                    break;
                case 252:
                    if (asyncResult.result instanceof SuppServiceNotification) {
                        MtkSuppMessageManager.this.onSuppServiceNotification((SuppServiceNotification) asyncResult.result, this.mPhone, null);
                    } else {
                        Log.e("SuppMsgMgr", "handleMessage()..Wrong data for SuppServiceNotification");
                    }
                    break;
            }
        }
    }

    private void onSuppServiceFailed(PhoneInternalInterface.SuppService suppService, Phone phone) {
        Log.d("SuppMsgMgr", "onSuppServiceFailed()... " + suppService);
        int suppServiceActionCode = getSuppServiceActionCode(suppService);
        Connection properOriginalConnection = getProperOriginalConnection(phone);
        Log.d("SuppMsgMgr", "getProperOriginalConnection originalConnection = " + properOriginalConnection);
        TelephonyConnection telephonyConnection = (TelephonyConnection) findConnection(properOriginalConnection);
        if (telephonyConnection != null) {
            telephonyConnection.notifyActionFailed(suppServiceActionCode);
        } else {
            Log.d("SuppMsgMgr", "onSuppServiceFailed()...connection is null");
        }
    }

    private void onSuppServiceNotification(SuppServiceNotification suppServiceNotification, Phone phone, android.telecom.Connection connection) {
        Log.d("SuppMsgMgr", "onSuppServiceNotification()... " + suppServiceNotification);
        boolean z = true;
        if (suppServiceNotification.notificationType == 0) {
            if (suppServiceNotification.code == 10) {
                android.telecom.Connection connectionWithState = getConnectionWithState(Call.State.DIALING);
                if (connectionWithState == null) {
                    connectionWithState = getConnectionWithState(Call.State.ALERTING);
                }
                if (connectionWithState instanceof TelephonyConnection) {
                    if (connection == null || connection == connectionWithState) {
                        ((TelephonyConnection) connectionWithState).notifyEcc();
                    }
                } else if (connectionWithState == null && connection == null) {
                    Log.v("SuppMsgMgr", "onSuppServiceNotification()...MO connection is null");
                    addSsnList(suppServiceNotification, phone);
                }
                z = false;
            }
        } else if (suppServiceNotification.notificationType != 1) {
            z = false;
        }
        if (z) {
            TelephonyConnection telephonyConnection = (TelephonyConnection) findConnection(getProperOriginalConnection(phone));
            if (telephonyConnection != null) {
                if (connection == null || connection == telephonyConnection) {
                    telephonyConnection.notifySSNotificationToast(suppServiceNotification.notificationType, suppServiceNotification.type, suppServiceNotification.code, suppServiceNotification.number, suppServiceNotification.index);
                    return;
                }
                return;
            }
            if (connection == null) {
                Log.d("SuppMsgMgr", "onSuppServiceNotification()...MT connection is null");
                addSsnList(suppServiceNotification, phone);
            }
        }
    }

    private void addSsnList(SuppServiceNotification suppServiceNotification, Phone phone) {
        Log.d("SuppMsgMgr", "addSsnList for " + phone + " phone " + phone.getPhoneId());
        ArrayList<SuppServiceNotification> arrayList = this.mCachedSsnsMap.get(Integer.valueOf(phone.getPhoneId()));
        if (arrayList == null) {
            arrayList = new ArrayList<>();
        } else {
            this.mCachedSsnsMap.remove(Integer.valueOf(phone.getPhoneId()));
        }
        arrayList.add(suppServiceNotification);
        this.mCachedSsnsMap.put(Integer.valueOf(phone.getPhoneId()), arrayList);
    }

    private void onCrssSuppServiceNotification(MtkSuppCrssNotification mtkSuppCrssNotification, Phone phone, android.telecom.Connection connection) {
        Log.d("SuppMsgMgr", "onCrssSuppServiceNotification... " + mtkSuppCrssNotification);
        switch (mtkSuppCrssNotification.code) {
            case 1:
                TelephonyConnection telephonyConnection = (TelephonyConnection) findConnection(getOriginalConnectionWithState(phone, Call.State.ACTIVE));
                if (telephonyConnection != null) {
                    if (connection == null || connection == telephonyConnection) {
                        telephonyConnection.notifyNumberUpdate(mtkSuppCrssNotification.number);
                    }
                } else if (connection == null) {
                    Log.d("SuppMsgMgr", "onCrssSuppServiceNotification()...connection is null");
                    addCrssnList(mtkSuppCrssNotification, phone);
                }
                break;
            case 2:
                TelephonyConnection telephonyConnection2 = (TelephonyConnection) findConnection(getOriginalConnectionWithState(phone, Call.State.INCOMING));
                if (telephonyConnection2 != null) {
                    if (connection == null || connection == telephonyConnection2) {
                        telephonyConnection2.notifyIncomingInfoUpdate(mtkSuppCrssNotification.type, mtkSuppCrssNotification.alphaid, mtkSuppCrssNotification.cli_validity);
                    }
                } else if (connection == null) {
                    Log.d("SuppMsgMgr", "onCrssSuppServiceNotification()...connection is null");
                    addCrssnList(mtkSuppCrssNotification, phone);
                }
                break;
            case 3:
                TelephonyConnection telephonyConnection3 = (TelephonyConnection) findConnection(getOriginalConnectionWithState(phone, Call.State.DIALING));
                if (telephonyConnection3 == null && (telephonyConnection3 = (TelephonyConnection) findConnection(getOriginalConnectionWithState(phone, Call.State.ALERTING))) == null) {
                    telephonyConnection3 = (TelephonyConnection) findConnection(getOriginalConnectionWithState(phone, Call.State.ACTIVE));
                }
                if (telephonyConnection3 != null) {
                    if (connection == null || connection == telephonyConnection3) {
                        telephonyConnection3.notifyNumberUpdate(PhoneNumberUtils.stringFromStringAndTOA(mtkSuppCrssNotification.number, mtkSuppCrssNotification.type));
                    }
                } else if (connection == null) {
                    Log.d("SuppMsgMgr", "onCrssSuppServiceNotification()...connection is null");
                    addCrssnList(mtkSuppCrssNotification, phone);
                }
                break;
        }
    }

    private void addCrssnList(MtkSuppCrssNotification mtkSuppCrssNotification, Phone phone) {
        Log.d("SuppMsgMgr", "addCrssnList for " + phone + " phone " + phone.getPhoneId());
        ArrayList<MtkSuppCrssNotification> arrayList = this.mCachedCrssnsMap.get(Integer.valueOf(phone.getPhoneId()));
        if (arrayList == null) {
            arrayList = new ArrayList<>();
        } else {
            this.mCachedCrssnsMap.remove(Integer.valueOf(phone.getPhoneId()));
        }
        arrayList.add(mtkSuppCrssNotification);
        this.mCachedCrssnsMap.put(Integer.valueOf(phone.getPhoneId()), arrayList);
    }

    private Connection getOriginalConnectionWithState(Phone phone, Call.State state) {
        Call foregroundCall;
        if (state == Call.State.INCOMING) {
            foregroundCall = phone.getRingingCall();
        } else if (state == Call.State.HOLDING) {
            foregroundCall = phone.getBackgroundCall();
        } else if (state == Call.State.DIALING || state == Call.State.ALERTING || state == Call.State.ACTIVE) {
            foregroundCall = phone.getForegroundCall();
        } else {
            foregroundCall = null;
        }
        if (foregroundCall == null || foregroundCall.getState() != state) {
            return null;
        }
        return foregroundCall.getLatestConnection();
    }

    private Connection getProperOriginalConnection(Phone phone) {
        if (phone == null) {
            Log.d("SuppMsgMgr", "getProperOriginalConnection: phone is null");
            return null;
        }
        Connection latestConnection = phone.getRingingCall().getLatestConnection();
        Log.d("SuppMsgMgr", "getRingingCall originalConnection = " + latestConnection);
        if (latestConnection == null) {
            List connections = phone.getForegroundCall().getConnections();
            int i = 0;
            while (true) {
                if (i >= connections.size()) {
                    break;
                }
                Connection connection = (Connection) connections.get(i);
                Log.d("SuppMsgMgr", "getForegroundCall iterate conn = " + connection);
                if (connection == null || !connection.isAlive()) {
                    i++;
                } else {
                    latestConnection = connection;
                    break;
                }
            }
        }
        if (latestConnection == null) {
            List connections2 = phone.getBackgroundCall().getConnections();
            int i2 = 0;
            while (true) {
                if (i2 >= connections2.size()) {
                    break;
                }
                Connection connection2 = (Connection) connections2.get(i2);
                Log.d("SuppMsgMgr", "getBackgroundCall iterate conn = " + connection2);
                if (connection2 == null || !connection2.isAlive()) {
                    i2++;
                } else {
                    latestConnection = connection2;
                    break;
                }
            }
        }
        Phone imsPhone = phone.getImsPhone();
        if (imsPhone != null) {
            if (latestConnection == null) {
                List connections3 = imsPhone.getForegroundCall().getConnections();
                int i3 = 0;
                while (true) {
                    if (i3 >= connections3.size()) {
                        break;
                    }
                    try {
                        Connection connection3 = (Connection) connections3.get(i3);
                        Log.d("SuppMsgMgr", "getForegroundCall (IMS) iterate conn = " + connection3);
                        if (connection3 == null || !connection3.isAlive()) {
                            i3++;
                        } else {
                            latestConnection = connection3;
                            break;
                        }
                    } catch (IndexOutOfBoundsException e) {
                        Log.e("SuppMsgMgr", "getForegroundCall (IMS) Exception in getting element: " + e);
                    }
                }
            }
            if (latestConnection == null) {
                List connections4 = imsPhone.getBackgroundCall().getConnections();
                for (int i4 = 0; i4 < connections4.size(); i4++) {
                    try {
                        Connection connection4 = (Connection) connections4.get(i4);
                        Log.d("SuppMsgMgr", "getBackgroundCall (IMS)  iterate conn = " + connection4);
                        if (connection4 != null && connection4.isAlive()) {
                            return connection4;
                        }
                    } catch (IndexOutOfBoundsException e2) {
                        Log.e("SuppMsgMgr", "getBackgroundCall (IMS) Exception in getting element: " + e2);
                        return latestConnection;
                    }
                }
                return latestConnection;
            }
            return latestConnection;
        }
        return latestConnection;
    }

    private ArrayList<TelephonyConnection> getAllTelephonyConnectionsFromService() {
        ArrayList<TelephonyConnection> arrayList = new ArrayList<>();
        if (this.mConnectionService != null) {
            for (android.telecom.Connection connection : this.mConnectionService.getAllConnections()) {
                if (connection instanceof TelephonyConnection) {
                    arrayList.add((TelephonyConnection) connection);
                }
            }
        }
        return arrayList;
    }

    private android.telecom.Connection findConnection(Connection connection) {
        ArrayList<TelephonyConnection> allTelephonyConnectionsFromService = getAllTelephonyConnectionsFromService();
        Log.d("SuppMsgMgr", "findConnection originalConnection = " + connection);
        Log.d("SuppMsgMgr", "findConnection telephonyConnections.size = " + allTelephonyConnectionsFromService.size());
        TelephonyConnection telephonyConnection = null;
        if (connection != null && allTelephonyConnectionsFromService != null) {
            for (TelephonyConnection telephonyConnection2 : allTelephonyConnectionsFromService) {
                Log.d("SuppMsgMgr", "findConnection telephonyConnection = " + telephonyConnection2);
                Log.d("SuppMsgMgr", "findConnection telephonyConnection.getOriginalConnection = " + telephonyConnection2.getOriginalConnection());
                if (connection == telephonyConnection2.getOriginalConnection()) {
                    telephonyConnection = telephonyConnection2;
                }
            }
        }
        return telephonyConnection;
    }

    private android.telecom.Connection getConnectionWithState(Call.State state) {
        Call call;
        ArrayList<TelephonyConnection> allTelephonyConnectionsFromService = getAllTelephonyConnectionsFromService();
        if (allTelephonyConnectionsFromService != null) {
            for (TelephonyConnection telephonyConnection : allTelephonyConnectionsFromService) {
                Connection originalConnection = telephonyConnection.getOriginalConnection();
                if (originalConnection != null && (call = originalConnection.getCall()) != null && call.getState() == state) {
                    return telephonyConnection;
                }
            }
        }
        return null;
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$com$android$internal$telephony$PhoneInternalInterface$SuppService = new int[PhoneInternalInterface.SuppService.values().length];

        static {
            try {
                $SwitchMap$com$android$internal$telephony$PhoneInternalInterface$SuppService[PhoneInternalInterface.SuppService.SWITCH.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$PhoneInternalInterface$SuppService[PhoneInternalInterface.SuppService.SEPARATE.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$PhoneInternalInterface$SuppService[PhoneInternalInterface.SuppService.TRANSFER.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$PhoneInternalInterface$SuppService[PhoneInternalInterface.SuppService.REJECT.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$PhoneInternalInterface$SuppService[PhoneInternalInterface.SuppService.HANGUP.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$PhoneInternalInterface$SuppService[PhoneInternalInterface.SuppService.UNKNOWN.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
        }
    }

    private int getSuppServiceActionCode(PhoneInternalInterface.SuppService suppService) {
        switch (AnonymousClass1.$SwitchMap$com$android$internal$telephony$PhoneInternalInterface$SuppService[suppService.ordinal()]) {
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            case 5:
                return 5;
            default:
                return 0;
        }
    }
}
