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

import java.awt.Button;
import java.awt.Graphics;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class Tracking extends JPanel {
	// Movieクラスのインスタンス化
	static int m_width;
	static int m_height;
	static Movie m_movie;

	// 座標をユーザーごとに保存
	static ArrayList<Integer> dst1;
	static ArrayList<Integer> dst2;
	static ArrayList<Integer> dst3;
	static ArrayList<Integer> src;

	static // 初期フィルタ
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

	private static boolean checkBeforeWritefile(File file) {
		if (file.exists()) {
			if (file.isFile() && file.canWrite()) {
				return true;
			}
		}
		return false;
	}

	static int cnt = 0;

	// ユーザーの番号
	// static int usernum=0;

	// 画像読み込み
	public static double[] Readimg(String img) {

		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		// 画像の読み込み (第二引数を0にするとグレースケールで読み込み)
		Mat im = Imgcodecs.imread(img, 0); // 入力画像の取得

		int cols = im.cols();
		int rows = im.rows();
		int size = cols * rows;
		double[] data = new double[size];

		int y;
		int x;
		int count = 0;

		// rows(行)の数分回す
		for (y = 0; y < rows; y++) {

			// cols(列)の数だけ読み込む
			for (x = 0; x < cols; x++) {
				double[] d = im.get(y, x);
				data[count] = d[0];
				// System.out.println(data[count]);
				count++;

			}
		}
		return data;
	}

	static boolean lock = false;

	public static void main(String arg[]) {
		// Load the native library.
		m_width = 432;
		m_height = 768;

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
		nearestNeighbor1.learn(4, Readimg("/Users/kannomaki/Documents/3pro/prj/def/phone.png"));

		BufferedImage img;
		BufferedImage imgRev;

		VideoCapture capture = new VideoCapture(0);
		JButton button1 = new JButton("user1");
		JButton button2 = new JButton("user2");
		JButton button3 = new JButton("user3");
		JButton open = new JButton("open");

		// トラッキング（登録）
		Tracking tracking = new Tracking();
		ArrayList<Mat> input = new ArrayList<Mat>();

		frame.add(button1);
		frame.add(button2);
		frame.add(button3);
		frame.add(open);

		frame.setSize(webcam_image[0].width() + 40, webcam_image[0].height() + 60);

		button1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {

				JLabel msg = new JLabel("user1の物体の登録を開始します。(10秒間)");
				JOptionPane.showMessageDialog(frame, msg);
				Timer timer = new Timer(false);

				TimerTask task = new TimerTask() {
					int[] answer = new int[2];
					// int miss = 0;//3回間違えた場合は最初からやり直す

					@Override
					public void run() {
						if (cnt < 13) {
							Imgcodecs.imwrite(
									"/Users/Karin.T/Documents/3pro/project_c/user1/" + String.valueOf(cnt) + ".png",
									webcam_image[0]);
							input.add(webcam_image[0]);

							System.out.println("initial filter create....");
							System.out.println(cnt);
							// movie.get_filter_original(data1);
						} else if (cnt == 13) {
							try {
								// クラス変数m_filterFourierに分母、分子、フィルタを格納
								tracking.m_movie.makeFilter(input);
							} catch (IOException e) {
								// TODO 自動生成された catch ブロック
								e.printStackTrace();
							}

							// 赤枠内の画像をトリミングして保存
							Rect roi = new Rect(m_height / 2 - m_width / 4, m_width / 2 - m_width / 4, m_width / 2,
									m_width / 2);
							Mat trim1 = new Mat(input.get(4), roi);
							Imgcodecs.imwrite("/Users/Karin.T/Documents/3pro/project_c/user1/trim1.jpg", trim1);
							tracking.user1 = tracking.m_movie.m_filterFourier[2].clone();
							System.out.println("filter create!!");

							// デバッグ用

							// 入力画像をフーリエ変換しやすいように変換後、フーリエ変換
							Mat[] input = new Mat[1];
							webcam_image[0].convertTo(webcam_image[0], CvType.CV_32FC3);
							input[0] = Mat.zeros(m_width, m_height, CvType.CV_32FC3);
							input[0] = webcam_image[0].clone();

							Mat[] ans_input = new Mat[1];
							ans_input[0] = Mat.zeros(m_width, m_height, CvType.CV_64FC2);

							// Mat output = Mat.zeros(width, height, CvType.CV_32FC3);
							try {
								m_movie.m_filter.toFourier(input, ans_input);
							} catch (IOException e) {
								// TODO 自動生成された catch ブロック
								e.printStackTrace();
							}

							ArrayList<Mat> planes2 = new ArrayList<Mat>();
							Mat output = Mat.zeros(m_width, m_height, CvType.CV_64FC2);

							m_movie.m_filterFourier[2].convertTo(m_movie.m_filterFourier[2], CvType.CV_64F);
							Core.mulSpectrums(ans_input[0], tracking.m_movie.m_filterFourier[2], output, 0);

							Core.idft(output, output);
							Mat restoredImage = Mat.zeros(m_width, m_height, CvType.CV_64FC1);// 0で初期化
							Core.split(output, planes2);
							Core.normalize(planes2.get(0), restoredImage, 0, 255, Core.NORM_MINMAX);

							Imgcodecs.imwrite("/Users/Karin.T/Documents/3pro/project_c/user1/debug.jpg", restoredImage);
							System.out.println("done!");
							// デバッグ終了→少しぼけているけど中心に一番反応している

							JLabel end = new JLabel("次に物体の軌跡の登録をします(10秒)");
							JOptionPane.showMessageDialog(frame, end);

						} else if (cnt > 13 && cnt < 34) {
							try {
								answer = tracking.m_movie.tracking(webcam_image, m_movie.m_filterFourier);
							} catch (IOException e) {
								// TODO 自動生成された catch ブロック
								e.printStackTrace();
							}
							// tracking.m_movie.drawsquare(webcam_image, answer[1], answer[0], m_width / 2,
							// m_width / 2);
							// 保存

							dst1.add(answer[0]);
							dst1.add(answer[1]);

							try {
								File file = new File("/Users/Karin.T/Documents/3pro/project_c/user1/track1.txt");

								if (checkBeforeWritefile(file)) {
									FileWriter filewriter = new FileWriter(file);

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
							System.out.println(cnt);
							System.out.println(answer[0]);
							System.out.println(answer[1]);

						} else if (cnt == 34) {
							timer.cancel();
							cnt = 0;
							System.out.println(dst1);
							System.out.println("exit");
						}
						cnt++;
					}
				};
				timer.schedule(task, 0, 700);
			}

		});

		button2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {

				lock = true;

				int count = 0;
				Mat[] data = new Mat[300];
				int[] answer = new int[2];
				BufferedImage img;
				BufferedImage imgRev;

				JLabel msg = new JLabel("user2の物体の登録を開始します。(約10秒間)");
				JOptionPane.showMessageDialog(frame, msg);

				if (capture.isOpened()) {

					while (true) {

						capture.read(webcam_image[0]);

						if (!webcam_image[0].empty()) {
							// 元々0.3で、0.6で大体画面いっぱい
							Imgproc.resize(webcam_image[0], webcam_image[0],
									new Size(webcam_image[0].size().width * 0.4, webcam_image[0].size().height * 0.4));
							frame.setSize(webcam_image[0].width() + 40, webcam_image[0].height() + 60);

							webcam_image2[0] = webcam_image[0].clone();// 表示用

							if (count < 40) {// 初期フィルタを作成するための入力画像を得る
								data[count] = Mat.zeros(m_width, m_height, CvType.CV_64FC3);
								data[count] = webcam_image[0].clone();

								Imgcodecs.imwrite("/Users/Karin.T/Documents/3pro/project_c/user2/input/"
										+ String.valueOf(count) + ".png", webcam_image2[0]);

								m_movie.drawsquare(webcam_image2, webcam_image2[0].width() / 2,
										webcam_image2[0].height() / 2, m_width / 2, m_width / 2);

								System.out.println("initial filter create....");
							} else {
								try {
									if (count == 40) {
										m_movie.get_filter_original(data);// クラス変数ArrayListのm_inputにフィルタ作成に用いる入力画像を格納する
										m_movie.makeFilter(m_movie.m_input);// クラス変数m_filterFourierに分母、分子、フィルタを格納
										// movie.new_makeFilter(m_filterFourier, webcam_image);

										// 赤枠内の画像をトリミングして保存
										Rect roi = new Rect(m_height / 2 - m_width / 4, m_width / 2 - m_width / 4,
												m_width / 2, m_width / 2);
										Mat trim2 = new Mat(data[3], roi);
										Imgcodecs.imwrite("/Users/Karin.T/Documents/3pro/project_c/user2/trim2.jpg",
												trim2);
										tracking.user2 = tracking.m_movie.m_filterFourier[2].clone();
										System.out.println("filter create!!");

										// デバッグ用

										// 入力画像をフーリエ変換しやすいように変換後、フーリエ変換
										/*
										 * Mat[] input = new Mat[1]; webcam_image[0].convertTo(webcam_image[0],
										 * CvType.CV_32FC3); input[0] = Mat.zeros(m_width, m_height, CvType.CV_32FC3);
										 * input[0] = webcam_image[0].clone();
										 * 
										 * Mat[] ans_input = new Mat[1]; ans_input[0] = Mat.zeros(m_width, m_height,
										 * CvType.CV_64FC2);
										 * 
										 * // Mat output = Mat.zeros(width, height, CvType.CV_32FC3); try {
										 * m_movie.m_filter.toFourier(input, ans_input); } catch (IOException e) { //
										 * TODO 自動生成された catch ブロック e.printStackTrace(); }
										 * 
										 * ArrayList<Mat> planes2 = new ArrayList<Mat>(); Mat output =
										 * Mat.zeros(m_width, m_height, CvType.CV_64FC2);
										 * 
										 * m_movie.m_filterFourier[2].convertTo(m_movie.m_filterFourier[2],
										 * CvType.CV_64F); Core.mulSpectrums(ans_input[0],
										 * tracking.m_movie.m_filterFourier[2], output, 0);
										 * 
										 * Core.idft(output, output); Mat restoredImage = Mat.zeros(m_width, m_height,
										 * CvType.CV_64FC1);// 0で初期化 Core.split(output, planes2);
										 * Core.normalize(planes2.get(0), restoredImage, 0, 255, Core.NORM_MINMAX);
										 * 
										 * Imgcodecs.imwrite("/Users/Karin.T/Documents/3pro/project_c/user2/debug.jpg",
										 * restoredImage); System.out.println("done!");
										 */
										// デバッグ終了

										JLabel end = new JLabel("次に物体の軌跡の登録をします(10秒)");
										JOptionPane.showMessageDialog(frame, end);
									} else if (count > 40 && count < 60) {
										// フィルタを更新
										// movie.new_makeFilter(m_updatefilter, webcam_image);
										try {
											answer = tracking.m_movie.tracking(webcam_image, m_movie.m_filterFourier);
										} catch (IOException e) {
											// TODO 自動生成された catch ブロック
											e.printStackTrace();
										}
										// tracking.m_movie.drawsquare(webcam_image, answer[1], answer[0], m_width / 2,
										// m_width / 2);
										// 保存

										dst2.add(answer[0]);
										dst2.add(answer[1]);

										try {
											File file = new File(
													"/Users/Karin.T/Documents/3pro/project_c/user2/input/track2.txt");

											if (checkBeforeWritefile(file)) {
												FileWriter filewriter = new FileWriter(file);

												filewriter.write(answer[0] + "\n");
												filewriter.write(answer[1] + "\n");

												filewriter.close();
											} else {
												System.out.println("ファイルに書き込めません");
											}
										} catch (IOException e) {
											System.out.println(e);
										}

										Imgcodecs.imwrite("//Users/Karin.T/Documents/3pro/project_c/user2/tracking/"
												+ String.valueOf(count) + ".png", webcam_image[0]);
										m_movie.drawsquare(webcam_image2, answer[1], answer[0], m_width / 2,
												m_width / 2);
										System.out.println("tracking now!");
										// System.out.println(answer[0]);
										// System.out.println(answer[1]);
									}
								} catch (IOException e) {
									e.printStackTrace();
								}
							}

							System.out.println(count);
							count++;

							System.out.println(count + "after");

							panel.repaint();

						} else {
							System.out.println(" --(!) No captured frame -- ");
						}

						// System.out.println(cnt);
						// cnt++;
						webcam_image2[0].convertTo(webcam_image2[0], CvType.CV_8UC3);
						img = matToBufferedImage(webcam_image2[0]);
						imgRev = createMirrorImage(img);// matからイメージに変換してから反転させる
						panel.setimage(imgRev);

						if (count == 60) {
							lock = false;
							System.out.println("登録完了!!");
							break;
						}

					}
				}
			}
		});

		button3.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				JLabel msg = new JLabel("user3の物体の登録を開始します。(15秒間)");
				JOptionPane.showMessageDialog(frame, msg);

				if (capture.isOpened()) {

					Timer timer = new Timer(false);

					TimerTask task = new TimerTask() {

						File file = new File("/Users/Karin.T/Documents/3pro/project_c/user3/track3.txt");
						int[] answer = new int[2];
						// int miss = 0;//3回間違えた場合は最初からやり直す

						BufferedImage img;
						BufferedImage imgRev; // 反転したイメージ

						@Override
						public void run() {

							capture.read(webcam_image[0]);
							Imgproc.resize(webcam_image[0], webcam_image[0],
									new Size(webcam_image[0].size().width * 0.4, webcam_image[0].size().height * 0.4));
							frame.setSize(webcam_image[0].width() + 40, webcam_image[0].height() + 60);

							if (cnt < 13) {
								Imgcodecs.imwrite("/Users/Karin.T/Documents/3pro/project_c/user3/input/"
										+ String.valueOf(cnt) + ".png", webcam_image[0]);
								if (webcam_image[0].size().width != m_width
										|| webcam_image[0].size().height != m_height) {
									Imgproc.resize(webcam_image[0], webcam_image[0], new Size(
											webcam_image[0].size().width * 0.6, webcam_image[0].size().height * 0.6));
								}
								input.add(webcam_image[0]);

								System.out.println("initial filter create....");
								// movie.get_filter_original(data1);
							} else if (cnt == 13) {
								try {
									// クラス変数m_filterFourierに分母、分子、フィルタを格納
									tracking.m_movie.makeFilter(input);
								} catch (IOException e) {
									// TODO 自動生成された catch ブロック
									e.printStackTrace();
								}

								// 赤枠内の画像をトリミングして保存
								Rect roi = new Rect(m_height / 2 - m_width / 4, m_width / 2 - m_width / 4, m_width / 2,
										m_width / 2);
								Mat trim3 = new Mat(input.get(4), roi);

								Imgcodecs.imwrite("/Users/Karin.T/Documents/3pro/project_c/user3/input/trim3.jpg",
										trim3);

								// 保存した画像を学習させる
								nearestNeighbor1.learn(3,
										Readimg("/Users/Karin.T/Documents/3pro/project_c/user3/input/trim3.jpg"));

								// デバッグ
								// System.out.println("学習させました");

								tracking.user3 = tracking.m_movie.m_filterFourier[2].clone();
								System.out.println("filter create!!");

								// デバッグ用
								/*
								 * // 入力画像をフーリエ変換しやすいように変換後、フーリエ変換 Mat[] input = new Mat[1];
								 * webcam_image[0].convertTo(webcam_image[0], CvType.CV_32FC3); input[0] =
								 * Mat.zeros(m_width, m_height, CvType.CV_32FC3); input[0] =
								 * webcam_image[0].clone();
								 * 
								 * Mat[] ans_input = new Mat[1]; ans_input[0] = Mat.zeros(m_width, m_height,
								 * CvType.CV_64FC2);
								 * 
								 * // Mat output = Mat.zeros(width, height, CvType.CV_32FC3); try {
								 * m_movie.m_filter.toFourier(input, ans_input); } catch (IOException e) { //
								 * TODO 自動生成された catch ブロック e.printStackTrace(); }
								 * 
								 * ArrayList<Mat> planes2 = new ArrayList<Mat>(); Mat output =
								 * Mat.zeros(m_width, m_height, CvType.CV_64FC2);
								 * 
								 * m_movie.m_filterFourier[2].convertTo(m_movie.m_filterFourier[2],
								 * CvType.CV_64F); Core.mulSpectrums(ans_input[0],
								 * tracking.m_movie.m_filterFourier[2], output, 0);
								 * 
								 * Core.idft(output, output); Mat restoredImage = Mat.zeros(m_width, m_height,
								 * CvType.CV_64FC1);// 0で初期化 Core.split(output, planes2);
								 * Core.normalize(planes2.get(0), restoredImage, 0, 255, Core.NORM_MINMAX);
								 * 
								 * Imgcodecs.imwrite("/Users/Karin.T/Documents/3pro/project_c/user3/debug.jpg",
								 * restoredImage); System.out.println("done!");
								 */
								// デバッグ終了→少しぼけているけど中心に一番反応している

								JLabel end = new JLabel("次に物体の軌跡の登録をします(10秒)");
								JOptionPane.showMessageDialog(frame, end);

							} else if (cnt > 13 && cnt < 34) {
								try {
									answer = tracking.m_movie.tracking(webcam_image, m_movie.m_filterFourier);
								} catch (IOException e) {
									// TODO 自動生成された catch ブロック
									e.printStackTrace();
								}
								// tracking.m_movie.drawsquare(webcam_image, answer[1], answer[0], m_width / 2,
								// m_width / 2);
								// 保存

								Imgcodecs.imwrite("/Users/Karin.T/Documents/3pro/project_c/user3/tracking/"
										+ String.valueOf(cnt) + ".png", webcam_image[0]);

								dst3.add(answer[0]);
								dst3.add(answer[1]);

								try {

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
								cnt = 0;
								System.out.println(dst3);
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
					timer.schedule(task, 0, 700);

					// ここで軌道を学習させる
					nearestNeighbor2.learn(3, dst3);

				} else {
					System.out.println(" --(!) No captured frame -- ");
				}
			}
		});

		open.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				// 画像2,3まいとって、色が違ったら認証できません 見たことある色だったら 軌跡を描いてください。
				JLabel msg = new JLabel("物体認証を開始します(3秒)");
				JOptionPane.showMessageDialog(frame, msg);

				Mat[] filter = new Mat[3];

				Timer timer = new Timer(false);
				TimerTask task = new TimerTask() {
					int cnt = 1;

					@Override
					public void run() {
						int[] answer = new int[2];
						if (cnt < 4) {
							// 最初に3枚画像をとる
							Imgcodecs.imwrite(
									"/Users/Karin.T/Documents/3pro/project_c/open/" + String.valueOf(cnt) + ".png",
									webcam_image[0]);
							// 赤枠の画像保存
							Rect roi = new Rect(m_height / 2 - m_width / 4, m_width / 2 - m_width / 4, m_width / 2,
									m_width / 2);
							Mat trim = new Mat(webcam_image[0], roi);
							Imgcodecs.imwrite("/Users/Karin.T/Documents/3pro/project_c/open/trim.jpg", trim);

						} else if (cnt == 4) {
							// 物体認証

							// ここで保存されている画像と入力画像のRGB値を比較（どのユーザーか値等を返してくれるとありがたい）
							String openimg = "/Users/Karin.T/Documents/3pro/project_c/open/trim.jpg";
							int ansnum = nearestNeighbor1.trial(Readimg(openimg));

							// デバッグ
							System.out.println(ansnum);

							if (ansnum == 1 || ansnum == 2 || ansnum == 3) {
								JLabel end = new JLabel("物体認証に成功しました。次に物体の軌跡を描いて下さい");
								JOptionPane.showMessageDialog(frame, end);
								JLabel msg = new JLabel("動かしてください");
								JOptionPane.showMessageDialog(frame, msg);
							} else {
								// falseだった場合、「もう一度やり直してください」と表示し、cntを1にして最初からやり直す
								JLabel end = new JLabel("もう一度やり直してください");
								JOptionPane.showMessageDialog(frame, end);
								cnt = 1;
							}

						} else if (cnt > 4 && cnt < 25) {
							// 動作認証（ここではトラッキングをして座標を取得）
							try {
								// if文でどのフィルターをcloneするか分ける（今はuser1）
								filter[2] = Mat.zeros(m_width, m_height, CvType.CV_64FC2);
								filter[2] = user1.clone();
								answer = tracking.m_movie.tracking(webcam_image, filter);

								// 四角描写
								// tracking.m_movie.drawsquare(webcam_image, answer[1], answer[0], m_width / 2,
								// m_width / 2);

								src.add(answer[0]);
								src.add(answer[1]);

								System.out.println(answer[0]);
								System.out.println(answer[1]);

							} catch (IOException e) {
								// TODO 自動生成された catch ブロック
								e.printStackTrace();
							}

						} else if (cnt == 25) {
							// ここで登録した軌跡と、今回とった軌跡を比較.
							int ansnum2 = nearestNeighbor2.trial(src);
							System.out.println(ansnum2);

							// trueの場合
							if (ansnum2 != 0) {
								JLabel end = new JLabel("動作認証に成功しました。ロックを解除します");
								JOptionPane.showMessageDialog(frame, end);
							} else {
								// falseの場合、「やり直してください」と出力してcntを5に設定し直して軌跡を取り直す
								JLabel end = new JLabel("やり直してください");
								JOptionPane.showMessageDialog(frame, end);
								cnt = 5;
							}
						} else if (cnt > 25) {
							// タイマー停止。解除終了
							timer.cancel();
							System.out.println("OPEN!!");
						}
						System.out.println(cnt);
						cnt++;
					}
				};
				timer.schedule(task, 0, 700);

			}
		});

		if (capture.isOpened()) {
			while (true) {

				capture.read(webcam_image[0]);
				if (!webcam_image[0].empty()) {
					
					// 元々0.3で、0.6で大体画面いっぱい
					Imgproc.resize(webcam_image[0], webcam_image[0],
							new Size(webcam_image[0].size().width * 0.4, webcam_image[0].size().height * 0.4));
					frame.setSize(webcam_image[0].width() + 40, webcam_image[0].height() + 60);
					// Button obj = new Button();
					// obj.count++;
					// img2=matToBufferedImage(webcam_image[0]);

					webcam_image2[0] = webcam_image[0].clone();
					// 四角描写
					m_movie.drawsquare(webcam_image2, webcam_image2[0].width() / 2, webcam_image2[0].height() / 2,
							m_width / 2, m_width / 2);
					img = matToBufferedImage(webcam_image2[0]);
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
