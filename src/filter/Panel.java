package filter;



	
	import java.awt.*;  
	import java.awt.image.BufferedImage;
	import java.io.File;
	
	import javax.swing.*;  
	import org.opencv.core.Core;
	import org.opencv.core.Mat;
	import org.opencv.core.MatOfRect;
	import org.opencv.core.Point;
	import org.opencv.core.Rect;
	import org.opencv.core.Scalar;
	import org.opencv.core.Size;
	import org.opencv.imgcodecs.Imgcodecs;
	import org.opencv.videoio.*; 
	import org.opencv.imgproc.Imgproc;
	import org.opencv.objdetect.CascadeClassifier;
	//四角に関するインポート
	import java.applet.Applet;
	import java.awt.Graphics;
	import java.awt.Color;
	import org.opencv.core.Point;
	import org.opencv.core.Rect;
	import org.opencv.core.Scalar;
	import org.opencv.imgproc.Imgproc;
	
	public class Panel extends JPanel{  
		private static final long serialVersionUID = 1L;  
		private BufferedImage image;  
		// Create a constructor method  
		public Panel(){  
			super();  
		}  
		private BufferedImage getimage(){  
			return image;  
		}  
		void setimage(BufferedImage newimage){  
			image=newimage;  
			return;  
		}  
		/**  
		 * Converts/writes a Mat into a BufferedImage.  
		 *  
		 * @param matrix Mat of type CV_8UC3 or CV_8UC1  
		 * @return BufferedImage of type TYPE_3BYTE_BGR or TYPE_BYTE_GRAY  
		 */  
		public static BufferedImage matToBufferedImage(Mat matrix) {  
			int cols = matrix.cols();  
			int rows = matrix.rows();  
			int elemSize = (int)matrix.elemSize();  
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
				for(int i=0; i<data.length; i=i+3) {  
					b = data[i];  
					data[i] = data[i+2];  
					data[i+2] = b;  
				}  
				break;  
			default:  
				return null;  
			}  
			
			
			BufferedImage image2 = new BufferedImage(cols, rows, type);  
			image2.getRaster().setDataElements(0, 0, cols, rows, data);  
			return image2;  
			
		}
		
		public void paintComponent(Graphics g){  
			BufferedImage temp=getimage();
			if(temp!=null){
				g.drawImage(temp,20,20,temp.getWidth(),temp.getHeight(), this);  
			}
		} 
		
		//カメラの動画を反転させる
		public static BufferedImage createMirrorImage(BufferedImage temp){
			int width = temp.getWidth();
			int height = temp.getHeight();
			int size = width * height;
			int []buf = new int[ size ];
			temp.getRGB(0, 0, width, height, buf, 0, width);//イメージを配列に変換

			//bufのイメージ配列で、左右を変換する。
			int x1, x2, temp2;
			for(int y = 0; y < size; y+=width){
				x1 = 0;
				x2 = width -1;
				while(x1 < x2){// 交換の繰り返し
					temp2 = buf[y+x1];
					buf[y+x1++] = buf[y+x2];
					buf[y+x2--] = temp2;
				}
			}
			BufferedImage img2= new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
			img2.setRGB(0, 0, width, height, buf, 0, width);//配列をイメージに書き込む
			return img2;
		}
		 
		
		public static void main(String arg[]){  
			// Load the native library.  
			System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
			JFrame frame = new JFrame("BasicPanel");  
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  
			frame.setSize(400,400);  
			Panel panel = new Panel();  
			frame.setContentPane(panel);       
			frame.setVisible(true);       
			Mat webcam_image=new Mat();  
			BufferedImage img;  
			BufferedImage imgRev;  //反転したイメージ
			VideoCapture capture =new VideoCapture(0);  
	
			
			
			
			if( capture.isOpened())  
			{  
				while( true )  
				{  
	
					capture.read(webcam_image);  
					if( !webcam_image.empty() )  
					{  
						//元々0.3で、0.7で大体画面いっぱい
						Imgproc.resize(webcam_image, webcam_image, new Size(webcam_image.size().width*0.7,webcam_image.size().height*0.7));
						frame.setSize(webcam_image.width()+40,webcam_image.height()+60); 
						/*登録用中央に赤い四角を表示する。以下3行の座標はのちに反転されるため右上が(0,0)*/
						//四角の中心(635,375)
						Imgproc.rectangle(webcam_image,new Point(375, 75),new Point(975, 675),new Scalar(0, 0, 225),8,8,0);
						Imgproc.line(webcam_image,new Point(675, 75),new Point(675, 675),new Scalar(0, 0, 225));
						Imgproc.line(webcam_image,new Point(375, 375),new Point(975, 375),new Scalar(0, 0, 225));
						/*ここまでが四角描写なので登録時以外は消す*/
						img=matToBufferedImage(webcam_image); 
						imgRev = createMirrorImage(img);//matからイメージに変換してから反転させる
						

	                    panel.setimage(imgRev);  
	                   
						panel.repaint();  
					}  
					else  
					{  
						System.out.println(" --(!) No captured frame -- ");  
					}  
				}  
			}  
			return;  
		}
		
		
	}  

