package com.android.server.fingerprint;

import android.R;
import android.content.Context;
import android.hardware.fingerprint.Fingerprint;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.annotations.GuardedBy;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

class FingerprintsUserState {
    private static final String ATTR_DEVICE_ID = "deviceId";
    private static final String ATTR_FINGER_ID = "fingerId";
    private static final String ATTR_GROUP_ID = "groupId";
    private static final String ATTR_NAME = "name";
    private static final String FINGERPRINT_FILE = "settings_fingerprint.xml";
    private static final String TAG = "FingerprintState";
    private static final String TAG_FINGERPRINT = "fingerprint";
    private static final String TAG_FINGERPRINTS = "fingerprints";
    private final Context mCtx;
    private final File mFile;

    @GuardedBy("this")
    private final ArrayList<Fingerprint> mFingerprints = new ArrayList<>();
    private final Runnable mWriteStateRunnable = new Runnable() {
        @Override
        public void run() throws Throwable {
            FingerprintsUserState.this.doWriteState();
        }
    };

    public FingerprintsUserState(Context context, int i) {
        this.mFile = getFileForUser(i);
        this.mCtx = context;
        synchronized (this) {
            readStateSyncLocked();
        }
    }

    public void addFingerprint(int i, int i2) {
        synchronized (this) {
            this.mFingerprints.add(new Fingerprint(getUniqueName(), i2, i, 0L));
            scheduleWriteStateLocked();
        }
    }

    public void removeFingerprint(int i) {
        synchronized (this) {
            int i2 = 0;
            while (true) {
                if (i2 >= this.mFingerprints.size()) {
                    break;
                }
                if (this.mFingerprints.get(i2).getFingerId() != i) {
                    i2++;
                } else {
                    this.mFingerprints.remove(i2);
                    scheduleWriteStateLocked();
                    break;
                }
            }
        }
    }

    public void renameFingerprint(int i, CharSequence charSequence) {
        synchronized (this) {
            int i2 = 0;
            while (true) {
                if (i2 >= this.mFingerprints.size()) {
                    break;
                }
                if (this.mFingerprints.get(i2).getFingerId() != i) {
                    i2++;
                } else {
                    Fingerprint fingerprint = this.mFingerprints.get(i2);
                    this.mFingerprints.set(i2, new Fingerprint(charSequence, fingerprint.getGroupId(), fingerprint.getFingerId(), fingerprint.getDeviceId()));
                    scheduleWriteStateLocked();
                    break;
                }
            }
        }
    }

    public List<Fingerprint> getFingerprints() {
        ArrayList<Fingerprint> copy;
        synchronized (this) {
            copy = getCopy(this.mFingerprints);
        }
        return copy;
    }

    private String getUniqueName() {
        int i = 1;
        while (true) {
            String string = this.mCtx.getString(R.string.config_defaultAmbientContextDetectionService, Integer.valueOf(i));
            if (isUnique(string)) {
                return string;
            }
            i++;
        }
    }

    private boolean isUnique(String str) {
        Iterator<Fingerprint> it = this.mFingerprints.iterator();
        while (it.hasNext()) {
            if (it.next().getName().equals(str)) {
                return false;
            }
        }
        return true;
    }

    private static File getFileForUser(int i) {
        return new File(Environment.getUserSystemDirectory(i), FINGERPRINT_FILE);
    }

    private void scheduleWriteStateLocked() {
        AsyncTask.execute(this.mWriteStateRunnable);
    }

    private ArrayList<Fingerprint> getCopy(ArrayList<Fingerprint> arrayList) {
        ArrayList<Fingerprint> arrayList2 = new ArrayList<>(arrayList.size());
        for (int i = 0; i < arrayList.size(); i++) {
            Fingerprint fingerprint = arrayList.get(i);
            arrayList2.add(new Fingerprint(fingerprint.getName(), fingerprint.getGroupId(), fingerprint.getFingerId(), fingerprint.getDeviceId()));
        }
        return arrayList2;
    }

    private void doWriteState() throws Throwable {
        ArrayList<Fingerprint> copy;
        FileOutputStream fileOutputStreamStartWrite;
        AtomicFile atomicFile = new AtomicFile(this.mFile);
        synchronized (this) {
            copy = getCopy(this.mFingerprints);
        }
        FileOutputStream fileOutputStream = null;
        try {
            try {
                fileOutputStreamStartWrite = atomicFile.startWrite();
            } catch (Throwable th) {
                th = th;
                fileOutputStreamStartWrite = null;
            }
        } catch (Throwable th2) {
            th = th2;
        }
        try {
            XmlSerializer xmlSerializerNewSerializer = Xml.newSerializer();
            xmlSerializerNewSerializer.setOutput(fileOutputStreamStartWrite, "utf-8");
            xmlSerializerNewSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            xmlSerializerNewSerializer.startDocument(null, true);
            xmlSerializerNewSerializer.startTag(null, TAG_FINGERPRINTS);
            int size = copy.size();
            for (int i = 0; i < size; i++) {
                Fingerprint fingerprint = copy.get(i);
                xmlSerializerNewSerializer.startTag(null, TAG_FINGERPRINT);
                xmlSerializerNewSerializer.attribute(null, ATTR_FINGER_ID, Integer.toString(fingerprint.getFingerId()));
                xmlSerializerNewSerializer.attribute(null, "name", fingerprint.getName().toString());
                xmlSerializerNewSerializer.attribute(null, ATTR_GROUP_ID, Integer.toString(fingerprint.getGroupId()));
                xmlSerializerNewSerializer.attribute(null, ATTR_DEVICE_ID, Long.toString(fingerprint.getDeviceId()));
                xmlSerializerNewSerializer.endTag(null, TAG_FINGERPRINT);
            }
            xmlSerializerNewSerializer.endTag(null, TAG_FINGERPRINTS);
            xmlSerializerNewSerializer.endDocument();
            atomicFile.finishWrite(fileOutputStreamStartWrite);
            IoUtils.closeQuietly(fileOutputStreamStartWrite);
        } catch (Throwable th3) {
            th = th3;
            IoUtils.closeQuietly(fileOutputStreamStartWrite);
            throw th;
        }
    }

    @GuardedBy("this")
    private void readStateSyncLocked() {
        if (!this.mFile.exists()) {
            return;
        }
        try {
            FileInputStream fileInputStream = new FileInputStream(this.mFile);
            try {
                try {
                    XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                    xmlPullParserNewPullParser.setInput(fileInputStream, null);
                    parseStateLocked(xmlPullParserNewPullParser);
                } catch (IOException | XmlPullParserException e) {
                    throw new IllegalStateException("Failed parsing settings file: " + this.mFile, e);
                }
            } finally {
                IoUtils.closeQuietly(fileInputStream);
            }
        } catch (FileNotFoundException e2) {
            Slog.i(TAG, "No fingerprint state");
        }
    }

    @GuardedBy("this")
    private void parseStateLocked(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next = xmlPullParser.next();
            if (next != 1) {
                if (next != 3 || xmlPullParser.getDepth() > depth) {
                    if (next != 3 && next != 4 && xmlPullParser.getName().equals(TAG_FINGERPRINTS)) {
                        parseFingerprintsLocked(xmlPullParser);
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    @GuardedBy("this")
    private void parseFingerprintsLocked(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next = xmlPullParser.next();
            if (next != 1) {
                if (next != 3 || xmlPullParser.getDepth() > depth) {
                    if (next != 3 && next != 4 && xmlPullParser.getName().equals(TAG_FINGERPRINT)) {
                        this.mFingerprints.add(new Fingerprint(xmlPullParser.getAttributeValue(null, "name"), Integer.parseInt(xmlPullParser.getAttributeValue(null, ATTR_GROUP_ID)), Integer.parseInt(xmlPullParser.getAttributeValue(null, ATTR_FINGER_ID)), Integer.parseInt(xmlPullParser.getAttributeValue(null, ATTR_DEVICE_ID))));
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }
}
