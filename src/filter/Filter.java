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
		
       //入力画像をフーリエ変換したものを表示
		/*JFrame magWin = new JFrame();
        magWin.getContentPane().add(new JLabel(
                new ImageIcon(fft.getMagImg2())));
        magWin.setVisible(true);
        magWin.pack();*/
		
        
	    String output = "result1.jpg";// フィルタの取得
	    Mat im = Imgcodecs.imread(output);
	    double[] data = new double[3];
	    
	    //フィルターをフーリエ変換する
	    OpenCVFFT2D fftg = new OpenCVFFT2D(output);
		Mat G = fftg.getMagImg();

		
       /* magWin.getContentPane().add(new JLabel(
                new ImageIcon(fftg.getMagImg2())));
        magWin.setVisible(true);
        magWin.pack();*/
	    
		
		List<Mat> planes = new ArrayList<Mat>();
		//System.out.println(F.size());
		//System.out.println(G.size());
	    Core.mulSpectrums(F, G, G, 0);
	    Core.idft(G, G);
        Mat restoredImage = new Mat();
        Core.split(G, planes);
        Core.normalize(planes.get(0), restoredImage, 0, 255, Core.NORM_MINMAX);
        
        
        System.out.println(restoredImage.type());
        /*List<Mat> planes_s = new ArrayList<Mat>();//Mat型のArrayList 実部と虚部に分ける
        Core.split(G, planes_s);*/

	    Imgcodecs.imwrite("Filter.png", restoredImage);			// 出力画像の保存
		System.out.println("Done!");
	}
}
