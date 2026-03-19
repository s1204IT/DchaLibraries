package gov.nist.javax.sip.header;

import gov.nist.core.Separators;
import gov.nist.javax.sip.address.GenericURI;
import gov.nist.javax.sip.header.extensions.Join;
import gov.nist.javax.sip.header.extensions.JoinHeader;
import gov.nist.javax.sip.header.extensions.MinSE;
import gov.nist.javax.sip.header.extensions.References;
import gov.nist.javax.sip.header.extensions.ReferencesHeader;
import gov.nist.javax.sip.header.extensions.ReferredBy;
import gov.nist.javax.sip.header.extensions.ReferredByHeader;
import gov.nist.javax.sip.header.extensions.Replaces;
import gov.nist.javax.sip.header.extensions.ReplacesHeader;
import gov.nist.javax.sip.header.extensions.SessionExpires;
import gov.nist.javax.sip.header.extensions.SessionExpiresHeader;
import gov.nist.javax.sip.header.ims.PAccessNetworkInfo;
import gov.nist.javax.sip.header.ims.PAccessNetworkInfoHeader;
import gov.nist.javax.sip.header.ims.PAssertedIdentity;
import gov.nist.javax.sip.header.ims.PAssertedIdentityHeader;
import gov.nist.javax.sip.header.ims.PAssertedService;
import gov.nist.javax.sip.header.ims.PAssertedServiceHeader;
import gov.nist.javax.sip.header.ims.PAssociatedURI;
import gov.nist.javax.sip.header.ims.PAssociatedURIHeader;
import gov.nist.javax.sip.header.ims.PCalledPartyID;
import gov.nist.javax.sip.header.ims.PCalledPartyIDHeader;
import gov.nist.javax.sip.header.ims.PChargingFunctionAddresses;
import gov.nist.javax.sip.header.ims.PChargingFunctionAddressesHeader;
import gov.nist.javax.sip.header.ims.PChargingVector;
import gov.nist.javax.sip.header.ims.PChargingVectorHeader;
import gov.nist.javax.sip.header.ims.PMediaAuthorization;
import gov.nist.javax.sip.header.ims.PMediaAuthorizationHeader;
import gov.nist.javax.sip.header.ims.PPreferredIdentity;
import gov.nist.javax.sip.header.ims.PPreferredIdentityHeader;
import gov.nist.javax.sip.header.ims.PPreferredService;
import gov.nist.javax.sip.header.ims.PPreferredServiceHeader;
import gov.nist.javax.sip.header.ims.PProfileKey;
import gov.nist.javax.sip.header.ims.PProfileKeyHeader;
import gov.nist.javax.sip.header.ims.PServedUser;
import gov.nist.javax.sip.header.ims.PServedUserHeader;
import gov.nist.javax.sip.header.ims.PUserDatabase;
import gov.nist.javax.sip.header.ims.PUserDatabaseHeader;
import gov.nist.javax.sip.header.ims.PVisitedNetworkID;
import gov.nist.javax.sip.header.ims.PVisitedNetworkIDHeader;
import gov.nist.javax.sip.header.ims.Path;
import gov.nist.javax.sip.header.ims.PathHeader;
import gov.nist.javax.sip.header.ims.Privacy;
import gov.nist.javax.sip.header.ims.PrivacyHeader;
import gov.nist.javax.sip.header.ims.SecurityClient;
import gov.nist.javax.sip.header.ims.SecurityClientHeader;
import gov.nist.javax.sip.header.ims.SecurityServer;
import gov.nist.javax.sip.header.ims.SecurityServerHeader;
import gov.nist.javax.sip.header.ims.SecurityVerify;
import gov.nist.javax.sip.header.ims.SecurityVerifyHeader;
import gov.nist.javax.sip.header.ims.ServiceRoute;
import gov.nist.javax.sip.header.ims.ServiceRouteHeader;
import gov.nist.javax.sip.parser.RequestLineParser;
import gov.nist.javax.sip.parser.StatusLineParser;
import gov.nist.javax.sip.parser.StringMsgParser;
import java.text.ParseException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import javax.sip.InvalidArgumentException;
import javax.sip.address.Address;
import javax.sip.address.URI;
import javax.sip.header.AcceptEncodingHeader;
import javax.sip.header.AcceptHeader;
import javax.sip.header.AcceptLanguageHeader;
import javax.sip.header.AlertInfoHeader;
import javax.sip.header.AllowEventsHeader;
import javax.sip.header.AllowHeader;
import javax.sip.header.AuthenticationInfoHeader;
import javax.sip.header.AuthorizationHeader;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.CallInfoHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentDispositionHeader;
import javax.sip.header.ContentEncodingHeader;
import javax.sip.header.ContentLanguageHeader;
import javax.sip.header.ContentLengthHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.DateHeader;
import javax.sip.header.ErrorInfoHeader;
import javax.sip.header.EventHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.header.ExtensionHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.Header;
import javax.sip.header.HeaderFactory;
import javax.sip.header.InReplyToHeader;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.MimeVersionHeader;
import javax.sip.header.MinExpiresHeader;
import javax.sip.header.OrganizationHeader;
import javax.sip.header.PriorityHeader;
import javax.sip.header.ProxyAuthenticateHeader;
import javax.sip.header.ProxyAuthorizationHeader;
import javax.sip.header.ProxyRequireHeader;
import javax.sip.header.RAckHeader;
import javax.sip.header.RSeqHeader;
import javax.sip.header.ReasonHeader;
import javax.sip.header.RecordRouteHeader;
import javax.sip.header.ReferToHeader;
import javax.sip.header.ReplyToHeader;
import javax.sip.header.RequireHeader;
import javax.sip.header.RetryAfterHeader;
import javax.sip.header.RouteHeader;
import javax.sip.header.SIPETagHeader;
import javax.sip.header.SIPIfMatchHeader;
import javax.sip.header.ServerHeader;
import javax.sip.header.SubjectHeader;
import javax.sip.header.SubscriptionStateHeader;
import javax.sip.header.SupportedHeader;
import javax.sip.header.TimeStampHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.UnsupportedHeader;
import javax.sip.header.UserAgentHeader;
import javax.sip.header.ViaHeader;
import javax.sip.header.WWWAuthenticateHeader;
import javax.sip.header.WarningHeader;

public class HeaderFactoryImpl implements HeaderFactory, HeaderFactoryExt {
    private boolean stripAddressScopeZones;

    @Override
    public void setPrettyEncoding(boolean z) {
        SIPHeaderList.setPrettyEncode(z);
    }

    @Override
    public AcceptEncodingHeader createAcceptEncodingHeader(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("the encoding parameter is null");
        }
        AcceptEncoding acceptEncoding = new AcceptEncoding();
        acceptEncoding.setEncoding(str);
        return acceptEncoding;
    }

    @Override
    public AcceptHeader createAcceptHeader(String str, String str2) throws ParseException {
        if (str == null || str2 == null) {
            throw new NullPointerException("contentType or subtype is null ");
        }
        Accept accept = new Accept();
        accept.setContentType(str);
        accept.setContentSubType(str2);
        return accept;
    }

    @Override
    public AcceptLanguageHeader createAcceptLanguageHeader(Locale locale) {
        if (locale == null) {
            throw new NullPointerException("null arg");
        }
        AcceptLanguage acceptLanguage = new AcceptLanguage();
        acceptLanguage.setAcceptLanguage(locale);
        return acceptLanguage;
    }

    @Override
    public AlertInfoHeader createAlertInfoHeader(URI uri) {
        if (uri == null) {
            throw new NullPointerException("null arg alertInfo");
        }
        AlertInfo alertInfo = new AlertInfo();
        alertInfo.setAlertInfo(uri);
        return alertInfo;
    }

    @Override
    public AllowEventsHeader createAllowEventsHeader(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("null arg eventType");
        }
        AllowEvents allowEvents = new AllowEvents();
        allowEvents.setEventType(str);
        return allowEvents;
    }

    @Override
    public AllowHeader createAllowHeader(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("null arg method");
        }
        Allow allow = new Allow();
        allow.setMethod(str);
        return allow;
    }

    @Override
    public AuthenticationInfoHeader createAuthenticationInfoHeader(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("null arg response");
        }
        AuthenticationInfo authenticationInfo = new AuthenticationInfo();
        authenticationInfo.setResponse(str);
        return authenticationInfo;
    }

    @Override
    public AuthorizationHeader createAuthorizationHeader(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("null arg scheme ");
        }
        Authorization authorization = new Authorization();
        authorization.setScheme(str);
        return authorization;
    }

    @Override
    public CSeqHeader createCSeqHeader(long j, String str) throws InvalidArgumentException, ParseException {
        if (j < 0) {
            throw new InvalidArgumentException("bad arg " + j);
        }
        if (str == null) {
            throw new NullPointerException("null arg method");
        }
        CSeq cSeq = new CSeq();
        cSeq.setMethod(str);
        cSeq.setSeqNumber(j);
        return cSeq;
    }

    @Override
    public CSeqHeader createCSeqHeader(int i, String str) throws InvalidArgumentException, ParseException {
        return createCSeqHeader(i, str);
    }

    @Override
    public CallIdHeader createCallIdHeader(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("null arg callId");
        }
        CallID callID = new CallID();
        callID.setCallId(str);
        return callID;
    }

    @Override
    public CallInfoHeader createCallInfoHeader(URI uri) {
        if (uri == null) {
            throw new NullPointerException("null arg callInfo");
        }
        CallInfo callInfo = new CallInfo();
        callInfo.setInfo(uri);
        return callInfo;
    }

    @Override
    public ContactHeader createContactHeader(Address address) {
        if (address == null) {
            throw new NullPointerException("null arg address");
        }
        Contact contact = new Contact();
        contact.setAddress(address);
        return contact;
    }

    @Override
    public ContactHeader createContactHeader() {
        Contact contact = new Contact();
        contact.setWildCardFlag(true);
        contact.setExpires(0);
        return contact;
    }

    @Override
    public ContentDispositionHeader createContentDispositionHeader(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("null arg contentDisposition");
        }
        ContentDisposition contentDisposition = new ContentDisposition();
        contentDisposition.setDispositionType(str);
        return contentDisposition;
    }

    @Override
    public ContentEncodingHeader createContentEncodingHeader(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("null encoding");
        }
        ContentEncoding contentEncoding = new ContentEncoding();
        contentEncoding.setEncoding(str);
        return contentEncoding;
    }

    @Override
    public ContentLanguageHeader createContentLanguageHeader(Locale locale) {
        if (locale == null) {
            throw new NullPointerException("null arg contentLanguage");
        }
        ContentLanguage contentLanguage = new ContentLanguage();
        contentLanguage.setContentLanguage(locale);
        return contentLanguage;
    }

    @Override
    public ContentLengthHeader createContentLengthHeader(int i) throws InvalidArgumentException {
        if (i < 0) {
            throw new InvalidArgumentException("bad contentLength");
        }
        ContentLength contentLength = new ContentLength();
        contentLength.setContentLength(i);
        return contentLength;
    }

    @Override
    public ContentTypeHeader createContentTypeHeader(String str, String str2) throws ParseException {
        if (str == null || str2 == null) {
            throw new NullPointerException("null contentType or subType");
        }
        ContentType contentType = new ContentType();
        contentType.setContentType(str);
        contentType.setContentSubType(str2);
        return contentType;
    }

    @Override
    public DateHeader createDateHeader(Calendar calendar) {
        SIPDateHeader sIPDateHeader = new SIPDateHeader();
        if (calendar == null) {
            throw new NullPointerException("null date");
        }
        sIPDateHeader.setDate(calendar);
        return sIPDateHeader;
    }

    @Override
    public EventHeader createEventHeader(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("null eventType");
        }
        Event event = new Event();
        event.setEventType(str);
        return event;
    }

    @Override
    public ExpiresHeader createExpiresHeader(int i) throws InvalidArgumentException {
        if (i < 0) {
            throw new InvalidArgumentException("bad value " + i);
        }
        Expires expires = new Expires();
        expires.setExpires(i);
        return expires;
    }

    @Override
    public ExtensionHeader createExtensionHeader(String str, String str2) throws ParseException {
        if (str == null) {
            throw new NullPointerException("bad name");
        }
        ExtensionHeaderImpl extensionHeaderImpl = new ExtensionHeaderImpl();
        extensionHeaderImpl.setName(str);
        extensionHeaderImpl.setValue(str2);
        return extensionHeaderImpl;
    }

    @Override
    public FromHeader createFromHeader(Address address, String str) throws ParseException {
        if (address == null) {
            throw new NullPointerException("null address arg");
        }
        From from = new From();
        from.setAddress(address);
        if (str != null) {
            from.setTag(str);
        }
        return from;
    }

    @Override
    public InReplyToHeader createInReplyToHeader(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("null callId arg");
        }
        InReplyTo inReplyTo = new InReplyTo();
        inReplyTo.setCallId(str);
        return inReplyTo;
    }

    @Override
    public MaxForwardsHeader createMaxForwardsHeader(int i) throws InvalidArgumentException {
        if (i < 0 || i > 255) {
            throw new InvalidArgumentException("bad maxForwards arg " + i);
        }
        MaxForwards maxForwards = new MaxForwards();
        maxForwards.setMaxForwards(i);
        return maxForwards;
    }

    @Override
    public MimeVersionHeader createMimeVersionHeader(int i, int i2) throws InvalidArgumentException {
        if (i < 0 || i2 < 0) {
            throw new InvalidArgumentException("bad major/minor version");
        }
        MimeVersion mimeVersion = new MimeVersion();
        mimeVersion.setMajorVersion(i);
        mimeVersion.setMinorVersion(i2);
        return mimeVersion;
    }

    @Override
    public MinExpiresHeader createMinExpiresHeader(int i) throws InvalidArgumentException {
        if (i < 0) {
            throw new InvalidArgumentException("bad minExpires " + i);
        }
        MinExpires minExpires = new MinExpires();
        minExpires.setExpires(i);
        return minExpires;
    }

    public ExtensionHeader createMinSEHeader(int i) throws InvalidArgumentException {
        if (i < 0) {
            throw new InvalidArgumentException("bad value " + i);
        }
        MinSE minSE = new MinSE();
        minSE.setExpires(i);
        return minSE;
    }

    @Override
    public OrganizationHeader createOrganizationHeader(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("bad organization arg");
        }
        Organization organization = new Organization();
        organization.setOrganization(str);
        return organization;
    }

    @Override
    public PriorityHeader createPriorityHeader(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("bad priority arg");
        }
        Priority priority = new Priority();
        priority.setPriority(str);
        return priority;
    }

    @Override
    public ProxyAuthenticateHeader createProxyAuthenticateHeader(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("bad scheme arg");
        }
        ProxyAuthenticate proxyAuthenticate = new ProxyAuthenticate();
        proxyAuthenticate.setScheme(str);
        return proxyAuthenticate;
    }

    @Override
    public ProxyAuthorizationHeader createProxyAuthorizationHeader(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("bad scheme arg");
        }
        ProxyAuthorization proxyAuthorization = new ProxyAuthorization();
        proxyAuthorization.setScheme(str);
        return proxyAuthorization;
    }

    @Override
    public ProxyRequireHeader createProxyRequireHeader(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("bad optionTag arg");
        }
        ProxyRequire proxyRequire = new ProxyRequire();
        proxyRequire.setOptionTag(str);
        return proxyRequire;
    }

    @Override
    public RAckHeader createRAckHeader(long j, long j2, String str) throws InvalidArgumentException, ParseException {
        if (str == null) {
            throw new NullPointerException("Bad method");
        }
        if (j2 < 0 || j < 0) {
            throw new InvalidArgumentException("bad cseq/rseq arg");
        }
        RAck rAck = new RAck();
        rAck.setMethod(str);
        rAck.setCSequenceNumber(j2);
        rAck.setRSequenceNumber(j);
        return rAck;
    }

    @Override
    public RAckHeader createRAckHeader(int i, int i2, String str) throws InvalidArgumentException, ParseException {
        return createRAckHeader(i, i2, str);
    }

    @Override
    public RSeqHeader createRSeqHeader(int i) throws InvalidArgumentException {
        return createRSeqHeader(i);
    }

    @Override
    public RSeqHeader createRSeqHeader(long j) throws InvalidArgumentException {
        if (j < 0) {
            throw new InvalidArgumentException("invalid sequenceNumber arg " + j);
        }
        RSeq rSeq = new RSeq();
        rSeq.setSeqNumber(j);
        return rSeq;
    }

    @Override
    public ReasonHeader createReasonHeader(String str, int i, String str2) throws InvalidArgumentException, ParseException {
        if (str == null) {
            throw new NullPointerException("bad protocol arg");
        }
        if (i < 0) {
            throw new InvalidArgumentException("bad cause");
        }
        Reason reason = new Reason();
        reason.setProtocol(str);
        reason.setCause(i);
        reason.setText(str2);
        return reason;
    }

    @Override
    public RecordRouteHeader createRecordRouteHeader(Address address) {
        if (address == null) {
            throw new NullPointerException("Null argument!");
        }
        RecordRoute recordRoute = new RecordRoute();
        recordRoute.setAddress(address);
        return recordRoute;
    }

    @Override
    public ReplyToHeader createReplyToHeader(Address address) {
        if (address == null) {
            throw new NullPointerException("null address");
        }
        ReplyTo replyTo = new ReplyTo();
        replyTo.setAddress(address);
        return replyTo;
    }

    @Override
    public RequireHeader createRequireHeader(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("null optionTag");
        }
        Require require = new Require();
        require.setOptionTag(str);
        return require;
    }

    @Override
    public RetryAfterHeader createRetryAfterHeader(int i) throws InvalidArgumentException {
        if (i < 0) {
            throw new InvalidArgumentException("bad retryAfter arg");
        }
        RetryAfter retryAfter = new RetryAfter();
        retryAfter.setRetryAfter(i);
        return retryAfter;
    }

    @Override
    public RouteHeader createRouteHeader(Address address) {
        if (address == null) {
            throw new NullPointerException("null address arg");
        }
        Route route = new Route();
        route.setAddress(address);
        return route;
    }

    @Override
    public ServerHeader createServerHeader(List list) throws ParseException {
        if (list == null) {
            throw new NullPointerException("null productList arg");
        }
        Server server = new Server();
        server.setProduct(list);
        return server;
    }

    @Override
    public SubjectHeader createSubjectHeader(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("null subject arg");
        }
        Subject subject = new Subject();
        subject.setSubject(str);
        return subject;
    }

    @Override
    public SubscriptionStateHeader createSubscriptionStateHeader(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("null subscriptionState arg");
        }
        SubscriptionState subscriptionState = new SubscriptionState();
        subscriptionState.setState(str);
        return subscriptionState;
    }

    @Override
    public SupportedHeader createSupportedHeader(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("null optionTag arg");
        }
        Supported supported = new Supported();
        supported.setOptionTag(str);
        return supported;
    }

    @Override
    public TimeStampHeader createTimeStampHeader(float f) throws InvalidArgumentException {
        if (f < 0.0f) {
            throw new IllegalArgumentException("illegal timeStamp");
        }
        TimeStamp timeStamp = new TimeStamp();
        timeStamp.setTimeStamp(f);
        return timeStamp;
    }

    @Override
    public ToHeader createToHeader(Address address, String str) throws ParseException {
        if (address == null) {
            throw new NullPointerException("null address");
        }
        To to = new To();
        to.setAddress(address);
        if (str != null) {
            to.setTag(str);
        }
        return to;
    }

    @Override
    public UnsupportedHeader createUnsupportedHeader(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException(str);
        }
        Unsupported unsupported = new Unsupported();
        unsupported.setOptionTag(str);
        return unsupported;
    }

    @Override
    public UserAgentHeader createUserAgentHeader(List list) throws ParseException {
        if (list == null) {
            throw new NullPointerException("null user agent");
        }
        UserAgent userAgent = new UserAgent();
        userAgent.setProduct(list);
        return userAgent;
    }

    @Override
    public ViaHeader createViaHeader(String str, int i, String str2, String str3) throws InvalidArgumentException, ParseException {
        int iIndexOf;
        if (str == null || str2 == null) {
            throw new NullPointerException("null arg");
        }
        Via via = new Via();
        if (str3 != null) {
            via.setBranch(str3);
        }
        if (str.indexOf(58) >= 0 && str.indexOf(91) < 0) {
            if (this.stripAddressScopeZones && (iIndexOf = str.indexOf(37)) != -1) {
                str = str.substring(0, iIndexOf);
            }
            str = '[' + str + ']';
        }
        via.setHost(str);
        via.setPort(i);
        via.setTransport(str2);
        return via;
    }

    @Override
    public WWWAuthenticateHeader createWWWAuthenticateHeader(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("null scheme");
        }
        WWWAuthenticate wWWAuthenticate = new WWWAuthenticate();
        wWWAuthenticate.setScheme(str);
        return wWWAuthenticate;
    }

    @Override
    public WarningHeader createWarningHeader(String str, int i, String str2) throws InvalidArgumentException, ParseException {
        if (str == null) {
            throw new NullPointerException("null arg");
        }
        Warning warning = new Warning();
        warning.setAgent(str);
        warning.setCode(i);
        warning.setText(str2);
        return warning;
    }

    @Override
    public ErrorInfoHeader createErrorInfoHeader(URI uri) {
        if (uri == null) {
            throw new NullPointerException("null arg");
        }
        return new ErrorInfo((GenericURI) uri);
    }

    @Override
    public Header createHeader(String str) throws ParseException {
        SIPHeader sIPHeader = new StringMsgParser().parseSIPHeader(str.trim());
        if (sIPHeader instanceof SIPHeaderList) {
            SIPHeaderList sIPHeaderList = (SIPHeaderList) sIPHeader;
            if (sIPHeaderList.size() > 1) {
                throw new ParseException("Only singleton allowed " + str, 0);
            }
            if (sIPHeaderList.size() == 0) {
                try {
                    return (Header) ((SIPHeaderList) sIPHeader).getMyClass().newInstance();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    return null;
                } catch (InstantiationException e2) {
                    e2.printStackTrace();
                    return null;
                }
            }
            return sIPHeaderList.getFirst();
        }
        return sIPHeader;
    }

    @Override
    public Header createHeader(String str, String str2) throws ParseException {
        if (str == null) {
            throw new NullPointerException("header name is null");
        }
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(str);
        stringBuffer.append(Separators.COLON);
        stringBuffer.append(str2);
        return createHeader(stringBuffer.toString());
    }

    @Override
    public List createHeaders(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("null arg!");
        }
        SIPHeader sIPHeader = new StringMsgParser().parseSIPHeader(str);
        if (sIPHeader instanceof SIPHeaderList) {
            return (SIPHeaderList) sIPHeader;
        }
        throw new ParseException("List of headers of this type is not allowed in a message", 0);
    }

    @Override
    public ReferToHeader createReferToHeader(Address address) {
        if (address == null) {
            throw new NullPointerException("null address!");
        }
        ReferTo referTo = new ReferTo();
        referTo.setAddress(address);
        return referTo;
    }

    @Override
    public ReferredByHeader createReferredByHeader(Address address) {
        if (address == null) {
            throw new NullPointerException("null address!");
        }
        ReferredBy referredBy = new ReferredBy();
        referredBy.setAddress(address);
        return referredBy;
    }

    @Override
    public ReplacesHeader createReplacesHeader(String str, String str2, String str3) throws ParseException {
        Replaces replaces = new Replaces();
        replaces.setCallId(str);
        replaces.setFromTag(str3);
        replaces.setToTag(str2);
        return replaces;
    }

    @Override
    public JoinHeader createJoinHeader(String str, String str2, String str3) throws ParseException {
        Join join = new Join();
        join.setCallId(str);
        join.setFromTag(str3);
        join.setToTag(str2);
        return join;
    }

    @Override
    public SIPETagHeader createSIPETagHeader(String str) throws ParseException {
        return new SIPETag(str);
    }

    @Override
    public SIPIfMatchHeader createSIPIfMatchHeader(String str) throws ParseException {
        return new SIPIfMatch(str);
    }

    @Override
    public PAccessNetworkInfoHeader createPAccessNetworkInfoHeader() {
        return new PAccessNetworkInfo();
    }

    @Override
    public PAssertedIdentityHeader createPAssertedIdentityHeader(Address address) throws ParseException, NullPointerException {
        if (address == null) {
            throw new NullPointerException("null address!");
        }
        PAssertedIdentity pAssertedIdentity = new PAssertedIdentity();
        pAssertedIdentity.setAddress(address);
        return pAssertedIdentity;
    }

    @Override
    public PAssociatedURIHeader createPAssociatedURIHeader(Address address) {
        if (address == null) {
            throw new NullPointerException("null associatedURI!");
        }
        PAssociatedURI pAssociatedURI = new PAssociatedURI();
        pAssociatedURI.setAddress(address);
        return pAssociatedURI;
    }

    @Override
    public PCalledPartyIDHeader createPCalledPartyIDHeader(Address address) {
        if (address == null) {
            throw new NullPointerException("null address!");
        }
        PCalledPartyID pCalledPartyID = new PCalledPartyID();
        pCalledPartyID.setAddress(address);
        return pCalledPartyID;
    }

    @Override
    public PChargingFunctionAddressesHeader createPChargingFunctionAddressesHeader() {
        return new PChargingFunctionAddresses();
    }

    @Override
    public PChargingVectorHeader createChargingVectorHeader(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("null icid arg!");
        }
        PChargingVector pChargingVector = new PChargingVector();
        pChargingVector.setICID(str);
        return pChargingVector;
    }

    @Override
    public PMediaAuthorizationHeader createPMediaAuthorizationHeader(String str) throws InvalidArgumentException, ParseException {
        if (str == null || str == "") {
            throw new InvalidArgumentException("The Media-Authorization-Token parameter is null or empty");
        }
        PMediaAuthorization pMediaAuthorization = new PMediaAuthorization();
        pMediaAuthorization.setMediaAuthorizationToken(str);
        return pMediaAuthorization;
    }

    @Override
    public PPreferredIdentityHeader createPPreferredIdentityHeader(Address address) {
        if (address == null) {
            throw new NullPointerException("null address!");
        }
        PPreferredIdentity pPreferredIdentity = new PPreferredIdentity();
        pPreferredIdentity.setAddress(address);
        return pPreferredIdentity;
    }

    @Override
    public PVisitedNetworkIDHeader createPVisitedNetworkIDHeader() {
        return new PVisitedNetworkID();
    }

    @Override
    public PathHeader createPathHeader(Address address) {
        if (address == null) {
            throw new NullPointerException("null address!");
        }
        Path path = new Path();
        path.setAddress(address);
        return path;
    }

    @Override
    public PrivacyHeader createPrivacyHeader(String str) {
        if (str == null) {
            throw new NullPointerException("null privacyType arg");
        }
        return new Privacy(str);
    }

    @Override
    public ServiceRouteHeader createServiceRouteHeader(Address address) {
        if (address == null) {
            throw new NullPointerException("null address!");
        }
        ServiceRoute serviceRoute = new ServiceRoute();
        serviceRoute.setAddress(address);
        return serviceRoute;
    }

    @Override
    public SecurityServerHeader createSecurityServerHeader() {
        return new SecurityServer();
    }

    @Override
    public SecurityClientHeader createSecurityClientHeader() {
        return new SecurityClient();
    }

    @Override
    public SecurityVerifyHeader createSecurityVerifyHeader() {
        return new SecurityVerify();
    }

    @Override
    public PUserDatabaseHeader createPUserDatabaseHeader(String str) {
        if (str == null || str.equals(Separators.SP)) {
            throw new NullPointerException("Database name is null");
        }
        PUserDatabase pUserDatabase = new PUserDatabase();
        pUserDatabase.setDatabaseName(str);
        return pUserDatabase;
    }

    @Override
    public PProfileKeyHeader createPProfileKeyHeader(Address address) {
        if (address == null) {
            throw new NullPointerException("Address is null");
        }
        PProfileKey pProfileKey = new PProfileKey();
        pProfileKey.setAddress(address);
        return pProfileKey;
    }

    @Override
    public PServedUserHeader createPServedUserHeader(Address address) {
        if (address == null) {
            throw new NullPointerException("Address is null");
        }
        PServedUser pServedUser = new PServedUser();
        pServedUser.setAddress(address);
        return pServedUser;
    }

    @Override
    public PPreferredServiceHeader createPPreferredServiceHeader() {
        return new PPreferredService();
    }

    @Override
    public PAssertedServiceHeader createPAssertedServiceHeader() {
        return new PAssertedService();
    }

    @Override
    public SessionExpiresHeader createSessionExpiresHeader(int i) throws InvalidArgumentException {
        if (i < 0) {
            throw new InvalidArgumentException("bad value " + i);
        }
        SessionExpires sessionExpires = new SessionExpires();
        sessionExpires.setExpires(i);
        return sessionExpires;
    }

    @Override
    public SipRequestLine createRequestLine(String str) throws ParseException {
        return new RequestLineParser(str).parse();
    }

    @Override
    public SipStatusLine createStatusLine(String str) throws ParseException {
        return new StatusLineParser(str).parse();
    }

    public ReferencesHeader createReferencesHeader(String str, String str2) throws ParseException {
        References references = new References();
        references.setCallId(str);
        references.setRel(str2);
        return references;
    }

    public HeaderFactoryImpl() {
        this.stripAddressScopeZones = false;
        this.stripAddressScopeZones = Boolean.getBoolean("gov.nist.core.STRIP_ADDR_SCOPES");
    }
}
