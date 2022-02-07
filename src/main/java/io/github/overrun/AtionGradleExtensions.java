package io.github.overrun;

import org.gradle.api.Project;

import java.io.File;

public class AtionGradleExtensions {
	public String gameVersion = "1.18.1";
	public String mainClientClass = "net.minecraft.client.main.Main";
	public String tweakClass = null;
	public String mixinRefMap = null;

	final Project project;

	public AtionGradleExtensions(Project project) {
		this.project = project;
	}

	public File getUserCache() {
		File file = new File(project.getGradle().getGradleUserHomeDir(), "caches" + File.separator + "ation-gradle");
		if (!file.exists()) //noinspection ResultOfMethodCallIgnored
			file.mkdir();
		return file;
	}
}
