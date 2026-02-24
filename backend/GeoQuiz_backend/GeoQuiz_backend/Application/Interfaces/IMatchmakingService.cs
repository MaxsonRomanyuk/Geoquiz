using GeoQuiz_backend.Domain.Entities;

namespace GeoQuiz_backend.Application.Interfaces
{
    public interface IMatchmakingService
    {
        Task<PvPMatch?> JoinQueueAsync(Guid userId);
        Task LeaveQueueAsync(Guid userId);
        Task<bool> IsInQueueAsync(Guid userId);
    }
}
