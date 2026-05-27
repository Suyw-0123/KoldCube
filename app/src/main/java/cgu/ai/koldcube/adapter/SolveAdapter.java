package cgu.ai.koldcube.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import cgu.ai.koldcube.R;
import cgu.ai.koldcube.db.Solve;
import cgu.ai.koldcube.util.StatisticsUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SolveAdapter extends RecyclerView.Adapter<SolveAdapter.ViewHolder> {
    private List<Solve> solves = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Solve solve);
    }

    public void setOnItemClickListener(OnItemClickListener l) {
        this.listener = l;
    }

    public void setSolves(List<Solve> newSolves) {
        this.solves = newSolves != null ? newSolves : new ArrayList<>();
        notifyDataSetChanged();
    }

    public Solve getSolveAt(int position) {
        return solves.get(position);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_solve, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Solve solve = solves.get(position);
        holder.bind(solve, listener);
    }

    @Override
    public int getItemCount() {
        return solves.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIndex, tvTime, tvStatus, tvDate;

        ViewHolder(View itemView) {
            super(itemView);
            tvIndex = itemView.findViewById(R.id.tvIndex);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDate = itemView.findViewById(R.id.tvDate);
        }

        void bind(Solve solve, OnItemClickListener listener) {
            tvIndex.setText(String.valueOf(getBindingAdapterPosition() + 1));
            tvTime.setText(StatisticsUtil.formatTime(solve.time, solve.status));
            tvStatus.setText(solve.status);

            int statusColor;
            switch (solve.status) {
                case "+2":
                    statusColor = ContextCompat.getColor(itemView.getContext(), R.color.status_plus2);
                    break;
                case "DNF":
                    statusColor = ContextCompat.getColor(itemView.getContext(), R.color.status_dnf);
                    break;
                default:
                    statusColor = ContextCompat.getColor(itemView.getContext(), R.color.status_ok);
                    break;
            }
            tvStatus.setTextColor(statusColor);

            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault());
            tvDate.setText(sdf.format(new Date(solve.date)));

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(solve);
            });
        }
    }
}
