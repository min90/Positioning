package dk.ubicomp.positioning;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {
    private static final String DEBUG_TAG = MainActivity.class.getSimpleName();

    @BindView(R.id.btn_map) Button btnMap;
    @BindView(R.id.btn_scan) Button btnScan;
    @BindView(R.id.btn_info) Button btnInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @OnClick(R.id.btn_map)
    public void openMap() {
        Log.d(DEBUG_TAG, "Map blev klikket");
        FragmentTransactioner.get().transactFragments(this, new MapFragment(), "map_fragment");
    }

    @OnClick(R.id.btn_scan)
    public void openScanList() {
        Log.d(DEBUG_TAG, "Åben scanner fragment");
    }

    @OnClick(R.id.btn_info)
    public void openInfoFragment() {
        Log.d(DEBUG_TAG, "Info Fragment");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
