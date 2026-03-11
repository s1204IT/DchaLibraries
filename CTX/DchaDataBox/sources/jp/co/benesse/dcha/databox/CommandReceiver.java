package jp.co.benesse.dcha.databox;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import jp.co.benesse.dcha.databox.db.ContractKvs;
import jp.co.benesse.dcha.util.FileUtils;
import jp.co.benesse.dcha.util.Logger;

public class CommandReceiver extends BroadcastReceiver {
    private static final String ACTION_COMMAND = "jp.co.benesse.dcha.databox.intent.action.COMMAND";
    private static final String ACTION_IMPORTED_ENVIRONMENT_EVENT = "jp.co.benesse.dcha.databox.intent.action.IMPORTED_ENVIRONMENT_EVENT";
    private static final String CATEGORIES_IMPORT_ENVIRONMENT = "jp.co.benesse.dcha.databox.intent.category.IMPORT_ENVIRONMENT";
    private static final String CATEGORIES_WIPE = "jp.co.benesse.dcha.databox.intent.category.WIPE";
    private static final String EXTRA_KEY_EXTERNAL_STORAGE = "EXTERNAL_STORAGE";
    private static final String EXTRA_KEY_WIPE = "send_service";
    private static final String EXTRA_VALUE_WIPE = "DchaService";
    private static final String IMPORT_ENVIRONMENT_FILENAME = "test_environment_info.xml";
    private static final String TAG = CommandReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Set<String> categories = intent.getCategories();
        Logger.d(TAG, "action:" + action);
        if (action.equals(ACTION_COMMAND)) {
            if (categories.contains(CATEGORIES_WIPE)) {
                Logger.d(TAG, "categories:jp.co.benesse.dcha.databox.intent.category.WIPE");
                if (EXTRA_VALUE_WIPE.equals(intent.getStringExtra(EXTRA_KEY_WIPE))) {
                    File[] fileArrListFiles = context.getFilesDir().listFiles();
                    if (fileArrListFiles != null) {
                        for (File file : fileArrListFiles) {
                            FileUtils.fileDelete(file);
                        }
                    }
                    ContentResolver contentResolver = context.getContentResolver();
                    new SboxProviderAdapter().wipe(contentResolver);
                    contentResolver.delete(Uri.withAppendedPath(ContractKvs.KVS.contentUri, "cmd/wipe"), null, null);
                    return;
                }
                return;
            }
            if (categories.contains(CATEGORIES_IMPORT_ENVIRONMENT)) {
                Logger.d(TAG, "categories:jp.co.benesse.dcha.databox.intent.category.IMPORT_ENVIRONMENT");
                ImportUrlsXml importUrlsXml = new ImportUrlsXml();
                importUrlsXml.delete(context);
                for (File file2 : getExternalStoragePaths(intent.getStringExtra(EXTRA_KEY_EXTERNAL_STORAGE))) {
                    if (importUrlsXml.execImport(context, new File(file2, IMPORT_ENVIRONMENT_FILENAME))) {
                        Logger.d(TAG, "imported path:" + file2.toString());
                        Intent intent2 = new Intent();
                        intent2.setAction(ACTION_IMPORTED_ENVIRONMENT_EVENT);
                        context.sendBroadcast(intent2);
                        return;
                    }
                }
            }
        }
    }

    protected List<File> getExternalStoragePaths(String str) {
        ArrayList arrayList = new ArrayList(2);
        if (!TextUtils.isEmpty(str)) {
            arrayList.add(new File(str));
        }
        String str2 = System.getenv("SECONDARY_STORAGE");
        if (!TextUtils.isEmpty(str2)) {
            arrayList.add(new File(str2));
        }
        arrayList.add(Environment.getExternalStorageDirectory());
        return arrayList;
    }
}
