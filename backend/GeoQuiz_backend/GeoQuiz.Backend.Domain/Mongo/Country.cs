namespace GeoQuiz.Backend.Domain.Mongo
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
    }
}
