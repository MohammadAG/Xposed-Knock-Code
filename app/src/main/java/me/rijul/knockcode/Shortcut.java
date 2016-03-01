package me.rijul.knockcode;

import java.util.ArrayList;

/**
 * Created by rijul on 1/3/16.
 */
public class Shortcut {
    String passCode;
    String uri;
    String friendlyName;

    Shortcut(String code, String uri, String name) {
        passCode = code;
        this.uri = uri;
        this.friendlyName = name;
    }

    @Override
    public String toString() {
        return "passCode : " + passCode + "\nuri : " + uri + "\nfriendlyName : " + friendlyName;
    }
}
