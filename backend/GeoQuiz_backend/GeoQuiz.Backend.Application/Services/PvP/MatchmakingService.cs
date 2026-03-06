using GeoQuiz.Backend.Application.Interfaces;
using GeoQuiz.Backend.Domain.Entities;
using GeoQuiz.Backend.Domain.Enums;
using GeoQuiz.Backend.Infrastructure.Data;

namespace GeoQuiz.Backend.Application.Services.PvP
{
    public class MatchmakingService : IMatchmakingService
    {
        private readonly MatchmakingQueue _queue;
        private readonly AppDbContext _db;
        private readonly IDraftService _draftService;

        public MatchmakingService(MatchmakingQueue queue, AppDbContext db, IDraftService draftService)
        {
            _queue = queue;
            _db = db;
            _draftService = draftService;
        }

        public async Task<PvPMatch?> JoinQueueAsync(Guid userId)
        {
            var opponentId = _queue.Enqueue(userId);

            if (opponentId == null)
                return null;

            var match = new PvPMatch
            {
                Id = Guid.NewGuid(),
                Player1Id = opponentId.Value,
                Player2Id = userId,
                Status = PvPMatchStatus.Drafting,
                CreatedAt = DateTime.UtcNow
            };

            _db.PvPMatches.Add(match);
            await _db.SaveChangesAsync();

            await _draftService.CreateDraftAsync(match);

            return match;
        }
        public Task LeaveQueueAsync(Guid userId)
        {
            _queue.Remove(userId); 
            return Task.CompletedTask;
        }

        public Task<bool> IsInQueueAsync(Guid userId)
        {
            return Task.FromResult(_queue.Contains(userId)); 
        }
    }
}
