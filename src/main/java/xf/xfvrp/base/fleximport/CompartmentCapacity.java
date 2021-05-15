package xf.xfvrp.base.fleximport;

public class CompartmentCapacity {

    private float onlyPickupCapacity = Float.MAX_VALUE;
    private float onlyDeliveryCapacity = Float.MAX_VALUE;
    private float deliveryAndPickupCapacity = Float.MAX_VALUE;

    public CompartmentCapacity() {
    }

    public CompartmentCapacity(float simpleCapacityValue) {
        this.onlyPickupCapacity = simpleCapacityValue;
        this.onlyDeliveryCapacity = simpleCapacityValue;
        this.deliveryAndPickupCapacity = simpleCapacityValue;
    }

    public float[] asArray() {
        return new float[]{onlyPickupCapacity, onlyDeliveryCapacity, deliveryAndPickupCapacity};
    }

    public float getOnlyPickupCapacity() {
        return onlyPickupCapacity;
    }

    public void setOnlyPickupCapacity(float onlyPickupCapacity) {
        this.onlyPickupCapacity = onlyPickupCapacity;
    }

    public float getOnlyDeliveryCapacity() {
        return onlyDeliveryCapacity;
    }

    public void setOnlyDeliveryCapacity(float onlyDeliveryCapacity) {
        this.onlyDeliveryCapacity = onlyDeliveryCapacity;
    }

    public float getDeliveryAndPickupCapacity() {
        return deliveryAndPickupCapacity;
    }

    public void setDeliveryAndPickupCapacity(float deliveryAndPickupCapacity) {
        this.deliveryAndPickupCapacity = deliveryAndPickupCapacity;
    }
}
