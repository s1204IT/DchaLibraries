package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.MimeVersion;
import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;
import javax.sip.InvalidArgumentException;

public class MimeVersionParser extends HeaderParser {
    public MimeVersionParser(String mimeVersion) {
        super(mimeVersion);
    }

    protected MimeVersionParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        if (debug) {
            dbg_enter("MimeVersionParser.parse");
        }
        MimeVersion mimeVersion = new MimeVersion();
        try {
            headerName(TokenTypes.MIME_VERSION);
            mimeVersion.setHeaderName("MIME-Version");
            try {
                String majorVersion = this.lexer.number();
                mimeVersion.setMajorVersion(Integer.parseInt(majorVersion));
                this.lexer.match(46);
                String minorVersion = this.lexer.number();
                mimeVersion.setMinorVersion(Integer.parseInt(minorVersion));
                this.lexer.SPorHT();
                this.lexer.match(10);
                return mimeVersion;
            } catch (InvalidArgumentException ex) {
                throw createParseException(ex.getMessage());
            }
        } finally {
            if (debug) {
                dbg_leave("MimeVersionParser.parse");
            }
        }
    }
}
