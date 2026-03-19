package android.view.textclassifier;

import android.content.res.AssetFileDescriptor;

final class TextClassifierImplNative {
    private final long mModelPtr;

    private static native AnnotatedSpan[] nativeAnnotate(long j, String str, AnnotationOptions annotationOptions);

    private static native ClassificationResult[] nativeClassifyText(long j, String str, int i, int i2, ClassificationOptions classificationOptions);

    private static native void nativeClose(long j);

    private static native String nativeGetLocales(int i);

    private static native int nativeGetVersion(int i);

    private static native long nativeNew(int i);

    private static native long nativeNewFromAssetFileDescriptor(AssetFileDescriptor assetFileDescriptor, long j, long j2);

    private static native long nativeNewFromPath(String str);

    private static native int[] nativeSuggestSelection(long j, String str, int i, int i2, SelectionOptions selectionOptions);

    static {
        System.loadLibrary("textclassifier");
    }

    TextClassifierImplNative(int i) {
        this.mModelPtr = nativeNew(i);
        if (this.mModelPtr == 0) {
            throw new IllegalArgumentException("Couldn't initialize TC from file descriptor.");
        }
    }

    TextClassifierImplNative(String str) {
        this.mModelPtr = nativeNewFromPath(str);
        if (this.mModelPtr == 0) {
            throw new IllegalArgumentException("Couldn't initialize TC from given file.");
        }
    }

    TextClassifierImplNative(AssetFileDescriptor assetFileDescriptor) {
        this.mModelPtr = nativeNewFromAssetFileDescriptor(assetFileDescriptor, assetFileDescriptor.getStartOffset(), assetFileDescriptor.getLength());
        if (this.mModelPtr == 0) {
            throw new IllegalArgumentException("Couldn't initialize TC from given AssetFileDescriptor");
        }
    }

    public int[] suggestSelection(String str, int i, int i2, SelectionOptions selectionOptions) {
        return nativeSuggestSelection(this.mModelPtr, str, i, i2, selectionOptions);
    }

    public ClassificationResult[] classifyText(String str, int i, int i2, ClassificationOptions classificationOptions) {
        return nativeClassifyText(this.mModelPtr, str, i, i2, classificationOptions);
    }

    public AnnotatedSpan[] annotate(String str, AnnotationOptions annotationOptions) {
        return nativeAnnotate(this.mModelPtr, str, annotationOptions);
    }

    public void close() {
        nativeClose(this.mModelPtr);
    }

    public static String getLocales(int i) {
        return nativeGetLocales(i);
    }

    public static int getVersion(int i) {
        return nativeGetVersion(i);
    }

    public static final class DatetimeResult {
        static final int GRANULARITY_DAY = 3;
        static final int GRANULARITY_HOUR = 4;
        static final int GRANULARITY_MINUTE = 5;
        static final int GRANULARITY_MONTH = 1;
        static final int GRANULARITY_SECOND = 6;
        static final int GRANULARITY_WEEK = 2;
        static final int GRANULARITY_YEAR = 0;
        private final int mGranularity;
        private final long mTimeMsUtc;

        DatetimeResult(long j, int i) {
            this.mGranularity = i;
            this.mTimeMsUtc = j;
        }

        public long getTimeMsUtc() {
            return this.mTimeMsUtc;
        }

        public int getGranularity() {
            return this.mGranularity;
        }
    }

    public static final class ClassificationResult {
        private final String mCollection;
        private final DatetimeResult mDatetimeResult;
        private final float mScore;

        ClassificationResult(String str, float f, DatetimeResult datetimeResult) {
            this.mCollection = str;
            this.mScore = f;
            this.mDatetimeResult = datetimeResult;
        }

        public String getCollection() {
            if (this.mCollection.equals("date") && this.mDatetimeResult != null) {
                switch (this.mDatetimeResult.getGranularity()) {
                    case 4:
                    case 5:
                    case 6:
                        return TextClassifier.TYPE_DATE_TIME;
                    default:
                        return "date";
                }
            }
            return this.mCollection;
        }

        public float getScore() {
            return this.mScore;
        }

        public DatetimeResult getDatetimeResult() {
            return this.mDatetimeResult;
        }
    }

    public static final class AnnotatedSpan {
        private final ClassificationResult[] mClassification;
        private final int mEndIndex;
        private final int mStartIndex;

        AnnotatedSpan(int i, int i2, ClassificationResult[] classificationResultArr) {
            this.mStartIndex = i;
            this.mEndIndex = i2;
            this.mClassification = classificationResultArr;
        }

        public int getStartIndex() {
            return this.mStartIndex;
        }

        public int getEndIndex() {
            return this.mEndIndex;
        }

        public ClassificationResult[] getClassification() {
            return this.mClassification;
        }
    }

    public static final class SelectionOptions {
        private final String mLocales;

        SelectionOptions(String str) {
            this.mLocales = str;
        }

        public String getLocales() {
            return this.mLocales;
        }
    }

    public static final class ClassificationOptions {
        private final String mLocales;
        private final long mReferenceTimeMsUtc;
        private final String mReferenceTimezone;

        ClassificationOptions(long j, String str, String str2) {
            this.mReferenceTimeMsUtc = j;
            this.mReferenceTimezone = str;
            this.mLocales = str2;
        }

        public long getReferenceTimeMsUtc() {
            return this.mReferenceTimeMsUtc;
        }

        public String getReferenceTimezone() {
            return this.mReferenceTimezone;
        }

        public String getLocale() {
            return this.mLocales;
        }
    }

    public static final class AnnotationOptions {
        private final String mLocales;
        private final long mReferenceTimeMsUtc;
        private final String mReferenceTimezone;

        AnnotationOptions(long j, String str, String str2) {
            this.mReferenceTimeMsUtc = j;
            this.mReferenceTimezone = str;
            this.mLocales = str2;
        }

        public long getReferenceTimeMsUtc() {
            return this.mReferenceTimeMsUtc;
        }

        public String getReferenceTimezone() {
            return this.mReferenceTimezone;
        }

        public String getLocale() {
            return this.mLocales;
        }
    }
}
