package android.icu.impl;

import android.icu.text.StringTransform;
import android.icu.text.SymbolTable;
import android.icu.text.UnicodeSet;
import android.icu.util.Freezable;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParsePosition;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

public class UnicodeRegex implements Cloneable, Freezable<UnicodeRegex>, StringTransform {
    private static final UnicodeRegex STANDARD = new UnicodeRegex();
    private SymbolTable symbolTable;
    private String bnfCommentString = "#";
    private String bnfVariableInfix = "=";
    private String bnfLineSeparator = "\n";
    private Comparator<Object> LongestFirst = new Comparator<Object>() {
        @Override
        public int compare(Object obj, Object obj2) {
            String string = obj.toString();
            String string2 = obj2.toString();
            int length = string.length();
            int length2 = string2.length();
            return length != length2 ? length2 - length : string.compareTo(string2);
        }
    };

    public SymbolTable getSymbolTable() {
        return this.symbolTable;
    }

    public UnicodeRegex setSymbolTable(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        return this;
    }

    @Override
    public String transform(String str) {
        char c;
        StringBuilder sb = new StringBuilder();
        UnicodeSet unicodeSet = new UnicodeSet();
        ParsePosition parsePosition = new ParsePosition(0);
        int iProcessSet = 0;
        char c2 = 0;
        while (iProcessSet < str.length()) {
            char cCharAt = str.charAt(iProcessSet);
            switch (c2) {
                case 0:
                    if (cCharAt == '\\') {
                        if (UnicodeSet.resemblesPattern(str, iProcessSet)) {
                            iProcessSet = processSet(str, iProcessSet, sb, unicodeSet, parsePosition);
                        }
                        sb.append(cCharAt);
                        c2 = c;
                    } else if (cCharAt == '[' && UnicodeSet.resemblesPattern(str, iProcessSet)) {
                        iProcessSet = processSet(str, iProcessSet, sb, unicodeSet, parsePosition);
                    } else {
                        c = c2;
                        sb.append(cCharAt);
                        c2 = c;
                    }
                    break;
                case 1:
                    c = cCharAt == 'Q' ? (char) 1 : (char) 0;
                    sb.append(cCharAt);
                    c2 = c;
                    break;
                case 2:
                    c = cCharAt == '\\' ? (char) 3 : c2;
                    sb.append(cCharAt);
                    c2 = c;
                    break;
                case 3:
                    if (cCharAt == 'E') {
                    }
                    c = 2;
                    sb.append(cCharAt);
                    c2 = c;
                    break;
                default:
                    c = c2;
                    sb.append(cCharAt);
                    c2 = c;
                    break;
            }
            iProcessSet++;
        }
        return sb.toString();
    }

    public static String fix(String str) {
        return STANDARD.transform(str);
    }

    public static Pattern compile(String str) {
        return Pattern.compile(STANDARD.transform(str));
    }

    public static Pattern compile(String str, int i) {
        return Pattern.compile(STANDARD.transform(str), i);
    }

    public String compileBnf(String str) {
        return compileBnf(Arrays.asList(str.split("\\r\\n?|\\n")));
    }

    public String compileBnf(List<String> list) {
        Map<String, String> variables = getVariables(list);
        LinkedHashSet linkedHashSet = new LinkedHashSet(variables.keySet());
        for (int i = 0; i < 2; i++) {
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                for (Map.Entry<String, String> entry2 : variables.entrySet()) {
                    String key2 = entry2.getKey();
                    String value2 = entry2.getValue();
                    if (!key.equals(key2)) {
                        String strReplace = value2.replace(key, value);
                        if (!strReplace.equals(value2)) {
                            linkedHashSet.remove(key);
                            variables.put(key2, strReplace);
                        }
                    }
                }
            }
        }
        if (linkedHashSet.size() != 1) {
            throw new IllegalArgumentException("Not a single root: " + linkedHashSet);
        }
        return variables.get(linkedHashSet.iterator().next());
    }

    public String getBnfCommentString() {
        return this.bnfCommentString;
    }

    public void setBnfCommentString(String str) {
        this.bnfCommentString = str;
    }

    public String getBnfVariableInfix() {
        return this.bnfVariableInfix;
    }

    public void setBnfVariableInfix(String str) {
        this.bnfVariableInfix = str;
    }

    public String getBnfLineSeparator() {
        return this.bnfLineSeparator;
    }

    public void setBnfLineSeparator(String str) {
        this.bnfLineSeparator = str;
    }

    public static List<String> appendLines(List<String> list, String str, String str2) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(str);
        try {
            return appendLines(list, fileInputStream, str2);
        } finally {
            fileInputStream.close();
        }
    }

    public static List<String> appendLines(List<String> list, InputStream inputStream, String str) throws IOException {
        if (str == null) {
            str = "UTF-8";
        }
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, str));
        while (true) {
            String line = bufferedReader.readLine();
            if (line != null) {
                list.add(line);
            } else {
                return list;
            }
        }
    }

    @Override
    public UnicodeRegex cloneAsThawed() {
        try {
            return (UnicodeRegex) clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public UnicodeRegex freeze() {
        return this;
    }

    @Override
    public boolean isFrozen() {
        return true;
    }

    private int processSet(String str, int i, StringBuilder sb, UnicodeSet unicodeSet, ParsePosition parsePosition) {
        try {
            parsePosition.setIndex(i);
            UnicodeSet unicodeSetApplyPattern = unicodeSet.clear().applyPattern(str, parsePosition, this.symbolTable, 0);
            unicodeSetApplyPattern.complement().complement();
            sb.append(unicodeSetApplyPattern.toPattern(false));
            return parsePosition.getIndex() - 1;
        } catch (Exception e) {
            throw ((IllegalArgumentException) new IllegalArgumentException("Error in " + str).initCause(e));
        }
    }

    private Map<String, String> getVariables(List<String> list) {
        String strSubstring;
        int iIndexOf;
        TreeMap treeMap = new TreeMap(this.LongestFirst);
        StringBuffer stringBuffer = new StringBuffer();
        Iterator<String> it = list.iterator();
        String strTrim = null;
        int i = 0;
        while (it.hasNext()) {
            String next = it.next();
            i++;
            if (next.length() != 0) {
                if (next.charAt(0) == 65279) {
                    next = next.substring(1);
                }
                if (this.bnfCommentString != null && (iIndexOf = next.indexOf(this.bnfCommentString)) >= 0) {
                    next = next.substring(0, iIndexOf);
                }
                String strTrim2 = next.trim();
                if (strTrim2.length() != 0 && next.trim().length() != 0) {
                    boolean zEndsWith = strTrim2.endsWith(";");
                    if (zEndsWith) {
                        strSubstring = next.substring(0, next.lastIndexOf(59));
                    } else {
                        strSubstring = next;
                    }
                    int iIndexOf2 = strSubstring.indexOf(this.bnfVariableInfix);
                    if (iIndexOf2 >= 0) {
                        if (strTrim != null) {
                            throw new IllegalArgumentException("Missing ';' before " + i + ") " + next);
                        }
                        strTrim = strSubstring.substring(0, iIndexOf2).trim();
                        if (treeMap.containsKey(strTrim)) {
                            throw new IllegalArgumentException("Duplicate variable definition in " + next);
                        }
                        stringBuffer.append(strSubstring.substring(iIndexOf2 + 1).trim());
                    } else {
                        if (strTrim == null) {
                            throw new IllegalArgumentException("Missing '=' at " + i + ") " + next);
                        }
                        stringBuffer.append(this.bnfLineSeparator);
                        stringBuffer.append(strSubstring);
                    }
                    if (zEndsWith) {
                        treeMap.put(strTrim, stringBuffer.toString());
                        stringBuffer.setLength(0);
                        strTrim = null;
                    }
                }
            }
        }
        if (strTrim != null) {
            throw new IllegalArgumentException("Missing ';' at end");
        }
        return treeMap;
    }
}
