using GeoQuiz_backend.DTOs.PvP;

namespace GeoQuiz_backend.Application.Hubs
{
    public interface IPvPHubClient
    {
        Task MatchFound(MatchFoundWithDraftData matchData);
        Task DraftUpdated(DraftUpdateData updateData);


        Task GameReady(GameReadyData gameData);
        Task QuestionResult(QuestionResultData resultData);
        Task TimerUpdate(TimerUpdateData timerData);


        Task GameFinished(GameFinishedData finishData);
        Task OpponentDisconnected(DisconnectData disconnectData);
    }
}
