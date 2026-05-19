using GeoQuiz_backend.Application.Payloads.Koth;
using GeoQuiz_backend.Domain.Entities;
using GeoQuiz_backend.Domain.Enums;
using GeoQuiz_backend.Domain.Mongo;

namespace GeoQuiz_backend.Application.Interfaces
{
    public interface IQuestionSetService
    {
        Task<QuestionSet> CreateQuestionSetAsync(Guid matchId, GameType gameType, int count, int averageLevel);
        Task<List<GameQuestion>> GenerateQuestionsAsync(QuestionSet questionSet);
        //Task<List<GameQuestion>> GenerateLanguageQuestionsAsync(QuestionSet questionSet);
    }
}
