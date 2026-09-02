package xf.xfvrp.opt.improve.routebased

import spock.lang.Specification

class SearchCacheTest extends Specification {

    def "initial state - not initialized, needs evaluation for all"() {
        given:
        def cache = new SearchCache()

        expect:
        !cache.isInitialized()
        cache.needsEvaluation(0, 1)
        cache.needsEvaluation(5, 5)
        cache.size() == 0
    }

    def "after reset - needs evaluation for all"() {
        given:
        def cache = new SearchCache()
        cache.invalidateRoutes(0, 1)

        when:
        cache.reset()

        then:
        !cache.isInitialized()
        cache.needsEvaluation(0, 1)
        cache.size() == 0
    }

    def "after invalidateRoutes - only changed routes need evaluation"() {
        given:
        def cache = new SearchCache()

        when:
        cache.invalidateRoutes(2, 5)

        then:
        cache.isInitialized()
        cache.needsEvaluation(2, 3)
        cache.needsEvaluation(0, 5)
        cache.needsEvaluation(2, 5)
        !cache.needsEvaluation(0, 1)
        !cache.needsEvaluation(3, 4)
        !cache.needsEvaluation(6, 7)
    }

    def "setMoves and getMoves"() {
        given:
        def cache = new SearchCache()
        def move1 = [1.5f, 0f, 1f, 2f, 3f, 0f, 0f, 0f] as float[]
        def move2 = [0.8f, 2f, 3f, 4f, 5f, 0f, 0f, 0f] as float[]

        when:
        cache.setMoves(0, 1, [move1])
        cache.setMoves(2, 3, [move2])

        then:
        cache.size() == 2
        cache.getMoves(0, 1) == [move1]
        cache.getMoves(2, 3) == [move2]
        cache.getMoves(0, 1)[0][0] == 1.5f
        cache.getMoves(2, 3)[0][0] == 0.8f
    }

    def "setMoves with null removes entry"() {
        given:
        def cache = new SearchCache()
        cache.setMoves(0, 1, [[1.0f, 0f, 1f] as float[]])

        when:
        cache.setMoves(0, 1, null)

        then:
        cache.size() == 0
    }

    def "invalidateRoutes removes cached entries for changed routes"() {
        given:
        def cache = new SearchCache()
        cache.setMoves(0, 1, [[1.0f, 0f, 1f] as float[]])
        cache.setMoves(0, 2, [[2.0f, 0f, 2f] as float[]])
        cache.setMoves(1, 2, [[3.0f, 1f, 2f] as float[]])
        cache.setMoves(3, 4, [[4.0f, 3f, 4f] as float[]])

        when:
        cache.invalidateRoutes(0, 2)

        then:
        cache.size() == 1
        cache.getMoves(3, 4) != null
        cache.getMoves(3, 4)[0][0] == 4.0f
        cache.getMoves(0, 1) == null
        cache.getMoves(0, 2) == null
        cache.getMoves(1, 2) == null
    }

    def "addChangedRoute extends the dirty set"() {
        given:
        def cache = new SearchCache()
        cache.setMoves(3, 4, [[1.0f, 3f, 4f] as float[]])
        cache.invalidateRoutes(0, 1)

        when:
        cache.addChangedRoute(3)

        then:
        cache.needsEvaluation(3, 5)
        cache.needsEvaluation(0, 3)
        !cache.needsEvaluation(4, 5)
        cache.size() == 0
    }

    def "symmetric key - pair (a,b) and (b,a) same entry"() {
        given:
        def cache = new SearchCache()
        def move = [1.0f, 0f, 1f] as float[]

        when:
        cache.setMoves(3, 5, [move])

        then:
        // Overwriting with reverse pair should replace the same entry
        cache.size() == 1
        cache.setMoves(5, 3, [[2.0f, 5f, 3f] as float[]])
        cache.size() == 1
    }

    def "successive invalidations replace changed routes"() {
        given:
        def cache = new SearchCache()

        when:
        cache.invalidateRoutes(0, 1)

        then:
        cache.getChangedRoutes() == [0, 1] as Set

        when:
        cache.invalidateRoutes(3, 4)

        then:
        cache.getChangedRoutes() == [3, 4] as Set
        !cache.needsEvaluation(0, 1)
        cache.needsEvaluation(3, 5)
    }

    def "getMoves returns all moves stored for a pair"() {
        given:
        def cache = new SearchCache()
        cache.setMoves(0, 1, [[3.0f, 0f, 1f] as float[], [1.0f, 0f, 1f] as float[]])
        cache.setMoves(0, 2, [[1.5f, 0f, 2f] as float[]])
        cache.setMoves(1, 2, [[2.0f, 1f, 2f] as float[]])

        expect:
        cache.size() == 3
        cache.getMoves(0, 1).size() == 2
        cache.getMoves(0, 1)[0][0] == 3.0f
        cache.getMoves(0, 1)[1][0] == 1.0f
        cache.getMoves(0, 2).size() == 1
        cache.getMoves(1, 2).size() == 1
    }
}
