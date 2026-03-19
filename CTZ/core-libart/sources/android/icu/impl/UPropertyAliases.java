package android.icu.impl;

import android.icu.impl.ICUBinary;
import android.icu.util.BytesTrie;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.MissingResourceException;

public final class UPropertyAliases {
    private static final int DATA_FORMAT = 1886282093;
    public static final UPropertyAliases INSTANCE;
    private static final IsAcceptable IS_ACCEPTABLE = new IsAcceptable();
    private static final int IX_BYTE_TRIES_OFFSET = 1;
    private static final int IX_NAME_GROUPS_OFFSET = 2;
    private static final int IX_RESERVED3_OFFSET = 3;
    private static final int IX_VALUE_MAPS_OFFSET = 0;
    private byte[] bytesTries;
    private String nameGroups;
    private int[] valueMaps;

    private static final class IsAcceptable implements ICUBinary.Authenticate {
        private IsAcceptable() {
        }

        @Override
        public boolean isDataVersionAcceptable(byte[] bArr) {
            return bArr[0] == 2;
        }
    }

    static {
        try {
            INSTANCE = new UPropertyAliases();
        } catch (IOException e) {
            MissingResourceException missingResourceException = new MissingResourceException("Could not construct UPropertyAliases. Missing pnames.icu", "", "");
            missingResourceException.initCause(e);
            throw missingResourceException;
        }
    }

    private void load(ByteBuffer byteBuffer) throws IOException {
        ICUBinary.readHeader(byteBuffer, DATA_FORMAT, IS_ACCEPTABLE);
        int i = byteBuffer.getInt() / 4;
        if (i < 8) {
            throw new IOException("pnames.icu: not enough indexes");
        }
        int[] iArr = new int[i];
        iArr[0] = i * 4;
        for (int i2 = 1; i2 < i; i2++) {
            iArr[i2] = byteBuffer.getInt();
        }
        int i3 = iArr[0];
        int i4 = iArr[1];
        this.valueMaps = ICUBinary.getInts(byteBuffer, (i4 - i3) / 4, 0);
        int i5 = iArr[2];
        this.bytesTries = new byte[i5 - i4];
        byteBuffer.get(this.bytesTries);
        int i6 = iArr[3] - i5;
        StringBuilder sb = new StringBuilder(i6);
        for (int i7 = 0; i7 < i6; i7++) {
            sb.append((char) byteBuffer.get());
        }
        this.nameGroups = sb.toString();
    }

    private UPropertyAliases() throws IOException {
        load(ICUBinary.getRequiredData("pnames.icu"));
    }

    private int findProperty(int i) {
        int i2 = 1;
        for (int i3 = this.valueMaps[0]; i3 > 0; i3--) {
            int i4 = this.valueMaps[i2];
            int i5 = this.valueMaps[i2 + 1];
            int i6 = i2 + 2;
            if (i < i4) {
                break;
            }
            if (i < i5) {
                return i6 + ((i - i4) * 2);
            }
            i2 = i6 + ((i5 - i4) * 2);
        }
        return 0;
    }

    private int findPropertyValueNameGroup(int i, int i2) {
        if (i == 0) {
            return 0;
        }
        int i3 = i + 1;
        int i4 = i3 + 1;
        int i5 = this.valueMaps[i3];
        if (i5 >= 16) {
            int i6 = (i5 + i4) - 16;
            int i7 = i4;
            do {
                int i8 = this.valueMaps[i7];
                if (i2 < i8) {
                    break;
                }
                if (i2 == i8) {
                    return this.valueMaps[(i6 + i7) - i4];
                }
                i7++;
            } while (i7 < i6);
        } else {
            while (i5 > 0) {
                int i9 = this.valueMaps[i4];
                int i10 = this.valueMaps[i4 + 1];
                int i11 = i4 + 2;
                if (i2 < i9) {
                    break;
                }
                if (i2 < i10) {
                    return this.valueMaps[(i11 + i2) - i9];
                }
                i4 = i11 + (i10 - i9);
                i5--;
            }
        }
        return 0;
    }

    private String getName(int i, int i2) {
        int i3;
        int i4 = i + 1;
        char cCharAt = this.nameGroups.charAt(i);
        if (i2 < 0 || cCharAt <= i2) {
            throw new IllegalIcuArgumentException("Invalid property (value) name choice");
        }
        while (i2 > 0) {
            while (true) {
                i3 = i4 + 1;
                if (this.nameGroups.charAt(i4) != 0) {
                    i4 = i3;
                }
            }
            i2--;
            i4 = i3;
        }
        int i5 = i4;
        while (this.nameGroups.charAt(i5) != 0) {
            i5++;
        }
        if (i4 == i5) {
            return null;
        }
        return this.nameGroups.substring(i4, i5);
    }

    private static int asciiToLowercase(int i) {
        return (65 > i || i > 90) ? i : i + 32;
    }

    private boolean containsName(BytesTrie bytesTrie, CharSequence charSequence) {
        BytesTrie.Result next = BytesTrie.Result.NO_VALUE;
        for (int i = 0; i < charSequence.length(); i++) {
            char cCharAt = charSequence.charAt(i);
            if (cCharAt != '-' && cCharAt != '_' && cCharAt != ' ' && ('\t' > cCharAt || cCharAt > '\r')) {
                if (!next.hasNext()) {
                    return false;
                }
                next = bytesTrie.next(asciiToLowercase(cCharAt));
            }
        }
        return next.hasValue();
    }

    public String getPropertyName(int i, int i2) {
        int iFindProperty = findProperty(i);
        if (iFindProperty == 0) {
            throw new IllegalArgumentException("Invalid property enum " + i + " (0x" + Integer.toHexString(i) + ")");
        }
        return getName(this.valueMaps[iFindProperty], i2);
    }

    public String getPropertyValueName(int i, int i2, int i3) {
        int iFindProperty = findProperty(i);
        if (iFindProperty == 0) {
            throw new IllegalArgumentException("Invalid property enum " + i + " (0x" + Integer.toHexString(i) + ")");
        }
        int iFindPropertyValueNameGroup = findPropertyValueNameGroup(this.valueMaps[iFindProperty + 1], i2);
        if (iFindPropertyValueNameGroup == 0) {
            throw new IllegalArgumentException("Property " + i + " (0x" + Integer.toHexString(i) + ") does not have named values");
        }
        return getName(iFindPropertyValueNameGroup, i3);
    }

    private int getPropertyOrValueEnum(int i, CharSequence charSequence) {
        BytesTrie bytesTrie = new BytesTrie(this.bytesTries, i);
        if (containsName(bytesTrie, charSequence)) {
            return bytesTrie.getValue();
        }
        return -1;
    }

    public int getPropertyEnum(CharSequence charSequence) {
        return getPropertyOrValueEnum(0, charSequence);
    }

    public int getPropertyValueEnum(int i, CharSequence charSequence) {
        int iFindProperty = findProperty(i);
        if (iFindProperty == 0) {
            throw new IllegalArgumentException("Invalid property enum " + i + " (0x" + Integer.toHexString(i) + ")");
        }
        int i2 = this.valueMaps[iFindProperty + 1];
        if (i2 == 0) {
            throw new IllegalArgumentException("Property " + i + " (0x" + Integer.toHexString(i) + ") does not have named values");
        }
        return getPropertyOrValueEnum(this.valueMaps[i2], charSequence);
    }

    public int getPropertyValueEnumNoThrow(int i, CharSequence charSequence) {
        int i2;
        int iFindProperty = findProperty(i);
        if (iFindProperty == 0 || (i2 = this.valueMaps[iFindProperty + 1]) == 0) {
            return -1;
        }
        return getPropertyOrValueEnum(this.valueMaps[i2], charSequence);
    }

    public static int compare(String str, String str2) {
        boolean z;
        int iAsciiToLowercase;
        int i = 0;
        int i2 = 0;
        char cCharAt = 0;
        char cCharAt2 = 0;
        while (true) {
            if (i < str.length()) {
                cCharAt = str.charAt(i);
                if (cCharAt != ' ' && cCharAt != '-' && cCharAt != '_') {
                    switch (cCharAt) {
                    }
                }
                i++;
            }
            while (i2 < str2.length()) {
                cCharAt2 = str2.charAt(i2);
                if (cCharAt2 != ' ' && cCharAt2 != '-' && cCharAt2 != '_') {
                    switch (cCharAt2) {
                    }
                    z = i != str.length();
                    boolean z2 = i2 == str2.length();
                    if (!z) {
                        if (z2) {
                            return 0;
                        }
                        cCharAt = 0;
                    } else if (z2) {
                        cCharAt2 = 0;
                    }
                    iAsciiToLowercase = asciiToLowercase(cCharAt) - asciiToLowercase(cCharAt2);
                    if (iAsciiToLowercase == 0) {
                        return iAsciiToLowercase;
                    }
                    i++;
                    i2++;
                }
                i2++;
            }
            if (i != str.length()) {
            }
            if (i2 == str2.length()) {
            }
            if (!z) {
            }
            iAsciiToLowercase = asciiToLowercase(cCharAt) - asciiToLowercase(cCharAt2);
            if (iAsciiToLowercase == 0) {
            }
        }
    }
}
