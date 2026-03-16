package android.content.res;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.TtmlUtils;
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

    public StringBlock(byte[] data, boolean useSparse) {
        this.mNative = nativeCreate(data, 0, data.length);
        this.mUseSparse = useSparse;
    }

    public StringBlock(byte[] data, int offset, int size, boolean useSparse) {
        this.mNative = nativeCreate(data, offset, size);
        this.mUseSparse = useSparse;
    }

    public CharSequence get(int idx) {
        CharSequence res;
        int[] style;
        synchronized (this) {
            if (this.mStrings != null) {
                res = this.mStrings[idx];
                if (res == null) {
                    String str = nativeGetString(this.mNative, idx);
                    res = str;
                    style = nativeGetStyle(this.mNative, idx);
                    if (style != null) {
                        if (this.mStyleIDs == null) {
                            this.mStyleIDs = new StyleIDs();
                        }
                        for (int styleIndex = 0; styleIndex < style.length; styleIndex += 3) {
                            int styleId = style[styleIndex];
                            if (styleId != this.mStyleIDs.boldId && styleId != this.mStyleIDs.italicId && styleId != this.mStyleIDs.underlineId && styleId != this.mStyleIDs.ttId && styleId != this.mStyleIDs.bigId && styleId != this.mStyleIDs.smallId && styleId != this.mStyleIDs.subId && styleId != this.mStyleIDs.supId && styleId != this.mStyleIDs.strikeId && styleId != this.mStyleIDs.listItemId && styleId != this.mStyleIDs.marqueeId) {
                                String styleTag = nativeGetString(this.mNative, styleId);
                                if (!styleTag.equals("b")) {
                                    if (!styleTag.equals("i")) {
                                        if (!styleTag.equals("u")) {
                                            if (!styleTag.equals(TtmlUtils.TAG_TT)) {
                                                if (!styleTag.equals("big")) {
                                                    if (!styleTag.equals("small")) {
                                                        if (!styleTag.equals("sup")) {
                                                            if (!styleTag.equals("sub")) {
                                                                if (!styleTag.equals("strike")) {
                                                                    if (!styleTag.equals("li")) {
                                                                        if (styleTag.equals("marquee")) {
                                                                            this.mStyleIDs.marqueeId = styleId;
                                                                        }
                                                                    } else {
                                                                        this.mStyleIDs.listItemId = styleId;
                                                                    }
                                                                } else {
                                                                    this.mStyleIDs.strikeId = styleId;
                                                                }
                                                            } else {
                                                                this.mStyleIDs.subId = styleId;
                                                            }
                                                        } else {
                                                            this.mStyleIDs.supId = styleId;
                                                        }
                                                    } else {
                                                        this.mStyleIDs.smallId = styleId;
                                                    }
                                                } else {
                                                    this.mStyleIDs.bigId = styleId;
                                                }
                                            } else {
                                                this.mStyleIDs.ttId = styleId;
                                            }
                                        } else {
                                            this.mStyleIDs.underlineId = styleId;
                                        }
                                    } else {
                                        this.mStyleIDs.italicId = styleId;
                                    }
                                } else {
                                    this.mStyleIDs.boldId = styleId;
                                }
                            }
                        }
                        res = applyStyles(str, style, this.mStyleIDs);
                    }
                    if (this.mStrings == null) {
                        this.mStrings[idx] = res;
                    } else {
                        this.mSparseStrings.put(idx, res);
                    }
                }
            } else {
                if (this.mSparseStrings != null) {
                    res = this.mSparseStrings.get(idx);
                    if (res != null) {
                    }
                } else {
                    int num = nativeGetSize(this.mNative);
                    if (this.mUseSparse && num > 250) {
                        this.mSparseStrings = new SparseArray<>();
                    } else {
                        this.mStrings = new CharSequence[num];
                    }
                }
                String str2 = nativeGetString(this.mNative, idx);
                res = str2;
                style = nativeGetStyle(this.mNative, idx);
                if (style != null) {
                }
                if (this.mStrings == null) {
                }
            }
        }
        return res;
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

    private CharSequence applyStyles(String str, int[] style, StyleIDs ids) {
        if (style.length != 0) {
            SpannableString buffer = new SpannableString(str);
            for (int i = 0; i < style.length; i += 3) {
                int type = style[i];
                if (type != ids.boldId) {
                    if (type != ids.italicId) {
                        if (type != ids.underlineId) {
                            if (type != ids.ttId) {
                                if (type != ids.bigId) {
                                    if (type != ids.smallId) {
                                        if (type != ids.subId) {
                                            if (type != ids.supId) {
                                                if (type != ids.strikeId) {
                                                    if (type != ids.listItemId) {
                                                        if (type == ids.marqueeId) {
                                                            buffer.setSpan(TextUtils.TruncateAt.MARQUEE, style[i + 1], style[i + 2] + 1, 18);
                                                        } else {
                                                            String tag = nativeGetString(this.mNative, type);
                                                            if (tag.startsWith("font;")) {
                                                                String sub = subtag(tag, ";height=");
                                                                if (sub != null) {
                                                                    int size = Integer.parseInt(sub);
                                                                    addParagraphSpan(buffer, new Height(size), style[i + 1], style[i + 2] + 1);
                                                                }
                                                                String sub2 = subtag(tag, ";size=");
                                                                if (sub2 != null) {
                                                                    int size2 = Integer.parseInt(sub2);
                                                                    buffer.setSpan(new AbsoluteSizeSpan(size2, true), style[i + 1], style[i + 2] + 1, 33);
                                                                }
                                                                String sub3 = subtag(tag, ";fgcolor=");
                                                                if (sub3 != null) {
                                                                    buffer.setSpan(getColor(sub3, true), style[i + 1], style[i + 2] + 1, 33);
                                                                }
                                                                String sub4 = subtag(tag, ";color=");
                                                                if (sub4 != null) {
                                                                    buffer.setSpan(getColor(sub4, true), style[i + 1], style[i + 2] + 1, 33);
                                                                }
                                                                String sub5 = subtag(tag, ";bgcolor=");
                                                                if (sub5 != null) {
                                                                    buffer.setSpan(getColor(sub5, false), style[i + 1], style[i + 2] + 1, 33);
                                                                }
                                                                String sub6 = subtag(tag, ";face=");
                                                                if (sub6 != null) {
                                                                    buffer.setSpan(new TypefaceSpan(sub6), style[i + 1], style[i + 2] + 1, 33);
                                                                }
                                                            } else if (tag.startsWith("a;")) {
                                                                String sub7 = subtag(tag, ";href=");
                                                                if (sub7 != null) {
                                                                    buffer.setSpan(new URLSpan(sub7), style[i + 1], style[i + 2] + 1, 33);
                                                                }
                                                            } else if (tag.startsWith("annotation;")) {
                                                                int len = tag.length();
                                                                int t = tag.indexOf(59);
                                                                while (t < len) {
                                                                    int eq = tag.indexOf(61, t);
                                                                    if (eq >= 0) {
                                                                        int next = tag.indexOf(59, eq);
                                                                        if (next < 0) {
                                                                            next = len;
                                                                        }
                                                                        String key = tag.substring(t + 1, eq);
                                                                        String value = tag.substring(eq + 1, next);
                                                                        buffer.setSpan(new Annotation(key, value), style[i + 1], style[i + 2] + 1, 33);
                                                                        t = next;
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        addParagraphSpan(buffer, new BulletSpan(10), style[i + 1], style[i + 2] + 1);
                                                    }
                                                } else {
                                                    buffer.setSpan(new StrikethroughSpan(), style[i + 1], style[i + 2] + 1, 33);
                                                }
                                            } else {
                                                buffer.setSpan(new SuperscriptSpan(), style[i + 1], style[i + 2] + 1, 33);
                                            }
                                        } else {
                                            buffer.setSpan(new SubscriptSpan(), style[i + 1], style[i + 2] + 1, 33);
                                        }
                                    } else {
                                        buffer.setSpan(new RelativeSizeSpan(0.8f), style[i + 1], style[i + 2] + 1, 33);
                                    }
                                } else {
                                    buffer.setSpan(new RelativeSizeSpan(1.25f), style[i + 1], style[i + 2] + 1, 33);
                                }
                            } else {
                                buffer.setSpan(new TypefaceSpan("monospace"), style[i + 1], style[i + 2] + 1, 33);
                            }
                        } else {
                            buffer.setSpan(new UnderlineSpan(), style[i + 1], style[i + 2] + 1, 33);
                        }
                    } else {
                        buffer.setSpan(new StyleSpan(2), style[i + 1], style[i + 2] + 1, 33);
                    }
                } else {
                    buffer.setSpan(new StyleSpan(1), style[i + 1], style[i + 2] + 1, 33);
                }
            }
            return new SpannedString(buffer);
        }
        return str;
    }

    private static CharacterStyle getColor(String color, boolean foreground) {
        int c = -16777216;
        if (!TextUtils.isEmpty(color)) {
            if (color.startsWith("@")) {
                Resources res = Resources.getSystem();
                String name = color.substring(1);
                int colorRes = res.getIdentifier(name, "color", ZenModeConfig.SYSTEM_AUTHORITY);
                if (colorRes != 0) {
                    ColorStateList colors = res.getColorStateList(colorRes);
                    if (foreground) {
                        return new TextAppearanceSpan(null, 0, 0, colors, null);
                    }
                    c = colors.getDefaultColor();
                }
            } else {
                c = Color.getHtmlColor(color);
            }
        }
        if (foreground) {
            return new ForegroundColorSpan(c);
        }
        return new BackgroundColorSpan(c);
    }

    private static void addParagraphSpan(Spannable buffer, Object what, int start, int end) {
        int len = buffer.length();
        if (start != 0 && start != len && buffer.charAt(start - 1) != '\n') {
            start--;
            while (start > 0 && buffer.charAt(start - 1) != '\n') {
                start--;
            }
        }
        if (end != 0 && end != len && buffer.charAt(end - 1) != '\n') {
            end++;
            while (end < len && buffer.charAt(end - 1) != '\n') {
                end++;
            }
        }
        buffer.setSpan(what, start, end, 51);
    }

    private static String subtag(String full, String attribute) {
        int start = full.indexOf(attribute);
        if (start < 0) {
            return null;
        }
        int start2 = start + attribute.length();
        int end = full.indexOf(59, start2);
        if (end < 0) {
            return full.substring(start2);
        }
        return full.substring(start2, end);
    }

    private static class Height implements LineHeightSpan.WithDensity {
        private static float sProportion = 0.0f;
        private int mSize;

        public Height(int size) {
            this.mSize = size;
        }

        @Override
        public void chooseHeight(CharSequence text, int start, int end, int spanstartv, int v, Paint.FontMetricsInt fm) {
            chooseHeight(text, start, end, spanstartv, v, fm, null);
        }

        @Override
        public void chooseHeight(CharSequence text, int start, int end, int spanstartv, int v, Paint.FontMetricsInt fm, TextPaint paint) {
            int size = this.mSize;
            if (paint != null) {
                size = (int) (size * paint.density);
            }
            if (fm.bottom - fm.top < size) {
                fm.top = fm.bottom - size;
                fm.ascent -= size;
                return;
            }
            if (sProportion == 0.0f) {
                Paint p = new Paint();
                p.setTextSize(100.0f);
                Rect r = new Rect();
                p.getTextBounds("ABCDEFG", 0, 7, r);
                sProportion = r.top / p.ascent();
            }
            int need = (int) Math.ceil((-fm.top) * sProportion);
            if (size - fm.descent >= need) {
                fm.top = fm.bottom - size;
                fm.ascent = fm.descent - size;
                return;
            }
            if (size >= need) {
                int i = -need;
                fm.ascent = i;
                fm.top = i;
                int i2 = fm.top + size;
                fm.descent = i2;
                fm.bottom = i2;
                return;
            }
            int i3 = -size;
            fm.ascent = i3;
            fm.top = i3;
            fm.descent = 0;
            fm.bottom = 0;
        }
    }

    StringBlock(long obj, boolean useSparse) {
        this.mNative = obj;
        this.mUseSparse = useSparse;
    }
}
