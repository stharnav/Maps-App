package com.example.maps;

import static java.lang.Double.parseDouble;

import android.content.Context;

import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.Manifest;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;

import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.MapTileIndex;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;


public class MainActivity extends AppCompatActivity {
    private MapView map = null;
    private RequestQueue requestQueue;
    private IMapController mapController;
    private GeoPoint startPoint;

    private TextView name;
    private TextView displayName;

    private ImageButton closeButton;
    private MaterialCardView infoCard;
    private static final int REQUEST_FINE_LOCATION = 100;
    private FusedLocationProviderClient fusedLocationClient;
    private FloatingActionButton myLocation;
    private FloatingActionButton mapStyle;
    private Marker userMarker;
    private Marker placeMarker;
    boolean Mapnik = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //set id
        name = findViewById(R.id.name);
        displayName = findViewById(R.id.displayName);
        closeButton = findViewById(R.id.closeButton);
        infoCard = findViewById(R.id.informationCard);
        myLocation = findViewById(R.id.gps);
        mapStyle = findViewById(R.id.mapStyle);

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        //setup maps
        requestQueue = Volley.newRequestQueue(this);

        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);

        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);

        mapController = map.getController();
        mapController.setZoom(5);
        startPoint = new GeoPoint(0, 0);
        mapController.setCenter(startPoint);

        //
        userMarker = new Marker(map);
        userMarker.setTitle("You are here");
        userMarker.setIcon(getResources().getDrawable(R.drawable.location_pin));
        userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        placeMarker = new Marker(map);
        placeMarker.setIcon(getResources().getDrawable(R.drawable.pin));
        placeMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);


        mapStyle.setOnClickListener(V->{
            changeMapStyle();
        });

        closeButton.setOnClickListener(V->{
            infoCard.setVisibility(View.GONE);
        });

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        myLocation.setOnClickListener(V->{
            checkLocationPermission();
        });


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    100);  // 100 is requestCode
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_menu, menu);
        MenuItem search = menu.findItem(R.id.search);
        SearchView searchCity = (SearchView) search.getActionView();
        searchCity.setQueryHint("Search");

        searchCity.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Trigger search
                search(query);
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                // Handle live changes if needed
                return false;
            }
        });

        return true;
    }

    public void search(String cityName) {
        String url = "https://nominatim.openstreetmap.org/search?q=" + cityName.replace(" ", "%20") + "&format=json&limit=1";

        JsonArrayRequest request = new JsonArrayRequest(url,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        if (response.length() > 0) {
                            try {
                                infoCard.setVisibility(View.VISIBLE);

                                JSONObject location = response.getJSONObject(0);
                                JSONArray boundingBox = location.getJSONArray("boundingbox");

                                double lat = parseDouble(location.getString("lat"));
                                double lon = parseDouble(location.getString("lon"));

                                String nameA = location.getString("name");
                                String display = location.getString("display_name");

                                double south =  parseDouble(boundingBox.getString(0));
                                double north =  parseDouble(boundingBox.getString(1));
                                double west =  parseDouble(boundingBox.getString(2));
                                double east =  parseDouble(boundingBox.getString(3));
                                BoundingBox box = new BoundingBox(north, east, south, west);



                                GeoPoint newPoint = new GeoPoint(lat, lon);
                                map.getOverlays().add(placeMarker);
                                placeMarker.setPosition(newPoint);
                                placeMarker.setTitle(nameA);
                                placeMarker.setSnippet("Lat: " + lat + "\nLon: " + lon);
                                map.invalidate();

                                startPoint = new GeoPoint(lat, lon);
                                mapController.setZoom(5);
                                mapController.animateTo(startPoint);

                                name.setText(nameA);
                                displayName.setText(display);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "No results found for: " + cityName, Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(MainActivity.this, "Error loading location data", Toast.LENGTH_SHORT).show();
                    }
                }
        );


        requestQueue.add(request);
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_FINE_LOCATION);
        } else {
            getCurrentLocation();
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();
                            startPoint = new GeoPoint(latitude, longitude);
                            //mapController.zoomTo();
                            GeoPoint newPoint = new GeoPoint(latitude, longitude);
                            // Move marker

                            map.getOverlays().add(userMarker);
                            userMarker.setPosition(newPoint);
                            userMarker.setSnippet("Lat: " + latitude + "\nLon: " + longitude);
                            map.invalidate();
                            mapController.animateTo(startPoint);
                        } else {
                            Toast.makeText(MainActivity.this,
                                    "Location not available", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_FINE_LOCATION) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void changeMapStyle(){
        //setup map style
        if(Mapnik){
            OnlineTileSourceBase tileSource = new OnlineTileSourceBase("OpenTopoMap", 0, 18, 256, ".png",
                    new String[] { "https://a.tile.opentopomap.org/" }) {
                @Override
                public String getTileURLString(long pMapTileIndex) {
                    return getBaseUrl()
                            + MapTileIndex.getZoom(pMapTileIndex) + "/"
                            + MapTileIndex.getX(pMapTileIndex) + "/"
                            + MapTileIndex.getY(pMapTileIndex) + mImageFilenameEnding;
                }
            };
            Mapnik = false;
            map.setTileSource(tileSource);
        }else{
            // Switch back to OSM Mapnik
            map.setTileSource(TileSourceFactory.MAPNIK);
            Mapnik = true;
        }



    }

}
