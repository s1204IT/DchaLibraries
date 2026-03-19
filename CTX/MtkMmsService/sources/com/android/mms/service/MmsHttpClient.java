package com.android.mms.service;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.dns.ResolvUtil;
import android.os.Bundle;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Base64;
import com.android.mms.service.exception.MmsHttpException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
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

public class MmsHttpClient {
    private static final Pattern MACRO_P = Pattern.compile("##(\\S+)##");
    private final ConnectivityManager mConnectivityManager;
    private final Context mContext;
    private final Network mNetwork;

    public MmsHttpClient(Context context, Network network, ConnectivityManager connectivityManager) {
        this.mContext = context;
        this.mNetwork = ResolvUtil.makeNetworkWithPrivateDnsBypass(network);
        this.mConnectivityManager = connectivityManager;
    }

    public byte[] execute(String str, byte[] bArr, String str2, boolean z, String str3, int i, Bundle bundle, int i2, String str4) throws Throwable {
        String str5;
        HttpURLConnection httpURLConnection;
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP: ");
        sb.append(str2);
        sb.append(" ");
        sb.append(redactUrlForNonVerbose(str));
        if (z) {
            str5 = ", proxy=" + str3 + ":" + i;
        } else {
            str5 = "";
        }
        sb.append(str5);
        sb.append(", PDU size=");
        sb.append(bArr != null ? bArr.length : 0);
        LogUtil.d(str4, sb.toString());
        checkMethod(str2);
        try {
            try {
                try {
                    int dataNetworkType = ((TelephonyManager) this.mContext.getSystemService("phone")).getDataNetworkType(i2);
                    if (dataNetworkType == 1 || dataNetworkType == 2 || dataNetworkType == 7) {
                        LogUtil.d(str4, "networkType=" + dataNetworkType + ", set socket.buffer.size");
                        System.setProperty("socket.buffer.size", "8192");
                    } else {
                        System.setProperty("socket.buffer.size", "");
                    }
                    Proxy proxy = Proxy.NO_PROXY;
                    if (z) {
                        proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(this.mNetwork.getByName(str3), i));
                    }
                    try {
                        URL url = new URL(str);
                        maybeWaitForIpv4(str4, url);
                        httpURLConnection = (HttpURLConnection) this.mNetwork.openConnection(url, proxy);
                    } catch (MalformedURLException e) {
                        e = e;
                    } catch (ProtocolException e2) {
                        e = e2;
                    }
                } catch (Throwable th) {
                    th = th;
                    httpURLConnection = null;
                }
            } catch (IOException e3) {
                e = e3;
            }
        } catch (MalformedURLException e4) {
            e = e4;
        } catch (ProtocolException e5) {
            e = e5;
        }
        try {
            httpURLConnection.setDoInput(true);
            httpURLConnection.setConnectTimeout(bundle.getInt("httpSocketTimeout"));
            httpURLConnection.setReadTimeout(90000);
            httpURLConnection.setWriteTimeout(180000);
            httpURLConnection.setRequestProperty("Accept", "*/*, application/vnd.wap.mms-message, application/vnd.wap.sic");
            httpURLConnection.setRequestProperty("Accept-Language", getCurrentAcceptLanguage(Locale.getDefault()));
            String string = bundle.getString("userAgent");
            LogUtil.i(str4, "HTTP: User-Agent=" + string);
            httpURLConnection.setRequestProperty("User-Agent", string);
            String string2 = bundle.getString("uaProfTagName");
            String string3 = bundle.getString("uaProfUrl");
            if (string3 != null) {
                LogUtil.i(str4, "HTTP: UaProfUrl=" + string3);
                httpURLConnection.setRequestProperty(string2, string3);
            }
            if (bundle.getBoolean("mmsCloseConnection", false)) {
                LogUtil.i(str4, "HTTP: Connection close after request");
                httpURLConnection.setRequestProperty("Connection", "close");
            }
            addExtraHeaders(httpURLConnection, bundle, i2);
            if ("POST".equals(str2)) {
                if (bArr == null || bArr.length < 1) {
                    LogUtil.e(str4, "HTTP: empty pdu");
                    throw new MmsHttpException(0, "Sending empty PDU");
                }
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setRequestMethod("POST");
                if (bundle.getBoolean("supportHttpCharsetHeader")) {
                    httpURLConnection.setRequestProperty("Content-Type", "application/vnd.wap.mms-message; charset=utf-8");
                } else {
                    httpURLConnection.setRequestProperty("Content-Type", "application/vnd.wap.mms-message");
                }
                if (LogUtil.isLoggable(2)) {
                    logHttpHeaders(httpURLConnection.getRequestProperties(), str4);
                }
                httpURLConnection.setFixedLengthStreamingMode(bArr.length);
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(httpURLConnection.getOutputStream());
                bufferedOutputStream.write(bArr);
                bufferedOutputStream.flush();
                bufferedOutputStream.close();
            } else if ("GET".equals(str2)) {
                if (LogUtil.isLoggable(2)) {
                    logHttpHeaders(httpURLConnection.getRequestProperties(), str4);
                }
                httpURLConnection.setRequestMethod("GET");
            }
            int responseCode = httpURLConnection.getResponseCode();
            String responseMessage = httpURLConnection.getResponseMessage();
            LogUtil.d(str4, "HTTP: " + responseCode + " " + responseMessage);
            if (LogUtil.isLoggable(2)) {
                logHttpHeaders(httpURLConnection.getHeaderFields(), str4);
            }
            if (responseCode / 100 != 2) {
                throw new MmsHttpException(responseCode, responseMessage);
            }
            BufferedInputStream bufferedInputStream = new BufferedInputStream(httpURLConnection.getInputStream());
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] bArr2 = new byte[4096];
            while (true) {
                int i3 = bufferedInputStream.read(bArr2);
                if (i3 <= 0) {
                    break;
                }
                byteArrayOutputStream.write(bArr2, 0, i3);
            }
            bufferedInputStream.close();
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            StringBuilder sb2 = new StringBuilder();
            sb2.append("HTTP: response size=");
            sb2.append(byteArray != null ? byteArray.length : 0);
            LogUtil.d(str4, sb2.toString());
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
            return byteArray;
        } catch (MalformedURLException e6) {
            e = e6;
            String strRedactUrlForNonVerbose = redactUrlForNonVerbose(str);
            LogUtil.e(str4, "HTTP: invalid URL " + strRedactUrlForNonVerbose, e);
            throw new MmsHttpException(0, "Invalid URL " + strRedactUrlForNonVerbose, e);
        } catch (ProtocolException e7) {
            e = e7;
            String strRedactUrlForNonVerbose2 = redactUrlForNonVerbose(str);
            LogUtil.e(str4, "HTTP: invalid URL protocol " + strRedactUrlForNonVerbose2, e);
            throw new MmsHttpException(0, "Invalid URL protocol " + strRedactUrlForNonVerbose2, e);
        } catch (IOException e8) {
            e = e8;
            LogUtil.e(str4, "HTTP: IO failure", e);
            throw new MmsHttpException(0, e);
        } catch (Throwable th2) {
            th = th2;
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
            throw th;
        }
    }

    private void maybeWaitForIpv4(String str, URL url) {
        Inet4Address inet4Address;
        try {
            inet4Address = (Inet4Address) InetAddress.parseNumericAddress(url.getHost());
        } catch (ClassCastException | IllegalArgumentException e) {
            inet4Address = null;
        }
        if (inet4Address == null) {
            return;
        }
        for (int i = 0; i < 15; i++) {
            LinkProperties linkProperties = this.mConnectivityManager.getLinkProperties(this.mNetwork);
            if (linkProperties != null) {
                if (!linkProperties.isReachable(inet4Address)) {
                    LogUtil.w(str, "HTTP: IPv4 not yet provisioned");
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException e2) {
                    }
                } else {
                    LogUtil.i(str, "HTTP: IPv4 provisioned");
                    return;
                }
            } else {
                LogUtil.w(str, "HTTP: network disconnected, skip ipv4 check");
                return;
            }
        }
    }

    private static void logHttpHeaders(Map<String, List<String>> map, String str) {
        StringBuilder sb = new StringBuilder();
        if (map != null) {
            for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                String key = entry.getKey();
                List<String> value = entry.getValue();
                if (value != null) {
                    for (String str2 : value) {
                        sb.append(key);
                        sb.append('=');
                        sb.append(str2);
                        sb.append('\n');
                    }
                }
            }
            LogUtil.v(str, "HTTP: headers\n" + sb.toString());
        }
    }

    private static void checkMethod(String str) throws MmsHttpException {
        if (!"GET".equals(str) && !"POST".equals(str)) {
            throw new MmsHttpException(0, "Invalid method " + str);
        }
    }

    public static String getCurrentAcceptLanguage(Locale locale) {
        StringBuilder sb = new StringBuilder();
        addLocaleToHttpAcceptLanguage(sb, locale);
        if (!Locale.US.equals(locale)) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("en-US");
        }
        return sb.toString();
    }

    private static String convertObsoleteLanguageCodeToNew(String str) {
        if (str == null) {
            return null;
        }
        if ("iw".equals(str)) {
            return "he";
        }
        if ("in".equals(str)) {
            return "id";
        }
        if ("ji".equals(str)) {
            return "yi";
        }
        return str;
    }

    private static void addLocaleToHttpAcceptLanguage(StringBuilder sb, Locale locale) {
        String strConvertObsoleteLanguageCodeToNew = convertObsoleteLanguageCodeToNew(locale.getLanguage());
        if (strConvertObsoleteLanguageCodeToNew != null) {
            sb.append(strConvertObsoleteLanguageCodeToNew);
            String country = locale.getCountry();
            if (country != null) {
                sb.append("-");
                sb.append(country);
            }
        }
    }

    private void addExtraHeaders(HttpURLConnection httpURLConnection, Bundle bundle, int i) {
        String string = bundle.getString("httpParams");
        if (!TextUtils.isEmpty(string)) {
            for (String str : string.split("\\|")) {
                String[] strArrSplit = str.split(":", 2);
                if (strArrSplit.length == 2) {
                    String strTrim = strArrSplit[0].trim();
                    String strResolveMacro = resolveMacro(this.mContext, strArrSplit[1].trim(), bundle, i);
                    if (!TextUtils.isEmpty(strTrim) && !TextUtils.isEmpty(strResolveMacro)) {
                        httpURLConnection.setRequestProperty(strTrim, strResolveMacro);
                    }
                }
            }
        }
    }

    private static String resolveMacro(Context context, String str, Bundle bundle, int i) {
        if (TextUtils.isEmpty(str)) {
            return str;
        }
        Matcher matcher = MACRO_P.matcher(str);
        int iEnd = 0;
        StringBuilder sb = null;
        while (matcher.find()) {
            if (sb == null) {
                sb = new StringBuilder();
            }
            int iStart = matcher.start();
            if (iStart > iEnd) {
                sb.append(str.substring(iEnd, iStart));
            }
            String macroValue = getMacroValue(context, matcher.group(1), bundle, i);
            if (macroValue != null) {
                sb.append(macroValue);
            }
            iEnd = matcher.end();
        }
        if (sb != null && iEnd < str.length()) {
            sb.append(str.substring(iEnd));
        }
        return sb == null ? str : sb.toString();
    }

    public static String redactUrlForNonVerbose(String str) {
        String protocol;
        String host;
        if (LogUtil.isLoggable(2) || TextUtils.isEmpty(str)) {
            return str;
        }
        try {
            URL url = new URL(str);
            protocol = url.getProtocol();
            try {
                host = url.getHost();
            } catch (MalformedURLException e) {
                host = "";
            }
        } catch (MalformedURLException e2) {
            protocol = "http";
        }
        return protocol + "://" + host + "[" + str.length() + "]";
    }

    private static String getMacroValue(Context context, String str, Bundle bundle, int i) {
        if ("LINE1".equals(str)) {
            return getLine1(context, i);
        }
        if ("LINE1NOCOUNTRYCODE".equals(str)) {
            return getLine1NoCountryCode(context, i);
        }
        if ("NAI".equals(str)) {
            return getNai(context, bundle, i);
        }
        LogUtil.e("Invalid macro " + str);
        return null;
    }

    private static String getLine1(Context context, int i) {
        return ((TelephonyManager) context.getSystemService("phone")).getLine1Number(i);
    }

    private static String getLine1NoCountryCode(Context context, int i) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService("phone");
        return PhoneUtils.getNationalNumber(telephonyManager, i, telephonyManager.getLine1Number(i));
    }

    private static String getNai(Context context, Bundle bundle, int i) {
        byte[] bArrEncode;
        String nai = ((TelephonyManager) context.getSystemService("phone")).getNai(SubscriptionManager.getSlotIndex(i));
        if (LogUtil.isLoggable(2)) {
            LogUtil.v("getNai: nai=" + nai);
        }
        if (!TextUtils.isEmpty(nai)) {
            String string = bundle.getString("naiSuffix");
            if (!TextUtils.isEmpty(string)) {
                nai = nai + string;
            }
            try {
                bArrEncode = Base64.encode(nai.getBytes("UTF-8"), 2);
            } catch (UnsupportedEncodingException e) {
                bArrEncode = Base64.encode(nai.getBytes(), 2);
            }
            try {
                return new String(bArrEncode, "UTF-8");
            } catch (UnsupportedEncodingException e2) {
                return new String(bArrEncode);
            }
        }
        return nai;
    }
}
