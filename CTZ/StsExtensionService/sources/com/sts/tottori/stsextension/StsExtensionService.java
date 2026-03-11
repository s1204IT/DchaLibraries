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
import java.io.OutputStreamWriter;

public class StsExtensionService extends Service {
    private int tp_type;
    static final File PROC_NVT_TP_VERSION = new File("/proc/nvt_fw_version");
    static final File FTS_TP_VERSION = new File("/sys/class/i2c-dev/i2c-3/device/3-0038/fts_fw_version");
    private boolean mIsUpdating = false;
    PowerManager mPowerManager = null;
    IStsExtensionService.Stub mStub = new AnonymousClass1(this);
    Handler mHandler = new Handler(true);
    Context mContext = this;

    class AnonymousClass1 extends IStsExtensionService.Stub {
        final StsExtensionService this$0;

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

        AnonymousClass1(StsExtensionService stsExtensionService) {
            this.this$0 = stsExtensionService;
        }

        public static void lambda$updateTouchpanelFw$0(AnonymousClass1 anonymousClass1, int i) {
            SystemProperties.set("nvt.nvt_fw_updating", "0");
            anonymousClass1.this$0.getPowerManager().setKeepAwake(false);
            anonymousClass1.this$0.mIsUpdating = false;
            anonymousClass1.this$0.mContext.sendBroadcastAsUser(new Intent("com.panasonic.sanyo.ts.intent.action.TOUCHPANEL_FIRMWARE_UPDATED").putExtra("result", i), UserHandle.ALL);
        }

        public static void lambda$updateTouchpanelFw$1(final AnonymousClass1 anonymousClass1, String str) throws Throwable {
            String str2;
            Process process;
            Throwable th;
            Runnable runnable;
            Handler handler;
            Handler handler2;
            Runnable runnable2;
            Process processStart;
            Throwable th2;
            Throwable th3;
            String str3;
            Throwable th4;
            Throwable th5;
            Throwable th6;
            try {
                anonymousClass1.this$0.getPowerManager().setKeepAwake(true);
                SystemProperties.set("nvt.nvt_fw_updating", "1");
                processStart = new ProcessBuilder("/bin/.NT36523_Cmd_v208", "-u", str).start();
            } catch (Throwable th7) {
                th = th7;
                str2 = null;
                process = null;
            }
            try {
                InputStream inputStream = processStart.getInputStream();
                try {
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                    try {
                        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                        str2 = null;
                        while (true) {
                            try {
                                try {
                                    String line = bufferedReader.readLine();
                                    if (line == null) {
                                        break;
                                    } else {
                                        str2 = line;
                                    }
                                } catch (Throwable th8) {
                                    th5 = th8;
                                    th = null;
                                    str3 = str2;
                                    $closeResource(th, inputStreamReader);
                                    throw th5;
                                }
                            } catch (Throwable th9) {
                                th = th9;
                                th6 = null;
                                $closeResource(th6, bufferedReader);
                                throw th;
                            }
                        }
                        $closeResource(null, bufferedReader);
                        try {
                            $closeResource(null, inputStreamReader);
                            if (inputStream != null) {
                                try {
                                    $closeResource(null, inputStream);
                                } catch (Throwable th10) {
                                    th2 = th10;
                                    process = processStart;
                                    th = th2;
                                    if (process == null) {
                                    }
                                    handler2.post(runnable2);
                                    throw th;
                                }
                            }
                        } catch (Throwable th11) {
                            th = th11;
                            throw th;
                        }
                    } catch (Throwable th12) {
                        th = th12;
                        str2 = null;
                    }
                } catch (Throwable th13) {
                    th = th13;
                    th3 = null;
                    str2 = null;
                }
            } catch (Throwable th14) {
                th = th14;
                str2 = null;
            }
            try {
                if (processStart == null) {
                    try {
                        processStart.waitFor();
                        i = "Verify OK ".equals(str2) ? 0 : -1;
                        handler = anonymousClass1.this$0.mHandler;
                        runnable = new Runnable(anonymousClass1, i) {
                            private final StsExtensionService.AnonymousClass1 f$0;
                            private final int f$1;

                            {
                                this.f$0 = anonymousClass1;
                                this.f$1 = i;
                            }

                            @Override
                            public final void run() {
                                StsExtensionService.AnonymousClass1.lambda$updateTouchpanelFw$0(this.f$0, this.f$1);
                            }
                        };
                    } catch (Throwable th15) {
                        Log.e("StsExtensionService", "----- Exception occurred!!! -----", th15);
                        final int i = "Verify OK ".equals(str2) ? 0 : -1;
                        Handler handler3 = anonymousClass1.this$0.mHandler;
                        runnable = new Runnable(anonymousClass1, i) {
                            private final StsExtensionService.AnonymousClass1 f$0;
                            private final int f$1;

                            {
                                this.f$0 = anonymousClass1;
                                this.f$1 = i;
                            }

                            @Override
                            public final void run() {
                                StsExtensionService.AnonymousClass1.lambda$updateTouchpanelFw$0(this.f$0, this.f$1);
                            }
                        };
                        handler = handler3;
                    }
                } else {
                    if ("Verify OK ".equals(str2)) {
                    }
                    handler = anonymousClass1.this$0.mHandler;
                    runnable = new Runnable(anonymousClass1, i) {
                        private final StsExtensionService.AnonymousClass1 f$0;
                        private final int f$1;

                        {
                            this.f$0 = anonymousClass1;
                            this.f$1 = i;
                        }

                        @Override
                        public final void run() {
                            StsExtensionService.AnonymousClass1.lambda$updateTouchpanelFw$0(this.f$0, this.f$1);
                        }
                    };
                }
                handler.post(runnable);
            } finally {
            }
        }

        public static void lambda$updateTouchpanelFw$2(AnonymousClass1 anonymousClass1) {
            anonymousClass1.this$0.getPowerManager().setKeepAwake(false);
            anonymousClass1.this$0.mIsUpdating = false;
            anonymousClass1.this$0.mContext.sendBroadcastAsUser(new Intent("com.panasonic.sanyo.ts.intent.action.TOUCHPANEL_FIRMWARE_UPDATED").putExtra("result", 0), UserHandle.ALL);
        }

        public static void lambda$updateTouchpanelFw$3(final AnonymousClass1 anonymousClass1, String str) throws Throwable {
            FileOutputStream fileOutputStream;
            BufferedWriter bufferedWriter;
            Exception e;
            FileOutputStream fileOutputStream2;
            OutputStreamWriter outputStreamWriter;
            BufferedWriter bufferedWriter2;
            Throwable th;
            BufferedWriter bufferedWriter3 = null;
            try {
                try {
                    anonymousClass1.this$0.getPowerManager().setKeepAwake(true);
                    fileOutputStream = new FileOutputStream("/sys/devices/platform/soc/1100f000.i2c/i2c-3/3-0038/fts_upgrade_bin");
                    try {
                        outputStreamWriter = new OutputStreamWriter(fileOutputStream, "UTF-8");
                    } catch (Exception e2) {
                        e = e2;
                        bufferedWriter = null;
                        fileOutputStream2 = fileOutputStream;
                        outputStreamWriter = null;
                    } catch (Throwable th2) {
                        th = th2;
                        bufferedWriter2 = null;
                        outputStreamWriter = null;
                        th = th;
                        if (bufferedWriter2 != null) {
                        }
                        if (outputStreamWriter != null) {
                        }
                        if (fileOutputStream != null) {
                        }
                        anonymousClass1.this$0.mHandler.post(new Runnable(anonymousClass1) {
                            private final StsExtensionService.AnonymousClass1 f$0;

                            {
                                this.f$0 = anonymousClass1;
                            }

                            @Override
                            public final void run() {
                                StsExtensionService.AnonymousClass1.lambda$updateTouchpanelFw$2(this.f$0);
                            }
                        });
                        throw th;
                    }
                    try {
                        bufferedWriter = new BufferedWriter(outputStreamWriter);
                        try {
                            Log.e("StsExtensionService", "----- fts_upgrade_bin ----- " + str.substring(str.indexOf("FT8205")));
                            bufferedWriter.write(str.substring(str.indexOf("FT8205")));
                            bufferedWriter.write("\n");
                            bufferedWriter.close();
                            outputStreamWriter.close();
                            fileOutputStream.close();
                            anonymousClass1.this$0.mHandler.post(new Runnable(anonymousClass1) {
                                private final StsExtensionService.AnonymousClass1 f$0;

                                {
                                    this.f$0 = anonymousClass1;
                                }

                                @Override
                                public final void run() {
                                    StsExtensionService.AnonymousClass1.lambda$updateTouchpanelFw$2(this.f$0);
                                }
                            });
                        } catch (Exception e3) {
                            e = e3;
                            fileOutputStream2 = fileOutputStream;
                            try {
                                Log.e("StsExtensionService", "----- Exception occurred!!! -----", e);
                                if (bufferedWriter != null) {
                                    bufferedWriter.close();
                                }
                                if (outputStreamWriter != null) {
                                    outputStreamWriter.close();
                                }
                                if (fileOutputStream2 != null) {
                                    fileOutputStream2.close();
                                }
                                anonymousClass1.this$0.mHandler.post(new Runnable(anonymousClass1) {
                                    private final StsExtensionService.AnonymousClass1 f$0;

                                    {
                                        this.f$0 = anonymousClass1;
                                    }

                                    @Override
                                    public final void run() {
                                        StsExtensionService.AnonymousClass1.lambda$updateTouchpanelFw$2(this.f$0);
                                    }
                                });
                            } catch (Throwable th3) {
                                th = th3;
                                bufferedWriter3 = bufferedWriter;
                                bufferedWriter2 = bufferedWriter3;
                                fileOutputStream = fileOutputStream2;
                                if (bufferedWriter2 != null) {
                                    try {
                                        bufferedWriter2.close();
                                    } catch (Exception e4) {
                                        Log.e("StsExtensionService", "----- Exception occurred!!! -----", e4);
                                        throw th;
                                    }
                                }
                                if (outputStreamWriter != null) {
                                    outputStreamWriter.close();
                                }
                                if (fileOutputStream != null) {
                                    fileOutputStream.close();
                                }
                                anonymousClass1.this$0.mHandler.post(new Runnable(anonymousClass1) {
                                    private final StsExtensionService.AnonymousClass1 f$0;

                                    {
                                        this.f$0 = anonymousClass1;
                                    }

                                    @Override
                                    public final void run() {
                                        StsExtensionService.AnonymousClass1.lambda$updateTouchpanelFw$2(this.f$0);
                                    }
                                });
                                throw th;
                            }
                        } catch (Throwable th4) {
                            bufferedWriter2 = bufferedWriter;
                            th = th4;
                            if (bufferedWriter2 != null) {
                            }
                            if (outputStreamWriter != null) {
                            }
                            if (fileOutputStream != null) {
                            }
                            anonymousClass1.this$0.mHandler.post(new Runnable(anonymousClass1) {
                                private final StsExtensionService.AnonymousClass1 f$0;

                                {
                                    this.f$0 = anonymousClass1;
                                }

                                @Override
                                public final void run() {
                                    StsExtensionService.AnonymousClass1.lambda$updateTouchpanelFw$2(this.f$0);
                                }
                            });
                            throw th;
                        }
                    } catch (Exception e5) {
                        e = e5;
                        bufferedWriter = null;
                        fileOutputStream2 = fileOutputStream;
                    } catch (Throwable th5) {
                        th = th5;
                        fileOutputStream2 = fileOutputStream;
                        bufferedWriter2 = bufferedWriter3;
                        fileOutputStream = fileOutputStream2;
                        if (bufferedWriter2 != null) {
                        }
                        if (outputStreamWriter != null) {
                        }
                        if (fileOutputStream != null) {
                        }
                        anonymousClass1.this$0.mHandler.post(new Runnable(anonymousClass1) {
                            private final StsExtensionService.AnonymousClass1 f$0;

                            {
                                this.f$0 = anonymousClass1;
                            }

                            @Override
                            public final void run() {
                                StsExtensionService.AnonymousClass1.lambda$updateTouchpanelFw$2(this.f$0);
                            }
                        });
                        throw th;
                    }
                } catch (Exception e6) {
                    Log.e("StsExtensionService", "----- Exception occurred!!! -----", e6);
                }
            } catch (Exception e7) {
                bufferedWriter = null;
                e = e7;
                fileOutputStream2 = null;
                outputStreamWriter = null;
            } catch (Throwable th6) {
                th = th6;
                fileOutputStream = null;
            }
        }

        public int getPenBattery() {
            return SystemProperties.getInt("persist.sys.nvt.penbattery", 0);
        }

        public String getTouchpanelVersion() {
            FileReader fileReader;
            Throwable th;
            if (this.this$0.mIsUpdating) {
                return null;
            }
            if (!StsExtensionService.PROC_NVT_TP_VERSION.exists()) {
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
                            $closeResource(null, fileReader);
                            return line;
                        } catch (Throwable th2) {
                            th = th2;
                            try {
                                throw th;
                            } catch (Throwable th3) {
                                th = th3;
                                $closeResource(th, bufferedReader);
                                throw th;
                            }
                        }
                    } finally {
                    }
                } catch (Throwable th4) {
                    return "";
                }
            }
            try {
                fileReader = new FileReader(StsExtensionService.PROC_NVT_TP_VERSION);
                try {
                    BufferedReader bufferedReader2 = new BufferedReader(fileReader);
                    try {
                        String line2 = bufferedReader2.readLine();
                        $closeResource(null, bufferedReader2);
                        $closeResource(null, fileReader);
                        int iIndexOf = line2.indexOf("fw_ver=");
                        int iIndexOf2 = line2.indexOf(",");
                        if (iIndexOf == -1 || iIndexOf2 == -1) {
                            return "";
                        }
                        return "0x" + Integer.toHexString(Integer.parseInt(line2.substring(iIndexOf + "fw_ver=".length(), iIndexOf2)));
                    } catch (Throwable th5) {
                        th = th5;
                        th = null;
                        $closeResource(th, bufferedReader2);
                        throw th;
                    }
                } finally {
                }
            } catch (Throwable th6) {
                return "";
            }
        }

        public boolean updateTouchpanelFw(final String str) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                if (!new File(str).isFile()) {
                    Log.e("StsExtensionService", "----- putString() : invalid file[" + str + "] -----");
                    return false;
                }
                if (this.this$0.mIsUpdating) {
                    Log.e("StsExtensionService", "----- FW update : already updating! -----");
                    return false;
                }
                this.this$0.mIsUpdating = true;
                Log.e("StsExtensionService", "----- updateTouchpanelFw ----- " + this.this$0.tp_type);
                if (this.this$0.tp_type == 0) {
                    new Thread(new Runnable(this, str) {
                        private final StsExtensionService.AnonymousClass1 f$0;
                        private final String f$1;

                        {
                            this.f$0 = this;
                            this.f$1 = str;
                        }

                        @Override
                        public final void run() throws Throwable {
                            StsExtensionService.AnonymousClass1.lambda$updateTouchpanelFw$1(this.f$0, this.f$1);
                        }
                    }).start();
                } else {
                    String strSubstring = str.substring(str.lastIndexOf("/") + 1);
                    if (strSubstring.length() >= 95) {
                        Log.e("StsExtensionService", "----- filename length(" + strSubstring.length() + ") fail -----");
                        this.this$0.mIsUpdating = false;
                        return false;
                    }
                    if (strSubstring.indexOf("FT8205") != 0) {
                        Log.e("StsExtensionService", "----- invalid file name [" + strSubstring + "] -----");
                        this.this$0.mIsUpdating = false;
                        return false;
                    }
                    new Thread(new Runnable(this, str) {
                        private final StsExtensionService.AnonymousClass1 f$0;
                        private final String f$1;

                        {
                            this.f$0 = this;
                            this.f$1 = str;
                        }

                        @Override
                        public final void run() throws Throwable {
                            StsExtensionService.AnonymousClass1.lambda$updateTouchpanelFw$3(this.f$0, this.f$1);
                        }
                    }).start();
                }
                return true;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    public StsExtensionService() {
        this.tp_type = -1;
        if (PROC_NVT_TP_VERSION.exists()) {
            Log.i("StsExtensionService", "----- TP:NVT -----");
            this.tp_type = 0;
        } else if (!FTS_TP_VERSION.exists()) {
            Log.e("StsExtensionService", "----- TP:Unkown -----");
        } else {
            Log.i("StsExtensionService", "----- TP:FTS -----");
            this.tp_type = 1;
        }
    }

    PowerManager getPowerManager() {
        if (this.mPowerManager == null) {
            this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        }
        return this.mPowerManager;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return this.mStub;
    }
}
