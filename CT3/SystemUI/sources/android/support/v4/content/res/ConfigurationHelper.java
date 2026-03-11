package android.support.v4.content.res;

import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.NonNull;

public final class ConfigurationHelper {
    private static final ConfigurationHelperImpl IMPL;

    private interface ConfigurationHelperImpl {
        int getScreenHeightDp(@NonNull Resources resources);

        int getScreenWidthDp(@NonNull Resources resources);

        int getSmallestScreenWidthDp(@NonNull Resources resources);
    }

    static {
        JellybeanMr1Impl jellybeanMr1Impl = null;
        int sdk = Build.VERSION.SDK_INT;
        if (sdk >= 17) {
            IMPL = new JellybeanMr1Impl(jellybeanMr1Impl);
        } else if (sdk >= 13) {
            IMPL = new HoneycombMr2Impl(jellybeanMr1Impl);
        } else {
            IMPL = new DonutImpl(jellybeanMr1Impl);
        }
    }

    private ConfigurationHelper() {
    }

    private static class DonutImpl implements ConfigurationHelperImpl {
        DonutImpl(DonutImpl donutImpl) {
            this();
        }

        private DonutImpl() {
        }

        @Override
        public int getScreenHeightDp(@NonNull Resources resources) {
            return ConfigurationHelperDonut.getScreenHeightDp(resources);
        }

        @Override
        public int getScreenWidthDp(@NonNull Resources resources) {
            return ConfigurationHelperDonut.getScreenWidthDp(resources);
        }

        @Override
        public int getSmallestScreenWidthDp(@NonNull Resources resources) {
            return ConfigurationHelperDonut.getSmallestScreenWidthDp(resources);
        }
    }

    private static class HoneycombMr2Impl extends DonutImpl {
        HoneycombMr2Impl(HoneycombMr2Impl honeycombMr2Impl) {
            this();
        }

        private HoneycombMr2Impl() {
            super(null);
        }

        @Override
        public int getScreenHeightDp(@NonNull Resources resources) {
            return ConfigurationHelperHoneycombMr2.getScreenHeightDp(resources);
        }

        @Override
        public int getScreenWidthDp(@NonNull Resources resources) {
            return ConfigurationHelperHoneycombMr2.getScreenWidthDp(resources);
        }

        @Override
        public int getSmallestScreenWidthDp(@NonNull Resources resources) {
            return ConfigurationHelperHoneycombMr2.getSmallestScreenWidthDp(resources);
        }
    }

    private static class JellybeanMr1Impl extends HoneycombMr2Impl {
        JellybeanMr1Impl(JellybeanMr1Impl jellybeanMr1Impl) {
            this();
        }

        private JellybeanMr1Impl() {
            super(null);
        }
    }

    public static int getScreenHeightDp(@NonNull Resources resources) {
        return IMPL.getScreenHeightDp(resources);
    }

    public static int getScreenWidthDp(@NonNull Resources resources) {
        return IMPL.getScreenWidthDp(resources);
    }

    public static int getSmallestScreenWidthDp(@NonNull Resources resources) {
        return IMPL.getSmallestScreenWidthDp(resources);
    }
}
