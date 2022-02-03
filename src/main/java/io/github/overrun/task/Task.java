package io.github.overrun.task;

import io.github.overrun.AtionGradleExtensions;
import org.gradle.api.DefaultTask;

public class Task extends DefaultTask {
	public AtionGradleExtensions ationGradleExtensions;

	public Task() {
		ationGradleExtensions = getProject().getExtensions().getByType(AtionGradleExtensions.class);
	}
}
