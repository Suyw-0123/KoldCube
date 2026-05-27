package cgu.ai.koldcube.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "solve")
public class Solve {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public long time;       // raw milliseconds
    public String scramble;
    public long date;       // System.currentTimeMillis()
    public String status;   // "OK", "+2", "DNF"

    public Solve(long time, String scramble, long date, String status) {
        this.time = time;
        this.scramble = scramble;
        this.date = date;
        this.status = status;
    }

    public double getDisplayTime() {
        if ("DNF".equals(status)) return Double.MAX_VALUE;
        double seconds = time / 1000.0;
        if ("+2".equals(status)) seconds += 2.0;
        return seconds;
    }
}
