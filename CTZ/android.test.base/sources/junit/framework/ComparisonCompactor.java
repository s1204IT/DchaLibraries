package junit.framework;

public class ComparisonCompactor {
    private static final String DELTA_END = "]";
    private static final String DELTA_START = "[";
    private static final String ELLIPSIS = "...";
    private String fActual;
    private int fContextLength;
    private String fExpected;
    private int fPrefix;
    private int fSuffix;

    public ComparisonCompactor(int i, String str, String str2) {
        this.fContextLength = i;
        this.fExpected = str;
        this.fActual = str2;
    }

    public String compact(String str) {
        if (this.fExpected == null || this.fActual == null || areStringsEqual()) {
            return format(str, this.fExpected, this.fActual);
        }
        findCommonPrefix();
        findCommonSuffix();
        return format(str, compactString(this.fExpected), compactString(this.fActual));
    }

    private String compactString(String str) {
        String str2 = DELTA_START + str.substring(this.fPrefix, (str.length() - this.fSuffix) + 1) + DELTA_END;
        if (this.fPrefix > 0) {
            str2 = computeCommonPrefix() + str2;
        }
        if (this.fSuffix > 0) {
            return str2 + computeCommonSuffix();
        }
        return str2;
    }

    private void findCommonPrefix() {
        this.fPrefix = 0;
        int iMin = Math.min(this.fExpected.length(), this.fActual.length());
        while (this.fPrefix < iMin && this.fExpected.charAt(this.fPrefix) == this.fActual.charAt(this.fPrefix)) {
            this.fPrefix++;
        }
    }

    private void findCommonSuffix() {
        int length = this.fExpected.length() - 1;
        int length2 = this.fActual.length() - 1;
        while (length2 >= this.fPrefix && length >= this.fPrefix && this.fExpected.charAt(length) == this.fActual.charAt(length2)) {
            length2--;
            length--;
        }
        this.fSuffix = this.fExpected.length() - length;
    }

    private String computeCommonPrefix() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.fPrefix > this.fContextLength ? ELLIPSIS : "");
        sb.append(this.fExpected.substring(Math.max(0, this.fPrefix - this.fContextLength), this.fPrefix));
        return sb.toString();
    }

    private String computeCommonSuffix() {
        int iMin = Math.min((this.fExpected.length() - this.fSuffix) + 1 + this.fContextLength, this.fExpected.length());
        StringBuilder sb = new StringBuilder();
        sb.append(this.fExpected.substring((this.fExpected.length() - this.fSuffix) + 1, iMin));
        sb.append((this.fExpected.length() - this.fSuffix) + 1 < this.fExpected.length() - this.fContextLength ? ELLIPSIS : "");
        return sb.toString();
    }

    private boolean areStringsEqual() {
        return this.fExpected.equals(this.fActual);
    }

    private static String format(String str, Object obj, Object obj2) {
        String str2 = "";
        if (str != null && str.length() > 0) {
            str2 = str + " ";
        }
        return str2 + "expected:<" + obj + "> but was:<" + obj2 + ">";
    }
}
