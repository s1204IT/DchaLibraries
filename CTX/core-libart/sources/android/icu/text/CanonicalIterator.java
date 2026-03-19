package android.icu.text;

import android.icu.impl.Norm2AllModes;
import android.icu.impl.Normalizer2Impl;
import android.icu.impl.Utility;
import android.icu.lang.UCharacter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public final class CanonicalIterator {
    private transient StringBuilder buffer = new StringBuilder();
    private int[] current;
    private boolean done;
    private final Normalizer2Impl nfcImpl;
    private final Normalizer2 nfd;
    private String[][] pieces;
    private String source;
    private static boolean PROGRESS = false;
    private static boolean SKIP_ZEROS = true;
    private static final Set<String> SET_WITH_NULL_STRING = new HashSet();

    public CanonicalIterator(String str) {
        Norm2AllModes nFCInstance = Norm2AllModes.getNFCInstance();
        this.nfd = nFCInstance.decomp;
        this.nfcImpl = nFCInstance.impl.ensureCanonIterData();
        setSource(str);
    }

    public String getSource() {
        return this.source;
    }

    public void reset() {
        this.done = false;
        for (int i = 0; i < this.current.length; i++) {
            this.current[i] = 0;
        }
    }

    public String next() {
        if (this.done) {
            return null;
        }
        this.buffer.setLength(0);
        for (int i = 0; i < this.pieces.length; i++) {
            this.buffer.append(this.pieces[i][this.current[i]]);
        }
        String string = this.buffer.toString();
        int length = this.current.length - 1;
        while (true) {
            if (length < 0) {
                this.done = true;
                break;
            }
            int[] iArr = this.current;
            iArr[length] = iArr[length] + 1;
            if (this.current[length] < this.pieces[length].length) {
                break;
            }
            this.current[length] = 0;
            length--;
        }
        return string;
    }

    public void setSource(String str) {
        this.source = this.nfd.normalize(str);
        this.done = false;
        if (str.length() == 0) {
            this.pieces = new String[1][];
            this.current = new int[1];
            this.pieces[0] = new String[]{""};
            return;
        }
        ArrayList arrayList = new ArrayList();
        int iFindOffsetFromCodePoint = UTF16.findOffsetFromCodePoint(this.source, 1);
        int i = 0;
        while (iFindOffsetFromCodePoint < this.source.length()) {
            int iCodePointAt = this.source.codePointAt(iFindOffsetFromCodePoint);
            if (this.nfcImpl.isCanonSegmentStarter(iCodePointAt)) {
                arrayList.add(this.source.substring(i, iFindOffsetFromCodePoint));
                i = iFindOffsetFromCodePoint;
            }
            iFindOffsetFromCodePoint += Character.charCount(iCodePointAt);
        }
        arrayList.add(this.source.substring(i, iFindOffsetFromCodePoint));
        this.pieces = new String[arrayList.size()][];
        this.current = new int[arrayList.size()];
        for (int i2 = 0; i2 < this.pieces.length; i2++) {
            if (PROGRESS) {
                System.out.println("SEGMENT");
            }
            this.pieces[i2] = getEquivalents((String) arrayList.get(i2));
        }
    }

    @Deprecated
    public static void permute(String str, boolean z, Set<String> set) {
        if (str.length() <= 2 && UTF16.countCodePoint(str) <= 1) {
            set.add(str);
            return;
        }
        HashSet hashSet = new HashSet();
        int charCount = 0;
        while (charCount < str.length()) {
            int iCharAt = UTF16.charAt(str, charCount);
            if (!z || charCount == 0 || UCharacter.getCombiningClass(iCharAt) != 0) {
                hashSet.clear();
                permute(str.substring(0, charCount) + str.substring(UTF16.getCharCount(iCharAt) + charCount), z, hashSet);
                String strValueOf = UTF16.valueOf(str, charCount);
                Iterator it = hashSet.iterator();
                while (it.hasNext()) {
                    set.add(strValueOf + ((String) it.next()));
                }
            }
            charCount += UTF16.getCharCount(iCharAt);
        }
    }

    static {
        SET_WITH_NULL_STRING.add("");
    }

    private String[] getEquivalents(String str) {
        HashSet hashSet = new HashSet();
        Set<String> equivalents2 = getEquivalents2(str);
        HashSet<String> hashSet2 = new HashSet();
        for (String str2 : equivalents2) {
            hashSet2.clear();
            permute(str2, SKIP_ZEROS, hashSet2);
            for (String str3 : hashSet2) {
                if (Normalizer.compare(str3, str, 0) == 0) {
                    if (PROGRESS) {
                        System.out.println("Adding Permutation: " + Utility.hex(str3));
                    }
                    hashSet.add(str3);
                } else if (PROGRESS) {
                    System.out.println("-Skipping Permutation: " + Utility.hex(str3));
                }
            }
        }
        String[] strArr = new String[hashSet.size()];
        hashSet.toArray(strArr);
        return strArr;
    }

    private Set<String> getEquivalents2(String str) {
        HashSet hashSet = new HashSet();
        if (PROGRESS) {
            System.out.println("Adding: " + Utility.hex(str));
        }
        hashSet.add(str);
        StringBuffer stringBuffer = new StringBuffer();
        UnicodeSet unicodeSet = new UnicodeSet();
        int iCharCount = 0;
        while (iCharCount < str.length()) {
            int iCodePointAt = str.codePointAt(iCharCount);
            if (this.nfcImpl.getCanonStartSet(iCodePointAt, unicodeSet)) {
                UnicodeSetIterator unicodeSetIterator = new UnicodeSetIterator(unicodeSet);
                while (unicodeSetIterator.next()) {
                    int i = unicodeSetIterator.codepoint;
                    Set<String> setExtract = extract(i, str, iCharCount, stringBuffer);
                    if (setExtract != null) {
                        String str2 = str.substring(0, iCharCount) + UTF16.valueOf(i);
                        Iterator<String> it = setExtract.iterator();
                        while (it.hasNext()) {
                            hashSet.add(str2 + it.next());
                        }
                    }
                }
            }
            iCharCount += Character.charCount(iCodePointAt);
        }
        return hashSet;
    }

    private Set<String> extract(int i, String str, int i2, StringBuffer stringBuffer) {
        boolean z;
        if (PROGRESS) {
            System.out.println(" extract: " + Utility.hex(UTF16.valueOf(i)) + ", " + Utility.hex(str.substring(i2)));
        }
        String decomposition = this.nfcImpl.getDecomposition(i);
        if (decomposition == null) {
            decomposition = UTF16.valueOf(i);
        }
        int iCharAt = UTF16.charAt(decomposition, 0);
        int charCount = UTF16.getCharCount(iCharAt) + 0;
        stringBuffer.setLength(0);
        int charCount2 = charCount;
        int iCharAt2 = iCharAt;
        int charCount3 = i2;
        while (true) {
            if (charCount3 < str.length()) {
                int iCharAt3 = UTF16.charAt(str, charCount3);
                if (iCharAt3 == iCharAt2) {
                    if (PROGRESS) {
                        System.out.println("  matches: " + Utility.hex(UTF16.valueOf(iCharAt3)));
                    }
                    if (charCount2 == decomposition.length()) {
                        stringBuffer.append(str.substring(charCount3 + UTF16.getCharCount(iCharAt3)));
                        z = true;
                        break;
                    }
                    iCharAt2 = UTF16.charAt(decomposition, charCount2);
                    charCount2 += UTF16.getCharCount(iCharAt2);
                } else {
                    if (PROGRESS) {
                        System.out.println("  buffer: " + Utility.hex(UTF16.valueOf(iCharAt3)));
                    }
                    UTF16.append(stringBuffer, iCharAt3);
                }
                charCount3 += UTF16.getCharCount(iCharAt3);
            } else {
                z = false;
                break;
            }
        }
        if (!z) {
            return null;
        }
        if (PROGRESS) {
            System.out.println("Matches");
        }
        if (stringBuffer.length() == 0) {
            return SET_WITH_NULL_STRING;
        }
        String string = stringBuffer.toString();
        if (Normalizer.compare(UTF16.valueOf(i) + string, str.substring(i2), 0) != 0) {
            return null;
        }
        return getEquivalents2(string);
    }
}
