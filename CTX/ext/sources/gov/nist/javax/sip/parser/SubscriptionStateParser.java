package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.SubscriptionState;
import java.text.ParseException;
import javax.sip.InvalidArgumentException;

public class SubscriptionStateParser extends HeaderParser {
    public SubscriptionStateParser(String str) {
        super(str);
    }

    protected SubscriptionStateParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        if (debug) {
            dbg_enter("SubscriptionStateParser.parse");
        }
        SubscriptionState subscriptionState = new SubscriptionState();
        try {
            headerName(TokenTypes.SUBSCRIPTION_STATE);
            subscriptionState.setHeaderName("Subscription-State");
            this.lexer.match(4095);
            subscriptionState.setState(this.lexer.getNextToken().getTokenValue());
            while (this.lexer.lookAhead(0) == ';') {
                this.lexer.match(59);
                this.lexer.SPorHT();
                this.lexer.match(4095);
                String tokenValue = this.lexer.getNextToken().getTokenValue();
                if (tokenValue.equalsIgnoreCase("reason")) {
                    this.lexer.match(61);
                    this.lexer.SPorHT();
                    this.lexer.match(4095);
                    subscriptionState.setReasonCode(this.lexer.getNextToken().getTokenValue());
                } else if (tokenValue.equalsIgnoreCase("expires")) {
                    this.lexer.match(61);
                    this.lexer.SPorHT();
                    this.lexer.match(4095);
                    try {
                        subscriptionState.setExpires(Integer.parseInt(this.lexer.getNextToken().getTokenValue()));
                    } catch (NumberFormatException e) {
                        throw createParseException(e.getMessage());
                    } catch (InvalidArgumentException e2) {
                        throw createParseException(e2.getMessage());
                    }
                } else if (tokenValue.equalsIgnoreCase("retry-after")) {
                    this.lexer.match(61);
                    this.lexer.SPorHT();
                    this.lexer.match(4095);
                    try {
                        subscriptionState.setRetryAfter(Integer.parseInt(this.lexer.getNextToken().getTokenValue()));
                    } catch (NumberFormatException e3) {
                        throw createParseException(e3.getMessage());
                    } catch (InvalidArgumentException e4) {
                        throw createParseException(e4.getMessage());
                    }
                } else {
                    this.lexer.match(61);
                    this.lexer.SPorHT();
                    this.lexer.match(4095);
                    subscriptionState.setParameter(tokenValue, this.lexer.getNextToken().getTokenValue());
                }
                this.lexer.SPorHT();
            }
            return subscriptionState;
        } finally {
            if (debug) {
                dbg_leave("SubscriptionStateParser.parse");
            }
        }
    }
}
