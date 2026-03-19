package org.apache.xml.serializer;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import javax.xml.transform.TransformerException;
import org.apache.xml.dtm.DTMManager;
import org.apache.xml.serializer.utils.SystemIDResolver;
import org.apache.xml.serializer.utils.Utils;
import org.apache.xml.serializer.utils.WrappedRuntimeException;

final class CharInfo {
    static final int ASCII_MAX = 128;
    private static final int LOW_ORDER_BITMASK = 31;
    private static final int SHIFT_PER_WORD = 5;
    static final char S_CARRIAGERETURN = '\r';
    static final char S_GT = '>';
    static final char S_HORIZONAL_TAB = '\t';
    static final char S_LINEFEED = '\n';
    static final char S_LINE_SEPARATOR = 8232;
    static final char S_LT = '<';
    static final char S_NEL = 133;
    static final char S_QUOTE = '\"';
    static final char S_SPACE = ' ';
    private final int[] array_of_bits;
    private int firstWordNotUsed;
    private final CharKey m_charKey;
    private HashMap m_charToString;
    boolean onlyQuotAmpLtGt;
    private final boolean[] shouldMapAttrChar_ASCII;
    private final boolean[] shouldMapTextChar_ASCII;
    public static final String HTML_ENTITIES_RESOURCE = SerializerBase.PKG_NAME + ".HTMLEntities";
    public static final String XML_ENTITIES_RESOURCE = SerializerBase.PKG_NAME + ".XMLEntities";
    private static Hashtable m_getCharInfoCache = new Hashtable();

    private CharInfo() {
        this.array_of_bits = createEmptySetOfIntegers(DTMManager.IDENT_NODE_DEFAULT);
        this.firstWordNotUsed = 0;
        this.shouldMapAttrChar_ASCII = new boolean[128];
        this.shouldMapTextChar_ASCII = new boolean[128];
        this.m_charKey = new CharKey();
        this.onlyQuotAmpLtGt = true;
    }

    private CharInfo(String str, String str2, boolean z) {
        ResourceBundle bundle;
        InputStream resourceAsStream;
        InputStream inputStream;
        InputStream inputStreamOpenStream;
        BufferedReader bufferedReader;
        this();
        this.m_charToString = new HashMap();
        InputStream inputStream2 = null;
        if (z) {
            try {
                bundle = PropertyResourceBundle.getBundle(str);
            } catch (Exception e) {
                bundle = null;
            }
        } else {
            bundle = null;
        }
        boolean z2 = true;
        if (bundle != null) {
            Enumeration<String> keys = bundle.getKeys();
            while (keys.hasMoreElements()) {
                String strNextElement = keys.nextElement();
                if (defineEntity(strNextElement, (char) Integer.parseInt(bundle.getString(strNextElement)))) {
                    z2 = false;
                }
            }
        } else {
            try {
                try {
                    if (z) {
                        inputStreamOpenStream = CharInfo.class.getResourceAsStream(str);
                    } else {
                        ClassLoader classLoaderFindClassLoader = ObjectFactory.findClassLoader();
                        if (classLoaderFindClassLoader == null) {
                            resourceAsStream = ClassLoader.getSystemResourceAsStream(str);
                        } else {
                            resourceAsStream = classLoaderFindClassLoader.getResourceAsStream(str);
                        }
                        inputStream = resourceAsStream;
                        if (inputStream == null) {
                            try {
                                inputStreamOpenStream = new URL(str).openStream();
                            } catch (Exception e2) {
                            }
                        }
                        if (inputStream != null) {
                            throw new RuntimeException(Utils.messages.createMessage("ER_RESOURCE_COULD_NOT_FIND", new Object[]{str, str}));
                        }
                        try {
                            bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                        } catch (UnsupportedEncodingException e3) {
                            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                        }
                        String line = bufferedReader.readLine();
                        boolean z3 = true;
                        while (line != null) {
                            if (line.length() == 0 || line.charAt(0) == '#') {
                                line = bufferedReader.readLine();
                            } else {
                                int iIndexOf = line.indexOf(32);
                                if (iIndexOf > 1) {
                                    String strSubstring = line.substring(0, iIndexOf);
                                    int i = iIndexOf + 1;
                                    if (i < line.length()) {
                                        String strSubstring2 = line.substring(i);
                                        int iIndexOf2 = strSubstring2.indexOf(32);
                                        if (defineEntity(strSubstring, (char) Integer.parseInt(iIndexOf2 > 0 ? strSubstring2.substring(0, iIndexOf2) : strSubstring2))) {
                                            z3 = false;
                                        }
                                    }
                                }
                                line = bufferedReader.readLine();
                            }
                        }
                        inputStream.close();
                        if (inputStream != null) {
                            try {
                                inputStream.close();
                            } catch (Exception e4) {
                            }
                        }
                        z2 = z3;
                    }
                    inputStream = inputStreamOpenStream;
                    if (inputStream != null) {
                    }
                } catch (Exception e5) {
                    throw new RuntimeException(Utils.messages.createMessage("ER_RESOURCE_COULD_NOT_LOAD", new Object[]{str, e5.toString(), str, e5.toString()}));
                }
            } catch (Throwable th) {
                if (0 != 0) {
                    try {
                        inputStream2.close();
                    } catch (Exception e6) {
                    }
                }
                throw th;
            }
        }
        this.onlyQuotAmpLtGt = z2;
        if ("xml".equals(str2)) {
            this.shouldMapTextChar_ASCII[34] = false;
        }
        if ("html".equals(str2)) {
            this.shouldMapAttrChar_ASCII[60] = false;
            this.shouldMapTextChar_ASCII[34] = false;
        }
    }

    private boolean defineEntity(String str, char c) {
        StringBuffer stringBuffer = new StringBuffer("&");
        stringBuffer.append(str);
        stringBuffer.append(';');
        return defineChar2StringMapping(stringBuffer.toString(), c);
    }

    String getOutputStringForChar(char c) {
        this.m_charKey.setChar(c);
        return (String) this.m_charToString.get(this.m_charKey);
    }

    final boolean shouldMapAttrChar(int i) {
        if (i < 128) {
            return this.shouldMapAttrChar_ASCII[i];
        }
        return get(i);
    }

    final boolean shouldMapTextChar(int i) {
        if (i < 128) {
            return this.shouldMapTextChar_ASCII[i];
        }
        return get(i);
    }

    private static CharInfo getCharInfoBasedOnPrivilege(final String str, final String str2, final boolean z) {
        return (CharInfo) AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                return new CharInfo(str, str2, z);
            }
        });
    }

    static CharInfo getCharInfo(String str, String str2) {
        CharInfo charInfo = (CharInfo) m_getCharInfoCache.get(str);
        if (charInfo != null) {
            return mutableCopyOf(charInfo);
        }
        try {
            CharInfo charInfoBasedOnPrivilege = getCharInfoBasedOnPrivilege(str, str2, true);
            m_getCharInfoCache.put(str, charInfoBasedOnPrivilege);
            return mutableCopyOf(charInfoBasedOnPrivilege);
        } catch (Exception e) {
            try {
                return getCharInfoBasedOnPrivilege(str, str2, false);
            } catch (Exception e2) {
                if (str.indexOf(58) < 0) {
                    SystemIDResolver.getAbsoluteURIFromRelative(str);
                } else {
                    try {
                        SystemIDResolver.getAbsoluteURI(str, null);
                    } catch (TransformerException e3) {
                        throw new WrappedRuntimeException(e3);
                    }
                }
                return getCharInfoBasedOnPrivilege(str, str2, false);
            }
        }
    }

    private static CharInfo mutableCopyOf(CharInfo charInfo) {
        CharInfo charInfo2 = new CharInfo();
        System.arraycopy(charInfo.array_of_bits, 0, charInfo2.array_of_bits, 0, charInfo.array_of_bits.length);
        charInfo2.firstWordNotUsed = charInfo.firstWordNotUsed;
        System.arraycopy(charInfo.shouldMapAttrChar_ASCII, 0, charInfo2.shouldMapAttrChar_ASCII, 0, charInfo.shouldMapAttrChar_ASCII.length);
        System.arraycopy(charInfo.shouldMapTextChar_ASCII, 0, charInfo2.shouldMapTextChar_ASCII, 0, charInfo.shouldMapTextChar_ASCII.length);
        charInfo2.m_charToString = (HashMap) charInfo.m_charToString.clone();
        charInfo2.onlyQuotAmpLtGt = charInfo.onlyQuotAmpLtGt;
        return charInfo2;
    }

    private static int arrayIndex(int i) {
        return i >> 5;
    }

    private static int bit(int i) {
        return 1 << (i & 31);
    }

    private int[] createEmptySetOfIntegers(int i) {
        this.firstWordNotUsed = 0;
        return new int[arrayIndex(i - 1) + 1];
    }

    private final void set(int i) {
        setASCIItextDirty(i);
        setASCIIattrDirty(i);
        int i2 = i >> 5;
        int i3 = i2 + 1;
        if (this.firstWordNotUsed < i3) {
            this.firstWordNotUsed = i3;
        }
        int[] iArr = this.array_of_bits;
        iArr[i2] = (1 << (i & 31)) | iArr[i2];
    }

    private final boolean get(int i) {
        int i2 = i >> 5;
        if (i2 < this.firstWordNotUsed) {
            return ((1 << (i & 31)) & this.array_of_bits[i2]) != 0;
        }
        return false;
    }

    private boolean extraEntity(String str, int i) {
        if (i < 128) {
            if (i != 34) {
                if (i != 38) {
                    if (i == 60) {
                        if (!str.equals(SerializerConstants.ENTITY_LT)) {
                            return true;
                        }
                    } else if (i != 62 || !str.equals(SerializerConstants.ENTITY_GT)) {
                        return true;
                    }
                } else if (!str.equals(SerializerConstants.ENTITY_AMP)) {
                    return true;
                }
            } else if (!str.equals(SerializerConstants.ENTITY_QUOT)) {
                return true;
            }
        }
        return false;
    }

    private void setASCIItextDirty(int i) {
        if (i >= 0 && i < 128) {
            this.shouldMapTextChar_ASCII[i] = true;
        }
    }

    private void setASCIIattrDirty(int i) {
        if (i >= 0 && i < 128) {
            this.shouldMapAttrChar_ASCII[i] = true;
        }
    }

    boolean defineChar2StringMapping(String str, char c) {
        this.m_charToString.put(new CharKey(c), str);
        set(c);
        return extraEntity(str, c);
    }

    private static class CharKey {
        private char m_char;

        public CharKey(char c) {
            this.m_char = c;
        }

        public CharKey() {
        }

        public final void setChar(char c) {
            this.m_char = c;
        }

        public final int hashCode() {
            return this.m_char;
        }

        public final boolean equals(Object obj) {
            return ((CharKey) obj).m_char == this.m_char;
        }
    }
}
