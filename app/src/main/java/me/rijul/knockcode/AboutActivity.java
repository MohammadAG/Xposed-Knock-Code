package me.rijul.knockcode;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.view.Gravity;
import android.widget.TextView;

/**
 * Created by rijul on 16/12/15.
 */
public class AboutActivity extends PreferenceActivity {
    private static final String URL_HELP = "http://repo.xposed.info";
    private static final String URL_LICENSE = "https://www.gnu.org/licenses/gpl-3.0.txt";
    private static final String URL_SOURCE = "https://github.com/Rijul-Ahuja/Xposed-Knock-Code";
    private static final String URL_MOHAMMAD = "https://mohammadag.xceleo.org/wiki";
    private static final String PROFILE_RIJUL = "rijul.ahuja";

    @Override
    @SuppressWarnings("deprecation")
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.about);
        ActionBar actionBar = getActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        findPreference("help_key").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference arg0) {
                startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(URL_HELP)));
                return false;
            }
        });

        findPreference("license_key").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(URL_LICENSE)));
                return false;
            }
        });

        findPreference("disclaimer_key").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog.Builder builder = new AlertDialog.Builder(AboutActivity.this);
                builder.setTitle(R.string.about_disclaimer_title);
                final TextView message = new TextView(AboutActivity.this);
                message.setText(R.string.about_disclaimer_message);
                message.setGravity(Gravity.CENTER);
                builder.setView(message);
                builder.setPositiveButton(" OK ", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
                Dialog dialog = builder.create();
                dialog.show();
                return true;
            }
        });

        findPreference("source_key").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(URL_SOURCE)));
                return false;
            }
        });

        findPreference("mohammad_key").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(URL_MOHAMMAD)));
                return false;
            }
        });

        findPreference("rijul_key").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("fb://profile/" + PROFILE_RIJUL)));
                } catch (Exception e) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/" + PROFILE_RIJUL)));
                }
                return false;
            }
        });
    }
}
