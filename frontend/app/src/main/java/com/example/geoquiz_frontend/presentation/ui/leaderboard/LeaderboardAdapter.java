package com.example.geoquiz_frontend.presentation.ui.leaderboard;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.geoquiz_frontend.R;

import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

    private Context context;
    private List<LeaderboardEntry> entries;
    private String currentUserId;

    public LeaderboardAdapter(Context context, List<LeaderboardEntry> entries, String currentUserId) {
        this.context = context;
        this.entries = entries;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_leaderboard, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LeaderboardEntry entry = entries.get(position);
        holder.bind(entry, currentUserId);
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    public void updateList(List<LeaderboardEntry> newEntries) {
        this.entries = newEntries;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRank, tvMedal, tvPlayerName, tvLevel, tvScore, tvBadge;
        View cardPlayer;

        ViewHolder(View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tvRank);
            tvMedal = itemView.findViewById(R.id.tvMedal);
            tvPlayerName = itemView.findViewById(R.id.tvPlayerName);
            tvLevel = itemView.findViewById(R.id.tvLevel);
            tvScore = itemView.findViewById(R.id.tvScore);
            cardPlayer = itemView.findViewById(R.id.cardPlayer);
            tvBadge = itemView.findViewById(R.id.tvYouBadge);
        }

        void bind(LeaderboardEntry entry, String currentUserId) {
            // Место
            int rank = entry.getRank();
            tvRank.setText(String.valueOf(rank));

            // Медалька для топ-3
            if (rank == 1) {
                tvMedal.setVisibility(View.VISIBLE);
                tvMedal.setText("🥇");
                tvRank.setVisibility(View.GONE);
            } else if (rank == 2) {
                tvMedal.setVisibility(View.VISIBLE);
                tvMedal.setText("🥈");
                tvRank.setVisibility(View.GONE);
            } else if (rank == 3) {
                tvMedal.setVisibility(View.VISIBLE);
                tvMedal.setText("🥉");
                tvRank.setVisibility(View.GONE);
            } else {
                tvMedal.setVisibility(View.GONE);
                tvRank.setVisibility(View.VISIBLE);
            }

            // Имя
            tvPlayerName.setText(entry.getPlayerName());

            // Уровень
            tvLevel.setText("Lvl " + entry.getLevel());

            // Очки
            tvScore.setText(String.valueOf(entry.getTotalScore()));

            // Выделяем текущего игрока
            if (entry.getPlayerId().equals(currentUserId)) {
                tvBadge.setVisibility(View.VISIBLE);
            } else {
                tvBadge.setVisibility(View.GONE);
            }
        }
    }
}