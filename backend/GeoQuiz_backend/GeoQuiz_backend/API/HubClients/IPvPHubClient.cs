using GeoQuiz_backend.Application.DTOs.PvP;

namespace GeoQuiz_backend.API.HubClients
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
