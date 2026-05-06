using GeoQuiz_backend.Application.Payloads.Koth;
using GeoQuiz_backend.Domain.Entities;
using GeoQuiz_backend.Domain.Enums;
using GeoQuiz_backend.Domain.Mongo;

namespace GeoQuiz_backend.Application.Interfaces
{
    public interface IQuestionSetService
    {
        Task<QuestionSet> CreateForMatchAsync(Guid matchId);
        Task<List<GameQuestion>> GenerateQuestionsAsync(int count, int seed, GameMode mode);
    }
}
