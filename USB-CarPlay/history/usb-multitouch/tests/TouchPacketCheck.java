import android.view.MotionEvent;
import cn.manstep.phonemirrorBox.BoxInterface.e;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class TouchPacketCheck {
    private static ByteBuffer packet(int action, int index, int[] ids, float[] xs, float[] ys, int length) {
        e encoder = new e();
        if (!encoder.b(new MotionEvent(action, index, ids, xs, ys), 10, 20, 1000, 500))
            throw new AssertionError("Event rejected");
        if (encoder.a() != length) throw new AssertionError("Unexpected packet length");
        ByteBuffer result = ByteBuffer.allocate(encoder.a()).order(ByteOrder.LITTLE_ENDIAN);
        encoder.c(result, 0);
        return result;
    }
    private static void contact(ByteBuffer bytes, int slot, float x, float y, int action, int id) {
        int offset = slot * 16;
        if (Math.abs(bytes.getFloat(offset) - x) > 0.00001 ||
            Math.abs(bytes.getFloat(offset + 4) - y) > 0.00001 ||
            bytes.getInt(offset + 8) != action || bytes.getInt(offset + 12) != id)
            throw new AssertionError("Incorrect coordinates, action, or pointer identity");
    }
    public static void main(String[] args) {
        contact(packet(0, 0, new int[]{0}, new float[]{210}, new float[]{270}, 16), 0, .2f, .5f, 1, 0);
        int[] ids = {0, 1}; float[] xs = {210, 810}; float[] ys = {270, 270};
        contact(packet(5, 1, ids, xs, ys, 16), 0, .8f, .5f, 1, 1);
        ByteBuffer move = packet(2, 0, ids, new float[]{110, 910}, ys, 32);
        contact(move, 0, .1f, .5f, 2, 0);
        contact(move, 1, .9f, .5f, 2, 1);
        contact(packet(6, 1, ids, xs, ys, 16), 0, .8f, .5f, 0, 1);
        contact(packet(1, 0, new int[]{0}, new float[]{210}, new float[]{270}, 16), 0, .2f, .5f, 0, 0);
        ByteBuffer cancel = packet(3, 0, ids, xs, ys, 32);
        contact(cancel, 0, .2f, .5f, 0, 0); contact(cancel, 1, .8f, .5f, 0, 1);
        System.out.println("PASS: actual decompiled encoder, 6 event scenarios / 8 contact assertions.");
        System.out.println("Scope: host-side payload only; USB box and iPhone not tested.");
    }
}
