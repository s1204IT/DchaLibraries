package gov.nist.javax.sip.message;

import gov.nist.core.InternalErrorHandler;
import gov.nist.core.Separators;
import gov.nist.javax.sip.SIPConstants;
import gov.nist.javax.sip.address.GenericURI;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.header.CSeq;
import gov.nist.javax.sip.header.CallID;
import gov.nist.javax.sip.header.ContactList;
import gov.nist.javax.sip.header.ContentLength;
import gov.nist.javax.sip.header.ContentType;
import gov.nist.javax.sip.header.Expires;
import gov.nist.javax.sip.header.From;
import gov.nist.javax.sip.header.MaxForwards;
import gov.nist.javax.sip.header.ParameterNames;
import gov.nist.javax.sip.header.ProxyAuthorization;
import gov.nist.javax.sip.header.RecordRouteList;
import gov.nist.javax.sip.header.RequestLine;
import gov.nist.javax.sip.header.RouteList;
import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.SIPHeaderList;
import gov.nist.javax.sip.header.TimeStamp;
import gov.nist.javax.sip.header.To;
import gov.nist.javax.sip.header.Via;
import gov.nist.javax.sip.header.ViaList;
import gov.nist.javax.sip.stack.SIPTransactionStack;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import javax.sip.address.URI;
import javax.sip.header.Header;
import javax.sip.header.ServerHeader;
import javax.sip.message.Request;

public final class SIPRequest extends SIPMessage implements Request, RequestExt {
    private static final String DEFAULT_TRANSPORT = "udp";
    private static final String DEFAULT_USER = "ip";
    private static final long serialVersionUID = 3360720013577322927L;
    private transient Object inviteTransaction;
    private transient Object messageChannel;
    private RequestLine requestLine;
    private transient Object transactionPointer;
    private static final Set<String> targetRefreshMethods = new HashSet();
    private static final Hashtable<String, String> nameTable = new Hashtable<>();

    static {
        targetRefreshMethods.add("INVITE");
        targetRefreshMethods.add(Request.UPDATE);
        targetRefreshMethods.add("SUBSCRIBE");
        targetRefreshMethods.add("NOTIFY");
        targetRefreshMethods.add(Request.REFER);
        putName("INVITE");
        putName("BYE");
        putName(Request.CANCEL);
        putName("ACK");
        putName(Request.PRACK);
        putName(Request.INFO);
        putName("MESSAGE");
        putName("NOTIFY");
        putName("OPTIONS");
        putName(Request.PRACK);
        putName("PUBLISH");
        putName(Request.REFER);
        putName("REGISTER");
        putName("SUBSCRIBE");
        putName(Request.UPDATE);
    }

    private static void putName(String name) {
        nameTable.put(name, name);
    }

    public static boolean isTargetRefresh(String ucaseMethod) {
        return targetRefreshMethods.contains(ucaseMethod);
    }

    public static boolean isDialogCreating(String ucaseMethod) {
        return SIPTransactionStack.isDialogCreated(ucaseMethod);
    }

    public static String getCannonicalName(String method) {
        return nameTable.containsKey(method) ? nameTable.get(method) : method;
    }

    public RequestLine getRequestLine() {
        return this.requestLine;
    }

    public void setRequestLine(RequestLine requestLine) {
        this.requestLine = requestLine;
    }

    @Override
    public String debugDump() {
        String superstring = super.debugDump();
        this.stringRepresentation = "";
        sprint(SIPRequest.class.getName());
        sprint("{");
        if (this.requestLine != null) {
            sprint(this.requestLine.debugDump());
        }
        sprint(superstring);
        sprint("}");
        return this.stringRepresentation;
    }

    public void checkHeaders() throws ParseException {
        if (getCSeq() == null) {
            throw new ParseException("Missing a required header : CSeq", 0);
        }
        if (getTo() == null) {
            throw new ParseException("Missing a required header : To", 0);
        }
        if (this.callIdHeader == null || this.callIdHeader.getCallId() == null || this.callIdHeader.getCallId().equals("")) {
            throw new ParseException("Missing a required header : Call-ID", 0);
        }
        if (getFrom() == null) {
            throw new ParseException("Missing a required header : From", 0);
        }
        if (getViaHeaders() == null) {
            throw new ParseException("Missing a required header : Via", 0);
        }
        if (getTopmostVia() == null) {
            throw new ParseException("No via header in request! ", 0);
        }
        if (getMethod().equals("NOTIFY")) {
            if (getHeader("Subscription-State") == null) {
                throw new ParseException("Missing a required header : Subscription-State", 0);
            }
            if (getHeader("Event") == null) {
                throw new ParseException("Missing a required header : Event", 0);
            }
        } else if (getMethod().equals("PUBLISH") && getHeader("Event") == null) {
            throw new ParseException("Missing a required header : Event", 0);
        }
        if (this.requestLine.getMethod().equals("INVITE") || this.requestLine.getMethod().equals("SUBSCRIBE") || this.requestLine.getMethod().equals(Request.REFER)) {
            if (getContactHeader() == null && getToTag() == null) {
                throw new ParseException("Missing a required header : Contact", 0);
            }
            if (this.requestLine.getUri() instanceof SipUri) {
                String scheme = ((SipUri) this.requestLine.getUri()).getScheme();
                if ("sips".equalsIgnoreCase(scheme)) {
                    SipUri sipUri = (SipUri) getContactHeader().getAddress().getURI();
                    if (!sipUri.getScheme().equals("sips")) {
                        throw new ParseException("Scheme for contact should be sips:" + sipUri, 0);
                    }
                }
            }
        }
        if (getContactHeader() == null && (getMethod().equals("INVITE") || getMethod().equals(Request.REFER) || getMethod().equals("SUBSCRIBE"))) {
            throw new ParseException("Contact Header is Mandatory for a SIP INVITE", 0);
        }
        if (this.requestLine != null && this.requestLine.getMethod() != null && getCSeq().getMethod() != null && this.requestLine.getMethod().compareTo(getCSeq().getMethod()) != 0) {
            throw new ParseException("CSEQ method mismatch with  Request-Line ", 0);
        }
    }

    protected void setDefaults() {
        String method;
        GenericURI u;
        if (this.requestLine != null && (method = this.requestLine.getMethod()) != null && (u = this.requestLine.getUri()) != null) {
            if ((method.compareTo("REGISTER") == 0 || method.compareTo("INVITE") == 0) && (u instanceof SipUri)) {
                SipUri sipUri = (SipUri) u;
                sipUri.setUserParam(DEFAULT_USER);
                try {
                    sipUri.setTransportParam("udp");
                } catch (ParseException e) {
                }
            }
        }
    }

    protected void setRequestLineDefaults() {
        CSeq cseq;
        String method = this.requestLine.getMethod();
        if (method == null && (cseq = (CSeq) getCSeq()) != null) {
            String method2 = getCannonicalName(cseq.getMethod());
            this.requestLine.setMethod(method2);
        }
    }

    @Override
    public URI getRequestURI() {
        if (this.requestLine == null) {
            return null;
        }
        return this.requestLine.getUri();
    }

    @Override
    public void setRequestURI(URI uri) {
        if (uri == null) {
            throw new NullPointerException("Null request URI");
        }
        if (this.requestLine == null) {
            this.requestLine = new RequestLine();
        }
        this.requestLine.setUri((GenericURI) uri);
        this.nullRequest = false;
    }

    @Override
    public void setMethod(String method) {
        if (method == null) {
            throw new IllegalArgumentException("null method");
        }
        if (this.requestLine == null) {
            this.requestLine = new RequestLine();
        }
        String meth = getCannonicalName(method);
        this.requestLine.setMethod(meth);
        if (this.cSeqHeader != null) {
            try {
                this.cSeqHeader.setMethod(meth);
            } catch (ParseException e) {
            }
        }
    }

    @Override
    public String getMethod() {
        if (this.requestLine == null) {
            return null;
        }
        return this.requestLine.getMethod();
    }

    @Override
    public String encode() {
        if (this.requestLine != null) {
            setRequestLineDefaults();
            String retval = this.requestLine.encode() + super.encode();
            return retval;
        }
        if (isNullRequest()) {
            return "\r\n\r\n";
        }
        String retval2 = super.encode();
        return retval2;
    }

    @Override
    public String encodeMessage() {
        if (this.requestLine != null) {
            setRequestLineDefaults();
            String retval = this.requestLine.encode() + super.encodeSIPHeaders();
            return retval;
        }
        if (isNullRequest()) {
            return "\r\n\r\n";
        }
        String retval2 = super.encodeSIPHeaders();
        return retval2;
    }

    @Override
    public String toString() {
        return encode();
    }

    @Override
    public Object clone() {
        SIPRequest retval = (SIPRequest) super.clone();
        retval.transactionPointer = null;
        if (this.requestLine != null) {
            retval.requestLine = (RequestLine) this.requestLine.clone();
        }
        return retval;
    }

    @Override
    public boolean equals(Object other) {
        if (!getClass().equals(other.getClass())) {
            return false;
        }
        SIPRequest that = (SIPRequest) other;
        return this.requestLine.equals(that.requestLine) && super.equals(other);
    }

    @Override
    public LinkedList getMessageAsEncodedStrings() {
        LinkedList<String> messageAsEncodedStrings = super.getMessageAsEncodedStrings();
        if (this.requestLine != null) {
            setRequestLineDefaults();
            messageAsEncodedStrings.addFirst(this.requestLine.encode());
        }
        return messageAsEncodedStrings;
    }

    @Override
    public boolean match(Object matchObj) {
        if (matchObj == null) {
            return true;
        }
        if (!matchObj.getClass().equals(getClass())) {
            return false;
        }
        if (matchObj == this) {
            return true;
        }
        SIPRequest that = (SIPRequest) matchObj;
        RequestLine rline = that.requestLine;
        if (this.requestLine == null && rline != null) {
            return false;
        }
        if (this.requestLine == rline) {
            return super.match(matchObj);
        }
        return this.requestLine.match(that.requestLine) && super.match(matchObj);
    }

    @Override
    public String getDialogId(boolean isServer) {
        CallID cid = (CallID) getCallId();
        StringBuffer retval = new StringBuffer(cid.getCallId());
        From from = (From) getFrom();
        To to = (To) getTo();
        if (!isServer) {
            if (from.getTag() != null) {
                retval.append(Separators.COLON);
                retval.append(from.getTag());
            }
            if (to.getTag() != null) {
                retval.append(Separators.COLON);
                retval.append(to.getTag());
            }
        } else {
            if (to.getTag() != null) {
                retval.append(Separators.COLON);
                retval.append(to.getTag());
            }
            if (from.getTag() != null) {
                retval.append(Separators.COLON);
                retval.append(from.getTag());
            }
        }
        return retval.toString().toLowerCase();
    }

    public String getDialogId(boolean isServer, String toTag) {
        From from = (From) getFrom();
        CallID cid = (CallID) getCallId();
        StringBuffer retval = new StringBuffer(cid.getCallId());
        if (!isServer) {
            if (from.getTag() != null) {
                retval.append(Separators.COLON);
                retval.append(from.getTag());
            }
            if (toTag != null) {
                retval.append(Separators.COLON);
                retval.append(toTag);
            }
        } else {
            if (toTag != null) {
                retval.append(Separators.COLON);
                retval.append(toTag);
            }
            if (from.getTag() != null) {
                retval.append(Separators.COLON);
                retval.append(from.getTag());
            }
        }
        return retval.toString().toLowerCase();
    }

    @Override
    public byte[] encodeAsBytes(String transport) {
        if (isNullRequest()) {
            return "\r\n\r\n".getBytes();
        }
        if (this.requestLine == null) {
            return new byte[0];
        }
        byte[] rlbytes = null;
        if (this.requestLine != null) {
            try {
                rlbytes = this.requestLine.encode().getBytes("UTF-8");
            } catch (UnsupportedEncodingException ex) {
                InternalErrorHandler.handleException(ex);
            }
        }
        byte[] superbytes = super.encodeAsBytes(transport);
        byte[] retval = new byte[rlbytes.length + superbytes.length];
        System.arraycopy(rlbytes, 0, retval, 0, rlbytes.length);
        System.arraycopy(superbytes, 0, retval, rlbytes.length, superbytes.length);
        return retval;
    }

    public SIPResponse createResponse(int statusCode) {
        String reasonPhrase = SIPResponse.getReasonPhrase(statusCode);
        return createResponse(statusCode, reasonPhrase);
    }

    public SIPResponse createResponse(int statusCode, String reasonPhrase) {
        SIPResponse newResponse = new SIPResponse();
        try {
            newResponse.setStatusCode(statusCode);
            if (reasonPhrase != null) {
                newResponse.setReasonPhrase(reasonPhrase);
            } else {
                newResponse.setReasonPhrase(SIPResponse.getReasonPhrase(statusCode));
            }
            Iterator<SIPHeader> headers = getHeaders();
            while (headers.hasNext()) {
                SIPHeader nextHeader = headers.next();
                if ((nextHeader instanceof From) || (nextHeader instanceof To) || (nextHeader instanceof ViaList) || (nextHeader instanceof CallID) || (((nextHeader instanceof RecordRouteList) && mustCopyRR(statusCode)) || (nextHeader instanceof CSeq) || (nextHeader instanceof TimeStamp))) {
                    try {
                        newResponse.attachHeader((SIPHeader) nextHeader.clone(), false);
                    } catch (SIPDuplicateHeaderException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (MessageFactoryImpl.getDefaultServerHeader() != null) {
                newResponse.setHeader(MessageFactoryImpl.getDefaultServerHeader());
            }
            if (newResponse.getStatusCode() == 100) {
                newResponse.getTo().removeParameter(ParameterNames.TAG);
            }
            ServerHeader server = MessageFactoryImpl.getDefaultServerHeader();
            if (server != null) {
                newResponse.setHeader(server);
            }
            return newResponse;
        } catch (ParseException e2) {
            throw new IllegalArgumentException("Bad code " + statusCode);
        }
    }

    private final boolean mustCopyRR(int code) {
        return code > 100 && code < 300 && isDialogCreating(getMethod()) && getToTag() == null;
    }

    public SIPRequest createCancelRequest() throws SipException {
        if (!getMethod().equals("INVITE")) {
            throw new SipException("Attempt to create CANCEL for " + getMethod());
        }
        SIPRequest cancel = new SIPRequest();
        cancel.setRequestLine((RequestLine) this.requestLine.clone());
        cancel.setMethod(Request.CANCEL);
        cancel.setHeader((Header) this.callIdHeader.clone());
        cancel.setHeader((Header) this.toHeader.clone());
        cancel.setHeader((Header) this.cSeqHeader.clone());
        try {
            cancel.getCSeq().setMethod(Request.CANCEL);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        cancel.setHeader((Header) this.fromHeader.clone());
        cancel.addFirst((Header) getTopmostVia().clone());
        cancel.setHeader((Header) this.maxForwardsHeader.clone());
        if (getRouteHeaders() != null) {
            cancel.setHeader((Header) getRouteHeaders().clone());
        }
        if (MessageFactoryImpl.getDefaultUserAgentHeader() != null) {
            cancel.setHeader(MessageFactoryImpl.getDefaultUserAgentHeader());
        }
        return cancel;
    }

    public SIPRequest createAckRequest(To responseToHeader) {
        SIPHeader nextHeader;
        SIPRequest newRequest = new SIPRequest();
        newRequest.setRequestLine((RequestLine) this.requestLine.clone());
        newRequest.setMethod("ACK");
        Iterator<SIPHeader> headers = getHeaders();
        while (headers.hasNext()) {
            SIPHeader nextHeader2 = headers.next();
            if (!(nextHeader2 instanceof RouteList) && !(nextHeader2 instanceof ProxyAuthorization)) {
                if (nextHeader2 instanceof ContentLength) {
                    nextHeader = (SIPHeader) nextHeader2.clone();
                    try {
                        ((ContentLength) nextHeader).setContentLength(0);
                    } catch (InvalidArgumentException e) {
                    }
                } else if (!(nextHeader2 instanceof ContentType)) {
                    if (nextHeader2 instanceof CSeq) {
                        CSeq cseq = (CSeq) nextHeader2.clone();
                        try {
                            cseq.setMethod("ACK");
                        } catch (ParseException e2) {
                        }
                        nextHeader = cseq;
                    } else if (nextHeader2 instanceof To) {
                        nextHeader = responseToHeader != null ? responseToHeader : (SIPHeader) nextHeader2.clone();
                    } else if (!(nextHeader2 instanceof ContactList) && !(nextHeader2 instanceof Expires)) {
                        nextHeader = nextHeader2 instanceof ViaList ? (SIPHeader) ((ViaList) nextHeader2).getFirst().clone() : (SIPHeader) nextHeader2.clone();
                    }
                }
                try {
                    newRequest.attachHeader(nextHeader, false);
                } catch (SIPDuplicateHeaderException e3) {
                    e3.printStackTrace();
                }
            }
        }
        if (MessageFactoryImpl.getDefaultUserAgentHeader() != null) {
            newRequest.setHeader(MessageFactoryImpl.getDefaultUserAgentHeader());
        }
        return newRequest;
    }

    public final SIPRequest createErrorAck(To responseToHeader) throws SipException, ParseException {
        SIPRequest newRequest = new SIPRequest();
        newRequest.setRequestLine((RequestLine) this.requestLine.clone());
        newRequest.setMethod("ACK");
        newRequest.setHeader((Header) this.callIdHeader.clone());
        newRequest.setHeader((Header) this.maxForwardsHeader.clone());
        newRequest.setHeader((Header) this.fromHeader.clone());
        newRequest.setHeader((Header) responseToHeader.clone());
        newRequest.addFirst((Header) getTopmostVia().clone());
        newRequest.setHeader((Header) this.cSeqHeader.clone());
        newRequest.getCSeq().setMethod("ACK");
        if (getRouteHeaders() != null) {
            newRequest.setHeader((SIPHeaderList<Via>) getRouteHeaders().clone());
        }
        if (MessageFactoryImpl.getDefaultUserAgentHeader() != null) {
            newRequest.setHeader(MessageFactoryImpl.getDefaultUserAgentHeader());
        }
        return newRequest;
    }

    public SIPRequest createSIPRequest(RequestLine requestLine, boolean switchHeaders) {
        SIPRequest newRequest = new SIPRequest();
        newRequest.requestLine = requestLine;
        Iterator<SIPHeader> headerIterator = getHeaders();
        while (headerIterator.hasNext()) {
            SIPHeader nextHeader = headerIterator.next();
            if (nextHeader instanceof CSeq) {
                CSeq newCseq = (CSeq) nextHeader.clone();
                nextHeader = newCseq;
                try {
                    newCseq.setMethod(requestLine.getMethod());
                } catch (ParseException e) {
                }
            } else if (nextHeader instanceof ViaList) {
                Via via = (Via) ((ViaList) nextHeader).getFirst().clone();
                via.removeParameter("branch");
                nextHeader = via;
            } else if (nextHeader instanceof To) {
                To to = (To) nextHeader;
                if (switchHeaders) {
                    nextHeader = new From(to);
                    ((From) nextHeader).removeTag();
                } else {
                    nextHeader = (SIPHeader) to.clone();
                    ((To) nextHeader).removeTag();
                }
            } else if (nextHeader instanceof From) {
                From from = (From) nextHeader;
                if (switchHeaders) {
                    nextHeader = new To(from);
                    ((To) nextHeader).removeTag();
                } else {
                    nextHeader = (SIPHeader) from.clone();
                    ((From) nextHeader).removeTag();
                }
            } else if (nextHeader instanceof ContentLength) {
                ContentLength cl = (ContentLength) nextHeader.clone();
                try {
                    cl.setContentLength(0);
                } catch (InvalidArgumentException e2) {
                }
                nextHeader = cl;
            } else if ((nextHeader instanceof CallID) || (nextHeader instanceof MaxForwards)) {
            }
            newRequest.attachHeader(nextHeader, false);
        }
        if (MessageFactoryImpl.getDefaultUserAgentHeader() != null) {
            newRequest.setHeader(MessageFactoryImpl.getDefaultUserAgentHeader());
        }
        return newRequest;
    }

    public SIPRequest createBYERequest(boolean switchHeaders) {
        RequestLine requestLine = (RequestLine) this.requestLine.clone();
        requestLine.setMethod("BYE");
        return createSIPRequest(requestLine, switchHeaders);
    }

    public SIPRequest createACKRequest() {
        RequestLine requestLine = (RequestLine) this.requestLine.clone();
        requestLine.setMethod("ACK");
        return createSIPRequest(requestLine, false);
    }

    public String getViaHost() {
        Via via = (Via) getViaHeaders().getFirst();
        return via.getHost();
    }

    public int getViaPort() {
        Via via = (Via) getViaHeaders().getFirst();
        if (via.hasPort()) {
            return via.getPort();
        }
        return 5060;
    }

    @Override
    public String getFirstLine() {
        if (this.requestLine == null) {
            return null;
        }
        return this.requestLine.encode();
    }

    @Override
    public void setSIPVersion(String sipVersion) throws ParseException {
        if (sipVersion == null || !sipVersion.equalsIgnoreCase(SIPConstants.SIP_VERSION_STRING)) {
            throw new ParseException("sipVersion", 0);
        }
        this.requestLine.setSipVersion(sipVersion);
    }

    @Override
    public String getSIPVersion() {
        return this.requestLine.getSipVersion();
    }

    public Object getTransaction() {
        return this.transactionPointer;
    }

    public void setTransaction(Object transaction) {
        this.transactionPointer = transaction;
    }

    public Object getMessageChannel() {
        return this.messageChannel;
    }

    public void setMessageChannel(Object messageChannel) {
        this.messageChannel = messageChannel;
    }

    public String getMergeId() {
        String fromTag = getFromTag();
        String cseq = this.cSeqHeader.toString();
        String callId = this.callIdHeader.getCallId();
        String requestUri = getRequestURI().toString();
        if (fromTag != null) {
            return new StringBuffer().append(requestUri).append(Separators.COLON).append(fromTag).append(Separators.COLON).append(cseq).append(Separators.COLON).append(callId).toString();
        }
        return null;
    }

    public void setInviteTransaction(Object inviteTransaction) {
        this.inviteTransaction = inviteTransaction;
    }

    public Object getInviteTransaction() {
        return this.inviteTransaction;
    }
}
