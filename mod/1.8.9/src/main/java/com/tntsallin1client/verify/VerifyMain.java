package com.tntsallin1client.verify;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Stands in for the game's main class when the JVM is started with
 * {@code -Dtntsallin1client.verify=true} (the build's {@code verifyMixins} task): puts every
 * mixin's target class through the same transformation the game would and checks that the mixin
 * ended up in it - without opening a game window. Covers the whole chain at once: the tweaker, Mixin, and the names
 * the build translated back to the obfuscated game.
 *
 * <p>A mixin that can't be applied (target method gone, wrong descriptor, ...) fails here the same
 * way it would crash the game, since the mixin configuration is {@code required}.
 */
public final class VerifyMain {
	private static final String MIXIN_CONFIG = "mixins.tntsallin1client.json";
	private static final String MIXIN_ANNOTATION = "Lorg/spongepowered/asm/mixin/Mixin;";
	private static final String MERGED_ANNOTATION = "Lorg/spongepowered/asm/mixin/transformer/meta/MixinMerged;";

	private VerifyMain() {
	}

	public static void main(String[] args) throws IOException {
		ClassLoader loader = VerifyMain.class.getClassLoader();
		int failures = 0;
		int checked = 0;
		for (String mixinClass : mixinClasses(loader)) {
			for (String target : targetsOf(loader, mixinClass)) {
				checked++;
				String problem = check(mixinClass, target);
				if (problem == null) {
					System.out.println("[tntsallin1client] OK      " + simpleName(mixinClass) + " -> " + target);
				} else {
					failures++;
					System.out.println("[tntsallin1client] FAILED  " + simpleName(mixinClass) + " -> " + target + ": " + problem);
				}
			}
		}
		if (checked == 0) {
			System.out.println("[tntsallin1client] VERIFY FAILED: no mixins found in " + MIXIN_CONFIG);
			System.exit(1);
		}
		if (failures > 0) {
			System.out.println("[tntsallin1client] VERIFY FAILED: " + failures + " of " + checked + " mixin targets");
			System.exit(1);
		}
		System.out.println("[tntsallin1client] VERIFY OK: " + checked + " mixin targets");
	}

	private static String simpleName(String className) {
		return className.substring(className.lastIndexOf('.') + 1);
	}

	/** Every mixin class the configuration lists, as full class names. */
	private static List<String> mixinClasses(ClassLoader loader) throws IOException {
		InputStream stream = loader.getResourceAsStream(MIXIN_CONFIG);
		if (stream == null) {
			throw new IOException(MIXIN_CONFIG + " not found");
		}
		JsonObject config;
		try {
			config = new JsonParser().parse(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
		} finally {
			stream.close();
		}
		String mixinPackage = config.get("package").getAsString();
		List<String> classes = new ArrayList<String>();
		for (String section : new String[] {"mixins", "client"}) {
			JsonArray names = config.getAsJsonArray(section);
			if (names == null) {
				continue;
			}
			for (JsonElement name : names) {
				classes.add(mixinPackage + "." + name.getAsString());
			}
		}
		return classes;
	}

	/**
	 * The classes a mixin targets, read from its {@code @Mixin} annotation. Read from the class file
	 * rather than through reflection - a mixin class is never loaded as a real class.
	 */
	private static List<String> targetsOf(ClassLoader loader, String mixinClass) throws IOException {
		final List<String> targets = new ArrayList<String>();
		InputStream stream = loader.getResourceAsStream(mixinClass.replace('.', '/') + ".class");
		if (stream == null) {
			throw new IOException("mixin class " + mixinClass + " not found");
		}
		try {
			new ClassReader(stream).accept(new ClassVisitor(Opcodes.ASM5) {
				@Override
				public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
					if (!MIXIN_ANNOTATION.equals(descriptor)) {
						return null;
					}
					return new AnnotationVisitor(Opcodes.ASM5) {
						@Override
						public AnnotationVisitor visitArray(String name) {
							// `value` holds class literals, `targets` class names as text.
							return new AnnotationVisitor(Opcodes.ASM5) {
								@Override
								public void visit(String ignored, Object value) {
									if (value instanceof Type) {
										targets.add(((Type) value).getClassName());
									} else if (value instanceof String) {
										targets.add(((String) value).replace('/', '.'));
									}
								}
							};
						}
					};
				}
			}, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
		} finally {
			stream.close();
		}
		return targets;
	}

	/** {@code null} if {@code mixinClass} is merged into {@code target}, otherwise what went wrong. */
	private static String check(String mixinClass, String target) {
		byte[] transformed;
		try {
			// The same steps LaunchWrapper's class loader takes when the game first uses the class,
			// minus actually defining it: that would pull in every class its members mention
			// (LWJGL, Netty, ...), none of which this check has or needs.
			transformed = Launch.classLoader.getClassBytes(target);
			if (transformed == null) {
				return "class not found in the game";
			}
			for (IClassTransformer transformer : Launch.classLoader.getTransformers()) {
				transformed = transformer.transform(target, target, transformed);
			}
		} catch (Throwable error) {
			return error.toString();
		}
		return hasMergedMember(transformed, mixinClass) ? null : "class transformed, but nothing of the mixin is in it";
	}

	/** Mixin marks every method and field it merges into a class with the mixin it came from. */
	private static boolean hasMergedMember(byte[] classBytes, final String mixinClass) {
		final boolean[] found = {false};
		final AnnotationVisitor mergedMarker = new AnnotationVisitor(Opcodes.ASM5) {
			@Override
			public void visit(String name, Object value) {
				if ("mixin".equals(name) && mixinClass.equals(value)) {
					found[0] = true;
				}
			}
		};
		new ClassReader(classBytes).accept(new ClassVisitor(Opcodes.ASM5) {
			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
				return new MethodVisitor(Opcodes.ASM5) {
					@Override
					public AnnotationVisitor visitAnnotation(String annotation, boolean visible) {
						return MERGED_ANNOTATION.equals(annotation) ? mergedMarker : null;
					}
				};
			}

			@Override
			public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
				return new FieldVisitor(Opcodes.ASM5) {
					@Override
					public AnnotationVisitor visitAnnotation(String annotation, boolean visible) {
						return MERGED_ANNOTATION.equals(annotation) ? mergedMarker : null;
					}
				};
			}
		}, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
		return found[0];
	}
}
