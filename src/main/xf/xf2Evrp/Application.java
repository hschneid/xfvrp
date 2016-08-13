package xf.xf2Evrp;

import xf.xfvrp.XFVRP;
import xf.xfvrp.base.LoadType;
import xf.xfvrp.opt.XFVRPOptType;
import xf.xfvrp.report.Report;
import xf.xfvrp.report.StringWriter;

public class Application {


	public static void main(String[] args) {
		new Application().start();
	}

	private void start() {
		Model model = new TestGenerator().generate();
		construct(model);
		System.out.println();
	}

	private void construct(Model m) {
		XFVRP vrp = new XFVRP();
		vrp.setMetric(m.metric);

		// Trivialer Vorlauf
		for (String[] sat : m.satellites) {
			vrp
			.addDepot()
			.setExternID(sat[0])
			.setXlong(Float.parseFloat(sat[1]))
			.setYlat(Float.parseFloat(sat[2]))
			.setOpen1(Float.parseFloat(sat[3]))
			.setClose1(Float.parseFloat(sat[4]));
		}
		
		for (String[] demF : m.demandsFull) {
			vrp.addCustomer()
			.setExternID(demF[0])
			.setXlong(Integer.parseInt(m.suppliers[Integer.parseInt(demF[1])][1]))
			.setYlat(Integer.parseInt(m.suppliers[Integer.parseInt(demF[1])][2]))
			.setLoadType(LoadType.PICKUP)
			.setDemand(1)
			.setOpen1(Float.parseFloat(demF[3]))
			.setClose1(100);
		}

		for (String[] demE : m.demandsEmpty) {
			vrp.addCustomer()
			.setExternID(demE[0])
			.setXlong(Integer.parseInt(m.suppliers[Integer.parseInt(demE[2])][1]))
			.setYlat(Integer.parseInt(m.suppliers[Integer.parseInt(demE[2])][2]))
			.setLoadType(LoadType.DELIVERY)
			.setDemand(1)
			.setOpen1(0)
			.setClose1(Float.parseFloat(demE[3]));
		}
		
		vrp.addVehicle()
		.setName("LKW")
		.setCapacity(new float[]{1});
		
		vrp.addOptType(XFVRPOptType.CONST);
		vrp.addOptType(XFVRPOptType.RELOCATE);
		
		vrp.executeRoutePlanning();
		
		Report rep = vrp.getReport();
		
		System.out.println(StringWriter.write(rep));

	}
}
