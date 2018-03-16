package vn.edu.vnu.uet.nlp.segmenter;

import java.util.List;

/**
 * Created by Eliana Vornov on 3/15/18.
 */
public class ExtractedFeatures {
    private List<SegmentFeature> segmentFeatures;
    private List<SyllabelFeature> sylList;

    public ExtractedFeatures(List<SegmentFeature> segmentList, List<SyllabelFeature> sylList) {
        this.segmentFeatures = segmentList;
        this.sylList = sylList;
    }

	public int getNumSamples() {
        return segmentFeatures.size();
	}

	public List<SegmentFeature> getSegmentList() {
		return this.segmentFeatures;
	}

    public List<SyllabelFeature> getSylList() {
        return this.sylList;
    }
}
