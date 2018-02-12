package vn.edu.vnu.uet.nlp.postagger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import edu.emory.clir.clearnlp.component.AbstractComponent;
import edu.emory.clir.clearnlp.component.configuration.DecodeConfiguration;
import edu.emory.clir.clearnlp.component.utils.GlobalLexica;
import edu.emory.clir.clearnlp.component.utils.NLPMode;
import edu.emory.clir.clearnlp.component.utils.NLPUtils;
import edu.emory.clir.clearnlp.dependency.DEPNode;
import edu.emory.clir.clearnlp.dependency.DEPTree;
import edu.emory.clir.clearnlp.util.IOUtils;
import edu.emory.clir.clearnlp.util.constant.StringConst;
import edu.emory.clir.clearnlp.util.lang.TLanguage;

/**
 * @author tuanphong94
 *
 */
public class UETTagger {

	private final static String defaultConfigFile = "resources/postagger/configure/config_decode_pos_vi.xml";

	private static DecodeConfiguration config = null;
	private final static NLPMode mode = NLPMode.pos;
	private static AbstractComponent[] components = null;
	private StringBuilder result;

	public UETTagger() {
		this(defaultConfigFile);
	}

	public UETTagger(String configFile) {
		GlobalLexica.init(IOUtils.createFileInputStream(configFile));
		config = new DecodeConfiguration(IOUtils.createFileInputStream(configFile));
		components = getComponents(config.getLanguage(), mode, config);
	}

	public UETTagger(InputStream in) throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		org.apache.commons.io.IOUtils.copy(in, outputStream);
		byte[] bytes = outputStream.toByteArray();
		ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);

		GlobalLexica.init(inputStream);
		inputStream.reset();
		config = new DecodeConfiguration(inputStream);

		components = getComponents(config.getLanguage(), mode, config);
	}

	/**
	 * @param input
	 *            A word-segmented text
	 * @return POS-tagged text
	 */
	public String tagString(String input) {
		String[] words = input.split("\\s+");
		List<String> sentence = Arrays.asList(words);

		result = new StringBuilder();

		DEPTree tree = new DEPTree(sentence);
		process(tree, result, mode, components);

		return result.toString().trim();

	}


	/**
	 * @param sentence
	 *            A tokenized/segmented sentence
	 * @return POS-tagged text
	 */
	public List<String> tagTokenizedString(List<String> sentence) {


		List<String> res = new ArrayList<>();

		DEPTree tree = new DEPTree(sentence);
		process(tree, res, mode, components);

		return res;

	}

	private static AbstractComponent[] getComponents(TLanguage language, NLPMode mode, DecodeConfiguration config) {
		List<AbstractComponent> list = new ArrayList<>();
		list.add(NLPUtils.getPOSTagger(language, config.getModelPath(NLPMode.pos)));

		return toReverseArray(list);
	}

	private static AbstractComponent[] toReverseArray(List<AbstractComponent> list) {
		AbstractComponent[] array = new AbstractComponent[list.size()];
		Collections.reverse(list);
		return list.toArray(array);
	}

	private void process(DEPTree tree, StringBuilder output, NLPMode mode, AbstractComponent[] components) {

		for (AbstractComponent component : components) {
			component.process(tree);
		}

		for (DEPNode node : tree) {
			output.append(node.getWordForm() + StringConst.FW_SLASH + node.getPOSTag() + StringConst.SPACE);
		}
	}

	private void process(DEPTree tree, List<String> output, NLPMode mode, AbstractComponent[] components) {

		for (AbstractComponent component : components) {
			component.process(tree);
		}

		for (DEPNode node : tree) {
			output.add(node.getPOSTag());
		}
	}


}
