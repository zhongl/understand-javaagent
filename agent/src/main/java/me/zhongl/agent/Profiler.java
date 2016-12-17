package me.zhongl.agent;

import net.bytebuddy.asm.Advice;

import java.util.LinkedList;
import java.util.List;

public class Profiler {
    private final static ThreadLocal<Frame> CURRENT = new ThreadLocal<>();

    @Advice.OnMethodEnter(inline = false)
    public static long enter() {
        final Frame frame = CURRENT.get();
        if (frame == null) {
            CURRENT.set(new Frame());
        } else {
            CURRENT.set(frame.in());
        }
        return System.nanoTime();
    }

    @Advice.OnMethodExit(inline = false)
    public static void exit(@Advice.Origin String method, @Advice.Enter long begin) {
        final Frame frame = CURRENT.get();
        final Frame parent = frame.out(method, System.nanoTime() - begin);
        if (parent != null) {
            CURRENT.set(parent);
        } else {
            System.out.println(frame.toString());
        }
    }

    public static class Frame {
        private final Frame       parent;
        private final List<Frame> children;
        private final int         level;

        String method;
        long   elapseNanos;

        Frame() {
            this(null, 1);
        }

        Frame(Frame parent, int level) {
            this.parent = parent;
            this.level = level;
            this.children = new LinkedList<>();
        }

        Frame in() {
            final Frame frame = new Frame(this, level + 1);
            children.add(frame);
            return frame;
        }

        Frame out(String method, long elapseNanos) {
            this.method = method;
            this.elapseNanos = elapseNanos;
            return parent;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append('[').append(elapseNanos).append(']').append(method);
            final int size = children.size();
            for (int i = 0; i < size; i++) {
                sb.append('\n');
                for (int j = 0; j < level; j++) {
                    sb.append('\t');
                }
                if (i == size - 1) sb.append("└── ");
                else sb.append("├── ");
                sb.append(children.get(i));
            }
            return sb.toString();
        }
    }
}