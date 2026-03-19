package java.net;

import libcore.net.MimeUtils;

class DefaultFileNameMap implements FileNameMap {
    DefaultFileNameMap() {
    }

    @Override
    public String getContentTypeFor(String str) {
        if (str.endsWith("/")) {
            return MimeUtils.guessMimeTypeFromExtension("html");
        }
        int iLastIndexOf = str.lastIndexOf(35);
        if (iLastIndexOf < 0) {
            iLastIndexOf = str.length();
        }
        int iLastIndexOf2 = str.lastIndexOf(46) + 1;
        String strSubstring = "";
        if (iLastIndexOf2 > str.lastIndexOf(47)) {
            strSubstring = str.substring(iLastIndexOf2, iLastIndexOf);
        }
        return MimeUtils.guessMimeTypeFromExtension(strSubstring);
    }
}
