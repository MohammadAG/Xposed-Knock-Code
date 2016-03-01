package me.rijul.knockcode;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * Created by rijul on 1/3/16.
 */
public class ShortcutAdapter extends ArrayAdapter<Shortcut> {
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
        final Shortcut shortcut = getItem(position);
        final TextView label = (TextView) convertView;
        label.setTag(shortcut);
        label.setText(shortcut.friendlyName);
        return convertView;
    }
}
