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

    public MonkeySourceScript(Random random, String filename, long throttle, boolean randomizeThrottle, long profileWaitTime, long deviceSleepTime) {
        this.mProfileWaitTime = 5000L;
        this.mDeviceSleepTime = 30000L;
        this.mScriptFileName = filename;
        this.mQ = new MonkeyEventQueue(random, throttle, randomizeThrottle);
        this.mProfileWaitTime = profileWaitTime;
        this.mDeviceSleepTime = deviceSleepTime;
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
            String line2 = line.trim();
            if (line2.indexOf(HEADER_COUNT) >= 0) {
                try {
                    String value = line2.substring(HEADER_COUNT.length() + 1).trim();
                    this.mEventCountInScript = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    System.err.println(e);
                    return false;
                }
            } else if (line2.indexOf(HEADER_SPEED) >= 0) {
                try {
                    String value2 = line2.substring(HEADER_COUNT.length() + 1).trim();
                    this.mSpeed = Double.parseDouble(value2);
                } catch (NumberFormatException e2) {
                    System.err.println(e2);
                    return false;
                }
            } else if (line2.indexOf(HEADER_LINE_BY_LINE) >= 0) {
                this.mReadScriptLineByLine = true;
            } else if (line2.indexOf(STARTING_DATA_LINE) >= 0) {
                return true;
            }
        }
    }

    private int readLines() throws IOException {
        for (int i = 0; i < MAX_ONE_TIME_READS; i++) {
            String line = this.mBufferedReader.readLine();
            if (line != null) {
                line.trim();
                processLine(line);
            } else {
                return i;
            }
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

    private void handleEvent(String s, String[] args) {
        MonkeyMotionEvent e;
        MonkeyMotionEvent e2;
        if (s.indexOf(EVENT_KEYWORD_KEY) >= 0 && args.length == 8) {
            try {
                System.out.println(" old key\n");
                long downTime = Long.parseLong(args[0]);
                long eventTime = Long.parseLong(args[1]);
                int action = Integer.parseInt(args[2]);
                int code = Integer.parseInt(args[3]);
                int repeat = Integer.parseInt(args[4]);
                int metaState = Integer.parseInt(args[5]);
                int device = Integer.parseInt(args[6]);
                int scancode = Integer.parseInt(args[7]);
                MonkeyKeyEvent e3 = new MonkeyKeyEvent(downTime, eventTime, action, code, repeat, metaState, device, scancode);
                System.out.println(" Key code " + code + "\n");
                this.mQ.addLast((MonkeyEvent) e3);
                System.out.println("Added key up \n");
                return;
            } catch (NumberFormatException e4) {
                return;
            }
        }
        if ((s.indexOf(EVENT_KEYWORD_POINTER) >= 0 || s.indexOf(EVENT_KEYWORD_TRACKBALL) >= 0) && args.length == 12) {
            try {
                long downTime2 = Long.parseLong(args[0]);
                long eventTime2 = Long.parseLong(args[1]);
                int action2 = Integer.parseInt(args[2]);
                float x = Float.parseFloat(args[3]);
                float y = Float.parseFloat(args[4]);
                float pressure = Float.parseFloat(args[5]);
                float size = Float.parseFloat(args[6]);
                int metaState2 = Integer.parseInt(args[7]);
                float xPrecision = Float.parseFloat(args[8]);
                float yPrecision = Float.parseFloat(args[9]);
                int device2 = Integer.parseInt(args[10]);
                int edgeFlags = Integer.parseInt(args[11]);
                if (s.indexOf("Pointer") > 0) {
                    e = new MonkeyTouchEvent(action2);
                } else {
                    e = new MonkeyTrackballEvent(action2);
                }
                e.setDownTime(downTime2).setEventTime(eventTime2).setMetaState(metaState2).setPrecision(xPrecision, yPrecision).setDeviceId(device2).setEdgeFlags(edgeFlags).addPointer(0, x, y, pressure, size);
                this.mQ.addLast((MonkeyEvent) e);
                return;
            } catch (NumberFormatException e5) {
                return;
            }
        }
        if ((s.indexOf(EVENT_KEYWORD_POINTER) >= 0 || s.indexOf(EVENT_KEYWORD_TRACKBALL) >= 0) && args.length == 13) {
            try {
                long downTime3 = Long.parseLong(args[0]);
                long eventTime3 = Long.parseLong(args[1]);
                int action3 = Integer.parseInt(args[2]);
                float x2 = Float.parseFloat(args[3]);
                float y2 = Float.parseFloat(args[4]);
                float pressure2 = Float.parseFloat(args[5]);
                float size2 = Float.parseFloat(args[6]);
                int metaState3 = Integer.parseInt(args[7]);
                float xPrecision2 = Float.parseFloat(args[8]);
                float yPrecision2 = Float.parseFloat(args[9]);
                int device3 = Integer.parseInt(args[10]);
                int edgeFlags2 = Integer.parseInt(args[11]);
                int pointerId = Integer.parseInt(args[12]);
                if (s.indexOf("Pointer") > 0) {
                    if (action3 == 5) {
                        e2 = new MonkeyTouchEvent((pointerId << 8) | 5).setIntermediateNote(true);
                    } else {
                        e2 = new MonkeyTouchEvent(action3);
                    }
                    if (this.mScriptStartTime < 0) {
                        this.mMonkeyStartTime = SystemClock.uptimeMillis();
                        this.mScriptStartTime = eventTime3;
                    }
                } else {
                    e2 = new MonkeyTrackballEvent(action3);
                }
                if (pointerId == 1) {
                    e2.setDownTime(downTime3).setEventTime(eventTime3).setMetaState(metaState3).setPrecision(xPrecision2, yPrecision2).setDeviceId(device3).setEdgeFlags(edgeFlags2).addPointer(0, this.mLastX[0], this.mLastY[0], pressure2, size2).addPointer(1, x2, y2, pressure2, size2);
                    this.mLastX[1] = x2;
                    this.mLastY[1] = y2;
                } else if (pointerId == 0) {
                    e2.setDownTime(downTime3).setEventTime(eventTime3).setMetaState(metaState3).setPrecision(xPrecision2, yPrecision2).setDeviceId(device3).setEdgeFlags(edgeFlags2).addPointer(0, x2, y2, pressure2, size2);
                    if (action3 == 6) {
                        e2.addPointer(1, this.mLastX[1], this.mLastY[1]);
                    }
                    this.mLastX[0] = x2;
                    this.mLastY[0] = y2;
                }
                if (this.mReadScriptLineByLine) {
                    long curUpTime = SystemClock.uptimeMillis();
                    long realElapsedTime = curUpTime - this.mMonkeyStartTime;
                    long scriptElapsedTime = eventTime3 - this.mScriptStartTime;
                    if (realElapsedTime < scriptElapsedTime) {
                        long waitDuration = scriptElapsedTime - realElapsedTime;
                        this.mQ.addLast((MonkeyEvent) new MonkeyWaitEvent(waitDuration));
                    }
                }
                this.mQ.addLast((MonkeyEvent) e2);
                return;
            } catch (NumberFormatException e6) {
                return;
            }
        }
        if (s.indexOf(EVENT_KEYWORD_ROTATION) >= 0 && args.length == 2) {
            try {
                int rotationDegree = Integer.parseInt(args[0]);
                int persist = Integer.parseInt(args[1]);
                if (rotationDegree == 0 || rotationDegree == 1 || rotationDegree == 2 || rotationDegree == 3) {
                    this.mQ.addLast((MonkeyEvent) new MonkeyRotationEvent(rotationDegree, persist != 0));
                    return;
                }
                return;
            } catch (NumberFormatException e7) {
                return;
            }
        }
        if (s.indexOf(EVENT_KEYWORD_TAP) >= 0 && args.length >= 2) {
            try {
                float x3 = Float.parseFloat(args[0]);
                float y3 = Float.parseFloat(args[1]);
                long tapDuration = 0;
                if (args.length == 3) {
                    tapDuration = Long.parseLong(args[2]);
                }
                long downTime4 = SystemClock.uptimeMillis();
                MonkeyMotionEvent e1 = new MonkeyTouchEvent(0).setDownTime(downTime4).setEventTime(downTime4).addPointer(0, x3, y3, 1.0f, 5.0f);
                this.mQ.addLast((MonkeyEvent) e1);
                if (tapDuration > 0) {
                    this.mQ.addLast((MonkeyEvent) new MonkeyWaitEvent(tapDuration));
                }
                this.mQ.addLast((MonkeyEvent) new MonkeyTouchEvent(1).setDownTime(downTime4).setEventTime(downTime4).addPointer(0, x3, y3, 1.0f, 5.0f));
                return;
            } catch (NumberFormatException e8) {
                System.err.println("// " + e8.toString());
                return;
            }
        }
        if (s.indexOf(EVENT_KEYWORD_PRESSANDHOLD) >= 0 && args.length == 3) {
            try {
                float x4 = Float.parseFloat(args[0]);
                float y4 = Float.parseFloat(args[1]);
                long pressDuration = Long.parseLong(args[2]);
                long downTime5 = SystemClock.uptimeMillis();
                MonkeyMotionEvent e12 = new MonkeyTouchEvent(0).setDownTime(downTime5).setEventTime(downTime5).addPointer(0, x4, y4, 1.0f, 5.0f);
                MonkeyWaitEvent e22 = new MonkeyWaitEvent(pressDuration);
                new MonkeyTouchEvent(1).setDownTime(downTime5 + pressDuration).setEventTime(downTime5 + pressDuration).addPointer(0, x4, y4, 1.0f, 5.0f);
                this.mQ.addLast((MonkeyEvent) e12);
                this.mQ.addLast((MonkeyEvent) e22);
                this.mQ.addLast((MonkeyEvent) e22);
                return;
            } catch (NumberFormatException e9) {
                System.err.println("// " + e9.toString());
                return;
            }
        }
        if (s.indexOf(EVENT_KEYWORD_DRAG) >= 0 && args.length == 5) {
            float xStart = Float.parseFloat(args[0]);
            float yStart = Float.parseFloat(args[1]);
            float xEnd = Float.parseFloat(args[2]);
            float yEnd = Float.parseFloat(args[3]);
            int stepCount = Integer.parseInt(args[4]);
            float x5 = xStart;
            float y5 = yStart;
            long downTime6 = SystemClock.uptimeMillis();
            long eventTime4 = SystemClock.uptimeMillis();
            if (stepCount > 0) {
                float xStep = (xEnd - xStart) / stepCount;
                float yStep = (yEnd - yStart) / stepCount;
                MonkeyMotionEvent e10 = new MonkeyTouchEvent(0).setDownTime(downTime6).setEventTime(eventTime4).addPointer(0, x5, y5, 1.0f, 5.0f);
                this.mQ.addLast((MonkeyEvent) e10);
                for (int i = 0; i < stepCount; i++) {
                    x5 += xStep;
                    y5 += yStep;
                    long eventTime5 = SystemClock.uptimeMillis();
                    MonkeyMotionEvent e11 = new MonkeyTouchEvent(2).setDownTime(downTime6).setEventTime(eventTime5).addPointer(0, x5, y5, 1.0f, 5.0f);
                    this.mQ.addLast((MonkeyEvent) e11);
                }
                long eventTime6 = SystemClock.uptimeMillis();
                MonkeyMotionEvent e13 = new MonkeyTouchEvent(1).setDownTime(downTime6).setEventTime(eventTime6).addPointer(0, x5, y5, 1.0f, 5.0f);
                this.mQ.addLast((MonkeyEvent) e13);
            }
        }
        if (s.indexOf(EVENT_KEYWORD_PINCH_ZOOM) >= 0 && args.length == 9) {
            float pt1xStart = Float.parseFloat(args[0]);
            float pt1yStart = Float.parseFloat(args[1]);
            float pt1xEnd = Float.parseFloat(args[2]);
            float pt1yEnd = Float.parseFloat(args[3]);
            float pt2xStart = Float.parseFloat(args[4]);
            float pt2yStart = Float.parseFloat(args[5]);
            float pt2xEnd = Float.parseFloat(args[6]);
            float pt2yEnd = Float.parseFloat(args[7]);
            int stepCount2 = Integer.parseInt(args[8]);
            float x1 = pt1xStart;
            float y1 = pt1yStart;
            float x22 = pt2xStart;
            float y22 = pt2yStart;
            long downTime7 = SystemClock.uptimeMillis();
            long eventTime7 = SystemClock.uptimeMillis();
            if (stepCount2 > 0) {
                float pt1xStep = (pt1xEnd - pt1xStart) / stepCount2;
                float pt1yStep = (pt1yEnd - pt1yStart) / stepCount2;
                float pt2xStep = (pt2xEnd - pt2xStart) / stepCount2;
                float pt2yStep = (pt2yEnd - pt2yStart) / stepCount2;
                this.mQ.addLast((MonkeyEvent) new MonkeyTouchEvent(0).setDownTime(downTime7).setEventTime(eventTime7).addPointer(0, x1, y1, 1.0f, 5.0f));
                this.mQ.addLast((MonkeyEvent) new MonkeyTouchEvent(261).setDownTime(downTime7).addPointer(0, x1, y1).addPointer(1, x22, y22).setIntermediateNote(true));
                for (int i2 = 0; i2 < stepCount2; i2++) {
                    x1 += pt1xStep;
                    y1 += pt1yStep;
                    x22 += pt2xStep;
                    y22 += pt2yStep;
                    long eventTime8 = SystemClock.uptimeMillis();
                    this.mQ.addLast((MonkeyEvent) new MonkeyTouchEvent(2).setDownTime(downTime7).setEventTime(eventTime8).addPointer(0, x1, y1, 1.0f, 5.0f).addPointer(1, x22, y22, 1.0f, 5.0f));
                }
                long eventTime9 = SystemClock.uptimeMillis();
                this.mQ.addLast((MonkeyEvent) new MonkeyTouchEvent(6).setDownTime(downTime7).setEventTime(eventTime9).addPointer(0, x1, y1).addPointer(1, x22, y22));
            }
        }
        if (s.indexOf(EVENT_KEYWORD_FLIP) >= 0 && args.length == 1) {
            boolean keyboardOpen = Boolean.parseBoolean(args[0]);
            MonkeyFlipEvent e14 = new MonkeyFlipEvent(keyboardOpen);
            this.mQ.addLast((MonkeyEvent) e14);
        }
        if (s.indexOf(EVENT_KEYWORD_ACTIVITY) >= 0 && args.length >= 2) {
            String pkg_name = args[0];
            String cl_name = args[1];
            long alarmTime = 0;
            ComponentName mApp = new ComponentName(pkg_name, cl_name);
            if (args.length > 2) {
                try {
                    alarmTime = Long.parseLong(args[2]);
                } catch (NumberFormatException e15) {
                    System.err.println("// " + e15.toString());
                    return;
                }
            }
            if (args.length == 2) {
                MonkeyActivityEvent e16 = new MonkeyActivityEvent(mApp);
                this.mQ.addLast((MonkeyEvent) e16);
                return;
            } else {
                MonkeyActivityEvent e17 = new MonkeyActivityEvent(mApp, alarmTime);
                this.mQ.addLast((MonkeyEvent) e17);
                return;
            }
        }
        if (s.indexOf(EVENT_KEYWORD_DEVICE_WAKEUP) >= 0) {
            long deviceSleepTime = this.mDeviceSleepTime;
            this.mQ.addLast((MonkeyEvent) new MonkeyActivityEvent(new ComponentName("com.google.android.powerutil", "com.google.android.powerutil.WakeUpScreen"), deviceSleepTime));
            this.mQ.addLast((MonkeyEvent) new MonkeyKeyEvent(0, 7));
            this.mQ.addLast((MonkeyEvent) new MonkeyKeyEvent(1, 7));
            this.mQ.addLast((MonkeyEvent) new MonkeyWaitEvent(3000 + deviceSleepTime));
            this.mQ.addLast((MonkeyEvent) new MonkeyKeyEvent(0, 82));
            this.mQ.addLast((MonkeyEvent) new MonkeyKeyEvent(1, 82));
            this.mQ.addLast((MonkeyEvent) new MonkeyKeyEvent(0, 4));
            this.mQ.addLast((MonkeyEvent) new MonkeyKeyEvent(1, 4));
            return;
        }
        if (s.indexOf(EVENT_KEYWORD_INSTRUMENTATION) >= 0 && args.length == 2) {
            String test_name = args[0];
            String runner_name = args[1];
            MonkeyInstrumentationEvent e18 = new MonkeyInstrumentationEvent(test_name, runner_name);
            this.mQ.addLast((MonkeyEvent) e18);
            return;
        }
        if (s.indexOf(EVENT_KEYWORD_WAIT) >= 0 && args.length == 1) {
            try {
                long sleeptime = Integer.parseInt(args[0]);
                MonkeyWaitEvent e19 = new MonkeyWaitEvent(sleeptime);
                this.mQ.addLast((MonkeyEvent) e19);
                return;
            } catch (NumberFormatException e20) {
                return;
            }
        }
        if (s.indexOf(EVENT_KEYWORD_PROFILE_WAIT) >= 0) {
            MonkeyWaitEvent e21 = new MonkeyWaitEvent(this.mProfileWaitTime);
            this.mQ.addLast((MonkeyEvent) e21);
            return;
        }
        if (s.indexOf(EVENT_KEYWORD_KEYPRESS) >= 0 && args.length == 1) {
            String key_name = args[0];
            int keyCode = MonkeySourceRandom.getKeyCode(key_name);
            if (keyCode != 0) {
                MonkeyKeyEvent e23 = new MonkeyKeyEvent(0, keyCode);
                this.mQ.addLast((MonkeyEvent) e23);
                MonkeyKeyEvent e24 = new MonkeyKeyEvent(1, keyCode);
                this.mQ.addLast((MonkeyEvent) e24);
                return;
            }
            return;
        }
        if (s.indexOf(EVENT_KEYWORD_LONGPRESS) >= 0) {
            MonkeyKeyEvent e25 = new MonkeyKeyEvent(0, 23);
            this.mQ.addLast((MonkeyEvent) e25);
            MonkeyWaitEvent we = new MonkeyWaitEvent(LONGPRESS_WAIT_TIME);
            this.mQ.addLast((MonkeyEvent) we);
            MonkeyKeyEvent e26 = new MonkeyKeyEvent(1, 23);
            this.mQ.addLast((MonkeyEvent) e26);
        }
        if (s.indexOf(EVENT_KEYWORD_POWERLOG) >= 0 && args.length > 0) {
            String power_log_type = args[0];
            if (args.length == 1) {
                MonkeyPowerEvent e27 = new MonkeyPowerEvent(power_log_type);
                this.mQ.addLast((MonkeyEvent) e27);
            } else if (args.length == 2) {
                String test_case_status = args[1];
                MonkeyPowerEvent e28 = new MonkeyPowerEvent(power_log_type, test_case_status);
                this.mQ.addLast((MonkeyEvent) e28);
            }
        }
        if (s.indexOf(EVENT_KEYWORD_WRITEPOWERLOG) >= 0) {
            MonkeyPowerEvent e29 = new MonkeyPowerEvent();
            this.mQ.addLast((MonkeyEvent) e29);
        }
        if (s.indexOf(EVENT_KEYWORD_RUNCMD) >= 0 && args.length == 1) {
            String cmd = args[0];
            MonkeyCommandEvent e30 = new MonkeyCommandEvent(cmd);
            this.mQ.addLast((MonkeyEvent) e30);
        }
        if (s.indexOf(EVENT_KEYWORD_INPUT_STRING) >= 0 && args.length == 1) {
            String input = args[0];
            String cmd2 = "input text " + input;
            MonkeyCommandEvent e31 = new MonkeyCommandEvent(cmd2);
            this.mQ.addLast((MonkeyEvent) e31);
            return;
        }
        if (s.indexOf(EVENT_KEYWORD_START_FRAMERATE_CAPTURE) >= 0) {
            MonkeyGetFrameRateEvent e32 = new MonkeyGetFrameRateEvent("start");
            this.mQ.addLast((MonkeyEvent) e32);
            return;
        }
        if (s.indexOf(EVENT_KEYWORD_END_FRAMERATE_CAPTURE) >= 0 && args.length == 1) {
            String input2 = args[0];
            MonkeyGetFrameRateEvent e33 = new MonkeyGetFrameRateEvent("end", input2);
            this.mQ.addLast((MonkeyEvent) e33);
        } else if (s.indexOf(EVENT_KEYWORD_START_APP_FRAMERATE_CAPTURE) >= 0 && args.length == 1) {
            String app = args[0];
            MonkeyGetAppFrameRateEvent e34 = new MonkeyGetAppFrameRateEvent("start", app);
            this.mQ.addLast((MonkeyEvent) e34);
        } else if (s.indexOf(EVENT_KEYWORD_END_APP_FRAMERATE_CAPTURE) >= 0 && args.length == 2) {
            String app2 = args[0];
            String label = args[1];
            MonkeyGetAppFrameRateEvent e35 = new MonkeyGetAppFrameRateEvent("end", app2, label);
            this.mQ.addLast((MonkeyEvent) e35);
        }
    }

    private void processLine(String line) {
        int index1 = line.indexOf(40);
        int index2 = line.indexOf(41);
        if (index1 >= 0 && index2 >= 0) {
            String[] args = line.substring(index1 + 1, index2).split(",");
            for (int i = 0; i < args.length; i++) {
                args[i] = args[i].trim();
            }
            handleEvent(line, args);
        }
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
        int linesRead;
        if (!this.mFileOpened) {
            resetValue();
            readHeader();
        }
        if (this.mReadScriptLineByLine) {
            linesRead = readOneLine();
        } else {
            linesRead = readLines();
        }
        if (linesRead == 0) {
            closeFile();
        }
    }

    private void needSleep(long time) {
        if (time >= 1) {
            try {
                Thread.sleep(time);
            } catch (InterruptedException e) {
            }
        }
    }

    @Override
    public boolean validate() {
        try {
            boolean validHeader = readHeader();
            closeFile();
            if (this.mVerbose > 0) {
                System.out.println("Replaying " + this.mEventCountInScript + " events with speed " + this.mSpeed);
                return validHeader;
            }
            return validHeader;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public void setVerbose(int verbose) {
        this.mVerbose = verbose;
    }

    private void adjustKeyEventTime(MonkeyKeyEvent e) {
        long thisDownTime;
        long thisEventTime;
        if (e.getEventTime() >= 0) {
            if (this.mLastRecordedEventTime <= 0) {
                thisDownTime = SystemClock.uptimeMillis();
                thisEventTime = thisDownTime;
            } else {
                if (e.getDownTime() != this.mLastRecordedDownTimeKey) {
                    thisDownTime = e.getDownTime();
                } else {
                    thisDownTime = this.mLastExportDownTimeKey;
                }
                long expectedDelay = (long) ((e.getEventTime() - this.mLastRecordedEventTime) * this.mSpeed);
                thisEventTime = this.mLastExportEventTime + expectedDelay;
                needSleep(expectedDelay - SLEEP_COMPENSATE_DIFF);
            }
            this.mLastRecordedDownTimeKey = e.getDownTime();
            this.mLastRecordedEventTime = e.getEventTime();
            e.setDownTime(thisDownTime);
            e.setEventTime(thisEventTime);
            this.mLastExportDownTimeKey = thisDownTime;
            this.mLastExportEventTime = thisEventTime;
        }
    }

    private void adjustMotionEventTime(MonkeyMotionEvent e) {
        long thisEventTime = SystemClock.uptimeMillis();
        long thisDownTime = e.getDownTime();
        if (thisDownTime == this.mLastRecordedDownTimeMotion) {
            e.setDownTime(this.mLastExportDownTimeMotion);
        } else {
            this.mLastRecordedDownTimeMotion = thisDownTime;
            e.setDownTime(thisEventTime);
            this.mLastExportDownTimeMotion = thisEventTime;
        }
        e.setEventTime(thisEventTime);
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
            MonkeyEvent ev = this.mQ.getFirst();
            this.mQ.removeFirst();
            if (ev.getEventType() == 0) {
                adjustKeyEventTime((MonkeyKeyEvent) ev);
                return ev;
            }
            if (ev.getEventType() == 1 || ev.getEventType() == 2) {
                adjustMotionEventTime((MonkeyMotionEvent) ev);
                return ev;
            }
            return ev;
        } catch (NoSuchElementException e2) {
            return null;
        }
    }
}
