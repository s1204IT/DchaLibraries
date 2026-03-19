package com.android.packageinstaller;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.AtomicFile;
import android.util.Log;
import android.util.SparseArray;
import android.util.Xml;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

class EventResultPersister {
    private static final String LOG_TAG = EventResultPersister.class.getSimpleName();
    private int mCounter;
    private boolean mIsPersistScheduled;
    private boolean mIsPersistingStateValid;
    private final AtomicFile mResultsFile;
    private final Object mLock = new Object();
    private final SparseArray<EventResult> mResults = new SparseArray<>();
    private final SparseArray<EventResultObserver> mObservers = new SparseArray<>();

    interface EventResultObserver {
        void onResult(int i, int i2, String str);
    }

    public int getNewId() throws OutOfIdsException {
        int i;
        synchronized (this.mLock) {
            if (this.mCounter == Integer.MAX_VALUE) {
                throw new OutOfIdsException();
            }
            this.mCounter++;
            writeState();
            i = this.mCounter - 1;
        }
        return i;
    }

    private static void nextElement(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        int next;
        do {
            next = xmlPullParser.next();
            if (next == 2) {
                return;
            }
        } while (next != 1);
    }

    private static int readIntAttribute(XmlPullParser xmlPullParser, String str) {
        return Integer.parseInt(xmlPullParser.getAttributeValue(null, str));
    }

    private static String readStringAttribute(XmlPullParser xmlPullParser, String str) {
        return xmlPullParser.getAttributeValue(null, str);
    }

    EventResultPersister(File file) {
        this.mResultsFile = new AtomicFile(file);
        this.mCounter = -2147483647;
        try {
            FileInputStream fileInputStreamOpenRead = this.mResultsFile.openRead();
            try {
                XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                xmlPullParserNewPullParser.setInput(fileInputStreamOpenRead, StandardCharsets.UTF_8.name());
                nextElement(xmlPullParserNewPullParser);
                while (xmlPullParserNewPullParser.getEventType() != 1) {
                    String name = xmlPullParserNewPullParser.getName();
                    if ("results".equals(name)) {
                        this.mCounter = readIntAttribute(xmlPullParserNewPullParser, "counter");
                    } else if ("result".equals(name)) {
                        int intAttribute = readIntAttribute(xmlPullParserNewPullParser, "id");
                        int intAttribute2 = readIntAttribute(xmlPullParserNewPullParser, "status");
                        int intAttribute3 = readIntAttribute(xmlPullParserNewPullParser, "legacyStatus");
                        String stringAttribute = readStringAttribute(xmlPullParserNewPullParser, "statusMessage");
                        if (this.mResults.get(intAttribute) != null) {
                            throw new Exception("id " + intAttribute + " has two results");
                        }
                        this.mResults.put(intAttribute, new EventResult(intAttribute2, intAttribute3, stringAttribute));
                    } else {
                        throw new Exception("unexpected tag");
                    }
                    nextElement(xmlPullParserNewPullParser);
                }
                if (fileInputStreamOpenRead != null) {
                    fileInputStreamOpenRead.close();
                }
            } finally {
            }
        } catch (Exception e) {
            this.mResults.clear();
            writeState();
        }
    }

    void onEventReceived(Context context, Intent intent) {
        int i = 0;
        int intExtra = intent.getIntExtra("android.content.pm.extra.STATUS", 0);
        if (intExtra == -1) {
            context.startActivity((Intent) intent.getParcelableExtra("android.intent.extra.INTENT"));
            return;
        }
        int intExtra2 = intent.getIntExtra("EventResultPersister.EXTRA_ID", 0);
        String stringExtra = intent.getStringExtra("android.content.pm.extra.STATUS_MESSAGE");
        int intExtra3 = intent.getIntExtra("android.content.pm.extra.LEGACY_STATUS", 0);
        EventResultObserver eventResultObserverValueAt = null;
        synchronized (this.mLock) {
            int size = this.mObservers.size();
            while (true) {
                if (i >= size) {
                    break;
                }
                if (this.mObservers.keyAt(i) != intExtra2) {
                    i++;
                } else {
                    eventResultObserverValueAt = this.mObservers.valueAt(i);
                    this.mObservers.removeAt(i);
                    break;
                }
            }
            if (eventResultObserverValueAt != null) {
                eventResultObserverValueAt.onResult(intExtra, intExtra3, stringExtra);
            } else {
                this.mResults.put(intExtra2, new EventResult(intExtra, intExtra3, stringExtra));
                writeState();
            }
        }
    }

    private void writeState() {
        synchronized (this.mLock) {
            this.mIsPersistingStateValid = false;
            if (!this.mIsPersistScheduled) {
                this.mIsPersistScheduled = true;
                AsyncTask.execute(new Runnable() {
                    @Override
                    public final void run() {
                        EventResultPersister.lambda$writeState$0(this.f$0);
                    }
                });
            }
        }
    }

    public static void lambda$writeState$0(EventResultPersister eventResultPersister) {
        int i;
        SparseArray<EventResult> sparseArrayClone;
        FileOutputStream fileOutputStreamStartWrite;
        while (true) {
            synchronized (eventResultPersister.mLock) {
                i = eventResultPersister.mCounter;
                sparseArrayClone = eventResultPersister.mResults.clone();
                eventResultPersister.mIsPersistingStateValid = true;
            }
            try {
                fileOutputStreamStartWrite = eventResultPersister.mResultsFile.startWrite();
            } catch (IOException e) {
                e = e;
                fileOutputStreamStartWrite = null;
            }
            try {
                XmlSerializer xmlSerializerNewSerializer = Xml.newSerializer();
                xmlSerializerNewSerializer.setOutput(fileOutputStreamStartWrite, StandardCharsets.UTF_8.name());
                xmlSerializerNewSerializer.startDocument(null, true);
                xmlSerializerNewSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
                xmlSerializerNewSerializer.startTag(null, "results");
                xmlSerializerNewSerializer.attribute(null, "counter", Integer.toString(i));
                int size = sparseArrayClone.size();
                for (int i2 = 0; i2 < size; i2++) {
                    xmlSerializerNewSerializer.startTag(null, "result");
                    xmlSerializerNewSerializer.attribute(null, "id", Integer.toString(sparseArrayClone.keyAt(i2)));
                    xmlSerializerNewSerializer.attribute(null, "status", Integer.toString(sparseArrayClone.valueAt(i2).status));
                    xmlSerializerNewSerializer.attribute(null, "legacyStatus", Integer.toString(sparseArrayClone.valueAt(i2).legacyStatus));
                    if (sparseArrayClone.valueAt(i2).message != null) {
                        xmlSerializerNewSerializer.attribute(null, "statusMessage", sparseArrayClone.valueAt(i2).message);
                    }
                    xmlSerializerNewSerializer.endTag(null, "result");
                }
                xmlSerializerNewSerializer.endTag(null, "results");
                xmlSerializerNewSerializer.endDocument();
                eventResultPersister.mResultsFile.finishWrite(fileOutputStreamStartWrite);
            } catch (IOException e2) {
                e = e2;
                if (fileOutputStreamStartWrite != null) {
                    eventResultPersister.mResultsFile.failWrite(fileOutputStreamStartWrite);
                }
                Log.e(LOG_TAG, "error writing results", e);
                eventResultPersister.mResultsFile.delete();
            }
            synchronized (eventResultPersister.mLock) {
                if (eventResultPersister.mIsPersistingStateValid) {
                    eventResultPersister.mIsPersistScheduled = false;
                    return;
                }
            }
        }
    }

    int addObserver(int i, EventResultObserver eventResultObserver) throws OutOfIdsException {
        synchronized (this.mLock) {
            int iIndexOfKey = -1;
            try {
                if (i == Integer.MIN_VALUE) {
                    i = getNewId();
                } else {
                    iIndexOfKey = this.mResults.indexOfKey(i);
                }
                if (iIndexOfKey >= 0) {
                    EventResult eventResultValueAt = this.mResults.valueAt(iIndexOfKey);
                    eventResultObserver.onResult(eventResultValueAt.status, eventResultValueAt.legacyStatus, eventResultValueAt.message);
                    this.mResults.removeAt(iIndexOfKey);
                    writeState();
                } else {
                    this.mObservers.put(i, eventResultObserver);
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return i;
    }

    void removeObserver(int i) {
        synchronized (this.mLock) {
            this.mObservers.delete(i);
        }
    }

    private class EventResult {
        public final int legacyStatus;
        public final String message;
        public final int status;

        private EventResult(int i, int i2, String str) {
            this.status = i;
            this.legacyStatus = i2;
            this.message = str;
        }
    }

    class OutOfIdsException extends Exception {
        OutOfIdsException() {
        }
    }
}
