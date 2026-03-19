package com.android.server;

import android.content.Context;
import android.os.Binder;
import android.os.UEventObserver;
import android.util.Slog;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public final class SDIOAutoKService extends Binder {
    private static final String TAG = SDIOAutoKService.class.getSimpleName();
    private final Context mContext;
    private final UEventObserver mSDIOAutoKObserver = new UEventObserver() {
        public void onUEvent(UEventObserver.UEvent event) {
            int paramsOffset;
            Slog.d(SDIOAutoKService.TAG, ">>>>>>> SDIOAutoK UEVENT: " + event.toString() + " <<<<<<<");
            String from = event.get("FROM");
            byte[] autokParams = new byte[256];
            byte[] procParams = new byte[512];
            File fAutoK = new File("data/autok");
            if ("sdio_autok".equals(from)) {
                if (fAutoK.exists()) {
                    return;
                }
                try {
                    FileInputStream fin = new FileInputStream("proc/autok");
                    BufferedInputStream bis = new BufferedInputStream(fin);
                    FileOutputStream fout = new FileOutputStream("data/autok");
                    BufferedOutputStream bos = new BufferedOutputStream(fout);
                    while (true) {
                        int autokLen = bis.read(autokParams);
                        if (autokLen != -1) {
                            String str = "";
                            for (int i = 0; i < autokLen; i++) {
                                str = str + Byte.toString(autokParams[i]);
                            }
                            Slog.d(SDIOAutoKService.TAG, "read from proc (Str): " + str + " \n length: " + String.valueOf(autokLen));
                            bos.write(autokParams, 0, autokLen);
                        } else {
                            bos.flush();
                            bos.close();
                            bis.close();
                            return;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                if ("lte_drv".equals(from)) {
                    String paramsStr = "";
                    byte[] stage = {0};
                    String sdiofunc = event.get("SDIOFUNC");
                    byte[] sdiofunc_addr = SDIOAutoKService.hexStringToByteArray_reverse(sdiofunc.substring(2));
                    System.arraycopy(sdiofunc_addr, 0, procParams, 0, sdiofunc_addr.length);
                    int paramsOffset2 = sdiofunc_addr.length + 0;
                    if (fAutoK.exists()) {
                        Slog.d(SDIOAutoKService.TAG, "/data/autok exists, do stage 2 auto-K");
                        stage[0] = 2;
                        System.arraycopy(stage, 0, procParams, paramsOffset2, stage.length);
                        paramsOffset = paramsOffset2 + stage.length;
                        try {
                            FileInputStream fin2 = new FileInputStream(fAutoK);
                            BufferedInputStream bis2 = new BufferedInputStream(fin2);
                            while (true) {
                                int autokLen2 = bis2.read(autokParams);
                                if (autokLen2 == -1) {
                                    break;
                                }
                                String str2 = "";
                                System.arraycopy(autokParams, 0, procParams, paramsOffset, autokLen2);
                                paramsOffset += autokLen2;
                                for (int i2 = 0; i2 < autokLen2; i2++) {
                                    str2 = str2 + Byte.toString(autokParams[i2]);
                                }
                                paramsStr = paramsStr + str2;
                            }
                            Slog.d(SDIOAutoKService.TAG, "/data/autok content:");
                            Slog.d(SDIOAutoKService.TAG, " " + paramsStr);
                            bis2.close();
                        } catch (IOException e2) {
                            e2.printStackTrace();
                        }
                    } else {
                        stage[0] = 1;
                        System.arraycopy(stage, 0, procParams, paramsOffset2, stage.length);
                        paramsOffset = paramsOffset2 + stage.length;
                    }
                    Slog.d(SDIOAutoKService.TAG, "length of params write to proc:" + String.valueOf(paramsOffset));
                    try {
                        FileOutputStream fout2 = new FileOutputStream("proc/autok");
                        BufferedOutputStream bos2 = new BufferedOutputStream(fout2);
                        bos2.write(procParams, 0, paramsOffset);
                        bos2.flush();
                        bos2.close();
                        return;
                    } catch (IOException e3) {
                        e3.printStackTrace();
                        return;
                    }
                }
                if (!"autok_done".equals(from)) {
                    return;
                }
                try {
                    FileOutputStream fout3 = new FileOutputStream("proc/lte_autok");
                    BufferedOutputStream bos3 = new BufferedOutputStream(fout3);
                    byte[] lteprocParams = "autok_done".getBytes("UTF-8");
                    String str3 = "";
                    for (byte b : lteprocParams) {
                        str3 = str3 + Byte.toString(b) + " ";
                    }
                    Slog.d(SDIOAutoKService.TAG, "autok_done procParams.length: " + String.valueOf(lteprocParams.length));
                    Slog.d(SDIOAutoKService.TAG, "autok_done procParam: " + str3);
                    bos3.write(lteprocParams, 0, lteprocParams.length);
                    bos3.flush();
                    bos3.close();
                } catch (IOException e4) {
                    e4.printStackTrace();
                }
            }
        }
    };

    public SDIOAutoKService(Context context) {
        File fAutoK = new File("proc/lte_autok");
        Slog.d(TAG, ">>>>>>> SDIOAutoK Start Observing <<<<<<<");
        this.mContext = context;
        this.mSDIOAutoKObserver.startObserving("FROM=");
        if (!fAutoK.exists()) {
            return;
        }
        try {
            FileOutputStream fout = new FileOutputStream("proc/lte_autok");
            BufferedOutputStream bos = new BufferedOutputStream(fout);
            byte[] procParams = "system_server".getBytes("UTF-8");
            String str = "";
            for (byte b : procParams) {
                str = str + Byte.toString(b) + " ";
            }
            Slog.d(TAG, "system_server procParams.length: " + String.valueOf(procParams.length));
            Slog.d(TAG, "system_server procParam: " + str);
            bos.write(procParams, 0, procParams.length);
            bos.flush();
            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static byte[] hexStringToByteArray_reverse(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[((len - i) - 2) / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
