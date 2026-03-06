namespace GeoQuiz.Backend.Application.DTOs.PvP;
public class SubmitAnswerResponse
{
    public bool IsCorrect { get; set; }

    public int YourScore { get; set; }
    public int OpponentScore { get; set; }

    public int YourAnswered { get; set; }
    public int OpponentAnswered { get; set; }
}
