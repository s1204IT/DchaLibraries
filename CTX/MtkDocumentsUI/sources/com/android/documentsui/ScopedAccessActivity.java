package com.android.documentsui;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.app.GrantedUriPermission;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.UserHandle;
import android.os.storage.StorageVolume;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import com.android.documentsui.base.SharedMinimal;
import com.android.documentsui.prefs.ScopedAccessLocalPreferences;
import java.io.File;

public class ScopedAccessActivity extends Activity {
    private ContentProviderClient mExternalStorageClient;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (bundle != null) {
            if (SharedMinimal.DEBUG) {
                Log.d("ScopedAccessActivity", "activity.onCreateDialog(): reusing instance");
                return;
            }
            return;
        }
        Intent intent = getIntent();
        if (intent == null) {
            if (SharedMinimal.DEBUG) {
                Log.d("ScopedAccessActivity", "missing intent");
            }
            ScopedAccessMetrics.logInvalidScopedAccessRequest(this, "docsui_scoped_directory_access_invalid_args");
            setResult(0);
            finish();
            return;
        }
        Parcelable parcelableExtra = intent.getParcelableExtra("android.os.storage.extra.STORAGE_VOLUME");
        if (!(parcelableExtra instanceof StorageVolume)) {
            if (SharedMinimal.DEBUG) {
                Log.d("ScopedAccessActivity", "extra android.os.storage.extra.STORAGE_VOLUME is not a StorageVolume: " + parcelableExtra);
            }
            ScopedAccessMetrics.logInvalidScopedAccessRequest(this, "docsui_scoped_directory_access_invalid_args");
            setResult(0);
            finish();
            return;
        }
        String internalDirectoryName = SharedMinimal.getInternalDirectoryName(intent.getStringExtra("android.os.storage.extra.DIRECTORY_NAME"));
        StorageVolume storageVolume = (StorageVolume) parcelableExtra;
        if (ScopedAccessLocalPreferences.getScopedAccessPermissionStatus(getApplicationContext(), getCallingPackage(), storageVolume.getUuid(), internalDirectoryName) == -1) {
            ScopedAccessMetrics.logValidScopedAccessRequest(this, internalDirectoryName, 4);
            setResult(0);
            finish();
        } else if (!showFragment(this, UserHandle.myUserId(), storageVolume, internalDirectoryName)) {
            setResult(0);
            finish();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.mExternalStorageClient != null) {
            this.mExternalStorageClient.close();
        }
    }

    private static boolean showFragment(final ScopedAccessActivity scopedAccessActivity, int i, final StorageVolume storageVolume, final String str) {
        return SharedMinimal.getUriPermission(scopedAccessActivity, scopedAccessActivity.getExternalStorageClient(), storageVolume, str, i, true, new SharedMinimal.GetUriPermissionCallback() {
            @Override
            public final boolean onResult(File file, String str2, boolean z, boolean z2, Uri uri, Uri uri2) {
                return ScopedAccessActivity.lambda$showFragment$0(this.f$0, str, storageVolume, file, str2, z, z2, uri, uri2);
            }
        });
    }

    static boolean lambda$showFragment$0(ScopedAccessActivity scopedAccessActivity, String str, StorageVolume storageVolume, File file, String str2, boolean z, boolean z2, Uri uri, Uri uri2) {
        Intent intentForExistingPermission = getIntentForExistingPermission(scopedAccessActivity, scopedAccessActivity.getCallingPackage(), uri, uri2);
        if (intentForExistingPermission != null) {
            if (z) {
                str = ".";
            }
            ScopedAccessMetrics.logValidScopedAccessRequest(scopedAccessActivity, str, 0);
            scopedAccessActivity.setResult(-1, intentForExistingPermission);
            scopedAccessActivity.finish();
            return true;
        }
        if (BenesseExtension.getDchaState() != 0) {
            Intent intentCreateGrantedUriPermissionsIntent = createGrantedUriPermissionsIntent(scopedAccessActivity, scopedAccessActivity.getExternalStorageClient(), file);
            if (intentCreateGrantedUriPermissionsIntent == null) {
                if (z) {
                    str = ".";
                }
                ScopedAccessMetrics.logValidScopedAccessRequest(scopedAccessActivity, str, 2);
                scopedAccessActivity.setResult(0);
            } else {
                if (z) {
                    str = ".";
                }
                ScopedAccessMetrics.logValidScopedAccessRequest(scopedAccessActivity, str, 1);
                scopedAccessActivity.setResult(-1, intentCreateGrantedUriPermissionsIntent);
            }
            scopedAccessActivity.finish();
            return true;
        }
        String appLabel = getAppLabel(scopedAccessActivity);
        if (appLabel == null) {
            return false;
        }
        Bundle bundle = new Bundle();
        bundle.putString("com.android.documentsui.FILE", file.getAbsolutePath());
        bundle.putString("com.android.documentsui.VOLUME_LABEL", str2);
        bundle.putString("com.android.documentsui.VOLUME_UUID", storageVolume.getUuid());
        bundle.putString("com.android.documentsui.APP_LABEL", appLabel);
        bundle.putBoolean("com.android.documentsui.IS_ROOT", z);
        bundle.putBoolean("com.android.documentsui.IS_PRIMARY", z2);
        FragmentTransaction fragmentTransactionBeginTransaction = scopedAccessActivity.getFragmentManager().beginTransaction();
        ScopedAccessDialogFragment scopedAccessDialogFragment = new ScopedAccessDialogFragment();
        scopedAccessDialogFragment.setArguments(bundle);
        fragmentTransactionBeginTransaction.add(scopedAccessDialogFragment, "open_external_directory");
        fragmentTransactionBeginTransaction.commitAllowingStateLoss();
        return true;
    }

    private static String getAppLabel(Activity activity) {
        String callingPackage = activity.getCallingPackage();
        PackageManager packageManager = activity.getPackageManager();
        try {
            return packageManager.getApplicationLabel(packageManager.getApplicationInfo(callingPackage, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            ScopedAccessMetrics.logInvalidScopedAccessRequest(activity, "docsui_scoped_directory_access_error");
            Log.w("ScopedAccessActivity", "Could not get label for package " + callingPackage);
            return null;
        }
    }

    private static Intent createGrantedUriPermissionsIntent(Context context, ContentProviderClient contentProviderClient, File file) {
        return createGrantedUriPermissionsIntent(SharedMinimal.getUriPermission(context, contentProviderClient, file));
    }

    private static Intent createGrantedUriPermissionsIntent(Uri uri) {
        Intent intent = new Intent();
        intent.setData(uri);
        intent.addFlags(195);
        return intent;
    }

    private static Intent getIntentForExistingPermission(Context context, String str, Uri uri, Uri uri2) {
        if (SharedMinimal.DEBUG) {
            Log.d("ScopedAccessActivity", "checking if " + str + " already has permission for " + uri + " or its root (" + uri2 + ")");
        }
        for (GrantedUriPermission grantedUriPermission : ((ActivityManager) context.getSystemService(ActivityManager.class)).getGrantedUriPermissions(str).getList()) {
            Uri uri3 = grantedUriPermission.uri;
            if (uri3 == null) {
                Log.w("ScopedAccessActivity", "null URI for " + grantedUriPermission);
            } else if (uri3.equals(uri) || uri3.equals(uri2)) {
                if (SharedMinimal.DEBUG) {
                    Log.d("ScopedAccessActivity", str + " already has permission: " + grantedUriPermission);
                }
                return createGrantedUriPermissionsIntent(uri);
            }
        }
        if (SharedMinimal.DEBUG) {
            Log.d("ScopedAccessActivity", str + " does not have permission for " + uri);
            return null;
        }
        return null;
    }

    public static class ScopedAccessDialogFragment extends DialogFragment {
        private ScopedAccessActivity mActivity;
        private String mAppLabel;
        private AlertDialog mDialog;
        private CheckBox mDontAskAgain;
        private File mFile;
        private boolean mIsPrimary;
        private boolean mIsRoot;
        private String mVolumeLabel;
        private String mVolumeUuid;

        @Override
        public void onCreate(Bundle bundle) {
            super.onCreate(bundle);
            setRetainInstance(true);
            Bundle arguments = getArguments();
            if (arguments != null) {
                this.mFile = new File(arguments.getString("com.android.documentsui.FILE"));
                this.mVolumeUuid = arguments.getString("com.android.documentsui.VOLUME_UUID");
                this.mVolumeLabel = arguments.getString("com.android.documentsui.VOLUME_LABEL");
                this.mAppLabel = arguments.getString("com.android.documentsui.APP_LABEL");
                this.mIsRoot = arguments.getBoolean("com.android.documentsui.IS_ROOT");
                this.mIsPrimary = arguments.getBoolean("com.android.documentsui.IS_PRIMARY");
            }
            this.mActivity = (ScopedAccessActivity) getActivity();
        }

        @Override
        public void onDestroyView() {
            if (this.mDialog != null && getRetainInstance()) {
                this.mDialog.setDismissMessage(null);
            }
            super.onDestroyView();
        }

        @Override
        public Dialog onCreateDialog(Bundle bundle) {
            CharSequence charSequenceExpandTemplate;
            if (this.mDialog != null) {
                if (SharedMinimal.DEBUG) {
                    Log.d("ScopedAccessActivity", "fragment.onCreateDialog(): reusing dialog");
                }
                return this.mDialog;
            }
            if (this.mActivity != getActivity()) {
                Log.wtf("ScopedAccessActivity", "activity references don't match on onCreateDialog(): mActivity = " + this.mActivity + " , getActivity() = " + getActivity());
                this.mActivity = (ScopedAccessActivity) getActivity();
            }
            final String name = this.mFile.getName();
            final String str = this.mIsRoot ? "ROOT_DIRECTORY" : name;
            final Context applicationContext = this.mActivity.getApplicationContext();
            DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Intent intentCreateGrantedUriPermissionsIntent;
                    if (i == -1) {
                        intentCreateGrantedUriPermissionsIntent = ScopedAccessActivity.createGrantedUriPermissionsIntent(ScopedAccessDialogFragment.this.mActivity, ScopedAccessDialogFragment.this.mActivity.getExternalStorageClient(), ScopedAccessDialogFragment.this.mFile);
                    } else {
                        intentCreateGrantedUriPermissionsIntent = null;
                    }
                    if (i == -2 || intentCreateGrantedUriPermissionsIntent == null) {
                        ScopedAccessMetrics.logValidScopedAccessRequest(ScopedAccessDialogFragment.this.mActivity, str, 2);
                        if (ScopedAccessDialogFragment.this.mDontAskAgain.isChecked()) {
                            ScopedAccessMetrics.logValidScopedAccessRequest(ScopedAccessDialogFragment.this.mActivity, name, 3);
                            ScopedAccessLocalPreferences.setScopedAccessPermissionStatus(applicationContext, ScopedAccessDialogFragment.this.mActivity.getCallingPackage(), ScopedAccessDialogFragment.this.mVolumeUuid, str, -1);
                        } else {
                            ScopedAccessLocalPreferences.setScopedAccessPermissionStatus(applicationContext, ScopedAccessDialogFragment.this.mActivity.getCallingPackage(), ScopedAccessDialogFragment.this.mVolumeUuid, str, 1);
                        }
                        ScopedAccessDialogFragment.this.mActivity.setResult(0);
                    } else {
                        ScopedAccessMetrics.logValidScopedAccessRequest(ScopedAccessDialogFragment.this.mActivity, name, 1);
                        ScopedAccessDialogFragment.this.mActivity.setResult(-1, intentCreateGrantedUriPermissionsIntent);
                    }
                    ScopedAccessDialogFragment.this.mActivity.finish();
                }
            };
            View viewInflate = View.inflate(this.mActivity, R.layout.dialog_open_scoped_directory, null);
            if (this.mIsRoot) {
                charSequenceExpandTemplate = TextUtils.expandTemplate(getText(R.string.open_external_dialog_root_request), this.mAppLabel, this.mVolumeLabel);
            } else {
                charSequenceExpandTemplate = TextUtils.expandTemplate(getText(this.mIsPrimary ? R.string.open_external_dialog_request_primary_volume : R.string.open_external_dialog_request), this.mAppLabel, name, this.mVolumeLabel);
            }
            ((TextView) viewInflate.findViewById(R.id.message)).setText(charSequenceExpandTemplate);
            this.mDialog = new AlertDialog.Builder(this.mActivity, R.style.Theme_AppCompat_Light_Dialog_Alert).setView(viewInflate).setPositiveButton(R.string.allow, onClickListener).setNegativeButton(R.string.deny, onClickListener).create();
            this.mDontAskAgain = (CheckBox) viewInflate.findViewById(R.id.do_not_ask_checkbox);
            if (ScopedAccessLocalPreferences.getScopedAccessPermissionStatus(applicationContext, this.mActivity.getCallingPackage(), this.mVolumeUuid, str) == 1) {
                this.mDontAskAgain.setVisibility(0);
                this.mDontAskAgain.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                        ScopedAccessDialogFragment.this.mDialog.getButton(-1).setEnabled(!z);
                    }
                });
            }
            return this.mDialog;
        }

        @Override
        public void onCancel(DialogInterface dialogInterface) {
            super.onCancel(dialogInterface);
            Activity activity = getActivity();
            ScopedAccessMetrics.logValidScopedAccessRequest(activity, this.mFile.getName(), 2);
            activity.setResult(0);
            activity.finish();
        }
    }

    private synchronized ContentProviderClient getExternalStorageClient() {
        if (this.mExternalStorageClient == null) {
            this.mExternalStorageClient = getContentResolver().acquireContentProviderClient("com.android.externalstorage.documents");
        }
        return this.mExternalStorageClient;
    }
}
