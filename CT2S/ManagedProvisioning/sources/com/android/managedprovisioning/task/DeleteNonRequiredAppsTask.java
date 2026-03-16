package com.android.managedprovisioning.task;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Xml;
import com.android.internal.util.FastXmlSerializer;
import com.android.managedprovisioning.ProvisionLogger;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class DeleteNonRequiredAppsTask {
    private final Callback mCallback;
    private final Context mContext;
    private final boolean mDisableInstallShortcutListenersAndTelecom;
    private final IPackageManager mIpm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
    private final String mMdmPackageName;
    private final boolean mNewProfile;
    private final PackageManager mPm;
    private final int mReqAppsList;
    private final int mUserId;
    private final int mVendorReqAppsList;

    public static abstract class Callback {
        public abstract void onError();

        public abstract void onSuccess();
    }

    public DeleteNonRequiredAppsTask(Context context, String mdmPackageName, int userId, int requiredAppsList, int vendorRequiredAppsList, boolean newProfile, boolean disableInstallShortcutListenersAndTelecom, Callback callback) {
        this.mCallback = callback;
        this.mContext = context;
        this.mMdmPackageName = mdmPackageName;
        this.mUserId = userId;
        this.mPm = context.getPackageManager();
        this.mReqAppsList = requiredAppsList;
        this.mVendorReqAppsList = vendorRequiredAppsList;
        this.mNewProfile = newProfile;
        this.mDisableInstallShortcutListenersAndTelecom = disableInstallShortcutListenersAndTelecom;
    }

    public void run() {
        if (this.mNewProfile) {
            disableBluetoothSharing();
        }
        deleteNonRequiredApps();
    }

    public static boolean shouldDeleteNonRequiredApps(Context context, int userId) {
        return getSystemAppsFile(context, userId).exists();
    }

    private void disableBluetoothSharing() {
        ProvisionLogger.logd("Disabling Bluetooth sharing.");
        disableComponent(new ComponentName("com.android.bluetooth", "com.android.bluetooth.opp.BluetoothOppLauncherActivity"));
    }

    private void deleteNonRequiredApps() {
        Set<String> previousApps;
        ProvisionLogger.logd("Deleting non required apps.");
        File systemAppsFile = getSystemAppsFile(this.mContext, this.mUserId);
        systemAppsFile.getParentFile().mkdirs();
        Set<String> currentApps = getCurrentSystemApps();
        if (this.mNewProfile) {
            previousApps = new HashSet<>();
        } else {
            if (!systemAppsFile.exists()) {
                ProvisionLogger.loge("No system apps list found for user " + this.mUserId);
                this.mCallback.onError();
                return;
            }
            previousApps = readSystemApps(systemAppsFile);
        }
        writeSystemApps(currentApps, systemAppsFile);
        currentApps.removeAll(previousApps);
        if (this.mDisableInstallShortcutListenersAndTelecom) {
            Intent actionShortcut = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
            if (previousApps.isEmpty()) {
                disableReceivers(actionShortcut);
            } else {
                for (String newApp : currentApps) {
                    actionShortcut.setPackage(newApp);
                    disableReceivers(actionShortcut);
                }
            }
        }
        currentApps.removeAll(getRequiredApps());
        currentApps.retainAll(getCurrentAppsWithLauncher());
        if (this.mDisableInstallShortcutListenersAndTelecom && this.mNewProfile) {
            currentApps.add("com.android.server.telecom");
        }
        if (currentApps.isEmpty()) {
            this.mCallback.onSuccess();
            return;
        }
        PackageDeleteObserver packageDeleteObserver = new PackageDeleteObserver(currentApps.size());
        for (String packageName : currentApps) {
            try {
                this.mIpm.deletePackageAsUser(packageName, packageDeleteObserver, this.mUserId, 4);
            } catch (RemoteException neverThrown) {
                ProvisionLogger.loge("This should not happen.", neverThrown);
            }
        }
    }

    static File getSystemAppsFile(Context context, int userId) {
        return new File(context.getFilesDir() + File.separator + "system_apps" + File.separator + "user" + userId + ".xml");
    }

    private void disableReceivers(Intent intent) {
        ComponentInfo ci;
        List<ResolveInfo> receivers = this.mPm.queryBroadcastReceivers(intent, 0, this.mUserId);
        for (ResolveInfo ri : receivers) {
            if (ri.activityInfo != null) {
                ci = ri.activityInfo;
            } else if (ri.serviceInfo != null) {
                ci = ri.serviceInfo;
            } else {
                ci = ri.providerInfo;
            }
            disableComponent(new ComponentName(ci.packageName, ci.name));
        }
    }

    private void disableComponent(ComponentName toDisable) {
        try {
            this.mIpm.setComponentEnabledSetting(toDisable, 2, 1, this.mUserId);
        } catch (RemoteException neverThrown) {
            ProvisionLogger.loge("This should not happen.", neverThrown);
        } catch (Exception e) {
            ProvisionLogger.logw("Component not found, not disabling it: " + toDisable.toShortString());
        }
    }

    private Set<String> getCurrentSystemApps() {
        Set<String> apps = new HashSet<>();
        List<ApplicationInfo> aInfos = null;
        try {
            aInfos = this.mIpm.getInstalledApplications(8192, this.mUserId).getList();
        } catch (RemoteException neverThrown) {
            ProvisionLogger.loge("This should not happen.", neverThrown);
        }
        for (ApplicationInfo aInfo : aInfos) {
            if ((aInfo.flags & 1) != 0) {
                apps.add(aInfo.packageName);
            }
        }
        return apps;
    }

    private Set<String> getCurrentAppsWithLauncher() {
        Intent launcherIntent = new Intent("android.intent.action.MAIN");
        launcherIntent.addCategory("android.intent.category.LAUNCHER");
        List<ResolveInfo> resolveInfos = this.mPm.queryIntentActivitiesAsUser(launcherIntent, 8192, this.mUserId);
        Set<String> apps = new HashSet<>();
        for (ResolveInfo resolveInfo : resolveInfos) {
            apps.add(resolveInfo.activityInfo.packageName);
        }
        return apps;
    }

    private void writeSystemApps(Set<String> packageNames, File systemAppsFile) {
        try {
            FileOutputStream stream = new FileOutputStream(systemAppsFile, false);
            FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(stream, "utf-8");
            fastXmlSerializer.startDocument(null, true);
            fastXmlSerializer.startTag(null, "system-apps");
            for (String packageName : packageNames) {
                fastXmlSerializer.startTag(null, "item");
                fastXmlSerializer.attribute(null, "value", packageName);
                fastXmlSerializer.endTag(null, "item");
            }
            fastXmlSerializer.endTag(null, "system-apps");
            fastXmlSerializer.endDocument();
            stream.close();
        } catch (IOException e) {
            ProvisionLogger.loge("IOException trying to write the system apps", e);
        }
    }

    private Set<String> readSystemApps(File systemAppsFile) {
        Set<String> result = new HashSet<>();
        if (systemAppsFile.exists()) {
            try {
                FileInputStream stream = new FileInputStream(systemAppsFile);
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(stream, null);
                parser.next();
                int outerDepth = parser.getDepth();
                while (true) {
                    int type = parser.next();
                    if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                        break;
                    }
                    if (type != 3 && type != 4) {
                        String tag = parser.getName();
                        if (tag.equals("item")) {
                            result.add(parser.getAttributeValue(null, "value"));
                        } else {
                            ProvisionLogger.loge("Unknown tag: " + tag);
                        }
                    }
                }
                stream.close();
            } catch (IOException e) {
                ProvisionLogger.loge("IOException trying to read the system apps", e);
            } catch (XmlPullParserException e2) {
                ProvisionLogger.loge("XmlPullParserException trying to read the system apps", e2);
            }
        }
        return result;
    }

    protected Set<String> getRequiredApps() {
        HashSet<String> requiredApps = new HashSet<>(Arrays.asList(this.mContext.getResources().getStringArray(this.mReqAppsList)));
        requiredApps.addAll(Arrays.asList(this.mContext.getResources().getStringArray(this.mVendorReqAppsList)));
        requiredApps.add(this.mMdmPackageName);
        return requiredApps;
    }

    class PackageDeleteObserver extends IPackageDeleteObserver.Stub {
        private final AtomicInteger mPackageCount = new AtomicInteger(0);

        public PackageDeleteObserver(int packageCount) {
            this.mPackageCount.set(packageCount);
        }

        public void packageDeleted(String packageName, int returnCode) {
            if (returnCode != 1) {
                ProvisionLogger.logw("Could not finish the provisioning: package deletion failed");
                DeleteNonRequiredAppsTask.this.mCallback.onError();
            }
            int currentPackageCount = this.mPackageCount.decrementAndGet();
            if (currentPackageCount == 0) {
                ProvisionLogger.logi("All non-required system apps have been uninstalled.");
                DeleteNonRequiredAppsTask.this.mCallback.onSuccess();
            }
        }
    }
}
