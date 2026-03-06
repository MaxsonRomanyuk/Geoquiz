using GeoQuiz.Backend.Application.DTOs.PvP;

namespace GeoQuiz.Backend.Application.Interfaces;
public interface IPvPResultService
{
    Task<PvPMatchResultDto> FinalizeMatchAsync(Guid matchId, GameFinishReason reason, Guid userId);
}
