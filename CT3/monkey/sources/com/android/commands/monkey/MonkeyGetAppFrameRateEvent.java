package com.android.commands.monkey;

import android.app.IActivityManager;
import android.os.Environment;
import android.util.Log;
import android.view.IWindowManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MonkeyGetAppFrameRateEvent extends MonkeyEvent {
    private static final String TAG = "MonkeyGetAppFrameRateEvent";
    private static float sDuration;
    private static int sEndFrameNo;
    private static long sEndTime;
    private static int sStartFrameNo;
    private static long sStartTime;
    private String GET_APP_FRAMERATE_TMPL;
    private String mStatus;
    private static String sActivityName = null;
    private static String sTestCaseName = null;
    private static final String LOG_FILE = new File(Environment.getExternalStorageDirectory(), "avgAppFrameRateOut.txt").getAbsolutePath();
    private static final Pattern NO_OF_FRAMES_PATTERN = Pattern.compile(".* ([0-9]*) frames rendered");

    public MonkeyGetAppFrameRateEvent(String status, String activityName, String testCaseName) {
        super(4);
        this.GET_APP_FRAMERATE_TMPL = "dumpsys gfxinfo %s";
        this.mStatus = status;
        sActivityName = activityName;
        sTestCaseName = testCaseName;
    }

    public MonkeyGetAppFrameRateEvent(String status, String activityName) {
        super(4);
        this.GET_APP_FRAMERATE_TMPL = "dumpsys gfxinfo %s";
        this.mStatus = status;
        sActivityName = activityName;
    }

    public MonkeyGetAppFrameRateEvent(String status) {
        super(4);
        this.GET_APP_FRAMERATE_TMPL = "dumpsys gfxinfo %s";
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
                Log.w(TAG, "file: " + LOG_FILE);
                writer = new FileWriter(LOG_FILE, true);
            } catch (Throwable th) {
                th = th;
            }
        } catch (IOException e) {
            e = e;
        }
        try {
            int totalNumberOfFrame = sEndFrameNo - sStartFrameNo;
            float avgFrameRate = getAverageFrameRate(totalNumberOfFrame, sDuration);
            writer.write(String.format("%s:%.2f\n", sTestCaseName, Float.valueOf(avgFrameRate)));
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e2) {
                    Log.e(TAG, "IOException " + e2.toString());
                }
            }
            writer2 = writer;
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

    private String getNumberOfFrames(BufferedReader reader) throws IOException {
        Matcher m;
        do {
            String line = reader.readLine();
            if (line == null) {
                return null;
            }
            m = NO_OF_FRAMES_PATTERN.matcher(line);
        } while (!m.matches());
        String noOfFrames = m.group(1);
        return noOfFrames;
    }

    @Override
    public int injectEvent(IWindowManager iwm, IActivityManager iam, int verbose) throws Throwable {
        BufferedReader result;
        Process p = null;
        BufferedReader bufferedReader = null;
        String cmd = String.format(this.GET_APP_FRAMERATE_TMPL, sActivityName);
        try {
            try {
                p = Runtime.getRuntime().exec(cmd);
                int status = p.waitFor();
                if (status != 0) {
                    System.err.println(String.format("// Shell command %s status was %s", cmd, Integer.valueOf(status)));
                }
                result = new BufferedReader(new InputStreamReader(p.getInputStream()));
            } catch (Exception e) {
                e = e;
            }
        } catch (Throwable th) {
            th = th;
        }
        try {
            String output = getNumberOfFrames(result);
            if (output != null) {
                if ("start".equals(this.mStatus)) {
                    sStartFrameNo = Integer.parseInt(output);
                    sStartTime = System.currentTimeMillis();
                } else if ("end".equals(this.mStatus)) {
                    sEndFrameNo = Integer.parseInt(output);
                    sEndTime = System.currentTimeMillis();
                    long diff = sEndTime - sStartTime;
                    sDuration = (float) (diff / 1000.0d);
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
            System.err.println("// Exception from " + cmd + ":");
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
