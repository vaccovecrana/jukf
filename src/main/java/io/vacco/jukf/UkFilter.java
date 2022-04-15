package io.vacco.jukf;

import org.la4j.LinearAlgebra;
import org.la4j.Matrix;
import org.la4j.Vector;
import org.la4j.decomposition.CholeskyDecompositor;
import org.la4j.matrix.DenseMatrix;

import java.util.Objects;
import java.util.Random;

public class UkFilter {
  private int L; // states
  private int m; // measurements

  private double c; // scale factor

  private DenseMatrix Wm; // means weights
  private DenseMatrix Wc; // covariance weights
  private Matrix x; // state
  private Matrix P; // covariance

  private DenseMatrix Q; // covariance of process
  private DenseMatrix R; // covariance of measurement

  private final UkFilterParams fp;

  public UkFilter(int L, UkFilterParams fp) {
    this.L = L;
    this.fp = Objects.requireNonNull(fp);
  }

  private void init() { // TODO move these out to initialization params
    x = DenseMatrix.random(L, 1, new Random()).multiply(fp.q); // initial state with noise

    P = DenseMatrix.diagonal(L, 1); // initial state covariance
    Q = DenseMatrix.diagonal(L, fp.q * fp.q); // covariance of process
    R = DenseMatrix.constant(m, m, fp.r * fp.r); // covariance of measurement

    double lambda = fp.alpha * fp.alpha * (L + fp.ki) - L;
    c = L + lambda;

    // weights for means
    Wm = DenseMatrix.constant(1, (2 * L + 1), 0.5 / c);
    Wm.set(0, 0, lambda / c);
    // weights for covariance
    Wc = (DenseMatrix) Wm.copy();
    Wc.set(0, 0, Wm.get(0, 0) + 1 - fp.alpha * fp.alpha + fp.beta);

    c = Math.sqrt(c);
  }

  public void update(double[] measurements) {
    if (m == 0) {
      var mNum = measurements.length;
      if (mNum > 0) {
        m = mNum;
        if (L == 0) L = mNum;
        init();
      }
    }

    var z = DenseMatrix.constant(m, 1, 0);

    z.setColumn(0, Vector.fromArray(measurements));

    // sigma points around x
    Matrix X = getSigmaPoints(x, P, c);

    // unscented transformation of process
    // X1=sigmas(x1,P1,c) - sigma points around x1
    // X2=X1-x1(:,ones(1,size(X1,2))) - deviation of X1
    Matrix[] ut_f_matrices = unscentedTransform(X, Wm, Wc, L, Q);
    Matrix x1 = ut_f_matrices[0];
    Matrix X1 = ut_f_matrices[1];
    Matrix P1 = ut_f_matrices[2];
    Matrix X2 = ut_f_matrices[3];

    // unscented transformation of measurements
    Matrix[] ut_h_matrices = unscentedTransform(X1, Wm, Wc, m, R);
    Matrix z1 = ut_h_matrices[0];
    Matrix Z1 = ut_h_matrices[1];
    Matrix P2 = ut_h_matrices[2];
    Matrix Z2 = ut_h_matrices[3];

    // transformed cross-covariance
    Matrix P12 = X2.multiply(Wc.getRow(0).toDiagonalMatrix()).multiply(Z2.transpose());
    Matrix K = P12.multiply(LinearAlgebra.InverterFactory.GAUSS_JORDAN.create(P2).inverse());

    // state update
    x = x1.add(K.multiply(z.subtract(z1)));
    // covariance update
    P = P1.subtract(K.multiply(P12.transpose()));
  }

  public double[] getState() {
    return x.toDenseMatrix().toArray()[0];
  }

  public double[][] getCovariance() {
    return P.toDenseMatrix().toArray();
  }

  /**
   * Unscented Transformation
   *
   * @param X sigma points
   * @param Wm weights for means
   * @param Wc weights for covariance
   * @param n number of outputs
   * @param R additive covariance
   * @return [transformed mean, transformed sampling points, transformed covariance, transformed deviations]
   */
  private static Matrix[] unscentedTransform(Matrix X, Matrix Wm, Matrix Wc, int n, Matrix R) {
    int L = X.columns();
    Matrix y = DenseMatrix.constant(n, 1, 0);
    Matrix Y = DenseMatrix.constant(n, L, 0);

    Matrix row_in_X;
    for (int k = 0; k < L; k++) {
      row_in_X = X.slice(0, k, X.rows(), k + 1);
      Y = Y.insert(row_in_X, 0, k, Y.rows(), 1);
      y = y.add(Y.slice(0, k, Y.rows(), k + 1).multiply(Wm.get(0, k)));
    }

    Matrix Y1 = Y.subtract(y.multiply(DenseMatrix.constant(1, L, 1)));
    Matrix P = Y1.multiply(Wc.getRow(0).toDiagonalMatrix());
    P = P.multiply(Y1.transpose());
    P = P.add(R);

    return new Matrix[] {y, Y, P, Y1};
  }

  /**
   * Sigma points around reference point.
   *
   * @param x reference point
   * @param P covariance
   * @param c coefficient
   * @return sigma points matrix
   */
  private Matrix getSigmaPoints(Matrix x, Matrix P, double c) {
    CholeskyDecompositor cd = new CholeskyDecompositor(P);

    Matrix A = cd.decompose()[0];
    A = A.multiply(c);
    A = A.transpose();
    int n = x.rows();

    Matrix Y = x.copy();

    Matrix X = DenseMatrix.constant(n, (2 * n + 1), 0);
    X = X.insert(x, 0, 0, n, 1);
    Matrix Y_plus_A = Y.add(A);
    X = X.insert(Y_plus_A, 0, 1, n, n);
    Matrix Y_minus_A = Y.subtract(A);
    X = X.insert(Y_minus_A, 0, n + 1, n, n);

    return X;
  }
}


