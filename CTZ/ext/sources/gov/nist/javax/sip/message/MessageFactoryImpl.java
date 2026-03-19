package gov.nist.javax.sip.message;

import gov.nist.javax.sip.header.CSeq;
import gov.nist.javax.sip.header.CallID;
import gov.nist.javax.sip.header.ContentType;
import gov.nist.javax.sip.header.From;
import gov.nist.javax.sip.header.MaxForwards;
import gov.nist.javax.sip.header.RequestLine;
import gov.nist.javax.sip.header.StatusLine;
import gov.nist.javax.sip.header.To;
import gov.nist.javax.sip.header.Via;
import gov.nist.javax.sip.parser.ParseExceptionListener;
import gov.nist.javax.sip.parser.StringMsgParser;
import java.text.ParseException;
import java.util.List;
import javax.sip.address.URI;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.ServerHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.UserAgentHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

public class MessageFactoryImpl implements MessageFactory, MessageFactoryExt {
    private static String defaultContentEncodingCharset = "UTF-8";
    private static ServerHeader server;
    private static UserAgentHeader userAgent;
    private boolean testing = false;
    private boolean strict = true;

    public void setStrict(boolean z) {
        this.strict = z;
    }

    public void setTest(boolean z) {
        this.testing = z;
    }

    @Override
    public Request createRequest(URI uri, String str, CallIdHeader callIdHeader, CSeqHeader cSeqHeader, FromHeader fromHeader, ToHeader toHeader, List list, MaxForwardsHeader maxForwardsHeader, ContentTypeHeader contentTypeHeader, Object obj) throws ParseException {
        if (uri == null || str == null || callIdHeader == null || cSeqHeader == null || fromHeader == null || toHeader == null || list == null || maxForwardsHeader == null || obj == null || contentTypeHeader == null) {
            throw new NullPointerException("Null parameters");
        }
        SIPRequest sIPRequest = new SIPRequest();
        sIPRequest.setRequestURI(uri);
        sIPRequest.setMethod(str);
        sIPRequest.setCallId(callIdHeader);
        sIPRequest.setCSeq(cSeqHeader);
        sIPRequest.setFrom(fromHeader);
        sIPRequest.setTo(toHeader);
        sIPRequest.setVia(list);
        sIPRequest.setMaxForwards(maxForwardsHeader);
        sIPRequest.setContent(obj, contentTypeHeader);
        if (userAgent != null) {
            sIPRequest.setHeader(userAgent);
        }
        return sIPRequest;
    }

    public Request createRequest(URI uri, String str, CallIdHeader callIdHeader, CSeqHeader cSeqHeader, FromHeader fromHeader, ToHeader toHeader, List list, MaxForwardsHeader maxForwardsHeader, byte[] bArr, ContentTypeHeader contentTypeHeader) throws ParseException {
        if (uri == null || str == null || callIdHeader == null || cSeqHeader == null || fromHeader == null || toHeader == null || list == null || maxForwardsHeader == null || bArr == null || contentTypeHeader == null) {
            throw new ParseException("JAIN-SIP Exception, some parameters are missing, unable to create the request", 0);
        }
        SIPRequest sIPRequest = new SIPRequest();
        sIPRequest.setRequestURI(uri);
        sIPRequest.setMethod(str);
        sIPRequest.setCallId(callIdHeader);
        sIPRequest.setCSeq(cSeqHeader);
        sIPRequest.setFrom(fromHeader);
        sIPRequest.setTo(toHeader);
        sIPRequest.setVia(list);
        sIPRequest.setMaxForwards(maxForwardsHeader);
        sIPRequest.setHeader((ContentType) contentTypeHeader);
        sIPRequest.setMessageContent(bArr);
        if (userAgent != null) {
            sIPRequest.setHeader(userAgent);
        }
        return sIPRequest;
    }

    @Override
    public Request createRequest(URI uri, String str, CallIdHeader callIdHeader, CSeqHeader cSeqHeader, FromHeader fromHeader, ToHeader toHeader, List list, MaxForwardsHeader maxForwardsHeader) throws ParseException {
        if (uri == null || str == null || callIdHeader == null || cSeqHeader == null || fromHeader == null || toHeader == null || list == null || maxForwardsHeader == null) {
            throw new ParseException("JAIN-SIP Exception, some parameters are missing, unable to create the request", 0);
        }
        SIPRequest sIPRequest = new SIPRequest();
        sIPRequest.setRequestURI(uri);
        sIPRequest.setMethod(str);
        sIPRequest.setCallId(callIdHeader);
        sIPRequest.setCSeq(cSeqHeader);
        sIPRequest.setFrom(fromHeader);
        sIPRequest.setTo(toHeader);
        sIPRequest.setVia(list);
        sIPRequest.setMaxForwards(maxForwardsHeader);
        if (userAgent != null) {
            sIPRequest.setHeader(userAgent);
        }
        return sIPRequest;
    }

    public Response createResponse(int i, CallIdHeader callIdHeader, CSeqHeader cSeqHeader, FromHeader fromHeader, ToHeader toHeader, List list, MaxForwardsHeader maxForwardsHeader, Object obj, ContentTypeHeader contentTypeHeader) throws ParseException {
        if (callIdHeader == null || cSeqHeader == null || fromHeader == null || toHeader == null || list == null || maxForwardsHeader == null || obj == null || contentTypeHeader == null) {
            throw new NullPointerException(" unable to create the response");
        }
        SIPResponse sIPResponse = new SIPResponse();
        StatusLine statusLine = new StatusLine();
        statusLine.setStatusCode(i);
        statusLine.setReasonPhrase(SIPResponse.getReasonPhrase(i));
        sIPResponse.setStatusLine(statusLine);
        sIPResponse.setCallId(callIdHeader);
        sIPResponse.setCSeq(cSeqHeader);
        sIPResponse.setFrom(fromHeader);
        sIPResponse.setTo(toHeader);
        sIPResponse.setVia(list);
        sIPResponse.setMaxForwards(maxForwardsHeader);
        sIPResponse.setContent(obj, contentTypeHeader);
        if (userAgent != null) {
            sIPResponse.setHeader(userAgent);
        }
        return sIPResponse;
    }

    public Response createResponse(int i, CallIdHeader callIdHeader, CSeqHeader cSeqHeader, FromHeader fromHeader, ToHeader toHeader, List list, MaxForwardsHeader maxForwardsHeader, byte[] bArr, ContentTypeHeader contentTypeHeader) throws ParseException {
        if (callIdHeader == null || cSeqHeader == null || fromHeader == null || toHeader == null || list == null || maxForwardsHeader == null || bArr == null || contentTypeHeader == null) {
            throw new NullPointerException("Null params ");
        }
        SIPResponse sIPResponse = new SIPResponse();
        sIPResponse.setStatusCode(i);
        sIPResponse.setCallId(callIdHeader);
        sIPResponse.setCSeq(cSeqHeader);
        sIPResponse.setFrom(fromHeader);
        sIPResponse.setTo(toHeader);
        sIPResponse.setVia(list);
        sIPResponse.setMaxForwards(maxForwardsHeader);
        sIPResponse.setHeader((ContentType) contentTypeHeader);
        sIPResponse.setMessageContent(bArr);
        if (userAgent != null) {
            sIPResponse.setHeader(userAgent);
        }
        return sIPResponse;
    }

    @Override
    public Response createResponse(int i, CallIdHeader callIdHeader, CSeqHeader cSeqHeader, FromHeader fromHeader, ToHeader toHeader, List list, MaxForwardsHeader maxForwardsHeader) throws ParseException {
        if (callIdHeader == null || cSeqHeader == null || fromHeader == null || toHeader == null || list == null || maxForwardsHeader == null) {
            throw new ParseException("JAIN-SIP Exception, some parameters are missing, unable to create the response", 0);
        }
        SIPResponse sIPResponse = new SIPResponse();
        sIPResponse.setStatusCode(i);
        sIPResponse.setCallId(callIdHeader);
        sIPResponse.setCSeq(cSeqHeader);
        sIPResponse.setFrom(fromHeader);
        sIPResponse.setTo(toHeader);
        sIPResponse.setVia(list);
        sIPResponse.setMaxForwards(maxForwardsHeader);
        if (userAgent != null) {
            sIPResponse.setHeader(userAgent);
        }
        return sIPResponse;
    }

    @Override
    public Response createResponse(int i, Request request, ContentTypeHeader contentTypeHeader, Object obj) throws ParseException {
        if (request == null || obj == null || contentTypeHeader == null) {
            throw new NullPointerException("null parameters");
        }
        SIPResponse sIPResponseCreateResponse = ((SIPRequest) request).createResponse(i);
        sIPResponseCreateResponse.setContent(obj, contentTypeHeader);
        if (server != null) {
            sIPResponseCreateResponse.setHeader(server);
        }
        return sIPResponseCreateResponse;
    }

    @Override
    public Response createResponse(int i, Request request, ContentTypeHeader contentTypeHeader, byte[] bArr) throws ParseException {
        if (request == null || bArr == null || contentTypeHeader == null) {
            throw new NullPointerException("null Parameters");
        }
        SIPResponse sIPResponseCreateResponse = ((SIPRequest) request).createResponse(i);
        sIPResponseCreateResponse.setHeader((ContentType) contentTypeHeader);
        sIPResponseCreateResponse.setMessageContent(bArr);
        if (server != null) {
            sIPResponseCreateResponse.setHeader(server);
        }
        return sIPResponseCreateResponse;
    }

    @Override
    public Response createResponse(int i, Request request) throws ParseException {
        if (request == null) {
            throw new NullPointerException("null parameters");
        }
        SIPResponse sIPResponseCreateResponse = ((SIPRequest) request).createResponse(i);
        sIPResponseCreateResponse.removeContent();
        sIPResponseCreateResponse.removeHeader("Content-Type");
        if (server != null) {
            sIPResponseCreateResponse.setHeader(server);
        }
        return sIPResponseCreateResponse;
    }

    @Override
    public Request createRequest(URI uri, String str, CallIdHeader callIdHeader, CSeqHeader cSeqHeader, FromHeader fromHeader, ToHeader toHeader, List list, MaxForwardsHeader maxForwardsHeader, ContentTypeHeader contentTypeHeader, byte[] bArr) throws ParseException {
        if (uri == null || str == null || callIdHeader == null || cSeqHeader == null || fromHeader == null || toHeader == null || list == null || maxForwardsHeader == null || bArr == null || contentTypeHeader == null) {
            throw new NullPointerException("missing parameters");
        }
        SIPRequest sIPRequest = new SIPRequest();
        sIPRequest.setRequestURI(uri);
        sIPRequest.setMethod(str);
        sIPRequest.setCallId(callIdHeader);
        sIPRequest.setCSeq(cSeqHeader);
        sIPRequest.setFrom(fromHeader);
        sIPRequest.setTo(toHeader);
        sIPRequest.setVia(list);
        sIPRequest.setMaxForwards(maxForwardsHeader);
        sIPRequest.setContent(bArr, contentTypeHeader);
        if (userAgent != null) {
            sIPRequest.setHeader(userAgent);
        }
        return sIPRequest;
    }

    @Override
    public Response createResponse(int i, CallIdHeader callIdHeader, CSeqHeader cSeqHeader, FromHeader fromHeader, ToHeader toHeader, List list, MaxForwardsHeader maxForwardsHeader, ContentTypeHeader contentTypeHeader, Object obj) throws ParseException {
        if (callIdHeader == null || cSeqHeader == null || fromHeader == null || toHeader == null || list == null || maxForwardsHeader == null || obj == null || contentTypeHeader == null) {
            throw new NullPointerException("missing parameters");
        }
        SIPResponse sIPResponse = new SIPResponse();
        StatusLine statusLine = new StatusLine();
        statusLine.setStatusCode(i);
        String reasonPhrase = SIPResponse.getReasonPhrase(i);
        if (reasonPhrase == null) {
            throw new ParseException(i + " Unknown", 0);
        }
        statusLine.setReasonPhrase(reasonPhrase);
        sIPResponse.setStatusLine(statusLine);
        sIPResponse.setCallId(callIdHeader);
        sIPResponse.setCSeq(cSeqHeader);
        sIPResponse.setFrom(fromHeader);
        sIPResponse.setTo(toHeader);
        sIPResponse.setVia(list);
        sIPResponse.setContent(obj, contentTypeHeader);
        if (userAgent != null) {
            sIPResponse.setHeader(userAgent);
        }
        return sIPResponse;
    }

    @Override
    public Response createResponse(int i, CallIdHeader callIdHeader, CSeqHeader cSeqHeader, FromHeader fromHeader, ToHeader toHeader, List list, MaxForwardsHeader maxForwardsHeader, ContentTypeHeader contentTypeHeader, byte[] bArr) throws ParseException {
        if (callIdHeader == null || cSeqHeader == null || fromHeader == null || toHeader == null || list == null || maxForwardsHeader == null || bArr == null || contentTypeHeader == null) {
            throw new NullPointerException("missing parameters");
        }
        SIPResponse sIPResponse = new SIPResponse();
        StatusLine statusLine = new StatusLine();
        statusLine.setStatusCode(i);
        String reasonPhrase = SIPResponse.getReasonPhrase(i);
        if (reasonPhrase == null) {
            throw new ParseException(i + " : Unknown", 0);
        }
        statusLine.setReasonPhrase(reasonPhrase);
        sIPResponse.setStatusLine(statusLine);
        sIPResponse.setCallId(callIdHeader);
        sIPResponse.setCSeq(cSeqHeader);
        sIPResponse.setFrom(fromHeader);
        sIPResponse.setTo(toHeader);
        sIPResponse.setVia(list);
        sIPResponse.setContent(bArr, contentTypeHeader);
        if (userAgent != null) {
            sIPResponse.setHeader(userAgent);
        }
        return sIPResponse;
    }

    @Override
    public Request createRequest(String str) throws ParseException {
        if (str == null || str.equals("")) {
            SIPRequest sIPRequest = new SIPRequest();
            sIPRequest.setNullRequest();
            return sIPRequest;
        }
        StringMsgParser stringMsgParser = new StringMsgParser();
        stringMsgParser.setStrict(this.strict);
        ParseExceptionListener parseExceptionListener = new ParseExceptionListener() {
            @Override
            public void handleException(ParseException parseException, SIPMessage sIPMessage, Class cls, String str2, String str3) throws ParseException {
                if (MessageFactoryImpl.this.testing) {
                    if (cls == From.class || cls == To.class || cls == CallID.class || cls == MaxForwards.class || cls == Via.class || cls == RequestLine.class || cls == StatusLine.class || cls == CSeq.class) {
                        throw parseException;
                    }
                    sIPMessage.addUnparsed(str2);
                }
            }
        };
        if (this.testing) {
            stringMsgParser.setParseExceptionListener(parseExceptionListener);
        }
        SIPMessage sIPMessage = stringMsgParser.parseSIPMessage(str);
        if (!(sIPMessage instanceof SIPRequest)) {
            throw new ParseException(str, 0);
        }
        return (SIPRequest) sIPMessage;
    }

    @Override
    public Response createResponse(String str) throws ParseException {
        if (str == null) {
            return new SIPResponse();
        }
        SIPMessage sIPMessage = new StringMsgParser().parseSIPMessage(str);
        if (!(sIPMessage instanceof SIPResponse)) {
            throw new ParseException(str, 0);
        }
        return (SIPResponse) sIPMessage;
    }

    @Override
    public void setDefaultUserAgentHeader(UserAgentHeader userAgentHeader) {
        userAgent = userAgentHeader;
    }

    @Override
    public void setDefaultServerHeader(ServerHeader serverHeader) {
        server = serverHeader;
    }

    public static UserAgentHeader getDefaultUserAgentHeader() {
        return userAgent;
    }

    public static ServerHeader getDefaultServerHeader() {
        return server;
    }

    @Override
    public void setDefaultContentEncodingCharset(String str) throws IllegalArgumentException, NullPointerException {
        if (str == null) {
            throw new NullPointerException("Null argument!");
        }
        defaultContentEncodingCharset = str;
    }

    public static String getDefaultContentEncodingCharset() {
        return defaultContentEncodingCharset;
    }

    @Override
    public MultipartMimeContent createMultipartMimeContent(ContentTypeHeader contentTypeHeader, String[] strArr, String[] strArr2, String[] strArr3) {
        String parameter = contentTypeHeader.getParameter("boundary");
        MultipartMimeContentImpl multipartMimeContentImpl = new MultipartMimeContentImpl(contentTypeHeader);
        for (int i = 0; i < strArr.length; i++) {
            ContentType contentType = new ContentType(strArr[i], strArr2[i]);
            ContentImpl contentImpl = new ContentImpl(strArr3[i], parameter);
            contentImpl.setContentTypeHeader(contentType);
            multipartMimeContentImpl.add(contentImpl);
        }
        return multipartMimeContentImpl;
    }
}
