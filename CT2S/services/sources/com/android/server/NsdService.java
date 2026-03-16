package com.android.server;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.nsd.DnsSdTxtRecord;
import android.net.nsd.INsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Binder;
import android.os.Message;
import android.os.Messenger;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.NativeDaemonConnector;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class NsdService extends INsdManager.Stub {
    private static final int BASE = 393216;
    private static final int CMD_TO_STRING_COUNT = 19;
    private static final boolean DBG = true;
    private static final String MDNS_TAG = "mDnsConnector";
    private static final String TAG = "NsdService";
    private static String[] sCmdToString = new String[19];
    private ContentResolver mContentResolver;
    private Context mContext;
    private HashMap<Messenger, ClientInfo> mClients = new HashMap<>();
    private SparseArray<ClientInfo> mIdToClientInfoMap = new SparseArray<>();
    private AsyncChannel mReplyChannel = new AsyncChannel();
    private int INVALID_ID = 0;
    private int mUniqueId = 1;
    private final CountDownLatch mNativeDaemonConnected = new CountDownLatch(1);
    private NativeDaemonConnector mNativeConnector = new NativeDaemonConnector(new NativeCallbackReceiver(), "mdns", 10, MDNS_TAG, 25, null);
    private NsdStateMachine mNsdStateMachine = new NsdStateMachine(TAG);

    static {
        sCmdToString[1] = "DISCOVER";
        sCmdToString[6] = "STOP-DISCOVER";
        sCmdToString[9] = "REGISTER";
        sCmdToString[12] = "UNREGISTER";
        sCmdToString[18] = "RESOLVE";
    }

    private static String cmdToString(int cmd) {
        int cmd2 = cmd - BASE;
        if (cmd2 < 0 || cmd2 >= sCmdToString.length) {
            return null;
        }
        return sCmdToString[cmd2];
    }

    private class NsdStateMachine extends StateMachine {
        private final DefaultState mDefaultState;
        private final DisabledState mDisabledState;
        private final EnabledState mEnabledState;

        protected String getWhatToString(int what) {
            return NsdService.cmdToString(what);
        }

        private void registerForNsdSetting() {
            ContentObserver contentObserver = new ContentObserver(getHandler()) {
                @Override
                public void onChange(boolean selfChange) {
                    if (NsdService.this.isNsdEnabled()) {
                        NsdService.this.mNsdStateMachine.sendMessage(393240);
                    } else {
                        NsdService.this.mNsdStateMachine.sendMessage(393241);
                    }
                }
            };
            NsdService.this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("nsd_on"), false, contentObserver);
        }

        NsdStateMachine(String name) {
            super(name);
            this.mDefaultState = new DefaultState();
            this.mDisabledState = new DisabledState();
            this.mEnabledState = new EnabledState();
            addState(this.mDefaultState);
            addState(this.mDisabledState, this.mDefaultState);
            addState(this.mEnabledState, this.mDefaultState);
            if (NsdService.this.isNsdEnabled()) {
                setInitialState(this.mEnabledState);
            } else {
                setInitialState(this.mDisabledState);
            }
            setLogRecSize(25);
            registerForNsdSetting();
        }

        class DefaultState extends State {
            DefaultState() {
            }

            public boolean processMessage(Message msg) {
                switch (msg.what) {
                    case 69632:
                        if (msg.arg1 == 0) {
                            AsyncChannel c = (AsyncChannel) msg.obj;
                            Slog.d(NsdService.TAG, "New client listening to asynchronous messages");
                            c.sendMessage(69634);
                            NsdService.this.mClients.put(msg.replyTo, new ClientInfo(c, msg.replyTo));
                        } else {
                            Slog.e(NsdService.TAG, "Client connection failure, error=" + msg.arg1);
                        }
                        return NsdService.DBG;
                    case 69633:
                        AsyncChannel ac = new AsyncChannel();
                        ac.connect(NsdService.this.mContext, NsdStateMachine.this.getHandler(), msg.replyTo);
                        return NsdService.DBG;
                    case 69636:
                        switch (msg.arg1) {
                            case 2:
                                Slog.e(NsdService.TAG, "Send failed, client connection lost");
                                break;
                            case 3:
                            default:
                                Slog.d(NsdService.TAG, "Client connection lost with reason: " + msg.arg1);
                                break;
                            case 4:
                                Slog.d(NsdService.TAG, "Client disconnected");
                                break;
                        }
                        ClientInfo cInfo = (ClientInfo) NsdService.this.mClients.get(msg.replyTo);
                        if (cInfo != null) {
                            cInfo.expungeAllRequests();
                            NsdService.this.mClients.remove(msg.replyTo);
                        }
                        if (NsdService.this.mClients.size() == 0) {
                            NsdService.this.stopMDnsDaemon();
                        }
                        return NsdService.DBG;
                    case 393217:
                        NsdService.this.replyToMessage(msg, 393219, 0);
                        return NsdService.DBG;
                    case 393222:
                        NsdService.this.replyToMessage(msg, 393223, 0);
                        return NsdService.DBG;
                    case 393225:
                        NsdService.this.replyToMessage(msg, 393226, 0);
                        return NsdService.DBG;
                    case 393228:
                        NsdService.this.replyToMessage(msg, 393229, 0);
                        return NsdService.DBG;
                    case 393234:
                        NsdService.this.replyToMessage(msg, 393235, 0);
                        return NsdService.DBG;
                    default:
                        Slog.e(NsdService.TAG, "Unhandled " + msg);
                        return false;
                }
            }
        }

        class DisabledState extends State {
            DisabledState() {
            }

            public void enter() {
                NsdService.this.sendNsdStateChangeBroadcast(false);
            }

            public boolean processMessage(Message msg) {
                switch (msg.what) {
                    case 393240:
                        NsdStateMachine.this.transitionTo(NsdStateMachine.this.mEnabledState);
                        return NsdService.DBG;
                    default:
                        return false;
                }
            }
        }

        class EnabledState extends State {
            EnabledState() {
            }

            public void enter() {
                NsdService.this.sendNsdStateChangeBroadcast(NsdService.DBG);
                if (NsdService.this.mClients.size() > 0) {
                    NsdService.this.startMDnsDaemon();
                }
            }

            public void exit() {
                if (NsdService.this.mClients.size() > 0) {
                    NsdService.this.stopMDnsDaemon();
                }
            }

            private boolean requestLimitReached(ClientInfo clientInfo) {
                if (clientInfo.mClientIds.size() < 10) {
                    return false;
                }
                Slog.d(NsdService.TAG, "Exceeded max outstanding requests " + clientInfo);
                return NsdService.DBG;
            }

            private void storeRequestMap(int clientId, int globalId, ClientInfo clientInfo, int what) {
                clientInfo.mClientIds.put(clientId, Integer.valueOf(globalId));
                clientInfo.mClientRequests.put(clientId, Integer.valueOf(what));
                NsdService.this.mIdToClientInfoMap.put(globalId, clientInfo);
            }

            private void removeRequestMap(int clientId, int globalId, ClientInfo clientInfo) {
                clientInfo.mClientIds.remove(clientId);
                clientInfo.mClientRequests.remove(clientId);
                NsdService.this.mIdToClientInfoMap.remove(globalId);
            }

            public boolean processMessage(Message msg) {
                switch (msg.what) {
                    case 69632:
                        if (msg.arg1 == 0 && NsdService.this.mClients.size() == 0) {
                            NsdService.this.startMDnsDaemon();
                        }
                        break;
                    case 69636:
                        break;
                    case 393217:
                        Slog.d(NsdService.TAG, "Discover services");
                        NsdServiceInfo servInfo = (NsdServiceInfo) msg.obj;
                        ClientInfo clientInfo = (ClientInfo) NsdService.this.mClients.get(msg.replyTo);
                        if (requestLimitReached(clientInfo)) {
                            NsdService.this.replyToMessage(msg, 393219, 4);
                        } else {
                            int id = NsdService.this.getUniqueId();
                            if (!NsdService.this.discoverServices(id, servInfo.getServiceType())) {
                                NsdService.this.stopServiceDiscovery(id);
                                NsdService.this.replyToMessage(msg, 393219, 0);
                            } else {
                                Slog.d(NsdService.TAG, "Discover " + msg.arg2 + " " + id + servInfo.getServiceType());
                                storeRequestMap(msg.arg2, id, clientInfo, msg.what);
                                NsdService.this.replyToMessage(msg, 393218, servInfo);
                            }
                        }
                        break;
                    case 393222:
                        Slog.d(NsdService.TAG, "Stop service discovery");
                        ClientInfo clientInfo2 = (ClientInfo) NsdService.this.mClients.get(msg.replyTo);
                        try {
                            int id2 = ((Integer) clientInfo2.mClientIds.get(msg.arg2)).intValue();
                            removeRequestMap(msg.arg2, id2, clientInfo2);
                            if (NsdService.this.stopServiceDiscovery(id2)) {
                                NsdService.this.replyToMessage(msg, 393224);
                            } else {
                                NsdService.this.replyToMessage(msg, 393223, 0);
                            }
                        } catch (NullPointerException e) {
                            NsdService.this.replyToMessage(msg, 393223, 0);
                            return NsdService.DBG;
                        }
                        break;
                    case 393225:
                        Slog.d(NsdService.TAG, "Register service");
                        ClientInfo clientInfo3 = (ClientInfo) NsdService.this.mClients.get(msg.replyTo);
                        if (requestLimitReached(clientInfo3)) {
                            NsdService.this.replyToMessage(msg, 393226, 4);
                        } else {
                            int id3 = NsdService.this.getUniqueId();
                            if (!NsdService.this.registerService(id3, (NsdServiceInfo) msg.obj)) {
                                NsdService.this.unregisterService(id3);
                                NsdService.this.replyToMessage(msg, 393226, 0);
                            } else {
                                Slog.d(NsdService.TAG, "Register " + msg.arg2 + " " + id3);
                                storeRequestMap(msg.arg2, id3, clientInfo3, msg.what);
                            }
                        }
                        break;
                    case 393228:
                        Slog.d(NsdService.TAG, "unregister service");
                        ClientInfo clientInfo4 = (ClientInfo) NsdService.this.mClients.get(msg.replyTo);
                        try {
                            int id4 = ((Integer) clientInfo4.mClientIds.get(msg.arg2)).intValue();
                            removeRequestMap(msg.arg2, id4, clientInfo4);
                            if (NsdService.this.unregisterService(id4)) {
                                NsdService.this.replyToMessage(msg, 393230);
                            } else {
                                NsdService.this.replyToMessage(msg, 393229, 0);
                            }
                        } catch (NullPointerException e2) {
                            NsdService.this.replyToMessage(msg, 393229, 0);
                            return NsdService.DBG;
                        }
                        break;
                    case 393234:
                        Slog.d(NsdService.TAG, "Resolve service");
                        NsdServiceInfo servInfo2 = (NsdServiceInfo) msg.obj;
                        ClientInfo clientInfo5 = (ClientInfo) NsdService.this.mClients.get(msg.replyTo);
                        if (clientInfo5.mResolvedService != null) {
                            NsdService.this.replyToMessage(msg, 393235, 3);
                        } else {
                            int id5 = NsdService.this.getUniqueId();
                            if (!NsdService.this.resolveService(id5, servInfo2)) {
                                NsdService.this.replyToMessage(msg, 393235, 0);
                            } else {
                                clientInfo5.mResolvedService = new NsdServiceInfo();
                                storeRequestMap(msg.arg2, id5, clientInfo5, msg.what);
                            }
                        }
                        break;
                    case 393241:
                        NsdStateMachine.this.transitionTo(NsdStateMachine.this.mDisabledState);
                        break;
                    case 393242:
                        NativeEvent event = (NativeEvent) msg.obj;
                        if (!handleNativeEvent(event.code, event.raw, event.cooked)) {
                        }
                        break;
                }
                return NsdService.DBG;
            }

            private boolean handleNativeEvent(int code, String raw, String[] cooked) {
                boolean handled = NsdService.DBG;
                int id = Integer.parseInt(cooked[1]);
                ClientInfo clientInfo = (ClientInfo) NsdService.this.mIdToClientInfoMap.get(id);
                if (clientInfo == null) {
                    Slog.e(NsdService.TAG, "Unique id with no client mapping: " + id);
                    return false;
                }
                int clientId = clientInfo.getClientId(id);
                if (clientId < 0) {
                    Slog.d(NsdService.TAG, "Notification for a listener that is no longer active: " + id);
                    return false;
                }
                switch (code) {
                    case NativeResponseCode.SERVICE_DISCOVERY_FAILED:
                        Slog.d(NsdService.TAG, "SERVICE_DISC_FAILED Raw: " + raw);
                        clientInfo.mChannel.sendMessage(393219, 0, clientId);
                        break;
                    case NativeResponseCode.SERVICE_FOUND:
                        Slog.d(NsdService.TAG, "SERVICE_FOUND Raw: " + raw);
                        NsdServiceInfo servInfo = new NsdServiceInfo(cooked[2], cooked[3]);
                        clientInfo.mChannel.sendMessage(393220, 0, clientId, servInfo);
                        break;
                    case NativeResponseCode.SERVICE_LOST:
                        Slog.d(NsdService.TAG, "SERVICE_LOST Raw: " + raw);
                        NsdServiceInfo servInfo2 = new NsdServiceInfo(cooked[2], cooked[3]);
                        clientInfo.mChannel.sendMessage(393221, 0, clientId, servInfo2);
                        break;
                    case 605:
                        Slog.d(NsdService.TAG, "SERVICE_REGISTER_FAILED Raw: " + raw);
                        clientInfo.mChannel.sendMessage(393226, 0, clientId);
                        break;
                    case NativeResponseCode.SERVICE_REGISTERED:
                        Slog.d(NsdService.TAG, "SERVICE_REGISTERED Raw: " + raw);
                        NsdServiceInfo servInfo3 = new NsdServiceInfo(cooked[2], null);
                        clientInfo.mChannel.sendMessage(393227, id, clientId, servInfo3);
                        break;
                    case NativeResponseCode.SERVICE_RESOLUTION_FAILED:
                        Slog.d(NsdService.TAG, "SERVICE_RESOLVE_FAILED Raw: " + raw);
                        NsdService.this.stopResolveService(id);
                        removeRequestMap(clientId, id, clientInfo);
                        clientInfo.mResolvedService = null;
                        clientInfo.mChannel.sendMessage(393235, 0, clientId);
                        break;
                    case NativeResponseCode.SERVICE_RESOLVED:
                        Slog.d(NsdService.TAG, "SERVICE_RESOLVED Raw: " + raw);
                        int index = 0;
                        while (index < cooked[2].length() && cooked[2].charAt(index) != '.') {
                            if (cooked[2].charAt(index) == '\\') {
                                index++;
                            }
                            index++;
                        }
                        if (index >= cooked[2].length()) {
                            Slog.e(NsdService.TAG, "Invalid service found " + raw);
                        } else {
                            String name = cooked[2].substring(0, index);
                            String rest = cooked[2].substring(index);
                            String type = rest.replace(".local.", "");
                            clientInfo.mResolvedService.setServiceName(NsdService.this.unescape(name));
                            clientInfo.mResolvedService.setServiceType(type);
                            clientInfo.mResolvedService.setPort(Integer.parseInt(cooked[4]));
                            NsdService.this.stopResolveService(id);
                            removeRequestMap(clientId, id, clientInfo);
                            int id2 = NsdService.this.getUniqueId();
                            if (NsdService.this.getAddrInfo(id2, cooked[3])) {
                                storeRequestMap(clientId, id2, clientInfo, 393234);
                            } else {
                                clientInfo.mChannel.sendMessage(393235, 0, clientId);
                                clientInfo.mResolvedService = null;
                            }
                        }
                        break;
                    case NativeResponseCode.SERVICE_UPDATED:
                    case NativeResponseCode.SERVICE_UPDATE_FAILED:
                        break;
                    case NativeResponseCode.SERVICE_GET_ADDR_FAILED:
                        NsdService.this.stopGetAddrInfo(id);
                        removeRequestMap(clientId, id, clientInfo);
                        clientInfo.mResolvedService = null;
                        Slog.d(NsdService.TAG, "SERVICE_RESOLVE_FAILED Raw: " + raw);
                        clientInfo.mChannel.sendMessage(393235, 0, clientId);
                        break;
                    case NativeResponseCode.SERVICE_GET_ADDR_SUCCESS:
                        Slog.d(NsdService.TAG, "SERVICE_GET_ADDR_SUCCESS Raw: " + raw);
                        try {
                            clientInfo.mResolvedService.setHost(InetAddress.getByName(cooked[4]));
                            clientInfo.mChannel.sendMessage(393236, 0, clientId, clientInfo.mResolvedService);
                        } catch (UnknownHostException e) {
                            clientInfo.mChannel.sendMessage(393235, 0, clientId);
                        }
                        NsdService.this.stopGetAddrInfo(id);
                        removeRequestMap(clientId, id, clientInfo);
                        clientInfo.mResolvedService = null;
                        break;
                    default:
                        handled = false;
                        break;
                }
                return handled;
            }
        }
    }

    private String unescape(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        int i = 0;
        while (true) {
            if (i >= s.length()) {
                break;
            }
            char c = s.charAt(i);
            if (c == '\\') {
                i++;
                if (i >= s.length()) {
                    Slog.e(TAG, "Unexpected end of escape sequence in: " + s);
                    break;
                }
                c = s.charAt(i);
                if (c != '.' && c != '\\') {
                    if (i + 2 >= s.length()) {
                        Slog.e(TAG, "Unexpected end of escape sequence in: " + s);
                        break;
                    }
                    c = (char) (((c - '0') * 100) + ((s.charAt(i + 1) - '0') * 10) + (s.charAt(i + 2) - '0'));
                    i += 2;
                }
            }
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    private NsdService(Context context) {
        this.mContext = context;
        this.mContentResolver = context.getContentResolver();
        this.mNsdStateMachine.start();
        Thread th = new Thread(this.mNativeConnector, MDNS_TAG);
        th.start();
    }

    public static NsdService create(Context context) throws InterruptedException {
        NsdService service = new NsdService(context);
        service.mNativeDaemonConnected.await();
        return service;
    }

    public Messenger getMessenger() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.INTERNET", TAG);
        return new Messenger(this.mNsdStateMachine.getHandler());
    }

    public void setEnabled(boolean enable) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        Settings.Global.putInt(this.mContentResolver, "nsd_on", enable ? 1 : 0);
        if (enable) {
            this.mNsdStateMachine.sendMessage(393240);
        } else {
            this.mNsdStateMachine.sendMessage(393241);
        }
    }

    private void sendNsdStateChangeBroadcast(boolean enabled) {
        Intent intent = new Intent("android.net.nsd.STATE_CHANGED");
        intent.addFlags(67108864);
        if (enabled) {
            intent.putExtra("nsd_state", 2);
        } else {
            intent.putExtra("nsd_state", 1);
        }
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    private boolean isNsdEnabled() {
        boolean ret = DBG;
        if (Settings.Global.getInt(this.mContentResolver, "nsd_on", 1) != 1) {
            ret = false;
        }
        Slog.d(TAG, "Network service discovery enabled " + ret);
        return ret;
    }

    private int getUniqueId() {
        int i = this.mUniqueId + 1;
        this.mUniqueId = i;
        if (i != this.INVALID_ID) {
            return this.mUniqueId;
        }
        int i2 = this.mUniqueId + 1;
        this.mUniqueId = i2;
        return i2;
    }

    class NativeResponseCode {
        public static final int SERVICE_DISCOVERY_FAILED = 602;
        public static final int SERVICE_FOUND = 603;
        public static final int SERVICE_GET_ADDR_FAILED = 611;
        public static final int SERVICE_GET_ADDR_SUCCESS = 612;
        public static final int SERVICE_LOST = 604;
        public static final int SERVICE_REGISTERED = 606;
        public static final int SERVICE_REGISTRATION_FAILED = 605;
        public static final int SERVICE_RESOLUTION_FAILED = 607;
        public static final int SERVICE_RESOLVED = 608;
        public static final int SERVICE_UPDATED = 609;
        public static final int SERVICE_UPDATE_FAILED = 610;

        NativeResponseCode() {
        }
    }

    private class NativeEvent {
        final int code;
        final String[] cooked;
        final String raw;

        NativeEvent(int code, String raw, String[] cooked) {
            this.code = code;
            this.raw = raw;
            this.cooked = cooked;
        }
    }

    class NativeCallbackReceiver implements INativeDaemonConnectorCallbacks {
        NativeCallbackReceiver() {
        }

        @Override
        public void onDaemonConnected() {
            NsdService.this.mNativeDaemonConnected.countDown();
        }

        @Override
        public boolean onCheckHoldWakeLock(int code) {
            return false;
        }

        @Override
        public boolean onEvent(int code, String raw, String[] cooked) {
            NativeEvent event = NsdService.this.new NativeEvent(code, raw, cooked);
            NsdService.this.mNsdStateMachine.sendMessage(393242, event);
            return NsdService.DBG;
        }
    }

    private boolean startMDnsDaemon() {
        Slog.d(TAG, "startMDnsDaemon");
        try {
            this.mNativeConnector.execute("mdnssd", "start-service");
            return DBG;
        } catch (NativeDaemonConnectorException e) {
            Slog.e(TAG, "Failed to start daemon" + e);
            return false;
        }
    }

    private boolean stopMDnsDaemon() {
        Slog.d(TAG, "stopMDnsDaemon");
        try {
            this.mNativeConnector.execute("mdnssd", "stop-service");
            return DBG;
        } catch (NativeDaemonConnectorException e) {
            Slog.e(TAG, "Failed to start daemon" + e);
            return false;
        }
    }

    private boolean registerService(int regId, NsdServiceInfo service) {
        Slog.d(TAG, "registerService: " + regId + " " + service);
        try {
            NativeDaemonConnector.Command cmd = new NativeDaemonConnector.Command("mdnssd", "register", Integer.valueOf(regId), service.getServiceName(), service.getServiceType(), Integer.valueOf(service.getPort()));
            Map<String, byte[]> txtRecords = service.getAttributes();
            for (String key : txtRecords.keySet()) {
                try {
                    byte[] recordValue = txtRecords.get(key);
                    Locale locale = Locale.US;
                    Object[] objArr = new Object[2];
                    objArr[0] = key;
                    objArr[1] = recordValue != null ? new String(recordValue, "UTF_8") : "";
                    cmd.appendArg(String.format(locale, "%s=%s", objArr));
                } catch (UnsupportedEncodingException e) {
                    Slog.e(TAG, "Failed to encode txtRecord " + e);
                }
            }
            this.mNativeConnector.execute(cmd);
            return DBG;
        } catch (NativeDaemonConnectorException e2) {
            Slog.e(TAG, "Failed to execute registerService " + e2);
            return false;
        }
    }

    private boolean unregisterService(int regId) {
        Slog.d(TAG, "unregisterService: " + regId);
        try {
            this.mNativeConnector.execute("mdnssd", "stop-register", Integer.valueOf(regId));
            return DBG;
        } catch (NativeDaemonConnectorException e) {
            Slog.e(TAG, "Failed to execute unregisterService " + e);
            return false;
        }
    }

    private boolean updateService(int regId, DnsSdTxtRecord t) {
        Slog.d(TAG, "updateService: " + regId + " " + t);
        if (t == null) {
            return false;
        }
        try {
            this.mNativeConnector.execute("mdnssd", "update", Integer.valueOf(regId), Integer.valueOf(t.size()), t.getRawData());
            return DBG;
        } catch (NativeDaemonConnectorException e) {
            Slog.e(TAG, "Failed to updateServices " + e);
            return false;
        }
    }

    private boolean discoverServices(int discoveryId, String serviceType) {
        Slog.d(TAG, "discoverServices: " + discoveryId + " " + serviceType);
        try {
            this.mNativeConnector.execute("mdnssd", "discover", Integer.valueOf(discoveryId), serviceType);
            return DBG;
        } catch (NativeDaemonConnectorException e) {
            Slog.e(TAG, "Failed to discoverServices " + e);
            return false;
        }
    }

    private boolean stopServiceDiscovery(int discoveryId) {
        Slog.d(TAG, "stopServiceDiscovery: " + discoveryId);
        try {
            this.mNativeConnector.execute("mdnssd", "stop-discover", Integer.valueOf(discoveryId));
            return DBG;
        } catch (NativeDaemonConnectorException e) {
            Slog.e(TAG, "Failed to stopServiceDiscovery " + e);
            return false;
        }
    }

    private boolean resolveService(int resolveId, NsdServiceInfo service) {
        Slog.d(TAG, "resolveService: " + resolveId + " " + service);
        try {
            this.mNativeConnector.execute("mdnssd", "resolve", Integer.valueOf(resolveId), service.getServiceName(), service.getServiceType(), "local.");
            return DBG;
        } catch (NativeDaemonConnectorException e) {
            Slog.e(TAG, "Failed to resolveService " + e);
            return false;
        }
    }

    private boolean stopResolveService(int resolveId) {
        Slog.d(TAG, "stopResolveService: " + resolveId);
        try {
            this.mNativeConnector.execute("mdnssd", "stop-resolve", Integer.valueOf(resolveId));
            return DBG;
        } catch (NativeDaemonConnectorException e) {
            Slog.e(TAG, "Failed to stop resolve " + e);
            return false;
        }
    }

    private boolean getAddrInfo(int resolveId, String hostname) {
        Slog.d(TAG, "getAdddrInfo: " + resolveId);
        try {
            this.mNativeConnector.execute("mdnssd", "getaddrinfo", Integer.valueOf(resolveId), hostname);
            return DBG;
        } catch (NativeDaemonConnectorException e) {
            Slog.e(TAG, "Failed to getAddrInfo " + e);
            return false;
        }
    }

    private boolean stopGetAddrInfo(int resolveId) {
        Slog.d(TAG, "stopGetAdddrInfo: " + resolveId);
        try {
            this.mNativeConnector.execute("mdnssd", "stop-getaddrinfo", Integer.valueOf(resolveId));
            return DBG;
        } catch (NativeDaemonConnectorException e) {
            Slog.e(TAG, "Failed to stopGetAddrInfo " + e);
            return false;
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            pw.println("Permission Denial: can't dump ServiceDiscoverService from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            return;
        }
        for (ClientInfo client : this.mClients.values()) {
            pw.println("Client Info");
            pw.println(client);
        }
        this.mNsdStateMachine.dump(fd, pw, args);
    }

    private Message obtainMessage(Message srcMsg) {
        Message msg = Message.obtain();
        msg.arg2 = srcMsg.arg2;
        return msg;
    }

    private void replyToMessage(Message msg, int what) {
        if (msg.replyTo != null) {
            Message dstMsg = obtainMessage(msg);
            dstMsg.what = what;
            this.mReplyChannel.replyToMessage(msg, dstMsg);
        }
    }

    private void replyToMessage(Message msg, int what, int arg1) {
        if (msg.replyTo != null) {
            Message dstMsg = obtainMessage(msg);
            dstMsg.what = what;
            dstMsg.arg1 = arg1;
            this.mReplyChannel.replyToMessage(msg, dstMsg);
        }
    }

    private void replyToMessage(Message msg, int what, Object obj) {
        if (msg.replyTo != null) {
            Message dstMsg = obtainMessage(msg);
            dstMsg.what = what;
            dstMsg.obj = obj;
            this.mReplyChannel.replyToMessage(msg, dstMsg);
        }
    }

    private class ClientInfo {
        private static final int MAX_LIMIT = 10;
        private final AsyncChannel mChannel;
        private SparseArray<Integer> mClientIds;
        private SparseArray<Integer> mClientRequests;
        private final Messenger mMessenger;
        private NsdServiceInfo mResolvedService;

        private ClientInfo(AsyncChannel c, Messenger m) {
            this.mClientIds = new SparseArray<>();
            this.mClientRequests = new SparseArray<>();
            this.mChannel = c;
            this.mMessenger = m;
            Slog.d(NsdService.TAG, "New client, channel: " + c + " messenger: " + m);
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("mChannel ").append(this.mChannel).append("\n");
            sb.append("mMessenger ").append(this.mMessenger).append("\n");
            sb.append("mResolvedService ").append(this.mResolvedService).append("\n");
            for (int i = 0; i < this.mClientIds.size(); i++) {
                int clientID = this.mClientIds.keyAt(i);
                sb.append("clientId ").append(clientID).append(" mDnsId ").append(this.mClientIds.valueAt(i)).append(" type ").append(this.mClientRequests.get(clientID)).append("\n");
            }
            return sb.toString();
        }

        private void expungeAllRequests() {
            for (int i = 0; i < this.mClientIds.size(); i++) {
                int clientId = this.mClientIds.keyAt(i);
                int globalId = this.mClientIds.valueAt(i).intValue();
                NsdService.this.mIdToClientInfoMap.remove(globalId);
                Slog.d(NsdService.TAG, "Terminating client-ID " + clientId + " global-ID " + globalId + " type " + this.mClientRequests.get(clientId));
                switch (this.mClientRequests.get(clientId).intValue()) {
                    case 393217:
                        NsdService.this.stopServiceDiscovery(globalId);
                        break;
                    case 393225:
                        NsdService.this.unregisterService(globalId);
                        break;
                    case 393234:
                        NsdService.this.stopResolveService(globalId);
                        break;
                }
            }
            this.mClientIds.clear();
            this.mClientRequests.clear();
        }

        private int getClientId(int globalId) {
            int nSize = this.mClientIds.size();
            for (int i = 0; i < nSize; i++) {
                int mDnsId = this.mClientIds.valueAt(i).intValue();
                if (globalId == mDnsId) {
                    return this.mClientIds.keyAt(i);
                }
            }
            return -1;
        }
    }
}
