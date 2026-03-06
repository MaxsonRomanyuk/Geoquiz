using GeoQuiz.Backend.Domain.Entities;
using GeoQuiz.Backend.Domain.Enums;

namespace GeoQuiz.Backend.Application.Interfaces;
public interface IDraftService
{
    Task<ModeDraft> CreateDraftAsync(PvPMatch match);
    Task<ModeDraft> GetDraftAsync(Guid matchId);
    Task<ModeDraft> BanModeAsync(Guid matchId, Guid userId, GameMode bannedMode);
}
