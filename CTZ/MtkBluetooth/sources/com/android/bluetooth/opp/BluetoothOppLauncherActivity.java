package com.android.bluetooth.opp;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;
import com.android.bluetooth.R;
import com.android.vcard.VCardConfig;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BluetoothOppLauncherActivity extends Activity {
    private static final int MAX_FILES_IN_SINGLE_TASK = 300;
    private static final String TAG = "BluetoothLauncherActivity";
    private static final boolean D = Constants.DEBUG;
    private static final boolean V = Constants.VERBOSE;
    private static final Pattern PLAIN_TEXT_TO_ESCAPE = Pattern.compile("[<>&]| {2,}|\r?\n");
    public static AtomicBoolean sSendingFileFlag = new AtomicBoolean(false);

    @Override
    public void onCreate(Bundle bundle) throws Throwable {
        super.onCreate(bundle);
        if (BenesseExtension.getDchaState() != 0) {
            finish();
            return;
        }
        requestWindowFeature(1);
        Intent intent = getIntent();
        String action = intent.getAction();
        if (action.equals("android.intent.action.SEND") || action.equals("android.intent.action.SEND_MULTIPLE")) {
            if (isBluetoothAllowed()) {
                sSendingFileFlag.set(true);
                if (action.equals("android.intent.action.SEND")) {
                    final String type = intent.getType();
                    final Uri uri = (Uri) intent.getParcelableExtra("android.intent.extra.STREAM");
                    CharSequence charSequenceExtra = intent.getCharSequenceExtra("android.intent.extra.TEXT");
                    if (uri != null && type != null) {
                        if (V) {
                            Log.v(TAG, "Get ACTION_SEND intent: Uri = " + uri + "; mimetype = " + type);
                        }
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                BluetoothOppUtility.closeUnstartedSendFileInfo();
                                BluetoothOppLauncherActivity.this.sendFileInfo(type, uri.toString(), false, true);
                            }
                        }).start();
                        return;
                    }
                    if (charSequenceExtra != null && type != null) {
                        if (V) {
                            Log.v(TAG, "Get ACTION_SEND intent with Extra_text = " + charSequenceExtra.toString() + "; mimetype = " + type);
                        }
                        final Uri uriCreatFileForSharedContent = creatFileForSharedContent(createCredentialProtectedStorageContext(), charSequenceExtra);
                        if (uriCreatFileForSharedContent != null) {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    BluetoothOppUtility.closeUnstartedSendFileInfo();
                                    BluetoothOppLauncherActivity.this.sendFileInfo(type, uriCreatFileForSharedContent.toString(), false, false);
                                }
                            }).start();
                            return;
                        } else {
                            Log.w(TAG, "Error trying to do set text...File not created!");
                            finish();
                            return;
                        }
                    }
                    Log.e(TAG, "type is null; or sending file URI is null");
                    finish();
                    return;
                }
                if (action.equals("android.intent.action.SEND_MULTIPLE")) {
                    final String type2 = intent.getType();
                    final ArrayList parcelableArrayListExtra = intent.getParcelableArrayListExtra("android.intent.extra.STREAM");
                    if (type2 != null && parcelableArrayListExtra != null) {
                        if (V) {
                            Log.v(TAG, "Get ACTION_SHARE_MULTIPLE intent: uris " + parcelableArrayListExtra + "\n Type= " + type2);
                        }
                        if ((parcelableArrayListExtra.size() + BluetoothOppUtility.getTotalTaskCount()) - BluetoothOppUtility.getUnstartedSendCount() > MAX_FILES_IN_SINGLE_TASK) {
                            if (V) {
                                Log.v(TAG, "uris.size() = " + parcelableArrayListExtra.size() + ", size > 300, return ");
                            }
                            Toast.makeText(this, getString(R.string.too_much_file_sent, String.valueOf(MAX_FILES_IN_SINGLE_TASK)), 1).show();
                            finish();
                            return;
                        }
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    BluetoothOppUtility.closeUnstartedSendFileInfo();
                                    BluetoothOppManager.getInstance(BluetoothOppLauncherActivity.this).saveSendingFileInfo(type2, parcelableArrayListExtra, false, true);
                                    BluetoothOppLauncherActivity.this.launchDevicePicker();
                                    BluetoothOppLauncherActivity.this.finish();
                                } catch (IllegalArgumentException e) {
                                    BluetoothOppLauncherActivity.this.showToast(e.getMessage());
                                    BluetoothOppLauncherActivity.this.finish();
                                }
                            }
                        }).start();
                        return;
                    }
                    Log.e(TAG, "type is null; or sending files URIs are null");
                    finish();
                    return;
                }
                return;
            }
            Intent intent2 = new Intent(this, (Class<?>) BluetoothOppBtErrorActivity.class);
            intent2.setFlags(VCardConfig.FLAG_REFRAIN_QP_TO_NAME_PROPERTIES);
            intent2.putExtra("title", getString(R.string.airplane_error_title));
            intent2.putExtra("content", getString(R.string.airplane_error_msg));
            startActivity(intent2);
            finish();
            return;
        }
        if (action.equals("android.btopp.intent.action.OPEN")) {
            Uri data = getIntent().getData();
            if (V) {
                Log.v(TAG, "Get ACTION_OPEN intent: Uri = " + data);
            }
            Intent intent3 = new Intent();
            intent3.setAction(action);
            intent3.setClassName("com.android.bluetooth", BluetoothOppReceiver.class.getName());
            intent3.setDataAndNormalize(data);
            sendBroadcast(intent3);
            finish();
            return;
        }
        Log.w(TAG, "Unsupported action: " + action);
        finish();
    }

    private void launchDevicePicker() {
        if (!BluetoothOppManager.getInstance(this).isEnabled()) {
            if (V) {
                Log.v(TAG, "Prepare Enable BT!! ");
            }
            Intent intent = new Intent(this, (Class<?>) BluetoothOppBtEnableActivity.class);
            intent.setFlags(VCardConfig.FLAG_REFRAIN_QP_TO_NAME_PROPERTIES);
            startActivity(intent);
            return;
        }
        if (BenesseExtension.getDchaState() != 0) {
            return;
        }
        if (V) {
            Log.v(TAG, "BT already enabled!! ");
        }
        Intent intent2 = new Intent("android.bluetooth.devicepicker.action.LAUNCH");
        intent2.setFlags(8388608);
        intent2.putExtra("android.bluetooth.devicepicker.extra.NEED_AUTH", false);
        intent2.putExtra("android.bluetooth.devicepicker.extra.FILTER_TYPE", 2);
        intent2.putExtra("android.bluetooth.devicepicker.extra.LAUNCH_PACKAGE", "com.android.bluetooth");
        intent2.putExtra("android.bluetooth.devicepicker.extra.DEVICE_PICKER_LAUNCH_CLASS", BluetoothOppReceiver.class.getName());
        if (V) {
            Log.d(TAG, "Launching android.bluetooth.devicepicker.action.LAUNCH");
        }
        startActivity(intent2);
    }

    private boolean isBluetoothAllowed() {
        ContentResolver contentResolver = getContentResolver();
        if (!(Settings.System.getInt(contentResolver, "airplane_mode_on", 0) == 1)) {
            return true;
        }
        String string = Settings.System.getString(contentResolver, "airplane_mode_radios");
        if (!(string == null || string.contains("bluetooth"))) {
            return true;
        }
        String string2 = Settings.System.getString(contentResolver, "airplane_mode_toggleable_radios");
        return string2 != null && string2.contains("bluetooth");
    }

    private Uri creatFileForSharedContent(Context context, CharSequence charSequence) throws Throwable {
        Uri uri;
        Uri uri2;
        Uri uri3;
        Uri uriFromFile;
        String str;
        FileOutputStream fileOutputStream = null;
        try {
            try {
                if (charSequence == 0) {
                    return null;
                }
                try {
                    String str2 = getString(R.string.bluetooth_share_file_name) + ".html";
                    context.deleteFile(str2);
                    StringBuffer stringBuffer = new StringBuffer("<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/></head><body>");
                    String strEscapeCharacterToDisplay = escapeCharacterToDisplay(charSequence.toString());
                    Pattern patternCompile = Pattern.compile("(?i)(http|https)://");
                    Matcher matcher = Pattern.compile("(" + Patterns.WEB_URL.pattern() + ")|(" + Patterns.EMAIL_ADDRESS.pattern() + ")|(" + Patterns.PHONE.pattern() + ")").matcher(strEscapeCharacterToDisplay);
                    while (matcher.find()) {
                        String strGroup = matcher.group();
                        if (Patterns.WEB_URL.matcher(strGroup).matches()) {
                            Matcher matcher2 = patternCompile.matcher(strGroup);
                            if (matcher2.find()) {
                                str = matcher2.group().toLowerCase(Locale.US) + strGroup.substring(matcher2.end());
                            } else {
                                str = "http://" + strGroup;
                            }
                        } else if (Patterns.EMAIL_ADDRESS.matcher(strGroup).matches()) {
                            str = "mailto:" + strGroup;
                        } else if (Patterns.PHONE.matcher(strGroup).matches()) {
                            str = "tel:" + strGroup;
                        } else {
                            str = null;
                        }
                        if (str != null) {
                            matcher.appendReplacement(stringBuffer, String.format("<a href=\"%s\">%s</a>", str, strGroup));
                        }
                    }
                    matcher.appendTail(stringBuffer);
                    stringBuffer.append("</body></html>");
                    byte[] bytes = stringBuffer.toString().getBytes();
                    FileOutputStream fileOutputStreamOpenFileOutput = context.openFileOutput(str2, 0);
                    if (fileOutputStreamOpenFileOutput != null) {
                        try {
                            try {
                                fileOutputStreamOpenFileOutput.write(bytes, 0, bytes.length);
                                uriFromFile = Uri.fromFile(new File(context.getFilesDir(), str2));
                                if (uriFromFile != null) {
                                    try {
                                        if (D) {
                                            Log.d(TAG, "Created one file for shared content: " + uriFromFile.toString());
                                        }
                                    } catch (FileNotFoundException e) {
                                        fileOutputStream = fileOutputStreamOpenFileOutput;
                                        uri3 = uriFromFile;
                                        e = e;
                                        Log.e(TAG, "FileNotFoundException: " + e.toString());
                                        e.printStackTrace();
                                        if (fileOutputStream == null) {
                                            return uri3;
                                        }
                                        fileOutputStream.close();
                                        charSequence = uri3;
                                        return charSequence;
                                    } catch (IOException e2) {
                                        fileOutputStream = fileOutputStreamOpenFileOutput;
                                        uri2 = uriFromFile;
                                        e = e2;
                                        Log.e(TAG, "IOException: " + e.toString());
                                        if (fileOutputStream == null) {
                                            return uri2;
                                        }
                                        fileOutputStream.close();
                                        charSequence = uri2;
                                        return charSequence;
                                    } catch (Exception e3) {
                                        fileOutputStream = fileOutputStreamOpenFileOutput;
                                        uri = uriFromFile;
                                        e = e3;
                                        Log.e(TAG, "Exception: " + e.toString());
                                        if (fileOutputStream == null) {
                                            return uri;
                                        }
                                        fileOutputStream.close();
                                        charSequence = uri;
                                        return charSequence;
                                    }
                                }
                            } catch (Throwable th) {
                                th = th;
                                fileOutputStream = fileOutputStreamOpenFileOutput;
                                if (fileOutputStream != null) {
                                    try {
                                        fileOutputStream.close();
                                    } catch (IOException e4) {
                                        e4.printStackTrace();
                                    }
                                }
                                throw th;
                            }
                        } catch (FileNotFoundException e5) {
                            e = e5;
                            uri3 = null;
                            fileOutputStream = fileOutputStreamOpenFileOutput;
                        } catch (IOException e6) {
                            e = e6;
                            uri2 = null;
                            fileOutputStream = fileOutputStreamOpenFileOutput;
                        } catch (Exception e7) {
                            e = e7;
                            uri = null;
                            fileOutputStream = fileOutputStreamOpenFileOutput;
                        }
                    } else {
                        uriFromFile = null;
                    }
                    if (fileOutputStreamOpenFileOutput != null) {
                        try {
                            fileOutputStreamOpenFileOutput.close();
                        } catch (IOException e8) {
                            e8.printStackTrace();
                        }
                    }
                    return uriFromFile;
                } catch (FileNotFoundException e9) {
                    e = e9;
                    uri3 = null;
                } catch (IOException e10) {
                    e = e10;
                    uri2 = null;
                } catch (Exception e11) {
                    e = e11;
                    uri = null;
                }
            } catch (IOException e12) {
                e12.printStackTrace();
                return charSequence;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private static String escapeCharacterToDisplay(String str) {
        Matcher matcher = PLAIN_TEXT_TO_ESCAPE.matcher(str);
        if (matcher.find()) {
            StringBuilder sb = new StringBuilder();
            int iEnd = 0;
            do {
                int iStart = matcher.start();
                sb.append(str.substring(iEnd, iStart));
                iEnd = matcher.end();
                int iCodePointAt = str.codePointAt(iStart);
                if (iCodePointAt == 32) {
                    int i = iEnd - iStart;
                    for (int i2 = 1; i2 < i; i2++) {
                        sb.append("&nbsp;");
                    }
                    sb.append(' ');
                } else if (iCodePointAt == 13 || iCodePointAt == 10) {
                    sb.append("<br>");
                } else if (iCodePointAt == 60) {
                    sb.append("&lt;");
                } else if (iCodePointAt == 62) {
                    sb.append("&gt;");
                } else if (iCodePointAt == 38) {
                    sb.append("&amp;");
                }
            } while (matcher.find());
            sb.append(str.substring(iEnd));
            return sb.toString();
        }
        return str;
    }

    private void sendFileInfo(String str, String str2, boolean z, boolean z2) {
        try {
            BluetoothOppManager.getInstance(getApplicationContext()).saveSendingFileInfo(str, str2, z, z2);
            launchDevicePicker();
            finish();
        } catch (IllegalArgumentException e) {
            showToast(e.getMessage());
            finish();
        }
    }

    private void showToast(final String str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(BluetoothOppLauncherActivity.this.getApplicationContext(), str, 0).show();
            }
        });
    }
}
