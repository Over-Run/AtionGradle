package io.github.overrun.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.overrun.AtionGradleExtensions;

import java.io.File;
import java.util.*;

import static io.github.overrun.util.UrlUtil.readString;

public class MinecraftUtil {
	public static File getMinecraftDirs() {
		File minecraftFolder;

		if (getOsName().contains("win")) {// windows
			minecraftFolder = new File(System.getenv("APPDATA"), ".minecraft");
		} else if (getOsName().contains("mac")) {// mac
			minecraftFolder = new File(System.getProperty("user.home"), "Library" + "/" + "Application Support" + "/" + "minecraft");
		} else {// linux
			minecraftFolder = new File(System.getProperty("user.home"), ".minecraft");
		}
		return minecraftFolder;
	}

	private static String getOsName() {
		return System.getProperty("os.name").toLowerCase(Locale.ROOT);
	}

	public static File getGameDir(AtionGradleExtensions ationGradleExtensions) {
		File game = new File(ationGradleExtensions.getUserCache(), "game");
		if (!game.exists()) {
			//noinspection ResultOfMethodCallIgnored
			game.mkdir();
		}
		return game;
	}

	public static File getClientFile(AtionGradleExtensions ationGradleExtensions) {
		return new File(getGameDir(ationGradleExtensions), ationGradleExtensions.gameVersion + "/" + ationGradleExtensions.gameVersion + "-client.jar");
	}

	public static File getClientCleanFile(AtionGradleExtensions ationGradleExtensions) {
		return new File(getGameDir(ationGradleExtensions), ationGradleExtensions.gameVersion + "/" + ationGradleExtensions.gameVersion + "-client-clean.jar");
	}

	public static String getJson(String version) {
		String jsonUrl = "";
		for (JsonElement versions : new Gson().fromJson(readString(UrlUtil.game_url), JsonObject.class).get("versions").getAsJsonArray()) {
			if (versions.getAsJsonObject().get("id").getAsString().equals(version)) {
				jsonUrl = versions.getAsJsonObject().get("url").getAsString();
			}
		}
		return readString(jsonUrl);
	}

	public static File getMappingDir(AtionGradleExtensions ationGradleExtensions) {
		File mapping = new File(ationGradleExtensions.getUserCache(), "mapping");
		if (!mapping.exists()) {
			mapping.mkdir();
		}
		return mapping;
	}

	public static File getClientMappingFile(AtionGradleExtensions ationGradleExtensions) {
		return new File(getMappingDir(ationGradleExtensions), ationGradleExtensions.gameVersion + "-client.txt");
	}

	public static File getClientAssetsDir(AtionGradleExtensions ationGradleExtensions) {
		File assets = new File(ationGradleExtensions.getUserCache(), "assets");
		if (!assets.exists()) {
			assets.mkdir();
		}
		return assets;
	}

	public static File getClientNativeDir(AtionGradleExtensions ationGradleExtensions) {
		File file = new File(MinecraftUtil.getGameDir(ationGradleExtensions), ationGradleExtensions.gameVersion + "/" + ationGradleExtensions.gameVersion + "-native");
		if (!file.exists()) {
			file.mkdir();
		}
		return file;
	}

	public static File getClientNativeJarDir(AtionGradleExtensions ationGradleExtensions) {
		File jars = new File(MinecraftUtil.getClientNativeDir(ationGradleExtensions), "jars");
		if (!jars.exists()) {
			jars.mkdir();
		}
		return jars;
	}

	public static File getClientNativeFileDir(AtionGradleExtensions ationGradleExtensions) {
		File jars = new File(MinecraftUtil.getClientNativeDir(ationGradleExtensions), "natives");
		if (!jars.exists()) {
			jars.mkdir();
		}
		return jars;
	}

	public static List<String> getLibraries(AtionGradleExtensions ationGradleExtensions) {
		LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<>();
		for (JsonElement jsonElement : new Gson().fromJson(getJson(ationGradleExtensions.gameVersion), JsonObject.class).get("libraries").getAsJsonArray()) {
			if (jsonElement.getAsJsonObject().has("natives")) {
				continue;
			}

			String name = jsonElement.getAsJsonObject().get("name").getAsString();
			linkedHashMap.put(name.substring(0, name.lastIndexOf(":")), name.substring(name.lastIndexOf(":")));
		}
		List<String> libraries = new ArrayList<>();
		for (Map.Entry<String, String> stringStringEntry : linkedHashMap.entrySet()) {
			libraries.add(stringStringEntry.getKey() + stringStringEntry.getValue());
		}
		return libraries;
	}

	public static List<String> getNatives(AtionGradleExtensions ationGradleExtensions) {
		List<String> libraries = new ArrayList<>();
		for (JsonElement jsonElement : new Gson().fromJson(getJson(ationGradleExtensions.gameVersion), JsonObject.class).get("libraries").getAsJsonArray()) {
			JsonObject downloads = jsonElement.getAsJsonObject().get("downloads").getAsJsonObject();
			if (downloads.has("classifiers")) {
				String name = "natives-linux";
				if (getOsName().contains("win")) {
					name = "natives-windows";
				} else if (getOsName().contains("mac")) {
					name = "natives-macos";
				}
				JsonObject classifiers = downloads.get("classifiers").getAsJsonObject();
				if (classifiers.has(name)) {
					libraries.add(downloads.get("classifiers").getAsJsonObject().get(name).getAsJsonObject().get("url").getAsString());
				}
			}
		}
		return libraries;
	}

}
