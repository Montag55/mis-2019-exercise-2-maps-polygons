package com.example.misexercuse_2;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.maps.android.SphericalUtil;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 1;
    private GoogleMap mMap = null;
    private EditText textInput = null;
    private LocationManager locationManager = null;
    private LocationListener locationListener = null;
    private SharedPreferences.Editor editor = null;
    private SharedPreferences pref = null;
    private int markerCount = 0;
    private List<Marker> selectedMarkers = null;
    private Boolean polygonExist = false;
    private Button button_area = null;


    /**
     * Not clearing Shared preferences at any point, markers stay forever
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        textInput = (EditText) findViewById(R.id.textInput);
        pref = (SharedPreferences) getApplicationContext().getSharedPreferences("Markers", MODE_PRIVATE);
        editor = (SharedPreferences.Editor) getSharedPreferences("Markers", MODE_PRIVATE).edit();
        selectedMarkers = new ArrayList<Marker>();

        // for unique marker ID, get marker count from previous session (also pref.getAll() map size)
        markerCount = pref.getInt("markerCount", 0);

        button_area = (Button) findViewById(R.id.button_area);
        button_area.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(polygonExist == false) {
                    calcPolygonArea();
                    button_area.setText("End Polygon");
                    polygonExist = true;
                } else {
                    mMap.clear();
                    selectedMarkers.clear();
                    checkLocationPermission();
                    button_area.setText("Start Polygon");
                    polygonExist = false;
                }
            }
        });
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        locationManager = (LocationManager) getSystemService(MapsActivity.this.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        checkLocationPermission();
        updatePreviousSession();

        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng point) {
            mMap.addMarker(new MarkerOptions().position(point).title(textInput.getText().toString()));

            String marker_str = Double.toString(point.latitude) + "@" + Double.toString(point.longitude);
            editor.putString( "ID" + Integer.toString(markerCount) + "@" + textInput.getText().toString(), marker_str);

            markerCount++;
            editor.putInt("markerCount", markerCount);

            editor.commit();

            String lat = String.format("%.02f", point.latitude);
            String lon = String.format("%.02f", point.longitude);
            Toast.makeText(MapsActivity.this, "New location:\n" + textInput.getText().toString() + "(" + lat + ", " + lon + ")" , Toast.LENGTH_LONG).show();
            }
        });

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {

                marker.showInfoWindow();
                if(selectedMarkers.contains(marker)) {
                    marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                    selectedMarkers.remove(marker);
                } else {
                    marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                    selectedMarkers.add(marker);
                }

                return true;
            }
        });
    }

    /**
     * Center and zoom in on current location. Sets marker of different color:
     * https://developers.google.com/maps/documentation/android-sdk/marker
     */
    public void centreMapOnLocation(Location location, String title) {
        LatLng coordinate = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(coordinate).title(title).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coordinate, 15));

        String lat = String.format("%.02f", coordinate.latitude);
        String lon = String.format("%.02f", coordinate.longitude);
        Toast.makeText(MapsActivity.this, "Your location:\n" + textInput.getText().toString() + "(" + lat + ", " + lon + ")" , Toast.LENGTH_LONG).show();
    }

    /**
     * Request permission to access GPS FINE_LOCATION if not yet granted.
     * If granted retrieve current location.
     */
    public void checkLocationPermission() {

        if (ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10, locationListener);
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                centreMapOnLocation(lastKnownLocation,"Your Location");
            } else{
                Toast.makeText(MapsActivity.this, "System does not support GPS_PROVIDER", Toast.LENGTH_LONG).show();
            }
        } else {
            ActivityCompat.requestPermissions(MapsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
        }
    }

    /**
     * Automatically called after calling requestPermission().
     * If permission granted, retrieve current location.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,10, locationListener);
                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    centreMapOnLocation(lastKnownLocation,"Your Location");
                } else {
                    Toast.makeText(MapsActivity.this, "System does not support GPS_PROVIDER", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(MapsActivity.this, "Permission not granted", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(MapsActivity.this, "Permission to access GPS denied", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Display markers from previous session, counter also in pref, skipp it!
     */
    public void updatePreviousSession(){
        Map<String,?> keys = pref.getAll();
        for(Map.Entry<String,?> entry : keys.entrySet()){
            if(!entry.getKey().equals("markerCount")){
                String [] key_str = entry.getKey().split("@", 2);
                String [] val_str = entry.getValue().toString().split("@", 2);
                mMap.addMarker(new MarkerOptions().position(new LatLng(Double.parseDouble(val_str[0]), Double.parseDouble(val_str[1]))).title(key_str[1]));
            }
        }
    }

    /**
     * Computes area of polygon spanning selected markers:
     * https://developers.google.com/maps/documentation/android-sdk/polygon-tutorial#add_polygons_to_represent_areas_on_the_map
     * http://googlemaps.github.io/android-maps-utils/javadoc/com/google/maps/android/SphericalUtil.html
     */
    public void calcPolygonArea(){

        if (selectedMarkers.size() >= 3) {

            double x = 0, y = 0;
            ArrayList<LatLng> polygon_verts = new ArrayList<>();
            PolygonOptions rectOptions = new PolygonOptions();

            for (Marker tmp : selectedMarkers) {
                polygon_verts.add(tmp.getPosition());
                y += tmp.getPosition().longitude;
                x += tmp.getPosition().latitude;
            }

            LatLng center = new LatLng(x/selectedMarkers.size(), y/selectedMarkers.size());
            polygon_verts = sortPoints(polygon_verts, center);
            rectOptions.addAll(polygon_verts);

            double area = SphericalUtil.computeArea(polygon_verts);
            String ending = null;

            if(area < 10000) {
                ending = " m2";
            }
            else if(area >= 10000 && area< 1000000){
                ending = " ha";
                area /= 10000;
            }
            else{
                ending = " km²";
                area /= 1e+6;
            }

            String area_str = String.format("%.02f", area);
            mMap.addPolygon(rectOptions.fillColor(0x556aa0f7).strokeWidth(1));
            mMap.addMarker(new MarkerOptions().position(new LatLng(center.latitude, center.longitude)).title(area_str + ending).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN)));
            polygonExist = true;

        } else {
            Toast.makeText(MapsActivity.this, "Not enough locations selected, a minimum of 3 have to be selected", Toast.LENGTH_LONG).show();
        }

    }

    /**
     * Sort points by angle to center using Bubblesort.
     */
    public ArrayList<LatLng> sortPoints(ArrayList<LatLng> points, LatLng center){

        for(int i = 1; i < points.size(); i++){
            System.out.println("i: " + i);
            for(int j = 0; j < points.size() - i; j++){
                if( Math.atan2(points.get(j).longitude - center.longitude, points.get(j).latitude - center.latitude) >
                    Math.atan2(points.get(j+1).longitude - center.longitude, points.get(j+1).latitude - center.latitude)){

                    LatLng tmp = points.get(j);
                    points.set(j, points.get(j+1));
                    points.set(j+1, tmp);
                }
            }
        }
        return points;
    }

}
