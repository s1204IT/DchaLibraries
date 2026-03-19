package com.android.commands.monkey;

import android.content.ComponentName;
import android.os.SystemClock;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.NoSuchElementException;
import java.util.Random;

public class MonkeySourceScript implements MonkeyEventSource {
    private static final String EVENT_KEYWORD_ACTIVITY = "LaunchActivity";
    private static final String EVENT_KEYWORD_DEVICE_WAKEUP = "DeviceWakeUp";
    private static final String EVENT_KEYWORD_DRAG = "Drag";
    private static final String EVENT_KEYWORD_END_APP_FRAMERATE_CAPTURE = "EndCaptureAppFramerate";
    private static final String EVENT_KEYWORD_END_FRAMERATE_CAPTURE = "EndCaptureFramerate";
    private static final String EVENT_KEYWORD_FLIP = "DispatchFlip";
    private static final String EVENT_KEYWORD_INPUT_STRING = "DispatchString";
    private static final String EVENT_KEYWORD_INSTRUMENTATION = "LaunchInstrumentation";
    private static final String EVENT_KEYWORD_KEY = "DispatchKey";
    private static final String EVENT_KEYWORD_KEYPRESS = "DispatchPress";
    private static final String EVENT_KEYWORD_LONGPRESS = "LongPress";
    private static final String EVENT_KEYWORD_PINCH_ZOOM = "PinchZoom";
    private static final String EVENT_KEYWORD_POINTER = "DispatchPointer";
    private static final String EVENT_KEYWORD_POWERLOG = "PowerLog";
    private static final String EVENT_KEYWORD_PRESSANDHOLD = "PressAndHold";
    private static final String EVENT_KEYWORD_PROFILE_WAIT = "ProfileWait";
    private static final String EVENT_KEYWORD_ROTATION = "RotateScreen";
    private static final String EVENT_KEYWORD_RUNCMD = "RunCmd";
    private static final String EVENT_KEYWORD_START_APP_FRAMERATE_CAPTURE = "StartCaptureAppFramerate";
    private static final String EVENT_KEYWORD_START_FRAMERATE_CAPTURE = "StartCaptureFramerate";
    private static final String EVENT_KEYWORD_TAP = "Tap";
    private static final String EVENT_KEYWORD_TRACKBALL = "DispatchTrackball";
    private static final String EVENT_KEYWORD_WAIT = "UserWait";
    private static final String EVENT_KEYWORD_WRITEPOWERLOG = "WriteLog";
    private static final String HEADER_COUNT = "count=";
    private static final String HEADER_LINE_BY_LINE = "linebyline";
    private static final String HEADER_SPEED = "speed=";
    private static int LONGPRESS_WAIT_TIME = 2000;
    private static final int MAX_ONE_TIME_READS = 100;
    private static final long SLEEP_COMPENSATE_DIFF = 16;
    private static final String STARTING_DATA_LINE = "start data >>";
    private static final boolean THIS_DEBUG = false;
    BufferedReader mBufferedReader;
    private long mDeviceSleepTime;
    FileInputStream mFStream;
    DataInputStream mInputStream;
    private long mProfileWaitTime;
    private MonkeyEventQueue mQ;
    private String mScriptFileName;
    private int mEventCountInScript = 0;
    private int mVerbose = 0;
    private double mSpeed = 1.0d;
    private long mLastRecordedDownTimeKey = 0;
    private long mLastRecordedDownTimeMotion = 0;
    private long mLastExportDownTimeKey = 0;
    private long mLastExportDownTimeMotion = 0;
    private long mLastExportEventTime = -1;
    private long mLastRecordedEventTime = -1;
    private boolean mReadScriptLineByLine = false;
    private boolean mFileOpened = false;
    private float[] mLastX = new float[2];
    private float[] mLastY = new float[2];
    private long mScriptStartTime = -1;
    private long mMonkeyStartTime = -1;

    public MonkeySourceScript(Random random, String str, long j, boolean z, long j2, long j3) {
        this.mProfileWaitTime = 5000L;
        this.mDeviceSleepTime = 30000L;
        this.mScriptFileName = str;
        this.mQ = new MonkeyEventQueue(random, j, z);
        this.mProfileWaitTime = j2;
        this.mDeviceSleepTime = j3;
    }

    private void resetValue() {
        this.mLastRecordedDownTimeKey = 0L;
        this.mLastRecordedDownTimeMotion = 0L;
        this.mLastRecordedEventTime = -1L;
        this.mLastExportDownTimeKey = 0L;
        this.mLastExportDownTimeMotion = 0L;
        this.mLastExportEventTime = -1L;
    }

    private boolean readHeader() throws IOException {
        this.mFileOpened = true;
        this.mFStream = new FileInputStream(this.mScriptFileName);
        this.mInputStream = new DataInputStream(this.mFStream);
        this.mBufferedReader = new BufferedReader(new InputStreamReader(this.mInputStream));
        while (true) {
            String line = this.mBufferedReader.readLine();
            if (line == null) {
                return false;
            }
            String strTrim = line.trim();
            if (strTrim.indexOf(HEADER_COUNT) >= 0) {
                try {
                    this.mEventCountInScript = Integer.parseInt(strTrim.substring(HEADER_COUNT.length() + 1).trim());
                } catch (NumberFormatException e) {
                    Logger.err.println("" + e);
                    return false;
                }
            } else if (strTrim.indexOf(HEADER_SPEED) >= 0) {
                try {
                    this.mSpeed = Double.parseDouble(strTrim.substring(HEADER_COUNT.length() + 1).trim());
                } catch (NumberFormatException e2) {
                    Logger.err.println("" + e2);
                    return false;
                }
            } else if (strTrim.indexOf(HEADER_LINE_BY_LINE) >= 0) {
                this.mReadScriptLineByLine = true;
            } else if (strTrim.indexOf(STARTING_DATA_LINE) >= 0) {
                return true;
            }
        }
    }

    private int readLines() throws IOException {
        for (int i = 0; i < MAX_ONE_TIME_READS; i++) {
            String line = this.mBufferedReader.readLine();
            if (line == null) {
                return i;
            }
            line.trim();
            processLine(line);
        }
        return MAX_ONE_TIME_READS;
    }

    private int readOneLine() throws IOException {
        String line = this.mBufferedReader.readLine();
        if (line == null) {
            return 0;
        }
        line.trim();
        processLine(line);
        return 1;
    }

    private void handleEvent(String str, String[] strArr) {
        MonkeyMotionEvent monkeyTrackballEvent;
        float f;
        MonkeyMotionEvent monkeyTrackballEvent2;
        MonkeyMotionEvent monkeyTouchEvent;
        String[] strArr2;
        long j;
        if (str.indexOf(EVENT_KEYWORD_KEY) >= 0 && strArr.length == 8) {
            try {
                Logger.out.println(" old key\n");
                long j2 = Long.parseLong(strArr[0]);
                long j3 = Long.parseLong(strArr[1]);
                int i = Integer.parseInt(strArr[2]);
                int i2 = Integer.parseInt(strArr[3]);
                MonkeyKeyEvent monkeyKeyEvent = new MonkeyKeyEvent(j2, j3, i, i2, Integer.parseInt(strArr[4]), Integer.parseInt(strArr[5]), Integer.parseInt(strArr[6]), Integer.parseInt(strArr[7]));
                Logger.out.println(" Key code " + i2 + "\n");
                this.mQ.addLast((MonkeyEvent) monkeyKeyEvent);
                Logger.out.println("Added key up \n");
                return;
            } catch (NumberFormatException e) {
                return;
            }
        }
        if ((str.indexOf(EVENT_KEYWORD_POINTER) >= 0 || str.indexOf(EVENT_KEYWORD_TRACKBALL) >= 0) && strArr.length == 12) {
            try {
                long j4 = Long.parseLong(strArr[0]);
                long j5 = Long.parseLong(strArr[1]);
                int i3 = Integer.parseInt(strArr[2]);
                float f2 = Float.parseFloat(strArr[3]);
                float f3 = Float.parseFloat(strArr[4]);
                float f4 = Float.parseFloat(strArr[5]);
                float f5 = Float.parseFloat(strArr[6]);
                int i4 = Integer.parseInt(strArr[7]);
                float f6 = Float.parseFloat(strArr[8]);
                float f7 = Float.parseFloat(strArr[9]);
                int i5 = Integer.parseInt(strArr[10]);
                int i6 = Integer.parseInt(strArr[11]);
                if (str.indexOf("Pointer") > 0) {
                    monkeyTrackballEvent = new MonkeyTouchEvent(i3);
                } else {
                    monkeyTrackballEvent = new MonkeyTrackballEvent(i3);
                }
                monkeyTrackballEvent.setDownTime(j4).setEventTime(j5).setMetaState(i4).setPrecision(f6, f7).setDeviceId(i5).setEdgeFlags(i6).addPointer(0, f2, f3, f4, f5);
                this.mQ.addLast((MonkeyEvent) monkeyTrackballEvent);
                return;
            } catch (NumberFormatException e2) {
                return;
            }
        }
        long j6 = 0;
        if ((str.indexOf(EVENT_KEYWORD_POINTER) >= 0 || str.indexOf(EVENT_KEYWORD_TRACKBALL) >= 0) && strArr.length == 13) {
            try {
                long j7 = Long.parseLong(strArr[0]);
                long j8 = Long.parseLong(strArr[1]);
                int i7 = Integer.parseInt(strArr[2]);
                float f8 = Float.parseFloat(strArr[3]);
                float f9 = Float.parseFloat(strArr[4]);
                float f10 = Float.parseFloat(strArr[5]);
                float f11 = Float.parseFloat(strArr[6]);
                int i8 = Integer.parseInt(strArr[7]);
                float f12 = Float.parseFloat(strArr[8]);
                float f13 = Float.parseFloat(strArr[9]);
                int i9 = Integer.parseInt(strArr[10]);
                int i10 = Integer.parseInt(strArr[11]);
                int i11 = Integer.parseInt(strArr[12]);
                if (str.indexOf("Pointer") > 0) {
                    if (i7 == 5) {
                        monkeyTouchEvent = new MonkeyTouchEvent(5 | (i11 << 8)).setIntermediateNote(true);
                    } else {
                        monkeyTouchEvent = new MonkeyTouchEvent(i7);
                    }
                    MonkeyMotionEvent monkeyMotionEvent = monkeyTouchEvent;
                    f = f10;
                    if (this.mScriptStartTime < 0) {
                        this.mMonkeyStartTime = SystemClock.uptimeMillis();
                        this.mScriptStartTime = j8;
                    }
                    monkeyTrackballEvent2 = monkeyMotionEvent;
                } else {
                    f = f10;
                    monkeyTrackballEvent2 = new MonkeyTrackballEvent(i7);
                }
                if (i11 == 1) {
                    monkeyTrackballEvent2.setDownTime(j7).setEventTime(j8).setMetaState(i8).setPrecision(f12, f13).setDeviceId(i9).setEdgeFlags(i10).addPointer(0, this.mLastX[0], this.mLastY[0], f, f11).addPointer(1, f8, f9, f, f11);
                    this.mLastX[1] = f8;
                    this.mLastY[1] = f9;
                } else if (i11 == 0) {
                    monkeyTrackballEvent2.setDownTime(j7).setEventTime(j8).setMetaState(i8).setPrecision(f12, f13).setDeviceId(i9).setEdgeFlags(i10).addPointer(0, f8, f9, f, f11);
                    if (i7 == 6) {
                        monkeyTrackballEvent2.addPointer(1, this.mLastX[1], this.mLastY[1]);
                    }
                    this.mLastX[0] = f8;
                    this.mLastY[0] = f9;
                }
                if (this.mReadScriptLineByLine) {
                    long jUptimeMillis = SystemClock.uptimeMillis() - this.mMonkeyStartTime;
                    long j9 = j8 - this.mScriptStartTime;
                    if (jUptimeMillis < j9) {
                        this.mQ.addLast((MonkeyEvent) new MonkeyWaitEvent(j9 - jUptimeMillis));
                    }
                }
                this.mQ.addLast((MonkeyEvent) monkeyTrackballEvent2);
                return;
            } catch (NumberFormatException e3) {
                return;
            }
        }
        if (str.indexOf(EVENT_KEYWORD_ROTATION) >= 0 && strArr.length == 2) {
            try {
                int i12 = Integer.parseInt(strArr[0]);
                int i13 = Integer.parseInt(strArr[1]);
                if (i12 == 0 || i12 == 1 || i12 == 2 || i12 == 3) {
                    this.mQ.addLast((MonkeyEvent) new MonkeyRotationEvent(i12, i13 != 0));
                    return;
                }
                return;
            } catch (NumberFormatException e4) {
                return;
            }
        }
        if (str.indexOf(EVENT_KEYWORD_TAP) >= 0 && strArr.length >= 2) {
            try {
                float f14 = Float.parseFloat(strArr[0]);
                float f15 = Float.parseFloat(strArr[1]);
                if (strArr.length == 3) {
                    j = Long.parseLong(strArr[2]);
                } else {
                    j = 0;
                }
                long jUptimeMillis2 = SystemClock.uptimeMillis();
                this.mQ.addLast((MonkeyEvent) new MonkeyTouchEvent(0).setDownTime(jUptimeMillis2).setEventTime(jUptimeMillis2).addPointer(0, f14, f15, 1.0f, 5.0f));
                if (j > 0) {
                    this.mQ.addLast((MonkeyEvent) new MonkeyWaitEvent(j));
                }
                this.mQ.addLast((MonkeyEvent) new MonkeyTouchEvent(1).setDownTime(jUptimeMillis2).setEventTime(jUptimeMillis2).addPointer(0, f14, f15, 1.0f, 5.0f));
                return;
            } catch (NumberFormatException e5) {
                Logger.err.println("// " + e5.toString());
                return;
            }
        }
        if (str.indexOf(EVENT_KEYWORD_PRESSANDHOLD) >= 0 && strArr.length == 3) {
            try {
                float f16 = Float.parseFloat(strArr[0]);
                float f17 = Float.parseFloat(strArr[1]);
                long j10 = Long.parseLong(strArr[2]);
                long jUptimeMillis3 = SystemClock.uptimeMillis();
                MonkeyMotionEvent monkeyMotionEventAddPointer = new MonkeyTouchEvent(0).setDownTime(jUptimeMillis3).setEventTime(jUptimeMillis3).addPointer(0, f16, f17, 1.0f, 5.0f);
                MonkeyWaitEvent monkeyWaitEvent = new MonkeyWaitEvent(j10);
                long j11 = jUptimeMillis3 + j10;
                new MonkeyTouchEvent(1).setDownTime(j11).setEventTime(j11).addPointer(0, f16, f17, 1.0f, 5.0f);
                this.mQ.addLast((MonkeyEvent) monkeyMotionEventAddPointer);
                this.mQ.addLast((MonkeyEvent) monkeyWaitEvent);
                this.mQ.addLast((MonkeyEvent) monkeyWaitEvent);
                return;
            } catch (NumberFormatException e6) {
                Logger.err.println("// " + e6.toString());
                return;
            }
        }
        if (str.indexOf(EVENT_KEYWORD_DRAG) >= 0 && strArr.length == 5) {
            float f18 = Float.parseFloat(strArr[0]);
            float f19 = Float.parseFloat(strArr[1]);
            float f20 = Float.parseFloat(strArr[2]);
            float f21 = Float.parseFloat(strArr[3]);
            int i14 = Integer.parseInt(strArr[4]);
            long jUptimeMillis4 = SystemClock.uptimeMillis();
            long jUptimeMillis5 = SystemClock.uptimeMillis();
            if (i14 > 0) {
                float f22 = i14;
                float f23 = (f20 - f18) / f22;
                float f24 = (f21 - f19) / f22;
                float f25 = f18;
                float f26 = f19;
                this.mQ.addLast((MonkeyEvent) new MonkeyTouchEvent(0).setDownTime(jUptimeMillis4).setEventTime(jUptimeMillis5).addPointer(0, f25, f26, 1.0f, 5.0f));
                int i15 = 0;
                while (i15 < i14) {
                    float f27 = f25 + f23;
                    float f28 = f26 + f24;
                    this.mQ.addLast((MonkeyEvent) new MonkeyTouchEvent(2).setDownTime(jUptimeMillis4).setEventTime(SystemClock.uptimeMillis()).addPointer(0, f27, f28, 1.0f, 5.0f));
                    i15++;
                    f25 = f27;
                    f26 = f28;
                }
                this.mQ.addLast((MonkeyEvent) new MonkeyTouchEvent(1).setDownTime(jUptimeMillis4).setEventTime(SystemClock.uptimeMillis()).addPointer(0, f25, f26, 1.0f, 5.0f));
            }
        }
        if (str.indexOf(EVENT_KEYWORD_PINCH_ZOOM) >= 0 && strArr.length == 9) {
            float f29 = Float.parseFloat(strArr[0]);
            float f30 = Float.parseFloat(strArr[1]);
            float f31 = Float.parseFloat(strArr[2]);
            float f32 = Float.parseFloat(strArr[3]);
            float f33 = Float.parseFloat(strArr[4]);
            float f34 = Float.parseFloat(strArr[5]);
            float f35 = Float.parseFloat(strArr[6]);
            float f36 = Float.parseFloat(strArr[7]);
            int i16 = Integer.parseInt(strArr[8]);
            long jUptimeMillis6 = SystemClock.uptimeMillis();
            long jUptimeMillis7 = SystemClock.uptimeMillis();
            if (i16 > 0) {
                float f37 = i16;
                float f38 = (f31 - f29) / f37;
                float f39 = (f32 - f30) / f37;
                float f40 = (f35 - f33) / f37;
                float f41 = (f36 - f34) / f37;
                this.mQ.addLast((MonkeyEvent) new MonkeyTouchEvent(0).setDownTime(jUptimeMillis6).setEventTime(jUptimeMillis7).addPointer(0, f29, f30, 1.0f, 5.0f));
                this.mQ.addLast((MonkeyEvent) new MonkeyTouchEvent(261).setDownTime(jUptimeMillis6).addPointer(0, f29, f30).addPointer(1, f33, f34).setIntermediateNote(true));
                float f42 = f30;
                float f43 = f29;
                int i17 = 0;
                while (i17 < i16) {
                    f43 += f38;
                    f42 += f39;
                    float f44 = f33 + f40;
                    float f45 = f34 + f41;
                    this.mQ.addLast((MonkeyEvent) new MonkeyTouchEvent(2).setDownTime(jUptimeMillis6).setEventTime(SystemClock.uptimeMillis()).addPointer(0, f43, f42, 1.0f, 5.0f).addPointer(1, f44, f45, 1.0f, 5.0f));
                    i17++;
                    f33 = f44;
                    f34 = f45;
                }
                this.mQ.addLast((MonkeyEvent) new MonkeyTouchEvent(6).setDownTime(jUptimeMillis6).setEventTime(SystemClock.uptimeMillis()).addPointer(0, f43, f42).addPointer(1, f33, f34));
            }
        }
        if (str.indexOf(EVENT_KEYWORD_FLIP) >= 0) {
            strArr2 = strArr;
            if (strArr2.length == 1) {
                this.mQ.addLast((MonkeyEvent) new MonkeyFlipEvent(Boolean.parseBoolean(strArr2[0])));
            }
        } else {
            strArr2 = strArr;
        }
        if (str.indexOf(EVENT_KEYWORD_ACTIVITY) >= 0 && strArr2.length >= 2) {
            ComponentName componentName = new ComponentName(strArr2[0], strArr2[1]);
            if (strArr2.length > 2) {
                try {
                    j6 = Long.parseLong(strArr2[2]);
                } catch (NumberFormatException e7) {
                    Logger.err.println("// " + e7.toString());
                    return;
                }
            }
            long j12 = j6;
            if (strArr2.length == 2) {
                this.mQ.addLast((MonkeyEvent) new MonkeyActivityEvent(componentName));
                return;
            } else {
                this.mQ.addLast((MonkeyEvent) new MonkeyActivityEvent(componentName, j12));
                return;
            }
        }
        if (str.indexOf(EVENT_KEYWORD_DEVICE_WAKEUP) >= 0) {
            long j13 = this.mDeviceSleepTime;
            this.mQ.addLast((MonkeyEvent) new MonkeyActivityEvent(new ComponentName("com.google.android.powerutil", "com.google.android.powerutil.WakeUpScreen"), j13));
            this.mQ.addLast((MonkeyEvent) new MonkeyKeyEvent(0, 7));
            this.mQ.addLast((MonkeyEvent) new MonkeyKeyEvent(1, 7));
            this.mQ.addLast((MonkeyEvent) new MonkeyWaitEvent(j13 + 3000));
            this.mQ.addLast((MonkeyEvent) new MonkeyKeyEvent(0, 82));
            this.mQ.addLast((MonkeyEvent) new MonkeyKeyEvent(1, 82));
            this.mQ.addLast((MonkeyEvent) new MonkeyKeyEvent(0, 4));
            this.mQ.addLast((MonkeyEvent) new MonkeyKeyEvent(1, 4));
            return;
        }
        if (str.indexOf(EVENT_KEYWORD_INSTRUMENTATION) >= 0 && strArr2.length == 2) {
            this.mQ.addLast((MonkeyEvent) new MonkeyInstrumentationEvent(strArr2[0], strArr2[1]));
            return;
        }
        if (str.indexOf(EVENT_KEYWORD_WAIT) >= 0 && strArr2.length == 1) {
            try {
                this.mQ.addLast((MonkeyEvent) new MonkeyWaitEvent(Integer.parseInt(strArr2[0])));
                return;
            } catch (NumberFormatException e8) {
                return;
            }
        }
        if (str.indexOf(EVENT_KEYWORD_PROFILE_WAIT) >= 0) {
            this.mQ.addLast((MonkeyEvent) new MonkeyWaitEvent(this.mProfileWaitTime));
            return;
        }
        if (str.indexOf(EVENT_KEYWORD_KEYPRESS) >= 0 && strArr2.length == 1) {
            int keyCode = MonkeySourceRandom.getKeyCode(strArr2[0]);
            if (keyCode == 0) {
                return;
            }
            this.mQ.addLast((MonkeyEvent) new MonkeyKeyEvent(0, keyCode));
            this.mQ.addLast((MonkeyEvent) new MonkeyKeyEvent(1, keyCode));
            return;
        }
        if (str.indexOf(EVENT_KEYWORD_LONGPRESS) >= 0) {
            this.mQ.addLast((MonkeyEvent) new MonkeyKeyEvent(0, 23));
            this.mQ.addLast((MonkeyEvent) new MonkeyWaitEvent(LONGPRESS_WAIT_TIME));
            this.mQ.addLast((MonkeyEvent) new MonkeyKeyEvent(1, 23));
        }
        if (str.indexOf(EVENT_KEYWORD_POWERLOG) >= 0 && strArr2.length > 0) {
            String str2 = strArr2[0];
            if (strArr2.length == 1) {
                this.mQ.addLast((MonkeyEvent) new MonkeyPowerEvent(str2));
            } else if (strArr2.length == 2) {
                this.mQ.addLast((MonkeyEvent) new MonkeyPowerEvent(str2, strArr2[1]));
            }
        }
        if (str.indexOf(EVENT_KEYWORD_WRITEPOWERLOG) >= 0) {
            this.mQ.addLast((MonkeyEvent) new MonkeyPowerEvent());
        }
        if (str.indexOf(EVENT_KEYWORD_RUNCMD) >= 0 && strArr2.length == 1) {
            this.mQ.addLast((MonkeyEvent) new MonkeyCommandEvent(strArr2[0]));
        }
        if (str.indexOf(EVENT_KEYWORD_INPUT_STRING) >= 0 && strArr2.length == 1) {
            this.mQ.addLast((MonkeyEvent) new MonkeyCommandEvent("input text " + strArr2[0]));
            return;
        }
        if (str.indexOf(EVENT_KEYWORD_START_FRAMERATE_CAPTURE) >= 0) {
            this.mQ.addLast((MonkeyEvent) new MonkeyGetFrameRateEvent("start"));
            return;
        }
        if (str.indexOf(EVENT_KEYWORD_END_FRAMERATE_CAPTURE) >= 0 && strArr2.length == 1) {
            this.mQ.addLast((MonkeyEvent) new MonkeyGetFrameRateEvent("end", strArr2[0]));
            return;
        }
        if (str.indexOf(EVENT_KEYWORD_START_APP_FRAMERATE_CAPTURE) >= 0 && strArr2.length == 1) {
            this.mQ.addLast((MonkeyEvent) new MonkeyGetAppFrameRateEvent("start", strArr2[0]));
        } else if (str.indexOf(EVENT_KEYWORD_END_APP_FRAMERATE_CAPTURE) >= 0 && strArr2.length == 2) {
            this.mQ.addLast((MonkeyEvent) new MonkeyGetAppFrameRateEvent("end", strArr2[0], strArr2[1]));
        }
    }

    private void processLine(String str) {
        int iIndexOf = str.indexOf(40);
        int iIndexOf2 = str.indexOf(41);
        if (iIndexOf < 0 || iIndexOf2 < 0) {
            return;
        }
        String[] strArrSplit = str.substring(iIndexOf + 1, iIndexOf2).split(",");
        for (int i = 0; i < strArrSplit.length; i++) {
            strArrSplit[i] = strArrSplit[i].trim();
        }
        handleEvent(str, strArrSplit);
    }

    private void closeFile() throws IOException {
        this.mFileOpened = false;
        try {
            this.mFStream.close();
            this.mInputStream.close();
        } catch (NullPointerException e) {
        }
    }

    private void readNextBatch() throws IOException {
        int lines;
        if (!this.mFileOpened) {
            resetValue();
            readHeader();
        }
        if (this.mReadScriptLineByLine) {
            lines = readOneLine();
        } else {
            lines = readLines();
        }
        if (lines == 0) {
            closeFile();
        }
    }

    private void needSleep(long j) {
        if (j < 1) {
            return;
        }
        try {
            Thread.sleep(j);
        } catch (InterruptedException e) {
        }
    }

    @Override
    public boolean validate() {
        try {
            boolean header = readHeader();
            closeFile();
            if (this.mVerbose > 0) {
                Logger.out.println("Replaying " + this.mEventCountInScript + " events with speed " + this.mSpeed);
            }
            return header;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public void setVerbose(int i) {
        this.mVerbose = i;
    }

    private void adjustKeyEventTime(MonkeyKeyEvent monkeyKeyEvent) {
        long downTime;
        long j;
        if (monkeyKeyEvent.getEventTime() < 0) {
            return;
        }
        if (this.mLastRecordedEventTime <= 0) {
            downTime = SystemClock.uptimeMillis();
            j = downTime;
        } else {
            if (monkeyKeyEvent.getDownTime() != this.mLastRecordedDownTimeKey) {
                downTime = monkeyKeyEvent.getDownTime();
            } else {
                downTime = this.mLastExportDownTimeKey;
            }
            long eventTime = (long) ((monkeyKeyEvent.getEventTime() - this.mLastRecordedEventTime) * this.mSpeed);
            j = this.mLastExportEventTime + eventTime;
            needSleep(eventTime - SLEEP_COMPENSATE_DIFF);
        }
        this.mLastRecordedDownTimeKey = monkeyKeyEvent.getDownTime();
        this.mLastRecordedEventTime = monkeyKeyEvent.getEventTime();
        monkeyKeyEvent.setDownTime(downTime);
        monkeyKeyEvent.setEventTime(j);
        this.mLastExportDownTimeKey = downTime;
        this.mLastExportEventTime = j;
    }

    private void adjustMotionEventTime(MonkeyMotionEvent monkeyMotionEvent) {
        long jUptimeMillis = SystemClock.uptimeMillis();
        long downTime = monkeyMotionEvent.getDownTime();
        if (downTime == this.mLastRecordedDownTimeMotion) {
            monkeyMotionEvent.setDownTime(this.mLastExportDownTimeMotion);
        } else {
            this.mLastRecordedDownTimeMotion = downTime;
            monkeyMotionEvent.setDownTime(jUptimeMillis);
            this.mLastExportDownTimeMotion = jUptimeMillis;
        }
        monkeyMotionEvent.setEventTime(jUptimeMillis);
    }

    @Override
    public MonkeyEvent getNextEvent() {
        if (this.mQ.isEmpty()) {
            try {
                readNextBatch();
            } catch (IOException e) {
                return null;
            }
        }
        try {
            MonkeyEvent first = this.mQ.getFirst();
            this.mQ.removeFirst();
            if (first.getEventType() == 0) {
                adjustKeyEventTime((MonkeyKeyEvent) first);
            } else if (first.getEventType() == 1 || first.getEventType() == 2) {
                adjustMotionEventTime((MonkeyMotionEvent) first);
            }
            return first;
        } catch (NoSuchElementException e2) {
            return null;
        }
    }
}
