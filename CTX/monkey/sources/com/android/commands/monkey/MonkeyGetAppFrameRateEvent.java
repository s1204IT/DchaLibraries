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

    public MonkeyGetAppFrameRateEvent(String str, String str2, String str3) {
        super(4);
        this.GET_APP_FRAMERATE_TMPL = "dumpsys gfxinfo %s";
        this.mStatus = str;
        sActivityName = str2;
        sTestCaseName = str3;
    }

    public MonkeyGetAppFrameRateEvent(String str, String str2) {
        super(4);
        this.GET_APP_FRAMERATE_TMPL = "dumpsys gfxinfo %s";
        this.mStatus = str;
        sActivityName = str2;
    }

    public MonkeyGetAppFrameRateEvent(String str) {
        super(4);
        this.GET_APP_FRAMERATE_TMPL = "dumpsys gfxinfo %s";
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
            Log.w(TAG, "file: " + LOG_FILE);
            fileWriter = new FileWriter(LOG_FILE, true);
            try {
                try {
                    fileWriter.write(String.format("%s:%.2f\n", sTestCaseName, Float.valueOf(getAverageFrameRate(sEndFrameNo - sStartFrameNo, sDuration))));
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

    private String getNumberOfFrames(BufferedReader bufferedReader) throws IOException {
        Matcher matcher;
        do {
            String line = bufferedReader.readLine();
            if (line != null) {
                matcher = NO_OF_FRAMES_PATTERN.matcher(line);
            } else {
                return null;
            }
        } while (!matcher.matches());
        return matcher.group(1);
    }

    @Override
    public int injectEvent(IWindowManager iWindowManager, IActivityManager iActivityManager, int i) throws Throwable {
        Exception e;
        BufferedReader bufferedReader;
        String str = this.GET_APP_FRAMERATE_TMPL;
        Process processExec = sActivityName;
        BufferedReader bufferedReader2 = null;
        String str2 = String.format(str, processExec);
        try {
            try {
                try {
                    processExec = Runtime.getRuntime().exec(str2);
                } catch (Throwable th) {
                    th = th;
                }
            } catch (Exception e2) {
                processExec = 0;
                e = e2;
                bufferedReader = null;
            } catch (Throwable th2) {
                th = th2;
                processExec = 0;
                bufferedReader2 = null;
            }
            try {
                int iWaitFor = processExec.waitFor();
                if (iWaitFor != 0) {
                    Logger.err.println(String.format("// Shell command %s status was %s", str2, Integer.valueOf(iWaitFor)));
                }
                bufferedReader = new BufferedReader(new InputStreamReader(processExec.getInputStream()));
                try {
                    String numberOfFrames = getNumberOfFrames(bufferedReader);
                    if (numberOfFrames != null) {
                        if ("start".equals(this.mStatus)) {
                            sStartFrameNo = Integer.parseInt(numberOfFrames);
                            sStartTime = System.currentTimeMillis();
                        } else if ("end".equals(this.mStatus)) {
                            sEndFrameNo = Integer.parseInt(numberOfFrames);
                            sEndTime = System.currentTimeMillis();
                            sDuration = (float) ((sEndTime - sStartTime) / 1000.0d);
                            writeAverageFrameRate();
                        }
                    }
                    bufferedReader.close();
                    if (processExec != 0) {
                        processExec.destroy();
                    }
                } catch (Exception e3) {
                    e = e3;
                    Logger.err.println("// Exception from " + str2 + ":");
                    Logger.err.println(e.toString());
                    if (bufferedReader != null) {
                        bufferedReader.close();
                    }
                    if (processExec != 0) {
                        processExec.destroy();
                    }
                }
            } catch (Exception e4) {
                bufferedReader = null;
                e = e4;
            } catch (Throwable th3) {
                th = th3;
                bufferedReader2 = null;
                if (bufferedReader2 != null) {
                    try {
                        bufferedReader2.close();
                    } catch (IOException e5) {
                        Logger.err.println(e5.toString());
                        throw th;
                    }
                }
                if (processExec != 0) {
                    processExec.destroy();
                }
                throw th;
            }
        } catch (IOException e6) {
            Logger.err.println(e6.toString());
        }
        return 1;
    }
}
