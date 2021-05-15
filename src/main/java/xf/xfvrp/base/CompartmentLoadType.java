package xf.xfvrp.base;

public enum CompartmentLoadType {

    PICKUP(0), // On route only pickups are done
    DELIVERY(1), // Route is only delivering stuff
    MIXED(2); // Route is picking up and deliverying

    private final int index;
    public static final int NBR_OF_LOAD_TYPES = 3;

    CompartmentLoadType(int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }
}
