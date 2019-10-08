
package optimalTransport;




public class GTTransport {
	private static int[] vertexVisited;
	private static int AugPathVerIndex;
	//private static int[] bFree;
	//private static int[] aFree;
	
	public double[][] Transport(double[][] C, double[] supplies, double[] demands, int n) {
		
		double sSum = 0;
		double dSum = 0;
		
		for(int i = 0; i < n; i++) {
			sSum += supplies[i];
			dSum += demands[i];
		}
		
		assert(sSum == dSum);
		
		double[][] CBA = transpose(C);
		double[][] CAB = C;
		
		int[] bFree = new int[n];
		int[] aFree = new int[n];
		
		for(int i = 0; i < n; i++) {
			bFree[i] = 1;
			aFree[i] = 1;
		}
		
		int[] dualWeights = new int[2*n];
		
		double[] deficiencyB = supplies;
		double[] deficiencyA = demands;
		
		for(int i = 0; i < n; i++) {
			if(deficiencyB[i] == 0) {
				bFree[i] = 0;
			}
			if(deficiencyA[i] == 0) {
				aFree[i] = 0;
			}
		}
		
		double[][] capacityAB = new double[n][n];
		double[][] capacityBA = new double[n][n];
		
		for(int j = 0; j < n; j++) {
			for(int i = 0; i < n; i++) {
				capacityBA[i][j] = Math.min(deficiencyB[j], deficiencyA[i]); //fix matlab vs java 2d array issues
			}
		}
		
		int iterationCount = 0;
		
		double[][] slackAB = new double[n][n];
		double[][] slackBA = new double[n][n];
		vertexVisited = new int[2*n];
		int[] augmentingPathVertices = new int[2*n];
		for(int i = 0; i < n; i++) {
			for(int j = 0; i < n; i++) {
				slackAB[j][i] = dualWeights[i + n] + dualWeights[j] - CAB[j][i];
				slackBA[j][i] = CBA[j][i] + 1 - dualWeights[i] - dualWeights[j + n];
			}
		}
		
		while(3 != n) {// main phase loop with bogus argument 
			
			
			//GTTransport.m line 213:
			
			for(int vertex = 1; vertex < n; vertex++) {
				if(bFree[vertex] == 1) {
					
					while(deficiencyB[vertex] > 0 && vertexVisited[vertex] < 2*n) {
						augmentingPathVertices = DFSUtil(vertex, augmentingPathVertices, n, slackAB, slackBA, aFree, capacityAB, capacityBA);
						
						if(AugPathVerIndex <= 0) {
							break;
						}
						else {
							
							double val = Math.min(deficiencyB[augmentingPathVertices[0]], deficiencyA[augmentingPathVertices[AugPathVerIndex] - n]);
							for(int j = 0; j < AugPathVerIndex; j++) {
								int vertex1 = augmentingPathVertices[j];
								int vertex2 = augmentingPathVertices[j + 1];
								
								if(vertex1 > n) {
									val = Math.min(val, capacityAB[vertex2][vertex1 - n]);
									
								}
								else {
									val = Math.min(val, capacityBA[vertex2 - n][vertex1]);
								}
							}
							
							for(int j = 0; j < AugPathVerIndex; j++) {
								int vertex1 = augmentingPathVertices[j];
								int vertex2 = augmentingPathVertices[j + 1];
								
								if(vertex1 > n) {
									capacityAB[vertex2] [vertex1 - n] = capacityAB[vertex2] [vertex1 - n] - val;
	                                capacityBA[vertex1 - n][ vertex2] = capacityBA[vertex1 - n] [vertex2] + val;
	                                if(capacityAB[vertex2][ vertex1 - n] > 0) {
                                    
	                                	vertexVisited[vertex1] = vertex2 - 1; 
	                                }
	                                
								}
								else {
									capacityBA[vertex2 - n][ vertex1] = capacityBA[vertex2 - n][ vertex1] - val;
	                                capacityAB[vertex1][ vertex2 - n] = capacityAB[vertex1][ vertex2 - n] + val;
	                                
	                                if(capacityBA[vertex2 - n] [vertex1] > 0) {
                                    
	                                	vertexVisited[vertex1] = vertex2 - 1; 
	                                }
								}
							}
							
							//update deficiencies 
							
							deficiencyB[vertex] = deficiencyB[vertex] - val;
	                        if(deficiencyB[vertex] == 0) {
	                            bFree[vertex] = 0;
	                        }
	                        
	                        int last = augmentingPathVertices[AugPathVerIndex] - n;
	                        deficiencyA[last] = deficiencyA[last] - val;
	                        if(deficiencyA[last] == 0){
	                            aFree[last] = 0;
	                        }
						}
						
					}
				}
			}
		}
		
		
		return null;
	}
	
	public static double[][] transpose(double[][] matrix){
		double[][] t = new double[matrix.length][matrix[0].length];
		
		for(int i = 0; i < matrix.length; i++) {
			for(int j = 0; j < matrix[0].length; j++) {
				t[j][i] = matrix[i][j];
			}
		}
		return t;
	}
	
	//returns augmentingPathVertices, with vertexVisited & augPathVerIndex as globals
	public int[] DFSUtil(int vertex, int[] augmentingPathVertices, int n, double[][] slackAB, double[][] slackBA, int[] aFree, double[][] capacityAB, double[][] capacityBA ){
		AugPathVerIndex = 0;
		augmentingPathVertices[AugPathVerIndex] = vertex;
		
		while(AugPathVerIndex >= 0) {
			vertex = augmentingPathVertices[AugPathVerIndex];
			
			
			if(vertex > n && aFree[vertex - n] == 1) { // THERE MAY BE AN ISSUE WITH THIS CONDITION
															// LOOK HERE WHEN IT DOESN'T WORK
				return augmentingPathVertices;
			}
			
			boolean backtrack = true;
			
			int range_var1 = vertexVisited[vertex]+1; //this +1 may be wrong too
			int range_var2 = 0;
			
			if(vertex < n) {
				range_var2 = 2*n;
			}
			else {
				range_var2 = n;
			}
			
			for(int i = range_var1; i <= range_var2; i++) {
				vertexVisited[vertex] = i;
				
				
				if(vertex < n) {
					int a  = i - n;
					if(slackBA[a][vertex] == 0 && capacityBA[a][vertex] > 0) {
						backtrack = false;
						augmentingPathVertices[++AugPathVerIndex] = i;
						break;
					}
				}
				else {
					int a = vertex - n;
					if(slackAB[i][a] == 0 && capacityAB[i][a] > 0) {
						backtrack = false;
						augmentingPathVertices[++AugPathVerIndex] = i;
						break;
					}
				}
			}
			if(backtrack) {
				augmentingPathVertices[AugPathVerIndex--] = 0;
			}
			
		}
		return augmentingPathVertices;
	}
	
	
	
	
	
	
}