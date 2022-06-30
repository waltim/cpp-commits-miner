package application;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

public class Program {

	public static void main(String[] args) throws Exception {

		ReaderResults rs = new ReaderResults();

		List<String> specialCases = rs
				.read("D:\\walterlucas\\Documents\\cpp-evolution-paper\\datasets\\all-results.csv");

		List<String> list = specialCases.stream().collect(Collectors.toList());

		try {
			Files.deleteIfExists(
					Paths.get("D:\\walterlucas\\Documents\\cpp-evolution-paper\\datasets\\specialCases.csv"));
			PrintStream fileStream = new PrintStream(
					new File("D:\\walterlucas\\Documents\\cpp-evolution-paper\\datasets\\specialCases.csv"));
			for (String project : list) {
				fileStream.println(project.toString());
			}
			fileStream.close();
			System.out.println("Successfully wrote to the file.");
		} catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}

		String directory = "D:\\walterlucas\\Documents\\cpp-evolution-paper\\projects";
		String results_dir = "D:\\walterlucas\\Documents\\cpp-evolution-paper\\out\\results.csv";

		Set<String> modernizationCommits = CommitsCompare.compare(list, directory, results_dir);

		modernizationCommits.stream().forEach(System.out::println);

	}
	
	
	public static void checkProjectsWithoutAnalysis() {
		List<String> programs = new ArrayList<String>();

		programs = readAllDataAtOnce("D:\\walterlucas\\Documents\\cpp-evolution-paper\\dataset-filtered.csv");

		File file = new File("D:\\walterlucas\\Documents\\cpp-evolution-paper\\out\\full-results.csv");

		List<String> projectsWithoutAnalysis = new ArrayList<String>();

		for (String program : programs) {
			String[] p = program.split(",");
			try {
				Scanner scanner = new Scanner(file);
				int lineNum = 0;
				while (scanner.hasNextLine()) {
					String line = scanner.nextLine();
					if (line.startsWith(p[1].trim())) {
						lineNum++;
						break;
					}
				}
				if (lineNum == 0) {
					projectsWithoutAnalysis.add("kde,"+p[1].trim()+",https://github.com/kde/" + p[1].trim() + ".git");
				}
				scanner.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		try {
			PrintStream fileStream = new PrintStream(
					new File("D:\\walterlucas\\Documents\\cpp-evolution-paper\\projectsWithoutAnalysis.csv"));
			
			fileStream.println("organization,repository_name,clone_url");
			
			for (String project : projectsWithoutAnalysis) {
				fileStream.println(project);
			}
			fileStream.close();
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	public static List<String> readAllDataAtOnce(String file) {
		try {
			// Create an object of file reader
			// class with CSV file as a parameter.
			FileReader filereader = new FileReader(file);

			// create csvReader object and skip first Line
			CSVReader csvReader = new CSVReaderBuilder(filereader).withSkipLines(1).build();
			List<String[]> allData = csvReader.readAll();
			List<String> commitsList = new ArrayList<String>();
			for (String[] row : allData) {
				String line = "";
				int i = 0;
				for (String cell : row) {
					if (i == row.length - 1) {
						line += cell;
					} else {
						line += cell + ",";
					}
					i++;
				}
				commitsList.add(line);
			}
			csvReader.close();
			filereader.close();
			return commitsList;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
