package com.android.documentsui.picker;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.documentsui.IconUtils;
import com.android.documentsui.Injector;
import com.android.documentsui.R;
import com.android.documentsui.base.BooleanConsumer;
import com.android.documentsui.base.DocumentInfo;

public class SaveFragment extends Fragment {
    private EditText mDisplayName;
    private boolean mIgnoreNextEdit;
    private Injector<ActionHandler<PickActivity>> mInjector;
    private ProgressBar mProgress;
    private DocumentInfo mReplaceTarget;
    private TextView mSave;
    private final BooleanConsumer mInProgressStateListener = new BooleanConsumer() {
        @Override
        public final void accept(boolean z) {
            this.f$0.setPending(z);
        }
    };
    private TextWatcher mDisplayNameWatcher = new TextWatcher() {
        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            if (SaveFragment.this.mIgnoreNextEdit) {
                SaveFragment.this.mIgnoreNextEdit = false;
            } else {
                SaveFragment.this.mReplaceTarget = null;
            }
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
        }
    };
    private View.OnClickListener mSaveListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            SaveFragment.this.performSave();
        }
    };

    static void show(FragmentManager fragmentManager, String str, String str2) {
        Bundle bundle = new Bundle();
        bundle.putString("mime_type", str);
        bundle.putString("display_name", str2);
        SaveFragment saveFragment = new SaveFragment();
        saveFragment.setArguments(bundle);
        FragmentTransaction fragmentTransactionBeginTransaction = fragmentManager.beginTransaction();
        fragmentTransactionBeginTransaction.replace(R.id.container_save, saveFragment, "SaveFragment");
        fragmentTransactionBeginTransaction.commitAllowingStateLoss();
    }

    public static SaveFragment get(FragmentManager fragmentManager) {
        return (SaveFragment) fragmentManager.findFragmentByTag("SaveFragment");
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        Context context = layoutInflater.getContext();
        View viewInflate = layoutInflater.inflate(R.layout.fragment_save, viewGroup, false);
        ((ImageView) viewInflate.findViewById(android.R.id.icon)).setImageDrawable(IconUtils.loadMimeIcon(context, getArguments().getString("mime_type")));
        this.mDisplayName = (EditText) viewInflate.findViewById(android.R.id.title);
        this.mDisplayName.addTextChangedListener(this.mDisplayNameWatcher);
        this.mDisplayName.setText(getArguments().getString("display_name"));
        this.mDisplayName.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (keyEvent.getAction() != 0) {
                    return false;
                }
                if (i == 67 && TextUtils.isEmpty(SaveFragment.this.mDisplayName.getText())) {
                    return true;
                }
                if (i != 66 || !SaveFragment.this.mSave.isEnabled()) {
                    return false;
                }
                SaveFragment.this.performSave();
                return true;
            }
        });
        this.mSave = (TextView) viewInflate.findViewById(android.R.id.button1);
        this.mSave.setOnClickListener(this.mSaveListener);
        this.mSave.setEnabled(false);
        this.mProgress = (ProgressBar) viewInflate.findViewById(android.R.id.progress);
        return viewInflate;
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        this.mInjector = ((PickActivity) getActivity()).getInjector();
        if (bundle != null) {
            this.mReplaceTarget = (DocumentInfo) bundle.getParcelable("document");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putParcelable("document", this.mReplaceTarget);
    }

    private void performSave() {
        if (this.mReplaceTarget != null) {
            ((ActionHandler) this.mInjector.actions).saveDocument(getChildFragmentManager(), this.mReplaceTarget);
            return;
        }
        ((ActionHandler) this.mInjector.actions).saveDocument(getArguments().getString("mime_type"), this.mDisplayName.getText().toString(), this.mInProgressStateListener);
    }

    public void setReplaceTarget(DocumentInfo documentInfo) {
        this.mReplaceTarget = documentInfo;
        if (this.mReplaceTarget != null) {
            getArguments().putString("display_name", documentInfo.displayName);
            this.mIgnoreNextEdit = true;
            this.mDisplayName.setText(documentInfo.displayName);
        }
    }

    public void prepareForDirectory(DocumentInfo documentInfo) {
        setSaveEnabled(documentInfo != null && documentInfo.isCreateSupported());
    }

    private void setSaveEnabled(boolean z) {
        this.mSave.setEnabled(z);
    }

    private void setPending(boolean z) {
        this.mSave.setVisibility(z ? 4 : 0);
        this.mProgress.setVisibility(z ? 0 : 8);
    }
}
