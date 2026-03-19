package gov.nist.javax.sip.parser.ims;

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
    public SecurityAgreeParser(String str) {
        super(str);
    }

    protected SecurityAgreeParser(Lexer lexer) {
        super(lexer);
    }

    protected void parseParameter(SecurityAgree securityAgree) throws ParseException {
        if (debug) {
            dbg_enter("parseParameter");
        }
        try {
            securityAgree.setParameter(nameValue('='));
        } finally {
            if (debug) {
                dbg_leave("parseParameter");
            }
        }
    }

    public SIPHeaderList parse(SecurityAgree securityAgree) throws ParseException {
        SIPHeaderList securityVerifyList;
        if (securityAgree.getClass().isInstance(new SecurityClient())) {
            securityVerifyList = new SecurityClientList();
        } else if (securityAgree.getClass().isInstance(new SecurityServer())) {
            securityVerifyList = new SecurityServerList();
        } else if (securityAgree.getClass().isInstance(new SecurityVerify())) {
            securityVerifyList = new SecurityVerifyList();
        } else {
            return null;
        }
        this.lexer.SPorHT();
        this.lexer.match(4095);
        securityAgree.setSecurityMechanism(this.lexer.getNextToken().getTokenValue());
        this.lexer.SPorHT();
        char cLookAhead = this.lexer.lookAhead(0);
        if (cLookAhead == '\n') {
            securityVerifyList.add(securityAgree);
            return securityVerifyList;
        }
        if (cLookAhead == ';') {
            this.lexer.match(59);
        }
        this.lexer.SPorHT();
        while (this.lexer.lookAhead(0) != '\n') {
            try {
                parseParameter(securityAgree);
                this.lexer.SPorHT();
                char cLookAhead2 = this.lexer.lookAhead(0);
                if (cLookAhead2 == '\n' || cLookAhead2 == 0) {
                    break;
                }
                if (cLookAhead2 == ',') {
                    securityVerifyList.add(securityAgree);
                    if (securityAgree.getClass().isInstance(new SecurityClient())) {
                        securityAgree = new SecurityClient();
                    } else if (securityAgree.getClass().isInstance(new SecurityServer())) {
                        securityAgree = new SecurityServer();
                    } else if (securityAgree.getClass().isInstance(new SecurityVerify())) {
                        securityAgree = new SecurityVerify();
                    }
                    this.lexer.match(44);
                    this.lexer.SPorHT();
                    this.lexer.match(4095);
                    securityAgree.setSecurityMechanism(this.lexer.getNextToken().getTokenValue());
                }
                this.lexer.SPorHT();
                if (this.lexer.lookAhead(0) == ';') {
                    this.lexer.match(59);
                }
                this.lexer.SPorHT();
            } catch (ParseException e) {
                throw e;
            }
        }
        securityVerifyList.add(securityAgree);
        return securityVerifyList;
    }
}
