package com.android.server.timezone;

import android.util.AtomicFile;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.FastXmlSerializer;
import com.android.server.backup.BackupManagerConstants;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

final class PackageStatusStorage {
    private static final String ATTRIBUTE_CHECK_STATUS = "checkStatus";
    private static final String ATTRIBUTE_DATA_APP_VERSION = "dataAppPackageVersion";
    private static final String ATTRIBUTE_OPTIMISTIC_LOCK_ID = "optimisticLockId";
    private static final String ATTRIBUTE_UPDATE_APP_VERSION = "updateAppPackageVersion";
    private static final String LOG_TAG = "timezone.PackageStatusStorage";
    private static final String TAG_PACKAGE_STATUS = "PackageStatus";
    private static final long UNKNOWN_PACKAGE_VERSION = -1;
    private final AtomicFile mPackageStatusFile;

    PackageStatusStorage(File file) {
        this.mPackageStatusFile = new AtomicFile(new File(file, "package-status.xml"), "timezone-status");
    }

    void initialize() throws IOException {
        if (!this.mPackageStatusFile.getBaseFile().exists()) {
            insertInitialPackageStatus();
        }
    }

    void deleteFileForTests() {
        synchronized (this) {
            this.mPackageStatusFile.delete();
        }
    }

    PackageStatus getPackageStatus() {
        PackageStatus packageStatusLocked;
        synchronized (this) {
            try {
                try {
                    packageStatusLocked = getPackageStatusLocked();
                } catch (ParseException e) {
                    Slog.e(LOG_TAG, "Package status invalid, resetting and retrying", e);
                    recoverFromBadData(e);
                    try {
                        return getPackageStatusLocked();
                    } catch (ParseException e2) {
                        throw new IllegalStateException("Recovery from bad file failed", e2);
                    }
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return packageStatusLocked;
    }

    @GuardedBy("this")
    private PackageStatus getPackageStatusLocked() throws Exception {
        try {
            FileInputStream fileInputStreamOpenRead = this.mPackageStatusFile.openRead();
            Throwable th = null;
            try {
                Integer nullableIntAttribute = getNullableIntAttribute(parseToPackageStatusTag(fileInputStreamOpenRead), ATTRIBUTE_CHECK_STATUS);
                if (nullableIntAttribute == null) {
                    return null;
                }
                PackageStatus packageStatus = new PackageStatus(nullableIntAttribute.intValue(), new PackageVersions(getIntAttribute(r2, ATTRIBUTE_UPDATE_APP_VERSION), getIntAttribute(r2, ATTRIBUTE_DATA_APP_VERSION)));
                if (fileInputStreamOpenRead != null) {
                    $closeResource(null, fileInputStreamOpenRead);
                }
                return packageStatus;
            } finally {
                if (fileInputStreamOpenRead != null) {
                }
            }
            if (fileInputStreamOpenRead != null) {
                $closeResource(th, fileInputStreamOpenRead);
            }
        } catch (IOException e) {
            ParseException parseException = new ParseException("Error reading package status", 0);
            parseException.initCause(e);
            throw parseException;
        }
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    @GuardedBy("this")
    private int recoverFromBadData(Exception exc) {
        this.mPackageStatusFile.delete();
        try {
            return insertInitialPackageStatus();
        } catch (IOException e) {
            IllegalStateException illegalStateException = new IllegalStateException(e);
            illegalStateException.addSuppressed(exc);
            throw illegalStateException;
        }
    }

    private int insertInitialPackageStatus() throws IOException {
        int iCurrentTimeMillis = (int) System.currentTimeMillis();
        writePackageStatusLocked(null, iCurrentTimeMillis, null);
        return iCurrentTimeMillis;
    }

    CheckToken generateCheckToken(PackageVersions packageVersions) {
        int iRecoverFromBadData;
        CheckToken checkToken;
        if (packageVersions == null) {
            throw new NullPointerException("currentInstalledVersions == null");
        }
        synchronized (this) {
            try {
                iRecoverFromBadData = getCurrentOptimisticLockId();
            } catch (ParseException e) {
                Slog.w(LOG_TAG, "Unable to find optimistic lock ID from package status");
                iRecoverFromBadData = recoverFromBadData(e);
            }
            int i = iRecoverFromBadData + 1;
            try {
                if (!writePackageStatusWithOptimisticLockCheck(iRecoverFromBadData, i, 1, packageVersions)) {
                    throw new IllegalStateException("Unable to update status to CHECK_STARTED. synchronization failure?");
                }
                checkToken = new CheckToken(i, packageVersions);
            } catch (IOException e2) {
                throw new IllegalStateException(e2);
            }
        }
        return checkToken;
    }

    void resetCheckState() {
        int iRecoverFromBadData;
        synchronized (this) {
            try {
                iRecoverFromBadData = getCurrentOptimisticLockId();
            } catch (ParseException e) {
                Slog.w(LOG_TAG, "resetCheckState: Unable to find optimistic lock ID from package status");
                iRecoverFromBadData = recoverFromBadData(e);
            }
            int i = iRecoverFromBadData + 1;
            try {
                if (!writePackageStatusWithOptimisticLockCheck(iRecoverFromBadData, i, null, null)) {
                    throw new IllegalStateException("resetCheckState: Unable to reset package status, newOptimisticLockId=" + i);
                }
            } catch (IOException e2) {
                throw new IllegalStateException(e2);
            }
        }
    }

    boolean markChecked(CheckToken checkToken, boolean z) {
        boolean zWritePackageStatusWithOptimisticLockCheck;
        synchronized (this) {
            int i = checkToken.mOptimisticLockId;
            try {
                zWritePackageStatusWithOptimisticLockCheck = writePackageStatusWithOptimisticLockCheck(i, i + 1, Integer.valueOf(z ? 2 : 3), checkToken.mPackageVersions);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return zWritePackageStatusWithOptimisticLockCheck;
    }

    @GuardedBy("this")
    private int getCurrentOptimisticLockId() throws Exception {
        try {
            FileInputStream fileInputStreamOpenRead = this.mPackageStatusFile.openRead();
            Throwable th = null;
            try {
                return getIntAttribute(parseToPackageStatusTag(fileInputStreamOpenRead), ATTRIBUTE_OPTIMISTIC_LOCK_ID);
            } finally {
                if (fileInputStreamOpenRead != null) {
                    $closeResource(th, fileInputStreamOpenRead);
                }
            }
        } catch (IOException e) {
            ParseException parseException = new ParseException("Unable to read file", 0);
            parseException.initCause(e);
            throw parseException;
        }
    }

    private static XmlPullParser parseToPackageStatusTag(FileInputStream fileInputStream) throws ParseException {
        try {
            XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
            xmlPullParserNewPullParser.setInput(fileInputStream, StandardCharsets.UTF_8.name());
            while (true) {
                int next = xmlPullParserNewPullParser.next();
                if (next != 1) {
                    String name = xmlPullParserNewPullParser.getName();
                    if (next == 2 && TAG_PACKAGE_STATUS.equals(name)) {
                        return xmlPullParserNewPullParser;
                    }
                } else {
                    throw new ParseException("Unable to find PackageStatus tag", 0);
                }
            }
        } catch (IOException e) {
            ParseException parseException = new ParseException("Error reading XML", 0);
            e.initCause(e);
            throw parseException;
        } catch (XmlPullParserException e2) {
            throw new IllegalStateException("Unable to configure parser", e2);
        }
    }

    @GuardedBy("this")
    private boolean writePackageStatusWithOptimisticLockCheck(int i, int i2, Integer num, PackageVersions packageVersions) throws IOException {
        try {
            if (getCurrentOptimisticLockId() != i) {
                return false;
            }
            writePackageStatusLocked(num, i2, packageVersions);
            return true;
        } catch (ParseException e) {
            recoverFromBadData(e);
            return false;
        }
    }

    @GuardedBy("this")
    private void writePackageStatusLocked(Integer num, int i, PackageVersions packageVersions) throws IOException {
        FileOutputStream fileOutputStreamStartWrite;
        long j;
        if ((num == null) != (packageVersions == null)) {
            throw new IllegalArgumentException("Provide both status and packageVersions, or neither.");
        }
        try {
            fileOutputStreamStartWrite = this.mPackageStatusFile.startWrite();
        } catch (IOException e) {
            e = e;
            fileOutputStreamStartWrite = null;
        }
        try {
            FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(fileOutputStreamStartWrite, StandardCharsets.UTF_8.name());
            fastXmlSerializer.startDocument(null, true);
            fastXmlSerializer.startTag(null, TAG_PACKAGE_STATUS);
            fastXmlSerializer.attribute(null, ATTRIBUTE_CHECK_STATUS, num == null ? BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS : Integer.toString(num.intValue()));
            fastXmlSerializer.attribute(null, ATTRIBUTE_OPTIMISTIC_LOCK_ID, Integer.toString(i));
            long j2 = -1;
            if (num != null) {
                j = packageVersions.mUpdateAppVersion;
            } else {
                j = -1;
            }
            fastXmlSerializer.attribute(null, ATTRIBUTE_UPDATE_APP_VERSION, Long.toString(j));
            if (num != null) {
                j2 = packageVersions.mDataAppVersion;
            }
            fastXmlSerializer.attribute(null, ATTRIBUTE_DATA_APP_VERSION, Long.toString(j2));
            fastXmlSerializer.endTag(null, TAG_PACKAGE_STATUS);
            fastXmlSerializer.endDocument();
            fastXmlSerializer.flush();
            this.mPackageStatusFile.finishWrite(fileOutputStreamStartWrite);
        } catch (IOException e2) {
            e = e2;
            if (fileOutputStreamStartWrite != null) {
                this.mPackageStatusFile.failWrite(fileOutputStreamStartWrite);
            }
            throw e;
        }
    }

    public void forceCheckStateForTests(int i, PackageVersions packageVersions) throws IOException {
        synchronized (this) {
            try {
                try {
                    writePackageStatusLocked(Integer.valueOf(i), (int) System.currentTimeMillis(), packageVersions);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private static Integer getNullableIntAttribute(XmlPullParser xmlPullParser, String str) throws ParseException {
        String attributeValue = xmlPullParser.getAttributeValue(null, str);
        try {
            if (attributeValue == null) {
                throw new ParseException("Attribute " + str + " missing", 0);
            }
            if (attributeValue.isEmpty()) {
                return null;
            }
            return Integer.valueOf(Integer.parseInt(attributeValue));
        } catch (NumberFormatException e) {
            throw new ParseException("Bad integer for attributeName=" + str + ": " + attributeValue, 0);
        }
    }

    private static int getIntAttribute(XmlPullParser xmlPullParser, String str) throws ParseException {
        Integer nullableIntAttribute = getNullableIntAttribute(xmlPullParser, str);
        if (nullableIntAttribute == null) {
            throw new ParseException("Missing attribute " + str, 0);
        }
        return nullableIntAttribute.intValue();
    }

    public void dump(PrintWriter printWriter) {
        printWriter.println("Package status: " + getPackageStatus());
    }
}
