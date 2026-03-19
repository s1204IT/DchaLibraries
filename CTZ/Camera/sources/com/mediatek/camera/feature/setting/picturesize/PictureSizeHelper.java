package com.mediatek.camera.feature.setting.picturesize;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import com.mediatek.camera.common.debug.LogUtil;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class PictureSizeHelper {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(PictureSizeHelper.class.getSimpleName());
    private static final double[] RATIOS = {1.7777777777777777d, 1.6666666666666667d, 1.5d, 1.3333333333333333d};
    private static final String[] RATIOS_IN_STRING = {"(16:9)", "(5:3)", "(3:2)", "(4:3)"};
    private static DecimalFormat sFormat = new DecimalFormat("##0");
    private static List<Double> sDesiredAspectRatios = new ArrayList();
    private static List<String> sDesiredAspectRatiosInStr = new ArrayList();
    private static double sDegressiveRatio = 0.0d;
    private static int sMaxCount = 0;

    public static double findFullScreenRatio(Context context) {
        Display defaultDisplay = ((WindowManager) context.getSystemService("window")).getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        defaultDisplay.getRealMetrics(displayMetrics);
        double dMax = ((double) Math.max(displayMetrics.widthPixels, displayMetrics.heightPixels)) / ((double) Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels));
        double d = 1.3333333333333333d;
        for (int i = 0; i < RATIOS.length; i++) {
            double d2 = RATIOS[i];
            if (Math.abs(d2 - dMax) < Math.abs(d - dMax)) {
                d = d2;
            }
        }
        return d;
    }

    public static void setDesiredAspectRatios(List<Double> list) {
        sDesiredAspectRatios.clear();
        sDesiredAspectRatiosInStr.clear();
        if (list != null) {
            sDesiredAspectRatios.addAll(list);
        }
        for (int i = 0; i < sDesiredAspectRatios.size(); i++) {
            double dDoubleValue = sDesiredAspectRatios.get(i).doubleValue();
            String str = null;
            int i2 = 0;
            while (true) {
                if (i2 >= RATIOS.length) {
                    break;
                }
                if (dDoubleValue != RATIOS[i2]) {
                    i2++;
                } else {
                    str = RATIOS_IN_STRING[i2];
                    break;
                }
            }
            sDesiredAspectRatiosInStr.add(str);
        }
    }

    public static void setFilterParameters(double d, int i) {
        sDegressiveRatio = d;
        sMaxCount = i;
    }

    public static List<String> filterSizes(List<String> list) {
        HashMap map = new HashMap();
        Iterator<Double> it = sDesiredAspectRatios.iterator();
        while (it.hasNext()) {
            double dDoubleValue = it.next().doubleValue();
            ResolutionBucket resolutionBucket = new ResolutionBucket();
            resolutionBucket.aspectRatio = dDoubleValue;
            map.put(Double.valueOf(dDoubleValue), resolutionBucket);
        }
        Iterator<String> it2 = list.iterator();
        while (true) {
            int i = 0;
            if (!it2.hasNext()) {
                break;
            }
            Size sizeValueToSize = valueToSize(it2.next());
            double d = ((double) sizeValueToSize.width) / ((double) sizeValueToSize.height);
            while (true) {
                if (i >= sDesiredAspectRatios.size()) {
                    i = -1;
                    break;
                }
                if (Math.abs(d - sDesiredAspectRatios.get(i).doubleValue()) < 0.02d) {
                    break;
                }
                i++;
            }
            if (i >= 0) {
                ((ResolutionBucket) map.get(sDesiredAspectRatios.get(i))).add(sizeValueToSize);
            }
        }
        LinkedList linkedList = new LinkedList();
        Iterator<Double> it3 = sDesiredAspectRatios.iterator();
        while (it3.hasNext()) {
            ResolutionBucket resolutionBucket2 = (ResolutionBucket) map.get(Double.valueOf(it3.next().doubleValue()));
            if (resolutionBucket2.sizes.size() != 0) {
                for (Size size : pickUpToThree(resolutionBucket2.sizes)) {
                    int size2 = 0;
                    while (true) {
                        if (size2 < linkedList.size()) {
                            if (area(size) >= area((Size) linkedList.get(size2))) {
                                break;
                            }
                            size2++;
                        } else {
                            size2 = -1;
                            break;
                        }
                    }
                    if (size2 == -1) {
                        size2 = linkedList.size();
                    }
                    linkedList.add(size2, size);
                }
            }
        }
        ArrayList arrayList = new ArrayList();
        Iterator it4 = linkedList.iterator();
        while (it4.hasNext()) {
            arrayList.add(sizeToStr((Size) it4.next()));
        }
        return arrayList;
    }

    public static String getPixelsAndRatio(String str) {
        Size sizeValueToSize = valueToSize(str);
        double d = ((double) sizeValueToSize.width) / ((double) sizeValueToSize.height);
        int i = 0;
        while (true) {
            if (i >= sDesiredAspectRatios.size()) {
                i = -1;
                break;
            }
            if (Math.abs(d - sDesiredAspectRatios.get(i).doubleValue()) < 0.02d) {
                break;
            }
            i++;
        }
        if (i == -1) {
            return null;
        }
        if (sizeValueToSize.width * sizeValueToSize.height < 500000.0d) {
            if ("320x240".equals(str)) {
                return "QVGA";
            }
            if ("400x240".equals(str)) {
                return "WQVGA";
            }
            if ("640x480".equals(str)) {
                return "VGA";
            }
            if ("800x480".equals(str)) {
                return "WVGA";
            }
            if ("800x600".equals(str)) {
                return "SVGA";
            }
        }
        String str2 = sDesiredAspectRatiosInStr.get(i);
        return sFormat.format(Math.round(((double) (sizeValueToSize.width * sizeValueToSize.height)) / 1000000.0d)) + "M" + str2;
    }

    public static double getStandardAspectRatio(String str) {
        Size sizeValueToSize = valueToSize(str);
        double d = ((double) sizeValueToSize.width) / ((double) sizeValueToSize.height);
        for (int i = 0; i < sDesiredAspectRatios.size(); i++) {
            double dDoubleValue = sDesiredAspectRatios.get(i).doubleValue();
            if (Math.abs(d - dDoubleValue) < 0.02d) {
                return dDoubleValue;
            }
        }
        return d;
    }

    private static List<Size> pickUpToThree(List<Size> list) {
        if (sDegressiveRatio == 0.0d || sMaxCount == 0) {
            return list;
        }
        ArrayList arrayList = new ArrayList();
        Size size = list.get(0);
        arrayList.add(size);
        Iterator<Size> it = list.iterator();
        Size size2 = size;
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            Size next = it.next();
            double dPow = Math.pow(sDegressiveRatio, arrayList.size()) * ((double) area(size));
            if (area(next) < dPow) {
                if (!arrayList.contains(size2) && ((double) area(size2)) - dPow < dPow - ((double) area(next))) {
                    arrayList.add(size2);
                } else {
                    arrayList.add(next);
                }
            }
            if (arrayList.size() != sMaxCount) {
                size2 = next;
            } else {
                size2 = next;
                break;
            }
        }
        if (arrayList.size() < sMaxCount && !arrayList.contains(size2)) {
            arrayList.add(size2);
        }
        return arrayList;
    }

    private static Size valueToSize(String str) {
        int iIndexOf = str.indexOf(120);
        int i = Integer.parseInt(str.substring(0, iIndexOf));
        int i2 = Integer.parseInt(str.substring(iIndexOf + 1));
        Size size = new Size();
        size.width = i;
        size.height = i2;
        return size;
    }

    private static String sizeToStr(Size size) {
        return size.width + "x" + size.height;
    }

    private static int area(Size size) {
        if (size == null) {
            return 0;
        }
        return size.width * size.height;
    }

    private static class Size {
        public int height;
        public int width;

        private Size() {
        }
    }

    private static class ResolutionBucket {
        public double aspectRatio;
        public List<Size> sizes;

        private ResolutionBucket() {
            this.sizes = new LinkedList();
        }

        public void add(Size size) {
            this.sizes.add(size);
        }
    }
}
