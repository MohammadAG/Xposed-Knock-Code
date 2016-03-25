package me.rijul.knockcode;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ListFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;

/**
 * Created by rijul on 3/3/16.
 */
public class CustomShortcutActivity extends Activity {
    private static SettingsHelper mSettingsHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mSettingsHelper = new SettingsHelper(this);
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        getFragmentManager().beginTransaction().replace(R.id.fragment_container, new CustomShortcutFragment()).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.toolbar_add:
                ((CustomShortcutFragment) getFragmentManager().findFragmentById(R.id.fragment_container)).mProgressDialog.show();
                ((CustomShortcutFragment) getFragmentManager().findFragmentById(R.id.fragment_container)).
                        mPicker.pickShortcut(null, null, 0);
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode==RESULT_OK) {
            switch (requestCode) {
                case ShortcutPickHelper.REQUEST_CREATE_SHORTCUT:
                case ShortcutPickHelper.REQUEST_PICK_APPLICATION:
                case ShortcutPickHelper.REQUEST_PICK_SHORTCUT:
                    ((CustomShortcutFragment) getFragmentManager().findFragmentById(R.id.fragment_container)).
                            mPicker.onActivityResult(requestCode, resultCode, data);
                    break;
            }
        }
        else
            ((CustomShortcutFragment) getFragmentManager().findFragmentById(R.id.fragment_container)).mProgressDialog.dismiss();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_custom_shortcuts, menu);
        return true;
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        intent.putExtra("requestCode", Integer.toString(requestCode));
        super.startActivityForResult(intent, requestCode);
    }

    public static class CustomShortcutFragment extends ListFragment implements ShortcutPickHelper.OnPickListener {
        private static final int MENU_ID_DELETE = 1, MENU_ID_EDIT = 2;
        public ShortcutPickHelper mPicker;
        public ProgressDialog mProgressDialog;
        private GesturesLoadTask mTask;
        private final Comparator<NamedShortcut> mSorter = new Comparator<NamedShortcut>() {
            public int compare(NamedShortcut object1, NamedShortcut object2) {
                return object1.name.compareTo(object2.name);
            }
        };

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_list, container, false);
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            mPicker = new ShortcutPickHelper(getActivity(), this);
            getListView().setAdapter(new ShortcutAdapter(getActivity()));
            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    getActivity().onBackPressed();
                }
            });
            mProgressDialog.setMessage(getActivity().getString(R.string.loading));
            registerForContextMenu(getListView());
            getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    view.showContextMenu();
                }
            });
            loadShortcuts();
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (resultCode==MainActivity.RESULT_OK) {
                switch (requestCode) {
                    case MainActivity.NEW_SHORTCUT:
                        mProgressDialog.dismiss();
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


        public void loadShortcuts() {
            if (mTask != null && mTask.getStatus() != AsyncTask.Status.FINISHED) {
                mTask.cancel(true);
            }
            mTask = (GesturesLoadTask) new GesturesLoadTask().execute();
        }

        @Override
        public void shortcutPicked(String uri, String friendlyName, boolean isApplication) {
            if (TextUtils.isEmpty(uri) || TextUtils.isEmpty(friendlyName)) {
                return;
            }
            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.putExtra("uri", uri);
            intent.putExtra("name", friendlyName);
            startActivityForResult(intent, MainActivity.NEW_SHORTCUT);
        }

        @Override
        public void startActivityForResult(Intent intent, int requestCode) {
            intent.putExtra("requestCode", Integer.toString(requestCode));
            super.startActivityForResult(intent, requestCode);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v,
                                        ContextMenu.ContextMenuInfo menuInfo) {
            super.onCreateContextMenu(menu, v, menuInfo);
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            menu.setHeaderTitle(((NamedShortcut) info.targetView.getTag()).name);
            menu.add(0, MENU_ID_EDIT, 0, R.string.custom_shortcuts_edit);
            menu.add(0, MENU_ID_DELETE, 0, R.string.custom_shortcuts_delete);
        }

        @Override
        public boolean onContextItemSelected(MenuItem item) {
            final AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo)
                    item.getMenuInfo();
            final NamedShortcut shortcut = (NamedShortcut) menuInfo.targetView.getTag();
            switch (item.getItemId()) {
                case MENU_ID_EDIT:
                    editShortcut(shortcut);
                    break;
                case MENU_ID_DELETE:
                    deleteShortcut(shortcut);
                    return true;
            }

            return super.onContextItemSelected(item);
        }

        private void editShortcut(NamedShortcut shortcut) {
            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.putExtra("uri", shortcut.uri);
            intent.putExtra("name", shortcut.name);
            intent.putExtra("passcode", Utils.passcodeToString(shortcut.passcode));
            startActivityForResult(intent, MainActivity.NEW_SHORTCUT);
        }

        private void deleteShortcut(NamedShortcut shortcut) {
            mSettingsHelper.removeShortcut(shortcut.passcode);
            getActivity().sendBroadcast(new Intent(Utils.SETTINGS_CHANGED));
            ShortcutAdapter shortcutAdapter = ((ShortcutAdapter) getListView().getAdapter());
            shortcutAdapter.setNotifyOnChange(false);
            shortcutAdapter.remove(shortcut);
            shortcutAdapter.sort(mSorter);
            shortcutAdapter.notifyDataSetChanged();
            loadShortcuts();
        }


        private class NamedShortcut {
            ArrayList<Integer> passcode;
            String name;
            String uri;
        }

        private class ShortcutAdapter extends ArrayAdapter<NamedShortcut> {
            private final LayoutInflater mInflater;

            public ShortcutAdapter(Context context) {
                super(context, 0);
                mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = mInflater.inflate(android.R.layout.simple_list_item_1, parent, false);
                }
                final NamedShortcut namedGesture = getItem(position);
                final TextView label = (TextView) convertView;
                label.setTag(namedGesture);
                label.setText(namedGesture.name);
                return convertView;
            }
        }

        private class GesturesLoadTask extends AsyncTask<Void, NamedShortcut, Integer> {
            public static final int STATUS_OK = 0;
            private ShortcutAdapter mAdapter;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mProgressDialog.show();
                mAdapter = (ShortcutAdapter) getListView().getAdapter();
                mAdapter.setNotifyOnChange(false);
                mAdapter.clear();
            }

            @Override
            protected Integer doInBackground(Void... params) {
                Map<String, ?> allEntries = mSettingsHelper.getAll();
                for(Map.Entry<String, ?> entry : allEntries.entrySet())
                    try {
                        if (entry.getKey().startsWith(Utils.PREFIX_SHORTCUT)) {
                            String value = (String) entry.getValue();
                            final int separator = value.indexOf('|');
                            if (separator == -1)
                                continue;
                            NamedShortcut namedShortcut = new NamedShortcut();
                            namedShortcut.uri = value.substring(0, separator);
                            namedShortcut.name = value.substring(separator + 1);
                            namedShortcut.passcode = Utils.stringToPasscode(entry.getKey().substring(9));
                            publishProgress(namedShortcut);
                        }
                    } catch (NullPointerException ignored) {}
                return STATUS_OK;
            }

            @Override
            protected void onProgressUpdate(NamedShortcut... values) {
                super.onProgressUpdate(values);

                final ShortcutAdapter adapter = mAdapter;
                adapter.setNotifyOnChange(false);

                for (NamedShortcut gesture : values) {
                    adapter.add(gesture);
                }

                adapter.sort(mSorter);
                adapter.notifyDataSetChanged();
            }

            @Override
            protected void onPostExecute(Integer result) {
                super.onPostExecute(result);
                mProgressDialog.dismiss();
            }
        }

    }

}
