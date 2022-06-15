package application;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CSVReader {

	public int previousLambda = 0;
	public int previousAuto = 0;
	public int previousRangedFor = 0;
	public int previousConstExpr = 0;
	public int previousStatements = 0;
	public String previousHash = null;
	public String previousDate = null;
	public int previousFiles = 0;
	public float step = 0.5f;
	
	public List<String> interestingCases = new ArrayList<String>();

	public String currentProject = null;

	public List<String> read(String path) throws NumberFormatException, Exception {
		String line = "";
		String splitBy = ",";

		try (BufferedReader br = new BufferedReader(new FileReader(path))) {
			while ((line = br.readLine()) != null) {

				String[] commit = line.split(splitBy);

				if (commit[0].startsWith("project")) {
					interestingCases.add("Project,previousDate,previousHash,previousValue,changesDate,changesHash,newValue,featureChanges,action");
					continue;
				}
				

				if (currentProject == null) {
					currentProject = commit[0];
				}

				if (currentProject.equals(commit[0])) {

					CheckChangesIsInteresting(Integer.parseInt(commit[4]), "lambda", currentProject, commit[1],
							commit[2],Integer.parseInt(commit[3]),Integer.parseInt(commit[16]));
					CheckChangesIsInteresting(Integer.parseInt(commit[5]), "auto", currentProject, commit[1],
							commit[2],Integer.parseInt(commit[3]),Integer.parseInt(commit[16]));
					CheckChangesIsInteresting(Integer.parseInt(commit[7]), "ranged-for", currentProject, commit[1],
							commit[2],Integer.parseInt(commit[3]),Integer.parseInt(commit[16]));

					previousHash = commit[2];
					previousDate = commit[1];
					previousFiles = Integer.parseInt(commit[3]);

				} else {
					currentProject = commit[0];
					previousHash = null;
					previousDate = null;
					previousLambda = 0;
					previousAuto = 0;
					previousRangedFor = 0;
					previousConstExpr = 0;
					previousFiles = 0;
					

					CheckChangesIsInteresting(Integer.parseInt(commit[4]), "lambda", currentProject, commit[1],
							commit[2],Integer.parseInt(commit[3]),Integer.parseInt(commit[16]));
					CheckChangesIsInteresting(Integer.parseInt(commit[5]), "auto", currentProject, commit[1],
							commit[2],Integer.parseInt(commit[3]),Integer.parseInt(commit[16]));
					CheckChangesIsInteresting(Integer.parseInt(commit[7]), "ranged-for", currentProject, commit[1],
							commit[2],Integer.parseInt(commit[3]),Integer.parseInt(commit[16]));

					previousHash = commit[2];
					previousDate = commit[1];
				}
				
				previousStatements = Integer.parseInt(commit[16]);

			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return interestingCases.stream().collect(Collectors.toList());

	}

//	feature + (feature * 0.5) > feature && statements < statements + (statements * 0.5)  --> por projeto
	
	public void CheckChangesIsInteresting(int value, String feature, String project, String date, String hash, int files, int statements)
			throws Exception {
		switch (feature) {
		case "lambda":
			if (value > previousLambda + (previousLambda * step) && statements < previousStatements + (previousStatements * 0.05) && previousLambda > 20) {
				interestingCases.add(project + "," + previousDate + ","+ previousHash + "," + previousLambda + "," + date + ","+ hash + "," + value + "," + feature + ",addtions");
			} else if (value < previousLambda * step && statements < previousStatements * 0.05 && previousLambda > 20) {
				interestingCases.add(project + "," + previousDate + ","+ previousHash + "," + previousLambda + "," + date + ","+ hash + "," + value + "," + feature + ",deletions");
			}
			previousLambda = value;
			break;
		case "auto":
			if (value > previousAuto + (previousAuto * step) && statements < previousStatements + (previousStatements * 0.05) && previousAuto > 20) {
				interestingCases.add(project + "," + previousDate + ","+ previousHash + "," + previousAuto + "," + date + ","+ hash + "," + value + "," + feature + ",addtions");
			} else if (value < previousAuto * step && statements < previousStatements * 0.05 && previousAuto > 20) {
				interestingCases.add(project + "," + previousDate + ","+ previousHash + "," + previousAuto + "," + date + ","+ hash + "," + value + "," + feature + ",deletions");
			}
			previousAuto = value;
			break;
		case "ranged-for":
			if (value > previousRangedFor + (previousRangedFor * step) && statements < previousStatements + (previousStatements * 0.05)  && previousRangedFor > 20) {
				interestingCases.add(project + "," + previousDate + ","+ previousHash + "," + previousRangedFor + "," + date + ","+ hash + "," + value + "," + feature + ",addtions");
			} else if (value < previousRangedFor * step && statements < previousStatements * 0.05  && previousRangedFor > 20) {
				interestingCases.add(project + "," + previousDate + ","+ previousHash + "," + previousRangedFor + "," + date + ","+ hash + "," + value + "," + feature + ",deletions");
			}
			previousRangedFor = value;
			break;
		default:
			break;
		}
	}
}
