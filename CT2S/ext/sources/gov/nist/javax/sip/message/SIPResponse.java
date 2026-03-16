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

    public static String getReasonPhrase(int rc) {
        switch (rc) {
            case 100:
                return "Trying";
            case Response.RINGING:
                return "Ringing";
            case Response.CALL_IS_BEING_FORWARDED:
                return "Call is being forwarded";
            case Response.QUEUED:
                return "Queued";
            case Response.SESSION_PROGRESS:
                return "Session progress";
            case 200:
                return "OK";
            case 202:
                return "Accepted";
            case 300:
                return "Multiple choices";
            case 301:
                return "Moved permanently";
            case 302:
                return "Moved Temporarily";
            case 305:
                return "Use proxy";
            case Response.ALTERNATIVE_SERVICE:
                return "Alternative service";
            case 400:
                return "Bad request";
            case 401:
                return "Unauthorized";
            case 402:
                return "Payment required";
            case 403:
                return "Forbidden";
            case 404:
                return "Not found";
            case 405:
                return "Method not allowed";
            case 406:
                return "Not acceptable";
            case 407:
                return "Proxy Authentication required";
            case 408:
                return "Request timeout";
            case 410:
                return "Gone";
            case 412:
                return "Conditional request failed";
            case 413:
                return "Request entity too large";
            case 414:
                return "Request-URI too large";
            case 415:
                return "Unsupported media type";
            case 416:
                return "Unsupported URI Scheme";
            case 420:
                return "Bad extension";
            case Response.EXTENSION_REQUIRED:
                return "Etension Required";
            case 423:
                return "Interval too brief";
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
            case Response.REQUEST_PENDING:
                return "Request Pending";
            case Response.UNDECIPHERABLE:
                return "Undecipherable";
            case 500:
                return "Server Internal Error";
            case 501:
                return "Not implemented";
            case 502:
                return "Bad gateway";
            case 503:
                return "Service unavailable";
            case 504:
                return "Gateway timeout";
            case 505:
                return "SIP version not supported";
            case Response.MESSAGE_TOO_LARGE:
                return "Message Too Large";
            case Response.BUSY_EVERYWHERE:
                return "Busy everywhere";
            case Response.DECLINE:
                return "Decline";
            case Response.DOES_NOT_EXIST_ANYWHERE:
                return "Does not exist anywhere";
            case Response.SESSION_NOT_ACCEPTABLE:
                return "Session Not acceptable";
            default:
                return "Unknown Status";
        }
    }

    @Override
    public void setStatusCode(int statusCode) throws ParseException {
        if (statusCode < 100 || statusCode > 699) {
            throw new ParseException("bad status code", 0);
        }
        if (this.statusLine == null) {
            this.statusLine = new StatusLine();
        }
        this.statusLine.setStatusCode(statusCode);
    }

    public StatusLine getStatusLine() {
        return this.statusLine;
    }

    @Override
    public int getStatusCode() {
        return this.statusLine.getStatusCode();
    }

    @Override
    public void setReasonPhrase(String reasonPhrase) {
        if (reasonPhrase == null) {
            throw new IllegalArgumentException("Bad reason phrase");
        }
        if (this.statusLine == null) {
            this.statusLine = new StatusLine();
        }
        this.statusLine.setReasonPhrase(reasonPhrase);
    }

    @Override
    public String getReasonPhrase() {
        return (this.statusLine == null || this.statusLine.getReasonPhrase() == null) ? "" : this.statusLine.getReasonPhrase();
    }

    public static boolean isFinalResponse(int rc) {
        return rc >= 200 && rc < 700;
    }

    public boolean isFinalResponse() {
        return isFinalResponse(this.statusLine.getStatusCode());
    }

    public void setStatusLine(StatusLine sl) {
        this.statusLine = sl;
    }

    @Override
    public String debugDump() {
        String superstring = super.debugDump();
        this.stringRepresentation = "";
        sprint(SIPResponse.class.getCanonicalName());
        sprint("{");
        if (this.statusLine != null) {
            sprint(this.statusLine.debugDump());
        }
        sprint(superstring);
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
            String retval = this.statusLine.encode() + super.encode();
            return retval;
        }
        String retval2 = super.encode();
        return retval2;
    }

    @Override
    public String encodeMessage() {
        if (this.statusLine != null) {
            String retval = this.statusLine.encode() + super.encodeSIPHeaders();
            return retval;
        }
        String retval2 = super.encodeSIPHeaders();
        return retval2;
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
        SIPResponse retval = (SIPResponse) super.clone();
        if (this.statusLine != null) {
            retval.statusLine = (StatusLine) this.statusLine.clone();
        }
        return retval;
    }

    @Override
    public boolean equals(Object other) {
        if (!getClass().equals(other.getClass())) {
            return false;
        }
        SIPResponse that = (SIPResponse) other;
        return this.statusLine.equals(that.statusLine) && super.equals(other);
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
        SIPResponse that = (SIPResponse) matchObj;
        StatusLine rline = that.statusLine;
        if (this.statusLine == null && rline != null) {
            return false;
        }
        if (this.statusLine == rline) {
            return super.match(matchObj);
        }
        return this.statusLine.match(that.statusLine) && super.match(matchObj);
    }

    @Override
    public byte[] encodeAsBytes(String transport) {
        byte[] slbytes = null;
        if (this.statusLine != null) {
            try {
                slbytes = this.statusLine.encode().getBytes("UTF-8");
            } catch (UnsupportedEncodingException ex) {
                InternalErrorHandler.handleException(ex);
            }
        }
        byte[] superbytes = super.encodeAsBytes(transport);
        byte[] retval = new byte[slbytes.length + superbytes.length];
        System.arraycopy(slbytes, 0, retval, 0, slbytes.length);
        System.arraycopy(superbytes, 0, retval, slbytes.length, superbytes.length);
        return retval;
    }

    @Override
    public String getDialogId(boolean isServer) {
        CallID cid = (CallID) getCallId();
        From from = (From) getFrom();
        To to = (To) getTo();
        StringBuffer retval = new StringBuffer(cid.getCallId());
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
        CallID cid = (CallID) getCallId();
        From from = (From) getFrom();
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

    private final void setBranch(Via via, String method) {
        String branch;
        if (method.equals("ACK")) {
            if (this.statusLine.getStatusCode() >= 300) {
                branch = getTopmostVia().getBranch();
            } else {
                branch = Utils.getInstance().generateBranchId();
            }
        } else if (method.equals(Request.CANCEL)) {
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
    public void setSIPVersion(String sipVersion) {
        this.statusLine.setSipVersion(sipVersion);
    }

    @Override
    public String getSIPVersion() {
        return this.statusLine.getSipVersion();
    }

    @Override
    public String toString() {
        return this.statusLine == null ? "" : this.statusLine.encode() + super.encode();
    }

    public SIPRequest createRequest(SipUri requestURI, Via via, CSeq cseq, From from, To to) {
        SIPRequest newRequest = new SIPRequest();
        String method = cseq.getMethod();
        newRequest.setMethod(method);
        newRequest.setRequestURI(requestURI);
        setBranch(via, method);
        newRequest.setHeader(via);
        newRequest.setHeader(cseq);
        Iterator<SIPHeader> headers = getHeaders();
        while (headers.hasNext()) {
            SIPHeader nextHeader = headers.next();
            if (!SIPMessage.isResponseHeader(nextHeader) && !(nextHeader instanceof ViaList) && !(nextHeader instanceof CSeq) && !(nextHeader instanceof ContentType) && !(nextHeader instanceof ContentLength) && !(nextHeader instanceof RecordRouteList) && !(nextHeader instanceof RequireList) && !(nextHeader instanceof ContactList) && !(nextHeader instanceof ContentLength) && !(nextHeader instanceof ServerHeader) && !(nextHeader instanceof ReasonHeader) && !(nextHeader instanceof SessionExpires) && !(nextHeader instanceof ReasonList)) {
                if (nextHeader instanceof To) {
                    nextHeader = to;
                } else if (nextHeader instanceof From) {
                    nextHeader = from;
                }
                try {
                    newRequest.attachHeader(nextHeader, false);
                } catch (SIPDuplicateHeaderException e) {
                    e.printStackTrace();
                }
            }
        }
        try {
            newRequest.attachHeader(new MaxForwards(70), false);
        } catch (Exception e2) {
        }
        if (MessageFactoryImpl.getDefaultUserAgentHeader() != null) {
            newRequest.setHeader(MessageFactoryImpl.getDefaultUserAgentHeader());
        }
        return newRequest;
    }
}
