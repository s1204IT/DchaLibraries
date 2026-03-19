package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.AddressParametersHeader;
import gov.nist.javax.sip.header.Contact;
import gov.nist.javax.sip.header.ContactList;
import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;

public class ContactParser extends AddressParametersParser {
    public ContactParser(String str) {
        super(str);
    }

    protected ContactParser(Lexer lexer) {
        super(lexer);
        this.lexer = lexer;
    }

    @Override
    public SIPHeader parse() throws ParseException {
        char cLookAhead;
        headerName(TokenTypes.CONTACT);
        ContactList contactList = new ContactList();
        while (true) {
            Contact contact = new Contact();
            if (this.lexer.lookAhead(0) == '*') {
                char cLookAhead2 = this.lexer.lookAhead(1);
                if (cLookAhead2 == ' ' || cLookAhead2 == '\t' || cLookAhead2 == '\r' || cLookAhead2 == '\n') {
                    this.lexer.match(42);
                    contact.setWildCardFlag(true);
                } else {
                    super.parse((AddressParametersHeader) contact);
                }
            } else {
                super.parse((AddressParametersHeader) contact);
            }
            contactList.add(contact);
            this.lexer.SPorHT();
            cLookAhead = this.lexer.lookAhead(0);
            if (cLookAhead != ',') {
                break;
            }
            this.lexer.match(44);
            this.lexer.SPorHT();
        }
        if (cLookAhead != '\n' && cLookAhead != 0) {
            throw createParseException("unexpected char");
        }
        return contactList;
    }
}
