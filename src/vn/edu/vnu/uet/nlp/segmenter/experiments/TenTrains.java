package vn.edu.vnu.uet.nlp.segmenter.experiments;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.List;

import vn.edu.vnu.uet.nlp.segmenter.*;

public class TenTrains {

	public static void main(String[] args) throws IOException {
		int numSamples = 0;
		int numSentences = 0;
		List<List<SegmentFeature>> segList = new ArrayList<>();

		for (int i = 0; i < 10; i++) {
			System.out.println("--------------------fold " + i + "--------------------");

			File modelFile = new File("testmodels/models_" + i);
			if (!modelFile.exists())
				modelFile.mkdirs();

			// Train
			Path path = Paths.get("data/train_" + i + ".txt");
			BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8);

			FeatureExtractor fe = new FeatureExtractor();

			System.out.println("extracting features...");
			String line = null;
			int cnt = 0;
			while ((line = br.readLine()) != null) {
				if (line.isEmpty())
					continue;
				ExtractedFeatures ef = fe.extract(Normalizer.normalize(line, Form.NFC), Configure.TRAIN);
				numSamples += ef.getNumSamples();
				segList.add(ef.getSegmentList());

				if (cnt % 1000 == 0 && cnt > 0) {
					System.out.println(cnt + " sentences extracted to features");
				}
				numSentences++;
				cnt++;
			}
			System.out.println(cnt + " sentences extracted to features");
			System.out.println("\t\t\t\t\t\tTotal number of unique features: " + fe.getFeatureMapSize());
			br.close();

			SegmentationSystem machine = new SegmentationSystem(fe, "testmodels/models_" + i);
			machine.train(numSamples, numSentences, segList);

		}

	}

}
