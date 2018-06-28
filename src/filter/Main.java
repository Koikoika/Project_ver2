package filter;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
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

//出力画像を作成するためのメソッド
   public static void createGimg(String filename,int number) throws FileNotFoundException {
	   Scanner scanner = new Scanner(new File("groundtruth_rect.txt"));// 座標の書いてあるテキストフィルタを読み込む
	   int x , y, w, h,count;
	   Mat dst1 = Mat.zeros(width, height, CvType.CV_64FC1);// 0で初期化
	   count = 1;
	   
	   while(count<number+1) {
		   
	    x = 0;
		y = 0;
		w = 0;
		h = 0;
		
	    x = Integer.parseInt(scanner.next());
		//System.out.println(x);
		y = Integer.parseInt(scanner.next());
		//System.out.println(y);
		w = Integer.parseInt(scanner.next());
		if(w%2==0) w = w+1;
		h = Integer.parseInt(scanner.next());
		if(h%2==0) h = h+1;

		double[] rgb = { 255, 255, 255 };// 読み込んだ座標のみ白にする
		dst1.put(y+h/2, x+w/2, rgb);

		Imgproc.GaussianBlur(dst1, dst1, new Size(w,h), 2, 2);
		
		String outname = "img_G/" + String.valueOf(count) + ".jpg";// 出力ファイル
		Imgcodecs.imwrite(outname, dst1);
		
		dst1 = Mat.zeros(width, height, CvType.CV_64FC1);// 0で初期化	
		count++;
	   }
   }
   
   //各画素に定数をかけるためのメソッド
   public static void mult(Mat src,double number,Mat dft_ans) {
	   double[] data = new double[2];
	   for (int i = 0; i <width; i++) {
			for (int j = 0; j < height;j++) {
				data = src.get(i,j);
				//System.out.println(data[0]);
				data[0] = data[0]*number;
				data[1] = data[1]*number;
				dft_ans.put(i, j, data);
				}
			}
		}
	
   
	public static void main(String[] args) throws IOException {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);// ライブラリーを読み込む
		Mat Gstar = Mat.zeros(width, height, CvType.CV_64FC1);// 0で初期化
		Mat result = Mat.zeros(width, height, CvType.CV_64FC2);// 0で初期化
		Mat result1 = Mat.zeros(width, height, CvType.CV_64FC2);// 0で初期化
		Mat result2 = Mat.zeros(width, height, CvType.CV_64FC2);// 0で初期化
		Mat Fresult =  Mat.zeros(width, height, CvType.CV_64FC2);// 0で初期化
		List<Mat> planes = new ArrayList<Mat>();
		
		int count = 1;
		String filename,outname;
		
		//createGimg("/Users/Karin.T/Documents/3pro/Girl/groundtruth_rect.txt",500);//読み込む座標のテキストと入力画像の枚数を引数に
		
		while(count < 11) {//フィルタの初期状態を作成
		// 出力画像（黒い画像に白い点が1点）の作成
		//1度作成した場合はコメントアウト
			
			outname = "img_G/" + String.valueOf(count) + ".jpg";//出力画像
			
			// MOSSEフィルター作成
			filename= "img/" + String.valueOf(count) + ".jpg";// 入力画像
			
			OpenCVFFT2D fft = new OpenCVFFT2D(filename);   //読み込んだ入力画像のフーリエ変換
			Mat[] F = new Mat[1];
			F[0] = Mat.zeros(width, height, CvType.CV_64FC2);
			fft.getMagImg(F);
			
			OpenCVFFT2D fftg = new OpenCVFFT2D(outname);    //出力画像のフーリエ変換
			Mat[] G = new Mat[1];
			G[0] = Mat.zeros(width, height, CvType.CV_64FC2);
			fftg.getMagImg(G);//フーリエ変換後(Mat型)
			
			Core.mulSpectrums(F[0], F[0], Fresult,Core.DFT_COMPLEX_OUTPUT,true);// MOSSEフィルタ作成の式の分母(出力画像のフーリエ変換したMat、その複素共役、出力先、ROW、2番目を複素共役にするか否か)
			Core.add(result1, Fresult, result1);//分母 B（控える）
			Fresult =  Mat.zeros(width, height, CvType.CV_64FC2);// 0で初期化
			
			Core.mulSpectrums(G[0], F[0], Gstar,Core.DFT_COMPLEX_OUTPUT,true);//  MOSSEフィルタ作成の式の分子(出力画像のフーリエ変換したMat、その複素共役、出力先、ROW、2番目を複素共役にするか否か
			Core.add(result2, Gstar, result2);//分子 A（控える）
			Gstar =  Mat.zeros(width, height, CvType.CV_64FC2);
			
			
			F[0] = Mat.zeros(width, height, CvType.CV_64FC2);// 0で初期化
			G[0] = Mat.zeros(width, height, CvType.CV_64FC2);// 0で初期化
			
			count++;
		}
		
		Core.divide(result2, result1, result);
		String resultfilename = "result_Girl1.jpg";//出力ファイル
		
		Core.idft(result, result);
        Mat restoredImage = Mat.zeros(width, height, CvType.CV_64FC2);// 0で初期化
        Core.split(result, planes);
        Core.normalize(planes.get(0), restoredImage, 0,255, Core.NORM_MINMAX);
		
		// 画像の保存
		Imgcodecs.imwrite(resultfilename, restoredImage);
		
		//フィルタの更新
		Mat Ai = Mat.ones(width,height,CvType.CV_64FC2);
		Mat Bi = Mat.ones(width,height,CvType.CV_64FC2);
		Mat Aia = Mat.zeros(width,height,CvType.CV_64FC2);
		Mat Bia = Mat.zeros(width,height,CvType.CV_64FC2);
		Mat result_filter =  Mat.zeros(width,height,CvType.CV_64FC2);
		Mat resultImage_filter =  Mat.zeros(width,height,CvType.CV_64FC2);
		
		
		while(count<501) {
			List<Mat> planes2 = new ArrayList<Mat>();
			outname = "img_G/" + String.valueOf(count) + ".jpg";//出力画像(黒い画像)	
			
			if(count>300)  filename= "/0" + String.valueOf(count) + ".jpg";// 入力画像(写真)
			else filename= "" + String.valueOf(count) + ".jpg";// 入力画像
			
			OpenCVFFT2D fft = new OpenCVFFT2D(filename);   //読み込んだ入力画像のフーリエ変換
			Mat[] F = new Mat[1];
			F[0] = Mat.zeros(width, height, CvType.CV_64FC2);
			fft.getMagImg(F);
			
			OpenCVFFT2D fftg = new OpenCVFFT2D(outname);    //出力画像のフーリエ変換
			Mat[] G = new Mat[1];
			G[0] = Mat.zeros(width, height, CvType.CV_64FC2);
			fftg.getMagImg(G);//フーリエ変換後(Mat型)
			
			// MOSSEフィルタ作成の式の分母(出力画像のフーリエ変換したMat、その複素共役、出力先、ROW、2番目を複素共役にするか否か)
			Core.mulSpectrums(F[0], F[0], Fresult,Core.DFT_COMPLEX_OUTPUT,true);//分母 B
			
			
			Core.mulSpectrums(G[0], F[0], Gstar,Core.DFT_COMPLEX_OUTPUT,true);//  MOSSEフィルタ作成の式の分子(出力画像のフーリエ変換したMat、その複素共役、出力先、ROW、2番目を複素共役にするか否か
			
			mult(Gstar,0.125,Gstar);
			mult(Fresult,0.125,Fresult);
			
			if(count==11) {
				mult(result2,(1-0.125),result2);
				mult(result1,(1-0.125),result2);
				Core.add(Gstar,result2,Aia);//分子
				Core.add(Fresult,result1,Bia);//分母
			} else {
				mult(Ai,(1-0.125),Ai);
				mult(Bi,(1-0.125),Bi);
			    Core.add(Gstar,Ai,Aia);//分子
				Core.add(Fresult,Bi,Bia);//分母	
			}
			
			Core.divide(Bia, Aia, result_filter);
			resultfilename = "result_Girl"+String.valueOf(count)+".jpg";
			
			Core.idft(result_filter, result_filter);
	        Core.split(result_filter, planes2);
	        Core.normalize(planes2.get(0), resultImage_filter, 0,255, Core.NORM_MINMAX);
			
			// 画像の保存
			Imgcodecs.imwrite(resultfilename, resultImage_filter);
			
			F[0] = Mat.zeros(width, height, CvType.CV_64FC2);// 0で初期化
			G[0] = Mat.zeros(width, height, CvType.CV_64FC2);// 0で初期化
			Fresult =  Mat.ones(width, height, CvType.CV_64FC2);// 0で初期化
			Gstar =  Mat.ones(width, height, CvType.CV_64FC2);
			Ai = Mat.ones(width,height,CvType.CV_64FC2);
			Bi = Mat.ones(width,height,CvType.CV_64FC2);
			
			Ai = Aia.clone();
			Bi = Bia.clone();
			
			Aia = Mat.zeros(width,height,CvType.CV_64FC2);
			Bia = Mat.zeros(width,height,CvType.CV_64FC2);
			
			count++;
			
		}
		
	   /*
		double[] data = new double[2];
		
		//出力したresultの複素共役を求める
		Core.split(result, planes);
		for(int i=0; i<width;i++) {
			for(int j=0;j<height;j++) {
				data = planes.get(1).get(i,j);
		        planes.get(1).put(i,j,data[0]*(-1));
			}
		}
		
		Core.merge(planes,result);*/
		
		//出力されたフィルタの逆フーリエ変換
		
		System.out.println(String.format("done."));
	}
}
