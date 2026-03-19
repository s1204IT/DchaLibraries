package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.ExtensionHeaderImpl;
import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;
import java.util.Calendar;
import java.util.TimeZone;

public class HeaderParser extends Parser {
    protected int wkday() throws ParseException {
        String str;
        dbg_enter("wkday");
        try {
            String lowerCase = this.lexer.ttoken().toLowerCase();
            if ("Mon".equalsIgnoreCase(lowerCase)) {
                return 2;
            }
            if ("Tue".equalsIgnoreCase(lowerCase)) {
                return 3;
            }
            if ("Wed".equalsIgnoreCase(lowerCase)) {
                return 4;
            }
            if ("Thu".equalsIgnoreCase(lowerCase)) {
                return 5;
            }
            if ("Fri".equalsIgnoreCase(lowerCase)) {
                return 6;
            }
            if ("Sat".equalsIgnoreCase(lowerCase)) {
                return 7;
            }
            if ("Sun".equalsIgnoreCase(lowerCase)) {
                return 1;
            }
            throw createParseException("bad wkday");
        } finally {
            dbg_leave("wkday");
        }
    }

    protected Calendar date() throws ParseException {
        try {
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            int i = Integer.parseInt(this.lexer.number());
            if (i <= 0 || i > 31) {
                throw createParseException("Bad day ");
            }
            calendar.set(5, i);
            this.lexer.match(32);
            String lowerCase = this.lexer.ttoken().toLowerCase();
            if (lowerCase.equals("jan")) {
                calendar.set(2, 0);
            } else if (lowerCase.equals("feb")) {
                calendar.set(2, 1);
            } else if (lowerCase.equals("mar")) {
                calendar.set(2, 2);
            } else if (lowerCase.equals("apr")) {
                calendar.set(2, 3);
            } else if (lowerCase.equals("may")) {
                calendar.set(2, 4);
            } else if (lowerCase.equals("jun")) {
                calendar.set(2, 5);
            } else if (lowerCase.equals("jul")) {
                calendar.set(2, 6);
            } else if (lowerCase.equals("aug")) {
                calendar.set(2, 7);
            } else if (lowerCase.equals("sep")) {
                calendar.set(2, 8);
            } else if (lowerCase.equals("oct")) {
                calendar.set(2, 9);
            } else if (lowerCase.equals("nov")) {
                calendar.set(2, 10);
            } else if (lowerCase.equals("dec")) {
                calendar.set(2, 11);
            }
            this.lexer.match(32);
            calendar.set(1, Integer.parseInt(this.lexer.number()));
            return calendar;
        } catch (Exception e) {
            throw createParseException("bad date field");
        }
    }

    protected void time(Calendar calendar) throws ParseException {
        try {
            calendar.set(11, Integer.parseInt(this.lexer.number()));
            this.lexer.match(58);
            calendar.set(12, Integer.parseInt(this.lexer.number()));
            this.lexer.match(58);
            calendar.set(13, Integer.parseInt(this.lexer.number()));
        } catch (Exception e) {
            throw createParseException("error processing time ");
        }
    }

    protected HeaderParser(String str) {
        this.lexer = new Lexer("command_keywordLexer", str);
    }

    protected HeaderParser(Lexer lexer) {
        this.lexer = lexer;
        this.lexer.selectLexer("command_keywordLexer");
    }

    public SIPHeader parse() throws ParseException {
        String nextToken = this.lexer.getNextToken(':');
        this.lexer.consume(1);
        String strTrim = this.lexer.getLine().trim();
        ExtensionHeaderImpl extensionHeaderImpl = new ExtensionHeaderImpl(nextToken);
        extensionHeaderImpl.setValue(strTrim);
        return extensionHeaderImpl;
    }

    protected void headerName(int i) throws ParseException {
        this.lexer.match(i);
        this.lexer.SPorHT();
        this.lexer.match(58);
        this.lexer.SPorHT();
    }
}
