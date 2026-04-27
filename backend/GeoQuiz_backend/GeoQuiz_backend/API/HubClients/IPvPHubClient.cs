using GeoQuiz_backend.Application.DTOs.PvP;
using GeoQuiz_backend.Domain.Mongo;

namespace GeoQuiz_backend.API.HubClients
{
    public interface IPvPHubClient
    {
        Task MatchFound(MatchFoundWithDraftData matchData);
        Task DraftUpdated(DraftUpdateData updateData);
        Task DraftResume(MatchFoundWithDraftData resumeData);

        Task GameReady(GameReadyData gameData);
        Task GameResume(GameResumeData gameResumeData);
        Task QuestionResult(SubmitAnswerResponse resultData);
        Task TimerUpdate(TimerUpdateData timerData);


        Task GameFinished(GameFinishedData finishData);
        Task OpponentDisconnected(DisconnectData disconnectData);

        Task ForceDisconnect(LocalizedText message);
    }
}
