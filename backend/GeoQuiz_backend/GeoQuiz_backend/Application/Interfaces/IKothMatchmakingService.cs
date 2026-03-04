using GeoQuiz_backend.Domain.Entities;

namespace GeoQuiz_backend.Application.Interfaces
{
    public interface IKothMatchmakingService
    {
        Task<(KothLobby? Lobby, string UserName, int UserLevel)> JoinLobbyAsync(Guid userId);
        Task LeaveLobbyAsync(Guid userId);
        Task<bool> IsInLobbyAsync(Guid userId);
    }
}
