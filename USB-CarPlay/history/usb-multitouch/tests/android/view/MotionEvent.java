package android.view;
public final class MotionEvent {
    private final int action, index;
    private final int[] ids;
    private final float[] xs, ys;
    public MotionEvent(int action, int index, int[] ids, float[] xs, float[] ys) {
        this.action = action; this.index = index; this.ids = ids; this.xs = xs; this.ys = ys;
    }
    public int getActionMasked() { return action; }
    public int getActionIndex() { return index; }
    public int getPointerCount() { return ids.length; }
    public int getPointerId(int index) { return ids[index]; }
    public float getX(int index) { return xs[index]; }
    public float getY(int index) { return ys[index]; }
}
