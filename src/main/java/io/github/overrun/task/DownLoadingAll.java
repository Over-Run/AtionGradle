package io.github.overrun.task;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.overrun.jni.Download;
import io.github.overrun.util.UrlUtil;
import org.gradle.api.tasks.TaskAction;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import static io.github.overrun.util.Universal.isMkdirs;
import static io.github.overrun.util.UrlUtil.readString;

public class DownLoadingAll extends Task{
	public static DownLoadingAll downLoadingAll = new DownLoadingAll();
	public static final String get_user_dir = System.getProperty("user.dir");
	public final String
			version_mainfest = getProject().getGradle().getGradleUserHomeDir().getAbsolutePath() + File.separatorChar + "version_mainfest.json",
			indexes = getProject().getGradle().getGradleUserHomeDir().getAbsolutePath() + File.separatorChar + "assets" + File.separatorChar + "indexes" + File.separatorChar + getAssetsIndex("id") + ".json",
			client_version = getProject().getGradle().getGradleUserHomeDir().getAbsolutePath() + File.separatorChar + "ation-gradle" + File.separatorChar + "MC" + File.separatorChar + ationGradleExtensions.gameVersion,
			server_version = getProject().getGradle().getGradleUserHomeDir().getAbsolutePath() + File.separatorChar + "ation-gradle" + File.separatorChar + "Server" + File.separatorChar + ationGradleExtensions.gameVersion,
			mappings = getProject().getGradle().getGradleUserHomeDir().getAbsolutePath() + File.separatorChar + "ation-gradle" + File.separatorChar + "mappings",
			object_file = getProject().getGradle().getGradleUserHomeDir().getAbsolutePath() + File.separatorChar + "ation-gradle" + File.separatorChar + "assets" + File.separatorChar + "objects",
			object_back = getProject().getGradle().getGradleUserHomeDir().getAbsolutePath() + File.separatorChar + "ation-gradle" + File.separatorChar + "assets" + File.separatorChar + "virtual" + File.separatorChar + "legacy";
	public static String jsonUrl;
	public static String getJson(String version) {
		for (JsonElement versions : new Gson().fromJson(readString(UrlUtil.game_url), JsonObject.class).get("versions").getAsJsonArray()) {
			if (versions.getAsJsonObject().get("id").getAsString().equals(version)) {
				jsonUrl = versions.getAsJsonObject().get("url").getAsString();
			}
		}
		return readString(jsonUrl);
	}

	public static String getAssetsIndex(String name) {
		return new Gson().fromJson(getJson(downLoadingAll.version_mainfest), JsonObject.class).get("assetsIndex").getAsJsonObject().get(name).getAsString();
	}

	public static void getAssetsObject() {
		JsonObject gson = new Gson().fromJson(downLoadingAll.indexes, JsonObject.class).get("objects").getAsJsonObject();
		for (String key : gson.keySet()) {
			String object_url = UrlUtil.game_resource + gson.get(key).getAsJsonObject().get("hash").getAsString(), a = new DownLoadingAll().object_file + File.separatorChar + gson.get(key).getAsJsonObject().get("hash").getAsString().substring(0, 2), b= new DownLoadingAll().object_back + File.separatorChar + gson.get(key).getAsJsonObject().get("hash").getAsString().substring(0, 2);
			isMkdirs(a);isMkdirs(b);
			Download.download(object_url, a + File.separatorChar + gson.get(key).getAsJsonObject().get("hash").getAsString());
			Download.download(object_url, b + File.separatorChar + gson.get(key).getAsJsonObject().get("hash").getAsString());
		}
	}

	public static String downloadClient() {
		return new Gson().fromJson(getJson(downLoadingAll.version_mainfest), JsonObject.class).get("downloads").getAsJsonObject().get("client").getAsJsonObject().get("url").getAsString();
	}

	public static String downloadServer() {
		return new Gson().fromJson(getJson(downLoadingAll.version_mainfest), JsonObject.class).get("downloads").getAsJsonObject().get("server").getAsJsonObject().get("url").getAsString();
	}

	public static String downloadClientMappings() {
		return new Gson().fromJson(getJson(downLoadingAll.version_mainfest), JsonObject.class).get("downloads").getAsJsonObject().get("client_mappings").getAsString();
	}

	public static String downloadServerMappings() {
		return new Gson().fromJson(getJson(downLoadingAll.version_mainfest), JsonObject.class).get("downloads").getAsJsonObject().get("server_mappings").getAsString();
	}


	@TaskAction
	public void downloadAll(){
		/**
		 * @author baka4n
		 * @since  download DownloadJni.dll
		 */
		try {

			URL url =  new URL("https://github.com/Over-Run/maven/raw/main/DownloadJni.dll");
			BufferedInputStream bis = new BufferedInputStream(url.openStream());
			FileOutputStream fis = new FileOutputStream(get_user_dir + File.separatorChar + "DownloadJni.dll");
			byte[] buffer = new byte[1024];
			@SuppressWarnings("UnusedAssignment") int count = 0;
			while ((count = bis.read(buffer, 0, 1024)) != -1) {
				fis.write(buffer, 0, count);
			}
			fis.close();
			bis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.loadLibrary("DownloadJni");
		Download.download(getJson(ationGradleExtensions.gameVersion), version_mainfest);
		Download.download(getAssetsIndex("url"), indexes);
		getAssetsObject();
		isMkdirs(client_version);isMkdirs(server_version);
		Download.download(downloadClient(), client_version + File.separatorChar + ationGradleExtensions.gameVersion + ".jar");
		Download.download(downloadServer(), server_version + File.separatorChar + ationGradleExtensions.gameVersion + ".jar");
		Download.download(downloadClientMappings(), mappings + File.separatorChar + ationGradleExtensions.gameVersion + "-client.jar");
		Download.download(downloadServerMappings(), mappings + File.separatorChar + ationGradleExtensions.gameVersion + "-server.jar");
	}
}
