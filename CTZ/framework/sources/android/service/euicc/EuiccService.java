package android.service.euicc;

import android.annotation.SystemApi;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.service.euicc.IEuiccService;
import android.telephony.euicc.DownloadableSubscription;
import android.telephony.euicc.EuiccInfo;
import android.util.ArraySet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@SystemApi
public abstract class EuiccService extends Service {
    public static final String ACTION_MANAGE_EMBEDDED_SUBSCRIPTIONS = "android.service.euicc.action.MANAGE_EMBEDDED_SUBSCRIPTIONS";
    public static final String ACTION_PROVISION_EMBEDDED_SUBSCRIPTION = "android.service.euicc.action.PROVISION_EMBEDDED_SUBSCRIPTION";
    public static final String ACTION_RESOLVE_CONFIRMATION_CODE = "android.service.euicc.action.RESOLVE_CONFIRMATION_CODE";
    public static final String ACTION_RESOLVE_DEACTIVATE_SIM = "android.service.euicc.action.RESOLVE_DEACTIVATE_SIM";
    public static final String ACTION_RESOLVE_NO_PRIVILEGES = "android.service.euicc.action.RESOLVE_NO_PRIVILEGES";
    public static final String CATEGORY_EUICC_UI = "android.service.euicc.category.EUICC_UI";
    public static final String EUICC_SERVICE_INTERFACE = "android.service.euicc.EuiccService";
    public static final String EXTRA_RESOLUTION_CALLING_PACKAGE = "android.service.euicc.extra.RESOLUTION_CALLING_PACKAGE";
    public static final String EXTRA_RESOLUTION_CONFIRMATION_CODE = "android.service.euicc.extra.RESOLUTION_CONFIRMATION_CODE";
    public static final String EXTRA_RESOLUTION_CONFIRMATION_CODE_RETRIED = "android.service.euicc.extra.RESOLUTION_CONFIRMATION_CODE_RETRIED";
    public static final String EXTRA_RESOLUTION_CONSENT = "android.service.euicc.extra.RESOLUTION_CONSENT";
    public static final ArraySet<String> RESOLUTION_ACTIONS = new ArraySet<>();
    public static final int RESULT_FIRST_USER = 1;
    public static final int RESULT_MUST_DEACTIVATE_SIM = -1;
    public static final int RESULT_NEED_CONFIRMATION_CODE = -2;
    public static final int RESULT_OK = 0;
    private ThreadPoolExecutor mExecutor;
    private final IEuiccService.Stub mStubWrapper = new IEuiccServiceWrapper();

    public static abstract class OtaStatusChangedCallback {
        public abstract void onOtaStatusChanged(int i);
    }

    public abstract int onDeleteSubscription(int i, String str);

    public abstract int onDownloadSubscription(int i, DownloadableSubscription downloadableSubscription, boolean z, boolean z2);

    public abstract int onEraseSubscriptions(int i);

    public abstract GetDefaultDownloadableSubscriptionListResult onGetDefaultDownloadableSubscriptionList(int i, boolean z);

    public abstract GetDownloadableSubscriptionMetadataResult onGetDownloadableSubscriptionMetadata(int i, DownloadableSubscription downloadableSubscription, boolean z);

    public abstract String onGetEid(int i);

    public abstract EuiccInfo onGetEuiccInfo(int i);

    public abstract GetEuiccProfileInfoListResult onGetEuiccProfileInfoList(int i);

    public abstract int onGetOtaStatus(int i);

    public abstract int onRetainSubscriptionsForFactoryReset(int i);

    public abstract void onStartOtaIfNecessary(int i, OtaStatusChangedCallback otaStatusChangedCallback);

    public abstract int onSwitchToSubscription(int i, String str, boolean z);

    public abstract int onUpdateSubscriptionNickname(int i, String str, String str2);

    static {
        RESOLUTION_ACTIONS.add(ACTION_RESOLVE_DEACTIVATE_SIM);
        RESOLUTION_ACTIONS.add(ACTION_RESOLVE_NO_PRIVILEGES);
        RESOLUTION_ACTIONS.add(ACTION_RESOLVE_CONFIRMATION_CODE);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.mExecutor = new ThreadPoolExecutor(4, 4, 30L, TimeUnit.SECONDS, new LinkedBlockingQueue(), new ThreadFactory() {
            private final AtomicInteger mCount = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable runnable) {
                return new Thread(runnable, "EuiccService #" + this.mCount.getAndIncrement());
            }
        });
        this.mExecutor.allowCoreThreadTimeOut(true);
    }

    @Override
    public void onDestroy() {
        this.mExecutor.shutdownNow();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return this.mStubWrapper;
    }

    private class IEuiccServiceWrapper extends IEuiccService.Stub {
        private IEuiccServiceWrapper() {
        }

        @Override
        public void downloadSubscription(final int i, final DownloadableSubscription downloadableSubscription, final boolean z, final boolean z2, final IDownloadSubscriptionCallback iDownloadSubscriptionCallback) {
            EuiccService.this.mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        iDownloadSubscriptionCallback.onComplete(EuiccService.this.onDownloadSubscription(i, downloadableSubscription, z, z2));
                    } catch (RemoteException e) {
                    }
                }
            });
        }

        @Override
        public void getEid(final int i, final IGetEidCallback iGetEidCallback) {
            EuiccService.this.mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        iGetEidCallback.onSuccess(EuiccService.this.onGetEid(i));
                    } catch (RemoteException e) {
                    }
                }
            });
        }

        @Override
        public void startOtaIfNecessary(final int i, final IOtaStatusChangedCallback iOtaStatusChangedCallback) {
            EuiccService.this.mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    EuiccService.this.onStartOtaIfNecessary(i, new OtaStatusChangedCallback() {
                        @Override
                        public void onOtaStatusChanged(int i2) {
                            try {
                                iOtaStatusChangedCallback.onOtaStatusChanged(i2);
                            } catch (RemoteException e) {
                            }
                        }
                    });
                }
            });
        }

        @Override
        public void getOtaStatus(final int i, final IGetOtaStatusCallback iGetOtaStatusCallback) {
            EuiccService.this.mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        iGetOtaStatusCallback.onSuccess(EuiccService.this.onGetOtaStatus(i));
                    } catch (RemoteException e) {
                    }
                }
            });
        }

        @Override
        public void getDownloadableSubscriptionMetadata(final int i, final DownloadableSubscription downloadableSubscription, final boolean z, final IGetDownloadableSubscriptionMetadataCallback iGetDownloadableSubscriptionMetadataCallback) {
            EuiccService.this.mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        iGetDownloadableSubscriptionMetadataCallback.onComplete(EuiccService.this.onGetDownloadableSubscriptionMetadata(i, downloadableSubscription, z));
                    } catch (RemoteException e) {
                    }
                }
            });
        }

        @Override
        public void getDefaultDownloadableSubscriptionList(final int i, final boolean z, final IGetDefaultDownloadableSubscriptionListCallback iGetDefaultDownloadableSubscriptionListCallback) {
            EuiccService.this.mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        iGetDefaultDownloadableSubscriptionListCallback.onComplete(EuiccService.this.onGetDefaultDownloadableSubscriptionList(i, z));
                    } catch (RemoteException e) {
                    }
                }
            });
        }

        @Override
        public void getEuiccProfileInfoList(final int i, final IGetEuiccProfileInfoListCallback iGetEuiccProfileInfoListCallback) {
            EuiccService.this.mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        iGetEuiccProfileInfoListCallback.onComplete(EuiccService.this.onGetEuiccProfileInfoList(i));
                    } catch (RemoteException e) {
                    }
                }
            });
        }

        @Override
        public void getEuiccInfo(final int i, final IGetEuiccInfoCallback iGetEuiccInfoCallback) {
            EuiccService.this.mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        iGetEuiccInfoCallback.onSuccess(EuiccService.this.onGetEuiccInfo(i));
                    } catch (RemoteException e) {
                    }
                }
            });
        }

        @Override
        public void deleteSubscription(final int i, final String str, final IDeleteSubscriptionCallback iDeleteSubscriptionCallback) {
            EuiccService.this.mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        iDeleteSubscriptionCallback.onComplete(EuiccService.this.onDeleteSubscription(i, str));
                    } catch (RemoteException e) {
                    }
                }
            });
        }

        @Override
        public void switchToSubscription(final int i, final String str, final boolean z, final ISwitchToSubscriptionCallback iSwitchToSubscriptionCallback) {
            EuiccService.this.mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        iSwitchToSubscriptionCallback.onComplete(EuiccService.this.onSwitchToSubscription(i, str, z));
                    } catch (RemoteException e) {
                    }
                }
            });
        }

        @Override
        public void updateSubscriptionNickname(final int i, final String str, final String str2, final IUpdateSubscriptionNicknameCallback iUpdateSubscriptionNicknameCallback) {
            EuiccService.this.mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        iUpdateSubscriptionNicknameCallback.onComplete(EuiccService.this.onUpdateSubscriptionNickname(i, str, str2));
                    } catch (RemoteException e) {
                    }
                }
            });
        }

        @Override
        public void eraseSubscriptions(final int i, final IEraseSubscriptionsCallback iEraseSubscriptionsCallback) {
            EuiccService.this.mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        iEraseSubscriptionsCallback.onComplete(EuiccService.this.onEraseSubscriptions(i));
                    } catch (RemoteException e) {
                    }
                }
            });
        }

        @Override
        public void retainSubscriptionsForFactoryReset(final int i, final IRetainSubscriptionsForFactoryResetCallback iRetainSubscriptionsForFactoryResetCallback) {
            EuiccService.this.mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        iRetainSubscriptionsForFactoryResetCallback.onComplete(EuiccService.this.onRetainSubscriptionsForFactoryReset(i));
                    } catch (RemoteException e) {
                    }
                }
            });
        }
    }
}
