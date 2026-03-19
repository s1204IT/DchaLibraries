package android.icu.text;

import android.icu.util.BytesTrie;
import android.icu.util.CharsTrie;
import java.text.CharacterIterator;

class CharsDictionaryMatcher extends DictionaryMatcher {
    private CharSequence characters;

    public CharsDictionaryMatcher(CharSequence charSequence) {
        this.characters = charSequence;
    }

    @Override
    public int matches(CharacterIterator characterIterator, int i, int[] iArr, int[] iArr2, int i2, int[] iArr3) {
        int iNextCodePoint;
        UCharacterIterator uCharacterIterator = UCharacterIterator.getInstance(characterIterator);
        CharsTrie charsTrie = new CharsTrie(this.characters, 0);
        int iNextCodePoint2 = uCharacterIterator.nextCodePoint();
        if (iNextCodePoint2 == -1) {
            return 0;
        }
        BytesTrie.Result resultFirstForCodePoint = charsTrie.firstForCodePoint(iNextCodePoint2);
        int i3 = 1;
        int i4 = 0;
        while (true) {
            if (resultFirstForCodePoint.hasValue()) {
                if (i4 < i2) {
                    if (iArr3 != null) {
                        iArr3[i4] = charsTrie.getValue();
                    }
                    iArr[i4] = i3;
                    i4++;
                }
                if (resultFirstForCodePoint != BytesTrie.Result.FINAL_VALUE) {
                    if (i3 >= i || (iNextCodePoint = uCharacterIterator.nextCodePoint()) == -1) {
                        break;
                    }
                    i3++;
                    resultFirstForCodePoint = charsTrie.nextForCodePoint(iNextCodePoint);
                } else {
                    break;
                }
            } else if (resultFirstForCodePoint == BytesTrie.Result.NO_MATCH) {
                break;
            }
        }
        iArr2[0] = i4;
        return i3;
    }

    @Override
    public int getType() {
        return 1;
    }
}
