package com.example.project72471.LocationTracker;

import static android.content.Context.LOCATION_SERVICE;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.example.project72471.Serial.SerialService;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.ui.IconGenerator;
import com.project72471.R;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MapsFragment extends Fragment implements LocationListener, TaskLoadedCallback, RoutingListener {

    private DatabaseReference reference;
    private LocationManager manager;

    View myView;

    private String fileName = "Test";
    private GoogleMap mMap;
    private String mLastUpdateTime;
    LatLng mCurrPosition = new LatLng(31.0461, 34.8516);
    MyLocation mCurrentLocation;


    private static final int REQUEST_CODE = 101;


    private final int MIN_TIME = 60 * 1000; // 2 min
    private final float MIN_DISTANCE = (float) 60; // meters;

    private Polyline currentPolyline;
    ArrayList<LatLng> markerPoints = new ArrayList<>();
    private List<Polyline> polylines = null;

    private SupportMapFragment mapFragment;


    private OnMapReadyCallback callback = new OnMapReadyCallback() {


        @Override
        public void onMapReady(GoogleMap googleMap) {
            mMap = googleMap;

            if ((ActivityCompat.checkSelfPermission(myView.getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                    && (ActivityCompat.checkSelfPermission(myView.getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {

                mMap.setMyLocationEnabled(true);
            } else {
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE);
            }
            mMap.getUiSettings().setZoomControlsEnabled(true);
            mMap.getUiSettings().setAllGesturesEnabled(true);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(mCurrPosition));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mCurrPosition, (float) 20));//Animates camera and zooms to preferred state on the user's current location.

        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_maps, container, false);

        myView = view;


        fileName = DateFormat.getDateTimeInstance().format(new Date());


        manager = (LocationManager) getActivity().getSystemService(LOCATION_SERVICE);
        String userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        reference = FirebaseDatabase.getInstance().getReference().child("LocationTracker").child(userUid).child(fileName);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);


        // Async map
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                // When map is loaded
                googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(LatLng latLng) {
                    }
                });
            }
        });

        getLocationUpdates();
        readChanges();
        return view;


    }

    public MyLocation getCurrentLocation() {
        return mCurrentLocation;
    }


    private void getLocationUpdates() {
        Location location = null;
        if (manager != null) {
            if ((ActivityCompat.checkSelfPermission(myView.getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                    && (ActivityCompat.checkSelfPermission(myView.getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {

                // getting GPS status
                boolean isGPSEnabled = manager.isProviderEnabled(LocationManager.GPS_PROVIDER);

                // getting network status
                boolean isNetworkEnabled = manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

                if (!isGPSEnabled && !isNetworkEnabled) {
                    Toast.makeText(myView.getContext(), "No Provider Enabled", Toast.LENGTH_SHORT).show();
                } else {
                    // First get location from gps
                    if (isGPSEnabled) {
                        manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DISTANCE, this);
                        if (manager != null) {
                            location = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            if (location != null) {
                                double longitude = location.getLongitude();
                                double latitude = location.getLatitude();
                                mCurrPosition = new LatLng(latitude, longitude);
                                mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
                                saveLocation(location);
                            }
                        }
                    }
                    //get the location by Network Provider
                    if (isNetworkEnabled) {
                        if (location == null) {
                            manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME, MIN_DISTANCE, this);
                            if (manager != null) {
                                location = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                                if (location != null) {
                                    double longitude = location.getLongitude();
                                    double latitude = location.getLatitude();
                                    mCurrPosition = new LatLng(latitude, longitude);
                                    mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
                                    saveLocation(location);
                                }
                            }
                        }
                    }
                }
            }

        } else {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().stopService(new Intent(getActivity(), SerialService.class));

    }

    private void readChanges() {
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.child("location").exists()) {
                    try {
                        MyLocation location = snapshot.child("location").getValue(MyLocation.class);
                        if (location != null) {
                            mCurrentLocation = location;
                            LatLng newMarker = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());

                            if (mCurrPosition.latitude != newMarker.latitude || mCurrPosition.longitude != newMarker.longitude) {
                                mCurrPosition = newMarker;
                                markerPoints.add(newMarker);
                                mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());

                                createMarker();
                                findRoutes();
                            }
                        }
                    } catch (Exception e) {
                        Toast.makeText(myView.getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    // function to find Routes.
    public void findRoutes() {
        if (markerPoints.size() >= 2) {
            Routing routing = new Routing.Builder()
                    .travelMode(AbstractRouting.TravelMode.WALKING)
                    .withListener(this)
                    .alternativeRoutes(true)
                    .waypoints(markerPoints.get(0), markerPoints.get(markerPoints.size() - 1))
                    .key(myView.getContext().getResources().getString(R.string.api_key))  //also define your api key here.
                    .build();
            routing.execute();
        }
//        else if (markerPoints.size()==1) {
//            Routing routing = new Routing.Builder()
//                    .travelMode(AbstractRouting.TravelMode.WALKING)
//                    .withListener(this)
//                    .alternativeRoutes(true)
//                    .waypoints(markerPoints.get(0))
//                    .key(getResources().getString(R.string.api_key))  //also define your api key here.
//                    .build();
//            routing.execute();
//        }

    }

    Context context;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.context = null;
    }


    private void createMarker() {
        MarkerOptions options = new MarkerOptions();
        IconGenerator iconFactory = new IconGenerator(myView.getContext());
        iconFactory.setStyle(IconGenerator.STYLE_PURPLE);
        options.icon(BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon(mLastUpdateTime)));
        options.anchor(iconFactory.getAnchorU(), iconFactory.getAnchorV());

        options.position(mCurrPosition);
        Marker mapMarker = mMap.addMarker(options);
        long atTime = mCurrentLocation.getTime();
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date(atTime));
        mapMarker.setTitle(mLastUpdateTime);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(mCurrPosition));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mCurrPosition, (float) 20));


    }

    private void saveLocation(Location location) {
        reference.child("location").setValue(location);
    }

    private void savePoint(LatLng point, String key) {
        reference.child(key).setValue(point);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(callback);
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        if (location != null) {
            saveLocation(location);
        } else {
            Toast.makeText(myView.getContext(), "No Location", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onRoutingFailure(RouteException e) {
        View parentLayout = myView.findViewById(android.R.id.content);
        Snackbar snackbar = Snackbar.make(parentLayout, e.toString(), Snackbar.LENGTH_LONG);
        snackbar.show();

    }

    @Override
    public void onRoutingStart() {

    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> arrayList, int i) {
        if (polylines != null) {
            polylines.clear();
        }
        PolylineOptions polyOptions = new PolylineOptions();
        LatLng polylineStartLatLng = markerPoints.get(0);
        LatLng polylineEndLatLng = markerPoints.get(markerPoints.size() - 1);
        savePoint(polylineStartLatLng, "start");
        savePoint(polylineEndLatLng, "finish");


        polylines = new ArrayList<>();

        polyOptions.color(myView.getContext().getResources().getColor(R.color.colorPrimary));
        polyOptions.width(7);
        polyOptions.addAll(markerPoints);
        Polyline polyline = mMap.addPolyline(polyOptions);
        polylines.add(polyline);

    }

    @Override
    public void onRoutingCancelled() {
        findRoutes();

    }

    @Override
    public void onTaskDone(Object... values) {
        if (currentPolyline != null)
            currentPolyline.remove();
        currentPolyline = mMap.addPolyline((PolylineOptions) values[0]);


    }

//    @Override
//    public void onMapReady(@NonNull GoogleMap googleMap) {
//        mMap = googleMap;
//
//        if ((ActivityCompat.checkSelfPermission(myView.getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
//                && (ActivityCompat.checkSelfPermission(myView.getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
//
//            mMap.setMyLocationEnabled(true);
//        } else {
//            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
//            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE);
//        }
//
//
//        mMap.getUiSettings().setZoomControlsEnabled(true);
//        mMap.getUiSettings().setAllGesturesEnabled(true);
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(mCurrPosition));
//        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mCurrPosition, (float) 20));//Animates camera and zooms to preferred state on the user's current location.
//
//
//    }
}