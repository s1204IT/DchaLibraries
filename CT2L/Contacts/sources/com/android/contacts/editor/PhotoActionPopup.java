package com.android.contacts.editor;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListPopupWindow;
import com.android.contacts.R;
import com.android.contacts.util.PhoneCapabilityTester;
import com.android.contacts.util.UiClosables;
import java.util.ArrayList;

public class PhotoActionPopup {

    public interface Listener {
        void onPickFromGalleryChosen();

        void onRemovePictureChosen();

        void onTakePhotoChosen();
    }

    public static ListPopupWindow createPopupMenu(Context context, View anchorView, final Listener listener, int mode) {
        final ArrayList<ChoiceListItem> choices = new ArrayList<>(4);
        if ((mode & 2) > 0) {
            choices.add(new ChoiceListItem(3, context.getString(R.string.removePhoto)));
        }
        if ((mode & 4) > 0) {
            boolean replace = (mode & 8) > 0;
            int takePhotoResId = replace ? R.string.take_new_photo : R.string.take_photo;
            String takePhotoString = context.getString(takePhotoResId);
            int pickPhotoResId = replace ? R.string.pick_new_photo : R.string.pick_photo;
            String pickPhotoString = context.getString(pickPhotoResId);
            if (PhoneCapabilityTester.isCameraIntentRegistered(context)) {
                choices.add(new ChoiceListItem(1, takePhotoString));
            }
            choices.add(new ChoiceListItem(2, pickPhotoString));
        }
        ListAdapter adapter = new ArrayAdapter(context, R.layout.select_dialog_item, choices);
        final ListPopupWindow listPopupWindow = new ListPopupWindow(context);
        AdapterView.OnItemClickListener clickListener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ChoiceListItem choice = (ChoiceListItem) choices.get(position);
                switch (choice.getId()) {
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
                UiClosables.closeQuietly(listPopupWindow);
            }
        };
        listPopupWindow.setAnchorView(anchorView);
        listPopupWindow.setAdapter(adapter);
        listPopupWindow.setOnItemClickListener(clickListener);
        listPopupWindow.setModal(true);
        listPopupWindow.setInputMethodMode(2);
        int minWidth = context.getResources().getDimensionPixelSize(R.dimen.photo_action_popup_min_width);
        if (anchorView.getWidth() < minWidth) {
            listPopupWindow.setWidth(minWidth);
        }
        return listPopupWindow;
    }

    private static final class ChoiceListItem {
        private final String mCaption;
        private final int mId;

        public ChoiceListItem(int id, String caption) {
            this.mId = id;
            this.mCaption = caption;
        }

        public String toString() {
            return this.mCaption;
        }

        public int getId() {
            return this.mId;
        }
    }
}
