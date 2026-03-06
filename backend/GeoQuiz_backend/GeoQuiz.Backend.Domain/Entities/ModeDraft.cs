using GeoQuiz.Backend.Domain.Enums;

namespace GeoQuiz.Backend.Domain.Entities
{
    public class ModeDraft
    {
        public Guid Id { get; set; }

        public Guid PvPMatchId { get; set; }
        public PvPMatch PvPMatch { get; set; } = null!;

        public Guid CurrentTurnUserId { get; set; }
       
        public List<GameMode> AvailableModes { get; set; } = new();

        public List<GameMode> BannedModes { get; set; } = new();

        public int Step { get; set; } 
    }
}
