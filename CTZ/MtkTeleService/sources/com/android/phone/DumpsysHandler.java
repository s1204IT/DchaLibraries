package com.android.phone;

import android.content.Context;
import com.android.phone.vvm.VvmDumpHandler;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class DumpsysHandler {
    public static void dump(Context context, FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        PhoneGlobals.getInstance().dump(fileDescriptor, printWriter, strArr);
        VvmDumpHandler.dump(context, fileDescriptor, printWriter, strArr);
    }
}
