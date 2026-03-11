package com.android.settings.dashboard;

import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.List;

public class DashboardCategory implements Parcelable {
    public static final Parcelable.Creator<DashboardCategory> CREATOR = new Parcelable.Creator<DashboardCategory>() {
        @Override
        public DashboardCategory createFromParcel(Parcel source) {
            return new DashboardCategory(source);
        }

        @Override
        public DashboardCategory[] newArray(int size) {
            return new DashboardCategory[size];
        }
    };
    public long id = -1;
    public List<DashboardTile> tiles = new ArrayList();
    public CharSequence title;
    public int titleRes;

    public DashboardCategory() {
    }

    public void addTile(DashboardTile tile) {
        this.tiles.add(tile);
    }

    public void removeTile(int n) {
        this.tiles.remove(n);
    }

    public int getTilesCount() {
        return this.tiles.size();
    }

    public DashboardTile getTile(int n) {
        return this.tiles.get(n);
    }

    public CharSequence getTitle(Resources res) {
        return this.titleRes != 0 ? res.getText(this.titleRes) : this.title;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.titleRes);
        TextUtils.writeToParcel(this.title, dest, flags);
        int count = this.tiles.size();
        dest.writeInt(count);
        for (int n = 0; n < count; n++) {
            DashboardTile tile = this.tiles.get(n);
            tile.writeToParcel(dest, flags);
        }
    }

    public void readFromParcel(Parcel in) {
        this.titleRes = in.readInt();
        this.title = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
        int count = in.readInt();
        for (int n = 0; n < count; n++) {
            DashboardTile tile = DashboardTile.CREATOR.createFromParcel(in);
            this.tiles.add(tile);
        }
    }

    DashboardCategory(Parcel in) {
        readFromParcel(in);
    }
}
