package cgu.ai.koldcube.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface SolveDao {
    @Insert
    long insertSolve(Solve solve);

    @Delete
    void deleteSolve(Solve solve);

    @Query("DELETE FROM solve")
    void deleteAll();

    @Update
    void updateSolve(Solve solve);

    @Query("SELECT * FROM solve ORDER BY date DESC")
    LiveData<List<Solve>> getAllSolves();

    @Query("SELECT * FROM solve ORDER BY date DESC LIMIT :limit")
    List<Solve> getRecentSolves(int limit);
}
