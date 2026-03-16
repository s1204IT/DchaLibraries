package com.android.providers.contacts;

import android.net.Uri;
import java.util.ArrayList;

public class ContactLookupKey {

    public static class LookupKeySegment implements Comparable<LookupKeySegment> {
        public int accountHashCode;
        public long contactId;
        public String key;
        public int lookupType;
        public String rawContactId;

        @Override
        public int compareTo(LookupKeySegment another) {
            if (this.contactId > another.contactId) {
                return -1;
            }
            if (this.contactId < another.contactId) {
                return 1;
            }
            return 0;
        }
    }

    public static int getAccountHashCode(String accountTypeWithDataSet, String accountName) {
        if (accountTypeWithDataSet == null || accountName == null) {
            return 0;
        }
        return (accountTypeWithDataSet.hashCode() ^ accountName.hashCode()) & 4095;
    }

    public static void appendToLookupKey(StringBuilder lookupKey, String accountTypeWithDataSet, String accountName, long rawContactId, String sourceId, String displayName) {
        if (displayName == null) {
            displayName = "";
        }
        if (lookupKey.length() != 0) {
            lookupKey.append(".");
        }
        lookupKey.append(getAccountHashCode(accountTypeWithDataSet, accountName));
        if (sourceId == null) {
            lookupKey.append('r').append(rawContactId).append('-').append(NameNormalizer.normalize(displayName));
            return;
        }
        int pos = lookupKey.length();
        lookupKey.append('i');
        if (appendEscapedSourceId(lookupKey, sourceId)) {
            lookupKey.setCharAt(pos, 'e');
        }
    }

    private static boolean appendEscapedSourceId(StringBuilder sb, String sourceId) {
        boolean escaped = false;
        int start = 0;
        while (true) {
            int index = sourceId.indexOf(46, start);
            if (index == -1) {
                sb.append((CharSequence) sourceId, start, sourceId.length());
                return escaped;
            }
            escaped = true;
            sb.append((CharSequence) sourceId, start, index);
            sb.append("..");
            start = index + 1;
        }
    }

    public ArrayList<LookupKeySegment> parse(String lookupKey) {
        int lookupType;
        String key;
        ArrayList<LookupKeySegment> list = new ArrayList<>();
        if ("profile".equals(lookupKey)) {
            LookupKeySegment profileSegment = new LookupKeySegment();
            profileSegment.lookupType = 3;
            list.add(profileSegment);
        } else {
            String string = Uri.decode(lookupKey);
            int offset = 0;
            int length = string.length();
            boolean escaped = false;
            String rawContactId = null;
            while (offset < length) {
                char c = 0;
                int hashCode = 0;
                int offset2 = offset;
                while (true) {
                    if (offset2 < length) {
                        offset = offset2 + 1;
                        c = string.charAt(offset2);
                        if (c >= '0' && c <= '9') {
                            hashCode = (hashCode * 10) + (c - '0');
                            offset2 = offset;
                        }
                    } else {
                        offset = offset2;
                    }
                }
                if (c == 'i') {
                    lookupType = 0;
                    escaped = false;
                } else if (c == 'e') {
                    lookupType = 0;
                    escaped = true;
                } else if (c == 'n') {
                    lookupType = 1;
                } else if (c == 'r') {
                    lookupType = 2;
                } else {
                    throw new IllegalArgumentException("Invalid lookup id: " + lookupKey);
                }
                switch (lookupType) {
                    case 0:
                        if (escaped) {
                            StringBuffer sb = new StringBuffer();
                            int offset3 = offset;
                            while (true) {
                                if (offset3 < length) {
                                    offset = offset3 + 1;
                                    char c2 = string.charAt(offset3);
                                    if (c2 == '.') {
                                        if (offset == length) {
                                            throw new IllegalArgumentException("Invalid lookup id: " + lookupKey);
                                        }
                                        char c3 = string.charAt(offset);
                                        if (c3 == '.') {
                                            sb.append('.');
                                            offset3 = offset + 1;
                                        }
                                    } else {
                                        sb.append(c2);
                                        offset3 = offset;
                                    }
                                } else {
                                    offset = offset3;
                                }
                            }
                            key = sb.toString();
                        } else {
                            int start = offset;
                            int offset4 = offset;
                            while (true) {
                                if (offset4 < length) {
                                    offset = offset4 + 1;
                                    char c4 = string.charAt(offset4);
                                    if (c4 != '.') {
                                        offset4 = offset;
                                    }
                                } else {
                                    offset = offset4;
                                }
                            }
                            if (offset == length) {
                                key = string.substring(start);
                            } else {
                                key = string.substring(start, offset - 1);
                            }
                        }
                        break;
                    case 1:
                        int start2 = offset;
                        int offset5 = offset;
                        while (true) {
                            if (offset5 < length) {
                                offset = offset5 + 1;
                                char c5 = string.charAt(offset5);
                                if (c5 != '.') {
                                    offset5 = offset;
                                }
                            } else {
                                offset = offset5;
                            }
                        }
                        if (offset == length) {
                            key = string.substring(start2);
                        } else {
                            key = string.substring(start2, offset - 1);
                        }
                        break;
                    case 2:
                        int dash = -1;
                        int start3 = offset;
                        while (offset < length) {
                            char c6 = string.charAt(offset);
                            if (c6 == '-' && dash == -1) {
                                dash = offset;
                            }
                            offset++;
                            if (c6 == '.') {
                                if (dash != -1) {
                                    rawContactId = string.substring(start3, dash);
                                    start3 = dash + 1;
                                }
                                if (offset != length) {
                                    key = string.substring(start3);
                                } else {
                                    key = string.substring(start3, offset - 1);
                                }
                                break;
                            }
                        }
                        if (dash != -1) {
                        }
                        if (offset != length) {
                        }
                        break;
                    default:
                        throw new IllegalStateException();
                }
                LookupKeySegment segment = new LookupKeySegment();
                segment.accountHashCode = hashCode;
                segment.lookupType = lookupType;
                segment.rawContactId = rawContactId;
                segment.key = key;
                segment.contactId = -1L;
                list.add(segment);
            }
        }
        return list;
    }
}
