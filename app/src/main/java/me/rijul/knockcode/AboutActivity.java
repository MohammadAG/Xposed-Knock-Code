package me.rijul.knockcode;

import android.preference.PreferenceGroup;
import android.support.v7.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.MenuItem;
import android.widget.TextView;

/**
 * Created by rijul on 16/12/15.
 */
public class AboutActivity extends AppCompatPreferenceActivity {
    private static final String PROFILE_RIJUL = "rijul.ahuja";
    private static final String TRANSLATOR_PT_BR_GABRIEL = "zzzgabriel";
    private static final String TRANSLATOR_ES_MX_ALAN = "alan.yadir.5";

    @Override
    @SuppressWarnings("deprecation")
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        addPreferencesFromResource(R.xml.about);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.abc_ic_ab_back_mtrl_am_alpha);

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

        findPreference("rijul_key").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                return viewFacebookProfile(PROFILE_RIJUL);
            }
        });

        findPreference("translator_pt_br_gabriel").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                return viewFacebookProfile(TRANSLATOR_PT_BR_GABRIEL);
            }
        });

        findPreference("translator_es_mx_alan").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                return viewFacebookProfile(TRANSLATOR_ES_MX_ALAN);
            }
        });

        ((PreferenceGroup) findPreference("hidden")).removeAll();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean viewFacebookProfile(String profile) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("fb://profile/" + profile)));
        } catch (Exception e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/" + profile)));
        }
        return false;
    }
}
