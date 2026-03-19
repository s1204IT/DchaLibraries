package gov.nist.javax.sip;

import gov.nist.core.Separators;
import gov.nist.core.ServerLogger;
import gov.nist.core.StackLogger;
import gov.nist.core.net.AddressResolver;
import gov.nist.core.net.NetworkLayer;
import gov.nist.core.net.SslNetworkLayer;
import gov.nist.javax.sip.clientauthutils.AccountManager;
import gov.nist.javax.sip.clientauthutils.AuthenticationHelper;
import gov.nist.javax.sip.clientauthutils.AuthenticationHelperImpl;
import gov.nist.javax.sip.clientauthutils.SecureAccountManager;
import gov.nist.javax.sip.parser.StringMsgParser;
import gov.nist.javax.sip.stack.DefaultMessageLogFactory;
import gov.nist.javax.sip.stack.DefaultRouter;
import gov.nist.javax.sip.stack.MessageProcessor;
import gov.nist.javax.sip.stack.SIPTransactionStack;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import javax.sip.InvalidArgumentException;
import javax.sip.ListeningPoint;
import javax.sip.ObjectInUseException;
import javax.sip.PeerUnavailableException;
import javax.sip.SipException;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.TransportNotSupportedException;
import javax.sip.address.Router;
import javax.sip.header.HeaderFactory;
import org.ccil.cowan.tagsoup.HTMLModels;

public class SipStackImpl extends SIPTransactionStack implements SipStack, SipStackExt {
    public static final Integer MAX_DATAGRAM_SIZE = Integer.valueOf(HTMLModels.M_LEGEND);
    private String[] cipherSuites;
    boolean deliverTerminatedEventForAck;
    boolean deliverUnsolicitedNotify;
    private String[] enabledProtocols;
    private EventScanner eventScanner;
    private Hashtable<String, ListeningPointImpl> listeningPoints;
    boolean reEntrantListener;
    SipListener sipListener;
    private LinkedList<SipProviderImpl> sipProviders;
    private Semaphore stackSemaphore;

    protected SipStackImpl() {
        this.deliverTerminatedEventForAck = false;
        this.deliverUnsolicitedNotify = false;
        this.stackSemaphore = new Semaphore(1);
        this.cipherSuites = new String[]{"TLS_RSA_WITH_AES_128_CBC_SHA", "SSL_RSA_WITH_3DES_EDE_CBC_SHA", "TLS_DH_anon_WITH_AES_128_CBC_SHA", "SSL_DH_anon_WITH_3DES_EDE_CBC_SHA"};
        this.enabledProtocols = new String[]{"SSLv3", "SSLv2Hello", "TLSv1"};
        super.setMessageFactory(new NistSipMessageFactoryImpl(this));
        this.eventScanner = new EventScanner(this);
        this.listeningPoints = new Hashtable<>();
        this.sipProviders = new LinkedList<>();
    }

    private void reInitialize() {
        super.reInit();
        this.eventScanner = new EventScanner(this);
        this.listeningPoints = new Hashtable<>();
        this.sipProviders = new LinkedList<>();
        this.sipListener = null;
    }

    boolean isAutomaticDialogSupportEnabled() {
        return this.isAutomaticDialogSupportEnabled;
    }

    public SipStackImpl(Properties properties) throws PeerUnavailableException {
        this();
        String property = properties.getProperty("javax.sip.IP_ADDRESS");
        if (property != null) {
            try {
                super.setHostAddress(property);
            } catch (UnknownHostException e) {
                throw new PeerUnavailableException("bad address " + property);
            }
        }
        String property2 = properties.getProperty("javax.sip.STACK_NAME");
        if (property2 == null) {
            throw new PeerUnavailableException("stack name is missing");
        }
        super.setStackName(property2);
        String property3 = properties.getProperty("gov.nist.javax.sip.STACK_LOGGER");
        property3 = property3 == null ? "gov.nist.core.LogWriter" : property3;
        try {
            int i = 0;
            StackLogger stackLogger = (StackLogger) Class.forName(property3).getConstructor(new Class[0]).newInstance(new Object[0]);
            stackLogger.setStackProperties(properties);
            super.setStackLogger(stackLogger);
            String property4 = properties.getProperty("gov.nist.javax.sip.SERVER_LOGGER");
            try {
                this.serverLogger = (ServerLogger) Class.forName(property4 == null ? "gov.nist.javax.sip.stack.ServerLog" : property4).getConstructor(new Class[0]).newInstance(new Object[0]);
                this.serverLogger.setSipStack(this);
                this.serverLogger.setStackProperties(properties);
                this.outboundProxy = properties.getProperty("javax.sip.OUTBOUND_PROXY");
                this.defaultRouter = new DefaultRouter(this, this.outboundProxy);
                String property5 = properties.getProperty("javax.sip.ROUTER_PATH");
                try {
                    super.setRouter((Router) Class.forName(property5 == null ? "gov.nist.javax.sip.stack.DefaultRouter" : property5).getConstructor(SipStack.class, String.class).newInstance(this, this.outboundProxy));
                    String property6 = properties.getProperty("javax.sip.USE_ROUTER_FOR_ALL_URIS");
                    this.useRouterForAll = true;
                    if (property6 != null) {
                        this.useRouterForAll = "true".equalsIgnoreCase(property6);
                    }
                    String property7 = properties.getProperty("javax.sip.EXTENSION_METHODS");
                    if (property7 != null) {
                        StringTokenizer stringTokenizer = new StringTokenizer(property7);
                        while (stringTokenizer.hasMoreTokens()) {
                            String strNextToken = stringTokenizer.nextToken(Separators.COLON);
                            if (strNextToken.equalsIgnoreCase("BYE") || strNextToken.equalsIgnoreCase("INVITE") || strNextToken.equalsIgnoreCase("SUBSCRIBE") || strNextToken.equalsIgnoreCase("NOTIFY") || strNextToken.equalsIgnoreCase("ACK") || strNextToken.equalsIgnoreCase("OPTIONS")) {
                                throw new PeerUnavailableException("Bad extension method " + strNextToken);
                            }
                            addExtensionMethod(strNextToken);
                        }
                    }
                    String property8 = properties.getProperty("javax.net.ssl.keyStore");
                    String property9 = properties.getProperty("javax.net.ssl.trustStore");
                    if (property8 != null) {
                        try {
                            this.networkLayer = new SslNetworkLayer(property9 == null ? property8 : property9, property8, properties.getProperty("javax.net.ssl.keyStorePassword").toCharArray(), properties.getProperty("javax.net.ssl.keyStoreType"));
                        } catch (Exception e2) {
                            getStackLogger().logError("could not instantiate SSL networking", e2);
                        }
                    }
                    this.isAutomaticDialogSupportEnabled = properties.getProperty("javax.sip.AUTOMATIC_DIALOG_SUPPORT", "on").equalsIgnoreCase("on");
                    this.isAutomaticDialogErrorHandlingEnabled = properties.getProperty("gov.nist.javax.sip.AUTOMATIC_DIALOG_ERROR_HANDLING", "true").equals(Boolean.TRUE.toString());
                    if (this.isAutomaticDialogSupportEnabled) {
                        this.isAutomaticDialogErrorHandlingEnabled = true;
                    }
                    if (properties.getProperty("gov.nist.javax.sip.MAX_LISTENER_RESPONSE_TIME") != null) {
                        this.maxListenerResponseTime = Integer.parseInt(properties.getProperty("gov.nist.javax.sip.MAX_LISTENER_RESPONSE_TIME"));
                        if (this.maxListenerResponseTime <= 0) {
                            throw new PeerUnavailableException("Bad configuration parameter gov.nist.javax.sip.MAX_LISTENER_RESPONSE_TIME : should be positive");
                        }
                    } else {
                        this.maxListenerResponseTime = -1;
                    }
                    this.deliverTerminatedEventForAck = properties.getProperty("gov.nist.javax.sip.DELIVER_TERMINATED_EVENT_FOR_ACK", "false").equalsIgnoreCase("true");
                    this.deliverUnsolicitedNotify = properties.getProperty("gov.nist.javax.sip.DELIVER_UNSOLICITED_NOTIFY", "false").equalsIgnoreCase("true");
                    String property10 = properties.getProperty("javax.sip.FORKABLE_EVENTS");
                    if (property10 != null) {
                        StringTokenizer stringTokenizer2 = new StringTokenizer(property10);
                        while (stringTokenizer2.hasMoreTokens()) {
                            this.forkedEvents.add(stringTokenizer2.nextToken());
                        }
                    }
                    if (properties.containsKey("gov.nist.javax.sip.NETWORK_LAYER")) {
                        String property11 = properties.getProperty("gov.nist.javax.sip.NETWORK_LAYER");
                        try {
                            this.networkLayer = (NetworkLayer) Class.forName(property11).getConstructor(new Class[0]).newInstance(new Object[0]);
                        } catch (Exception e3) {
                            throw new PeerUnavailableException("can't find or instantiate NetworkLayer implementation: " + property11);
                        }
                    }
                    if (properties.containsKey("gov.nist.javax.sip.ADDRESS_RESOLVER")) {
                        String property12 = properties.getProperty("gov.nist.javax.sip.ADDRESS_RESOLVER");
                        try {
                            this.addressResolver = (AddressResolver) Class.forName(property12).getConstructor(new Class[0]).newInstance(new Object[0]);
                        } catch (Exception e4) {
                            throw new PeerUnavailableException("can't find or instantiate AddressResolver implementation: " + property12);
                        }
                    }
                    String property13 = properties.getProperty("gov.nist.javax.sip.MAX_CONNECTIONS");
                    if (property13 != null) {
                        try {
                            this.maxConnections = new Integer(property13).intValue();
                        } catch (NumberFormatException e5) {
                            if (isLoggingEnabled()) {
                                getStackLogger().logError("max connections - bad value " + e5.getMessage());
                            }
                        }
                    }
                    String property14 = properties.getProperty("gov.nist.javax.sip.THREAD_POOL_SIZE");
                    if (property14 != null) {
                        try {
                            this.threadPoolSize = new Integer(property14).intValue();
                        } catch (NumberFormatException e6) {
                            if (isLoggingEnabled()) {
                                getStackLogger().logError("thread pool size - bad value " + e6.getMessage());
                            }
                        }
                    }
                    String property15 = properties.getProperty("gov.nist.javax.sip.MAX_SERVER_TRANSACTIONS");
                    if (property15 != null) {
                        try {
                            this.serverTransactionTableHighwaterMark = new Integer(property15).intValue();
                            this.serverTransactionTableLowaterMark = (this.serverTransactionTableHighwaterMark * 80) / 100;
                        } catch (NumberFormatException e7) {
                            if (isLoggingEnabled()) {
                                getStackLogger().logError("transaction table size - bad value " + e7.getMessage());
                            }
                        }
                    } else {
                        this.unlimitedServerTransactionTableSize = true;
                    }
                    String property16 = properties.getProperty("gov.nist.javax.sip.MAX_CLIENT_TRANSACTIONS");
                    if (property16 != null) {
                        try {
                            this.clientTransactionTableHiwaterMark = new Integer(property16).intValue();
                            this.clientTransactionTableLowaterMark = (this.clientTransactionTableLowaterMark * 80) / 100;
                        } catch (NumberFormatException e8) {
                            if (isLoggingEnabled()) {
                                getStackLogger().logError("transaction table size - bad value " + e8.getMessage());
                            }
                        }
                    } else {
                        this.unlimitedClientTransactionTableSize = true;
                    }
                    this.cacheServerConnections = true;
                    String property17 = properties.getProperty("gov.nist.javax.sip.CACHE_SERVER_CONNECTIONS");
                    if (property17 != null && "false".equalsIgnoreCase(property17.trim())) {
                        this.cacheServerConnections = false;
                    }
                    this.cacheClientConnections = true;
                    String property18 = properties.getProperty("gov.nist.javax.sip.CACHE_CLIENT_CONNECTIONS");
                    if (property18 != null && "false".equalsIgnoreCase(property18.trim())) {
                        this.cacheClientConnections = false;
                    }
                    String property19 = properties.getProperty("gov.nist.javax.sip.READ_TIMEOUT");
                    if (property19 != null) {
                        try {
                            int i2 = Integer.parseInt(property19);
                            if (i2 >= 100) {
                                this.readTimeout = i2;
                            } else {
                                System.err.println("Value too low " + property19);
                            }
                        } catch (NumberFormatException e9) {
                            if (isLoggingEnabled()) {
                                getStackLogger().logError("Bad read timeout " + property19);
                            }
                        }
                    }
                    if (properties.getProperty("gov.nist.javax.sip.STUN_SERVER") != null) {
                        getStackLogger().logWarning("Ignoring obsolete property gov.nist.javax.sip.STUN_SERVER");
                    }
                    String property20 = properties.getProperty("gov.nist.javax.sip.MAX_MESSAGE_SIZE");
                    try {
                        if (property20 != null) {
                            this.maxMessageSize = new Integer(property20).intValue();
                            if (this.maxMessageSize < 4096) {
                                this.maxMessageSize = 4096;
                            }
                        } else {
                            this.maxMessageSize = 0;
                        }
                    } catch (NumberFormatException e10) {
                        if (isLoggingEnabled()) {
                            getStackLogger().logError("maxMessageSize - bad value " + e10.getMessage());
                        }
                    }
                    String property21 = properties.getProperty("gov.nist.javax.sip.REENTRANT_LISTENER");
                    this.reEntrantListener = property21 != null && "true".equalsIgnoreCase(property21);
                    String property22 = properties.getProperty("gov.nist.javax.sip.THREAD_AUDIT_INTERVAL_IN_MILLISECS");
                    if (property22 != null) {
                        try {
                            getThreadAuditor().setPingIntervalInMillisecs(Long.valueOf(property22).longValue() / 2);
                        } catch (NumberFormatException e11) {
                            if (isLoggingEnabled()) {
                                getStackLogger().logError("THREAD_AUDIT_INTERVAL_IN_MILLISECS - bad value [" + property22 + "] " + e11.getMessage());
                            }
                        }
                    }
                    setNon2XXAckPassedToListener(Boolean.valueOf(properties.getProperty("gov.nist.javax.sip.PASS_INVITE_NON_2XX_ACK_TO_LISTENER", "false")).booleanValue());
                    this.generateTimeStampHeader = Boolean.valueOf(properties.getProperty("gov.nist.javax.sip.AUTO_GENERATE_TIMESTAMP", "false")).booleanValue();
                    String property23 = properties.getProperty("gov.nist.javax.sip.LOG_FACTORY");
                    if (property23 != null) {
                        try {
                            this.logRecordFactory = (LogRecordFactory) Class.forName(property23).getConstructor(new Class[0]).newInstance(new Object[0]);
                        } catch (Exception e12) {
                            if (isLoggingEnabled()) {
                                getStackLogger().logError("Bad configuration value for LOG_FACTORY -- using default logger");
                            }
                            this.logRecordFactory = new DefaultMessageLogFactory();
                        }
                    } else {
                        this.logRecordFactory = new DefaultMessageLogFactory();
                    }
                    StringMsgParser.setComputeContentLengthFromMessage(properties.getProperty("gov.nist.javax.sip.COMPUTE_CONTENT_LENGTH_FROM_MESSAGE_BODY", "false").equalsIgnoreCase("true"));
                    String property24 = properties.getProperty("gov.nist.javax.sip.TLS_CLIENT_PROTOCOLS");
                    if (property24 != null) {
                        StringTokenizer stringTokenizer3 = new StringTokenizer(property24, " ,");
                        String[] strArr = new String[stringTokenizer3.countTokens()];
                        while (stringTokenizer3.hasMoreTokens()) {
                            strArr[i] = stringTokenizer3.nextToken();
                            i++;
                        }
                        this.enabledProtocols = strArr;
                    }
                    this.rfc2543Supported = properties.getProperty("gov.nist.javax.sip.RFC_2543_SUPPORT_ENABLED", "true").equalsIgnoreCase("true");
                    this.cancelClientTransactionChecked = properties.getProperty("gov.nist.javax.sip.CANCEL_CLIENT_TRANSACTION_CHECKED", "true").equalsIgnoreCase("true");
                    this.logStackTraceOnMessageSend = properties.getProperty("gov.nist.javax.sip.LOG_STACK_TRACE_ON_MESSAGE_SEND", "false").equalsIgnoreCase("true");
                    if (isLoggingEnabled()) {
                        getStackLogger().logDebug("created Sip stack. Properties = " + properties);
                    }
                    InputStream resourceAsStream = getClass().getResourceAsStream("/TIMESTAMP");
                    if (resourceAsStream != null) {
                        try {
                            String line = new BufferedReader(new InputStreamReader(resourceAsStream)).readLine();
                            if (resourceAsStream != null) {
                                resourceAsStream.close();
                            }
                            getStackLogger().setBuildTimeStamp(line);
                        } catch (IOException e13) {
                            getStackLogger().logError("Could not open build timestamp.");
                        }
                    }
                    super.setReceiveUdpBufferSize(new Integer(properties.getProperty("gov.nist.javax.sip.RECEIVE_UDP_BUFFER_SIZE", MAX_DATAGRAM_SIZE.toString())).intValue());
                    super.setSendUdpBufferSize(new Integer(properties.getProperty("gov.nist.javax.sip.SEND_UDP_BUFFER_SIZE", MAX_DATAGRAM_SIZE.toString())).intValue());
                    this.stackDoesCongestionControl = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.CONGESTION_CONTROL_ENABLED", Boolean.TRUE.toString()));
                    this.isBackToBackUserAgent = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.IS_BACK_TO_BACK_USER_AGENT", Boolean.FALSE.toString()));
                    this.checkBranchId = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.REJECT_STRAY_RESPONSES", Boolean.FALSE.toString()));
                    this.isDialogTerminatedEventDeliveredForNullDialog = Boolean.parseBoolean(properties.getProperty("gov.nist.javax.sip.DELIVER_TERMINATED_EVENT_FOR_NULL_DIALOG", Boolean.FALSE.toString()));
                    this.maxForkTime = Integer.parseInt(properties.getProperty("gov.nist.javax.sip.MAX_FORK_TIME_SECONDS", "0"));
                } catch (InvocationTargetException e14) {
                    getStackLogger().logError("could not instantiate router -- invocation target problem", (Exception) e14.getCause());
                    throw new PeerUnavailableException("Cound not instantiate router - check constructor", e14);
                } catch (Exception e15) {
                    getStackLogger().logError("could not instantiate router", (Exception) e15.getCause());
                    throw new PeerUnavailableException("Could not instantiate router", e15);
                }
            } catch (InvocationTargetException e16) {
                throw new IllegalArgumentException("Cound not instantiate server logger " + property3 + "- check that it is present on the classpath and that there is a no-args constructor defined", e16);
            } catch (Exception e17) {
                throw new IllegalArgumentException("Cound not instantiate server logger " + property3 + "- check that it is present on the classpath and that there is a no-args constructor defined", e17);
            }
        } catch (InvocationTargetException e18) {
            throw new IllegalArgumentException("Cound not instantiate stack logger " + property3 + "- check that it is present on the classpath and that there is a no-args constructor defined", e18);
        } catch (Exception e19) {
            throw new IllegalArgumentException("Cound not instantiate stack logger " + property3 + "- check that it is present on the classpath and that there is a no-args constructor defined", e19);
        }
    }

    @Override
    public synchronized ListeningPoint createListeningPoint(String str, int i, String str2) throws InvalidArgumentException, TransportNotSupportedException {
        if (isLoggingEnabled()) {
            getStackLogger().logDebug("createListeningPoint : address = " + str + " port = " + i + " transport = " + str2);
        }
        if (str == null) {
            throw new NullPointerException("Address for listening point is null!");
        }
        if (str2 == null) {
            throw new NullPointerException("null transport");
        }
        if (i <= 0) {
            throw new InvalidArgumentException("bad port");
        }
        if (!str2.equalsIgnoreCase(ListeningPoint.UDP) && !str2.equalsIgnoreCase(ListeningPoint.TLS) && !str2.equalsIgnoreCase(ListeningPoint.TCP) && !str2.equalsIgnoreCase(ListeningPoint.SCTP)) {
            throw new TransportNotSupportedException("bad transport " + str2);
        }
        if (!isAlive()) {
            this.toExit = false;
            reInitialize();
        }
        String strMakeKey = ListeningPointImpl.makeKey(str, i, str2);
        ListeningPointImpl listeningPointImpl = this.listeningPoints.get(strMakeKey);
        if (listeningPointImpl != null) {
            return listeningPointImpl;
        }
        try {
            MessageProcessor messageProcessorCreateMessageProcessor = createMessageProcessor(InetAddress.getByName(str), i, str2);
            if (isLoggingEnabled()) {
                getStackLogger().logDebug("Created Message Processor: " + str + " port = " + i + " transport = " + str2);
            }
            ListeningPointImpl listeningPointImpl2 = new ListeningPointImpl(this, i, str2);
            listeningPointImpl2.messageProcessor = messageProcessorCreateMessageProcessor;
            messageProcessorCreateMessageProcessor.setListeningPoint(listeningPointImpl2);
            this.listeningPoints.put(strMakeKey, listeningPointImpl2);
            messageProcessorCreateMessageProcessor.start();
            return listeningPointImpl2;
        } catch (IOException e) {
            if (isLoggingEnabled()) {
                getStackLogger().logError("Invalid argument address = " + str + " port = " + i + " transport = " + str2);
            }
            throw new InvalidArgumentException(e.getMessage(), e);
        }
    }

    @Override
    public SipProvider createSipProvider(ListeningPoint listeningPoint) throws ObjectInUseException {
        if (listeningPoint == null) {
            throw new NullPointerException("null listeningPoint");
        }
        if (isLoggingEnabled()) {
            getStackLogger().logDebug("createSipProvider: " + listeningPoint);
        }
        ListeningPointImpl listeningPointImpl = (ListeningPointImpl) listeningPoint;
        if (listeningPointImpl.sipProvider != null) {
            throw new ObjectInUseException("Provider already attached!");
        }
        SipProviderImpl sipProviderImpl = new SipProviderImpl(this);
        sipProviderImpl.setListeningPoint(listeningPointImpl);
        listeningPointImpl.sipProvider = sipProviderImpl;
        this.sipProviders.add(sipProviderImpl);
        return sipProviderImpl;
    }

    @Override
    public void deleteListeningPoint(ListeningPoint listeningPoint) throws ObjectInUseException {
        if (listeningPoint == null) {
            throw new NullPointerException("null listeningPoint arg");
        }
        ListeningPointImpl listeningPointImpl = (ListeningPointImpl) listeningPoint;
        super.removeMessageProcessor(listeningPointImpl.messageProcessor);
        this.listeningPoints.remove(listeningPointImpl.getKey());
    }

    @Override
    public void deleteSipProvider(SipProvider sipProvider) throws ObjectInUseException {
        if (sipProvider == null) {
            throw new NullPointerException("null provider arg");
        }
        SipProviderImpl sipProviderImpl = (SipProviderImpl) sipProvider;
        if (sipProviderImpl.getSipListener() != null) {
            throw new ObjectInUseException("SipProvider still has an associated SipListener!");
        }
        sipProviderImpl.removeListeningPoints();
        sipProviderImpl.stop();
        this.sipProviders.remove(sipProvider);
        if (this.sipProviders.isEmpty()) {
            stopStack();
        }
    }

    @Override
    public String getIPAddress() {
        return super.getHostAddress();
    }

    @Override
    public Iterator getListeningPoints() {
        return this.listeningPoints.values().iterator();
    }

    @Override
    public boolean isRetransmissionFilterActive() {
        return true;
    }

    @Override
    public Iterator<SipProviderImpl> getSipProviders() {
        return this.sipProviders.iterator();
    }

    @Override
    public String getStackName() {
        return this.stackName;
    }

    protected void finalize() {
        stopStack();
    }

    @Override
    public ListeningPoint createListeningPoint(int i, String str) throws InvalidArgumentException, TransportNotSupportedException {
        if (this.stackAddress == null) {
            throw new NullPointerException("Stack does not have a default IP Address!");
        }
        return createListeningPoint(this.stackAddress, i, str);
    }

    @Override
    public void stop() {
        if (isLoggingEnabled()) {
            getStackLogger().logDebug("stopStack -- stoppping the stack");
        }
        stopStack();
        this.sipProviders = new LinkedList<>();
        this.listeningPoints = new Hashtable<>();
        if (this.eventScanner != null) {
            this.eventScanner.forceStop();
        }
        this.eventScanner = null;
    }

    @Override
    public void start() throws SipException {
        if (this.eventScanner == null) {
            this.eventScanner = new EventScanner(this);
        }
    }

    public SipListener getSipListener() {
        return this.sipListener;
    }

    public LogRecordFactory getLogRecordFactory() {
        return this.logRecordFactory;
    }

    @Deprecated
    public EventScanner getEventScanner() {
        return this.eventScanner;
    }

    @Override
    public AuthenticationHelper getAuthenticationHelper(AccountManager accountManager, HeaderFactory headerFactory) {
        return new AuthenticationHelperImpl(this, accountManager, headerFactory);
    }

    @Override
    public AuthenticationHelper getSecureAuthenticationHelper(SecureAccountManager secureAccountManager, HeaderFactory headerFactory) {
        return new AuthenticationHelperImpl(this, secureAccountManager, headerFactory);
    }

    @Override
    public void setEnabledCipherSuites(String[] strArr) {
        this.cipherSuites = strArr;
    }

    public String[] getEnabledCipherSuites() {
        return this.cipherSuites;
    }

    public void setEnabledProtocols(String[] strArr) {
        this.enabledProtocols = strArr;
    }

    public String[] getEnabledProtocols() {
        return this.enabledProtocols;
    }

    public void setIsBackToBackUserAgent(boolean z) {
        this.isBackToBackUserAgent = z;
    }

    public boolean isBackToBackUserAgent() {
        return this.isBackToBackUserAgent;
    }

    public boolean isAutomaticDialogErrorHandlingEnabled() {
        return this.isAutomaticDialogErrorHandlingEnabled;
    }

    public boolean acquireSem() {
        try {
            return this.stackSemaphore.tryAcquire(10L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return false;
        }
    }

    public void releaseSem() {
        this.stackSemaphore.release();
    }
}
