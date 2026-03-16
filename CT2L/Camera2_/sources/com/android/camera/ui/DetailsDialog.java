package com.android.camera.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.android.camera.data.MediaDetails;
import com.android.camera2.R;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

public class DetailsDialog {
    public static Dialog create(Context context, MediaDetails mediaDetails) {
        ListView detailsList = (ListView) LayoutInflater.from(context).inflate(R.layout.details_list, (ViewGroup) null, false);
        detailsList.setAdapter((ListAdapter) new DetailsAdapter(context, mediaDetails));
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        return builder.setTitle(R.string.details).setView(detailsList).setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        }).create();
    }

    private static class DetailsAdapter extends BaseAdapter {
        private final Context mContext;
        private final ArrayList<String> mItems;
        private final MediaDetails mMediaDetails;
        private final Locale mDefaultLocale = Locale.getDefault();
        private final DecimalFormat mDecimalFormat = new DecimalFormat(".####");
        private int mWidthIndex = -1;
        private int mHeightIndex = -1;

        public DetailsAdapter(Context context, MediaDetails details) {
            this.mContext = context;
            this.mMediaDetails = details;
            this.mItems = new ArrayList<>(details.size());
            setDetails(context, details);
        }

        private void setDetails(Context context, MediaDetails details) {
            String value;
            String value2;
            boolean resolutionIsValid = true;
            String path = null;
            for (Map.Entry<Integer, Object> detail : details) {
                switch (detail.getKey().intValue()) {
                    case 5:
                        this.mWidthIndex = this.mItems.size();
                        if (detail.getValue().toString().equalsIgnoreCase("0")) {
                            value = context.getString(R.string.unknown);
                            resolutionIsValid = false;
                        } else {
                            value = toLocalInteger(detail.getValue());
                        }
                        break;
                    case 6:
                        this.mHeightIndex = this.mItems.size();
                        if (detail.getValue().toString().equalsIgnoreCase("0")) {
                            value = context.getString(R.string.unknown);
                            resolutionIsValid = false;
                        } else {
                            value = toLocalInteger(detail.getValue());
                        }
                        break;
                    case 7:
                        value = toLocalInteger(detail.getValue());
                        break;
                    case 10:
                        value = Formatter.formatFileSize(context, ((Long) detail.getValue()).longValue());
                        break;
                    case 102:
                        MediaDetails.FlashState flash = (MediaDetails.FlashState) detail.getValue();
                        if (flash.isFlashFired()) {
                            value = context.getString(R.string.flash_on);
                        } else {
                            value = context.getString(R.string.flash_off);
                        }
                        break;
                    case 103:
                        double focalLength = Double.parseDouble(detail.getValue().toString());
                        value = toLocalNumber(focalLength);
                        break;
                    case 104:
                        value = "1".equals(detail.getValue()) ? context.getString(R.string.manual) : context.getString(R.string.auto);
                        break;
                    case 107:
                        String value3 = (String) detail.getValue();
                        double time = Double.valueOf(value3).doubleValue();
                        if (time < 1.0d) {
                            value = String.format(this.mDefaultLocale, "%d/%d", 1, Integer.valueOf((int) (0.5d + (1.0d / time))));
                        } else {
                            int integer = (int) time;
                            double time2 = time - ((double) integer);
                            value = String.valueOf(integer) + "''";
                            if (time2 > 1.0E-4d) {
                                value = value + String.format(this.mDefaultLocale, " %d/%d", 1, Integer.valueOf((int) (0.5d + (1.0d / time2))));
                            }
                        }
                        break;
                    case 108:
                        value = toLocalNumber(Integer.parseInt((String) detail.getValue()));
                        break;
                    case MediaDetails.INDEX_PATH:
                        value = "\n" + detail.getValue().toString();
                        path = detail.getValue().toString();
                        break;
                    default:
                        Object valueObj = detail.getValue();
                        if (valueObj == null) {
                            DetailsDialog.fail("%s's value is Null", DetailsDialog.getDetailsName(context, detail.getKey().intValue()));
                        }
                        value = valueObj.toString();
                        break;
                }
                int key = detail.getKey().intValue();
                if (details.hasUnit(key)) {
                    value2 = String.format("%s: %s %s", DetailsDialog.getDetailsName(context, key), value, context.getString(details.getUnit(key)));
                } else {
                    value2 = String.format("%s: %s", DetailsDialog.getDetailsName(context, key), value);
                }
                this.mItems.add(value2);
            }
            if (!resolutionIsValid) {
                resolveResolution(path);
            }
        }

        public void resolveResolution(String path) {
            Bitmap bitmap = BitmapFactory.decodeFile(path);
            if (bitmap != null) {
                onResolutionAvailable(bitmap.getWidth(), bitmap.getHeight());
            }
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return false;
        }

        @Override
        public int getCount() {
            return this.mItems.size();
        }

        @Override
        public Object getItem(int position) {
            return this.mMediaDetails.getDetail(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView tv;
            if (convertView == null) {
                tv = (TextView) LayoutInflater.from(this.mContext).inflate(R.layout.details, parent, false);
            } else {
                tv = (TextView) convertView;
            }
            tv.setText(this.mItems.get(position));
            return tv;
        }

        public void onResolutionAvailable(int width, int height) {
            if (width != 0 && height != 0) {
                String widthString = String.format(this.mDefaultLocale, "%s: %d", DetailsDialog.getDetailsName(this.mContext, 5), Integer.valueOf(width));
                String heightString = String.format(this.mDefaultLocale, "%s: %d", DetailsDialog.getDetailsName(this.mContext, 6), Integer.valueOf(height));
                this.mItems.set(this.mWidthIndex, String.valueOf(widthString));
                this.mItems.set(this.mHeightIndex, String.valueOf(heightString));
                notifyDataSetChanged();
            }
        }

        private String toLocalInteger(Object valueObj) {
            if (valueObj instanceof Integer) {
                return toLocalNumber(((Integer) valueObj).intValue());
            }
            String value = valueObj.toString();
            try {
                return toLocalNumber(Integer.parseInt(value));
            } catch (NumberFormatException e) {
                return value;
            }
        }

        private String toLocalNumber(int n) {
            return String.format(this.mDefaultLocale, "%d", Integer.valueOf(n));
        }

        private String toLocalNumber(double n) {
            return this.mDecimalFormat.format(n);
        }
    }

    public static String getDetailsName(Context context, int key) {
        switch (key) {
            case 1:
                return context.getString(R.string.title);
            case 2:
                return context.getString(R.string.description);
            case 3:
                return context.getString(R.string.time);
            case 4:
                return context.getString(R.string.location);
            case 5:
                return context.getString(R.string.width);
            case 6:
                return context.getString(R.string.height);
            case 7:
                return context.getString(R.string.orientation);
            case 8:
                return context.getString(R.string.duration);
            case 9:
                return context.getString(R.string.mimetype);
            case 10:
                return context.getString(R.string.file_size);
            case MediaDetails.INDEX_MAKE:
                return context.getString(R.string.maker);
            case 101:
                return context.getString(R.string.model);
            case 102:
                return context.getString(R.string.flash);
            case 103:
                return context.getString(R.string.focal_length);
            case 104:
                return context.getString(R.string.white_balance);
            case 105:
                return context.getString(R.string.aperture);
            case 107:
                return context.getString(R.string.exposure_time);
            case 108:
                return context.getString(R.string.iso);
            case MediaDetails.INDEX_PATH:
                return context.getString(R.string.path);
            default:
                return "Unknown key" + key;
        }
    }

    private static void fail(String message, Object... args) {
        if (args.length != 0) {
            message = String.format(message, args);
        }
        throw new AssertionError(message);
    }
}
