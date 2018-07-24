package filter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import filter.OpenCVFFT2D;

public class CreateFilter {
	static int width;// 画像サイズ y
	static int height;// 画像サイズx

	public CreateFilter(int img_width, int img_height) {
		width = img_width;
		height = img_height;
	}

	public void toFourier(Mat[] mat, Mat[] ans) throws IOException {
		OpenCVFFT2D fft = new OpenCVFFT2D(mat[0]); // 読み込んだ入力画像のフーリエ変換
		Mat[] F = new Mat[1];
		F[0] = Mat.zeros(width, height, CvType.CV_64FC2);
		fft.getMagImg(F);
		ans[0] = F[0].clone();
	}

	public void IDFT(Mat[] mat, Mat[] ans) throws IOException {
		OpenCVFFT2D fft = new OpenCVFFT2D(mat[0]);
		Mat[] G = new Mat[1];
		G[0] = Mat.zeros(width, height, CvType.CV_64FC3);
		fft.getIDFTImg(G);
		ans[0] = G[0].clone();
	}
	
	// 出力画像を作成するためのメソッド
	public void createGimg(Mat[] ans, int[] data) throws FileNotFoundException {
		Mat dst1 = Mat.zeros(width, height, CvType.CV_64FC1);// 0で初期化

		/*
		 * x = Integer.parseInt(scanner.next());data[0] //System.out.println(x); y =
		 * Integer.parseInt(scanner.next());data[1] //System.out.println(y); w =
		 * Integer.parseInt(scanner.next());data[2] if(w%2==0) w = w+1; h =
		 * Integer.parseInt(scanner.next());data[3] if(h%2==0) h = h+1;
		 */

		double[] rgb = { 255, 255, 255 };// 読み込んだ座標のみ白にする
		dst1.put(data[1], data[0], rgb);
		if (data[2] % 2 == 0)
			data[2] = data[2] + 1;
		if (data[3] % 2 == 0)
			data[3] = data[3] + 1;
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
	
	public void add(Mat dft_src,double number,Mat dft_dst) {
		double[] data = new double[2];
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				data = dft_src.get(i, j);
				// System.out.println(data[0]);
				data[0] = data[0] + number;
				data[1] = data[1] + number;
				dft_dst.put(i, j, data);
			}
		}
	}

	// フィルタの初期状態を作成
	// 引数 フーリエ変換した入力画像、フーリエ変換した出力画像、count、結果を格納する配列（分母、分子、初期フィルタ）
	public void createFilter(Mat[] Fourier_input_mat, Mat[] Fourier_output_mat, int tocount, Mat[] result)
			throws IOException {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);// ライブラリーを読み込む
		Mat Gstar = Mat.zeros(width, height, CvType.CV_64FC1);// 0で初期化
		Mat result1 = Mat.zeros(width, height, CvType.CV_64FC2);// 0で初期化
		Mat result1_new = Mat.zeros(width, height, CvType.CV_64FC2);// 0で初期化
		Mat result2 = Mat.zeros(width, height, CvType.CV_64FC2);// 0で初期化
		Mat Fresult = Mat.zeros(width, height, CvType.CV_64FC2);// 0で初期化

		for (int i = 0; i < tocount; i++) {
			Core.mulSpectrums(Fourier_input_mat[0], Fourier_input_mat[0], Fresult, Core.DFT_COMPLEX_OUTPUT, true);// MOSSEフィルタ作成の式の分母(出力画像のフーリエ変換したMat、その複素共役、出力先、ROW、2番目を複素共役にするか否か)
			
			Core.add(result1, Fresult, result1);// 分母 B（控える）
			Fresult = Mat.zeros(width, height, CvType.CV_64FC2);// 0で初期化

			Core.mulSpectrums(Fourier_output_mat[0], Fourier_input_mat[0], Gstar, Core.DFT_COMPLEX_OUTPUT, true);// MOSSEフィルタ作成の式の分子(出力画像のフーリエ変換したMat、その複素共役、出力先、ROW、2番目を複素共役にするか否か
			Core.add(result2, Gstar, result2);// 分子 A（控える）
			Gstar = Mat.zeros(width, height, CvType.CV_64FC2);
		}
		result[0] = result1.clone();// 分母
		result[1] = result2.clone();// 分子

		result[2] = Mat.zeros(width, height, CvType.CV_64FC2);// フィルタ
		
		add(result1,0.0000000001,result1_new);

		Core.divide(result2, result1_new, result[2]);
		//result[2].convertTo(result[2], CvType.CV_64FC2);

		// デバッグ用
		/*
		 * Mat output = Mat.zeros(width, height, CvType.CV_64FC2);
		 * 
		 * Core.mulSpectrums(Fourier_input_mat[0], result[2], output, 0);
		 * 
		 * List<Mat> planes = new ArrayList<Mat>();
		 * 
		 * Core.idft(output, output); Mat restoredImage = Mat.zeros(width, height,
		 * CvType.CV_64FC1);// 0で初期化 Core.split(output, planes);
		 * Core.normalize(planes.get(0), restoredImage, 0, 255, Core.NORM_MINMAX);
		 * 
		 * // 画像の保存 Imgcodecs.imwrite(
		 * "/Users/Karin.T/Documents/3pro/project_c/filter_output/debug_initial.jpg",
		 * restoredImage); System.out.println("done!");
		 */
	}

	// 前の分母、分子、フィルタが格納された配列 //フーリエ変換した入力画像//結果を格納する配列
	public void updatefilter(Mat[] result, Mat[] Fourier_input_file, Mat[] update_result) {// フィルタを引き継ぐ
		// フィルタの更新
		Mat Aia = Mat.zeros(width, height, CvType.CV_64FC2);
		Mat Bia = Mat.zeros(width, height, CvType.CV_64FC2);
		Mat Bia_new = Mat.zeros(width, height, CvType.CV_64FC2);
		Mat result_filter = Mat.zeros(width, height, CvType.CV_64FC2);
		Mat input_result = Mat.ones(width, height, CvType.CV_64FC2);
		Mat output_result = Mat.ones(width, height, CvType.CV_64FC2);

		Mat output_mat = Mat.zeros(width, height, CvType.CV_64FC2);
		// 出力画像を作成
		Core.mulSpectrums(Fourier_input_file[0], result[2], output_mat, 0);

		// デバッグ
		List<Mat> planes = new ArrayList<Mat>();
		Mat debug = Mat.zeros(width, height, CvType.CV_64FC2);

		debug = output_mat.clone();

		Core.idft(debug, debug);
		Mat restoredImage = Mat.zeros(width, height, CvType.CV_64FC1);// 0で初期化
		Core.split(debug, planes);
		Core.normalize(planes.get(0), restoredImage, 0, 255, Core.NORM_MINMAX);
		//渡されたフィルタを新たな入力画像にかけた時の結果
		Imgcodecs.imwrite("/Users/Karin.T/Documents/3pro/project_c/output/debug_new.jpg", restoredImage);
		//デバッグ終了
		
		
		// MOSSEフィルタ作成の式の分母(出力画像のフーリエ変換したMat、その複素共役、出力先、ROW、2番目を複素共役にするか否か)
		Core.mulSpectrums(Fourier_input_file[0], Fourier_input_file[0], input_result, Core.DFT_COMPLEX_OUTPUT, true);// 分母
																														// B
		// ここで定数を足す 0.0???? F・F*+e フィルタの安定性を高めるため
		
		Core.mulSpectrums(output_mat, Fourier_input_file[0], output_result, Core.DFT_COMPLEX_OUTPUT, true);// MOSSEフィルタ作成の式の分子(出力画像のフーリエ変換したMat、その複素共役、出力先、ROW、2番目を複素共役にするか否か

		mult(output_result, 0.125, output_result);//分子
		mult(input_result, 0.125, input_result);//分母

		mult(result[1], (1 - 0.125), result[1]);// 前の分子
		mult(result[0], (1 - 0.125), result[0]);// 前の分母
		
		Core.add(output_result, result[1], Aia);// 分子
		Core.add(input_result, result[0], Bia);// 分母

		add(Bia,0.0000000001,Bia_new);
		
		Core.divide(Aia, Bia_new, result_filter);

		for (int i = 0; i < result.length; i++) {
			update_result[i] = Mat.zeros(width, height, CvType.CV_64FC2);
		}

		// フィルタの保存
		update_result[2] = result_filter.clone();

		input_result = Mat.zeros(width, height, CvType.CV_64FC2);// 0で初期化
		output_result = Mat.zeros(width, height, CvType.CV_64FC2);// 0で初期化
		input_result = Mat.ones(width, height, CvType.CV_64FC2);// 0で初期化
		output_result = Mat.ones(width, height, CvType.CV_64FC2);

		update_result[1] = Aia.clone();// 分子の保存
		update_result[0] = Bia.clone();// 分母の保存

		Aia = Mat.zeros(width, height, CvType.CV_64FC2);
		Bia = Mat.zeros(width, height, CvType.CV_64FC2);

		// デバッグ用
		List<Mat> planes2 = new ArrayList<Mat>();
		Mat output = Mat.zeros(width, height, CvType.CV_64FC2);

		Core.mulSpectrums(Fourier_input_file[0], update_result[2], output, 0);

		Core.idft(output, output);
		restoredImage = Mat.zeros(width, height, CvType.CV_64FC1);// 0で初期化
		Core.split(output, planes2);
		Core.normalize(planes2.get(0), restoredImage, 0, 255, Core.NORM_MINMAX);

		// 
		Imgcodecs.imwrite("/Users/Karin.T/Documents/3pro/project_c/filter_output/debug_newoutput.jpg", restoredImage);
		System.out.println("done!");

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
