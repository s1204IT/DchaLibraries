package com.android.systemui.statusbar.car;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import com.android.systemui.R;
import com.android.systemui.statusbar.UserUtil;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import com.android.systemui.statusbar.policy.UserSwitcherController;

public class UserGridView extends GridView {
    private Adapter mAdapter;
    private int mPendingUserId;
    private PhoneStatusBar mStatusBar;
    private UserSwitcherController mUserSwitcherController;

    public UserGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mPendingUserId = -10000;
    }

    public void init(PhoneStatusBar statusBar, UserSwitcherController userSwitcherController) {
        this.mStatusBar = statusBar;
        this.mUserSwitcherController = userSwitcherController;
        this.mAdapter = new Adapter(this.mUserSwitcherController);
        setAdapter((ListAdapter) this.mAdapter);
        setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                UserGridView.this.mPendingUserId = -10000;
                UserSwitcherController.UserRecord record = UserGridView.this.mAdapter.getItem(position);
                if (record == null) {
                    return;
                }
                if (record.isGuest || record.isAddUser) {
                    UserGridView.this.mUserSwitcherController.switchTo(record);
                } else {
                    if (record.isCurrent) {
                        UserGridView.this.showOfflineAuthUi();
                        return;
                    }
                    UserGridView.this.mPendingUserId = record.info.id;
                    UserGridView.this.mUserSwitcherController.switchTo(record);
                }
            }
        });
        setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                UserSwitcherController.UserRecord record = UserGridView.this.mAdapter.getItem(position);
                if (record == null || record.isAddUser) {
                    return false;
                }
                if (record.isGuest) {
                    if (record.isCurrent) {
                        UserGridView.this.mUserSwitcherController.switchTo(record);
                    }
                    return true;
                }
                UserUtil.deleteUserWithPrompt(UserGridView.this.getContext(), record.info.id, UserGridView.this.mUserSwitcherController);
                return true;
            }
        });
    }

    public void onUserSwitched(int newUserId) {
        if (this.mPendingUserId == newUserId) {
            post(new Runnable() {
                @Override
                public void run() {
                    UserGridView.this.showOfflineAuthUi();
                }
            });
        }
        this.mPendingUserId = -10000;
    }

    public void showOfflineAuthUi() {
        this.mStatusBar.executeRunnableDismissingKeyguard(null, null, true, true, true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
        if (widthMode == 0) {
            setNumColumns(-1);
        } else {
            int columnWidth = Math.max(1, getRequestedColumnWidth());
            int itemCount = getAdapter() == null ? 0 : getAdapter().getCount();
            int numColumns = Math.max(1, Math.min(itemCount, widthSize / columnWidth));
            setNumColumns(numColumns);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private final class Adapter extends UserSwitcherController.BaseUserAdapter {
        public Adapter(UserSwitcherController controller) {
            super(controller);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) UserGridView.this.getContext().getSystemService("layout_inflater");
                convertView = inflater.inflate(R.layout.car_fullscreen_user_pod, (ViewGroup) null);
            }
            UserSwitcherController.UserRecord record = getItem(position);
            TextView nameView = (TextView) convertView.findViewById(R.id.user_name);
            if (record != null) {
                nameView.setText(getName(UserGridView.this.getContext(), record));
                convertView.setActivated(record.isCurrent);
            } else {
                nameView.setText("Unknown");
            }
            ImageView iconView = (ImageView) convertView.findViewById(R.id.user_avatar);
            if (record == null || record.picture == null) {
                iconView.setImageDrawable(getDrawable(UserGridView.this.getContext(), record));
            } else {
                iconView.setImageBitmap(record.picture);
            }
            return convertView;
        }
    }
}
