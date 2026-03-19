package android.content.pm;

import android.Manifest;
import android.accounts.GrantCredentialsPermissionActivity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.Environment;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.util.AtomicFile;
import android.util.AttributeSet;
import android.util.IntArray;
import android.util.Log;
import android.util.SparseArray;
import android.util.Xml;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FastXmlSerializer;
import com.google.android.collect.Lists;
import com.google.android.collect.Maps;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public abstract class RegisteredServicesCache<V> {
    private static final boolean DEBUG = false;
    protected static final String REGISTERED_SERVICES_DIR = "registered_services";
    private static final String TAG = "PackageManager";
    private final String mAttributesName;
    public final Context mContext;
    private Handler mHandler;
    private final String mInterfaceName;
    private RegisteredServicesCacheListener<V> mListener;
    private final String mMetaDataName;
    private final XmlSerializerAndParser<V> mSerializerAndParser;
    protected final Object mServicesLock = new Object();

    @GuardedBy("mServicesLock")
    private final SparseArray<UserServices<V>> mUserServices = new SparseArray<>(2);
    private final BroadcastReceiver mPackageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int intExtra = intent.getIntExtra(Intent.EXTRA_UID, -1);
            if (intExtra != -1) {
                RegisteredServicesCache.this.handlePackageEvent(intent, UserHandle.getUserId(intExtra));
            }
        }
    };
    private final BroadcastReceiver mExternalReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            RegisteredServicesCache.this.handlePackageEvent(intent, 0);
        }
    };
    private final BroadcastReceiver mUserRemovedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            RegisteredServicesCache.this.onUserRemoved(intent.getIntExtra(Intent.EXTRA_USER_HANDLE, -1));
        }
    };

    public abstract V parseServiceAttributes(Resources resources, String str, AttributeSet attributeSet);

    private static class UserServices<V> {

        @GuardedBy("mServicesLock")
        boolean mBindInstantServiceAllowed;

        @GuardedBy("mServicesLock")
        boolean mPersistentServicesFileDidNotExist;

        @GuardedBy("mServicesLock")
        final Map<V, Integer> persistentServices;

        @GuardedBy("mServicesLock")
        Map<V, ServiceInfo<V>> services;

        private UserServices() {
            this.persistentServices = Maps.newHashMap();
            this.services = null;
            this.mPersistentServicesFileDidNotExist = true;
            this.mBindInstantServiceAllowed = false;
        }
    }

    @GuardedBy("mServicesLock")
    private UserServices<V> findOrCreateUserLocked(int i) {
        return findOrCreateUserLocked(i, true);
    }

    @GuardedBy("mServicesLock")
    private UserServices<V> findOrCreateUserLocked(int i, boolean z) throws Throwable {
        UserInfo user;
        FileInputStream fileInputStreamOpenRead;
        UserServices<V> userServices = this.mUserServices.get(i);
        if (userServices == null) {
            ?? r1 = 0;
            r1 = 0;
            FileInputStream fileInputStream = null;
            userServices = new UserServices<>();
            this.mUserServices.put(i, userServices);
            if (z && this.mSerializerAndParser != null && (user = getUser(i)) != null) {
                AtomicFile atomicFileCreateFileForUser = createFileForUser(user.id);
                try {
                    if (atomicFileCreateFileForUser.getBaseFile().exists()) {
                        try {
                            fileInputStreamOpenRead = atomicFileCreateFileForUser.openRead();
                        } catch (Exception e) {
                            e = e;
                        }
                        try {
                            readPersistentServicesLocked(fileInputStreamOpenRead);
                            IoUtils.closeQuietly(fileInputStreamOpenRead);
                        } catch (Exception e2) {
                            fileInputStream = fileInputStreamOpenRead;
                            e = e2;
                            Log.w(TAG, "Error reading persistent services for user " + user.id, e);
                            IoUtils.closeQuietly(fileInputStream);
                            r1 = fileInputStream;
                        } catch (Throwable th) {
                            th = th;
                            r1 = fileInputStreamOpenRead;
                            IoUtils.closeQuietly((AutoCloseable) r1);
                            throw th;
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            }
        }
        return userServices;
    }

    public RegisteredServicesCache(Context context, String str, String str2, String str3, XmlSerializerAndParser<V> xmlSerializerAndParser) throws Throwable {
        this.mContext = context;
        this.mInterfaceName = str;
        this.mMetaDataName = str2;
        this.mAttributesName = str3;
        this.mSerializerAndParser = xmlSerializerAndParser;
        migrateIfNecessaryLocked();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addDataScheme("package");
        this.mContext.registerReceiverAsUser(this.mPackageReceiver, UserHandle.ALL, intentFilter, null, null);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
        intentFilter2.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
        this.mContext.registerReceiver(this.mExternalReceiver, intentFilter2);
        IntentFilter intentFilter3 = new IntentFilter();
        intentFilter2.addAction(Intent.ACTION_USER_REMOVED);
        this.mContext.registerReceiver(this.mUserRemovedReceiver, intentFilter3);
    }

    private final void handlePackageEvent(Intent intent, int i) {
        String action = intent.getAction();
        boolean z = Intent.ACTION_PACKAGE_REMOVED.equals(action) || Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE.equals(action);
        boolean booleanExtra = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
        if (!z || !booleanExtra) {
            int[] intArrayExtra = null;
            if (Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE.equals(action) || Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE.equals(action)) {
                intArrayExtra = intent.getIntArrayExtra(Intent.EXTRA_CHANGED_UID_LIST);
            } else {
                int intExtra = intent.getIntExtra(Intent.EXTRA_UID, -1);
                if (intExtra > 0) {
                    intArrayExtra = new int[]{intExtra};
                }
            }
            generateServicesMap(intArrayExtra, i);
        }
    }

    public void invalidateCache(int i) {
        synchronized (this.mServicesLock) {
            findOrCreateUserLocked(i).services = null;
            onServicesChangedLocked(i);
        }
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr, int i) {
        synchronized (this.mServicesLock) {
            UserServices<V> userServicesFindOrCreateUserLocked = findOrCreateUserLocked(i);
            if (userServicesFindOrCreateUserLocked.services != null) {
                printWriter.println("RegisteredServicesCache: " + userServicesFindOrCreateUserLocked.services.size() + " services");
                Iterator<ServiceInfo<V>> it = userServicesFindOrCreateUserLocked.services.values().iterator();
                while (it.hasNext()) {
                    printWriter.println("  " + it.next());
                }
            } else {
                printWriter.println("RegisteredServicesCache: services not loaded");
            }
        }
    }

    public RegisteredServicesCacheListener<V> getListener() {
        RegisteredServicesCacheListener<V> registeredServicesCacheListener;
        synchronized (this) {
            registeredServicesCacheListener = this.mListener;
        }
        return registeredServicesCacheListener;
    }

    public void setListener(RegisteredServicesCacheListener<V> registeredServicesCacheListener, Handler handler) {
        if (handler == null) {
            handler = new Handler(this.mContext.getMainLooper());
        }
        synchronized (this) {
            this.mHandler = handler;
            this.mListener = registeredServicesCacheListener;
        }
    }

    private void notifyListener(final V v, final int i, final boolean z) {
        final RegisteredServicesCacheListener<V> registeredServicesCacheListener;
        Handler handler;
        synchronized (this) {
            registeredServicesCacheListener = this.mListener;
            handler = this.mHandler;
        }
        if (registeredServicesCacheListener == null) {
            return;
        }
        handler.post(new Runnable() {
            @Override
            public void run() {
                registeredServicesCacheListener.onServiceChanged(v, i, z);
            }
        });
    }

    public static class ServiceInfo<V> {
        public final ComponentInfo componentInfo;
        public final ComponentName componentName;
        public final V type;
        public final int uid;

        public ServiceInfo(V v, ComponentInfo componentInfo, ComponentName componentName) {
            this.type = v;
            this.componentInfo = componentInfo;
            this.componentName = componentName;
            this.uid = componentInfo != null ? componentInfo.applicationInfo.uid : -1;
        }

        public String toString() {
            return "ServiceInfo: " + this.type + ", " + this.componentName + ", uid " + this.uid;
        }
    }

    public ServiceInfo<V> getServiceInfo(V v, int i) {
        ServiceInfo<V> serviceInfo;
        synchronized (this.mServicesLock) {
            UserServices<V> userServicesFindOrCreateUserLocked = findOrCreateUserLocked(i);
            if (userServicesFindOrCreateUserLocked.services == null) {
                generateServicesMap(null, i);
            }
            serviceInfo = userServicesFindOrCreateUserLocked.services.get(v);
        }
        return serviceInfo;
    }

    public Collection<ServiceInfo<V>> getAllServices(int i) {
        Collection<ServiceInfo<V>> collectionUnmodifiableCollection;
        synchronized (this.mServicesLock) {
            UserServices<V> userServicesFindOrCreateUserLocked = findOrCreateUserLocked(i);
            if (userServicesFindOrCreateUserLocked.services == null) {
                generateServicesMap(null, i);
            }
            collectionUnmodifiableCollection = Collections.unmodifiableCollection(new ArrayList(userServicesFindOrCreateUserLocked.services.values()));
        }
        return collectionUnmodifiableCollection;
    }

    public void updateServices(int i) {
        ApplicationInfo applicationInfoAsUser;
        synchronized (this.mServicesLock) {
            UserServices<V> userServicesFindOrCreateUserLocked = findOrCreateUserLocked(i);
            if (userServicesFindOrCreateUserLocked.services == null) {
                return;
            }
            IntArray intArray = null;
            for (ServiceInfo serviceInfo : new ArrayList(userServicesFindOrCreateUserLocked.services.values())) {
                long j = serviceInfo.componentInfo.applicationInfo.versionCode;
                try {
                    applicationInfoAsUser = this.mContext.getPackageManager().getApplicationInfoAsUser(serviceInfo.componentInfo.packageName, 0, i);
                } catch (PackageManager.NameNotFoundException e) {
                    applicationInfoAsUser = null;
                }
                if (applicationInfoAsUser == null || applicationInfoAsUser.versionCode != j) {
                    if (intArray == null) {
                        intArray = new IntArray();
                    }
                    intArray.add(serviceInfo.uid);
                }
            }
            if (intArray != null && intArray.size() > 0) {
                generateServicesMap(intArray.toArray(), i);
            }
        }
    }

    public boolean getBindInstantServiceAllowed(int i) {
        boolean z;
        this.mContext.enforceCallingOrSelfPermission(Manifest.permission.MANAGE_BIND_INSTANT_SERVICE, "getBindInstantServiceAllowed");
        synchronized (this.mServicesLock) {
            z = findOrCreateUserLocked(i).mBindInstantServiceAllowed;
        }
        return z;
    }

    public void setBindInstantServiceAllowed(int i, boolean z) {
        this.mContext.enforceCallingOrSelfPermission(Manifest.permission.MANAGE_BIND_INSTANT_SERVICE, "setBindInstantServiceAllowed");
        synchronized (this.mServicesLock) {
            findOrCreateUserLocked(i).mBindInstantServiceAllowed = z;
        }
    }

    @VisibleForTesting
    protected boolean inSystemImage(int i) {
        String[] packagesForUid = this.mContext.getPackageManager().getPackagesForUid(i);
        if (packagesForUid != null) {
            for (String str : packagesForUid) {
                try {
                    if ((this.mContext.getPackageManager().getPackageInfo(str, 0).applicationInfo.flags & 1) != 0) {
                        return true;
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    return false;
                }
            }
        }
        return false;
    }

    @VisibleForTesting
    protected List<ResolveInfo> queryIntentServices(int i) {
        int i2;
        PackageManager packageManager = this.mContext.getPackageManager();
        synchronized (this.mServicesLock) {
            if (findOrCreateUserLocked(i).mBindInstantServiceAllowed) {
                i2 = 9175168;
            } else {
                i2 = 786560;
            }
        }
        return packageManager.queryIntentServicesAsUser(new Intent(this.mInterfaceName), i2, i);
    }

    private void generateServicesMap(int[] iArr, int i) {
        ArrayList<ServiceInfo<V>> arrayList = new ArrayList();
        for (ResolveInfo resolveInfo : queryIntentServices(i)) {
            try {
                ServiceInfo serviceInfo = parseServiceInfo(resolveInfo);
                if (serviceInfo == null) {
                    Log.w(TAG, "Unable to load service info " + resolveInfo.toString());
                } else {
                    arrayList.add(serviceInfo);
                }
            } catch (IOException | XmlPullParserException e) {
                Log.w(TAG, "Unable to load service info " + resolveInfo.toString(), e);
            }
        }
        synchronized (this.mServicesLock) {
            UserServices userServicesFindOrCreateUserLocked = findOrCreateUserLocked(i);
            boolean z = userServicesFindOrCreateUserLocked.services == null;
            if (z) {
                userServicesFindOrCreateUserLocked.services = Maps.newHashMap();
            }
            new StringBuilder();
            boolean z2 = false;
            for (ServiceInfo<V> serviceInfo2 : arrayList) {
                Integer num = userServicesFindOrCreateUserLocked.persistentServices.get(serviceInfo2.type);
                if (num == null) {
                    userServicesFindOrCreateUserLocked.services.put(serviceInfo2.type, serviceInfo2);
                    userServicesFindOrCreateUserLocked.persistentServices.put(serviceInfo2.type, Integer.valueOf(serviceInfo2.uid));
                    if (!userServicesFindOrCreateUserLocked.mPersistentServicesFileDidNotExist || !z) {
                        notifyListener(serviceInfo2.type, i, false);
                    }
                } else if (num.intValue() == serviceInfo2.uid) {
                    userServicesFindOrCreateUserLocked.services.put(serviceInfo2.type, serviceInfo2);
                } else if (inSystemImage(serviceInfo2.uid) || !containsTypeAndUid(arrayList, serviceInfo2.type, num.intValue())) {
                    userServicesFindOrCreateUserLocked.services.put(serviceInfo2.type, serviceInfo2);
                    userServicesFindOrCreateUserLocked.persistentServices.put(serviceInfo2.type, Integer.valueOf(serviceInfo2.uid));
                    notifyListener(serviceInfo2.type, i, false);
                }
                z2 = true;
            }
            ArrayList arrayListNewArrayList = Lists.newArrayList();
            for (V v : userServicesFindOrCreateUserLocked.persistentServices.keySet()) {
                if (!containsType(arrayList, v) && containsUid(iArr, userServicesFindOrCreateUserLocked.persistentServices.get(v).intValue())) {
                    arrayListNewArrayList.add(v);
                }
            }
            for (Object obj : arrayListNewArrayList) {
                userServicesFindOrCreateUserLocked.persistentServices.remove(obj);
                userServicesFindOrCreateUserLocked.services.remove(obj);
                notifyListener(obj, i, true);
                z2 = true;
            }
            if (z2) {
                onServicesChangedLocked(i);
                writePersistentServicesLocked(userServicesFindOrCreateUserLocked, i);
            }
        }
    }

    protected void onServicesChangedLocked(int i) {
    }

    private boolean containsUid(int[] iArr, int i) {
        return iArr == null || ArrayUtils.contains(iArr, i);
    }

    private boolean containsType(ArrayList<ServiceInfo<V>> arrayList, V v) {
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            if (arrayList.get(i).type.equals(v)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsTypeAndUid(ArrayList<ServiceInfo<V>> arrayList, V v, int i) {
        int size = arrayList.size();
        for (int i2 = 0; i2 < size; i2++) {
            ServiceInfo<V> serviceInfo = arrayList.get(i2);
            if (serviceInfo.type.equals(v) && serviceInfo.uid == i) {
                return true;
            }
        }
        return false;
    }

    @VisibleForTesting
    protected ServiceInfo<V> parseServiceInfo(ResolveInfo resolveInfo) throws Throwable {
        XmlResourceParser xmlResourceParserLoadXmlMetaData;
        int next;
        android.content.pm.ServiceInfo serviceInfo = resolveInfo.serviceInfo;
        ComponentName componentName = new ComponentName(serviceInfo.packageName, serviceInfo.name);
        PackageManager packageManager = this.mContext.getPackageManager();
        try {
            try {
                xmlResourceParserLoadXmlMetaData = serviceInfo.loadXmlMetaData(packageManager, this.mMetaDataName);
            } catch (Throwable th) {
                th = th;
                xmlResourceParserLoadXmlMetaData = null;
            }
        } catch (PackageManager.NameNotFoundException e) {
        }
        try {
            if (xmlResourceParserLoadXmlMetaData == null) {
                throw new XmlPullParserException("No " + this.mMetaDataName + " meta-data");
            }
            AttributeSet attributeSetAsAttributeSet = Xml.asAttributeSet(xmlResourceParserLoadXmlMetaData);
            do {
                next = xmlResourceParserLoadXmlMetaData.next();
                if (next == 1) {
                    break;
                }
            } while (next != 2);
            if (!this.mAttributesName.equals(xmlResourceParserLoadXmlMetaData.getName())) {
                throw new XmlPullParserException("Meta-data does not start with " + this.mAttributesName + " tag");
            }
            V serviceAttributes = parseServiceAttributes(packageManager.getResourcesForApplication(serviceInfo.applicationInfo), serviceInfo.packageName, attributeSetAsAttributeSet);
            if (serviceAttributes == null) {
                if (xmlResourceParserLoadXmlMetaData != null) {
                    xmlResourceParserLoadXmlMetaData.close();
                }
                return null;
            }
            ServiceInfo<V> serviceInfo2 = new ServiceInfo<>(serviceAttributes, resolveInfo.serviceInfo, componentName);
            if (xmlResourceParserLoadXmlMetaData != null) {
                xmlResourceParserLoadXmlMetaData.close();
            }
            return serviceInfo2;
        } catch (PackageManager.NameNotFoundException e2) {
            throw new XmlPullParserException("Unable to load resources for pacakge " + serviceInfo.packageName);
        } catch (Throwable th2) {
            th = th2;
            if (xmlResourceParserLoadXmlMetaData != null) {
                xmlResourceParserLoadXmlMetaData.close();
            }
            throw th;
        }
    }

    private void readPersistentServicesLocked(InputStream inputStream) throws XmlPullParserException, IOException {
        XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
        xmlPullParserNewPullParser.setInput(inputStream, StandardCharsets.UTF_8.name());
        for (int eventType = xmlPullParserNewPullParser.getEventType(); eventType != 2 && eventType != 1; eventType = xmlPullParserNewPullParser.next()) {
        }
        if ("services".equals(xmlPullParserNewPullParser.getName())) {
            int next = xmlPullParserNewPullParser.next();
            do {
                if (next == 2 && xmlPullParserNewPullParser.getDepth() == 2 && "service".equals(xmlPullParserNewPullParser.getName())) {
                    V vCreateFromXml = this.mSerializerAndParser.createFromXml(xmlPullParserNewPullParser);
                    if (vCreateFromXml != null) {
                        int i = Integer.parseInt(xmlPullParserNewPullParser.getAttributeValue(null, GrantCredentialsPermissionActivity.EXTRAS_REQUESTING_UID));
                        findOrCreateUserLocked(UserHandle.getUserId(i), false).persistentServices.put(vCreateFromXml, Integer.valueOf(i));
                    } else {
                        return;
                    }
                }
                next = xmlPullParserNewPullParser.next();
            } while (next != 1);
        }
    }

    private void migrateIfNecessaryLocked() throws Throwable {
        Throwable th;
        FileInputStream fileInputStreamOpenRead;
        Exception e;
        if (this.mSerializerAndParser == null) {
            return;
        }
        File file = new File(new File(getDataDirectory(), StorageManager.UUID_SYSTEM), REGISTERED_SERVICES_DIR);
        AtomicFile atomicFile = new AtomicFile(new File(file, this.mInterfaceName + ".xml"));
        if (atomicFile.getBaseFile().exists()) {
            File file2 = new File(file, this.mInterfaceName + ".xml.migrated");
            if (file2.exists()) {
                return;
            }
            try {
                fileInputStreamOpenRead = atomicFile.openRead();
                try {
                    try {
                        this.mUserServices.clear();
                        readPersistentServicesLocked(fileInputStreamOpenRead);
                    } catch (Exception e2) {
                        e = e2;
                        Log.w(TAG, "Error reading persistent services, starting from scratch", e);
                    }
                } catch (Throwable th2) {
                    th = th2;
                    IoUtils.closeQuietly(fileInputStreamOpenRead);
                    throw th;
                }
            } catch (Exception e3) {
                e = e3;
                fileInputStreamOpenRead = null;
            } catch (Throwable th3) {
                th = th3;
                fileInputStreamOpenRead = null;
                IoUtils.closeQuietly(fileInputStreamOpenRead);
                throw th;
            }
            IoUtils.closeQuietly(fileInputStreamOpenRead);
            try {
                for (UserInfo userInfo : getUsers()) {
                    UserServices<V> userServices = this.mUserServices.get(userInfo.id);
                    if (userServices != null) {
                        writePersistentServicesLocked(userServices, userInfo.id);
                    }
                }
                file2.createNewFile();
            } catch (Exception e4) {
                Log.w(TAG, "Migration failed", e4);
            }
            this.mUserServices.clear();
        }
    }

    private void writePersistentServicesLocked(UserServices<V> userServices, int i) {
        FileOutputStream fileOutputStreamStartWrite;
        if (this.mSerializerAndParser == null) {
            return;
        }
        AtomicFile atomicFileCreateFileForUser = createFileForUser(i);
        try {
            fileOutputStreamStartWrite = atomicFileCreateFileForUser.startWrite();
        } catch (IOException e) {
            e = e;
            fileOutputStreamStartWrite = null;
        }
        try {
            FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(fileOutputStreamStartWrite, StandardCharsets.UTF_8.name());
            fastXmlSerializer.startDocument(null, true);
            fastXmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            fastXmlSerializer.startTag(null, "services");
            for (Map.Entry<V, Integer> entry : userServices.persistentServices.entrySet()) {
                fastXmlSerializer.startTag(null, "service");
                fastXmlSerializer.attribute(null, GrantCredentialsPermissionActivity.EXTRAS_REQUESTING_UID, Integer.toString(entry.getValue().intValue()));
                this.mSerializerAndParser.writeAsXml(entry.getKey(), fastXmlSerializer);
                fastXmlSerializer.endTag(null, "service");
            }
            fastXmlSerializer.endTag(null, "services");
            fastXmlSerializer.endDocument();
            atomicFileCreateFileForUser.finishWrite(fileOutputStreamStartWrite);
        } catch (IOException e2) {
            e = e2;
            Log.w(TAG, "Error writing accounts", e);
            if (fileOutputStreamStartWrite != null) {
                atomicFileCreateFileForUser.failWrite(fileOutputStreamStartWrite);
            }
        }
    }

    @VisibleForTesting
    protected void onUserRemoved(int i) {
        synchronized (this.mServicesLock) {
            this.mUserServices.remove(i);
        }
    }

    @VisibleForTesting
    protected List<UserInfo> getUsers() {
        return UserManager.get(this.mContext).getUsers(true);
    }

    @VisibleForTesting
    protected UserInfo getUser(int i) {
        return UserManager.get(this.mContext).getUserInfo(i);
    }

    private AtomicFile createFileForUser(int i) {
        return new AtomicFile(new File(getUserSystemDirectory(i), "registered_services/" + this.mInterfaceName + ".xml"));
    }

    @VisibleForTesting
    protected File getUserSystemDirectory(int i) {
        return Environment.getUserSystemDirectory(i);
    }

    @VisibleForTesting
    protected File getDataDirectory() {
        return Environment.getDataDirectory();
    }

    @VisibleForTesting
    protected Map<V, Integer> getPersistentServices(int i) {
        return findOrCreateUserLocked(i).persistentServices;
    }
}
