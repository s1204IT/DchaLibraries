package gov.nist.javax.sip.parser.ims;

import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.ims.PChargingFunctionAddresses;
import gov.nist.javax.sip.parser.Lexer;
import gov.nist.javax.sip.parser.ParametersParser;
import gov.nist.javax.sip.parser.TokenTypes;
import java.text.ParseException;

public class PChargingFunctionAddressesParser extends ParametersParser implements TokenTypes {
    public PChargingFunctionAddressesParser(String str) {
        super(str);
    }

    protected PChargingFunctionAddressesParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        if (debug) {
            dbg_enter("parse");
        }
        try {
            headerName(TokenTypes.P_CHARGING_FUNCTION_ADDRESSES);
            PChargingFunctionAddresses pChargingFunctionAddresses = new PChargingFunctionAddresses();
            while (this.lexer.lookAhead(0) != '\n') {
                try {
                    parseParameter(pChargingFunctionAddresses);
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
            super.parse(pChargingFunctionAddresses);
            return pChargingFunctionAddresses;
        } finally {
            if (debug) {
                dbg_leave("parse");
            }
        }
    }

    protected void parseParameter(PChargingFunctionAddresses pChargingFunctionAddresses) throws ParseException {
        if (debug) {
            dbg_enter("parseParameter");
        }
        try {
            pChargingFunctionAddresses.setMultiParameter(nameValue('='));
        } finally {
            if (debug) {
                dbg_leave("parseParameter");
            }
        }
    }

    public static void main(String[] strArr) throws ParseException {
        String[] strArr2 = {"P-Charging-Function-Addresses: ccf=\"test str\"; ecf=token\n", "P-Charging-Function-Addresses: ccf=192.1.1.1; ccf=192.1.1.2; ecf=192.1.1.3; ecf=192.1.1.4\n", "P-Charging-Function-Addresses: ccf=[5555::b99:c88:d77:e66]; ccf=[5555::a55:b44:c33:d22]; ecf=[5555::1ff:2ee:3dd:4cc]; ecf=[5555::6aa:7bb:8cc:9dd]\n"};
        for (int i = 0; i < strArr2.length; i++) {
            PChargingFunctionAddressesParser pChargingFunctionAddressesParser = new PChargingFunctionAddressesParser(strArr2[i]);
            System.out.println("original = " + strArr2[i]);
            PChargingFunctionAddresses pChargingFunctionAddresses = (PChargingFunctionAddresses) pChargingFunctionAddressesParser.parse();
            System.out.println("encoded = " + pChargingFunctionAddresses.encode());
        }
    }
}
