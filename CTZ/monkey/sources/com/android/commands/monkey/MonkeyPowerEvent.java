package com.android.commands.monkey;

import android.app.IActivityManager;
import android.content.ContentValues;
import android.os.Build;
import android.util.Log;
import android.view.IWindowManager;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class MonkeyPowerEvent extends MonkeyEvent {
    private static final String LOG_FILE = "/sdcard/autotester.log";
    private static final String TAG = "PowerTester";
    private static final String TEST_DELAY_STARTED = "AUTOTEST_TEST_BEGIN_DELAY";
    private static final String TEST_ENDED = "AUTOTEST_TEST_SUCCESS";
    private static final String TEST_IDLE_ENDED = "AUTOTEST_IDLE_SUCCESS";
    private static final String TEST_SEQ_BEGIN = "AUTOTEST_SEQUENCE_BEGIN";
    private static final String TEST_STARTED = "AUTOTEST_TEST_BEGIN";
    private static final long USB_DELAY_TIME = 10000;
    private static ArrayList<ContentValues> mLogEvents = new ArrayList<>();
    private static long mTestStartTime;
    private String mPowerLogTag;
    private String mTestResult;

    public MonkeyPowerEvent(String str, String str2) {
        super(4);
        this.mPowerLogTag = str;
        this.mTestResult = str2;
    }

    public MonkeyPowerEvent(String str) {
        super(4);
        this.mPowerLogTag = str;
        this.mTestResult = null;
    }

    public MonkeyPowerEvent() {
        super(4);
        this.mPowerLogTag = null;
        this.mTestResult = null;
    }

    private void bufferLogEvent(String str, String str2) {
        long jCurrentTimeMillis = System.currentTimeMillis();
        if (str.compareTo(TEST_STARTED) == 0) {
            mTestStartTime = jCurrentTimeMillis;
        } else if (str.compareTo(TEST_IDLE_ENDED) == 0) {
            jCurrentTimeMillis = Long.parseLong(str2) + mTestStartTime;
            str = TEST_ENDED;
        } else if (str.compareTo(TEST_DELAY_STARTED) == 0) {
            mTestStartTime = jCurrentTimeMillis + USB_DELAY_TIME;
            jCurrentTimeMillis = mTestStartTime;
            str = TEST_STARTED;
        }
        ContentValues contentValues = new ContentValues();
        contentValues.put("date", Long.valueOf(jCurrentTimeMillis));
        contentValues.put("tag", str);
        if (str2 != null) {
            contentValues.put("value", str2);
        }
        mLogEvents.add(contentValues);
    }

    private void writeLogEvents() throws Throwable {
        StringBuffer stringBuffer;
        FileWriter fileWriter;
        ContentValues[] contentValuesArr = (ContentValues[]) mLogEvents.toArray(new ContentValues[0]);
        mLogEvents.clear();
        FileWriter fileWriter2 = null;
        try {
            try {
                try {
                    stringBuffer = new StringBuffer();
                    for (ContentValues contentValues : contentValuesArr) {
                        stringBuffer.append(MonkeyUtils.toCalendarTime(contentValues.getAsLong("date").longValue()));
                        stringBuffer.append(contentValues.getAsString("tag"));
                        if (contentValues.containsKey("value")) {
                            String asString = contentValues.getAsString("value");
                            stringBuffer.append(" ");
                            stringBuffer.append(asString.replace('\n', '/'));
                        }
                        stringBuffer.append("\n");
                    }
                    fileWriter = new FileWriter(LOG_FILE, true);
                } catch (IOException e) {
                    e = e;
                }
            } catch (Throwable th) {
                th = th;
            }
            try {
                fileWriter.write(stringBuffer.toString());
                fileWriter.close();
            } catch (IOException e2) {
                fileWriter2 = fileWriter;
                e = e2;
                Log.w(TAG, "Can't write sdcard log file", e);
                if (fileWriter2 != null) {
                    fileWriter2.close();
                }
            } catch (Throwable th2) {
                fileWriter2 = fileWriter;
                th = th2;
                if (fileWriter2 != null) {
                    try {
                        fileWriter2.close();
                    } catch (IOException e3) {
                    }
                }
                throw th;
            }
        } catch (IOException e4) {
        }
    }

    @Override
    public int injectEvent(IWindowManager iWindowManager, IActivityManager iActivityManager, int i) throws Throwable {
        if (this.mPowerLogTag != null) {
            if (this.mPowerLogTag.compareTo(TEST_SEQ_BEGIN) == 0) {
                bufferLogEvent(this.mPowerLogTag, Build.FINGERPRINT);
                return 1;
            }
            if (this.mTestResult != null) {
                bufferLogEvent(this.mPowerLogTag, this.mTestResult);
                return 1;
            }
            return 1;
        }
        writeLogEvents();
        return 1;
    }
}
