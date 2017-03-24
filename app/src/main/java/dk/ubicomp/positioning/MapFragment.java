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
import com.google.android.gms.maps.model.MarkerOptions;
import com.kontakt.sdk.android.ble.connection.OnServiceReadyListener;
import com.kontakt.sdk.android.ble.manager.ProximityManager;
import com.kontakt.sdk.android.ble.manager.ProximityManagerFactory;
import com.kontakt.sdk.android.ble.manager.listeners.IBeaconListener;
import com.kontakt.sdk.android.ble.manager.listeners.simple.SimpleIBeaconListener;
import com.kontakt.sdk.android.common.KontaktSDK;
import com.kontakt.sdk.android.common.profile.IBeaconDevice;
import com.kontakt.sdk.android.common.profile.IBeaconRegion;
import com.kontakt.sdk.android.common.util.ArrayUtils;
import com.mapspeople.data.OnDataReadyListener;
import com.mapspeople.mapcontrol.MapControl;
import com.mapspeople.mapcontrol.OnFloorUpdateListener;
import com.mapspeople.models.AppConfig;
import com.mapspeople.models.Building;
import com.mapspeople.models.CategoryCollection;
import com.mapspeople.models.Point;
import com.mapspeople.models.PushMessageCollection;
import com.mapspeople.models.Solution;
import com.mapspeople.models.VenueCollection;
import com.google.android.gms.maps.model.LatLng;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private boolean useBT = false;

    @BindView(R.id.map_view)
    MapView mapView;

    private GoogleApiClient mGoogleApiClient;
    private Location lastKnownLocation;
    private GoogleMap mGoogleMap;
    private MapControl mapControl;

    private ArrayList<Beacon> parsedBeacons;
    private ArrayList<Geometry> geom;
    private Marker userBTLocation;
    private boolean snappingInUse = false;


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

        //Beacons
        //Check bluetooth on device
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null) {
            //Enable bluetooth if not enabled
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }else {
                listenForBluetooth();
            }
        }


        return view;
    }

    private void listenForBluetooth() {
        Log.d(DEBUG_TAG, "Started listening for bluetooth");
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
                snappingInUse = true;
                Toast.makeText(getActivity(), "Switching to snapping", Toast.LENGTH_SHORT).show();
                break;
            case R.id.explorer_centroid:
                snappingInUse = false;
                Toast.makeText(getActivity(), "Switching to centroid", Toast.LENGTH_SHORT).show();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
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
        startScanning();
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
        lastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        Point pos = new Point(lastKnownLocation.getLongitude(), lastKnownLocation.getLatitude());
        mapControl.setMapPosition(pos, true);
        moveCamera(mGoogleMap, 17, new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()));
        notifyLocalizationMethod();
    }

    private void notifyLocalizationMethod() {
        if(snappingInUse) {
            Toast.makeText(getActivity(), "Snapping is in use!", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getActivity(), "Centroid Localization is in use!", Toast.LENGTH_LONG).show();
        }
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

    private void startScanning() {
        proximityManager.connect(new OnServiceReadyListener() {
            @Override
            public void onServiceReady() {
                Log.d(DEBUG_TAG, "ProxyMgr started scanning!");
                proximityManager.startScanning();
            }
        });
    }


    // Use a marker for user location
    // Use snapping where we look at the strength of the signal (RSSI)
    // So if room U165 is the strongest we snap to that room
    // Update beacons every 5 sec.
    private IBeaconListener createIBeaconListener() {
        return new SimpleIBeaconListener() {
            @Override
            public void onIBeaconDiscovered(IBeaconDevice ibeacon, IBeaconRegion region) {
                Log.d(DEBUG_TAG, "IBeacon discovered: " + ibeacon.toString());
                beacons.add(ibeacon);
                printBeacons();
                IBeaconDevice beaconDevice = getHighestRSSI();

                LatLng userLocation = null;
                HashMap<String, LatLng> userMap;
                String floor = "";
                if(snappingInUse) {
                    userMap = findLocationBySnapping(beaconDevice);
                } else {
                    userMap = findLocationByCentroidLoc(beacons);
                }
                if(userMap != null) {
                    for (Map.Entry<String, LatLng> entry : userMap.entrySet()) {
                        userLocation = entry.getValue();
                        floor = entry.getKey();
                        Log.d(DEBUG_TAG, "Location: " + userLocation.toString());
                        Log.d(DEBUG_TAG, "Floor: " + floor);
                    }
                }

                if(userLocation != null && floor != null && mapControl != null) {
                    if(userBTLocation != null)
                        userBTLocation.remove();
                    userBTLocation = mGoogleMap.addMarker(new MarkerOptions().position(new LatLng(userLocation.latitude, userLocation.longitude))
                            .title(beaconDevice.getName()).snippet("Info: " + userLocation.toString() + " " + floor));
                    mapControl.selectFloor(Integer.parseInt(floor));
                    moveCamera(mGoogleMap, 20, new LatLng(userLocation.latitude, userLocation.longitude));
                }


            }
            @Override
            public void onIBeaconsUpdated(List<IBeaconDevice> iBeacons, IBeaconRegion region) {
                Log.d("Sample", "Beacons updated size: " + iBeacons.size());
                beacons = new ArrayList<>(iBeacons);
                printBeacons();
                IBeaconDevice beaconDevice = getHighestRSSI();

                LatLng userLocation = null;
                String floor = "";
                HashMap<String, LatLng> userMap;
                if(snappingInUse) {
                    userMap = findLocationBySnapping(beaconDevice);
                } else {
                    userMap = findLocationByCentroidLoc(iBeacons);
                }
                if(userMap != null) {
                    for (Map.Entry<String, LatLng> entry : userMap.entrySet()) {
                        userLocation = entry.getValue();
                        floor = entry.getKey();
                        Log.d(DEBUG_TAG, "Location: " + userLocation.toString());
                        Log.d(DEBUG_TAG, "Floor: " + floor);
                    }
                }

                if(userLocation != null && floor != null && mapControl != null) {
                    if(userBTLocation != null)
                        userBTLocation.remove();
                    userBTLocation = mGoogleMap.addMarker(new MarkerOptions().position(new LatLng(userLocation.latitude, userLocation.longitude))
                            .title(beaconDevice.getName()).snippet("Info: " + userLocation.toString() + " " + floor));
                    mapControl.selectFloor(Integer.parseInt(floor));
                    moveCamera(mGoogleMap, 20, new LatLng(userLocation.latitude, userLocation.longitude));
                }


            }
            @Override
            public void onIBeaconLost(IBeaconDevice iBeacon, IBeaconRegion region) {
                Log.d("Sample", "Beacon lost: " + iBeacon.toString());
                beacons.remove(iBeacon);
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

    private String getFloor(ArrayList<String> mostCounts) {
        int max = 0;
        int curr;
        String currKey =  null;
        Set<String> unique = new HashSet<>(mostCounts);

        for (String key : unique) {
            curr = Collections.frequency(mostCounts, key);

            if(max < curr){
                max = curr;
                currKey = key;
            }
        }

        return currKey;
    }

    private HashMap<String, LatLng> findLocationByCentroidLoc(List<IBeaconDevice> beacons) {
        HashMap<String, LatLng> updater = new HashMap<>();
        double latitude;
        double longitude;
        ArrayList<Beacon> disBeacons = new ArrayList<>();
        ArrayList<LatLng> latLngs = new ArrayList<>();

        for (IBeaconDevice beacon : beacons) {
            for (Beacon be : parsedBeacons) {
                if(beacon.getUniqueId().equals(be.getAlias())) {
                    disBeacons.add(be);
                }
            }
        }

        ArrayList<String> mostCounts = new ArrayList<>();

        for (Geometry geo : geom) {
            for (Beacon be : disBeacons) {
                latitude = 0.0;
                longitude = 0.0;
                // Found a match in discovered beacons and location
                if(geo.getRoomId().equalsIgnoreCase(be.getRoomId())) {
                    for (int i = 0; i < geo.getCoordinates().size(); i++) {
                        if (i != geo.getCoordinates().size() - 1) {
                            latitude += geo.getCoordinates().get(i).latitude;
                            longitude += geo.getCoordinates().get(i).longitude;
                        }
                    }
                    mostCounts.add(be.getLevel().substring(0, 1));
                    latitude = latitude / (double) (geo.getCoordinates().size() - 1);
                    longitude = longitude / (double) (geo.getCoordinates().size() - 1);
                    LatLng loc = new LatLng(latitude, longitude);
                    latLngs.add(loc);
                }
            }
        }

        String floor = getFloor(mostCounts);
        Log.d(DEBUG_TAG, "Floor in centroid: " + floor);

        double tmpLongitude = 0.0;
        double tmpLatitude = 0.0;
        for (int i = 0; i < latLngs.size(); i++) {
            tmpLongitude += latLngs.get(i).longitude;
            tmpLatitude += latLngs.get(i).latitude;
        }

        if(latLngs.size() == 0) return null;
        double finalLongitude = tmpLongitude / latLngs.size();
        double finalLatitude = tmpLatitude / latLngs.size();

        LatLng finalLocation = new LatLng(finalLatitude, finalLongitude);
        updater.put(floor, finalLocation);

        return updater;
    }

    private HashMap<String, LatLng> findLocationBySnapping(IBeaconDevice iBeacon){
        HashMap<String, LatLng> updater = new HashMap<>();

        String roomAlias = iBeacon.getUniqueId();
        ArrayList<LatLng> latlngs = new ArrayList<>();
        String floor = "";
        double latitude = 0.0;
        double longitude = 0.0;

        //Find ibeacon room
        String room = "";
        for (Beacon b : parsedBeacons) {
            if(b.getAlias().equals(roomAlias)) {
                room = b.getRoomId();
                floor = b.getLevel().substring(0, 1);

            }
        }

        for (int i=0; i<geom.size(); i++){
            if (geom.get(i).getRoomId().equals(room))
                latlngs = geom.get(i).getCoordinates();

        }

        if(latlngs.size() == 0 || room.equalsIgnoreCase(""))
            return null;

        for (LatLng latlng: latlngs) {
            latitude += latlng.latitude;
            longitude += latlng.longitude;
        }

        longitude = longitude/latlngs.size();
        latitude = latitude/latlngs.size();

        LatLng userLocation = new LatLng(latitude, longitude);
        

        updater.put(floor, userLocation);
        return updater;
    }

    private void printBeacons() {
        Log.i(DEBUG_TAG, "Beacon list:");
        for (IBeaconDevice beacon : beacons) {
            Log.d(DEBUG_TAG, "Beacon: " + beacon.getRssi());
            Log.i(DEBUG_TAG, beacon.toString());
        }
    }

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
