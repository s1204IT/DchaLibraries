package com.panasonic.sanyo.ce.bej.hard;

import android.util.Log;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class Touchpanel {
    private static boolean isTsOpen = false;
    private static int[] cal_a = new int[7];
    private static boolean isCalibInit = false;

    public static native int DevTsCalibWrite(int[] iArr);

    public static native int DevTsClose();

    public static native int DevTsOpen();

    static {
        System.loadLibrary("ts");
    }

    public static int coefficient_init() {
        int[] prm = new int[7];
        int ret = Calib_prm_write(prm);
        if (ret == 0) {
            isCalibInit = true;
        }
        return ret;
    }

    public static int coefficient_set(int mode) {
        int ret = 0;
        if ((mode & 1) != 0) {
            ret = Calib_prm_write(cal_a);
        }
        if ((mode & 2) != 0) {
            ret = CalibF_prm_write(cal_a);
        }
        if (ret == 0) {
            isCalibInit = false;
        }
        return ret;
    }

    public static int coefficient_cancele() {
        int[] prm = new int[7];
        int ret = CalibF_prm_read(prm);
        if (ret == 0) {
            ret = Calib_prm_write(prm);
        }
        if (ret == 0) {
            isCalibInit = false;
        }
        return ret;
    }

    public static int calibration(int[] dx, int[] dy, int[] tx, int[] ty, int[] Pos) {
        int[] ctx = new int[5];
        int[] cty = new int[5];
        int[] cdx = new int[5];
        int[] cdy = new int[5];
        int[] ptc = new int[5];
        int ret = 0;
        if (!isCalibInit) {
            return -7;
        }
        if (dx.length < 5 || dy.length < 5 || tx.length < 5 || ty.length < 5 || Pos.length < 5) {
            Log.e("Touchpanel", "The number error of elements of arrangement");
            return -8;
        }
        int i = 0;
        while (true) {
            if (i >= 5) {
                break;
            }
            if (Pos[i] < 1 || 5 < Pos[i]) {
                break;
            }
            if (ptc[Pos[i] - 1] != 0) {
                ret = -10;
                Log.e("Touchpanel", "Duplication of coordinates");
                break;
            }
            ptc[Pos[i] - 1] = Pos[i];
            cdx[Pos[i] - 1] = dx[i];
            cdy[Pos[i] - 1] = dy[i];
            ctx[Pos[i] - 1] = tx[i];
            cty[Pos[i] - 1] = ty[i];
            i++;
        }
        if (i < 5) {
            return ret;
        }
        float xy = 0.0f;
        float y2 = 0.0f;
        float x2 = 0.0f;
        float y = 0.0f;
        float x = 0.0f;
        float n = 0.0f;
        for (int i2 = 0; i2 < 5; i2++) {
            n = (float) (((double) n) + 1.0d);
            x += ctx[i2];
            y += cty[i2];
            x2 += ctx[i2] * ctx[i2];
            y2 += cty[i2] * cty[i2];
            xy += ctx[i2] * cty[i2];
        }
        float det = (((x2 * y2) - (xy * xy)) * n) + (((xy * y) - (x * y2)) * x) + (((x * xy) - (y * x2)) * y);
        if (det < 0.1d && det > -0.1d) {
            Log.v("Touchpanel", "ts_calibrate: determinant is too small -- " + det);
            ret = -11;
        } else {
            float a = ((x2 * y2) - (xy * xy)) / det;
            float b = ((xy * y) - (x * y2)) / det;
            float c = ((x * xy) - (y * x2)) / det;
            float e = ((n * y2) - (y * y)) / det;
            float f = ((x * y) - (n * xy)) / det;
            float g = ((n * x2) - (x * x)) / det;
            float zy = 0.0f;
            float zx = 0.0f;
            float z = 0.0f;
            for (int i3 = 0; i3 < 5; i3++) {
                z += dx[i3];
                zx += cdx[i3] * ctx[i3];
                zy += cdx[i3] * cty[i3];
            }
            cal_a[0] = (int) (((b * z) + (e * zx) + (f * zy)) * 65536.0f);
            cal_a[1] = (int) (((c * z) + (f * zx) + (g * zy)) * 65536.0f);
            cal_a[2] = (int) (((a * z) + (b * zx) + (c * zy)) * 65536.0f);
            float zy2 = 0.0f;
            float zx2 = 0.0f;
            float z2 = 0.0f;
            for (int i4 = 0; i4 < 5; i4++) {
                z2 += cdy[i4];
                zx2 += cdy[i4] * ctx[i4];
                zy2 += cdy[i4] * cty[i4];
            }
            cal_a[3] = (int) (((b * z2) + (e * zx2) + (f * zy2)) * 65536.0f);
            cal_a[4] = (int) (((c * z2) + (f * zx2) + (g * zy2)) * 65536.0f);
            cal_a[5] = (int) (((a * z2) + (b * zx2) + (c * zy2)) * 65536.0f);
            cal_a[6] = (int) 65536.0f;
            String msg = "a:" + cal_a[0] + " b:" + cal_a[1] + " c:" + cal_a[2] + " d:" + cal_a[3] + " e:" + cal_a[4] + " f:" + cal_a[5] + " s:" + cal_a[6];
            Log.v("Touchpanel", msg);
        }
        return ret;
    }

    private static int CalibF_prm_write(int[] prm) {
        int[] prmw = new int[9];
        for (int i = 0; i < 7; i++) {
            prmw[i] = prm[i];
        }
        try {
            File cfile = new File("/factory/calibration");
            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(cfile)));
            for (int i2 = 0; i2 < 9; i2++) {
                pw.println(prmw[i2]);
            }
            pw.close();
            return 0;
        } catch (IOException e) {
            System.out.println(e);
            return -5;
        }
    }

    private static int CalibF_prm_read(int[] prm) {
        try {
            File cfile = new File("/factory/calibration");
            FileReader in = new FileReader(cfile);
            BufferedReader br = new BufferedReader(in);
            int i = 0;
            while (true) {
                String tmp_str = br.readLine();
                if (tmp_str != null) {
                    prm[i] = Integer.valueOf(tmp_str).intValue();
                    Log.v("Touchpanel", " Calib  i:" + i + ":" + prm[i]);
                    if (i >= 6) {
                        break;
                    }
                    i++;
                } else {
                    break;
                }
            }
            br.close();
            in.close();
            return i < 6 ? -6 : 0;
        } catch (FileNotFoundException e) {
            Log.v("Touchpanel", "FILE NOT FOUND\n");
            return -6;
        } catch (IOException e2) {
            Log.v("Touchpanel", "FILE I/O Error\n");
            return -6;
        }
    }

    private static boolean isTouchOpen() {
        return isTsOpen;
    }

    private static int touch_Open() {
        if (isTsOpen) {
            Log.w("Touchpanel", "Touch Panel Device file alrady opened ");
            return 0;
        }
        int ret = DevTsOpen();
        if (ret == 0) {
            isTsOpen = true;
            return 0;
        }
        Log.e("Touchpanel", "Touch Panel Device file Open Error=" + ret);
        return -1;
    }

    private static int touch_Close() {
        if (!isTouchOpen()) {
            isTsOpen = false;
            Log.w("Touchpanel", "Touch Panel Device file alrady Closed ");
            return 0;
        }
        int ret = DevTsClose();
        if (ret == 0) {
            isTsOpen = false;
            return 0;
        }
        Log.e("Touchpanel", "Touch Panel Device file Close Error=" + ret);
        return -2;
    }

    private static int Calib_prm_write(int[] prm) {
        int ret = touch_Open();
        if (ret < 0) {
            return ret;
        }
        int ret2 = DevTsCalibWrite(prm);
        touch_Close();
        return ret2;
    }
}
