package com.android.mms.service;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import com.android.mms.service.MmsConfig;
import com.android.mms.service.exception.MmsHttpException;
import com.android.okhttp.ConnectionPool;
import com.android.okhttp.HostResolver;
import com.android.okhttp.HttpHandler;
import com.android.okhttp.HttpsHandler;
import com.android.okhttp.OkHttpClient;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.SocketFactory;

public class MmsHttpClient {
    private static final Pattern MACRO_P = Pattern.compile("##(\\S+)##");
    private final ConnectionPool mConnectionPool;
    private final Context mContext;
    private final HostResolver mHostResolver;
    private final SocketFactory mSocketFactory;

    public MmsHttpClient(Context context, SocketFactory socketFactory, HostResolver hostResolver, ConnectionPool connectionPool) {
        this.mContext = context;
        this.mSocketFactory = socketFactory;
        this.mHostResolver = hostResolver;
        this.mConnectionPool = connectionPool;
    }

    public byte[] execute(String urlString, byte[] pdu, String method, boolean isProxySet, String proxyHost, int proxyPort, MmsConfig.Overridden mmsConfig) throws MmsHttpException {
        Log.d("MmsService", "HTTP: " + method + " " + urlString + (isProxySet ? ", proxy=" + proxyHost + ":" + proxyPort : "") + ", PDU size=" + (pdu != null ? pdu.length : 0));
        checkMethod(method);
        HttpURLConnection connection = null;
        Proxy proxy = null;
        try {
            if (isProxySet) {
                try {
                    try {
                        Proxy proxy2 = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
                        proxy = proxy2;
                    } catch (ProtocolException e) {
                        Log.e("MmsService", "HTTP: invalid URL protocol " + urlString, e);
                        throw new MmsHttpException(0, "Invalid URL protocol " + urlString, e);
                    }
                } catch (MalformedURLException e2) {
                    Log.e("MmsService", "HTTP: invalid URL " + urlString, e2);
                    throw new MmsHttpException(0, "Invalid URL " + urlString, e2);
                } catch (IOException e3) {
                    Log.e("MmsService", "HTTP: IO failure", e3);
                    throw new MmsHttpException(0, e3);
                }
            }
            URL url = new URL(urlString);
            connection = openConnection(url, proxy);
            connection.setDoInput(true);
            connection.setConnectTimeout(mmsConfig.getHttpSocketTimeout());
            connection.setRequestProperty("Accept", "*/*, application/vnd.wap.mms-message, application/vnd.wap.sic");
            connection.setRequestProperty("Accept-Language", getCurrentAcceptLanguage(Locale.getDefault()));
            String userAgent = mmsConfig.getUserAgent();
            Log.i("MmsService", "HTTP: User-Agent=" + userAgent);
            connection.setRequestProperty("User-Agent", userAgent);
            String uaProfUrlTagName = mmsConfig.getUaProfTagName();
            String uaProfUrl = mmsConfig.getUaProfUrl();
            if (uaProfUrl != null) {
                Log.i("MmsService", "HTTP: UaProfUrl=" + uaProfUrl);
                connection.setRequestProperty(uaProfUrlTagName, uaProfUrl);
            }
            addExtraHeaders(connection, mmsConfig);
            if ("POST".equals(method)) {
                if (pdu == null || pdu.length < 1) {
                    Log.e("MmsService", "HTTP: empty pdu");
                    throw new MmsHttpException(0, "Sending empty PDU");
                }
                connection.setDoOutput(true);
                connection.setRequestMethod("POST");
                if (mmsConfig.getSupportHttpCharsetHeader()) {
                    connection.setRequestProperty("Content-Type", "application/vnd.wap.mms-message; charset=utf-8");
                } else {
                    connection.setRequestProperty("Content-Type", "application/vnd.wap.mms-message");
                }
                if (Log.isLoggable("MmsService", 2)) {
                    logHttpHeaders(connection.getRequestProperties());
                }
                connection.setFixedLengthStreamingMode(pdu.length);
                OutputStream out = new BufferedOutputStream(connection.getOutputStream());
                out.write(pdu);
                out.flush();
                out.close();
            } else if ("GET".equals(method)) {
                if (Log.isLoggable("MmsService", 2)) {
                    logHttpHeaders(connection.getRequestProperties());
                }
                connection.setRequestMethod("GET");
            }
            int responseCode = connection.getResponseCode();
            String responseMessage = connection.getResponseMessage();
            Log.d("MmsService", "HTTP: " + responseCode + " " + responseMessage);
            if (Log.isLoggable("MmsService", 2)) {
                logHttpHeaders(connection.getHeaderFields());
            }
            if (responseCode / 100 != 2) {
                throw new MmsHttpException(responseCode, responseMessage);
            }
            InputStream in = new BufferedInputStream(connection.getInputStream());
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            while (true) {
                int count = in.read(buf);
                if (count <= 0) {
                    break;
                }
                byteOut.write(buf, 0, count);
            }
            in.close();
            byte[] responseBody = byteOut.toByteArray();
            Log.d("MmsService", "HTTP: response size=" + (responseBody != null ? responseBody.length : 0));
            return responseBody;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private HttpURLConnection openConnection(URL url, Proxy proxy) throws MalformedURLException {
        OkHttpClient okHttpClient;
        String protocol = url.getProtocol();
        if (protocol.equals("http")) {
            okHttpClient = HttpHandler.createHttpOkHttpClient(proxy);
        } else if (protocol.equals("https")) {
            okHttpClient = HttpsHandler.createHttpsOkHttpClient(proxy);
        } else {
            throw new MalformedURLException("Invalid URL or unrecognized protocol " + protocol);
        }
        return okHttpClient.setSocketFactory(this.mSocketFactory).setHostResolver(this.mHostResolver).setConnectionPool(this.mConnectionPool).open(url);
    }

    private static void logHttpHeaders(Map<String, List<String>> headers) {
        StringBuilder sb = new StringBuilder();
        if (headers != null) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                String key = entry.getKey();
                List<String> values = entry.getValue();
                if (values != null) {
                    for (String value : values) {
                        sb.append(key).append('=').append(value).append('\n');
                    }
                }
            }
            Log.v("MmsService", "HTTP: headers\n" + sb.toString());
        }
    }

    private static void checkMethod(String method) throws MmsHttpException {
        if (!"GET".equals(method) && !"POST".equals(method)) {
            throw new MmsHttpException(0, "Invalid method " + method);
        }
    }

    public static String getCurrentAcceptLanguage(Locale locale) {
        StringBuilder buffer = new StringBuilder();
        addLocaleToHttpAcceptLanguage(buffer, locale);
        if (!Locale.US.equals(locale)) {
            if (buffer.length() > 0) {
                buffer.append(", ");
            }
            buffer.append("en-US");
        }
        return buffer.toString();
    }

    private static String convertObsoleteLanguageCodeToNew(String langCode) {
        if (langCode == null) {
            return null;
        }
        if ("iw".equals(langCode)) {
            return "he";
        }
        if ("in".equals(langCode)) {
            return "id";
        }
        if ("ji".equals(langCode)) {
            return "yi";
        }
        return langCode;
    }

    private static void addLocaleToHttpAcceptLanguage(StringBuilder builder, Locale locale) {
        String language = convertObsoleteLanguageCodeToNew(locale.getLanguage());
        if (language != null) {
            builder.append(language);
            String country = locale.getCountry();
            if (country != null) {
                builder.append("-");
                builder.append(country);
            }
        }
    }

    private static String resolveMacro(Context context, String value, MmsConfig.Overridden mmsConfig) {
        if (!TextUtils.isEmpty(value)) {
            Matcher matcher = MACRO_P.matcher(value);
            int nextStart = 0;
            StringBuilder replaced = null;
            while (matcher.find()) {
                if (replaced == null) {
                    replaced = new StringBuilder();
                }
                int matchedStart = matcher.start();
                if (matchedStart > nextStart) {
                    replaced.append(value.substring(nextStart, matchedStart));
                }
                String macro = matcher.group(1);
                String macroValue = mmsConfig.getHttpParamMacro(context, macro);
                if (macroValue != null) {
                    replaced.append(macroValue);
                } else {
                    Log.w("MmsService", "HTTP: invalid macro " + macro);
                }
                nextStart = matcher.end();
            }
            if (replaced != null && nextStart < value.length()) {
                replaced.append(value.substring(nextStart));
            }
            return replaced != null ? replaced.toString() : value;
        }
        return value;
    }

    private void addExtraHeaders(HttpURLConnection connection, MmsConfig.Overridden mmsConfig) {
        String extraHttpParams = mmsConfig.getHttpParams();
        if (!TextUtils.isEmpty(extraHttpParams)) {
            String[] paramList = extraHttpParams.split("\\|");
            for (String paramPair : paramList) {
                String[] splitPair = paramPair.split(":", 2);
                if (splitPair.length == 2) {
                    String name = splitPair[0].trim();
                    String value = resolveMacro(this.mContext, splitPair[1].trim(), mmsConfig);
                    if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(value)) {
                        connection.setRequestProperty(name, value);
                    }
                }
            }
        }
    }
}
