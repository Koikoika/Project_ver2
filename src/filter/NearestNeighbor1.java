package filter;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

//import keyopen.LearningMachine;
//import keyopen.MachineLearning;
import filter.NearestNeighbor1;
import filter.LearningMachine;


public class NearestNeighbor1 implements LearningMachine{

	List<Map.Entry<Integer, double[]>> patterns = 
            new ArrayList<Map.Entry<Integer, double[]>>();

   /* public static void main(String[] args) {
        new MachineLearning(new NearestNeighbor());
    }*/

    public void learn(int cls, double[] data) {//最近傍法を行うためにサンプルを集める
        patterns.add(new AbstractMap.SimpleEntry(cls, data));
    }

    public int trial(double[] data) {//識別
        int cls = 0;
        //一番近いパターンを求める
        double mindist = Double.POSITIVE_INFINITY;
        for (Map.Entry<Integer, double[]> entry : patterns) {
            double[] ss = entry.getValue();
            
            //System.out.println(ss.length);
            //System.out.println(data.length);
            
            //学習画像と判断画像の配列の長さが違わないかどうか
            if (ss.length != data.length) {
                System.out.println("へんなデータ");
                continue;
            }
            
            //データ間の距離を求める
            double dist = 0;
            for (int i = 0; i < ss.length; ++i) {
                dist += (ss[i] - data[i]) * (ss[i] - data[i]);
                //デバッグ
                System.out.println("データの距離を求めてる");
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
    /*
    //画像を読み込み配列を返す
    public static double[] Readimg(String img) { 
    	
    	System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    	// 画像の読み込み (第二引数を0にするとグレースケールで読み込み)
        Mat im = Imgcodecs.imread(img,0);	// 入力画像の取得
       
        int cols = im.cols();
        int rows = im.rows();
        int size = cols * rows;
        double[] data = new double[size];
        
        int y;
        int x;
        int count=0;
        
        //rows(行)の数分回す
        for(y=0;y<rows;y++) {
        	
        //cols(列)の数だけ読み込む
        for(x=0;x<cols;x++) {
        	double[] d=im.get(y, x);
        	data[count] = d[0];
        	
        	count++;
        	
        }
        }
        return data;

}*/
}
