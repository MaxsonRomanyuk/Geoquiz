using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace GeoQuiz_backend.Migrations
{
    /// <inheritdoc />
    public partial class InitialCreate : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AlterDatabase()
                .Annotation("MySql:CharSet", "utf8mb4");

            migrationBuilder.CreateTable(
                name: "achievements",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "char(36)", nullable: false, collation: "ascii_general_ci"),
                    Code = table.Column<string>(type: "varchar(50)", maxLength: 50, nullable: false)
                        .Annotation("MySql:CharSet", "utf8mb4"),
                    Title = table.Column<string>(type: "varchar(100)", maxLength: 100, nullable: false)
                        .Annotation("MySql:CharSet", "utf8mb4"),
                    Description = table.Column<string>(type: "varchar(500)", maxLength: 500, nullable: false)
                        .Annotation("MySql:CharSet", "utf8mb4"),
                    Icon = table.Column<string>(type: "varchar(100)", maxLength: 100, nullable: false)
                        .Annotation("MySql:CharSet", "utf8mb4"),
                    Category = table.Column<int>(type: "int", nullable: false),
                    Rarity = table.Column<int>(type: "int", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_achievements", x => x.Id);
                })
                .Annotation("MySql:CharSet", "utf8mb4");

            migrationBuilder.CreateTable(
                name: "users",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "char(36)", nullable: false, collation: "ascii_general_ci"),
                    UserName = table.Column<string>(type: "varchar(100)", maxLength: 100, nullable: false)
                        .Annotation("MySql:CharSet", "utf8mb4"),
                    Email = table.Column<string>(type: "varchar(200)", maxLength: 200, nullable: false)
                        .Annotation("MySql:CharSet", "utf8mb4"),
                    PasswordHash = table.Column<string>(type: "varchar(500)", maxLength: 500, nullable: false)
                        .Annotation("MySql:CharSet", "utf8mb4"),
                    RegisteredAt = table.Column<DateTime>(type: "datetime(6)", nullable: false),
                    IsPremium = table.Column<bool>(type: "tinyint(1)", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_users", x => x.Id);
                })
                .Annotation("MySql:CharSet", "utf8mb4");

            migrationBuilder.CreateTable(
                name: "kothmatches",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "char(36)", nullable: false, collation: "ascii_general_ci"),
                    Status = table.Column<int>(type: "int", nullable: false),
                    SelectedMode = table.Column<int>(type: "int", nullable: false),
                    WinnerId = table.Column<Guid>(type: "char(36)", nullable: true, collation: "ascii_general_ci"),
                    CurrentRound = table.Column<int>(type: "int", nullable: false, defaultValue: 1),
                    CurrentRoundType = table.Column<int>(type: "int", nullable: false, defaultValue: 1),
                    CreatedAt = table.Column<DateTime>(type: "datetime(6)", nullable: false),
                    StartedAt = table.Column<DateTime>(type: "datetime(6)", nullable: true),
                    FinishedAt = table.Column<DateTime>(type: "datetime(6)", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_kothmatches", x => x.Id);
                    table.ForeignKey(
                        name: "FK_kothmatches_users_WinnerId",
                        column: x => x.WinnerId,
                        principalTable: "users",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.SetNull);
                })
                .Annotation("MySql:CharSet", "utf8mb4");

            migrationBuilder.CreateTable(
                name: "pvpmatches",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "char(36)", nullable: false, collation: "ascii_general_ci"),
                    Player1Id = table.Column<Guid>(type: "char(36)", nullable: false, collation: "ascii_general_ci"),
                    Player2Id = table.Column<Guid>(type: "char(36)", nullable: false, collation: "ascii_general_ci"),
                    Status = table.Column<int>(type: "int", nullable: false),
                    SelectedMode = table.Column<int>(type: "int", nullable: true),
                    WinnerId = table.Column<Guid>(type: "char(36)", nullable: true, collation: "ascii_general_ci"),
                    CreatedAt = table.Column<DateTime>(type: "datetime(6)", nullable: false),
                    FinishedAt = table.Column<DateTime>(type: "datetime(6)", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_pvpmatches", x => x.Id);
                    table.ForeignKey(
                        name: "FK_pvpmatches_users_Player1Id",
                        column: x => x.Player1Id,
                        principalTable: "users",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Restrict);
                    table.ForeignKey(
                        name: "FK_pvpmatches_users_Player2Id",
                        column: x => x.Player2Id,
                        principalTable: "users",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Restrict);
                    table.ForeignKey(
                        name: "FK_pvpmatches_users_WinnerId",
                        column: x => x.WinnerId,
                        principalTable: "users",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.SetNull);
                })
                .Annotation("MySql:CharSet", "utf8mb4");

            migrationBuilder.CreateTable(
                name: "subscriptions",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "char(36)", nullable: false, collation: "ascii_general_ci"),
                    UserId = table.Column<Guid>(type: "char(36)", nullable: false, collation: "ascii_general_ci"),
                    StartDate = table.Column<DateTime>(type: "datetime(6)", nullable: false),
                    EndDate = table.Column<DateTime>(type: "datetime(6)", nullable: false),
                    Type = table.Column<string>(type: "varchar(50)", maxLength: 50, nullable: false)
                        .Annotation("MySql:CharSet", "utf8mb4"),
                    IsActive = table.Column<bool>(type: "tinyint(1)", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_subscriptions", x => x.Id);
                    table.ForeignKey(
                        name: "FK_subscriptions_users_UserId",
                        column: x => x.UserId,
                        principalTable: "users",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                })
                .Annotation("MySql:CharSet", "utf8mb4");

            migrationBuilder.CreateTable(
                name: "userachievements",
                columns: table => new
                {
                    UserId = table.Column<Guid>(type: "char(36)", nullable: false, collation: "ascii_general_ci"),
                    AchievementId = table.Column<Guid>(type: "char(36)", nullable: false, collation: "ascii_general_ci"),
                    Progress = table.Column<int>(type: "int", nullable: false),
                    IsUnlocked = table.Column<bool>(type: "tinyint(1)", nullable: false),
                    UnlockedAt = table.Column<DateTime>(type: "datetime(6)", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_userachievements", x => new { x.UserId, x.AchievementId });
                    table.ForeignKey(
                        name: "FK_userachievements_achievements_AchievementId",
                        column: x => x.AchievementId,
                        principalTable: "achievements",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_userachievements_users_UserId",
                        column: x => x.UserId,
                        principalTable: "users",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                })
                .Annotation("MySql:CharSet", "utf8mb4");

            migrationBuilder.CreateTable(
                name: "userstats",
                columns: table => new
                {
                    UserId = table.Column<Guid>(type: "char(36)", nullable: false, collation: "ascii_general_ci"),
                    Level = table.Column<int>(type: "int", nullable: false, defaultValue: 1),
                    Experience = table.Column<int>(type: "int", nullable: false),
                    TotalGamesPlayed = table.Column<int>(type: "int", nullable: false),
                    TotalGamesWon = table.Column<int>(type: "int", nullable: false),
                    TotalCorrectAnswers = table.Column<int>(type: "int", nullable: false),
                    TotalQuickAnswers = table.Column<int>(type: "int", nullable: false),
                    TotalLastSecondWins = table.Column<int>(type: "int", nullable: false),
                    CurrentWinStreak = table.Column<int>(type: "int", nullable: false, defaultValue: 0),
                    MaxWinStreak = table.Column<int>(type: "int", nullable: false, defaultValue: 0),
                    DailyLoginStreak = table.Column<int>(type: "int", nullable: false, defaultValue: 0),
                    LastLoginDate = table.Column<DateTime>(type: "datetime(6)", nullable: false),
                    EuropeCorrect = table.Column<int>(type: "int", nullable: false),
                    AsiaCorrect = table.Column<int>(type: "int", nullable: false),
                    AfricaCorrect = table.Column<int>(type: "int", nullable: false),
                    AmericaCorrect = table.Column<int>(type: "int", nullable: false),
                    OceaniaCorrect = table.Column<int>(type: "int", nullable: false),
                    FlagsCorrect = table.Column<int>(type: "int", nullable: false),
                    CapitalsCorrect = table.Column<int>(type: "int", nullable: false),
                    OutlinesCorrect = table.Column<int>(type: "int", nullable: false),
                    LanguagesCorrect = table.Column<int>(type: "int", nullable: false),
                    PvPGamesPlayed = table.Column<int>(type: "int", nullable: false),
                    PvPGamesWon = table.Column<int>(type: "int", nullable: false),
                    CurrentPvPStreak = table.Column<int>(type: "int", nullable: false),
                    KothGamesPlayed = table.Column<int>(type: "int", nullable: false, defaultValue: 0),
                    KothGamesWon = table.Column<int>(type: "int", nullable: false, defaultValue: 0),
                    KothTop3Finishes = table.Column<int>(type: "int", nullable: false, defaultValue: 0)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_userstats", x => x.UserId);
                    table.ForeignKey(
                        name: "FK_userstats_users_UserId",
                        column: x => x.UserId,
                        principalTable: "users",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                })
                .Annotation("MySql:CharSet", "utf8mb4");

            migrationBuilder.CreateTable(
                name: "kothanswers",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "char(36)", nullable: false, collation: "ascii_general_ci"),
                    MatchId = table.Column<Guid>(type: "char(36)", nullable: false, collation: "ascii_general_ci"),
                    UserId = table.Column<Guid>(type: "char(36)", nullable: false, collation: "ascii_general_ci"),
                    QuestionId = table.Column<string>(type: "varchar(100)", maxLength: 100, nullable: false)
                        .Annotation("MySql:CharSet", "utf8mb4"),
                    RoundNumber = table.Column<int>(type: "int", nullable: false),
                    IsCorrect = table.Column<bool>(type: "tinyint(1)", nullable: false),
                    TimeSpentMs = table.Column<int>(type: "int", nullable: false),
                    ScoreGained = table.Column<int>(type: "int", nullable: false),
                    AnsweredAt = table.Column<DateTime>(type: "datetime(6)", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_kothanswers", x => x.Id);
                    table.ForeignKey(
                        name: "FK_kothanswers_kothmatches_MatchId",
                        column: x => x.MatchId,
                        principalTable: "kothmatches",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_kothanswers_users_UserId",
                        column: x => x.UserId,
                        principalTable: "users",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                })
                .Annotation("MySql:CharSet", "utf8mb4");

            migrationBuilder.CreateTable(
                name: "kothplayers",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "char(36)", nullable: false, collation: "ascii_general_ci"),
                    MatchId = table.Column<Guid>(type: "char(36)", nullable: false, collation: "ascii_general_ci"),
                    UserId = table.Column<Guid>(type: "char(36)", nullable: false, collation: "ascii_general_ci"),
                    JoinedAt = table.Column<DateTime>(type: "datetime(6)", nullable: false),
                    Place = table.Column<int>(type: "int", nullable: true),
                    IsActive = table.Column<bool>(type: "tinyint(1)", nullable: false, defaultValue: true),
                    RoundEliminated = table.Column<int>(type: "int", nullable: false, defaultValue: 0)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_kothplayers", x => x.Id);
                    table.ForeignKey(
                        name: "FK_kothplayers_kothmatches_MatchId",
                        column: x => x.MatchId,
                        principalTable: "kothmatches",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_kothplayers_users_UserId",
                        column: x => x.UserId,
                        principalTable: "users",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                })
                .Annotation("MySql:CharSet", "utf8mb4");

            migrationBuilder.CreateTable(
                name: "gamesessions",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "char(36)", nullable: false, collation: "ascii_general_ci"),
                    UserId = table.Column<Guid>(type: "char(36)", nullable: false, collation: "ascii_general_ci"),
                    PvPMatchId = table.Column<Guid>(type: "char(36)", nullable: true, collation: "ascii_general_ci"),
                    KothMatchId = table.Column<Guid>(type: "char(36)", nullable: true, collation: "ascii_general_ci"),
                    Place = table.Column<int>(type: "int", nullable: true),
                    RoundsSurvived = table.Column<int>(type: "int", nullable: true),
                    Mode = table.Column<int>(type: "int", nullable: false),
                    TotalQuestions = table.Column<int>(type: "int", nullable: false),
                    CorrectAnswers = table.Column<int>(type: "int", nullable: false),
                    Score = table.Column<int>(type: "int", nullable: false),
                    IsOnline = table.Column<bool>(type: "tinyint(1)", nullable: false),
                    PlayedAt = table.Column<DateTime>(type: "datetime(6)", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_gamesessions", x => x.Id);
                    table.ForeignKey(
                        name: "FK_gamesessions_kothmatches_KothMatchId",
                        column: x => x.KothMatchId,
                        principalTable: "kothmatches",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.SetNull);
                    table.ForeignKey(
                        name: "FK_gamesessions_pvpmatches_PvPMatchId",
                        column: x => x.PvPMatchId,
                        principalTable: "pvpmatches",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.SetNull);
                    table.ForeignKey(
                        name: "FK_gamesessions_users_UserId",
                        column: x => x.UserId,
                        principalTable: "users",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                })
                .Annotation("MySql:CharSet", "utf8mb4");

            migrationBuilder.CreateTable(
                name: "modedraft",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "char(36)", nullable: false, collation: "ascii_general_ci"),
                    PvPMatchId = table.Column<Guid>(type: "char(36)", nullable: false, collation: "ascii_general_ci"),
                    CurrentTurnUserId = table.Column<Guid>(type: "char(36)", nullable: false, collation: "ascii_general_ci"),
                    AvailableModes = table.Column<string>(type: "longtext", nullable: false)
                        .Annotation("MySql:CharSet", "utf8mb4"),
                    BannedModes = table.Column<string>(type: "longtext", nullable: false)
                        .Annotation("MySql:CharSet", "utf8mb4"),
                    Step = table.Column<int>(type: "int", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_modedraft", x => x.Id);
                    table.ForeignKey(
                        name: "FK_modedraft_pvpmatches_PvPMatchId",
                        column: x => x.PvPMatchId,
                        principalTable: "pvpmatches",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                })
                .Annotation("MySql:CharSet", "utf8mb4");

            migrationBuilder.CreateTable(
                name: "pvpanswers",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "char(36)", nullable: false, collation: "ascii_general_ci"),
                    MatchId = table.Column<Guid>(type: "char(36)", nullable: false, collation: "ascii_general_ci"),
                    UserId = table.Column<Guid>(type: "char(36)", nullable: false, collation: "ascii_general_ci"),
                    QuestionId = table.Column<string>(type: "varchar(100)", maxLength: 100, nullable: false)
                        .Annotation("MySql:CharSet", "utf8mb4"),
                    IsCorrect = table.Column<bool>(type: "tinyint(1)", nullable: false),
                    TimeSpentMs = table.Column<int>(type: "int", nullable: false),
                    ScoreGained = table.Column<int>(type: "int", nullable: false),
                    AnsweredAt = table.Column<DateTime>(type: "datetime(6)", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_pvpanswers", x => x.Id);
                    table.ForeignKey(
                        name: "FK_pvpanswers_pvpmatches_MatchId",
                        column: x => x.MatchId,
                        principalTable: "pvpmatches",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_pvpanswers_users_UserId",
                        column: x => x.UserId,
                        principalTable: "users",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                })
                .Annotation("MySql:CharSet", "utf8mb4");

            migrationBuilder.CreateTable(
                name: "questionset",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "char(36)", nullable: false, collation: "ascii_general_ci"),
                    PvPMatchId = table.Column<Guid>(type: "char(36)", nullable: true, collation: "ascii_general_ci"),
                    KothMatchId = table.Column<Guid>(type: "char(36)", nullable: true, collation: "ascii_general_ci"),
                    Mode = table.Column<int>(type: "int", nullable: false),
                    Language = table.Column<int>(type: "int", nullable: false),
                    QuestionIds = table.Column<string>(type: "longtext", nullable: false)
                        .Annotation("MySql:CharSet", "utf8mb4"),
                    Seed = table.Column<int>(type: "int", nullable: false),
                    CreatedAt = table.Column<DateTime>(type: "datetime(6)", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_questionset", x => x.Id);
                    table.ForeignKey(
                        name: "FK_questionset_kothmatches_KothMatchId",
                        column: x => x.KothMatchId,
                        principalTable: "kothmatches",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_questionset_pvpmatches_PvPMatchId",
                        column: x => x.PvPMatchId,
                        principalTable: "pvpmatches",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                })
                .Annotation("MySql:CharSet", "utf8mb4");

            migrationBuilder.CreateIndex(
                name: "IX_achievements_Code",
                table: "achievements",
                column: "Code",
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_gamesessions_KothMatchId",
                table: "gamesessions",
                column: "KothMatchId");

            migrationBuilder.CreateIndex(
                name: "IX_gamesessions_PvPMatchId",
                table: "gamesessions",
                column: "PvPMatchId");

            migrationBuilder.CreateIndex(
                name: "IX_gamesessions_UserId",
                table: "gamesessions",
                column: "UserId");

            migrationBuilder.CreateIndex(
                name: "IX_kothanswers_MatchId_RoundNumber",
                table: "kothanswers",
                columns: new[] { "MatchId", "RoundNumber" });

            migrationBuilder.CreateIndex(
                name: "IX_kothanswers_UserId",
                table: "kothanswers",
                column: "UserId");

            migrationBuilder.CreateIndex(
                name: "IX_kothmatches_WinnerId",
                table: "kothmatches",
                column: "WinnerId");

            migrationBuilder.CreateIndex(
                name: "IX_kothplayers_MatchId_UserId",
                table: "kothplayers",
                columns: new[] { "MatchId", "UserId" },
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_kothplayers_UserId",
                table: "kothplayers",
                column: "UserId");

            migrationBuilder.CreateIndex(
                name: "IX_modedraft_PvPMatchId",
                table: "modedraft",
                column: "PvPMatchId",
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_pvpanswers_MatchId",
                table: "pvpanswers",
                column: "MatchId");

            migrationBuilder.CreateIndex(
                name: "IX_pvpanswers_UserId",
                table: "pvpanswers",
                column: "UserId");

            migrationBuilder.CreateIndex(
                name: "IX_pvpmatches_Player1Id",
                table: "pvpmatches",
                column: "Player1Id");

            migrationBuilder.CreateIndex(
                name: "IX_pvpmatches_Player2Id",
                table: "pvpmatches",
                column: "Player2Id");

            migrationBuilder.CreateIndex(
                name: "IX_pvpmatches_WinnerId",
                table: "pvpmatches",
                column: "WinnerId");

            migrationBuilder.CreateIndex(
                name: "IX_questionset_KothMatchId",
                table: "questionset",
                column: "KothMatchId");

            migrationBuilder.CreateIndex(
                name: "IX_questionset_PvPMatchId",
                table: "questionset",
                column: "PvPMatchId",
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_subscriptions_UserId",
                table: "subscriptions",
                column: "UserId",
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_userachievements_AchievementId",
                table: "userachievements",
                column: "AchievementId");

            migrationBuilder.CreateIndex(
                name: "IX_users_Email",
                table: "users",
                column: "Email",
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_users_UserName",
                table: "users",
                column: "UserName",
                unique: true);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(
                name: "gamesessions");

            migrationBuilder.DropTable(
                name: "kothanswers");

            migrationBuilder.DropTable(
                name: "kothplayers");

            migrationBuilder.DropTable(
                name: "modedraft");

            migrationBuilder.DropTable(
                name: "pvpanswers");

            migrationBuilder.DropTable(
                name: "questionset");

            migrationBuilder.DropTable(
                name: "subscriptions");

            migrationBuilder.DropTable(
                name: "userachievements");

            migrationBuilder.DropTable(
                name: "userstats");

            migrationBuilder.DropTable(
                name: "kothmatches");

            migrationBuilder.DropTable(
                name: "pvpmatches");

            migrationBuilder.DropTable(
                name: "achievements");

            migrationBuilder.DropTable(
                name: "users");
        }
    }
}
