using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace GeoQuiz_backend.Migrations
{
    /// <inheritdoc />
    public partial class AddKothEntities : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<int>(
                name: "KothGamesPlayed",
                table: "userstats",
                type: "int",
                nullable: false,
                defaultValue: 0);

            migrationBuilder.AddColumn<int>(
                name: "KothGamesWon",
                table: "userstats",
                type: "int",
                nullable: false,
                defaultValue: 0);

            migrationBuilder.AddColumn<int>(
                name: "KothTop3Finishes",
                table: "userstats",
                type: "int",
                nullable: false,
                defaultValue: 0);

            migrationBuilder.AlterColumn<Guid>(
                name: "PvPMatchId",
                table: "questionset",
                type: "char(36)",
                nullable: true,
                collation: "ascii_general_ci",
                oldClrType: typeof(Guid),
                oldType: "char(36)")
                .OldAnnotation("Relational:Collation", "ascii_general_ci");

            migrationBuilder.AddColumn<Guid>(
                name: "KothMatchId",
                table: "questionset",
                type: "char(36)",
                nullable: true,
                collation: "ascii_general_ci");

            migrationBuilder.AddColumn<Guid>(
                name: "KothMatchId",
                table: "gamesessions",
                type: "char(36)",
                nullable: true,
                collation: "ascii_general_ci");

            migrationBuilder.AddColumn<int>(
                name: "Place",
                table: "gamesessions",
                type: "int",
                nullable: true);

            migrationBuilder.AddColumn<int>(
                name: "RoundsSurvived",
                table: "gamesessions",
                type: "int",
                nullable: true);

            migrationBuilder.CreateTable(
                name: "kothmatches",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "char(36)", nullable: false, collation: "ascii_general_ci"),
                    Status = table.Column<int>(type: "int", nullable: false),
                    SelectedMode = table.Column<int>(type: "int", nullable: true),
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

            migrationBuilder.CreateIndex(
                name: "IX_questionset_KothMatchId",
                table: "questionset",
                column: "KothMatchId");

            migrationBuilder.CreateIndex(
                name: "IX_gamesessions_KothMatchId",
                table: "gamesessions",
                column: "KothMatchId");

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

            migrationBuilder.AddForeignKey(
                name: "FK_gamesessions_kothmatches_KothMatchId",
                table: "gamesessions",
                column: "KothMatchId",
                principalTable: "kothmatches",
                principalColumn: "Id",
                onDelete: ReferentialAction.SetNull);

            migrationBuilder.AddForeignKey(
                name: "FK_questionset_kothmatches_KothMatchId",
                table: "questionset",
                column: "KothMatchId",
                principalTable: "kothmatches",
                principalColumn: "Id",
                onDelete: ReferentialAction.Cascade);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropForeignKey(
                name: "FK_gamesessions_kothmatches_KothMatchId",
                table: "gamesessions");

            migrationBuilder.DropForeignKey(
                name: "FK_questionset_kothmatches_KothMatchId",
                table: "questionset");

            migrationBuilder.DropTable(
                name: "kothanswers");

            migrationBuilder.DropTable(
                name: "kothplayers");

            migrationBuilder.DropTable(
                name: "kothmatches");

            migrationBuilder.DropIndex(
                name: "IX_questionset_KothMatchId",
                table: "questionset");

            migrationBuilder.DropIndex(
                name: "IX_gamesessions_KothMatchId",
                table: "gamesessions");

            migrationBuilder.DropColumn(
                name: "KothGamesPlayed",
                table: "userstats");

            migrationBuilder.DropColumn(
                name: "KothGamesWon",
                table: "userstats");

            migrationBuilder.DropColumn(
                name: "KothTop3Finishes",
                table: "userstats");

            migrationBuilder.DropColumn(
                name: "KothMatchId",
                table: "questionset");

            migrationBuilder.DropColumn(
                name: "KothMatchId",
                table: "gamesessions");

            migrationBuilder.DropColumn(
                name: "Place",
                table: "gamesessions");

            migrationBuilder.DropColumn(
                name: "RoundsSurvived",
                table: "gamesessions");

            migrationBuilder.AlterColumn<Guid>(
                name: "PvPMatchId",
                table: "questionset",
                type: "char(36)",
                nullable: false,
                defaultValue: new Guid("00000000-0000-0000-0000-000000000000"),
                collation: "ascii_general_ci",
                oldClrType: typeof(Guid),
                oldType: "char(36)",
                oldNullable: true)
                .OldAnnotation("Relational:Collation", "ascii_general_ci");
        }
    }
}
