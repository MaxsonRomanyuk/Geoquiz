using GeoQuiz_backend.Domain.Mongo;

namespace GeoQuiz_backend.Application.Payloads.Questions
{
    public class CountryGrouping
    {
        public List<Country> AllCountries { get; set; }
        public ILookup<string, Country> CountriesByRegion { get; set; }
        public List<string> Regions { get; set; }

        public CountryGrouping(List<Country> allCountries)
        {
            AllCountries = allCountries;
            CountriesByRegion = allCountries.ToLookup(c => c.Region);
            Regions = CountriesByRegion.Select(g => g.Key).ToList();
        }
    }
}
