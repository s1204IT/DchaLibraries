package com.android.contacts.quickcontact;

import android.content.Context;
import com.android.contacts.ContactSaveService;
import com.android.contacts.group.GroupMetaData;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.Contact;
import com.android.contacts.model.RawContact;
import com.android.contacts.model.RawContactDelta;
import com.android.contacts.model.RawContactDeltaList;
import com.android.contacts.model.RawContactModifier;
import com.android.contacts.model.ValuesDelta;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.dataitem.DataItem;
import com.android.contacts.model.dataitem.GroupMembershipDataItem;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.Iterator;
import java.util.List;

public class InvisibleContactUtil {
    public static boolean isInvisibleAndAddable(Contact contact, Context context) {
        ImmutableList<GroupMetaData> groupMetaData;
        RawContact rawContact;
        AccountType accountType;
        boolean z = false;
        if (contact == null || contact.isDirectoryEntry() || contact.isUserProfile() || contact.getRawContacts().size() != 1 || (groupMetaData = contact.getGroupMetaData()) == null) {
            return false;
        }
        long defaultGroupId = getDefaultGroupId(groupMetaData);
        if (defaultGroupId == -1 || (accountType = (rawContact = contact.getRawContacts().get(0)).getAccountType(context)) == null || !accountType.areContactsWritable()) {
            return false;
        }
        Iterator it = Iterables.filter(rawContact.getDataItems(), GroupMembershipDataItem.class).iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            Long groupRowId = ((GroupMembershipDataItem) ((DataItem) it.next())).getGroupRowId();
            if (groupRowId != null && groupRowId.longValue() == defaultGroupId) {
                z = true;
                break;
            }
        }
        return !z;
    }

    public static void addToDefaultGroup(Contact contact, Context context) {
        RawContactDeltaList rawContactDeltaListCreateRawContactDeltaList = contact.createRawContactDeltaList();
        if (markAddToDefaultGroup(contact, rawContactDeltaListCreateRawContactDeltaList, context)) {
            ContactSaveService.startService(context, ContactSaveService.createSaveContactIntent(context, rawContactDeltaListCreateRawContactDeltaList, "", 0, false, QuickContactActivity.class, "android.intent.action.VIEW", null, null, null));
        }
    }

    public static boolean markAddToDefaultGroup(Contact contact, RawContactDeltaList rawContactDeltaList, Context context) {
        long defaultGroupId = getDefaultGroupId(contact.getGroupMetaData());
        if (defaultGroupId == -1) {
            return false;
        }
        RawContactDelta rawContactDelta = rawContactDeltaList.get(0);
        ValuesDelta valuesDeltaInsertChild = RawContactModifier.insertChild(rawContactDelta, rawContactDelta.getAccountType(AccountTypeManager.getInstance(context)).getKindForMimetype("vnd.android.cursor.item/group_membership"));
        if (valuesDeltaInsertChild == null) {
            return false;
        }
        valuesDeltaInsertChild.setGroupRowId(defaultGroupId);
        return true;
    }

    private static long getDefaultGroupId(List<GroupMetaData> list) {
        long j = -1;
        for (GroupMetaData groupMetaData : list) {
            if (groupMetaData.defaultGroup) {
                if (j != -1) {
                    return -1L;
                }
                j = groupMetaData.groupId;
            }
        }
        return j;
    }
}
