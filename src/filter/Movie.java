package filter;

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.*;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.videoio.*;
import org.opencv.imgproc.Imgproc;
//import org.opencv.highgui.*;
//四角に関するインポート
import java.awt.Graphics;


public class Movie extends JPanel {
	private static final long serialVersionUID = 1L;
	private BufferedImage image;
	static int width;
	static int height;

	// Create a constructor method
	public Movie() {
		super();
	}

	private BufferedImage getimage() {
		return image;
	}

	/*private void setimage(BufferedImage newimage) {
		image = newimage;
		return;
	}*/
	
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

	public static void initialize(Mat[] mat) {//Mat型の配列を初期化する
		for(int i=0;i<mat.length;i++) {
			mat[i] = Mat.zeros(width, height, CvType.CV_64FC3);
		}
	}
	// 四角を書くためのメソッド
	public static void drawsquare(Mat webcam_image,int x,int y,int width,int height) {
        //Imgproc.rectangle(Mat,四角の1つの頂点の座標,その対角線上にある頂点の座標,線の色, 線の幅, 線の種類, 0);
		Imgproc.rectangle(webcam_image, new Point(x-width/2, y-height/2), new Point(x+width/2, y+height/2), new Scalar(0, 0, 225), 8, 8, 0);
		Imgproc.line(webcam_image, new Point(x,y-height/2), new Point(x, y+height/2), new Scalar(0, 0, 225));// 縦
		Imgproc.line(webcam_image, new Point(x-width/2, y), new Point(x+width/2,y), new Scalar(0, 0, 225));// 横

	}

	public static int[] max(Mat img) {// 最大の画素値の座標を返す
		double max = -1;
		double[] data = new double[2];
		int[] place = new int[2];
		for (int i = 0; i < img.width(); i++) {
			for (int j = 0; j < img.height(); j++) {
				data = img.get(j, i);
				if (max < data[0]) {
					max = data[0];
					place[0] = j;
					place[1] = i;
				}
			}
		}
		return place;
	}

	public  Mat[] get_filter_original(Mat[] webcam_img) {// カメラから得た画像からフィルタの元となる入力画像を得る
		Mat[] input = new Mat[13];

		for (int i = 0; i < 13; i++) {
			input[i] = Mat.zeros(width, height, CvType.CV_64FC3);
			webcam_img[30 *(i+1)].convertTo(webcam_img[30 *(i+1)], CvType.CV_32SC3);
			input[i] = webcam_img[30 * (i+1)];
		}
		return input;
	}

	public  Mat[] makeFilter(Mat[] input) throws IOException {// 数枚の入力画像と出力画像から初期フィルタを作成する
		CreateFilter filter = new CreateFilter(width, height);

		Mat[] ans_output = new Mat[1];
		Mat[] output = new Mat[input.length];
		Mat result[] = new Mat[3];
		for (int i = 0; i < result.length; i++) {
			result[i] = Mat.zeros(width, height, CvType.CV_64FC2);
		}
		ans_output[0] = Mat.zeros(width, height, CvType.CV_64FC2);

		// 出力画像を作成し、それをフーリエ変換する
		int[] data = new int[4];
		data[0] = height / 2;
		data[1] = width / 2;
		data[2] = height / 2;
		data[3] = width / 2;

		Mat[] img = new Mat[1];
		Mat[] img_input = new Mat[1];
		img[0] = Mat.zeros(width, height, CvType.CV_64FC3);
		filter.createGimg(img, data);
		img[0].convertTo(img[0], CvType.CV_64FC3);
		filter.toFourier(img, ans_output);// 白ポチ
		for (int i = 0; i < input.length; i++) {
			//出力画像の配列の作成
			output[i] = Mat.zeros(width, height, CvType.CV_64FC2);
			output[i] = ans_output[0].clone();
			//入力画像のフーリエ変換
			img_input[0] = Mat.zeros(width, height, CvType.CV_64FC3);
			img_input[0] = input[i].clone();
			img_input[0].convertTo(img_input[0], CvType.CV_32FC3);
			filter.toFourier(img_input, img_input);
			input[i] = img_input[0].clone();
		}
		// 入力画像と出力画像を用いて初期フィルタを作成し、分母、分子、フィルタの順にこれらが格納された配列resultが返ってくる
		filter.createFilter(input, output, input.length, result);

		return result;
	}

	public Mat[] new_makeFilter(Mat[] result, Mat webcam_img) throws IOException {// フィルタの更新を行い、それから最大画素値の座標を出力
		CreateFilter filter = new CreateFilter(width, height);
		Mat update_result[] = new Mat[3];
		for (int i = 0; i < update_result.length; i++) {
			update_result[i] = Mat.zeros(width, height, CvType.CV_64FC2);
		}

		// 入力画像をフーリエ変換しやすいように変換後、フーリエ変換
		Mat[] input = new Mat[1];
		webcam_img.convertTo(webcam_img, CvType.CV_32FC3);
		input[0] = Mat.zeros(width, height, CvType.CV_32FC3);
		input[0] = webcam_img.clone();
		//Mat output = Mat.zeros(width, height, CvType.CV_32FC3);
		filter.toFourier(input, input);

		// 入力画像とフィルタと前の分子と分母を利用してフィルタの更新
		filter.updatefilter(result, input, update_result);
		return update_result;
	}
	
	public int[] tracking(Mat webcam_img,Mat new_filter) throws IOException {//カメラの画像とフーリエ変換済みのフィルター
		CreateFilter filter = new CreateFilter(width, height);
		Mat[] input = new Mat[1];
		input[0] =  Mat.zeros(width, height, CvType.CV_64FC3);
		
		Mat[] output = new Mat[1];
		output[0]= Mat.zeros(width, height, CvType.CV_64FC2);
		int[] xy = new int[2];
	
		webcam_img.convertTo(webcam_img, CvType.CV_32FC3);
		input[0] = webcam_img.clone();
		filter.toFourier(input, input);
		
		/*Mat[] Filter = new Mat[1];
		Filter[0] = Mat.zeros(width, height, CvType.CV_64FC3);
		Filter[0] = new_filter.clone();
		filter.toFourier(Filter, Filter);*/
		
		//入力画像、出力画像をフーリエ変換して出力されたフィルタを逆フーリエ変換して最大画素値の座標を出力
		Core.mulSpectrums(input[0], new_filter, output[0], 0);
		filter.IDFT(output, output);
		
		xy = max(output[0]);
		
		return xy;
	}
	
	public static void main(String arg[]) {
		width = 216;
		height  = 384;
		int count = 0;
		
		// Load the native library.
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		JFrame frame = new JFrame("BasicPanel");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(500, 400);
		Panel panel = new Panel();
		frame.setContentPane(panel);
		frame.setVisible(true);
		Mat webcam_image = new Mat();
		BufferedImage temp;
		VideoCapture capture = new VideoCapture(0);
		
		Mat[] data = new Mat[400];
		Mat[] result = new Mat[3];
		initialize(result);
		Mat[] new_result = new Mat[3];
		initialize(new_result);
		Mat[] update_result = new Mat[3];
		initialize(update_result);

		Movie movie = new Movie();
		
		int[] answer = new int[2]; 
		
		if (capture.isOpened()) {
			while (true) {

				capture.read(webcam_image);
				
				if (!webcam_image.empty()) {
					Imgproc.resize(webcam_image, webcam_image,
							new Size(webcam_image.size().width * 0.3, webcam_image.size().height * 0.3));
					frame.setSize(webcam_image.width() + 40, webcam_image.height() + 60);
					if(count<400) {//初期フィルタを作成
						data[count] = Mat.zeros(width, height, CvType.CV_64FC3);
						data[count] = webcam_image.clone();
						drawsquare(webcam_image,webcam_image.width()/2,webcam_image.height()/2,webcam_image.width()/2,webcam_image.height()/2);
						System.out.println("initial filter create....");
					}else {
						try {
							if(count==400) {
							result = movie.makeFilter(movie.get_filter_original(data));
							new_result = movie.new_makeFilter(result,webcam_image);
							System.out.println("filter create!!");
							} else {
							//フィルタを更新
							update_result = movie.new_makeFilter(new_result,webcam_image);
							new_result = update_result.clone();
							initialize(update_result);
							
							answer = movie.tracking(webcam_image,new_result[2]);
							drawsquare(webcam_image,answer[0],answer[1],webcam_image.width()/2,webcam_image.height()/2);
							System.out.println("tracking now!");
							System.out.println(answer[0]);
							System.out.println(answer[1]);
							}
						} catch (IOException e) {
							// TODO 自動生成された catch ブロック
							e.printStackTrace();
						}
					}
					count++;
				    webcam_image.convertTo(webcam_image, CvType.CV_8UC3);
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
