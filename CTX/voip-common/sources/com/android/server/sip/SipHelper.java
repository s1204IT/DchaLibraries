package com.android.server.sip;

import android.net.sip.SipProfile;
import android.telephony.Rlog;
import gov.nist.javax.sip.clientauthutils.AccountManager;
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
import javax.sip.header.HeaderFactory;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.Message;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

class SipHelper {
    private static final boolean DBG = false;
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

    private FromHeader createFromHeader(SipProfile sipProfile, String str) throws ParseException {
        return this.mHeaderFactory.createFromHeader(sipProfile.getSipAddress(), str);
    }

    private ToHeader createToHeader(SipProfile sipProfile) throws ParseException {
        return createToHeader(sipProfile, null);
    }

    private ToHeader createToHeader(SipProfile sipProfile, String str) throws ParseException {
        return this.mHeaderFactory.createToHeader(sipProfile.getSipAddress(), str);
    }

    private CallIdHeader createCallIdHeader() {
        return this.mSipProvider.getNewCallId();
    }

    private CSeqHeader createCSeqHeader(String str) throws InvalidArgumentException, ParseException {
        return this.mHeaderFactory.createCSeqHeader((long) (Math.random() * 10000.0d), str);
    }

    private MaxForwardsHeader createMaxForwardsHeader() throws InvalidArgumentException {
        return this.mHeaderFactory.createMaxForwardsHeader(70);
    }

    private MaxForwardsHeader createMaxForwardsHeader(int i) throws InvalidArgumentException {
        return this.mHeaderFactory.createMaxForwardsHeader(i);
    }

    private ListeningPoint getListeningPoint() throws SipException {
        ListeningPoint[] listeningPoints;
        ListeningPoint listeningPoint = this.mSipProvider.getListeningPoint("UDP");
        if (listeningPoint == null) {
            listeningPoint = this.mSipProvider.getListeningPoint("TCP");
        }
        if (listeningPoint == null && (listeningPoints = this.mSipProvider.getListeningPoints()) != null && listeningPoints.length > 0) {
            listeningPoint = listeningPoints[0];
        }
        if (listeningPoint == null) {
            throw new SipException("no listening point is available");
        }
        return listeningPoint;
    }

    private List<ViaHeader> createViaHeaders() throws SipException, ParseException {
        ArrayList arrayList = new ArrayList(1);
        ListeningPoint listeningPoint = getListeningPoint();
        ViaHeader viaHeaderCreateViaHeader = this.mHeaderFactory.createViaHeader(listeningPoint.getIPAddress(), listeningPoint.getPort(), listeningPoint.getTransport(), (String) null);
        viaHeaderCreateViaHeader.setRPort();
        arrayList.add(viaHeaderCreateViaHeader);
        return arrayList;
    }

    private ContactHeader createContactHeader(SipProfile sipProfile) throws SipException, ParseException {
        return createContactHeader(sipProfile, null, 0);
    }

    private ContactHeader createContactHeader(SipProfile sipProfile, String str, int i) throws SipException, ParseException {
        SipURI sipURICreateSipUri;
        if (str == null) {
            sipURICreateSipUri = createSipUri(sipProfile.getUserName(), sipProfile.getProtocol(), getListeningPoint());
        } else {
            sipURICreateSipUri = createSipUri(sipProfile.getUserName(), sipProfile.getProtocol(), str, i);
        }
        Address addressCreateAddress = this.mAddressFactory.createAddress(sipURICreateSipUri);
        addressCreateAddress.setDisplayName(sipProfile.getDisplayName());
        return this.mHeaderFactory.createContactHeader(addressCreateAddress);
    }

    private ContactHeader createWildcardContactHeader() {
        ContactHeader contactHeaderCreateContactHeader = this.mHeaderFactory.createContactHeader();
        contactHeaderCreateContactHeader.setWildCard();
        return contactHeaderCreateContactHeader;
    }

    private SipURI createSipUri(String str, String str2, ListeningPoint listeningPoint) throws ParseException {
        return createSipUri(str, str2, listeningPoint.getIPAddress(), listeningPoint.getPort());
    }

    private SipURI createSipUri(String str, String str2, String str3, int i) throws ParseException {
        SipURI sipURICreateSipURI = this.mAddressFactory.createSipURI(str, str3);
        try {
            sipURICreateSipURI.setPort(i);
            sipURICreateSipURI.setTransportParam(str2);
            return sipURICreateSipURI;
        } catch (InvalidArgumentException e) {
            throw new RuntimeException((Throwable) e);
        }
    }

    public ClientTransaction sendOptions(SipProfile sipProfile, SipProfile sipProfile2, String str) throws InvalidArgumentException, SipException {
        Request requestCreateRequest;
        try {
            if (sipProfile == sipProfile2) {
                requestCreateRequest = createRequest("OPTIONS", sipProfile, str);
            } else {
                requestCreateRequest = createRequest("OPTIONS", sipProfile, sipProfile2, str);
            }
            ClientTransaction newClientTransaction = this.mSipProvider.getNewClientTransaction(requestCreateRequest);
            newClientTransaction.sendRequest();
            return newClientTransaction;
        } catch (Exception e) {
            throw new SipException("sendOptions()", e);
        }
    }

    public ClientTransaction sendRegister(SipProfile sipProfile, String str, int i) throws InvalidArgumentException, SipException {
        try {
            Request requestCreateRequest = createRequest("REGISTER", sipProfile, str);
            if (i == 0) {
                requestCreateRequest.addHeader(createWildcardContactHeader());
            } else {
                requestCreateRequest.addHeader(createContactHeader(sipProfile));
            }
            requestCreateRequest.addHeader(this.mHeaderFactory.createExpiresHeader(i));
            ClientTransaction newClientTransaction = this.mSipProvider.getNewClientTransaction(requestCreateRequest);
            newClientTransaction.sendRequest();
            return newClientTransaction;
        } catch (ParseException e) {
            throw new SipException("sendRegister()", e);
        }
    }

    private Request createRequest(String str, SipProfile sipProfile, String str2) throws InvalidArgumentException, SipException, ParseException {
        FromHeader fromHeaderCreateFromHeader = createFromHeader(sipProfile, str2);
        ToHeader toHeaderCreateToHeader = createToHeader(sipProfile);
        Request requestCreateRequest = this.mMessageFactory.createRequest(this.mAddressFactory.createSipURI(sipProfile.getUriString().replaceFirst(Pattern.quote(sipProfile.getUserName() + "@"), "")), str, createCallIdHeader(), createCSeqHeader(str), fromHeaderCreateFromHeader, toHeaderCreateToHeader, createViaHeaders(), createMaxForwardsHeader());
        requestCreateRequest.addHeader(this.mHeaderFactory.createHeader("User-Agent", "SIPAUA/0.1.001"));
        return requestCreateRequest;
    }

    public ClientTransaction handleChallenge(ResponseEvent responseEvent, AccountManager accountManager) throws SipException {
        ClientTransaction clientTransactionHandleChallenge = this.mSipStack.getAuthenticationHelper(accountManager, this.mHeaderFactory).handleChallenge(responseEvent.getResponse(), responseEvent.getClientTransaction(), this.mSipProvider, 5);
        clientTransactionHandleChallenge.sendRequest();
        return clientTransactionHandleChallenge;
    }

    private Request createRequest(String str, SipProfile sipProfile, SipProfile sipProfile2, String str2) throws InvalidArgumentException, SipException, ParseException {
        FromHeader fromHeaderCreateFromHeader = createFromHeader(sipProfile, str2);
        ToHeader toHeaderCreateToHeader = createToHeader(sipProfile2);
        SipURI uri = sipProfile2.getUri();
        List<ViaHeader> listCreateViaHeaders = createViaHeaders();
        Request requestCreateRequest = this.mMessageFactory.createRequest(uri, str, createCallIdHeader(), createCSeqHeader(str), fromHeaderCreateFromHeader, toHeaderCreateToHeader, listCreateViaHeaders, createMaxForwardsHeader());
        requestCreateRequest.addHeader(createContactHeader(sipProfile));
        return requestCreateRequest;
    }

    public ClientTransaction sendInvite(SipProfile sipProfile, SipProfile sipProfile2, String str, String str2, ReferredByHeader referredByHeader, String str3) throws InvalidArgumentException, SipException {
        try {
            Request requestCreateRequest = createRequest("INVITE", sipProfile, sipProfile2, str2);
            if (referredByHeader != null) {
                requestCreateRequest.addHeader(referredByHeader);
            }
            if (str3 != null) {
                requestCreateRequest.addHeader(this.mHeaderFactory.createHeader("Replaces", str3));
            }
            requestCreateRequest.setContent(str, this.mHeaderFactory.createContentTypeHeader("application", "sdp"));
            ClientTransaction newClientTransaction = this.mSipProvider.getNewClientTransaction(requestCreateRequest);
            newClientTransaction.sendRequest();
            return newClientTransaction;
        } catch (ParseException e) {
            throw new SipException("sendInvite()", e);
        }
    }

    public ClientTransaction sendReinvite(Dialog dialog, String str) throws SipException {
        try {
            Request requestCreateRequest = dialog.createRequest("INVITE");
            requestCreateRequest.setContent(str, this.mHeaderFactory.createContentTypeHeader("application", "sdp"));
            ViaHeader header = requestCreateRequest.getHeader("Via");
            if (header != null) {
                header.setRPort();
            }
            ClientTransaction newClientTransaction = this.mSipProvider.getNewClientTransaction(requestCreateRequest);
            dialog.sendRequest(newClientTransaction);
            return newClientTransaction;
        } catch (ParseException e) {
            throw new SipException("sendReinvite()", e);
        }
    }

    public ServerTransaction getServerTransaction(RequestEvent requestEvent) throws SipException {
        ServerTransaction serverTransaction = requestEvent.getServerTransaction();
        if (serverTransaction == null) {
            return this.mSipProvider.getNewServerTransaction(requestEvent.getRequest());
        }
        return serverTransaction;
    }

    public ServerTransaction sendRinging(RequestEvent requestEvent, String str) throws SipException {
        try {
            Request request = requestEvent.getRequest();
            ServerTransaction serverTransaction = getServerTransaction(requestEvent);
            Response responseCreateResponse = this.mMessageFactory.createResponse(180, request);
            ToHeader header = responseCreateResponse.getHeader("To");
            header.setTag(str);
            responseCreateResponse.addHeader(header);
            serverTransaction.sendResponse(responseCreateResponse);
            return serverTransaction;
        } catch (ParseException e) {
            throw new SipException("sendRinging()", e);
        }
    }

    public ServerTransaction sendInviteOk(RequestEvent requestEvent, SipProfile sipProfile, String str, ServerTransaction serverTransaction, String str2, int i) throws SipException {
        try {
            Response responseCreateResponse = this.mMessageFactory.createResponse(200, requestEvent.getRequest());
            responseCreateResponse.addHeader(createContactHeader(sipProfile, str2, i));
            responseCreateResponse.setContent(str, this.mHeaderFactory.createContentTypeHeader("application", "sdp"));
            if (serverTransaction == null) {
                serverTransaction = getServerTransaction(requestEvent);
            }
            if (serverTransaction.getState() != TransactionState.COMPLETED) {
                serverTransaction.sendResponse(responseCreateResponse);
            }
            return serverTransaction;
        } catch (ParseException e) {
            throw new SipException("sendInviteOk()", e);
        }
    }

    public void sendInviteBusyHere(RequestEvent requestEvent, ServerTransaction serverTransaction) throws SipException {
        try {
            Response responseCreateResponse = this.mMessageFactory.createResponse(486, requestEvent.getRequest());
            if (serverTransaction == null) {
                serverTransaction = getServerTransaction(requestEvent);
            }
            if (serverTransaction.getState() != TransactionState.COMPLETED) {
                serverTransaction.sendResponse(responseCreateResponse);
            }
        } catch (ParseException e) {
            throw new SipException("sendInviteBusyHere()", e);
        }
    }

    public void sendInviteAck(ResponseEvent responseEvent, Dialog dialog) throws SipException {
        dialog.sendAck(dialog.createAck(responseEvent.getResponse().getHeader("CSeq").getSeqNumber()));
    }

    public void sendBye(Dialog dialog) throws SipException {
        dialog.sendRequest(this.mSipProvider.getNewClientTransaction(dialog.createRequest("BYE")));
    }

    public void sendCancel(ClientTransaction clientTransaction) throws SipException {
        this.mSipProvider.getNewClientTransaction(clientTransaction.createCancel()).sendRequest();
    }

    public void sendResponse(RequestEvent requestEvent, int i) throws SipException {
        try {
            getServerTransaction(requestEvent).sendResponse(this.mMessageFactory.createResponse(i, requestEvent.getRequest()));
        } catch (ParseException e) {
            throw new SipException("sendResponse()", e);
        }
    }

    public void sendReferNotify(Dialog dialog, String str) throws SipException {
        try {
            Request requestCreateRequest = dialog.createRequest("NOTIFY");
            requestCreateRequest.addHeader(this.mHeaderFactory.createSubscriptionStateHeader("active;expires=60"));
            requestCreateRequest.setContent(str, this.mHeaderFactory.createContentTypeHeader("message", "sipfrag"));
            requestCreateRequest.addHeader(this.mHeaderFactory.createEventHeader("refer"));
            dialog.sendRequest(this.mSipProvider.getNewClientTransaction(requestCreateRequest));
        } catch (ParseException e) {
            throw new SipException("sendReferNotify()", e);
        }
    }

    public void sendInviteRequestTerminated(Request request, ServerTransaction serverTransaction) throws SipException {
        try {
            serverTransaction.sendResponse(this.mMessageFactory.createResponse(487, request));
        } catch (ParseException e) {
            throw new SipException("sendInviteRequestTerminated()", e);
        }
    }

    public static String getCallId(EventObject eventObject) {
        ServerTransaction clientTransaction;
        if (eventObject == null) {
            return null;
        }
        if (eventObject instanceof RequestEvent) {
            return getCallId((Message) ((RequestEvent) eventObject).getRequest());
        }
        if (eventObject instanceof ResponseEvent) {
            return getCallId((Message) ((ResponseEvent) eventObject).getResponse());
        }
        if (eventObject instanceof DialogTerminatedEvent) {
            DialogTerminatedEvent dialogTerminatedEvent = (DialogTerminatedEvent) eventObject;
            dialogTerminatedEvent.getDialog();
            return getCallId(dialogTerminatedEvent.getDialog());
        }
        if (eventObject instanceof TransactionTerminatedEvent) {
            TransactionTerminatedEvent transactionTerminatedEvent = (TransactionTerminatedEvent) eventObject;
            if (transactionTerminatedEvent.isServerTransaction()) {
                clientTransaction = transactionTerminatedEvent.getServerTransaction();
            } else {
                clientTransaction = transactionTerminatedEvent.getClientTransaction();
            }
            return getCallId((Transaction) clientTransaction);
        }
        Object source = eventObject.getSource();
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
        return message.getHeader("Call-ID").getCallId();
    }

    private static String getCallId(Dialog dialog) {
        return dialog.getCallId().getCallId();
    }

    private void log(String str) {
        Rlog.d(TAG, str);
    }
}
