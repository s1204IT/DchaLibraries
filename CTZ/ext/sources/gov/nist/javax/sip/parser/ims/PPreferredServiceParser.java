package gov.nist.javax.sip.parser.ims;

import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.ims.PPreferredService;
import gov.nist.javax.sip.header.ims.ParameterNamesIms;
import gov.nist.javax.sip.parser.HeaderParser;
import gov.nist.javax.sip.parser.Lexer;
import gov.nist.javax.sip.parser.TokenTypes;
import java.text.ParseException;
import javax.sip.InvalidArgumentException;

public class PPreferredServiceParser extends HeaderParser implements TokenTypes {
    protected PPreferredServiceParser(Lexer lexer) {
        super(lexer);
    }

    public PPreferredServiceParser(String str) {
        super(str);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        if (debug) {
            dbg_enter("PPreferredServiceParser.parse");
        }
        try {
            this.lexer.match(TokenTypes.P_PREFERRED_SERVICE);
            this.lexer.SPorHT();
            this.lexer.match(58);
            this.lexer.SPorHT();
            PPreferredService pPreferredService = new PPreferredService();
            String buffer = this.lexer.getBuffer();
            if (buffer.contains(ParameterNamesIms.SERVICE_ID)) {
                if (buffer.contains(ParameterNamesIms.SERVICE_ID_LABEL)) {
                    String str = buffer.split("3gpp-service.")[1];
                    if (str.trim().equals("")) {
                        try {
                            throw new InvalidArgumentException("URN should atleast have one sub-service");
                        } catch (InvalidArgumentException e) {
                            e.printStackTrace();
                        }
                    } else {
                        pPreferredService.setSubserviceIdentifiers(str);
                    }
                } else if (buffer.contains(ParameterNamesIms.APPLICATION_ID_LABEL)) {
                    String str2 = buffer.split(ParameterNamesIms.APPLICATION_ID_LABEL)[1];
                    if (str2.trim().equals("")) {
                        try {
                            throw new InvalidArgumentException("URN should atleast have one sub-application");
                        } catch (InvalidArgumentException e2) {
                            e2.printStackTrace();
                        }
                    } else {
                        pPreferredService.setApplicationIdentifiers(str2);
                    }
                } else {
                    try {
                        throw new InvalidArgumentException("URN is not well formed");
                    } catch (InvalidArgumentException e3) {
                        e3.printStackTrace();
                    }
                }
            }
            super.parse();
            return pPreferredService;
        } finally {
            if (debug) {
                dbg_enter("PPreferredServiceParser.parse");
            }
        }
    }
}
