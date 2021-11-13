package com.example.bajarecorderx;

import android.location.Location;
import android.location.LocationListener;

public class BajaLocationListener implements LocationListener {
    private double latitude;
    private double longitude;
    private double speed;
    private float gnss_accuracy;
    private long last_timestamp;
    public BajaLocationListener()
    {
        latitude = -999;
        longitude = -999;
        speed = -999;
        gnss_accuracy = -999;
        last_timestamp = -999;
    }

    @Override
    public void onLocationChanged(Location loc) {
        speed = loc.getSpeed();
        latitude = loc.getLatitude();
        longitude = loc.getLongitude();
        last_timestamp = loc.getTime();
        gnss_accuracy = loc.getAccuracy();
    }

    public void reset()
    {
        latitude = -999;
        longitude = -999;
        speed = -999;
        last_timestamp = -999;
        gnss_accuracy = -999;
    }
    public double getLat()
    {
        return latitude;
    }

    public double getLong()
    {
        return longitude;
    }

    public double getSpeed()
    {
        return speed;
    }

    public long getTimestamp()
    {
        return last_timestamp;
    }

    public float getAccuracy()
    {
        return gnss_accuracy;
    }
}
