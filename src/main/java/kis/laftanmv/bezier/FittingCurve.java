/*
 * FittingCurve.java
 * 
 * Created on 2007/07/10, 15:11:42
 */

package kis.laftanmv.bezier;

import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import static kis.laftanmv.bezier.PointUtils.*;
/**
 *
 * @author kishida
 */
public class FittingCurve {

    public static int fitCurve(GeneralPath path, Point2D[] d, double error, int[] curveCount, Point2D tan, boolean recursive){
        if(d.length <= 1) return 0;
        Point2D tHat1;// = computeLeftTangent(d, 0);
        if(tan != null && (tan.getX() != 0 || tan.getY() != 0)){
            tHat1 = tan;
        }else{
            tHat1 = computeLeftTangent(d, 0);
        }
        Point2D tHat2 = computeRightTangent(d, d.length - 1);
        return fitCubic(path, d, 0, d.length - 1, tHat1, tHat2, error, curveCount, tan, recursive);
    }
    
    private static int fitCubic(GeneralPath path, Point2D[] d, int first, int last, Point2D tHat1, Point2D tHat2, double error, int[] curveCount, Point2D tan, boolean recursive){
        double iterationError = error * error;
        int npts = last - first + 1;
        int maxIterations = 4;
        
        if(npts == 2){
            //double dist = distanceBetween2Points(d[last], d[first]) / 3;
            double dist = d[last].distance(d[first]) / 3;
            Point2D[] bez = new Point2D[4];
            bez[0] = d[first];
            bez[3] = d[last];
            bez[1] = vectorAdd(bez[0], vectorScale(tHat1, dist));
            bez[2] = vectorAdd(bez[3], vectorScale(tHat2, dist));
            //path.moveTo(bez[0].getX(), bez[0].getY());
            path.curveTo(
                    (float)bez[1].getX(), (float)bez[1].getY(),
                    (float)bez[2].getX(), (float)bez[2].getY(),
                    (float)bez[3].getX(), (float)bez[3].getY());
            if(curveCount != null) curveCount[0]++;
            return 1;
        }
        
        double[] u = chordLengthParameterize(d, first, last);
        Point2D[] bez = generateBezier(d, first, last, u, tHat1, tHat2);
        
        int[] splitPoint = {0};
        double maxError = computeMaxError(d, first, last, bez, u, splitPoint);
        if(maxError < error){
            //path.moveTo(bez[0].getX(), bez[0].getY());
            path.curveTo(
                    (float)bez[1].getX(), (float)bez[1].getY(),
                    (float)bez[2].getX(), (float)bez[2].getY(),
                    (float)bez[3].getX(), (float)bez[3].getY());
            if(curveCount != null) curveCount[0]++;
            return last;
        }
        
        if(maxError < iterationError){
            for(int i = 0; i < maxIterations; ++i){
                double[] uPrime = reparameterize(d, first, last, u, bez);
                bez = generateBezier(d, first, last, uPrime, tHat1, tHat2);
                maxError = computeMaxError(d, first, last, bez, uPrime, splitPoint);
                if(maxError < error){
                    //path.moveTo(bez[0].getX(), bez[0].getY());
                    path.curveTo(
                            (float)bez[1].getX(), (float)bez[1].getY(),
                            (float)bez[2].getX(), (float)bez[2].getY(),
                            (float)bez[3].getX(), (float)bez[3].getY());
                    if(curveCount != null) curveCount[0]++;
                    return last; 
                }
                u = uPrime;
            }
        }
        
        Point2D tHatCenter = computeCenterTangent(d, splitPoint[0]);
        int n = fitCubic(path, d, first, splitPoint[0], tHat1, tHatCenter, error, curveCount, tan, recursive);
        Point2D neg = vectorNegate(tHatCenter);
        if(!recursive){
            if(tan != null) tan.setLocation(neg);
            return n;//�ЂƂ������Ȃ�
        }
        fitCubic(path, d, splitPoint[0], last, neg, tHat2, error, curveCount, tan, recursive);
        return n;
    }
    
    private static Point2D[] generateBezier(Point2D[] d, int first, int last, double[] uPrime, Point2D tHat1, Point2D tHat2){
        double[][] c = { {0, 0}, {0, 0} };
        double[] x = {0, 0};
        Point2D[] bez = new Point2D[4];
        
        int npts = last - first + 1;
        Point2D[][] a = new Point2D[npts][2];
        
        for(int i = 0; i < npts; ++i){
            a[i][0] = vectorScale(tHat1, b1(uPrime[i]));
            a[i][1] = vectorScale(tHat2, b2(uPrime[i]));
        }
        
        for(int i = 0; i < npts; ++i){
            c[0][0] += vectorDot(a[i][0], a[i][0]);
            c[0][1] += vectorDot(a[i][0], a[i][1]);
            c[1][0] = c[0][1];
            c[1][1] += vectorDot(a[i][1], a[i][1]);
            
            Point2D tmp = vectorSub(
                    d[first + i], 
                    vectorAdd(
                        vectorScale(d[first], b0(uPrime[i])),
                        vectorAdd(
                            vectorScale(d[first], b1(uPrime[i])),
                            vectorAdd(
                                vectorScale(d[last], b2(uPrime[i])), 
                                vectorScale(d[last], b3(uPrime[i]))))));
            x[0] += vectorDot(a[i][0], tmp);
            x[1] += vectorDot(a[i][1], tmp);
        }
        double detC0C1 = c[0][0] * c[1][1] - c[1][0] * c[0][1];
        double detC0x  = c[0][0] * x[1]    - c[1][0] * x[0];
        double detxC1  = x[0]    * c[1][1] - x[1]    * c[0][1];
        
        double alphaL = (detC0C1 == 0) ? 0 : detxC1 / detC0C1;
        double alphaR = (detC0C1 == 0) ? 0 : detC0x / detC0C1;
        
        double segLength = d[last].distance(d[first]);
        double epsilon = 1.0e-6 * segLength;
        if(alphaL < epsilon || alphaR < epsilon){
            double dist = segLength / 3;
            bez[0] = d[first];
            bez[3] = d[last];
            bez[1] = vectorAdd(bez[0], vectorScale(tHat1, dist));
            bez[2] = vectorAdd(bez[3], vectorScale(tHat2, dist));
            return bez;
        }
        
        bez[0] = d[first];
        bez[3] = d[last];
        bez[1] = vectorAdd(bez[0], vectorScale(tHat1, alphaL));
        bez[2] = vectorAdd(bez[3], vectorScale(tHat2, alphaR));
        
        return bez;
    }
    
    private static double[] reparameterize(Point2D[] d, int first, int last, double[] u, Point2D[] bez){
        double[] uPrime = new double[last - first + 1];
        for(int i = first; i <= last; ++i){
            uPrime[i - first] = newtonRaphsonRootFind(bez, d[i], u[i - first]);
        }
        return uPrime;
    }
    
    private static double newtonRaphsonRootFind(Point2D[] curve, Point2D p, double u){
        Point2D qu = bezier(3, curve, u);
        Point2D[] q1 = new Point2D[3];
        Point2D[] q2 = new Point2D[2];
        for(int i = 0; i <= 2; ++i){
            q1[i] = new Point2D.Double(
                    (curve[i + 1].getX() - curve[i].getX()) * 3,
                    (curve[i + 1].getY() - curve[i].getY()) * 3);
        }
        for(int i = 0; i <= 1; ++i){
            q2[i] = new Point2D.Double(
                    (q1[i + 1].getX() - q1[i].getX()) * 2,
                    (q1[i + 1].getY() - q1[i].getY()) * 2);
        }
        
        Point2D q1u = bezier(2, q1, u);
        Point2D q2u = bezier(1, q2, u);
        
        double numerator = (qu.getX() - p.getX()) * q1u.getX() + (qu.getY() - p.getY()) * q1u.getY();
        double denominator = q1u.getX() * q1u.getX() + q1u.getY() * q1u.getY() + 
                (qu.getX() - p.getX()) * q2u.getX() + (qu.getY() - p.getY()) * q2u.getY();
        if(denominator == 0) return u;
        
        return u - (numerator / denominator);
    }
    
    static Point2D computeLeftTangent(Point2D[] d, int end){
        Point2D tHat = vectorSub(d[end + 1], d[end]);
        return vectorNormalize(tHat);
    }
    
    private static Point2D computeRightTangent(Point2D[] d, int end){
        Point2D tHat = vectorSub(d[end - 1], d[end]);
        return vectorNormalize(tHat);
    }
    
    private static Point2D computeCenterTangent(Point2D[] d, int center){
        Point2D v1 = vectorSub(d[center - 1], d[center]);
        Point2D v2 = vectorSub(d[center], d[center + 1]);
        Point2D tHatCenter = new Point2D.Double(
                (v1.getX() + v2.getX()) / 2,
                (v1.getY() + v2.getY()) / 2);
        
        return vectorNormalize(tHatCenter);
    }
    
    private static double[] chordLengthParameterize(Point2D[] d, int first, int last){
        double[] u = new double[last - first + 1];
        u[0] = 0;
        for(int i = first + 1; i <= last; ++i){
            u[i - first] = u[i - first - 1] +
                    d[i].distance(d[i - 1]);
        }
        
        for(int i = first + 1; i <= last; ++i){
            u[i - first] /= u[last - first];
        }
        return u;
    }
    
    private static double computeMaxError(Point2D[] d, int first, int last, Point2D[] bezCurve, double[] u, int[] splitPoint){
        splitPoint[0] = (last - first + 1) / 2;
        double maxDist = 0;
        for(int i = first + 1; i <last; ++i){
            Point2D p = bezier(3, bezCurve, u[i - first]);
            Point2D v = vectorSub(p, d[i]);
            double dist = vectorSquaredLength(v);
            if(dist >= maxDist){
                maxDist = dist;
                splitPoint[0] = i;
            }
        }
        
        return maxDist;
    }
        
    
    private static Point2D bezier(int degree, Point2D[] v, double t){
        Point2D[] vtemp = new Point2D[degree + 1];
        for(int i = 0; i <= degree; ++i){
            vtemp[i] = v[i];
        }
        for(int i = 1; i <= degree; ++i){
            for(int j = 0; j <= degree - i; ++j){
                vtemp[j] = new Point2D.Double(
                        (1 - t) * vtemp[j].getX() + t * vtemp[j + 1].getX(),
                        (1 - t) * vtemp[j].getY() + t * vtemp[j + 1].getY());
            }
        }
        return vtemp[0];
    }
    private static double b0(double u){
        double tmp = 1 - u;
        return tmp * tmp * tmp;
    }
    private static double b1(double u){
        double tmp = 1 - u;
        return 3 * tmp * tmp * u;
    }
    private static double b2(double u){
        double tmp = 1- u;
        return 3 * tmp * u * u;
    }
    private static double b3(double u){
        return u * u * u;
    }
    

}
