package android.icu.impl;

import android.icu.impl.ICUBinary;
import android.icu.util.BytesTrie;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.MissingResourceException;

public final class UPropertyAliases {
    private static final int DATA_FORMAT = 1886282093;
    public static final UPropertyAliases INSTANCE;
    private static final IsAcceptable IS_ACCEPTABLE = new IsAcceptable(null);
    private static final int IX_BYTE_TRIES_OFFSET = 1;
    private static final int IX_NAME_GROUPS_OFFSET = 2;
    private static final int IX_RESERVED3_OFFSET = 3;
    private static final int IX_VALUE_MAPS_OFFSET = 0;
    private byte[] bytesTries;
    private String nameGroups;
    private int[] valueMaps;

    private static final class IsAcceptable implements ICUBinary.Authenticate {
        IsAcceptable(IsAcceptable isAcceptable) {
            this();
        }

        private IsAcceptable() {
        }

        @Override
        public boolean isDataVersionAcceptable(byte[] version) {
            return version[0] == 2;
        }
    }

    static {
        try {
            INSTANCE = new UPropertyAliases();
        } catch (IOException e) {
            MissingResourceException mre = new MissingResourceException("Could not construct UPropertyAliases. Missing pnames.icu", "", "");
            mre.initCause(e);
            throw mre;
        }
    }

    private void load(ByteBuffer bytes) throws IOException {
        ICUBinary.readHeader(bytes, DATA_FORMAT, IS_ACCEPTABLE);
        int indexesLength = bytes.getInt() / 4;
        if (indexesLength < 8) {
            throw new IOException("pnames.icu: not enough indexes");
        }
        int[] inIndexes = new int[indexesLength];
        inIndexes[0] = indexesLength * 4;
        for (int i = 1; i < indexesLength; i++) {
            inIndexes[i] = bytes.getInt();
        }
        int offset = inIndexes[0];
        int nextOffset = inIndexes[1];
        int numInts = (nextOffset - offset) / 4;
        this.valueMaps = ICUBinary.getInts(bytes, numInts, 0);
        int nextOffset2 = inIndexes[2];
        this.bytesTries = new byte[nextOffset2 - nextOffset];
        bytes.get(this.bytesTries);
        int numBytes = inIndexes[3] - nextOffset2;
        StringBuilder sb = new StringBuilder(numBytes);
        for (int i2 = 0; i2 < numBytes; i2++) {
            sb.append((char) bytes.get());
        }
        this.nameGroups = sb.toString();
    }

    private UPropertyAliases() throws IOException {
        ByteBuffer bytes = ICUBinary.getRequiredData("pnames.icu");
        load(bytes);
    }

    private int findProperty(int property) {
        int i = 1;
        for (int numRanges = this.valueMaps[0]; numRanges > 0; numRanges--) {
            int start = this.valueMaps[i];
            int limit = this.valueMaps[i + 1];
            int i2 = i + 2;
            if (property < start) {
                break;
            }
            if (property < limit) {
                return ((property - start) * 2) + i2;
            }
            i = i2 + ((limit - start) * 2);
        }
        return 0;
    }

    private int findPropertyValueNameGroup(int valueMapIndex, int value) {
        if (valueMapIndex == 0) {
            return 0;
        }
        int valueMapIndex2 = valueMapIndex + 1;
        int valueMapIndex3 = valueMapIndex2 + 1;
        int numRanges = this.valueMaps[valueMapIndex2];
        if (numRanges < 16) {
            int valueMapIndex4 = valueMapIndex3;
            while (numRanges > 0) {
                int start = this.valueMaps[valueMapIndex4];
                int limit = this.valueMaps[valueMapIndex4 + 1];
                int valueMapIndex5 = valueMapIndex4 + 2;
                if (value < start) {
                    break;
                }
                if (value < limit) {
                    return this.valueMaps[(valueMapIndex5 + value) - start];
                }
                valueMapIndex4 = valueMapIndex5 + (limit - start);
                numRanges--;
            }
        } else {
            int nameGroupOffsetsStart = (valueMapIndex3 + numRanges) - 16;
            int valueMapIndex6 = valueMapIndex3;
            do {
                int v = this.valueMaps[valueMapIndex6];
                if (value < v) {
                    break;
                }
                if (value == v) {
                    return this.valueMaps[(nameGroupOffsetsStart + valueMapIndex6) - valueMapIndex3];
                }
                valueMapIndex6++;
            } while (valueMapIndex6 < nameGroupOffsetsStart);
        }
        return 0;
    }

    private String getName(int nameGroupsIndex, int nameIndex) {
        int nameGroupsIndex2;
        int nameGroupsIndex3 = nameGroupsIndex + 1;
        int numNames = this.nameGroups.charAt(nameGroupsIndex);
        if (nameIndex < 0 || numNames <= nameIndex) {
            throw new IllegalIcuArgumentException("Invalid property (value) name choice");
        }
        int nameGroupsIndex4 = nameGroupsIndex3;
        while (nameIndex > 0) {
            while (true) {
                nameGroupsIndex2 = nameGroupsIndex4 + 1;
                if (this.nameGroups.charAt(nameGroupsIndex4) != 0) {
                    nameGroupsIndex4 = nameGroupsIndex2;
                }
            }
            nameIndex--;
            nameGroupsIndex4 = nameGroupsIndex2;
        }
        int nameStart = nameGroupsIndex4;
        while (this.nameGroups.charAt(nameGroupsIndex4) != 0) {
            nameGroupsIndex4++;
        }
        if (nameStart == nameGroupsIndex4) {
            return null;
        }
        return this.nameGroups.substring(nameStart, nameGroupsIndex4);
    }

    private static int asciiToLowercase(int c) {
        return (65 > c || c > 90) ? c : c + 32;
    }

    private boolean containsName(BytesTrie trie, CharSequence name) {
        BytesTrie.Result result = BytesTrie.Result.NO_VALUE;
        for (int i = 0; i < name.length(); i++) {
            int c = name.charAt(i);
            if (c != 45 && c != 95 && c != 32 && (9 > c || c > 13)) {
                if (!result.hasNext()) {
                    return false;
                }
                result = trie.next(asciiToLowercase(c));
            }
        }
        return result.hasValue();
    }

    public String getPropertyName(int property, int nameChoice) {
        int valueMapIndex = findProperty(property);
        if (valueMapIndex == 0) {
            throw new IllegalArgumentException("Invalid property enum " + property + " (0x" + Integer.toHexString(property) + ")");
        }
        return getName(this.valueMaps[valueMapIndex], nameChoice);
    }

    public String getPropertyValueName(int property, int value, int nameChoice) {
        int valueMapIndex = findProperty(property);
        if (valueMapIndex == 0) {
            throw new IllegalArgumentException("Invalid property enum " + property + " (0x" + Integer.toHexString(property) + ")");
        }
        int nameGroupOffset = findPropertyValueNameGroup(this.valueMaps[valueMapIndex + 1], value);
        if (nameGroupOffset == 0) {
            throw new IllegalArgumentException("Property " + property + " (0x" + Integer.toHexString(property) + ") does not have named values");
        }
        return getName(nameGroupOffset, nameChoice);
    }

    private int getPropertyOrValueEnum(int bytesTrieOffset, CharSequence alias) {
        BytesTrie trie = new BytesTrie(this.bytesTries, bytesTrieOffset);
        if (containsName(trie, alias)) {
            return trie.getValue();
        }
        return -1;
    }

    public int getPropertyEnum(CharSequence alias) {
        return getPropertyOrValueEnum(0, alias);
    }

    public int getPropertyValueEnum(int property, CharSequence alias) {
        int valueMapIndex = findProperty(property);
        if (valueMapIndex == 0) {
            throw new IllegalArgumentException("Invalid property enum " + property + " (0x" + Integer.toHexString(property) + ")");
        }
        int valueMapIndex2 = this.valueMaps[valueMapIndex + 1];
        if (valueMapIndex2 == 0) {
            throw new IllegalArgumentException("Property " + property + " (0x" + Integer.toHexString(property) + ") does not have named values");
        }
        return getPropertyOrValueEnum(this.valueMaps[valueMapIndex2], alias);
    }

    public int getPropertyValueEnumNoThrow(int property, CharSequence alias) {
        int valueMapIndex;
        int valueMapIndex2 = findProperty(property);
        if (valueMapIndex2 == 0 || (valueMapIndex = this.valueMaps[valueMapIndex2 + 1]) == 0) {
            return -1;
        }
        return getPropertyOrValueEnum(this.valueMaps[valueMapIndex], alias);
    }

    public static int compare(String stra, String strb) {
        boolean endstra;
        int rc;
        int istra = 0;
        int istrb = 0;
        int cstra = 0;
        int cstrb = 0;
        while (true) {
            if (istra < stra.length()) {
                cstra = stra.charAt(istra);
                switch (cstra) {
                    case 9:
                    case 10:
                    case 11:
                    case 12:
                    case 13:
                    case 32:
                    case 45:
                    case 95:
                        istra++;
                        continue;
                }
            }
            while (istrb < strb.length()) {
                cstrb = strb.charAt(istrb);
                switch (cstrb) {
                    case 9:
                    case 10:
                    case 11:
                    case 12:
                    case 13:
                    case 32:
                    case 45:
                    case 95:
                        istrb++;
                        break;
                }
                endstra = istra != stra.length();
                boolean endstrb = istrb != strb.length();
                if (!endstra) {
                    if (endstrb) {
                        return 0;
                    }
                    cstra = 0;
                } else if (endstrb) {
                    cstrb = 0;
                }
                rc = asciiToLowercase(cstra) - asciiToLowercase(cstrb);
                if (rc == 0) {
                    return rc;
                }
                istra++;
                istrb++;
            }
            if (istra != stra.length()) {
            }
            if (istrb != strb.length()) {
            }
            if (!endstra) {
            }
            rc = asciiToLowercase(cstra) - asciiToLowercase(cstrb);
            if (rc == 0) {
            }
        }
    }
}
