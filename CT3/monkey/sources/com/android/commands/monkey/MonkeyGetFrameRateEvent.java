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

    public MonkeyGetFrameRateEvent(String status, String testCaseName) {
        super(4);
        this.GET_FRAMERATE_CMD = "service call SurfaceFlinger 1013";
        this.mStatus = status;
        mTestCaseName = testCaseName;
    }

    public MonkeyGetFrameRateEvent(String status) {
        super(4);
        this.GET_FRAMERATE_CMD = "service call SurfaceFlinger 1013";
        this.mStatus = status;
    }

    private float getAverageFrameRate(int totalNumberOfFrame, float duration) {
        if (duration <= 0.0f) {
            return 0.0f;
        }
        float avgFrameRate = totalNumberOfFrame / duration;
        return avgFrameRate;
    }

    private void writeAverageFrameRate() throws Throwable {
        FileWriter writer;
        FileWriter writer2 = null;
        try {
            try {
                writer = new FileWriter(LOG_FILE, true);
            } catch (IOException e) {
                e = e;
            }
        } catch (Throwable th) {
            th = th;
        }
        try {
            int totalNumberOfFrame = mEndFrameNo - mStartFrameNo;
            float avgFrameRate = getAverageFrameRate(totalNumberOfFrame, mDuration);
            writer.write(String.format("%s:%.2f\n", mTestCaseName, Float.valueOf(avgFrameRate)));
            writer.close();
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e2) {
                    Log.e(TAG, "IOException " + e2.toString());
                }
            }
        } catch (IOException e3) {
            e = e3;
            writer2 = writer;
            Log.w(TAG, "Can't write sdcard log file", e);
            if (writer2 != null) {
                try {
                    writer2.close();
                } catch (IOException e4) {
                    Log.e(TAG, "IOException " + e4.toString());
                }
            }
        } catch (Throwable th2) {
            th = th2;
            writer2 = writer;
            if (writer2 != null) {
                try {
                    writer2.close();
                } catch (IOException e5) {
                    Log.e(TAG, "IOException " + e5.toString());
                }
            }
            throw th;
        }
    }

    private String getNumberOfFrames(String input) {
        Matcher m = NO_OF_FRAMES_PATTERN.matcher(input);
        if (!m.matches()) {
            return null;
        }
        String noOfFrames = m.group(1);
        return noOfFrames;
    }

    @Override
    public int injectEvent(IWindowManager iwm, IActivityManager iam, int verbose) throws Throwable {
        BufferedReader result;
        Process p = null;
        BufferedReader bufferedReader = null;
        try {
            try {
                p = Runtime.getRuntime().exec(this.GET_FRAMERATE_CMD);
                int status = p.waitFor();
                if (status != 0) {
                    System.err.println(String.format("// Shell command %s status was %s", this.GET_FRAMERATE_CMD, Integer.valueOf(status)));
                }
                result = new BufferedReader(new InputStreamReader(p.getInputStream()));
            } catch (Throwable th) {
                th = th;
            }
        } catch (Exception e) {
            e = e;
        }
        try {
            String output = result.readLine();
            if (output != null) {
                if (this.mStatus == "start") {
                    mStartFrameNo = Integer.parseInt(getNumberOfFrames(output), 16);
                    mStartTime = System.currentTimeMillis();
                } else if (this.mStatus == "end") {
                    mEndFrameNo = Integer.parseInt(getNumberOfFrames(output), 16);
                    mEndTime = System.currentTimeMillis();
                    long diff = mEndTime - mStartTime;
                    mDuration = (float) (diff / 1000.0d);
                    writeAverageFrameRate();
                }
            }
            if (result != null) {
                try {
                    result.close();
                } catch (IOException e2) {
                    System.err.println(e2.toString());
                }
            }
            if (p != null) {
                p.destroy();
            }
            return 1;
        } catch (Exception e3) {
            e = e3;
            bufferedReader = result;
            System.err.println("// Exception from " + this.GET_FRAMERATE_CMD + ":");
            System.err.println(e.toString());
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e4) {
                    System.err.println(e4.toString());
                    return 1;
                }
            }
            if (p != null) {
                p.destroy();
                return 1;
            }
            return 1;
        } catch (Throwable th2) {
            th = th2;
            bufferedReader = result;
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e5) {
                    System.err.println(e5.toString());
                    throw th;
                }
            }
            if (p != null) {
                p.destroy();
            }
            throw th;
        }
    }
}
