using GeoQuiz_backend.Application.DTOs.KingOfTheHill;
using GeoQuiz_backend.Application.Interfaces;
using GeoQuiz_backend.Domain.Entities;
using GeoQuiz_backend.Infrastructure.Persistence.MySQL;
using Microsoft.EntityFrameworkCore;
using System.Collections.Generic;

namespace GeoQuiz_backend.Application.Services.KingOfTheHill
{
    public class KothMatchmakingService : IKothMatchmakingService
    {
        private readonly AppDbContext _db;
        private readonly ISignalRNotificationService _notificationService;
        private readonly IKothGameService _gameService;
        private readonly IServiceScopeFactory _serviceScopeFactory;
        private readonly ILogger<KothMatchmakingService> _logger;

        private static readonly Dictionary<Guid, KothLobby> _activeLobbies = new();
        private static readonly object _lobbyLock = new();
        private static readonly Dictionary<Guid, CancellationTokenSource> _lobbyTimers = new();

        public KothMatchmakingService(
            AppDbContext db,
            ISignalRNotificationService notificationService,
            IKothGameService gameService,
            IServiceScopeFactory serviceScopeFactory,
            ILogger<KothMatchmakingService> logger)
        {
            _db = db;
            _notificationService = notificationService;
            _gameService = gameService;
            _serviceScopeFactory = serviceScopeFactory;
            _logger = logger;
        }

        public async Task<(KothLobby? Lobby, string UserName, int UserLevel)> JoinLobbyAsync(Guid userId)
        {
            KothLobby lobby;
            string userName;
            int userLevel;

            var user = await _db.Users
                .Include(u => u.Stats)
                .FirstOrDefaultAsync(u => u.Id == userId);

            if (user == null)
            {
                _logger.LogError("User {UserId} not found", userId);
                return (null, string.Empty, 0); 
            }

            userName = user.UserName;
            userLevel = user.Stats?.Level ?? 1;

            lock (_lobbyLock)
            {
                var existingLobby = _activeLobbies.Values.FirstOrDefault(l => l.PlayersLobby.Any(p => p.Id == userId));

                if (existingLobby != null)
                {
                    _logger.LogWarning("User {UserId} already in lobby {LobbyId}", userId, existingLobby.Id);
                    return (existingLobby, userName, userLevel);
                }

                lobby = _activeLobbies.Values.FirstOrDefault(l => l.PlayersLobby.Count < 32)!;

                if (lobby == null)
                {
                    lobby = new KothLobby
                    {
                        Id = Guid.NewGuid(),
                        PlayersLobby = new List<PlayerLobby> { new PlayerLobby { Id = userId, Name = userName, Level = userLevel } },
                        CreatedAt = DateTime.UtcNow
                    };
                    _activeLobbies[lobby.Id] = lobby;

                    _logger.LogInformation("Created new lobby {LobbyId} with first player {UserId}", lobby.Id, userId);
                }
                else
                {
                    lobby.PlayersLobby.Add(new PlayerLobby
                    {
                        Id = userId,
                        Name = userName,
                        Level = userLevel
                    });
                    _logger.LogInformation("User {UserId} joined lobby {LobbyId}, now {Count}/32", userId, lobby.Id, lobby.PlayersLobby.Count);
                }
            }

            await CheckStartConditionsAsync(lobby.Id);


            return (lobby, userName, userLevel);
        }
        private async Task CheckStartConditionsAsync(Guid lobbyId)
        {
            try
            {
                bool shouldStartTimer = false;
                bool shouldStartImmediately = false;
                List<PlayerLobby> playersToStart = null;

                lock (_lobbyLock)
                {
                    if (!_activeLobbies.TryGetValue(lobbyId, out var lobby))
                        return;

                    if (lobby.PlayersLobby.Count >= 32)
                    {
                        _logger.LogInformation("Lobby {LobbyId} full (32 players), starting immediately", lobbyId);
                        shouldStartImmediately = true;
                        playersToStart = lobby.PlayersLobby;
                        _activeLobbies.Remove(lobbyId);

                        if (_lobbyTimers.TryGetValue(lobbyId, out var cts))
                        {
                            cts.Cancel();
                            _lobbyTimers.Remove(lobbyId);
                        }
                    }
                    else if (lobby.PlayersLobby.Count >= 2 && !_lobbyTimers.ContainsKey(lobbyId))
                    {
                        shouldStartTimer = true;
                    }
                }

                if (shouldStartImmediately && playersToStart != null)
                {
                    _ = Task.Run(async () =>
                    {
                        using (var scope = _serviceScopeFactory.CreateScope())
                        {
                            var gameService = scope.ServiceProvider.GetRequiredService<IKothGameService>();
                            var playersInfo = playersToStart.Select(p => PlayerInfo.FromPlayerLobby(p)).ToList();
                            await gameService.StartMatchFromLobbyAsync(playersInfo, lobbyId);
                        }
                    });
                }
                else if (shouldStartTimer)
                {
                    _ = Task.Run(async () => await StartLobbyTimerAsync(lobbyId));
                }
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error in CheckStartConditions for lobby {LobbyId}", lobbyId);
            }
        }
        public async Task LeaveLobbyAsync(Guid userId)
        {
            Guid? lobbyId = null;
            bool shouldCancelTimer = false;
            int remainingPlayers = 0;

            lock (_lobbyLock)
            {
                var lobby = _activeLobbies.Values
                    .FirstOrDefault(l => l.PlayersLobby.Any(p => p.Id == userId));

                if (lobby == null)
                    return;

                lobbyId = lobby.Id;

                var playerLobby = lobby.PlayersLobby.FirstOrDefault(p => p.Id == userId);
                lobby.PlayersLobby.Remove(playerLobby);
                remainingPlayers = lobby.PlayersLobby.Count;

                _logger.LogInformation("User {UserId} left lobby {LobbyId}, remaining {Count}", userId, lobby.Id, remainingPlayers);

                if (remainingPlayers == 0)
                {
                    _activeLobbies.Remove(lobby.Id);

                    if (_lobbyTimers.TryGetValue(lobby.Id, out var cts))
                    {
                        cts.Cancel();
                        _lobbyTimers.Remove(lobby.Id);
                    }

                    _logger.LogInformation("Lobby {LobbyId} removed (empty)", lobby.Id);
                    return;
                }

                if (remainingPlayers < 4 && _lobbyTimers.ContainsKey(lobby.Id))
                {
                    shouldCancelTimer = true;
                }
            }

            if (lobbyId.HasValue)
            {
                await _notificationService.NotifyPlayerLeft(lobbyId.Value, new PlayerLeftData
                {
                    LobbyId = lobbyId.Value,
                    PlayerId = userId,
                    TotalPlayers = remainingPlayers
                });
            }

            if (shouldCancelTimer && lobbyId.HasValue)
            {
                _ = Task.Run(async () => await CancelLobbyTimerAsync(lobbyId.Value));
            }
        }

        private async Task StartLobbyTimerAsync(Guid lobbyId)
        {
            const int COUNTDOWN_SECONDS = 15;

            var cts = new CancellationTokenSource();

            lock (_lobbyLock)
            {
                _lobbyTimers[lobbyId] = cts;
            }

            try
            {
                for (int i = COUNTDOWN_SECONDS; i > 0; i--)
                {
                    if (cts.Token.IsCancellationRequested) return;

                    using (var scope = _serviceScopeFactory.CreateScope())
                    {
                        var notificationService = scope.ServiceProvider.GetRequiredService<ISignalRNotificationService>();
                        await notificationService.NotifyLobbyCountdown(lobbyId, i);
                    }

                    await Task.Delay(1000, cts.Token);

                    bool stillValid;
                    List<PlayerLobby> playersToStart = null;

                    lock (_lobbyLock)
                    {
                        if (!_activeLobbies.TryGetValue(lobbyId, out var lobby))
                            return;

                        stillValid = lobby.PlayersLobby.Count >= 2;

                        if (i == 1 && stillValid)
                        {
                            playersToStart = lobby.PlayersLobby;
                            _activeLobbies.Remove(lobbyId);
                        }
                    }

                    if (!stillValid)
                    {
                        _logger.LogInformation("Lobby {LobbyId} countdown cancelled - less than 4 players", lobbyId);
                        await CancelLobbyTimerAsync(lobbyId);
                        return;
                    }

                    if (i == 1 && playersToStart != null)
                    {
                        _logger.LogInformation("Lobby {LobbyId} countdown finished, starting game", lobbyId);

                        lock (_lobbyLock)
                        {
                            _lobbyTimers.Remove(lobbyId);
                        }

                        using (var scope = _serviceScopeFactory.CreateScope())
                        {
                            var gameService = scope.ServiceProvider.GetRequiredService<IKothGameService>();
                            var playersInfo = playersToStart.Select(p => PlayerInfo.FromPlayerLobby(p)).ToList();
                            await gameService.StartMatchFromLobbyAsync(playersInfo, lobbyId);
                        }
                        return;
                    }
                }
            }
            catch (TaskCanceledException)
            {
                _logger.LogDebug("Lobby {LobbyId} timer cancelled", lobbyId);
            }
        }

        private async Task CancelLobbyTimerAsync(Guid lobbyId)
        {
            CancellationTokenSource? cts = null;

            lock (_lobbyLock)
            {
                if (_lobbyTimers.TryGetValue(lobbyId, out cts))
                {
                    _lobbyTimers.Remove(lobbyId);
                }
            }

            if (cts != null)
            {
                cts.Cancel();
                using (var scope = _serviceScopeFactory.CreateScope())
                {
                    var notificationService = scope.ServiceProvider.GetRequiredService<ISignalRNotificationService>();
                    await notificationService.NotifyLobbyCountdownCancelled(lobbyId);
                }
            }
        }

        public Task<bool> IsInLobbyAsync(Guid userId)
        {
            lock (_lobbyLock)
            {
                var inLobby = _activeLobbies.Values.Any(l => l.PlayersLobby.Any(p => p.Id == userId));
                return Task.FromResult(inLobby);
            }
        }
        
    }
}
