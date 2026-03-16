package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.address.GenericURI;
import gov.nist.javax.sip.header.AlertInfo;
import gov.nist.javax.sip.header.AlertInfoList;
import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;

public class AlertInfoParser extends ParametersParser {
    public AlertInfoParser(String alertInfo) {
        super(alertInfo);
    }

    protected AlertInfoParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        if (debug) {
            dbg_enter("AlertInfoParser.parse");
        }
        AlertInfoList list = new AlertInfoList();
        try {
            headerName(TokenTypes.ALERT_INFO);
            while (this.lexer.lookAhead(0) != '\n') {
                AlertInfo alertInfo = new AlertInfo();
                alertInfo.setHeaderName("Alert-Info");
                while (true) {
                    this.lexer.SPorHT();
                    if (this.lexer.lookAhead(0) == '<') {
                        this.lexer.match(60);
                        URLParser urlParser = new URLParser((Lexer) this.lexer);
                        GenericURI uri = urlParser.uriReference(true);
                        alertInfo.setAlertInfo(uri);
                        this.lexer.match(62);
                    } else {
                        String alertInfoStr = this.lexer.byteStringNoSemicolon();
                        alertInfo.setAlertInfo(alertInfoStr);
                    }
                    this.lexer.SPorHT();
                    super.parse(alertInfo);
                    list.add(alertInfo);
                    if (this.lexer.lookAhead(0) == ',') {
                        this.lexer.match(44);
                    }
                }
            }
            return list;
        } finally {
            if (debug) {
                dbg_leave("AlertInfoParser.parse");
            }
        }
    }
}
