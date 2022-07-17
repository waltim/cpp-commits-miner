package application;

public class osValidation {
	
	public static String osBarLine() {
		String barline = "";
		String OS = System.getProperty("os.name").toLowerCase();

		if (OS.contains("win")) {
			barline = "//";
		} else if (OS.contains("nix") || OS.contains("nux") || OS.contains("aix")) {
			barline = "/";
		} else {
			barline = "/";
		}
		return barline;
	}

}
