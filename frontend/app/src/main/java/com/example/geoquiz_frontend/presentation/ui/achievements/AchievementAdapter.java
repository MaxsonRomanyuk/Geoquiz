package com.example.geoquiz_frontend.presentation.ui.achievements;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.geoquiz_frontend.R;
import com.example.geoquiz_frontend.domain.entities.Achievement;
import com.example.geoquiz_frontend.presentation.utils.PreferencesHelper;
import com.example.geoquiz_frontend.presentation.utils.SecurePreferencesHelper;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class AchievementAdapter extends RecyclerView.Adapter<AchievementAdapter.ViewHolder> {

    private Context context;
    private List<Achievement> achievements;
    private SecurePreferencesHelper preferencesHelper;
    private String currentLanguage;
    private int lastPosition = -1;

    public AchievementAdapter(Context context, List<Achievement> achievements) {
        this.context = context;
        this.achievements = achievements;
        this.preferencesHelper = new SecurePreferencesHelper(context);
        this.currentLanguage = preferencesHelper.getLanguage();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_achievement, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Achievement achievement = achievements.get(position);
        holder.bind(achievement, currentLanguage);
        setAnimation(holder.itemView, position);
    }

    private void setAnimation(View view, int position) {
        if (position > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(context, android.R.anim.slide_in_left);
            animation.setDuration(300);
            animation.setStartOffset(position * 50);
            view.startAnimation(animation);
            lastPosition = position;
        }
    }

    @Override
    public int getItemCount() {
        return achievements.size();
    }

    public void updateList(List<Achievement> newList) {
        this.achievements = newList;
        lastPosition = -1;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardAchievement;
        LinearLayout layoutAchievement;
        ImageView ivIcon;
        TextView tvLockIcon;
        TextView tvTitle;
        TextView tvDescription;
        TextView tvRarityIcon;
        LinearLayout layoutProgress;
        TextView tvProgress;
        ProgressBar progressBar;
        View glowView;
        View iconBorder;

        ViewHolder(View itemView) {
            super(itemView);
            cardAchievement = itemView.findViewById(R.id.cardAchievement);
            layoutAchievement = itemView.findViewById(R.id.layoutAchievement);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvLockIcon = itemView.findViewById(R.id.tvLockIcon);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvRarityIcon = itemView.findViewById(R.id.tvRarityIcon);
            layoutProgress = itemView.findViewById(R.id.layoutProgress);
            tvProgress = itemView.findViewById(R.id.tvProgress);
            progressBar = itemView.findViewById(R.id.progressBar);
            glowView = itemView.findViewById(R.id.glowView);
            iconBorder = itemView.findViewById(R.id.iconBorder);
        }

        void bind(Achievement achievement, String language ) {
            Context ctx = itemView.getContext();
            boolean isUnlocked = achievement.isUnlocked;

            applyRarityStyle(achievement.rarity, isUnlocked);

            String rarityIcon = getRarityIcon(achievement.rarity);
            tvRarityIcon.setText(rarityIcon);

            if (achievement.rarity == 4) {
                startGlowAnimation();
            } else if (glowView != null) {
                glowView.setVisibility(View.GONE);
            }

            if (isUnlocked) {
                cardAchievement.setAlpha(1.0f);
                tvLockIcon.setVisibility(View.GONE);
                ivIcon.setAlpha(1.0f);
            } else {
                cardAchievement.setAlpha(0.7f);
                tvLockIcon.setVisibility(View.VISIBLE);
                ivIcon.setAlpha(0.2f);
            }

            tvTitle.setText(language.equals("ru") ? achievement.title.getRu() : achievement.title.getEn());
            tvDescription.setText(language.equals("ru") ? achievement.description.getRu() : achievement.description.getEn());

            int iconRes = ctx.getResources().getIdentifier(achievement.icon, "drawable", ctx.getPackageName());
            if (iconRes != 0) {
                ivIcon.setImageResource(iconRes);
            } else {
                ivIcon.setImageResource(R.drawable.ic_achievement_placeholder);
            }

            layoutProgress.setVisibility(View.GONE);
        }

        private void applyRarityStyle(int rarity, boolean isUnlocked) {
            Context ctx = itemView.getContext();
            int strokeColor;
            int strokeWidth;
            int backgroundColor;
            int iconBorderRes;

            float alpha = isUnlocked ? 1.0f : 0.7f;

            switch (rarity) {
                case 1:
                    strokeColor = ContextCompat.getColor(ctx, R.color.common_rarity);
                    strokeWidth = 2;
                    backgroundColor = R.drawable.card_bg_common;
                    iconBorderRes = R.drawable.border_common;
                    break;
                case 2:
                    strokeColor = ContextCompat.getColor(ctx, R.color.rare_rarity);
                    strokeWidth = 3;
                    backgroundColor = R.drawable.card_bg_rare;
                    iconBorderRes = R.drawable.border_rare;
                    break;
                case 3:
                    strokeColor = ContextCompat.getColor(ctx, R.color.epic_rarity);
                    strokeWidth = 3;
                    backgroundColor = R.drawable.card_bg_epic;
                    iconBorderRes = R.drawable.border_epic;
                    break;
                case 4:
                    strokeColor = ContextCompat.getColor(ctx, R.color.legendary_rarity);
                    strokeWidth = 4;
                    backgroundColor = R.drawable.card_bg_legendary;
                    iconBorderRes = R.drawable.border_legendary;
                    break;
                default:
                    strokeColor = ContextCompat.getColor(ctx, R.color.common_rarity);
                    strokeWidth = 2;
                    backgroundColor = R.drawable.card_bg_common;
                    iconBorderRes = R.drawable.border_common;
                    break;
            }

            cardAchievement.setStrokeColor(strokeColor);
            cardAchievement.setStrokeWidth(strokeWidth);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                layoutAchievement.setBackground(ContextCompat.getDrawable(ctx, backgroundColor));
                iconBorder.setBackground(ContextCompat.getDrawable(ctx, iconBorderRes));
            } else {
                layoutAchievement.setBackgroundDrawable(ContextCompat.getDrawable(ctx, backgroundColor));
                iconBorder.setBackgroundDrawable(ContextCompat.getDrawable(ctx, iconBorderRes));
            }

            cardAchievement.setCardElevation(isUnlocked ? 4 + rarity : 2);
        }

        private void startGlowAnimation() {
            if (glowView != null) {
                glowView.setVisibility(View.VISIBLE);
                ObjectAnimator fadeIn = ObjectAnimator.ofFloat(glowView, "alpha", 0f, 0.8f);
                fadeIn.setDuration(1000);
                fadeIn.setRepeatCount(ObjectAnimator.INFINITE);
                fadeIn.setRepeatMode(ObjectAnimator.REVERSE);
                fadeIn.start();
            }
        }

        private String getRarityIcon(int rarity) {
            switch (rarity) {
                case 1: return "⭐";
                case 2: return "⭐⭐";
                case 3: return "⭐⭐⭐";
                case 4: return "👑";
                default: return "⭐";
            }
        }
    }
}