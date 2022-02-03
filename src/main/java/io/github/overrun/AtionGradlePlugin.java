package io.github.overrun;

import io.github.overrun.task.DownloadMinecraftClientTask;
import io.github.overrun.task.Task;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * @author baka4n
 */
public class AtionGradlePlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		project.getLogger().lifecycle("Hello World");
		project.getTasks().create("Download Minecraft Client", DownloadMinecraftClientTask.class, downloadMinecraftClientTask -> downloadMinecraftClientTask.setGroup("ation-gradle"));
		project.getExtensions().create("extensions", AtionGradleExtensions.class, project);
	}
}
