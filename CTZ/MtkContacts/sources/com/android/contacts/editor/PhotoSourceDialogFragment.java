package com.android.contacts.editor;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.TextView;
import com.android.contacts.R;
import com.android.contacts.editor.PhotoActionPopup;
import java.util.ArrayList;

public class PhotoSourceDialogFragment extends DialogFragment {

    public interface Listener {
        void onPickFromGalleryChosen();

        void onRemovePictureChosen();

        void onTakePhotoChosen();
    }

    public static void show(Activity activity, int i) {
        if (!(activity instanceof Listener)) {
            throw new IllegalArgumentException("Activity must implement " + Listener.class.getName());
        }
        Bundle bundle = new Bundle();
        bundle.putInt("photoMode", i);
        PhotoSourceDialogFragment photoSourceDialogFragment = new PhotoSourceDialogFragment();
        photoSourceDialogFragment.setArguments(bundle);
        photoSourceDialogFragment.show(activity.getFragmentManager(), "photoSource");
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        final ArrayList<PhotoActionPopup.ChoiceListItem> choices = PhotoActionPopup.getChoices(getActivity(), getArguments().getInt("photoMode"));
        CharSequence[] charSequenceArr = new CharSequence[choices.size()];
        for (int i = 0; i < charSequenceArr.length; i++) {
            charSequenceArr[i] = choices.get(i).toString();
        }
        DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i2) {
                Listener listener = (Listener) PhotoSourceDialogFragment.this.getActivity();
                switch (((PhotoActionPopup.ChoiceListItem) choices.get(i2)).getId()) {
                    case 1:
                        listener.onTakePhotoChosen();
                        break;
                    case 2:
                        listener.onPickFromGalleryChosen();
                        break;
                    case 3:
                        listener.onRemovePictureChosen();
                        break;
                }
                PhotoSourceDialogFragment.this.dismissAllowingStateLoss();
            }
        };
        TextView textView = (TextView) View.inflate(getActivity(), R.layout.dialog_title, null);
        textView.setText(R.string.menu_change_photo);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setCustomTitle(textView);
        builder.setItems(charSequenceArr, onClickListener);
        builder.setNegativeButton(android.R.string.cancel, null);
        return builder.create();
    }
}
