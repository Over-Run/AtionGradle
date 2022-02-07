package io.github.overrun.task;

import io.github.overrun.util.MappingUtil;
import io.github.overrun.util.MinecraftUtil;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;

public class CleanMinecraftClientTask extends Task {

	@TaskAction
	public void cleanMinecraftClient() {
		try {
			MappingUtil.init(ationGradleExtensions);
			File clientCleanFile = MinecraftUtil.getClientCleanFile(ationGradleExtensions);
			File clientFile = MinecraftUtil.getClientFile(ationGradleExtensions);
			if (clientFile.exists()) {
				MappingUtil.cleanJar(clientFile, clientCleanFile);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
