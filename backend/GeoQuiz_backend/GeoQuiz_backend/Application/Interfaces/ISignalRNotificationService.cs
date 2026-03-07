using GeoQuiz_backend.Application.DTOs.KingOfTheHill;
using GeoQuiz_backend.Application.DTOs.PvP;

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



        Task NotifyPlayerJoinedToOthers(Guid lobbyId, PlayerJoinedData data, string connectionIdToExclude);
        Task NotifyCurrentPlayerAboutLobby(Guid userId, LobbyInitialStateData data);
        Task NotifyPlayerLeft(Guid lobbyId, PlayerLeftData data);
        Task NotifyLobbyCountdown(Guid lobbyId, int secondsRemaining);
        Task NotifyLobbyCountdownCancelled(Guid lobbyId);

        Task NotifyMatchStarted(Guid matchId, MatchStartedData data);
        Task NotifyRoundStarted(Guid matchId, RoundStartedData data);
        Task NotifyRoundFinished(Guid matchId, RoundFinishedData data);
        Task NotifyPlayerEliminated(Guid userId, PlayerEliminatedData data);
        Task NotifyAnswerResult(Guid userId, AnswerResultData data);
        Task NotifyMatchFinished(Guid userId, MatchFinishedData data);
    }
}
