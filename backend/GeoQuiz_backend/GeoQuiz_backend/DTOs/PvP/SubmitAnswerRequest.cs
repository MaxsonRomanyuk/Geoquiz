namespace GeoQuiz_backend.DTOs.PvP
{
    public class SubmitAnswerRequest
    {
        public Guid MatchId { get; set; }           
        public string QuestionId { get; set; } = null!;
        public int SelectedIndex { get; set; }
        public int TimeSpentMs { get; set; }
        public int QuestionNumber { get; set; }
    }
}
