package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.CallInfo;
import gov.nist.javax.sip.header.CallInfoList;
import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;

public class CallInfoParser extends ParametersParser {
    public CallInfoParser(String str) {
        super(str);
    }

    protected CallInfoParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        if (debug) {
            dbg_enter("CallInfoParser.parse");
        }
        CallInfoList callInfoList = new CallInfoList();
        try {
            headerName(TokenTypes.CALL_INFO);
            while (this.lexer.lookAhead(0) != '\n') {
                CallInfo callInfo = new CallInfo();
                callInfo.setHeaderName("Call-Info");
                this.lexer.SPorHT();
                this.lexer.match(60);
                callInfo.setInfo(new URLParser((Lexer) this.lexer).uriReference(true));
                this.lexer.match(62);
                this.lexer.SPorHT();
                super.parse(callInfo);
                callInfoList.add(callInfo);
                while (this.lexer.lookAhead(0) == ',') {
                    this.lexer.match(44);
                    this.lexer.SPorHT();
                    CallInfo callInfo2 = new CallInfo();
                    this.lexer.SPorHT();
                    this.lexer.match(60);
                    callInfo2.setInfo(new URLParser((Lexer) this.lexer).uriReference(true));
                    this.lexer.match(62);
                    this.lexer.SPorHT();
                    super.parse(callInfo2);
                    callInfoList.add(callInfo2);
                }
            }
            return callInfoList;
        } finally {
            if (debug) {
                dbg_leave("CallInfoParser.parse");
            }
        }
    }
}
