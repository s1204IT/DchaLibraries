package gov.nist.javax.sip.clientauthutils;

import gov.nist.javax.sip.SipStackImpl;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.stack.SIPClientTransaction;
import java.text.ParseException;
import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Timer;
import javax.sip.ClientTransaction;
import javax.sip.DialogState;
import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import javax.sip.SipProvider;
import javax.sip.address.Hop;
import javax.sip.address.SipURI;
import javax.sip.address.URI;
import javax.sip.header.AuthorizationHeader;
import javax.sip.header.CSeqHeader;
import javax.sip.header.Header;
import javax.sip.header.HeaderFactory;
import javax.sip.header.ProxyAuthenticateHeader;
import javax.sip.header.ViaHeader;
import javax.sip.header.WWWAuthenticateHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

public class AuthenticationHelperImpl implements AuthenticationHelper {
    private Object accountManager;
    private CredentialsCache cachedCredentials;
    private HeaderFactory headerFactory;
    private SipStackImpl sipStack;
    Timer timer;

    public AuthenticationHelperImpl(SipStackImpl sipStackImpl, AccountManager accountManager, HeaderFactory headerFactory) {
        this.accountManager = null;
        this.accountManager = accountManager;
        this.headerFactory = headerFactory;
        this.sipStack = sipStackImpl;
        this.cachedCredentials = new CredentialsCache(sipStackImpl.getTimer());
    }

    public AuthenticationHelperImpl(SipStackImpl sipStackImpl, SecureAccountManager secureAccountManager, HeaderFactory headerFactory) {
        this.accountManager = null;
        this.accountManager = secureAccountManager;
        this.headerFactory = headerFactory;
        this.sipStack = sipStackImpl;
        this.cachedCredentials = new CredentialsCache(sipStackImpl.getTimer());
    }

    @Override
    public ClientTransaction handleChallenge(Response response, ClientTransaction clientTransaction, SipProvider sipProvider, int i) throws SipException, NullPointerException {
        Request requestCreateRequest;
        ListIterator headers;
        String sipDomain;
        AuthorizationHeader authorization;
        try {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("handleChallenge: " + response);
            }
            SIPRequest sIPRequest = (SIPRequest) clientTransaction.getRequest();
            if (sIPRequest.getToTag() != null || clientTransaction.getDialog() == null || clientTransaction.getDialog().getState() != DialogState.CONFIRMED) {
                requestCreateRequest = (Request) sIPRequest.clone();
            } else {
                requestCreateRequest = clientTransaction.getDialog().createRequest(sIPRequest.getMethod());
                ListIterator<String> headerNames = sIPRequest.getHeaderNames();
                while (headerNames.hasNext()) {
                    String next = headerNames.next();
                    if (requestCreateRequest.getHeader(next) != null) {
                        ListIterator headers2 = requestCreateRequest.getHeaders(next);
                        while (headers2.hasNext()) {
                            requestCreateRequest.addHeader((Header) headers2.next());
                        }
                    }
                }
            }
            removeBranchID(requestCreateRequest);
            if (response == null || requestCreateRequest == null) {
                throw new NullPointerException("A null argument was passed to handle challenge.");
            }
            if (response.getStatusCode() == 401) {
                headers = response.getHeaders("WWW-Authenticate");
            } else if (response.getStatusCode() == 407) {
                headers = response.getHeaders("Proxy-Authenticate");
            } else {
                throw new IllegalArgumentException("Unexpected status code ");
            }
            if (headers == null) {
                throw new IllegalArgumentException("Could not find WWWAuthenticate or ProxyAuthenticate headers");
            }
            requestCreateRequest.removeHeader("Authorization");
            requestCreateRequest.removeHeader("Proxy-Authorization");
            CSeqHeader cSeqHeader = (CSeqHeader) requestCreateRequest.getHeader("CSeq");
            try {
                cSeqHeader.setSeqNumber(cSeqHeader.getSeqNumber() + 1);
                if (sIPRequest.getRouteHeaders() == null) {
                    Hop nextHop = ((SIPClientTransaction) clientTransaction).getNextHop();
                    SipURI sipURI = (SipURI) requestCreateRequest.getRequestURI();
                    if (!nextHop.getHost().equalsIgnoreCase(sipURI.getHost()) && !nextHop.equals(this.sipStack.getRouter(sIPRequest).getOutboundProxy())) {
                        sipURI.setMAddrParam(nextHop.getHost());
                    }
                    if (nextHop.getPort() != -1) {
                        sipURI.setPort(nextHop.getPort());
                    }
                }
                ClientTransaction newClientTransaction = sipProvider.getNewClientTransaction(requestCreateRequest);
                while (headers.hasNext()) {
                    WWWAuthenticateHeader wWWAuthenticateHeader = (WWWAuthenticateHeader) headers.next();
                    String realm = wWWAuthenticateHeader.getRealm();
                    if (this.accountManager instanceof SecureAccountManager) {
                        UserCredentialHash credentialHash = ((SecureAccountManager) this.accountManager).getCredentialHash(clientTransaction, realm);
                        URI requestURI = requestCreateRequest.getRequestURI();
                        sipDomain = credentialHash.getSipDomain();
                        authorization = getAuthorization(requestCreateRequest.getMethod(), requestURI.toString(), requestCreateRequest.getContent() == null ? "" : new String(requestCreateRequest.getRawContent()), wWWAuthenticateHeader, credentialHash);
                    } else {
                        UserCredentials credentials = ((AccountManager) this.accountManager).getCredentials(clientTransaction, realm);
                        sipDomain = credentials.getSipDomain();
                        if (credentials == null) {
                            throw new SipException("Cannot find user creds for the given user name and realm");
                        }
                        authorization = getAuthorization(requestCreateRequest.getMethod(), requestCreateRequest.getRequestURI().toString(), requestCreateRequest.getContent() == null ? "" : new String(requestCreateRequest.getRawContent()), wWWAuthenticateHeader, credentials);
                    }
                    if (this.sipStack.isLoggingEnabled()) {
                        this.sipStack.getStackLogger().logDebug("Created authorization header: " + authorization.toString());
                    }
                    if (i != 0) {
                        this.cachedCredentials.cacheAuthorizationHeader(sipDomain, authorization, i);
                    }
                    requestCreateRequest.addHeader(authorization);
                }
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("Returning authorization transaction." + newClientTransaction);
                }
                return newClientTransaction;
            } catch (InvalidArgumentException e) {
                throw new SipException("Invalid CSeq -- could not increment : " + cSeqHeader.getSeqNumber());
            }
        } catch (SipException e2) {
            throw e2;
        } catch (Exception e3) {
            this.sipStack.getStackLogger().logError("Unexpected exception ", e3);
            throw new SipException("Unexpected exception ", e3);
        }
    }

    private AuthorizationHeader getAuthorization(String str, String str2, String str3, WWWAuthenticateHeader wWWAuthenticateHeader, UserCredentials userCredentials) {
        AuthorizationHeader authorizationHeaderCreateAuthorizationHeader;
        String str4 = wWWAuthenticateHeader.getQop() != null ? "auth" : null;
        String strCalculateResponse = MessageDigestAlgorithm.calculateResponse(wWWAuthenticateHeader.getAlgorithm(), userCredentials.getUserName(), wWWAuthenticateHeader.getRealm(), userCredentials.getPassword(), wWWAuthenticateHeader.getNonce(), "00000001", "xyz", str, str2, str3, str4, this.sipStack.getStackLogger());
        try {
            if (wWWAuthenticateHeader instanceof ProxyAuthenticateHeader) {
                authorizationHeaderCreateAuthorizationHeader = this.headerFactory.createProxyAuthorizationHeader(wWWAuthenticateHeader.getScheme());
            } else {
                authorizationHeaderCreateAuthorizationHeader = this.headerFactory.createAuthorizationHeader(wWWAuthenticateHeader.getScheme());
            }
            authorizationHeaderCreateAuthorizationHeader.setUsername(userCredentials.getUserName());
            authorizationHeaderCreateAuthorizationHeader.setRealm(wWWAuthenticateHeader.getRealm());
            authorizationHeaderCreateAuthorizationHeader.setNonce(wWWAuthenticateHeader.getNonce());
            authorizationHeaderCreateAuthorizationHeader.setParameter("uri", str2);
            authorizationHeaderCreateAuthorizationHeader.setResponse(strCalculateResponse);
            if (wWWAuthenticateHeader.getAlgorithm() != null) {
                authorizationHeaderCreateAuthorizationHeader.setAlgorithm(wWWAuthenticateHeader.getAlgorithm());
            }
            if (wWWAuthenticateHeader.getOpaque() != null) {
                authorizationHeaderCreateAuthorizationHeader.setOpaque(wWWAuthenticateHeader.getOpaque());
            }
            if (str4 != null) {
                authorizationHeaderCreateAuthorizationHeader.setQop(str4);
                authorizationHeaderCreateAuthorizationHeader.setCNonce("xyz");
                authorizationHeaderCreateAuthorizationHeader.setNonceCount(Integer.parseInt("00000001"));
            }
            authorizationHeaderCreateAuthorizationHeader.setResponse(strCalculateResponse);
            return authorizationHeaderCreateAuthorizationHeader;
        } catch (ParseException e) {
            throw new RuntimeException("Failed to create an authorization header!");
        }
    }

    private AuthorizationHeader getAuthorization(String str, String str2, String str3, WWWAuthenticateHeader wWWAuthenticateHeader, UserCredentialHash userCredentialHash) {
        AuthorizationHeader authorizationHeaderCreateAuthorizationHeader;
        String str4 = wWWAuthenticateHeader.getQop() != null ? "auth" : null;
        String strCalculateResponse = MessageDigestAlgorithm.calculateResponse(wWWAuthenticateHeader.getAlgorithm(), userCredentialHash.getHashUserDomainPassword(), wWWAuthenticateHeader.getNonce(), "00000001", "xyz", str, str2, str3, str4, this.sipStack.getStackLogger());
        try {
            if (wWWAuthenticateHeader instanceof ProxyAuthenticateHeader) {
                authorizationHeaderCreateAuthorizationHeader = this.headerFactory.createProxyAuthorizationHeader(wWWAuthenticateHeader.getScheme());
            } else {
                authorizationHeaderCreateAuthorizationHeader = this.headerFactory.createAuthorizationHeader(wWWAuthenticateHeader.getScheme());
            }
            authorizationHeaderCreateAuthorizationHeader.setUsername(userCredentialHash.getUserName());
            authorizationHeaderCreateAuthorizationHeader.setRealm(wWWAuthenticateHeader.getRealm());
            authorizationHeaderCreateAuthorizationHeader.setNonce(wWWAuthenticateHeader.getNonce());
            authorizationHeaderCreateAuthorizationHeader.setParameter("uri", str2);
            authorizationHeaderCreateAuthorizationHeader.setResponse(strCalculateResponse);
            if (wWWAuthenticateHeader.getAlgorithm() != null) {
                authorizationHeaderCreateAuthorizationHeader.setAlgorithm(wWWAuthenticateHeader.getAlgorithm());
            }
            if (wWWAuthenticateHeader.getOpaque() != null) {
                authorizationHeaderCreateAuthorizationHeader.setOpaque(wWWAuthenticateHeader.getOpaque());
            }
            if (str4 != null) {
                authorizationHeaderCreateAuthorizationHeader.setQop(str4);
                authorizationHeaderCreateAuthorizationHeader.setCNonce("xyz");
                authorizationHeaderCreateAuthorizationHeader.setNonceCount(Integer.parseInt("00000001"));
            }
            authorizationHeaderCreateAuthorizationHeader.setResponse(strCalculateResponse);
            return authorizationHeaderCreateAuthorizationHeader;
        } catch (ParseException e) {
            throw new RuntimeException("Failed to create an authorization header!");
        }
    }

    private void removeBranchID(Request request) {
        ((ViaHeader) request.getHeader("Via")).removeParameter("branch");
    }

    @Override
    public void setAuthenticationHeaders(Request request) {
        String callId = ((SIPRequest) request).getCallId().getCallId();
        request.removeHeader("Authorization");
        Collection<AuthorizationHeader> cachedAuthorizationHeaders = this.cachedCredentials.getCachedAuthorizationHeaders(callId);
        if (cachedAuthorizationHeaders == null) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("Could not find authentication headers for " + callId);
                return;
            }
            return;
        }
        Iterator<AuthorizationHeader> it = cachedAuthorizationHeaders.iterator();
        while (it.hasNext()) {
            request.addHeader(it.next());
        }
    }

    @Override
    public void removeCachedAuthenticationHeaders(String str) {
        if (str == null) {
            throw new NullPointerException("Null callId argument ");
        }
        this.cachedCredentials.removeAuthenticationHeader(str);
    }
}
