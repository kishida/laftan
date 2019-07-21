package kis.laftanmv;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
//import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileFilter;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;
import kis.laftanmv.bezier.FittingCurve;

/*
 * LafPaintFrame.java
 *
 * Created on 2007/08/12, 19:49
 */

/**
 *
 * @author  kishida
 */
public class LafPaintFrame extends javax.swing.JFrame {
    Color selectedColor;
    float strength = 1;
    
    Map<String, Draw> actions = new HashMap<String, Draw>();
    String mode = "LINE";
    
    Action saveAction = new AbstractAction("保存"){
        public void actionPerformed(ActionEvent e) {
            saveActionExec();
        }
    };
    Action undoAction = new AbstractAction("元に戻す"){
        public void actionPerformed(ActionEvent e) {
            undoMan.undo();
            undoUpdate();
        }
        
    };
    Action redoAction = new AbstractAction("再実行"){
        public void actionPerformed(ActionEvent e) {
            undoMan.redo();
            undoUpdate();
        }
    };
    
    UndoManager undoMan = new UndoManager();
    
    void undoUpdate(){
        redoAction.setEnabled(undoMan.canRedo());
        undoAction.setEnabled(undoMan.canUndo());
    }
    
    //Image img;
    /** Creates new form LafPaintFrame */
    public LafPaintFrame() {
        initComponents();
        add(canvas);
        
        /*
        try {
            img = ImageIO.read(new File("D:\\kishida\\java\\trial\\nb6m10\\Laftan\\laf.png"));
        } catch (IOException e) {
        }*/
        
        undoMan.setLimit(30);
        saveAction.putValue(Action.ACCELERATOR_KEY, 
                KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
        btnSave.setAction(saveAction);
        miSave.setAction(saveAction);
        
        undoAction.putValue(Action.ACCELERATOR_KEY, 
                KeyStroke.getKeyStroke(KeyEvent.VK_Z, ActionEvent.CTRL_MASK));
        btnUndo.setAction(undoAction);
        miUndo.setAction(undoAction);

        redoAction.putValue(Action.ACCELERATOR_KEY, 
                KeyStroke.getKeyStroke(KeyEvent.VK_Y, ActionEvent.CTRL_MASK));
        btnRedo.setAction(redoAction);
        miRedo.setAction(redoAction);
        
        undoUpdate();
        
        //色設定ボタン
        Color[] colors = {
            Color.BLACK, Color.GRAY, Color.LIGHT_GRAY, Color.WHITE,
            Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW,Color.ORANGE
        };
        boolean first = true;
        for(Color col : colors){
            JToggleButton tb = new JToggleButton();
            tb.setBackground(col);
            tb.setText("   ");
            tb.setMinimumSize(new Dimension(32, 32));
            tb.setPreferredSize(new Dimension(32, 32));
            bgColor.add(tb);
            tbColor.add(tb);
            if(first){
                tb.setSelected(true);
                selectedColor = tb.getBackground();
                first = false;
            }
            tb.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent ae) {
                    selectColor(((JToggleButton)ae.getSource()).getBackground());
                }
            });
        }
        
        actions.put("LINE", new DrawLine());
        actions.put("FILL", new DrawFillLine());
        actions.put("ERASE", new Erase());
        canvas.addMouseListener(new MouseListener(){

            public void mouseClicked(MouseEvent e) {
            }

            public void mousePressed(MouseEvent e) {
                Draw d = actions.get(getMode());
                if(d == null) return;
                d.canvasMousePressed(e);
            }

            public void mouseReleased(MouseEvent e) {
                Draw d = actions.get(getMode());
                if(d == null) return;
                d.canvasMouseReleased(e);
            }

            public void mouseEntered(MouseEvent e) {
                Draw d = actions.get(getMode());
                if(d == null) return;
                d.canvasMouseEntered(e);
            }

            public void mouseExited(MouseEvent e) {
                Draw d = actions.get(getMode());
                if(d == null) return;
                d.canvasMouseExited(e);
            }

        });
        canvas.addMouseMotionListener(new MouseMotionListener(){
            public void mouseDragged(MouseEvent e) {
                Draw d = actions.get(getMode());
                if(d == null) return;
                d.canvasMouseDragged(e);
            }

            public void mouseMoved(MouseEvent e) {
            }
        });
        
    }
    
    String getMode(){
        return bgMode.getSelection().getActionCommand();
    }
    
    void selectColor(Color col){
        selectedColor = col;
    }
    
    float selectedStrength(){
        return Float.parseFloat(bgStrength.getSelection().getActionCommand());
    }
    
    MyCanvas canvas = new MyCanvas();
    class MyCanvas extends JComponent{
        @Override
        protected void paintComponent(Graphics g1) {
            Graphics2D g = (Graphics2D) g1;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, getWidth(), getHeight());
            //if(img != null) g.drawImage(img, 0, 0, canvas);
            for(Freeline fl : fills){
                fl.draw(g);
            }
            if("FILL".equals(bgMode.getSelection().getActionCommand())){
                if(drawingLine != null){
                    drawingLine.draw(g);
                    if(candidate != null){
                        //g.setColor(Color.GREEN);
                        g.draw(candidate);
                    }
                }
            }
            for(Freeline fl : lines){
                fl.draw(g);
            }
            if(!"FILL".equals(bgMode.getSelection().getActionCommand())){
                if(drawingLine != null){
                    drawingLine.draw(g);
                    if(candidate != null){
                        //g.setColor(Color.GREEN);
                        g.draw(candidate);
                    }
                }
            }
        }
    }

    class Freeline{
        GeneralPath gp = new GeneralPath();
        float strength;
        Color color;
        void draw(Graphics2D g){
            g.setStroke(new BasicStroke(strength, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(color);
            g.draw(gp);
        }
        Shape range;
        Shape getRange(){
            if(range == null){
                float s = strength;
                if(s < 5) s = 5;
                range = new BasicStroke(s, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND).createStrokedShape(gp);
            }
            return range;
        }
        Rectangle rect;
        boolean isHit(Point2D p){
            Shape s = getRange();
            if(rect == null){
                rect = s.getBounds();
            }
            if(!rect.contains(p)){
                return false;
            }
            return s.contains(p);
        }
    }
    
    abstract class UndoableDrawAction implements UndoableEdit{
        public boolean canUndo() {
            return true;
        }
        public boolean canRedo() {
            return true;
        }
        public void die() {
            //throw new UnsupportedOperationException("Not supported yet.");
        }
        public boolean addEdit(UndoableEdit anEdit) {
            return false;
        }
        public boolean replaceEdit(UndoableEdit anEdit) {
            return false;
        }
        public boolean isSignificant() {
            return true;
        }
    };
    
    class LineUndo extends UndoableDrawAction{
        Freeline fl;

        public LineUndo(kis.laftanmv.LafPaintFrame.Freeline fl) {
            this.fl = fl;
        }
        
        public void undo() throws CannotUndoException {
            lines.remove(fl);
            canvas.repaint();
        }
        public void redo() throws CannotRedoException {
            lines.add(fl);
            canvas.repaint();
        }
        public String getPresentationName() {
            return "線の取り消し";
        }
        public String getUndoPresentationName() {
            return "線を取り消す";
        }
        public String getRedoPresentationName() {
            return "線を再実行";
        }
    }
    
    class FillUndo extends UndoableDrawAction{
        Freeline fl;

        public FillUndo(kis.laftanmv.LafPaintFrame.Freeline fl) {
            this.fl = fl;
        }
        
        public void undo() throws CannotUndoException {
            fills.remove(fl);
            canvas.repaint();
        }

        public void redo() throws CannotRedoException {
            fills.add(fl);
            canvas.repaint();
        }

        public String getPresentationName() {
            return "塗りの取り消し";
        }

        public String getUndoPresentationName() {
            return "塗りを取り消す";
        }

        public String getRedoPresentationName() {
            return "塗りを再実行";
        }
    }
    
    class EraseUndo extends UndoableDrawAction{
        List<Freeline> erasedLine;
        List<Freeline> erasedFill;

        public EraseUndo(List<Freeline> erasedLine, List<Freeline> erasedFill) {
            this.erasedLine = erasedLine;
            this.erasedFill = erasedFill;
        }
        
        public void undo() throws CannotUndoException {
            for(Freeline fl : erasedLine){
                lines.add(fl);
            }
            for(Freeline fl : erasedFill){
                fills.add(fl);
            }
            canvas.repaint();
        }

        public void redo() throws CannotRedoException {
            for(Freeline fl : erasedLine){
                lines.remove(fl);
            }
            for(Freeline fl : erasedFill){
                fills.remove(fl);
            }
            canvas.repaint();
        }

        public String getPresentationName() {
            return "消しゴムの取り消し";
        }

        public String getUndoPresentationName() {
            return "消しゴムを取り消す";
        }

        public String getRedoPresentationName() {
            return "消しゴムの再実行";
        }
        
    }
    
    List<Freeline> lines = new ArrayList<Freeline>();
    List<Freeline> fills = new ArrayList<Freeline>();
    GeneralPath candidate;
    Freeline drawingLine;

    void addUndoEdit(UndoableEdit edit){
        undoMan.addEdit(edit);
        undoUpdate();
    }
    
    class DrawFillLine extends DrawFreeLine{
        void draw(kis.laftanmv.LafPaintFrame.Freeline line) {
            fills.add(line);
            UndoableEdit edit = new FillUndo(line);
            addUndoEdit(edit);
        }
    }
    
    class DrawLine extends DrawFreeLine{
        void draw(kis.laftanmv.LafPaintFrame.Freeline line) {
            lines.add(line);
            UndoableEdit edit = new LineUndo(line);
            addUndoEdit(edit);
        }
    }
    
    abstract class DrawFreeLine extends Draw{
        List<Point2D> pointList;
        
        abstract void draw(Freeline line);
        
        @Override
        void canvasMousePressed(MouseEvent me){
            pointList = new ArrayList<Point2D>();
            pointList.add(new Point2D.Double(me.getPoint().x, me.getPoint().y));
            drawingLine = new Freeline();
            drawingLine.color = selectedColor;
            drawingLine.strength = selectedStrength();
            canvas.repaint();
        }
        @Override
        void canvasMouseReleased(MouseEvent me){
            if(candidate != null){
                drawingLine.gp.append(candidate, true);
            }
            draw(drawingLine);
            drawingLine = null;
            candidate = null;
            pointCount = 0;
            tan = new  Point2D.Double();
            canvas.repaint();
        }

        int pointCount = 0;
        Point2D tan = new Point2D.Double();
        static final double PHAI = 10;
        @Override
        void canvasMouseDragged(MouseEvent me){
            pointList.add(new Point2D.Double(me.getPoint().getX(), me.getPoint().getY()));
            Point2D[] cps = pointList.toArray(new Point2D[0]);
            candidate = new GeneralPath();
            candidate.moveTo((float)cps[0].getX(), (float)cps[0].getY());
            int[] count = {0};
            Point2D tantemp = (Point2D) tan.clone();
            int n = FittingCurve.fitCurve(candidate, cps, PHAI, count, tantemp, true);
            if(n < pointCount || count[0] > 5){
                for(int j = 0; j < count[0] - 1; ++j){
                    cps = pointList.toArray(new Point2D[0]);
                    GeneralPath commit = new GeneralPath();
                    commit.moveTo((float)cps[0].getX(), (float)cps[0].getY());
                    //count[0] = 0;
                    int c = FittingCurve.fitCurve(commit, cps, PHAI, null, tan, false);
                    drawingLine.gp.append(commit, true);
                    for(int i = 0; i < c; ++i) pointList.remove(0);
                }
                //再計算
                cps = pointList.toArray(new Point2D[0]);
                candidate = new GeneralPath();
                candidate.moveTo((float)cps[0].getX(), (float)cps[0].getY());
                count[0] = 0;
                tantemp = (Point2D) tan.clone();
                pointCount = FittingCurve.fitCurve(candidate, cps, PHAI, count, tantemp, true);
            }else{
                pointCount = n;
            }
            canvas.repaint();
        }

    }
    class Draw{
        void canvasMousePressed(MouseEvent me){
        }
        void canvasMouseReleased(MouseEvent me){
        }
        void canvasMouseDragged(MouseEvent me){
        }
        void canvasMouseEntered(MouseEvent me) {
        }
        void canvasMouseExited(MouseEvent me) {
        }
    }
    
    class Erase extends Draw{
        List<Freeline> ll = new ArrayList<LafPaintFrame.Freeline>();
        List<Freeline> ff = new ArrayList<LafPaintFrame.Freeline>();

        @Override
        void canvasMousePressed(MouseEvent me) {
            ll = new ArrayList<LafPaintFrame.Freeline>();
            ff = new ArrayList<LafPaintFrame.Freeline>();
        }

        @Override
        void canvasMouseReleased(MouseEvent me) {
            if(ll.size() > 0 || ff.size() > 0){
                UndoableEdit edit = new EraseUndo(ll, ff);
                addUndoEdit(edit);
            }
            
            ll = null;
            ff = null;
        }
        
        
        @Override
        void canvasMouseDragged(MouseEvent me) {
            Point p = me.getPoint();
            for(Freeline fl : lines){
                if(fl.isHit(p)){
                    ll.add(fl);
                }
            }
            for(Freeline fl : ll){
                lines.remove(fl);
            }
            
            for(Freeline fl : fills){
                if(fl.isHit(p)){
                    ff.add(fl);
                }
            }
            for(Freeline fl : ff){
                fills.remove(fl);
            }
            
            canvas.repaint();
        }
        
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {

        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        miSave = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        miUndo = new javax.swing.JMenuItem();
        miRedo = new javax.swing.JMenuItem();
        bgMode = new javax.swing.ButtonGroup();
        bgStrength = new javax.swing.ButtonGroup();
        bgColor = new javax.swing.ButtonGroup();
        jToggleButton1 = new javax.swing.JToggleButton();
        jPanel1 = new javax.swing.JPanel();
        jToolBar1 = new javax.swing.JToolBar();
        btnSave = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JToolBar.Separator();
        jToggleButton2 = new javax.swing.JToggleButton();
        jToggleButton3 = new javax.swing.JToggleButton();
        jToggleButton4 = new javax.swing.JToggleButton();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        jToggleButton9 = new javax.swing.JToggleButton();
        jToggleButton5 = new javax.swing.JToggleButton();
        jToggleButton6 = new javax.swing.JToggleButton();
        jToggleButton7 = new javax.swing.JToggleButton();
        jToggleButton8 = new javax.swing.JToggleButton();
        jSeparator3 = new javax.swing.JToolBar.Separator();
        btnUndo = new javax.swing.JButton();
        btnRedo = new javax.swing.JButton();
        tbColor = new javax.swing.JToolBar();
        btnColor = new javax.swing.JButton();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        miSave = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        miUndo = new javax.swing.JMenuItem();
        miRedo = new javax.swing.JMenuItem();

        jMenu1.setText("File");

        miSave.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        miSave.setText("Item");
        jMenu1.add(miSave);

        jMenuBar1.add(jMenu1);

        jMenu2.setText("Edit");

        miUndo.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, java.awt.event.InputEvent.CTRL_MASK));
        miUndo.setText("Item");
        jMenu2.add(miUndo);

        miRedo.setText("Item");
        jMenu2.add(miRedo);

        jMenuBar1.add(jMenu2);

        setJMenuBar(jMenuBar1);

        bgMode.add(jToggleButton1);
        jToggleButton1.setText("\u9078\u629e");
        jToggleButton1.setEnabled(false);
        jToggleButton1.setFocusable(false);
        jToggleButton1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jToggleButton1.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("\u3089\u3075\u305f\u3093");

        jPanel1.setLayout(new java.awt.GridLayout(0, 1));

        jToolBar1.setRollover(true);

        btnSave.setText("\u4fdd\u5b58");
        btnSave.setFocusable(false);
        btnSave.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnSave.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar1.add(btnSave);
        jToolBar1.add(jSeparator2);

        bgMode.add(jToggleButton2);
        jToggleButton2.setSelected(true);
        jToggleButton2.setText("\u7dda");
        jToggleButton2.setActionCommand("LINE");
        jToggleButton2.setFocusable(false);
        jToggleButton2.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jToggleButton2.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar1.add(jToggleButton2);

        bgMode.add(jToggleButton3);
        jToggleButton3.setText("\u5857\u308a");
        jToggleButton3.setActionCommand("FILL");
        jToggleButton3.setFocusable(false);
        jToggleButton3.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jToggleButton3.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar1.add(jToggleButton3);

        bgMode.add(jToggleButton4);
        jToggleButton4.setText("\u6d88\u3057\u30b4\u30e0");
        jToggleButton4.setActionCommand("ERASE");
        jToggleButton4.setFocusable(false);
        jToggleButton4.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jToggleButton4.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar1.add(jToggleButton4);
        jToolBar1.add(jSeparator1);

        bgStrength.add(jToggleButton9);
        jToggleButton9.setText("\u6975\u7d30");
        jToggleButton9.setActionCommand("1");
        jToggleButton9.setFocusable(false);
        jToggleButton9.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jToggleButton9.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar1.add(jToggleButton9);

        bgStrength.add(jToggleButton5);
        jToggleButton5.setSelected(true);
        jToggleButton5.setText("\u7d30");
        jToggleButton5.setActionCommand("3");
        jToggleButton5.setFocusable(false);
        jToggleButton5.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jToggleButton5.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar1.add(jToggleButton5);

        bgStrength.add(jToggleButton6);
        jToggleButton6.setText("\u4e2d");
        jToggleButton6.setActionCommand("8");
        jToggleButton6.setFocusable(false);
        jToggleButton6.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jToggleButton6.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar1.add(jToggleButton6);

        bgStrength.add(jToggleButton7);
        jToggleButton7.setText("\u592a");
        jToggleButton7.setActionCommand("15");
        jToggleButton7.setFocusable(false);
        jToggleButton7.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jToggleButton7.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar1.add(jToggleButton7);

        bgStrength.add(jToggleButton8);
        jToggleButton8.setText("\u6975\u592a");
        jToggleButton8.setActionCommand("32");
        jToggleButton8.setFocusable(false);
        jToggleButton8.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jToggleButton8.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar1.add(jToggleButton8);
        jToolBar1.add(jSeparator3);

        btnUndo.setText("\u5143\u306b\u623b\u3059");
        btnUndo.setFocusable(false);
        btnUndo.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnUndo.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar1.add(btnUndo);

        btnRedo.setText("\u518d\u5b9f\u884c");
        btnRedo.setFocusable(false);
        btnRedo.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnRedo.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar1.add(btnRedo);

        jPanel1.add(jToolBar1);

        tbColor.setRollover(true);

        btnColor.setText("\u8272");
        btnColor.setFocusable(false);
        btnColor.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnColor.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnColor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnColorActionPerformed(evt);
            }
        });
        tbColor.add(btnColor);

        jPanel1.add(tbColor);

        getContentPane().add(jPanel1, java.awt.BorderLayout.NORTH);

        jMenu1.setText("File");

        miSave.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        miSave.setText("Item");
        jMenu1.add(miSave);

        jMenuBar1.add(jMenu1);

        jMenu2.setText("Edit");

        miUndo.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, java.awt.event.InputEvent.CTRL_MASK));
        miUndo.setText("Item");
        jMenu2.add(miUndo);

        miRedo.setText("Item");
        jMenu2.add(miRedo);

        jMenuBar1.add(jMenu2);

        setJMenuBar(jMenuBar1);

        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        setBounds((screenSize.width-622)/2, (screenSize.height-502)/2, 622, 502);
    }// </editor-fold>//GEN-END:initComponents

private void btnColorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnColorActionPerformed
    Color c = JColorChooser.showDialog(this, "色を選択", selectedColor);
    if(c != null){
        selectColor(c);
        btnColor.setBackground(c);
    }
}//GEN-LAST:event_btnColorActionPerformed

    File current;
    void saveActionExec(){
        JFileChooser jfc;
        if (current != null) {
            jfc = new JFileChooser(current);
        } else {
            jfc = new JFileChooser();
        }
        //jfc.setFileFilter(new FileNameExtensionFilter("PNGファイル", "png"));
        jfc.setFileFilter(new FileFilter(){
            public boolean accept(File f) {
                return f.getAbsolutePath().toLowerCase().endsWith(".png");
            }
            public String getDescription() {
                return "PNGファイル";
            }
        });
        int ret = jfc.showSaveDialog(this);
        if (ret != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File f = jfc.getSelectedFile();
        current = f.getParentFile();
        if(!(f.getAbsolutePath().contains("."))) f = new File(f.getAbsoluteFile() + ".png");
        BufferedImage img = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics g = img.getGraphics();
        canvas.paintComponent(g);
        g.dispose();
        try {
            ImageIO.write(img, "png", f);
        } catch (IOException ex) {
            Logger.getLogger("global").log(Level.SEVERE, null, ex);
        }        
    }
        
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new LafPaintFrame().setVisible(true);
            }
        });
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup bgColor;
    private javax.swing.ButtonGroup bgMode;
    private javax.swing.ButtonGroup bgStrength;
    private javax.swing.JButton btnColor;
    private javax.swing.JButton btnRedo;
    private javax.swing.JButton btnSave;
    private javax.swing.JButton btnUndo;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JToggleButton jToggleButton1;
    private javax.swing.JToggleButton jToggleButton2;
    private javax.swing.JToggleButton jToggleButton3;
    private javax.swing.JToggleButton jToggleButton4;
    private javax.swing.JToggleButton jToggleButton5;
    private javax.swing.JToggleButton jToggleButton6;
    private javax.swing.JToggleButton jToggleButton7;
    private javax.swing.JToggleButton jToggleButton8;
    private javax.swing.JToggleButton jToggleButton9;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JMenuItem miRedo;
    private javax.swing.JMenuItem miSave;
    private javax.swing.JMenuItem miUndo;
    private javax.swing.JToolBar tbColor;
    // End of variables declaration//GEN-END:variables
    
}
