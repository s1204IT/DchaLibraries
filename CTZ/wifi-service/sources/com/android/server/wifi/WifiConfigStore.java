package com.android.server.wifi;

import android.app.AlarmManager;
import android.content.Context;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Xml;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.AtomicFile;
import com.android.internal.util.FastXmlSerializer;
import com.android.server.wifi.util.XmlUtil;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class WifiConfigStore {
    private static final int BUFFERED_WRITE_ALARM_INTERVAL_MS = 10000;

    @VisibleForTesting
    public static final String BUFFERED_WRITE_ALARM_TAG = "WriteBufferAlarm";
    private static final int CURRENT_CONFIG_STORE_DATA_VERSION = 1;
    private static final int INITIAL_CONFIG_STORE_DATA_VERSION = 1;
    private static final String STORE_DIRECTORY_NAME = "wifi";
    private static final String STORE_FILE_NAME = "WifiConfigStore.xml";
    private static final String TAG = "WifiConfigStore";
    private static final String XML_TAG_DOCUMENT_HEADER = "WifiConfigStoreData";
    private static final String XML_TAG_VERSION = "Version";
    private final AlarmManager mAlarmManager;
    private final Clock mClock;
    private final Handler mEventHandler;
    private StoreFile mSharedStore;
    private boolean mVerboseLoggingEnabled = false;
    private boolean mBufferedWritePending = false;
    private final AlarmManager.OnAlarmListener mBufferedWriteListener = new AlarmManager.OnAlarmListener() {
        @Override
        public void onAlarm() {
            try {
                WifiConfigStore.this.writeBufferedData();
            } catch (IOException e) {
                Log.wtf(WifiConfigStore.TAG, "Buffered write failed", e);
            }
        }
    };
    private final Map<String, StoreData> mStoreDataList = new HashMap();
    private StoreFile mUserStore = null;

    public interface StoreData {
        void deserializeData(XmlPullParser xmlPullParser, int i, boolean z) throws XmlPullParserException, IOException;

        String getName();

        void resetData(boolean z);

        void serializeData(XmlSerializer xmlSerializer, boolean z) throws XmlPullParserException, IOException;

        boolean supportShareData();
    }

    public WifiConfigStore(Context context, Looper looper, Clock clock, StoreFile storeFile) {
        this.mAlarmManager = (AlarmManager) context.getSystemService("alarm");
        this.mEventHandler = new Handler(looper);
        this.mClock = clock;
        this.mSharedStore = storeFile;
    }

    public void setUserStore(StoreFile storeFile) {
        this.mUserStore = storeFile;
    }

    public boolean registerStoreData(StoreData storeData) {
        if (storeData == null) {
            Log.e(TAG, "Unable to register null store data");
            return false;
        }
        this.mStoreDataList.put(storeData.getName(), storeData);
        return true;
    }

    private static StoreFile createFile(File file) {
        File file2 = new File(file, STORE_DIRECTORY_NAME);
        if (!file2.exists() && !file2.mkdir()) {
            Log.w(TAG, "Could not create store directory " + file2);
        }
        return new StoreFile(new File(file2, STORE_FILE_NAME));
    }

    public static StoreFile createSharedFile() {
        return createFile(Environment.getDataMiscDirectory());
    }

    public static StoreFile createUserFile(int i) {
        return createFile(Environment.getDataMiscCeDirectory(i));
    }

    public void enableVerboseLogging(boolean z) {
        this.mVerboseLoggingEnabled = z;
    }

    public boolean areStoresPresent() {
        return this.mSharedStore.exists() || (this.mUserStore != null && this.mUserStore.exists());
    }

    public void write(boolean z) throws XmlPullParserException, IOException {
        this.mSharedStore.storeRawDataToWrite(serializeData(true));
        if (this.mUserStore != null) {
            this.mUserStore.storeRawDataToWrite(serializeData(false));
        }
        if (z) {
            writeBufferedData();
        } else {
            startBufferedWriteAlarm();
        }
    }

    private byte[] serializeData(boolean z) throws XmlPullParserException, IOException {
        XmlSerializer fastXmlSerializer = new FastXmlSerializer();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        fastXmlSerializer.setOutput(byteArrayOutputStream, StandardCharsets.UTF_8.name());
        XmlUtil.writeDocumentStart(fastXmlSerializer, XML_TAG_DOCUMENT_HEADER);
        XmlUtil.writeNextValue(fastXmlSerializer, XML_TAG_VERSION, 1);
        for (Map.Entry<String, StoreData> entry : this.mStoreDataList.entrySet()) {
            String key = entry.getKey();
            StoreData value = entry.getValue();
            if (!z || value.supportShareData()) {
                XmlUtil.writeNextSectionStart(fastXmlSerializer, key);
                value.serializeData(fastXmlSerializer, z);
                XmlUtil.writeNextSectionEnd(fastXmlSerializer, key);
            }
        }
        XmlUtil.writeDocumentEnd(fastXmlSerializer, XML_TAG_DOCUMENT_HEADER);
        return byteArrayOutputStream.toByteArray();
    }

    private void startBufferedWriteAlarm() {
        if (!this.mBufferedWritePending) {
            this.mAlarmManager.set(2, this.mClock.getElapsedSinceBootMillis() + 10000, BUFFERED_WRITE_ALARM_TAG, this.mBufferedWriteListener, this.mEventHandler);
            this.mBufferedWritePending = true;
        }
    }

    private void stopBufferedWriteAlarm() {
        if (this.mBufferedWritePending) {
            this.mAlarmManager.cancel(this.mBufferedWriteListener);
            this.mBufferedWritePending = false;
        }
    }

    private void writeBufferedData() throws IOException {
        stopBufferedWriteAlarm();
        long elapsedSinceBootMillis = this.mClock.getElapsedSinceBootMillis();
        this.mSharedStore.writeBufferedRawData();
        if (this.mUserStore != null) {
            this.mUserStore.writeBufferedRawData();
        }
        Log.d(TAG, "Writing to stores completed in " + (this.mClock.getElapsedSinceBootMillis() - elapsedSinceBootMillis) + " ms.");
    }

    public void read() throws XmlPullParserException, IOException {
        resetStoreData(true);
        if (this.mUserStore != null) {
            resetStoreData(false);
        }
        long elapsedSinceBootMillis = this.mClock.getElapsedSinceBootMillis();
        byte[] rawData = this.mSharedStore.readRawData();
        byte[] rawData2 = null;
        if (this.mUserStore != null) {
            rawData2 = this.mUserStore.readRawData();
        }
        Log.d(TAG, "Reading from stores completed in " + (this.mClock.getElapsedSinceBootMillis() - elapsedSinceBootMillis) + " ms.");
        deserializeData(rawData, true);
        if (this.mUserStore != null) {
            deserializeData(rawData2, false);
        }
    }

    public void switchUserStoreAndRead(StoreFile storeFile) throws XmlPullParserException, IOException {
        resetStoreData(false);
        stopBufferedWriteAlarm();
        this.mUserStore = storeFile;
        long elapsedSinceBootMillis = this.mClock.getElapsedSinceBootMillis();
        byte[] rawData = this.mUserStore.readRawData();
        Log.d(TAG, "Reading from user store completed in " + (this.mClock.getElapsedSinceBootMillis() - elapsedSinceBootMillis) + " ms.");
        deserializeData(rawData, false);
    }

    private void resetStoreData(boolean z) {
        Iterator<Map.Entry<String, StoreData>> it = this.mStoreDataList.entrySet().iterator();
        while (it.hasNext()) {
            it.next().getValue().resetData(z);
        }
    }

    private void indicateNoDataForStoreDatas(Collection<StoreData> collection, boolean z) throws XmlPullParserException, IOException {
        Iterator<StoreData> it = collection.iterator();
        while (it.hasNext()) {
            it.next().deserializeData(null, 0, z);
        }
    }

    private void deserializeData(byte[] bArr, boolean z) throws XmlPullParserException, IOException {
        if (bArr == null) {
            indicateNoDataForStoreDatas(this.mStoreDataList.values(), z);
            return;
        }
        XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
        xmlPullParserNewPullParser.setInput(new ByteArrayInputStream(bArr), StandardCharsets.UTF_8.name());
        int depth = xmlPullParserNewPullParser.getDepth() + 1;
        parseDocumentStartAndVersionFromXml(xmlPullParserNewPullParser);
        String[] strArr = new String[1];
        HashSet hashSet = new HashSet();
        while (XmlUtil.gotoNextSectionOrEnd(xmlPullParserNewPullParser, strArr, depth)) {
            StoreData storeData = this.mStoreDataList.get(strArr[0]);
            if (storeData == null) {
                throw new XmlPullParserException("Unknown store data: " + strArr[0]);
            }
            storeData.deserializeData(xmlPullParserNewPullParser, depth + 1, z);
            hashSet.add(storeData);
        }
        HashSet hashSet2 = new HashSet(this.mStoreDataList.values());
        hashSet2.removeAll(hashSet);
        indicateNoDataForStoreDatas(hashSet2, z);
    }

    private static int parseDocumentStartAndVersionFromXml(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        XmlUtil.gotoDocumentStart(xmlPullParser, XML_TAG_DOCUMENT_HEADER);
        int iIntValue = ((Integer) XmlUtil.readNextValueWithName(xmlPullParser, XML_TAG_VERSION)).intValue();
        if (iIntValue < 1 || iIntValue > 1) {
            throw new XmlPullParserException("Invalid version of data: " + iIntValue);
        }
        return iIntValue;
    }

    public static class StoreFile {
        private static final int FILE_MODE = 384;
        private final AtomicFile mAtomicFile;
        private String mFileName;
        private byte[] mWriteData;

        public StoreFile(File file) {
            this.mAtomicFile = new AtomicFile(file);
            this.mFileName = this.mAtomicFile.getBaseFile().getAbsolutePath();
        }

        public boolean exists() {
            return this.mAtomicFile.exists();
        }

        public byte[] readRawData() throws IOException {
            try {
                return this.mAtomicFile.readFully();
            } catch (FileNotFoundException e) {
                return null;
            }
        }

        public void storeRawDataToWrite(byte[] bArr) {
            this.mWriteData = bArr;
        }

        public void writeBufferedRawData() throws IOException {
            FileOutputStream fileOutputStreamStartWrite;
            IOException e;
            if (this.mWriteData == null) {
                Log.w(WifiConfigStore.TAG, "No data stored for writing to file: " + this.mFileName);
                return;
            }
            try {
                fileOutputStreamStartWrite = this.mAtomicFile.startWrite();
            } catch (IOException e2) {
                fileOutputStreamStartWrite = null;
                e = e2;
            }
            try {
                FileUtils.setPermissions(this.mFileName, FILE_MODE, -1, -1);
                fileOutputStreamStartWrite.write(this.mWriteData);
                this.mAtomicFile.finishWrite(fileOutputStreamStartWrite);
                this.mWriteData = null;
            } catch (IOException e3) {
                e = e3;
                if (fileOutputStreamStartWrite != null) {
                    this.mAtomicFile.failWrite(fileOutputStreamStartWrite);
                }
                throw e;
            }
        }
    }
}
