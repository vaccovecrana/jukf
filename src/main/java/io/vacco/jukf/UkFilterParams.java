package io.vacco.jukf;

public class UkFilterParams {
  public double alpha; // alpha coefficient, characterizes sigma-points dispersion around mean1
  public double ki;
  public double beta; // beta coefficient, characterizes type of distribution (2 for normal one)

  public double q; // std of process
  public double r; // std of measurement

  public static UkFilterParams from(double alpha, double beta, double ki, double q, double r) {
    var fp = new UkFilterParams();
    fp.alpha = alpha;
    fp.ki = ki;
    fp.beta = beta;
    fp.q = q;
    fp.r = r;
    return fp;
  }

  public static UkFilterParams getDefault() {
    return from(1e-3f, 2, 0, 0.05, 0.3);
  }
}
