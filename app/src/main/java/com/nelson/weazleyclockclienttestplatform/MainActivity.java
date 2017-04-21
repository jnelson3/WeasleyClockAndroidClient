package com.nelson.weazleyclockclienttestplatform;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {
    ArrayList<Geofence> geofences = new ArrayList<>();
    PendingIntent pendingIntent;
    final int GEOFENCE_RADIUS = 200;
    GoogleApiClient googleApiClient;
    final String TAG = "Weazely Main";
    JSONArray locations;
    String userName = "";
    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    private void makeToast(String message){
        Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG);
        toast.show();
    }

    protected void onStart() {
        googleApiClient.connect();

        super.onStart();

        sharedPref = getApplicationContext().getSharedPreferences("UserData", Context.MODE_PRIVATE);
        editor = sharedPref.edit();

        final EditText userNameEditText = (EditText) findViewById(R.id.userNameEditText);
        final Button setUserNameButton = (Button) findViewById(R.id.setUserNameButton);
        setUserNameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userName = userNameEditText.getText().toString();
                editor.putString("userName", userName);
                editor.commit();
                getLocations();
            }

        });
    }

    protected void onStop() {
        googleApiClient.disconnect();
        super.onStop();
    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(geofences);
        return builder.build();
    }

    private PendingIntent getGeofencingPendingIntent() {
        // reuse pending intent if we already have it
        if (pendingIntent != null)
            return pendingIntent;

        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        //createJonathansGeofences();
        //createBJsGeofences();

        getLocations();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onResult(@NonNull Status status) {
        switch (status.getStatusCode()) {
            case CommonStatusCodes.SUCCESS:
                Log.d(TAG, "geofences created");
                break;
            case CommonStatusCodes.ERROR:
                Log.d(TAG, "error creating geofences");
                break;
            default:
                Log.d(TAG, "general error creating geofences");
                break;
        }
    }

    private void createGeofences() {
        try {
            for (int i = 0; i < locations.length(); i++){
                // check for empty coordinates, can use lat or lon
                if (locations.getJSONObject(i).get("lat") != JSONObject.NULL) {
                    geofences.add(new Geofence.Builder()
                            .setRequestId(locations.getJSONObject(i).getString("locationName"))
                            .setCircularRegion(
                                    locations.getJSONObject(i).getDouble("lat"),
                                    locations.getJSONObject(i).getDouble("lon"),
                                    locations.getJSONObject(i).getInt("radius")
                            )
                            .setExpirationDuration(Geofence.NEVER_EXPIRE)
                            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                            .build());
                }
            }

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "permissions not granted");
                makeToast("permission not granted");
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }

            LocationServices.GeofencingApi.addGeofences(
                    googleApiClient,
                    getGeofencingRequest(),
                    getGeofencingPendingIntent()
            ).setResultCallback(this);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void createJonathansGeofences () {
        geofences.add(new Geofence.Builder()
                .setRequestId("home")
                .setCircularRegion(
                        40.273339,
                        -111.713601,
                        GEOFENCE_RADIUS
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build());
        geofences.add(new Geofence.Builder()
                .setRequestId("school")
                .setCircularRegion(
                        40.2789849,
                        -111.7109747,
                        GEOFENCE_RADIUS
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build());
    }

    private void createBJsGeofences () {
        geofences.add(new Geofence.Builder()
                .setRequestId("home")
                .setCircularRegion(
                        40.273339,
                        -111.713601,
                        GEOFENCE_RADIUS
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build());
        geofences.add(new Geofence.Builder()
                .setRequestId("work")
                .setCircularRegion(
                        40.4292738,
                        -111.8935564,
                        GEOFENCE_RADIUS
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build());
    }

    private void sendNotification(String geofenceTransitionDetails) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.location_icon)
                        .setContentTitle("My notification")
                        .setContentText("Hello World!");
// Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, MainActivity.class);

// The stack builder object will contain an artificial back stack for the
// started Activity.
// This ensures that navigating backward from the Activity leads out of
// your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
// Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
// Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
// mId allows you to update the notification later on.
        mNotificationManager.notify(1000, mBuilder.build());
    }

private void getLocations() {
    RequestQueue queue = Volley.newRequestQueue(this);
    String url ="http://techingthetech.asuscomm.com:8080/api/v1/" + userName + ".json";
    Log.d(TAG, url);

// Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    try {
                        locations = new JSONObject(response).getJSONArray("locations");
                        createGeofences();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Volley failure");
            }
        });
// Add the request to the RequestQueue.
        queue.add(stringRequest);
    }
}