namespace GeoQuiz_backend.Application.DTOs.PvP
{
    public class SubmitAnswerRequest
    {
        public Guid MatchId { get; set; }
        public string CountryId { get; set; } = null!;
        public int SelectedIndex { get; set; }
        public int TimeSpentMs { get; set; }
        public int QuestionNumber { get; set; }
    }
}
