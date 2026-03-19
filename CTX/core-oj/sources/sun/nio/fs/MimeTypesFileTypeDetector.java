package sun.nio.fs;

import java.nio.file.Path;
import libcore.net.MimeUtils;

class MimeTypesFileTypeDetector extends AbstractFileTypeDetector {
    MimeTypesFileTypeDetector() {
    }

    @Override
    protected String implProbeContentType(Path path) {
        String strGuessMimeTypeFromExtension;
        Path fileName = path.getFileName();
        if (fileName == null) {
            return null;
        }
        String extension = getExtension(fileName.toString());
        if (extension.isEmpty()) {
            return null;
        }
        do {
            strGuessMimeTypeFromExtension = MimeUtils.guessMimeTypeFromExtension(extension);
            if (strGuessMimeTypeFromExtension == null) {
                extension = getExtension(extension);
            }
            if (strGuessMimeTypeFromExtension != null) {
                break;
            }
        } while (!extension.isEmpty());
        return strGuessMimeTypeFromExtension;
    }

    private static String getExtension(String str) {
        int iIndexOf;
        if (str == null || str.isEmpty() || (iIndexOf = str.indexOf(46)) < 0 || iIndexOf >= str.length() - 1) {
            return "";
        }
        return str.substring(iIndexOf + 1);
    }
}
