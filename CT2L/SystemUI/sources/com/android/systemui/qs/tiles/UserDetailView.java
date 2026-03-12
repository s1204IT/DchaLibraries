package com.android.systemui.qs.tiles;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.systemui.R;
import com.android.systemui.qs.PseudoGridView;
import com.android.systemui.statusbar.policy.UserSwitcherController;

public class UserDetailView extends PseudoGridView {
    private Adapter mAdapter;

    public UserDetailView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public static UserDetailView inflate(Context context, ViewGroup parent, boolean attach) {
        return (UserDetailView) LayoutInflater.from(context).inflate(R.layout.qs_user_detail, parent, attach);
    }

    public void createAndSetAdapter(UserSwitcherController controller) {
        this.mAdapter = new Adapter(this.mContext, controller);
        PseudoGridView.ViewGroupAdapterBridge.link(this, this.mAdapter);
    }

    public void refreshAdapter() {
        this.mAdapter.refresh();
    }

    public static class Adapter extends UserSwitcherController.BaseUserAdapter implements View.OnClickListener {
        private Context mContext;

        public Adapter(Context context, UserSwitcherController controller) {
            super(controller);
            this.mContext = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            UserSwitcherController.UserRecord item = getItem(position);
            UserDetailItemView v = UserDetailItemView.convertOrInflate(this.mContext, convertView, parent);
            if (v != convertView) {
                v.setOnClickListener(this);
            }
            String name = getName(this.mContext, item);
            if (item.picture == null) {
                v.bind(name, getDrawable(this.mContext, item));
            } else {
                v.bind(name, item.picture);
            }
            v.setActivated(item.isCurrent);
            v.setTag(item);
            return v;
        }

        @Override
        public void onClick(View view) {
            UserSwitcherController.UserRecord tag = (UserSwitcherController.UserRecord) view.getTag();
            switchTo(tag);
        }
    }
}
