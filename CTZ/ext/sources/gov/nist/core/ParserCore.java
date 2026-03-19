package gov.nist.core;

import java.text.ParseException;

public abstract class ParserCore {
    public static final boolean debug = Debug.parserDebug;
    static int nesting_level;
    protected LexerCore lexer;

    protected NameValue nameValue(char c) throws ParseException {
        String strQuotedString;
        boolean z;
        if (debug) {
            dbg_enter("nameValue");
        }
        try {
            this.lexer.match(4095);
            Token nextToken = this.lexer.getNextToken();
            this.lexer.SPorHT();
            try {
                boolean z2 = true;
                if (this.lexer.lookAhead(0) != c) {
                    NameValue nameValue = new NameValue(nextToken.tokenValue, "", true);
                    if (debug) {
                        dbg_leave("nameValue");
                    }
                    return nameValue;
                }
                this.lexer.consume(1);
                this.lexer.SPorHT();
                if (this.lexer.lookAhead(0) == '\"') {
                    strQuotedString = this.lexer.quotedString();
                    z = true;
                    z2 = false;
                } else {
                    this.lexer.match(4095);
                    strQuotedString = this.lexer.getNextToken().tokenValue;
                    if (strQuotedString == null) {
                        strQuotedString = "";
                        z = false;
                    } else {
                        z = false;
                        z2 = false;
                    }
                }
                NameValue nameValue2 = new NameValue(nextToken.tokenValue, strQuotedString, z2);
                if (z) {
                    nameValue2.setQuotedValue();
                }
                if (debug) {
                    dbg_leave("nameValue");
                }
                return nameValue2;
            } catch (ParseException e) {
                NameValue nameValue3 = new NameValue(nextToken.tokenValue, null, false);
                if (debug) {
                    dbg_leave("nameValue");
                }
                return nameValue3;
            }
        } catch (Throwable th) {
            if (debug) {
                dbg_leave("nameValue");
            }
            throw th;
        }
    }

    protected void dbg_enter(String str) {
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < nesting_level; i++) {
            stringBuffer.append(Separators.GREATER_THAN);
        }
        if (debug) {
            System.out.println(((Object) stringBuffer) + str + "\nlexer buffer = \n" + this.lexer.getRest());
        }
        nesting_level++;
    }

    protected void dbg_leave(String str) {
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < nesting_level; i++) {
            stringBuffer.append(Separators.LESS_THAN);
        }
        if (debug) {
            System.out.println(((Object) stringBuffer) + str + "\nlexer buffer = \n" + this.lexer.getRest());
        }
        nesting_level--;
    }

    protected NameValue nameValue() throws ParseException {
        return nameValue('=');
    }

    protected void peekLine(String str) {
        if (debug) {
            Debug.println(str + Separators.SP + this.lexer.peekLine());
        }
    }
}
