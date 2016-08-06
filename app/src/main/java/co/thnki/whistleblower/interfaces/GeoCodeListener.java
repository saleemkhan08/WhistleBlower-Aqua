package co.thnki.whistleblower.interfaces;

public interface GeoCodeListener
{
    void onAddressObtained(String result);
    void onGeoCodingFailed();
    void onCancelled();
}
