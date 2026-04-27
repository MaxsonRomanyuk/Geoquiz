using GeoQuiz_backend.Application.DTOs.PvP;
using GeoQuiz_backend.Domain.Entities;
using GeoQuiz_backend.Domain.Enums;

namespace GeoQuiz_backend.Application.Interfaces
{
    public interface IDraftService
    {
        Task<ModeDraft> CreateDraftAsync(PvPMatch match);
        Task<ModeDraft> GetDraftAsync(Guid matchId);
        Task<ModeDraft> BanModeAsync(Guid matchId, Guid userId, GameMode bannedMode, int expectedStep);
        void StartDraftTimer(Guid matchId, int step);
        DateTime? GetDraftTimerEndsAt(Guid matchId);
    }
}
