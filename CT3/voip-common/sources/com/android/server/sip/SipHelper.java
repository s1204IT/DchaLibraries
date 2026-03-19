package com.android.server.sip;

import android.net.sip.SipProfile;
import android.telephony.Rlog;
import gov.nist.javax.sip.clientauthutils.AccountManager;
import gov.nist.javax.sip.clientauthutils.AuthenticationHelper;
import gov.nist.javax.sip.header.extensions.ReferredByHeader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.regex.Pattern;
import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.DialogTerminatedEvent;
import javax.sip.InvalidArgumentException;
import javax.sip.ListeningPoint;
import javax.sip.PeerUnavailableException;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.SipFactory;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.Transaction;
import javax.sip.TransactionState;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.Header;
import javax.sip.header.HeaderFactory;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.Message;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

class SipHelper {
    private static final boolean DBG = true;
    private static final boolean DBG_PING = false;
    private static final String TAG = SipHelper.class.getSimpleName();
    private AddressFactory mAddressFactory;
    private HeaderFactory mHeaderFactory;
    private MessageFactory mMessageFactory;
    private SipProvider mSipProvider;
    private SipStack mSipStack;

    public SipHelper(SipStack sipStack, SipProvider sipProvider) throws PeerUnavailableException {
        this.mSipStack = sipStack;
        this.mSipProvider = sipProvider;
        SipFactory sipFactory = SipFactory.getInstance();
        this.mAddressFactory = sipFactory.createAddressFactory();
        this.mHeaderFactory = sipFactory.createHeaderFactory();
        this.mMessageFactory = sipFactory.createMessageFactory();
    }

    private FromHeader createFromHeader(SipProfile profile, String tag) throws ParseException {
        return this.mHeaderFactory.createFromHeader(profile.getSipAddress(), tag);
    }

    private ToHeader createToHeader(SipProfile profile) throws ParseException {
        return createToHeader(profile, null);
    }

    private ToHeader createToHeader(SipProfile profile, String tag) throws ParseException {
        return this.mHeaderFactory.createToHeader(profile.getSipAddress(), tag);
    }

    private CallIdHeader createCallIdHeader() {
        return this.mSipProvider.getNewCallId();
    }

    private CSeqHeader createCSeqHeader(String method) throws InvalidArgumentException, ParseException {
        long sequence = (long) (Math.random() * 10000.0d);
        return this.mHeaderFactory.createCSeqHeader(sequence, method);
    }

    private MaxForwardsHeader createMaxForwardsHeader() throws InvalidArgumentException {
        return this.mHeaderFactory.createMaxForwardsHeader(70);
    }

    private MaxForwardsHeader createMaxForwardsHeader(int max) throws InvalidArgumentException {
        return this.mHeaderFactory.createMaxForwardsHeader(max);
    }

    private ListeningPoint getListeningPoint() throws SipException {
        ListeningPoint[] lps;
        ListeningPoint lp = this.mSipProvider.getListeningPoint("UDP");
        if (lp == null) {
            lp = this.mSipProvider.getListeningPoint("TCP");
        }
        if (lp == null && (lps = this.mSipProvider.getListeningPoints()) != null && lps.length > 0) {
            lp = lps[0];
        }
        if (lp == null) {
            throw new SipException("no listening point is available");
        }
        return lp;
    }

    private List<ViaHeader> createViaHeaders() throws SipException, ParseException {
        List<ViaHeader> viaHeaders = new ArrayList<>(1);
        ListeningPoint lp = getListeningPoint();
        ViaHeader viaHeader = this.mHeaderFactory.createViaHeader(lp.getIPAddress(), lp.getPort(), lp.getTransport(), (String) null);
        viaHeader.setRPort();
        viaHeaders.add(viaHeader);
        return viaHeaders;
    }

    private ContactHeader createContactHeader(SipProfile profile) throws SipException, ParseException {
        return createContactHeader(profile, null, 0);
    }

    private ContactHeader createContactHeader(SipProfile profile, String ip, int port) throws SipException, ParseException {
        SipURI contactURI;
        if (ip == null) {
            contactURI = createSipUri(profile.getUserName(), profile.getProtocol(), getListeningPoint());
        } else {
            contactURI = createSipUri(profile.getUserName(), profile.getProtocol(), ip, port);
        }
        Address contactAddress = this.mAddressFactory.createAddress(contactURI);
        contactAddress.setDisplayName(profile.getDisplayName());
        return this.mHeaderFactory.createContactHeader(contactAddress);
    }

    private ContactHeader createWildcardContactHeader() {
        ContactHeader contactHeader = this.mHeaderFactory.createContactHeader();
        contactHeader.setWildCard();
        return contactHeader;
    }

    private SipURI createSipUri(String username, String transport, ListeningPoint lp) throws ParseException {
        return createSipUri(username, transport, lp.getIPAddress(), lp.getPort());
    }

    private SipURI createSipUri(String username, String transport, String ip, int port) throws ParseException {
        SipURI uri = this.mAddressFactory.createSipURI(username, ip);
        try {
            uri.setPort(port);
            uri.setTransportParam(transport);
            return uri;
        } catch (InvalidArgumentException e) {
            throw new RuntimeException((Throwable) e);
        }
    }

    public ClientTransaction sendOptions(SipProfile caller, SipProfile callee, String tag) throws InvalidArgumentException, SipException {
        Request request;
        try {
            if (caller == callee) {
                request = createRequest("OPTIONS", caller, tag);
            } else {
                request = createRequest("OPTIONS", caller, callee, tag);
            }
            ClientTransaction clientTransaction = this.mSipProvider.getNewClientTransaction(request);
            clientTransaction.sendRequest();
            return clientTransaction;
        } catch (Exception e) {
            throw new SipException("sendOptions()", e);
        }
    }

    public ClientTransaction sendRegister(SipProfile userProfile, String tag, int expiry) throws InvalidArgumentException, SipException {
        try {
            Request request = createRequest("REGISTER", userProfile, tag);
            if (expiry == 0) {
                request.addHeader(createWildcardContactHeader());
            } else {
                request.addHeader(createContactHeader(userProfile));
            }
            request.addHeader(this.mHeaderFactory.createExpiresHeader(expiry));
            ClientTransaction clientTransaction = this.mSipProvider.getNewClientTransaction(request);
            clientTransaction.sendRequest();
            return clientTransaction;
        } catch (ParseException e) {
            throw new SipException("sendRegister()", e);
        }
    }

    private Request createRequest(String requestType, SipProfile userProfile, String tag) throws InvalidArgumentException, SipException, ParseException {
        FromHeader fromHeader = createFromHeader(userProfile, tag);
        ToHeader toHeader = createToHeader(userProfile);
        String replaceStr = Pattern.quote(userProfile.getUserName() + "@");
        SipURI requestURI = this.mAddressFactory.createSipURI(userProfile.getUriString().replaceFirst(replaceStr, ""));
        List<ViaHeader> viaHeaders = createViaHeaders();
        CallIdHeader callIdHeader = createCallIdHeader();
        CSeqHeader cSeqHeader = createCSeqHeader(requestType);
        MaxForwardsHeader maxForwards = createMaxForwardsHeader();
        Request request = this.mMessageFactory.createRequest(requestURI, requestType, callIdHeader, cSeqHeader, fromHeader, toHeader, viaHeaders, maxForwards);
        Header userAgentHeader = this.mHeaderFactory.createHeader("User-Agent", "SIPAUA/0.1.001");
        request.addHeader(userAgentHeader);
        return request;
    }

    public ClientTransaction handleChallenge(ResponseEvent responseEvent, AccountManager accountManager) throws SipException {
        AuthenticationHelper authenticationHelper = this.mSipStack.getAuthenticationHelper(accountManager, this.mHeaderFactory);
        ClientTransaction tid = responseEvent.getClientTransaction();
        ClientTransaction ct = authenticationHelper.handleChallenge(responseEvent.getResponse(), tid, this.mSipProvider, 5);
        log("send request with challenge response: " + ct.getRequest());
        ct.sendRequest();
        return ct;
    }

    private Request createRequest(String requestType, SipProfile caller, SipProfile callee, String tag) throws InvalidArgumentException, SipException, ParseException {
        FromHeader fromHeader = createFromHeader(caller, tag);
        ToHeader toHeader = createToHeader(callee);
        SipURI requestURI = callee.getUri();
        List<ViaHeader> viaHeaders = createViaHeaders();
        CallIdHeader callIdHeader = createCallIdHeader();
        CSeqHeader cSeqHeader = createCSeqHeader(requestType);
        MaxForwardsHeader maxForwards = createMaxForwardsHeader();
        Request request = this.mMessageFactory.createRequest(requestURI, requestType, callIdHeader, cSeqHeader, fromHeader, toHeader, viaHeaders, maxForwards);
        request.addHeader(createContactHeader(caller));
        return request;
    }

    public ClientTransaction sendInvite(SipProfile caller, SipProfile callee, String sessionDescription, String tag, ReferredByHeader referredBy, String replaces) throws InvalidArgumentException, SipException {
        try {
            Request request = createRequest("INVITE", caller, callee, tag);
            if (referredBy != null) {
                request.addHeader(referredBy);
            }
            if (replaces != null) {
                request.addHeader(this.mHeaderFactory.createHeader("Replaces", replaces));
            }
            request.setContent(sessionDescription, this.mHeaderFactory.createContentTypeHeader("application", "sdp"));
            ClientTransaction clientTransaction = this.mSipProvider.getNewClientTransaction(request);
            log("send INVITE: " + request);
            clientTransaction.sendRequest();
            return clientTransaction;
        } catch (ParseException e) {
            throw new SipException("sendInvite()", e);
        }
    }

    public ClientTransaction sendReinvite(Dialog dialog, String sessionDescription) throws SipException {
        try {
            Request request = dialog.createRequest("INVITE");
            request.setContent(sessionDescription, this.mHeaderFactory.createContentTypeHeader("application", "sdp"));
            ViaHeader viaHeader = request.getHeader("Via");
            if (viaHeader != null) {
                viaHeader.setRPort();
            }
            ClientTransaction clientTransaction = this.mSipProvider.getNewClientTransaction(request);
            log("send RE-INVITE: " + request);
            dialog.sendRequest(clientTransaction);
            return clientTransaction;
        } catch (ParseException e) {
            throw new SipException("sendReinvite()", e);
        }
    }

    public ServerTransaction getServerTransaction(RequestEvent event) throws SipException {
        ServerTransaction transaction = event.getServerTransaction();
        if (transaction == null) {
            Request request = event.getRequest();
            return this.mSipProvider.getNewServerTransaction(request);
        }
        return transaction;
    }

    public ServerTransaction sendRinging(RequestEvent event, String tag) throws SipException {
        try {
            Request request = event.getRequest();
            ServerTransaction transaction = getServerTransaction(event);
            Response response = this.mMessageFactory.createResponse(180, request);
            ToHeader toHeader = response.getHeader("To");
            toHeader.setTag(tag);
            response.addHeader(toHeader);
            log("send RINGING: " + response);
            transaction.sendResponse(response);
            return transaction;
        } catch (ParseException e) {
            throw new SipException("sendRinging()", e);
        }
    }

    public ServerTransaction sendInviteOk(RequestEvent event, SipProfile localProfile, String sessionDescription, ServerTransaction inviteTransaction, String externalIp, int externalPort) throws SipException {
        try {
            Request request = event.getRequest();
            Response response = this.mMessageFactory.createResponse(200, request);
            response.addHeader(createContactHeader(localProfile, externalIp, externalPort));
            response.setContent(sessionDescription, this.mHeaderFactory.createContentTypeHeader("application", "sdp"));
            if (inviteTransaction == null) {
                inviteTransaction = getServerTransaction(event);
            }
            if (inviteTransaction.getState() != TransactionState.COMPLETED) {
                log("send OK: " + response);
                inviteTransaction.sendResponse(response);
            }
            return inviteTransaction;
        } catch (ParseException e) {
            throw new SipException("sendInviteOk()", e);
        }
    }

    public void sendInviteBusyHere(RequestEvent event, ServerTransaction inviteTransaction) throws SipException {
        try {
            Request request = event.getRequest();
            Response response = this.mMessageFactory.createResponse(486, request);
            if (inviteTransaction == null) {
                inviteTransaction = getServerTransaction(event);
            }
            if (inviteTransaction.getState() == TransactionState.COMPLETED) {
                return;
            }
            log("send BUSY HERE: " + response);
            inviteTransaction.sendResponse(response);
        } catch (ParseException e) {
            throw new SipException("sendInviteBusyHere()", e);
        }
    }

    public void sendInviteAck(ResponseEvent event, Dialog dialog) throws SipException {
        Response response = event.getResponse();
        long cseq = response.getHeader("CSeq").getSeqNumber();
        Request ack = dialog.createAck(cseq);
        log("send ACK: " + ack);
        dialog.sendAck(ack);
    }

    public void sendBye(Dialog dialog) throws SipException {
        Request byeRequest = dialog.createRequest("BYE");
        log("send BYE: " + byeRequest);
        dialog.sendRequest(this.mSipProvider.getNewClientTransaction(byeRequest));
    }

    public void sendCancel(ClientTransaction inviteTransaction) throws SipException {
        Request cancelRequest = inviteTransaction.createCancel();
        log("send CANCEL: " + cancelRequest);
        this.mSipProvider.getNewClientTransaction(cancelRequest).sendRequest();
    }

    public void sendResponse(RequestEvent event, int responseCode) throws SipException {
        try {
            Request request = event.getRequest();
            Response response = this.mMessageFactory.createResponse(responseCode, request);
            if (!"OPTIONS".equals(request.getMethod())) {
                log("send response: " + response);
            }
            getServerTransaction(event).sendResponse(response);
        } catch (ParseException e) {
            throw new SipException("sendResponse()", e);
        }
    }

    public void sendReferNotify(Dialog dialog, String content) throws SipException {
        try {
            Request request = dialog.createRequest("NOTIFY");
            request.addHeader(this.mHeaderFactory.createSubscriptionStateHeader("active;expires=60"));
            request.setContent(content, this.mHeaderFactory.createContentTypeHeader("message", "sipfrag"));
            request.addHeader(this.mHeaderFactory.createEventHeader("refer"));
            log("send NOTIFY: " + request);
            dialog.sendRequest(this.mSipProvider.getNewClientTransaction(request));
        } catch (ParseException e) {
            throw new SipException("sendReferNotify()", e);
        }
    }

    public void sendInviteRequestTerminated(Request inviteRequest, ServerTransaction inviteTransaction) throws SipException {
        try {
            Response response = this.mMessageFactory.createResponse(487, inviteRequest);
            log("send response: " + response);
            inviteTransaction.sendResponse(response);
        } catch (ParseException e) {
            throw new SipException("sendInviteRequestTerminated()", e);
        }
    }

    public static String getCallId(EventObject event) {
        ServerTransaction clientTransaction;
        if (event == null) {
            return null;
        }
        if (event instanceof RequestEvent) {
            return getCallId((Message) event.getRequest());
        }
        if (event instanceof ResponseEvent) {
            return getCallId((Message) event.getResponse());
        }
        if (event instanceof DialogTerminatedEvent) {
            event.getDialog();
            return getCallId(event.getDialog());
        }
        if (event instanceof TransactionTerminatedEvent) {
            if (event.isServerTransaction()) {
                clientTransaction = event.getServerTransaction();
            } else {
                clientTransaction = event.getClientTransaction();
            }
            return getCallId((Transaction) clientTransaction);
        }
        Object source = event.getSource();
        if (source instanceof Transaction) {
            return getCallId((Transaction) source);
        }
        if (source instanceof Dialog) {
            return getCallId((Dialog) source);
        }
        return "";
    }

    public static String getCallId(Transaction transaction) {
        return transaction != null ? getCallId((Message) transaction.getRequest()) : "";
    }

    private static String getCallId(Message message) {
        CallIdHeader callIdHeader = message.getHeader("Call-ID");
        return callIdHeader.getCallId();
    }

    private static String getCallId(Dialog dialog) {
        return dialog.getCallId().getCallId();
    }

    private void log(String s) {
        Rlog.d(TAG, s);
    }
}
