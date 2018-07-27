package filter;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//import keyopen.LearningMachine;
//import keyopen.MachineLearning;
import filter.NearestNeighbor1;
import filter.LearningMachine;

public class NearestNeighbor1 implements LearningMachine {

	List<Map.Entry<Integer, double[]>> patterns = new ArrayList<Map.Entry<Integer, double[]>>();

	public void learn(int cls, double[] data) {// 最近傍法を行うためにサンプルを集める
		patterns.add(new AbstractMap.SimpleEntry<Integer, double[]>(cls, data));
	}

	public int trial(double[] data) {// 識別
		int cls = 0;
		// 一番近いパターンを求める
		double mindist = Double.POSITIVE_INFINITY;
		for (Map.Entry<Integer, double[]> entry : patterns) {
			double[] ss = entry.getValue();

			// 学習画像と判断画像の配列の長さが違わないかどうか
			if (ss.length != data.length) {
				System.out.println("へんなデータ");
				continue;
			}

			// データ間の距離を求める
			double dist = 0;
			for (int i = 0; i < ss.length; ++i) {
				dist += (ss[i] - data[i]) * (ss[i] - data[i]);
			}

			if (mindist > dist) {
				mindist = dist;
				cls = entry.getKey();
			}
		}
		return cls;
	}
}
