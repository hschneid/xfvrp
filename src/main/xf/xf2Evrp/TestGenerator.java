package xf.xf2Evrp;

import java.util.ArrayList;
import java.util.Random;

import xf.xfvrp.base.metric.EucledianMetric;
import xf.xfvrp.base.metric.MapMetric;
import xf.xfvrp.base.metric.Metric;

public class TestGenerator {

	private Random rand = new Random(1231);
	
	public Model generate() {
		Model m = new Model();
		
		// Facilities
		String[][] facilities = new String[3][5];
		for (int i = 0; i < facilities.length; i++) {
			facilities[i][0] = "F" + (i + 1);
			facilities[i][1] = getRand(45, 55) + "";
			facilities[i][2] = getRand(25 * (i + 1) - 5, 25 * (i + 1) + 5) + "";
			facilities[i][3] = 0 + "";
			facilities[i][4] = 100 + "";
		}
		m.facilities = facilities;
		
		// Satellites
		String[][] satellites = new String[4][5];
		for (int i = 0; i < 2; i++) {
			satellites[2 * i + 0][0] = "S" + (2 * i + 1);
			satellites[2 * i + 0][1] = getRand(50 * (i + 1) - 25 - 10, (50 * (i + 1) - 25) + 10) + "";
			satellites[2 * i + 0][2] = getRand(25 - 10, 25 + 10) + "";
			satellites[2 * i + 0][3] = 0 + "";
			satellites[2 * i + 0][4] = 100 + "";

			satellites[2 * i + 1][0] = "S" + (2 * i + 2);
			satellites[2 * i + 1][1] = getRand(50 * (i + 1) - 25 - 10, (50 * (i + 1) - 25) + 10) + "";
			satellites[2 * i + 1][2] = getRand(75 - 10, 75 + 10) + "";
			satellites[2 * i + 1][3] = 0 + "";
			satellites[2 * i + 1][4] = 100 + "";
		}
		m.satellites = satellites;
		
		// Suppliers
		String[][] suppliers = new String[50][3];
		for (int i = 0; i < suppliers.length; i++) {
			suppliers[i][0] = "C" + (i + 1);
			suppliers[i][1] = getRand(0, 100) + "";
			suppliers[i][2] = getRand(0, 100) + "";
		}
		m.suppliers = suppliers;
		
		// Demands
		m.demandsFull = new ArrayList<>();
		m.demandsEmpty = new ArrayList<>();
		int nbrOfDemands = 0;
		for (int j = 0; j < suppliers.length; j++) {
			boolean[] facs = new boolean[3];
			
			// Full
			int nbr = getRand(1, 3);
			for (int i = 0; i < nbr; i++) {
				int fac = -1;
				do {
					fac = getRand(0, 3 - 1);
				} while(facs[fac]);
				
				String[] demandFull = new String[6];
				demandFull[0] = "DF" + nbrOfDemands;
				demandFull[1] = j + "";
				demandFull[2] = fac + "";
				demandFull[3] = getRand(0, 20) + "";
				demandFull[4] = 100 + "";
				demandFull[5] = getRand(1, 3) + "";
				
				nbrOfDemands++;
				facs[fac] = true;
				
				m.demandsFull.add(demandFull);
			}
			
			// Empty
			facs = new boolean[3];
			nbr = getRand(1, 2);
			for (int i = 0; i < nbr; i++) {
				int fac = -1;
				do {
					fac = getRand(0, 3 - 1);
				} while(facs[fac]);
				
				String[] demandEmpty = new String[6];
				demandEmpty[0] = "DE" + nbrOfDemands;
				demandEmpty[1] = fac + "";
				demandEmpty[2] = j + "";
				demandEmpty[3] = getRand(0, 20) + "";
				demandEmpty[4] = 100 + "";
				demandEmpty[5] = 1 + "";
				
				nbrOfDemands++;
				facs[fac] = true;
				
				m.demandsEmpty.add(demandEmpty);
			}
		}
		
		// Metric
		m.metric = new EucledianMetric();
		
		return m;
	}
	
	private int getRand(int lowerBound, int upperBound) {
		return rand.nextInt((upperBound - lowerBound) + 1) + lowerBound;
	}
}
