package io.github.overrun.task;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.overrun.util.MinecraftUtil;
import io.github.overrun.util.UrlUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.impldep.org.apache.commons.compress.archivers.zip.ZipUtil;

import java.io.*;
import java.net.URL;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import static io.github.overrun.jni.Download.download;
import static io.github.overrun.util.Universal.isMkdirs;
import static io.github.overrun.util.UrlUtil.readString;
import static java.io.File.separatorChar;

public class DownLoadingAll extends Task{
	public final String get_user_dir = System.getProperty("user.dir");
	public final String version_mainfest = getProject().getGradle().getGradleUserHomeDir().getAbsolutePath() + separatorChar + "version_mainfest.json";
	public String jsonUrl;
	public String getJson(String version) {
		for (JsonElement versions : new Gson().fromJson(readString(UrlUtil.game_url), JsonObject.class).get("versions").getAsJsonArray()) {
			if (versions.getAsJsonObject().get("id").getAsString().equals(version)) {
				jsonUrl = versions.getAsJsonObject().get("url").getAsString();
			}
		}
		return readString(jsonUrl);
	}
	public void getBasicDownloadDll() throws IOException {
		URL url =  new URL("https://github.com/Over-Run/maven/raw/main/DownloadJni.dll");
		BufferedInputStream bis = new BufferedInputStream(url.openStream());
		FileOutputStream fis = new FileOutputStream(get_user_dir + separatorChar + "DownloadJni.dll");
		byte[] buffer = new byte[1024];
		@SuppressWarnings("UnusedAssignment") int count = 0;
		while ((count = bis.read(buffer, 0, 1024)) != -1) {
			fis.write(buffer, 0, count);
		}
		fis.close();
		bis.close();
	}
	public void loadDll(String dll) {
		System.loadLibrary(dll);
	}
	@SuppressWarnings("unused")
	public void downloadOtherDll(String url, String path, String name) {
		download(url, path);
		loadDll(name);
	}
	public void getAssetsObject(String c) {
		JsonObject gson = new Gson().fromJson(c, JsonObject.class).get("objects").getAsJsonObject();
		for (String key : gson.keySet()) {
			var object_url = UrlUtil.game_resource + gson.get(key).getAsJsonObject().get("hash").getAsString();
			var a = getProject().getGradle().getGradleUserHomeDir().getAbsolutePath() + separatorChar + "ation-gradle" + separatorChar + "assets" + separatorChar + "objects" + separatorChar + gson.get(key).getAsJsonObject().get("hash").getAsString().substring(0, 2);
			var b= getProject().getGradle().getGradleUserHomeDir().getAbsolutePath() + separatorChar + "ation-gradle" + separatorChar + "assets" + separatorChar + "virtual" + separatorChar + "legacy" + separatorChar + gson.get(key).getAsJsonObject().get("hash").getAsString().substring(0, 2);
			isMkdirs(a);isMkdirs(b);
			download(object_url, a + separatorChar + gson.get(key).getAsJsonObject().get("hash").getAsString());
			download(object_url, b + separatorChar + gson.get(key).getAsJsonObject().get("hash").getAsString());
		}
	}
	public void getAssetsIndexesAndObjects() {
		var a = new Gson().fromJson(getJson(version_mainfest), JsonObject.class).get("assetsIndex").getAsJsonObject().get("url").getAsString();
		var b = getProject().getGradle().getGradleUserHomeDir().getAbsolutePath() + separatorChar + "assets" + separatorChar + "indexes" + separatorChar + new Gson().fromJson(getJson(version_mainfest), JsonObject.class).get("assetsIndex").getAsJsonObject().get("id").getAsString() + ".json";
		download(a, b);
		getAssetsObject(b);
	}
	public void getClient() {
		var url = new Gson().fromJson(getJson(version_mainfest), JsonObject.class).get("downloads").getAsJsonObject().get("client").getAsJsonObject().get("url").getAsString();
		var client_version = getProject().getGradle().getGradleUserHomeDir().getAbsolutePath() + separatorChar + "ation-gradle" + separatorChar + "MC" + separatorChar + ationGradleExtensions.gameVersion;
		isMkdirs(client_version);
		download(url, client_version + separatorChar + ationGradleExtensions.gameVersion + ".jar");
	}
	public void getServer() {
		var url = new Gson().fromJson(getJson(version_mainfest), JsonObject.class).get("downloads").getAsJsonObject().get("server").getAsJsonObject().get("url").getAsString();
		var server_version = getProject().getGradle().getGradleUserHomeDir().getAbsolutePath() + separatorChar + "ation-gradle" + separatorChar + "Server" + separatorChar + ationGradleExtensions.gameVersion;
		isMkdirs(server_version);
		download(url, server_version + separatorChar + ationGradleExtensions.gameVersion + ".jar");
	}
	public void getMappings() {
		var download_clientMappings = new Gson().fromJson(getJson(version_mainfest), JsonObject.class).get("downloads").getAsJsonObject().get("client_mappings").getAsString();
		var download_serverMappings = new Gson().fromJson(getJson(version_mainfest), JsonObject.class).get("downloads").getAsJsonObject().get("server_mappings").getAsString();
		var mappings = getProject().getGradle().getGradleUserHomeDir().getAbsolutePath() + separatorChar + "ation-gradle" + separatorChar + "mappings";
		isMkdirs(mappings);
		download(download_clientMappings, mappings + separatorChar + ationGradleExtensions.gameVersion + "-client.txt");
		download(download_serverMappings, mappings + separatorChar + ationGradleExtensions.gameVersion + "-server.txt");
	}
	public void getNative() {
		MinecraftUtil.getNatives(ationGradleExtensions).forEach(link -> {

			String name = link.substring(link.lastIndexOf("/") + 1);
			File nativesJarFile = new File(MinecraftUtil.getClientNativeJarDir(ationGradleExtensions), name);
			if (!nativesJarFile.exists()) {
				try {
					FileUtils.writeByteArrayToFile(nativesJarFile, UrlUtil.readFile(link));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			try {
				ZipFile zipFile = new ZipFile(nativesJarFile);
				Enumeration<? extends ZipEntry> entries = zipFile.entries();
				while (entries.hasMoreElements()) {
					ZipEntry entry = entries.nextElement();
					if (entry.isDirectory() || !entry.getName().contains("META-INF")) continue;
					FileUtils.writeByteArrayToFile(new File(MinecraftUtil.getClientNativeFileDir(ationGradleExtensions), entry.getName()), IOUtils.toByteArray(zipFile.getInputStream(entry)));
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}


	@TaskAction
	public void downloadAll(){
		/*
		  @author baka4n
		 * @since  download DownloadJni.dll
		 */
		try {
			getBasicDownloadDll();
		} catch (IOException e) {
			e.printStackTrace();
		}
		loadDll("DownloadJni");
		download(getJson(ationGradleExtensions.gameVersion), version_mainfest);
		getAssetsIndexesAndObjects();
		getClient();
		getServer();
		getMappings();
		getNative();
	}
}
