package android.text;

import android.app.backup.FullBackup;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.TtmlUtils;
import android.provider.Telephony;
import android.text.Html;
import android.text.Layout;
import android.text.style.AlignmentSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.BulletSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.ParagraphStyle;
import android.text.style.QuoteSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.TypefaceSpan;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;
import com.android.internal.R;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.ccil.cowan.tagsoup.Parser;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

class HtmlToSpannedConverter implements ContentHandler {
    private static Pattern sBackgroundColorPattern;
    private static Pattern sForegroundColorPattern;
    private static Pattern sTextAlignPattern;
    private static Pattern sTextDecorationPattern;
    private int mFlags;
    private Html.ImageGetter mImageGetter;
    private XMLReader mReader;
    private String mSource;
    private SpannableStringBuilder mSpannableStringBuilder = new SpannableStringBuilder();
    private Html.TagHandler mTagHandler;
    private static final float[] HEADING_SIZES = {1.5f, 1.4f, 1.3f, 1.2f, 1.1f, 1.0f};
    private static final Map<String, Integer> sColorMap = new HashMap();

    static {
        sColorMap.put("darkgray", -5658199);
        sColorMap.put("gray", -8355712);
        sColorMap.put("lightgray", -2894893);
        sColorMap.put("darkgrey", -5658199);
        sColorMap.put("grey", -8355712);
        sColorMap.put("lightgrey", -2894893);
        sColorMap.put("green", -16744448);
    }

    private static Pattern getTextAlignPattern() {
        if (sTextAlignPattern == null) {
            sTextAlignPattern = Pattern.compile("(?:\\s+|\\A)text-align\\s*:\\s*(\\S*)\\b");
        }
        return sTextAlignPattern;
    }

    private static Pattern getForegroundColorPattern() {
        if (sForegroundColorPattern == null) {
            sForegroundColorPattern = Pattern.compile("(?:\\s+|\\A)color\\s*:\\s*(\\S*)\\b");
        }
        return sForegroundColorPattern;
    }

    private static Pattern getBackgroundColorPattern() {
        if (sBackgroundColorPattern == null) {
            sBackgroundColorPattern = Pattern.compile("(?:\\s+|\\A)background(?:-color)?\\s*:\\s*(\\S*)\\b");
        }
        return sBackgroundColorPattern;
    }

    private static Pattern getTextDecorationPattern() {
        if (sTextDecorationPattern == null) {
            sTextDecorationPattern = Pattern.compile("(?:\\s+|\\A)text-decoration\\s*:\\s*(\\S*)\\b");
        }
        return sTextDecorationPattern;
    }

    public HtmlToSpannedConverter(String str, Html.ImageGetter imageGetter, Html.TagHandler tagHandler, Parser parser, int i) {
        this.mSource = str;
        this.mImageGetter = imageGetter;
        this.mTagHandler = tagHandler;
        this.mReader = parser;
        this.mFlags = i;
    }

    public Spanned convert() {
        this.mReader.setContentHandler(this);
        try {
            this.mReader.parse(new InputSource(new StringReader(this.mSource)));
            Object[] spans = this.mSpannableStringBuilder.getSpans(0, this.mSpannableStringBuilder.length(), ParagraphStyle.class);
            for (int i = 0; i < spans.length; i++) {
                int spanStart = this.mSpannableStringBuilder.getSpanStart(spans[i]);
                int spanEnd = this.mSpannableStringBuilder.getSpanEnd(spans[i]);
                int i2 = spanEnd - 2;
                if (i2 >= 0 && this.mSpannableStringBuilder.charAt(spanEnd - 1) == '\n' && this.mSpannableStringBuilder.charAt(i2) == '\n') {
                    spanEnd--;
                }
                if (spanEnd == spanStart) {
                    this.mSpannableStringBuilder.removeSpan(spans[i]);
                } else {
                    this.mSpannableStringBuilder.setSpan(spans[i], spanStart, spanEnd, 51);
                }
            }
            return this.mSpannableStringBuilder;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SAXException e2) {
            throw new RuntimeException(e2);
        }
    }

    private void handleStartTag(String str, Attributes attributes) {
        if (!str.equalsIgnoreCase(TtmlUtils.TAG_BR)) {
            if (str.equalsIgnoreCase(TtmlUtils.TAG_P)) {
                startBlockElement(this.mSpannableStringBuilder, attributes, getMarginParagraph());
                startCssStyle(this.mSpannableStringBuilder, attributes);
                return;
            }
            if (str.equalsIgnoreCase("ul")) {
                startBlockElement(this.mSpannableStringBuilder, attributes, getMarginList());
                return;
            }
            if (str.equalsIgnoreCase("li")) {
                startLi(this.mSpannableStringBuilder, attributes);
                return;
            }
            if (str.equalsIgnoreCase(TtmlUtils.TAG_DIV)) {
                startBlockElement(this.mSpannableStringBuilder, attributes, getMarginDiv());
                return;
            }
            if (str.equalsIgnoreCase(TtmlUtils.TAG_SPAN)) {
                startCssStyle(this.mSpannableStringBuilder, attributes);
                return;
            }
            if (str.equalsIgnoreCase("strong")) {
                start(this.mSpannableStringBuilder, new Bold());
                return;
            }
            if (str.equalsIgnoreCase("b")) {
                start(this.mSpannableStringBuilder, new Bold());
                return;
            }
            if (str.equalsIgnoreCase("em")) {
                start(this.mSpannableStringBuilder, new Italic());
                return;
            }
            if (str.equalsIgnoreCase("cite")) {
                start(this.mSpannableStringBuilder, new Italic());
                return;
            }
            if (str.equalsIgnoreCase("dfn")) {
                start(this.mSpannableStringBuilder, new Italic());
                return;
            }
            if (str.equalsIgnoreCase("i")) {
                start(this.mSpannableStringBuilder, new Italic());
                return;
            }
            if (str.equalsIgnoreCase("big")) {
                start(this.mSpannableStringBuilder, new Big());
                return;
            }
            if (str.equalsIgnoreCase("small")) {
                start(this.mSpannableStringBuilder, new Small());
                return;
            }
            if (str.equalsIgnoreCase("font")) {
                startFont(this.mSpannableStringBuilder, attributes);
                return;
            }
            if (str.equalsIgnoreCase("blockquote")) {
                startBlockquote(this.mSpannableStringBuilder, attributes);
                return;
            }
            if (str.equalsIgnoreCase(TtmlUtils.TAG_TT)) {
                start(this.mSpannableStringBuilder, new Monospace());
                return;
            }
            if (str.equalsIgnoreCase(FullBackup.APK_TREE_TOKEN)) {
                startA(this.mSpannableStringBuilder, attributes);
                return;
            }
            if (str.equalsIgnoreCase("u")) {
                start(this.mSpannableStringBuilder, new Underline());
                return;
            }
            if (str.equalsIgnoreCase("del")) {
                start(this.mSpannableStringBuilder, new Strikethrough());
                return;
            }
            if (str.equalsIgnoreCase("s")) {
                start(this.mSpannableStringBuilder, new Strikethrough());
                return;
            }
            if (str.equalsIgnoreCase("strike")) {
                start(this.mSpannableStringBuilder, new Strikethrough());
                return;
            }
            if (str.equalsIgnoreCase("sup")) {
                start(this.mSpannableStringBuilder, new Super());
                return;
            }
            if (str.equalsIgnoreCase(Telephony.BaseMmsColumns.SUBJECT)) {
                start(this.mSpannableStringBuilder, new Sub());
                return;
            }
            if (str.length() == 2 && Character.toLowerCase(str.charAt(0)) == 'h' && str.charAt(1) >= '1' && str.charAt(1) <= '6') {
                startHeading(this.mSpannableStringBuilder, attributes, str.charAt(1) - '1');
            } else if (str.equalsIgnoreCase("img")) {
                startImg(this.mSpannableStringBuilder, attributes, this.mImageGetter);
            } else if (this.mTagHandler != null) {
                this.mTagHandler.handleTag(true, str, this.mSpannableStringBuilder, this.mReader);
            }
        }
    }

    private void handleEndTag(String str) {
        if (str.equalsIgnoreCase(TtmlUtils.TAG_BR)) {
            handleBr(this.mSpannableStringBuilder);
            return;
        }
        if (str.equalsIgnoreCase(TtmlUtils.TAG_P)) {
            endCssStyle(this.mSpannableStringBuilder);
            endBlockElement(this.mSpannableStringBuilder);
            return;
        }
        if (str.equalsIgnoreCase("ul")) {
            endBlockElement(this.mSpannableStringBuilder);
            return;
        }
        if (str.equalsIgnoreCase("li")) {
            endLi(this.mSpannableStringBuilder);
            return;
        }
        if (str.equalsIgnoreCase(TtmlUtils.TAG_DIV)) {
            endBlockElement(this.mSpannableStringBuilder);
            return;
        }
        if (str.equalsIgnoreCase(TtmlUtils.TAG_SPAN)) {
            endCssStyle(this.mSpannableStringBuilder);
            return;
        }
        if (str.equalsIgnoreCase("strong")) {
            end(this.mSpannableStringBuilder, Bold.class, new StyleSpan(1));
            return;
        }
        if (str.equalsIgnoreCase("b")) {
            end(this.mSpannableStringBuilder, Bold.class, new StyleSpan(1));
            return;
        }
        if (str.equalsIgnoreCase("em")) {
            end(this.mSpannableStringBuilder, Italic.class, new StyleSpan(2));
            return;
        }
        if (str.equalsIgnoreCase("cite")) {
            end(this.mSpannableStringBuilder, Italic.class, new StyleSpan(2));
            return;
        }
        if (str.equalsIgnoreCase("dfn")) {
            end(this.mSpannableStringBuilder, Italic.class, new StyleSpan(2));
            return;
        }
        if (str.equalsIgnoreCase("i")) {
            end(this.mSpannableStringBuilder, Italic.class, new StyleSpan(2));
            return;
        }
        if (str.equalsIgnoreCase("big")) {
            end(this.mSpannableStringBuilder, Big.class, new RelativeSizeSpan(1.25f));
            return;
        }
        if (str.equalsIgnoreCase("small")) {
            end(this.mSpannableStringBuilder, Small.class, new RelativeSizeSpan(0.8f));
            return;
        }
        if (str.equalsIgnoreCase("font")) {
            endFont(this.mSpannableStringBuilder);
            return;
        }
        if (str.equalsIgnoreCase("blockquote")) {
            endBlockquote(this.mSpannableStringBuilder);
            return;
        }
        if (str.equalsIgnoreCase(TtmlUtils.TAG_TT)) {
            end(this.mSpannableStringBuilder, Monospace.class, new TypefaceSpan("monospace"));
            return;
        }
        if (str.equalsIgnoreCase(FullBackup.APK_TREE_TOKEN)) {
            endA(this.mSpannableStringBuilder);
            return;
        }
        if (str.equalsIgnoreCase("u")) {
            end(this.mSpannableStringBuilder, Underline.class, new UnderlineSpan());
            return;
        }
        if (str.equalsIgnoreCase("del")) {
            end(this.mSpannableStringBuilder, Strikethrough.class, new StrikethroughSpan());
            return;
        }
        if (str.equalsIgnoreCase("s")) {
            end(this.mSpannableStringBuilder, Strikethrough.class, new StrikethroughSpan());
            return;
        }
        if (str.equalsIgnoreCase("strike")) {
            end(this.mSpannableStringBuilder, Strikethrough.class, new StrikethroughSpan());
            return;
        }
        if (str.equalsIgnoreCase("sup")) {
            end(this.mSpannableStringBuilder, Super.class, new SuperscriptSpan());
            return;
        }
        if (str.equalsIgnoreCase(Telephony.BaseMmsColumns.SUBJECT)) {
            end(this.mSpannableStringBuilder, Sub.class, new SubscriptSpan());
            return;
        }
        if (str.length() == 2 && Character.toLowerCase(str.charAt(0)) == 'h' && str.charAt(1) >= '1' && str.charAt(1) <= '6') {
            endHeading(this.mSpannableStringBuilder);
        } else if (this.mTagHandler != null) {
            this.mTagHandler.handleTag(false, str, this.mSpannableStringBuilder, this.mReader);
        }
    }

    private int getMarginParagraph() {
        return getMargin(1);
    }

    private int getMarginHeading() {
        return getMargin(2);
    }

    private int getMarginListItem() {
        return getMargin(4);
    }

    private int getMarginList() {
        return getMargin(8);
    }

    private int getMarginDiv() {
        return getMargin(16);
    }

    private int getMarginBlockquote() {
        return getMargin(32);
    }

    private int getMargin(int i) {
        if ((i & this.mFlags) != 0) {
            return 1;
        }
        return 2;
    }

    private static void appendNewlines(Editable editable, int i) {
        int length = editable.length();
        if (length == 0) {
            return;
        }
        int i2 = 0;
        for (int i3 = length - 1; i3 >= 0 && editable.charAt(i3) == '\n'; i3--) {
            i2++;
        }
        while (i2 < i) {
            editable.append("\n");
            i2++;
        }
    }

    private static void startBlockElement(Editable editable, Attributes attributes, int i) {
        editable.length();
        if (i > 0) {
            appendNewlines(editable, i);
            start(editable, new Newline(i));
        }
        String value = attributes.getValue("", TtmlUtils.TAG_STYLE);
        if (value != null) {
            Matcher matcher = getTextAlignPattern().matcher(value);
            if (matcher.find()) {
                String strGroup = matcher.group(1);
                if (strGroup.equalsIgnoreCase(Telephony.BaseMmsColumns.START)) {
                    start(editable, new Alignment(Layout.Alignment.ALIGN_NORMAL));
                } else if (strGroup.equalsIgnoreCase("center")) {
                    start(editable, new Alignment(Layout.Alignment.ALIGN_CENTER));
                } else if (strGroup.equalsIgnoreCase("end")) {
                    start(editable, new Alignment(Layout.Alignment.ALIGN_OPPOSITE));
                }
            }
        }
    }

    private static void endBlockElement(Editable editable) {
        Newline newline = (Newline) getLast(editable, Newline.class);
        if (newline != null) {
            appendNewlines(editable, newline.mNumNewlines);
            editable.removeSpan(newline);
        }
        Alignment alignment = (Alignment) getLast(editable, Alignment.class);
        if (alignment != null) {
            setSpanFromMark(editable, alignment, new AlignmentSpan.Standard(alignment.mAlignment));
        }
    }

    private static void handleBr(Editable editable) {
        editable.append('\n');
    }

    private void startLi(Editable editable, Attributes attributes) {
        startBlockElement(editable, attributes, getMarginListItem());
        start(editable, new Bullet());
        startCssStyle(editable, attributes);
    }

    private static void endLi(Editable editable) {
        endCssStyle(editable);
        endBlockElement(editable);
        end(editable, Bullet.class, new BulletSpan());
    }

    private void startBlockquote(Editable editable, Attributes attributes) {
        startBlockElement(editable, attributes, getMarginBlockquote());
        start(editable, new Blockquote());
    }

    private static void endBlockquote(Editable editable) {
        endBlockElement(editable);
        end(editable, Blockquote.class, new QuoteSpan());
    }

    private void startHeading(Editable editable, Attributes attributes, int i) {
        startBlockElement(editable, attributes, getMarginHeading());
        start(editable, new Heading(i));
    }

    private static void endHeading(Editable editable) {
        Heading heading = (Heading) getLast(editable, Heading.class);
        if (heading != null) {
            setSpanFromMark(editable, heading, new RelativeSizeSpan(HEADING_SIZES[heading.mLevel]), new StyleSpan(1));
        }
        endBlockElement(editable);
    }

    private static <T> T getLast(Spanned spanned, Class<T> cls) {
        Object[] spans = spanned.getSpans(0, spanned.length(), cls);
        if (spans.length == 0) {
            return null;
        }
        return (T) spans[spans.length - 1];
    }

    private static void setSpanFromMark(Spannable spannable, Object obj, Object... objArr) {
        int spanStart = spannable.getSpanStart(obj);
        spannable.removeSpan(obj);
        int length = spannable.length();
        if (spanStart != length) {
            for (Object obj2 : objArr) {
                spannable.setSpan(obj2, spanStart, length, 33);
            }
        }
    }

    private static void start(Editable editable, Object obj) {
        int length = editable.length();
        editable.setSpan(obj, length, length, 17);
    }

    private static void end(Editable editable, Class cls, Object obj) {
        editable.length();
        Object last = getLast(editable, cls);
        if (last != null) {
            setSpanFromMark(editable, last, obj);
        }
    }

    private void startCssStyle(Editable editable, Attributes attributes) {
        int htmlColor;
        int htmlColor2;
        String value = attributes.getValue("", TtmlUtils.TAG_STYLE);
        if (value != null) {
            Matcher matcher = getForegroundColorPattern().matcher(value);
            if (matcher.find() && (htmlColor2 = getHtmlColor(matcher.group(1))) != -1) {
                start(editable, new Foreground(htmlColor2 | (-16777216)));
            }
            Matcher matcher2 = getBackgroundColorPattern().matcher(value);
            if (matcher2.find() && (htmlColor = getHtmlColor(matcher2.group(1))) != -1) {
                start(editable, new Background(htmlColor | (-16777216)));
            }
            Matcher matcher3 = getTextDecorationPattern().matcher(value);
            if (matcher3.find() && matcher3.group(1).equalsIgnoreCase("line-through")) {
                start(editable, new Strikethrough());
            }
        }
    }

    private static void endCssStyle(Editable editable) {
        Strikethrough strikethrough = (Strikethrough) getLast(editable, Strikethrough.class);
        if (strikethrough != null) {
            setSpanFromMark(editable, strikethrough, new StrikethroughSpan());
        }
        Background background = (Background) getLast(editable, Background.class);
        if (background != null) {
            setSpanFromMark(editable, background, new BackgroundColorSpan(background.mBackgroundColor));
        }
        Foreground foreground = (Foreground) getLast(editable, Foreground.class);
        if (foreground != null) {
            setSpanFromMark(editable, foreground, new ForegroundColorSpan(foreground.mForegroundColor));
        }
    }

    private static void startImg(Editable editable, Attributes attributes, Html.ImageGetter imageGetter) {
        Drawable drawable;
        String value = attributes.getValue("", "src");
        if (imageGetter != null) {
            drawable = imageGetter.getDrawable(value);
        } else {
            drawable = null;
        }
        if (drawable == null) {
            drawable = Resources.getSystem().getDrawable(R.drawable.unknown_image);
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        }
        int length = editable.length();
        editable.append("￼");
        editable.setSpan(new ImageSpan(drawable, value), length, editable.length(), 33);
    }

    private void startFont(Editable editable, Attributes attributes) {
        int htmlColor;
        String value = attributes.getValue("", "color");
        String value2 = attributes.getValue("", "face");
        if (!TextUtils.isEmpty(value) && (htmlColor = getHtmlColor(value)) != -1) {
            start(editable, new Foreground(htmlColor | (-16777216)));
        }
        if (!TextUtils.isEmpty(value2)) {
            start(editable, new Font(value2));
        }
    }

    private static void endFont(Editable editable) {
        Font font = (Font) getLast(editable, Font.class);
        if (font != null) {
            setSpanFromMark(editable, font, new TypefaceSpan(font.mFace));
        }
        Foreground foreground = (Foreground) getLast(editable, Foreground.class);
        if (foreground != null) {
            setSpanFromMark(editable, foreground, new ForegroundColorSpan(foreground.mForegroundColor));
        }
    }

    private static void startA(Editable editable, Attributes attributes) {
        start(editable, new Href(attributes.getValue("", "href")));
    }

    private static void endA(Editable editable) {
        Href href = (Href) getLast(editable, Href.class);
        if (href != null && href.mHref != null) {
            setSpanFromMark(editable, href, new URLSpan(href.mHref));
        }
    }

    private int getHtmlColor(String str) {
        Integer num;
        if ((this.mFlags & 256) == 256 && (num = sColorMap.get(str.toLowerCase(Locale.US))) != null) {
            return num.intValue();
        }
        return Color.getHtmlColor(str);
    }

    @Override
    public void setDocumentLocator(Locator locator) {
    }

    @Override
    public void startDocument() throws SAXException {
    }

    @Override
    public void endDocument() throws SAXException {
    }

    @Override
    public void startPrefixMapping(String str, String str2) throws SAXException {
    }

    @Override
    public void endPrefixMapping(String str) throws SAXException {
    }

    @Override
    public void startElement(String str, String str2, String str3, Attributes attributes) throws SAXException {
        handleStartTag(str2, attributes);
    }

    @Override
    public void endElement(String str, String str2, String str3) throws SAXException {
        handleEndTag(str2);
    }

    @Override
    public void characters(char[] cArr, int i, int i2) throws SAXException {
        char cCharAt;
        StringBuilder sb = new StringBuilder();
        for (int i3 = 0; i3 < i2; i3++) {
            char c = cArr[i3 + i];
            if (c == ' ' || c == '\n') {
                int length = sb.length();
                if (length == 0) {
                    int length2 = this.mSpannableStringBuilder.length();
                    if (length2 != 0) {
                        cCharAt = this.mSpannableStringBuilder.charAt(length2 - 1);
                    } else {
                        cCharAt = '\n';
                    }
                } else {
                    cCharAt = sb.charAt(length - 1);
                }
                if (cCharAt != ' ' && cCharAt != '\n') {
                    sb.append(' ');
                }
            } else {
                sb.append(c);
            }
        }
        this.mSpannableStringBuilder.append((CharSequence) sb);
    }

    @Override
    public void ignorableWhitespace(char[] cArr, int i, int i2) throws SAXException {
    }

    @Override
    public void processingInstruction(String str, String str2) throws SAXException {
    }

    @Override
    public void skippedEntity(String str) throws SAXException {
    }

    private static class Bold {
        private Bold() {
        }
    }

    private static class Italic {
        private Italic() {
        }
    }

    private static class Underline {
        private Underline() {
        }
    }

    private static class Strikethrough {
        private Strikethrough() {
        }
    }

    private static class Big {
        private Big() {
        }
    }

    private static class Small {
        private Small() {
        }
    }

    private static class Monospace {
        private Monospace() {
        }
    }

    private static class Blockquote {
        private Blockquote() {
        }
    }

    private static class Super {
        private Super() {
        }
    }

    private static class Sub {
        private Sub() {
        }
    }

    private static class Bullet {
        private Bullet() {
        }
    }

    private static class Font {
        public String mFace;

        public Font(String str) {
            this.mFace = str;
        }
    }

    private static class Href {
        public String mHref;

        public Href(String str) {
            this.mHref = str;
        }
    }

    private static class Foreground {
        private int mForegroundColor;

        public Foreground(int i) {
            this.mForegroundColor = i;
        }
    }

    private static class Background {
        private int mBackgroundColor;

        public Background(int i) {
            this.mBackgroundColor = i;
        }
    }

    private static class Heading {
        private int mLevel;

        public Heading(int i) {
            this.mLevel = i;
        }
    }

    private static class Newline {
        private int mNumNewlines;

        public Newline(int i) {
            this.mNumNewlines = i;
        }
    }

    private static class Alignment {
        private Layout.Alignment mAlignment;

        public Alignment(Layout.Alignment alignment) {
            this.mAlignment = alignment;
        }
    }
}
