package me.rijul.knockcode;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;

public class CustomShortcutActivity extends AppCompatActivity implements ShortcutPickHelper.OnPickListener {
    private static final int MENU_ID_EDIT_CODE = 1, MENU_ID_EDIT_TARGET = 2, MENU_ID_DELETE = 3;

    ListView mListView;
    ProgressDialog mProgressDialog;
    ShortcutPickHelper mPicker;
    SettingsHelper mSettingHelper;
    ShortcutAdapter mAdapter;
    ShortcutsLoadTask mTask;

    private final Comparator<Shortcut> mSorter = new Comparator<Shortcut>() {
        public int compare(Shortcut object1, Shortcut object2) {
            return object1.friendlyName.compareTo(object2.friendlyName);
        }
    };

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
                //startActivityForResult(new Intent(CustomShortcutActivity.this, MainActivity.class), MainActivity.GET_A_CODE);
                mProgressDialog.show();
                mPicker.pickShortcut(null, null, 0);
            }
        });

        mSettingHelper = new SettingsHelper(this);
        mListView = (ListView) findViewById(R.id.shortcuts);
        mPicker = new ShortcutPickHelper(this, this);
        mAdapter = new ShortcutAdapter(this);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                view.showContextMenu();
            }
        });
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setMessage(getString(R.string.loading));
        registerForContextMenu(mListView);
        loadShortcuts();
    }

    private void loadShortcuts() {
        if (mTask != null && mTask.getStatus() != AsyncTask.Status.FINISHED) {
            mTask.cancel(true);
        }
        mTask = (ShortcutsLoadTask) new ShortcutsLoadTask().execute();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode==RESULT_OK) {
            switch (requestCode) {
                case MainActivity.GET_A_CODE :
                    Toast.makeText(CustomShortcutActivity.this, R.string.reboot_required, Toast.LENGTH_SHORT).show();
                    loadShortcuts();
                    break;
                case ShortcutPickHelper.REQUEST_CREATE_SHORTCUT:
                case ShortcutPickHelper.REQUEST_PICK_APPLICATION:
                case ShortcutPickHelper.REQUEST_PICK_SHORTCUT:
                    mPicker.onActivityResult(requestCode, resultCode, data);
                    break;
            }
        }
        else
            mProgressDialog.dismiss();
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        intent.putExtra("requestCode", Integer.toString(requestCode));
        super.startActivityForResult(intent, requestCode);
    }

    @Override
    public void shortcutPicked(String uri, String friendlyName, boolean isApplication) {
        if (TextUtils.isEmpty(uri) || TextUtils.isEmpty(friendlyName)) {
            return;
        }
        Intent intent = new Intent(CustomShortcutActivity.this, MainActivity.class);
        intent.putExtra("uri", uri);
        intent.putExtra("name", friendlyName);
        startActivityForResult(intent, MainActivity.GET_A_CODE);
    }

    private class ShortcutsLoadTask extends AsyncTask<Void, Shortcut, Integer> {
        public static final int STATUS_CANCELLED = -1;
        public static final int STATUS_OK = 1;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog.show();
            mAdapter.setNotifyOnChange(false);
            mAdapter.clear();
            findViewById(R.id.fab).setEnabled(false);
        }

        @Override
        protected Integer doInBackground(Void... params) {
            if (isCancelled()) return STATUS_CANCELLED;
            Map<String, ?> allEntries = mSettingHelper.getAll();
            for(Map.Entry<String, ?> entry : allEntries.entrySet())
                //uri_1,2,3,4 : (uri)|friendlyName
                try {
                    if ((entry.getKey()!=null) && (entry.getKey().startsWith("uri_"))) {
                        Shortcut shortcut = new Shortcut(entry.getKey().substring(4),
                                ((String) entry.getValue()).split("\\|")[0], ((String) entry.getValue()).split("\\|")[1]);
                        publishProgress(shortcut);
                    }
                } catch (NullPointerException e) {
                }
            return STATUS_OK;
        }

        @Override
        protected void onProgressUpdate(Shortcut... values) {
            super.onProgressUpdate(values);
            final ShortcutAdapter adapter = mAdapter;
            adapter.setNotifyOnChange(false);
            adapter.addAll(values);
            adapter.sort(mSorter);
            adapter.notifyDataSetChanged();
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            mProgressDialog.dismiss();
            findViewById(R.id.fab).setEnabled(true);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        menu.setHeaderTitle(((Shortcut) info.targetView.getTag()).friendlyName);
        menu.add(0, MENU_ID_DELETE, 0, R.string.delete_shortcut);
        Toast.makeText(CustomShortcutActivity.this, ((Shortcut) info.targetView.getTag()).uri, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo)
                item.getMenuInfo();
        final Shortcut shortcut = (Shortcut) menuInfo.targetView.getTag();
        switch (item.getItemId()) {
            case MENU_ID_DELETE:
                deleteShortcut(shortcut);
                return true;
        }

        return super.onContextItemSelected(item);
    }

    private void deleteShortcut(Shortcut shortcut) {
        mSettingHelper.remove("uri_" + shortcut.passCode);
        mAdapter.remove(shortcut);
        loadShortcuts();
    }
}
