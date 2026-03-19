package android.widget;

import android.database.Cursor;
import android.widget.Filter;

class CursorFilter extends Filter {
    CursorFilterClient mClient;

    interface CursorFilterClient {
        void changeCursor(Cursor cursor);

        CharSequence convertToString(Cursor cursor);

        Cursor getCursor();

        Cursor runQueryOnBackgroundThread(CharSequence charSequence);
    }

    CursorFilter(CursorFilterClient cursorFilterClient) {
        this.mClient = cursorFilterClient;
    }

    @Override
    public CharSequence convertResultToString(Object obj) {
        return this.mClient.convertToString((Cursor) obj);
    }

    @Override
    protected Filter.FilterResults performFiltering(CharSequence charSequence) {
        Cursor cursorRunQueryOnBackgroundThread = this.mClient.runQueryOnBackgroundThread(charSequence);
        Filter.FilterResults filterResults = new Filter.FilterResults();
        if (cursorRunQueryOnBackgroundThread != null) {
            filterResults.count = cursorRunQueryOnBackgroundThread.getCount();
            filterResults.values = cursorRunQueryOnBackgroundThread;
        } else {
            filterResults.count = 0;
            filterResults.values = null;
        }
        return filterResults;
    }

    @Override
    protected void publishResults(CharSequence charSequence, Filter.FilterResults filterResults) {
        Cursor cursor = this.mClient.getCursor();
        if (filterResults.values != null && filterResults.values != cursor) {
            this.mClient.changeCursor((Cursor) filterResults.values);
        }
    }
}
