package application;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import br.unb.cic.cpp.evolution.Main;

public class CommitsCompare {

	private static Set<String> autoCommits = new HashSet<String>();
	private static Set<String> lambdaCommits = new HashSet<String>();
	private static Set<String> rangedForCommits = new HashSet<String>();

	public static String compare(List<String> cases, String directory, String results_dir) throws IOException {

		cases = cases.stream().filter(t -> t.contains("okular")).collect(Collectors.toList());
		
		cloneRepositories(cases, directory);

		for (String commit : cases) {
			String[] data = commit.split(",");

			if (data[0].equals("Project") || data[8].equals("deletions")) {
				continue;
			}

			String initDate = changeDates(data[1], "init");
			String endDate = changeDates(data[4], "end");

			Integer featureValue = Integer.parseInt(data[3]);
			String featureChanged = data[7];

			switch (featureChanged) {
			case "ranged-for":
				System.out
						.println("Analyse case of - " + data[0].toUpperCase() + " - " + featureChanged + " " + data[8]);
				break;
			case "auto":
				System.out
						.println("Analyse case of - " + data[0].toUpperCase() + " - " + featureChanged + " " + data[8]);
				break;
			case "lambda":
				System.out
						.println("Analyse case of - " + data[0].toUpperCase() + " - " + featureChanged + " " + data[8]);
				break;
			default:
				break;
			}

			String parameters = "--threads=1 --date-init=" + initDate + " --date-end=" + endDate + " --step=0 "
					+ "--project=" + data[0];

			try {
				String executable = directory + " " + parameters;
				String[] args = executable.split(" ");

				CheckTypeOfOccurrence(args, featureChanged, results_dir, featureValue, data[8]);
				

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return "Total [auto-statement=" + autoCommits.size() + ",lambdaExpressions=" + lambdaCommits.size()
				+ ",rangedFor=" + rangedForCommits.size() + "]";
	}

	private static void cloneRepositories(List<String> cases, String directory) throws IOException {

		File file = new File(directory);
		FileUtils.cleanDirectory(file);		
		String previousProject = "";

		for (String commit : cases) {
			String[] data = commit.split(",");
			if (data[0].equals("Project") || data[8].equals("deletions") || previousProject.equals(data[0])) {
				continue;
			}
			System.out.println("Cloning: "+data[0]+" ....");
			try {
				file = new File(directory);
				File project = new File(directory + "\\" + data[0]);
				if (!project.exists()) {
					project.mkdir();
					Git git = Git.cloneRepository().setURI("https://github.com/KDE/" + data[0] + ".git")
							.setDirectory(project).call();
					git.close();
					previousProject = data[0];
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static String changeDates(String date, String type) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		try {
			Date dateFormated = sdf.parse(date);
			Calendar cal = Calendar.getInstance();
			cal.setTime(dateFormated);
			DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
			if (type.equals("init")) {
				cal.add(Calendar.DATE, -1);
				return dateFormat.format(cal.getTime());
			} else {
				cal.add(Calendar.DATE, 1);
				return dateFormat.format(cal.getTime());
			}
		} catch (Exception e) {
			e.getStackTrace();
			System.out.println(e.getMessage());
		}
		return date;
	}

	private static List<String> readAllDataAtOnce(String file) {
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
			return commitsList;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static void CheckTypeOfOccurrence(String[] args, String featureChanged, String results_dir,
			Integer featureValue, String changeType) {
		try {

			Main.main(args);

			List<String> commitsFromCsv = readAllDataAtOnce(results_dir);

			Integer featureValueFromNewAnalysis = null;

			for (String cmt : commitsFromCsv) {
				String[] splitered = cmt.split(",");
				switch (featureChanged) {
				case "ranged-for":
					featureValueFromNewAnalysis = Integer.parseInt(splitered[7]);
					if (featureValue + 20 <= featureValueFromNewAnalysis) {
						rangedForCommits.add(cmt);
					}
					featureValue = featureValueFromNewAnalysis;
					break;
				case "auto":
					featureValueFromNewAnalysis = Integer.parseInt(splitered[5]);
					if (featureValue + 20 <= featureValueFromNewAnalysis) {
						autoCommits.add(cmt);
					}
					featureValue = featureValueFromNewAnalysis;
					break;
				case "lambda":
					featureValueFromNewAnalysis = Integer.parseInt(splitered[4]);
					if (featureValue + 20 <= featureValueFromNewAnalysis) {
						lambdaCommits.add(cmt);
					}
					featureValue = featureValueFromNewAnalysis;
					break;
				default:
					break;
				}
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
}
