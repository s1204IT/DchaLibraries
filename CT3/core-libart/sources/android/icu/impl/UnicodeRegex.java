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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

public class UnicodeRegex implements Cloneable, Freezable<UnicodeRegex>, StringTransform {
    private static UnicodeRegex STANDARD = new UnicodeRegex();
    private SymbolTable symbolTable;
    private String bnfCommentString = "#";
    private String bnfVariableInfix = "=";
    private String bnfLineSeparator = "\n";
    private Appendable log = null;
    private Comparator<Object> LongestFirst = new Comparator<Object>() {
        @Override
        public int compare(Object obj0, Object obj1) {
            String arg0 = obj0.toString();
            String arg1 = obj1.toString();
            int len0 = arg0.length();
            int len1 = arg1.length();
            return len0 != len1 ? len1 - len0 : arg0.compareTo(arg1);
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
    public String transform(String regex) {
        StringBuilder result = new StringBuilder();
        UnicodeSet temp = new UnicodeSet();
        ParsePosition pos = new ParsePosition(0);
        int state = 0;
        int i = 0;
        while (i < regex.length()) {
            char ch = regex.charAt(i);
            switch (state) {
                case 0:
                    if (ch == '\\') {
                        if (UnicodeSet.resemblesPattern(regex, i)) {
                            i = processSet(regex, i, result, temp, pos);
                        } else {
                            state = 1;
                            result.append(ch);
                        }
                    } else if (ch == '[' && UnicodeSet.resemblesPattern(regex, i)) {
                        i = processSet(regex, i, result, temp, pos);
                    } else {
                        result.append(ch);
                    }
                    break;
                case 1:
                    if (ch == 'Q') {
                        state = 1;
                    } else {
                        state = 0;
                    }
                    result.append(ch);
                    break;
                case 2:
                    if (ch == '\\') {
                        state = 3;
                    }
                    result.append(ch);
                    break;
                case 3:
                    if (ch == 'E') {
                    }
                    state = 2;
                    result.append(ch);
                    break;
            }
            i++;
        }
        return result.toString();
    }

    public static String fix(String regex) {
        return STANDARD.transform(regex);
    }

    public static Pattern compile(String regex) {
        return Pattern.compile(STANDARD.transform(regex));
    }

    public static Pattern compile(String regex, int options) {
        return Pattern.compile(STANDARD.transform(regex), options);
    }

    public String compileBnf(String bnfLines) {
        return compileBnf(Arrays.asList(bnfLines.split("\\r\\n?|\\n")));
    }

    public String compileBnf(List<String> lines) {
        Map<String, String> variables = getVariables(lines);
        Set<String> unused = new LinkedHashSet<>(variables.keySet());
        for (int i = 0; i < 2; i++) {
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                String variable = entry.getKey();
                CharSequence definition = (String) entry.getValue();
                for (Map.Entry<String, String> entry2 : variables.entrySet()) {
                    String variable2 = entry2.getKey();
                    String definition2 = entry2.getValue();
                    if (!variable.equals(variable2)) {
                        String altered2 = definition2.replace(variable, definition);
                        if (altered2.equals(definition2)) {
                            continue;
                        } else {
                            unused.remove(variable);
                            variables.put(variable2, altered2);
                            if (this.log != null) {
                                try {
                                    this.log.append(variable2 + "=" + altered2 + ";");
                                } catch (IOException e) {
                                    throw ((IllegalArgumentException) new IllegalArgumentException().initCause(e));
                                }
                            } else {
                                continue;
                            }
                        }
                    }
                }
            }
        }
        if (unused.size() != 1) {
            throw new IllegalArgumentException("Not a single root: " + unused);
        }
        return variables.get(unused.iterator().next());
    }

    public String getBnfCommentString() {
        return this.bnfCommentString;
    }

    public void setBnfCommentString(String bnfCommentString) {
        this.bnfCommentString = bnfCommentString;
    }

    public String getBnfVariableInfix() {
        return this.bnfVariableInfix;
    }

    public void setBnfVariableInfix(String bnfVariableInfix) {
        this.bnfVariableInfix = bnfVariableInfix;
    }

    public String getBnfLineSeparator() {
        return this.bnfLineSeparator;
    }

    public void setBnfLineSeparator(String bnfLineSeparator) {
        this.bnfLineSeparator = bnfLineSeparator;
    }

    public static List<String> appendLines(List<String> result, String file, String encoding) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(file);
        try {
            return appendLines(result, fileInputStream, encoding);
        } finally {
            fileInputStream.close();
        }
    }

    public static List<String> appendLines(List<String> result, InputStream inputStream, String encoding) throws IOException {
        if (encoding == null) {
            encoding = "UTF-8";
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream, encoding));
        while (true) {
            String line = in.readLine();
            if (line != null) {
                result.add(line);
            } else {
                return result;
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

    private int processSet(String regex, int i, StringBuilder result, UnicodeSet temp, ParsePosition pos) {
        try {
            pos.setIndex(i);
            UnicodeSet x = temp.clear().applyPattern(regex, pos, this.symbolTable, 0);
            x.complement().complement();
            result.append(x.toPattern(false));
            int i2 = pos.getIndex() - 1;
            return i2;
        } catch (Exception e) {
            throw ((IllegalArgumentException) new IllegalArgumentException("Error in " + regex).initCause(e));
        }
    }

    private Map<String, String> getVariables(List<String> lines) {
        int hashPos;
        Map<String, String> variables = new TreeMap<>((Comparator<? super String>) this.LongestFirst);
        String variable = null;
        StringBuffer definition = new StringBuffer();
        int count = 0;
        for (String line : lines) {
            count++;
            if (line.length() != 0) {
                if (line.charAt(0) == 65279) {
                    line = line.substring(1);
                }
                if (this.bnfCommentString != null && (hashPos = line.indexOf(this.bnfCommentString)) >= 0) {
                    line = line.substring(0, hashPos);
                }
                String trimline = line.trim();
                if (trimline.length() != 0) {
                    String linePart = line;
                    if (line.trim().length() == 0) {
                        continue;
                    } else {
                        boolean terminated = trimline.endsWith(";");
                        if (terminated) {
                            linePart = linePart.substring(0, linePart.lastIndexOf(59));
                        }
                        int equalsPos = linePart.indexOf(this.bnfVariableInfix);
                        if (equalsPos >= 0) {
                            if (variable != null) {
                                throw new IllegalArgumentException("Missing ';' before " + count + ") " + line);
                            }
                            variable = linePart.substring(0, equalsPos).trim();
                            if (variables.containsKey(variable)) {
                                throw new IllegalArgumentException("Duplicate variable definition in " + line);
                            }
                            definition.append(linePart.substring(equalsPos + 1).trim());
                        } else {
                            if (variable == null) {
                                throw new IllegalArgumentException("Missing '=' at " + count + ") " + line);
                            }
                            definition.append(this.bnfLineSeparator).append(linePart);
                        }
                        if (terminated) {
                            variables.put(variable, definition.toString());
                            variable = null;
                            definition.setLength(0);
                        }
                    }
                } else {
                    continue;
                }
            }
        }
        if (variable != null) {
            throw new IllegalArgumentException("Missing ';' at end");
        }
        return variables;
    }
}
