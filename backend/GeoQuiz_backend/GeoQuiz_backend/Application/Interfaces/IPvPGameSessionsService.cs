using GeoQuiz_backend.Application.DTOs.PvP;

namespace GeoQuiz_backend.Application.Interfaces
{
    public interface IPvPGameSessionService
    {
        Task StartMatchAsync(Guid matchId);
        Task<PvPGameStateDto> GetGameStateAsync(Guid matchId, Guid userId);
        Task<SubmitAnswerResponse> SubmitAnswerAsync(Guid matchId, Guid userId, SubmitAnswerRequest dto);
        Task MonitorGameTimeAsync(Guid matchId);
    }
}
