package net.diexv.potionenchant.entity;

/**
 * 单条激光束的服务端 -> 客户端同步数据（多束并存）。
 */
public class LaserBeamData {
    public float progress;
    public float originX;
    public float originY;
    public float originZ;
    public float dirX;
    public float dirY;
    public float dirZ;

    public LaserBeamData() {
    }

    public LaserBeamData(float progress, float originX, float originY, float originZ,
                         float dirX, float dirY, float dirZ) {
        this.progress = progress;
        this.originX = originX;
        this.originY = originY;
        this.originZ = originZ;
        this.dirX = dirX;
        this.dirY = dirY;
        this.dirZ = dirZ;
    }

    public LaserBeamData copy() {
        return new LaserBeamData(progress, originX, originY, originZ, dirX, dirY, dirZ);
    }
}
