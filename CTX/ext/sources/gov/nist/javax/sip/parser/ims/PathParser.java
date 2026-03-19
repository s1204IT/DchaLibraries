package gov.nist.javax.sip.parser.ims;

import gov.nist.javax.sip.header.AddressParametersHeader;
import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.ims.Path;
import gov.nist.javax.sip.header.ims.PathList;
import gov.nist.javax.sip.parser.AddressParametersParser;
import gov.nist.javax.sip.parser.Lexer;
import gov.nist.javax.sip.parser.TokenTypes;
import java.text.ParseException;

public class PathParser extends AddressParametersParser implements TokenTypes {
    public PathParser(String str) {
        super(str);
    }

    protected PathParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        char cLookAhead;
        PathList pathList = new PathList();
        if (debug) {
            dbg_enter("PathParser.parse");
        }
        try {
            this.lexer.match(TokenTypes.PATH);
            this.lexer.SPorHT();
            this.lexer.match(58);
            this.lexer.SPorHT();
            while (true) {
                Path path = new Path();
                super.parse((AddressParametersHeader) path);
                pathList.add(path);
                this.lexer.SPorHT();
                cLookAhead = this.lexer.lookAhead(0);
                if (cLookAhead != ',') {
                    break;
                }
                this.lexer.match(44);
                this.lexer.SPorHT();
            }
            if (cLookAhead != '\n') {
                throw createParseException("unexpected char");
            }
            return pathList;
        } finally {
            if (debug) {
                dbg_leave("PathParser.parse");
            }
        }
    }
}
