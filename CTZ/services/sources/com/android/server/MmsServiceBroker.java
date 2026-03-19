package com.android.server;

import android.app.ActivityManagerInternal;
import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.telephony.TelephonyManager;
import android.util.Slog;
import com.android.internal.telephony.IMms;
import com.android.server.UiModeManagerService;
import java.util.List;

public class MmsServiceBroker extends SystemService {
    private static final int MSG_TRY_CONNECTING = 1;
    private static final long RETRY_DELAY_ON_DISCONNECTION_MS = 3000;
    private static final long SERVICE_CONNECTION_WAIT_TIME_MS = 4000;
    private static final String TAG = "MmsServiceBroker";
    private volatile AppOpsManager mAppOpsManager;
    private ServiceConnection mConnection;
    private final Handler mConnectionHandler;
    private Context mContext;
    private volatile PackageManager mPackageManager;
    private volatile IMms mService;
    private final IMms mServiceStubForFailure;
    private volatile TelephonyManager mTelephonyManager;
    private static final ComponentName MMS_SERVICE_COMPONENT = new ComponentName("com.android.mms.service", "com.android.mms.service.MmsService");
    private static final Uri FAKE_SMS_SENT_URI = Uri.parse("content://sms/sent/0");
    private static final Uri FAKE_MMS_SENT_URI = Uri.parse("content://mms/sent/0");
    private static final Uri FAKE_SMS_DRAFT_URI = Uri.parse("content://sms/draft/0");
    private static final Uri FAKE_MMS_DRAFT_URI = Uri.parse("content://mms/draft/0");

    public MmsServiceBroker(Context context) {
        super(context);
        this.mAppOpsManager = null;
        this.mPackageManager = null;
        this.mTelephonyManager = null;
        this.mConnectionHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == 1) {
                    MmsServiceBroker.this.tryConnecting();
                } else {
                    Slog.e(MmsServiceBroker.TAG, "Unknown message");
                }
            }
        };
        this.mConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                Slog.i(MmsServiceBroker.TAG, "MmsService connected");
                synchronized (MmsServiceBroker.this) {
                    MmsServiceBroker.this.mService = IMms.Stub.asInterface(Binder.allowBlocking(iBinder));
                    MmsServiceBroker.this.notifyAll();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                Slog.i(MmsServiceBroker.TAG, "MmsService unexpectedly disconnected");
                synchronized (MmsServiceBroker.this) {
                    MmsServiceBroker.this.mService = null;
                    MmsServiceBroker.this.notifyAll();
                }
                MmsServiceBroker.this.mConnectionHandler.sendMessageDelayed(MmsServiceBroker.this.mConnectionHandler.obtainMessage(1), MmsServiceBroker.RETRY_DELAY_ON_DISCONNECTION_MS);
            }
        };
        this.mServiceStubForFailure = new IMms() {
            public IBinder asBinder() {
                return null;
            }

            public void sendMessage(int i, String str, Uri uri, String str2, Bundle bundle, PendingIntent pendingIntent) throws RemoteException {
                returnPendingIntentWithError(pendingIntent);
            }

            public void downloadMessage(int i, String str, String str2, Uri uri, Bundle bundle, PendingIntent pendingIntent) throws RemoteException {
                returnPendingIntentWithError(pendingIntent);
            }

            public Bundle getCarrierConfigValues(int i) throws RemoteException {
                return null;
            }

            public Uri importTextMessage(String str, String str2, int i, String str3, long j, boolean z, boolean z2) throws RemoteException {
                return null;
            }

            public Uri importMultimediaMessage(String str, Uri uri, String str2, long j, boolean z, boolean z2) throws RemoteException {
                return null;
            }

            public boolean deleteStoredMessage(String str, Uri uri) throws RemoteException {
                return false;
            }

            public boolean deleteStoredConversation(String str, long j) throws RemoteException {
                return false;
            }

            public boolean updateStoredMessageStatus(String str, Uri uri, ContentValues contentValues) throws RemoteException {
                return false;
            }

            public boolean archiveStoredConversation(String str, long j, boolean z) throws RemoteException {
                return false;
            }

            public Uri addTextMessageDraft(String str, String str2, String str3) throws RemoteException {
                return null;
            }

            public Uri addMultimediaMessageDraft(String str, Uri uri) throws RemoteException {
                return null;
            }

            public void sendStoredMessage(int i, String str, Uri uri, Bundle bundle, PendingIntent pendingIntent) throws RemoteException {
                returnPendingIntentWithError(pendingIntent);
            }

            public void setAutoPersisting(String str, boolean z) throws RemoteException {
            }

            public boolean getAutoPersisting() throws RemoteException {
                return false;
            }

            private void returnPendingIntentWithError(PendingIntent pendingIntent) {
                try {
                    pendingIntent.send(MmsServiceBroker.this.mContext, 1, (Intent) null);
                } catch (PendingIntent.CanceledException e) {
                    Slog.e(MmsServiceBroker.TAG, "Failed to return pending intent result", e);
                }
            }
        };
        this.mContext = context;
        this.mService = null;
    }

    @Override
    public void onStart() {
        publishBinderService("imms", new BinderService());
    }

    public void systemRunning() {
        Slog.i(TAG, "Delay connecting to MmsService until an API is called");
    }

    private void tryConnecting() {
        Slog.i(TAG, "Connecting to MmsService");
        synchronized (this) {
            if (this.mService != null) {
                Slog.d(TAG, "Already connected");
                return;
            }
            Intent intent = new Intent();
            intent.setComponent(MMS_SERVICE_COMPONENT);
            try {
                if (!this.mContext.bindService(intent, this.mConnection, 1)) {
                    Slog.e(TAG, "Failed to bind to MmsService");
                }
            } catch (SecurityException e) {
                Slog.e(TAG, "Forbidden to bind to MmsService", e);
            }
        }
    }

    private IMms getOrConnectService() {
        synchronized (this) {
            if (this.mService != null) {
                return this.mService;
            }
            Slog.w(TAG, "MmsService not connected. Try connecting...");
            this.mConnectionHandler.sendMessage(this.mConnectionHandler.obtainMessage(1));
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            long j = jElapsedRealtime + SERVICE_CONNECTION_WAIT_TIME_MS;
            for (long jElapsedRealtime2 = SERVICE_CONNECTION_WAIT_TIME_MS; jElapsedRealtime2 > 0; jElapsedRealtime2 = j - SystemClock.elapsedRealtime()) {
                try {
                    wait(jElapsedRealtime2);
                } catch (InterruptedException e) {
                    Slog.w(TAG, "Connection wait interrupted", e);
                }
                if (this.mService != null) {
                    return this.mService;
                }
            }
            Slog.e(TAG, "Can not connect to MmsService (timed out)");
            return null;
        }
    }

    private IMms getServiceGuarded() {
        IMms orConnectService = getOrConnectService();
        if (orConnectService != null) {
            return orConnectService;
        }
        return this.mServiceStubForFailure;
    }

    private AppOpsManager getAppOpsManager() {
        if (this.mAppOpsManager == null) {
            this.mAppOpsManager = (AppOpsManager) this.mContext.getSystemService("appops");
        }
        return this.mAppOpsManager;
    }

    private PackageManager getPackageManager() {
        if (this.mPackageManager == null) {
            this.mPackageManager = this.mContext.getPackageManager();
        }
        return this.mPackageManager;
    }

    private TelephonyManager getTelephonyManager() {
        if (this.mTelephonyManager == null) {
            this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        }
        return this.mTelephonyManager;
    }

    private String getCallingPackageName() {
        String[] packagesForUid = getPackageManager().getPackagesForUid(Binder.getCallingUid());
        if (packagesForUid != null && packagesForUid.length > 0) {
            return packagesForUid[0];
        }
        return UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN;
    }

    private final class BinderService extends IMms.Stub {
        private static final String PHONE_PACKAGE_NAME = "com.android.phone";

        private BinderService() {
        }

        public void sendMessage(int i, String str, Uri uri, String str2, Bundle bundle, PendingIntent pendingIntent) throws RemoteException {
            Slog.d(MmsServiceBroker.TAG, "sendMessage() by " + str);
            MmsServiceBroker.this.mContext.enforceCallingPermission("android.permission.SEND_SMS", "Send MMS message");
            if (MmsServiceBroker.this.getAppOpsManager().noteOp(20, Binder.getCallingUid(), str) != 0) {
                return;
            }
            MmsServiceBroker.this.getServiceGuarded().sendMessage(i, str, adjustUriForUserAndGrantPermission(uri, "android.service.carrier.CarrierMessagingService", 1), str2, bundle, pendingIntent);
        }

        public void downloadMessage(int i, String str, String str2, Uri uri, Bundle bundle, PendingIntent pendingIntent) throws RemoteException {
            Slog.d(MmsServiceBroker.TAG, "downloadMessage() by " + str);
            MmsServiceBroker.this.mContext.enforceCallingPermission("android.permission.RECEIVE_MMS", "Download MMS message");
            if (MmsServiceBroker.this.getAppOpsManager().noteOp(18, Binder.getCallingUid(), str) != 0) {
                return;
            }
            MmsServiceBroker.this.getServiceGuarded().downloadMessage(i, str, str2, adjustUriForUserAndGrantPermission(uri, "android.service.carrier.CarrierMessagingService", 3), bundle, pendingIntent);
        }

        public Bundle getCarrierConfigValues(int i) throws RemoteException {
            Slog.d(MmsServiceBroker.TAG, "getCarrierConfigValues() by " + MmsServiceBroker.this.getCallingPackageName());
            return MmsServiceBroker.this.getServiceGuarded().getCarrierConfigValues(i);
        }

        public Uri importTextMessage(String str, String str2, int i, String str3, long j, boolean z, boolean z2) throws RemoteException {
            return MmsServiceBroker.this.getAppOpsManager().noteOp(15, Binder.getCallingUid(), str) != 0 ? MmsServiceBroker.FAKE_SMS_SENT_URI : MmsServiceBroker.this.getServiceGuarded().importTextMessage(str, str2, i, str3, j, z, z2);
        }

        public Uri importMultimediaMessage(String str, Uri uri, String str2, long j, boolean z, boolean z2) throws RemoteException {
            return MmsServiceBroker.this.getAppOpsManager().noteOp(15, Binder.getCallingUid(), str) != 0 ? MmsServiceBroker.FAKE_MMS_SENT_URI : MmsServiceBroker.this.getServiceGuarded().importMultimediaMessage(str, uri, str2, j, z, z2);
        }

        public boolean deleteStoredMessage(String str, Uri uri) throws RemoteException {
            if (MmsServiceBroker.this.getAppOpsManager().noteOp(15, Binder.getCallingUid(), str) == 0) {
                return MmsServiceBroker.this.getServiceGuarded().deleteStoredMessage(str, uri);
            }
            return false;
        }

        public boolean deleteStoredConversation(String str, long j) throws RemoteException {
            if (MmsServiceBroker.this.getAppOpsManager().noteOp(15, Binder.getCallingUid(), str) == 0) {
                return MmsServiceBroker.this.getServiceGuarded().deleteStoredConversation(str, j);
            }
            return false;
        }

        public boolean updateStoredMessageStatus(String str, Uri uri, ContentValues contentValues) throws RemoteException {
            if (MmsServiceBroker.this.getAppOpsManager().noteOp(15, Binder.getCallingUid(), str) != 0) {
                return false;
            }
            return MmsServiceBroker.this.getServiceGuarded().updateStoredMessageStatus(str, uri, contentValues);
        }

        public boolean archiveStoredConversation(String str, long j, boolean z) throws RemoteException {
            if (MmsServiceBroker.this.getAppOpsManager().noteOp(15, Binder.getCallingUid(), str) != 0) {
                return false;
            }
            return MmsServiceBroker.this.getServiceGuarded().archiveStoredConversation(str, j, z);
        }

        public Uri addTextMessageDraft(String str, String str2, String str3) throws RemoteException {
            return MmsServiceBroker.this.getAppOpsManager().noteOp(15, Binder.getCallingUid(), str) != 0 ? MmsServiceBroker.FAKE_SMS_DRAFT_URI : MmsServiceBroker.this.getServiceGuarded().addTextMessageDraft(str, str2, str3);
        }

        public Uri addMultimediaMessageDraft(String str, Uri uri) throws RemoteException {
            return MmsServiceBroker.this.getAppOpsManager().noteOp(15, Binder.getCallingUid(), str) != 0 ? MmsServiceBroker.FAKE_MMS_DRAFT_URI : MmsServiceBroker.this.getServiceGuarded().addMultimediaMessageDraft(str, uri);
        }

        public void sendStoredMessage(int i, String str, Uri uri, Bundle bundle, PendingIntent pendingIntent) throws RemoteException {
            if (MmsServiceBroker.this.getAppOpsManager().noteOp(20, Binder.getCallingUid(), str) == 0) {
                MmsServiceBroker.this.getServiceGuarded().sendStoredMessage(i, str, uri, bundle, pendingIntent);
            }
        }

        public void setAutoPersisting(String str, boolean z) throws RemoteException {
            if (MmsServiceBroker.this.getAppOpsManager().noteOp(15, Binder.getCallingUid(), str) == 0) {
                MmsServiceBroker.this.getServiceGuarded().setAutoPersisting(str, z);
            }
        }

        public boolean getAutoPersisting() throws RemoteException {
            return MmsServiceBroker.this.getServiceGuarded().getAutoPersisting();
        }

        private Uri adjustUriForUserAndGrantPermission(Uri uri, String str, int i) {
            Intent intent = new Intent();
            intent.setData(uri);
            intent.setFlags(i);
            int callingUid = Binder.getCallingUid();
            int callingUserId = UserHandle.getCallingUserId();
            if (callingUserId != 0) {
                uri = ContentProvider.maybeAddUserId(uri, callingUserId);
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                ((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).grantUriPermissionFromIntent(callingUid, PHONE_PACKAGE_NAME, intent, 0);
                List carrierPackageNamesForIntent = ((TelephonyManager) MmsServiceBroker.this.mContext.getSystemService("phone")).getCarrierPackageNamesForIntent(new Intent(str));
                if (carrierPackageNamesForIntent != null && carrierPackageNamesForIntent.size() == 1) {
                    ((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).grantUriPermissionFromIntent(callingUid, (String) carrierPackageNamesForIntent.get(0), intent, 0);
                }
                return uri;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }
}
