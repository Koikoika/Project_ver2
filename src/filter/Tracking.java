package filter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.*;
import java.awt.event.*;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.*;
import org.opencv.imgproc.Imgproc;

import java.awt.Graphics;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class Tracking extends JPanel {
	// Movieクラスのインスタンス化
	static int m_width;
	static int m_height;
	Movie m_movie;

	// 座標をユーザーごとに保存
	ArrayList<Integer> dst1;
	ArrayList<Integer> dst2;
	ArrayList<Integer> dst3;
	ArrayList<Integer> src;

	// 初期フィルタ
	Mat user1;
	Mat user2;
	Mat user3;

	private static final long serialVersionUID = 1L;
	private BufferedImage image;

	// Create a constructor method
	public Tracking() {
		super();
		m_movie = new Movie(m_width, m_height);

		dst1 = new ArrayList<>();
		dst2 = new ArrayList<>();
		dst3 = new ArrayList<>();
		src = new ArrayList<>();

		user1 = Mat.zeros(m_width, m_height, CvType.CV_64FC2);
		user2 = Mat.zeros(m_width, m_height, CvType.CV_64FC2);
		user3 = Mat.zeros(m_width, m_height, CvType.CV_64FC2);
	}

	private BufferedImage getimage() {
		return image;
	}

	/**
	 * Converts/writes a Mat into a BufferedImage.
	 * 
	 * @param matrix
	 *            Mat of type CV_8UC3 or CV_8UC1
	 * @return BufferedImage of type TYPE_3BYTE_BGR or TYPE_BYTE_GRAY
	 */
	
	//Mat型の画像をBufferedImage型にする
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

	// カメラの動画を反転させる
	public static BufferedImage createMirrorImage(BufferedImage temp) {
		int width = temp.getWidth();
		int height = temp.getHeight();
		int size = width * height;
		int[] buf = new int[size];
		temp.getRGB(0, 0, width, height, buf, 0, width); // イメージを配列に変換

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

	//ファイルが存在するかどうか確認する
	private static boolean checkBeforeWritefile(File file) {
		if (file.exists()) {
			if (file.isFile() && file.canWrite()) {
				return true;
			}
		}
		return false;
	}

	static int cnt = 0;

	// 画像読み込み
	public static double[] Readimg(String img) {

		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		// 画像の読み込み
		Mat im = Imgcodecs.imread(img,1); // 入力画像の取得

		int cols = im.cols();
		int rows = im.rows();
		int size = cols * rows*3;
		double[] data = new double[size];

		int y;
		int x;
		int count = 0;

		// rows(行)の数分回す
		for (y = 0; y < rows; y++) {

			// cols(列)の数だけ読み込む
			for (x = 0; x < cols; x++) {
				double[] d = im.get(y, x);
				data[count++] = d[0];
				data[count++] = d[1];
				data[count++] = d[2];

			}
		}
		return data;
	}

	// 2チャンネルのMatの要素をテキストファイルに読み込む(初期フィルタをテキストファイルに保存)
	public void initial_filter_write(int user, Mat src) throws IOException {
		// 保存するファイル先を記入
		double[] data = new double[2];
		try {
			File file = new File("/Users/Karin.T/Documents/3pro/project_c/user" + String.valueOf(user) + "/initial_filter.txt");

			if (checkBeforeWritefile(file)) {
				FileWriter filewriter = new FileWriter(file, true);
				
				filewriter.write(src.width() + "\n");
				filewriter.write(src.height() + "\n");
				
				for (int i = 0; i < src.width(); i++) {
					for (int j = 0; j < src.height(); j++) {
						data = src.get(j, i);
						filewriter.write(data[0] + "\n");
						filewriter.write(data[1] + "\n");
					}
				}
				
				filewriter.close();
			} else {
				System.out.println("ファイルに書き込めません");
			}
		} catch (IOException e) {
			System.out.println(e);
		}
	}


	public static void main(String arg[]) {
		// Load the native library.
		m_width = 324;
		m_height = 576;

		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		JFrame frame = new JFrame("Unlocking System");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(400, 400);
		Panel panel = new Panel();
		frame.setContentPane(panel);
		frame.setVisible(true);

		Mat[] webcam_image = new Mat[1];
		webcam_image[0] = Mat.zeros(m_width, m_height, CvType.CV_64FC3);

		Mat[] webcam_image2 = new Mat[1];// ウィンド表示用
		webcam_image2[0] = Mat.zeros(m_width, m_height, CvType.CV_64FC3);

		NearestNeighbor1 nearestNeighbor1 = new NearestNeighbor1();
		NearestNeighbor2 nearestNeighbor2 = new NearestNeighbor2();
		
		
		for(int i=3;i<6;i++) {
		nearestNeighbor1.learn(1,
				Readimg("/Users/Karin.T/Documents/3pro/project_c/user1/input/trim"+String.valueOf(i)+".jpg"));
		nearestNeighbor1.learn(2,
				Readimg("/Users/Karin.T/Documents/3pro/project_c/user2/input/trim"+String.valueOf(i)+".jpg"));
		nearestNeighbor1.learn(3,
				Readimg("/Users/Karin.T/Documents/3pro/project_c/user3/input/trim"+String.valueOf(i)+".jpg"));
		nearestNeighbor1.learn(4,
				Readimg("/Users/Karin.T/Documents/3pro/project_c/other/1/trim"+String.valueOf(i)+".jpg"));
		nearestNeighbor1.learn(5,
				Readimg("/Users/Karin.T/Documents/3pro/project_c/other/2/trim"+String.valueOf(i)+".jpg"));
		
		}
		
		VideoCapture capture = new VideoCapture(0);
		JButton button1 = new JButton("user1");
		JButton button2 = new JButton("user2");
		JButton button3 = new JButton("user3");
		JButton open = new JButton("open");

		// トラッキング（登録）に使用
		Tracking tracking = new Tracking();
		ArrayList<Mat> input = new ArrayList<Mat>();

		frame.add(button1);
		frame.add(button2);
		frame.add(button3);
		frame.add(open);

		frame.setSize(webcam_image[0].width() + 40, webcam_image[0].height() + 60);

		button1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				JLabel msg = new JLabel("user1の物体の登録を開始します(15秒間)。黒枠の中に物体を入れてください");
				JOptionPane.showMessageDialog(frame, msg);

				if (capture.isOpened()) {

					Timer timer = new Timer(false);
					
					Timer timer2 = new Timer(false);

					TimerTask task = new TimerTask() {

						int[] answer = new int[2];

						BufferedImage img;
						BufferedImage imgRev; // 反転したイメージ

						@Override
						public void run() {

							capture.read(webcam_image[0]);
							
							Imgproc.resize(webcam_image[0], webcam_image[0],
									new Size(webcam_image[0].size().width * 0.3, webcam_image[0].size().height * 0.3));
							frame.setSize(webcam_image[0].width() + 40, webcam_image[0].height() + 60);

							if (cnt < 13) {
								Imgcodecs.imwrite("/Users/Karin.T/Documents/3pro/project_c/user1/input/"
										+ String.valueOf(cnt) + ".png", webcam_image[0]);
								if (webcam_image[0].size().width != m_width
										|| webcam_image[0].size().height != m_height) {
									Imgproc.resize(webcam_image[0], webcam_image[0], new Size(
											webcam_image[0].size().width * 0.3, webcam_image[0].size().height * 0.3));
								}
								input.add(webcam_image[0]);

								System.out.println("initial filter create....");
							} else if (cnt == 13) {
								try {
									// クラス変数m_filterFourierに分母、分子、フィルタを格納
									tracking.m_movie.makeFilter(input);
								} catch (IOException e) {
									e.printStackTrace();
								}
								for(int i=3; i<6;i++) {
								// 赤枠内の画像をトリミングして保存
								Rect roi = new Rect(m_height / 2 - m_width / 4, m_width / 2 - m_width / 4, m_width / 2,
										m_width / 2);
								Mat trim3 = new Mat(input.get(i), roi);

								Imgcodecs.imwrite("/Users/Karin.T/Documents/3pro/project_c/user1/input/trim"+String.valueOf(i)+".jpg",
										trim3);
								
								// 保存した画像を学習させる
								nearestNeighbor1.learn(1,
										Readimg("/Users/Karin.T/Documents/3pro/project_c/user1/input/trim"+String.valueOf(i)+".jpg"));
								}
								
								//初期フィルタを保存
								tracking.user1 = tracking.m_movie.m_filterFourier[2].clone();
								
								// テキストファイルに保存
								try {
									tracking.initial_filter_write(1, tracking.m_movie.m_filterFourier[2]);
								} catch (IOException e1) {
									e1.printStackTrace();
								}
								
								System.out.println("filter create!!");

								JLabel end = new JLabel("次に物体の軌跡の登録をします(10秒)");
								JOptionPane.showMessageDialog(frame, end);

							} else if (cnt > 13 && cnt < 34) {
								try {
									answer = tracking.m_movie.tracking(webcam_image, tracking.m_movie.m_filterFourier);
								} catch (IOException e) {
									e.printStackTrace();
								}
								
								// 保存
								Imgcodecs.imwrite("/Users/Karin.T/Documents/3pro/project_c/user1/tracking/"
										+ String.valueOf(cnt) + ".png", webcam_image[0]);

								tracking.dst1.add(answer[0]);
								tracking.dst1.add(answer[1]);

								try {
									File file = new File("/Users/Karin.T/Documents/3pro/project_c/user1/track1.txt");

									if (checkBeforeWritefile(file)) {
										FileWriter filewriter = new FileWriter(file, true);

										filewriter.write(answer[0] + "\n");
										filewriter.write(answer[1] + "\n");

										filewriter.close();
									} else {
										System.out.println("ファイルに書き込めません");
									}
								} catch (IOException e) {
									System.out.println(e);
								}

								System.out.println("tracking now!");
								System.out.println(answer[0]);
								System.out.println(answer[1]);

							} else if (cnt == 34) {
								timer.cancel();
								timer2.cancel();
								
								cnt = 0;

								int[] data1 = new int[tracking.dst1.size()];
								
								for(int i=0; i<tracking.dst1.size();i++) {
									data1[i] = tracking.dst1.get(i);
								}
								
								//軌道を学習する
								nearestNeighbor2.learn(1, data1);
								
								// 登録完了
								JLabel end = new JLabel("登録完了");
								JOptionPane.showMessageDialog(frame, end);
							}

							System.out.println(cnt);
							cnt++;
							webcam_image[0].convertTo(webcam_image[0], CvType.CV_8UC3);
							img = matToBufferedImage(webcam_image[0]);
							imgRev = createMirrorImage(img);// matからイメージに変換してから反転させる
							panel.setimage(imgRev);

						}
					};
					timer.schedule(task, 0, 500);


					TimerTask task2 = new TimerTask() {
						BufferedImage img;
						BufferedImage imgRev;

						public void run() {
							if (capture.isOpened()) {
								

								capture.read(webcam_image[0]);
								if (!webcam_image[0].empty()) {

									Imgproc.resize(webcam_image[0], webcam_image[0], new Size(
											webcam_image[0].size().width * 0.3, webcam_image[0].size().height * 0.3));
									frame.setSize(webcam_image[0].width() + 40, webcam_image[0].height() + 60);

									webcam_image2[0] = webcam_image[0].clone();
									// 四角描写
									tracking.m_movie.drawsquare(webcam_image2, webcam_image2[0].width() / 2,
											webcam_image2[0].height() / 2, m_width / 2, m_width / 2);
									img = matToBufferedImage(webcam_image2[0]);
									imgRev = createMirrorImage(img);// matからイメージに変換してから反転させる
									panel.setimage(imgRev);
									panel.repaint();

								} else {
									System.out.println(" --(!) No captured frame -- ");
								}
							}
						}

					};
					timer2.schedule(task2, 0, 500);

					
				} else {
					System.out.println(" --(!) No captured frame -- ");
				}
			}
		});
		

		button2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				JLabel msg = new JLabel("user2の物体の登録を開始します(15秒間)。黒枠の中に物体を入れてください");
				JOptionPane.showMessageDialog(frame, msg);

				if (capture.isOpened()) {
					
					cnt = 0;

					Timer timer = new Timer(false);
					Timer timer2 = new Timer(false);

					TimerTask task = new TimerTask() {

						
						int[] answer = new int[2];

						BufferedImage img;
						BufferedImage imgRev; // 反転したイメージ

						@Override
						public void run() {

							capture.read(webcam_image[0]);
							Imgproc.resize(webcam_image[0], webcam_image[0],
									new Size(webcam_image[0].size().width * 0.3, webcam_image[0].size().height * 0.3));
							frame.setSize(webcam_image[0].width() + 40, webcam_image[0].height() + 60);

							if (cnt < 13) {
								Imgcodecs.imwrite("/Users/Karin.T/Documents/3pro/project_c/user2/input/"
										+ String.valueOf(cnt) + ".png", webcam_image[0]);
								if (webcam_image[0].size().width != m_width
										|| webcam_image[0].size().height != m_height) {
									Imgproc.resize(webcam_image[0], webcam_image[0], new Size(
											webcam_image[0].size().width * 0.3, webcam_image[0].size().height * 0.3));
								}
								input.add(webcam_image[0]);

								System.out.println("initial filter create....");
								// movie.get_filter_original(data1);
							} else if (cnt == 13) {
								try {
									// クラス変数m_filterFourierに分母、分子、フィルタを格納
									tracking.m_movie.makeFilter(input);
								} catch (IOException e) {
									e.printStackTrace();
								}
								for(int i=3;i<6;i++) {
								// 赤枠内の画像をトリミングして保存
								Rect roi = new Rect(m_height / 2 - m_width / 4, m_width / 2 - m_width / 4, m_width / 2,
										m_width / 2);
								Mat trim3 = new Mat(input.get(i), roi);

								Imgcodecs.imwrite("/Users/Karin.T/Documents/3pro/project_c/user2/input/trim"+String.valueOf(i)+".jpg",
										trim3);

								// 保存した画像を学習させる
								nearestNeighbor1.learn(2,
										Readimg("/Users/Karin.T/Documents/3pro/project_c/user2/input/trim"+String.valueOf(i)+".jpg"));
								}

								tracking.user2 = tracking.m_movie.m_filterFourier[2].clone();
								// テキストファイルに保存
								try {
									tracking.initial_filter_write(2, tracking.m_movie.m_filterFourier[2]);
								} catch (IOException e1) {
									e1.printStackTrace();
								}
								System.out.println("filter create!!");

								JLabel end = new JLabel("次に物体の軌跡の登録をします(10秒)");
								JOptionPane.showMessageDialog(frame, end);

							} else if (cnt > 13 && cnt < 34) {
								try {
									answer = tracking.m_movie.tracking(webcam_image, tracking.m_movie.m_filterFourier);
								} catch (IOException e) {
									e.printStackTrace();
								}
								// 保存

								Imgcodecs.imwrite("/Users/Karin.T/Documents/3pro/project_c/user2/tracking/"
										+ String.valueOf(cnt) + ".png", webcam_image[0]);

								tracking.dst2.add(answer[0]);
								tracking.dst2.add(answer[1]);

								try {
									File file = new File("/Users/Karin.T/Documents/3pro/project_c/user2/track2.txt");

									if (checkBeforeWritefile(file)) {
										FileWriter filewriter = new FileWriter(file, true);

										filewriter.write(answer[0] + "\n");
										filewriter.write(answer[1] + "\n");

										filewriter.close();
									} else {
										System.out.println("ファイルに書き込めません");
									}
								} catch (IOException e) {
									System.out.println(e);
								}

								System.out.println("tracking now!");
								System.out.println(answer[0]);
								System.out.println(answer[1]);

							} else if (cnt == 34) {
								timer.cancel();
								timer2.cancel();
								cnt = 0;
								
                                int[] data2 = new int[tracking.dst2.size()];
								
								for(int i=0; i<tracking.dst2.size();i++) {
									data2[i] = tracking.dst2.get(i);
								}
								
								nearestNeighbor2.learn(2, data2);
								
								// 登録完了
								JLabel end = new JLabel("登録完了");
								JOptionPane.showMessageDialog(frame, end);
							}

							System.out.println(cnt);
							cnt++;
							webcam_image[0].convertTo(webcam_image[0], CvType.CV_8UC3);
							img = matToBufferedImage(webcam_image[0]);
							imgRev = createMirrorImage(img);// matからイメージに変換してから反転させる
							panel.setimage(imgRev);

						}
					};
					timer.schedule(task, 0, 500);


					TimerTask task2 = new TimerTask() {
						BufferedImage img;
						BufferedImage imgRev;

						public void run() {
							if (capture.isOpened()) {

								capture.read(webcam_image[0]);
								if (!webcam_image[0].empty()) {

									// 元々0.3で、0.6で大体画面いっぱい
									Imgproc.resize(webcam_image[0], webcam_image[0], new Size(
											webcam_image[0].size().width * 0.3, webcam_image[0].size().height * 0.3));
									frame.setSize(webcam_image[0].width() + 40, webcam_image[0].height() + 60);

									webcam_image2[0] = webcam_image[0].clone();
									// 四角描写
									tracking.m_movie.drawsquare(webcam_image2, webcam_image2[0].width() / 2,
											webcam_image2[0].height() / 2, m_width / 2, m_width / 2);
									img = matToBufferedImage(webcam_image2[0]);
									imgRev = createMirrorImage(img);// matからイメージに変換してから反転させる
									panel.setimage(imgRev);
									panel.repaint();

								} else {
									System.out.println(" --(!) No captured frame -- ");
								}
							}
						}

					};
					timer2.schedule(task2, 0, 500);
					// ここで軌道を学習させる

					
				} else {
					System.out.println(" --(!) No captured frame -- ");
				}
			}
		});

		button3.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				JLabel msg = new JLabel("user3の物体の登録を開始します(15秒間)。黒枠の中に物体を入れてください");
				JOptionPane.showMessageDialog(frame, msg);
				

				if (capture.isOpened()) {

					Timer timer = new Timer(false);
					Timer timer2 = new Timer(false);
					cnt = 0;
					
					TimerTask task = new TimerTask() {

						int[] answer = new int[2];

						BufferedImage img;
						BufferedImage imgRev; // 反転したイメージ

						
						@Override
						public void run() {
							
							

							capture.read(webcam_image[0]);
							Imgproc.resize(webcam_image[0], webcam_image[0],
									new Size(webcam_image[0].size().width * 0.3, webcam_image[0].size().height * 0.3));
							frame.setSize(webcam_image[0].width() + 40, webcam_image[0].height() + 60);

							if (cnt < 13) {
								Imgcodecs.imwrite("/Users/Karin.T/Documents/3pro/project_c/user3/input/"
										+ String.valueOf(cnt) + ".png", webcam_image[0]);
								if (webcam_image[0].size().width != m_width
										|| webcam_image[0].size().height != m_height) {
									Imgproc.resize(webcam_image[0], webcam_image[0], new Size(
											webcam_image[0].size().width * 0.3, webcam_image[0].size().height * 0.3));
								}
								input.add(webcam_image[0]);

								System.out.println("initial filter create....");
								
							} else if (cnt == 13) {
								try {
									// クラス変数m_filterFourierに分母、分子、フィルタを格納
									tracking.m_movie.makeFilter(input);
								} catch (IOException e) {
									e.printStackTrace();
								}
								
								for(int i=3;i<6;i++) {
								// 赤枠内の画像をトリミングして保存
								Rect roi = new Rect(m_height / 2 - m_width / 4, m_width / 2 - m_width / 4, m_width / 2,
										m_width / 2);
								Mat trim3 = new Mat(input.get(i), roi);

								Imgcodecs.imwrite("/Users/Karin.T/Documents/3pro/project_c/user3/input/trim"+String.valueOf(i)+".jpg",
										trim3);

								// 保存した画像を学習させる
								nearestNeighbor1.learn(3,
										Readimg("/Users/Karin.T/Documents/3pro/project_c/user3/input/trim"+String.valueOf(i)+".jpg"));
								}

								tracking.user3 = tracking.m_movie.m_filterFourier[2].clone();
								// テキストファイルに保存
								try {
									tracking.initial_filter_write(3, tracking.m_movie.m_filterFourier[2]);
								} catch (IOException e1) {
									e1.printStackTrace();
								}
								System.out.println("filter create!!");

								JLabel end = new JLabel("次に物体の軌跡の登録をします(10秒)");
								JOptionPane.showMessageDialog(frame, end);

							} else if (cnt > 13 && cnt < 34) {
								try {
									answer = tracking.m_movie.tracking(webcam_image, tracking.m_movie.m_filterFourier);
								} catch (IOException e) {
									e.printStackTrace();
								}
								// 保存

								Imgcodecs.imwrite("/Users/Karin.T/Documents/3pro/project_c/user3/tracking/"
										+ String.valueOf(cnt) + ".png", webcam_image[0]);

								tracking.dst3.add(answer[0]);
								tracking.dst3.add(answer[1]);

								try {
									File file = new File("/Users/Karin.T/Documents/3pro/project_c/user3/track3.txt");

									if (checkBeforeWritefile(file)) {
										FileWriter filewriter = new FileWriter(file, true);

										filewriter.write(answer[0] + "\n");
										filewriter.write(answer[1] + "\n");

										filewriter.close();
									} else {
										System.out.println("ファイルに書き込めません");
									}
								} catch (IOException e) {
									System.out.println(e);
								}

								System.out.println("tracking now!");
								System.out.println(answer[0]);
								System.out.println(answer[1]);

							} else if (cnt == 34) {
								timer.cancel();
								timer2.cancel();
								
								System.out.println(tracking.dst3);
								
								//軌道を学習
                                int[] data3 = new int[tracking.dst3.size()];
								
								for(int i=0; i<tracking.dst3.size();i++) {
									data3[i] = tracking.dst3.get(i);
								}
								
								nearestNeighbor2.learn(3, data3);
								
								cnt = 0;

								// 登録完了
								JLabel end = new JLabel("登録完了");
								JOptionPane.showMessageDialog(frame, end);
							}

							System.out.println(cnt);
							cnt++;
							webcam_image[0].convertTo(webcam_image[0], CvType.CV_8UC3);
							img = matToBufferedImage(webcam_image[0]);
							imgRev = createMirrorImage(img);// matからイメージに変換してから反転させる
							panel.setimage(imgRev);

						}
					};
					timer.schedule(task, 0, 500);


					TimerTask task2 = new TimerTask() {
						BufferedImage img;
						BufferedImage imgRev;

						public void run() {
							if (capture.isOpened()) {

								capture.read(webcam_image[0]);
								if (!webcam_image[0].empty()) {

									// 元々0.3で、0.6で大体画面いっぱい
									Imgproc.resize(webcam_image[0], webcam_image[0], new Size(
											webcam_image[0].size().width * 0.3, webcam_image[0].size().height * 0.3));
									frame.setSize(webcam_image[0].width() + 40, webcam_image[0].height() + 60);

									webcam_image2[0] = webcam_image[0].clone();
									// 四角描写
									tracking.m_movie.drawsquare(webcam_image2, webcam_image2[0].width() / 2,
											webcam_image2[0].height() / 2, m_width / 2, m_width / 2);
									img = matToBufferedImage(webcam_image2[0]);
									imgRev = createMirrorImage(img);// matからイメージに変換してから反転させる
									panel.setimage(imgRev);
									panel.repaint();

								} else {
									System.out.println(" --(!) No captured frame -- ");
								}
							}
						}

					};
					timer2.schedule(task2, 0, 500);
					
				} else {
					System.out.println(" --(!) No captured frame -- ");
				}
			}
		});

		open.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {

				JLabel msg = new JLabel("物体認証を開始します(3秒)。黒枠の中に物体を入れてください");
				JOptionPane.showMessageDialog(frame, msg);

				Mat[] filter = new Mat[3];

				Timer timer = new Timer(false);
				Timer timer2 = new Timer(false);
				
				TimerTask task = new TimerTask() {
					int cnt = 1;

					int ansnum = 0;
					int ansnum2 = 0;
					
					@Override
					public void run() {
						int[] answer = new int[2];

						if (cnt>1 && cnt < 7) {
							// 最初に3枚画像をとる
							Imgcodecs.imwrite(
									"/Users/Karin.T/Documents/3pro/project_c/open/" + String.valueOf(cnt) + ".png",
									webcam_image[0]);
							// 赤枠の画像保存
							Rect roi = new Rect(m_height / 2 - m_width / 4, m_width / 2 - m_width / 4, m_width / 2,
									m_width / 2);
							Mat trim = new Mat(webcam_image[0], roi);
							Imgcodecs.imwrite("/Users/Karin.T/Documents/3pro/project_c/open/trim.jpg", trim);

						} else if (cnt == 7) {
							// 物体認証
							// ここで保存されている画像と入力画像のRGB値を比較
							String openimg = "/Users/Karin.T/Documents/3pro/project_c/open/trim.jpg";
							ansnum = nearestNeighbor1.trial(Readimg(openimg));

							if (ansnum == 1 || ansnum == 2 || ansnum == 3) {
								JLabel end = new JLabel(
										"ユーザー" + String.valueOf(ansnum) + "  物体認証に成功しました。次に物体の軌跡を描いて下さい");
								JOptionPane.showMessageDialog(frame, end);
								JLabel msg = new JLabel("動かしてください");
								JOptionPane.showMessageDialog(frame, msg);
							} else {
								// falseだった場合、cntを1にして最初からやり直す
								JLabel end = new JLabel("もう一度やり直してください");
								JOptionPane.showMessageDialog(frame, end);
								cnt = 1;
							}

						} else if (cnt > 7 && cnt < 28) {
							// 動作認証（ここではトラッキングをして座標を取得）
							try {
								filter[0] = Mat.zeros(m_width, m_height, CvType.CV_64FC2);
								filter[1] = Mat.zeros(m_width, m_height, CvType.CV_64FC2);
								filter[2] = Mat.zeros(m_width, m_height, CvType.CV_64FC2);
								
								if (ansnum == 1) {
									filter[2] = tracking.user1.clone();
								} else if (ansnum == 2) {
									filter[2] = tracking.user2.clone();
								} else {// 3のみ
									filter[2] = tracking.user3.clone();
								}
								
								answer = tracking.m_movie.tracking(webcam_image, filter);

								tracking.src.add(answer[0]);
								tracking.src.add(answer[1]);

								System.out.println("x="+answer[0]);
								System.out.println("y="+answer[1]);

							} catch (IOException e) {
								e.printStackTrace();
							}

						} else if (cnt == 28) {
							// 登録した軌跡と、今回とった軌跡を比較.
							System.out.println(tracking.src);
							
                            int[] src = new int[tracking.src.size()];
							
							for(int i=0; i<tracking.src.size();i++) {
								src[i] = tracking.src.get(i);
							}
							
							ansnum2 = nearestNeighbor2.trial(src);
							
							System.out.println("軌跡　"+ansnum2);
							
							// trueの場合
							if (ansnum2 == ansnum) {
	
								JLabel end = new JLabel("動作認証に成功しました。ロックを解除します");
								JOptionPane.showMessageDialog(frame, end);
								
							} else {
								// falseの場合、「やり直してください」と出力してcntを8に設定し直して軌跡を取り直す
								JLabel end = new JLabel("やり直してください");
								JOptionPane.showMessageDialog(frame, end);
								cnt = 8;
							}
						} else if (cnt > 28) {
							// タイマー停止。解除終了
							timer.cancel();
							timer2.cancel();
							
							System.out.println("OPEN!!");
						}

						System.out.println(cnt);
						cnt++;
					}
				};
				timer.schedule(task, 0, 500);
				

				TimerTask task2 = new TimerTask() {
					BufferedImage img;
					BufferedImage imgRev;

					public void run() {
						if (capture.isOpened()) {

							capture.read(webcam_image[0]);
							if (!webcam_image[0].empty()) {

								// 元々0.3で、0.6で大体画面いっぱい
								Imgproc.resize(webcam_image[0], webcam_image[0], new Size(
										webcam_image[0].size().width * 0.3, webcam_image[0].size().height * 0.3));
								frame.setSize(webcam_image[0].width() + 40, webcam_image[0].height() + 60);

								webcam_image2[0] = webcam_image[0].clone();
								// 四角描写
								tracking.m_movie.drawsquare(webcam_image2, webcam_image2[0].width() / 2,
										webcam_image2[0].height() / 2, m_width / 2, m_width / 2);
								img = matToBufferedImage(webcam_image2[0]);
								imgRev = createMirrorImage(img);// matからイメージに変換してから反転させる
								panel.setimage(imgRev);
								panel.repaint();

							} else {
								System.out.println(" --(!) No captured frame -- ");
							}
						}
					}

				};
				timer2.schedule(task2, 0, 500);
			}
		});
		
		return;
	}
}
