package filter;

import java.util.*;


public class NearestNeighbor implements LearningMachine{
	 List<Map.Entry<Integer, double[]>> patterns = 
	            new ArrayList<Map.Entry<Integer, double[]>>();

	    public static void main(String[] args) {
	        new MachineLearning("NN法", new NearestNeighbor());
	    }

	    public void learn(int cls, double[] data) {
	        patterns.add(new AbstractMap.SimpleEntry(cls, data));
	    }

	    public int trial(double[] data) {
	        int cls = 0;
	        //一番近いパターンを求める
	        double mindist = Double.POSITIVE_INFINITY;
	        for (Map.Entry<Integer, double[]> entry : patterns) {
	            double[] ss = entry.getValue();
	            if (ss.length != data.length) {
	                System.out.println("へんなデータ");
	                continue;
	            }
	            //データ間の距離を求める
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
