package gov.nist.javax.sip.parser;

import gov.nist.core.Separators;
import gov.nist.javax.sip.header.StatusLine;
import java.text.ParseException;

public class StatusLineParser extends Parser {
    public StatusLineParser(String statusLine) {
        this.lexer = new Lexer("status_lineLexer", statusLine);
    }

    public StatusLineParser(Lexer lexer) {
        this.lexer = lexer;
        this.lexer.selectLexer("status_lineLexer");
    }

    protected int statusCode() throws ParseException {
        String scode = this.lexer.number();
        if (debug) {
            dbg_enter("statusCode");
        }
        try {
            try {
                int retval = Integer.parseInt(scode);
                return retval;
            } catch (NumberFormatException ex) {
                throw new ParseException(this.lexer.getBuffer() + Separators.COLON + ex.getMessage(), this.lexer.getPtr());
            }
        } finally {
            if (debug) {
                dbg_leave("statusCode");
            }
        }
    }

    protected String reasonPhrase() throws ParseException {
        return this.lexer.getRest().trim();
    }

    public StatusLine parse() throws ParseException {
        try {
            if (debug) {
                dbg_enter("parse");
            }
            StatusLine retval = new StatusLine();
            String version = sipVersion();
            retval.setSipVersion(version);
            this.lexer.SPorHT();
            int scode = statusCode();
            retval.setStatusCode(scode);
            this.lexer.SPorHT();
            String rp = reasonPhrase();
            retval.setReasonPhrase(rp);
            this.lexer.SPorHT();
            return retval;
        } finally {
            if (debug) {
                dbg_leave("parse");
            }
        }
    }
}
