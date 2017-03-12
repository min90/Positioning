package dk.ubicomp.positioning;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import java.lang.ref.WeakReference;

/**
 * Created by Jesper on 10/03/2017.
 */

public class FragmentTransactioner {

    private static FragmentTransactioner fragmentTransactioner;

    public static FragmentTransactioner get() {
        if (fragmentTransactioner == null) {
            fragmentTransactioner = new FragmentTransactioner();
        }
        return fragmentTransactioner;
    }

    private FragmentTransactioner() {
    }


    public void returnToHome(Activity activity) {
        FragmentManager fragmentManager = ((MainActivity) activity).getSupportFragmentManager();

        // First, clear back stack
        if (fragmentManager.getBackStackEntryCount() != 0) {
            fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    public void transactFragments(Activity activity, Fragment fragment, String backStackTag) {
        if (fragment != null) {
            FragmentManager fragmentManager = ((FragmentActivity) activity).getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            if (backStackTag != null) {
                fragmentTransaction.addToBackStack(backStackTag);
            }
            fragmentTransaction.replace(R.id.fragment_container, fragment);
            fragmentTransaction.commitAllowingStateLoss();
            Log.d(FragmentTransactioner.class.getSimpleName(), "Fragment replace");
        }
    }
}
