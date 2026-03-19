package android.icu.text;

import android.icu.impl.Assert;
import android.icu.util.BytesTrie;
import java.text.CharacterIterator;

class BytesDictionaryMatcher extends DictionaryMatcher {
    private final byte[] characters;
    private final int transform;

    public BytesDictionaryMatcher(byte[] bArr, int i) {
        this.characters = bArr;
        Assert.assrt((2130706432 & i) == 16777216);
        this.transform = i;
    }

    private int transform(int i) {
        if (i == 8205) {
            return 255;
        }
        if (i == 8204) {
            return 254;
        }
        int i2 = i - (this.transform & DictionaryData.TRANSFORM_OFFSET_MASK);
        if (i2 < 0 || 253 < i2) {
            return -1;
        }
        return i2;
    }

    @Override
    public int matches(CharacterIterator characterIterator, int i, int[] iArr, int[] iArr2, int i2, int[] iArr3) {
        int iNextCodePoint;
        UCharacterIterator uCharacterIterator = UCharacterIterator.getInstance(characterIterator);
        BytesTrie bytesTrie = new BytesTrie(this.characters, 0);
        int iNextCodePoint2 = uCharacterIterator.nextCodePoint();
        if (iNextCodePoint2 == -1) {
            return 0;
        }
        BytesTrie.Result resultFirst = bytesTrie.first(transform(iNextCodePoint2));
        int i3 = 1;
        int i4 = 0;
        while (true) {
            if (resultFirst.hasValue()) {
                if (i4 < i2) {
                    if (iArr3 != null) {
                        iArr3[i4] = bytesTrie.getValue();
                    }
                    iArr[i4] = i3;
                    i4++;
                }
                if (resultFirst != BytesTrie.Result.FINAL_VALUE) {
                    if (i3 >= i || (iNextCodePoint = uCharacterIterator.nextCodePoint()) == -1) {
                        break;
                    }
                    i3++;
                    resultFirst = bytesTrie.next(transform(iNextCodePoint));
                } else {
                    break;
                }
            } else if (resultFirst == BytesTrie.Result.NO_MATCH) {
                break;
            }
        }
        iArr2[0] = i4;
        return i3;
    }

    @Override
    public int getType() {
        return 0;
    }
}
