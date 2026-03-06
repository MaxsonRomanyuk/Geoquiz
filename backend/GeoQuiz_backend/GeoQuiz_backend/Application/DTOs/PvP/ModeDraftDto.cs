using GeoQuiz_backend.Domain.Enums;

namespace GeoQuiz_backend.Application.DTOs.PvP
{
    public class ModeDraftDto
    {
        public Guid MatchId { get; set; }
        public Guid CurrentTurnUserId { get; set; }
        public List<GameMode> AvailableModes { get; set; } = new();
        public List<GameMode> BannedModes { get; set; } = new();
        public int Step { get; set; }
        public PvPMatchStatus Status { get; set; }
        public GameMode? SelectedMode { get; set; }
    }
}
