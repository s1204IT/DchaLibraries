package com.android.launcher3.compat;

import android.annotation.TargetApi;
import android.content.Context;
import android.icu.text.AlphabeticIndex;
import android.os.LocaleList;
import android.util.Log;
import com.android.launcher3.Utilities;
import java.lang.reflect.Method;
import java.util.Locale;

public class AlphabeticIndexCompat {
    private static final String MID_DOT = "∙";
    private static final String TAG = "AlphabeticIndexCompat";
    private final BaseIndex mBaseIndex;
    private final String mDefaultMiscLabel;

    public AlphabeticIndexCompat(Context context) {
        BaseIndex alphabeticIndexV16;
        try {
            if (Utilities.ATLEAST_NOUGAT) {
                alphabeticIndexV16 = new AlphabeticIndexVN(context);
            } else {
                alphabeticIndexV16 = null;
            }
        } catch (Exception e) {
            Log.d(TAG, "Unable to load the system index", e);
            alphabeticIndexV16 = null;
        }
        if (alphabeticIndexV16 == null) {
            try {
                alphabeticIndexV16 = new AlphabeticIndexV16(context);
            } catch (Exception e2) {
                Log.d(TAG, "Unable to load the system index", e2);
            }
        }
        this.mBaseIndex = alphabeticIndexV16 == null ? new BaseIndex() : alphabeticIndexV16;
        if (context.getResources().getConfiguration().locale.getLanguage().equals(Locale.JAPANESE.getLanguage())) {
            this.mDefaultMiscLabel = "他";
        } else {
            this.mDefaultMiscLabel = MID_DOT;
        }
    }

    public String computeSectionName(CharSequence charSequence) {
        String strTrim = Utilities.trim(charSequence);
        String bucketLabel = this.mBaseIndex.getBucketLabel(this.mBaseIndex.getBucketIndex(strTrim));
        if (Utilities.trim(bucketLabel).isEmpty() && strTrim.length() > 0) {
            int iCodePointAt = strTrim.codePointAt(0);
            if (Character.isDigit(iCodePointAt)) {
                return "#";
            }
            if (Character.isLetter(iCodePointAt)) {
                return this.mDefaultMiscLabel;
            }
            return MID_DOT;
        }
        return bucketLabel;
    }

    private static class BaseIndex {
        private static final String BUCKETS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-";
        private static final int UNKNOWN_BUCKET_INDEX = BUCKETS.length() - 1;

        private BaseIndex() {
        }

        protected int getBucketIndex(String str) {
            if (str.isEmpty()) {
                return UNKNOWN_BUCKET_INDEX;
            }
            int iIndexOf = BUCKETS.indexOf(str.substring(0, 1).toUpperCase());
            if (iIndexOf != -1) {
                return iIndexOf;
            }
            return UNKNOWN_BUCKET_INDEX;
        }

        protected String getBucketLabel(int i) {
            return BUCKETS.substring(i, i + 1);
        }
    }

    private static class AlphabeticIndexV16 extends BaseIndex {
        private Object mAlphabeticIndex;
        private Method mGetBucketIndexMethod;
        private Method mGetBucketLabelMethod;

        public AlphabeticIndexV16(Context context) throws Exception {
            super();
            Locale locale = context.getResources().getConfiguration().locale;
            Class<?> cls = Class.forName("libcore.icu.AlphabeticIndex");
            this.mGetBucketIndexMethod = cls.getDeclaredMethod("getBucketIndex", String.class);
            this.mGetBucketLabelMethod = cls.getDeclaredMethod("getBucketLabel", Integer.TYPE);
            this.mAlphabeticIndex = cls.getConstructor(Locale.class).newInstance(locale);
            if (!locale.getLanguage().equals(Locale.ENGLISH.getLanguage())) {
                cls.getDeclaredMethod("addLabels", Locale.class).invoke(this.mAlphabeticIndex, Locale.ENGLISH);
            }
        }

        @Override
        protected int getBucketIndex(String str) {
            try {
                return ((Integer) this.mGetBucketIndexMethod.invoke(this.mAlphabeticIndex, str)).intValue();
            } catch (Exception e) {
                e.printStackTrace();
                return super.getBucketIndex(str);
            }
        }

        @Override
        protected String getBucketLabel(int i) {
            try {
                return (String) this.mGetBucketLabelMethod.invoke(this.mAlphabeticIndex, Integer.valueOf(i));
            } catch (Exception e) {
                e.printStackTrace();
                return super.getBucketLabel(i);
            }
        }
    }

    @TargetApi(24)
    private static class AlphabeticIndexVN extends BaseIndex {
        private final AlphabeticIndex.ImmutableIndex mAlphabeticIndex;

        public AlphabeticIndexVN(Context context) {
            super();
            LocaleList locales = context.getResources().getConfiguration().getLocales();
            int size = locales.size();
            AlphabeticIndex alphabeticIndex = new AlphabeticIndex(size == 0 ? Locale.ENGLISH : locales.get(0));
            for (int i = 1; i < size; i++) {
                alphabeticIndex.addLabels(locales.get(i));
            }
            alphabeticIndex.addLabels(Locale.ENGLISH);
            this.mAlphabeticIndex = alphabeticIndex.buildImmutableIndex();
        }

        @Override
        protected int getBucketIndex(String str) {
            return this.mAlphabeticIndex.getBucketIndex(str);
        }

        @Override
        protected String getBucketLabel(int i) {
            return this.mAlphabeticIndex.getBucket(i).getLabel();
        }
    }
}
