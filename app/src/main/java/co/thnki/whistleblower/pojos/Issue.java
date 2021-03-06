package co.thnki.whistleblower.pojos;

import android.os.Parcel;
import android.os.Parcelable;

public class Issue implements Parcelable
{
    public String description;
    public String imgUrl;
    public String username;
    public String userDpUrl;
    public String userId;
    public String areaType;
    public String issueId;
    public String status;
    public int radius;
    public boolean anonymous;
    public String longitude;
    public String latitude;
    public String photoId;

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(this.description);
        dest.writeString(this.photoId);
        dest.writeString(this.imgUrl);
        dest.writeString(this.username);
        dest.writeString(this.userDpUrl);
        dest.writeString(this.userId);
        dest.writeString(this.areaType);
        dest.writeString(this.issueId);
        dest.writeString(this.status);
        dest.writeInt(this.radius);
        dest.writeByte(anonymous ? (byte) 1 : (byte) 0);
        dest.writeString(this.longitude);
        dest.writeString(this.latitude);
    }

    public Issue()
    {
    }

    protected Issue(Parcel in)
    {
        this.description = in.readString();
        this.photoId = in.readString();
        this.imgUrl = in.readString();
        this.username = in.readString();
        this.userDpUrl = in.readString();
        this.userId = in.readString();
        this.areaType = in.readString();
        this.issueId = in.readString();
        this.status = in.readString();
        this.radius = in.readInt();
        this.anonymous = in.readByte() != 0;
        this.longitude = in.readString();
        this.latitude = in.readString();
    }

    public static final Creator<Issue> CREATOR = new Creator<Issue>()
    {
        @Override
        public Issue createFromParcel(Parcel source)
        {
            return new Issue(source);
        }

        @Override
        public Issue[] newArray(int size)
        {
            return new Issue[size];
        }
    };
}