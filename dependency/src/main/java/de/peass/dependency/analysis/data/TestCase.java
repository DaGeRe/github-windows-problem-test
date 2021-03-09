package de.peass.dependency.analysis.data;

import java.io.File;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Represents a testcase with its class and its method. If no method is given,
 * the whole class with all methods is represented.
 * 
 * @author reichelt
 *
 */
public class TestCase implements Comparable<TestCase> {
	private final String module;
	private final String clazz;
	private final String method;


	public TestCase(final String clazz, final String method) {
		if (clazz.contains(File.separator)) { // possibly assertion, if speed
												// becomes issue..
			throw new RuntimeException("Testcase should be full qualified name, not path: " + clazz);
		}
		module = clazz.substring(0, clazz.indexOf("§"));
		this.clazz = clazz.substring(clazz.indexOf("#") + 1, clazz.length());
		this.method = method;
	}

	public String getClazz() {
		return clazz;
	}

	public String getMethod() {
		return method;
	}

	public String getModule() {
		return module;
	}

	@JsonIgnore
	public String getTestclazzWithModuleName() {
		String testcase;
		if (module != null && !module.equals("")) {
			testcase = module + "§" + clazz;
		} else {
			testcase = clazz;
		}
		return testcase;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
		result = prime * result + ((method == null) ? 0 : method.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final TestCase other = (TestCase) obj;
		if (clazz == null) {
			if (other.clazz != null) {
				return false;
			}
		} else if (!clazz.equals(other.clazz)) {
			final String shortClazz = clazz.substring(clazz.lastIndexOf('.') + 1);
			final String shortClazzOther = other.getClazz().substring(other.getClazz().lastIndexOf('.') + 1);
			if (!shortClazz.equals(shortClazzOther)) { // Dirty Hack - better
														// transfer clazz-info
														// always
				return false;
			}
		}
		if (method == null) {
			if (other.method != null) {
				return false;
			}
		} else if (!method.equals(other.method)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		if (module != null && !"".equals(module)) {
			return "TestCase [clazz=" + clazz + ", method=" + method + ", module=" + module + "]";
		} else {
			return "TestCase [clazz=" + clazz + ", method=" + method + "]";
		}
	}

	@JsonIgnore
	public String getExecutable() {
		if (method != null) {
			return clazz + "#" + method;
		} else {
			return clazz;
		}

	}

	@JsonIgnore
	public String getShortClazz() {
		return clazz.substring(clazz.lastIndexOf('.') + 1, clazz.length());
	}

	@Override
	public int compareTo(final TestCase arg0) {
		return toString().compareTo(arg0.toString());
	}

}