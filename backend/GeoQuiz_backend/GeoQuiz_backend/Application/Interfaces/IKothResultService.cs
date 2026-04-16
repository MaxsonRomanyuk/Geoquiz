using GeoQuiz_backend.Application.DTOs.KingOfTheHill;
using GeoQuiz_backend.Application.Payloads.Koth;

namespace GeoQuiz_backend.Application.Interfaces
{
    public interface IKothResultService
    {
        Task FinalizeMatchAsync(KothGameState gameState);
    }
}
