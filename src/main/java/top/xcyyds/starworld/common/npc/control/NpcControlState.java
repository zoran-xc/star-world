package top.xcyyds.starworld.common.npc.control;

public final class NpcControlState {
    public float forward;
    public float strafe;

    public boolean jumping;
    public boolean sprinting;
    public boolean sneaking;

    public boolean swingMainHand;
    public boolean swingOffhand;

    public void clearTransient() {
        swingMainHand = false;
        swingOffhand = false;
    }
}
