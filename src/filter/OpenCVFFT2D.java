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

	/*public BufferedImage getSrcImg() throws IOException {// 元の画像を得る
		return this.convertMatToBufferedImage(img);
	}*/

	public Mat getMagImg() throws IOException {// フーリエ変換の結果を得る
		Mat[] ans;
		ans = getDFT(img);
		return ans[0];
	}

	/*public BufferedImage getMagImg2() throws IOException {// フーリエ変換で出来た画像を得る(BufferedImage型)
		Mat[] ans;
		ans = getDFT(img);
		return this.convertMatToBufferedImage(ans[0]);
	}*/

	public Mat get2MagImg() throws IOException {// 逆フーリエ変換で出来た画像を得る(Mat型)
		Mat[] ans;
		ans = getDFT(img);
		return ans[1];
	}

	/*public BufferedImage get2MagImg2() throws IOException {// 逆フーリエ変換で出来た画像を得る(BufferedImage型)
		Mat[] ans;
		ans = getDFT(img);
		return this.convertMatToBufferedImage(ans[1]);
	}

	/*public Mat getAnswer() throws IOException {
		Mat[] ans;
		ans = getDFT(img);
		return ans[0];
	}*/

	/*private BufferedImage convertMatToBufferedImage(Mat m)// 画像を出力できる形式に変換するメソッド
			throws IOException {
		MatOfByte byteMat = new MatOfByte();
		Imgcodecs.imencode(".jpg", m, byteMat);
		InputStream in = new ByteArrayInputStream(byteMat.toArray());
		return ImageIO.read(in);
	}*/

	private Mat[] getDFT(Mat singleChannelImage) {

		Mat dst[] = new Mat[3];
		//グレースケール変換
		Mat grayImage = Mat.zeros(singleChannelImage.size(), CvType.CV_64F);
		// 関数 cvtColor は，入力画像の色空間を別の色空間に変換します
		Imgproc.cvtColor(singleChannelImage, grayImage, Imgproc.COLOR_RGB2GRAY);//カラー画像からグレースケール画像へ
		grayImage.convertTo(grayImage, CvType.CV_64F);

		//正規化（やり方1）
		 Core.normalize(grayImage, grayImage, 0,1, Core.NORM_MINMAX);
		 /*double[] data = new double[2];
		 
		 正規化（やり方2）
		/*double max = -1;

		
		Size size = grayImage.size();
		
		
		for (int i = 0; i <size.width; i++) {
			for (int j = 0; j < size.height;j++) {
				data = grayImage.get(i, j);
				//System.out.println(data[0]);
				//System.out.println(max);
				if (max < data[0]) {
					max = data[0];
				}
			}
		}
		//System.out.println(max);
		//System.out.println(data[0]/max);

		//System.out.println(grayImage.type());
		
		for (int i = 0; i < size.width; i++) {
			for (int j = 0; j < size.height; j++) {
				data = grayImage.get(i, j);
				//double data2[] = {data[0]/max,0,0};
				grayImage.put(i,j,data[0]/max);
			}
		}*/
		 
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
		
		/*Core.split(complexI2, planes);// マルチチャンネルの配列を，複数のシングルチャンネルの配列に分割します．(マルチチャンネルの入力配列,出力配列)
		Mat mag = new Mat(planes.get(0).size(), planes.get(0).type());// 新たな画像を作成
		Core.magnitude(planes.get(0), planes.get(1), mag);

		Mat magI = mag;
		Mat magI2 = new Mat(magI.size(), magI.type());
		Mat magI3 = new Mat(magI.size(), magI.type());
		Mat magI4 = new Mat(magI.size(), magI.type());
		Mat magI5 = new Mat(magI.size(), magI.type());

		// Normalize.
		Core.add(magI, Mat.ones(padded.size(), CvType.CV_64F), magI2);// ones 行列要素を1で埋めて初期化します．
		Core.log(magI2, magI3);// 各配列要素の絶対値の自然対数を求めます．（入力配列、出力配列）

		// 交換
		Mat crop = new Mat(magI3, new Rect(0, 0, magI3.cols() & -2, magI3.rows() & -2));

		magI4 = crop.clone();

		int cx = magI4.cols() / 2;// 横
		int cy = magI4.rows() / 2;// 縦

		Rect q0Rect = new Rect(0, 0, cx, cy); // top left=(x, y), (width, height)
		Rect q1Rect = new Rect(cx, 0, cx, cy);
		Rect q2Rect = new Rect(0, cy, cx, cy);
		Rect q3Rect = new Rect(cx, cy, cx, cy);

		Mat q0 = new Mat(magI4, q0Rect); // Top-Left
		Mat q1 = new Mat(magI4, q1Rect); // Top-Right
		Mat q2 = new Mat(magI4, q2Rect); // Bottom-Left
		Mat q3 = new Mat(magI4, q3Rect); // Bottom-Right

		Mat tmp = new Mat();
		q0.copyTo(tmp);
		q3.copyTo(q0);
		tmp.copyTo(q3);

		q1.copyTo(tmp);
		q2.copyTo(q1);
		tmp.copyTo(q2);

		Core.normalize(magI4, magI5, 0, 255, Core.NORM_MINMAX);// 配列のノルム，またはその範囲を正規化します．

		// 変換
		Mat realResult = new Mat(magI5.size(), CvType.CV_8UC1);
		magI5.convertTo(realResult, CvType.CV_8UC1);
		dst[0] = realResult;*/

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

		// Show the Source image.
		/*JFrame orgWin = new JFrame();
		orgWin.getContentPane().add(new JLabel(new ImageIcon(fft.getSrcImg())));
		orgWin.setVisible(true);
		orgWin.pack();*/

		// Show the magnitude image.
		/*JFrame magWin = new JFrame();
		magWin.getContentPane().add(new JLabel(new ImageIcon(fft.getMagImg2())));
		magWin.setVisible(true);
		magWin.pack();
		String filename = "output";// 出力ファイル
		System.out.println(String.format("Write %s", filename));
		Imgcodecs.imwrite(filename, fft.getAnswer());*/

		// 逆フーリエ変換の結果の出力
		/*JFrame org2Win = new JFrame();
		org2Win.getContentPane().add(new JLabel(new ImageIcon(fft.get2MagImg2())));
		org2Win.setVisible(true);
		org2Win.pack();
		String filename2 = "output";// 出力ファイル
		System.out.println(String.format("Write %s", filename2));
		Imgcodecs.imwrite(filename2, fft.get2MagImg());*/

		System.out.println(String.format("done."));
	}
}
