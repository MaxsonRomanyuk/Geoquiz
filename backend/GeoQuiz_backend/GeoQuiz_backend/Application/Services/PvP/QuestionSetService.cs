using GeoQuiz_backend.Application.Interfaces;
using GeoQuiz_backend.Application.Payloads.Koth;
using GeoQuiz_backend.Application.Payloads.Questions;
using GeoQuiz_backend.Domain.Entities;
using GeoQuiz_backend.Domain.Enums;
using GeoQuiz_backend.Domain.Mongo;
using GeoQuiz_backend.Infrastructure.Persistence.MySQL;
using Microsoft.EntityFrameworkCore;
using System.Diagnostics.Metrics;

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
        public async Task<QuestionSet> CreateQuestionSetAsync(Guid matchId, GameType gameType, int count, int averageLevel)
        {
            object match;
            GameMode? selectedMode = null;

            if (gameType == GameType.PvP)
            {
                var pvpMatch = await _db.PvPMatches
                    .Include(m => m.QuestionSet)
                    .FirstAsync(m => m.Id == matchId);

                if (pvpMatch.QuestionSet != null)
                    return pvpMatch.QuestionSet;

                if (pvpMatch.SelectedMode == null)
                    throw new Exception("Game mode not selected yet");

                selectedMode = pvpMatch.SelectedMode;
                match = pvpMatch;
            }
            else 
            {
                var kothMatch = await _db.KothMatches
                    .Include(m => m.QuestionSet)
                    .FirstAsync(m => m.Id == matchId);

                if (kothMatch.QuestionSet != null)
                    return kothMatch.QuestionSet;

                //if (kothMatch.SelectedMode == null)
                //    throw new Exception("Game mode not selected yet");

                selectedMode = kothMatch.SelectedMode;
                match = kothMatch;
            }

            var seed = Random.Shared.Next();
            var rnd = new Random(seed);

            var allCountries = await _countryRepo.GetAllAsync();
            var selected = allCountries
                .OrderBy(q => rnd.Next())
                .Take(count)
                .ToList();

            var questionSet = new QuestionSet
            {
                Id = Guid.NewGuid(),
                Mode = selectedMode.Value,
                Seed = seed,
                Difficality = CalculateDifficulty(averageLevel),
                CreatedAt = DateTime.UtcNow,
                CountryIds = selected.Select(s => s.Id).ToList(),
                Regions = selected.Select(s => s.RegionEnum).ToList(),
            };
            if (gameType == GameType.PvP)
            {
                questionSet.PvPMatchId = matchId;
                ((PvPMatch)match).QuestionSet = questionSet;
            }
            else
            {
                questionSet.KothMatchId = matchId;
                ((KothMatch)match).QuestionSet = questionSet;
            }

            _db.QuestionSets.Add(questionSet);
            await _db.SaveChangesAsync();

            return questionSet;
        }
        public async Task<List<GameQuestion>> GenerateQuestionsAsync(QuestionSet questionSet)
        {
            if (questionSet == null) throw new Exception("Question set not created yet");
            if (questionSet.Mode == GameMode.Language)
            {
               return await GenerateLanguageQuestionsAsync(questionSet);
            }
            var questions = new List<GameQuestion>();
            var allCountries = await _countryRepo.GetAllAsync();

            var grouping = new CountryGrouping(allCountries);

            var selectedCountries = questionSet.CountryIds
                .Select(id => allCountries.First(c => c.Id == id))
                .ToList();

            var seed = questionSet.Seed;
            var mode = questionSet.Mode;
            var difficulty = questionSet.Difficality;
            
            foreach (var country in selectedCountries)
            {
                var optionRandom = new Random(seed + country.Id.GetHashCode());

                var options = new List<GameOption>();
                var correctOption = new GameOption
                {
                    Index = 0,
                    Text = GetLocalizedTextForCountry(country, mode)
                };

                var wrongCountries = GetWrongCountriesByDifficulty(
                    country,
                    grouping,
                    optionRandom,
                    difficulty,
                    3
                );
                //var wrongCountries = allCountries
                //    .Where(c => c.Id != country.Id)
                //    .OrderBy(x => optionRandom.Next())
                //    .Take(3)
                //    .ToList();

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
                    AudioUrl = null
                });
            }

            return questions;
        }
        private async Task<List<GameQuestion>> GenerateLanguageQuestionsAsync(QuestionSet questionSet)
        {
            var questions = new List<GameQuestion>();
            var allCountries = await _countryRepo.GetAllAsync();
            //var allLanguagePairs = allCountries
            //    .SelectMany(c => c.Languages.Select(l => new { Country = c, Language = l }))
            //    .ToList();
            var grouping = new CountryGrouping(allCountries);

            var seed = questionSet.Seed;
            var random = new Random(seed);
            var difficulty = questionSet.Difficality;

            var selectedCountries = questionSet.CountryIds
                .Select(id => allCountries.First(c => c.Id == id))
                .ToList();

            foreach (var country in selectedCountries)
            {
                if (country.Languages == null || !country.Languages.Any())
                    continue;

                var selectedLanguage = country.Languages[random.Next(country.Languages.Count)];

                var questionType = random.Next(2) == 0
                    ? LanguageQuestionType.LanguageByCountry
                    : LanguageQuestionType.CountryByLanguage;

                var optionRandom = new Random(seed + country.Id.GetHashCode() + selectedLanguage.Id.GetHashCode());
                var options = new List<GameOption>();

                if (questionType == LanguageQuestionType.LanguageByCountry)
                {
                    options.Add(new GameOption
                    {
                        Index = 0,
                        Text = selectedLanguage.Name
                    });

                    var otherLanguages = GetWrongLanguagesByDifficultyForCountry(
                        country,
                        selectedLanguage,
                        grouping,
                        optionRandom,
                        difficulty,
                        3
                    );

                    //var otherLanguages = allLanguagePairs
                    //    .Select(lp => lp.Language)
                    //    .Where(l => l.Id != selectedLanguage.Id)
                    //    .Where(l => !country.Languages.Any(cl => cl.Id == l.Id))
                    //    .DistinctBy(l => l.Id)
                    //    .OrderBy(x => optionRandom.Next())
                    //    .Take(3)
                    //    .ToList();

                    foreach (var wrongLanguage in otherLanguages)
                    {
                        options.Add(new GameOption
                        {
                            Index = options.Count,
                            Text = wrongLanguage.Name
                        });
                    }

                    var shuffledOptions = options.OrderBy(x => optionRandom.Next()).ToList();
                    var correctIndex = shuffledOptions.FindIndex(o => o.Text.En == selectedLanguage.Name.En);

                    questions.Add(new GameQuestion
                    {
                        CountryId = country.Id,
                        QuestionText = new LocalizedText
                        {
                            Ru = $"Какой язык является официальным в {country.Name.Ru}?",
                            En = $"Which language is official in {country.Name.En}?"
                        },
                        Options = shuffledOptions,
                        CorrectOptionIndex = correctIndex,
                        ImageUrl = null,
                        AudioUrl = null
                    });
                }
                else
                {
                    options.Add(new GameOption
                    {
                        Index = 0,
                        Text = country.Name
                    });

                    var otherCountries = GetWrongCountriesByDifficultyForLanguage(
                        country,
                        selectedLanguage,
                        grouping,
                        optionRandom,
                        difficulty,
                        3
                    );

                    //var otherCountries = allCountries
                    //    .Where(c => c.Id != country.Id)
                    //    .Where(c => !c.Languages.Any(l => l.Id == selectedLanguage.Id))
                    //    .OrderBy(x => optionRandom.Next())
                    //    .Take(3)
                    //    .ToList();

                    foreach (var wrongCountry in otherCountries)
                    {
                        options.Add(new GameOption
                        {
                            Index = options.Count,
                            Text = wrongCountry.Name
                        });
                    }

                    var shuffledOptions = options.OrderBy(x => optionRandom.Next()).ToList();
                    var correctIndex = shuffledOptions.FindIndex(o => o.Text.En == country.Name.En);

                    questions.Add(new GameQuestion
                    {
                        CountryId = country.Id,
                        QuestionText = new LocalizedText
                        {
                            Ru = $"В какой стране говорят на {selectedLanguage.Name.Ru}?",
                            En = $"In which country do they speak {selectedLanguage.Name.En}?"
                        },
                        Options = shuffledOptions,
                        CorrectOptionIndex = correctIndex,
                        ImageUrl = null,
                        AudioUrl = selectedLanguage.AudioUrl
                    });
                }
            }

            return questions;
        }
      
        private List<Country> GetWrongCountriesByDifficulty(Country correctCountry,CountryGrouping grouping,Random random,int difficulty,int count = 3)
        {
            var wrongCountries = new List<Country>();
            switch (difficulty)
            {
                case 1: 
                    var otherRegions = grouping.Regions
                        .Where(r => r != correctCountry.Region)
                        .ToList();

                    var shuffledRegions = otherRegions.OrderBy(x => random.Next()).ToList();

                    foreach (var region in shuffledRegions)
                    {
                        var candidates = grouping.CountriesByRegion[region]
                            .Where(c => c.Id != correctCountry.Id)
                            .ToList();

                        if (candidates.Any())
                        {
                            var selected = candidates[random.Next(candidates.Count)];
                            wrongCountries.Add(selected);
                        }

                        if (wrongCountries.Count >= count) break;
                    }

                    break;

                case 2: 
                    wrongCountries = grouping.AllCountries
                        .Where(c => c.Id != correctCountry.Id)
                        .OrderBy(x => random.Next())
                        .Take(count)
                        .ToList();
                    break;

                case 3: 
                    var sameRegionCountries = grouping.CountriesByRegion[correctCountry.Region]
                        .Where(c => c.Id != correctCountry.Id)
                        .ToList();

                    wrongCountries = sameRegionCountries
                        .OrderBy(x => random.Next())
                        .Take(count)
                        .ToList();
                    
                    break;
            }

            return wrongCountries;
        }
        private List<Country> GetWrongCountriesByDifficultyForLanguage(Country correctCountry, CountryLanguage selectedLanguage, CountryGrouping grouping,
            Random random, int difficulty, int count = 3)
        {
            var wrongCountries = new List<Country>();

            switch (difficulty)
            {
                case 1: 
                    var otherRegions = grouping.Regions
                        .Where(r => r != correctCountry.Region)
                        .ToList();

                    var shuffledRegions = otherRegions.OrderBy(x => random.Next()).ToList();

                    foreach (var region in shuffledRegions)
                    {
                        var candidates = grouping.CountriesByRegion[region]
                            .Where(c => c.Id != correctCountry.Id)
                            .Where(c => c.Languages == null ||
                                   !c.Languages.Any(l => l.Id == selectedLanguage.Id))
                            .ToList();

                        if (candidates.Any())
                        {
                            var selected = candidates[random.Next(candidates.Count)];
                            wrongCountries.Add(selected);
                        }

                        if (wrongCountries.Count >= count) break;
                    }

                    break;

                case 2: 
                    wrongCountries = grouping.AllCountries
                        .Where(c => c.Id != correctCountry.Id)
                        .Where(c => c.Languages == null ||
                                   !c.Languages.Any(l => l.Id == selectedLanguage.Id))
                        .OrderBy(x => random.Next())
                        .Take(count)
                        .ToList();
                    break;

                case 3:
                    var sameRegionCandidates = grouping.CountriesByRegion[correctCountry.Region]
                        .Where(c => c.Id != correctCountry.Id)
                        .Where(c => c.Languages == null ||
                                   !c.Languages.Any(l => l.Id == selectedLanguage.Id))
                        .ToList();

                    if (sameRegionCandidates.Count >= count)
                    {
                        wrongCountries = sameRegionCandidates
                            .OrderBy(x => random.Next())
                            .Take(count)
                            .ToList();
                    }
                    else
                    {
                        wrongCountries.AddRange(sameRegionCandidates);

                        var additional = grouping.AllCountries
                            .Where(c => c.Id != correctCountry.Id &&
                                        !wrongCountries.Contains(c) &&
                                        (c.Languages == null ||
                                         !c.Languages.Any(l => l.Id == selectedLanguage.Id)))
                            .OrderBy(x => random.Next())
                            .Take(count - wrongCountries.Count)
                            .ToList();
                        wrongCountries.AddRange(additional);
                    }
                    break;
            }

            return wrongCountries;
        }
        private List<CountryLanguage> GetWrongLanguagesByDifficultyForCountry(Country correctCountry, CountryLanguage selectedLanguage, CountryGrouping grouping,
            Random random, int difficulty, int count = 3)
        {
            var wrongLanguages = new List<CountryLanguage>();

            var correctCountryLanguageIds = correctCountry.Languages
                .Select(l => l.Id)
                .ToHashSet();

            switch (difficulty)
            {
                case 1: 
                    var languagesFromOtherRegions = grouping.Regions
                        .Where(r => r != correctCountry.Region)
                        .SelectMany(r => grouping.CountriesByRegion[r])
                        .SelectMany(c => c.Languages ?? new List<CountryLanguage>())
                        .Where(l => l.Id != selectedLanguage.Id)
                        .Where(l => !correctCountryLanguageIds.Contains(l.Id)) 
                        .DistinctBy(l => l.Id)
                        .ToList();

                    wrongLanguages = languagesFromOtherRegions
                        .OrderBy(x => random.Next())
                        .Take(count)
                        .ToList();
                    break;

                case 2: 
                    var allOtherLanguages = grouping.AllCountries
                        .SelectMany(c => c.Languages ?? new List<CountryLanguage>())
                        .Where(l => l.Id != selectedLanguage.Id)
                        .Where(l => !correctCountryLanguageIds.Contains(l.Id)) 
                        .DistinctBy(l => l.Id)
                        .ToList();

                    wrongLanguages = allOtherLanguages
                        .OrderBy(x => random.Next())
                        .Take(count)
                        .ToList();
                    break;

                case 3: 
                    var languagesFromSameRegion = grouping.CountriesByRegion[correctCountry.Region]
                        .Where(c => c.Id != correctCountry.Id)
                        .SelectMany(c => c.Languages ?? new List<CountryLanguage>())
                        .Where(l => l.Id != selectedLanguage.Id)
                        .Where(l => !correctCountryLanguageIds.Contains(l.Id))
                        .DistinctBy(l => l.Id)
                        .ToList();

                    if (languagesFromSameRegion.Count >= count)
                    {
                        wrongLanguages = languagesFromSameRegion
                            .OrderBy(x => random.Next())
                            .Take(count)
                            .ToList();
                    }
                    else
                    {
                        wrongLanguages.AddRange(languagesFromSameRegion);

                        var additional = grouping.AllCountries
                            .SelectMany(c => c.Languages ?? new List<CountryLanguage>())
                            .Where(l => l.Id != selectedLanguage.Id)
                            .Where(l => !correctCountryLanguageIds.Contains(l.Id))
                            .Where(l => !wrongLanguages.Contains(l))
                            .DistinctBy(l => l.Id)
                            .OrderBy(x => random.Next())
                            .Take(count - wrongLanguages.Count)
                            .ToList();
                        wrongLanguages.AddRange(additional);
                    }
                    break;
            }

            return wrongLanguages;
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
                    Ru = "Какой стране принадлежит этот флаг?",
                    En = "Which country does this flag belong to?"
                },
                GameMode.Outline => new LocalizedText
                {
                    Ru = "Какой стране принадлежит этот контур?",
                    En = "Which country does this outline belong to?"
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
        private int CalculateDifficulty(int averageLevel)
        {
            if (averageLevel <= 5) return 1;
            else if (averageLevel > 5 && averageLevel <= 10) return 2;
            else return 3;
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
