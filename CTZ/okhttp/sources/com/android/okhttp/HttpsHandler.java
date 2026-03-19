package com.android.okhttp;

import java.net.Proxy;
import java.util.Collections;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;

public final class HttpsHandler extends HttpHandler {
    private final ConfigAwareConnectionPool configAwareConnectionPool = ConfigAwareConnectionPool.getInstance();
    private static final ConnectionSpec TLS_CONNECTION_SPEC = ConnectionSpecs.builder(true).allEnabledCipherSuites().allEnabledTlsVersions().supportsTlsExtensions(true).build();
    private static final List<Protocol> HTTP_1_1_ONLY = Collections.singletonList(Protocol.HTTP_1_1);

    @Override
    protected int getDefaultPort() {
        return 443;
    }

    @Override
    protected OkUrlFactory newOkUrlFactory(Proxy proxy) {
        OkUrlFactory okUrlFactoryCreateHttpsOkUrlFactory = createHttpsOkUrlFactory(proxy);
        okUrlFactoryCreateHttpsOkUrlFactory.client().setConnectionPool(this.configAwareConnectionPool.get());
        return okUrlFactoryCreateHttpsOkUrlFactory;
    }

    public static OkUrlFactory createHttpsOkUrlFactory(Proxy proxy) {
        OkUrlFactory okUrlFactoryCreateHttpOkUrlFactory = HttpHandler.createHttpOkUrlFactory(proxy);
        OkUrlFactories.setUrlFilter(okUrlFactoryCreateHttpOkUrlFactory, null);
        OkHttpClient okHttpClientClient = okUrlFactoryCreateHttpOkUrlFactory.client();
        okHttpClientClient.setProtocols(HTTP_1_1_ONLY);
        okHttpClientClient.setConnectionSpecs(Collections.singletonList(TLS_CONNECTION_SPEC));
        okHttpClientClient.setCertificatePinner(CertificatePinner.DEFAULT);
        okUrlFactoryCreateHttpOkUrlFactory.client().setHostnameVerifier(HttpsURLConnection.getDefaultHostnameVerifier());
        okHttpClientClient.setSslSocketFactory(HttpsURLConnection.getDefaultSSLSocketFactory());
        return okUrlFactoryCreateHttpOkUrlFactory;
    }
}
