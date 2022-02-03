package io.github.overrun.task;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.overrun.util.MinecraftUtil;
import io.github.overrun.util.UrlUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class DownloadAssetsTask extends Task{

	@TaskAction
	public void downloadAssets() {
		String clientAssets = MinecraftUtil.getClientAssets(ationGradleExtensions);
		getProject().getLogger().lifecycle("Download index ...");
		//index file
		if (!MinecraftUtil.getClientAssetsIndexFile(ationGradleExtensions).exists()) {
			try {
				FileUtils.write(MinecraftUtil.getClientAssetsIndexFile(ationGradleExtensions), clientAssets, StandardCharsets.UTF_8);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		getProject().getLogger().lifecycle("Download assets ...");
		Map<String, AssetObject> objectMap = new HashMap<>();
		for (Map.Entry<String, JsonElement> objects : new Gson().fromJson(clientAssets, JsonObject.class).get("objects").getAsJsonObject().entrySet()) {
			objectMap.put(objects.getKey(), new Gson().fromJson(objects.getValue(), AssetObject.class));
		}

		objectMap.forEach((name, assetObject) -> {
			File localClientAssetsObjectFile = MinecraftUtil.getLocalClientAssetsObjectFile(assetObject.getHash());
			File clientAssetsObjectFile = MinecraftUtil.getClientAssetsObjectFile(ationGradleExtensions, assetObject.getHash());
			try {
				if (localClientAssetsObjectFile.exists()) {
					FileUtils.copyFile(localClientAssetsObjectFile, clientAssetsObjectFile);
				} else {
					if (!clientAssetsObjectFile.exists()) {
						FileUtils.writeByteArrayToFile(clientAssetsObjectFile, UrlUtil.readFile(UrlUtil.game_resource + "/" + assetObject.getHash().substring(0, 2) + "/" + assetObject.getHash()));
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		});


		getProject().getLogger().lifecycle("Download natives ...");
		File clientNativeJarDir = MinecraftUtil.getClientNativeJarDir(ationGradleExtensions);
		File clientNativeFileDir = MinecraftUtil.getClientNativeFileDir(ationGradleExtensions);

		MinecraftUtil.getNatives(ationGradleExtensions).forEach(link -> {
			String name = link.substring(link.lastIndexOf("/") + 1);
			File file = new File(clientNativeJarDir, name);
			try {
				if (!file.exists()) {
					FileUtils.writeByteArrayToFile(file, UrlUtil.readFile(link));
				}

				ZipFile zipFile = new ZipFile(file);
				Enumeration<? extends ZipEntry> entries = zipFile.entries();
				while (entries.hasMoreElements()) {
					ZipEntry zipEntry = entries.nextElement();
					if (zipEntry.isDirectory() || zipEntry.getName().contains("META-INF")) {
						continue;
					}
					FileUtils.writeByteArrayToFile(new File(clientNativeFileDir, zipEntry.getName()), IOUtils.toByteArray(zipFile.getInputStream(zipEntry)));
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	private record AssetObject(String hash, long size) {

		public String getHash() {
			return hash;
		}

		public long getSize() {
			return size;
		}
	}

}
