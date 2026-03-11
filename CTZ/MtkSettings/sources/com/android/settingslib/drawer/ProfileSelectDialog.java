package com.android.settingslib.drawer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import com.android.settingslib.R;
import java.util.ArrayList;

public class ProfileSelectDialog extends DialogFragment implements DialogInterface.OnClickListener {
    private static final boolean DEBUG = Log.isLoggable("ProfileSelectDialog", 3);
    private Tile mSelectedTile;

    public static void show(FragmentManager fragmentManager, Tile tile) {
        ProfileSelectDialog profileSelectDialog = new ProfileSelectDialog();
        Bundle bundle = new Bundle();
        bundle.putParcelable("selectedTile", tile);
        profileSelectDialog.setArguments(bundle);
        profileSelectDialog.show(fragmentManager, "select_profile");
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mSelectedTile = (Tile) getArguments().getParcelable("selectedTile");
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        Activity activity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.choose_profile).setAdapter(UserAdapter.createUserAdapter(UserManager.get(activity), activity, this.mSelectedTile.userHandle), this);
        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        UserHandle userHandle = this.mSelectedTile.userHandle.get(i);
        this.mSelectedTile.intent.addFlags(32768);
        getActivity().startActivityAsUser(this.mSelectedTile.intent, userHandle);
    }

    public static void updateUserHandlesIfNeeded(Context context, Tile tile) {
        ArrayList<UserHandle> arrayList = tile.userHandle;
        if (tile.userHandle == null || tile.userHandle.size() <= 1) {
            return;
        }
        UserManager userManager = UserManager.get(context);
        for (int size = arrayList.size() - 1; size >= 0; size--) {
            if (userManager.getUserInfo(arrayList.get(size).getIdentifier()) == null) {
                if (DEBUG) {
                    Log.d("ProfileSelectDialog", "Delete the user: " + arrayList.get(size).getIdentifier());
                }
                arrayList.remove(size);
            }
        }
    }
}
