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

    private static void putName(String str) {
        nameTable.put(str, str);
    }

    public static boolean isTargetRefresh(String str) {
        return targetRefreshMethods.contains(str);
    }

    public static boolean isDialogCreating(String str) {
        return SIPTransactionStack.isDialogCreated(str);
    }

    public static String getCannonicalName(String str) {
        if (nameTable.containsKey(str)) {
            return nameTable.get(str);
        }
        return str;
    }

    public RequestLine getRequestLine() {
        return this.requestLine;
    }

    public void setRequestLine(RequestLine requestLine) {
        this.requestLine = requestLine;
    }

    @Override
    public String debugDump() {
        String strDebugDump = super.debugDump();
        this.stringRepresentation = "";
        sprint(SIPRequest.class.getName());
        sprint("{");
        if (this.requestLine != null) {
            sprint(this.requestLine.debugDump());
        }
        sprint(strDebugDump);
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
            if ((this.requestLine.getUri() instanceof SipUri) && "sips".equalsIgnoreCase(((SipUri) this.requestLine.getUri()).getScheme())) {
                SipUri sipUri = (SipUri) getContactHeader().getAddress().getURI();
                if (!sipUri.getScheme().equals("sips")) {
                    throw new ParseException("Scheme for contact should be sips:" + sipUri, 0);
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
        GenericURI uri;
        if (this.requestLine == null || (method = this.requestLine.getMethod()) == null || (uri = this.requestLine.getUri()) == null) {
            return;
        }
        if ((method.compareTo("REGISTER") == 0 || method.compareTo("INVITE") == 0) && (uri instanceof SipUri)) {
            SipUri sipUri = (SipUri) uri;
            sipUri.setUserParam(DEFAULT_USER);
            try {
                sipUri.setTransportParam("udp");
            } catch (ParseException e) {
            }
        }
    }

    protected void setRequestLineDefaults() {
        CSeq cSeq;
        if (this.requestLine.getMethod() == null && (cSeq = (CSeq) getCSeq()) != null) {
            this.requestLine.setMethod(getCannonicalName(cSeq.getMethod()));
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
    public void setMethod(String str) {
        if (str == null) {
            throw new IllegalArgumentException("null method");
        }
        if (this.requestLine == null) {
            this.requestLine = new RequestLine();
        }
        String cannonicalName = getCannonicalName(str);
        this.requestLine.setMethod(cannonicalName);
        if (this.cSeqHeader != null) {
            try {
                this.cSeqHeader.setMethod(cannonicalName);
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
            return this.requestLine.encode() + super.encode();
        }
        if (isNullRequest()) {
            return "\r\n\r\n";
        }
        return super.encode();
    }

    @Override
    public String encodeMessage() {
        if (this.requestLine != null) {
            setRequestLineDefaults();
            return this.requestLine.encode() + super.encodeSIPHeaders();
        }
        if (isNullRequest()) {
            return "\r\n\r\n";
        }
        return super.encodeSIPHeaders();
    }

    @Override
    public String toString() {
        return encode();
    }

    @Override
    public Object clone() {
        SIPRequest sIPRequest = (SIPRequest) super.clone();
        sIPRequest.transactionPointer = null;
        if (this.requestLine != null) {
            sIPRequest.requestLine = (RequestLine) this.requestLine.clone();
        }
        return sIPRequest;
    }

    @Override
    public boolean equals(Object obj) {
        return getClass().equals(obj.getClass()) && this.requestLine.equals(((SIPRequest) obj).requestLine) && super.equals(obj);
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
    public boolean match(Object obj) {
        if (obj == null) {
            return true;
        }
        if (!obj.getClass().equals(getClass())) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        SIPRequest sIPRequest = (SIPRequest) obj;
        RequestLine requestLine = sIPRequest.requestLine;
        if (this.requestLine == null && requestLine != null) {
            return false;
        }
        if (this.requestLine == requestLine) {
            return super.match(obj);
        }
        return this.requestLine.match(sIPRequest.requestLine) && super.match(obj);
    }

    @Override
    public String getDialogId(boolean z) {
        StringBuffer stringBuffer = new StringBuffer(((CallID) getCallId()).getCallId());
        From from = (From) getFrom();
        To to = (To) getTo();
        if (!z) {
            if (from.getTag() != null) {
                stringBuffer.append(Separators.COLON);
                stringBuffer.append(from.getTag());
            }
            if (to.getTag() != null) {
                stringBuffer.append(Separators.COLON);
                stringBuffer.append(to.getTag());
            }
        } else {
            if (to.getTag() != null) {
                stringBuffer.append(Separators.COLON);
                stringBuffer.append(to.getTag());
            }
            if (from.getTag() != null) {
                stringBuffer.append(Separators.COLON);
                stringBuffer.append(from.getTag());
            }
        }
        return stringBuffer.toString().toLowerCase();
    }

    public String getDialogId(boolean z, String str) {
        From from = (From) getFrom();
        StringBuffer stringBuffer = new StringBuffer(((CallID) getCallId()).getCallId());
        if (!z) {
            if (from.getTag() != null) {
                stringBuffer.append(Separators.COLON);
                stringBuffer.append(from.getTag());
            }
            if (str != null) {
                stringBuffer.append(Separators.COLON);
                stringBuffer.append(str);
            }
        } else {
            if (str != null) {
                stringBuffer.append(Separators.COLON);
                stringBuffer.append(str);
            }
            if (from.getTag() != null) {
                stringBuffer.append(Separators.COLON);
                stringBuffer.append(from.getTag());
            }
        }
        return stringBuffer.toString().toLowerCase();
    }

    @Override
    public byte[] encodeAsBytes(String str) throws UnsupportedEncodingException {
        if (isNullRequest()) {
            return "\r\n\r\n".getBytes();
        }
        if (this.requestLine == null) {
            return new byte[0];
        }
        byte[] bytes = null;
        if (this.requestLine != null) {
            try {
                bytes = this.requestLine.encode().getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                InternalErrorHandler.handleException(e);
            }
        }
        byte[] bArrEncodeAsBytes = super.encodeAsBytes(str);
        byte[] bArr = new byte[bytes.length + bArrEncodeAsBytes.length];
        System.arraycopy(bytes, 0, bArr, 0, bytes.length);
        System.arraycopy(bArrEncodeAsBytes, 0, bArr, bytes.length, bArrEncodeAsBytes.length);
        return bArr;
    }

    public SIPResponse createResponse(int i) {
        return createResponse(i, SIPResponse.getReasonPhrase(i));
    }

    public SIPResponse createResponse(int i, String str) {
        SIPResponse sIPResponse = new SIPResponse();
        try {
            sIPResponse.setStatusCode(i);
            if (str != null) {
                sIPResponse.setReasonPhrase(str);
            } else {
                sIPResponse.setReasonPhrase(SIPResponse.getReasonPhrase(i));
            }
            Iterator<SIPHeader> headers = getHeaders();
            while (headers.hasNext()) {
                SIPHeader next = headers.next();
                if ((next instanceof From) || (next instanceof To) || (next instanceof ViaList) || (next instanceof CallID) || (((next instanceof RecordRouteList) && mustCopyRR(i)) || (next instanceof CSeq) || (next instanceof TimeStamp))) {
                    try {
                        sIPResponse.attachHeader((SIPHeader) next.clone(), false);
                    } catch (SIPDuplicateHeaderException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (MessageFactoryImpl.getDefaultServerHeader() != null) {
                sIPResponse.setHeader(MessageFactoryImpl.getDefaultServerHeader());
            }
            if (sIPResponse.getStatusCode() == 100) {
                sIPResponse.getTo().removeParameter(ParameterNames.TAG);
            }
            ServerHeader defaultServerHeader = MessageFactoryImpl.getDefaultServerHeader();
            if (defaultServerHeader != null) {
                sIPResponse.setHeader(defaultServerHeader);
            }
            return sIPResponse;
        } catch (ParseException e2) {
            throw new IllegalArgumentException("Bad code " + i);
        }
    }

    private final boolean mustCopyRR(int i) {
        return i > 100 && i < 300 && isDialogCreating(getMethod()) && getToTag() == null;
    }

    public SIPRequest createCancelRequest() throws SipException {
        if (!getMethod().equals("INVITE")) {
            throw new SipException("Attempt to create CANCEL for " + getMethod());
        }
        SIPRequest sIPRequest = new SIPRequest();
        sIPRequest.setRequestLine((RequestLine) this.requestLine.clone());
        sIPRequest.setMethod(Request.CANCEL);
        sIPRequest.setHeader((Header) this.callIdHeader.clone());
        sIPRequest.setHeader((Header) this.toHeader.clone());
        sIPRequest.setHeader((Header) this.cSeqHeader.clone());
        try {
            sIPRequest.getCSeq().setMethod(Request.CANCEL);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        sIPRequest.setHeader((Header) this.fromHeader.clone());
        sIPRequest.addFirst((Header) getTopmostVia().clone());
        sIPRequest.setHeader((Header) this.maxForwardsHeader.clone());
        if (getRouteHeaders() != null) {
            sIPRequest.setHeader((Header) getRouteHeaders().clone());
        }
        if (MessageFactoryImpl.getDefaultUserAgentHeader() != null) {
            sIPRequest.setHeader(MessageFactoryImpl.getDefaultUserAgentHeader());
        }
        return sIPRequest;
    }

    public SIPRequest createAckRequest(To to) {
        ?? r2;
        ?? sIPRequest = new SIPRequest();
        sIPRequest.setRequestLine((RequestLine) this.requestLine.clone());
        sIPRequest.setMethod("ACK");
        Iterator<SIPHeader> headers = getHeaders();
        while (headers.hasNext()) {
            SIPHeader next = headers.next();
            if (!(next instanceof RouteList) && !(next instanceof ProxyAuthorization)) {
                if (next instanceof ContentLength) {
                    r2 = (SIPHeader) next.clone();
                    try {
                        ((ContentLength) r2).setContentLength(0);
                    } catch (InvalidArgumentException e) {
                    }
                } else if (!(next instanceof ContentType)) {
                    if (next instanceof CSeq) {
                        r2 = (CSeq) next.clone();
                        try {
                            r2.setMethod("ACK");
                        } catch (ParseException e2) {
                        }
                    } else if (!(next instanceof To)) {
                        if (!(next instanceof ContactList) && !(next instanceof Expires)) {
                            r2 = next instanceof ViaList ? (SIPHeader) ((ViaList) next).getFirst().clone() : (SIPHeader) next.clone();
                        }
                    } else {
                        r2 = to != null ? to : (SIPHeader) next.clone();
                    }
                }
                try {
                    sIPRequest.attachHeader(r2, false);
                } catch (SIPDuplicateHeaderException e3) {
                    e3.printStackTrace();
                }
            }
        }
        if (MessageFactoryImpl.getDefaultUserAgentHeader() != null) {
            sIPRequest.setHeader(MessageFactoryImpl.getDefaultUserAgentHeader());
        }
        return sIPRequest;
    }

    public final SIPRequest createErrorAck(To to) throws SipException, ParseException {
        SIPRequest sIPRequest = new SIPRequest();
        sIPRequest.setRequestLine((RequestLine) this.requestLine.clone());
        sIPRequest.setMethod("ACK");
        sIPRequest.setHeader((Header) this.callIdHeader.clone());
        sIPRequest.setHeader((Header) this.maxForwardsHeader.clone());
        sIPRequest.setHeader((Header) this.fromHeader.clone());
        sIPRequest.setHeader((Header) to.clone());
        sIPRequest.addFirst((Header) getTopmostVia().clone());
        sIPRequest.setHeader((Header) this.cSeqHeader.clone());
        sIPRequest.getCSeq().setMethod("ACK");
        if (getRouteHeaders() != null) {
            sIPRequest.setHeader((SIPHeaderList<Via>) getRouteHeaders().clone());
        }
        if (MessageFactoryImpl.getDefaultUserAgentHeader() != null) {
            sIPRequest.setHeader(MessageFactoryImpl.getDefaultUserAgentHeader());
        }
        return sIPRequest;
    }

    public SIPRequest createSIPRequest(RequestLine requestLine, boolean z) {
        ?? sIPRequest = new SIPRequest();
        sIPRequest.requestLine = requestLine;
        Iterator<SIPHeader> headers = getHeaders();
        while (headers.hasNext()) {
            SIPHeader next = headers.next();
            if (next instanceof CSeq) {
                next = (CSeq) next.clone();
                try {
                    next.setMethod(requestLine.getMethod());
                } catch (ParseException e) {
                }
            } else if (next instanceof ViaList) {
                next = (Via) ((ViaList) next).getFirst().clone();
                next.removeParameter("branch");
            } else if (next instanceof To) {
                To to = (To) next;
                if (z) {
                    From from = new From(to);
                    from.removeTag();
                    next = from;
                } else {
                    next = (SIPHeader) to.clone();
                    ((To) next).removeTag();
                }
            } else if (next instanceof From) {
                From from2 = (From) next;
                if (z) {
                    To to2 = new To(from2);
                    to2.removeTag();
                    next = to2;
                } else {
                    next = (SIPHeader) from2.clone();
                    ((From) next).removeTag();
                }
            } else if (next instanceof ContentLength) {
                next = (ContentLength) next.clone();
                try {
                    next.setContentLength(0);
                } catch (InvalidArgumentException e2) {
                }
            } else if ((next instanceof CallID) || (next instanceof MaxForwards)) {
            }
            sIPRequest.attachHeader(next, false);
        }
        if (MessageFactoryImpl.getDefaultUserAgentHeader() != null) {
            sIPRequest.setHeader(MessageFactoryImpl.getDefaultUserAgentHeader());
        }
        return sIPRequest;
    }

    public SIPRequest createBYERequest(boolean z) {
        RequestLine requestLine = (RequestLine) this.requestLine.clone();
        requestLine.setMethod("BYE");
        return createSIPRequest(requestLine, z);
    }

    public SIPRequest createACKRequest() {
        RequestLine requestLine = (RequestLine) this.requestLine.clone();
        requestLine.setMethod("ACK");
        return createSIPRequest(requestLine, false);
    }

    public String getViaHost() {
        return ((Via) getViaHeaders().getFirst()).getHost();
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
    public void setSIPVersion(String str) throws ParseException {
        if (str == null || !str.equalsIgnoreCase(SIPConstants.SIP_VERSION_STRING)) {
            throw new ParseException("sipVersion", 0);
        }
        this.requestLine.setSipVersion(str);
    }

    @Override
    public String getSIPVersion() {
        return this.requestLine.getSipVersion();
    }

    public Object getTransaction() {
        return this.transactionPointer;
    }

    public void setTransaction(Object obj) {
        this.transactionPointer = obj;
    }

    public Object getMessageChannel() {
        return this.messageChannel;
    }

    public void setMessageChannel(Object obj) {
        this.messageChannel = obj;
    }

    public String getMergeId() {
        String fromTag = getFromTag();
        String string = this.cSeqHeader.toString();
        String callId = this.callIdHeader.getCallId();
        String string2 = getRequestURI().toString();
        if (fromTag != null) {
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append(string2);
            stringBuffer.append(Separators.COLON);
            stringBuffer.append(fromTag);
            stringBuffer.append(Separators.COLON);
            stringBuffer.append(string);
            stringBuffer.append(Separators.COLON);
            stringBuffer.append(callId);
            return stringBuffer.toString();
        }
        return null;
    }

    public void setInviteTransaction(Object obj) {
        this.inviteTransaction = obj;
    }

    public Object getInviteTransaction() {
        return this.inviteTransaction;
    }
}
