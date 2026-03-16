package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.Server;
import java.text.ParseException;

public class ServerParser extends HeaderParser {
    public ServerParser(String server) {
        super(server);
    }

    protected ServerParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
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
                    String comment = this.lexer.comment();
                    server.addProductToken('(' + comment + ')');
                } else {
                    int marker = 0;
                    try {
                        marker = this.lexer.markInputPosition();
                        String tok = this.lexer.getString('/');
                        if (tok.charAt(tok.length() - 1) == '\n') {
                            tok = tok.trim();
                        }
                        server.addProductToken(tok);
                    } catch (ParseException e) {
                        this.lexer.rewindInputPosition(marker);
                        server.addProductToken(this.lexer.getRest().trim());
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
