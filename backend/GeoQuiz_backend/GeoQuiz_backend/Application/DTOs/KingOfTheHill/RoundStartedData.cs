using GeoQuiz_backend.Application.DTOs.PvP;
using GeoQuiz_backend.Application.Payloads.Questions;
using GeoQuiz_backend.Domain.Entities;
using GeoQuiz_backend.Domain.Mongo;

namespace GeoQuiz_backend.Application.DTOs.KingOfTheHill
{
    public class RoundStartedData
    {
        public int RoundNumber { get; set; }
        public int RoundType { get; set; } 
        public QuestionData Question { get; set; } = null!;
        public long ServerTime { get; set; }
        public long RoundEndAt { get; set; }
    }

}
