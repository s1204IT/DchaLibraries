package com.android.contacts.quickcontact;

import android.content.Context;
import android.content.Intent;
import com.android.contacts.ContactSaveService;
import com.android.contacts.common.GroupMetaData;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.Contact;
import com.android.contacts.common.model.RawContact;
import com.android.contacts.common.model.RawContactDelta;
import com.android.contacts.common.model.RawContactDeltaList;
import com.android.contacts.common.model.RawContactModifier;
import com.android.contacts.common.model.ValuesDelta;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.model.dataitem.DataItem;
import com.android.contacts.common.model.dataitem.DataKind;
import com.android.contacts.common.model.dataitem.GroupMembershipDataItem;
import com.google.common.collect.Iterables;
import java.util.Iterator;
import java.util.List;

public class InvisibleContactUtil {
    public static boolean isInvisibleAndAddable(Contact contactData, Context context) {
        List<GroupMetaData> groups;
        RawContact rawContact;
        AccountType type;
        if (contactData == null || contactData.isDirectoryEntry() || contactData.isUserProfile() || contactData.getRawContacts().size() != 1 || (groups = contactData.getGroupMetaData()) == null) {
            return false;
        }
        long defaultGroupId = getDefaultGroupId(groups);
        if (defaultGroupId == -1 || (type = (rawContact = contactData.getRawContacts().get(0)).getAccountType(context)) == null || !type.areContactsWritable()) {
            return false;
        }
        boolean isInDefaultGroup = false;
        Iterator i$ = Iterables.filter(rawContact.getDataItems(), GroupMembershipDataItem.class).iterator();
        while (true) {
            if (!i$.hasNext()) {
                break;
            }
            DataItem dataItem = (DataItem) i$.next();
            GroupMembershipDataItem groupMembership = (GroupMembershipDataItem) dataItem;
            Long groupId = groupMembership.getGroupRowId();
            if (groupId != null && groupId.longValue() == defaultGroupId) {
                isInDefaultGroup = true;
                break;
            }
        }
        return isInDefaultGroup ? false : true;
    }

    public static void addToDefaultGroup(Contact contactData, Context context) {
        long defaultGroupId = getDefaultGroupId(contactData.getGroupMetaData());
        if (defaultGroupId != -1) {
            RawContactDeltaList contactDeltaList = contactData.createRawContactDeltaList();
            RawContactDelta rawContactEntityDelta = contactDeltaList.get(0);
            AccountTypeManager accountTypes = AccountTypeManager.getInstance(context);
            AccountType type = rawContactEntityDelta.getAccountType(accountTypes);
            DataKind groupMembershipKind = type.getKindForMimetype("vnd.android.cursor.item/group_membership");
            ValuesDelta entry = RawContactModifier.insertChild(rawContactEntityDelta, groupMembershipKind);
            if (entry != null) {
                entry.setGroupRowId(defaultGroupId);
                Intent intent = ContactSaveService.createSaveContactIntent(context, contactDeltaList, null, "", 0, false, QuickContactActivity.class, "android.intent.action.VIEW", null);
                context.startService(intent);
            }
        }
    }

    private static long getDefaultGroupId(List<GroupMetaData> groups) {
        long defaultGroupId = -1;
        for (GroupMetaData group : groups) {
            if (group.isDefaultGroup()) {
                if (defaultGroupId != -1) {
                    return -1L;
                }
                defaultGroupId = group.getGroupId();
            }
        }
        return defaultGroupId;
    }
}
