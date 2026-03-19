package com.android.phone.euicc;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.IPackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.euicc.EuiccManager;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.euicc.EuiccConnector;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EuiccUiDispatcherActivity extends Activity {
    private final IPackageManager mPackageManager = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        try {
            Intent intentResolveEuiccUiIntent = resolveEuiccUiIntent();
            if (intentResolveEuiccUiIntent == null) {
                setResult(0);
                onDispatchFailure();
            } else {
                intentResolveEuiccUiIntent.setFlags(33554432);
                startActivity(intentResolveEuiccUiIntent);
            }
        } finally {
            finish();
        }
    }

    @VisibleForTesting
    Intent resolveEuiccUiIntent() {
        if (!((EuiccManager) getSystemService("euicc")).isEnabled()) {
            Log.w("EuiccUiDispatcher", "eUICC not enabled");
            return null;
        }
        Intent euiccUiIntent = getEuiccUiIntent();
        if (euiccUiIntent == null) {
            Log.w("EuiccUiDispatcher", "Unable to handle intent");
            return null;
        }
        revokePermissionFromLuiApps(euiccUiIntent);
        ActivityInfo activityInfoFindBestActivity = findBestActivity(euiccUiIntent);
        if (activityInfoFindBestActivity == null) {
            Log.w("EuiccUiDispatcher", "Could not resolve activity for intent: " + euiccUiIntent);
            return null;
        }
        grantDefaultPermissionsToActiveLuiApp(activityInfoFindBestActivity);
        euiccUiIntent.setComponent(activityInfoFindBestActivity.getComponentName());
        return euiccUiIntent;
    }

    protected void onDispatchFailure() {
    }

    protected Intent getEuiccUiIntent() {
        byte b;
        String action = getIntent().getAction();
        Intent intent = new Intent();
        intent.putExtras(getIntent());
        int iHashCode = action.hashCode();
        if (iHashCode != -344086803) {
            b = (iHashCode == -281938662 && action.equals("android.telephony.euicc.action.PROVISION_EMBEDDED_SUBSCRIPTION")) ? (byte) 1 : (byte) -1;
        } else if (action.equals("android.telephony.euicc.action.MANAGE_EMBEDDED_SUBSCRIPTIONS")) {
            b = 0;
        }
        switch (b) {
            case 0:
                intent.setAction("android.service.euicc.action.MANAGE_EMBEDDED_SUBSCRIPTIONS");
                return intent;
            case 1:
                intent.setAction("android.service.euicc.action.PROVISION_EMBEDDED_SUBSCRIPTION");
                return intent;
            default:
                Log.w("EuiccUiDispatcher", "Unsupported action: " + action);
                return null;
        }
    }

    @VisibleForTesting
    ActivityInfo findBestActivity(Intent intent) {
        return EuiccConnector.findBestActivity(getPackageManager(), intent);
    }

    @VisibleForTesting
    protected void grantDefaultPermissionsToActiveLuiApp(ActivityInfo activityInfo) {
        try {
            this.mPackageManager.grantDefaultPermissionsToActiveLuiApp(activityInfo.packageName, getUserId());
        } catch (RemoteException e) {
            Log.e("EuiccUiDispatcher", "Failed to grant permissions to active LUI app.", e);
        }
    }

    @VisibleForTesting
    protected void revokePermissionFromLuiApps(Intent intent) {
        try {
            Set<String> allLuiAppPackageNames = getAllLuiAppPackageNames(intent);
            this.mPackageManager.revokeDefaultPermissionsFromLuiApps((String[]) allLuiAppPackageNames.toArray(new String[allLuiAppPackageNames.size()]), getUserId());
        } catch (RemoteException e) {
            Log.e("EuiccUiDispatcher", "Failed to revoke LUI app permissions.");
            throw e.rethrowAsRuntimeException();
        }
    }

    private Set<String> getAllLuiAppPackageNames(Intent intent) {
        List<ResolveInfo> listQueryIntentServices = getPackageManager().queryIntentServices(intent, 269484096);
        HashSet hashSet = new HashSet();
        for (ResolveInfo resolveInfo : listQueryIntentServices) {
            if (resolveInfo.serviceInfo != null) {
                hashSet.add(resolveInfo.serviceInfo.packageName);
            }
        }
        return hashSet;
    }
}
