package filter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.videoio.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgcodecs.Imgcodecs;

public class Movie extends JPanel {
	private static final long serialVersionUID = 1L;
	private BufferedImage image;
	static int width = 96;
	static int height = 128;

	// Create a constructor method
	public Movie() {
		super();
	}

	private BufferedImage getimage() {
		return image;
	}

	private void setimage(BufferedImage newimage) {
		image = newimage;
		return;
	}

	/**
	 * Converts/writes a Mat into a BufferedImage.
	 * 
	 * @param matrix
	 *            Mat of type CV_8UC3 or CV_8UC1
	 * @return BufferedImage of type TYPE_3BYTE_BGR or TYPE_BYTE_GRAY
	 */
	public static BufferedImage matToBufferedImage(Mat matrix) {
		int cols = matrix.cols();
		int rows = matrix.rows();
		int elemSize = (int) matrix.elemSize();
		byte[] data = new byte[cols * rows * elemSize];
		int type;
		matrix.get(0, 0, data);

		switch (matrix.channels()) {
		case 1:
			type = BufferedImage.TYPE_BYTE_GRAY;
			break;
		case 3:
			type = BufferedImage.TYPE_3BYTE_BGR;
			// bgr to rgb
			byte b;
			for (int i = 0; i < data.length; i = i + 3) {
				b = data[i];
				data[i] = data[i + 2];
				data[i + 2] = b;
			}
			break;
		default:
			return null;
		}

		BufferedImage image2 = new BufferedImage(cols, rows, type);
		image2.getRaster().setDataElements(0, 0, cols, rows, data);
		return image2;

	}

	public void paintComponent(Graphics g) {
		BufferedImage temp = getimage();
		if (temp != null) {
			g.drawImage(temp, 20, 20, temp.getWidth(), temp.getHeight(), this);
		}
	}

	// 各画素に定数をかけるためのメソッド
	public static void mult(Mat src, double number, Mat dft_ans) {
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

	public static int[] max(int width, int height, Mat img) {
		double max = -1;
		double[] data = new double[2];
		int[] place = new int[2];
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				data = img.get(i, j);
				if (max < data[0]) {
					max = data[0];
					place[0] = j;
					place[1] = i;
				}
			}
		}
		return place;
	}

	public static void main(String arg[]) throws IOException {
		// Load the native library.
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		JFrame frame = new JFrame("BasicPanel");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(500, 400);
		Movie panel = new Movie();
		frame.setContentPane(panel);
		frame.setVisible(true);
		Mat webcam_image = Mat.ones(width, height, CvType.CV_64F);
		BufferedImage temp;
		VideoCapture capture = new VideoCapture(0);

		Mat Ai = Mat.ones(width, height, CvType.CV_64FC2);
		Mat Bi = Mat.ones(width, height, CvType.CV_64FC2);
		Mat Aia = Mat.zeros(width, height, CvType.CV_64FC2);
		Mat Bia = Mat.zeros(width, height, CvType.CV_64FC2);
		Mat result_filter = Mat.zeros(width, height, CvType.CV_64FC2);
		Mat resultImage_filter = Mat.zeros(width, height, CvType.CV_64FC2);
		Mat Fresult = Mat.ones(width, height, CvType.CV_64FC2);// 0で初期化
		Mat Gstar = Mat.ones(width, height, CvType.CV_64FC2);
		Mat result1 = Mat.zeros(width, height, CvType.CV_64FC2);// 0で初期化
		Mat result2 = Mat.zeros(width, height, CvType.CV_64FC2);// 0で初期化
		Mat grayImage = Mat.zeros(width,height, CvType.CV_64F);
		
		Main filter = new Main();
		filter.createFilter();
		Mat[] ans = new Mat[1];
		ans[0] = Mat.zeros(width, height, CvType.CV_64FC2);// 0で初期化
		filter.getFilter(ans);
		Mat filter_img = Mat.ones(width, height, CvType.CV_64FC2);
		String resultfilename;

		int count = 0;
		List<Mat> planes2 = new ArrayList<Mat>();

		if (capture.isOpened()) {
			while (true) {

				capture.read(webcam_image);

				if (!webcam_image.empty()) {
					Imgproc.cvtColor(webcam_image,grayImage, Imgproc.COLOR_RGB2GRAY);//カラー画像からグレースケール画像へ
					grayImage.convertTo(grayImage, CvType.CV_64F);
					Core.normalize(grayImage, grayImage, 0,1, Core.NORM_MINMAX);
					int m = Core.getOptimalDFTSize(grayImage.rows());
					int n = Core.getOptimalDFTSize(grayImage.cols());
					
					Mat padded = new Mat(new Size(n, m), grayImage.type());

					// 画像のまわりに境界を作成します．（入力画像、出力画像、以下で上下左右の各方向に，元の画像矩形から何ピクセル分の境界を作る必要があるかを指定）
					Core.copyMakeBorder(grayImage, padded, 0, m - grayImage.rows(), 0, n - grayImage.cols(),
							Core.BORDER_CONSTANT);

					// Make complex matrix.
					List<Mat> planes = new ArrayList<Mat>();// Mat型のArrayList 実部と虚部に分ける
					planes.add(padded);// グレイスケール画像と同じ大きさの新たな画像を追加
					planes.add(Mat.zeros(padded.size(), padded.type()));// 先ほど加えた画像と同じ大きさの全ての値が0である画像を追加
					Mat complexI = Mat.zeros(padded.size(), CvType.CV_64F);
					Mat complexI2 = Mat.zeros(padded.size(), CvType.CV_64F);
					Core.merge(planes, complexI);// 複数のシングルチャンネル配列からマルチチャンネル配列を作成．*/

					
					Core.dft(complexI, filter_img);
					Mat[] G = new Mat[1];
					G[0] = Mat.zeros(width, height, CvType.CV_64FC2);
					System.out.println(filter_img.size());
					System.out.println( ans[0].size());
					System.out.println( G[0].type());
					if (count == 0)
						Core.mulSpectrums(filter_img, ans[0], G[0], 0);
					else
						Core.mulSpectrums(filter_img, result_filter, G[0], 0);
					result_filter = Mat.zeros(width, height, CvType.CV_64FC2);

					Core.mulSpectrums(filter_img, filter_img, Fresult, Core.DFT_COMPLEX_OUTPUT, true);// 分母 B
					Core.mulSpectrums(G[0], filter_img, Gstar, Core.DFT_COMPLEX_OUTPUT, true);// MOSSEフィルタ作成の式の分子(出力画像のフーリエ変換したMat、その複素共役、出力先、ROW、2番目を複素共役にするか否か

					mult(Gstar, 0.125, Gstar);
					mult(Fresult, 0.125, Fresult);

					if (count == 1) {
						mult(result2, (1 - 0.125), result2);// 分子
						mult(result1, (1 - 0.125), result1);// 分母
						Core.add(Gstar, result2, Aia);// 分子
						Core.add(Fresult, result1, Bia);// 分母
					} else {
						mult(Ai, (1 - 0.125), Ai);
						mult(Bi, (1 - 0.125), Bi);
						Core.add(Gstar, Ai, Aia);// 分子
						Core.add(Fresult, Bi, Bia);// 分母
					}

					Core.divide(Aia, Bia, result_filter);
					// "/Documents/3pro/Girl/result_Filter/result_Girl"+String.valueOf(count)+".jpg";//出力先

					Core.idft(G[0], G[0]);
					Core.split(G[0], planes2);
					Core.normalize(planes2.get(0), resultImage_filter, 0, 255, Core.NORM_MINMAX);

					int[] value = max(resultImage_filter.width(),resultImage_filter.height(),resultImage_filter);
					
					int[] rgb = {255,0,0};
					webcam_image.put(value[0], value[1], rgb);
					webcam_image.put(value[0]+1, value[1], rgb);
					webcam_image.put(value[0]-1, value[1], rgb);
					webcam_image.put(value[0], value[1]+1, rgb);
					webcam_image.put(value[0], value[1]-1, rgb);
					
					filter_img = Mat.zeros(width, height, CvType.CV_64FC2);// 0で初期化
					G[0] = Mat.zeros(width, height, CvType.CV_64FC2);// 0で初期化
					Fresult = Mat.ones(width, height, CvType.CV_64FC2);// 0で初期化
					Gstar = Mat.ones(width, height, CvType.CV_64FC2);
					Ai = Mat.ones(width, height, CvType.CV_64FC2);
					Bi = Mat.ones(width, height, CvType.CV_64FC2);

					Ai = Aia.clone();
					Bi = Bia.clone();

					Aia = Mat.zeros(width, height, CvType.CV_64FC2);
					Bia = Mat.zeros(width, height, CvType.CV_64FC2);

					
					
					count++;
					/*
					 * Imgproc.resize(webcam_image, webcam_image, new
					 * Size(webcam_image.size().width*0.3,webcam_image.size().height*0.3));
					 * frame.setSize(webcam_image.width()+40,webcam_image.height()+60);
					 */
					temp = matToBufferedImage(webcam_image);
					panel.setimage(temp);
					panel.repaint();
				} else {
					System.out.println(" --(!) No captured frame -- ");
				}
			}
		}
		return;
	}
}
