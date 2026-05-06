using GeoQuiz_backend.Domain.Enums;
using MongoDB.Bson.Serialization.Attributes;
using System.Text.Json.Serialization;

namespace GeoQuiz_backend.Domain.Mongo
{
    public class Country
    {
        [BsonId]
        [BsonElement("_id")]
        [JsonPropertyName("_id")]
        public string Id { get; set; } = null!;
        public LocalizedText Name { get; set; } = null!;
        public LocalizedText Capital { get; set; } = null!;
        public string Region { get; set; } = null!;
        public string FlagImage { get; set; } = null!;
        public string OutlineImage { get; set; } = null!;
        public List<CountryLanguage> Languages { get; set; } = new();

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
    public class CountryLanguage
    {
        [JsonPropertyName("Id")]
        public string Id { get; set; } = null!;
        public LocalizedText Name { get; set; } = null!;
        public string AudioUrl { get; set; } = null!;
    }
}
