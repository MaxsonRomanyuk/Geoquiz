using GeoQuiz.Backend.Domain.Entities;
using GeoQuiz.Backend.Domain.Enums;
using GeoQuiz.Backend.Domain.Mongo;

namespace GeoQuiz.Backend.Application.Interfaces;
public interface IQuestionSetService
{
    Task<QuestionSet> CreateForMatchAsync(Guid matchId, AppLanguage lang);
    Task<List<Question>> GetQuestionsAsync(Guid matchId);
}
