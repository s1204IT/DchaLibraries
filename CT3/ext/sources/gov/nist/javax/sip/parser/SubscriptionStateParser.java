package gov.nist.javax.sip.parser;

import gov.nist.core.Token;
import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.SubscriptionState;
import java.text.ParseException;
import javax.sip.InvalidArgumentException;

public class SubscriptionStateParser extends HeaderParser {
    public SubscriptionStateParser(String subscriptionState) {
        super(subscriptionState);
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
            Token token = this.lexer.getNextToken();
            subscriptionState.setState(token.getTokenValue());
            while (this.lexer.lookAhead(0) == ';') {
                this.lexer.match(59);
                this.lexer.SPorHT();
                this.lexer.match(4095);
                Token token2 = this.lexer.getNextToken();
                String value = token2.getTokenValue();
                if (value.equalsIgnoreCase("reason")) {
                    this.lexer.match(61);
                    this.lexer.SPorHT();
                    this.lexer.match(4095);
                    Token token3 = this.lexer.getNextToken();
                    subscriptionState.setReasonCode(token3.getTokenValue());
                } else if (value.equalsIgnoreCase("expires")) {
                    this.lexer.match(61);
                    this.lexer.SPorHT();
                    this.lexer.match(4095);
                    Token token4 = this.lexer.getNextToken();
                    try {
                        int expires = Integer.parseInt(token4.getTokenValue());
                        subscriptionState.setExpires(expires);
                    } catch (NumberFormatException ex) {
                        throw createParseException(ex.getMessage());
                    } catch (InvalidArgumentException ex2) {
                        throw createParseException(ex2.getMessage());
                    }
                } else if (value.equalsIgnoreCase("retry-after")) {
                    this.lexer.match(61);
                    this.lexer.SPorHT();
                    this.lexer.match(4095);
                    Token token5 = this.lexer.getNextToken();
                    try {
                        int retryAfter = Integer.parseInt(token5.getTokenValue());
                        subscriptionState.setRetryAfter(retryAfter);
                    } catch (NumberFormatException ex3) {
                        throw createParseException(ex3.getMessage());
                    } catch (InvalidArgumentException ex4) {
                        throw createParseException(ex4.getMessage());
                    }
                } else {
                    this.lexer.match(61);
                    this.lexer.SPorHT();
                    this.lexer.match(4095);
                    Token secondToken = this.lexer.getNextToken();
                    String secondValue = secondToken.getTokenValue();
                    subscriptionState.setParameter(value, secondValue);
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
