package com.android.server.backup;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.backup.IBackupTransport;
import com.android.internal.util.Preconditions;
import com.android.server.backup.transport.OnTransportRegisteredListener;
import com.android.server.backup.transport.TransportClient;
import com.android.server.backup.transport.TransportClientManager;
import com.android.server.backup.transport.TransportNotAvailableException;
import com.android.server.backup.transport.TransportNotRegisteredException;
import com.android.server.backup.transport.TransportStats;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class TransportManager {

    @VisibleForTesting
    public static final String SERVICE_ACTION_TRANSPORT_HOST = "android.backup.TRANSPORT_HOST";
    private static final String TAG = "BackupTransportManager";
    private final Context mContext;

    @GuardedBy("mTransportLock")
    private volatile String mCurrentTransportName;
    private final PackageManager mPackageManager;
    private final TransportClientManager mTransportClientManager;
    private final Set<ComponentName> mTransportWhitelist;
    private final Intent mTransportServiceIntent = new Intent(SERVICE_ACTION_TRANSPORT_HOST);
    private OnTransportRegisteredListener mOnTransportRegisteredListener = $$Lambda$TransportManager$Z9ckpFUW2V4jkdHnyXIEiLuAoBc.INSTANCE;
    private final Object mTransportLock = new Object();

    @GuardedBy("mTransportLock")
    private final Map<ComponentName, TransportDescription> mRegisteredTransportsDescriptionMap = new ArrayMap();
    private final TransportStats mTransportStats = new TransportStats();

    static void lambda$new$0(String str, String str2) {
    }

    TransportManager(Context context, Set<ComponentName> set, String str) {
        this.mContext = context;
        this.mPackageManager = context.getPackageManager();
        this.mTransportWhitelist = (Set) Preconditions.checkNotNull(set);
        this.mCurrentTransportName = str;
        this.mTransportClientManager = new TransportClientManager(context, this.mTransportStats);
    }

    @VisibleForTesting
    TransportManager(Context context, Set<ComponentName> set, String str, TransportClientManager transportClientManager) {
        this.mContext = context;
        this.mPackageManager = context.getPackageManager();
        this.mTransportWhitelist = (Set) Preconditions.checkNotNull(set);
        this.mCurrentTransportName = str;
        this.mTransportClientManager = transportClientManager;
    }

    public void setOnTransportRegisteredListener(OnTransportRegisteredListener onTransportRegisteredListener) {
        this.mOnTransportRegisteredListener = onTransportRegisteredListener;
    }

    static boolean lambda$onPackageAdded$1(ComponentName componentName) {
        return true;
    }

    void onPackageAdded(String str) {
        registerTransportsFromPackage(str, new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return TransportManager.lambda$onPackageAdded$1((ComponentName) obj);
            }
        });
    }

    void onPackageRemoved(String str) {
        synchronized (this.mTransportLock) {
            this.mRegisteredTransportsDescriptionMap.keySet().removeIf(fromPackageFilter(str));
        }
    }

    void onPackageChanged(String str, String... strArr) {
        final ArraySet arraySet = new ArraySet(strArr.length);
        for (String str2 : strArr) {
            arraySet.add(new ComponentName(str, str2));
        }
        synchronized (this.mTransportLock) {
            Set<ComponentName> setKeySet = this.mRegisteredTransportsDescriptionMap.keySet();
            Objects.requireNonNull(arraySet);
            setKeySet.removeIf(new Predicate() {
                @Override
                public final boolean test(Object obj) {
                    return arraySet.contains((ComponentName) obj);
                }
            });
        }
        Objects.requireNonNull(arraySet);
        registerTransportsFromPackage(str, new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return arraySet.contains((ComponentName) obj);
            }
        });
    }

    ComponentName[] getRegisteredTransportComponents() {
        ComponentName[] componentNameArr;
        synchronized (this.mTransportLock) {
            componentNameArr = (ComponentName[]) this.mRegisteredTransportsDescriptionMap.keySet().toArray(new ComponentName[this.mRegisteredTransportsDescriptionMap.size()]);
        }
        return componentNameArr;
    }

    String[] getRegisteredTransportNames() {
        String[] strArr;
        synchronized (this.mTransportLock) {
            strArr = new String[this.mRegisteredTransportsDescriptionMap.size()];
            int i = 0;
            Iterator<TransportDescription> it = this.mRegisteredTransportsDescriptionMap.values().iterator();
            while (it.hasNext()) {
                strArr[i] = it.next().name;
                i++;
            }
        }
        return strArr;
    }

    Set<ComponentName> getTransportWhitelist() {
        return this.mTransportWhitelist;
    }

    String getCurrentTransportName() {
        return this.mCurrentTransportName;
    }

    public String getTransportName(ComponentName componentName) throws TransportNotRegisteredException {
        String str;
        synchronized (this.mTransportLock) {
            str = getRegisteredTransportDescriptionOrThrowLocked(componentName).name;
        }
        return str;
    }

    public String getTransportDirName(ComponentName componentName) throws TransportNotRegisteredException {
        String str;
        synchronized (this.mTransportLock) {
            str = getRegisteredTransportDescriptionOrThrowLocked(componentName).transportDirName;
        }
        return str;
    }

    public String getTransportDirName(String str) throws TransportNotRegisteredException {
        String str2;
        synchronized (this.mTransportLock) {
            str2 = getRegisteredTransportDescriptionOrThrowLocked(str).transportDirName;
        }
        return str2;
    }

    public Intent getTransportConfigurationIntent(String str) throws TransportNotRegisteredException {
        Intent intent;
        synchronized (this.mTransportLock) {
            intent = getRegisteredTransportDescriptionOrThrowLocked(str).configurationIntent;
        }
        return intent;
    }

    public String getTransportCurrentDestinationString(String str) throws TransportNotRegisteredException {
        String str2;
        synchronized (this.mTransportLock) {
            str2 = getRegisteredTransportDescriptionOrThrowLocked(str).currentDestinationString;
        }
        return str2;
    }

    public Intent getTransportDataManagementIntent(String str) throws TransportNotRegisteredException {
        Intent intent;
        synchronized (this.mTransportLock) {
            intent = getRegisteredTransportDescriptionOrThrowLocked(str).dataManagementIntent;
        }
        return intent;
    }

    public String getTransportDataManagementLabel(String str) throws TransportNotRegisteredException {
        String str2;
        synchronized (this.mTransportLock) {
            str2 = getRegisteredTransportDescriptionOrThrowLocked(str).dataManagementLabel;
        }
        return str2;
    }

    public boolean isTransportRegistered(String str) {
        boolean z;
        synchronized (this.mTransportLock) {
            z = getRegisteredTransportEntryLocked(str) != null;
        }
        return z;
    }

    public void forEachRegisteredTransport(Consumer<String> consumer) {
        synchronized (this.mTransportLock) {
            Iterator<TransportDescription> it = this.mRegisteredTransportsDescriptionMap.values().iterator();
            while (it.hasNext()) {
                consumer.accept(it.next().name);
            }
        }
    }

    public void updateTransportAttributes(ComponentName componentName, String str, Intent intent, String str2, Intent intent2, String str3) {
        synchronized (this.mTransportLock) {
            TransportDescription transportDescription = this.mRegisteredTransportsDescriptionMap.get(componentName);
            if (transportDescription == null) {
                Slog.e(TAG, "Transport " + str + " not registered tried to change description");
                return;
            }
            transportDescription.name = str;
            transportDescription.configurationIntent = intent;
            transportDescription.currentDestinationString = str2;
            transportDescription.dataManagementIntent = intent2;
            transportDescription.dataManagementLabel = str3;
            Slog.d(TAG, "Transport " + str + " updated its attributes");
        }
    }

    @GuardedBy("mTransportLock")
    private TransportDescription getRegisteredTransportDescriptionOrThrowLocked(ComponentName componentName) throws TransportNotRegisteredException {
        TransportDescription transportDescription = this.mRegisteredTransportsDescriptionMap.get(componentName);
        if (transportDescription == null) {
            throw new TransportNotRegisteredException(componentName);
        }
        return transportDescription;
    }

    @GuardedBy("mTransportLock")
    private TransportDescription getRegisteredTransportDescriptionOrThrowLocked(String str) throws TransportNotRegisteredException {
        TransportDescription registeredTransportDescriptionLocked = getRegisteredTransportDescriptionLocked(str);
        if (registeredTransportDescriptionLocked == null) {
            throw new TransportNotRegisteredException(str);
        }
        return registeredTransportDescriptionLocked;
    }

    @GuardedBy("mTransportLock")
    private ComponentName getRegisteredTransportComponentLocked(String str) {
        Map.Entry<ComponentName, TransportDescription> registeredTransportEntryLocked = getRegisteredTransportEntryLocked(str);
        if (registeredTransportEntryLocked == null) {
            return null;
        }
        return registeredTransportEntryLocked.getKey();
    }

    @GuardedBy("mTransportLock")
    private TransportDescription getRegisteredTransportDescriptionLocked(String str) {
        Map.Entry<ComponentName, TransportDescription> registeredTransportEntryLocked = getRegisteredTransportEntryLocked(str);
        if (registeredTransportEntryLocked == null) {
            return null;
        }
        return registeredTransportEntryLocked.getValue();
    }

    @GuardedBy("mTransportLock")
    private Map.Entry<ComponentName, TransportDescription> getRegisteredTransportEntryLocked(String str) {
        for (Map.Entry<ComponentName, TransportDescription> entry : this.mRegisteredTransportsDescriptionMap.entrySet()) {
            if (str.equals(entry.getValue().name)) {
                return entry;
            }
        }
        return null;
    }

    public TransportClient getTransportClient(String str, String str2) {
        try {
            return getTransportClientOrThrow(str, str2);
        } catch (TransportNotRegisteredException e) {
            Slog.w(TAG, "Transport " + str + " not registered");
            return null;
        }
    }

    public TransportClient getTransportClientOrThrow(String str, String str2) throws TransportNotRegisteredException {
        TransportClient transportClient;
        synchronized (this.mTransportLock) {
            ComponentName registeredTransportComponentLocked = getRegisteredTransportComponentLocked(str);
            if (registeredTransportComponentLocked == null) {
                throw new TransportNotRegisteredException(str);
            }
            transportClient = this.mTransportClientManager.getTransportClient(registeredTransportComponentLocked, str2);
        }
        return transportClient;
    }

    public TransportClient getCurrentTransportClient(String str) {
        TransportClient transportClient;
        synchronized (this.mTransportLock) {
            transportClient = getTransportClient(this.mCurrentTransportName, str);
        }
        return transportClient;
    }

    public TransportClient getCurrentTransportClientOrThrow(String str) throws TransportNotRegisteredException {
        TransportClient transportClientOrThrow;
        synchronized (this.mTransportLock) {
            transportClientOrThrow = getTransportClientOrThrow(this.mCurrentTransportName, str);
        }
        return transportClientOrThrow;
    }

    public void disposeOfTransportClient(TransportClient transportClient, String str) {
        this.mTransportClientManager.disposeOfTransportClient(transportClient, str);
    }

    @Deprecated
    String selectTransport(String str) {
        String str2;
        synchronized (this.mTransportLock) {
            str2 = this.mCurrentTransportName;
            this.mCurrentTransportName = str;
        }
        return str2;
    }

    public int registerAndSelectTransport(ComponentName componentName) {
        synchronized (this.mTransportLock) {
            try {
                try {
                    selectTransport(getTransportName(componentName));
                } catch (TransportNotRegisteredException e) {
                    int iRegisterTransport = registerTransport(componentName);
                    if (iRegisterTransport != 0) {
                        return iRegisterTransport;
                    }
                    synchronized (this.mTransportLock) {
                        try {
                            try {
                                selectTransport(getTransportName(componentName));
                                return 0;
                            } finally {
                            }
                        } catch (TransportNotRegisteredException e2) {
                            Slog.wtf(TAG, "Transport got unregistered");
                            return -1;
                        }
                    }
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return 0;
    }

    static boolean lambda$registerTransports$2(ComponentName componentName) {
        return true;
    }

    public void registerTransports() {
        registerTransportsForIntent(this.mTransportServiceIntent, new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return TransportManager.lambda$registerTransports$2((ComponentName) obj);
            }
        });
    }

    private void registerTransportsFromPackage(String str, Predicate<ComponentName> predicate) {
        try {
            this.mPackageManager.getPackageInfo(str, 0);
            registerTransportsForIntent(new Intent(this.mTransportServiceIntent).setPackage(str), predicate.and(fromPackageFilter(str)));
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(TAG, "Trying to register transports from package not found " + str);
        }
    }

    private void registerTransportsForIntent(Intent intent, Predicate<ComponentName> predicate) {
        List listQueryIntentServicesAsUser = this.mPackageManager.queryIntentServicesAsUser(intent, 0, 0);
        if (listQueryIntentServicesAsUser == null) {
            return;
        }
        Iterator it = listQueryIntentServicesAsUser.iterator();
        while (it.hasNext()) {
            ComponentName componentName = ((ResolveInfo) it.next()).serviceInfo.getComponentName();
            if (predicate.test(componentName) && isTransportTrusted(componentName)) {
                registerTransport(componentName);
            }
        }
    }

    private boolean isTransportTrusted(ComponentName componentName) {
        if (!this.mTransportWhitelist.contains(componentName)) {
            Slog.w(TAG, "BackupTransport " + componentName.flattenToShortString() + " not whitelisted.");
            return false;
        }
        try {
            if ((this.mPackageManager.getPackageInfo(componentName.getPackageName(), 0).applicationInfo.privateFlags & 8) == 0) {
                Slog.w(TAG, "Transport package " + componentName.getPackageName() + " not privileged");
                return false;
            }
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Slog.w(TAG, "Package not found.", e);
            return false;
        }
    }

    private int registerTransport(ComponentName componentName) {
        checkCanUseTransport();
        if (!isTransportTrusted(componentName)) {
            return -2;
        }
        String strFlattenToShortString = componentName.flattenToShortString();
        Bundle bundle = new Bundle();
        bundle.putBoolean("android.app.backup.extra.TRANSPORT_REGISTRATION", true);
        TransportClient transportClient = this.mTransportClientManager.getTransportClient(componentName, bundle, "TransportManager.registerTransport()");
        int i = -1;
        try {
            IBackupTransport iBackupTransportConnectOrThrow = transportClient.connectOrThrow("TransportManager.registerTransport()");
            try {
                String strName = iBackupTransportConnectOrThrow.name();
                String strTransportDirName = iBackupTransportConnectOrThrow.transportDirName();
                registerTransport(componentName, iBackupTransportConnectOrThrow);
                Slog.d(TAG, "Transport " + strFlattenToShortString + " registered");
                this.mOnTransportRegisteredListener.onTransportRegistered(strName, strTransportDirName);
                i = 0;
            } catch (RemoteException e) {
                Slog.e(TAG, "Transport " + strFlattenToShortString + " died while registering");
            }
            this.mTransportClientManager.disposeOfTransportClient(transportClient, "TransportManager.registerTransport()");
            return i;
        } catch (TransportNotAvailableException e2) {
            Slog.e(TAG, "Couldn't connect to transport " + strFlattenToShortString + " for registration");
            this.mTransportClientManager.disposeOfTransportClient(transportClient, "TransportManager.registerTransport()");
            return -1;
        }
    }

    private void registerTransport(ComponentName componentName, IBackupTransport iBackupTransport) throws RemoteException {
        checkCanUseTransport();
        TransportDescription transportDescription = new TransportDescription(iBackupTransport.name(), iBackupTransport.transportDirName(), iBackupTransport.configurationIntent(), iBackupTransport.currentDestinationString(), iBackupTransport.dataManagementIntent(), iBackupTransport.dataManagementLabel());
        synchronized (this.mTransportLock) {
            this.mRegisteredTransportsDescriptionMap.put(componentName, transportDescription);
        }
    }

    private void checkCanUseTransport() {
        Preconditions.checkState(!Thread.holdsLock(this.mTransportLock), "Can't call transport with transport lock held");
    }

    public void dumpTransportClients(PrintWriter printWriter) {
        this.mTransportClientManager.dump(printWriter);
    }

    public void dumpTransportStats(PrintWriter printWriter) {
        this.mTransportStats.dump(printWriter);
    }

    private static Predicate<ComponentName> fromPackageFilter(final String str) {
        return new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return str.equals(((ComponentName) obj).getPackageName());
            }
        };
    }

    private static class TransportDescription {
        private Intent configurationIntent;
        private String currentDestinationString;
        private Intent dataManagementIntent;
        private String dataManagementLabel;
        private String name;
        private final String transportDirName;

        private TransportDescription(String str, String str2, Intent intent, String str3, Intent intent2, String str4) {
            this.name = str;
            this.transportDirName = str2;
            this.configurationIntent = intent;
            this.currentDestinationString = str3;
            this.dataManagementIntent = intent2;
            this.dataManagementLabel = str4;
        }
    }
}
