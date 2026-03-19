package com.android.internal.telephony;

import android.app.AlarmManager;
import android.app.DownloadManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.telephony.CarrierConfigManager;
import android.telephony.ImsiEncryptionInfo;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import com.android.internal.annotations.VisibleForTesting;
import com.android.org.bouncycastle.util.io.pem.PemReader;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CarrierKeyDownloadManager {
    private static final int[] CARRIER_KEY_TYPES = {1, 2};
    private static final int DAY_IN_MILLIS = 86400000;
    private static final int END_RENEWAL_WINDOW_DAYS = 7;
    private static final String INTENT_KEY_RENEWAL_ALARM_PREFIX = "com.android.internal.telephony.carrier_key_download_alarm";
    private static final String JSON_CARRIER_KEYS = "carrier-keys";
    private static final String JSON_CERTIFICATE = "certificate";
    private static final String JSON_CERTIFICATE_ALTERNATE = "public-key";
    private static final String JSON_IDENTIFIER = "key-identifier";
    private static final String JSON_TYPE = "key-type";
    private static final String JSON_TYPE_VALUE_EPDG = "EPDG";
    private static final String JSON_TYPE_VALUE_WLAN = "WLAN";
    private static final String LOG_TAG = "CarrierKeyDownloadManager";
    public static final String MCC = "MCC";
    private static final String MCC_MNC_PREF_TAG = "CARRIER_KEY_DM_MCC_MNC";
    public static final String MNC = "MNC";
    private static final String SEPARATOR = ":";
    private static final int START_RENEWAL_WINDOW_DAYS = 21;
    private static final int UNINITIALIZED_KEY_TYPE = -1;
    private final Context mContext;
    public final DownloadManager mDownloadManager;
    private final Phone mPhone;
    private String mURL;

    @VisibleForTesting
    public int mKeyAvailability = 0;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) throws Throwable {
            String action = intent.getAction();
            int phoneId = CarrierKeyDownloadManager.this.mPhone.getPhoneId();
            if (action.equals(CarrierKeyDownloadManager.INTENT_KEY_RENEWAL_ALARM_PREFIX + phoneId)) {
                Log.d(CarrierKeyDownloadManager.LOG_TAG, "Handling key renewal alarm: " + action);
                CarrierKeyDownloadManager.this.handleAlarmOrConfigChange();
                return;
            }
            if (action.equals("com.android.internal.telephony.ACTION_CARRIER_CERTIFICATE_DOWNLOAD")) {
                if (phoneId == intent.getIntExtra("phone", -1)) {
                    Log.d(CarrierKeyDownloadManager.LOG_TAG, "Handling reset intent: " + action);
                    CarrierKeyDownloadManager.this.handleAlarmOrConfigChange();
                    return;
                }
                return;
            }
            if (action.equals("android.telephony.action.CARRIER_CONFIG_CHANGED")) {
                if (phoneId == intent.getIntExtra("phone", -1)) {
                    Log.d(CarrierKeyDownloadManager.LOG_TAG, "Carrier Config changed: " + action);
                    CarrierKeyDownloadManager.this.handleAlarmOrConfigChange();
                    return;
                }
                return;
            }
            if (action.equals("android.intent.action.DOWNLOAD_COMPLETE")) {
                Log.d(CarrierKeyDownloadManager.LOG_TAG, "Download Complete");
                long longExtra = intent.getLongExtra("extra_download_id", 0L);
                String mccMncSetFromPref = CarrierKeyDownloadManager.this.getMccMncSetFromPref();
                if (CarrierKeyDownloadManager.this.isValidDownload(mccMncSetFromPref)) {
                    CarrierKeyDownloadManager.this.onDownloadComplete(longExtra, mccMncSetFromPref);
                    CarrierKeyDownloadManager.this.onPostDownloadProcessing(longExtra);
                }
            }
        }
    };

    public CarrierKeyDownloadManager(Phone phone) {
        this.mPhone = phone;
        this.mContext = phone.getContext();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.telephony.action.CARRIER_CONFIG_CHANGED");
        intentFilter.addAction("android.intent.action.DOWNLOAD_COMPLETE");
        intentFilter.addAction(INTENT_KEY_RENEWAL_ALARM_PREFIX + this.mPhone.getPhoneId());
        intentFilter.addAction("com.android.internal.telephony.ACTION_CARRIER_CERTIFICATE_DOWNLOAD");
        this.mContext.registerReceiver(this.mBroadcastReceiver, intentFilter, null, phone);
        this.mDownloadManager = (DownloadManager) this.mContext.getSystemService("download");
    }

    private void onPostDownloadProcessing(long j) {
        resetRenewalAlarm();
        cleanupDownloadPreferences(j);
    }

    private void handleAlarmOrConfigChange() {
        if (carrierUsesKeys()) {
            if (areCarrierKeysAbsentOrExpiring() && !downloadKey()) {
                resetRenewalAlarm();
                return;
            }
            return;
        }
        cleanupRenewalAlarms();
    }

    private void cleanupDownloadPreferences(long j) {
        Log.d(LOG_TAG, "Cleaning up download preferences: " + j);
        SharedPreferences.Editor editorEdit = PreferenceManager.getDefaultSharedPreferences(this.mContext).edit();
        editorEdit.remove(String.valueOf(j));
        editorEdit.commit();
    }

    private void cleanupRenewalAlarms() {
        Log.d(LOG_TAG, "Cleaning up existing renewal alarms");
        PendingIntent broadcast = PendingIntent.getBroadcast(this.mContext, 0, new Intent(INTENT_KEY_RENEWAL_ALARM_PREFIX + this.mPhone.getPhoneId()), 201326592);
        Context context = this.mContext;
        Context context2 = this.mContext;
        ((AlarmManager) context.getSystemService("alarm")).cancel(broadcast);
    }

    @VisibleForTesting
    public long getExpirationDate() {
        ImsiEncryptionInfo carrierInfoForImsiEncryption;
        long time = Long.MAX_VALUE;
        for (int i : CARRIER_KEY_TYPES) {
            if (isKeyEnabled(i) && (carrierInfoForImsiEncryption = this.mPhone.getCarrierInfoForImsiEncryption(i)) != null && carrierInfoForImsiEncryption.getExpirationTime() != null && time > carrierInfoForImsiEncryption.getExpirationTime().getTime()) {
                time = carrierInfoForImsiEncryption.getExpirationTime().getTime();
            }
        }
        if (time == Long.MAX_VALUE || time < System.currentTimeMillis() + 604800000) {
            return System.currentTimeMillis() + 86400000;
        }
        return time - ((long) (new Random().nextInt(1209600000) + 604800000));
    }

    @VisibleForTesting
    public void resetRenewalAlarm() {
        cleanupRenewalAlarms();
        int phoneId = this.mPhone.getPhoneId();
        long expirationDate = getExpirationDate();
        Log.d(LOG_TAG, "minExpirationDate: " + new Date(expirationDate));
        AlarmManager alarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        Intent intent = new Intent(INTENT_KEY_RENEWAL_ALARM_PREFIX + phoneId);
        alarmManager.set(2, expirationDate, PendingIntent.getBroadcast(this.mContext, 0, intent, 201326592));
        Log.d(LOG_TAG, "setRenewelAlarm: action=" + intent.getAction() + " time=" + new Date(expirationDate));
    }

    private String getMccMncSetFromPref() {
        int phoneId = this.mPhone.getPhoneId();
        return PreferenceManager.getDefaultSharedPreferences(this.mContext).getString(MCC_MNC_PREF_TAG + phoneId, null);
    }

    @VisibleForTesting
    public String getSimOperator() {
        return ((TelephonyManager) this.mContext.getSystemService("phone")).getSimOperator(this.mPhone.getSubId());
    }

    @VisibleForTesting
    public boolean isValidDownload(String str) {
        String simOperator = getSimOperator();
        if (TextUtils.isEmpty(simOperator) || TextUtils.isEmpty(str)) {
            Log.e(LOG_TAG, "simOperator or mcc/mnc is empty");
            return false;
        }
        String[] strArrSplit = str.split(SEPARATOR);
        String str2 = strArrSplit[0];
        String str3 = strArrSplit[1];
        Log.d(LOG_TAG, "values from sharedPrefs mcc, mnc: " + str2 + "," + str3);
        String strSubstring = simOperator.substring(0, 3);
        String strSubstring2 = simOperator.substring(3);
        Log.d(LOG_TAG, "using values for mcc, mnc: " + strSubstring + "," + strSubstring2);
        return TextUtils.equals(str3, strSubstring2) && TextUtils.equals(str2, strSubstring);
    }

    private void onDownloadComplete(long j, String str) throws Throwable {
        FileInputStream fileInputStream;
        Log.d(LOG_TAG, "onDownloadComplete: " + j);
        DownloadManager.Query query = new DownloadManager.Query();
        int i = 1;
        i = 1;
        i = 1;
        i = 1;
        query.setFilterById(j);
        Cursor cursorQuery = this.mDownloadManager.query(query);
        if (cursorQuery == null) {
            return;
        }
        if (cursorQuery.moveToFirst()) {
            ?? r2 = cursorQuery.getInt(cursorQuery.getColumnIndex("status"));
            if (8 == r2) {
                r2 = 0;
                FileInputStream fileInputStream2 = null;
                r2 = 0;
                try {
                    try {
                        try {
                            fileInputStream = new FileInputStream(this.mDownloadManager.openDownloadedFile(j).getFileDescriptor());
                        } catch (Throwable th) {
                            th = th;
                        }
                    } catch (Exception e) {
                        e = e;
                    }
                    try {
                        String strConvertToString = convertToString(fileInputStream);
                        parseJsonAndPersistKey(strConvertToString, str);
                        long[] jArr = {j};
                        this.mDownloadManager.remove(jArr);
                        fileInputStream.close();
                        i = jArr;
                        r2 = strConvertToString;
                    } catch (Exception e2) {
                        e = e2;
                        fileInputStream2 = fileInputStream;
                        Log.e(LOG_TAG, "Error in download:" + j + ". " + e);
                        long[] jArr2 = {j};
                        this.mDownloadManager.remove(jArr2);
                        fileInputStream2.close();
                        i = jArr2;
                        r2 = fileInputStream2;
                    } catch (Throwable th2) {
                        th = th2;
                        r2 = fileInputStream;
                        ?? r0 = this.mDownloadManager;
                        ?? r1 = new long[i];
                        r1[0] = j;
                        r0.remove(r1);
                        try {
                            r2.close();
                        } catch (IOException e3) {
                            e3.printStackTrace();
                        }
                        throw th;
                    }
                } catch (IOException e4) {
                    e4.printStackTrace();
                }
            }
            j = LOG_TAG;
            Log.d(LOG_TAG, "Completed downloading keys");
        }
        cursorQuery.close();
    }

    private boolean carrierUsesKeys() {
        PersistableBundle configForSubId;
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager) this.mContext.getSystemService("carrier_config");
        if (carrierConfigManager == null || (configForSubId = carrierConfigManager.getConfigForSubId(this.mPhone.getSubId())) == null) {
            return false;
        }
        this.mKeyAvailability = configForSubId.getInt("imsi_key_availability_int");
        this.mURL = configForSubId.getString("imsi_key_download_url_string");
        if (TextUtils.isEmpty(this.mURL) || this.mKeyAvailability == 0) {
            Log.d(LOG_TAG, "Carrier not enabled or invalid values");
            return false;
        }
        for (int i : CARRIER_KEY_TYPES) {
            if (isKeyEnabled(i)) {
                return true;
            }
        }
        return false;
    }

    private static String convertToString(InputStream inputStream) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new GZIPInputStream(inputStream), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            while (true) {
                String line = bufferedReader.readLine();
                if (line != null) {
                    sb.append(line);
                    sb.append('\n');
                } else {
                    return sb.toString();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @VisibleForTesting
    public void parseJsonAndPersistKey(String str, String str2) throws Throwable {
        String str3;
        StringBuilder sb;
        if (TextUtils.isEmpty(str) || TextUtils.isEmpty(str2)) {
            Log.e(LOG_TAG, "jsonStr or mcc, mnc: is empty");
            return;
        }
        PemReader pemReader = null;
        try {
            try {
                String[] strArrSplit = str2.split(SEPARATOR);
                int i = 0;
                String str4 = strArrSplit[0];
                String str5 = strArrSplit[1];
                JSONArray jSONArray = new JSONObject(str).getJSONArray(JSON_CARRIER_KEYS);
                while (i < jSONArray.length()) {
                    JSONObject jSONObject = jSONArray.getJSONObject(i);
                    String string = jSONObject.has(JSON_CERTIFICATE) ? jSONObject.getString(JSON_CERTIFICATE) : jSONObject.getString(JSON_CERTIFICATE_ALTERNATE);
                    String string2 = jSONObject.getString(JSON_TYPE);
                    int i2 = string2.equals(JSON_TYPE_VALUE_WLAN) ? 2 : string2.equals(JSON_TYPE_VALUE_EPDG) ? 1 : -1;
                    String string3 = jSONObject.getString(JSON_IDENTIFIER);
                    PemReader pemReader2 = new PemReader(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(string.getBytes()))));
                    try {
                        Pair<PublicKey, Long> keyInformation = getKeyInformation(pemReader2.readPemObject().getContent());
                        pemReader2.close();
                        savePublicKey((PublicKey) keyInformation.first, i2, string3, ((Long) keyInformation.second).longValue(), str4, str5);
                        i++;
                        pemReader = pemReader2;
                    } catch (JSONException e) {
                        e = e;
                        pemReader = pemReader2;
                        Log.e(LOG_TAG, "Json parsing error: " + e.getMessage());
                        if (pemReader != null) {
                            try {
                                pemReader.close();
                                return;
                            } catch (Exception e2) {
                                e = e2;
                                str3 = LOG_TAG;
                                sb = new StringBuilder();
                                sb.append("Exception getting certificate: ");
                                sb.append(e);
                                Log.e(str3, sb.toString());
                            }
                        }
                        return;
                    } catch (Exception e3) {
                        e = e3;
                        pemReader = pemReader2;
                        Log.e(LOG_TAG, "Exception getting certificate: " + e);
                        if (pemReader != null) {
                            try {
                                pemReader.close();
                                return;
                            } catch (Exception e4) {
                                e = e4;
                                str3 = LOG_TAG;
                                sb = new StringBuilder();
                                sb.append("Exception getting certificate: ");
                                sb.append(e);
                                Log.e(str3, sb.toString());
                            }
                        }
                        return;
                    } catch (Throwable th) {
                        th = th;
                        pemReader = pemReader2;
                        if (pemReader != null) {
                            try {
                                pemReader.close();
                            } catch (Exception e5) {
                                Log.e(LOG_TAG, "Exception getting certificate: " + e5);
                            }
                        }
                        throw th;
                    }
                }
                if (pemReader != null) {
                    try {
                        pemReader.close();
                    } catch (Exception e6) {
                        e = e6;
                        str3 = LOG_TAG;
                        sb = new StringBuilder();
                        sb.append("Exception getting certificate: ");
                        sb.append(e);
                        Log.e(str3, sb.toString());
                    }
                }
            } catch (Throwable th2) {
                th = th2;
            }
        } catch (JSONException e7) {
            e = e7;
        } catch (Exception e8) {
            e = e8;
        }
    }

    @VisibleForTesting
    public boolean isKeyEnabled(int i) {
        return ((this.mKeyAvailability >> (i - 1)) & 1) == 1;
    }

    @VisibleForTesting
    public boolean areCarrierKeysAbsentOrExpiring() {
        for (int i : CARRIER_KEY_TYPES) {
            if (isKeyEnabled(i)) {
                ImsiEncryptionInfo carrierInfoForImsiEncryption = this.mPhone.getCarrierInfoForImsiEncryption(i);
                if (carrierInfoForImsiEncryption != null) {
                    return carrierInfoForImsiEncryption.getExpirationTime().getTime() - System.currentTimeMillis() < 1814400000;
                }
                Log.d(LOG_TAG, "Key not found for: " + i);
                return true;
            }
        }
        return false;
    }

    private boolean downloadKey() {
        Log.d(LOG_TAG, "starting download from: " + this.mURL);
        String simOperator = getSimOperator();
        if (!TextUtils.isEmpty(simOperator)) {
            String strSubstring = simOperator.substring(0, 3);
            String strSubstring2 = simOperator.substring(3);
            Log.d(LOG_TAG, "using values for mcc, mnc: " + strSubstring + "," + strSubstring2);
            try {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(this.mURL));
                request.setAllowedOverMetered(false);
                request.setVisibleInDownloadsUi(false);
                request.setNotificationVisibility(2);
                Long lValueOf = Long.valueOf(this.mDownloadManager.enqueue(request));
                SharedPreferences.Editor editorEdit = PreferenceManager.getDefaultSharedPreferences(this.mContext).edit();
                String str = strSubstring + SEPARATOR + strSubstring2;
                int phoneId = this.mPhone.getPhoneId();
                Log.d(LOG_TAG, "storing values in sharedpref mcc, mnc, days: " + strSubstring + "," + strSubstring2 + "," + lValueOf);
                StringBuilder sb = new StringBuilder();
                sb.append(MCC_MNC_PREF_TAG);
                sb.append(phoneId);
                editorEdit.putString(sb.toString(), str);
                editorEdit.commit();
                return true;
            } catch (Exception e) {
                Log.e(LOG_TAG, "exception trying to dowload key from url: " + this.mURL);
                return false;
            }
        }
        Log.e(LOG_TAG, "mcc, mnc: is empty");
        return false;
    }

    @VisibleForTesting
    public static Pair<PublicKey, Long> getKeyInformation(byte[] bArr) throws Exception {
        X509Certificate x509Certificate = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(bArr));
        return new Pair<>(x509Certificate.getPublicKey(), Long.valueOf(x509Certificate.getNotAfter().getTime()));
    }

    @VisibleForTesting
    public void savePublicKey(PublicKey publicKey, int i, String str, long j, String str2, String str3) {
        this.mPhone.setCarrierInfoForImsiEncryption(new ImsiEncryptionInfo(str2, str3, i, str, publicKey, new Date(j)));
    }
}
