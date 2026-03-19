package com.android.server.devicepolicy;

import android.R;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.ArraySet;
import android.view.inputmethod.InputMethodInfo;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import com.android.internal.view.IInputMethodManager;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class OverlayPackagesProvider {
    protected static final String TAG = "OverlayPackagesProvider";
    private final Context mContext;
    private final IInputMethodManager mIInputMethodManager;
    private final PackageManager mPm;

    public OverlayPackagesProvider(Context context) {
        this(context, getIInputMethodManager());
    }

    @VisibleForTesting
    OverlayPackagesProvider(Context context, IInputMethodManager iInputMethodManager) {
        this.mContext = context;
        this.mPm = (PackageManager) Preconditions.checkNotNull(context.getPackageManager());
        this.mIInputMethodManager = (IInputMethodManager) Preconditions.checkNotNull(iInputMethodManager);
    }

    public Set<String> getNonRequiredApps(ComponentName componentName, int i, String str) {
        Set<String> launchableApps = getLaunchableApps(i);
        launchableApps.removeAll(getRequiredApps(str, componentName.getPackageName()));
        if ("android.app.action.PROVISION_MANAGED_DEVICE".equals(str) || "android.app.action.PROVISION_MANAGED_USER".equals(str)) {
            launchableApps.removeAll(getSystemInputMethods());
        }
        launchableApps.addAll(getDisallowedApps(str));
        return launchableApps;
    }

    private Set<String> getLaunchableApps(int i) {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.LAUNCHER");
        List listQueryIntentActivitiesAsUser = this.mPm.queryIntentActivitiesAsUser(intent, 795136, i);
        ArraySet arraySet = new ArraySet();
        Iterator it = listQueryIntentActivitiesAsUser.iterator();
        while (it.hasNext()) {
            arraySet.add(((ResolveInfo) it.next()).activityInfo.packageName);
        }
        return arraySet;
    }

    private Set<String> getSystemInputMethods() {
        try {
            List<InputMethodInfo> inputMethodList = this.mIInputMethodManager.getInputMethodList();
            ArraySet arraySet = new ArraySet();
            for (InputMethodInfo inputMethodInfo : inputMethodList) {
                if (inputMethodInfo.getServiceInfo().applicationInfo.isSystemApp()) {
                    arraySet.add(inputMethodInfo.getPackageName());
                }
            }
            return arraySet;
        } catch (RemoteException e) {
            return null;
        }
    }

    private Set<String> getRequiredApps(String str, String str2) {
        ArraySet arraySet = new ArraySet();
        arraySet.addAll(getRequiredAppsSet(str));
        arraySet.addAll(getVendorRequiredAppsSet(str));
        arraySet.add(str2);
        return arraySet;
    }

    private Set<String> getDisallowedApps(String str) {
        ArraySet arraySet = new ArraySet();
        arraySet.addAll(getDisallowedAppsSet(str));
        arraySet.addAll(getVendorDisallowedAppsSet(str));
        return arraySet;
    }

    private static IInputMethodManager getIInputMethodManager() {
        return IInputMethodManager.Stub.asInterface(ServiceManager.getService("input_method"));
    }

    private Set<String> getRequiredAppsSet(String str) {
        byte b;
        int i;
        int iHashCode = str.hashCode();
        if (iHashCode != -920528692) {
            if (iHashCode != -514404415) {
                b = (iHashCode == -340845101 && str.equals("android.app.action.PROVISION_MANAGED_PROFILE")) ? (byte) 1 : (byte) -1;
            } else if (str.equals("android.app.action.PROVISION_MANAGED_USER")) {
                b = 0;
            }
        } else if (str.equals("android.app.action.PROVISION_MANAGED_DEVICE")) {
            b = 2;
        }
        switch (b) {
            case 0:
                i = R.array.config_dockExtconStateMapping;
                break;
            case 1:
                i = R.array.config_display_no_service_when_sim_unready;
                break;
            case 2:
                i = R.array.config_displayWhiteBalanceStrongDisplayColorTemperatures;
                break;
            default:
                throw new IllegalArgumentException("Provisioning type " + str + " not supported.");
        }
        return new ArraySet(Arrays.asList(this.mContext.getResources().getStringArray(i)));
    }

    private Set<String> getDisallowedAppsSet(String str) {
        byte b;
        int i;
        int iHashCode = str.hashCode();
        if (iHashCode != -920528692) {
            if (iHashCode != -514404415) {
                b = (iHashCode == -340845101 && str.equals("android.app.action.PROVISION_MANAGED_PROFILE")) ? (byte) 1 : (byte) -1;
            } else if (str.equals("android.app.action.PROVISION_MANAGED_USER")) {
                b = 0;
            }
        } else if (str.equals("android.app.action.PROVISION_MANAGED_DEVICE")) {
            b = 2;
        }
        switch (b) {
            case 0:
                i = R.array.config_displayWhiteBalanceDisplayNominalWhite;
                break;
            case 1:
                i = R.array.config_displayWhiteBalanceDisplayColorTemperatures;
                break;
            case 2:
                i = R.array.config_displayWhiteBalanceDecreaseThresholds;
                break;
            default:
                throw new IllegalArgumentException("Provisioning type " + str + " not supported.");
        }
        return new ArraySet(Arrays.asList(this.mContext.getResources().getStringArray(i)));
    }

    private Set<String> getVendorRequiredAppsSet(String str) {
        byte b;
        int i;
        int iHashCode = str.hashCode();
        if (iHashCode != -920528692) {
            if (iHashCode != -514404415) {
                b = (iHashCode == -340845101 && str.equals("android.app.action.PROVISION_MANAGED_PROFILE")) ? (byte) 1 : (byte) -1;
            } else if (str.equals("android.app.action.PROVISION_MANAGED_USER")) {
                b = 0;
            }
        } else if (str.equals("android.app.action.PROVISION_MANAGED_DEVICE")) {
            b = 2;
        }
        switch (b) {
            case 0:
                i = R.array.config_face_acquire_vendor_enroll_ignorelist;
                break;
            case 1:
                i = R.array.config_face_acquire_vendor_biometricprompt_ignorelist;
                break;
            case 2:
                i = R.array.config_face_acquire_keyguard_ignorelist;
                break;
            default:
                throw new IllegalArgumentException("Provisioning type " + str + " not supported.");
        }
        return new ArraySet(Arrays.asList(this.mContext.getResources().getStringArray(i)));
    }

    private Set<String> getVendorDisallowedAppsSet(String str) {
        byte b;
        int i;
        int iHashCode = str.hashCode();
        if (iHashCode != -920528692) {
            if (iHashCode != -514404415) {
                b = (iHashCode == -340845101 && str.equals("android.app.action.PROVISION_MANAGED_PROFILE")) ? (byte) 1 : (byte) -1;
            } else if (str.equals("android.app.action.PROVISION_MANAGED_USER")) {
                b = 0;
            }
        } else if (str.equals("android.app.action.PROVISION_MANAGED_DEVICE")) {
            b = 2;
        }
        switch (b) {
            case 0:
                i = R.array.config_face_acquire_enroll_ignorelist;
                break;
            case 1:
                i = R.array.config_face_acquire_biometricprompt_ignorelist;
                break;
            case 2:
                i = R.array.config_ethernet_interfaces;
                break;
            default:
                throw new IllegalArgumentException("Provisioning type " + str + " not supported.");
        }
        return new ArraySet(Arrays.asList(this.mContext.getResources().getStringArray(i)));
    }
}
