package android.database.sqlite;

import android.app.slice.Slice;
import android.bluetooth.BluetoothHidDevice;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiScanner;
import android.security.Credentials;
import com.android.internal.midi.MidiConstants;
import com.android.internal.telephony.GsmAlphabet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

public class SQLiteTokenizer {
    public static final int OPTION_NONE = 0;
    public static final int OPTION_TOKEN_ONLY = 1;

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

    private static IllegalArgumentException genException(String str, String str2) {
        throw new IllegalArgumentException(str + " in '" + str2 + "'");
    }

    private static char peek(String str, int i) {
        if (i < str.length()) {
            return str.charAt(i);
        }
        return (char) 0;
    }

    public static List<String> tokenize(String str, int i) {
        final ArrayList arrayList = new ArrayList();
        Objects.requireNonNull(arrayList);
        tokenize(str, i, new Consumer() {
            @Override
            public final void accept(Object obj) {
                arrayList.add((String) obj);
            }
        });
        return arrayList;
    }

    public static void tokenize(String str, int i, Consumer<String> consumer) {
        if (str == null) {
            return;
        }
        int i2 = 0;
        int length = str.length();
        while (i2 < length) {
            char cPeek = peek(str, i2);
            if (isAlpha(cPeek)) {
                int i3 = i2 + 1;
                while (isAlNum(peek(str, i3))) {
                    i3++;
                }
                consumer.accept(str.substring(i2, i3));
                i2 = i3;
            } else if (isAnyOf(cPeek, "'\"`")) {
                int i4 = i2 + 1;
                int i5 = i4;
                while (true) {
                    int iIndexOf = str.indexOf(cPeek, i5);
                    if (iIndexOf < 0) {
                        throw genException("Unterminated quote", str);
                    }
                    int i6 = iIndexOf + 1;
                    if (peek(str, i6) != cPeek) {
                        if (cPeek != '\'') {
                            String strSubstring = str.substring(i4, iIndexOf);
                            if (strSubstring.indexOf(cPeek) >= 0) {
                                strSubstring = strSubstring.replaceAll(String.valueOf(cPeek) + cPeek, String.valueOf(cPeek));
                            }
                            consumer.accept(strSubstring);
                        } else {
                            i &= 1;
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
                int i7 = i2 + 1;
                int iIndexOf2 = str.indexOf(93, i7);
                if (iIndexOf2 < 0) {
                    throw genException("Unterminated quote", str);
                }
                consumer.accept(str.substring(i7, iIndexOf2));
                i2 = iIndexOf2 + 1;
            } else {
                i &= 1;
                if (i != 0) {
                    throw genException("Non-token detected", str);
                }
                if (cPeek == '-' && peek(str, i2 + 1) == '-') {
                    int iIndexOf3 = str.indexOf(10, i2 + 2);
                    if (iIndexOf3 < 0) {
                        throw genException("Unterminated comment", str);
                    }
                    i2 = iIndexOf3 + 1;
                } else if (cPeek == '/' && peek(str, i2 + 1) == '*') {
                    int iIndexOf4 = str.indexOf("*/", i2 + 2);
                    if (iIndexOf4 < 0) {
                        throw genException("Unterminated comment", str);
                    }
                    i2 = iIndexOf4 + 2;
                } else {
                    if (cPeek == ';') {
                        throw genException("Semicolon is not allowed", str);
                    }
                    i2++;
                }
            }
        }
    }

    public static boolean isKeyword(String str) {
        byte b;
        String upperCase = str.toUpperCase(Locale.US);
        switch (upperCase.hashCode()) {
            case -2137067054:
                b = !upperCase.equals("IGNORE") ? (byte) -1 : (byte) 63;
                break;
            case -2130463047:
                if (upperCase.equals("INSERT")) {
                    b = 70;
                    break;
                }
                break;
            case -2125979215:
                if (upperCase.equals("ISNULL")) {
                    b = 75;
                    break;
                }
                break;
            case -2032180703:
                if (upperCase.equals("DEFAULT")) {
                    b = 33;
                    break;
                }
                break;
            case -1986874255:
                if (upperCase.equals("NOCASE")) {
                    b = 84;
                    break;
                }
                break;
            case -1966450541:
                if (upperCase.equals("OFFSET")) {
                    b = 90;
                    break;
                }
                break;
            case -1953474717:
                if (upperCase.equals("OTHERS")) {
                    b = 94;
                    break;
                }
                break;
            case -1926899396:
                if (upperCase.equals("PRAGMA")) {
                    b = 99;
                    break;
                }
                break;
            case -1881469687:
                if (upperCase.equals("REGEXP")) {
                    b = 107;
                    break;
                }
                break;
            case -1881265346:
                if (upperCase.equals("RENAME")) {
                    b = 110;
                    break;
                }
                break;
            case -1852692228:
                if (upperCase.equals("SELECT")) {
                    b = 119;
                    break;
                }
                break;
            case -1848073207:
                if (upperCase.equals("NATURAL")) {
                    b = 82;
                    break;
                }
                break;
            case -1787199535:
                if (upperCase.equals("UNIQUE")) {
                    b = 131;
                    break;
                }
                break;
            case -1785516855:
                if (upperCase.equals("UPDATE")) {
                    b = 132;
                    break;
                }
                break;
            case -1770751051:
                if (upperCase.equals("VACUUM")) {
                    b = 134;
                    break;
                }
                break;
            case -1770483422:
                if (upperCase.equals("VALUES")) {
                    b = 135;
                    break;
                }
                break;
            case -1757367375:
                if (upperCase.equals("INITIALLY")) {
                    b = 68;
                    break;
                }
                break;
            case -1734422544:
                if (upperCase.equals("WINDOW")) {
                    b = 140;
                    break;
                }
                break;
            case -1722875525:
                if (upperCase.equals("DATABASE")) {
                    b = 32;
                    break;
                }
                break;
            case -1633692463:
                if (upperCase.equals("INDEXED")) {
                    b = 67;
                    break;
                }
                break;
            case -1619411166:
                if (upperCase.equals("INSTEAD")) {
                    b = 71;
                    break;
                }
                break;
            case -1447660627:
                if (upperCase.equals("NOTHING")) {
                    b = 86;
                    break;
                }
                break;
            case -1447470406:
                if (upperCase.equals("NOTNULL")) {
                    b = 87;
                    break;
                }
                break;
            case -1322009984:
                if (upperCase.equals("AUTOINCREMENT")) {
                    b = 11;
                    break;
                }
                break;
            case -1308685805:
                if (upperCase.equals("SAVEPOINT")) {
                    b = 118;
                    break;
                }
                break;
            case -1005357825:
                if (upperCase.equals("INTERSECT")) {
                    b = 72;
                    break;
                }
                break;
            case -742456719:
                if (upperCase.equals("FOLLOWING")) {
                    b = 53;
                    break;
                }
                break;
            case -603166278:
                if (upperCase.equals("EXCLUDE")) {
                    b = 47;
                    break;
                }
                break;
            case -591179561:
                if (upperCase.equals("EXPLAIN")) {
                    b = 50;
                    break;
                }
                break;
            case -479705388:
                if (upperCase.equals("CURRENT_DATE")) {
                    b = 29;
                    break;
                }
                break;
            case -479221261:
                if (upperCase.equals("CURRENT_TIME")) {
                    b = 30;
                    break;
                }
                break;
            case -383989871:
                if (upperCase.equals("IMMEDIATE")) {
                    b = BluetoothHidDevice.SUBCLASS1_KEYBOARD;
                    break;
                }
                break;
            case -342592494:
                if (upperCase.equals("RECURSIVE")) {
                    b = 105;
                    break;
                }
                break;
            case -341909096:
                if (upperCase.equals("TRIGGER")) {
                    b = 128;
                    break;
                }
                break;
            case -262905456:
                if (upperCase.equals("CURRENT_TIMESTAMP")) {
                    b = 31;
                    break;
                }
                break;
            case -146347732:
                if (upperCase.equals("ANALYZE")) {
                    b = 6;
                    break;
                }
                break;
            case -760130:
                if (upperCase.equals("TRANSACTION")) {
                    b = 127;
                    break;
                }
                break;
            case 2098:
                if (upperCase.equals("AS")) {
                    b = 8;
                    break;
                }
                break;
            case 2135:
                if (upperCase.equals("BY")) {
                    b = WifiScanner.PnoSettings.PnoNetwork.FLAG_SAME_NETWORK;
                    break;
                }
                break;
            case 2187:
                if (upperCase.equals("DO")) {
                    b = 40;
                    break;
                }
                break;
            case 2333:
                if (upperCase.equals("IF")) {
                    b = 62;
                    break;
                }
                break;
            case 2341:
                if (upperCase.equals("IN")) {
                    b = 65;
                    break;
                }
                break;
            case 2346:
                if (upperCase.equals("IS")) {
                    b = 74;
                    break;
                }
                break;
            case 2497:
                if (upperCase.equals("NO")) {
                    b = 83;
                    break;
                }
                break;
            case 2519:
                if (upperCase.equals("OF")) {
                    b = 89;
                    break;
                }
                break;
            case 2527:
                if (upperCase.equals("ON")) {
                    b = 91;
                    break;
                }
                break;
            case 2531:
                if (upperCase.equals("OR")) {
                    b = 92;
                    break;
                }
                break;
            case 2683:
                if (upperCase.equals("TO")) {
                    b = 126;
                    break;
                }
                break;
            case 64641:
                if (upperCase.equals("ADD")) {
                    b = 2;
                    break;
                }
                break;
            case 64897:
                if (upperCase.equals("ALL")) {
                    b = 4;
                    break;
                }
                break;
            case 64951:
                if (upperCase.equals("AND")) {
                    b = 7;
                    break;
                }
                break;
            case 65105:
                if (upperCase.equals("ASC")) {
                    b = 9;
                    break;
                }
                break;
            case 68795:
                if (upperCase.equals("END")) {
                    b = 44;
                    break;
                }
                break;
            case 69801:
                if (upperCase.equals("FOR")) {
                    b = 54;
                    break;
                }
                break;
            case 74303:
                if (upperCase.equals(Credentials.EXTRA_PUBLIC_KEY)) {
                    b = 77;
                    break;
                }
                break;
            case 77491:
                if (upperCase.equals("NOT")) {
                    b = 85;
                    break;
                }
                break;
            case 81338:
                if (upperCase.equals("ROW")) {
                    b = 115;
                    break;
                }
                break;
            case 81986:
                if (upperCase.equals("SET")) {
                    b = 120;
                    break;
                }
                break;
            case 2061104:
                if (upperCase.equals("CASE")) {
                    b = 18;
                    break;
                }
                break;
            case 2061119:
                if (upperCase.equals("CAST")) {
                    b = 19;
                    break;
                }
                break;
            case 2094737:
                if (upperCase.equals("DESC")) {
                    b = 37;
                    break;
                }
                break;
            case 2107119:
                if (upperCase.equals("DROP")) {
                    b = 41;
                    break;
                }
                break;
            case 2120193:
                if (upperCase.equals("EACH")) {
                    b = 42;
                    break;
                }
                break;
            case 2131257:
                if (upperCase.equals("ELSE")) {
                    b = 43;
                    break;
                }
                break;
            case 2150174:
                if (upperCase.equals("FAIL")) {
                    b = 51;
                    break;
                }
                break;
            case 2166698:
                if (upperCase.equals("FROM")) {
                    b = 56;
                    break;
                }
                break;
            case 2169487:
                if (upperCase.equals("FULL")) {
                    b = 57;
                    break;
                }
                break;
            case 2190712:
                if (upperCase.equals("GLOB")) {
                    b = 58;
                    break;
                }
                break;
            case 2252384:
                if (upperCase.equals("INTO")) {
                    b = 73;
                    break;
                }
                break;
            case 2282794:
                if (upperCase.equals("JOIN")) {
                    b = 76;
                    break;
                }
                break;
            case 2332679:
                if (upperCase.equals("LEFT")) {
                    b = 78;
                    break;
                }
                break;
            case 2336663:
                if (upperCase.equals("LIKE")) {
                    b = 79;
                    break;
                }
                break;
            case 2407815:
                if (upperCase.equals(WifiEnterpriseConfig.EMPTY_VALUE)) {
                    b = 88;
                    break;
                }
                break;
            case 2438356:
                if (upperCase.equals("OVER")) {
                    b = 96;
                    break;
                }
                break;
            case 2458409:
                if (upperCase.equals("PLAN")) {
                    b = 98;
                    break;
                }
                break;
            case 2521561:
                if (upperCase.equals("ROWS")) {
                    b = 116;
                    break;
                }
                break;
            case 2571220:
                if (upperCase.equals("TEMP")) {
                    b = 122;
                    break;
                }
                break;
            case 2573853:
                if (upperCase.equals("THEN")) {
                    b = 124;
                    break;
                }
                break;
            case 2574819:
                if (upperCase.equals("TIES")) {
                    b = 125;
                    break;
                }
                break;
            case 2634405:
                if (upperCase.equals("VIEW")) {
                    b = 136;
                    break;
                }
                break;
            case 2663226:
                if (upperCase.equals("WHEN")) {
                    b = 138;
                    break;
                }
                break;
            case 2664646:
                if (upperCase.equals("WITH")) {
                    b = 141;
                    break;
                }
                break;
            case 40307892:
                if (upperCase.equals("FOREIGN")) {
                    b = 55;
                    break;
                }
                break;
            case 62073616:
                if (upperCase.equals("ABORT")) {
                    b = 0;
                    break;
                }
                break;
            case 62197180:
                if (upperCase.equals("AFTER")) {
                    b = 3;
                    break;
                }
                break;
            case 62375926:
                if (upperCase.equals("ALTER")) {
                    b = 5;
                    break;
                }
                break;
            case 63078537:
                if (upperCase.equals("BEGIN")) {
                    b = 13;
                    break;
                }
                break;
            case 64089320:
                if (upperCase.equals("CHECK")) {
                    b = 20;
                    break;
                }
                break;
            case 64397344:
                if (upperCase.equals("CROSS")) {
                    b = GsmAlphabet.GSM_EXTENDED_ESCAPE;
                    break;
                }
                break;
            case 68091487:
                if (upperCase.equals("GROUP")) {
                    b = 59;
                    break;
                }
                break;
            case 69808306:
                if (upperCase.equals("INDEX")) {
                    b = 66;
                    break;
                }
                break;
            case 69817910:
                if (upperCase.equals("INNER")) {
                    b = 69;
                    break;
                }
                break;
            case 72438683:
                if (upperCase.equals("LIMIT")) {
                    b = 80;
                    break;
                }
                break;
            case 73130405:
                if (upperCase.equals("MATCH")) {
                    b = 81;
                    break;
                }
                break;
            case 75468590:
                if (upperCase.equals("ORDER")) {
                    b = 93;
                    break;
                }
                break;
            case 75573339:
                if (upperCase.equals("OUTER")) {
                    b = 95;
                    break;
                }
                break;
            case 77406376:
                if (upperCase.equals("QUERY")) {
                    b = 102;
                    break;
                }
                break;
            case 77737932:
                if (upperCase.equals("RAISE")) {
                    b = 103;
                    break;
                }
                break;
            case 77742365:
                if (upperCase.equals("RANGE")) {
                    b = 104;
                    break;
                }
                break;
            case 77974012:
                if (upperCase.equals("RIGHT")) {
                    b = 113;
                    break;
                }
                break;
            case 78312308:
                if (upperCase.equals("RTRIM")) {
                    b = 117;
                    break;
                }
                break;
            case 79578030:
                if (upperCase.equals("TABLE")) {
                    b = 121;
                    break;
                }
                break;
            case 80895663:
                if (upperCase.equals("UNION")) {
                    b = 130;
                    break;
                }
                break;
            case 81044580:
                if (upperCase.equals("USING")) {
                    b = 133;
                    break;
                }
                break;
            case 82560199:
                if (upperCase.equals("WHERE")) {
                    b = 139;
                    break;
                }
                break;
            case 178245246:
                if (upperCase.equals("EXCLUSIVE")) {
                    b = 48;
                    break;
                }
                break;
            case 202578898:
                if (upperCase.equals("CONFLICT")) {
                    b = 24;
                    break;
                }
                break;
            case 273740228:
                if (upperCase.equals("UNBOUNDED")) {
                    b = 129;
                    break;
                }
                break;
            case 294715869:
                if (upperCase.equals("CONSTRAINT")) {
                    b = 25;
                    break;
                }
                break;
            case 337882266:
                if (upperCase.equals("DEFERRABLE")) {
                    b = 34;
                    break;
                }
                break;
            case 403216866:
                if (upperCase.equals("PRIMARY")) {
                    b = 101;
                    break;
                }
                break;
            case 446081724:
                if (upperCase.equals("RESTRICT")) {
                    b = 112;
                    break;
                }
                break;
            case 476614193:
                if (upperCase.equals("TEMPORARY")) {
                    b = 123;
                    break;
                }
                break;
            case 501348328:
                if (upperCase.equals("BETWEEN")) {
                    b = BluetoothHidDevice.ERROR_RSP_UNKNOWN;
                    break;
                }
                break;
            case 522907364:
                if (upperCase.equals("ROLLBACK")) {
                    b = 114;
                    break;
                }
                break;
            case 986784458:
                if (upperCase.equals("PARTITION")) {
                    b = 97;
                    break;
                }
                break;
            case 1071324924:
                if (upperCase.equals("DISTINCT")) {
                    b = 39;
                    break;
                }
                break;
            case 1184148203:
                if (upperCase.equals("VIRTUAL")) {
                    b = 137;
                    break;
                }
                break;
            case 1272812180:
                if (upperCase.equals("CASCADE")) {
                    b = 17;
                    break;
                }
                break;
            case 1406276771:
                if (upperCase.equals("PRECEDING")) {
                    b = 100;
                    break;
                }
                break;
            case 1430517727:
                if (upperCase.equals("DEFERRED")) {
                    b = 35;
                    break;
                }
                break;
            case 1667424262:
                if (upperCase.equals("COLLATE")) {
                    b = 21;
                    break;
                }
                break;
            case 1806077535:
                if (upperCase.equals("REINDEX")) {
                    b = 108;
                    break;
                }
                break;
            case 1808577511:
                if (upperCase.equals("RELEASE")) {
                    b = 109;
                    break;
                }
                break;
            case 1812479636:
                if (upperCase.equals("REPLACE")) {
                    b = 111;
                    break;
                }
                break;
            case 1844922713:
                if (upperCase.equals("CURRENT")) {
                    b = 28;
                    break;
                }
                break;
            case 1870042760:
                if (upperCase.equals("REFERENCES")) {
                    b = 106;
                    break;
                }
                break;
            case 1925345846:
                if (upperCase.equals("ACTION")) {
                    b = 1;
                    break;
                }
                break;
            case 1941037637:
                if (upperCase.equals("ATTACH")) {
                    b = 10;
                    break;
                }
                break;
            case 1955410815:
                if (upperCase.equals("BEFORE")) {
                    b = 12;
                    break;
                }
                break;
            case 1959329793:
                if (upperCase.equals("BINARY")) {
                    b = MidiConstants.STATUS_CHANNEL_MASK;
                    break;
                }
                break;
            case 1993459542:
                if (upperCase.equals("COLUMN")) {
                    b = 22;
                    break;
                }
                break;
            case 1993481527:
                if (upperCase.equals("COMMIT")) {
                    b = 23;
                    break;
                }
                break;
            case 1996002556:
                if (upperCase.equals("CREATE")) {
                    b = 26;
                    break;
                }
                break;
            case 2012838315:
                if (upperCase.equals("DELETE")) {
                    b = 36;
                    break;
                }
                break;
            case 2013072275:
                if (upperCase.equals("DETACH")) {
                    b = 38;
                    break;
                }
                break;
            case 2054124673:
                if (upperCase.equals("ESCAPE")) {
                    b = 45;
                    break;
                }
                break;
            case 2058746137:
                if (upperCase.equals("EXCEPT")) {
                    b = 46;
                    break;
                }
                break;
            case 2058938460:
                if (upperCase.equals("EXISTS")) {
                    b = 49;
                    break;
                }
                break;
            case 2073136296:
                if (upperCase.equals("WITHOUT")) {
                    b = 142;
                    break;
                }
                break;
            case 2073804664:
                if (upperCase.equals("FILTER")) {
                    b = 52;
                    break;
                }
                break;
            case 2110836180:
                if (upperCase.equals("GROUPS")) {
                    b = 60;
                    break;
                }
                break;
            case 2123962405:
                if (upperCase.equals("HAVING")) {
                    b = 61;
                    break;
                }
                break;
        }
        switch (b) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
            case 16:
            case 17:
            case 18:
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
            case 24:
            case 25:
            case 26:
            case 27:
            case 28:
            case 29:
            case 30:
            case 31:
            case 32:
            case 33:
            case 34:
            case 35:
            case 36:
            case 37:
            case 38:
            case 39:
            case 40:
            case 41:
            case 42:
            case 43:
            case 44:
            case 45:
            case 46:
            case 47:
            case 48:
            case 49:
            case 50:
            case 51:
            case 52:
            case 53:
            case 54:
            case 55:
            case 56:
            case 57:
            case 58:
            case 59:
            case 60:
            case 61:
            case 62:
            case 63:
            case 64:
            case 65:
            case 66:
            case 67:
            case 68:
            case 69:
            case 70:
            case 71:
            case 72:
            case 73:
            case 74:
            case 75:
            case 76:
            case 77:
            case 78:
            case 79:
            case 80:
            case 81:
            case 82:
            case 83:
            case 84:
            case 85:
            case 86:
            case 87:
            case 88:
            case 89:
            case 90:
            case 91:
            case 92:
            case 93:
            case 94:
            case 95:
            case 96:
            case 97:
            case 98:
            case 99:
            case 100:
            case 101:
            case 102:
            case 103:
            case 104:
            case 105:
            case 106:
            case 107:
            case 108:
            case 109:
            case 110:
            case 111:
            case 112:
            case 113:
            case 114:
            case 115:
            case 116:
            case 117:
            case 118:
            case 119:
            case 120:
            case 121:
            case 122:
            case 123:
            case 124:
            case 125:
            case 126:
            case 127:
            case 128:
            case 129:
            case 130:
            case 131:
            case 132:
            case 133:
            case 134:
            case 135:
            case 136:
            case 137:
            case 138:
            case 139:
            case 140:
            case 141:
            case 142:
                return true;
            default:
                return false;
        }
    }

    public static boolean isFunction(String str) {
        byte b;
        String lowerCase = str.toLowerCase(Locale.US);
        switch (lowerCase.hashCode()) {
            case -1191314396:
                b = !lowerCase.equals("ifnull") ? (byte) -1 : (byte) 8;
                break;
            case -1106363674:
                if (lowerCase.equals("length")) {
                    b = 10;
                    break;
                }
                break;
            case -1102761116:
                if (lowerCase.equals("likely")) {
                    b = 13;
                    break;
                }
                break;
            case -1034384156:
                if (lowerCase.equals("nullif")) {
                    b = 18;
                    break;
                }
                break;
            case -946884697:
                if (lowerCase.equals("coalesce")) {
                    b = 3;
                    break;
                }
                break;
            case -938285885:
                if (lowerCase.equals("random")) {
                    b = 19;
                    break;
                }
                break;
            case -891529231:
                if (lowerCase.equals("substr")) {
                    b = 24;
                    break;
                }
                break;
            case -858802543:
                if (lowerCase.equals("typeof")) {
                    b = 28;
                    break;
                }
                break;
            case -660406060:
                if (lowerCase.equals("group_concat")) {
                    b = 6;
                    break;
                }
                break;
            case -414949776:
                if (lowerCase.equals("likelihood")) {
                    b = 12;
                    break;
                }
                break;
            case -287016227:
                if (lowerCase.equals("unicode")) {
                    b = 29;
                    break;
                }
                break;
            case -216257731:
                if (lowerCase.equals("unlikely")) {
                    b = 30;
                    break;
                }
                break;
            case 96370:
                if (lowerCase.equals("abs")) {
                    b = 0;
                    break;
                }
                break;
            case 96978:
                if (lowerCase.equals("avg")) {
                    b = 1;
                    break;
                }
                break;
            case 103195:
                if (lowerCase.equals("hex")) {
                    b = 7;
                    break;
                }
                break;
            case 107876:
                if (lowerCase.equals(Slice.SUBTYPE_MAX)) {
                    b = WifiScanner.PnoSettings.PnoNetwork.FLAG_SAME_NETWORK;
                    break;
                }
                break;
            case 108114:
                if (lowerCase.equals("min")) {
                    b = 17;
                    break;
                }
                break;
            case 114251:
                if (lowerCase.equals("sum")) {
                    b = 25;
                    break;
                }
                break;
            case 3052374:
                if (lowerCase.equals("char")) {
                    b = 2;
                    break;
                }
                break;
            case 3175800:
                if (lowerCase.equals("glob")) {
                    b = 5;
                    break;
                }
                break;
            case 3321751:
                if (lowerCase.equals("like")) {
                    b = 11;
                    break;
                }
                break;
            case 3568674:
                if (lowerCase.equals("trim")) {
                    b = GsmAlphabet.GSM_EXTENDED_ESCAPE;
                    break;
                }
                break;
            case 94851343:
                if (lowerCase.equals("count")) {
                    b = 4;
                    break;
                }
                break;
            case 100360940:
                if (lowerCase.equals("instr")) {
                    b = 9;
                    break;
                }
                break;
            case 103164673:
                if (lowerCase.equals("lower")) {
                    b = BluetoothHidDevice.ERROR_RSP_UNKNOWN;
                    break;
                }
                break;
            case 103308942:
                if (lowerCase.equals("ltrim")) {
                    b = MidiConstants.STATUS_CHANNEL_MASK;
                    break;
                }
                break;
            case 108704142:
                if (lowerCase.equals("round")) {
                    b = 22;
                    break;
                }
                break;
            case 108850068:
                if (lowerCase.equals("rtrim")) {
                    b = 23;
                    break;
                }
                break;
            case 110549828:
                if (lowerCase.equals("total")) {
                    b = 26;
                    break;
                }
                break;
            case 111499426:
                if (lowerCase.equals("upper")) {
                    b = 31;
                    break;
                }
                break;
            case 116062944:
                if (lowerCase.equals("randomblob")) {
                    b = 20;
                    break;
                }
                break;
            case 687315525:
                if (lowerCase.equals("zeroblob")) {
                    b = 32;
                    break;
                }
                break;
            case 1094496948:
                if (lowerCase.equals("replace")) {
                    b = 21;
                    break;
                }
                break;
        }
        switch (b) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
            case 16:
            case 17:
            case 18:
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
            case 24:
            case 25:
            case 26:
            case 27:
            case 28:
            case 29:
            case 30:
            case 31:
            case 32:
                return true;
            default:
                return false;
        }
    }

    public static boolean isType(String str) {
        byte b;
        String upperCase = str.toUpperCase(Locale.US);
        switch (upperCase.hashCode()) {
            case -2034720975:
                b = !upperCase.equals("DECIMAL") ? (byte) -1 : (byte) 19;
                break;
            case -1718637701:
                if (upperCase.equals("DATETIME")) {
                    b = 22;
                    break;
                }
                break;
            case -1618932450:
                if (upperCase.equals("INTEGER")) {
                    b = 1;
                    break;
                }
                break;
            case -1282431251:
                if (upperCase.equals("NUMERIC")) {
                    b = 18;
                    break;
                }
                break;
            case -594415409:
                if (upperCase.equals("TINYINT")) {
                    b = 2;
                    break;
                }
                break;
            case -545151281:
                if (upperCase.equals("NVARCHAR")) {
                    b = 11;
                    break;
                }
                break;
            case 72655:
                if (upperCase.equals("INT")) {
                    b = 0;
                    break;
                }
                break;
            case 2041757:
                if (upperCase.equals("BLOB")) {
                    b = BluetoothHidDevice.ERROR_RSP_UNKNOWN;
                    break;
                }
                break;
            case 2071548:
                if (upperCase.equals("CLOB")) {
                    b = 13;
                    break;
                }
                break;
            case 2090926:
                if (upperCase.equals("DATE")) {
                    b = 21;
                    break;
                }
                break;
            case 2252355:
                if (upperCase.equals("INT2")) {
                    b = 6;
                    break;
                }
                break;
            case 2252361:
                if (upperCase.equals("INT8")) {
                    b = 7;
                    break;
                }
                break;
            case 2511262:
                if (upperCase.equals("REAL")) {
                    b = MidiConstants.STATUS_CHANNEL_MASK;
                    break;
                }
                break;
            case 2571565:
                if (upperCase.equals("TEXT")) {
                    b = 12;
                    break;
                }
                break;
            case 55823113:
                if (upperCase.equals("CHARACTER")) {
                    b = 8;
                    break;
                }
                break;
            case 66988604:
                if (upperCase.equals("FLOAT")) {
                    b = 17;
                    break;
                }
                break;
            case 74101924:
                if (upperCase.equals("NCHAR")) {
                    b = 10;
                    break;
                }
                break;
            case 176095624:
                if (upperCase.equals("SMALLINT")) {
                    b = 3;
                    break;
                }
                break;
            case 651290682:
                if (upperCase.equals("MEDIUMINT")) {
                    b = 4;
                    break;
                }
                break;
            case 782694408:
                if (upperCase.equals("BOOLEAN")) {
                    b = 20;
                    break;
                }
                break;
            case 954596061:
                if (upperCase.equals("VARCHAR")) {
                    b = 9;
                    break;
                }
                break;
            case 1959128815:
                if (upperCase.equals("BIGINT")) {
                    b = 5;
                    break;
                }
                break;
            case 2022338513:
                if (upperCase.equals("DOUBLE")) {
                    b = WifiScanner.PnoSettings.PnoNetwork.FLAG_SAME_NETWORK;
                    break;
                }
                break;
        }
        switch (b) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
            case 16:
            case 17:
            case 18:
            case 19:
            case 20:
            case 21:
            case 22:
                return true;
            default:
                return false;
        }
    }
}
