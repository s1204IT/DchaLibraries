package com.android.server.notification;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.UserInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Looper;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.proto.ProtoOutputStream;
import com.android.internal.util.XmlUtils;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.job.controllers.JobStatus;
import com.android.server.notification.NotificationManagerService;
import com.android.server.slice.SliceClientPermissions;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public abstract class ManagedServices {
    static final int APPROVAL_BY_COMPONENT = 1;
    static final int APPROVAL_BY_PACKAGE = 0;
    static final String ATT_APPROVED_LIST = "approved";
    static final String ATT_IS_PRIMARY = "primary";
    static final String ATT_USER_ID = "user";
    static final String ATT_VERSION = "version";
    static final int DB_VERSION = 1;
    protected static final String ENABLED_SERVICES_SEPARATOR = ":";
    private static final int ON_BINDING_DIED_REBIND_DELAY_MS = 10000;
    static final String TAG_MANAGED_SERVICES = "service_listing";
    protected final Context mContext;
    private int[] mLastSeenProfileIds;
    protected final Object mMutex;
    private final IPackageManager mPm;
    protected final UserManager mUm;
    private boolean mUseXml;
    private final UserProfiles mUserProfiles;
    protected final String TAG = getClass().getSimpleName();
    protected final boolean DEBUG = Log.isLoggable(this.TAG, 3);
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final ArrayList<ManagedServiceInfo> mServices = new ArrayList<>();
    private final ArrayList<String> mServicesBinding = new ArrayList<>();
    private final ArraySet<String> mServicesRebinding = new ArraySet<>();
    private ArraySet<ComponentName> mEnabledServicesForCurrentProfiles = new ArraySet<>();
    private ArraySet<String> mEnabledServicesPackageNames = new ArraySet<>();
    private ArraySet<ComponentName> mSnoozingForCurrentProfiles = new ArraySet<>();
    private ArrayMap<Integer, ArrayMap<Boolean, ArraySet<String>>> mApproved = new ArrayMap<>();
    private final Config mConfig = getConfig();
    protected int mApprovalLevel = 1;

    public static class Config {
        public String bindPermission;
        public String caption;
        public int clientLabel;
        public String secondarySettingName;
        public String secureSettingName;
        public String serviceInterface;
        public String settingsAction;
        public String xmlTag;
    }

    protected abstract IInterface asInterface(IBinder iBinder);

    protected abstract boolean checkType(IInterface iInterface);

    protected abstract Config getConfig();

    protected abstract void onServiceAdded(ManagedServiceInfo managedServiceInfo);

    public ManagedServices(Context context, Object obj, UserProfiles userProfiles, IPackageManager iPackageManager) {
        this.mContext = context;
        this.mMutex = obj;
        this.mUserProfiles = userProfiles;
        this.mPm = iPackageManager;
        this.mUm = (UserManager) this.mContext.getSystemService(ATT_USER_ID);
    }

    private String getCaption() {
        return this.mConfig.caption;
    }

    protected List<ManagedServiceInfo> getServices() {
        ArrayList arrayList;
        synchronized (this.mMutex) {
            arrayList = new ArrayList(this.mServices);
        }
        return arrayList;
    }

    protected void onServiceRemovedLocked(ManagedServiceInfo managedServiceInfo) {
    }

    private ManagedServiceInfo newServiceInfo(IInterface iInterface, ComponentName componentName, int i, boolean z, ServiceConnection serviceConnection, int i2) {
        return new ManagedServiceInfo(iInterface, componentName, i, z, serviceConnection, i2);
    }

    public void onBootPhaseAppsCanStart() {
    }

    public void dump(PrintWriter printWriter, NotificationManagerService.DumpFilter dumpFilter) {
        printWriter.println("    Allowed " + getCaption() + "s:");
        int size = this.mApproved.size();
        for (int i = 0; i < size; i++) {
            int iIntValue = this.mApproved.keyAt(i).intValue();
            ArrayMap<Boolean, ArraySet<String>> arrayMapValueAt = this.mApproved.valueAt(i);
            if (arrayMapValueAt != null) {
                int size2 = arrayMapValueAt.size();
                for (int i2 = 0; i2 < size2; i2++) {
                    boolean zBooleanValue = arrayMapValueAt.keyAt(i2).booleanValue();
                    ArraySet<String> arraySetValueAt = arrayMapValueAt.valueAt(i2);
                    if (arrayMapValueAt != null && arrayMapValueAt.size() > 0) {
                        printWriter.println("      " + String.join(ENABLED_SERVICES_SEPARATOR, arraySetValueAt) + " (user: " + iIntValue + " isPrimary: " + zBooleanValue + ")");
                    }
                }
            }
        }
        printWriter.println("    All " + getCaption() + "s (" + this.mEnabledServicesForCurrentProfiles.size() + ") enabled for current profiles:");
        for (ComponentName componentName : this.mEnabledServicesForCurrentProfiles) {
            if (dumpFilter == null || dumpFilter.matches(componentName)) {
                printWriter.println("      " + componentName);
            }
        }
        printWriter.println("    Live " + getCaption() + "s (" + this.mServices.size() + "):");
        for (ManagedServiceInfo managedServiceInfo : this.mServices) {
            if (dumpFilter == null || dumpFilter.matches(managedServiceInfo.component)) {
                StringBuilder sb = new StringBuilder();
                sb.append("      ");
                sb.append(managedServiceInfo.component);
                sb.append(" (user ");
                sb.append(managedServiceInfo.userid);
                sb.append("): ");
                sb.append(managedServiceInfo.service);
                sb.append(managedServiceInfo.isSystem ? " SYSTEM" : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                sb.append(managedServiceInfo.isGuest(this) ? " GUEST" : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                printWriter.println(sb.toString());
            }
        }
        printWriter.println("    Snoozed " + getCaption() + "s (" + this.mSnoozingForCurrentProfiles.size() + "):");
        Iterator<ComponentName> it = this.mSnoozingForCurrentProfiles.iterator();
        while (it.hasNext()) {
            printWriter.println("      " + it.next().flattenToShortString());
        }
    }

    public void dump(ProtoOutputStream protoOutputStream, NotificationManagerService.DumpFilter dumpFilter) {
        int i;
        protoOutputStream.write(1138166333441L, getCaption());
        int size = this.mApproved.size();
        int i2 = 0;
        while (i2 < size) {
            int iIntValue = this.mApproved.keyAt(i2).intValue();
            ArrayMap<Boolean, ArraySet<String>> arrayMapValueAt = this.mApproved.valueAt(i2);
            if (arrayMapValueAt != null) {
                int size2 = arrayMapValueAt.size();
                int i3 = 0;
                while (i3 < size2) {
                    boolean zBooleanValue = arrayMapValueAt.keyAt(i3).booleanValue();
                    ArraySet<String> arraySetValueAt = arrayMapValueAt.valueAt(i3);
                    if (arrayMapValueAt == null || arrayMapValueAt.size() <= 0) {
                        i = i2;
                    } else {
                        long jStart = protoOutputStream.start(2246267895810L);
                        Iterator<String> it = arraySetValueAt.iterator();
                        while (it.hasNext()) {
                            protoOutputStream.write(2237677961217L, it.next());
                            i2 = i2;
                        }
                        i = i2;
                        protoOutputStream.write(1120986464258L, iIntValue);
                        protoOutputStream.write(1133871366147L, zBooleanValue);
                        protoOutputStream.end(jStart);
                    }
                    i3++;
                    i2 = i;
                }
            }
            i2++;
        }
        for (ComponentName componentName : this.mEnabledServicesForCurrentProfiles) {
            if (dumpFilter == null || dumpFilter.matches(componentName)) {
                componentName.writeToProto(protoOutputStream, 2246267895811L);
            }
        }
        for (ManagedServiceInfo managedServiceInfo : this.mServices) {
            if (dumpFilter == null || dumpFilter.matches(managedServiceInfo.component)) {
                managedServiceInfo.writeToProto(protoOutputStream, 2246267895812L, this);
            }
        }
        Iterator<ComponentName> it2 = this.mSnoozingForCurrentProfiles.iterator();
        while (it2.hasNext()) {
            it2.next().writeToProto(protoOutputStream, 2246267895813L);
        }
    }

    protected void onSettingRestored(String str, String str2, int i, int i2) {
        if (!this.mUseXml) {
            Slog.d(this.TAG, "Restored managed service setting: " + str);
            if (this.mConfig.secureSettingName.equals(str) || (this.mConfig.secondarySettingName != null && this.mConfig.secondarySettingName.equals(str))) {
                if (i < 26) {
                    String approved = getApproved(i2, this.mConfig.secureSettingName.equals(str));
                    if (!TextUtils.isEmpty(approved)) {
                        if (!TextUtils.isEmpty(str2)) {
                            str2 = str2 + ENABLED_SERVICES_SEPARATOR + approved;
                        } else {
                            str2 = approved;
                        }
                    }
                }
                Settings.Secure.putStringForUser(this.mContext.getContentResolver(), str, str2, i2);
                loadAllowedComponentsFromSettings();
                rebindServices(false);
            }
        }
    }

    public void writeXml(XmlSerializer xmlSerializer, boolean z) throws IOException {
        xmlSerializer.startTag(null, getConfig().xmlTag);
        xmlSerializer.attribute(null, ATT_VERSION, String.valueOf(1));
        if (z) {
            trimApprovedListsAccordingToInstalledServices();
        }
        int size = this.mApproved.size();
        for (int i = 0; i < size; i++) {
            int iIntValue = this.mApproved.keyAt(i).intValue();
            ArrayMap<Boolean, ArraySet<String>> arrayMapValueAt = this.mApproved.valueAt(i);
            if (arrayMapValueAt != null) {
                int size2 = arrayMapValueAt.size();
                for (int i2 = 0; i2 < size2; i2++) {
                    boolean zBooleanValue = arrayMapValueAt.keyAt(i2).booleanValue();
                    ArraySet<String> arraySetValueAt = arrayMapValueAt.valueAt(i2);
                    if (arraySetValueAt != null) {
                        String strJoin = String.join(ENABLED_SERVICES_SEPARATOR, arraySetValueAt);
                        xmlSerializer.startTag(null, TAG_MANAGED_SERVICES);
                        xmlSerializer.attribute(null, ATT_APPROVED_LIST, strJoin);
                        xmlSerializer.attribute(null, ATT_USER_ID, Integer.toString(iIntValue));
                        xmlSerializer.attribute(null, ATT_IS_PRIMARY, Boolean.toString(zBooleanValue));
                        xmlSerializer.endTag(null, TAG_MANAGED_SERVICES);
                        if (!z && zBooleanValue) {
                            Settings.Secure.putStringForUser(this.mContext.getContentResolver(), getConfig().secureSettingName, strJoin, iIntValue);
                        }
                    }
                }
            }
        }
        xmlSerializer.endTag(null, getConfig().xmlTag);
    }

    protected void migrateToXml() {
        loadAllowedComponentsFromSettings();
    }

    public void readXml(XmlPullParser xmlPullParser, Predicate<String> predicate) throws XmlPullParserException, IOException {
        int intAttribute = XmlUtils.readIntAttribute(xmlPullParser, ATT_VERSION, 0);
        Iterator it = this.mUm.getUsers(true).iterator();
        while (it.hasNext()) {
            upgradeXml(intAttribute, ((UserInfo) it.next()).getUserHandle().getIdentifier());
        }
        while (true) {
            int next = xmlPullParser.next();
            if (next == 1) {
                break;
            }
            String name = xmlPullParser.getName();
            if (next == 3 && getConfig().xmlTag.equals(name)) {
                break;
            }
            if (next == 2 && TAG_MANAGED_SERVICES.equals(name)) {
                Slog.i(this.TAG, "Read " + this.mConfig.caption + " permissions from xml");
                String stringAttribute = XmlUtils.readStringAttribute(xmlPullParser, ATT_APPROVED_LIST);
                int intAttribute2 = XmlUtils.readIntAttribute(xmlPullParser, ATT_USER_ID, 0);
                boolean booleanAttribute = XmlUtils.readBooleanAttribute(xmlPullParser, ATT_IS_PRIMARY, true);
                if (predicate == null || predicate.test(getPackageName(stringAttribute))) {
                    if (this.mUm.getUserInfo(intAttribute2) != null) {
                        addApprovedList(stringAttribute, intAttribute2, booleanAttribute);
                    }
                    this.mUseXml = true;
                }
            }
        }
        rebindServices(false);
    }

    protected void upgradeXml(int i, int i2) {
    }

    private void loadAllowedComponentsFromSettings() {
        for (UserInfo userInfo : this.mUm.getUsers()) {
            ContentResolver contentResolver = this.mContext.getContentResolver();
            addApprovedList(Settings.Secure.getStringForUser(contentResolver, getConfig().secureSettingName, userInfo.id), userInfo.id, true);
            if (!TextUtils.isEmpty(getConfig().secondarySettingName)) {
                addApprovedList(Settings.Secure.getStringForUser(contentResolver, getConfig().secondarySettingName, userInfo.id), userInfo.id, false);
            }
        }
        Slog.d(this.TAG, "Done loading approved values from settings");
    }

    protected void addApprovedList(String str, int i, boolean z) {
        if (TextUtils.isEmpty(str)) {
            str = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        ArrayMap<Boolean, ArraySet<String>> arrayMap = this.mApproved.get(Integer.valueOf(i));
        if (arrayMap == null) {
            arrayMap = new ArrayMap<>();
            this.mApproved.put(Integer.valueOf(i), arrayMap);
        }
        ArraySet<String> arraySet = arrayMap.get(Boolean.valueOf(z));
        if (arraySet == null) {
            arraySet = new ArraySet<>();
            arrayMap.put(Boolean.valueOf(z), arraySet);
        }
        for (String str2 : str.split(ENABLED_SERVICES_SEPARATOR)) {
            String approvedValue = getApprovedValue(str2);
            if (approvedValue != null) {
                arraySet.add(approvedValue);
            }
        }
    }

    protected boolean isComponentEnabledForPackage(String str) {
        return this.mEnabledServicesPackageNames.contains(str);
    }

    protected void setPackageOrComponentEnabled(String str, int i, boolean z, boolean z2) {
        String str2 = this.TAG;
        StringBuilder sb = new StringBuilder();
        sb.append(z2 ? " Allowing " : "Disallowing ");
        sb.append(this.mConfig.caption);
        sb.append(" ");
        sb.append(str);
        Slog.i(str2, sb.toString());
        ArrayMap<Boolean, ArraySet<String>> arrayMap = this.mApproved.get(Integer.valueOf(i));
        if (arrayMap == null) {
            arrayMap = new ArrayMap<>();
            this.mApproved.put(Integer.valueOf(i), arrayMap);
        }
        ArraySet<String> arraySet = arrayMap.get(Boolean.valueOf(z));
        if (arraySet == null) {
            arraySet = new ArraySet<>();
            arrayMap.put(Boolean.valueOf(z), arraySet);
        }
        String approvedValue = getApprovedValue(str);
        if (approvedValue != null) {
            if (z2) {
                arraySet.add(approvedValue);
            } else {
                arraySet.remove(approvedValue);
            }
        }
        rebindServices(false);
    }

    private String getApprovedValue(String str) {
        if (this.mApprovalLevel == 1) {
            if (ComponentName.unflattenFromString(str) != null) {
                return str;
            }
            return null;
        }
        return getPackageName(str);
    }

    protected String getApproved(int i, boolean z) {
        return String.join(ENABLED_SERVICES_SEPARATOR, this.mApproved.getOrDefault(Integer.valueOf(i), new ArrayMap<>()).getOrDefault(Boolean.valueOf(z), new ArraySet<>()));
    }

    protected List<ComponentName> getAllowedComponents(int i) {
        ArrayList arrayList = new ArrayList();
        ArrayMap<Boolean, ArraySet<String>> orDefault = this.mApproved.getOrDefault(Integer.valueOf(i), new ArrayMap<>());
        for (int i2 = 0; i2 < orDefault.size(); i2++) {
            ArraySet<String> arraySetValueAt = orDefault.valueAt(i2);
            for (int i3 = 0; i3 < arraySetValueAt.size(); i3++) {
                ComponentName componentNameUnflattenFromString = ComponentName.unflattenFromString(arraySetValueAt.valueAt(i3));
                if (componentNameUnflattenFromString != null) {
                    arrayList.add(componentNameUnflattenFromString);
                }
            }
        }
        return arrayList;
    }

    protected List<String> getAllowedPackages(int i) {
        ArrayList arrayList = new ArrayList();
        ArrayMap<Boolean, ArraySet<String>> orDefault = this.mApproved.getOrDefault(Integer.valueOf(i), new ArrayMap<>());
        for (int i2 = 0; i2 < orDefault.size(); i2++) {
            ArraySet<String> arraySetValueAt = orDefault.valueAt(i2);
            for (int i3 = 0; i3 < arraySetValueAt.size(); i3++) {
                String packageName = getPackageName(arraySetValueAt.valueAt(i3));
                if (!TextUtils.isEmpty(packageName)) {
                    arrayList.add(packageName);
                }
            }
        }
        return arrayList;
    }

    protected boolean isPackageOrComponentAllowed(String str, int i) {
        ArrayMap<Boolean, ArraySet<String>> orDefault = this.mApproved.getOrDefault(Integer.valueOf(i), new ArrayMap<>());
        for (int i2 = 0; i2 < orDefault.size(); i2++) {
            if (orDefault.valueAt(i2).contains(str)) {
                return true;
            }
        }
        return false;
    }

    public void onPackagesChanged(boolean z, String[] strArr, int[] iArr) {
        boolean zRemoveUninstalledItemsFromApprovedLists;
        if (this.DEBUG) {
            String str = this.TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("onPackagesChanged removingPackage=");
            sb.append(z);
            sb.append(" pkgList=");
            sb.append(strArr == null ? null : Arrays.asList(strArr));
            sb.append(" mEnabledServicesPackageNames=");
            sb.append(this.mEnabledServicesPackageNames);
            Slog.d(str, sb.toString());
        }
        if (strArr != null && strArr.length > 0) {
            if (z) {
                int iMin = Math.min(strArr.length, iArr.length);
                zRemoveUninstalledItemsFromApprovedLists = false;
                for (int i = 0; i < iMin; i++) {
                    zRemoveUninstalledItemsFromApprovedLists = removeUninstalledItemsFromApprovedLists(UserHandle.getUserId(iArr[i]), strArr[i]);
                }
            } else {
                zRemoveUninstalledItemsFromApprovedLists = false;
            }
            for (String str2 : strArr) {
                if (this.mEnabledServicesPackageNames.contains(str2)) {
                    zRemoveUninstalledItemsFromApprovedLists = true;
                }
            }
            if (zRemoveUninstalledItemsFromApprovedLists) {
                rebindServices(false);
            }
        }
    }

    public void onUserRemoved(int i) {
        Slog.i(this.TAG, "Removing approved services for removed user " + i);
        this.mApproved.remove(Integer.valueOf(i));
        rebindServices(true);
    }

    public void onUserSwitched(int i) {
        if (this.DEBUG) {
            Slog.d(this.TAG, "onUserSwitched u=" + i);
        }
        if (Arrays.equals(this.mLastSeenProfileIds, this.mUserProfiles.getCurrentProfileIds())) {
            if (this.DEBUG) {
                Slog.d(this.TAG, "Current profile IDs didn't change, skipping rebindServices().");
                return;
            }
            return;
        }
        rebindServices(true);
    }

    public void onUserUnlocked(int i) {
        if (this.DEBUG) {
            Slog.d(this.TAG, "onUserUnlocked u=" + i);
        }
        rebindServices(false);
    }

    private ManagedServiceInfo getServiceFromTokenLocked(IInterface iInterface) {
        if (iInterface == null) {
            return null;
        }
        IBinder iBinderAsBinder = iInterface.asBinder();
        int size = this.mServices.size();
        for (int i = 0; i < size; i++) {
            ManagedServiceInfo managedServiceInfo = this.mServices.get(i);
            if (managedServiceInfo.service.asBinder() == iBinderAsBinder) {
                return managedServiceInfo;
            }
        }
        return null;
    }

    protected boolean isServiceTokenValidLocked(IInterface iInterface) {
        if (iInterface == null || getServiceFromTokenLocked(iInterface) == null) {
            return false;
        }
        return true;
    }

    protected ManagedServiceInfo checkServiceTokenLocked(IInterface iInterface) {
        checkNotNull(iInterface);
        ManagedServiceInfo serviceFromTokenLocked = getServiceFromTokenLocked(iInterface);
        if (serviceFromTokenLocked != null) {
            return serviceFromTokenLocked;
        }
        throw new SecurityException("Disallowed call from unknown " + getCaption() + ": " + iInterface + " " + iInterface.getClass());
    }

    public void unregisterService(IInterface iInterface, int i) {
        checkNotNull(iInterface);
        unregisterServiceImpl(iInterface, i);
    }

    public void registerService(IInterface iInterface, ComponentName componentName, int i) {
        checkNotNull(iInterface);
        ManagedServiceInfo managedServiceInfoRegisterServiceImpl = registerServiceImpl(iInterface, componentName, i);
        if (managedServiceInfoRegisterServiceImpl != null) {
            onServiceAdded(managedServiceInfoRegisterServiceImpl);
        }
    }

    protected void registerGuestService(ManagedServiceInfo managedServiceInfo) {
        checkNotNull(managedServiceInfo.service);
        if (!checkType(managedServiceInfo.service)) {
            throw new IllegalArgumentException();
        }
        if (registerServiceImpl(managedServiceInfo) != null) {
            onServiceAdded(managedServiceInfo);
        }
    }

    protected void setComponentState(ComponentName componentName, boolean z) {
        if ((!this.mSnoozingForCurrentProfiles.contains(componentName)) == z) {
            return;
        }
        if (z) {
            this.mSnoozingForCurrentProfiles.remove(componentName);
        } else {
            this.mSnoozingForCurrentProfiles.add(componentName);
        }
        String str = this.TAG;
        StringBuilder sb = new StringBuilder();
        sb.append(z ? "Enabling " : "Disabling ");
        sb.append("component ");
        sb.append(componentName.flattenToShortString());
        Slog.d(str, sb.toString());
        synchronized (this.mMutex) {
            for (int i : this.mUserProfiles.getCurrentProfileIds()) {
                if (z) {
                    registerServiceLocked(componentName, i);
                } else {
                    unregisterServiceLocked(componentName, i);
                }
            }
        }
    }

    private ArraySet<ComponentName> loadComponentNamesFromValues(ArraySet<String> arraySet, int i) {
        if (arraySet == null || arraySet.size() == 0) {
            return new ArraySet<>();
        }
        ArraySet<ComponentName> arraySet2 = new ArraySet<>(arraySet.size());
        for (int i2 = 0; i2 < arraySet.size(); i2++) {
            String strValueAt = arraySet.valueAt(i2);
            if (!TextUtils.isEmpty(strValueAt)) {
                ComponentName componentNameUnflattenFromString = ComponentName.unflattenFromString(strValueAt);
                if (componentNameUnflattenFromString != null) {
                    arraySet2.add(componentNameUnflattenFromString);
                } else {
                    arraySet2.addAll(queryPackageForServices(strValueAt, i));
                }
            }
        }
        return arraySet2;
    }

    protected Set<ComponentName> queryPackageForServices(String str, int i) {
        return queryPackageForServices(str, 0, i);
    }

    protected Set<ComponentName> queryPackageForServices(String str, int i, int i2) {
        ArraySet arraySet = new ArraySet();
        PackageManager packageManager = this.mContext.getPackageManager();
        Intent intent = new Intent(this.mConfig.serviceInterface);
        if (!TextUtils.isEmpty(str)) {
            intent.setPackage(str);
        }
        List listQueryIntentServicesAsUser = packageManager.queryIntentServicesAsUser(intent, 132 | i, i2);
        if (this.DEBUG) {
            Slog.v(this.TAG, this.mConfig.serviceInterface + " services: " + listQueryIntentServicesAsUser);
        }
        if (listQueryIntentServicesAsUser != null) {
            int size = listQueryIntentServicesAsUser.size();
            for (int i3 = 0; i3 < size; i3++) {
                ServiceInfo serviceInfo = ((ResolveInfo) listQueryIntentServicesAsUser.get(i3)).serviceInfo;
                ComponentName componentName = new ComponentName(serviceInfo.packageName, serviceInfo.name);
                if (!this.mConfig.bindPermission.equals(serviceInfo.permission)) {
                    Slog.w(this.TAG, "Skipping " + getCaption() + " service " + serviceInfo.packageName + SliceClientPermissions.SliceAuthority.DELIMITER + serviceInfo.name + ": it does not require the permission " + this.mConfig.bindPermission);
                } else {
                    arraySet.add(componentName);
                }
            }
        }
        return arraySet;
    }

    private void trimApprovedListsAccordingToInstalledServices() {
        int size = this.mApproved.size();
        for (int i = 0; i < size; i++) {
            int iIntValue = this.mApproved.keyAt(i).intValue();
            ArrayMap<Boolean, ArraySet<String>> arrayMapValueAt = this.mApproved.valueAt(i);
            int size2 = arrayMapValueAt.size();
            for (int i2 = 0; i2 < size2; i2++) {
                ArraySet<String> arraySetValueAt = arrayMapValueAt.valueAt(i2);
                for (int size3 = arraySetValueAt.size() - 1; size3 >= 0; size3--) {
                    String strValueAt = arraySetValueAt.valueAt(size3);
                    if (!isValidEntry(strValueAt, iIntValue)) {
                        arraySetValueAt.removeAt(size3);
                        Slog.v(this.TAG, "Removing " + strValueAt + " from approved list; no matching services found");
                    } else if (this.DEBUG) {
                        Slog.v(this.TAG, "Keeping " + strValueAt + " on approved list; matching services found");
                    }
                }
            }
        }
    }

    private boolean removeUninstalledItemsFromApprovedLists(int i, String str) {
        ArrayMap<Boolean, ArraySet<String>> arrayMap = this.mApproved.get(Integer.valueOf(i));
        if (arrayMap != null) {
            int size = arrayMap.size();
            for (int i2 = 0; i2 < size; i2++) {
                ArraySet<String> arraySetValueAt = arrayMap.valueAt(i2);
                for (int size2 = arraySetValueAt.size() - 1; size2 >= 0; size2--) {
                    String strValueAt = arraySetValueAt.valueAt(size2);
                    if (TextUtils.equals(str, getPackageName(strValueAt))) {
                        arraySetValueAt.removeAt(size2);
                        if (this.DEBUG) {
                            Slog.v(this.TAG, "Removing " + strValueAt + " from approved list; uninstalled");
                        }
                    }
                }
            }
        }
        return false;
    }

    protected String getPackageName(String str) {
        ComponentName componentNameUnflattenFromString = ComponentName.unflattenFromString(str);
        if (componentNameUnflattenFromString != null) {
            return componentNameUnflattenFromString.getPackageName();
        }
        return str;
    }

    protected boolean isValidEntry(String str, int i) {
        return hasMatchingServices(str, i);
    }

    private boolean hasMatchingServices(String str, int i) {
        return !TextUtils.isEmpty(str) && queryPackageForServices(getPackageName(str), i).size() > 0;
    }

    protected void rebindServices(boolean z) {
        if (this.DEBUG) {
            Slog.d(this.TAG, "rebindServices");
        }
        int[] currentProfileIds = this.mUserProfiles.getCurrentProfileIds();
        int length = currentProfileIds.length;
        SparseArray sparseArray = new SparseArray();
        for (int i = 0; i < length; i++) {
            int i2 = currentProfileIds[i];
            ArrayMap<Boolean, ArraySet<String>> arrayMap = this.mApproved.get(Integer.valueOf(currentProfileIds[i]));
            if (arrayMap != null) {
                int size = arrayMap.size();
                for (int i3 = 0; i3 < size; i3++) {
                    ArraySet arraySet = (ArraySet) sparseArray.get(i2);
                    if (arraySet == null) {
                        arraySet = new ArraySet();
                        sparseArray.put(i2, arraySet);
                    }
                    arraySet.addAll((ArraySet) loadComponentNamesFromValues(arrayMap.valueAt(i3), i2));
                }
            }
        }
        ArrayList<ManagedServiceInfo> arrayList = new ArrayList();
        SparseArray sparseArray2 = new SparseArray();
        synchronized (this.mMutex) {
            for (ManagedServiceInfo managedServiceInfo : this.mServices) {
                if (!managedServiceInfo.isSystem && !managedServiceInfo.isGuest(this)) {
                    arrayList.add(managedServiceInfo);
                }
            }
            this.mEnabledServicesForCurrentProfiles.clear();
            this.mEnabledServicesPackageNames.clear();
            for (int i4 = 0; i4 < length; i4++) {
                ArraySet<? extends ComponentName> arraySet2 = (ArraySet) sparseArray.get(currentProfileIds[i4]);
                if (arraySet2 == null) {
                    sparseArray2.put(currentProfileIds[i4], new ArraySet());
                } else {
                    HashSet hashSet = new HashSet(arraySet2);
                    hashSet.removeAll(this.mSnoozingForCurrentProfiles);
                    sparseArray2.put(currentProfileIds[i4], hashSet);
                    this.mEnabledServicesForCurrentProfiles.addAll(arraySet2);
                    for (int i5 = 0; i5 < arraySet2.size(); i5++) {
                        this.mEnabledServicesPackageNames.add(arraySet2.valueAt(i5).getPackageName());
                    }
                }
            }
        }
        for (ManagedServiceInfo managedServiceInfo2 : arrayList) {
            ComponentName componentName = managedServiceInfo2.component;
            int i6 = managedServiceInfo2.userid;
            Set set = (Set) sparseArray2.get(managedServiceInfo2.userid);
            if (set != null) {
                if (set.contains(componentName) && !z) {
                    set.remove(componentName);
                } else {
                    Slog.v(this.TAG, "disabling " + getCaption() + " for user " + i6 + ": " + componentName);
                    unregisterService(componentName, i6);
                }
            }
        }
        for (int i7 = 0; i7 < length; i7++) {
            for (ComponentName componentName2 : (Set) sparseArray2.get(currentProfileIds[i7])) {
                try {
                    ServiceInfo serviceInfo = this.mPm.getServiceInfo(componentName2, 786432, currentProfileIds[i7]);
                    if (serviceInfo == null) {
                        Slog.w(this.TAG, "Not binding " + getCaption() + " service " + componentName2 + ": service not found");
                    } else if (!this.mConfig.bindPermission.equals(serviceInfo.permission)) {
                        Slog.w(this.TAG, "Not binding " + getCaption() + " service " + componentName2 + ": it does not require the permission " + this.mConfig.bindPermission);
                    } else {
                        Slog.v(this.TAG, "enabling " + getCaption() + " for " + currentProfileIds[i7] + ": " + componentName2);
                        registerService(componentName2, currentProfileIds[i7]);
                    }
                } catch (RemoteException e) {
                    e.rethrowFromSystemServer();
                }
            }
        }
        this.mLastSeenProfileIds = currentProfileIds;
    }

    private void registerService(ComponentName componentName, int i) {
        synchronized (this.mMutex) {
            registerServiceLocked(componentName, i);
        }
    }

    public void registerSystemService(ComponentName componentName, int i) {
        synchronized (this.mMutex) {
            registerServiceLocked(componentName, i, true);
        }
    }

    private void registerServiceLocked(ComponentName componentName, int i) {
        registerServiceLocked(componentName, i, false);
    }

    private void registerServiceLocked(ComponentName componentName, final int i, final boolean z) {
        ApplicationInfo applicationInfo;
        if (this.DEBUG) {
            Slog.v(this.TAG, "registerService: " + componentName + " u=" + i);
        }
        final String str = componentName.toString() + SliceClientPermissions.SliceAuthority.DELIMITER + i;
        if (this.mServicesBinding.contains(str)) {
            Slog.v(this.TAG, "Not registering " + componentName + " as bind is already in progress");
            return;
        }
        this.mServicesBinding.add(str);
        for (int size = this.mServices.size() - 1; size >= 0; size--) {
            ManagedServiceInfo managedServiceInfo = this.mServices.get(size);
            if (componentName.equals(managedServiceInfo.component) && managedServiceInfo.userid == i) {
                Slog.v(this.TAG, "    disconnecting old " + getCaption() + ": " + managedServiceInfo.service);
                removeServiceLocked(size);
                if (managedServiceInfo.connection != null) {
                    try {
                        this.mContext.unbindService(managedServiceInfo.connection);
                    } catch (IllegalArgumentException e) {
                        Slog.e(this.TAG, "failed to unbind " + componentName, e);
                    }
                }
            }
        }
        Intent intent = new Intent(this.mConfig.serviceInterface);
        intent.setComponent(componentName);
        intent.putExtra("android.intent.extra.client_label", this.mConfig.clientLabel);
        intent.putExtra("android.intent.extra.client_intent", PendingIntent.getActivity(this.mContext, 0, new Intent(this.mConfig.settingsAction), 0));
        try {
            applicationInfo = this.mContext.getPackageManager().getApplicationInfo(componentName.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e2) {
            applicationInfo = null;
        }
        final int i2 = applicationInfo != null ? applicationInfo.targetSdkVersion : 1;
        try {
            Slog.v(this.TAG, "binding: " + intent);
            if (!this.mContext.bindServiceAsUser(intent, new ServiceConnection() {
                IInterface mService;

                @Override
                public void onServiceConnected(ComponentName componentName2, IBinder iBinder) {
                    ManagedServiceInfo managedServiceInfoNewServiceInfo;
                    boolean zAdd;
                    synchronized (ManagedServices.this.mMutex) {
                        ManagedServices.this.mServicesRebinding.remove(str);
                        ManagedServices.this.mServicesBinding.remove(str);
                        try {
                            this.mService = ManagedServices.this.asInterface(iBinder);
                            managedServiceInfoNewServiceInfo = ManagedServices.this.newServiceInfo(this.mService, componentName2, i, z, this, i2);
                            try {
                                iBinder.linkToDeath(managedServiceInfoNewServiceInfo, 0);
                                zAdd = ManagedServices.this.mServices.add(managedServiceInfoNewServiceInfo);
                            } catch (RemoteException e3) {
                                zAdd = false;
                            }
                        } catch (RemoteException e4) {
                            managedServiceInfoNewServiceInfo = null;
                        }
                    }
                    if (zAdd) {
                        ManagedServices.this.onServiceAdded(managedServiceInfoNewServiceInfo);
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName componentName2) {
                    ManagedServices.this.mServicesBinding.remove(str);
                    Slog.v(ManagedServices.this.TAG, ManagedServices.this.getCaption() + " connection lost: " + componentName2);
                }

                @Override
                public void onBindingDied(final ComponentName componentName2) {
                    Slog.w(ManagedServices.this.TAG, ManagedServices.this.getCaption() + " binding died: " + componentName2);
                    synchronized (ManagedServices.this.mMutex) {
                        ManagedServices.this.mServicesBinding.remove(str);
                        try {
                            ManagedServices.this.mContext.unbindService(this);
                        } catch (IllegalArgumentException e3) {
                            Slog.e(ManagedServices.this.TAG, "failed to unbind " + componentName2, e3);
                        }
                        if (!ManagedServices.this.mServicesRebinding.contains(str)) {
                            ManagedServices.this.mServicesRebinding.add(str);
                            ManagedServices.this.mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    ManagedServices.this.registerService(componentName2, i);
                                }
                            }, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
                        } else {
                            Slog.v(ManagedServices.this.TAG, ManagedServices.this.getCaption() + " not rebinding as a previous rebind attempt was made: " + componentName2);
                        }
                    }
                }
            }, 83886081, new UserHandle(i))) {
                this.mServicesBinding.remove(str);
                Slog.w(this.TAG, "Unable to bind " + getCaption() + " service: " + intent);
            }
        } catch (SecurityException e3) {
            this.mServicesBinding.remove(str);
            Slog.e(this.TAG, "Unable to bind " + getCaption() + " service: " + intent, e3);
        }
    }

    private void unregisterService(ComponentName componentName, int i) {
        synchronized (this.mMutex) {
            unregisterServiceLocked(componentName, i);
        }
    }

    private void unregisterServiceLocked(ComponentName componentName, int i) {
        for (int size = this.mServices.size() - 1; size >= 0; size--) {
            ManagedServiceInfo managedServiceInfo = this.mServices.get(size);
            if (componentName.equals(managedServiceInfo.component) && managedServiceInfo.userid == i) {
                removeServiceLocked(size);
                if (managedServiceInfo.connection != null) {
                    try {
                        this.mContext.unbindService(managedServiceInfo.connection);
                    } catch (IllegalArgumentException e) {
                        Slog.e(this.TAG, getCaption() + " " + componentName + " could not be unbound: " + e);
                    }
                }
            }
        }
    }

    private ManagedServiceInfo removeServiceImpl(IInterface iInterface, int i) {
        if (this.DEBUG) {
            Slog.d(this.TAG, "removeServiceImpl service=" + iInterface + " u=" + i);
        }
        ManagedServiceInfo managedServiceInfoRemoveServiceLocked = null;
        synchronized (this.mMutex) {
            for (int size = this.mServices.size() - 1; size >= 0; size--) {
                ManagedServiceInfo managedServiceInfo = this.mServices.get(size);
                if (managedServiceInfo.service.asBinder() == iInterface.asBinder() && managedServiceInfo.userid == i) {
                    Slog.d(this.TAG, "Removing active service " + managedServiceInfo.component);
                    managedServiceInfoRemoveServiceLocked = removeServiceLocked(size);
                }
            }
        }
        return managedServiceInfoRemoveServiceLocked;
    }

    private ManagedServiceInfo removeServiceLocked(int i) {
        ManagedServiceInfo managedServiceInfoRemove = this.mServices.remove(i);
        onServiceRemovedLocked(managedServiceInfoRemove);
        return managedServiceInfoRemove;
    }

    private void checkNotNull(IInterface iInterface) {
        if (iInterface == null) {
            throw new IllegalArgumentException(getCaption() + " must not be null");
        }
    }

    private ManagedServiceInfo registerServiceImpl(IInterface iInterface, ComponentName componentName, int i) {
        return registerServiceImpl(newServiceInfo(iInterface, componentName, i, true, null, 21));
    }

    private ManagedServiceInfo registerServiceImpl(ManagedServiceInfo managedServiceInfo) {
        synchronized (this.mMutex) {
            try {
                try {
                    managedServiceInfo.service.asBinder().linkToDeath(managedServiceInfo, 0);
                    this.mServices.add(managedServiceInfo);
                } catch (RemoteException e) {
                    return null;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return managedServiceInfo;
    }

    private void unregisterServiceImpl(IInterface iInterface, int i) {
        ManagedServiceInfo managedServiceInfoRemoveServiceImpl = removeServiceImpl(iInterface, i);
        if (managedServiceInfoRemoveServiceImpl != null && managedServiceInfoRemoveServiceImpl.connection != null && !managedServiceInfoRemoveServiceImpl.isGuest(this)) {
            this.mContext.unbindService(managedServiceInfoRemoveServiceImpl.connection);
        }
    }

    public class ManagedServiceInfo implements IBinder.DeathRecipient {
        public ComponentName component;
        public ServiceConnection connection;
        public boolean isSystem;
        public IInterface service;
        public int targetSdkVersion;
        public int userid;

        public ManagedServiceInfo(IInterface iInterface, ComponentName componentName, int i, boolean z, ServiceConnection serviceConnection, int i2) {
            this.service = iInterface;
            this.component = componentName;
            this.userid = i;
            this.isSystem = z;
            this.connection = serviceConnection;
            this.targetSdkVersion = i2;
        }

        public boolean isGuest(ManagedServices managedServices) {
            return ManagedServices.this != managedServices;
        }

        public ManagedServices getOwner() {
            return ManagedServices.this;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("ManagedServiceInfo[");
            sb.append("component=");
            sb.append(this.component);
            sb.append(",userid=");
            sb.append(this.userid);
            sb.append(",isSystem=");
            sb.append(this.isSystem);
            sb.append(",targetSdkVersion=");
            sb.append(this.targetSdkVersion);
            sb.append(",connection=");
            sb.append(this.connection == null ? null : "<connection>");
            sb.append(",service=");
            sb.append(this.service);
            sb.append(']');
            return sb.toString();
        }

        public void writeToProto(ProtoOutputStream protoOutputStream, long j, ManagedServices managedServices) {
            long jStart = protoOutputStream.start(j);
            this.component.writeToProto(protoOutputStream, 1146756268033L);
            protoOutputStream.write(1120986464258L, this.userid);
            protoOutputStream.write(1138166333443L, this.service.getClass().getName());
            protoOutputStream.write(1133871366148L, this.isSystem);
            protoOutputStream.write(1133871366149L, isGuest(managedServices));
            protoOutputStream.end(jStart);
        }

        public boolean enabledAndUserMatches(int i) {
            if (!isEnabledForCurrentProfiles()) {
                return false;
            }
            if (this.userid == -1 || this.isSystem || i == -1 || i == this.userid) {
                return true;
            }
            return supportsProfiles() && ManagedServices.this.mUserProfiles.isCurrentProfile(i) && isPermittedForProfile(i);
        }

        public boolean supportsProfiles() {
            return this.targetSdkVersion >= 21;
        }

        @Override
        public void binderDied() {
            if (ManagedServices.this.DEBUG) {
                Slog.d(ManagedServices.this.TAG, "binderDied");
            }
            ManagedServices.this.removeServiceImpl(this.service, this.userid);
        }

        public boolean isEnabledForCurrentProfiles() {
            if (this.isSystem) {
                return true;
            }
            if (this.connection == null) {
                return false;
            }
            return ManagedServices.this.mEnabledServicesForCurrentProfiles.contains(this.component);
        }

        public boolean isPermittedForProfile(int i) {
            if (!ManagedServices.this.mUserProfiles.isManagedProfile(i)) {
                return true;
            }
            DevicePolicyManager devicePolicyManager = (DevicePolicyManager) ManagedServices.this.mContext.getSystemService("device_policy");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return devicePolicyManager.isNotificationListenerServicePermitted(this.component.getPackageName(), i);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    public boolean isComponentEnabledForCurrentProfiles(ComponentName componentName) {
        return this.mEnabledServicesForCurrentProfiles.contains(componentName);
    }

    public static class UserProfiles {
        private final SparseArray<UserInfo> mCurrentProfiles = new SparseArray<>();

        public void updateCache(Context context) {
            UserManager userManager = (UserManager) context.getSystemService(ManagedServices.ATT_USER_ID);
            if (userManager != null) {
                List<UserInfo> profiles = userManager.getProfiles(ActivityManager.getCurrentUser());
                synchronized (this.mCurrentProfiles) {
                    this.mCurrentProfiles.clear();
                    for (UserInfo userInfo : profiles) {
                        this.mCurrentProfiles.put(userInfo.id, userInfo);
                    }
                }
            }
        }

        public int[] getCurrentProfileIds() {
            int[] iArr;
            synchronized (this.mCurrentProfiles) {
                iArr = new int[this.mCurrentProfiles.size()];
                int size = this.mCurrentProfiles.size();
                for (int i = 0; i < size; i++) {
                    iArr[i] = this.mCurrentProfiles.keyAt(i);
                }
            }
            return iArr;
        }

        public boolean isCurrentProfile(int i) {
            boolean z;
            synchronized (this.mCurrentProfiles) {
                z = this.mCurrentProfiles.get(i) != null;
            }
            return z;
        }

        public boolean isManagedProfile(int i) {
            boolean z;
            synchronized (this.mCurrentProfiles) {
                UserInfo userInfo = this.mCurrentProfiles.get(i);
                z = userInfo != null && userInfo.isManagedProfile();
            }
            return z;
        }
    }
}
