/*
 * NearestPoint.java
 * 
 * Created on 2007/07/08, 17:28:11
 */

package kis.laftanmv.bezier;

import java.awt.geom.Point2D;
import static kis.laftanmv.bezier.PointUtils.*;

/**
 *
 * @author naoki
 */
public class NearestPoint {
    static final int MAXDEPTH = 64;
    //static final double EPSILON = Math.scalb(1, -(MAXDEPTH + 1));
    static final double EPSILON = Math.pow(2, -(MAXDEPTH + 1));
    
    static double nearestPointOnCurve(Point2D p, Point2D[] points){
        double[] tCandidate = new double[6];
        Point2D[] w = convertToBezierForm(p, points);
        int nSolutions = findRoots(w, tCandidate, 0);
        
        double dist = vectorSquaredLength(vectorSub(p, points[0]));
        double t = 0;
        
        for(int i = 0; i < nSolutions; ++i){
            Point2D pp = bezier3Point(
                    points[0].getX(), points[0].getY(),
                    points[1].getX(), points[1].getY(),
                    points[2].getX(), points[2].getY(),
                    points[3].getX(), points[3].getY(), tCandidate[i]);
            double newDist = vectorSquaredLength(vectorSub(p, pp));
            if(newDist < dist){
                dist = newDist;
                t = tCandidate[i];
            }
        }
        
        double newDist = vectorSquaredLength(vectorSub(p, points[3]));
        if(newDist < dist){
            dist = newDist;
            t = 1;
        }
        
        return t;
    }
    
   
    static Point2D[] convertToBezierForm(Point2D p, Point2D[] points){
        Point2D[] c = new Point2D[4];
        Point2D[] d = new Point2D[3];
        double[][] cdTable = new double[3][4];
        double[][] z = {
            {1.0, 0.6, 0.3, 0.1},
            {0.4, 0.6, 0.6, 0.4},
            {0.1, 0.3, 0.6, 1.0}
        };
        
        for(int i = 0; i <= 3; ++i){
            c[i] = vectorSub(points[i], p);
        }
        
        for(int i = 0; i < 3; ++i){
            d[i] = vectorScale(vectorSub(points[i + 1], points[i]), 3);
        }
        
        for( int row = 0; row < 3; ++row){
            for(int column = 0; column <= 3; ++column){
                cdTable[row][column] = vectorDot(d[row], c[column]);
            }
        }
        
        Point2D[] w = new Point2D[6];
        for(int i = 0; i <= 5; ++i){
            w[i] = new Point2D.Double(i / 5., 0);
        }
        
        int n = 3;
        int m = 2;
        for(int i = 0; i <= n + m; ++i){
            int lb = Math.max(0, i - m);
            int ub = Math.min(i, n);
            for( int j = lb; j <= ub; ++j){
                int k = i - j;
                w[j + k].setLocation(w[j + k].getX(), w[j + k].getY() + cdTable[k][j] * z[k][j]);
            }
        }
        return w;
    }
    

    
    static int findRoots(Point2D[] points, double[] t, int depth){
        Point2D[] left = new Point2D[6];
        Point2D[] right = new Point2D[6];
        double[] leftT = new double[6];
        double[] rightT = new double[6];
        
        switch (crossingCount(points)){
        case 0:
            return 0;
        case 1:
            if(depth >= MAXDEPTH){
                t[0] = (points[0].getX() + points[points.length - 1].getX()) / 2;
                return 1;
            }
            if(controlPolygonFlatEnough(points)){
                t[0] = computeXIntercept(points);
                return 1;
            }
            break;
        }
        
        bezier5Point(points, 0.5, left, right);
        int leftCount = findRoots(left, leftT, depth + 1);
        int rightCount = findRoots(right, rightT, depth + 1);
        
        for(int i = 0; i < leftCount; ++i){
            t[i] = leftT[i];
        }
        for(int i = 0; i < rightCount; ++i){
            t[i + leftCount] = rightT[i];
        }
        
        return leftCount + rightCount;
    }
    
    static int crossingCount(Point2D[] points){
        int n = 0;
        int sign = getSign(points[0].getY());
        int oldsign = sign;
        for(int i = 1; i < points.length; ++i){
            sign = getSign(points[i].getY());
            if(sign != oldsign) ++n;
            oldsign = sign;
        }
        return n;
    }
    static int getSign(double v){
        if(v == 0) return 0;
        return v < 0 ? -1 : 1;
    }
    
    static boolean controlPolygonFlatEnough(Point2D[] points){
        int degree = points.length - 1;
        double[] distance = new double[degree + 1];

        double a = points[0].getY() - points[degree].getY();
        double b = points[degree].getX() - points[0].getX();
        double c = points[0].getX() * points[degree].getY() 
                - points[degree].getX() * points[0].getY();

        double squared = a * a + b * b;

        for (int i = 1; i < degree; ++i) {
            distance[i] = a * points[i].getX() + b * points[i].getY() + c;
            if (distance[i] > 0) {
                distance[i] = (distance[i] * distance[i]) / squared;
            }
            if (distance[i] < 0) {
                distance[i] = -(distance[i] * distance[i]) / squared;
            }
        }
        
        double aboveMaxDistance = 0;
        double belowMaxDistance = 0;
        for(int i = 1; i < degree; ++i){
            if(distance[i] < 0){
                belowMaxDistance = Math.min(belowMaxDistance, distance[i]);
            }
            if(distance[i] > 0){
                aboveMaxDistance = Math.max(aboveMaxDistance, distance[i]);
            }
        }
        
        double a1 = 0;
        double b1 = 1;
        double c1 = 0;

        double a2 = a;
        double b2 = b;
        double c2 = c + aboveMaxDistance;
        
        double det = a1 * b2 - a2 * b1;
        double detInv = 1 / det;
        
        double intercept1 = (b1 * c2 - b2 * c1) * detInv;
        
        a2 = a;
        b2 = b;
        c2 = c + belowMaxDistance;
        det = a1 * b2 - a2 * b1;
        detInv = 1 / det;
        
        double intercept2 = (b1 * c2 - b2 * c1) * detInv;
        
        double leftIntercept = Math.min(intercept1, intercept2);
        double rightIntercept = Math.max(intercept1, intercept2);
        
        double error = 0.5 * (rightIntercept - leftIntercept);
        
        return error < EPSILON;
    }
    
    
    static double computeXIntercept(Point2D[] points){
        //double xlk = 1 - 0;
        //double ylk = 0 - 0;
        double xnm = points[points.length - 1].getX() - points[0].getX();
        double ynm = points[points.length - 1].getY() - points[0].getY();
        double xmk = points[0].getX();
        double ymk = points[0].getY();
        
        //double det = ynm;
        //double det2 = xnm * ylk - ynm * xlk;
        //double detInv = 1 / det;
        double detInv = - 1 / ynm;
        
        double s = (xnm * ymk - ynm * xmk) * detInv;
        //double x = 0 + xlk * s;
        //double x = s;
        
        //return x;
        return s;
    }
    
    static Point2D bezier5Point(
            Point2D[] points, double t, Point2D[] left, Point2D[] right)
    {
        Point2D[][] vtemp = new Point2D[6][6];
        
        for(int i = 0; i <= 5; ++i){
            vtemp[0][i] = points[i];
        }
        for(int i = 1; i <= 5; ++i){
            for(int j = 0; j <= 5 - i; ++j){
                vtemp[i][j] = new Point2D.Double(
                        (1 - t) * vtemp[i - 1][j].getX() + t * vtemp[i - 1][j + 1].getX(),
                        (1 - t) * vtemp[i - 1][j].getY() + t * vtemp[i - 1][j + 1].getY());
            }
        }
        if(left != null){
            for(int i = 0; i <= 5; ++i){
                left[i]  = vtemp[i][0];
            }
        }
        if(right != null){
            for(int i = 0; i <= 5; ++i){
                right[i] = vtemp[5 - i][i];
            }
        }
        
        return vtemp[5][0];
    }
    
    static Point2D bezier3Point(
            double x1, double y1,
            double x2, double y2,
            double x3, double y3,
            double x4, double y4,
            double t){
        double ti = 1 - t;
        double x = x1 * ti * ti * ti + 
                3 * t * ti * ti * x2 + 
                3 * t * t * ti * x3 + 
                t * t * t * x4;
        double y = y1 * ti * ti * ti + 
                3 * t * ti * ti * y2 + 
                3 * t * t * ti * y3 + 
                t * t * t * y4;
        return new Point2D.Double(x, y);
    }

    private NearestPoint() {
    }

}
