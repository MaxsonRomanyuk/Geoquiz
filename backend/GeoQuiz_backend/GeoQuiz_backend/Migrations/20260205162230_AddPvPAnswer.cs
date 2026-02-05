using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace GeoQuiz_backend.Migrations
{
    /// <inheritdoc />
    public partial class AddPvPAnswer : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
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

            migrationBuilder.CreateIndex(
                name: "IX_pvpanswers_MatchId",
                table: "pvpanswers",
                column: "MatchId");

            migrationBuilder.CreateIndex(
                name: "IX_pvpanswers_UserId",
                table: "pvpanswers",
                column: "UserId");
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(
                name: "pvpanswers");
        }
    }
}
