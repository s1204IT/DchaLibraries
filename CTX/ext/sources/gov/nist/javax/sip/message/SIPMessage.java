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
import gov.nist.javax.sip.parser.ParserFactory;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.text.ParseException;
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

    public static boolean isRequestHeader(SIPHeader sIPHeader) {
        return (sIPHeader instanceof AlertInfo) || (sIPHeader instanceof InReplyTo) || (sIPHeader instanceof Authorization) || (sIPHeader instanceof MaxForwards) || (sIPHeader instanceof UserAgent) || (sIPHeader instanceof Priority) || (sIPHeader instanceof ProxyAuthorization) || (sIPHeader instanceof ProxyRequire) || (sIPHeader instanceof ProxyRequireList) || (sIPHeader instanceof Route) || (sIPHeader instanceof RouteList) || (sIPHeader instanceof Subject) || (sIPHeader instanceof SIPIfMatch);
    }

    public static boolean isResponseHeader(SIPHeader sIPHeader) {
        return (sIPHeader instanceof ErrorInfo) || (sIPHeader instanceof ProxyAuthenticate) || (sIPHeader instanceof Server) || (sIPHeader instanceof Unsupported) || (sIPHeader instanceof RetryAfter) || (sIPHeader instanceof Warning) || (sIPHeader instanceof WWWAuthenticate) || (sIPHeader instanceof SIPETag) || (sIPHeader instanceof RSeq);
    }

    public LinkedList<String> getMessageAsEncodedStrings() {
        LinkedList<String> linkedList = new LinkedList<>();
        for (SIPHeader sIPHeader : this.headers) {
            if (sIPHeader instanceof SIPHeaderList) {
                linkedList.addAll(((SIPHeaderList) sIPHeader).getHeadersAsEncodedStrings());
            } else {
                linkedList.add(sIPHeader.encode());
            }
        }
        return linkedList;
    }

    protected String encodeSIPHeaders() {
        StringBuffer stringBuffer = new StringBuffer();
        for (SIPHeader sIPHeader : this.headers) {
            if (!(sIPHeader instanceof ContentLength)) {
                sIPHeader.encode(stringBuffer);
            }
        }
        StringBuffer stringBufferEncode = this.contentLengthHeader.encode(stringBuffer);
        stringBufferEncode.append(Separators.NEWLINE);
        return stringBufferEncode.toString();
    }

    @Override
    public boolean match(Object obj) {
        boolean z;
        boolean z2;
        if (obj == null) {
            return true;
        }
        if (!obj.getClass().equals(getClass())) {
            return false;
        }
        Iterator<SIPHeader> headers = ((SIPMessage) obj).getHeaders();
        while (headers.hasNext()) {
            SIPHeader next = headers.next();
            List<SIPHeader> headerList = getHeaderList(next.getHeaderName());
            if (headerList == null || headerList.size() == 0) {
                return false;
            }
            if (next instanceof SIPHeaderList) {
                ListIterator listIterator = ((SIPHeaderList) next).listIterator();
                while (listIterator.hasNext()) {
                    SIPHeader sIPHeader = (SIPHeader) listIterator.next();
                    if (!(sIPHeader instanceof ContentLength)) {
                        ListIterator<SIPHeader> listIterator2 = headerList.listIterator();
                        while (true) {
                            if (listIterator2.hasNext()) {
                                if (listIterator2.next().match(sIPHeader)) {
                                    z = true;
                                    break;
                                }
                            } else {
                                z = false;
                                break;
                            }
                        }
                        if (!z) {
                            return false;
                        }
                    }
                }
            } else {
                ListIterator<SIPHeader> listIterator3 = headerList.listIterator();
                while (true) {
                    if (listIterator3.hasNext()) {
                        if (listIterator3.next().match(next)) {
                            z2 = true;
                            break;
                        }
                    } else {
                        z2 = false;
                        break;
                    }
                }
                if (!z2) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void merge(Object obj) {
        if (!obj.getClass().equals(getClass())) {
            throw new IllegalArgumentException("Bad class " + obj.getClass());
        }
        for (Object obj2 : ((SIPMessage) obj).headers.toArray()) {
            SIPHeader sIPHeader = (SIPHeader) obj2;
            List<SIPHeader> headerList = getHeaderList(sIPHeader.getHeaderName());
            if (headerList == null) {
                attachHeader(sIPHeader);
            } else {
                ListIterator<SIPHeader> listIterator = headerList.listIterator();
                while (listIterator.hasNext()) {
                    listIterator.next().merge(sIPHeader);
                }
            }
        }
    }

    @Override
    public String encode() {
        String str;
        StringBuffer stringBuffer = new StringBuffer();
        for (SIPHeader sIPHeader : this.headers) {
            if (!(sIPHeader instanceof ContentLength)) {
                stringBuffer.append(sIPHeader.encode());
            }
        }
        Iterator<String> it = this.unrecognizedHeaders.iterator();
        while (it.hasNext()) {
            stringBuffer.append(it.next());
            stringBuffer.append(Separators.NEWLINE);
        }
        stringBuffer.append(this.contentLengthHeader.encode());
        stringBuffer.append(Separators.NEWLINE);
        if (this.messageContentObject != null) {
            stringBuffer.append(getContent().toString());
        } else if (this.messageContent != null || this.messageContentBytes != null) {
            String str2 = null;
            try {
                if (this.messageContent != null) {
                    str = this.messageContent;
                } else {
                    str = new String(this.messageContentBytes, getCharset());
                }
                str2 = str;
            } catch (UnsupportedEncodingException e) {
                InternalErrorHandler.handleException(e);
            }
            stringBuffer.append(str2);
        }
        return stringBuffer.toString();
    }

    public byte[] encodeAsBytes(String str) {
        byte[] bytes;
        if ((this instanceof SIPRequest) && ((SIPRequest) this).isNullRequest()) {
            return "\r\n\r\n".getBytes();
        }
        try {
            ((ViaHeader) getHeader("Via")).setTransport(str);
        } catch (ParseException e) {
            InternalErrorHandler.handleException(e);
        }
        StringBuffer stringBuffer = new StringBuffer();
        synchronized (this.headers) {
            for (SIPHeader sIPHeader : this.headers) {
                if (!(sIPHeader instanceof ContentLength)) {
                    sIPHeader.encode(stringBuffer);
                }
            }
        }
        this.contentLengthHeader.encode(stringBuffer);
        stringBuffer.append(Separators.NEWLINE);
        byte[] rawContent = getRawContent();
        if (rawContent != null) {
            try {
                bytes = stringBuffer.toString().getBytes(getCharset());
            } catch (UnsupportedEncodingException e2) {
                InternalErrorHandler.handleException(e2);
                bytes = null;
            }
            byte[] bArr = new byte[bytes.length + rawContent.length];
            System.arraycopy(bytes, 0, bArr, 0, bytes.length);
            System.arraycopy(rawContent, 0, bArr, bytes.length, rawContent.length);
            return bArr;
        }
        try {
            return stringBuffer.toString().getBytes(getCharset());
        } catch (UnsupportedEncodingException e3) {
            InternalErrorHandler.handleException(e3);
            return null;
        }
    }

    @Override
    public Object clone() {
        SIPMessage sIPMessage = (SIPMessage) super.clone();
        sIPMessage.nameTable = new Hashtable<>();
        sIPMessage.fromHeader = null;
        sIPMessage.toHeader = null;
        sIPMessage.cSeqHeader = null;
        sIPMessage.callIdHeader = null;
        sIPMessage.contentLengthHeader = null;
        sIPMessage.maxForwardsHeader = null;
        if (this.headers != null) {
            sIPMessage.headers = new ConcurrentLinkedQueue<>();
            Iterator<SIPHeader> it = this.headers.iterator();
            while (it.hasNext()) {
                sIPMessage.attachHeader((SIPHeader) it.next().clone());
            }
        }
        if (this.messageContentBytes != null) {
            sIPMessage.messageContentBytes = (byte[]) this.messageContentBytes.clone();
        }
        if (this.messageContentObject != null) {
            sIPMessage.messageContentObject = makeClone(this.messageContentObject);
        }
        sIPMessage.unrecognizedHeaders = this.unrecognizedHeaders;
        return sIPMessage;
    }

    @Override
    public String debugDump() {
        this.stringRepresentation = "";
        sprint("SIPMessage:");
        sprint("{");
        try {
            for (Field field : getClass().getDeclaredFields()) {
                Class<?> type = field.getType();
                String name = field.getName();
                if (field.get(this) != null && SIPHeader.class.isAssignableFrom(type) && name.compareTo("headers") != 0) {
                    sprint(name + Separators.EQUALS);
                    sprint(((SIPHeader) field.get(this)).debugDump());
                }
            }
        } catch (Exception e) {
            InternalErrorHandler.handleException(e);
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

    private void attachHeader(SIPHeader sIPHeader) {
        if (sIPHeader == null) {
            throw new IllegalArgumentException("null header!");
        }
        try {
            if ((sIPHeader instanceof SIPHeaderList) && ((SIPHeaderList) sIPHeader).isEmpty()) {
                return;
            }
            attachHeader(sIPHeader, false, false);
        } catch (SIPDuplicateHeaderException e) {
        }
    }

    @Override
    public void setHeader(Header header) {
        SIPHeader sIPHeader = (SIPHeader) header;
        if (sIPHeader == null) {
            throw new IllegalArgumentException("null header!");
        }
        try {
            if ((sIPHeader instanceof SIPHeaderList) && ((SIPHeaderList) sIPHeader).isEmpty()) {
                return;
            }
            removeHeader(sIPHeader.getHeaderName());
            attachHeader(sIPHeader, true, false);
        } catch (SIPDuplicateHeaderException e) {
            InternalErrorHandler.handleException(e);
        }
    }

    public void setHeaders(List<SIPHeader> list) {
        ListIterator<SIPHeader> listIterator = list.listIterator();
        while (listIterator.hasNext()) {
            try {
                attachHeader(listIterator.next(), false);
            } catch (SIPDuplicateHeaderException e) {
            }
        }
    }

    public void attachHeader(SIPHeader sIPHeader, boolean z) throws SIPDuplicateHeaderException {
        attachHeader(sIPHeader, z, false);
    }

    public void attachHeader(SIPHeader sIPHeader, boolean z, boolean z2) throws SIPDuplicateHeaderException {
        SIPHeader sIPHeader2;
        SIPHeaderList sIPHeaderList;
        if (sIPHeader == null) {
            throw new NullPointerException("null header");
        }
        if (ListMap.hasList(sIPHeader) && !SIPHeaderList.class.isAssignableFrom(sIPHeader.getClass())) {
            SIPHeaderList<SIPHeader> list = ListMap.getList(sIPHeader);
            list.add(sIPHeader);
            sIPHeader2 = list;
        } else {
            sIPHeader2 = sIPHeader;
        }
        String lowerCase = SIPHeaderNamesCache.toLowerCase(sIPHeader2.getName());
        if (z) {
            this.nameTable.remove(lowerCase);
        } else if (this.nameTable.containsKey(lowerCase) && !(sIPHeader2 instanceof SIPHeaderList)) {
            if (sIPHeader2 instanceof ContentLength) {
                try {
                    this.contentLengthHeader.setContentLength(((ContentLength) sIPHeader2).getContentLength());
                    return;
                } catch (InvalidArgumentException e) {
                    return;
                }
            }
            return;
        }
        SIPHeader sIPHeader3 = (SIPHeader) getHeader(sIPHeader.getName());
        if (sIPHeader3 != null) {
            Iterator<SIPHeader> it = this.headers.iterator();
            while (it.hasNext()) {
                if (it.next().equals(sIPHeader3)) {
                    it.remove();
                }
            }
        }
        if (!this.nameTable.containsKey(lowerCase)) {
            this.nameTable.put(lowerCase, sIPHeader2);
            this.headers.add(sIPHeader2);
        } else if ((sIPHeader2 instanceof SIPHeaderList) && (sIPHeaderList = (SIPHeaderList) this.nameTable.get(lowerCase)) != null) {
            sIPHeaderList.concatenate((SIPHeaderList) sIPHeader2, z2);
        } else {
            this.nameTable.put(lowerCase, sIPHeader2);
        }
        if (sIPHeader2 instanceof From) {
            this.fromHeader = (From) sIPHeader2;
            return;
        }
        if (sIPHeader2 instanceof ContentLength) {
            this.contentLengthHeader = (ContentLength) sIPHeader2;
            return;
        }
        if (sIPHeader2 instanceof To) {
            this.toHeader = (To) sIPHeader2;
            return;
        }
        if (sIPHeader2 instanceof CSeq) {
            this.cSeqHeader = (CSeq) sIPHeader2;
        } else if (sIPHeader2 instanceof CallID) {
            this.callIdHeader = (CallID) sIPHeader2;
        } else if (sIPHeader2 instanceof MaxForwards) {
            this.maxForwardsHeader = (MaxForwards) sIPHeader2;
        }
    }

    public void removeHeader(String str, boolean z) {
        String lowerCase = SIPHeaderNamesCache.toLowerCase(str);
        SIPHeader sIPHeader = this.nameTable.get(lowerCase);
        if (sIPHeader == null) {
            return;
        }
        if (sIPHeader instanceof SIPHeaderList) {
            SIPHeaderList sIPHeaderList = (SIPHeaderList) sIPHeader;
            if (z) {
                sIPHeaderList.removeFirst();
            } else {
                sIPHeaderList.removeLast();
            }
            if (sIPHeaderList.isEmpty()) {
                Iterator<SIPHeader> it = this.headers.iterator();
                while (it.hasNext()) {
                    if (it.next().getName().equalsIgnoreCase(lowerCase)) {
                        it.remove();
                    }
                }
                this.nameTable.remove(lowerCase);
                return;
            }
            return;
        }
        this.nameTable.remove(lowerCase);
        if (sIPHeader instanceof From) {
            this.fromHeader = null;
        } else if (sIPHeader instanceof To) {
            this.toHeader = null;
        } else if (sIPHeader instanceof CSeq) {
            this.cSeqHeader = null;
        } else if (sIPHeader instanceof CallID) {
            this.callIdHeader = null;
        } else if (sIPHeader instanceof MaxForwards) {
            this.maxForwardsHeader = null;
        } else if (sIPHeader instanceof ContentLength) {
            this.contentLengthHeader = null;
        }
        Iterator<SIPHeader> it2 = this.headers.iterator();
        while (it2.hasNext()) {
            if (it2.next().getName().equalsIgnoreCase(str)) {
                it2.remove();
            }
        }
    }

    @Override
    public void removeHeader(String str) {
        if (str == null) {
            throw new NullPointerException("null arg");
        }
        String lowerCase = SIPHeaderNamesCache.toLowerCase(str);
        SIPHeader sIPHeaderRemove = this.nameTable.remove(lowerCase);
        if (sIPHeaderRemove == null) {
            return;
        }
        if (sIPHeaderRemove instanceof From) {
            this.fromHeader = null;
        } else if (sIPHeaderRemove instanceof To) {
            this.toHeader = null;
        } else if (sIPHeaderRemove instanceof CSeq) {
            this.cSeqHeader = null;
        } else if (sIPHeaderRemove instanceof CallID) {
            this.callIdHeader = null;
        } else if (sIPHeaderRemove instanceof MaxForwards) {
            this.maxForwardsHeader = null;
        } else if (sIPHeaderRemove instanceof ContentLength) {
            this.contentLengthHeader = null;
        }
        Iterator<SIPHeader> it = this.headers.iterator();
        while (it.hasNext()) {
            if (it.next().getName().equalsIgnoreCase(lowerCase)) {
                it.remove();
            }
        }
    }

    public String getTransactionId() {
        Via via;
        if (!getViaHeaders().isEmpty()) {
            via = (Via) getViaHeaders().getFirst();
        } else {
            via = null;
        }
        if (via != null && via.getBranch() != null && via.getBranch().toUpperCase().startsWith(SIPConstants.BRANCH_MAGIC_COOKIE_UPPER_CASE)) {
            if (getCSeq().getMethod().equals(Request.CANCEL)) {
                return (via.getBranch() + Separators.COLON + getCSeq().getMethod()).toLowerCase();
            }
            return via.getBranch().toLowerCase();
        }
        StringBuffer stringBuffer = new StringBuffer();
        From from = (From) getFrom();
        if (from.hasTag()) {
            stringBuffer.append(from.getTag());
            stringBuffer.append("-");
        }
        stringBuffer.append(this.callIdHeader.getCallId());
        stringBuffer.append("-");
        stringBuffer.append(this.cSeqHeader.getSequenceNumber());
        stringBuffer.append("-");
        stringBuffer.append(this.cSeqHeader.getMethod());
        if (via != null) {
            stringBuffer.append("-");
            stringBuffer.append(via.getSentBy().encode());
            if (!via.getSentBy().hasPort()) {
                stringBuffer.append("-");
                stringBuffer.append(5060);
            }
        }
        if (getCSeq().getMethod().equals(Request.CANCEL)) {
            stringBuffer.append(Request.CANCEL);
        }
        return stringBuffer.toString().toLowerCase().replace(Separators.COLON, "-").replace(Separators.AT, "-") + Utils.getSignature();
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
    public Header getHeader(String str) {
        return getHeaderLowerCase(SIPHeaderNamesCache.toLowerCase(str));
    }

    private Header getHeaderLowerCase(String str) {
        if (str == null) {
            throw new NullPointerException("bad name");
        }
        SIPHeader sIPHeader = this.nameTable.get(str);
        if (sIPHeader instanceof SIPHeaderList) {
            return ((SIPHeaderList) sIPHeader).getFirst();
        }
        return sIPHeader;
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
        ContactList contactHeaders = getContactHeaders();
        if (contactHeaders != null) {
            return (Contact) contactHeaders.getFirst();
        }
        return null;
    }

    public ViaList getViaHeaders() {
        return (ViaList) getSIPHeaderListLowerCase(VIA_LOWERCASE);
    }

    public void setVia(List list) {
        ViaList viaList = new ViaList();
        ListIterator listIterator = list.listIterator();
        while (listIterator.hasNext()) {
            viaList.add((Via) listIterator.next());
        }
        setHeader((SIPHeaderList<Via>) viaList);
    }

    public void setHeader(SIPHeaderList<Via> sIPHeaderList) {
        setHeader((Header) sIPHeaderList);
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

    public void setMaxForwards(MaxForwardsHeader maxForwardsHeader) {
        setHeader(maxForwardsHeader);
    }

    public RouteList getRouteHeaders() {
        return (RouteList) getSIPHeaderListLowerCase(ROUTE_LOWERCASE);
    }

    public CallIdHeader getCallId() {
        return this.callIdHeader;
    }

    public void setCallId(CallIdHeader callIdHeader) {
        setHeader(callIdHeader);
    }

    public void setCallId(String str) throws ParseException {
        if (this.callIdHeader == null) {
            setHeader(new CallID());
        }
        this.callIdHeader.setCallId(str);
    }

    public RecordRouteList getRecordRouteHeaders() {
        return (RecordRouteList) getSIPHeaderListLowerCase(RECORDROUTE_LOWERCASE);
    }

    public ToHeader getTo() {
        return this.toHeader;
    }

    public void setTo(ToHeader toHeader) {
        setHeader(toHeader);
    }

    public void setFrom(FromHeader fromHeader) {
        setHeader(fromHeader);
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
                    this.messageContentBytes = this.messageContentObject.toString().getBytes(getCharset());
                } else if (this.messageContent != null) {
                    this.messageContentBytes = this.messageContent.getBytes(getCharset());
                }
            }
            return this.messageContentBytes;
        } catch (UnsupportedEncodingException e) {
            InternalErrorHandler.handleException(e);
            return null;
        }
    }

    public void setMessageContent(String str, String str2, String str3) {
        if (str3 == null) {
            throw new IllegalArgumentException("messgeContent is null");
        }
        setHeader(new ContentType(str, str2));
        this.messageContent = str3;
        this.messageContentBytes = null;
        this.messageContentObject = null;
        computeContentLength(str3);
    }

    @Override
    public void setContent(Object obj, ContentTypeHeader contentTypeHeader) throws ParseException {
        if (obj == null) {
            throw new NullPointerException("null content");
        }
        setHeader(contentTypeHeader);
        this.messageContent = null;
        this.messageContentBytes = null;
        this.messageContentObject = null;
        if (obj instanceof String) {
            this.messageContent = (String) obj;
        } else if (obj instanceof byte[]) {
            this.messageContentBytes = (byte[]) obj;
        } else {
            this.messageContentObject = obj;
        }
        computeContentLength(obj);
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

    public void setMessageContent(String str, String str2, byte[] bArr) {
        setHeader(new ContentType(str, str2));
        setMessageContent(bArr);
        computeContentLength(bArr);
    }

    public void setMessageContent(String str, boolean z, boolean z2, int i) throws ParseException {
        computeContentLength(str);
        if (!z2 && ((!z && this.contentLengthHeader.getContentLength() != i) || this.contentLengthHeader.getContentLength() < i)) {
            throw new ParseException("Invalid content length " + this.contentLengthHeader.getContentLength() + " / " + i, 0);
        }
        this.messageContent = str;
        this.messageContentBytes = null;
        this.messageContentObject = null;
    }

    public void setMessageContent(byte[] bArr) {
        computeContentLength(bArr);
        this.messageContentBytes = bArr;
        this.messageContent = null;
        this.messageContentObject = null;
    }

    public void setMessageContent(byte[] bArr, boolean z, int i) throws ParseException {
        computeContentLength(bArr);
        if (!z && this.contentLengthHeader.getContentLength() < i) {
            throw new ParseException("Invalid content length " + this.contentLengthHeader.getContentLength() + " / " + i, 0);
        }
        this.messageContentBytes = bArr;
        this.messageContent = null;
        this.messageContentObject = null;
    }

    private void computeContentLength(Object obj) {
        int length;
        if (obj != null) {
            if (obj instanceof String) {
                try {
                    length = ((String) obj).getBytes(getCharset()).length;
                } catch (UnsupportedEncodingException e) {
                    InternalErrorHandler.handleException(e);
                    length = 0;
                }
            } else if (obj instanceof byte[]) {
                length = ((byte[]) obj).length;
            } else {
                length = obj.toString().length();
            }
        } else {
            length = 0;
        }
        try {
            this.contentLengthHeader.setContentLength(length);
        } catch (InvalidArgumentException e2) {
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
    public ListIterator<SIPHeader> getHeaders(String str) {
        if (str == null) {
            throw new NullPointerException("null headerName");
        }
        SIPHeader sIPHeader = this.nameTable.get(SIPHeaderNamesCache.toLowerCase(str));
        if (sIPHeader == null) {
            return new LinkedList().listIterator();
        }
        if (sIPHeader instanceof SIPHeaderList) {
            return ((SIPHeaderList) sIPHeader).listIterator();
        }
        return new HeaderIterator(this, sIPHeader);
    }

    public String getHeaderAsFormattedString(String str) {
        String lowerCase = str.toLowerCase();
        if (this.nameTable.containsKey(lowerCase)) {
            return this.nameTable.get(lowerCase).toString();
        }
        return getHeader(str).toString();
    }

    private SIPHeader getSIPHeaderListLowerCase(String str) {
        return this.nameTable.get(str);
    }

    private List<SIPHeader> getHeaderList(String str) {
        SIPHeader sIPHeader = this.nameTable.get(SIPHeaderNamesCache.toLowerCase(str));
        if (sIPHeader == null) {
            return null;
        }
        if (sIPHeader instanceof SIPHeaderList) {
            return ((SIPHeaderList) sIPHeader).getHeaderList();
        }
        LinkedList linkedList = new LinkedList();
        linkedList.add(sIPHeader);
        return linkedList;
    }

    public boolean hasHeader(String str) {
        return this.nameTable.containsKey(SIPHeaderNamesCache.toLowerCase(str));
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

    public void setFromTag(String str) {
        try {
            this.fromHeader.setTag(str);
        } catch (ParseException e) {
        }
    }

    public void setToTag(String str) {
        try {
            this.toHeader.setTag(str);
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
    public void addHeader(Header header) {
        SIPHeader sIPHeader = (SIPHeader) header;
        try {
            if ((header instanceof ViaHeader) || (header instanceof RecordRouteHeader)) {
                attachHeader(sIPHeader, false, true);
            } else {
                attachHeader(sIPHeader, false, false);
            }
        } catch (SIPDuplicateHeaderException e) {
            try {
                if (header instanceof ContentLength) {
                    this.contentLengthHeader.setContentLength(((ContentLength) header).getContentLength());
                }
            } catch (InvalidArgumentException e2) {
            }
        }
    }

    public void addUnparsed(String str) {
        this.unrecognizedHeaders.add(str);
    }

    public void addHeader(String str) {
        String str2 = str.trim() + Separators.RETURN;
        try {
            attachHeader(ParserFactory.createParser(str).parse(), false);
        } catch (ParseException e) {
            this.unrecognizedHeaders.add(str2);
        }
    }

    @Override
    public ListIterator<String> getUnrecognizedHeaders() {
        return this.unrecognizedHeaders.listIterator();
    }

    @Override
    public ListIterator<String> getHeaderNames() {
        Iterator<SIPHeader> it = this.headers.iterator();
        LinkedList linkedList = new LinkedList();
        while (it.hasNext()) {
            linkedList.add(it.next().getName());
        }
        return linkedList.listIterator();
    }

    @Override
    public boolean equals(Object obj) {
        if (!obj.getClass().equals(getClass())) {
            return false;
        }
        SIPMessage sIPMessage = (SIPMessage) obj;
        if (this.nameTable.size() != sIPMessage.nameTable.size()) {
            return false;
        }
        for (SIPHeader sIPHeader : this.nameTable.values()) {
            SIPHeader sIPHeader2 = sIPMessage.nameTable.get(SIPHeaderNamesCache.toLowerCase(sIPHeader.getName()));
            if (sIPHeader2 == null || !sIPHeader2.equals(sIPHeader)) {
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
    public void setContentLength(ContentLengthHeader contentLengthHeader) {
        try {
            this.contentLengthHeader.setContentLength(contentLengthHeader.getContentLength());
        } catch (InvalidArgumentException e) {
        }
    }

    public void setSize(int i) {
        this.size = i;
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
    public void removeFirst(String str) throws NullPointerException {
        if (str == null) {
            throw new NullPointerException("Null argument Provided!");
        }
        removeHeader(str, true);
    }

    @Override
    public void removeLast(String str) {
        if (str == null) {
            throw new NullPointerException("Null argument Provided!");
        }
        removeHeader(str, false);
    }

    public void setCSeq(CSeqHeader cSeqHeader) {
        setHeader(cSeqHeader);
    }

    @Override
    public void setApplicationData(Object obj) {
        this.applicationData = obj;
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
        MultipartMimeContentImpl multipartMimeContentImpl = new MultipartMimeContentImpl(getContentTypeHeader());
        try {
            multipartMimeContentImpl.createContentList(new String(getRawContent(), getCharset()));
            return multipartMimeContentImpl;
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
        ContentType contentTypeHeader = getContentTypeHeader();
        if (contentTypeHeader != null) {
            String charset = contentTypeHeader.getCharset();
            return charset != null ? charset : this.contentEncodingCharset;
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
