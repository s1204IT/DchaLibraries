package com.android.packageinstaller;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import com.android.internal.content.PackageHelper;
import com.android.packageinstaller.EventResultPersister;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class InstallInstalling extends Activity {
    private static final String LOG_TAG = InstallInstalling.class.getSimpleName();
    private Button mCancelButton;
    private int mInstallId;
    private InstallingAsyncTask mInstallingTask;
    private Uri mPackageURI;
    private PackageInstaller.SessionCallback mSessionCallback;
    private int mSessionId;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.install_installing);
        ApplicationInfo applicationInfo = (ApplicationInfo) getIntent().getParcelableExtra("com.android.packageinstaller.applicationInfo");
        this.mPackageURI = getIntent().getData();
        if ("package".equals(this.mPackageURI.getScheme())) {
            try {
                getPackageManager().installExistingPackage(applicationInfo.packageName);
                launchSuccess();
                return;
            } catch (PackageManager.NameNotFoundException e) {
                launchFailure(-110, null);
                return;
            }
        }
        PackageUtil.initSnippetForNewApp(this, PackageUtil.getAppSnippet(this, applicationInfo, new File(this.mPackageURI.getPath())), R.id.app_snippet);
        if (bundle != null) {
            this.mSessionId = bundle.getInt("com.android.packageinstaller.SESSION_ID");
            this.mInstallId = bundle.getInt("com.android.packageinstaller.INSTALL_ID");
            try {
                InstallEventReceiver.addObserver(this, this.mInstallId, new EventResultPersister.EventResultObserver() {
                    @Override
                    public final void onResult(int i, int i2, String str) {
                        this.f$0.launchFinishBasedOnResult(i, i2, str);
                    }
                });
            } catch (EventResultPersister.OutOfIdsException e2) {
            }
        } else {
            PackageInstaller.SessionParams sessionParams = new PackageInstaller.SessionParams(1);
            sessionParams.installFlags = 16384;
            sessionParams.referrerUri = (Uri) getIntent().getParcelableExtra("android.intent.extra.REFERRER");
            sessionParams.originatingUri = (Uri) getIntent().getParcelableExtra("android.intent.extra.ORIGINATING_URI");
            sessionParams.originatingUid = getIntent().getIntExtra("android.intent.extra.ORIGINATING_UID", -1);
            sessionParams.installerPackageName = getIntent().getStringExtra("android.intent.extra.INSTALLER_PACKAGE_NAME");
            File file = new File(this.mPackageURI.getPath());
            try {
                PackageParser.PackageLite packageLite = PackageParser.parsePackageLite(file, 0);
                sessionParams.setAppPackageName(packageLite.packageName);
                sessionParams.setInstallLocation(packageLite.installLocation);
                sessionParams.setSize(PackageHelper.calculateInstalledSize(packageLite, false, sessionParams.abiOverride));
            } catch (PackageParser.PackageParserException e3) {
                Log.e(LOG_TAG, "Cannot parse package " + file + ". Assuming defaults.");
                Log.e(LOG_TAG, "Cannot calculate installed size " + file + ". Try only apk size.");
                sessionParams.setSize(file.length());
            } catch (IOException e4) {
                Log.e(LOG_TAG, "Cannot calculate installed size " + file + ". Try only apk size.");
                sessionParams.setSize(file.length());
            }
            try {
                this.mInstallId = InstallEventReceiver.addObserver(this, Integer.MIN_VALUE, new EventResultPersister.EventResultObserver() {
                    @Override
                    public final void onResult(int i, int i2, String str) {
                        this.f$0.launchFinishBasedOnResult(i, i2, str);
                    }
                });
            } catch (EventResultPersister.OutOfIdsException e5) {
                launchFailure(-110, null);
            }
            try {
                this.mSessionId = getPackageManager().getPackageInstaller().createSession(sessionParams);
            } catch (IOException e6) {
                launchFailure(-110, null);
            }
        }
        this.mCancelButton = (Button) findViewById(R.id.cancel_button);
        this.mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                InstallInstalling.lambda$onCreate$0(this.f$0, view);
            }
        });
        this.mSessionCallback = new InstallSessionCallback();
    }

    public static void lambda$onCreate$0(InstallInstalling installInstalling, View view) {
        if (installInstalling.mInstallingTask != null) {
            installInstalling.mInstallingTask.cancel(true);
        }
        if (installInstalling.mSessionId > 0) {
            installInstalling.getPackageManager().getPackageInstaller().abandonSession(installInstalling.mSessionId);
            installInstalling.mSessionId = 0;
        }
        installInstalling.setResult(0);
        installInstalling.finish();
    }

    private void launchSuccess() {
        Intent intent = new Intent(getIntent());
        intent.setClass(this, InstallSuccess.class);
        intent.addFlags(33554432);
        startActivity(intent);
        finish();
    }

    private void launchFailure(int i, String str) {
        Intent intent = new Intent(getIntent());
        intent.setClass(this, InstallFailed.class);
        intent.addFlags(33554432);
        intent.putExtra("android.content.pm.extra.LEGACY_STATUS", i);
        intent.putExtra("android.content.pm.extra.STATUS_MESSAGE", str);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        getPackageManager().getPackageInstaller().registerSessionCallback(this.mSessionCallback);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.mInstallingTask == null) {
            PackageInstaller.SessionInfo sessionInfo = getPackageManager().getPackageInstaller().getSessionInfo(this.mSessionId);
            if (sessionInfo == null || sessionInfo.isActive()) {
                this.mCancelButton.setEnabled(false);
                setFinishOnTouchOutside(false);
            } else {
                this.mInstallingTask = new InstallingAsyncTask();
                this.mInstallingTask.execute(new Void[0]);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putInt("com.android.packageinstaller.SESSION_ID", this.mSessionId);
        bundle.putInt("com.android.packageinstaller.INSTALL_ID", this.mInstallId);
    }

    @Override
    public void onBackPressed() {
        if (this.mCancelButton.isEnabled()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        getPackageManager().getPackageInstaller().unregisterSessionCallback(this.mSessionCallback);
    }

    @Override
    protected void onDestroy() {
        if (this.mInstallingTask != null) {
            this.mInstallingTask.cancel(true);
            synchronized (this.mInstallingTask) {
                while (!this.mInstallingTask.isDone) {
                    try {
                        this.mInstallingTask.wait();
                    } catch (InterruptedException e) {
                        Log.i(LOG_TAG, "Interrupted while waiting for installing task to cancel", e);
                    }
                }
            }
        }
        InstallEventReceiver.removeObserver(this, this.mInstallId);
        super.onDestroy();
    }

    private void launchFinishBasedOnResult(int i, int i2, String str) {
        if (i == 0) {
            launchSuccess();
        } else {
            launchFailure(i2, str);
        }
    }

    private class InstallSessionCallback extends PackageInstaller.SessionCallback {
        private InstallSessionCallback() {
        }

        @Override
        public void onCreated(int i) {
        }

        @Override
        public void onBadgingChanged(int i) {
        }

        @Override
        public void onActiveChanged(int i, boolean z) {
        }

        @Override
        public void onProgressChanged(int i, float f) {
            if (i == InstallInstalling.this.mSessionId) {
                ProgressBar progressBar = (ProgressBar) InstallInstalling.this.findViewById(R.id.progress_bar);
                progressBar.setMax(Preference.DEFAULT_ORDER);
                progressBar.setProgress((int) (2.1474836E9f * f));
            }
        }

        @Override
        public void onFinished(int i, boolean z) {
        }
    }

    private final class InstallingAsyncTask extends AsyncTask<Void, Void, PackageInstaller.Session> {
        volatile boolean isDone;

        private InstallingAsyncTask() {
        }

        @Override
        protected PackageInstaller.Session doInBackground(Void... voidArr) {
            Throwable th;
            Throwable th2;
            try {
                PackageInstaller.Session sessionOpenSession = InstallInstalling.this.getPackageManager().getPackageInstaller().openSession(InstallInstalling.this.mSessionId);
                sessionOpenSession.setStagingProgress(0.0f);
                try {
                    try {
                        File file = new File(InstallInstalling.this.mPackageURI.getPath());
                        FileInputStream fileInputStream = new FileInputStream(file);
                        try {
                            long length = file.length();
                            OutputStream outputStreamOpenWrite = sessionOpenSession.openWrite("PackageInstaller", 0L, length);
                            try {
                                byte[] bArr = new byte[1048576];
                                while (true) {
                                    int i = fileInputStream.read(bArr);
                                    if (i == -1) {
                                        sessionOpenSession.fsync(outputStreamOpenWrite);
                                        break;
                                    }
                                    if (isCancelled()) {
                                        sessionOpenSession.close();
                                        break;
                                    }
                                    outputStreamOpenWrite.write(bArr, 0, i);
                                    if (length > 0) {
                                        sessionOpenSession.addProgress(i / length);
                                    }
                                }
                                if (outputStreamOpenWrite != null) {
                                    $closeResource(null, outputStreamOpenWrite);
                                }
                                $closeResource(null, fileInputStream);
                                synchronized (this) {
                                    this.isDone = true;
                                    notifyAll();
                                }
                                return sessionOpenSession;
                            } catch (Throwable th3) {
                                th = th3;
                                th2 = null;
                                if (outputStreamOpenWrite != null) {
                                }
                            }
                        } catch (Throwable th4) {
                            th = th4;
                            th = null;
                            $closeResource(th, fileInputStream);
                            throw th;
                        }
                    } catch (IOException | SecurityException e) {
                        Log.e(InstallInstalling.LOG_TAG, "Could not write package", e);
                        sessionOpenSession.close();
                        synchronized (this) {
                            this.isDone = true;
                            notifyAll();
                            return null;
                        }
                    }
                } catch (Throwable th5) {
                    synchronized (this) {
                        this.isDone = true;
                        notifyAll();
                        throw th5;
                    }
                }
            } catch (IOException e2) {
                return null;
            }
        }

        private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
            if (th == null) {
                autoCloseable.close();
                return;
            }
            try {
                autoCloseable.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
        }

        @Override
        protected void onPostExecute(PackageInstaller.Session session) {
            if (session == null) {
                InstallInstalling.this.getPackageManager().getPackageInstaller().abandonSession(InstallInstalling.this.mSessionId);
                if (!isCancelled()) {
                    InstallInstalling.this.launchFailure(-2, null);
                    return;
                }
                return;
            }
            Intent intent = new Intent("com.android.packageinstaller.ACTION_INSTALL_COMMIT");
            intent.setFlags(268435456);
            intent.setPackage(InstallInstalling.this.getPackageManager().getPermissionControllerPackageName());
            intent.putExtra("EventResultPersister.EXTRA_ID", InstallInstalling.this.mInstallId);
            session.commit(PendingIntent.getBroadcast(InstallInstalling.this, InstallInstalling.this.mInstallId, intent, 134217728).getIntentSender());
            InstallInstalling.this.mCancelButton.setEnabled(false);
            InstallInstalling.this.setFinishOnTouchOutside(false);
        }
    }
}
