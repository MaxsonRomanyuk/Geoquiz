namespace GeoQuiz_backend.Application.DTOs.KingOfTheHill
{
    public class PlayerEliminatedData
    {
        public int RoundNumber { get; set; }
        public int Place { get; set; } 
        public int CorrectAnswers { get; set; }
        public int TotalScore { get; set; }
        public int RoundsSurvived { get; set; }
    }
}
