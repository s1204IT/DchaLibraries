package com.android.gallery3d.filtershow.presets;

import android.content.Context;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.category.Action;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.filters.FilterUserPresetRepresentation;
import java.util.ArrayList;

public class UserPresetsAdapter extends ArrayAdapter<Action> implements View.OnClickListener, View.OnFocusChangeListener {
    private ArrayList<FilterUserPresetRepresentation> mChangedRepresentations;
    private EditText mCurrentEditText;
    private ArrayList<FilterUserPresetRepresentation> mDeletedRepresentations;
    private int mIconSize;
    private LayoutInflater mInflater;

    public UserPresetsAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
        this.mIconSize = 160;
        this.mDeletedRepresentations = new ArrayList<>();
        this.mChangedRepresentations = new ArrayList<>();
        this.mInflater = LayoutInflater.from(context);
        this.mIconSize = context.getResources().getDimensionPixelSize(R.dimen.category_panel_icon_size);
    }

    public UserPresetsAdapter(Context context) {
        this(context, 0);
    }

    @Override
    public void add(Action action) {
        super.add(action);
        action.setAdapter(this);
    }

    private void deletePreset(Action action) {
        FilterRepresentation rep = action.getRepresentation();
        if (rep instanceof FilterUserPresetRepresentation) {
            this.mDeletedRepresentations.add((FilterUserPresetRepresentation) rep);
        }
        remove(action);
        notifyDataSetChanged();
    }

    private void changePreset(Action action) {
        FilterRepresentation rep = action.getRepresentation();
        rep.setName(action.getName());
        if (rep instanceof FilterUserPresetRepresentation) {
            this.mChangedRepresentations.add((FilterUserPresetRepresentation) rep);
        }
    }

    public void updateCurrent() {
        if (this.mCurrentEditText != null) {
            updateActionFromEditText(this.mCurrentEditText);
        }
    }

    static class UserPresetViewHolder {
        ImageButton deleteButton;
        EditText editText;
        ImageView imageView;

        UserPresetViewHolder() {
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        UserPresetViewHolder viewHolder;
        if (convertView == null) {
            convertView = this.mInflater.inflate(R.layout.filtershow_presets_management_row, (ViewGroup) null);
            viewHolder = new UserPresetViewHolder();
            viewHolder.imageView = (ImageView) convertView.findViewById(R.id.imageView);
            viewHolder.editText = (EditText) convertView.findViewById(R.id.editView);
            viewHolder.deleteButton = (ImageButton) convertView.findViewById(R.id.deleteUserPreset);
            viewHolder.editText.setOnClickListener(this);
            viewHolder.editText.setOnFocusChangeListener(this);
            viewHolder.deleteButton.setOnClickListener(this);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (UserPresetViewHolder) convertView.getTag();
        }
        Action action = getItem(position);
        viewHolder.imageView.setImageBitmap(action.getImage());
        if (action.getImage() == null) {
            action.setImageFrame(new Rect(0, 0, this.mIconSize, this.mIconSize), 0);
        }
        viewHolder.deleteButton.setTag(action);
        viewHolder.editText.setTag(action);
        viewHolder.editText.setHint(action.getName());
        return convertView;
    }

    public ArrayList<FilterUserPresetRepresentation> getDeletedRepresentations() {
        return this.mDeletedRepresentations;
    }

    public void clearDeletedRepresentations() {
        this.mDeletedRepresentations.clear();
    }

    public ArrayList<FilterUserPresetRepresentation> getChangedRepresentations() {
        return this.mChangedRepresentations;
    }

    public void clearChangedRepresentations() {
        this.mChangedRepresentations.clear();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.editView:
                v.requestFocus();
                break;
            case R.id.deleteUserPreset:
                Action action = (Action) v.getTag();
                deletePreset(action);
                break;
        }
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (v.getId() == R.id.editView) {
            EditText editText = (EditText) v;
            if (!hasFocus) {
                updateActionFromEditText(editText);
            } else {
                this.mCurrentEditText = editText;
            }
        }
    }

    private void updateActionFromEditText(EditText editText) {
        Action action = (Action) editText.getTag();
        String newName = editText.getText().toString();
        if (newName.length() > 0) {
            action.setName(editText.getText().toString());
            changePreset(action);
        }
    }
}
