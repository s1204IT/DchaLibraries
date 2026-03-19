package android.view;

import android.content.res.Configuration;
import android.view.ViewRootImpl;
import java.text.BreakIterator;
import java.util.Locale;

public final class AccessibilityIterators {

    public interface TextSegmentIterator {
        int[] following(int i);

        int[] preceding(int i);
    }

    public static abstract class AbstractTextSegmentIterator implements TextSegmentIterator {
        private final int[] mSegment = new int[2];
        protected String mText;

        public void initialize(String str) {
            this.mText = str;
        }

        protected int[] getRange(int i, int i2) {
            if (i < 0 || i2 < 0 || i == i2) {
                return null;
            }
            this.mSegment[0] = i;
            this.mSegment[1] = i2;
            return this.mSegment;
        }
    }

    static class CharacterTextSegmentIterator extends AbstractTextSegmentIterator implements ViewRootImpl.ConfigChangedCallback {
        private static CharacterTextSegmentIterator sInstance;
        protected BreakIterator mImpl;
        private Locale mLocale;

        public static CharacterTextSegmentIterator getInstance(Locale locale) {
            if (sInstance == null) {
                sInstance = new CharacterTextSegmentIterator(locale);
            }
            return sInstance;
        }

        private CharacterTextSegmentIterator(Locale locale) {
            this.mLocale = locale;
            onLocaleChanged(locale);
            ViewRootImpl.addConfigCallback(this);
        }

        @Override
        public void initialize(String str) {
            super.initialize(str);
            this.mImpl.setText(str);
        }

        @Override
        public int[] following(int i) {
            int length = this.mText.length();
            if (length <= 0 || i >= length) {
                return null;
            }
            if (i < 0) {
                i = 0;
            }
            while (!this.mImpl.isBoundary(i)) {
                i = this.mImpl.following(i);
                if (i == -1) {
                    return null;
                }
            }
            int iFollowing = this.mImpl.following(i);
            if (iFollowing == -1) {
                return null;
            }
            return getRange(i, iFollowing);
        }

        @Override
        public int[] preceding(int i) {
            int length = this.mText.length();
            if (length <= 0 || i <= 0) {
                return null;
            }
            if (i > length) {
                i = length;
            }
            while (!this.mImpl.isBoundary(i)) {
                i = this.mImpl.preceding(i);
                if (i == -1) {
                    return null;
                }
            }
            int iPreceding = this.mImpl.preceding(i);
            if (iPreceding == -1) {
                return null;
            }
            return getRange(iPreceding, i);
        }

        @Override
        public void onConfigurationChanged(Configuration configuration) {
            Locale locale = configuration.getLocales().get(0);
            if (!this.mLocale.equals(locale)) {
                this.mLocale = locale;
                onLocaleChanged(locale);
            }
        }

        protected void onLocaleChanged(Locale locale) {
            this.mImpl = BreakIterator.getCharacterInstance(locale);
        }
    }

    static class WordTextSegmentIterator extends CharacterTextSegmentIterator {
        private static WordTextSegmentIterator sInstance;

        public static WordTextSegmentIterator getInstance(Locale locale) {
            if (sInstance == null) {
                sInstance = new WordTextSegmentIterator(locale);
            }
            return sInstance;
        }

        private WordTextSegmentIterator(Locale locale) {
            super(locale);
        }

        @Override
        protected void onLocaleChanged(Locale locale) {
            this.mImpl = BreakIterator.getWordInstance(locale);
        }

        @Override
        public int[] following(int i) {
            if (this.mText.length() <= 0 || i >= this.mText.length()) {
                return null;
            }
            if (i < 0) {
                i = 0;
            }
            while (!isLetterOrDigit(i) && !isStartBoundary(i)) {
                i = this.mImpl.following(i);
                if (i == -1) {
                    return null;
                }
            }
            int iFollowing = this.mImpl.following(i);
            if (iFollowing == -1 || !isEndBoundary(iFollowing)) {
                return null;
            }
            return getRange(i, iFollowing);
        }

        @Override
        public int[] preceding(int i) {
            int length = this.mText.length();
            if (length <= 0 || i <= 0) {
                return null;
            }
            if (i > length) {
                i = length;
            }
            while (i > 0 && !isLetterOrDigit(i - 1) && !isEndBoundary(i)) {
                i = this.mImpl.preceding(i);
                if (i == -1) {
                    return null;
                }
            }
            int iPreceding = this.mImpl.preceding(i);
            if (iPreceding == -1 || !isStartBoundary(iPreceding)) {
                return null;
            }
            return getRange(iPreceding, i);
        }

        private boolean isStartBoundary(int i) {
            return isLetterOrDigit(i) && (i == 0 || !isLetterOrDigit(i - 1));
        }

        private boolean isEndBoundary(int i) {
            if (i > 0 && isLetterOrDigit(i - 1) && (i == this.mText.length() || !isLetterOrDigit(i))) {
                return true;
            }
            return false;
        }

        private boolean isLetterOrDigit(int i) {
            if (i >= 0 && i < this.mText.length()) {
                return Character.isLetterOrDigit(this.mText.codePointAt(i));
            }
            return false;
        }
    }

    static class ParagraphTextSegmentIterator extends AbstractTextSegmentIterator {
        private static ParagraphTextSegmentIterator sInstance;

        ParagraphTextSegmentIterator() {
        }

        public static ParagraphTextSegmentIterator getInstance() {
            if (sInstance == null) {
                sInstance = new ParagraphTextSegmentIterator();
            }
            return sInstance;
        }

        @Override
        public int[] following(int i) {
            int length = this.mText.length();
            if (length <= 0 || i >= length) {
                return null;
            }
            if (i < 0) {
                i = 0;
            }
            while (i < length && this.mText.charAt(i) == '\n' && !isStartBoundary(i)) {
                i++;
            }
            if (i >= length) {
                return null;
            }
            int i2 = i + 1;
            while (i2 < length && !isEndBoundary(i2)) {
                i2++;
            }
            return getRange(i, i2);
        }

        @Override
        public int[] preceding(int i) {
            int length = this.mText.length();
            if (length <= 0 || i <= 0) {
                return null;
            }
            if (i > length) {
                i = length;
            }
            while (i > 0 && this.mText.charAt(i - 1) == '\n' && !isEndBoundary(i)) {
                i--;
            }
            if (i <= 0) {
                return null;
            }
            int i2 = i - 1;
            while (i2 > 0 && !isStartBoundary(i2)) {
                i2--;
            }
            return getRange(i2, i);
        }

        private boolean isStartBoundary(int i) {
            return this.mText.charAt(i) != '\n' && (i == 0 || this.mText.charAt(i - 1) == '\n');
        }

        private boolean isEndBoundary(int i) {
            if (i > 0 && this.mText.charAt(i - 1) != '\n' && (i == this.mText.length() || this.mText.charAt(i) == '\n')) {
                return true;
            }
            return false;
        }
    }
}
