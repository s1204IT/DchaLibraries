package com.android.gallery3d.ui;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import com.android.gallery3d.R;
import com.android.gallery3d.ui.PopupList;

public class SelectionMenu implements View.OnClickListener {
    private static final String TAG = "Gallery2/SelectionMenu";
    private final Button mButton;
    private final Context mContext;
    private final PopupList mPopupList;

    public SelectionMenu(Context context, Button button, PopupList.OnPopupItemClickListener onPopupItemClickListener) {
        this.mContext = context;
        this.mButton = button;
        this.mPopupList = new PopupList(context, this.mButton);
        this.mPopupList.addItem(R.id.action_select_all, context.getString(R.string.select_all));
        this.mPopupList.setOnPopupItemClickListener(onPopupItemClickListener);
        this.mButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        this.mPopupList.show();
    }

    public void updateSelectAllMode(boolean z) {
        PopupList.Item itemFindItem = this.mPopupList.findItem(R.id.action_select_all);
        if (itemFindItem != null) {
            itemFindItem.setTitle(this.mContext.getString(z ? R.string.deselect_all : R.string.select_all));
        }
    }

    public void setTitle(CharSequence charSequence) {
        this.mButton.setText(charSequence);
    }

    public void disablePopup() {
        this.mButton.setClickable(false);
    }

    public void finish() {
        if (this.mPopupList != null) {
            this.mPopupList.finish();
        }
    }
}
