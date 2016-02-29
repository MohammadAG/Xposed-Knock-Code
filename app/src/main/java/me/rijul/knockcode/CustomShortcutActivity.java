package me.rijul.knockcode;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class CustomShortcutActivity extends AppCompatActivity implements LoadShortcuts.AsyncResponse, LoadApps.AsyncResponse,
        GetPackageNameForApp.AsyncResponse, GetKeyForValue.AsyncResponse {
    ListView mListView;
    ArrayList<Integer> mNewCode;
    ProgressDialog mProgressDialog;

    ListView.OnItemClickListener appListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String appName = (String) mListView.getItemAtPosition(position);
            mProgressDialog.show();
            (new GetPackageNameForApp(CustomShortcutActivity.this, GetPackageNameForApp.APP_LISTENER)).execute(appName);
        }
    };

    ListView.OnItemClickListener shortcutListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String appName = (String) mListView.getItemAtPosition(position);
            mProgressDialog.show();
            (new GetPackageNameForApp(CustomShortcutActivity.this, GetPackageNameForApp.SHORTCUT_LISTENER)).execute(appName);
        }
    };

    private String getString(ArrayList<Integer> passcode) {
        String string = "";
        for (int i = 0; i < passcode.size(); i++) {
            string += String.valueOf(passcode.get(i));
            if (i != passcode.size()-1) {
                string += ",";
            }
        }
        return string;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_shortcut);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(new Intent(CustomShortcutActivity.this, MainActivity.class), MainActivity.GET_A_CODE);
            }
        });


        mListView = (ListView) findViewById(R.id.shortcuts);
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setMessage(getString(R.string.loading));
        mProgressDialog.show();
        (new LoadShortcuts(this)).execute();
    }

    @Override
    public void shortcutsFinish(ArrayList<String> result) {
        mProgressDialog.dismiss();
        ((TextView) findViewById(R.id.shortcut_hint_text)).setText(R.string.tap_delete_shortcut);
        findViewById(R.id.shortcut_hint).setVisibility(result.isEmpty() ? View.GONE : View.VISIBLE);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, result);
        findViewById(R.id.fab).setVisibility(View.VISIBLE);
        mListView.setAdapter(arrayAdapter);
        mListView.setOnItemClickListener(shortcutListener);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode==RESULT_OK) {
            mNewCode = stringToPasscode(data.getExtras().getString("newCode"));
            findViewById(R.id.fab).setVisibility(View.GONE);
            mProgressDialog.show();
            (new LoadApps(this)).execute();
        }
    }

    private ArrayList<Integer> stringToPasscode(String string) {
        ArrayList<Integer> passcode = new ArrayList<>();
        String[] integers = string.split(",");
        for (String digitString : integers) {
            passcode.add(Integer.parseInt(digitString));
        }
        return passcode;
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        intent.putExtra("requestCode", Integer.toString(requestCode));
        super.startActivityForResult(intent, requestCode);
    }

    @Override
    public void appsFinish(ArrayList<String> result) {
        mProgressDialog.dismiss();
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, result);
        mListView.setAdapter(arrayAdapter);
        mListView.setOnItemClickListener(appListener);
        findViewById(R.id.shortcut_hint).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.shortcut_hint_text)).setText(R.string.select_app);
    }

    @Override
    public void foundPackageName(String packageName, int requestCode) {
        mProgressDialog.dismiss();
        if (requestCode==GetPackageNameForApp.APP_LISTENER) {
            (new SettingsHelper(CustomShortcutActivity.this)).edit().putString("package_"
                    + getString(mNewCode), packageName).commit();
            Toast.makeText(CustomShortcutActivity.this, R.string.reboot_required, Toast.LENGTH_LONG).show();
            mProgressDialog.show();
            (new LoadShortcuts(CustomShortcutActivity.this)).execute();
        } else if (requestCode==GetPackageNameForApp.SHORTCUT_LISTENER) {
            mProgressDialog.show();
            (new GetKeyForValue(this)).execute(packageName);
        }
    }

    @Override
    public void foundKey(String key) {
        mProgressDialog.dismiss();
        (new SettingsHelper(CustomShortcutActivity.this)).edit().remove(key).commit();
        Toast.makeText(CustomShortcutActivity.this, R.string.reboot_required, Toast.LENGTH_LONG).show();
        mProgressDialog.show();
        (new LoadShortcuts(CustomShortcutActivity.this)).execute();
    }
}
