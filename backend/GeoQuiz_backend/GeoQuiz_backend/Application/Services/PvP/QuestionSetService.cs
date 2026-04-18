using GeoQuiz_backend.Application.Interfaces;
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
        private readonly IQuestionRepository _questionRepo;
        private readonly ICountryRepository _countryRepo;

        public QuestionSetService(AppDbContext db, IQuestionRepository questionRepo, ICountryRepository countryRepo)
        {
            _db = db;
            _questionRepo = questionRepo;
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

            var allQuestions = await _questionRepo.GetByTypeAsync(match.SelectedMode.Value);
            var selected = allQuestions
                .OrderBy(q => rnd.Next())
                .Take(10)
                .ToList();

            var countriesIds = selected.Select(q => RemoveLastUnderscorePart(q.Id)).ToList();
            var countries = await _countryRepo.GetByIdsAsync(countriesIds);

            var questionSet = new QuestionSet
            {
                Id = Guid.NewGuid(),
                PvPMatchId = matchId,
                Mode = match.SelectedMode.Value,
                Language = AppLanguage.En,
                Seed = seed,
                CreatedAt = DateTime.UtcNow,
                QuestionIds = selected.Select(q => q.Id).ToList(),
                Regions = countries.Select(c => c.RegionEnum).ToList(),
            };

            match.QuestionSet = questionSet;
            _db.QuestionSets.Add(questionSet);

            await _db.SaveChangesAsync();
            return questionSet;
        }

        public async Task<List<Question>> GetQuestionsAsync(Guid matchId)
        {
            var set = await _db.QuestionSets
                .FirstAsync(q => q.PvPMatchId == matchId);

            return await _questionRepo.GetByIdsAsync(set.QuestionIds);
        }
        public static string RemoveLastUnderscorePart(string input)
        {
            if (string.IsNullOrEmpty(input)) return input;

            for (int i = input.Length - 1; i >= 0; i--)
            {
                if (input[i] == '_')
                {
                    return input.Substring(0, i);
                }
            }
            return input;
        }
    }
}
