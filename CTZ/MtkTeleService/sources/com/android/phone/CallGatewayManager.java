package com.android.phone;

import android.content.Intent;
import android.net.Uri;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.Connection;
import java.util.concurrent.ConcurrentHashMap;

public class CallGatewayManager {
    static final String EXTRA_GATEWAY_PROVIDER_PACKAGE = "com.android.phone.extra.GATEWAY_PROVIDER_PACKAGE";
    static final String EXTRA_GATEWAY_URI = "com.android.phone.extra.GATEWAY_URI";
    private static CallGatewayManager sSingleton;
    private final ConcurrentHashMap<Connection, RawGatewayInfo> mMap = new ConcurrentHashMap<>(4, 0.9f, 1);
    private static final String LOG_TAG = CallGatewayManager.class.getSimpleName();
    public static final RawGatewayInfo EMPTY_INFO = new RawGatewayInfo(null, null, null);

    public static synchronized CallGatewayManager getInstance() {
        if (sSingleton == null) {
            sSingleton = new CallGatewayManager();
        }
        return sSingleton;
    }

    private CallGatewayManager() {
    }

    public static RawGatewayInfo getRawGatewayInfo(Intent intent, String str) {
        if (hasPhoneProviderExtras(intent)) {
            return new RawGatewayInfo(intent.getStringExtra(EXTRA_GATEWAY_PROVIDER_PACKAGE), getProviderGatewayUri(intent), str);
        }
        return EMPTY_INFO;
    }

    public void setGatewayInfoForConnection(Connection connection, RawGatewayInfo rawGatewayInfo) {
        if (!rawGatewayInfo.isEmpty()) {
            this.mMap.put(connection, rawGatewayInfo);
        } else {
            this.mMap.remove(connection);
        }
    }

    public void clearGatewayData(Connection connection) {
        setGatewayInfoForConnection(connection, EMPTY_INFO);
    }

    public RawGatewayInfo getGatewayInfo(Connection connection) {
        RawGatewayInfo rawGatewayInfo = this.mMap.get(connection);
        if (rawGatewayInfo != null) {
            return rawGatewayInfo;
        }
        return EMPTY_INFO;
    }

    public static boolean hasPhoneProviderExtras(Intent intent) {
        if (intent == null) {
            return false;
        }
        return (TextUtils.isEmpty(intent.getStringExtra(EXTRA_GATEWAY_PROVIDER_PACKAGE)) || TextUtils.isEmpty(intent.getStringExtra(EXTRA_GATEWAY_URI))) ? false : true;
    }

    public static void checkAndCopyPhoneProviderExtras(Intent intent, Intent intent2) {
        if (!hasPhoneProviderExtras(intent)) {
            Log.d(LOG_TAG, "checkAndCopyPhoneProviderExtras: some or all extras are missing.");
        } else {
            intent2.putExtra(EXTRA_GATEWAY_PROVIDER_PACKAGE, intent.getStringExtra(EXTRA_GATEWAY_PROVIDER_PACKAGE));
            intent2.putExtra(EXTRA_GATEWAY_URI, intent.getStringExtra(EXTRA_GATEWAY_URI));
        }
    }

    public static Uri getProviderGatewayUri(Intent intent) {
        String stringExtra = intent.getStringExtra(EXTRA_GATEWAY_URI);
        if (TextUtils.isEmpty(stringExtra)) {
            return null;
        }
        return Uri.parse(stringExtra);
    }

    public static String formatProviderUri(Uri uri) {
        if (uri != null) {
            if ("tel".equals(uri.getScheme())) {
                return PhoneNumberUtils.formatNumber(uri.getSchemeSpecificPart());
            }
            return uri.toString();
        }
        return null;
    }

    public static class RawGatewayInfo {
        public Uri gatewayUri;
        public String packageName;
        public String trueNumber;

        public RawGatewayInfo(String str, Uri uri, String str2) {
            this.packageName = str;
            this.gatewayUri = uri;
            this.trueNumber = str2;
        }

        public String getFormattedGatewayNumber() {
            return CallGatewayManager.formatProviderUri(this.gatewayUri);
        }

        public boolean isEmpty() {
            return TextUtils.isEmpty(this.packageName) || this.gatewayUri == null;
        }
    }
}
