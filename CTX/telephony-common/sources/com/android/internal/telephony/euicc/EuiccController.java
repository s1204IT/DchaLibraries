package com.android.internal.telephony.euicc;

import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.ServiceManager;
import android.provider.Settings;
import android.service.euicc.GetDefaultDownloadableSubscriptionListResult;
import android.service.euicc.GetDownloadableSubscriptionMetadataResult;
import android.service.euicc.GetEuiccProfileInfoListResult;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.UiccAccessRule;
import android.telephony.euicc.DownloadableSubscription;
import android.telephony.euicc.EuiccInfo;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.euicc.EuiccConnector;
import com.android.internal.telephony.euicc.IEuiccController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class EuiccController extends IEuiccController.Stub {
    private static final int ERROR = 2;
    private static final String EXTRA_EMBEDDED_SUBSCRIPTION_DOWNLOADABLE_SUBSCRIPTION = "android.telephony.euicc.extra.EMBEDDED_SUBSCRIPTION_DOWNLOADABLE_SUBSCRIPTION";

    @VisibleForTesting
    static final String EXTRA_OPERATION = "operation";
    private static final int OK = 0;
    private static final String RESOLUTION_ACTIVITY_CLASS_NAME = "com.android.phone.euicc.EuiccResolutionUiDispatcherActivity";
    private static final String RESOLUTION_ACTIVITY_PACKAGE_NAME = "com.android.phone";
    private static final int RESOLVABLE_ERROR = 1;
    private static final String TAG = "EuiccController";
    private static EuiccController sInstance;
    private final AppOpsManager mAppOpsManager;
    private final EuiccConnector mConnector;
    private final Context mContext;
    private final PackageManager mPackageManager;
    private final SubscriptionManager mSubscriptionManager;

    public static EuiccController init(Context context) {
        synchronized (EuiccController.class) {
            if (sInstance == null) {
                sInstance = new EuiccController(context);
            } else {
                Log.wtf(TAG, "init() called multiple times! sInstance = " + sInstance);
            }
        }
        return sInstance;
    }

    public static EuiccController get() {
        if (sInstance == null) {
            synchronized (EuiccController.class) {
                if (sInstance == null) {
                    throw new IllegalStateException("get() called before init()");
                }
            }
        }
        return sInstance;
    }

    private EuiccController(Context context) {
        this(context, new EuiccConnector(context));
        ServiceManager.addService("econtroller", this);
    }

    @VisibleForTesting
    public EuiccController(Context context, EuiccConnector euiccConnector) {
        this.mContext = context;
        this.mConnector = euiccConnector;
        this.mSubscriptionManager = (SubscriptionManager) context.getSystemService("telephony_subscription_service");
        this.mAppOpsManager = (AppOpsManager) context.getSystemService("appops");
        this.mPackageManager = context.getPackageManager();
    }

    public void continueOperation(Intent intent, Bundle bundle) {
        if (!callerCanWriteEmbeddedSubscriptions()) {
            throw new SecurityException("Must have WRITE_EMBEDDED_SUBSCRIPTIONS to continue operation");
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            EuiccOperation euiccOperation = (EuiccOperation) intent.getParcelableExtra(EXTRA_OPERATION);
            if (euiccOperation == null) {
                throw new IllegalArgumentException("Invalid resolution intent");
            }
            euiccOperation.continueOperation(bundle, (PendingIntent) intent.getParcelableExtra("android.telephony.euicc.extra.EMBEDDED_SUBSCRIPTION_RESOLUTION_CALLBACK_INTENT"));
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public String getEid() {
        if (!callerCanReadPhoneStatePrivileged() && !callerHasCarrierPrivilegesForActiveSubscription()) {
            throw new SecurityException("Must have carrier privileges on active subscription to read EID");
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            return blockingGetEidFromEuiccService();
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public int getOtaStatus() {
        if (!callerCanWriteEmbeddedSubscriptions()) {
            throw new SecurityException("Must have WRITE_EMBEDDED_SUBSCRIPTIONS to get OTA status");
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            return blockingGetOtaStatusFromEuiccService();
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void startOtaUpdatingIfNecessary() {
        this.mConnector.startOtaIfNecessary(new EuiccConnector.OtaStatusChangedCallback() {
            @Override
            public void onOtaStatusChanged(int i) {
                EuiccController.this.sendOtaStatusChangedBroadcast();
            }

            @Override
            public void onEuiccServiceUnavailable() {
            }
        });
    }

    public void getDownloadableSubscriptionMetadata(DownloadableSubscription downloadableSubscription, String str, PendingIntent pendingIntent) {
        getDownloadableSubscriptionMetadata(downloadableSubscription, false, str, pendingIntent);
    }

    void getDownloadableSubscriptionMetadata(DownloadableSubscription downloadableSubscription, boolean z, String str, PendingIntent pendingIntent) {
        if (!callerCanWriteEmbeddedSubscriptions()) {
            throw new SecurityException("Must have WRITE_EMBEDDED_SUBSCRIPTIONS to get metadata");
        }
        this.mAppOpsManager.checkPackage(Binder.getCallingUid(), str);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mConnector.getDownloadableSubscriptionMetadata(downloadableSubscription, z, new GetMetadataCommandCallback(jClearCallingIdentity, downloadableSubscription, str, pendingIntent));
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    class GetMetadataCommandCallback implements EuiccConnector.GetMetadataCommandCallback {
        protected final PendingIntent mCallbackIntent;
        protected final String mCallingPackage;
        protected final long mCallingToken;
        protected final DownloadableSubscription mSubscription;

        GetMetadataCommandCallback(long j, DownloadableSubscription downloadableSubscription, String str, PendingIntent pendingIntent) {
            this.mCallingToken = j;
            this.mSubscription = downloadableSubscription;
            this.mCallingPackage = str;
            this.mCallbackIntent = pendingIntent;
        }

        @Override
        public void onGetMetadataComplete(GetDownloadableSubscriptionMetadataResult getDownloadableSubscriptionMetadataResult) {
            int i;
            Intent intent = new Intent();
            switch (getDownloadableSubscriptionMetadataResult.getResult()) {
                case -1:
                    EuiccController.this.addResolutionIntent(intent, "android.service.euicc.action.RESOLVE_DEACTIVATE_SIM", this.mCallingPackage, false, getOperationForDeactivateSim());
                    i = 1;
                    break;
                case 0:
                    i = 0;
                    intent.putExtra(EuiccController.EXTRA_EMBEDDED_SUBSCRIPTION_DOWNLOADABLE_SUBSCRIPTION, getDownloadableSubscriptionMetadataResult.getDownloadableSubscription());
                    break;
                default:
                    i = 2;
                    intent.putExtra("android.telephony.euicc.extra.EMBEDDED_SUBSCRIPTION_DETAILED_CODE", getDownloadableSubscriptionMetadataResult.getResult());
                    break;
            }
            EuiccController.this.sendResult(this.mCallbackIntent, i, intent);
        }

        @Override
        public void onEuiccServiceUnavailable() {
            EuiccController.this.sendResult(this.mCallbackIntent, 2, null);
        }

        protected EuiccOperation getOperationForDeactivateSim() {
            return EuiccOperation.forGetMetadataDeactivateSim(this.mCallingToken, this.mSubscription, this.mCallingPackage);
        }
    }

    public void downloadSubscription(DownloadableSubscription downloadableSubscription, boolean z, String str, PendingIntent pendingIntent) {
        downloadSubscription(downloadableSubscription, z, str, false, pendingIntent);
    }

    void downloadSubscription(DownloadableSubscription downloadableSubscription, boolean z, String str, boolean z2, PendingIntent pendingIntent) {
        boolean zCallerCanWriteEmbeddedSubscriptions = callerCanWriteEmbeddedSubscriptions();
        this.mAppOpsManager.checkPackage(Binder.getCallingUid(), str);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            if (zCallerCanWriteEmbeddedSubscriptions) {
                downloadSubscriptionPrivileged(jClearCallingIdentity, downloadableSubscription, z, z2, str, pendingIntent);
            } else {
                this.mConnector.getDownloadableSubscriptionMetadata(downloadableSubscription, z2, new DownloadSubscriptionGetMetadataCommandCallback(jClearCallingIdentity, downloadableSubscription, z, str, z2, pendingIntent));
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    class DownloadSubscriptionGetMetadataCommandCallback extends GetMetadataCommandCallback {
        private final boolean mForceDeactivateSim;
        private final boolean mSwitchAfterDownload;

        DownloadSubscriptionGetMetadataCommandCallback(long j, DownloadableSubscription downloadableSubscription, boolean z, String str, boolean z2, PendingIntent pendingIntent) {
            super(j, downloadableSubscription, str, pendingIntent);
            this.mSwitchAfterDownload = z;
            this.mForceDeactivateSim = z2;
        }

        @Override
        public void onGetMetadataComplete(GetDownloadableSubscriptionMetadataResult getDownloadableSubscriptionMetadataResult) {
            UiccAccessRule[] uiccAccessRuleArr;
            if (getDownloadableSubscriptionMetadataResult.getResult() == -1) {
                Intent intent = new Intent();
                EuiccController.this.addResolutionIntent(intent, "android.service.euicc.action.RESOLVE_NO_PRIVILEGES", this.mCallingPackage, false, EuiccOperation.forDownloadNoPrivileges(this.mCallingToken, this.mSubscription, this.mSwitchAfterDownload, this.mCallingPackage));
                EuiccController.this.sendResult(this.mCallbackIntent, 1, intent);
                return;
            }
            if (getDownloadableSubscriptionMetadataResult.getResult() != 0) {
                super.onGetMetadataComplete(getDownloadableSubscriptionMetadataResult);
                return;
            }
            DownloadableSubscription downloadableSubscription = getDownloadableSubscriptionMetadataResult.getDownloadableSubscription();
            List accessRules = downloadableSubscription.getAccessRules();
            if (accessRules != null) {
                uiccAccessRuleArr = (UiccAccessRule[]) accessRules.toArray(new UiccAccessRule[accessRules.size()]);
            } else {
                uiccAccessRuleArr = null;
            }
            if (uiccAccessRuleArr != null) {
                try {
                    PackageInfo packageInfo = EuiccController.this.mPackageManager.getPackageInfo(this.mCallingPackage, 64);
                    for (UiccAccessRule uiccAccessRule : uiccAccessRuleArr) {
                        if (uiccAccessRule.getCarrierPrivilegeStatus(packageInfo) == 1) {
                            if (EuiccController.this.canManageActiveSubscription(this.mCallingPackage)) {
                                EuiccController.this.downloadSubscriptionPrivileged(this.mCallingToken, downloadableSubscription, this.mSwitchAfterDownload, this.mForceDeactivateSim, this.mCallingPackage, this.mCallbackIntent);
                                return;
                            }
                            Intent intent2 = new Intent();
                            EuiccController.this.addResolutionIntent(intent2, "android.service.euicc.action.RESOLVE_NO_PRIVILEGES", this.mCallingPackage, false, EuiccOperation.forDownloadNoPrivileges(this.mCallingToken, downloadableSubscription, this.mSwitchAfterDownload, this.mCallingPackage));
                            EuiccController.this.sendResult(this.mCallbackIntent, 1, intent2);
                            return;
                        }
                    }
                    Log.e(EuiccController.TAG, "Caller is not permitted to download this profile");
                    EuiccController.this.sendResult(this.mCallbackIntent, 2, null);
                    return;
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(EuiccController.TAG, "Calling package valid but gone");
                    EuiccController.this.sendResult(this.mCallbackIntent, 2, null);
                    return;
                }
            }
            Log.e(EuiccController.TAG, "No access rules but caller is unprivileged");
            EuiccController.this.sendResult(this.mCallbackIntent, 2, null);
        }

        @Override
        protected EuiccOperation getOperationForDeactivateSim() {
            return EuiccOperation.forDownloadDeactivateSim(this.mCallingToken, this.mSubscription, this.mSwitchAfterDownload, this.mCallingPackage);
        }
    }

    void downloadSubscriptionPrivileged(final long j, final DownloadableSubscription downloadableSubscription, final boolean z, boolean z2, final String str, final PendingIntent pendingIntent) {
        this.mConnector.downloadSubscription(downloadableSubscription, z, z2, new EuiccConnector.DownloadCommandCallback() {
            @Override
            public void onDownloadComplete(int i) {
                boolean z3;
                Intent intent = new Intent();
                int i2 = 0;
                switch (i) {
                    case -2:
                        if (TextUtils.isEmpty(downloadableSubscription.getConfirmationCode())) {
                            z3 = false;
                        } else {
                            z3 = true;
                        }
                        EuiccController.this.addResolutionIntent(intent, "android.service.euicc.action.RESOLVE_CONFIRMATION_CODE", str, z3, EuiccOperation.forDownloadConfirmationCode(j, downloadableSubscription, z, str));
                        i2 = 1;
                        EuiccController.this.sendResult(pendingIntent, i2, intent);
                        break;
                    case -1:
                        EuiccController.this.addResolutionIntent(intent, "android.service.euicc.action.RESOLVE_DEACTIVATE_SIM", str, false, EuiccOperation.forDownloadDeactivateSim(j, downloadableSubscription, z, str));
                        i2 = 1;
                        EuiccController.this.sendResult(pendingIntent, i2, intent);
                        break;
                    case 0:
                        Settings.Global.putInt(EuiccController.this.mContext.getContentResolver(), "euicc_provisioned", 1);
                        intent.putExtra(EuiccController.EXTRA_EMBEDDED_SUBSCRIPTION_DOWNLOADABLE_SUBSCRIPTION, downloadableSubscription);
                        if (!z) {
                            EuiccController.this.refreshSubscriptionsAndSendResult(pendingIntent, 0, intent);
                        }
                        EuiccController.this.sendResult(pendingIntent, i2, intent);
                        break;
                    default:
                        i2 = 2;
                        intent.putExtra("android.telephony.euicc.extra.EMBEDDED_SUBSCRIPTION_DETAILED_CODE", i);
                        EuiccController.this.sendResult(pendingIntent, i2, intent);
                        break;
                }
            }

            @Override
            public void onEuiccServiceUnavailable() {
                EuiccController.this.sendResult(pendingIntent, 2, null);
            }
        });
    }

    public GetEuiccProfileInfoListResult blockingGetEuiccProfileInfoList() {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final AtomicReference atomicReference = new AtomicReference();
        this.mConnector.getEuiccProfileInfoList(new EuiccConnector.GetEuiccProfileInfoListCommandCallback() {
            @Override
            public void onListComplete(GetEuiccProfileInfoListResult getEuiccProfileInfoListResult) {
                atomicReference.set(getEuiccProfileInfoListResult);
                countDownLatch.countDown();
            }

            @Override
            public void onEuiccServiceUnavailable() {
                countDownLatch.countDown();
            }
        });
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return (GetEuiccProfileInfoListResult) atomicReference.get();
    }

    public void getDefaultDownloadableSubscriptionList(String str, PendingIntent pendingIntent) {
        getDefaultDownloadableSubscriptionList(false, str, pendingIntent);
    }

    void getDefaultDownloadableSubscriptionList(boolean z, String str, PendingIntent pendingIntent) {
        if (!callerCanWriteEmbeddedSubscriptions()) {
            throw new SecurityException("Must have WRITE_EMBEDDED_SUBSCRIPTIONS to get default list");
        }
        this.mAppOpsManager.checkPackage(Binder.getCallingUid(), str);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mConnector.getDefaultDownloadableSubscriptionList(z, new GetDefaultListCommandCallback(jClearCallingIdentity, str, pendingIntent));
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    class GetDefaultListCommandCallback implements EuiccConnector.GetDefaultListCommandCallback {
        final PendingIntent mCallbackIntent;
        final String mCallingPackage;
        final long mCallingToken;

        GetDefaultListCommandCallback(long j, String str, PendingIntent pendingIntent) {
            this.mCallingToken = j;
            this.mCallingPackage = str;
            this.mCallbackIntent = pendingIntent;
        }

        @Override
        public void onGetDefaultListComplete(GetDefaultDownloadableSubscriptionListResult getDefaultDownloadableSubscriptionListResult) {
            int i;
            Intent intent = new Intent();
            switch (getDefaultDownloadableSubscriptionListResult.getResult()) {
                case -1:
                    EuiccController.this.addResolutionIntent(intent, "android.service.euicc.action.RESOLVE_DEACTIVATE_SIM", this.mCallingPackage, false, EuiccOperation.forGetDefaultListDeactivateSim(this.mCallingToken, this.mCallingPackage));
                    i = 1;
                    break;
                case 0:
                    i = 0;
                    List downloadableSubscriptions = getDefaultDownloadableSubscriptionListResult.getDownloadableSubscriptions();
                    if (downloadableSubscriptions != null && downloadableSubscriptions.size() > 0) {
                        intent.putExtra("android.telephony.euicc.extra.EMBEDDED_SUBSCRIPTION_DOWNLOADABLE_SUBSCRIPTIONS", (Parcelable[]) downloadableSubscriptions.toArray(new DownloadableSubscription[downloadableSubscriptions.size()]));
                    }
                    break;
                default:
                    i = 2;
                    intent.putExtra("android.telephony.euicc.extra.EMBEDDED_SUBSCRIPTION_DETAILED_CODE", getDefaultDownloadableSubscriptionListResult.getResult());
                    break;
            }
            EuiccController.this.sendResult(this.mCallbackIntent, i, intent);
        }

        @Override
        public void onEuiccServiceUnavailable() {
            EuiccController.this.sendResult(this.mCallbackIntent, 2, null);
        }
    }

    public EuiccInfo getEuiccInfo() {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            return blockingGetEuiccInfoFromEuiccService();
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void deleteSubscription(int i, String str, PendingIntent pendingIntent) {
        boolean zCallerCanWriteEmbeddedSubscriptions = callerCanWriteEmbeddedSubscriptions();
        this.mAppOpsManager.checkPackage(Binder.getCallingUid(), str);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            SubscriptionInfo subscriptionForSubscriptionId = getSubscriptionForSubscriptionId(i);
            if (subscriptionForSubscriptionId == null) {
                Log.e(TAG, "Cannot delete nonexistent subscription: " + i);
                sendResult(pendingIntent, 2, null);
                return;
            }
            if (zCallerCanWriteEmbeddedSubscriptions || subscriptionForSubscriptionId.canManageSubscription(this.mContext, str)) {
                deleteSubscriptionPrivileged(subscriptionForSubscriptionId.getIccId(), pendingIntent);
                return;
            }
            Log.e(TAG, "No permissions: " + i);
            sendResult(pendingIntent, 2, null);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    void deleteSubscriptionPrivileged(String str, final PendingIntent pendingIntent) {
        this.mConnector.deleteSubscription(str, new EuiccConnector.DeleteCommandCallback() {
            @Override
            public void onDeleteComplete(int i) {
                Intent intent = new Intent();
                if (i == 0) {
                    EuiccController.this.refreshSubscriptionsAndSendResult(pendingIntent, 0, intent);
                } else {
                    intent.putExtra("android.telephony.euicc.extra.EMBEDDED_SUBSCRIPTION_DETAILED_CODE", i);
                    EuiccController.this.sendResult(pendingIntent, 2, intent);
                }
            }

            @Override
            public void onEuiccServiceUnavailable() {
                EuiccController.this.sendResult(pendingIntent, 2, null);
            }
        });
    }

    public void switchToSubscription(int i, String str, PendingIntent pendingIntent) {
        switchToSubscription(i, false, str, pendingIntent);
    }

    void switchToSubscription(int i, boolean z, String str, PendingIntent pendingIntent) {
        boolean zCallerCanWriteEmbeddedSubscriptions = callerCanWriteEmbeddedSubscriptions();
        this.mAppOpsManager.checkPackage(Binder.getCallingUid(), str);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        boolean z2 = zCallerCanWriteEmbeddedSubscriptions ? true : z;
        String iccId = null;
        try {
            if (i != -1) {
                SubscriptionInfo subscriptionForSubscriptionId = getSubscriptionForSubscriptionId(i);
                if (subscriptionForSubscriptionId == null) {
                    Log.e(TAG, "Cannot switch to nonexistent subscription: " + i);
                    sendResult(pendingIntent, 2, null);
                    return;
                }
                if (!zCallerCanWriteEmbeddedSubscriptions && !this.mSubscriptionManager.canManageSubscription(subscriptionForSubscriptionId, str)) {
                    Log.e(TAG, "Not permitted to switch to subscription: " + i);
                    sendResult(pendingIntent, 2, null);
                    return;
                }
                iccId = subscriptionForSubscriptionId.getIccId();
            } else if (!zCallerCanWriteEmbeddedSubscriptions) {
                Log.e(TAG, "Not permitted to switch to empty subscription");
                sendResult(pendingIntent, 2, null);
                return;
            }
            if (zCallerCanWriteEmbeddedSubscriptions || canManageActiveSubscription(str)) {
                switchToSubscriptionPrivileged(jClearCallingIdentity, i, iccId, z2, str, pendingIntent);
                return;
            }
            Intent intent = new Intent();
            addResolutionIntent(intent, "android.service.euicc.action.RESOLVE_NO_PRIVILEGES", str, false, EuiccOperation.forSwitchNoPrivileges(jClearCallingIdentity, i, str));
            sendResult(pendingIntent, 1, intent);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    void switchToSubscriptionPrivileged(long j, int i, boolean z, String str, PendingIntent pendingIntent) {
        String iccId;
        SubscriptionInfo subscriptionForSubscriptionId = getSubscriptionForSubscriptionId(i);
        if (subscriptionForSubscriptionId != null) {
            iccId = subscriptionForSubscriptionId.getIccId();
        } else {
            iccId = null;
        }
        switchToSubscriptionPrivileged(j, i, iccId, z, str, pendingIntent);
    }

    void switchToSubscriptionPrivileged(final long j, final int i, String str, boolean z, final String str2, final PendingIntent pendingIntent) {
        this.mConnector.switchToSubscription(str, z, new EuiccConnector.SwitchCommandCallback() {
            @Override
            public void onSwitchComplete(int i2) {
                int i3;
                Intent intent = new Intent();
                switch (i2) {
                    case -1:
                        i3 = 1;
                        EuiccController.this.addResolutionIntent(intent, "android.service.euicc.action.RESOLVE_DEACTIVATE_SIM", str2, false, EuiccOperation.forSwitchDeactivateSim(j, i, str2));
                        break;
                    case 0:
                        i3 = 0;
                        break;
                    default:
                        intent.putExtra("android.telephony.euicc.extra.EMBEDDED_SUBSCRIPTION_DETAILED_CODE", i2);
                        i3 = 2;
                        break;
                }
                EuiccController.this.sendResult(pendingIntent, i3, intent);
            }

            @Override
            public void onEuiccServiceUnavailable() {
                EuiccController.this.sendResult(pendingIntent, 2, null);
            }
        });
    }

    public void updateSubscriptionNickname(int i, String str, final PendingIntent pendingIntent) {
        if (!callerCanWriteEmbeddedSubscriptions()) {
            throw new SecurityException("Must have WRITE_EMBEDDED_SUBSCRIPTIONS to update nickname");
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            SubscriptionInfo subscriptionForSubscriptionId = getSubscriptionForSubscriptionId(i);
            if (subscriptionForSubscriptionId == null) {
                Log.e(TAG, "Cannot update nickname to nonexistent subscription: " + i);
                sendResult(pendingIntent, 2, null);
                return;
            }
            this.mConnector.updateSubscriptionNickname(subscriptionForSubscriptionId.getIccId(), str, new EuiccConnector.UpdateNicknameCommandCallback() {
                @Override
                public void onUpdateNicknameComplete(int i2) {
                    int i3;
                    Intent intent = new Intent();
                    if (i2 == 0) {
                        i3 = 0;
                    } else {
                        i3 = 2;
                        intent.putExtra("android.telephony.euicc.extra.EMBEDDED_SUBSCRIPTION_DETAILED_CODE", i2);
                    }
                    EuiccController.this.sendResult(pendingIntent, i3, intent);
                }

                @Override
                public void onEuiccServiceUnavailable() {
                    EuiccController.this.sendResult(pendingIntent, 2, null);
                }
            });
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void eraseSubscriptions(final PendingIntent pendingIntent) {
        if (!callerCanWriteEmbeddedSubscriptions()) {
            throw new SecurityException("Must have WRITE_EMBEDDED_SUBSCRIPTIONS to erase subscriptions");
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mConnector.eraseSubscriptions(new EuiccConnector.EraseCommandCallback() {
                @Override
                public void onEraseComplete(int i) {
                    Intent intent = new Intent();
                    if (i == 0) {
                        EuiccController.this.refreshSubscriptionsAndSendResult(pendingIntent, 0, intent);
                    } else {
                        intent.putExtra("android.telephony.euicc.extra.EMBEDDED_SUBSCRIPTION_DETAILED_CODE", i);
                        EuiccController.this.sendResult(pendingIntent, 2, intent);
                    }
                }

                @Override
                public void onEuiccServiceUnavailable() {
                    EuiccController.this.sendResult(pendingIntent, 2, null);
                }
            });
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void retainSubscriptionsForFactoryReset(final PendingIntent pendingIntent) {
        this.mContext.enforceCallingPermission("android.permission.MASTER_CLEAR", "Must have MASTER_CLEAR to retain subscriptions for factory reset");
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mConnector.retainSubscriptions(new EuiccConnector.RetainSubscriptionsCommandCallback() {
                @Override
                public void onRetainSubscriptionsComplete(int i) {
                    int i2;
                    Intent intent = new Intent();
                    if (i == 0) {
                        i2 = 0;
                    } else {
                        i2 = 2;
                        intent.putExtra("android.telephony.euicc.extra.EMBEDDED_SUBSCRIPTION_DETAILED_CODE", i);
                    }
                    EuiccController.this.sendResult(pendingIntent, i2, intent);
                }

                @Override
                public void onEuiccServiceUnavailable() {
                    EuiccController.this.sendResult(pendingIntent, 2, null);
                }
            });
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public void refreshSubscriptionsAndSendResult(final PendingIntent pendingIntent, final int i, final Intent intent) {
        SubscriptionController.getInstance().requestEmbeddedSubscriptionInfoListRefresh(new Runnable() {
            @Override
            public final void run() {
                this.f$0.sendResult(pendingIntent, i, intent);
            }
        });
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public void sendResult(PendingIntent pendingIntent, int i, Intent intent) {
        try {
            pendingIntent.send(this.mContext, i, intent);
        } catch (PendingIntent.CanceledException e) {
        }
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public void addResolutionIntent(Intent intent, String str, String str2, boolean z, EuiccOperation euiccOperation) {
        Intent intent2 = new Intent("android.telephony.euicc.action.RESOLVE_ERROR");
        intent2.setPackage(RESOLUTION_ACTIVITY_PACKAGE_NAME);
        intent2.setComponent(new ComponentName(RESOLUTION_ACTIVITY_PACKAGE_NAME, RESOLUTION_ACTIVITY_CLASS_NAME));
        intent2.putExtra("android.telephony.euicc.extra.EMBEDDED_SUBSCRIPTION_RESOLUTION_ACTION", str);
        intent2.putExtra("android.service.euicc.extra.RESOLUTION_CALLING_PACKAGE", str2);
        intent2.putExtra("android.service.euicc.extra.RESOLUTION_CONFIRMATION_CODE_RETRIED", z);
        intent2.putExtra(EXTRA_OPERATION, euiccOperation);
        intent.putExtra("android.telephony.euicc.extra.EMBEDDED_SUBSCRIPTION_RESOLUTION_INTENT", PendingIntent.getActivity(this.mContext, 0, intent2, 1073741824));
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.DUMP", "Requires DUMP");
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mConnector.dump(fileDescriptor, printWriter, strArr);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public void sendOtaStatusChangedBroadcast() {
        Intent intent = new Intent("android.telephony.euicc.action.OTA_STATUS_CHANGED");
        EuiccConnector euiccConnector = this.mConnector;
        ComponentInfo componentInfoFindBestComponent = EuiccConnector.findBestComponent(this.mContext.getPackageManager());
        if (componentInfoFindBestComponent != null) {
            intent.setPackage(componentInfoFindBestComponent.packageName);
        }
        this.mContext.sendBroadcast(intent, "android.permission.WRITE_EMBEDDED_SUBSCRIPTIONS");
    }

    private SubscriptionInfo getSubscriptionForSubscriptionId(int i) {
        List availableSubscriptionInfoList = this.mSubscriptionManager.getAvailableSubscriptionInfoList();
        int size = availableSubscriptionInfoList.size();
        for (int i2 = 0; i2 < size; i2++) {
            SubscriptionInfo subscriptionInfo = (SubscriptionInfo) availableSubscriptionInfoList.get(i2);
            if (i == subscriptionInfo.getSubscriptionId()) {
                return subscriptionInfo;
            }
        }
        return null;
    }

    private String blockingGetEidFromEuiccService() {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final AtomicReference atomicReference = new AtomicReference();
        this.mConnector.getEid(new EuiccConnector.GetEidCommandCallback() {
            @Override
            public void onGetEidComplete(String str) {
                atomicReference.set(str);
                countDownLatch.countDown();
            }

            @Override
            public void onEuiccServiceUnavailable() {
                countDownLatch.countDown();
            }
        });
        return (String) awaitResult(countDownLatch, atomicReference);
    }

    private int blockingGetOtaStatusFromEuiccService() {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final AtomicReference atomicReference = new AtomicReference(5);
        this.mConnector.getOtaStatus(new EuiccConnector.GetOtaStatusCommandCallback() {
            @Override
            public void onGetOtaStatusComplete(int i) {
                atomicReference.set(Integer.valueOf(i));
                countDownLatch.countDown();
            }

            @Override
            public void onEuiccServiceUnavailable() {
                countDownLatch.countDown();
            }
        });
        return ((Integer) awaitResult(countDownLatch, atomicReference)).intValue();
    }

    private EuiccInfo blockingGetEuiccInfoFromEuiccService() {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final AtomicReference atomicReference = new AtomicReference();
        this.mConnector.getEuiccInfo(new EuiccConnector.GetEuiccInfoCommandCallback() {
            @Override
            public void onGetEuiccInfoComplete(EuiccInfo euiccInfo) {
                atomicReference.set(euiccInfo);
                countDownLatch.countDown();
            }

            @Override
            public void onEuiccServiceUnavailable() {
                countDownLatch.countDown();
            }
        });
        return (EuiccInfo) awaitResult(countDownLatch, atomicReference);
    }

    private static <T> T awaitResult(CountDownLatch countDownLatch, AtomicReference<T> atomicReference) {
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return atomicReference.get();
    }

    private boolean canManageActiveSubscription(String str) {
        List<SubscriptionInfo> activeSubscriptionInfoList = this.mSubscriptionManager.getActiveSubscriptionInfoList();
        if (activeSubscriptionInfoList == null) {
            return false;
        }
        int size = activeSubscriptionInfoList.size();
        for (int i = 0; i < size; i++) {
            SubscriptionInfo subscriptionInfo = activeSubscriptionInfoList.get(i);
            if (subscriptionInfo.isEmbedded() && this.mSubscriptionManager.canManageSubscription(subscriptionInfo, str)) {
                return true;
            }
        }
        return false;
    }

    private boolean callerCanReadPhoneStatePrivileged() {
        return this.mContext.checkCallingPermission("android.permission.READ_PRIVILEGED_PHONE_STATE") == 0;
    }

    private boolean callerCanWriteEmbeddedSubscriptions() {
        return this.mContext.checkCallingPermission("android.permission.WRITE_EMBEDDED_SUBSCRIPTIONS") == 0;
    }

    private boolean callerHasCarrierPrivilegesForActiveSubscription() {
        return ((TelephonyManager) this.mContext.getSystemService("phone")).hasCarrierPrivileges();
    }
}
