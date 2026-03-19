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
            String tok = this.lexer.ttoken();
            String id = tok.toLowerCase();
            if ("Mon".equalsIgnoreCase(id)) {
                return 2;
            }
            if ("Tue".equalsIgnoreCase(id)) {
                return 3;
            }
            if ("Wed".equalsIgnoreCase(id)) {
                return 4;
            }
            if ("Thu".equalsIgnoreCase(id)) {
                return 5;
            }
            if ("Fri".equalsIgnoreCase(id)) {
                return 6;
            }
            if ("Sat".equalsIgnoreCase(id)) {
                return 7;
            }
            if ("Sun".equalsIgnoreCase(id)) {
                return 1;
            }
            throw createParseException("bad wkday");
        } finally {
            dbg_leave("wkday");
        }
    }

    protected Calendar date() throws ParseException {
        try {
            Calendar retval = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            String s1 = this.lexer.number();
            int day = Integer.parseInt(s1);
            if (day <= 0 || day > 31) {
                throw createParseException("Bad day ");
            }
            retval.set(5, day);
            this.lexer.match(32);
            String month = this.lexer.ttoken().toLowerCase();
            if (month.equals("jan")) {
                retval.set(2, 0);
            } else if (month.equals("feb")) {
                retval.set(2, 1);
            } else if (month.equals("mar")) {
                retval.set(2, 2);
            } else if (month.equals("apr")) {
                retval.set(2, 3);
            } else if (month.equals("may")) {
                retval.set(2, 4);
            } else if (month.equals("jun")) {
                retval.set(2, 5);
            } else if (month.equals("jul")) {
                retval.set(2, 6);
            } else if (month.equals("aug")) {
                retval.set(2, 7);
            } else if (month.equals("sep")) {
                retval.set(2, 8);
            } else if (month.equals("oct")) {
                retval.set(2, 9);
            } else if (month.equals("nov")) {
                retval.set(2, 10);
            } else if (month.equals("dec")) {
                retval.set(2, 11);
            }
            this.lexer.match(32);
            String s2 = this.lexer.number();
            int yr = Integer.parseInt(s2);
            retval.set(1, yr);
            return retval;
        } catch (Exception e) {
            throw createParseException("bad date field");
        }
    }

    protected void time(Calendar calendar) throws ParseException {
        try {
            String s = this.lexer.number();
            int hour = Integer.parseInt(s);
            calendar.set(11, hour);
            this.lexer.match(58);
            String s2 = this.lexer.number();
            int min = Integer.parseInt(s2);
            calendar.set(12, min);
            this.lexer.match(58);
            String s3 = this.lexer.number();
            int sec = Integer.parseInt(s3);
            calendar.set(13, sec);
        } catch (Exception e) {
            throw createParseException("error processing time ");
        }
    }

    protected HeaderParser(String header) {
        this.lexer = new Lexer("command_keywordLexer", header);
    }

    protected HeaderParser(Lexer lexer) {
        this.lexer = lexer;
        this.lexer.selectLexer("command_keywordLexer");
    }

    public SIPHeader parse() throws ParseException {
        String name = this.lexer.getNextToken(':');
        this.lexer.consume(1);
        String body = this.lexer.getLine().trim();
        ExtensionHeaderImpl retval = new ExtensionHeaderImpl(name);
        retval.setValue(body);
        return retval;
    }

    protected void headerName(int tok) throws ParseException {
        this.lexer.match(tok);
        this.lexer.SPorHT();
        this.lexer.match(58);
        this.lexer.SPorHT();
    }
}
