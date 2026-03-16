package org.apache.http.impl.auth;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.apache.http.HeaderElement;
import org.apache.http.auth.MalformedChallengeException;
import org.apache.http.message.BasicHeaderValueParser;
import org.apache.http.message.HeaderValueParser;
import org.apache.http.message.ParserCursor;
import org.apache.http.util.CharArrayBuffer;

@Deprecated
public abstract class RFC2617Scheme extends AuthSchemeBase {
    private Map<String, String> params;

    @Override
    protected void parseChallenge(CharArrayBuffer buffer, int pos, int len) throws MalformedChallengeException {
        HeaderValueParser parser = BasicHeaderValueParser.DEFAULT;
        ParserCursor cursor = new ParserCursor(pos, buffer.length());
        HeaderElement[] elements = parser.parseElements(buffer, cursor);
        if (elements.length == 0) {
            throw new MalformedChallengeException("Authentication challenge is empty");
        }
        this.params = new HashMap(elements.length);
        for (HeaderElement element : elements) {
            this.params.put(element.getName(), element.getValue());
        }
    }

    protected Map<String, String> getParameters() {
        if (this.params == null) {
            this.params = new HashMap();
        }
        return this.params;
    }

    @Override
    public String getParameter(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Parameter name may not be null");
        }
        if (this.params == null) {
            return null;
        }
        return this.params.get(name.toLowerCase(Locale.ENGLISH));
    }

    @Override
    public String getRealm() {
        return getParameter("realm");
    }
}
