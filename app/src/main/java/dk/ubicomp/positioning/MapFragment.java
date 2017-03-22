package dk.ubicomp.positioning;


import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.kontakt.sdk.android.ble.connection.OnServiceReadyListener;
import com.kontakt.sdk.android.ble.manager.ProximityManager;
import com.kontakt.sdk.android.ble.manager.ProximityManagerFactory;
import com.kontakt.sdk.android.ble.manager.listeners.IBeaconListener;
import com.kontakt.sdk.android.ble.manager.listeners.simple.SimpleIBeaconListener;
import com.kontakt.sdk.android.common.KontaktSDK;
import com.kontakt.sdk.android.common.profile.IBeaconDevice;
import com.kontakt.sdk.android.common.profile.IBeaconRegion;
import com.mapspeople.data.OnDataReadyListener;
import com.mapspeople.mapcontrol.MapControl;
import com.mapspeople.models.AppConfig;
import com.mapspeople.models.CategoryCollection;
import com.mapspeople.models.Point;
import com.mapspeople.models.PushMessageCollection;
import com.mapspeople.models.Solution;
import com.mapspeople.models.VenueCollection;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import Utilities.BeaconLocation;
import Utilities.FindLocationByBeacon;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Jesper on 10/03/2017.
 */

public class MapFragment extends Fragment implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, GoogleMap.OnMarkerClickListener, com.google.android.gms.location.LocationListener,
        GoogleMap.OnInfoWindowClickListener, OnDataReadyListener {
    private static final String DEBUG_TAG = MapFragment.class.getSimpleName();
    private static final int PERMISSION_FINE_LOCATION_CODE = 101;
    //Beacons
    private static int REQUEST_ENABLE_BT = 1;
    private ProximityManager proximityManager;
    private ArrayList<IBeaconDevice> beacons = new ArrayList<>();

    @BindView(R.id.map_view)
    MapView mapView;

    private GoogleApiClient mGoogleApiClient;
    private Location lastKnownLocation;
    private GoogleMap mGoogleMap;
    private MapControl mapControl;

    private ArrayList<Beacon> parsedBeacons;
    private ArrayList<Geometry> geom;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        ButterKnife.bind(this, view);
        setHasOptionsMenu(true);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        mapControl = new MapControl(getActivity(), mapView);
        mapControl.setOnDataReadyListener(this);
        mapControl.initMap("55cdde212a91e0049824fe86", "sdu");
        connectToGoogleAPI();

        GeometryData geometryData = new GeometryData(getActivity());
        parsedBeacons = geometryData.parseBeacons();
        Log.d(DEBUG_TAG, "beacons " + parsedBeacons.toString());

        geom = geometryData.parseOULocations();
        Log.d(DEBUG_TAG, "Geometry: " + geom.toString());

//        //Beacons
//        //Check bluetooth on device
//        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        if (mBluetoothAdapter == null) {
//            // Device does not support Bluetooth
//        }
//        //Enable bluetooth if not enabled
//        if (!mBluetoothAdapter.isEnabled()) {
//            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
//        }else {
//            listenForBluetooth();
//        }

        return view;
    }

    private void listenForBluetooth() {
        KontaktSDK.initialize("qslWWgrHTjrQIcJWBfLnIPfyohnvKhfS");
        proximityManager = ProximityManagerFactory.create(getActivity());
        proximityManager.setIBeaconListener(createIBeaconListener());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MenuInflater menuInflater = getActivity().getMenuInflater();
        menuInflater.inflate(R.menu.map_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(DEBUG_TAG, "hello");
        switch (item.getItemId()) {
            case R.id.explorer_update:
                Toast.makeText(getActivity(), "Switching to indoor", Toast.LENGTH_SHORT).show();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private void connectToGoogleAPI() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    private void moveCamera(GoogleMap mGoogleMap, int zoomLevel, LatLng latLng) {
        if (mGoogleMap != null) {
            mGoogleMap.animateCamera(CameraUpdateFactory.newCameraPosition(makeInitialZoom(latLng, zoomLevel)));
            return;
        }
        Toast.makeText(getActivity(), "Kortet fejlede, prøv igen!", Toast.LENGTH_SHORT).show();
    }

    private CameraPosition makeInitialZoom(LatLng latLng, int zoomLevel) {
        return new CameraPosition.Builder()
                .target(latLng)
                .zoom(zoomLevel)
                .build();
    }


    @Override
    public void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) mGoogleApiClient.connect();
        //startScanning();
    }

    @Override
    public void onResume() {
        if (mapView != null) mapView.onResume();
        super.onResume();
    }

    @Override
    public void onPause() {
        if (mapView != null) mapView.onPause();
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mGoogleApiClient != null) mGoogleApiClient.disconnect();
        proximityManager.stopScanning();
    }

    @Override
    public void onDestroy() {
        if (mapView != null) mapView.onDestroy();
        proximityManager.disconnect();
        proximityManager = null;
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        if (mapView != null) mapView.onLowMemory();
        super.onLowMemory();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    private void initGoogleMapInstance() {
        if (mGoogleMap != null) {
            try {
                mGoogleMap.setMyLocationEnabled(true);
            } catch (SecurityException ex) {
                Log.e(DEBUG_TAG, "The Fine location permission were not granted", ex);
            }
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_FINE_LOCATION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_FINE_LOCATION_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) { // If the request is cancelled the grantResults array is empty so we check for higher than zero.
                    initGoogleMapInstance();
                } else {
                    Toast.makeText(getActivity(), "Permission to access fine location were not granted!", Toast.LENGTH_LONG).show();
                    FragmentTransactioner.get().returnToHome(getActivity());
                }
                return;
        }
    }


    @Override
    public void onInfoWindowClick(Marker marker) {

    }

    @Override
    public void onLocationDataReady() {

    }

    @Override
    public void onAppDataReady() {

    }

    @Override
    public void onVenueDataReady(VenueCollection venueCollection) {

    }

    @Override
    public void onSolutionDataReady(Solution solution) {

    }

    @Override
    public void onCategoryDataReady(CategoryCollection categoryCollection) {

    }

    @Override
    public void onPushMessageDataReady(PushMessageCollection pushMessageCollection) {

    }

    @Override
    public void onAppConfigDataReady(AppConfig appConfig) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_FINE_LOCATION_CODE);
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_FINE_LOCATION_CODE);

            return;
        }
        //lastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        //Point pos = new Point(lastKnownLocation.getLongitude(), lastKnownLocation.getLatitude());
        //mapControl.setMapPosition(pos, true);
        //moveCamera(mGoogleMap, 17, new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()));
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }

//    private void startScanning() {
//        proximityManager.connect(new OnServiceReadyListener() {
//            @Override
//            public void onServiceReady() {
//                proximityManager.startScanning();
//            }
//        });
//    }


    // Use a marker for user location
    // Use snapping where we look at the strength of the signal (RSSI)
    // So if room U165 is the strongest we snap to that room
    // Update beacons every 5 sec.
    private IBeaconListener createIBeaconListener() {
        return new SimpleIBeaconListener() {
            @Override
            public void onIBeaconDiscovered(IBeaconDevice ibeacon, IBeaconRegion region) {
                Log.i(DEBUG_TAG, "IBeacon discovered: " + ibeacon.toString());
                beacons.add(ibeacon);
                printBeacons();
                IBeaconDevice beaconDevice = getHighestRSSI();
                //Location userLocation = findLocationByLateration(beacons);
                //if(userLocation != null)
                //    Log.i(DEBUG_TAG, userLocation.toString());

            }
            @Override
            public void onIBeaconsUpdated(List<IBeaconDevice> iBeacons, IBeaconRegion region) {
                Log.i("Sample", "Beacons updated size: " + iBeacons.size());
                beacons = new ArrayList<>(iBeacons);
                printBeacons();
                IBeaconDevice beaconDevice = getHighestRSSI();
                //Location userLocation = findLocationByLateration(beacons);
                //if(userLocation != null)
                //    Log.i(DEBUG_TAG, userLocation.toString());
            }
            @Override
            public void onIBeaconLost(IBeaconDevice iBeacon, IBeaconRegion region) {
                Log.i("Sample", "Beacon lost: " + iBeacon.toString());
                beacons.remove(iBeacon);
                printBeacons();
                IBeaconDevice beaconDevice = getHighestRSSI();
                //Location userLocation = findLocationByLateration(beacons);
                //if(userLocation != null)
                //   Log.i(DEBUG_TAG, userLocation.toString());
            }
        };
    }

    private IBeaconDevice getHighestRSSI() {
        IBeaconDevice maxBeacon = null;
        for (IBeaconDevice beacon : beacons) {

            if(maxBeacon == null || maxBeacon.getRssi() > beacon.getRssi()) {
                maxBeacon = beacon;
            }

            Log.d(DEBUG_TAG, "Beacon: " + beacon.getRssi());
        }
        return maxBeacon;
    }

    private void printBeacons() {
        Log.i(DEBUG_TAG, "Beacon list:");
        for (IBeaconDevice beacon : beacons) {
            Log.d(DEBUG_TAG, "Beacon: " + beacon.getRssi());
            Log.i(DEBUG_TAG, beacon.toString());
        }
    }

//    private Location findLocationByLateration(ArrayList<IBeaconDevice> b) {
//        if(b.size() >= 3) {
//            Location beaconA = BeaconLocation.getBeaconLocation(b.get(0));
//            Location beaconB = BeaconLocation.getBeaconLocation(b.get(1));
//            Location beaconC = BeaconLocation.getBeaconLocation(b.get(2));
//
//            return FindLocationByBeacon.getLocationWithTrilateration(
//                    beaconA,
//                    beaconB,
//                    beaconC,
//                    FindLocationByBeacon.getRssiAsMeters(b.get(0).getRssi()),
//                    FindLocationByBeacon.getRssiAsMeters(b.get(1).getRssi()),
//                    FindLocationByBeacon.getRssiAsMeters(b.get(2).getRssi())
//            );
//        } else {
//            return null;
//        }
//    }
    //For bluetooth
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == getActivity().RESULT_OK) {
                listenForBluetooth();
            } else {
                //finish();
            }
        }
    }
}
