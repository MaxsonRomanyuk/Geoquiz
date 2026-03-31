using GeoQuiz_backend.API.Hubs;
using GeoQuiz_backend.Application.DTOs.PvP;
using GeoQuiz_backend.Application.Interfaces;
using GeoQuiz_backend.Application.Services.KingOfTheHill;
using GeoQuiz_backend.Domain.Entities;
using GeoQuiz_backend.Domain.Enums;
using GeoQuiz_backend.Infrastructure.Persistence.MySQL;
using Microsoft.EntityFrameworkCore;
using System.Collections.Concurrent;

namespace GeoQuiz_backend.Application.Services.PvP
{
    public class DraftService : IDraftService
    {
        private readonly AppDbContext _db;
        private readonly ILogger<DraftService> _logger;
        private readonly IServiceScopeFactory _scopeFactory;
        private readonly ISignalRNotificationService _notificationService;
        private readonly IPvPGameSessionService _pvpGameSessionService;

        private static readonly ConcurrentDictionary<Guid, CancellationTokenSource> _draftTimers = new();
        private static readonly ConcurrentDictionary<Guid, SemaphoreSlim> _matchLocks = new();

        public DraftService(
            AppDbContext db,
            ILogger<DraftService> logger,
            ISignalRNotificationService notificationService,
            IServiceScopeFactory scopeFactory,
            IPvPGameSessionService pvpGameSessionService)
            
        {
            _db = db;
            _logger = logger;
            _notificationService = notificationService;
            _scopeFactory = scopeFactory;
            _pvpGameSessionService = pvpGameSessionService;
        }

        public async Task<ModeDraft> CreateDraftAsync(PvPMatch match)
        {
            var allModes = Enum.GetValues<GameMode>().ToList();

            var firstTurn = Random.Shared.Next(0, 2) == 0
                ? match.Player1Id
                : match.Player2Id;

            var draft = new ModeDraft
            {
                Id = Guid.NewGuid(),
                PvPMatchId = match.Id,
                AvailableModes = allModes,
                BannedModes = new List<GameMode>(),
                Step = 0,
                CurrentTurnUserId = firstTurn
            };

            match.Status = PvPMatchStatus.Drafting;
            match.Draft = draft;

            _db.ModeDrafts.Add(draft);
            await _db.SaveChangesAsync();

            return draft;
        }

        public async Task<ModeDraft> GetDraftAsync(Guid matchId)
        {
            return await _db.ModeDrafts
                .Include(d => d.PvPMatch)
                .FirstAsync(d => d.PvPMatchId == matchId);
        }

        public async Task<ModeDraft> BanModeAsync(Guid matchId, Guid userId, GameMode bannedMode, int expectedStep)
        {
            var semaphore = _matchLocks.GetOrAdd(matchId, new SemaphoreSlim(1, 1));
            await semaphore.WaitAsync();

            try
            {
                CancelDraftTimer(matchId);
                var draft = await _db.ModeDrafts
                    .Include(d => d.PvPMatch)
                    .FirstAsync(d => d.PvPMatchId == matchId);

                if (draft.Step != expectedStep)
                {
                    _logger.LogWarning("Outdated ban request ignored. Match {MatchId}", matchId);
                    return draft;
                }

                if (draft.PvPMatch.Status != PvPMatchStatus.Drafting)
                    throw new Exception("Draft is not active");

                if (draft.CurrentTurnUserId != userId)
                    throw new Exception("Not your turn");

                if (!draft.AvailableModes.Contains(bannedMode))
                    throw new Exception("Mode is already banned or invalid");

                draft.AvailableModes = draft.AvailableModes
                    .Where(m => m != bannedMode)
                    .ToList();
                draft.BannedModes = draft.BannedModes
                    .Append(bannedMode)
                    .ToList();
                draft.Step++;

                if (draft.AvailableModes.Count == 1)
                {
                    var finalMode = draft.AvailableModes.First();
                    draft.PvPMatch.SelectedMode = finalMode;
                    draft.PvPMatch.Status = PvPMatchStatus.Ready;
                }
                else
                {
                    draft.CurrentTurnUserId =
                        draft.CurrentTurnUserId == draft.PvPMatch.Player1Id
                        ? draft.PvPMatch.Player2Id
                        : draft.PvPMatch.Player1Id;
                }
                await _db.SaveChangesAsync();

                var updateData = new DraftUpdateData
                {
                    MatchId = matchId,
                    BannedMode = bannedMode,
                    BannedByUserId = userId,
                    RemainingModes = draft.AvailableModes,
                    NextTurnUserId = draft.CurrentTurnUserId,
                    Step = draft.Step,
                    IsDraftCompleted = draft.PvPMatch.Status == PvPMatchStatus.Ready
                };
                await _notificationService.NotifyDraftUpdated(matchId, updateData);

                if (draft.PvPMatch.Status == PvPMatchStatus.Drafting)
                {
                    StartDraftTimer(matchId, draft.Step);
                }
                else if (draft.PvPMatch.Status == PvPMatchStatus.Ready)
                {
                    _logger.LogInformation("Draft completed for match {MatchId}, selected mode: {Mode}", matchId, draft.PvPMatch.SelectedMode);

                    await _pvpGameSessionService.StartMatchAsync(matchId);
                }

                return draft;
            }
            finally
            {
                semaphore.Release();
            }
        }
        public void StartDraftTimer(Guid matchId, int step)
        {
            CancelDraftTimer(matchId);

            var cts = new CancellationTokenSource();
            _draftTimers[matchId] = cts;

            Task.Run(async () =>
            {
                try
                {
                    await Task.Delay(TimeSpan.FromSeconds(10), cts.Token);

                    if (cts.Token.IsCancellationRequested)
                    {
                        _logger.LogInformation("Timer cancelled after data load for match {MatchId}", matchId);
                        return;
                    }
                    using var scope = _scopeFactory.CreateScope();
                    var db = scope.ServiceProvider.GetRequiredService<AppDbContext>();
                    var draftService = scope.ServiceProvider.GetRequiredService<IDraftService>();

                    var draft = await db.ModeDrafts
                        .Include(d => d.PvPMatch)
                        .FirstOrDefaultAsync(d => d.PvPMatchId == matchId, cts.Token);

                    if (draft == null)
                    {
                        _logger.LogWarning("Draft not found for match {MatchId}", matchId);
                        return;
                    }
                    if (draft.Step != step)
                    {
                        _logger.LogInformation("Timer is outdated for match {MatchId}", matchId);
                        return;
                    }

                    if (draft.PvPMatch.Status == PvPMatchStatus.Drafting && draft.AvailableModes.Count > 1)
                    {
                        var currentUser = draft.CurrentTurnUserId;
                        var modeToBan = draft.AvailableModes.First();
                        await draftService.BanModeAsync(matchId, currentUser, modeToBan, step);
                    }
                    
                }
                catch (OperationCanceledException)
                {
                    _logger.LogInformation("Draft timer cancelled for match {MatchId}", matchId);
                }
                catch (Exception ex)
                {
                    _logger.LogError(ex, "Error in draft timer for match {MatchId}", matchId);
                }
                finally
                {
                    _draftTimers.TryRemove(matchId, out _);
                }
            }, cts.Token);
        }
        public static void CancelDraftTimer(Guid matchId)
        {
            if (_draftTimers.TryRemove(matchId, out var cts))
            {
                cts.Cancel();
                cts.Dispose();
            }
        }
    }
}
