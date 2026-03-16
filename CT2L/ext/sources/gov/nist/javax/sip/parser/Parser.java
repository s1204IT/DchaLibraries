package gov.nist.javax.sip.parser;

import gov.nist.core.Debug;
import gov.nist.core.LexerCore;
import gov.nist.core.ParserCore;
import gov.nist.core.Separators;
import gov.nist.core.Token;
import gov.nist.javax.sip.SIPConstants;
import java.text.ParseException;
import org.ccil.cowan.tagsoup.XMLWriter;

public abstract class Parser extends ParserCore implements TokenTypes {
    protected ParseException createParseException(String exceptionString) {
        return new ParseException(this.lexer.getBuffer() + Separators.COLON + exceptionString, this.lexer.getPtr());
    }

    protected Lexer getLexer() {
        return (Lexer) this.lexer;
    }

    protected String sipVersion() throws ParseException {
        if (debug) {
            dbg_enter("sipVersion");
        }
        try {
            Token tok = this.lexer.match(TokenTypes.SIP);
            if (!tok.getTokenValue().equalsIgnoreCase("SIP")) {
                createParseException("Expecting SIP");
            }
            this.lexer.match(47);
            Token tok2 = this.lexer.match(4095);
            if (!tok2.getTokenValue().equals("2.0")) {
                createParseException("Expecting SIP/2.0");
            }
            return SIPConstants.SIP_VERSION_STRING;
        } finally {
            if (debug) {
                dbg_leave("sipVersion");
            }
        }
    }

    protected String method() throws ParseException {
        try {
            if (debug) {
                dbg_enter(XMLWriter.METHOD);
            }
            Token[] tokens = this.lexer.peekNextToken(1);
            Token token = tokens[0];
            if (token.getTokenType() == 2053 || token.getTokenType() == 2054 || token.getTokenType() == 2056 || token.getTokenType() == 2055 || token.getTokenType() == 2052 || token.getTokenType() == 2057 || token.getTokenType() == 2101 || token.getTokenType() == 2102 || token.getTokenType() == 2115 || token.getTokenType() == 2118 || token.getTokenType() == 4095) {
                this.lexer.consume();
                return token.getTokenValue();
            }
            throw createParseException("Invalid Method");
        } finally {
            if (Debug.debug) {
                dbg_leave(XMLWriter.METHOD);
            }
        }
    }

    public static final void checkToken(String token) throws ParseException {
        if (token == null || token.length() == 0) {
            throw new ParseException("null or empty token", -1);
        }
        for (int i = 0; i < token.length(); i++) {
            if (!LexerCore.isTokenChar(token.charAt(i))) {
                throw new ParseException("Invalid character(s) in string (not allowed in 'token')", i);
            }
        }
    }
}
