package com.android.wallpaperpicker.common;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import com.android.wallpaperpicker.R;

public class DialogUtils {
    public static void executeCropTaskAfterPrompt(Context context, final AsyncTask<Integer, ?, ?> asyncTask, DialogInterface.OnCancelListener onCancelListener) {
        if (Utilities.isAtLeastN()) {
            new AlertDialog.Builder(context).setTitle(R.string.wallpaper_instructions).setItems(R.array.which_wallpaper_options, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    int i2;
                    if (i != 0) {
                        if (i == 1) {
                            i2 = 2;
                        } else {
                            i2 = 3;
                        }
                    } else {
                        i2 = 1;
                    }
                    asyncTask.execute(Integer.valueOf(i2));
                }
            }).setOnCancelListener(onCancelListener).show();
        } else {
            asyncTask.execute(1);
        }
    }
}
