using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace GeoQuiz_backend.Migrations
{
    /// <inheritdoc />
    public partial class UpdateQuestionSet : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<string>(
                name: "Regions",
                table: "questionset",
                type: "longtext",
                nullable: false)
                .Annotation("MySql:CharSet", "utf8mb4");
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(
                name: "Regions",
                table: "questionset");
        }
    }
}
