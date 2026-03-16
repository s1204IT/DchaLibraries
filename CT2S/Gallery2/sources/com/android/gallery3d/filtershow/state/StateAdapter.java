package com.android.gallery3d.filtershow.state;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import java.util.Vector;

public class StateAdapter extends ArrayAdapter<State> {
    private int mOrientation;
    private String mOriginalText;
    private String mResultText;

    public StateAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
        this.mOriginalText = context.getString(R.string.state_panel_original);
        this.mResultText = context.getString(R.string.state_panel_result);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = new StateView(getContext());
        }
        StateView view = (StateView) convertView;
        State state = getItem(position);
        view.setState(state);
        view.setOrientation(this.mOrientation);
        FilterRepresentation currentRep = MasterImage.getImage().getCurrentFilterRepresentation();
        FilterRepresentation stateRep = state.getFilterRepresentation();
        if (currentRep != null && stateRep != null && currentRep.getFilterClass() == stateRep.getFilterClass() && currentRep.getEditorId() != R.id.imageOnlyEditor) {
            view.setSelected(true);
        } else {
            view.setSelected(false);
        }
        return view;
    }

    public boolean contains(State state) {
        for (int i = 0; i < getCount(); i++) {
            if (state == getItem(i)) {
                return true;
            }
        }
        return false;
    }

    public void setOrientation(int orientation) {
        this.mOrientation = orientation;
    }

    public void addOriginal() {
        add(new State(this.mOriginalText));
    }

    public boolean same(Vector<State> states) {
        if (states.size() + 1 != getCount()) {
            return false;
        }
        for (int i = 1; i < getCount(); i++) {
            State state = getItem(i);
            if (!state.equals(states.elementAt(i - 1))) {
                return false;
            }
        }
        return true;
    }

    public void fill(Vector<State> states) {
        if (!same(states)) {
            clear();
            addOriginal();
            addAll(states);
            notifyDataSetChanged();
        }
    }

    @Override
    public void remove(State state) {
        super.remove(state);
        FilterRepresentation filterRepresentation = state.getFilterRepresentation();
        FilterShowActivity activity = (FilterShowActivity) getContext();
        activity.removeFilterRepresentation(filterRepresentation);
    }
}
