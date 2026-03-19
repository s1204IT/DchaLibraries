package com.android.internal.telephony;

import android.R;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.telecom.PhoneAccountHandle;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsMessage;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.VisualVoicemailSms;
import android.telephony.VisualVoicemailSmsFilterSettings;
import android.util.ArrayMap;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.VisualVoicemailSmsParser;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class VisualVoicemailSmsFilter {
    private static final String TAG = "VvmSmsFilter";
    private static Map<String, List<Pattern>> sPatterns;
    private static final String TELEPHONY_SERVICE_PACKAGE = "com.android.phone";
    private static final ComponentName PSTN_CONNECTION_SERVICE_COMPONENT = new ComponentName(TELEPHONY_SERVICE_PACKAGE, "com.android.services.telephony.TelephonyConnectionService");
    private static final PhoneAccountHandleConverter DEFAULT_PHONE_ACCOUNT_HANDLE_CONVERTER = new PhoneAccountHandleConverter() {
        @Override
        public PhoneAccountHandle fromSubId(int i) {
            int phoneId;
            if (SubscriptionManager.isValidSubscriptionId(i) && (phoneId = SubscriptionManager.getPhoneId(i)) != -1) {
                return new PhoneAccountHandle(VisualVoicemailSmsFilter.PSTN_CONNECTION_SERVICE_COMPONENT, PhoneFactory.getPhone(phoneId).getFullIccSerialNumber());
            }
            return null;
        }
    };
    private static PhoneAccountHandleConverter sPhoneAccountHandleConverter = DEFAULT_PHONE_ACCOUNT_HANDLE_CONVERTER;

    @VisibleForTesting
    public interface PhoneAccountHandleConverter {
        PhoneAccountHandle fromSubId(int i);
    }

    private static class FullMessage {
        public SmsMessage firstMessage;
        public String fullMessageBody;

        private FullMessage() {
        }
    }

    public static boolean filter(Context context, byte[][] bArr, String str, int i, int i2) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService("phone");
        VisualVoicemailSmsFilterSettings activeVisualVoicemailSmsFilterSettings = telephonyManager.getActiveVisualVoicemailSmsFilterSettings(i2);
        if (activeVisualVoicemailSmsFilterSettings == null) {
            return false;
        }
        PhoneAccountHandle phoneAccountHandleFromSubId = sPhoneAccountHandleConverter.fromSubId(i2);
        if (phoneAccountHandleFromSubId == null) {
            Log.e(TAG, "Unable to convert subId " + i2 + " to PhoneAccountHandle");
            return false;
        }
        FullMessage fullMessage = getFullMessage(bArr, str);
        if (fullMessage == null) {
            Log.i(TAG, "Unparsable SMS received");
            VisualVoicemailSmsParser.WrappedMessageData alternativeFormat = VisualVoicemailSmsParser.parseAlternativeFormat(parseAsciiPduMessage(bArr));
            if (alternativeFormat != null) {
                sendVvmSmsBroadcast(context, activeVisualVoicemailSmsFilterSettings, phoneAccountHandleFromSubId, alternativeFormat, null);
            }
            return false;
        }
        String str2 = fullMessage.fullMessageBody;
        VisualVoicemailSmsParser.WrappedMessageData wrappedMessageData = VisualVoicemailSmsParser.parse(activeVisualVoicemailSmsFilterSettings.clientPrefix, str2);
        if (wrappedMessageData != null) {
            if (activeVisualVoicemailSmsFilterSettings.destinationPort == -2) {
                if (i == -1) {
                    Log.i(TAG, "SMS matching VVM format received but is not a DATA SMS");
                    return false;
                }
            } else if (activeVisualVoicemailSmsFilterSettings.destinationPort != -1 && activeVisualVoicemailSmsFilterSettings.destinationPort != i) {
                Log.i(TAG, "SMS matching VVM format received but is not directed to port " + activeVisualVoicemailSmsFilterSettings.destinationPort);
                return false;
            }
            if (!activeVisualVoicemailSmsFilterSettings.originatingNumbers.isEmpty() && !isSmsFromNumbers(fullMessage.firstMessage, activeVisualVoicemailSmsFilterSettings.originatingNumbers)) {
                Log.i(TAG, "SMS matching VVM format received but is not from originating numbers");
                return false;
            }
            sendVvmSmsBroadcast(context, activeVisualVoicemailSmsFilterSettings, phoneAccountHandleFromSubId, wrappedMessageData, null);
            return true;
        }
        buildPatternsMap(context);
        List<Pattern> list = sPatterns.get(telephonyManager.getSimOperator(i2));
        if (list == null || list.isEmpty()) {
            return false;
        }
        for (Pattern pattern : list) {
            if (pattern.matcher(str2).matches()) {
                Log.w(TAG, "Incoming SMS matches pattern " + pattern + " but has illegal format, still dropping as VVM SMS");
                sendVvmSmsBroadcast(context, activeVisualVoicemailSmsFilterSettings, phoneAccountHandleFromSubId, null, str2);
                return true;
            }
        }
        return false;
    }

    @VisibleForTesting
    public static void setPhoneAccountHandleConverterForTest(PhoneAccountHandleConverter phoneAccountHandleConverter) {
        if (phoneAccountHandleConverter == null) {
            sPhoneAccountHandleConverter = DEFAULT_PHONE_ACCOUNT_HANDLE_CONVERTER;
        } else {
            sPhoneAccountHandleConverter = phoneAccountHandleConverter;
        }
    }

    private static void buildPatternsMap(Context context) {
        if (sPatterns != null) {
            return;
        }
        sPatterns = new ArrayMap();
        for (String str : context.getResources().getStringArray(R.array.config_displayUniqueIdArray)) {
            String[] strArrSplit = str.split(";")[0].split(",");
            Pattern patternCompile = Pattern.compile(str.split(";")[1]);
            for (String str2 : strArrSplit) {
                if (!sPatterns.containsKey(str2)) {
                    sPatterns.put(str2, new ArrayList());
                }
                sPatterns.get(str2).add(patternCompile);
            }
        }
    }

    private static void sendVvmSmsBroadcast(Context context, VisualVoicemailSmsFilterSettings visualVoicemailSmsFilterSettings, PhoneAccountHandle phoneAccountHandle, VisualVoicemailSmsParser.WrappedMessageData wrappedMessageData, String str) {
        Log.i(TAG, "VVM SMS received");
        Intent intent = new Intent("com.android.internal.provider.action.VOICEMAIL_SMS_RECEIVED");
        VisualVoicemailSms.Builder builder = new VisualVoicemailSms.Builder();
        if (wrappedMessageData != null) {
            builder.setPrefix(wrappedMessageData.prefix);
            builder.setFields(wrappedMessageData.fields);
        }
        if (str != null) {
            builder.setMessageBody(str);
        }
        builder.setPhoneAccountHandle(phoneAccountHandle);
        intent.putExtra("android.provider.extra.VOICEMAIL_SMS", builder.build());
        intent.putExtra("android.provider.extra.TARGET_PACAKGE", visualVoicemailSmsFilterSettings.packageName);
        intent.setPackage(TELEPHONY_SERVICE_PACKAGE);
        context.sendBroadcast(intent);
    }

    private static FullMessage getFullMessage(byte[][] bArr, String str) {
        FullMessage fullMessage = new FullMessage();
        StringBuilder sb = new StringBuilder();
        CharsetDecoder charsetDecoderNewDecoder = StandardCharsets.UTF_8.newDecoder();
        for (byte[] bArr2 : bArr) {
            SmsMessage smsMessageCreateFromPdu = SmsMessage.createFromPdu(bArr2, str);
            if (smsMessageCreateFromPdu == null) {
                return null;
            }
            if (fullMessage.firstMessage == null) {
                fullMessage.firstMessage = smsMessageCreateFromPdu;
            }
            String messageBody = smsMessageCreateFromPdu.getMessageBody();
            if (messageBody == null && smsMessageCreateFromPdu.getUserData() != null) {
                try {
                    messageBody = charsetDecoderNewDecoder.decode(ByteBuffer.wrap(smsMessageCreateFromPdu.getUserData())).toString();
                } catch (CharacterCodingException e) {
                    return null;
                }
            }
            if (messageBody != null) {
                sb.append(messageBody);
            }
        }
        fullMessage.fullMessageBody = sb.toString();
        return fullMessage;
    }

    private static String parseAsciiPduMessage(byte[][] bArr) {
        StringBuilder sb = new StringBuilder();
        for (byte[] bArr2 : bArr) {
            sb.append(new String(bArr2, StandardCharsets.US_ASCII));
        }
        return sb.toString();
    }

    private static boolean isSmsFromNumbers(SmsMessage smsMessage, List<String> list) {
        if (smsMessage == null) {
            Log.e(TAG, "Unable to create SmsMessage from PDU, cannot determine originating number");
            return false;
        }
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            if (PhoneNumberUtils.compare(it.next(), smsMessage.getOriginatingAddress())) {
                return true;
            }
        }
        return false;
    }
}
