package filter;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NearestNeighbor2 {

	List<Map.Entry<Integer, int[]>> patterns = 
            new ArrayList<Map.Entry<Integer, int[]>>();

	
	public void learn(int cls, int[] data) {//最近傍法を行うためにサンプルを集める
	
		patterns.add(new AbstractMap.SimpleEntry<Integer, int[]>(cls, data));
    }

    public int trial(int[] data) {//識別
        int cls = 0;
        int length=0;
        double dist = 0;
        
        //一番近いパターンを求める
        double mindist = Double.POSITIVE_INFINITY;
        for (Map.Entry<Integer, int[]> entry : patterns) {
            int[] ss = entry.getValue();
            
            length = ss.length;
            
            System.out.println(ss.length);
            System.out.println(data.length);
            
            
            //データ間の距離を求める
            dist = 0;
            for (int i = 0; i < length; ++i) {
                dist += (ss[i] - data[i]) * (ss[i] - data[i]);
             // デバッグ
				System.out.println("軌跡の距離を求めてる");
				System.out.println(ss[i]);
				System.out.println(data[i]);
            }
            
            if (mindist > dist) {
                mindist = dist;
                cls =entry.getKey(); 
            }
        }
        return cls;
    }
}
