package org.junit;

public class ComparisonFailure extends AssertionError {
    private static final int MAX_CONTEXT_LENGTH = 20;
    private static final long serialVersionUID = 1;
    private String fActual;
    private String fExpected;

    public ComparisonFailure(String str, String str2, String str3) {
        super(str);
        this.fExpected = str2;
        this.fActual = str3;
    }

    @Override
    public String getMessage() {
        return new ComparisonCompactor(MAX_CONTEXT_LENGTH, this.fExpected, this.fActual).compact(super.getMessage());
    }

    public String getActual() {
        return this.fActual;
    }

    public String getExpected() {
        return this.fExpected;
    }

    private static class ComparisonCompactor {
        private static final String DIFF_END = "]";
        private static final String DIFF_START = "[";
        private static final String ELLIPSIS = "...";
        private final String actual;
        private final int contextLength;
        private final String expected;

        public ComparisonCompactor(int i, String str, String str2) {
            this.contextLength = i;
            this.expected = str;
            this.actual = str2;
        }

        public String compact(String str) {
            if (this.expected == null || this.actual == null || this.expected.equals(this.actual)) {
                return Assert.format(str, this.expected, this.actual);
            }
            DiffExtractor diffExtractor = new DiffExtractor();
            String strCompactPrefix = diffExtractor.compactPrefix();
            String strCompactSuffix = diffExtractor.compactSuffix();
            return Assert.format(str, strCompactPrefix + diffExtractor.expectedDiff() + strCompactSuffix, strCompactPrefix + diffExtractor.actualDiff() + strCompactSuffix);
        }

        private String sharedPrefix() {
            int iMin = Math.min(this.expected.length(), this.actual.length());
            for (int i = 0; i < iMin; i++) {
                if (this.expected.charAt(i) != this.actual.charAt(i)) {
                    return this.expected.substring(0, i);
                }
            }
            return this.expected.substring(0, iMin);
        }

        private String sharedSuffix(String str) {
            int iMin = Math.min(this.expected.length() - str.length(), this.actual.length() - str.length()) - 1;
            int i = 0;
            while (i <= iMin && this.expected.charAt((this.expected.length() - 1) - i) == this.actual.charAt((this.actual.length() - 1) - i)) {
                i++;
            }
            return this.expected.substring(this.expected.length() - i);
        }

        private class DiffExtractor {
            private final String sharedPrefix;
            private final String sharedSuffix;

            private DiffExtractor() {
                this.sharedPrefix = ComparisonCompactor.this.sharedPrefix();
                this.sharedSuffix = ComparisonCompactor.this.sharedSuffix(this.sharedPrefix);
            }

            public String expectedDiff() {
                return extractDiff(ComparisonCompactor.this.expected);
            }

            public String actualDiff() {
                return extractDiff(ComparisonCompactor.this.actual);
            }

            public String compactPrefix() {
                if (this.sharedPrefix.length() <= ComparisonCompactor.this.contextLength) {
                    return this.sharedPrefix;
                }
                return ComparisonCompactor.ELLIPSIS + this.sharedPrefix.substring(this.sharedPrefix.length() - ComparisonCompactor.this.contextLength);
            }

            public String compactSuffix() {
                if (this.sharedSuffix.length() <= ComparisonCompactor.this.contextLength) {
                    return this.sharedSuffix;
                }
                return this.sharedSuffix.substring(0, ComparisonCompactor.this.contextLength) + ComparisonCompactor.ELLIPSIS;
            }

            private String extractDiff(String str) {
                return ComparisonCompactor.DIFF_START + str.substring(this.sharedPrefix.length(), str.length() - this.sharedSuffix.length()) + ComparisonCompactor.DIFF_END;
            }
        }
    }
}
