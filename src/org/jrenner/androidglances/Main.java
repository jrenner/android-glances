package org.jrenner.androidglances;

import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.SpinnerAdapter;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class Main extends SherlockFragmentActivity {
	private static final String TAG = "Glances-Main";
	private MonitorFragment monitorFrag;
	private SpinnerAdapter serverSpinnerAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		if (monitorFrag == null) {
			initializeFragments();
		} else {
			Log.d(TAG, "monitorFrag already exists, not initializing");
		}
		ActionBar abar = getSupportActionBar();
		abar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

		loadServers();
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
				GlancesInstance server = allGlances.get(itemPosition);
				monitorFrag.setServer(server.url.toString(), server.nickName);
				return true;
			}
		};

		abar.setListNavigationCallbacks(serverSpinnerAdapter, navListener);

		return true;
	}

	public void selectServer(String nickName) {
		List<GlancesInstance> servers = monitorFrag.getAllGlancesServers();
		for (GlancesInstance server : servers) {
			if (server.nickName.equals(nickName)) {
				int index = servers.indexOf(server);
				getSupportActionBar().setSelectedNavigationItem(index);
			}
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		saveServers();
	}

	void saveServers() {
		// we save the server url + name in default prefs
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
		case R.id.action_quit:
			shutdownApp();
			break;
		case R.id.action_add_server:
			AddServerDialog addDialog = new AddServerDialog();
			addDialog.show(getSupportFragmentManager(), getString(R.string.add_server));
			break;
		case R.id.action_edit_server:
			EditServerDialog editDialog = new EditServerDialog();
			editDialog.show(getSupportFragmentManager(), getString(R.string.edit_server));
			break;
		case R.id.action_remove_server:
			RemoveServerDialog removeDialog = new RemoveServerDialog();
			removeDialog.show(getSupportFragmentManager(), getString(R.string.remove_server));
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
		monitorFrag = MonitorFragment.getInstance();
		fragmentTransaction.add(R.id.fragment_container, monitorFrag);
		fragmentTransaction.commit();
		fragmentManager.executePendingTransactions();
		if (monitorFrag == null)
			Log.e(TAG, "monitorFrag is null after trying to init");
		Log.d(TAG, "Finished init of fragment");
	}

	void shutdownApp() {
		// delete sharedprefs for DEBUG
		Log.w(TAG, "Trying to shutdown");
		monitorFrag.shutdown();
		finish();
	}

	class AddServerDialog extends SherlockDialogFragment {
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
						invalidInput = String.format("Port: '%s' is not a valid", port);
					}
					if (url.length() < 1) {
						invalidInput = String.format("URL: '%s' is too short", url);
					}
					if (nickName.length() < 1) {
						invalidInput = String.format("Server name: '%s' is too short", nickName);
					}
					if (invalidInput != null) {
						Toast.makeText(getApplicationContext(),
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
	}

	class EditServerDialog extends SherlockDialogFragment {
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
						Toast.makeText(getApplicationContext(),
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
	}

	class RemoveServerDialog extends SherlockDialogFragment {
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			final String[] serverNames = monitorFrag.getServerNames();
			builder.setTitle(getString(R.string.remove_server))
			.setItems(serverNames, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int selection) {
					boolean removed = monitorFrag.removeServerFromList(serverNames[selection]);
					if (removed) {
						Toast.makeText(getApplicationContext(), "Removed " + serverNames[selection],
								Toast.LENGTH_SHORT).show();
					}
				}
			});
			return builder.create();
		}
	}

	/**
	 * Smartly concatenate a url + port combo
	 * @param userURL
	 * @param userPort
	 * @return
	 */
	String smartURL(String userURL, String userPort) {
		// cut out user inputted http:// if its there
		String url = userURL.replace("http://", "");
		// now we make sure it's there by doing it ourselves
		url = "http://" + url + ":" + userPort;
		Log.i(TAG, "Final URL from user input: " + url);
		return url;
	}

	boolean isInteger(String s) {
		try {
			Integer.parseInt(s);
		} catch(NumberFormatException e) {
			return false;
		}
		return true;
	}
}
