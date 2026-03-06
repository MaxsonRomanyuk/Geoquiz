using GeoQuiz.Backend.Domain.Enums;

namespace GeoQuiz.Backend.Application.DTOs.PvP;
public class PvPGameStateDto
{
    public Guid MatchId { get; set; }
    public GameMode Mode { get; set; }
    public AppLanguage Language { get; set; }
    public List<string> QuestionIds { get; set; } = new();
    public int YourAnswered { get; set; }
    public int OpponentAnswered { get; set; }
    public bool IsFinished { get; set; }
}
