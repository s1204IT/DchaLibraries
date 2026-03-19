package android.content.res;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.TtmlUtils;
import android.provider.Telephony;
import android.service.notification.ZenModeConfig;
import android.text.Annotation;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannedString;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.BulletSpan;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.LineHeightSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.TextAppearanceSpan;
import android.text.style.TypefaceSpan;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;
import android.util.SparseArray;

final class StringBlock {
    private static final String TAG = "AssetManager";
    private static final boolean localLOGV = false;
    private final long mNative;
    private SparseArray<CharSequence> mSparseStrings;
    private CharSequence[] mStrings;
    private final boolean mUseSparse;
    StyleIDs mStyleIDs = null;
    private final boolean mOwnsNative = false;

    private static native long nativeCreate(byte[] bArr, int i, int i2);

    private static native void nativeDestroy(long j);

    private static native int nativeGetSize(long j);

    private static native String nativeGetString(long j, int i);

    private static native int[] nativeGetStyle(long j, int i);

    public StringBlock(byte[] bArr, boolean z) {
        this.mNative = nativeCreate(bArr, 0, bArr.length);
        this.mUseSparse = z;
    }

    public StringBlock(byte[] bArr, int i, int i2, boolean z) {
        this.mNative = nativeCreate(bArr, i, i2);
        this.mUseSparse = z;
    }

    public CharSequence get(int i) {
        synchronized (this) {
            if (this.mStrings != null) {
                CharSequence charSequence = this.mStrings[i];
                if (charSequence != null) {
                    return charSequence;
                }
            } else if (this.mSparseStrings != null) {
                CharSequence charSequence2 = this.mSparseStrings.get(i);
                if (charSequence2 != null) {
                    return charSequence2;
                }
            } else {
                int iNativeGetSize = nativeGetSize(this.mNative);
                if (this.mUseSparse && iNativeGetSize > 250) {
                    this.mSparseStrings = new SparseArray<>();
                } else {
                    this.mStrings = new CharSequence[iNativeGetSize];
                }
            }
            String strNativeGetString = nativeGetString(this.mNative, i);
            int[] iArrNativeGetStyle = nativeGetStyle(this.mNative, i);
            String strApplyStyles = strNativeGetString;
            if (iArrNativeGetStyle != null) {
                if (this.mStyleIDs == null) {
                    this.mStyleIDs = new StyleIDs();
                }
                for (int i2 = 0; i2 < iArrNativeGetStyle.length; i2 += 3) {
                    int i3 = iArrNativeGetStyle[i2];
                    if (i3 != this.mStyleIDs.boldId && i3 != this.mStyleIDs.italicId && i3 != this.mStyleIDs.underlineId && i3 != this.mStyleIDs.ttId && i3 != this.mStyleIDs.bigId && i3 != this.mStyleIDs.smallId && i3 != this.mStyleIDs.subId && i3 != this.mStyleIDs.supId && i3 != this.mStyleIDs.strikeId && i3 != this.mStyleIDs.listItemId && i3 != this.mStyleIDs.marqueeId) {
                        String strNativeGetString2 = nativeGetString(this.mNative, i3);
                        if (!strNativeGetString2.equals("b")) {
                            if (!strNativeGetString2.equals("i")) {
                                if (!strNativeGetString2.equals("u")) {
                                    if (!strNativeGetString2.equals(TtmlUtils.TAG_TT)) {
                                        if (!strNativeGetString2.equals("big")) {
                                            if (!strNativeGetString2.equals("small")) {
                                                if (!strNativeGetString2.equals("sup")) {
                                                    if (!strNativeGetString2.equals(Telephony.BaseMmsColumns.SUBJECT)) {
                                                        if (!strNativeGetString2.equals("strike")) {
                                                            if (!strNativeGetString2.equals("li")) {
                                                                if (strNativeGetString2.equals("marquee")) {
                                                                    this.mStyleIDs.marqueeId = i3;
                                                                }
                                                            } else {
                                                                this.mStyleIDs.listItemId = i3;
                                                            }
                                                        } else {
                                                            this.mStyleIDs.strikeId = i3;
                                                        }
                                                    } else {
                                                        this.mStyleIDs.subId = i3;
                                                    }
                                                } else {
                                                    this.mStyleIDs.supId = i3;
                                                }
                                            } else {
                                                this.mStyleIDs.smallId = i3;
                                            }
                                        } else {
                                            this.mStyleIDs.bigId = i3;
                                        }
                                    } else {
                                        this.mStyleIDs.ttId = i3;
                                    }
                                } else {
                                    this.mStyleIDs.underlineId = i3;
                                }
                            } else {
                                this.mStyleIDs.italicId = i3;
                            }
                        } else {
                            this.mStyleIDs.boldId = i3;
                        }
                    }
                }
                strApplyStyles = applyStyles(strNativeGetString, iArrNativeGetStyle, this.mStyleIDs);
            }
            if (this.mStrings != null) {
                this.mStrings[i] = strApplyStyles;
            } else {
                this.mSparseStrings.put(i, strApplyStyles);
            }
            return strApplyStyles;
        }
    }

    protected void finalize() throws Throwable {
        try {
            super.finalize();
        } finally {
            if (this.mOwnsNative) {
                nativeDestroy(this.mNative);
            }
        }
    }

    static final class StyleIDs {
        private int boldId = -1;
        private int italicId = -1;
        private int underlineId = -1;
        private int ttId = -1;
        private int bigId = -1;
        private int smallId = -1;
        private int subId = -1;
        private int supId = -1;
        private int strikeId = -1;
        private int listItemId = -1;
        private int marqueeId = -1;

        StyleIDs() {
        }
    }

    private CharSequence applyStyles(String str, int[] iArr, StyleIDs styleIDs) {
        if (iArr.length == 0) {
            return str;
        }
        SpannableString spannableString = new SpannableString(str);
        for (int i = 0; i < iArr.length; i += 3) {
            int i2 = iArr[i];
            if (i2 != styleIDs.boldId) {
                if (i2 != styleIDs.italicId) {
                    if (i2 != styleIDs.underlineId) {
                        if (i2 != styleIDs.ttId) {
                            if (i2 != styleIDs.bigId) {
                                if (i2 != styleIDs.smallId) {
                                    if (i2 != styleIDs.subId) {
                                        if (i2 != styleIDs.supId) {
                                            if (i2 != styleIDs.strikeId) {
                                                if (i2 != styleIDs.listItemId) {
                                                    if (i2 == styleIDs.marqueeId) {
                                                        spannableString.setSpan(TextUtils.TruncateAt.MARQUEE, iArr[i + 1], iArr[i + 2] + 1, 18);
                                                    } else {
                                                        String strNativeGetString = nativeGetString(this.mNative, i2);
                                                        if (strNativeGetString.startsWith("font;")) {
                                                            String strSubtag = subtag(strNativeGetString, ";height=");
                                                            if (strSubtag != null) {
                                                                addParagraphSpan(spannableString, new Height(Integer.parseInt(strSubtag)), iArr[i + 1], iArr[i + 2] + 1);
                                                            }
                                                            String strSubtag2 = subtag(strNativeGetString, ";size=");
                                                            if (strSubtag2 != null) {
                                                                spannableString.setSpan(new AbsoluteSizeSpan(Integer.parseInt(strSubtag2), true), iArr[i + 1], iArr[i + 2] + 1, 33);
                                                            }
                                                            String strSubtag3 = subtag(strNativeGetString, ";fgcolor=");
                                                            if (strSubtag3 != null) {
                                                                spannableString.setSpan(getColor(strSubtag3, true), iArr[i + 1], iArr[i + 2] + 1, 33);
                                                            }
                                                            String strSubtag4 = subtag(strNativeGetString, ";color=");
                                                            if (strSubtag4 != null) {
                                                                spannableString.setSpan(getColor(strSubtag4, true), iArr[i + 1], iArr[i + 2] + 1, 33);
                                                            }
                                                            String strSubtag5 = subtag(strNativeGetString, ";bgcolor=");
                                                            if (strSubtag5 != null) {
                                                                spannableString.setSpan(getColor(strSubtag5, false), iArr[i + 1], iArr[i + 2] + 1, 33);
                                                            }
                                                            String strSubtag6 = subtag(strNativeGetString, ";face=");
                                                            if (strSubtag6 != null) {
                                                                spannableString.setSpan(new TypefaceSpan(strSubtag6), iArr[i + 1], iArr[i + 2] + 1, 33);
                                                            }
                                                        } else if (strNativeGetString.startsWith("a;")) {
                                                            String strSubtag7 = subtag(strNativeGetString, ";href=");
                                                            if (strSubtag7 != null) {
                                                                spannableString.setSpan(new URLSpan(strSubtag7), iArr[i + 1], iArr[i + 2] + 1, 33);
                                                            }
                                                        } else if (strNativeGetString.startsWith("annotation;")) {
                                                            int length = strNativeGetString.length();
                                                            int iIndexOf = strNativeGetString.indexOf(59);
                                                            while (iIndexOf < length) {
                                                                int iIndexOf2 = strNativeGetString.indexOf(61, iIndexOf);
                                                                if (iIndexOf2 < 0) {
                                                                    break;
                                                                }
                                                                int iIndexOf3 = strNativeGetString.indexOf(59, iIndexOf2);
                                                                if (iIndexOf3 < 0) {
                                                                    iIndexOf3 = length;
                                                                }
                                                                spannableString.setSpan(new Annotation(strNativeGetString.substring(iIndexOf + 1, iIndexOf2), strNativeGetString.substring(iIndexOf2 + 1, iIndexOf3)), iArr[i + 1], iArr[i + 2] + 1, 33);
                                                                iIndexOf = iIndexOf3;
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    addParagraphSpan(spannableString, new BulletSpan(10), iArr[i + 1], iArr[i + 2] + 1);
                                                }
                                            } else {
                                                spannableString.setSpan(new StrikethroughSpan(), iArr[i + 1], iArr[i + 2] + 1, 33);
                                            }
                                        } else {
                                            spannableString.setSpan(new SuperscriptSpan(), iArr[i + 1], iArr[i + 2] + 1, 33);
                                        }
                                    } else {
                                        spannableString.setSpan(new SubscriptSpan(), iArr[i + 1], iArr[i + 2] + 1, 33);
                                    }
                                } else {
                                    spannableString.setSpan(new RelativeSizeSpan(0.8f), iArr[i + 1], iArr[i + 2] + 1, 33);
                                }
                            } else {
                                spannableString.setSpan(new RelativeSizeSpan(1.25f), iArr[i + 1], iArr[i + 2] + 1, 33);
                            }
                        } else {
                            spannableString.setSpan(new TypefaceSpan("monospace"), iArr[i + 1], iArr[i + 2] + 1, 33);
                        }
                    } else {
                        spannableString.setSpan(new UnderlineSpan(), iArr[i + 1], iArr[i + 2] + 1, 33);
                    }
                } else {
                    spannableString.setSpan(new StyleSpan(2), iArr[i + 1], iArr[i + 2] + 1, 33);
                }
            } else {
                spannableString.setSpan(new StyleSpan(1), iArr[i + 1], iArr[i + 2] + 1, 33);
            }
        }
        return new SpannedString(spannableString);
    }

    private static CharacterStyle getColor(String str, boolean z) {
        int color = -16777216;
        if (!TextUtils.isEmpty(str)) {
            if (str.startsWith("@")) {
                Resources system = Resources.getSystem();
                int identifier = system.getIdentifier(str.substring(1), "color", ZenModeConfig.SYSTEM_AUTHORITY);
                if (identifier != 0) {
                    ColorStateList colorStateList = system.getColorStateList(identifier, null);
                    if (z) {
                        return new TextAppearanceSpan(null, 0, 0, colorStateList, null);
                    }
                    color = colorStateList.getDefaultColor();
                }
            } else {
                try {
                    color = Color.parseColor(str);
                } catch (IllegalArgumentException e) {
                }
            }
        }
        if (z) {
            return new ForegroundColorSpan(color);
        }
        return new BackgroundColorSpan(color);
    }

    private static void addParagraphSpan(Spannable spannable, Object obj, int i, int i2) {
        int length = spannable.length();
        if (i != 0 && i != length && spannable.charAt(i - 1) != '\n') {
            do {
                i--;
                if (i <= 0) {
                    break;
                }
            } while (spannable.charAt(i - 1) != '\n');
        }
        if (i2 != 0 && i2 != length && spannable.charAt(i2 - 1) != '\n') {
            do {
                i2++;
                if (i2 >= length) {
                    break;
                }
            } while (spannable.charAt(i2 - 1) != '\n');
        }
        spannable.setSpan(obj, i, i2, 51);
    }

    private static String subtag(String str, String str2) {
        int iIndexOf = str.indexOf(str2);
        if (iIndexOf < 0) {
            return null;
        }
        int length = iIndexOf + str2.length();
        int iIndexOf2 = str.indexOf(59, length);
        if (iIndexOf2 < 0) {
            return str.substring(length);
        }
        return str.substring(length, iIndexOf2);
    }

    private static class Height implements LineHeightSpan.WithDensity {
        private static float sProportion = 0.0f;
        private int mSize;

        public Height(int i) {
            this.mSize = i;
        }

        @Override
        public void chooseHeight(CharSequence charSequence, int i, int i2, int i3, int i4, Paint.FontMetricsInt fontMetricsInt) {
            chooseHeight(charSequence, i, i2, i3, i4, fontMetricsInt, null);
        }

        @Override
        public void chooseHeight(CharSequence charSequence, int i, int i2, int i3, int i4, Paint.FontMetricsInt fontMetricsInt, TextPaint textPaint) {
            int i5 = this.mSize;
            if (textPaint != null) {
                i5 = (int) (i5 * textPaint.density);
            }
            if (fontMetricsInt.bottom - fontMetricsInt.top < i5) {
                fontMetricsInt.top = fontMetricsInt.bottom - i5;
                fontMetricsInt.ascent -= i5;
                return;
            }
            if (sProportion == 0.0f) {
                Paint paint = new Paint();
                paint.setTextSize(100.0f);
                paint.getTextBounds("ABCDEFG", 0, 7, new Rect());
                sProportion = r4.top / paint.ascent();
            }
            int iCeil = (int) Math.ceil((-fontMetricsInt.top) * sProportion);
            if (i5 - fontMetricsInt.descent >= iCeil) {
                fontMetricsInt.top = fontMetricsInt.bottom - i5;
                fontMetricsInt.ascent = fontMetricsInt.descent - i5;
                return;
            }
            if (i5 >= iCeil) {
                int i6 = -iCeil;
                fontMetricsInt.ascent = i6;
                fontMetricsInt.top = i6;
                int i7 = fontMetricsInt.top + i5;
                fontMetricsInt.descent = i7;
                fontMetricsInt.bottom = i7;
                return;
            }
            int i8 = -i5;
            fontMetricsInt.ascent = i8;
            fontMetricsInt.top = i8;
            fontMetricsInt.descent = 0;
            fontMetricsInt.bottom = 0;
        }
    }

    StringBlock(long j, boolean z) {
        this.mNative = j;
        this.mUseSparse = z;
    }
}
