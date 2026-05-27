package cgu.ai.koldcube.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import cgu.ai.koldcube.db.AppDatabase;
import cgu.ai.koldcube.db.Solve;
import cgu.ai.koldcube.db.SolveDao;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class SolveViewModel extends AndroidViewModel {
    private final SolveDao dao;
    private final LiveData<List<Solve>> allSolves;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public SolveViewModel(@NonNull Application application) {
        super(application);
        dao = AppDatabase.getInstance(application).solveDao();
        allSolves = dao.getAllSolves();
    }

    public LiveData<List<Solve>> getAllSolves() {
        return allSolves;
    }

    public void insertSolve(Solve solve, Consumer<Long> callback) {
        executor.execute(() -> {
            long id = dao.insertSolve(solve);
            if (callback != null) {
                mainHandler.post(() -> callback.accept(id));
            }
        });
    }

    public void updateSolve(Solve solve) {
        executor.execute(() -> dao.updateSolve(solve));
    }

    public void deleteSolve(Solve solve) {
        executor.execute(() -> dao.deleteSolve(solve));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
