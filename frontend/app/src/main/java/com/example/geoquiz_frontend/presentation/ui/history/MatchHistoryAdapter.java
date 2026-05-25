package com.example.geoquiz_frontend.presentation.ui.history;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.geoquiz_frontend.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MatchHistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SOLO = 1;
    private static final int TYPE_PVP = 2;
    private static final int TYPE_KOTH = 3;

    private Context context;
    private List<MatchHistoryEntry> entries;
    private long serverTimeOffset;

    public MatchHistoryAdapter(Context context, List<MatchHistoryEntry> entries, long serverTimeOffset) {
        this.context = context;
        this.entries = entries;
        this.serverTimeOffset = serverTimeOffset;
    }

    @Override
    public int getItemViewType(int position) {
        return entries.get(position).getGameType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view;

        if (viewType == TYPE_KOTH) {
            view = inflater.inflate(R.layout.item_match_history_koth, parent, false);
            return new KothViewHolder(view);
        } else if (viewType == TYPE_PVP) {
            view = inflater.inflate(R.layout.item_match_history_pvp, parent, false);
            return new PvpViewHolder(view);
        } else {
            view = inflater.inflate(R.layout.item_match_history_solo, parent, false);
            return new SoloViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MatchHistoryEntry entry = entries.get(position);

        if (holder instanceof SoloViewHolder) {
            ((SoloViewHolder) holder).bind(entry, serverTimeOffset);
        } else if (holder instanceof PvpViewHolder) {
            ((PvpViewHolder) holder).bind(entry, serverTimeOffset);
        } else if (holder instanceof KothViewHolder) {
            ((KothViewHolder) holder).bind(entry, serverTimeOffset);
        }
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    public void updateList(List<MatchHistoryEntry> newEntries, long serverTimeOffset) {
        this.entries = newEntries;
        this.serverTimeOffset = serverTimeOffset;
        notifyDataSetChanged();
    }

    private String formatDateTime(long timestamp, long timeOffset) {
        long localTimestamp = timestamp + timeOffset;
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM HH:mm", Locale.getDefault());
        return sdf.format(new Date(localTimestamp));
    }

    private String getGameModeText(int gameMode) {
        switch (gameMode) {
            case 1: return context.getString(R.string.capitals);
            case 2: return context.getString(R.string.flags);
            case 3: return context.getString(R.string.borders);
            case 4: return context.getString(R.string.languages);
            default: return context.getString(R.string.unknown);
        }
    }

    private int getGameModeIcon(int gameMode) {
        switch (gameMode) {
            case 1: return R.drawable.capital;
            case 2: return R.drawable.flag;
            case 3: return R.drawable.outline;
            case 4: return R.drawable.language;
            default: return R.drawable.ic_achievement_placeholder;
        }
    }

    private int getGamePhoto(int gameType) {
        switch (gameType) {
            case TYPE_SOLO: return R.drawable.solo;
            case TYPE_PVP: return R.drawable.pvp;
            case TYPE_KOTH: return R.drawable.king;
            default: return R.drawable.solo;
        }
    }

    private String getGameTypeText(int gameType) {
        switch (gameType) {
            case TYPE_SOLO: return context.getString(R.string.solo);
            case TYPE_PVP: return context.getString(R.string.duel);
            case TYPE_KOTH: return context.getString(R.string.king_of_the_hill);
            default: return context.getString(R.string.solo);
        }
    }

    class SoloViewHolder extends RecyclerView.ViewHolder {
        View viewStatusBar;
        ImageView ivGamePhoto;
        TextView tvGameType, tvDateTime, tvStatus, tvGameMode, tvCorrectAnswers, tvScore;

        SoloViewHolder(View itemView) {
            super(itemView);
            viewStatusBar = itemView.findViewById(R.id.viewStatusBar);
            ivGamePhoto = itemView.findViewById(R.id.ivGamePhoto);
            tvGameType = itemView.findViewById(R.id.tvGameType);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvGameMode = itemView.findViewById(R.id.tvGameMode);
            tvCorrectAnswers = itemView.findViewById(R.id.tvCorrectAnswers);
            tvScore = itemView.findViewById(R.id.tvScore);
        }

        void bind(MatchHistoryEntry entry, long timeOffset) {
            ivGamePhoto.setImageResource(getGamePhoto(entry.getGameType()));
            tvGameType.setText(getGameTypeText(entry.getGameType()));
            tvDateTime.setText(formatDateTime(entry.getTimestamp(), timeOffset));
            tvGameMode.setText(getGameModeText(entry.getGameMode()));
            tvCorrectAnswers.setText(entry.getCorrectAnswers() + "/" + entry.getTotalQuestions());
            tvScore.setText("+" + entry.getExperienceGained() + " XP");

            if (entry.isWin()) {
                viewStatusBar.setBackgroundColor(context.getColor(R.color.colorSuccess));
                tvStatus.setText(R.string.victory);
                tvStatus.setBackgroundColor(context.getColor(R.color.colorSuccess));
                tvCorrectAnswers.setTextColor(context.getColor(R.color.colorSuccess));
            } else {
                viewStatusBar.setBackgroundColor(context.getColor(R.color.colorError));
                tvStatus.setText(R.string.defeat);
                tvStatus.setBackgroundColor(context.getColor(R.color.colorError));
                tvCorrectAnswers.setTextColor(context.getColor(R.color.colorError));
            }
        }
    }

    class PvpViewHolder extends RecyclerView.ViewHolder {
        View viewStatusBar;
        ImageView ivGamePhoto;
        TextView tvGameType, tvDateTime, tvStatus, tvGameMode, tvCorrectAnswers, tvScore;

        PvpViewHolder(View itemView) {
            super(itemView);
            viewStatusBar = itemView.findViewById(R.id.viewStatusBar);
            ivGamePhoto = itemView.findViewById(R.id.ivGamePhoto);
            tvGameType = itemView.findViewById(R.id.tvGameType);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvGameMode = itemView.findViewById(R.id.tvGameMode);
            tvCorrectAnswers = itemView.findViewById(R.id.tvCorrectAnswers);
            tvScore = itemView.findViewById(R.id.tvScore);
        }

        void bind(MatchHistoryEntry entry, long timeOffset) {
            ivGamePhoto.setImageResource(getGamePhoto(entry.getGameType()));
            tvGameType.setText(getGameTypeText(entry.getGameType()));
            tvDateTime.setText(formatDateTime(entry.getTimestamp(), timeOffset));
            tvGameMode.setText(getGameModeText(entry.getGameMode()));
            tvCorrectAnswers.setText(entry.getCorrectAnswers() + "/" + entry.getTotalQuestions());
            tvScore.setText("+" + entry.getExperienceGained() + " XP");

            if (entry.isWin()) {
                viewStatusBar.setBackgroundColor(context.getColor(R.color.colorSuccess));
                tvStatus.setText(R.string.victory);
                tvStatus.setBackgroundColor(context.getColor(R.color.colorSuccess));
                tvCorrectAnswers.setTextColor(context.getColor(R.color.colorSuccess));
            } else {
                viewStatusBar.setBackgroundColor(context.getColor(R.color.colorError));
                tvStatus.setText(R.string.defeat);
                tvStatus.setBackgroundColor(context.getColor(R.color.colorError));
                tvCorrectAnswers.setTextColor(context.getColor(R.color.colorError));
            }
        }
    }

    class KothViewHolder extends RecyclerView.ViewHolder {
        View viewStatusBar;
        ImageView ivGamePhoto;
        TextView tvGameType, tvDateTime, tvStatus, tvGameMode, tvCorrectAnswers, tvScore, tvPlace;

        KothViewHolder(View itemView) {
            super(itemView);
            viewStatusBar = itemView.findViewById(R.id.viewStatusBar);
            ivGamePhoto = itemView.findViewById(R.id.ivGamePhoto);
            tvGameType = itemView.findViewById(R.id.tvGameType);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvGameMode = itemView.findViewById(R.id.tvGameMode);
            tvCorrectAnswers = itemView.findViewById(R.id.tvCorrectAnswers);
            tvScore = itemView.findViewById(R.id.tvScore);
        }

        void bind(MatchHistoryEntry entry, long timeOffset) {
            ivGamePhoto.setImageResource(getGamePhoto(entry.getGameType()));
            tvGameType.setText(getGameTypeText(entry.getGameType()));
            tvDateTime.setText(formatDateTime(entry.getTimestamp(), timeOffset));
            tvGameMode.setText(getGameModeText(entry.getGameMode()));
            tvCorrectAnswers.setText(entry.getCorrectAnswers() + "/" + entry.getTotalQuestions());
            tvScore.setText("+" + entry.getExperienceGained() + " XP");


            if (entry.isWin()) {
                viewStatusBar.setBackgroundColor(context.getColor(R.color.colorSuccess));
                tvStatus.setText(R.string.victory);
                tvStatus.setBackgroundColor(context.getColor(R.color.colorSuccess));
                tvCorrectAnswers.setTextColor(context.getColor(R.color.colorSuccess));
            } else if (entry.getPlace() <= 3) {
                viewStatusBar.setBackgroundColor(context.getColor(R.color.colorWarning));
                tvStatus.setText(entry.getPlace() + " " + context.getString(R.string.place));
                tvStatus.setBackgroundColor(context.getColor(R.color.colorWarning));
                tvCorrectAnswers.setTextColor(context.getColor(R.color.primary));
            } else {
                viewStatusBar.setBackgroundColor(context.getColor(R.color.text_secondary));
                tvStatus.setText(entry.getPlace() + " " + context.getString(R.string.place));
                tvStatus.setBackgroundColor(context.getColor(R.color.text_secondary));
                tvCorrectAnswers.setTextColor(context.getColor(R.color.primary));
            }
        }
    }
}