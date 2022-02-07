package io.github.overrun.task;

import io.github.overrun.util.MinecraftUtil;
import org.apache.commons.io.FileUtils;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;

public class DownloadMinecraftClientTask extends Task {
	@TaskAction
	public void downloadMinecraftClient() {
		File localClient = MinecraftUtil.getLocalClient(ationGradleExtensions);
		try {
			if (localClient.exists()) {
				FileUtils.copyFile(localClient, MinecraftUtil.getClientFile(ationGradleExtensions));
			} else {
				if (!MinecraftUtil.getClientFile(ationGradleExtensions).exists()) {
					FileUtils.writeByteArrayToFile(MinecraftUtil.getClientFile(ationGradleExtensions), MinecraftUtil.getClientJar(ationGradleExtensions));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
