using GeoQuiz_backend.Application.Hubs;
using GeoQuiz_backend.Application.Interfaces;
using GeoQuiz_backend.DTOs.KingOfTheHill;
using GeoQuiz_backend.DTOs.PvP;
using Microsoft.AspNetCore.SignalR;

namespace GeoQuiz_backend.Application.Services.PvP
{
    public class SignalRNotificationService : ISignalRNotificationService
    {
        private readonly IHubContext<PvPHub, IPvPHubClient> _pvpHub;
        private readonly IHubContext<KothHub, IKothHubClient> _kothHub;

        public SignalRNotificationService(IHubContext<PvPHub, IPvPHubClient> hubContext)
        {
            _pvpHub = hubContext;
        }

        public async Task NotifyMatchFound(Guid userId, MatchFoundWithDraftData matchData)
        {
            await _pvpHub.Clients.User(userId.ToString()).MatchFound(matchData);
        }
        public async Task NotifyDraftUpdated(Guid matchId, DraftUpdateData updateData)
        {
            await _pvpHub.Clients.Group($"match_{matchId}").DraftUpdated(updateData);
        }
        public async Task NotifyGameReady(Guid matchId, GameReadyData gameData)
        {
            await _pvpHub.Clients.Group($"match_{matchId}").GameReady(gameData);
        }

        public async Task NotifyQuestionResult(Guid userId, QuestionResultData resultData)
        {
            await _pvpHub.Clients.User(userId.ToString()).QuestionResult(resultData);
        }

        public async Task NotifyTimerUpdate(Guid matchId, TimerUpdateData timerData)
        {
            await _pvpHub.Clients.Group($"match_{matchId}").TimerUpdate(timerData);
        }

        public async Task NotifyGameFinished(Guid userId, GameFinishedData finishData)
        {
            await _pvpHub.Clients.User(userId.ToString()).GameFinished(finishData);
        }

        public async Task NotifyOpponentDisconnected(Guid userId, DisconnectData disconnectData)
        {
            await _pvpHub.Clients.User(userId.ToString()).OpponentDisconnected(disconnectData);
        }



        public async Task NotifyPlayerJoined(Guid lobbyId, PlayerJoinedData data)
        {
             await _kothHub.Clients.Group($"lobby_{lobbyId}").PlayerJoined(data);
        }

        public async Task NotifyPlayerLeft(Guid lobbyId, PlayerLeftData data)
        {
             await _kothHub.Clients.Group($"lobby_{lobbyId}").PlayerLeft(data);
        }

        public async Task NotifyLobbyCountdown(Guid lobbyId, int secondsRemaining)
        {
             await _kothHub.Clients.Group($"lobby_{lobbyId}").LobbyCountdown(secondsRemaining);
        }

        public async Task NotifyLobbyCountdownCancelled(Guid lobbyId)
        {
             await _kothHub.Clients.Group($"lobby_{lobbyId}").LobbyCountdownCancelled();
        }
    }
}
