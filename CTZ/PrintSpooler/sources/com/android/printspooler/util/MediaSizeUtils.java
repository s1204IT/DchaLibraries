package com.android.printspooler.util;

import android.content.Context;
import android.content.res.Configuration;
import android.print.PrintAttributes;
import com.android.printspooler.R;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public final class MediaSizeUtils {
    private static int sMediaSizeStandardIso;
    private static Map<PrintAttributes.MediaSize, Integer> sMediaSizeToStandardMap;

    public static PrintAttributes.MediaSize getDefault(Context context) {
        return PrintAttributes.MediaSize.getStandardMediaSizeById(context.getString(R.string.mediasize_default));
    }

    private static int getStandardForMediaSize(Context context, PrintAttributes.MediaSize mediaSize) {
        if (sMediaSizeToStandardMap == null) {
            sMediaSizeStandardIso = Integer.parseInt(context.getString(R.string.mediasize_standard_iso));
            sMediaSizeToStandardMap = new HashMap();
            String[] stringArray = context.getResources().getStringArray(R.array.mediasize_to_standard_map);
            int length = stringArray.length;
            for (int i = 0; i < length; i += 2) {
                sMediaSizeToStandardMap.put(PrintAttributes.MediaSize.getStandardMediaSizeById(stringArray[i]), Integer.valueOf(Integer.parseInt(stringArray[i + 1])));
            }
        }
        Integer num = sMediaSizeToStandardMap.get(mediaSize);
        return num != null ? num.intValue() : sMediaSizeStandardIso;
    }

    public static final class MediaSizeComparator implements Comparator<PrintAttributes.MediaSize> {
        private final Context mContext;
        private Configuration mCurrentConfig;
        private int mCurrentStandard;
        private final Map<PrintAttributes.MediaSize, String> mMediaSizeToLabel = new HashMap();

        public MediaSizeComparator(Context context) {
            this.mContext = context;
            this.mCurrentStandard = Integer.parseInt(this.mContext.getString(R.string.mediasize_standard));
        }

        public void onConfigurationChanged(Configuration configuration) {
            if (this.mCurrentConfig == null || (configuration.diff(this.mCurrentConfig) & 4) != 0) {
                this.mCurrentStandard = Integer.parseInt(this.mContext.getString(R.string.mediasize_standard));
                this.mMediaSizeToLabel.clear();
                this.mCurrentConfig = configuration;
            }
        }

        public String getLabel(Context context, PrintAttributes.MediaSize mediaSize) {
            String str = this.mMediaSizeToLabel.get(mediaSize);
            if (str == null) {
                String label = mediaSize.getLabel(context.getPackageManager());
                this.mMediaSizeToLabel.put(mediaSize, label);
                return label;
            }
            return str;
        }

        @Override
        public int compare(PrintAttributes.MediaSize mediaSize, PrintAttributes.MediaSize mediaSize2) {
            int standardForMediaSize = MediaSizeUtils.getStandardForMediaSize(this.mContext, mediaSize);
            int standardForMediaSize2 = MediaSizeUtils.getStandardForMediaSize(this.mContext, mediaSize2);
            if (standardForMediaSize == this.mCurrentStandard) {
                if (standardForMediaSize2 != this.mCurrentStandard) {
                    return -1;
                }
            } else if (standardForMediaSize2 == this.mCurrentStandard) {
                return 1;
            }
            if (standardForMediaSize != standardForMediaSize2) {
                return Integer.valueOf(standardForMediaSize).compareTo(Integer.valueOf(standardForMediaSize2));
            }
            return getLabel(this.mContext, mediaSize).compareTo(getLabel(this.mContext, mediaSize2));
        }
    }
}
