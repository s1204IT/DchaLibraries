package com.android.server.storage;

import android.app.usage.CacheQuotaHint;
import android.app.usage.ICacheQuotaService;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManagerInternal;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.UserInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.ArrayMap;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseLongArray;
import android.util.Xml;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.AtomicFile;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.Preconditions;
import com.android.server.pm.Installer;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class CacheQuotaStrategy implements RemoteCallback.OnResultListener {
    private static final String ATTR_PREVIOUS_BYTES = "previousBytes";
    private static final String ATTR_QUOTA_IN_BYTES = "bytes";
    private static final String ATTR_UID = "uid";
    private static final String ATTR_UUID = "uuid";
    private static final String CACHE_INFO_TAG = "cache-info";
    private static final String TAG = "CacheQuotaStrategy";
    private static final String TAG_QUOTA = "quota";
    private final Context mContext;
    private final Installer mInstaller;
    private final Object mLock = new Object();
    private AtomicFile mPreviousValuesFile = new AtomicFile(new File(new File(Environment.getDataDirectory(), "system"), "cachequota.xml"));
    private final ArrayMap<String, SparseLongArray> mQuotaMap;
    private ICacheQuotaService mRemoteService;
    private ServiceConnection mServiceConnection;
    private final UsageStatsManagerInternal mUsageStats;

    public CacheQuotaStrategy(Context context, UsageStatsManagerInternal usageStatsManagerInternal, Installer installer, ArrayMap<String, SparseLongArray> arrayMap) {
        this.mContext = (Context) Preconditions.checkNotNull(context);
        this.mUsageStats = (UsageStatsManagerInternal) Preconditions.checkNotNull(usageStatsManagerInternal);
        this.mInstaller = (Installer) Preconditions.checkNotNull(installer);
        this.mQuotaMap = (ArrayMap) Preconditions.checkNotNull(arrayMap);
    }

    public void recalculateQuotas() {
        createServiceConnection();
        ComponentName serviceComponentName = getServiceComponentName();
        if (serviceComponentName != null) {
            Intent intent = new Intent();
            intent.setComponent(serviceComponentName);
            this.mContext.bindServiceAsUser(intent, this.mServiceConnection, 1, UserHandle.CURRENT);
        }
    }

    private void createServiceConnection() {
        if (this.mServiceConnection != null) {
            return;
        }
        this.mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, final IBinder iBinder) {
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (CacheQuotaStrategy.this.mLock) {
                            CacheQuotaStrategy.this.mRemoteService = ICacheQuotaService.Stub.asInterface(iBinder);
                            List unfulfilledRequests = CacheQuotaStrategy.this.getUnfulfilledRequests();
                            try {
                                CacheQuotaStrategy.this.mRemoteService.computeCacheQuotaHints(new RemoteCallback(CacheQuotaStrategy.this), unfulfilledRequests);
                            } catch (RemoteException e) {
                                Slog.w(CacheQuotaStrategy.TAG, "Remote exception occurred while trying to get cache quota", e);
                            }
                        }
                    }
                });
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                synchronized (CacheQuotaStrategy.this.mLock) {
                    CacheQuotaStrategy.this.mRemoteService = null;
                }
            }
        };
    }

    private List<CacheQuotaHint> getUnfulfilledRequests() {
        PackageManager packageManager;
        long jCurrentTimeMillis = System.currentTimeMillis();
        long j = jCurrentTimeMillis - 31449600000L;
        ArrayList arrayList = new ArrayList();
        List users = ((UserManager) this.mContext.getSystemService(UserManager.class)).getUsers();
        int size = users.size();
        PackageManager packageManager2 = this.mContext.getPackageManager();
        int i = 0;
        while (i < size) {
            UserInfo userInfo = (UserInfo) users.get(i);
            int i2 = i;
            UserInfo userInfo2 = userInfo;
            PackageManager packageManager3 = packageManager2;
            int i3 = size;
            List<UsageStats> listQueryUsageStatsForUser = this.mUsageStats.queryUsageStatsForUser(userInfo.id, 4, j, jCurrentTimeMillis, false);
            if (listQueryUsageStatsForUser != null) {
                for (UsageStats usageStats : listQueryUsageStatsForUser) {
                    UserInfo userInfo3 = userInfo2;
                    try {
                        packageManager = packageManager3;
                        try {
                            ApplicationInfo applicationInfoAsUser = packageManager.getApplicationInfoAsUser(usageStats.getPackageName(), 0, userInfo3.id);
                            arrayList.add(new CacheQuotaHint.Builder().setVolumeUuid(applicationInfoAsUser.volumeUuid).setUid(applicationInfoAsUser.uid).setUsageStats(usageStats).setQuota(-1L).build());
                        } catch (PackageManager.NameNotFoundException e) {
                        }
                    } catch (PackageManager.NameNotFoundException e2) {
                        packageManager = packageManager3;
                    }
                    userInfo2 = userInfo3;
                    packageManager3 = packageManager;
                }
            }
            i = i2 + 1;
            size = i3;
            packageManager2 = packageManager3;
        }
        return arrayList;
    }

    public void onResult(Bundle bundle) {
        ArrayList parcelableArrayList = bundle.getParcelableArrayList("requests");
        pushProcessedQuotas(parcelableArrayList);
        writeXmlToFile(parcelableArrayList);
    }

    private void pushProcessedQuotas(List<CacheQuotaHint> list) {
        int size = list.size();
        for (int i = 0; i < size; i++) {
            CacheQuotaHint cacheQuotaHint = list.get(i);
            long quota = cacheQuotaHint.getQuota();
            if (quota != -1) {
                try {
                    int uid = cacheQuotaHint.getUid();
                    this.mInstaller.setAppQuota(cacheQuotaHint.getVolumeUuid(), UserHandle.getUserId(uid), UserHandle.getAppId(uid), quota);
                    insertIntoQuotaMap(cacheQuotaHint.getVolumeUuid(), UserHandle.getUserId(uid), UserHandle.getAppId(uid), quota);
                } catch (Installer.InstallerException e) {
                    Slog.w(TAG, "Failed to set cache quota for " + cacheQuotaHint.getUid(), e);
                }
            }
        }
        disconnectService();
    }

    private void insertIntoQuotaMap(String str, int i, int i2, long j) {
        SparseLongArray sparseLongArray = this.mQuotaMap.get(str);
        if (sparseLongArray == null) {
            sparseLongArray = new SparseLongArray();
            this.mQuotaMap.put(str, sparseLongArray);
        }
        sparseLongArray.put(UserHandle.getUid(i, i2), j);
    }

    private void disconnectService() {
        if (this.mServiceConnection != null) {
            this.mContext.unbindService(this.mServiceConnection);
            this.mServiceConnection = null;
        }
    }

    private ComponentName getServiceComponentName() {
        String servicesSystemSharedLibraryPackageName = this.mContext.getPackageManager().getServicesSystemSharedLibraryPackageName();
        if (servicesSystemSharedLibraryPackageName == null) {
            Slog.w(TAG, "could not access the cache quota service: no package!");
            return null;
        }
        Intent intent = new Intent("android.app.usage.CacheQuotaService");
        intent.setPackage(servicesSystemSharedLibraryPackageName);
        ResolveInfo resolveInfoResolveService = this.mContext.getPackageManager().resolveService(intent, 132);
        if (resolveInfoResolveService == null || resolveInfoResolveService.serviceInfo == null) {
            Slog.w(TAG, "No valid components found.");
            return null;
        }
        ServiceInfo serviceInfo = resolveInfoResolveService.serviceInfo;
        return new ComponentName(serviceInfo.packageName, serviceInfo.name);
    }

    private void writeXmlToFile(List<CacheQuotaHint> list) {
        FileOutputStream fileOutputStreamStartWrite;
        FastXmlSerializer fastXmlSerializer;
        try {
            fastXmlSerializer = new FastXmlSerializer();
            fileOutputStreamStartWrite = this.mPreviousValuesFile.startWrite();
        } catch (Exception e) {
            e = e;
            fileOutputStreamStartWrite = null;
        }
        try {
            fastXmlSerializer.setOutput(fileOutputStreamStartWrite, StandardCharsets.UTF_8.name());
            saveToXml(fastXmlSerializer, list, 0L);
            this.mPreviousValuesFile.finishWrite(fileOutputStreamStartWrite);
        } catch (Exception e2) {
            e = e2;
            Slog.e(TAG, "An error occurred while writing the cache quota file.", e);
            this.mPreviousValuesFile.failWrite(fileOutputStreamStartWrite);
        }
    }

    public long setupQuotasFromFile() throws IOException {
        try {
            try {
                Pair<Long, List<CacheQuotaHint>> fromXml = readFromXml(this.mPreviousValuesFile.openRead());
                if (fromXml == null) {
                    Slog.e(TAG, "An error occurred while parsing the cache quota file.");
                    return -1L;
                }
                pushProcessedQuotas((List) fromXml.second);
                return ((Long) fromXml.first).longValue();
            } catch (XmlPullParserException e) {
                throw new IllegalStateException(e.getMessage());
            }
        } catch (FileNotFoundException e2) {
            return -1L;
        }
    }

    @VisibleForTesting
    static void saveToXml(XmlSerializer xmlSerializer, List<CacheQuotaHint> list, long j) throws IOException {
        xmlSerializer.startDocument(null, true);
        xmlSerializer.startTag(null, CACHE_INFO_TAG);
        int size = list.size();
        xmlSerializer.attribute(null, ATTR_PREVIOUS_BYTES, Long.toString(j));
        for (int i = 0; i < size; i++) {
            CacheQuotaHint cacheQuotaHint = list.get(i);
            xmlSerializer.startTag(null, TAG_QUOTA);
            if (cacheQuotaHint.getVolumeUuid() != null) {
                xmlSerializer.attribute(null, ATTR_UUID, cacheQuotaHint.getVolumeUuid());
            }
            xmlSerializer.attribute(null, "uid", Integer.toString(cacheQuotaHint.getUid()));
            xmlSerializer.attribute(null, ATTR_QUOTA_IN_BYTES, Long.toString(cacheQuotaHint.getQuota()));
            xmlSerializer.endTag(null, TAG_QUOTA);
        }
        xmlSerializer.endTag(null, CACHE_INFO_TAG);
        xmlSerializer.endDocument();
    }

    protected static Pair<Long, List<CacheQuotaHint>> readFromXml(InputStream inputStream) throws XmlPullParserException, IOException {
        XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
        xmlPullParserNewPullParser.setInput(inputStream, StandardCharsets.UTF_8.name());
        int eventType = xmlPullParserNewPullParser.getEventType();
        while (eventType != 2 && eventType != 1) {
            eventType = xmlPullParserNewPullParser.next();
        }
        if (eventType == 1) {
            Slog.d(TAG, "No quotas found in quota file.");
            return null;
        }
        if (!CACHE_INFO_TAG.equals(xmlPullParserNewPullParser.getName())) {
            throw new IllegalStateException("Invalid starting tag.");
        }
        ArrayList arrayList = new ArrayList();
        try {
            long j = Long.parseLong(xmlPullParserNewPullParser.getAttributeValue(null, ATTR_PREVIOUS_BYTES));
            int next = xmlPullParserNewPullParser.next();
            do {
                if (next == 2 && TAG_QUOTA.equals(xmlPullParserNewPullParser.getName())) {
                    CacheQuotaHint requestFromXml = getRequestFromXml(xmlPullParserNewPullParser);
                    if (requestFromXml != null) {
                        arrayList.add(requestFromXml);
                        next = xmlPullParserNewPullParser.next();
                    }
                } else {
                    next = xmlPullParserNewPullParser.next();
                }
            } while (next != 1);
            return new Pair<>(Long.valueOf(j), arrayList);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Previous bytes formatted incorrectly; aborting quota read.");
        }
    }

    @VisibleForTesting
    static CacheQuotaHint getRequestFromXml(XmlPullParser xmlPullParser) {
        try {
            String attributeValue = xmlPullParser.getAttributeValue(null, ATTR_UUID);
            int i = Integer.parseInt(xmlPullParser.getAttributeValue(null, "uid"));
            return new CacheQuotaHint.Builder().setVolumeUuid(attributeValue).setUid(i).setQuota(Long.parseLong(xmlPullParser.getAttributeValue(null, ATTR_QUOTA_IN_BYTES))).build();
        } catch (NumberFormatException e) {
            Slog.e(TAG, "Invalid cache quota request, skipping.");
            return null;
        }
    }
}
