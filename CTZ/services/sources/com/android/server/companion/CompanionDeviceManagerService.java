package com.android.server.companion;

import android.app.PendingIntent;
import android.companion.AssociationRequest;
import android.companion.ICompanionDeviceDiscoveryService;
import android.companion.ICompanionDeviceDiscoveryServiceCallback;
import android.companion.ICompanionDeviceManager;
import android.companion.IFindDeviceCallback;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.NetworkPolicyManager;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.IDeviceIdleController;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.UserHandle;
import android.provider.Settings;
import android.provider.SettingsStringUtil;
import android.text.BidiFormatter;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.ExceptionUtils;
import android.util.Log;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.app.IAppOpsService;
import com.android.internal.content.PackageMonitor;
import com.android.internal.notification.NotificationAccessConfirmationActivityContract;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.CollectionUtils;
import com.android.internal.util.FunctionalUtils;
import com.android.internal.util.Preconditions;
import com.android.internal.util.function.TriFunction;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.FgThread;
import com.android.server.SystemService;
import com.android.server.companion.CompanionDeviceManagerService;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class CompanionDeviceManagerService extends SystemService implements IBinder.DeathRecipient {
    private static final boolean DEBUG = false;
    private static final String LOG_TAG = "CompanionDeviceManagerService";
    private static final ComponentName SERVICE_TO_BIND_TO = ComponentName.createRelative("com.android.companiondevicemanager", ".DeviceDiscoveryService");
    private static final String XML_ATTR_DEVICE = "device";
    private static final String XML_ATTR_PACKAGE = "package";
    private static final String XML_FILE_NAME = "companion_device_manager_associations.xml";
    private static final String XML_TAG_ASSOCIATION = "association";
    private static final String XML_TAG_ASSOCIATIONS = "associations";
    private IAppOpsService mAppOpsManager;
    private String mCallingPackage;
    private IFindDeviceCallback mFindDeviceCallback;
    private IDeviceIdleController mIdleController;
    private final CompanionDeviceManagerImpl mImpl;
    private final Object mLock;
    private AssociationRequest mRequest;
    private ServiceConnection mServiceConnection;
    private final ConcurrentMap<Integer, AtomicFile> mUidToStorage;

    public CompanionDeviceManagerService(Context context) {
        super(context);
        this.mUidToStorage = new ConcurrentHashMap();
        this.mLock = new Object();
        this.mImpl = new CompanionDeviceManagerImpl();
        this.mIdleController = IDeviceIdleController.Stub.asInterface(ServiceManager.getService("deviceidle"));
        this.mAppOpsManager = IAppOpsService.Stub.asInterface(ServiceManager.getService("appops"));
        registerPackageMonitor();
    }

    class AnonymousClass1 extends PackageMonitor {
        AnonymousClass1() {
        }

        public void onPackageRemoved(final String str, int i) {
            CompanionDeviceManagerService.this.updateAssociations(new Function() {
                @Override
                public final Object apply(Object obj) {
                    return CollectionUtils.filter((Set) obj, new Predicate() {
                        @Override
                        public final boolean test(Object obj2) {
                            return CompanionDeviceManagerService.AnonymousClass1.lambda$onPackageRemoved$0(str, (CompanionDeviceManagerService.Association) obj2);
                        }
                    });
                }
            }, getChangingUserId());
        }

        static boolean lambda$onPackageRemoved$0(String str, Association association) {
            return !Objects.equals(association.companionAppPackage, str);
        }

        public void onPackageModified(String str) {
            int changingUserId = getChangingUserId();
            if (!ArrayUtils.isEmpty(CompanionDeviceManagerService.this.readAllAssociations(changingUserId, str))) {
                CompanionDeviceManagerService.this.updateSpecialAccessPermissionForAssociatedPackage(str, changingUserId);
            }
        }
    }

    private void registerPackageMonitor() {
        new AnonymousClass1().register(getContext(), FgThread.get().getLooper(), UserHandle.ALL, true);
    }

    @Override
    public void onStart() {
        publishBinderService("companiondevice", this.mImpl);
    }

    @Override
    public void binderDied() {
        Handler.getMain().post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.cleanup();
            }
        });
    }

    private void cleanup() {
        synchronized (this.mLock) {
            this.mServiceConnection = unbind(this.mServiceConnection);
            this.mFindDeviceCallback = unlinkToDeath(this.mFindDeviceCallback, this, 0);
            this.mRequest = null;
            this.mCallingPackage = null;
        }
    }

    private static <T extends IInterface> T unlinkToDeath(T t, IBinder.DeathRecipient deathRecipient, int i) {
        if (t != null) {
            t.asBinder().unlinkToDeath(deathRecipient, i);
            return null;
        }
        return null;
    }

    private ServiceConnection unbind(ServiceConnection serviceConnection) {
        if (serviceConnection != null) {
            getContext().unbindService(serviceConnection);
            return null;
        }
        return null;
    }

    class CompanionDeviceManagerImpl extends ICompanionDeviceManager.Stub {
        CompanionDeviceManagerImpl() {
        }

        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            try {
                return super.onTransact(i, parcel, parcel2, i2);
            } catch (Throwable th) {
                Slog.e(CompanionDeviceManagerService.LOG_TAG, "Error during IPC", th);
                throw ExceptionUtils.propagate(th, RemoteException.class);
            }
        }

        public void associate(AssociationRequest associationRequest, IFindDeviceCallback iFindDeviceCallback, String str) throws RemoteException {
            Preconditions.checkNotNull(associationRequest, "Request cannot be null");
            Preconditions.checkNotNull(iFindDeviceCallback, "Callback cannot be null");
            checkCallerIsSystemOr(str);
            int callingUserId = CompanionDeviceManagerService.getCallingUserId();
            checkUsesFeature(str, callingUserId);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                CompanionDeviceManagerService.this.getContext().bindServiceAsUser(new Intent().setComponent(CompanionDeviceManagerService.SERVICE_TO_BIND_TO), CompanionDeviceManagerService.this.createServiceConnection(associationRequest, iFindDeviceCallback, str), 1, UserHandle.of(callingUserId));
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void stopScan(AssociationRequest associationRequest, IFindDeviceCallback iFindDeviceCallback, String str) {
            if (Objects.equals(associationRequest, CompanionDeviceManagerService.this.mRequest) && Objects.equals(iFindDeviceCallback, CompanionDeviceManagerService.this.mFindDeviceCallback) && Objects.equals(str, CompanionDeviceManagerService.this.mCallingPackage)) {
                CompanionDeviceManagerService.this.cleanup();
            }
        }

        public List<String> getAssociations(String str, int i) throws RemoteException {
            checkCallerIsSystemOr(str, i);
            checkUsesFeature(str, CompanionDeviceManagerService.getCallingUserId());
            return new ArrayList(CollectionUtils.map(CompanionDeviceManagerService.this.readAllAssociations(i, str), new Function() {
                @Override
                public final Object apply(Object obj) {
                    return ((CompanionDeviceManagerService.Association) obj).deviceAddress;
                }
            }));
        }

        public void disassociate(String str, String str2) throws RemoteException {
            Preconditions.checkNotNull(str);
            checkCallerIsSystemOr(str2);
            checkUsesFeature(str2, CompanionDeviceManagerService.getCallingUserId());
            CompanionDeviceManagerService.this.removeAssociation(CompanionDeviceManagerService.getCallingUserId(), str2, str);
        }

        private void checkCallerIsSystemOr(String str) throws RemoteException {
            checkCallerIsSystemOr(str, CompanionDeviceManagerService.getCallingUserId());
        }

        private void checkCallerIsSystemOr(String str, int i) throws RemoteException {
            if (!CompanionDeviceManagerService.isCallerSystem()) {
                Preconditions.checkArgument(CompanionDeviceManagerService.getCallingUserId() == i, "Must be called by either same user or system");
                int callingUid = Binder.getCallingUid();
                if (CompanionDeviceManagerService.this.mAppOpsManager.checkPackage(callingUid, str) != 0) {
                    throw new SecurityException(str + " doesn't belong to uid " + callingUid);
                }
            }
        }

        public PendingIntent requestNotificationAccess(ComponentName componentName) throws RemoteException {
            String packageName = componentName.getPackageName();
            checkCanCallNotificationApi(packageName);
            int callingUserId = CompanionDeviceManagerService.getCallingUserId();
            String strUnicodeWrap = BidiFormatter.getInstance().unicodeWrap(CompanionDeviceManagerService.this.getPackageInfo(packageName, callingUserId).applicationInfo.loadSafeLabel(CompanionDeviceManagerService.this.getContext().getPackageManager()).toString());
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return PendingIntent.getActivity(CompanionDeviceManagerService.this.getContext(), 0, NotificationAccessConfirmationActivityContract.launcherIntent(callingUserId, componentName, strUnicodeWrap), 1409286144);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public boolean hasNotificationAccess(ComponentName componentName) throws RemoteException {
            checkCanCallNotificationApi(componentName.getPackageName());
            return new SettingsStringUtil.ComponentNameSet(Settings.Secure.getString(CompanionDeviceManagerService.this.getContext().getContentResolver(), "enabled_notification_listeners")).contains(componentName);
        }

        private void checkCanCallNotificationApi(String str) throws RemoteException {
            checkCallerIsSystemOr(str);
            int callingUserId = CompanionDeviceManagerService.getCallingUserId();
            Preconditions.checkState(!ArrayUtils.isEmpty(CompanionDeviceManagerService.this.readAllAssociations(callingUserId, str)), "App must have an association before calling this API");
            checkUsesFeature(str, callingUserId);
        }

        private void checkUsesFeature(String str, int i) {
            if (!CompanionDeviceManagerService.isCallerSystem()) {
                FeatureInfo[] featureInfoArr = CompanionDeviceManagerService.this.getPackageInfo(str, i).reqFeatures;
                int size = ArrayUtils.size(featureInfoArr);
                for (int i2 = 0; i2 < size; i2++) {
                    if ("android.software.companion_device_setup".equals(featureInfoArr[i2].name)) {
                        return;
                    }
                }
                throw new IllegalStateException("Must declare uses-feature android.software.companion_device_setup in manifest to use this API");
            }
        }

        public void onShellCommand(FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, FileDescriptor fileDescriptor3, String[] strArr, ShellCallback shellCallback, ResultReceiver resultReceiver) throws RemoteException {
            CompanionDeviceManagerService.this.new ShellCmd().exec(this, fileDescriptor, fileDescriptor2, fileDescriptor3, strArr, shellCallback, resultReceiver);
        }
    }

    private static int getCallingUserId() {
        return UserHandle.getUserId(Binder.getCallingUid());
    }

    private static boolean isCallerSystem() {
        return Binder.getCallingUid() == 1000;
    }

    private ServiceConnection createServiceConnection(final AssociationRequest associationRequest, final IFindDeviceCallback iFindDeviceCallback, final String str) {
        this.mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                CompanionDeviceManagerService.this.mFindDeviceCallback = iFindDeviceCallback;
                CompanionDeviceManagerService.this.mRequest = associationRequest;
                CompanionDeviceManagerService.this.mCallingPackage = str;
                try {
                    CompanionDeviceManagerService.this.mFindDeviceCallback.asBinder().linkToDeath(CompanionDeviceManagerService.this, 0);
                    try {
                        ICompanionDeviceDiscoveryService.Stub.asInterface(iBinder).startDiscovery(associationRequest, str, iFindDeviceCallback, CompanionDeviceManagerService.this.getServiceCallback());
                    } catch (RemoteException e) {
                        Log.e(CompanionDeviceManagerService.LOG_TAG, "Error while initiating device discovery", e);
                    }
                } catch (RemoteException e2) {
                    CompanionDeviceManagerService.this.cleanup();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
            }
        };
        return this.mServiceConnection;
    }

    private ICompanionDeviceDiscoveryServiceCallback.Stub getServiceCallback() {
        return new ICompanionDeviceDiscoveryServiceCallback.Stub() {
            public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
                try {
                    return super.onTransact(i, parcel, parcel2, i2);
                } catch (Throwable th) {
                    Slog.e(CompanionDeviceManagerService.LOG_TAG, "Error during IPC", th);
                    throw ExceptionUtils.propagate(th, RemoteException.class);
                }
            }

            public void onDeviceSelected(String str, int i, String str2) {
                CompanionDeviceManagerService.this.addAssociation(i, str, str2);
                CompanionDeviceManagerService.this.cleanup();
            }

            public void onDeviceSelectionCancel() {
                CompanionDeviceManagerService.this.cleanup();
            }
        };
    }

    void addAssociation(int i, String str, String str2) {
        updateSpecialAccessPermissionForAssociatedPackage(str, i);
        recordAssociation(str, str2);
    }

    void removeAssociation(final int i, final String str, final String str2) {
        updateAssociations(new Function() {
            @Override
            public final Object apply(Object obj) {
                return CollectionUtils.remove((Set) obj, new CompanionDeviceManagerService.Association(this.f$0, i, str2, str, null));
            }
        });
    }

    private void updateSpecialAccessPermissionForAssociatedPackage(String str, int i) {
        PackageInfo packageInfo = getPackageInfo(str, i);
        if (packageInfo == null) {
            return;
        }
        Binder.withCleanCallingIdentity(PooledLambda.obtainRunnable(new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                ((CompanionDeviceManagerService) obj).updateSpecialAccessPermissionAsSystem((PackageInfo) obj2);
            }
        }, this, packageInfo).recycleOnUse());
    }

    private void updateSpecialAccessPermissionAsSystem(PackageInfo packageInfo) {
        try {
            if (containsEither(packageInfo.requestedPermissions, "android.permission.RUN_IN_BACKGROUND", "android.permission.REQUEST_COMPANION_RUN_IN_BACKGROUND")) {
                this.mIdleController.addPowerSaveWhitelistApp(packageInfo.packageName);
            } else {
                this.mIdleController.removePowerSaveWhitelistApp(packageInfo.packageName);
            }
        } catch (RemoteException e) {
        }
        NetworkPolicyManager networkPolicyManagerFrom = NetworkPolicyManager.from(getContext());
        if (containsEither(packageInfo.requestedPermissions, "android.permission.USE_DATA_IN_BACKGROUND", "android.permission.REQUEST_COMPANION_USE_DATA_IN_BACKGROUND")) {
            networkPolicyManagerFrom.addUidPolicy(packageInfo.applicationInfo.uid, 4);
        } else {
            networkPolicyManagerFrom.removeUidPolicy(packageInfo.applicationInfo.uid, 4);
        }
    }

    private static <T> boolean containsEither(T[] tArr, T t, T t2) {
        return ArrayUtils.contains(tArr, t) || ArrayUtils.contains(tArr, t2);
    }

    private PackageInfo getPackageInfo(String str, int i) {
        return (PackageInfo) Binder.withCleanCallingIdentity(PooledLambda.obtainSupplier(new TriFunction() {
            public final Object apply(Object obj, Object obj2, Object obj3) {
                return CompanionDeviceManagerService.lambda$getPackageInfo$1((Context) obj, (String) obj2, (Integer) obj3);
            }
        }, getContext(), str, Integer.valueOf(i)).recycleOnUse());
    }

    static PackageInfo lambda$getPackageInfo$1(Context context, String str, Integer num) {
        try {
            return context.getPackageManager().getPackageInfoAsUser(str, 20480, num.intValue());
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(LOG_TAG, "Failed to get PackageInfo for package " + str, e);
            return null;
        }
    }

    private void recordAssociation(final String str, final String str2) {
        final int callingUserId = getCallingUserId();
        updateAssociations(new Function() {
            @Override
            public final Object apply(Object obj) {
                return CollectionUtils.add((Set) obj, new CompanionDeviceManagerService.Association(this.f$0, callingUserId, str2, str, null));
            }
        });
    }

    private void updateAssociations(Function<Set<Association>, Set<Association>> function) {
        updateAssociations(function, getCallingUserId());
    }

    private void updateAssociations(Function<Set<Association>, Set<Association>> function, int i) {
        AtomicFile storageFileForUser = getStorageFileForUser(i);
        synchronized (storageFileForUser) {
            Set<Association> allAssociations = readAllAssociations(i);
            Set setCopyOf = CollectionUtils.copyOf(allAssociations);
            final Set<Association> setApply = function.apply(allAssociations);
            if (CollectionUtils.size(setCopyOf) == CollectionUtils.size(setApply)) {
                return;
            }
            storageFileForUser.write(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    CompanionDeviceManagerService.lambda$updateAssociations$4(setApply, (FileOutputStream) obj);
                }
            });
        }
    }

    static void lambda$updateAssociations$4(Set set, FileOutputStream fileOutputStream) {
        final XmlSerializer xmlSerializerNewSerializer = Xml.newSerializer();
        try {
            xmlSerializerNewSerializer.setOutput(fileOutputStream, StandardCharsets.UTF_8.name());
            xmlSerializerNewSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            xmlSerializerNewSerializer.startDocument(null, true);
            xmlSerializerNewSerializer.startTag(null, XML_TAG_ASSOCIATIONS);
            CollectionUtils.forEach(set, new FunctionalUtils.ThrowingConsumer() {
                public final void acceptOrThrow(Object obj) throws IOException {
                    CompanionDeviceManagerService.Association association = (CompanionDeviceManagerService.Association) obj;
                    xmlSerializerNewSerializer.startTag(null, CompanionDeviceManagerService.XML_TAG_ASSOCIATION).attribute(null, "package", association.companionAppPackage).attribute(null, CompanionDeviceManagerService.XML_ATTR_DEVICE, association.deviceAddress).endTag(null, CompanionDeviceManagerService.XML_TAG_ASSOCIATION);
                }
            });
            xmlSerializerNewSerializer.endTag(null, XML_TAG_ASSOCIATIONS);
            xmlSerializerNewSerializer.endDocument();
        } catch (Exception e) {
            Slog.e(LOG_TAG, "Error while writing associations file", e);
            throw ExceptionUtils.propagate(e);
        }
    }

    private AtomicFile getStorageFileForUser(int i) {
        return this.mUidToStorage.computeIfAbsent(Integer.valueOf(i), new Function() {
            @Override
            public final Object apply(Object obj) {
                return CompanionDeviceManagerService.lambda$getStorageFileForUser$5((Integer) obj);
            }
        });
    }

    static AtomicFile lambda$getStorageFileForUser$5(Integer num) {
        return new AtomicFile(new File(Environment.getUserSystemDirectory(num.intValue()), XML_FILE_NAME));
    }

    private Set<Association> readAllAssociations(int i) {
        return readAllAssociations(i, null);
    }

    private Set<Association> readAllAssociations(int i, String str) {
        Throwable th;
        ArraySet arraySetAdd;
        AtomicFile storageFileForUser = getStorageFileForUser(i);
        if (!storageFileForUser.getBaseFile().exists()) {
            return null;
        }
        XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
        synchronized (storageFileForUser) {
            try {
                FileInputStream fileInputStreamOpenRead = storageFileForUser.openRead();
                try {
                    xmlPullParserNewPullParser.setInput(fileInputStreamOpenRead, StandardCharsets.UTF_8.name());
                    arraySetAdd = null;
                    while (true) {
                        int next = xmlPullParserNewPullParser.next();
                        if (next == 1) {
                            break;
                        }
                        if (next == 2 || XML_TAG_ASSOCIATIONS.equals(xmlPullParserNewPullParser.getName())) {
                            String attributeValue = xmlPullParserNewPullParser.getAttributeValue(null, "package");
                            String attributeValue2 = xmlPullParserNewPullParser.getAttributeValue(null, XML_ATTR_DEVICE);
                            if (attributeValue != null && attributeValue2 != null && (str == null || str.equals(attributeValue))) {
                                arraySetAdd = ArrayUtils.add(arraySetAdd, new Association(this, i, attributeValue2, attributeValue, null));
                            }
                        }
                    }
                    if (fileInputStreamOpenRead != null) {
                        fileInputStreamOpenRead.close();
                    }
                } catch (Throwable th2) {
                    th = th2;
                    th = null;
                    if (fileInputStreamOpenRead != null) {
                    }
                }
            } catch (IOException | XmlPullParserException e) {
                Slog.e(LOG_TAG, "Error while reading associations file", e);
                return null;
            }
        }
        return arraySetAdd;
    }

    private class Association {
        public final String companionAppPackage;
        public final String deviceAddress;
        public final int uid;

        Association(CompanionDeviceManagerService companionDeviceManagerService, int i, String str, String str2, AnonymousClass1 anonymousClass1) {
            this(i, str, str2);
        }

        private Association(int i, String str, String str2) {
            this.uid = i;
            this.deviceAddress = (String) Preconditions.checkNotNull(str);
            this.companionAppPackage = (String) Preconditions.checkNotNull(str2);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            Association association = (Association) obj;
            if (this.uid != association.uid || !this.deviceAddress.equals(association.deviceAddress)) {
                return false;
            }
            return this.companionAppPackage.equals(association.companionAppPackage);
        }

        public int hashCode() {
            return (31 * ((this.uid * 31) + this.deviceAddress.hashCode())) + this.companionAppPackage.hashCode();
        }
    }

    private class ShellCmd extends ShellCommand {
        public static final String USAGE = "help\nlist USER_ID\nassociate USER_ID PACKAGE MAC_ADDRESS\ndisassociate USER_ID PACKAGE MAC_ADDRESS";

        ShellCmd() {
            CompanionDeviceManagerService.this.getContext().enforceCallingOrSelfPermission("android.permission.MANAGE_COMPANION_DEVICES", "ShellCmd");
        }

        public int onCommand(String str) {
            byte b;
            int iHashCode = str.hashCode();
            if (iHashCode != 3322014) {
                if (iHashCode != 784321104) {
                    b = (iHashCode == 1586499358 && str.equals("associate")) ? (byte) 1 : (byte) -1;
                } else if (str.equals("disassociate")) {
                    b = 2;
                }
            } else if (str.equals("list")) {
                b = 0;
            }
            switch (b) {
                case 0:
                    CollectionUtils.forEach(CompanionDeviceManagerService.this.readAllAssociations(getNextArgInt()), new FunctionalUtils.ThrowingConsumer() {
                        public final void acceptOrThrow(Object obj) {
                            CompanionDeviceManagerService.Association association = (CompanionDeviceManagerService.Association) obj;
                            this.f$0.getOutPrintWriter().println(association.companionAppPackage + " " + association.deviceAddress);
                        }
                    });
                    return 0;
                case 1:
                    CompanionDeviceManagerService.this.addAssociation(getNextArgInt(), getNextArgRequired(), getNextArgRequired());
                    return 0;
                case 2:
                    CompanionDeviceManagerService.this.removeAssociation(getNextArgInt(), getNextArgRequired(), getNextArgRequired());
                    return 0;
                default:
                    return handleDefaultCommands(str);
            }
        }

        private int getNextArgInt() {
            return Integer.parseInt(getNextArgRequired());
        }

        public void onHelp() {
            getOutPrintWriter().println(USAGE);
        }
    }
}
