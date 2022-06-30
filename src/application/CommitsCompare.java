package application;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import br.unb.cic.cpp.evolution.git.RepositoryWalker;
import br.unb.cic.cpp.evolution.io.FileCSV;

//#recupera todos os arquivos que foram modificados ou apagados
//git diff --stat 12e7e38306d306ea61278315c4f155c940d520cf..a05a3b0bce00cc88c0346ecf7449b1d2ff6d2636 

//# exemplo de ver todas as mudanças naquele arquivo e retornar o email do autor da mudança em determinada linha
//git blame -e  kerfuffle/jobs.cpp 

//# forma de recuperar todas as linhas deletadas ou adicionadas entre dois commits
//git diff 12e7e38306d306ea61278315c4f155c940d520cf..a05a3b0bce00cc88c0346ecf7449b1d2ff6d2636 | grep "^-[^-]"
//git diff 12e7e38306d306ea61278315c4f155c940d520cf..a05a3b0bce00cc88c0346ecf7449b1d2ff6d2636 | grep "^+[^+]"

//#commando git que busca uma parte de texto e retorna quem introduziu ou modificou o código, o commit da alteração e etc..
//git log -S"foreach (const Archive::Entry* e, files) {" --pretty=format:'%h %an %ae %ad %s'

//#TODO: tentar identificar cenários em que a remoção não retorna no próximo commit; verificar se a qtd max de uma feature é no ultimo commit..

public class CommitsCompare {

	public static List<String> commits;

//	@SuppressWarnings("finally")
	public static Set<String> compare(List<String> cases, String directory, String results_dir) throws IOException {

		Set<String> modernizeCommits = new HashSet<String>();
		Set<String> modernizeCommitsMsg = new HashSet<String>();

		cloneRepositories(cases, directory);

		for (String commit : cases) {
			String[] data = commit.split(",");

			if (data[0].equals("Project") || data[8].equals("deletions")) {
				continue;
			}

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

			String projectPath = directory + "\\" + data[0];
			String projectName = data[0];

			try {
				SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

				File file = new File(results_dir);

				String start = sdf.format(file.lastModified());

				findEffortsToModernize(data[2], data[5], results_dir, projectName, projectPath, featureChanged);

				String end = sdf.format(file.lastModified());

				if (!start.equals(end)) {
					modernizeCommits.addAll(
							CheckForModernizationIncidents(results_dir, projectName, projectPath, featureChanged));
					modernizeCommits.stream().forEach(System.out::println);
				}

				PrintStream fileStream = new PrintStream(
						new File("D:\\walterlucas\\Documents\\cpp-evolution-paper\\datasets\\modernizations.csv"));
				for (String mc : modernizeCommits) {
					fileStream.println(mc);
				}
				fileStream.close();

				String[] words = { "modern", "modernize", "port away", "migrate", "migration", "use ", "c++11", "c++14",
						"c++17", "c++20" };
				String[] autoKeywords = { " auto ", "'auto'", " auto*" };
				String[] rangeKeywords = { "ranged", "range-based", " range " };
				String[] lambdaKeywords = { "lambda" };

				if (featureChanged.equals("ranged-for")) {
					modernizeCommitsMsg.addAll(findEffortsToModernizeInCommitMsgs(results_dir, projectName, projectPath,
							featureChanged, words, rangeKeywords));
				}
				if (featureChanged.equals("auto")) {
					modernizeCommitsMsg.addAll(findEffortsToModernizeInCommitMsgs(results_dir, projectName, projectPath,
							featureChanged, words, autoKeywords));
				}
				if (featureChanged.equals("lambda")) {
					modernizeCommitsMsg.addAll(findEffortsToModernizeInCommitMsgs(results_dir, projectName, projectPath,
							featureChanged, words, lambdaKeywords));
				}

				PrintStream fileStreamMsg = new PrintStream(
						new File("D:\\walterlucas\\Documents\\cpp-evolution-paper\\datasets\\modernizations-msgs.csv"));
				for (String mcm : modernizeCommitsMsg) {
					fileStreamMsg.println(mcm);
				}
				fileStreamMsg.close();

			} catch (Exception e) {
				System.out.println(e.getMessage());
				e.getStackTrace();
			}
		}

		return modernizeCommits;
	}

	public static boolean containsWords(String input, String[] words) {
		for (String string : words) {
			if (input.trim().contains(string)) {
				return true;
			}
		}
		return false;
	}

	private static List<String> findEffortsToModernizeInCommitMsgs(String results_dir, String projectName,
			String projectPath, String featureChanged, String[] words, String[] keywords) throws Exception {

		File project = new File(projectPath);

		Git git = Git.open(project);

		Iterable<RevCommit> rc = git.log().all().call();

		List<String> commitHashs = new ArrayList<String>();

		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");

		for (RevCommit revC : rc) {
			String fullMsg = revC.getFullMessage().toLowerCase();
			String shortMsg = revC.getShortMessage().toLowerCase();

			Date commitDate = sdf.parse(sdf.format(revC.getAuthorIdent().getWhen()));
			Date pattern = sdf.parse("01/01/2011");

			if (commitDate.before(pattern)) {
				continue;
			}

			if (containsWords(shortMsg, words) && containsWords(shortMsg, keywords)) {
				commitHashs.add(projectName + "," + sdf.format(revC.getAuthorIdent().getWhen()) + "," + revC.getName()
						+ "," + revC.getAuthorIdent().getName() + "," + revC.getAuthorIdent().getEmailAddress());
				break;
			} else if (containsWords(shortMsg, words) && containsWords(fullMsg, keywords)) {
				commitHashs.add(projectName + "," + sdf.format(revC.getAuthorIdent().getWhen()) + "," + revC.getName()
						+ "," + revC.getAuthorIdent().getName() + "," + revC.getAuthorIdent().getEmailAddress());
				break;
			} else if (containsWords(shortMsg, keywords)) {
				commitHashs.add(projectName + "," + sdf.format(revC.getAuthorIdent().getWhen()) + "," + revC.getName()
						+ "," + revC.getAuthorIdent().getName() + "," + revC.getAuthorIdent().getEmailAddress());
				break;
			}
		}

		git.close();

		return commitHashs;
	}

//	private static String changeDates(String date, String type) {
//		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
//		try {
//			Date dateFormated = sdf.parse(date);
//			Calendar cal = Calendar.getInstance();
//			cal.setTime(dateFormated);
//			DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
//			if (type.equals("init")) {
//				cal.add(Calendar.DATE, -1);
//				return dateFormat.format(cal.getTime());
//			} else {
//				cal.add(Calendar.DATE, 1);
//				return dateFormat.format(cal.getTime());
//			}
//		} catch (Exception e) {
//			e.getStackTrace();
//			System.out.println(e.getMessage());
//		}
//		return date;
//	}

	public static boolean stringContainsItemFromList(String inputStr, String[] items) {
		return Arrays.stream(items).anyMatch(inputStr::contains);
	}

	private static void findEffortsToModernize(String since, String until, String results_dir, String projectName,
			String projectPath, String featureChanged) throws Exception {

		FileCSV csv = new FileCSV(results_dir);

		File project = new File(projectPath);

		Git git = Git.open(project);

		Iterable<RevCommit> rc = git.log().addRange(ObjectId.fromString(since), ObjectId.fromString(until)).call();

		List<String> commitHashs = new ArrayList<String>();

		for (RevCommit revC : rc) {
			commitHashs.add(revC.getName());
		}

		if (commitHashs.size() <= 100) {
			System.out.println(projectName + ": " + commitHashs.size());
			RepositoryWalker walker = new RepositoryWalker(projectName, projectPath, commitHashs);
			walker.walk();

			csv.print(walker.getSummary());
		}

		csv.close();
		git.close();
	}

	private static Set<String> CheckForModernizationIncidents(String results_dir, String projectName,
			String projectPath, String featureChanged) throws Exception {

		ReaderResults rs = new ReaderResults();

		Set<String> commits = new HashSet<String>();
		List<String> specialCases = rs.read(results_dir);
		specialCases.stream().forEach(System.out::println);
		try {
			for (String cmt : specialCases) {
				List<String> lines = new ArrayList<String>();
				String[] splitered = cmt.split(",");
				if (splitered[2].equals("previousHash") || splitered[2].equals("changesHash")) {
					continue;
				} else {
					List<String> modernize = new ArrayList<String>();

					findEffortsToModernize(splitered[2], splitered[5], results_dir, projectName, projectPath,
							featureChanged);
					lines = readAllDataAtOnce(results_dir);
					List<String> cases = rs.read(results_dir);
					for (String c : cases) {
						String[] data = c.split(",");
						modernize = lines.stream().filter(t -> t.contains(data[5])).collect(Collectors.toList());
					}
					for (String m : modernize) {
						String line = m.substring(0, m.lastIndexOf(",") - 1);
						commits.add(line + featureChanged);
					}
				}
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return commits;
	}

	private static void cloneRepositories(List<String> cases, String directory) throws IOException {

		File file = new File(directory);
		FileUtils.cleanDirectory(file);
		String previousProject = "";

		for (String commit : cases) {
			String[] data = commit.split(",");
			if (data[0].equals("Project") || previousProject.equals(data[0]) || data[8].equals("deletions")) {
				continue;
			}
			System.out.println("Cloning: " + data[0] + " ....");
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
			csvReader.close();
			filereader.close();
			return commitsList;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
