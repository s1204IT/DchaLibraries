package jp.co.omronsoft.iwnnime.ml;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class WnnArrayAdapter<T> extends ArrayAdapter<T> {
    private int mCheckIndex;
    private ArrayList<Drawable> mEntriesImage;
    private int mLayoutId;
    private ArrayList<RadioButton> mRadioButtonList;

    public WnnArrayAdapter(Context context, int resource) {
        super(context, resource);
        this.mEntriesImage = null;
        this.mCheckIndex = 0;
        this.mRadioButtonList = new ArrayList<>();
        this.mLayoutId = R.layout.image_string_list_layout;
    }

    public WnnArrayAdapter(Context context, int resource, int textViewResourceId) {
        super(context, resource, textViewResourceId);
        this.mEntriesImage = null;
        this.mCheckIndex = 0;
        this.mRadioButtonList = new ArrayList<>();
        this.mLayoutId = R.layout.image_string_list_layout;
    }

    public WnnArrayAdapter(Context context, int resource, T[] objects) {
        super(context, resource, objects);
        this.mEntriesImage = null;
        this.mCheckIndex = 0;
        this.mRadioButtonList = new ArrayList<>();
        this.mLayoutId = R.layout.image_string_list_layout;
    }

    public WnnArrayAdapter(Context context, int resource, int textViewResourceId, T[] objects) {
        super(context, resource, textViewResourceId, objects);
        this.mEntriesImage = null;
        this.mCheckIndex = 0;
        this.mRadioButtonList = new ArrayList<>();
        this.mLayoutId = R.layout.image_string_list_layout;
    }

    public WnnArrayAdapter(Context context, int resource, List<T> objects) {
        super(context, resource, objects);
        this.mEntriesImage = null;
        this.mCheckIndex = 0;
        this.mRadioButtonList = new ArrayList<>();
        this.mLayoutId = R.layout.image_string_list_layout;
    }

    public WnnArrayAdapter(Context context, int resource, int textViewResourceId, List<T> objects) {
        super(context, resource, textViewResourceId, objects);
        this.mEntriesImage = null;
        this.mCheckIndex = 0;
        this.mRadioButtonList = new ArrayList<>();
        this.mLayoutId = R.layout.image_string_list_layout;
    }

    public void setEntriesImage(ArrayList<Drawable> entries) {
        this.mEntriesImage = entries;
    }

    public void setCheckIndex(int index) {
        this.mCheckIndex = index;
    }

    public ArrayList<RadioButton> getRadioButtonList() {
        return this.mRadioButtonList;
    }

    public void setLayoutId(int id) {
        this.mLayoutId = id;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            Context context = getContext();
            LayoutInflater inflater = (LayoutInflater) context.getSystemService("layout_inflater");
            if (inflater == null) {
                return new View(context);
            }
            convertView = inflater.inflate(this.mLayoutId, parent, false);
        }
        RadioButton button = (RadioButton) convertView.findViewById(R.id.list_button_area);
        if (button != null) {
            button.setChecked(this.mCheckIndex == position);
            this.mRadioButtonList.add(button);
        }
        ImageView image = (ImageView) convertView.findViewById(R.id.list_image_area);
        if (image != null && this.mEntriesImage != null) {
            image.setImageDrawable(this.mEntriesImage.get(position));
        }
        TextView text = (TextView) convertView.findViewById(R.id.list_text_area);
        T item = getItem(position);
        if (text != null && item != null) {
            if (item instanceof CharSequence) {
                text.setText((CharSequence) item);
            } else {
                text.setText(item.toString());
            }
        }
        return convertView;
    }
}
