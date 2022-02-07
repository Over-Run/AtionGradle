package io.github.overrun.task;

import io.github.overrun.util.MinecraftUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class GenIdeaRunTask extends Task{

	@TaskAction
	public void genIdeaRun() {
		StringBuilder vmArgs = new StringBuilder("-Djava.library.path=" + MinecraftUtil.getClientNativeFileDir(ationGradleExtensions).getAbsolutePath());
		StringBuilder programArgs = new StringBuilder();

		if (ationGradleExtensions.tweakClass != null) {
			programArgs.append("--tweakClass").append(" ").append(ationGradleExtensions.tweakClass).append(" ");
		}
		programArgs.append("--assetsDir").append(" ").append(MinecraftUtil.getClientAssetsDir(ationGradleExtensions).getAbsolutePath()).append(" ");
		programArgs.append("--assetIndex").append(" ").append(ationGradleExtensions.gameVersion).append(" ");
		programArgs.append("--version").append(" ").append("ation-gradle").append(" ");
		programArgs.append("--accessToken").append(" ").append("0").append(" ");
		programArgs.append("--gameDir").append(" ").append(getProject().getRootProject().file("run").getAbsolutePath()).append(" ");

		try {
			String idea = IOUtils.toString(Objects.requireNonNull(GenIdeaRunTask.class.getResourceAsStream("/Modloader_Develop_Client.xml")), StandardCharsets.UTF_8);

			idea = idea.replace("%NAME%", "ation gradle Client");
			idea = idea.replace("%MAIN_CLASS%", ationGradleExtensions.mainClientClass);
			idea = idea.replace("%IDEA_MODULE%", getModule());
			idea = idea.replace("%PROGRAM_ARGS%", programArgs.toString().replaceAll("\"", "&quot;"));
			idea = idea.replace("%VM_ARGS%", vmArgs.toString().replaceAll("\"", "&quot;"));
			String projectPath = getProject() == getProject().getRootProject() ? "" : getProject().getPath().replace(":", "_");
			File ideaConfigurationDir = getProject().getRootProject().file(".idea");
			File runConfigurations = new File(ideaConfigurationDir, "runConfigurations");
			File clientRunConfiguration = new File(runConfigurations, "ation_gradle_Client" + projectPath + ".xml");
			if (!runConfigurations.exists()) {
				//noinspection ResultOfMethodCallIgnored
				runConfigurations.mkdir();
			}
			FileUtils.write(clientRunConfiguration, idea, StandardCharsets.UTF_8);
			File run = getProject().getRootProject().file("run");
			if (!run.exists()) {
				//noinspection ResultOfMethodCallIgnored
				run.mkdir();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String getModule() {
		Project project = getProject();
		StringBuilder stringBuilder = new StringBuilder(project.getName() + ".main");
		while ((project = project.getParent()) != null) {
			stringBuilder.insert(0, project.getName() + ".");
		}
		return stringBuilder.toString();
	}

}
