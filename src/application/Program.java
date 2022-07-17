package application;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class Program {

	public static void main(String[] args) throws Exception {

		if (args.length == 0 || args[0].isEmpty()) {
			System.exit(1);
		}

		String path = "";

		if (args[0].startsWith("--path")) {
			path = args[0].replace("--path=", "");
		}

		ReaderResults rs = new ReaderResults();

		List<String> specialCases = rs.read(path + "datasets" + osValidation.osBarLine() + "full-results.csv");

		List<String> list = specialCases.stream().collect(Collectors.toList());

		try {
			Files.deleteIfExists(Paths.get(path + "datasets" + osValidation.osBarLine() + "specialCases.csv"));
			PrintStream fileStream = new PrintStream(
					new File(path + "datasets" + osValidation.osBarLine() + "specialCases.csv"));
			for (String project : list) {
				fileStream.println(project.toString());
			}
			fileStream.close();
			System.out.println("Successfully wrote to the file.");
		} catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}

		String directory = path + "projects";
		String results_dir = path + "out" + osValidation.osBarLine() + "results.csv";

		CommitsCompare.compare(list, directory, results_dir, path);

	}
}
