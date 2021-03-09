package de.peass.dependency.execution;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
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

			}

			Files.write(buildfile.toPath(), gradleFileContents, StandardCharsets.UTF_8);
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return visitor;
	}

}
