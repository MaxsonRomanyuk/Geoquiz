using GeoQuiz_backend.Domain.Entities;
using GeoQuiz_backend.Domain.Enums;
using GeoQuiz_backend.Domain.Mongo;

namespace GeoQuiz_backend.Application.Interfaces
{
    public interface IQuestionSetService
    {
        Task<QuestionSet> CreateForMatchAsync(Guid matchId, AppLanguage lang);
        Task<List<Question>> GetQuestionsAsync(Guid matchId);
    }
}
