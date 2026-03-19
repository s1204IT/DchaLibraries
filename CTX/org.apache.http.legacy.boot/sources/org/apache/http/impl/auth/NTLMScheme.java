package org.apache.http.impl.auth;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.auth.AUTH;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.InvalidCredentialsException;
import org.apache.http.auth.MalformedChallengeException;
import org.apache.http.auth.NTCredentials;
import org.apache.http.message.BufferedHeader;
import org.apache.http.util.CharArrayBuffer;

@Deprecated
public class NTLMScheme extends AuthSchemeBase {
    private String challenge;
    private final NTLMEngine engine;
    private State state;

    enum State {
        UNINITIATED,
        CHALLENGE_RECEIVED,
        MSG_TYPE1_GENERATED,
        MSG_TYPE2_RECEVIED,
        MSG_TYPE3_GENERATED,
        FAILED
    }

    public NTLMScheme(NTLMEngine nTLMEngine) {
        if (nTLMEngine == null) {
            throw new IllegalArgumentException("NTLM engine may not be null");
        }
        this.engine = nTLMEngine;
        this.state = State.UNINITIATED;
        this.challenge = null;
    }

    @Override
    public String getSchemeName() {
        return "ntlm";
    }

    @Override
    public String getParameter(String str) {
        return null;
    }

    @Override
    public String getRealm() {
        return null;
    }

    @Override
    public boolean isConnectionBased() {
        return true;
    }

    @Override
    protected void parseChallenge(CharArrayBuffer charArrayBuffer, int i, int i2) throws MalformedChallengeException {
        String strSubstringTrimmed = charArrayBuffer.substringTrimmed(i, i2);
        if (strSubstringTrimmed.length() == 0) {
            if (this.state == State.UNINITIATED) {
                this.state = State.CHALLENGE_RECEIVED;
            } else {
                this.state = State.FAILED;
            }
            this.challenge = null;
            return;
        }
        this.state = State.MSG_TYPE2_RECEVIED;
        this.challenge = strSubstringTrimmed;
    }

    @Override
    public Header authenticate(Credentials credentials, HttpRequest httpRequest) throws AuthenticationException {
        String strGenerateType1Msg;
        try {
            NTCredentials nTCredentials = (NTCredentials) credentials;
            if (this.state == State.CHALLENGE_RECEIVED || this.state == State.FAILED) {
                strGenerateType1Msg = this.engine.generateType1Msg(nTCredentials.getDomain(), nTCredentials.getWorkstation());
                this.state = State.MSG_TYPE1_GENERATED;
            } else if (this.state == State.MSG_TYPE2_RECEVIED) {
                strGenerateType1Msg = this.engine.generateType3Msg(nTCredentials.getUserName(), nTCredentials.getPassword(), nTCredentials.getDomain(), nTCredentials.getWorkstation(), this.challenge);
                this.state = State.MSG_TYPE3_GENERATED;
            } else {
                throw new AuthenticationException("Unexpected state: " + this.state);
            }
            CharArrayBuffer charArrayBuffer = new CharArrayBuffer(32);
            if (isProxy()) {
                charArrayBuffer.append(AUTH.PROXY_AUTH_RESP);
            } else {
                charArrayBuffer.append(AUTH.WWW_AUTH_RESP);
            }
            charArrayBuffer.append(": NTLM ");
            charArrayBuffer.append(strGenerateType1Msg);
            return new BufferedHeader(charArrayBuffer);
        } catch (ClassCastException e) {
            throw new InvalidCredentialsException("Credentials cannot be used for NTLM authentication: " + credentials.getClass().getName());
        }
    }

    @Override
    public boolean isComplete() {
        return this.state == State.MSG_TYPE3_GENERATED || this.state == State.FAILED;
    }
}
