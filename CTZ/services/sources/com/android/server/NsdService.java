package com.android.server;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.net.nsd.DnsSdTxtRecord;
import android.net.nsd.INsdManager;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Messenger;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Base64;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.NsdService;
import com.android.server.backup.BackupManagerConstants;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

public class NsdService extends INsdManager.Stub {
    private static final boolean DBG = true;
    private static final int INVALID_ID = 0;
    private static final String MDNS_TAG = "mDnsConnector";
    private static final String TAG = "NsdService";
    private final Context mContext;
    private final DaemonConnection mDaemon;
    private final NativeCallbackReceiver mDaemonCallback;
    private final NsdSettings mNsdSettings;
    private final NsdStateMachine mNsdStateMachine;
    private final HashMap<Messenger, ClientInfo> mClients = new HashMap<>();
    private final SparseArray<ClientInfo> mIdToClientInfoMap = new SparseArray<>();
    private final AsyncChannel mReplyChannel = new AsyncChannel();
    private int mUniqueId = 1;

    interface DaemonConnectionSupplier {
        DaemonConnection get(NativeCallbackReceiver nativeCallbackReceiver);
    }

    private class NsdStateMachine extends StateMachine {
        private final DefaultState mDefaultState;
        private final DisabledState mDisabledState;
        private final EnabledState mEnabledState;

        protected String getWhatToString(int i) {
            return NsdManager.nameOf(i);
        }

        private void registerForNsdSetting() {
            ContentObserver contentObserver = new ContentObserver(getHandler()) {
                @Override
                public void onChange(boolean z) {
                    NsdService.this.notifyEnabled(NsdService.this.isNsdEnabled());
                }
            };
            NsdService.this.mNsdSettings.registerContentObserver(Settings.Global.getUriFor("nsd_on"), contentObserver);
        }

        NsdStateMachine(String str, Handler handler) {
            super(str, handler);
            this.mDefaultState = new DefaultState();
            this.mDisabledState = new DisabledState();
            this.mEnabledState = new EnabledState();
            addState(this.mDefaultState);
            addState(this.mDisabledState, this.mDefaultState);
            addState(this.mEnabledState, this.mDefaultState);
            setInitialState(NsdService.this.isNsdEnabled() ? this.mEnabledState : this.mDisabledState);
            setLogRecSize(25);
            registerForNsdSetting();
        }

        class DefaultState extends State {
            DefaultState() {
            }

            public boolean processMessage(Message message) {
                switch (message.what) {
                    case 69632:
                        if (message.arg1 == 0) {
                            AsyncChannel asyncChannel = (AsyncChannel) message.obj;
                            Slog.d(NsdService.TAG, "New client listening to asynchronous messages");
                            asyncChannel.sendMessage(69634);
                            NsdService.this.mClients.put(message.replyTo, new ClientInfo(asyncChannel, message.replyTo));
                            return true;
                        }
                        Slog.e(NsdService.TAG, "Client connection failure, error=" + message.arg1);
                        return true;
                    case 69633:
                        new AsyncChannel().connect(NsdService.this.mContext, NsdStateMachine.this.getHandler(), message.replyTo);
                        return true;
                    case 69636:
                        int i = message.arg1;
                        if (i == 2) {
                            Slog.e(NsdService.TAG, "Send failed, client connection lost");
                        } else if (i == 4) {
                            Slog.d(NsdService.TAG, "Client disconnected");
                        } else {
                            Slog.d(NsdService.TAG, "Client connection lost with reason: " + message.arg1);
                        }
                        ClientInfo clientInfo = (ClientInfo) NsdService.this.mClients.get(message.replyTo);
                        if (clientInfo != null) {
                            clientInfo.expungeAllRequests();
                            NsdService.this.mClients.remove(message.replyTo);
                        }
                        if (NsdService.this.mClients.size() == 0) {
                            NsdService.this.mDaemon.stop();
                            return true;
                        }
                        return true;
                    case 393217:
                        NsdService.this.replyToMessage(message, 393219, 0);
                        return true;
                    case 393222:
                        NsdService.this.replyToMessage(message, 393223, 0);
                        return true;
                    case 393225:
                        NsdService.this.replyToMessage(message, 393226, 0);
                        return true;
                    case 393228:
                        NsdService.this.replyToMessage(message, 393229, 0);
                        return true;
                    case 393234:
                        NsdService.this.replyToMessage(message, 393235, 0);
                        return true;
                    default:
                        Slog.e(NsdService.TAG, "Unhandled " + message);
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

            public boolean processMessage(Message message) {
                if (message.what == 393240) {
                    NsdStateMachine.this.transitionTo(NsdStateMachine.this.mEnabledState);
                    return true;
                }
                return false;
            }
        }

        class EnabledState extends State {
            EnabledState() {
            }

            public void enter() {
                NsdService.this.sendNsdStateChangeBroadcast(true);
                if (NsdService.this.mClients.size() > 0) {
                    NsdService.this.mDaemon.start();
                }
            }

            public void exit() {
                if (NsdService.this.mClients.size() > 0) {
                    NsdService.this.mDaemon.stop();
                }
            }

            private boolean requestLimitReached(ClientInfo clientInfo) {
                if (clientInfo.mClientIds.size() >= 10) {
                    Slog.d(NsdService.TAG, "Exceeded max outstanding requests " + clientInfo);
                    return true;
                }
                return false;
            }

            private void storeRequestMap(int i, int i2, ClientInfo clientInfo, int i3) {
                clientInfo.mClientIds.put(i, i2);
                clientInfo.mClientRequests.put(i, i3);
                NsdService.this.mIdToClientInfoMap.put(i2, clientInfo);
            }

            private void removeRequestMap(int i, int i2, ClientInfo clientInfo) {
                clientInfo.mClientIds.delete(i);
                clientInfo.mClientRequests.delete(i);
                NsdService.this.mIdToClientInfoMap.remove(i2);
            }

            public boolean processMessage(Message message) {
                switch (message.what) {
                    case 69632:
                        if (message.arg1 == 0 && NsdService.this.mClients.size() == 0) {
                            NsdService.this.mDaemon.start();
                        }
                        break;
                    case 69636:
                        break;
                    case 393217:
                        Slog.d(NsdService.TAG, "Discover services");
                        NsdServiceInfo nsdServiceInfo = (NsdServiceInfo) message.obj;
                        ClientInfo clientInfo = (ClientInfo) NsdService.this.mClients.get(message.replyTo);
                        if (requestLimitReached(clientInfo)) {
                            NsdService.this.replyToMessage(message, 393219, 4);
                        } else {
                            int uniqueId = NsdService.this.getUniqueId();
                            if (!NsdService.this.discoverServices(uniqueId, nsdServiceInfo.getServiceType())) {
                                NsdService.this.stopServiceDiscovery(uniqueId);
                                NsdService.this.replyToMessage(message, 393219, 0);
                            } else {
                                Slog.d(NsdService.TAG, "Discover " + message.arg2 + " " + uniqueId + nsdServiceInfo.getServiceType());
                                storeRequestMap(message.arg2, uniqueId, clientInfo, message.what);
                                NsdService.this.replyToMessage(message, 393218, nsdServiceInfo);
                            }
                        }
                        break;
                    case 393222:
                        Slog.d(NsdService.TAG, "Stop service discovery");
                        ClientInfo clientInfo2 = (ClientInfo) NsdService.this.mClients.get(message.replyTo);
                        try {
                            int i = clientInfo2.mClientIds.get(message.arg2);
                            removeRequestMap(message.arg2, i, clientInfo2);
                            if (NsdService.this.stopServiceDiscovery(i)) {
                                NsdService.this.replyToMessage(message, 393224);
                            } else {
                                NsdService.this.replyToMessage(message, 393223, 0);
                            }
                        } catch (NullPointerException e) {
                            NsdService.this.replyToMessage(message, 393223, 0);
                            return true;
                        }
                        break;
                    case 393225:
                        Slog.d(NsdService.TAG, "Register service");
                        ClientInfo clientInfo3 = (ClientInfo) NsdService.this.mClients.get(message.replyTo);
                        if (requestLimitReached(clientInfo3)) {
                            NsdService.this.replyToMessage(message, 393226, 4);
                        } else {
                            int uniqueId2 = NsdService.this.getUniqueId();
                            if (!NsdService.this.registerService(uniqueId2, (NsdServiceInfo) message.obj)) {
                                NsdService.this.unregisterService(uniqueId2);
                                NsdService.this.replyToMessage(message, 393226, 0);
                            } else {
                                Slog.d(NsdService.TAG, "Register " + message.arg2 + " " + uniqueId2);
                                storeRequestMap(message.arg2, uniqueId2, clientInfo3, message.what);
                            }
                        }
                        break;
                    case 393228:
                        Slog.d(NsdService.TAG, "unregister service");
                        ClientInfo clientInfo4 = (ClientInfo) NsdService.this.mClients.get(message.replyTo);
                        try {
                            int i2 = clientInfo4.mClientIds.get(message.arg2);
                            removeRequestMap(message.arg2, i2, clientInfo4);
                            if (NsdService.this.unregisterService(i2)) {
                                NsdService.this.replyToMessage(message, 393230);
                            } else {
                                NsdService.this.replyToMessage(message, 393229, 0);
                            }
                        } catch (NullPointerException e2) {
                            NsdService.this.replyToMessage(message, 393229, 0);
                            return true;
                        }
                        break;
                    case 393234:
                        Slog.d(NsdService.TAG, "Resolve service");
                        NsdServiceInfo nsdServiceInfo2 = (NsdServiceInfo) message.obj;
                        ClientInfo clientInfo5 = (ClientInfo) NsdService.this.mClients.get(message.replyTo);
                        if (clientInfo5.mResolvedService != null) {
                            NsdService.this.replyToMessage(message, 393235, 3);
                        } else {
                            int uniqueId3 = NsdService.this.getUniqueId();
                            if (!NsdService.this.resolveService(uniqueId3, nsdServiceInfo2)) {
                                NsdService.this.replyToMessage(message, 393235, 0);
                            } else {
                                clientInfo5.mResolvedService = new NsdServiceInfo();
                                storeRequestMap(message.arg2, uniqueId3, clientInfo5, message.what);
                            }
                        }
                        break;
                    case 393241:
                        NsdStateMachine.this.transitionTo(NsdStateMachine.this.mDisabledState);
                        break;
                    case 393242:
                        NativeEvent nativeEvent = (NativeEvent) message.obj;
                        if (!handleNativeEvent(nativeEvent.code, nativeEvent.raw, nativeEvent.cooked)) {
                        }
                        break;
                }
                return false;
            }

            private boolean handleNativeEvent(int i, String str, String[] strArr) {
                int i2 = Integer.parseInt(strArr[1]);
                ClientInfo clientInfo = (ClientInfo) NsdService.this.mIdToClientInfoMap.get(i2);
                if (clientInfo == null) {
                    Slog.e(NsdService.TAG, String.format("id %d for %s has no client mapping", Integer.valueOf(i2), NativeResponseCode.nameOf(i)));
                    return false;
                }
                int clientId = clientInfo.getClientId(i2);
                if (clientId < 0) {
                    Slog.d(NsdService.TAG, String.format("Notification %s for listener id %d that is no longer active", NativeResponseCode.nameOf(i), Integer.valueOf(i2)));
                    return false;
                }
                Slog.d(NsdService.TAG, String.format("Native daemon message %s: %s", NativeResponseCode.nameOf(i), str));
                switch (i) {
                    case NativeResponseCode.SERVICE_DISCOVERY_FAILED:
                        clientInfo.mChannel.sendMessage(393219, 0, clientId);
                        return true;
                    case NativeResponseCode.SERVICE_FOUND:
                        clientInfo.mChannel.sendMessage(393220, 0, clientId, new NsdServiceInfo(strArr[2], strArr[3]));
                        return true;
                    case NativeResponseCode.SERVICE_LOST:
                        clientInfo.mChannel.sendMessage(393221, 0, clientId, new NsdServiceInfo(strArr[2], strArr[3]));
                        return true;
                    case NativeResponseCode.SERVICE_REGISTRATION_FAILED:
                        clientInfo.mChannel.sendMessage(393226, 0, clientId);
                        return true;
                    case NativeResponseCode.SERVICE_REGISTERED:
                        clientInfo.mChannel.sendMessage(393227, i2, clientId, new NsdServiceInfo(strArr[2], null));
                        return true;
                    case NativeResponseCode.SERVICE_RESOLUTION_FAILED:
                        NsdService.this.stopResolveService(i2);
                        removeRequestMap(clientId, i2, clientInfo);
                        clientInfo.mResolvedService = null;
                        clientInfo.mChannel.sendMessage(393235, 0, clientId);
                        return true;
                    case NativeResponseCode.SERVICE_RESOLVED:
                        int i3 = 0;
                        while (i3 < strArr[2].length() && strArr[2].charAt(i3) != '.') {
                            if (strArr[2].charAt(i3) == '\\') {
                                i3++;
                            }
                            i3++;
                        }
                        if (i3 >= strArr[2].length()) {
                            Slog.e(NsdService.TAG, "Invalid service found " + str);
                        } else {
                            String strSubstring = strArr[2].substring(0, i3);
                            String strReplace = strArr[2].substring(i3).replace(".local.", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                            clientInfo.mResolvedService.setServiceName(NsdService.this.unescape(strSubstring));
                            clientInfo.mResolvedService.setServiceType(strReplace);
                            clientInfo.mResolvedService.setPort(Integer.parseInt(strArr[4]));
                            clientInfo.mResolvedService.setTxtRecords(strArr[6]);
                            NsdService.this.stopResolveService(i2);
                            removeRequestMap(clientId, i2, clientInfo);
                            int uniqueId = NsdService.this.getUniqueId();
                            if (NsdService.this.getAddrInfo(uniqueId, strArr[3])) {
                                storeRequestMap(clientId, uniqueId, clientInfo, 393234);
                            } else {
                                clientInfo.mChannel.sendMessage(393235, 0, clientId);
                                clientInfo.mResolvedService = null;
                            }
                        }
                        return true;
                    case NativeResponseCode.SERVICE_UPDATED:
                    case NativeResponseCode.SERVICE_UPDATE_FAILED:
                        return true;
                    case NativeResponseCode.SERVICE_GET_ADDR_FAILED:
                        NsdService.this.stopGetAddrInfo(i2);
                        removeRequestMap(clientId, i2, clientInfo);
                        clientInfo.mResolvedService = null;
                        clientInfo.mChannel.sendMessage(393235, 0, clientId);
                        return true;
                    case NativeResponseCode.SERVICE_GET_ADDR_SUCCESS:
                        try {
                            clientInfo.mResolvedService.setHost(InetAddress.getByName(strArr[4]));
                            clientInfo.mChannel.sendMessage(393236, 0, clientId, clientInfo.mResolvedService);
                            break;
                        } catch (UnknownHostException e) {
                            clientInfo.mChannel.sendMessage(393235, 0, clientId);
                        }
                        NsdService.this.stopGetAddrInfo(i2);
                        removeRequestMap(clientId, i2, clientInfo);
                        clientInfo.mResolvedService = null;
                        return true;
                    default:
                        return false;
                }
            }
        }
    }

    private String unescape(String str) {
        StringBuilder sb = new StringBuilder(str.length());
        int i = 0;
        while (true) {
            if (i >= str.length()) {
                break;
            }
            char cCharAt = str.charAt(i);
            if (cCharAt == '\\') {
                i++;
                if (i >= str.length()) {
                    Slog.e(TAG, "Unexpected end of escape sequence in: " + str);
                    break;
                }
                cCharAt = str.charAt(i);
                if (cCharAt != '.' && cCharAt != '\\') {
                    int i2 = i + 2;
                    if (i2 >= str.length()) {
                        Slog.e(TAG, "Unexpected end of escape sequence in: " + str);
                        break;
                    }
                    cCharAt = (char) (((cCharAt - '0') * 100) + ((str.charAt(i + 1) - '0') * 10) + (str.charAt(i2) - '0'));
                    i = i2;
                }
            }
            sb.append(cCharAt);
            i++;
        }
        return sb.toString();
    }

    @VisibleForTesting
    NsdService(Context context, NsdSettings nsdSettings, Handler handler, DaemonConnectionSupplier daemonConnectionSupplier) {
        this.mContext = context;
        this.mNsdSettings = nsdSettings;
        this.mNsdStateMachine = new NsdStateMachine(TAG, handler);
        this.mNsdStateMachine.start();
        this.mDaemonCallback = new NativeCallbackReceiver();
        this.mDaemon = daemonConnectionSupplier.get(this.mDaemonCallback);
    }

    public static NsdService create(Context context) throws InterruptedException {
        NsdSettings nsdSettingsMakeDefault = NsdSettings.makeDefault(context);
        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        NsdService nsdService = new NsdService(context, nsdSettingsMakeDefault, new Handler(handlerThread.getLooper()), new DaemonConnectionSupplier() {
            @Override
            public final NsdService.DaemonConnection get(NsdService.NativeCallbackReceiver nativeCallbackReceiver) {
                return new NsdService.DaemonConnection(nativeCallbackReceiver);
            }
        });
        nsdService.mDaemonCallback.awaitConnection();
        return nsdService;
    }

    public Messenger getMessenger() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.INTERNET", TAG);
        return new Messenger(this.mNsdStateMachine.getHandler());
    }

    public void setEnabled(boolean z) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        this.mNsdSettings.putEnabledStatus(z);
        notifyEnabled(z);
    }

    private void notifyEnabled(boolean z) {
        this.mNsdStateMachine.sendMessage(z ? 393240 : 393241);
    }

    private void sendNsdStateChangeBroadcast(boolean z) {
        Intent intent = new Intent("android.net.nsd.STATE_CHANGED");
        intent.addFlags(67108864);
        intent.putExtra("nsd_state", z ? 2 : 1);
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    private boolean isNsdEnabled() {
        boolean zIsEnabled = this.mNsdSettings.isEnabled();
        StringBuilder sb = new StringBuilder();
        sb.append("Network service discovery is ");
        sb.append(zIsEnabled ? "enabled" : "disabled");
        Slog.d(TAG, sb.toString());
        return zIsEnabled;
    }

    private int getUniqueId() {
        int i = this.mUniqueId + 1;
        this.mUniqueId = i;
        if (i != 0) {
            return this.mUniqueId;
        }
        int i2 = this.mUniqueId + 1;
        this.mUniqueId = i2;
        return i2;
    }

    static final class NativeResponseCode {
        private static final SparseArray<String> CODE_NAMES = new SparseArray<>();
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

        static {
            CODE_NAMES.put(SERVICE_DISCOVERY_FAILED, "SERVICE_DISCOVERY_FAILED");
            CODE_NAMES.put(SERVICE_FOUND, "SERVICE_FOUND");
            CODE_NAMES.put(SERVICE_LOST, "SERVICE_LOST");
            CODE_NAMES.put(SERVICE_REGISTRATION_FAILED, "SERVICE_REGISTRATION_FAILED");
            CODE_NAMES.put(SERVICE_REGISTERED, "SERVICE_REGISTERED");
            CODE_NAMES.put(SERVICE_RESOLUTION_FAILED, "SERVICE_RESOLUTION_FAILED");
            CODE_NAMES.put(SERVICE_RESOLVED, "SERVICE_RESOLVED");
            CODE_NAMES.put(SERVICE_UPDATED, "SERVICE_UPDATED");
            CODE_NAMES.put(SERVICE_UPDATE_FAILED, "SERVICE_UPDATE_FAILED");
            CODE_NAMES.put(SERVICE_GET_ADDR_FAILED, "SERVICE_GET_ADDR_FAILED");
            CODE_NAMES.put(SERVICE_GET_ADDR_SUCCESS, "SERVICE_GET_ADDR_SUCCESS");
        }

        static String nameOf(int i) {
            String str = CODE_NAMES.get(i);
            if (str == null) {
                return Integer.toString(i);
            }
            return str;
        }
    }

    private class NativeEvent {
        final int code;
        final String[] cooked;
        final String raw;

        NativeEvent(int i, String str, String[] strArr) {
            this.code = i;
            this.raw = str;
            this.cooked = strArr;
        }
    }

    class NativeCallbackReceiver implements INativeDaemonConnectorCallbacks {
        private final CountDownLatch connected = new CountDownLatch(1);

        NativeCallbackReceiver() {
        }

        public void awaitConnection() throws InterruptedException {
            this.connected.await();
        }

        @Override
        public void onDaemonConnected() {
            this.connected.countDown();
        }

        @Override
        public boolean onCheckHoldWakeLock(int i) {
            return false;
        }

        @Override
        public boolean onEvent(int i, String str, String[] strArr) {
            NsdService.this.mNsdStateMachine.sendMessage(393242, NsdService.this.new NativeEvent(i, str, strArr));
            return true;
        }
    }

    @VisibleForTesting
    public static class DaemonConnection {
        final NativeDaemonConnector mNativeConnector;

        DaemonConnection(NativeCallbackReceiver nativeCallbackReceiver) {
            this.mNativeConnector = new NativeDaemonConnector(nativeCallbackReceiver, "mdns", 10, NsdService.MDNS_TAG, 25, null);
            new Thread(this.mNativeConnector, NsdService.MDNS_TAG).start();
        }

        public boolean execute(Object... objArr) {
            Slog.d(NsdService.TAG, "mdnssd " + Arrays.toString(objArr));
            try {
                this.mNativeConnector.execute("mdnssd", objArr);
                return true;
            } catch (NativeDaemonConnectorException e) {
                Slog.e(NsdService.TAG, "Failed to execute mdnssd " + Arrays.toString(objArr), e);
                return false;
            }
        }

        public void start() {
            execute("start-service");
        }

        public void stop() {
            execute("stop-service");
        }
    }

    private boolean registerService(int i, NsdServiceInfo nsdServiceInfo) {
        Slog.d(TAG, "registerService: " + i + " " + nsdServiceInfo);
        return this.mDaemon.execute("register", Integer.valueOf(i), nsdServiceInfo.getServiceName(), nsdServiceInfo.getServiceType(), Integer.valueOf(nsdServiceInfo.getPort()), Base64.encodeToString(nsdServiceInfo.getTxtRecord(), 0).replace("\n", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS));
    }

    private boolean unregisterService(int i) {
        return this.mDaemon.execute("stop-register", Integer.valueOf(i));
    }

    private boolean updateService(int i, DnsSdTxtRecord dnsSdTxtRecord) {
        if (dnsSdTxtRecord == null) {
            return false;
        }
        return this.mDaemon.execute("update", Integer.valueOf(i), Integer.valueOf(dnsSdTxtRecord.size()), dnsSdTxtRecord.getRawData());
    }

    private boolean discoverServices(int i, String str) {
        return this.mDaemon.execute("discover", Integer.valueOf(i), str);
    }

    private boolean stopServiceDiscovery(int i) {
        return this.mDaemon.execute("stop-discover", Integer.valueOf(i));
    }

    private boolean resolveService(int i, NsdServiceInfo nsdServiceInfo) {
        return this.mDaemon.execute("resolve", Integer.valueOf(i), nsdServiceInfo.getServiceName(), nsdServiceInfo.getServiceType(), "local.");
    }

    private boolean stopResolveService(int i) {
        return this.mDaemon.execute("stop-resolve", Integer.valueOf(i));
    }

    private boolean getAddrInfo(int i, String str) {
        return this.mDaemon.execute("getaddrinfo", Integer.valueOf(i), str);
    }

    private boolean stopGetAddrInfo(int i) {
        return this.mDaemon.execute("stop-getaddrinfo", Integer.valueOf(i));
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, printWriter)) {
            for (ClientInfo clientInfo : this.mClients.values()) {
                printWriter.println("Client Info");
                printWriter.println(clientInfo);
            }
            this.mNsdStateMachine.dump(fileDescriptor, printWriter, strArr);
        }
    }

    private Message obtainMessage(Message message) {
        Message messageObtain = Message.obtain();
        messageObtain.arg2 = message.arg2;
        return messageObtain;
    }

    private void replyToMessage(Message message, int i) {
        if (message.replyTo == null) {
            return;
        }
        Message messageObtainMessage = obtainMessage(message);
        messageObtainMessage.what = i;
        this.mReplyChannel.replyToMessage(message, messageObtainMessage);
    }

    private void replyToMessage(Message message, int i, int i2) {
        if (message.replyTo == null) {
            return;
        }
        Message messageObtainMessage = obtainMessage(message);
        messageObtainMessage.what = i;
        messageObtainMessage.arg1 = i2;
        this.mReplyChannel.replyToMessage(message, messageObtainMessage);
    }

    private void replyToMessage(Message message, int i, Object obj) {
        if (message.replyTo == null) {
            return;
        }
        Message messageObtainMessage = obtainMessage(message);
        messageObtainMessage.what = i;
        messageObtainMessage.obj = obj;
        this.mReplyChannel.replyToMessage(message, messageObtainMessage);
    }

    private class ClientInfo {
        private static final int MAX_LIMIT = 10;
        private final AsyncChannel mChannel;
        private final SparseIntArray mClientIds;
        private final SparseIntArray mClientRequests;
        private final Messenger mMessenger;
        private NsdServiceInfo mResolvedService;

        private ClientInfo(AsyncChannel asyncChannel, Messenger messenger) {
            this.mClientIds = new SparseIntArray();
            this.mClientRequests = new SparseIntArray();
            this.mChannel = asyncChannel;
            this.mMessenger = messenger;
            Slog.d(NsdService.TAG, "New client, channel: " + asyncChannel + " messenger: " + messenger);
        }

        public String toString() {
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append("mChannel ");
            stringBuffer.append(this.mChannel);
            stringBuffer.append("\n");
            stringBuffer.append("mMessenger ");
            stringBuffer.append(this.mMessenger);
            stringBuffer.append("\n");
            stringBuffer.append("mResolvedService ");
            stringBuffer.append(this.mResolvedService);
            stringBuffer.append("\n");
            for (int i = 0; i < this.mClientIds.size(); i++) {
                int iKeyAt = this.mClientIds.keyAt(i);
                stringBuffer.append("clientId ");
                stringBuffer.append(iKeyAt);
                stringBuffer.append(" mDnsId ");
                stringBuffer.append(this.mClientIds.valueAt(i));
                stringBuffer.append(" type ");
                stringBuffer.append(this.mClientRequests.get(iKeyAt));
                stringBuffer.append("\n");
            }
            return stringBuffer.toString();
        }

        private void expungeAllRequests() {
            for (int i = 0; i < this.mClientIds.size(); i++) {
                int iKeyAt = this.mClientIds.keyAt(i);
                int iValueAt = this.mClientIds.valueAt(i);
                NsdService.this.mIdToClientInfoMap.remove(iValueAt);
                Slog.d(NsdService.TAG, "Terminating client-ID " + iKeyAt + " global-ID " + iValueAt + " type " + this.mClientRequests.get(iKeyAt));
                int i2 = this.mClientRequests.get(iKeyAt);
                if (i2 == 393217) {
                    NsdService.this.stopServiceDiscovery(iValueAt);
                } else if (i2 == 393225) {
                    NsdService.this.unregisterService(iValueAt);
                } else if (i2 == 393234) {
                    NsdService.this.stopResolveService(iValueAt);
                }
            }
            this.mClientIds.clear();
            this.mClientRequests.clear();
        }

        private int getClientId(int i) {
            int iIndexOfValue = this.mClientIds.indexOfValue(i);
            if (iIndexOfValue < 0) {
                return iIndexOfValue;
            }
            return this.mClientIds.keyAt(iIndexOfValue);
        }
    }

    @VisibleForTesting
    public interface NsdSettings {
        boolean isEnabled();

        void putEnabledStatus(boolean z);

        void registerContentObserver(Uri uri, ContentObserver contentObserver);

        static NsdSettings makeDefault(Context context) {
            final ContentResolver contentResolver = context.getContentResolver();
            return new NsdSettings() {
                @Override
                public boolean isEnabled() {
                    return Settings.Global.getInt(contentResolver, "nsd_on", 1) == 1;
                }

                @Override
                public void putEnabledStatus(boolean z) {
                    Settings.Global.putInt(contentResolver, "nsd_on", z ? 1 : 0);
                }

                @Override
                public void registerContentObserver(Uri uri, ContentObserver contentObserver) {
                    contentResolver.registerContentObserver(uri, false, contentObserver);
                }
            };
        }
    }
}
