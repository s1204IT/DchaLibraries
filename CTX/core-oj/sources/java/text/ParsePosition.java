package java.text;

public class ParsePosition {
    int errorIndex = -1;
    int index;

    public int getIndex() {
        return this.index;
    }

    public void setIndex(int i) {
        this.index = i;
    }

    public ParsePosition(int i) {
        this.index = 0;
        this.index = i;
    }

    public void setErrorIndex(int i) {
        this.errorIndex = i;
    }

    public int getErrorIndex() {
        return this.errorIndex;
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof ParsePosition)) {
            return false;
        }
        ParsePosition parsePosition = (ParsePosition) obj;
        return this.index == parsePosition.index && this.errorIndex == parsePosition.errorIndex;
    }

    public int hashCode() {
        return (this.errorIndex << 16) | this.index;
    }

    public String toString() {
        return getClass().getName() + "[index=" + this.index + ",errorIndex=" + this.errorIndex + ']';
    }
}
