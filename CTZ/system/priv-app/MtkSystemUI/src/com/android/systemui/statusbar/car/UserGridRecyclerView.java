package com.android.systemui.statusbar.car;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.car.widget.PagedListView;
import com.android.internal.util.UserIcons;
import com.android.settingslib.users.UserManagerHelper;
import com.android.systemui.R;
import com.android.systemui.statusbar.car.UserGridRecyclerView;
import com.android.systemui.statusbar.phone.SystemUIDialog;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class UserGridRecyclerView extends PagedListView implements UserManagerHelper.OnUsersUpdateListener {
    private UserAdapter mAdapter;
    private Context mContext;
    private UserManagerHelper mUserManagerHelper;
    private UserSelectionListener mUserSelectionListener;

    interface UserSelectionListener {
        void onUserSelected(UserRecord userRecord);
    }

    public UserGridRecyclerView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mContext = context;
        this.mUserManagerHelper = new UserManagerHelper(this.mContext);
    }

    @Override // android.view.View
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mUserManagerHelper.registerOnUsersUpdateListener(this);
    }

    @Override // androidx.car.widget.PagedListView, android.view.ViewGroup, android.view.View
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mUserManagerHelper.unregisterOnUsersUpdateListener();
    }

    public void buildAdapter() {
        this.mAdapter = new UserAdapter(this.mContext, createUserRecords(this.mUserManagerHelper.getAllUsers()));
        super.setAdapter(this.mAdapter);
    }

    private List<UserRecord> createUserRecords(List<UserInfo> list) {
        ArrayList arrayList = new ArrayList();
        for (UserInfo userInfo : list) {
            if (!userInfo.isGuest()) {
                arrayList.add(new UserRecord(userInfo, false, false, this.mUserManagerHelper.getForegroundUserId() == userInfo.id));
            }
        }
        if (!this.mUserManagerHelper.foregroundUserIsGuestUser()) {
            arrayList.add(addGuestUserRecord());
        }
        if (this.mUserManagerHelper.foregroundUserCanAddUsers()) {
            arrayList.add(addUserRecord());
        }
        return arrayList;
    }

    private UserRecord addGuestUserRecord() {
        UserInfo userInfo = new UserInfo();
        userInfo.name = this.mContext.getString(R.string.car_guest);
        return new UserRecord(userInfo, true, false, false);
    }

    private UserRecord addUserRecord() {
        UserInfo userInfo = new UserInfo();
        userInfo.name = this.mContext.getString(R.string.car_add_user);
        return new UserRecord(userInfo, false, true, false);
    }

    public void setUserSelectionListener(UserSelectionListener userSelectionListener) {
        this.mUserSelectionListener = userSelectionListener;
    }

    @Override // com.android.settingslib.users.UserManagerHelper.OnUsersUpdateListener
    public void onUsersUpdate() {
        this.mAdapter.clearUsers();
        this.mAdapter.updateUsers(createUserRecords(this.mUserManagerHelper.getAllUsers()));
        this.mAdapter.notifyDataSetChanged();
    }

    public final class UserAdapter extends RecyclerView.Adapter<UserAdapterViewHolder> implements DialogInterface.OnClickListener {
        private UserRecord mAddUserRecord;
        private View mAddUserView;
        private final Context mContext;
        private AlertDialog mDialog;
        private final String mGuestName;
        private final String mNewUserName;
        private final Resources mRes;
        private List<UserRecord> mUsers;

        public UserAdapter(Context context, List<UserRecord> list) {
            this.mRes = context.getResources();
            this.mContext = context;
            updateUsers(list);
            this.mGuestName = this.mRes.getString(R.string.car_guest);
            this.mNewUserName = this.mRes.getString(R.string.car_new_user);
        }

        public void clearUsers() {
            this.mUsers.clear();
        }

        public void updateUsers(List<UserRecord> list) {
            this.mUsers = list;
        }

        /* JADX DEBUG: Method merged with bridge method: onCreateViewHolder(Landroid/view/ViewGroup;I)Landroid/support/v7/widget/RecyclerView$ViewHolder; */
        @Override // android.support.v7.widget.RecyclerView.Adapter
        public UserAdapterViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View viewInflate = LayoutInflater.from(this.mContext).inflate(R.layout.car_fullscreen_user_pod, viewGroup, false);
            viewInflate.setAlpha(1.0f);
            viewInflate.bringToFront();
            return new UserAdapterViewHolder(viewInflate);
        }

        /* JADX DEBUG: Method merged with bridge method: onBindViewHolder(Landroid/support/v7/widget/RecyclerView$ViewHolder;I)V */
        @Override // android.support.v7.widget.RecyclerView.Adapter
        public void onBindViewHolder(final UserAdapterViewHolder userAdapterViewHolder, int i) {
            final UserRecord userRecord = this.mUsers.get(i);
            RoundedBitmapDrawable roundedBitmapDrawableCreate = RoundedBitmapDrawableFactory.create(this.mRes, getUserRecordIcon(userRecord));
            roundedBitmapDrawableCreate.setCircular(true);
            userAdapterViewHolder.mUserAvatarImageView.setImageDrawable(roundedBitmapDrawableCreate);
            userAdapterViewHolder.mUserNameTextView.setText(userRecord.mInfo.name);
            userAdapterViewHolder.mView.setOnClickListener(new View.OnClickListener() { // from class: com.android.systemui.statusbar.car.-$$Lambda$UserGridRecyclerView$UserAdapter$n2iLfR_SwwIaOydjDtvjvbVeQ9Y
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    UserGridRecyclerView.UserAdapter.lambda$onBindViewHolder$0(this.f$0, userRecord, userAdapterViewHolder, view);
                }
            });
        }

        public static /* synthetic */ void lambda$onBindViewHolder$0(UserAdapter userAdapter, UserRecord userRecord, UserAdapterViewHolder userAdapterViewHolder, View view) {
            if (userRecord == null) {
                return;
            }
            if (userRecord.mIsStartGuestSession) {
                userAdapter.notifyUserSelected(userRecord);
                UserGridRecyclerView.this.mUserManagerHelper.startNewGuestSession(userAdapter.mGuestName);
                return;
            }
            if (userRecord.mIsAddUser) {
                userAdapter.mAddUserView = userAdapterViewHolder.mView;
                userAdapter.mAddUserView.setEnabled(false);
                String strConcat = userAdapter.mRes.getString(R.string.user_add_user_message_setup).concat(System.getProperty("line.separator")).concat(System.getProperty("line.separator")).concat(userAdapter.mRes.getString(R.string.user_add_user_message_update));
                userAdapter.mAddUserRecord = userRecord;
                userAdapter.mDialog = new AlertDialog.Builder(userAdapter.mContext, 2131886592).setTitle(R.string.user_add_user_title).setMessage(strConcat).setNegativeButton(android.R.string.cancel, userAdapter).setPositiveButton(android.R.string.ok, userAdapter).create();
                SystemUIDialog.applyFlags(userAdapter.mDialog);
                userAdapter.mDialog.show();
                return;
            }
            userAdapter.notifyUserSelected(userRecord);
            UserGridRecyclerView.this.mUserManagerHelper.switchToUser(userRecord.mInfo);
        }

        private void notifyUserSelected(UserRecord userRecord) {
            if (UserGridRecyclerView.this.mUserSelectionListener != null) {
                UserGridRecyclerView.this.mUserSelectionListener.onUserSelected(userRecord);
            }
        }

        private Bitmap getUserRecordIcon(UserRecord userRecord) {
            if (userRecord.mIsStartGuestSession) {
                return UserGridRecyclerView.this.mUserManagerHelper.getGuestDefaultIcon();
            }
            if (!userRecord.mIsAddUser) {
                return UserGridRecyclerView.this.mUserManagerHelper.getUserIcon(userRecord.mInfo);
            }
            return UserIcons.convertToBitmap(this.mContext.getDrawable(R.drawable.car_add_circle_round));
        }

        @Override // android.content.DialogInterface.OnClickListener
        public void onClick(DialogInterface dialogInterface, int i) {
            if (i == -1) {
                notifyUserSelected(this.mAddUserRecord);
                new AddNewUserTask().execute(this.mNewUserName);
            } else if (i == -2 && this.mAddUserView != null) {
                this.mAddUserView.setEnabled(true);
            }
        }

        private class AddNewUserTask extends AsyncTask<String, Void, UserInfo> {
            private AddNewUserTask() {
            }

            /* JADX DEBUG: Method merged with bridge method: doInBackground([Ljava/lang/Object;)Ljava/lang/Object; */
            @Override // android.os.AsyncTask
            protected UserInfo doInBackground(String... strArr) {
                return UserGridRecyclerView.this.mUserManagerHelper.createNewUser(strArr[0]);
            }

            @Override // android.os.AsyncTask
            protected void onPreExecute() {
            }

            /* JADX DEBUG: Method merged with bridge method: onPostExecute(Ljava/lang/Object;)V */
            @Override // android.os.AsyncTask
            protected void onPostExecute(UserInfo userInfo) {
                if (userInfo != null) {
                    UserGridRecyclerView.this.mUserManagerHelper.switchToUser(userInfo);
                }
            }
        }

        @Override // android.support.v7.widget.RecyclerView.Adapter
        public int getItemCount() {
            return this.mUsers.size();
        }

        public class UserAdapterViewHolder extends RecyclerView.ViewHolder {
            public ImageView mUserAvatarImageView;
            public TextView mUserNameTextView;
            public View mView;

            public UserAdapterViewHolder(View view) {
                super(view);
                this.mView = view;
                this.mUserAvatarImageView = (ImageView) view.findViewById(R.id.user_avatar);
                this.mUserNameTextView = (TextView) view.findViewById(R.id.user_name);
            }
        }
    }

    public static final class UserRecord {
        public final UserInfo mInfo;
        public final boolean mIsAddUser;
        public final boolean mIsForeground;
        public final boolean mIsStartGuestSession;

        public UserRecord(UserInfo userInfo, boolean z, boolean z2, boolean z3) {
            this.mInfo = userInfo;
            this.mIsStartGuestSession = z;
            this.mIsAddUser = z2;
            this.mIsForeground = z3;
        }
    }
}
