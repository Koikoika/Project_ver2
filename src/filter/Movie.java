package filter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.swing.*;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.videoio.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.highgui.*;

public class Movie extends JPanel {
	private static final long serialVersionUID = 1L;
	private BufferedImage image;
	static int width = 720;
	static int height = 1280;

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

	// 四角を書くためのメソッド
	public static void drawsquare(Mat input_img, int[] data) {
		int[] rgb = { 255, 0, 0 };
		input_img.put(data[1], data[0], rgb);
		input_img.put(data[1] + 1, data[0], rgb);
		input_img.put(data[1] - 1, data[0], rgb);
		input_img.put(data[1], data[0] + 1, rgb);
		input_img.put(data[1], data[0] - 1, rgb);
		/*
		 * for (int i = 0; i <data[2]; i++) { for (int j = 0; j <data[3] ; j++) {
		 * input_img.put(data[0]+j, data[1], rgb); } }
		 */
	}

	// 各画素に定数をかけるためのメソッド
	public static void mult(Mat src, double number, Mat dft_ans) {
		double[] data = new double[2];
		for (int i = 0; i < dft_ans.width(); i++) {
			for (int j = 0; j < dft_ans.height(); j++) {
				data = src.get(i, j);
				// System.out.println(data[0]);
				data[0] = data[0] * number;
				data[1] = data[1] * number;
				dft_ans.put(i, j, data);
			}
		}
	}

	public static int[] max(Mat img) {// 最大の画素値の座標を返す
		double max = -1;
		double[] data = new double[2];
		int[] place = new int[2];
		for (int i = 0; i < img.width(); i++) {
			for (int j = 0; j < img.height(); j++) {
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
		Mat webcam_image = Mat.ones(width, height, CvType.CV_64FC3);
		BufferedImage temp;
		VideoCapture capture = new VideoCapture(0);

		// capture.read(webcam_image);
		// System.out.println(webcam_image.size());

		CreateFilter filter = new CreateFilter(width, height);

		Mat filter_img = Mat.ones(width, height, CvType.CV_64FC2);
		String resultfilename;

		
		int x, y, w, h;
		
		List<Mat> planes2 = new ArrayList<Mat>();

		//フィルタ作成の際に使用するMat
		Mat[] ans_input = new Mat[1];
		Mat[] ans_output = new Mat[1];
		Mat[] input = new Mat[20];
		Mat[] output = new Mat[20];
		Mat[] result = new Mat[3];
		
		//フィルタ更新の際に使用するMat
		Mat[] update_input = new Mat[1];
        Mat[] result_update = new Mat[3];
        result_update[0] =  Mat.zeros(width, height, CvType.CV_64FC2);
        result_update[1] =  Mat.zeros(width, height, CvType.CV_64FC2);
        result_update[2] =  Mat.zeros(width, height, CvType.CV_64FC2);
        Mat output_mat = Mat.zeros(width, height, CvType.CV_64FC3);
        int[] ouput_data = new int[4];
        
		int[] data = new int[4];
		data[0] = height/2;
		data[1] = width/2;
		data[2] = height/2;
		data[3] = width/2;
		
		//int[] value = new int[2];
		int count = 1;
		Mat Camera = Mat.ones(width, height, CvType.CV_32S);
		List<Mat> list = new ArrayList<>();
		
		
		ans_input[0] = Mat.zeros(width, height, CvType.CV_64FC2);
		ans_output[0] = Mat.zeros(width, height, CvType.CV_64FC2);
		
		Mat sample = Mat.zeros(width, height, CvType.CV_64FC3);
		
		//フィルタ作成における白ポチ画像
		Mat[] img = new Mat[1];
		img[0] = Mat.zeros(width, height, CvType.CV_64FC3);
		filter.createGimg(img, data);
		sample = img[0].clone();
		Imgcodecs.imwrite("/Users/Karin.T/Documents/3pro/project_c/outputfile/ouput_initial.jpg", sample);

		filter.toFourier(img, ans_output);// 白ポチ
		
		
		if (capture.isOpened()) {

			while (true) {

				capture.read(webcam_image);

				if (!webcam_image.empty()) {
					        //フィルタの作成
					        Imgproc.resize(webcam_image, webcam_image, new Size(webcam_image.size().width*0.3,webcam_image.size().height*0.3));
					        frame.setSize(webcam_image.width()+40,webcam_image.height()+60);  
							Camera = webcam_image.clone();
							Camera.convertTo(Camera, CvType.CV_32SC3);
							if(count<=400) {
							while(count<=400) {
							//drawsquare(Camera, data);
							
							//listの初期化？
                            list.add(webcam_image);
                            
							Camera.convertTo(Camera, CvType.CV_8UC3);
							
							webcam_image = Camera.clone();
							temp=matToBufferedImage(webcam_image);  
						    panel.setimage(temp);
						    panel.repaint();
						    count++;
							}
							int index = 0;
							Mat[] listget = new Mat[1];
							for(int i=1;i<400;i= i+30) {
							// フーリエ変換する
							listget[0] = Mat.zeros(width, height, CvType.CV_64FC3);
							listget[0] = list.get(i);
							filter.toFourier(listget, ans_input);// カメラの画像
							// 初期の画像を作るため createFilterクラスの createFiletrを呼び出す
							input[index] = Mat.ones(width, height, CvType.CV_64FC2);
							input[index] = ans_input[0].clone();
							ans_input[0] = Mat.zeros(width, height, CvType.CV_64FC2);
							output[index] = Mat.ones(width, height, CvType.CV_64FC2);
							output[index] = ans_output[0].clone();
							ans_output[0] = Mat.zeros(width, height, CvType.CV_64FC2);
							index++;
							}						
						filter.createFilter(input, output, 10, result);
						System.out.println(result[0].type());	
					} /*else {
						// フィルタの更新
						update_input[0] = webcam_image.clone();
						filter.toFourier(update_input, ans_input);// カメラの画像
						if(count==400) filter.updatefilter(result, update_input, result_update);
						else filter.updatefilter(result_update, update_input, result_update);
						Core.mulSpectrums(update_input[0], result_update[2], output_mat, 0);
						data = max(output);
						data[2] = height/2;
						data[3] = width/2;
						drawSquare(Camera,data);
						
						
						update_input[0] =  Mat.zeros(width, height, CvType.CV_64FC3);
					    Camera.convertTo(Camera, CvType.CV_8UC3);
					    webcam_image = Camera.clone();
					    temp=matToBufferedImage(webcam_image);  
						panel.setimage(temp);
						panel.repaint();
						
						ans_input[0] = Mat.zeros(width, height, CvType.CV_64FC2);
						count++;
					}*/
				} else {
					System.out.println(" --(!) No captured frame -- ");
				}
			}
		}
		return;
	}

}
