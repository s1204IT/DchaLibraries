package android.text.util;

import android.net.ProxyInfo;
import android.telephony.PhoneNumberUtils;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.URLSpan;
import android.util.Patterns;
import android.webkit.WebView;
import android.widget.TextView;
import com.android.i18n.phonenumbers.PhoneNumberMatch;
import com.android.i18n.phonenumbers.PhoneNumberUtil;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Linkify {
    public static final int ALL = 15;
    public static final int EMAIL_ADDRESSES = 2;
    public static final int MAP_ADDRESSES = 8;
    public static final int PHONE_NUMBERS = 4;
    private static final int PHONE_NUMBER_MINIMUM_DIGITS = 5;
    public static final int WEB_URLS = 1;
    public static final MatchFilter sUrlMatchFilter = new MatchFilter() {
        @Override
        public final boolean acceptMatch(CharSequence s, int start, int end) {
            return start == 0 || s.charAt(start + (-1)) != '@';
        }
    };
    public static final MatchFilter sPhoneNumberMatchFilter = new MatchFilter() {
        @Override
        public final boolean acceptMatch(CharSequence s, int start, int end) {
            int digitCount = 0;
            for (int i = start; i < end; i++) {
                if (Character.isDigit(s.charAt(i)) && (digitCount = digitCount + 1) >= 5) {
                    return true;
                }
            }
            return false;
        }
    };
    public static final TransformFilter sPhoneNumberTransformFilter = new TransformFilter() {
        @Override
        public final String transformUrl(Matcher match, String url) {
            return Patterns.digitsAndPlusOnly(match);
        }
    };

    public interface MatchFilter {
        boolean acceptMatch(CharSequence charSequence, int i, int i2);
    }

    public interface TransformFilter {
        String transformUrl(Matcher matcher, String str);
    }

    public static final boolean addLinks(Spannable text, int mask) {
        if (mask == 0) {
            return false;
        }
        URLSpan[] old = (URLSpan[]) text.getSpans(0, text.length(), URLSpan.class);
        for (int i = old.length - 1; i >= 0; i--) {
            text.removeSpan(old[i]);
        }
        ArrayList<LinkSpec> links = new ArrayList<>();
        if ((mask & 1) != 0) {
            gatherLinks(links, text, Patterns.WEB_URL, new String[]{"http://", "https://", "rtsp://"}, sUrlMatchFilter, null);
        }
        if ((mask & 2) != 0) {
            gatherLinks(links, text, Patterns.EMAIL_ADDRESS, new String[]{"mailto:"}, null, null);
        }
        if ((mask & 4) != 0) {
            gatherTelLinks(links, text);
        }
        if ((mask & 8) != 0) {
            gatherMapLinks(links, text);
        }
        pruneOverlaps(links);
        if (links.size() == 0) {
            return false;
        }
        for (LinkSpec link : links) {
            applyLink(link.url, link.start, link.end, text);
        }
        return true;
    }

    public static final boolean addLinks(TextView text, int mask) {
        if (mask == 0) {
            return false;
        }
        CharSequence t = text.getText();
        if (t instanceof Spannable) {
            if (!addLinks((Spannable) t, mask)) {
                return false;
            }
            addLinkMovementMethod(text);
            return true;
        }
        SpannableString s = SpannableString.valueOf(t);
        if (!addLinks(s, mask)) {
            return false;
        }
        addLinkMovementMethod(text);
        text.setText(s);
        return true;
    }

    private static final void addLinkMovementMethod(TextView t) {
        MovementMethod m = t.getMovementMethod();
        if ((m == null || !(m instanceof LinkMovementMethod)) && t.getLinksClickable()) {
            t.setMovementMethod(LinkMovementMethod.getInstance());
        }
    }

    public static final void addLinks(TextView text, Pattern pattern, String scheme) {
        addLinks(text, pattern, scheme, (MatchFilter) null, (TransformFilter) null);
    }

    public static final void addLinks(TextView text, Pattern p, String scheme, MatchFilter matchFilter, TransformFilter transformFilter) {
        SpannableString s = SpannableString.valueOf(text.getText());
        if (addLinks(s, p, scheme, matchFilter, transformFilter)) {
            text.setText(s);
            addLinkMovementMethod(text);
        }
    }

    public static final boolean addLinks(Spannable text, Pattern pattern, String scheme) {
        return addLinks(text, pattern, scheme, (MatchFilter) null, (TransformFilter) null);
    }

    public static final boolean addLinks(Spannable s, Pattern p, String scheme, MatchFilter matchFilter, TransformFilter transformFilter) {
        boolean hasMatches = false;
        String prefix = scheme == null ? ProxyInfo.LOCAL_EXCL_LIST : scheme.toLowerCase(Locale.ROOT);
        Matcher m = p.matcher(s);
        while (m.find()) {
            int start = m.start();
            int end = m.end();
            boolean allowed = true;
            if (matchFilter != null) {
                allowed = matchFilter.acceptMatch(s, start, end);
            }
            if (allowed) {
                String url = makeUrl(m.group(0), new String[]{prefix}, m, transformFilter);
                applyLink(url, start, end, s);
                hasMatches = true;
            }
        }
        return hasMatches;
    }

    private static final void applyLink(String url, int start, int end, Spannable text) {
        URLSpan span = new URLSpan(url);
        text.setSpan(span, start, end, 33);
    }

    private static final String makeUrl(String url, String[] prefixes, Matcher m, TransformFilter filter) {
        if (filter != null) {
            url = filter.transformUrl(m, url);
        }
        boolean hasPrefix = false;
        int i = 0;
        while (true) {
            if (i >= prefixes.length) {
                break;
            }
            if (!url.regionMatches(true, 0, prefixes[i], 0, prefixes[i].length())) {
                i++;
            } else {
                hasPrefix = true;
                if (!url.regionMatches(false, 0, prefixes[i], 0, prefixes[i].length())) {
                    url = prefixes[i] + url.substring(prefixes[i].length());
                }
            }
        }
        if (!hasPrefix) {
            return prefixes[0] + url;
        }
        return url;
    }

    private static final void gatherLinks(ArrayList<LinkSpec> links, Spannable s, Pattern pattern, String[] schemes, MatchFilter matchFilter, TransformFilter transformFilter) {
        Matcher m = pattern.matcher(s);
        while (m.find()) {
            int start = m.start();
            int end = m.end();
            if (matchFilter == null || matchFilter.acceptMatch(s, start, end)) {
                LinkSpec spec = new LinkSpec();
                String url = makeUrl(m.group(0), schemes, m, transformFilter);
                spec.url = url;
                spec.start = start;
                spec.end = end;
                links.add(spec);
            }
        }
    }

    private static final void gatherTelLinks(ArrayList<LinkSpec> links, Spannable s) {
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        Iterable<PhoneNumberMatch> matches = phoneUtil.findNumbers(s.toString(), Locale.getDefault().getCountry(), PhoneNumberUtil.Leniency.POSSIBLE, Long.MAX_VALUE);
        for (PhoneNumberMatch match : matches) {
            LinkSpec spec = new LinkSpec();
            spec.url = WebView.SCHEME_TEL + PhoneNumberUtils.normalizeNumber(match.rawString());
            spec.start = match.start();
            spec.end = match.end();
            links.add(spec);
        }
    }

    private static final void gatherMapLinks(ArrayList<LinkSpec> links, Spannable s) {
        int start;
        String string = s.toString();
        int base = 0;
        while (true) {
            try {
                String address = WebView.findAddress(string);
                if (address != null && (start = string.indexOf(address)) >= 0) {
                    LinkSpec spec = new LinkSpec();
                    int length = address.length();
                    int end = start + length;
                    spec.start = base + start;
                    spec.end = base + end;
                    string = string.substring(end);
                    base += end;
                    try {
                        String encodedAddress = URLEncoder.encode(address, "UTF-8");
                        spec.url = WebView.SCHEME_GEO + encodedAddress;
                        links.add(spec);
                    } catch (UnsupportedEncodingException e) {
                    }
                } else {
                    return;
                }
            } catch (UnsupportedOperationException e2) {
                return;
            }
        }
    }

    private static final void pruneOverlaps(ArrayList<LinkSpec> links) {
        Comparator<LinkSpec> c = new Comparator<LinkSpec>() {
            @Override
            public final int compare(LinkSpec a, LinkSpec b) {
                if (a.start < b.start) {
                    return -1;
                }
                if (a.start <= b.start && a.end >= b.end) {
                    return a.end <= b.end ? 0 : -1;
                }
                return 1;
            }

            @Override
            public final boolean equals(Object o) {
                return false;
            }
        };
        Collections.sort(links, c);
        int len = links.size();
        int i = 0;
        while (i < len - 1) {
            LinkSpec a = links.get(i);
            LinkSpec b = links.get(i + 1);
            int remove = -1;
            if (a.start <= b.start && a.end > b.start) {
                if (b.end <= a.end || a.end - a.start > b.end - b.start) {
                    remove = i + 1;
                } else if (a.end - a.start < b.end - b.start) {
                    remove = i;
                }
                if (remove != -1) {
                    links.remove(remove);
                    len--;
                }
            }
            i++;
        }
    }
}
