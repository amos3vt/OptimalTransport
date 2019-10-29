package optimalTransport;

import java.io.File;
import java.io.FileNotFoundException;

public class Testing {
	
	
	public static void main(String[] args) throws FileNotFoundException {
		
		int n = Integer.parseInt(args[0]);
		String supplies = (args[1]);
		String demands = (args[2]);
		String cost = (args[3]);
		
		OptimalTransport o = new OptimalTransport(n, supplies, demands, cost);
		
		
	}
}
