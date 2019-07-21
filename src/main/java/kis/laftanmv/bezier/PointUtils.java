/*
 * PointUtil.java
 *
 * Created on 2007/07/12, 18:02
 */

package kis.laftanmv.bezier;

import java.awt.geom.Point2D;

/**
 *
 * @author naoki
 */
public class PointUtils {
    
    /** Creates a new instance of PointUtil */
    private PointUtils() {
    }
    public static Point2D vectorNormalize(Point2D p){
        double l = Math.hypot(p.getX(), p.getY());
        if(l == 0) return p;
        return new Point2D.Double(p.getX() / l, p.getY() / l);
    }
    
    public static double vectorSquaredLength(Point2D p){
        return p.getX() * p.getX() + p.getY() * p.getY();
    }
    
    public static Point2D vectorAdd(Point2D a, Point2D b){
        return new Point2D.Double(
                a.getX() + b.getX(), a.getY() + b.getY());
    }
    
    public static Point2D vectorScale(Point2D v, double scale){
        return new Point2D.Double(
                v.getX() * scale, v.getY() * scale);
    }
    
    public static Point2D vectorSub(Point2D a, Point2D b){
        return new Point2D.Double(
                a.getX() - b.getX(), a.getY() - b.getY());
    }

    public static double vectorDot(Point2D a, Point2D b){
        return a.getX() * b.getX() + a.getY() * b.getY();
    }
    public static Point2D vectorNegate(Point2D p){
        return new Point2D.Double(
                - p.getX(), -p.getY());
    }    
}
