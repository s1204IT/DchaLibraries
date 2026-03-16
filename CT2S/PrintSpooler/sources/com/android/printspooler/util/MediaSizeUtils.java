package com.android.printspooler.util;

import android.content.Context;
import android.print.PrintAttributes;
import android.util.ArrayMap;
import com.android.printspooler.R;
import java.util.Comparator;
import java.util.Map;

public final class MediaSizeUtils {
    private static Map<PrintAttributes.MediaSize, String> sMediaSizeToStandardMap;

    public static PrintAttributes.MediaSize getDefault(Context context) {
        String mediaSizeId = context.getString(R.string.mediasize_default);
        return PrintAttributes.MediaSize.getStandardMediaSizeById(mediaSizeId);
    }

    private static String getStandardForMediaSize(Context context, PrintAttributes.MediaSize mediaSize) {
        if (sMediaSizeToStandardMap == null) {
            sMediaSizeToStandardMap = new ArrayMap();
            String[] mediaSizeToStandardMapValues = context.getResources().getStringArray(R.array.mediasize_to_standard_map);
            int mediaSizeToStandardCount = mediaSizeToStandardMapValues.length;
            for (int i = 0; i < mediaSizeToStandardCount; i += 2) {
                String mediaSizeId = mediaSizeToStandardMapValues[i];
                PrintAttributes.MediaSize key = PrintAttributes.MediaSize.getStandardMediaSizeById(mediaSizeId);
                String value = mediaSizeToStandardMapValues[i + 1];
                sMediaSizeToStandardMap.put(key, value);
            }
        }
        String standard = sMediaSizeToStandardMap.get(mediaSize);
        return standard != null ? standard : context.getString(R.string.mediasize_standard_iso);
    }

    public static final class MediaSizeComparator implements Comparator<PrintAttributes.MediaSize> {
        private final Context mContext;

        public MediaSizeComparator(Context context) {
            this.mContext = context;
        }

        @Override
        public int compare(PrintAttributes.MediaSize lhs, PrintAttributes.MediaSize rhs) {
            Object currentStandard = this.mContext.getString(R.string.mediasize_standard);
            String lhsStandard = MediaSizeUtils.getStandardForMediaSize(this.mContext, lhs);
            String rhsStandard = MediaSizeUtils.getStandardForMediaSize(this.mContext, rhs);
            if (lhsStandard.equals(currentStandard)) {
                if (!rhsStandard.equals(currentStandard)) {
                    return -1;
                }
            } else if (rhsStandard.equals(currentStandard)) {
                return 1;
            }
            if (!lhsStandard.equals(rhsStandard)) {
                return lhsStandard.compareTo(rhsStandard);
            }
            return lhs.getLabel(this.mContext.getPackageManager()).compareTo(rhs.getLabel(this.mContext.getPackageManager()));
        }
    }
}
