package utils;

public class Frame {

    float[] corners; // top left, lower left, bottom right, upper right
    float r;
    float cx, cy;

    public Frame(float[] corners, float r, float cx, float cy) {
        if (corners.length != 8)
            Utils.handleException(new Exception("Invalid corners given for Frame. Length should be 8, is actually " +
                    corners.length), "Frame", "Frame(float[]", true);
        this.corners = corners;
        this.r = r;
        this.cx = cx;
        this.cy = cy;
    }

    public boolean contains(float x, float y) {
        Pair rp = Utils.rotatePoint(this.cx, this.cy, x, y, this.r);
        x = rp.x; y = rp.y;
        return (corners[0] < x && corners[1] > y &&
                corners[2] < x && corners[3] < y &&
                corners[4] > x && corners[5] < y &&
                corners[6] > x && corners[7] > y);
    }

    public Frame translate(float x, float y) {
        this.cx += x;
        this.cy += y;
        for (int i = 0; i < corners.length; i++) corners[i] += i % 2 == 0 ? x : y;
        return this;
    }
}
