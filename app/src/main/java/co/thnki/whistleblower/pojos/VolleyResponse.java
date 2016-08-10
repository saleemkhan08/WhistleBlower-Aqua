package co.thnki.whistleblower.pojos;

public class VolleyResponse
{
    public String mResponse;
    public int mRequestCode;
    public boolean mStatus;

    public VolleyResponse(String response, int requestCode, boolean status)
    {
        mResponse = response;
        mRequestCode = requestCode;
        mStatus = status;
    }
}
