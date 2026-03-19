package gov.nist.javax.sip.stack;

import gov.nist.core.InternalErrorHandler;
import gov.nist.core.net.AddressResolver;
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

    public DefaultRouter(SipStack sipStack, String defaultRoute) {
        this.sipStack = (SipStackImpl) sipStack;
        if (defaultRoute == null) {
            return;
        }
        try {
            this.defaultRoute = this.sipStack.getAddressResolver().resolveAddress(new HopImpl(defaultRoute));
        } catch (IllegalArgumentException ex) {
            ((SIPTransactionStack) sipStack).getStackLogger().logError("Invalid default route specification - need host:port/transport");
            throw ex;
        }
    }

    @Override
    public Hop getNextHop(Request request) throws SipException {
        SIPRequest sipRequest = (SIPRequest) request;
        RequestLine requestLine = sipRequest.getRequestLine();
        if (requestLine == null) {
            return this.defaultRoute;
        }
        URI requestURI = requestLine.getUri();
        if (requestURI == null) {
            throw new IllegalArgumentException("Bad message: Null requestURI");
        }
        RouteList routes = sipRequest.getRouteHeaders();
        if (routes != null) {
            Route route = (Route) routes.getFirst();
            URI uri = route.getAddress().getURI();
            if (uri.isSipURI()) {
                SipURI sipUri = (SipURI) uri;
                if (!sipUri.hasLrParam()) {
                    fixStrictRouting(sipRequest);
                    if (this.sipStack.isLoggingEnabled()) {
                        this.sipStack.getStackLogger().logDebug("Route post processing fixed strict routing");
                    }
                }
                Hop hop = createHop(sipUri, request);
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("NextHop based on Route:" + hop);
                }
                return hop;
            }
            throw new SipException("First Route not a SIP URI");
        }
        if (requestURI.isSipURI() && ((SipURI) requestURI).getMAddrParam() != null) {
            Hop hop2 = createHop((SipURI) requestURI, request);
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("Using request URI maddr to route the request = " + hop2.toString());
            }
            return hop2;
        }
        if (this.defaultRoute != null) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("Using outbound proxy to route the request = " + this.defaultRoute.toString());
            }
            return this.defaultRoute;
        }
        if (requestURI.isSipURI()) {
            Hop hop3 = createHop((SipURI) requestURI, request);
            if (hop3 != null && this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("Used request-URI for nextHop = " + hop3.toString());
            } else if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("returning null hop -- loop detected");
            }
            return hop3;
        }
        InternalErrorHandler.handleException("Unexpected non-sip URI", this.sipStack.getStackLogger());
        return null;
    }

    public void fixStrictRouting(SIPRequest req) {
        RouteList routes = req.getRouteHeaders();
        Route first = (Route) routes.getFirst();
        SipUri firstUri = (SipUri) first.getAddress().getURI();
        routes.removeFirst();
        AddressImpl addr = new AddressImpl();
        addr.setAddess(req.getRequestURI());
        Route route = new Route(addr);
        routes.add(route);
        req.setRequestURI(firstUri);
        if (!this.sipStack.isLoggingEnabled()) {
            return;
        }
        this.sipStack.getStackLogger().logDebug("post: fixStrictRouting" + req);
    }

    private final Hop createHop(SipURI sipUri, Request request) {
        int port;
        String transport = sipUri.isSecure() ? ParameterNames.TLS : sipUri.getTransportParam();
        if (transport == null) {
            ViaHeader via = (ViaHeader) request.getHeader("Via");
            transport = via.getTransport();
        }
        if (sipUri.getPort() != -1) {
            port = sipUri.getPort();
        } else if (transport.equalsIgnoreCase(ParameterNames.TLS)) {
            port = 5061;
        } else {
            port = 5060;
        }
        String host = sipUri.getMAddrParam() != null ? sipUri.getMAddrParam() : sipUri.getHost();
        AddressResolver addressResolver = this.sipStack.getAddressResolver();
        return addressResolver.resolveAddress(new HopImpl(host, port, transport));
    }

    @Override
    public Hop getOutboundProxy() {
        return this.defaultRoute;
    }

    @Override
    public ListIterator getNextHops(Request request) {
        try {
            LinkedList llist = new LinkedList();
            llist.add(getNextHop(request));
            return llist.listIterator();
        } catch (SipException e) {
            return null;
        }
    }
}
