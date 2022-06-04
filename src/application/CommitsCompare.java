package application;

import java.io.File;
import java.io.FileReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jgit.api.Git;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import br.unb.cic.cpp.evolution.Main;

public class CommitsCompare {

	public static List<String> commits;

	@SuppressWarnings("finally")
	public static List<String> compare(List<String> cases, String directory) {

		commits = new ArrayList<String>(cases.stream().collect(Collectors.toList()).size());

		for (String commit : cases) {
			String[] data = commit.split(",");

			if (data[0].equals("Project")) {
				continue;
			}

			String initDate = changeDates(data[1], "init");
			String endDate = changeDates(data[4], "end");

			Integer featureValue = Integer.parseInt(data[6]);
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

			String parameters = "--threads=1 --date-init=" + initDate + " --date-end=" + endDate + " --step=0";
			try {
				File file = new File(directory);
				deleteDirectory(file);

				File project = new File(directory + "\\" + data[0]);
				Git git = Git.cloneRepository().setURI("https://github.com/KDE/" + data[0] + ".git")
						.setDirectory(project).call();
				String executable = directory + " " + parameters;
				String[] args = executable.split(" ");
				Main.main(args);

				List<String> commitsFromCsv = readAllDataAtOnce(
						"D:\\walterlucas\\Documents\\cpp-evolution-paper\\out\\results.csv");

				commitsFromCsv.stream().forEach(System.out::println);

				Integer featureValueFromNewAnalysis = null;
				String comparableHash = null;

				for (String cmt : commitsFromCsv) {
					String[] splitered = cmt.split(",");
					comparableHash = splitered[2];
					switch (featureChanged) {
					case "ranged-for":
						featureValueFromNewAnalysis = Integer.parseInt(splitered[7]);
						break;
					case "auto":
						featureValueFromNewAnalysis = Integer.parseInt(splitered[5]);
						break;
					case "lambda":
						featureValueFromNewAnalysis = Integer.parseInt(splitered[4]);
						break;
					default:
						break;
					}

//						#recupera todos os arquivos que foram modificados ou apagados
//						git diff --stat 12e7e38306d306ea61278315c4f155c940d520cf..a05a3b0bce00cc88c0346ecf7449b1d2ff6d2636 

//						# exemplo de ver todas as mudanças naquele arquivo e retornar o email do autor da mudança em determinada linha
//						git blame -e  kerfuffle/jobs.cpp 
						
//						# forma de recuperar todas as linhas deletadas ou adicionadas entre dois commits
//						git diff 12e7e38306d306ea61278315c4f155c940d520cf..a05a3b0bce00cc88c0346ecf7449b1d2ff6d2636 | grep "^-[^-]"
//						git diff 12e7e38306d306ea61278315c4f155c940d520cf..a05a3b0bce00cc88c0346ecf7449b1d2ff6d2636 | grep "^+[^+]"
//					#commando git que busca uma parte de texto e retorna quem introduziu ou modificou o código, o commit da alteração e etc..
//					git log -S"foreach (const Archive::Entry* e, files) {" --pretty=format:'%h %an %ae %ad %s'

					if (featureValue != featureValueFromNewAnalysis) {
						git.checkout().setName(comparableHash);
						System.out.println(git.log().call());
					}

				}

			} catch (Exception e) {
				System.out.println(e.getMessage());
			} finally {
				break;
			}
		}

		return commits;
	}

	public static boolean deleteDirectory(File directoryToBeDeleted) {
		File[] allContents = directoryToBeDeleted.listFiles();
		if (allContents != null) {
			for (File file : allContents) {
				deleteDirectory(file);
			}
		}
		return directoryToBeDeleted.delete();
	}

	public static String changeDates(String date, String type) {
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

	// Java code to illustrate reading a
	// all data at once
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
			return commitsList;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
