package gov.nist.javax.sip.parser;

import gov.nist.core.Separators;
import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.UserAgent;
import java.text.ParseException;

public class UserAgentParser extends HeaderParser {
    public UserAgentParser(String str) {
        super(str);
    }

    protected UserAgentParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        if (debug) {
            dbg_enter("UserAgentParser.parse");
        }
        UserAgent userAgent = new UserAgent();
        try {
            headerName(TokenTypes.USER_AGENT);
            if (this.lexer.lookAhead(0) == '\n') {
                throw createParseException("empty header");
            }
            while (this.lexer.lookAhead(0) != '\n' && this.lexer.lookAhead(0) != 0) {
                if (this.lexer.lookAhead(0) == '(') {
                    userAgent.addProductToken('(' + this.lexer.comment() + ')');
                } else {
                    getLexer().SPorHT();
                    String strByteStringNoSlash = this.lexer.byteStringNoSlash();
                    if (strByteStringNoSlash == null) {
                        throw createParseException("Expected product string");
                    }
                    StringBuffer stringBuffer = new StringBuffer(strByteStringNoSlash);
                    if (this.lexer.peekNextToken().getTokenType() == 47) {
                        this.lexer.match(47);
                        getLexer().SPorHT();
                        String strByteStringNoSlash2 = this.lexer.byteStringNoSlash();
                        if (strByteStringNoSlash2 == null) {
                            throw createParseException("Expected product version");
                        }
                        stringBuffer.append(Separators.SLASH);
                        stringBuffer.append(strByteStringNoSlash2);
                    }
                    userAgent.addProductToken(stringBuffer.toString());
                }
                this.lexer.SPorHT();
            }
            return userAgent;
        } finally {
            if (debug) {
                dbg_leave("UserAgentParser.parse");
            }
        }
    }

    public static void main(String[] strArr) throws ParseException {
        for (String str : new String[]{"User-Agent: Softphone/Beta1.5 \n", "User-Agent:Nist/Beta1 (beta version) \n", "User-Agent: Nist UA (beta version)\n", "User-Agent: Nist1.0/Beta2 Ubi/vers.1.0 (very cool) \n", "User-Agent: SJphone/1.60.299a/L (SJ Labs)\n", "User-Agent: sipXecs/3.5.11 sipXecs/sipxbridge (Linux)\n"}) {
            System.out.println("encoded = " + ((UserAgent) new UserAgentParser(str).parse()).encode());
        }
    }
}
