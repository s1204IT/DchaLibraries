package com.android.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.UserInfo;
import android.content.res.TypedArray;
import android.net.http.SslCertificate;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.security.IKeyChainService;
import android.security.KeyChain;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Switch;
import android.widget.TabHost;
import android.widget.TextView;
import com.android.internal.util.ParcelableString;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class TrustedCredentialsSettings extends Fragment {
    private AliasOperation mAliasOperation;
    private TabHost mTabHost;
    private UserManager mUserManager;
    private HashMap<Tab, AdapterData.AliasLoader> mAliasLoaders = new HashMap<>(2);
    private final SparseArray<KeyChain.KeyChainConnection> mKeyChainConnectionByProfileId = new SparseArray<>();

    private interface TrustedCertificateAdapterCommons {
        int getListViewId(Tab tab);

        void load();

        void notifyDataSetChanged();

        void remove(CertHolder certHolder);
    }

    private enum Tab {
        SYSTEM("system", R.string.trusted_credentials_system_tab, R.id.system_tab, R.id.system_progress, R.id.system_list, R.id.system_expandable_list, true),
        USER("user", R.string.trusted_credentials_user_tab, R.id.user_tab, R.id.user_progress, R.id.user_list, R.id.user_expandable_list, false);

        private final int mExpandableList;
        private final int mLabel;
        private final int mList;
        private final int mProgress;
        private final boolean mSwitch;
        private final String mTag;
        private final int mView;

        Tab(String tag, int label, int view, int progress, int list, int expandableList, boolean withSwitch) {
            this.mTag = tag;
            this.mLabel = label;
            this.mView = view;
            this.mProgress = progress;
            this.mList = list;
            this.mExpandableList = expandableList;
            this.mSwitch = withSwitch;
        }

        public List<ParcelableString> getAliases(IKeyChainService service) throws RemoteException {
            switch (this) {
                case SYSTEM:
                    return service.getSystemCaAliases().getList();
                case USER:
                    return service.getUserCaAliases().getList();
                default:
                    throw new AssertionError();
            }
        }

        public boolean deleted(IKeyChainService service, String alias) throws RemoteException {
            switch (this) {
                case SYSTEM:
                    return !service.containsCaAlias(alias);
                case USER:
                    return false;
                default:
                    throw new AssertionError();
            }
        }

        public int getButtonLabel(CertHolder certHolder) {
            switch (this) {
                case SYSTEM:
                    if (certHolder.mDeleted) {
                        return R.string.trusted_credentials_enable_label;
                    }
                    return R.string.trusted_credentials_disable_label;
                case USER:
                    return R.string.trusted_credentials_remove_label;
                default:
                    throw new AssertionError();
            }
        }

        public int getButtonConfirmation(CertHolder certHolder) {
            switch (this) {
                case SYSTEM:
                    if (certHolder.mDeleted) {
                        return R.string.trusted_credentials_enable_confirmation;
                    }
                    return R.string.trusted_credentials_disable_confirmation;
                case USER:
                    return R.string.trusted_credentials_remove_confirmation;
                default:
                    throw new AssertionError();
            }
        }

        public void postOperationUpdate(boolean ok, CertHolder certHolder) {
            if (ok) {
                if (certHolder.mTab.mSwitch) {
                    certHolder.mDeleted = !certHolder.mDeleted;
                } else {
                    certHolder.mAdapter.remove(certHolder);
                }
                certHolder.mAdapter.notifyDataSetChanged();
                return;
            }
            certHolder.mAdapter.load();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mUserManager = (UserManager) getActivity().getSystemService("user");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        this.mTabHost = (TabHost) inflater.inflate(R.layout.trusted_credentials, parent, false);
        this.mTabHost.setup();
        addTab(Tab.SYSTEM);
        addTab(Tab.USER);
        if (getActivity().getIntent() != null && "com.android.settings.TRUSTED_CREDENTIALS_USER".equals(getActivity().getIntent().getAction())) {
            this.mTabHost.setCurrentTabByTag(Tab.USER.mTag);
        }
        return this.mTabHost;
    }

    @Override
    public void onDestroy() {
        for (AdapterData.AliasLoader aliasLoader : this.mAliasLoaders.values()) {
            aliasLoader.cancel(true);
        }
        if (this.mAliasOperation != null) {
            this.mAliasOperation.cancel(true);
            this.mAliasOperation = null;
        }
        closeKeyChainConnections();
        super.onDestroy();
    }

    private void closeKeyChainConnections() {
        int n = this.mKeyChainConnectionByProfileId.size();
        for (int i = 0; i < n; i++) {
            this.mKeyChainConnectionByProfileId.valueAt(i).close();
        }
        this.mKeyChainConnectionByProfileId.clear();
    }

    private void addTab(Tab tab) {
        TabHost.TabSpec systemSpec = this.mTabHost.newTabSpec(tab.mTag).setIndicator(getActivity().getString(tab.mLabel)).setContent(tab.mView);
        this.mTabHost.addTab(systemSpec);
        if (this.mUserManager.getUserProfiles().size() > 1) {
            ExpandableListView lv = (ExpandableListView) this.mTabHost.findViewById(tab.mExpandableList);
            final TrustedCertificateExpandableAdapter adapter = new TrustedCertificateExpandableAdapter(tab);
            lv.setAdapter(adapter);
            lv.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                    TrustedCredentialsSettings.this.showCertDialog(adapter.getChild(groupPosition, childPosition));
                    return true;
                }
            });
            return;
        }
        ListView lv2 = (ListView) this.mTabHost.findViewById(tab.mList);
        final TrustedCertificateAdapter adapter2 = new TrustedCertificateAdapter(tab);
        lv2.setAdapter((ListAdapter) adapter2);
        lv2.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                TrustedCredentialsSettings.this.showCertDialog(adapter2.getItem(pos));
            }
        });
    }

    private class TrustedCertificateExpandableAdapter extends BaseExpandableListAdapter implements TrustedCertificateAdapterCommons {
        private AdapterData mData;

        private TrustedCertificateExpandableAdapter(Tab tab) {
            this.mData = new AdapterData(tab, this);
            load();
        }

        @Override
        public void remove(CertHolder certHolder) {
            this.mData.remove(certHolder);
        }

        @Override
        public int getGroupCount() {
            return this.mData.mCertHoldersByUserId.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            List<CertHolder> certHolders = (List) this.mData.mCertHoldersByUserId.valueAt(groupPosition);
            if (certHolders != null) {
                return certHolders.size();
            }
            return 0;
        }

        @Override
        public UserHandle getGroup(int groupPosition) {
            return new UserHandle(this.mData.mCertHoldersByUserId.keyAt(groupPosition));
        }

        @Override
        public CertHolder getChild(int groupPosition, int childPosition) {
            return (CertHolder) ((List) this.mData.mCertHoldersByUserId.valueAt(groupPosition)).get(childPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return this.mData.mCertHoldersByUserId.keyAt(groupPosition);
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) TrustedCredentialsSettings.this.getActivity().getSystemService("layout_inflater");
                convertView = inflateCategoryHeader(inflater, parent);
            }
            TextView title = (TextView) convertView.findViewById(android.R.id.title);
            UserHandle profile = getGroup(groupPosition);
            UserInfo userInfo = TrustedCredentialsSettings.this.mUserManager.getUserInfo(profile.getIdentifier());
            if (userInfo.isManagedProfile()) {
                title.setText(R.string.category_work);
            } else {
                title.setText(R.string.category_personal);
            }
            title.setTextAlignment(6);
            return convertView;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            return TrustedCredentialsSettings.this.getViewForCertificate(getChild(groupPosition, childPosition), this.mData.mTab, convertView, parent);
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        @Override
        public void load() {
            AdapterData adapterData = this.mData;
            adapterData.getClass();
            adapterData.new AliasLoader().execute(new Void[0]);
        }

        @Override
        public int getListViewId(Tab tab) {
            return tab.mExpandableList;
        }

        private View inflateCategoryHeader(LayoutInflater inflater, ViewGroup parent) {
            TypedArray a = inflater.getContext().obtainStyledAttributes(null, com.android.internal.R.styleable.Preference, android.R.attr.preferenceCategoryStyle, 0);
            int resId = a.getResourceId(3, 0);
            return inflater.inflate(resId, parent, false);
        }
    }

    private class TrustedCertificateAdapter extends BaseAdapter implements TrustedCertificateAdapterCommons {
        private final AdapterData mData;

        private TrustedCertificateAdapter(Tab tab) {
            this.mData = new AdapterData(tab, this);
            load();
        }

        @Override
        public void remove(CertHolder certHolder) {
            this.mData.remove(certHolder);
        }

        @Override
        public int getListViewId(Tab tab) {
            return tab.mList;
        }

        @Override
        public void load() {
            AdapterData adapterData = this.mData;
            adapterData.getClass();
            adapterData.new AliasLoader().execute(new Void[0]);
        }

        @Override
        public int getCount() {
            List<CertHolder> certHolders = (List) this.mData.mCertHoldersByUserId.valueAt(0);
            if (certHolders != null) {
                return certHolders.size();
            }
            return 0;
        }

        @Override
        public CertHolder getItem(int position) {
            return (CertHolder) ((List) this.mData.mCertHoldersByUserId.valueAt(0)).get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            return TrustedCredentialsSettings.this.getViewForCertificate(getItem(position), this.mData.mTab, view, parent);
        }
    }

    private class AdapterData {
        private final TrustedCertificateAdapterCommons mAdapter;
        private final SparseArray<List<CertHolder>> mCertHoldersByUserId;
        private final Tab mTab;

        private AdapterData(Tab tab, TrustedCertificateAdapterCommons adapter) {
            this.mCertHoldersByUserId = new SparseArray<>();
            this.mAdapter = adapter;
            this.mTab = tab;
        }

        private class AliasLoader extends AsyncTask<Void, Integer, SparseArray<List<CertHolder>>> {
            private Context mContext;
            private View mList;
            private ProgressBar mProgressBar;

            public AliasLoader() {
                this.mContext = TrustedCredentialsSettings.this.getActivity();
                TrustedCredentialsSettings.this.mAliasLoaders.put(AdapterData.this.mTab, this);
            }

            @Override
            protected void onPreExecute() {
                View content = TrustedCredentialsSettings.this.mTabHost.getTabContentView();
                this.mProgressBar = (ProgressBar) content.findViewById(AdapterData.this.mTab.mProgress);
                this.mList = content.findViewById(AdapterData.this.mAdapter.getListViewId(AdapterData.this.mTab));
                this.mProgressBar.setVisibility(0);
                this.mList.setVisibility(8);
            }

            @Override
            public SparseArray<List<CertHolder>> doInBackground(Void... params) {
                SparseArray<List<CertHolder>> certHoldersByProfile = new SparseArray<>();
                try {
                    List<UserHandle> profiles = TrustedCredentialsSettings.this.mUserManager.getUserProfiles();
                    int n = profiles.size();
                    SparseArray<List<ParcelableString>> aliasesByProfileId = new SparseArray<>(n);
                    int max = 0;
                    int progress = 0;
                    for (int i = 0; i < n; i++) {
                        UserHandle profile = profiles.get(i);
                        int profileId = profile.getIdentifier();
                        KeyChain.KeyChainConnection keyChainConnection = KeyChain.bindAsUser(this.mContext, profile);
                        TrustedCredentialsSettings.this.mKeyChainConnectionByProfileId.put(profileId, keyChainConnection);
                        List<ParcelableString> aliases = AdapterData.this.mTab.getAliases(keyChainConnection.getService());
                        if (isCancelled()) {
                            return new SparseArray<>();
                        }
                        max += aliases.size();
                        aliasesByProfileId.put(profileId, aliases);
                    }
                    for (int i2 = 0; i2 < n; i2++) {
                        int profileId2 = profiles.get(i2).getIdentifier();
                        List<ParcelableString> aliases2 = aliasesByProfileId.get(profileId2);
                        if (!isCancelled()) {
                            IKeyChainService service = ((KeyChain.KeyChainConnection) TrustedCredentialsSettings.this.mKeyChainConnectionByProfileId.get(profileId2)).getService();
                            List<CertHolder> certHolders = new ArrayList<>(max);
                            int aliasMax = aliases2.size();
                            for (int j = 0; j < aliasMax; j++) {
                                String alias = aliases2.get(j).string;
                                byte[] encodedCertificate = service.getEncodedCaCertificate(alias, true);
                                X509Certificate cert = KeyChain.toCertificate(encodedCertificate);
                                certHolders.add(new CertHolder(service, AdapterData.this.mAdapter, AdapterData.this.mTab, alias, cert, profileId2));
                                progress++;
                                publishProgress(Integer.valueOf(progress), Integer.valueOf(max));
                            }
                            Collections.sort(certHolders);
                            certHoldersByProfile.put(profileId2, certHolders);
                        } else {
                            return new SparseArray<>();
                        }
                    }
                    return certHoldersByProfile;
                } catch (RemoteException e) {
                    Log.e("TrustedCredentialsSettings", "Remote exception while loading aliases.", e);
                    return new SparseArray<>();
                } catch (InterruptedException e2) {
                    Log.e("TrustedCredentialsSettings", "InterruptedException while loading aliases.", e2);
                    return new SparseArray<>();
                }
            }

            @Override
            public void onProgressUpdate(Integer... progressAndMax) {
                int progress = progressAndMax[0].intValue();
                int max = progressAndMax[1].intValue();
                if (max != this.mProgressBar.getMax()) {
                    this.mProgressBar.setMax(max);
                }
                this.mProgressBar.setProgress(progress);
            }

            @Override
            public void onPostExecute(SparseArray<List<CertHolder>> certHolders) {
                AdapterData.this.mCertHoldersByUserId.clear();
                int n = certHolders.size();
                for (int i = 0; i < n; i++) {
                    AdapterData.this.mCertHoldersByUserId.put(certHolders.keyAt(i), certHolders.valueAt(i));
                }
                AdapterData.this.mAdapter.notifyDataSetChanged();
                this.mProgressBar.setVisibility(8);
                this.mList.setVisibility(0);
                this.mProgressBar.setProgress(0);
                TrustedCredentialsSettings.this.mAliasLoaders.remove(AdapterData.this.mTab);
            }
        }

        public void remove(CertHolder certHolder) {
            List<CertHolder> certs;
            if (this.mCertHoldersByUserId != null && (certs = this.mCertHoldersByUserId.get(certHolder.mProfileId)) != null) {
                certs.remove(certHolder);
            }
        }
    }

    private static class CertHolder implements Comparable<CertHolder> {
        private final TrustedCertificateAdapterCommons mAdapter;
        private final String mAlias;
        private boolean mDeleted;
        public int mProfileId;
        private final IKeyChainService mService;
        private final SslCertificate mSslCert;
        private final String mSubjectPrimary;
        private final String mSubjectSecondary;
        private final Tab mTab;
        private final X509Certificate mX509Cert;

        private CertHolder(IKeyChainService service, TrustedCertificateAdapterCommons adapter, Tab tab, String alias, X509Certificate x509Cert, int profileId) {
            this.mProfileId = profileId;
            this.mService = service;
            this.mAdapter = adapter;
            this.mTab = tab;
            this.mAlias = alias;
            this.mX509Cert = x509Cert;
            this.mSslCert = new SslCertificate(x509Cert);
            String cn = this.mSslCert.getIssuedTo().getCName();
            String o = this.mSslCert.getIssuedTo().getOName();
            String ou = this.mSslCert.getIssuedTo().getUName();
            if (!o.isEmpty()) {
                if (!cn.isEmpty()) {
                    this.mSubjectPrimary = o;
                    this.mSubjectSecondary = cn;
                } else {
                    this.mSubjectPrimary = o;
                    this.mSubjectSecondary = ou;
                }
            } else if (!cn.isEmpty()) {
                this.mSubjectPrimary = cn;
                this.mSubjectSecondary = "";
            } else {
                this.mSubjectPrimary = this.mSslCert.getIssuedTo().getDName();
                this.mSubjectSecondary = "";
            }
            try {
                this.mDeleted = this.mTab.deleted(this.mService, this.mAlias);
            } catch (RemoteException e) {
                Log.e("TrustedCredentialsSettings", "Remote exception while checking if alias " + this.mAlias + " is deleted.", e);
                this.mDeleted = false;
            }
        }

        @Override
        public int compareTo(CertHolder o) {
            int primary = this.mSubjectPrimary.compareToIgnoreCase(o.mSubjectPrimary);
            return primary != 0 ? primary : this.mSubjectSecondary.compareToIgnoreCase(o.mSubjectSecondary);
        }

        public boolean equals(Object o) {
            if (!(o instanceof CertHolder)) {
                return false;
            }
            CertHolder other = (CertHolder) o;
            return this.mAlias.equals(other.mAlias);
        }

        public int hashCode() {
            return this.mAlias.hashCode();
        }
    }

    public View getViewForCertificate(CertHolder certHolder, Tab mTab, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            convertView = inflater.inflate(R.layout.trusted_credential, parent, false);
            holder = new ViewHolder();
            holder.mSubjectPrimaryView = (TextView) convertView.findViewById(R.id.trusted_credential_subject_primary);
            holder.mSubjectSecondaryView = (TextView) convertView.findViewById(R.id.trusted_credential_subject_secondary);
            holder.mSwitch = (Switch) convertView.findViewById(R.id.trusted_credential_status);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.mSubjectPrimaryView.setText(certHolder.mSubjectPrimary);
        holder.mSubjectSecondaryView.setText(certHolder.mSubjectSecondary);
        if (mTab.mSwitch) {
            holder.mSwitch.setChecked(!certHolder.mDeleted);
            holder.mSwitch.setEnabled(this.mUserManager.hasUserRestriction("no_config_credentials", new UserHandle(certHolder.mProfileId)) ? false : true);
            holder.mSwitch.setVisibility(0);
        }
        return convertView;
    }

    private static class ViewHolder {
        private TextView mSubjectPrimaryView;
        private TextView mSubjectSecondaryView;
        private Switch mSwitch;

        private ViewHolder() {
        }
    }

    public void showCertDialog(final CertHolder certHolder) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(android.R.string.lockscreen_sim_locked_message);
        final ArrayList<View> views = new ArrayList<>();
        ArrayList<String> titles = new ArrayList<>();
        addCertChain(certHolder, views, titles);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, titles);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner spinner = new Spinner(getActivity());
        spinner.setAdapter((SpinnerAdapter) arrayAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int i = 0;
                while (i < views.size()) {
                    ((View) views.get(i)).setVisibility(i == position ? 0 : 8);
                    i++;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        LinearLayout container = new LinearLayout(getActivity());
        container.setOrientation(1);
        container.addView(spinner);
        for (int i = 0; i < views.size(); i++) {
            View certificateView = views.get(i);
            if (i != 0) {
                certificateView.setVisibility(8);
            }
            container.addView(certificateView);
        }
        builder.setView(container);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        final Dialog certDialog = builder.create();
        ViewGroup body = (ViewGroup) container.findViewById(android.R.id.home_screen);
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        Button removeButton = (Button) inflater.inflate(R.layout.trusted_credential_details, body, false);
        if (!this.mUserManager.hasUserRestriction("no_config_credentials", new UserHandle(certHolder.mProfileId))) {
            body.addView(removeButton);
        }
        removeButton.setText(certHolder.mTab.getButtonLabel(certHolder));
        removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder2 = new AlertDialog.Builder(TrustedCredentialsSettings.this.getActivity());
                builder2.setMessage(certHolder.mTab.getButtonConfirmation(certHolder));
                builder2.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        new AliasOperation(certHolder).execute(new Void[0]);
                        dialog.dismiss();
                        certDialog.dismiss();
                    }
                });
                builder2.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
                AlertDialog alert = builder2.create();
                alert.show();
            }
        });
        certDialog.show();
    }

    private void addCertChain(CertHolder certHolder, ArrayList<View> views, ArrayList<String> titles) {
        try {
            KeyChain.KeyChainConnection keyChainConnection = this.mKeyChainConnectionByProfileId.get(certHolder.mProfileId);
            IKeyChainService service = keyChainConnection.getService();
            List<String> chain = service.getCaCertificateChainAliases(certHolder.mAlias, true);
            int n = chain.size();
            List<X509Certificate> certificates = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                try {
                    byte[] encodedCertificate = service.getEncodedCaCertificate(chain.get(i), true);
                    X509Certificate certificate = KeyChain.toCertificate(encodedCertificate);
                    certificates.add(certificate);
                } catch (RemoteException e) {
                    ex = e;
                    Log.e("TrustedCredentialsSettings", "RemoteException while retrieving certificate chain for root " + certHolder.mAlias, ex);
                    return;
                }
            }
            for (X509Certificate certificate2 : certificates) {
                addCertDetails(certificate2, views, titles);
            }
        } catch (RemoteException e2) {
            ex = e2;
        }
    }

    private void addCertDetails(X509Certificate certificate, ArrayList<View> views, ArrayList<String> titles) {
        SslCertificate sslCert = new SslCertificate(certificate);
        views.add(sslCert.inflateCertificateView(getActivity()));
        titles.add(sslCert.getIssuedTo().getCName());
    }

    private class AliasOperation extends AsyncTask<Void, Void, Boolean> {
        private final CertHolder mCertHolder;

        private AliasOperation(CertHolder certHolder) {
            this.mCertHolder = certHolder;
            TrustedCredentialsSettings.this.mAliasOperation = this;
        }

        @Override
        public Boolean doInBackground(Void... params) {
            boolean zValueOf;
            try {
                KeyChain.KeyChainConnection keyChainConnection = (KeyChain.KeyChainConnection) TrustedCredentialsSettings.this.mKeyChainConnectionByProfileId.get(this.mCertHolder.mProfileId);
                IKeyChainService service = keyChainConnection.getService();
                if (this.mCertHolder.mDeleted) {
                    byte[] bytes = this.mCertHolder.mX509Cert.getEncoded();
                    service.installCaCertificate(bytes);
                    zValueOf = true;
                } else {
                    zValueOf = Boolean.valueOf(service.deleteCaCertificate(this.mCertHolder.mAlias));
                }
                return zValueOf;
            } catch (RemoteException | IllegalStateException | SecurityException | CertificateEncodingException e) {
                Log.w("TrustedCredentialsSettings", "Error while toggling alias " + this.mCertHolder.mAlias, e);
                return false;
            }
        }

        @Override
        public void onPostExecute(Boolean ok) {
            this.mCertHolder.mTab.postOperationUpdate(ok.booleanValue(), this.mCertHolder);
            TrustedCredentialsSettings.this.mAliasOperation = null;
        }
    }
}
