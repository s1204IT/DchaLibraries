package com.android.launcher3.compat;

import android.content.Context;
import android.content.res.Configuration;
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
        BaseIndex index;
        BaseIndex index2;
        BaseIndex baseIndex = null;
        BaseIndex index3 = null;
        try {
            if (Utilities.ATLEAST_N) {
                BaseIndex index4 = new AlphabeticIndexVN(context);
                index3 = index4;
            }
            index = index3;
        } catch (Exception e) {
            Log.d(TAG, "Unable to load the system index", e);
            index = null;
        }
        if (index == null) {
            try {
                index2 = new AlphabeticIndexV16(context);
            } catch (Exception e2) {
                Log.d(TAG, "Unable to load the system index", e2);
                index2 = index;
            }
        } else {
            index2 = index;
        }
        this.mBaseIndex = index2 == null ? new BaseIndex(baseIndex) : index2;
        if (context.getResources().getConfiguration().locale.getLanguage().equals(Locale.JAPANESE.getLanguage())) {
            this.mDefaultMiscLabel = "他";
        } else {
            this.mDefaultMiscLabel = MID_DOT;
        }
    }

    public String computeSectionName(CharSequence cs) {
        String s = Utilities.trim(cs);
        String sectionName = this.mBaseIndex.getBucketLabel(this.mBaseIndex.getBucketIndex(s));
        if (Utilities.trim(sectionName).isEmpty() && s.length() > 0) {
            int c = s.codePointAt(0);
            boolean startsWithDigit = Character.isDigit(c);
            if (startsWithDigit) {
                return "#";
            }
            boolean startsWithLetter = Character.isLetter(c);
            if (startsWithLetter) {
                return this.mDefaultMiscLabel;
            }
            return MID_DOT;
        }
        return sectionName;
    }

    private static class BaseIndex {
        private static final String BUCKETS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-";
        private static final int UNKNOWN_BUCKET_INDEX = BUCKETS.length() - 1;

        BaseIndex(BaseIndex baseIndex) {
            this();
        }

        private BaseIndex() {
        }

        protected int getBucketIndex(String s) {
            if (s.isEmpty()) {
                return UNKNOWN_BUCKET_INDEX;
            }
            int index = BUCKETS.indexOf(s.substring(0, 1).toUpperCase());
            if (index != -1) {
                return index;
            }
            return UNKNOWN_BUCKET_INDEX;
        }

        protected String getBucketLabel(int index) {
            return BUCKETS.substring(index, index + 1);
        }
    }

    private static class AlphabeticIndexV16 extends BaseIndex {
        private Object mAlphabeticIndex;
        private Method mGetBucketIndexMethod;
        private Method mGetBucketLabelMethod;

        public AlphabeticIndexV16(Context context) throws Exception {
            super(null);
            Locale curLocale = context.getResources().getConfiguration().locale;
            Class<?> cls = Class.forName("libcore.icu.AlphabeticIndex");
            this.mGetBucketIndexMethod = cls.getDeclaredMethod("getBucketIndex", String.class);
            this.mGetBucketLabelMethod = cls.getDeclaredMethod("getBucketLabel", Integer.TYPE);
            this.mAlphabeticIndex = cls.getConstructor(Locale.class).newInstance(curLocale);
            if (curLocale.getLanguage().equals(Locale.ENGLISH.getLanguage())) {
                return;
            }
            cls.getDeclaredMethod("addLabels", Locale.class).invoke(this.mAlphabeticIndex, Locale.ENGLISH);
        }

        @Override
        protected int getBucketIndex(String s) {
            try {
                return ((Integer) this.mGetBucketIndexMethod.invoke(this.mAlphabeticIndex, s)).intValue();
            } catch (Exception e) {
                e.printStackTrace();
                return super.getBucketIndex(s);
            }
        }

        @Override
        protected String getBucketLabel(int index) {
            try {
                return (String) this.mGetBucketLabelMethod.invoke(this.mAlphabeticIndex, Integer.valueOf(index));
            } catch (Exception e) {
                e.printStackTrace();
                return super.getBucketLabel(index);
            }
        }
    }

    private static class AlphabeticIndexVN extends BaseIndex {
        private Object mAlphabeticIndex;
        private Method mGetBucketIndexMethod;
        private Method mGetBucketMethod;
        private Method mGetLabelMethod;

        public AlphabeticIndexVN(Context context) throws Exception {
            super(null);
            Object locales = Configuration.class.getDeclaredMethod("getLocales", new Class[0]).invoke(context.getResources().getConfiguration(), new Object[0]);
            int localeCount = ((Integer) locales.getClass().getDeclaredMethod("size", new Class[0]).invoke(locales, new Object[0])).intValue();
            Method localeGetter = locales.getClass().getDeclaredMethod("get", Integer.TYPE);
            Locale primaryLocale = localeCount == 0 ? Locale.ENGLISH : (Locale) localeGetter.invoke(locales, 0);
            Class<?> cls = Class.forName("android.icu.text.AlphabeticIndex");
            this.mAlphabeticIndex = cls.getConstructor(Locale.class).newInstance(primaryLocale);
            Method addLocales = cls.getDeclaredMethod("addLabels", Locale[].class);
            for (int i = 1; i < localeCount; i++) {
                Locale l = (Locale) localeGetter.invoke(locales, Integer.valueOf(i));
                addLocales.invoke(this.mAlphabeticIndex, new Locale[]{l});
            }
            addLocales.invoke(this.mAlphabeticIndex, new Locale[]{Locale.ENGLISH});
            this.mAlphabeticIndex = this.mAlphabeticIndex.getClass().getDeclaredMethod("buildImmutableIndex", new Class[0]).invoke(this.mAlphabeticIndex, new Object[0]);
            this.mGetBucketIndexMethod = this.mAlphabeticIndex.getClass().getDeclaredMethod("getBucketIndex", CharSequence.class);
            this.mGetBucketMethod = this.mAlphabeticIndex.getClass().getDeclaredMethod("getBucket", Integer.TYPE);
            this.mGetLabelMethod = this.mGetBucketMethod.getReturnType().getDeclaredMethod("getLabel", new Class[0]);
        }

        @Override
        protected int getBucketIndex(String s) {
            try {
                return ((Integer) this.mGetBucketIndexMethod.invoke(this.mAlphabeticIndex, s)).intValue();
            } catch (Exception e) {
                e.printStackTrace();
                return super.getBucketIndex(s);
            }
        }

        @Override
        protected String getBucketLabel(int index) {
            try {
                return (String) this.mGetLabelMethod.invoke(this.mGetBucketMethod.invoke(this.mAlphabeticIndex, Integer.valueOf(index)), new Object[0]);
            } catch (Exception e) {
                e.printStackTrace();
                return super.getBucketLabel(index);
            }
        }
    }
}
