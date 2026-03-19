package com.android.documentsui.inspector;

import android.content.Context;
import android.content.res.Resources;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import com.android.documentsui.R;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.inspector.InspectorController;
import java.io.IOException;
import java.util.function.Consumer;

public class MediaView extends TableView implements InspectorController.MediaDisplay {
    private final Context mContext;
    private final Resources mResources;

    public MediaView(Context context) {
        this(context, null);
    }

    public MediaView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public MediaView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mContext = context;
        this.mResources = context.getResources();
    }

    @Override
    public void accept(DocumentInfo documentInfo, Bundle bundle, Runnable runnable) {
        setTitle(R.string.inspector_metadata_section, true);
        Bundle bundle2 = bundle.getBundle("android:documentExif");
        if (bundle2 != null) {
            showExifData(this, this.mResources, documentInfo, bundle2, runnable, new Consumer() {
                @Override
                public final void accept(Object obj) {
                    this.f$0.getAddress((float[]) obj);
                }
            });
        }
        Bundle bundle3 = bundle.getBundle("android.media.metadata.video");
        if (bundle3 != null) {
            showVideoData(this, this.mResources, documentInfo, bundle3, runnable);
        }
        Bundle bundle4 = bundle.getBundle("android.media.metadata.audio");
        if (bundle4 != null) {
            showAudioData(this, bundle4);
        }
        setVisible(!isEmpty());
    }

    public static void showAudioData(InspectorController.TableDisplay tableDisplay, Bundle bundle) {
        if (bundle.containsKey("android.media.metadata.ARTIST")) {
            tableDisplay.put(R.string.metadata_artist, bundle.getString("android.media.metadata.ARTIST"));
        }
        if (bundle.containsKey("android.media.metadata.COMPOSER")) {
            tableDisplay.put(R.string.metadata_composer, bundle.getString("android.media.metadata.COMPOSER"));
        }
        if (bundle.containsKey("android.media.metadata.ALBUM")) {
            tableDisplay.put(R.string.metadata_album, bundle.getString("android.media.metadata.ALBUM"));
        }
        if (bundle.containsKey("android.media.metadata.DURATION")) {
            tableDisplay.put(R.string.metadata_duration, android.text.format.DateUtils.formatElapsedTime(bundle.getInt("android.media.metadata.DURATION") / 1000));
        }
    }

    public static void showVideoData(InspectorController.TableDisplay tableDisplay, Resources resources, DocumentInfo documentInfo, Bundle bundle, Runnable runnable) {
        addDimensionsRow(tableDisplay, resources, bundle);
        if (MetadataUtils.hasVideoCoordinates(bundle)) {
            showCoordiantes(tableDisplay, resources, MetadataUtils.getVideoCoords(bundle), runnable);
        }
        if (bundle.containsKey("android.media.metadata.DURATION")) {
            tableDisplay.put(R.string.metadata_duration, android.text.format.DateUtils.formatElapsedTime(bundle.getInt("android.media.metadata.DURATION") / 1000));
        }
    }

    public static void showExifData(InspectorController.TableDisplay tableDisplay, Resources resources, DocumentInfo documentInfo, Bundle bundle, Runnable runnable, Consumer<float[]> consumer) {
        addDimensionsRow(tableDisplay, resources, bundle);
        if (bundle.containsKey("DateTime")) {
            tableDisplay.put(R.string.metadata_date_time, bundle.getString("DateTime"));
        }
        if (bundle.containsKey("GPSAltitude")) {
            tableDisplay.put(R.string.metadata_altitude, String.valueOf(bundle.getDouble("GPSAltitude")));
        }
        if (bundle.containsKey("Make") || bundle.containsKey("Model")) {
            String string = bundle.getString("Make");
            String string2 = bundle.getString("Model");
            if (string == null) {
                string = "";
            }
            if (string2 == null) {
                string2 = "";
            }
            tableDisplay.put(R.string.metadata_camera, resources.getString(R.string.metadata_camera_format, string, string2));
        }
        if (bundle.containsKey("FNumber")) {
            tableDisplay.put(R.string.metadata_aperture, resources.getString(R.string.metadata_aperture_format, Double.valueOf(bundle.getDouble("FNumber"))));
        }
        if (bundle.containsKey("ShutterSpeedValue")) {
            tableDisplay.put(R.string.metadata_shutter_speed, String.valueOf(formatShutterSpeed(bundle.getDouble("ShutterSpeedValue"))));
        }
        if (bundle.containsKey("FocalLength")) {
            tableDisplay.put(R.string.metadata_focal_length, String.format(resources.getString(R.string.metadata_focal_format), Double.valueOf(bundle.getDouble("FocalLength"))));
        }
        if (bundle.containsKey("ISOSpeedRatings")) {
            tableDisplay.put(R.string.metadata_iso_speed_ratings, String.format(resources.getString(R.string.metadata_iso_format), Integer.valueOf(bundle.getInt("ISOSpeedRatings"))));
        }
        if (MetadataUtils.hasExifGpsFields(bundle)) {
            float[] exifGpsCoords = MetadataUtils.getExifGpsCoords(bundle);
            showCoordiantes(tableDisplay, resources, exifGpsCoords, runnable);
            consumer.accept(exifGpsCoords);
        }
    }

    private static void showCoordiantes(InspectorController.TableDisplay tableDisplay, Resources resources, float[] fArr, final Runnable runnable) {
        String string = resources.getString(R.string.metadata_coordinates_format, Float.valueOf(fArr[0]), Float.valueOf(fArr[1]));
        if (runnable != null) {
            tableDisplay.put(R.string.metadata_coordinates, string, new View.OnClickListener() {
                @Override
                public final void onClick(View view) {
                    runnable.run();
                }
            });
        } else {
            tableDisplay.put(R.string.metadata_coordinates, string);
        }
    }

    private void getAddress(float[] fArr) {
        new AsyncTask<Float, Void, Address>() {
            static final boolean $assertionsDisabled = false;

            @Override
            protected Address doInBackground(Float... fArr2) {
                try {
                    return new Geocoder(MediaView.this.mContext).getFromLocation(fArr2[0].floatValue(), fArr2[1].floatValue(), 1).get(0);
                } catch (IOException e) {
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Address address) {
                if (address != null) {
                    MediaView mediaView = MediaView.this;
                    if (address.getMaxAddressLineIndex() >= 0) {
                        StringBuilder sb = new StringBuilder("");
                        sb.append(address.getAddressLine(0));
                        for (int i = 1; i < address.getMaxAddressLineIndex(); i++) {
                            sb.append("\n");
                            sb.append(address.getAddressLine(i));
                        }
                        mediaView.put(R.string.metadata_address, sb.toString());
                        return;
                    }
                    if (address.getLocality() != null) {
                        mediaView.put(R.string.metadata_address, address.getLocality());
                        return;
                    }
                    if (address.getSubAdminArea() != null) {
                        mediaView.put(R.string.metadata_address, address.getSubAdminArea());
                    } else if (address.getAdminArea() != null) {
                        mediaView.put(R.string.metadata_address, address.getAdminArea());
                    } else if (address.getCountryName() != null) {
                        mediaView.put(R.string.metadata_address, address.getCountryName());
                    }
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, Float.valueOf(fArr[0]), Float.valueOf(fArr[1]));
    }

    private static String formatShutterSpeed(double d) {
        if (d <= 0.0d) {
            return String.valueOf(Math.round(Math.pow(2.0d, (-1.0d) * d) * 10.0d) / 10.0d);
        }
        return "1/" + String.valueOf(((int) Math.pow(2.0d, d)) + 1);
    }

    private static void addDimensionsRow(InspectorController.TableDisplay tableDisplay, Resources resources, Bundle bundle) {
        if (bundle.containsKey("ImageWidth") && bundle.containsKey("ImageLength")) {
            tableDisplay.put(R.string.metadata_dimensions, resources.getString(R.string.metadata_dimensions_format, Integer.valueOf(bundle.getInt("ImageWidth")), Integer.valueOf(bundle.getInt("ImageLength")), Float.valueOf((r8 * r0) / 1000000.0f)));
        }
    }
}
