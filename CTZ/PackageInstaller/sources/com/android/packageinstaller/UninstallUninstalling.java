package com.android.packageinstaller;

import android.app.Activity;
import android.app.ActivityThread;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDeleteObserver2;
import android.content.pm.IPackageInstaller;
import android.content.pm.VersionedPackage;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import android.widget.Toast;
import com.android.packageinstaller.EventResultPersister;

public class UninstallUninstalling extends Activity implements EventResultPersister.EventResultObserver {
    private static final String LOG_TAG = UninstallUninstalling.class.getSimpleName();
    private ApplicationInfo mAppInfo;
    private IBinder mCallback;
    private String mLabel;
    private boolean mReturnResult;
    private int mUninstallId;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        int i = 0;
        setFinishOnTouchOutside(false);
        this.mAppInfo = (ApplicationInfo) getIntent().getParcelableExtra("com.android.packageinstaller.applicationInfo");
        this.mCallback = getIntent().getIBinderExtra("android.content.pm.extra.CALLBACK");
        this.mReturnResult = getIntent().getBooleanExtra("android.intent.extra.RETURN_RESULT", false);
        this.mLabel = getIntent().getStringExtra("com.android.packageinstaller.extra.APP_LABEL");
        try {
            if (bundle == null) {
                boolean booleanExtra = getIntent().getBooleanExtra("android.intent.extra.UNINSTALL_ALL_USERS", false);
                UserHandle userHandle = (UserHandle) getIntent().getParcelableExtra("android.intent.extra.USER");
                FragmentTransaction fragmentTransactionBeginTransaction = getFragmentManager().beginTransaction();
                Fragment fragmentFindFragmentByTag = getFragmentManager().findFragmentByTag("dialog");
                if (fragmentFindFragmentByTag != null) {
                    fragmentTransactionBeginTransaction.remove(fragmentFindFragmentByTag);
                }
                UninstallUninstallingFragment uninstallUninstallingFragment = new UninstallUninstallingFragment();
                uninstallUninstallingFragment.setCancelable(false);
                uninstallUninstallingFragment.show(fragmentTransactionBeginTransaction, "dialog");
                this.mUninstallId = UninstallEventReceiver.addObserver(this, Integer.MIN_VALUE, this);
                Intent intent = new Intent("com.android.packageinstaller.ACTION_UNINSTALL_COMMIT");
                intent.setFlags(268435456);
                intent.putExtra("EventResultPersister.EXTRA_ID", this.mUninstallId);
                intent.setPackage(getPackageName());
                PendingIntent broadcast = PendingIntent.getBroadcast(this, this.mUninstallId, intent, 134217728);
                try {
                    IPackageInstaller packageInstaller = ActivityThread.getPackageManager().getPackageInstaller();
                    VersionedPackage versionedPackage = new VersionedPackage(this.mAppInfo.packageName, -1);
                    String packageName = getPackageName();
                    if (booleanExtra) {
                        i = 2;
                    }
                    packageInstaller.uninstall(versionedPackage, packageName, i, broadcast.getIntentSender(), userHandle.getIdentifier());
                } catch (RemoteException e) {
                    e.rethrowFromSystemServer();
                }
                return;
            }
            this.mUninstallId = bundle.getInt("com.android.packageinstaller.UNINSTALL_ID");
            UninstallEventReceiver.addObserver(this, this.mUninstallId, this);
        } catch (EventResultPersister.OutOfIdsException | IllegalArgumentException e2) {
            Log.e(LOG_TAG, "Fails to start uninstall", e2);
            onResult(1, -1, null);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putInt("com.android.packageinstaller.UNINSTALL_ID", this.mUninstallId);
    }

    @Override
    public void onBackPressed() {
    }

    @Override
    public void onResult(int i, int i2, String str) {
        if (this.mCallback != null) {
            try {
                IPackageDeleteObserver2.Stub.asInterface(this.mCallback).onPackageDeleted(this.mAppInfo.packageName, i2, str);
            } catch (RemoteException e) {
            }
        } else {
            if (this.mReturnResult) {
                Intent intent = new Intent();
                intent.putExtra("android.intent.extra.INSTALL_RESULT", i2);
                setResult(i == 0 ? -1 : 1, intent);
            } else if (i != 0) {
                Toast.makeText(this, getString(R.string.uninstall_failed_app, new Object[]{this.mLabel}), 1).show();
            }
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        UninstallEventReceiver.removeObserver(this, this.mUninstallId);
        super.onDestroy();
    }

    public static class UninstallUninstallingFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle bundle) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setCancelable(false);
            builder.setMessage(getActivity().getString(R.string.uninstalling_app, new Object[]{((UninstallUninstalling) getActivity()).mLabel}));
            AlertDialog alertDialogCreate = builder.create();
            alertDialogCreate.setCanceledOnTouchOutside(false);
            return alertDialogCreate;
        }
    }
}
