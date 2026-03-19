package com.android.internal.telephony.cat;

import android.os.Parcel;
import android.os.Parcelable;

public class CommandDetails extends ValueObject implements Parcelable {
    public static final Parcelable.Creator<CommandDetails> CREATOR = new Parcelable.Creator<CommandDetails>() {
        @Override
        public CommandDetails createFromParcel(Parcel parcel) {
            return new CommandDetails(parcel);
        }

        @Override
        public CommandDetails[] newArray(int i) {
            return new CommandDetails[i];
        }
    };
    public int commandNumber;
    public int commandQualifier;
    public boolean compRequired;
    public int typeOfCommand;

    @Override
    public ComprehensionTlvTag getTag() {
        return ComprehensionTlvTag.COMMAND_DETAILS;
    }

    public CommandDetails() {
    }

    public boolean compareTo(CommandDetails commandDetails) {
        return this.compRequired == commandDetails.compRequired && this.commandNumber == commandDetails.commandNumber && this.commandQualifier == commandDetails.commandQualifier && this.typeOfCommand == commandDetails.typeOfCommand;
    }

    public CommandDetails(Parcel parcel) {
        this.compRequired = parcel.readInt() != 0;
        this.commandNumber = parcel.readInt();
        this.typeOfCommand = parcel.readInt();
        this.commandQualifier = parcel.readInt();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.compRequired ? 1 : 0);
        parcel.writeInt(this.commandNumber);
        parcel.writeInt(this.typeOfCommand);
        parcel.writeInt(this.commandQualifier);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String toString() {
        return "CmdDetails: compRequired=" + this.compRequired + " commandNumber=" + this.commandNumber + " typeOfCommand=" + this.typeOfCommand + " commandQualifier=" + this.commandQualifier;
    }
}
