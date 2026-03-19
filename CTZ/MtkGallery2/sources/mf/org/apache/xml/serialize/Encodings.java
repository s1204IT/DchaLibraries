package mf.org.apache.xml.serialize;

import java.io.UnsupportedEncodingException;
import java.util.Hashtable;
import java.util.Locale;
import mf.org.apache.xerces.util.EncodingMap;

public class Encodings {
    static final String[] UNICODE_ENCODINGS = {"Unicode", "UnicodeBig", "UnicodeLittle", "GB2312", "UTF8", "UTF-16"};
    static Hashtable _encodings = new Hashtable();

    static EncodingInfo getEncodingInfo(String encoding, boolean allowJavaNames) throws UnsupportedEncodingException {
        if (encoding == null) {
            EncodingInfo eInfo = (EncodingInfo) _encodings.get("UTF8");
            if (eInfo != null) {
                return eInfo;
            }
            EncodingInfo eInfo2 = new EncodingInfo(EncodingMap.getJava2IANAMapping("UTF8"), "UTF8", 65535);
            _encodings.put("UTF8", eInfo2);
            return eInfo2;
        }
        String encoding2 = encoding.toUpperCase(Locale.ENGLISH);
        String jName = EncodingMap.getIANA2JavaMapping(encoding2);
        if (jName == null) {
            if (allowJavaNames) {
                EncodingInfo.testJavaEncodingName(encoding2);
                EncodingInfo encodingInfo = (EncodingInfo) _encodings.get(encoding2);
                EncodingInfo eInfo3 = encodingInfo;
                if (encodingInfo != null) {
                    return eInfo3;
                }
                int i = 0;
                while (true) {
                    if (i >= UNICODE_ENCODINGS.length) {
                        break;
                    }
                    if (!UNICODE_ENCODINGS[i].equalsIgnoreCase(encoding2)) {
                        i++;
                    } else {
                        eInfo3 = new EncodingInfo(EncodingMap.getJava2IANAMapping(encoding2), encoding2, 65535);
                        break;
                    }
                }
                if (i == UNICODE_ENCODINGS.length) {
                    eInfo3 = new EncodingInfo(EncodingMap.getJava2IANAMapping(encoding2), encoding2, 127);
                }
                _encodings.put(encoding2, eInfo3);
                return eInfo3;
            }
            throw new UnsupportedEncodingException(encoding2);
        }
        EncodingInfo encodingInfo2 = (EncodingInfo) _encodings.get(jName);
        EncodingInfo eInfo4 = encodingInfo2;
        if (encodingInfo2 != null) {
            return eInfo4;
        }
        int i2 = 0;
        while (true) {
            if (i2 >= UNICODE_ENCODINGS.length) {
                break;
            }
            if (!UNICODE_ENCODINGS[i2].equalsIgnoreCase(jName)) {
                i2++;
            } else {
                eInfo4 = new EncodingInfo(encoding2, jName, 65535);
                break;
            }
        }
        if (i2 == UNICODE_ENCODINGS.length) {
            eInfo4 = new EncodingInfo(encoding2, jName, 127);
        }
        _encodings.put(jName, eInfo4);
        return eInfo4;
    }
}
