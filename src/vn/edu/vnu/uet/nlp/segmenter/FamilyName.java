package vn.edu.vnu.uet.nlp.segmenter;

import java.io.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Vietnamese family name.
 * 
 * @author tuanphong94
 *
 */
@SuppressWarnings("unchecked")
public class FamilyName {
	private Set<String> nameList;
	private static String path = "dictionary/VNFamilyNameObject";

	public FamilyName() {
		getInstance();
	}

	public FamilyName(Set<String> loadedList) {
		nameList = Collections.unmodifiableSet(loadedList);
	}

	private void getInstance() {
		nameList = new HashSet<String>();
		FileInputStream fin = null;
		try {
			fin = new FileInputStream(path);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		ObjectInputStream ois = null;
		try {
			ois = new ObjectInputStream(fin);
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			nameList = Collections.unmodifiableSet((Set<String>) ois.readObject());
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
		try {
			ois.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public boolean isVNFamilyName(String syl) {
		if (nameList == null) {
			getInstance();
		}
		return nameList.contains(syl.trim().toLowerCase());
	}

	public static void setPath(String _path) {
		path = _path;
	}
}
