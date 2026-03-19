package android.text.util;

import android.content.Context;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.URLSpan;
import android.util.EventLog;
import android.util.Log;
import android.util.Patterns;
import android.view.textclassifier.TextClassifier;
import android.view.textclassifier.TextLinks;
import android.view.textclassifier.TextLinksParams;
import android.webkit.WebView;
import android.widget.TextView;
import com.android.i18n.phonenumbers.PhoneNumberMatch;
import com.android.i18n.phonenumbers.PhoneNumberUtil;
import com.android.internal.util.Preconditions;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import libcore.util.EmptyArray;
import mediatek.text.util.LinkifyExt;

public class Linkify {
    public static final int ALL = 15;
    public static final int EMAIL_ADDRESSES = 2;
    private static final String LOG_TAG = "Linkify";

    @Deprecated
    public static final int MAP_ADDRESSES = 8;
    public static final int PHONE_NUMBERS = 4;
    private static final int PHONE_NUMBER_MINIMUM_DIGITS = 5;
    public static final int WEB_URLS = 1;
    public static final MatchFilter sUrlMatchFilter = new MatchFilter() {
        @Override
        public final boolean acceptMatch(CharSequence charSequence, int i, int i2) {
            if (i == 0 || charSequence.charAt(i - 1) != '@') {
                return true;
            }
            return false;
        }
    };
    public static final MatchFilter sPhoneNumberMatchFilter = new MatchFilter() {
        @Override
        public final boolean acceptMatch(CharSequence charSequence, int i, int i2) {
            int i3 = 0;
            while (i < i2) {
                if (!Character.isDigit(charSequence.charAt(i)) || (i3 = i3 + 1) < 5) {
                    i++;
                } else {
                    return true;
                }
            }
            return false;
        }
    };
    public static final TransformFilter sPhoneNumberTransformFilter = new TransformFilter() {
        @Override
        public final String transformUrl(Matcher matcher, String str) {
            return Patterns.digitsAndPlusOnly(matcher);
        }
    };

    @Retention(RetentionPolicy.SOURCE)
    public @interface LinkifyMask {
    }

    public interface MatchFilter {
        boolean acceptMatch(CharSequence charSequence, int i, int i2);
    }

    public interface TransformFilter {
        String transformUrl(Matcher matcher, String str);
    }

    public static final boolean addLinks(Spannable spannable, int i) {
        return addLinks(spannable, i, (Context) null);
    }

    private static boolean addLinks(Spannable spannable, int i, Context context) {
        if (spannable != null && containsUnsupportedCharacters(spannable.toString())) {
            EventLog.writeEvent(1397638484, "116321860", -1, "");
            return false;
        }
        if (i == 0) {
            return false;
        }
        URLSpan[] uRLSpanArr = (URLSpan[]) spannable.getSpans(0, spannable.length(), URLSpan.class);
        for (int length = uRLSpanArr.length - 1; length >= 0; length--) {
            spannable.removeSpan(uRLSpanArr[length]);
        }
        ArrayList<LinkSpec> arrayList = new ArrayList();
        if ((i & 1) != 0) {
            gatherLinks(arrayList, spannable, LinkifyExt.getExtWebUrlPattern(Patterns.AUTOLINK_WEB_URL), LinkifyExt.getExtWebProtocolNames(new String[]{"http://", "https://", "rtsp://"}), sUrlMatchFilter, null);
        }
        if ((i & 2) != 0) {
            gatherLinks(arrayList, spannable, Patterns.AUTOLINK_EMAIL_ADDRESS, new String[]{"mailto:"}, null, null);
        }
        if ((i & 4) != 0) {
            gatherTelLinks(arrayList, spannable, context);
        }
        if ((i & 8) != 0) {
            gatherMapLinks(arrayList, spannable);
        }
        pruneOverlaps(arrayList);
        if (arrayList.size() == 0) {
            return false;
        }
        for (LinkSpec linkSpec : arrayList) {
            applyLink(linkSpec.url, linkSpec.start, linkSpec.end, spannable);
        }
        return true;
    }

    public static boolean containsUnsupportedCharacters(String str) {
        if (str.contains("\u202c")) {
            Log.e(LOG_TAG, "Unsupported character for applying links: u202C");
            return true;
        }
        if (str.contains("\u202d")) {
            Log.e(LOG_TAG, "Unsupported character for applying links: u202D");
            return true;
        }
        if (str.contains("\u202e")) {
            Log.e(LOG_TAG, "Unsupported character for applying links: u202E");
            return true;
        }
        return false;
    }

    public static final boolean addLinks(TextView textView, int i) {
        if (i == 0) {
            return false;
        }
        Context context = textView.getContext();
        CharSequence text = textView.getText();
        if (text instanceof Spannable) {
            if (!addLinks((Spannable) text, i, context)) {
                return false;
            }
            addLinkMovementMethod(textView);
            return true;
        }
        SpannableString spannableStringValueOf = SpannableString.valueOf(text);
        if (!addLinks(spannableStringValueOf, i, context)) {
            return false;
        }
        addLinkMovementMethod(textView);
        textView.setText(spannableStringValueOf);
        return true;
    }

    private static final void addLinkMovementMethod(TextView textView) {
        MovementMethod movementMethod = textView.getMovementMethod();
        if ((movementMethod == null || !(movementMethod instanceof LinkMovementMethod)) && textView.getLinksClickable()) {
            textView.setMovementMethod(LinkMovementMethod.getInstance());
        }
    }

    public static final void addLinks(TextView textView, Pattern pattern, String str) {
        addLinks(textView, pattern, str, (String[]) null, (MatchFilter) null, (TransformFilter) null);
    }

    public static final void addLinks(TextView textView, Pattern pattern, String str, MatchFilter matchFilter, TransformFilter transformFilter) {
        addLinks(textView, pattern, str, (String[]) null, matchFilter, transformFilter);
    }

    public static final void addLinks(TextView textView, Pattern pattern, String str, String[] strArr, MatchFilter matchFilter, TransformFilter transformFilter) {
        SpannableString spannableStringValueOf = SpannableString.valueOf(textView.getText());
        if (addLinks(spannableStringValueOf, pattern, str, strArr, matchFilter, transformFilter)) {
            textView.setText(spannableStringValueOf);
            addLinkMovementMethod(textView);
        }
    }

    public static final boolean addLinks(Spannable spannable, Pattern pattern, String str) {
        return addLinks(spannable, pattern, str, (String[]) null, (MatchFilter) null, (TransformFilter) null);
    }

    public static final boolean addLinks(Spannable spannable, Pattern pattern, String str, MatchFilter matchFilter, TransformFilter transformFilter) {
        return addLinks(spannable, pattern, str, (String[]) null, matchFilter, transformFilter);
    }

    public static final boolean addLinks(Spannable spannable, Pattern pattern, String str, String[] strArr, MatchFilter matchFilter, TransformFilter transformFilter) {
        boolean zAcceptMatch;
        if (spannable != null && containsUnsupportedCharacters(spannable.toString())) {
            EventLog.writeEvent(1397638484, "116321860", -1, "");
            return false;
        }
        if (str == null) {
            str = "";
        }
        if (strArr == null || strArr.length < 1) {
            strArr = EmptyArray.STRING;
        }
        String[] strArr2 = new String[strArr.length + 1];
        strArr2[0] = str.toLowerCase(Locale.ROOT);
        int i = 0;
        while (i < strArr.length) {
            String str2 = strArr[i];
            i++;
            strArr2[i] = str2 == null ? "" : str2.toLowerCase(Locale.ROOT);
        }
        Matcher matcher = pattern.matcher(spannable);
        boolean z = false;
        while (matcher.find()) {
            int iStart = matcher.start();
            int iEnd = matcher.end();
            if (matchFilter != null) {
                zAcceptMatch = matchFilter.acceptMatch(spannable, iStart, iEnd);
            } else {
                zAcceptMatch = true;
            }
            if (zAcceptMatch) {
                applyLink(makeUrl(matcher.group(0), strArr2, matcher, transformFilter), iStart, iEnd, spannable);
                z = true;
            }
        }
        return z;
    }

    public static Future<Void> addLinksAsync(TextView textView, TextLinksParams textLinksParams) {
        return addLinksAsync(textView, textLinksParams, null, null);
    }

    public static Future<Void> addLinksAsync(TextView textView, int i) {
        return addLinksAsync(textView, TextLinksParams.fromLinkMask(i), null, null);
    }

    public static Future<Void> addLinksAsync(final TextView textView, TextLinksParams textLinksParams, Executor executor, Consumer<Integer> consumer) {
        Preconditions.checkNotNull(textView);
        final CharSequence text = textView.getText();
        final Spannable spannableValueOf = text instanceof Spannable ? (Spannable) text : SpannableString.valueOf(text);
        return addLinksAsync(spannableValueOf, textView.getTextClassifier(), textLinksParams, executor, consumer, new Runnable() {
            @Override
            public final void run() {
                Linkify.lambda$addLinksAsync$0(textView, spannableValueOf, text);
            }
        });
    }

    static void lambda$addLinksAsync$0(TextView textView, Spannable spannable, CharSequence charSequence) {
        addLinkMovementMethod(textView);
        if (spannable != charSequence) {
            textView.setText(spannable);
        }
    }

    public static Future<Void> addLinksAsync(Spannable spannable, TextClassifier textClassifier, TextLinksParams textLinksParams) {
        return addLinksAsync(spannable, textClassifier, textLinksParams, null, null);
    }

    public static Future<Void> addLinksAsync(Spannable spannable, TextClassifier textClassifier, int i) {
        return addLinksAsync(spannable, textClassifier, TextLinksParams.fromLinkMask(i), null, null);
    }

    public static Future<Void> addLinksAsync(Spannable spannable, TextClassifier textClassifier, TextLinksParams textLinksParams, Executor executor, Consumer<Integer> consumer) {
        return addLinksAsync(spannable, textClassifier, textLinksParams, executor, consumer, null);
    }

    private static Future<Void> addLinksAsync(final Spannable spannable, final TextClassifier textClassifier, final TextLinksParams textLinksParams, Executor executor, final Consumer<Integer> consumer, final Runnable runnable) {
        Preconditions.checkNotNull(spannable);
        Preconditions.checkNotNull(textClassifier);
        final CharSequence charSequenceSubSequence = spannable.subSequence(0, Math.min(spannable.length(), textClassifier.getMaxGenerateLinksTextLength()));
        final TextLinks.Request requestBuild = new TextLinks.Request.Builder(charSequenceSubSequence).setLegacyFallback(true).setEntityConfig(textLinksParams == null ? null : textLinksParams.getEntityConfig()).build();
        Supplier supplier = new Supplier() {
            @Override
            public final Object get() {
                return textClassifier.generateLinks(requestBuild);
            }
        };
        Consumer consumer2 = new Consumer() {
            @Override
            public final void accept(Object obj) {
                Linkify.lambda$addLinksAsync$2(consumer, spannable, charSequenceSubSequence, textLinksParams, runnable, (TextLinks) obj);
            }
        };
        if (executor == null) {
            return CompletableFuture.supplyAsync(supplier).thenAccept(consumer2);
        }
        return CompletableFuture.supplyAsync(supplier, executor).thenAccept(consumer2);
    }

    static void lambda$addLinksAsync$2(Consumer consumer, Spannable spannable, CharSequence charSequence, TextLinksParams textLinksParams, Runnable runnable, TextLinks textLinks) {
        if (textLinks.getLinks().isEmpty()) {
            if (consumer != null) {
                consumer.accept(1);
                return;
            }
            return;
        }
        TextLinks.TextLinkSpan[] textLinkSpanArr = (TextLinks.TextLinkSpan[]) spannable.getSpans(0, charSequence.length(), TextLinks.TextLinkSpan.class);
        for (int length = textLinkSpanArr.length - 1; length >= 0; length--) {
            spannable.removeSpan(textLinkSpanArr[length]);
        }
        int iApply = textLinksParams.apply(spannable, textLinks);
        if (iApply == 0 && runnable != null) {
            runnable.run();
        }
        if (consumer != null) {
            consumer.accept(Integer.valueOf(iApply));
        }
    }

    private static final void applyLink(String str, int i, int i2, Spannable spannable) {
        spannable.setSpan(new URLSpan(str), i, i2, 33);
    }

    private static final String makeUrl(String str, String[] strArr, Matcher matcher, TransformFilter transformFilter) {
        boolean z;
        if (transformFilter != null) {
            str = transformFilter.transformUrl(matcher, str);
        }
        int i = 0;
        while (true) {
            z = true;
            if (i < strArr.length) {
                if (!str.regionMatches(true, 0, strArr[i], 0, strArr[i].length())) {
                    i++;
                } else {
                    if (!str.regionMatches(false, 0, strArr[i], 0, strArr[i].length())) {
                        str = strArr[i] + str.substring(strArr[i].length());
                    }
                }
            } else {
                z = false;
                break;
            }
        }
        if (!z && strArr.length > 0) {
            return strArr[0] + str;
        }
        return str;
    }

    private static final void gatherLinks(ArrayList<LinkSpec> arrayList, Spannable spannable, Pattern pattern, String[] strArr, MatchFilter matchFilter, TransformFilter transformFilter) {
        Matcher matcher = pattern.matcher(spannable);
        while (matcher.find()) {
            int iStart = matcher.start();
            int iEnd = matcher.end();
            Bundle extWebUrl = LinkifyExt.getExtWebUrl(matcher.group(0), iStart, iEnd, pattern);
            String strGroup = matcher.group(0);
            if (extWebUrl != null) {
                strGroup = extWebUrl.getString("value");
                iStart = extWebUrl.getInt(Telephony.BaseMmsColumns.START);
                iEnd = extWebUrl.getInt("end");
            }
            if (matchFilter == null || matchFilter.acceptMatch(spannable, iStart, iEnd)) {
                LinkSpec linkSpec = new LinkSpec();
                linkSpec.url = makeUrl(strGroup, strArr, matcher, transformFilter);
                linkSpec.start = iStart;
                linkSpec.end = iEnd;
                arrayList.add(linkSpec);
            }
        }
    }

    private static void gatherTelLinks(ArrayList<LinkSpec> arrayList, Spannable spannable, Context context) {
        TelephonyManager telephonyManagerFrom;
        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        if (context == null) {
            telephonyManagerFrom = TelephonyManager.getDefault();
        } else {
            telephonyManagerFrom = TelephonyManager.from(context);
        }
        for (PhoneNumberMatch phoneNumberMatch : phoneNumberUtil.findNumbers(spannable.toString(), telephonyManagerFrom.getSimCountryIso().toUpperCase(Locale.US), PhoneNumberUtil.Leniency.POSSIBLE, Long.MAX_VALUE)) {
            LinkSpec linkSpec = new LinkSpec();
            linkSpec.url = WebView.SCHEME_TEL + PhoneNumberUtils.normalizeNumber(phoneNumberMatch.rawString());
            linkSpec.start = phoneNumberMatch.start();
            linkSpec.end = phoneNumberMatch.end();
            arrayList.add(linkSpec);
        }
    }

    private static final void gatherMapLinks(ArrayList<LinkSpec> arrayList, Spannable spannable) {
        int iIndexOf;
        String string = spannable.toString();
        int i = 0;
        while (true) {
            try {
                String strFindAddress = WebView.findAddress(string);
                if (strFindAddress != null && (iIndexOf = string.indexOf(strFindAddress)) >= 0) {
                    LinkSpec linkSpec = new LinkSpec();
                    int length = strFindAddress.length() + iIndexOf;
                    linkSpec.start = iIndexOf + i;
                    i += length;
                    linkSpec.end = i;
                    string = string.substring(length);
                    try {
                        linkSpec.url = WebView.SCHEME_GEO + URLEncoder.encode(strFindAddress, "UTF-8");
                        arrayList.add(linkSpec);
                    } catch (UnsupportedEncodingException e) {
                    }
                }
                return;
            } catch (UnsupportedOperationException e2) {
                return;
            }
        }
    }

    private static final void pruneOverlaps(ArrayList<LinkSpec> arrayList) {
        Collections.sort(arrayList, new Comparator<LinkSpec>() {
            @Override
            public final int compare(LinkSpec linkSpec, LinkSpec linkSpec2) {
                if (linkSpec.start < linkSpec2.start) {
                    return -1;
                }
                if (linkSpec.start <= linkSpec2.start && linkSpec.end >= linkSpec2.end) {
                    return linkSpec.end > linkSpec2.end ? -1 : 0;
                }
                return 1;
            }
        });
        int size = arrayList.size();
        int i = 0;
        while (i < size - 1) {
            LinkSpec linkSpec = arrayList.get(i);
            int i2 = i + 1;
            LinkSpec linkSpec2 = arrayList.get(i2);
            if (linkSpec.start <= linkSpec2.start && linkSpec.end > linkSpec2.start) {
                int i3 = (linkSpec2.end > linkSpec.end && linkSpec.end - linkSpec.start <= linkSpec2.end - linkSpec2.start) ? linkSpec.end - linkSpec.start < linkSpec2.end - linkSpec2.start ? i : -1 : i2;
                if (i3 != -1) {
                    arrayList.remove(i3);
                    size--;
                }
            }
            i = i2;
        }
    }
}
