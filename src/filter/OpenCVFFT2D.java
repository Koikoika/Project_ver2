package filter;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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

public class OpenCVFFT2D {
	Mat img;

	public OpenCVFFT2D(String filename) throws IOException {
		img = Imgcodecs.imread(filename, Imgcodecs.CV_LOAD_IMAGE_COLOR);// 3チャンネルとして読み込む
		if (img == null) {
			throw new RuntimeException("Can't load image.");
		}
	}

	public Mat getMagImg() throws IOException {// フーリエ変換の結果を得る
		Mat[] ans;
		ans = getDFT(img);
		return ans[0];
	}


	public Mat get2MagImg() throws IOException {// 逆フーリエ変換で出来た画像を得る(Mat型)
		Mat[] ans;
		ans = getDFT(img);
		return ans[1];
	}
	
	private Mat[] getDFT(Mat singleChannelImage) {

		Mat dst[] = new Mat[3];
		//グレースケール変換
		Mat grayImage = Mat.zeros(singleChannelImage.size(), CvType.CV_64F);
		// 関数 cvtColor は，入力画像の色空間を別の色空間に変換します
		Imgproc.cvtColor(singleChannelImage, grayImage, Imgproc.COLOR_RGB2GRAY);//カラー画像からグレースケール画像へ
		grayImage.convertTo(grayImage, CvType.CV_64F);

		//正規化（やり方1）
		 Core.normalize(grayImage, grayImage, 0,1, Core.NORM_MINMAX);
		 /*double[] data = new double[2];*/
		 
		
		 
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
		dst[0] = complexI2;
		

		// 逆フーリエ変換
		Core.idft(complexI2, complexI2);
		Mat restoredImage = new Mat();
		Core.split(complexI2, planes);
		Core.normalize(planes.get(0), restoredImage, 0, 255, Core.NORM_MINMAX);
		dst[1] = restoredImage;

		return dst;
	}

	void shiftDFT(Mat src, Mat dst) {
		Mat tmp = new Mat(src.size(), src.type());
		;
		int cx = src.cols() / 2;
		int cy = src.rows() / 2;

		for (int i = 0; i <= cx; i += cx) {
			Mat qs = new Mat(src, new Rect(i ^ cx, 0, cx, cy));
			Mat qd = new Mat(dst, new Rect(i, cy, cx, cy));
			qs.copyTo(tmp);
			qd.copyTo(qs);
			tmp.copyTo(qd);
		}
	}

	public static void main(String[] args) throws Exception {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		File file = new File("input");// 入力画像
		System.out.println(String.format("Read %s.", file.getName()));
		OpenCVFFT2D fft = new OpenCVFFT2D(file.getAbsolutePath());
		
		System.out.println(String.format("done."));
	}
}
