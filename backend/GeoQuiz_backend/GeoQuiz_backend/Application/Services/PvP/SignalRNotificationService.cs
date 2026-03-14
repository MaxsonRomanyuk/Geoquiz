using GeoQuiz_backend.Application.Interfaces;
using GeoQuiz_backend.API.HubClients;
using GeoQuiz_backend.API.Hubs;
using GeoQuiz_backend.Application.DTOs.KingOfTheHill;
using GeoQuiz_backend.Application.DTOs.PvP;
using Microsoft.AspNetCore.SignalR;

namespace GeoQuiz_backend.Application.Services.PvP
{
    public class SignalRNotificationService : ISignalRNotificationService
    {
        private readonly IHubContext<PvPHub, IPvPHubClient> _pvpHub;
        private readonly IHubContext<KothHub, IKothHubClient> _kothHub;

        public SignalRNotificationService(IHubContext<PvPHub, IPvPHubClient> pvpHub, IHubContext<KothHub, IKothHubClient> kothHub)
        {
            _pvpHub = pvpHub;
            _kothHub = kothHub;

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



        public async Task NotifyPlayerJoinedToOthers(Guid lobbyId, PlayerJoinedData data, string connectionIdToExclude)
        {
             await _kothHub.Clients.GroupExcept($"lobby_{lobbyId}", connectionIdToExclude).PlayerJoinedToOthers(data);
        }
        public async Task NotifyCurrentPlayerAboutLobby(Guid userId, LobbyInitialStateData data)
        {
            await _kothHub.Clients.User(userId.ToString()).PlayerAboutLobby(data);
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

        public async Task NotifyMatchStarted(Guid lobbyId, MatchStartedData data)
        {
            await _kothHub.Clients.Group($"lobby_{lobbyId}").MatchStarted(data);
        }
        public async Task NotifyRoundStarted(Guid matchId, RoundStartedData data)
        {
            await _kothHub.Clients.Group($"match_{matchId}").RoundStarted(data);
            //await _kothHub.Clients.User(matchId.ToString()).RoundStarted(data);
        }
        public async Task NotifyRoundFinished(Guid matchId, RoundFinishedData data)
        {
            await _kothHub.Clients.Group($"match_{matchId}").RoundFinished(data);
        }
        public async Task NotifyPlayerEliminated(Guid userId, PlayerEliminatedData data)
        {
            await _kothHub.Clients.User(userId.ToString()).PlayerEliminated(data);

        }
        public async Task NotifyAnswerResult(Guid userId, AnswerResultData data)
        {
            await _kothHub.Clients.User(userId.ToString()).AnswerResult(data);
            
        }
        public async Task NotifyMatchFinished(Guid matchId, MatchFinishedData data)
        {
            await _kothHub.Clients.Group($"match_{matchId}").MatchFinished(data);
        }
    }
}
