package com.android.camera;

import android.support.v4.view.MotionEventCompat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PanoUtil {
    public static String createName(String format, long dateTaken) {
        Date date = new Date(dateTaken);
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        return dateFormat.format(date);
    }

    public static double calculateDifferenceBetweenAngles(double firstAngle, double secondAngle) {
        double difference1 = (secondAngle - firstAngle) % 360.0d;
        if (difference1 < 0.0d) {
            difference1 += 360.0d;
        }
        double difference2 = (firstAngle - secondAngle) % 360.0d;
        if (difference2 < 0.0d) {
            difference2 += 360.0d;
        }
        return Math.min(difference1, difference2);
    }

    public static void decodeYUV420SPQuarterRes(int[] rgb, byte[] yuv420sp, int width, int height) {
        int frameSize = width * height;
        int ypd = 0;
        for (int j = 0; j < height; j += 4) {
            int uvp = frameSize + ((j >> 1) * width);
            int u = 0;
            int v = 0;
            int i = 0;
            while (true) {
                int uvp2 = uvp;
                if (i < width) {
                    int y = (yuv420sp[(j * width) + i] & 255) - 16;
                    if (y < 0) {
                        y = 0;
                    }
                    if ((i & 1) == 0) {
                        int uvp3 = uvp2 + 1;
                        v = (yuv420sp[uvp2] & 255) - 128;
                        u = (yuv420sp[uvp3] & 255) - 128;
                        uvp = uvp3 + 1 + 2;
                    } else {
                        uvp = uvp2;
                    }
                    int y1192 = y * 1192;
                    int r = y1192 + (v * 1634);
                    int g = (y1192 - (v * 833)) - (u * AnimationManager.SHRINK_DURATION);
                    int b = y1192 + (u * 2066);
                    if (r < 0) {
                        r = 0;
                    } else if (r > 262143) {
                        r = 262143;
                    }
                    if (g < 0) {
                        g = 0;
                    } else if (g > 262143) {
                        g = 262143;
                    }
                    if (b < 0) {
                        b = 0;
                    } else if (b > 262143) {
                        b = 262143;
                    }
                    rgb[ypd] = (-16777216) | ((r << 6) & 16711680) | ((g >> 2) & MotionEventCompat.ACTION_POINTER_INDEX_MASK) | ((b >> 10) & MotionEventCompat.ACTION_MASK);
                    i += 4;
                    ypd++;
                }
            }
        }
    }
}
