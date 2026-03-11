package com.android.settings;

import android.content.Context;
import android.content.pm.UserInfo;
import android.database.DataSetObserver;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.os.UserManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import com.android.internal.util.UserIcons;
import java.util.ArrayList;

public class UserSpinnerAdapter implements SpinnerAdapter {
    private ArrayList<UserDetails> data;
    private final LayoutInflater mInflater;

    public static class UserDetails {
        private final Drawable icon;
        private final UserHandle mUserHandle;
        private final String name;

        public UserDetails(UserHandle userHandle, UserManager um, Context context) {
            this.mUserHandle = userHandle;
            UserInfo userInfo = um.getUserInfo(this.mUserHandle.getIdentifier());
            if (userInfo.isManagedProfile()) {
                this.name = context.getString(R.string.managed_user_title);
                this.icon = context.getDrawable(android.R.drawable.emo_im_wtf);
                return;
            }
            this.name = userInfo.name;
            int userId = userInfo.id;
            if (um.getUserIcon(userId) != null) {
                this.icon = new BitmapDrawable(context.getResources(), um.getUserIcon(userId));
            } else {
                this.icon = UserIcons.getDefaultUserIcon(userId, false);
            }
        }
    }

    public UserSpinnerAdapter(Context context, ArrayList<UserDetails> users) {
        if (users == null) {
            throw new IllegalArgumentException("A list of user details must be provided");
        }
        this.data = users;
        this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
    }

    public UserHandle getUserHandle(int position) {
        if (position >= 0 && position < this.data.size()) {
            return this.data.get(position).mUserHandle;
        }
        return null;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View row = convertView != null ? convertView : createUser(parent);
        UserDetails user = this.data.get(position);
        ((ImageView) row.findViewById(android.R.id.icon)).setImageDrawable(user.icon);
        ((TextView) row.findViewById(android.R.id.title)).setText(user.name);
        return row;
    }

    private View createUser(ViewGroup parent) {
        return this.mInflater.inflate(R.layout.user_preference, parent, false);
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
    }

    @Override
    public int getCount() {
        return this.data.size();
    }

    @Override
    public UserDetails getItem(int position) {
        return this.data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return this.data.get(position).mUserHandle.getIdentifier();
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getDropDownView(position, convertView, parent);
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return this.data.isEmpty();
    }
}
