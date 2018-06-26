package filter;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class Main{
	static int width =96;// 画像サイズ y
	static int height = 128;// 画像サイズx

	
	public static void main(String[] args) throws IOException {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);// ライブラリーを読み込む
		Mat dst1 = Mat.zeros(width, height, CvType.CV_64FC1);// 0で初期化
		Mat Gstar = Mat.zeros(width, height, CvType.CV_64FC1);// 0で初期化
		Mat result = Mat.zeros(width, height, CvType.CV_64FC2);// 0で初期化
		Mat result1 = Mat.zeros(width, height, CvType.CV_64FC2);// 0で初期化
		Mat result2 = Mat.zeros(width, height, CvType.CV_64FC2);// 0で初期化
		Mat Fresult =  Mat.zeros(width, height, CvType.CV_64FC2);// 0で初期化
		
		
		int count = 1;
		
		Scanner scanner = new Scanner(new File("/Users/Karin.T/Documents/3pro/Girl/groundtruth_rect.txt"));// 座標の書いてあるテキストフィルタを読み込む
		//画像の保存
		MatOfByte byteMat = new MatOfByte();
		Imgcodecs.imencode(".jpg", dst1, byteMat);
		InputStream in = new ByteArrayInputStream(byteMat.toArray());
		
		while(count < 11) {
		// 出力画像（黒い画像に白い点が1点）の作成
		//1度作成した場合はコメントアウト
		
		int x = 0, y = 0, w, h;
		
			x = Integer.parseInt(scanner.next());
			System.out.println(x);
			y = Integer.parseInt(scanner.next());
			System.out.println(y);
			w = Integer.parseInt(scanner.next());
			if(w%2==0) w = w+1;
			h = Integer.parseInt(scanner.next());
			if(h%2==0) h = h+1;

			double[] rgb = { 255, 255, 255 };// 読み込んだ座標のみ白にする
			dst1.put(y, x, rgb);

			Imgproc.GaussianBlur(dst1, dst1, new Size(w,h), 2, 2);

			String outname = "/Users/Karin.T/Documents/3pro/Girl/img_G/" + String.valueOf(count) + ".jpg";// 出力ファイル
			Imgcodecs.imwrite(outname, dst1);
			
			String filename ;
			// MOSSEフィルター作成
			if(count>300)  filename= "/Users/Karin.T/Documents/3pro/Girl/img/0" + String.valueOf(count) + ".jpg";// 入力画像
			else filename= "/Users/Karin.T/Documents/3pro/Girl/img/" + String.valueOf(count) + ".jpg";// 入力画像
			
			OpenCVFFT2D fft = new OpenCVFFT2D(filename);   //読み込んだ入力画像のフーリエ変換
			Mat[] F = new Mat[1];
			F[0] = Mat.zeros(width, height, CvType.CV_64FC2);
			fft.getMagImg(F);//フーリエ変換後(Mat型)
			//System.out.println(F[0].size());
			
			OpenCVFFT2D fftg = new OpenCVFFT2D(outname);//出力画像のフーリエ変換
			Mat[] G = new Mat[1];
			G[0] = Mat.zeros(width, height, CvType.CV_64FC2);
			fftg.getMagImg(G);//フーリエ変換後(Mat型)
			
			Core.mulSpectrums(F[0], F[0], Fresult,Core.DFT_COMPLEX_OUTPUT,true);// MOSSEフィルタ作成の式の分母(出力画像のフーリエ変換したMat、その複素共役、出力先、ROW、2番目を複素共役にするか否か)
			Core.add(result1, Fresult, result1);//分母 B（控える）
			Fresult =  Mat.zeros(width, height, CvType.CV_64FC2);// 0で初期化
			
			Core.mulSpectrums(G[0], F[0], Gstar,Core.DFT_COMPLEX_OUTPUT,true);//  MOSSEフィルタ作成の式の分子(出力画像のフーリエ変換したMat、その複素共役、出力先、ROW、2番目を複素共役にするか否か
			Core.add(result2, Gstar, result2);//分子 A（控える）
			Gstar =  Mat.zeros(width, height, CvType.CV_64FC2);
			
			dst1 = Mat.zeros(width, height, CvType.CV_64FC1);// 0で初期化
			F[0] = Mat.zeros(width, height, CvType.CV_64FC2);// 0で初期化
			G[0] = Mat.zeros(width, height, CvType.CV_64FC2);// 0で初期化
			
			count++;
		}
		
		
	    double[] data2 = new double[3];
	    data2 = result1.get(90, 90);//分母の虚数部は0になっているか確認
		System.out.println("実部：" + data2[0]);
		System.out.println("虚部：" + data2[1]);
		
		Core.divide(result2, result1, result);
		
		String resultfilename = "/Users/Karin.T/Documents/3pro/result_Girl1.jpg";//出力ファイル
		
		List<Mat> planes = new ArrayList<Mat>();

		double[] data = new double[2];
		
		//出力したresultの複素共役を求める
		/*Core.split(result, planes);
		for(int i=0; i<width;i++) {
			for(int j=0;j<height;j++) {
				data = planes.get(1).get(i,j);
		        planes.get(1).put(i,j,data[0]*(-1));
			}
		}
		
		Core.merge(planes,result);*/
		
		//出力されたフィルタの逆フーリエ変換
		Core.idft(result, result);
        Mat restoredImage = new Mat();
        Core.split(result, planes);
        Core.normalize(planes.get(0), restoredImage, 0,255, Core.NORM_MINMAX);
		
		// 画像の保存・表示
		Imgcodecs.imencode(".jpg", dst1, byteMat);
		Imgcodecs.imwrite(resultfilename, restoredImage);
		
		
		
		System.out.println(String.format("done."));
	}

}
