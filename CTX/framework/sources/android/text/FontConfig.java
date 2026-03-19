package android.text;

import android.graphics.fonts.FontVariationAxis;
import android.net.Uri;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class FontConfig {
    private final Alias[] mAliases;
    private final Family[] mFamilies;

    public FontConfig(Family[] familyArr, Alias[] aliasArr) {
        this.mFamilies = familyArr;
        this.mAliases = aliasArr;
    }

    public Family[] getFamilies() {
        return this.mFamilies;
    }

    public Alias[] getAliases() {
        return this.mAliases;
    }

    public static final class Font {
        private final FontVariationAxis[] mAxes;
        private final String mFallbackFor;
        private final String mFontName;
        private final boolean mIsItalic;
        private final int mTtcIndex;
        private Uri mUri;
        private final int mWeight;

        public Font(String str, int i, FontVariationAxis[] fontVariationAxisArr, int i2, boolean z, String str2) {
            this.mFontName = str;
            this.mTtcIndex = i;
            this.mAxes = fontVariationAxisArr;
            this.mWeight = i2;
            this.mIsItalic = z;
            this.mFallbackFor = str2;
        }

        public String getFontName() {
            return this.mFontName;
        }

        public int getTtcIndex() {
            return this.mTtcIndex;
        }

        public FontVariationAxis[] getAxes() {
            return this.mAxes;
        }

        public int getWeight() {
            return this.mWeight;
        }

        public boolean isItalic() {
            return this.mIsItalic;
        }

        public Uri getUri() {
            return this.mUri;
        }

        public void setUri(Uri uri) {
            this.mUri = uri;
        }

        public String getFallbackFor() {
            return this.mFallbackFor;
        }
    }

    public static final class Alias {
        private final String mName;
        private final String mToName;
        private final int mWeight;

        public Alias(String str, String str2, int i) {
            this.mName = str;
            this.mToName = str2;
            this.mWeight = i;
        }

        public String getName() {
            return this.mName;
        }

        public String getToName() {
            return this.mToName;
        }

        public int getWeight() {
            return this.mWeight;
        }
    }

    public static final class Family {
        public static final int VARIANT_COMPACT = 1;
        public static final int VARIANT_DEFAULT = 0;
        public static final int VARIANT_ELEGANT = 2;
        private final Font[] mFonts;
        private final String[] mLanguages;
        private final String mName;
        private final int mVariant;

        @Retention(RetentionPolicy.SOURCE)
        public @interface Variant {
        }

        public Family(String str, Font[] fontArr, String[] strArr, int i) {
            this.mName = str;
            this.mFonts = fontArr;
            this.mLanguages = strArr;
            this.mVariant = i;
        }

        public String getName() {
            return this.mName;
        }

        public Font[] getFonts() {
            return this.mFonts;
        }

        public String[] getLanguages() {
            return this.mLanguages;
        }

        public int getVariant() {
            return this.mVariant;
        }
    }
}
