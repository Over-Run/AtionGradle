package io.github.overrun;

import io.github.overrun.task.*;
import io.github.overrun.util.MinecraftUtil;
import io.github.overrun.util.UrlUtil;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * @author baka4n
 */
public class AtionGradlePlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {

		project.getExtensions().create("ation gradle extensions", AtionGradleExtensions.class, project);

		project.afterEvaluate(after -> {

			after.getRepositories().maven(mavenArtifactRepository -> {
				mavenArtifactRepository.setName("minecraft");
				mavenArtifactRepository.setUrl(UrlUtil.game_libraries);
			});

			after.getRepositories().maven(mavenArtifactRepository -> {
				mavenArtifactRepository.setName("overrun");
				mavenArtifactRepository.setUrl("https://over-run.github.io/maven/");
			});

			after.getRepositories().maven(mavenArtifactRepository -> {
				mavenArtifactRepository.setName("SpongePowered");
				mavenArtifactRepository.setUrl("https://repo.spongepowered.org/repository/maven-public/");
			});

			after.getRepositories().mavenCentral();
			after.getRepositories().mavenLocal();

			AtionGradleExtensions ationGradleExtensions = after.getExtensions().getByType(AtionGradleExtensions.class);
			MinecraftUtil.getLibraries((AtionGradleExtensions) after).forEach(library -> after.getDependencies().add("implementation", library));

			after.getPlugins().apply("java");
			after.getPlugins().apply("idea");
			after.getTasks().create("downloadAll", DownLoadingAll.class, downLoadingAll -> downLoadingAll.setGroup("ation-gradle"));
			after.getTasks().create("CleanClient", CleanMinecraftClientTask.class, cleanClientTask -> cleanClientTask.setGroup("ation-gradle"));
			after.getTasks().create("RemappingClass", RemappingTask.class, remappingTask -> remappingTask.setGroup("ation-gradle"));
			after.getTasks().create("GenIdeaRun", GenIdeaRunTask.class, genIdeaRunTask -> genIdeaRunTask.setGroup("ation-gradle"));

			after.getTasks().getByName("idea").finalizedBy(
					after.getTasks().getByName("downloadAll"),
					after.getTasks().getByName("CleanClient"),
					after.getTasks().getByName("GenIdeaRun"));

			after.getTasks().getByName("compileJava").finalizedBy(after.getTasks().getByName("RemappingClass"));

			after.getDependencies().add("compileOnly", after.getDependencies().create(after.files(MinecraftUtil.getClientCleanFile(ationGradleExtensions).getAbsolutePath())));
			after.getDependencies().add("runtimeOnly", after.getDependencies().create(after.files(MinecraftUtil.getClientFile(ationGradleExtensions).getAbsolutePath())));
		});
	}
}
