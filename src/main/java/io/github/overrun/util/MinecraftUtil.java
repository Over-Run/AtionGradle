package io.github.overrun.util;

import java.io.File;
import java.util.Locale;

public class MinecraftUtil {

	public static File getMinecraftDirs() {
		File minecraftFolder;
		if (getOsName().contains("win")) {
			minecraftFolder = new File(System.getenv("APPDATA"), ".minecraft");
		} else if (getOsName().contains("mac")) {
			minecraftFolder = new File(System.getProperty("user.home"), "Library" + "/" + "Application Support" + "/" + "minecraft");
		} else {
			minecraftFolder = new File(System.getProperty("user.home"), ".minecraft");
		}
		return minecraftFolder;
	}

	private static String getOsName() {
		return System.getProperty("os.name").toLowerCase(Locale.ROOT);
	}

}
