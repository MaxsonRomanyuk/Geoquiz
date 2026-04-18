using GeoQuiz_backend.Domain.Enums;
using MongoDB.Bson.Serialization.Attributes;

namespace GeoQuiz_backend.Domain.Mongo
{
    public class Country
    {
        public string Id { get; set; }

        public LocalizedText Name { get; set; }
        public LocalizedText Capital { get; set; }

        public string Region { get; set; }

        public string FlagImage { get; set; }
        public string OutlineImage { get; set; }
        public string LanguageAudio { get; set; }

        [BsonIgnore]
        public Region RegionEnum => GetRegionEnum();

        private Region GetRegionEnum() => Region switch
        {
            "europe" => Enums.Region.Europe,
            "asia" => Enums.Region.Asia,
            "africa" => Enums.Region.Africa,
            "america" => Enums.Region.America,
            "oceania" => Enums.Region.Oceania,
            _ => throw new ArgumentException($"Unknown region: {Region}")
        };
    }
}
