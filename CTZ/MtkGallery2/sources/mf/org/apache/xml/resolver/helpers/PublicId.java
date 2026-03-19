package mf.org.apache.xml.resolver.helpers;

public abstract class PublicId {
    public static String normalize(String publicId) {
        String normal = publicId.replace('\t', ' ');
        String normal2 = normal.replace('\r', ' ').replace('\n', ' ').trim();
        while (true) {
            int pos = normal2.indexOf("  ");
            if (pos >= 0) {
                normal2 = String.valueOf(normal2.substring(0, pos)) + normal2.substring(pos + 1);
            } else {
                return normal2;
            }
        }
    }

    public static String decodeURN(String urn) {
        if (urn.startsWith("urn:publicid:")) {
            String publicId = urn.substring(13);
            return stringReplace(stringReplace(stringReplace(stringReplace(stringReplace(stringReplace(stringReplace(stringReplace(stringReplace(stringReplace(stringReplace(publicId, "%2F", "/"), ":", "//"), "%3A", ":"), ";", "::"), "+", " "), "%2B", "+"), "%23", "#"), "%3F", "?"), "%27", "'"), "%3B", ";"), "%25", "%");
        }
        return urn;
    }

    private static String stringReplace(String str, String oldStr, String newStr) {
        String result = "";
        int pos = str.indexOf(oldStr);
        while (pos >= 0) {
            result = String.valueOf(String.valueOf(result) + str.substring(0, pos)) + newStr;
            str = str.substring(pos + 1);
            pos = str.indexOf(oldStr);
        }
        return String.valueOf(result) + str;
    }
}
