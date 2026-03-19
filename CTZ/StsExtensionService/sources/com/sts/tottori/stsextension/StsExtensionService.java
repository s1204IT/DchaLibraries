package com.sts.tottori.stsextension;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.IStsExtensionService;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Log;
import com.sts.tottori.stsextension.StsExtensionService;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class StsExtensionService extends Service {
    private int tp_type;
    static final File PROC_NVT_TP_VERSION = new File("/proc/nvt_fw_version");
    static final File FTS_TP_VERSION = new File("/sys/class/i2c-dev/i2c-3/device/3-0038/fts_fw_version");
    private boolean mIsUpdating = false;
    PowerManager mPowerManager = null;
    IStsExtensionService.Stub mStub = new AnonymousClass1();
    Handler mHandler = new Handler(true);
    Context mContext = this;

    public StsExtensionService() {
        this.tp_type = -1;
        if (!PROC_NVT_TP_VERSION.exists()) {
            if (!FTS_TP_VERSION.exists()) {
                Log.e("StsExtensionService", "----- TP:Unkown -----");
                return;
            } else {
                Log.i("StsExtensionService", "----- TP:FTS -----");
                this.tp_type = 1;
                return;
            }
        }
        Log.i("StsExtensionService", "----- TP:NVT -----");
        this.tp_type = 0;
    }

    PowerManager getPowerManager() {
        if (this.mPowerManager == null) {
            this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        }
        return this.mPowerManager;
    }

    class AnonymousClass1 extends IStsExtensionService.Stub {
        AnonymousClass1() {
        }

        public boolean updateTouchpanelFw(final String str) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                if (!new File(str).isFile()) {
                    Log.e("StsExtensionService", "----- putString() : invalid file[" + str + "] -----");
                    return false;
                }
                if (StsExtensionService.this.mIsUpdating) {
                    Log.e("StsExtensionService", "----- FW update : already updating! -----");
                    return false;
                }
                StsExtensionService.this.mIsUpdating = true;
                Log.e("StsExtensionService", "----- updateTouchpanelFw ----- " + StsExtensionService.this.tp_type);
                if (StsExtensionService.this.tp_type == 0) {
                    new Thread(new Runnable() {
                        @Override
                        public final void run() throws Throwable {
                            StsExtensionService.AnonymousClass1.lambda$updateTouchpanelFw$1(this.f$0, str);
                        }
                    }).start();
                } else {
                    String strSubstring = str.substring(str.lastIndexOf("/") + 1);
                    if (strSubstring.length() >= 95) {
                        Log.e("StsExtensionService", "----- filename length(" + strSubstring.length() + ") fail -----");
                        StsExtensionService.this.mIsUpdating = false;
                        return false;
                    }
                    if (strSubstring.indexOf("FT8205") != 0) {
                        Log.e("StsExtensionService", "----- invalid file name [" + strSubstring + "] -----");
                        StsExtensionService.this.mIsUpdating = false;
                        return false;
                    }
                    new Thread(new Runnable() {
                        @Override
                        public final void run() throws Throwable {
                            StsExtensionService.AnonymousClass1.lambda$updateTouchpanelFw$3(this.f$0, str);
                        }
                    }).start();
                }
                return true;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public static void lambda$updateTouchpanelFw$1(final AnonymousClass1 anonymousClass1, String str) throws Throwable {
            String str2;
            Handler handler;
            Runnable runnable;
            Handler handler2;
            Runnable runnable2;
            Process processStart;
            Throwable th;
            Throwable th2;
            String str3;
            InputStream inputStream;
            InputStreamReader inputStreamReader;
            Throwable th3;
            Throwable th4;
            Throwable th5;
            Process process = null;
            th = null;
            th = null;
            Throwable th6 = null;
            process = null;
            try {
                StsExtensionService.this.getPowerManager().setKeepAwake(true);
                SystemProperties.set("nvt.nvt_fw_updating", "1");
                processStart = new ProcessBuilder("/bin/.NT36523_Cmd_v208", "-u", str).start();
                try {
                    inputStream = processStart.getInputStream();
                    try {
                        inputStreamReader = new InputStreamReader(inputStream);
                    } catch (Throwable th7) {
                        th = th7;
                        str2 = null;
                    }
                } catch (Throwable th8) {
                    str2 = null;
                    process = processStart;
                    th = th8;
                    try {
                        if (process == null) {
                        }
                        handler2.post(runnable2);
                        throw th;
                    } finally {
                    }
                }
            } catch (Throwable th9) {
                th = th9;
                str2 = null;
            }
            try {
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                str3 = null;
                while (true) {
                    try {
                        try {
                            String line = bufferedReader.readLine();
                            if (line == null) {
                                break;
                            } else {
                                str3 = line;
                            }
                        } catch (Throwable th10) {
                            try {
                                throw th10;
                            } catch (Throwable th11) {
                                th4 = th10;
                                th5 = th11;
                                $closeResource(th4, bufferedReader);
                                throw th5;
                            }
                        }
                    } catch (Throwable th12) {
                        th = th12;
                        try {
                            throw th;
                        } catch (Throwable th13) {
                            String str4 = str3;
                            th3 = th;
                            th = th13;
                            str2 = str4;
                            $closeResource(th3, inputStreamReader);
                            throw th;
                        }
                    }
                }
                $closeResource(null, bufferedReader);
                try {
                    $closeResource(null, inputStreamReader);
                    if (inputStream != null) {
                        try {
                            $closeResource(null, inputStream);
                        } catch (Throwable th14) {
                            th2 = th14;
                            str2 = str3;
                            Throwable th15 = th2;
                            process = processStart;
                            th = th15;
                            if (process == null) {
                            }
                            handler2.post(runnable2);
                            throw th;
                        }
                    }
                    if (processStart == null) {
                        try {
                            processStart.waitFor();
                            i = "Verify OK ".equals(str3) ? 0 : -1;
                            handler = StsExtensionService.this.mHandler;
                            runnable = new Runnable() {
                                @Override
                                public final void run() {
                                    StsExtensionService.AnonymousClass1.lambda$updateTouchpanelFw$0(this.f$0, i);
                                }
                            };
                        } catch (Throwable th16) {
                            Log.e("StsExtensionService", "----- Exception occurred!!! -----", th16);
                            i = "Verify OK ".equals(str3) ? 0 : -1;
                            handler = StsExtensionService.this.mHandler;
                            runnable = new Runnable() {
                                @Override
                                public final void run() {
                                    StsExtensionService.AnonymousClass1.lambda$updateTouchpanelFw$0(this.f$0, i);
                                }
                            };
                        }
                    } else {
                        if ("Verify OK ".equals(str3)) {
                        }
                        handler = StsExtensionService.this.mHandler;
                        runnable = new Runnable() {
                            @Override
                            public final void run() {
                                StsExtensionService.AnonymousClass1.lambda$updateTouchpanelFw$0(this.f$0, i);
                            }
                        };
                    }
                    handler.post(runnable);
                } catch (Throwable th17) {
                    th = th17;
                    str2 = str3;
                    if (inputStream != null) {
                    }
                    throw th;
                }
            } catch (Throwable th18) {
                th = th18;
                str3 = null;
            }
        }

        public static void lambda$updateTouchpanelFw$0(AnonymousClass1 anonymousClass1, int i) {
            SystemProperties.set("nvt.nvt_fw_updating", "0");
            StsExtensionService.this.getPowerManager().setKeepAwake(false);
            StsExtensionService.this.mIsUpdating = false;
            StsExtensionService.this.mContext.sendBroadcastAsUser(new Intent("com.panasonic.sanyo.ts.intent.action.TOUCHPANEL_FIRMWARE_UPDATED").putExtra("result", i), UserHandle.ALL);
        }

        public static void lambda$updateTouchpanelFw$3(final AnonymousClass1 anonymousClass1, String str) throws Throwable {
            ?? bufferedWriter;
            OutputStreamWriter outputStreamWriter;
            ?? fileOutputStream;
            Object obj = null;
             = 0;
             = 0;
            ?? r0 = 0;
            try {
                try {
                    try {
                        StsExtensionService.this.getPowerManager().setKeepAwake(true);
                        fileOutputStream = new FileOutputStream("/sys/devices/platform/soc/1100f000.i2c/i2c-3/3-0038/fts_upgrade_bin");
                    } catch (Throwable th) {
                        th = th;
                        bufferedWriter = obj;
                    }
                } catch (Exception e) {
                    e = e;
                    fileOutputStream = 0;
                    outputStreamWriter = null;
                } catch (Throwable th2) {
                    th = th2;
                    fileOutputStream = 0;
                    outputStreamWriter = null;
                }
                try {
                    outputStreamWriter = new OutputStreamWriter((OutputStream) fileOutputStream, "UTF-8");
                    try {
                        bufferedWriter = new BufferedWriter(outputStreamWriter);
                    } catch (Exception e2) {
                        e = e2;
                    }
                    try {
                        Log.e("StsExtensionService", "----- fts_upgrade_bin ----- " + str.substring(str.indexOf("FT8205")));
                        bufferedWriter.write(str.substring(str.indexOf("FT8205")));
                        bufferedWriter.write("\n");
                        bufferedWriter.close();
                        outputStreamWriter.close();
                        fileOutputStream.close();
                        Handler handler = StsExtensionService.this.mHandler;
                        Runnable runnable = new Runnable() {
                            @Override
                            public final void run() {
                                StsExtensionService.AnonymousClass1.lambda$updateTouchpanelFw$2(this.f$0);
                            }
                        };
                        handler.post(runnable);
                        obj = runnable;
                        fileOutputStream = fileOutputStream;
                    } catch (Exception e3) {
                        e = e3;
                        r0 = bufferedWriter;
                        Log.e("StsExtensionService", "----- Exception occurred!!! -----", e);
                        if (r0 != 0) {
                            r0.close();
                        }
                        if (outputStreamWriter != null) {
                            outputStreamWriter.close();
                        }
                        if (fileOutputStream != 0) {
                            fileOutputStream.close();
                        }
                        Handler handler2 = StsExtensionService.this.mHandler;
                        Runnable runnable2 = new Runnable() {
                            @Override
                            public final void run() {
                                StsExtensionService.AnonymousClass1.lambda$updateTouchpanelFw$2(this.f$0);
                            }
                        };
                        handler2.post(runnable2);
                        obj = runnable2;
                        fileOutputStream = fileOutputStream;
                    } catch (Throwable th3) {
                        th = th3;
                        if (bufferedWriter != 0) {
                            try {
                                bufferedWriter.close();
                            } catch (Exception e4) {
                                Log.e("StsExtensionService", "----- Exception occurred!!! -----", e4);
                                throw th;
                            }
                        }
                        if (outputStreamWriter != null) {
                            outputStreamWriter.close();
                        }
                        if (fileOutputStream != 0) {
                            fileOutputStream.close();
                        }
                        StsExtensionService.this.mHandler.post(new Runnable() {
                            @Override
                            public final void run() {
                                StsExtensionService.AnonymousClass1.lambda$updateTouchpanelFw$2(this.f$0);
                            }
                        });
                        throw th;
                    }
                } catch (Exception e5) {
                    e = e5;
                    outputStreamWriter = null;
                } catch (Throwable th4) {
                    th = th4;
                    outputStreamWriter = null;
                    fileOutputStream = fileOutputStream;
                    bufferedWriter = outputStreamWriter;
                    if (bufferedWriter != 0) {
                    }
                    if (outputStreamWriter != null) {
                    }
                    if (fileOutputStream != 0) {
                    }
                    StsExtensionService.this.mHandler.post(new Runnable() {
                        @Override
                        public final void run() {
                            StsExtensionService.AnonymousClass1.lambda$updateTouchpanelFw$2(this.f$0);
                        }
                    });
                    throw th;
                }
            } catch (Exception e6) {
                Log.e("StsExtensionService", "----- Exception occurred!!! -----", e6);
                obj = "StsExtensionService";
                fileOutputStream = "----- Exception occurred!!! -----";
            }
        }

        public static void lambda$updateTouchpanelFw$2(AnonymousClass1 anonymousClass1) {
            StsExtensionService.this.getPowerManager().setKeepAwake(false);
            StsExtensionService.this.mIsUpdating = false;
            StsExtensionService.this.mContext.sendBroadcastAsUser(new Intent("com.panasonic.sanyo.ts.intent.action.TOUCHPANEL_FIRMWARE_UPDATED").putExtra("result", 0), UserHandle.ALL);
        }

        public String getTouchpanelVersion() {
            FileReader fileReader;
            Throwable th;
            Throwable th2;
            Throwable th3;
            Throwable th4;
            if (StsExtensionService.this.mIsUpdating) {
                return null;
            }
            ?? Exists = StsExtensionService.PROC_NVT_TP_VERSION.exists();
            if (Exists == 0) {
                if (!StsExtensionService.FTS_TP_VERSION.exists()) {
                    return "";
                }
                try {
                    fileReader = new FileReader(StsExtensionService.FTS_TP_VERSION);
                    try {
                        BufferedReader bufferedReader = new BufferedReader(fileReader);
                        try {
                            String line = bufferedReader.readLine();
                            $closeResource(null, bufferedReader);
                            return line;
                        } catch (Throwable th5) {
                            try {
                                throw th5;
                            } catch (Throwable th6) {
                                th3 = th5;
                                th4 = th6;
                                $closeResource(th3, bufferedReader);
                                throw th4;
                            }
                        }
                    } finally {
                        $closeResource(null, fileReader);
                    }
                } catch (Throwable th7) {
                    return "";
                }
            }
            try {
                try {
                    fileReader = new FileReader(StsExtensionService.PROC_NVT_TP_VERSION);
                    BufferedReader bufferedReader2 = new BufferedReader(fileReader);
                    try {
                        String line2 = bufferedReader2.readLine();
                        $closeResource(null, bufferedReader2);
                        int iIndexOf = line2.indexOf("fw_ver=");
                        int iIndexOf2 = line2.indexOf(",");
                        if (iIndexOf == -1 || iIndexOf2 == -1) {
                            return "";
                        }
                        return "0x" + Integer.toHexString(Integer.parseInt(line2.substring(iIndexOf + "fw_ver=".length(), iIndexOf2)));
                    } catch (Throwable th8) {
                        try {
                            throw th8;
                        } catch (Throwable th9) {
                            th = th8;
                            th2 = th9;
                            $closeResource(th, bufferedReader2);
                            throw th2;
                        }
                    }
                } catch (Throwable th10) {
                    $closeResource(null, Exists);
                    throw th10;
                }
            } catch (Throwable th11) {
                return "";
            }
        }

        private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
            if (th == null) {
                autoCloseable.close();
                return;
            }
            try {
                autoCloseable.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
        }

        public int getPenBattery() {
            return SystemProperties.getInt("persist.sys.nvt.penbattery", 0);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return this.mStub;
    }
}
