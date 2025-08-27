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

/* loaded from: classes.dex */
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

    /* renamed from: com.sts.tottori.stsextension.StsExtensionService$1, reason: invalid class name */
    class AnonymousClass1 extends IStsExtensionService.Stub {
        AnonymousClass1() {
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [179=6] */
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
                    new Thread(new Runnable() { // from class: com.sts.tottori.stsextension.-$$Lambda$StsExtensionService$1$2k56XctykEVEWEJ1N9zz89Tl0kM
                        @Override // java.lang.Runnable
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
                    new Thread(new Runnable() { // from class: com.sts.tottori.stsextension.-$$Lambda$StsExtensionService$1$tupqgzeAP6XLKCQr0ErO3KLVrmQ
                        @Override // java.lang.Runnable
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

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [99=9, 106=10, 112=5, 117=21, 118=18, 124=5] */
        /* JADX WARN: Removed duplicated region for block: B:117:0x015a  */
        /* JADX WARN: Removed duplicated region for block: B:141:0x01aa  */
        /* JADX WARN: Removed duplicated region for block: B:143:0x01b2  */
        /* JADX WARN: Removed duplicated region for block: B:158:0x0092 A[EXC_TOP_SPLITTER, SYNTHETIC] */
        /* JADX WARN: Removed duplicated region for block: B:165:0x00ac A[EXC_TOP_SPLITTER, SYNTHETIC] */
        /* JADX WARN: Removed duplicated region for block: B:167:0x016f A[EXC_TOP_SPLITTER, SYNTHETIC] */
        /* JADX WARN: Removed duplicated region for block: B:83:0x00e8  */
        /* JADX WARN: Removed duplicated region for block: B:85:0x00f0  */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        public static /* synthetic */ void lambda$updateTouchpanelFw$1(final AnonymousClass1 anonymousClass1, String str) throws Throwable {
            String str2;
            Handler handler;
            Runnable runnable;
            Handler handler2;
            Runnable runnable2;
            Process processStart;
            Throwable th;
            String str3;
            InputStreamReader inputStreamReader;
            Throwable th2;
            Throwable th3;
            Process process = null;
            th = null;
            th = null;
            Throwable th4 = null;
            process = null;
            try {
                StsExtensionService.this.getPowerManager().setKeepAwake(true);
                SystemProperties.set("nvt.nvt_fw_updating", "1");
                processStart = new ProcessBuilder("/bin/.NT36523_Cmd_v208", "-u", str).start();
            } catch (Throwable th5) {
                th = th5;
                str2 = null;
            }
            try {
                InputStream inputStream = processStart.getInputStream();
                try {
                    inputStreamReader = new InputStreamReader(inputStream);
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
                                } catch (Throwable th6) {
                                    th = th6;
                                    str2 = str3;
                                    th2 = null;
                                    $closeResource(th2, inputStreamReader);
                                    throw th;
                                }
                            } catch (Throwable th7) {
                                th = th7;
                                th3 = null;
                                $closeResource(th3, bufferedReader);
                                throw th;
                            }
                        }
                        $closeResource(null, bufferedReader);
                    } catch (Throwable th8) {
                        th = th8;
                        th2 = null;
                        str2 = null;
                    }
                } catch (Throwable th9) {
                    str2 = null;
                    th4 = th9;
                    throw th4;
                }
                try {
                    $closeResource(null, inputStreamReader);
                    if (inputStream != null) {
                        try {
                            $closeResource(null, inputStream);
                        } catch (Throwable th10) {
                            th = th10;
                            str2 = str3;
                            Log.e("StsExtensionService", "----- Exception occurred!!! -----", th);
                            str3 = str2;
                            if (processStart != null) {
                            }
                            handler.post(runnable);
                        }
                    }
                } catch (Throwable th11) {
                    throw th11;
                }
            } catch (Throwable th12) {
                str2 = null;
                th = th12;
            }
            try {
                if (processStart != null) {
                    try {
                        processStart.waitFor();
                        i = "Verify OK ".equals(str3) ? 0 : -1;
                        handler = StsExtensionService.this.mHandler;
                        runnable = new Runnable() { // from class: com.sts.tottori.stsextension.-$$Lambda$StsExtensionService$1$WpRMRUhj7TEva2-aEOltpRrtlEI
                            @Override // java.lang.Runnable
                            public final void run() {
                                StsExtensionService.AnonymousClass1.lambda$updateTouchpanelFw$0(this.f$0, i);
                            }
                        };
                    } catch (Throwable th13) {
                        Log.e("StsExtensionService", "----- Exception occurred!!! -----", th13);
                        i = "Verify OK ".equals(str3) ? 0 : -1;
                        handler = StsExtensionService.this.mHandler;
                        runnable = new Runnable() { // from class: com.sts.tottori.stsextension.-$$Lambda$StsExtensionService$1$WpRMRUhj7TEva2-aEOltpRrtlEI
                            @Override // java.lang.Runnable
                            public final void run() {
                                StsExtensionService.AnonymousClass1.lambda$updateTouchpanelFw$0(this.f$0, i);
                            }
                        };
                    }
                } else {
                    if ("Verify OK ".equals(str3)) {
                    }
                    handler = StsExtensionService.this.mHandler;
                    runnable = new Runnable() { // from class: com.sts.tottori.stsextension.-$$Lambda$StsExtensionService$1$WpRMRUhj7TEva2-aEOltpRrtlEI
                        @Override // java.lang.Runnable
                        public final void run() {
                            StsExtensionService.AnonymousClass1.lambda$updateTouchpanelFw$0(this.f$0, i);
                        }
                    };
                }
                handler.post(runnable);
            } catch (Throwable th14) {
                i = "Verify OK ".equals(str3) ? 0 : -1;
                StsExtensionService.this.mHandler.post(new Runnable() { // from class: com.sts.tottori.stsextension.-$$Lambda$StsExtensionService$1$WpRMRUhj7TEva2-aEOltpRrtlEI
                    @Override // java.lang.Runnable
                    public final void run() {
                        StsExtensionService.AnonymousClass1.lambda$updateTouchpanelFw$0(this.f$0, i);
                    }
                });
                throw th14;
            }
        }

        public static /* synthetic */ void lambda$updateTouchpanelFw$0(AnonymousClass1 anonymousClass1, int i) {
            SystemProperties.set("nvt.nvt_fw_updating", "0");
            StsExtensionService.this.getPowerManager().setKeepAwake(false);
            StsExtensionService.this.mIsUpdating = false;
            StsExtensionService.this.mContext.sendBroadcastAsUser(new Intent("com.panasonic.sanyo.ts.intent.action.TOUCHPANEL_FIRMWARE_UPDATED").putExtra("result", i), UserHandle.ALL);
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [154=4, 169=4] */
        /* JADX DEBUG: Failed to insert an additional move for type inference into block B:13:0x0071 */
        /* JADX DEBUG: Failed to insert an additional move for type inference into block B:53:? */
        /* JADX DEBUG: Failed to insert an additional move for type inference into block B:9:0x006c */
        /* JADX WARN: Multi-variable type inference failed */
        /* JADX WARN: Removed duplicated region for block: B:44:0x00bd A[Catch: Exception -> 0x00b9, TryCatch #5 {Exception -> 0x00b9, blocks: (B:40:0x00b5, B:44:0x00bd, B:46:0x00c2, B:47:0x00c5), top: B:55:0x00b5 }] */
        /* JADX WARN: Removed duplicated region for block: B:46:0x00c2 A[Catch: Exception -> 0x00b9, TryCatch #5 {Exception -> 0x00b9, blocks: (B:40:0x00b5, B:44:0x00bd, B:46:0x00c2, B:47:0x00c5), top: B:55:0x00b5 }] */
        /* JADX WARN: Removed duplicated region for block: B:55:0x00b5 A[EXC_TOP_SPLITTER, SYNTHETIC] */
        /* JADX WARN: Type inference failed for: r0v13, types: [com.sts.tottori.stsextension.-$$Lambda$StsExtensionService$1$xX5WvrQaI0uqge2KCuBJqzt_JxI, java.lang.Runnable] */
        /* JADX WARN: Type inference failed for: r0v8, types: [com.sts.tottori.stsextension.-$$Lambda$StsExtensionService$1$xX5WvrQaI0uqge2KCuBJqzt_JxI, java.lang.Runnable] */
        /* JADX WARN: Type inference failed for: r1v12, types: [java.io.FileOutputStream, java.io.OutputStream] */
        /* JADX WARN: Type inference failed for: r1v13 */
        /* JADX WARN: Type inference failed for: r1v14 */
        /* JADX WARN: Type inference failed for: r1v15 */
        /* JADX WARN: Type inference failed for: r1v16 */
        /* JADX WARN: Type inference failed for: r1v2, types: [java.io.FileOutputStream] */
        /* JADX WARN: Type inference failed for: r1v5 */
        /* JADX WARN: Type inference failed for: r1v6 */
        /* JADX WARN: Type inference failed for: r1v7 */
        /* JADX WARN: Type inference failed for: r1v8 */
        /* JADX WARN: Type inference failed for: r1v9, types: [java.io.FileOutputStream] */
        /* JADX WARN: Type inference failed for: r3v0 */
        /* JADX WARN: Type inference failed for: r3v1, types: [java.io.BufferedWriter] */
        /* JADX WARN: Type inference failed for: r3v2 */
        /* JADX WARN: Type inference failed for: r3v5, types: [java.io.BufferedWriter] */
        /* JADX WARN: Type inference failed for: r7v18, types: [android.os.Handler] */
        /* JADX WARN: Type inference failed for: r7v9, types: [android.os.Handler] */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        public static /* synthetic */ void lambda$updateTouchpanelFw$3(final AnonymousClass1 anonymousClass1, String str) throws Throwable {
            ?? bufferedWriter;
            OutputStreamWriter outputStreamWriter;
            ?? fileOutputStream;
            String str2 = null;
            bufferedWriter = null;
            bufferedWriter = null;
            BufferedWriter bufferedWriter2 = null;
            try {
            } catch (Exception e) {
                Log.e("StsExtensionService", "----- Exception occurred!!! -----", e);
                str2 = "StsExtensionService";
                fileOutputStream = "----- Exception occurred!!! -----";
            }
            try {
                try {
                    StsExtensionService.this.getPowerManager().setKeepAwake(true);
                    fileOutputStream = new FileOutputStream("/sys/devices/platform/soc/1100f000.i2c/i2c-3/3-0038/fts_upgrade_bin");
                    try {
                        outputStreamWriter = new OutputStreamWriter((OutputStream) fileOutputStream, "UTF-8");
                        try {
                            bufferedWriter = new BufferedWriter(outputStreamWriter);
                            try {
                                Log.e("StsExtensionService", "----- fts_upgrade_bin ----- " + str.substring(str.indexOf("FT8205")));
                                bufferedWriter.write(str.substring(str.indexOf("FT8205")));
                                bufferedWriter.write("\n");
                                bufferedWriter.close();
                                outputStreamWriter.close();
                                fileOutputStream.close();
                                ?? r7 = StsExtensionService.this.mHandler;
                                ?? r0 = new Runnable() { // from class: com.sts.tottori.stsextension.-$$Lambda$StsExtensionService$1$xX5WvrQaI0uqge2KCuBJqzt_JxI
                                    @Override // java.lang.Runnable
                                    public final void run() {
                                        StsExtensionService.AnonymousClass1.lambda$updateTouchpanelFw$2(this.f$0);
                                    }
                                };
                                r7.post(r0);
                                str2 = r0;
                                fileOutputStream = fileOutputStream;
                                outputStreamWriter = outputStreamWriter;
                            } catch (Exception e2) {
                                e = e2;
                                bufferedWriter2 = bufferedWriter;
                                Log.e("StsExtensionService", "----- Exception occurred!!! -----", e);
                                if (bufferedWriter2 != null) {
                                    bufferedWriter2.close();
                                }
                                if (outputStreamWriter != null) {
                                    outputStreamWriter.close();
                                }
                                if (fileOutputStream != 0) {
                                    fileOutputStream.close();
                                }
                                ?? r72 = StsExtensionService.this.mHandler;
                                ?? r02 = new Runnable() { // from class: com.sts.tottori.stsextension.-$$Lambda$StsExtensionService$1$xX5WvrQaI0uqge2KCuBJqzt_JxI
                                    @Override // java.lang.Runnable
                                    public final void run() {
                                        StsExtensionService.AnonymousClass1.lambda$updateTouchpanelFw$2(this.f$0);
                                    }
                                };
                                r72.post(r02);
                                str2 = r02;
                                fileOutputStream = fileOutputStream;
                                outputStreamWriter = outputStreamWriter;
                            } catch (Throwable th) {
                                th = th;
                                if (bufferedWriter != null) {
                                    try {
                                        bufferedWriter.close();
                                    } catch (Exception e3) {
                                        Log.e("StsExtensionService", "----- Exception occurred!!! -----", e3);
                                        throw th;
                                    }
                                }
                                if (outputStreamWriter != null) {
                                    outputStreamWriter.close();
                                }
                                if (fileOutputStream != 0) {
                                    fileOutputStream.close();
                                }
                                StsExtensionService.this.mHandler.post(new Runnable() { // from class: com.sts.tottori.stsextension.-$$Lambda$StsExtensionService$1$xX5WvrQaI0uqge2KCuBJqzt_JxI
                                    @Override // java.lang.Runnable
                                    public final void run() {
                                        StsExtensionService.AnonymousClass1.lambda$updateTouchpanelFw$2(this.f$0);
                                    }
                                });
                                throw th;
                            }
                        } catch (Exception e4) {
                            e = e4;
                        }
                    } catch (Exception e5) {
                        e = e5;
                        outputStreamWriter = null;
                    } catch (Throwable th2) {
                        th = th2;
                        outputStreamWriter = null;
                        fileOutputStream = fileOutputStream;
                        bufferedWriter = outputStreamWriter;
                        if (bufferedWriter != null) {
                        }
                        if (outputStreamWriter != null) {
                        }
                        if (fileOutputStream != 0) {
                        }
                        StsExtensionService.this.mHandler.post(new Runnable() { // from class: com.sts.tottori.stsextension.-$$Lambda$StsExtensionService$1$xX5WvrQaI0uqge2KCuBJqzt_JxI
                            @Override // java.lang.Runnable
                            public final void run() {
                                StsExtensionService.AnonymousClass1.lambda$updateTouchpanelFw$2(this.f$0);
                            }
                        });
                        throw th;
                    }
                } catch (Exception e6) {
                    e = e6;
                    fileOutputStream = 0;
                    outputStreamWriter = null;
                } catch (Throwable th3) {
                    th = th3;
                    fileOutputStream = 0;
                    outputStreamWriter = null;
                }
            } catch (Throwable th4) {
                th = th4;
                bufferedWriter = str2;
            }
        }

        public static /* synthetic */ void lambda$updateTouchpanelFw$2(AnonymousClass1 anonymousClass1) {
            StsExtensionService.this.getPowerManager().setKeepAwake(false);
            StsExtensionService.this.mIsUpdating = false;
            StsExtensionService.this.mContext.sendBroadcastAsUser(new Intent("com.panasonic.sanyo.ts.intent.action.TOUCHPANEL_FIRMWARE_UPDATED").putExtra("result", 0), UserHandle.ALL);
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [196=4, 205=4] */
        /* JADX DEBUG: Failed to insert an additional move for type inference into block B:73:0x0050 */
        /* JADX WARN: Multi-variable type inference failed */
        /* JADX WARN: Type inference failed for: r0v3, types: [boolean] */
        /* JADX WARN: Type inference failed for: r0v4, types: [java.lang.AutoCloseable] */
        /* JADX WARN: Type inference failed for: r0v7, types: [java.io.FileReader, java.io.Reader, java.lang.AutoCloseable] */
        public String getTouchpanelVersion() {
            FileReader fileReader;
            Throwable th;
            Throwable th2;
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
                        } catch (Throwable th3) {
                            th = th3;
                            th2 = null;
                            $closeResource(th2, bufferedReader);
                            throw th;
                        }
                    } finally {
                        $closeResource(null, fileReader);
                    }
                } catch (Throwable th4) {
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
                    } catch (Throwable th5) {
                        th = th5;
                        th = null;
                        $closeResource(th, bufferedReader2);
                        throw th;
                    }
                } catch (Throwable th6) {
                    $closeResource(null, Exists);
                    throw th6;
                }
            } catch (Throwable th7) {
                return "";
            }
        }

        private static /* synthetic */ void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
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

    @Override // android.app.Service
    public IBinder onBind(Intent intent) {
        return this.mStub;
    }
}
