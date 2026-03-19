package com.android.commands.monkey;

import android.app.IActivityManager;
import android.util.Log;
import android.view.IWindowManager;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MonkeyGetFrameRateEvent extends MonkeyEvent {
    private static final String LOG_FILE = "/sdcard/avgFrameRateOut.txt";
    private static final String TAG = "MonkeyGetFrameRateEvent";
    private static float mDuration;
    private static int mEndFrameNo;
    private static long mEndTime;
    private static int mStartFrameNo;
    private static long mStartTime;
    private String GET_FRAMERATE_CMD;
    private String mStatus;
    private static String mTestCaseName = null;
    private static final Pattern NO_OF_FRAMES_PATTERN = Pattern.compile(".*\\(([a-f[A-F][0-9]].*?)\\s.*\\)");

    public MonkeyGetFrameRateEvent(String str, String str2) {
        super(4);
        this.GET_FRAMERATE_CMD = "service call SurfaceFlinger 1013";
        this.mStatus = str;
        mTestCaseName = str2;
    }

    public MonkeyGetFrameRateEvent(String str) {
        super(4);
        this.GET_FRAMERATE_CMD = "service call SurfaceFlinger 1013";
        this.mStatus = str;
    }

    private float getAverageFrameRate(int i, float f) {
        if (f <= 0.0f) {
            return 0.0f;
        }
        return i / f;
    }

    private void writeAverageFrameRate() throws Throwable {
        FileWriter fileWriter;
        Throwable th;
        IOException e;
        String str;
        StringBuilder sb;
        try {
            try {
                fileWriter = new FileWriter(LOG_FILE, true);
                try {
                    fileWriter.write(String.format("%s:%.2f\n", mTestCaseName, Float.valueOf(getAverageFrameRate(mEndFrameNo - mStartFrameNo, mDuration))));
                    fileWriter.close();
                    try {
                        fileWriter.close();
                    } catch (IOException e2) {
                        e = e2;
                        str = TAG;
                        sb = new StringBuilder();
                        sb.append("IOException ");
                        sb.append(e.toString());
                        Log.e(str, sb.toString());
                    }
                } catch (IOException e3) {
                    e = e3;
                    Log.w(TAG, "Can't write sdcard log file", e);
                    if (fileWriter != null) {
                        try {
                            fileWriter.close();
                        } catch (IOException e4) {
                            e = e4;
                            str = TAG;
                            sb = new StringBuilder();
                            sb.append("IOException ");
                            sb.append(e.toString());
                            Log.e(str, sb.toString());
                        }
                    }
                }
            } catch (Throwable th2) {
                th = th2;
                if (fileWriter != null) {
                    try {
                        fileWriter.close();
                    } catch (IOException e5) {
                        Log.e(TAG, "IOException " + e5.toString());
                    }
                }
                throw th;
            }
        } catch (IOException e6) {
            fileWriter = null;
            e = e6;
        } catch (Throwable th3) {
            fileWriter = null;
            th = th3;
            if (fileWriter != null) {
            }
            throw th;
        }
    }

    private String getNumberOfFrames(String str) {
        Matcher matcher = NO_OF_FRAMES_PATTERN.matcher(str);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }

    @Override
    public int injectEvent(IWindowManager iWindowManager, IActivityManager iActivityManager, int i) throws Throwable {
        BufferedReader bufferedReader;
        Exception e;
        BufferedReader bufferedReader2;
        try {
            try {
                try {
                    i = Runtime.getRuntime().exec(this.GET_FRAMERATE_CMD);
                    try {
                        int iWaitFor = i.waitFor();
                        if (iWaitFor != 0) {
                            Logger.err.println(String.format("// Shell command %s status was %s", this.GET_FRAMERATE_CMD, Integer.valueOf(iWaitFor)));
                        }
                        bufferedReader2 = new BufferedReader(new InputStreamReader(i.getInputStream()));
                        try {
                            String line = bufferedReader2.readLine();
                            if (line != null) {
                                if (this.mStatus == "start") {
                                    mStartFrameNo = Integer.parseInt(getNumberOfFrames(line), 16);
                                    mStartTime = System.currentTimeMillis();
                                } else if (this.mStatus == "end") {
                                    mEndFrameNo = Integer.parseInt(getNumberOfFrames(line), 16);
                                    mEndTime = System.currentTimeMillis();
                                    mDuration = (float) ((mEndTime - mStartTime) / 1000.0d);
                                    writeAverageFrameRate();
                                }
                            }
                            bufferedReader2.close();
                            if (i != 0) {
                                i.destroy();
                            }
                        } catch (Exception e2) {
                            e = e2;
                            Logger.err.println("// Exception from " + this.GET_FRAMERATE_CMD + ":");
                            Logger.err.println(e.toString());
                            if (bufferedReader2 != null) {
                                bufferedReader2.close();
                            }
                            if (i != 0) {
                                i.destroy();
                            }
                        }
                    } catch (Exception e3) {
                        bufferedReader2 = null;
                        e = e3;
                    } catch (Throwable th) {
                        th = th;
                        bufferedReader = null;
                        if (bufferedReader != null) {
                            try {
                                bufferedReader.close();
                            } catch (IOException e4) {
                                Logger.err.println(e4.toString());
                                throw th;
                            }
                        }
                        if (i != 0) {
                            i.destroy();
                        }
                        throw th;
                    }
                } catch (IOException e5) {
                    Logger.err.println(e5.toString());
                }
            } catch (Exception e6) {
                i = 0;
                e = e6;
                bufferedReader2 = null;
            } catch (Throwable th2) {
                th = th2;
                i = 0;
                bufferedReader = null;
            }
            return 1;
        } catch (Throwable th3) {
            th = th3;
        }
    }
}
