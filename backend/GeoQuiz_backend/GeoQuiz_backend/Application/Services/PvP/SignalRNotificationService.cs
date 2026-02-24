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

    }
}
