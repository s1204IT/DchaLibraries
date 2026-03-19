package android.text;

public abstract class LoginFilter implements InputFilter {
    private boolean mAppendInvalid;

    public abstract boolean isAllowed(char c);

    LoginFilter(boolean z) {
        this.mAppendInvalid = z;
    }

    LoginFilter() {
        this.mAppendInvalid = false;
    }

    @Override
    public CharSequence filter(CharSequence charSequence, int i, int i2, Spanned spanned, int i3, int i4) {
        onStart();
        int i5 = 0;
        for (int i6 = 0; i6 < i3; i6++) {
            char cCharAt = spanned.charAt(i6);
            if (!isAllowed(cCharAt)) {
                onInvalidCharacter(cCharAt);
            }
        }
        SpannableStringBuilder spannableStringBuilder = null;
        for (int i7 = i; i7 < i2; i7++) {
            char cCharAt2 = charSequence.charAt(i7);
            if (isAllowed(cCharAt2)) {
                i5++;
            } else {
                if (this.mAppendInvalid) {
                    i5++;
                } else {
                    if (spannableStringBuilder == null) {
                        spannableStringBuilder = new SpannableStringBuilder(charSequence, i, i2);
                        i5 = i7 - i;
                    }
                    spannableStringBuilder.delete(i5, i5 + 1);
                }
                onInvalidCharacter(cCharAt2);
            }
        }
        while (i4 < spanned.length()) {
            char cCharAt3 = spanned.charAt(i4);
            if (!isAllowed(cCharAt3)) {
                onInvalidCharacter(cCharAt3);
            }
            i4++;
        }
        onStop();
        return spannableStringBuilder;
    }

    public void onStart() {
    }

    public void onInvalidCharacter(char c) {
    }

    public void onStop() {
    }

    public static class UsernameFilterGMail extends LoginFilter {
        public UsernameFilterGMail() {
            super(false);
        }

        public UsernameFilterGMail(boolean z) {
            super(z);
        }

        @Override
        public boolean isAllowed(char c) {
            if ('0' <= c && c <= '9') {
                return true;
            }
            if ('a' <= c && c <= 'z') {
                return true;
            }
            if (('A' <= c && c <= 'Z') || '.' == c) {
                return true;
            }
            return false;
        }
    }

    public static class UsernameFilterGeneric extends LoginFilter {
        private static final String mAllowed = "@_-+.";

        public UsernameFilterGeneric() {
            super(false);
        }

        public UsernameFilterGeneric(boolean z) {
            super(z);
        }

        @Override
        public boolean isAllowed(char c) {
            if ('0' <= c && c <= '9') {
                return true;
            }
            if ('a' <= c && c <= 'z') {
                return true;
            }
            if (('A' <= c && c <= 'Z') || mAllowed.indexOf(c) != -1) {
                return true;
            }
            return false;
        }
    }

    public static class PasswordFilterGMail extends LoginFilter {
        public PasswordFilterGMail() {
            super(false);
        }

        public PasswordFilterGMail(boolean z) {
            super(z);
        }

        @Override
        public boolean isAllowed(char c) {
            if (' ' <= c && c <= 127) {
                return true;
            }
            if (160 <= c && c <= 255) {
                return true;
            }
            return false;
        }
    }
}
