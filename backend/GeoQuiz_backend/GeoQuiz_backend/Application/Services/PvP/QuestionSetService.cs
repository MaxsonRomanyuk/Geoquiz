using GeoQuiz_backend.Application.Interfaces;
using GeoQuiz_backend.Application.Payloads.Koth;
using GeoQuiz_backend.Application.Payloads.Questions;
using GeoQuiz_backend.Domain.Entities;
using GeoQuiz_backend.Domain.Enums;
using GeoQuiz_backend.Domain.Mongo;
using GeoQuiz_backend.Infrastructure.Persistence.MySQL;
using Microsoft.EntityFrameworkCore;

namespace GeoQuiz_backend.Application.Services.PvP
{
    public class QuestionSetService : IQuestionSetService
    {
        private readonly AppDbContext _db;
        private readonly ICountryRepository _countryRepo;

        public QuestionSetService(AppDbContext db, ICountryRepository countryRepo)
        {
            _db = db;
            _countryRepo = countryRepo;
        }

        public async Task<QuestionSet> CreateForMatchAsync(Guid matchId)
        {
            var match = await _db.PvPMatches
                .Include(m => m.QuestionSet)
                .FirstAsync(m => m.Id == matchId);

            if (match.QuestionSet != null)
                return match.QuestionSet;

            if (match.SelectedMode == null)
                throw new Exception("Game mode not selected yet");

            var seed = Random.Shared.Next();
            var rnd = new Random(seed);

            var allCountries = await _countryRepo.GetAllAsync();
            var selected = allCountries
                .OrderBy(q => rnd.Next())
                .Take(10)
                .ToList();

            var questionSet = new QuestionSet
            {
                Id = Guid.NewGuid(),
                PvPMatchId = matchId,
                Mode = match.SelectedMode.Value,
                Seed = seed,
                CreatedAt = DateTime.UtcNow,
                CountryIds = selected.Select(s => s.Id).ToList(),
                Regions = selected.Select(s => s.RegionEnum).ToList(),
            };

            match.QuestionSet = questionSet;
            _db.QuestionSets.Add(questionSet);

            await _db.SaveChangesAsync();
            return questionSet;
        }
        public async Task<List<GameQuestion>> GenerateQuestionsAsync(int count, int seed, GameMode mode)
        {
            var questions = new List<GameQuestion>();
            var allCountries = await _countryRepo.GetAllAsync();

            var random = new Random(seed);

            var selectedCountries = allCountries
                .OrderBy(x => random.Next())
                .Take(count)
                .ToList();

            foreach (var country in selectedCountries)
            {
                var optionRandom = new Random(seed + country.Id.GetHashCode());

                var options = new List<GameOption>();
                var correctOption = new GameOption
                {
                    Index = 0,
                    Text = GetLocalizedTextForCountry(country, mode)
                };

                var wrongCountries = allCountries
                    .Where(c => c.Id != country.Id)
                    .OrderBy(x => optionRandom.Next())
                    .Take(3)
                    .ToList();

                options.Add(correctOption);
                foreach (var wrongCountry in wrongCountries)
                {
                    options.Add(new GameOption
                    {
                        Index = options.Count,
                        Text = GetLocalizedTextForCountry(wrongCountry, mode)
                    });
                }

                options = options.OrderBy(x => optionRandom.Next()).ToList();

                var correctIndex = options.FindIndex(o =>
                    o.Text.Ru == correctOption.Text.Ru &&
                    o.Text.En == correctOption.Text.En);

                questions.Add(new GameQuestion
                {
                    CountryId = country.Id,
                    QuestionText = GetQuestionLocalizedText(mode, country),
                    Options = options,
                    CorrectOptionIndex = correctIndex,
                    ImageUrl = GetImageUrl(mode, country),
                    AudioUrl = GetAudioUrl(mode, country)
                });
            }

            return questions;
        }
        private LocalizedText GetLocalizedTextForCountry(Country country, GameMode mode)
        {
            return mode switch
            {
                GameMode.Capital => country.Capital,
                GameMode.Flag => country.Name,
                GameMode.Outline => country.Name,
                GameMode.Language => country.Name,
                _ => country.Name
            };
        }

        private LocalizedText GetQuestionLocalizedText(GameMode mode, Country country)
        {
            return mode switch
            {
                GameMode.Capital => new LocalizedText
                {
                    Ru = $"Столица страны {country.Name.Ru}?",
                    En = $"Capital of {country.Name.En}?"
                },
                GameMode.Flag => new LocalizedText
                {
                    Ru = "Флаг какой страны?",
                    En = "Which country's flag?"
                },
                GameMode.Outline => new LocalizedText
                {
                    Ru = "Контур какой страны?",
                    En = "Which country's outline?"
                },
                GameMode.Language => new LocalizedText
                {
                    Ru = $"Официальный язык {country.Name.Ru}?",
                    En = $"Official language of {country.Name.En}?"
                },
                _ => new LocalizedText
                {
                    Ru = "Вопрос",
                    En = "Question"
                }
            };
        }

        private string? GetImageUrl(GameMode mode, Country country)
        {
            return mode == GameMode.Flag ? country.FlagImage
                : mode == GameMode.Outline ? country.OutlineImage
                : null;
        }

        private string? GetAudioUrl(GameMode mode, Country country)
        {
            if (mode != GameMode.Language) return null;

            var languages = country.Languages;
            if (languages == null || languages.Count == 0) return null;

            return languages[Random.Shared.Next(languages.Count)].AudioUrl;
        }
    }
}
