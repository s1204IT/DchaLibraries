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
import jp.co.benesse.dcha.util.Logger;

public class CommandReceiver extends BroadcastReceiver {
    private static final String ACTION_COMMAND = "jp.co.benesse.dcha.databox.intent.action.COMMAND";
    private static final String ACTION_IMPORTED_ENVIRONMENT_EVENT = "jp.co.benesse.dcha.databox.intent.action.IMPORTED_ENVIRONMENT_EVENT";
    private static final String CATEGORIES_IMPORT_ENVIRONMENT = "jp.co.benesse.dcha.databox.intent.category.IMPORT_ENVIRONMENT";
    private static final String CATEGORIES_WIPE = "jp.co.benesse.dcha.databox.intent.category.WIPE";
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
                    Intent startIntent = new Intent(context, (Class<?>) Sbox.class);
                    startIntent.putExtra(Sbox.REQUEST_COMMAND_TYPE, 1);
                    context.startService(startIntent);
                    File[] files = context.getFilesDir().listFiles();
                    if (files != null) {
                        for (File file : files) {
                            fileDelete(file);
                        }
                    }
                    ContentResolver cr = context.getContentResolver();
                    cr.delete(Uri.withAppendedPath(ContractKvs.KVS.contentUri, "cmd/wipe"), null, null);
                    return;
                }
                return;
            }
            if (categories.contains(CATEGORIES_IMPORT_ENVIRONMENT)) {
                Logger.d(TAG, "categories:jp.co.benesse.dcha.databox.intent.category.IMPORT_ENVIRONMENT");
                ImportUrlsXml irx = new ImportUrlsXml();
                irx.delete(context);
                for (File path : getExternalStoragePaths()) {
                    irx.setPath(new File(path, IMPORT_ENVIRONMENT_FILENAME));
                    if (irx.execImport(context)) {
                        Logger.d(TAG, "imported path:" + path.toString());
                        Intent importedIntent = new Intent();
                        importedIntent.setAction(ACTION_IMPORTED_ENVIRONMENT_EVENT);
                        context.sendBroadcast(importedIntent);
                        return;
                    }
                }
            }
        }
    }

    protected boolean fileDelete(File file) {
        File[] files;
        if (!file.exists()) {
            return false;
        }
        if (file.isFile()) {
            return file.delete();
        }
        if (!file.isDirectory() || (files = file.listFiles()) == null) {
            return false;
        }
        for (File file2 : files) {
            if (!fileDelete(file2)) {
                return false;
            }
        }
        return file.delete();
    }

    protected List<File> getExternalStoragePaths() {
        List<File> list = new ArrayList<>(2);
        String path = System.getenv("SECONDARY_STORAGE");
        if (!TextUtils.isEmpty(path)) {
            list.add(new File(path));
        }
        list.add(Environment.getExternalStorageDirectory());
        return list;
    }
}
