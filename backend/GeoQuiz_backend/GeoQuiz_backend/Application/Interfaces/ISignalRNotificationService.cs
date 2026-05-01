using GeoQuiz_backend.Application.DTOs.KingOfTheHill;
using GeoQuiz_backend.Application.DTOs.PvP;
using GeoQuiz_backend.Application.DTOs.User;
using GeoQuiz_backend.Domain.Mongo;

namespace GeoQuiz_backend.Application.Interfaces
{
    public interface ISignalRNotificationService
    {
        Task NotifyAchievementUnlocked(Guid userId, AchievementUnlockedMessage data);

        Task NotifyMatchFound(Guid userId, MatchFoundWithDraftData matchData);
        Task NotifyDraftUpdated(Guid matchId, DraftUpdateData updateData);
        Task NotifyDraftResume(Guid userId, MatchFoundWithDraftData resumeData);

        Task NotifyGameReady(Guid matchId, GameReadyData gameData);
        Task NotifyGameResume(Guid userId, GameResumeData resumeData);

        Task NotifyQuestionResult(Guid userId, SubmitAnswerResponse resultData);

        Task NotifyTimerUpdate(Guid matchId, TimerUpdateData timerData);

        Task NotifyGameFinished(Guid userId, GameFinishedData finishData);

        Task NotifyOpponentDisconnected(Guid userId, DisconnectData disconnectData);
        Task NotifyForcePvPDisconnect(string connectionId, LocalizedText message);


        Task NotifyPlayerJoinedToOthers(Guid lobbyId, PlayerJoinedData data, string connectionIdToExclude);
        Task NotifyCurrentPlayerAboutLobby(Guid userId, LobbyInitialStateData data);
        Task NotifyPlayerLeft(Guid lobbyId, PlayerLeftData data);
        Task NotifyLobbyCountdown(Guid lobbyId, int secondsRemaining);
        Task NotifyLobbyCountdownCancelled(Guid lobbyId);
        Task NotifyMatchStarted(Guid lobbyId, MatchStartedData data);
        Task NotifyMatchResume(Guid userId, MatchResumeData resumeData);
        Task NotifyRoundStarted(Guid matchId, RoundStartedData data);
        Task NotifyRoundFinished(Guid matchId, RoundFinishedData data);
        Task NotifyPlayerEliminated(Guid userId, PlayerEliminatedData data);
        Task NotifyAnswerResult(Guid userId, AnswerResultData data);
        Task NotifyMatchFinished(Guid userId, MatchFinishedData data);
        Task NotifyForceKothDisconnect(string connectionId, LocalizedText message);
    }
}
