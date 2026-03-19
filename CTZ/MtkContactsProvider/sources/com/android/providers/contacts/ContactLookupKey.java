package com.android.providers.contacts;

import java.util.ArrayList;

public class ContactLookupKey {

    public static class LookupKeySegment implements Comparable<LookupKeySegment> {
        public int accountHashCode;
        public long contactId;
        public String key;
        public int lookupType;
        public String rawContactId;

        @Override
        public int compareTo(LookupKeySegment lookupKeySegment) {
            if (this.contactId > lookupKeySegment.contactId) {
                return -1;
            }
            if (this.contactId < lookupKeySegment.contactId) {
                return 1;
            }
            return 0;
        }
    }

    public static int getAccountHashCode(String str, String str2) {
        if (str == null || str2 == null) {
            return 0;
        }
        return (str.hashCode() ^ str2.hashCode()) & 4095;
    }

    public static void appendToLookupKey(StringBuilder sb, String str, String str2, long j, String str3, String str4) {
        if (str4 == null) {
            str4 = "";
        }
        if (sb.length() != 0) {
            sb.append(".");
        }
        sb.append(getAccountHashCode(str, str2));
        if (str3 == null) {
            sb.append('r');
            sb.append(j);
            sb.append('-');
            sb.append(NameNormalizer.normalize(str4));
            return;
        }
        int length = sb.length();
        sb.append('i');
        if (appendEscapedSourceId(sb, str3)) {
            sb.setCharAt(length, 'e');
        }
    }

    private static boolean appendEscapedSourceId(StringBuilder sb, String str) {
        int i = 0;
        boolean z = false;
        while (true) {
            int iIndexOf = str.indexOf(46, i);
            if (iIndexOf == -1) {
                sb.append((CharSequence) str, i, str.length());
                return z;
            }
            sb.append((CharSequence) str, i, iIndexOf);
            sb.append("..");
            i = iIndexOf + 1;
            z = true;
        }
    }

    public ArrayList<LookupKeySegment> parse(String str) {
        int i;
        char cCharAt;
        int i2;
        int i3;
        String strSubstring;
        ArrayList<LookupKeySegment> arrayList = new ArrayList<>();
        if ("profile".equals(str)) {
            LookupKeySegment lookupKeySegment = new LookupKeySegment();
            lookupKeySegment.lookupType = 3;
            arrayList.add(lookupKeySegment);
            return arrayList;
        }
        int length = str.length();
        String strSubstring2 = null;
        int i4 = 0;
        boolean z = false;
        while (i4 < length) {
            char c = 0;
            int i5 = 0;
            while (true) {
                if (i4 < length) {
                    i = i4 + 1;
                    cCharAt = str.charAt(i4);
                    if (cCharAt >= '0' && cCharAt <= '9') {
                        i5 = (i5 * 10) + (cCharAt - '0');
                        c = cCharAt;
                        i4 = i;
                    }
                } else {
                    char c2 = c;
                    i = i4;
                    cCharAt = c2;
                }
            }
            if (cCharAt == 'i') {
                i2 = 0;
                z = false;
            } else if (cCharAt == 'e') {
                i2 = 0;
                z = true;
            } else if (cCharAt == 'n') {
                i2 = 1;
            } else {
                if (cCharAt != 'r') {
                    if (cCharAt == 'c') {
                        throw new IllegalArgumentException("Work contact lookup key is not accepted here: " + str);
                    }
                    throw new IllegalArgumentException("Invalid lookup id: " + str);
                }
                i2 = 2;
            }
            switch (i2) {
                case 0:
                    if (!z) {
                        i3 = i;
                        while (true) {
                            if (i3 < length) {
                                int i6 = i3 + 1;
                                if (str.charAt(i3) == '.') {
                                    i3 = i6;
                                } else {
                                    i3 = i6;
                                }
                            }
                        }
                        strSubstring = i3 != length ? str.substring(i, i3 - 1) : str.substring(i);
                    } else {
                        StringBuffer stringBuffer = new StringBuffer();
                        while (true) {
                            if (i < length) {
                                int i7 = i + 1;
                                char cCharAt2 = str.charAt(i);
                                if (cCharAt2 != '.') {
                                    stringBuffer.append(cCharAt2);
                                    i = i7;
                                } else {
                                    if (i7 == length) {
                                        throw new IllegalArgumentException("Invalid lookup id: " + str);
                                    }
                                    if (str.charAt(i7) == '.') {
                                        stringBuffer.append('.');
                                        i = i7 + 1;
                                    } else {
                                        i = i7;
                                    }
                                }
                            }
                        }
                        String string = stringBuffer.toString();
                        i3 = i;
                        strSubstring = string;
                    }
                    break;
                case 1:
                    i3 = i;
                    while (true) {
                        if (i3 < length) {
                            int i8 = i3 + 1;
                            if (str.charAt(i3) == '.') {
                                i3 = i8;
                            } else {
                                i3 = i8;
                            }
                        }
                    }
                    strSubstring = i3 != length ? str.substring(i, i3 - 1) : str.substring(i);
                    break;
                case 2:
                    int i9 = i;
                    int i10 = -1;
                    while (i9 < length) {
                        char cCharAt3 = str.charAt(i9);
                        if (cCharAt3 == '-' && i10 == -1) {
                            i10 = i9;
                        }
                        i9++;
                        if (cCharAt3 == '.') {
                            if (i10 != -1) {
                                strSubstring2 = str.substring(i, i10);
                                i = i10 + 1;
                            }
                            strSubstring = i9 != length ? str.substring(i) : str.substring(i, i9 - 1);
                            i3 = i9;
                            break;
                        }
                    }
                    if (i10 != -1) {
                    }
                    if (i9 != length) {
                    }
                    i3 = i9;
                    break;
                default:
                    throw new IllegalStateException();
            }
            LookupKeySegment lookupKeySegment2 = new LookupKeySegment();
            lookupKeySegment2.accountHashCode = i5;
            lookupKeySegment2.lookupType = i2;
            lookupKeySegment2.rawContactId = strSubstring2;
            lookupKeySegment2.key = strSubstring;
            lookupKeySegment2.contactId = -1L;
            arrayList.add(lookupKeySegment2);
            i4 = i3;
        }
        return arrayList;
    }
}
