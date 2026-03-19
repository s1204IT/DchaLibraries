package android.telephony.euicc;

import android.annotation.SystemApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import com.android.internal.telephony.euicc.IEuiccController;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class EuiccManager {
    public static final String ACTION_MANAGE_EMBEDDED_SUBSCRIPTIONS = "android.telephony.euicc.action.MANAGE_EMBEDDED_SUBSCRIPTIONS";
    public static final String ACTION_NOTIFY_CARRIER_SETUP_INCOMPLETE = "android.telephony.euicc.action.NOTIFY_CARRIER_SETUP_INCOMPLETE";

    @SystemApi
    public static final String ACTION_OTA_STATUS_CHANGED = "android.telephony.euicc.action.OTA_STATUS_CHANGED";

    @SystemApi
    public static final String ACTION_PROVISION_EMBEDDED_SUBSCRIPTION = "android.telephony.euicc.action.PROVISION_EMBEDDED_SUBSCRIPTION";
    public static final String ACTION_RESOLVE_ERROR = "android.telephony.euicc.action.RESOLVE_ERROR";
    public static final int EMBEDDED_SUBSCRIPTION_RESULT_ERROR = 2;
    public static final int EMBEDDED_SUBSCRIPTION_RESULT_OK = 0;
    public static final int EMBEDDED_SUBSCRIPTION_RESULT_RESOLVABLE_ERROR = 1;

    @SystemApi
    public static final int EUICC_OTA_FAILED = 2;

    @SystemApi
    public static final int EUICC_OTA_IN_PROGRESS = 1;

    @SystemApi
    public static final int EUICC_OTA_NOT_NEEDED = 4;

    @SystemApi
    public static final int EUICC_OTA_STATUS_UNAVAILABLE = 5;

    @SystemApi
    public static final int EUICC_OTA_SUCCEEDED = 3;
    public static final String EXTRA_EMBEDDED_SUBSCRIPTION_DETAILED_CODE = "android.telephony.euicc.extra.EMBEDDED_SUBSCRIPTION_DETAILED_CODE";
    public static final String EXTRA_EMBEDDED_SUBSCRIPTION_DOWNLOADABLE_SUBSCRIPTION = "android.telephony.euicc.extra.EMBEDDED_SUBSCRIPTION_DOWNLOADABLE_SUBSCRIPTION";

    @SystemApi
    public static final String EXTRA_EMBEDDED_SUBSCRIPTION_DOWNLOADABLE_SUBSCRIPTIONS = "android.telephony.euicc.extra.EMBEDDED_SUBSCRIPTION_DOWNLOADABLE_SUBSCRIPTIONS";
    public static final String EXTRA_EMBEDDED_SUBSCRIPTION_RESOLUTION_ACTION = "android.telephony.euicc.extra.EMBEDDED_SUBSCRIPTION_RESOLUTION_ACTION";
    public static final String EXTRA_EMBEDDED_SUBSCRIPTION_RESOLUTION_CALLBACK_INTENT = "android.telephony.euicc.extra.EMBEDDED_SUBSCRIPTION_RESOLUTION_CALLBACK_INTENT";
    public static final String EXTRA_EMBEDDED_SUBSCRIPTION_RESOLUTION_INTENT = "android.telephony.euicc.extra.EMBEDDED_SUBSCRIPTION_RESOLUTION_INTENT";
    public static final String EXTRA_FORCE_PROVISION = "android.telephony.euicc.extra.FORCE_PROVISION";
    public static final String META_DATA_CARRIER_ICON = "android.telephony.euicc.carriericon";
    private final Context mContext;

    @SystemApi
    @Retention(RetentionPolicy.SOURCE)
    public @interface OtaStatus {
    }

    public EuiccManager(Context context) {
        this.mContext = context;
    }

    public boolean isEnabled() {
        return getIEuiccController() != null;
    }

    public String getEid() {
        if (!isEnabled()) {
            return null;
        }
        try {
            return getIEuiccController().getEid();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public int getOtaStatus() {
        if (!isEnabled()) {
            return 5;
        }
        try {
            return getIEuiccController().getOtaStatus();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void downloadSubscription(DownloadableSubscription downloadableSubscription, boolean z, PendingIntent pendingIntent) {
        if (!isEnabled()) {
            sendUnavailableError(pendingIntent);
            return;
        }
        try {
            getIEuiccController().downloadSubscription(downloadableSubscription, z, this.mContext.getOpPackageName(), pendingIntent);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void startResolutionActivity(Activity activity, int i, Intent intent, PendingIntent pendingIntent) throws IntentSender.SendIntentException {
        PendingIntent pendingIntent2 = (PendingIntent) intent.getParcelableExtra(EXTRA_EMBEDDED_SUBSCRIPTION_RESOLUTION_INTENT);
        if (pendingIntent2 == null) {
            throw new IllegalArgumentException("Invalid result intent");
        }
        Intent intent2 = new Intent();
        intent2.putExtra(EXTRA_EMBEDDED_SUBSCRIPTION_RESOLUTION_CALLBACK_INTENT, pendingIntent);
        activity.startIntentSenderForResult(pendingIntent2.getIntentSender(), i, intent2, 0, 0, 0);
    }

    @SystemApi
    public void continueOperation(Intent intent, Bundle bundle) {
        if (!isEnabled()) {
            PendingIntent pendingIntent = (PendingIntent) intent.getParcelableExtra(EXTRA_EMBEDDED_SUBSCRIPTION_RESOLUTION_CALLBACK_INTENT);
            if (pendingIntent != null) {
                sendUnavailableError(pendingIntent);
                return;
            }
            return;
        }
        try {
            getIEuiccController().continueOperation(intent, bundle);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public void getDownloadableSubscriptionMetadata(DownloadableSubscription downloadableSubscription, PendingIntent pendingIntent) {
        if (!isEnabled()) {
            sendUnavailableError(pendingIntent);
            return;
        }
        try {
            getIEuiccController().getDownloadableSubscriptionMetadata(downloadableSubscription, this.mContext.getOpPackageName(), pendingIntent);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public void getDefaultDownloadableSubscriptionList(PendingIntent pendingIntent) {
        if (!isEnabled()) {
            sendUnavailableError(pendingIntent);
            return;
        }
        try {
            getIEuiccController().getDefaultDownloadableSubscriptionList(this.mContext.getOpPackageName(), pendingIntent);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public EuiccInfo getEuiccInfo() {
        if (!isEnabled()) {
            return null;
        }
        try {
            return getIEuiccController().getEuiccInfo();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void deleteSubscription(int i, PendingIntent pendingIntent) {
        if (!isEnabled()) {
            sendUnavailableError(pendingIntent);
            return;
        }
        try {
            getIEuiccController().deleteSubscription(i, this.mContext.getOpPackageName(), pendingIntent);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void switchToSubscription(int i, PendingIntent pendingIntent) {
        if (!isEnabled()) {
            sendUnavailableError(pendingIntent);
            return;
        }
        try {
            getIEuiccController().switchToSubscription(i, this.mContext.getOpPackageName(), pendingIntent);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void updateSubscriptionNickname(int i, String str, PendingIntent pendingIntent) {
        if (!isEnabled()) {
            sendUnavailableError(pendingIntent);
            return;
        }
        try {
            getIEuiccController().updateSubscriptionNickname(i, str, pendingIntent);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public void eraseSubscriptions(PendingIntent pendingIntent) {
        if (!isEnabled()) {
            sendUnavailableError(pendingIntent);
            return;
        }
        try {
            getIEuiccController().eraseSubscriptions(pendingIntent);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void retainSubscriptionsForFactoryReset(PendingIntent pendingIntent) {
        if (!isEnabled()) {
            sendUnavailableError(pendingIntent);
            return;
        }
        try {
            getIEuiccController().retainSubscriptionsForFactoryReset(pendingIntent);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private static void sendUnavailableError(PendingIntent pendingIntent) {
        try {
            pendingIntent.send(2);
        } catch (PendingIntent.CanceledException e) {
        }
    }

    private static IEuiccController getIEuiccController() {
        return IEuiccController.Stub.asInterface(ServiceManager.getService("econtroller"));
    }
}
