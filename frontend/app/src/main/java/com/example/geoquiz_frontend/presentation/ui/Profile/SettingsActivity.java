package com.example.geoquiz_frontend.presentation.ui.Profile;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;

import com.example.geoquiz_frontend.domain.entities.UserStats;
import com.example.geoquiz_frontend.presentation.utils.AuthManager;
import com.example.geoquiz_frontend.data.remote.dtos.solo.BootstrapResponse;
import com.example.geoquiz_frontend.data.repositories.GameRepository;
import com.example.geoquiz_frontend.domain.engine.GameManager;
import com.example.geoquiz_frontend.domain.entities.User;
import com.example.geoquiz_frontend.presentation.utils.LocaleHelper;
import com.example.geoquiz_frontend.presentation.utils.PreferencesHelper;
import com.example.geoquiz_frontend.R;
import com.example.geoquiz_frontend.presentation.ui.Auth.LoginActivity;
import com.example.geoquiz_frontend.presentation.ui.Base.BaseActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.Gson;

public class SettingsActivity extends BaseActivity {
    private PreferencesHelper preferencesHelper;
    private AuthManager authManager;
    private User currentUser;
    private GameManager gameManager;
    private TextView tvCurrentLanguage, tvCurrentTheme, tvUpdateStatus, tvSubscriptionStatus;
    private MaterialCardView btnLanguage, btnTheme, btnData, btnAbout, btnPrivacy, btnTerms, btnSubscription, btnTransfer, btnLogout, btnBack;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_settings);


        preferencesHelper = new PreferencesHelper(this);
        authManager = new AuthManager(this);
        currentUser = authManager.getCurrentUser();
        gameManager = GameManager.getInstance(this);
        initViews();
        setupClickListeners();
        setupCurrentSettings();

        if (preferencesHelper.getUserId().equals("uid")) btnTransfer.setVisibility(View.VISIBLE);
    }
    private void initViews() {
        tvCurrentLanguage = findViewById(R.id.tv_current_language);
        tvCurrentTheme = findViewById(R.id.tv_current_theme);
        tvSubscriptionStatus = findViewById(R.id.tv_subscription_status);
        tvUpdateStatus = findViewById(R.id.tv_update_status);

        btnLanguage = findViewById(R.id.btn_language);
        btnTheme = findViewById(R.id.btn_theme);
        btnData = findViewById(R.id.btn_update_data);
        btnAbout = findViewById(R.id.btn_about);
        btnPrivacy = findViewById(R.id.btn_privacy);
        btnTerms = findViewById(R.id.btn_terms);
        btnSubscription = findViewById(R.id.btn_subscription);
        btnTransfer = findViewById(R.id.btn_transfer);
        btnLogout = findViewById(R.id.btn_logout);
        btnBack = findViewById(R.id.btn_back);
    }
    private void setupClickListeners() {
        btnLanguage.setOnClickListener(v -> showLanguageDialog());

        btnTheme.setOnClickListener(v -> showThemeDialog());

        btnData.setOnClickListener(v -> setupDataUpdate());

        btnAbout.setOnClickListener(v -> showAboutDialog());

        btnPrivacy.setOnClickListener(v -> showPrivacyDialog());

        btnTerms.setOnClickListener(v -> showTermsDialog());

        btnSubscription.setOnClickListener(v -> openSubscriptionScreen());

        btnTransfer.setOnClickListener(v-> showTransferConfirmation());

        btnLogout.setOnClickListener(v -> showLogoutConfirmation());

        btnBack.setOnClickListener(v -> backToProfile());


    }
    private void setupCurrentSettings() {
        String currentLanguage = preferencesHelper.getLanguage();
        tvCurrentLanguage.setText("ru".equals(currentLanguage) ? "Русский" : "English");

        String currentTheme = preferencesHelper.getTheme();
        tvCurrentTheme.setText("dark".equals(currentTheme) ? ("ru".equals(currentLanguage) ? "Тёмная" : "Dark") : ("ru".equals(currentLanguage) ? "Светлая" : "Light"));


        boolean isPremium = currentUser.isPremium();
        String subscriptionText = isPremium ?
                ("ru".equals(currentLanguage) ? "Премиум" : "Premium") :
                ("ru".equals(currentLanguage) ? "Не активирована" : "Not Subscribed");
        tvSubscriptionStatus.setText(subscriptionText);
    }
    private void showLanguageDialog() {
        String currentLanguage = preferencesHelper.getLanguage();

        String[] languages = {"Русский", "English"};
        int currentSelection = "en".equals(currentLanguage) ? 1 : 0;

        String title = "ru".equals(currentLanguage) ? "Выберите язык" : "Select Language";
        String positiveButton = "ru".equals(currentLanguage) ? "Применить" : "Apply";
        String negativeButton = "ru".equals(currentLanguage) ? "Отмена" : "Cancel";
        String toastMessage = "ru".equals(currentLanguage) ?
                "Язык изменен. Перезапустите приложение" : "Language changed. Restart the app";

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setSingleChoiceItems(languages, currentSelection, null)
                .setPositiveButton(positiveButton, (dialog, which) -> {
                    int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    String selectedLanguage = selectedPosition == 0 ? "ru" : "en";

                    if (!selectedLanguage.equals(currentLanguage)) {
                        applyLanguage(selectedLanguage);
                        Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();

                        LocaleHelper.setLocale(this, selectedLanguage);
                        //applyLanguage(selectedLanguage);
                    }
                })
                .setNegativeButton(negativeButton, null)
                .show();
    }
    private void applyLanguage(String language) {
        preferencesHelper.setLanguage(language);
        Intent intent = getIntent();
        finish();
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
    private void applyTheme() {
        String theme = preferencesHelper.getTheme();
        if ("dark".equals(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
    private void showThemeDialog() {
        String currentLanguage = preferencesHelper.getLanguage();

        String[] themes = {"ru".equals(currentLanguage) ? "Светлая" : "Light",
                "ru".equals(currentLanguage) ? "Тёмная" : "Dark"};
        int currentSelection = "dark".equals(preferencesHelper.getTheme()) ? 1 : 0;

        String title = "ru".equals(currentLanguage) ? "Выберите тему" : "Select Theme";
        String positiveButton = "ru".equals(currentLanguage) ? "Применить" : "Apply";
        String negativeButton = "ru".equals(currentLanguage) ? "Отмена" : "Cancel";

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setSingleChoiceItems(themes, currentSelection, null)
                .setPositiveButton(positiveButton, (dialog, which) -> {
                    int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    String selectedTheme = selectedPosition == 0 ? "light" : "dark";

                    preferencesHelper.setTheme(selectedTheme);
                    applyTheme();
                    recreate();
                })
                .setNegativeButton(negativeButton, null)
                .show();
    }
    private void setupDataUpdate() {

        String currentLanguage = preferencesHelper.getLanguage();

        String title = "ru".equals(currentLanguage) ? "Обновить данные" : "Update Data";
        String message = "ru".equals(currentLanguage) ?
                "Это загрузит свежие вопросы с сервера. Продолжить?" :
                "This will download fresh questions from the server. Continue?";

        String positiveButton = "ru".equals(currentLanguage) ? "Да" : "Yes";
        String negativeButton = "ru".equals(currentLanguage) ? "Нет" : "No";
        String loadingMessage = "ru".equals(currentLanguage) ? "Загрузка..." : "Loading...";
        String successMessage = "ru".equals(currentLanguage) ? "Данные обновлены" : "Data updated";
        String errorPrefix = "ru".equals(currentLanguage) ? "Ошибка: " : "Error: ";
        String upToDateMessage = "ru".equals(currentLanguage) ? "У вас последняя версия" : "You have the latest version";

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveButton, (d, which) -> {
                    ProgressDialog progress = new ProgressDialog(this);
                    progress.setMessage(loadingMessage);
                    progress.setCancelable(false);
                    progress.show();

                    tvUpdateStatus.setText(loadingMessage);

                    gameManager.loadBootstrapData(new GameRepository.BootstrapCallback() {
                        @Override
                        public void onSuccess(BootstrapResponse data) {
                            progress.dismiss();
                            tvUpdateStatus.setText(successMessage);

                            Toast.makeText(SettingsActivity.this,
                                    successMessage,
                                    Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String error) {
                            progress.dismiss();
                            tvUpdateStatus.setText(upToDateMessage);

                            Toast.makeText(SettingsActivity.this,
                                    errorPrefix + error,
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton(negativeButton, null)
                .show();


    }


    private void showAboutDialog() {
        String currentLanguage = preferencesHelper.getLanguage();

        String title = "ru".equals(currentLanguage) ? "О приложении" : "About App";
        String message = "ru".equals(currentLanguage) ?
                "🌍 GeoQuiz - Географическая викторина\n\n" +
                        "▫️ Версия: 1.0.0\n" +
                        "▫️ Разработчик: Команда GeoQuiz\n" +
                        "▫️ Дата выпуска: Февраль 2026\n" +
                        "▫️ Платформа: Android 8.0+\n\n" +

                        "🎯 О ПРИЛОЖЕНИИ:\n" +
                        "GeoQuiz - это интерактивная географическая викторина, которая сочетает одиночный режим, соревновательный PvP-режим и захватывающий режим «Царь горы». Проверьте свои знания о мире в увлекательной игровой форме!\n\n" +

                        "🚀 ИГРОВЫЕ РЕЖИМЫ:\n" +
                        "🧭 Соло-режим: Путешествуйте по миру в одиночку, изучая страны, столицы и флаги. Доступен офлайн!\n" +
                        "⚔️ PvP-дуэль: Сразитесь с другими игроками в режиме реального времени. Кто быстрее и точнее?\n" +
                        "👑 Царь горы: Battle-royale режим, где множество игроков участвуют в одной сессии. Последний оставшийся побеждает!\n\n" +

                        "🌍 КОНТЕНТ:\n" +
                        "• База данных: 195 стран мира\n" +
                        "• 500+ уникальных вопросов\n" +
                        "• 4 типа вопросов: флаги, столицы, контуры стран, языки\n" +
                        "• Регулярное обновление контента\n\n" +

                        "📊 СТАТИСТИКА И ПРОГРЕСС:\n" +
                        "• Детальная статистика по каждому режиму\n" +
                        "• Отслеживание прогресса по континентам\n" +
                        "• Система достижений и уровней\n" +
                        "• Серии побед и рекорды\n\n" +

                        "🔧 ТЕХНИЧЕСКИЕ ОСОБЕННОСТИ:\n" +
                        "• Полноценный офлайн-режим\n" +
                        "• Автоматическая синхронизация при подключении к сети\n" +
                        "• Адаптивные темы (светлая/тёмная)\n" +
                        "• Мультиязычная поддержка (русский, английский)\n\n" +

                        "🔒 КОНФИДЕНЦИАЛЬНОСТЬ:\n" +
                        "• Минимальный сбор данных для работы аккаунта\n" +
                        "• Статистика игр хранится локально и на сервере\n" +
                        "• Отсутствует трекинг активности\n" +
                        "• Соответствие GDPR\n\n" +

                        "🔄 ПЛАНОВЫЕ ОБНОВЛЕНИЯ:\n" +
                        "• Ежемесячное добавление новых вопросов\n" +
                        "• Сезонные события и турниры\n" +
                        "• Улучшение matchmaking для PvP\n" +
                        "• Оптимизация производительности\n\n" +

                        "📞 ПОДДЕРЖКА И СВЯЗЬ:\n" +
                        "• Email: support@geoquiz.app\n" +
                        "• Telegram: @geoquiz_support\n" +
                        "• Время ответа: до 24 часов в будние дни\n\n" +

                        "💝 БЛАГОДАРНОСТИ:\n" +
                        "Спасибо, что выбрали GeoQuiz! Мы постоянно работаем над улучшением приложения. Если у вас есть идеи или предложения, пишите нам в поддержку.\n\n" +

                        "⭐ ОЦЕНИТЕ ПРИЛОЖЕНИЕ:\n" +
                        "Если вам нравится GeoQuiz, поставьте нам 5 звёзд в Google Play!" :

                "🌍 GeoQuiz - Geography Quiz\n\n" +
                        "▫️ Version: 1.0.0\n" +
                        "▫️ Developer: GeoQuiz Team\n" +
                        "▫️ Release Date: February 2026\n" +
                        "▫️ Platform: Android 8.0+\n\n" +

                        "🎯 ABOUT THE APP:\n" +
                        "GeoQuiz is an interactive geography quiz that combines solo mode, competitive PvP mode, and an exciting King of the Hill mode. Test your knowledge of the world in a fun gaming format!\n\n" +

                        "🚀 GAME MODES:\n" +
                        "🧭 Solo Mode: Travel the world alone, learning countries, capitals, and flags. Available offline!\n" +
                        "⚔️ PvP Duel: Battle other players in real-time. Who's faster and more accurate?\n" +
                        "👑 King of the Hill: Battle-royale mode where multiple players participate in one session. Last one standing wins!\n\n" +

                        "🌍 CONTENT:\n" +
                        "• Database: 195 countries worldwide\n" +
                        "• 500+ unique questions\n" +
                        "• 4 question types: flags, capitals, country outlines, languages\n" +
                        "• Regular content updates\n\n" +

                        "📊 STATISTICS & PROGRESS:\n" +
                        "• Detailed statistics for each mode\n" +
                        "• Progress tracking by continent\n" +
                        "• Achievements and leveling system\n" +
                        "• Win streaks and records\n\n" +

                        "🔧 TECHNICAL FEATURES:\n" +
                        "• Full offline mode\n" +
                        "• Automatic synchronization when online\n" +
                        "• Adaptive themes (light/dark)\n" +
                        "• Multilingual support (English, Russian)\n\n" +

                        "🔒 PRIVACY:\n" +
                        "• Minimal data collection for account functionality\n" +
                        "• Game statistics stored locally and on server\n" +
                        "• No activity tracking\n" +
                        "• GDPR compliant\n\n" +

                        "🔄 PLANNED UPDATES:\n" +
                        "• Monthly addition of new questions\n" +
                        "• Seasonal events and tournaments\n" +
                        "• Improved PvP matchmaking\n" +
                        "• Performance optimization\n\n" +

                        "📞 SUPPORT & CONTACT:\n" +
                        "• Email: support@geoquiz.app\n" +
                        "• Telegram: @geoquiz_support\n" +
                        "• Response time: up to 24 hours on weekdays\n\n" +

                        "💝 ACKNOWLEDGEMENTS:\n" +
                        "Thank you for choosing GeoQuiz! We're constantly working to improve the app. If you have ideas or suggestions, write to us in support.\n\n" +

                        "⭐ RATE THE APP:\n" +
                        "If you like GeoQuiz, give us 5 stars on Google Play!";

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .setIcon(R.drawable.ic_info)
                .show();
    }
    private void showPrivacyDialog() {
        String currentLanguage = preferencesHelper.getLanguage();

        String title = "ru".equals(currentLanguage) ? "Политика конфиденциальности" : "Privacy Policy";
        String message = "ru".equals(currentLanguage) ?
                "🔒 ПОЛИТИКА КОНФИДЕНЦИАЛЬНОСТИ GEOQUIZ\n\n" +

                        "📅 Дата вступления в силу: 15 февраля 2026 г.\n" +
                        "📅 Последнее обновление: 15 февраля 2026 г.\n" +
                        "🌐 Версия: 2.0\n\n" +

                        "1. ВВЕДЕНИЕ\n" +
                        "1.1. Настоящая Политика конфиденциальности (далее — «Политика») описывает, как компания GeoQuiz собирает, использует и раскрывает вашу информацию при использовании мобильного приложения GeoQuiz (далее — «Приложение»).\n\n" +

                        "2. ОПРЕДЕЛЕНИЯ\n" +
                        "2.1. «Персональные данные» — любая информация, относящаяся к идентифицированному или идентифицируемому физическому лицу.\n" +
                        "2.2. «Обработка» — любое действие с персональными данными.\n" +
                        "2.3. «Пользователь» — лицо, использующее Приложение.\n\n" +

                        "3. СОБИРАЕМЫЕ ДАННЫЕ\n" +
                        "3.1. АВТОМАТИЧЕСКИ СОБИРАЕМЫЕ ДАННЫЕ:\n" +
                        "   • Идентификатор устройства (Device ID)\n" +
                        "   • Версия операционной системы\n" +
                        "   • Модель устройства\n" +
                        "   • Данные об использовании Приложения\n" +
                        "   • IP-адрес (в анонимизированной форме)\n" +
                        "   • Данные о сбоях и производительности\n\n" +

                        "3.2. ДАННЫЕ АККАУНТА (при создании):\n" +
                        "   • Имя пользователя (никнейм)\n" +
                        "   • Адрес электронной почты\n" +
                        "   • Хешированный пароль\n" +
                        "   • Игровая статистика\n" +
                        "   • История матчей и достижений\n" +
                        "   • Языковые предпочтения\n\n" +

                        "3.3. ПЛАТЕЖНЫЕ ДАННЫЕ:\n" +
                        "   • Мы НЕ собираем и НЕ храним платежную информацию\n" +
                        "   • Все платежи обрабатываются через Google Play\n" +
                        "   • Данные о покупках (история транзакций)\n\n" +

                        "3.4. PvP И СОЦИАЛЬНЫЕ ДАННЫЕ:\n" +
                        "   • История PvP-матчей\n" +
                        "   • Статистика побед и поражений\n" +
                        "   • Рейтинг в режиме «Царь горы»\n" +
                        "   • Данные о матчмейкинге\n\n" +

                        "4. ПРАВОВЫЕ ОСНОВАНИЯ ОБРАБОТКИ\n" +
                        "4.1. Обработка осуществляется на следующих основаниях:\n" +
                        "   • Исполнение договора (пользовательское соглашение)\n" +
                        "   • Законный интерес (улучшение сервиса)\n" +
                        "   • Согласие пользователя (маркетинговые коммуникации)\n" +
                        "   • Исполнение юридических обязательств\n\n" +

                        "5. ЦЕЛИ СБОРА ДАННЫХ\n" +
                        "5.1. Мы используем данные для:\n" +
                        "   • Обеспечения функциональности Приложения\n" +
                        "   • Сохранения игрового прогресса\n" +
                        "   • Организации PvP-матчей\n" +
                        "   • Расчета рейтингов и статистики\n" +
                        "   • Технической поддержки\n" +
                        "   • Улучшения пользовательского опыта\n" +
                        "   • Предотвращения мошенничества\n" +
                        "   • Аналитики и оптимизации\n\n" +

                        "6. ПЕРЕДАЧА ДАННЫХ ТРЕТЬИМ ЛИЦАМ\n" +
                        "6.1. МЫ НЕ ПРОДАЕМ ваши персональные данные.\n\n" +

                        "6.2. ТРЕТЬИ ЛИЦА (Обработчики данных):\n" +
                        "   • Google (Google Play Services, Firebase)\n" +
                        "       - Аутентификация\n" +
                        "       - Аналитика\n" +
                        "       - Crash Reporting\n" +
                        "   • MongoDB (хостинг игрового контента)\n" +
                        "   • MySQL (хостинг пользовательских данных)\n" +
                        "   • Серверная инфраструктура (ASP.NET Core)\n\n" +

                        "6.3. ЮРИДИЧЕСКИЕ ТРЕБОВАНИЯ:\n" +
                        "   • Мы можем раскрыть данные по требованию суда\n" +
                        "   • Для защиты наших прав и безопасности\n" +
                        "   • Для предотвращения мошенничества\n\n" +

                        "7. МЕЖДУНАРОДНАЯ ПЕРЕДАЧА ДАННЫХ\n" +
                        "7.1. Ваши данные могут передаваться и обрабатываться:\n" +
                        "   • В стране вашего проживания\n" +
                        "   • В странах расположения серверов (EU/US)\n" +
                        "   • С использованием стандартных договорных положений ЕС\n" +
                        "   • В соответствии с GDPR и local laws\n\n" +

                        "8. ХРАНЕНИЕ И БЕЗОПАСНОСТЬ\n" +
                        "8.1. СРОК ХРАНЕНИЯ:\n" +
                        "   • Данные аккаунта: до удаления аккаунта\n" +
                        "   • Игровая статистика: 3 года после последней активности\n" +
                        "   • Логи: 6 месяцев\n" +
                        "   • Аналитические данные: 2 года\n\n" +

                        "8.2. МЕРЫ БЕЗОПАСНОСТИ:\n" +
                        "   • Шифрование данных при передаче (TLS 1.3)\n" +
                        "   • Хеширование паролей (bcrypt)\n" +
                        "   • Регулярные обновления безопасности\n" +
                        "   • Ограниченный доступ к данным\n" +
                        "   • Двухфакторная аутентификация (опционально)\n\n" +

                        "9. ПРАВА ПОЛЬЗОВАТЕЛЕЙ\n" +
                        "9.1. В соответствии с GDPR и local laws, вы имеете право:\n" +
                        "   • На доступ к своим данным\n" +
                        "   • На исправление неточных данных\n" +
                        "   • На удаление данных («право быть забытым»)\n" +
                        "   • На ограничение обработки\n" +
                        "   • На возражение против обработки\n" +
                        "   • На переносимость данных\n" +
                        "   • Не подвергаться автоматизированному решению\n" +
                        "   • Отозвать согласие в любое время\n\n" +

                        "10. УДАЛЕНИЕ АККАУНТА\n" +
                        "10.1. Вы можете удалить аккаунт:\n" +
                        "   • В настройках Приложения (Настройки → Аккаунт → Удалить)\n" +
                        "   • Отправив запрос на privacy@geoquiz.app\n" +
                        "   • Через Google Play Games\n\n" +

                        "10.2. При удалении удаляются:\n" +
                        "   • Все персональные данные\n" +
                        "   • Игровая статистика\n" +
                        "   • История матчей\n" +
                        "   • Достижения\n" +
                        "   • Подписки\n\n" +

                        "11. COOKIES И АНАЛИТИКА\n" +
                        "11.1. Используем:\n" +
                        "   • Firebase Analytics\n" +
                        "   • Crashlytics\n" +
                        "   • Собственную аналитику матчей\n" +
                        "11.2. НЕ используем для ретаргетинга\n" +
                        "11.3. Вы можете отключить аналитику в настройках\n\n" +

                        "12. ИЗМЕНЕНИЯ В ПОЛИТИКЕ\n" +
                        "12.1. Мы можем обновлять Политику.\n" +
                        "12.2. Уведомляем за 30 дней до значительных изменений.\n" +
                        "12.3. Продолжение использования означает согласие с новой версией.\n\n" +

                        "13. КОНТАКТНАЯ ИНФОРМАЦИЯ\n" +
                        "13.1. По вопросам конфиденциальности:\n" +
                        "   • Email: privacy@geoquiz.app\n" +
                        "   • Адрес: Беларусь, г.Гродно\n" +
                        "   • Время ответа: до 14 рабочих дней\n\n" +

                        "14.2. Надзорный орган:\n" +
                        "   • Вы имеете право подать жалобу в местный орган по защите данных\n" +
                        "   • Контакты предоставляются по запросу\n\n" +

                        "15. СПЕЦИАЛЬНЫЕ ПОЛОЖЕНИЯ\n\n" +

                        "15.1. ДЛЯ РЕЗИДЕНТОВ ЕС (GDPR):\n" +
                        "   • Контролер данных: GeoQuiz [регистрационные данные]\n" +
                        "   • Представитель в ЕС: [контакты]\n" +
                        "   • Правовое основание: Art. 6 GDPR\n\n" +

                        "16. ТЕХНИЧЕСКИЕ ДЕТАЛИ\n" +
                        "16.1. Обработка осуществляется:\n" +
                        "   • Локально на устройстве (Room database)\n" +
                        "   • На серверах (MySQL, MongoDB)\n" +
                        "   • С использованием шифрования AES-256\n" +
                        "   • С регулярным резервным копированием\n\n" +

                        "17. PvP И СОРЕВНОВАТЕЛЬНЫЕ РЕЖИМЫ\n" +
                        "17.1. В PvP-режимах обрабатываются:\n" +
                        "   • Временные данные матчей\n" +
                        "   • Результаты и статистика\n" +
                        "   • История взаимодействий\n" +
                        "   • IP-адреса для матчмейкинга\n\n" +

                        "17.2. Эти данные необходимы для:\n" +
                        "   • Обеспечения честной игры\n" +
                        "   • Предотвращения читерства\n" +
                        "   • Расчета рейтингов\n\n" +

                        "18. МАРКЕТИНГОВЫЕ КОММУНИКАЦИИ\n" +
                        "18.1. Только с вашего согласия\n" +
                        "18.2. Вы можете отписаться в любое время\n" +
                        "18.3. Мы не передаем данные для маркетинга третьим лицам\n\n" +

                        "⚠️ ВАЖНО: Используя Приложение, вы подтверждаете, что прочитали и поняли настоящую Политику конфиденциальности.\n\n" +

                        "📱 GeoQuiz — проверь свои знания географии!\n" +
                        "Последнее обновление: 15 февраля 2026 г." :

                "🔒 GEOQUIZ PRIVACY POLICY\n\n" +

                        "📅 Effective Date: February 15, 2026\n" +
                        "📅 Last Updated: February 15, 2026\n" +
                        "🌐 Version: 2.0\n\n" +

                        "1. INTRODUCTION\n" +
                        "1.1. This Privacy Policy describes how GeoQuiz («we», «us», or «our») collects, uses, and discloses your information when you use the GeoQuiz mobile application (the «App»).\n\n" +

                        "2. DEFINITIONS\n" +
                        "2.1. «Personal Data» means any information relating to an identified or identifiable individual.\n" +
                        "2.2. «Processing» means any operation performed on Personal Data.\n" +
                        "2.3. «User» means any person using the App.\n\n" +

                        "3. DATA WE COLLECT\n" +
                        "3.1. AUTOMATICALLY COLLECTED DATA:\n" +
                        "   • Device ID\n" +
                        "   • Operating system version\n" +
                        "   • Device model\n" +
                        "   • Usage data\n" +
                        "   • Anonymized IP address\n" +
                        "   • Crash and performance data\n\n" +

                        "3.2. ACCOUNT DATA (when created):\n" +
                        "   • Username\n" +
                        "   • Email address\n" +
                        "   • Hashed password\n" +
                        "   • Game statistics\n" +
                        "   • Match history and achievements\n" +
                        "   • Language preferences\n\n" +

                        "3.3. PAYMENT DATA:\n" +
                        "   • We DO NOT collect or store payment information\n" +
                        "   • All payments are processed by Google Play\n" +
                        "   • Purchase history is stored\n\n" +

                        "3.4. PvP AND SOCIAL DATA:\n" +
                        "   • PvP match history\n" +
                        "   • Win/loss statistics\n" +
                        "   • King of the Hill rankings\n" +
                        "   • Matchmaking data\n\n" +

                        "4. LEGAL BASIS FOR PROCESSING\n" +
                        "4.1. We process data based on:\n" +
                        "   • Contract performance (Terms of Service)\n" +
                        "   • Legitimate interests (service improvement)\n" +
                        "   • Consent (marketing communications)\n" +
                        "   • Legal obligations\n\n" +

                        "5. PURPOSES OF DATA COLLECTION\n" +
                        "5.1. We use data to:\n" +
                        "   • Provide App functionality\n" +
                        "   • Save game progress\n" +
                        "   • Organize PvP matches\n" +
                        "   • Calculate rankings and statistics\n" +
                        "   • Provide technical support\n" +
                        "   • Improve user experience\n" +
                        "   • Prevent fraud\n" +
                        "   • Analytics and optimization\n\n" +

                        "6. DATA SHARING WITH THIRD PARTIES\n" +
                        "6.1. WE DO NOT SELL your personal data.\n\n" +

                        "6.2. THIRD PARTY PROCESSORS:\n" +
                        "   • Google (Google Play Services, Firebase)\n" +
                        "       - Authentication\n" +
                        "       - Analytics\n" +
                        "       - Crash Reporting\n" +
                        "   • MongoDB (game content hosting)\n" +
                        "   • MySQL (user data hosting)\n" +
                        "   • Server infrastructure (ASP.NET Core)\n\n" +

                        "6.3. LEGAL REQUIREMENTS:\n" +
                        "   • We may disclose data if required by law\n" +
                        "   • To protect our rights and safety\n" +
                        "   • To prevent fraud\n\n" +

                        "7. INTERNATIONAL DATA TRANSFERS\n" +
                        "7.1. Your data may be transferred to:\n" +
                        "   • Your country of residence\n" +
                        "   • Server locations (EU/US)\n" +
                        "   • Using EU Standard Contractual Clauses\n" +
                        "   • In compliance with GDPR and local laws\n\n" +

                        "8. DATA STORAGE AND SECURITY\n" +
                        "8.1. RETENTION PERIODS:\n" +
                        "   • Account data: until account deletion\n" +
                        "   • Game statistics: 3 years after last activity\n" +
                        "   • Logs: 6 months\n" +
                        "   • Analytics data: 2 years\n\n" +

                        "8.2. SECURITY MEASURES:\n" +
                        "   • Data encryption in transit (TLS 1.3)\n" +
                        "   • Password hashing (bcrypt)\n" +
                        "   • Regular security updates\n" +
                        "   • Restricted data access\n" +
                        "   • Two-factor authentication (optional)\n\n" +

                        "9. USER RIGHTS\n" +
                        "9.1. Under GDPR and local laws, you have the right to:\n" +
                        "   • Access your data\n" +
                        "   • Rectify inaccurate data\n" +
                        "   • Erase data («right to be forgotten»)\n" +
                        "   • Restrict processing\n" +
                        "   • Object to processing\n" +
                        "   • Data portability\n" +
                        "   • Not be subject to automated decisions\n" +
                        "   • Withdraw consent at any time\n\n" +

                        "10. ACCOUNT DELETION\n" +
                        "10.1. You can delete your account:\n" +
                        "   • In App Settings (Settings → Account → Delete)\n" +
                        "   • By emailing privacy@geoquiz.app\n" +
                        "   • Through Google Play Games\n\n" +

                        "10.2. Upon deletion, we remove:\n" +
                        "   • All personal data\n" +
                        "   • Game statistics\n" +
                        "   • Match history\n" +
                        "   • Achievements\n" +
                        "   • Subscriptions\n\n" +

                        "11. COOKIES AND ANALYTICS\n" +
                        "11.1. We use:\n" +
                        "   • Firebase Analytics\n" +
                        "   • Crashlytics\n" +
                        "   • Custom match analytics\n" +
                        "11.2. NOT used for retargeting\n" +
                        "11.3. You can disable analytics in settings\n\n" +

                        "12. POLICY CHANGES\n" +
                        "12.1. We may update this Policy.\n" +
                        "12.2. We will notify you 30 days before material changes.\n" +
                        "12.3. Continued use constitutes acceptance of new terms.\n\n" +

                        "13. CONTACT INFORMATION\n" +
                        "13.1. For privacy inquiries:\n" +
                        "   • Email: privacy@geoquiz.app\n" +
                        "   • Address: Belarus, Grodno\n" +
                        "   • Response time: up to 14 business days\n\n" +

                        "14.2. Supervisory Authority:\n" +
                        "   • You have the right to lodge a complaint with your local data protection authority\n" +
                        "   • Contact details available upon request\n\n" +

                        "15. SPECIAL PROVISIONS\n\n" +

                        "15.1. FOR EU RESIDENTS (GDPR):\n" +
                        "   • Data Controller: GeoQuiz [registration details]\n" +
                        "   • EU Representative: [contact details]\n" +
                        "   • Legal basis: Art. 6 GDPR\n\n" +

                        "16. TECHNICAL DETAILS\n" +
                        "16.1. Processing occurs:\n" +
                        "   • Locally on device (Room database)\n" +
                        "   • On servers (MySQL, MongoDB)\n" +
                        "   • Using AES-256 encryption\n" +
                        "   • With regular backups\n\n" +

                        "17. PvP AND COMPETITIVE MODES\n" +
                        "17.1. In PvP modes we process:\n" +
                        "   • Temporary match data\n" +
                        "   • Results and statistics\n" +
                        "   • Interaction history\n" +
                        "   • IP addresses for matchmaking\n\n" +

                        "17.2. This data is necessary for:\n" +
                        "   • Ensuring fair play\n" +
                        "   • Preventing cheating\n" +
                        "   • Ranking calculations\n\n" +

                        "18. MARKETING COMMUNICATIONS\n" +
                        "18.1. Only with your consent\n" +
                        "18.2. You can unsubscribe at any time\n" +
                        "18.3. No data sharing for marketing with third parties\n\n" +

                        "⚠️ IMPORTANT: By using the App, you acknowledge that you have read and understood this Privacy Policy.\n\n" +

                        "📱 GeoQuiz — test your geography knowledge!\n" +
                        "Last updated: February 15, 2026";

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .setIcon(R.drawable.ic_privacy)
                .show();
    }
    private void showTermsDialog() {
        String currentLanguage = preferencesHelper.getLanguage();

        String title = "ru".equals(currentLanguage) ? "Пользовательское соглашение" : "Terms of Use";
        String message = "ru".equals(currentLanguage) ?
                "📝 ПОЛЬЗОВАТЕЛЬСКОЕ СОГЛАШЕНИЕ GEOQUIZ\n\n" +

                        "📅 Дата вступления в силу: 15 февраля 2026 г.\n" +
                        "📅 Последнее обновление: 15 февраля 2026 г.\n" +
                        "🌐 Версия: 2.0\n\n" +

                        "1. ОПРЕДЕЛЕНИЯ\n" +
                        "1.1. GeoQuiz — мобильное приложение для географических викторин.\n" +
                        "1.2. Правообладатель — компания GeoQuiz (далее — «Мы», «Нас», «Наш»).\n" +
                        "1.3. Пользователь — любое лицо, использующее Приложение (далее — «Вы», «Ваш»).\n" +
                        "1.4. Сервис — функциональность Приложения, включая игровые режимы.\n\n" +

                        "2. ПРИНЯТИЕ УСЛОВИЙ\n" +
                        "2.1. Используя Приложение, Вы подтверждаете, что:\n" +
                        "   • Прочитали и поняли условия Соглашения\n" +
                        "   • Принимаете все условия без ограничений\n" +
                        "   • Достигли возраста, необходимого для использования\n\n" +

                        "2.2. Если Вы не согласны с условиями, прекратите использование Приложения.\n\n" +

                        "3. РЕГИСТРАЦИЯ И АККАУНТ\n" +
                        "3.1. Для использования некоторых функций требуется регистрация.\n" +
                        "3.2. Вы обязуетесь:\n" +
                        "   • Предоставлять достоверную информацию\n" +
                        "   • Не создавать несколько аккаунтов\n" +
                        "   • Не передавать доступ третьим лицам\n" +
                        "   • Сохранять конфиденциальность пароля\n\n" +

                        "3.3. Мы имеем право:\n" +
                        "   • Блокировать подозрительные аккаунты\n" +
                        "   • Удалять неактивные аккаунты\n" +
                        "   • Требовать подтверждения данных\n\n" +

                        "4. ЛИЦЕНЗИЯ НА ИСПОЛЬЗОВАНИЕ\n" +
                        "4.1. Мы предоставляем Вам ограниченную, неисключительную, непередаваемую, отзывную лицензию на использование Приложения в личных, некоммерческих целях.\n\n" +

                        "4.2. ЛИЦЕНЗИЯ НЕ РАЗРЕШАЕТ:\n" +
                        "   • Копировать, модифицировать Приложение\n" +
                        "   • Распространять Приложение\n" +
                        "   • Декомпилировать, дизассемблировать код\n" +
                        "   • Создавать производные продукты\n" +
                        "   • Использовать в коммерческих целях\n" +
                        "   • Сдавать в аренду, субаренду\n\n" +

                        "5. ПРАВИЛА ПОВЕДЕНИЯ\n" +
                        "5.1. ЗАПРЕЩАЕТСЯ:\n" +
                        "   • Использовать читы, ботов, автоматизацию\n" +
                        "   • Эксплуатировать ошибки (баги)\n" +
                        "   • Публиковать неприемлемый контент\n" +
                        "   • Создавать оскорбительные никнеймы\n" +
                        "   • Пытаться взломать аккаунты\n" +
                        "   • Использовать Приложение для мошенничества\n\n" +

                        "5.2. Нарушение правил влечет:\n" +
                        "   • Предупреждение\n" +
                        "   • Временную блокировку\n" +
                        "   • Перманентную блокировку\n" + "Удаление аккаунта\n\n" +

        "6. PvP И СОРЕВНОВАТЕЛЬНЫЕ РЕЖИМЫ\n" +
                "6.1. Участвуя в PvP-режимах, Вы соглашаетесь:\n" +
                "   • Соблюдать принципы честной игры\n" +
                "   • Принимать результаты матчей\n" +
                "   • Не использовать сторонние программы\n" +
                "   • Не договариваться о результатах\n\n" +

                "6.2. Мы имеем право:\n" +
                "   • Аннулировать результаты подозрительных матчей\n" +
                "   • Корректировать рейтинги\n" +
                "   • Блокировать нарушителей\n\n" +

                "7. ПЛАТЕЖИ И ПОДПИСКИ\n" +
                "7.1. ПЛАТЕЖИ:\n" +
                "   • Обрабатываются через Google Play\n" +
                "   • Валюта определяется регионом\n" +
                "   • Цены могут меняться с уведомлением\n\n" +

                "7.2. ПОДПИСКИ:\n" +
                "   • Продлеваются автоматически\n" +
                "   • Отмена возможна в любое время\n" +
                "   • Средства за неиспользованный период не возвращаются\n" +
                "   • Цены фиксируются на период подписки\n\n" +

                "7.3. ВОЗВРАТ СРЕДСТВ:\n" +
                "   • Осуществляется через Google Play\n" +
                "   • Срок рассмотрения: до 14 дней\n" +
                "   • Технические ошибки рассматриваются индивидуально\n\n" +

                "8. ИНТЕЛЛЕКТУАЛЬНАЯ СОБСТВЕННОСТЬ\n" +
                "8.1. Мы владеем всеми правами на:\n" +
                "   • Исходный код Приложения\n" +
                "   • Дизайн и графику\n" +
                "   • Базу вопросов и ответов\n" +
                "   • Алгоритмы матчмейкинга\n" +
                "   • Бренд GeoQuiz (логотип, название)\n\n" +

                "8.2. Вы сохраняете права на:\n" +
                "   • Ваш игровой прогресс\n" +
                "   • Ваши достижения\n" +
                "   • Вашу статистику\n\n" +

                "8.3. Предоставляя отзыв, Вы разрешаете нам использовать его для улучшения Сервиса.\n\n" +

                "9. ОГРАНИЧЕНИЕ ОТВЕТСТВЕННОСТИ\n" +
                "9.1. ПРИЛОЖЕНИЕ ПРЕДОСТАВЛЯЕТСЯ «КАК ЕСТЬ»:\n" +
                "   • Без гарантий бесперебойной работы\n" +
                "   • Без гарантий точности всех данных\n" +
                "   • С возможными техническими задержками\n\n" +

                "9.2. МЫ НЕ НЕСЕМ ОТВЕТСТВЕННОСТИ ЗА:\n" +
                "   • Косвенные убытки\n" +
                "   • Потерю данных\n" +
                "   • Упущенную выгоду\n" +
                "   • Действия других пользователей\n" +
                "   • Сбои в работе интернета\n" +
                "   • Проблемы с устройством\n" +
                "   • Результаты использования Приложения\n\n" +

                "9.3. Максимальная ответственность ограничена стоимостью подписки за последние 12 месяцев.\n\n" +

                "10. КОНТЕНТ ПОЛЬЗОВАТЕЛЕЙ\n" +
                "10.1. Вы несете ответственность за создаваемый контент.\n" +
                "10.2. Мы имеем право удалять любой контент, нарушающий правила.\n" +
                "10.3. Мы не обязаны модерировать весь контент.\n\n" +

                "11. ПРЕКРАЩЕНИЕ ДОСТУПА\n" +
                "11.1. Мы можем прекратить или приостановить доступ:\n" +
                "   • При нарушении условий\n" +
                "   • По требованию органов власти\n" +
                "   • При технических проблемах\n" +
                "   • При подозрении в мошенничестве\n\n" +

                "11.2. Вы можете прекратить использование в любое время, удалив аккаунт.\n\n" +

                "12. ИЗМЕНЕНИЕ УСЛОВИЙ\n" +
                "12.1. Мы можем обновлять Соглашение.\n" +
                "12.2. Уведомляем за 30 дней до значительных изменений.\n" +
                "12.3. Продолжение использования означает принятие новой версии.\n\n" +

                "13. ПРИМЕНИМОЕ ПРАВО\n" +
                "13.1. Соглашение регулируется законодательством страны регистрации компании.\n" +
                "13.2. Споры разрешаются в суде по месту нахождения компании.\n\n" +

                "14. РАЗРЕШЕНИЕ СПОРОВ\n" +
                "14.1. Досудебный порядок обязателен:\n" +
                "   • Срок рассмотрения претензии: 30 дней\n" +
                "   • Претензия направляется на legal@geoquiz.app\n" +
                "   • Ответ предоставляется в письменной форме\n\n" +

                "15. ФОРС-МАЖОР\n" +
                "15.1. Мы освобождаемся от ответственности при:\n" +
                "   • Стихийных бедствиях\n" +
                "   • Военных действиях\n" +
                "   • Законах и актах госорганов\n" +
                "   • Техногенных катастрофах\n" +
                "   • Сбоях в работе интернета\n\n" +

                "16. ПОДДЕРЖКА ПОЛЬЗОВАТЕЛЕЙ\n" +
                "16.1. По вопросам использования:\n" +
                "   • Email: support@geoquiz.app\n" +
                "   • Telegram: @geoquiz_support\n" +
                "   • Время ответа: до 24 часов в будни\n\n" +

                "16.2. По юридическим вопросам:\n" +
                "   • Email: legal@geoquiz.app\n" +
                "   • Время ответа: до 5 рабочих дней\n\n" +

                "17. ЗАКЛЮЧИТЕЛЬНЫЕ ПОЛОЖЕНИЯ\n" +
                "17.1. Если часть условий признана недействительной, остальные продолжают действовать.\n" +
                "17.2. Неисполнение наших прав не является отказом от них.\n" +
                "17.3. Мы можем передавать права по Соглашению третьим лицам.\n" +
                "17.4. Вы не можете передавать права без нашего согласия.\n\n" +

                "18. ПОЛНОТА СОГЛАШЕНИЯ\n" +
                "18.1. Настоящее Соглашение вместе с Политикой конфиденциальности составляют полное соглашение между Вами и GeoQuiz относительно использования Приложения.\n\n" +

                "⚠️ ВАЖНО: Используя Приложение, Вы подтверждаете, что прочитали, поняли и согласны соблюдать условия настоящего Пользовательского соглашения.\n\n" +

                "📱 GeoQuiz — играй и познавай мир!\n" +
                "Последнее обновление: 15 февраля 2026 г." :

        "📝 GEOQUIZ TERMS OF USE\n\n" +

                "📅 Effective Date: February 15, 2026\n" +
                "📅 Last Updated: February 15, 2026\n" +
                "🌐 Version: 1.0\n\n" +

                "1. DEFINITIONS\n" +
                "1.1. GeoQuiz — a mobile application for geography quizzes.\n" +
                "1.2. Rights Holder — GeoQuiz company (hereinafter — «We», «Us», «Our»).\n" +
                "1.3. User — any person using the Application (hereinafter — «You», «Your»).\n" +
                "1.4. Service — Application functionality, including game modes.\n\n" +

                "2. ACCEPTANCE OF TERMS\n" +
                "2.1. By using the Application, You confirm that:\n" +
                "   • You have read and understood the Terms\n" +
                "   • You accept all terms without limitation\n" +
                "   • You have reached the required age\n\n" +

                "2.2. If You do not agree, stop using the Application.\n\n" +

                "3. REGISTRATION AND ACCOUNT\n" +
                "3.1. Registration is required for some features.\n" +
                "3.2. You agree to:\n" +
                "   • Provide accurate information\n" +
                "   • Not create multiple accounts\n" +
                "   • Not share access with third parties\n" +
                "   • Keep password confidential\n\n" +

                "3.3. We have the right to:\n" +
                "   • Block suspicious accounts\n" +
                "   • Delete inactive accounts\n" +
                "   • Request data verification\n\n" +

                "4. LICENSE TO USE\n" +
                "4.1. We grant You a limited, non-exclusive, non-transferable, revocable license to use the Application for personal, non-commercial purposes.\n\n" +

                "4.2. LICENSE DOES NOT ALLOW:\n" +
                "   • Copying, modifying the Application\n" +
                "   • Distributing the Application\n" +
                "   • Decompiling, disassembling code\n" +
                "   • Creating derivative products\n" +
                "   • Commercial use\n" +
                "   • Renting, sublicensing\n\n" +

                "5. CODE OF CONDUCT\n" +
                "5.1. PROHIBITED:\n" +
                "   • Using cheats, bots, automation\n" +
                "   • Exploiting bugs\n" +
                "   • Posting inappropriate content\n" +
                "   • Creating offensive usernames\n" +
                "   • Attempting account hacking\n" +
                "   • Using the Application for fraud\n\n" +

                "5.2. Violations may result in:\n" +
                "   • Warning\n" +
                "   • Temporary suspension\n" +
                "   • Permanent ban\n" +
                "   • Account deletion\n\n" +

                "6. PvP AND COMPETITIVE MODES\n" +
                "6.1. By participating in PvP modes, You agree to:\n" +
                "   • Follow fair play principles\n" +
                "   • Accept match results\n" +
                "   • Not use third-party programs\n" +
                "   • Not collude on results\n\n" +

                "6.2. We have the right to:\n" +
                "   • Void suspicious match results\n" +
                "   • Adjust rankings\n" +
                "   • Block violators\n\n" +

                "7. PAYMENTS AND SUBSCRIPTIONS\n" +
                "7.1. PAYMENTS:\n" +
                "   • Processed through Google Play\n" +
                "   • Currency determined by region\n" +
                "   • Prices may change with notice\n\n" +

                "7.2. SUBSCRIPTIONS:\n" +
                "   • Auto-renewable\n" +
                "   • Cancel anytime\n" +
                "   • No refunds for unused periods\n" +
                "   • Prices fixed during subscription\n\n" +

                "7.3. REFUNDS:\n" +
                "   • Handled through Google Play\n" +
                "   • Processing time: up to 14 days\n" +
                "   • Technical errors reviewed individually\n\n" +

                "8. INTELLECTUAL PROPERTY\n" +
                "8.1. We own all rights to:\n" +
                "   • Application source code\n" +
                "   • Design and graphics\n" +
                "   • Questions and answers database\n" +
                "   • Matchmaking algorithms\n" +
                "   • GeoQuiz brand (logo, name)\n\n" +

                "8.2. You retain rights to:\n" +
                "   • Your game progress\n" +
                "   • Your achievements\n" +
                "   • Your statistics\n\n" +

                "8.3. By providing feedback, You allow us to use it for Service improvement.\n\n" +

                "9. LIMITATION OF LIABILITY\n" +
                "9.1. APPLICATION PROVIDED «AS IS»:\n" +
                "   • No guarantees of uninterrupted operation\n" +
                "   • No guarantees of data accuracy\n" +
                "   • Possible technical delays\n\n" +

                "9.2. WE ARE NOT LIABLE FOR:\n" +
                "   • Indirect damages\n" +
                "   • Data loss\n" +
                "   • Lost profits\n" +
                "   • Actions of other users\n" +
                "   • Internet failures\n" +
                "   • Device problems\n" +
                "   • Results of Application use\n\n" +

                "9.3. Maximum liability limited to subscription cost for last 12 months.\n\n" +


                "10. USER CONTENT\n" +
                "10.1. You are responsible for created content.\n" +
                "10.2. We may remove any violating content.\n" +
                "10.3. We are not obligated to moderate all content.\n\n" +

                "11. TERMINATION\n" +
                "11.1. We may terminate or suspend access:\n" +
                "   • For Terms violation\n" +
                "   • By legal requirement\n" +
                "   • For technical issues\n" +
                "   • On suspicion of fraud\n\n" +

                "11.2. You may terminate use anytime by deleting account.\n\n" +

                "12. TERMS CHANGES\n" +
                "12.1. We may update the Terms.\n" +
                "12.2. We notify 30 days before material changes.\n" +
                "12.3. Continued use means acceptance of new version.\n\n" +

                "13. GOVERNING LAW\n" +
                "13.1. Terms governed by laws of company registration country.\n" +
                "13.2. Disputes resolved in courts at company location.\n\n" +

                "14. DISPUTE RESOLUTION\n" +
                "14.1. Pre-trial procedure mandatory:\n" +
                "   • Claim review: 30 days\n" +
                "   • Send claims to legal@geoquiz.app\n" +
                "   • Written response provided\n\n" +

                "15. FORCE MAJEURE\n" +
                "15.1. We are released from liability for:\n" +
                "   • Natural disasters\n" +
                "   • Military actions\n" +
                "   • Government acts\n" +
                "   • Man-made disasters\n" +
                "   • Internet failures\n\n" +

                "16. USER SUPPORT\n" +
                "16.1. For usage questions:\n" +
                "   • Email: support@geoquiz.app\n" +
                "   • Telegram: @geoquiz_support\n" +
                "   • Response time: up to 24 hours weekdays\n\n" +

                "16.2. For legal matters:\n" +
                "   • Email: legal@geoquiz.app\n" +
                "   • Response time: up to 5 business days\n\n" +

                "17. FINAL PROVISIONS\n" +
                "17.1. If any part is invalid, rest remains effective.\n" +
                "17.2. Failure to enforce rights is not waiver.\n" +
                "17.3. We may assign rights to third parties.\n" +
                "17.4. You may not assign rights without consent.\n\n" +

                "18. ENTIRE AGREEMENT\n" +
                "18.1. These Terms together with Privacy Policy constitute the entire agreement between You and GeoQuiz regarding Application use.\n\n" +

                "⚠️ IMPORTANT: By using the Application, You confirm that You have read, understood, and agree to comply with these Terms of Use.\n\n" +

                "📱 GeoQuiz — play and discover the world!\n" +
                "Last updated: February 15, 2026";

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .setIcon(R.drawable.ic_terms)
                .show();
    }
    private void openSubscriptionScreen() {
        Toast.makeText(this, "Экран подписки", Toast.LENGTH_SHORT).show();
//        Intent intent = new Intent(this, SubscriptionActivity.class);
//        startActivity(intent);
    }
    private void showTransferConfirmation() {
        String currentLanguage = preferencesHelper.getLanguage();

        String title = "ru".equals(currentLanguage)
                ? "Перенести прогресс на новый аккаунт?"
                : "Transfer progress to a new account?";

        String message = "ru".equals(currentLanguage)
                ? "Вы сохраните все достижения и сможете продолжить игру позже.\n\n" +
                "Для этого потребуется интернет-соединение.\n" +
                "Вы выйдете из текущего режима и сможете зарегистрироваться."
                : "Your progress and achievements will be saved.\n\n" +
                "An internet connection is required.\n" +
                "You will exit the current mode and can create an account.";

        String positiveButton = "ru".equals(currentLanguage)
                ? "Сохранить"
                : "Save";

        String negativeButton = "ru".equals(currentLanguage)
                ? "Отмена"
                : "Cancel";

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveButton, (dialog, which) -> openRegistration())
                .setNegativeButton(negativeButton, null)
                .show();
    }
    private void openRegistration() {
        Intent intent = new Intent(this, LoginActivity.class);
        UserStats stats = authManager.getCurrentStats();

        if (stats != null) {
            Gson gson = new Gson();
            String statsJson = gson.toJson(stats);
            intent.putExtra("USER_STATS_JSON", statsJson);
        }

        authManager.logout();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
    private void showLogoutConfirmation() {
        String currentLanguage = preferencesHelper.getLanguage();

        String title = "ru".equals(currentLanguage) ? "Выход из аккаунта" : "Logout";
        String message = "ru".equals(currentLanguage) ?
                "Вы уверены, что хотите выйти из аккаунта?" :
                "Are you sure you want to log out?";
        String positiveButton = "ru".equals(currentLanguage) ? "Выйти" : "Logout";
        String negativeButton = "ru".equals(currentLanguage) ? "Отмена" : "Cancel";
        String toastMessage = "ru".equals(currentLanguage) ?
                "Вы вышли из аккаунта" : "You have logged out";

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveButton, (dialog, which) -> logout())
                .setNegativeButton(negativeButton, null)
                .show();
    }
    private void logout() {
        String currentLanguage = preferencesHelper.getLanguage();
        String toastMessage = "ru".equals(currentLanguage) ?
                "Вы вышли из аккаунта" : "You have logged out";

        authManager.logout();
        Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    private void backToProfile(){
        Intent intent = new Intent(this, ProfileActivity.class);
        startActivity(intent);
        finish();
    }
}
