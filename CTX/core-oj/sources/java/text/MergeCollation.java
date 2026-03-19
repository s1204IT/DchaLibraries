package java.text;

import java.text.PatternEntry;
import java.util.ArrayList;

final class MergeCollation {
    ArrayList<PatternEntry> patterns = new ArrayList<>();
    private transient PatternEntry saveEntry = null;
    private transient PatternEntry lastEntry = null;
    private transient StringBuffer excess = new StringBuffer();
    private transient byte[] statusArray = new byte[8192];
    private final byte BITARRAYMASK = 1;
    private final int BYTEPOWER = 3;
    private final int BYTEMASK = 7;

    public MergeCollation(String str) throws ParseException {
        for (int i = 0; i < this.statusArray.length; i++) {
            this.statusArray[i] = 0;
        }
        setPattern(str);
    }

    public String getPattern() {
        return getPattern(true);
    }

    public String getPattern(boolean z) {
        StringBuffer stringBuffer = new StringBuffer();
        ArrayList arrayList = null;
        int i = 0;
        while (i < this.patterns.size()) {
            PatternEntry patternEntry = this.patterns.get(i);
            if (patternEntry.extension.length() != 0) {
                if (arrayList == null) {
                    arrayList = new ArrayList();
                }
                arrayList.add(patternEntry);
            } else {
                if (arrayList != null) {
                    PatternEntry patternEntryFindLastWithNoExtension = findLastWithNoExtension(i - 1);
                    for (int size = arrayList.size() - 1; size >= 0; size--) {
                        ((PatternEntry) arrayList.get(size)).addToBuffer(stringBuffer, false, z, patternEntryFindLastWithNoExtension);
                    }
                    arrayList = null;
                }
                patternEntry.addToBuffer(stringBuffer, false, z, null);
            }
            i++;
        }
        if (arrayList != null) {
            PatternEntry patternEntryFindLastWithNoExtension2 = findLastWithNoExtension(i - 1);
            for (int size2 = arrayList.size() - 1; size2 >= 0; size2--) {
                ((PatternEntry) arrayList.get(size2)).addToBuffer(stringBuffer, false, z, patternEntryFindLastWithNoExtension2);
            }
        }
        return stringBuffer.toString();
    }

    private final PatternEntry findLastWithNoExtension(int i) {
        PatternEntry patternEntry;
        do {
            i--;
            if (i >= 0) {
                patternEntry = this.patterns.get(i);
            } else {
                return null;
            }
        } while (patternEntry.extension.length() != 0);
        return patternEntry;
    }

    public String emitPattern() {
        return emitPattern(true);
    }

    public String emitPattern(boolean z) {
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < this.patterns.size(); i++) {
            PatternEntry patternEntry = this.patterns.get(i);
            if (patternEntry != null) {
                patternEntry.addToBuffer(stringBuffer, true, z, null);
            }
        }
        return stringBuffer.toString();
    }

    public void setPattern(String str) throws ParseException {
        this.patterns.clear();
        addPattern(str);
    }

    public void addPattern(String str) throws ParseException {
        if (str == null) {
            return;
        }
        PatternEntry.Parser parser = new PatternEntry.Parser(str);
        for (PatternEntry next = parser.next(); next != null; next = parser.next()) {
            fixEntry(next);
        }
    }

    public int getCount() {
        return this.patterns.size();
    }

    public PatternEntry getItemAt(int i) {
        return this.patterns.get(i);
    }

    private final void fixEntry(PatternEntry patternEntry) throws ParseException {
        int iLastIndexOf;
        if (this.lastEntry != null && patternEntry.chars.equals(this.lastEntry.chars) && patternEntry.extension.equals(this.lastEntry.extension)) {
            if (patternEntry.strength != 3 && patternEntry.strength != -2) {
                throw new ParseException("The entries " + ((Object) this.lastEntry) + " and " + ((Object) patternEntry) + " are adjacent in the rules, but have conflicting strengths: A character can't be unequal to itself.", -1);
            }
            return;
        }
        boolean z = true;
        if (patternEntry.strength != -2) {
            if (patternEntry.chars.length() == 1) {
                char cCharAt = patternEntry.chars.charAt(0);
                int i = cCharAt >> 3;
                byte b = this.statusArray[i];
                byte b2 = (byte) (1 << (cCharAt & 7));
                if (b != 0 && (b & b2) != 0) {
                    iLastIndexOf = this.patterns.lastIndexOf(patternEntry);
                } else {
                    this.statusArray[i] = (byte) (b2 | b);
                    iLastIndexOf = -1;
                }
            } else {
                iLastIndexOf = this.patterns.lastIndexOf(patternEntry);
            }
            if (iLastIndexOf != -1) {
                this.patterns.remove(iLastIndexOf);
            }
            this.excess.setLength(0);
            int iFindLastEntry = findLastEntry(this.lastEntry, this.excess);
            if (this.excess.length() != 0) {
                patternEntry.extension = ((Object) this.excess) + patternEntry.extension;
                if (iFindLastEntry != this.patterns.size()) {
                    this.lastEntry = this.saveEntry;
                    z = false;
                }
            }
            if (iFindLastEntry == this.patterns.size()) {
                this.patterns.add(patternEntry);
                this.saveEntry = patternEntry;
            } else {
                this.patterns.add(iFindLastEntry, patternEntry);
            }
        }
        if (z) {
            this.lastEntry = patternEntry;
        }
    }

    private final int findLastEntry(PatternEntry patternEntry, StringBuffer stringBuffer) throws ParseException {
        int iLastIndexOf;
        if (patternEntry == null) {
            return 0;
        }
        if (patternEntry.strength != -2) {
            if (patternEntry.chars.length() == 1) {
                if ((this.statusArray[patternEntry.chars.charAt(0) >> 3] & (1 << (patternEntry.chars.charAt(0) & 7))) != 0) {
                    iLastIndexOf = this.patterns.lastIndexOf(patternEntry);
                } else {
                    iLastIndexOf = -1;
                }
            } else {
                iLastIndexOf = this.patterns.lastIndexOf(patternEntry);
            }
            if (iLastIndexOf == -1) {
                throw new ParseException("couldn't find last entry: " + ((Object) patternEntry), iLastIndexOf);
            }
            return iLastIndexOf + 1;
        }
        int size = this.patterns.size() - 1;
        while (true) {
            if (size < 0) {
                break;
            }
            PatternEntry patternEntry2 = this.patterns.get(size);
            if (!patternEntry2.chars.regionMatches(0, patternEntry.chars, 0, patternEntry2.chars.length())) {
                size--;
            } else {
                stringBuffer.append(patternEntry.chars.substring(patternEntry2.chars.length(), patternEntry.chars.length()));
                break;
            }
        }
        if (size == -1) {
            throw new ParseException("couldn't find: " + ((Object) patternEntry), size);
        }
        return size + 1;
    }
}
