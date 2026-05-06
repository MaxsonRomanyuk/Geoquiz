namespace GeoQuiz_backend.Application.DTOs.KingOfTheHill
{
    public class SubmitAnswerRequest
    {
        public Guid MatchId { get; set; }
        public int RoundNumber { get; set; }
        public string CountryId { get; set; } = null!;
        public int SelectedOptionIndex { get; set; }
        public int TimeSpentMs { get; set; }
    }
}
