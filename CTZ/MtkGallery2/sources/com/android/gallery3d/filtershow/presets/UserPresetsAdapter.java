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

    public UserPresetsAdapter(Context context, int i) {
        super(context, i);
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
        FilterRepresentation representation = action.getRepresentation();
        if (representation instanceof FilterUserPresetRepresentation) {
            this.mDeletedRepresentations.add((FilterUserPresetRepresentation) representation);
        }
        remove(action);
        notifyDataSetChanged();
    }

    private void changePreset(Action action) {
        FilterRepresentation representation = action.getRepresentation();
        representation.setName(action.getName());
        if (representation instanceof FilterUserPresetRepresentation) {
            this.mChangedRepresentations.add((FilterUserPresetRepresentation) representation);
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
    public View getView(int i, View view, ViewGroup viewGroup) {
        UserPresetViewHolder userPresetViewHolder;
        if (view == null) {
            view = this.mInflater.inflate(R.layout.filtershow_presets_management_row, (ViewGroup) null);
            userPresetViewHolder = new UserPresetViewHolder();
            userPresetViewHolder.imageView = (ImageView) view.findViewById(R.id.imageView);
            userPresetViewHolder.editText = (EditText) view.findViewById(R.id.editView);
            userPresetViewHolder.deleteButton = (ImageButton) view.findViewById(R.id.deleteUserPreset);
            userPresetViewHolder.editText.setOnClickListener(this);
            userPresetViewHolder.editText.setOnFocusChangeListener(this);
            userPresetViewHolder.deleteButton.setOnClickListener(this);
            view.setTag(userPresetViewHolder);
        } else {
            userPresetViewHolder = (UserPresetViewHolder) view.getTag();
        }
        Action item = getItem(i);
        userPresetViewHolder.imageView.setImageBitmap(item.getImage());
        if (item.getImage() == null) {
            item.setImageFrame(new Rect(0, 0, this.mIconSize, this.mIconSize), 0);
        }
        userPresetViewHolder.deleteButton.setTag(item);
        userPresetViewHolder.editText.setTag(item);
        userPresetViewHolder.editText.setHint(item.getName());
        return view;
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
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.editView) {
            view.requestFocus();
        } else if (id == R.id.deleteUserPreset) {
            deletePreset((Action) view.getTag());
        }
    }

    @Override
    public void onFocusChange(View view, boolean z) {
        if (view.getId() != R.id.editView) {
            return;
        }
        EditText editText = (EditText) view;
        if (!z) {
            updateActionFromEditText(editText);
        } else {
            this.mCurrentEditText = editText;
        }
    }

    private void updateActionFromEditText(EditText editText) {
        Action action = (Action) editText.getTag();
        if (editText.getText().toString().length() > 0) {
            action.setName(editText.getText().toString());
            changePreset(action);
        }
    }
}
