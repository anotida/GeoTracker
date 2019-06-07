package zw.co.vokers.geo_tracker.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
//import android.os.Bundle;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import zw.co.vokers.geo_tracker.R;
import zw.co.vokers.geo_tracker.TrackingService;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initialiseTracking();

        final Button startTracking = findViewById(R.id.startTrackingBtn);
        final Button getLocation = findViewById(R.id.getLocationBtn);
        final TextView locationText = findViewById(R.id.locationCrdtsTxt);

        startTracking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("Button", "Start tracking the location ...");
                initialiseTracking();
            }
        });

        getLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (android.os.Build.VERSION.SDK_INT > 9) {
                    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                    StrictMode.setThreadPolicy(policy);
                }

                Log.i("Button", "Get the current location ...");

                HttpURLConnection urlConnection = null;
                try {
                    URL url = new URL("https://geo-tracker-b059b.firebaseio.com/.json");
                    urlConnection = (HttpURLConnection) url.openConnection();
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    String readStream = readStream(in);

                    Log.i("Info", readStream);

                    JSONObject jsonObject = new JSONObject(readStream);
                    String rawData = (String) jsonObject.get("location");

                    String[] latLng = extractLatLng(rawData);

                    locationText.setText("Location | lat[" + Double.parseDouble(latLng[0]) + "] lng[" + Double.parseDouble(latLng[1]) + "]");
                } catch (Exception e) {
                    e.printStackTrace();
                }finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                }



            }
        });

    }

    private String readStream(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader r = new BufferedReader(new InputStreamReader(is),1000);
        for (String line = r.readLine(); line != null; line =r.readLine()){
            sb.append(line);
        }
        is.close();
        return sb.toString();
    }

    private void initialiseTracking() {
        //Check whether GPS tracking is enabled//
        Log.i("GPS Enabled", "Check whether GPS tracking is enabled");
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            finish();
        }

        //Check whether this app has access to the location permission//

        Log.i("Location Permission", "Check whether this app has access to the location permission");
        int permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        //If the location permission has been granted, then start the TrackerService//

        Log.i("Permission Granted",
                "If the location permission has been granted, then start the TrackerService");
        if (permission == PackageManager.PERMISSION_GRANTED) {
            startTrackerService();
        } else {

            //If the app doesn’t currently have access to the user’s location, then request access//

            Log.i("User Location Access",
                    "If the app doesn’t currently have access to the user’s location, then request access");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[]
            grantResults) {

//If the permission has been granted...//

        Log.i("Permissions", "If the permission has been granted...");
        if (requestCode == PERMISSIONS_REQUEST && grantResults.length == 1
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

//...then start the GPS tracking service//
            Log.i("Start", "...then start the GPS tracking service");
            startTrackerService();
        } else {

//If the user denies the permission request, then display a toast with some more information//
            Log.i("User Deniel",
                    "If the user denies the permission request, then display a toast with some more information");
            Toast.makeText(this, "Please enable location services to allow GPS tracking", Toast.LENGTH_SHORT).show();
        }
    }

    //Start the TrackerService//

    private void startTrackerService() {
        startService(new Intent(this, TrackingService.class));

//Notify the user that tracking has been enabled//
        Log.i("Notification", "Notify the user that tracking has been enabled");
        Toast.makeText(this, "GPS tracking enabled", Toast.LENGTH_SHORT).show();

//Close MainActivity//

//        finish();
    }

    private String[] extractLatLng(String rawData) {
        Log.i("Raw", "The raw data is " + rawData);
        String text = rawData.substring(rawData.indexOf(" "), rawData.indexOf("hAcc"));
        String[] location = text.trim().split(",");
        return location;
    }


    public static void main(String[] args) {
        String json = "Location[fused -17.807174,31.135454 hAcc=3 et=+1d18h25m55s297ms alt=1556.3474650381663 vel=0.023371672 vAcc=8 sAcc=??? bAcc=??? {Bundle[mParcelledData.dataSize=52]}]";

        String text = json.substring(json.indexOf(" "), json.indexOf("hAcc"));
        String[] location = text.trim().split(",");
        System.out.println("Location | lat[" + Double.parseDouble(location[0]) + "] lng[" + Double.parseDouble(location[1]) + "]");
    }

}
