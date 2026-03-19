package gov.nist.javax.sip.parser.ims;

import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.ims.PChargingVector;
import gov.nist.javax.sip.header.ims.ParameterNamesIms;
import gov.nist.javax.sip.parser.Lexer;
import gov.nist.javax.sip.parser.ParametersParser;
import gov.nist.javax.sip.parser.TokenTypes;
import java.text.ParseException;

public class PChargingVectorParser extends ParametersParser implements TokenTypes {
    public PChargingVectorParser(String str) {
        super(str);
    }

    protected PChargingVectorParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        if (debug) {
            dbg_enter("parse");
        }
        try {
            headerName(TokenTypes.P_VECTOR_CHARGING);
            PChargingVector pChargingVector = new PChargingVector();
            while (this.lexer.lookAhead(0) != '\n') {
                try {
                    parseParameter(pChargingVector);
                    this.lexer.SPorHT();
                    char cLookAhead = this.lexer.lookAhead(0);
                    if (cLookAhead == '\n' || cLookAhead == 0) {
                        break;
                    }
                    this.lexer.match(59);
                    this.lexer.SPorHT();
                } catch (ParseException e) {
                    throw e;
                }
            }
            super.parse(pChargingVector);
            if (pChargingVector.getParameter(ParameterNamesIms.ICID_VALUE) == null) {
                throw new ParseException("Missing a required Parameter : icid-value", 0);
            }
            return pChargingVector;
        } finally {
            if (debug) {
                dbg_leave("parse");
            }
        }
    }

    protected void parseParameter(PChargingVector pChargingVector) throws ParseException {
        if (debug) {
            dbg_enter("parseParameter");
        }
        try {
            pChargingVector.setParameter(nameValue('='));
        } finally {
            if (debug) {
                dbg_leave("parseParameter");
            }
        }
    }
}
