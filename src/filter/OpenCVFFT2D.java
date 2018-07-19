package filter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class OpenCVFFT2D {
	Mat img;
	Mat dft_img;

	public OpenCVFFT2D(String filename) throws IOException {
		img = Imgcodecs.imread(filename, Imgcodecs.CV_LOAD_IMAGE_COLOR);// 3チャンネルとして読み込む
		if (img == null) {
			throw new RuntimeException("Can't load image.");
		}
	}

	public OpenCVFFT2D(Mat inputmat) throws IOException {
		img = inputmat.clone();
	}// Mat型によるinput

	public void getMagImg(Mat[] ans) throws IOException {// フーリエ変換の結果を得る
		getDFT(img, ans);
	}

	public void getIDFTImg(Mat[] ans) throws IOException {// 逆フーリエ変換で出来た画像を得る(Mat型)
		getIDFT(img, ans);
	}

	public void getDFT(Mat singleChannelImage, Mat[] dst) {
		Mat grayImage = Mat.zeros(singleChannelImage.size(), CvType.CV_64F);
		if (singleChannelImage.channels() > 1) {
			// グレースケール変換
			// 関数 cvtColor は，入力画像の色空間を別の色空間に変換します
			Imgproc.cvtColor(singleChannelImage, grayImage, Imgproc.COLOR_RGB2GRAY);// カラー画像からグレースケール画像へ
			grayImage.convertTo(grayImage, CvType.CV_64F);
		} else {
			grayImage = singleChannelImage.clone();
			grayImage.convertTo(grayImage, CvType.CV_64F);
		}
		// 正規化
		Core.normalize(grayImage, grayImage, 0, 1, Core.NORM_MINMAX);

		// DFT 変換のサイズを計算
		int m = Core.getOptimalDFTSize(grayImage.rows());
		int n = Core.getOptimalDFTSize(grayImage.cols());

		Mat padded = new Mat(new Size(n, m), grayImage.type());

		// 画像のまわりに境界を作成します．（入力画像、出力画像、以下で上下左右の各方向に，元の画像矩形から何ピクセル分の境界を作る必要があるかを指定）
		Core.copyMakeBorder(grayImage, padded, 0, m - singleChannelImage.rows(), 0, n - singleChannelImage.cols(),
				Core.BORDER_CONSTANT);

		// Make complex matrix.
		List<Mat> planes = new ArrayList<Mat>();// Mat型のArrayList 実部と虚部に分ける
		planes.add(padded);// グレイスケール画像と同じ大きさの新たな画像を追加
		planes.add(Mat.zeros(padded.size(), padded.type()));// 先ほど加えた画像と同じ大きさの全ての値が0である画像を追加
		Mat complexI = Mat.zeros(padded.size(), CvType.CV_64F);
		Mat complexI2 = Mat.zeros(padded.size(), CvType.CV_64F);
		Core.merge(planes, complexI);// 複数のシングルチャンネル配列からマルチチャンネル配列を作成．

		// フーリエ変換
		Core.dft(complexI, complexI2);
		dst[0] = complexI2.clone();

	}

	public void getIDFT(Mat DFTChannelImage, Mat[] dst) {
		List<Mat> planes = new ArrayList<Mat>();
		// 逆フーリエ変換
		Core.idft(DFTChannelImage, DFTChannelImage);
		Mat restoredImage = new Mat();
		Core.split(DFTChannelImage, planes);
		Core.normalize(planes.get(0), restoredImage, 0, 255, Core.NORM_MINMAX);
		dst[0] = restoredImage.clone();
	}

	public static void main(String[] args) throws Exception {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		File file = new File("input");// 入力画像
		System.out.println(String.format("Read %s.", file.getName()));

		System.out.println(String.format("done."));
	}
}
