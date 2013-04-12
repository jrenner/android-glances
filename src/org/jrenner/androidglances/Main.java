package org.jrenner.androidglances;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;
import android.widget.Toast;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import java.util.ArrayList;

public class Main extends SherlockFragmentActivity {
    private static final String TAG = "Glances-Main";
    private MonitorFragment monitorFrag;
    private ArrayList<ServerEntry> serverList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        if (monitorFrag == null) {
            initializeFragments();
        } else {
            Log.d(TAG, "monitorFrag already exists, not initializing");
        }
        serverList = new ArrayList<ServerEntry>();
        serverList.add(new ServerEntry("http://home.jrenner.org:7113", "Raspberry Pi"));
        serverList.add(new ServerEntry("http://192.168.173.103:61209", "Ubuntu PC"));
        ActionBar abar = getSupportActionBar();
        abar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.bar, menu);
        ActionBar abar = getSupportActionBar();
        abar.setDisplayShowHomeEnabled(false);
        abar.setDisplayShowTitleEnabled(false);
        SpinnerAdapter adapter = new ArrayAdapter<ServerEntry>(this, android.R.layout.simple_spinner_dropdown_item,
                serverList);

        ActionBar.OnNavigationListener navListener = new ActionBar.OnNavigationListener() {
            @Override
            public boolean onNavigationItemSelected(int itemPosition, long itemId) {
                ServerEntry server = serverList.get(itemPosition);
                monitorFrag.setServer(server.url, server.name);
                return true;
            }
        };

        abar.setListNavigationCallbacks(adapter, navListener);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_quit:
                shutdownApp();
                break;
            default:
                Toast.makeText(this, "Unhandled action item", Toast.LENGTH_LONG).show();
                break;
        }

        return true;
    }

    void initializeFragments() {
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        monitorFrag = MonitorFragment.getInstance();
        fragmentTransaction.add(R.id.fragment_container, monitorFrag);
        fragmentTransaction.commit();
        fragmentManager.executePendingTransactions();
        if (monitorFrag == null)
            Log.e(TAG, "monitorFrag is null after trying to init");
        Log.d(TAG, "Finished init of fragment");
    }

    void shutdownApp() {
        Log.w(TAG, "Trying to shutdown");
        monitorFrag.shutdown();
        finish();
    }

    class ServerEntry {
        String url;
        String name;

        ServerEntry(String url, String name) {
            this.url = url;
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }

    }
}
