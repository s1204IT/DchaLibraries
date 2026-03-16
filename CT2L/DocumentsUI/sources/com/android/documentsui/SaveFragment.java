package com.android.documentsui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import com.android.documentsui.model.DocumentInfo;

public class SaveFragment extends Fragment {
    private EditText mDisplayName;
    private boolean mIgnoreNextEdit;
    private ProgressBar mProgress;
    private DocumentInfo mReplaceTarget;
    private Button mSave;
    private TextWatcher mDisplayNameWatcher = new TextWatcher() {
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (SaveFragment.this.mIgnoreNextEdit) {
                SaveFragment.this.mIgnoreNextEdit = false;
            } else {
                SaveFragment.this.mReplaceTarget = null;
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };
    private View.OnClickListener mSaveListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            DocumentsActivity activity = DocumentsActivity.get(SaveFragment.this);
            if (SaveFragment.this.mReplaceTarget != null) {
                activity.onSaveRequested(SaveFragment.this.mReplaceTarget);
                return;
            }
            String mimeType = SaveFragment.this.getArguments().getString("mime_type");
            String displayName = SaveFragment.this.mDisplayName.getText().toString();
            activity.onSaveRequested(mimeType, displayName);
        }
    };

    public static void show(FragmentManager fm, String mimeType, String displayName) {
        Bundle args = new Bundle();
        args.putString("mime_type", mimeType);
        args.putString("display_name", displayName);
        SaveFragment fragment = new SaveFragment();
        fragment.setArguments(args);
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.container_save, fragment, "SaveFragment");
        ft.commitAllowingStateLoss();
    }

    public static SaveFragment get(FragmentManager fm) {
        return (SaveFragment) fm.findFragmentByTag("SaveFragment");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Context context = inflater.getContext();
        View view = inflater.inflate(R.layout.fragment_save, container, false);
        ImageView icon = (ImageView) view.findViewById(android.R.id.icon);
        icon.setImageDrawable(IconUtils.loadMimeIcon(context, getArguments().getString("mime_type")));
        this.mDisplayName = (EditText) view.findViewById(android.R.id.title);
        this.mDisplayName.addTextChangedListener(this.mDisplayNameWatcher);
        this.mDisplayName.setText(getArguments().getString("display_name"));
        this.mSave = (Button) view.findViewById(android.R.id.button1);
        this.mSave.setOnClickListener(this.mSaveListener);
        this.mSave.setEnabled(false);
        this.mProgress = (ProgressBar) view.findViewById(android.R.id.progress);
        return view;
    }

    public void setReplaceTarget(DocumentInfo replaceTarget) {
        this.mReplaceTarget = replaceTarget;
        if (this.mReplaceTarget != null) {
            getArguments().putString("display_name", replaceTarget.displayName);
            this.mIgnoreNextEdit = true;
            this.mDisplayName.setText(replaceTarget.displayName);
        }
    }

    public void setSaveEnabled(boolean enabled) {
        this.mSave.setEnabled(enabled);
    }

    public void setPending(boolean pending) {
        this.mSave.setVisibility(pending ? 4 : 0);
        this.mProgress.setVisibility(pending ? 0 : 8);
    }
}
