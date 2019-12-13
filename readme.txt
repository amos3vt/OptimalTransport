How to call from MATLAB:


javaaddpath('PATH\Optimal Transport\bin\');
GTTransport_time = tic;
arr = optimalTransport.GTTransportMapping.callFromMATLAB(n, a, b', C, delta);
total_cost_transport = arr(1);
iterationCountTransport = arr(2);
GTTransport_time = toc(GTTransport_time);



Description of tests:

SinkhornComparison.m is run with our Java code calculating the GTTransprt_time, GTTransport_iteration and the total_cost_transport.

Our code can be found at https://git.cs.vt.edu/mnj98/optimal-transport, but this may be private at the moment.


10 different delta values used
deltas = [0.005, 0.01, 0.025, 0.05, 0.075, 0.1, 0.125, 0.15, 0.175, 0.2];

number of runs = 100;

Data used in GTTime plot:
NOTE: mean_SinkError and mean_ErrorGT should be ignored as linprog could not be run to verify error in a test of this size.

delta    GroupCount    mean_SinkTime    mean_SinkError    mean_SinkIters    mean_GTTime    mean_GTIter    mean_ErrorGT
    _____    __________    _____________    ______________    ______________    ___________    ___________    ____________

    0.005       100              7.38           1.0075            2559.1          0.30421         53.99        1.0077
     0.01       100            3.1064           1.0076              1081           0.2028         28.28        1.0081
    0.025       100            0.8501           1.0082            329.97          0.13512         12.05        1.0092
     0.05       100           0.35801            1.009            129.62          0.10534          5.93        1.0111
    0.075       100           0.21375           1.0098             73.29         0.095603          3.56        1.0126
      0.1       100           0.15862           1.0106             48.19         0.091661          2.66        1.0138
    0.125       100           0.12634           1.0113             34.43         0.092876          1.96        1.0153
     0.15       100           0.10854            1.012             26.11         0.088423          1.39        1.017
    0.175       100           0.09161           1.0127             20.55         0.091408          1.24        1.0177
      0.2       100          0.082455           1.0134             16.67          0.09241          1.13        1.0196

Our algorithm runs much faster than Sinkhorn for small delta values, but as the deltas increase it runs at almost the exact
  same speed as Sinkhorn.


Correctness of code:

The code is verified using two measures.
1) There is code at the end of GTTransportMapping.java that asserts that the transport generated is valid
   All residual values should be less than 0.0000001 if all of the supply fills all of the demand.
   If it prints out the error message and kills the program we know that there was an invalid solution.

        double tolerance = 0.0000001; //if any value is off by less than this it's ok
          double[] finalDemandsUsed = new double[n];
          double[] finalSuppliesUsed = new double[n];
          for(int i = 0; i < n; i++) {
            for(int j = 0; j < n*2; j++) {
              finalDemandsUsed[i] += capacity[i + n][j];
              finalSuppliesUsed[i]+= capacity[j][i];

            }
          }

          //For testing the valididty of the solution, the residual supplies and demands are calculated
          double[] residualS = new double[n];
          double[] residualD = new double[n];
          for(int i = 0; i < n; i++) {
            residualS[i] = supplies[i] - finalSuppliesUsed[i];
            residualD[i] = demands[i] - finalDemandsUsed[i];

          }

          //Assert that the transport is valid
          absolute(residualS);
          absolute(residualD);

          if(!allLessOrEqual(residualS, tolerance)) {
            System.out.println("Error: GT did not return a valid transport; a supply constraint was violated.");
          }
          if(!allLessOrEqual(residualD, tolerance)) {
            System.out.println("Error2: GT did not return a valid transport; a demand constraint was violated.");
          }
          assert(Math.abs(fulfilledCapacity - 1) <= tolerance);


2) We also check that that error is less than delta by running linprog to find the true optimal transport.
  One example with the same delta values as above, but with only 10 runs shows that the mean_ErrorGT is always lower than delta

  delta    GroupCount    mean_SinkTime    mean_SinkError    mean_SinkIters    mean_GTTime    mean_GTIter     mean_ErrorGT
_____    __________    _____________    ______________    ______________    ___________    ___________      ____________

0.005        10             9.599         5.4111e-05          2723.1           0.4593         54.5          0.00035319
 0.01        10            4.0173         0.00020145            1130          0.25278         28.8          0.00074966
0.025        10            1.0489         0.00075006           330.4          0.15236         12.2          0.0017697
 0.05        10           0.41871          0.0016156           124.2          0.12809          5.8          0.0036806
0.075        10           0.26508          0.0024227            68.2           0.1197          3.8          0.0056298
  0.1        10           0.20182          0.0031851            43.5          0.13959            3          0.0070351
0.125        10           0.14496          0.0039114            30.2          0.11414          1.9          0.0084007
 0.15        10           0.13304           0.004597            22.5          0.11394          1.3          0.0099113
0.175        10           0.11961          0.0052619            17.6          0.10775          1.3          0.010428
  0.2        10           0.11064          0.0059142            14.2          0.10944            1          0.012335
