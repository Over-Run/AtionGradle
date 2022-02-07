package io.github.overrun.task.mappings;

import groovyjarjarasm.asm.ClassReader;
import groovyjarjarasm.asm.Type;
import groovyjarjarasm.asm.tree.AnnotationNode;
import groovyjarjarasm.asm.tree.ClassNode;
import groovyjarjarasm.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MixinMapping {
	public final List<String> mixins = new ArrayList<>();
	public final List<String> targets = new ArrayList<>();
	public final HashMap<String, String> invokes = new HashMap<>();
	public final HashMap<String, String> accessors = new HashMap<>();
	public final HashMap<String, List<String>> methods = new HashMap<>();
	public String className = null;

	public void accept(InputStream inputStream) throws IOException {
		ClassReader classReader = new ClassReader(inputStream);
		ClassNode classNode = new ClassNode();
		classReader.accept(classNode, 0);

		if (classNode.invisibleAnnotations == null) {
			return;
		}

		for (AnnotationNode invisibleAnnotation : classNode.invisibleAnnotations) {
			if (invisibleAnnotation.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;")) {
				className = classNode.name;
				List<Type> values = getAnnotationValue(invisibleAnnotation, "value");
				if (values != null) {
					for (Type type : values) {
						mixins.add(type.getClassName().replace(".", "/"));
					}
				}
				List<String> targets = getAnnotationValue(invisibleAnnotation, "targets");
				if (targets != null) {
					for (String target : targets) {
						mixins.add(target.replace(".", "/"));
					}
				}
			}
		}

		if (className == null) {
			return;
		}

		for (MethodNode methodNode : classNode.methods) {

			if (methodNode.visibleAnnotations == null) {
				continue;
			}

			for (AnnotationNode visibleAnnotation : methodNode.visibleAnnotations) {
				if (visibleAnnotation.desc.equals("Lorg/spongepowered/asm/mixin/injection/Inject;")) {

					List<String> method = getAnnotationValue(visibleAnnotation, "method");
					if (method != null) {
						methods.put(methodNode.desc, method);
					}

					List<AnnotationNode> at = getAnnotationValue(visibleAnnotation, "at");
					if (at != null) {
						String target = getAnnotationValue(visibleAnnotation, "target");
						if (target != null) {
							targets.add(target);
						}
					}
				}

				if (visibleAnnotation.desc.equals("Lorg/spongepowered/asm/mixin/gen/Invoker;")) {
					String value = getAnnotationValue(visibleAnnotation, "value");
					if (value != null) {
						invokes.put(methodNode.desc, value);
					}
				}

				if (visibleAnnotation.desc.equals("Lorg/spongepowered/asm/mixin/gen/Accessor;")) {
					String value = getAnnotationValue(visibleAnnotation, "value");
					if (value != null) {
						accessors.put(methodNode.desc, value);
					}
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T getAnnotationValue(AnnotationNode annotationNode, String key) {
		boolean getNextValue = false;

		if (annotationNode.values == null) {
			return null;
		}

		for (Object value : annotationNode.values) {
			if (getNextValue) {
				return (T) value;
			}
			if (value.equals(key)) {
				getNextValue = true;
			}
		}

		return null;
	}
}
