package sun.security.util;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.HashMap;

public class ManifestDigester {
    public static final String MF_MAIN_ATTRS = "Manifest-Main-Attributes";
    private HashMap<String, Entry> entries = new HashMap<>();
    private byte[] rawBytes;

    static class Position {
        int endOfFirstLine;
        int endOfSection;
        int startOfNext;

        Position() {
        }
    }

    private boolean findSection(int i, Position position) {
        int length = this.rawBytes.length;
        position.endOfFirstLine = -1;
        int i2 = i;
        boolean z = true;
        while (i < length) {
            byte b = this.rawBytes[i];
            if (b == 10) {
                if (position.endOfFirstLine == -1) {
                    position.endOfFirstLine = i - 1;
                }
                if (!z || i == length - 1) {
                    if (i != length - 1) {
                        position.endOfSection = i;
                    } else {
                        position.endOfSection = i2;
                    }
                    position.startOfNext = i + 1;
                    return true;
                }
                i2 = i;
                z = true;
            } else {
                if (b == 13) {
                    if (position.endOfFirstLine == -1) {
                        position.endOfFirstLine = i - 1;
                    }
                    if (i < length) {
                        int i3 = i + 1;
                        if (this.rawBytes[i3] == 10) {
                            i = i3;
                        }
                    }
                    if (position.endOfFirstLine == -1) {
                    }
                    if (!z) {
                    }
                    if (i != length - 1) {
                    }
                    position.startOfNext = i + 1;
                    return true;
                }
                z = false;
            }
            i++;
        }
        return false;
    }

    public ManifestDigester(byte[] bArr) {
        this.rawBytes = bArr;
        new ByteArrayOutputStream();
        Position position = new Position();
        if (findSection(0, position)) {
            this.entries.put(MF_MAIN_ATTRS, new Entry(0, position.endOfSection + 1, position.startOfNext, this.rawBytes));
            for (int i = position.startOfNext; findSection(i, position); i = position.startOfNext) {
                int i2 = (position.endOfFirstLine - i) + 1;
                int i3 = (position.endOfSection - i) + 1;
                int i4 = position.startOfNext - i;
                if (i2 > 6 && isNameAttr(bArr, i)) {
                    StringBuilder sb = new StringBuilder(i3);
                    try {
                        sb.append(new String(bArr, i + 6, i2 - 6, "UTF8"));
                        int i5 = i2 + i;
                        if (i5 - i < i3) {
                            if (bArr[i5] == 13) {
                                i5 += 2;
                            } else {
                                i5++;
                            }
                        }
                        while (i5 - i < i3) {
                            int i6 = i5 + 1;
                            if (bArr[i5] != 32) {
                                break;
                            }
                            i5 = i6;
                            while (true) {
                                if (i5 - i >= i3) {
                                    break;
                                }
                                int i7 = i5 + 1;
                                if (bArr[i5] != 10) {
                                    i5 = i7;
                                } else {
                                    i5 = i7;
                                    break;
                                }
                            }
                            if (bArr[i5 - 1] != 10) {
                                return;
                            }
                            sb.append(new String(bArr, i6, bArr[i5 - 2] == 13 ? (i5 - i6) - 2 : (i5 - i6) - 1, "UTF8"));
                        }
                        this.entries.put(sb.toString(), new Entry(i, i3, i4, this.rawBytes));
                    } catch (UnsupportedEncodingException e) {
                        throw new IllegalStateException("UTF8 not available on platform");
                    }
                }
            }
        }
    }

    private boolean isNameAttr(byte[] bArr, int i) {
        if (bArr[i] == 78 || bArr[i] == 110) {
            int i2 = i + 1;
            if (bArr[i2] == 97 || bArr[i2] == 65) {
                int i3 = i + 2;
                if (bArr[i3] == 109 || bArr[i3] == 77) {
                    int i4 = i + 3;
                    if ((bArr[i4] == 101 || bArr[i4] == 69) && bArr[i + 4] == 58 && bArr[i + 5] == 32) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static class Entry {
        int length;
        int lengthWithBlankLine;
        int offset;
        boolean oldStyle;
        byte[] rawBytes;

        public Entry(int i, int i2, int i3, byte[] bArr) {
            this.offset = i;
            this.length = i2;
            this.lengthWithBlankLine = i3;
            this.rawBytes = bArr;
        }

        public byte[] digest(MessageDigest messageDigest) {
            messageDigest.reset();
            if (this.oldStyle) {
                doOldStyle(messageDigest, this.rawBytes, this.offset, this.lengthWithBlankLine);
            } else {
                messageDigest.update(this.rawBytes, this.offset, this.lengthWithBlankLine);
            }
            return messageDigest.digest();
        }

        private void doOldStyle(MessageDigest messageDigest, byte[] bArr, int i, int i2) {
            int i3 = i2 + i;
            byte b = -1;
            int i4 = i;
            while (i < i3) {
                if (bArr[i] == 13 && b == 32) {
                    messageDigest.update(bArr, i4, (i - i4) - 1);
                    i4 = i;
                }
                b = bArr[i];
                i++;
            }
            messageDigest.update(bArr, i4, i - i4);
        }

        public byte[] digestWorkaround(MessageDigest messageDigest) {
            messageDigest.reset();
            messageDigest.update(this.rawBytes, this.offset, this.length);
            return messageDigest.digest();
        }
    }

    public Entry get(String str, boolean z) {
        Entry entry = this.entries.get(str);
        if (entry != null) {
            entry.oldStyle = z;
        }
        return entry;
    }

    public byte[] manifestDigest(MessageDigest messageDigest) {
        messageDigest.reset();
        messageDigest.update(this.rawBytes, 0, this.rawBytes.length);
        return messageDigest.digest();
    }
}
