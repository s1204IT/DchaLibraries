package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.address.GenericURI;
import java.text.ParseException;

public class AddressParser extends Parser {
    public AddressParser(Lexer lexer) {
        this.lexer = lexer;
        this.lexer.selectLexer("charLexer");
    }

    public AddressParser(String str) {
        this.lexer = new Lexer("charLexer", str);
    }

    protected AddressImpl nameAddr() throws ParseException {
        String nextToken;
        if (debug) {
            dbg_enter("nameAddr");
        }
        try {
            if (this.lexer.lookAhead(0) == '<') {
                this.lexer.consume(1);
                this.lexer.selectLexer("sip_urlLexer");
                this.lexer.SPorHT();
                GenericURI genericURIUriReference = new URLParser((Lexer) this.lexer).uriReference(true);
                AddressImpl addressImpl = new AddressImpl();
                addressImpl.setAddressType(1);
                addressImpl.setURI(genericURIUriReference);
                this.lexer.SPorHT();
                this.lexer.match(62);
                return addressImpl;
            }
            AddressImpl addressImpl2 = new AddressImpl();
            addressImpl2.setAddressType(1);
            if (this.lexer.lookAhead(0) == '\"') {
                nextToken = this.lexer.quotedString();
                this.lexer.SPorHT();
            } else {
                nextToken = this.lexer.getNextToken('<');
            }
            addressImpl2.setDisplayName(nextToken.trim());
            this.lexer.match(60);
            this.lexer.SPorHT();
            GenericURI genericURIUriReference2 = new URLParser((Lexer) this.lexer).uriReference(true);
            new AddressImpl();
            addressImpl2.setAddressType(1);
            addressImpl2.setURI(genericURIUriReference2);
            this.lexer.SPorHT();
            this.lexer.match(62);
            if (debug) {
                dbg_leave("nameAddr");
            }
            return addressImpl2;
        } finally {
            if (debug) {
                dbg_leave("nameAddr");
            }
        }
    }

    public AddressImpl address(boolean z) throws ParseException {
        AddressImpl addressImplNameAddr;
        char cLookAhead;
        if (debug) {
            dbg_enter("address");
        }
        int i = 0;
        while (this.lexer.hasMoreChars() && (cLookAhead = this.lexer.lookAhead(i)) != '<' && cLookAhead != '\"' && cLookAhead != ':' && cLookAhead != '/') {
            try {
                if (cLookAhead == 0) {
                    throw createParseException("unexpected EOL");
                }
                i++;
            } finally {
                if (debug) {
                    dbg_leave("address");
                }
            }
        }
        char cLookAhead2 = this.lexer.lookAhead(i);
        if (cLookAhead2 == '<' || cLookAhead2 == '\"') {
            addressImplNameAddr = nameAddr();
        } else {
            if (cLookAhead2 != ':' && cLookAhead2 != '/') {
                throw createParseException("Bad address spec");
            }
            addressImplNameAddr = new AddressImpl();
            GenericURI genericURIUriReference = new URLParser((Lexer) this.lexer).uriReference(z);
            addressImplNameAddr.setAddressType(2);
            addressImplNameAddr.setURI(genericURIUriReference);
        }
        return addressImplNameAddr;
    }
}
