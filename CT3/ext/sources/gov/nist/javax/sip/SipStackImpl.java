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
import java.lang.reflect.Constructor;
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
        NistSipMessageFactoryImpl msgFactory = new NistSipMessageFactoryImpl(this);
        super.setMessageFactory(msgFactory);
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

    public SipStackImpl(Properties configurationProperties) throws PeerUnavailableException {
        this();
        String address = configurationProperties.getProperty("javax.sip.IP_ADDRESS");
        if (address != null) {
            try {
                super.setHostAddress(address);
            } catch (UnknownHostException e) {
                throw new PeerUnavailableException("bad address " + address);
            }
        }
        String name = configurationProperties.getProperty("javax.sip.STACK_NAME");
        if (name == null) {
            throw new PeerUnavailableException("stack name is missing");
        }
        super.setStackName(name);
        String stackLoggerClassName = configurationProperties.getProperty("gov.nist.javax.sip.STACK_LOGGER");
        stackLoggerClassName = stackLoggerClassName == null ? "gov.nist.core.LogWriter" : stackLoggerClassName;
        try {
            Class<?> stackLoggerClass = Class.forName(stackLoggerClassName);
            Class<?>[] constructorArgs = new Class[0];
            Constructor<?> cons = stackLoggerClass.getConstructor(constructorArgs);
            Object[] args = new Object[0];
            StackLogger stackLogger = (StackLogger) cons.newInstance(args);
            stackLogger.setStackProperties(configurationProperties);
            super.setStackLogger(stackLogger);
            String serverLoggerClassName = configurationProperties.getProperty("gov.nist.javax.sip.SERVER_LOGGER");
            try {
                Class<?> serverLoggerClass = Class.forName(serverLoggerClassName == null ? "gov.nist.javax.sip.stack.ServerLog" : serverLoggerClassName);
                Class<?>[] constructorArgs2 = new Class[0];
                Constructor<?> cons2 = serverLoggerClass.getConstructor(constructorArgs2);
                Object[] args2 = new Object[0];
                this.serverLogger = (ServerLogger) cons2.newInstance(args2);
                this.serverLogger.setSipStack(this);
                this.serverLogger.setStackProperties(configurationProperties);
                this.outboundProxy = configurationProperties.getProperty("javax.sip.OUTBOUND_PROXY");
                this.defaultRouter = new DefaultRouter(this, this.outboundProxy);
                String routerPath = configurationProperties.getProperty("javax.sip.ROUTER_PATH");
                try {
                    Class<?> routerClass = Class.forName(routerPath == null ? "gov.nist.javax.sip.stack.DefaultRouter" : routerPath);
                    Class<?>[] constructorArgs3 = {SipStack.class, String.class};
                    Constructor<?> cons3 = routerClass.getConstructor(constructorArgs3);
                    Object[] args3 = {this, this.outboundProxy};
                    Router router = (Router) cons3.newInstance(args3);
                    super.setRouter(router);
                    String useRouterForAll = configurationProperties.getProperty("javax.sip.USE_ROUTER_FOR_ALL_URIS");
                    this.useRouterForAll = true;
                    if (useRouterForAll != null) {
                        this.useRouterForAll = "true".equalsIgnoreCase(useRouterForAll);
                    }
                    String extensionMethods = configurationProperties.getProperty("javax.sip.EXTENSION_METHODS");
                    if (extensionMethods != null) {
                        StringTokenizer st = new StringTokenizer(extensionMethods);
                        while (st.hasMoreTokens()) {
                            String em = st.nextToken(Separators.COLON);
                            if (em.equalsIgnoreCase("BYE") || em.equalsIgnoreCase("INVITE") || em.equalsIgnoreCase("SUBSCRIBE") || em.equalsIgnoreCase("NOTIFY") || em.equalsIgnoreCase("ACK") || em.equalsIgnoreCase("OPTIONS")) {
                                throw new PeerUnavailableException("Bad extension method " + em);
                            }
                            addExtensionMethod(em);
                        }
                    }
                    String keyStoreFile = configurationProperties.getProperty("javax.net.ssl.keyStore");
                    String trustStoreFile = configurationProperties.getProperty("javax.net.ssl.trustStore");
                    if (keyStoreFile != null) {
                        trustStoreFile = trustStoreFile == null ? keyStoreFile : trustStoreFile;
                        String keyStorePassword = configurationProperties.getProperty("javax.net.ssl.keyStorePassword");
                        try {
                            this.networkLayer = new SslNetworkLayer(trustStoreFile, keyStoreFile, keyStorePassword.toCharArray(), configurationProperties.getProperty("javax.net.ssl.keyStoreType"));
                        } catch (Exception e1) {
                            getStackLogger().logError("could not instantiate SSL networking", e1);
                        }
                    }
                    this.isAutomaticDialogSupportEnabled = configurationProperties.getProperty("javax.sip.AUTOMATIC_DIALOG_SUPPORT", "on").equalsIgnoreCase("on");
                    this.isAutomaticDialogErrorHandlingEnabled = configurationProperties.getProperty("gov.nist.javax.sip.AUTOMATIC_DIALOG_ERROR_HANDLING", "true").equals(Boolean.TRUE.toString());
                    if (this.isAutomaticDialogSupportEnabled) {
                        this.isAutomaticDialogErrorHandlingEnabled = true;
                    }
                    if (configurationProperties.getProperty("gov.nist.javax.sip.MAX_LISTENER_RESPONSE_TIME") != null) {
                        this.maxListenerResponseTime = Integer.parseInt(configurationProperties.getProperty("gov.nist.javax.sip.MAX_LISTENER_RESPONSE_TIME"));
                        if (this.maxListenerResponseTime <= 0) {
                            throw new PeerUnavailableException("Bad configuration parameter gov.nist.javax.sip.MAX_LISTENER_RESPONSE_TIME : should be positive");
                        }
                    } else {
                        this.maxListenerResponseTime = -1;
                    }
                    this.deliverTerminatedEventForAck = configurationProperties.getProperty("gov.nist.javax.sip.DELIVER_TERMINATED_EVENT_FOR_ACK", "false").equalsIgnoreCase("true");
                    this.deliverUnsolicitedNotify = configurationProperties.getProperty("gov.nist.javax.sip.DELIVER_UNSOLICITED_NOTIFY", "false").equalsIgnoreCase("true");
                    String forkedSubscriptions = configurationProperties.getProperty("javax.sip.FORKABLE_EVENTS");
                    if (forkedSubscriptions != null) {
                        StringTokenizer st2 = new StringTokenizer(forkedSubscriptions);
                        while (st2.hasMoreTokens()) {
                            String nextEvent = st2.nextToken();
                            this.forkedEvents.add(nextEvent);
                        }
                    }
                    if (configurationProperties.containsKey("gov.nist.javax.sip.NETWORK_LAYER")) {
                        String path = configurationProperties.getProperty("gov.nist.javax.sip.NETWORK_LAYER");
                        try {
                            Class<?> clazz = Class.forName(path);
                            Constructor<?> c = clazz.getConstructor(new Class[0]);
                            this.networkLayer = (NetworkLayer) c.newInstance(new Object[0]);
                        } catch (Exception e2) {
                            throw new PeerUnavailableException("can't find or instantiate NetworkLayer implementation: " + path);
                        }
                    }
                    if (configurationProperties.containsKey("gov.nist.javax.sip.ADDRESS_RESOLVER")) {
                        String path2 = configurationProperties.getProperty("gov.nist.javax.sip.ADDRESS_RESOLVER");
                        try {
                            Class<?> clazz2 = Class.forName(path2);
                            Constructor<?> c2 = clazz2.getConstructor(new Class[0]);
                            this.addressResolver = (AddressResolver) c2.newInstance(new Object[0]);
                        } catch (Exception e3) {
                            throw new PeerUnavailableException("can't find or instantiate AddressResolver implementation: " + path2);
                        }
                    }
                    String maxConnections = configurationProperties.getProperty("gov.nist.javax.sip.MAX_CONNECTIONS");
                    if (maxConnections != null) {
                        try {
                            this.maxConnections = new Integer(maxConnections).intValue();
                        } catch (NumberFormatException ex) {
                            if (isLoggingEnabled()) {
                                getStackLogger().logError("max connections - bad value " + ex.getMessage());
                            }
                        }
                    }
                    String threadPoolSize = configurationProperties.getProperty("gov.nist.javax.sip.THREAD_POOL_SIZE");
                    if (threadPoolSize != null) {
                        try {
                            this.threadPoolSize = new Integer(threadPoolSize).intValue();
                        } catch (NumberFormatException ex2) {
                            if (isLoggingEnabled()) {
                                getStackLogger().logError("thread pool size - bad value " + ex2.getMessage());
                            }
                        }
                    }
                    String serverTransactionTableSize = configurationProperties.getProperty("gov.nist.javax.sip.MAX_SERVER_TRANSACTIONS");
                    if (serverTransactionTableSize != null) {
                        try {
                            this.serverTransactionTableHighwaterMark = new Integer(serverTransactionTableSize).intValue();
                            this.serverTransactionTableLowaterMark = (this.serverTransactionTableHighwaterMark * 80) / 100;
                        } catch (NumberFormatException ex3) {
                            if (isLoggingEnabled()) {
                                getStackLogger().logError("transaction table size - bad value " + ex3.getMessage());
                            }
                        }
                    } else {
                        this.unlimitedServerTransactionTableSize = true;
                    }
                    String clientTransactionTableSize = configurationProperties.getProperty("gov.nist.javax.sip.MAX_CLIENT_TRANSACTIONS");
                    if (clientTransactionTableSize != null) {
                        try {
                            this.clientTransactionTableHiwaterMark = new Integer(clientTransactionTableSize).intValue();
                            this.clientTransactionTableLowaterMark = (this.clientTransactionTableLowaterMark * 80) / 100;
                        } catch (NumberFormatException ex4) {
                            if (isLoggingEnabled()) {
                                getStackLogger().logError("transaction table size - bad value " + ex4.getMessage());
                            }
                        }
                    } else {
                        this.unlimitedClientTransactionTableSize = true;
                    }
                    this.cacheServerConnections = true;
                    String flag = configurationProperties.getProperty("gov.nist.javax.sip.CACHE_SERVER_CONNECTIONS");
                    if (flag != null && "false".equalsIgnoreCase(flag.trim())) {
                        this.cacheServerConnections = false;
                    }
                    this.cacheClientConnections = true;
                    String cacheflag = configurationProperties.getProperty("gov.nist.javax.sip.CACHE_CLIENT_CONNECTIONS");
                    if (cacheflag != null && "false".equalsIgnoreCase(cacheflag.trim())) {
                        this.cacheClientConnections = false;
                    }
                    String readTimeout = configurationProperties.getProperty("gov.nist.javax.sip.READ_TIMEOUT");
                    if (readTimeout != null) {
                        try {
                            int rt = Integer.parseInt(readTimeout);
                            if (rt >= 100) {
                                this.readTimeout = rt;
                            } else {
                                System.err.println("Value too low " + readTimeout);
                            }
                        } catch (NumberFormatException e4) {
                            if (isLoggingEnabled()) {
                                getStackLogger().logError("Bad read timeout " + readTimeout);
                            }
                        }
                    }
                    String stunAddr = configurationProperties.getProperty("gov.nist.javax.sip.STUN_SERVER");
                    if (stunAddr != null) {
                        getStackLogger().logWarning("Ignoring obsolete property gov.nist.javax.sip.STUN_SERVER");
                    }
                    String maxMsgSize = configurationProperties.getProperty("gov.nist.javax.sip.MAX_MESSAGE_SIZE");
                    try {
                        if (maxMsgSize != null) {
                            this.maxMessageSize = new Integer(maxMsgSize).intValue();
                            if (this.maxMessageSize < 4096) {
                                this.maxMessageSize = 4096;
                            }
                        } else {
                            this.maxMessageSize = 0;
                        }
                    } catch (NumberFormatException ex5) {
                        if (isLoggingEnabled()) {
                            getStackLogger().logError("maxMessageSize - bad value " + ex5.getMessage());
                        }
                    }
                    String rel = configurationProperties.getProperty("gov.nist.javax.sip.REENTRANT_LISTENER");
                    this.reEntrantListener = rel != null ? "true".equalsIgnoreCase(rel) : false;
                    String interval = configurationProperties.getProperty("gov.nist.javax.sip.THREAD_AUDIT_INTERVAL_IN_MILLISECS");
                    if (interval != null) {
                        try {
                            getThreadAuditor().setPingIntervalInMillisecs(Long.valueOf(interval).longValue() / 2);
                        } catch (NumberFormatException ex6) {
                            if (isLoggingEnabled()) {
                                getStackLogger().logError("THREAD_AUDIT_INTERVAL_IN_MILLISECS - bad value [" + interval + "] " + ex6.getMessage());
                            }
                        }
                    }
                    setNon2XXAckPassedToListener(Boolean.valueOf(configurationProperties.getProperty("gov.nist.javax.sip.PASS_INVITE_NON_2XX_ACK_TO_LISTENER", "false")).booleanValue());
                    this.generateTimeStampHeader = Boolean.valueOf(configurationProperties.getProperty("gov.nist.javax.sip.AUTO_GENERATE_TIMESTAMP", "false")).booleanValue();
                    String messageLogFactoryClasspath = configurationProperties.getProperty("gov.nist.javax.sip.LOG_FACTORY");
                    if (messageLogFactoryClasspath != null) {
                        try {
                            Class<?> clazz3 = Class.forName(messageLogFactoryClasspath);
                            Constructor<?> c3 = clazz3.getConstructor(new Class[0]);
                            this.logRecordFactory = (LogRecordFactory) c3.newInstance(new Object[0]);
                        } catch (Exception e5) {
                            if (isLoggingEnabled()) {
                                getStackLogger().logError("Bad configuration value for LOG_FACTORY -- using default logger");
                            }
                            this.logRecordFactory = new DefaultMessageLogFactory();
                        }
                    } else {
                        this.logRecordFactory = new DefaultMessageLogFactory();
                    }
                    boolean computeContentLength = configurationProperties.getProperty("gov.nist.javax.sip.COMPUTE_CONTENT_LENGTH_FROM_MESSAGE_BODY", "false").equalsIgnoreCase("true");
                    StringMsgParser.setComputeContentLengthFromMessage(computeContentLength);
                    String tlsClientProtocols = configurationProperties.getProperty("gov.nist.javax.sip.TLS_CLIENT_PROTOCOLS");
                    if (tlsClientProtocols != null) {
                        StringTokenizer st3 = new StringTokenizer(tlsClientProtocols, " ,");
                        String[] protocols = new String[st3.countTokens()];
                        int i = 0;
                        while (st3.hasMoreTokens()) {
                            protocols[i] = st3.nextToken();
                            i++;
                        }
                        this.enabledProtocols = protocols;
                    }
                    this.rfc2543Supported = configurationProperties.getProperty("gov.nist.javax.sip.RFC_2543_SUPPORT_ENABLED", "true").equalsIgnoreCase("true");
                    this.cancelClientTransactionChecked = configurationProperties.getProperty("gov.nist.javax.sip.CANCEL_CLIENT_TRANSACTION_CHECKED", "true").equalsIgnoreCase("true");
                    this.logStackTraceOnMessageSend = configurationProperties.getProperty("gov.nist.javax.sip.LOG_STACK_TRACE_ON_MESSAGE_SEND", "false").equalsIgnoreCase("true");
                    if (isLoggingEnabled()) {
                        getStackLogger().logDebug("created Sip stack. Properties = " + configurationProperties);
                    }
                    InputStream in = getClass().getResourceAsStream("/TIMESTAMP");
                    if (in != null) {
                        BufferedReader streamReader = new BufferedReader(new InputStreamReader(in));
                        try {
                            String buildTimeStamp = streamReader.readLine();
                            if (in != null) {
                                in.close();
                            }
                            getStackLogger().setBuildTimeStamp(buildTimeStamp);
                        } catch (IOException e6) {
                            getStackLogger().logError("Could not open build timestamp.");
                        }
                    }
                    String bufferSize = configurationProperties.getProperty("gov.nist.javax.sip.RECEIVE_UDP_BUFFER_SIZE", MAX_DATAGRAM_SIZE.toString());
                    int bufferSizeInteger = new Integer(bufferSize).intValue();
                    super.setReceiveUdpBufferSize(bufferSizeInteger);
                    String bufferSize2 = configurationProperties.getProperty("gov.nist.javax.sip.SEND_UDP_BUFFER_SIZE", MAX_DATAGRAM_SIZE.toString());
                    int bufferSizeInteger2 = new Integer(bufferSize2).intValue();
                    super.setSendUdpBufferSize(bufferSizeInteger2);
                    boolean congetstionControlEnabled = Boolean.parseBoolean(configurationProperties.getProperty("gov.nist.javax.sip.CONGESTION_CONTROL_ENABLED", Boolean.TRUE.toString()));
                    this.stackDoesCongestionControl = congetstionControlEnabled;
                    this.isBackToBackUserAgent = Boolean.parseBoolean(configurationProperties.getProperty("gov.nist.javax.sip.IS_BACK_TO_BACK_USER_AGENT", Boolean.FALSE.toString()));
                    this.checkBranchId = Boolean.parseBoolean(configurationProperties.getProperty("gov.nist.javax.sip.REJECT_STRAY_RESPONSES", Boolean.FALSE.toString()));
                    this.isDialogTerminatedEventDeliveredForNullDialog = Boolean.parseBoolean(configurationProperties.getProperty("gov.nist.javax.sip.DELIVER_TERMINATED_EVENT_FOR_NULL_DIALOG", Boolean.FALSE.toString()));
                    this.maxForkTime = Integer.parseInt(configurationProperties.getProperty("gov.nist.javax.sip.MAX_FORK_TIME_SECONDS", "0"));
                } catch (InvocationTargetException ex1) {
                    getStackLogger().logError("could not instantiate router -- invocation target problem", (Exception) ex1.getCause());
                    throw new PeerUnavailableException("Cound not instantiate router - check constructor", ex1);
                } catch (Exception ex7) {
                    getStackLogger().logError("could not instantiate router", (Exception) ex7.getCause());
                    throw new PeerUnavailableException("Could not instantiate router", ex7);
                }
            } catch (InvocationTargetException ex12) {
                throw new IllegalArgumentException("Cound not instantiate server logger " + stackLoggerClassName + "- check that it is present on the classpath and that there is a no-args constructor defined", ex12);
            } catch (Exception ex8) {
                throw new IllegalArgumentException("Cound not instantiate server logger " + stackLoggerClassName + "- check that it is present on the classpath and that there is a no-args constructor defined", ex8);
            }
        } catch (InvocationTargetException ex13) {
            throw new IllegalArgumentException("Cound not instantiate stack logger " + stackLoggerClassName + "- check that it is present on the classpath and that there is a no-args constructor defined", ex13);
        } catch (Exception ex9) {
            throw new IllegalArgumentException("Cound not instantiate stack logger " + stackLoggerClassName + "- check that it is present on the classpath and that there is a no-args constructor defined", ex9);
        }
    }

    @Override
    public synchronized ListeningPoint createListeningPoint(String address, int port, String transport) throws InvalidArgumentException, TransportNotSupportedException {
        MessageProcessor messageProcessor;
        ListeningPointImpl lip;
        if (isLoggingEnabled()) {
            getStackLogger().logDebug("createListeningPoint : address = " + address + " port = " + port + " transport = " + transport);
        }
        if (address == null) {
            throw new NullPointerException("Address for listening point is null!");
        }
        if (transport == null) {
            throw new NullPointerException("null transport");
        }
        if (port <= 0) {
            throw new InvalidArgumentException("bad port");
        }
        if (!transport.equalsIgnoreCase(ListeningPoint.UDP) && !transport.equalsIgnoreCase(ListeningPoint.TLS) && !transport.equalsIgnoreCase(ListeningPoint.TCP) && !transport.equalsIgnoreCase(ListeningPoint.SCTP)) {
            throw new TransportNotSupportedException("bad transport " + transport);
        }
        if (!isAlive()) {
            this.toExit = false;
            reInitialize();
        }
        String key = ListeningPointImpl.makeKey(address, port, transport);
        ListeningPointImpl lip2 = this.listeningPoints.get(key);
        if (lip2 != null) {
            return lip2;
        }
        try {
            InetAddress inetAddr = InetAddress.getByName(address);
            messageProcessor = createMessageProcessor(inetAddr, port, transport);
            if (isLoggingEnabled()) {
                getStackLogger().logDebug("Created Message Processor: " + address + " port = " + port + " transport = " + transport);
            }
            lip = new ListeningPointImpl(this, port, transport);
        } catch (IOException e) {
            ex = e;
        }
        try {
            lip.messageProcessor = messageProcessor;
            messageProcessor.setListeningPoint(lip);
            this.listeningPoints.put(key, lip);
            messageProcessor.start();
            return lip;
        } catch (IOException e2) {
            ex = e2;
            if (isLoggingEnabled()) {
                getStackLogger().logError("Invalid argument address = " + address + " port = " + port + " transport = " + transport);
            }
            throw new InvalidArgumentException(ex.getMessage(), ex);
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
        SipProviderImpl provider = new SipProviderImpl(this);
        provider.setListeningPoint(listeningPointImpl);
        listeningPointImpl.sipProvider = provider;
        this.sipProviders.add(provider);
        return provider;
    }

    @Override
    public void deleteListeningPoint(ListeningPoint listeningPoint) throws ObjectInUseException {
        if (listeningPoint == null) {
            throw new NullPointerException("null listeningPoint arg");
        }
        ListeningPointImpl lip = (ListeningPointImpl) listeningPoint;
        super.removeMessageProcessor(lip.messageProcessor);
        String key = lip.getKey();
        this.listeningPoints.remove(key);
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
        if (!this.sipProviders.isEmpty()) {
            return;
        }
        stopStack();
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
    public ListeningPoint createListeningPoint(int port, String transport) throws InvalidArgumentException, TransportNotSupportedException {
        if (this.stackAddress == null) {
            throw new NullPointerException("Stack does not have a default IP Address!");
        }
        return createListeningPoint(this.stackAddress, port, transport);
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
        if (this.eventScanner != null) {
            return;
        }
        this.eventScanner = new EventScanner(this);
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
    public AuthenticationHelper getSecureAuthenticationHelper(SecureAccountManager accountManager, HeaderFactory headerFactory) {
        return new AuthenticationHelperImpl(this, accountManager, headerFactory);
    }

    @Override
    public void setEnabledCipherSuites(String[] newCipherSuites) {
        this.cipherSuites = newCipherSuites;
    }

    public String[] getEnabledCipherSuites() {
        return this.cipherSuites;
    }

    public void setEnabledProtocols(String[] newProtocols) {
        this.enabledProtocols = newProtocols;
    }

    public String[] getEnabledProtocols() {
        return this.enabledProtocols;
    }

    public void setIsBackToBackUserAgent(boolean flag) {
        this.isBackToBackUserAgent = flag;
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
