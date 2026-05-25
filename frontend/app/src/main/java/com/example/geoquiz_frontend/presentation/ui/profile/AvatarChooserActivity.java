package com.example.geoquiz_frontend.presentation.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.gridlayout.widget.GridLayout;

import com.example.geoquiz_frontend.R;
import com.example.geoquiz_frontend.presentation.ui.base.BaseActivity;
import com.example.geoquiz_frontend.presentation.utils.SecurePreferencesHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class AvatarChooserActivity extends BaseActivity {

    private ImageView ivBack;
    private GridLayout gridAvatars;
    private MaterialButton btnSave;

    private String selectedAvatarKey;
    private int selectedAvatarResId;

    private AvatarItem[] avatars = {
            new AvatarItem("avatar_1", R.drawable.avatar_1, R.id.avatar1),
            new AvatarItem("avatar_2", R.drawable.avatar_2, R.id.avatar2),
            new AvatarItem("avatar_3", R.drawable.avatar_3, R.id.avatar3),
            new AvatarItem("avatar_4", R.drawable.avatar_4, R.id.avatar4),
            new AvatarItem("avatar_5", R.drawable.avatar_5, R.id.avatar5),
            new AvatarItem("avatar_6", R.drawable.avatar_6, R.id.avatar6),
            new AvatarItem("avatar_7", R.drawable.avatar_7, R.id.avatar7),
            new AvatarItem("avatar_8", R.drawable.avatar_8, R.id.avatar8),
            new AvatarItem("avatar_9", R.drawable.avatar_9, R.id.avatar9),
            new AvatarItem("avatar_10", R.drawable.avatar_10, R.id.avatar10),
            new AvatarItem("avatar_11", R.drawable.avatar_11, R.id.avatar11),
            new AvatarItem("avatar_12", R.drawable.avatar_12, R.id.avatar12)
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_avatar_chooser);

        preferencesHelper = new SecurePreferencesHelper(this);

        initViews();
        gridAvatars.post(() -> setupGridSize());
        setupClickListeners();
        loadCurrentAvatar();
        setupAvatarGrid();
        hideSystemBars();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        gridAvatars = findViewById(R.id.gridAvatars);
        btnSave = findViewById(R.id.btnSave);
    }
    private void setupGridSize() {
        int spanCount = 3;
        int spacing = 10;

        int gridWidth = gridAvatars.getWidth();

        int spacingPx = (int) (spacing * getResources().getDisplayMetrics().density);
        int totalSpacing = spacingPx * (spanCount*2);
        int itemWidth = (gridWidth - totalSpacing) / (spanCount);

        for (int i = 0; i < gridAvatars.getChildCount(); i++) {
            View item = gridAvatars.getChildAt(i);
            FrameLayout frameLayout = (FrameLayout) item;

            GridLayout.LayoutParams params = (GridLayout.LayoutParams) frameLayout.getLayoutParams();
            params.width = itemWidth;
            params.height = itemWidth;
            params.setMargins(spacingPx, spacingPx, spacingPx, spacingPx);
            frameLayout.setLayoutParams(params);
        }
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> {
            if (selectedAvatarKey != null) {
                saveAvatar();
            }
        });
    }

    private void loadCurrentAvatar() {
        selectedAvatarKey = preferencesHelper.getAvatarKey();
        if (selectedAvatarKey == null) {
            selectedAvatarKey = "avatar_0";
            selectedAvatarResId = R.drawable.ic_profile_placeholder;
            return;
        }

        for (AvatarItem item : avatars) {
            if (item.key.equals(selectedAvatarKey)) {
                selectedAvatarResId = item.drawableResId;
                break;
            }
        }
    }

    private void setupAvatarGrid() {
        for (AvatarItem item : avatars) {
            View avatarView = gridAvatars.findViewById(item.layoutId);
            if (avatarView != null) {
                MaterialCardView cardAvatar = avatarView.findViewById(R.id.cardAvatar);
                ImageView ivAvatar = avatarView.findViewById(R.id.ivAvatar);
                View viewSelected = avatarView.findViewById(R.id.viewSelected);
                ImageView ivCheck = avatarView.findViewById(R.id.ivCheck);

                ivAvatar.setImageResource(item.drawableResId);

                boolean isSelected = item.key.equals(selectedAvatarKey);
                updateSelectionState(viewSelected, ivCheck, isSelected);

                cardAvatar.setOnClickListener(v -> {
                    clearAllSelections();

                    selectedAvatarKey = item.key;
                    selectedAvatarResId = item.drawableResId;
                    updateSelectionState(viewSelected, ivCheck, true);
                });
            }
        }
    }

    private void clearAllSelections() {
        for (AvatarItem item : avatars) {
            View avatarView = gridAvatars.findViewById(item.layoutId);
            if (avatarView != null) {
                View viewSelected = avatarView.findViewById(R.id.viewSelected);
                ImageView ivCheck = avatarView.findViewById(R.id.ivCheck);
                updateSelectionState(viewSelected, ivCheck, false);
            }
        }
    }

    private void updateSelectionState(View viewSelected, ImageView ivCheck, boolean isSelected) {
        if (isSelected) {
            viewSelected.setVisibility(View.VISIBLE);
            ivCheck.setVisibility(View.VISIBLE);
        } else {
            viewSelected.setVisibility(View.GONE);
            ivCheck.setVisibility(View.GONE);
        }
    }

    private void saveAvatar() {
        preferencesHelper.setAvatarKey(selectedAvatarKey);

        Intent resultIntent = new Intent();
        resultIntent.putExtra("avatar_key", selectedAvatarKey);
        resultIntent.putExtra("avatar_res_id", selectedAvatarResId);
        setResult(RESULT_OK, resultIntent);

        finish();
    }
    private void hideSystemBars() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }
    private static class AvatarItem {
        String key;
        int drawableResId;
        int layoutId;

        AvatarItem(String key, int drawableResId, int layoutId) {
            this.key = key;
            this.drawableResId = drawableResId;
            this.layoutId = layoutId;
        }
    }
}