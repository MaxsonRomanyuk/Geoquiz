using GeoQuiz_backend.Application.DTOs.PvP;
using GeoQuiz_backend.Infrastructure.Persistence.MySQL;

namespace GeoQuiz_backend.Application.Interfaces
{
    public interface IPvPResultService
    {
        Task<PvPMatchResultDto> FinalizeMatchAsync(Guid matchId, GameFinishReason reason, Guid? userId = null);
    }
}
