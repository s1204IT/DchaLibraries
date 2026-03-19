package android.app.backup;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.XmlResourceParser;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.system.ErrnoException;
import android.system.Os;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.TelephonyIntents;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class FullBackup {
    public static final String APK_TREE_TOKEN = "a";
    public static final String APPS_PREFIX = "apps/";
    public static final String CACHE_TREE_TOKEN = "c";
    public static final String CONF_TOKEN_INTENT_EXTRA = "conftoken";
    public static final String DATABASE_TREE_TOKEN = "db";
    public static final String DEVICE_CACHE_TREE_TOKEN = "d_c";
    public static final String DEVICE_DATABASE_TREE_TOKEN = "d_db";
    public static final String DEVICE_FILES_TREE_TOKEN = "d_f";
    public static final String DEVICE_NO_BACKUP_TREE_TOKEN = "d_nb";
    public static final String DEVICE_ROOT_TREE_TOKEN = "d_r";
    public static final String DEVICE_SHAREDPREFS_TREE_TOKEN = "d_sp";
    public static final String FILES_TREE_TOKEN = "f";
    public static final String FLAG_REQUIRED_CLIENT_SIDE_ENCRYPTION = "clientSideEncryption";
    public static final String FLAG_REQUIRED_DEVICE_TO_DEVICE_TRANSFER = "deviceToDeviceTransfer";
    public static final String FLAG_REQUIRED_FAKE_CLIENT_SIDE_ENCRYPTION = "fakeClientSideEncryption";
    public static final String FULL_BACKUP_INTENT_ACTION = "fullback";
    public static final String FULL_RESTORE_INTENT_ACTION = "fullrest";
    public static final String KEY_VALUE_DATA_TOKEN = "k";
    public static final String MANAGED_EXTERNAL_TREE_TOKEN = "ef";
    public static final String NO_BACKUP_TREE_TOKEN = "nb";
    public static final String OBB_TREE_TOKEN = "obb";
    public static final String ROOT_TREE_TOKEN = "r";
    public static final String SHAREDPREFS_TREE_TOKEN = "sp";
    public static final String SHARED_PREFIX = "shared/";
    public static final String SHARED_STORAGE_TOKEN = "shared";
    static final String TAG = "FullBackup";
    static final String TAG_XML_PARSER = "BackupXmlParserLogging";
    private static final Map<String, BackupScheme> kPackageBackupSchemeMap = new ArrayMap();

    public static native int backupToTar(String str, String str2, String str3, String str4, String str5, FullBackupDataOutput fullBackupDataOutput);

    static synchronized BackupScheme getBackupScheme(Context context) {
        BackupScheme backupScheme;
        backupScheme = kPackageBackupSchemeMap.get(context.getPackageName());
        if (backupScheme == null) {
            backupScheme = new BackupScheme(context);
            kPackageBackupSchemeMap.put(context.getPackageName(), backupScheme);
        }
        return backupScheme;
    }

    public static BackupScheme getBackupSchemeForTest(Context context) {
        BackupScheme backupScheme = new BackupScheme(context);
        backupScheme.mExcludes = new ArraySet<>();
        backupScheme.mIncludes = new ArrayMap();
        return backupScheme;
    }

    public static void restoreFile(ParcelFileDescriptor parcelFileDescriptor, long j, int i, long j2, long j3, File file) throws IOException {
        FileOutputStream fileOutputStream;
        long j4 = 0;
        if (i == 2) {
            if (file != null) {
                file.mkdirs();
            }
        } else {
            if (file != null) {
                try {
                    File parentFile = file.getParentFile();
                    if (!parentFile.exists()) {
                        parentFile.mkdirs();
                    }
                    fileOutputStream = new FileOutputStream(file);
                } catch (IOException e) {
                    Log.e(TAG, "Unable to create/open file " + file.getPath(), e);
                    fileOutputStream = null;
                }
            } else {
                fileOutputStream = null;
            }
            byte[] bArr = new byte[32768];
            FileInputStream fileInputStream = new FileInputStream(parcelFileDescriptor.getFileDescriptor());
            long j5 = j;
            FileOutputStream fileOutputStream2 = fileOutputStream;
            while (true) {
                if (j5 <= j4) {
                    break;
                }
                int i2 = fileInputStream.read(bArr, 0, j5 > ((long) bArr.length) ? bArr.length : (int) j5);
                if (i2 <= 0) {
                    Log.w(TAG, "Incomplete read: expected " + j5 + " but got " + (j - j5));
                    break;
                }
                if (fileOutputStream2 != null) {
                    try {
                        fileOutputStream2.write(bArr, 0, i2);
                    } catch (IOException e2) {
                        Log.e(TAG, "Unable to write to file " + file.getPath(), e2);
                        fileOutputStream2.close();
                        file.delete();
                        fileOutputStream2 = null;
                    }
                }
                j5 -= (long) i2;
                j4 = 0;
            }
            if (fileOutputStream2 != null) {
                fileOutputStream2.close();
            }
        }
        if (j2 >= 0 && file != null) {
            try {
                Os.chmod(file.getPath(), (int) (j2 & 448));
            } catch (ErrnoException e3) {
                e3.rethrowAsIOException();
            }
            file.setLastModified(j3);
        }
    }

    @VisibleForTesting
    public static class BackupScheme {
        private static final String TAG_EXCLUDE = "exclude";
        private static final String TAG_INCLUDE = "include";
        private final File CACHE_DIR;
        private final File DATABASE_DIR;
        private final File DEVICE_CACHE_DIR;
        private final File DEVICE_DATABASE_DIR;
        private final File DEVICE_FILES_DIR;
        private final File DEVICE_NOBACKUP_DIR;
        private final File DEVICE_ROOT_DIR;
        private final File DEVICE_SHAREDPREF_DIR;
        private final File EXTERNAL_DIR;
        private final File FILES_DIR;
        private final File NOBACKUP_DIR;
        private final File ROOT_DIR;
        private final File SHAREDPREF_DIR;
        ArraySet<PathWithRequiredFlags> mExcludes;
        final int mFullBackupContent;
        Map<String, Set<PathWithRequiredFlags>> mIncludes;
        final PackageManager mPackageManager;
        final String mPackageName;
        final StorageManager mStorageManager;
        private StorageVolume[] mVolumes = null;

        String tokenToDirectoryPath(String str) {
            try {
                if (str.equals(FullBackup.FILES_TREE_TOKEN)) {
                    return this.FILES_DIR.getCanonicalPath();
                }
                if (str.equals(FullBackup.DATABASE_TREE_TOKEN)) {
                    return this.DATABASE_DIR.getCanonicalPath();
                }
                if (str.equals(FullBackup.ROOT_TREE_TOKEN)) {
                    return this.ROOT_DIR.getCanonicalPath();
                }
                if (str.equals(FullBackup.SHAREDPREFS_TREE_TOKEN)) {
                    return this.SHAREDPREF_DIR.getCanonicalPath();
                }
                if (str.equals(FullBackup.CACHE_TREE_TOKEN)) {
                    return this.CACHE_DIR.getCanonicalPath();
                }
                if (str.equals(FullBackup.NO_BACKUP_TREE_TOKEN)) {
                    return this.NOBACKUP_DIR.getCanonicalPath();
                }
                if (str.equals(FullBackup.DEVICE_FILES_TREE_TOKEN)) {
                    return this.DEVICE_FILES_DIR.getCanonicalPath();
                }
                if (str.equals(FullBackup.DEVICE_DATABASE_TREE_TOKEN)) {
                    return this.DEVICE_DATABASE_DIR.getCanonicalPath();
                }
                if (str.equals(FullBackup.DEVICE_ROOT_TREE_TOKEN)) {
                    return this.DEVICE_ROOT_DIR.getCanonicalPath();
                }
                if (str.equals(FullBackup.DEVICE_SHAREDPREFS_TREE_TOKEN)) {
                    return this.DEVICE_SHAREDPREF_DIR.getCanonicalPath();
                }
                if (str.equals(FullBackup.DEVICE_CACHE_TREE_TOKEN)) {
                    return this.DEVICE_CACHE_DIR.getCanonicalPath();
                }
                if (str.equals(FullBackup.DEVICE_NO_BACKUP_TREE_TOKEN)) {
                    return this.DEVICE_NOBACKUP_DIR.getCanonicalPath();
                }
                if (str.equals(FullBackup.MANAGED_EXTERNAL_TREE_TOKEN)) {
                    if (this.EXTERNAL_DIR == null) {
                        return null;
                    }
                    return this.EXTERNAL_DIR.getCanonicalPath();
                }
                if (str.startsWith(FullBackup.SHARED_PREFIX)) {
                    return sharedDomainToPath(str);
                }
                Log.i(FullBackup.TAG, "Unrecognized domain " + str);
                return null;
            } catch (Exception e) {
                Log.i(FullBackup.TAG, "Error reading directory for domain: " + str);
                return null;
            }
        }

        private String sharedDomainToPath(String str) throws IOException {
            String strSubstring = str.substring(FullBackup.SHARED_PREFIX.length());
            StorageVolume[] volumeList = getVolumeList();
            int i = Integer.parseInt(strSubstring);
            if (i < this.mVolumes.length) {
                return volumeList[i].getPathFile().getCanonicalPath();
            }
            return null;
        }

        private StorageVolume[] getVolumeList() {
            if (this.mStorageManager != null) {
                if (this.mVolumes == null) {
                    this.mVolumes = this.mStorageManager.getVolumeList();
                }
            } else {
                Log.e(FullBackup.TAG, "Unable to access Storage Manager");
            }
            return this.mVolumes;
        }

        public static class PathWithRequiredFlags {
            private final String mPath;
            private final int mRequiredFlags;

            public PathWithRequiredFlags(String str, int i) {
                this.mPath = str;
                this.mRequiredFlags = i;
            }

            public String getPath() {
                return this.mPath;
            }

            public int getRequiredFlags() {
                return this.mRequiredFlags;
            }
        }

        BackupScheme(Context context) {
            this.mFullBackupContent = context.getApplicationInfo().fullBackupContent;
            this.mStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
            this.mPackageManager = context.getPackageManager();
            this.mPackageName = context.getPackageName();
            Context contextCreateCredentialProtectedStorageContext = context.createCredentialProtectedStorageContext();
            this.FILES_DIR = contextCreateCredentialProtectedStorageContext.getFilesDir();
            this.DATABASE_DIR = contextCreateCredentialProtectedStorageContext.getDatabasePath("foo").getParentFile();
            this.ROOT_DIR = contextCreateCredentialProtectedStorageContext.getDataDir();
            this.SHAREDPREF_DIR = contextCreateCredentialProtectedStorageContext.getSharedPreferencesPath("foo").getParentFile();
            this.CACHE_DIR = contextCreateCredentialProtectedStorageContext.getCacheDir();
            this.NOBACKUP_DIR = contextCreateCredentialProtectedStorageContext.getNoBackupFilesDir();
            Context contextCreateDeviceProtectedStorageContext = context.createDeviceProtectedStorageContext();
            this.DEVICE_FILES_DIR = contextCreateDeviceProtectedStorageContext.getFilesDir();
            this.DEVICE_DATABASE_DIR = contextCreateDeviceProtectedStorageContext.getDatabasePath("foo").getParentFile();
            this.DEVICE_ROOT_DIR = contextCreateDeviceProtectedStorageContext.getDataDir();
            this.DEVICE_SHAREDPREF_DIR = contextCreateDeviceProtectedStorageContext.getSharedPreferencesPath("foo").getParentFile();
            this.DEVICE_CACHE_DIR = contextCreateDeviceProtectedStorageContext.getCacheDir();
            this.DEVICE_NOBACKUP_DIR = contextCreateDeviceProtectedStorageContext.getNoBackupFilesDir();
            if (Process.myUid() != 1000) {
                this.EXTERNAL_DIR = context.getExternalFilesDir(null);
            } else {
                this.EXTERNAL_DIR = null;
            }
        }

        boolean isFullBackupContentEnabled() {
            if (this.mFullBackupContent < 0) {
                if (Log.isLoggable(FullBackup.TAG_XML_PARSER, 2)) {
                    Log.v(FullBackup.TAG_XML_PARSER, "android:fullBackupContent - \"false\"");
                    return false;
                }
                return false;
            }
            return true;
        }

        public synchronized Map<String, Set<PathWithRequiredFlags>> maybeParseAndGetCanonicalIncludePaths() throws XmlPullParserException, IOException {
            if (this.mIncludes == null) {
                maybeParseBackupSchemeLocked();
            }
            return this.mIncludes;
        }

        public synchronized ArraySet<PathWithRequiredFlags> maybeParseAndGetCanonicalExcludePaths() throws XmlPullParserException, IOException {
            if (this.mExcludes == null) {
                maybeParseBackupSchemeLocked();
            }
            return this.mExcludes;
        }

        private void maybeParseBackupSchemeLocked() throws Throwable {
            XmlResourceParser xml;
            this.mIncludes = new ArrayMap();
            this.mExcludes = new ArraySet<>();
            if (this.mFullBackupContent == 0) {
                if (Log.isLoggable(FullBackup.TAG_XML_PARSER, 2)) {
                    Log.v(FullBackup.TAG_XML_PARSER, "android:fullBackupContent - \"true\"");
                    return;
                }
                return;
            }
            if (Log.isLoggable(FullBackup.TAG_XML_PARSER, 2)) {
                Log.v(FullBackup.TAG_XML_PARSER, "android:fullBackupContent - found xml resource");
            }
            XmlResourceParser xmlResourceParser = null;
            try {
                try {
                    xml = this.mPackageManager.getResourcesForApplication(this.mPackageName).getXml(this.mFullBackupContent);
                } catch (PackageManager.NameNotFoundException e) {
                    e = e;
                }
            } catch (Throwable th) {
                th = th;
            }
            try {
                parseBackupSchemeFromXmlLocked(xml, this.mExcludes, this.mIncludes);
                if (xml != null) {
                    xml.close();
                }
            } catch (PackageManager.NameNotFoundException e2) {
                e = e2;
                xmlResourceParser = xml;
                throw new IOException(e);
            } catch (Throwable th2) {
                th = th2;
                xmlResourceParser = xml;
                if (xmlResourceParser != null) {
                    xmlResourceParser.close();
                }
                throw th;
            }
        }

        @VisibleForTesting
        public void parseBackupSchemeFromXmlLocked(XmlPullParser xmlPullParser, Set<PathWithRequiredFlags> set, Map<String, Set<PathWithRequiredFlags>> map) throws XmlPullParserException, IOException {
            int eventType = xmlPullParser.getEventType();
            while (eventType != 2) {
                eventType = xmlPullParser.next();
            }
            if ("full-backup-content".equals(xmlPullParser.getName())) {
                if (Log.isLoggable(FullBackup.TAG_XML_PARSER, 2)) {
                    Log.v(FullBackup.TAG_XML_PARSER, "\n");
                    Log.v(FullBackup.TAG_XML_PARSER, "====================================================");
                    Log.v(FullBackup.TAG_XML_PARSER, "Found valid fullBackupContent; parsing xml resource.");
                    Log.v(FullBackup.TAG_XML_PARSER, "====================================================");
                    Log.v(FullBackup.TAG_XML_PARSER, "");
                }
                while (true) {
                    int next = xmlPullParser.next();
                    if (next == 1) {
                        break;
                    }
                    if (next == 2) {
                        validateInnerTagContents(xmlPullParser);
                        String attributeValue = xmlPullParser.getAttributeValue(null, TelephonyIntents.EXTRA_DOMAIN);
                        File directoryForCriteriaDomain = getDirectoryForCriteriaDomain(attributeValue);
                        if (directoryForCriteriaDomain == null) {
                            if (Log.isLoggable(FullBackup.TAG_XML_PARSER, 2)) {
                                Log.v(FullBackup.TAG_XML_PARSER, "...parsing \"" + xmlPullParser.getName() + "\": domain=\"" + attributeValue + "\" invalid; skipping");
                            }
                        } else {
                            File fileExtractCanonicalFile = extractCanonicalFile(directoryForCriteriaDomain, xmlPullParser.getAttributeValue(null, "path"));
                            if (fileExtractCanonicalFile != null) {
                                int requiredFlagsFromString = 0;
                                if (TAG_INCLUDE.equals(xmlPullParser.getName())) {
                                    requiredFlagsFromString = getRequiredFlagsFromString(xmlPullParser.getAttributeValue(null, "requireFlags"));
                                }
                                Set<PathWithRequiredFlags> currentTagForDomain = parseCurrentTagForDomain(xmlPullParser, set, map, attributeValue);
                                currentTagForDomain.add(new PathWithRequiredFlags(fileExtractCanonicalFile.getCanonicalPath(), requiredFlagsFromString));
                                if (Log.isLoggable(FullBackup.TAG_XML_PARSER, 2)) {
                                    Log.v(FullBackup.TAG_XML_PARSER, "...parsed " + fileExtractCanonicalFile.getCanonicalPath() + " for domain \"" + attributeValue + "\", requiredFlags + \"" + requiredFlagsFromString + "\"");
                                }
                                if ("database".equals(attributeValue) && !fileExtractCanonicalFile.isDirectory()) {
                                    String str = fileExtractCanonicalFile.getCanonicalPath() + "-journal";
                                    currentTagForDomain.add(new PathWithRequiredFlags(str, requiredFlagsFromString));
                                    if (Log.isLoggable(FullBackup.TAG_XML_PARSER, 2)) {
                                        Log.v(FullBackup.TAG_XML_PARSER, "...automatically generated " + str + ". Ignore if nonexistent.");
                                    }
                                    String str2 = fileExtractCanonicalFile.getCanonicalPath() + "-wal";
                                    currentTagForDomain.add(new PathWithRequiredFlags(str2, requiredFlagsFromString));
                                    if (Log.isLoggable(FullBackup.TAG_XML_PARSER, 2)) {
                                        Log.v(FullBackup.TAG_XML_PARSER, "...automatically generated " + str2 + ". Ignore if nonexistent.");
                                    }
                                }
                                if ("sharedpref".equals(attributeValue) && !fileExtractCanonicalFile.isDirectory() && !fileExtractCanonicalFile.getCanonicalPath().endsWith(".xml")) {
                                    String str3 = fileExtractCanonicalFile.getCanonicalPath() + ".xml";
                                    currentTagForDomain.add(new PathWithRequiredFlags(str3, requiredFlagsFromString));
                                    if (Log.isLoggable(FullBackup.TAG_XML_PARSER, 2)) {
                                        Log.v(FullBackup.TAG_XML_PARSER, "...automatically generated " + str3 + ". Ignore if nonexistent.");
                                    }
                                }
                            }
                        }
                    }
                }
                if (Log.isLoggable(FullBackup.TAG_XML_PARSER, 2)) {
                    Log.v(FullBackup.TAG_XML_PARSER, "\n");
                    Log.v(FullBackup.TAG_XML_PARSER, "Xml resource parsing complete.");
                    Log.v(FullBackup.TAG_XML_PARSER, "Final tally.");
                    Log.v(FullBackup.TAG_XML_PARSER, "Includes:");
                    if (map.isEmpty()) {
                        Log.v(FullBackup.TAG_XML_PARSER, "  ...nothing specified (This means the entirety of app data minus excludes)");
                    } else {
                        for (Map.Entry<String, Set<PathWithRequiredFlags>> entry : map.entrySet()) {
                            Log.v(FullBackup.TAG_XML_PARSER, "  domain=" + entry.getKey());
                            for (PathWithRequiredFlags pathWithRequiredFlags : entry.getValue()) {
                                Log.v(FullBackup.TAG_XML_PARSER, " path: " + pathWithRequiredFlags.getPath() + " requiredFlags: " + pathWithRequiredFlags.getRequiredFlags());
                            }
                        }
                    }
                    Log.v(FullBackup.TAG_XML_PARSER, "Excludes:");
                    if (set.isEmpty()) {
                        Log.v(FullBackup.TAG_XML_PARSER, "  ...nothing to exclude.");
                    } else {
                        for (PathWithRequiredFlags pathWithRequiredFlags2 : set) {
                            Log.v(FullBackup.TAG_XML_PARSER, " path: " + pathWithRequiredFlags2.getPath() + " requiredFlags: " + pathWithRequiredFlags2.getRequiredFlags());
                        }
                    }
                    Log.v(FullBackup.TAG_XML_PARSER, "  ");
                    Log.v(FullBackup.TAG_XML_PARSER, "====================================================");
                    Log.v(FullBackup.TAG_XML_PARSER, "\n");
                    return;
                }
                return;
            }
            throw new XmlPullParserException("Xml file didn't start with correct tag (<full-backup-content>). Found \"" + xmlPullParser.getName() + "\"");
        }

        private int getRequiredFlagsFromString(String str) {
            if (str == null || str.length() == 0) {
                return 0;
            }
            int i = 0;
            for (String str2 : str.split("\\|")) {
                byte b = -1;
                int iHashCode = str2.hashCode();
                if (iHashCode != 482744282) {
                    if (iHashCode != 1499007205) {
                        if (iHashCode == 1935925810 && str2.equals(FullBackup.FLAG_REQUIRED_DEVICE_TO_DEVICE_TRANSFER)) {
                            b = 1;
                        }
                    } else if (str2.equals(FullBackup.FLAG_REQUIRED_CLIENT_SIDE_ENCRYPTION)) {
                        b = 0;
                    }
                } else if (str2.equals(FullBackup.FLAG_REQUIRED_FAKE_CLIENT_SIDE_ENCRYPTION)) {
                    b = 2;
                }
                switch (b) {
                    case 0:
                        i |= 1;
                        continue;
                        break;
                    case 1:
                        i |= 2;
                        continue;
                        break;
                    case 2:
                        i |= Integer.MIN_VALUE;
                        break;
                }
                Log.w(FullBackup.TAG, "Unrecognized requiredFlag provided, value: \"" + str2 + "\"");
            }
            return i;
        }

        private Set<PathWithRequiredFlags> parseCurrentTagForDomain(XmlPullParser xmlPullParser, Set<PathWithRequiredFlags> set, Map<String, Set<PathWithRequiredFlags>> map, String str) throws XmlPullParserException {
            if (TAG_INCLUDE.equals(xmlPullParser.getName())) {
                String tokenForXmlDomain = getTokenForXmlDomain(str);
                Set<PathWithRequiredFlags> set2 = map.get(tokenForXmlDomain);
                if (set2 == null) {
                    ArraySet arraySet = new ArraySet();
                    map.put(tokenForXmlDomain, arraySet);
                    return arraySet;
                }
                return set2;
            }
            if (TAG_EXCLUDE.equals(xmlPullParser.getName())) {
                return set;
            }
            if (Log.isLoggable(FullBackup.TAG_XML_PARSER, 2)) {
                Log.v(FullBackup.TAG_XML_PARSER, "Invalid tag found in xml \"" + xmlPullParser.getName() + "\"; aborting operation.");
            }
            throw new XmlPullParserException("Unrecognised tag in backup criteria xml (" + xmlPullParser.getName() + ")");
        }

        private String getTokenForXmlDomain(String str) {
            if ("root".equals(str)) {
                return FullBackup.ROOT_TREE_TOKEN;
            }
            if (ContentResolver.SCHEME_FILE.equals(str)) {
                return FullBackup.FILES_TREE_TOKEN;
            }
            if ("database".equals(str)) {
                return FullBackup.DATABASE_TREE_TOKEN;
            }
            if ("sharedpref".equals(str)) {
                return FullBackup.SHAREDPREFS_TREE_TOKEN;
            }
            if ("device_root".equals(str)) {
                return FullBackup.DEVICE_ROOT_TREE_TOKEN;
            }
            if ("device_file".equals(str)) {
                return FullBackup.DEVICE_FILES_TREE_TOKEN;
            }
            if ("device_database".equals(str)) {
                return FullBackup.DEVICE_DATABASE_TREE_TOKEN;
            }
            if ("device_sharedpref".equals(str)) {
                return FullBackup.DEVICE_SHAREDPREFS_TREE_TOKEN;
            }
            if ("external".equals(str)) {
                return FullBackup.MANAGED_EXTERNAL_TREE_TOKEN;
            }
            return null;
        }

        private File extractCanonicalFile(File file, String str) {
            if (str == null) {
                str = "";
            }
            if (str.contains("..")) {
                if (Log.isLoggable(FullBackup.TAG_XML_PARSER, 2)) {
                    Log.v(FullBackup.TAG_XML_PARSER, "...resolved \"" + file.getPath() + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + str + "\", but the \"..\" path is not permitted; skipping.");
                }
                return null;
            }
            if (str.contains("//")) {
                if (Log.isLoggable(FullBackup.TAG_XML_PARSER, 2)) {
                    Log.v(FullBackup.TAG_XML_PARSER, "...resolved \"" + file.getPath() + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + str + "\", which contains the invalid \"//\" sequence; skipping.");
                }
                return null;
            }
            return new File(file, str);
        }

        private File getDirectoryForCriteriaDomain(String str) {
            if (TextUtils.isEmpty(str)) {
                return null;
            }
            if (ContentResolver.SCHEME_FILE.equals(str)) {
                return this.FILES_DIR;
            }
            if ("database".equals(str)) {
                return this.DATABASE_DIR;
            }
            if ("root".equals(str)) {
                return this.ROOT_DIR;
            }
            if ("sharedpref".equals(str)) {
                return this.SHAREDPREF_DIR;
            }
            if ("device_file".equals(str)) {
                return this.DEVICE_FILES_DIR;
            }
            if ("device_database".equals(str)) {
                return this.DEVICE_DATABASE_DIR;
            }
            if ("device_root".equals(str)) {
                return this.DEVICE_ROOT_DIR;
            }
            if ("device_sharedpref".equals(str)) {
                return this.DEVICE_SHAREDPREF_DIR;
            }
            if ("external".equals(str)) {
                return this.EXTERNAL_DIR;
            }
            return null;
        }

        private void validateInnerTagContents(XmlPullParser xmlPullParser) throws XmlPullParserException {
            if (xmlPullParser == null) {
                return;
            }
            String name = xmlPullParser.getName();
            byte b = -1;
            int iHashCode = name.hashCode();
            if (iHashCode != -1321148966) {
                if (iHashCode == 1942574248 && name.equals(TAG_INCLUDE)) {
                    b = 0;
                }
            } else if (name.equals(TAG_EXCLUDE)) {
                b = 1;
            }
            switch (b) {
                case 0:
                    if (xmlPullParser.getAttributeCount() > 3) {
                        throw new XmlPullParserException("At most 3 tag attributes allowed for \"include\" tag (\"domain\" & \"path\" & optional \"requiredFlags\").");
                    }
                    return;
                case 1:
                    if (xmlPullParser.getAttributeCount() > 2) {
                        throw new XmlPullParserException("At most 2 tag attributes allowed for \"exclude\" tag (\"domain\" & \"path\".");
                    }
                    return;
                default:
                    throw new XmlPullParserException("A valid tag is one of \"<include/>\" or \"<exclude/>. You provided \"" + xmlPullParser.getName() + "\"");
            }
        }
    }
}
