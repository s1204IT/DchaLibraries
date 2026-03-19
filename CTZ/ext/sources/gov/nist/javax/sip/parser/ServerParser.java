package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.Server;
import java.text.ParseException;

public class ServerParser extends HeaderParser {
    public ServerParser(String str) {
        super(str);
    }

    protected ServerParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        int iMarkInputPosition;
        if (debug) {
            dbg_enter("ServerParser.parse");
        }
        Server server = new Server();
        try {
            headerName(TokenTypes.SERVER);
            if (this.lexer.lookAhead(0) == '\n') {
                throw createParseException("empty header");
            }
            while (this.lexer.lookAhead(0) != '\n' && this.lexer.lookAhead(0) != 0) {
                if (this.lexer.lookAhead(0) == '(') {
                    server.addProductToken('(' + this.lexer.comment() + ')');
                } else {
                    try {
                        iMarkInputPosition = this.lexer.markInputPosition();
                        try {
                            String string = this.lexer.getString('/');
                            if (string.charAt(string.length() - 1) == '\n') {
                                string = string.trim();
                            }
                            server.addProductToken(string);
                        } catch (ParseException e) {
                            this.lexer.rewindInputPosition(iMarkInputPosition);
                            server.addProductToken(this.lexer.getRest().trim());
                            return server;
                        }
                    } catch (ParseException e2) {
                        iMarkInputPosition = 0;
                    }
                }
            }
            return server;
        } finally {
            if (debug) {
                dbg_leave("ServerParser.parse");
            }
        }
    }
}
