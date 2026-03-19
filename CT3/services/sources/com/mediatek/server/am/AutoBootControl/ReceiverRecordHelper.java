package com.mediatek.server.am.AutoBootControl;

import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.ParceledListSlice;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.os.Environment;
import android.os.FileObserver;
import android.os.IUserManager;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.AtomicFile;
import android.util.Log;
import android.util.Xml;
import com.android.internal.util.XmlUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

class ReceiverRecordHelper {
    private static final String CLIENT_PKGNAME = "com.mediatek.security";
    public static final boolean DEFAULT_STATUS = true;
    private static final String FILE_EXTENSION = ".xml";
    private static final String FILE_NAME = "bootreceiver";
    public static boolean SUPPORT_SYSTEM_APP = false;
    private static final String TAG = "ReceiverRecordHelper";
    private BootReceiverPolicy mBootReceiverPolicy;
    private Context mContext;
    private AtomicFile mFile;
    private FileChangeListener mFileChangeListener;
    private IPackageManager mPm;
    private IUserManager mUm;
    private Map<Integer, Map<String, ReceiverRecord>> mBootReceiverList = new HashMap();
    private Map<Integer, List<String>> mPendingSettings = new HashMap();
    private boolean mReady = false;

    public ReceiverRecordHelper(Context context, IUserManager um, IPackageManager pm) {
        this.mContext = null;
        this.mFile = null;
        this.mUm = null;
        this.mPm = null;
        this.mBootReceiverPolicy = null;
        this.mFileChangeListener = null;
        File dataDir = Environment.getDataDirectory();
        File clientDataDir = new File(dataDir, "data/com.mediatek.security");
        File storeDir = new File(clientDataDir, "bootreceiver.xml");
        this.mFile = new AtomicFile(storeDir);
        this.mContext = context;
        this.mUm = um;
        this.mPm = pm;
        this.mBootReceiverPolicy = BootReceiverPolicy.getInstance(this.mContext);
        Log.d(TAG, "storeDir = " + clientDataDir.getPath());
        this.mFileChangeListener = new FileChangeListener(clientDataDir.getPath());
    }

    public void initReceiverList() {
        enforceBasicBootPolicy();
        List<UserInfo> userList = getUserList();
        synchronized (this.mBootReceiverList) {
            for (int i = 0; i < userList.size(); i++) {
                initReceiverCache(userList.get(i).id, false);
            }
            loadDataFromFileToCache();
        }
        this.mReady = true;
        if (this.mFileChangeListener == null) {
            return;
        }
        this.mFileChangeListener.startWatching();
    }

    public void initReceiverCache(int userId) {
        initReceiverCache(userId, false);
    }

    private void initReceiverCache(int userId, boolean readFile) {
        Log.d(TAG, "initReceiverCache() at User(" + userId + ")");
        List<String> list = getPackageListReceivingSpecifiedIntent(userId);
        Map<String, ReceiverRecord> receiverList = new HashMap<>();
        synchronized (this.mBootReceiverList) {
            this.mBootReceiverList.put(Integer.valueOf(userId), receiverList);
        }
        for (int i = 0; i < list.size(); i++) {
            String packageName = list.get(i);
            receiverList.put(packageName, new ReceiverRecord(packageName, true));
            Log.d(TAG, "initReceiverCache() packageName: " + packageName);
        }
        if (!readFile) {
            return;
        }
        loadDataFromFileToCache();
    }

    public List<String> getPackageListReceivingSpecifiedIntent(int userId) {
        List<ResolveInfo> receivers;
        List<String> bootReceivers = new ArrayList<>();
        List<String> policy = this.mBootReceiverPolicy.getBootPolicy();
        for (int i = 0; i < policy.size(); i++) {
            Intent intent = new Intent(policy.get(i));
            try {
                Log.d(TAG, "getPackageListReceivingSpecifiedIntent() find activities receiving intent = " + intent.getAction());
                ParceledListSlice<ResolveInfo> parceledList = this.mPm.queryIntentReceivers(intent, (String) null, 268436480, userId);
                if (parceledList != null && (receivers = parceledList.getList()) != null) {
                    for (int j = 0; j < receivers.size(); j++) {
                        ResolveInfo info = receivers.get(j);
                        String packageName = info.activityInfo != null ? info.activityInfo.packageName : null;
                        if ((SUPPORT_SYSTEM_APP || !isSystemApp(userId, packageName)) && packageName != null && !bootReceivers.contains(packageName)) {
                            Log.d(TAG, "getPackageListReceivingSpecifiedIntent() add " + packageName + " in the list");
                            bootReceivers.add(packageName);
                        }
                    }
                }
            } catch (RemoteException e) {
            }
        }
        return bootReceivers;
    }

    private void enforceBasicBootPolicy() {
        List<String> policy = this.mBootReceiverPolicy.getBootPolicy();
        boolean valid = true;
        if (!policy.contains("android.intent.action.BOOT_COMPLETED") || !policy.contains("android.intent.action.ACTION_BOOT_IPO")) {
            valid = false;
        }
        if (valid) {
        } else {
            throw new RuntimeException("Should NOT remove basic boot policy!");
        }
    }

    public boolean getReceiverDataEnabled(int userId, String packageName) {
        ReceiverRecord data;
        if (this.mReady) {
            Map<String, ReceiverRecord> receiverList = getBootReceiverListByUser(userId);
            if (receiverList != null && receiverList.containsKey(packageName) && (data = receiverList.get(packageName)) != null) {
                return data.enabled;
            }
            return true;
        }
        Log.e(TAG, "getReceiverDataEnabled() not ready!");
        return true;
    }

    private boolean setReceiverRecord(int userId, String packageName, boolean enable) {
        Map<String, ReceiverRecord> receiverList = getBootReceiverListByUser(userId);
        if (receiverList == null || !receiverList.containsKey(packageName)) {
            return false;
        }
        ReceiverRecord data = receiverList.get(packageName);
        data.enabled = enable;
        return true;
    }

    private void addReceiverRecord(int userId, String packageName, boolean enabled) {
        Log.d(TAG, "addReceiverRecord() with " + packageName + " at User(" + userId + ") enabled: " + enabled);
        Map<String, ReceiverRecord> receiverList = getBootReceiverListByUser(userId);
        if (receiverList == null) {
            receiverList = new HashMap<>();
            synchronized (this.mBootReceiverList) {
                this.mBootReceiverList.put(Integer.valueOf(userId), receiverList);
            }
        }
        receiverList.put(packageName, new ReceiverRecord(packageName, enabled));
    }

    private boolean isPendingSetting(int userId, String packageName) {
        synchronized (this.mPendingSettings) {
            List<String> settings = this.mPendingSettings.get(Integer.valueOf(userId));
            if (settings != null && settings.contains(packageName)) {
                Log.d(TAG, "Found a pending setting for pkg: " + packageName + " at User(" + userId + ")");
                return true;
            }
            return false;
        }
    }

    private void removePendingSetting(int userId, String packageName) {
        synchronized (this.mPendingSettings) {
            List<String> settings = this.mPendingSettings.get(Integer.valueOf(userId));
            if (settings != null && settings.contains(packageName)) {
                settings.remove(packageName);
            }
        }
    }

    public void loadDataFromFileToCache() {
        XmlPullParser parser;
        int type;
        Log.d(TAG, "loadDataFromFileToCache()");
        synchronized (this.mFile) {
            try {
                FileInputStream stream = this.mFile.openRead();
                try {
                    try {
                        try {
                            try {
                                try {
                                    try {
                                        parser = Xml.newPullParser();
                                        parser.setInput(stream, null);
                                        do {
                                            type = parser.next();
                                            if (type == 2) {
                                                break;
                                            }
                                        } while (type != 1);
                                    } catch (IllegalStateException e) {
                                        Log.w(TAG, "Failed parsing " + e);
                                        if (0 == 0) {
                                            synchronized (this.mBootReceiverList) {
                                                this.mBootReceiverList.clear();
                                            }
                                        }
                                        try {
                                            stream.close();
                                        } catch (IOException e2) {
                                            throw new RuntimeException("Fail to read receiver list");
                                        }
                                    }
                                } catch (IndexOutOfBoundsException e3) {
                                    Log.w(TAG, "Failed parsing " + e3);
                                    if (0 == 0) {
                                        synchronized (this.mBootReceiverList) {
                                            this.mBootReceiverList.clear();
                                        }
                                    }
                                    try {
                                        stream.close();
                                    } catch (IOException e4) {
                                        throw new RuntimeException("Fail to read receiver list");
                                    }
                                }
                            } catch (IOException e5) {
                                Log.w(TAG, "Failed parsing " + e5);
                                if (0 == 0) {
                                    synchronized (this.mBootReceiverList) {
                                        this.mBootReceiverList.clear();
                                    }
                                }
                                try {
                                    stream.close();
                                } catch (IOException e6) {
                                    throw new RuntimeException("Fail to read receiver list");
                                }
                            }
                        } catch (NullPointerException e7) {
                            Log.w(TAG, "Failed parsing " + e7);
                            if (0 == 0) {
                                synchronized (this.mBootReceiverList) {
                                    this.mBootReceiverList.clear();
                                }
                            }
                            try {
                                stream.close();
                            } catch (IOException e8) {
                                throw new RuntimeException("Fail to read receiver list");
                            }
                        }
                    } catch (NumberFormatException e9) {
                        Log.w(TAG, "Failed parsing " + e9);
                        if (0 == 0) {
                            synchronized (this.mBootReceiverList) {
                                this.mBootReceiverList.clear();
                            }
                        }
                        try {
                            stream.close();
                        } catch (IOException e10) {
                            throw new RuntimeException("Fail to read receiver list");
                        }
                    } catch (XmlPullParserException e11) {
                        Log.w(TAG, "Failed parsing " + e11);
                        if (0 == 0) {
                            synchronized (this.mBootReceiverList) {
                                this.mBootReceiverList.clear();
                            }
                        }
                        try {
                            stream.close();
                        } catch (IOException e12) {
                            throw new RuntimeException("Fail to read receiver list");
                        }
                    }
                    if (type != 2) {
                        throw new IllegalStateException("no start tag found");
                    }
                    int outerDepth = parser.getDepth();
                    while (true) {
                        int type2 = parser.next();
                        if (type2 == 1 || (type2 == 3 && parser.getDepth() <= outerDepth)) {
                            break;
                        }
                        if (type2 != 3 && type2 != 4) {
                            String tagName = parser.getName();
                            if (tagName.equals("pkg")) {
                                String pkgName = parser.getAttributeValue(null, "n");
                                int userId = Integer.parseInt(parser.getAttributeValue(null, "u"));
                                boolean enabled = Boolean.parseBoolean(parser.getAttributeValue(null, "e"));
                                Log.d(TAG, "Read package name: " + pkgName + " enabled: " + enabled + " at User(" + userId + ")");
                                if (setReceiverRecord(userId, pkgName, enabled)) {
                                    continue;
                                } else {
                                    Log.w(TAG, "Found a pending settings for package: " + pkgName);
                                    synchronized (this.mPendingSettings) {
                                        try {
                                            if (!this.mPendingSettings.containsKey(Integer.valueOf(userId))) {
                                                try {
                                                    this.mPendingSettings.put(Integer.valueOf(userId), new ArrayList<>());
                                                } catch (Throwable th) {
                                                    th = th;
                                                    throw th;
                                                }
                                            }
                                            List<String> pendingSettings = this.mPendingSettings.get(Integer.valueOf(userId));
                                            if (!pendingSettings.contains(pkgName)) {
                                                pendingSettings.add(pkgName);
                                            }
                                        } catch (Throwable th2) {
                                            th = th2;
                                        }
                                    }
                                }
                            } else {
                                Log.w(TAG, "Unknown element under <boot-receiver>: " + parser.getName());
                                XmlUtils.skipCurrentTag(parser);
                            }
                        }
                    }
                    if (1 == 0) {
                        synchronized (this.mBootReceiverList) {
                            this.mBootReceiverList.clear();
                        }
                    }
                    try {
                        stream.close();
                    } catch (IOException e13) {
                        throw new RuntimeException("Fail to read receiver list");
                    }
                } catch (Throwable th3) {
                    if (0 != 0) {
                        stream.close();
                        throw th3;
                    }
                    synchronized (this.mBootReceiverList) {
                        this.mBootReceiverList.clear();
                        try {
                            stream.close();
                            throw th3;
                        } catch (IOException e14) {
                            throw new RuntimeException("Fail to read receiver list");
                        }
                    }
                }
            } catch (FileNotFoundException e15) {
                Log.i(TAG, "No existing " + this.mFile.getBaseFile() + "; starting empty");
                resetAllReceiverRecords();
            }
        }
    }

    private List<UserInfo> getUserList() {
        try {
            List<UserInfo> list = this.mUm.getUsers(false);
            return list;
        } catch (RemoteException e) {
            Log.e(TAG, "getUserList() failed!", e);
            return null;
        }
    }

    private PackageInfo getPackageInfoByUser(int userId, String packageName) {
        try {
            PackageInfo packageInfo = this.mPm.getPackageInfo(packageName, 4096, userId);
            return packageInfo;
        } catch (RemoteException e) {
            Log.e(TAG, "getPackageInfoByUser() failed! with userId: " + userId, e);
            return null;
        }
    }

    private boolean isSystemApp(int userId, String packageName) {
        PackageInfo pkgInfo = getPackageInfoByUser(userId, packageName);
        if (pkgInfo != null && pkgInfo.applicationInfo != null) {
            int appId = UserHandle.getAppId(pkgInfo.applicationInfo.uid);
            return (pkgInfo.applicationInfo.flags & 1) != 0 || appId == 1000;
        }
        Log.d(TAG, "isSystemApp() return false with null packageName");
        return false;
    }

    private Map<String, ReceiverRecord> getBootReceiverListByUser(int userId) {
        synchronized (this.mBootReceiverList) {
            if (this.mBootReceiverList.containsKey(Integer.valueOf(userId))) {
                return this.mBootReceiverList.get(Integer.valueOf(userId));
            }
            return null;
        }
    }

    private class FileChangeListener extends FileObserver {
        public FileChangeListener(String path) {
            super(path);
        }

        @Override
        public void onEvent(int event, String path) {
            if (path == null || !path.equals("bootreceiver.xml")) {
                return;
            }
            switch (event) {
                case 2:
                case 8:
                case 256:
                case 512:
                    Log.d(ReceiverRecordHelper.TAG, "FileChangeListener.onEvent(), event = " + event + ", reload the file.");
                    ReceiverRecordHelper.this.loadDataFromFileToCache();
                    break;
            }
        }
    }

    public void updateReceiverCache() {
        Log.d(TAG, "updateReceiverCache()");
        List<UserInfo> userList = getUserList();
        if (userList == null) {
            return;
        }
        for (UserInfo user : userList) {
            updateReceiverCache(user.id);
        }
    }

    void updateReceiverCache(int userId) {
        Log.d(TAG, "updateReceiverCache() at User(" + userId + ")");
        List<ReceiverRecord> oldList = getReceiverList(userId);
        List<String> updateList = getPackageListReceivingSpecifiedIntent(userId);
        for (int i = 0; i < updateList.size(); i++) {
            boolean enabled = true;
            boolean found = false;
            String packageName = updateList.get(i);
            int j = 0;
            while (true) {
                if (j >= oldList.size()) {
                    break;
                }
                ReceiverRecord record = oldList.get(j);
                if (record == null || !packageName.equals(record.packageName)) {
                    j++;
                } else {
                    enabled = record.enabled;
                    found = true;
                    break;
                }
            }
            if (found) {
                setReceiverRecord(userId, packageName, enabled);
            } else if (isPendingSetting(userId, packageName)) {
                addReceiverRecord(userId, packageName, false);
                removePendingSetting(userId, packageName);
            } else {
                addReceiverRecord(userId, packageName, true);
            }
        }
    }

    private List<ReceiverRecord> getReceiverList(int userId) {
        List<ReceiverRecord> res = new ArrayList<>();
        Map<String, ReceiverRecord> data = getBootReceiverListByUser(userId);
        if (data != null) {
            for (String pkgName : data.keySet()) {
                res.add(new ReceiverRecord(data.get(pkgName)));
            }
        }
        return res;
    }

    void resetAllReceiverRecords() {
        Log.d(TAG, "resetAllReceiverRecords()");
        List<UserInfo> userList = getUserList();
        if (userList == null) {
            return;
        }
        for (UserInfo user : userList) {
            int userId = user.id;
            List<ReceiverRecord> curList = getReceiverList(userId);
            for (int index = 0; index < curList.size(); index++) {
                ReceiverRecord record = curList.get(index);
                if (record != null && !record.enabled) {
                    Log.d(TAG, "resetAllReceiverRecords() - found pkg = " + record.packageName + " to be reset");
                    record.enabled = true;
                }
            }
        }
    }
}
