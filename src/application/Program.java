package application;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Program {

	public static void main(String[] args) throws Exception {

		if (args.length < 2 || args[0].isEmpty()) {
			System.out.println("You need to pass two parameters: --path and --features.");
			System.exit(1);
		}

		String path = "";
		ArrayList<String> features = new ArrayList<>();

		if (args[0].startsWith("--path")) {
			path = args[0].replace("--path=", "");
		}

		if (args[1].startsWith("--features")) {
			String[] list = args[1].replace("--features=", "").split(",");
			for (int i = 0; i < list.length; i++) {
				features.add(list[i]);
			}
		}

		ReaderResults rs = new ReaderResults();

		List<String> specialCases = rs.read(path + osValidation.osBarLine() + "results.csv", features);

		List<String> list = specialCases.stream().collect(Collectors.toList());

		try {
			Files.deleteIfExists(Paths.get(path + osValidation.osBarLine() + "specialCases.csv"));
			PrintStream fileStream = new PrintStream(
					new File(path + osValidation.osBarLine() + "specialCases.csv"));
			for (String project : list) {
				fileStream.println(project.toString());
			}
			fileStream.close();
			System.out.println("Successfully wrote to the file.");
		} catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}

		String directory = path + "/../dataset";
		String results_dir = directory + "/js-miner-out";

		Set<String> modernizeCommits = CommitsCompare.compare(list, directory, results_dir, path, features);

		try {
			PrintStream fileStream = new PrintStream(
					new File(path + osValidation.osBarLine() + "modernizations.csv"));
			for (String mc : modernizeCommits) {
				fileStream.println(mc);
			}
			fileStream.close();
		} catch (OutOfMemoryError error) {
			error.getStackTrace();
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.getStackTrace();
		}
	}
}
