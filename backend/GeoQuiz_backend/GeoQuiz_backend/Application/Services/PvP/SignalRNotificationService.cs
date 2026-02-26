using GeoQuiz_backend.Application.Hubs;
using GeoQuiz_backend.Application.Interfaces;
using GeoQuiz_backend.DTOs.PvP;
using Microsoft.AspNetCore.SignalR;

namespace GeoQuiz_backend.Application.Services.PvP
{
    public class SignalRNotificationService : ISignalRNotificationService
    {
        private readonly IHubContext<PvPHub, IPvPHubClient> _hubContext;

        public SignalRNotificationService(IHubContext<PvPHub, IPvPHubClient> hubContext)
        {
            _hubContext = hubContext;
        }

        public async Task NotifyMatchFound(Guid userId, MatchFoundWithDraftData matchData)
        {
            await _hubContext.Clients.User(userId.ToString()).MatchFound(matchData);
        }
        public async Task NotifyDraftUpdated(Guid matchId, DraftUpdateData updateData)
        {
            await _hubContext.Clients.Group($"match_{matchId}").DraftUpdated(updateData);
        }
        public async Task NotifyGameReady(Guid matchId, GameReadyData gameData)
        {
            await _hubContext.Clients.Group($"match_{matchId}").GameReady(gameData);
        }

        public async Task NotifyQuestionResult(Guid userId, QuestionResultData resultData)
        {
            await _hubContext.Clients.User(userId.ToString()).QuestionResult(resultData);
        }

        public async Task NotifyTimerUpdate(Guid matchId, TimerUpdateData timerData)
        {
            await _hubContext.Clients.Group($"match_{matchId}").TimerUpdate(timerData);
        }

        public async Task NotifyGameFinished(Guid userId, GameFinishedData finishData)
        {
            await _hubContext.Clients.User(userId.ToString()).GameFinished(finishData);
        }

        public async Task NotifyOpponentDisconnected(Guid userId, DisconnectData disconnectData)
        {
            await _hubContext.Clients.User(userId.ToString()).OpponentDisconnected(disconnectData);
        }
    }
}
