package com.android.contacts.quickcontact;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import com.android.contacts.ContactSaveService;
import com.android.contacts.R;
import com.android.contacts.common.model.Contact;
import com.android.contacts.common.model.account.AccountWithDataSet;
import java.util.ArrayList;

public class DirectoryContactUtil {
    public static boolean isDirectoryContact(Contact contactData) {
        return (contactData == null || !contactData.isDirectoryEntry() || contactData.getDirectoryExportSupport() == 0) ? false : true;
    }

    public static void createCopy(ArrayList<ContentValues> values, AccountWithDataSet account, Context context) {
        Toast.makeText(context, R.string.toast_making_personal_copy, 1).show();
        Intent serviceIntent = ContactSaveService.createNewRawContactIntent(context, values, account, QuickContactActivity.class, "android.intent.action.VIEW");
        context.startService(serviceIntent);
    }
}
