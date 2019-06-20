package com.theacedia.localweather;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class MainActivity extends Activity implements LocationListener {

    TextView latitudeField;
    TextView longitudeField;
    TextView cityField;
    TextView countryField;
    TextView adminAreaField;
    TextView thoroughFareField;

    LocationManager locationManager;
    String provider;

    String weatherSetCity;
    final String apiKey = "c3a60c4eabf8c88a705e053fe34230ce";
    final String urladdress = "https://api.openweathermap.org/data/2.5/weather";
    final String units = "metric";
    RequestQueue mRequestQueue;

    @RequiresApi(api = Build.VERSION_CODES.M)

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        latitudeField = findViewById(R.id.viewLatitude);
        longitudeField = findViewById(R.id.viewLongitude);
        cityField = findViewById(R.id.viewCity);
        countryField = findViewById(R.id.viewCountry);
        thoroughFareField = findViewById(R.id.viewThoroughFare);
        adminAreaField = findViewById(R.id.viewAdminArea);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        Criteria criteria = new Criteria();
        provider = locationManager.getBestProvider(criteria, false);

        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION}  , 1);
            return;
        }

        Location location = locationManager.getLastKnownLocation(provider);

        if (location != null) {
            System.out.println("Provider " + provider + " has been selected.");
            onLocationChanged(location);
        } else {
            latitudeField.setText("Location not available");
            longitudeField.setText("Location not available");
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                }
                return;
            }
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION}  , 1);
            return;
        }
        locationManager.requestLocationUpdates(provider, 400, 1, this);
    }


    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(this);
    }

    @Override
    public void onLocationChanged(Location location) {
        double lat =  location.getLatitude();
        double lng =  location.getLongitude();
        latitudeField.setText("LAT: " + lat);
        longitudeField.setText("LNG: " + lng);

        getLocationData(location, new OnGeocoderFinishedListener() {
            @Override
            public void onFinished(List<Address> results) {
                weatherSetCity = results.get(0).getLocality();
                countryField.setText(results.get(0).getLocality() + ", " + results.get(0).getSubLocality());
                cityField.setText(results.get(0).getCountryCode() + ", " + results.get(0).getCountryName());
                adminAreaField.setText(results.get(0).getAdminArea() + ", " + results.get(0).getSubAdminArea());
                thoroughFareField.setText(results.get(0).getThoroughfare() + ", " + results.get(0).getSubThoroughfare());
            }
        });
    }
    @SuppressLint("StaticFieldLeak")
    public void getLocationData(final Location location, final OnGeocoderFinishedListener listener) {
        new  AsyncTask<Void, Integer, List<Address>>() {
            @Override
            protected List<Address> doInBackground(Void... arg0) {
                Geocoder geoCoder = new Geocoder(getApplicationContext(), Locale.getDefault());
                List<Address> results = null;
                try {
                    results = geoCoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                } catch (IOException e) {
                    Log.e("TAG", "doInBackground: ", e);
                }
                return results;
            }

            @Override
            protected void onPostExecute(List<Address> results) {
                if (results != null && listener != null) {
                    listener.onFinished(results);
                }
            }
        }.execute();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
        Toast.makeText(this, "Enabled new provider " + provider, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(this, "Disabled provider " + provider, Toast.LENGTH_SHORT).show();
    }
    public abstract class OnGeocoderFinishedListener {
        public abstract void onFinished(List<Address> results);
    }
    @Override
    protected void onStop () {
        super.onStop();
        if (mRequestQueue != null) {
            mRequestQueue.cancelAll("weatherProcess");
        }
    }
    public void GetWeatherData (View v) {
        String url = urladdress + "?q=" + weatherSetCity + "&APPID=" + apiKey + "&units=" + units;
        final TextView text = findViewById(R.id.viewWeatherData);

        RequestQueue mRequestQueue = Volley.newRequestQueue(this);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        String jsonResponse = "";
                        try {
                            JSONObject weatherMain = response.getJSONObject("main");
                            JSONArray weatherDesc = response.getJSONArray("weather");
                            jsonResponse +=  weatherDesc.getJSONObject(0).getString("description");
                            jsonResponse += "\n";
                            jsonResponse += "Temperature: ";
                            jsonResponse += weatherMain.getString("temp");
                            jsonResponse += "°C";
                        } catch (JSONException e) {
                            Log.d("requestResult", "jsonError");
                            Log.d("requestResult", e.toString());
                        }

                        text.setText(jsonResponse);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        text.setText("That didn't work!");
                        Log.d("requestResult", error.getMessage());
                    }
                });
        jsonObjectRequest.setTag("weatherProcess");

        mRequestQueue.add(jsonObjectRequest);
        }
    }