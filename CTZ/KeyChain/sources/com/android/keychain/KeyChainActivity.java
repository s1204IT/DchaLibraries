package com.android.keychain;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.admin.IDevicePolicyManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.security.Credentials;
import android.security.IKeyChainAliasCallback;
import android.security.KeyChain;
import android.security.KeyStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import com.android.internal.annotations.VisibleForTesting;
import com.android.keychain.internal.KeyInfoProvider;
import com.android.org.bouncycastle.asn1.x509.X509Name;
import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class KeyChainActivity extends Activity {
    private static String KEY_STATE = "state";
    private KeyStore mKeyStore = KeyStore.getInstance();
    private PendingIntent mSender;
    private int mSenderUid;
    private State mState;

    private enum State {
        INITIAL,
        UNLOCK_REQUESTED,
        UNLOCK_CANCELED
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (bundle == null) {
            this.mState = State.INITIAL;
            return;
        }
        this.mState = (State) bundle.getSerializable(KEY_STATE);
        if (this.mState == null) {
            this.mState = State.INITIAL;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mSender = (PendingIntent) getIntent().getParcelableExtra("sender");
        if (this.mSender == null) {
            finish(null);
            return;
        }
        try {
            this.mSenderUid = getPackageManager().getPackageInfo(this.mSender.getIntentSender().getTargetPackage(), 0).applicationInfo.uid;
            switch (AnonymousClass8.$SwitchMap$com$android$keychain$KeyChainActivity$State[this.mState.ordinal()]) {
                case 1:
                    if (!this.mKeyStore.isUnlocked()) {
                        if (BenesseExtension.getDchaState() != 0) {
                            throw new AssertionError();
                        }
                        this.mState = State.UNLOCK_REQUESTED;
                        startActivityForResult(new Intent("com.android.credentials.UNLOCK"), 1);
                        return;
                    }
                    chooseCertificate();
                    return;
                case 2:
                    return;
                case 3:
                    this.mState = State.INITIAL;
                    finish(null);
                    return;
                default:
                    throw new AssertionError();
            }
        } catch (PackageManager.NameNotFoundException e) {
            finish(null);
        }
    }

    static class AnonymousClass8 {
        static final int[] $SwitchMap$com$android$keychain$KeyChainActivity$State = new int[State.values().length];

        static {
            try {
                $SwitchMap$com$android$keychain$KeyChainActivity$State[State.INITIAL.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$keychain$KeyChainActivity$State[State.UNLOCK_REQUESTED.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$keychain$KeyChainActivity$State[State.UNLOCK_CANCELED.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }

    private void chooseCertificate() {
        final AliasLoader aliasLoader = new AliasLoader(this.mKeyStore, this, new KeyInfoProvider() {
            @Override
            public boolean isUserSelectable(String str) {
                try {
                    KeyChain.KeyChainConnection keyChainConnectionBind = KeyChain.bind(KeyChainActivity.this);
                    Throwable th = null;
                    try {
                        boolean zIsUserSelectable = keyChainConnectionBind.getService().isUserSelectable(str);
                        if (keyChainConnectionBind != null) {
                            keyChainConnectionBind.close();
                        }
                        return zIsUserSelectable;
                    } catch (Throwable th2) {
                        if (keyChainConnectionBind != null) {
                            if (0 != 0) {
                                try {
                                    keyChainConnectionBind.close();
                                } catch (Throwable th3) {
                                    th.addSuppressed(th3);
                                }
                            } else {
                                keyChainConnectionBind.close();
                            }
                        }
                        throw th2;
                    }
                } catch (InterruptedException e) {
                    Log.e("KeyChain", "interrupted while checking if key is user-selectable", e);
                    Thread.currentThread().interrupt();
                    return false;
                } catch (Exception e2) {
                    Log.e("KeyChain", "error while checking if key is user-selectable", e2);
                    return false;
                }
            }
        });
        aliasLoader.execute(new Void[0]);
        IKeyChainAliasCallback.Stub stub = new IKeyChainAliasCallback.Stub() {
            public void alias(String str) {
                if (str != null) {
                    KeyChainActivity.this.finishWithAliasFromPolicy(str);
                    return;
                }
                try {
                    final CertificateAdapter certificateAdapter = aliasLoader.get();
                    KeyChainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            KeyChainActivity.this.displayCertChooserDialog(certificateAdapter);
                        }
                    });
                } catch (InterruptedException | ExecutionException e) {
                    Log.e("KeyChain", "Loading certificate aliases interrupted", e);
                    KeyChainActivity.this.finish(null);
                }
            }
        };
        try {
            IDevicePolicyManager.Stub.asInterface(ServiceManager.getService("device_policy")).choosePrivateKeyAlias(this.mSenderUid, (Uri) getIntent().getParcelableExtra("uri"), getIntent().getStringExtra("alias"), stub);
        } catch (RemoteException e) {
            Log.e("KeyChain", "Unable to request alias from DevicePolicyManager", e);
            try {
                stub.alias((String) null);
            } catch (RemoteException e2) {
                finish(null);
            }
        }
    }

    @VisibleForTesting
    static class AliasLoader extends AsyncTask<Void, Void, CertificateAdapter> {
        private final Context mContext;
        private final KeyInfoProvider mInfoProvider;
        private final KeyStore mKeyStore;

        public AliasLoader(KeyStore keyStore, Context context, KeyInfoProvider keyInfoProvider) {
            this.mKeyStore = keyStore;
            this.mContext = context;
            this.mInfoProvider = keyInfoProvider;
        }

        @Override
        protected CertificateAdapter doInBackground(Void... voidArr) {
            List listAsList;
            String[] list = this.mKeyStore.list("USRPKEY_");
            if (list == null) {
                listAsList = Collections.emptyList();
            } else {
                listAsList = Arrays.asList(list);
            }
            KeyStore keyStore = this.mKeyStore;
            Context context = this.mContext;
            Stream stream = listAsList.stream();
            final KeyInfoProvider keyInfoProvider = this.mInfoProvider;
            Objects.requireNonNull(keyInfoProvider);
            return new CertificateAdapter(keyStore, context, (List) stream.filter(new Predicate() {
                @Override
                public final boolean test(Object obj) {
                    return keyInfoProvider.isUserSelectable((String) obj);
                }
            }).sorted().collect(Collectors.toList()));
        }
    }

    private void displayCertChooserDialog(final CertificateAdapter certificateAdapter) {
        String string;
        int i;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        boolean zIsEmpty = certificateAdapter.mAliases.isEmpty();
        builder.setNegativeButton(zIsEmpty ? android.R.string.cancel : R.string.deny_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i2) {
                dialogInterface.cancel();
            }
        });
        Resources resources = getResources();
        if (zIsEmpty) {
            string = resources.getString(R.string.title_no_certs);
            i = -1;
        } else {
            string = resources.getString(R.string.title_select_cert);
            String stringExtra = getIntent().getStringExtra("alias");
            if (stringExtra != null) {
                int iIndexOf = certificateAdapter.mAliases.indexOf(stringExtra);
                if (iIndexOf != -1) {
                    i = iIndexOf + 1;
                } else {
                    i = -1;
                }
            } else if (certificateAdapter.mAliases.size() != 1) {
                i = -1;
            } else {
                i = 1;
            }
            builder.setPositiveButton(R.string.allow_button, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i2) {
                    if (dialogInterface instanceof AlertDialog) {
                        int checkedItemPosition = ((AlertDialog) dialogInterface).getListView().getCheckedItemPosition() - 1;
                        KeyChainActivity.this.finish(checkedItemPosition >= 0 ? certificateAdapter.getItem(checkedItemPosition) : null);
                        return;
                    }
                    Log.wtf("KeyChain", "Expected AlertDialog, got " + dialogInterface, new Exception());
                    KeyChainActivity.this.finish(null);
                }
            });
        }
        builder.setTitle(string);
        builder.setSingleChoiceItems(certificateAdapter, i, (DialogInterface.OnClickListener) null);
        final AlertDialog alertDialogCreate = builder.create();
        TextView textView = (TextView) View.inflate(this, R.layout.cert_chooser_header, null);
        final ListView listView = alertDialogCreate.getListView();
        listView.addHeaderView(textView, null, false);
        listView.addFooterView(View.inflate(this, R.layout.cert_install, null));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i2, long j) {
                if (i2 == 0) {
                    return;
                }
                if (i2 == certificateAdapter.getCount() + 1) {
                    alertDialogCreate.dismiss();
                    Credentials.getInstance().install(KeyChainActivity.this);
                } else {
                    alertDialogCreate.getButton(-1).setEnabled(true);
                    listView.setItemChecked(i2, true);
                    certificateAdapter.notifyDataSetChanged();
                }
            }
        });
        String targetPackage = this.mSender.getIntentSender().getTargetPackage();
        PackageManager packageManager = getPackageManager();
        try {
            targetPackage = packageManager.getApplicationLabel(packageManager.getApplicationInfo(targetPackage, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
        }
        String str = String.format(resources.getString(R.string.requesting_application), targetPackage);
        Uri uri = (Uri) getIntent().getParcelableExtra("uri");
        if (uri != null) {
            String str2 = String.format(resources.getString(R.string.requesting_server), uri.getAuthority());
            if (str != null) {
                str = str + " " + str2;
            } else {
                str = str2;
            }
        }
        textView.setText(str);
        if (i == -1) {
            alertDialogCreate.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialogInterface) {
                    alertDialogCreate.getButton(-1).setEnabled(false);
                }
            });
        }
        alertDialogCreate.create();
        alertDialogCreate.getButton(-1).setFilterTouchesWhenObscured(true);
        alertDialogCreate.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                KeyChainActivity.this.finish(null);
            }
        });
        alertDialogCreate.show();
    }

    @VisibleForTesting
    static class CertificateAdapter extends BaseAdapter {
        private final List<String> mAliases;
        private final Context mContext;
        private final KeyStore mKeyStore;
        private final List<String> mSubjects;

        private CertificateAdapter(KeyStore keyStore, Context context, List<String> list) {
            this.mSubjects = new ArrayList();
            this.mAliases = list;
            this.mSubjects.addAll(Collections.nCopies(list.size(), (String) null));
            this.mKeyStore = keyStore;
            this.mContext = context;
        }

        @Override
        public int getCount() {
            return this.mAliases.size();
        }

        @Override
        public String getItem(int i) {
            return this.mAliases.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            if (view == null) {
                view = LayoutInflater.from(this.mContext).inflate(R.layout.cert_item, viewGroup, false);
                viewHolder = new ViewHolder();
                viewHolder.mAliasTextView = (TextView) view.findViewById(R.id.cert_item_alias);
                viewHolder.mSubjectTextView = (TextView) view.findViewById(R.id.cert_item_subject);
                viewHolder.mRadioButton = (RadioButton) view.findViewById(R.id.cert_item_selected);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }
            viewHolder.mAliasTextView.setText(this.mAliases.get(i));
            String str = this.mSubjects.get(i);
            if (str == null) {
                new CertLoader(i, viewHolder.mSubjectTextView).execute(new Void[0]);
            } else {
                viewHolder.mSubjectTextView.setText(str);
            }
            viewHolder.mRadioButton.setChecked(i == ((ListView) viewGroup).getCheckedItemPosition() - 1);
            return view;
        }

        private class CertLoader extends AsyncTask<Void, Void, String> {
            private final int mAdapterPosition;
            private final TextView mSubjectView;

            private CertLoader(int i, TextView textView) {
                this.mAdapterPosition = i;
                this.mSubjectView = textView;
            }

            @Override
            protected String doInBackground(Void... voidArr) {
                String str = (String) CertificateAdapter.this.mAliases.get(this.mAdapterPosition);
                byte[] bArr = CertificateAdapter.this.mKeyStore.get("USRCERT_" + str);
                if (bArr == null) {
                    return null;
                }
                try {
                    return X509Name.getInstance(((X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(bArr))).getSubjectX500Principal().getEncoded()).toString(true, X509Name.DefaultSymbols);
                } catch (CertificateException e) {
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String str) {
                CertificateAdapter.this.mSubjects.set(this.mAdapterPosition, str);
                this.mSubjectView.setText(str);
            }
        }
    }

    private static class ViewHolder {
        TextView mAliasTextView;
        RadioButton mRadioButton;
        TextView mSubjectTextView;

        private ViewHolder() {
        }
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) {
        if (i == 1) {
            if (this.mKeyStore.isUnlocked()) {
                this.mState = State.INITIAL;
                chooseCertificate();
                return;
            } else {
                this.mState = State.UNLOCK_CANCELED;
                return;
            }
        }
        throw new AssertionError();
    }

    private void finish(String str) {
        finish(str, false);
    }

    private void finishWithAliasFromPolicy(String str) {
        finish(str, true);
    }

    private void finish(String str, boolean z) {
        if (str == null) {
            setResult(0);
        } else {
            Intent intent = new Intent();
            intent.putExtra("android.intent.extra.TEXT", str);
            setResult(-1, intent);
        }
        IKeyChainAliasCallback iKeyChainAliasCallbackAsInterface = IKeyChainAliasCallback.Stub.asInterface(getIntent().getIBinderExtra("response"));
        if (iKeyChainAliasCallbackAsInterface != null) {
            new ResponseSender(iKeyChainAliasCallbackAsInterface, str, z).execute(new Void[0]);
        } else {
            finish();
        }
    }

    private class ResponseSender extends AsyncTask<Void, Void, Void> {
        private String mAlias;
        private boolean mFromPolicy;
        private IKeyChainAliasCallback mKeyChainAliasResponse;

        private ResponseSender(IKeyChainAliasCallback iKeyChainAliasCallback, String str, boolean z) {
            this.mKeyChainAliasResponse = iKeyChainAliasCallback;
            this.mAlias = str;
            this.mFromPolicy = z;
        }

        @Override
        protected Void doInBackground(Void... voidArr) {
            try {
                if (this.mAlias != null) {
                    KeyChain.KeyChainConnection keyChainConnectionBind = KeyChain.bind(KeyChainActivity.this);
                    try {
                        if (this.mFromPolicy || keyChainConnectionBind.getService().isUserSelectable(this.mAlias)) {
                            keyChainConnectionBind.getService().setGrant(KeyChainActivity.this.mSenderUid, this.mAlias, true);
                            keyChainConnectionBind.close();
                        } else {
                            Log.w("KeyChain", String.format("Alias %s not user-selectable.", this.mAlias));
                            return null;
                        }
                    } finally {
                        keyChainConnectionBind.close();
                    }
                }
                this.mKeyChainAliasResponse.alias(this.mAlias);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.d("KeyChain", "interrupted while granting access", e);
            } catch (Exception e2) {
                Log.e("KeyChain", "error while granting access", e2);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void r1) {
            KeyChainActivity.this.finish();
        }
    }

    @Override
    public void onBackPressed() {
        finish(null);
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        if (this.mState != State.INITIAL) {
            bundle.putSerializable(KEY_STATE, this.mState);
        }
    }
}
