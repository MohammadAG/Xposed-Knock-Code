package com.mohammadag.knockcode;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;

public class ResourceHelper {
	public static String getString(Context context, int id) {
		return getOwnResources(context).getString(id);
	}

	public static String getString(Context context, int id, Object... args) {
		return getOwnResources(context).getString(id, args);
	}

	public static Resources getResourcesForPackage(Context context, String packageName) {
		try {
			return context.getPackageManager().getResourcesForApplication(packageName);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		return null;
	}

	public static Resources getOwnResources(Context context) {
		return getResourcesForPackage(context, "com.mohammadag.knockcode");
	}
}
