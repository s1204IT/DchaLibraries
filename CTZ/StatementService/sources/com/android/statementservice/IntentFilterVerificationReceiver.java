package com.android.statementservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import com.android.statementservice.retriever.Utils;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Pattern;

public final class IntentFilterVerificationReceiver extends BroadcastReceiver {
    private static final String TAG = IntentFilterVerificationReceiver.class.getSimpleName();
    private static final Integer MAX_HOSTS_PER_REQUEST = 10;
    private static final Pattern ANDROID_PACKAGE_NAME_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*)*$");

    private static void sendErrorToPackageManager(PackageManager packageManager, int i) {
        packageManager.verifyIntentFilter(i, -1, Collections.emptyList());
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if ("android.intent.action.INTENT_FILTER_NEEDS_VERIFICATION".equals(action)) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                Intent intent2 = new Intent(context, (Class<?>) DirectStatementService.class);
                intent2.setAction("com.android.statementservice.service.CHECK_ALL_ACTION");
                int i = extras.getInt("android.content.pm.extra.INTENT_FILTER_VERIFICATION_ID");
                String string = extras.getString("android.content.pm.extra.INTENT_FILTER_VERIFICATION_URI_SCHEME");
                String string2 = extras.getString("android.content.pm.extra.INTENT_FILTER_VERIFICATION_HOSTS");
                String string3 = extras.getString("android.content.pm.extra.INTENT_FILTER_VERIFICATION_PACKAGE_NAME");
                Bundle bundle = new Bundle();
                bundle.putString("com.android.statementservice.service.RELATION", "delegate_permission/common.handle_all_urls");
                String[] strArrSplit = string2.split(" ");
                if (strArrSplit.length > MAX_HOSTS_PER_REQUEST.intValue()) {
                    Log.w(TAG, String.format("Request contains %d hosts which is more than the allowed %d.", Integer.valueOf(strArrSplit.length), MAX_HOSTS_PER_REQUEST));
                    sendErrorToPackageManager(context.getPackageManager(), i);
                    return;
                }
                ArrayList<String> arrayList = new ArrayList<>(strArrSplit.length);
                try {
                    ArrayList<String> arrayList2 = new ArrayList<>();
                    for (String strSubstring : strArrSplit) {
                        if (strSubstring.startsWith("*.")) {
                            strSubstring = strSubstring.substring(2);
                        }
                        arrayList2.add(createWebAssetString(string, strSubstring));
                        arrayList.add(strSubstring);
                    }
                    bundle.putStringArrayList("com.android.statementservice.service.SOURCE_ASSET_DESCRIPTORS", arrayList2);
                    try {
                        bundle.putString("com.android.statementservice.service.TARGET_ASSET_DESCRIPTOR", createAndroidAssetString(context, string3));
                        bundle.putParcelable("com.android.statementservice.service.RESULT_RECEIVER", new IsAssociatedResultReceiver(new Handler(), context.getPackageManager(), i));
                        logValidationParametersForCTS(i, string, arrayList, string3);
                        intent2.putExtras(bundle);
                        context.startService(intent2);
                        return;
                    } catch (PackageManager.NameNotFoundException e) {
                        Log.w(TAG, "Error when processing input Android package: " + e.getMessage());
                        sendErrorToPackageManager(context.getPackageManager(), i);
                        return;
                    }
                } catch (MalformedURLException e2) {
                    Log.w(TAG, "Error when processing input host: " + e2.getMessage());
                    sendErrorToPackageManager(context.getPackageManager(), i);
                    return;
                }
            }
            return;
        }
        Log.w(TAG, "Intent action not supported: " + action);
    }

    private void logValidationParametersForCTS(int i, String str, ArrayList<String> arrayList, String str2) {
        Log.i(TAG, String.format("Verifying IntentFilter. verificationId:%d scheme:\"%s\" hosts:\"%s\" package:\"%s\".", Integer.valueOf(i), str, TextUtils.join(" ", arrayList.toArray()), str2));
    }

    private String createAndroidAssetString(Context context, String str) throws PackageManager.NameNotFoundException {
        if (!ANDROID_PACKAGE_NAME_PATTERN.matcher(str).matches()) {
            throw new PackageManager.NameNotFoundException("Input package name is not valid.");
        }
        return String.format("{\"namespace\": \"android_app\", \"package_name\": \"%s\", \"sha256_cert_fingerprints\": [\"%s\"]}", str, Utils.joinStrings("\", \"", Utils.getCertFingerprintsFromPackageManager(str, context)));
    }

    private String createWebAssetString(String str, String str2) throws MalformedURLException {
        if (!Patterns.DOMAIN_NAME.matcher(str2).matches()) {
            throw new MalformedURLException("Input host is not valid.");
        }
        if (!str.equals("http") && !str.equals("https")) {
            throw new MalformedURLException("Input scheme is not valid.");
        }
        return String.format("{\"namespace\": \"web\", \"site\": \"%s\"}", new URL(str, str2, "").toString());
    }

    private static class IsAssociatedResultReceiver extends ResultReceiver {
        private final PackageManager mPackageManager;
        private final int mVerificationId;

        public IsAssociatedResultReceiver(Handler handler, PackageManager packageManager, int i) {
            super(handler);
            this.mVerificationId = i;
            this.mPackageManager = packageManager;
        }

        @Override
        protected void onReceiveResult(int i, Bundle bundle) {
            if (i != 0) {
                IntentFilterVerificationReceiver.sendErrorToPackageManager(this.mPackageManager, this.mVerificationId);
            } else if (bundle.getBoolean("is_associated")) {
                this.mPackageManager.verifyIntentFilter(this.mVerificationId, 1, Collections.emptyList());
            } else {
                this.mPackageManager.verifyIntentFilter(this.mVerificationId, -1, bundle.getStringArrayList("failed_sources"));
            }
        }
    }
}
