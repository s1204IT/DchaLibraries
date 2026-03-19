package android.media;

import android.app.backup.FullBackup;
import android.net.wifi.WifiEnterpriseConfig;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TtmlUtils {
    public static final String ATTR_BEGIN = "begin";
    public static final String ATTR_DURATION = "dur";
    public static final String ATTR_END = "end";
    public static final long INVALID_TIMESTAMP = Long.MAX_VALUE;
    public static final String PCDATA = "#pcdata";
    public static final String TAG_BODY = "body";
    public static final String TAG_BR = "br";
    public static final String TAG_DIV = "div";
    public static final String TAG_HEAD = "head";
    public static final String TAG_LAYOUT = "layout";
    public static final String TAG_METADATA = "metadata";
    public static final String TAG_P = "p";
    public static final String TAG_REGION = "region";
    public static final String TAG_SMPTE_DATA = "smpte:data";
    public static final String TAG_SMPTE_IMAGE = "smpte:image";
    public static final String TAG_SMPTE_INFORMATION = "smpte:information";
    public static final String TAG_SPAN = "span";
    public static final String TAG_STYLE = "style";
    public static final String TAG_STYLING = "styling";
    public static final String TAG_TT = "tt";
    private static final Pattern CLOCK_TIME = Pattern.compile("^([0-9][0-9]+):([0-9][0-9]):([0-9][0-9])(?:(\\.[0-9]+)|:([0-9][0-9])(?:\\.([0-9]+))?)?$");
    private static final Pattern OFFSET_TIME = Pattern.compile("^([0-9]+(?:\\.[0-9]+)?)(h|m|s|ms|f|t)$");

    private TtmlUtils() {
    }

    public static long parseTimeExpression(String str, int i, int i2, int i3) throws NumberFormatException {
        Matcher matcher = CLOCK_TIME.matcher(str);
        if (matcher.matches()) {
            double d = (Long.parseLong(matcher.group(1)) * 3600) + (Long.parseLong(matcher.group(2)) * 60) + Long.parseLong(matcher.group(3));
            String strGroup = matcher.group(4);
            double d2 = 0.0d;
            double d3 = d + (strGroup != null ? Double.parseDouble(strGroup) : 0.0d) + (matcher.group(5) != null ? Long.parseLong(r0) / ((double) i) : 0.0d);
            if (matcher.group(6) != null) {
                d2 = (Long.parseLong(r0) / ((double) i2)) / ((double) i);
            }
            return (long) ((d3 + d2) * 1000.0d);
        }
        Matcher matcher2 = OFFSET_TIME.matcher(str);
        if (matcher2.matches()) {
            double d4 = Double.parseDouble(matcher2.group(1));
            String strGroup2 = matcher2.group(2);
            if (strGroup2.equals("h")) {
                d4 *= 3.6E9d;
            } else if (strGroup2.equals("m")) {
                d4 *= 6.0E7d;
            } else if (strGroup2.equals("s")) {
                d4 *= 1000000.0d;
            } else if (strGroup2.equals("ms")) {
                d4 *= 1000.0d;
            } else if (strGroup2.equals(FullBackup.FILES_TREE_TOKEN)) {
                d4 = (d4 / ((double) i)) * 1000000.0d;
            } else if (strGroup2.equals("t")) {
                d4 = (d4 / ((double) i3)) * 1000000.0d;
            }
            return (long) d4;
        }
        throw new NumberFormatException("Malformed time expression : " + str);
    }

    public static String applyDefaultSpacePolicy(String str) {
        return applySpacePolicy(str, true);
    }

    public static String applySpacePolicy(String str, boolean z) {
        String strReplaceAll = str.replaceAll("\r\n", "\n").replaceAll(" *\n *", "\n");
        if (z) {
            strReplaceAll = strReplaceAll.replaceAll("\n", WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        }
        return strReplaceAll.replaceAll("[ \t\\x0B\f\r]+", WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
    }

    public static String extractText(TtmlNode ttmlNode, long j, long j2) {
        StringBuilder sb = new StringBuilder();
        extractText(ttmlNode, j, j2, sb, false);
        return sb.toString().replaceAll("\n$", "");
    }

    private static void extractText(TtmlNode ttmlNode, long j, long j2, StringBuilder sb, boolean z) {
        if (ttmlNode.mName.equals(PCDATA) && z) {
            sb.append(ttmlNode.mText);
            return;
        }
        if (ttmlNode.mName.equals(TAG_BR) && z) {
            sb.append("\n");
            return;
        }
        if (!ttmlNode.mName.equals(TAG_METADATA) && ttmlNode.isActive(j, j2)) {
            boolean zEquals = ttmlNode.mName.equals(TAG_P);
            int length = sb.length();
            for (int i = 0; i < ttmlNode.mChildren.size(); i++) {
                extractText(ttmlNode.mChildren.get(i), j, j2, sb, zEquals || z);
            }
            if (zEquals && length != sb.length()) {
                sb.append("\n");
            }
        }
    }

    public static String extractTtmlFragment(TtmlNode ttmlNode, long j, long j2) {
        StringBuilder sb = new StringBuilder();
        extractTtmlFragment(ttmlNode, j, j2, sb);
        return sb.toString();
    }

    private static void extractTtmlFragment(TtmlNode ttmlNode, long j, long j2, StringBuilder sb) {
        if (ttmlNode.mName.equals(PCDATA)) {
            sb.append(ttmlNode.mText);
            return;
        }
        if (ttmlNode.mName.equals(TAG_BR)) {
            sb.append("<br/>");
            return;
        }
        if (ttmlNode.isActive(j, j2)) {
            sb.append("<");
            sb.append(ttmlNode.mName);
            sb.append(ttmlNode.mAttributes);
            sb.append(">");
            for (int i = 0; i < ttmlNode.mChildren.size(); i++) {
                extractTtmlFragment(ttmlNode.mChildren.get(i), j, j2, sb);
            }
            sb.append("</");
            sb.append(ttmlNode.mName);
            sb.append(">");
        }
    }
}
