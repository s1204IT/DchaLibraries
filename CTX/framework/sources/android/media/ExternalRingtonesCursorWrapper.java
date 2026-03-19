package android.media;

import android.content.ContentProvider;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;

public class ExternalRingtonesCursorWrapper extends CursorWrapper {
    private int mUserId;

    public ExternalRingtonesCursorWrapper(Cursor cursor, int i) {
        super(cursor);
        this.mUserId = i;
    }

    @Override
    public String getString(int i) {
        String string = super.getString(i);
        if (i == 2) {
            return ContentProvider.maybeAddUserId(Uri.parse(string), this.mUserId).toString();
        }
        return string;
    }
}
