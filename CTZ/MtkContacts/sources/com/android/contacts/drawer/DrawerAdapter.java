package com.android.contacts.drawer;

import android.app.Activity;
import android.content.Context;
import android.graphics.PorterDuff;
import android.os.PersistableBundle;
import android.telecom.PhoneAccount;
import android.telecom.TelecomManager;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.contacts.R;
import com.android.contacts.activities.PeopleActivity;
import com.android.contacts.compat.CompatUtils;
import com.android.contacts.group.GroupListItem;
import com.android.contacts.list.ContactListFilter;
import com.android.contacts.model.account.AccountDisplayInfo;
import com.android.contacts.model.account.AccountDisplayInfoFactory;
import com.android.contacts.util.PermissionsUtil;
import com.android.contacts.util.SharedPreferenceUtil;
import com.android.contactsbind.HelpUtils;
import com.android.contactsbind.ObjectFactory;
import com.mediatek.contacts.simcontact.SubInfoUtils;
import com.mediatek.contacts.util.Log;
import com.mediatek.telephony.MtkTelephonyManagerEx;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DrawerAdapter extends BaseAdapter {
    private AccountDisplayInfoFactory mAccountDisplayFactory;
    private final Activity mActivity;
    private boolean mAreGroupWritableAccountsAvailable;
    private final LayoutInflater mInflater;
    private boolean mIsVolteConfCallEnabled;
    private ContactListFilter mSelectedAccount;
    private long mSelectedGroupId;
    private PeopleActivity.ContactsView mSelectedView;
    private NavSpacerItem mNavSpacerItem = null;
    private List<PrimaryItem> mPrimaryItems = new ArrayList();
    private HeaderItem mGroupHeader = null;
    private List<GroupEntryItem> mGroupEntries = new ArrayList();
    private BaseDrawerItem mCreateLabelButton = null;
    private HeaderItem mAccountHeader = null;
    private List<AccountEntryItem> mAccountEntries = new ArrayList();
    private List<BaseDrawerItem> mMiscItems = new ArrayList();
    private List<BaseDrawerItem> mItemsList = new ArrayList();
    private Map<Integer, Boolean> mIsVolteEnabledMap = new HashMap();

    public DrawerAdapter(Activity activity) {
        this.mInflater = LayoutInflater.from(activity);
        this.mActivity = activity;
        initVolteState();
        this.mIsVolteConfCallEnabled = isVoLTEConfCallEnable(this.mActivity);
        initializeDrawerMenuItems();
    }

    private void initializeDrawerMenuItems() {
        this.mNavSpacerItem = new NavSpacerItem(R.id.nav_drawer_spacer);
        this.mPrimaryItems.add(new PrimaryItem(R.id.nav_all_contacts, R.string.contactsList, R.drawable.quantum_ic_account_circle_vd_theme_24, PeopleActivity.ContactsView.ALL_CONTACTS));
        if (ObjectFactory.getAssistantFragment() != null) {
            this.mPrimaryItems.add(new PrimaryItem(R.id.nav_assistant, R.string.menu_assistant, R.drawable.quantum_ic_assistant_vd_theme_24, PeopleActivity.ContactsView.ASSISTANT));
        }
        this.mGroupHeader = new HeaderItem(R.id.nav_groups, R.string.menu_title_groups);
        this.mAccountHeader = new HeaderItem(R.id.nav_filters, R.string.menu_title_filters);
        this.mCreateLabelButton = new BaseDrawerItem(5, R.id.nav_create_label, R.string.menu_new_group_action_bar, R.drawable.quantum_ic_add_vd_theme_24);
        this.mMiscItems.add(new DividerItem());
        this.mMiscItems.add(new MiscItem(R.id.nav_settings, R.string.menu_settings, R.drawable.quantum_ic_settings_vd_theme_24));
        if (HelpUtils.isHelpAndFeedbackAvailable()) {
            this.mMiscItems.add(new MiscItem(R.id.nav_help, R.string.menu_help, R.drawable.quantum_ic_help_vd_theme_24));
        }
        rebuildItemsList();
    }

    private void rebuildItemsList() {
        this.mItemsList.clear();
        this.mItemsList.add(this.mNavSpacerItem);
        this.mItemsList.addAll(this.mPrimaryItems);
        if (this.mAreGroupWritableAccountsAvailable || !this.mGroupEntries.isEmpty()) {
            this.mItemsList.add(this.mGroupHeader);
        }
        this.mItemsList.addAll(this.mGroupEntries);
        if (this.mAreGroupWritableAccountsAvailable) {
            this.mItemsList.add(this.mCreateLabelButton);
        }
        if (this.mAccountEntries.size() > 0) {
            this.mItemsList.add(this.mAccountHeader);
        }
        this.mItemsList.addAll(this.mAccountEntries);
        this.mMiscItems.clear();
        this.mMiscItems.add(new DividerItem());
        if (this.mIsVolteConfCallEnabled) {
            this.mMiscItems.add(new MiscItem(R.id.nav_conf_call, R.string.menu_conference_call, R.drawable.mtk_ic_menu_conference_call));
        }
        this.mMiscItems.add(new MiscItem(R.id.nav_settings, R.string.menu_settings, R.drawable.quantum_ic_settings_vd_theme_24));
        if (HelpUtils.isHelpAndFeedbackAvailable()) {
            this.mMiscItems.add(new MiscItem(R.id.nav_help, R.string.menu_help, R.drawable.quantum_ic_help_vd_theme_24));
        }
        this.mItemsList.addAll(this.mMiscItems);
        this.mItemsList.add(this.mNavSpacerItem);
    }

    public void setGroups(List<GroupListItem> list, boolean z) {
        ArrayList arrayList = new ArrayList();
        Iterator<GroupListItem> it = list.iterator();
        while (it.hasNext()) {
            arrayList.add(new GroupEntryItem(R.id.nav_group, it.next()));
        }
        this.mGroupEntries.clear();
        this.mGroupEntries.addAll(arrayList);
        this.mAreGroupWritableAccountsAvailable = z;
        notifyChangeAndRebuildList();
    }

    public void setAccounts(List<ContactListFilter> list) {
        ArrayList arrayList = new ArrayList();
        Iterator<ContactListFilter> it = list.iterator();
        while (it.hasNext()) {
            arrayList.add(new AccountEntryItem(R.id.nav_filter, it.next()));
        }
        this.mAccountDisplayFactory = AccountDisplayInfoFactory.fromListFilters(this.mActivity, list);
        this.mAccountEntries.clear();
        this.mAccountEntries.addAll(arrayList);
        notifyChangeAndRebuildList();
    }

    @Override
    public int getCount() {
        return this.mItemsList.size();
    }

    @Override
    public BaseDrawerItem getItem(int i) {
        return this.mItemsList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return getItem(i).id;
    }

    @Override
    public int getViewTypeCount() {
        return 9;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        BaseDrawerItem item = getItem(i);
        switch (item.viewType) {
            case 0:
                return getPrimaryItemView((PrimaryItem) item, view, viewGroup);
            case 1:
                return getDrawerItemView(item, view, viewGroup);
            case 2:
                return getHeaderItemView((HeaderItem) item, view, viewGroup);
            case 3:
                return getGroupEntryView((GroupEntryItem) item, view, viewGroup);
            case CompatUtils.TYPE_ASSERT:
                return getAccountItemView((AccountEntryItem) item, view, viewGroup);
            case 5:
                return getDrawerItemView(item, view, viewGroup);
            case 6:
                return getBaseItemView(R.layout.nav_drawer_spacer, view, viewGroup);
            case 7:
                return getBaseItemView(R.layout.drawer_horizontal_divider, view, viewGroup);
            default:
                throw new IllegalStateException("Unknown drawer item " + item);
        }
    }

    private View getBaseItemView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            return this.mInflater.inflate(i, viewGroup, false);
        }
        return view;
    }

    private View getPrimaryItemView(PrimaryItem primaryItem, View view, ViewGroup viewGroup) {
        int i;
        boolean z;
        boolean z2 = false;
        if (view == null) {
            view = this.mInflater.inflate(R.layout.drawer_primary_item, viewGroup, false);
        }
        TextView textView = (TextView) view.findViewById(R.id.title);
        textView.setText(primaryItem.text);
        ImageView imageView = (ImageView) view.findViewById(R.id.icon);
        imageView.setImageResource(primaryItem.icon);
        TextView textView2 = (TextView) view.findViewById(R.id.assistant_new_badge);
        boolean z3 = !SharedPreferenceUtil.isWelcomeCardDismissed(this.mActivity);
        if (primaryItem.contactsView == PeopleActivity.ContactsView.ASSISTANT && z3) {
            i = 0;
        } else {
            i = 8;
        }
        textView2.setVisibility(i);
        if (primaryItem.contactsView != this.mSelectedView) {
            z = false;
        } else {
            z = true;
        }
        view.setActivated(z);
        if (primaryItem.contactsView == this.mSelectedView) {
            z2 = true;
        }
        updateSelectedStatus(textView, imageView, z2);
        view.setId(primaryItem.id);
        return view;
    }

    private View getHeaderItemView(HeaderItem headerItem, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = this.mInflater.inflate(R.layout.drawer_header, viewGroup, false);
        }
        ((TextView) view.findViewById(R.id.title)).setText(headerItem.text);
        view.setId(headerItem.id);
        return view;
    }

    private View getGroupEntryView(GroupEntryItem groupEntryItem, View view, ViewGroup viewGroup) {
        if (view == null || !(view.getTag() instanceof GroupEntryItem)) {
            view = this.mInflater.inflate(R.layout.drawer_item, viewGroup, false);
            view.setId(groupEntryItem.id);
        }
        GroupListItem groupListItem = groupEntryItem.group;
        TextView textView = (TextView) view.findViewById(R.id.title);
        textView.setText(groupListItem.getTitle());
        textView.setTextAlignment(5);
        ImageView imageView = (ImageView) view.findViewById(R.id.icon);
        imageView.setImageResource(R.drawable.quantum_ic_label_vd_theme_24);
        boolean z = groupListItem.getGroupId() == this.mSelectedGroupId && this.mSelectedView == PeopleActivity.ContactsView.GROUP_VIEW;
        updateSelectedStatus(textView, imageView, z);
        view.setActivated(z);
        view.setTag(groupListItem);
        view.setContentDescription(this.mActivity.getString(R.string.navigation_drawer_label, new Object[]{groupListItem.getTitle()}));
        return view;
    }

    private View getAccountItemView(AccountEntryItem accountEntryItem, View view, ViewGroup viewGroup) {
        boolean z = false;
        if (view == null || !(view.getTag() instanceof ContactListFilter)) {
            view = this.mInflater.inflate(R.layout.drawer_item, viewGroup, false);
            view.setId(accountEntryItem.id);
        }
        ContactListFilter contactListFilter = accountEntryItem.account;
        TextView textView = (TextView) view.findViewById(R.id.title);
        textView.setText(contactListFilter.accountName);
        textView.setTextAlignment(5);
        if (contactListFilter.equals(this.mSelectedAccount) && this.mSelectedView == PeopleActivity.ContactsView.ACCOUNT_VIEW) {
            z = true;
        }
        textView.setTextAppearance(this.mActivity, z ? R.style.DrawerItemTextActiveStyle : R.style.DrawerItemTextInactiveStyle);
        ImageView imageView = (ImageView) view.findViewById(R.id.icon);
        AccountDisplayInfo accountDisplayInfoFor = this.mAccountDisplayFactory.getAccountDisplayInfoFor(accountEntryItem.account);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setImageDrawable(accountDisplayInfoFor.getIcon());
        view.setTag(contactListFilter);
        view.setActivated(z);
        view.setContentDescription(((Object) accountDisplayInfoFor.getTypeLabel()) + " " + accountEntryItem.account.accountName);
        return view;
    }

    private View getDrawerItemView(BaseDrawerItem baseDrawerItem, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = this.mInflater.inflate(R.layout.drawer_item, viewGroup, false);
        }
        TextView textView = (TextView) view.findViewById(R.id.title);
        textView.setText(baseDrawerItem.text);
        if (baseDrawerItem.id == R.id.nav_conf_call) {
            textView.setTextAlignment(5);
        }
        ImageView imageView = (ImageView) view.findViewById(R.id.icon);
        imageView.setImageResource(baseDrawerItem.icon);
        view.setId(baseDrawerItem.id);
        updateSelectedStatus(textView, imageView, false);
        return view;
    }

    @Override
    public int getItemViewType(int i) {
        return getItem(i).viewType;
    }

    private void updateSelectedStatus(TextView textView, ImageView imageView, boolean z) {
        textView.setTextAppearance(this.mActivity, z ? R.style.DrawerItemTextActiveStyle : R.style.DrawerItemTextInactiveStyle);
        if (z) {
            imageView.setColorFilter(this.mActivity.getResources().getColor(R.color.primary_color), PorterDuff.Mode.SRC_ATOP);
        } else {
            imageView.clearColorFilter();
        }
    }

    private void notifyChangeAndRebuildList() {
        rebuildItemsList();
        notifyDataSetChanged();
    }

    public void setSelectedContactsView(PeopleActivity.ContactsView contactsView) {
        if (this.mSelectedView == contactsView) {
            return;
        }
        this.mSelectedView = contactsView;
        notifyChangeAndRebuildList();
    }

    public void setSelectedGroupId(long j) {
        if (this.mSelectedGroupId == j) {
            return;
        }
        this.mSelectedGroupId = j;
        notifyChangeAndRebuildList();
    }

    public long getSelectedGroupId() {
        return this.mSelectedGroupId;
    }

    public void setSelectedAccount(ContactListFilter contactListFilter) {
        if (this.mSelectedAccount == contactListFilter) {
            return;
        }
        this.mSelectedAccount = contactListFilter;
        notifyChangeAndRebuildList();
    }

    public ContactListFilter getSelectedAccount() {
        return this.mSelectedAccount;
    }

    public static class BaseDrawerItem {
        public final int icon;
        public final int id;
        public final int text;
        public final int viewType;

        public BaseDrawerItem(int i, int i2, int i3, int i4) {
            this.viewType = i;
            this.id = i2;
            this.text = i3;
            this.icon = i4;
        }
    }

    public static class PrimaryItem extends BaseDrawerItem {
        public final PeopleActivity.ContactsView contactsView;

        public PrimaryItem(int i, int i2, int i3, PeopleActivity.ContactsView contactsView) {
            super(0, i, i2, i3);
            this.contactsView = contactsView;
        }
    }

    public static class MiscItem extends BaseDrawerItem {
        public MiscItem(int i, int i2, int i3) {
            super(1, i, i2, i3);
        }
    }

    public static class HeaderItem extends BaseDrawerItem {
        public HeaderItem(int i, int i2) {
            super(2, i, i2, 0);
        }
    }

    public static class NavSpacerItem extends BaseDrawerItem {
        public NavSpacerItem(int i) {
            super(6, i, 0, 0);
        }
    }

    public static class DividerItem extends BaseDrawerItem {
        public DividerItem() {
            super(7, 0, 0, 0);
        }
    }

    public static class GroupEntryItem extends BaseDrawerItem {
        private final GroupListItem group;

        public GroupEntryItem(int i, GroupListItem groupListItem) {
            super(3, i, 0, 0);
            this.group = groupListItem;
        }
    }

    public static class AccountEntryItem extends BaseDrawerItem {
        private final ContactListFilter account;

        public AccountEntryItem(int i, ContactListFilter contactListFilter) {
            super(4, i, 0, 0);
            this.account = contactListFilter;
        }
    }

    private void initVolteState() {
        List<SubscriptionInfo> activatedSubInfoList = SubInfoUtils.getActivatedSubInfoList();
        if (activatedSubInfoList == null || activatedSubInfoList.size() <= 0) {
            Log.w("DrawerAdapter", "[initVolteState] No valid subscriptionInfoList");
            return;
        }
        if (!this.mIsVolteEnabledMap.isEmpty()) {
            this.mIsVolteEnabledMap.clear();
        }
        for (SubscriptionInfo subscriptionInfo : activatedSubInfoList) {
            if (MtkTelephonyManagerEx.getDefault().isVolteEnabled(subscriptionInfo.getSubscriptionId())) {
                this.mIsVolteEnabledMap.put(Integer.valueOf(subscriptionInfo.getSubscriptionId()), true);
                Log.d("DrawerAdapter", "[initVolteState] subId:enabled = " + subscriptionInfo.getSubscriptionId() + ":true");
            } else {
                this.mIsVolteEnabledMap.put(Integer.valueOf(subscriptionInfo.getSubscriptionId()), false);
                Log.d("DrawerAdapter", "[initVolteState] subId:enabled = " + subscriptionInfo.getSubscriptionId() + ":false");
            }
        }
    }

    public void notifySubChanged() {
        initVolteState();
        notifyConfCallStateChanged();
    }

    public boolean isVolteStateChanged(int i, boolean z) {
        if (this.mIsVolteEnabledMap.containsKey(Integer.valueOf(i)) && this.mIsVolteEnabledMap.get(Integer.valueOf(i)).booleanValue() == z) {
            return false;
        }
        this.mIsVolteEnabledMap.put(Integer.valueOf(i), Boolean.valueOf(z));
        Log.i("DrawerAdapter", "[isVolteStateChanged] volte state changed, subId:isVolteEnabled = " + i + ":" + z);
        return true;
    }

    private boolean isVoLTEConfCallEnable(Context context) {
        if (!PermissionsUtil.hasPermission(context, "android.permission.READ_PHONE_STATE")) {
            Log.e("DrawerAdapter", "[isVoLTEConfCallEnable] no READ_PHONE_STATE permission");
            return false;
        }
        TelecomManager telecomManager = (TelecomManager) context.getSystemService("telecom");
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(TelephonyManager.class);
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager) context.getSystemService("carrier_config");
        Iterator it = telecomManager.getAllPhoneAccounts().iterator();
        while (it.hasNext()) {
            int subIdForPhoneAccount = telephonyManager.getSubIdForPhoneAccount((PhoneAccount) it.next());
            if (this.mIsVolteEnabledMap.get(Integer.valueOf(subIdForPhoneAccount)) == null || !this.mIsVolteEnabledMap.get(Integer.valueOf(subIdForPhoneAccount)).booleanValue()) {
                Log.d("DrawerAdapter", "[isVoLTEConfCallEnable] volte is disabled, subId = " + subIdForPhoneAccount);
            } else {
                Log.d("DrawerAdapter", "[isVoLTEConfCallEnable] volte is enabled, subId = " + subIdForPhoneAccount);
                PersistableBundle configForSubId = carrierConfigManager.getConfigForSubId(subIdForPhoneAccount);
                if (configForSubId != null) {
                    boolean z = configForSubId.getBoolean("mtk_volte_conference_enhanced_enable_bool");
                    Log.d("DrawerAdapter", "[isVoLTEConfCallEnable] subId: " + subIdForPhoneAccount + " is " + z);
                    if (z) {
                        return true;
                    }
                } else {
                    continue;
                }
            }
        }
        return false;
    }

    public void notifyConfCallStateChanged() {
        boolean zIsVoLTEConfCallEnable = isVoLTEConfCallEnable(this.mActivity);
        Log.e("DrawerAdapter", "[notifyConfCallStateChanged] old = " + this.mIsVolteConfCallEnabled + ", enabled = " + zIsVoLTEConfCallEnable);
        if (zIsVoLTEConfCallEnable != this.mIsVolteConfCallEnabled) {
            this.mIsVolteConfCallEnabled = zIsVoLTEConfCallEnable;
            notifyChangeAndRebuildList();
        }
    }
}
