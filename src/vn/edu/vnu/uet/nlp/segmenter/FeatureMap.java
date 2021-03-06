package vn.edu.vnu.uet.nlp.segmenter;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Contain the map of String feature -> integer number.
 * 
 * @author tuanphong94
 *
 */
public class FeatureMap {
	private Map<String, Integer> map;

	public FeatureMap() {
		map = new HashMap<>(100000);
	}

	public Integer getIndex(String feature, int mode) {
		feature = feature.intern();
		if (!map.containsKey(feature)) {
			// PREDICT or TEST mode
			if (mode == Configure.PREDICT || mode == Configure.TEST) {
				return map.size();
			}

			// TRAIN mode
			map.put(feature, map.size());

			if (getSize() % 50000 == 0 && getSize() > 0) {
				System.out.println("\t\t\t\t\t\tNumber of unique features: " + getSize());
			}

			return map.size() - 1;

		} else {
			return map.get(feature);
		}
	}

	public int getSize() {
		return map.size();
	}

	@SuppressWarnings("unchecked")
	public void load(String path) throws IOException, ClassNotFoundException {
		FileInputStream fin = new FileInputStream(path);
		ObjectInputStream ois = new ObjectInputStream(fin);
		map = Collections.unmodifiableMap((HashMap<String, Integer>) ois.readObject());
		ois.close();
	}

	public void load(InputStream stream) throws IOException, ClassNotFoundException {
		ObjectInputStream ois = new ObjectInputStream(stream);
		map = Collections.unmodifiableMap((HashMap<String, Integer>) ois.readObject());
		ois.close();
	}

	public void save(String path) throws IOException {
		Path filePath = Paths.get(path);
		BufferedWriter file = Files.newBufferedWriter(filePath, Charset.forName("utf-8"), StandardOpenOption.CREATE);
		file.close();

		FileOutputStream fout = new FileOutputStream(path);
		ObjectOutputStream oos = new ObjectOutputStream(fout);
		oos.writeObject(map);
		oos.close();
	}

	@Override
	public String toString() {
		Set<String> keys = map.keySet();
		StringBuffer sb = new StringBuffer();
		sb.append("Feature map size: " + map.size() + "\n");
		for (String key : keys) {
			sb.append(key + "\t" + map.get(key) + "\n");
		}
		return sb.toString();
	}

}
