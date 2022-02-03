package io.github.overrun.util;

import groovyjarjarasm.asm.ClassReader;
import groovyjarjarasm.asm.ClassVisitor;
import groovyjarjarasm.asm.ClassWriter;
import groovyjarjarasm.asm.commons.ClassRemapper;
import groovyjarjarasm.asm.commons.SimpleRemapper;
import io.github.overrun.AtionGradleExtensions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import static org.objectweb.asm.Opcodes.ASM9;

public class MappingUtil {

	public static final Map<String, String> classObfToClean = new HashMap<>();
	public static final Map<String, String> classCleanToObf = new HashMap<>();

	public static final Map<String, String> fieldObfToClean = new HashMap<>();
	public static final Map<String, String> fieldCleanToObf = new HashMap<>();

	public static final Map<String, String> methodObfToClean = new HashMap<>();
	public static final Map<String, String> methodCleanToObf = new HashMap<>();

	private static boolean init = false;

	public static final Map<String, ArrayList<String>> superHashMap = new HashMap<>();

	public static void init(AtionGradleExtensions ationGradleExtensions) throws IOException {
		if (init) {
			return;
		}

		String mapping = Files.readString(MinecraftUtil.getClientMappingFile(ationGradleExtensions).toPath());
		String[] split = mapping.split("\\r\\n|\\n");
		for (String line : split) {
			if (line.startsWith("#")) {
				continue;
			}

			if (line.contains("->") && line.endsWith(":")) {
				String[] rightSplit = line.trim().split("( |->)+");
				String obf = internalize(rightSplit[1].substring(0, rightSplit[1].length() - 1));
				String clean = internalize(rightSplit[0]);
				classObfToClean.put(obf, clean);
				classCleanToObf.put(clean, obf);
			}
		}

		String obf = null;
		String clean = null;

		for (String line : split) {
			if (line.startsWith("#")) {
				continue;
			}

			if (line.contains("->") && line.endsWith(":")) {
				String[] rightSplit = line.trim().split("( |->)+");
				obf = internalize(rightSplit[1].substring(0, rightSplit[1].length() - 1));
				clean = internalize(rightSplit[0]);
			}

			if (obf == null) {
				continue;
			}

			if (line.contains("->") && !line.endsWith(":")) {
				if (!line.contains("(")) {
					String[] fieldRight = line.trim().split("( |->)+");
					String fieldObf = obf + "/" + fieldRight[2];
					String fieldClean = clean + "/" + fieldRight[1];
					fieldObfToClean.put(fieldObf, fieldClean);
					fieldCleanToObf.put(fieldClean, fieldObf);
				} else {
					String[] methodRight = line.contains(":")
							? line.substring(line.lastIndexOf(":") + 1).trim().split("( |->)+") :
							line.trim().split("( |->)+");

					String methodCleanReturn = !primitive(methodRight[0]) ? "L" + internalize(methodRight[0]) + ";" : internalize(methodRight[0]);
					String methodCleanName = methodRight[1].substring(0, methodRight[1].indexOf("("));
					String methodCleanArgs = methodRight[1].substring(methodRight[1].indexOf("(") + 1, methodRight[1].lastIndexOf(")"));

					String methodObfReturn = !primitive(methodRight[0]) ? "L" + classCleanToObf.getOrDefault(internalize(methodRight[0]), internalize(methodRight[0])) + ";" : internalize(methodRight[0]);
					String methodObfName = methodRight[2];
					String methodObfArgs;

					if (methodCleanArgs.equals("")) {
						methodCleanArgs = "()";
						methodObfArgs = "()";
					} else {
						StringBuilder tempCleanArgs = new StringBuilder();
						StringBuilder tempObfArgs = new StringBuilder();
						for (String s : methodCleanArgs.split(",")) {
							if (primitive(s)) {
								tempCleanArgs.append(internalize(s));
								tempObfArgs.append(internalize(s));
							} else {
								tempCleanArgs.append("L").append(internalize(s)).append(";");
								tempObfArgs.append("L").append(classCleanToObf.getOrDefault(internalize(s), internalize(s))).append(";");
							}
						}
						methodCleanArgs = "(" + tempCleanArgs + ")";
						methodObfArgs = "(" + tempObfArgs + ")";
					}

					String methodObf = obf + "/" + methodObfName + " " + methodObfArgs + methodObfReturn;
					String methodClean = clean + "/" + methodCleanName + " " + methodCleanArgs + methodCleanReturn;

					methodCleanToObf.put(methodClean, methodObf);
					methodObfToClean.put(methodObf, methodClean);
				}
			}
		}
		init = true;
	}

	public static void cleanJar(File input, File output) throws IOException {
		analyze(new JarFile(input));
		Map<String, String> map = getMap(true);
		JarFile jarFile = new JarFile(input);
		Enumeration<JarEntry> jarEntryEnumeration = jarFile.entries();
		JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(output));
		while (jarEntryEnumeration.hasMoreElements()) {
			JarEntry jarEntry = jarEntryEnumeration.nextElement();

			if (jarEntry.isDirectory() || jarEntry.getName().contains("META-INF")) continue;

			if (jarEntry.getName().endsWith(".class")) {
				byte[] accept = classMapping(jarFile.getInputStream(jarEntry), map);
				String substring = jarEntry.getName().substring(0, jarEntry.getName().lastIndexOf(".class"));
				JarEntry classEntry = new JarEntry(map.getOrDefault(substring, substring) + ".class");
				jarOutputStream.putNextEntry(classEntry);
				jarOutputStream.write(accept);
			} else {
				JarEntry file = new JarEntry(jarEntry.getName());
				jarOutputStream.putNextEntry(file);
				jarOutputStream.write(jarFile.getInputStream(jarEntry).readAllBytes());
			}
		}
		jarOutputStream.closeEntry();
		jarOutputStream.close();
	}

	public static Map<String, String> getMap(boolean clean) {
		final Map<String, String> map = new HashMap<>();
		if (clean) {
			map.putAll(classObfToClean);
		} else {
			map.putAll(classCleanToObf);
		}

		//field map
		for (Map.Entry<String, String> stringStringEntry : fieldObfToClean.entrySet()) {
			String key = clean ? stringStringEntry.getKey() : stringStringEntry.getValue();
			String value = clean ? stringStringEntry.getValue() : stringStringEntry.getKey();
			String className = key.substring(0, key.lastIndexOf("/"));
			String fieldName = key.substring(key.lastIndexOf("/") + 1);
			map.put(className + "." + fieldName, value.substring(value.lastIndexOf("/") + 1));
		}

		//method map
		for (Map.Entry<String, String> stringStringEntry : methodObfToClean.entrySet()) {
			String[] methodObfSplit = (clean ? stringStringEntry.getKey() : stringStringEntry.getValue()).split(" ");
			String[] methodCleanSplit = (clean ? stringStringEntry.getValue() : stringStringEntry.getKey()).split(" ");
			String methodObfClass = methodObfSplit[0].substring(0, methodObfSplit[0].lastIndexOf("/"));
			String methodObfName = methodObfSplit[0].substring(methodObfSplit[0].lastIndexOf("/") + 1);
			String methodCleanName = methodCleanSplit[0].substring(methodCleanSplit[0].lastIndexOf("/") + 1);
			map.put(methodObfClass + "." + methodObfName + methodObfSplit[1], methodCleanName);
		}
		return map;
	}

	public static byte[] classMapping(InputStream inputStream, Map<String, String> map) throws IOException {
		ClassReader classReader = new ClassReader(inputStream);
		ClassWriter classWriter = new ClassWriter(0);
		ClassRemapper classRemapper = new ClassRemapper(new ClassVisitor(ASM9, classWriter) {
		}, new SimpleRemapper(map) {
			@Override
			public String mapFieldName(String owner, String name, String descriptor) {
				String remappedName = map(owner + '.' + name);
				if (remappedName == null) {
					if (superHashMap.get(owner) != null) {
						for (String s : superHashMap.get(owner)) {
							String rn = mapFieldName(s, name, descriptor);
							if (rn != null) {
								return rn;
							}
						}
					}
				}
				return remappedName == null ? name : remappedName;
			}

			@Override
			public String mapMethodName(String owner, String name, String descriptor) {
				String remappedName = map(owner + '.' + name + descriptor);
				if (remappedName == null) {
					if (superHashMap.get(owner) != null) {
						for (String s : superHashMap.get(owner)) {
							String rn = mapMethodName(s, name, descriptor);
							if (rn != null) {
								return rn;
							}
						}
					}
				}
				return remappedName == null ? name : remappedName;
			}
		});
		classReader.accept(classRemapper, 0);
		return classWriter.toByteArray();
	}

	public static void analyze(JarFile jarFile) throws IOException {
		Enumeration<JarEntry> entries = jarFile.entries();
		while (entries.hasMoreElements()) {
			JarEntry jarEntry = entries.nextElement();
			if (jarEntry.getName().endsWith(".class")) {
				classAnalyze(jarFile.getInputStream(jarEntry));
			}
		}
	}

	public static void classAnalyze(InputStream inputStream) throws IOException {
		ClassReader classReader = new ClassReader(inputStream);
		classReader.accept(new ClassVisitor(ASM9) {
			@Override
			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				ArrayList<String> strings = new ArrayList<>();
				if (superHashMap.containsKey(name)) {
					if (superName != null) {
						if (!superHashMap.get(name).contains(superName)) {
							strings.add(superName);
						}
					}

					if (interfaces != null) {
						for (String anInterface : interfaces) {
							if (!superHashMap.get(name).contains(anInterface)) {
								strings.add(anInterface);
							}
						}
					}
					superHashMap.get(name).addAll(strings);
				} else {
					if (superName != null) {
						strings.add(superName);
					}

					if (interfaces != null) {
						Collections.addAll(strings, interfaces);
					}
					superHashMap.put(name, strings);
				}
				super.visit(version, access, name, signature, superName, interfaces);
			}
		}, 0);
	}

	private static String internalize(String name) {
		return switch (name) {
			case "int" -> "I";
			case "float" -> "F";
			case "double" -> "D";
			case "long" -> "J";
			case "boolean" -> "Z";
			case "short" -> "S";
			case "byte" -> "B";
			case "void" -> "V";
			default -> name.replace('.', '/');
		};
	}

	private static boolean primitive(String name) {
		return switch (name) {
			case "int", "float", "double", "long", "boolean", "short", "byte", "void" -> true;
			default -> false;
		};
	}

}
