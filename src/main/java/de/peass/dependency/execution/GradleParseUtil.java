package de.peass.dependency.execution;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.builder.AstBuilder;
import org.codehaus.plexus.util.IOUtil;

import de.peass.dependency.execution.gradle.AndroidVersionUtil;
import de.peass.dependency.execution.gradle.FindDependencyVisitor;

public class GradleParseUtil {

	public static FindDependencyVisitor setAndroidTools(final File buildfile) {
		FindDependencyVisitor visitor = null;
		try {
			System.out.println("Editing: " + buildfile);
			visitor = parseBuildfile(buildfile);
			final List<String> gradleFileContents = Files.readAllLines(Paths.get(buildfile.toURI()));

			if (visitor.getBuildTools() != -1) {
				updateBuildTools(visitor, gradleFileContents);
			}

			if (visitor.getBuildToolsVersion() != -1) {
				updateBuildToolsVersion(visitor, gradleFileContents);
			}

			Files.write(buildfile.toPath(), gradleFileContents, StandardCharsets.UTF_8);
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return visitor;
	}

	public static FindDependencyVisitor parseBuildfile(final File buildfile) throws IOException, FileNotFoundException {
		FindDependencyVisitor visitor;
		final AstBuilder builder = new AstBuilder();
		final List<ASTNode> nodes = builder.buildFromString(IOUtil.toString(new FileInputStream(buildfile), "UTF-8"));

		visitor = new FindDependencyVisitor();
		for (final ASTNode node : nodes) {
			node.visit(visitor);
		}
		return visitor;
	}

	public static void updateBuildTools(final FindDependencyVisitor visitor, final List<String> gradleFileContents) {
		final int lineIndex = visitor.getBuildTools() - 1;
		final String versionLine = gradleFileContents.get(lineIndex).trim().replaceAll("'", "").replace("\"", "");
		final String versionString = versionLine.split(":")[1].trim();
		final String runningVersion = AndroidVersionUtil.getRunningVersion(versionString);
		if (runningVersion != null) {
			gradleFileContents.set(lineIndex, "'buildTools': '" + runningVersion + "'");
		} else {
			visitor.setHasVersion(false);
		}
	}

	public static void updateBuildToolsVersion(final FindDependencyVisitor visitor,
			final List<String> gradleFileContents) {
		final int lineIndex = visitor.getBuildToolsVersion() - 1;
		final String versionLine = gradleFileContents.get(lineIndex).trim().replaceAll("'", "").replace("\"", "");
		final String versionString = versionLine.split(" ")[1].trim();
		System.out.println(lineIndex + " " + versionLine);
		final String runningVersion = AndroidVersionUtil.getRunningVersion(versionString);
		if (runningVersion != null) {
			gradleFileContents.set(lineIndex, "buildToolsVersion " + runningVersion);
		} else {
			visitor.setHasVersion(false);
		}
	}

	public static FindDependencyVisitor addDependencies(final File buildfile, final File tempFolder) {
		FindDependencyVisitor visitor = null;
		try {
			visitor = parseBuildfile(buildfile);
			final List<String> gradleFileContents = Files.readAllLines(Paths.get(buildfile.toURI()));
			if (visitor.isUseJava() == true) {
				if (visitor.getBuildTools() != -1) {
					updateBuildTools(visitor, gradleFileContents);
				}

				if (visitor.getBuildToolsVersion() != -1) {
					updateBuildToolsVersion(visitor, gradleFileContents);
				}

				if (visitor.getDependencyLine() != -1) {
					for (RequiredDependency dependency : RequiredDependency.getAll(false)) {
						final String dependencyGradle = "implementation '" + dependency.getGradleDependency() + "'";
						gradleFileContents.add(visitor.getDependencyLine() - 1, dependencyGradle);
					}
				} else {
					gradleFileContents.add("dependencies { ");
					for (RequiredDependency dependency : RequiredDependency.getAll(false)) {
						final String dependencyGradle = "implementation '" + dependency.getGradleDependency() + "'";
						gradleFileContents.add(dependencyGradle);
					}
					gradleFileContents.add("}");
				}

				addKiekerLine(tempFolder, visitor, gradleFileContents);
			}

			Files.write(buildfile.toPath(), gradleFileContents, StandardCharsets.UTF_8);
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return visitor;
	}

	public static void addKiekerLine(final File tempFolder, final FindDependencyVisitor visitor,
			final List<String> gradleFileContents) {
		if (tempFolder != null) {
			final String javaagentArgument = "-xAsd";
			if (visitor.getAndroidLine() != -1) {
				if (visitor.getUnitTestsAll() != -1) {
					gradleFileContents.add(visitor.getUnitTestsAll() - 1, javaagentArgument);
				} else if (visitor.getTestOptionsAndroid() != -1) {
					gradleFileContents.add(visitor.getTestOptionsAndroid() - 1,
							"unitTests.all{" + javaagentArgument + "}");
				} else {
					gradleFileContents.add(visitor.getAndroidLine() - 1,
							"testOptions{ unitTests.all{" + javaagentArgument + "} }");
				}
			} else {
				if (visitor.getTestLine() != -1) {
					gradleFileContents.add(visitor.getDependencyLine() - 1, javaagentArgument);
				} else {
					gradleFileContents.add("test { " + javaagentArgument + "}");
				}
			}
		}
	}

	public static List<File> getModules(final File projectFolder) throws FileNotFoundException, IOException {
		final File settingsFile = new File(projectFolder, "settings.gradle");
		final List<File> modules = new LinkedList<>();
		if (settingsFile.exists()) {
			try (BufferedReader reader = new BufferedReader(new FileReader(settingsFile))) {
				String line;
				while ((line = reader.readLine()) != null) {
					parseModuleLine(projectFolder, modules, line);
				}
			}
		} else {
			System.out.println("settings-file {} not found " + settingsFile);
			modules.add(projectFolder);
		}
		return modules;
	}

	private static void parseModuleLine(final File projectFolder, final List<File> modules, final String line) {
		final String[] splitted = line.split(" ");
		if (splitted.length == 2 && splitted[0].equals("include")) {
			final String candidate = splitted[1].substring(2, splitted[1].length() - 1);
			final File module = new File(projectFolder, candidate.replace(':', File.separatorChar));
			if (module.exists()) {
				modules.add(module);
			} else {
				System.out.println(line + " not found!");
			}
		}
	}
}
