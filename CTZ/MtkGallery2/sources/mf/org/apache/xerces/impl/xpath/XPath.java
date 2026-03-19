package mf.org.apache.xerces.impl.xpath;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Vector;
import mf.org.apache.xerces.util.SymbolTable;
import mf.org.apache.xerces.util.XMLChar;
import mf.org.apache.xerces.util.XMLSymbols;
import mf.org.apache.xerces.xni.NamespaceContext;
import mf.org.apache.xerces.xni.QName;

public class XPath {
    private static final boolean DEBUG_ALL = false;
    private static final boolean DEBUG_ANY = false;
    private static final boolean DEBUG_XPATH_PARSE = false;
    protected final String fExpression;
    protected final LocationPath[] fLocationPaths;
    protected final SymbolTable fSymbolTable;

    public XPath(String xpath, SymbolTable symbolTable, NamespaceContext context) throws XPathException {
        this.fExpression = xpath;
        this.fSymbolTable = symbolTable;
        this.fLocationPaths = parseExpression(context);
    }

    public LocationPath[] getLocationPaths() {
        LocationPath[] ret = new LocationPath[this.fLocationPaths.length];
        for (int i = 0; i < this.fLocationPaths.length; i++) {
            ret[i] = (LocationPath) this.fLocationPaths[i].clone();
        }
        return ret;
    }

    public LocationPath getLocationPath() {
        return (LocationPath) this.fLocationPaths[0].clone();
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < this.fLocationPaths.length; i++) {
            if (i > 0) {
                buf.append('|');
            }
            buf.append(this.fLocationPaths[i].toString());
        }
        return buf.toString();
    }

    private static void check(boolean b) throws XPathException {
        if (!b) {
            throw new XPathException("c-general-xpath");
        }
    }

    private LocationPath buildLocationPath(Vector stepsVector) throws XPathException {
        int size = stepsVector.size();
        check(size != 0);
        Step[] steps = new Step[size];
        stepsVector.copyInto(steps);
        stepsVector.removeAllElements();
        return new LocationPath(steps);
    }

    private LocationPath[] parseExpression(NamespaceContext context) throws XPathException {
        Tokens xtokens = new Tokens(this.fSymbolTable);
        Scanner scanner = new Scanner(this.fSymbolTable) {
            @Override
            protected void addToken(Tokens tokens, int token) throws XPathException {
                if (token == 6 || token == 11 || token == 21 || token == 4 || token == 9 || token == 10 || token == 22 || token == 23 || token == 36 || token == 35 || token == 8) {
                    super.addToken(tokens, token);
                    return;
                }
                throw new XPathException("c-general-xpath");
            }
        };
        int length = this.fExpression.length();
        boolean success = scanner.scanExpr(this.fSymbolTable, xtokens, this.fExpression, 0, length);
        if (!success) {
            throw new XPathException("c-general-xpath");
        }
        Vector stepsVector = new Vector();
        ArrayList locationPathsVector = new ArrayList();
        boolean expectingStep = true;
        while (true) {
            if (xtokens.hasMore()) {
                int token = xtokens.nextToken();
                if (token == 4) {
                    check(expectingStep);
                    expectingStep = false;
                    if (stepsVector.size() == 0) {
                        Axis axis = new Axis((short) 3);
                        NodeTest nodeTest = new NodeTest((short) 3);
                        Step step = new Step(axis, nodeTest);
                        stepsVector.addElement(step);
                        if (xtokens.hasMore() && xtokens.peekToken() == 22) {
                            xtokens.nextToken();
                            Axis axis2 = new Axis((short) 4);
                            NodeTest nodeTest2 = new NodeTest((short) 3);
                            Step step2 = new Step(axis2, nodeTest2);
                            stepsVector.addElement(step2);
                            expectingStep = true;
                        }
                    }
                } else if (token == 6) {
                    check(expectingStep);
                    Step step3 = new Step(new Axis((short) 2), parseNodeTest(xtokens.nextToken(), xtokens, context));
                    stepsVector.addElement(step3);
                    expectingStep = false;
                } else {
                    switch (token) {
                        case 8:
                            throw new XPathException("c-general-xpath");
                        case 9:
                        case 10:
                        case 11:
                            check(expectingStep);
                            Step step4 = new Step(new Axis((short) 1), parseNodeTest(token, xtokens, context));
                            stepsVector.addElement(step4);
                            expectingStep = false;
                            break;
                        default:
                            switch (token) {
                                case Tokens.EXPRTOKEN_OPERATOR_SLASH:
                                    check(expectingStep ? false : true);
                                    expectingStep = true;
                                    break;
                                case Tokens.EXPRTOKEN_OPERATOR_DOUBLE_SLASH:
                                    throw new XPathException("c-general-xpath");
                                case Tokens.EXPRTOKEN_OPERATOR_UNION:
                                    check(expectingStep ? false : true);
                                    locationPathsVector.add(buildLocationPath(stepsVector));
                                    expectingStep = true;
                                    break;
                                default:
                                    switch (token) {
                                        case Tokens.EXPRTOKEN_AXISNAME_ATTRIBUTE:
                                            check(expectingStep);
                                            if (xtokens.nextToken() != 8) {
                                                throw new XPathException("c-general-xpath");
                                            }
                                            Step step5 = new Step(new Axis((short) 2), parseNodeTest(xtokens.nextToken(), xtokens, context));
                                            stepsVector.addElement(step5);
                                            expectingStep = false;
                                            break;
                                            break;
                                        case Tokens.EXPRTOKEN_AXISNAME_CHILD:
                                            check(expectingStep);
                                            if (xtokens.nextToken() != 8) {
                                                throw new XPathException("c-general-xpath");
                                            }
                                            Step step6 = new Step(new Axis((short) 1), parseNodeTest(xtokens.nextToken(), xtokens, context));
                                            stepsVector.addElement(step6);
                                            expectingStep = false;
                                            break;
                                            break;
                                        default:
                                            throw new InternalError();
                                    }
                                    break;
                            }
                            break;
                    }
                }
            } else {
                check(expectingStep ? false : true);
                locationPathsVector.add(buildLocationPath(stepsVector));
                return (LocationPath[]) locationPathsVector.toArray(new LocationPath[locationPathsVector.size()]);
            }
        }
    }

    private NodeTest parseNodeTest(int typeToken, Tokens xtokens, NamespaceContext context) throws XPathException {
        String rawname;
        switch (typeToken) {
            case 9:
                return new NodeTest((short) 2);
            case 10:
            case 11:
                String prefix = xtokens.nextTokenAsString();
                String uri = null;
                if (context != null && prefix != XMLSymbols.EMPTY_STRING) {
                    uri = context.getURI(prefix);
                }
                if (prefix != XMLSymbols.EMPTY_STRING && context != null && uri == null) {
                    throw new XPathException("c-general-xpath-ns");
                }
                if (typeToken == 10) {
                    return new NodeTest(prefix, uri);
                }
                String localpart = xtokens.nextTokenAsString();
                if (prefix == XMLSymbols.EMPTY_STRING) {
                    rawname = localpart;
                } else {
                    rawname = this.fSymbolTable.addSymbol(String.valueOf(prefix) + ':' + localpart);
                }
                return new NodeTest(new QName(prefix, localpart, rawname, uri));
            default:
                throw new XPathException("c-general-xpath");
        }
    }

    public static class LocationPath implements Cloneable {
        public final Step[] steps;

        public LocationPath(Step[] steps) {
            this.steps = steps;
        }

        protected LocationPath(LocationPath path) {
            this.steps = new Step[path.steps.length];
            for (int i = 0; i < this.steps.length; i++) {
                this.steps[i] = (Step) path.steps[i].clone();
            }
        }

        public String toString() {
            StringBuffer str = new StringBuffer();
            for (int i = 0; i < this.steps.length; i++) {
                if (i > 0 && this.steps[i - 1].axis.type != 4 && this.steps[i].axis.type != 4) {
                    str.append('/');
                }
                str.append(this.steps[i].toString());
            }
            return str.toString();
        }

        public Object clone() {
            return new LocationPath(this);
        }
    }

    public static class Step implements Cloneable {
        public final Axis axis;
        public final NodeTest nodeTest;

        public Step(Axis axis, NodeTest nodeTest) {
            this.axis = axis;
            this.nodeTest = nodeTest;
        }

        protected Step(Step step) {
            this.axis = (Axis) step.axis.clone();
            this.nodeTest = (NodeTest) step.nodeTest.clone();
        }

        public String toString() {
            if (this.axis.type == 3) {
                return ".";
            }
            if (this.axis.type == 2) {
                return "@" + this.nodeTest.toString();
            }
            if (this.axis.type == 1) {
                return this.nodeTest.toString();
            }
            if (this.axis.type == 4) {
                return "//";
            }
            return "??? (" + ((int) this.axis.type) + ')';
        }

        public Object clone() {
            return new Step(this);
        }
    }

    public static class Axis implements Cloneable {
        public static final short ATTRIBUTE = 2;
        public static final short CHILD = 1;
        public static final short DESCENDANT = 4;
        public static final short SELF = 3;
        public final short type;

        public Axis(short type) {
            this.type = type;
        }

        protected Axis(Axis axis) {
            this.type = axis.type;
        }

        public String toString() {
            switch (this.type) {
                case 1:
                    return "child";
                case 2:
                    return "attribute";
                case 3:
                    return "self";
                case 4:
                    return "descendant";
                default:
                    return "???";
            }
        }

        public Object clone() {
            return new Axis(this);
        }
    }

    public static class NodeTest implements Cloneable {
        public static final short NAMESPACE = 4;
        public static final short NODE = 3;
        public static final short QNAME = 1;
        public static final short WILDCARD = 2;
        public final QName name;
        public final short type;

        public NodeTest(short type) {
            this.name = new QName();
            this.type = type;
        }

        public NodeTest(QName name) {
            this.name = new QName();
            this.type = (short) 1;
            this.name.setValues(name);
        }

        public NodeTest(String prefix, String uri) {
            this.name = new QName();
            this.type = (short) 4;
            this.name.setValues(prefix, null, null, uri);
        }

        public NodeTest(NodeTest nodeTest) {
            this.name = new QName();
            this.type = nodeTest.type;
            this.name.setValues(nodeTest.name);
        }

        public String toString() {
            switch (this.type) {
                case 1:
                    if (this.name.prefix.length() != 0) {
                        if (this.name.uri != null) {
                            return String.valueOf(this.name.prefix) + ':' + this.name.localpart;
                        }
                        return "{" + this.name.uri + '}' + this.name.prefix + ':' + this.name.localpart;
                    }
                    return this.name.localpart;
                case 2:
                    return "*";
                case 3:
                    return "node()";
                case 4:
                    if (this.name.prefix.length() != 0) {
                        if (this.name.uri != null) {
                            return String.valueOf(this.name.prefix) + ":*";
                        }
                        return "{" + this.name.uri + '}' + this.name.prefix + ":*";
                    }
                    return "???:*";
                default:
                    return "???";
            }
        }

        public Object clone() {
            return new NodeTest(this);
        }
    }

    private static final class Tokens {
        static final boolean DUMP_TOKENS = false;
        public static final int EXPRTOKEN_ATSIGN = 6;
        public static final int EXPRTOKEN_AXISNAME_ANCESTOR = 33;
        public static final int EXPRTOKEN_AXISNAME_ANCESTOR_OR_SELF = 34;
        public static final int EXPRTOKEN_AXISNAME_ATTRIBUTE = 35;
        public static final int EXPRTOKEN_AXISNAME_CHILD = 36;
        public static final int EXPRTOKEN_AXISNAME_DESCENDANT = 37;
        public static final int EXPRTOKEN_AXISNAME_DESCENDANT_OR_SELF = 38;
        public static final int EXPRTOKEN_AXISNAME_FOLLOWING = 39;
        public static final int EXPRTOKEN_AXISNAME_FOLLOWING_SIBLING = 40;
        public static final int EXPRTOKEN_AXISNAME_NAMESPACE = 41;
        public static final int EXPRTOKEN_AXISNAME_PARENT = 42;
        public static final int EXPRTOKEN_AXISNAME_PRECEDING = 43;
        public static final int EXPRTOKEN_AXISNAME_PRECEDING_SIBLING = 44;
        public static final int EXPRTOKEN_AXISNAME_SELF = 45;
        public static final int EXPRTOKEN_CLOSE_BRACKET = 3;
        public static final int EXPRTOKEN_CLOSE_PAREN = 1;
        public static final int EXPRTOKEN_COMMA = 7;
        public static final int EXPRTOKEN_DOUBLE_COLON = 8;
        public static final int EXPRTOKEN_DOUBLE_PERIOD = 5;
        public static final int EXPRTOKEN_FUNCTION_NAME = 32;
        public static final int EXPRTOKEN_LITERAL = 46;
        public static final int EXPRTOKEN_NAMETEST_ANY = 9;
        public static final int EXPRTOKEN_NAMETEST_NAMESPACE = 10;
        public static final int EXPRTOKEN_NAMETEST_QNAME = 11;
        public static final int EXPRTOKEN_NODETYPE_COMMENT = 12;
        public static final int EXPRTOKEN_NODETYPE_NODE = 15;
        public static final int EXPRTOKEN_NODETYPE_PI = 14;
        public static final int EXPRTOKEN_NODETYPE_TEXT = 13;
        public static final int EXPRTOKEN_NUMBER = 47;
        public static final int EXPRTOKEN_OPEN_BRACKET = 2;
        public static final int EXPRTOKEN_OPEN_PAREN = 0;
        public static final int EXPRTOKEN_OPERATOR_AND = 16;
        public static final int EXPRTOKEN_OPERATOR_DIV = 19;
        public static final int EXPRTOKEN_OPERATOR_DOUBLE_SLASH = 22;
        public static final int EXPRTOKEN_OPERATOR_EQUAL = 26;
        public static final int EXPRTOKEN_OPERATOR_GREATER = 30;
        public static final int EXPRTOKEN_OPERATOR_GREATER_EQUAL = 31;
        public static final int EXPRTOKEN_OPERATOR_LESS = 28;
        public static final int EXPRTOKEN_OPERATOR_LESS_EQUAL = 29;
        public static final int EXPRTOKEN_OPERATOR_MINUS = 25;
        public static final int EXPRTOKEN_OPERATOR_MOD = 18;
        public static final int EXPRTOKEN_OPERATOR_MULT = 20;
        public static final int EXPRTOKEN_OPERATOR_NOT_EQUAL = 27;
        public static final int EXPRTOKEN_OPERATOR_OR = 17;
        public static final int EXPRTOKEN_OPERATOR_PLUS = 24;
        public static final int EXPRTOKEN_OPERATOR_SLASH = 21;
        public static final int EXPRTOKEN_OPERATOR_UNION = 23;
        public static final int EXPRTOKEN_PERIOD = 4;
        public static final int EXPRTOKEN_VARIABLE_REFERENCE = 48;
        private static final int INITIAL_TOKEN_COUNT = 256;
        private static final String[] fgTokenNames = {"EXPRTOKEN_OPEN_PAREN", "EXPRTOKEN_CLOSE_PAREN", "EXPRTOKEN_OPEN_BRACKET", "EXPRTOKEN_CLOSE_BRACKET", "EXPRTOKEN_PERIOD", "EXPRTOKEN_DOUBLE_PERIOD", "EXPRTOKEN_ATSIGN", "EXPRTOKEN_COMMA", "EXPRTOKEN_DOUBLE_COLON", "EXPRTOKEN_NAMETEST_ANY", "EXPRTOKEN_NAMETEST_NAMESPACE", "EXPRTOKEN_NAMETEST_QNAME", "EXPRTOKEN_NODETYPE_COMMENT", "EXPRTOKEN_NODETYPE_TEXT", "EXPRTOKEN_NODETYPE_PI", "EXPRTOKEN_NODETYPE_NODE", "EXPRTOKEN_OPERATOR_AND", "EXPRTOKEN_OPERATOR_OR", "EXPRTOKEN_OPERATOR_MOD", "EXPRTOKEN_OPERATOR_DIV", "EXPRTOKEN_OPERATOR_MULT", "EXPRTOKEN_OPERATOR_SLASH", "EXPRTOKEN_OPERATOR_DOUBLE_SLASH", "EXPRTOKEN_OPERATOR_UNION", "EXPRTOKEN_OPERATOR_PLUS", "EXPRTOKEN_OPERATOR_MINUS", "EXPRTOKEN_OPERATOR_EQUAL", "EXPRTOKEN_OPERATOR_NOT_EQUAL", "EXPRTOKEN_OPERATOR_LESS", "EXPRTOKEN_OPERATOR_LESS_EQUAL", "EXPRTOKEN_OPERATOR_GREATER", "EXPRTOKEN_OPERATOR_GREATER_EQUAL", "EXPRTOKEN_FUNCTION_NAME", "EXPRTOKEN_AXISNAME_ANCESTOR", "EXPRTOKEN_AXISNAME_ANCESTOR_OR_SELF", "EXPRTOKEN_AXISNAME_ATTRIBUTE", "EXPRTOKEN_AXISNAME_CHILD", "EXPRTOKEN_AXISNAME_DESCENDANT", "EXPRTOKEN_AXISNAME_DESCENDANT_OR_SELF", "EXPRTOKEN_AXISNAME_FOLLOWING", "EXPRTOKEN_AXISNAME_FOLLOWING_SIBLING", "EXPRTOKEN_AXISNAME_NAMESPACE", "EXPRTOKEN_AXISNAME_PARENT", "EXPRTOKEN_AXISNAME_PRECEDING", "EXPRTOKEN_AXISNAME_PRECEDING_SIBLING", "EXPRTOKEN_AXISNAME_SELF", "EXPRTOKEN_LITERAL", "EXPRTOKEN_NUMBER", "EXPRTOKEN_VARIABLE_REFERENCE"};
        private int fCurrentTokenIndex;
        private SymbolTable fSymbolTable;
        private int[] fTokens = new int[256];
        private int fTokenCount = 0;
        private Hashtable fSymbolMapping = new Hashtable();
        private Hashtable fTokenNames = new Hashtable();

        public Tokens(SymbolTable symbolTable) {
            this.fSymbolTable = symbolTable;
            String[] symbols = {"ancestor", "ancestor-or-self", "attribute", "child", "descendant", "descendant-or-self", "following", "following-sibling", "namespace", "parent", "preceding", "preceding-sibling", "self"};
            for (int i = 0; i < symbols.length; i++) {
                this.fSymbolMapping.put(this.fSymbolTable.addSymbol(symbols[i]), new Integer(i));
            }
            this.fTokenNames.put(new Integer(0), "EXPRTOKEN_OPEN_PAREN");
            this.fTokenNames.put(new Integer(1), "EXPRTOKEN_CLOSE_PAREN");
            this.fTokenNames.put(new Integer(2), "EXPRTOKEN_OPEN_BRACKET");
            this.fTokenNames.put(new Integer(3), "EXPRTOKEN_CLOSE_BRACKET");
            this.fTokenNames.put(new Integer(4), "EXPRTOKEN_PERIOD");
            this.fTokenNames.put(new Integer(5), "EXPRTOKEN_DOUBLE_PERIOD");
            this.fTokenNames.put(new Integer(6), "EXPRTOKEN_ATSIGN");
            this.fTokenNames.put(new Integer(7), "EXPRTOKEN_COMMA");
            this.fTokenNames.put(new Integer(8), "EXPRTOKEN_DOUBLE_COLON");
            this.fTokenNames.put(new Integer(9), "EXPRTOKEN_NAMETEST_ANY");
            this.fTokenNames.put(new Integer(10), "EXPRTOKEN_NAMETEST_NAMESPACE");
            this.fTokenNames.put(new Integer(11), "EXPRTOKEN_NAMETEST_QNAME");
            this.fTokenNames.put(new Integer(12), "EXPRTOKEN_NODETYPE_COMMENT");
            this.fTokenNames.put(new Integer(13), "EXPRTOKEN_NODETYPE_TEXT");
            this.fTokenNames.put(new Integer(14), "EXPRTOKEN_NODETYPE_PI");
            this.fTokenNames.put(new Integer(15), "EXPRTOKEN_NODETYPE_NODE");
            this.fTokenNames.put(new Integer(16), "EXPRTOKEN_OPERATOR_AND");
            this.fTokenNames.put(new Integer(17), "EXPRTOKEN_OPERATOR_OR");
            this.fTokenNames.put(new Integer(18), "EXPRTOKEN_OPERATOR_MOD");
            this.fTokenNames.put(new Integer(19), "EXPRTOKEN_OPERATOR_DIV");
            this.fTokenNames.put(new Integer(20), "EXPRTOKEN_OPERATOR_MULT");
            this.fTokenNames.put(new Integer(21), "EXPRTOKEN_OPERATOR_SLASH");
            this.fTokenNames.put(new Integer(22), "EXPRTOKEN_OPERATOR_DOUBLE_SLASH");
            this.fTokenNames.put(new Integer(23), "EXPRTOKEN_OPERATOR_UNION");
            this.fTokenNames.put(new Integer(24), "EXPRTOKEN_OPERATOR_PLUS");
            this.fTokenNames.put(new Integer(25), "EXPRTOKEN_OPERATOR_MINUS");
            this.fTokenNames.put(new Integer(26), "EXPRTOKEN_OPERATOR_EQUAL");
            this.fTokenNames.put(new Integer(27), "EXPRTOKEN_OPERATOR_NOT_EQUAL");
            this.fTokenNames.put(new Integer(28), "EXPRTOKEN_OPERATOR_LESS");
            this.fTokenNames.put(new Integer(29), "EXPRTOKEN_OPERATOR_LESS_EQUAL");
            this.fTokenNames.put(new Integer(30), "EXPRTOKEN_OPERATOR_GREATER");
            this.fTokenNames.put(new Integer(31), "EXPRTOKEN_OPERATOR_GREATER_EQUAL");
            this.fTokenNames.put(new Integer(32), "EXPRTOKEN_FUNCTION_NAME");
            this.fTokenNames.put(new Integer(33), "EXPRTOKEN_AXISNAME_ANCESTOR");
            this.fTokenNames.put(new Integer(34), "EXPRTOKEN_AXISNAME_ANCESTOR_OR_SELF");
            this.fTokenNames.put(new Integer(35), "EXPRTOKEN_AXISNAME_ATTRIBUTE");
            this.fTokenNames.put(new Integer(36), "EXPRTOKEN_AXISNAME_CHILD");
            this.fTokenNames.put(new Integer(37), "EXPRTOKEN_AXISNAME_DESCENDANT");
            this.fTokenNames.put(new Integer(38), "EXPRTOKEN_AXISNAME_DESCENDANT_OR_SELF");
            this.fTokenNames.put(new Integer(39), "EXPRTOKEN_AXISNAME_FOLLOWING");
            this.fTokenNames.put(new Integer(40), "EXPRTOKEN_AXISNAME_FOLLOWING_SIBLING");
            this.fTokenNames.put(new Integer(41), "EXPRTOKEN_AXISNAME_NAMESPACE");
            this.fTokenNames.put(new Integer(42), "EXPRTOKEN_AXISNAME_PARENT");
            this.fTokenNames.put(new Integer(43), "EXPRTOKEN_AXISNAME_PRECEDING");
            this.fTokenNames.put(new Integer(44), "EXPRTOKEN_AXISNAME_PRECEDING_SIBLING");
            this.fTokenNames.put(new Integer(45), "EXPRTOKEN_AXISNAME_SELF");
            this.fTokenNames.put(new Integer(46), "EXPRTOKEN_LITERAL");
            this.fTokenNames.put(new Integer(47), "EXPRTOKEN_NUMBER");
            this.fTokenNames.put(new Integer(48), "EXPRTOKEN_VARIABLE_REFERENCE");
        }

        public String getTokenString(int token) {
            return (String) this.fTokenNames.get(new Integer(token));
        }

        public void addToken(String tokenStr) {
            Integer tokenInt = (Integer) this.fTokenNames.get(tokenStr);
            if (tokenInt == null) {
                tokenInt = new Integer(this.fTokenNames.size());
                this.fTokenNames.put(tokenInt, tokenStr);
            }
            addToken(tokenInt.intValue());
        }

        public void addToken(int token) {
            try {
                this.fTokens[this.fTokenCount] = token;
            } catch (ArrayIndexOutOfBoundsException e) {
                int[] oldList = this.fTokens;
                this.fTokens = new int[this.fTokenCount << 1];
                System.arraycopy(oldList, 0, this.fTokens, 0, this.fTokenCount);
                this.fTokens[this.fTokenCount] = token;
            }
            this.fTokenCount++;
        }

        public void rewind() {
            this.fCurrentTokenIndex = 0;
        }

        public boolean hasMore() {
            return this.fCurrentTokenIndex < this.fTokenCount;
        }

        public int nextToken() throws XPathException {
            if (this.fCurrentTokenIndex == this.fTokenCount) {
                throw new XPathException("c-general-xpath");
            }
            int[] iArr = this.fTokens;
            int i = this.fCurrentTokenIndex;
            this.fCurrentTokenIndex = i + 1;
            return iArr[i];
        }

        public int peekToken() throws XPathException {
            if (this.fCurrentTokenIndex == this.fTokenCount) {
                throw new XPathException("c-general-xpath");
            }
            return this.fTokens[this.fCurrentTokenIndex];
        }

        public String nextTokenAsString() throws XPathException {
            String s = getTokenString(nextToken());
            if (s == null) {
                throw new XPathException("c-general-xpath");
            }
            return s;
        }

        public void dumpTokens() {
            int i = 0;
            while (i < this.fTokenCount) {
                switch (this.fTokens[i]) {
                    case 0:
                        System.out.print("<OPEN_PAREN/>");
                        break;
                    case 1:
                        System.out.print("<CLOSE_PAREN/>");
                        break;
                    case 2:
                        System.out.print("<OPEN_BRACKET/>");
                        break;
                    case 3:
                        System.out.print("<CLOSE_BRACKET/>");
                        break;
                    case 4:
                        System.out.print("<PERIOD/>");
                        break;
                    case 5:
                        System.out.print("<DOUBLE_PERIOD/>");
                        break;
                    case 6:
                        System.out.print("<ATSIGN/>");
                        break;
                    case 7:
                        System.out.print("<COMMA/>");
                        break;
                    case 8:
                        System.out.print("<DOUBLE_COLON/>");
                        break;
                    case 9:
                        System.out.print("<NAMETEST_ANY/>");
                        break;
                    case 10:
                        System.out.print("<NAMETEST_NAMESPACE");
                        PrintStream printStream = System.out;
                        StringBuilder sb = new StringBuilder(" prefix=\"");
                        i++;
                        sb.append(getTokenString(this.fTokens[i]));
                        sb.append("\"");
                        printStream.print(sb.toString());
                        System.out.print("/>");
                        break;
                    case 11:
                        System.out.print("<NAMETEST_QNAME");
                        int i2 = i + 1;
                        if (this.fTokens[i2] != -1) {
                            System.out.print(" prefix=\"" + getTokenString(this.fTokens[i2]) + "\"");
                        }
                        PrintStream printStream2 = System.out;
                        StringBuilder sb2 = new StringBuilder(" localpart=\"");
                        i = i2 + 1;
                        sb2.append(getTokenString(this.fTokens[i]));
                        sb2.append("\"");
                        printStream2.print(sb2.toString());
                        System.out.print("/>");
                        break;
                    case 12:
                        System.out.print("<NODETYPE_COMMENT/>");
                        break;
                    case 13:
                        System.out.print("<NODETYPE_TEXT/>");
                        break;
                    case 14:
                        System.out.print("<NODETYPE_PI/>");
                        break;
                    case 15:
                        System.out.print("<NODETYPE_NODE/>");
                        break;
                    case 16:
                        System.out.print("<OPERATOR_AND/>");
                        break;
                    case 17:
                        System.out.print("<OPERATOR_OR/>");
                        break;
                    case 18:
                        System.out.print("<OPERATOR_MOD/>");
                        break;
                    case EXPRTOKEN_OPERATOR_DIV:
                        System.out.print("<OPERATOR_DIV/>");
                        break;
                    case EXPRTOKEN_OPERATOR_MULT:
                        System.out.print("<OPERATOR_MULT/>");
                        break;
                    case EXPRTOKEN_OPERATOR_SLASH:
                        System.out.print("<OPERATOR_SLASH/>");
                        if (i + 1 < this.fTokenCount) {
                            System.out.println();
                            System.out.print("  ");
                        }
                        break;
                    case EXPRTOKEN_OPERATOR_DOUBLE_SLASH:
                        System.out.print("<OPERATOR_DOUBLE_SLASH/>");
                        break;
                    case EXPRTOKEN_OPERATOR_UNION:
                        System.out.print("<OPERATOR_UNION/>");
                        break;
                    case EXPRTOKEN_OPERATOR_PLUS:
                        System.out.print("<OPERATOR_PLUS/>");
                        break;
                    case 25:
                        System.out.print("<OPERATOR_MINUS/>");
                        break;
                    case EXPRTOKEN_OPERATOR_EQUAL:
                        System.out.print("<OPERATOR_EQUAL/>");
                        break;
                    case EXPRTOKEN_OPERATOR_NOT_EQUAL:
                        System.out.print("<OPERATOR_NOT_EQUAL/>");
                        break;
                    case EXPRTOKEN_OPERATOR_LESS:
                        System.out.print("<OPERATOR_LESS/>");
                        break;
                    case EXPRTOKEN_OPERATOR_LESS_EQUAL:
                        System.out.print("<OPERATOR_LESS_EQUAL/>");
                        break;
                    case EXPRTOKEN_OPERATOR_GREATER:
                        System.out.print("<OPERATOR_GREATER/>");
                        break;
                    case EXPRTOKEN_OPERATOR_GREATER_EQUAL:
                        System.out.print("<OPERATOR_GREATER_EQUAL/>");
                        break;
                    case 32:
                        System.out.print("<FUNCTION_NAME");
                        int i3 = i + 1;
                        if (this.fTokens[i3] != -1) {
                            System.out.print(" prefix=\"" + getTokenString(this.fTokens[i3]) + "\"");
                        }
                        PrintStream printStream3 = System.out;
                        StringBuilder sb3 = new StringBuilder(" localpart=\"");
                        i = i3 + 1;
                        sb3.append(getTokenString(this.fTokens[i]));
                        sb3.append("\"");
                        printStream3.print(sb3.toString());
                        System.out.print("/>");
                        break;
                    case EXPRTOKEN_AXISNAME_ANCESTOR:
                        System.out.print("<AXISNAME_ANCESTOR/>");
                        break;
                    case EXPRTOKEN_AXISNAME_ANCESTOR_OR_SELF:
                        System.out.print("<AXISNAME_ANCESTOR_OR_SELF/>");
                        break;
                    case EXPRTOKEN_AXISNAME_ATTRIBUTE:
                        System.out.print("<AXISNAME_ATTRIBUTE/>");
                        break;
                    case EXPRTOKEN_AXISNAME_CHILD:
                        System.out.print("<AXISNAME_CHILD/>");
                        break;
                    case EXPRTOKEN_AXISNAME_DESCENDANT:
                        System.out.print("<AXISNAME_DESCENDANT/>");
                        break;
                    case EXPRTOKEN_AXISNAME_DESCENDANT_OR_SELF:
                        System.out.print("<AXISNAME_DESCENDANT_OR_SELF/>");
                        break;
                    case EXPRTOKEN_AXISNAME_FOLLOWING:
                        System.out.print("<AXISNAME_FOLLOWING/>");
                        break;
                    case EXPRTOKEN_AXISNAME_FOLLOWING_SIBLING:
                        System.out.print("<AXISNAME_FOLLOWING_SIBLING/>");
                        break;
                    case EXPRTOKEN_AXISNAME_NAMESPACE:
                        System.out.print("<AXISNAME_NAMESPACE/>");
                        break;
                    case EXPRTOKEN_AXISNAME_PARENT:
                        System.out.print("<AXISNAME_PARENT/>");
                        break;
                    case EXPRTOKEN_AXISNAME_PRECEDING:
                        System.out.print("<AXISNAME_PRECEDING/>");
                        break;
                    case EXPRTOKEN_AXISNAME_PRECEDING_SIBLING:
                        System.out.print("<AXISNAME_PRECEDING_SIBLING/>");
                        break;
                    case EXPRTOKEN_AXISNAME_SELF:
                        System.out.print("<AXISNAME_SELF/>");
                        break;
                    case EXPRTOKEN_LITERAL:
                        System.out.print("<LITERAL");
                        PrintStream printStream4 = System.out;
                        StringBuilder sb4 = new StringBuilder(" value=\"");
                        i++;
                        sb4.append(getTokenString(this.fTokens[i]));
                        sb4.append("\"");
                        printStream4.print(sb4.toString());
                        System.out.print("/>");
                        break;
                    case EXPRTOKEN_NUMBER:
                        System.out.print("<NUMBER");
                        PrintStream printStream5 = System.out;
                        StringBuilder sb5 = new StringBuilder(" whole=\"");
                        int i4 = i + 1;
                        sb5.append(getTokenString(this.fTokens[i4]));
                        sb5.append("\"");
                        printStream5.print(sb5.toString());
                        PrintStream printStream6 = System.out;
                        StringBuilder sb6 = new StringBuilder(" part=\"");
                        i = i4 + 1;
                        sb6.append(getTokenString(this.fTokens[i]));
                        sb6.append("\"");
                        printStream6.print(sb6.toString());
                        System.out.print("/>");
                        break;
                    case 48:
                        System.out.print("<VARIABLE_REFERENCE");
                        int i5 = i + 1;
                        if (this.fTokens[i5] != -1) {
                            System.out.print(" prefix=\"" + getTokenString(this.fTokens[i5]) + "\"");
                        }
                        PrintStream printStream7 = System.out;
                        StringBuilder sb7 = new StringBuilder(" localpart=\"");
                        i = i5 + 1;
                        sb7.append(getTokenString(this.fTokens[i]));
                        sb7.append("\"");
                        printStream7.print(sb7.toString());
                        System.out.print("/>");
                        break;
                    default:
                        System.out.println("<???/>");
                        break;
                }
                i++;
            }
            System.out.println();
        }
    }

    private static class Scanner {
        private static final byte CHARTYPE_INVALID = 0;
        private static final byte CHARTYPE_NONASCII = 25;
        private SymbolTable fSymbolTable;
        private static final byte CHARTYPE_WHITESPACE = 2;
        private static final byte CHARTYPE_EXCLAMATION = 3;
        private static final byte CHARTYPE_QUOTE = 4;
        private static final byte CHARTYPE_OTHER = 1;
        private static final byte CHARTYPE_DOLLAR = 5;
        private static final byte CHARTYPE_OPEN_PAREN = 6;
        private static final byte CHARTYPE_CLOSE_PAREN = 7;
        private static final byte CHARTYPE_STAR = 8;
        private static final byte CHARTYPE_PLUS = 9;
        private static final byte CHARTYPE_COMMA = 10;
        private static final byte CHARTYPE_MINUS = 11;
        private static final byte CHARTYPE_PERIOD = 12;
        private static final byte CHARTYPE_SLASH = 13;
        private static final byte CHARTYPE_DIGIT = 14;
        private static final byte CHARTYPE_COLON = 15;
        private static final byte CHARTYPE_LESS = 16;
        private static final byte CHARTYPE_EQUAL = 17;
        private static final byte CHARTYPE_GREATER = 18;
        private static final byte CHARTYPE_ATSIGN = 19;
        private static final byte CHARTYPE_LETTER = 20;
        private static final byte CHARTYPE_OPEN_BRACKET = 21;
        private static final byte CHARTYPE_CLOSE_BRACKET = 22;
        private static final byte CHARTYPE_UNDERSCORE = 23;
        private static final byte CHARTYPE_UNION = 24;
        private static final byte[] fASCIICharMap = {0, 0, 0, 0, 0, 0, 0, 0, 0, CHARTYPE_WHITESPACE, CHARTYPE_WHITESPACE, 0, 0, CHARTYPE_WHITESPACE, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, CHARTYPE_WHITESPACE, CHARTYPE_EXCLAMATION, CHARTYPE_QUOTE, CHARTYPE_OTHER, CHARTYPE_DOLLAR, CHARTYPE_OTHER, CHARTYPE_OTHER, CHARTYPE_QUOTE, CHARTYPE_OPEN_PAREN, CHARTYPE_CLOSE_PAREN, CHARTYPE_STAR, CHARTYPE_PLUS, CHARTYPE_COMMA, CHARTYPE_MINUS, CHARTYPE_PERIOD, CHARTYPE_SLASH, CHARTYPE_DIGIT, CHARTYPE_DIGIT, CHARTYPE_DIGIT, CHARTYPE_DIGIT, CHARTYPE_DIGIT, CHARTYPE_DIGIT, CHARTYPE_DIGIT, CHARTYPE_DIGIT, CHARTYPE_DIGIT, CHARTYPE_DIGIT, CHARTYPE_COLON, CHARTYPE_OTHER, CHARTYPE_LESS, CHARTYPE_EQUAL, CHARTYPE_GREATER, CHARTYPE_OTHER, CHARTYPE_ATSIGN, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_OPEN_BRACKET, CHARTYPE_OTHER, CHARTYPE_CLOSE_BRACKET, CHARTYPE_OTHER, CHARTYPE_UNDERSCORE, CHARTYPE_OTHER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_OTHER, CHARTYPE_UNION, CHARTYPE_OTHER, CHARTYPE_OTHER, CHARTYPE_OTHER};
        private static final String fAndSymbol = "and".intern();
        private static final String fOrSymbol = "or".intern();
        private static final String fModSymbol = "mod".intern();
        private static final String fDivSymbol = "div".intern();
        private static final String fCommentSymbol = "comment".intern();
        private static final String fTextSymbol = "text".intern();
        private static final String fPISymbol = "processing-instruction".intern();
        private static final String fNodeSymbol = "node".intern();
        private static final String fAncestorSymbol = "ancestor".intern();
        private static final String fAncestorOrSelfSymbol = "ancestor-or-self".intern();
        private static final String fAttributeSymbol = "attribute".intern();
        private static final String fChildSymbol = "child".intern();
        private static final String fDescendantSymbol = "descendant".intern();
        private static final String fDescendantOrSelfSymbol = "descendant-or-self".intern();
        private static final String fFollowingSymbol = "following".intern();
        private static final String fFollowingSiblingSymbol = "following-sibling".intern();
        private static final String fNamespaceSymbol = "namespace".intern();
        private static final String fParentSymbol = "parent".intern();
        private static final String fPrecedingSymbol = "preceding".intern();
        private static final String fPrecedingSiblingSymbol = "preceding-sibling".intern();
        private static final String fSelfSymbol = "self".intern();

        public Scanner(SymbolTable symbolTable) {
            this.fSymbolTable = symbolTable;
        }

        public boolean scanExpr(SymbolTable symbolTable, Tokens tokens, String data, int currentOffset, int endOffset) throws XPathException {
            int ch;
            String prefixHandle;
            int ch2;
            int currentOffset2 = currentOffset;
            boolean starIsMultiplyOperator = false;
            while (currentOffset2 != endOffset) {
                int ch3 = data.charAt(currentOffset2);
                while (true) {
                    if ((ch3 == 32 || ch3 == 10 || ch3 == 9 || ch3 == 13) && (currentOffset2 = currentOffset2 + 1) != endOffset) {
                        ch3 = data.charAt(currentOffset2);
                    }
                }
                if (currentOffset2 == endOffset) {
                    return true;
                }
                byte chartype = ch3 >= 128 ? CHARTYPE_NONASCII : fASCIICharMap[ch3];
                switch (chartype) {
                    case 3:
                        int currentOffset3 = currentOffset2 + 1;
                        if (currentOffset3 == endOffset) {
                            return false;
                        }
                        int ch4 = data.charAt(currentOffset3);
                        if (ch4 != 61) {
                            return false;
                        }
                        addToken(tokens, 27);
                        starIsMultiplyOperator = false;
                        currentOffset2 = currentOffset3 + 1;
                        if (currentOffset2 == endOffset) {
                        }
                        break;
                    case 4:
                        int qchar = ch3;
                        int currentOffset4 = currentOffset2 + 1;
                        if (currentOffset4 == endOffset) {
                            return false;
                        }
                        int ch5 = data.charAt(currentOffset4);
                        while (ch5 != qchar) {
                            currentOffset4++;
                            if (currentOffset4 == endOffset) {
                                return false;
                            }
                            ch5 = data.charAt(currentOffset4);
                        }
                        int litLength = currentOffset4 - currentOffset4;
                        addToken(tokens, 46);
                        starIsMultiplyOperator = true;
                        tokens.addToken(symbolTable.addSymbol(data.substring(currentOffset4, currentOffset4 + litLength)));
                        currentOffset2 = currentOffset4 + 1;
                        if (currentOffset2 == endOffset) {
                        }
                        break;
                    case 5:
                        int currentOffset5 = currentOffset2 + 1;
                        if (currentOffset5 == endOffset || (currentOffset2 = scanNCName(data, endOffset, currentOffset5)) == currentOffset5) {
                            return false;
                        }
                        if (currentOffset2 < endOffset) {
                            ch = data.charAt(currentOffset2);
                        } else {
                            ch = -1;
                        }
                        String nameHandle = symbolTable.addSymbol(data.substring(currentOffset5, currentOffset2));
                        if (ch != 58) {
                            prefixHandle = XMLSymbols.EMPTY_STRING;
                        } else {
                            prefixHandle = nameHandle;
                            int currentOffset6 = currentOffset2 + 1;
                            if (currentOffset6 == endOffset || (currentOffset2 = scanNCName(data, endOffset, currentOffset6)) == currentOffset6) {
                                return false;
                            }
                            if (currentOffset2 < endOffset) {
                                data.charAt(currentOffset2);
                            }
                            nameHandle = symbolTable.addSymbol(data.substring(currentOffset6, currentOffset2));
                        }
                        addToken(tokens, 48);
                        starIsMultiplyOperator = true;
                        tokens.addToken(prefixHandle);
                        tokens.addToken(nameHandle);
                        break;
                    case 6:
                        addToken(tokens, 0);
                        starIsMultiplyOperator = false;
                        currentOffset2++;
                        if (currentOffset2 == endOffset) {
                        }
                        break;
                    case 7:
                        addToken(tokens, 1);
                        starIsMultiplyOperator = true;
                        currentOffset2++;
                        if (currentOffset2 == endOffset) {
                        }
                        break;
                    case 8:
                        if (starIsMultiplyOperator) {
                            addToken(tokens, 20);
                            starIsMultiplyOperator = false;
                        } else {
                            addToken(tokens, 9);
                            starIsMultiplyOperator = true;
                        }
                        currentOffset2++;
                        if (currentOffset2 == endOffset) {
                        }
                        break;
                    case 9:
                        addToken(tokens, 24);
                        starIsMultiplyOperator = false;
                        currentOffset2++;
                        if (currentOffset2 == endOffset) {
                        }
                        break;
                    case 10:
                        addToken(tokens, 7);
                        starIsMultiplyOperator = false;
                        currentOffset2++;
                        if (currentOffset2 == endOffset) {
                        }
                        break;
                    case 11:
                        addToken(tokens, 25);
                        starIsMultiplyOperator = false;
                        currentOffset2++;
                        if (currentOffset2 == endOffset) {
                        }
                        break;
                    case 12:
                        int ch6 = currentOffset2 + 1;
                        if (ch6 == endOffset) {
                            addToken(tokens, 4);
                            starIsMultiplyOperator = true;
                            currentOffset2++;
                        } else {
                            int ch7 = data.charAt(currentOffset2 + 1);
                            if (ch7 == 46) {
                                addToken(tokens, 5);
                                starIsMultiplyOperator = true;
                                currentOffset2 += 2;
                            } else if (ch7 >= 48 && ch7 <= 57) {
                                addToken(tokens, 47);
                                starIsMultiplyOperator = true;
                                currentOffset2 = scanNumber(tokens, data, endOffset, currentOffset2);
                            } else if (ch7 == 47) {
                                addToken(tokens, 4);
                                starIsMultiplyOperator = true;
                                currentOffset2++;
                            } else if (ch7 == 124) {
                                addToken(tokens, 4);
                                starIsMultiplyOperator = true;
                                currentOffset2++;
                            } else if (ch7 == 32 || ch7 == 10 || ch7 == 9 || ch7 == 13) {
                                while (true) {
                                    currentOffset2++;
                                    if (currentOffset2 != endOffset) {
                                        ch7 = data.charAt(currentOffset2);
                                        if (ch7 != 32 && ch7 != 10 && ch7 != 9) {
                                            if (ch7 != 13) {
                                            }
                                        }
                                    }
                                }
                                if (currentOffset2 == endOffset || ch7 == 124) {
                                    addToken(tokens, 4);
                                    starIsMultiplyOperator = true;
                                } else {
                                    throw new XPathException("c-general-xpath");
                                }
                            } else {
                                throw new XPathException("c-general-xpath");
                            }
                            if (currentOffset2 == endOffset) {
                            }
                        }
                        break;
                    case 13:
                        currentOffset2++;
                        if (currentOffset2 == endOffset) {
                            addToken(tokens, 21);
                            starIsMultiplyOperator = false;
                        } else {
                            int ch8 = data.charAt(currentOffset2);
                            if (ch8 == 47) {
                                addToken(tokens, 22);
                                starIsMultiplyOperator = false;
                                currentOffset2++;
                                if (currentOffset2 == endOffset) {
                                }
                            } else {
                                addToken(tokens, 21);
                                starIsMultiplyOperator = false;
                            }
                        }
                        break;
                    case 14:
                        addToken(tokens, 47);
                        starIsMultiplyOperator = true;
                        currentOffset2 = scanNumber(tokens, data, endOffset, currentOffset2);
                        break;
                    case 15:
                        int currentOffset7 = currentOffset2 + 1;
                        if (currentOffset7 == endOffset) {
                            return false;
                        }
                        int ch9 = data.charAt(currentOffset7);
                        if (ch9 != 58) {
                            return false;
                        }
                        addToken(tokens, 8);
                        starIsMultiplyOperator = false;
                        currentOffset2 = currentOffset7 + 1;
                        if (currentOffset2 == endOffset) {
                        }
                        break;
                    case 16:
                        currentOffset2++;
                        if (currentOffset2 == endOffset) {
                            addToken(tokens, 28);
                            starIsMultiplyOperator = false;
                        } else {
                            int ch10 = data.charAt(currentOffset2);
                            if (ch10 == 61) {
                                addToken(tokens, 29);
                                starIsMultiplyOperator = false;
                                currentOffset2++;
                                if (currentOffset2 == endOffset) {
                                }
                            } else {
                                addToken(tokens, 28);
                                starIsMultiplyOperator = false;
                            }
                        }
                        break;
                    case 17:
                        addToken(tokens, 26);
                        starIsMultiplyOperator = false;
                        currentOffset2++;
                        if (currentOffset2 == endOffset) {
                        }
                        break;
                    case 18:
                        currentOffset2++;
                        if (currentOffset2 == endOffset) {
                            addToken(tokens, 30);
                            starIsMultiplyOperator = false;
                        } else {
                            int ch11 = data.charAt(currentOffset2);
                            if (ch11 == 61) {
                                addToken(tokens, 31);
                                starIsMultiplyOperator = false;
                                currentOffset2++;
                                if (currentOffset2 == endOffset) {
                                }
                            } else {
                                addToken(tokens, 30);
                                starIsMultiplyOperator = false;
                            }
                        }
                        break;
                    case Tokens.EXPRTOKEN_OPERATOR_DIV:
                        addToken(tokens, 6);
                        starIsMultiplyOperator = false;
                        currentOffset2++;
                        if (currentOffset2 == endOffset) {
                        }
                        break;
                    case Tokens.EXPRTOKEN_OPERATOR_MULT:
                    case Tokens.EXPRTOKEN_OPERATOR_UNION:
                    case 25:
                        int nameOffset = currentOffset2;
                        currentOffset2 = scanNCName(data, endOffset, currentOffset2);
                        int nameOffset2 = nameOffset;
                        if (currentOffset2 == nameOffset2) {
                            return false;
                        }
                        if (currentOffset2 < endOffset) {
                            ch2 = data.charAt(currentOffset2);
                        } else {
                            ch2 = -1;
                        }
                        String nameHandle2 = symbolTable.addSymbol(data.substring(nameOffset2, currentOffset2));
                        boolean isNameTestNCName = false;
                        boolean isAxisName = false;
                        String prefixHandle2 = XMLSymbols.EMPTY_STRING;
                        if (ch2 == 58) {
                            int currentOffset8 = currentOffset2 + 1;
                            if (currentOffset8 == endOffset) {
                                return false;
                            }
                            ch2 = data.charAt(currentOffset8);
                            if (ch2 != 42) {
                                if (ch2 == 58) {
                                    currentOffset2 = currentOffset8 + 1;
                                    if (currentOffset2 < endOffset) {
                                        ch2 = data.charAt(currentOffset2);
                                    }
                                    isAxisName = true;
                                } else {
                                    prefixHandle2 = nameHandle2;
                                    nameOffset2 = currentOffset8;
                                    currentOffset2 = scanNCName(data, endOffset, currentOffset8);
                                    if (currentOffset2 == nameOffset2) {
                                        return false;
                                    }
                                    if (currentOffset2 < endOffset) {
                                        ch2 = data.charAt(currentOffset2);
                                    } else {
                                        ch2 = -1;
                                    }
                                    nameHandle2 = symbolTable.addSymbol(data.substring(nameOffset2, currentOffset2));
                                }
                            } else {
                                currentOffset2 = currentOffset8 + 1;
                                if (currentOffset2 < endOffset) {
                                    ch2 = data.charAt(currentOffset2);
                                }
                                isNameTestNCName = true;
                            }
                        }
                        String prefixHandle3 = prefixHandle2;
                        while (true) {
                            if (ch2 == 32 || ch2 == 10 || ch2 == 9 || ch2 == 13) {
                                currentOffset2++;
                                if (currentOffset2 != endOffset) {
                                    ch2 = data.charAt(currentOffset2);
                                }
                            }
                        }
                        if (starIsMultiplyOperator) {
                            if (nameHandle2 == fAndSymbol) {
                                addToken(tokens, 16);
                                starIsMultiplyOperator = false;
                            } else if (nameHandle2 == fOrSymbol) {
                                addToken(tokens, 17);
                                starIsMultiplyOperator = false;
                            } else if (nameHandle2 == fModSymbol) {
                                addToken(tokens, 18);
                                starIsMultiplyOperator = false;
                            } else {
                                if (nameHandle2 != fDivSymbol) {
                                    return false;
                                }
                                addToken(tokens, 19);
                                starIsMultiplyOperator = false;
                            }
                            if (isNameTestNCName || isAxisName) {
                                return false;
                            }
                        } else if (ch2 == 40 && !isNameTestNCName && !isAxisName) {
                            if (nameHandle2 == fCommentSymbol) {
                                addToken(tokens, 12);
                            } else if (nameHandle2 == fTextSymbol) {
                                addToken(tokens, 13);
                            } else if (nameHandle2 == fPISymbol) {
                                addToken(tokens, 14);
                            } else if (nameHandle2 == fNodeSymbol) {
                                addToken(tokens, 15);
                            } else {
                                addToken(tokens, 32);
                                tokens.addToken(prefixHandle3);
                                tokens.addToken(nameHandle2);
                            }
                            addToken(tokens, 0);
                            starIsMultiplyOperator = false;
                            currentOffset2++;
                            if (currentOffset2 == endOffset) {
                            }
                        } else if (isAxisName || (ch2 == 58 && currentOffset2 + 1 < endOffset && data.charAt(currentOffset2 + 1) == ':')) {
                            if (nameHandle2 == fAncestorSymbol) {
                                addToken(tokens, 33);
                            } else if (nameHandle2 == fAncestorOrSelfSymbol) {
                                addToken(tokens, 34);
                            } else if (nameHandle2 == fAttributeSymbol) {
                                addToken(tokens, 35);
                            } else if (nameHandle2 == fChildSymbol) {
                                addToken(tokens, 36);
                            } else if (nameHandle2 == fDescendantSymbol) {
                                addToken(tokens, 37);
                            } else if (nameHandle2 == fDescendantOrSelfSymbol) {
                                addToken(tokens, 38);
                            } else if (nameHandle2 == fFollowingSymbol) {
                                addToken(tokens, 39);
                            } else if (nameHandle2 == fFollowingSiblingSymbol) {
                                addToken(tokens, 40);
                            } else if (nameHandle2 == fNamespaceSymbol) {
                                addToken(tokens, 41);
                            } else if (nameHandle2 == fParentSymbol) {
                                addToken(tokens, 42);
                            } else if (nameHandle2 == fPrecedingSymbol) {
                                addToken(tokens, 43);
                            } else if (nameHandle2 == fPrecedingSiblingSymbol) {
                                addToken(tokens, 44);
                            } else {
                                if (nameHandle2 != fSelfSymbol) {
                                    return false;
                                }
                                addToken(tokens, 45);
                            }
                            if (isNameTestNCName) {
                                return false;
                            }
                            addToken(tokens, 8);
                            starIsMultiplyOperator = false;
                            if (isAxisName || (currentOffset2 = currentOffset2 + 1 + 1) == endOffset) {
                            }
                        } else if (isNameTestNCName) {
                            addToken(tokens, 10);
                            starIsMultiplyOperator = true;
                            tokens.addToken(nameHandle2);
                        } else {
                            addToken(tokens, 11);
                            starIsMultiplyOperator = true;
                            tokens.addToken(prefixHandle3);
                            tokens.addToken(nameHandle2);
                        }
                        break;
                    case Tokens.EXPRTOKEN_OPERATOR_SLASH:
                        addToken(tokens, 2);
                        starIsMultiplyOperator = false;
                        currentOffset2++;
                        if (currentOffset2 == endOffset) {
                        }
                        break;
                    case Tokens.EXPRTOKEN_OPERATOR_DOUBLE_SLASH:
                        addToken(tokens, 3);
                        starIsMultiplyOperator = true;
                        currentOffset2++;
                        if (currentOffset2 == endOffset) {
                        }
                        break;
                    case Tokens.EXPRTOKEN_OPERATOR_PLUS:
                        addToken(tokens, 23);
                        starIsMultiplyOperator = false;
                        currentOffset2++;
                        if (currentOffset2 == endOffset) {
                        }
                        break;
                    default:
                        return false;
                }
            }
            return true;
        }

        int scanNCName(String data, int endOffset, int currentOffset) {
            int ch = data.charAt(currentOffset);
            if (ch >= 128) {
                if (!XMLChar.isNameStart(ch)) {
                    return currentOffset;
                }
            } else {
                byte chartype = fASCIICharMap[ch];
                if (chartype != 20 && chartype != 23) {
                    return currentOffset;
                }
            }
            while (true) {
                currentOffset++;
                if (currentOffset >= endOffset) {
                    break;
                }
                int ch2 = data.charAt(currentOffset);
                if (ch2 >= 128) {
                    if (!XMLChar.isName(ch2)) {
                        break;
                    }
                } else {
                    byte chartype2 = fASCIICharMap[ch2];
                    if (chartype2 != 20 && chartype2 != 14 && chartype2 != 12 && chartype2 != 11 && chartype2 != 23) {
                        break;
                    }
                }
            }
            return currentOffset;
        }

        private int scanNumber(Tokens tokens, String data, int endOffset, int currentOffset) {
            int ch = data.charAt(currentOffset);
            int whole = 0;
            int part = 0;
            while (ch >= 48 && ch <= 57) {
                whole = (whole * 10) + (ch - 48);
                currentOffset++;
                if (currentOffset == endOffset) {
                    break;
                }
                ch = data.charAt(currentOffset);
            }
            if (ch == 46 && (currentOffset = currentOffset + 1) < endOffset) {
                int ch2 = data.charAt(currentOffset);
                while (ch2 >= 48 && ch2 <= 57) {
                    part = (part * 10) + (ch2 - 48);
                    currentOffset++;
                    if (currentOffset == endOffset) {
                        break;
                    }
                    ch2 = data.charAt(currentOffset);
                }
                if (part != 0) {
                    throw new RuntimeException("find a solution!");
                }
            }
            tokens.addToken(whole);
            tokens.addToken(part);
            return currentOffset;
        }

        protected void addToken(Tokens tokens, int token) throws XPathException {
            tokens.addToken(token);
        }
    }

    public static void main(String[] argv) throws Exception {
        for (String expression : argv) {
            System.out.println("# XPath expression: \"" + expression + '\"');
            try {
                SymbolTable symbolTable = new SymbolTable();
                XPath xpath = new XPath(expression, symbolTable, null);
                System.out.println("expanded xpath: \"" + xpath.toString() + '\"');
            } catch (XPathException e) {
                System.out.println("error: " + e.getMessage());
            }
        }
    }
}
