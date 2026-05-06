using GeoQuiz_backend.Domain.Enums;
using GeoQuiz_backend.Domain.Mongo;
using MongoDB.Driver;
using System.Globalization;
using System.Text;
using System.Text.Json;

namespace GeoQuiz_backend.Infrastructure.Persistence.Mongo
{
    public class MongoSeeder
    {
        private readonly IMongoCollection<Country> _countries;

        public MongoSeeder(MongoContext context)
        {
            _countries = context.GetCollection<Country>("countries");
        }
        public async Task SeedCountriesAsync()
        {
            if (await _countries.CountDocumentsAsync(_ => true) > 0)
                return;

            try
            {
                var jsonPath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "seedData", "countries.json");

                if (!File.Exists(jsonPath))
                {
                    jsonPath = Path.Combine(Directory.GetCurrentDirectory(), "seedData", "countries.json");
                }

                if (!File.Exists(jsonPath))
                {
                    throw new FileNotFoundException($"JSON file not found at {jsonPath}");
                }

                var jsonContent = await File.ReadAllTextAsync(jsonPath);
                var seedCountries = JsonSerializer.Deserialize<List<Country>>(jsonContent);

                if (seedCountries == null || !seedCountries.Any())
                {
                    throw new InvalidOperationException("No countries found in JSON file");
                }

                var countries = seedCountries.Select(sc => new Country
                {
                    Id = sc.Id,
                    Name = sc.Name,
                    Capital = sc.Capital,
                    Region = sc.Region,
                    FlagImage = sc.FlagImage,
                    OutlineImage = sc.OutlineImage,
                    Languages = sc.Languages.Select(l => new CountryLanguage
                    {
                        Id = l.Id,
                        Name = l.Name,
                        AudioUrl = l.AudioUrl
                    }).ToList()
                }).ToList();

                await _countries.InsertManyAsync(countries);

                Console.WriteLine($"Successfully seeded {countries.Count} countries");
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Error seeding countries: {ex.Message}");
                throw;
            }
        }
        
    }
}
