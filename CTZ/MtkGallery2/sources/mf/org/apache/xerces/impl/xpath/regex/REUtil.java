package mf.org.apache.xerces.impl.xpath.regex;

import com.mediatek.plugin.preload.SoOperater;
import java.text.CharacterIterator;

public final class REUtil {
    static final int CACHESIZE = 20;
    static final RegularExpression[] regexCache = new RegularExpression[20];

    private REUtil() {
    }

    static final int composeFromSurrogates(int high, int low) {
        return ((65536 + ((high - 55296) << 10)) + low) - 56320;
    }

    static final boolean isLowSurrogate(int ch) {
        return (64512 & ch) == 56320;
    }

    static final boolean isHighSurrogate(int ch) {
        return (64512 & ch) == 55296;
    }

    static final String decomposeToSurrogates(int ch) {
        int ch2 = ch - 65536;
        char[] chs = {(char) ((ch2 >> 10) + 55296), (char) ((ch2 & 1023) + 56320)};
        return new String(chs);
    }

    static final String substring(CharacterIterator iterator, int begin, int end) {
        char[] src = new char[end - begin];
        for (int i = 0; i < src.length; i++) {
            src[i] = iterator.setIndex(i + begin);
        }
        return new String(src);
    }

    static final int getOptionValue(int ch) {
        if (ch == 44) {
            return SoOperater.STEP;
        }
        if (ch == 70) {
            return 256;
        }
        if (ch == 72) {
            return 128;
        }
        if (ch == 88) {
            return 512;
        }
        if (ch == 105) {
            return 2;
        }
        if (ch == 109) {
            return 8;
        }
        if (ch == 115) {
            return 4;
        }
        if (ch == 117) {
            return 32;
        }
        switch (ch) {
            case 119:
                return 64;
            case 120:
                return 16;
            default:
                return 0;
        }
    }

    static final int parseOptions(String opts) throws ParseException {
        if (opts == null) {
            return 0;
        }
        int options = 0;
        for (int i = 0; i < opts.length(); i++) {
            int v = getOptionValue(opts.charAt(i));
            if (v == 0) {
                throw new ParseException("Unknown Option: " + opts.substring(i), -1);
            }
            options |= v;
        }
        return options;
    }

    static final String createOptionString(int options) {
        StringBuffer sb = new StringBuffer(9);
        if ((options & 256) != 0) {
            sb.append('F');
        }
        if ((options & 128) != 0) {
            sb.append('H');
        }
        if ((options & 512) != 0) {
            sb.append('X');
        }
        if ((options & 2) != 0) {
            sb.append('i');
        }
        if ((options & 8) != 0) {
            sb.append('m');
        }
        if ((options & 4) != 0) {
            sb.append('s');
        }
        if ((options & 32) != 0) {
            sb.append('u');
        }
        if ((options & 64) != 0) {
            sb.append('w');
        }
        if ((options & 16) != 0) {
            sb.append('x');
        }
        if ((options & SoOperater.STEP) != 0) {
            sb.append(',');
        }
        return sb.toString().intern();
    }

    static String stripExtendedComment(String regex) {
        int next;
        int len = regex.length();
        StringBuffer buffer = new StringBuffer(len);
        int ch = 0;
        int charClass = 0;
        while (ch < len) {
            int offset = ch + 1;
            int offset2 = regex.charAt(ch);
            if (offset2 == 9 || offset2 == 10 || offset2 == 12 || offset2 == 13 || offset2 == 32) {
                if (charClass > 0) {
                    buffer.append((char) offset2);
                }
                ch = offset;
            } else if (offset2 == 35) {
                ch = offset;
                while (ch < len) {
                    int offset3 = ch + 1;
                    int ch2 = regex.charAt(ch);
                    if (ch2 == 13 || ch2 == 10) {
                        ch = offset3;
                        break;
                    }
                    ch = offset3;
                }
            } else {
                if (offset2 == 92 && offset < len) {
                    int next2 = regex.charAt(offset);
                    if (next2 == 35 || next2 == 9 || next2 == 10 || next2 == 12 || next2 == 13 || next2 == 32) {
                        buffer.append((char) next2);
                        offset++;
                    } else {
                        buffer.append('\\');
                        buffer.append((char) next2);
                        offset++;
                    }
                } else if (offset2 == 91) {
                    charClass++;
                    buffer.append((char) offset2);
                    if (offset < len) {
                        int next3 = regex.charAt(offset);
                        if (next3 == 91 || next3 == 93) {
                            buffer.append((char) next3);
                            offset++;
                        } else if (next3 == 94 && offset + 1 < len && ((next = regex.charAt(offset + 1)) == 91 || next == 93)) {
                            buffer.append('^');
                            buffer.append((char) next);
                            offset += 2;
                        }
                    }
                } else {
                    if (charClass > 0 && offset2 == 93) {
                        charClass--;
                    }
                    buffer.append((char) offset2);
                }
                ch = offset;
            }
        }
        return buffer.toString();
    }

    public static void main(String[] argv) {
        String pattern = null;
        try {
            String options = "";
            String target = null;
            if (argv.length == 0) {
                System.out.println("Error:Usage: java REUtil -i|-m|-s|-u|-w|-X regularExpression String");
                System.exit(0);
            }
            for (int i = 0; i < argv.length; i++) {
                if (argv[i].length() == 0 || argv[i].charAt(0) != '-') {
                    if (pattern == null) {
                        pattern = argv[i];
                    } else if (target == null) {
                        target = argv[i];
                    } else {
                        System.err.println("Unnecessary: " + argv[i]);
                    }
                } else if (argv[i].equals("-i")) {
                    options = String.valueOf(options) + "i";
                } else if (argv[i].equals("-m")) {
                    options = String.valueOf(options) + "m";
                } else if (argv[i].equals("-s")) {
                    options = String.valueOf(options) + "s";
                } else if (argv[i].equals("-u")) {
                    options = String.valueOf(options) + "u";
                } else if (argv[i].equals("-w")) {
                    options = String.valueOf(options) + "w";
                } else if (argv[i].equals("-X")) {
                    options = String.valueOf(options) + "X";
                } else {
                    System.err.println("Unknown option: " + argv[i]);
                }
            }
            RegularExpression reg = new RegularExpression(pattern, options);
            System.out.println("RegularExpression: " + reg);
            Match match = new Match();
            reg.matches(target, match);
            for (int i2 = 0; i2 < match.getNumberOfGroups(); i2++) {
                if (i2 == 0) {
                    System.out.print("Matched range for the whole pattern: ");
                } else {
                    System.out.print("[" + i2 + "]: ");
                }
                if (match.getBeginning(i2) < 0) {
                    System.out.println("-1");
                } else {
                    System.out.print(String.valueOf(match.getBeginning(i2)) + ", " + match.getEnd(i2) + ", ");
                    System.out.println("\"" + match.getCapturedText(i2) + "\"");
                }
            }
        } catch (ParseException pe) {
            if (pattern == null) {
                pe.printStackTrace();
                return;
            }
            System.err.println("mf.org.apache.xerces.utils.regex.ParseException: " + pe.getMessage());
            System.err.println(String.valueOf("        ") + pattern);
            int loc = pe.getLocation();
            if (loc >= 0) {
                System.err.print("        ");
                for (int i3 = 0; i3 < loc; i3++) {
                    System.err.print("-");
                }
                System.err.println("^");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static RegularExpression createRegex(String pattern, String options) throws ParseException {
        RegularExpression re = null;
        int intOptions = parseOptions(options);
        synchronized (regexCache) {
            int i = 0;
            while (true) {
                if (i >= 20) {
                    break;
                }
                RegularExpression cached = regexCache[i];
                if (cached == null) {
                    i = -1;
                    break;
                }
                if (!cached.equals(pattern, intOptions)) {
                    i++;
                } else {
                    re = cached;
                    break;
                }
            }
            if (re != null) {
                if (i != 0) {
                    System.arraycopy(regexCache, 0, regexCache, 1, i);
                    regexCache[0] = re;
                }
            } else {
                re = new RegularExpression(pattern, options);
                System.arraycopy(regexCache, 0, regexCache, 1, 19);
                regexCache[0] = re;
            }
        }
        return re;
    }

    public static boolean matches(String regex, String target) throws ParseException {
        return createRegex(regex, null).matches(target);
    }

    public static boolean matches(String regex, String options, String target) throws ParseException {
        return createRegex(regex, options).matches(target);
    }

    public static String quoteMeta(String literal) {
        int len = literal.length();
        StringBuffer buffer = null;
        for (int i = 0; i < len; i++) {
            int ch = literal.charAt(i);
            if (".*+?{[()|\\^$".indexOf(ch) >= 0) {
                if (buffer == null) {
                    buffer = new StringBuffer(((len - i) * 2) + i);
                    if (i > 0) {
                        buffer.append(literal.substring(0, i));
                    }
                }
                buffer.append('\\');
                buffer.append((char) ch);
            } else if (buffer != null) {
                buffer.append((char) ch);
            }
        }
        return buffer != null ? buffer.toString() : literal;
    }

    static void dumpString(String v) {
        for (int i = 0; i < v.length(); i++) {
            System.out.print(Integer.toHexString(v.charAt(i)));
            System.out.print(" ");
        }
        System.out.println();
    }
}
