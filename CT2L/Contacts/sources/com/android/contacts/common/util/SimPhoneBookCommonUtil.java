package com.android.contacts.common.util;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.ContactsContract;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.internal.telephony.IIccPhoneBook;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimPhoneBookCommonUtil {
    private static TelephonyManager mTelMan;
    private static IIccPhoneBook simPhoneBook;
    private static int mCurrentSimState = -1;
    public static int GET_ADN_MODE1 = 1;
    public static int GET_ADN_MODE2 = 2;
    private static boolean isFirstCheck = true;
    private static final Uri ADN_URI = Uri.parse("content://icc/adn");
    private static final Uri ADN_URI_SUB = Uri.parse("content://icc/adn/subId/");

    public static class AdnRecord {
        public String name = "";
        public String name_sne = "";
        public String phone = "";
        public String phone2 = "";
        public String email = "";
        public String email2 = "";
        public ArrayList<String> phones = new ArrayList<>();
        public ArrayList<String> emails = new ArrayList<>();

        public boolean isEmpty() {
            return this.name.isEmpty() && this.name_sne.isEmpty() && this.phone.isEmpty() && this.phone2.isEmpty() && this.email.isEmpty() && this.email2.isEmpty();
        }

        public String toString() {
            StringBuilder output = new StringBuilder("ADN Record: \n");
            output.append("name = " + this.name + "\n");
            output.append("name_sne = " + this.name_sne + "\n");
            output.append("phone = " + this.phone + "\n");
            output.append("phone2 = " + this.phone2 + "\n");
            output.append("email = " + this.email + "\n");
            output.append("email2 = " + this.email2 + "\n");
            return output.toString();
        }
    }

    public static int getPBInitCount(int slotId) {
        IIccPhoneBook iccIpb = IIccPhoneBook.Stub.asInterface(ServiceManager.getService("simphonebook"));
        int[] subs = SubscriptionManager.getSubId(slotId);
        if (subs == null || iccIpb == null) {
            return -1;
        }
        try {
            int ret = iccIpb.getPBInitCountUsingSubId(subs[0]);
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static boolean isSimContactsLoaded(int slotId) {
        IIccPhoneBook iccIpb = IIccPhoneBook.Stub.asInterface(ServiceManager.getService("simphonebook"));
        int[] subs = SubscriptionManager.getSubId(slotId);
        if (subs == null || iccIpb == null) {
            return false;
        }
        try {
            boolean ret = iccIpb.isSimContactsLoadedUsingSubId(subs[0]);
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isSimEnabled(int slotId) {
        mTelMan = TelephonyManager.getDefault();
        int simState = mTelMan.getSimState(slotId);
        if (simState == 0 || simState == 1) {
            mCurrentSimState = simState;
            Log.d("SimPhoneBookCommonUtil", "isSimEnabled() return false");
            return false;
        }
        if (mCurrentSimState == simState) {
            return true;
        }
        isFirstCheck = true;
        mCurrentSimState = simState;
        return true;
    }

    public static int getEmailFieldSize() {
        simPhoneBook = IIccPhoneBook.Stub.asInterface(ServiceManager.getService("simphonebook"));
        try {
            if (simPhoneBook == null) {
                return 0;
            }
            int ret = simPhoneBook.getEmailFieldSize();
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static boolean isSneFieldEnable() {
        return isSneFieldEnable(0);
    }

    public static boolean isSneFieldEnable(int slotId) {
        IIccPhoneBook iccIpb = IIccPhoneBook.Stub.asInterface(ServiceManager.getService("simphonebook"));
        int[] subs = SubscriptionManager.getSubId(slotId);
        if (subs == null || iccIpb == null) {
            return false;
        }
        try {
            boolean ret = iccIpb.isSneFieldEnableUsingSubId(subs[0]);
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String getSimGrpStringViaRawId(ContentResolver resolver, long rawContactId) {
        String grpString = "";
        Cursor c = resolver.query(ContactsContract.Data.CONTENT_URI, new String[]{"data1", "mimetype"}, "raw_contact_id=" + rawContactId, null, null);
        if (c != null) {
            while (c.moveToNext()) {
                if (c.getString(1).equals("vnd.android.cursor.item/group_membership")) {
                    int group_id = c.getInt(0);
                    Cursor gc = resolver.query(ContactsContract.Groups.CONTENT_URI, new String[]{"sourceid"}, "_id=" + group_id, null, null);
                    if (gc.moveToFirst()) {
                        grpString = grpString + gc.getString(0) + ",";
                    }
                }
            }
            c.close();
        }
        return grpString;
    }

    public static ArrayList<ContentProviderOperation> setUpOperationListSim2Phone(ContentResolver resolver, ArrayList<ContentProviderOperation> operationList, String accountName, String accountType, String name, String number, String[] emails, String anr, String sne, String grpString) {
        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI);
        builder.withValue("account_name", accountName);
        builder.withValue("account_type", accountType);
        builder.withValue("aggregation_mode", 3);
        operationList.add(builder.build());
        ContentProviderOperation.Builder builder2 = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
        builder2.withValueBackReference("raw_contact_id", 0);
        builder2.withValue("mimetype", "vnd.android.cursor.item/name");
        builder2.withValue("data2", name);
        operationList.add(builder2.build());
        if (sne != null && !sne.isEmpty()) {
            ContentProviderOperation.Builder builder3 = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
            builder3.withValueBackReference("raw_contact_id", 0);
            builder3.withValue("mimetype", "vnd.android.cursor.item/nickname");
            builder3.withValue("data1", sne);
            operationList.add(builder3.build());
        }
        ContentProviderOperation.Builder builder4 = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
        builder4.withValueBackReference("raw_contact_id", 0);
        builder4.withValue("mimetype", "vnd.android.cursor.item/phone_v2");
        builder4.withValue("data2", 2);
        builder4.withValue("data1", number);
        builder4.withValue("is_primary", 1);
        operationList.add(builder4.build());
        if (emails[0] != null && !emails[0].isEmpty()) {
            ContentProviderOperation.Builder builder5 = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
            builder5.withValueBackReference("raw_contact_id", 0);
            builder5.withValue("mimetype", "vnd.android.cursor.item/email_v2");
            builder5.withValue("data1", emails[0]);
            builder5.withValue("data2", 4);
            operationList.add(builder5.build());
        }
        if (emails.length > 1 && emails[1] != null && !emails[1].isEmpty()) {
            ContentProviderOperation.Builder builder6 = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
            builder6.withValueBackReference("raw_contact_id", 0);
            builder6.withValue("mimetype", "vnd.android.cursor.item/email_v2");
            builder6.withValue("data1", emails[1]);
            builder6.withValue("data2", 3);
            operationList.add(builder6.build());
        }
        if (anr != null && !anr.isEmpty()) {
            ContentProviderOperation.Builder builder7 = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
            builder7.withValueBackReference("raw_contact_id", 0);
            builder7.withValue("mimetype", "vnd.android.cursor.item/phone_v2");
            builder7.withValue("data2", 1);
            builder7.withValue("data1", anr);
            builder7.withValue("is_primary", 0);
            operationList.add(builder7.build());
        }
        if (grpString != null && !grpString.isEmpty()) {
            String[] grps = grpString.split(",");
            int groupId = 0;
            for (String grp : grps) {
                Log.d("SimPhoneBookCommonUtil", "grp = " + grp);
                if (!grp.isEmpty()) {
                    Cursor c = resolver.query(ContactsContract.Groups.CONTENT_URI, new String[]{"_id"}, "account_name=? AND sourceid=?", new String[]{accountName, grp}, null);
                    if (c != null) {
                        if (c.moveToFirst()) {
                            Log.d("SimPhoneBookCommonUtil", "----> groupId one = " + c.getString(0));
                            groupId = c.getInt(0);
                        }
                        c.close();
                    }
                    Log.d("SimPhoneBookCommonUtil", "groupId = " + groupId);
                    ContentProviderOperation.Builder builder8 = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
                    builder8.withValueBackReference("raw_contact_id", 0);
                    builder8.withValue("mimetype", "vnd.android.cursor.item/group_membership");
                    builder8.withValue("data1", Integer.valueOf(groupId));
                    operationList.add(builder8.build());
                }
            }
        }
        return operationList;
    }

    public static ArrayList<ContentProviderOperation> setUpOperationListSim2Phone(ContentResolver resolver, ArrayList<ContentProviderOperation> operationList, String accountName, String accountType, String name, String number, String[] emails, String anr, String sne, String grpString, int previousResult) {
        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI);
        builder.withValue("account_name", accountName);
        builder.withValue("account_type", accountType);
        builder.withValue("aggregation_mode", 3);
        operationList.add(builder.build());
        ContentProviderOperation.Builder builder2 = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
        builder2.withValueBackReference("raw_contact_id", previousResult);
        builder2.withValue("mimetype", "vnd.android.cursor.item/name");
        builder2.withValue("data2", name);
        operationList.add(builder2.build());
        if (sne != null && !sne.isEmpty()) {
            ContentProviderOperation.Builder builder3 = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
            builder3.withValueBackReference("raw_contact_id", previousResult);
            builder3.withValue("mimetype", "vnd.android.cursor.item/nickname");
            builder3.withValue("data1", sne);
            operationList.add(builder3.build());
        }
        ContentProviderOperation.Builder builder4 = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
        builder4.withValueBackReference("raw_contact_id", previousResult);
        builder4.withValue("mimetype", "vnd.android.cursor.item/phone_v2");
        builder4.withValue("data2", 2);
        builder4.withValue("data1", number);
        builder4.withValue("is_primary", 1);
        operationList.add(builder4.build());
        if (emails[0] != null && !emails[0].isEmpty()) {
            ContentProviderOperation.Builder builder5 = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
            builder5.withValueBackReference("raw_contact_id", previousResult);
            builder5.withValue("mimetype", "vnd.android.cursor.item/email_v2");
            builder5.withValue("data1", emails[0]);
            builder5.withValue("data2", 4);
            operationList.add(builder5.build());
        }
        if (emails.length > 1 && emails[1] != null && !emails[1].isEmpty()) {
            ContentProviderOperation.Builder builder6 = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
            builder6.withValueBackReference("raw_contact_id", previousResult);
            builder6.withValue("mimetype", "vnd.android.cursor.item/email_v2");
            builder6.withValue("data1", emails[1]);
            builder6.withValue("data2", 3);
            operationList.add(builder6.build());
        }
        if (anr != null && !anr.isEmpty()) {
            ContentProviderOperation.Builder builder7 = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
            builder7.withValueBackReference("raw_contact_id", previousResult);
            builder7.withValue("mimetype", "vnd.android.cursor.item/phone_v2");
            builder7.withValue("data2", 1);
            builder7.withValue("data1", anr);
            builder7.withValue("is_primary", 0);
            operationList.add(builder7.build());
        }
        if (grpString != null && !grpString.isEmpty()) {
            String[] grps = grpString.split(",");
            int groupId = 0;
            for (String grp : grps) {
                Log.d("SimPhoneBookCommonUtil", "grp = " + grp);
                if (!grp.isEmpty()) {
                    Cursor c = resolver.query(ContactsContract.Groups.CONTENT_URI, new String[]{"_id"}, "account_name=? AND sourceid=?", new String[]{accountName, grp}, null);
                    if (c != null) {
                        if (c.moveToFirst()) {
                            Log.d("SimPhoneBookCommonUtil", "----> groupId one = " + c.getString(0));
                            groupId = c.getInt(0);
                        }
                        c.close();
                    }
                    Log.d("SimPhoneBookCommonUtil", "groupId = " + groupId);
                    ContentProviderOperation.Builder builder8 = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
                    builder8.withValueBackReference("raw_contact_id", previousResult);
                    builder8.withValue("mimetype", "vnd.android.cursor.item/group_membership");
                    builder8.withValue("data1", Integer.valueOf(groupId));
                    operationList.add(builder8.build());
                }
            }
        }
        return operationList;
    }

    public static boolean updateSimContact(int slotId, ContentResolver cr, String tag, String number, String email, String number2, String sne, String group, String newTag, String newNumber, String newEmails, String newNumber2, String newSne, String newgroup) {
        int[] subs = SubscriptionManager.getSubId(slotId);
        if (subs == null) {
            return false;
        }
        Uri adnUri = Uri.parse("content://icc/adn/subId/" + subs[0]);
        ContentValues values = new ContentValues();
        if (newTag == null) {
            newTag = "";
        }
        if (newNumber == null) {
            newNumber = "";
        }
        if (newEmails == null) {
            newEmails = "";
        }
        if (newNumber2 == null) {
            newNumber2 = "";
        }
        if (newSne == null) {
            newSne = "";
        }
        if (newgroup == null) {
            newgroup = "";
        }
        if (tag == null) {
            tag = "";
        }
        if (number == null) {
            number = "";
        }
        if (email == null) {
            email = "";
        }
        if (number2 == null) {
            number2 = "";
        }
        if (sne == null) {
            sne = "";
        }
        if (group == null) {
            group = "";
        }
        String newNumber3 = newNumber.replaceAll("( |-)", "");
        String newNumber22 = newNumber2.replaceAll("( |-)", "");
        String number3 = number.replaceAll("( |-)", "");
        String number22 = number2.replaceAll("( |-)", "");
        if (!isPatternMatch("^[0-9\\*#\\+\\.,;]{0,40}$", newNumber3) || !isPatternMatch("^[0-9\\*#\\+\\.,;]{0,40}$", newNumber22)) {
            Log.d("SimPhoneBookCommonUtil", "updateSimContact newNumber Pattern mismatch");
            return false;
        }
        values.put("tag", tag);
        values.put("number", number3);
        values.put("emails", email);
        values.put("number2", number22);
        values.put("sne", sne);
        values.put("grps", group);
        values.put("newTag", newTag);
        values.put("newNumber", newNumber3);
        values.put("newEmails", newEmails);
        values.put("newNumber2", newNumber22);
        values.put("newSne", newSne);
        values.put("newGrps", newgroup);
        int num = cr.update(adnUri, values, null, null);
        Log.d("SimPhoneBookCommonUtil", "updateSimContact -------> num = " + num);
        return num > 0;
    }

    public static boolean isPatternMatch(String patternString, String str) {
        if (str.isEmpty()) {
            return true;
        }
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(str);
        return matcher.matches();
    }

    public static boolean isOutOfSpace(int slotId) {
        IIccPhoneBook iccIpb = IIccPhoneBook.Stub.asInterface(ServiceManager.getService("simphonebook"));
        int[] subs = SubscriptionManager.getSubId(slotId);
        if (subs == null) {
            return false;
        }
        try {
            List<com.android.internal.telephony.uicc.AdnRecord> adnRecords = iccIpb.getAdnRecordsInEfForSubscriber(subs[0], 28474);
            int available = 0;
            if (adnRecords != null) {
                int totalSize = adnRecords.size();
                int i = 0;
                while (true) {
                    if (i >= totalSize) {
                        break;
                    }
                    if (!adnRecords.get(i).isEmpty()) {
                        i++;
                    } else {
                        available = 0 + 1;
                        break;
                    }
                }
            } else {
                Log.w("SimPhoneBookCommonUtil", "Cannot load ADN records");
            }
            return available <= 0;
        } catch (RemoteException e) {
            return false;
        } catch (SecurityException e2) {
            return false;
        }
    }

    public static AdnRecord getSimAdnViaRawId(ContentResolver resolver, long rawContactId, int mode) {
        Log.d("SimPhoneBookCommonUtil", "getSimAdnViaRawId() ");
        AdnRecord adnRec = new AdnRecord();
        Cursor c = resolver.query(ContactsContract.Data.CONTENT_URI, new String[]{"data1", "mimetype", "data2", "data6"}, "raw_contact_id=" + rawContactId, null, null);
        if (c != null) {
            if (mode == GET_ADN_MODE1) {
                while (c.moveToNext()) {
                    Log.v("SimPhoneBookCommonUtil", "----> c.getInt(1) = " + c.getString(1));
                    if (c.getString(1).equals("vnd.android.cursor.item/name")) {
                        adnRec.name = c.getString(0) == null ? "" : c.getString(0);
                    } else if (c.getString(1).equals("vnd.android.cursor.item/nickname")) {
                        adnRec.name_sne = c.getString(0) == null ? "" : c.getString(0);
                    } else if (c.getString(1).equals("vnd.android.cursor.item/phone_v2")) {
                        if (c.getInt(2) == 2 && c.getString(0) != null) {
                            adnRec.phone = c.getString(0);
                        } else if (c.getInt(2) == 1 && c.getString(0) != null) {
                            if (adnRec.phone.isEmpty()) {
                                adnRec.phone = c.getString(0);
                            } else if (adnRec.phone2.isEmpty()) {
                                adnRec.phone2 = c.getString(0);
                            }
                        }
                    } else if (c.getString(1).equals("vnd.android.cursor.item/email_v2")) {
                        if (c.getInt(2) == 4 && c.getString(0) != null) {
                            adnRec.email = c.getString(0);
                        } else if (c.getInt(2) == 3 && c.getString(0) != null) {
                            adnRec.email2 = c.getString(0);
                        }
                    }
                }
            } else if (mode == GET_ADN_MODE2) {
                while (c.moveToNext()) {
                    Log.v("SimPhoneBookCommonUtil", "----> c.getInt(1) = " + c.getString(1));
                    if (c.getString(1).equals("vnd.android.cursor.item/name")) {
                        adnRec.name = c.getString(0) == null ? "" : c.getString(0);
                    } else if (c.getString(1).equals("vnd.android.cursor.item/nickname") && isSneFieldEnable()) {
                        adnRec.name_sne = c.getString(0) == null ? "" : c.getString(0);
                    } else if (c.getString(1).equals("vnd.android.cursor.item/phone_v2")) {
                        if (c.getString(0) != null) {
                            adnRec.phones.add(c.getString(0));
                        }
                        if (c.getInt(2) == 2 && c.getString(0) != null) {
                            if (!adnRec.phone.isEmpty()) {
                                adnRec.phone2 = adnRec.phone;
                            }
                            adnRec.phone = c.getString(0);
                        } else if (c.getString(0) != null) {
                            if (adnRec.phone.isEmpty()) {
                                adnRec.phone = c.getString(0);
                            } else if (adnRec.phone2.isEmpty()) {
                                adnRec.phone2 = c.getString(0);
                            }
                        }
                    } else if (c.getString(1).equals("vnd.android.cursor.item/email_v2") && c.getString(0) != null) {
                        switch (getEmailFieldSize()) {
                            case 1:
                                adnRec.emails.add(c.getString(0));
                                adnRec.emails.add("");
                                if (adnRec.email.isEmpty()) {
                                    adnRec.email = c.getString(0);
                                }
                                break;
                            case 2:
                                adnRec.emails.add(c.getString(0));
                                if (adnRec.email.isEmpty()) {
                                    adnRec.email = c.getString(0);
                                } else if (adnRec.email2.isEmpty()) {
                                    adnRec.email2 = c.getString(0);
                                }
                                break;
                        }
                    }
                }
            }
            c.close();
        }
        return adnRec;
    }
}
