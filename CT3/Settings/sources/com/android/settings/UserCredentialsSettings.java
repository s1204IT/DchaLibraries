package com.android.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.UserHandle;
import android.security.IKeyChainService;
import android.security.KeyChain;
import android.security.KeyStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.android.settingslib.RestrictedLockUtils;
import java.util.EnumSet;
import java.util.SortedMap;
import java.util.TreeMap;

public class UserCredentialsSettings extends OptionsMenuFragment implements AdapterView.OnItemClickListener {
    private ListView mListView;
    private View mRootView;

    @Override
    protected int getMetricsCategory() {
        return 285;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshItems();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        this.mRootView = inflater.inflate(R.layout.user_credentials, parent, false);
        this.mListView = (ListView) this.mRootView.findViewById(R.id.credential_list);
        this.mListView.setOnItemClickListener(this);
        return this.mRootView;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Credential item = (Credential) parent.getItemAtPosition(position);
        CredentialDialogFragment.show(this, item);
    }

    protected void refreshItems() {
        if (!isAdded()) {
            return;
        }
        new AliasLoader(this, null).execute(new Void[0]);
    }

    public static class CredentialDialogFragment extends DialogFragment {
        public static void show(Fragment target, Credential item) {
            Bundle args = new Bundle();
            args.putParcelable("credential", item);
            if (target.getFragmentManager().findFragmentByTag("CredentialDialogFragment") != null) {
                return;
            }
            DialogFragment frag = new CredentialDialogFragment();
            frag.setTargetFragment(target, -1);
            frag.setArguments(args);
            frag.show(target.getFragmentManager(), "CredentialDialogFragment");
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Credential item = (Credential) getArguments().getParcelable("credential");
            View root = getActivity().getLayoutInflater().inflate(R.layout.user_credential_dialog, (ViewGroup) null);
            ViewGroup infoContainer = (ViewGroup) root.findViewById(R.id.credential_container);
            View view = new CredentialAdapter(getActivity(), R.layout.user_credential, new Credential[]{item}).getView(0, null, null);
            infoContainer.addView(view);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setView(root).setTitle(R.string.user_credential_title).setPositiveButton(R.string.done, (DialogInterface.OnClickListener) null);
            final int myUserId = UserHandle.myUserId();
            if (!RestrictedLockUtils.hasBaseUserRestriction(getContext(), "no_config_credentials", myUserId)) {
                DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        RestrictedLockUtils.EnforcedAdmin admin = RestrictedLockUtils.checkIfRestrictionEnforced(CredentialDialogFragment.this.getContext(), "no_config_credentials", myUserId);
                        if (admin != null) {
                            RestrictedLockUtils.sendShowAdminSupportDetailsIntent(CredentialDialogFragment.this.getContext(), admin);
                        } else {
                            CredentialDialogFragment.this.new RemoveCredentialsTask(CredentialDialogFragment.this.getContext(), CredentialDialogFragment.this.getTargetFragment()).execute(item.alias);
                        }
                        dialog.dismiss();
                    }
                };
                builder.setNegativeButton(R.string.trusted_credentials_remove_label, listener);
            }
            return builder.create();
        }

        private class RemoveCredentialsTask extends AsyncTask<String, Void, Void> {
            private Context context;
            private Fragment targetFragment;

            public RemoveCredentialsTask(Context context, Fragment targetFragment) {
                this.context = context;
                this.targetFragment = targetFragment;
            }

            @Override
            public Void doInBackground(String... aliases) {
                try {
                    KeyChain.KeyChainConnection conn = KeyChain.bind(CredentialDialogFragment.this.getContext());
                    try {
                        IKeyChainService keyChain = conn.getService();
                        for (String alias : aliases) {
                            keyChain.removeKeyPair(alias);
                        }
                        return null;
                    } catch (RemoteException e) {
                        Log.w("CredentialDialogFragment", "Removing credentials", e);
                        return null;
                    } finally {
                        conn.close();
                    }
                } catch (InterruptedException e2) {
                    Log.w("CredentialDialogFragment", "Connecting to keychain", e2);
                    return null;
                }
            }

            @Override
            public void onPostExecute(Void result) {
                if (!(this.targetFragment instanceof UserCredentialsSettings)) {
                    return;
                }
                ((UserCredentialsSettings) this.targetFragment).refreshItems();
            }
        }
    }

    private class AliasLoader extends AsyncTask<Void, Void, SortedMap<String, Credential>> {
        AliasLoader(UserCredentialsSettings this$0, AliasLoader aliasLoader) {
            this();
        }

        private AliasLoader() {
        }

        @Override
        public SortedMap<String, Credential> doInBackground(Void... params) {
            SortedMap<String, Credential> credentials = new TreeMap<>();
            KeyStore keyStore = KeyStore.getInstance();
            for (Credential.Type type : Credential.Type.valuesCustom()) {
                for (String alias : keyStore.list(type.prefix)) {
                    if (!alias.startsWith("profile_key_name_encrypt_") && !alias.startsWith("profile_key_name_decrypt_")) {
                        Credential c = credentials.get(alias);
                        if (c == null) {
                            c = new Credential(alias);
                            credentials.put(alias, c);
                        }
                        c.storedTypes.add(type);
                    }
                }
            }
            return credentials;
        }

        @Override
        public void onPostExecute(SortedMap<String, Credential> credentials) {
            UserCredentialsSettings.this.mListView.setAdapter((ListAdapter) new CredentialAdapter(UserCredentialsSettings.this.getContext(), R.layout.user_credential, (Credential[]) credentials.values().toArray(new Credential[0])));
        }
    }

    private static class CredentialAdapter extends ArrayAdapter<Credential> {
        public CredentialAdapter(Context context, int resource, Credential[] objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            if (view == null) {
                view = LayoutInflater.from(getContext()).inflate(R.layout.user_credential, parent, false);
            }
            Credential item = getItem(position);
            ((TextView) view.findViewById(R.id.alias)).setText(item.alias);
            view.findViewById(R.id.contents_userkey).setVisibility(item.storedTypes.contains(Credential.Type.USER_PRIVATE_KEY) ? 0 : 8);
            view.findViewById(R.id.contents_usercrt).setVisibility(item.storedTypes.contains(Credential.Type.USER_CERTIFICATE) ? 0 : 8);
            view.findViewById(R.id.contents_cacrt).setVisibility(item.storedTypes.contains(Credential.Type.CA_CERTIFICATE) ? 0 : 8);
            return view;
        }
    }

    static class Credential implements Parcelable {
        public static final Parcelable.Creator<Credential> CREATOR = new Parcelable.Creator<Credential>() {
            @Override
            public Credential createFromParcel(Parcel in) {
                return new Credential(in);
            }

            @Override
            public Credential[] newArray(int size) {
                return new Credential[size];
            }
        };
        final String alias;
        final EnumSet<Type> storedTypes;

        enum Type {
            CA_CERTIFICATE("CACERT_"),
            USER_CERTIFICATE("USRCERT_"),
            USER_PRIVATE_KEY("USRPKEY_"),
            USER_SECRET_KEY("USRSKEY_");

            final String prefix;

            public static Type[] valuesCustom() {
                return values();
            }

            Type(String prefix) {
                this.prefix = prefix;
            }
        }

        Credential(String alias) {
            this.storedTypes = EnumSet.noneOf(Type.class);
            this.alias = alias;
        }

        Credential(Parcel in) {
            this(in.readString());
            long typeBits = in.readLong();
            for (Type i : Type.valuesCustom()) {
                if (((1 << i.ordinal()) & typeBits) != 0) {
                    this.storedTypes.add(i);
                }
            }
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeString(this.alias);
            long typeBits = 0;
            for (Type i : this.storedTypes) {
                typeBits |= 1 << i.ordinal();
            }
            out.writeLong(typeBits);
        }

        @Override
        public int describeContents() {
            return 0;
        }
    }
}
