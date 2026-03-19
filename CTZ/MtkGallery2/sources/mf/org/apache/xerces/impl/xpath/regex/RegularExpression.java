package mf.org.apache.xerces.impl.xpath.regex;

import java.io.Serializable;
import java.text.CharacterIterator;
import java.util.Locale;
import java.util.Stack;
import mf.org.apache.xerces.impl.xpath.XPath;
import mf.org.apache.xerces.impl.xpath.regex.Op;
import mf.org.apache.xerces.impl.xpath.regex.Token;
import mf.org.apache.xerces.util.IntStack;

public class RegularExpression implements Serializable {
    static final int CARRIAGE_RETURN = 13;
    static final boolean DEBUG = false;
    static final int EXTENDED_COMMENT = 16;
    static final int IGNORE_CASE = 2;
    static final int LINE_FEED = 10;
    static final int LINE_SEPARATOR = 8232;
    static final int MULTIPLE_LINES = 8;
    static final int PARAGRAPH_SEPARATOR = 8233;
    static final int PROHIBIT_FIXED_STRING_OPTIMIZATION = 256;
    static final int PROHIBIT_HEAD_CHARACTER_OPTIMIZATION = 128;
    static final int SINGLE_LINE = 4;
    static final int SPECIAL_COMMA = 1024;
    static final int UNICODE_WORD_BOUNDARY = 64;
    static final int USE_UNICODE_CATEGORY = 32;
    private static final int WT_IGNORE = 0;
    private static final int WT_LETTER = 1;
    private static final int WT_OTHER = 2;
    static final int XMLSCHEMA_MODE = 512;
    private static final long serialVersionUID = 6242499334195006401L;
    transient Context context;
    transient RangeToken firstChar;
    transient String fixedString;
    transient boolean fixedStringOnly;
    transient int fixedStringOptions;
    transient BMPattern fixedStringTable;
    boolean hasBackReferences;
    transient int minlength;
    int nofparen;
    transient int numberOfClosures;
    transient Op operations;
    int options;
    String regex;
    Token tokentree;

    private synchronized void compile(Token tok) {
        if (this.operations != null) {
            return;
        }
        this.numberOfClosures = 0;
        this.operations = compile(tok, null, false);
    }

    private Op compile(Token token, Op op, boolean z) {
        ?? CreateChar;
        Op.ChildOp childOpCreateClosure;
        ?? r6;
        int i = token.type;
        switch (i) {
            case 0:
                CreateChar = Op.createChar(token.getChar());
                CreateChar.next = op;
                break;
            case 1:
                if (!z) {
                    int size = token.size() - 1;
                    CreateChar = op;
                    while (size >= 0) {
                        Op opCompile = compile(token.getChild(size), CreateChar, false);
                        size--;
                        CreateChar = opCompile;
                    }
                } else {
                    int i2 = 0;
                    CreateChar = op;
                    while (i2 < token.size()) {
                        Op opCompile2 = compile(token.getChild(i2), CreateChar, true);
                        i2++;
                        CreateChar = opCompile2;
                    }
                }
                break;
            case 2:
                Op.UnionOp unionOpCreateUnion = Op.createUnion(token.size());
                for (int i3 = 0; i3 < token.size(); i3++) {
                    unionOpCreateUnion.addElement(compile(token.getChild(i3), op, z));
                }
                CreateChar = unionOpCreateUnion;
                break;
            case 3:
            case 9:
                Token child = token.getChild(0);
                int min = token.getMin();
                int max = token.getMax();
                if (min >= 0 && min == max) {
                    int i4 = 0;
                    CreateChar = op;
                    while (i4 < min) {
                        i4++;
                        CreateChar = compile(child, CreateChar, z);
                    }
                } else {
                    if (min > 0 && max > 0) {
                        max -= min;
                    }
                    if (max <= 0) {
                        if (token.type == 9) {
                            childOpCreateClosure = Op.createNonGreedyClosure();
                        } else {
                            int i5 = this.numberOfClosures;
                            this.numberOfClosures = i5 + 1;
                            childOpCreateClosure = Op.createClosure(i5);
                        }
                        r6 = childOpCreateClosure;
                        r6.next = op;
                        r6.setChild(compile(child, r6, z));
                    } else {
                        r6 = op;
                        for (int i6 = 0; i6 < max; i6++) {
                            Op.ChildOp childOpCreateQuestion = Op.createQuestion(token.type == 9);
                            childOpCreateQuestion.next = op;
                            childOpCreateQuestion.setChild(compile(child, r6, z));
                            r6 = childOpCreateQuestion;
                        }
                    }
                    CreateChar = r6;
                    if (min > 0) {
                        int i7 = 0;
                        CreateChar = CreateChar;
                        while (i7 < min) {
                            i7++;
                            CreateChar = compile(child, CreateChar, z);
                        }
                    }
                }
                break;
            case 4:
            case 5:
                CreateChar = Op.createRange(token);
                CreateChar.next = op;
                break;
            case 6:
                if (token.getParenNumber() == 0) {
                    CreateChar = compile(token.getChild(0), op, z);
                } else if (z) {
                    CreateChar = Op.createCapture(-token.getParenNumber(), compile(token.getChild(0), Op.createCapture(token.getParenNumber(), op), z));
                } else {
                    CreateChar = Op.createCapture(token.getParenNumber(), compile(token.getChild(0), Op.createCapture(-token.getParenNumber(), op), z));
                }
                break;
            case 7:
                CreateChar = op;
                break;
            case 8:
                CreateChar = Op.createAnchor(token.getChar());
                CreateChar.next = op;
                break;
            case 10:
                CreateChar = Op.createString(token.getString());
                CreateChar.next = op;
                break;
            case 11:
                CreateChar = Op.createDot();
                CreateChar.next = op;
                break;
            case 12:
                CreateChar = Op.createBackReference(token.getReferenceNumber());
                CreateChar.next = op;
                break;
            default:
                switch (i) {
                    case XPath.Tokens.EXPRTOKEN_OPERATOR_MULT:
                        CreateChar = Op.createLook(20, op, compile(token.getChild(0), null, false));
                        break;
                    case XPath.Tokens.EXPRTOKEN_OPERATOR_SLASH:
                        CreateChar = Op.createLook(21, op, compile(token.getChild(0), null, false));
                        break;
                    case XPath.Tokens.EXPRTOKEN_OPERATOR_DOUBLE_SLASH:
                        CreateChar = Op.createLook(22, op, compile(token.getChild(0), null, true));
                        break;
                    case XPath.Tokens.EXPRTOKEN_OPERATOR_UNION:
                        CreateChar = Op.createLook(23, op, compile(token.getChild(0), null, true));
                        break;
                    case XPath.Tokens.EXPRTOKEN_OPERATOR_PLUS:
                        CreateChar = Op.createIndependent(op, compile(token.getChild(0), null, z));
                        break;
                    case 25:
                        CreateChar = Op.createModifier(op, compile(token.getChild(0), null, z), ((Token.ModifierToken) token).getOptions(), ((Token.ModifierToken) token).getOptionsMask());
                        break;
                    case XPath.Tokens.EXPRTOKEN_OPERATOR_EQUAL:
                        Token.ConditionToken conditionToken = (Token.ConditionToken) token;
                        CreateChar = Op.createCondition(op, conditionToken.refNumber, conditionToken.condition == null ? null : compile(conditionToken.condition, null, z), compile(conditionToken.yes, op, z), conditionToken.no != null ? compile(conditionToken.no, op, z) : null);
                        break;
                    default:
                        throw new RuntimeException("Unknown token type: " + token.type);
                }
                break;
        }
        return CreateChar;
    }

    public boolean matches(char[] target) {
        return matches(target, 0, target.length, (Match) null);
    }

    public boolean matches(char[] target, int start, int end) {
        return matches(target, start, end, (Match) null);
    }

    public boolean matches(char[] target, Match match) {
        return matches(target, 0, target.length, match);
    }

    public boolean matches(char[] cArr, int i, int i2, Match match) throws Throwable {
        Match match2;
        int i3;
        int i4;
        int iMatch;
        RangeToken rangeToken;
        boolean z;
        synchronized (this) {
            try {
                if (this.operations == null) {
                    prepare();
                }
                if (this.context == null) {
                    this.context = new Context();
                }
            } catch (Throwable th) {
                th = th;
                while (true) {
                    try {
                        throw th;
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
            }
        }
        synchronized (this.context) {
            try {
                Context context = this.context.inuse ? new Context() : this.context;
                try {
                    try {
                        context.reset(cArr, i, i2, this.numberOfClosures);
                        if (match == null) {
                            if (this.hasBackReferences) {
                                match2 = new Match();
                                match2.setNumberOfGroups(this.nofparen);
                            }
                            context.match = match2;
                            if (!isSet(this.options, 512)) {
                                int iMatch2 = match(context, this.operations, context.start, 1, this.options);
                                if (iMatch2 != context.limit) {
                                    return false;
                                }
                                if (context.match != null) {
                                    context.match.setBeginning(0, context.start);
                                    context.match.setEnd(0, iMatch2);
                                }
                                context.setInUse(false);
                                return true;
                            }
                            if (this.fixedStringOnly) {
                                int iMatches = this.fixedStringTable.matches(cArr, context.start, context.limit);
                                if (iMatches < 0) {
                                    context.setInUse(false);
                                    return false;
                                }
                                if (context.match != null) {
                                    context.match.setBeginning(0, iMatches);
                                    context.match.setEnd(0, this.fixedString.length() + iMatches);
                                }
                                context.setInUse(false);
                                return true;
                            }
                            if (this.fixedString != null && this.fixedStringTable.matches(cArr, context.start, context.limit) < 0) {
                                context.setInUse(false);
                                return false;
                            }
                            int i5 = context.limit - this.minlength;
                            if (this.operations == null || this.operations.type != 7 || this.operations.getChild().type != 0) {
                                if (this.firstChar != null) {
                                    RangeToken rangeToken2 = this.firstChar;
                                    i3 = -1;
                                    i4 = context.start;
                                    while (i4 <= i5) {
                                        char c = cArr[i4];
                                        boolean zIsHighSurrogate = REUtil.isHighSurrogate(c);
                                        int iComposeFromSurrogates = c;
                                        if (zIsHighSurrogate) {
                                            iComposeFromSurrogates = c;
                                            if (i4 + 1 < context.limit) {
                                                iComposeFromSurrogates = REUtil.composeFromSurrogates(c, cArr[i4 + 1]);
                                            }
                                        }
                                        if (rangeToken2.match(iComposeFromSurrogates == true ? 1 : 0)) {
                                            rangeToken = rangeToken2;
                                            int iMatch3 = match(context, this.operations, i4, 1, this.options);
                                            iMatch = iMatch3;
                                            if (iMatch3 >= 0) {
                                                break;
                                            }
                                            i3 = iMatch;
                                        } else {
                                            rangeToken = rangeToken2;
                                        }
                                        i4++;
                                        rangeToken2 = rangeToken;
                                    }
                                } else {
                                    i3 = -1;
                                    i4 = context.start;
                                    while (i4 <= i5) {
                                        int iMatch4 = match(context, this.operations, i4, 1, this.options);
                                        i3 = iMatch4;
                                        if (iMatch4 >= 0) {
                                            break;
                                        }
                                        i4++;
                                    }
                                }
                                iMatch = i3;
                            } else if (isSet(this.options, 4)) {
                                int i6 = context.start;
                                iMatch = match(context, this.operations, context.start, 1, this.options);
                                i4 = i6;
                            } else {
                                boolean z2 = true;
                                int i7 = -1;
                                i4 = context.start;
                                while (true) {
                                    if (i4 > i5) {
                                        iMatch = i7;
                                        break;
                                    }
                                    if (isEOLChar(cArr[i4])) {
                                        z = true;
                                    } else {
                                        if (z2) {
                                            int iMatch5 = match(context, this.operations, i4, 1, this.options);
                                            iMatch = iMatch5;
                                            if (iMatch5 >= 0) {
                                                break;
                                            }
                                            i7 = iMatch;
                                        }
                                        z = false;
                                    }
                                    z2 = z;
                                    i4++;
                                }
                            }
                            if (iMatch < 0) {
                                context.setInUse(false);
                                return false;
                            }
                            if (context.match != null) {
                                context.match.setBeginning(0, i4);
                                context.match.setEnd(0, iMatch);
                            }
                            context.setInUse(false);
                            return true;
                        }
                        match.setNumberOfGroups(this.nofparen);
                        match.setSource(cArr);
                        match2 = match;
                        context.match = match2;
                        if (!isSet(this.options, 512)) {
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        while (true) {
                            try {
                                throw th;
                            } catch (Throwable th4) {
                                th = th4;
                            }
                        }
                    }
                } catch (Throwable th5) {
                    th = th5;
                }
            } catch (Throwable th6) {
                th = th6;
            }
        }
    }

    public boolean matches(String target) {
        return matches(target, 0, target.length(), (Match) null);
    }

    public boolean matches(String target, int start, int end) {
        return matches(target, start, end, (Match) null);
    }

    public boolean matches(String target, Match match) {
        return matches(target, 0, target.length(), match);
    }

    public boolean matches(String target, int start, int end, Match match) throws Throwable {
        Match match2;
        int matchEnd;
        int matchStart;
        int matchEnd2;
        RangeToken range;
        boolean previousIsEOL;
        synchronized (this) {
            try {
                if (this.operations == null) {
                    prepare();
                }
                if (this.context == null) {
                    this.context = new Context();
                }
            } catch (Throwable th) {
                th = th;
                while (true) {
                    try {
                        throw th;
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
            }
        }
        synchronized (this.context) {
            try {
                Context con = this.context.inuse ? new Context() : this.context;
                try {
                    try {
                        con.reset(target, start, end, this.numberOfClosures);
                        if (match == null) {
                            if (this.hasBackReferences) {
                                match2 = new Match();
                                match2.setNumberOfGroups(this.nofparen);
                            }
                            con.match = match2;
                            if (!isSet(this.options, 512)) {
                                int matchEnd3 = match(con, this.operations, con.start, 1, this.options);
                                if (matchEnd3 != con.limit) {
                                    return false;
                                }
                                if (con.match != null) {
                                    con.match.setBeginning(0, con.start);
                                    con.match.setEnd(0, matchEnd3);
                                }
                                con.setInUse(false);
                                return true;
                            }
                            if (this.fixedStringOnly) {
                                int o = this.fixedStringTable.matches(target, con.start, con.limit);
                                if (o < 0) {
                                    con.setInUse(false);
                                    return false;
                                }
                                if (con.match != null) {
                                    con.match.setBeginning(0, o);
                                    con.match.setEnd(0, this.fixedString.length() + o);
                                }
                                con.setInUse(false);
                                return true;
                            }
                            if (this.fixedString != null) {
                                int o2 = this.fixedStringTable.matches(target, con.start, con.limit);
                                if (o2 < 0) {
                                    con.setInUse(false);
                                    return false;
                                }
                            }
                            int o3 = con.limit;
                            int limit = o3 - this.minlength;
                            if (this.operations == null || this.operations.type != 7 || this.operations.getChild().type != 0) {
                                if (this.firstChar != null) {
                                    RangeToken range2 = this.firstChar;
                                    int matchStart2 = con.start;
                                    matchEnd = -1;
                                    matchStart = matchStart2;
                                    while (matchStart <= limit) {
                                        int ch = target.charAt(matchStart);
                                        if (REUtil.isHighSurrogate(ch) && matchStart + 1 < con.limit) {
                                            ch = REUtil.composeFromSurrogates(ch, target.charAt(matchStart + 1));
                                        }
                                        if (range2.match(ch)) {
                                            range = range2;
                                            int iMatch = match(con, this.operations, matchStart, 1, this.options);
                                            matchEnd2 = iMatch;
                                            if (iMatch >= 0) {
                                                break;
                                            }
                                            matchEnd = matchEnd2;
                                        } else {
                                            range = range2;
                                        }
                                        matchStart++;
                                        range2 = range;
                                    }
                                } else {
                                    int matchStart3 = con.start;
                                    matchEnd = -1;
                                    matchStart = matchStart3;
                                    while (matchStart <= limit) {
                                        int iMatch2 = match(con, this.operations, matchStart, 1, this.options);
                                        matchEnd = iMatch2;
                                        if (iMatch2 >= 0) {
                                            break;
                                        }
                                        matchStart++;
                                    }
                                }
                                matchEnd2 = matchEnd;
                            } else if (isSet(this.options, 4)) {
                                int matchStart4 = con.start;
                                Op op = this.operations;
                                int i = con.start;
                                int matchStart5 = this.options;
                                int matchEnd4 = match(con, op, i, 1, matchStart5);
                                matchEnd2 = matchEnd4;
                                matchStart = matchStart4;
                            } else {
                                int matchStart6 = con.start;
                                boolean previousIsEOL2 = true;
                                int matchEnd5 = -1;
                                matchStart = matchStart6;
                                while (true) {
                                    if (matchStart > limit) {
                                        matchEnd2 = matchEnd5;
                                        break;
                                    }
                                    if (isEOLChar(target.charAt(matchStart))) {
                                        previousIsEOL = true;
                                    } else {
                                        if (previousIsEOL2) {
                                            int iMatch3 = match(con, this.operations, matchStart, 1, this.options);
                                            matchEnd2 = iMatch3;
                                            if (iMatch3 >= 0) {
                                                break;
                                            }
                                            matchEnd5 = matchEnd2;
                                        }
                                        previousIsEOL = false;
                                    }
                                    previousIsEOL2 = previousIsEOL;
                                    matchStart++;
                                }
                            }
                            if (matchEnd2 < 0) {
                                con.setInUse(false);
                                return false;
                            }
                            if (con.match != null) {
                                con.match.setBeginning(0, matchStart);
                                con.match.setEnd(0, matchEnd2);
                            }
                            con.setInUse(false);
                            return true;
                        }
                        match.setNumberOfGroups(this.nofparen);
                        match.setSource(target);
                        match2 = match;
                        con.match = match2;
                        if (!isSet(this.options, 512)) {
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        while (true) {
                            try {
                                throw th;
                            } catch (Throwable th4) {
                                th = th4;
                            }
                        }
                    }
                } catch (Throwable th5) {
                    th = th5;
                }
            } catch (Throwable th6) {
                th = th6;
            }
        }
    }

    private int match(Context con, Op op, int offset, int dx, int opts) {
        int retValue;
        Op op2;
        int dx2;
        int retValue2;
        Op op3;
        Op op4;
        Op op5;
        Op op6;
        ExpressionTarget target = con.target;
        Stack opStack = new Stack();
        IntStack dataStack = new IntStack();
        boolean isSetIgnoreCase = isSet(opts, 2);
        Op op7 = op;
        int offset2 = offset;
        int dx3 = dx;
        int opts2 = opts;
        int id = 0;
        while (true) {
            int i = id;
            if (op7 != null && offset2 <= con.limit && offset2 >= con.start) {
                int retValue3 = op7.type;
                switch (retValue3) {
                    case 0:
                        dx2 = dx3;
                        int o1 = dx2 > 0 ? offset2 : offset2 - 1;
                        if (o1 < con.limit && o1 >= 0) {
                            if (!isSet(opts2, 4)) {
                                int ch = target.charAt(o1);
                                if (REUtil.isHighSurrogate(ch) && o1 + dx2 >= 0 && o1 + dx2 < con.limit) {
                                    o1 += dx2;
                                    ch = REUtil.composeFromSurrogates(ch, target.charAt(o1));
                                }
                                if (isEOLChar(ch)) {
                                    id = 1;
                                    retValue = -1;
                                    dx3 = dx2;
                                    while (id != 0) {
                                    }
                                }
                            } else if (REUtil.isHighSurrogate(target.charAt(o1)) && o1 + dx2 >= 0 && o1 + dx2 < con.limit) {
                                o1 += dx2;
                            }
                            int offset3 = dx2 > 0 ? o1 + 1 : o1;
                            op7 = op7.next;
                            offset2 = offset3;
                            id = i;
                            retValue = -1;
                            dx3 = dx2;
                            while (id != 0) {
                            }
                            break;
                        } else {
                            retValue2 = 1;
                            id = retValue2;
                            retValue = -1;
                            dx3 = dx2;
                            while (id != 0) {
                            }
                        }
                        break;
                    case 1:
                        dx2 = dx3;
                        int o12 = dx2 > 0 ? offset2 : offset2 - 1;
                        if (o12 < con.limit && o12 >= 0 && matchChar(op7.getData(), target.charAt(o12), isSetIgnoreCase)) {
                            offset2 += dx2;
                            op3 = op7.next;
                            op7 = op3;
                            id = i;
                            retValue = -1;
                            dx3 = dx2;
                            while (id != 0) {
                            }
                        } else {
                            retValue2 = 1;
                            id = retValue2;
                            retValue = -1;
                            dx3 = dx2;
                            while (id != 0) {
                            }
                        }
                        break;
                    case 2:
                    case 12:
                    case 13:
                    case 14:
                    case 17:
                    case 18:
                    case XPath.Tokens.EXPRTOKEN_OPERATOR_DIV:
                    default:
                        throw new RuntimeException("Unknown operation type: " + op7.type);
                    case 3:
                    case 4:
                        dx2 = dx3;
                        int o13 = dx2 > 0 ? offset2 : offset2 - 1;
                        if (o13 < con.limit && o13 >= 0) {
                            int ch2 = target.charAt(offset2);
                            if (REUtil.isHighSurrogate(ch2) && o13 + dx2 < con.limit && o13 + dx2 >= 0) {
                                o13 += dx2;
                                ch2 = REUtil.composeFromSurrogates(ch2, target.charAt(o13));
                            }
                            RangeToken tok = op7.getToken();
                            if (!tok.match(ch2)) {
                                id = 1;
                                retValue = -1;
                                dx3 = dx2;
                                while (id != 0) {
                                }
                            } else {
                                int offset4 = dx2 > 0 ? o13 + 1 : o13;
                                op7 = op7.next;
                                offset2 = offset4;
                                id = i;
                                retValue = -1;
                                dx3 = dx2;
                                while (id != 0) {
                                }
                            }
                        } else {
                            retValue2 = 1;
                            id = retValue2;
                            retValue = -1;
                            dx3 = dx2;
                            while (id != 0) {
                            }
                        }
                        break;
                    case 5:
                        dx2 = dx3;
                        if (!matchAnchor(target, op7, con, offset2, opts2)) {
                            id = 1;
                            retValue = -1;
                            dx3 = dx2;
                            while (id != 0) {
                            }
                        } else {
                            op3 = op7.next;
                            op7 = op3;
                            id = i;
                            retValue = -1;
                            dx3 = dx2;
                            while (id != 0) {
                            }
                        }
                        break;
                    case 6:
                        dx2 = dx3;
                        String literal = op7.getString();
                        int literallen = literal.length();
                        if (dx2 <= 0) {
                            if (!target.regionMatches(isSetIgnoreCase, offset2 - literallen, con.limit, literal, literallen)) {
                                id = 1;
                                retValue = -1;
                                dx3 = dx2;
                                while (id != 0) {
                                }
                            } else {
                                offset2 -= literallen;
                                op3 = op7.next;
                                op7 = op3;
                                id = i;
                                retValue = -1;
                                dx3 = dx2;
                                while (id != 0) {
                                }
                            }
                        } else if (!target.regionMatches(isSetIgnoreCase, offset2, con.limit, literal, literallen)) {
                            id = 1;
                            retValue = -1;
                            dx3 = dx2;
                            while (id != 0) {
                            }
                        } else {
                            offset2 += literallen;
                            op3 = op7.next;
                            op7 = op3;
                            id = i;
                            retValue = -1;
                            dx3 = dx2;
                            while (id != 0) {
                            }
                        }
                        break;
                    case 7:
                        dx2 = dx3;
                        int id2 = op7.getData();
                        if (!con.closureContexts[id2].contains(offset2)) {
                            con.closureContexts[id2].addOffset(offset2);
                            opStack.push(op7);
                            dataStack.push(offset2);
                            op3 = op7.getChild();
                            op7 = op3;
                            id = i;
                            retValue = -1;
                            dx3 = dx2;
                            while (id != 0) {
                            }
                        } else {
                            retValue2 = 1;
                            id = retValue2;
                            retValue = -1;
                            dx3 = dx2;
                            while (id != 0) {
                            }
                        }
                        break;
                    case 8:
                    case 10:
                        opStack.push(op7);
                        dataStack.push(offset2);
                        op7 = op7.next;
                        id = i;
                        retValue = -1;
                        while (id != 0) {
                        }
                        break;
                    case 9:
                        dx2 = dx3;
                        opStack.push(op7);
                        dataStack.push(offset2);
                        op3 = op7.getChild();
                        op7 = op3;
                        id = i;
                        retValue = -1;
                        dx3 = dx2;
                        while (id != 0) {
                        }
                        break;
                    case 11:
                        dx2 = dx3;
                        if (op7.size() != 0) {
                            opStack.push(op7);
                            dataStack.push(0);
                            dataStack.push(offset2);
                            op3 = op7.elementAt(0);
                            op7 = op3;
                            id = i;
                            retValue = -1;
                            dx3 = dx2;
                            while (id != 0) {
                            }
                        } else {
                            id = 1;
                            retValue = -1;
                            dx3 = dx2;
                            while (id != 0) {
                            }
                        }
                        break;
                    case 15:
                        dx2 = dx3;
                        int refno = op7.getData();
                        if (con.match != null) {
                            if (refno > 0) {
                                dataStack.push(con.match.getBeginning(refno));
                                con.match.setBeginning(refno, offset2);
                            } else {
                                int index = -refno;
                                dataStack.push(con.match.getEnd(index));
                                con.match.setEnd(index, offset2);
                            }
                            opStack.push(op7);
                            dataStack.push(offset2);
                        }
                        op4 = op7.next;
                        op7 = op4;
                        id = i;
                        retValue = -1;
                        dx3 = dx2;
                        while (id != 0) {
                        }
                        break;
                    case 16:
                        int refno2 = op7.getData();
                        if (refno2 > 0 && refno2 < this.nofparen) {
                            if (con.match.getBeginning(refno2) >= 0 && con.match.getEnd(refno2) >= 0) {
                                int o2 = con.match.getBeginning(refno2);
                                int literallen2 = con.match.getEnd(refno2) - o2;
                                if (dx3 > 0) {
                                    dx2 = dx3;
                                    if (target.regionMatches(isSetIgnoreCase, offset2, con.limit, o2, literallen2)) {
                                        offset2 += literallen2;
                                        op4 = op7.next;
                                        op7 = op4;
                                        id = i;
                                        retValue = -1;
                                        dx3 = dx2;
                                    } else {
                                        id = 1;
                                        retValue = -1;
                                        dx3 = dx2;
                                    }
                                } else {
                                    dx2 = dx3;
                                    if (target.regionMatches(isSetIgnoreCase, offset2 - literallen2, con.limit, o2, literallen2)) {
                                        offset2 -= literallen2;
                                        op4 = op7.next;
                                        op7 = op4;
                                        id = i;
                                        retValue = -1;
                                        dx3 = dx2;
                                    } else {
                                        id = 1;
                                        retValue = -1;
                                        dx3 = dx2;
                                    }
                                }
                                while (id != 0) {
                                }
                            } else {
                                dx2 = dx3;
                                id = 1;
                                retValue = -1;
                                dx3 = dx2;
                                while (id != 0) {
                                }
                            }
                        }
                        break;
                    case XPath.Tokens.EXPRTOKEN_OPERATOR_MULT:
                    case XPath.Tokens.EXPRTOKEN_OPERATOR_SLASH:
                    case XPath.Tokens.EXPRTOKEN_OPERATOR_DOUBLE_SLASH:
                    case XPath.Tokens.EXPRTOKEN_OPERATOR_UNION:
                        opStack.push(op7);
                        dataStack.push(dx3);
                        dataStack.push(offset2);
                        dx = (op7.type == 20 || op7.type == 21) ? 1 : -1;
                        op5 = op7.getChild();
                        dx3 = dx;
                        op7 = op5;
                        id = i;
                        retValue = -1;
                        while (id != 0) {
                        }
                        break;
                    case XPath.Tokens.EXPRTOKEN_OPERATOR_PLUS:
                        opStack.push(op7);
                        dataStack.push(offset2);
                        op6 = op7.getChild();
                        op7 = op6;
                        id = i;
                        retValue = -1;
                        while (id != 0) {
                        }
                        break;
                    case 25:
                        int localopts = opts2;
                        int localopts2 = (localopts | op7.getData()) & (~op7.getData2());
                        opStack.push(op7);
                        dataStack.push(opts2);
                        dataStack.push(offset2);
                        op7 = op7.getChild();
                        opts2 = localopts2;
                        id = i;
                        retValue = -1;
                        while (id != 0) {
                        }
                        break;
                    case XPath.Tokens.EXPRTOKEN_OPERATOR_EQUAL:
                        Op.ConditionOp cop = (Op.ConditionOp) op7;
                        if (cop.refNumber <= 0) {
                            opStack.push(op7);
                            dataStack.push(offset2);
                            op6 = cop.condition;
                            op7 = op6;
                            id = i;
                            retValue = -1;
                            while (id != 0) {
                            }
                        } else {
                            if (cop.refNumber >= this.nofparen) {
                                throw new RuntimeException("Internal Error: Reference number must be more than zero: " + cop.refNumber);
                            }
                            if (con.match.getBeginning(cop.refNumber) < 0 || con.match.getEnd(cop.refNumber) < 0) {
                                Op op8 = cop.no;
                                op5 = op8 != null ? cop.no : cop.next;
                            } else {
                                op5 = cop.yes;
                            }
                            op7 = op5;
                            id = i;
                            retValue = -1;
                            while (id != 0) {
                            }
                        }
                        break;
                }
            } else {
                int dx4 = dx3;
                if (op7 != null) {
                    dx = -1;
                } else if (!isSet(opts2, 512) || offset2 == con.limit) {
                    dx = offset2;
                }
                dx3 = dx4;
                retValue = dx;
                id = 1;
                while (id != 0) {
                    if (opStack.isEmpty()) {
                        return retValue;
                    }
                    op7 = (Op) opStack.pop();
                    offset2 = dataStack.pop();
                    int i2 = op7.type;
                    if (i2 != 15) {
                        switch (i2) {
                            case 7:
                            case 9:
                                if (retValue < 0) {
                                    op7 = op7.next;
                                    id = 0;
                                }
                                break;
                            case 8:
                            case 10:
                                if (retValue < 0) {
                                    op7 = op7.getChild();
                                    id = 0;
                                }
                                break;
                            case 11:
                                int unionIndex = dataStack.pop();
                                if (retValue < 0) {
                                    int unionIndex2 = unionIndex + 1;
                                    if (unionIndex2 < op7.size()) {
                                        opStack.push(op7);
                                        dataStack.push(unionIndex2);
                                        dataStack.push(offset2);
                                        op7 = op7.elementAt(unionIndex2);
                                        id = 0;
                                    } else {
                                        retValue = -1;
                                    }
                                }
                                break;
                            default:
                                switch (i2) {
                                    case XPath.Tokens.EXPRTOKEN_OPERATOR_MULT:
                                    case XPath.Tokens.EXPRTOKEN_OPERATOR_DOUBLE_SLASH:
                                        dx3 = dataStack.pop();
                                        if (retValue >= 0) {
                                            id = 0;
                                            op7 = op7.next;
                                        }
                                        retValue = -1;
                                        break;
                                    case XPath.Tokens.EXPRTOKEN_OPERATOR_SLASH:
                                    case XPath.Tokens.EXPRTOKEN_OPERATOR_UNION:
                                        dx3 = dataStack.pop();
                                        if (retValue < 0) {
                                            id = 0;
                                            op7 = op7.next;
                                        }
                                        retValue = -1;
                                        break;
                                    case XPath.Tokens.EXPRTOKEN_OPERATOR_PLUS:
                                        if (retValue < 0) {
                                            offset2 = retValue;
                                            op7 = op7.next;
                                            id = 0;
                                        }
                                        break;
                                    case 25:
                                        opts2 = dataStack.pop();
                                        if (retValue < 0) {
                                        }
                                        break;
                                    case XPath.Tokens.EXPRTOKEN_OPERATOR_EQUAL:
                                        Op.ConditionOp cop2 = (Op.ConditionOp) op7;
                                        if (retValue >= 0) {
                                            op2 = cop2.yes;
                                        } else if (cop2.no == null) {
                                            Op op9 = cop2.next;
                                            op7 = op9;
                                            id = 0;
                                        } else {
                                            op2 = cop2.no;
                                        }
                                        op7 = op2;
                                        id = 0;
                                        break;
                                }
                                while (id != 0) {
                                }
                                break;
                        }
                    } else {
                        int refno3 = op7.getData();
                        int saved = dataStack.pop();
                        if (retValue < 0) {
                            if (refno3 > 0) {
                                con.match.setBeginning(refno3, saved);
                            } else {
                                con.match.setEnd(-refno3, saved);
                            }
                        }
                    }
                    while (id != 0) {
                    }
                }
            }
        }
    }

    private boolean matchChar(int ch, int other, boolean ignoreCase) {
        return ignoreCase ? matchIgnoreCase(ch, other) : ch == other;
    }

    boolean matchAnchor(ExpressionTarget target, Op op, Context con, int offset, int opts) {
        int after;
        boolean go;
        int data = op.getData();
        if (data != 36) {
            if (data != 60) {
                if (data != 62) {
                    if (data != 90) {
                        if (data != 94) {
                            if (data != 98) {
                                if (data != 122) {
                                    switch (data) {
                                        case 64:
                                            if (offset != con.start && (offset <= con.start || !isEOLChar(target.charAt(offset - 1)))) {
                                                return false;
                                            }
                                            break;
                                        case 65:
                                            if (offset != con.start) {
                                                return false;
                                            }
                                            break;
                                        case 66:
                                            if (con.length == 0) {
                                                go = true;
                                            } else {
                                                int after2 = getWordType(target, con.start, con.limit, offset, opts);
                                                go = after2 == 0 || after2 == getPreviousWordType(target, con.start, con.limit, offset, opts);
                                            }
                                            if (!go) {
                                                return false;
                                            }
                                            break;
                                    }
                                } else if (offset != con.limit) {
                                    return false;
                                }
                            } else {
                                if (con.length == 0 || (after = getWordType(target, con.start, con.limit, offset, opts)) == 0) {
                                    return false;
                                }
                                int before = getPreviousWordType(target, con.start, con.limit, offset, opts);
                                if (after == before) {
                                    return false;
                                }
                            }
                        } else if (isSet(opts, 8)) {
                            if (offset != con.start && (offset <= con.start || offset >= con.limit || !isEOLChar(target.charAt(offset - 1)))) {
                                return false;
                            }
                        } else if (offset != con.start) {
                            return false;
                        }
                    } else if (offset != con.limit && ((offset + 1 != con.limit || !isEOLChar(target.charAt(offset))) && (offset + 2 != con.limit || target.charAt(offset) != '\r' || target.charAt(offset + 1) != '\n'))) {
                        return false;
                    }
                } else if (con.length == 0 || offset == con.start || getWordType(target, con.start, con.limit, offset, opts) != 2 || getPreviousWordType(target, con.start, con.limit, offset, opts) != 1) {
                    return false;
                }
            } else if (con.length == 0 || offset == con.limit || getWordType(target, con.start, con.limit, offset, opts) != 1 || getPreviousWordType(target, con.start, con.limit, offset, opts) != 2) {
                return false;
            }
        } else if (isSet(opts, 8)) {
            if (offset != con.limit && (offset >= con.limit || !isEOLChar(target.charAt(offset)))) {
                return false;
            }
        } else if (offset != con.limit && ((offset + 1 != con.limit || !isEOLChar(target.charAt(offset))) && (offset + 2 != con.limit || target.charAt(offset) != '\r' || target.charAt(offset + 1) != '\n'))) {
            return false;
        }
        return true;
    }

    private static final int getPreviousWordType(ExpressionTarget target, int begin, int end, int offset, int opts) {
        int offset2 = offset - 1;
        int ret = getWordType(target, begin, end, offset2, opts);
        while (ret == 0) {
            offset2--;
            ret = getWordType(target, begin, end, offset2, opts);
        }
        return ret;
    }

    private static final int getWordType(ExpressionTarget target, int begin, int end, int offset, int opts) {
        if (offset < begin || offset >= end) {
            return 2;
        }
        return getWordType0(target.charAt(offset), opts);
    }

    public boolean matches(CharacterIterator target) {
        return matches(target, (Match) null);
    }

    public boolean matches(java.text.CharacterIterator r21, mf.org.apache.xerces.impl.xpath.regex.Match r22) {
        r9 = r21.getBeginIndex();
        r10 = r21.getEndIndex();
        synchronized (r20) {
            ;
            if (r20.operations == null) {
                prepare();
            }
            if (r20.context == null) {
                r20.context = new mf.org.apache.xerces.impl.xpath.regex.RegularExpression.Context();
            }
        }
        r3 = r20.context;
        synchronized (r3) {
            ;
            if (r20.context.inuse) {
                r0 = new mf.org.apache.xerces.impl.xpath.regex.RegularExpression.Context();
            } else {
                r0 = r20.context;
            }
            r11 = r0;
            r11.reset(r21, r9, r10, r20.numberOfClosures);
            if (r22 != null) {
                r22.setNumberOfGroups(r20.nofparen);
                r22.setSource(r21);
            } else {
                if (r20.hasBackReferences) {
                    r0 = new mf.org.apache.xerces.impl.xpath.regex.Match();
                    r0.setNumberOfGroups(r20.nofparen);
                }
                r11.match = r0;
                if (!isSet(r20.options, 512)) {
                    r1 = match(r11, r20.operations, r11.start, 1, r20.options);
                    if (r1 == r11.limit) {
                        if (r11.match != null) {
                            r11.match.setBeginning(0, r11.start);
                            r11.match.setEnd(0, r1);
                        }
                        r11.setInUse(false);
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    if (r20.fixedStringOnly) {
                        r1 = r20.fixedStringTable.matches(r21, r11.start, r11.limit);
                        if (r1 >= 0) {
                            if (r11.match != null) {
                                r11.match.setBeginning(0, r1);
                                r11.match.setEnd(0, r20.fixedString.length() + r1);
                            }
                            r11.setInUse(false);
                            return true;
                        } else {
                            r11.setInUse(false);
                            return false;
                        }
                    } else {
                        if (r20.fixedString != null) {
                            r1 = r20.fixedStringTable.matches(r21, r11.start, r11.limit);
                            if (r1 < 0) {
                                r11.setInUse(false);
                                return false;
                            }
                        }
                        r1 = r11.limit;
                        r14 = r1 - r20.minlength;
                        if (r20.operations == null || r20.operations.type != 7 || r20.operations.getChild().type != 0) {
                            if (r20.firstChar != null) {
                                r6 = r20.firstChar;
                                r1 = r11.start;
                                r16 = -1;
                                r15 = r1;
                                while (r15 <= r14) {
                                    r1 = r21.setIndex(r15);
                                    if (mf.org.apache.xerces.impl.xpath.regex.REUtil.isHighSurrogate(r1) && r15 + 1 < r11.limit) {
                                        r1 = mf.org.apache.xerces.impl.xpath.regex.REUtil.composeFromSurrogates(r1, r21.setIndex(r15 + 1));
                                    }
                                    if (!r6.match(r1)) {
                                        r17 = r6;
                                    } else {
                                        r4 = r15;
                                        r17 = r6;
                                        r1 = match(r11, r20.operations, r4, 1, r20.options);
                                        r2 = r1;
                                        if (r1 >= 0) {
                                        } else {
                                            r16 = r2;
                                        }
                                    }
                                    r15 = r15 + 1;
                                    r6 = r17;
                                }
                            } else {
                                r1 = r11.start;
                                r16 = -1;
                                r15 = r1;
                                while (r15 <= r14) {
                                    r4 = r15;
                                    r1 = match(r11, r20.operations, r4, 1, r20.options);
                                    r16 = r1;
                                    if (r1 >= 0) {
                                    } else {
                                        r15 = r15 + 1;
                                    }
                                }
                            }
                            r2 = r16;
                        } else {
                            if (isSet(r20.options, 4)) {
                                r6 = r11.start;
                                r3 = r20.operations;
                                r4 = r11.start;
                                r2 = r20.options;
                                r1 = match(r11, r3, r4, 1, r2);
                                r2 = r1;
                                r15 = r6;
                            } else {
                                r2 = r11.start;
                                r16 = true;
                                r17 = -1;
                                r15 = r2;
                                while (true) {
                                    if (r15 > r14) {
                                        r2 = r17;
                                    } else {
                                        if (isEOLChar(r21.setIndex(r15))) {
                                            r1 = true;
                                        } else {
                                            if (r16) {
                                                r4 = r15;
                                                r1 = match(r11, r20.operations, r4, 1, r20.options);
                                                r2 = r1;
                                                if (r1 >= 0) {
                                                } else {
                                                    r17 = r2;
                                                }
                                            }
                                            r1 = false;
                                        }
                                        r16 = r1;
                                        r15 = r15 + 1;
                                    }
                                }
                            }
                        }
                        if (r2 >= 0) {
                            if (r11.match != null) {
                                r11.match.setBeginning(0, r15);
                                r11.match.setEnd(0, r2);
                            }
                            r11.setInUse(false);
                            return true;
                        } else {
                            r11.setInUse(false);
                            return false;
                        }
                    }
                }
            }
            r0 = r22;
            r11.match = r0;
            if (!isSet(r20.options, 512)) {
            }
        }
    }

    static abstract class ExpressionTarget {
        abstract char charAt(int i);

        abstract boolean regionMatches(boolean z, int i, int i2, int i3, int i4);

        abstract boolean regionMatches(boolean z, int i, int i2, String str, int i3);

        ExpressionTarget() {
        }
    }

    static final class StringTarget extends ExpressionTarget {
        private String target;

        StringTarget(String target) {
            this.target = target;
        }

        final void resetTarget(String target) {
            this.target = target;
        }

        @Override
        final char charAt(int index) {
            return this.target.charAt(index);
        }

        @Override
        final boolean regionMatches(boolean ignoreCase, int offset, int limit, String part, int partlen) {
            if (limit - offset < partlen) {
                return false;
            }
            return ignoreCase ? this.target.regionMatches(true, offset, part, 0, partlen) : this.target.regionMatches(offset, part, 0, partlen);
        }

        @Override
        final boolean regionMatches(boolean ignoreCase, int offset, int limit, int offset2, int partlen) {
            if (limit - offset < partlen) {
                return false;
            }
            return ignoreCase ? this.target.regionMatches(true, offset, this.target, offset2, partlen) : this.target.regionMatches(offset, this.target, offset2, partlen);
        }
    }

    static final class CharArrayTarget extends ExpressionTarget {
        char[] target;

        CharArrayTarget(char[] target) {
            this.target = target;
        }

        final void resetTarget(char[] target) {
            this.target = target;
        }

        @Override
        char charAt(int index) {
            return this.target[index];
        }

        @Override
        final boolean regionMatches(boolean ignoreCase, int offset, int limit, String part, int partlen) {
            if (offset < 0 || limit - offset < partlen) {
                return false;
            }
            return ignoreCase ? regionMatchesIgnoreCase(offset, limit, part, partlen) : regionMatches(offset, limit, part, partlen);
        }

        private final boolean regionMatches(int offset, int limit, String part, int i) {
            int i2 = 0;
            while (true) {
                int partlen = i - 1;
                if (i > 0) {
                    int offset2 = offset + 1;
                    int i3 = i2 + 1;
                    if (this.target[offset] == part.charAt(i2)) {
                        i2 = i3;
                        i = partlen;
                        offset = offset2;
                    } else {
                        return false;
                    }
                } else {
                    return true;
                }
            }
        }

        private final boolean regionMatchesIgnoreCase(int offset, int limit, String part, int i) {
            char uch1;
            char uch2;
            int i2 = 0;
            while (true) {
                int partlen = i - 1;
                if (i > 0) {
                    int offset2 = offset + 1;
                    char ch1 = this.target[offset];
                    int i3 = i2 + 1;
                    char ch2 = part.charAt(i2);
                    if (ch1 == ch2 || (uch1 = Character.toUpperCase(ch1)) == (uch2 = Character.toUpperCase(ch2)) || Character.toLowerCase(uch1) == Character.toLowerCase(uch2)) {
                        i2 = i3;
                        i = partlen;
                        offset = offset2;
                    } else {
                        return false;
                    }
                } else {
                    return true;
                }
            }
        }

        @Override
        final boolean regionMatches(boolean ignoreCase, int offset, int limit, int offset2, int partlen) {
            if (offset < 0 || limit - offset < partlen) {
                return false;
            }
            return ignoreCase ? regionMatchesIgnoreCase(offset, limit, offset2, partlen) : regionMatches(offset, limit, offset2, partlen);
        }

        private final boolean regionMatches(int offset, int limit, int offset2, int partlen) {
            int i = offset2;
            while (true) {
                int partlen2 = partlen - 1;
                if (partlen > 0) {
                    int offset3 = offset + 1;
                    int i2 = i + 1;
                    if (this.target[offset] == this.target[i]) {
                        partlen = partlen2;
                        offset = offset3;
                        i = i2;
                    } else {
                        return false;
                    }
                } else {
                    return true;
                }
            }
        }

        private final boolean regionMatchesIgnoreCase(int offset, int limit, int offset2, int partlen) {
            char uch1;
            char uch2;
            int i = offset2;
            while (true) {
                int partlen2 = partlen - 1;
                if (partlen > 0) {
                    int offset3 = offset + 1;
                    char ch1 = this.target[offset];
                    int i2 = i + 1;
                    char ch2 = this.target[i];
                    if (ch1 == ch2 || (uch1 = Character.toUpperCase(ch1)) == (uch2 = Character.toUpperCase(ch2)) || Character.toLowerCase(uch1) == Character.toLowerCase(uch2)) {
                        partlen = partlen2;
                        offset = offset3;
                        i = i2;
                    } else {
                        return false;
                    }
                } else {
                    return true;
                }
            }
        }
    }

    static final class CharacterIteratorTarget extends ExpressionTarget {
        CharacterIterator target;

        CharacterIteratorTarget(CharacterIterator target) {
            this.target = target;
        }

        final void resetTarget(CharacterIterator target) {
            this.target = target;
        }

        @Override
        final char charAt(int index) {
            return this.target.setIndex(index);
        }

        @Override
        final boolean regionMatches(boolean ignoreCase, int offset, int limit, String part, int partlen) {
            if (offset < 0 || limit - offset < partlen) {
                return false;
            }
            return ignoreCase ? regionMatchesIgnoreCase(offset, limit, part, partlen) : regionMatches(offset, limit, part, partlen);
        }

        private final boolean regionMatches(int offset, int limit, String part, int i) {
            int i2 = 0;
            while (true) {
                int partlen = i - 1;
                if (i > 0) {
                    int offset2 = offset + 1;
                    int i3 = i2 + 1;
                    if (this.target.setIndex(offset) == part.charAt(i2)) {
                        i2 = i3;
                        i = partlen;
                        offset = offset2;
                    } else {
                        return false;
                    }
                } else {
                    return true;
                }
            }
        }

        private final boolean regionMatchesIgnoreCase(int offset, int limit, String part, int i) {
            char uch1;
            char uch2;
            int i2 = 0;
            while (true) {
                int partlen = i - 1;
                if (i > 0) {
                    int offset2 = offset + 1;
                    char ch1 = this.target.setIndex(offset);
                    int i3 = i2 + 1;
                    char ch2 = part.charAt(i2);
                    if (ch1 == ch2 || (uch1 = Character.toUpperCase(ch1)) == (uch2 = Character.toUpperCase(ch2)) || Character.toLowerCase(uch1) == Character.toLowerCase(uch2)) {
                        i2 = i3;
                        i = partlen;
                        offset = offset2;
                    } else {
                        return false;
                    }
                } else {
                    return true;
                }
            }
        }

        @Override
        final boolean regionMatches(boolean ignoreCase, int offset, int limit, int offset2, int partlen) {
            if (offset < 0 || limit - offset < partlen) {
                return false;
            }
            return ignoreCase ? regionMatchesIgnoreCase(offset, limit, offset2, partlen) : regionMatches(offset, limit, offset2, partlen);
        }

        private final boolean regionMatches(int offset, int limit, int offset2, int partlen) {
            int i = offset2;
            while (true) {
                int partlen2 = partlen - 1;
                if (partlen > 0) {
                    int offset3 = offset + 1;
                    int i2 = i + 1;
                    if (this.target.setIndex(offset) == this.target.setIndex(i)) {
                        partlen = partlen2;
                        offset = offset3;
                        i = i2;
                    } else {
                        return false;
                    }
                } else {
                    return true;
                }
            }
        }

        private final boolean regionMatchesIgnoreCase(int offset, int limit, int offset2, int partlen) {
            char uch1;
            char uch2;
            int i = offset2;
            while (true) {
                int partlen2 = partlen - 1;
                if (partlen > 0) {
                    int offset3 = offset + 1;
                    char ch1 = this.target.setIndex(offset);
                    int i2 = i + 1;
                    char ch2 = this.target.setIndex(i);
                    if (ch1 == ch2 || (uch1 = Character.toUpperCase(ch1)) == (uch2 = Character.toUpperCase(ch2)) || Character.toLowerCase(uch1) == Character.toLowerCase(uch2)) {
                        partlen = partlen2;
                        offset = offset3;
                        i = i2;
                    } else {
                        return false;
                    }
                } else {
                    return true;
                }
            }
        }
    }

    static final class ClosureContext {
        int[] offsets = new int[4];
        int currentIndex = 0;

        ClosureContext() {
        }

        boolean contains(int offset) {
            for (int i = 0; i < this.currentIndex; i++) {
                if (this.offsets[i] == offset) {
                    return true;
                }
            }
            return false;
        }

        void reset() {
            this.currentIndex = 0;
        }

        void addOffset(int offset) {
            if (this.currentIndex == this.offsets.length) {
                this.offsets = expandOffsets();
            }
            int[] iArr = this.offsets;
            int i = this.currentIndex;
            this.currentIndex = i + 1;
            iArr[i] = offset;
        }

        private int[] expandOffsets() {
            int len = this.offsets.length;
            int newLen = len << 1;
            int[] newOffsets = new int[newLen];
            System.arraycopy(this.offsets, 0, newOffsets, 0, this.currentIndex);
            return newOffsets;
        }
    }

    static final class Context {
        private CharArrayTarget charArrayTarget;
        private CharacterIteratorTarget characterIteratorTarget;
        ClosureContext[] closureContexts;
        boolean inuse = false;
        int length;
        int limit;
        Match match;
        int start;
        private StringTarget stringTarget;
        ExpressionTarget target;

        Context() {
        }

        private void resetCommon(int nofclosures) {
            this.length = this.limit - this.start;
            setInUse(true);
            this.match = null;
            if (this.closureContexts == null || this.closureContexts.length != nofclosures) {
                this.closureContexts = new ClosureContext[nofclosures];
            }
            for (int i = 0; i < nofclosures; i++) {
                if (this.closureContexts[i] == null) {
                    this.closureContexts[i] = new ClosureContext();
                } else {
                    this.closureContexts[i].reset();
                }
            }
        }

        void reset(CharacterIterator target, int start, int limit, int nofclosures) {
            if (this.characterIteratorTarget == null) {
                this.characterIteratorTarget = new CharacterIteratorTarget(target);
            } else {
                this.characterIteratorTarget.resetTarget(target);
            }
            this.target = this.characterIteratorTarget;
            this.start = start;
            this.limit = limit;
            resetCommon(nofclosures);
        }

        void reset(String target, int start, int limit, int nofclosures) {
            if (this.stringTarget == null) {
                this.stringTarget = new StringTarget(target);
            } else {
                this.stringTarget.resetTarget(target);
            }
            this.target = this.stringTarget;
            this.start = start;
            this.limit = limit;
            resetCommon(nofclosures);
        }

        void reset(char[] target, int start, int limit, int nofclosures) {
            if (this.charArrayTarget == null) {
                this.charArrayTarget = new CharArrayTarget(target);
            } else {
                this.charArrayTarget.resetTarget(target);
            }
            this.target = this.charArrayTarget;
            this.start = start;
            this.limit = limit;
            resetCommon(nofclosures);
        }

        synchronized void setInUse(boolean inUse) {
            this.inuse = inUse;
        }
    }

    void prepare() {
        compile(this.tokentree);
        this.minlength = this.tokentree.getMinLength();
        this.firstChar = null;
        if (!isSet(this.options, 128) && !isSet(this.options, 512)) {
            RangeToken firstChar = Token.createRange();
            int fresult = this.tokentree.analyzeFirstCharacter(firstChar, this.options);
            if (fresult == 1) {
                firstChar.compactRanges();
                this.firstChar = firstChar;
            }
        }
        if (this.operations != null && ((this.operations.type == 6 || this.operations.type == 1) && this.operations.next == null)) {
            this.fixedStringOnly = true;
            if (this.operations.type == 6) {
                this.fixedString = this.operations.getString();
            } else if (this.operations.getData() >= 65536) {
                this.fixedString = REUtil.decomposeToSurrogates(this.operations.getData());
            } else {
                char[] ac = {(char) this.operations.getData()};
                this.fixedString = new String(ac);
            }
            this.fixedStringOptions = this.options;
            this.fixedStringTable = new BMPattern(this.fixedString, 256, isSet(this.fixedStringOptions, 2));
            return;
        }
        if (!isSet(this.options, 256) && !isSet(this.options, 512)) {
            Token.FixedStringContainer container = new Token.FixedStringContainer();
            this.tokentree.findFixedString(container, this.options);
            this.fixedString = container.token == null ? null : container.token.getString();
            this.fixedStringOptions = container.options;
            if (this.fixedString != null && this.fixedString.length() < 2) {
                this.fixedString = null;
            }
            if (this.fixedString != null) {
                this.fixedStringTable = new BMPattern(this.fixedString, 256, isSet(this.fixedStringOptions, 2));
            }
        }
    }

    private static final boolean isSet(int options, int flag) {
        return (options & flag) == flag;
    }

    public RegularExpression(String regex) throws ParseException {
        this(regex, null);
    }

    public RegularExpression(String regex, String options) throws ParseException {
        this.hasBackReferences = false;
        this.operations = null;
        this.context = null;
        this.firstChar = null;
        this.fixedString = null;
        this.fixedStringTable = null;
        this.fixedStringOnly = false;
        setPattern(regex, options);
    }

    public RegularExpression(String regex, String options, Locale locale) throws ParseException {
        this.hasBackReferences = false;
        this.operations = null;
        this.context = null;
        this.firstChar = null;
        this.fixedString = null;
        this.fixedStringTable = null;
        this.fixedStringOnly = false;
        setPattern(regex, options, locale);
    }

    RegularExpression(String regex, Token tok, int parens, boolean hasBackReferences, int options) {
        this.hasBackReferences = false;
        this.operations = null;
        this.context = null;
        this.firstChar = null;
        this.fixedString = null;
        this.fixedStringTable = null;
        this.fixedStringOnly = false;
        this.regex = regex;
        this.tokentree = tok;
        this.nofparen = parens;
        this.options = options;
        this.hasBackReferences = hasBackReferences;
    }

    public void setPattern(String newPattern) throws ParseException {
        setPattern(newPattern, Locale.getDefault());
    }

    public void setPattern(String newPattern, Locale locale) throws ParseException {
        setPattern(newPattern, this.options, locale);
    }

    private void setPattern(String newPattern, int options, Locale locale) throws ParseException {
        this.regex = newPattern;
        this.options = options;
        RegexParser rp = isSet(this.options, 512) ? new ParserForXMLSchema(locale) : new RegexParser(locale);
        this.tokentree = rp.parse(this.regex, this.options);
        this.nofparen = rp.parennumber;
        this.hasBackReferences = rp.hasBackReferences;
        this.operations = null;
        this.context = null;
    }

    public void setPattern(String newPattern, String options) throws ParseException {
        setPattern(newPattern, options, Locale.getDefault());
    }

    public void setPattern(String newPattern, String options, Locale locale) throws ParseException {
        setPattern(newPattern, REUtil.parseOptions(options), locale);
    }

    public String getPattern() {
        return this.regex;
    }

    public String toString() {
        return this.tokentree.toString(this.options);
    }

    public String getOptions() {
        return REUtil.createOptionString(this.options);
    }

    public boolean equals(Object obj) {
        return obj != 0 && (obj instanceof RegularExpression) && this.regex.equals(obj.regex) && this.options == obj.options;
    }

    boolean equals(String pattern, int options) {
        return this.regex.equals(pattern) && this.options == options;
    }

    public int hashCode() {
        return (String.valueOf(this.regex) + "/" + getOptions()).hashCode();
    }

    public int getNumberOfGroups() {
        return this.nofparen;
    }

    private static final int getWordType0(char r4, int r5) {
        if (!isSet(r5, 64)) {
            if (isSet(r5, 32)) {
                if (mf.org.apache.xerces.impl.xpath.regex.Token.getRange("IsWord", true).match(r4)) {
                    return 1;
                } else {
                    return 2;
                }
            } else {
                if (isWordChar(r4)) {
                    return 1;
                } else {
                    return 2;
                }
            }
        } else {
            switch (java.lang.Character.getType(r4)) {
                case 15:
                    switch (r4) {
                    }
            }
            return 1;
        }
    }

    private static final boolean isEOLChar(int ch) {
        return ch == 10 || ch == 13 || ch == LINE_SEPARATOR || ch == PARAGRAPH_SEPARATOR;
    }

    private static final boolean isWordChar(int ch) {
        if (ch == 95) {
            return true;
        }
        if (ch < 48 || ch > 122) {
            return false;
        }
        if (ch <= 57) {
            return true;
        }
        if (ch < 65) {
            return false;
        }
        return ch <= 90 || ch >= 97;
    }

    private static final boolean matchIgnoreCase(int chardata, int ch) {
        if (chardata == ch) {
            return true;
        }
        if (chardata > 65535 || ch > 65535) {
            return false;
        }
        char uch1 = Character.toUpperCase((char) chardata);
        char uch2 = Character.toUpperCase((char) ch);
        if (uch1 == uch2 || Character.toLowerCase(uch1) == Character.toLowerCase(uch2)) {
            return true;
        }
        return false;
    }
}
