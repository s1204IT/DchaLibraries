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
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.StatFs;
import android.provider.Downloads;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.HashSet;

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
    protected String SDPath = "/mnt/m_external_sd/update.zip";
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            BaseActivity.this.CancelAction(true);
            BaseActivity.this.dlg.setCancelable(false);
            switch (msg.what) {
                case 0:
                case 1:
                    BaseActivity.this.dlg.setTitle("アップデートデータの読込みに失敗しました");
                    BaseActivity.this.dlg.setMessage("SDカードが正常に読み込めません。\nSDカードが挿入されているか確認してください。");
                    break;
                case 2:
                case 3:
                case 4:
                    BaseActivity.this.dlg.setTitle("アップデート処理に失敗しました");
                    break;
                case 5:
                    BaseActivity.this.dlg.setTitle("充電してください");
                    BaseActivity.this.dlg.setMessage("ローバッテリーになるとシステムアップデートが失敗する場合があります。\n電源を挿してからもう一度やり直してください。");
                    break;
            }
            BaseActivity.this.dlg.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                    BaseActivity.this.finish();
                }
            });
            BaseActivity.this.dlg.show();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(128);
        this.dlg = new AlertDialog.Builder(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.BatteryBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (!action.equals("android.intent.action.BATTERY_CHANGED")) {
                    return;
                }
                BaseActivity.this.status = intent.getIntExtra("status", 0);
                BaseActivity.this.level = intent.getIntExtra("level", 0);
                Log.v("status", "status:" + BaseActivity.this.status);
                Log.v("level", "level:" + BaseActivity.this.level);
            }
        };
        this.MediaBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (!action.equals("android.intent.action.MEDIA_EJECT")) {
                    return;
                }
                Log.v("MediaBroadcastReceiver", "SDカードが抜かれました\n");
                if (BaseActivity.thread == null) {
                    return;
                }
                BaseActivity.this.UpdateMediaEject = true;
            }
        };
        if (this.ReceverRegistered) {
            return;
        }
        this.ReceverRegistered = true;
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.BATTERY_CHANGED");
        registerReceiver(this.BatteryBroadcastReceiver, filter);
        IntentFilter filter2 = new IntentFilter();
        filter2.addAction("android.intent.action.MEDIA_EJECT");
        filter2.addDataScheme("file");
        registerReceiver(this.MediaBroadcastReceiver, filter2);
    }

    @Override
    protected void onPause() {
        Log.v("onPause", "onPause");
        super.onPause();
        this.UpdateCancel = true;
        if (this.ReceverRegistered) {
            this.ReceverRegistered = false;
            unregisterReceiver(this.BatteryBroadcastReceiver);
            unregisterReceiver(this.MediaBroadcastReceiver);
        }
        if (!this.ProgressDialogActive) {
            return;
        }
        Log.v("onPause", "progressDialog.dismiss");
        this.ProgressDialogActive = false;
        this.progressDialog.dismiss();
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
        File UpDatefile = new File(this.SDPath);
        if (!UpDatefile.exists()) {
            this.handler.sendEmptyMessage(1);
            return;
        }
        long UpdateSize = UpDatefile.length();
        StatFs sf = new StatFs("/cache");
        int AvailableBlock = sf.getAvailableBlocks();
        int BlockSize = sf.getBlockSize();
        int AvailableCache = AvailableBlock * BlockSize;
        if (UpdateSize > AvailableCache) {
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
                Log.v("UpdateCancel", "update.zipコピー start\n");
                FileChannel srcChannel = new FileInputStream(this.SDPath).getChannel();
                FileChannel destChannel = new FileOutputStream("/cache/update.zip").getChannel();
                long pos = 0;
                try {
                    long size = srcChannel.size();
                    while (pos < size) {
                        long add = srcChannel.transferTo(pos, 1048576L, destChannel);
                        if (add == 0) {
                            Log.v("UpdateStart", "IOException-throw\n");
                            throw new IOException();
                        }
                        pos += add;
                        Log.v("transferTo", "残り：" + (size - pos));
                        if (this.UpdateCancel || this.UpdateMediaEject) {
                            Log.v("transferTo", "中断されました");
                            break;
                        }
                    }
                    srcChannel.close();
                    destChannel.close();
                    Log.v("UpdateStart", "/recoveryディレクトリチェック\n");
                    if (this.UpdateCancel) {
                        CancelAction(true);
                        return;
                    }
                    File Recoveryfile = new File("/cache/recovery");
                    if (!Recoveryfile.exists() && !Recoveryfile.mkdir()) {
                        this.handler.sendEmptyMessage(3);
                        return;
                    }
                    Log.v("UpdateStart", "commandファイルチェック\n");
                    if (!this.UpdateCancel) {
                        String CommandfilePath = "/cache/recovery/command";
                        File Commandfile = new File(CommandfilePath);
                        if (!Commandfile.exists()) {
                            try {
                                if (!Commandfile.createNewFile()) {
                                    this.handler.sendEmptyMessage(4);
                                    return;
                                }
                                FileWriter filewriter = new FileWriter(Commandfile);
                                filewriter.write("boot-recovery\n");
                                filewriter.write("--update_package=/cache/update.zip\n");
                                filewriter.close();
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
                        PowerManager pm = (PowerManager) getSystemService("power");
                        pm.reboot("recovery-update");
                        if (!this.ProgressDialogActive) {
                            return;
                        }
                        Log.v("UpdateStart", "progressDialog.dismiss");
                        this.ProgressDialogActive = false;
                        this.progressDialog.dismiss();
                        return;
                    }
                    CancelAction(true);
                } catch (IOException e2) {
                    srcChannel.close();
                    destChannel.close();
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

    private void CacheDirFileCheck() {
        File[] files = Environment.getDownloadCacheDirectory().listFiles();
        if (files == null) {
            return;
        }
        HashSet<String> fileSet = new HashSet<>();
        for (int i = 0; i < files.length; i++) {
            if (!files[i].getName().equals("lost+found") && !files[i].getName().equalsIgnoreCase("recovery")) {
                fileSet.add(files[i].getPath());
            }
        }
        Cursor cursor = getContentResolver().query(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, new String[]{"_data"}, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    fileSet.remove(cursor.getString(0));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        for (String filename : fileSet) {
            delete(new File(filename));
        }
    }

    private static void delete(File f) {
        File[] files;
        if (f.isFile()) {
            f.delete();
        }
        if (!f.isDirectory() || (files = f.listFiles()) == null) {
            return;
        }
        for (File file : files) {
            delete(file);
        }
        f.delete();
    }

    public void CancelAction(boolean action) {
        Log.v("CancelAction", "start");
        File Updatefile = new File("/cache/update.zip");
        if (Updatefile.exists()) {
            Updatefile.delete();
        }
        File Commandfile = new File("/cache/recovery/command");
        if (Commandfile.exists()) {
            Commandfile.delete();
        }
        if (!action || !this.ProgressDialogActive) {
            return;
        }
        Log.v("CancelAction", "progressDialog.dismiss");
        this.ProgressDialogActive = false;
        this.progressDialog.dismiss();
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
}
