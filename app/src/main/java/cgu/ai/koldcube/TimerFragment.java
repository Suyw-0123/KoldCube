package cgu.ai.koldcube;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import cgu.ai.koldcube.databinding.FragmentTimerBinding;
import cgu.ai.koldcube.db.Solve;
import cgu.ai.koldcube.scramble.ScrambleGenerator;
import cgu.ai.koldcube.util.StatisticsUtil;
import cgu.ai.koldcube.viewmodel.SolveViewModel;

import java.util.Locale;

public class TimerFragment extends Fragment {

    private enum State { IDLE, INSPECTION, RUNNING, RESULT }

    private FragmentTimerBinding binding;
    private SolveViewModel viewModel;
    private SharedPreferences prefs;

    private State state = State.IDLE;
    private String currentScramble = "";
    private Solve currentSolve;

    // Inspection
    private static final int INSPECTION_SECONDS = 15;
    private int inspectionSecondsLeft;
    private final Handler handler = new Handler(Looper.getMainLooper());

    // Long-press detection
    private static final long HOLD_THRESHOLD_MS = 500;
    private long touchDownTime;
    private boolean holdReady = false;
    private boolean touchStartedFromIdle = false; // first tap that entered INSPECTION
    private String inspectionPenalty = null; // "+2" if inspection ran 15-17s
    private final Runnable holdReadyRunnable = () -> {
        holdReady = true;
        binding.tvTimer.setTextColor(ContextCompat.getColor(requireContext(), R.color.timer_ready));
    };

    // Running timer
    private long startElapsed;
    private long pausedElapsed;
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long elapsed = SystemClock.elapsedRealtime() - startElapsed;
            binding.tvTimer.setText(formatElapsed(elapsed));
            handler.postDelayed(this, 10);
        }
    };

    // Inspection countdown
    private final Runnable inspectionRunnable = new Runnable() {
        @Override
        public void run() {
            inspectionSecondsLeft--;
            int display = inspectionSecondsLeft;
            if (display >= 0) {
                binding.tvTimer.setText(String.valueOf(display));
            } else {
                binding.tvTimer.setText(display == -1 ? "+2" : "DNF");
            }
            handler.postDelayed(this, 1000);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTimerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(SolveViewModel.class);
        prefs = requireContext().getSharedPreferences(
                SettingsFragment.PREFS_NAME, Context.MODE_PRIVATE);

        refreshScramble();
        resetToIdle();
        setupTouchListener();
        setupResultButtons();

        binding.btnHistory.setOnClickListener(v ->
                ((MainActivity) requireActivity()).navigateTo(MainActivity.PAGE_HISTORY));
    }

    private void setupTouchListener() {
        binding.tvTimer.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    return onTouchDown();
                case MotionEvent.ACTION_UP:
                    return onTouchUp();
            }
            return false;
        });
    }

    private boolean onTouchDown() {
        switch (state) {
            case IDLE:
                if (isInspectionEnabled()) {
                    touchStartedFromIdle = true;
                    startInspection();
                } else {
                    // No WCA inspection: hold-to-ready then release starts timing directly.
                    armDirectStart();
                    beginHold();
                }
                return true;
            case INSPECTION:
                touchStartedFromIdle = false;
                beginHold();
                return true;
            case RUNNING:
                stopTimer();
                return true;
            case RESULT:
                resetToIdle();
                return true;
        }
        return false;
    }

    private boolean onTouchUp() {
        if (state == State.INSPECTION) {
            // If this touch-up is from the initial tap that entered inspection, ignore hold logic
            if (touchStartedFromIdle) {
                touchStartedFromIdle = false;
                return true;
            }
            handler.removeCallbacks(holdReadyRunnable);
            long heldMs = SystemClock.elapsedRealtime() - touchDownTime;
            if (heldMs < HOLD_THRESHOLD_MS) {
                // Not long enough — restore inspection color and continue
                binding.tvTimer.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
                return true;
            }
            // Long press confirmed — check inspection overrun (WCA: >15s=+2, >17s=DNF)
            if (inspectionSecondsLeft >= 0) {
                startRunning();
            } else if (inspectionSecondsLeft >= -2) {
                // 15–17 seconds elapsed → start timer with +2 penalty applied at stop
                inspectionPenalty = "+2";
                startRunning();
            } else {
                // >17 seconds elapsed → DNF, no timer
                autoRecord("DNF");
            }
            return true;
        }
        return false;
    }

    private boolean isInspectionEnabled() {
        return prefs.getBoolean(SettingsFragment.KEY_INSPECTION, SettingsFragment.DEFAULT_INSPECTION);
    }

    /**
     * Enter the "armed" state used when WCA inspection is disabled: no countdown
     * runs, so the subsequent hold-and-release in onTouchUp starts timing
     * immediately with no penalty (inspectionSecondsLeft stays >= 0).
     */
    private void armDirectStart() {
        state = State.INSPECTION;
        inspectionSecondsLeft = INSPECTION_SECONDS;
        inspectionPenalty = null;
        touchStartedFromIdle = false;
        binding.tvScramble.setVisibility(View.GONE);
        binding.btnHistory.setVisibility(View.GONE);
    }

    private void beginHold() {
        touchDownTime = SystemClock.elapsedRealtime();
        holdReady = false;
        binding.tvTimer.setTextColor(ContextCompat.getColor(requireContext(), R.color.timer_holding));
        handler.postDelayed(holdReadyRunnable, HOLD_THRESHOLD_MS);
    }

    private void startInspection() {
        state = State.INSPECTION;
        inspectionSecondsLeft = INSPECTION_SECONDS;
        inspectionPenalty = null;
        binding.tvScramble.setVisibility(View.GONE);
        binding.btnHistory.setVisibility(View.GONE);
        binding.tvTimer.setText(String.valueOf(INSPECTION_SECONDS));
        binding.tvTimer.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        handler.postDelayed(inspectionRunnable, 1000);
    }

    private void startRunning() {
        handler.removeCallbacks(inspectionRunnable);
        state = State.RUNNING;
        startElapsed = SystemClock.elapsedRealtime();
        binding.tvTimer.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        binding.tvTimer.setText("0.00");
        startLoadingAnim();
        handler.post(timerRunnable);
    }

    private void stopTimer() {
        handler.removeCallbacks(timerRunnable);
        stopLoadingAnim();
        pausedElapsed = SystemClock.elapsedRealtime() - startElapsed;
        state = State.RESULT;

        String status = inspectionPenalty != null ? inspectionPenalty : "OK";
        Solve solve = new Solve(pausedElapsed, currentScramble, System.currentTimeMillis(), status);
        currentSolve = solve;
        viewModel.insertSolve(solve, id -> currentSolve.id = (int) (long) id);

        playFinishSound();
        showResult();
    }

    private void playFinishSound() {
        if (!prefs.getBoolean(SettingsFragment.KEY_SOUND, SettingsFragment.DEFAULT_SOUND)) {
            return;
        }
        try {
            final ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
            tone.startTone(ToneGenerator.TONE_PROP_BEEP, 200);
            handler.postDelayed(tone::release, 300);
        } catch (RuntimeException ignored) {
            // ToneGenerator can throw if audio resources are unavailable; ignore.
        }
    }

    private void autoRecord(String status) {
        handler.removeCallbacks(inspectionRunnable);
        state = State.RESULT;
        Solve solve = new Solve(0, currentScramble, System.currentTimeMillis(), status);
        currentSolve = solve;
        viewModel.insertSolve(solve, id -> currentSolve.id = (int) (long) id);
        showResult();
    }

    private void showResult() {
        String timeText = StatisticsUtil.formatTime(currentSolve.time, currentSolve.status);
        binding.tvTimer.setText(timeText);
        binding.tvTimer.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        binding.layoutResultButtons.setVisibility(View.VISIBLE);
        binding.btnHistory.setVisibility(View.VISIBLE);
        updateStatusButtonHighlight(currentSolve.status);
    }

    private void setupResultButtons() {
        binding.btnOk.setOnClickListener(v -> setStatus("OK"));
        binding.btnPlus2.setOnClickListener(v -> setStatus("+2"));
        binding.btnDnf.setOnClickListener(v -> setStatus("DNF"));
        binding.btnDelete.setOnClickListener(v -> {
            if (currentSolve != null) viewModel.deleteSolve(currentSolve);
            resetToIdle();
        });
    }

    private void setStatus(String status) {
        if (currentSolve == null) return;
        currentSolve.status = status;
        viewModel.updateSolve(currentSolve);
        binding.tvTimer.setText(StatisticsUtil.formatTime(currentSolve.time, status));
        updateStatusButtonHighlight(status);
    }

    private void updateStatusButtonHighlight(String status) {
        int dim = 0x80FFFFFF; // semi-transparent white
        binding.btnOk.setTextColor("OK".equals(status)
                ? ContextCompat.getColor(requireContext(), R.color.status_ok) : dim);
        binding.btnPlus2.setTextColor("+2".equals(status)
                ? ContextCompat.getColor(requireContext(), R.color.status_plus2) : dim);
        binding.btnDnf.setTextColor("DNF".equals(status)
                ? ContextCompat.getColor(requireContext(), R.color.status_dnf) : dim);
    }

    private void startLoadingAnim() {
        binding.ivLoadingAnim.setVisibility(View.VISIBLE);
        Drawable d = binding.ivLoadingAnim.getDrawable();
        if (d instanceof AnimationDrawable) {
            ((AnimationDrawable) d).start();
        }
    }

    private void stopLoadingAnim() {
        Drawable d = binding.ivLoadingAnim.getDrawable();
        if (d instanceof AnimationDrawable) {
            ((AnimationDrawable) d).stop();
        }
        binding.ivLoadingAnim.setVisibility(View.GONE);
    }

    private void resetToIdle() {
        handler.removeCallbacksAndMessages(null);
        stopLoadingAnim();
        state = State.IDLE;
        currentSolve = null;
        holdReady = false;
        binding.tvTimer.setText("0.00");
        binding.tvTimer.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        binding.layoutResultButtons.setVisibility(View.GONE);
        binding.btnHistory.setVisibility(View.VISIBLE);
        refreshScramble();
    }

    private void refreshScramble() {
        new Thread(() -> {
            String s = ScrambleGenerator.getScramble333();
            handler.post(() -> {
                currentScramble = s;
                if (binding != null) {
                    binding.tvScramble.setText(s);
                    binding.tvScramble.setVisibility(View.VISIBLE);
                }
            });
        }).start();
    }

    private static String formatElapsed(long millis) {
        return String.format(Locale.US, "%.2f", millis / 1000.0);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        binding = null;
    }
}
