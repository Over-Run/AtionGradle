package io.github.overrun.task;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import groovyjarjarasm.asm.ClassReader;
import groovyjarjarasm.asm.ClassVisitor;
import groovyjarjarasm.asm.ClassWriter;
import groovyjarjarasm.asm.MethodVisitor;
import io.github.overrun.task.mappings.MixinMapping;
import io.github.overrun.util.MappingUtil;
import io.github.overrun.util.MinecraftUtil;
import org.apache.commons.io.FileUtils;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Map;
import java.util.jar.JarFile;

import static org.objectweb.asm.Opcodes.ASM9;

public class RemappingTask extends Task{
	@TaskAction
	public void remappingClass() {
		try {
			MappingUtil.init(ationGradleExtensions);
			File classes = new File(getProject().getBuildDir(), "classes");
			MappingUtil.analyze(new JarFile(MinecraftUtil.getClientCleanFile(ationGradleExtensions)));

			Files.walkFileTree(classes.toPath(), new SimpleFileVisitor<>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (file.toFile().getName().endsWith(".class")) {
						InputStream inputStream = file.toUri().toURL().openStream();
						MappingUtil.classAnalyze(inputStream);
						inputStream.close();
					}
					return super.visitFile(file, attrs);
				}
			});

			JsonObject mixinReMap = new JsonObject();
			JsonObject mixinMappings = new JsonObject();
			if (ationGradleExtensions.mixinRefMap != null) {
				Files.walkFileTree(classes.toPath(), new SimpleFileVisitor<>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						if (file.toFile().getName().endsWith(".class")) {
							InputStream inputStream = file.toUri().toURL().openStream();
							MixinMapping mixinMapping = new MixinMapping();
							mixinMapping.accept(inputStream);
							MappingUtil.superHashMap.put(mixinMapping.className, new ArrayList<>(mixinMapping.mixins));
							for (String mixin : mixinMapping.mixins) {

								JsonObject mapping = new JsonObject();

								mixinMapping.methods.forEach((descriptor, methods) -> {
									for (String method : methods) {
										if (method.contains("(")) {
											mapping.addProperty(method, getMethodObf(mixin, method, false));
										} else {
											mapping.addProperty(method, getMethodObf(mixin, method + descriptor.replace("Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;", ""), false));
										}
									}
								});

								for (String mixinTarget : mixinMapping.targets) {
									if (!mixinTarget.contains("field:")) {
										String targetClass = mixinTarget.substring(1, mixinTarget.indexOf(";"));

										String targetMethod = getMethodObf(targetClass, mixinTarget.substring(mixinTarget.indexOf(";") + 1), false);
										if (targetMethod == null) {
											continue;
										}
										mapping.addProperty(mixinTarget, targetMethod);
									} else {
										String left = mixinTarget.split("field:")[0];
										String right = mixinTarget.split("field:")[1];
										String targetClass = MappingUtil.classCleanToObf.get(left.substring(1, left.indexOf(";")));
										String targetField = MappingUtil.classCleanToObf.get(right.substring(1, right.indexOf(";")));

										if (targetClass == null || targetField == null) {
											continue;
										}

										mapping.addProperty(mixinTarget, "L" + targetClass + ";field:L" + targetField + ";");
									}
								}

								for (Map.Entry<String, String> entry : mixinMapping.accessors.entrySet()) {

									String fieldName = MappingUtil.fieldCleanToObf.get(mixin + "/" + entry.getValue());

									if (fieldName == null) {
										continue;
									}

									if (entry.getKey().contains(";")) {
										String arg;
										if (!entry.getKey().contains(")V")) {
											arg = entry.getKey().substring(entry.getKey().lastIndexOf(")") + 1);
										} else {
											arg = entry.getKey().substring(entry.getKey().indexOf("(") + 1, entry.getKey().lastIndexOf(")"));
										}

										arg = arg.substring(1, arg.lastIndexOf(";"));
										arg = MappingUtil.classCleanToObf.get(arg);
										if (arg == null) {
											continue;
										}
										mapping.addProperty(entry.getValue(), fieldName.split("/")[1] + ":L" + arg + ";");
									} else {
										mapping.addProperty(entry.getValue(), entry.getKey());
									}
								}

								for (Map.Entry<String, String> entry : mixinMapping.invokes.entrySet()) {
									mapping.addProperty(entry.getValue(), getMethodObf(mixin, entry.getValue() + entry.getKey(), false));
								}
								mixinMappings.add(mixinMapping.className, mapping);
							}
							inputStream.close();
						}
						return super.visitFile(file, attrs);
					}
				});
				mixinReMap.add("mappings", mixinMappings);
			}

			@SuppressWarnings("deprecation") JavaPluginConvention java = (JavaPluginConvention) getProject().getConvention().getPlugins().get("java");
			File resourceDir = new File(getProject().getBuildDir(), "resources");
			for (SourceSet sourceSet : java.getSourceSets()) {
				if (!resourceDir.exists()) {
					//noinspection ResultOfMethodCallIgnored
					resourceDir.mkdir();
				}
				File dir = new File(resourceDir, sourceSet.getName());
				if (!dir.exists()) {
					//noinspection ResultOfMethodCallIgnored
					dir.mkdir();
				}

				if (ationGradleExtensions.mixinRefMap != null) {
					FileUtils.write(new File(dir, ationGradleExtensions.mixinRefMap), new GsonBuilder().setPrettyPrinting().create().toJson(mixinReMap), StandardCharsets.UTF_8);
				}
			}

			Files.walkFileTree(classes.toPath(), new SimpleFileVisitor<>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (file.toFile().getName().endsWith(".class")) {
						InputStream inputStream = file.toUri().toURL().openStream();
						byte[] bytes = MappingUtil.classMapping(inputStream, MappingUtil.getMap(false));
						bytes = getRemapping(bytes);
						Files.write(file, bytes);
						inputStream.close();
					}
					return super.visitFile(file, attrs);
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}


	}

	private byte[] getRemapping(byte[] bytes) {
		ClassReader classReader = new ClassReader(bytes);
		ClassWriter classWriter = new ClassWriter(0);
		classReader.accept(new ClassVisitor(ASM9, classWriter) {
			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
				return new MethodVisitor(api, super.visitMethod(access, name, descriptor, signature, exceptions)) {
					@Override
					public void visitLdcInsn(Object value) {
						String s = value.toString();
						if (s.startsWith("remapping:")) {
							s = s.substring("remapping".length() + 1);
							value = MappingUtil.getMap(false).getOrDefault(s, value.toString());
						}
						super.visitLdcInsn(value);
					}
				};
			}
		}, 0);
		return classWriter.toByteArray();
	}

	private String getMethodObf(String klass, String method, boolean only) {
		String methodName = method.substring(0, method.indexOf("("));
		String methodDescriptor = method.substring(method.indexOf("("));
		String methodObf = MappingUtil.methodCleanToObf.get(klass + "/" + methodName + " " + methodDescriptor);
		if (methodObf == null) {
			return null;
		}
		if (!only) {
			methodObf = "L" + methodObf.split(" ")[0].replace("/", ";") + methodObf.split(" ")[1];
		} else {
			methodObf = methodObf.split(" ")[0];
			methodObf = methodObf.substring(methodObf.lastIndexOf("/") + 1);
		}
		return methodObf;
	}
}
