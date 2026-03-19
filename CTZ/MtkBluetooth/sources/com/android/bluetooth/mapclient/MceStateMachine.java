package com.android.bluetooth.mapclient;

import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothUuid;
import android.bluetooth.SdpMasRecord;
import android.content.Intent;
import android.net.Uri;
import android.provider.Telephony;
import android.util.Log;
import com.android.bluetooth.BluetoothMetricsProto;
import com.android.bluetooth.btservice.MetricsLogger;
import com.android.bluetooth.btservice.ProfileService;
import com.android.bluetooth.mapclient.Bmessage;
import com.android.bluetooth.mapclient.EventReport;
import com.android.bluetooth.mapclient.MasClient;
import com.android.bluetooth.mapclient.RequestSetMessageStatus;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IState;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.vcard.VCardConfig;
import com.android.vcard.VCardConstants;
import com.android.vcard.VCardEntry;
import com.android.vcard.VCardProperty;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

final class MceStateMachine extends StateMachine {
    private static final Boolean DBG = false;
    private static final String FOLDER_INBOX = "inbox";
    private static final String FOLDER_MSG = "msg";
    private static final String FOLDER_OUTBOX = "outbox";
    private static final String FOLDER_TELECOM = "telecom";
    private static final String INBOX_PATH = "telecom/msg/inbox";
    private static final int MAX_MESSAGES = 20;
    private static final int MSG_CONNECT = 1;
    private static final int MSG_CONNECTING_TIMEOUT = 3;
    private static final int MSG_DISCONNECT = 2;
    private static final int MSG_DISCONNECTING_TIMEOUT = 4;
    static final int MSG_GET_LISTING = 2004;
    static final int MSG_GET_MESSAGE_LISTING = 2005;
    static final int MSG_INBOUND_MESSAGE = 2002;
    static final int MSG_MAS_CONNECTED = 1001;
    static final int MSG_MAS_DISCONNECTED = 1002;
    static final int MSG_MAS_REQUEST_COMPLETED = 1003;
    static final int MSG_MAS_REQUEST_FAILED = 1004;
    static final int MSG_MAS_SDP_DONE = 1005;
    static final int MSG_MAS_SDP_FAILED = 1006;
    static final int MSG_NOTIFICATION = 2003;
    static final int MSG_OUTBOUND_MESSAGE = 2001;
    private static final String TAG = "MceSM";
    private static final int TIMEOUT = 10000;
    private State mConnected;
    private State mConnecting;
    private Bmessage.Type mDefaultMessageType;
    private HashMap<Bmessage, PendingIntent> mDeliveryReceiptRequested;
    private final BluetoothDevice mDevice;
    private State mDisconnected;
    private State mDisconnecting;
    private MasClient mMasClient;
    private int mPreviousState;
    private HashMap<String, Bmessage> mSentMessageLog;
    private HashMap<Bmessage, PendingIntent> mSentReceiptRequested;
    private MapClientService mService;

    MceStateMachine(MapClientService mapClientService, BluetoothDevice bluetoothDevice) {
        this(mapClientService, bluetoothDevice, null);
    }

    @VisibleForTesting
    MceStateMachine(MapClientService mapClientService, BluetoothDevice bluetoothDevice, MasClient masClient) {
        super(TAG);
        this.mPreviousState = 0;
        this.mSentMessageLog = new HashMap<>(20);
        this.mSentReceiptRequested = new HashMap<>(20);
        this.mDeliveryReceiptRequested = new HashMap<>(20);
        this.mDefaultMessageType = Bmessage.Type.SMS_CDMA;
        this.mMasClient = masClient;
        this.mService = mapClientService;
        this.mPreviousState = 0;
        this.mDevice = bluetoothDevice;
        this.mDisconnected = new Disconnected();
        this.mConnecting = new Connecting();
        this.mDisconnecting = new Disconnecting();
        this.mConnected = new Connected();
        addState(this.mDisconnected);
        addState(this.mConnecting);
        addState(this.mDisconnecting);
        addState(this.mConnected);
        setInitialState(this.mConnecting);
        start();
    }

    public void doQuit() {
        quitNow();
    }

    protected void onQuitting() {
        if (this.mService != null) {
            this.mService.cleanupDevice(this.mDevice);
        }
    }

    synchronized BluetoothDevice getDevice() {
        return this.mDevice;
    }

    private void onConnectionStateChanged(int i, int i2) {
        if (this.mDevice == null) {
            return;
        }
        if (DBG.booleanValue()) {
            Log.d(TAG, "Connection state " + this.mDevice + ": " + i + "->" + i2);
        }
        if (i != i2 && i2 == 2) {
            MetricsLogger.logProfileConnectionEvent(BluetoothMetricsProto.ProfileId.MAP_CLIENT);
        }
        Intent intent = new Intent("android.bluetooth.mapmce.profile.action.CONNECTION_STATE_CHANGED");
        intent.putExtra("android.bluetooth.profile.extra.PREVIOUS_STATE", i);
        intent.putExtra("android.bluetooth.profile.extra.STATE", i2);
        intent.putExtra("android.bluetooth.device.extra.DEVICE", this.mDevice);
        intent.addFlags(VCardConfig.FLAG_APPEND_TYPE_PARAM);
        this.mService.sendBroadcast(intent, ProfileService.BLUETOOTH_PERM);
    }

    public synchronized int getState() {
        IState currentState = getCurrentState();
        if (currentState.getClass() == Disconnected.class) {
            return 0;
        }
        if (currentState.getClass() == Connected.class) {
            return 2;
        }
        if (currentState.getClass() == Connecting.class) {
            return 1;
        }
        return currentState.getClass() == Disconnecting.class ? 3 : 0;
    }

    public boolean disconnect() {
        if (DBG.booleanValue()) {
            Log.d(TAG, "Disconnect Request " + this.mDevice.getAddress());
        }
        sendMessage(2, this.mDevice);
        return true;
    }

    public synchronized boolean sendMapMessage(Uri[] uriArr, String str, PendingIntent pendingIntent, PendingIntent pendingIntent2) {
        if (DBG.booleanValue()) {
            Log.d(TAG, "Send Message " + str);
        }
        if (uriArr != null && uriArr.length > 0) {
            if (getCurrentState() != this.mConnected) {
                return false;
            }
            Bmessage bmessage = new Bmessage();
            bmessage.setType(getDefaultMessageType());
            bmessage.setStatus(Bmessage.Status.READ);
            for (Uri uri : uriArr) {
                VCardEntry vCardEntry = new VCardEntry();
                VCardProperty vCardProperty = new VCardProperty();
                if (DBG.booleanValue()) {
                    Log.d(TAG, "Scheme " + uri.getScheme());
                }
                if ("tel".equals(uri.getScheme())) {
                    vCardProperty.setName(VCardConstants.PROPERTY_TEL);
                    vCardProperty.addValues(uri.getSchemeSpecificPart());
                    if (DBG.booleanValue()) {
                        Log.d(TAG, "Sending to phone numbers " + vCardProperty.getValueList());
                    }
                    vCardEntry.addProperty(vCardProperty);
                    bmessage.addRecipient(vCardEntry);
                } else {
                    if (DBG.booleanValue()) {
                        Log.w(TAG, "Scheme " + uri.getScheme() + " not supported.");
                    }
                    return false;
                }
            }
            bmessage.setBodyContent(str);
            if (pendingIntent != null) {
                this.mSentReceiptRequested.put(bmessage, pendingIntent);
            }
            if (pendingIntent2 != null) {
                this.mDeliveryReceiptRequested.put(bmessage, pendingIntent2);
            }
            sendMessage(MSG_OUTBOUND_MESSAGE, bmessage);
            return true;
        }
        return false;
    }

    synchronized boolean getMessage(String str) {
        if (DBG.booleanValue()) {
            Log.d(TAG, "getMessage" + str);
        }
        if (getCurrentState() == this.mConnected) {
            sendMessage(MSG_INBOUND_MESSAGE, str);
            return true;
        }
        return false;
    }

    synchronized boolean getUnreadMessages() {
        if (DBG.booleanValue()) {
            Log.d(TAG, "getMessage");
        }
        if (getCurrentState() == this.mConnected) {
            sendMessage(MSG_GET_MESSAGE_LISTING, FOLDER_INBOX);
            return true;
        }
        return false;
    }

    private String getContactURIFromPhone(String str) {
        return "tel:" + str;
    }

    Bmessage.Type getDefaultMessageType() {
        Bmessage.Type type;
        synchronized (this.mDefaultMessageType) {
            type = this.mDefaultMessageType;
        }
        return type;
    }

    void setDefaultMessageType(SdpMasRecord sdpMasRecord) {
        int supportedMessageTypes = sdpMasRecord.getSupportedMessageTypes();
        synchronized (this.mDefaultMessageType) {
            try {
                if ((supportedMessageTypes & 4) > 0) {
                    this.mDefaultMessageType = Bmessage.Type.SMS_CDMA;
                } else if ((supportedMessageTypes & 2) > 0) {
                    this.mDefaultMessageType = Bmessage.Type.SMS_GSM;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public void dump(StringBuilder sb) {
        ProfileService.println(sb, "mCurrentDevice: " + this.mDevice.getAddress() + " (name = " + this.mDevice.getName() + "), StateMachine: " + toString());
    }

    class Disconnected extends State {
        Disconnected() {
        }

        public void enter() {
            if (MceStateMachine.DBG.booleanValue()) {
                Log.d(MceStateMachine.TAG, "Enter Disconnected: " + MceStateMachine.this.getCurrentMessage().what);
            }
            MceStateMachine.this.onConnectionStateChanged(MceStateMachine.this.mPreviousState, 0);
            MceStateMachine.this.mPreviousState = 0;
            MceStateMachine.this.quit();
        }

        public void exit() {
            MceStateMachine.this.mPreviousState = 0;
        }
    }

    class Connecting extends State {
        Connecting() {
        }

        public void enter() {
            if (MceStateMachine.DBG.booleanValue()) {
                Log.d(MceStateMachine.TAG, "Enter Connecting: " + MceStateMachine.this.getCurrentMessage().what);
            }
            MceStateMachine.this.onConnectionStateChanged(MceStateMachine.this.mPreviousState, 1);
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
            MceStateMachine.this.mDevice.sdpSearch(BluetoothUuid.MAS);
            MceStateMachine.this.sendMessageDelayed(3, 10000L);
        }

        public boolean processMessage(android.os.Message message) {
            if (MceStateMachine.DBG.booleanValue()) {
                Log.d(MceStateMachine.TAG, "processMessage" + getName() + message.what);
            }
            int i = message.what;
            if (i == 1005) {
                if (MceStateMachine.DBG.booleanValue()) {
                    Log.d(MceStateMachine.TAG, "SDP Complete");
                }
                if (MceStateMachine.this.mMasClient == null) {
                    MceStateMachine.this.mMasClient = new MasClient(MceStateMachine.this.mDevice, MceStateMachine.this, (SdpMasRecord) message.obj);
                    MceStateMachine.this.setDefaultMessageType((SdpMasRecord) message.obj);
                    return true;
                }
                return true;
            }
            switch (i) {
                case 1:
                case 2:
                    MceStateMachine.this.deferMessage(message);
                    return true;
                case 3:
                    MceStateMachine.this.transitionTo(MceStateMachine.this.mDisconnecting);
                    return true;
                default:
                    switch (i) {
                        case 1001:
                            MceStateMachine.this.transitionTo(MceStateMachine.this.mConnected);
                            return true;
                        case 1002:
                            MceStateMachine.this.transitionTo(MceStateMachine.this.mDisconnected);
                            return true;
                        default:
                            Log.w(MceStateMachine.TAG, "Unexpected message: " + message.what + " from state:" + getName());
                            return false;
                    }
            }
        }

        public void exit() {
            MceStateMachine.this.mPreviousState = 1;
            MceStateMachine.this.removeMessages(3);
        }
    }

    class Connected extends State {
        Connected() {
        }

        public void enter() {
            if (MceStateMachine.DBG.booleanValue()) {
                Log.d(MceStateMachine.TAG, "Enter Connected: " + MceStateMachine.this.getCurrentMessage().what);
            }
            MceStateMachine.this.onConnectionStateChanged(MceStateMachine.this.mPreviousState, 2);
            MceStateMachine.this.mMasClient.makeRequest(new RequestSetPath(MceStateMachine.FOLDER_TELECOM));
            MceStateMachine.this.mMasClient.makeRequest(new RequestSetPath("msg"));
            MceStateMachine.this.mMasClient.makeRequest(new RequestSetPath(MceStateMachine.FOLDER_INBOX));
            MceStateMachine.this.mMasClient.makeRequest(new RequestGetFolderListing(0, 0));
            MceStateMachine.this.mMasClient.makeRequest(new RequestSetPath(false));
            MceStateMachine.this.mMasClient.makeRequest(new RequestSetNotificationRegistration(true));
        }

        public boolean processMessage(android.os.Message message) {
            int i = message.what;
            if (i != 1003) {
                switch (i) {
                    case 1:
                        if (!MceStateMachine.this.mDevice.equals(message.obj)) {
                            MceStateMachine.this.deferMessage(message);
                            MceStateMachine.this.transitionTo(MceStateMachine.this.mDisconnecting);
                        }
                        break;
                    case 2:
                        if (MceStateMachine.this.mDevice.equals(message.obj)) {
                            MceStateMachine.this.transitionTo(MceStateMachine.this.mDisconnecting);
                        }
                        break;
                    default:
                        switch (i) {
                            case MceStateMachine.MSG_OUTBOUND_MESSAGE:
                                MceStateMachine.this.mMasClient.makeRequest(new RequestPushMessage(MceStateMachine.FOLDER_OUTBOX, (Bmessage) message.obj, null, false, false));
                                break;
                            case MceStateMachine.MSG_INBOUND_MESSAGE:
                                MceStateMachine.this.mMasClient.makeRequest(new RequestGetMessage((String) message.obj, MasClient.CharsetType.UTF_8, false));
                                break;
                            case MceStateMachine.MSG_NOTIFICATION:
                                processNotification(message);
                                break;
                            case MceStateMachine.MSG_GET_LISTING:
                                MceStateMachine.this.mMasClient.makeRequest(new RequestGetFolderListing(0, 0));
                                break;
                            case MceStateMachine.MSG_GET_MESSAGE_LISTING:
                                MessagesFilter messagesFilter = new MessagesFilter();
                                messagesFilter.setMessageType((byte) 0);
                                messagesFilter.setReadStatus((byte) 1);
                                Calendar calendar = Calendar.getInstance();
                                calendar.add(5, -7);
                                messagesFilter.setPeriod(calendar.getTime(), null);
                                MceStateMachine.this.mMasClient.makeRequest(new RequestGetMessagesListing((String) message.obj, 0, messagesFilter, 0, 50, 0));
                                break;
                            default:
                                Log.w(MceStateMachine.TAG, "Unexpected message: " + message.what + " from state:" + getName());
                                return false;
                        }
                        break;
                }
            } else {
                if (MceStateMachine.DBG.booleanValue()) {
                    Log.d(MceStateMachine.TAG, "Completed request");
                }
                if (message.obj instanceof RequestGetMessage) {
                    processInboundMessage((RequestGetMessage) message.obj);
                } else if (message.obj instanceof RequestPushMessage) {
                    String msgHandle = ((RequestPushMessage) message.obj).getMsgHandle();
                    if (MceStateMachine.DBG.booleanValue()) {
                        Log.d(MceStateMachine.TAG, "Message Sent......." + msgHandle);
                    }
                    MceStateMachine.this.mSentMessageLog.put(msgHandle.substring(2), ((RequestPushMessage) message.obj).getBMsg());
                } else if (message.obj instanceof RequestGetMessagesListing) {
                    processMessageListing((RequestGetMessagesListing) message.obj);
                }
            }
            return true;
        }

        public void exit() {
            MceStateMachine.this.mPreviousState = 2;
        }

        private void processNotification(android.os.Message message) {
            if (MceStateMachine.DBG.booleanValue()) {
                Log.d(MceStateMachine.TAG, "Handler: msg: " + message.what);
            }
            if (message.what == MceStateMachine.MSG_NOTIFICATION) {
                EventReport eventReport = (EventReport) message.obj;
                if (MceStateMachine.DBG.booleanValue()) {
                    Log.d(MceStateMachine.TAG, "Message Type = " + eventReport.getType());
                }
                if (MceStateMachine.DBG.booleanValue()) {
                    Log.d(MceStateMachine.TAG, "Message handle = " + eventReport.getHandle());
                }
                switch (AnonymousClass1.$SwitchMap$com$android$bluetooth$mapclient$EventReport$Type[eventReport.getType().ordinal()]) {
                    case 1:
                        MceStateMachine.this.mMasClient.makeRequest(new RequestGetMessage(eventReport.getHandle(), MasClient.CharsetType.UTF_8, false));
                        break;
                    case 2:
                    case 3:
                        notifySentMessageStatus(eventReport.getHandle(), eventReport.getType());
                        break;
                }
            }
        }

        private void markMessageRead(RequestGetMessage requestGetMessage) {
            if (MceStateMachine.DBG.booleanValue()) {
                Log.d(MceStateMachine.TAG, "markMessageRead");
            }
            MceStateMachine.this.mMasClient.makeRequest(new RequestSetMessageStatus(requestGetMessage.getHandle(), RequestSetMessageStatus.StatusIndicator.READ));
        }

        private void markMessageDeleted(RequestGetMessage requestGetMessage) {
            if (MceStateMachine.DBG.booleanValue()) {
                Log.d(MceStateMachine.TAG, "markMessageDeleted");
            }
            MceStateMachine.this.mMasClient.makeRequest(new RequestSetMessageStatus(requestGetMessage.getHandle(), RequestSetMessageStatus.StatusIndicator.DELETED));
        }

        private void processMessageListing(RequestGetMessagesListing requestGetMessagesListing) {
            if (MceStateMachine.DBG.booleanValue()) {
                Log.d(MceStateMachine.TAG, "processMessageListing");
            }
            ArrayList<Message> list = requestGetMessagesListing.getList();
            if (list != null) {
                for (Message message : list) {
                    if (MceStateMachine.DBG.booleanValue()) {
                        Log.d(MceStateMachine.TAG, "getting message ");
                    }
                    MceStateMachine.this.getMessage(message.getHandle());
                }
            }
        }

        private void processInboundMessage(RequestGetMessage requestGetMessage) {
            Bmessage message = requestGetMessage.getMessage();
            if (MceStateMachine.DBG.booleanValue()) {
                Log.d(MceStateMachine.TAG, "Notify inbound Message" + message);
            }
            if (message == null) {
            }
            if (!MceStateMachine.INBOX_PATH.equalsIgnoreCase(message.getFolder())) {
                if (MceStateMachine.DBG.booleanValue()) {
                    Log.d(MceStateMachine.TAG, "Ignoring message received in " + message.getFolder() + ".");
                    return;
                }
                return;
            }
            switch (AnonymousClass1.$SwitchMap$com$android$bluetooth$mapclient$Bmessage$Type[message.getType().ordinal()]) {
                case 1:
                case 2:
                    if (MceStateMachine.DBG.booleanValue()) {
                        Log.d(MceStateMachine.TAG, "Body: " + message.getBodyContent());
                    }
                    if (MceStateMachine.DBG.booleanValue()) {
                        Log.d(MceStateMachine.TAG, message.toString());
                    }
                    if (MceStateMachine.DBG.booleanValue()) {
                        Log.d(MceStateMachine.TAG, "Recipients" + message.getRecipients().toString());
                    }
                    Intent intent = new Intent();
                    intent.setAction("android.bluetooth.mapmce.profile.action.MESSAGE_RECEIVED");
                    intent.putExtra("android.bluetooth.device.extra.DEVICE", MceStateMachine.this.mDevice);
                    intent.putExtra("android.bluetooth.mapmce.profile.extra.MESSAGE_HANDLE", requestGetMessage.getHandle());
                    intent.putExtra("android.intent.extra.TEXT", message.getBodyContent());
                    VCardEntry originator = message.getOriginator();
                    if (originator != null) {
                        if (MceStateMachine.DBG.booleanValue()) {
                            Log.d(MceStateMachine.TAG, originator.toString());
                        }
                        List<VCardEntry.PhoneData> phoneList = originator.getPhoneList();
                        if (phoneList != null && phoneList.size() > 0) {
                            String number = phoneList.get(0).getNumber();
                            if (MceStateMachine.DBG.booleanValue()) {
                                Log.d(MceStateMachine.TAG, "Originator number: " + number);
                            }
                            intent.putExtra("android.bluetooth.mapmce.profile.extra.SENDER_CONTACT_URI", MceStateMachine.this.getContactURIFromPhone(number));
                        }
                        intent.putExtra("android.bluetooth.mapmce.profile.extra.SENDER_CONTACT_NAME", originator.getDisplayName());
                    }
                    String defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(MceStateMachine.this.mService);
                    if (defaultSmsPackage != null) {
                        intent.setPackage(defaultSmsPackage);
                    }
                    MceStateMachine.this.mService.sendBroadcast(intent, "android.permission.RECEIVE_SMS");
                    break;
                default:
                    Log.e(MceStateMachine.TAG, "Received unhandled type" + message.getType().toString());
                    break;
            }
        }

        private void notifySentMessageStatus(String str, EventReport.Type type) {
            if (MceStateMachine.DBG.booleanValue()) {
                Log.d(MceStateMachine.TAG, "got a status for " + str + " Status = " + type);
            }
            PendingIntent pendingIntent = null;
            String strSubstring = str.substring(2);
            if (type == EventReport.Type.SENDING_FAILURE || type == EventReport.Type.SENDING_SUCCESS) {
                pendingIntent = (PendingIntent) MceStateMachine.this.mSentReceiptRequested.remove(MceStateMachine.this.mSentMessageLog.get(strSubstring));
            } else if (type == EventReport.Type.DELIVERY_SUCCESS || type == EventReport.Type.DELIVERY_FAILURE) {
                pendingIntent = (PendingIntent) MceStateMachine.this.mDeliveryReceiptRequested.remove(MceStateMachine.this.mSentMessageLog.get(strSubstring));
            }
            if (pendingIntent != null) {
                try {
                    if (MceStateMachine.DBG.booleanValue()) {
                        Log.d(MceStateMachine.TAG, "*******Sending " + pendingIntent);
                    }
                    int i = -1;
                    if (type == EventReport.Type.SENDING_FAILURE || type == EventReport.Type.DELIVERY_FAILURE) {
                        i = 1;
                    }
                    pendingIntent.send(i);
                    return;
                } catch (PendingIntent.CanceledException e) {
                    Log.w(MceStateMachine.TAG, "Notification Request Canceled" + e);
                    return;
                }
            }
            Log.e(MceStateMachine.TAG, "Received a notification on message with handle = " + str + ", but it is NOT found in mSentMessageLog! where did it go?");
        }
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$com$android$bluetooth$mapclient$Bmessage$Type = new int[Bmessage.Type.values().length];
        static final int[] $SwitchMap$com$android$bluetooth$mapclient$EventReport$Type;

        static {
            try {
                $SwitchMap$com$android$bluetooth$mapclient$Bmessage$Type[Bmessage.Type.SMS_CDMA.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$bluetooth$mapclient$Bmessage$Type[Bmessage.Type.SMS_GSM.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$bluetooth$mapclient$Bmessage$Type[Bmessage.Type.MMS.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$bluetooth$mapclient$Bmessage$Type[Bmessage.Type.EMAIL.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            $SwitchMap$com$android$bluetooth$mapclient$EventReport$Type = new int[EventReport.Type.values().length];
            try {
                $SwitchMap$com$android$bluetooth$mapclient$EventReport$Type[EventReport.Type.NEW_MESSAGE.ordinal()] = 1;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$android$bluetooth$mapclient$EventReport$Type[EventReport.Type.DELIVERY_SUCCESS.ordinal()] = 2;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$com$android$bluetooth$mapclient$EventReport$Type[EventReport.Type.SENDING_SUCCESS.ordinal()] = 3;
            } catch (NoSuchFieldError e7) {
            }
        }
    }

    class Disconnecting extends State {
        Disconnecting() {
        }

        public void enter() {
            if (MceStateMachine.DBG.booleanValue()) {
                Log.d(MceStateMachine.TAG, "Enter Disconnecting: " + MceStateMachine.this.getCurrentMessage().what);
            }
            MceStateMachine.this.onConnectionStateChanged(MceStateMachine.this.mPreviousState, 3);
            if (MceStateMachine.this.mMasClient != null) {
                MceStateMachine.this.mMasClient.makeRequest(new RequestSetNotificationRegistration(false));
                MceStateMachine.this.mMasClient.shutdown();
                MceStateMachine.this.sendMessageDelayed(4, 10000L);
                return;
            }
            MceStateMachine.this.transitionTo(MceStateMachine.this.mDisconnected);
        }

        public boolean processMessage(android.os.Message message) {
            int i = message.what;
            if (i == 4 || i == 1002) {
                MceStateMachine.this.mMasClient = null;
                MceStateMachine.this.transitionTo(MceStateMachine.this.mDisconnected);
                return true;
            }
            switch (i) {
                case 1:
                case 2:
                    MceStateMachine.this.deferMessage(message);
                    return true;
                default:
                    Log.w(MceStateMachine.TAG, "Unexpected message: " + message.what + " from state:" + getName());
                    return false;
            }
        }

        public void exit() {
            MceStateMachine.this.mPreviousState = 3;
            MceStateMachine.this.removeMessages(4);
        }
    }

    void receiveEvent(EventReport eventReport) {
        if (DBG.booleanValue()) {
            Log.d(TAG, "Message Type = " + eventReport.getType());
        }
        if (DBG.booleanValue()) {
            Log.d(TAG, "Message handle = " + eventReport.getHandle());
        }
        sendMessage(MSG_NOTIFICATION, eventReport);
    }
}
