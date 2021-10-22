package xf.xfvrp.base

import spock.lang.Specification
import util.instances.TestVehicle
import util.instances.TestXFVRPModel
import xf.xfvrp.opt.Solution

class NormalizeSpec extends Specification {

	def service = new NormalizeSolutionService()
	def sol = new Solution()
	
	def "Regular normalize - no empty routes"() {
		def model = createModel()
		def n = model.getNodes()
		sol.setGiantRoute([n[0], n[3], n[4], n[1], n[5], n[6], n[7], n[1]] as Node[])
				
		when:
		sol = service.normalizeRoute(sol, model)
		
		def result = sol.getGiantRoute()
		
		then:
		result.length == 11 
		result[0].externID == "1"
		result[1].externID == "2"
		result[2].externID == "3"
		result[3].externID == "4"
		result[4].externID == "5"
		result[5].externID == "6"
		result[6].externID == "7"
		result[7].externID == "1"
		result[8].externID == "4"
		result[9].externID == "8"
		result[10].externID == "8"
	}
	
	def "Regular normalize - one empty route"() {
		def model = createModel()

		def n = model.getNodes()
		sol.setGiantRoute([n[0], n[3], n[4], n[2], n[1], n[5], n[6], n[7], n[1]] as Node[])
				
		when:
		sol = service.normalizeRoute(sol, model)
		
		def result = sol.getGiantRoute()
		
		then:
		result.length == 11 
		result[0].externID == "1"
		result[1].externID == "2"
		result[2].externID == "3"
		result[3].externID == "1"
		result[4].externID == "4"
		result[5].externID == "5"
		result[6].externID == "6"
		result[7].externID == "7"
		result[8].externID == "4"
		result[9].externID == "8"
		result[10].externID == "8"
	}
	
	def "Regular normalize - two empty routes (1)"() {
		def model = createModel()

		def n = model.getNodes()
		sol.setGiantRoute([n[1], n[0], n[3], n[4], n[2], n[1], n[5], n[6], n[7], n[1]] as Node[])
				
		when:
		sol = service.normalizeRoute(sol, model)
		
		def result = sol.getGiantRoute()
		
		then:
		result.length == 11
		result[0].externID == "1"
		result[1].externID == "1"
		result[2].externID == "2"
		result[3].externID == "3"
		result[4].externID == "4"
		result[5].externID == "4"
		result[6].externID == "5"
		result[7].externID == "6"
		result[8].externID == "7"
		result[9].externID == "8"
		result[10].externID == "8"
	}
	
	def "Regular normalize - two empty routes (2)"() {
		def model = createModel()

		def n = model.getNodes()
		sol.setGiantRoute([n[1], n[1], n[0], n[3], n[4], n[2], n[2], n[1], n[5], n[6], n[7], n[1]] as Node[])
				
		when:
		sol = service.normalizeRoute(sol, model)
		
		def result = sol.getGiantRoute()
		
		then:
		result.length == 11
		result[0].externID == "1"
		result[1].externID == "4"
		result[2].externID == "1"
		result[3].externID == "2"
		result[4].externID == "3"
		result[5].externID == "8"
		result[6].externID == "4"
		result[7].externID == "5"
		result[8].externID == "6"
		result[9].externID == "7"
		result[10].externID == "4"
	}
	
	def "Regular normalize - With replenishs"() {
		def model = createModelWithReplenishs()

		def n = model.getNodes()
		sol.setGiantRoute([n[0], n[5], n[3], n[6], n[1], n[7], n[4], n[8], n[9], n[1]] as Node[])
				
		when:
		sol = service.normalizeRoute(sol, model)
		
		def result = sol.getGiantRoute()
		
		then:
		result.length == 16
		result[0].externID == "1"
		result[1].externID == "2"
		result[2].externID == "R1"
		result[3].externID == "3"
		result[4].externID == "4"
		result[5].externID == "5"
		result[6].externID == "R2"
		result[7].externID == "6"
		result[8].externID == "7"
		result[9].externID == "1"
		result[10].externID == "4"
		result[11].externID == "8"
		result[12].externID == "1"
		result[13].externID == "R1"
		result[14].externID == "R2"
		result[15].externID == "1"
	}
	
	def "Regular normalize - With empty replenishs"() {
		def model = createModelWithReplenishs()

		def n = model.getNodes()
		sol.setGiantRoute([n[1], n[0], n[5], n[6], n[3], n[2], n[1], n[4], n[7], n[8], n[9], n[1]] as Node[])
				
		when:
		sol = service.normalizeRoute(sol, model)
		
		def result = sol.getGiantRoute()
		
		then:
		result.length == 16
		result[0].externID == "1"
		result[1].externID == "1"
		result[2].externID == "2"
		result[3].externID == "3"
		result[4].externID == "R1"
		result[5].externID == "4"
		result[6].externID == "4"
		result[7].externID == "R2"
		result[8].externID == "5"
		result[9].externID == "6"
		result[10].externID == "7"
		result[11].externID == "8"
		result[12].externID == "1"
		result[13].externID == "R1"
		result[14].externID == "R2"
		result[15].externID == "1"
	}
	
	def "Regular normalize - With more empty replenishs"() {
		def model = createModelWithReplenishs()

		def n = model.getNodes()
		sol.setGiantRoute([n[1], n[0], n[5], n[3], n[3], n[6], n[2], n[1], n[4], n[7], n[8], n[9], n[1]] as Node[])
				
		when:
		sol = service.normalizeRoute(sol, model)
		
		def result = sol.getGiantRoute()
		
		then:
		result.length == 16
		result[0].externID == "1"
		result[1].externID == "1"
		result[2].externID == "2"
		result[3].externID == "R1"
		result[4].externID == "3"
		result[5].externID == "4"
		result[6].externID == "4"
		result[7].externID == "R2"
		result[8].externID == "5"
		result[9].externID == "6"
		result[10].externID == "7"
		result[11].externID == "8"
		result[12].externID == "1"
		result[13].externID == "R1"
		result[14].externID == "R2"
		result[15].externID == "1"
	}
	
	def "Irregular normalize - model null"() {
		def model = createModel()
		def n = model.getNodes()
		sol.setGiantRoute([n[0], n[3], n[4], n[1], n[5], n[6], n[7], n[1]] as Node[])
				
		when:
		sol = service.normalizeRoute(sol, null)
		
		def result = sol.getGiantRoute()
		
		then:
		thrown NullPointerException
	}
	
	def "Irregular normalize - giant route null"() {
		def model = createModel()
		def n = model.getNodes()
		sol.setGiantRoute([n[0], n[3], n[4], n[1], n[5], n[6], n[7], n[1]] as Node[])
				
		when:
		sol = service.normalizeRoute(null, model)
		
		def result = sol.getGiantRoute()
		
		then:
		thrown NullPointerException
	}

	def "Irregular normalize - empty giant route"() {
		def model = createModel()
		def n = model.getNodes()
		sol.setGiantRoute([] as Node[])
				
		when:
		sol = service.normalizeRoute(sol, model)
		
		def result = sol.getGiantRoute()
		
		then:
		result.length == 4
		result[0].externID == "1"
		result[1].externID == "4"
		result[2].externID == "8"
		result[3].externID == "8"
	}
	
	private XFVRPModel createModel() {
		def n1 = new Node(externID: "1", siteType: SiteType.DEPOT)
		def n2 = new Node(externID: "2", siteType: SiteType.CUSTOMER)
		def n3 = new Node(externID: "3", siteType: SiteType.CUSTOMER)
		def n4 = new Node(externID: "4", siteType: SiteType.DEPOT)
		def n5 = new Node(externID: "5", siteType: SiteType.CUSTOMER)
		def n6 = new Node(externID: "6", siteType: SiteType.CUSTOMER)
		def n7 = new Node(externID: "7", siteType: SiteType.CUSTOMER)
		def n8 = new Node(externID: "8", siteType: SiteType.DEPOT)

		return TestXFVRPModel.get([n1, n4, n8, n2, n3, n5, n6, n7], new TestVehicle(capacity: [3,3]).getVehicle())
	}
	
	private XFVRPModel createModelWithReplenishs() {
		def n1 = new Node(externID: "1", siteType: SiteType.DEPOT)
		def n2 = new Node(externID: "2", siteType: SiteType.CUSTOMER)
		def n3 = new Node(externID: "3", siteType: SiteType.CUSTOMER)
		def n4 = new Node(externID: "4", siteType: SiteType.DEPOT)
		def n5 = new Node(externID: "5", siteType: SiteType.CUSTOMER)
		def n6 = new Node(externID: "6", siteType: SiteType.CUSTOMER)
		def n7 = new Node(externID: "7", siteType: SiteType.CUSTOMER)
		def n8 = new Node(externID: "8", siteType: SiteType.DEPOT)
		def n9 = new Node(externID: "R1", siteType: SiteType.REPLENISH)
		def n10 = new Node(externID: "R2", siteType: SiteType.REPLENISH)
		
		def model = TestXFVRPModel.get([n1, n4, n8, n9, n10, n2, n3, n5, n6, n7], new TestVehicle(capacity: [3,3]).getVehicle())

		return model
	}
}
