package vn.edu.vnu.uet.nlp.segmenter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


import edu.emory.clir.clearnlp.collection.pair.Pair;
import vn.edu.vnu.uet.liblinear.Model;

import vn.edu.vnu.uet.nlp.tokenizer.StringConst;
import vn.edu.vnu.uet.nlp.tokenizer.Tokenizer;
import vn.edu.vnu.uet.nlp.utils.Logging;

/**
 * The main class provides APIs for word segmentation
 * 
 * @author tuanphong94
 */
public class UETSegmenter {
	private final static String defaultModels = "resources/segmenter/models";

	private SegmentationSystem machine = null;

	public UETSegmenter() {
		this(defaultModels);
	}

	public UETSegmenter(String modelpath) {
		if (machine == null) {
			Logging.LOG.info("Loading segmenter model.\n");
			try {
				machine = new SegmentationSystem(modelpath);
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
			}
		}
	}

	public UETSegmenter(Model segmenterModel, FeatureExtractor segmenterFeatureExtractor, Dictionary dictionary, RareNames rareNames) {
		if (machine == null) {
			Logging.LOG.info("Loading segmenter model.\n");
			try {
				machine = new SegmentationSystem(segmenterModel, segmenterFeatureExtractor, dictionary, rareNames);
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * @param str
	 *            A tokenized text
	 * @return Segmented text
	 */
	public String segmentTokenizedText(String str) {
		StringBuffer sb = new StringBuffer();

		List<String> tokens = new ArrayList<String>();
		List<String> sentences = new ArrayList<String>();

		tokens.addAll(Arrays.asList(str.split("\\s+")));
		sentences = Tokenizer.joinSentences(tokens);

		for (String sentence : sentences) {
			sb.append((machine.segment(sentence)));
			sb.append(StringConst.SPACE);
		}

		tokens.clear();
		sentences.clear();

		return sb.toString().trim();
	}

	/**
	 *
	 * @param tokens
     * @return segmented text -- each element of list has the token text and the number of tokens it's composed of
     */
	public List<Pair<String, Integer>> segmentTokenizedText(List<String> tokens) {
		List<Pair<String, Integer>> result = new ArrayList<>();
		result.addAll(machine.segmentTokenized(tokens));
		return  result;
	}
	/**
	 * @param str
	 *            A raw text
	 * @return Segmented text
	 */
	public String segment(String str) {
		StringBuffer sb = new StringBuffer();

		List<String> tokens = new ArrayList<String>();
		List<String> sentences = new ArrayList<String>();

		try {
			tokens = Tokenizer.tokenize(str);
			sentences = Tokenizer.joinSentences(tokens);
		} catch (IOException e) {
			e.printStackTrace();
		}

		for (String sentence : sentences) {
			sb.append((machine.segment(sentence)));
			sb.append(StringConst.SPACE);
		}

		tokens.clear();
		sentences.clear();

		return sb.toString().trim();
	}

	/**
	 * @param corpus
	 *            A raw text
	 * @return List of segmented sentences
	 */
	public List<String> segmentSentences(String corpus) {
		List<String> result = new ArrayList<String>();

		List<String> tokens = new ArrayList<String>();
		List<String> sentences = new ArrayList<String>();

		try {
			tokens = Tokenizer.tokenize(corpus);
			sentences = Tokenizer.joinSentences(tokens);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for (String sentence : sentences) {
			result.add(machine.segment(sentence));
		}

		tokens.clear();
		sentences.clear();

		return result;
	}

	/**
	 * Change threshold r for post-processing
	 * 
	 * @param r
	 *            threshold r
	 */
	public void setR(double r) {
		this.machine.setR(r);
	}

}
