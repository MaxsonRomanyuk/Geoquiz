using GeoQuiz.Backend.Domain.Entities;

namespace GeoQuiz.Backend.Application.Interfaces;
public interface IMatchmakingService
{
    Task<PvPMatch?> JoinQueueAsync(Guid userId);
    Task LeaveQueueAsync(Guid userId);
    Task<bool> IsInQueueAsync(Guid userId);
}
