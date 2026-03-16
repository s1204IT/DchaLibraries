package com.android.gallery3d.filtershow.presets;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.FilterShowActivity;

public class PresetManagementDialog extends DialogFragment implements View.OnClickListener {
    private UserPresetsAdapter mAdapter;
    private EditText mEditText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.filtershow_presets_management_dialog, container);
        FilterShowActivity activity = (FilterShowActivity) getActivity();
        this.mAdapter = activity.getUserPresetsAdapter();
        this.mEditText = (EditText) view.findViewById(R.id.editView);
        view.findViewById(R.id.cancel).setOnClickListener(this);
        view.findViewById(R.id.ok).setOnClickListener(this);
        getDialog().setTitle(getString(R.string.filtershow_save_preset));
        return view;
    }

    @Override
    public void onClick(View v) {
        FilterShowActivity activity = (FilterShowActivity) getActivity();
        switch (v.getId()) {
            case R.id.cancel:
                this.mAdapter.clearChangedRepresentations();
                this.mAdapter.clearDeletedRepresentations();
                activity.updateUserPresetsFromAdapter(this.mAdapter);
                dismiss();
                break;
            case R.id.ok:
                String text = String.valueOf(this.mEditText.getText());
                activity.saveCurrentImagePreset(text);
                this.mAdapter.updateCurrent();
                activity.updateUserPresetsFromAdapter(this.mAdapter);
                dismiss();
                break;
        }
    }
}
