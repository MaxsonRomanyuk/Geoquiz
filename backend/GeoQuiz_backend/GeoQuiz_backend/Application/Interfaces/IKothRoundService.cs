using GeoQuiz_backend.Application.DTOs.KingOfTheHill;
using GeoQuiz_backend.Application.Payloads.Koth;
using GeoQuiz_backend.Domain.Entities;

namespace GeoQuiz_backend.Application.Interfaces
{
    public interface IKothRoundService
    {
        RoundStartedData? StartRound(KothGameState gameState);
        AnswerResultData ProcessAnswer(KothGameState gameState, Guid userId, SubmitAnswerRequest request);
        void ProcessBotAnswers(KothGameState gameState, Guid matchId);
        List<Guid> FinishRound(KothGameState gameState);
        void AssignPlaces(KothGameState gameState, List<Guid> eliminated);
        bool IsRoundComplete(KothGameState gameState);
    }
}
