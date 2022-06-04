package application;

import java.util.List;
import java.util.stream.Collectors;

public class Program {

	public static void main(String[] args) throws Exception {
		
		CSVReader csvr = new CSVReader();

		List<String> specialCases = csvr
				.read("D:\\walterlucas\\Documents\\cpp-evolution-paper\\datasets\\all-results.csv");

		List<String> list = specialCases.stream().collect(Collectors.toList());

//		try {
//			Files.deleteIfExists(Paths.get("D:\\walterlucas\\Documents\\cpp-evolution-paper\\datasets\\specialCases.csv"));
//			PrintStream fileStream = new PrintStream(new File("D:\\walterlucas\\Documents\\cpp-evolution-paper\\datasets\\specialCases.csv"));
//			for (String project : list) {
//				fileStream.println(project.toString());
//			}
//			fileStream.close();
//			System.out.println("Successfully wrote to the file.");
//		} catch (IOException e) {
//			System.out.println("An error occurred.");
//			e.printStackTrace();
//		}
		
		String directory = "D:\\walterlucas\\Documents\\cpp-evolution-paper\\projects";
		CommitsCompare.compare(list, directory);
	}
}
