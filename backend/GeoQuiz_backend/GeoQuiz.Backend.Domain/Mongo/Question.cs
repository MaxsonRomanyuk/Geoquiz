using GeoQuiz.Backend.Domain.Enums;

namespace GeoQuiz.Backend.Domain.Mongo
{
    public class Question
    {
        public string Id { get; set; }

        public string CountryId { get; set; }
        public int Difficulty { get; set; }
        public GameMode Type { get; set; }
    }
}
