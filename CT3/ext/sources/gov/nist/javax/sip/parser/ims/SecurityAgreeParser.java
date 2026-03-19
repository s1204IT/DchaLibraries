package gov.nist.javax.sip.parser.ims;

import gov.nist.core.NameValue;
import gov.nist.core.Token;
import gov.nist.javax.sip.header.SIPHeaderList;
import gov.nist.javax.sip.header.ims.SecurityAgree;
import gov.nist.javax.sip.header.ims.SecurityClient;
import gov.nist.javax.sip.header.ims.SecurityClientList;
import gov.nist.javax.sip.header.ims.SecurityServer;
import gov.nist.javax.sip.header.ims.SecurityServerList;
import gov.nist.javax.sip.header.ims.SecurityVerify;
import gov.nist.javax.sip.header.ims.SecurityVerifyList;
import gov.nist.javax.sip.parser.HeaderParser;
import gov.nist.javax.sip.parser.Lexer;
import java.text.ParseException;

public class SecurityAgreeParser extends HeaderParser {
    public SecurityAgreeParser(String security) {
        super(security);
    }

    protected SecurityAgreeParser(Lexer lexer) {
        super(lexer);
    }

    protected void parseParameter(SecurityAgree header) throws ParseException {
        if (debug) {
            dbg_enter("parseParameter");
        }
        try {
            NameValue nv = nameValue('=');
            header.setParameter(nv);
        } finally {
            if (debug) {
                dbg_leave("parseParameter");
            }
        }
    }

    public SIPHeaderList parse(SecurityAgree header) throws ParseException {
        SIPHeaderList list;
        SecurityAgree header2;
        if (header.getClass().isInstance(new SecurityClient())) {
            list = new SecurityClientList();
        } else if (header.getClass().isInstance(new SecurityServer())) {
            list = new SecurityServerList();
        } else if (header.getClass().isInstance(new SecurityVerify())) {
            list = new SecurityVerifyList();
        } else {
            return null;
        }
        this.lexer.SPorHT();
        this.lexer.match(4095);
        Token type = this.lexer.getNextToken();
        header.setSecurityMechanism(type.getTokenValue());
        this.lexer.SPorHT();
        char la = this.lexer.lookAhead(0);
        if (la == '\n') {
            list.add(header);
            return list;
        }
        if (la == ';') {
            this.lexer.match(59);
        }
        this.lexer.SPorHT();
        while (true) {
            try {
                header2 = header;
                if (this.lexer.lookAhead(0) == '\n') {
                    break;
                }
                parseParameter(header2);
                this.lexer.SPorHT();
                char laInLoop = this.lexer.lookAhead(0);
                if (laInLoop == '\n' || laInLoop == 0) {
                    break;
                }
                if (laInLoop == ',') {
                    list.add(header2);
                    if (header2.getClass().isInstance(new SecurityClient())) {
                        header = new SecurityClient();
                    } else if (header2.getClass().isInstance(new SecurityServer())) {
                        header = new SecurityServer();
                    } else {
                        header = header2.getClass().isInstance(new SecurityVerify()) ? new SecurityVerify() : header2;
                    }
                    try {
                        this.lexer.match(44);
                        this.lexer.SPorHT();
                        this.lexer.match(4095);
                        Token type2 = this.lexer.getNextToken();
                        header.setSecurityMechanism(type2.getTokenValue());
                    } catch (ParseException ex) {
                        throw ex;
                    }
                } else {
                    header = header2;
                }
                this.lexer.SPorHT();
                if (this.lexer.lookAhead(0) == ';') {
                    this.lexer.match(59);
                }
                this.lexer.SPorHT();
            } catch (ParseException ex2) {
                throw ex2;
            }
        }
        list.add(header2);
        return list;
    }
}
