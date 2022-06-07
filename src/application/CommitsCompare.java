package application;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
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

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import br.unb.cic.cpp.evolution.Main;
import entities.CodeSnippet;

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

	@SuppressWarnings("finally")
	public static Set<CodeSnippet> compare(List<String> cases, String directory, String results_dir) {

		Set<CodeSnippet> commits = new HashSet<CodeSnippet>();

		for (String commit : cases) {
			String[] data = commit.split(",");

			if (data[0].equals("Project")) {
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

			String parameters = "--threads=1 --date-init=" + initDate + " --date-end=" + endDate + " --step=0";
			try {
				File file = new File(directory);
				deleteDirectory(file);

				boolean projectDir = new File(directory + "\\" + data[0]).mkdirs();
				File project = null;
				if (projectDir) {
					project = new File(directory + "\\" + data[0]);
				}

				Git.cloneRepository().setURI("https://github.com/KDE/" + data[0] + ".git").setDirectory(project).call();

				String executable = directory + " " + parameters;
				String[] args = executable.split(" ");
				String typeOfCommitChange = CheckTypeOfOccurrence(args, featureChanged, results_dir, featureValue,
						data[8]);
				System.out.println(typeOfCommitChange);

				Git git = Git.open(project);

				RevCommit before = null;
				RevCommit after = null;

				Repository repository = git.getRepository();
				RevWalk walk = new RevWalk(repository);
				ObjectId commitId = ObjectId.fromString(data[2]);
				before = walk.parseCommit(commitId);
				commitId = ObjectId.fromString(data[5]);
				after = walk.parseCommit(commitId);
				RevTree treeBefore = walk.parseTree(before.getTree().getId());
				RevTree treeAfter = walk.parseTree(after.getTree().getId());

				List<DiffEntry> diffs = git.diff().setOldTree(prepareTreeParser(repository, treeBefore))
						.setNewTree(prepareTreeParser(repository, treeAfter)).call();

				ArrayList<String> linesDiff = callDiff(diffs, git);

				Set<String> deletedLines = new HashSet<String>();
				Set<String> addedLines = new HashSet<String>();
				Set<CodeSnippet> linesInMethods = null;

				if (data[8].equals("deletions")) {
					deletedLines = getLinesChanged(linesDiff, "- ");
					linesInMethods = findInMethodsFile(deletedLines,
							results_dir.replaceAll("results.csv", "") + data[0] + ".md","-");
				} else {
					addedLines = getLinesChanged(linesDiff, "+ ");
					linesInMethods = findInMethodsFile(addedLines,
							results_dir.replaceAll("results.csv", "") + data[0] + ".md","+");
				}
				

				commits = linesInMethods;
				System.out.println("TOTAL: "+commits.size());
				System.out.println("RANGE FOR STATEMENT: "+commits.stream().filter(t -> t.getType().equals("RANGE FOR STATEMENT")).count());
				System.out.println("LAMBDA EXPRESSION: "+commits.stream().filter(t -> t.getType().equals("LAMBDA EXPRESSION")).count());
				System.out.println("AUTO: "+commits.stream().filter(t -> t.getType().equals("AUTO")).count());


				try {
					FileWriter myWriter = new FileWriter(
							results_dir.replaceAll("results.csv", "") + data[0] + "-" + initDate + "-diff.txt");
					for (String line : linesDiff) {
						myWriter.write(line);
					}
					myWriter.close();
				} catch (IOException e) {
					System.out.println("An error occurred.");
					e.printStackTrace();
				}

				walk.close();
				git.close();

				break;

			} catch (Exception e) {
				System.out.println(e.getMessage());
				e.getStackTrace();
			} finally {
				break;
			}
		}

		return commits;
	}

	@SuppressWarnings("null")
	private static Set<CodeSnippet> findInMethodsFile(Set<String> lines, String path,String initChar) {
		List<CodeSnippet> methods = convertMethodsToList(path);
		Set<CodeSnippet> changedMethods = new HashSet<CodeSnippet>();
		for (String line : lines) {
			if (methods.stream().filter(t -> t.getBody().contains(line.replaceAll(initChar, "").trim())).count() > 0) {
				changedMethods.addAll(methods.stream().filter(t -> t.getBody().contains(line.replaceAll(initChar, "").trim())).collect(Collectors.toList()));
			}
		}
		return changedMethods;
	}

	private static List<CodeSnippet> convertMethodsToList(String path) {
		List<CodeSnippet> methods = new ArrayList<CodeSnippet>();
		try (BufferedReader br = new BufferedReader(new FileReader(path))) {
		    String line;
			String typeChange = null;
			String body = "";
			while ((line = br.readLine()) != null) {
				line = line.replace("\n", "").replace("\r", "").trim();
				if (line.startsWith("####")) {
					if (typeChange == null) {
						typeChange = line.replaceFirst("#### ", "");
					} else {
						CodeSnippet cs = new CodeSnippet();
						cs.setType(typeChange);
						cs.setBody(body);
						methods.add(cs);
						typeChange = line.replaceFirst("#### ", "");
						body = "";
					}
				} else if (line.startsWith("```{c}")) {
					continue;
				} else if (line.startsWith("```")) {
					continue;
				} else if (line.length() == 0 || line == "") {
					continue;
				} else {
					body += line + "¬¬";
				}
			}
			return methods;
		} catch (IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		
		return methods;
	}

	private static boolean deleteDirectory(File directoryToBeDeleted) {
		File[] allContents = directoryToBeDeleted.listFiles();
		if (allContents != null) {
			for (File file : allContents) {
				deleteDirectory(file);
			}
		}
		return directoryToBeDeleted.delete();
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
				cal.add(Calendar.DATE, 30);
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

	private static String CheckTypeOfOccurrence(String[] args, String featureChanged, String results_dir,
			Integer featureValue, String changeType) {
		try {

			Main.main(args);

			List<String> commitsFromCsv = readAllDataAtOnce(results_dir);

			Integer featureValueFromNewAnalysis = null;
//			String comparableHash = null;
			int qtdDeletions = 0;
			int qtdAdditons = 0;

			for (String cmt : commitsFromCsv) {
				String[] splitered = cmt.split(",");
//				comparableHash = splitered[2];
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

				if (featureValue < featureValueFromNewAnalysis) {
					qtdDeletions++;
				} else if (featureValue > featureValueFromNewAnalysis) {
					qtdAdditons++;
				} else if (featureValueFromNewAnalysis == featureValue) {
					qtdDeletions = 0;
					qtdAdditons = 0;
				}
			}

			System.out.println("Diff - deletions=" + qtdDeletions + ",additions=" + qtdAdditons);

			if (changeType.equals("deletions") && qtdDeletions > 0 && qtdAdditons == 0) {
				return "Delete Refactoring";
			} else if (changeType.equals("deletions") && qtdDeletions > 0 && qtdAdditons > 0) {
				return "Commit merge removal";
			} else if (changeType.equals("deletions") && qtdDeletions == 0 && qtdAdditons == 0) {
				return "Commit merge removal";
			}

			if (changeType.equals("addtions") && qtdDeletions == 0 && qtdAdditons > 0) {
				return "Rejuvenation Refactoring";
			} else if (changeType.equals("addtions") && qtdDeletions > 0 && qtdAdditons > 0) {
				return "Improvements Refactoring";
			} else if (changeType.equals("addtions") && qtdDeletions == 0 && qtdAdditons == 0) {
				return "Commit merge addtional";
			}

		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return null;
	}

	private static AbstractTreeIterator prepareTreeParser(Repository repository, RevTree tree)
			throws IOException, MissingObjectException, IncorrectObjectTypeException {

		CanonicalTreeParser oldTreeParser = new CanonicalTreeParser();
		ObjectReader oldReader = repository.newObjectReader();
		try {
			oldTreeParser.reset(oldReader, tree.getId());
		} finally {
			oldReader.close();
		}
		return oldTreeParser;
	}

	private static ArrayList<String> callDiff(List<DiffEntry> diffs, Git git) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();

			DiffFormatter df = new DiffFormatter(out);
			// Set the repository the formatter can load object contents from.
			df.setRepository(git.getRepository());
			ArrayList<String> diffText = new ArrayList<String>();
			// A DiffEntry is 'A value class representing a change to a file' therefore for
			// each file you have a diff entry
			for (DiffEntry diff : diffs) {
				try {
					df.setContext(0);
					// Format a patch script for one file entry.
					df.format(diff);
					RawText r = new RawText(out.toByteArray());
					r.getLineDelimiter();
					diffText.add(out.toString());
					out.reset();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			df.close();
			return diffText;
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return null;
	}

	private static Set<String> getLinesChanged(List<String> linesDiff, String search) {
		Set<String> matchLines = new HashSet<String>();
		for (String line : linesDiff) {
			line = line.replace("\n", "¬¬").replace("\r", "¬¬");
			String[] fileLineChanges = line.split("¬¬");
			if (fileLineChanges[0].contains(".cpp") || fileLineChanges[0].contains(".hpp")
					|| fileLineChanges[0].contains(".h")) {
				for (String flc : fileLineChanges) {
					if (flc.startsWith(search)) {
						matchLines.add(flc);
					} else {
						continue;
					}
				}
			}
		}
		return matchLines;
	}
}
