package com.android.providers.contacts;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.android.providers.contacts.aggregation.ContactAggregator;
import java.io.IOException;

public class DataRowHandlerForPhoto extends DataRowHandler {
    private final int mMaxDisplayPhotoDim;
    private final int mMaxThumbnailPhotoDim;
    private final PhotoStore mPhotoStore;

    public DataRowHandlerForPhoto(Context context, ContactsDatabaseHelper dbHelper, ContactAggregator aggregator, PhotoStore photoStore, int maxDisplayPhotoDim, int maxThumbnailPhotoDim) {
        super(context, dbHelper, aggregator, "vnd.android.cursor.item/photo");
        this.mPhotoStore = photoStore;
        this.mMaxDisplayPhotoDim = maxDisplayPhotoDim;
        this.mMaxThumbnailPhotoDim = maxThumbnailPhotoDim;
    }

    @Override
    public long insert(SQLiteDatabase db, TransactionContext txContext, long rawContactId, ContentValues values) {
        if (values.containsKey("skip_processing")) {
            values.remove("skip_processing");
        } else if (!preProcessPhoto(values)) {
            return 0L;
        }
        long dataId = super.insert(db, txContext, rawContactId, values);
        if (!txContext.isNewRawContact(rawContactId)) {
            this.mContactAggregator.updatePhotoId(db, rawContactId);
            return dataId;
        }
        return dataId;
    }

    @Override
    public boolean update(SQLiteDatabase db, TransactionContext txContext, ContentValues values, Cursor c, boolean callerIsSyncAdapter) {
        long rawContactId = c.getLong(1);
        if (values.containsKey("skip_processing")) {
            values.remove("skip_processing");
        } else if (!preProcessPhoto(values)) {
            return false;
        }
        if (!super.update(db, txContext, values, c, callerIsSyncAdapter)) {
            return false;
        }
        this.mContactAggregator.updatePhotoId(db, rawContactId);
        return true;
    }

    private boolean preProcessPhoto(ContentValues values) {
        if (values.containsKey("data15")) {
            boolean photoExists = hasNonNullPhoto(values);
            if (photoExists) {
                if (!processPhoto(values)) {
                    return false;
                }
            } else {
                values.putNull("data15");
                values.putNull("data14");
            }
        }
        return true;
    }

    private boolean hasNonNullPhoto(ContentValues values) {
        byte[] photoBytes = values.getAsByteArray("data15");
        return photoBytes != null && photoBytes.length > 0;
    }

    @Override
    public int delete(SQLiteDatabase db, TransactionContext txContext, Cursor c) {
        long rawContactId = c.getLong(2);
        int count = super.delete(db, txContext, c);
        this.mContactAggregator.updatePhotoId(db, rawContactId);
        return count;
    }

    private boolean processPhoto(ContentValues values) {
        byte[] originalPhoto = values.getAsByteArray("data15");
        if (originalPhoto != null) {
            try {
                PhotoProcessor processor = new PhotoProcessor(originalPhoto, this.mMaxDisplayPhotoDim, this.mMaxThumbnailPhotoDim);
                long photoFileId = this.mPhotoStore.insert(processor);
                if (photoFileId != 0) {
                    values.put("data14", Long.valueOf(photoFileId));
                } else {
                    values.putNull("data14");
                }
                values.put("data15", processor.getThumbnailPhotoBytes());
                return true;
            } catch (IOException ioe) {
                Log.e("DataRowHandlerForPhoto", "Could not process photo for insert or update", ioe);
            }
        }
        return false;
    }
}
