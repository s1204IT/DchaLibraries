package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.address.GenericURI;
import java.text.ParseException;

public class AddressParser extends Parser {
    public AddressParser(Lexer lexer) {
        this.lexer = lexer;
        this.lexer.selectLexer("charLexer");
    }

    public AddressParser(String address) {
        this.lexer = new Lexer("charLexer", address);
    }

    protected AddressImpl nameAddr() throws ParseException {
        String name;
        if (debug) {
            dbg_enter("nameAddr");
        }
        try {
            if (this.lexer.lookAhead(0) == '<') {
                this.lexer.consume(1);
                this.lexer.selectLexer("sip_urlLexer");
                this.lexer.SPorHT();
                URLParser uriParser = new URLParser((Lexer) this.lexer);
                GenericURI uri = uriParser.uriReference(true);
                AddressImpl retval = new AddressImpl();
                retval.setAddressType(1);
                retval.setURI(uri);
                this.lexer.SPorHT();
                this.lexer.match(62);
                return retval;
            }
            AddressImpl addr = new AddressImpl();
            addr.setAddressType(1);
            if (this.lexer.lookAhead(0) == '\"') {
                name = this.lexer.quotedString();
                this.lexer.SPorHT();
            } else {
                name = this.lexer.getNextToken('<');
            }
            addr.setDisplayName(name.trim());
            this.lexer.match(60);
            this.lexer.SPorHT();
            URLParser uriParser2 = new URLParser((Lexer) this.lexer);
            GenericURI uri2 = uriParser2.uriReference(true);
            new AddressImpl();
            addr.setAddressType(1);
            addr.setURI(uri2);
            this.lexer.SPorHT();
            this.lexer.match(62);
            if (debug) {
                dbg_leave("nameAddr");
            }
            return addr;
        } finally {
            if (debug) {
                dbg_leave("nameAddr");
            }
        }
    }

    public AddressImpl address(boolean inclParams) throws Throwable {
        AddressImpl retval;
        char la;
        if (debug) {
            dbg_enter("address");
        }
        int k = 0;
        while (this.lexer.hasMoreChars() && (la = this.lexer.lookAhead(k)) != '<' && la != '\"' && la != ':' && la != '/') {
            try {
                if (la == 0) {
                    throw createParseException("unexpected EOL");
                }
                k++;
            } catch (Throwable th) {
                th = th;
            }
        }
        char la2 = this.lexer.lookAhead(k);
        if (la2 == '<' || la2 == '\"') {
            retval = nameAddr();
        } else if (la2 == ':' || la2 == '/') {
            AddressImpl retval2 = new AddressImpl();
            try {
                URLParser uriParser = new URLParser((Lexer) this.lexer);
                GenericURI uri = uriParser.uriReference(inclParams);
                retval2.setAddressType(2);
                retval2.setURI(uri);
                retval = retval2;
            } catch (Throwable th2) {
                th = th2;
                if (debug) {
                    dbg_leave("address");
                }
                throw th;
            }
        } else {
            throw createParseException("Bad address spec");
        }
        if (debug) {
            dbg_leave("address");
        }
        return retval;
    }
}
