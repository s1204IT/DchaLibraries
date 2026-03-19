package com.android.providers.contacts.sqlite;

import android.util.ArraySet;
import android.util.Log;
import com.android.providers.contacts.AbstractContactsProvider;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class SqlChecker {
    static final int OPTION_NONE = 0;
    static final int OPTION_TOKEN_ONLY = 1;
    private static final boolean VERBOSE_LOGGING = AbstractContactsProvider.VERBOSE_LOGGING;
    private final ArraySet<String> mInvalidTokens;

    public SqlChecker(List<String> list) {
        this.mInvalidTokens = new ArraySet<>(list.size());
        for (int size = list.size() - 1; size >= 0; size--) {
            this.mInvalidTokens.add(list.get(size).toLowerCase());
        }
        if (VERBOSE_LOGGING) {
            Log.d("SqlChecker", "Initialized with invalid tokens: " + list);
        }
    }

    private static boolean isAlpha(char c) {
        return ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z') || c == '_';
    }

    private static boolean isNum(char c) {
        return '0' <= c && c <= '9';
    }

    private static boolean isAlNum(char c) {
        return isAlpha(c) || isNum(c);
    }

    private static boolean isAnyOf(char c, String str) {
        return str.indexOf(c) >= 0;
    }

    public static final class InvalidSqlException extends IllegalArgumentException {
        public InvalidSqlException(String str) {
            super(str);
        }
    }

    private static InvalidSqlException genException(String str, String str2) {
        throw new InvalidSqlException(str + " in '" + str2 + "'");
    }

    private void throwIfContainsToken(String str, String str2) {
        String lowerCase = str.toLowerCase();
        if (this.mInvalidTokens.contains(lowerCase) || lowerCase.startsWith("x_")) {
            throw genException("Detected disallowed token: " + str, str2);
        }
    }

    public void ensureNoInvalidTokens(final String str) {
        findTokens(str, OPTION_NONE, new Consumer() {
            @Override
            public final void accept(Object obj) {
                this.f$0.throwIfContainsToken((String) obj, str);
            }
        });
    }

    public void ensureSingleTokenOnly(final String str) {
        final AtomicBoolean atomicBoolean = new AtomicBoolean();
        findTokens(str, OPTION_TOKEN_ONLY, new Consumer() {
            @Override
            public final void accept(Object obj) {
                SqlChecker.lambda$ensureSingleTokenOnly$1(this.f$0, atomicBoolean, str, (String) obj);
            }
        });
        if (!atomicBoolean.get()) {
            throw genException("Token not found", str);
        }
    }

    public static void lambda$ensureSingleTokenOnly$1(SqlChecker sqlChecker, AtomicBoolean atomicBoolean, String str, String str2) {
        if (atomicBoolean.get()) {
            throw genException("Multiple tokens detected", str);
        }
        atomicBoolean.set(true);
        sqlChecker.throwIfContainsToken(str2, str);
    }

    private static char peek(String str, int i) {
        if (i < str.length()) {
            return str.charAt(i);
        }
        return (char) 0;
    }

    static void findTokens(String str, int i, Consumer<String> consumer) {
        if (str == null) {
            return;
        }
        int i2 = OPTION_NONE;
        int length = str.length();
        while (i2 < length) {
            char cPeek = peek(str, i2);
            if (isAlpha(cPeek)) {
                int i3 = i2 + OPTION_TOKEN_ONLY;
                while (isAlNum(peek(str, i3))) {
                    i3 += OPTION_TOKEN_ONLY;
                }
                consumer.accept(str.substring(i2, i3));
                i2 = i3;
            } else if (isAnyOf(cPeek, "'\"`")) {
                int i4 = i2 + OPTION_TOKEN_ONLY;
                int i5 = i4;
                while (true) {
                    int iIndexOf = str.indexOf(cPeek, i5);
                    if (iIndexOf < 0) {
                        throw genException("Unterminated quote", str);
                    }
                    int i6 = iIndexOf + OPTION_TOKEN_ONLY;
                    if (peek(str, i6) != cPeek) {
                        if (cPeek != '\'') {
                            String strSubstring = str.substring(i4, iIndexOf);
                            if (strSubstring.indexOf(cPeek) >= 0) {
                                strSubstring = strSubstring.replaceAll(String.valueOf(cPeek) + cPeek, String.valueOf(cPeek));
                            }
                            consumer.accept(strSubstring);
                        } else {
                            i &= OPTION_TOKEN_ONLY;
                            if (i != 0) {
                                throw genException("Non-token detected", str);
                            }
                        }
                        i2 = i6;
                    } else {
                        i5 = iIndexOf + 2;
                    }
                }
            } else if (cPeek == '[') {
                int i7 = i2 + OPTION_TOKEN_ONLY;
                int iIndexOf2 = str.indexOf(93, i7);
                if (iIndexOf2 < 0) {
                    throw genException("Unterminated quote", str);
                }
                int i8 = iIndexOf2 + OPTION_TOKEN_ONLY;
                consumer.accept(str.substring(i7, iIndexOf2));
                i2 = i8;
            } else {
                i &= OPTION_TOKEN_ONLY;
                if (i != 0) {
                    throw genException("Non-token detected", str);
                }
                if (cPeek == '-' && peek(str, i2 + OPTION_TOKEN_ONLY) == '-') {
                    int iIndexOf3 = str.indexOf(10, i2 + 2);
                    if (iIndexOf3 < 0) {
                        throw genException("Unterminated comment", str);
                    }
                    i2 = iIndexOf3 + OPTION_TOKEN_ONLY;
                } else if (cPeek == '/' && peek(str, i2 + OPTION_TOKEN_ONLY) == '*') {
                    int iIndexOf4 = str.indexOf("*/", i2 + 2);
                    if (iIndexOf4 < 0) {
                        throw genException("Unterminated comment", str);
                    }
                    i2 = iIndexOf4 + 2;
                } else {
                    if (cPeek == ';') {
                        throw genException("Semicolon is not allowed", str);
                    }
                    i2 += OPTION_TOKEN_ONLY;
                }
            }
        }
    }
}
