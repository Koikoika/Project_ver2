package filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;


public class Filter {
	static int width = 96;// 画像サイズ 
	static int height = 128;
	
	 
	
	public static void main(String[] args) throws IOException {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		String input = "/Users/Karin.T/Documents/3pro/Girl/img/0500.jpg";	// 入力画像
		
		OpenCVFFT2D fft = new OpenCVFFT2D(input);
		Mat[] F = new Mat[1];
		F[0] = Mat.zeros(width, height, CvType.CV_64FC2);
		fft.getMagImg(F);//フーリエ変換後(Mat型)
        
	    String output = "/Users/Karin.T/Documents/3pro/Girl/result_Filter/result_Girl500.jpg";// フィルタの取得
	    double[] data = new double[3];
	    
	    //フィルターをフーリエ変換する
	    OpenCVFFT2D fftg = new OpenCVFFT2D(output);
		Mat[] G = new Mat[1];
		G[0] = Mat.zeros(width, height, CvType.CV_64FC2);
		fftg.getMagImg(G);//フーリエ変換後(Mat型)

		List<Mat> planes = new ArrayList<Mat>();
	    Core.mulSpectrums(F[0], G[0], G[0], 0);
	    
	    Core.idft(G[0], G[0]);
        Mat restoredImage = Mat.zeros(width, height, CvType.CV_64FC3);
        Core.split(G[0], planes);
        
        Core.normalize(planes.get(0), restoredImage, 0, 255, Core.NORM_MINMAX);
        
	    Imgcodecs.imwrite("/Users/Karin.T/Documents/3pro/Girl/Result/result500.jpg", restoredImage);			// 出力画像の保存
	    
        double max = -1;
        
        int[] place = new int[2];
        
		for (int i = 0; i <width; i++) {
			for (int j = 0; j < height;j++) {
				data =restoredImage.get(i, j);
				if (max < data[0]) {
					max = data[0];
					place[0] = j;
					place[1] = i;
				}
			}
		}
		System.out.println("x="+place[0]+"　y="+place[1]);
		
		System.out.println("Done!");
	}
}
