package filter;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class CreateFilter {
	static int width;// 画像サイズ y
	static int height;// 画像サイズx
	

	public CreateFilter(int img_width,int img_height) {
		width = img_width;
		height = img_height; 
	}

	public void toFourier(Mat[] mat,Mat[] ans) throws IOException {
		OpenCVFFT2D fft = new OpenCVFFT2D(mat[0]); // 読み込んだ入力画像のフーリエ変換
		Mat[] F = new Mat[1];
		F[0] = Mat.zeros(width, height, CvType.CV_64FC2);
		fft.getMagImg(F);
		ans[0] = F[0].clone();
	}
	
	public void IDFT(Mat[] mat,Mat[] ans) throws IOException {
		OpenCVFFT2D fft = new OpenCVFFT2D(mat[0]); 
		Mat[] G = new Mat[1];
		G[0] = Mat.zeros(width, height, CvType.CV_64FC3);
		fft.getIDFTImg(G);
		ans[0] = G[0].clone();
	}

	// 出力画像を作成するためのメソッド
	public  void createGimg(Mat[] ans, int[] data) throws FileNotFoundException {
		Mat dst1 = Mat.zeros(width, height, CvType.CV_64FC1);// 0で初期化

		/*
		 * x = Integer.parseInt(scanner.next());data[0] //System.out.println(x); y =
		 * Integer.parseInt(scanner.next());data[1] //System.out.println(y); w =
		 * Integer.parseInt(scanner.next());data[2] if(w%2==0) w = w+1; h =
		 * Integer.parseInt(scanner.next());data[3] if(h%2==0) h = h+1;
		 */

		double[] rgb = { 255, 255, 255 };// 読み込んだ座標のみ白にする
		dst1.put(data[1], data[0], rgb);
        if(data[2]%2==0) data[2] = data[2] +1;
        if(data[3]%2==0) data[3] = data[3] +1;
		Imgproc.GaussianBlur(dst1, dst1, new Size(data[2], data[3]), 2, 2);
        ans[0] = dst1;

		dst1 = Mat.zeros(width, height, CvType.CV_64FC1);// 0で初期化

	}

	// 各画素に定数をかけるためのメソッド
	public void mult(Mat src, double number, Mat dft_ans) {
		double[] data = new double[2];
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				data = src.get(i, j);
				// System.out.println(data[0]);
				data[0] = data[0] * number;
				data[1] = data[1] * number;
				dft_ans.put(i, j, data);
			}
		}
	}

	// フィルタの初期状態を作成
	public void createFilter(Mat[] Fourier_input_mat, Mat[] Fourier_output_mat, int tocount, Mat[] result)
			throws IOException {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);// ライブラリーを読み込む
		Mat Gstar = Mat.zeros(width, height, CvType.CV_64FC1);// 0で初期化
		Mat result1 = Mat.zeros(width, height, CvType.CV_64FC2);// 0で初期化
		Mat result2 = Mat.zeros(width, height, CvType.CV_64FC2);// 0で初期化
		Mat Fresult = Mat.zeros(width, height, CvType.CV_64FC2);// 0で初期化

		// createGimg("/Users/Karin.T/Documents/3pro/Girl/groundtruth_rect.txt",500);//読み込む座標のテキストと入力画像の枚数を引数に
		for (int i = 0; i < tocount; i++) {
			Core.mulSpectrums(Fourier_input_mat[i], Fourier_input_mat[i], Fresult, Core.DFT_COMPLEX_OUTPUT, true);// MOSSEフィルタ作成の式の分母(出力画像のフーリエ変換したMat、その複素共役、出力先、ROW、2番目を複素共役にするか否か)
			Core.add(result1, Fresult, result1);// 分母 B（控える）
			result[0] = result1.clone();
			Fresult = Mat.zeros(width, height, CvType.CV_64FC2);// 0で初期化

			Core.mulSpectrums(Fourier_output_mat[i], Fourier_input_mat[i], Gstar, Core.DFT_COMPLEX_OUTPUT, true);// MOSSEフィルタ作成の式の分子(出力画像のフーリエ変換したMat、その複素共役、出力先、ROW、2番目を複素共役にするか否か
			Core.add(result2, Gstar, result2);// 分子 A（控える）
			result[1] = result2.clone();
			Gstar = Mat.zeros(width, height, CvType.CV_64FC2);
		}
		Core.divide(result2, result1, result[2]);
	}

	// 分子 //分母
	public void updatefilter(Mat[] result, Mat[] Fourier_input_file,Mat[] update_result) {// フィルタを引き継ぐ
		// フィルタの更新
		Mat Aia = Mat.zeros(width, height, CvType.CV_64FC2);
		Mat Bia = Mat.zeros(width, height, CvType.CV_64FC2);
		Mat result_filter = Mat.zeros(width, height, CvType.CV_64FC2);
		Mat resultImage_filter = Mat.zeros(width, height, CvType.CV_64FC2);
		Mat input_result = Mat.ones(width, height, CvType.CV_64FC2);
		Mat output_result = Mat.ones(width, height, CvType.CV_64FC2);
		
		Mat output_mat = Mat.zeros(width, height, CvType.CV_64FC2);
		// if(count==11)
		Core.mulSpectrums(Fourier_input_file[0], result[2], output_mat, 0);
		// else Core.mulSpectrums(F[0], result_filter, G[0], 0);
		result_filter = Mat.zeros(width, height, CvType.CV_64FC2);

		// MOSSEフィルタ作成の式の分母(出力画像のフーリエ変換したMat、その複素共役、出力先、ROW、2番目を複素共役にするか否か)
		Core.mulSpectrums(Fourier_input_file[0], Fourier_input_file[0], input_result, Core.DFT_COMPLEX_OUTPUT, true);// 分母 B
		// ここで定数を足す 0.0???? F・F*+e フィルタの安定性を高めるため
		Core.mulSpectrums(output_mat, Fourier_input_file[0], output_result, Core.DFT_COMPLEX_OUTPUT, true);// MOSSEフィルタ作成の式の分子(出力画像のフーリエ変換したMat、その複素共役、出力先、ROW、2番目を複素共役にするか否か

		mult(output_result, 0.125, output_result);
		mult(input_result, 0.125, input_result);

		mult(result[1], (1 - 0.125), result[1]);// 分子
		mult(result[0], (1 - 0.125), result[0]);// 分母
		Core.add(output_result, result[1], Aia);// 分子
		Core.add(input_result, result[0], Bia);// 分母

		Core.divide(Aia, Bia, result_filter);

		update_result[2] = resultImage_filter;
		/*Core.idft(output_result, output_result);
		Core.split(output_result, planes2);
		Core.normalize(planes2.get(0), resultImage_filter, 0, 255, Core.NORM_MINMAX);
		update_result[2] = resultImage_filter;//フィルタ*/
		
		// 画像の保存
		//Imgcodecs.imwrite(resultfilename, resultImage_filter);

		input_result = Mat.zeros(width, height, CvType.CV_64FC2);// 0で初期化
		output_result = Mat.zeros(width, height, CvType.CV_64FC2);// 0で初期化
		input_result = Mat.ones(width, height, CvType.CV_64FC2);// 0で初期化
		output_result = Mat.ones(width, height, CvType.CV_64FC2);
		
		update_result[1] =  Aia.clone();//分子
		update_result[0] =  Bia.clone();//分母
		

		Aia = Mat.zeros(width, height, CvType.CV_64FC2);
		Bia = Mat.zeros(width, height, CvType.CV_64FC2);

	}

	/*
	 * double[] data = new double[2];
	 * 
	 * //出力したresultの複素共役を求める Core.split(result, planes); for(int i=0; i<width;i++) {
	 * for(int j=0;j<height;j++) { data = planes.get(1).get(i,j);
	 * planes.get(1).put(i,j,data[0]*(-1)); } }
	 * 
	 * Core.merge(planes,result);
	 */

	// 出力されたフィルタの逆フーリエ変換

}
