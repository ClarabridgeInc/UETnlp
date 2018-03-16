package vn.edu.vnu.uet.nlp.segmenter;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Used for detecting rare names such as David de Gea, Louis van Gaal, etc.
 * 
 * Do not use for testing.
 * 
 * @author tuanphong94
 * 
 */
public class RareNames {
	private Set<String> list;
	private static String path = "dictionary/rare_names.txt";

	public RareNames() {
		getInstance();
	}

	public RareNames(Set<String> nameList) {
		list = Collections.unmodifiableSet(nameList);
	}

	private void getInstance() {
		Set<String> loadedList = new HashSet<>();

		Path p = Paths.get(path);
		BufferedReader br = null;
		try {
			br = Files.newBufferedReader(p, StandardCharsets.UTF_8);
			String line = null;

			while ((line = br.readLine()) != null) {
				if (!line.isEmpty()) {
					loadedList.add(line.toLowerCase().trim());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("The dictionary of rare names 'dictionary/rare_names.txt' is not found!");
		}
		list = Collections.unmodifiableSet(loadedList);
	}

	public boolean isRareName(String word) {
		if (word == null || word.isEmpty())
			return false;
		if (list == null) {
			getInstance();
		}

		return list.contains(word.trim().toLowerCase());
	}

	public static void setPath(String _path) {
		path = _path;
	}
}
