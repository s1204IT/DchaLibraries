package android.text;

import android.content.Context;
import android.content.res.Resources;
import android.icu.lang.UCharacter;
import android.icu.text.CaseMap;
import android.icu.text.Edits;
import android.icu.util.ULocale;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.AccessibilityClickableSpan;
import android.text.style.AccessibilityURLSpan;
import android.text.style.AlignmentSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.BulletSpan;
import android.text.style.CharacterStyle;
import android.text.style.EasyEditSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.LocaleSpan;
import android.text.style.ParagraphStyle;
import android.text.style.QuoteSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.ReplacementSpan;
import android.text.style.ScaleXSpan;
import android.text.style.SpellCheckSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuggestionRangeSpan;
import android.text.style.SuggestionSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.TextAppearanceSpan;
import android.text.style.TtsSpan;
import android.text.style.TypefaceSpan;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;
import android.text.style.UpdateAppearance;
import android.util.Log;
import android.util.Printer;
import com.android.internal.R;
import com.android.internal.content.NativeLibraryHelper;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.Preconditions;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class TextUtils {
    public static final int ABSOLUTE_SIZE_SPAN = 16;
    public static final int ACCESSIBILITY_CLICKABLE_SPAN = 25;
    public static final int ACCESSIBILITY_URL_SPAN = 26;
    public static final int ALIGNMENT_SPAN = 1;
    public static final int ANNOTATION = 18;
    public static final int BACKGROUND_COLOR_SPAN = 12;
    public static final int BULLET_SPAN = 8;
    public static final int CAP_MODE_CHARACTERS = 4096;
    public static final int CAP_MODE_SENTENCES = 16384;
    public static final int CAP_MODE_WORDS = 8192;
    public static final int EASY_EDIT_SPAN = 22;
    static final char ELLIPSIS_FILLER = 65279;
    private static final String ELLIPSIS_NORMAL = "…";
    private static final String ELLIPSIS_TWO_DOTS = "‥";
    public static final int FIRST_SPAN = 1;
    public static final int FOREGROUND_COLOR_SPAN = 2;
    public static final int LAST_SPAN = 26;
    public static final int LEADING_MARGIN_SPAN = 10;
    public static final int LOCALE_SPAN = 23;
    private static final int PARCEL_SAFE_TEXT_LENGTH = 100000;
    public static final int QUOTE_SPAN = 9;
    public static final int RELATIVE_SIZE_SPAN = 3;
    public static final int SCALE_X_SPAN = 4;
    public static final int SPELL_CHECK_SPAN = 20;
    public static final int STRIKETHROUGH_SPAN = 5;
    public static final int STYLE_SPAN = 7;
    public static final int SUBSCRIPT_SPAN = 15;
    public static final int SUGGESTION_RANGE_SPAN = 21;
    public static final int SUGGESTION_SPAN = 19;
    public static final int SUPERSCRIPT_SPAN = 14;
    private static final String TAG = "TextUtils";
    public static final int TEXT_APPEARANCE_SPAN = 17;
    public static final int TTS_SPAN = 24;
    public static final int TYPEFACE_SPAN = 13;
    public static final int UNDERLINE_SPAN = 6;
    public static final int URL_SPAN = 11;
    public static final Parcelable.Creator<CharSequence> CHAR_SEQUENCE_CREATOR = new Parcelable.Creator<CharSequence>() {
        @Override
        public CharSequence createFromParcel(Parcel parcel) {
            int i = parcel.readInt();
            String string = parcel.readString();
            if (string == null) {
                return null;
            }
            if (i == 1) {
                return string;
            }
            SpannableString spannableString = new SpannableString(string);
            while (true) {
                int i2 = parcel.readInt();
                if (i2 != 0) {
                    switch (i2) {
                        case 1:
                            TextUtils.readSpan(parcel, spannableString, new AlignmentSpan.Standard(parcel));
                            break;
                        case 2:
                            TextUtils.readSpan(parcel, spannableString, new ForegroundColorSpan(parcel));
                            break;
                        case 3:
                            TextUtils.readSpan(parcel, spannableString, new RelativeSizeSpan(parcel));
                            break;
                        case 4:
                            TextUtils.readSpan(parcel, spannableString, new ScaleXSpan(parcel));
                            break;
                        case 5:
                            TextUtils.readSpan(parcel, spannableString, new StrikethroughSpan(parcel));
                            break;
                        case 6:
                            TextUtils.readSpan(parcel, spannableString, new UnderlineSpan(parcel));
                            break;
                        case 7:
                            TextUtils.readSpan(parcel, spannableString, new StyleSpan(parcel));
                            break;
                        case 8:
                            TextUtils.readSpan(parcel, spannableString, new BulletSpan(parcel));
                            break;
                        case 9:
                            TextUtils.readSpan(parcel, spannableString, new QuoteSpan(parcel));
                            break;
                        case 10:
                            TextUtils.readSpan(parcel, spannableString, new LeadingMarginSpan.Standard(parcel));
                            break;
                        case 11:
                            TextUtils.readSpan(parcel, spannableString, new URLSpan(parcel));
                            break;
                        case 12:
                            TextUtils.readSpan(parcel, spannableString, new BackgroundColorSpan(parcel));
                            break;
                        case 13:
                            TextUtils.readSpan(parcel, spannableString, new TypefaceSpan(parcel));
                            break;
                        case 14:
                            TextUtils.readSpan(parcel, spannableString, new SuperscriptSpan(parcel));
                            break;
                        case 15:
                            TextUtils.readSpan(parcel, spannableString, new SubscriptSpan(parcel));
                            break;
                        case 16:
                            TextUtils.readSpan(parcel, spannableString, new AbsoluteSizeSpan(parcel));
                            break;
                        case 17:
                            TextUtils.readSpan(parcel, spannableString, new TextAppearanceSpan(parcel));
                            break;
                        case 18:
                            TextUtils.readSpan(parcel, spannableString, new Annotation(parcel));
                            break;
                        case 19:
                            TextUtils.readSpan(parcel, spannableString, new SuggestionSpan(parcel));
                            break;
                        case 20:
                            TextUtils.readSpan(parcel, spannableString, new SpellCheckSpan(parcel));
                            break;
                        case 21:
                            TextUtils.readSpan(parcel, spannableString, new SuggestionRangeSpan(parcel));
                            break;
                        case 22:
                            TextUtils.readSpan(parcel, spannableString, new EasyEditSpan(parcel));
                            break;
                        case 23:
                            TextUtils.readSpan(parcel, spannableString, new LocaleSpan(parcel));
                            break;
                        case 24:
                            TextUtils.readSpan(parcel, spannableString, new TtsSpan(parcel));
                            break;
                        case 25:
                            TextUtils.readSpan(parcel, spannableString, new AccessibilityClickableSpan(parcel));
                            break;
                        case 26:
                            TextUtils.readSpan(parcel, spannableString, new AccessibilityURLSpan(parcel));
                            break;
                        default:
                            throw new RuntimeException("bogus span encoding " + i2);
                    }
                } else {
                    return spannableString;
                }
            }
        }

        @Override
        public CharSequence[] newArray(int i) {
            return new CharSequence[i];
        }
    };
    private static Object sLock = new Object();
    private static char[] sTemp = null;
    private static String[] EMPTY_STRING_ARRAY = new String[0];

    public interface EllipsizeCallback {
        void ellipsized(int i, int i2);
    }

    public interface StringSplitter extends Iterable<String> {
        void setString(String str);
    }

    public enum TruncateAt {
        START,
        MIDDLE,
        END,
        MARQUEE,
        END_SMALL
    }

    public static String getEllipsisString(TruncateAt truncateAt) {
        return truncateAt == TruncateAt.END_SMALL ? ELLIPSIS_TWO_DOTS : ELLIPSIS_NORMAL;
    }

    private TextUtils() {
    }

    public static void getChars(CharSequence charSequence, int i, int i2, char[] cArr, int i3) {
        Class<?> cls = charSequence.getClass();
        if (cls == String.class) {
            ((String) charSequence).getChars(i, i2, cArr, i3);
            return;
        }
        if (cls == StringBuffer.class) {
            ((StringBuffer) charSequence).getChars(i, i2, cArr, i3);
            return;
        }
        if (cls == StringBuilder.class) {
            ((StringBuilder) charSequence).getChars(i, i2, cArr, i3);
            return;
        }
        if (charSequence instanceof GetChars) {
            ((GetChars) charSequence).getChars(i, i2, cArr, i3);
            return;
        }
        while (i < i2) {
            cArr[i3] = charSequence.charAt(i);
            i++;
            i3++;
        }
    }

    public static int indexOf(CharSequence charSequence, char c) {
        return indexOf(charSequence, c, 0);
    }

    public static int indexOf(CharSequence charSequence, char c, int i) {
        if (charSequence.getClass() == String.class) {
            return ((String) charSequence).indexOf(c, i);
        }
        return indexOf(charSequence, c, i, charSequence.length());
    }

    public static int indexOf(CharSequence charSequence, char c, int i, int i2) {
        Class<?> cls = charSequence.getClass();
        if ((charSequence instanceof GetChars) || cls == StringBuffer.class || cls == StringBuilder.class || cls == String.class) {
            char[] cArrObtain = obtain(500);
            while (i < i2) {
                int i3 = i + 500;
                if (i3 > i2) {
                    i3 = i2;
                }
                getChars(charSequence, i, i3, cArrObtain, 0);
                int i4 = i3 - i;
                for (int i5 = 0; i5 < i4; i5++) {
                    if (cArrObtain[i5] == c) {
                        recycle(cArrObtain);
                        return i5 + i;
                    }
                }
                i = i3;
            }
            recycle(cArrObtain);
            return -1;
        }
        while (i < i2) {
            if (charSequence.charAt(i) != c) {
                i++;
            } else {
                return i;
            }
        }
        return -1;
    }

    public static int lastIndexOf(CharSequence charSequence, char c) {
        return lastIndexOf(charSequence, c, charSequence.length() - 1);
    }

    public static int lastIndexOf(CharSequence charSequence, char c, int i) {
        if (charSequence.getClass() == String.class) {
            return ((String) charSequence).lastIndexOf(c, i);
        }
        return lastIndexOf(charSequence, c, 0, i);
    }

    public static int lastIndexOf(CharSequence charSequence, char c, int i, int i2) {
        if (i2 < 0) {
            return -1;
        }
        if (i2 >= charSequence.length()) {
            i2 = charSequence.length() - 1;
        }
        int i3 = i2 + 1;
        Class<?> cls = charSequence.getClass();
        if ((charSequence instanceof GetChars) || cls == StringBuffer.class || cls == StringBuilder.class || cls == String.class) {
            char[] cArrObtain = obtain(500);
            while (i < i3) {
                int i4 = i3 - 500;
                if (i4 < i) {
                    i4 = i;
                }
                getChars(charSequence, i4, i3, cArrObtain, 0);
                for (int i5 = (i3 - i4) - 1; i5 >= 0; i5--) {
                    if (cArrObtain[i5] == c) {
                        recycle(cArrObtain);
                        return i5 + i4;
                    }
                }
                i3 = i4;
            }
            recycle(cArrObtain);
            return -1;
        }
        for (int i6 = i3 - 1; i6 >= i; i6--) {
            if (charSequence.charAt(i6) == c) {
                return i6;
            }
        }
        return -1;
    }

    public static int indexOf(CharSequence charSequence, CharSequence charSequence2) {
        return indexOf(charSequence, charSequence2, 0, charSequence.length());
    }

    public static int indexOf(CharSequence charSequence, CharSequence charSequence2, int i) {
        return indexOf(charSequence, charSequence2, i, charSequence.length());
    }

    public static int indexOf(CharSequence charSequence, CharSequence charSequence2, int i, int i2) {
        int length = charSequence2.length();
        if (length == 0) {
            return i;
        }
        char cCharAt = charSequence2.charAt(0);
        while (true) {
            int iIndexOf = indexOf(charSequence, cCharAt, i);
            if (iIndexOf > i2 - length || iIndexOf < 0) {
                return -1;
            }
            if (regionMatches(charSequence, iIndexOf, charSequence2, 0, length)) {
                return iIndexOf;
            }
            i = iIndexOf + 1;
        }
    }

    public static boolean regionMatches(CharSequence charSequence, int i, CharSequence charSequence2, int i2, int i3) {
        int i4 = 2 * i3;
        if (i4 < i3) {
            throw new IndexOutOfBoundsException();
        }
        char[] cArrObtain = obtain(i4);
        boolean z = false;
        getChars(charSequence, i, i + i3, cArrObtain, 0);
        getChars(charSequence2, i2, i2 + i3, cArrObtain, i3);
        int i5 = 0;
        while (true) {
            if (i5 < i3) {
                if (cArrObtain[i5] != cArrObtain[i5 + i3]) {
                    break;
                }
                i5++;
            } else {
                z = true;
                break;
            }
        }
        recycle(cArrObtain);
        return z;
    }

    public static String substring(CharSequence charSequence, int i, int i2) {
        if (charSequence instanceof String) {
            return ((String) charSequence).substring(i, i2);
        }
        if (charSequence instanceof StringBuilder) {
            return ((StringBuilder) charSequence).substring(i, i2);
        }
        if (charSequence instanceof StringBuffer) {
            return ((StringBuffer) charSequence).substring(i, i2);
        }
        int i3 = i2 - i;
        char[] cArrObtain = obtain(i3);
        getChars(charSequence, i, i2, cArrObtain, 0);
        String str = new String(cArrObtain, 0, i3);
        recycle(cArrObtain);
        return str;
    }

    public static String join(CharSequence charSequence, Object[] objArr) {
        int length = objArr.length;
        if (length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(objArr[0]);
        for (int i = 1; i < length; i++) {
            sb.append(charSequence);
            sb.append(objArr[i]);
        }
        return sb.toString();
    }

    public static String join(CharSequence charSequence, Iterable iterable) {
        Iterator it = iterable.iterator();
        if (!it.hasNext()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(it.next());
        while (it.hasNext()) {
            sb.append(charSequence);
            sb.append(it.next());
        }
        return sb.toString();
    }

    public static String[] split(String str, String str2) {
        if (str.length() == 0) {
            return EMPTY_STRING_ARRAY;
        }
        return str.split(str2, -1);
    }

    public static String[] split(String str, Pattern pattern) {
        if (str.length() == 0) {
            return EMPTY_STRING_ARRAY;
        }
        return pattern.split(str, -1);
    }

    public static class SimpleStringSplitter implements StringSplitter, Iterator<String> {
        private char mDelimiter;
        private int mLength;
        private int mPosition;
        private String mString;

        public SimpleStringSplitter(char c) {
            this.mDelimiter = c;
        }

        @Override
        public void setString(String str) {
            this.mString = str;
            this.mPosition = 0;
            this.mLength = this.mString.length();
        }

        @Override
        public Iterator<String> iterator() {
            return this;
        }

        @Override
        public boolean hasNext() {
            return this.mPosition < this.mLength;
        }

        @Override
        public String next() {
            int iIndexOf = this.mString.indexOf(this.mDelimiter, this.mPosition);
            if (iIndexOf == -1) {
                iIndexOf = this.mLength;
            }
            String strSubstring = this.mString.substring(this.mPosition, iIndexOf);
            this.mPosition = iIndexOf + 1;
            return strSubstring;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public static CharSequence stringOrSpannedString(CharSequence charSequence) {
        if (charSequence == null) {
            return null;
        }
        if (charSequence instanceof SpannedString) {
            return charSequence;
        }
        if (charSequence instanceof Spanned) {
            return new SpannedString(charSequence);
        }
        return charSequence.toString();
    }

    public static boolean isEmpty(CharSequence charSequence) {
        return charSequence == null || charSequence.length() == 0;
    }

    public static String nullIfEmpty(String str) {
        if (isEmpty(str)) {
            return null;
        }
        return str;
    }

    public static String emptyIfNull(String str) {
        return str == null ? "" : str;
    }

    public static String firstNotEmpty(String str, String str2) {
        return !isEmpty(str) ? str : (String) Preconditions.checkStringNotEmpty(str2);
    }

    public static int length(String str) {
        if (isEmpty(str)) {
            return 0;
        }
        return str.length();
    }

    public static String safeIntern(String str) {
        if (str != null) {
            return str.intern();
        }
        return null;
    }

    public static int getTrimmedLength(CharSequence charSequence) {
        int length = charSequence.length();
        int i = 0;
        while (i < length && charSequence.charAt(i) <= ' ') {
            i++;
        }
        while (length > i && charSequence.charAt(length - 1) <= ' ') {
            length--;
        }
        return length - i;
    }

    public static boolean equals(CharSequence charSequence, CharSequence charSequence2) {
        int length;
        if (charSequence == charSequence2) {
            return true;
        }
        if (charSequence == null || charSequence2 == null || (length = charSequence.length()) != charSequence2.length()) {
            return false;
        }
        if ((charSequence instanceof String) && (charSequence2 instanceof String)) {
            return charSequence.equals(charSequence2);
        }
        for (int i = 0; i < length; i++) {
            if (charSequence.charAt(i) != charSequence2.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    @Deprecated
    public static CharSequence getReverse(CharSequence charSequence, int i, int i2) {
        return new Reverser(charSequence, i, i2);
    }

    private static class Reverser implements CharSequence, GetChars {
        private int mEnd;
        private CharSequence mSource;
        private int mStart;

        public Reverser(CharSequence charSequence, int i, int i2) {
            this.mSource = charSequence;
            this.mStart = i;
            this.mEnd = i2;
        }

        @Override
        public int length() {
            return this.mEnd - this.mStart;
        }

        @Override
        public CharSequence subSequence(int i, int i2) {
            char[] cArr = new char[i2 - i];
            getChars(i, i2, cArr, 0);
            return new String(cArr);
        }

        @Override
        public String toString() {
            return subSequence(0, length()).toString();
        }

        @Override
        public char charAt(int i) {
            return (char) UCharacter.getMirror(this.mSource.charAt((this.mEnd - 1) - i));
        }

        @Override
        public void getChars(int i, int i2, char[] cArr, int i3) {
            TextUtils.getChars(this.mSource, this.mStart + i, this.mStart + i2, cArr, i3);
            int i4 = i2 - i;
            AndroidCharacter.mirror(cArr, 0, i4);
            int i5 = i4 / 2;
            for (int i6 = 0; i6 < i5; i6++) {
                int i7 = i3 + i6;
                char c = cArr[i7];
                int i8 = ((i3 + i4) - i6) - 1;
                cArr[i7] = cArr[i8];
                cArr[i8] = c;
            }
        }
    }

    public static void writeToParcel(CharSequence charSequence, Parcel parcel, int i) {
        if (charSequence instanceof Spanned) {
            parcel.writeInt(0);
            parcel.writeString(charSequence.toString());
            Spanned spanned = (Spanned) charSequence;
            Object[] spans = spanned.getSpans(0, charSequence.length(), Object.class);
            for (int i2 = 0; i2 < spans.length; i2++) {
                Object obj = spans[i2];
                Object underlying = spans[i2];
                if (underlying instanceof CharacterStyle) {
                    underlying = ((CharacterStyle) underlying).getUnderlying();
                }
                if (underlying instanceof ParcelableSpan) {
                    ParcelableSpan parcelableSpan = (ParcelableSpan) underlying;
                    int spanTypeIdInternal = parcelableSpan.getSpanTypeIdInternal();
                    if (spanTypeIdInternal < 1 || spanTypeIdInternal > 26) {
                        Log.e(TAG, "External class \"" + parcelableSpan.getClass().getSimpleName() + "\" is attempting to use the frameworks-only ParcelableSpan interface");
                    } else {
                        parcel.writeInt(spanTypeIdInternal);
                        parcelableSpan.writeToParcelInternal(parcel, i);
                        writeWhere(parcel, spanned, obj);
                    }
                }
            }
            parcel.writeInt(0);
            return;
        }
        parcel.writeInt(1);
        if (charSequence != null) {
            parcel.writeString(charSequence.toString());
        } else {
            parcel.writeString(null);
        }
    }

    private static void writeWhere(Parcel parcel, Spanned spanned, Object obj) {
        parcel.writeInt(spanned.getSpanStart(obj));
        parcel.writeInt(spanned.getSpanEnd(obj));
        parcel.writeInt(spanned.getSpanFlags(obj));
    }

    public static void dumpSpans(CharSequence charSequence, Printer printer, String str) {
        if (charSequence instanceof Spanned) {
            Spanned spanned = (Spanned) charSequence;
            for (Object obj : spanned.getSpans(0, charSequence.length(), Object.class)) {
                printer.println(str + ((Object) charSequence.subSequence(spanned.getSpanStart(obj), spanned.getSpanEnd(obj))) + ": " + Integer.toHexString(System.identityHashCode(obj)) + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + obj.getClass().getCanonicalName() + " (" + spanned.getSpanStart(obj) + NativeLibraryHelper.CLEAR_ABI_OVERRIDE + spanned.getSpanEnd(obj) + ") fl=#" + spanned.getSpanFlags(obj));
            }
            return;
        }
        printer.println(str + ((Object) charSequence) + ": (no spans)");
    }

    public static CharSequence replace(CharSequence charSequence, String[] strArr, CharSequence[] charSequenceArr) {
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(charSequence);
        for (int i = 0; i < strArr.length; i++) {
            int iIndexOf = indexOf(spannableStringBuilder, strArr[i]);
            if (iIndexOf >= 0) {
                spannableStringBuilder.setSpan(strArr[i], iIndexOf, strArr[i].length() + iIndexOf, 33);
            }
        }
        for (int i2 = 0; i2 < strArr.length; i2++) {
            int spanStart = spannableStringBuilder.getSpanStart(strArr[i2]);
            int spanEnd = spannableStringBuilder.getSpanEnd(strArr[i2]);
            if (spanStart >= 0) {
                spannableStringBuilder.replace(spanStart, spanEnd, charSequenceArr[i2]);
            }
        }
        return spannableStringBuilder;
    }

    public static CharSequence expandTemplate(CharSequence charSequence, CharSequence... charSequenceArr) {
        if (charSequenceArr.length > 9) {
            throw new IllegalArgumentException("max of 9 values are supported");
        }
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(charSequence);
        int length = 0;
        while (length < spannableStringBuilder.length()) {
            try {
                if (spannableStringBuilder.charAt(length) == '^') {
                    int i = length + 1;
                    char cCharAt = spannableStringBuilder.charAt(i);
                    if (cCharAt == '^') {
                        spannableStringBuilder.delete(i, length + 2);
                        length = i;
                    } else if (Character.isDigit(cCharAt)) {
                        int numericValue = Character.getNumericValue(cCharAt) - 1;
                        if (numericValue < 0) {
                            throw new IllegalArgumentException("template requests value ^" + (numericValue + 1));
                        }
                        if (numericValue >= charSequenceArr.length) {
                            throw new IllegalArgumentException("template requests value ^" + (numericValue + 1) + "; only " + charSequenceArr.length + " provided");
                        }
                        spannableStringBuilder.replace(length, length + 2, charSequenceArr[numericValue]);
                        length += charSequenceArr[numericValue].length();
                    }
                }
                length++;
            } catch (IndexOutOfBoundsException e) {
            }
        }
        return spannableStringBuilder;
    }

    public static int getOffsetBefore(CharSequence charSequence, int i) {
        char cCharAt;
        if (i == 0 || i == 1) {
            return 0;
        }
        char cCharAt2 = charSequence.charAt(i - 1);
        int i2 = (cCharAt2 >= 56320 && cCharAt2 <= 57343 && (cCharAt = charSequence.charAt(i - 2)) >= 55296 && cCharAt <= 56319) ? i - 2 : i - 1;
        if (charSequence instanceof Spanned) {
            Spanned spanned = (Spanned) charSequence;
            ReplacementSpan[] replacementSpanArr = (ReplacementSpan[]) spanned.getSpans(i2, i2, ReplacementSpan.class);
            for (int i3 = 0; i3 < replacementSpanArr.length; i3++) {
                int spanStart = spanned.getSpanStart(replacementSpanArr[i3]);
                int spanEnd = spanned.getSpanEnd(replacementSpanArr[i3]);
                if (spanStart < i2 && spanEnd > i2) {
                    i2 = spanStart;
                }
            }
        }
        return i2;
    }

    public static int getOffsetAfter(CharSequence charSequence, int i) {
        int i2;
        int length = charSequence.length();
        if (i == length || i == length - 1) {
            return length;
        }
        char cCharAt = charSequence.charAt(i);
        if (cCharAt >= 55296 && cCharAt <= 56319) {
            i2 = i + 1;
            char cCharAt2 = charSequence.charAt(i2);
            if (cCharAt2 >= 56320 && cCharAt2 <= 57343) {
                i2 = i + 2;
            }
        } else {
            i2 = i + 1;
        }
        if (charSequence instanceof Spanned) {
            Spanned spanned = (Spanned) charSequence;
            ReplacementSpan[] replacementSpanArr = (ReplacementSpan[]) spanned.getSpans(i2, i2, ReplacementSpan.class);
            for (int i3 = 0; i3 < replacementSpanArr.length; i3++) {
                int spanStart = spanned.getSpanStart(replacementSpanArr[i3]);
                int spanEnd = spanned.getSpanEnd(replacementSpanArr[i3]);
                if (spanStart < i2 && spanEnd > i2) {
                    i2 = spanEnd;
                }
            }
        }
        return i2;
    }

    private static void readSpan(Parcel parcel, Spannable spannable, Object obj) {
        spannable.setSpan(obj, parcel.readInt(), parcel.readInt(), parcel.readInt());
    }

    public static void copySpansFrom(Spanned spanned, int i, int i2, Class cls, Spannable spannable, int i3) {
        if (cls == null) {
            cls = Object.class;
        }
        Object[] spans = spanned.getSpans(i, i2, cls);
        for (int i4 = 0; i4 < spans.length; i4++) {
            int spanStart = spanned.getSpanStart(spans[i4]);
            int spanEnd = spanned.getSpanEnd(spans[i4]);
            int spanFlags = spanned.getSpanFlags(spans[i4]);
            if (spanStart < i) {
                spanStart = i;
            }
            if (spanEnd > i2) {
                spanEnd = i2;
            }
            spannable.setSpan(spans[i4], (spanStart - i) + i3, (spanEnd - i) + i3, spanFlags);
        }
    }

    public static CharSequence toUpperCase(Locale locale, CharSequence charSequence, boolean z) {
        Edits edits = new Edits();
        if (!z) {
            return edits.hasChanges() ? (StringBuilder) CaseMap.toUpper().apply(locale, charSequence, new StringBuilder(), edits) : charSequence;
        }
        SpannableStringBuilder spannableStringBuilder = (SpannableStringBuilder) CaseMap.toUpper().apply(locale, charSequence, new SpannableStringBuilder(), edits);
        if (!edits.hasChanges()) {
            return charSequence;
        }
        Edits.Iterator fineIterator = edits.getFineIterator();
        int length = charSequence.length();
        Spanned spanned = (Spanned) charSequence;
        for (Object obj : spanned.getSpans(0, length, Object.class)) {
            int spanStart = spanned.getSpanStart(obj);
            int spanEnd = spanned.getSpanEnd(obj);
            spannableStringBuilder.setSpan(obj, spanStart == length ? spannableStringBuilder.length() : toUpperMapToDest(fineIterator, spanStart), spanEnd == length ? spannableStringBuilder.length() : toUpperMapToDest(fineIterator, spanEnd), spanned.getSpanFlags(obj));
        }
        return spannableStringBuilder;
    }

    private static int toUpperMapToDest(Edits.Iterator iterator, int i) {
        iterator.findSourceIndex(i);
        if (i == iterator.sourceIndex()) {
            return iterator.destinationIndex();
        }
        if (iterator.hasChange()) {
            return iterator.destinationIndex() + iterator.newLength();
        }
        return iterator.destinationIndex() + (i - iterator.sourceIndex());
    }

    public static CharSequence ellipsize(CharSequence charSequence, TextPaint textPaint, float f, TruncateAt truncateAt) {
        return ellipsize(charSequence, textPaint, f, truncateAt, false, null);
    }

    public static CharSequence ellipsize(CharSequence charSequence, TextPaint textPaint, float f, TruncateAt truncateAt, boolean z, EllipsizeCallback ellipsizeCallback) {
        return ellipsize(charSequence, textPaint, f, truncateAt, z, ellipsizeCallback, TextDirectionHeuristics.FIRSTSTRONG_LTR, getEllipsisString(truncateAt));
    }

    public static CharSequence ellipsize(CharSequence charSequence, TextPaint textPaint, float f, TruncateAt truncateAt, boolean z, EllipsizeCallback ellipsizeCallback, TextDirectionHeuristic textDirectionHeuristic, String str) throws Throwable {
        MeasuredParagraph measuredParagraphBuildForMeasurement;
        int iBreakText;
        int iBreakText2;
        int length = charSequence.length();
        try {
            measuredParagraphBuildForMeasurement = MeasuredParagraph.buildForMeasurement(textPaint, charSequence, 0, charSequence.length(), textDirectionHeuristic, null);
            try {
                if (measuredParagraphBuildForMeasurement.getWholeWidth() <= f) {
                    if (ellipsizeCallback != null) {
                        ellipsizeCallback.ellipsized(0, 0);
                    }
                    if (measuredParagraphBuildForMeasurement != null) {
                        measuredParagraphBuildForMeasurement.recycle();
                    }
                    return charSequence;
                }
                float fMeasureText = f - textPaint.measureText(str);
                if (fMeasureText >= 0.0f) {
                    if (truncateAt == TruncateAt.START) {
                        iBreakText2 = length - measuredParagraphBuildForMeasurement.breakText(length, false, fMeasureText);
                        iBreakText = 0;
                    } else {
                        if (truncateAt != TruncateAt.END && truncateAt != TruncateAt.END_SMALL) {
                            iBreakText2 = length - measuredParagraphBuildForMeasurement.breakText(length, false, fMeasureText / 2.0f);
                            iBreakText = measuredParagraphBuildForMeasurement.breakText(iBreakText2, true, fMeasureText - measuredParagraphBuildForMeasurement.measure(iBreakText2, length));
                        }
                        iBreakText = measuredParagraphBuildForMeasurement.breakText(length, true, fMeasureText);
                    }
                    if (ellipsizeCallback != null) {
                        ellipsizeCallback.ellipsized(iBreakText, iBreakText2);
                    }
                    char[] chars = measuredParagraphBuildForMeasurement.getChars();
                    Spanned spanned = !(charSequence instanceof Spanned) ? (Spanned) charSequence : null;
                    int i = iBreakText2 - iBreakText;
                    int i2 = length - i;
                    if (!z) {
                        if (i2 > 0 && i >= str.length()) {
                            str.getChars(0, str.length(), chars, iBreakText);
                            iBreakText += str.length();
                        }
                        while (iBreakText < iBreakText2) {
                            chars[iBreakText] = ELLIPSIS_FILLER;
                            iBreakText++;
                        }
                        String str2 = new String(chars, 0, length);
                        if (spanned == null) {
                            if (measuredParagraphBuildForMeasurement != null) {
                                measuredParagraphBuildForMeasurement.recycle();
                            }
                            return str2;
                        }
                        SpannableString spannableString = new SpannableString(str2);
                        copySpansFrom(spanned, 0, length, Object.class, spannableString, 0);
                        if (measuredParagraphBuildForMeasurement != null) {
                            measuredParagraphBuildForMeasurement.recycle();
                        }
                        return spannableString;
                    }
                    if (i2 == 0) {
                        if (measuredParagraphBuildForMeasurement != null) {
                            measuredParagraphBuildForMeasurement.recycle();
                        }
                        return "";
                    }
                    if (spanned != null) {
                        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
                        spannableStringBuilder.append(charSequence, 0, iBreakText);
                        spannableStringBuilder.append((CharSequence) str);
                        spannableStringBuilder.append(charSequence, iBreakText2, length);
                        if (measuredParagraphBuildForMeasurement != null) {
                            measuredParagraphBuildForMeasurement.recycle();
                        }
                        return spannableStringBuilder;
                    }
                    StringBuilder sb = new StringBuilder(i2 + str.length());
                    sb.append(chars, 0, iBreakText);
                    sb.append(str);
                    sb.append(chars, iBreakText2, length - iBreakText2);
                    String string = sb.toString();
                    if (measuredParagraphBuildForMeasurement != null) {
                        measuredParagraphBuildForMeasurement.recycle();
                    }
                    return string;
                }
                iBreakText = 0;
                iBreakText2 = length;
                if (ellipsizeCallback != null) {
                }
                char[] chars2 = measuredParagraphBuildForMeasurement.getChars();
                if (!(charSequence instanceof Spanned)) {
                }
                int i3 = iBreakText2 - iBreakText;
                int i22 = length - i3;
                if (!z) {
                }
            } catch (Throwable th) {
                th = th;
                if (measuredParagraphBuildForMeasurement != null) {
                    measuredParagraphBuildForMeasurement.recycle();
                }
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
            measuredParagraphBuildForMeasurement = null;
        }
    }

    public static CharSequence listEllipsize(Context context, List<CharSequence> list, String str, TextPaint textPaint, float f, int i) {
        int size;
        Resources resources;
        BidiFormatter bidiFormatter;
        String quantityString;
        if (list == null || (size = list.size()) == 0) {
            return "";
        }
        if (context == null) {
            resources = null;
            bidiFormatter = BidiFormatter.getInstance();
        } else {
            resources = context.getResources();
            bidiFormatter = BidiFormatter.getInstance(resources.getConfiguration().getLocales().get(0));
        }
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
        int[] iArr = new int[size];
        for (int i2 = 0; i2 < size; i2++) {
            spannableStringBuilder.append(bidiFormatter.unicodeWrap(list.get(i2)));
            if (i2 != size - 1) {
                spannableStringBuilder.append((CharSequence) str);
            }
            iArr[i2] = spannableStringBuilder.length();
        }
        for (int i3 = size - 1; i3 >= 0; i3--) {
            spannableStringBuilder.delete(iArr[i3], spannableStringBuilder.length());
            int i4 = (size - i3) - 1;
            if (i4 > 0) {
                if (resources == null) {
                    quantityString = ELLIPSIS_NORMAL;
                } else {
                    quantityString = resources.getQuantityString(i, i4, Integer.valueOf(i4));
                }
                spannableStringBuilder.append(bidiFormatter.unicodeWrap((CharSequence) quantityString));
            }
            if (textPaint.measureText(spannableStringBuilder, 0, spannableStringBuilder.length()) <= f) {
                return spannableStringBuilder;
            }
        }
        return "";
    }

    @Deprecated
    public static CharSequence commaEllipsize(CharSequence charSequence, TextPaint textPaint, float f, String str, String str2) {
        return commaEllipsize(charSequence, textPaint, f, str, str2, TextDirectionHeuristics.FIRSTSTRONG_LTR);
    }

    @Deprecated
    public static CharSequence commaEllipsize(CharSequence charSequence, TextPaint textPaint, float f, String str, String str2, TextDirectionHeuristic textDirectionHeuristic) throws Throwable {
        MeasuredParagraph measuredParagraphBuildForMeasurement;
        char c;
        MeasuredParagraph measuredParagraph;
        char[] cArr;
        String string;
        MeasuredParagraph measuredParagraph2 = null;
        try {
            int length = charSequence.length();
            measuredParagraphBuildForMeasurement = MeasuredParagraph.buildForMeasurement(textPaint, charSequence, 0, length, textDirectionHeuristic, null);
            try {
                if (measuredParagraphBuildForMeasurement.getWholeWidth() <= f) {
                    if (measuredParagraphBuildForMeasurement != null) {
                        measuredParagraphBuildForMeasurement.recycle();
                    }
                    return charSequence;
                }
                char[] chars = measuredParagraphBuildForMeasurement.getChars();
                int i = 0;
                int i2 = 0;
                while (true) {
                    c = ',';
                    if (i >= length) {
                        break;
                    }
                    if (chars[i] == ',') {
                        i2++;
                    }
                    i++;
                }
                int i3 = 1;
                int i4 = i2 + 1;
                float[] rawArray = measuredParagraphBuildForMeasurement.getWidths().getRawArray();
                int i5 = i4;
                MeasuredParagraph measuredParagraph3 = null;
                int i6 = 0;
                int i7 = 0;
                String str3 = "";
                int i8 = 0;
                while (i6 < length) {
                    try {
                        i8 = (int) (i8 + rawArray[i6]);
                        if (chars[i6] == c) {
                            i5--;
                            if (i5 == i3) {
                                try {
                                    string = WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + str;
                                } catch (Throwable th) {
                                    th = th;
                                    measuredParagraph2 = measuredParagraph3;
                                }
                            } else {
                                StringBuilder sb = new StringBuilder();
                                sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                                Object[] objArr = new Object[i3];
                                objArr[0] = Integer.valueOf(i5);
                                sb.append(String.format(str2, objArr));
                                string = sb.toString();
                            }
                            measuredParagraph = measuredParagraph3;
                            try {
                                MeasuredParagraph measuredParagraphBuildForMeasurement2 = MeasuredParagraph.buildForMeasurement(textPaint, string, 0, string.length(), textDirectionHeuristic, measuredParagraph);
                                try {
                                    cArr = chars;
                                    if (i8 + measuredParagraphBuildForMeasurement2.getWholeWidth() <= f) {
                                        i7 = i6 + 1;
                                        str3 = string;
                                    }
                                    measuredParagraph3 = measuredParagraphBuildForMeasurement2;
                                } catch (Throwable th2) {
                                    th = th2;
                                    measuredParagraph2 = measuredParagraphBuildForMeasurement2;
                                }
                            } catch (Throwable th3) {
                                th = th3;
                                measuredParagraph2 = measuredParagraph;
                            }
                        } else {
                            cArr = chars;
                        }
                        i6++;
                        chars = cArr;
                        i3 = 1;
                        c = ',';
                    } catch (Throwable th4) {
                        th = th4;
                        measuredParagraph = measuredParagraph3;
                        measuredParagraph2 = measuredParagraph;
                    }
                }
                measuredParagraph = measuredParagraph3;
                SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(str3);
                spannableStringBuilder.insert(0, charSequence, 0, i7);
                if (measuredParagraphBuildForMeasurement != null) {
                    measuredParagraphBuildForMeasurement.recycle();
                }
                if (measuredParagraph != null) {
                    measuredParagraph.recycle();
                }
                return spannableStringBuilder;
            } catch (Throwable th5) {
                th = th5;
            }
        } catch (Throwable th6) {
            th = th6;
            measuredParagraphBuildForMeasurement = null;
        }
        if (measuredParagraphBuildForMeasurement != null) {
            measuredParagraphBuildForMeasurement.recycle();
        }
        if (measuredParagraph2 != null) {
            measuredParagraph2.recycle();
        }
        throw th;
    }

    static boolean couldAffectRtl(char c) {
        return (1424 <= c && c <= 2303) || c == 8206 || c == 8207 || (8234 <= c && c <= 8238) || ((8294 <= c && c <= 8297) || ((55296 <= c && c <= 57343) || ((64285 <= c && c <= 65023) || (65136 <= c && c <= 65278))));
    }

    static boolean doesNotNeedBidi(char[] cArr, int i, int i2) {
        int i3 = i2 + i;
        while (i < i3) {
            if (!couldAffectRtl(cArr[i])) {
                i++;
            } else {
                return false;
            }
        }
        return true;
    }

    static char[] obtain(int i) {
        char[] cArr;
        synchronized (sLock) {
            cArr = sTemp;
            sTemp = null;
        }
        if (cArr == null || cArr.length < i) {
            return ArrayUtils.newUnpaddedCharArray(i);
        }
        return cArr;
    }

    static void recycle(char[] cArr) {
        if (cArr.length > 1000) {
            return;
        }
        synchronized (sLock) {
            sTemp = cArr;
        }
    }

    public static String htmlEncode(String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char cCharAt = str.charAt(i);
            if (cCharAt != '\"') {
                if (cCharAt != '<') {
                    if (cCharAt == '>') {
                        sb.append("&gt;");
                    } else {
                        switch (cCharAt) {
                            case '&':
                                sb.append("&amp;");
                                break;
                            case '\'':
                                sb.append("&#39;");
                                break;
                            default:
                                sb.append(cCharAt);
                                break;
                        }
                    }
                } else {
                    sb.append("&lt;");
                }
            } else {
                sb.append("&quot;");
            }
        }
        return sb.toString();
    }

    public static CharSequence concat(CharSequence... charSequenceArr) {
        if (charSequenceArr.length == 0) {
            return "";
        }
        int i = 0;
        boolean z = true;
        if (charSequenceArr.length == 1) {
            return charSequenceArr[0];
        }
        int length = charSequenceArr.length;
        int i2 = 0;
        while (true) {
            if (i2 < length) {
                if (charSequenceArr[i2] instanceof Spanned) {
                    break;
                }
                i2++;
            } else {
                z = false;
                break;
            }
        }
        if (z) {
            SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
            int length2 = charSequenceArr.length;
            while (i < length2) {
                CharSequence charSequence = charSequenceArr[i];
                if (charSequence == null) {
                    charSequence = "null";
                }
                spannableStringBuilder.append(charSequence);
                i++;
            }
            return new SpannedString(spannableStringBuilder);
        }
        StringBuilder sb = new StringBuilder();
        int length3 = charSequenceArr.length;
        while (i < length3) {
            sb.append(charSequenceArr[i]);
            i++;
        }
        return sb.toString();
    }

    public static boolean isGraphic(CharSequence charSequence) {
        int length = charSequence.length();
        int iCharCount = 0;
        while (iCharCount < length) {
            int iCodePointAt = Character.codePointAt(charSequence, iCharCount);
            int type = Character.getType(iCodePointAt);
            if (type == 15 || type == 16 || type == 19 || type == 0 || type == 13 || type == 14 || type == 12) {
                iCharCount += Character.charCount(iCodePointAt);
            } else {
                return true;
            }
        }
        return false;
    }

    @Deprecated
    public static boolean isGraphic(char c) {
        int type = Character.getType(c);
        return (type == 15 || type == 16 || type == 19 || type == 0 || type == 13 || type == 14 || type == 12) ? false : true;
    }

    public static boolean isDigitsOnly(CharSequence charSequence) {
        int length = charSequence.length();
        int iCharCount = 0;
        while (iCharCount < length) {
            int iCodePointAt = Character.codePointAt(charSequence, iCharCount);
            if (!Character.isDigit(iCodePointAt)) {
                return false;
            }
            iCharCount += Character.charCount(iCodePointAt);
        }
        return true;
    }

    public static boolean isPrintableAscii(char c) {
        return (' ' <= c && c <= '~') || c == '\r' || c == '\n';
    }

    public static boolean isPrintableAsciiOnly(CharSequence charSequence) {
        int length = charSequence.length();
        for (int i = 0; i < length; i++) {
            if (!isPrintableAscii(charSequence.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static int getCapsMode(CharSequence charSequence, int i, int i2) {
        char cCharAt;
        char cCharAt2;
        int i3 = 0;
        if (i < 0) {
            return 0;
        }
        if ((i2 & 4096) != 0) {
            i3 = 4096;
        }
        if ((i2 & 24576) == 0) {
            return i3;
        }
        while (i > 0 && ((cCharAt2 = charSequence.charAt(i - 1)) == '\"' || cCharAt2 == '\'' || Character.getType(cCharAt2) == 21)) {
            i--;
        }
        int i4 = i;
        while (i4 > 0) {
            char cCharAt3 = charSequence.charAt(i4 - 1);
            if (cCharAt3 != ' ' && cCharAt3 != '\t') {
                break;
            }
            i4--;
        }
        if (i4 == 0 || charSequence.charAt(i4 - 1) == '\n') {
            return i3 | 8192;
        }
        if ((i2 & 16384) == 0) {
            return i != i4 ? i3 | 8192 : i3;
        }
        if (i == i4) {
            return i3;
        }
        while (i4 > 0) {
            char cCharAt4 = charSequence.charAt(i4 - 1);
            if (cCharAt4 != '\"' && cCharAt4 != '\'' && Character.getType(cCharAt4) != 22) {
                break;
            }
            i4--;
        }
        if (i4 > 0 && ((cCharAt = charSequence.charAt(i4 - 1)) == '.' || cCharAt == '?' || cCharAt == '!')) {
            if (cCharAt == '.') {
                for (int i5 = i4 - 2; i5 >= 0; i5--) {
                    char cCharAt5 = charSequence.charAt(i5);
                    if (cCharAt5 == '.') {
                        return i3;
                    }
                    if (!Character.isLetter(cCharAt5)) {
                        break;
                    }
                }
            }
            return i3 | 16384;
        }
        return i3;
    }

    public static boolean delimitedStringContains(String str, char c, String str2) {
        if (isEmpty(str) || isEmpty(str2)) {
            return false;
        }
        int length = str.length();
        int iIndexOf = -1;
        while (true) {
            iIndexOf = str.indexOf(str2, iIndexOf + 1);
            if (iIndexOf == -1) {
                return false;
            }
            if (iIndexOf <= 0 || str.charAt(iIndexOf - 1) == c) {
                int length2 = str2.length() + iIndexOf;
                if (length2 == length || str.charAt(length2) == c) {
                    return true;
                }
            }
        }
    }

    public static <T> T[] removeEmptySpans(T[] tArr, Spanned spanned, Class<T> cls) {
        int i = 0;
        Object[] objArr = null;
        for (int i2 = 0; i2 < tArr.length; i2++) {
            T t = tArr[i2];
            if (spanned.getSpanStart(t) == spanned.getSpanEnd(t)) {
                if (objArr == null) {
                    objArr = (Object[]) Array.newInstance((Class<?>) cls, tArr.length - 1);
                    System.arraycopy(tArr, 0, objArr, 0, i2);
                    i = i2;
                }
            } else if (objArr != null) {
                objArr[i] = t;
                i++;
            }
        }
        if (objArr != null) {
            T[] tArr2 = (T[]) ((Object[]) Array.newInstance((Class<?>) cls, i));
            System.arraycopy(objArr, 0, tArr2, 0, i);
            return tArr2;
        }
        return tArr;
    }

    public static long packRangeInLong(int i, int i2) {
        return ((long) i2) | (((long) i) << 32);
    }

    public static int unpackRangeStartFromLong(long j) {
        return (int) (j >>> 32);
    }

    public static int unpackRangeEndFromLong(long j) {
        return (int) (j & 4294967295L);
    }

    public static int getLayoutDirectionFromLocale(Locale locale) {
        if ((locale == null || locale.equals(Locale.ROOT) || !ULocale.forLocale(locale).isRightToLeft()) && !SystemProperties.getBoolean(Settings.Global.DEVELOPMENT_FORCE_RTL, false)) {
            return 0;
        }
        return 1;
    }

    public static CharSequence formatSelectedCount(int i) {
        return Resources.getSystem().getQuantityString(R.plurals.selected_count, i, Integer.valueOf(i));
    }

    public static boolean hasStyleSpan(Spanned spanned) {
        Preconditions.checkArgument(spanned != null);
        for (Class cls : new Class[]{CharacterStyle.class, ParagraphStyle.class, UpdateAppearance.class}) {
            if (spanned.nextSpanTransition(-1, spanned.length(), cls) < spanned.length()) {
                return true;
            }
        }
        return false;
    }

    public static CharSequence trimNoCopySpans(CharSequence charSequence) {
        if (charSequence != null && (charSequence instanceof Spanned)) {
            return new SpannableStringBuilder(charSequence);
        }
        return charSequence;
    }

    public static void wrap(StringBuilder sb, String str, String str2) {
        sb.insert(0, str);
        sb.append(str2);
    }

    public static <T extends CharSequence> T trimToParcelableSize(T t) {
        return (T) trimToSize(t, 100000);
    }

    public static <T extends CharSequence> T trimToSize(T t, int i) {
        Preconditions.checkArgument(i > 0);
        if (isEmpty(t) || t.length() <= i) {
            return t;
        }
        int i2 = i - 1;
        if (Character.isHighSurrogate(t.charAt(i2)) && Character.isLowSurrogate(t.charAt(i))) {
            i = i2;
        }
        return (T) t.subSequence(0, i);
    }
}
