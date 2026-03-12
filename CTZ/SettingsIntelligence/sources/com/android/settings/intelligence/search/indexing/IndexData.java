package com.android.settings.intelligence.search.indexing;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import com.android.settings.intelligence.search.ResultPayload;
import com.android.settings.intelligence.search.ResultPayloadUtils;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public class IndexData {
    private static final Pattern REMOVE_DIACRITICALS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    public final String childClassName;
    public final String className;
    public final boolean enabled;
    public final String entries;
    public final int iconResId;
    public final String intentAction;
    public final String intentTargetClass;
    public final String intentTargetPackage;
    public final String key;
    public final String locale;
    public final String normalizedSummaryOn;
    public final String normalizedTitle;
    public final String packageName;
    public final byte[] payload;
    public final int payloadType;
    public final String screenTitle;
    public final String spaceDelimitedKeywords;
    public final String updatedSummaryOn;
    public final String updatedTitle;

    private IndexData(Builder builder) {
        byte[] bArrMarshall;
        this.locale = Locale.getDefault().toString();
        this.updatedTitle = normalizeHyphen(builder.mTitle);
        this.updatedSummaryOn = normalizeHyphen(builder.mSummaryOn);
        if (Locale.JAPAN.toString().equalsIgnoreCase(this.locale)) {
            this.normalizedTitle = normalizeJapaneseString(builder.mTitle);
            this.normalizedSummaryOn = normalizeJapaneseString(builder.mSummaryOn);
        } else {
            this.normalizedTitle = normalizeString(builder.mTitle);
            this.normalizedSummaryOn = normalizeString(builder.mSummaryOn);
        }
        this.entries = builder.mEntries;
        this.className = builder.mClassName;
        this.childClassName = builder.mChildClassName;
        this.screenTitle = builder.mScreenTitle;
        this.iconResId = builder.mIconResId;
        this.spaceDelimitedKeywords = normalizeKeywords(builder.mKeywords);
        this.intentAction = builder.mIntentAction;
        this.packageName = builder.mPackageName;
        this.intentTargetPackage = builder.mIntentTargetPackage;
        this.intentTargetClass = builder.mIntentTargetClass;
        this.enabled = builder.mEnabled;
        this.key = builder.mKey;
        this.payloadType = builder.mPayloadType;
        if (builder.mPayload == null) {
            bArrMarshall = null;
        } else {
            bArrMarshall = ResultPayloadUtils.marshall(builder.mPayload);
        }
        this.payload = bArrMarshall;
    }

    public String toString() {
        return this.updatedTitle + ": " + this.updatedSummaryOn;
    }

    public static String normalizeKeywords(String str) {
        return str != null ? str.replaceAll("[,]\\s*", " ") : "";
    }

    public static String normalizeHyphen(String str) {
        return str != null ? str.replaceAll("‑", "-") : "";
    }

    public static String normalizeString(String str) {
        return REMOVE_DIACRITICALS_PATTERN.matcher(Normalizer.normalize(str != null ? normalizeHyphen(str).replaceAll("-", "") : "", Normalizer.Form.NFD)).replaceAll("").toLowerCase();
    }

    public static String normalizeJapaneseString(String str) {
        String strNormalize = Normalizer.normalize(str != null ? str.replaceAll("-", "") : "", Normalizer.Form.NFKD);
        StringBuffer stringBuffer = new StringBuffer();
        int length = strNormalize.length();
        for (int i = 0; i < length; i++) {
            char cCharAt = strNormalize.charAt(i);
            if (cCharAt >= 12353 && cCharAt <= 12438) {
                stringBuffer.append((char) ((cCharAt - 12353) + 12449));
            } else {
                stringBuffer.append(cCharAt);
            }
        }
        return REMOVE_DIACRITICALS_PATTERN.matcher(stringBuffer.toString()).replaceAll("").toLowerCase();
    }

    public static class Builder {
        private String mChildClassName;
        private String mClassName;
        private boolean mEnabled;
        private String mEntries;
        private int mIconResId;
        private String mIntentAction;
        private String mIntentTargetClass;
        private String mIntentTargetPackage;
        private String mKey;
        private String mKeywords;
        private String mPackageName;
        private ResultPayload mPayload;
        private int mPayloadType;
        private String mScreenTitle;
        private String mSummaryOn;
        private String mTitle;

        public String toString() {
            return "IndexData.Builder {title: " + this.mTitle + ",package: " + this.mPackageName + "}";
        }

        public Builder setTitle(String str) {
            this.mTitle = str;
            return this;
        }

        public String getKey() {
            return this.mKey;
        }

        public Builder setSummaryOn(String str) {
            this.mSummaryOn = str;
            return this;
        }

        public Builder setEntries(String str) {
            this.mEntries = str;
            return this;
        }

        public Builder setClassName(String str) {
            this.mClassName = str;
            return this;
        }

        public Builder setChildClassName(String str) {
            this.mChildClassName = str;
            return this;
        }

        public Builder setScreenTitle(String str) {
            this.mScreenTitle = str;
            return this;
        }

        public Builder setPackageName(String str) {
            this.mPackageName = str;
            return this;
        }

        public Builder setIconResId(int i) {
            this.mIconResId = i;
            return this;
        }

        public Builder setKeywords(String str) {
            this.mKeywords = str;
            return this;
        }

        public Builder setIntentAction(String str) {
            this.mIntentAction = str;
            return this;
        }

        public Builder setIntentTargetPackage(String str) {
            this.mIntentTargetPackage = str;
            return this;
        }

        public Builder setIntentTargetClass(String str) {
            this.mIntentTargetClass = str;
            return this;
        }

        public Builder setEnabled(boolean z) {
            this.mEnabled = z;
            return this;
        }

        public Builder setKey(String str) {
            this.mKey = str;
            return this;
        }

        private void setIntent(Context context) {
            if (this.mPayload != null) {
                return;
            }
            this.mPayload = new ResultPayload(buildIntent(context));
            this.mPayloadType = 0;
        }

        private Intent buildIntent(Context context) {
            if (TextUtils.isEmpty(this.mIntentAction)) {
                return DatabaseIndexingUtils.buildSearchTrampolineIntent(context, this.mClassName, this.mKey, this.mScreenTitle);
            }
            return DatabaseIndexingUtils.buildDirectSearchResultIntent(this.mIntentAction, this.mIntentTargetPackage, this.mIntentTargetClass, this.mKey);
        }

        public IndexData build(Context context) {
            setIntent(context);
            return new IndexData(this);
        }
    }
}
