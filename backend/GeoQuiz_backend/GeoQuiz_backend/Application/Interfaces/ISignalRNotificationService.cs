using GeoQuiz_backend.DTOs.KingOfTheHill;
using GeoQuiz_backend.DTOs.PvP;

namespace GeoQuiz_backend.Application.Interfaces
{
    public interface ISignalRNotificationService
    {
        Task NotifyMatchFound(Guid userId, MatchFoundWithDraftData matchData);

        Task NotifyDraftUpdated(Guid matchId, DraftUpdateData updateData);

        Task NotifyGameReady(Guid matchId, GameReadyData gameData);

        Task NotifyQuestionResult(Guid userId, QuestionResultData resultData);

        Task NotifyTimerUpdate(Guid matchId, TimerUpdateData timerData);

        Task NotifyGameFinished(Guid userId, GameFinishedData finishData);

        Task NotifyOpponentDisconnected(Guid userId, DisconnectData disconnectData);



        Task NotifyPlayerJoined(Guid lobbyId, PlayerJoinedData data);
        Task NotifyPlayerLeft(Guid lobbyId, PlayerLeftData data);
        Task NotifyLobbyCountdown(Guid lobbyId, int secondsRemaining);
        Task NotifyLobbyCountdownCancelled(Guid lobbyId);
    }
}
