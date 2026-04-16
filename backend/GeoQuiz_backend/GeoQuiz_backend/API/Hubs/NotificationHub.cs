using GeoQuiz_backend.API.HubClients;
using Microsoft.AspNetCore.SignalR;
using System.Collections.Concurrent;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;

namespace GeoQuiz_backend.API.Hubs
{
    public class NotificationHub : Hub<INotificationClient>
    {
        private readonly ILogger<NotificationHub> _logger;
        private static readonly ConcurrentDictionary<Guid, string> _connections = new();
        public NotificationHub(ILogger<NotificationHub> logger)
        {
            _logger = logger;
        }

        public override async Task OnConnectedAsync()
        {
            var userId = GetUserId();
            var connectionId = Context.ConnectionId;

            if (_connections.TryGetValue(userId, out var oldConnectionId))
            {
                _logger.LogInformation("User {UserId} reconnecting. Old: {Old}, New: {New}", userId, oldConnectionId, connectionId);
            }

            _connections[userId] = connectionId;

            _logger.LogInformation("User {UserId} connected to NotificationHub ({ConnectionId}) at {date}",userId, connectionId, DateTime.UtcNow.ToString());

            await base.OnConnectedAsync();
        }

        public override async Task OnDisconnectedAsync(Exception? exception)
        {
            var userId = GetUserId();

            if (_connections.TryRemove(userId, out var connectionId))
            {
                _logger.LogInformation("User {UserId} disconnected from NotificationHub ({ConnectionId}) at {date}", userId, connectionId, DateTime.UtcNow.ToString());
            }

            await base.OnDisconnectedAsync(exception);
        }

        private Guid GetUserId()
        {
            var userIdClaim = Context.User?.FindFirstValue(ClaimTypes.NameIdentifier)
                ?? Context.User?.FindFirstValue(JwtRegisteredClaimNames.Sub);

            if (string.IsNullOrEmpty(userIdClaim) || !Guid.TryParse(userIdClaim, out var userId))
                throw new HubException("Invalid user identification");

            return userId;
        }
    }
}
