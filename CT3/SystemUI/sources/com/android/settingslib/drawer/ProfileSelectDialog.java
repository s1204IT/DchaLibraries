package com.android.settingslib.drawer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import com.android.settingslib.R$string;

public class ProfileSelectDialog extends DialogFragment implements DialogInterface.OnClickListener {
    private Tile mSelectedTile;

    public static void show(FragmentManager manager, Tile tile) {
        ProfileSelectDialog dialog = new ProfileSelectDialog();
        Bundle args = new Bundle();
        args.putParcelable("selectedTile", tile);
        dialog.setArguments(args);
        dialog.show(manager, "select_profile");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mSelectedTile = (Tile) getArguments().getParcelable("selectedTile");
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        UserAdapter adapter = UserAdapter.createUserAdapter(UserManager.get(context), context, this.mSelectedTile.userHandle);
        builder.setTitle(R$string.choose_profile).setAdapter(adapter, this);
        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        UserHandle user = this.mSelectedTile.userHandle.get(which);
        this.mSelectedTile.intent.putExtra("show_drawer_menu", true);
        this.mSelectedTile.intent.addFlags(32768);
        getActivity().startActivityAsUser(this.mSelectedTile.intent, user);
        ((SettingsDrawerActivity) getActivity()).onProfileTileOpen();
    }
}
