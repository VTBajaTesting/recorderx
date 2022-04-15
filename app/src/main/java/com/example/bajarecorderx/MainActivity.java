package com.example.bajarecorderx;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.ar.core.ArCoreApk;
import com.example.bajarecorderx.CameraPermissionHelper;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.MissingGlContextException;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.core.exceptions.SessionPausedException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    //our (hopeful) sensors
    private SensorManager mSensorManager;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Sensor mAccelerometer;
    private Sensor mRotation;

    //File writer
    private FileWriter writer;
    private FileOutputStream fileout;
    private OutputStreamWriter outputWriter;

    //Depth structures
    private Session mSession;
    private boolean mUserRequestedInstall;

    //results
    private double linear_acceleration_x;
    private double linear_acceleration_y;
    private double linear_acceleration_z;
    private int linear_acceleration_accuracy;
    private double rot_acceleration_x;
    private double rot_acceleration_y;
    private double rot_acceleration_z;
    private int rot_acceleration_accuracy;
    private long last_timestamp;

    private TextView infoText = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        linear_acceleration_x = -999;
        linear_acceleration_y = -999;
        linear_acceleration_z = -999;
        rot_acceleration_x = -999;
        rot_acceleration_y = -999;
        rot_acceleration_z = -999;
        last_timestamp = -999;
        getPerms();

        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        //get some sensors
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        infoText = (TextView)findViewById(R.id.info);
        try {
            locationListener = new BajaLocationListener();
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, locationListener);
        } catch (SecurityException e) {
            e.printStackTrace();
            Log.e("TSInf", "y u reject perms");
        }
        mRotation = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        //create a file to save in
        try {
            //fileout=openFileOutput("mytextfile.txt", MODE_PRIVATE);
            //outputWriter=new OutputStreamWriter(fileout);
            //outputWriter.write("Time(ns),LinearAcclerationX,LinearAcclerationY,LinearAcclerationZ,AccelAccuracy,RotationX,RotationY,RotationZ,RotationAccuracy,Latitude,Longitude,GPS Accuracy,Speed\n");
            //writer = new FileWriter(Environment.getDataDirectory()+"/test_data.csv");
            //Log.i("File", Environment.getDataDirectory()+"/test_data.csv");
            //writer.write("Time(ns),LinearAcclerationX,LinearAcclerationY,LinearAcclerationZ,AccelAccuracy,RotationX,RotationY,RotationZ,RotationAccuracy,Latitude,Longitude,GPS Accuracy,Speed\n");
            OutputStream os = openFileOutput("test_data.csv", MODE_PRIVATE);
            Log.i("Files dir", getFilesDir().getAbsolutePath());
            outputWriter = new OutputStreamWriter(os);
            outputWriter.write("Time(ns),LinearAcclerationX,LinearAcclerationY,LinearAcclerationZ,AccelMag,AccelAccuracy,RotationX,RotationY,RotationZ,RotationAccuracy,Latitude,Longitude,GPS Accuracy,Speed\n");
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("TSInf", "ded");
        }
    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mRotation, SensorManager.SENSOR_DELAY_NORMAL);
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            CameraPermissionHelper.requestCameraPermission(this);
        }

        try {
            if (mSession == null) {
                if(checkDepthAvailability()) {
                    switch (ArCoreApk.getInstance().requestInstall(this, mUserRequestedInstall)) {
                        case INSTALLED:
                            // Success: Safe to create the AR session.
                            mSession = new Session(this);
                            Config config = new Config(mSession);
                            boolean isDepthSupported = mSession.isDepthModeSupported(Config.DepthMode.AUTOMATIC);
                            if (isDepthSupported) {
                                config.setDepthMode(Config.DepthMode.AUTOMATIC);
                            } else {
                                Log.e("Depth", "not supported");
                            }

                            mSession.configure(config);
                            mSession.setCameraTextureNames(
                                    new int[] {0});
                            mSession.resume();
                            double depth = getDepth();
                            Log.w("ARcore Installed", "Depth enabled");

                            break;
                        case INSTALL_REQUESTED:
                            // When this method returns `INSTALL_REQUESTED`:
                            // 1. ARCore pauses this activity.
                            // 2. ARCore prompts the user to install or update Google Play
                            //    Services for AR (market://details?id=com.google.ar.core).
                            // 3. ARCore downloads the latest device profile data.
                            // 4. ARCore resumes this activity. The next invocation of
                            //    requestInstall() will either return `INSTALLED` or throw an
                            //    exception if the installation or update did not succeed.
                            mUserRequestedInstall = false;
                    }
                }
            }
        } catch (UnavailableUserDeclinedInstallationException e) {
            // Display an appropriate message to the user and return gracefully.
            Log.e("Asswipe", "wont install AR Core");
            e.printStackTrace();
        } catch (Exception e) {
            Log.e("ARCore", "Unknown ARcore exception");
            e.printStackTrace();
        }

    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this);
            }
            finish();
        }
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
        mSession.pause();
    }

    protected void onDestroy()
    {
        super.onDestroy();
        try {
            outputWriter.close();
        }catch(IOException e)
        {
            e.printStackTrace();
        }
        mSession.close();
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //Log.i("Sensor Changed","Imminent");
        if (event.sensor.getName().equals(mAccelerometer.getName())) {
            //Log.i("Sensor Changed","Linear Accelerator");
            linear_acceleration_x = event.values[0];
            linear_acceleration_y = event.values[1];
            linear_acceleration_z = event.values[2];
            linear_acceleration_accuracy = event.accuracy;
            last_timestamp = event.timestamp;
            attemptWrite();
        } else if (event.sensor.getName().equals(mRotation.getName())) {
            //Log.i("Sensor Changed","Rotation");
            rot_acceleration_x = event.values[0];
            rot_acceleration_y = event.values[1];
            rot_acceleration_z = event.values[2];
            rot_acceleration_accuracy = event.accuracy;
            last_timestamp = event.timestamp;
            attemptWrite();
        }
    }

    /*
    public void onLocationChanged(Location loc) {
        speed = loc.getSpeed();
        latitude = loc.getLatitude();
        longitude = loc.getLongitude();
        last_timestamp = loc.getTime();
        gnss_accuracy = loc.getAccuracy();
        attemptWrite();
    }*/

    private void attemptWrite() {

        if (linear_acceleration_x != -999 &&
                linear_acceleration_y != -999 &&
                linear_acceleration_z != -999 &&
                rot_acceleration_x != -999 &&
                rot_acceleration_y != -999 &&
                rot_acceleration_z != -999 &&
                last_timestamp != -999) {
            last_timestamp = Math.max(last_timestamp, ((BajaLocationListener)locationListener).getTimestamp());
            //Log.i("Writing",last_timestamp + "," + linear_acceleration_x + "," + linear_acceleration_y + "," + linear_acceleration_z + "," + linear_acceleration_accuracy + "," + rot_acceleration_x + "," + rot_acceleration_y + "," + rot_acceleration_z + "," + rot_acceleration_accuracy + "," + ((BajaLocationListener)locationListener).getLat() + "," + ((BajaLocationListener)locationListener).getLong() + "," + ((BajaLocationListener)locationListener).getAccuracy() + "," + ((BajaLocationListener)locationListener).getSpeed())
            try {
                double accelMag = Math.sqrt(Math.pow(linear_acceleration_x,2.0) + Math.pow(linear_acceleration_y,2.0) + Math.pow(linear_acceleration_z,2.0));
                infoText.setText(
                        last_timestamp + "," + linear_acceleration_x + "," + linear_acceleration_y + "," + linear_acceleration_z + "," + accelMag + "," +linear_acceleration_accuracy + "," + rot_acceleration_x + "," + rot_acceleration_y + "," + rot_acceleration_z + "," + rot_acceleration_accuracy + "," + ((BajaLocationListener)locationListener).getLat() + "," + ((BajaLocationListener)locationListener).getLong() + "," + ((BajaLocationListener)locationListener).getAccuracy() + "," + ((BajaLocationListener)locationListener).getSpeed()+","
                                + " "+ RotationsPerMinuteCalc.calcWRPM(((BajaLocationListener)locationListener).getSpeed())+","+RotationsPerMinuteCalc.calcSecondaryRPM(RotationsPerMinuteCalc.calcWRPM(((BajaLocationListener)locationListener).getSpeed())));
                outputWriter.write(
                        last_timestamp + "," + linear_acceleration_x + "," + linear_acceleration_y + "," + linear_acceleration_z + "," + accelMag + "," + linear_acceleration_accuracy + "," + rot_acceleration_x + "," + rot_acceleration_y + "," + rot_acceleration_z + "," + rot_acceleration_accuracy + "," + ((BajaLocationListener)locationListener).getLat() + "," + ((BajaLocationListener)locationListener).getLong() + "," + ((BajaLocationListener)locationListener).getAccuracy() + "," + ((BajaLocationListener)locationListener).getSpeed() + ","+RotationsPerMinuteCalc.calcWRPM(((BajaLocationListener)locationListener).getSpeed())+","+RotationsPerMinuteCalc.calcSecondaryRPM(RotationsPerMinuteCalc.calcWRPM(((BajaLocationListener)locationListener).getSpeed()))+","+"\n");
            } catch (IOException e) {
                e.printStackTrace();
            }

            linear_acceleration_x = -999;
            linear_acceleration_y = -999;
            linear_acceleration_z = -999;
            rot_acceleration_x = -999;
            rot_acceleration_y = -999;
            rot_acceleration_z = -999;
            last_timestamp = -999;
            ((BajaLocationListener)locationListener).reset();
        }


    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
        //deprecated, no clue why I need this
    }

    public void onProviderEnabled(String provider) {

    }

    public void onProviderDisabled(String provider) {

    }

    private void getPerms() {
        int permissionCheck = ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Log.e("BITCH","JUsT give ME tHE FUCKING terms you Absolute IDIOt!!!! I MAKE THIS FUckiNG SHIT fOr free AT 3 IN THE GOd DAMN MORNING THE LEAST YOU CAN FucKN DO IS MAKE MY LYFE EASY!!!!!!!!");
            } else {
                requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, 1);
            }
        }

        permissionCheck = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {
                Log.e("BITCH","JUsT give ME tHE FUCKING terms you Absolute IDIOt!!!! I MAKE THIS FUckiNG SHIT fOr free AT 3 IN THE GOd DAMN MORNING THE LEAST YOU CAN FucKN DO IS MAKE MY LYFE EASY!!!!!!!!");
            } else {
                requestPermission(Manifest.permission.ACCESS_COARSE_LOCATION, 1);
            }
        }

        permissionCheck = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                Log.e("BITCH","JUsT give ME tHE FUCKING terms you Absolute IDIOt!!!! I MAKE THIS FUckiNG SHIT fOr free AT 3 IN THE GOd DAMN MORNING THE LEAST YOU CAN FucKN DO IS MAKE MY LYFE EASY!!!!!!!!");
            } else {
                requestPermission(Manifest.permission.ACCESS_FINE_LOCATION, 1);
            }
        }
    }

    private void requestPermission(String permissionName, int permissionRequestCode) {
        Log.i("RequestPerms","Requesting Permissions");
        ActivityCompat.requestPermissions(this,
                new String[]{permissionName}, permissionRequestCode);
    }

    private boolean checkDepthAvailability() {
        ArCoreApk.Availability availability = ArCoreApk.getInstance().checkAvailability(this);
        if (availability.isTransient()) {
            // Continue to query availability at 5Hz while compatibility is checked in the background.
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    checkDepthAvailability();
                }
            }, 200);
        }
        if (availability.isSupported()) {
            return true;
        } else { // The device is unsupported or unknown.
            return false;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private double getDepth()
    {
        // Retrieve the depth image for the current frame, if available.
        Image depthImage = null;

        Frame frame = null;
        try {
            frame = mSession.update();
        }catch(CameraNotAvailableException e)
        {
            Log.e("Depth", "Camera not available");
            return -999;
        }
        catch(SessionPausedException e){
            Log.e("ARCore", "ARCore SessionPausedException thrown, frame update requires that ARCore is running");
            return -999;
        }
        catch(MissingGlContextException e)
        {
            Log.e("ARCore", "This operation requires a GL context that is not present");
            return -999;
        }
        try {
            depthImage = frame.acquireDepthImage();
            Image.Plane plane = depthImage.getPlanes()[0];
            int byteIndex = depthImage.getWidth()/2 * plane.getPixelStride() + depthImage.getHeight()/2 * plane.getRowStride();
            ByteBuffer buffer = plane.getBuffer().order(ByteOrder.nativeOrder());
            return buffer.getShort(byteIndex);
            // Use the depth image here.
        } catch (NotYetAvailableException e) {
            // This means that depth data is not available yet.
            // Depth data will not be available if there are no tracked
            // feature points. This can happen when there is no motion, or when the
            // camera loses its ability to track objects in the surrounding
            // environment.
        } finally {
            if (depthImage != null) {
                depthImage.close();
            }
        }

        return -999;
    }

}
