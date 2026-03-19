package android.icu.text;

import android.icu.impl.PatternTokenizer;
import android.icu.impl.Utility;
import android.icu.text.Transliterator;
import android.icu.util.ULocale;

class UnescapeTransliterator extends Transliterator {
    private static final char END = 65535;
    private char[] spec;

    static void register() {
        Transliterator.registerFactory("Hex-Any/Unicode", new Transliterator.Factory() {
            @Override
            public Transliterator getInstance(String ID) {
                return new UnescapeTransliterator("Hex-Any/Unicode", new char[]{2, 0, 16, 4, 6, 'U', '+', 65535});
            }
        });
        Transliterator.registerFactory("Hex-Any/Java", new Transliterator.Factory() {
            @Override
            public Transliterator getInstance(String ID) {
                return new UnescapeTransliterator("Hex-Any/Java", new char[]{2, 0, 16, 4, 4, PatternTokenizer.BACK_SLASH, 'u', 65535});
            }
        });
        Transliterator.registerFactory("Hex-Any/C", new Transliterator.Factory() {
            @Override
            public Transliterator getInstance(String ID) {
                return new UnescapeTransliterator("Hex-Any/C", new char[]{2, 0, 16, 4, 4, PatternTokenizer.BACK_SLASH, 'u', 2, 0, 16, '\b', '\b', PatternTokenizer.BACK_SLASH, 'U', 65535});
            }
        });
        Transliterator.registerFactory("Hex-Any/XML", new Transliterator.Factory() {
            @Override
            public Transliterator getInstance(String ID) {
                return new UnescapeTransliterator("Hex-Any/XML", new char[]{3, 1, 16, 1, 6, '&', '#', ULocale.PRIVATE_USE_EXTENSION, ';', 65535});
            }
        });
        Transliterator.registerFactory("Hex-Any/XML10", new Transliterator.Factory() {
            @Override
            public Transliterator getInstance(String ID) {
                return new UnescapeTransliterator("Hex-Any/XML10", new char[]{2, 1, '\n', 1, 7, '&', '#', ';', 65535});
            }
        });
        Transliterator.registerFactory("Hex-Any/Perl", new Transliterator.Factory() {
            @Override
            public Transliterator getInstance(String ID) {
                return new UnescapeTransliterator("Hex-Any/Perl", new char[]{3, 1, 16, 1, 6, PatternTokenizer.BACK_SLASH, ULocale.PRIVATE_USE_EXTENSION, '{', '}', 65535});
            }
        });
        Transliterator.registerFactory("Hex-Any", new Transliterator.Factory() {
            @Override
            public Transliterator getInstance(String ID) {
                return new UnescapeTransliterator("Hex-Any", new char[]{2, 0, 16, 4, 6, 'U', '+', 2, 0, 16, 4, 4, PatternTokenizer.BACK_SLASH, 'u', 2, 0, 16, '\b', '\b', PatternTokenizer.BACK_SLASH, 'U', 3, 1, 16, 1, 6, '&', '#', ULocale.PRIVATE_USE_EXTENSION, ';', 2, 1, '\n', 1, 7, '&', '#', ';', 3, 1, 16, 1, 6, PatternTokenizer.BACK_SLASH, ULocale.PRIVATE_USE_EXTENSION, '{', '}', 65535});
            }
        });
    }

    UnescapeTransliterator(String ID, char[] spec) {
        super(ID, null);
        this.spec = spec;
    }

    @Override
    protected void handleTransliterate(Replaceable text, Transliterator.Position pos, boolean isIncremental) {
        char c;
        char c2;
        int ipat;
        int s;
        int start = pos.start;
        int limit = pos.limit;
        loop0: while (start < limit) {
            int ipat2 = 0;
            while (true) {
                if (this.spec[ipat2] == 65535) {
                    break;
                }
                int ipat3 = ipat2 + 1;
                c = this.spec[ipat2];
                int ipat4 = ipat3 + 1;
                c2 = this.spec[ipat3];
                int ipat5 = ipat4 + 1;
                char c3 = this.spec[ipat4];
                int ipat6 = ipat5 + 1;
                char c4 = this.spec[ipat5];
                ipat = ipat6 + 1;
                char c5 = this.spec[ipat6];
                int s2 = start;
                boolean match = true;
                int i = 0;
                int s3 = s2;
                while (true) {
                    if (i >= c) {
                        s = s3;
                        break;
                    }
                    if (s3 >= limit && i > 0) {
                        if (isIncremental) {
                            break loop0;
                        }
                        match = false;
                        s = s3;
                    } else {
                        s = s3 + 1;
                        char c6 = text.charAt(s3);
                        if (c6 == this.spec[ipat + i]) {
                            i++;
                            s3 = s;
                        } else {
                            match = false;
                            break;
                        }
                    }
                }
                ipat2 = ipat + c + c2;
            }
            if (start < limit) {
                start += UTF16.getCharCount(text.char32At(start));
            }
        }
        pos.contextLimit += limit - pos.limit;
        pos.limit = limit;
        pos.start = start;
    }

    @Override
    public void addSourceTargetSet(UnicodeSet inputFilter, UnicodeSet sourceSet, UnicodeSet targetSet) {
        UnicodeSet myFilter = getFilterAsUnicodeSet(inputFilter);
        UnicodeSet items = new UnicodeSet();
        StringBuilder buffer = new StringBuilder();
        int i = 0;
        while (this.spec[i] != 65535) {
            int end = this.spec[i] + i + this.spec[i + 1] + 5;
            char c = this.spec[i + 2];
            for (int j = 0; j < c; j++) {
                Utility.appendNumber(buffer, j, c, 0);
            }
            for (int j2 = i + 5; j2 < end; j2++) {
                items.add(this.spec[j2]);
            }
            i = end;
        }
        items.addAll(buffer.toString());
        items.retainAll(myFilter);
        if (items.size() <= 0) {
            return;
        }
        sourceSet.addAll(items);
        targetSet.addAll(0, 1114111);
    }
}
