package com.android.gallery3d.ui;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import com.android.gallery3d.R;
import com.android.gallery3d.ui.PopupList;

public class SelectionMenu implements View.OnClickListener {
    private final Button mButton;
    private final Context mContext;
    private final PopupList mPopupList;

    public SelectionMenu(Context context, Button button, PopupList.OnPopupItemClickListener listener) {
        this.mContext = context;
        this.mButton = button;
        this.mPopupList = new PopupList(context, this.mButton);
        this.mPopupList.addItem(R.id.action_select_all, context.getString(R.string.select_all));
        this.mPopupList.setOnPopupItemClickListener(listener);
        this.mButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        this.mPopupList.show();
    }

    public void updateSelectAllMode(boolean inSelectAllMode) {
        PopupList.Item item = this.mPopupList.findItem(R.id.action_select_all);
        if (item != null) {
            item.setTitle(this.mContext.getString(inSelectAllMode ? R.string.deselect_all : R.string.select_all));
        }
    }

    public void setTitle(CharSequence title) {
        this.mButton.setText(title);
    }
}
