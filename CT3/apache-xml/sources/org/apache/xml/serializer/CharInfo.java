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

    CharInfo(String entitiesResource, String method, boolean internal, CharInfo charInfo) {
        this(entitiesResource, method, internal);
    }

    private CharInfo() {
        this.array_of_bits = createEmptySetOfIntegers(DTMManager.IDENT_NODE_DEFAULT);
        this.firstWordNotUsed = 0;
        this.shouldMapAttrChar_ASCII = new boolean[128];
        this.shouldMapTextChar_ASCII = new boolean[128];
        this.m_charKey = new CharKey();
        this.onlyQuotAmpLtGt = true;
    }

    private CharInfo(String entitiesResource, String method, boolean internal) {
        InputStream is;
        BufferedReader reader;
        this();
        this.m_charToString = new HashMap();
        ResourceBundle entities = null;
        boolean noExtraEntities = true;
        if (internal) {
            try {
                entities = PropertyResourceBundle.getBundle(entitiesResource);
            } catch (Exception e) {
            }
        }
        if (entities != null) {
            Enumeration<String> keys = entities.getKeys();
            while (keys.hasMoreElements()) {
                String name = keys.nextElement();
                int code = Integer.parseInt(entities.getString(name));
                boolean extra = defineEntity(name, (char) code);
                if (extra) {
                    noExtraEntities = false;
                }
            }
        } else {
            InputStream inputStream = null;
            try {
                try {
                    if (internal) {
                        is = CharInfo.class.getResourceAsStream(entitiesResource);
                    } else {
                        ClassLoader cl = ObjectFactory.findClassLoader();
                        if (cl == null) {
                            is = ClassLoader.getSystemResourceAsStream(entitiesResource);
                        } else {
                            is = cl.getResourceAsStream(entitiesResource);
                        }
                        if (is == null) {
                            try {
                                URL url = new URL(entitiesResource);
                                is = url.openStream();
                            } catch (Exception e2) {
                            }
                        }
                    }
                    if (is == null) {
                        throw new RuntimeException(Utils.messages.createMessage("ER_RESOURCE_COULD_NOT_FIND", new Object[]{entitiesResource, entitiesResource}));
                    }
                    try {
                        reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    } catch (UnsupportedEncodingException e3) {
                        reader = new BufferedReader(new InputStreamReader(is));
                    }
                    String line = reader.readLine();
                    while (line != null) {
                        if (line.length() == 0 || line.charAt(0) == '#') {
                            line = reader.readLine();
                        } else {
                            int index = line.indexOf(32);
                            if (index > 1) {
                                String name2 = line.substring(0, index);
                                int index2 = index + 1;
                                if (index2 < line.length()) {
                                    String value = line.substring(index2);
                                    int index3 = value.indexOf(32);
                                    int code2 = Integer.parseInt(index3 > 0 ? value.substring(0, index3) : value);
                                    boolean extra2 = defineEntity(name2, (char) code2);
                                    if (extra2) {
                                        noExtraEntities = false;
                                    }
                                }
                            }
                            line = reader.readLine();
                        }
                    }
                    is.close();
                    if (is != null) {
                        try {
                            is.close();
                        } catch (Exception e4) {
                        }
                    }
                } catch (Exception e5) {
                    throw new RuntimeException(Utils.messages.createMessage("ER_RESOURCE_COULD_NOT_LOAD", new Object[]{entitiesResource, e5.toString(), entitiesResource, e5.toString()}));
                }
            } catch (Throwable th) {
                if (0 != 0) {
                    try {
                        inputStream.close();
                    } catch (Exception e6) {
                    }
                }
                throw th;
            }
        }
        this.onlyQuotAmpLtGt = noExtraEntities;
        if ("xml".equals(method)) {
            this.shouldMapTextChar_ASCII[34] = false;
        }
        if (!"html".equals(method)) {
            return;
        }
        this.shouldMapAttrChar_ASCII[60] = false;
        this.shouldMapTextChar_ASCII[34] = false;
    }

    private boolean defineEntity(String name, char value) {
        StringBuffer sb = new StringBuffer("&");
        sb.append(name);
        sb.append(';');
        String entityString = sb.toString();
        boolean extra = defineChar2StringMapping(entityString, value);
        return extra;
    }

    String getOutputStringForChar(char value) {
        this.m_charKey.setChar(value);
        return (String) this.m_charToString.get(this.m_charKey);
    }

    final boolean shouldMapAttrChar(int value) {
        if (value < 128) {
            return this.shouldMapAttrChar_ASCII[value];
        }
        return get(value);
    }

    final boolean shouldMapTextChar(int value) {
        if (value < 128) {
            return this.shouldMapTextChar_ASCII[value];
        }
        return get(value);
    }

    private static CharInfo getCharInfoBasedOnPrivilege(final String entitiesFileName, final String method, final boolean internal) {
        return (CharInfo) AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                return new CharInfo(entitiesFileName, method, internal, null);
            }
        });
    }

    static CharInfo getCharInfo(String entitiesFileName, String method) {
        CharInfo charInfo = (CharInfo) m_getCharInfoCache.get(entitiesFileName);
        if (charInfo != null) {
            return mutableCopyOf(charInfo);
        }
        try {
            CharInfo charInfo2 = getCharInfoBasedOnPrivilege(entitiesFileName, method, true);
            m_getCharInfoCache.put(entitiesFileName, charInfo2);
            return mutableCopyOf(charInfo2);
        } catch (Exception e) {
            try {
                return getCharInfoBasedOnPrivilege(entitiesFileName, method, false);
            } catch (Exception e2) {
                if (entitiesFileName.indexOf(58) < 0) {
                    SystemIDResolver.getAbsoluteURIFromRelative(entitiesFileName);
                } else {
                    try {
                        SystemIDResolver.getAbsoluteURI(entitiesFileName, null);
                    } catch (TransformerException te) {
                        throw new WrappedRuntimeException(te);
                    }
                }
                return getCharInfoBasedOnPrivilege(entitiesFileName, method, false);
            }
        }
    }

    private static CharInfo mutableCopyOf(CharInfo charInfo) {
        CharInfo copy = new CharInfo();
        int max = charInfo.array_of_bits.length;
        System.arraycopy(charInfo.array_of_bits, 0, copy.array_of_bits, 0, max);
        copy.firstWordNotUsed = charInfo.firstWordNotUsed;
        int max2 = charInfo.shouldMapAttrChar_ASCII.length;
        System.arraycopy(charInfo.shouldMapAttrChar_ASCII, 0, copy.shouldMapAttrChar_ASCII, 0, max2);
        int max3 = charInfo.shouldMapTextChar_ASCII.length;
        System.arraycopy(charInfo.shouldMapTextChar_ASCII, 0, copy.shouldMapTextChar_ASCII, 0, max3);
        copy.m_charToString = (HashMap) charInfo.m_charToString.clone();
        copy.onlyQuotAmpLtGt = charInfo.onlyQuotAmpLtGt;
        return copy;
    }

    private static int arrayIndex(int i) {
        return i >> 5;
    }

    private static int bit(int i) {
        int ret = 1 << (i & 31);
        return ret;
    }

    private int[] createEmptySetOfIntegers(int max) {
        this.firstWordNotUsed = 0;
        int[] arr = new int[arrayIndex(max - 1) + 1];
        return arr;
    }

    private final void set(int i) {
        setASCIItextDirty(i);
        setASCIIattrDirty(i);
        int j = i >> 5;
        int k = j + 1;
        if (this.firstWordNotUsed < k) {
            this.firstWordNotUsed = k;
        }
        int[] iArr = this.array_of_bits;
        iArr[j] = iArr[j] | (1 << (i & 31));
    }

    private final boolean get(int i) {
        int j = i >> 5;
        return j < this.firstWordNotUsed && (this.array_of_bits[j] & (1 << (i & 31))) != 0;
    }

    private boolean extraEntity(String outputString, int charToMap) {
        if (charToMap >= 128) {
            return false;
        }
        switch (charToMap) {
            case 34:
                if (!outputString.equals(SerializerConstants.ENTITY_QUOT)) {
                }
                break;
            case 38:
                if (!outputString.equals(SerializerConstants.ENTITY_AMP)) {
                }
                break;
            case 60:
                if (!outputString.equals(SerializerConstants.ENTITY_LT)) {
                }
                break;
            case 62:
                if (!outputString.equals(SerializerConstants.ENTITY_GT)) {
                }
                break;
        }
        return false;
    }

    private void setASCIItextDirty(int j) {
        if (j < 0 || j >= 128) {
            return;
        }
        this.shouldMapTextChar_ASCII[j] = true;
    }

    private void setASCIIattrDirty(int j) {
        if (j < 0 || j >= 128) {
            return;
        }
        this.shouldMapAttrChar_ASCII[j] = true;
    }

    boolean defineChar2StringMapping(String outputString, char inputChar) {
        CharKey character = new CharKey(inputChar);
        this.m_charToString.put(character, outputString);
        set(inputChar);
        boolean extraMapping = extraEntity(outputString, inputChar);
        return extraMapping;
    }

    private static class CharKey {
        private char m_char;

        public CharKey(char key) {
            this.m_char = key;
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
