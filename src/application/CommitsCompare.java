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

import org.apache.commons.lang3.time.DateUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.filter.CommitTimeRevFilter;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.ResetCommand;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import br.unb.cic.js.App;
import lombok.val;

public class CommitsCompare {

	public static List<String> commits;
	public static Set<String> revCommitsAnalyzed = new HashSet<String>();
	public static Set<String> modernizeCommits = new HashSet<String>();
	public static Set<String> modernizeCommitsMsg = new HashSet<String>();

	// @SuppressWarnings("finally")
	public static Set<String> compare(List<String> cases, String directory, String results_dir, String path,
			ArrayList<String> features) throws IOException {

		for (String commit : cases) {
			String[] data = commit.split(",");

			if (data[0].equals("Project") || data[8].equals("deletions")) {
				continue;
			}

			String featureChanged = data[7];

			System.out.println("Analyse case of - " + data[0].toUpperCase() + " - " + featureChanged + " " + data[8]);

			String projectPath = directory + "/" + data[0];
			String projectName = data[0];
			results_dir = results_dir + "/"+projectName+".csv";
			String since = data[2];
			String until = data[5];

			try {
				specialCases(since, until, results_dir, projectName, projectPath, featureChanged, path, features,
						commit);
			} catch (Exception e) {
				System.out.println(e.getMessage());
				e.getStackTrace();
			} catch (OutOfMemoryError error) {
				error.getStackTrace();
			}
		}

		return modernizeCommits;
	}

	private static void specialCases(String since, String until, String results_dir, String projectName,
			String projectPath, String featureChanged, String path, ArrayList<String> features, String commit)
			throws Exception {

		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

		File file = new File(results_dir);

		String start = sdf.format(file.lastModified());

		try {
			findEffortsToModernize(since, until, results_dir, projectName, projectPath, featureChanged, path, commit);
		} catch (OutOfMemoryError error) {
			error.getStackTrace();
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.getStackTrace();
		}

		String end = sdf.format(file.lastModified());

		if (!start.equals(end)) {

			Integer mcCount = modernizeCommits.size();

			try {
				modernizeCommits.addAll(CheckForModernizationIncidents(results_dir, projectName, projectPath,
						featureChanged, path, features, commit));
				if (mcCount < modernizeCommits.size()) {
					PrintStream fileStream = new PrintStream(
							new File(path + osValidation.osBarLine() + "modernizations.csv"));

					for (String mc : modernizeCommits) {
						fileStream.println(mc);
					}
					fileStream.close();
				}
			} catch (OutOfMemoryError error) {
				error.getStackTrace();
			} catch (Exception e) {
				System.out.println(e.getMessage());
				e.getStackTrace();
			}
		}
	}

	public static boolean containsWords(String input, String[] words) {
		for (String string : words) {
			if (input.trim().contains(string)) {
				return true;
			}
		}
		return false;
	}

	private static void modernizeInCommitMsgs(String since, String until, String results_dir, String projectName,
			String projectPath, String featureChanged, String path) throws Exception {

		String[] words = { "async declarations", "await declarations", "const declarations", "class declarations",
				"arrow function declarations", "let declarations", "export declarations", "yield declarations",
				"import statements", "promise declarations", "promise all and then", "default parameters",
				"rest statements", "spread arguments", "array destructuring", "object destructuring", "ES6", "ES8",
				"ES7" };

		Integer mcmCount = modernizeCommitsMsg.size();

		try {
			modernizeCommitsMsg.addAll(findEffortsToModernizeInCommitMsgs(since, until, results_dir, projectName,
					projectPath, featureChanged, words));

		} catch (OutOfMemoryError error) {
			error.getStackTrace();
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.getStackTrace();
		}

		if (mcmCount < modernizeCommitsMsg.size()) {
			PrintStream fileStreamMsg = new PrintStream(
					new File(path + osValidation.osBarLine() + "modernizations-msgs.csv"));
			for (String mcm : modernizeCommitsMsg) {
				fileStreamMsg.println(mcm);
			}
			fileStreamMsg.close();
		} else {
			System.out.println(modernizeCommitsMsg.size());
		}
	}

	private static List<String> findEffortsToModernizeInCommitMsgs(String since, String until, String results_dir,
			String projectName, String projectPath, String featureChanged, String[] words) throws Exception {

		List<String> commitHashs = new ArrayList<String>();

		Repository repository = Git.open(new File(projectPath + "/.git")).getRepository();

		val git = new Git(repository);

		val branches = git.branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call().stream()
				.filter(n -> n.getName().equals("refs/remotes/origin/HEAD")).findFirst();

		var mainBranch = "";

		if (branches.isPresent()) {
			mainBranch = branches.get().getTarget().getName().substring("refs/remotes/origin/".length());

			git.reset().setMode(ResetCommand.ResetType.HARD).call();
			git.checkout().setName(mainBranch).call();

			Iterable<RevCommit> rc = listCommitsInBranchWithinRange(repository, mainBranch, since, until);

			SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

			for (RevCommit revC : rc) {
				String fullMsg = revC.getFullMessage().toLowerCase();
				String shortMsg = revC.getShortMessage().toLowerCase();

				if (containsWords(shortMsg, words) || containsWords(fullMsg, words)) {
					commitHashs.add(projectName + "," + sdf.format(revC.getAuthorIdent().getWhen()) + ","
							+ revC.getName() + "," + revC.getAuthorIdent().getName() + ","
							+ revC.getAuthorIdent().getEmailAddress() + "," + featureChanged);
					break;
				}
			}
		}

		git.close();
		return commitHashs;
	}

	public static boolean stringContainsItemFromList(String inputStr, String[] items) {
		return Arrays.stream(items).anyMatch(inputStr::contains);
	}

	private static void findEffortsToModernize(String since, String until, String results_dir, String projectName,
			String projectPath, String featureChanged, String path, String commit) throws Exception {

		Repository repository = Git.open(new File(projectPath + "/.git")).getRepository();

		val git = new Git(repository);

		val branches = git.branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call().stream()
				.filter(n -> n.getName().equals("refs/remotes/origin/HEAD")).findFirst();

		var mainBranch = "";

		if (branches.isPresent()) {
			mainBranch = branches.get().getTarget().getName().substring("refs/remotes/origin/".length());

			git.reset().setMode(ResetCommand.ResetType.HARD).call();
			git.checkout().setName(mainBranch).call();

			Iterable<RevCommit> rc = listCommitsInBranchWithinRange(repository, mainBranch, since, until);

			List<String> commitHashs = new ArrayList<String>();

			for (RevCommit revC : rc) {
				if (!revCommitsAnalyzed.contains(projectName + "-" + revC.getName())) {
					commitHashs.add(revC.getName());
					revCommitsAnalyzed.add(projectName + "-" + revC.getName());
				}
			}
			if (commitHashs.size() == 0) {
				String[] data = commit.split(",");
				modernizeCommits.add(projectName+","+data[5]+","+data[6]+","+featureChanged);
				modernizeInCommitMsgs(since, until, results_dir, projectName, projectPath, featureChanged, path);
			} else if (commitHashs.size() <= 100 && commitHashs.size() > 0) {
				System.out.println(projectName + ": " + commitHashs.size());
				SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
				String sinceDate = sdf.format(getCommitDate(repository, since,"minusDays"));
				String untilDate = sdf.format(getCommitDate(repository, until,"addDays"));
				try {
					App.main(new String[] { "-d", projectPath, "-p", projectName, "-s", "0", "-id", sinceDate, "-ed",
							untilDate, "-ft", "10", "-pt", "1" });
				} catch (OutOfMemoryError error) {
					error.getStackTrace();
				} catch (Exception e) {
					System.out.println(e.getMessage());
					e.getStackTrace();
				}
			} else {
				System.out.println(projectName + ": " + commitHashs.size());
				modernizeInCommitMsgs(since, until, results_dir, projectName, projectPath, featureChanged, path);
			}
		}
		git.close();
	}

	public static Date getCommitDate(Repository repository, String objectId, String type) {
		try {
			ObjectId commitId = repository.resolve(objectId);
			RevCommit commit = repository.parseCommit(commitId);
			PersonIdent authorIdent = commit.getAuthorIdent();
			Date commitDate = authorIdent.getWhen();
			if(type.equals("addDays")){
				commitDate = DateUtils.addDays(commitDate, +1);
			}else{
				commitDate = DateUtils.addDays(commitDate, -1);
			}
			return commitDate;
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		return null;
	}

	public static Iterable<RevCommit> listCommitsInBranchWithinRange(Repository repository, String branchName,
			String sinceRevision, String untilRevision) throws GitAPIException, IOException {
		try (Git git = new Git(repository)) {
			Date initial = getCommitDate(repository, sinceRevision,"minusDays");
			Date end = getCommitDate(repository, untilRevision, "addDays");

			val head = repository.resolve("refs/heads/" + branchName);

			Iterable<RevCommit> revisions = git.log()
					.add(head)
					.setRevFilter(CommitTimeRevFilter.between(initial, end))
					.call();

			return revisions;
		}
	}

	private static Set<String> CheckForModernizationIncidents(String results_dir, String projectName,
			String projectPath, String featureChanged, String path, ArrayList<String> features, String commit)
			throws Exception {

		ReaderResults rs = new ReaderResults();

		Set<String> commits = new HashSet<String>();
		List<String> specialCases = rs.read(results_dir, features);
		specialCases.removeIf(s -> s.contains("previousHash"));
		specialCases.stream().forEach(System.out::println);
		try {
			for (String cmt : specialCases) {
				List<String> lines = new ArrayList<String>();
				String[] splitered = cmt.split(",");
				List<String> modernize = new ArrayList<String>();
				lines = readAllDataAtOnce(results_dir);

				if (specialCases.size() == 1) {
					for (String c : specialCases) {
						String[] data = c.split(",");
						modernize = lines.stream().filter(t -> t.contains(data[5])).collect(Collectors.toList());
					}
					for (String m : modernize) {
						String line = m.substring(0, m.lastIndexOf(",") - 1);
						commits.add(line + featureChanged);
					}
					break;
				}
				SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

				File file = new File(results_dir);

				String start = sdf.format(file.lastModified());

				specialCases(splitered[2], splitered[5], results_dir, projectName, projectPath, featureChanged, path,
						features, commit);

				String end = sdf.format(file.lastModified());

				if (!start.equals(end)) {
					lines = readAllDataAtOnce(results_dir);
					List<String> cases = rs.read(results_dir, features);
					cases.removeIf(s -> s.contains("previousHash"));
					if (cases.size() == 1) {
						for (String c : cases) {
							String[] data = c.split(",");
							modernize = lines.stream().filter(t -> t.contains(data[5])).collect(Collectors.toList());
						}
						for (String m : modernize) {
							String line = m.substring(0, m.lastIndexOf(",") - 1);
							commits.add(line + featureChanged);
						}
					} else if (cases.size() > 1) {
						for (String c : cases) {
							String[] data = c.split(",");
							specialCases(data[2], data[5], results_dir, projectName, projectPath, featureChanged, path,
									features, commit);
						}
					}
				} else {
					String since = splitered[2];
					String until = splitered[5];
					modernizeInCommitMsgs(since, until, results_dir, projectName, projectPath, featureChanged, path);
				}
			}
		} catch (OutOfMemoryError error) {
			error.getStackTrace();
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.getStackTrace();
		}
		return commits;
	}

	private static List<String> readAllDataAtOnce(String file) {
		try {
			FileReader filereader = new FileReader(file);

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
