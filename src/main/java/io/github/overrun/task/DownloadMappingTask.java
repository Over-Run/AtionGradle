package io.github.overrun.task;

import io.github.overrun.util.MinecraftUtil;
import org.apache.commons.io.FileUtils;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class DownloadMappingTask extends Task {

	@TaskAction
	public void downloadMapping() {
		File clientMappingFile = MinecraftUtil.getClientMappingFile(ationGradleExtensions);
		if (!clientMappingFile.exists()) {
			try {
				if (!clientMappingFile.exists()) {
					FileUtils.write(clientMappingFile, MinecraftUtil.getClientMapping(ationGradleExtensions), StandardCharsets.UTF_8);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
