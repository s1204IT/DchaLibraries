package com.panasonic.sanyo.ts.firmwareupdate;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.Downloads;
import android.support.v4.provider.DocumentFile;
import android.util.Log;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;

public class BaseActivity extends Activity implements Runnable {
    static Thread thread = null;
    private AlertDialog.Builder dlg;
    private ProgressDialog progressDialog;
    private int status = 0;
    private int level = 0;
    private BroadcastReceiver BatteryBroadcastReceiver = null;
    private BroadcastReceiver MediaBroadcastReceiver = null;
    private boolean ReceverRegistered = false;
    protected boolean UpdateCancel = false;
    private boolean ProgressDialogActive = false;
    private boolean UpdateMediaEject = false;
    protected boolean Updatewait = false;
    protected String SDPath = "/storage/sdcard1/update.zip";
    protected String UPDATE_FILE = "update.zip";
    protected String CachePath = "/cache/update.zip";
    StorageVolume mount_vol = null;
    private Handler handler = new AnonymousClass3(this);

    class AnonymousClass3 extends Handler {
        final BaseActivity this$0;

        AnonymousClass3(BaseActivity baseActivity) {
            this.this$0 = baseActivity;
        }

        @Override
        public void handleMessage(Message message) {
            this.this$0.CancelAction(true);
            this.this$0.dlg.setCancelable(false);
            switch (message.what) {
                case 0:
                case 1:
                    this.this$0.dlg.setTitle("アップデートデータの読込みに失敗しました");
                    this.this$0.dlg.setMessage("SDカードが正常に読み込めません。\nSDカードが挿入されているか確認してください。");
                    break;
                case 2:
                case 3:
                case 4:
                    this.this$0.dlg.setTitle("アップデート処理に失敗しました");
                    break;
                case 5:
                    this.this$0.dlg.setTitle("充電してください");
                    this.this$0.dlg.setMessage("ローバッテリーになるとシステムアップデートが失敗する場合があります。\n電源を挿してからもう一度やり直してください。");
                    break;
            }
            this.this$0.dlg.setPositiveButton("OK", new DialogInterface.OnClickListener(this) {
                final AnonymousClass3 this$1;

                {
                    this.this$1 = this;
                }

                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    this.this$1.this$0.finish();
                }
            });
            this.this$0.dlg.show();
        }
    }

    private void CacheDirFileCheck() {
        File[] fileArrListFiles = Environment.getDownloadCacheDirectory().listFiles();
        if (fileArrListFiles == null) {
            return;
        }
        HashSet hashSet = new HashSet();
        for (int i = 0; i < fileArrListFiles.length; i++) {
            if (!fileArrListFiles[i].getName().equals("lost+found") && !fileArrListFiles[i].getName().equalsIgnoreCase("recovery")) {
                hashSet.add(fileArrListFiles[i].getPath());
            }
        }
        Cursor cursorQuery = getContentResolver().query(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, new String[]{"_data"}, null, null, null);
        if (cursorQuery != null) {
            if (cursorQuery.moveToFirst()) {
                do {
                    hashSet.remove(cursorQuery.getString(0));
                } while (cursorQuery.moveToNext());
            }
            cursorQuery.close();
        }
        Iterator it = hashSet.iterator();
        while (it.hasNext()) {
            delete(new File((String) it.next()));
        }
    }

    public void CancelAction(boolean z) {
        Log.v("CancelAction", "start");
        File file = new File("/cache/update.zip");
        if (file.exists()) {
            file.delete();
        }
        File file2 = new File("/cache/recovery/command");
        if (file2.exists()) {
            file2.delete();
        }
        if (z && this.ProgressDialogActive) {
            Log.v("CancelAction", "progressDialog.dismiss");
            this.ProgressDialogActive = false;
            this.progressDialog.dismiss();
        }
    }

    private void UpdateStart() {
        Log.v("UpdateStart", "バッテリーチェック\n");
        if (this.UpdateCancel) {
            CancelAction(true);
            return;
        }
        if (this.status != 5 && this.status != 2) {
            this.handler.sendEmptyMessage(5);
            return;
        }
        Log.v("UpdateStart", "容量比較\n");
        if (this.UpdateCancel) {
            CancelAction(true);
            return;
        }
        Log.v("SDPath", "SDPath:" + this.SDPath);
        for (StorageVolume storageVolume : ((StorageManager) getSystemService("storage")).getStorageVolumes()) {
            if (storageVolume.isRemovable()) {
                this.mount_vol = storageVolume;
                startActivityForResult(this.mount_vol.createAccessIntent(null), 2317);
                return;
            }
        }
        this.handler.sendEmptyMessage(1);
    }

    private static void delete(File file) {
        File[] fileArrListFiles;
        if (file.isFile()) {
            file.delete();
        }
        if (!file.isDirectory() || (fileArrListFiles = file.listFiles()) == null) {
            return;
        }
        for (File file2 : fileArrListFiles) {
            delete(file2);
        }
        file.delete();
    }

    @Override
    public void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        Log.i("onActivityResult", "nActivityResultCall.");
        if (i != 2317 || i2 != -1) {
            this.handler.sendEmptyMessage(1);
            return;
        }
        if (intent == null) {
            Log.i("onActivityResult", "resultData is null");
            this.handler.sendEmptyMessage(1);
            return;
        }
        Uri data = intent.getData();
        Log.i("onActivityResult", "URI = " + data.getEncodedPath());
        Log.i("onActivityResult", "URI = " + data.getPath());
        getContentResolver().takePersistableUriPermission(data, 3);
        DocumentFile documentFileFromTreeUri = DocumentFile.fromTreeUri(this, data);
        Log.i("onActivityResult", "outUri1 = " + documentFileFromTreeUri.getUri().getPath());
        DocumentFile documentFileFindFile = documentFileFromTreeUri.findFile(this.UPDATE_FILE);
        if (documentFileFindFile == null) {
            this.handler.sendEmptyMessage(1);
            return;
        }
        Log.v("UpDatefile", "file exists\n");
        long length = documentFileFindFile.length();
        Log.v("CacheCheck", "UpdateSize : " + length);
        StatFs statFs = new StatFs("/cache");
        int blockSize = statFs.getBlockSize() * statFs.getAvailableBlocks();
        Log.v("CacheCheck", "AvailableCache : " + blockSize);
        if (length > blockSize) {
            CacheDirFileCheck();
        }
        Log.v("UpdateStart", "ファイルチェック\n");
        if (this.UpdateCancel) {
            CancelAction(true);
            return;
        }
        CancelAction(false);
        try {
            try {
                InputStream inputStreamOpenInputStream = getContentResolver().openInputStream(documentFileFindFile.getUri());
                FileOutputStream fileOutputStream = new FileOutputStream(this.CachePath);
                try {
                    byte[] bArr = new byte[4096];
                    int i3 = 0;
                    while (true) {
                        int i4 = inputStreamOpenInputStream.read(bArr);
                        if (-1 == i4) {
                            break;
                        }
                        fileOutputStream.write(bArr, 0, i4);
                        i3 += i4;
                    }
                    Log.v("UpdateStart", "read-write OK:" + i3);
                    inputStreamOpenInputStream.close();
                    fileOutputStream.flush();
                    fileOutputStream.close();
                    Log.v("UpdateStart", "/recoveryディレクトリチェック\n");
                    if (this.UpdateCancel) {
                        CancelAction(true);
                        return;
                    }
                    File file = new File("/cache/recovery");
                    if (!file.exists() && !file.mkdir()) {
                        this.handler.sendEmptyMessage(3);
                        return;
                    }
                    Log.v("UpdateStart", "commandファイルチェック\n");
                    if (this.UpdateCancel) {
                        CancelAction(true);
                        return;
                    }
                    File file2 = new File("/cache/recovery/command");
                    if (!file2.exists()) {
                        try {
                            if (!file2.createNewFile()) {
                                this.handler.sendEmptyMessage(4);
                                return;
                            }
                            FileWriter fileWriter = new FileWriter(file2);
                            fileWriter.write("boot-recovery\n");
                            fileWriter.write("--update_package=/cache/update.zip\n");
                            fileWriter.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                            this.handler.sendEmptyMessage(2);
                            return;
                        }
                    }
                    Log.v("UpdateStart", "リブートチェック\n");
                    if (this.UpdateCancel) {
                        CancelAction(true);
                        return;
                    }
                    if (this.UpdateMediaEject) {
                        this.UpdateMediaEject = false;
                        this.handler.sendEmptyMessage(2);
                        return;
                    }
                    this.Updatewait = true;
                    ((PowerManager) getSystemService("power")).reboot("recovery-update");
                    if (this.ProgressDialogActive) {
                        Log.v("UpdateStart", "progressDialog.dismiss");
                        this.ProgressDialogActive = false;
                        this.progressDialog.dismiss();
                    }
                } catch (IOException e2) {
                    inputStreamOpenInputStream.close();
                    fileOutputStream.close();
                    Log.v("UpdateStart", "IOException-transfer\n");
                    e2.printStackTrace();
                    this.handler.sendEmptyMessage(2);
                }
            } catch (IOException e3) {
                Log.v("UpdateStart", "IOException\n");
                e3.printStackTrace();
                this.handler.sendEmptyMessage(2);
            }
        } catch (FileNotFoundException e4) {
            Log.v("UpdateStart", "FileNotFoundException\n");
            e4.printStackTrace();
            this.handler.sendEmptyMessage(1);
        }
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getWindow().addFlags(128);
        this.dlg = new AlertDialog.Builder(this);
    }

    @Override
    protected void onPause() {
        Log.v("onPause", "onPause");
        super.onPause();
        if (this.Updatewait) {
            this.UpdateCancel = true;
            if (this.ReceverRegistered) {
                this.ReceverRegistered = false;
                unregisterReceiver(this.BatteryBroadcastReceiver);
                unregisterReceiver(this.MediaBroadcastReceiver);
            }
            if (this.ProgressDialogActive) {
                Log.v("onPause", "progressDialog.dismiss");
                this.ProgressDialogActive = false;
                this.progressDialog.dismiss();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.BatteryBroadcastReceiver = new BroadcastReceiver(this) {
            final BaseActivity this$0;

            {
                this.this$0 = this;
            }

            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("android.intent.action.BATTERY_CHANGED")) {
                    this.this$0.status = intent.getIntExtra("status", 0);
                    this.this$0.level = intent.getIntExtra("level", 0);
                    Log.v("status", "status:" + this.this$0.status);
                    Log.v("level", "level:" + this.this$0.level);
                }
            }
        };
        this.MediaBroadcastReceiver = new BroadcastReceiver(this) {
            final BaseActivity this$0;

            {
                this.this$0 = this;
            }

            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("android.intent.action.MEDIA_EJECT")) {
                    Log.v("MediaBroadcastReceiver", "SDカードが抜かれました\n");
                    if (BaseActivity.thread != null) {
                        this.this$0.UpdateMediaEject = true;
                    }
                }
            }
        };
        if (this.ReceverRegistered) {
            return;
        }
        this.ReceverRegistered = true;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.BATTERY_CHANGED");
        registerReceiver(this.BatteryBroadcastReceiver, intentFilter);
        new IntentFilter().addAction("android.intent.action.MEDIA_EJECT");
    }

    @Override
    public void run() {
        Log.d("runProcess", "run");
        try {
            Thread.sleep(500L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        UpdateStart();
        thread = null;
    }

    protected void startprogress() {
        this.ProgressDialogActive = true;
        this.progressDialog = new ProgressDialog(this);
        this.progressDialog.setProgressStyle(0);
        this.progressDialog.setTitle("処理中");
        this.progressDialog.setMessage("お待ちください･･･");
        this.progressDialog.setCancelable(false);
        this.progressDialog.show();
        Log.v("thread", "thread" + thread);
        thread = new Thread(this);
        thread.start();
    }
}
