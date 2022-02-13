package io.github.overrun.util;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

public class UrlUtil {
	public static final String game_url = "https://launchermeta.mojang.com/mc/game/version_manifest.json"; // new downloading
//	public static final String game_url = "https://launchermeta.mojang.com/mc/game.version_mainfest_v2.json";
	public static final String game_libraries = "https://libraries.minecraft.net";
	public static final String game_resource = "https://resources.download.minecraft.net";
	public static String readString(String link) {
		StringBuffer stringBuffer = new StringBuffer();
		try {
			URL url = new URL(link);
			URLConnection urlConnection = url.openConnection();
			HttpURLConnection httpURLConnection = null;
			if (urlConnection instanceof HttpURLConnection) {
				httpURLConnection = (HttpURLConnection) urlConnection;
			}
			if (httpURLConnection != null) {
				IOUtils.readLines(httpURLConnection.getInputStream(), StandardCharsets.UTF_8).forEach(line -> stringBuffer.append(line).append("\n"));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return stringBuffer.toString();
	}

	public static byte[] readFile(String link){
		byte[] bytes = null;
		try {
			URL url = new URL(link);
			URLConnection urlConnection = url.openConnection();
			HttpURLConnection httpURLConnection = null;
			if (urlConnection instanceof HttpURLConnection) {
				httpURLConnection = (HttpURLConnection) urlConnection;
			}
			if (httpURLConnection != null) {
				bytes = IOUtils.toByteArray(httpURLConnection.getInputStream());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return bytes;
	}

}
