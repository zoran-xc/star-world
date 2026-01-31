package top.xcyyds.starworld.common.npc.control;

public interface NpcControlApi {
    void setMoveInput(float forward, float strafe);

    void setJumping(boolean jumping);

    void setSprinting(boolean sprinting);

    void setSneaking(boolean sneaking);

    void swingMainHand();

    void swingOffhand();
}
