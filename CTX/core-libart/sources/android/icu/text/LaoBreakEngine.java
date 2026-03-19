package android.icu.text;

import android.icu.lang.UCharacter;
import android.icu.lang.UProperty;
import android.icu.text.DictionaryBreakEngine;
import java.io.IOException;
import java.text.CharacterIterator;

class LaoBreakEngine extends DictionaryBreakEngine {
    private static final byte LAO_LOOKAHEAD = 3;
    private static final byte LAO_MIN_WORD = 2;
    private static final byte LAO_PREFIX_COMBINE_THRESHOLD = 3;
    private static final byte LAO_ROOT_COMBINE_THRESHOLD = 3;
    private static UnicodeSet fEndWordSet;
    private DictionaryMatcher fDictionary;
    private static UnicodeSet fLaoWordSet = new UnicodeSet();
    private static UnicodeSet fMarkSet = new UnicodeSet();
    private static UnicodeSet fBeginWordSet = new UnicodeSet();

    static {
        fLaoWordSet.applyPattern("[[:Laoo:]&[:LineBreak=SA:]]");
        fLaoWordSet.compact();
        fMarkSet.applyPattern("[[:Laoo:]&[:LineBreak=SA:]&[:M:]]");
        fMarkSet.add(32);
        fEndWordSet = new UnicodeSet(fLaoWordSet);
        fEndWordSet.remove(3776, 3780);
        fBeginWordSet.add(3713, 3758);
        fBeginWordSet.add(3804, 3805);
        fBeginWordSet.add(3776, 3780);
        fMarkSet.compact();
        fEndWordSet.compact();
        fBeginWordSet.compact();
        fLaoWordSet.freeze();
        fMarkSet.freeze();
        fEndWordSet.freeze();
        fBeginWordSet.freeze();
    }

    public LaoBreakEngine() throws IOException {
        super(1, 2);
        setCharacters(fLaoWordSet);
        this.fDictionary = DictionaryData.loadDictionaryFor("Laoo");
    }

    public boolean equals(Object obj) {
        return obj instanceof LaoBreakEngine;
    }

    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean handles(int i, int i2) {
        return (i2 == 1 || i2 == 2) && UCharacter.getIntPropertyValue(i, UProperty.SCRIPT) == 24;
    }

    @Override
    public int divideUpDictionaryRange(CharacterIterator characterIterator, int i, int i2, DictionaryBreakEngine.DequeI dequeI) {
        int iAcceptMarked;
        if (i2 - i < 2) {
            return 0;
        }
        DictionaryBreakEngine.PossibleWord[] possibleWordArr = new DictionaryBreakEngine.PossibleWord[3];
        for (int i3 = 0; i3 < 3; i3++) {
            possibleWordArr[i3] = new DictionaryBreakEngine.PossibleWord();
        }
        characterIterator.setIndex(i);
        int i4 = 0;
        while (true) {
            int index = characterIterator.getIndex();
            if (index >= i2) {
                break;
            }
            int i5 = i4 % 3;
            int iCandidates = possibleWordArr[i5].candidates(characterIterator, this.fDictionary, i2);
            if (iCandidates == 1) {
                iAcceptMarked = possibleWordArr[i5].acceptMarked(characterIterator);
                i4++;
            } else if (iCandidates > 1) {
                if (characterIterator.getIndex() < i2) {
                    boolean z = false;
                    do {
                        int i6 = (i4 + 1) % 3;
                        if (possibleWordArr[i6].candidates(characterIterator, this.fDictionary, i2) > 0) {
                            possibleWordArr[i5].markCurrent();
                            if (characterIterator.getIndex() >= i2) {
                                break;
                            }
                            while (true) {
                                if (possibleWordArr[(i4 + 2) % 3].candidates(characterIterator, this.fDictionary, i2) > 0) {
                                    possibleWordArr[i5].markCurrent();
                                    z = true;
                                    break;
                                }
                                if (!possibleWordArr[i6].backUp(characterIterator)) {
                                    break;
                                }
                            }
                            if (possibleWordArr[i5].backUp(characterIterator)) {
                                break;
                            }
                        } else if (possibleWordArr[i5].backUp(characterIterator)) {
                        }
                    } while (!z);
                }
                iAcceptMarked = possibleWordArr[i5].acceptMarked(characterIterator);
                i4++;
            } else {
                iAcceptMarked = 0;
            }
            if (characterIterator.getIndex() < i2 && iAcceptMarked < 3) {
                int i7 = i4 % 3;
                if (possibleWordArr[i7].candidates(characterIterator, this.fDictionary, i2) <= 0 && (iAcceptMarked == 0 || possibleWordArr[i7].longestPrefix() < 3)) {
                    int i8 = index + iAcceptMarked;
                    char cCurrent = characterIterator.current();
                    int i9 = i2 - i8;
                    int i10 = 0;
                    while (true) {
                        characterIterator.next();
                        char cCurrent2 = characterIterator.current();
                        i10++;
                        i9--;
                        if (i9 <= 0) {
                            break;
                        }
                        if (fEndWordSet.contains(cCurrent) && fBeginWordSet.contains(cCurrent2)) {
                            int iCandidates2 = possibleWordArr[(i4 + 1) % 3].candidates(characterIterator, this.fDictionary, i2);
                            characterIterator.setIndex(i8 + i10);
                            if (iCandidates2 > 0) {
                                break;
                            }
                        }
                        cCurrent = cCurrent2;
                    }
                    if (iAcceptMarked <= 0) {
                        i4++;
                    }
                    iAcceptMarked += i10;
                } else {
                    characterIterator.setIndex(index + iAcceptMarked);
                }
            }
            while (true) {
                int index2 = characterIterator.getIndex();
                if (index2 >= i2 || !fMarkSet.contains(characterIterator.current())) {
                    break;
                }
                characterIterator.next();
                iAcceptMarked += characterIterator.getIndex() - index2;
            }
            if (iAcceptMarked > 0) {
                dequeI.push(Integer.valueOf(index + iAcceptMarked).intValue());
            }
        }
        if (dequeI.peek() >= i2) {
            dequeI.pop();
            return i4 - 1;
        }
        return i4;
    }
}
