package com.android.internal.telephony.euicc;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.service.euicc.GetDefaultDownloadableSubscriptionListResult;
import android.service.euicc.GetDownloadableSubscriptionMetadataResult;
import android.service.euicc.GetEuiccProfileInfoListResult;
import android.service.euicc.IDeleteSubscriptionCallback;
import android.service.euicc.IDownloadSubscriptionCallback;
import android.service.euicc.IEraseSubscriptionsCallback;
import android.service.euicc.IEuiccService;
import android.service.euicc.IGetDefaultDownloadableSubscriptionListCallback;
import android.service.euicc.IGetDownloadableSubscriptionMetadataCallback;
import android.service.euicc.IGetEidCallback;
import android.service.euicc.IGetEuiccInfoCallback;
import android.service.euicc.IGetEuiccProfileInfoListCallback;
import android.service.euicc.IGetOtaStatusCallback;
import android.service.euicc.IOtaStatusChangedCallback;
import android.service.euicc.IRetainSubscriptionsForFactoryResetCallback;
import android.service.euicc.ISwitchToSubscriptionCallback;
import android.service.euicc.IUpdateSubscriptionNicknameCallback;
import android.telephony.euicc.DownloadableSubscription;
import android.telephony.euicc.EuiccInfo;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.content.PackageMonitor;
import com.android.internal.telephony.euicc.EuiccConnector;
import com.android.internal.util.IState;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class EuiccConnector extends StateMachine implements ServiceConnection {
    private static final int BIND_TIMEOUT_MILLIS = 30000;
    private static final int CMD_COMMAND_COMPLETE = 6;
    private static final int CMD_CONNECT_TIMEOUT = 2;
    private static final int CMD_DELETE_SUBSCRIPTION = 106;
    private static final int CMD_DOWNLOAD_SUBSCRIPTION = 102;
    private static final int CMD_ERASE_SUBSCRIPTIONS = 109;
    private static final int CMD_GET_DEFAULT_DOWNLOADABLE_SUBSCRIPTION_LIST = 104;
    private static final int CMD_GET_DOWNLOADABLE_SUBSCRIPTION_METADATA = 101;
    private static final int CMD_GET_EID = 100;
    private static final int CMD_GET_EUICC_INFO = 105;
    private static final int CMD_GET_EUICC_PROFILE_INFO_LIST = 103;
    private static final int CMD_GET_OTA_STATUS = 111;
    private static final int CMD_LINGER_TIMEOUT = 3;
    private static final int CMD_PACKAGE_CHANGE = 1;
    private static final int CMD_RETAIN_SUBSCRIPTIONS = 110;
    private static final int CMD_SERVICE_CONNECTED = 4;
    private static final int CMD_SERVICE_DISCONNECTED = 5;
    private static final int CMD_START_OTA_IF_NECESSARY = 112;
    private static final int CMD_SWITCH_TO_SUBSCRIPTION = 107;
    private static final int CMD_UPDATE_SUBSCRIPTION_NICKNAME = 108;
    private static final int EUICC_QUERY_FLAGS = 269484096;

    @VisibleForTesting
    static final int LINGER_TIMEOUT_MILLIS = 60000;
    private static final String TAG = "EuiccConnector";
    private Set<BaseEuiccCommandCallback> mActiveCommandCallbacks;

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public AvailableState mAvailableState;

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public BindingState mBindingState;

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public ConnectedState mConnectedState;
    private Context mContext;

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public DisconnectedState mDisconnectedState;
    private IEuiccService mEuiccService;
    private final PackageMonitor mPackageMonitor;
    private PackageManager mPm;
    private ServiceInfo mSelectedComponent;

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public UnavailableState mUnavailableState;
    private final BroadcastReceiver mUserUnlockedReceiver;

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public interface BaseEuiccCommandCallback {
        void onEuiccServiceUnavailable();
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public interface DeleteCommandCallback extends BaseEuiccCommandCallback {
        void onDeleteComplete(int i);
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public interface DownloadCommandCallback extends BaseEuiccCommandCallback {
        void onDownloadComplete(int i);
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public interface EraseCommandCallback extends BaseEuiccCommandCallback {
        void onEraseComplete(int i);
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public interface GetDefaultListCommandCallback extends BaseEuiccCommandCallback {
        void onGetDefaultListComplete(GetDefaultDownloadableSubscriptionListResult getDefaultDownloadableSubscriptionListResult);
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public interface GetEidCommandCallback extends BaseEuiccCommandCallback {
        void onGetEidComplete(String str);
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public interface GetEuiccInfoCommandCallback extends BaseEuiccCommandCallback {
        void onGetEuiccInfoComplete(EuiccInfo euiccInfo);
    }

    interface GetEuiccProfileInfoListCommandCallback extends BaseEuiccCommandCallback {
        void onListComplete(GetEuiccProfileInfoListResult getEuiccProfileInfoListResult);
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public interface GetMetadataCommandCallback extends BaseEuiccCommandCallback {
        void onGetMetadataComplete(GetDownloadableSubscriptionMetadataResult getDownloadableSubscriptionMetadataResult);
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public interface GetOtaStatusCommandCallback extends BaseEuiccCommandCallback {
        void onGetOtaStatusComplete(int i);
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public interface OtaStatusChangedCallback extends BaseEuiccCommandCallback {
        void onOtaStatusChanged(int i);
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public interface RetainSubscriptionsCommandCallback extends BaseEuiccCommandCallback {
        void onRetainSubscriptionsComplete(int i);
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public interface SwitchCommandCallback extends BaseEuiccCommandCallback {
        void onSwitchComplete(int i);
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public interface UpdateNicknameCommandCallback extends BaseEuiccCommandCallback {
        void onUpdateNicknameComplete(int i);
    }

    private static boolean isEuiccCommand(int i) {
        return i >= 100;
    }

    public static ActivityInfo findBestActivity(PackageManager packageManager, Intent intent) {
        ActivityInfo activityInfo = (ActivityInfo) findBestComponent(packageManager, packageManager.queryIntentActivities(intent, EUICC_QUERY_FLAGS));
        if (activityInfo == null) {
            Log.w(TAG, "No valid component found for intent: " + intent);
        }
        return activityInfo;
    }

    public static ComponentInfo findBestComponent(PackageManager packageManager) {
        ComponentInfo componentInfoFindBestComponent = findBestComponent(packageManager, packageManager.queryIntentServices(new Intent("android.service.euicc.EuiccService"), EUICC_QUERY_FLAGS));
        if (componentInfoFindBestComponent == null) {
            Log.w(TAG, "No valid EuiccService implementation found");
        }
        return componentInfoFindBestComponent;
    }

    static class GetMetadataRequest {
        GetMetadataCommandCallback mCallback;
        boolean mForceDeactivateSim;
        DownloadableSubscription mSubscription;

        GetMetadataRequest() {
        }
    }

    static class DownloadRequest {
        DownloadCommandCallback mCallback;
        boolean mForceDeactivateSim;
        DownloadableSubscription mSubscription;
        boolean mSwitchAfterDownload;

        DownloadRequest() {
        }
    }

    static class GetDefaultListRequest {
        GetDefaultListCommandCallback mCallback;
        boolean mForceDeactivateSim;

        GetDefaultListRequest() {
        }
    }

    static class DeleteRequest {
        DeleteCommandCallback mCallback;
        String mIccid;

        DeleteRequest() {
        }
    }

    static class SwitchRequest {
        SwitchCommandCallback mCallback;
        boolean mForceDeactivateSim;
        String mIccid;

        SwitchRequest() {
        }
    }

    static class UpdateNicknameRequest {
        UpdateNicknameCommandCallback mCallback;
        String mIccid;
        String mNickname;

        UpdateNicknameRequest() {
        }
    }

    EuiccConnector(Context context) {
        super(TAG);
        this.mPackageMonitor = new EuiccPackageMonitor();
        this.mUserUnlockedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if ("android.intent.action.USER_UNLOCKED".equals(intent.getAction())) {
                    EuiccConnector.this.sendMessage(1);
                }
            }
        };
        this.mActiveCommandCallbacks = new ArraySet();
        init(context);
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public EuiccConnector(Context context, Looper looper) {
        super(TAG, looper);
        this.mPackageMonitor = new EuiccPackageMonitor();
        this.mUserUnlockedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if ("android.intent.action.USER_UNLOCKED".equals(intent.getAction())) {
                    EuiccConnector.this.sendMessage(1);
                }
            }
        };
        this.mActiveCommandCallbacks = new ArraySet();
        init(context);
    }

    private void init(Context context) {
        this.mContext = context;
        this.mPm = context.getPackageManager();
        this.mUnavailableState = new UnavailableState();
        addState(this.mUnavailableState);
        this.mAvailableState = new AvailableState();
        addState(this.mAvailableState, this.mUnavailableState);
        this.mBindingState = new BindingState();
        addState(this.mBindingState);
        this.mDisconnectedState = new DisconnectedState();
        addState(this.mDisconnectedState);
        this.mConnectedState = new ConnectedState();
        addState(this.mConnectedState, this.mDisconnectedState);
        this.mSelectedComponent = findBestComponent();
        setInitialState(this.mSelectedComponent != null ? this.mAvailableState : this.mUnavailableState);
        this.mPackageMonitor.register(this.mContext, (Looper) null, false);
        this.mContext.registerReceiver(this.mUserUnlockedReceiver, new IntentFilter("android.intent.action.USER_UNLOCKED"));
        start();
    }

    public void onHalting() {
        this.mPackageMonitor.unregister();
        this.mContext.unregisterReceiver(this.mUserUnlockedReceiver);
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public void getEid(GetEidCommandCallback getEidCommandCallback) {
        sendMessage(100, getEidCommandCallback);
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public void getOtaStatus(GetOtaStatusCommandCallback getOtaStatusCommandCallback) {
        sendMessage(111, getOtaStatusCommandCallback);
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public void startOtaIfNecessary(OtaStatusChangedCallback otaStatusChangedCallback) {
        sendMessage(112, otaStatusChangedCallback);
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public void getDownloadableSubscriptionMetadata(DownloadableSubscription downloadableSubscription, boolean z, GetMetadataCommandCallback getMetadataCommandCallback) {
        GetMetadataRequest getMetadataRequest = new GetMetadataRequest();
        getMetadataRequest.mSubscription = downloadableSubscription;
        getMetadataRequest.mForceDeactivateSim = z;
        getMetadataRequest.mCallback = getMetadataCommandCallback;
        sendMessage(101, getMetadataRequest);
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public void downloadSubscription(DownloadableSubscription downloadableSubscription, boolean z, boolean z2, DownloadCommandCallback downloadCommandCallback) {
        DownloadRequest downloadRequest = new DownloadRequest();
        downloadRequest.mSubscription = downloadableSubscription;
        downloadRequest.mSwitchAfterDownload = z;
        downloadRequest.mForceDeactivateSim = z2;
        downloadRequest.mCallback = downloadCommandCallback;
        sendMessage(102, downloadRequest);
    }

    void getEuiccProfileInfoList(GetEuiccProfileInfoListCommandCallback getEuiccProfileInfoListCommandCallback) {
        sendMessage(CMD_GET_EUICC_PROFILE_INFO_LIST, getEuiccProfileInfoListCommandCallback);
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public void getDefaultDownloadableSubscriptionList(boolean z, GetDefaultListCommandCallback getDefaultListCommandCallback) {
        GetDefaultListRequest getDefaultListRequest = new GetDefaultListRequest();
        getDefaultListRequest.mForceDeactivateSim = z;
        getDefaultListRequest.mCallback = getDefaultListCommandCallback;
        sendMessage(CMD_GET_DEFAULT_DOWNLOADABLE_SUBSCRIPTION_LIST, getDefaultListRequest);
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public void getEuiccInfo(GetEuiccInfoCommandCallback getEuiccInfoCommandCallback) {
        sendMessage(CMD_GET_EUICC_INFO, getEuiccInfoCommandCallback);
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public void deleteSubscription(String str, DeleteCommandCallback deleteCommandCallback) {
        DeleteRequest deleteRequest = new DeleteRequest();
        deleteRequest.mIccid = str;
        deleteRequest.mCallback = deleteCommandCallback;
        sendMessage(106, deleteRequest);
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public void switchToSubscription(String str, boolean z, SwitchCommandCallback switchCommandCallback) {
        SwitchRequest switchRequest = new SwitchRequest();
        switchRequest.mIccid = str;
        switchRequest.mForceDeactivateSim = z;
        switchRequest.mCallback = switchCommandCallback;
        sendMessage(CMD_SWITCH_TO_SUBSCRIPTION, switchRequest);
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public void updateSubscriptionNickname(String str, String str2, UpdateNicknameCommandCallback updateNicknameCommandCallback) {
        UpdateNicknameRequest updateNicknameRequest = new UpdateNicknameRequest();
        updateNicknameRequest.mIccid = str;
        updateNicknameRequest.mNickname = str2;
        updateNicknameRequest.mCallback = updateNicknameCommandCallback;
        sendMessage(CMD_UPDATE_SUBSCRIPTION_NICKNAME, updateNicknameRequest);
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public void eraseSubscriptions(EraseCommandCallback eraseCommandCallback) {
        sendMessage(CMD_ERASE_SUBSCRIPTIONS, eraseCommandCallback);
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public void retainSubscriptions(RetainSubscriptionsCommandCallback retainSubscriptionsCommandCallback) {
        sendMessage(CMD_RETAIN_SUBSCRIPTIONS, retainSubscriptionsCommandCallback);
    }

    private class UnavailableState extends State {
        private UnavailableState() {
        }

        public boolean processMessage(Message message) {
            if (message.what != 1) {
                if (EuiccConnector.isEuiccCommand(message.what)) {
                    EuiccConnector.getCallback(message).onEuiccServiceUnavailable();
                    return true;
                }
                return false;
            }
            EuiccConnector.this.mSelectedComponent = EuiccConnector.this.findBestComponent();
            if (EuiccConnector.this.mSelectedComponent != null) {
                EuiccConnector.this.transitionTo(EuiccConnector.this.mAvailableState);
            } else if (EuiccConnector.this.getCurrentState() != EuiccConnector.this.mUnavailableState) {
                EuiccConnector.this.transitionTo(EuiccConnector.this.mUnavailableState);
            }
            return true;
        }
    }

    private class AvailableState extends State {
        private AvailableState() {
        }

        public boolean processMessage(Message message) {
            if (EuiccConnector.isEuiccCommand(message.what)) {
                EuiccConnector.this.deferMessage(message);
                EuiccConnector.this.transitionTo(EuiccConnector.this.mBindingState);
                return true;
            }
            return false;
        }
    }

    private class BindingState extends State {
        private BindingState() {
        }

        public void enter() {
            if (EuiccConnector.this.createBinding()) {
                EuiccConnector.this.transitionTo(EuiccConnector.this.mDisconnectedState);
            } else {
                EuiccConnector.this.transitionTo(EuiccConnector.this.mAvailableState);
            }
        }

        public boolean processMessage(Message message) {
            EuiccConnector.this.deferMessage(message);
            return true;
        }
    }

    private class DisconnectedState extends State {
        private DisconnectedState() {
        }

        public void enter() {
            EuiccConnector.this.sendMessageDelayed(2, 30000L);
        }

        public boolean processMessage(Message message) {
            if (message.what == 4) {
                EuiccConnector.this.mEuiccService = (IEuiccService) message.obj;
                EuiccConnector.this.transitionTo(EuiccConnector.this.mConnectedState);
                return true;
            }
            boolean z = false;
            if (message.what == 1) {
                ServiceInfo serviceInfoFindBestComponent = EuiccConnector.this.findBestComponent();
                String str = (String) message.obj;
                boolean z2 = serviceInfoFindBestComponent != null ? EuiccConnector.this.mSelectedComponent == null || Objects.equals(serviceInfoFindBestComponent.getComponentName(), EuiccConnector.this.mSelectedComponent.getComponentName()) : EuiccConnector.this.mSelectedComponent != null;
                if (serviceInfoFindBestComponent != null && Objects.equals(serviceInfoFindBestComponent.packageName, str)) {
                    z = true;
                }
                if (!z2 || z) {
                    EuiccConnector.this.unbind();
                    EuiccConnector.this.mSelectedComponent = serviceInfoFindBestComponent;
                    if (EuiccConnector.this.mSelectedComponent == null) {
                        EuiccConnector.this.transitionTo(EuiccConnector.this.mUnavailableState);
                    } else {
                        EuiccConnector.this.transitionTo(EuiccConnector.this.mBindingState);
                    }
                }
                return true;
            }
            if (message.what != 2) {
                if (!EuiccConnector.isEuiccCommand(message.what)) {
                    return false;
                }
                EuiccConnector.this.deferMessage(message);
                return true;
            }
            EuiccConnector.this.transitionTo(EuiccConnector.this.mAvailableState);
            return true;
        }
    }

    private class ConnectedState extends State {
        private ConnectedState() {
        }

        public void enter() {
            EuiccConnector.this.removeMessages(2);
            EuiccConnector.this.sendMessageDelayed(3, 60000L);
        }

        public boolean processMessage(Message message) {
            if (message.what == 5) {
                EuiccConnector.this.mEuiccService = null;
                EuiccConnector.this.transitionTo(EuiccConnector.this.mDisconnectedState);
                return true;
            }
            if (message.what == 3) {
                EuiccConnector.this.unbind();
                EuiccConnector.this.transitionTo(EuiccConnector.this.mAvailableState);
                return true;
            }
            if (message.what != 6) {
                if (EuiccConnector.isEuiccCommand(message.what)) {
                    BaseEuiccCommandCallback callback = EuiccConnector.getCallback(message);
                    EuiccConnector.this.onCommandStart(callback);
                    try {
                        switch (message.what) {
                            case 100:
                                EuiccConnector.this.mEuiccService.getEid(-1, new AnonymousClass1(callback));
                                break;
                            case 101:
                                GetMetadataRequest getMetadataRequest = (GetMetadataRequest) message.obj;
                                EuiccConnector.this.mEuiccService.getDownloadableSubscriptionMetadata(-1, getMetadataRequest.mSubscription, getMetadataRequest.mForceDeactivateSim, new AnonymousClass2(callback));
                                break;
                            case 102:
                                DownloadRequest downloadRequest = (DownloadRequest) message.obj;
                                EuiccConnector.this.mEuiccService.downloadSubscription(-1, downloadRequest.mSubscription, downloadRequest.mSwitchAfterDownload, downloadRequest.mForceDeactivateSim, new AnonymousClass3(callback));
                                break;
                            case EuiccConnector.CMD_GET_EUICC_PROFILE_INFO_LIST:
                                EuiccConnector.this.mEuiccService.getEuiccProfileInfoList(-1, new AnonymousClass4(callback));
                                break;
                            case EuiccConnector.CMD_GET_DEFAULT_DOWNLOADABLE_SUBSCRIPTION_LIST:
                                EuiccConnector.this.mEuiccService.getDefaultDownloadableSubscriptionList(-1, ((GetDefaultListRequest) message.obj).mForceDeactivateSim, new AnonymousClass5(callback));
                                break;
                            case EuiccConnector.CMD_GET_EUICC_INFO:
                                EuiccConnector.this.mEuiccService.getEuiccInfo(-1, new AnonymousClass6(callback));
                                break;
                            case 106:
                                EuiccConnector.this.mEuiccService.deleteSubscription(-1, ((DeleteRequest) message.obj).mIccid, new AnonymousClass7(callback));
                                break;
                            case EuiccConnector.CMD_SWITCH_TO_SUBSCRIPTION:
                                SwitchRequest switchRequest = (SwitchRequest) message.obj;
                                EuiccConnector.this.mEuiccService.switchToSubscription(-1, switchRequest.mIccid, switchRequest.mForceDeactivateSim, new AnonymousClass8(callback));
                                break;
                            case EuiccConnector.CMD_UPDATE_SUBSCRIPTION_NICKNAME:
                                UpdateNicknameRequest updateNicknameRequest = (UpdateNicknameRequest) message.obj;
                                EuiccConnector.this.mEuiccService.updateSubscriptionNickname(-1, updateNicknameRequest.mIccid, updateNicknameRequest.mNickname, new AnonymousClass9(callback));
                                break;
                            case EuiccConnector.CMD_ERASE_SUBSCRIPTIONS:
                                EuiccConnector.this.mEuiccService.eraseSubscriptions(-1, new AnonymousClass10(callback));
                                break;
                            case EuiccConnector.CMD_RETAIN_SUBSCRIPTIONS:
                                EuiccConnector.this.mEuiccService.retainSubscriptionsForFactoryReset(-1, new AnonymousClass11(callback));
                                break;
                            case 111:
                                EuiccConnector.this.mEuiccService.getOtaStatus(-1, new AnonymousClass12(callback));
                                break;
                            case 112:
                                EuiccConnector.this.mEuiccService.startOtaIfNecessary(-1, new AnonymousClass13(callback));
                                break;
                            default:
                                Log.wtf(EuiccConnector.TAG, "Unimplemented eUICC command: " + message.what);
                                callback.onEuiccServiceUnavailable();
                                EuiccConnector.this.onCommandEnd(callback);
                                return true;
                        }
                    } catch (Exception e) {
                        Log.w(EuiccConnector.TAG, "Exception making binder call to EuiccService", e);
                        callback.onEuiccServiceUnavailable();
                        EuiccConnector.this.onCommandEnd(callback);
                    }
                    return true;
                }
                return false;
            }
            ((Runnable) message.obj).run();
            return true;
        }

        class AnonymousClass1 extends IGetEidCallback.Stub {
            final BaseEuiccCommandCallback val$callback;

            AnonymousClass1(BaseEuiccCommandCallback baseEuiccCommandCallback) {
                this.val$callback = baseEuiccCommandCallback;
            }

            public void onSuccess(final String str) {
                EuiccConnector euiccConnector = EuiccConnector.this;
                final BaseEuiccCommandCallback baseEuiccCommandCallback = this.val$callback;
                euiccConnector.sendMessage(6, new Runnable() {
                    @Override
                    public final void run() {
                        EuiccConnector.ConnectedState.AnonymousClass1.lambda$onSuccess$0(this.f$0, baseEuiccCommandCallback, str);
                    }
                });
            }

            public static void lambda$onSuccess$0(AnonymousClass1 anonymousClass1, BaseEuiccCommandCallback baseEuiccCommandCallback, String str) {
                ((GetEidCommandCallback) baseEuiccCommandCallback).onGetEidComplete(str);
                EuiccConnector.this.onCommandEnd(baseEuiccCommandCallback);
            }
        }

        class AnonymousClass2 extends IGetDownloadableSubscriptionMetadataCallback.Stub {
            final BaseEuiccCommandCallback val$callback;

            AnonymousClass2(BaseEuiccCommandCallback baseEuiccCommandCallback) {
                this.val$callback = baseEuiccCommandCallback;
            }

            public void onComplete(final GetDownloadableSubscriptionMetadataResult getDownloadableSubscriptionMetadataResult) {
                EuiccConnector euiccConnector = EuiccConnector.this;
                final BaseEuiccCommandCallback baseEuiccCommandCallback = this.val$callback;
                euiccConnector.sendMessage(6, new Runnable() {
                    @Override
                    public final void run() {
                        EuiccConnector.ConnectedState.AnonymousClass2.lambda$onComplete$0(this.f$0, baseEuiccCommandCallback, getDownloadableSubscriptionMetadataResult);
                    }
                });
            }

            public static void lambda$onComplete$0(AnonymousClass2 anonymousClass2, BaseEuiccCommandCallback baseEuiccCommandCallback, GetDownloadableSubscriptionMetadataResult getDownloadableSubscriptionMetadataResult) {
                ((GetMetadataCommandCallback) baseEuiccCommandCallback).onGetMetadataComplete(getDownloadableSubscriptionMetadataResult);
                EuiccConnector.this.onCommandEnd(baseEuiccCommandCallback);
            }
        }

        class AnonymousClass3 extends IDownloadSubscriptionCallback.Stub {
            final BaseEuiccCommandCallback val$callback;

            AnonymousClass3(BaseEuiccCommandCallback baseEuiccCommandCallback) {
                this.val$callback = baseEuiccCommandCallback;
            }

            public void onComplete(final int i) {
                EuiccConnector euiccConnector = EuiccConnector.this;
                final BaseEuiccCommandCallback baseEuiccCommandCallback = this.val$callback;
                euiccConnector.sendMessage(6, new Runnable() {
                    @Override
                    public final void run() {
                        EuiccConnector.ConnectedState.AnonymousClass3.lambda$onComplete$0(this.f$0, baseEuiccCommandCallback, i);
                    }
                });
            }

            public static void lambda$onComplete$0(AnonymousClass3 anonymousClass3, BaseEuiccCommandCallback baseEuiccCommandCallback, int i) {
                ((DownloadCommandCallback) baseEuiccCommandCallback).onDownloadComplete(i);
                EuiccConnector.this.onCommandEnd(baseEuiccCommandCallback);
            }
        }

        class AnonymousClass4 extends IGetEuiccProfileInfoListCallback.Stub {
            final BaseEuiccCommandCallback val$callback;

            AnonymousClass4(BaseEuiccCommandCallback baseEuiccCommandCallback) {
                this.val$callback = baseEuiccCommandCallback;
            }

            public void onComplete(final GetEuiccProfileInfoListResult getEuiccProfileInfoListResult) {
                EuiccConnector euiccConnector = EuiccConnector.this;
                final BaseEuiccCommandCallback baseEuiccCommandCallback = this.val$callback;
                euiccConnector.sendMessage(6, new Runnable() {
                    @Override
                    public final void run() {
                        EuiccConnector.ConnectedState.AnonymousClass4.lambda$onComplete$0(this.f$0, baseEuiccCommandCallback, getEuiccProfileInfoListResult);
                    }
                });
            }

            public static void lambda$onComplete$0(AnonymousClass4 anonymousClass4, BaseEuiccCommandCallback baseEuiccCommandCallback, GetEuiccProfileInfoListResult getEuiccProfileInfoListResult) {
                ((GetEuiccProfileInfoListCommandCallback) baseEuiccCommandCallback).onListComplete(getEuiccProfileInfoListResult);
                EuiccConnector.this.onCommandEnd(baseEuiccCommandCallback);
            }
        }

        class AnonymousClass5 extends IGetDefaultDownloadableSubscriptionListCallback.Stub {
            final BaseEuiccCommandCallback val$callback;

            AnonymousClass5(BaseEuiccCommandCallback baseEuiccCommandCallback) {
                this.val$callback = baseEuiccCommandCallback;
            }

            public void onComplete(final GetDefaultDownloadableSubscriptionListResult getDefaultDownloadableSubscriptionListResult) {
                EuiccConnector euiccConnector = EuiccConnector.this;
                final BaseEuiccCommandCallback baseEuiccCommandCallback = this.val$callback;
                euiccConnector.sendMessage(6, new Runnable() {
                    @Override
                    public final void run() {
                        EuiccConnector.ConnectedState.AnonymousClass5.lambda$onComplete$0(this.f$0, baseEuiccCommandCallback, getDefaultDownloadableSubscriptionListResult);
                    }
                });
            }

            public static void lambda$onComplete$0(AnonymousClass5 anonymousClass5, BaseEuiccCommandCallback baseEuiccCommandCallback, GetDefaultDownloadableSubscriptionListResult getDefaultDownloadableSubscriptionListResult) {
                ((GetDefaultListCommandCallback) baseEuiccCommandCallback).onGetDefaultListComplete(getDefaultDownloadableSubscriptionListResult);
                EuiccConnector.this.onCommandEnd(baseEuiccCommandCallback);
            }
        }

        class AnonymousClass6 extends IGetEuiccInfoCallback.Stub {
            final BaseEuiccCommandCallback val$callback;

            AnonymousClass6(BaseEuiccCommandCallback baseEuiccCommandCallback) {
                this.val$callback = baseEuiccCommandCallback;
            }

            public void onSuccess(final EuiccInfo euiccInfo) {
                EuiccConnector euiccConnector = EuiccConnector.this;
                final BaseEuiccCommandCallback baseEuiccCommandCallback = this.val$callback;
                euiccConnector.sendMessage(6, new Runnable() {
                    @Override
                    public final void run() {
                        EuiccConnector.ConnectedState.AnonymousClass6.lambda$onSuccess$0(this.f$0, baseEuiccCommandCallback, euiccInfo);
                    }
                });
            }

            public static void lambda$onSuccess$0(AnonymousClass6 anonymousClass6, BaseEuiccCommandCallback baseEuiccCommandCallback, EuiccInfo euiccInfo) {
                ((GetEuiccInfoCommandCallback) baseEuiccCommandCallback).onGetEuiccInfoComplete(euiccInfo);
                EuiccConnector.this.onCommandEnd(baseEuiccCommandCallback);
            }
        }

        class AnonymousClass7 extends IDeleteSubscriptionCallback.Stub {
            final BaseEuiccCommandCallback val$callback;

            AnonymousClass7(BaseEuiccCommandCallback baseEuiccCommandCallback) {
                this.val$callback = baseEuiccCommandCallback;
            }

            public void onComplete(final int i) {
                EuiccConnector euiccConnector = EuiccConnector.this;
                final BaseEuiccCommandCallback baseEuiccCommandCallback = this.val$callback;
                euiccConnector.sendMessage(6, new Runnable() {
                    @Override
                    public final void run() {
                        EuiccConnector.ConnectedState.AnonymousClass7.lambda$onComplete$0(this.f$0, baseEuiccCommandCallback, i);
                    }
                });
            }

            public static void lambda$onComplete$0(AnonymousClass7 anonymousClass7, BaseEuiccCommandCallback baseEuiccCommandCallback, int i) {
                ((DeleteCommandCallback) baseEuiccCommandCallback).onDeleteComplete(i);
                EuiccConnector.this.onCommandEnd(baseEuiccCommandCallback);
            }
        }

        class AnonymousClass8 extends ISwitchToSubscriptionCallback.Stub {
            final BaseEuiccCommandCallback val$callback;

            AnonymousClass8(BaseEuiccCommandCallback baseEuiccCommandCallback) {
                this.val$callback = baseEuiccCommandCallback;
            }

            public void onComplete(final int i) {
                EuiccConnector euiccConnector = EuiccConnector.this;
                final BaseEuiccCommandCallback baseEuiccCommandCallback = this.val$callback;
                euiccConnector.sendMessage(6, new Runnable() {
                    @Override
                    public final void run() {
                        EuiccConnector.ConnectedState.AnonymousClass8.lambda$onComplete$0(this.f$0, baseEuiccCommandCallback, i);
                    }
                });
            }

            public static void lambda$onComplete$0(AnonymousClass8 anonymousClass8, BaseEuiccCommandCallback baseEuiccCommandCallback, int i) {
                ((SwitchCommandCallback) baseEuiccCommandCallback).onSwitchComplete(i);
                EuiccConnector.this.onCommandEnd(baseEuiccCommandCallback);
            }
        }

        class AnonymousClass9 extends IUpdateSubscriptionNicknameCallback.Stub {
            final BaseEuiccCommandCallback val$callback;

            AnonymousClass9(BaseEuiccCommandCallback baseEuiccCommandCallback) {
                this.val$callback = baseEuiccCommandCallback;
            }

            public void onComplete(final int i) {
                EuiccConnector euiccConnector = EuiccConnector.this;
                final BaseEuiccCommandCallback baseEuiccCommandCallback = this.val$callback;
                euiccConnector.sendMessage(6, new Runnable() {
                    @Override
                    public final void run() {
                        EuiccConnector.ConnectedState.AnonymousClass9.lambda$onComplete$0(this.f$0, baseEuiccCommandCallback, i);
                    }
                });
            }

            public static void lambda$onComplete$0(AnonymousClass9 anonymousClass9, BaseEuiccCommandCallback baseEuiccCommandCallback, int i) {
                ((UpdateNicknameCommandCallback) baseEuiccCommandCallback).onUpdateNicknameComplete(i);
                EuiccConnector.this.onCommandEnd(baseEuiccCommandCallback);
            }
        }

        class AnonymousClass10 extends IEraseSubscriptionsCallback.Stub {
            final BaseEuiccCommandCallback val$callback;

            AnonymousClass10(BaseEuiccCommandCallback baseEuiccCommandCallback) {
                this.val$callback = baseEuiccCommandCallback;
            }

            public void onComplete(final int i) {
                EuiccConnector euiccConnector = EuiccConnector.this;
                final BaseEuiccCommandCallback baseEuiccCommandCallback = this.val$callback;
                euiccConnector.sendMessage(6, new Runnable() {
                    @Override
                    public final void run() {
                        EuiccConnector.ConnectedState.AnonymousClass10.lambda$onComplete$0(this.f$0, baseEuiccCommandCallback, i);
                    }
                });
            }

            public static void lambda$onComplete$0(AnonymousClass10 anonymousClass10, BaseEuiccCommandCallback baseEuiccCommandCallback, int i) {
                ((EraseCommandCallback) baseEuiccCommandCallback).onEraseComplete(i);
                EuiccConnector.this.onCommandEnd(baseEuiccCommandCallback);
            }
        }

        class AnonymousClass11 extends IRetainSubscriptionsForFactoryResetCallback.Stub {
            final BaseEuiccCommandCallback val$callback;

            AnonymousClass11(BaseEuiccCommandCallback baseEuiccCommandCallback) {
                this.val$callback = baseEuiccCommandCallback;
            }

            public void onComplete(final int i) {
                EuiccConnector euiccConnector = EuiccConnector.this;
                final BaseEuiccCommandCallback baseEuiccCommandCallback = this.val$callback;
                euiccConnector.sendMessage(6, new Runnable() {
                    @Override
                    public final void run() {
                        EuiccConnector.ConnectedState.AnonymousClass11.lambda$onComplete$0(this.f$0, baseEuiccCommandCallback, i);
                    }
                });
            }

            public static void lambda$onComplete$0(AnonymousClass11 anonymousClass11, BaseEuiccCommandCallback baseEuiccCommandCallback, int i) {
                ((RetainSubscriptionsCommandCallback) baseEuiccCommandCallback).onRetainSubscriptionsComplete(i);
                EuiccConnector.this.onCommandEnd(baseEuiccCommandCallback);
            }
        }

        class AnonymousClass12 extends IGetOtaStatusCallback.Stub {
            final BaseEuiccCommandCallback val$callback;

            AnonymousClass12(BaseEuiccCommandCallback baseEuiccCommandCallback) {
                this.val$callback = baseEuiccCommandCallback;
            }

            public void onSuccess(final int i) {
                EuiccConnector euiccConnector = EuiccConnector.this;
                final BaseEuiccCommandCallback baseEuiccCommandCallback = this.val$callback;
                euiccConnector.sendMessage(6, new Runnable() {
                    @Override
                    public final void run() {
                        EuiccConnector.ConnectedState.AnonymousClass12.lambda$onSuccess$0(this.f$0, baseEuiccCommandCallback, i);
                    }
                });
            }

            public static void lambda$onSuccess$0(AnonymousClass12 anonymousClass12, BaseEuiccCommandCallback baseEuiccCommandCallback, int i) {
                ((GetOtaStatusCommandCallback) baseEuiccCommandCallback).onGetOtaStatusComplete(i);
                EuiccConnector.this.onCommandEnd(baseEuiccCommandCallback);
            }
        }

        class AnonymousClass13 extends IOtaStatusChangedCallback.Stub {
            final BaseEuiccCommandCallback val$callback;

            AnonymousClass13(BaseEuiccCommandCallback baseEuiccCommandCallback) {
                this.val$callback = baseEuiccCommandCallback;
            }

            public void onOtaStatusChanged(final int i) throws RemoteException {
                if (i == 1) {
                    EuiccConnector euiccConnector = EuiccConnector.this;
                    final BaseEuiccCommandCallback baseEuiccCommandCallback = this.val$callback;
                    euiccConnector.sendMessage(6, new Runnable() {
                        @Override
                        public final void run() {
                            ((EuiccConnector.OtaStatusChangedCallback) baseEuiccCommandCallback).onOtaStatusChanged(i);
                        }
                    });
                } else {
                    EuiccConnector euiccConnector2 = EuiccConnector.this;
                    final BaseEuiccCommandCallback baseEuiccCommandCallback2 = this.val$callback;
                    euiccConnector2.sendMessage(6, new Runnable() {
                        @Override
                        public final void run() {
                            EuiccConnector.ConnectedState.AnonymousClass13.lambda$onOtaStatusChanged$1(this.f$0, baseEuiccCommandCallback2, i);
                        }
                    });
                }
            }

            public static void lambda$onOtaStatusChanged$1(AnonymousClass13 anonymousClass13, BaseEuiccCommandCallback baseEuiccCommandCallback, int i) {
                ((OtaStatusChangedCallback) baseEuiccCommandCallback).onOtaStatusChanged(i);
                EuiccConnector.this.onCommandEnd(baseEuiccCommandCallback);
            }
        }

        public void exit() {
            EuiccConnector.this.removeMessages(3);
            Iterator it = EuiccConnector.this.mActiveCommandCallbacks.iterator();
            while (it.hasNext()) {
                ((BaseEuiccCommandCallback) it.next()).onEuiccServiceUnavailable();
            }
            EuiccConnector.this.mActiveCommandCallbacks.clear();
        }
    }

    private static BaseEuiccCommandCallback getCallback(Message message) {
        switch (message.what) {
            case 100:
            case CMD_GET_EUICC_PROFILE_INFO_LIST:
            case CMD_GET_EUICC_INFO:
            case CMD_ERASE_SUBSCRIPTIONS:
            case CMD_RETAIN_SUBSCRIPTIONS:
            case 111:
            case 112:
                return (BaseEuiccCommandCallback) message.obj;
            case 101:
                return ((GetMetadataRequest) message.obj).mCallback;
            case 102:
                return ((DownloadRequest) message.obj).mCallback;
            case CMD_GET_DEFAULT_DOWNLOADABLE_SUBSCRIPTION_LIST:
                return ((GetDefaultListRequest) message.obj).mCallback;
            case 106:
                return ((DeleteRequest) message.obj).mCallback;
            case CMD_SWITCH_TO_SUBSCRIPTION:
                return ((SwitchRequest) message.obj).mCallback;
            case CMD_UPDATE_SUBSCRIPTION_NICKNAME:
                return ((UpdateNicknameRequest) message.obj).mCallback;
            default:
                throw new IllegalArgumentException("Unsupported message: " + message.what);
        }
    }

    private void onCommandStart(BaseEuiccCommandCallback baseEuiccCommandCallback) {
        this.mActiveCommandCallbacks.add(baseEuiccCommandCallback);
        removeMessages(3);
    }

    private void onCommandEnd(BaseEuiccCommandCallback baseEuiccCommandCallback) {
        if (!this.mActiveCommandCallbacks.remove(baseEuiccCommandCallback)) {
            Log.wtf(TAG, "Callback already removed from mActiveCommandCallbacks");
        }
        if (this.mActiveCommandCallbacks.isEmpty()) {
            sendMessageDelayed(3, 60000L);
        }
    }

    private ServiceInfo findBestComponent() {
        return (ServiceInfo) findBestComponent(this.mPm);
    }

    private boolean createBinding() {
        if (this.mSelectedComponent == null) {
            Log.wtf(TAG, "Attempting to create binding but no component is selected");
            return false;
        }
        Intent intent = new Intent("android.service.euicc.EuiccService");
        intent.setComponent(this.mSelectedComponent.getComponentName());
        return this.mContext.bindService(intent, this, 67108865);
    }

    private void unbind() {
        this.mEuiccService = null;
        this.mContext.unbindService(this);
    }

    private static ComponentInfo findBestComponent(PackageManager packageManager, List<ResolveInfo> list) {
        ComponentInfo componentInfo = null;
        if (list != null) {
            int priority = Integer.MIN_VALUE;
            for (ResolveInfo resolveInfo : list) {
                if (isValidEuiccComponent(packageManager, resolveInfo) && resolveInfo.filter.getPriority() > priority) {
                    priority = resolveInfo.filter.getPriority();
                    componentInfo = resolveInfo.getComponentInfo();
                }
            }
        }
        return componentInfo;
    }

    private static boolean isValidEuiccComponent(PackageManager packageManager, ResolveInfo resolveInfo) {
        String str;
        ComponentInfo componentInfo = resolveInfo.getComponentInfo();
        String packageName = componentInfo.getComponentName().getPackageName();
        if (packageManager.checkPermission("android.permission.WRITE_EMBEDDED_SUBSCRIPTIONS", packageName) != 0) {
            Log.wtf(TAG, "Package " + packageName + " does not declare WRITE_EMBEDDED_SUBSCRIPTIONS");
            return false;
        }
        if (componentInfo instanceof ServiceInfo) {
            str = ((ServiceInfo) componentInfo).permission;
        } else if (componentInfo instanceof ActivityInfo) {
            str = ((ActivityInfo) componentInfo).permission;
        } else {
            throw new IllegalArgumentException("Can only verify services/activities");
        }
        if (!TextUtils.equals(str, "android.permission.BIND_EUICC_SERVICE")) {
            Log.wtf(TAG, "Package " + packageName + " does not require the BIND_EUICC_SERVICE permission");
            return false;
        }
        if (resolveInfo.filter == null || resolveInfo.filter.getPriority() == 0) {
            Log.wtf(TAG, "Package " + packageName + " does not specify a priority");
            return false;
        }
        return true;
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        sendMessage(4, IEuiccService.Stub.asInterface(iBinder));
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        sendMessage(5);
    }

    private class EuiccPackageMonitor extends PackageMonitor {
        private EuiccPackageMonitor() {
        }

        public void onPackageAdded(String str, int i) {
            sendPackageChange(str, true);
        }

        public void onPackageRemoved(String str, int i) {
            sendPackageChange(str, true);
        }

        public void onPackageUpdateFinished(String str, int i) {
            sendPackageChange(str, true);
        }

        public void onPackageModified(String str) {
            sendPackageChange(str, false);
        }

        public boolean onHandleForceStop(Intent intent, String[] strArr, int i, boolean z) {
            if (z) {
                for (String str : strArr) {
                    sendPackageChange(str, true);
                }
            }
            return super.onHandleForceStop(intent, strArr, i, z);
        }

        private void sendPackageChange(String str, boolean z) {
            EuiccConnector euiccConnector = EuiccConnector.this;
            if (!z) {
                str = null;
            }
            euiccConnector.sendMessage(1, str);
        }
    }

    protected void unhandledMessage(Message message) {
        IState currentState = getCurrentState();
        StringBuilder sb = new StringBuilder();
        sb.append("Unhandled message ");
        sb.append(message.what);
        sb.append(" in state ");
        sb.append(currentState == null ? "null" : currentState.getName());
        Log.wtf(TAG, sb.toString());
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        super.dump(fileDescriptor, printWriter, strArr);
        printWriter.println("mSelectedComponent=" + this.mSelectedComponent);
        printWriter.println("mEuiccService=" + this.mEuiccService);
        printWriter.println("mActiveCommandCount=" + this.mActiveCommandCallbacks.size());
    }
}
