package com.android.contacts.common.extensions;

import android.content.Context;
import com.android.contacts.common.list.DirectoryPartition;
import java.util.List;

public interface ExtendedPhoneDirectoriesManager {
    List<DirectoryPartition> getExtendedDirectories(Context context);
}
