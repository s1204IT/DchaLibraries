package gov.nist.javax.sip.parser.ims;

import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.ims.PAssertedService;
import gov.nist.javax.sip.header.ims.ParameterNamesIms;
import gov.nist.javax.sip.parser.HeaderParser;
import gov.nist.javax.sip.parser.Lexer;
import gov.nist.javax.sip.parser.TokenTypes;
import java.text.ParseException;
import javax.sip.InvalidArgumentException;

public class PAssertedServiceParser extends HeaderParser implements TokenTypes {
    protected PAssertedServiceParser(Lexer lexer) {
        super(lexer);
    }

    public PAssertedServiceParser(String str) {
        super(str);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        if (debug) {
            dbg_enter("PAssertedServiceParser.parse");
        }
        try {
            this.lexer.match(TokenTypes.P_ASSERTED_SERVICE);
            this.lexer.SPorHT();
            this.lexer.match(58);
            this.lexer.SPorHT();
            PAssertedService pAssertedService = new PAssertedService();
            String buffer = this.lexer.getBuffer();
            if (buffer.contains(ParameterNamesIms.SERVICE_ID)) {
                if (buffer.contains(ParameterNamesIms.SERVICE_ID_LABEL)) {
                    if (buffer.split("3gpp-service.")[1].trim().equals("")) {
                        try {
                            throw new InvalidArgumentException("URN should atleast have one sub-service");
                        } catch (InvalidArgumentException e) {
                            e.printStackTrace();
                            super.parse();
                            return pAssertedService;
                        }
                    }
                    pAssertedService.setSubserviceIdentifiers(buffer.split(ParameterNamesIms.SERVICE_ID_LABEL)[1]);
                } else if (buffer.contains(ParameterNamesIms.APPLICATION_ID_LABEL)) {
                    if (buffer.split("3gpp-application.")[1].trim().equals("")) {
                        try {
                            throw new InvalidArgumentException("URN should atleast have one sub-application");
                        } catch (InvalidArgumentException e2) {
                            e2.printStackTrace();
                            super.parse();
                            return pAssertedService;
                        }
                    }
                    pAssertedService.setApplicationIdentifiers(buffer.split(ParameterNamesIms.APPLICATION_ID_LABEL)[1]);
                } else {
                    try {
                        throw new InvalidArgumentException("URN is not well formed");
                    } catch (InvalidArgumentException e3) {
                        e3.printStackTrace();
                        super.parse();
                        return pAssertedService;
                    }
                }
            }
            super.parse();
            return pAssertedService;
        } finally {
            if (debug) {
                dbg_enter("PAssertedServiceParser.parse");
            }
        }
    }
}
