package com.android.contacts.editor;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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

    public static ArrayList<ChoiceListItem> getChoices(Context context, int i) {
        boolean z;
        ArrayList<ChoiceListItem> arrayList = new ArrayList<>(4);
        if ((i & 2) > 0) {
            arrayList.add(new ChoiceListItem(3, context.getString(R.string.removePhoto)));
        }
        if ((i & 4) > 0) {
            if ((i & 8) <= 0) {
                z = false;
            } else {
                z = true;
            }
            String string = context.getString(z ? R.string.take_new_photo : R.string.take_photo);
            String string2 = context.getString(z ? R.string.pick_new_photo : R.string.pick_photo);
            if (PhoneCapabilityTester.isCameraIntentRegistered(context)) {
                arrayList.add(new ChoiceListItem(1, string));
            }
            arrayList.add(new ChoiceListItem(2, string2));
        }
        return arrayList;
    }

    public static ListPopupWindow createPopupMenu(Context context, View view, final Listener listener, int i) {
        final ArrayList<ChoiceListItem> choices = getChoices(context, i);
        ArrayAdapter arrayAdapter = new ArrayAdapter(context, R.layout.select_dialog_item, choices);
        final ListPopupWindow listPopupWindow = new ListPopupWindow(context);
        AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view2, int i2, long j) {
                switch (((ChoiceListItem) choices.get(i2)).getId()) {
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
        listPopupWindow.setAnchorView(view);
        listPopupWindow.setAdapter(arrayAdapter);
        listPopupWindow.setOnItemClickListener(onItemClickListener);
        listPopupWindow.setModal(true);
        listPopupWindow.setInputMethodMode(2);
        int dimensionPixelSize = context.getResources().getDimensionPixelSize(R.dimen.photo_action_popup_min_width);
        if (view.getWidth() < dimensionPixelSize) {
            listPopupWindow.setWidth(dimensionPixelSize);
        }
        return listPopupWindow;
    }

    public static final class ChoiceListItem {
        private final String mCaption;
        private final int mId;

        public ChoiceListItem(int i, String str) {
            this.mId = i;
            this.mCaption = str;
        }

        public String toString() {
            return this.mCaption;
        }

        public int getId() {
            return this.mId;
        }
    }
}
