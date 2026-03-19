package gov.nist.javax.sip.message;

import gov.nist.core.InternalErrorHandler;
import gov.nist.core.Separators;
import gov.nist.javax.sip.Utils;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.header.CSeq;
import gov.nist.javax.sip.header.CallID;
import gov.nist.javax.sip.header.ContactList;
import gov.nist.javax.sip.header.ContentLength;
import gov.nist.javax.sip.header.ContentType;
import gov.nist.javax.sip.header.From;
import gov.nist.javax.sip.header.MaxForwards;
import gov.nist.javax.sip.header.ReasonList;
import gov.nist.javax.sip.header.RecordRouteList;
import gov.nist.javax.sip.header.RequireList;
import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.StatusLine;
import gov.nist.javax.sip.header.To;
import gov.nist.javax.sip.header.Via;
import gov.nist.javax.sip.header.ViaList;
import gov.nist.javax.sip.header.extensions.SessionExpires;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.Iterator;
import java.util.LinkedList;
import javax.sip.header.ReasonHeader;
import javax.sip.header.ServerHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

public final class SIPResponse extends SIPMessage implements Response, ResponseExt {
    protected StatusLine statusLine;

    public static String getReasonPhrase(int i) {
        switch (i) {
            case Response.RINGING:
                return "Ringing";
            case Response.CALL_IS_BEING_FORWARDED:
                return "Call is being forwarded";
            case Response.QUEUED:
                return "Queued";
            case Response.SESSION_PROGRESS:
                return "Session progress";
            default:
                switch (i) {
                    case Response.MULTIPLE_CHOICES:
                        return "Multiple choices";
                    case Response.MOVED_PERMANENTLY:
                        return "Moved permanently";
                    case Response.MOVED_TEMPORARILY:
                        return "Moved Temporarily";
                    default:
                        switch (i) {
                            case Response.BAD_REQUEST:
                                return "Bad request";
                            case Response.UNAUTHORIZED:
                                return "Unauthorized";
                            case Response.PAYMENT_REQUIRED:
                                return "Payment required";
                            case Response.FORBIDDEN:
                                return "Forbidden";
                            case Response.NOT_FOUND:
                                return "Not found";
                            case Response.METHOD_NOT_ALLOWED:
                                return "Method not allowed";
                            case Response.NOT_ACCEPTABLE:
                                return "Not acceptable";
                            case Response.PROXY_AUTHENTICATION_REQUIRED:
                                return "Proxy Authentication required";
                            case Response.REQUEST_TIMEOUT:
                                return "Request timeout";
                            default:
                                switch (i) {
                                    case Response.CONDITIONAL_REQUEST_FAILED:
                                        return "Conditional request failed";
                                    case Response.REQUEST_ENTITY_TOO_LARGE:
                                        return "Request entity too large";
                                    case Response.REQUEST_URI_TOO_LONG:
                                        return "Request-URI too large";
                                    case Response.UNSUPPORTED_MEDIA_TYPE:
                                        return "Unsupported media type";
                                    case Response.UNSUPPORTED_URI_SCHEME:
                                        return "Unsupported URI Scheme";
                                    default:
                                        switch (i) {
                                            case Response.BAD_EXTENSION:
                                                return "Bad extension";
                                            case Response.EXTENSION_REQUIRED:
                                                return "Etension Required";
                                            default:
                                                switch (i) {
                                                    case Response.TEMPORARILY_UNAVAILABLE:
                                                        return "Temporarily Unavailable";
                                                    case Response.CALL_OR_TRANSACTION_DOES_NOT_EXIST:
                                                        return "Call leg/Transaction does not exist";
                                                    case Response.LOOP_DETECTED:
                                                        return "Loop detected";
                                                    case Response.TOO_MANY_HOPS:
                                                        return "Too many hops";
                                                    case Response.ADDRESS_INCOMPLETE:
                                                        return "Address incomplete";
                                                    case Response.AMBIGUOUS:
                                                        return "Ambiguous";
                                                    case Response.BUSY_HERE:
                                                        return "Busy here";
                                                    case Response.REQUEST_TERMINATED:
                                                        return "Request Terminated";
                                                    case Response.NOT_ACCEPTABLE_HERE:
                                                        return "Not Acceptable here";
                                                    case Response.BAD_EVENT:
                                                        return "Bad Event";
                                                    default:
                                                        switch (i) {
                                                            case 500:
                                                                return "Server Internal Error";
                                                            case Response.NOT_IMPLEMENTED:
                                                                return "Not implemented";
                                                            case Response.BAD_GATEWAY:
                                                                return "Bad gateway";
                                                            case Response.SERVICE_UNAVAILABLE:
                                                                return "Service unavailable";
                                                            case Response.SERVER_TIMEOUT:
                                                                return "Gateway timeout";
                                                            case Response.VERSION_NOT_SUPPORTED:
                                                                return "SIP version not supported";
                                                            default:
                                                                switch (i) {
                                                                    case Response.DECLINE:
                                                                        return "Decline";
                                                                    case Response.DOES_NOT_EXIST_ANYWHERE:
                                                                        return "Does not exist anywhere";
                                                                    default:
                                                                        switch (i) {
                                                                            case Response.TRYING:
                                                                                return "Trying";
                                                                            case Response.OK:
                                                                                return "OK";
                                                                            case Response.ACCEPTED:
                                                                                return "Accepted";
                                                                            case Response.USE_PROXY:
                                                                                return "Use proxy";
                                                                            case Response.ALTERNATIVE_SERVICE:
                                                                                return "Alternative service";
                                                                            case Response.GONE:
                                                                                return "Gone";
                                                                            case Response.INTERVAL_TOO_BRIEF:
                                                                                return "Interval too brief";
                                                                            case Response.REQUEST_PENDING:
                                                                                return "Request Pending";
                                                                            case Response.UNDECIPHERABLE:
                                                                                return "Undecipherable";
                                                                            case Response.MESSAGE_TOO_LARGE:
                                                                                return "Message Too Large";
                                                                            case Response.BUSY_EVERYWHERE:
                                                                                return "Busy everywhere";
                                                                            case Response.SESSION_NOT_ACCEPTABLE:
                                                                                return "Session Not acceptable";
                                                                            default:
                                                                                return "Unknown Status";
                                                                        }
                                                                }
                                                        }
                                                }
                                        }
                                }
                        }
                }
        }
    }

    @Override
    public void setStatusCode(int i) throws ParseException {
        if (i < 100 || i > 699) {
            throw new ParseException("bad status code", 0);
        }
        if (this.statusLine == null) {
            this.statusLine = new StatusLine();
        }
        this.statusLine.setStatusCode(i);
    }

    public StatusLine getStatusLine() {
        return this.statusLine;
    }

    @Override
    public int getStatusCode() {
        return this.statusLine.getStatusCode();
    }

    @Override
    public void setReasonPhrase(String str) {
        if (str == null) {
            throw new IllegalArgumentException("Bad reason phrase");
        }
        if (this.statusLine == null) {
            this.statusLine = new StatusLine();
        }
        this.statusLine.setReasonPhrase(str);
    }

    @Override
    public String getReasonPhrase() {
        if (this.statusLine == null || this.statusLine.getReasonPhrase() == null) {
            return "";
        }
        return this.statusLine.getReasonPhrase();
    }

    public static boolean isFinalResponse(int i) {
        return i >= 200 && i < 700;
    }

    public boolean isFinalResponse() {
        return isFinalResponse(this.statusLine.getStatusCode());
    }

    public void setStatusLine(StatusLine statusLine) {
        this.statusLine = statusLine;
    }

    @Override
    public String debugDump() {
        String strDebugDump = super.debugDump();
        this.stringRepresentation = "";
        sprint(SIPResponse.class.getCanonicalName());
        sprint("{");
        if (this.statusLine != null) {
            sprint(this.statusLine.debugDump());
        }
        sprint(strDebugDump);
        sprint("}");
        return this.stringRepresentation;
    }

    public void checkHeaders() throws ParseException {
        if (getCSeq() == null) {
            throw new ParseException("CSeq Is missing ", 0);
        }
        if (getTo() == null) {
            throw new ParseException("To Is missing ", 0);
        }
        if (getFrom() == null) {
            throw new ParseException("From Is missing ", 0);
        }
        if (getViaHeaders() == null) {
            throw new ParseException("Via Is missing ", 0);
        }
        if (getCallId() == null) {
            throw new ParseException("Call-ID Is missing ", 0);
        }
        if (getStatusCode() > 699) {
            throw new ParseException("Unknown error code!" + getStatusCode(), 0);
        }
    }

    @Override
    public String encode() {
        if (this.statusLine != null) {
            return this.statusLine.encode() + super.encode();
        }
        return super.encode();
    }

    @Override
    public String encodeMessage() {
        if (this.statusLine != null) {
            return this.statusLine.encode() + super.encodeSIPHeaders();
        }
        return super.encodeSIPHeaders();
    }

    @Override
    public LinkedList getMessageAsEncodedStrings() {
        LinkedList<String> messageAsEncodedStrings = super.getMessageAsEncodedStrings();
        if (this.statusLine != null) {
            messageAsEncodedStrings.addFirst(this.statusLine.encode());
        }
        return messageAsEncodedStrings;
    }

    @Override
    public Object clone() {
        SIPResponse sIPResponse = (SIPResponse) super.clone();
        if (this.statusLine != null) {
            sIPResponse.statusLine = (StatusLine) this.statusLine.clone();
        }
        return sIPResponse;
    }

    @Override
    public boolean equals(Object obj) {
        return getClass().equals(obj.getClass()) && this.statusLine.equals(((SIPResponse) obj).statusLine) && super.equals(obj);
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
        SIPResponse sIPResponse = (SIPResponse) obj;
        StatusLine statusLine = sIPResponse.statusLine;
        if (this.statusLine == null && statusLine != null) {
            return false;
        }
        if (this.statusLine == statusLine) {
            return super.match(obj);
        }
        return this.statusLine.match(sIPResponse.statusLine) && super.match(obj);
    }

    @Override
    public byte[] encodeAsBytes(String str) {
        byte[] bytes;
        if (this.statusLine != null) {
            try {
                bytes = this.statusLine.encode().getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                InternalErrorHandler.handleException(e);
                bytes = null;
            }
        } else {
            bytes = null;
        }
        byte[] bArrEncodeAsBytes = super.encodeAsBytes(str);
        byte[] bArr = new byte[bytes.length + bArrEncodeAsBytes.length];
        System.arraycopy(bytes, 0, bArr, 0, bytes.length);
        System.arraycopy(bArrEncodeAsBytes, 0, bArr, bytes.length, bArrEncodeAsBytes.length);
        return bArr;
    }

    @Override
    public String getDialogId(boolean z) {
        CallID callID = (CallID) getCallId();
        From from = (From) getFrom();
        To to = (To) getTo();
        StringBuffer stringBuffer = new StringBuffer(callID.getCallId());
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
        CallID callID = (CallID) getCallId();
        From from = (From) getFrom();
        StringBuffer stringBuffer = new StringBuffer(callID.getCallId());
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

    private final void setBranch(Via via, String str) {
        String branch;
        if (str.equals("ACK")) {
            if (this.statusLine.getStatusCode() >= 300) {
                branch = getTopmostVia().getBranch();
            } else {
                branch = Utils.getInstance().generateBranchId();
            }
        } else if (str.equals(Request.CANCEL)) {
            branch = getTopmostVia().getBranch();
        } else {
            return;
        }
        try {
            via.setBranch(branch);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getFirstLine() {
        if (this.statusLine == null) {
            return null;
        }
        return this.statusLine.encode();
    }

    @Override
    public void setSIPVersion(String str) {
        this.statusLine.setSipVersion(str);
    }

    @Override
    public String getSIPVersion() {
        return this.statusLine.getSipVersion();
    }

    @Override
    public String toString() {
        if (this.statusLine == null) {
            return "";
        }
        return this.statusLine.encode() + super.encode();
    }

    public SIPRequest createRequest(SipUri sipUri, Via via, CSeq cSeq, From from, To to) {
        boolean z;
        SIPRequest sIPRequest = new SIPRequest();
        String method = cSeq.getMethod();
        sIPRequest.setMethod(method);
        sIPRequest.setRequestURI(sipUri);
        setBranch(via, method);
        sIPRequest.setHeader(via);
        sIPRequest.setHeader(cSeq);
        Iterator<SIPHeader> headers = getHeaders();
        while (headers.hasNext()) {
            SIPHeader next = headers.next();
            if (!SIPMessage.isResponseHeader(next) && !(next instanceof ViaList) && !(next instanceof CSeq) && !(next instanceof ContentType) && !((z = next instanceof ContentLength)) && !(next instanceof RecordRouteList) && !(next instanceof RequireList) && !(next instanceof ContactList) && !z && !(next instanceof ServerHeader) && !(next instanceof ReasonHeader) && !(next instanceof SessionExpires) && !(next instanceof ReasonList)) {
                if (!(next instanceof To)) {
                    if (next instanceof From) {
                        next = from;
                    }
                } else {
                    next = to;
                }
                try {
                    sIPRequest.attachHeader(next, false);
                } catch (SIPDuplicateHeaderException e) {
                    e.printStackTrace();
                }
            }
        }
        try {
            sIPRequest.attachHeader(new MaxForwards(70), false);
        } catch (Exception e2) {
        }
        if (MessageFactoryImpl.getDefaultUserAgentHeader() != null) {
            sIPRequest.setHeader(MessageFactoryImpl.getDefaultUserAgentHeader());
        }
        return sIPRequest;
    }
}
