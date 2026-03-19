package gov.nist.javax.sip.stack;

import gov.nist.core.InternalErrorHandler;
import gov.nist.javax.sip.SipStackImpl;
import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.address.ParameterNames;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.header.RequestLine;
import gov.nist.javax.sip.header.Route;
import gov.nist.javax.sip.header.RouteList;
import gov.nist.javax.sip.message.SIPRequest;
import java.util.LinkedList;
import java.util.ListIterator;
import javax.sip.SipException;
import javax.sip.SipStack;
import javax.sip.address.Hop;
import javax.sip.address.Router;
import javax.sip.address.SipURI;
import javax.sip.address.URI;
import javax.sip.header.ViaHeader;
import javax.sip.message.Request;

public class DefaultRouter implements Router {
    private Hop defaultRoute;
    private SipStackImpl sipStack;

    private DefaultRouter() {
    }

    public DefaultRouter(SipStack sipStack, String str) {
        this.sipStack = (SipStackImpl) sipStack;
        if (str != null) {
            try {
                this.defaultRoute = this.sipStack.getAddressResolver().resolveAddress(new HopImpl(str));
            } catch (IllegalArgumentException e) {
                ((SIPTransactionStack) sipStack).getStackLogger().logError("Invalid default route specification - need host:port/transport");
                throw e;
            }
        }
    }

    @Override
    public Hop getNextHop(Request request) throws SipException {
        SIPRequest sIPRequest = (SIPRequest) request;
        RequestLine requestLine = sIPRequest.getRequestLine();
        if (requestLine == null) {
            return this.defaultRoute;
        }
        URI uri = requestLine.getUri();
        if (uri == null) {
            throw new IllegalArgumentException("Bad message: Null requestURI");
        }
        RouteList routeHeaders = sIPRequest.getRouteHeaders();
        if (routeHeaders != null) {
            URI uri2 = ((Route) routeHeaders.getFirst()).getAddress().getURI();
            if (uri2.isSipURI()) {
                SipURI sipURI = (SipURI) uri2;
                if (!sipURI.hasLrParam()) {
                    fixStrictRouting(sIPRequest);
                    if (this.sipStack.isLoggingEnabled()) {
                        this.sipStack.getStackLogger().logDebug("Route post processing fixed strict routing");
                    }
                }
                Hop hopCreateHop = createHop(sipURI, request);
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("NextHop based on Route:" + hopCreateHop);
                }
                return hopCreateHop;
            }
            throw new SipException("First Route not a SIP URI");
        }
        if (uri.isSipURI()) {
            SipURI sipURI2 = (SipURI) uri;
            if (sipURI2.getMAddrParam() != null) {
                Hop hopCreateHop2 = createHop(sipURI2, request);
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("Using request URI maddr to route the request = " + hopCreateHop2.toString());
                }
                return hopCreateHop2;
            }
        }
        if (this.defaultRoute != null) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("Using outbound proxy to route the request = " + this.defaultRoute.toString());
            }
            return this.defaultRoute;
        }
        if (uri.isSipURI()) {
            Hop hopCreateHop3 = createHop((SipURI) uri, request);
            if (hopCreateHop3 != null && this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("Used request-URI for nextHop = " + hopCreateHop3.toString());
            } else if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("returning null hop -- loop detected");
            }
            return hopCreateHop3;
        }
        InternalErrorHandler.handleException("Unexpected non-sip URI", this.sipStack.getStackLogger());
        return null;
    }

    public void fixStrictRouting(SIPRequest sIPRequest) {
        RouteList routeHeaders = sIPRequest.getRouteHeaders();
        SipUri sipUri = (SipUri) ((Route) routeHeaders.getFirst()).getAddress().getURI();
        routeHeaders.removeFirst();
        AddressImpl addressImpl = new AddressImpl();
        addressImpl.setAddess(sIPRequest.getRequestURI());
        routeHeaders.add(new Route(addressImpl));
        sIPRequest.setRequestURI(sipUri);
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("post: fixStrictRouting" + sIPRequest);
        }
    }

    private final Hop createHop(SipURI sipURI, Request request) {
        int port;
        String transportParam = sipURI.isSecure() ? ParameterNames.TLS : sipURI.getTransportParam();
        if (transportParam == null) {
            transportParam = ((ViaHeader) request.getHeader("Via")).getTransport();
        }
        if (sipURI.getPort() != -1) {
            port = sipURI.getPort();
        } else if (transportParam.equalsIgnoreCase(ParameterNames.TLS)) {
            port = 5061;
        } else {
            port = 5060;
        }
        return this.sipStack.getAddressResolver().resolveAddress(new HopImpl(sipURI.getMAddrParam() != null ? sipURI.getMAddrParam() : sipURI.getHost(), port, transportParam));
    }

    @Override
    public Hop getOutboundProxy() {
        return this.defaultRoute;
    }

    @Override
    public ListIterator getNextHops(Request request) {
        try {
            LinkedList linkedList = new LinkedList();
            linkedList.add(getNextHop(request));
            return linkedList.listIterator();
        } catch (SipException e) {
            return null;
        }
    }
}
