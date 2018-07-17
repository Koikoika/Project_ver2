package filter;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.*;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.*;
import org.opencv.imgproc.Imgproc;
//import org.opencv.highgui.*;
//四角に関するインポート
import java.awt.Graphics;

public class Movie extends JPanel {
	private static final long serialVersionUID = 1L;
	private BufferedImage image;
	static int m_width;//処理するイメージの大きさ
	static int m_height;
	CreateFilter m_filter;//CreateFilterクラスのインスタンス
	static ArrayList<Mat> m_input;//初期フィルタ作成用画像例
	static Mat[] m_filterFourier;//フィルタの分母、分子、初期フィルタ
	static Mat[] m_updatefilter;//フィルタ更新のための分母、分子、フィルタ

	
	public Movie() {
		super();
		 m_filter = new CreateFilter(m_width, m_height);
		 m_input = new ArrayList<Mat>();
		 m_filterFourier = new Mat[3];
		 m_updatefilter = new Mat[3];
	}

	private BufferedImage getimage() {
		return image;
	}

	/*
	 * private void setimage(BufferedImage newimage) { image = newimage; return; }
	 */

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

	public static void initialize(Mat[] mat) {// Mat型の配列を初期化する
		for (int i = 0; i < mat.length; i++) {
			mat[i] = Mat.zeros(m_width, m_height, CvType.CV_64FC3);
		}
	}

	// 四角を書くためのメソッド
	public static void drawsquare(Mat[] webcam_image, int x, int y, int width, int height) {
		// Imgproc.rectangle(Mat,四角の1つの頂点の座標,その対角線上にある頂点の座標,線の色, 線の幅, 線の種類, 0);
		Imgproc.rectangle(webcam_image[0], new Point(x - width / 2, y - height / 2),
				new Point(x + width / 2, y + height / 2), new Scalar(0, 0, 225), 8, 8, 0);
		Imgproc.line(webcam_image[0], new Point(x, y - height / 2), new Point(x, y + height / 2), new Scalar(0, 0, 225));// 縦
		Imgproc.line(webcam_image[0], new Point(x - width / 2, y), new Point(x + width / 2, y), new Scalar(0, 0, 225));// 横

	}

	public static int[] max(Mat[] img) {// 最大の画素値の座標を返す
		double max = -1;
		double[] data = new double[2];
		int[] place = new int[2];
		for (int i = 0; i < img[0].width(); i++) {
			for (int j = 0; j < img[0].height(); j++) {
				data = img[0].get(j, i);
				if (max < data[0]) {
					max = data[0];
					place[0] = j;
					place[1] = i;
				}
			}
		}
		return place;
	}

	public void get_filter_original(Mat[] webcam_img) {// カメラから得た画像（複数枚）からフィルタの元となる入力画像を得る
		for (int i = 0; i < 13; i++) {
			webcam_img[30 * (i + 1)].convertTo(webcam_img[30 * (i + 1)], CvType.CV_32SC3);
			m_input.add(webcam_img[30 * (i + 1)].clone());
			//Imgcodecs.imwrite("/Users/Karin.T/Documents/3pro/project_c/debug/"+Integer.toString(i)+".jpg", webcam_img[30 * (i + 1)]);
		}
	}

	public void makeFilter(ArrayList<Mat> m_input) throws IOException {// 複数枚の入力画像と出力画像から初期フィルタを作成する
		Mat[] ans_output = new Mat[1];
		Mat[] input = new Mat[m_input.size()];
		Mat[] output = new Mat[m_input.size()];

		ans_output[0] = Mat.zeros(m_width, m_height, CvType.CV_64FC2);

		// 出力画像を作成し、それをフーリエ変換する
		int[] data = new int[4];
		data[0] = m_height / 2;
		data[1] = m_width / 2;
		data[2] = m_height / 2;
		data[3] = m_width / 2;

		Mat[] img = new Mat[1];
		Mat[] img_input = new Mat[1];
		img[0] = Mat.zeros(m_width, m_height, CvType.CV_64FC3);
		m_filter.createGimg(img, data);
		img[0].convertTo(img[0], CvType.CV_64FC3);
		m_filter.toFourier(img, ans_output);// 白ポチの画像（フーリエ領域）の完成
		
		for (int i = 0; i < m_input.size(); i++) {
			// 出力画像の配列の作成
			input[i] = Mat.zeros(m_width, m_height, CvType.CV_64FC2);
			output[i] = Mat.zeros(m_width, m_height, CvType.CV_64FC2);
			output[i] = ans_output[0].clone();
			// 入力画像のフーリエ変換
			img_input[0] = Mat.zeros(m_width, m_height, CvType.CV_64FC3);
			img_input[0] = m_input.get(i).clone();
			img_input[0].convertTo(img_input[0], CvType.CV_32FC3);
			m_filter.toFourier(img_input, img_input);
			input[i] = img_input[0].clone();
		}
		// 入力画像と出力画像を用いて初期フィルタを作成し、分母、分子、フィルタの順にこれらが格納された配列resultが返ってくる
		m_filter.createFilter(input, output, m_input.size(), m_filterFourier);
	}

	public void new_makeFilter(Mat[] result, Mat[] webcam_img) throws IOException {// フィルタの更新を行い、それから最大画素値の座標を出力
		
		// 入力画像をフーリエ変換しやすいように変換後、フーリエ変換
		Mat[] input = new Mat[1];
		webcam_img[0].convertTo(webcam_img[0], CvType.CV_32FC3);
		input[0] = Mat.zeros(m_width, m_height, CvType.CV_32FC3);
		input[0] = webcam_img[0].clone();
		// Mat output = Mat.zeros(width, height, CvType.CV_32FC3);
		m_filter.toFourier(input, input);

		// 入力画像とフィルタと前の分子と分母を利用してフィルタの更新
		m_filter.updatefilter(result, input, m_updatefilter);
	}

	public int[] tracking(Mat[] webcam_img, Mat[] new_filter) throws IOException {// カメラの画像とフーリエ変換済みのフィルター
		Mat[] input = new Mat[1];
		input[0] = Mat.zeros(m_width, m_height, CvType.CV_64FC3);

		Mat[] output = new Mat[1];
		output[0] = Mat.zeros(m_width, m_height, CvType.CV_64FC2);
		int[] xy = new int[2];

		webcam_img[0].convertTo(webcam_img[0], CvType.CV_32FC3);
		input[0] = webcam_img[0].clone();
		m_filter.toFourier(input, input);

		/*
		 * Mat[] Filter = new Mat[1]; Filter[0] = Mat.zeros(width, height,
		 * CvType.CV_64FC3); Filter[0] = new_filter.clone(); filter.toFourier(Filter,
		 * Filter);
		 */

		// 入力画像、出力画像をフーリエ変換して出力されたフィルタを逆フーリエ変換して最大画素値の座標を出力
		Core.mulSpectrums(input[0], new_filter[2], output[0], 0);
		m_filter.IDFT(output, output);

		xy = max(output);

		return xy;
	}

	// カメラの動画を反転させる
	public static BufferedImage createMirrorImage(BufferedImage temp) {
		int width = temp.getWidth();
		int height = temp.getHeight();
		int size = width * height;
		int[] buf = new int[size];
		temp.getRGB(0, 0, width, height, buf, 0, width);// イメージを配列に変換

		// bufのイメージ配列で、左右を変換する。
		int x1, x2, temp2;
		for (int y = 0; y < size; y += width) {
			x1 = 0;
			x2 = width - 1;
			while (x1 < x2) {// 交換の繰り返し
				temp2 = buf[y + x1];
				buf[y + x1++] = buf[y + x2];
				buf[y + x2--] = temp2;
			}
		}
		BufferedImage img2 = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
		img2.setRGB(0, 0, width, height, buf, 0, width);// 配列をイメージに書き込む
		return img2;
	}

	public static void main(String arg[]) {
		m_width = 432;
		m_height = 768;
		int count = 0;

		// Load the native library.
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		JFrame frame = new JFrame("BasicPanel");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(500, 400);
		Panel panel = new Panel();
		frame.setContentPane(panel);
		frame.setVisible(true);
		Mat[] webcam_image = new Mat[1];
		webcam_image[0] = Mat.zeros(m_width, m_height, CvType.CV_64FC3);
		BufferedImage img;
		BufferedImage imgRev; // 反転したイメージ
		VideoCapture capture = new VideoCapture(0);

		Mat[] data = new Mat[400];
		Movie movie = new Movie();
		int[] answer = new int[2];

		if (capture.isOpened()) {
			
			while (true) {

				capture.read(webcam_image[0]);
				
				if (!webcam_image[0].empty()) {
					// 元々0.3で、0.6で大体画面いっぱい
					Imgproc.resize(webcam_image[0], webcam_image[0],
							new Size(webcam_image[0].size().width * 0.6, webcam_image[0].size().height * 0.6));
					frame.setSize(webcam_image[0].width() + 40, webcam_image[0].height() + 60);
					if (count < 400) {// 初期フィルタを作成するための入力画像を得る
						data[count] = Mat.zeros(m_width, m_height, CvType.CV_64FC3);
						data[count] = webcam_image[0].clone();
						drawsquare(webcam_image, webcam_image[0].width() / 2, webcam_image[0].height() / 2,
								webcam_image[0].width() / 2, webcam_image[0].height() / 2);
						System.out.println("initial filter create....");
					} else {
						try {
							if (count == 400) {
								movie.get_filter_original(data);//クラス変数ArrayListのm_inputにフィルタ作成に用いる入力画像を格納する
								//System.out.println(m_input.size());
								movie.makeFilter(m_input);//クラス変数m_filterFourierに分母、分子、フィルタを格納
								movie.new_makeFilter(m_filterFourier, webcam_image);
								System.out.println("filter create!!");
							} else {
								// フィルタを更新
								movie.new_makeFilter(m_updatefilter, webcam_image);

								answer = movie.tracking(webcam_image,m_updatefilter );
								drawsquare(webcam_image, answer[0], answer[1], webcam_image[0].width() / 2,
										webcam_image[0].height() / 2);
								System.out.println("tracking now!");
								System.out.println(answer[0]);
								System.out.println(answer[1]);
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					System.out.println(count);
					count++;
					webcam_image[0].convertTo(webcam_image[0], CvType.CV_8UC3);
					img = matToBufferedImage(webcam_image[0]);
					imgRev = createMirrorImage(img);// matからイメージに変換してから反転させる

					panel.setimage(imgRev);

					panel.repaint();
				} else {
					System.out.println(" --(!) No captured frame -- ");
				}
			}
		}
		return;
	}
}
