package filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class Filter {

	public static void main(String[] args) throws IOException {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		String input = "img.jpg";	// 入力画像
		
		OpenCVFFT2D fft = new OpenCVFFT2D(input);
		Mat F = fft.getMagImg();
        
	    String output = "result1.jpg";// フィルタの取得
	    Mat im = Imgcodecs.imread(output);
	    double[] data = new double[3];
	    
	    //フィルターをフーリエ変換する
	    OpenCVFFT2D fftg = new OpenCVFFT2D(output);
		Mat G = fftg.getMagImg();

		List<Mat> planes = new ArrayList<Mat>();
	    Core.mulSpectrums(F, G, G, 0);
	    
	    Core.idft(G, G);
        Mat restoredImage = new Mat();
        Core.split(G, planes);
        Core.normalize(planes.get(0), restoredImage, 0, 255, Core.NORM_MINMAX);
        
	    Imgcodecs.imwrite("Filter.png", restoredImage);			// 出力画像の保存
		System.out.println("Done!");
	}
}
