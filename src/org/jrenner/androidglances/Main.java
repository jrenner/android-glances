package org.jrenner.androidglances;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class Main extends Activity {
    private static final String TAG = "Glances-Main";
    private TextView mainText;
    private MonitorFragment monitorFrag;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Button quitButton = (Button) findViewById(R.id.quitButton);
        quitButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                ShutdownApp();
            }
        });
        Button raspberryButton = (Button) findViewById(R.id.raspberryButton);
        raspberryButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                monitorFrag.setServer("http://home.jrenner.org:7113", "Raspberry Pi");
            }
        });
        Button pcButton = (Button) findViewById(R.id.pcButton);
        pcButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                monitorFrag.setServer("http://192.168.173.103:61209", "Ubuntu PC");
            }
        });
        Button stopButton = (Button) findViewById(R.id.stopButton);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                monitorFrag.stopUpdates();
            }
        });
        if (monitorFrag == null) {
            initializeFragments();
        } else {
            Log.d(TAG, "monitorFrag already exists, not initializing");
        }
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

    void ShutdownApp() {
        monitorFrag.stopUpdates();
        Log.w(TAG, "Trying to shutdown");
        finish();
    }
}
