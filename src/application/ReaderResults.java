package application;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReaderResults {

	public int previousFeature = 0;
	public int previousStatements = 0;
	public String previousHash = null;
	public String previousDate = null;
	public int previousFiles = 0;
	public float step = 0.5f;

	public int statementsKey = 0;
	public int filesKey = 0;
	public int hashKey = 0;
	public int dateKey = 0;
	public int projectKey = 0;

	public List<String> interestingCases = new ArrayList<String>();

	public String currentProject = null;

	public HashMap<String, Integer> keyFeatures = new HashMap<>();

	public HashMap<String, Integer> allKeys(String path, ArrayList<String> features) {
		HashMap<String, Integer> allFeatures = new HashMap<>();
		String line = "";
		String splitBy = ",";
		try (BufferedReader br = new BufferedReader(new FileReader(path))) {
			while ((line = br.readLine()) != null) {
				String[] commit = line.split(splitBy);
				if (commit[projectKey].startsWith("project")) {
					for (String feature : features) {
						for (int i = 0; i < commit.length; i++) {
							if (commit[i].equals("statements")) {
								statementsKey = i;
							} else if (commit[i].equals("files")) {
								filesKey = i;
							} else if (commit[i].equals("revision")) {
								hashKey = i;
							} else if (commit[i].equals("date")) {
								dateKey = i;
							} else if (commit[i].equals("project")) {
								projectKey = i;
							} else if (commit[i].equals(feature)) {
								allFeatures.put(feature, i);
							}
						}
					}
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return allFeatures;
	}

	public List<String> read(String path, ArrayList<String> features) throws NumberFormatException, Exception {
		String line = "";
		String splitBy = ",";
		Integer keyVal = -1;
		Boolean header = false;

		keyFeatures = allKeys(path, features);

		for (Map.Entry<String, Integer> entry : keyFeatures.entrySet()) {
			String feature = entry.getKey();
			keyVal = entry.getValue();

			try (BufferedReader br = new BufferedReader(new FileReader(path))) {
				while ((line = br.readLine()) != null) {

					String[] commit = line.split(splitBy);

					if (commit[projectKey].startsWith("project")) {
						if (header == false) {
							interestingCases.add(
									"Project,previousDate,previousHash,previousValue,changesDate,changesHash,newValue,featureChanges,action");
							header = true;
						}
						continue;
					}

					if (currentProject == null) {
						currentProject = commit[projectKey];
					}

					if (currentProject.equals(commit[projectKey])) {

						CheckChangesIsInteresting(Integer.parseInt(commit[keyVal]), feature, currentProject,
								commit[dateKey],
								commit[hashKey], Integer.parseInt(commit[filesKey]),
								Integer.parseInt(commit[statementsKey]));

						previousHash = commit[hashKey];
						previousDate = commit[dateKey];
						previousFiles = Integer.parseInt(commit[filesKey]);

					} else {
						currentProject = commit[projectKey];
						previousHash = null;
						previousDate = null;
						previousFeature = 0;
						previousFiles = 0;

						CheckChangesIsInteresting(Integer.parseInt(commit[keyVal]), feature, currentProject,
								commit[dateKey],
								commit[hashKey], Integer.parseInt(commit[filesKey]),
								Integer.parseInt(commit[statementsKey]));

						previousHash = commit[hashKey];
						previousDate = commit[dateKey];
					}

					previousStatements = Integer.parseInt(commit[statementsKey]);
				}
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return interestingCases.stream().collect(Collectors.toList());

	}

	public void CheckChangesIsInteresting(int value, String feature, String project, String date, String hash,
			int files, int statements) throws Exception {
		if (value > previousFeature + (previousFeature * step)
				&& statements < previousStatements + (previousStatements * 0.5) && previousFeature > 10) {
			interestingCases.add(project + "," + previousDate + "," + previousHash + "," + previousFeature + "," + date
					+ "," + hash + "," + value + "," + feature + ",addtions");
		} else if (value < previousFeature - (previousFeature * step)
				&& statements < previousStatements - (previousStatements * 0.5) && previousFeature > 10) {
			interestingCases.add(project + "," + previousDate + "," + previousHash + "," + previousFeature + "," + date
					+ "," + hash + "," + value + "," + feature + ",deletions");
		}
		previousFeature = value;
	}
}