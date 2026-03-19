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

    public StateAdapter(Context context, int i) {
        super(context, i);
        this.mOriginalText = context.getString(R.string.state_panel_original);
        this.mResultText = context.getString(R.string.state_panel_result);
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = new StateView(getContext());
        }
        StateView stateView = (StateView) view;
        State item = getItem(i);
        stateView.setState(item);
        stateView.setOrientation(this.mOrientation);
        FilterRepresentation currentFilterRepresentation = MasterImage.getImage().getCurrentFilterRepresentation();
        FilterRepresentation filterRepresentation = item.getFilterRepresentation();
        if (currentFilterRepresentation != null && filterRepresentation != null && currentFilterRepresentation.getFilterClass() == filterRepresentation.getFilterClass() && currentFilterRepresentation.getEditorId() != R.id.imageOnlyEditor) {
            stateView.setSelected(true);
        } else {
            stateView.setSelected(false);
        }
        return stateView;
    }

    public boolean contains(State state) {
        for (int i = 0; i < getCount(); i++) {
            if (state == getItem(i)) {
                return true;
            }
        }
        return false;
    }

    public void setOrientation(int i) {
        this.mOrientation = i;
    }

    public void addOriginal() {
        add(new State(this.mOriginalText));
    }

    public boolean same(Vector<State> vector) {
        if (vector.size() + 1 != getCount()) {
            return false;
        }
        for (int i = 1; i < getCount(); i++) {
            if (!getItem(i).equals(vector.elementAt(i - 1))) {
                return false;
            }
        }
        return true;
    }

    public void fill(Vector<State> vector) {
        if (same(vector)) {
            return;
        }
        clear();
        addOriginal();
        addAll(vector);
        notifyDataSetChanged();
    }

    @Override
    public void remove(State state) {
        super.remove(state);
        ((FilterShowActivity) getContext()).removeFilterRepresentation(state.getFilterRepresentation());
    }
}
