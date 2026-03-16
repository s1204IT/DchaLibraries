package com.android.internal.telephony;

import android.R;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.Telephony;
import android.telephony.Rlog;
import android.telephony.SmsMessage;
import android.util.Log;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class VzwSmsFilter {
    private static final String INTENT_DIRECTED_SMS = "verizon.intent.action.DIRECTED_SMS_RECEIVED";
    private static boolean Q_LOG = true;
    private static final String TAG = "VzwSmsFilter";
    private Context mContext;
    private InboundSmsHandler mInboundSmsHandler;
    private SmsMessage mSms;
    private String mSmsFormat;

    public VzwSmsFilter(InboundSmsHandler handler, Context context, SmsMessage sms, String format) {
        this.mInboundSmsHandler = handler;
        this.mContext = context;
        this.mSms = sms;
        this.mSmsFormat = format;
    }

    public boolean filter() {
        if (this.mSms != null && this.mSms.getPdu() != null) {
            byte[] pdu = this.mSms.getPdu();
            if (Q_LOG) {
                String str = "";
                for (byte b : pdu) {
                    str = str + String.format("%02X ", Byte.valueOf(b));
                }
                Log.d(TAG, String.format("byte length=%d, HEX=%s", Integer.valueOf(pdu.length), str));
            }
        } else if (Q_LOG) {
            Log.d(TAG, "sms is null");
        }
        String ms = null;
        if (this.mSms != null) {
            if (this.mSms.getMessageBody() == null && Q_LOG) {
                Log.d(TAG, "getMessageBody is null ");
            }
            ms = this.mSms.getMessageBody() != null ? this.mSms.getMessageBody().toString() : "";
            if (Q_LOG) {
                Log.d(TAG, "MessageBody = " + ms);
            }
        } else if (Q_LOG) {
            Log.d(TAG, "sms is null");
        }
        if ((ms == null || ms.isEmpty()) && this.mSms != null) {
            if (Q_LOG) {
                Log.d(TAG, "ms is null, createFromPdu format with " + this.mSmsFormat);
            }
            byte[] pdu2 = this.mSms.getPdu();
            if (this.mSmsFormat.equalsIgnoreCase(SmsMessage.FORMAT_3GPP)) {
                this.mSms = SmsMessage.createFromPdu(pdu2, SmsMessage.FORMAT_3GPP);
            } else if (this.mSmsFormat.equalsIgnoreCase(SmsMessage.FORMAT_3GPP2)) {
                this.mSms = SmsMessage.createFromPdu(pdu2, SmsMessage.FORMAT_3GPP2);
            } else {
                if (Q_LOG) {
                    Log.e(TAG, "mSmsFormat is incorrect");
                }
                return false;
            }
            if (this.mSms != null && this.mSms.getMessageBody() == null) {
                if (Q_LOG) {
                    Log.d(TAG, "getMessageBody (" + this.mSmsFormat + ") is null ");
                }
            } else if (this.mSms == null) {
                if (Q_LOG) {
                    Log.d(TAG, "createFromPdu failed, fall back to exist handler ");
                }
                return false;
            }
            ms = this.mSms.getMessageBody() != null ? this.mSms.getMessageBody().toString() : "";
            if (Q_LOG) {
                Log.d(TAG, "MessageBody(" + this.mSmsFormat + ")= " + ms);
            }
        } else if (Q_LOG) {
            Log.d(TAG, "sms is null");
        }
        String oriAddr = null;
        if (this.mSms != null) {
            oriAddr = this.mSms.getOriginatingAddress();
        }
        if (Q_LOG) {
            Log.d(TAG, "OriginatingAddress = " + oriAddr);
        }
        PackageManager pm = this.mContext.getPackageManager();
        new ArrayList();
        Boolean isSysSignature = false;
        Intent intent = new Intent(INTENT_DIRECTED_SMS);
        List<ResolveInfo> activities = pm.queryBroadcastReceivers(intent, 0);
        if (Q_LOG && activities != null) {
            Log.d(TAG, String.format("supported activities = %d", Integer.valueOf(activities.size())));
        }
        for (ResolveInfo resolveInfo : activities) {
            ActivityInfo activityInfo = resolveInfo.activityInfo;
            if (activityInfo != null) {
                try {
                    pm.getPackageInfo(activityInfo.packageName, 64);
                    if (Q_LOG) {
                        Log.d(TAG, "isSysSignature = " + isSysSignature);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "exception!!");
                    e.printStackTrace();
                }
                if (isSysSignature.booleanValue()) {
                    String meta_value = null;
                    try {
                        ApplicationInfo appInfo = pm.getApplicationInfo(activityInfo.packageName, 128);
                        meta_value = appInfo.metaData.getString("com.verizon.directedAppSMS");
                        if (Q_LOG) {
                            Log.d(TAG, "meta_value = " + meta_value);
                        }
                    } catch (Exception e2) {
                        Log.e(TAG, "exception isSysSignature!!");
                        e2.printStackTrace();
                    }
                    if (ms != null) {
                        if (Q_LOG) {
                            Log.d(TAG, "ms exist! check ms start with");
                        }
                        if (ms.startsWith("//VZWPAYG-T")) {
                            if (Q_LOG) {
                                Log.d(TAG, " message start with //VZWPAYG-T");
                            }
                            String mfv = null;
                            if (ms.contains(":")) {
                                String[] names = ms.split(":");
                                mfv = names[1];
                            } else if (ms.contains(";")) {
                                String[] names2 = ms.split(";");
                                mfv = names2[1];
                            }
                            if (Q_LOG) {
                                Log.d(TAG, " mfv is " + mfv);
                            }
                            AlertDialog d = new AlertDialog.Builder(this.mContext).setTitle("Message From Verizon").setMessage(mfv).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            }).create();
                            d.getWindow().setType(2003);
                            d.show();
                            return true;
                        }
                        if (ms.startsWith("//VZWRDD") || ms.startsWith("//VZWDMNEW")) {
                            if (Q_LOG) {
                                Rlog.w(TAG, "this message is start with //VZWRDD or //VZWDMNEW");
                            }
                        } else {
                            if (ms.startsWith("//VZW")) {
                                if (Q_LOG) {
                                    Log.d(TAG, " message start with //VZW");
                                }
                                String splitMsg = ms.substring(5);
                                String app_prefix = null;
                                String parameters = null;
                                if (splitMsg.contains(":")) {
                                    String[] names3 = splitMsg.split(":");
                                    app_prefix = names3[0];
                                    parameters = names3[1];
                                } else if (splitMsg.contains(";")) {
                                    String[] names4 = splitMsg.split(";");
                                    app_prefix = names4[0];
                                    parameters = names4[1];
                                } else if (meta_value != null) {
                                    String ap = splitMsg.substring(0, meta_value.length());
                                    if (ap.equals(meta_value)) {
                                        app_prefix = ap;
                                        parameters = splitMsg.substring(meta_value.length());
                                    }
                                }
                                if (Q_LOG) {
                                    Log.d(TAG, "prefix = " + app_prefix + ", originator = " + oriAddr + ", parameters = " + parameters);
                                    Log.d(TAG, "pkg: " + activityInfo.packageName + ", name: " + activityInfo.name);
                                }
                                ComponentName component = new ComponentName(activityInfo.packageName, activityInfo.name);
                                intent.setComponent(component);
                                intent.putExtra("prefix", app_prefix);
                                intent.putExtra("originator", oriAddr);
                                intent.putExtra("parameters", parameters);
                                this.mContext.sendBroadcast(intent);
                                return true;
                            }
                            if (Q_LOG) {
                                Log.w(TAG, "no match");
                            }
                        }
                    } else if (Q_LOG) {
                        Log.w(TAG, "ms is null");
                    }
                } else if (Q_LOG) {
                    Log.w(TAG, "not system");
                }
            } else if (Q_LOG) {
                Log.w(TAG, "no activity info");
            }
        }
        if (ms.startsWith("//VZWRDD") || ms.startsWith("//VZWDMNEW")) {
            if (Q_LOG) {
                Rlog.w(TAG, "this message is start with //VZWRDD or //VZWDMNEW");
            }
            int desport = this.mSms.mWrappedSmsMessage.getUserDataHeader().portAddrs.destPort;
            if (Q_LOG) {
                Rlog.w(TAG, "desport = " + desport);
            }
            Uri uri = Uri.parse("sms://localhost:" + desport);
            Intent intent2 = new Intent(Telephony.Sms.Intents.DATA_SMS_RECEIVED_ACTION, uri);
            ?? r0 = {ms.getBytes()};
            intent2.putExtra("pdus", (Serializable) r0);
            String test = "";
            for (byte[] bArr : r0) {
                String tmp = new String(bArr);
                test = test.concat(tmp);
                if (Q_LOG) {
                    Rlog.w(TAG, "tmp = " + tmp);
                }
            }
            if (Q_LOG) {
                Rlog.w(TAG, "pdus = " + test);
            }
            intent2.putExtra(Telephony.CellBroadcasts.MESSAGE_FORMAT, SmsMessage.FORMAT_3GPP);
            this.mInboundSmsHandler.dispatchIntent(intent2, "android.permission.RECEIVE_SMS", 16, null, null);
            return true;
        }
        if (Q_LOG) {
            Log.d(TAG, "App Directed SMS handle end");
        }
        if ((ms == null || !ms.startsWith("//VZWRDD")) && ms != null && ms.startsWith("//VZW")) {
            Log.e(TAG, "no matched Application Directed SMS receiver! drop message");
            return true;
        }
        return false;
    }
}
