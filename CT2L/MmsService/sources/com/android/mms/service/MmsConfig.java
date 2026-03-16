package com.android.mms.service;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import com.android.mms.service.MmsConfigXmlProcessor;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MmsConfig {
    private static final Map<String, Object> DEFAULTS = new ConcurrentHashMap();
    private final int mSubId;
    private String mUserAgent = null;
    private String mUaProfUrl = null;
    private final Map<String, Object> mKeyValues = new ConcurrentHashMap();

    static {
        DEFAULTS.put("enabledMMS", true);
        DEFAULTS.put("enabledTransID", false);
        DEFAULTS.put("enabledNotifyWapMMSC", false);
        DEFAULTS.put("aliasEnabled", false);
        DEFAULTS.put("allowAttachAudio", true);
        DEFAULTS.put("enableMultipartSMS", true);
        DEFAULTS.put("enableSMSDeliveryReports", true);
        DEFAULTS.put("enableGroupMms", true);
        DEFAULTS.put("supportMmsContentDisposition", true);
        DEFAULTS.put("config_cellBroadcastAppLinks", true);
        DEFAULTS.put("sendMultipartSmsAsSeparateMessages", false);
        DEFAULTS.put("enableMMSReadReports", false);
        DEFAULTS.put("enableMMSDeliveryReports", false);
        DEFAULTS.put("supportHttpCharsetHeader", false);
        DEFAULTS.put("maxMessageSize", 307200);
        DEFAULTS.put("maxImageHeight", 480);
        DEFAULTS.put("maxImageWidth", 640);
        DEFAULTS.put("recipientLimit", Integer.MAX_VALUE);
        DEFAULTS.put("httpSocketTimeout", 60000);
        DEFAULTS.put("aliasMinChars", 2);
        DEFAULTS.put("aliasMaxChars", 48);
        DEFAULTS.put("smsToMmsTextThreshold", -1);
        DEFAULTS.put("smsToMmsTextLengthThreshold", -1);
        DEFAULTS.put("maxMessageTextSize", -1);
        DEFAULTS.put("maxSubjectLength", 40);
        DEFAULTS.put("uaProfTagName", "x-wap-profile");
        DEFAULTS.put("userAgent", "");
        DEFAULTS.put("uaProfUrl", "");
        DEFAULTS.put("httpParams", "");
        DEFAULTS.put("emailGatewayNumber", "");
        DEFAULTS.put("naiSuffix", "");
    }

    public MmsConfig(Context context, int subId) {
        this.mSubId = subId;
        this.mKeyValues.clear();
        this.mKeyValues.putAll(DEFAULTS);
        loadDeviceUaSettings(context);
        Log.v("MmsService", "MmsConfig: mUserAgent=" + this.mUserAgent + ", mUaProfUrl=" + this.mUaProfUrl);
        loadFromResources(context);
        Log.v("MmsService", "MmsConfig: all settings -- " + this.mKeyValues);
    }

    public int getSubId() {
        return this.mSubId;
    }

    public static boolean isValidKey(String key, String type) {
        if (!TextUtils.isEmpty(key) && DEFAULTS.containsKey(key)) {
            Object defVal = DEFAULTS.get(key);
            Class valueType = defVal != null ? defVal.getClass() : String.class;
            if ("int".equals(type)) {
                return valueType == Integer.class;
            }
            if ("bool".equals(type)) {
                return valueType == Boolean.class;
            }
            if ("string".equals(type)) {
                return valueType == String.class;
            }
        }
        return false;
    }

    public Bundle getCarrierConfigValues() {
        Bundle bundle = new Bundle();
        for (Map.Entry<String, Object> entry : this.mKeyValues.entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();
            Class valueType = val != null ? val.getClass() : String.class;
            if (valueType == Integer.class) {
                bundle.putInt(key, ((Integer) val).intValue());
            } else if (valueType == Boolean.class) {
                bundle.putBoolean(key, ((Boolean) val).booleanValue());
            } else if (valueType == String.class) {
                bundle.putString(key, (String) val);
            }
        }
        return bundle;
    }

    private String getNullableStringValue(String key) {
        Object value = this.mKeyValues.get(key);
        if (value != null) {
            return (String) value;
        }
        return null;
    }

    private void update(String key, String value, String type) {
        try {
            if ("int".equals(type)) {
                this.mKeyValues.put(key, Integer.valueOf(Integer.parseInt(value)));
            } else if ("bool".equals(type)) {
                this.mKeyValues.put(key, Boolean.valueOf(Boolean.parseBoolean(value)));
            } else if ("string".equals(type)) {
                this.mKeyValues.put(key, value);
            }
        } catch (NumberFormatException e) {
            Log.e("MmsService", "MmsConfig.update: invalid " + key + "," + value + "," + type);
        }
    }

    private void loadDeviceUaSettings(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService("phone");
        this.mUserAgent = telephonyManager.getMmsUserAgent();
        this.mUaProfUrl = telephonyManager.getMmsUAProfUrl();
    }

    private void loadFromResources(Context context) {
        Log.d("MmsService", "MmsConfig.loadFromResources");
        XmlResourceParser parser = context.getResources().getXml(R.xml.mms_config);
        MmsConfigXmlProcessor processor = MmsConfigXmlProcessor.get(parser);
        processor.setMmsConfigHandler(new MmsConfigXmlProcessor.MmsConfigHandler() {
            @Override
            public void process(String key, String value, String type) {
                MmsConfig.this.update(key, value, type);
            }
        });
        try {
            processor.process();
        } finally {
            parser.close();
        }
    }

    public static class Overridden {
        private final MmsConfig mBase;
        private final Bundle mOverrides;

        public Overridden(MmsConfig base, Bundle overrides) {
            this.mBase = base;
            this.mOverrides = overrides;
        }

        private int getInt(String key) {
            Integer def = (Integer) this.mBase.mKeyValues.get(key);
            return this.mOverrides != null ? this.mOverrides.getInt(key, def.intValue()) : def.intValue();
        }

        private boolean getBoolean(String key) {
            Boolean def = (Boolean) this.mBase.mKeyValues.get(key);
            return this.mOverrides != null ? this.mOverrides.getBoolean(key, def.booleanValue()) : def.booleanValue();
        }

        private String getString(String key) {
            if (this.mOverrides == null || !this.mOverrides.containsKey(key)) {
                return this.mBase.getNullableStringValue(key);
            }
            return this.mOverrides.getString(key);
        }

        public int getMaxMessageSize() {
            return getInt("maxMessageSize");
        }

        public String getUserAgent() {
            if (this.mOverrides == null || !this.mOverrides.containsKey("userAgent")) {
                return !TextUtils.isEmpty(this.mBase.mUserAgent) ? this.mBase.mUserAgent : this.mBase.getNullableStringValue("userAgent");
            }
            return this.mOverrides.getString("userAgent");
        }

        public String getUaProfTagName() {
            return getString("uaProfTagName");
        }

        public String getUaProfUrl() {
            if (this.mOverrides == null || !this.mOverrides.containsKey("uaProfUrl")) {
                return !TextUtils.isEmpty(this.mBase.mUaProfUrl) ? this.mBase.mUaProfUrl : this.mBase.getNullableStringValue("uaProfUrl");
            }
            return this.mOverrides.getString("uaProfUrl");
        }

        public String getHttpParams() {
            return getString("httpParams");
        }

        public int getHttpSocketTimeout() {
            return getInt("httpSocketTimeout");
        }

        public boolean getSupportMmsContentDisposition() {
            return getBoolean("supportMmsContentDisposition");
        }

        public String getNaiSuffix() {
            return getString("naiSuffix");
        }

        public boolean getSupportHttpCharsetHeader() {
            return getBoolean("supportHttpCharsetHeader");
        }

        public String getHttpParamMacro(Context context, String macro) {
            if ("LINE1".equals(macro)) {
                return getLine1(context, this.mBase.getSubId());
            }
            if ("LINE1NOCOUNTRYCODE".equals(macro)) {
                return getLine1NoCountryCode(context, this.mBase.getSubId());
            }
            if ("NAI".equals(macro)) {
                return getNai(context, this.mBase.getSubId());
            }
            return null;
        }

        private static String getLine1(Context context, int subId) {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService("phone");
            return telephonyManager.getLine1NumberForSubscriber(subId);
        }

        private static String getLine1NoCountryCode(Context context, int subId) {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService("phone");
            return PhoneUtils.getNationalNumber(telephonyManager, subId, telephonyManager.getLine1NumberForSubscriber(subId));
        }

        private String getNai(Context context, int subId) {
            byte[] encoded;
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService("phone");
            String nai = telephonyManager.getNai(SubscriptionManager.getSlotId(subId));
            if (Log.isLoggable("MmsService", 2)) {
                Log.v("MmsService", "MmsConfig.getNai: nai=" + nai);
            }
            if (!TextUtils.isEmpty(nai)) {
                String naiSuffix = getNaiSuffix();
                if (!TextUtils.isEmpty(naiSuffix)) {
                    nai = nai + naiSuffix;
                }
                try {
                    encoded = Base64.encode(nai.getBytes("UTF-8"), 2);
                } catch (UnsupportedEncodingException e) {
                    encoded = Base64.encode(nai.getBytes(), 2);
                }
                try {
                    return new String(encoded, "UTF-8");
                } catch (UnsupportedEncodingException e2) {
                    return new String(encoded);
                }
            }
            return nai;
        }
    }
}
