package android.text;

import android.net.ProxyInfo;

public interface InputFilter {
    CharSequence filter(CharSequence charSequence, int i, int i2, Spanned spanned, int i3, int i4);

    public static class AllCaps implements InputFilter {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            for (int i = start; i < end; i++) {
                if (Character.isLowerCase(source.charAt(i))) {
                    char[] v = new char[end - start];
                    TextUtils.getChars(source, start, end, v, 0);
                    String s = new String(v).toUpperCase();
                    if (!(source instanceof Spanned)) {
                        return s;
                    }
                    SpannableString sp = new SpannableString(s);
                    TextUtils.copySpansFrom((Spanned) source, start, end, null, sp, 0);
                    return sp;
                }
            }
            return null;
        }
    }

    public static class LengthFilter implements InputFilter {
        private final int mMax;

        public LengthFilter(int max) {
            this.mMax = max;
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            int keep = this.mMax - (dest.length() - (dend - dstart));
            if (keep <= 0) {
                return ProxyInfo.LOCAL_EXCL_LIST;
            }
            if (keep >= end - start) {
                return null;
            }
            int keep2 = keep + start;
            if (Character.isHighSurrogate(source.charAt(keep2 - 1)) && keep2 - 1 == start) {
                return ProxyInfo.LOCAL_EXCL_LIST;
            }
            return source.subSequence(start, keep2);
        }

        public int getMax() {
            return this.mMax;
        }
    }
}
