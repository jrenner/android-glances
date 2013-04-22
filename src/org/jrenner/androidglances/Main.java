package org.jrenner.androidglances;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class Main extends SherlockFragmentActivity {
    private static Main instance;
    private static final String TAG = "Glances-Main";
    private static MonitorFragment monitorFrag;
    private SpinnerAdapter serverSpinnerAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        instance = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        if (monitorFrag == null) {
            initializeFragments();
        } else {
            Log.d(TAG, "monitorFrag already exists, not initializing");
        }
        ActionBar abar = getSupportActionBar();
        abar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        TextSetter.setActivity(this);
        loadServers();
        monitorFrag.resetUpdateTimers();
    }

    @Override
    public void onPause() {
        super.onPause();
        saveServers();
        monitorFrag.stopUpdates();
        if (monitorFrag.getMonitoredServer() != null) {
            getSharedPreferences("system_data", MODE_PRIVATE).edit().putString("last_selected_server",
                monitorFrag.getMonitoredServer().nickName).commit();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        String serverNick = getSharedPreferences("system_data", MODE_PRIVATE).getString("last_selected_server", "");
        GlancesInstance server = monitorFrag.getServerByName(serverNick);
        if (server != null) {
            getSupportActionBar().setSelectedNavigationItem(monitorFrag.getAllGlancesServers().indexOf(server));
        }
        monitorFrag.startUpdates();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.bar, menu);
        ActionBar abar = getSupportActionBar();
        abar.setDisplayShowHomeEnabled(false);
        abar.setDisplayShowTitleEnabled(false);
        serverSpinnerAdapter = new ArrayAdapter<GlancesInstance>(this, android.R.layout.simple_spinner_dropdown_item,
                monitorFrag.getAllGlancesServers());

        ActionBar.OnNavigationListener navListener = new ActionBar.OnNavigationListener() {
            @Override
            public boolean onNavigationItemSelected(int itemPosition, long itemId) {
                List<GlancesInstance> allGlances = monitorFrag.getAllGlancesServers();
                if (allGlances.size() > 0) {
                    GlancesInstance server = allGlances.get(itemPosition);
                    monitorFrag.setServer(server.url.toString(), server.nickName);
                }
                return true;
            }
        };

        abar.setListNavigationCallbacks(serverSpinnerAdapter, navListener);

        return true;
    }

    public static Main getInstance() {
        return instance;
    }

    public static void selectServer(String nickName) {
        List<GlancesInstance> servers = monitorFrag.getAllGlancesServers();
        for (GlancesInstance server : servers) {
            if (server.nickName.equals(nickName)) {
                int index = servers.indexOf(server);
                getInstance().getSupportActionBar().setSelectedNavigationItem(index);
            }
        }
    }

    void saveServers() {
        // we save the server url + name in default prefs
        // and save server url + password in serverPasswords
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        SharedPreferences serverPasswords = getSharedPreferences("serverPasswords", MODE_PRIVATE);
        SharedPreferences.Editor passEditor = serverPasswords.edit();
        int count = 0;
        for (GlancesInstance server : monitorFrag.getAllGlancesServers()) {
            editor.putString(server.nickName, server.url.toString());
            passEditor.putString(server.nickName, server.password);
            count++;
        }
        editor.commit();
        passEditor.commit();
        Log.i(TAG, "Saved " + count + " servers to Preferences");
    }

    void loadServers() {
        monitorFrag.removeAllServers();
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        SharedPreferences serverPasswords = getSharedPreferences("serverPasswords", MODE_PRIVATE);
        Map<String, ?> nameUrlMap = prefs.getAll();
        Set<String> names = nameUrlMap.keySet();
        int count = 0;
        for (String name : names) {
            String url = prefs.getString(name, null);
            String password = serverPasswords.getString(name, null);
            monitorFrag.addServerToList(url, name, password);
            count++;
        }
        Log.i(TAG, "Loaded " + count + " servers from Preferences");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_server:
                AddServerDialog addDialog = new AddServerDialog();
                addDialog.show(getSupportFragmentManager(), getString(R.string.add_server));
                break;
            case R.id.action_edit_server:
                if (monitorFrag.getMonitoredServer() != null) {
                    EditServerDialog editDialog = new EditServerDialog();
                    editDialog.show(getSupportFragmentManager(), getString(R.string.edit_server));
                } else {
                    Toast.makeText(this, getString(R.string.no_server_selected), Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.action_remove_server:
                RemoveServerDialog removeDialog = new RemoveServerDialog();
                removeDialog.show(getSupportFragmentManager(), getString(R.string.remove_server));
                break;
            case R.id.action_about:
                AboutDialog aboutDialog = new AboutDialog();
                aboutDialog.show(getSupportFragmentManager(), getString(R.string.action_about));
                break;
            case R.id.action_settings:
                Intent settingsIntent = new Intent(this, UserSettings.class);
                startActivity(settingsIntent);
                break;
            case R.id.action_menu1:
                // avoid default
                break;
            default:
                Toast.makeText(this, "Unhandled action item", Toast.LENGTH_LONG).show();
                break;
        }
        return true;
    }

    void initializeFragments() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        if (monitorFrag == null) {
            monitorFrag = new MonitorFragment();
        }
        monitorFrag.resetUpdateTime();
        fragmentTransaction.replace(R.id.fragment_container, monitorFrag);
        fragmentTransaction.commit();
        fragmentManager.executePendingTransactions();
        Log.d(TAG, "Finished init of fragment");
    }

    public static class AboutDialog extends SherlockDialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.about, null);
            TextView emailText = (TextView) dialogView.findViewById(R.id.about_email);
            TextView urlText = (TextView) dialogView.findViewById(R.id.about_url);
            TextView glancesText = (TextView) dialogView.findViewById(R.id.about_glances);
            Linkify.addLinks(emailText, Linkify.EMAIL_ADDRESSES);
            Linkify.addLinks(urlText, Linkify.WEB_URLS);
            Linkify.addLinks(glancesText, Linkify.WEB_URLS);
            builder.setView(dialogView);
            return builder.create();
        }

        public AboutDialog() {
            // don't delete this, it keeps the dialog alive on rotation!
        }
    }

    public static class AddServerDialog extends SherlockDialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.add_server, null);
            final EditText urlEdit = (EditText) dialogView.findViewById(R.id.server_url_edittext);
            final EditText portEdit = (EditText) dialogView.findViewById(R.id.server_port_edittext);
            final EditText passwordEdit = (EditText) dialogView.findViewById(R.id.server_password_edittext);
            final EditText nameEdit = (EditText) dialogView.findViewById(R.id.server_name_edittext);
            builder.setView(dialogView);
            builder.setMessage(getString(R.string.add_server))
                    .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Activity act = getSherlockActivity();
                            String url = urlEdit.getText().toString();
                            String port = portEdit.getText().toString();
                            String password = passwordEdit.getText().toString();
                            if (password.length() < 1) {
                                // passing a null password means no authentication required by server
                                password = null;
                            }
                            String nickName = nameEdit.getText().toString();
                            // Make sure input is valid
                            String invalidInput = null;
                            if (port.length() < 1) {
                                port = "61209"; // default port, some users might expect this behavior
                            }
                            if (!isInteger(port)) {
                                invalidInput = String.format(getString(R.string.invalid_port), port);
                            }
                            if (url.length() < 1) {
                                invalidInput = String.format(getString(R.string.invalid_url), url);
                            }
                            if (nickName.length() < 1) {
                                invalidInput = String.format(getString(R.string.invalid_server_name), nickName);
                            }
                            if (invalidInput != null) {
                                Toast.makeText(getActivity(),
                                        invalidInput, Toast.LENGTH_LONG).show();
                            } else {
                                String finalURL = smartURL(url, port);
                                monitorFrag.addServerToList(finalURL, nickName, password);
                                selectServer(nickName);
                            }
                        }
                    })
                    .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            //Toast.makeText(getApplicationContext(), "Canceled add server", Toast.LENGTH_LONG).show();
                        }
                    });
            return builder.create();
        }

        public AddServerDialog() {
            // don't delete this, it keeps the dialog alive on rotation!
        }
    }

    public static class EditServerDialog extends SherlockDialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.add_server, null);
            GlancesInstance server = monitorFrag.getMonitoredServer();
            final String originalName = server.nickName;
            final EditText urlEdit = (EditText) dialogView.findViewById(R.id.server_url_edittext);
            urlEdit.setText(server.url.getHost());
            final EditText portEdit = (EditText) dialogView.findViewById(R.id.server_port_edittext);
            portEdit.setText(Integer.toString(server.url.getPort()));
            final EditText passwordEdit = (EditText) dialogView.findViewById(R.id.server_password_edittext);
            passwordEdit.setText(server.password);
            final EditText nameEdit = (EditText) dialogView.findViewById(R.id.server_name_edittext);
            nameEdit.setText(server.nickName);
            builder.setView(dialogView);
            builder.setMessage(getString(R.string.edit_server))
                    .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Activity act = getSherlockActivity();
                            String url = urlEdit.getText().toString();
                            String port = portEdit.getText().toString();
                            String password = passwordEdit.getText().toString();
                            if (password.length() < 1) {
                                // passing a null password means no authentication required by server
                                password = null;
                            }
                            String nickName = nameEdit.getText().toString();
                            // Make sure input is valid
                            String invalidInput = null;
                            if (port.length() < 1) {
                                port = "61209"; // default port, some users might expect this behavior
                            }
                            if (!isInteger(port)) {
                                invalidInput = String.format("Port: '%s' is not valid", port);
                            }
                            if (url.length() < 1) {
                                invalidInput = String.format("URL: '%s' is not valid", url);
                            }
                            if (nickName.length() < 1) {
                                invalidInput = String.format("Server name: '%s' is not valid ", nickName);
                            }
                            if (invalidInput != null) {
                                Toast.makeText(getInstance(),
                                        invalidInput, Toast.LENGTH_LONG).show();
                            } else {
                                String finalURL = smartURL(url, port);
                                monitorFrag.removeServerFromList(originalName);
                                monitorFrag.addServerToList(finalURL, nickName, password);
                                selectServer(nickName);

                            }
                        }
                    })
                    .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            //Toast.makeText(getApplicationContext(), "Canceled add server", Toast.LENGTH_LONG).show();
                        }
                    });
            return builder.create();
        }

        public EditServerDialog() {
            // stops crash on rotation
        }
    }

    public static class RemoveServerDialog extends SherlockDialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            final String[] serverNames = monitorFrag.getServerNames();
            builder.setTitle(getString(R.string.remove_server))
            .setItems(serverNames, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int selection) {
                    boolean removed = monitorFrag.removeServerFromList(serverNames[selection]);
                    if (removed) {
                        Toast.makeText(getInstance(), getString(R.string.server_removed) + serverNames[selection],
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
            return builder.create();
        }

        public RemoveServerDialog() {
            // don't delete this, it keeps the dialog alive on rotation!
        }
    }

    /**
     * Smartly concatenate a url + port combo
     * @param userURL
     * @param userPort
     * @return
     */
    static String smartURL(String userURL, String userPort) {
        // cut out user inputted http:// if its there
        String url = userURL.replace("http://", "");
        // now we make sure it's there by doing it ourselves
        url = "http://" + url + ":" + userPort;
        return url;
    }

    static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
        } catch(NumberFormatException e) {
            return false;
        }
        return true;
    }
}
