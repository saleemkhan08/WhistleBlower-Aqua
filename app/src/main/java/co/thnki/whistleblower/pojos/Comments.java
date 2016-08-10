package co.thnki.whistleblower.pojos;

import android.os.Parcel;
import android.os.Parcelable;

public class Comments implements Parcelable
{
    public String userName;
    public String userId;
    public String userDpUrl;
    public String comment;
    public String ngoName;
    public String ngoUrl;
    public long commentId;

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(this.userName);
        dest.writeString(this.userId);
        dest.writeString(this.userDpUrl);
        dest.writeString(this.comment);
        dest.writeString(this.ngoName);
        dest.writeString(this.ngoUrl);
        dest.writeLong(this.commentId);
    }

    public Comments()
    {
    }

    protected Comments(Parcel in)
    {
        this.userName = in.readString();
        this.userId = in.readString();
        this.userDpUrl = in.readString();
        this.comment = in.readString();
        this.ngoName = in.readString();
        this.ngoUrl = in.readString();
        this.commentId = in.readLong();
    }

    public static final Parcelable.Creator<Comments> CREATOR = new Parcelable.Creator<Comments>()
    {
        @Override
        public Comments createFromParcel(Parcel source)
        {
            return new Comments(source);
        }

        @Override
        public Comments[] newArray(int size)
        {
            return new Comments[size];
        }
    };
}