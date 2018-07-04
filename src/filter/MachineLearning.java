package filter;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.swing.*;

public class MachineLearning extends JComponent implements ActionListener, MouseMotionListener
{
	
	Image img = new BufferedImage(300, 300, BufferedImage.TYPE_INT_RGB);
    Graphics bg = img.getGraphics();
    Point pt;
    LearningMachine learningMachine;
    int mesh = 10;

    public MachineLearning(String title, LearningMachine learningMachine) {
        this.learningMachine = learningMachine;
       JFrame f = new JFrame(title);
       f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
       f.setSize(320, 420);
       clear();
       f.add(this);
       
       JPanel p = new JPanel();
       p.setLayout(new GridLayout(2, 5));
       for(int i = 0; i < 10; ++i){
           JButton b = new JButton(i +"");
           p.add(b);
           b.addActionListener(this);
       }
       addMouseMotionListener(this);
       
       JButton b = new JButton("判定");
       b.addActionListener(this);
       f.add(b, BorderLayout.NORTH);
       
       f.add(p, BorderLayout.SOUTH);
       f.setVisible(true);
    }
    
    /** ボタンが押されたときの処理 */
    public void actionPerformed(ActionEvent e) {
        BufferedImage bi = new BufferedImage(mesh, mesh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = (Graphics2D) bi.getGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.drawImage(img, 0, 0, mesh, mesh, this);
        double[] data = new double[mesh * mesh];
        for(int x = 0; x < mesh; ++x){
            for(int y = 0; y < mesh; ++y){
                data[y * mesh + x] = (255 - bi.getRGB(x, y) & 255) / 255.;
            }
        }
        
        if("判定".equals(e.getActionCommand())){
            //判定
            int ans = learningMachine.trial(data);
            System.out.println("答えは" + ans);
        }else{
            //学習処理
            int idx = Integer.parseInt(e.getActionCommand());
            learningMachine.learn(idx, data);
        }
        clear();
    }

    /** マウスで描画 */
    public void mouseDragged(MouseEvent e) {
        Point old = pt;
        pt = e.getPoint();
        if(old != null){
            bg.setColor(Color.BLACK);
            ((Graphics2D)bg).setStroke(new BasicStroke(25));
            bg.drawLine(old.x, old.y, pt.x, pt.y);
            repaint();
        }
    }

    public void mouseMoved(MouseEvent e) {
        pt = null;
    }
    
    //描画
    @Override
    protected void paintComponent(Graphics g) {
        g.drawImage(img, 0, 0, this);
    }
    
    private void clear(){
        bg.setColor(Color.WHITE);
        bg.fillRect(0, 0, 300, 300);
        bg.setColor(Color.LIGHT_GRAY);
        ((Graphics2D)bg).setStroke(new BasicStroke(1));
        bg.drawRect(20, 20, 260, 260);
        bg.drawLine(0, 150, 300, 150);
        bg.drawLine(150, 0, 150, 300);
        repaint();
    }

}