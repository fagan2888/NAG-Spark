import java.io.Serializable;
import java.io.IOException;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;

import java.io.FileWriter;
import java.io.File;
import java.io.BufferedWriter;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.mllib.regression.LabeledPoint;

import com.nag.exceptions.NAGBadIntegerException;
import com.nag.routines.Routine;
import com.nag.routines.G02.G02BU;
import com.nag.routines.G02.G02BZ;
import com.nag.routines.G02.G02CG;
import com.nag.routines.G02.G02BW;
import com.nag.routines.F01.F01ZA;

public class NAGLinearRegression {

	private int _numvars;
        private int _dataSize;
        private int _chunkSize = 10000;
        private double[] _con ;
        private double[] _coef;
        private double _Rsquared ;
        private int _ifail;
        private double [] _correlations;

	static class NAGData implements Serializable {
	    NAGData(int length, double[] means, double[] ssq) {
              this.length = length;
	      this.means = means;
	      this.ssq = ssq;
	    }
            int length;
	    double[] means;
	    double[] ssq;
	  }

        static class combineNAGData implements Function2<NAGData,NAGData,NAGData> {
                @Override
                public NAGData call(NAGData data1,NAGData data2) throws Exception {
                        G02BZ g02bz = new G02BZ();
                        int IFAIL = 1;
                        g02bz.eval("M", data1.means.length, data1.length, data1.means,
                                        data1.ssq, data2.length, data2.means, data2.ssq, IFAIL);
                        data1.length = (int)(g02bz.getXSW()); 
                        if(g02bz.getIFAIL()>0) {
                                System.out.println("Error with g02bz!!!");
                                System.exit(1);
                        }                        
                        return data1;
                }
        }        

	 static class ParseSet implements FlatMapFunction<Iterator<LabeledPoint>, NAGData> {
	    @Override
	    public Iterable<NAGData> call(Iterator<LabeledPoint> iter) throws Exception {
              List<LabeledPoint> mypoints= new ArrayList<LabeledPoint>();
              while(iter.hasNext()) {
                        mypoints.add(iter.next());                      
               }
                int length = mypoints.size();
                int numvars = mypoints.get(0).features().toArray().length + 1;
                double[] x = new double[length*numvars];
                double[] features;	      
                for(int i=0;i<length;i++) {
                        features = mypoints.get(i).features().toArray();
                        for(int j=0;j<numvars-1;j++) {
                                x[(j) * length + i] = features[j];
                        }
                        x[(numvars-1) * length + i] = mypoints.get(i).label();
                }
                
                G02BU g02bu = new G02BU();
                double[] means = new double [numvars];
                double[] ssq = new double [(numvars*numvars+numvars)/2];
                int ifail = 1;
                double sw = 0;
                g02bu.eval("M", "U", length, numvars, x, length, x, 0.0, 
                                                        means, ssq, ifail);
                if(g02bu.getIFAIL()>0) {
                        System.out.println("Error with g02bu!!!");
                        System.exit(1);
                }                        
	      return Arrays.asList(new NAGData(length,means,ssq));
	    }
        }

        public void RunRegression(JavaRDD<LabeledPoint> mypoints)
                                        throws NAGBadIntegerException {

                Routine.init();                
                _numvars = mypoints.take(1).get(0).features().size() + 1;               

                long startTime = System.currentTimeMillis();

                _dataSize = (int) mypoints.count();                
                int numPartitions = (int) (_dataSize / _chunkSize) + 1;

                JavaRDD<NAGData> dataset = mypoints.repartition(numPartitions).mapPartitions(
                                                                                new ParseSet());
                double[] emptyMeans = new double[_numvars];
                double[] emptySSQ = new double[(_numvars*_numvars+_numvars)/2];
                NAGData NAGEmpty = new NAGData(0, emptyMeans, emptySSQ);
                NAGData finalpt = dataset.aggregate(NAGEmpty, new combineNAGData(),
                                                                new combineNAGData());
                int ifail = 1;
                int K1 = _numvars;
                int K = K1 - 1;

                double[] result = new double[13];
                double[] coef = new double[K*3];
                double[] con = new double[3];
                double[] rinv = new double[K*K];
                double[] c = new double[K*K];
                double[] wkz = new double[K*K];
                double[] ssq;

                ssq = convertMatrix(finalpt.ssq, K1);

                G02BW g02bw = new G02BW();

                g02bw.eval(_numvars, finalpt.ssq, ifail);
                if(g02bw.getIFAIL() > 0) {
                        System.out.println("Error with NAG (g02bw) IFAIL = " + g02bw.getIFAIL());       
                        System.exit(1);                
                }

                double[] pearsonR;

                pearsonR = convertMatrix(finalpt.ssq, K1);
                _correlations = convertMatrix(finalpt.ssq, K1);

                G02CG g02cg = new G02CG();                     
                g02cg.eval(finalpt.length, K1, K, finalpt.means, ssq, K1, pearsonR, 
                                K1, result, coef, K, con, rinv, K, c, K, wkz, K, ifail);

                long endTime = System.currentTimeMillis();
                _con = con;
                _Rsquared = result[11];
                _coef = coef;
                _ifail = g02cg.getIFAIL();
        }

        /* Method to convert packed 1-d array into unpack 2-d array */
        private double[] convertMatrix(double[] input, int n) throws NAGBadIntegerException {
                String JOB = "U";
                String UPLO = "U";
                String DIAG = "N";
                int N = n;
                int LDA = N;
                double[] B = new double[N*N];
                int ifail = 1;
                F01ZA f01za = new F01ZA(JOB, UPLO, DIAG, N, B, LDA, input, ifail);
                f01za.eval();
                if(f01za.getIFAIL()>0) {
                        System.out.println("Error with f01za!!!");
                        System.exit(1);
                }                        
                return B;
        }

        public void writeLogFile(String fileName) throws Exception {
                File file;
                FileWriter fw;
                BufferedWriter bw;
        
                file = new File(fileName);
                if(!file.exists()){
                        file.createNewFile();    
                }

                fw = new FileWriter(file.getAbsoluteFile());
                bw = new BufferedWriter(fw);
                bw.write("NAG Spark Multi-Linear Regression\n");
                bw.write("*************************************************\n");
                bw.write("Total number of points: " + _dataSize + "\n");
                bw.write("Var\t\t|Coef\t\tSE\t\tt-value\n");
                bw.write("------------------------------------------------------\n");
                bw.write(String.format("Intcp\t\t|%d\t\t%.1f\t\t%.1f", (int)_con[0], 
                                                        _con[1], _con[2]));
                bw.newLine();
                for(int i=0;i<_numvars-1;i++) 
                {
                        bw.write(String.format(i+"\t\t|%.1f\t\t%.1f\t\t%.1f", _coef[i],
                                                        _coef[i+3], _coef[i+6]));
                        bw.newLine();
                }
                bw.newLine();
                bw.write("R-Squared = " + _Rsquared + "\n");
                bw.write("NAG IFAIL = " + _ifail + "\n");
                bw.write("*************************************************\n");
                bw.write("Correlations:");
                bw.newLine();
                for(int i=0;i<_numvars;i++){
                        for(int j=0;j<_numvars;j++) 
                                bw.write(String.format("%.3f\t", _correlations[j+i*_numvars]));    
                        bw.newLine();
                }
                bw.close();        
        } 
}