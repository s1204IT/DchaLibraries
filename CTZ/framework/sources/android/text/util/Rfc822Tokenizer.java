package android.text.util;

import android.widget.MultiAutoCompleteTextView;
import java.util.ArrayList;
import java.util.Collection;

public class Rfc822Tokenizer implements MultiAutoCompleteTextView.Tokenizer {
    public static void tokenize(CharSequence charSequence, Collection<Rfc822Token> collection) {
        StringBuilder sb = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        StringBuilder sb3 = new StringBuilder();
        int length = charSequence.length();
        int i = 0;
        while (i < length) {
            char cCharAt = charSequence.charAt(i);
            if (cCharAt == ',' || cCharAt == ';') {
                i++;
                while (i < length && charSequence.charAt(i) == ' ') {
                    i++;
                }
                crunch(sb);
                if (sb2.length() > 0) {
                    collection.add(new Rfc822Token(sb.toString(), sb2.toString(), sb3.toString()));
                } else if (sb.length() > 0) {
                    collection.add(new Rfc822Token(null, sb.toString(), sb3.toString()));
                }
                sb.setLength(0);
                sb2.setLength(0);
                sb3.setLength(0);
            } else if (cCharAt == '\"') {
                i++;
                while (true) {
                    if (i >= length) {
                        break;
                    }
                    char cCharAt2 = charSequence.charAt(i);
                    if (cCharAt2 == '\"') {
                        i++;
                        break;
                    } else if (cCharAt2 == '\\') {
                        int i2 = i + 1;
                        if (i2 < length) {
                            sb.append(charSequence.charAt(i2));
                        }
                        i += 2;
                    } else {
                        sb.append(cCharAt2);
                        i++;
                    }
                }
            } else if (cCharAt == '(') {
                i++;
                int i3 = 1;
                while (i < length && i3 > 0) {
                    char cCharAt3 = charSequence.charAt(i);
                    if (cCharAt3 == ')') {
                        if (i3 > 1) {
                            sb3.append(cCharAt3);
                        }
                        i3--;
                        i++;
                    } else if (cCharAt3 == '(') {
                        sb3.append(cCharAt3);
                        i3++;
                        i++;
                    } else if (cCharAt3 == '\\') {
                        int i4 = i + 1;
                        if (i4 < length) {
                            sb3.append(charSequence.charAt(i4));
                        }
                        i += 2;
                    } else {
                        sb3.append(cCharAt3);
                        i++;
                    }
                }
            } else if (cCharAt == '<') {
                i++;
                while (true) {
                    if (i >= length) {
                        break;
                    }
                    char cCharAt4 = charSequence.charAt(i);
                    if (cCharAt4 == '>') {
                        i++;
                        break;
                    } else {
                        sb2.append(cCharAt4);
                        i++;
                    }
                }
            } else if (cCharAt == ' ') {
                sb.append((char) 0);
                i++;
            } else {
                sb.append(cCharAt);
                i++;
            }
        }
        crunch(sb);
        if (sb2.length() > 0) {
            collection.add(new Rfc822Token(sb.toString(), sb2.toString(), sb3.toString()));
        } else if (sb.length() > 0) {
            collection.add(new Rfc822Token(null, sb.toString(), sb3.toString()));
        }
    }

    public static Rfc822Token[] tokenize(CharSequence charSequence) {
        ArrayList arrayList = new ArrayList();
        tokenize(charSequence, arrayList);
        return (Rfc822Token[]) arrayList.toArray(new Rfc822Token[arrayList.size()]);
    }

    private static void crunch(StringBuilder sb) {
        int length = sb.length();
        int i = 0;
        while (i < length) {
            if (sb.charAt(i) == 0) {
                if (i != 0 && i != length - 1) {
                    int i2 = i - 1;
                    if (sb.charAt(i2) != ' ' && sb.charAt(i2) != 0) {
                        int i3 = i + 1;
                        if (sb.charAt(i3) != ' ' && sb.charAt(i3) != 0) {
                            i = i3;
                        }
                    }
                }
                sb.deleteCharAt(i);
                length--;
            } else {
                i++;
            }
        }
        for (int i4 = 0; i4 < length; i4++) {
            if (sb.charAt(i4) == 0) {
                sb.setCharAt(i4, ' ');
            }
        }
    }

    @Override
    public int findTokenStart(CharSequence charSequence, int i) {
        int iFindTokenEnd = 0;
        while (true) {
            int i2 = iFindTokenEnd;
            while (iFindTokenEnd < i) {
                iFindTokenEnd = findTokenEnd(charSequence, iFindTokenEnd);
                if (iFindTokenEnd < i) {
                    iFindTokenEnd++;
                    while (iFindTokenEnd < i && charSequence.charAt(iFindTokenEnd) == ' ') {
                        iFindTokenEnd++;
                    }
                    if (iFindTokenEnd < i) {
                        break;
                    }
                }
            }
            return i2;
        }
    }

    @Override
    public int findTokenEnd(CharSequence charSequence, int i) {
        int length = charSequence.length();
        while (i < length) {
            char cCharAt = charSequence.charAt(i);
            if (cCharAt == ',' || cCharAt == ';') {
                return i;
            }
            if (cCharAt == '\"') {
                i++;
                while (true) {
                    if (i >= length) {
                        break;
                    }
                    char cCharAt2 = charSequence.charAt(i);
                    if (cCharAt2 == '\"') {
                        i++;
                        break;
                    }
                    if (cCharAt2 == '\\' && i + 1 < length) {
                        i += 2;
                    } else {
                        i++;
                    }
                }
            } else if (cCharAt == '(') {
                i++;
                int i2 = 1;
                while (i < length && i2 > 0) {
                    char cCharAt3 = charSequence.charAt(i);
                    if (cCharAt3 == ')') {
                        i2--;
                        i++;
                    } else if (cCharAt3 == '(') {
                        i2++;
                        i++;
                    } else if (cCharAt3 == '\\' && i + 1 < length) {
                        i += 2;
                    } else {
                        i++;
                    }
                }
            } else if (cCharAt == '<') {
                i++;
                while (true) {
                    if (i >= length) {
                        break;
                    }
                    if (charSequence.charAt(i) == '>') {
                        i++;
                        break;
                    }
                    i++;
                }
            } else {
                i++;
            }
        }
        return i;
    }

    @Override
    public CharSequence terminateToken(CharSequence charSequence) {
        return ((Object) charSequence) + ", ";
    }
}
