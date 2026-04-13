package com.example.geoquiz_frontend.presentation.utils;

import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.geoquiz_frontend.R;
import com.example.geoquiz_frontend.domain.entities.Achievement;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class AchievementDialogHelper {

    private Context context;
    private Dialog dialog;
    private List<Achievement> achievements;
    private int currentIndex = 0;

    private LinearLayout dotsContainer;
    private Button btnNext;
    private ImageButton btnClose;
    private MaterialCardView cardAchievement;
    private ImageView ivIcon;
    private TextView tvHeader, tvTitle;
    private View iconBorder, glowView;

    public AchievementDialogHelper(Context context) {
        this.context = context;
    }

    public void showAchievements(List<Achievement> achievements) {
        this.achievements = achievements;
        this.currentIndex = 0;

        createDialog();
        updateDisplay();
        if (achievements.size()>1) updateDots();
        updateButton();

        dialog.show();
        animateDialogEntry();
    }

    private void createDialog() {
        dialog = new Dialog(context, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar_MinWidth);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_achievement_unlocked, null);
        dialog.setContentView(view);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        dotsContainer = view.findViewById(R.id.dotsContainer);
        btnNext = view.findViewById(R.id.btnNext);
        btnClose = view.findViewById(R.id.btnClose);
        cardAchievement = view.findViewById(R.id.cardAchievement);
        ivIcon = view.findViewById(R.id.ivIcon);
        tvHeader = view.findViewById(R.id.tvHeader);
        tvTitle = view.findViewById(R.id.tvTitle);
        iconBorder = view.findViewById(R.id.iconBorder);
        glowView = view.findViewById(R.id.glowView);

        tvHeader.setText(R.string.achievement_unlocked);

        btnNext.setOnClickListener(v -> {
            if (currentIndex < achievements.size() - 1) {
                currentIndex++;
                animateCardTransition();
                updateDisplay();
                updateDots();
                updateButton();
            } else {
                animateDialogExit();
                dialog.dismiss();
            }
        });

        btnClose.setOnClickListener(v -> {
            animateDialogExit();
            dialog.dismiss();
        });

        dialog.setCanceledOnTouchOutside(true);
        dialog.setOnCancelListener(dialogInterface -> animateDialogExit());
    }

    private void updateDisplay() {
        PreferencesHelper prefs = new PreferencesHelper(context);
        boolean isRu = "ru".equals(prefs.getLanguage());
        Achievement achievement = achievements.get(currentIndex);

        applyBorderStyle(achievement.rarity);

        tvTitle.setText(isRu ? achievement.title.getRu() : achievement.title.getEn());

        int iconRes = context.getResources().getIdentifier(
                achievement.icon, "drawable", context.getPackageName()
        );
        ivIcon.setImageResource(iconRes != 0 ? iconRes : R.drawable.ic_achievement_placeholder);

        if (achievement.rarity == 4) {
            startGlow();
        } else if (glowView != null) {
            glowView.setVisibility(View.GONE);
        }
    }

    private void applyBorderStyle(int rarity) {
        int strokeColor;
        int strokeWidth;
        int borderRes;

        switch (rarity) {
            case 1:
                strokeColor = R.color.common_rarity;
                strokeWidth = 2;
                borderRes = R.drawable.border_common;
                break;
            case 2:
                strokeColor = R.color.rare_rarity;
                strokeWidth = 3;
                borderRes = R.drawable.border_rare;
                break;
            case 3:
                strokeColor = R.color.epic_rarity;
                strokeWidth = 3;
                borderRes = R.drawable.border_epic;
                break;
            case 4:
                strokeColor = R.color.legendary_rarity;
                strokeWidth = 4;
                borderRes = R.drawable.border_legendary;
                break;
            default:
                strokeColor = R.color.common_rarity;
                strokeWidth = 2;
                borderRes = R.drawable.border_common;
        }

        cardAchievement.setStrokeColor(ContextCompat.getColor(context, strokeColor));
        cardAchievement.setStrokeWidth(strokeWidth);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            iconBorder.setBackground(ContextCompat.getDrawable(context, borderRes));
        } else {
            iconBorder.setBackgroundDrawable(ContextCompat.getDrawable(context, borderRes));
        }
    }

    private void startGlow() {
        if (glowView != null) {
            glowView.setVisibility(View.VISIBLE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                glowView.setBackground(ContextCompat.getDrawable(context, R.drawable.glow_legendary));
            }
            ObjectAnimator fade = ObjectAnimator.ofFloat(glowView, "alpha", 0f, 0.5f);
            fade.setDuration(1000);
            fade.setRepeatCount(ObjectAnimator.INFINITE);
            fade.setRepeatMode(ObjectAnimator.REVERSE);
            fade.start();
        }
    }

    private void updateButton() {
        boolean isLast = (currentIndex == achievements.size() - 1);

        if (isLast) {
            btnNext.setText(R.string.claim_reward);

            btnNext.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.legendary_rarity));
            btnNext.setTextColor(Color.BLACK);
        } else {
            btnNext.setText(R.string.next);
            btnNext.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.primary));
            btnNext.setTextColor(Color.WHITE);
        }
    }

    private void updateDots() {
        dotsContainer.removeAllViews();
        for (int i = 0; i < achievements.size(); i++) {
            View dot = new View(context);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(8, 8);
            params.setMargins(6, 0, 6, 0);
            dot.setLayoutParams(params);

            if (i == currentIndex) {
                dot.setBackgroundResource(R.drawable.dot_active);
            } else if (i < currentIndex) {
                dot.setBackgroundResource(R.drawable.dot_completed);
            } else {
                dot.setBackgroundResource(R.drawable.dot_inactive);
            }

            dotsContainer.addView(dot);
        }
    }

    private void animateCardTransition() {
        Animation anim = AnimationUtils.loadAnimation(context, R.anim.slide_in_right);
        cardAchievement.startAnimation(anim);
    }

    private void animateDialogEntry() {
        View dialogView = dialog.getWindow().getDecorView().findViewById(android.R.id.content);
        Animation animation = AnimationUtils.loadAnimation(context, R.anim.scale_in);
        dialogView.startAnimation(animation);
    }

    private void animateDialogExit() {
        View dialogView = dialog.getWindow().getDecorView().findViewById(android.R.id.content);
        Animation animation = AnimationUtils.loadAnimation(context, R.anim.scale_out);
        dialogView.startAnimation(animation);
    }
}