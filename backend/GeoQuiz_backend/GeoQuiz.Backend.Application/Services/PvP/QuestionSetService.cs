using GeoQuiz.Backend.Application.Interfaces;
using GeoQuiz.Backend.Domain.Entities;
using GeoQuiz.Backend.Domain.Enums;
using GeoQuiz.Backend.Domain.Mongo;
using GeoQuiz.Backend.Infrastructure.Data;
using Microsoft.EntityFrameworkCore;

namespace GeoQuiz.Backend.Application.Services.PvP
{
    public class QuestionSetService : IQuestionSetService
    {
        private readonly AppDbContext _db;
        private readonly IQuestionRepository _questionRepo;

        public QuestionSetService(AppDbContext db, IQuestionRepository questionRepo)
        {
            _db = db;
            _questionRepo = questionRepo;
        }

        public async Task<QuestionSet> CreateForMatchAsync(Guid matchId, AppLanguage lang)
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

            var questionSet = new QuestionSet
            {
                Id = Guid.NewGuid(),
                PvPMatchId = matchId,
                Mode = match.SelectedMode.Value,
                Language = lang,
                Seed = seed,
                CreatedAt = DateTime.UtcNow,
                QuestionIds = selected.Select(q => q.Id).ToList()
            };

            _db.QuestionSets.Add(questionSet);
            match.Status = PvPMatchStatus.Ready;

            await _db.SaveChangesAsync();
            return questionSet;
        }

        public async Task<List<Question>> GetQuestionsAsync(Guid matchId)
        {
            var set = await _db.QuestionSets
                .FirstAsync(q => q.PvPMatchId == matchId);

            return await _questionRepo.GetByIdsAsync(set.QuestionIds);
        }
    }
}
