package me.rijul.knockcode;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;

public class ResourceHelper {
	public static String getString(Context context, int id) {
		return getOwnResources(context).getString(id);
	}

	public static String getString(Context context, int id, Object... args) {
		return getOwnResources(context).getString(id, args);
	}

	public static String getString(Context ctx, String packageName, String resName, String resType) {
		try {
			ctx = ctx.createPackageContext(packageName, Context.CONTEXT_RESTRICTED);
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
			return null;
		}
		return ctx.getResources().getString(
				ctx.getResources().getIdentifier(resName, resType, ctx.getPackageName()));
	}


	public static Resources getResourcesForPackage(Context context, String packageName) {
		try {
			context = context.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY);
			return context.getResources();
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		return null;
	}

	public static Resources getOwnResources(Context context) {
		return getResourcesForPackage(context, "me.rijul.knockcode");
	}

	public static int getResource(Context ctx, String packageName, String resName, String resType) {
		try {
			ctx = ctx.createPackageContext(packageName, Context.CONTEXT_RESTRICTED);
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
			return -1;
		}
		return ctx.getResources().getDimensionPixelOffset(
				ctx.getResources().getIdentifier(resName, resType, ctx.getPackageName()));
	}
}
