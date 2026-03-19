package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.address.GenericURI;
import gov.nist.javax.sip.header.CallInfo;
import gov.nist.javax.sip.header.CallInfoList;
import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;

public class CallInfoParser extends ParametersParser {
    public CallInfoParser(String callInfo) {
        super(callInfo);
    }

    protected CallInfoParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        if (debug) {
            dbg_enter("CallInfoParser.parse");
        }
        CallInfoList list = new CallInfoList();
        try {
            headerName(TokenTypes.CALL_INFO);
            while (this.lexer.lookAhead(0) != '\n') {
                CallInfo callInfo = new CallInfo();
                callInfo.setHeaderName("Call-Info");
                this.lexer.SPorHT();
                this.lexer.match(60);
                URLParser urlParser = new URLParser((Lexer) this.lexer);
                GenericURI uri = urlParser.uriReference(true);
                callInfo.setInfo(uri);
                this.lexer.match(62);
                this.lexer.SPorHT();
                super.parse(callInfo);
                list.add(callInfo);
                while (this.lexer.lookAhead(0) == ',') {
                    this.lexer.match(44);
                    this.lexer.SPorHT();
                    CallInfo callInfo2 = new CallInfo();
                    this.lexer.SPorHT();
                    this.lexer.match(60);
                    URLParser urlParser2 = new URLParser((Lexer) this.lexer);
                    GenericURI uri2 = urlParser2.uriReference(true);
                    callInfo2.setInfo(uri2);
                    this.lexer.match(62);
                    this.lexer.SPorHT();
                    super.parse(callInfo2);
                    list.add(callInfo2);
                }
            }
            return list;
        } finally {
            if (debug) {
                dbg_leave("CallInfoParser.parse");
            }
        }
    }
}
