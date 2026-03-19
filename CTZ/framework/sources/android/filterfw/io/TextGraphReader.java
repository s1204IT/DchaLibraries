package android.filterfw.io;

import android.filterfw.core.Filter;
import android.filterfw.core.FilterFactory;
import android.filterfw.core.FilterGraph;
import android.filterfw.core.KeyValueMap;
import android.filterfw.core.ProtocolException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;

public class TextGraphReader extends GraphReader {
    private KeyValueMap mBoundReferences;
    private ArrayList<Command> mCommands = new ArrayList<>();
    private Filter mCurrentFilter;
    private FilterGraph mCurrentGraph;
    private FilterFactory mFactory;
    private KeyValueMap mSettings;

    private interface Command {
        void execute(TextGraphReader textGraphReader) throws GraphIOException;
    }

    private class ImportPackageCommand implements Command {
        private String mPackageName;

        public ImportPackageCommand(String str) {
            this.mPackageName = str;
        }

        @Override
        public void execute(TextGraphReader textGraphReader) throws GraphIOException {
            try {
                textGraphReader.mFactory.addPackage(this.mPackageName);
            } catch (IllegalArgumentException e) {
                throw new GraphIOException(e.getMessage());
            }
        }
    }

    private class AddLibraryCommand implements Command {
        private String mLibraryName;

        public AddLibraryCommand(String str) {
            this.mLibraryName = str;
        }

        @Override
        public void execute(TextGraphReader textGraphReader) {
            FilterFactory unused = textGraphReader.mFactory;
            FilterFactory.addFilterLibrary(this.mLibraryName);
        }
    }

    private class AllocateFilterCommand implements Command {
        private String mClassName;
        private String mFilterName;

        public AllocateFilterCommand(String str, String str2) {
            this.mClassName = str;
            this.mFilterName = str2;
        }

        @Override
        public void execute(TextGraphReader textGraphReader) throws GraphIOException {
            try {
                textGraphReader.mCurrentFilter = textGraphReader.mFactory.createFilterByClassName(this.mClassName, this.mFilterName);
            } catch (IllegalArgumentException e) {
                throw new GraphIOException(e.getMessage());
            }
        }
    }

    private class InitFilterCommand implements Command {
        private KeyValueMap mParams;

        public InitFilterCommand(KeyValueMap keyValueMap) {
            this.mParams = keyValueMap;
        }

        @Override
        public void execute(TextGraphReader textGraphReader) throws GraphIOException {
            try {
                textGraphReader.mCurrentFilter.initWithValueMap(this.mParams);
                textGraphReader.mCurrentGraph.addFilter(TextGraphReader.this.mCurrentFilter);
            } catch (ProtocolException e) {
                throw new GraphIOException(e.getMessage());
            }
        }
    }

    private class ConnectCommand implements Command {
        private String mSourceFilter;
        private String mSourcePort;
        private String mTargetFilter;
        private String mTargetName;

        public ConnectCommand(String str, String str2, String str3, String str4) {
            this.mSourceFilter = str;
            this.mSourcePort = str2;
            this.mTargetFilter = str3;
            this.mTargetName = str4;
        }

        @Override
        public void execute(TextGraphReader textGraphReader) {
            textGraphReader.mCurrentGraph.connect(this.mSourceFilter, this.mSourcePort, this.mTargetFilter, this.mTargetName);
        }
    }

    @Override
    public FilterGraph readGraphString(String str) throws GraphIOException {
        FilterGraph filterGraph = new FilterGraph();
        reset();
        this.mCurrentGraph = filterGraph;
        parseString(str);
        applySettings();
        executeCommands();
        reset();
        return filterGraph;
    }

    private void reset() {
        this.mCurrentGraph = null;
        this.mCurrentFilter = null;
        this.mCommands.clear();
        this.mBoundReferences = new KeyValueMap();
        this.mSettings = new KeyValueMap();
        this.mFactory = new FilterFactory();
    }

    private void parseString(String str) throws GraphIOException {
        Pattern pattern;
        Pattern pattern2;
        Pattern pattern3;
        String strEat;
        PatternScanner patternScanner;
        Pattern pattern4;
        Pattern pattern5;
        Pattern pattern6;
        Pattern pattern7;
        Pattern patternCompile = Pattern.compile("@[a-zA-Z]+");
        Pattern patternCompile2 = Pattern.compile("\\}");
        Pattern patternCompile3 = Pattern.compile("\\{");
        Pattern patternCompile4 = Pattern.compile("(\\s+|//[^\\n]*\\n)+");
        Pattern patternCompile5 = Pattern.compile("[a-zA-Z\\.]+");
        Pattern patternCompile6 = Pattern.compile("[a-zA-Z\\./:]+");
        Pattern patternCompile7 = Pattern.compile("\\[[a-zA-Z0-9\\-_]+\\]");
        Pattern patternCompile8 = Pattern.compile("=>");
        Pattern patternCompile9 = Pattern.compile(";");
        Pattern patternCompile10 = Pattern.compile("[a-zA-Z0-9\\-_]+");
        PatternScanner patternScanner2 = new PatternScanner(str, patternCompile4);
        String str2 = null;
        String strEat2 = null;
        String strSubstring = null;
        String strEat3 = null;
        char c = 0;
        while (!patternScanner2.atEnd()) {
            switch (c) {
                case 0:
                    pattern = patternCompile5;
                    pattern2 = patternCompile6;
                    pattern3 = patternCompile9;
                    strEat = str2;
                    patternScanner = patternScanner2;
                    pattern4 = patternCompile;
                    String strEat4 = patternScanner.eat(pattern4, "<command>");
                    if (strEat4.equals("@import")) {
                        c = 1;
                    } else if (strEat4.equals("@library")) {
                        c = 2;
                    } else if (strEat4.equals("@filter")) {
                        c = 3;
                    } else if (strEat4.equals("@connect")) {
                        c = '\b';
                    } else if (strEat4.equals("@set")) {
                        c = '\r';
                    } else if (strEat4.equals("@external")) {
                        c = 14;
                    } else {
                        if (!strEat4.equals("@setting")) {
                            throw new GraphIOException("Unknown command '" + strEat4 + "'!");
                        }
                        c = 15;
                    }
                    patternCompile = pattern4;
                    str2 = strEat;
                    patternScanner2 = patternScanner;
                    patternCompile9 = pattern3;
                    patternCompile6 = pattern2;
                    patternCompile5 = pattern;
                    break;
                case 1:
                    Pattern pattern8 = patternCompile5;
                    pattern2 = patternCompile6;
                    pattern3 = patternCompile9;
                    strEat = str2;
                    patternScanner = patternScanner2;
                    pattern = pattern8;
                    this.mCommands.add(new ImportPackageCommand(patternScanner.eat(pattern, "<package-name>")));
                    pattern4 = patternCompile;
                    c = 16;
                    patternCompile = pattern4;
                    str2 = strEat;
                    patternScanner2 = patternScanner;
                    patternCompile9 = pattern3;
                    patternCompile6 = pattern2;
                    patternCompile5 = pattern;
                    break;
                case 2:
                    Pattern pattern9 = patternCompile5;
                    Pattern pattern10 = patternCompile6;
                    pattern3 = patternCompile9;
                    strEat = str2;
                    patternScanner = patternScanner2;
                    pattern2 = pattern10;
                    this.mCommands.add(new AddLibraryCommand(patternScanner.eat(pattern2, "<library-name>")));
                    pattern4 = patternCompile;
                    pattern = pattern9;
                    c = 16;
                    patternCompile = pattern4;
                    str2 = strEat;
                    patternScanner2 = patternScanner;
                    patternCompile9 = pattern3;
                    patternCompile6 = pattern2;
                    patternCompile5 = pattern;
                    break;
                case 3:
                    pattern5 = patternCompile;
                    pattern6 = patternCompile5;
                    pattern7 = patternCompile6;
                    pattern3 = patternCompile9;
                    patternScanner = patternScanner2;
                    strEat = patternScanner.eat(patternCompile10, "<class-name>");
                    c = 4;
                    pattern4 = pattern5;
                    pattern = pattern6;
                    pattern2 = pattern7;
                    patternCompile = pattern4;
                    str2 = strEat;
                    patternScanner2 = patternScanner;
                    patternCompile9 = pattern3;
                    patternCompile6 = pattern2;
                    patternCompile5 = pattern;
                    break;
                case 4:
                    pattern5 = patternCompile;
                    pattern6 = patternCompile5;
                    pattern7 = patternCompile6;
                    pattern3 = patternCompile9;
                    strEat = str2;
                    patternScanner = patternScanner2;
                    this.mCommands.add(new AllocateFilterCommand(strEat, patternScanner.eat(patternCompile10, "<filter-name>")));
                    c = 5;
                    pattern4 = pattern5;
                    pattern = pattern6;
                    pattern2 = pattern7;
                    patternCompile = pattern4;
                    str2 = strEat;
                    patternScanner2 = patternScanner;
                    patternCompile9 = pattern3;
                    patternCompile6 = pattern2;
                    patternCompile5 = pattern;
                    break;
                case 5:
                    pattern5 = patternCompile;
                    pattern6 = patternCompile5;
                    pattern7 = patternCompile6;
                    pattern3 = patternCompile9;
                    strEat = str2;
                    patternScanner = patternScanner2;
                    patternScanner.eat(patternCompile3, "{");
                    c = 6;
                    pattern4 = pattern5;
                    pattern = pattern6;
                    pattern2 = pattern7;
                    patternCompile = pattern4;
                    str2 = strEat;
                    patternScanner2 = patternScanner;
                    patternCompile9 = pattern3;
                    patternCompile6 = pattern2;
                    patternCompile5 = pattern;
                    break;
                case 6:
                    pattern5 = patternCompile;
                    pattern6 = patternCompile5;
                    pattern7 = patternCompile6;
                    pattern3 = patternCompile9;
                    strEat = str2;
                    patternScanner = patternScanner2;
                    this.mCommands.add(new InitFilterCommand(readKeyValueAssignments(patternScanner, patternCompile2)));
                    c = 7;
                    pattern4 = pattern5;
                    pattern = pattern6;
                    pattern2 = pattern7;
                    patternCompile = pattern4;
                    str2 = strEat;
                    patternScanner2 = patternScanner;
                    patternCompile9 = pattern3;
                    patternCompile6 = pattern2;
                    patternCompile5 = pattern;
                    break;
                case 7:
                    pattern5 = patternCompile;
                    pattern6 = patternCompile5;
                    pattern7 = patternCompile6;
                    pattern3 = patternCompile9;
                    strEat = str2;
                    patternScanner = patternScanner2;
                    patternScanner.eat(patternCompile2, "}");
                    c = 0;
                    pattern4 = pattern5;
                    pattern = pattern6;
                    pattern2 = pattern7;
                    patternCompile = pattern4;
                    str2 = strEat;
                    patternScanner2 = patternScanner;
                    patternCompile9 = pattern3;
                    patternCompile6 = pattern2;
                    patternCompile5 = pattern;
                    break;
                case '\b':
                    pattern5 = patternCompile;
                    pattern6 = patternCompile5;
                    pattern7 = patternCompile6;
                    pattern3 = patternCompile9;
                    strEat = str2;
                    patternScanner = patternScanner2;
                    strEat2 = patternScanner.eat(patternCompile10, "<source-filter-name>");
                    c = '\t';
                    pattern4 = pattern5;
                    pattern = pattern6;
                    pattern2 = pattern7;
                    patternCompile = pattern4;
                    str2 = strEat;
                    patternScanner2 = patternScanner;
                    patternCompile9 = pattern3;
                    patternCompile6 = pattern2;
                    patternCompile5 = pattern;
                    break;
                case '\t':
                    pattern5 = patternCompile;
                    pattern6 = patternCompile5;
                    pattern7 = patternCompile6;
                    pattern3 = patternCompile9;
                    strEat = str2;
                    patternScanner = patternScanner2;
                    String strEat5 = patternScanner.eat(patternCompile7, "[<source-port-name>]");
                    strSubstring = strEat5.substring(1, strEat5.length() - 1);
                    c = '\n';
                    pattern4 = pattern5;
                    pattern = pattern6;
                    pattern2 = pattern7;
                    patternCompile = pattern4;
                    str2 = strEat;
                    patternScanner2 = patternScanner;
                    patternCompile9 = pattern3;
                    patternCompile6 = pattern2;
                    patternCompile5 = pattern;
                    break;
                case '\n':
                    pattern5 = patternCompile;
                    pattern6 = patternCompile5;
                    pattern7 = patternCompile6;
                    pattern3 = patternCompile9;
                    strEat = str2;
                    patternScanner = patternScanner2;
                    patternScanner.eat(patternCompile8, "=>");
                    c = 11;
                    pattern4 = pattern5;
                    pattern = pattern6;
                    pattern2 = pattern7;
                    patternCompile = pattern4;
                    str2 = strEat;
                    patternScanner2 = patternScanner;
                    patternCompile9 = pattern3;
                    patternCompile6 = pattern2;
                    patternCompile5 = pattern;
                    break;
                case 11:
                    pattern5 = patternCompile;
                    pattern6 = patternCompile5;
                    pattern7 = patternCompile6;
                    pattern3 = patternCompile9;
                    strEat = str2;
                    patternScanner = patternScanner2;
                    strEat3 = patternScanner.eat(patternCompile10, "<target-filter-name>");
                    c = '\f';
                    pattern4 = pattern5;
                    pattern = pattern6;
                    pattern2 = pattern7;
                    patternCompile = pattern4;
                    str2 = strEat;
                    patternScanner2 = patternScanner;
                    patternCompile9 = pattern3;
                    patternCompile6 = pattern2;
                    patternCompile5 = pattern;
                    break;
                case '\f':
                    String strEat6 = patternScanner2.eat(patternCompile7, "[<target-port-name>]");
                    pattern3 = patternCompile9;
                    Pattern pattern11 = patternCompile5;
                    strEat = str2;
                    Pattern pattern12 = patternCompile6;
                    patternScanner = patternScanner2;
                    this.mCommands.add(new ConnectCommand(strEat2, strSubstring, strEat3, strEat6.substring(1, strEat6.length() - 1)));
                    pattern4 = patternCompile;
                    pattern = pattern11;
                    pattern2 = pattern12;
                    c = 16;
                    patternCompile = pattern4;
                    str2 = strEat;
                    patternScanner2 = patternScanner;
                    patternCompile9 = pattern3;
                    patternCompile6 = pattern2;
                    patternCompile5 = pattern;
                    break;
                case '\r':
                    this.mBoundReferences.putAll(readKeyValueAssignments(patternScanner2, patternCompile9));
                    c = 16;
                    pattern = patternCompile5;
                    pattern2 = patternCompile6;
                    pattern3 = patternCompile9;
                    strEat = str2;
                    patternScanner = patternScanner2;
                    pattern4 = patternCompile;
                    patternCompile = pattern4;
                    str2 = strEat;
                    patternScanner2 = patternScanner;
                    patternCompile9 = pattern3;
                    patternCompile6 = pattern2;
                    patternCompile5 = pattern;
                    break;
                case 14:
                    bindExternal(patternScanner2.eat(patternCompile10, "<external-identifier>"));
                    c = 16;
                    pattern = patternCompile5;
                    pattern2 = patternCompile6;
                    pattern3 = patternCompile9;
                    strEat = str2;
                    patternScanner = patternScanner2;
                    pattern4 = patternCompile;
                    patternCompile = pattern4;
                    str2 = strEat;
                    patternScanner2 = patternScanner;
                    patternCompile9 = pattern3;
                    patternCompile6 = pattern2;
                    patternCompile5 = pattern;
                    break;
                case 15:
                    this.mSettings.putAll(readKeyValueAssignments(patternScanner2, patternCompile9));
                    c = 16;
                    pattern = patternCompile5;
                    pattern2 = patternCompile6;
                    pattern3 = patternCompile9;
                    strEat = str2;
                    patternScanner = patternScanner2;
                    pattern4 = patternCompile;
                    patternCompile = pattern4;
                    str2 = strEat;
                    patternScanner2 = patternScanner;
                    patternCompile9 = pattern3;
                    patternCompile6 = pattern2;
                    patternCompile5 = pattern;
                    break;
                case 16:
                    patternScanner2.eat(patternCompile9, ";");
                    pattern = patternCompile5;
                    pattern2 = patternCompile6;
                    pattern3 = patternCompile9;
                    c = 0;
                    strEat = str2;
                    patternScanner = patternScanner2;
                    pattern4 = patternCompile;
                    patternCompile = pattern4;
                    str2 = strEat;
                    patternScanner2 = patternScanner;
                    patternCompile9 = pattern3;
                    patternCompile6 = pattern2;
                    patternCompile5 = pattern;
                    break;
                default:
                    pattern = patternCompile5;
                    pattern2 = patternCompile6;
                    pattern3 = patternCompile9;
                    strEat = str2;
                    patternScanner = patternScanner2;
                    pattern4 = patternCompile;
                    patternCompile = pattern4;
                    str2 = strEat;
                    patternScanner2 = patternScanner;
                    patternCompile9 = pattern3;
                    patternCompile6 = pattern2;
                    patternCompile5 = pattern;
                    break;
            }
        }
        if (c != 16 && c != 0) {
            throw new GraphIOException("Unexpected end of input!");
        }
    }

    @Override
    public KeyValueMap readKeyValueAssignments(String str) throws GraphIOException {
        return readKeyValueAssignments(new PatternScanner(str, Pattern.compile("\\s+")), null);
    }

    private KeyValueMap readKeyValueAssignments(PatternScanner patternScanner, Pattern pattern) throws GraphIOException {
        Pattern patternCompile = Pattern.compile("=");
        Pattern patternCompile2 = Pattern.compile(";");
        Pattern patternCompile3 = Pattern.compile("[a-zA-Z]+[a-zA-Z0-9]*");
        Pattern patternCompile4 = Pattern.compile("'[^']*'|\\\"[^\\\"]*\\\"");
        Pattern patternCompile5 = Pattern.compile("[0-9]+");
        Pattern patternCompile6 = Pattern.compile("[0-9]*\\.[0-9]+f?");
        Pattern patternCompile7 = Pattern.compile("\\$[a-zA-Z]+[a-zA-Z0-9]");
        Pattern patternCompile8 = Pattern.compile("true|false");
        KeyValueMap keyValueMap = new KeyValueMap();
        char c = 0;
        String strEat = null;
        while (!patternScanner.atEnd() && (pattern == null || !patternScanner.peek(pattern))) {
            switch (c) {
                case 0:
                    strEat = patternScanner.eat(patternCompile3, "<identifier>");
                    c = 1;
                    break;
                case 1:
                    patternScanner.eat(patternCompile, "=");
                    c = 2;
                    break;
                case 2:
                    String strTryEat = patternScanner.tryEat(patternCompile4);
                    if (strTryEat != null) {
                        keyValueMap.put(strEat, strTryEat.substring(1, strTryEat.length() - 1));
                    } else {
                        String strTryEat2 = patternScanner.tryEat(patternCompile7);
                        if (strTryEat2 != null) {
                            String strSubstring = strTryEat2.substring(1, strTryEat2.length());
                            Object obj = this.mBoundReferences != null ? this.mBoundReferences.get(strSubstring) : null;
                            if (obj == null) {
                                throw new GraphIOException("Unknown object reference to '" + strSubstring + "'!");
                            }
                            keyValueMap.put(strEat, obj);
                        } else {
                            String strTryEat3 = patternScanner.tryEat(patternCompile8);
                            if (strTryEat3 != null) {
                                keyValueMap.put(strEat, Boolean.valueOf(Boolean.parseBoolean(strTryEat3)));
                            } else {
                                String strTryEat4 = patternScanner.tryEat(patternCompile6);
                                if (strTryEat4 != null) {
                                    keyValueMap.put(strEat, Float.valueOf(Float.parseFloat(strTryEat4)));
                                } else {
                                    String strTryEat5 = patternScanner.tryEat(patternCompile5);
                                    if (strTryEat5 == null) {
                                        throw new GraphIOException(patternScanner.unexpectedTokenMessage("<value>"));
                                    }
                                    keyValueMap.put(strEat, Integer.valueOf(Integer.parseInt(strTryEat5)));
                                }
                            }
                        }
                    }
                    c = 3;
                    break;
                case 3:
                    patternScanner.eat(patternCompile2, ";");
                    c = 0;
                    break;
            }
        }
        if (c == 0 || c == 3) {
            return keyValueMap;
        }
        throw new GraphIOException("Unexpected end of assignments on line " + patternScanner.lineNo() + "!");
    }

    private void bindExternal(String str) throws GraphIOException {
        if (this.mReferences.containsKey(str)) {
            this.mBoundReferences.put(str, this.mReferences.get(str));
        } else {
            throw new GraphIOException("Unknown external variable '" + str + "'! You must add a reference to this external in the host program using addReference(...)!");
        }
    }

    private void checkReferences() throws GraphIOException {
        for (String str : this.mReferences.keySet()) {
            if (!this.mBoundReferences.containsKey(str)) {
                throw new GraphIOException("Host program specifies reference to '" + str + "', which is not declared @external in graph file!");
            }
        }
    }

    private void applySettings() throws GraphIOException {
        for (String str : this.mSettings.keySet()) {
            Object obj = this.mSettings.get(str);
            if (str.equals("autoBranch")) {
                expectSettingClass(str, obj, String.class);
                if (obj.equals("synced")) {
                    this.mCurrentGraph.setAutoBranchMode(1);
                } else if (obj.equals("unsynced")) {
                    this.mCurrentGraph.setAutoBranchMode(2);
                } else if (obj.equals("off")) {
                    this.mCurrentGraph.setAutoBranchMode(0);
                } else {
                    throw new GraphIOException("Unknown autobranch setting: " + obj + "!");
                }
            } else if (str.equals("discardUnconnectedOutputs")) {
                expectSettingClass(str, obj, Boolean.class);
                this.mCurrentGraph.setDiscardUnconnectedOutputs(((Boolean) obj).booleanValue());
            } else {
                throw new GraphIOException("Unknown @setting '" + str + "'!");
            }
        }
    }

    private void expectSettingClass(String str, Object obj, Class cls) throws GraphIOException {
        if (obj.getClass() != cls) {
            throw new GraphIOException("Setting '" + str + "' must have a value of type " + cls.getSimpleName() + ", but found a value of type " + obj.getClass().getSimpleName() + "!");
        }
    }

    private void executeCommands() throws GraphIOException {
        Iterator<Command> it = this.mCommands.iterator();
        while (it.hasNext()) {
            it.next().execute(this);
        }
    }
}
