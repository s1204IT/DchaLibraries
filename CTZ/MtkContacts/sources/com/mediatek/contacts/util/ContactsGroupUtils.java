package com.mediatek.contacts.util;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.text.TextUtils;
import com.android.contacts.R;
import com.mediatek.contacts.simcontact.PhbInfoUtils;
import com.mediatek.contacts.simcontact.SubInfoUtils;
import com.mediatek.internal.telephony.phb.IMtkIccPhoneBook;
import com.mediatek.internal.telephony.phb.UsimGroup;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class ContactsGroupUtils {
    public static IMtkIccPhoneBook getIMtkIccPhoneBook() {
        return IMtkIccPhoneBook.Stub.asInterface(ServiceManager.getService(SubInfoUtils.getMtkPhoneBookServiceName()));
    }

    public static final class USIMGroup {
        private static final HashMap<Integer, ArrayList<UsimGroup>> UGRP_LISTARRAY = new HashMap<Integer, ArrayList<UsimGroup>>() {
            @Override
            public ArrayList<UsimGroup> get(Object obj) {
                Integer num = (Integer) obj;
                if (super.get((Object) num) == null) {
                    put(num, new ArrayList());
                }
                return (ArrayList) super.get(obj);
            }
        };

        public static void addGroupItemToLocal(int i, UsimGroup usimGroup) {
            UGRP_LISTARRAY.get(Integer.valueOf(i)).add(usimGroup);
            Log.d("ContactsGroupUtils.USIMGroup", "[addGroupItemToLocal]: usimGroup: " + Log.anonymize(usimGroup));
        }

        public static boolean updateLocalGroupName(int i, int i2, String str) {
            UsimGroup usimGroup = null;
            for (UsimGroup usimGroup2 : UGRP_LISTARRAY.get(Integer.valueOf(i))) {
                if (usimGroup2.getRecordIndex() == i2) {
                    usimGroup = usimGroup2;
                }
            }
            StringBuilder sb = new StringBuilder();
            sb.append("[updateLocalGroupName]needUpateGroup is null = ");
            sb.append(usimGroup == null);
            Log.d("ContactsGroupUtils.USIMGroup", sb.toString());
            if (usimGroup == null) {
                return false;
            }
            usimGroup.setAlphaTag(str);
            return true;
        }

        public static boolean removeLocalGroupItem(int i, int i2) {
            ArrayList<UsimGroup> arrayList = UGRP_LISTARRAY.get(Integer.valueOf(i));
            UsimGroup usimGroup = null;
            for (UsimGroup usimGroup2 : arrayList) {
                if (usimGroup2.getRecordIndex() == i2) {
                    usimGroup = usimGroup2;
                }
            }
            Log.d("ContactsGroupUtils.USIMGroup", "[removeLocalGroupItem]: dirtyGroup: " + Log.anonymize(usimGroup));
            if (usimGroup != null) {
                arrayList.remove(usimGroup);
                return true;
            }
            return false;
        }

        public static int hasExistGroup(int i, String str) throws RemoteException {
            IMtkIccPhoneBook iMtkIccPhoneBook = ContactsGroupUtils.getIMtkIccPhoneBook();
            Log.d("ContactsGroupUtils.USIMGroup", "[hasExistGroup]grpName:" + Log.anonymize(str) + "|iIccPhb:" + iMtkIccPhoneBook);
            int recordIndex = -1;
            if (TextUtils.isEmpty(str) || iMtkIccPhoneBook == null) {
                return -1;
            }
            ArrayList<UsimGroup> arrayList = UGRP_LISTARRAY.get(Integer.valueOf(i));
            if (arrayList.isEmpty()) {
                List<UsimGroup> usimGroups = iMtkIccPhoneBook.getUsimGroups(i);
                if (usimGroups == null) {
                    Log.w("ContactsGroupUtils.USIMGroup", "[hasExistGroup] can't get usimGroupList from iIccPhb!");
                    return -1;
                }
                for (UsimGroup usimGroup : usimGroups) {
                    String alphaTag = usimGroup.getAlphaTag();
                    int recordIndex2 = usimGroup.getRecordIndex();
                    if (!TextUtils.isEmpty(alphaTag) && recordIndex2 > 0) {
                        arrayList.add(new UsimGroup(recordIndex2, alphaTag));
                        if (alphaTag.equals(str)) {
                            recordIndex = recordIndex2;
                        }
                    }
                }
            } else {
                Iterator<UsimGroup> it = arrayList.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    UsimGroup next = it.next();
                    if (str.equals(next.getAlphaTag())) {
                        recordIndex = next.getRecordIndex();
                        break;
                    }
                }
            }
            Log.d("ContactsGroupUtils.USIMGroup", "[hasExistGroup]grpId:" + recordIndex);
            return recordIndex;
        }

        public static int syncUSIMGroupNewIfMissing(int i, String str) throws RemoteException, USIMGroupException {
            int length;
            Log.i("ContactsGroupUtils.USIMGroup", "[syncUSIMGroupNewIfMissing]name:" + Log.anonymize(str) + ",subId:" + i);
            if (TextUtils.isEmpty(str)) {
                Log.w("ContactsGroupUtils.USIMGroup", "[syncUSIMGroupNewIfMissing]name is null,return.");
                return -1;
            }
            try {
                length = str.getBytes("GBK").length;
            } catch (UnsupportedEncodingException e) {
                Log.w("ContactsGroupUtils.USIMGroup", "[syncUSIMGroupNewIfMissing]UnsupportedEncodingException:" + e);
                length = str.length();
            }
            int usimGroupMaxNameLength = PhbInfoUtils.getUsimGroupMaxNameLength(i);
            Log.d("ContactsGroupUtils.USIMGroup", "[syncUSIMGroupNewIfMissing]nameLen:" + length + " ||usimGrpMaxNameLen:" + usimGroupMaxNameLength);
            if (usimGroupMaxNameLength == -1) {
                Log.e("ContactsGroupUtils.USIMGroup", "[syncUSIMGroupNewIfMissing]nameLen:" + length + " ||getUSIMGrpMaxNameLen(subId) is -1.");
                throw new USIMGroupException("Group generic error", 3, i);
            }
            if (length > usimGroupMaxNameLength) {
                throw new USIMGroupException("Group name out of bound", 1, i);
            }
            IMtkIccPhoneBook iMtkIccPhoneBook = ContactsGroupUtils.getIMtkIccPhoneBook();
            int iHasExistGroup = hasExistGroup(i, str);
            if (iHasExistGroup < 1 && iMtkIccPhoneBook != null) {
                iHasExistGroup = iMtkIccPhoneBook.insertUsimGroup(i, str);
                Log.i("ContactsGroupUtils.USIMGroup", "[syncUSIMGroupNewIfMissing]inserted grpId:" + iHasExistGroup);
                if (iHasExistGroup > 0) {
                    addGroupItemToLocal(i, new UsimGroup(iHasExistGroup, str));
                }
            }
            Log.d("ContactsGroupUtils.USIMGroup", "[syncUSIMGroupNewIfMissing]grpId:" + iHasExistGroup);
            if (iHasExistGroup < 1) {
                if (iHasExistGroup == -20) {
                    throw new USIMGroupException("Group count out of bound", 2, i);
                }
                if (iHasExistGroup == -10) {
                    throw new USIMGroupException("Group name out of bound", 1, i);
                }
                throw new USIMGroupException("Group generic error", 3, i);
            }
            return iHasExistGroup;
        }

        public static int syncUSIMGroupUpdate(int i, String str, String str2) throws RemoteException, USIMGroupException {
            int length;
            IMtkIccPhoneBook iMtkIccPhoneBook = ContactsGroupUtils.getIMtkIccPhoneBook();
            int iHasExistGroup = hasExistGroup(i, str);
            Log.d("ContactsGroupUtils.USIMGroup", "[syncUSIMGroupUpdate]grpId:" + iHasExistGroup + "|subId:" + i + "|oldName:" + Log.anonymize(str) + "|newName:" + Log.anonymize(str2));
            if (iHasExistGroup > 0) {
                try {
                    if (!TextUtils.isEmpty(str2)) {
                        length = str2.getBytes("GBK").length;
                    } else {
                        return iHasExistGroup;
                    }
                } catch (UnsupportedEncodingException e) {
                    Log.w("ContactsGroupUtils.USIMGroup", "[syncUSIMGroupUpdate]UnsupportedEncodingException:" + e);
                    length = str2.length();
                }
                if (length > PhbInfoUtils.getUsimGroupMaxNameLength(i)) {
                    Log.e("ContactsGroupUtils.USIMGroup", "[syncUSIMGroupUpdate]nameLength:" + length + ",getUsimGroupMaxNameLength(subId):" + PhbInfoUtils.getUsimGroupMaxNameLength(i));
                    throw new USIMGroupException("Group name out of bound", 1, i);
                }
                int iUpdateUsimGroup = iMtkIccPhoneBook.updateUsimGroup(i, iHasExistGroup, str2);
                Log.d("ContactsGroupUtils.USIMGroup", "[syncUSIMGroupUpdate]updateUsimGroup ret:" + iUpdateUsimGroup);
                if (iUpdateUsimGroup < 0) {
                    if (iUpdateUsimGroup == -20) {
                        throw new USIMGroupException("Group count out of bound", 2, i);
                    }
                    if (iUpdateUsimGroup == -10) {
                        throw new USIMGroupException("Group name out of bound", 1, i);
                    }
                    throw new USIMGroupException("Group generic error", 3, i);
                }
                updateLocalGroupName(i, iHasExistGroup, str2);
            }
            return iHasExistGroup;
        }

        public static int deleteUSIMGroup(int i, String str) {
            int i2;
            IMtkIccPhoneBook iMtkIccPhoneBook = ContactsGroupUtils.getIMtkIccPhoneBook();
            int i3 = -2;
            try {
                int iHasExistGroup = hasExistGroup(i, str);
                if (iHasExistGroup > 0) {
                    if (iMtkIccPhoneBook.removeUsimGroupById(i, iHasExistGroup)) {
                        removeLocalGroupItem(i, iHasExistGroup);
                        i2 = 0;
                    } else {
                        i2 = -1;
                    }
                    i3 = i2;
                }
            } catch (RemoteException e) {
                Log.e("ContactsGroupUtils.USIMGroup", "[deleteUSIMGroup]RemoteException:" + e);
            }
            Log.d("ContactsGroupUtils.USIMGroup", "[deleteUSIMGroup]errCode:" + i3);
            return i3;
        }

        public static boolean addUSIMGroupMember(int i, int i2, int i3) {
            boolean zAddContactToGroup = false;
            if (i3 > 0) {
                try {
                    IMtkIccPhoneBook iMtkIccPhoneBook = ContactsGroupUtils.getIMtkIccPhoneBook();
                    if (iMtkIccPhoneBook != null) {
                        zAddContactToGroup = iMtkIccPhoneBook.addContactToGroup(i, i2, i3);
                    }
                } catch (RemoteException e) {
                    Log.e("ContactsGroupUtils.USIMGroup", "[deleteUSIMGroup]RemoteException:" + e);
                }
            }
            Log.i("ContactsGroupUtils.USIMGroup", "[addUSIMGroupMember]succFlag:" + zAddContactToGroup);
            return zAddContactToGroup;
        }

        public static boolean deleteUSIMGroupMember(int i, int i2, int i3) {
            Log.d("ContactsGroupUtils.USIMGroup", "[deleteUSIMGroupMember]subId:" + i + "|simIndex:" + i2 + "|grpId:" + i3);
            boolean zRemoveContactFromGroup = false;
            if (i3 > 0) {
                try {
                    IMtkIccPhoneBook iMtkIccPhoneBook = ContactsGroupUtils.getIMtkIccPhoneBook();
                    if (iMtkIccPhoneBook != null) {
                        zRemoveContactFromGroup = iMtkIccPhoneBook.removeContactFromGroup(i, i2, i3);
                    }
                } catch (RemoteException e) {
                    Log.e("ContactsGroupUtils.USIMGroup", "[deleteUSIMGroup]RemoteException:" + e);
                }
            }
            Log.d("ContactsGroupUtils.USIMGroup", "[deleteUSIMGroupMember]result:" + zRemoveContactFromGroup + ",subId:" + i + ",simIndex:" + i2 + ",grpId:" + i3);
            return zRemoveContactFromGroup;
        }
    }

    public static class USIMGroupException extends Exception {
        private static final long serialVersionUID = 1;
        int mErrorType;
        int mSubId;

        public USIMGroupException(String str, int i, int i2) {
            super(str);
            this.mErrorType = i;
            this.mSubId = i2;
        }

        public int getErrorType() {
            return this.mErrorType;
        }

        public int getErrorSubId() {
            return this.mSubId;
        }

        @Override
        public String getMessage() {
            return "Details message: errorType:" + this.mErrorType + "\n" + super.getMessage();
        }

        public static int getErrorToastId(int i) {
            if (i != 4) {
                switch (i) {
                    case 1:
                        return R.string.usim_group_name_exceed_limit;
                    case 2:
                        return R.string.usim_group_count_exceed_limit;
                    default:
                        return R.string.generic_failure;
                }
            }
            return R.string.callFailed_simError;
        }
    }

    public static class ContactsGroupArrayData implements Parcelable {
        public static final Parcelable.Creator<ContactsGroupArrayData> CREATOR = new Parcelable.Creator<ContactsGroupArrayData>() {
            @Override
            public ContactsGroupArrayData createFromParcel(Parcel parcel) {
                return new ContactsGroupArrayData(parcel);
            }

            @Override
            public ContactsGroupArrayData[] newArray(int i) {
                return new ContactsGroupArrayData[i];
            }
        };
        private int mSimIndex;
        private int mSimIndexPhoneOrSim;

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.mSimIndex);
            parcel.writeInt(this.mSimIndexPhoneOrSim);
        }

        public ContactsGroupArrayData(Parcel parcel) {
            this.mSimIndex = parcel.readInt();
            this.mSimIndexPhoneOrSim = parcel.readInt();
        }
    }

    public static class ParcelableHashMap implements Parcelable {
        public static final Parcelable.Creator<ParcelableHashMap> CREATOR = new Parcelable.Creator<ParcelableHashMap>() {
            @Override
            public ParcelableHashMap createFromParcel(Parcel parcel) {
                return new ParcelableHashMap(parcel);
            }

            @Override
            public ParcelableHashMap[] newArray(int i) {
                return new ParcelableHashMap[i];
            }
        };
        private HashMap<Long, ContactsGroupArrayData> mMap;

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeMap(this.mMap);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public ParcelableHashMap(Parcel parcel) {
            this.mMap = parcel.readHashMap(ParcelableHashMap.class.getClassLoader());
        }
    }
}
