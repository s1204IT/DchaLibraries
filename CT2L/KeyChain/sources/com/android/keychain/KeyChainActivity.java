package com.android.keychain;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.BenesseExtension;
import android.os.Bundle;
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
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import com.android.org.bouncycastle.asn1.x509.X509Name;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.security.auth.x500.X500Principal;

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
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        if (savedState == null) {
            this.mState = State.INITIAL;
            return;
        }
        this.mState = (State) savedState.getSerializable(KEY_STATE);
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
            switch (this.mState) {
                case INITIAL:
                    if (!this.mKeyStore.isUnlocked()) {
                        if (BenesseExtension.getDchaState() == 0) {
                            this.mState = State.UNLOCK_REQUESTED;
                            startActivityForResult(new Intent("com.android.credentials.UNLOCK"), 1);
                            return;
                        }
                        return;
                    }
                    showCertChooserDialog();
                    return;
                case UNLOCK_REQUESTED:
                    return;
                case UNLOCK_CANCELED:
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

    private void showCertChooserDialog() {
        new AliasLoader().execute(new Void[0]);
    }

    private class AliasLoader extends AsyncTask<Void, Void, CertificateAdapter> {
        private AliasLoader() {
        }

        @Override
        protected CertificateAdapter doInBackground(Void... params) {
            String[] aliasArray = KeyChainActivity.this.mKeyStore.saw("USRPKEY_");
            List<String> aliasList = aliasArray == null ? Collections.emptyList() : Arrays.asList(aliasArray);
            Collections.sort(aliasList);
            return new CertificateAdapter(aliasList);
        }

        @Override
        protected void onPostExecute(CertificateAdapter adapter) {
            KeyChainActivity.this.displayCertChooserDialog(adapter);
        }
    }

    private void displayCertChooserDialog(final CertificateAdapter adapter) {
        String title;
        CharSequence applicationLabel;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        TextView contextView = (TextView) View.inflate(this, R.layout.cert_chooser_header, null);
        View footer = View.inflate(this, R.layout.cert_chooser_footer, null);
        final ListView lv = (ListView) View.inflate(this, R.layout.cert_chooser, null);
        lv.addHeaderView(contextView, null, false);
        lv.addFooterView(footer, null, false);
        lv.setAdapter((ListAdapter) adapter);
        builder.setView(lv);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                lv.setItemChecked(position, true);
            }
        });
        boolean empty = adapter.mAliases.isEmpty();
        int negativeLabel = empty ? android.R.string.cancel : R.string.deny_button;
        builder.setNegativeButton(negativeLabel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        Resources res = getResources();
        if (empty) {
            title = res.getString(R.string.title_no_certs);
        } else {
            title = res.getString(R.string.title_select_cert);
            String alias = getIntent().getStringExtra("alias");
            if (alias != null) {
                int adapterPosition = adapter.mAliases.indexOf(alias);
                if (adapterPosition != -1) {
                    int listViewPosition = adapterPosition + 1;
                    lv.setItemChecked(listViewPosition, true);
                }
            } else if (adapter.mAliases.size() == 1) {
                int listViewPosition2 = 0 + 1;
                lv.setItemChecked(listViewPosition2, true);
            }
            builder.setPositiveButton(R.string.allow_button, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    int listViewPosition3 = lv.getCheckedItemPosition();
                    int adapterPosition2 = listViewPosition3 - 1;
                    String alias2 = adapterPosition2 >= 0 ? adapter.getItem(adapterPosition2) : null;
                    KeyChainActivity.this.finish(alias2);
                }
            });
        }
        builder.setTitle(title);
        final AlertDialog dialog = builder.create();
        String pkg = this.mSender.getIntentSender().getTargetPackage();
        PackageManager pm = getPackageManager();
        try {
            applicationLabel = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            applicationLabel = pkg;
        }
        String appMessage = String.format(res.getString(R.string.requesting_application), applicationLabel);
        String contextMessage = appMessage;
        String host = getIntent().getStringExtra("host");
        if (host != null) {
            String hostString = host;
            int port = getIntent().getIntExtra("port", -1);
            if (port != -1) {
                hostString = hostString + ":" + port;
            }
            String hostMessage = String.format(res.getString(R.string.requesting_server), hostString);
            if (contextMessage == null) {
                contextMessage = hostMessage;
            } else {
                contextMessage = contextMessage + " " + hostMessage;
            }
        }
        contextView.setText(contextMessage);
        String installMessage = String.format(res.getString(R.string.install_new_cert_message), ".pfx", ".p12");
        TextView installText = (TextView) footer.findViewById(R.id.cert_chooser_install_message);
        installText.setText(installMessage);
        Button installButton = (Button) footer.findViewById(R.id.cert_chooser_install_button);
        installButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                Credentials.getInstance().install(KeyChainActivity.this);
            }
        });
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog2) {
                KeyChainActivity.this.finish(null);
            }
        });
        dialog.show();
    }

    private class CertificateAdapter extends BaseAdapter {
        private final List<String> mAliases;
        private final List<String> mSubjects;

        private CertificateAdapter(List<String> aliases) {
            this.mSubjects = new ArrayList();
            this.mAliases = aliases;
            this.mSubjects.addAll(Collections.nCopies(aliases.size(), (String) null));
        }

        @Override
        public int getCount() {
            return this.mAliases.size();
        }

        @Override
        public String getItem(int adapterPosition) {
            return this.mAliases.get(adapterPosition);
        }

        @Override
        public long getItemId(int adapterPosition) {
            return adapterPosition;
        }

        @Override
        public View getView(int adapterPosition, View view, ViewGroup parent) {
            ViewHolder holder;
            if (view == null) {
                LayoutInflater inflater = LayoutInflater.from(KeyChainActivity.this);
                view = inflater.inflate(R.layout.cert_item, parent, false);
                holder = new ViewHolder();
                holder.mAliasTextView = (TextView) view.findViewById(R.id.cert_item_alias);
                holder.mSubjectTextView = (TextView) view.findViewById(R.id.cert_item_subject);
                holder.mRadioButton = (RadioButton) view.findViewById(R.id.cert_item_selected);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }
            String alias = this.mAliases.get(adapterPosition);
            holder.mAliasTextView.setText(alias);
            String subject = this.mSubjects.get(adapterPosition);
            if (subject == null) {
                new CertLoader(adapterPosition, holder.mSubjectTextView).execute(new Void[0]);
            } else {
                holder.mSubjectTextView.setText(subject);
            }
            ListView lv = (ListView) parent;
            int listViewCheckedItemPosition = lv.getCheckedItemPosition();
            int adapterCheckedItemPosition = listViewCheckedItemPosition - 1;
            holder.mRadioButton.setChecked(adapterPosition == adapterCheckedItemPosition);
            return view;
        }

        private class CertLoader extends AsyncTask<Void, Void, String> {
            private final int mAdapterPosition;
            private final TextView mSubjectView;

            private CertLoader(int adapterPosition, TextView subjectView) {
                this.mAdapterPosition = adapterPosition;
                this.mSubjectView = subjectView;
            }

            @Override
            protected String doInBackground(Void... params) {
                String alias = (String) CertificateAdapter.this.mAliases.get(this.mAdapterPosition);
                byte[] bytes = KeyChainActivity.this.mKeyStore.get("USRCERT_" + alias);
                if (bytes == null) {
                    return null;
                }
                InputStream in = new ByteArrayInputStream(bytes);
                try {
                    CertificateFactory cf = CertificateFactory.getInstance("X.509");
                    X509Certificate cert = (X509Certificate) cf.generateCertificate(in);
                    X500Principal subjectPrincipal = cert.getSubjectX500Principal();
                    X509Name subjectName = X509Name.getInstance(subjectPrincipal.getEncoded());
                    return subjectName.toString(true, X509Name.DefaultSymbols);
                } catch (CertificateException e) {
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String subjectString) {
                CertificateAdapter.this.mSubjects.set(this.mAdapterPosition, subjectString);
                this.mSubjectView.setText(subjectString);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 1:
                if (this.mKeyStore.isUnlocked()) {
                    this.mState = State.INITIAL;
                    showCertChooserDialog();
                    return;
                } else {
                    this.mState = State.UNLOCK_CANCELED;
                    return;
                }
            default:
                throw new AssertionError();
        }
    }

    private void finish(String alias) {
        if (alias == null) {
            setResult(0);
        } else {
            Intent result = new Intent();
            result.putExtra("android.intent.extra.TEXT", alias);
            setResult(-1, result);
        }
        IKeyChainAliasCallback keyChainAliasResponse = IKeyChainAliasCallback.Stub.asInterface(getIntent().getIBinderExtra("response"));
        if (keyChainAliasResponse != null) {
            new ResponseSender(keyChainAliasResponse, alias).execute(new Void[0]);
        } else {
            finish();
        }
    }

    private class ResponseSender extends AsyncTask<Void, Void, Void> {
        private String mAlias;
        private IKeyChainAliasCallback mKeyChainAliasResponse;

        private ResponseSender(IKeyChainAliasCallback keyChainAliasResponse, String alias) {
            this.mKeyChainAliasResponse = keyChainAliasResponse;
            this.mAlias = alias;
        }

        @Override
        protected Void doInBackground(Void... unused) {
            try {
                if (this.mAlias != null) {
                    KeyChain.KeyChainConnection connection = KeyChain.bind(KeyChainActivity.this);
                    try {
                        connection.getService().setGrant(KeyChainActivity.this.mSenderUid, this.mAlias, true);
                    } finally {
                        connection.close();
                    }
                }
                this.mKeyChainAliasResponse.alias(this.mAlias);
                return null;
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                Log.d("KeyChain", "interrupted while granting access", ignored);
                return null;
            } catch (Exception ignored2) {
                Log.e("KeyChain", "error while granting access", ignored2);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Void unused) {
            KeyChainActivity.this.finish();
        }
    }

    @Override
    public void onBackPressed() {
        finish(null);
    }

    @Override
    protected void onSaveInstanceState(Bundle savedState) {
        super.onSaveInstanceState(savedState);
        if (this.mState != State.INITIAL) {
            savedState.putSerializable(KEY_STATE, this.mState);
        }
    }
}
