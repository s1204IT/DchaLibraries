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
import com.android.bluetooth.R;
import com.android.vcard.VCardConfig;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BluetoothOppLauncherActivity extends Activity {
    private static final boolean D = true;
    private static final Pattern PLAIN_TEXT_TO_ESCAPE = Pattern.compile("[<>&]| {2,}|\r?\n");
    private static final String TAG = "BluetoothLauncherActivity";
    private static final boolean V = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String action = intent.getAction();
        if (BenesseExtension.getDchaState() != 0) {
            finish();
            return;
        }
        if (action == null) {
            Log.v(TAG, "action is null, set it ACTION_SEND by default");
            action = "android.intent.action.SEND";
        }
        if (action.equals("android.intent.action.SEND") || action.equals("android.intent.action.SEND_MULTIPLE")) {
            if (!isBluetoothAllowed()) {
                Intent in = new Intent(this, (Class<?>) BluetoothOppBtErrorActivity.class);
                in.setFlags(VCardConfig.FLAG_REFRAIN_QP_TO_NAME_PROPERTIES);
                in.putExtra("title", getString(R.string.airplane_error_title));
                in.putExtra("content", getString(R.string.airplane_error_msg));
                startActivity(in);
                finish();
                return;
            }
            if (action.equals("android.intent.action.SEND")) {
                final String type = intent.getType();
                final Uri stream = (Uri) intent.getParcelableExtra("android.intent.extra.STREAM");
                CharSequence extra_text = intent.getCharSequenceExtra("android.intent.extra.TEXT");
                if (stream != null && type != null) {
                    Thread t = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            BluetoothOppManager.getInstance(BluetoothOppLauncherActivity.this).saveSendingFileInfo(type, stream.toString(), false, true);
                            BluetoothOppLauncherActivity.this.launchDevicePicker();
                            BluetoothOppLauncherActivity.this.finish();
                        }
                    });
                    t.start();
                    return;
                }
                if (extra_text != null && type != null) {
                    final Uri fileUri = creatFileForSharedContent(this, extra_text);
                    if (fileUri != null) {
                        Thread t2 = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                BluetoothOppManager.getInstance(BluetoothOppLauncherActivity.this).saveSendingFileInfo(type, fileUri.toString(), false, false);
                                BluetoothOppLauncherActivity.this.launchDevicePicker();
                                BluetoothOppLauncherActivity.this.finish();
                            }
                        });
                        t2.start();
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
                final String mimeType = intent.getType();
                final ArrayList<Uri> uris = intent.getParcelableArrayListExtra("android.intent.extra.STREAM");
                if (mimeType != null && uris != null) {
                    Thread t3 = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            BluetoothOppManager.getInstance(BluetoothOppLauncherActivity.this).saveSendingFileInfo(mimeType, uris, false, true);
                            BluetoothOppLauncherActivity.this.launchDevicePicker();
                            BluetoothOppLauncherActivity.this.finish();
                        }
                    });
                    t3.start();
                    return;
                } else {
                    Log.e(TAG, "type is null; or sending files URIs are null");
                    finish();
                    return;
                }
            }
            return;
        }
        if (action.equals(Constants.ACTION_OPEN)) {
            Uri uri = getIntent().getData();
            Intent intent1 = new Intent();
            intent1.setAction(action);
            intent1.setClassName("com.android.bluetooth", BluetoothOppReceiver.class.getName());
            intent1.setDataAndNormalize(uri);
            sendBroadcast(intent1);
            finish();
            return;
        }
        Log.w(TAG, "Unsupported action: " + action);
        finish();
    }

    private final void launchDevicePicker() {
        if (!BluetoothOppManager.getInstance(this).isEnabled()) {
            Intent in = new Intent(this, (Class<?>) BluetoothOppBtEnableActivity.class);
            in.setFlags(VCardConfig.FLAG_REFRAIN_QP_TO_NAME_PROPERTIES);
            startActivity(in);
        } else if (BenesseExtension.getDchaState() == 0) {
            Intent in1 = new Intent("android.bluetooth.devicepicker.action.LAUNCH");
            in1.setFlags(VCardConfig.FLAG_REFRAIN_IMAGE_EXPORT);
            in1.putExtra("android.bluetooth.devicepicker.extra.NEED_AUTH", false);
            in1.putExtra("android.bluetooth.devicepicker.extra.FILTER_TYPE", 2);
            in1.putExtra("android.bluetooth.devicepicker.extra.LAUNCH_PACKAGE", "com.android.bluetooth");
            in1.putExtra("android.bluetooth.devicepicker.extra.DEVICE_PICKER_LAUNCH_CLASS", BluetoothOppReceiver.class.getName());
            startActivity(in1);
        }
    }

    private final boolean isBluetoothAllowed() {
        ContentResolver resolver = getContentResolver();
        boolean isAirplaneModeOn = Settings.System.getInt(resolver, "airplane_mode_on", 0) == 1;
        if (!isAirplaneModeOn) {
            return true;
        }
        String airplaneModeRadios = Settings.System.getString(resolver, "airplane_mode_radios");
        boolean isAirplaneSensitive = airplaneModeRadios == null ? true : airplaneModeRadios.contains("bluetooth");
        if (!isAirplaneSensitive) {
            return true;
        }
        String airplaneModeToggleableRadios = Settings.System.getString(resolver, "airplane_mode_toggleable_radios");
        boolean isAirplaneToggleable = airplaneModeToggleableRadios == null ? false : airplaneModeToggleableRadios.contains("bluetooth");
        return isAirplaneToggleable;
    }

    private Uri creatFileForSharedContent(Context context, CharSequence shareContent) {
        if (shareContent == null) {
            return null;
        }
        Uri fileUri = null;
        FileOutputStream outStream = null;
        try {
            try {
                try {
                    try {
                        String fileName = getString(R.string.bluetooth_share_file_name) + ".html";
                        context.deleteFile(fileName);
                        StringBuffer sb = new StringBuffer("<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/></head><body>");
                        String text = escapeCharacterToDisplay(shareContent.toString());
                        Pattern webUrlProtocol = Pattern.compile("(?i)(http|https)://");
                        Pattern pattern = Pattern.compile("(" + Patterns.WEB_URL.pattern() + ")|(" + Patterns.EMAIL_ADDRESS.pattern() + ")|(" + Patterns.PHONE.pattern() + ")");
                        Matcher m = pattern.matcher(text);
                        while (m.find()) {
                            String matchStr = m.group();
                            String link = null;
                            if (Patterns.WEB_URL.matcher(matchStr).matches()) {
                                Matcher proto = webUrlProtocol.matcher(matchStr);
                                link = proto.find() ? proto.group().toLowerCase(Locale.US) + matchStr.substring(proto.end()) : "http://" + matchStr;
                            } else if (Patterns.EMAIL_ADDRESS.matcher(matchStr).matches()) {
                                link = "mailto:" + matchStr;
                            } else if (Patterns.PHONE.matcher(matchStr).matches()) {
                                link = "tel:" + matchStr;
                            }
                            if (link != null) {
                                String href = String.format("<a href=\"%s\">%s</a>", link, matchStr);
                                m.appendReplacement(sb, href);
                            }
                        }
                        m.appendTail(sb);
                        sb.append("</body></html>");
                        byte[] byteBuff = sb.toString().getBytes();
                        outStream = context.openFileOutput(fileName, 0);
                        if (outStream != null) {
                            outStream.write(byteBuff, 0, byteBuff.length);
                            fileUri = Uri.fromFile(new File(context.getFilesDir(), fileName));
                            if (fileUri != null) {
                                Log.d(TAG, "Created one file for shared content: " + fileUri.toString());
                            }
                        }
                        if (outStream == null) {
                            return fileUri;
                        }
                        try {
                            outStream.close();
                            return fileUri;
                        } catch (IOException e) {
                            e.printStackTrace();
                            return fileUri;
                        }
                    } catch (Throwable th) {
                        if (outStream != null) {
                            try {
                                outStream.close();
                            } catch (IOException e2) {
                                e2.printStackTrace();
                            }
                        }
                        throw th;
                    }
                } catch (Exception e3) {
                    Log.e(TAG, "Exception: " + e3.toString());
                    if (outStream == null) {
                        return fileUri;
                    }
                    try {
                        outStream.close();
                        return fileUri;
                    } catch (IOException e4) {
                        e4.printStackTrace();
                        return fileUri;
                    }
                }
            } catch (IOException e5) {
                Log.e(TAG, "IOException: " + e5.toString());
                if (outStream == null) {
                    return fileUri;
                }
                try {
                    outStream.close();
                    return fileUri;
                } catch (IOException e6) {
                    e6.printStackTrace();
                    return fileUri;
                }
            }
        } catch (FileNotFoundException e7) {
            Log.e(TAG, "FileNotFoundException: " + e7.toString());
            e7.printStackTrace();
            if (outStream == null) {
                return fileUri;
            }
            try {
                outStream.close();
                return fileUri;
            } catch (IOException e8) {
                e8.printStackTrace();
                return fileUri;
            }
        }
    }

    private static String escapeCharacterToDisplay(String text) {
        Pattern pattern = PLAIN_TEXT_TO_ESCAPE;
        Matcher match = pattern.matcher(text);
        if (match.find()) {
            StringBuilder out = new StringBuilder();
            int end = 0;
            do {
                int start = match.start();
                out.append(text.substring(end, start));
                end = match.end();
                int c = text.codePointAt(start);
                if (c == 32) {
                    int n = end - start;
                    for (int i = 1; i < n; i++) {
                        out.append("&nbsp;");
                    }
                    out.append(' ');
                } else if (c == 13 || c == 10) {
                    out.append("<br>");
                } else if (c == 60) {
                    out.append("&lt;");
                } else if (c == 62) {
                    out.append("&gt;");
                } else if (c == 38) {
                    out.append("&amp;");
                }
            } while (match.find());
            out.append(text.substring(end));
            return out.toString();
        }
        return text;
    }
}
