package vn.edu.vnu.uet.nlp.segmenter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.SortedSet;

import edu.emory.clir.clearnlp.collection.pair.Pair;
import vn.edu.vnu.uet.liblinear.Feature;
import vn.edu.vnu.uet.liblinear.FeatureNode;
import vn.edu.vnu.uet.liblinear.Linear;
import vn.edu.vnu.uet.liblinear.Model;
import vn.edu.vnu.uet.liblinear.Parameter;
import vn.edu.vnu.uet.liblinear.Problem;
import vn.edu.vnu.uet.liblinear.SolverType;
import vn.edu.vnu.uet.nlp.segmenter.experiments.Segment;
import vn.edu.vnu.uet.nlp.segmenter.measurement.F1Score;
import vn.edu.vnu.uet.nlp.segmenter.measurement.ScoreKeeper;
import vn.edu.vnu.uet.nlp.tokenizer.StringConst;
import vn.edu.vnu.uet.nlp.utils.FileUtils;
import vn.edu.vnu.uet.nlp.utils.Logging;

/**
 * The segmentation system contains three components: Longest matching, Logistic
 * regression, Post-processing.
 * 
 * @author tuanphong94
 *
 */
public class SegmentationSystem {
	private static double r = 0.33;
	private Parameter parameter;
	private Model model;
	private FeatureExtractor fe;
	private String pathToSave = "models";
	private int n; // number of unique features, used as parameter of LIBLINEAR

	private Dictionary dictionary;
	private RareNames rareNames;

	// Constructor for TRAINING
	public SegmentationSystem(FeatureExtractor _fe, String pathToSave) {
		this.parameter = new Parameter(SolverType.L2R_LR, 1.0, 0.01);
		this.fe = _fe;
		this.pathToSave = pathToSave;
		File file = new File(pathToSave);
		if (!file.exists()) {
			file.mkdirs();
		}

		this.n = fe.getFeatureMap().getSize();
		dictionary = new Dictionary();
		rareNames = new RareNames();
	}

	// Constructor for TESTING and SEGMENTING
	public SegmentationSystem(String folderpath) throws ClassNotFoundException, IOException {
		parameter = new Parameter(SolverType.L2R_LR, 1.0, 0.01);
		load(folderpath);
		dictionary = new Dictionary();
		rareNames = new RareNames();
	}

	public SegmentationSystem(Model segmenterModel, FeatureExtractor segmenterFeatureExtractor, Dictionary dictionary, RareNames rareNames) throws ClassNotFoundException, IOException {
		parameter = new Parameter(SolverType.L2R_LR, 1.0, 0.01);

		model = segmenterModel;
		fe = segmenterFeatureExtractor;
		pathToSave = null;
		n = fe.getFeatureMap().getSize();
		this.dictionary = dictionary;
		this.rareNames = rareNames;
	}

	// Convert to problem's format of LIBLINEAR
	private Problem setProblem(int numSamples, int numSentences, List<List<SegmentFeature>> segmentList) {
		FeatureNode[][] x = new FeatureNode[numSamples][];
		double[] y = new double[numSamples];

		SortedSet<Integer> featSet;

		int sampleNo = 0;

		for (int s = 0; s < numSentences; s++) {
			for (int i = 0; i < segmentList.get(s).size(); i++) {
				y[sampleNo] = segmentList.get(s).get(i).getLabel();

				featSet = segmentList.get(s).get(i).getFeatset();

				x[sampleNo] = new FeatureNode[featSet.size()];

				int cnt = 0;
				for (Integer t : featSet) {
					x[sampleNo][cnt] = new FeatureNode(
							t + 1 /* Feature index must be greater than 0 */, 1.0);
					cnt++;
				}

				// free the memory
				featSet.clear();
				sampleNo++;
			}
		}
		Problem problem = new Problem();
		problem.l = numSamples;
		problem.n = n;
		problem.y = y;
		problem.x = x;
		problem.bias = -1;

		return problem;
	}


	// Convert to problem's format of LIBLINEAR
	private Problem setProblem(int numSamples, List<SegmentFeature> segmentList) {
		FeatureNode[][] x = new FeatureNode[numSamples][];
		double[] y = new double[numSamples];

		SortedSet<Integer> featSet;

		int sampleNo = 0;

		for (SegmentFeature segmentFeature : segmentList) {
			y[sampleNo] = segmentFeature.getLabel();

			featSet = segmentFeature.getFeatset();

			x[sampleNo] = new FeatureNode[featSet.size()];

			int cnt = 0;
			for (Integer t : featSet) {
				x[sampleNo][cnt] = new FeatureNode(
						t + 1 /* Feature index must be greater than 0 */, 1.0);
				cnt++;
			}

			// free the memory
			featSet.clear();
			sampleNo++;
		}
		Problem problem = new Problem();
		problem.l = numSamples;
		problem.n = n;
		problem.y = y;
		problem.x = x;
		problem.bias = -1;

		return problem;
	}

	/**
	 * Training the logistic regression classifier.
	 */
	public void train(int numSamples, int numSentences, List<List<SegmentFeature>> segmentList) {
		Logging.LOG.info("saving feature map");
		saveMap();

		Logging.LOG.info("setting up the problem");
		Problem problem = setProblem(numSamples, numSentences, segmentList);

		Logging.LOG.info("start training");
		Model model = Linear.train(problem, parameter);
		Logging.LOG.info("finish training");

		Logging.LOG.info("saving model");
		saveModel(model);

		Logging.LOG.info("finish.");
	}

	/**
	 * @param sentences
	 *            A list of word-segmented sentences
	 * @return F1Score
	 * @throws IOException
	 */
	public F1Score test(List<String> sentences) throws IOException {
		int sentCnt = 0;
		ScoreKeeper score = new ScoreKeeper();
		// create log file
		try {
			File fol = new File("log");
			fol.mkdir();
		} catch (Exception e) {
			// do nothing
		}
		String logName = "log/log_test_" + new Date() + ".txt";
		BufferedWriter bw = FileUtils.newUTF8BufferedWriterFromNewFile(logName);
		// finish creating log file

		int sentID = 1;
		for (String sentence : sentences) {
			sentence = Normalizer.normalize(sentence, Form.NFC);
			if (testSentence(sentence, score)) {
				sentCnt++;
			}

			else {
				bw.write("\n-----Sent " + (sentID) + "-----\n" + sentence + "\n" + segment(sentence) + "\n\n");
				bw.flush();
			}
			sentID++;
		}

		bw.close();

		F1Score result = new F1Score(score.getN1(), score.getN2(), score.getN3());

		double pre = result.getPrecision();
		double rec = result.getRecall();
		double f_measure = result.getF1Score();

		System.out.println("\n" + "Number of words recognized by the system:\t\t\t\tN1 = " + score.getN1()
				+ "\nNumber of words in reality appearing in the corpus:\t\tN2 = " + score.getN2()
				+ "\nNumber of words that are correctly recognized by the system:\tN3 = " + score.getN3() + "\n");
		System.out.println("Precision\t\tP = N3/N1\t\t=\t" + pre + "%");
		System.out.println("Recall\t\tR = N3/N2\t\t=\t" + rec + "%");
		System.out.println("\nF-Measure\t\tF = (2*P*R)/(P+R)\t=\t" + f_measure + "%\n");

		System.out.println("\nNumber of sentences:\t" + sentences.size());
		System.out.println("Sentences right:\t\t" + sentCnt);
		System.out
				.println("\nSentences right accuracy:\t" + (double) sentCnt / (double) sentences.size() * 100.0 + "%");

		System.out.println("\nLogged wrong predictions to " + logName);

		return result;
	}

	/**
	 * @param sentence
	 *            A word-segmented sentence
	 * @return Whether the system can produce a right segmented sentence or not.
	 */
	private boolean testSentence(String sentence, ScoreKeeper score) {
		boolean sentCheck = true;

		ArrayList<SyllabelFeature> segmentList = new ArrayList<>();
		ExtractedFeatures extractedFeatures = fe.extract(sentence, Configure.TEST);

		List<SyllabelFeature> sylList = extractedFeatures.getSylList();
		List<SegmentFeature> segmentFeatures = extractedFeatures.getSegmentList();

		if (segmentFeatures.isEmpty()) {

			if (sylList.size() == 2 * Configure.WINDOW_LENGTH + 1) { // Sentence has only one token
				score.incrementN1();
				score.incrementN2();
				score.incrementN3();

				return true;

			} else // Sentence is empty
				return true;
		}

		for (int i = Configure.WINDOW_LENGTH; i < sylList.size() - Configure.WINDOW_LENGTH; i++) {
			segmentList.add(sylList.get(i));
		}

		sylList.clear();

		int size = segmentList.size() - 1;

		double[] reality = new double[size];

		double[] predictions = new double[size];

		double[] confidences = new double[size];

		for (int i = 0; i < size; i++) {
			reality[i] = segmentList.get(i).getLabel();
			confidences[i] = Double.MIN_VALUE;
		}

		// Convert features to FeatureNode structure of LIBLINEAR
		Problem problem = setProblem(extractedFeatures.getNumSamples(), extractedFeatures.getSegmentList());

		// Processing the prediction
		process(segmentList, problem, predictions, confidences, size, Configure.TEST);

		// Get the comparision
		boolean previousSpaceMatch = true;

		for (int i = 0; i < size; i++) {
			if (reality[i] == Configure.SPACE) {
				score.incrementN2();
			}

			if (predictions[i] == Configure.SPACE) {
				score.incrementN1();
				if (reality[i] == Configure.SPACE) {
					if (previousSpaceMatch) {
						score.incrementN3();
					}
					previousSpaceMatch = true;
				}
			}

			if (predictions[i] != reality[i]) {
				sentCheck = false;
				previousSpaceMatch = false;
			}
		}
		// The last word of sentence
		score.incrementN1();
		score.incrementN2();
		if (previousSpaceMatch) {
			score.incrementN3();
		}

		return sentCheck;

	}

	/**
	 * @param sentence
	 *            A tokenized sentence
	 * @return The word-segmented sentence
	 */
	public String segment(String sentence) {
		ArrayList<SyllabelFeature> segmentList = new ArrayList<>();
		ExtractedFeatures extractedFeatures = fe.extract(sentence, Configure.TEST);
		List<SyllabelFeature> sylList = extractedFeatures.getSylList();

		// No feature set returned
		if (extractedFeatures.getSegmentList().isEmpty()) {
			if (sylList.size() == 2 * Configure.WINDOW_LENGTH + 1) { // Sentence has only one token
				return sylList.get(Configure.WINDOW_LENGTH).getSyllabel();

			} else // Sentence is empty
				return "";
		}

		for (int i = Configure.WINDOW_LENGTH; i < sylList.size() - Configure.WINDOW_LENGTH; i++) {
			segmentList.add(sylList.get(i));
		}

		int size = segmentList.size() - 1;

		double[] predictions = new double[size];

		double[] confidences = new double[size];

		for (int i = 0; i < size; i++) {
			confidences[i] = Double.MIN_VALUE;
		}

		// Convert features to FeatureNode structure of LIBLINEAR
		Problem problem = setProblem(extractedFeatures.getNumSamples(), extractedFeatures.getSegmentList());

		// Processing the prediction
		process(segmentList, problem, predictions, confidences, size, Configure.PREDICT);

		// Get the result
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < size; i++) {
			sb.append(segmentList.get(i).getSyllabel());

			if (predictions[i] == Configure.SPACE)
				sb.append(StringConst.SPACE);
			else
				sb.append(StringConst.UNDERSCORE);
		}
		sb.append(segmentList.get(size).getSyllabel());

		return sb.toString().trim();
	}


	/**
	 * @param sentence
	 *            A tokenized sentence
	 * @return The word-segmented sentence
	 */
	public List<Pair<String, Integer>> segmentTokenized(List<String> sentence) {
		ArrayList<SyllabelFeature> segmentList = new ArrayList<>();
		ExtractedFeatures extractedFeatures = fe.extractTokenized(sentence, Configure.TEST);
		List<SyllabelFeature> sylList = extractedFeatures.getSylList();
		List<SegmentFeature> segmentFeatures = extractedFeatures.getSegmentList();

		List<Pair<String, Integer>> result = new ArrayList<>();

		if (segmentFeatures.isEmpty()) {

			if (sylList.size() == 2 * Configure.WINDOW_LENGTH + 1) { // Sentence has only one token
				result.add(new Pair<>(sylList.get(Configure.WINDOW_LENGTH).getSyllabel(), 1));
				return result;

			} else // Sentence is empty
				return result;
		}

		for (int i = Configure.WINDOW_LENGTH; i < sylList.size() - Configure.WINDOW_LENGTH; i++) {
			segmentList.add(sylList.get(i));
		}

		sylList.clear();

		int size = segmentList.size() - 1;

		double[] predictions = new double[size];

		double[] confidences = new double[size];

		for (int i = 0; i < size; i++) {
			confidences[i] = Double.MIN_VALUE;
		}

		// Convert features to FeatureNode structure of LIBLINEAR
		Problem problem = setProblem(extractedFeatures.getNumSamples(), extractedFeatures.getSegmentList());

		// Processing the prediction
		process(segmentList, problem, predictions, confidences, size, Configure.PREDICT);

		// Get the result
		boolean newWord = true;
		for (int i = 0; i < size; i++) {
			String nextSyl = segmentList.get(i).getSyllabel();
			if (newWord) {
				result.add(new Pair<>(nextSyl, 1));
			} else {
				int idx = result.size() - 1;
				Pair<String, Integer> prev = result.get(idx);
				prev.set(prev.o1 + " " + nextSyl, prev.o2 + 1);
				result.set(idx, prev);
			}
			if (predictions[i] != Configure.SPACE)
				newWord = false;
			else
				newWord = true;
		}
		result.add(new Pair<>(segmentList.get(size).getSyllabel(), 1));

		return result;
	}

	private void process(List<SyllabelFeature> segmentList, Problem problem, double[] predictions, double[] confidences, int size, int mode) {
		// Longest matching for over2-syllable words
		longestMatching(segmentList, predictions, size, mode);

		// Logistic regression
		logisticRegression(problem, predictions, confidences, size, mode);

		// Other post processing
		postProcessing(segmentList, predictions, confidences, size, mode);
	}

	// Longest matching for over-2-syllable words
	private void longestMatching(List<SyllabelFeature> segmentList, double[] predictions, int size, int mode) {
		for (int i = 0; i < size; i++) {
			if (segmentList.get(i).getType() == SyllableType.OTHER)
				continue;
			for (int n = 6; n >= 2; n--) {
				StringBuilder sb = new StringBuilder();
				boolean hasUpper = false;
				boolean hasLower = false;
				int j = i;
				for (j = i; j < i + n; j++) {

					if (j >= segmentList.size() || segmentList.get(j).getType() == SyllableType.OTHER) {
						break;
					}

					if (mode != Configure.TEST) {
						if (segmentList.get(j).getType() == SyllableType.UPPER
								|| segmentList.get(j).getType() == SyllableType.ALLUPPER) {
							hasUpper = true;
						}
					}

					if (segmentList.get(j).getType() == SyllableType.LOWER) {
						hasLower = true;
					}

					sb.append(" " + segmentList.get(j).getSyllabel());
				}

				if (j == i + n) {
					String word = sb.toString();

					if (n > 2 && hasLower && dictionary.inVNDict(word)) {

						for (int k = i; k < j - 1; k++) {
							predictions[k] = Configure.UNDERSCORE;
						}

						i = j - 1;
						break;
					}
					// if (mode != Configure.TEST) {
					if (hasUpper && rareNames.isRareName(word)) {

						for (int k = i; k < j - 1; k++) {
							predictions[k] = Configure.UNDERSCORE;
						}

						i = j - 1;
						break;
					}
					// }
				}
			}
		}

		// ruleForProperName(predictions, size, mode);
	}

	@SuppressWarnings("unused")
	private void ruleForProperName(List<SyllabelFeature> segmentList, double[] predictions, int size, int mode) {
		double[] temp = new double[size];
		for (int i = 0; i < size; i++) {
			temp[i] = predictions[i];
		}
		for (int i = 0; i < size; i++) {
			if (temp[i] != Configure.UNDERSCORE) {
				if ((i == 0 || temp[i - 1] != Configure.UNDERSCORE)
						&& (i == size - 1 || temp[i + 1] != Configure.UNDERSCORE)) {

					// Dictionary
					if (segmentList.get(i).getType() == SyllableType.UPPER
							&& segmentList.get(i + 1).getType() == SyllableType.UPPER) {
						predictions[i] = Configure.UNDERSCORE;
						continue;
					}
				}
			}
		}
	}

	private void logisticRegression(Problem problem, double[] predictions, double[] confidences, int size, int mode) {
		double[] temp = new double[size];
		for (int i = 0; i < size; i++) {
			temp[i] = predictions[i];
		}
		for (int i = 0; i < size; i++) {
			if (temp[i] != Configure.UNDERSCORE) {
				if ((i == 0 || temp[i - 1] != Configure.UNDERSCORE)
						&& (i == size - 1 || temp[i + 1] != Configure.UNDERSCORE)) {
					// Machine Learning
					predictions[i] = predict(confidences, problem.x[i], i, mode);
				}
			}
		}
	}

	// Logistic regression classifier
	private double predict(double[] confidences, Feature[] featSet, int sampleNo, int mode) {

		double[] dec_values = new double[model.getNrClass()];
		double result = Linear.predict(model, featSet, dec_values);

		confidences[sampleNo] = dec_values[0];

		return result;
	}

	// Post-processing
	private void postProcessing(List<SyllabelFeature> segmentList, double[] predictions, double[] confidences, int size, int mode) {
		if (size < 2) {
			return;
		}

		for (int i = 0; i < size - 1; i++) {
			double sigm = sigmoid(confidences[i]);

			SyllabelFeature preSyl = (i > 0) ? segmentList.get(i - 1) : null;
			SyllabelFeature thisSyl = segmentList.get(i);
			SyllabelFeature nextSyl = segmentList.get(i + 1);

			// Xu ly tu co 2 am tiet confidence thap
			if ((i == 0 || predictions[i - 1] == Configure.SPACE) && predictions[i + 1] == Configure.SPACE) {
				// confidence thap
				if (Math.abs(sigm - 0.5) < r) {
					String thisOne = thisSyl.getSyllabel();
					String nextOne = nextSyl.getSyllabel();

					// LOWER cases
					if ((preSyl == null && nextSyl.getType() == SyllableType.LOWER)
							|| ((thisSyl.getType() == SyllableType.LOWER || thisSyl.getType() == SyllableType.UPPER)
									&& (nextSyl.getType() == SyllableType.LOWER))) {

						String word1 = thisOne + " " + nextOne;

						if (dictionary.inVNDict(word1)) {
							predictions[i] = Configure.UNDERSCORE;
						}

						if (!(dictionary.inVNDict(word1))) {
							predictions[i] = Configure.SPACE;
						}
					}
				}
				continue;
			}

			// xu ly nhap nhang tu co 3 am tiet ko co trong tu dien
			if (predictions[i] == Configure.UNDERSCORE) {
				if ((i == 0 || predictions[i - 1] == Configure.SPACE) && predictions[i + 1] == Configure.UNDERSCORE
						&& (i == size - 2 || predictions[i + 2] == Configure.SPACE)) {

					// check dieu kien ton tai it nhat 1 lower
					int j = i;
					boolean flag = false;

					for (j = i; j < i + 3; j++) {

						if (j == 0 && (segmentList.get(j).getType() == SyllableType.LOWER
								|| segmentList.get(j).getType() == SyllableType.UPPER)) {
							continue;
						}

						if (segmentList.get(j).getType() == SyllableType.LOWER) {
							flag = true;
						}

						if (!(segmentList.get(j).getType() == SyllableType.LOWER
								|| segmentList.get(j).getType() == SyllableType.UPPER)) {
							break;
						}
					}

					// thoa man dieu kien ton tai it nhat 1 lower syllable
					if (j == i + 3 && flag) {
						String word = segmentList.get(i).getSyllabel() + " " + segmentList.get(i + 1).getSyllabel()
								+ " " + segmentList.get(i + 2).getSyllabel();
						// khong co trong tu dien
						if (!dictionary.inVNDict(word) && !rareNames.isRareName(word)) {
							String leftWord = segmentList.get(i).getSyllabel() + " "
									+ segmentList.get(i + 1).getSyllabel();
							String rightWord = segmentList.get(i + 1).getSyllabel() + " "
									+ segmentList.get(i + 2).getSyllabel();

							// s1s2 in dict, s2s3 not in dict
							if (dictionary.inVNDict(leftWord) && !dictionary.inVNDict(rightWord)) {
								predictions[i + 1] = Configure.SPACE;
							}

							// s1s2 not in dict, s2s3 in dict
							if (dictionary.inVNDict(rightWord) && !dictionary.inVNDict(leftWord)) {
								predictions[i] = Configure.SPACE;
							}

							// both in dict
							if (dictionary.inVNDict(rightWord) && dictionary.inVNDict(leftWord)) {
								if (confidences[i] * confidences[i + 1] > 0) {
									if (Math.abs(confidences[i]) < Math.abs(confidences[i + 1])) {
										predictions[i] = Configure.SPACE;
									} else {
										predictions[i + 1] = Configure.SPACE;
									}
								}
							}

							// none of them in dict
							if (!dictionary.inVNDict(rightWord) && !dictionary.inVNDict(leftWord)) {

								if (segmentList.get(i).getType() == SyllableType.LOWER
										|| segmentList.get(i + 1).getType() == SyllableType.LOWER) {
									predictions[i] = Configure.SPACE;
								}

								if (segmentList.get(i + 2).getType() == SyllableType.LOWER
										|| segmentList.get(i + 1).getType() == SyllableType.LOWER) {
									predictions[i + 1] = Configure.SPACE;
								}
							}

						}

						i = i + 2;
					}
				}
			}
		}
	}

	private void saveMap() {
		String mapFile = pathToSave + File.separator + "features";
		fe.saveMap(mapFile);
	}

	private void saveModel(Model model) {
		File modelFile = new File(pathToSave + File.separator + "model");

		try {
			model.save(modelFile);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	private void load(String path) throws ClassNotFoundException, IOException {
		if (path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}
		String modelPath = path + File.separator + "model";
		String featMapPath = path + File.separator + "features";

		File modelFile = new File(modelPath);

		model = Model.load(modelFile);
		fe = new FeatureExtractor(featMapPath);
		pathToSave = path;
		n = fe.getFeatureMap().getSize();
	}

	private double sigmoid(double d) {
		return 1 / (1 + Math.exp(-d));
	}

	public void setR(double r) {
		if (r < 0 || r > 0.5) {
			return;
		}
		SegmentationSystem.r = r;
	}
}
