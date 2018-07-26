package filter;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//import filter.MachineLearning2;
//import filter.NearestNeighbor;

public class NearestNeighbor2 {

	List<Map.Entry<Integer, Integer[]>> patterns = 
            new ArrayList<Map.Entry<Integer, Integer[]>>();

	
	public void learn(int cls, ArrayList<Integer> data) {//最近傍法を行うためにサンプルを集める
		int size = data.size();
		Integer[] sample=data.toArray(new Integer[size]);
		patterns.add(new AbstractMap.SimpleEntry<Integer, Integer[]>(cls, sample));
    }

    public int trial(ArrayList<Integer> dsts) {//識別
        int cls = 0;
        int length=0;
        int size = dsts.size();
        Integer[] data=dsts.toArray(new Integer[size]);
        
        //一番近いパターンを求める
        double mindist = Double.POSITIVE_INFINITY;
        for (Map.Entry<Integer, Integer[]> entry : patterns) {
            Integer[] ss = entry.getValue();
            
            //登録した軌跡と解除する軌跡の配列が異なる場合、短い方の配列の長さに合わせる
            if (ss.length - data.length>0) {
                length=data.length;
            }else {
            	length=ss.length;
            }
            
            //データ間の距離を求める
            double dist = 0;
            for (int i = 0; i < length; ++i) {
                dist += (ss[i] - data[i]) * (ss[i] - data[i]);
            }
            
            if (mindist > dist) {
                mindist = dist;
                cls =entry.getKey(); 
            }
        }
        return cls;
    }
}
