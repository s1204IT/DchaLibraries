package gov.nist.javax.sip.message;

import gov.nist.core.InternalErrorHandler;
import gov.nist.core.Separators;
import gov.nist.javax.sip.SIPConstants;
import gov.nist.javax.sip.Utils;
import gov.nist.javax.sip.header.AlertInfo;
import gov.nist.javax.sip.header.Authorization;
import gov.nist.javax.sip.header.CSeq;
import gov.nist.javax.sip.header.CallID;
import gov.nist.javax.sip.header.Contact;
import gov.nist.javax.sip.header.ContactList;
import gov.nist.javax.sip.header.ContentLength;
import gov.nist.javax.sip.header.ContentType;
import gov.nist.javax.sip.header.ErrorInfo;
import gov.nist.javax.sip.header.ErrorInfoList;
import gov.nist.javax.sip.header.From;
import gov.nist.javax.sip.header.InReplyTo;
import gov.nist.javax.sip.header.MaxForwards;
import gov.nist.javax.sip.header.Priority;
import gov.nist.javax.sip.header.ProxyAuthenticate;
import gov.nist.javax.sip.header.ProxyAuthorization;
import gov.nist.javax.sip.header.ProxyRequire;
import gov.nist.javax.sip.header.ProxyRequireList;
import gov.nist.javax.sip.header.RSeq;
import gov.nist.javax.sip.header.RecordRouteList;
import gov.nist.javax.sip.header.RetryAfter;
import gov.nist.javax.sip.header.Route;
import gov.nist.javax.sip.header.RouteList;
import gov.nist.javax.sip.header.SIPETag;
import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.SIPHeaderList;
import gov.nist.javax.sip.header.SIPHeaderNamesCache;
import gov.nist.javax.sip.header.SIPIfMatch;
import gov.nist.javax.sip.header.Server;
import gov.nist.javax.sip.header.Subject;
import gov.nist.javax.sip.header.To;
import gov.nist.javax.sip.header.Unsupported;
import gov.nist.javax.sip.header.UserAgent;
import gov.nist.javax.sip.header.Via;
import gov.nist.javax.sip.header.ViaList;
import gov.nist.javax.sip.header.WWWAuthenticate;
import gov.nist.javax.sip.header.Warning;
import gov.nist.javax.sip.parser.HeaderParser;
import gov.nist.javax.sip.parser.ParserFactory;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContentDispositionHeader;
import javax.sip.header.ContentEncodingHeader;
import javax.sip.header.ContentLanguageHeader;
import javax.sip.header.ContentLengthHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.Header;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.RecordRouteHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.Message;
import javax.sip.message.Request;

public abstract class SIPMessage extends MessageObject implements Message, MessageExt {
    protected Object applicationData;
    protected CSeq cSeqHeader;
    protected CallID callIdHeader;
    protected ContentLength contentLengthHeader;
    protected From fromHeader;
    protected MaxForwards maxForwardsHeader;
    private String messageContent;
    private byte[] messageContentBytes;
    private Object messageContentObject;
    protected boolean nullRequest;
    protected int size;
    protected To toHeader;
    private static final String CONTENT_TYPE_LOWERCASE = SIPHeaderNamesCache.toLowerCase("Content-Type");
    private static final String ERROR_LOWERCASE = SIPHeaderNamesCache.toLowerCase("Error-Info");
    private static final String CONTACT_LOWERCASE = SIPHeaderNamesCache.toLowerCase("Contact");
    private static final String VIA_LOWERCASE = SIPHeaderNamesCache.toLowerCase("Via");
    private static final String AUTHORIZATION_LOWERCASE = SIPHeaderNamesCache.toLowerCase("Authorization");
    private static final String ROUTE_LOWERCASE = SIPHeaderNamesCache.toLowerCase("Route");
    private static final String RECORDROUTE_LOWERCASE = SIPHeaderNamesCache.toLowerCase("Record-Route");
    private static final String CONTENT_DISPOSITION_LOWERCASE = SIPHeaderNamesCache.toLowerCase("Content-Disposition");
    private static final String CONTENT_ENCODING_LOWERCASE = SIPHeaderNamesCache.toLowerCase("Content-Encoding");
    private static final String CONTENT_LANGUAGE_LOWERCASE = SIPHeaderNamesCache.toLowerCase("Content-Language");
    private static final String EXPIRES_LOWERCASE = SIPHeaderNamesCache.toLowerCase("Expires");
    private String contentEncodingCharset = MessageFactoryImpl.getDefaultContentEncodingCharset();
    protected LinkedList<String> unrecognizedHeaders = new LinkedList<>();
    protected ConcurrentLinkedQueue<SIPHeader> headers = new ConcurrentLinkedQueue<>();
    private Hashtable<String, SIPHeader> nameTable = new Hashtable<>();

    public abstract String encodeMessage();

    public abstract String getDialogId(boolean z);

    @Override
    public abstract String getFirstLine();

    @Override
    public abstract String getSIPVersion();

    @Override
    public abstract void setSIPVersion(String str) throws ParseException;

    @Override
    public abstract String toString();

    public static boolean isRequestHeader(SIPHeader sipHeader) {
        return (sipHeader instanceof AlertInfo) || (sipHeader instanceof InReplyTo) || (sipHeader instanceof Authorization) || (sipHeader instanceof MaxForwards) || (sipHeader instanceof UserAgent) || (sipHeader instanceof Priority) || (sipHeader instanceof ProxyAuthorization) || (sipHeader instanceof ProxyRequire) || (sipHeader instanceof ProxyRequireList) || (sipHeader instanceof Route) || (sipHeader instanceof RouteList) || (sipHeader instanceof Subject) || (sipHeader instanceof SIPIfMatch);
    }

    public static boolean isResponseHeader(SIPHeader sipHeader) {
        return (sipHeader instanceof ErrorInfo) || (sipHeader instanceof ProxyAuthenticate) || (sipHeader instanceof Server) || (sipHeader instanceof Unsupported) || (sipHeader instanceof RetryAfter) || (sipHeader instanceof Warning) || (sipHeader instanceof WWWAuthenticate) || (sipHeader instanceof SIPETag) || (sipHeader instanceof RSeq);
    }

    public LinkedList<String> getMessageAsEncodedStrings() {
        LinkedList<String> retval = new LinkedList<>();
        for (SIPHeader sipHeader : this.headers) {
            if (sipHeader instanceof SIPHeaderList) {
                SIPHeaderList<?> shl = (SIPHeaderList) sipHeader;
                retval.addAll(shl.getHeadersAsEncodedStrings());
            } else {
                retval.add(sipHeader.encode());
            }
        }
        return retval;
    }

    protected String encodeSIPHeaders() {
        StringBuffer encoding = new StringBuffer();
        for (SIPHeader siphdr : this.headers) {
            if (!(siphdr instanceof ContentLength)) {
                siphdr.encode(encoding);
            }
        }
        return this.contentLengthHeader.encode(encoding).append(Separators.NEWLINE).toString();
    }

    @Override
    public boolean match(Object other) {
        if (other == null) {
            return true;
        }
        if (!other.getClass().equals(getClass())) {
            return false;
        }
        SIPMessage matchObj = (SIPMessage) other;
        Iterator<SIPHeader> li = matchObj.getHeaders();
        while (li.hasNext()) {
            SIPHeader hisHeaders = li.next();
            List<SIPHeader> myHeaders = getHeaderList(hisHeaders.getHeaderName());
            if (myHeaders == null || myHeaders.size() == 0) {
                return false;
            }
            if (hisHeaders instanceof SIPHeaderList) {
                ListIterator<?> outerIterator = ((SIPHeaderList) hisHeaders).listIterator();
                while (outerIterator.hasNext()) {
                    SIPHeader hisHeader = (SIPHeader) outerIterator.next();
                    if (!(hisHeader instanceof ContentLength)) {
                        ListIterator<?> innerIterator = myHeaders.listIterator();
                        boolean found = false;
                        while (true) {
                            if (!innerIterator.hasNext()) {
                                break;
                            }
                            SIPHeader myHeader = innerIterator.next();
                            if (myHeader.match(hisHeader)) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            return false;
                        }
                    }
                }
            } else {
                ListIterator<SIPHeader> innerIterator2 = myHeaders.listIterator();
                boolean found2 = false;
                while (true) {
                    if (!innerIterator2.hasNext()) {
                        break;
                    }
                    SIPHeader myHeader2 = innerIterator2.next();
                    if (myHeader2.match(hisHeaders)) {
                        found2 = true;
                        break;
                    }
                }
                if (!found2) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void merge(Object template) {
        if (!template.getClass().equals(getClass())) {
            throw new IllegalArgumentException("Bad class " + template.getClass());
        }
        SIPMessage templateMessage = (SIPMessage) template;
        Object[] templateHeaders = templateMessage.headers.toArray();
        for (Object obj : templateHeaders) {
            SIPHeader hdr = (SIPHeader) obj;
            String hdrName = hdr.getHeaderName();
            List<SIPHeader> myHdrs = getHeaderList(hdrName);
            if (myHdrs == null) {
                attachHeader(hdr);
            } else {
                ListIterator<SIPHeader> it = myHdrs.listIterator();
                while (it.hasNext()) {
                    SIPHeader sipHdr = it.next();
                    sipHdr.merge(hdr);
                }
            }
        }
    }

    @Override
    public String encode() {
        StringBuffer encoding = new StringBuffer();
        for (SIPHeader siphdr : this.headers) {
            if (!(siphdr instanceof ContentLength)) {
                encoding.append(siphdr.encode());
            }
        }
        for (String unrecognized : this.unrecognizedHeaders) {
            encoding.append(unrecognized).append(Separators.NEWLINE);
        }
        encoding.append(this.contentLengthHeader.encode()).append(Separators.NEWLINE);
        if (this.messageContentObject != null) {
            String mbody = getContent().toString();
            encoding.append(mbody);
        } else if (this.messageContent != null || this.messageContentBytes != null) {
            String content = null;
            try {
                if (this.messageContent != null) {
                    content = this.messageContent;
                } else {
                    String content2 = new String(this.messageContentBytes, getCharset());
                    content = content2;
                }
            } catch (UnsupportedEncodingException ex) {
                InternalErrorHandler.handleException(ex);
            }
            encoding.append(content);
        }
        return encoding.toString();
    }

    public byte[] encodeAsBytes(String transport) {
        if ((this instanceof SIPRequest) && ((SIPRequest) this).isNullRequest()) {
            return "\r\n\r\n".getBytes();
        }
        ViaHeader topVia = (ViaHeader) getHeader("Via");
        try {
            topVia.setTransport(transport);
        } catch (ParseException e) {
            InternalErrorHandler.handleException(e);
        }
        StringBuffer encoding = new StringBuffer();
        synchronized (this.headers) {
            for (SIPHeader siphdr : this.headers) {
                if (!(siphdr instanceof ContentLength)) {
                    siphdr.encode(encoding);
                }
            }
        }
        this.contentLengthHeader.encode(encoding);
        encoding.append(Separators.NEWLINE);
        byte[] content = getRawContent();
        if (content != null) {
            byte[] msgarray = null;
            try {
                msgarray = encoding.toString().getBytes(getCharset());
            } catch (UnsupportedEncodingException ex) {
                InternalErrorHandler.handleException(ex);
            }
            byte[] retval = new byte[msgarray.length + content.length];
            System.arraycopy(msgarray, 0, retval, 0, msgarray.length);
            System.arraycopy(content, 0, retval, msgarray.length, content.length);
            return retval;
        }
        try {
            return encoding.toString().getBytes(getCharset());
        } catch (UnsupportedEncodingException ex2) {
            InternalErrorHandler.handleException(ex2);
            return null;
        }
    }

    @Override
    public Object clone() {
        SIPMessage retval = (SIPMessage) super.clone();
        retval.nameTable = new Hashtable<>();
        retval.fromHeader = null;
        retval.toHeader = null;
        retval.cSeqHeader = null;
        retval.callIdHeader = null;
        retval.contentLengthHeader = null;
        retval.maxForwardsHeader = null;
        if (this.headers != null) {
            retval.headers = new ConcurrentLinkedQueue<>();
            for (SIPHeader hdr : this.headers) {
                retval.attachHeader((SIPHeader) hdr.clone());
            }
        }
        if (this.messageContentBytes != null) {
            retval.messageContentBytes = (byte[]) this.messageContentBytes.clone();
        }
        if (this.messageContentObject != null) {
            retval.messageContentObject = makeClone(this.messageContentObject);
        }
        retval.unrecognizedHeaders = this.unrecognizedHeaders;
        return retval;
    }

    @Override
    public String debugDump() {
        this.stringRepresentation = "";
        sprint("SIPMessage:");
        sprint("{");
        try {
            Field[] fields = getClass().getDeclaredFields();
            for (Field f : fields) {
                Class<?> fieldType = f.getType();
                String fieldName = f.getName();
                if (f.get(this) != null && SIPHeader.class.isAssignableFrom(fieldType) && fieldName.compareTo("headers") != 0) {
                    sprint(fieldName + Separators.EQUALS);
                    sprint(((SIPHeader) f.get(this)).debugDump());
                }
            }
        } catch (Exception ex) {
            InternalErrorHandler.handleException(ex);
        }
        sprint("List of headers : ");
        sprint(this.headers.toString());
        sprint("messageContent = ");
        sprint("{");
        sprint(this.messageContent);
        sprint("}");
        if (getContent() != null) {
            sprint(getContent().toString());
        }
        sprint("}");
        return this.stringRepresentation;
    }

    public SIPMessage() {
        try {
            attachHeader(new ContentLength(0), false);
        } catch (Exception e) {
        }
    }

    private void attachHeader(SIPHeader h) {
        if (h == null) {
            throw new IllegalArgumentException("null header!");
        }
        try {
            if (h instanceof SIPHeaderList) {
                SIPHeaderList<?> hl = (SIPHeaderList) h;
                if (hl.isEmpty()) {
                    return;
                }
            }
            attachHeader(h, false, false);
        } catch (SIPDuplicateHeaderException e) {
        }
    }

    @Override
    public void setHeader(Header sipHeader) {
        SIPHeader header = (SIPHeader) sipHeader;
        if (header == null) {
            throw new IllegalArgumentException("null header!");
        }
        try {
            if (header instanceof SIPHeaderList) {
                SIPHeaderList<?> hl = (SIPHeaderList) header;
                if (hl.isEmpty()) {
                    return;
                }
            }
            removeHeader(header.getHeaderName());
            attachHeader(header, true, false);
        } catch (SIPDuplicateHeaderException ex) {
            InternalErrorHandler.handleException(ex);
        }
    }

    public void setHeaders(List<SIPHeader> headers) {
        ListIterator<SIPHeader> listIterator = headers.listIterator();
        while (listIterator.hasNext()) {
            SIPHeader sipHeader = listIterator.next();
            try {
                attachHeader(sipHeader, false);
            } catch (SIPDuplicateHeaderException e) {
            }
        }
    }

    public void attachHeader(SIPHeader h, boolean replaceflag) throws SIPDuplicateHeaderException {
        attachHeader(h, replaceflag, false);
    }

    public void attachHeader(SIPHeader header, boolean replaceFlag, boolean top) throws SIPDuplicateHeaderException {
        SIPHeader h;
        SIPHeaderList<?> hdrlist;
        if (header == null) {
            throw new NullPointerException("null header");
        }
        if (ListMap.hasList(header) && !SIPHeaderList.class.isAssignableFrom(header.getClass())) {
            SIPHeaderList<SIPHeader> hdrList = ListMap.getList(header);
            hdrList.add(header);
            h = hdrList;
        } else {
            h = header;
        }
        String headerNameLowerCase = SIPHeaderNamesCache.toLowerCase(h.getName());
        if (replaceFlag) {
            this.nameTable.remove(headerNameLowerCase);
        } else if (this.nameTable.containsKey(headerNameLowerCase) && !(h instanceof SIPHeaderList)) {
            if (h instanceof ContentLength) {
                try {
                    ContentLength cl = (ContentLength) h;
                    this.contentLengthHeader.setContentLength(cl.getContentLength());
                    return;
                } catch (InvalidArgumentException e) {
                    return;
                }
            }
            return;
        }
        SIPHeader originalHeader = (SIPHeader) getHeader(header.getName());
        if (originalHeader != null) {
            Iterator<SIPHeader> li = this.headers.iterator();
            while (li.hasNext()) {
                SIPHeader next = li.next();
                if (next.equals(originalHeader)) {
                    li.remove();
                }
            }
        }
        if (!this.nameTable.containsKey(headerNameLowerCase)) {
            this.nameTable.put(headerNameLowerCase, h);
            this.headers.add(h);
        } else if ((h instanceof SIPHeaderList) && (hdrlist = (SIPHeaderList) this.nameTable.get(headerNameLowerCase)) != null) {
            hdrlist.concatenate((SIPHeaderList) h, top);
        } else {
            this.nameTable.put(headerNameLowerCase, h);
        }
        if (h instanceof From) {
            this.fromHeader = (From) h;
            return;
        }
        if (h instanceof ContentLength) {
            this.contentLengthHeader = (ContentLength) h;
            return;
        }
        if (h instanceof To) {
            this.toHeader = (To) h;
            return;
        }
        if (h instanceof CSeq) {
            this.cSeqHeader = (CSeq) h;
        } else if (h instanceof CallID) {
            this.callIdHeader = (CallID) h;
        } else if (h instanceof MaxForwards) {
            this.maxForwardsHeader = (MaxForwards) h;
        }
    }

    public void removeHeader(String headerName, boolean top) {
        String headerNameLowerCase = SIPHeaderNamesCache.toLowerCase(headerName);
        SIPHeader toRemove = this.nameTable.get(headerNameLowerCase);
        if (toRemove != null) {
            if (toRemove instanceof SIPHeaderList) {
                SIPHeaderList<?> hdrList = (SIPHeaderList) toRemove;
                if (top) {
                    hdrList.removeFirst();
                } else {
                    hdrList.removeLast();
                }
                if (hdrList.isEmpty()) {
                    Iterator<SIPHeader> li = this.headers.iterator();
                    while (li.hasNext()) {
                        SIPHeader sipHeader = li.next();
                        if (sipHeader.getName().equalsIgnoreCase(headerNameLowerCase)) {
                            li.remove();
                        }
                    }
                    this.nameTable.remove(headerNameLowerCase);
                    return;
                }
                return;
            }
            this.nameTable.remove(headerNameLowerCase);
            if (toRemove instanceof From) {
                this.fromHeader = null;
            } else if (toRemove instanceof To) {
                this.toHeader = null;
            } else if (toRemove instanceof CSeq) {
                this.cSeqHeader = null;
            } else if (toRemove instanceof CallID) {
                this.callIdHeader = null;
            } else if (toRemove instanceof MaxForwards) {
                this.maxForwardsHeader = null;
            } else if (toRemove instanceof ContentLength) {
                this.contentLengthHeader = null;
            }
            Iterator<SIPHeader> li2 = this.headers.iterator();
            while (li2.hasNext()) {
                SIPHeader sipHeader2 = li2.next();
                if (sipHeader2.getName().equalsIgnoreCase(headerName)) {
                    li2.remove();
                }
            }
        }
    }

    @Override
    public void removeHeader(String headerName) {
        if (headerName == null) {
            throw new NullPointerException("null arg");
        }
        String headerNameLowerCase = SIPHeaderNamesCache.toLowerCase(headerName);
        SIPHeader removed = this.nameTable.remove(headerNameLowerCase);
        if (removed != null) {
            if (removed instanceof From) {
                this.fromHeader = null;
            } else if (removed instanceof To) {
                this.toHeader = null;
            } else if (removed instanceof CSeq) {
                this.cSeqHeader = null;
            } else if (removed instanceof CallID) {
                this.callIdHeader = null;
            } else if (removed instanceof MaxForwards) {
                this.maxForwardsHeader = null;
            } else if (removed instanceof ContentLength) {
                this.contentLengthHeader = null;
            }
            Iterator<SIPHeader> li = this.headers.iterator();
            while (li.hasNext()) {
                SIPHeader sipHeader = li.next();
                if (sipHeader.getName().equalsIgnoreCase(headerNameLowerCase)) {
                    li.remove();
                }
            }
        }
    }

    public String getTransactionId() {
        Via topVia = null;
        if (!getViaHeaders().isEmpty()) {
            topVia = (Via) getViaHeaders().getFirst();
        }
        if (topVia != null && topVia.getBranch() != null && topVia.getBranch().toUpperCase().startsWith(SIPConstants.BRANCH_MAGIC_COOKIE_UPPER_CASE)) {
            if (getCSeq().getMethod().equals(Request.CANCEL)) {
                return (topVia.getBranch() + Separators.COLON + getCSeq().getMethod()).toLowerCase();
            }
            return topVia.getBranch().toLowerCase();
        }
        StringBuffer retval = new StringBuffer();
        From from = (From) getFrom();
        if (from.hasTag()) {
            retval.append(from.getTag()).append("-");
        }
        String cid = this.callIdHeader.getCallId();
        retval.append(cid).append("-");
        retval.append(this.cSeqHeader.getSequenceNumber()).append("-").append(this.cSeqHeader.getMethod());
        if (topVia != null) {
            retval.append("-").append(topVia.getSentBy().encode());
            if (!topVia.getSentBy().hasPort()) {
                retval.append("-").append(5060);
            }
        }
        if (getCSeq().getMethod().equals(Request.CANCEL)) {
            retval.append(Request.CANCEL);
        }
        return retval.toString().toLowerCase().replace(Separators.COLON, "-").replace(Separators.AT, "-") + Utils.getSignature();
    }

    @Override
    public int hashCode() {
        if (this.callIdHeader == null) {
            throw new RuntimeException("Invalid message! Cannot compute hashcode! call-id header is missing !");
        }
        return this.callIdHeader.getCallId().hashCode();
    }

    public boolean hasContent() {
        return (this.messageContent == null && this.messageContentBytes == null) ? false : true;
    }

    public Iterator<SIPHeader> getHeaders() {
        return this.headers.iterator();
    }

    @Override
    public Header getHeader(String headerName) {
        return getHeaderLowerCase(SIPHeaderNamesCache.toLowerCase(headerName));
    }

    private Header getHeaderLowerCase(String lowerCaseHeaderName) {
        if (lowerCaseHeaderName == null) {
            throw new NullPointerException("bad name");
        }
        SIPHeader sipHeader = this.nameTable.get(lowerCaseHeaderName);
        if (sipHeader instanceof SIPHeaderList) {
            return ((SIPHeaderList) sipHeader).getFirst();
        }
        return sipHeader;
    }

    @Override
    public ContentType getContentTypeHeader() {
        return (ContentType) getHeaderLowerCase(CONTENT_TYPE_LOWERCASE);
    }

    @Override
    public ContentLengthHeader getContentLengthHeader() {
        return getContentLength();
    }

    public FromHeader getFrom() {
        return this.fromHeader;
    }

    public ErrorInfoList getErrorInfoHeaders() {
        return (ErrorInfoList) getSIPHeaderListLowerCase(ERROR_LOWERCASE);
    }

    public ContactList getContactHeaders() {
        return (ContactList) getSIPHeaderListLowerCase(CONTACT_LOWERCASE);
    }

    public Contact getContactHeader() {
        ContactList clist = getContactHeaders();
        if (clist != null) {
            return (Contact) clist.getFirst();
        }
        return null;
    }

    public ViaList getViaHeaders() {
        return (ViaList) getSIPHeaderListLowerCase(VIA_LOWERCASE);
    }

    public void setVia(List viaList) {
        ViaList vList = new ViaList();
        ListIterator it = viaList.listIterator();
        while (it.hasNext()) {
            Via via = (Via) it.next();
            vList.add(via);
        }
        setHeader((SIPHeaderList<Via>) vList);
    }

    public void setHeader(SIPHeaderList<Via> sipHeaderList) {
        setHeader((Header) sipHeaderList);
    }

    public Via getTopmostVia() {
        if (getViaHeaders() == null) {
            return null;
        }
        return (Via) getViaHeaders().getFirst();
    }

    public CSeqHeader getCSeq() {
        return this.cSeqHeader;
    }

    public Authorization getAuthorization() {
        return (Authorization) getHeaderLowerCase(AUTHORIZATION_LOWERCASE);
    }

    public MaxForwardsHeader getMaxForwards() {
        return this.maxForwardsHeader;
    }

    public void setMaxForwards(MaxForwardsHeader maxForwards) {
        setHeader(maxForwards);
    }

    public RouteList getRouteHeaders() {
        return (RouteList) getSIPHeaderListLowerCase(ROUTE_LOWERCASE);
    }

    public CallIdHeader getCallId() {
        return this.callIdHeader;
    }

    public void setCallId(CallIdHeader callId) {
        setHeader(callId);
    }

    public void setCallId(String callId) throws ParseException {
        if (this.callIdHeader == null) {
            setHeader(new CallID());
        }
        this.callIdHeader.setCallId(callId);
    }

    public RecordRouteList getRecordRouteHeaders() {
        return (RecordRouteList) getSIPHeaderListLowerCase(RECORDROUTE_LOWERCASE);
    }

    public ToHeader getTo() {
        return this.toHeader;
    }

    public void setTo(ToHeader to) {
        setHeader(to);
    }

    public void setFrom(FromHeader from) {
        setHeader(from);
    }

    @Override
    public ContentLengthHeader getContentLength() {
        return this.contentLengthHeader;
    }

    public String getMessageContent() throws UnsupportedEncodingException {
        if (this.messageContent == null && this.messageContentBytes == null) {
            return null;
        }
        if (this.messageContent == null) {
            this.messageContent = new String(this.messageContentBytes, getCharset());
        }
        return this.messageContent;
    }

    @Override
    public byte[] getRawContent() {
        try {
            if (this.messageContentBytes == null) {
                if (this.messageContentObject != null) {
                    String messageContent = this.messageContentObject.toString();
                    this.messageContentBytes = messageContent.getBytes(getCharset());
                } else if (this.messageContent != null) {
                    this.messageContentBytes = this.messageContent.getBytes(getCharset());
                }
            }
            return this.messageContentBytes;
        } catch (UnsupportedEncodingException ex) {
            InternalErrorHandler.handleException(ex);
            return null;
        }
    }

    public void setMessageContent(String type, String subType, String messageContent) {
        if (messageContent == null) {
            throw new IllegalArgumentException("messgeContent is null");
        }
        ContentType ct = new ContentType(type, subType);
        setHeader(ct);
        this.messageContent = messageContent;
        this.messageContentBytes = null;
        this.messageContentObject = null;
        computeContentLength(messageContent);
    }

    @Override
    public void setContent(Object content, ContentTypeHeader contentTypeHeader) throws ParseException {
        if (content == null) {
            throw new NullPointerException("null content");
        }
        setHeader(contentTypeHeader);
        this.messageContent = null;
        this.messageContentBytes = null;
        this.messageContentObject = null;
        if (content instanceof String) {
            this.messageContent = (String) content;
        } else if (content instanceof byte[]) {
            this.messageContentBytes = (byte[]) content;
        } else {
            this.messageContentObject = content;
        }
        computeContentLength(content);
    }

    @Override
    public Object getContent() {
        if (this.messageContentObject != null) {
            return this.messageContentObject;
        }
        if (this.messageContent != null) {
            return this.messageContent;
        }
        if (this.messageContentBytes != null) {
            return this.messageContentBytes;
        }
        return null;
    }

    public void setMessageContent(String type, String subType, byte[] messageContent) {
        ContentType ct = new ContentType(type, subType);
        setHeader(ct);
        setMessageContent(messageContent);
        computeContentLength(messageContent);
    }

    public void setMessageContent(String content, boolean strict, boolean computeContentLength, int givenLength) throws ParseException {
        computeContentLength(content);
        if (!computeContentLength && ((!strict && this.contentLengthHeader.getContentLength() != givenLength) || this.contentLengthHeader.getContentLength() < givenLength)) {
            throw new ParseException("Invalid content length " + this.contentLengthHeader.getContentLength() + " / " + givenLength, 0);
        }
        this.messageContent = content;
        this.messageContentBytes = null;
        this.messageContentObject = null;
    }

    public void setMessageContent(byte[] content) {
        computeContentLength(content);
        this.messageContentBytes = content;
        this.messageContent = null;
        this.messageContentObject = null;
    }

    public void setMessageContent(byte[] content, boolean computeContentLength, int givenLength) throws ParseException {
        computeContentLength(content);
        if (!computeContentLength && this.contentLengthHeader.getContentLength() < givenLength) {
            throw new ParseException("Invalid content length " + this.contentLengthHeader.getContentLength() + " / " + givenLength, 0);
        }
        this.messageContentBytes = content;
        this.messageContent = null;
        this.messageContentObject = null;
    }

    private void computeContentLength(Object content) {
        int length = 0;
        if (content != null) {
            if (content instanceof String) {
                try {
                    length = ((String) content).getBytes(getCharset()).length;
                } catch (UnsupportedEncodingException ex) {
                    InternalErrorHandler.handleException(ex);
                }
            } else {
                length = content instanceof byte[] ? ((byte[]) content).length : content.toString().length();
            }
        }
        try {
            this.contentLengthHeader.setContentLength(length);
        } catch (InvalidArgumentException e) {
        }
    }

    @Override
    public void removeContent() {
        this.messageContent = null;
        this.messageContentBytes = null;
        this.messageContentObject = null;
        try {
            this.contentLengthHeader.setContentLength(0);
        } catch (InvalidArgumentException e) {
        }
    }

    @Override
    public ListIterator<SIPHeader> getHeaders(String headerName) {
        if (headerName == null) {
            throw new NullPointerException("null headerName");
        }
        SIPHeader sipHeader = this.nameTable.get(SIPHeaderNamesCache.toLowerCase(headerName));
        if (sipHeader == null) {
            return new LinkedList().listIterator();
        }
        if (sipHeader instanceof SIPHeaderList) {
            return ((SIPHeaderList) sipHeader).listIterator();
        }
        return new HeaderIterator(this, sipHeader);
    }

    public String getHeaderAsFormattedString(String name) {
        String lowerCaseName = name.toLowerCase();
        return this.nameTable.containsKey(lowerCaseName) ? this.nameTable.get(lowerCaseName).toString() : getHeader(name).toString();
    }

    private SIPHeader getSIPHeaderListLowerCase(String lowerCaseHeaderName) {
        return this.nameTable.get(lowerCaseHeaderName);
    }

    private List<SIPHeader> getHeaderList(String headerName) {
        SIPHeader sipHeader = this.nameTable.get(SIPHeaderNamesCache.toLowerCase(headerName));
        if (sipHeader == null) {
            return null;
        }
        if (sipHeader instanceof SIPHeaderList) {
            return ((SIPHeaderList) sipHeader).getHeaderList();
        }
        LinkedList<SIPHeader> ll = new LinkedList<>();
        ll.add(sipHeader);
        return ll;
    }

    public boolean hasHeader(String headerName) {
        return this.nameTable.containsKey(SIPHeaderNamesCache.toLowerCase(headerName));
    }

    public boolean hasFromTag() {
        return (this.fromHeader == null || this.fromHeader.getTag() == null) ? false : true;
    }

    public boolean hasToTag() {
        return (this.toHeader == null || this.toHeader.getTag() == null) ? false : true;
    }

    public String getFromTag() {
        if (this.fromHeader == null) {
            return null;
        }
        return this.fromHeader.getTag();
    }

    public void setFromTag(String tag) {
        try {
            this.fromHeader.setTag(tag);
        } catch (ParseException e) {
        }
    }

    public void setToTag(String tag) {
        try {
            this.toHeader.setTag(tag);
        } catch (ParseException e) {
        }
    }

    public String getToTag() {
        if (this.toHeader == null) {
            return null;
        }
        return this.toHeader.getTag();
    }

    @Override
    public void addHeader(Header sipHeader) {
        SIPHeader sh = (SIPHeader) sipHeader;
        try {
            if ((sipHeader instanceof ViaHeader) || (sipHeader instanceof RecordRouteHeader)) {
                attachHeader(sh, false, true);
            } else {
                attachHeader(sh, false, false);
            }
        } catch (SIPDuplicateHeaderException e) {
            try {
                if (sipHeader instanceof ContentLength) {
                    ContentLength cl = (ContentLength) sipHeader;
                    this.contentLengthHeader.setContentLength(cl.getContentLength());
                }
            } catch (InvalidArgumentException e2) {
            }
        }
    }

    public void addUnparsed(String unparsed) {
        this.unrecognizedHeaders.add(unparsed);
    }

    public void addHeader(String sipHeader) {
        String hdrString = sipHeader.trim() + Separators.RETURN;
        try {
            HeaderParser parser = ParserFactory.createParser(sipHeader);
            SIPHeader sh = parser.parse();
            attachHeader(sh, false);
        } catch (ParseException e) {
            this.unrecognizedHeaders.add(hdrString);
        }
    }

    @Override
    public ListIterator<String> getUnrecognizedHeaders() {
        return this.unrecognizedHeaders.listIterator();
    }

    @Override
    public ListIterator<String> getHeaderNames() {
        LinkedList<String> retval = new LinkedList<>();
        for (SIPHeader sipHeader : this.headers) {
            String name = sipHeader.getName();
            retval.add(name);
        }
        return retval.listIterator();
    }

    @Override
    public boolean equals(Object other) {
        if (!other.getClass().equals(getClass())) {
            return false;
        }
        SIPMessage otherMessage = (SIPMessage) other;
        Collection<SIPHeader> values = this.nameTable.values();
        if (this.nameTable.size() != otherMessage.nameTable.size()) {
            return false;
        }
        for (SIPHeader mine : values) {
            SIPHeader his = otherMessage.nameTable.get(SIPHeaderNamesCache.toLowerCase(mine.getName()));
            if (his == null || !his.equals(mine)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ContentDispositionHeader getContentDisposition() {
        return (ContentDispositionHeader) getHeaderLowerCase(CONTENT_DISPOSITION_LOWERCASE);
    }

    @Override
    public ContentEncodingHeader getContentEncoding() {
        return (ContentEncodingHeader) getHeaderLowerCase(CONTENT_ENCODING_LOWERCASE);
    }

    @Override
    public ContentLanguageHeader getContentLanguage() {
        return (ContentLanguageHeader) getHeaderLowerCase(CONTENT_LANGUAGE_LOWERCASE);
    }

    @Override
    public ExpiresHeader getExpires() {
        return (ExpiresHeader) getHeaderLowerCase(EXPIRES_LOWERCASE);
    }

    @Override
    public void setExpires(ExpiresHeader expiresHeader) {
        setHeader(expiresHeader);
    }

    @Override
    public void setContentDisposition(ContentDispositionHeader contentDispositionHeader) {
        setHeader(contentDispositionHeader);
    }

    @Override
    public void setContentEncoding(ContentEncodingHeader contentEncodingHeader) {
        setHeader(contentEncodingHeader);
    }

    @Override
    public void setContentLanguage(ContentLanguageHeader contentLanguageHeader) {
        setHeader(contentLanguageHeader);
    }

    @Override
    public void setContentLength(ContentLengthHeader contentLength) {
        try {
            this.contentLengthHeader.setContentLength(contentLength.getContentLength());
        } catch (InvalidArgumentException e) {
        }
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getSize() {
        return this.size;
    }

    @Override
    public void addLast(Header header) throws SipException, NullPointerException {
        if (header == null) {
            throw new NullPointerException("null arg!");
        }
        try {
            attachHeader((SIPHeader) header, false, false);
        } catch (SIPDuplicateHeaderException e) {
            throw new SipException("Cannot add header - header already exists");
        }
    }

    @Override
    public void addFirst(Header header) throws SipException, NullPointerException {
        if (header == null) {
            throw new NullPointerException("null arg!");
        }
        try {
            attachHeader((SIPHeader) header, false, true);
        } catch (SIPDuplicateHeaderException e) {
            throw new SipException("Cannot add header - header already exists");
        }
    }

    @Override
    public void removeFirst(String headerName) throws NullPointerException {
        if (headerName == null) {
            throw new NullPointerException("Null argument Provided!");
        }
        removeHeader(headerName, true);
    }

    @Override
    public void removeLast(String headerName) {
        if (headerName == null) {
            throw new NullPointerException("Null argument Provided!");
        }
        removeHeader(headerName, false);
    }

    public void setCSeq(CSeqHeader cseqHeader) {
        setHeader(cseqHeader);
    }

    @Override
    public void setApplicationData(Object applicationData) {
        this.applicationData = applicationData;
    }

    @Override
    public Object getApplicationData() {
        return this.applicationData;
    }

    @Override
    public MultipartMimeContent getMultipartMimeContent() throws ParseException {
        if (this.contentLengthHeader.getContentLength() == 0) {
            return null;
        }
        MultipartMimeContentImpl retval = new MultipartMimeContentImpl(getContentTypeHeader());
        byte[] rawContent = getRawContent();
        try {
            String body = new String(rawContent, getCharset());
            retval.createContentList(body);
            return retval;
        } catch (UnsupportedEncodingException e) {
            InternalErrorHandler.handleException(e);
            return null;
        }
    }

    @Override
    public CallIdHeader getCallIdHeader() {
        return this.callIdHeader;
    }

    @Override
    public FromHeader getFromHeader() {
        return this.fromHeader;
    }

    @Override
    public ToHeader getToHeader() {
        return this.toHeader;
    }

    @Override
    public ViaHeader getTopmostViaHeader() {
        return getTopmostVia();
    }

    @Override
    public CSeqHeader getCSeqHeader() {
        return this.cSeqHeader;
    }

    protected final String getCharset() {
        ContentType ct = getContentTypeHeader();
        if (ct != null) {
            String c = ct.getCharset();
            if (c == null) {
                return this.contentEncodingCharset;
            }
            return c;
        }
        return this.contentEncodingCharset;
    }

    public boolean isNullRequest() {
        return this.nullRequest;
    }

    public void setNullRequest() {
        this.nullRequest = true;
    }
}
