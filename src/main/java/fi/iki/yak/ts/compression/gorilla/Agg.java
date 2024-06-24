package fi.iki.yak.ts.compression.gorilla;

import java.util.Objects;

public class Agg {
    private long timestamp;
    private long sum;
    private long count;

    public Agg(long timestamp, long sum, long count) {
        this.timestamp = timestamp;
        this.sum = sum;
        this.count = count;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getLongSum() {
        return sum;
    }

    public long getLongCount() {
        return count;
    }

    public double getDoubleSum() {
        return Double.longBitsToDouble(sum);
    }

    public double getDoubleCount() {
        return Double.longBitsToDouble(count);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Agg agg = (Agg) o;
        return timestamp == agg.timestamp &&
                sum == agg.sum &&
                count == agg.count;
    }

    @Override
    public int hashCode() {

        return Objects.hash(timestamp, sum, count);
    }
}
