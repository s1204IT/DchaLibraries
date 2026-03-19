package com.android.server.location;

import android.content.Context;
import android.hardware.contexthub.V1_0.ContextHubMsg;
import android.hardware.contexthub.V1_0.HubAppInfo;
import android.hardware.contexthub.V1_0.IContexthub;
import android.hardware.contexthub.V1_0.IContexthubCallback;
import android.hardware.location.ContextHubInfo;
import android.hardware.location.ContextHubMessage;
import android.hardware.location.ContextHubTransaction;
import android.hardware.location.IContextHubCallback;
import android.hardware.location.IContextHubClient;
import android.hardware.location.IContextHubClientCallback;
import android.hardware.location.IContextHubService;
import android.hardware.location.IContextHubTransactionCallback;
import android.hardware.location.NanoApp;
import android.hardware.location.NanoAppBinary;
import android.hardware.location.NanoAppFilter;
import android.hardware.location.NanoAppInstanceInfo;
import android.hardware.location.NanoAppMessage;
import android.hardware.location.NanoAppState;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;
import com.android.internal.util.DumpUtils;
import com.android.server.backup.BackupManagerConstants;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class ContextHubService extends IContextHubService.Stub {
    public static final int MSG_DISABLE_NANO_APP = 2;
    public static final int MSG_ENABLE_NANO_APP = 1;
    public static final int MSG_HUB_RESET = 7;
    public static final int MSG_LOAD_NANO_APP = 3;
    public static final int MSG_QUERY_MEMORY = 6;
    public static final int MSG_QUERY_NANO_APPS = 5;
    public static final int MSG_UNLOAD_NANO_APP = 4;
    private static final int OS_APP_INSTANCE = -1;
    private static final String TAG = "ContextHubService";
    private final ContextHubClientManager mClientManager;
    private final Context mContext;
    private final Map<Integer, ContextHubInfo> mContextHubIdToInfoMap;
    private final List<ContextHubInfo> mContextHubInfoList;
    private final Map<Integer, IContextHubClient> mDefaultClientMap;
    private final ContextHubTransactionManager mTransactionManager;
    private final RemoteCallbackList<IContextHubCallback> mCallbacksList = new RemoteCallbackList<>();
    private final NanoAppStateManager mNanoAppStateManager = new NanoAppStateManager();
    private final IContexthub mContextHubProxy = getContextHubProxy();

    private class ContextHubServiceCallback extends IContexthubCallback.Stub {
        private final int mContextHubId;

        ContextHubServiceCallback(int i) {
            this.mContextHubId = i;
        }

        @Override
        public void handleClientMsg(ContextHubMsg contextHubMsg) {
            ContextHubService.this.handleClientMessageCallback(this.mContextHubId, contextHubMsg);
        }

        @Override
        public void handleTxnResult(int i, int i2) {
            ContextHubService.this.handleTransactionResultCallback(this.mContextHubId, i, i2);
        }

        @Override
        public void handleHubEvent(int i) {
            ContextHubService.this.handleHubEventCallback(this.mContextHubId, i);
        }

        @Override
        public void handleAppAbort(long j, int i) {
            ContextHubService.this.handleAppAbortCallback(this.mContextHubId, j, i);
        }

        @Override
        public void handleAppsInfo(ArrayList<HubAppInfo> arrayList) {
            ContextHubService.this.handleQueryAppsCallback(this.mContextHubId, arrayList);
        }
    }

    public ContextHubService(Context context) {
        List listEmptyList;
        this.mContext = context;
        if (this.mContextHubProxy == null) {
            this.mTransactionManager = null;
            this.mClientManager = null;
            this.mDefaultClientMap = Collections.emptyMap();
            this.mContextHubIdToInfoMap = Collections.emptyMap();
            this.mContextHubInfoList = Collections.emptyList();
            return;
        }
        this.mClientManager = new ContextHubClientManager(this.mContext, this.mContextHubProxy);
        this.mTransactionManager = new ContextHubTransactionManager(this.mContextHubProxy, this.mClientManager, this.mNanoAppStateManager);
        try {
            listEmptyList = this.mContextHubProxy.getHubs();
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException while getting Context Hub info", e);
            listEmptyList = Collections.emptyList();
        }
        this.mContextHubIdToInfoMap = Collections.unmodifiableMap(ContextHubServiceUtil.createContextHubInfoMap(listEmptyList));
        this.mContextHubInfoList = new ArrayList(this.mContextHubIdToInfoMap.values());
        HashMap map = new HashMap();
        Iterator<Integer> it = this.mContextHubIdToInfoMap.keySet().iterator();
        while (it.hasNext()) {
            int iIntValue = it.next().intValue();
            map.put(Integer.valueOf(iIntValue), this.mClientManager.registerClient(createDefaultClientCallback(iIntValue), iIntValue));
            try {
                this.mContextHubProxy.registerCallback(iIntValue, new ContextHubServiceCallback(iIntValue));
            } catch (RemoteException e2) {
                Log.e(TAG, "RemoteException while registering service callback for hub (ID = " + iIntValue + ")", e2);
            }
            queryNanoAppsInternal(iIntValue);
        }
        this.mDefaultClientMap = Collections.unmodifiableMap(map);
    }

    private IContextHubClientCallback createDefaultClientCallback(final int i) {
        return new IContextHubClientCallback.Stub() {
            public void onMessageFromNanoApp(NanoAppMessage nanoAppMessage) {
                ContextHubService.this.onMessageReceiptOldApi(nanoAppMessage.getMessageType(), i, ContextHubService.this.mNanoAppStateManager.getNanoAppHandle(i, nanoAppMessage.getNanoAppId()), nanoAppMessage.getMessageBody());
            }

            public void onHubReset() {
                ContextHubService.this.onMessageReceiptOldApi(7, i, -1, new byte[]{0});
            }

            public void onNanoAppAborted(long j, int i2) {
            }

            public void onNanoAppLoaded(long j) {
            }

            public void onNanoAppUnloaded(long j) {
            }

            public void onNanoAppEnabled(long j) {
            }

            public void onNanoAppDisabled(long j) {
            }
        };
    }

    private IContexthub getContextHubProxy() {
        try {
            return IContexthub.getService(true);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException while attaching to Context Hub HAL proxy", e);
            return null;
        } catch (NoSuchElementException e2) {
            Log.i(TAG, "Context Hub HAL service not found");
            return null;
        }
    }

    public int registerCallback(IContextHubCallback iContextHubCallback) throws RemoteException {
        checkPermissions();
        this.mCallbacksList.register(iContextHubCallback);
        Log.d(TAG, "Added callback, total callbacks " + this.mCallbacksList.getRegisteredCallbackCount());
        return 0;
    }

    public int[] getContextHubHandles() throws RemoteException {
        checkPermissions();
        return ContextHubServiceUtil.createPrimitiveIntArray(this.mContextHubIdToInfoMap.keySet());
    }

    public ContextHubInfo getContextHubInfo(int i) throws RemoteException {
        checkPermissions();
        if (!this.mContextHubIdToInfoMap.containsKey(Integer.valueOf(i))) {
            Log.e(TAG, "Invalid Context Hub handle " + i + " in getContextHubInfo");
            return null;
        }
        return this.mContextHubIdToInfoMap.get(Integer.valueOf(i));
    }

    public List<ContextHubInfo> getContextHubs() throws RemoteException {
        checkPermissions();
        return this.mContextHubInfoList;
    }

    private IContextHubTransactionCallback createLoadTransactionCallback(final int i, final NanoAppBinary nanoAppBinary) {
        return new IContextHubTransactionCallback.Stub() {
            public void onTransactionComplete(int i2) {
                ContextHubService.this.handleLoadResponseOldApi(i, i2, nanoAppBinary);
            }

            public void onQueryResponse(int i2, List<NanoAppState> list) {
            }
        };
    }

    private IContextHubTransactionCallback createUnloadTransactionCallback(final int i) {
        return new IContextHubTransactionCallback.Stub() {
            public void onTransactionComplete(int i2) {
                ContextHubService.this.handleUnloadResponseOldApi(i, i2);
            }

            public void onQueryResponse(int i2, List<NanoAppState> list) {
            }
        };
    }

    private IContextHubTransactionCallback createQueryTransactionCallback(final int i) {
        return new IContextHubTransactionCallback.Stub() {
            public void onTransactionComplete(int i2) {
            }

            public void onQueryResponse(int i2, List<NanoAppState> list) {
                ContextHubService.this.onMessageReceiptOldApi(5, i, -1, new byte[]{(byte) i2});
            }
        };
    }

    public int loadNanoApp(int i, NanoApp nanoApp) throws RemoteException {
        checkPermissions();
        if (this.mContextHubProxy == null) {
            return -1;
        }
        if (!isValidContextHubId(i)) {
            Log.e(TAG, "Invalid Context Hub handle " + i + " in loadNanoApp");
            return -1;
        }
        if (nanoApp == null) {
            Log.e(TAG, "NanoApp cannot be null in loadNanoApp");
            return -1;
        }
        NanoAppBinary nanoAppBinary = new NanoAppBinary(nanoApp.getAppBinary());
        this.mTransactionManager.addTransaction(this.mTransactionManager.createLoadTransaction(i, nanoAppBinary, createLoadTransactionCallback(i, nanoAppBinary)));
        return 0;
    }

    public int unloadNanoApp(int i) throws RemoteException {
        checkPermissions();
        if (this.mContextHubProxy == null) {
            return -1;
        }
        NanoAppInstanceInfo nanoAppInstanceInfo = this.mNanoAppStateManager.getNanoAppInstanceInfo(i);
        if (nanoAppInstanceInfo == null) {
            Log.e(TAG, "Invalid nanoapp handle " + i + " in unloadNanoApp");
            return -1;
        }
        int contexthubId = nanoAppInstanceInfo.getContexthubId();
        this.mTransactionManager.addTransaction(this.mTransactionManager.createUnloadTransaction(contexthubId, nanoAppInstanceInfo.getAppId(), createUnloadTransactionCallback(contexthubId)));
        return 0;
    }

    public NanoAppInstanceInfo getNanoAppInstanceInfo(int i) throws RemoteException {
        checkPermissions();
        return this.mNanoAppStateManager.getNanoAppInstanceInfo(i);
    }

    public int[] findNanoAppOnHub(int i, NanoAppFilter nanoAppFilter) throws RemoteException {
        checkPermissions();
        ArrayList arrayList = new ArrayList();
        for (NanoAppInstanceInfo nanoAppInstanceInfo : this.mNanoAppStateManager.getNanoAppInstanceInfoCollection()) {
            if (nanoAppFilter.testMatch(nanoAppInstanceInfo)) {
                arrayList.add(Integer.valueOf(nanoAppInstanceInfo.getHandle()));
            }
        }
        int[] iArr = new int[arrayList.size()];
        for (int i2 = 0; i2 < arrayList.size(); i2++) {
            iArr[i2] = ((Integer) arrayList.get(i2)).intValue();
        }
        return iArr;
    }

    private int queryNanoAppsInternal(int i) {
        if (this.mContextHubProxy == null) {
            return 1;
        }
        this.mTransactionManager.addTransaction(this.mTransactionManager.createQueryTransaction(i, createQueryTransactionCallback(i)));
        return 0;
    }

    public int sendMessage(int i, int i2, ContextHubMessage contextHubMessage) throws RemoteException {
        checkPermissions();
        if (this.mContextHubProxy == null) {
            return -1;
        }
        if (contextHubMessage == null) {
            Log.e(TAG, "ContextHubMessage cannot be null in sendMessage");
            return -1;
        }
        if (contextHubMessage.getData() == null) {
            Log.e(TAG, "ContextHubMessage message body cannot be null in sendMessage");
            return -1;
        }
        if (!isValidContextHubId(i)) {
            Log.e(TAG, "Invalid Context Hub handle " + i + " in sendMessage");
            return -1;
        }
        boolean z = true;
        if (i2 == -1) {
            if (contextHubMessage.getMsgType() == 5) {
                if (queryNanoAppsInternal(i) != 0) {
                }
            } else {
                Log.e(TAG, "Invalid OS message params of type " + contextHubMessage.getMsgType());
            }
            z = false;
        } else {
            NanoAppInstanceInfo nanoAppInstanceInfo = getNanoAppInstanceInfo(i2);
            if (nanoAppInstanceInfo != null) {
                if (this.mDefaultClientMap.get(Integer.valueOf(i)).sendMessageToNanoApp(NanoAppMessage.createMessageToNanoApp(nanoAppInstanceInfo.getAppId(), contextHubMessage.getMsgType(), contextHubMessage.getData())) != 0) {
                    z = false;
                }
            } else {
                Log.e(TAG, "Failed to send nanoapp message - nanoapp with handle " + i2 + " does not exist.");
                z = false;
            }
        }
        return z ? 0 : -1;
    }

    private void handleClientMessageCallback(int i, ContextHubMsg contextHubMsg) {
        this.mClientManager.onMessageFromNanoApp(i, contextHubMsg);
    }

    private void handleLoadResponseOldApi(int i, int i2, NanoAppBinary nanoAppBinary) {
        if (nanoAppBinary == null) {
            Log.e(TAG, "Nanoapp binary field was null for a load transaction");
            return;
        }
        byte[] bArr = new byte[5];
        bArr[0] = (byte) i2;
        ByteBuffer.wrap(bArr, 1, 4).order(ByteOrder.nativeOrder()).putInt(this.mNanoAppStateManager.getNanoAppHandle(i, nanoAppBinary.getNanoAppId()));
        onMessageReceiptOldApi(3, i, -1, bArr);
    }

    private void handleUnloadResponseOldApi(int i, int i2) {
        onMessageReceiptOldApi(4, i, -1, new byte[]{(byte) i2});
    }

    private void handleTransactionResultCallback(int i, int i2, int i3) {
        this.mTransactionManager.onTransactionResponse(i2, i3);
    }

    private void handleHubEventCallback(int i, int i2) {
        if (i2 == 1) {
            this.mTransactionManager.onHubReset();
            queryNanoAppsInternal(i);
            this.mClientManager.onHubReset(i);
            return;
        }
        Log.i(TAG, "Received unknown hub event (hub ID = " + i + ", type = " + i2 + ")");
    }

    private void handleAppAbortCallback(int i, long j, int i2) {
        this.mClientManager.onNanoAppAborted(i, j, i2);
    }

    private void handleQueryAppsCallback(int i, List<HubAppInfo> list) {
        List<NanoAppState> listCreateNanoAppStateList = ContextHubServiceUtil.createNanoAppStateList(list);
        this.mNanoAppStateManager.updateCache(i, list);
        this.mTransactionManager.onQueryResponse(listCreateNanoAppStateList);
    }

    private boolean isValidContextHubId(int i) {
        return this.mContextHubIdToInfoMap.containsKey(Integer.valueOf(i));
    }

    public IContextHubClient createClient(IContextHubClientCallback iContextHubClientCallback, int i) throws RemoteException {
        checkPermissions();
        if (!isValidContextHubId(i)) {
            throw new IllegalArgumentException("Invalid context hub ID " + i);
        }
        if (iContextHubClientCallback == null) {
            throw new NullPointerException("Cannot register client with null callback");
        }
        return this.mClientManager.registerClient(iContextHubClientCallback, i);
    }

    public void loadNanoAppOnHub(int i, IContextHubTransactionCallback iContextHubTransactionCallback, NanoAppBinary nanoAppBinary) throws RemoteException {
        checkPermissions();
        if (!checkHalProxyAndContextHubId(i, iContextHubTransactionCallback, 0)) {
            return;
        }
        if (nanoAppBinary == null) {
            Log.e(TAG, "NanoAppBinary cannot be null in loadNanoAppOnHub");
            iContextHubTransactionCallback.onTransactionComplete(2);
        } else {
            this.mTransactionManager.addTransaction(this.mTransactionManager.createLoadTransaction(i, nanoAppBinary, iContextHubTransactionCallback));
        }
    }

    public void unloadNanoAppFromHub(int i, IContextHubTransactionCallback iContextHubTransactionCallback, long j) throws RemoteException {
        checkPermissions();
        if (!checkHalProxyAndContextHubId(i, iContextHubTransactionCallback, 1)) {
            return;
        }
        this.mTransactionManager.addTransaction(this.mTransactionManager.createUnloadTransaction(i, j, iContextHubTransactionCallback));
    }

    public void enableNanoApp(int i, IContextHubTransactionCallback iContextHubTransactionCallback, long j) throws RemoteException {
        checkPermissions();
        if (!checkHalProxyAndContextHubId(i, iContextHubTransactionCallback, 2)) {
            return;
        }
        this.mTransactionManager.addTransaction(this.mTransactionManager.createEnableTransaction(i, j, iContextHubTransactionCallback));
    }

    public void disableNanoApp(int i, IContextHubTransactionCallback iContextHubTransactionCallback, long j) throws RemoteException {
        checkPermissions();
        if (!checkHalProxyAndContextHubId(i, iContextHubTransactionCallback, 3)) {
            return;
        }
        this.mTransactionManager.addTransaction(this.mTransactionManager.createDisableTransaction(i, j, iContextHubTransactionCallback));
    }

    public void queryNanoApps(int i, IContextHubTransactionCallback iContextHubTransactionCallback) throws RemoteException {
        checkPermissions();
        if (!checkHalProxyAndContextHubId(i, iContextHubTransactionCallback, 4)) {
            return;
        }
        this.mTransactionManager.addTransaction(this.mTransactionManager.createQueryTransaction(i, iContextHubTransactionCallback));
    }

    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, printWriter)) {
            printWriter.println("Dumping ContextHub Service");
            printWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            printWriter.println("=================== CONTEXT HUBS ====================");
            Iterator<ContextHubInfo> it = this.mContextHubIdToInfoMap.values().iterator();
            while (it.hasNext()) {
                printWriter.println(it.next());
            }
            printWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            printWriter.println("=================== NANOAPPS ====================");
            Iterator<NanoAppInstanceInfo> it2 = this.mNanoAppStateManager.getNanoAppInstanceInfoCollection().iterator();
            while (it2.hasNext()) {
                printWriter.println(it2.next());
            }
        }
    }

    private void checkPermissions() {
        ContextHubServiceUtil.checkPermissions(this.mContext);
    }

    private int onMessageReceiptOldApi(int i, int i2, int i3, byte[] bArr) {
        if (bArr == null) {
            return -1;
        }
        int iBeginBroadcast = this.mCallbacksList.beginBroadcast();
        Log.d(TAG, "Sending message " + i + " version 0 from hubHandle " + i2 + ", appInstance " + i3 + ", callBackCount " + iBeginBroadcast);
        if (iBeginBroadcast < 1) {
            Log.v(TAG, "No message callbacks registered.");
            return 0;
        }
        ContextHubMessage contextHubMessage = new ContextHubMessage(i, 0, bArr);
        for (int i4 = 0; i4 < iBeginBroadcast; i4++) {
            IContextHubCallback broadcastItem = this.mCallbacksList.getBroadcastItem(i4);
            try {
                broadcastItem.onMessageReceipt(i2, i3, contextHubMessage);
            } catch (RemoteException e) {
                Log.i(TAG, "Exception (" + e + ") calling remote callback (" + broadcastItem + ").");
            }
        }
        this.mCallbacksList.finishBroadcast();
        return 0;
    }

    private boolean checkHalProxyAndContextHubId(int i, IContextHubTransactionCallback iContextHubTransactionCallback, int i2) {
        if (this.mContextHubProxy == null) {
            try {
                iContextHubTransactionCallback.onTransactionComplete(8);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException while calling onTransactionComplete", e);
            }
            return false;
        }
        if (!isValidContextHubId(i)) {
            Log.e(TAG, "Cannot start " + ContextHubTransaction.typeToString(i2, false) + " transaction for invalid hub ID " + i);
            try {
                iContextHubTransactionCallback.onTransactionComplete(2);
            } catch (RemoteException e2) {
                Log.e(TAG, "RemoteException while calling onTransactionComplete", e2);
            }
            return false;
        }
        return true;
    }
}
