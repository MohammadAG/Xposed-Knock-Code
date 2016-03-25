package me.rijul.knockcode;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.view.Gravity;
import android.view.MenuItem;
import android.widget.TextView;

/**
 * Created by rijul on 25/3/16.
 */
public class AboutActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        getFragmentManager().beginTransaction().replace(R.id.fragment_container, new AboutFragment()).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static class AboutFragment extends PreferenceFragment {
        private static final String FACEBOOK_RIJUL = "rijul.ahuja";

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.about);
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
            String key = preference.getKey();
            if (key==null)
                return false;
            else if (key.equals(Utils.ABOUT_DISCLAIMER)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.about_disclaimer_title);
                final TextView message = new TextView(getActivity());
                message.setText(R.string.about_disclaimer_message);
                message.setGravity(Gravity.CENTER);
                builder.setView(message);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
                Dialog dialog = builder.create();
                dialog.show();
                return true;
            } else if (key.equals(Utils.ABOUT_RIJUL)) {
                openFacebook(FACEBOOK_RIJUL);
                return true;
            }
            return false;
        }

        private void openFacebook(String profile) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("fb://profile/" + profile)));
            } catch (Exception e) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/" + profile)));
            }
        }
    }

}
