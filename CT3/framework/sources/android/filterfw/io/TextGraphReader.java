package android.filterfw.io;

import android.filterfw.core.Filter;
import android.filterfw.core.FilterFactory;
import android.filterfw.core.FilterGraph;
import android.filterfw.core.KeyValueMap;
import android.filterfw.core.ProtocolException;
import java.util.ArrayList;
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

        public ImportPackageCommand(String packageName) {
            this.mPackageName = packageName;
        }

        @Override
        public void execute(TextGraphReader reader) throws GraphIOException {
            try {
                reader.mFactory.addPackage(this.mPackageName);
            } catch (IllegalArgumentException e) {
                throw new GraphIOException(e.getMessage());
            }
        }
    }

    private class AddLibraryCommand implements Command {
        private String mLibraryName;

        public AddLibraryCommand(String libraryName) {
            this.mLibraryName = libraryName;
        }

        @Override
        public void execute(TextGraphReader reader) {
            FilterFactory unused = reader.mFactory;
            FilterFactory.addFilterLibrary(this.mLibraryName);
        }
    }

    private class AllocateFilterCommand implements Command {
        private String mClassName;
        private String mFilterName;

        public AllocateFilterCommand(String className, String filterName) {
            this.mClassName = className;
            this.mFilterName = filterName;
        }

        @Override
        public void execute(TextGraphReader reader) throws GraphIOException {
            try {
                Filter filter = reader.mFactory.createFilterByClassName(this.mClassName, this.mFilterName);
                reader.mCurrentFilter = filter;
            } catch (IllegalArgumentException e) {
                throw new GraphIOException(e.getMessage());
            }
        }
    }

    private class InitFilterCommand implements Command {
        private KeyValueMap mParams;

        public InitFilterCommand(KeyValueMap params) {
            this.mParams = params;
        }

        @Override
        public void execute(TextGraphReader reader) throws GraphIOException {
            Filter filter = reader.mCurrentFilter;
            try {
                filter.initWithValueMap(this.mParams);
                reader.mCurrentGraph.addFilter(TextGraphReader.this.mCurrentFilter);
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

        public ConnectCommand(String sourceFilter, String sourcePort, String targetFilter, String targetName) {
            this.mSourceFilter = sourceFilter;
            this.mSourcePort = sourcePort;
            this.mTargetFilter = targetFilter;
            this.mTargetName = targetName;
        }

        @Override
        public void execute(TextGraphReader reader) {
            reader.mCurrentGraph.connect(this.mSourceFilter, this.mSourcePort, this.mTargetFilter, this.mTargetName);
        }
    }

    @Override
    public FilterGraph readGraphString(String graphString) throws GraphIOException {
        FilterGraph result = new FilterGraph();
        reset();
        this.mCurrentGraph = result;
        parseString(graphString);
        applySettings();
        executeCommands();
        reset();
        return result;
    }

    private void reset() {
        this.mCurrentGraph = null;
        this.mCurrentFilter = null;
        this.mCommands.clear();
        this.mBoundReferences = new KeyValueMap();
        this.mSettings = new KeyValueMap();
        this.mFactory = new FilterFactory();
    }

    private void parseString(String graphString) throws GraphIOException {
        Pattern commandPattern = Pattern.compile("@[a-zA-Z]+");
        Pattern curlyClosePattern = Pattern.compile("\\}");
        Pattern curlyOpenPattern = Pattern.compile("\\{");
        Pattern ignorePattern = Pattern.compile("(\\s+|//[^\\n]*\\n)+");
        Pattern packageNamePattern = Pattern.compile("[a-zA-Z\\.]+");
        Pattern libraryNamePattern = Pattern.compile("[a-zA-Z\\./:]+");
        Pattern portPattern = Pattern.compile("\\[[a-zA-Z0-9\\-_]+\\]");
        Pattern rightArrowPattern = Pattern.compile("=>");
        Pattern semicolonPattern = Pattern.compile(";");
        Pattern wordPattern = Pattern.compile("[a-zA-Z0-9\\-_]+");
        int state = 0;
        PatternScanner scanner = new PatternScanner(graphString, ignorePattern);
        String curClassName = null;
        String curSourceFilterName = null;
        String curSourcePortName = null;
        String curTargetFilterName = null;
        while (!scanner.atEnd()) {
            switch (state) {
                case 0:
                    String curCommand = scanner.eat(commandPattern, "<command>");
                    if (curCommand.equals("@import")) {
                        state = 1;
                    } else if (curCommand.equals("@library")) {
                        state = 2;
                    } else if (curCommand.equals("@filter")) {
                        state = 3;
                    } else if (curCommand.equals("@connect")) {
                        state = 8;
                    } else if (curCommand.equals("@set")) {
                        state = 13;
                    } else if (curCommand.equals("@external")) {
                        state = 14;
                    } else if (curCommand.equals("@setting")) {
                        state = 15;
                    } else {
                        throw new GraphIOException("Unknown command '" + curCommand + "'!");
                    }
                    break;
                case 1:
                    String packageName = scanner.eat(packageNamePattern, "<package-name>");
                    this.mCommands.add(new ImportPackageCommand(packageName));
                    state = 16;
                    break;
                case 2:
                    String libraryName = scanner.eat(libraryNamePattern, "<library-name>");
                    this.mCommands.add(new AddLibraryCommand(libraryName));
                    state = 16;
                    break;
                case 3:
                    curClassName = scanner.eat(wordPattern, "<class-name>");
                    state = 4;
                    break;
                case 4:
                    String curFilterName = scanner.eat(wordPattern, "<filter-name>");
                    this.mCommands.add(new AllocateFilterCommand(curClassName, curFilterName));
                    state = 5;
                    break;
                case 5:
                    scanner.eat(curlyOpenPattern, "{");
                    state = 6;
                    break;
                case 6:
                    KeyValueMap params = readKeyValueAssignments(scanner, curlyClosePattern);
                    this.mCommands.add(new InitFilterCommand(params));
                    state = 7;
                    break;
                case 7:
                    scanner.eat(curlyClosePattern, "}");
                    state = 0;
                    break;
                case 8:
                    curSourceFilterName = scanner.eat(wordPattern, "<source-filter-name>");
                    state = 9;
                    break;
                case 9:
                    String portString = scanner.eat(portPattern, "[<source-port-name>]");
                    curSourcePortName = portString.substring(1, portString.length() - 1);
                    state = 10;
                    break;
                case 10:
                    scanner.eat(rightArrowPattern, "=>");
                    state = 11;
                    break;
                case 11:
                    curTargetFilterName = scanner.eat(wordPattern, "<target-filter-name>");
                    state = 12;
                    break;
                case 12:
                    String portString2 = scanner.eat(portPattern, "[<target-port-name>]");
                    String curTargetPortName = portString2.substring(1, portString2.length() - 1);
                    this.mCommands.add(new ConnectCommand(curSourceFilterName, curSourcePortName, curTargetFilterName, curTargetPortName));
                    state = 16;
                    break;
                case 13:
                    KeyValueMap assignment = readKeyValueAssignments(scanner, semicolonPattern);
                    this.mBoundReferences.putAll(assignment);
                    state = 16;
                    break;
                case 14:
                    String externalName = scanner.eat(wordPattern, "<external-identifier>");
                    bindExternal(externalName);
                    state = 16;
                    break;
                case 15:
                    KeyValueMap setting = readKeyValueAssignments(scanner, semicolonPattern);
                    this.mSettings.putAll(setting);
                    state = 16;
                    break;
                case 16:
                    scanner.eat(semicolonPattern, ";");
                    state = 0;
                    break;
            }
        }
        if (state != 16 && state != 0) {
            throw new GraphIOException("Unexpected end of input!");
        }
    }

    @Override
    public KeyValueMap readKeyValueAssignments(String assignments) throws GraphIOException {
        Pattern ignorePattern = Pattern.compile("\\s+");
        PatternScanner scanner = new PatternScanner(assignments, ignorePattern);
        return readKeyValueAssignments(scanner, null);
    }

    private KeyValueMap readKeyValueAssignments(PatternScanner scanner, Pattern endPattern) throws GraphIOException {
        Object referencedObject;
        Pattern equalsPattern = Pattern.compile("=");
        Pattern semicolonPattern = Pattern.compile(";");
        Pattern wordPattern = Pattern.compile("[a-zA-Z]+[a-zA-Z0-9]*");
        Pattern stringPattern = Pattern.compile("'[^']*'|\\\"[^\\\"]*\\\"");
        Pattern intPattern = Pattern.compile("[0-9]+");
        Pattern floatPattern = Pattern.compile("[0-9]*\\.[0-9]+f?");
        Pattern referencePattern = Pattern.compile("\\$[a-zA-Z]+[a-zA-Z0-9]");
        Pattern booleanPattern = Pattern.compile("true|false");
        int state = 0;
        KeyValueMap newVals = new KeyValueMap();
        String curKey = null;
        while (!scanner.atEnd() && (endPattern == null || !scanner.peek(endPattern))) {
            switch (state) {
                case 0:
                    curKey = scanner.eat(wordPattern, "<identifier>");
                    state = 1;
                    break;
                case 1:
                    scanner.eat(equalsPattern, "=");
                    state = 2;
                    break;
                case 2:
                    String curValue = scanner.tryEat(stringPattern);
                    if (curValue != null) {
                        newVals.put(curKey, curValue.substring(1, curValue.length() - 1));
                    } else {
                        String curValue2 = scanner.tryEat(referencePattern);
                        if (curValue2 != null) {
                            String refName = curValue2.substring(1, curValue2.length());
                            if (this.mBoundReferences != null) {
                                referencedObject = this.mBoundReferences.get(refName);
                            } else {
                                referencedObject = null;
                            }
                            if (referencedObject == null) {
                                throw new GraphIOException("Unknown object reference to '" + refName + "'!");
                            }
                            newVals.put(curKey, referencedObject);
                        } else {
                            String curValue3 = scanner.tryEat(booleanPattern);
                            if (curValue3 != null) {
                                newVals.put(curKey, Boolean.valueOf(Boolean.parseBoolean(curValue3)));
                            } else {
                                String curValue4 = scanner.tryEat(floatPattern);
                                if (curValue4 != null) {
                                    newVals.put(curKey, Float.valueOf(Float.parseFloat(curValue4)));
                                } else {
                                    String curValue5 = scanner.tryEat(intPattern);
                                    if (curValue5 != null) {
                                        newVals.put(curKey, Integer.valueOf(Integer.parseInt(curValue5)));
                                    } else {
                                        throw new GraphIOException(scanner.unexpectedTokenMessage("<value>"));
                                    }
                                }
                            }
                        }
                    }
                    state = 3;
                    break;
                case 3:
                    scanner.eat(semicolonPattern, ";");
                    state = 0;
                    break;
            }
        }
        if (state != 0 && state != 3) {
            throw new GraphIOException("Unexpected end of assignments on line " + scanner.lineNo() + "!");
        }
        return newVals;
    }

    private void bindExternal(String name) throws GraphIOException {
        if (this.mReferences.containsKey(name)) {
            Object value = this.mReferences.get(name);
            this.mBoundReferences.put(name, value);
            return;
        }
        throw new GraphIOException("Unknown external variable '" + name + "'! You must add a reference to this external in the host program using addReference(...)!");
    }

    private void checkReferences() throws GraphIOException {
        for (String reference : this.mReferences.keySet()) {
            if (!this.mBoundReferences.containsKey(reference)) {
                throw new GraphIOException("Host program specifies reference to '" + reference + "', which is not declared @external in graph file!");
            }
        }
    }

    private void applySettings() throws GraphIOException {
        for (String setting : this.mSettings.keySet()) {
            Object value = this.mSettings.get(setting);
            if (setting.equals("autoBranch")) {
                expectSettingClass(setting, value, String.class);
                if (value.equals("synced")) {
                    this.mCurrentGraph.setAutoBranchMode(1);
                } else if (value.equals("unsynced")) {
                    this.mCurrentGraph.setAutoBranchMode(2);
                } else if (value.equals("off")) {
                    this.mCurrentGraph.setAutoBranchMode(0);
                } else {
                    throw new GraphIOException("Unknown autobranch setting: " + value + "!");
                }
            } else if (setting.equals("discardUnconnectedOutputs")) {
                expectSettingClass(setting, value, Boolean.class);
                this.mCurrentGraph.setDiscardUnconnectedOutputs(((Boolean) value).booleanValue());
            } else {
                throw new GraphIOException("Unknown @setting '" + setting + "'!");
            }
        }
    }

    private void expectSettingClass(String setting, Object value, Class expectedClass) throws GraphIOException {
        if (value.getClass() != expectedClass) {
            throw new GraphIOException("Setting '" + setting + "' must have a value of type " + expectedClass.getSimpleName() + ", but found a value of type " + value.getClass().getSimpleName() + "!");
        }
    }

    private void executeCommands() throws GraphIOException {
        for (Command command : this.mCommands) {
            command.execute(this);
        }
    }
}
