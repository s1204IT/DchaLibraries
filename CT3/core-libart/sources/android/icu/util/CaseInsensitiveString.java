package android.icu.util;

import android.icu.lang.UCharacter;

public class CaseInsensitiveString {
    private String string;
    private int hash = 0;
    private String folded = null;

    private static String foldCase(String foldee) {
        return UCharacter.foldCase(foldee, true);
    }

    private void getFolded() {
        if (this.folded != null) {
            return;
        }
        this.folded = foldCase(this.string);
    }

    public CaseInsensitiveString(String s) {
        this.string = s;
    }

    public String getString() {
        return this.string;
    }

    public boolean equals(Object obj) {
        if (obj == 0) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CaseInsensitiveString)) {
            return false;
        }
        getFolded();
        obj.getFolded();
        return this.folded.equals(obj.folded);
    }

    public int hashCode() {
        getFolded();
        if (this.hash == 0) {
            this.hash = this.folded.hashCode();
        }
        return this.hash;
    }

    public String toString() {
        return this.string;
    }
}
