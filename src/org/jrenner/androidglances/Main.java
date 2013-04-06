package org.jrenner.androidglances;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import org.jrenner.glances.*;

import java.net.MalformedURLException;

import java.net.URL;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

public class Main extends Activity {
    private static final String TAG = "Glances-Main";
    private TextView mainText;
    private MonitorFragment monitorFrag;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mainText = (TextView) findViewById(R.id.mainText);
        Button startButton = (Button) findViewById(R.id.startButton);
        Button quitButton = (Button) findViewById(R.id.quitButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                monitorFrag.startUpdates();
            }
        });
        quitButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Log.w(TAG, "Trying to shutdown...");
                finish();
            }
        });
        initializeFragments();
        monitorFrag.startUpdates();
    }

    void initializeFragments() {
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        monitorFrag = new MonitorFragment();
        fragmentTransaction.add(R.id.fragment_container, monitorFrag);
        fragmentTransaction.commit();
        fragmentManager.executePendingTransactions();
        if (monitorFrag == null)
            Log.e(TAG, "monitorFrag is null after trying to init");
        Log.d(TAG, "Finished init of fragment");
    }

}
