namespace GeoQuiz_backend.Application.DTOs.PvP
{
    public class SubmitAnswerResponse
    {
        public bool IsCorrect { get; set; }
        public int CorrectOptionIndex { get; set; }
        public int QuestionNumber { get; set; }
        public int YourScore { get; set; }
        public int OpponentScore { get; set; }

        public int YourAnswered { get; set; }
        public int OpponentAnswered { get; set; }
    }
}
