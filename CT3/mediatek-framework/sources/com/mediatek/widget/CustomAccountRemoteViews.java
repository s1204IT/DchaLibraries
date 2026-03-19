package com.mediatek.widget;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.widget.RemoteViews;
import java.util.ArrayList;
import java.util.List;

public class CustomAccountRemoteViews {
    private static final int MOSTACCOUNTNUMBER = 8;
    private static final int ROWACCOUNTNUMBER = 4;
    private static final String TAG = "CustomAccountRemoteViews";
    private final int[][] RESOURCE_ID;
    private RemoteViews mBigRemoteViews;
    private Context mContext;
    private List<AccountInfo> mData;
    private RemoteViews mNormalRemoteViews;
    private List<AccountInfo> mOtherAccounts;
    private int mRequestCode;

    private final class IdIndex {
        public static final int CONTAINER_ID = 0;
        public static final int HIGHTLIGHT_DIVIDER_ID = 5;
        public static final int HIGHTLIGHT_DIVIDER_MORE_ID = 7;
        public static final int IMG_ID = 1;
        public static final int NAME_ID = 2;
        public static final int NORMAL_DIVIDER_ID = 4;
        public static final int NORMAL_DIVIDER_MORE_ID = 6;
        public static final int NUMBER_ID = 3;

        private IdIndex() {
        }
    }

    public CustomAccountRemoteViews(Context context, String packageName) {
        this(context, packageName, null);
    }

    public CustomAccountRemoteViews(Context context, String packageName, List<AccountInfo> data) {
        this.mOtherAccounts = new ArrayList();
        this.RESOURCE_ID = new int[][]{new int[]{135331975, 135331890, 135331895, 135331896, 135331891, 135331892, 135331893, 135331894}, new int[]{135331976, 135331855, 135331860, 135331861, 135331856, 135331857, 135331858, 135331859}, new int[]{135331977, 135331883, 135331888, 135331889, 135331884, 135331885, 135331886, 135331887}, new int[]{135331978, 135331876, 135331881, 135331882, 135331877, 135331878, 135331879, 135331880}, new int[]{135331847, 135331848, 135331853, 135331854, 135331849, 135331850, 135331851, 135331852}, new int[]{135331980, 135331840, 135331845, 135331846, 135331841, 135331842, 135331843, 135331844}, new int[]{135331981, 135331869, 135331874, 135331875, 135331870, 135331871, 135331872, 135331873}, new int[]{135331982, 135331862, 135331867, 135331868, 135331863, 135331864, 135331865, 135331866}};
        this.mNormalRemoteViews = new RemoteViews(packageName, 134676507);
        this.mBigRemoteViews = new RemoteViews(packageName, 134676494);
        this.mData = data;
        this.mContext = context;
        this.mRequestCode = 0;
    }

    public RemoteViews getNormalRemoteViews() {
        return this.mNormalRemoteViews;
    }

    public RemoteViews getBigRemoteViews() {
        return this.mBigRemoteViews;
    }

    public void setData(List<AccountInfo> data) {
        this.mData = data;
    }

    public void configureView() {
        int itemCount;
        boolean showRowTwo;
        if (this.mData != null) {
            Log.d(TAG, "---configureView---" + this.mData.size());
            resetAccounts();
            List<AccountInfo> simAccounts = new ArrayList<>();
            for (AccountInfo account : this.mData) {
                if (account.isSimAccount()) {
                    simAccounts.add(account);
                } else {
                    this.mOtherAccounts.add(account);
                }
            }
            if (this.mOtherAccounts.size() > 0) {
                itemCount = simAccounts.size() + 1;
                showRowTwo = itemCount > 4;
            } else {
                itemCount = simAccounts.size();
                showRowTwo = itemCount > 4;
            }
            if (showRowTwo) {
                this.mBigRemoteViews.setViewVisibility(135331979, 0);
            } else {
                this.mBigRemoteViews.setViewVisibility(135331979, 8);
            }
            for (int i = 0; i < itemCount && i < 8; i++) {
                Log.d(TAG, "--- configure account id: " + i + ", mOtherAccounts.size = " + this.mOtherAccounts.size());
                if (i == itemCount - 1 && this.mOtherAccounts.size() > 0) {
                    if (this.mOtherAccounts.size() > 1) {
                        if (hasActiveAccount(this.mOtherAccounts)) {
                            AccountInfo activeAccount = getActiveAccount(this.mOtherAccounts);
                            configureAccount(this.RESOURCE_ID[i], generateOtherAccount(activeAccount));
                        } else {
                            AccountInfo otherAccount = generateOtherAccount();
                            otherAccount.setActiveStatus(false);
                            configureAccount(this.RESOURCE_ID[i], otherAccount);
                        }
                    } else if (this.mOtherAccounts.size() == 1) {
                        AccountInfo accountInfo = this.mOtherAccounts.get(0);
                        int[] resourceId = this.RESOURCE_ID[i];
                        configureAccount(resourceId, accountInfo);
                    }
                } else {
                    AccountInfo accountInfo2 = simAccounts.get(i);
                    int[] resourceId2 = this.RESOURCE_ID[i];
                    configureAccount(resourceId2, accountInfo2);
                }
            }
            return;
        }
        Log.w(TAG, "Data can not be null");
    }

    public List<AccountInfo> getOtherAccounts() {
        return this.mOtherAccounts;
    }

    private void resetAccounts() {
        for (int i = 0; i < 4; i++) {
            this.mBigRemoteViews.setViewVisibility(this.RESOURCE_ID[i][0], 8);
        }
        for (int i2 = 4; i2 < 8; i2++) {
            this.mBigRemoteViews.setViewVisibility(this.RESOURCE_ID[i2][0], 4);
        }
    }

    private boolean hasActiveAccount(List<AccountInfo> accounts) {
        for (AccountInfo account : accounts) {
            if (account.isActive()) {
                return true;
            }
        }
        return false;
    }

    private AccountInfo getActiveAccount(List<AccountInfo> accounts) {
        for (AccountInfo account : accounts) {
            if (account.isActive()) {
                return account;
            }
        }
        return null;
    }

    private AccountInfo generateOtherAccount() {
        Intent otherIntent = new Intent(DefaultAccountSelectionBar.SELECT_OTHER_ACCOUNTS_ACTION);
        String other_accounts = this.mContext.getString(134545622);
        AccountInfo otherAccount = new AccountInfo(134348804, other_accounts, (String) null, otherIntent);
        return otherAccount;
    }

    private AccountInfo generateOtherAccount(AccountInfo accountInfo) {
        Intent otherIntent = new Intent(DefaultAccountSelectionBar.SELECT_OTHER_ACCOUNTS_ACTION);
        this.mContext.getString(134545622);
        AccountInfo otherAccount = new AccountInfo(accountInfo.getIconId(), accountInfo.getIcon(), accountInfo.getLabel(), accountInfo.getNumber(), otherIntent, accountInfo.isActive(), accountInfo.isSimAccount());
        return otherAccount;
    }

    private void configureAccount(int[] resourceId, AccountInfo accountInfo) {
        if (accountInfo.getIcon() != null) {
            this.mBigRemoteViews.setViewVisibility(resourceId[0], 0);
            this.mBigRemoteViews.setImageViewBitmap(resourceId[1], accountInfo.getIcon());
        } else if (accountInfo.getIconId() != 0) {
            this.mBigRemoteViews.setViewVisibility(resourceId[0], 0);
            this.mBigRemoteViews.setImageViewResource(resourceId[1], accountInfo.getIconId());
        } else {
            Log.w(TAG, "--- The icon of account is null ---");
        }
        if (accountInfo.getLabel() == null) {
            this.mBigRemoteViews.setViewVisibility(resourceId[2], 8);
        } else {
            this.mBigRemoteViews.setViewVisibility(resourceId[2], 0);
            this.mBigRemoteViews.setTextViewText(resourceId[2], accountInfo.getLabel());
        }
        if (accountInfo.getNumber() == null) {
            this.mBigRemoteViews.setViewVisibility(resourceId[3], 8);
        } else {
            this.mBigRemoteViews.setViewVisibility(resourceId[3], 0);
            this.mBigRemoteViews.setTextViewText(resourceId[3], accountInfo.getNumber());
        }
        Log.d(TAG, "active: " + accountInfo.isActive());
        if (accountInfo.isActive()) {
            if (DefaultAccountSelectionBar.SELECT_OTHER_ACCOUNTS_ACTION.equals(accountInfo.getIntent().getAction())) {
                this.mBigRemoteViews.setViewVisibility(resourceId[7], 0);
                this.mBigRemoteViews.setViewVisibility(resourceId[5], 8);
                this.mBigRemoteViews.setViewVisibility(resourceId[4], 8);
                this.mBigRemoteViews.setViewVisibility(resourceId[6], 8);
            } else {
                this.mBigRemoteViews.setViewVisibility(resourceId[5], 0);
                this.mBigRemoteViews.setViewVisibility(resourceId[7], 8);
                this.mBigRemoteViews.setViewVisibility(resourceId[6], 8);
                this.mBigRemoteViews.setViewVisibility(resourceId[4], 8);
            }
        } else if (DefaultAccountSelectionBar.SELECT_OTHER_ACCOUNTS_ACTION.equals(accountInfo.getIntent().getAction())) {
            this.mBigRemoteViews.setViewVisibility(resourceId[6], 0);
            this.mBigRemoteViews.setViewVisibility(resourceId[4], 8);
            this.mBigRemoteViews.setViewVisibility(resourceId[7], 8);
            this.mBigRemoteViews.setViewVisibility(resourceId[5], 8);
        } else {
            this.mBigRemoteViews.setViewVisibility(resourceId[4], 0);
            this.mBigRemoteViews.setViewVisibility(resourceId[6], 8);
            this.mBigRemoteViews.setViewVisibility(resourceId[5], 8);
            this.mBigRemoteViews.setViewVisibility(resourceId[7], 8);
        }
        if (accountInfo.getIntent() == null) {
            return;
        }
        Context context = this.mContext;
        int i = this.mRequestCode;
        this.mRequestCode = i + 1;
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, i, accountInfo.getIntent(), 134217728);
        this.mBigRemoteViews.setOnClickPendingIntent(resourceId[0], pendingIntent);
    }

    public static class AccountInfo implements Parcelable {
        public static final Parcelable.Creator<AccountInfo> CREATOR = new Parcelable.Creator<AccountInfo>() {
            @Override
            public AccountInfo createFromParcel(Parcel in) {
                return new AccountInfo(in);
            }

            @Override
            public AccountInfo[] newArray(int size) {
                return new AccountInfo[size];
            }
        };
        private Bitmap mIcon;
        private int mIconId;
        private Intent mIntent;
        private boolean mIsActive;
        private boolean mIsSimAccount;
        private String mLabel;
        private String mNumber;

        public AccountInfo(Bitmap icon, String label, String number, Intent intent) {
            this(0, icon, label, number, intent, false, true);
        }

        public AccountInfo(int iconId, String label, String number, Intent intent) {
            this(iconId, null, label, number, intent, false, true);
        }

        public AccountInfo(int iconId, Bitmap icon, String label, String number, Intent intent, boolean isActive) {
            this(iconId, icon, label, number, intent, isActive, true);
        }

        public AccountInfo(int iconId, Bitmap icon, String label, String number, Intent intent, boolean isActive, boolean isSimAccount) {
            this.mIconId = iconId;
            this.mIcon = icon;
            this.mLabel = label;
            this.mNumber = number;
            this.mIntent = intent;
            this.mIsActive = isActive;
            this.mIsSimAccount = isSimAccount;
        }

        public int getIconId() {
            if (this.mIconId != 0) {
                return this.mIconId;
            }
            return 0;
        }

        public Bitmap getIcon() {
            if (this.mIcon != null) {
                return this.mIcon;
            }
            return null;
        }

        public String getLabel() {
            return this.mLabel;
        }

        public String getNumber() {
            return this.mNumber;
        }

        public void setIntent(Intent intent) {
            this.mIntent = intent;
        }

        public Intent getIntent() {
            return this.mIntent;
        }

        public boolean isActive() {
            return this.mIsActive;
        }

        public void setActiveStatus(boolean active) {
            this.mIsActive = active;
        }

        public boolean isSimAccount() {
            return this.mIsSimAccount;
        }

        public void setSimAccount(boolean isSimAccount) {
            this.mIsSimAccount = isSimAccount;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.mIconId);
            dest.writeParcelable(this.mIcon, 0);
            dest.writeString(this.mLabel);
            dest.writeString(this.mNumber);
            dest.writeParcelable(this.mIntent, 0);
            dest.writeInt(this.mIsActive ? 1 : 0);
            dest.writeInt(this.mIsSimAccount ? 1 : 0);
        }

        public AccountInfo(Parcel in) {
            ClassLoader loader = getClass().getClassLoader();
            this.mIconId = in.readInt();
            this.mIcon = (Bitmap) in.readParcelable(loader);
            this.mLabel = in.readString();
            this.mNumber = in.readString();
            this.mIntent = (Intent) in.readParcelable(loader);
            this.mIsActive = in.readInt() == 1;
            this.mIsSimAccount = in.readInt() == 1;
        }
    }
}
