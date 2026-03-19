package android.icu.text;

import android.icu.impl.CharacterIteration;
import android.icu.lang.UCharacter;
import android.icu.lang.UProperty;
import android.icu.text.DictionaryBreakEngine;
import java.text.CharacterIterator;
import java.util.concurrent.atomic.AtomicReferenceArray;

final class UnhandledBreakEngine implements LanguageBreakEngine {
    final AtomicReferenceArray<UnicodeSet> fHandled = new AtomicReferenceArray<>(5);

    public UnhandledBreakEngine() {
        for (int i = 0; i < this.fHandled.length(); i++) {
            this.fHandled.set(i, new UnicodeSet());
        }
    }

    @Override
    public boolean handles(int i, int i2) {
        return i2 >= 0 && i2 < this.fHandled.length() && this.fHandled.get(i2).contains(i);
    }

    @Override
    public int findBreaks(CharacterIterator characterIterator, int i, int i2, int i3, DictionaryBreakEngine.DequeI dequeI) {
        if (i3 >= 0 && i3 < this.fHandled.length()) {
            UnicodeSet unicodeSet = this.fHandled.get(i3);
            int iCurrent32 = CharacterIteration.current32(characterIterator);
            while (characterIterator.getIndex() < i2 && unicodeSet.contains(iCurrent32)) {
                CharacterIteration.next32(characterIterator);
                iCurrent32 = CharacterIteration.current32(characterIterator);
            }
            return 0;
        }
        return 0;
    }

    public void handleChar(int i, int i2) {
        if (i2 >= 0 && i2 < this.fHandled.length() && i != Integer.MAX_VALUE) {
            UnicodeSet unicodeSet = this.fHandled.get(i2);
            if (!unicodeSet.contains(i)) {
                int intPropertyValue = UCharacter.getIntPropertyValue(i, UProperty.SCRIPT);
                UnicodeSet unicodeSet2 = new UnicodeSet();
                unicodeSet2.applyIntPropertyValue(UProperty.SCRIPT, intPropertyValue);
                unicodeSet2.addAll(unicodeSet);
                this.fHandled.set(i2, unicodeSet2);
            }
        }
    }
}
