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
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View viewInflate = layoutInflater.inflate(R.layout.filtershow_presets_management_dialog, viewGroup);
        this.mEditText = (EditText) viewInflate.findViewById(R.id.editView);
        viewInflate.findViewById(R.id.cancel).setOnClickListener(this);
        viewInflate.findViewById(R.id.ok).setOnClickListener(this);
        getDialog().setTitle(getString(R.string.filtershow_save_preset));
        return viewInflate;
    }

    @Override
    public void onClick(View view) {
        FilterShowActivity filterShowActivity = (FilterShowActivity) getActivity();
        this.mAdapter = filterShowActivity.getUserPresetsAdapter();
        int id = view.getId();
        if (id == R.id.cancel) {
            this.mAdapter.clearChangedRepresentations();
            this.mAdapter.clearDeletedRepresentations();
            filterShowActivity.updateUserPresetsFromAdapter(this.mAdapter);
            dismiss();
            return;
        }
        if (id == R.id.ok) {
            filterShowActivity.saveCurrentImagePreset(String.valueOf(this.mEditText.getText()));
            this.mAdapter.updateCurrent();
            filterShowActivity.updateUserPresetsFromAdapter(this.mAdapter);
            dismiss();
        }
    }
}
