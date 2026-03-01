using GeoQuiz_backend.DTOs.PvP;

namespace GeoQuiz_backend.Application.Interfaces
{
    public interface IPvPResultService
    {
        Task<PvPMatchResultDto> FinalizeMatchAsync(Guid matchId, GameFinishReason reason, Guid userId);
    }
}
