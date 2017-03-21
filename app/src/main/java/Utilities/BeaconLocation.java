package Utilities;

import android.location.Location;

import com.kontakt.sdk.android.common.profile.IBeaconDevice;

/**
 * Created by Mads on 21-03-2017.
 */
public class BeaconLocation {
    public static Location getBeaconLocation(IBeaconDevice beacon) {
        return new Location("Beacon location");
    }
}
