package com.tntsallin1client.launch;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

/**
 * Our way into a legacy Minecraft (before 1.14, where Fabric doesn't exist): the launcher starts
 * the game through Mojang's LaunchWrapper with this class as {@code --tweakClass}. LaunchWrapper
 * loads the game through a class loader that lets registered transformers rewrite classes as they
 * load - all this tweaker does is switch Mixin on for it and hand over our mixin configuration.
 *
 * <p>Lives in a package of its own: LaunchWrapper keeps a tweaker's whole package out of that class
 * loader, so nothing that touches game classes may sit next to it.
 */
public class ClientTweaker implements ITweaker {
	/** Set by the build's own check ({@code VerifyMain}) instead of starting the game. */
	private static final String VERIFY_PROPERTY = "tntsallin1client.verify";

	private final List<String> arguments = new ArrayList<String>();

	@Override
	public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
		// LaunchWrapper takes these three out of the argument list for itself - the game needs
		// them back.
		this.arguments.addAll(args);
		addArgument("--version", profile);
		addArgument("--gameDir", gameDir == null ? null : gameDir.getAbsolutePath());
		addArgument("--assetsDir", assetsDir == null ? null : assetsDir.getAbsolutePath());
	}

	private void addArgument(String name, String value) {
		if (value != null && !this.arguments.contains(name)) {
			this.arguments.add(name);
			this.arguments.add(value);
		}
	}

	@Override
	public void injectIntoClassLoader(LaunchClassLoader classLoader) {
		MixinBootstrap.init();
		Mixins.addConfiguration("mixins.tntsallin1client.json");
		MixinEnvironment environment = MixinEnvironment.getDefaultEnvironment();
		// The game runs under its obfuscated names - no loader in between that renames anything.
		if (environment.getObfuscationContext() == null) {
			environment.setObfuscationContext("notch");
		}
		environment.setSide(MixinEnvironment.Side.CLIENT);
	}

	@Override
	public String getLaunchTarget() {
		return Boolean.getBoolean(VERIFY_PROPERTY) ? "com.tntsallin1client.verify.VerifyMain" : "net.minecraft.client.main.Main";
	}

	@Override
	public String[] getLaunchArguments() {
		return this.arguments.toArray(new String[0]);
	}
}
