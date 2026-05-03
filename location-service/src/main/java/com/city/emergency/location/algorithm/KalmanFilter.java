package com.city.emergency.location.algorithm;

import lombok.Data;

@Data
public class KalmanFilter {

    private double[][] state;
    private double[][] covariance;
    private double[][] processNoise;
    private double[][] measurementNoise;
    private double[][] stateTransition;
    private double[][] observationModel;
    
    private static final double DEFAULT_PROCESS_NOISE = 0.01;
    private static final double DEFAULT_MEASUREMENT_NOISE = 0.1;

    public KalmanFilter() {
        this.state = new double[4][1];
        this.covariance = new double[4][4];
        this.processNoise = new double[4][4];
        this.measurementNoise = new double[2][2];
        this.stateTransition = new double[4][4];
        this.observationModel = new double[2][4];
        
        initializeMatrices();
    }

    private void initializeMatrices() {
        for (int i = 0; i < 4; i++) {
            state[i][0] = 0;
            for (int j = 0; j < 4; j++) {
                covariance[i][j] = (i == j) ? 1.0 : 0.0;
                processNoise[i][j] = (i == j) ? DEFAULT_PROCESS_NOISE : 0.0;
                stateTransition[i][j] = (i == j) ? 1.0 : 0.0;
            }
        }
        
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                measurementNoise[i][j] = (i == j) ? DEFAULT_MEASUREMENT_NOISE : 0.0;
            }
        }
        
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 4; j++) {
                observationModel[i][j] = (i == j) ? 1.0 : 0.0;
            }
        }
    }

    public void setDeltaTime(double dt) {
        stateTransition[0][2] = dt;
        stateTransition[1][3] = dt;
    }

    public void setInitialState(double longitude, double latitude) {
        this.state[0][0] = longitude;
        this.state[1][0] = latitude;
        this.state[2][0] = 0.0;
        this.state[3][0] = 0.0;
    }

    public void predict() {
        state = multiply(stateTransition, state);
        covariance = add(multiply(multiply(stateTransition, covariance), transpose(stateTransition)), processNoise);
    }

    public void update(double longitude, double latitude) {
        double[][] measurement = {{longitude}, {latitude}};
        update(measurement);
    }

    public void update(double[][] measurement) {
        double[][] innovation = subtract(measurement, multiply(observationModel, state));
        double[][] innovationCovariance = add(multiply(multiply(observationModel, covariance), transpose(observationModel)), measurementNoise);
        double[][] kalmanGain = multiply(multiply(covariance, transpose(observationModel)), inverse(innovationCovariance));
        
        state = add(state, multiply(kalmanGain, innovation));
        
        double[][] identity = createIdentity(4);
        double[][] temp = subtract(identity, multiply(kalmanGain, observationModel));
        covariance = multiply(multiply(temp, covariance), transpose(temp));
        covariance = add(covariance, multiply(multiply(kalmanGain, measurementNoise), transpose(kalmanGain)));
    }

    public double getLongitude() {
        return state[0][0];
    }

    public double getLatitude() {
        return state[1][0];
    }

    public double getVelocityX() {
        return state[2][0];
    }

    public double getVelocityY() {
        return state[3][0];
    }

    private double[][] multiply(double[][] a, double[][] b) {
        int rowsA = a.length;
        int colsA = a[0].length;
        int colsB = b[0].length;
        
        double[][] result = new double[rowsA][colsB];
        
        for (int i = 0; i < rowsA; i++) {
            for (int j = 0; j < colsB; j++) {
                for (int k = 0; k < colsA; k++) {
                    result[i][j] += a[i][k] * b[k][j];
                }
            }
        }
        
        return result;
    }

    private double[][] add(double[][] a, double[][] b) {
        int rows = a.length;
        int cols = a[0].length;
        
        double[][] result = new double[rows][cols];
        
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                result[i][j] = a[i][j] + b[i][j];
            }
        }
        
        return result;
    }

    private double[][] subtract(double[][] a, double[][] b) {
        int rows = a.length;
        int cols = a[0].length;
        
        double[][] result = new double[rows][cols];
        
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                result[i][j] = a[i][j] - b[i][j];
            }
        }
        
        return result;
    }

    private double[][] transpose(double[][] a) {
        int rows = a.length;
        int cols = a[0].length;
        
        double[][] result = new double[cols][rows];
        
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                result[j][i] = a[i][j];
            }
        }
        
        return result;
    }

    private double[][] inverse(double[][] a) {
        int n = a.length;
        
        double[][] augmented = new double[n][2 * n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                augmented[i][j] = a[i][j];
            }
            augmented[i][i + n] = 1.0;
        }
        
        for (int i = 0; i < n; i++) {
            int pivot = i;
            for (int j = i + 1; j < n; j++) {
                if (Math.abs(augmented[j][i]) > Math.abs(augmented[pivot][i])) {
                    pivot = j;
                }
            }
            
            if (pivot != i) {
                double[] temp = augmented[i];
                augmented[i] = augmented[pivot];
                augmented[pivot] = temp;
            }
            
            double div = augmented[i][i];
            for (int j = i; j < 2 * n; j++) {
                augmented[i][j] /= div;
            }
            
            for (int j = 0; j < n; j++) {
                if (j != i) {
                    double factor = augmented[j][i];
                    for (int k = i; k < 2 * n; k++) {
                        augmented[j][k] -= factor * augmented[i][k];
                    }
                }
            }
        }
        
        double[][] result = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                result[i][j] = augmented[i][j + n];
            }
        }
        
        return result;
    }

    private double[][] createIdentity(int n) {
        double[][] result = new double[n][n];
        for (int i = 0; i < n; i++) {
            result[i][i] = 1.0;
        }
        return result;
    }
}
