package cgu.ai.koldcube;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import cgu.ai.koldcube.adapter.SolveAdapter;
import cgu.ai.koldcube.databinding.FragmentHistoryBinding;
import cgu.ai.koldcube.db.Solve;
import cgu.ai.koldcube.util.StatisticsUtil;
import cgu.ai.koldcube.viewmodel.SolveViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryFragment extends Fragment {
    private FragmentHistoryBinding binding;
    private SolveViewModel viewModel;
    private SolveAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(SolveViewModel.class);

        adapter = new SolveAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(this::showDetailDialog);
        setupSwipeToDelete();

        viewModel.getAllSolves().observe(getViewLifecycleOwner(), solves -> {
            List<Solve> list = solves != null ? solves : new ArrayList<>();
            adapter.setSolves(list);
            updateStats(list);
        });

        binding.btnBackToTimer.setOnClickListener(v ->
                ((MainActivity) requireActivity()).navigateTo(MainActivity.PAGE_TIMER));
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback swipeCallback =
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                    @Override
                    public boolean onMove(@NonNull RecyclerView rv,
                                         @NonNull RecyclerView.ViewHolder vh,
                                         @NonNull RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                        Solve solve = adapter.getSolveAt(viewHolder.getBindingAdapterPosition());
                        viewModel.deleteSolve(solve);
                    }
                };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.recyclerView);
    }

    private void updateStats(List<Solve> solves) {
        String best  = StatisticsUtil.getBestSingle(solves);
        String ao5   = StatisticsUtil.calculateAo(solves, 5);
        String ao12  = StatisticsUtil.calculateAo(solves, 12);
        String ao100 = StatisticsUtil.calculateAo(solves, 100);
        String bestAo5   = StatisticsUtil.getBestAo(solves, 5);
        String bestAo12  = StatisticsUtil.getBestAo(solves, 12);
        String bestAo100 = StatisticsUtil.getBestAo(solves, 100);

        binding.tvBestSingle.setText("Best\n" + best);
        binding.tvAo5.setText("ao5\n" + ao5);
        binding.tvAo12.setText("ao12\n" + ao12);
        binding.tvAo100.setText("ao100\n" + ao100);
        binding.tvBestAo5.setText("Best ao5\n" + bestAo5);
        binding.tvBestAo12.setText("Best ao12\n" + bestAo12);
        binding.tvBestAo100.setText("Best ao100\n" + bestAo100);
    }

    private void showDetailDialog(Solve solve) {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_solve_detail);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvTime    = dialog.findViewById(R.id.tvDetailTime);
        TextView tvStatus  = dialog.findViewById(R.id.tvDetailStatus);
        TextView tvScramble = dialog.findViewById(R.id.tvDetailScramble);
        TextView tvDate    = dialog.findViewById(R.id.tvDetailDate);
        MaterialButton btnClose = dialog.findViewById(R.id.btnDetailClose);

        tvTime.setText(StatisticsUtil.formatTime(solve.time, solve.status));
        tvStatus.setText(solve.status);
        tvScramble.setText(solve.scramble);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault());
        tvDate.setText(sdf.format(new Date(solve.date)));

        int statusColor;
        switch (solve.status) {
            case "+2":
                statusColor = ContextCompat.getColor(requireContext(), R.color.status_plus2);
                break;
            case "DNF":
                statusColor = ContextCompat.getColor(requireContext(), R.color.status_dnf);
                break;
            default:
                statusColor = ContextCompat.getColor(requireContext(), R.color.status_ok);
                break;
        }
        tvStatus.setTextColor(statusColor);

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
